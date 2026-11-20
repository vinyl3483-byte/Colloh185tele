package telefam.company

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
import telefam.company.core.ChatAudienceOption
import telefam.company.core.ChatPrivacyRoute
import telefam.company.core.ChatPrivacySettingsState
import telefam.company.core.TelefamChatPrivacyCatalog
import telefam.company.data.local.DeviceIdentityStore
import telefam.company.data.local.TelefamDatabase
import telefam.company.data.network.AppVersionResponseDto
import telefam.company.data.network.AuditEventDto
import telefam.company.data.network.AuthSessionResponseDto
import telefam.company.data.network.ChatPrivacyResponseDto
import telefam.company.data.network.ChatPrivacySettingsDto
import telefam.company.data.network.ContactActionDto
import telefam.company.data.network.ContactSyncBatchRequestDto
import telefam.company.data.network.ContactsPageResponseDto
import telefam.company.data.network.ContentPreferencesDto
import telefam.company.data.network.CreatePostRequestDto
import telefam.company.data.network.CreatePostResponseDto
import telefam.company.data.network.CreatedPostDto
import telefam.company.data.network.DeviceContextTelemetryDto
import telefam.company.data.network.DeviceContextTelemetryResponseDto
import telefam.company.data.network.FaqListResponseDto
import telefam.company.data.network.FeedInteractionBatchRequestDto
import telefam.company.data.network.FeedInteractionRequestDto
import telefam.company.data.network.FeedInteractionResponseDto
import telefam.company.data.network.FeedPageResponseDto
import telefam.company.data.network.FeedSearchResponseDto
import telefam.company.data.network.FollowMutationResponseDto
import telefam.company.data.network.GenericSuccessDto
import telefam.company.data.network.HashtagsSearchResponseDto
import telefam.company.data.network.LegalDocumentsResponseDto
import telefam.company.data.network.LoginApprovalStatusResponseDto
import telefam.company.data.network.LogoutRequestDto
import telefam.company.data.network.MediaUploadResponseDto
import telefam.company.data.network.NetworkQualityMonitor
import telefam.company.data.network.NotificationSettingsDto
import telefam.company.data.network.OAuthDto
import telefam.company.data.network.PendingLoginRequestsResponseDto
import telefam.company.data.network.PrivacySettingsDto
import telefam.company.data.network.ProblemReportRequestDto
import telefam.company.data.network.ProblemReportResponseDto
import telefam.company.data.network.ProfileBackgroundAttachDto
import telefam.company.data.network.ProfileBackgroundListResponseDto
import telefam.company.data.network.ProfileBackgroundReorderDto
import telefam.company.data.network.ProfileBackgroundUploadResponseDto
import telefam.company.data.network.ProfileSetupDto
import telefam.company.data.network.ProfileUpdateRequestDto
import telefam.company.data.network.ProfileUpdateResponseDto
import telefam.company.data.network.RefreshTokenDto
import telefam.company.data.network.RequestOtpDto
import telefam.company.data.network.RequestOtpResponseDto
import telefam.company.data.network.RespondLoginRequestDto
import telefam.company.data.network.RespondLoginRequestResponseDto
import telefam.company.data.network.RestrictedAccountsResponseDto
import telefam.company.data.network.SettingsBundleResponseDto
import telefam.company.data.network.SongsSearchResponseDto
import telefam.company.data.network.SupportTicketsResponseDto
import telefam.company.data.network.SyncBatchRequestDto
import telefam.company.data.network.SyncBatchResponseDto
import telefam.company.data.network.SystemStatusResponseDto
import telefam.company.data.network.TelefamApiService
import telefam.company.data.network.UpdateChatThemeRequestDto
import telefam.company.data.network.UpdatePostRequestDto
import telefam.company.data.network.UserBlockResponseDto
import telefam.company.data.network.UserConnectionsResponseDto
import telefam.company.data.network.UserMuteResponseDto
import telefam.company.data.network.UserProfileResponseDto
import telefam.company.data.network.UserReportRequestDto
import telefam.company.data.network.UserReportResponseDto
import telefam.company.data.network.UsernameCheckResponseDto
import telefam.company.data.network.VerifyLinkDto
import telefam.company.data.network.VerifyOtpDto
import telefam.company.data.repository.TelefamRepository
import telefam.company.ui.screens.TelefamChatPrivacyHostScreen
import telefam.company.ui.theme.TelefamTheme
import telefam.company.ui.theme.ThemeMode
import telefam.company.ui.viewmodel.TelefamViewModel

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h915dp")
class TelefamChatPrivacyRobolectricTest {

