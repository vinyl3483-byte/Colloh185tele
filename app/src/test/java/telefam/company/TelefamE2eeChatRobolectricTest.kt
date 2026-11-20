package telefam.company

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetworkCapabilities
import retrofit2.Response
import telefam.company.chat.PeerBundleInput
import telefam.company.chat.SignalE2eeManager
import telefam.company.chat.TelefamChatUiState
import telefam.company.core.ChatAudienceOption
import telefam.company.core.ChatPrivacySettingsState
import telefam.company.data.local.ChatConversationEntity
import telefam.company.data.local.ChatMessageEntity
import telefam.company.data.local.DeviceIdentityStore
import telefam.company.data.local.TelefamDatabase
import telefam.company.data.network.BatchConversationActionRequestDto
import telefam.company.data.network.ChatConversationDto
import telefam.company.data.network.ChatConversationsResponseDto
import telefam.company.data.network.ChatMessageDto
import telefam.company.data.network.ChatMessageSingleResponseDto
import telefam.company.data.network.ChatMessagesListResponseDto
import telefam.company.data.network.ChatPeerCapabilitiesDto
import telefam.company.data.network.E2eeKeyBundleResponseDto
import telefam.company.data.network.EditChatMessageRequestDto
import telefam.company.data.network.GenericSuccessDto
import telefam.company.data.network.NetworkQualityMonitor
import telefam.company.data.network.OpenDirectConversationResponseDto
import telefam.company.data.network.RecordSecurityFlagRequestDto
import telefam.company.data.network.SecurityFlagResponseDto
import telefam.company.data.network.SendChatMessageRequestDto
import telefam.company.data.network.ViewOnceOpenResponseDto
import telefam.company.data.repository.TelefamRepository
import telefam.company.ui.screens.TelefamHomeScreen
import telefam.company.ui.screens.TelefamOneOnOneChatScreen
import telefam.company.ui.theme.TelefamTheme
import telefam.company.ui.theme.ThemeMode
import telefam.company.ui.viewmodel.HomeTab
import telefam.company.ui.viewmodel.TelefamUiState
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h915dp")
class TelefamE2eeChatRobolectricTest {

  @get:Rule
  val composeRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var db: TelefamDatabase
  private lateinit var identityStore: DeviceIdentityStore
  private lateinit var networkMonitor: NetworkQualityMonitor
  private lateinit var fakeApi: FakeChatFlowApiService
  private lateinit var repository: TelefamRepository

  @Before
  fun setup() = runBlocking {
    context = ApplicationProvider.getApplicationContext()
    setNetworkOnline(true)
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
    fakeApi = FakeChatFlowApiService()
    repository = TelefamRepository(
      appContext = context,
      dao = db.telefamDao(),
      identityStore = identityStore,
      networkMonitor = networkMonitor,
      apiService = fakeApi,
    )
  }

  @After
  fun tearDown() {
    shadowOf(Looper.getMainLooper()).idle()
    Thread.sleep(50L)
    shadowOf(Looper.getMainLooper()).idle()
    db.close()
  }

  @Test
  fun `libsignal X3DH PQXDH compress encrypt decrypt and self-history seal roundtrip`() {
    val alice = SignalE2eeManager("user_alice")
    val bob = SignalE2eeManager("user_bob")
    val bobSnap = bob.exportPublicBundle()

    alice.initSessionWithPeer(
      PeerBundleInput(
        peerUserId = "user_bob",
        deviceId = 1,
        registrationId = bobSnap.registrationId,
        identityPubB64 = bobSnap.identityPubB64,
        signedPreKeyId = bobSnap.signedPreKeyId,
        signedPreKeyPubB64 = bobSnap.signedPreKeyPubB64,
        signedPreKeySigB64 = bobSnap.signedPreKeySigB64,
        kyberPreKeyId = bobSnap.kyberPreKeyId,
        kyberPreKeyPubB64 = bobSnap.kyberPreKeyPubB64,
        kyberPreKeySigB64 = bobSnap.kyberPreKeySigB64,
        preKeyId = bobSnap.oneTimePreKeys.first().first,
        preKeyPubB64 = bobSnap.oneTimePreKeys.first().second,
      ),
    )

    val longPlain = "Good morning Bob! It's morning in Tokyo. " + "Encrypted compressed message payload. ".repeat(12)
    val envelope = alice.compressAndEncrypt("user_bob", longPlain)
    assertEquals("zlib", envelope.compressionCodec)
    assertEquals("libsignal_v1", envelope.encryptionScheme)
    assertTrue(envelope.ciphertextB64.isNotBlank())
    assertTrue(envelope.senderCiphertextB64.isNotBlank())

    // Recipient Bob decrypts the Double Ratchet PreKeySignalMessage
    val bobDecoded = bob.decryptAndDecompress(
      peerUserId = "user_alice",
      isOutgoingFromSelf = false,
      ciphertextB64 = envelope.ciphertextB64,
      senderCiphertextB64 = envelope.senderCiphertextB64,
      signalMsgType = envelope.signalMsgType,
      compressionCodec = envelope.compressionCodec,
    )
    assertEquals(longPlain, bobDecoded)

    // Restored Alice state decrypts own history via EC seal without needing Bob's ratchet
    val aliceRestored = SignalE2eeManager("user_alice", alice.exportStateJson("user_alice"))
    val aliceSelfDecoded = aliceRestored.decryptAndDecompress(
      peerUserId = "user_bob",
      isOutgoingFromSelf = true,
      ciphertextB64 = envelope.ciphertextB64,
      senderCiphertextB64 = envelope.senderCiphertextB64,
      signalMsgType = envelope.signalMsgType,
      compressionCodec = envelope.compressionCodec,
    )
    assertEquals(longPlain, aliceSelfDecoded)
  }

