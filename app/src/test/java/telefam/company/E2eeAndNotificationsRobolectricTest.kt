package telefam.company

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
import org.robolectric.shadows.ShadowNetworkCapabilities
import retrofit2.Response
import telefam.company.chat.PeerBundleInput
import telefam.company.chat.SignalE2eeManager
import telefam.company.core.NotificationSettingsState
import telefam.company.data.local.CachedNotificationEntity
import telefam.company.data.local.ChatConversationEntity
import telefam.company.data.local.ChatMessageEntity
import telefam.company.data.local.DeviceIdentityStore
import telefam.company.data.local.TelefamDatabase
import telefam.company.data.network.BatchConversationActionRequestDto
import telefam.company.data.network.ChatConversationDto
import telefam.company.data.network.ChatConversationsResponseDto
import telefam.company.data.network.ChatMessageDto
import telefam.company.data.network.ChatMessageSingleResponseDto
import telefam.company.data.network.ChatPeerCapabilitiesDto
import telefam.company.data.network.ChatReceiptRequestDto
import telefam.company.data.network.E2eeKeyBundleResponseDto
import telefam.company.data.network.GenericSuccessDto
import telefam.company.data.network.MarkNotificationReadRequestDto
import telefam.company.data.network.NetworkQualityMonitor
import telefam.company.data.network.NotificationUnreadCountResponseDto
import telefam.company.data.network.RegisterPushTokenRequestDto
import telefam.company.data.network.SendChatMessageRequestDto
import telefam.company.data.repository.TelefamRepository
import telefam.company.notifications.TelefamFcmService
import telefam.company.notifications.TelefamNotificationManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h915dp")
class E2eeAndNotificationsRobolectricTest {

  private lateinit var context: Context
  private lateinit var db: TelefamDatabase
  private lateinit var identityStore: DeviceIdentityStore
  private lateinit var networkMonitor: NetworkQualityMonitor
  private lateinit var fakeApi: FakeNotifAndE2eeApi
  private lateinit var repository: TelefamRepository
  private lateinit var fcmService: TelefamFcmService

  @Before
  fun setUp(): Unit = runBlocking {
    context = ApplicationProvider.getApplicationContext()
    setNetworkOnline(true)
    shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
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
    networkMonitor.refreshStatus()
    fakeApi = FakeNotifAndE2eeApi()
    repository = TelefamRepository(
      appContext = context,
      dao = db.telefamDao(),
      identityStore = identityStore,
      networkMonitor = networkMonitor,
      apiService = fakeApi,
    )
    fcmService = Robolectric.buildService(TelefamFcmService::class.java).create().get()
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    shadowOf(nm).setNotificationsEnabled(true)
  }

  @After
  fun tearDown() {
    shadowOf(Looper.getMainLooper()).idle()
    db.close()
  }

