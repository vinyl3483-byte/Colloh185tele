package telefam.company

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetworkCapabilities
import retrofit2.Response
import telefam.company.core.PostMode
import telefam.company.core.PostSongTrack
import telefam.company.core.TelefamPostRules
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
import telefam.company.data.network.FeedPageResponseDto
import telefam.company.data.network.FeedSearchResponseDto
import telefam.company.data.network.FollowMutationResponseDto
import telefam.company.data.network.GenericSuccessDto
import telefam.company.data.network.HashtagItemDto
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
import telefam.company.data.network.SongTrackDto
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
import telefam.company.data.repository.PostSubmissionInput
import telefam.company.data.repository.TelefamRepository
import telefam.company.media.MediaUploadProcessor
import telefam.company.media.VideoPrepareResult
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TelefamPostRobolectricTest {
  private lateinit var context: Context
  private lateinit var db: TelefamDatabase
  private lateinit var identityStore: DeviceIdentityStore
  private lateinit var networkMonitor: NetworkQualityMonitor
  private lateinit var fakeApi: FakePostApiService
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
      userId = "author_1",
      email = "author@telefam.test",
      accessToken = "acc_author_1",
      refreshToken = "ref_author_1",
      accessExpiresInSec = 900L,
      refreshExpiresInSec = 2_592_000L,
      profileCompleted = true,
    )
    networkMonitor = NetworkQualityMonitor(context)
    fakeApi = FakePostApiService()
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
  fun `video per-frame content id is deterministic and trim-sensitive`() {
    val frames = listOf(
      "0123456789abcdef",
      "fedcba9876543210",
      "0f0f0f0f1e1e1e1e",
    )
    val id1 = TelefamPostRules.computeVideoContentId(frames, 12_000L, 0L, 12_000L)
    val id2 = TelefamPostRules.computeVideoContentId(frames, 12_000L, 0L, 12_000L)
    val idTrimmed = TelefamPostRules.computeVideoContentId(frames, 12_000L, 1_500L, 9_500L)
    val idDifferentFrame = TelefamPostRules.computeVideoContentId(
      listOf("0123456789abcdef", "aaaaaaaaaaaaaaaa", "0f0f0f0f1e1e1e1e"),
      12_000L,
      0L,
      12_000L,
    )

    assertEquals(id1, id2)
    assertTrue(id1.startsWith("vid_"))
    assertEquals(36, id1.length)
    assertNotEquals(id1, idTrimmed)
    assertNotEquals(id1, idDifferentFrame)
  }

  @Test
  fun `video magic-byte validation accepts mp4 mov webm and rejects over 200mb`() {
    val mp4 = byteArrayOf(0, 0, 0, 0x20, 'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte(), 'i'.code.toByte(), 's'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte())
    val mov = byteArrayOf(0, 0, 0, 0x14, 'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte(), 'q'.code.toByte(), 't'.code.toByte(), ' '.code.toByte(), ' '.code.toByte())
    val webm = byteArrayOf(0x1A.toByte(), 0x45.toByte(), 0xDF.toByte(), 0xA3.toByte(), 0, 0, 0, 0, 0, 0, 0, 0)
    val script = "#!/bin/sh\nrm -rf /".toByteArray()

    assertEquals("video/mp4", MediaUploadProcessor.sniffVideoMagicMime(mp4))
    assertEquals("video/quicktime", MediaUploadProcessor.sniffVideoMagicMime(mov))
    assertEquals("video/webm", MediaUploadProcessor.sniffVideoMagicMime(webm))
    assertNull(MediaUploadProcessor.sniffVideoMagicMime(script))

    // Test > 200 MB rejection using sparse temp file
    val oversized = File(context.cacheDir, "oversized_test.mp4")
    RandomAccessFile(oversized, "rw").use { raf ->
      raf.write(mp4)
      raf.setLength(TelefamPostRules.MAX_VIDEO_BYTES + 1024L)
    }
    try {
      val rejected = MediaUploadProcessor.inspectAndExtractVideoFrames(context, Uri.fromFile(oversized))
      assertTrue(rejected is VideoPrepareResult.Rejected)
    } finally {
      oversized.delete()
    }

    // Valid compact MP4 copies safely to UUID-named staging file and generates contentId
    val validMp4 = File(context.cacheDir, "sample_clip.mp4")
    FileOutputStream(validMp4).use { fos ->
      fos.write(mp4)
      fos.write(ByteArray(8192) { (it % 251).toByte() })
    }
    try {
      val ready = MediaUploadProcessor.inspectAndExtractVideoFrames(context, Uri.fromFile(validMp4))
      assertTrue(ready is VideoPrepareResult.Ready)
      val prepared = (ready as VideoPrepareResult.Ready).video
      assertEquals("video/mp4", prepared.mimeType)
      assertTrue(prepared.contentId.startsWith("vid_"))
      assertFalse(prepared.localVideoPath.contains("sample_clip"))
      assertTrue(File(prepared.localVideoPath).exists())
    } finally {
      validMp4.delete()
    }
  }

  @Test
  fun `photo compression renames with uuid computes fingerprints and caps at 10 photos`() = runTest {
    val bmp = Bitmap.createBitmap(2000, 1200, Bitmap.Config.ARGB_8888).apply {
      eraseColor(Color.RED)
    }
    val processed = MediaUploadProcessor.compressPostPhotoBitmap(context, bmp)
    bmp.recycle()

    assertTrue(processed.width <= 1440)
    assertTrue(processed.height <= 1440)
    assertEquals(64, processed.sha256Fingerprint.length)
    assertEquals(16, processed.dHashFingerprint.length)
    assertTrue(processed.contentId.startsWith("img_"))
    assertTrue(File(processed.localFilePath).name.endsWith(".jpg"))
    assertTrue(runCatching { UUID.fromString(processed.localId) }.isSuccess)

    // Verify 10-photo limit enforcement via compressPostPhotos with 12 URIs when 3 already selected
    val photoFiles = (1..12).map { idx ->
      val f = File(context.cacheDir, "pic_$idx.jpg")
      val b = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
      FileOutputStream(f).use { out -> b.compress(Bitmap.CompressFormat.JPEG, 80, out) }
      b.recycle()
      f
    }
    try {
      val added = repository.compressPostPhotos(photoFiles.map { Uri.fromFile(it) }, existingCount = 3)
      assertEquals(7, added.size) // 3 existing + 7 added = 10 max
    } finally {
      photoFiles.forEach { it.delete() }
    }
  }

  @Test
  fun `song search hashtags search and cached post counts work properly`() = runTest {
    val songs = repository.searchSongs("sunset")
    assertEquals(1, songs.size)
    assertEquals("Golden Hour", songs.first().title)
    assertFalse(songs.first().previewUrl.isBlank())

    val tags = repository.searchHashtags("fam")
    assertTrue(tags.any { it.tag == "family" && it.postCount == 42L })

    // Offline hashtag lookup falls back to cached hashtag counts from Room
    setNetworkOnline(false)
    networkMonitor.refreshStatus()
    val offlineTags = repository.searchHashtags("fam")
    assertTrue(offlineTags.any { it.tag == "family" && it.postCount == 42L })
  }

  @Test
  fun `offline video post queues in room and auto-uploads on flushOfflineQueue`() = runTest {
    val bmp = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.DKGRAY) }
    val thumb = MediaUploadProcessor.compressPostPhotoBitmap(context, bmp)
    bmp.recycle()

    val videoFile = File(context.cacheDir, "${UUID.randomUUID()}.mp4")
    val mp4 = byteArrayOf(0, 0, 0, 0x20, 'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte(), 'i'.code.toByte(), 's'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte())
    FileOutputStream(videoFile).use { fos ->
      fos.write(mp4)
      fos.write(ByteArray(4096) { 0x42 })
    }

    try {
      setNetworkOnline(false)
      networkMonitor.refreshStatus()

      val frames = listOf("0123456789abcdef", "abcdef0123456789")
      val cid = TelefamPostRules.computeVideoContentId(frames, 10_000L, 500L, 8_500L)

      val queued = repository.submitOrQueuePost(
        PostSubmissionInput(
          mode = PostMode.VIDEO,
          textContent = "",
          caption = "Weekend roadtrip #family",
          themeId = "crimson",
          fontId = "classic",
          song = PostSongTrack("tr_1", "Golden Hour", "JVKE", previewUrl = "https://cdn.example/p.m4a"),
          taggedUserIds = listOf("u_brian"),
          hashtags = listOf("family", "roadtrip"),
          localPhotoPaths = emptyList(),
          localVideoPath = videoFile.absolutePath,
          localThumbnailPath = thumb.localFilePath,
          frameHashes = frames,
          durationMs = 10_000L,
          trimStartMs = 500L,
          trimEndMs = 8_500L,
          width = 1280,
          height = 720,
          muteOriginalAudio = true,
          contentId = cid,
        ),
      )

      assertTrue(queued is PostPublishResult.QueuedOffline)
      assertEquals(1, db.telefamDao().getReadyPendingPostUploads(System.currentTimeMillis() + 60_000L, 10).size)

      // Reconnect and flush
      setNetworkOnline(true)
      networkMonitor.refreshStatus()
      repository.flushOfflineQueue()

      assertTrue(db.telefamDao().getReadyPendingPostUploads(System.currentTimeMillis() + 60_000L, 10).isEmpty())
      assertEquals(1, fakeApi.uploadedVideosCount)
      assertEquals(1, fakeApi.createdPosts.size)
      val created = fakeApi.createdPosts.first()
      assertEquals("video", created.mode)
      assertTrue(created.muteOriginalAudio)
      assertEquals(500L, created.trimStartMs)
      assertEquals(8_500L, created.trimEndMs)
      assertEquals(cid, created.contentId)
    } finally {
      videoFile.delete()
    }
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

  private class FakePostApiService : AbstractTestChatApiStub() {
    var uploadedVideosCount = 0
    val createdPosts = mutableListOf<CreatePostRequestDto>()

    override suspend fun uploadPostPhoto(
      bearer: String,
      deviceFingerprint: String,
      imageBody: RequestBody,
    ): Response<MediaUploadResponseDto> =
      Response.success(MediaUploadResponseDto(success = true, mediaId = "srv_photo_1", contentId = "img_srv_1"))

    override suspend fun uploadPostVideo(
      bearer: String,
      deviceFingerprint: String,
      frameHashesCsv: String,
      durationMs: Long,
      trimStartMs: Long,
      trimEndMs: Long,
      width: Int,
      height: Int,
      videoPart: MultipartBody.Part,
      thumbnailPart: MultipartBody.Part?,
    ): Response<MediaUploadResponseDto> {
      uploadedVideosCount++
      val cid = TelefamPostRules.computeVideoContentId(frameHashesCsv.split(","), durationMs, trimStartMs, trimEndMs)
      return Response.success(
        MediaUploadResponseDto(
          success = true,
          mediaId = "srv_vid_1",
          thumbnailMediaId = "srv_thumb_1",
          contentId = cid,
        ),
      )
    }

    override suspend fun searchSongs(
      bearer: String,
      deviceFingerprint: String,
      query: String,
    ): Response<SongsSearchResponseDto> =
      Response.success(
        SongsSearchResponseDto(
          success = true,
          items = listOf(
            SongTrackDto(
              trackId = "itunes_101",
              title = "Golden Hour",
              artist = "JVKE",
              album = "this is what ____ feels like",
              previewUrl = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview.m4a",
              durationMs = 30_000L,
              source = "itunes",
            ),
          ),
        ),
      )

    override suspend fun searchHashtags(
      bearer: String,
      deviceFingerprint: String,
      query: String,
    ): Response<HashtagsSearchResponseDto> =
      Response.success(
        HashtagsSearchResponseDto(
          success = true,
          items = listOf(HashtagItemDto(tag = "family", postCount = 42L)),
        ),
      )

    override suspend fun createPost(
      bearer: String,
      deviceFingerprint: String,
      body: CreatePostRequestDto,
    ): Response<CreatePostResponseDto> {
      createdPosts.add(body)
      return Response.success(
        CreatePostResponseDto(
          success = true,
          post = CreatedPostDto(
            postId = "post_${createdPosts.size}",
            clientOpId = body.clientOpId,
            mode = body.mode,
            contentId = body.contentId,
            createdAtMs = System.currentTimeMillis(),
          ),
        ),
      )
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

    override suspend fun updatePost(bearer: String, deviceFingerprint: String, postId: String, body: UpdatePostRequestDto): Response<CreatePostResponseDto> =
      Response.success(CreatePostResponseDto(success = true, post = CreatedPostDto(postId = postId)))

    override suspend fun deletePost(bearer: String, deviceFingerprint: String, postId: String): Response<GenericSuccessDto> =
      Response.success(GenericSuccessDto(success = true))

    override suspend fun getFeeds(bearer: String, deviceFingerprint: String, tab: String, page: Int, pageSize: Int): Response<FeedPageResponseDto> =
      Response.success(FeedPageResponseDto(success = true, tab = tab, page = page, pageSize = pageSize))

    override suspend fun searchFeeds(bearer: String, deviceFingerprint: String, query: String, shortcut: String, page: Int, pageSize: Int): Response<FeedSearchResponseDto> =
      Response.success(FeedSearchResponseDto(success = true, query = query, shortcut = shortcut, page = page, pageSize = pageSize))

    override suspend fun interactFeed(bearer: String, deviceFingerprint: String, body: FeedInteractionRequestDto): Response<FeedInteractionResponseDto> =
      Response.success(FeedInteractionResponseDto(success = true, postId = body.postId, action = body.action))

    override suspend fun interactFeedBatch(bearer: String, deviceFingerprint: String, body: FeedInteractionBatchRequestDto): Response<SyncBatchResponseDto> =
      Response.success(SyncBatchResponseDto(success = true, ackedOpIds = body.operations.map { it.clientOpId }))

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
