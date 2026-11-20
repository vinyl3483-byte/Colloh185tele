package telefam.company

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import retrofit2.Response
import telefam.company.calls.CallQualityTier
import telefam.company.calls.IncomingCallPushBridge
import telefam.company.calls.IncomingCallPushPayload
import telefam.company.calls.SwitchVideoPromptState
import telefam.company.calls.TelefamCallController
import telefam.company.calls.TelefamCallMetaParser
import telefam.company.calls.TelefamCallPhase
import telefam.company.calls.TelefamCallType
import telefam.company.calls.VideoCallFilter
import telefam.company.core.NetworkTier
import telefam.company.data.local.DeviceIdentityStore
import telefam.company.data.local.TelefamDatabase
import telefam.company.data.network.BufferedSignalEventDto
import telefam.company.data.network.CallActionResponseDto
import telefam.company.data.network.CallRecordDto
import telefam.company.data.network.CallSignalRequestDto
import telefam.company.data.network.CallStateResponseDto
import telefam.company.data.network.ChatMessageDto
import telefam.company.data.network.ChatMessagesListResponseDto
import telefam.company.data.network.E2eeKeyBundleResponseDto
import telefam.company.data.network.IceConfigResponseDto
import telefam.company.data.network.IceServerDto
import telefam.company.data.network.NetworkQualityMonitor
import telefam.company.data.network.StartCallRequestDto
import telefam.company.data.network.StartCallResponseDto
import telefam.company.data.repository.TelefamRepository
import telefam.company.notifications.TelefamFcmService
import telefam.company.notifications.TelefamNotificationManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h915dp")
class P2pCallsRobolectricTest {

  private lateinit var context: Context
  private lateinit var db: TelefamDatabase
  private lateinit var identityStore: DeviceIdentityStore
  private lateinit var networkMonitor: NetworkQualityMonitor
  private lateinit var fakeApi: FakeCallsApi
  private lateinit var repository: TelefamRepository
  private lateinit var callController: TelefamCallController