  @Test
  fun `signal E2EE persists cross-platform ECDH fallback keys across restart and handles peer identity rotation and OTK replenishment`() {
    val alice = SignalE2eeManager("user_alice")
    val desktopPeer = SignalE2eeManager("user_desktop")
    val desktopSnap = desktopPeer.exportPublicBundle()

    // Simulate cross-platform ECDH peer (empty kyber key triggers telefam_ecdh_v1 fallback)
    alice.initSessionWithPeer(
      PeerBundleInput(
        peerUserId = "user_desktop",
        deviceId = 1,
        registrationId = desktopSnap.registrationId,
        identityPubB64 = desktopSnap.identityPubB64,
        signedPreKeyId = desktopSnap.signedPreKeyId,
        signedPreKeyPubB64 = desktopSnap.signedPreKeyPubB64,
        signedPreKeySigB64 = desktopSnap.signedPreKeySigB64,
        kyberPreKeyId = 0,
        kyberPreKeyPubB64 = "",
        kyberPreKeySigB64 = "",
      ),
    )
    assertTrue(alice.hasSession("user_desktop"))

    // Restart Alice engine from serialized JSON — fallback peer key must survive restart!
    val aliceRestarted = SignalE2eeManager("user_alice", alice.exportStateJson("user_alice"))
    assertTrue(aliceRestarted.hasSession("user_desktop"))

    val env = aliceRestarted.compressAndEncrypt("user_desktop", "Cross-platform encrypted secret")
    assertEquals("telefam_ecdh_v1", env.encryptionScheme)
    assertEquals("zlib", env.compressionCodec)

    val peerDecoded = desktopPeer.decryptAndDecompress(
      peerUserId = "user_alice",
      isOutgoingFromSelf = false,
      ciphertextB64 = env.ciphertextB64,
      senderCiphertextB64 = env.senderCiphertextB64,
      signalMsgType = env.signalMsgType,
      compressionCodec = env.compressionCodec,
      encryptionScheme = env.encryptionScheme,
    )
    assertEquals("Cross-platform encrypted secret", peerDecoded)

    val aliceSelfDecoded = aliceRestarted.decryptAndDecompress(
      peerUserId = "user_desktop",
      isOutgoingFromSelf = true,
      ciphertextB64 = env.ciphertextB64,
      senderCiphertextB64 = env.senderCiphertextB64,
      signalMsgType = env.signalMsgType,
      compressionCodec = env.compressionCodec,
      encryptionScheme = env.encryptionScheme,
    )
    assertEquals("Cross-platform encrypted secret", aliceSelfDecoded)

    // Test Libsignal peer identity rotation resets cleanly without UntrustedIdentityException
    val bobOld = SignalE2eeManager("user_bob")
    val bobOldSnap = bobOld.exportPublicBundle()
    alice.initSessionWithPeer(
      PeerBundleInput(
        peerUserId = "user_bob",
        deviceId = 1,
        registrationId = bobOldSnap.registrationId,
        identityPubB64 = bobOldSnap.identityPubB64,
        signedPreKeyId = bobOldSnap.signedPreKeyId,
        signedPreKeyPubB64 = bobOldSnap.signedPreKeyPubB64,
        signedPreKeySigB64 = bobOldSnap.signedPreKeySigB64,
        kyberPreKeyId = bobOldSnap.kyberPreKeyId,
        kyberPreKeyPubB64 = bobOldSnap.kyberPreKeyPubB64,
        kyberPreKeySigB64 = bobOldSnap.kyberPreKeySigB64,
        preKeyId = bobOldSnap.oneTimePreKeys.first().first,
        preKeyPubB64 = bobOldSnap.oneTimePreKeys.first().second,
      ),
    )

    val bobRotated = SignalE2eeManager("user_bob")
    val bobRotatedSnap = bobRotated.exportPublicBundle()
    alice.initSessionWithPeer(
      PeerBundleInput(
        peerUserId = "user_bob",
        deviceId = 1,
        registrationId = bobRotatedSnap.registrationId,
        identityPubB64 = bobRotatedSnap.identityPubB64,
        signedPreKeyId = bobRotatedSnap.signedPreKeyId,
        signedPreKeyPubB64 = bobRotatedSnap.signedPreKeyPubB64,
        signedPreKeySigB64 = bobRotatedSnap.signedPreKeySigB64,
        kyberPreKeyId = bobRotatedSnap.kyberPreKeyId,
        kyberPreKeyPubB64 = bobRotatedSnap.kyberPreKeyPubB64,
        kyberPreKeySigB64 = bobRotatedSnap.kyberPreKeySigB64,
        preKeyId = bobRotatedSnap.oneTimePreKeys.first().first,
        preKeyPubB64 = bobRotatedSnap.oneTimePreKeys.first().second,
      ),
    )
    assertTrue(alice.consumeIdentityChangedPeers().contains("user_bob"))
    val rotatedEnv = alice.compressAndEncrypt("user_bob", "Hello rotated identity")
    val rotatedDecoded = bobRotated.decryptAndDecompress(
      peerUserId = "user_alice",
      isOutgoingFromSelf = false,
      ciphertextB64 = rotatedEnv.ciphertextB64,
      senderCiphertextB64 = rotatedEnv.senderCiphertextB64,
      signalMsgType = rotatedEnv.signalMsgType,
      compressionCodec = rotatedEnv.compressionCodec,
      encryptionScheme = rotatedEnv.encryptionScheme,
    )
    assertEquals("Hello rotated identity", rotatedDecoded)

    // OTK replenishment check (default 20 keys, replenish with minThreshold = 25 adds more)
    val initialCount = alice.remainingPreKeyCount()
    assertEquals(20, initialCount)
    assertFalse(alice.replenishPreKeysIfLow(minThreshold = 5, targetBatch = 20))
    assertTrue(alice.replenishPreKeysIfLow(minThreshold = 21, targetBatch = 25))
    assertEquals(25, alice.remainingPreKeyCount())
  }