  @Test
  fun `offline-first Room send queues locally then syncs edit and unsend leaves zero traces`() = runTest {
    val bob = SignalE2eeManager("user_bob")
    fakeApi.peerBundle = bob.exportPublicBundle()

    val conv = sampleConversation("conv_1", "user_bob", "GORI GOR")
    db.telefamDao().upsertConversation(conv)
    fakeApi.conversations["conv_1"] = ChatConversationDto(
      conversationId = "conv_1",
      peerUserId = "user_bob",
      peerFullName = "GORI GOR",
      peerUsername = "gorigor",
      permissions = ChatPeerCapabilitiesDto(),
    )

    // Go offline before sending
    setNetworkOnline(false)
    networkMonitor.refreshStatus()

    val chatRepo = repository.chatRepository
    val sentLocal = requireNotNull(chatRepo.sendTextMessage("conv_1", "user_bob", "Meet at Tokyo station"))
    assertEquals("pending", sentLocal.syncState)
    assertEquals("sending", sentLocal.deliveryStatus)
    assertEquals("Meet at Tokyo station", db.telefamDao().getChatMessage(sentLocal.messageId)?.plainText)
    assertTrue(db.telefamDao().countPendingChatOps() >= 1)

    // Reconnect and flush outbox
    setNetworkOnline(true)
    networkMonitor.refreshStatus()
    chatRepo.flushPendingChatOps()

    assertEquals(0, db.telefamDao().countPendingChatOps())
    assertEquals(1, fakeApi.sentMessages.size)
    // Verify ciphertext sent to server is encrypted, NOT plaintext
    assertFalse(fakeApi.sentMessages.first().ciphertextB64.contains("Tokyo"))
    val afterSync = db.telefamDao().getChatMessage(sentLocal.messageId)
    assertNotNull(afterSync)
    assertEquals("synced", afterSync?.syncState)
    assertEquals("sent", afterSync?.deliveryStatus)

    // Edit while online
    chatRepo.editMessage(sentLocal.messageId, "conv_1", "user_bob", "Updated Tokyo time 09:30")
    val afterEdit = db.telefamDao().getChatMessage(sentLocal.messageId)
    assertEquals("Updated Tokyo time 09:30", afterEdit?.plainText)
    assertTrue((afterEdit?.editedAtMs ?: 0L) > 0L)
    assertEquals(1, fakeApi.editCount)

    // Unsend removes all local and server traces
    chatRepo.unsendMessage(sentLocal.messageId, "conv_1")
    assertNull(db.telefamDao().getChatMessage(sentLocal.messageId))
    assertEquals(1, fakeApi.unsendCount)
    assertTrue(fakeApi.sentMessages.none { it.clientOpId == sentLocal.messageId })
  }

