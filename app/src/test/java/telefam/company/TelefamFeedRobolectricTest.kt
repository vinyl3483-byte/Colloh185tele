package telefam.company

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
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
import telefam.company.core.AdaptiveStreamSelector
import telefam.company.core.FeedAspectRatioCalculator
import telefam.company.core.FeedInteractionAction
import telefam.company.core.FeedTab
import telefam.company.core.FeedVideoRendition
import telefam.company.core.NetworkTier
import telefam.company.core.PostMode
import telefam.company.core.PostSongTrack
import telefam.company.core.formatCompactCount
import telefam.company.data.local.DeviceIdentityStore
import telefam.company.data.local.TelefamDatabase
import telefam.company.data.network.AuditEventDto
import telefam.company.data.network.AuthSessionResponseDto
import telefam.company.data.network.ContactActionDto
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
import telefam.company.data.network.FeedVideoRenditionDto
import telefam.company.data.network.FollowMutationResponseDto
import telefam.company.data.network.GenericSuccessDto
import telefam.company.data.network.HashtagsSearchResponseDto
import telefam.company.data.network.MediaUploadResponseDto
import telefam.company.data.network.NetworkQualityMonitor
import telefam.company.data.network.OAuthDto
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
import telefam.company.data.network.SearchShortcutDto
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
import telefam.company.data.repository.PostPublishResult
import telefam.company.data.repository.TelefamRepository
import telefam.company.ui.screens.ReusableVideoFeedScreen
import telefam.company.ui.theme.TelefamTheme
import telefam.company.ui.theme.ThemeMode
import telefam.company.ui.viewmodel.FeedUiItem
import telefam.company.ui.viewmodel.TelefamViewModel

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TelefamFeedRobolectricTest {

  @get:Rule
  val composeRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var db: TelefamDatabase
  private lateinit var identityStore: DeviceIdentityStore
  private lateinit var networkMonitor: NetworkQualityMonitor
  private lateinit var fakeApi: FakeFeedApiService
  private lateinit var repository: TelefamRepository

  @Before
  fun setup() = runTest {
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
    fakeApi = FakeFeedApiService()
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
  fun `aspect ratio calculator preserves proportions across portrait square landscape and ultrawide without clipping`() {
    val containerW = 1080f
    val containerH = 1920f

    // 9:16 portrait fills container cleanly
    val p916 = FeedAspectRatioCalculator.computeFittedSize(containerW, containerH, 1080, 1920)
    assertEquals(1080f, p916.width, 1f)
    assertEquals(1920f, p916.height, 1f)

    // 1:1 square preserves 1080x1080 centered without stretching
    val p11 = FeedAspectRatioCalculator.computeFittedSize(containerW, containerH, 1080, 1080)
    assertEquals(1080f, p11.width, 1f)
    assertEquals(1080f, p11.height, 1f)

    // 16:9 landscape fits width and scales height proportionally
    val p169 = FeedAspectRatioCalculator.computeFittedSize(containerW, containerH, 1920, 1080)
    assertEquals(1080f, p169.width, 1f)
    assertEquals(607.5f, p169.height, 1f)

    // 21:9 ultrawide fits width and scales height proportionally
    val p219 = FeedAspectRatioCalculator.computeFittedSize(containerW, containerH, 2520, 1080)
    assertEquals(1080f, p219.width, 1f)
    assertEquals(462.8f, p219.height, 2f)

    // Wide desktop container with portrait 9:16 video fits height without cropping
    val desk = FeedAspectRatioCalculator.computeFittedSize(1600f, 900f, 1080, 1920)
    assertEquals(900f, desk.height, 1f)
    assertEquals(506.25f, desk.width, 1f)
  }

  @Test
  fun `adaptive stream selector and compact counter formatting match reference layout`() {
    assertEquals("0", formatCompactCount(0L))
    assertEquals("2,345", formatCompactCount(2_345L))
    assertEquals("32.1K", formatCompactCount(32_140L))
    assertEquals("128.4K", formatCompactCount(128_420L))
    assertEquals("1.2M", formatCompactCount(1_240_000L))

    val renditions = listOf(
      FeedVideoRendition("360p", 360, 640, 450, "/v1/media/f/vid_360.mp4", 400_000),
      FeedVideoRendition("720p", 720, 1280, 1400, "/v1/media/f/vid_720.mp4", 1_200_000),
      FeedVideoRendition("1080p", 1080, 1920, 3400, "/v1/media/f/vid_1080.mp4", 2_800_000),
    )

    val goodUrl = AdaptiveStreamSelector.selectRenditionUrl("https://api.telefam.app", "", renditions, NetworkTier.GOOD)
    val weakUrl = AdaptiveStreamSelector.selectRenditionUrl("https://api.telefam.app", "", renditions, NetworkTier.WEAK)
    val offlineUrl = AdaptiveStreamSelector.selectRenditionUrl("https://api.telefam.app", "", renditions, NetworkTier.OFFLINE)

    assertEquals("https://api.telefam.app/v1/media/f/vid_720.mp4", goodUrl)
    assertEquals("https://api.telefam.app/v1/media/f/vid_360.mp4", weakUrl)
    assertEquals("https://api.telefam.app/v1/media/f/vid_360.mp4", offlineUrl)
  }

  @Test
  fun `offline feed interactions queue optimistically and flush batch on reconnect`() = runTest {
    repository.loadFeedPage(FeedTab.FOR_YOU, page = 1, pageSize = 15)
    val initial = db.telefamDao().observeFeedPosts(FeedTab.FOR_YOU.apiParam).first()
    assertEquals(2, initial.size)
    assertEquals("post_owner", initial[0].postId)

    // Go offline and perform Like + Repost + Not Interested
    setNetworkOnline(false)
    networkMonitor.refreshStatus()

    repository.performFeedInteraction("post_owner", FeedInteractionAction.LIKE)
    repository.performFeedInteraction("post_owner", FeedInteractionAction.REPOST)
    repository.performFeedInteraction("post_other", FeedInteractionAction.NOT_INTERESTED)

    val afterOffline = db.telefamDao().observeFeedPosts(FeedTab.FOR_YOU.apiParam).first()
    assertEquals(1, afterOffline.size) // post_other hidden locally immediately
    assertTrue(afterOffline[0].isLiked)
    assertTrue(afterOffline[0].isReposted)
    assertEquals(128_421L, afterOffline[0].likesCount)
    assertEquals(3, db.telefamDao().getReadyFeedInteractions(Long.MAX_VALUE, 50).size)

    // Reconnect and flush offline queue
    setNetworkOnline(true)
    networkMonitor.refreshStatus()
    repository.flushOfflineQueue()

    assertTrue(db.telefamDao().getReadyFeedInteractions(Long.MAX_VALUE, 50).isEmpty())
    assertTrue(fakeApi.batchedActions.any { it.postId == "post_owner" && it.action == "like" })
    assertTrue(fakeApi.batchedActions.any { it.postId == "post_owner" && it.action == "repost" })
    assertTrue(fakeApi.batchedActions.any { it.postId == "post_other" && it.action == "not_interested" })
  }

  @Test
  fun `owner update delete and viewmodel post autofill from hashtag song edit work properly`() = runTest {
    repository.loadFeedPage(FeedTab.FOR_YOU, page = 1, pageSize = 15)

    // Update owner post
    val updated = repository.updateOwnerPost(
      postId = "post_owner",
      textContent = "",
      caption = "Updated night lights!",
      themeId = "crimson",
      fontId = "classic",
      song = PostSongTrack("s1", "City Lights", "Rohan"),
      taggedUserIds = emptyList(),
      hashtags = listOf("citylights", "nightvibes"),
      muteOriginalAudio = true,
    )
    assertTrue(updated is PostPublishResult.Uploaded)
    assertEquals("Updated night lights!", db.telefamDao().getFeedPost("post_owner")?.caption)

    // ViewModel autofill state transitions
    val vmStore = ViewModelStore()
    val vm = ViewModelProvider(
      vmStore,
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = TelefamViewModel(repository) as T
      },
    )[TelefamViewModel::class.java]
    try {
      vm.openPostWithHashtag("#Explore!", PostMode.VIDEO)
      assertTrue(vm.uiState.value.postComposer.isOpen)
      assertNull(vm.uiState.value.postComposer.editingPostId)
      assertEquals(listOf("explore"), vm.uiState.value.postComposer.selectedHashtags)

      val song = PostSongTrack("trk_9", "Midnight", "Rohan")
      vm.openPostWithSong(song, PostMode.VIDEO)
      assertTrue(vm.uiState.value.postComposer.isOpen)
      assertEquals(song, vm.uiState.value.postComposer.selectedSong)

      vm.openPostForEdit(
        FeedUiItem(
          postId = "post_owner",
          authorUserId = "caller_1",
          authorUsername = "caller",
          authorFullName = "Caller",
          authorAvatarUrl = null,
          isFollowingAuthor = false,
          isOwner = true,
          mode = PostMode.VIDEO,
          textContent = "",
          caption = "Updated night lights!",
          themeId = "crimson",
          fontOption = telefam.company.core.PostFontOption.CLASSIC,
          song = song,
          hashtags = listOf("citylights"),
          photoUrls = emptyList(),
          primaryStreamUrl = "/v1/media/f/vid.mp4",
          rawDownloadUrl = "/v1/feeds/post_owner/download",
          thumbnailUrl = "/v1/media/f/thumb.jpg",
          renditions = emptyList(),
          width = 1080,
          height = 1920,
          durationMs = 15_000L,
          muteOriginalAudio = true,
          contentId = "vid_123",
          likesCount = 10L,
          commentsCount = 0L,
          sharesCount = 0L,
          favouritesCount = 0L,
          repostsCount = 0L,
          isLiked = false,
          isFavourited = false,
          isReposted = false,
          repostedByUsername = "",
          recommendationLabel = "You might like this",
          createdAtMs = 1_000L,
        ),
      )
      assertTrue(vm.uiState.value.postComposer.isOpen)
      assertEquals("post_owner", vm.uiState.value.postComposer.editingPostId)
      assertTrue(vm.uiState.value.postComposer.canShare)
    } finally {
      vmStore.clear()
    }

    // Delete owner post
    val deleted = repository.deleteOwnerPost("post_owner")
    assertTrue(deleted)
    assertNull(db.telefamDao().getFeedPost("post_owner"))
    assertFalse(db.telefamDao().observeFeedPosts(FeedTab.FOR_YOU.apiParam).first().any { it.postId == "post_owner" })
  }

  @Test
  fun `resolveMediaUrl preserves local filesystem paths and prefixes backend relative paths`() {
    assertEquals(
      "file:///data/user/0/com.telefam.company.app/files/media/avatar.jpg",
      AdaptiveStreamSelector.resolveMediaUrl(
        "https://api.telefam.app",
        "/data/user/0/com.telefam.company.app/files/media/avatar.jpg",
      ),
    )
    assertEquals(
      "https://api.telefam.app/v1/media/f/vid_720.mp4",
      AdaptiveStreamSelector.resolveMediaUrl("https://api.telefam.app/", "/v1/media/f/vid_720.mp4"),
    )
    assertEquals(
      "file:///storage/emulated/0/Movies/clip.mp4",
      AdaptiveStreamSelector.resolveMediaUrl("https://api.telefam.app", "/storage/emulated/0/Movies/clip.mp4"),
    )
    assertEquals(
      "https://cdn.telefam.app/stream/item.mp4",
      AdaptiveStreamSelector.resolveMediaUrl("https://api.telefam.app", "https://cdn.telefam.app/stream/item.mp4"),
    )
  }

  @Test
  fun `processAndUploadAvatar adopts server returned mediaId in result and local cache`() = runTest {
    val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
    val uploaded = repository.processAndUploadAvatar(
      userId = "caller_1",
      sourceBitmap = bmp,
      centerX = 0.5f,
      centerY = 0.5f,
      zoomScale = 1.0f,
    )
    assertEquals("srv_avatar_media_99", uploaded.mediaId)
    val cursor = db.openHelper.readableDatabase.query(
      "SELECT mediaId FROM media_uploads WHERE mediaId = ?",
      arrayOf("srv_avatar_media_99"),
    )
    try {
      assertTrue(cursor.moveToFirst())
      assertEquals("srv_avatar_media_99", cursor.getString(0))
    } finally {
      cursor.close()
    }
  }

  @Test
  fun `reusable feed screen renders multi ratio posts and toggles read more and read less on long captions`() {
    val longCaption =
      "Capturing late-night city skyline reflections across every single district while testing wide landscape framing and crystal clear sound design for Telefam creators."
    val items = listOf(
      FeedUiItem(
        postId = "post_16_9",
        authorUserId = "u_1",
        authorUsername = "rohan.verma",
        authorFullName = "Rohan Verma",
        authorAvatarUrl = "https://api.telefam.app/v1/media/f/av1.jpg",
        isFollowingAuthor = false,
        isOwner = false,
        mode = PostMode.VIDEO,
        textContent = "",
        caption = longCaption,
        themeId = "crimson",
        fontOption = telefam.company.core.PostFontOption.CLASSIC,
        song = PostSongTrack("s_1", "Night Drive", "FM-84", previewUrl = "https://api.telefam.app/v1/media/f/s1.mp3"),
        hashtags = listOf("nightvibes", "citylights"),
        photoUrls = emptyList(),
        primaryStreamUrl = "/v1/media/f/vid_landscape.mp4",
        rawDownloadUrl = "/v1/feeds/post_16_9/download",
        thumbnailUrl = "/v1/media/f/thumb_landscape.jpg",
        renditions = listOf(
          FeedVideoRendition("720p", 1280, 720, 1400, "/v1/media/f/vid_landscape.mp4", 1_000_000),
        ),
        width = 1920,
        height = 1080,
        durationMs = 15_000L,
        muteOriginalAudio = false,
        contentId = "cid_16_9",
        likesCount = 128_420L,
        commentsCount = 2_345L,
        sharesCount = 8_765L,
        favouritesCount = 32_100L,
        repostsCount = 12L,
        isLiked = false,
        isFavourited = false,
        isReposted = false,
        repostedByUsername = "",
        recommendationLabel = "You might like this",
        createdAtMs = 1_000L,
      ),
      FeedUiItem(
        postId = "post_1_1_photos",
        authorUserId = "u_2",
        authorUsername = "amara_reads",
        authorFullName = "Amara Okafor",
        authorAvatarUrl = null,
        isFollowingAuthor = true,
        isOwner = true,
        mode = PostMode.PHOTOS,
        textContent = "",
        caption = longCaption,
        themeId = "cobalt",
        fontOption = telefam.company.core.PostFontOption.SERIF,
        song = PostSongTrack("s_2", "Warm Coffee", "LoFi", previewUrl = "https://api.telefam.app/v1/media/f/s2.mp3"),
        hashtags = listOf("photos"),
        photoUrls = listOf("/v1/media/f/p1.jpg", "/v1/media/f/p2.jpg"),
        primaryStreamUrl = "",
        rawDownloadUrl = "",
        thumbnailUrl = "/v1/media/f/p1.jpg",
        renditions = emptyList(),
        width = 1080,
        height = 1080,
        durationMs = 0L,
        muteOriginalAudio = true,
        contentId = "cid_1_1",
        likesCount = 450L,
        commentsCount = 10L,
        sharesCount = 2L,
        favouritesCount = 5L,
        repostsCount = 0L,
        isLiked = true,
        isFavourited = false,
        isReposted = false,
        repostedByUsername = "",
        recommendationLabel = "You might like this",
        createdAtMs = 2_000L,
      ),
    )

    composeRule.setContent {
      TelefamTheme(themeMode = ThemeMode.DARK) {
        ReusableVideoFeedScreen(
          items = items,
          initialIndex = 0,
          isHostActive = false,
          streamAccessToken = "acc_caller_1",
          streamDeviceFp = "fp_1",
          onToggleFollowAuthor = { _, _ -> },
          onToggleLike = { _, _ -> },
          onDoubleTapLike = {},
          onCommentClick = {},
          onToggleFavourite = { _, _ -> },
          onShareRecorded = {},
          onToggleRepost = { _, _ -> },
          onDownloadVideo = {},
          onNotInterested = {},
          onReportPost = { _, _ -> },
          onBlockAuthor = { _, _ -> },
          onEditOwnerPost = {},
          onDeleteOwnerPost = {},
          onTapHashtag = { _, _ -> },
          onTapAudio = { _, _ -> },
          onRecordWatchProgress = { _, _, _ -> },
          onShowToast = {},
        )
      }
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithTag("feed_vertical_pager").assertIsDisplayed()
    composeRule.onAllNodesWithTag("feed_caption_text").onFirst().assertIsDisplayed()
    composeRule.onAllNodesWithTag("feed_caption_toggle").onFirst().assertIsDisplayed()
    composeRule.onAllNodesWithText("Read more").onFirst().assertIsDisplayed()

    composeRule.onAllNodesWithTag("feed_caption_toggle").onFirst().performClick()
    composeRule.waitForIdle()
    composeRule.onAllNodesWithText("Read less").onFirst().assertIsDisplayed()

    composeRule.onAllNodesWithTag("feed_caption_toggle").onFirst().performClick()
    composeRule.waitForIdle()
    composeRule.onAllNodesWithText("Read more").onFirst().assertIsDisplayed()
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

  private class FakeFeedApiService : AbstractTestChatApiStub() {
    val batchedActions = mutableListOf<FeedInteractionRequestDto>()

    private val feedItems = mutableListOf(
      FeedItemDto(
        postId = "post_owner",
        authorUserId = "caller_1",
        authorUsername = "rohan.verma",
        authorFullName = "Rohan Verma",
        isOwner = true,
        mode = "video",
        caption = "Beautiful nights in the city",
        hashtags = listOf("nightvibes", "citylights", "explore"),
        videoUrl = "/v1/media/f/vid1_720.mp4",
        thumbnailUrl = "/v1/media/f/thumb1.jpg",
        renditions = listOf(
          FeedVideoRenditionDto("360p", 360, 640, 450, "/v1/media/f/vid1_360.mp4", 400_000),
          FeedVideoRenditionDto("720p", 720, 1280, 1400, "/v1/media/f/vid1_720.mp4", 1_200_000),
        ),
        width = 1080,
        height = 1920,
        durationMs = 15_000L,
        likesCount = 128_420L,
        commentsCount = 2_345L,
        sharesCount = 8_765L,
        favouritesCount = 32_100L,
        recommendationLabel = "You might like this",
      ),
      FeedItemDto(
        postId = "post_other",
        authorUserId = "u_2",
        authorUsername = "amara_reads",
        authorFullName = "Amara Okafor",
        isOwner = false,
        mode = "video",
        caption = "Sunset horizon",
        hashtags = listOf("sunset"),
        videoUrl = "/v1/media/f/vid2.mp4",
        width = 1920,
        height = 1080,
        durationMs = 12_000L,
        likesCount = 500L,
        recommendationLabel = "You might like this",
      ),
    )

    override suspend fun getFeeds(
      bearer: String,
      deviceFingerprint: String,
      tab: String,
      page: Int,
      pageSize: Int,
    ): Response<FeedPageResponseDto> =
      Response.success(
        FeedPageResponseDto(
          success = true,
          tab = tab,
          items = feedItems,
          page = page,
          pageSize = pageSize,
          hasMore = false,
        ),
      )

    override suspend fun searchFeeds(
      bearer: String,
      deviceFingerprint: String,
      query: String,
      shortcut: String,
      page: Int,
      pageSize: Int,
    ): Response<FeedSearchResponseDto> =
      Response.success(
        FeedSearchResponseDto(
          success = true,
          query = query,
          shortcut = shortcut,
          shortcuts = listOf(SearchShortcutDto("nightvibes", "#nightvibes", 12)),
          items = feedItems,
          page = page,
          pageSize = pageSize,
          hasMore = false,
        ),
      )

    override suspend fun interactFeed(
      bearer: String,
      deviceFingerprint: String,
      body: FeedInteractionRequestDto,
    ): Response<FeedInteractionResponseDto> =
      Response.success(FeedInteractionResponseDto(success = true, postId = body.postId, action = body.action))

    override suspend fun interactFeedBatch(
      bearer: String,
      deviceFingerprint: String,
      body: FeedInteractionBatchRequestDto,
    ): Response<SyncBatchResponseDto> {
      batchedActions.addAll(body.operations)
      return Response.success(SyncBatchResponseDto(success = true, ackedOpIds = body.operations.map { it.clientOpId }))
    }

    override suspend fun updatePost(
      bearer: String,
      deviceFingerprint: String,
      postId: String,
      body: UpdatePostRequestDto,
    ): Response<CreatePostResponseDto> =
      Response.success(CreatePostResponseDto(success = true, post = CreatedPostDto(postId = postId)))

    override suspend fun deletePost(
      bearer: String,
      deviceFingerprint: String,
      postId: String,
    ): Response<GenericSuccessDto> {
      feedItems.removeAll { it.postId == postId }
      return Response.success(GenericSuccessDto(success = true))
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
      Response.success(MediaUploadResponseDto(success = true, mediaId = "srv_avatar_media_99"))

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

    override suspend fun setupProfile(bearer: String, deviceFingerprint: String, body: ProfileSetupDto): Response<GenericSuccessDto> =
      Response.success(GenericSuccessDto(success = true))

    override suspend fun recordAuditEvent(bearer: String, deviceFingerprint: String, body: AuditEventDto): Response<GenericSuccessDto> =
      Response.success(GenericSuccessDto(success = true))

    override suspend fun syncBatch(bearer: String, deviceFingerprint: String, body: SyncBatchRequestDto): Response<SyncBatchResponseDto> =
      Response.success(SyncBatchResponseDto(success = true))

    override suspend fun listContacts(bearer: String, deviceFingerprint: String, filter: String, query: String, page: Int, pageSize: Int): Response<ContactsPageResponseDto> =
      Response.success(ContactsPageResponseDto(success = true))

    override suspend fun followContact(bearer: String, deviceFingerprint: String, body: ContactActionDto): Response<FollowMutationResponseDto> =
      Response.success(FollowMutationResponseDto(success = true))

    override suspend fun unfollowContact(bearer: String, deviceFingerprint: String, body: ContactActionDto): Response<FollowMutationResponseDto> =
      Response.success(FollowMutationResponseDto(success = true))

    override suspend fun syncContactActions(bearer: String, deviceFingerprint: String, body: ContactSyncBatchRequestDto): Response<SyncBatchResponseDto> =
      Response.success(SyncBatchResponseDto(success = true))

    override suspend fun getUserProfile(bearer: String, deviceFingerprint: String, userId: String): Response<UserProfileResponseDto> =
      Response.success(UserProfileResponseDto(success = true, userId = userId))

    override suspend fun getUserConnections(bearer: String, deviceFingerprint: String, userId: String, type: String, page: Int, pageSize: Int): Response<UserConnectionsResponseDto> =
      Response.success(UserConnectionsResponseDto(success = true, page = page))

    override suspend fun getUserFollowers(bearer: String, deviceFingerprint: String, userId: String, page: Int, pageSize: Int): Response<UserConnectionsResponseDto> =
      Response.success(UserConnectionsResponseDto(success = true, page = page))

    override suspend fun getUserFollowing(bearer: String, deviceFingerprint: String, userId: String, page: Int, pageSize: Int): Response<UserConnectionsResponseDto> =
      Response.success(UserConnectionsResponseDto(success = true, page = page))

    override suspend fun blockUser(bearer: String, deviceFingerprint: String, userId: String): Response<UserBlockResponseDto> =
      Response.success(UserBlockResponseDto(success = true, targetUserId = userId, isBlocked = true))

    override suspend fun unblockUser(bearer: String, deviceFingerprint: String, userId: String): Response<UserBlockResponseDto> =
      Response.success(UserBlockResponseDto(success = true, targetUserId = userId, isBlocked = false))

    override suspend fun reportUser(bearer: String, deviceFingerprint: String, userId: String, body: UserReportRequestDto): Response<UserReportResponseDto> =
      Response.success(UserReportResponseDto(success = true, targetUserId = userId, reason = body.reason))

    override suspend fun uploadProfileBackground(bearer: String, deviceFingerprint: String, imageBody: RequestBody): Response<ProfileBackgroundUploadResponseDto> =
      Response.success(ProfileBackgroundUploadResponseDto(success = true, mediaId = "bg1"))

    override suspend fun attachProfileBackground(bearer: String, deviceFingerprint: String, body: ProfileBackgroundAttachDto): Response<ProfileBackgroundListResponseDto> =
      Response.success(ProfileBackgroundListResponseDto(success = true))

    override suspend fun reorderProfileBackgrounds(bearer: String, deviceFingerprint: String, body: ProfileBackgroundReorderDto): Response<ProfileBackgroundListResponseDto> =
      Response.success(ProfileBackgroundListResponseDto(success = true))

    override suspend fun deleteProfileBackground(bearer: String, deviceFingerprint: String, mediaId: String): Response<ProfileBackgroundListResponseDto> =
      Response.success(ProfileBackgroundListResponseDto(success = true))

    override suspend fun updateProfile(bearer: String, deviceFingerprint: String, body: ProfileUpdateRequestDto): Response<ProfileUpdateResponseDto> =
      Response.success(ProfileUpdateResponseDto(success = true, username = body.username, fullName = body.fullName))

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