  @Test
  fun `FCM decrypts encrypted chat push, masks view-once without reply action, and auto-cancels on unsend`() = runBlocking {
    val bob = SignalE2eeManager("user_bob")
    val aliceChat = repository.chatRepository
    val aliceSnap = run {
      // Trigger Alice's Signal key creation & upload in fakeApi
      fakeApi.peerBundle = bob.exportPublicBundle()
      db.telefamDao().upsertConversation(sampleConv("conv_push", "user_bob", "Bob"))
      aliceChat.sendTextMessage("conv_push", "user_bob", "Bootstrap")
      fakeApi.uploadedBundle!!
    }

    bob.initSessionWithPeer(
      PeerBundleInput(
        peerUserId = "user_alice",
        deviceId = 1,
        registrationId = aliceSnap.registrationId,
        identityPubB64 = aliceSnap.identityPubB64,
        signedPreKeyId = aliceSnap.signedPreKeyId,
        signedPreKeyPubB64 = aliceSnap.signedPreKeyPubB64,
        signedPreKeySigB64 = aliceSnap.signedPreKeySigB64,
        kyberPreKeyId = aliceSnap.kyberPreKeyId,
        kyberPreKeyPubB64 = aliceSnap.kyberPreKeyPubB64,
        kyberPreKeySigB64 = aliceSnap.kyberPreKeySigB64,
        preKeyId = aliceSnap.oneTimePreKeys.first().keyId,
        preKeyPubB64 = aliceSnap.oneTimePreKeys.first().publicKeyB64,
      ),
    )

    val env = bob.compressAndEncrypt("user_alice", "Hey @alice look at this secret")
    fcmService.handlePushPayload(
      repository,
      mapOf(
        "type" to "chat_message",
        "recipient_user_id" to "user_alice",
        "conversation_id" to "conv_push",
        "message_id" to "msg_push_1",
        "sender_user_id" to "user_bob",
        "sender_name" to "Bob",
        "sender_username" to "bob",
        "message_type" to "text",
        "compression_codec" to env.compressionCodec,
        "encryption_scheme" to env.encryptionScheme,
        "ciphertext_b64" to env.ciphertextB64,
        "signal_msg_type" to env.signalMsgType.toString(),
        "is_view_once" to "false",
        "created_at_ms" to "1700001000000",
      ),
    )

    val cachedMsg = db.telefamDao().getChatMessage("msg_push_1")
    assertNotNull(cachedMsg)
    assertEquals("Hey @alice look at this secret", cachedMsg?.plainText)
    assertTrue(fakeApi.sentReceipts.any { it.first == "msg_push_1" && it.second == "delivered" })

    // Verify notification posted with both Reply (RemoteInput) and Mark as read actions
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val active = shadowOf(nm).allNotifications
    assertTrue(active.isNotEmpty())
    val chatNotif = active.last()
    val titles = chatNotif.actions?.map { it.title?.toString().orEmpty() }.orEmpty()
    assertTrue(titles.any { it.contains("Reply", ignoreCase = true) })
    assertTrue(titles.any { it.contains("Mark", ignoreCase = true) })
    val replyAction = chatNotif.actions!!.first { it.title.toString().contains("Reply", ignoreCase = true) }
    assertTrue((replyAction.remoteInputs?.size ?: 0) > 0)

    // Now test View-Once Voice Push: must mask body and MUST NOT include Reply RemoteInput action
    nm.cancelAll()
    fcmService.handlePushPayload(
      repository,
      mapOf(
        "type" to "chat_message",
        "recipient_user_id" to "user_alice",
        "conversation_id" to "conv_push_vo",
        "message_id" to "msg_push_vo",
        "sender_user_id" to "user_bob",
        "sender_name" to "Bob",
        "message_type" to "voice_once",
        "ciphertext_b64" to "SECRET_SHOULD_NOT_SHOW_12s",
        "is_view_once" to "true",
        "created_at_ms" to "1700002000000",
      ),
    )

    val voNotif = shadowOf(nm).allNotifications.lastOrNull()
    assertNotNull(voNotif)
    val voText = voNotif!!.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
    assertFalse(voText.contains("SECRET_SHOULD_NOT_SHOW"))
    assertTrue(voText.contains("view-once", ignoreCase = true))
    val voActionTitles = voNotif.actions?.map { it.title?.toString().orEmpty() }.orEmpty()
    assertFalse(voActionTitles.any { it.contains("Reply", ignoreCase = true) })
    assertTrue(voActionTitles.any { it.contains("Mark", ignoreCase = true) })

    // Now test remote message_cancel removes cached message & cancels notification
    fcmService.handlePushPayload(
      repository,
      mapOf(
        "type" to "message_cancel",
        "recipient_user_id" to "user_alice",
        "conversation_id" to "conv_push_vo",
        "message_id" to "msg_push_vo",
      ),
    )
    assertNull(db.telefamDao().getChatMessage("msg_push_vo"))
    assertNull(shadowOf(nm).getNotification(TelefamNotificationManager.notificationIdForConversation("conv_push_vo")))
  }