  @Test
  fun `view-once voice blocks sender open, allows receiver open and purges all traces while preserving security flag`() = runTest {
    val conv = sampleConversation("conv_vo", "user_bob", "GORI GOR")
    db.telefamDao().upsertConversation(conv)

    val senderFile = File(context.cacheDir, "sender_vo.m4a").apply { writeText("audio") }
    val senderMsg = ChatMessageEntity(
      messageId = "msg_vo_own",
      conversationId = "conv_vo",
      senderUserId = "user_alice",
      recipientUserId = "user_bob",
      clientOpId = "op_own",
      messageType = "voice_once",
      plainText = "",
      mediaUrl = "",
      mediaLocalPath = senderFile.absolutePath,
      mediaId = "m_own",
      voiceDurationMs = 4500L,
      viewOnceTtlSec = 10,
      viewOnceOpenedAtMs = 0L,
      viewOnceExpiresAtMs = 0L,
      forwardedFromUserId = "",
      deliveryStatus = "delivered",
      syncState = "synced",
      editedAtMs = 0L,
      isPendingUnsend = false,
      createdAtMs = 1000L,
    )
    db.telefamDao().upsertChatMessage(senderMsg)

    val chatRepo = repository.chatRepository
    // Sender cannot open own view-once
    assertNull(chatRepo.openViewOnceVoice("msg_vo_own", "conv_vo"))

    // Incoming view-once from Bob CAN be opened by Alice
    val incomingFile = File(context.cacheDir, "incoming_vo.m4a").apply { writeText("secret_voice") }
    val incomingMsg = senderMsg.copy(
      messageId = "msg_vo_in",
      senderUserId = "user_bob",
      recipientUserId = "user_alice",
      clientOpId = "op_in",
      mediaLocalPath = incomingFile.absolutePath,
      mediaId = "m_in",
    )
    db.telefamDao().upsertChatMessage(incomingMsg)

    val opened = chatRepo.openViewOnceVoice("msg_vo_in", "conv_vo")
    assertNotNull(opened)
    assertTrue((opened?.viewOnceOpenedAtMs ?: 0L) > 0L)

    // Security flag persisted in Room and synced
    assertTrue(fakeApi.recordedFlags.any { it.flagType == "view_once_opened" })

    // Burn consumes & deletes message + local voice file permanently
    chatRepo.consumeViewOnceVoice("msg_vo_in", "conv_vo")
    assertNull(db.telefamDao().getChatMessage("msg_vo_in"))
    assertFalse(incomingFile.exists())
    assertTrue(fakeApi.consumedViewOnceIds.contains("msg_vo_in"))
  }

  @Test
  fun `persistent security flags survive Room database close and reopen`() = runBlocking {
    val dbName = "telefam_flag_persist_${UUID.randomUUID()}"
    var diskDb = Room.databaseBuilder(context, TelefamDatabase::class.java, dbName)
      .allowMainThreadQueries()
      .build()
    try {
      val repo = TelefamRepository(
        appContext = context,
        dao = diskDb.telefamDao(),
        identityStore = identityStore,
        networkMonitor = networkMonitor,
        apiService = null,
      )
      repo.chatRepository.recordSecurityFlag(
        conversationId = "conv_sec",
        messageId = "msg_sec",
        flagType = "screenshot_disallowed",
        details = "Screenshot capture attempt detected while screenshots are restricted",
      )
      diskDb.close()

      // Reopen fresh instance simulating app restart
      diskDb = Room.databaseBuilder(context, TelefamDatabase::class.java, dbName)
        .allowMainThreadQueries()
        .build()
      val persisted = diskDb.telefamDao().getUnsyncedSecurityFlags()
      assertEquals(1, persisted.size)
      assertEquals("screenshot_disallowed", persisted.first().flagType)
      assertEquals("conv_sec", persisted.first().conversationId)
    } finally {
      diskDb.close()
      context.deleteDatabase(dbName)
    }
  }

