package telefam.company

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.delay
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
import androidx.compose.ui.test.assertIsNotEnabled
import telefam.company.core.ContactRelationshipState
import telefam.company.core.OwnerProfileTab
import telefam.company.core.PostFontOption
import telefam.company.core.PostMode
import telefam.company.core.PostPrivacy
import telefam.company.core.ProfileBackgroundItem
import telefam.company.core.ProfilePostFilter
import telefam.company.core.formatProfileMetricCount
import telefam.company.data.local.DeviceIdentityStore
import telefam.company.data.local.TelefamDatabase
import telefam.company.data.network.AuditEventDto
import telefam.company.data.network.AuthSessionResponseDto
import telefam.company.data.network.ContactActionDto
import telefam.company.data.network.ContactItemDto
import telefam.company.data.network.ContactSyncBatchRequestDto
import telefam.company.data.network.ContactsPageResponseDto
import telefam.company.data.network.CreatePostRequestDto
import telefam.company.data.network.CreatePostResponseDto
import telefam.company.data.network.CreatedPostDto
import telefam.company.data.network.FeedInteractionBatchRequestDto
import telefam.company.data.network.FeedInteractionRequestDto
import telefam.company.data.network.FeedInteractionResponseDto
import telefam.company.data.network.FeedItemDto
import telefam.company.data.network.FeedPageResponseDto
import telefam.company.data.network.FeedSearchResponseDto
import telefam.company.data.network.FollowMutationResponseDto
import telefam.company.data.network.GenericSuccessDto
import telefam.company.data.network.HashtagsSearchResponseDto
import telefam.company.data.network.MediaUploadResponseDto
import telefam.company.data.network.NetworkQualityMonitor
import telefam.company.data.network.OAuthDto
import telefam.company.data.network.ProfileBackgroundAttachDto
import telefam.company.data.network.ProfileBackgroundItemDto
import telefam.company.data.network.ProfileBackgroundListResponseDto
import telefam.company.data.network.ProfileBackgroundReorderDto
import telefam.company.data.network.ProfileBackgroundUploadResponseDto
import telefam.company.data.network.ProfileSetupDto
import telefam.company.data.network.ProfileUpdateRequestDto
import telefam.company.data.network.ProfileUpdateResponseDto
import telefam.company.data.network.RefreshTokenDto
import telefam.company.data.network.RequestOtpDto
import telefam.company.data.network.RequestOtpResponseDto
import telefam.company.data.network.SongsSearchResponseDto
import telefam.company.data.network.SyncBatchRequestDto
import telefam.company.data.network.SyncBatchResponseDto
import telefam.company.data.network.TelefamApiService
import telefam.company.data.network.UpdatePostRequestDto
import telefam.company.data.network.UserBlockResponseDto
import telefam.company.data.network.UserConnectionsResponseDto
import telefam.company.data.network.UserProfileResponseDto
import telefam.company.data.network.UserReportRequestDto
import telefam.company.data.network.UserReportResponseDto
import telefam.company.data.network.AppVersionResponseDto
import telefam.company.data.network.ContentPreferencesDto
import telefam.company.data.network.FaqListResponseDto
import telefam.company.data.network.LegalDocumentsResponseDto
import telefam.company.data.network.LogoutRequestDto
import telefam.company.data.network.NotificationSettingsDto
import telefam.company.data.network.PrivacySettingsDto
import telefam.company.data.network.ProblemReportRequestDto
import telefam.company.data.network.ProblemReportResponseDto
import telefam.company.data.network.RestrictedAccountsResponseDto
import telefam.company.data.network.SettingsBundleResponseDto
import telefam.company.data.network.SupportTicketsResponseDto
import telefam.company.data.network.SystemStatusResponseDto
import telefam.company.data.network.UserMuteResponseDto
import telefam.company.data.network.UsernameCheckResponseDto
import telefam.company.data.network.VerifyLinkDto
import telefam.company.data.network.VerifyOtpDto
import telefam.company.data.repository.TelefamRepository
import telefam.company.ui.components.EditBackgroundsBottomSheet
import telefam.company.ui.components.EditProfileModalSheet
import telefam.company.ui.components.FeedMoreOptionsSheet
import telefam.company.ui.components.OwnerProfileSettingsSheet
import telefam.company.ui.screens.TelefamUserProfileScreen
import telefam.company.ui.theme.TelefamTheme
import telefam.company.ui.theme.ThemeMode
import telefam.company.ui.viewmodel.FeedUiItem
import telefam.company.ui.viewmodel.ProfileFormState
import telefam.company.ui.viewmodel.TelefamViewModel
import telefam.company.ui.viewmodel.UserProfileScreenState

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h915dp")
class TelefamProfileNavigationRobolectricTest {