  @Test
  fun `FCM enforces recipient_user_id isolation, social notifications, quiet hours, and logout token cleanup`() = runBlocking {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // 1. Cross-account payload (intended for user_other) must be dropped completely
    fcmService.handlePushPayload(
      repository,
      mapOf(
        "type" to "follow",
        "recipient_user_id" to "user_other",
        "notification_id" to "notif_cross_1",
        "actor_user_id" to "user_bob",
        "actor_name" to "Bob",
        "title" to "Bob",
        "body" to "Started following you",
      ),
    )
    assertTrue(db.telefamDao().observeNotifications("user_alice").first().isEmpty())
    assertTrue(shadowOf(nm).allNotifications.isEmpty())

    // 2. Same-user follow, like, mention payloads are persisted in Room and displayed
    listOf("follow", "like", "mention").forEachIndexed { idx, kind ->
      fcmService.handlePushPayload(
        repository,
        mapOf(
          "type" to kind,
          "recipient_user_id" to "user_alice",
          "notification_id" to "notif_$kind",
          "actor_user_id" to "user_bob",
          "actor_name" to "Bob",
          "actor_username" to "bob",
          "post_id" to if (kind != "follow") "post_$idx" else "",
          "title" to "Bob",
          "body" to "Event $kind",
          "created_at_ms" to (1_700_000_000_000L + idx).toString(),
        ),
      )
    }
    val stored = db.telefamDao().observeNotifications("user_alice").first()
    assertEquals(3, stored.size)
    assertEquals(3, db.telefamDao().observeUnreadNotificationCount("user_alice").first())

    // 3. Quiet hours & preference checks
    val quietOvernight = NotificationSettingsState(
      masterPause = false,
      channelPush = true,
      typeMessages = true,
      typeComments = true,
      typeFollowers = false,
      quietHoursEnabled = true,
      quietStartMinutes = 22 * 60,
      quietEndMinutes = 7 * 60,
    )
    assertTrue(TelefamNotificationManager.isQuietHoursActive(quietOvernight, nowMinutesOfDay = 23 * 60))
    assertTrue(TelefamNotificationManager.isQuietHoursActive(quietOvernight, nowMinutesOfDay = 2 * 60))
    assertFalse(TelefamNotificationManager.isQuietHoursActive(quietOvernight, nowMinutesOfDay = 12 * 60))
    assertFalse(TelefamNotificationManager.shouldDeliverPushNotification(quietOvernight, "chat_message", nowMinutesOfDay = 23 * 60))
    assertTrue(TelefamNotificationManager.shouldDeliverPushNotification(quietOvernight, "chat_message", nowMinutesOfDay = 12 * 60))
    assertFalse(TelefamNotificationManager.shouldDeliverPushNotification(quietOvernight, "follow", nowMinutesOfDay = 12 * 60))

    // 4. Register push token & verify signOut revokes push token and clears notifications
    assertTrue(repository.registerPushToken("fcm_tok_alice_123"))
    assertEquals("fcm_tok_alice_123", fakeApi.registeredToken?.token)

    repository.signOut(allDevices = false)
    assertTrue(fakeApi.unregisteredPushTokenCalled)
    assertTrue(db.telefamDao().observeNotifications("user_alice").first().isEmpty())
    assertEquals(0, shadowOf(nm).allNotifications.size)
  }

