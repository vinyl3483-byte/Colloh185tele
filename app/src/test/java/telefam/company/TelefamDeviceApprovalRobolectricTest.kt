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
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
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
import telefam.company.data.local.DeviceIdentityStore
import telefam.company.data.local.TelefamDatabase
import telefam.company.data.local.UserProfileEntity
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
import telefam.company.data.network.PendingLoginRequestItemDto
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
import telefam.company.data.repository.AuthActionResult
import telefam.company.data.repository.TelefamRepository
import telefam.company.security.DeviceContextCollector
import telefam.company.ui.screens.DeviceApprovalWaitScreen
import telefam.company.ui.screens.IncomingLoginRequestFullScreenOverlay
import telefam.company.ui.screens.LoginApprovedFullScreenOverlay
import telefam.company.ui.theme.TelefamTheme
import telefam.company.ui.viewmodel.AuthStep
import telefam.company.ui.viewmodel.DeviceApprovalWaitState
import telefam.company.ui.viewmodel.IncomingLoginRequestState
import telefam.company.ui.viewmodel.LoginApprovedOverlayState
import telefam.company.ui.viewmodel.TelefamViewModel
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h915dp")
class TelefamDeviceApprovalRobolectricTest {

  @get:Rule
  val composeRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var db: TelefamDatabase
  private lateinit var identityStore: DeviceIdentityStore
  private lateinit var fakeApi: FakeDeviceSecurityApiService
  private lateinit var repository: TelefamRepository

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    setNetworkOnline(true)
    db = Room.inMemoryDatabaseBuilder(context, TelefamDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    identityStore = DeviceIdentityStore(context)
    runBlocking { identityStore.clearAll() }
    fakeApi = FakeDeviceSecurityApiService()
    repository = TelefamRepository(
      appContext = context,
      dao = db.telefamDao(),
      identityStore = identityStore,
      networkMonitor = NetworkQualityMonitor(context),
      apiService = fakeApi,
    )
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `existing email on new device skips otp enters approval wait then hydrates profile without setup`() = runBlocking {
    val vm = TelefamViewModel(repository)
    vm.onEmailChanged("alex@telefam.company")
    vm.sendOtp()

    val deadline = System.currentTimeMillis() + 3_000L
    while (vm.uiState.value.authStep != AuthStep.DEVICE_APPROVAL_WAIT && System.currentTimeMillis() < deadline) {
      shadowOf(Looper.getMainLooper()).idle()
      Thread.sleep(25L)
    }

    assertEquals(AuthStep.DEVICE_APPROVAL_WAIT, vm.uiState.value.authStep)
    val waitState = vm.uiState.value.pendingLoginApprovalWait
    assertNotNull(waitState)
    assertEquals("Samsung Galaxy S23", waitState!!.targetDeviceName)
    assertEquals("Nairobi, Kenya", waitState.targetLocation)

    // Simulate trusted device accepting request on server -> poller receives session & skips setup
    fakeApi.pollApproved = true
    val authDeadline = System.currentTimeMillis() + 5_000L
    while (!vm.uiState.value.isAuthenticated && System.currentTimeMillis() < authDeadline) {
      shadowOf(Looper.getMainLooper()).idleFor(1_000L, TimeUnit.MILLISECONDS)
      Thread.sleep(20L)
    }

    assertTrue(vm.uiState.value.isAuthenticated)
    assertTrue(vm.uiState.value.profileCompleted)
    assertEquals("user_existing_alex", vm.uiState.value.userId)
    var hydrated = db.telefamDao().observeProfile("user_existing_alex").first()
    val hydrateDeadline = System.currentTimeMillis() + 3_000L
    while (hydrated == null && System.currentTimeMillis() < hydrateDeadline) {
      shadowOf(Looper.getMainLooper()).idleFor(100L, TimeUnit.MILLISECONDS)
      Thread.sleep(20L)
      hydrated = db.telefamDao().observeProfile("user_existing_alex").first()
    }
    if (hydrated == null) {
      val diagFetch = repository.fetchUserProfile("user_existing_alex")
      val diagSession = identityStore.sessionFlow.first()
      assertNotNull(
        "hydrated profile missing (networkTier=${repository.networkTier.value}, sessionTokenPresent=${!diagSession.accessToken.isNullOrBlank()}, diagFetch=$diagFetch)",
        hydrated,
      )
    }
    assertEquals("Alex Mwangi", hydrated!!.fullName)
    assertEquals("alexmwangi", hydrated.username)
    assertEquals("KE", hydrated.countryIso)
  }

  @Test
  fun `try another way enforces matching google email and authenticates with existing profile`() = runBlocking {
    val vm = TelefamViewModel(repository)
    vm.onEmailChanged("alex@telefam.company")
    vm.sendOtp()

    val waitDeadline = System.currentTimeMillis() + 2_500L
    while (vm.uiState.value.pendingLoginApprovalWait == null && System.currentTimeMillis() < waitDeadline) {
      shadowOf(Looper.getMainLooper()).idle()
      Thread.sleep(25L)
    }
    vm.prepareTryAnotherWayGoogleSignIn()

    // Mismatched Google account must be rejected client-side
    vm.onGoogleSignInSuccess("tok_other", "someone.else@gmail.com")
    shadowOf(Looper.getMainLooper()).idle()
    Thread.sleep(50L)
    assertFalse(vm.uiState.value.isAuthenticated)
    assertTrue((vm.uiState.value.errorMessage ?: "").contains("alex@telefam.company"))

    // Matching Google account authenticates and hydrates existing profile
    vm.onGoogleSignInSuccess("tok_valid_alex", "alex@telefam.company")
    val authDeadline = System.currentTimeMillis() + 3_000L
    while (!vm.uiState.value.isAuthenticated && System.currentTimeMillis() < authDeadline) {
      shadowOf(Looper.getMainLooper()).idle()
      Thread.sleep(30L)
    }

    assertTrue(vm.uiState.value.isAuthenticated)
    assertTrue(vm.uiState.value.profileCompleted)
    assertEquals("req_approval_1", fakeApi.lastOAuthDto?.approvalRequestId)
    assertEquals("alex@telefam.company", fakeApi.lastOAuthDto?.expectedEmail)
  }

  @Test
  fun `wait screen renders target device details and dispatches try another way action`() {
    var tryAnotherWayClicked = false
    composeRule.setContent {
      TelefamTheme {
        DeviceApprovalWaitScreen(
          waitState = DeviceApprovalWaitState(
            email = "alex@telefam.company",
            requestId = "req_1",
            pollToken = "poll_1",
            targetDeviceName = "Samsung Galaxy S23",
            targetLocation = "Nairobi, Kenya",
            requestTimeMs = 1_760_000_000_000L,
            remainingSeconds = 55L,
          ),
          isDeniedBlocked = false,
          isLoading = false,
          errorMessage = null,
          onBackChangeEmail = {},
          onTryAnotherWayGoogle = { tryAnotherWayClicked = true },
        )
      }
    }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("device_approval_wait_screen").assertIsDisplayed()
    composeRule.onNodeWithTag("device_approval_target_name").assertIsDisplayed()
    composeRule.onNodeWithTag("device_approval_try_another_way_button").performClick()
    composeRule.waitForIdle()
    assertTrue(tryAnotherWayClicked)
  }

  @Test
  fun `incoming login request overlay renders masked IP timer and dispatches accept`() {
    var respondedAction: Boolean? = null
    composeRule.setContent {
      TelefamTheme {
        IncomingLoginRequestFullScreenOverlay(
          request = IncomingLoginRequestState(
            requestId = "req_1",
            deviceName = "Google Pixel 9",
            location = "Nairobi, Kenya",
            ipAddressMasked = "41.89.XXX.XX",
            requestedAtMs = 1_760_000_000_000L,
            remainingSeconds = 60L,
          ),
          isResponding = false,
          actionError = null,
          onDeny = { respondedAction = false },
          onAccept = { respondedAction = true },
        )
      }
    }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("incoming_login_request_overlay").assertIsDisplayed()
    composeRule.onNodeWithText("41.89.XXX.XX").assertIsDisplayed()
    composeRule.onNodeWithText("01:00").assertIsDisplayed()
    composeRule.onNodeWithTag("incoming_login_accept_button")
      .performScrollTo()
      .performClick()
    composeRule.waitForIdle()
    assertEquals(true, respondedAction)
  }

  @Test
  fun `login approved overlay renders confirmation card and done action`() {
    var doneClicked = false
    composeRule.setContent {
      TelefamTheme {
        LoginApprovedFullScreenOverlay(
          approved = LoginApprovedOverlayState(
            deviceName = "Google Pixel 9",
            location = "Nairobi, Kenya",
            requestedAtMs = 1_760_000_000_000L,
          ),
          onDone = { doneClicked = true },
        )
      }
    }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("login_approved_overlay").assertIsDisplayed()
    composeRule.onNodeWithText("All set!").assertIsDisplayed()
    composeRule.onNodeWithTag("login_approved_done_button").performClick()
    composeRule.waitForIdle()
    assertTrue(doneClicked)
  }

  @Test
  fun `offline device context caches country signal locally and flushes silently when online`() = runTest {
    identityStore.saveEncryptedSession(
      userId = "user_existing_alex",
      email = "alex@telefam.company",
      accessToken = "tok_123",
      refreshToken = "ref_123",
      accessExpiresInSec = 900L,
      refreshExpiresInSec = 86_400L,
      profileCompleted = true,
    )
    db.telefamDao().upsertProfile(
      UserProfileEntity(
        userId = "user_existing_alex",
        email = "alex@telefam.company",
        fullName = "Alex Mwangi",
        username = "alexmwangi",
        usernameLower = "alexmwangi",
        phoneE164 = "+14155550100",
        countryIso = "US",
        gender = "Male",
        dateOfBirthIso = "1996-05-14",
        location = "Nairobi",
        bio = "",
        avatarMediaId = null,
        avatarLocalPath = null,
        avatarSha256 = null,
        avatarDHash = null,
        updatedAtMs = 1_000L,
      ),
    )
    fakeApi.resolvedTelemetryCountryIso = "KE"
    fakeApi.resolvedTelemetryCountryName = "Kenya"

    // Go offline first -> signal cached cleanly in encrypted identity store without failure
    setNetworkOnline(false)
    val offlineCountry = repository.captureAndSyncDeviceContext(force = true)
    assertEquals("", offlineCountry)
    assertFalse(identityStore.getPendingDeviceContextJson().isNullOrBlank())
    assertEquals(0, fakeApi.telemetryCalls)

    // Return online -> cached snapshot flushes silently and updates owner profile countryIso even when phone differs
    setNetworkOnline(true)
    val onlineCountry = repository.flushPendingDeviceContext(force = true)
    assertEquals("KE", onlineCountry)
    assertNull(identityStore.getPendingDeviceContextJson())
    assertEquals(1, fakeApi.telemetryCalls)
    assertEquals("KE", identityStore.getLastSyncedCountryIso())
    val profile = db.telefamDao().observeProfile("user_existing_alex").first()
    assertNotNull(profile)
    assertEquals("KE", profile!!.countryIso)
  }

  @Test
  fun `real retrofit proxy dispatches new login-request status and device-context HTTP endpoints`() = runBlocking {
    val pathsSeen = mutableListOf<String>()
    val deviceNameHeaderSeen = mutableListOf<String>()
    val server = ServerSocket(0)
    val readyLatch = CountDownLatch(1)
    val serverThread = thread(start = true) {
      readyLatch.countDown()
      repeat(2) {
        val sock = server.accept()
        sock.use { client ->
          val reader = BufferedReader(InputStreamReader(client.getInputStream()))
          val requestLine = reader.readLine().orEmpty()
          val path = requestLine.split(" ").getOrNull(1).orEmpty()
          pathsSeen += path
          var contentLength = 0
          while (true) {
            val line = reader.readLine() ?: break
            if (line.isEmpty()) break
            if (line.startsWith("Content-Length:", ignoreCase = true)) {
              contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
            }
            if (line.startsWith("X-Telefam-Device-Name:", ignoreCase = true)) {
              deviceNameHeaderSeen += line.substringAfter(":").trim()
            }
          }
          if (contentLength > 0) {
            val buf = CharArray(contentLength)
            reader.read(buf, 0, contentLength)
          }
          val jsonBody = if (path.contains("telemetry")) {
            """{"success":true,"effective_country_iso":"KE","monetization_country":"Kenya","server_epoch_ms":1760000000000}"""
          } else {
            """{"success":true,"status":"pending","remaining_seconds":55,"target_device_name":"Pixel Test"}"""
          }
          val resp = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${jsonBody.length}\r\nConnection: close\r\n\r\n$jsonBody"
          client.getOutputStream().write(resp.toByteArray(Charsets.UTF_8))
          client.getOutputStream().flush()
        }
      }
    }
    readyLatch.await(2, TimeUnit.SECONDS)

    val realService = TelefamApiService.create(
      baseUrl = "http://127.0.0.1:${server.localPort}",
      enforceHttps = false,
      deviceHeaderProvider = { DeviceContextCollector.toHttpHeaders(DeviceContextCollector.collectSnapshot(context)) },
    )!!

    val statusResp = realService.getLoginRequestStatus(
      deviceFingerprint = "fp_test",
      pollToken = "poll_tok",
      requestId = "req_99",
    )
    assertTrue(statusResp.isSuccessful)
    assertEquals("pending", statusResp.body()?.status)

    val telemetryResp = realService.syncDeviceContext(
      bearer = "Bearer tok_1",
      deviceFingerprint = "fp_test",
      body = DeviceContextTelemetryDto(deviceName = DeviceContextCollector.formatRealDeviceName()),
    )
    assertTrue(telemetryResp.isSuccessful)
    assertEquals("KE", telemetryResp.body()?.effectiveCountryIso)

    serverThread.join(3_000L)
    server.close()
    assertTrue(pathsSeen.any { it == "/v1/auth/login-requests/req_99/status" })
    assertTrue(pathsSeen.any { it == "/v1/telemetry/device-context" })
    assertTrue(deviceNameHeaderSeen.isNotEmpty())
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

  private class FakeDeviceSecurityApiService : AbstractTestChatApiStub() {
    var pollApproved = false
    var pollDenied = false
    var telemetryCalls = 0
    var resolvedTelemetryCountryIso = "KE"
    var resolvedTelemetryCountryName = "Kenya"
    var lastOAuthDto: OAuthDto? = null
    private var incomingPending = mutableListOf(
      PendingLoginRequestItemDto(
        requestId = "req_incoming_1",
        deviceName = "iPhone 16 Pro",
        location = "Mombasa, Kenya",
        ipAddressMasked = "41.89.XXX.XX",
        requestedAtMs = 1_760_000_000_000L,
        expiresAtMs = System.currentTimeMillis() + 60_000L,
        remainingSeconds = 60L,
      ),
    )

    override suspend fun requestOtp(
      deviceFingerprint: String,
      ipFingerprint: String,
      body: RequestOtpDto,
    ): Response<RequestOtpResponseDto> = Response.success(
      RequestOtpResponseDto(
        success = true,
        requiresDeviceApproval = true,
        requestId = "req_approval_1",
        pollToken = "poll_tok_1",
        targetDeviceName = "Samsung Galaxy S23",
        targetLocation = "Nairobi, Kenya",
        requestTimeMs = 1_760_000_000_000L,
        expiresInSeconds = 60L,
      ),
    )

    override suspend fun getLoginRequestStatus(
      deviceFingerprint: String,
      pollToken: String,
      requestId: String,
    ): Response<LoginApprovalStatusResponseDto> = when {
      pollApproved -> Response.success(
        LoginApprovalStatusResponseDto(
          success = true,
          status = "approved",
          remainingSeconds = 40L,
          userId = "user_existing_alex",
          email = "alex@telefam.company",
          profileCompleted = true,
          sessionToken = "access_alex",
          refreshToken = "refresh_alex",
        ),
      )
      pollDenied -> Response.success(
        LoginApprovalStatusResponseDto(
          success = true,
          status = "denied",
          blocked = true,
        ),
      )
      else -> Response.success(
        LoginApprovalStatusResponseDto(
          success = true,
          status = "pending",
          remainingSeconds = 58L,
          targetDeviceName = "Samsung Galaxy S23",
          targetLocation = "Nairobi, Kenya",
        ),
      )
    }

    override suspend fun getPendingLoginRequests(
      bearer: String,
      deviceFingerprint: String,
    ): Response<PendingLoginRequestsResponseDto> = Response.success(
      PendingLoginRequestsResponseDto(success = true, requests = incomingPending.toList()),
    )

    override suspend fun respondToLoginRequest(
      bearer: String,
      deviceFingerprint: String,
      requestId: String,
      body: RespondLoginRequestDto,
    ): Response<RespondLoginRequestResponseDto> {
      incomingPending.removeAll { it.requestId == requestId }
      return Response.success(
        RespondLoginRequestResponseDto(
          success = true,
          requestId = requestId,
          status = if (body.action == "accept") "approved" else "denied",
        ),
      )
    }

    override suspend fun syncDeviceContext(
      bearer: String,
      deviceFingerprint: String,
      body: DeviceContextTelemetryDto,
    ): Response<DeviceContextTelemetryResponseDto> {
      telemetryCalls++
      return Response.success(
        DeviceContextTelemetryResponseDto(
          success = true,
          effectiveCountryIso = resolvedTelemetryCountryIso,
          monetizationCountry = resolvedTelemetryCountryName,
          serverEpochMs = 1_760_000_000_000L,
        ),
      )
    }

    override suspend fun verifyGoogleOAuth(
      deviceFingerprint: String,
      ipFingerprint: String,
      body: OAuthDto,
    ): Response<AuthSessionResponseDto> {
      lastOAuthDto = body
      return Response.success(
        AuthSessionResponseDto(
          success = true,
          userId = "user_existing_alex",
          email = body.expectedEmail.orEmpty().ifBlank { "alex@telefam.company" },
          profileCompleted = true,
          sessionToken = "access_google_alex",
          refreshToken = "refresh_google_alex",
        ),
      )
    }

    override suspend fun getUserProfile(
      bearer: String,
      deviceFingerprint: String,
      userId: String,
    ): Response<UserProfileResponseDto> = Response.success(
      UserProfileResponseDto(
        success = true,
        userId = userId,
        fullName = "Alex Mwangi",
        username = "alexmwangi",
        bio = "Dreamer & Builder",
        location = "Nairobi, Kenya",
        countryIso = resolvedTelemetryCountryIso,
        gender = "Male",
        dateOfBirth = "1996-05-14",
        phoneNumber = "+254712345678",
        isOwner = true,
      ),
    )

    override suspend fun verifyOtp(deviceFingerprint: String, ipFingerprint: String, body: VerifyOtpDto): Response<AuthSessionResponseDto> = Response.success(AuthSessionResponseDto(success = true))
    override suspend fun verifyLink(deviceFingerprint: String, ipFingerprint: String, body: VerifyLinkDto): Response<AuthSessionResponseDto> = Response.success(AuthSessionResponseDto(success = true))
    override suspend fun refreshToken(deviceFingerprint: String, body: RefreshTokenDto): Response<AuthSessionResponseDto> = Response.success(AuthSessionResponseDto(success = true))
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
    override suspend fun getUserConnections(bearer: String, deviceFingerprint: String, userId: String, type: String, page: Int, pageSize: Int): Response<UserConnectionsResponseDto> = Response.success(UserConnectionsResponseDto(success = true))
    override suspend fun getUserFollowers(bearer: String, deviceFingerprint: String, userId: String, page: Int, pageSize: Int): Response<UserConnectionsResponseDto> = Response.success(UserConnectionsResponseDto(success = true))
    override suspend fun getUserFollowing(bearer: String, deviceFingerprint: String, userId: String, page: Int, pageSize: Int): Response<UserConnectionsResponseDto> = Response.success(UserConnectionsResponseDto(success = true))
    override suspend fun blockUser(bearer: String, deviceFingerprint: String, userId: String): Response<UserBlockResponseDto> = Response.success(UserBlockResponseDto(success = true))
    override suspend fun reportUser(bearer: String, deviceFingerprint: String, userId: String, body: UserReportRequestDto): Response<UserReportResponseDto> = Response.success(UserReportResponseDto(success = true))
    override suspend fun getSettings(bearer: String, deviceFingerprint: String): Response<SettingsBundleResponseDto> = Response.success(SettingsBundleResponseDto(success = true))
    override suspend fun updatePrivacySettings(bearer: String, deviceFingerprint: String, body: PrivacySettingsDto): Response<SettingsBundleResponseDto> = Response.success(SettingsBundleResponseDto(success = true))
    override suspend fun updateNotificationSettings(bearer: String, deviceFingerprint: String, body: NotificationSettingsDto): Response<SettingsBundleResponseDto> = Response.success(SettingsBundleResponseDto(success = true))
    override suspend fun updateContentPreferences(bearer: String, deviceFingerprint: String, body: ContentPreferencesDto): Response<SettingsBundleResponseDto> = Response.success(SettingsBundleResponseDto(success = true))
    override suspend fun listBlockedAccounts(bearer: String, deviceFingerprint: String, query: String): Response<RestrictedAccountsResponseDto> = Response.success(RestrictedAccountsResponseDto(success = true))
    override suspend fun listMutedAccounts(bearer: String, deviceFingerprint: String, query: String): Response<RestrictedAccountsResponseDto> = Response.success(RestrictedAccountsResponseDto(success = true))
    override suspend fun unblockUser(bearer: String, deviceFingerprint: String, userId: String): Response<UserBlockResponseDto> = Response.success(UserBlockResponseDto(success = true))
    override suspend fun muteUser(bearer: String, deviceFingerprint: String, userId: String): Response<UserMuteResponseDto> = Response.success(UserMuteResponseDto(success = true))
    override suspend fun unmuteUser(bearer: String, deviceFingerprint: String, userId: String): Response<UserMuteResponseDto> = Response.success(UserMuteResponseDto(success = true))
    override suspend fun submitProblemReport(bearer: String, deviceFingerprint: String, body: ProblemReportRequestDto): Response<ProblemReportResponseDto> = Response.success(ProblemReportResponseDto(success = true))
    override suspend fun listSupportTickets(bearer: String, deviceFingerprint: String): Response<SupportTicketsResponseDto> = Response.success(SupportTicketsResponseDto(success = true))
    override suspend fun searchFaqs(bearer: String, deviceFingerprint: String, query: String): Response<FaqListResponseDto> = Response.success(FaqListResponseDto(success = true))
    override suspend fun getSystemStatus(bearer: String, deviceFingerprint: String): Response<SystemStatusResponseDto> = Response.success(SystemStatusResponseDto(success = true))
    override suspend fun getLegalDocuments(bearer: String, deviceFingerprint: String): Response<LegalDocumentsResponseDto> = Response.success(LegalDocumentsResponseDto(success = true))
    override suspend fun checkAppVersion(bearer: String, deviceFingerprint: String, version: String): Response<AppVersionResponseDto> = Response.success(AppVersionResponseDto(success = true))
    override suspend fun logout(bearer: String, deviceFingerprint: String, body: LogoutRequestDto): Response<GenericSuccessDto> = Response.success(GenericSuccessDto(success = true))
    override suspend fun getChatPrivacy(bearer: String, deviceFingerprint: String): Response<telefam.company.data.network.ChatPrivacyResponseDto> = Response.success(telefam.company.data.network.ChatPrivacyResponseDto(success = true))
    override suspend fun updateChatPrivacy(bearer: String, deviceFingerprint: String, body: telefam.company.data.network.ChatPrivacySettingsDto): Response<telefam.company.data.network.ChatPrivacyResponseDto> = Response.success(telefam.company.data.network.ChatPrivacyResponseDto(success = true, chatPrivacy = body))
    override suspend fun updateChatTheme(bearer: String, deviceFingerprint: String, body: telefam.company.data.network.UpdateChatThemeRequestDto): Response<telefam.company.data.network.ChatPrivacyResponseDto> = Response.success(telefam.company.data.network.ChatPrivacyResponseDto(success = true))
    override suspend fun downloadMedia(url: String): Response<ResponseBody> = Response.success(ByteArray(0).toResponseBody("application/octet-stream".toMediaType()))
    override suspend fun getSyncSettings(bearer: String, deviceFingerprint: String): Response<telefam.company.data.network.SyncSettingsResponseDto> = Response.success(telefam.company.data.network.SyncSettingsResponseDto(success = true))
    override suspend fun updateSyncSettings(bearer: String, deviceFingerprint: String, body: telefam.company.data.network.SyncPreferencesDto): Response<telefam.company.data.network.SyncSettingsResponseDto> = Response.success(telefam.company.data.network.SyncSettingsResponseDto(success = true, syncPreferences = body))
    override suspend fun syncNow(bearer: String, deviceFingerprint: String, body: telefam.company.data.network.ManualSyncRequestDto): Response<telefam.company.data.network.ManualSyncResponseDto> = Response.success(telefam.company.data.network.ManualSyncResponseDto(success = true, ackedOpIds = body.operations.map { it.clientOpId }))
  }
}