  @get:Rule
  val composeRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var db: TelefamDatabase
  private lateinit var identityStore: DeviceIdentityStore
  private lateinit var networkMonitor: NetworkQualityMonitor
  private lateinit var fakeApi: FakeChatPrivacyApiService
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
      userId = "caller_cp_1",
      email = "caller.cp@telefam.test",
      accessToken = "acc_cp_1",
      refreshToken = "ref_cp_1",
      accessExpiresInSec = 900L,
      refreshExpiresInSec = 2_592_000L,
      profileCompleted = true,
    )
    networkMonitor = NetworkQualityMonitor(context)
    fakeApi = FakeChatPrivacyApiService()
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
    Thread.sleep(60L)
    shadowOf(Looper.getMainLooper()).idle()
    db.close()
  }

  @Test
  fun `offline chat privacy and theme changes persist in Room and sync on reconnect`() = runTest {
    setNetworkOnline(false)
    networkMonitor.refreshStatus()

    repository.saveChatPrivacy(
      ChatPrivacySettingsState(
        messageRequests = ChatAudienceOption.FRIENDS,
        whoCanScreenshot = ChatAudienceOption.FRIENDS,
        typingIndicators = ChatAudienceOption.ANYONE,
      ),
    )
    repository.saveChatThemeAndBubbleColor(themeId = "spring_meadow", bubbleColorId = "crimson")

    val cached = repository.loadCachedChatPrivacy()
    assertEquals(ChatAudienceOption.FRIENDS, cached.messageRequests)
    assertEquals(ChatAudienceOption.FRIENDS, cached.whoCanScreenshot)
    assertFalse(cached.blocksScreenshots)
    assertEquals("spring_meadow", cached.chatThemeId)
    assertEquals("crimson", cached.bubbleColorId)

    val ops = db.telefamDao().getReadySettingsOps("caller_cp_1", System.currentTimeMillis() + 60_000L, 20)
    assertTrue(ops.any { it.opKind == "chat_privacy" })
    assertTrue(ops.any { it.opKind == "chat_theme" })

    setNetworkOnline(true)
    networkMonitor.refreshStatus()
    repository.flushOfflineQueue()

    val remainingOps = db.telefamDao().getReadySettingsOps("caller_cp_1", System.currentTimeMillis() + 60_000L, 20)
    assertTrue(remainingOps.isEmpty())
    assertTrue(fakeApi.privacyUpdates >= 1)
    assertTrue(fakeApi.themeUpdates >= 1)
    assertEquals("spring_meadow", fakeApi.stored.chatThemeId)
    assertEquals("crimson", fakeApi.stored.bubbleColorId)
  }

  @Test
  fun `viewmodel navigation stack manages Privacy, Chats Theme, and Bubble Colors`() {
    val vmStore = ViewModelStore()
    val vm = ViewModelProvider(
      vmStore,
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = TelefamViewModel(repository) as T
      },
    )[TelefamViewModel::class.java]
    try {
      waitUntilMain { vm.uiState.value.isAuthenticated }

      vm.openChatPrivacy()
      assertEquals(ChatPrivacyRoute.PRIVACY, vm.uiState.value.activeChatPrivacyRoute)

      vm.openChatsThemePicker()
      assertEquals(ChatPrivacyRoute.CHATS_THEME, vm.uiState.value.activeChatPrivacyRoute)

      vm.selectChatTheme("cosmic_nebula")
      waitUntilMain { vm.uiState.value.chatPrivacy.chatThemeId == "cosmic_nebula" }

      vm.navigateChatPrivacyBack()
      assertEquals(ChatPrivacyRoute.PRIVACY, vm.uiState.value.activeChatPrivacyRoute)

      vm.openBubbleColorPicker()
      assertEquals(ChatPrivacyRoute.BUBBLE_COLORS, vm.uiState.value.activeChatPrivacyRoute)

      vm.selectBubbleColor("violet")
      waitUntilMain { vm.uiState.value.chatPrivacy.bubbleColorId == "violet" }

      vm.navigateChatPrivacyBack()
      vm.navigateChatPrivacyBack()
      assertEquals(null, vm.uiState.value.activeChatPrivacyRoute)
    } finally {
      vmStore.clear()
      shadowOf(Looper.getMainLooper()).idle()
    }
  }

  @Test
  fun `chat privacy screen shows all 11 rows, updates screenshot guard, and navigates themes & 6 bubbles`() {
    val backStack = mutableStateListOf(ChatPrivacyRoute.PRIVACY)
    var state by mutableStateOf(ChatPrivacySettingsState())

    composeRule.setContent {
      TelefamTheme(themeMode = ThemeMode.DARK) {
        val route = backStack.lastOrNull()
        if (route != null) {
          TelefamChatPrivacyHostScreen(
            route = route,
            state = state,
            previewSenderName = "Alex Mwangi",
            onNavigateBack = {
              if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex)
            },
            onUpdatePrivacy = { state = it },
            onOpenChatsTheme = { backStack.add(ChatPrivacyRoute.CHATS_THEME) },
            onOpenBubbleColors = { backStack.add(ChatPrivacyRoute.BUBBLE_COLORS) },
            onSelectTheme = { state = state.copy(chatThemeId = it) },
            onSelectBubbleColor = { state = state.copy(bubbleColorId = it) },
          )
        }
      }
    }

    composeRule.onNodeWithText("Privacy").assertIsDisplayed()
    composeRule.onNodeWithText("Control who can interact with you and your messages.").assertIsDisplayed()

    val listNode = composeRule.onNodeWithTag("chat_privacy_list")
    val rowTags = listOf(
      "chat_privacy_message_requests",
      "chat_privacy_who_can_call",
      "chat_privacy_screenshot",
      "chat_privacy_share_chats",
      "chat_privacy_copy_messages",
      "chat_privacy_download_media",
      "chat_privacy_theme",
      "chat_privacy_bubble_colour",
      "chat_privacy_read_receipts",
      "chat_privacy_typing_indicators",
      "chat_privacy_online_indicators",
    )
    rowTags.forEach { tag ->
      listNode.performScrollToNode(hasTestTag(tag))
      composeRule.onNodeWithTag(tag).assertIsDisplayed()
    }

    // Toggle Who Can Screenshot Chats from Nobody -> Friends
    assertTrue(state.blocksScreenshots)
    listNode.performScrollToNode(hasTestTag("chat_privacy_screenshot"))
    composeRule.onNodeWithTag("chat_privacy_screenshot").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("audience_option_friends").performClick()
    composeRule.waitForIdle()
    assertEquals(ChatAudienceOption.FRIENDS, state.whoCanScreenshot)
    assertFalse(state.blocksScreenshots)

    // Open Chats Theme -> verify catalog & live preview -> select Spring Meadow
    listNode.performScrollToNode(hasTestTag("chat_privacy_theme"))
    composeRule.onNodeWithTag("chat_privacy_theme").performClick()
    composeRule.waitForIdle()
    assertEquals(13, TelefamChatPrivacyCatalog.themes.size)
    composeRule.onNodeWithText("Select theme").assertIsDisplayed()
    composeRule.onNodeWithText("Alex Mwangi").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithText("Executive officer").assertDoesNotExist()
    composeRule.onNodeWithText("It's morning in Tokyo").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("theme_tile_spring_meadow").performScrollTo().performClick()
    composeRule.waitForIdle()
    assertEquals("spring_meadow", state.chatThemeId)

    composeRule.onNodeWithTag("chat_theme_back").performClick()
    composeRule.waitForIdle()

    // Open Message Bubbles Colour -> verify all 6 colors & pick Emerald
    composeRule.onNodeWithTag("chat_privacy_list").performScrollToNode(hasTestTag("chat_privacy_bubble_colour"))
    composeRule.onNodeWithTag("chat_privacy_bubble_colour").performClick()
    composeRule.waitForIdle()
    assertEquals(6, TelefamChatPrivacyCatalog.bubbleColors.size)
    composeRule.onNodeWithTag("bubble_color_card_emerald").performScrollTo().performClick()
    composeRule.waitForIdle()
    assertEquals("emerald", state.bubbleColorId)
  }

  private fun waitUntilMain(timeoutMs: Long = 3_000L, predicate: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (!predicate() && System.currentTimeMillis() < deadline) {
      shadowOf(Looper.getMainLooper()).idle()
      if (!predicate()) {
        Thread.sleep(15L)
      }
    }
    shadowOf(Looper.getMainLooper()).idle()
  }

  private fun setNetworkOnline(online: Boolean) {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val active = cm.activeNetwork ?: return
    val caps = ShadowNetworkCapabilities.newInstance()
    if (online) {
      shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    shadowOf(cm).setNetworkCapabilities(active, caps)
  }

  private class FakeChatPrivacyApiService : AbstractTestChatApiStub() {
    var privacyUpdates = 0
    var themeUpdates = 0
    var stored = ChatPrivacySettingsDto()

    override suspend fun getChatPrivacy(bearer: String, deviceFingerprint: String): Response<ChatPrivacyResponseDto> =
      Response.success(ChatPrivacyResponseDto(success = true, chatPrivacy = stored))

    override suspend fun updateChatPrivacy(bearer: String, deviceFingerprint: String, body: ChatPrivacySettingsDto): Response<ChatPrivacyResponseDto> {
      privacyUpdates++
      stored = body
      return Response.success(ChatPrivacyResponseDto(success = true, chatPrivacy = stored))
    }

    override suspend fun updateChatTheme(bearer: String, deviceFingerprint: String, body: UpdateChatThemeRequestDto): Response<ChatPrivacyResponseDto> {
      themeUpdates++
      stored = stored.copy(
        chatThemeId = body.chatThemeId ?: stored.chatThemeId,
        bubbleColorId = body.bubbleColorId ?: stored.bubbleColorId,
      )
      return Response.success(ChatPrivacyResponseDto(success = true, chatPrivacy = stored))
    }

    override suspend fun getSettings(bearer: String, deviceFingerprint: String): Response<SettingsBundleResponseDto> =
      Response.success(SettingsBundleResponseDto(success = true, chatPrivacy = stored))

    override suspend fun updatePrivacySettings(bearer: String, deviceFingerprint: String, body: PrivacySettingsDto): Response<SettingsBundleResponseDto> =
      Response.success(SettingsBundleResponseDto(success = true, privacy = body))

    override suspend fun updateNotificationSettings(bearer: String, deviceFingerprint: String, body: NotificationSettingsDto): Response<SettingsBundleResponseDto> =
      Response.success(SettingsBundleResponseDto(success = true, notifications = body))

    override suspend fun updateContentPreferences(bearer: String, deviceFingerprint: String, body: ContentPreferencesDto): Response<SettingsBundleResponseDto> =
      Response.success(SettingsBundleResponseDto(success = true, contentPreferences = body))

    override suspend fun listBlockedAccounts(bearer: String, deviceFingerprint: String, query: String): Response<RestrictedAccountsResponseDto> = Response.success(RestrictedAccountsResponseDto(success = true))
    override suspend fun listMutedAccounts(bearer: String, deviceFingerprint: String, query: String): Response<RestrictedAccountsResponseDto> = Response.success(RestrictedAccountsResponseDto(success = true))
    override suspend fun submitProblemReport(bearer: String, deviceFingerprint: String, body: ProblemReportRequestDto): Response<ProblemReportResponseDto> = Response.success(ProblemReportResponseDto(success = true))
    override suspend fun listSupportTickets(bearer: String, deviceFingerprint: String): Response<SupportTicketsResponseDto> = Response.success(SupportTicketsResponseDto(success = true))
    override suspend fun searchFaqs(bearer: String, deviceFingerprint: String, query: String): Response<FaqListResponseDto> = Response.success(FaqListResponseDto(success = true))
    override suspend fun getSystemStatus(bearer: String, deviceFingerprint: String): Response<SystemStatusResponseDto> = Response.success(SystemStatusResponseDto(success = true))
    override suspend fun getLegalDocuments(bearer: String, deviceFingerprint: String): Response<LegalDocumentsResponseDto> = Response.success(LegalDocumentsResponseDto(success = true))
    override suspend fun checkAppVersion(bearer: String, deviceFingerprint: String, version: String): Response<AppVersionResponseDto> = Response.success(AppVersionResponseDto(success = true, latestVersion = version))
    override suspend fun logout(bearer: String, deviceFingerprint: String, body: LogoutRequestDto): Response<GenericSuccessDto> = Response.success(GenericSuccessDto(success = true))
    override suspend fun requestOtp(deviceFingerprint: String, ipFingerprint: String, body: RequestOtpDto): Response<RequestOtpResponseDto> = Response.success(RequestOtpResponseDto(success = true))
    override suspend fun verifyOtp(deviceFingerprint: String, ipFingerprint: String, body: VerifyOtpDto): Response<AuthSessionResponseDto> = Response.success(AuthSessionResponseDto(success = true))
    override suspend fun verifyLink(deviceFingerprint: String, ipFingerprint: String, body: VerifyLinkDto): Response<AuthSessionResponseDto> = Response.success(AuthSessionResponseDto(success = true))
    override suspend fun refreshToken(deviceFingerprint: String, body: RefreshTokenDto): Response<AuthSessionResponseDto> = Response.success(AuthSessionResponseDto(success = true))
    override suspend fun verifyGoogleOAuth(deviceFingerprint: String, ipFingerprint: String, body: OAuthDto): Response<AuthSessionResponseDto> = Response.success(AuthSessionResponseDto(success = true))
    override suspend fun checkUsername(username: String): Response<UsernameCheckResponseDto> = Response.success(UsernameCheckResponseDto(available = true))
    override suspend fun uploadAvatar(bearer: String, deviceFingerprint: String, cropCenterX: Float, cropCenterY: Float, cropScale: Float, imageBody: RequestBody): Response<MediaUploadResponseDto> = Response.success(MediaUploadResponseDto(success = true))
    override suspend fun uploadProfileBackground(bearer: String, deviceFingerprint: String, imageBody: RequestBody): Response<ProfileBackgroundUploadResponseDto> = Response.success(ProfileBackgroundUploadResponseDto(success = true))
    override suspend fun uploadPostPhoto(bearer: String, deviceFingerprint: String, imageBody: RequestBody): Response<MediaUploadResponseDto> = Response.success(MediaUploadResponseDto(success = true))
    override suspend fun uploadPostVideo(bearer: String, deviceFingerprint: String, frameHashesCsv: String, durationMs: Long, trimStartMs: Long, trimEndMs: Long, width: Int, height: Int, videoPart: MultipartBody.Part, thumbnailPart: MultipartBody.Part?): Response<MediaUploadResponseDto> = Response.success(MediaUploadResponseDto(success = true))
    override suspend fun searchSongs(bearer: String, deviceFingerprint: String, query: String): Response<SongsSearchResponseDto> = Response.success(SongsSearchResponseDto(success = true))
    override suspend fun searchHashtags(bearer: String, deviceFingerprint: String, query: String): Response<HashtagsSearchResponseDto> = Response.success(HashtagsSearchResponseDto(success = true))
    override suspend fun createPost(bearer: String, deviceFingerprint: String, body: CreatePostRequestDto): Response<CreatePostResponseDto> = Response.success(CreatePostResponseDto(success = true, post = CreatedPostDto(postId = "p1")))
    override suspend fun updatePost(bearer: String, deviceFingerprint: String, postId: String, body: UpdatePostRequestDto): Response<CreatePostResponseDto> = Response.success(CreatePostResponseDto(success = true, post = CreatedPostDto(postId = postId)))
    override suspend fun deletePost(bearer: String, deviceFingerprint: String, postId: String): Response<GenericSuccessDto> = Response.success(GenericSuccessDto(success = true))
    override suspend fun getFeeds(bearer: String, deviceFingerprint: String, tab: String, page: Int, pageSize: Int): Response<FeedPageResponseDto> = Response.success(FeedPageResponseDto(success = true))
    override suspend fun searchFeeds(bearer: String, deviceFingerprint: String, query: String, shortcut: String, page: Int, pageSize: Int): Response<FeedSearchResponseDto> = Response.success(FeedSearchResponseDto(success = true))
    override suspend fun interactFeed(bearer: String, deviceFingerprint: String, body: FeedInteractionRequestDto): Response<FeedInteractionResponseDto> = Response.success(FeedInteractionResponseDto(success = true))
    override suspend fun interactFeedBatch(bearer: String, deviceFingerprint: String, body: FeedInteractionBatchRequestDto): Response<SyncBatchResponseDto> = Response.success(SyncBatchResponseDto(success = true))
    override suspend fun setupProfile(bearer: String, deviceFingerprint: String, body: ProfileSetupDto): Response<GenericSuccessDto> = Response.success(GenericSuccessDto(success = true))
    override suspend fun updateProfile(bearer: String, deviceFingerprint: String, body: ProfileUpdateRequestDto): Response<ProfileUpdateResponseDto> = Response.success(ProfileUpdateResponseDto(success = true))
    override suspend fun attachProfileBackground(bearer: String, deviceFingerprint: String, body: ProfileBackgroundAttachDto): Response<ProfileBackgroundListResponseDto> = Response.success(ProfileBackgroundListResponseDto(success = true))
    override suspend fun reorderProfileBackgrounds(bearer: String, deviceFingerprint: String, body: ProfileBackgroundReorderDto): Response<ProfileBackgroundListResponseDto> = Response.success(ProfileBackgroundListResponseDto(success = true))
    override suspend fun deleteProfileBackground(bearer: String, deviceFingerprint: String, mediaId: String): Response<ProfileBackgroundListResponseDto> = Response.success(ProfileBackgroundListResponseDto(success = true))
    override suspend fun recordAuditEvent(bearer: String, deviceFingerprint: String, body: AuditEventDto): Response<GenericSuccessDto> = Response.success(GenericSuccessDto(success = true))
    override suspend fun syncBatch(bearer: String, deviceFingerprint: String, body: SyncBatchRequestDto): Response<SyncBatchResponseDto> = Response.success(SyncBatchResponseDto(success = true))
    override suspend fun listContacts(bearer: String, deviceFingerprint: String, filter: String, query: String, page: Int, pageSize: Int): Response<ContactsPageResponseDto> = Response.success(ContactsPageResponseDto(success = true))
    override suspend fun followContact(bearer: String, deviceFingerprint: String, body: ContactActionDto): Response<FollowMutationResponseDto> = Response.success(FollowMutationResponseDto(success = true))
    override suspend fun unfollowContact(bearer: String, deviceFingerprint: String, body: ContactActionDto): Response<FollowMutationResponseDto> = Response.success(FollowMutationResponseDto(success = true))
    override suspend fun syncContactActions(bearer: String, deviceFingerprint: String, body: ContactSyncBatchRequestDto): Response<SyncBatchResponseDto> = Response.success(SyncBatchResponseDto(success = true))
    override suspend fun getUserProfile(bearer: String, deviceFingerprint: String, userId: String): Response<UserProfileResponseDto> = Response.success(UserProfileResponseDto(success = true, userId = userId))
    override suspend fun getUserConnections(bearer: String, deviceFingerprint: String, userId: String, type: String, page: Int, pageSize: Int): Response<UserConnectionsResponseDto> = Response.success(UserConnectionsResponseDto(success = true))
    override suspend fun getUserFollowers(bearer: String, deviceFingerprint: String, userId: String, page: Int, pageSize: Int): Response<UserConnectionsResponseDto> = Response.success(UserConnectionsResponseDto(success = true))
    override suspend fun getUserFollowing(bearer: String, deviceFingerprint: String, userId: String, page: Int, pageSize: Int): Response<UserConnectionsResponseDto> = Response.success(UserConnectionsResponseDto(success = true))
    override suspend fun blockUser(bearer: String, deviceFingerprint: String, userId: String): Response<UserBlockResponseDto> = Response.success(UserBlockResponseDto(success = true))
    override suspend fun unblockUser(bearer: String, deviceFingerprint: String, userId: String): Response<UserBlockResponseDto> = Response.success(UserBlockResponseDto(success = true))
    override suspend fun muteUser(bearer: String, deviceFingerprint: String, userId: String): Response<UserMuteResponseDto> = Response.success(UserMuteResponseDto(success = true))
    override suspend fun unmuteUser(bearer: String, deviceFingerprint: String, userId: String): Response<UserMuteResponseDto> = Response.success(UserMuteResponseDto(success = true))
    override suspend fun reportUser(bearer: String, deviceFingerprint: String, userId: String, body: UserReportRequestDto): Response<UserReportResponseDto> = Response.success(UserReportResponseDto(success = true))
    override suspend fun downloadMedia(url: String): Response<ResponseBody> = Response.success(ByteArray(0).toResponseBody("application/octet-stream".toMediaType()))
    override suspend fun getPendingLoginRequests(bearer: String, deviceFingerprint: String): Response<PendingLoginRequestsResponseDto> = Response.success(PendingLoginRequestsResponseDto(success = true))
    override suspend fun getLoginRequestStatus(deviceFingerprint: String, pollToken: String, requestId: String): Response<LoginApprovalStatusResponseDto> = Response.success(LoginApprovalStatusResponseDto(success = true, status = "pending", remainingSeconds = 60L))
    override suspend fun respondToLoginRequest(bearer: String, deviceFingerprint: String, requestId: String, body: RespondLoginRequestDto): Response<RespondLoginRequestResponseDto> = Response.success(RespondLoginRequestResponseDto(success = true, requestId = requestId))
    override suspend fun syncDeviceContext(bearer: String, deviceFingerprint: String, body: DeviceContextTelemetryDto): Response<DeviceContextTelemetryResponseDto> = Response.success(DeviceContextTelemetryResponseDto(success = true))
    override suspend fun getSyncSettings(bearer: String, deviceFingerprint: String): Response<telefam.company.data.network.SyncSettingsResponseDto> = Response.success(telefam.company.data.network.SyncSettingsResponseDto(success = true))
    override suspend fun updateSyncSettings(bearer: String, deviceFingerprint: String, body: telefam.company.data.network.SyncPreferencesDto): Response<telefam.company.data.network.SyncSettingsResponseDto> = Response.success(telefam.company.data.network.SyncSettingsResponseDto(success = true, syncPreferences = body))
    override suspend fun syncNow(bearer: String, deviceFingerprint: String, body: telefam.company.data.network.ManualSyncRequestDto): Response<telefam.company.data.network.ManualSyncResponseDto> = Response.success(telefam.company.data.network.ManualSyncResponseDto(success = true, ackedOpIds = body.operations.map { it.clientOpId }))
  }
}