  @Test
  fun `direct reply encrypts and marks read, and markAsRead honors showReadReceipts privacy`() = runBlocking {
    val bob = SignalE2eeManager("user_bob")
    fakeApi.peerBundle = bob.exportPublicBundle()

    db.telefamDao().upsertConversation(sampleConv("conv_act", "user_bob", "Bob").copy(unreadCount = 2, showReadReceipts = false))
    db.telefamDao().upsertChatMessage(
      ChatMessageEntity(
        messageId = "msg_in_priv",
        conversationId = "conv_act",
        senderUserId = "user_bob",
        recipientUserId = "user_alice",
        clientOpId = "op_in_priv",
        messageType = "text",
        plainText = "Ping",
        mediaUrl = "",
        mediaLocalPath = "",
        mediaId = "",
        voiceDurationMs = 0L,
        viewOnceTtlSec = 0,
        viewOnceOpenedAtMs = 0L,
        viewOnceExpiresAtMs = 0L,
        forwardedFromUserId = "",
        deliveryStatus = "sent",
        syncState = "synced",
        editedAtMs = 0L,
        isPendingUnsend = false,
        createdAtMs = 1000L,
      ),
    )

    // With showReadReceipts = false: markConversationReadFromNotification must NOT send "seen"; must send "delivered" + mark_read
    assertTrue(repository.chatRepository.markConversationReadFromNotification("conv_act"))
    assertEquals(0, db.telefamDao().getConversation("conv_act")?.unreadCount)
    assertTrue(fakeApi.sentReceipts.none { it.second == "seen" })
    assertTrue(fakeApi.sentReceipts.any { it.first == "msg_in_priv" && it.second == "delivered" })
    assertTrue(fakeApi.batchActions.any { it.action == "mark_read" && it.conversationIds.contains("conv_act") })

    // Direct reply via notification helper sends encrypted message
    fakeApi.sentMessages.clear()
    assertTrue(repository.chatRepository.sendQuickReplyFromNotification("conv_act", "user_bob", "On my way!"))
    assertEquals(1, fakeApi.sentMessages.size)
    assertFalse(fakeApi.sentMessages.first().ciphertextB64.contains("On my way!"))

    // Verify RemoteInput extraction works with TelefamNotificationManager keys
    val inputIntent = Intent()
    val bundle = Bundle().apply { putCharSequence(TelefamNotificationManager.KEY_DIRECT_REPLY_TEXT, "Reply from shade") }
    RemoteInput.addResultsToIntent(
      arrayOf(RemoteInput.Builder(TelefamNotificationManager.KEY_DIRECT_REPLY_TEXT).build()),
      inputIntent,
      bundle,
    )
    val extracted = RemoteInput.getResultsFromIntent(inputIntent)
      ?.getCharSequence(TelefamNotificationManager.KEY_DIRECT_REPLY_TEXT)
      ?.toString()
    assertEquals("Reply from shade", extracted)
  }

  private fun sampleConv(id: String, peerId: String, name: String) = ChatConversationEntity(
    conversationId = id,
    peerUserId = peerId,
    peerFullName = name,
    peerUsername = name.lowercase(),
    peerAvatarUrl = "",
    lastMessageId = "",
    lastMessageType = "",
    lastSenderId = "",
    lastMessagePreviewPlain = "",
    lastDeliveryStatus = "sent",
    lastMessageAtMs = 1_700_000_000_000L,
    unreadCount = 0,
    isArchived = false,
    isMuted = false,
    isDeletedLocally = false,
    isOnline = true,
    lastSeenMs = 0L,
    lastSeenLabel = "online",
    canCall = true,
    canScreenshot = true,
    canShareChats = true,
    canCopyMessages = true,
    canDownloadMedia = true,
    showReadReceipts = true,
    showTypingIndicator = true,
    showOnlineIndicator = true,
    isBlocked = false,
    draftText = "",
    updatedAtMs = 1_700_000_000_000L,
  )

  @Suppress("DEPRECATION")
  private fun setNetworkOnline(online: Boolean) {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val shadowCm = shadowOf(cm)
    val activeInfo: android.net.NetworkInfo? = cm.activeNetworkInfo
    if (!online) {
      shadowCm.setDefaultNetworkActive(false)
      val emptyCaps = ShadowNetworkCapabilities.newInstance()
      shadowCm.setNetworkCapabilities(cm.activeNetwork, emptyCaps)
      if (activeInfo != null) {
        shadowOf(activeInfo).setConnectionStatus(android.net.NetworkInfo.State.DISCONNECTED)
      }
    } else {
      val caps = ShadowNetworkCapabilities.newInstance()
      shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
      shadowOf(caps).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
      shadowCm.setNetworkCapabilities(cm.activeNetwork, caps)
      shadowCm.setDefaultNetworkActive(true)
      if (activeInfo != null) {
        shadowOf(activeInfo).setConnectionStatus(android.net.NetworkInfo.State.CONNECTED)
      }
    }
  }
}