  @Test
  fun `chat screen hides copy and forward when peer or owner restricts them`() {
    val restrictedConv = sampleConversation(
      id = "conv_priv",
      peerId = "user_bob",
      peerName = "GORI GOR",
      canCopy = false,
      canShare = false,
    )
    val msg = ChatMessageEntity(
      messageId = "m_own_1",
      conversationId = "conv_priv",
      senderUserId = "user_alice",
      recipientUserId = "user_bob",
      clientOpId = "m_own_1",
      messageType = "text",
      plainText = "Confidential note",
      mediaUrl = "",
      mediaLocalPath = "",
      mediaId = "",
      voiceDurationMs = 0L,
      viewOnceTtlSec = 0,
      viewOnceOpenedAtMs = 0L,
      viewOnceExpiresAtMs = 0L,
      forwardedFromUserId = "",
      deliveryStatus = "delivered",
      syncState = "synced",
      editedAtMs = 0L,
      isPendingUnsend = false,
      createdAtMs = System.currentTimeMillis(),
    )

    var copyAllowed by mutableStateOf(ChatAudienceOption.NOBODY)
    var convState by mutableStateOf(restrictedConv)

    composeRule.setContent {
      TelefamTheme(themeMode = ThemeMode.DARK) {
        TelefamOneOnOneChatScreen(
          conversation = convState,
          chatState = TelefamChatUiState(
            activeConversationId = convState.conversationId,
            conversations = listOf(convState),
            activeMessages = listOf(msg),
          ),
          chatPrivacy = ChatPrivacySettingsState(
            whoCanCopyMessages = copyAllowed,
            whoCanShareChats = copyAllowed,
          ),
          currentUserId = "user_alice",
          onBack = {},
          onOpenPeerProfile = {},
          onComposerTextChanged = {},
          onSendTextOrEdit = {},
          onSendGreetingSticker = {},
          onStartEditMessage = {},
          onCancelEditMessage = {},
          onUnsendMessage = {},
          onDeleteMessageForSelf = {},
          onStartForwardMessage = {},
          onDismissForwardMessage = {},
          onConfirmForwardMessage = {},
          onStartVoiceRecording = {},
          onStopVoiceRecording = {},
          onDiscardVoiceRecording = {},
          onToggleVoicePlayback = { _, _ -> },
          onToggleVoicePreviewViewOnce = {},
          onSelectVoicePreviewTtl = {},
          onSendVoiceNote = {},
          onOpenViewOnceVoice = {},
          onBurnViewOnceVoice = {},
          onOpenGiflyPicker = {},
          onCloseGiflyPicker = {},
          onSelectGiflyTab = {},
          onGiflyQueryChanged = {},
          onLoadMoreGifly = {},
          onPreviewGiflyItem = {},
          onSendPreviewGifly = {},
          onToggleMuteConversation = {},
          onBlockPeer = {},
          onReportPeer = { _, _ -> },
          onOpenSharedMediaTab = {},
          onTriggerComingSoon = { _, _ -> },
          onDismissToast = {},
        )
      }
    }

    composeRule.onNodeWithTag("chat_msg_bubble_m_own_1").performTouchInput { longClick() }
    composeRule.onNodeWithTag("msg_action_edit").assertIsDisplayed()
    composeRule.onNodeWithTag("msg_action_unsend").assertIsDisplayed()
    composeRule.onNodeWithTag("msg_action_delete").assertIsDisplayed()
    assertTrue(composeRule.onAllNodesWithTag("msg_action_copy").fetchSemanticsNodes().isEmpty())
    assertTrue(composeRule.onAllNodesWithTag("msg_action_forward").fetchSemanticsNodes().isEmpty())

    // Dismiss menu and enable Copy + Forward privacy
    composeRule.onNodeWithTag("msg_action_delete").performClick()
    composeRule.runOnIdle {
      copyAllowed = ChatAudienceOption.ANYONE
      convState = convState.copy(canCopyMessages = true, canShareChats = true)
    }
    composeRule.onNodeWithTag("chat_msg_bubble_m_own_1").performTouchInput { longClick() }
    composeRule.onNodeWithTag("msg_action_copy").assertIsDisplayed()
    composeRule.onNodeWithTag("msg_action_forward").assertIsDisplayed()
  }

  @Test
  fun `composer hides gift and file on typing, switches mic to send, and shows blocked notice below composer`() {
    var composerText by mutableStateOf("")
    var isBlocked by mutableStateOf(false)
    val baseConv = sampleConversation("conv_c", "user_bob", "GORI GOR")

    composeRule.setContent {
      TelefamTheme(themeMode = ThemeMode.DARK) {
        TelefamOneOnOneChatScreen(
          conversation = baseConv.copy(isBlocked = isBlocked),
          chatState = TelefamChatUiState(
            activeConversationId = baseConv.conversationId,
            conversations = listOf(baseConv.copy(isBlocked = isBlocked)),
            composerText = composerText,
          ),
          chatPrivacy = ChatPrivacySettingsState(),
          currentUserId = "user_alice",
          onBack = {},
          onOpenPeerProfile = {},
          onComposerTextChanged = { composerText = it },
          onSendTextOrEdit = {},
          onSendGreetingSticker = {},
          onStartEditMessage = {},
          onCancelEditMessage = {},
          onUnsendMessage = {},
          onDeleteMessageForSelf = {},
          onStartForwardMessage = {},
          onDismissForwardMessage = {},
          onConfirmForwardMessage = {},
          onStartVoiceRecording = {},
          onStopVoiceRecording = {},
          onDiscardVoiceRecording = {},
          onToggleVoicePlayback = { _, _ -> },
          onToggleVoicePreviewViewOnce = {},
          onSelectVoicePreviewTtl = {},
          onSendVoiceNote = {},
          onOpenViewOnceVoice = {},
          onBurnViewOnceVoice = {},
          onOpenGiflyPicker = {},
          onCloseGiflyPicker = {},
          onSelectGiflyTab = {},
          onGiflyQueryChanged = {},
          onLoadMoreGifly = {},
          onPreviewGiflyItem = {},
          onSendPreviewGifly = {},
          onToggleMuteConversation = {},
          onBlockPeer = {},
          onReportPeer = { _, _ -> },
          onOpenSharedMediaTab = {},
          onTriggerComingSoon = { _, _ -> },
          onDismissToast = {},
        )
      }
    }

    // Empty state: Mic, Gift, Attach visible
    composeRule.onNodeWithTag("chat_mic_button").assertIsDisplayed()
    composeRule.onNodeWithTag("chat_gift_button").assertIsDisplayed()
    composeRule.onNodeWithTag("chat_attach_button").assertIsDisplayed()
    assertTrue(composeRule.onAllNodesWithTag("chat_blocked_notice").fetchSemanticsNodes().isEmpty())

    // Start typing: Send appears, Gift & Attach disappear
    composeRule.runOnIdle { composerText = "Hello" }
    composeRule.onNodeWithTag("chat_send_button").assertIsDisplayed()
    assertTrue(composeRule.onAllNodesWithTag("chat_gift_button").fetchSemanticsNodes().isEmpty())
    assertTrue(composeRule.onAllNodesWithTag("chat_attach_button").fetchSemanticsNodes().isEmpty())

    // Blocked state: exact notice shown below composer and input disabled
    composeRule.runOnIdle {
      composerText = ""
      isBlocked = true
    }
    composeRule.onNodeWithTag("chat_blocked_notice").assertIsDisplayed()
    composeRule.onNodeWithText("You cannot message this user").assertIsDisplayed()
    assertTrue(composeRule.onAllNodesWithTag("chat_message_input").fetchSemanticsNodes().isEmpty())
  }

