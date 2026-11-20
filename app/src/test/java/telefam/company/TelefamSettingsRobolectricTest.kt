package telefam.company

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
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
import org.junit.Assert.assertNotNull
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
import telefam.company.core.ContentPreferencesState
import telefam.company.core.NotificationSettingsState
import telefam.company.core.OfflineSyncStatusSnapshot
import telefam.company.core.PrivacySettingsState
import telefam.company.core.SettingsDestination
import telefam.company.core.SyncPreferencesState
import telefam.company.core.TelefamQrEncoder
import telefam.company.core.VideoQualityOption
import telefam.company.data.local.DeviceIdentityStore
import telefam.company.data.local.TelefamDatabase
import telefam.company.data.network.AppVersionResponseDto
import telefam.company.data.network.AuditEventDto
import telefam.company.data.network.AuthSessionResponseDto
import telefam.company.data.network.ContactActionDto
import telefam.company.data.network.ContactSyncBatchRequestDto
import telefam.company.data.network.ContactsPageResponseDto
import telefam.company.data.network.ContentPreferencesDto
import telefam.company.data.network.CreatePostRequestDto
import telefam.company.data.network.CreatePostResponseDto
import telefam.company.data.network.CreatedPostDto
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
import telefam.company.data.network.LogoutRequestDto
import telefam.company.data.network.MediaUploadResponseDto
import telefam.company.data.network.NetworkQualityMonitor
import telefam.company.data.network.NotificationSettingsDto
import telefam.company.data.network.OAuthDto
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
import telefam.company.data.network.RestrictedAccountItemDto
import telefam.company.data.network.RestrictedAccountsResponseDto
import telefam.company.data.network.SettingsBundleResponseDto
import telefam.company.data.network.SongsSearchResponseDto
import telefam.company.data.network.SupportTicketDto
import telefam.company.data.network.SupportTicketsResponseDto
import telefam.company.data.network.SyncBatchRequestDto
import telefam.company.data.network.SyncBatchResponseDto
import telefam.company.data.network.SystemStatusResponseDto
import telefam.company.data.network.TelefamApiService
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
import telefam.company.ui.screens.TelefamSettingsScreen
import telefam.company.ui.theme.TelefamTheme
import telefam.company.ui.theme.ThemeMode
import telefam.company.ui.viewmodel.HomeTab
import telefam.company.ui.viewmodel.TelefamUiState
import telefam.company.ui.viewmodel.TelefamViewModel

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h915dp")
class TelefamSettingsRobolectricTest {

