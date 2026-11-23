package telefam.company

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Looper
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
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
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import retrofit2.Response
import telefam.company.data.local.DeviceIdentityStore
import telefam.company.data.local.TelefamDatabase
import telefam.company.data.network.DocumentSessionRequestDto
import telefam.company.data.network.DocumentSessionResponseDto
import telefam.company.data.network.DocumentSubmitRequestDto
import telefam.company.data.network.DocumentSubmitResponseDto
import telefam.company.data.network.LivenessSessionRequestDto
import telefam.company.data.network.LivenessSessionResponseDto
import telefam.company.data.network.LivenessStepTelemetryDto
import telefam.company.data.network.LivenessSubmitRequestDto
import telefam.company.data.network.LivenessSubmitResponseDto
import telefam.company.data.network.NetworkQualityMonitor
import telefam.company.data.network.TrialActivateRequestDto
import telefam.company.data.network.VerificationCheckoutInitRequestDto
import telefam.company.data.network.VerificationCheckoutInitResponseDto
import telefam.company.data.network.VerificationCheckoutVerifyRequestDto
import telefam.company.data.network.VerificationCheckoutVerifyResponseDto
import telefam.company.data.network.VerificationPaymentDto
import telefam.company.data.network.VerificationStatusResponseDto
import telefam.company.data.repository.TelefamRepository
import telefam.company.notifications.TelefamFcmService
import telefam.company.ui.components.TelefamVerifiedBadgeIcon
import telefam.company.ui.screens.TelefamVerificationHostScreen
import telefam.company.ui.theme.TelefamTheme
import telefam.company.ui.theme.ThemeMode
import telefam.company.verification.TelefamVerificationCatalog
import telefam.company.verification.TelefamVerificationController
import telefam.company.verification.VerificationDocumentType
import telefam.company.verification.VerificationPaymentProvider
import telefam.company.verification.VerificationRoute
import telefam.company.verification.VerificationTierKind

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h915dp")
class TelefamVerificationRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var db: TelefamDatabase
  private lateinit var identityStore: DeviceIdentityStore
  private lateinit var networkMonitor: NetworkQualityMonitor
  private lateinit var fakeApi: FakeVerificationApi
  private lateinit var repository: TelefamRepository
  private lateinit var controllerScope: CoroutineScope
  private lateinit var controller: TelefamVerificationController

  @Before
  fun setUp(): Unit = runBlocking {
    context = ApplicationProvider.getApplicationContext()
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
    fakeApi = FakeVerificationApi()
    repository = TelefamRepository(
      appContext = context,
      dao = db.telefamDao(),
      identityStore = identityStore,
      networkMonitor = networkMonitor,
      apiService = fakeApi,
    )
    controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    controller = TelefamVerificationController(context, controllerScope, repository)
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    shadowOf(nm).setNotificationsEnabled(true)
  }

  @After
  fun tearDown() {
    controller.dispose()
    controllerScope.cancel()
    runBlocking { repository.chatRepository.clearLocalChatSession() }
    shadowOf(Looper.getMainLooper()).idle()
    db.close()
  }

  private fun awaitCondition(timeoutMs: Long = 3_000L, predicate: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
      shadowOf(Looper.getMainLooper()).idle()
      if (predicate()) return
      Thread.sleep(20L)
    }
    shadowOf(Looper.getMainLooper()).idle()
    assertTrue("Timed out waiting for verification condition", predicate())
  }

  private fun mountVerificationHost(themeMode: ThemeMode = ThemeMode.LIGHT) {
    composeTestRule.setContent {
      val state by controller.state.collectAsState()
      TelefamTheme(themeMode = themeMode) {
        if (state.isOpen) {
          TelefamVerificationHostScreen(
            state = state,
            onBack = controller::navigateBack,
            onDismissTutorial = controller::dismissFirstOpenTutorial,
            onReopenTutorial = controller::reopenTutorial,
            onSelectTier = controller::selectTier,
            onToggleBillingCycle = controller::toggleBillingCycle,
            onApplyTier = controller::openApplyOrSubscribe,
            onActivateFreeTrial = controller::activateFreeTrial,
            onSelectProvider = controller::selectPaymentProvider,
            onProceedToSubscribePay = controller::proceedToSubscribePayment,
            onCloseCheckoutWebView = controller::dismissCheckoutWebView,
            onPaymentReturnCaptured = controller::handlePaymentReturnDeepLink,
            onProceedFromSelfieGuidance = controller::proceedFromSelfieGuidanceToLiveCamera,
            onLiveSelfieProgress = controller::updateLiveSelfieProgress,
            onSubmitLiveSelfie = controller::submitAutoCapturedLiveness,
            onSelectDocType = controller::selectDocumentType,
            onStartLiveIdCapture = controller::startLiveIdCapture,
            onLiveDocReadiness = controller::updateLiveDocumentReadiness,
            onSubmitLiveDocument = controller::submitAutoCapturedDocument,
            onRefreshStatus = { controller.refreshStatus(silent = false) },
            onOpenTiersExplorer = controller::openTiersExplorer,
            onDismissBanner = controller::dismissStatusMessage,
          )
        }
      }
    }
  }

  @Test
  fun catalogPricingCountryConversionAndTierMetadata() {
    val keBiz = TelefamVerificationCatalog.pricingForTier(VerificationTierKind.BUSINESS, "KE")
    assertEquals("KES", keBiz.currencyCode)
    assertEquals("Ksh 1,200", keBiz.monthlyDisplay)
    assertEquals("Ksh 11,520", keBiz.annualDisplay)
    assertEquals("Ksh 14,400", keBiz.annualOriginalDisplay)
    assertEquals(20, keBiz.discountPercent)

    val keGov = TelefamVerificationCatalog.pricingForTier(VerificationTierKind.GOVERNMENT, "KE")
    assertEquals("Ksh 700", keGov.monthlyDisplay)
    assertEquals("Ksh 6,720", keGov.annualDisplay)
    assertEquals("Ksh 8,400", keGov.annualOriginalDisplay)

    val keBlue = TelefamVerificationCatalog.pricingForTier(VerificationTierKind.BLUE, "KE")
    assertEquals("Ksh 700", keBlue.monthlyDisplay)

    val ngBiz = TelefamVerificationCatalog.pricingForTier(VerificationTierKind.BUSINESS, "NG")
    assertEquals("NGN", ngBiz.currencyCode)
    assertEquals("NGN 14,400", ngBiz.monthlyDisplay)

    val usBiz = TelefamVerificationCatalog.pricingForTier(VerificationTierKind.BUSINESS, "US")
    assertEquals("USD", usBiz.currencyCode)
    assertEquals("$9.99", usBiz.monthlyDisplay)

    assertEquals(12, TelefamVerificationCatalog.businessBenefits.size)
    assertEquals(9, TelefamVerificationCatalog.governmentBenefits.size)
    assertEquals(6, TelefamVerificationCatalog.blueBenefits.size)
    assertEquals(4, TelefamVerificationCatalog.correctIdRules.size)
    assertEquals(8, TelefamVerificationCatalog.incorrectIdExamples.size)
  }

  @Test
  fun tiersPagerFirstOpenTutorialSwipeAndFreeTrialActivation() {
    context.getSharedPreferences("telefam_verification_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    mountVerificationHost(ThemeMode.LIGHT)

    controller.openGetVerified(
      profileCountryIso = "KE",
      fullName = "Alice Telefam",
      username = "alice_ke",
    )
    composeTestRule.waitForIdle()

    // First-open tutorial appears automatically and advances through steps
    composeTestRule.onNodeWithTag("verification_tutorial_overlay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("verification_tutorial_dismiss").performClick()
    composeTestRule.onNodeWithTag("verification_tutorial_dismiss").performClick()
    composeTestRule.onNodeWithTag("verification_tutorial_dismiss").performClick()
    composeTestRule.waitForIdle()
    assertFalse(controller.state.value.showFirstOpenTutorial)

    // Toggle Annual -> pricing updates to annual 20% off
    composeTestRule.onNodeWithTag("verification_annual_toggle").performClick()
    composeTestRule.waitForIdle()
    assertTrue(composeTestRule.onAllNodesWithText("Ksh 11,520 / year", substring = false).fetchSemanticsNodes().isNotEmpty())

    // Swipe sideways across Business -> Government -> Blue
    composeTestRule.onNodeWithTag("verification_tier_pager").performTouchInput { swipeLeft() }
    composeTestRule.waitForIdle()
    assertEquals(VerificationTierKind.GOVERNMENT, controller.state.value.selectedTier)

    composeTestRule.onNodeWithTag("verification_tier_pager").performTouchInput { swipeLeft() }
    composeTestRule.waitForIdle()
    assertEquals(VerificationTierKind.BLUE, controller.state.value.selectedTier)

    // Blue tier Subscribe Now button -> Trial & Payment screen
    composeTestRule.onNodeWithTag("verification_tier_list_blue").performScrollToKey("cta")
    composeTestRule.onNodeWithTag("verification_subscribe_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals(VerificationRoute.TRIAL_AND_PAYMENT, controller.state.value.currentRoute)

    // Activate 1-week free trial with Idempotency-Key -> Selfie guidance screen
    composeTestRule.onNodeWithTag("verification_trial_list").performScrollToKey("trial_card")
    composeTestRule.onNodeWithTag("verification_trial_activate_button").performClick()
    awaitCondition { controller.state.value.currentRoute == VerificationRoute.SELFIE_GUIDANCE }
    assertEquals(1, fakeApi.trialIdempKeys.size)
    assertEquals("trial_active", controller.state.value.status.planStatus)
    composeTestRule.onNodeWithTag("verification_selfie_guidance_list").performScrollToKey("start_btn")
    composeTestRule.onNodeWithTag("verification_selfie_start_camera").assertIsDisplayed()
  }

  @Test
  fun paystackAndPaypalCheckoutVerifyWithIdempotencyKeyAndDeepLinkReturn() {
    mountVerificationHost(ThemeMode.DARK)
    controller.openGetVerified(profileCountryIso = "KE", fullName = "Royal Business")
    controller.dismissFirstOpenTutorial()
    controller.openApplyOrSubscribe(VerificationTierKind.BUSINESS)
    composeTestRule.waitForIdle()

    // Choose PayPal checkout
    composeTestRule.onNodeWithTag("verification_trial_list").performScrollToKey("checkout_options")
    composeTestRule.onNodeWithTag("verification_provider_paypal").performClick()
    composeTestRule.onNodeWithTag("verification_checkout_pay_button").performClick()
    awaitCondition { controller.state.value.activeCheckoutUrl?.contains("paypal.com") == true }
    assertEquals(1, fakeApi.checkoutInitIdempKeys.size)

    // Simulate return deep link verification -> server verifies and moves to SELFIE_GUIDANCE
    val activePayId = controller.state.value.activePayment?.paymentId.orEmpty()
    controller.handlePaymentReturnDeepLink(
      paymentId = activePayId,
      paypalOrderId = "pp_order_biz",
      status = "success",
    )
    awaitCondition { controller.state.value.currentRoute == VerificationRoute.SELFIE_GUIDANCE }
    assertEquals(1, fakeApi.checkoutVerifyIdempKeys.size)
    assertEquals("subscribed", controller.state.value.status.planStatus)

    // Switch back to Trial/Payment and test Paystack init
    controller.openApplyOrSubscribe(VerificationTierKind.GOVERNMENT)
    fakeApi.planStatus = "none"
    controller.refreshStatus(silent = false)
    awaitCondition { !controller.state.value.isLoadingStatus }
    controller.selectPaymentProvider(VerificationPaymentProvider.PAYSTACK)
    controller.proceedToSubscribePayment()
    awaitCondition { controller.state.value.activeCheckoutUrl?.contains("paystack.com") == true }
  }

  @Test
  fun liveSelfieAndDocumentOcrFlowReachesUnderReviewWithSecurePurgeAndBadgeUnlock() {
    mountVerificationHost(ThemeMode.LIGHT)
    fakeApi.planStatus = "trial_active"
    controller.openGetVerified(profileCountryIso = "KE", fullName = "Rono Kiptoo", username = "rono_kiptoo")
    controller.dismissFirstOpenTutorial()
    composeTestRule.waitForIdle()

    // Open live selfie flow -> issues server-signed challenge token
    controller.openApplyOrSubscribe(VerificationTierKind.BLUE)
    awaitCondition { controller.state.value.currentRoute == VerificationRoute.SELFIE_GUIDANCE }
    composeTestRule.onNodeWithTag("verification_selfie_guidance_list").performScrollToKey("start_btn")
    composeTestRule.onNodeWithTag("verification_selfie_start_camera").performClick()
    awaitCondition { controller.state.value.currentRoute == VerificationRoute.SELFIE_LIVENESS }
    assertTrue(controller.state.value.livenessChallengeToken.isNotBlank())
    assertEquals(listOf("left", "right", "up", "left", "right", "center"), controller.state.value.livenessSequence)

    // Submit 100%-score telemetry -> advances to ID Guidance & Selection
    val steps = listOf("left", "right", "up", "left", "right", "center").mapIndexed { idx, dir ->
      LivenessStepTelemetryDto(
        step = dir,
        yaw = when (dir) {
          "left" -> -22.0
          "right" -> 22.0
          else -> 0.5
        },
        pitch = if (dir == "up") 18.0 else 0.0,
        leftEyeOpen = 0.96,
        rightEyeOpen = 0.95,
        luma = 128.0,
        capturedAtMs = System.currentTimeMillis() + idx * 500L,
      )
    }
    controller.submitAutoCapturedLiveness(ByteArray(128) { 0x42 }, steps)
    awaitCondition { controller.state.value.currentRoute == VerificationRoute.ID_GUIDANCE_AND_SELECT }

    // Screenshot 4 ID guidance rules + all 8 bad examples + secure purge notice visible
    composeTestRule.onNodeWithTag("verification_doc_guidance_list").performScrollToKey("doc_types")
    composeTestRule.onNodeWithTag("verification_doc_select_military_id").performClick()
    assertEquals(VerificationDocumentType.MILITARY_ID, controller.state.value.selectedDocumentType)
    composeTestRule.onNodeWithTag("verification_doc_guidance_list").performScrollToKey("correct_id_panel")
    composeTestRule.onNodeWithTag("verification_correct_id_guidance").assertIsDisplayed()
    composeTestRule.onNodeWithTag("verification_doc_guidance_list").performScrollToKey("incorrect_id_panel")
    composeTestRule.onNodeWithText("Too blurry").assertIsDisplayed()
    composeTestRule.onNodeWithText("Glare / Reflection").assertIsDisplayed()
    composeTestRule.onNodeWithTag("verification_doc_guidance_list").performScrollToKey("purge_and_start")
    composeTestRule.onNodeWithTag("verification_secure_delete_notice").assertIsDisplayed()

    // Issue document capture challenge & submit OCR capture -> Under Review 3 working days
    composeTestRule.onNodeWithTag("verification_doc_start_camera").performClick()
    awaitCondition { controller.state.value.currentRoute == VerificationRoute.ID_LIVE_CAPTURE }
    assertTrue(controller.state.value.documentChallengeToken.isNotBlank())

    controller.submitAutoCapturedDocument(
      jpegBytes = ByteArray(256) { 0x55 },
      ocrRawText = "REPUBLIC OF KENYA\nRONO KIPTOO\nID 33445566\nEXPIRY 2031-12-31",
      sharpnessScore = 0.92,
      luminanceMean = 140.0,
      glareRatio = 0.02,
      allCornersDetected = true,
    )
    awaitCondition { controller.state.value.currentRoute == VerificationRoute.STATUS_REVIEW }

    composeTestRule.onNodeWithTag("verification_under_review_card").assertIsDisplayed()
    composeTestRule.onNodeWithText("It always takes 3 working days to review. You will be notified.")
      .assertIsDisplayed()
    assertTrue(controller.state.value.status.mergedToProfile)
    assertTrue(controller.state.value.status.documentsPurged)

    // Server approves verification -> badge unlocks on STATUS_REVIEW
    fakeApi.reviewStatus = "verified"
    fakeApi.verifiedTier = "blue"
    composeTestRule.onNodeWithTag("verification_refresh_status").performClick()
    awaitCondition { controller.state.value.status.verifiedTier == "blue" }
    composeTestRule.onNodeWithTag("verification_unlocked_badge_card").assertIsDisplayed()
  }

  @Test
  fun verifiedBadgesRenderInLightAndDarkThemesAndFcmNotificationDispatches() = runBlocking {
    composeTestRule.setContent {
      TelefamTheme(themeMode = ThemeMode.DARK) {
        Column {
          TelefamVerifiedBadgeIcon(tier = "business")
          TelefamVerifiedBadgeIcon(tier = "government")
          TelefamVerifiedBadgeIcon(tier = "blue")
        }
      }
    }
    composeTestRule.onNodeWithContentDescription("Verified business badge").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Verified government badge").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Verified blue badge").assertIsDisplayed()

    val fcm = Robolectric.setupService(TelefamFcmService::class.java)
    fcm.handlePushPayload(
      repo = repository,
      data = mapOf(
        "type" to "verification_update",
        "notification_id" to "notif_ver_1",
        "recipient_user_id" to "user_alice",
        "title" to "Telefam Verified",
        "body" to "Your Business verification badge is now active.",
        "created_at_ms" to System.currentTimeMillis().toString(),
      ),
    )

    val stored = repository.observeNotifications("user_alice").first()
    val notif = stored.firstOrNull { it.id == "notif_ver_1" }
    assertNotNull(notif)
    assertEquals("verification_update", notif?.kind)

    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    assertTrue(shadowOf(nm).allNotifications.isNotEmpty())
  }

  private class FakeVerificationApi : AbstractTestChatApiStub() {
    var verifiedTier: String = "none"
    var pendingTier: String = "business"
    var planStatus: String = "none"
    var trialEligible: Boolean = true
    var livenessStatus: String = "not_started"
    var livenessScore: Int = 0
    var docStatus: String = "not_started"
    var reviewStatus: String = "not_started"
    var docLast4: String = ""
    var extractedName: String = ""
    var mergedToProfile: Boolean = false
    var docsPurged: Boolean = false

    val trialIdempKeys = mutableListOf<String>()
    val checkoutInitIdempKeys = mutableListOf<String>()
    val checkoutVerifyIdempKeys = mutableListOf<String>()

    private fun statusDto(countryIso: String = "KE"): VerificationStatusResponseDto {
      return VerificationStatusResponseDto(
        success = true,
        userId = "user_alice",
        verifiedTier = verifiedTier,
        pendingTier = pendingTier,
        billingCycle = "monthly",
        planStatus = planStatus,
        trialEligible = trialEligible,
        livenessStatus = livenessStatus,
        livenessScore = livenessScore,
        documentStatus = docStatus,
        docLast4 = docLast4,
        docExpiryIso = if (docLast4.isNotBlank()) "2031-12-31" else "",
        extractedFullName = extractedName,
        mergedToProfile = mergedToProfile,
        reviewStatus = reviewStatus,
        reviewEtaWorkingDays = 3,
        documentsPurged = docsPurged,
        countryIso = countryIso.ifBlank { "KE" },
        tiers = listOf(
          TelefamVerificationCatalog.pricingForTier(VerificationTierKind.BUSINESS, countryIso.ifBlank { "KE" }),
          TelefamVerificationCatalog.pricingForTier(VerificationTierKind.GOVERNMENT, countryIso.ifBlank { "KE" }),
          TelefamVerificationCatalog.pricingForTier(VerificationTierKind.BLUE, countryIso.ifBlank { "KE" }),
        ),
        serverEpochMs = System.currentTimeMillis(),
      )
    }

    override suspend fun getVerificationStatus(
      bearer: String,
      deviceFingerprint: String,
      countryIso: String,
    ): Response<VerificationStatusResponseDto> = Response.success(statusDto(countryIso))

    override suspend fun activateVerificationTrial(
      bearer: String,
      deviceFingerprint: String,
      idempotencyKey: String,
      body: TrialActivateRequestDto,
    ): Response<VerificationStatusResponseDto> {
      trialIdempKeys.add(idempotencyKey)
      pendingTier = body.tier
      planStatus = "trial_active"
      trialEligible = false
      return Response.success(statusDto())
    }

    override suspend fun initVerificationCheckout(
      bearer: String,
      deviceFingerprint: String,
      idempotencyKey: String,
      body: VerificationCheckoutInitRequestDto,
    ): Response<VerificationCheckoutInitResponseDto> {
      checkoutInitIdempKeys.add(idempotencyKey)
      pendingTier = body.tier
      val url = if (body.provider == "paypal") {
        "https://www.sandbox.paypal.com/checkoutnow?token=pp_order_biz"
      } else {
        "https://checkout.paystack.com/telefam_ver_ref"
      }
      return Response.success(
        VerificationCheckoutInitResponseDto(
          success = true,
          payment = VerificationPaymentDto(
            paymentId = "vpay_${body.tier}",
            tier = body.tier,
            billingCycle = body.billingCycle,
            provider = body.provider,
            providerReference = "ps_ver_ref",
            providerOrderId = "pp_order_biz",
            expectedAmountMinor = 120000L,
            expectedCurrency = "KES",
            status = "pending",
            checkoutUrl = url,
          ),
          status = statusDto(body.countryIso),
          returnUrl = "telefam://verification/payment-return",
        ),
      )
    }

    override suspend fun verifyVerificationCheckout(
      bearer: String,
      deviceFingerprint: String,
      idempotencyKey: String,
      body: VerificationCheckoutVerifyRequestDto,
    ): Response<VerificationCheckoutVerifyResponseDto> {
      checkoutVerifyIdempKeys.add(idempotencyKey)
      planStatus = "subscribed"
      return Response.success(
        VerificationCheckoutVerifyResponseDto(
          success = true,
          payment = VerificationPaymentDto(
            paymentId = body.paymentId,
            tier = pendingTier,
            status = "succeeded",
            verifiedVia = "api_verify",
          ),
          status = statusDto(),
        ),
      )
    }

    override suspend fun issueLivenessSession(
      bearer: String,
      deviceFingerprint: String,
      body: LivenessSessionRequestDto,
    ): Response<LivenessSessionResponseDto> {
      if (body.screenCaptureActive) {
        return Response.error(403, """{"message":"Screen capture blocked"}""".toResponseBody("application/json".toMediaType()))
      }
      return Response.success(
        LivenessSessionResponseDto(
          success = true,
          challengeId = "liveness_1",
          challengeToken = "signed_tok_1",
          nonce = "nonce_1",
          sequence = listOf("left", "right", "up", "left", "right", "center"),
          expiresAtMs = System.currentTimeMillis() + 180_000L,
        ),
      )
    }

    override suspend fun submitLiveness(
      bearer: String,
      deviceFingerprint: String,
      body: LivenessSubmitRequestDto,
    ): Response<LivenessSubmitResponseDto> {
      livenessStatus = "passed"
      livenessScore = 100
      return Response.success(
        LivenessSubmitResponseDto(
          success = true,
          passed = true,
          livenessScore = 100,
          message = "Live facial check verified at 100%.",
          status = statusDto(),
        ),
      )
    }

    override suspend fun issueDocumentSession(
      bearer: String,
      deviceFingerprint: String,
      body: DocumentSessionRequestDto,
    ): Response<DocumentSessionResponseDto> {
      return Response.success(
        DocumentSessionResponseDto(
          success = true,
          challengeId = "doc_1",
          challengeToken = "signed_doc_tok_1",
          documentType = body.documentType,
          nonce = "doc_nonce_1",
          expiresAtMs = System.currentTimeMillis() + 240_000L,
        ),
      )
    }

    override suspend fun submitDocument(
      bearer: String,
      deviceFingerprint: String,
      body: DocumentSubmitRequestDto,
    ): Response<DocumentSubmitResponseDto> {
      docStatus = "submitted"
      docLast4 = "5566"
      extractedName = "RONO KIPTOO"
      mergedToProfile = true
      docsPurged = true
      reviewStatus = "under_review"
      return Response.success(
        DocumentSubmitResponseDto(
          success = true,
          accepted = true,
          extractedFullName = extractedName,
          extractedDocLast4 = docLast4,
          extractedExpiry = "2031-12-31",
          documentsPurged = true,
          message = "Under review. It always takes 3 working days to review. You will be notified.",
          status = statusDto(),
        ),
      )
    }
  }
}