  @Test
  @Config(qualifiers = "w840dp-h900dp")
  fun `wide screen renders split-screen chats layout with batch select and undo bar`() {
    val conv1 = sampleConversation("conv_w1", "user_bob", "GORI GOR").copy(unreadCount = 3, lastMessagePreviewPlain = "Hey there")
    val conv2 = sampleConversation("conv_w2", "user_claire", "Claire").copy(unreadCount = 0, lastMessagePreviewPlain = "See you soon")

    var chatState by mutableStateOf(
      TelefamChatUiState(
        conversations = listOf(conv1, conv2),
      ),
    )

    composeRule.setContent {
      TelefamTheme(themeMode = ThemeMode.LIGHT) {
        TelefamHomeScreen(
          uiState = TelefamUiState(
            userId = "user_alice",
            isAuthenticated = true,
            profileCompleted = true,
            activeHomeTab = HomeTab.CHATS,
            chatState = chatState,
          ),
          onSelectTab = {},
          onTriggerComingSoon = { _, _ -> },
          onDismissComingSoon = {},
          onToggleTheme = {},
          onEnterChatMultiSelect = { id ->
            chatState = chatState.copy(isMultiSelectMode = true, selectedConversationIds = setOf(id))
          },
          onToggleChatSelection = { id ->
            val next = if (chatState.selectedConversationIds.contains(id)) chatState.selectedConversationIds - id else chatState.selectedConversationIds + id
            chatState = chatState.copy(isMultiSelectMode = next.isNotEmpty(), selectedConversationIds = next)
          },
          onBatchChatAction = { action ->
            chatState = chatState.copy(
              isMultiSelectMode = false,
              selectedConversationIds = emptySet(),
              conversations = chatState.conversations.filterNot { it.conversationId == "conv_w1" },
              undoBatchState = telefam.company.chat.UndoChatBatchState(action, "Archived 1 chat(s)", listOf(conv1)),
            )
          },
          onUndoChatBatch = {
            chatState = chatState.copy(
              conversations = listOf(conv1, conv2),
              undoBatchState = null,
            )
          },
        )
      }
    }

    composeRule.onNodeWithTag("chats_split_layout").assertIsDisplayed()
    composeRule.onNodeWithTag("chats_split_detail_pane").assertIsDisplayed()
    composeRule.onNodeWithTag("conv_row_conv_w1").assertIsDisplayed()
    composeRule.onNodeWithTag("conv_unread_conv_w1", useUnmergedTree = true).assertIsDisplayed()

    composeRule.onNodeWithTag("conv_row_conv_w1").performTouchInput { longClick() }
    composeRule.onNodeWithTag("chat_multiselect_bar").assertIsDisplayed()
    composeRule.onNodeWithTag("batch_archive").performClick()

    composeRule.onNodeWithTag("chat_undo_snackbar").assertIsDisplayed()
    assertTrue(composeRule.onAllNodesWithTag("conv_row_conv_w1").fetchSemanticsNodes().isEmpty())

    composeRule.onNodeWithTag("chat_undo_button").performClick()
    composeRule.onNodeWithTag("conv_row_conv_w1").assertIsDisplayed()
  }

