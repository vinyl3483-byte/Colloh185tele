package telefam.company

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Looper
import android.provider.Settings
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.telefam.company.app.R
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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetworkCapabilities
import retrofit2.Response
import telefam.company.core.TelefamProfileSanitizer
import telefam.company.data.local.DeviceIdentityStore
import telefam.company.data.local.TelefamDatabase
import telefam.company.data.model.CountryCatalog
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
import telefam.company.data.network.EmailCheckResponseDto
import telefam.company.data.network.UsernameCheckResponseDto
import telefam.company.data.network.VerifyLinkDto
import telefam.company.data.network.VerifyOtpDto
import telefam.company.data.repository.AuthActionResult
import telefam.company.data.repository.ProfileActionResult
import telefam.company.data.repository.TelefamRepository
import telefam.company.media.MediaUploadProcessor
import telefam.company.ui.viewmodel.TelefamViewModel
import java.util.Calendar
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TelefamAuthRobolectricTest {
  private lateinit var db: TelefamDatabase
  private lateinit var repository: TelefamRepository
  private lateinit var context: Context
  private lateinit var recordingApi: RecordingTelefamApiService

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val active = cm.activeNetwork
    if (active != null) {
      val caps = ShadowNetworkCapabilities.newInstance()
      shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
      shadowOf(cm).setNetworkCapabilities(active, caps)
    }
    db = Room.inMemoryDatabaseBuilder(context, TelefamDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    recordingApi = RecordingTelefamApiService()
    repository = TelefamRepository(
      appContext = context,
      dao = db.telefamDao(),
      identityStore = DeviceIdentityStore(context),
      networkMonitor = NetworkQualityMonitor(context),
      apiService = recordingApi,
    )
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `app name matches Telefam`() {
    assertEquals("Telefam", context.getString(R.string.app_name))
  }

  @Test
  fun `request otp sends fingerprint headers enforces cooldown and verifies issued code`() = runTest {
    val first = repository.requestOtp("colloh185@gmail.com")
    assertTrue(first is AuthActionResult.OtpSent)
    assertEquals(60L, (first as AuthActionResult.OtpSent).cooldownSeconds)
    assertTrue(recordingApi.lastDeviceFp.length == 64)
    assertTrue(recordingApi.lastIpFp.length == 64)
    assertEquals("colloh185@gmail.com", recordingApi.lastRequestedEmail)

    val immediateRetry = repository.requestOtp("colloh185@gmail.com")
    assertTrue(immediateRetry is AuthActionResult.SanitizedFailure)
    val failure = immediateRetry as AuthActionResult.SanitizedFailure
    assertTrue(failure.message.contains("error has occurred", ignoreCase = true))
    assertTrue(failure.remainingCooldownSeconds in 1L..60L)

    val wrongVerify = repository.verifyOtp("colloh185@gmail.com", "123456")
    assertTrue(wrongVerify is AuthActionResult.SanitizedFailure)

    val validVerify = repository.verifyOtp("colloh185@gmail.com", "849201")
    assertTrue(validVerify is AuthActionResult.Verified)
    assertEquals("colloh185@gmail.com", (validVerify as AuthActionResult.Verified).email)
  }

  @Test
  fun `email prefix autofills full name and username and rejects under 13`() = runTest {
    val (name, user) = TelefamProfileSanitizer.autofillFromEmail("colloh.dev185@gmail.com")
    assertEquals("Colloh Dev185", name)
    assertEquals("colloh_dev185", user)

    val nowYear = Calendar.getInstance().get(Calendar.YEAR)
    val tooYoung = repository.saveProfileSetup(
      userId = "user_1",
      email = "colloh.dev185@gmail.com",
      fullNameRaw = name,
      usernameRaw = user,
      dialCode = "+254",
      phoneDigitsRaw = "712345678",
      countryIso = "KE",
      genderRaw = "Male",
      dobYear = nowYear - 10,
      dobMonthZeroBased = 0,
      dobDay = 15,
      locationRaw = "Nairobi",
      bioRaw = "Hello <script>alert(1)</script>",
      avatar = null,
    )
    assertTrue(tooYoung is ProfileActionResult.Error)

    val valid = repository.saveProfileSetup(
      userId = "user_1",
      email = "colloh.dev185@gmail.com",
      fullNameRaw = name,
      usernameRaw = user,
      dialCode = "+254",
      phoneDigitsRaw = "712345678",
      countryIso = "KE",
      genderRaw = "Male",
      dobYear = nowYear - 20,
      dobMonthZeroBased = 0,
      dobDay = 15,
      locationRaw = "Nairobi",
      bioRaw = "Hello <b>world</b>",
      avatar = null,
    )
    assertTrue(valid is ProfileActionResult.Saved)

    val duplicateUsername = repository.isUsernameAvailable(user, "user_2")
    assertFalse(duplicateUsername)
  }

  @Test
  fun `media magic bytes and country catalog work properly`() {
    assertTrue(CountryCatalog.allCountries.size > 150)
    val jpegHeader = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0, 0, 0, 0, 0, 0, 0, 0, 0)
    val scriptHeader = "<svg onload=alert(1)>".toByteArray()
    assertTrue(MediaUploadProcessor.isSupportedImageMagicBytes(jpegHeader))
    assertFalse(MediaUploadProcessor.isSupportedImageMagicBytes(scriptHeader))
  }

  @Test
  fun `existing email backend check surfaces notice and same ANDROID_ID keeps fingerprint stable across reinstall`() = runBlocking {
    Settings.Secure.putString(context.contentResolver, Settings.Secure.ANDROID_ID, "stable_hw_android_id_01")
    recordingApi.existingEmails["existing.user@gmail.com"] = true
    val vm = TelefamViewModel(repository)
    vm.onEmailChanged("existing.user@gmail.com")

    val deadline = System.currentTimeMillis() + 3_000L
    while (vm.uiState.value.emailAccountNotice.isNullOrBlank() && System.currentTimeMillis() < deadline) {
      shadowOf(Looper.getMainLooper()).idleFor(100L, TimeUnit.MILLISECONDS)
      Thread.sleep(20L)
    }
    assertFalse(vm.uiState.value.emailAccountNotice.isNullOrBlank())

    // Simulate first install request -> captures device fingerprint
    repository.requestOtp("colloh185@gmail.com")
    val firstFp = recordingApi.lastDeviceFp
    assertEquals(64, firstFp.length)

    // Simulate app uninstall/reinstall clearing DataStore installId while hardware ANDROID_ID stays same
    DeviceIdentityStore(context).clearAll()
    val reinstalledRepo = TelefamRepository(
      appContext = context,
      dao = db.telefamDao(),
      identityStore = DeviceIdentityStore(context),
      networkMonitor = NetworkQualityMonitor(context),
      apiService = recordingApi,
    )
    reinstalledRepo.requestOtp("colloh185+2@gmail.com")
    assertEquals(firstFp, recordingApi.lastDeviceFp)
  }

  private class RecordingTelefamApiService : AbstractTestChatApiStub() {
    var lastDeviceFp: String = ""
    var lastIpFp: String = ""
    var lastRequestedEmail: String = ""
    val existingEmails = mutableMapOf<String, Boolean>()
    private val expectedCode = "849201"

    override suspend fun checkEmail(
      deviceFingerprint: String,
      email: String,
    ): Response<EmailCheckResponseDto> {
      val exists = existingEmails[email.trim().lowercase()] == true
      return Response.success(
        EmailCheckResponseDto(
          success = true,
          exists = exists,
          profileCompleted = exists,
        ),
      )
    }

    override suspend fun requestOtp(
      deviceFingerprint: String,
      ipFingerprint: String,
      body: RequestOtpDto,
    ): Response<RequestOtpResponseDto> {
      lastDeviceFp = deviceFingerprint
      lastIpFp = ipFingerprint
      lastRequestedEmail = body.email
      return Response.success(RequestOtpResponseDto(success = true, cooldownSeconds = 60L))
    }

    override suspend fun verifyOtp(
      deviceFingerprint: String,
      ipFingerprint: String,
      body: VerifyOtpDto,
    ): Response<AuthSessionResponseDto> {
      if (body.otp != expectedCode) {
        return Response.success(AuthSessionResponseDto(success = false))
      }
      return Response.success(
        AuthSessionResponseDto(
          success = true,
          sessionToken = "access_tok_test",
          refreshToken = "refresh_tok_test",
          email = body.email,
        ),
      )
    }

    override suspend fun verifyLink(
      deviceFingerprint: String,
      ipFingerprint: String,
      body: VerifyLinkDto,
    ): Response<AuthSessionResponseDto> =
      Response.success(AuthSessionResponseDto(success = true, sessionToken = "acc", refreshToken = "ref", email = "colloh185@gmail.com"))

    override suspend fun refreshToken(
      deviceFingerprint: String,
      body: RefreshTokenDto,
    ): Response<AuthSessionResponseDto> =
      Response.success(AuthSessionResponseDto(success = true, sessionToken = "acc2", refreshToken = "ref2"))

    override suspend fun verifyGoogleOAuth(
      deviceFingerprint: String,
      ipFingerprint: String,
      body: OAuthDto,
    ): Response<AuthSessionResponseDto> =
      Response.success(AuthSessionResponseDto(success = true, sessionToken = "accg", refreshToken = "refg"))

    override suspend fun checkUsername(username: String): Response<UsernameCheckResponseDto> =
      Response.success(UsernameCheckResponseDto(available = true))

    override suspend fun uploadAvatar(
      bearer: String,
      deviceFingerprint: String,
      cropCenterX: Float,
      cropCenterY: Float,
      cropScale: Float,
      imageBody: RequestBody,
    ): Response<MediaUploadResponseDto> =
      Response.success(MediaUploadResponseDto(success = true, mediaId = "m1"))

    override suspend fun setupProfile(
      bearer: String,
      deviceFingerprint: String,
      body: ProfileSetupDto,
    ): Response<GenericSuccessDto> =
      Response.success(GenericSuccessDto(success = true))

    override suspend fun recordAuditEvent(
      bearer: String,
      deviceFingerprint: String,
      body: AuditEventDto,
    ): Response<GenericSuccessDto> =
      Response.success(GenericSuccessDto(success = true))

    override suspend fun syncBatch(
      bearer: String,
      deviceFingerprint: String,
      body: SyncBatchRequestDto,
    ): Response<SyncBatchResponseDto> =
      Response.success(SyncBatchResponseDto(success = true))

    override suspend fun listContacts(
      bearer: String,
      deviceFingerprint: String,
      filter: String,
      query: String,
      page: Int,
      pageSize: Int,
    ): Response<ContactsPageResponseDto> =
      Response.success(ContactsPageResponseDto(success = true, items = emptyList(), page = page, pageSize = pageSize))

    override suspend fun followContact(
      bearer: String,
      deviceFingerprint: String,
      body: ContactActionDto,
    ): Response<FollowMutationResponseDto> =
      Response.success(
        FollowMutationResponseDto(
          success = true,
          targetUserId = body.targetUserId,
          isFollowing = true,
          relationshipState = "FOLLOWING",
        ),
      )

    override suspend fun unfollowContact(
      bearer: String,
      deviceFingerprint: String,
      body: ContactActionDto,
    ): Response<FollowMutationResponseDto> =
      Response.success(
        FollowMutationResponseDto(
          success = true,
          targetUserId = body.targetUserId,
          isFollowing = false,
          relationshipState = "FOLLOW",
        ),
      )

    override suspend fun syncContactActions(
      bearer: String,
      deviceFingerprint: String,
      body: ContactSyncBatchRequestDto,
    ): Response<SyncBatchResponseDto> =
      Response.success(SyncBatchResponseDto(success = true, ackedOpIds = body.actions.map { it.clientOpId }))

    override suspend fun uploadPostPhoto(
      bearer: String,
      deviceFingerprint: String,
      imageBody: RequestBody,
    ): Response<MediaUploadResponseDto> =
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

    override suspend fun searchSongs(
      bearer: String,
      deviceFingerprint: String,
      query: String,
    ): Response<SongsSearchResponseDto> =
      Response.success(SongsSearchResponseDto(success = true))

    override suspend fun searchHashtags(
      bearer: String,
      deviceFingerprint: String,
      query: String,
    ): Response<HashtagsSearchResponseDto> =
      Response.success(HashtagsSearchResponseDto(success = true))

    override suspend fun createPost(
      bearer: String,
      deviceFingerprint: String,
      body: CreatePostRequestDto,
    ): Response<CreatePostResponseDto> =
      Response.success(CreatePostResponseDto(success = true, post = CreatedPostDto(postId = "p1", clientOpId = body.clientOpId)))

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
    ): Response<GenericSuccessDto> =
      Response.success(GenericSuccessDto(success = true))

    override suspend fun getFeeds(
      bearer: String,
      deviceFingerprint: String,
      tab: String,
      page: Int,
      pageSize: Int,
    ): Response<FeedPageResponseDto> =
      Response.success(FeedPageResponseDto(success = true, tab = tab, page = page, pageSize = pageSize))

    override suspend fun searchFeeds(
      bearer: String,
      deviceFingerprint: String,
      query: String,
      shortcut: String,
      page: Int,
      pageSize: Int,
    ): Response<FeedSearchResponseDto> =
      Response.success(FeedSearchResponseDto(success = true, query = query, shortcut = shortcut, page = page, pageSize = pageSize))

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
    ): Response<SyncBatchResponseDto> =
      Response.success(SyncBatchResponseDto(success = true, ackedOpIds = body.operations.map { it.clientOpId }))

    override suspend fun getUserProfile(
      bearer: String,
      deviceFingerprint: String,
      userId: String,
    ): Response<UserProfileResponseDto> =
      Response.success(UserProfileResponseDto(success = true, userId = userId))

    override suspend fun getUserConnections(
      bearer: String,
      deviceFingerprint: String,
      userId: String,
      type: String,
      page: Int,
      pageSize: Int,
    ): Response<UserConnectionsResponseDto> =
      Response.success(UserConnectionsResponseDto(success = true, page = page))

    override suspend fun getUserFollowers(
      bearer: String,
      deviceFingerprint: String,
      userId: String,
      page: Int,
      pageSize: Int,
    ): Response<UserConnectionsResponseDto> =
      Response.success(UserConnectionsResponseDto(success = true, page = page))

    override suspend fun getUserFollowing(
      bearer: String,
      deviceFingerprint: String,
      userId: String,
      page: Int,
      pageSize: Int,
    ): Response<UserConnectionsResponseDto> =
      Response.success(UserConnectionsResponseDto(success = true, page = page))

    override suspend fun blockUser(
      bearer: String,
      deviceFingerprint: String,
      userId: String,
    ): Response<UserBlockResponseDto> =
      Response.success(UserBlockResponseDto(success = true, targetUserId = userId, isBlocked = true))

    override suspend fun unblockUser(
      bearer: String,
      deviceFingerprint: String,
      userId: String,
    ): Response<UserBlockResponseDto> =
      Response.success(UserBlockResponseDto(success = true, targetUserId = userId, isBlocked = false))

    override suspend fun reportUser(
      bearer: String,
      deviceFingerprint: String,
      userId: String,
      body: UserReportRequestDto,
    ): Response<UserReportResponseDto> =
      Response.success(UserReportResponseDto(success = true, targetUserId = userId, reason = body.reason))

    override suspend fun uploadProfileBackground(
      bearer: String,
      deviceFingerprint: String,
      imageBody: RequestBody,
    ): Response<ProfileBackgroundUploadResponseDto> =
      Response.success(ProfileBackgroundUploadResponseDto(success = true, mediaId = "bg1"))

    override suspend fun attachProfileBackground(
      bearer: String,
      deviceFingerprint: String,
      body: ProfileBackgroundAttachDto,
    ): Response<ProfileBackgroundListResponseDto> =
      Response.success(ProfileBackgroundListResponseDto(success = true))

    override suspend fun reorderProfileBackgrounds(
      bearer: String,
      deviceFingerprint: String,
      body: ProfileBackgroundReorderDto,
    ): Response<ProfileBackgroundListResponseDto> =
      Response.success(ProfileBackgroundListResponseDto(success = true))

    override suspend fun deleteProfileBackground(
      bearer: String,
      deviceFingerprint: String,
      mediaId: String,
    ): Response<ProfileBackgroundListResponseDto> =
      Response.success(ProfileBackgroundListResponseDto(success = true))

    override suspend fun updateProfile(
      bearer: String,
      deviceFingerprint: String,
      body: ProfileUpdateRequestDto,
    ): Response<ProfileUpdateResponseDto> =
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