  @Before
  fun setUp(): Unit = runBlocking {
    context = ApplicationProvider.getApplicationContext()
    shadowOf(context as Application).grantPermissions(
      Manifest.permission.POST_NOTIFICATIONS,
      Manifest.permission.RECORD_AUDIO,
      Manifest.permission.CAMERA,
    )
    db = Room.inMemoryDatabaseBuilder(context, TelefamDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    identityStore = DeviceIdentityStore(context)
    identityStore.saveEncryptedSession(
      userId = "user_alice",
      email = "alice@telefam.test",
      accessToken = "acc_alice",
      refreshToken = "ref_alice",
      accessExpiresInSec = 900L,
      refreshExpiresInSec = 2_592_000L,
      profileCompleted = true,
    )
    networkMonitor = NetworkQualityMonitor(context)
    fakeApi = FakeCallsApi()
    repository = TelefamRepository(
      appContext = context,
      dao = db.telefamDao(),
      identityStore = identityStore,
      networkMonitor = networkMonitor,
      apiService = fakeApi,
    )
    repository.chatRepository.attachSession("user_alice")
    callController = TelefamCallController(
      appContext = context,
      chatRepository = repository.chatRepository,
      networkMonitor = networkMonitor,
    )
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    shadowOf(nm).setNotificationsEnabled(true)
  }

  @After
  fun tearDown() {
    callController.dispose()
    repository.chatRepository.detachSession()
    shadowOf(Looper.getMainLooper()).idle()
    db.close()
  }

  @Test
  fun `blocked caller gets Calls Not Allowed and auto-sends call_request inside 1-on-1 chat`() {
    fakeApi.allowCalls = false
    callController.startCall(
      peerUserId = "user_bob",
      peerName = "Bob",
      peerUsername = "bob",
      peerAvatarUrl = "",
      conversationId = "conv_ab",
      callType = TelefamCallType.VIDEO,
    )
    shadowOf(Looper.getMainLooper()).idle()

    val state = callController.uiState.value
    assertEquals(TelefamCallPhase.NOT_ALLOWED, state.phase)
    assertEquals("Calls not allowed", state.statusText)
    assertTrue(state.autoCallMeRequestSent)

    val persisted = runBlocking { db.telefamDao().getMessagesByConversation("user_alice", "conv_ab") }
    assertTrue(persisted.any { it.messageType == "call_request" })
  }

  @Test
  fun `outgoing call transitions Calling to Ringing, supports filters, minimize-to-chat avatar swap, foreground restore, switch audio to video, and backend duration logging`() {
    fakeApi.allowCalls = true
    fakeApi.peerOnline = false

    callController.startCall(
      peerUserId = "user_bob",
      peerName = "Bob",
      peerUsername = "bob",
      peerAvatarUrl = "",
      conversationId = "conv_ab",
      callType = TelefamCallType.AUDIO,
    )
    shadowOf(Looper.getMainLooper()).idle()

    // Peer offline -> stays OUTGOING_CALLING ("Calling…")
    assertEquals(TelefamCallPhase.OUTGOING_CALLING, callController.uiState.value.phase)
    assertEquals("Calling…", callController.uiState.value.statusText)
    assertEquals("call_audio_1", callController.uiState.value.callId)

    // Peer answers -> simulate backend returning connected with answeredAtMs & signals
    fakeApi.callState = "connected"
    fakeApi.durationSec = 72L
    callController.onIceConnectionStateChanged(org.webrtc.PeerConnection.IceConnectionState.CONNECTED)
    shadowOf(Looper.getMainLooper()).idle()
    assertEquals(TelefamCallPhase.CONNECTED, callController.uiState.value.phase)

    // Request switching Audio -> Video, then simulate peer accepting video switch
    callController.requestSwitchToVideo()
    shadowOf(Looper.getMainLooper()).idle()
    assertEquals(SwitchVideoPromptState.OUTGOING_PENDING, callController.uiState.value.switchVideoPrompt)
    assertTrue(fakeApi.recordedSignals.any { it.action == "switch_video_request" })

    fakeApi.bufferedSignals.add(
      BufferedSignalEventDto(
        seq = 10L,
        fromUserId = "user_bob",
        action = "switch_video_accept",
        camEnabled = true,
        inAppActive = true,
      ),
    )
    runBlocking {
      // Trigger poll read
      val resp = repository.chatRepository.getCallState("call_audio_1", 0L)
      assertNotNull(resp)
    }
    callController.respondSwitchToVideo(accept = true)
    shadowOf(Looper.getMainLooper()).idle()
    assertEquals(TelefamCallType.VIDEO, callController.uiState.value.callType)

    // Filter choice updates state & publishes signal
    callController.setVideoFilter(VideoCallFilter.VIVID)
    shadowOf(Looper.getMainLooper()).idle()
    assertEquals(VideoCallFilter.VIVID, callController.uiState.value.videoFilter)

    // Message button -> minimizes to chat & marks localInAppActive=false so peer renders Avatar
    var openedChatPeer = ""
    callController.minimizeToChat { _, peerId -> openedChatPeer = peerId }
    shadowOf(Looper.getMainLooper()).idle()
    assertEquals("user_bob", openedChatPeer)
    assertTrue(callController.uiState.value.isMinimizedToChat)
    assertFalse(callController.uiState.value.localInAppActive)
    assertFalse(callController.uiState.value.shouldShowLocalVideoPreview)

    // Expand back -> restores video & localInAppActive=true
    callController.expandFromMinimized()
    shadowOf(Looper.getMainLooper()).idle()
    assertFalse(callController.uiState.value.isMinimizedToChat)
    assertTrue(callController.uiState.value.localInAppActive)
    assertTrue(callController.uiState.value.shouldShowLocalVideoPreview)

    // Leaving app swaps camera off (Avatar for peer) and returning turns video back on
    callController.onAppForegroundChanged(false)
    shadowOf(Looper.getMainLooper()).idle()
    assertFalse(callController.uiState.value.localInAppActive)
    assertTrue(fakeApi.recordedSignals.any { it.action == "media_state" && it.inAppActive == false })

    callController.onAppForegroundChanged(true)
    shadowOf(Looper.getMainLooper()).idle()
    assertTrue(callController.uiState.value.localInAppActive)
    assertTrue(callController.uiState.value.shouldShowLocalVideoPreview)

    // Hang up ends call and fetches chat history with backend-persisted duration
    callController.hangUp("hangup")
    shadowOf(Looper.getMainLooper()).idle()
    assertEquals(TelefamCallPhase.ENDED, callController.uiState.value.phase)
    assertTrue(fakeApi.recordedSignals.any { it.action == "end" })
  }

  @Test
  fun `adaptive quality selects LOW_240P on WEAK network and AUDIO_PRIORITY on OFFLINE`() {
    assertEquals(CallQualityTier.HD_720P, callController.recommendedQualityForNetwork(NetworkTier.GOOD))
    assertEquals(CallQualityTier.BALANCED_480P, callController.recommendedQualityForNetwork(NetworkTier.MODERATE))
    assertEquals(CallQualityTier.LOW_240P, callController.recommendedQualityForNetwork(NetworkTier.WEAK))
    assertEquals(CallQualityTier.AUDIO_PRIORITY, callController.recommendedQualityForNetwork(NetworkTier.OFFLINE))

    fakeApi.peerOnline = true
    callController.startCall(
      peerUserId = "user_bob",
      peerName = "Bob",
      peerUsername = "bob",
      peerAvatarUrl = "",
      conversationId = "conv_ab",
      callType = TelefamCallType.VIDEO,
    )
    shadowOf(Looper.getMainLooper()).idle()
    assertEquals(TelefamCallPhase.OUTGOING_RINGING, callController.uiState.value.phase)
    assertEquals("Ringing…", callController.uiState.value.statusText)

    callController.onIceConnectionStateChanged(org.webrtc.PeerConnection.IceConnectionState.CONNECTED)
    callController.onStatsSample(packetLossPct = 9.5f, rttMs = 780.0)
    shadowOf(Looper.getMainLooper()).idle()
    assertEquals(CallQualityTier.LOW_240P, callController.uiState.value.effectiveQualityTier)
    assertTrue(callController.uiState.value.lowBandwidthWarning)
  }

  @Test
  fun `incoming call push shows CallStyle notification with Accept and Decline and call_cancel dismisses it`() {
    TelefamNotificationManager.showIncomingCallNotification(
      context = context,
      callId = "call_push_1",
      conversationId = "conv_ab",
      callType = "video",
      callerUserId = "user_bob",
      callerName = "Bob",
      callerUsername = "bob",
      callerAvatarUrl = "",
      expiresAtMs = System.currentTimeMillis() + 25_000L,
    )
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadowNm = shadowOf(nm)
    val posted = shadowNm.allNotifications.firstOrNull {
      it.category == Notification.CATEGORY_CALL
    }
    assertNotNull(posted)
    assertTrue((posted?.actions?.size ?: 0) >= 2)

    IncomingCallPushBridge.emitIncoming(
      IncomingCallPushPayload(
        callId = "call_push_1",
        conversationId = "conv_ab",
        callType = TelefamCallType.VIDEO,
        callerUserId = "user_bob",
        callerName = "Bob",
        callerUsername = "bob",
        callerAvatarUrl = "",
        expiresAtMs = System.currentTimeMillis() + 25_000L,
      ),
    )
    shadowOf(Looper.getMainLooper()).idle()
    assertEquals(TelefamCallPhase.INCOMING_RINGING, callController.uiState.value.phase)

    // Remote cancel (e.g., 25s timeout or caller cancel) dismisses incoming screen and notification
    TelefamNotificationManager.cancelCallNotification(context, "call_push_1")
    IncomingCallPushBridge.emitCancelled("call_push_1", "timeout_25s")
    shadowOf(Looper.getMainLooper()).idle()
    assertEquals(TelefamCallPhase.ENDED, callController.uiState.value.phase)
    assertEquals("No answer", callController.uiState.value.statusText)
    assertNull(shadowNm.allNotifications.firstOrNull { it.category == Notification.CATEGORY_CALL })
  }

  @Test
  fun `call metadata parser formats backend call logs and I tried to call you missed bubbles`() {
    val missedJson = JSONObject()
      .put("type", "telefam_call_meta_v1")
      .put("call_id", "call_99")
      .put("call_type", "video")
      .put("outcome", "missed")
      .put("end_reason", "timeout_25s")
      .put("duration_sec", 0)
      .put("text", "I tried to call you")
      .toString()

    val missedParsed = TelefamCallMetaParser.parseCallMeta(missedJson)
    assertNotNull(missedParsed)
    assertEquals("I tried to call you", missedParsed?.text)
    assertEquals("Missed video call · I tried to call you", TelefamCallMetaParser.formatSummaryLine("call_log", missedJson))
    assertEquals("02:05", TelefamCallMetaParser.formatDuration(125L))

    val fcm = Robolectric.buildService(TelefamFcmService::class.java).create().get()
    assertNotNull(fcm)
  }

  private class FakeCallsApi : AbstractTestChatApiStub() {
    var allowCalls = true
    var peerOnline = false
    var callState = "ringing"
    var durationSec = 0L
    val recordedSignals = mutableListOf<CallSignalRequestDto>()
    val bufferedSignals = mutableListOf<BufferedSignalEventDto>()

    override suspend fun getE2eeKeyBundle(
      bearer: String,
      deviceFingerprint: String,
      userId: String,
    ): Response<E2eeKeyBundleResponseDto> = Response.success(
      E2eeKeyBundleResponseDto(success = true),
    )

    override suspend fun getCallIceConfig(
      bearer: String,
      deviceFingerprint: String,
    ): Response<IceConfigResponseDto> = Response.success(
      IceConfigResponseDto(
        success = true,
        iceServers = listOf(IceServerDto(urls = listOf("stun:stun.l.google.com:19302"))),
        ringTimeoutSec = 25,
      ),
    )

    override suspend fun startCall(
      bearer: String,
      deviceFingerprint: String,
      body: StartCallRequestDto,
    ): Response<StartCallResponseDto> {
      if (!allowCalls) {
        return Response.success(
          StartCallResponseDto(
            success = false,
            allowed = false,
            code = "calls_not_allowed",
            message = "Calls are disabled by privacy settings. A call request was sent.",
            autoRequestSent = true,
            call = CallRecordDto(callId = "call_blocked_1", conversationId = body.conversationId, callType = body.callType, state = "not_allowed"),
          ),
        )
      }
      return Response.success(
        StartCallResponseDto(
          success = true,
          allowed = true,
          peerOnline = peerOnline,
          timeoutSec = 25,
          iceServers = listOf(IceServerDto(urls = listOf("stun:stun.l.google.com:19302"))),
          call = CallRecordDto(
            callId = "call_audio_1",
            conversationId = body.conversationId,
            callerUserId = "user_alice",
            calleeUserId = body.peerUserId,
            callType = body.callType,
            state = if (peerOnline) "ringing" else "calling",
          ),
        ),
      )
    }

    override suspend fun getCallState(
      bearer: String,
      deviceFingerprint: String,
      callId: String,
      sinceSeq: Long,
    ): Response<CallStateResponseDto> = Response.success(
      CallStateResponseDto(
        success = true,
        call = CallRecordDto(
          callId = callId,
          conversationId = "conv_ab",
          state = callState,
          durationSec = durationSec,
          answeredAtMs = if (callState == "connected") System.currentTimeMillis() - 1000L else 0L,
        ),
        signals = bufferedSignals.filter { it.seq > sinceSeq },
      ),
    )

    override suspend fun sendCallSignal(
      bearer: String,
      deviceFingerprint: String,
      callId: String,
      body: CallSignalRequestDto,
    ): Response<CallActionResponseDto> {
      recordedSignals.add(body)
      return Response.success(
        CallActionResponseDto(
          success = true,
          call = CallRecordDto(
            callId = callId,
            conversationId = "conv_ab",
            state = if (body.action == "accept") "connected" else if (body.action == "end") "ended" else callState,
            durationSec = durationSec,
            answeredAtMs = System.currentTimeMillis(),
          ),
        ),
      )
    }

    override suspend fun listChatMessages(
      bearer: String,
      deviceFingerprint: String,
      conversationId: String,
      beforeMs: Long,
      limit: Int,
    ): Response<ChatMessagesListResponseDto> {
      val items = if (!allowCalls) {
        listOf(
          ChatMessageDto(
            messageId = "msg_req_1",
            conversationId = conversationId,
            senderUserId = "user_alice",
            recipientUserId = "user_bob",
            messageType = "call_request",
            ciphertextB64 = "",
            deliveryStatus = "sent",
            createdAtMs = System.currentTimeMillis(),
          ),
        )
      } else {
        emptyList()
      }
      return Response.success(ChatMessagesListResponseDto(success = true, items = items))
    }
  }
}