  @Test
  fun `background message sync preserves unread count and sends delivered while opening chat marks seen or falls back to mark_read`() = runTest {
    val conv = sampleConversation("conv_receipts", "user_bob", "GORI GOR")
    db.telefamDao().upsertConversation(conv)
    val incomingDto = ChatMessageDto(
      messageId = "msg_in_1",
      conversationId = "conv_receipts",
      senderUserId = "user_bob",
      recipientUserId = "user_alice",
      clientOpId = "op_in_1",
      messageType = "text",
      ciphertextB64 = "",
      deliveryStatus = "sent",
      createdAtMs = 2_000_000L,
    )
    fakeApi.conversations["conv_receipts"] = ChatConversationDto(
      conversationId = "conv_receipts",
      peerUserId = "user_bob",
      peerFullName = "GORI GOR",
      unreadCount = 2,
      permissions = ChatPeerCapabilitiesDto(showReadReceipts = true),
    )
    fakeApi.messagesByConv["conv_receipts"] = mutableListOf(incomingDto)

    val chatRepo = repository.chatRepository

    // Background sync (markAsRead = false) must NOT zero unreadCount and must only ack delivered
    chatRepo.loadConversationMessages("conv_receipts", markAsRead = false)
    val afterBgConv = db.telefamDao().getConversation("conv_receipts")
    assertEquals(2, afterBgConv?.unreadCount)
    assertEquals(listOf("msg_in_1" to "delivered"), fakeApi.sentReceipts.toList())
    assertEquals("delivered", db.telefamDao().getChatMessage("msg_in_1")?.deliveryStatus)

    // Opening chat with showReadReceipts = false must send mark_read batch action instead of seen receipt
    fakeApi.sentReceipts.clear()
    fakeApi.batchActions.clear()
    fakeApi.conversations["conv_receipts"] = fakeApi.conversations["conv_receipts"]!!.copy(
      permissions = ChatPeerCapabilitiesDto(showReadReceipts = false),
    )
    chatRepo.loadConversationMessages("conv_receipts", markAsRead = true)
    assertEquals(0, db.telefamDao().getConversation("conv_receipts")?.unreadCount)
    assertTrue(fakeApi.sentReceipts.none { it.second == "seen" })
    assertTrue(fakeApi.batchActions.any { it.action == "mark_read" && it.conversationIds.contains("conv_receipts") })

    // Opening chat with showReadReceipts = true sends seen receipt
    fakeApi.conversations["conv_receipts"] = fakeApi.conversations["conv_receipts"]!!.copy(
      permissions = ChatPeerCapabilitiesDto(showReadReceipts = true),
    )
    chatRepo.loadConversationMessages("conv_receipts", markAsRead = true)
    assertTrue(fakeApi.sentReceipts.any { it.first == "msg_in_1" && it.second == "seen" })
    assertEquals("seen", db.telefamDao().getChatMessage("msg_in_1")?.deliveryStatus)
  }