private class FakeNotifAndE2eeApi : AbstractTestChatApiStub() {
  var peerBundle: telefam.company.chat.SignalPublicBundleSnapshot? = null
  var uploadedBundle: telefam.company.data.network.E2eeKeyBundleUploadDto? = null
  var registeredToken: RegisterPushTokenRequestDto? = null
  var unregisteredPushTokenCalled = false
  val sentReceipts = mutableListOf<Pair<String, String>>()
  val sentMessages = mutableListOf<SendChatMessageRequestDto>()
  val batchActions = mutableListOf<BatchConversationActionRequestDto>()
  val markReadCalls = mutableListOf<MarkNotificationReadRequestDto>()

  override suspend fun uploadE2eeKeys(
    bearer: String,
    deviceFingerprint: String,
    body: telefam.company.data.network.E2eeKeyBundleUploadDto,
  ): Response<GenericSuccessDto> {
    uploadedBundle = body
    return Response.success(GenericSuccessDto(success = true))
  }

  override suspend fun fetchPeerE2eeKeys(
    bearer: String,
    deviceFingerprint: String,
    userId: String,
  ): Response<E2eeKeyBundleResponseDto> {
    val b = peerBundle ?: return super.fetchPeerE2eeKeys(bearer, deviceFingerprint, userId)
    return Response.success(
      E2eeKeyBundleResponseDto(
        success = true,
        userId = userId,
        deviceId = 1,
        registrationId = b.registrationId,
        identityPubB64 = b.identityPubB64,
        signedPreKeyId = b.signedPreKeyId,
        signedPreKeyPubB64 = b.signedPreKeyPubB64,
        signedPreKeySigB64 = b.signedPreKeySigB64,
        kyberPreKeyId = b.kyberPreKeyId,
        kyberPreKeyPubB64 = b.kyberPreKeyPubB64,
        kyberPreKeySigB64 = b.kyberPreKeySigB64,
        preKeyId = b.oneTimePreKeys.firstOrNull()?.first ?: 0,
        preKeyPubB64 = b.oneTimePreKeys.firstOrNull()?.second.orEmpty(),
      ),
    )
  }

  override suspend fun sendChatMessage(
    bearer: String,
    deviceFingerprint: String,
    conversationId: String,
    body: SendChatMessageRequestDto,
  ): Response<ChatMessageSingleResponseDto> {
    sentMessages.add(body)
    return Response.success(
      ChatMessageSingleResponseDto(
        success = true,
        message = ChatMessageDto(
          messageId = body.clientOpId,
          conversationId = conversationId,
          senderUserId = "user_alice",
          recipientUserId = "user_bob",
          clientOpId = body.clientOpId,
          messageType = body.messageType,
          compressionCodec = body.compressionCodec,
          encryptionScheme = body.encryptionScheme,
          ciphertextB64 = body.ciphertextB64,
          senderCiphertextB64 = body.senderCiphertextB64,
          signalMsgType = body.signalMsgType,
          deliveryStatus = "sent",
          createdAtMs = System.currentTimeMillis(),
        ),
      ),
    )
  }

  override suspend fun sendChatReceipt(
    bearer: String,
    deviceFingerprint: String,
    messageId: String,
    body: ChatReceiptRequestDto,
  ): Response<ChatMessageSingleResponseDto> {
    sentReceipts.add(messageId to body.status)
    return Response.success(ChatMessageSingleResponseDto(success = true, message = ChatMessageDto(messageId = messageId, deliveryStatus = body.status)))
  }

  override suspend fun batchConversationAction(
    bearer: String,
    deviceFingerprint: String,
    body: BatchConversationActionRequestDto,
  ): Response<ChatConversationsResponseDto> {
    batchActions.add(body)
    return Response.success(ChatConversationsResponseDto(success = true))
  }

  override suspend fun registerPushToken(
    bearer: String,
    deviceFingerprint: String,
    body: RegisterPushTokenRequestDto,
  ): Response<GenericSuccessDto> {
    registeredToken = body
    return Response.success(GenericSuccessDto(success = true))
  }

  override suspend fun unregisterPushToken(
    bearer: String,
    deviceFingerprint: String,
    allDevices: Boolean,
  ): Response<GenericSuccessDto> {
    unregisteredPushTokenCalled = true
    return Response.success(GenericSuccessDto(success = true))
  }

  override suspend fun markNotificationRead(
    bearer: String,
    deviceFingerprint: String,
    body: MarkNotificationReadRequestDto,
  ): Response<NotificationUnreadCountResponseDto> {
    markReadCalls.add(body)
    return Response.success(NotificationUnreadCountResponseDto(success = true))
  }
}