  @get:Rule
  val composeRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var db: TelefamDatabase
  private lateinit var identityStore: DeviceIdentityStore
  private lateinit var networkMonitor: NetworkQualityMonitor
  private lateinit var fakeApi: FakeProfileApiService
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
    fakeApi = FakeProfileApiService()
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
    db.close()
  }

  @Test
  fun `metric formatter matches Alex Mwangi compact format across ranges`() {
    assertEquals("126", formatProfileMetricCount(126L))
    assertEquals("842", formatProfileMetricCount(842L))
    assertEquals("1.2K", formatProfileMetricCount(1_240L))
    assertEquals("2.3K", formatProfileMetricCount(2_310L))
    assertEquals("54.2K", formatProfileMetricCount(54_200L))
    assertEquals("1.2M", formatProfileMetricCount(1_210_000L))
  }

  @Test
  fun `repository caches profile and connections locally for offline access`() = runTest {
    val onlineProfile = repository.fetchUserProfile("u_alex")
    assertNotNull(onlineProfile)
    assertEquals("Alex Mwangi", onlineProfile?.fullName)
    assertEquals(54_200L, onlineProfile?.followersCount)
    assertEquals(2, onlineProfile?.posts?.size)

    val followersOnline = repository.fetchUserConnections("u_alex", isFollowers = true, page = 1, pageSize = 30)
    assertTrue(followersOnline.items.any { it.userId == "u_amara" })

    // Disconnect internet and confirm offline fallback reads from Room cache cleanly
    setNetworkOnline(false)
    networkMonitor.refreshStatus()

    val offlineProfile = repository.fetchUserProfile("u_alex")
    assertNotNull(offlineProfile)
    assertEquals("Alex Mwangi", offlineProfile?.fullName)
    assertEquals("@alexmwangi", "@${offlineProfile?.username}")
    assertEquals(2, offlineProfile?.posts?.size)

    val followersOffline = repository.fetchUserConnections("u_alex", isFollowers = true, page = 1, pageSize = 30)
    assertTrue(followersOffline.items.any { it.userId == "u_amara" })
  }

  @Test
  fun `viewmodel profile navigation backstack, post view counting, follow, block and report work end to end`() {
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
      assertTrue(vm.uiState.value.isAuthenticated)

      vm.openUserProfile("u_alex")
      waitUntilMain { vm.uiState.value.activeUserProfile?.fullName == "Alex Mwangi" }
      assertEquals(1, vm.uiState.value.profileBackStack.size)
      assertEquals("Alex Mwangi", vm.uiState.value.activeUserProfile?.fullName)

      // Open Followers modal sheet and tap nested user profile
      vm.openProfileConnections("u_alex", isFollowers = true)
      waitUntilMain { vm.uiState.value.activeUserProfile?.connections?.isNotEmpty() == true }
      assertEquals("followers", vm.uiState.value.activeUserProfile?.activeConnectionsType)
      assertEquals(1, vm.uiState.value.activeUserProfile?.connections?.size)

      vm.openUserProfile("u_amara")
      waitUntilMain { vm.uiState.value.profileBackStack.size == 2 && vm.uiState.value.activeUserProfile?.userId == "u_amara" }
      assertEquals(2, vm.uiState.value.profileBackStack.size)
      assertEquals("u_amara", vm.uiState.value.activeUserProfile?.userId)

      // Pop back to Alex
      vm.closeTopUserProfile()
      assertEquals(1, vm.uiState.value.profileBackStack.size)
      assertEquals("u_alex", vm.uiState.value.activeUserProfile?.userId)

      // Tapping grid video opens reusable feed viewer and increments viewsCount + sends view action
      val posts = vm.uiState.value.activeUserProfile!!.filteredAndSortedPosts
      assertEquals(2_300L, posts.first().viewsCount)
      vm.openProfilePostViewer(posts, 0)
      waitUntilMain { fakeApi.interactions.any { it.postId == posts.first().postId && it.action == "view" } }
      assertNotNull(vm.uiState.value.profileViewerItems)
      assertEquals(2_301L, vm.uiState.value.profileViewerItems!!.first().viewsCount)
      assertTrue(fakeApi.interactions.any { it.postId == posts.first().postId && it.action == "view" })

      // Back first closes viewer, second pops Alex profile
      vm.closeTopUserProfile()
      assertNull(vm.uiState.value.profileViewerItems)
      assertEquals(1, vm.uiState.value.profileBackStack.size)

      // Follow -> Block -> Unblock -> Report
      vm.toggleFollowOnProfile("u_alex")
      waitUntilMain { vm.uiState.value.activeUserProfile?.isFollowing == true }
      assertTrue(vm.uiState.value.activeUserProfile!!.isFollowing)

      vm.blockUserFromProfile("u_alex")
      waitUntilMain { vm.uiState.value.activeUserProfile?.isBlocked == true && "u_alex" in fakeApi.blockedIds }
      assertTrue(vm.uiState.value.activeUserProfile!!.isBlocked)
      assertTrue("u_alex" in fakeApi.blockedIds)

      vm.unblockUserFromProfile("u_alex")
      waitUntilMain { vm.uiState.value.activeUserProfile?.isBlocked == false && "u_alex" !in fakeApi.blockedIds }
      assertFalse(vm.uiState.value.activeUserProfile!!.isBlocked)
      assertFalse("u_alex" in fakeApi.blockedIds)

      vm.reportUserFromProfile("u_alex", "Spam or misleading", "Suspicious links")
      waitUntilMain { fakeApi.reports.isNotEmpty() }
      assertEquals(1, fakeApi.reports.size)
      assertEquals("Spam or misleading", fakeApi.reports.first().second.reason)

      vm.closeTopUserProfile()
      assertTrue(vm.uiState.value.profileBackStack.isEmpty())
    } finally {
      vmStore.clear()
      shadowOf(Looper.getMainLooper()).idle()
    }
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

  @Test
  fun `compose user profile screen displays metrics, coming soon triggers, filter and post tap`() {
    var followedClicked = false
    var messageClicked = false
    var subscribeClicked = false
    var openedPostIdx = -1
    var selectedFilter = ProfilePostFilter.ALL

    val samplePosts = listOf(
      FeedUiItem(
        postId = "p1",
        authorUserId = "u_alex",
        authorUsername = "alexmwangi",
        authorFullName = "Alex Mwangi",
        authorAvatarUrl = null,
        isFollowingAuthor = false,
        isOwner = false,
        mode = PostMode.VIDEO,
        textContent = "",
        caption = "Mountains hike",
        themeId = "crimson",
        fontOption = PostFontOption.CLASSIC,
        song = null,
        hashtags = emptyList(),
        photoUrls = emptyList(),
        primaryStreamUrl = "/v1/media/f/v1.mp4",
        rawDownloadUrl = "/v1/feeds/p1/download",
        thumbnailUrl = "",
        renditions = emptyList(),
        width = 1080,
        height = 1920,
        durationMs = 15_000L,
        muteOriginalAudio = false,
        contentId = "c1",
        likesCount = 100L,
        commentsCount = 0L,
        sharesCount = 0L,
        favouritesCount = 0L,
        repostsCount = 0L,
        viewsCount = 2_300L,
        isLiked = false,
        isFavourited = false,
        isReposted = false,
        repostedByUsername = "",
        recommendationLabel = "",
        createdAtMs = 2_000L,
      ),
    )

    val profileState = UserProfileScreenState(
      userId = "u_alex",
      fullName = "Alex Mwangi",
      username = "alexmwangi",
      bio = "Dreamer. Traveller. Coffee lover.",
      location = "Nairobi, Kenya",
      followingCount = 126L,
      followersCount = 54_200L,
      likesCount = 842L,
      isFollowing = false,
      followsYou = false,
      isOwner = false,
      isBlocked = false,
      relationshipState = ContactRelationshipState.FOLLOW,
      posts = samplePosts,
      isLoading = false,
    )

    composeRule.setContent {
      TelefamTheme(themeMode = ThemeMode.LIGHT) {
        TelefamUserProfileScreen(
          profile = profileState,
          isEmbeddedOwnProfileTab = false,
          onBack = {},
          onToggleFollow = { followedClicked = true },
          onMessageClick = { messageClicked = true },
          onSubscribeClick = { subscribeClicked = true },
          onOpenConnections = {},
          onDismissConnections = {},
          onLoadMoreConnections = {},
          onToggleConnectionFollow = {},
          onOpenUserProfile = {},
          onSelectFilter = { selectedFilter = it },
          onSelectSort = {},
          onOpenPostAtIndex = { _, idx -> openedPostIdx = idx },
          onBlockUser = {},
          onUnblockUser = {},
          onReportUser = { _, _ -> },
          onShowToast = {},
        )
      }
    }

    composeRule.onNodeWithText("Alex Mwangi").assertIsDisplayed()
    composeRule.onNodeWithText("@alexmwangi").assertIsDisplayed()
    composeRule.onNodeWithText("126").assertIsDisplayed()
    composeRule.onNodeWithText("54.2K").assertIsDisplayed()
    composeRule.onNodeWithText("842").assertIsDisplayed()
    composeRule.onNodeWithText("Nairobi, Kenya").assertIsDisplayed()

    composeRule.onNodeWithTag("profile_follow_button").performClick()
    composeRule.onNodeWithTag("profile_message_button").performClick()
    composeRule.onNodeWithTag("profile_subscribe_button").performClick()
    composeRule.onNodeWithTag("profile_posts_grid").performScrollToIndex(1)
    composeRule.onNodeWithTag("profile_post_tile_p1").performClick()

    assertTrue(followedClicked)
    assertTrue(messageClicked)
    assertTrue(subscribeClicked)
    assertEquals(0, openedPostIdx)
  }

  @Test
  fun `owner profile screen shows hero banner, 4 tabs, and switches private posts into Locked`() {
    var selectedTab = OwnerProfileTab.POSTS
    var editClicked = false
    var bgClicked = false

    val publicPost = FeedUiItem(
      postId = "owner_pub_1",
      authorUserId = "caller_1",
      authorUsername = "colloh",
      authorFullName = "Felistar",
      authorAvatarUrl = null,
      isFollowingAuthor = false,
      isOwner = true,
      privacy = PostPrivacy.PUBLIC,
      mode = PostMode.VIDEO,
      textContent = "",
      caption = "Public post",
      themeId = "crimson",
      fontOption = PostFontOption.CLASSIC,
      song = null,
      hashtags = emptyList(),
      photoUrls = emptyList(),
      primaryStreamUrl = "/v1/media/f/pub1.mp4",
      rawDownloadUrl = "/v1/feeds/owner_pub_1/download",
      thumbnailUrl = "",
      renditions = emptyList(),
      width = 1080,
      height = 1920,
      durationMs = 15_000L,
      muteOriginalAudio = false,
      contentId = "c_pub_1",
      likesCount = 3L,
      commentsCount = 0L,
      sharesCount = 0L,
      favouritesCount = 0L,
      repostsCount = 0L,
      viewsCount = 120L,
      isLiked = false,
      isFavourited = false,
      isReposted = false,
      repostedByUsername = "",
      recommendationLabel = "",
      createdAtMs = 5_000L,
    )
    val privatePost = publicPost.copy(
      postId = "owner_priv_1",
      caption = "Private post",
      privacy = PostPrivacy.PRIVATE,
      createdAtMs = 4_000L,
    )

    val ownerState = UserProfileScreenState(
      userId = "caller_1",
      fullName = "Felistar",
      username = "colloh",
      location = "Konoin, Kenya",
      followingCount = 1L,
      followersCount = 1L,
      likesCount = 5L,
      isOwner = true,
      backgrounds = listOf(ProfileBackgroundItem(mediaId = "bg1", url = "/v1/media/f/bg1.jpg")),
      posts = listOf(publicPost),
      lockedPosts = listOf(privatePost),
      activeOwnerTab = OwnerProfileTab.POSTS,
      isLoading = false,
    )

    composeRule.setContent {
      TelefamTheme(themeMode = ThemeMode.DARK) {
        TelefamUserProfileScreen(
          profile = ownerState,
          isEmbeddedOwnProfileTab = true,
          onBack = {},
          onToggleFollow = {},
          onMessageClick = {},
          onSubscribeClick = {},
          onOpenConnections = {},
          onDismissConnections = {},
          onLoadMoreConnections = {},
          onToggleConnectionFollow = {},
          onOpenUserProfile = {},
          onSelectOwnerTab = { selectedTab = it },
          onOpenEditProfile = { editClicked = true },
          onOpenBackgrounds = { bgClicked = true },
          onSelectFilter = {},
          onSelectSort = {},
          onOpenPostAtIndex = { _, _ -> },
          onBlockUser = {},
          onUnblockUser = {},
          onReportUser = { _, _ -> },
          onShowToast = {},
        )
      }
    }

    composeRule.onNodeWithTag("profile_hero_banner").assertIsDisplayed()
    composeRule.onNodeWithTag("profile_edit_background_button").performClick()
    composeRule.onNodeWithTag("profile_edit_button").performClick()
    composeRule.onNodeWithTag("profile_tab_locked").performClick()

    assertTrue(bgClicked)
    assertTrue(editClicked)
    assertEquals(OwnerProfileTab.LOCKED, selectedTab)
    assertEquals(listOf("owner_priv_1"), ownerState.copy(activeOwnerTab = OwnerProfileTab.LOCKED).filteredAndSortedPosts.map { it.postId })
  }

  @Test
  fun `owner settings sheet places logout button strictly first above theme`() {
    var loggedOut = false
    composeRule.setContent {
      TelefamTheme(themeMode = ThemeMode.DARK) {
        OwnerProfileSettingsSheet(
          themeMode = ThemeMode.DARK,
          username = "colloh",
          email = "colloh@telefam.test",
          onSignOut = { loggedOut = true },
          onToggleTheme = {},
          onDismiss = {},
        )
      }
    }

    composeRule.waitForIdle()
    val logoutBounds = composeRule.onNodeWithTag("settings_logout_button").fetchSemanticsNode().boundsInRoot
    val themeBounds = composeRule.onNodeWithTag("settings_theme_button").fetchSemanticsNode().boundsInRoot
    assertTrue("Log out must appear before Theme in Settings", logoutBounds.top < themeBounds.top)

    composeRule.onNodeWithTag("settings_logout_button").performClick()
    assertTrue(loggedOut)
  }

  @Test
  fun `backgrounds sheet enforces max 5 limit and displays fingerprints`() {
    val fiveBgs = (1..5).map { idx ->
      ProfileBackgroundItem(
        mediaId = "bg_$idx",
        url = "/v1/media/f/bg_$idx.jpg",
        sha256Fingerprint = "abcdef1234567890",
        dHashFingerprint = "1122334455667788",
        width = 1080,
        height = 1920,
        byteSize = 64_000,
      )
    }

    composeRule.setContent {
      TelefamTheme(themeMode = ThemeMode.LIGHT) {
        EditBackgroundsBottomSheet(
          backgrounds = fiveBgs,
          isUploading = false,
          errorMessage = null,
          onAddUris = {},
          onDeleteBackground = {},
          onReorderBackgrounds = {},
          onDismiss = {},
        )
      }
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithTag("backgrounds_add_button").assertIsNotEnabled()
    composeRule.onNodeWithTag("background_delete_bg_1").assertIsDisplayed()
  }

  @Test
  fun `edit profile modal locks username when 14 day cooldown is active`() {
    val futureUnlock = System.currentTimeMillis() + (7L * 24L * 60L * 60L * 1000L)
    val lockedOwnerState = UserProfileScreenState(
      userId = "caller_1",
      fullName = "Felistar",
      username = "colloh",
      location = "Konoin, Kenya",
      isOwner = true,
      nextUsernameChangeMs = futureUnlock,
    )

    composeRule.setContent {
      TelefamTheme(themeMode = ThemeMode.DARK) {
        EditProfileModalSheet(
          profile = lockedOwnerState,
          formState = ProfileFormState(),
          cropBitmap = null,
          isSaving = false,
          errorMessage = null,
          onPhotoPicked = {},
          onDismissCrop = {},
          onConfirmCrop = { _, _, _, _ -> },
          onSaveProfile = { _, _, _, _, _, _, _, _, _, _ -> },
          onDismiss = {},
        )
      }
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithTag("edit_profile_modal").assertIsDisplayed()
    composeRule.onNodeWithTag("edit_profile_username_input").assertIsNotEnabled()
  }

  @Test
  fun `feed more options sheet exposes owner privacy submenu with public, friends, and private`() {
    var changedPrivacy: PostPrivacy? = null
    val ownerFeedItem = FeedUiItem(
      postId = "p_owner_priv",
      authorUserId = "caller_1",
      authorUsername = "colloh",
      authorFullName = "Felistar",
      authorAvatarUrl = null,
      isFollowingAuthor = false,
      isOwner = true,
      mode = PostMode.VIDEO,
      textContent = "",
      caption = "Privacy test",
      themeId = "crimson",
      fontOption = PostFontOption.CLASSIC,
      song = null,
      hashtags = emptyList(),
      photoUrls = emptyList(),
      primaryStreamUrl = "/v1/media/f/v1.mp4",
      rawDownloadUrl = "/v1/feeds/p/download",
      thumbnailUrl = "",
      renditions = emptyList(),
      width = 1080,
      height = 1920,
      durationMs = 12_000L,
      muteOriginalAudio = false,
      contentId = "c1",
      likesCount = 1L,
      commentsCount = 0L,
      sharesCount = 0L,
      favouritesCount = 0L,
      repostsCount = 0L,
      viewsCount = 10L,
      isLiked = false,
      isFavourited = false,
      isReposted = false,
      repostedByUsername = "",
      recommendationLabel = "",
      createdAtMs = 1_000L,
      privacy = PostPrivacy.PUBLIC,
    )

    composeRule.setContent {
      TelefamTheme(themeMode = ThemeMode.DARK) {
        FeedMoreOptionsSheet(
          item = ownerFeedItem,
          onRepost = {},
          onDownload = {},
          onNotInterested = {},
          onReport = {},
          onBlock = {},
          onEdit = {},
          onChangePrivacy = { changedPrivacy = it },
          onDelete = {},
          onDismiss = {},
        )
      }
    }

    composeRule.onNodeWithTag("feed_menu_privacy").performClick()
    composeRule.onNodeWithTag("feed_privacy_option_private").performClick()
    assertEquals(PostPrivacy.PRIVATE, changedPrivacy)
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

  private class FakeProfileApiService : AbstractTestChatApiStub() {
    var followingAlex = false
    val blockedIds = mutableSetOf<String>()
    val reports = mutableListOf<Pair<String, UserReportRequestDto>>()
    val interactions = mutableListOf<FeedInteractionRequestDto>()
    val ownerBackgrounds = mutableListOf<ProfileBackgroundItemDto>()

    private val alexPosts = listOf(
      FeedItemDto(
        postId = "p_alex_1",
        authorUserId = "u_alex",
        authorUsername = "alexmwangi",
        authorFullName = "Alex Mwangi",
        mode = "video",
        caption = "Morning walk",
        videoUrl = "/v1/media/f/alex1.mp4",
        likesCount = 500L,
        viewsCount = 2_300L,
        createdAtMs = 20_000L,
      ),
      FeedItemDto(
        postId = "p_alex_2",
        authorUserId = "u_alex",
        authorUsername = "alexmwangi",
        authorFullName = "Alex Mwangi",
        mode = "video",
        caption = "Coast sunset",
        videoUrl = "/v1/media/f/alex2.mp4",
        likesCount = 342L,
        viewsCount = 1_800L,
        createdAtMs = 10_000L,
      ),
    )

    override suspend fun getUserProfile(
      bearer: String,
      deviceFingerprint: String,
      userId: String,
    ): Response<UserProfileResponseDto> {
      if (userId == "u_amara") {
        return Response.success(
          UserProfileResponseDto(
            success = true,
            userId = "u_amara",
            fullName = "Amara Okafor",
            username = "amara_reads",
            location = "Lagos, Nigeria",
            followingCount = 40L,
            followersCount = 900L,
            likesCount = 120L,
          ),
        )
      }
      val isBlk = userId in blockedIds
      return Response.success(
        UserProfileResponseDto(
          success = true,
          userId = userId,
          fullName = "Alex Mwangi",
          username = "alexmwangi",
          bio = "Dreamer. Traveller. Coffee lover.\nLiving life, one adventure at a time",
          location = "Nairobi, Kenya",
          countryIso = "KE",
          followingCount = 126L,
          followersCount = if (followingAlex) 54_201L else 54_200L,
          likesCount = 842L,
          isFollowing = followingAlex,
          followsYou = false,
          isOwner = false,
          relationshipState = if (followingAlex) "FOLLOWING" else "FOLLOW",
          isBlocked = isBlk,
          posts = if (isBlk) emptyList() else alexPosts,
        ),
      )
    }

    override suspend fun getUserConnections(
      bearer: String,
      deviceFingerprint: String,
      userId: String,
      type: String,
      page: Int,
      pageSize: Int,
    ): Response<UserConnectionsResponseDto> =
      Response.success(
        UserConnectionsResponseDto(
          success = true,
          items = listOf(
            ContactItemDto("u_amara", "Amara Okafor", "amara_reads", "Lagos", "NG", null, isFollowing = false, followsYou = true, relationshipState = "FOLLOW_BACK"),
          ),
          page = page,
          hasMore = false,
        ),
      )

    override suspend fun getUserFollowers(bearer: String, deviceFingerprint: String, userId: String, page: Int, pageSize: Int): Response<UserConnectionsResponseDto> =
      getUserConnections(bearer, deviceFingerprint, userId, "followers", page, pageSize)

    override suspend fun getUserFollowing(bearer: String, deviceFingerprint: String, userId: String, page: Int, pageSize: Int): Response<UserConnectionsResponseDto> =
      getUserConnections(bearer, deviceFingerprint, userId, "following", page, pageSize)

    override suspend fun blockUser(bearer: String, deviceFingerprint: String, userId: String): Response<UserBlockResponseDto> {
      blockedIds.add(userId)
      followingAlex = false
      return Response.success(UserBlockResponseDto(success = true, targetUserId = userId, isBlocked = true, isFollowing = false))
    }

    override suspend fun unblockUser(bearer: String, deviceFingerprint: String, userId: String): Response<UserBlockResponseDto> {
      blockedIds.remove(userId)
      return Response.success(UserBlockResponseDto(success = true, targetUserId = userId, isBlocked = false, isFollowing = false))
    }

    override suspend fun reportUser(bearer: String, deviceFingerprint: String, userId: String, body: UserReportRequestDto): Response<UserReportResponseDto> {
      reports.add(userId to body)
      return Response.success(UserReportResponseDto(success = true, targetUserId = userId, reason = body.reason))
    }

    override suspend fun followContact(bearer: String, deviceFingerprint: String, body: ContactActionDto): Response<FollowMutationResponseDto> {
      if (body.targetUserId == "u_alex") followingAlex = true
      return Response.success(FollowMutationResponseDto(success = true, targetUserId = body.targetUserId, isFollowing = true, relationshipState = "FOLLOWING"))
    }

    override suspend fun unfollowContact(bearer: String, deviceFingerprint: String, body: ContactActionDto): Response<FollowMutationResponseDto> {
      if (body.targetUserId == "u_alex") followingAlex = false
      return Response.success(FollowMutationResponseDto(success = true, targetUserId = body.targetUserId, isFollowing = false, relationshipState = "FOLLOW"))
    }

    override suspend fun interactFeed(bearer: String, deviceFingerprint: String, body: FeedInteractionRequestDto): Response<FeedInteractionResponseDto> {
      interactions.add(body)
      return Response.success(FeedInteractionResponseDto(success = true, postId = body.postId, action = body.action))
    }

    override suspend fun interactFeedBatch(bearer: String, deviceFingerprint: String, body: FeedInteractionBatchRequestDto): Response<SyncBatchResponseDto> {
      interactions.addAll(body.operations)
      return Response.success(SyncBatchResponseDto(success = true, ackedOpIds = body.operations.map { it.clientOpId }))
    }

    override suspend fun requestOtp(deviceFingerprint: String, ipFingerprint: String, body: RequestOtpDto): Response<RequestOtpResponseDto> =
      Response.success(RequestOtpResponseDto(success = true))

    override suspend fun verifyOtp(deviceFingerprint: String, ipFingerprint: String, body: VerifyOtpDto): Response<AuthSessionResponseDto> =
      Response.success(AuthSessionResponseDto(success = true))

    override suspend fun verifyLink(deviceFingerprint: String, ipFingerprint: String, body: VerifyLinkDto): Response<AuthSessionResponseDto> =
      Response.success(AuthSessionResponseDto(success = true))

    override suspend fun refreshToken(deviceFingerprint: String, body: RefreshTokenDto): Response<AuthSessionResponseDto> =
      Response.success(AuthSessionResponseDto(success = true))

    override suspend fun verifyGoogleOAuth(deviceFingerprint: String, ipFingerprint: String, body: OAuthDto): Response<AuthSessionResponseDto> =
      Response.success(AuthSessionResponseDto(success = true))

    override suspend fun checkUsername(username: String): Response<UsernameCheckResponseDto> =
      Response.success(UsernameCheckResponseDto(available = true))

    override suspend fun uploadAvatar(bearer: String, deviceFingerprint: String, cropCenterX: Float, cropCenterY: Float, cropScale: Float, imageBody: RequestBody): Response<MediaUploadResponseDto> =
      Response.success(MediaUploadResponseDto(success = true))

    override suspend fun uploadPostPhoto(bearer: String, deviceFingerprint: String, imageBody: RequestBody): Response<MediaUploadResponseDto> =
      Response.success(MediaUploadResponseDto(success = true))

    override suspend fun uploadPostVideo(bearer: String, deviceFingerprint: String, frameHashesCsv: String, durationMs: Long, trimStartMs: Long, trimEndMs: Long, width: Int, height: Int, videoPart: MultipartBody.Part, thumbnailPart: MultipartBody.Part?): Response<MediaUploadResponseDto> =
      Response.success(MediaUploadResponseDto(success = true))

    override suspend fun searchSongs(bearer: String, deviceFingerprint: String, query: String): Response<SongsSearchResponseDto> =
      Response.success(SongsSearchResponseDto(success = true))

    override suspend fun searchHashtags(bearer: String, deviceFingerprint: String, query: String): Response<HashtagsSearchResponseDto> =
      Response.success(HashtagsSearchResponseDto(success = true))

    override suspend fun createPost(bearer: String, deviceFingerprint: String, body: CreatePostRequestDto): Response<CreatePostResponseDto> =
      Response.success(CreatePostResponseDto(success = true, post = CreatedPostDto(postId = "p1", clientOpId = body.clientOpId)))

    override suspend fun updatePost(bearer: String, deviceFingerprint: String, postId: String, body: UpdatePostRequestDto): Response<CreatePostResponseDto> =
      Response.success(CreatePostResponseDto(success = true, post = CreatedPostDto(postId = postId)))

    override suspend fun deletePost(bearer: String, deviceFingerprint: String, postId: String): Response<GenericSuccessDto> =
      Response.success(GenericSuccessDto(success = true))

    override suspend fun getFeeds(bearer: String, deviceFingerprint: String, tab: String, page: Int, pageSize: Int): Response<FeedPageResponseDto> =
      Response.success(FeedPageResponseDto(success = true, tab = tab, page = page, pageSize = pageSize))

    override suspend fun searchFeeds(bearer: String, deviceFingerprint: String, query: String, shortcut: String, page: Int, pageSize: Int): Response<FeedSearchResponseDto> =
      Response.success(FeedSearchResponseDto(success = true, query = query, shortcut = shortcut, page = page, pageSize = pageSize))

    override suspend fun setupProfile(bearer: String, deviceFingerprint: String, body: ProfileSetupDto): Response<GenericSuccessDto> =
      Response.success(GenericSuccessDto(success = true))

    override suspend fun recordAuditEvent(bearer: String, deviceFingerprint: String, body: AuditEventDto): Response<GenericSuccessDto> =
      Response.success(GenericSuccessDto(success = true))

    override suspend fun syncBatch(bearer: String, deviceFingerprint: String, body: SyncBatchRequestDto): Response<SyncBatchResponseDto> =
      Response.success(SyncBatchResponseDto(success = true))

    override suspend fun listContacts(bearer: String, deviceFingerprint: String, filter: String, query: String, page: Int, pageSize: Int): Response<ContactsPageResponseDto> =
      Response.success(ContactsPageResponseDto(success = true))

    override suspend fun syncContactActions(bearer: String, deviceFingerprint: String, body: ContactSyncBatchRequestDto): Response<SyncBatchResponseDto> =
      Response.success(SyncBatchResponseDto(success = true))

    override suspend fun uploadProfileBackground(
      bearer: String,
      deviceFingerprint: String,
      imageBody: RequestBody,
    ): Response<ProfileBackgroundUploadResponseDto> {
      val item = ProfileBackgroundItemDto(
        mediaId = "bg_${ownerBackgrounds.size + 1}",
        url = "/v1/media/f/bg_${ownerBackgrounds.size + 1}.jpg",
      )
      if (ownerBackgrounds.size < 5) ownerBackgrounds.add(item)
      return Response.success(
        ProfileBackgroundUploadResponseDto(
          success = true,
          mediaId = item.mediaId,
          background = item,
          backgrounds = ownerBackgrounds.toList(),
        ),
      )
    }

    override suspend fun attachProfileBackground(
      bearer: String,
      deviceFingerprint: String,
      body: ProfileBackgroundAttachDto,
    ): Response<ProfileBackgroundListResponseDto> =
      Response.success(ProfileBackgroundListResponseDto(success = true, backgrounds = ownerBackgrounds.toList()))

    override suspend fun reorderProfileBackgrounds(
      bearer: String,
      deviceFingerprint: String,
      body: ProfileBackgroundReorderDto,
    ): Response<ProfileBackgroundListResponseDto> {
      val byId = ownerBackgrounds.associateBy { it.mediaId }
      val reordered = body.mediaIds.mapNotNull { byId[it] }
      ownerBackgrounds.clear()
      ownerBackgrounds.addAll(reordered)
      return Response.success(ProfileBackgroundListResponseDto(success = true, backgrounds = ownerBackgrounds.toList()))
    }

    override suspend fun deleteProfileBackground(
      bearer: String,
      deviceFingerprint: String,
      mediaId: String,
    ): Response<ProfileBackgroundListResponseDto> {
      ownerBackgrounds.removeAll { it.mediaId == mediaId }
      return Response.success(ProfileBackgroundListResponseDto(success = true, backgrounds = ownerBackgrounds.toList()))
    }

    override suspend fun updateProfile(
      bearer: String,
      deviceFingerprint: String,
      body: ProfileUpdateRequestDto,
    ): Response<ProfileUpdateResponseDto> =
      Response.success(
        ProfileUpdateResponseDto(
          success = true,
          username = body.username,
          fullName = body.fullName,
          usernameChangedAtMs = System.currentTimeMillis(),
          nextUsernameChangeMs = System.currentTimeMillis() + (14L * 24L * 60L * 60L * 1000L),
        ),
      )

    override suspend fun muteUser(bearer: String, deviceFingerprint: String, userId: String): Response<UserMuteResponseDto> =
      Response.success(UserMuteResponseDto(success = true, userId = userId, isMuted = true))

    override suspend fun unmuteUser(bearer: String, deviceFingerprint: String, userId: String): Response<UserMuteResponseDto> =
      Response.success(UserMuteResponseDto(success = true, userId = userId, isMuted = false))

    override suspend fun getSettings(bearer: String, deviceFingerprint: String): Response<SettingsBundleResponseDto> =
      Response.success(SettingsBundleResponseDto(success = true))

    override suspend fun updatePrivacySettings(bearer: String, deviceFingerprint: String, body: PrivacySettingsDto): Response<SettingsBundleResponseDto> =
      Response.success(SettingsBundleResponseDto(success = true, privacy = body))

    override suspend fun updateNotificationSettings(bearer: String, deviceFingerprint: String, body: NotificationSettingsDto): Response<SettingsBundleResponseDto> =
      Response.success(SettingsBundleResponseDto(success = true, notifications = body))

    override suspend fun updateContentPreferences(bearer: String, deviceFingerprint: String, body: ContentPreferencesDto): Response<SettingsBundleResponseDto> =
      Response.success(SettingsBundleResponseDto(success = true, contentPreferences = body))

    override suspend fun listBlockedAccounts(bearer: String, deviceFingerprint: String, query: String): Response<RestrictedAccountsResponseDto> =
      Response.success(RestrictedAccountsResponseDto(success = true))

    override suspend fun listMutedAccounts(bearer: String, deviceFingerprint: String, query: String): Response<RestrictedAccountsResponseDto> =
      Response.success(RestrictedAccountsResponseDto(success = true))

    override suspend fun submitProblemReport(bearer: String, deviceFingerprint: String, body: ProblemReportRequestDto): Response<ProblemReportResponseDto> =
      Response.success(ProblemReportResponseDto(success = true, referenceNumber = "TF-TEST-1"))

    override suspend fun listSupportTickets(bearer: String, deviceFingerprint: String): Response<SupportTicketsResponseDto> =
      Response.success(SupportTicketsResponseDto(success = true))

    override suspend fun searchFaqs(bearer: String, deviceFingerprint: String, query: String): Response<FaqListResponseDto> =
      Response.success(FaqListResponseDto(success = true))

    override suspend fun getSystemStatus(bearer: String, deviceFingerprint: String): Response<SystemStatusResponseDto> =
      Response.success(SystemStatusResponseDto(success = true))

    override suspend fun getLegalDocuments(bearer: String, deviceFingerprint: String): Response<LegalDocumentsResponseDto> =
      Response.success(LegalDocumentsResponseDto(success = true))

    override suspend fun checkAppVersion(bearer: String, deviceFingerprint: String, version: String): Response<AppVersionResponseDto> =
      Response.success(AppVersionResponseDto(success = true, latestVersion = version))

    override suspend fun logout(bearer: String, deviceFingerprint: String, body: LogoutRequestDto): Response<GenericSuccessDto> =
      Response.success(GenericSuccessDto(success = true))

    override suspend fun getChatPrivacy(bearer: String, deviceFingerprint: String): Response<telefam.company.data.network.ChatPrivacyResponseDto> =
      Response.success(telefam.company.data.network.ChatPrivacyResponseDto(success = true))

    override suspend fun updateChatPrivacy(bearer: String, deviceFingerprint: String, body: telefam.company.data.network.ChatPrivacySettingsDto): Response<telefam.company.data.network.ChatPrivacyResponseDto> =
      Response.success(telefam.company.data.network.ChatPrivacyResponseDto(success = true, chatPrivacy = body))

    override suspend fun updateChatTheme(bearer: String, deviceFingerprint: String, body: telefam.company.data.network.UpdateChatThemeRequestDto): Response<telefam.company.data.network.ChatPrivacyResponseDto> =
      Response.success(telefam.company.data.network.ChatPrivacyResponseDto(success = true))

    override suspend fun downloadMedia(url: String): Response<ResponseBody> =
      Response.success(ByteArray(0).toResponseBody("application/octet-stream".toMediaType()))

    override suspend fun getPendingLoginRequests(bearer: String, deviceFingerprint: String): Response<telefam.company.data.network.PendingLoginRequestsResponseDto> =
      Response.success(telefam.company.data.network.PendingLoginRequestsResponseDto(success = true))

    override suspend fun getLoginRequestStatus(deviceFingerprint: String, pollToken: String, requestId: String): Response<telefam.company.data.network.LoginApprovalStatusResponseDto> =
      Response.success(telefam.company.data.network.LoginApprovalStatusResponseDto(success = true, status = "pending", remainingSeconds = 60L))

    override suspend fun respondToLoginRequest(bearer: String, deviceFingerprint: String, requestId: String, body: telefam.company.data.network.RespondLoginRequestDto): Response<telefam.company.data.network.RespondLoginRequestResponseDto> =
      Response.success(telefam.company.data.network.RespondLoginRequestResponseDto(success = true, requestId = requestId))

    override suspend fun syncDeviceContext(bearer: String, deviceFingerprint: String, body: telefam.company.data.network.DeviceContextTelemetryDto): Response<telefam.company.data.network.DeviceContextTelemetryResponseDto> =
      Response.success(telefam.company.data.network.DeviceContextTelemetryResponseDto(success = true))

    override suspend fun getSyncSettings(bearer: String, deviceFingerprint: String): Response<telefam.company.data.network.SyncSettingsResponseDto> =
      Response.success(telefam.company.data.network.SyncSettingsResponseDto(success = true))

    override suspend fun updateSyncSettings(bearer: String, deviceFingerprint: String, body: telefam.company.data.network.SyncPreferencesDto): Response<telefam.company.data.network.SyncSettingsResponseDto> =
      Response.success(telefam.company.data.network.SyncSettingsResponseDto(success = true, syncPreferences = body))

    override suspend fun syncNow(bearer: String, deviceFingerprint: String, body: telefam.company.data.network.ManualSyncRequestDto): Response<telefam.company.data.network.ManualSyncResponseDto> =
      Response.success(telefam.company.data.network.ManualSyncResponseDto(success = true, ackedOpIds = body.operations.map { it.clientOpId }))
  }
}