  @Test
  fun `resolveLocalVoiceFilePath downloads authenticated voice notes and sniffs WAV vs M4A`() = runBlocking {
    val server = java.net.ServerSocket(0)
    val wavBytes = "RIFF".toByteArray() + ByteArray(64) { 0x11 }
    val m4aBytes = byteArrayOf(0x00, 0x00, 0x00, 0x20, 'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte()) + ByteArray(64) { 0x22 }
    val authHeadersSeen = mutableListOf<String>()
    val thread = kotlin.concurrent.thread(start = true) {
      repeat(2) { idx ->
        server.accept().use { sock ->
          val reader = java.io.BufferedReader(java.io.InputStreamReader(sock.getInputStream()))
          while (true) {
            val line = reader.readLine() ?: break
            if (line.isEmpty()) break
            if (line.startsWith("Authorization:", ignoreCase = true)) {
              authHeadersSeen += line.substringAfter(":").trim()
            }
          }
          val payload = if (idx == 0) wavBytes else m4aBytes
          val hdr = "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: ${payload.size}\r\nConnection: close\r\n\r\n"
          sock.getOutputStream().write(hdr.toByteArray(Charsets.UTF_8))
          sock.getOutputStream().write(payload)
          sock.getOutputStream().flush()
        }
      }
    }

    try {
      db.telefamDao().upsertChatMessage(
        ChatMessageEntity(
          messageId = "msg_wav_1",
          conversationId = "conv_v",
          senderUserId = "user_bob",
          recipientUserId = "user_alice",
          clientOpId = "op_wav_1",
          messageType = "voice",
          plainText = "",
          mediaUrl = "http://127.0.0.1:${server.localPort}/v1/media/voice1",
          mediaLocalPath = "",
          mediaId = "vm_wav",
          voiceDurationMs = 2000L,
          viewOnceTtlSec = 0,
          viewOnceOpenedAtMs = 0L,
          viewOnceExpiresAtMs = 0L,
          forwardedFromUserId = "",
          deliveryStatus = "delivered",
          syncState = "synced",
          editedAtMs = 0L,
          isPendingUnsend = false,
          createdAtMs = 1000L,
        ),
      )

      val chatRepo = repository.chatRepository
      val wavPath = chatRepo.resolveLocalVoiceFilePath(
        messageId = "msg_wav_1",
        localPath = "",
        remoteUrl = "http://127.0.0.1:${server.localPort}/v1/media/voice1",
      )
      assertNotNull(wavPath)
      assertTrue(wavPath!!.endsWith(".wav"))
      assertTrue(File(wavPath).exists())
      assertEquals(wavPath, db.telefamDao().getChatMessage("msg_wav_1")?.mediaLocalPath)

      val m4aPath = chatRepo.resolveLocalVoiceFilePath(
        messageId = "msg_m4a_1",
        localPath = "",
        remoteUrl = "http://127.0.0.1:${server.localPort}/v1/media/voice2",
      )
      assertNotNull(m4aPath)
      assertTrue(m4aPath!!.endsWith(".m4a"))
      assertTrue(File(m4aPath).exists())
      assertTrue(authHeadersSeen.all { it == "Bearer acc_alice" })
    } finally {
      thread.join(3_000L)
      server.close()
    }
  }

  @Test
  fun `signOut clears Room chat tables cached voices and identity session`() = runTest {
    val conv = sampleConversation("conv_out", "user_bob", "GORI GOR")
    db.telefamDao().upsertConversation(conv)
    val voiceCacheDir = File(context.cacheDir, "telefam_voice_notes").apply { mkdirs() }
    val cachedVoice = File(voiceCacheDir, "vn_test_out.m4a").apply { writeText("voice_data") }
    db.telefamDao().upsertChatMessage(
      ChatMessageEntity(
        messageId = "msg_out_1",
        conversationId = "conv_out",
        senderUserId = "user_alice",
        recipientUserId = "user_bob",
        clientOpId = "op_out_1",
        messageType = "voice",
        plainText = "secret",
        mediaUrl = "",
        mediaLocalPath = cachedVoice.absolutePath,
        mediaId = "m_out",
        voiceDurationMs = 1000L,
        viewOnceTtlSec = 0,
        viewOnceOpenedAtMs = 0L,
        viewOnceExpiresAtMs = 0L,
        forwardedFromUserId = "",
        deliveryStatus = "sent",
        syncState = "synced",
        editedAtMs = 0L,
        isPendingUnsend = false,
        createdAtMs = 5000L,
      ),
    )

    repository.signOut(allDevices = false)

    assertNull(db.telefamDao().getConversation("conv_out"))
    assertNull(db.telefamDao().getChatMessage("msg_out_1"))
    assertFalse(cachedVoice.exists())
    assertTrue(fakeApi.logoutCalled)
    val snap = identityStore.sessionFlow.first()
    assertNull(snap.accessToken)
    assertNull(snap.userId)
  }

  private fun sampleConversation(
    id: String,
    peerId: String,
    peerName: String,
    canCopy: Boolean = true,
    canShare: Boolean = true,
  ) = ChatConversationEntity(
    conversationId = id,
    peerUserId = peerId,
    peerFullName = peerName,
    peerUsername = peerName.lowercase().replace(" ", ""),
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
    canShareChats = canShare,
    canCopyMessages = canCopy,
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

private class FakeChatFlowApiService : AbstractTestChatApiStub() {
  var peerBundle: telefam.company.chat.SignalPublicBundleSnapshot? = null
  val conversations = linkedMapOf<String, ChatConversationDto>()
  val messagesByConv = linkedMapOf<String, MutableList<ChatMessageDto>>()
  val sentMessages = mutableListOf<SendChatMessageRequestDto>()
  val sentReceipts = mutableListOf<Pair<String, String>>()
  val batchActions = mutableListOf<BatchConversationActionRequestDto>()
  var editCount = 0
  var unsendCount = 0
  var logoutCalled = false
  val recordedFlags = mutableListOf<RecordSecurityFlagRequestDto>()
  val consumedViewOnceIds = mutableListOf<String>()

  override suspend fun logout(
    bearer: String,
    deviceFingerprint: String,
    body: telefam.company.data.network.LogoutRequestDto,
  ): Response<GenericSuccessDto> {
    logoutCalled = true
    return Response.success(GenericSuccessDto(success = true))
  }

  override suspend fun listChatMessages(
    bearer: String,
    deviceFingerprint: String,
    conversationId: String,
    sinceMs: Long?,
  ): Response<ChatMessagesListResponseDto> {
    val conv = conversations[conversationId] ?: ChatConversationDto(conversationId = conversationId)
    val msgs = messagesByConv[conversationId].orEmpty()
    return Response.success(
      ChatMessagesListResponseDto(
        success = true,
        conversation = conv,
        messages = msgs,
      ),
    )
  }

  override suspend fun sendChatReceipt(
    bearer: String,
    deviceFingerprint: String,
    messageId: String,
    body: telefam.company.data.network.ChatReceiptRequestDto,
  ): Response<ChatMessageSingleResponseDto> {
    sentReceipts.add(messageId to body.status)
    messagesByConv.values.flatten().let { all ->
      val idx = all.indexOfFirst { it.messageId == messageId }
      if (idx >= 0) {
        val convId = all[idx].conversationId
        val list = messagesByConv[convId]
        if (list != null) {
          val localIdx = list.indexOfFirst { it.messageId == messageId }
          if (localIdx >= 0) {
            list[localIdx] = list[localIdx].copy(deliveryStatus = body.status)
          }
        }
      }
    }
    return Response.success(
      ChatMessageSingleResponseDto(
        success = true,
        message = ChatMessageDto(messageId = messageId, deliveryStatus = body.status),
      ),
    )
  }

  override suspend fun batchConversationAction(
    bearer: String,
    deviceFingerprint: String,
    body: BatchConversationActionRequestDto,
  ): Response<ChatConversationsResponseDto> {
    batchActions.add(body)
    return Response.success(ChatConversationsResponseDto(success = true, conversations = conversations.values.toList()))
  }

  override suspend fun fetchPeerE2eeKeys(
    bearer: String,
    deviceFingerprint: String,
    userId: String,
  ): Response<E2eeKeyBundleResponseDto> {
    val b = peerBundle
    return if (b != null) {
      Response.success(
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
    } else {
      super.fetchPeerE2eeKeys(bearer, deviceFingerprint, userId)
    }
  }

  override suspend fun listChatConversations(
    bearer: String,
    deviceFingerprint: String,
    archived: Boolean,
  ): Response<ChatConversationsResponseDto> =
    Response.success(ChatConversationsResponseDto(success = true, conversations = conversations.values.toList()))

  override suspend fun openDirectConversation(
    bearer: String,
    deviceFingerprint: String,
    userId: String,
  ): Response<OpenDirectConversationResponseDto> {
    val conv = conversations.values.firstOrNull { it.peerUserId == userId }
      ?: ChatConversationDto(conversationId = "conv_$userId", peerUserId = userId, peerFullName = userId)
    conversations[conv.conversationId] = conv
    return Response.success(OpenDirectConversationResponseDto(success = true, conversation = conv))
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
          recipientUserId = conversations[conversationId]?.peerUserId ?: "user_bob",
          clientOpId = body.clientOpId,
          messageType = body.messageType,
          compressionCodec = body.compressionCodec,
          encryptionScheme = body.encryptionScheme,
          ciphertextB64 = body.ciphertextB64,
          senderCiphertextB64 = body.senderCiphertextB64,
          signalMsgType = body.signalMsgType,
          mediaUrl = body.mediaUrl,
          mediaId = body.mediaId,
          voiceDurationMs = body.voiceDurationMs,
          viewOnceTtlSec = body.viewOnceTtlSec,
          deliveryStatus = "sent",
          createdAtMs = System.currentTimeMillis(),
        ),
      ),
    )
  }

  override suspend fun editChatMessage(
    bearer: String,
    deviceFingerprint: String,
    messageId: String,
    body: EditChatMessageRequestDto,
  ): Response<ChatMessageSingleResponseDto> {
    editCount++
    return Response.success(
      ChatMessageSingleResponseDto(
        success = true,
        message = ChatMessageDto(
          messageId = messageId,
          senderUserId = "user_alice",
          ciphertextB64 = body.ciphertextB64,
          senderCiphertextB64 = body.senderCiphertextB64,
          signalMsgType = body.signalMsgType,
          editedAtMs = System.currentTimeMillis(),
        ),
      ),
    )
  }

  override suspend fun deleteChatMessage(
    bearer: String,
    deviceFingerprint: String,
    messageId: String,
    mode: String,
  ): Response<GenericSuccessDto> {
    if (mode == "unsend") {
      unsendCount++
      sentMessages.removeAll { it.clientOpId == messageId }
    }
    return Response.success(GenericSuccessDto(success = true))
  }

  override suspend fun openViewOnceVoice(
    bearer: String,
    deviceFingerprint: String,
    messageId: String,
  ): Response<ViewOnceOpenResponseDto> = Response.success(
    ViewOnceOpenResponseDto(
      success = true,
      message = ChatMessageDto(
        messageId = messageId,
        conversationId = "conv_vo",
        senderUserId = "user_bob",
        recipientUserId = "user_alice",
        messageType = "voice_once",
        viewOnceTtlSec = 10,
        viewOnceOpenedAtMs = System.currentTimeMillis(),
        viewOnceExpiresAtMs = System.currentTimeMillis() + 10_000L,
      ),
    ),
  )

  override suspend fun consumeViewOnceVoice(
    bearer: String,
    deviceFingerprint: String,
    messageId: String,
  ): Response<GenericSuccessDto> {
    consumedViewOnceIds.add(messageId)
    return Response.success(GenericSuccessDto(success = true))
  }

  override suspend fun recordSecurityFlag(
    bearer: String,
    deviceFingerprint: String,
    conversationId: String,
    body: RecordSecurityFlagRequestDto,
  ): Response<SecurityFlagResponseDto> {
    recordedFlags.add(body)
    return Response.success(SecurityFlagResponseDto(success = true))
  }
}