  @get:Rule
  val composeRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var db: TelefamDatabase
  private lateinit var identityStore: DeviceIdentityStore
  private lateinit var networkMonitor: NetworkQualityMonitor
  private lateinit var fakeApi: FakeSettingsApiService
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
      userId = "caller_1",
      email = "caller@telefam.test",
      accessToken = "acc_caller_1",
      refreshToken = "ref_caller_1",
      accessExpiresInSec = 900L,
      refreshExpiresInSec = 2_592_000L,
      profileCompleted = true,
    )
    networkMonitor = NetworkQualityMonitor(context)
    fakeApi = FakeSettingsApiService()
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
  fun `tapping own userId navigates to canonical profile tab without pushing duplicate overlay`() {
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
      vm.selectHomeTab(HomeTab.FEEDS)
      vm.openUserProfile("u_other")
      waitUntilMain { vm.uiState.value.profileBackStack.size == 1 }

      // Now open own userId -> must clear overlay backstack and focus HomeTab.PROFILE
      vm.openUserProfile("caller_1")
      waitUntilMain { vm.uiState.value.activeHomeTab == HomeTab.PROFILE && vm.uiState.value.profileBackStack.isEmpty() }

      assertTrue(vm.uiState.value.profileBackStack.isEmpty())
      assertEquals(HomeTab.PROFILE, vm.uiState.value.activeHomeTab)
    } finally {
      vmStore.clear()
      shadowOf(Looper.getMainLooper()).idle()
    }
  }

  @Test
  fun `offline settings save queues ops locally and syncs upon reconnect`() = runTest {
    setNetworkOnline(false)
    networkMonitor.refreshStatus()

    repository.savePrivacySettings(PrivacySettingsState(isPrivateAccount = true, showActivityStatus = false))
    repository.saveNotificationSettings(NotificationSettingsState(masterPause = true, channelSms = true))
    repository.saveContentPreferences(ContentPreferencesState(autoplayVideos = false, dataSaver = true, videoQuality = VideoQualityOption.DATA_SAVER))

    val queuedOps = db.telefamDao().getReadySettingsOps("caller_1", System.currentTimeMillis() + 60_000L, 20)
    assertEquals(3, queuedOps.size)
    assertTrue(repository.fetchSettings().privacy.isPrivateAccount)
    assertTrue(repository.fetchSettings().notifications.masterPause)
    assertFalse(repository.fetchSettings().contentPreferences.autoplayVideos)

    setNetworkOnline(true)
    networkMonitor.refreshStatus()
    repository.flushOfflineQueue()

    assertTrue(db.telefamDao().getReadySettingsOps("caller_1", System.currentTimeMillis() + 60_000L, 20).isEmpty())
    assertTrue(fakeApi.privacySavedCount >= 1)
    assertTrue(fakeApi.notificationSavedCount >= 1)
    assertTrue(fakeApi.contentPrefSavedCount >= 1)
  }

  @Test
  fun `blocked and muted accounts list, problem report reference, and logout all devices work end to end`() {
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

      vm.openOwnerSettingsModal()
      vm.navigateToSettings(SettingsDestination.BLOCKED_ACCOUNTS)
      waitUntilMain { vm.uiState.value.blockedAccounts.isNotEmpty() && vm.uiState.value.mutedAccounts.isNotEmpty() }

      assertEquals("u_blk_1", vm.uiState.value.blockedAccounts.first().userId)
      assertEquals("u_mute_1", vm.uiState.value.mutedAccounts.first().userId)

      vm.unblockAccountFromSettings("u_blk_1")
      vm.unmuteAccountFromSettings("u_mute_1")
      waitUntilMain { vm.uiState.value.blockedAccounts.isEmpty() && vm.uiState.value.mutedAccounts.isEmpty() }

      vm.submitProblemReport("Video playback", "Video paused on buffer switch", null)
      waitUntilMain { vm.uiState.value.lastCreatedTicket != null }
      val ticket = vm.uiState.value.lastCreatedTicket
      assertNotNull(ticket)
      assertTrue(ticket!!.referenceNumber.startsWith("TF-"))

      vm.requestLogoutConfirmation()
      vm.setLogoutAllDevicesChoice(true)
      vm.confirmSignOut()
      waitUntilMain { !vm.uiState.value.isAuthenticated }
      assertEquals(true, fakeApi.lastLogoutAllDevices)
    } finally {
      vmStore.clear()
      shadowOf(Looper.getMainLooper()).idle()
    }
  }

  @Test
  fun `qr encoder generates valid 33x33 matrix with intact finder corners`() {
    val matrix = TelefamQrEncoder.encodeToMatrix("https://telefam.app/u/colloh")
    assertEquals(33, matrix.size)
    assertTrue(matrix.all { it.size == 33 })
    // Top-left 7x7 finder outer ring
    assertTrue(matrix[0][0])
    assertTrue(matrix[0][6])
    assertTrue(matrix[6][0])
    assertFalse(matrix[1][1])
    assertTrue(matrix[3][3])
    // Top-right 7x7 finder
    assertTrue(matrix[0][26])
    assertTrue(matrix[0][32])
    assertTrue(matrix[3][29])
    // Bottom-left 7x7 finder
    assertTrue(matrix[26][0])
    assertTrue(matrix[32][0])
    assertTrue(matrix[29][3])
  }

  @Test
  fun `settings screen renders hub, filters search, and shows logout confirmation dialog`() {
    var searchVal by mutableStateOf("")
    var showDialog by mutableStateOf(false)
    var logoutAll by mutableStateOf(false)
    var confirmed by mutableStateOf(false)

    composeRule.setContent {
      TelefamTheme(themeMode = ThemeMode.LIGHT) {
        TelefamSettingsScreen(
          uiState = TelefamUiState(
            isAuthenticated = true,
            profileCompleted = true,
            userId = "caller_1",
            showSettingsModal = true,
            settingsBackStack = listOf(SettingsDestination.HUB),
            settingsSearchQuery = searchVal,
            showLogoutConfirmDialog = showDialog,
            logoutAllDevicesChoice = logoutAll,
          ),
          onNavigateBack = {},
          onOpenDestination = {},
          onSearchQueryChanged = { searchVal = it },
          onToggleTheme = {},
          onUpdatePrivacy = {},
          onUpdateNotifications = {},
          onUpdateContentPreferences = { _, _ -> },
          onRestrictedTabSelected = {},
          onRestrictedQueryChanged = {},
          onUnblockAccount = {},
          onUnmuteAccount = {},
          onSubmitProblemReport = { _, _, _ -> },
          onClearLastCreatedTicket = {},
          onSearchFaq = {},
          onRefreshTickets = {},
          onRefreshStatus = {},
          onSelectLegalDoc = {},
          onCheckAppUpdates = {},
          onShowToast = {},
          onRequestLogout = { showDialog = true },
          onDismissLogoutDialog = { showDialog = false },
          onToggleLogoutAllDevices = { logoutAll = it },
          onConfirmLogout = { confirmed = true },
        )
      }
    }

    composeRule.onNodeWithText("Settings and privacy").assertIsDisplayed()
    composeRule.onNodeWithTag("settings_search_input").assertIsDisplayed().performTextInput("blocked")
    composeRule.waitForIdle()
    assertEquals("blocked", searchVal)
    composeRule.onNodeWithTag("settings_row_blocked_accounts").assertIsDisplayed()

    composeRule.runOnUiThread { searchVal = "" }
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("settings_hub_list").performScrollToNode(hasTestTag("settings_row_account_privacy"))
    composeRule.onNodeWithTag("settings_row_account_privacy").assertIsDisplayed()
    composeRule.onNodeWithTag("settings_hub_list").performScrollToNode(hasTestTag("settings_row_share_profile"))
    composeRule.onNodeWithTag("settings_row_share_profile").assertIsDisplayed()
    composeRule.onNodeWithTag("settings_hub_list").performScrollToNode(hasTestTag("settings_row_safety_center"))
    composeRule.onNodeWithTag("settings_row_safety_center").assertIsDisplayed()

    composeRule.onNodeWithTag("settings_hub_list").performScrollToNode(hasTestTag("settings_logout_button"))
    composeRule.onNodeWithTag("settings_logout_button").performClick()
    composeRule.waitForIdle()
    assertTrue(showDialog)
  }

  @Test
  fun `offline sync preferences queue locally and manual syncNow clears outbox and updates server`() = runTest {
    setNetworkOnline(false)
    networkMonitor.refreshStatus()

    repository.savePrivacySettings(PrivacySettingsState(isPrivateAccount = true))
    repository.saveSyncPreferences(
      SyncPreferencesState(
        autoSyncEnabled = false,
        syncOverWifiOnly = true,
        backgroundSyncEnabled = false,
        cacheFeedsOffline = false,
        cacheChatMediaOffline = true,
      ),
    )

    val ops = db.telefamDao().getReadySettingsOps("caller_1", System.currentTimeMillis() + 60_000L, 20)
    assertTrue(ops.any { it.opKind == "sync_prefs" })
    assertTrue(ops.size >= 2)
    assertTrue(repository.getOfflineSyncStatus().totalPendingCount >= 2)

    // On metered connection with syncOverWifiOnly=true, sync waits for Wi-Fi
    setNetworkOnline(online = true, wifi = false)
    networkMonitor.refreshStatus()
    val (_, meteredStatus) = repository.performManualServerSyncNow()
    assertTrue(meteredStatus.lastResultMessage.contains("Wi-Fi", ignoreCase = true))
    assertEquals(0, fakeApi.manualSyncCount)

    // Switch to Wi-Fi -> manual sync flushes outbox and updates server state
    setNetworkOnline(online = true, wifi = true)
    networkMonitor.refreshStatus()
    val (mergedSnapshot, postStatus) = repository.performManualServerSyncNow()
    assertTrue(fakeApi.manualSyncCount >= 1)
    assertTrue(fakeApi.storedSyncPrefs.syncOverWifiOnly)
    assertFalse(fakeApi.storedSyncPrefs.autoSyncEnabled)
    assertTrue(mergedSnapshot.syncPreferences.syncOverWifiOnly)
    assertTrue(db.telefamDao().getReadySettingsOps("caller_1", System.currentTimeMillis() + 60_000L, 20).isEmpty())
    assertEquals(0, postStatus.totalPendingCount)
    assertEquals(0, repository.getOfflineSyncStatus().totalPendingCount)
  }

  @Test
  fun `settings screen shows offline sync hub row and triggers sync now and toggles`() {
    var activeDest by mutableStateOf(SettingsDestination.HUB)
    var syncPrefs by mutableStateOf(SyncPreferencesState())
    var syncTriggered by mutableStateOf(false)

    composeRule.setContent {
      TelefamTheme(themeMode = ThemeMode.LIGHT) {
        TelefamSettingsScreen(
          uiState = TelefamUiState(
            isAuthenticated = true,
            profileCompleted = true,
            userId = "caller_1",
            showSettingsModal = true,
            settingsBackStack = listOf(activeDest),
            settings = telefam.company.core.SettingsSnapshot(syncPreferences = syncPrefs),
            syncStatus = OfflineSyncStatusSnapshot(
              pendingSettingsOps = 2,
              lastSyncedAtMs = System.currentTimeMillis(),
              lastResultMessage = "2 queued changes ready",
            ),
          ),
          onNavigateBack = { activeDest = SettingsDestination.HUB },
          onOpenDestination = { activeDest = it },
          onSearchQueryChanged = {},
          onToggleTheme = {},
          onUpdatePrivacy = {},
          onUpdateNotifications = {},
          onUpdateContentPreferences = { _, _ -> },
          onUpdateSyncPreferences = { syncPrefs = it },
          onTriggerManualSync = { syncTriggered = true },
          onRefreshSyncStatus = {},
          onRestrictedTabSelected = {},
          onRestrictedQueryChanged = {},
          onUnblockAccount = {},
          onUnmuteAccount = {},
          onSubmitProblemReport = { _, _, _ -> },
          onClearLastCreatedTicket = {},
          onSearchFaq = {},
          onRefreshTickets = {},
          onRefreshStatus = {},
          onSelectLegalDoc = {},
          onCheckAppUpdates = {},
          onShowToast = {},
          onRequestLogout = {},
          onDismissLogoutDialog = {},
          onToggleLogoutAllDevices = {},
          onConfirmLogout = {},
        )
      }
    }

    composeRule.onNodeWithTag("settings_hub_list").performScrollToNode(hasTestTag("settings_row_offline_sync"))
    composeRule.onNodeWithTag("settings_row_offline_sync").assertIsDisplayed().performClick()
    composeRule.waitForIdle()
    assertEquals(SettingsDestination.OFFLINE_SYNC, activeDest)

    composeRule.onNodeWithTag("offline_sync_pending_count").assertIsDisplayed()
    composeRule.onNodeWithTag("sync_wifi_only_switch").performClick()
    composeRule.waitForIdle()
    assertTrue(syncPrefs.syncOverWifiOnly)

    composeRule.onNodeWithTag("sync_auto_sync_switch").performClick()
    composeRule.waitForIdle()
    assertFalse(syncPrefs.autoSyncEnabled)

    composeRule.onNodeWithTag("settings_offline_sync_now_button").performClick()
    composeRule.waitForIdle()
    assertTrue(syncTriggered)
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

  private fun setNetworkOnline(online: Boolean, wifi: Boolean = true) {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val active = cm.activeNetwork ?: return
    val caps = ShadowNetworkCapabilities.newInstance()
    if (online) {
      shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
      if (wifi) {
        shadowOf(caps).addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
      } else {
        shadowOf(caps).addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
      }
    }
    shadowOf(cm).setNetworkCapabilities(active, caps)
  }

  private class FakeSettingsApiService : AbstractTestChatApiStub() {
    var privacySavedCount = 0
    var notificationSavedCount = 0
    var contentPrefSavedCount = 0
    var syncPrefSavedCount = 0
    var manualSyncCount = 0
    var lastLogoutAllDevices: Boolean? = null
    var storedSyncPrefs = telefam.company.data.network.SyncPreferencesDto()

    private val blocked = mutableListOf(
      RestrictedAccountItemDto("u_blk_1", "Blocked User", "blocked_u", "", 1_000L),
    )
    private val muted = mutableListOf(
      RestrictedAccountItemDto("u_mute_1", "Muted User", "muted_u", "", 2_000L),
    )
    private val tickets = mutableListOf<SupportTicketDto>()

    override suspend fun getSettings(bearer: String, deviceFingerprint: String): Response<SettingsBundleResponseDto> =
      Response.success(
        SettingsBundleResponseDto(
          success = true,
          privacy = PrivacySettingsDto(),
          notifications = NotificationSettingsDto(),
          contentPreferences = ContentPreferencesDto(),
          syncPreferences = storedSyncPrefs,
        ),
      )

    override suspend fun updatePrivacySettings(bearer: String, deviceFingerprint: String, body: PrivacySettingsDto): Response<SettingsBundleResponseDto> {
      privacySavedCount++
      return Response.success(SettingsBundleResponseDto(success = true, privacy = body))
    }

    override suspend fun updateNotificationSettings(bearer: String, deviceFingerprint: String, body: NotificationSettingsDto): Response<SettingsBundleResponseDto> {
      notificationSavedCount++
      return Response.success(SettingsBundleResponseDto(success = true, notifications = body))
    }

    override suspend fun updateContentPreferences(bearer: String, deviceFingerprint: String, body: ContentPreferencesDto): Response<SettingsBundleResponseDto> {
      contentPrefSavedCount++
      return Response.success(SettingsBundleResponseDto(success = true, contentPreferences = body))
    }

    override suspend fun listBlockedAccounts(bearer: String, deviceFingerprint: String, query: String): Response<RestrictedAccountsResponseDto> =
      Response.success(RestrictedAccountsResponseDto(success = true, items = blocked.toList()))

    override suspend fun listMutedAccounts(bearer: String, deviceFingerprint: String, query: String): Response<RestrictedAccountsResponseDto> =
      Response.success(RestrictedAccountsResponseDto(success = true, items = muted.toList()))

    override suspend fun unblockUser(bearer: String, deviceFingerprint: String, userId: String): Response<UserBlockResponseDto> {
      blocked.removeAll { it.userId == userId }
      return Response.success(UserBlockResponseDto(success = true, targetUserId = userId, isBlocked = false))
    }

    override suspend fun muteUser(bearer: String, deviceFingerprint: String, userId: String): Response<UserMuteResponseDto> =
      Response.success(UserMuteResponseDto(success = true, userId = userId, isMuted = true))

    override suspend fun unmuteUser(bearer: String, deviceFingerprint: String, userId: String): Response<UserMuteResponseDto> {
      muted.removeAll { it.userId == userId }
      return Response.success(UserMuteResponseDto(success = true, userId = userId, isMuted = false))
    }

    override suspend fun submitProblemReport(bearer: String, deviceFingerprint: String, body: ProblemReportRequestDto): Response<ProblemReportResponseDto> {
      val ref = "TF-20261115-ABCD"
      val t = SupportTicketDto(
        ticketId = "t_${tickets.size + 1}",
        referenceNumber = ref,
        category = body.category,
        description = body.description,
        status = "open",
        createdAtMs = System.currentTimeMillis(),
      )
      tickets.add(0, t)
      return Response.success(ProblemReportResponseDto(success = true, referenceNumber = ref, ticket = t))
    }

    override suspend fun listSupportTickets(bearer: String, deviceFingerprint: String): Response<SupportTicketsResponseDto> =
      Response.success(SupportTicketsResponseDto(success = true, items = tickets.toList()))

    override suspend fun searchFaqs(bearer: String, deviceFingerprint: String, query: String): Response<FaqListResponseDto> =
      Response.success(FaqListResponseDto(success = true))

    override suspend fun getSystemStatus(bearer: String, deviceFingerprint: String): Response<SystemStatusResponseDto> =
      Response.success(SystemStatusResponseDto(success = true))

    override suspend fun getLegalDocuments(bearer: String, deviceFingerprint: String): Response<LegalDocumentsResponseDto> =
      Response.success(LegalDocumentsResponseDto(success = true))

    override suspend fun checkAppVersion(bearer: String, deviceFingerprint: String, version: String): Response<AppVersionResponseDto> =
      Response.success(AppVersionResponseDto(success = true, latestVersion = version))

    override suspend fun getChatPrivacy(bearer: String, deviceFingerprint: String): Response<telefam.company.data.network.ChatPrivacyResponseDto> =
      Response.success(telefam.company.data.network.ChatPrivacyResponseDto(success = true))

    override suspend fun updateChatPrivacy(bearer: String, deviceFingerprint: String, body: telefam.company.data.network.ChatPrivacySettingsDto): Response<telefam.company.data.network.ChatPrivacyResponseDto> =
      Response.success(telefam.company.data.network.ChatPrivacyResponseDto(success = true, chatPrivacy = body))

    override suspend fun updateChatTheme(bearer: String, deviceFingerprint: String, body: telefam.company.data.network.UpdateChatThemeRequestDto): Response<telefam.company.data.network.ChatPrivacyResponseDto> =
      Response.success(telefam.company.data.network.ChatPrivacyResponseDto(success = true))

    override suspend fun logout(bearer: String, deviceFingerprint: String, body: LogoutRequestDto): Response<GenericSuccessDto> {
      lastLogoutAllDevices = body.allDevices
      return Response.success(GenericSuccessDto(success = true))
    }

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
    override suspend fun reportUser(bearer: String, deviceFingerprint: String, userId: String, body: UserReportRequestDto): Response<UserReportResponseDto> = Response.success(UserReportResponseDto(success = true))
    override suspend fun downloadMedia(url: String): Response<ResponseBody> = Response.success(ByteArray(0).toResponseBody("application/octet-stream".toMediaType()))
    override suspend fun getPendingLoginRequests(bearer: String, deviceFingerprint: String): Response<telefam.company.data.network.PendingLoginRequestsResponseDto> = Response.success(telefam.company.data.network.PendingLoginRequestsResponseDto(success = true))
    override suspend fun getLoginRequestStatus(deviceFingerprint: String, pollToken: String, requestId: String): Response<telefam.company.data.network.LoginApprovalStatusResponseDto> = Response.success(telefam.company.data.network.LoginApprovalStatusResponseDto(success = true, status = "pending", remainingSeconds = 60L))
    override suspend fun respondToLoginRequest(bearer: String, deviceFingerprint: String, requestId: String, body: telefam.company.data.network.RespondLoginRequestDto): Response<telefam.company.data.network.RespondLoginRequestResponseDto> = Response.success(telefam.company.data.network.RespondLoginRequestResponseDto(success = true, requestId = requestId))
    override suspend fun syncDeviceContext(bearer: String, deviceFingerprint: String, body: telefam.company.data.network.DeviceContextTelemetryDto): Response<telefam.company.data.network.DeviceContextTelemetryResponseDto> = Response.success(telefam.company.data.network.DeviceContextTelemetryResponseDto(success = true))

    override suspend fun getSyncSettings(bearer: String, deviceFingerprint: String): Response<telefam.company.data.network.SyncSettingsResponseDto> =
      Response.success(telefam.company.data.network.SyncSettingsResponseDto(success = true, syncPreferences = storedSyncPrefs))

    override suspend fun updateSyncSettings(bearer: String, deviceFingerprint: String, body: telefam.company.data.network.SyncPreferencesDto): Response<telefam.company.data.network.SyncSettingsResponseDto> {
      syncPrefSavedCount++
      storedSyncPrefs = body.copy(lastSyncedAtMs = System.currentTimeMillis())
      return Response.success(telefam.company.data.network.SyncSettingsResponseDto(success = true, syncPreferences = storedSyncPrefs))
    }

    override suspend fun syncNow(bearer: String, deviceFingerprint: String, body: telefam.company.data.network.ManualSyncRequestDto): Response<telefam.company.data.network.ManualSyncResponseDto> {
      manualSyncCount++
      val now = System.currentTimeMillis()
      val updatedPrefs = (body.syncPreferences ?: storedSyncPrefs).copy(
        lastSyncedAtMs = now,
        lastSyncStatus = "synced",
        totalSyncedOps = storedSyncPrefs.totalSyncedOps + body.operations.size.toLong(),
      )
      storedSyncPrefs = updatedPrefs
      return Response.success(
        telefam.company.data.network.ManualSyncResponseDto(
          success = true,
          syncedOpsCount = body.operations.size,
          ackedOpIds = body.operations.map { it.clientOpId },
          syncPreferences = updatedPrefs,
          privacy = body.privacy,
          notifications = body.notifications,
          contentPreferences = body.contentPreferences,
          chatPrivacy = body.chatPrivacy,
          serverEpochMs = now,
        ),
      )
    }
  }
}
