package telefam.company

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetworkCapabilities
import retrofit2.Response
import telefam.company.core.ContactFilterTab
import telefam.company.core.ContactRelationshipState
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
import telefam.company.data.network.FeedPageResponseDto
import telefam.company.data.network.FeedSearchResponseDto
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
import telefam.company.data.repository.ContactToggleResult
import telefam.company.data.repository.TelefamRepository

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TelefamContactsRobolectricTest {
  private lateinit var context: Context
  private lateinit var db: TelefamDatabase
  private lateinit var identityStore: DeviceIdentityStore
  private lateinit var networkMonitor: NetworkQualityMonitor
  private lateinit var fakeApi: FakeContactsApiService
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
    fakeApi = FakeContactsApiService()
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
  fun `contacts pagination caches aggressively and survives offline filtering`() = runTest {
    val page1 = repository.loadContactsPage(ContactFilterTab.ALL, "", page = 1, pageSize = 2)
    assertFalse(page1.fromCache)
    assertTrue(page1.hasMore)
    assertEquals(2, page1.pageItems.size)

    val page2 = repository.loadContactsPage(ContactFilterTab.ALL, "", page = 2, pageSize = 2)
    assertFalse(page2.fromCache)
    assertFalse(page2.hasMore)
    assertEquals(2, page2.pageItems.size)

    // Go offline and verify cached directory + search + filters work seamlessly
    setNetworkOnline(false)
    networkMonitor.refreshStatus()

    val offlineAll = repository.loadContactsPage(ContactFilterTab.ALL, "amara", page = 1, pageSize = 20)
    assertTrue(offlineAll.fromCache)
    assertEquals(1, offlineAll.pageItems.size)
    assertEquals("Amara Okafor", offlineAll.pageItems.first().fullName)

    val followers = repository.observeCachedContacts(ContactFilterTab.FOLLOWERS, "", "caller_1").first()
    assertEquals(listOf("u_brian", "u_elena"), followers.map { it.userId }.sorted())

    val friends = repository.observeCachedContacts(ContactFilterTab.FRIENDS, "", "caller_1").first()
    assertEquals(listOf("u_elena"), friends.map { it.userId })
  }

  @Test
  fun `follow toggles relationship states queues offline actions and flushes on reconnect`() = runTest {
    repository.loadContactsPage(ContactFilterTab.ALL, "", page = 1, pageSize = 20)

    // Relationship computation check: Follow -> Following, Follow Back -> Friends
    assertEquals(ContactRelationshipState.FOLLOW, ContactRelationshipState.compute(isFollowing = false, followsYou = false))
    assertEquals(ContactRelationshipState.FOLLOW_BACK, ContactRelationshipState.compute(isFollowing = false, followsYou = true))
    assertEquals(ContactRelationshipState.FOLLOWING, ContactRelationshipState.compute(isFollowing = true, followsYou = false))
    assertEquals(ContactRelationshipState.FRIENDS, ContactRelationshipState.compute(isFollowing = true, followsYou = true))

    // Drop offline and follow Brian (who follows us -> becomes Friends immediately in local cache)
    setNetworkOnline(false)
    networkMonitor.refreshStatus()
    val offlineToggle = repository.toggleContactFollow("u_brian")
    assertTrue(offlineToggle is ContactToggleResult.QueuedOffline)

    val cachedBrian = db.telefamDao().getCachedContact("u_brian")!!
    assertTrue(cachedBrian.isFollowing)
    assertEquals(
      ContactRelationshipState.FRIENDS,
      ContactRelationshipState.compute(cachedBrian.isFollowing, cachedBrian.followsYou),
    )
    assertEquals(1, db.telefamDao().getAllPendingContactSyncActions().size)

    // Reconnect and flush offline queue
    setNetworkOnline(true)
    networkMonitor.refreshStatus()
    repository.flushOfflineQueue()

    assertTrue(db.telefamDao().getAllPendingContactSyncActions().isEmpty())
    assertTrue(fakeApi.syncedActions.any { it.targetUserId == "u_brian" && it.action == "follow" })
  }

  @Test
  fun `contacts action rate limiter enforces 20 actions per minute with Try again later`() = runTest {
    repository.loadContactsPage(ContactFilterTab.ALL, "", page = 1, pageSize = 20)

    repeat(20) {
      val res = repository.toggleContactFollow("u_amara")
      assertFalse(res is ContactToggleResult.RateLimited)
    }

    val limited = repository.toggleContactFollow("u_amara")
    assertTrue(limited is ContactToggleResult.RateLimited)
    assertEquals("Try again later", (limited as ContactToggleResult.RateLimited).message)
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

  private class FakeContactsApiService : AbstractTestChatApiStub() {
    val syncedActions = mutableListOf<telefam.company.data.network.ContactSyncActionDto>()
    private val directory = mutableListOf(
      ContactItemDto("u_amara", "Amara Okafor", "amara_reads", "Lagos", "NG", null, isFollowing = false, followsYou = false, relationshipState = "FOLLOW", riskScore = 10.0),
      ContactItemDto("u_brian", "Brian Kiprop", "brian_k", "Nairobi", "KE", null, isFollowing = false, followsYou = true, relationshipState = "FOLLOW_BACK", riskScore = 12.0),
      ContactItemDto("u_chloe", "Chloe Martin", "chloe_m", "Paris", "FR", null, isFollowing = true, followsYou = false, relationshipState = "FOLLOWING", riskScore = 14.0),
      ContactItemDto("u_elena", "Elena Rossi", "elena_r", "Rome", "IT", null, isFollowing = true, followsYou = true, relationshipState = "FRIENDS", riskScore = 5.0),
    )

    override suspend fun listContacts(
      bearer: String,
      deviceFingerprint: String,
      filter: String,
      query: String,
      page: Int,
      pageSize: Int,
    ): Response<ContactsPageResponseDto> {
      val offset = (page - 1) * pageSize
      val slice = if (offset >= directory.size) emptyList() else directory.subList(offset, minOf(directory.size, offset + pageSize))
      return Response.success(
        ContactsPageResponseDto(
          success = true,
          items = slice,
          page = page,
          pageSize = pageSize,
          totalCount = directory.size,
          hasMore = offset + slice.size < directory.size,
        ),
      )
    }

    override suspend fun followContact(bearer: String, deviceFingerprint: String, body: ContactActionDto): Response<FollowMutationResponseDto> {
      val idx = directory.indexOfFirst { it.userId == body.targetUserId }
      val cur = if (idx >= 0) directory[idx] else null
      if (idx >= 0) directory[idx] = directory[idx].copy(isFollowing = true)
      return Response.success(
        FollowMutationResponseDto(
          success = true,
          targetUserId = body.targetUserId,
          isFollowing = true,
          followsYou = cur?.followsYou ?: false,
        ),
      )
    }

    override suspend fun unfollowContact(bearer: String, deviceFingerprint: String, body: ContactActionDto): Response<FollowMutationResponseDto> {
      val idx = directory.indexOfFirst { it.userId == body.targetUserId }
      val cur = if (idx >= 0) directory[idx] else null
      if (idx >= 0) directory[idx] = directory[idx].copy(isFollowing = false)
      return Response.success(
        FollowMutationResponseDto(
          success = true,
          targetUserId = body.targetUserId,
          isFollowing = false,
          followsYou = cur?.followsYou ?: false,
        ),
      )
    }

    override suspend fun syncContactActions(bearer: String, deviceFingerprint: String, body: ContactSyncBatchRequestDto): Response<SyncBatchResponseDto> {
      syncedActions.addAll(body.actions)
      return Response.success(SyncBatchResponseDto(success = true, ackedOpIds = body.actions.map { it.clientOpId }))
    }

    override suspend fun downloadMedia(url: String): Response<ResponseBody> =
      Response.success(ByteArray(0).toResponseBody("application/octet-stream".toMediaType()))

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

    override suspend fun uploadPostPhoto(bearer: String, deviceFingerprint: String, imageBody: RequestBody): Response<MediaUploadResponseDto> =
      Response.success(MediaUploadResponseDto(success = true, mediaId = "pm1"))

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
    ): Response<MediaUploadResponseDto> =
      Response.success(MediaUploadResponseDto(success = true, mediaId = "vm1", thumbnailMediaId = "tm1"))

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

    override suspend fun interactFeed(bearer: String, deviceFingerprint: String, body: FeedInteractionRequestDto): Response<FeedInteractionResponseDto> =
      Response.success(FeedInteractionResponseDto(success = true, postId = body.postId, action = body.action))

    override suspend fun interactFeedBatch(bearer: String, deviceFingerprint: String, body: FeedInteractionBatchRequestDto): Response<SyncBatchResponseDto> =
      Response.success(SyncBatchResponseDto(success = true, ackedOpIds = body.operations.map { it.clientOpId }))

    override suspend fun syncBatch(bearer: String, deviceFingerprint: String, body: SyncBatchRequestDto): Response<SyncBatchResponseDto> =
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

    override suspend fun updateProfile(bearer: String, deviceFingerprint: String, body: ProfileUpdateRequestDto): Response<ProfileUpdateResponseDto> =
      Response.success(ProfileUpdateResponseDto(success = true, username = body.username, fullName = body.fullName))

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
