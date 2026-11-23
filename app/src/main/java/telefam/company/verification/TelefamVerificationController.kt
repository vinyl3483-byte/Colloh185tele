package telefam.company.verification

import android.content.Context
import android.hardware.display.DisplayManager
import android.util.Base64
import android.view.Display
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import telefam.company.data.network.DocumentSessionRequestDto
import telefam.company.data.network.DocumentSubmitRequestDto
import telefam.company.data.network.LivenessSessionRequestDto
import telefam.company.data.network.LivenessStepTelemetryDto
import telefam.company.data.network.LivenessSubmitRequestDto
import telefam.company.data.network.TrialActivateRequestDto
import telefam.company.data.network.VerificationCheckoutInitRequestDto
import telefam.company.data.network.VerificationCheckoutVerifyRequestDto
import telefam.company.data.network.VerificationPaymentDto
import telefam.company.data.network.VerificationStatusResponseDto
import telefam.company.data.repository.TelefamRepository
import telefam.company.stars.TelefamStarsCatalog
import java.util.UUID

data class TelefamVerificationUiState(
  val isOpen: Boolean = false,
  val routeStack: List<VerificationRoute> = listOf(VerificationRoute.TIERS_PAGER),
  val showFirstOpenTutorial: Boolean = false,
  val selectedTier: VerificationTierKind = VerificationTierKind.BUSINESS,
  val billingCycle: VerificationBillingCycle = VerificationBillingCycle.MONTHLY,
  val selectedProvider: VerificationPaymentProvider = VerificationPaymentProvider.PAYSTACK,
  val selectedDocumentType: VerificationDocumentType = VerificationDocumentType.NATIONAL_ID,
  val countryIso: String = "US",
  val profileFullName: String = "",
  val profileUsername: String = "",
  val profileAvatarUrl: String = "",
  val status: VerificationStatusResponseDto = VerificationStatusResponseDto(),
  val activePayment: VerificationPaymentDto? = null,
  val activeCheckoutUrl: String? = null,
  val activeVerifyIdempKey: String? = null,
  val livenessChallengeId: String = "",
  val livenessChallengeToken: String = "",
  val livenessSequence: List<String> = listOf("left", "right", "up", "left", "right", "center"),
  val livenessExpiresAtMs: Long = 0L,
  val currentLivenessStepIndex: Int = 0,
  val currentLivenessScore: Int = 0,
  val liveFaceVisible: Boolean = false,
  val liveLightOk: Boolean = true,
  val liveMovementPrompt: String = "Center your face inside the oval",
  val documentChallengeId: String = "",
  val documentChallengeToken: String = "",
  val documentExpiresAtMs: Long = 0L,
  val docCornersReady: Boolean = false,
  val docSharpReady: Boolean = false,
  val docLightingReady: Boolean = false,
  val docGlareFreeReady: Boolean = false,
  val docOcrPreviewReady: Boolean = false,
  val docQualityReadinessPct: Int = 0,
  val screenCaptureBlocked: Boolean = false,
  val isLoadingStatus: Boolean = false,
  val isActivatingTrial: Boolean = false,
  val isInitiatingCheckout: Boolean = false,
  val isVerifyingPayment: Boolean = false,
  val isSubmittingLiveness: Boolean = false,
  val isSubmittingDocument: Boolean = false,
  val statusMessage: String? = null,
  val errorMessage: String? = null,
) {
  val currentRoute: VerificationRoute
    get() = routeStack.lastOrNull() ?: VerificationRoute.TIERS_PAGER

  val requiresSecureFlag: Boolean
    get() = isOpen && (currentRoute == VerificationRoute.SELFIE_LIVENESS || currentRoute == VerificationRoute.ID_LIVE_CAPTURE)
}

class TelefamVerificationController(
  private val appContext: Context,
  private val scope: CoroutineScope,
  private val repository: TelefamRepository,
) {
  private val prefs = appContext.getSharedPreferences("telefam_verification_prefs", Context.MODE_PRIVATE)
  private val _state = MutableStateFlow(TelefamVerificationUiState())
  val state: StateFlow<TelefamVerificationUiState> = _state.asStateFlow()

  private var lastCheckoutSig: String = ""
  private var lastCheckoutIdempKey: String = ""
  private var trialIdempKey: String = ""

  private val displayManager = appContext.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
  private val displayListener = object : DisplayManager.DisplayListener {
    override fun onDisplayAdded(displayId: Int) { refreshScreenCaptureFlag() }
    override fun onDisplayRemoved(displayId: Int) { refreshScreenCaptureFlag() }
    override fun onDisplayChanged(displayId: Int) { refreshScreenCaptureFlag() }
  }

  init {
    displayManager?.registerDisplayListener(displayListener, null)
    refreshScreenCaptureFlag()

    scope.launch {
      repository.chatRepository.wsEvents.collect { ev ->
        if (ev.event == "verification.updated") {
          handleRealtimeVerificationEvent(ev.rawJson)
          refreshStatus(silent = true)
        }
      }
    }
  }

  fun refreshScreenCaptureFlag(): Boolean {
    val activeCapture = detectActiveScreenMirrorOrRecording()
    _state.update { it.copy(screenCaptureBlocked = activeCapture) }
    return activeCapture
  }

  private fun detectActiveScreenMirrorOrRecording(): Boolean {
    val dm = displayManager ?: return false
    return runCatching {
      dm.displays.any { d ->
        d.displayId != Display.DEFAULT_DISPLAY &&
          (d.flags and Display.FLAG_PRESENTATION) == 0 &&
          d.state != Display.STATE_OFF
      }
    }.getOrDefault(false)
  }

  private fun handleRealtimeVerificationEvent(rawJson: String) {
    if (rawJson.isBlank()) return
    val obj = runCatching { JSONObject(rawJson) }.getOrNull() ?: return
    val verifiedTier = obj.optString("verified_tier", "")
    val reviewStatus = obj.optString("review_status", "")
    _state.update { s ->
      val updatedStatus = s.status.copy(
        verifiedTier = verifiedTier.ifBlank { s.status.verifiedTier },
        reviewStatus = reviewStatus.ifBlank { s.status.reviewStatus },
      )
      val toast = when {
        reviewStatus == "verified" && verifiedTier != "none" && verifiedTier.isNotBlank() ->
          "Telefam ${VerificationTierKind.fromWire(verifiedTier).headerTitle} badge unlocked"
        reviewStatus == "under_review" ->
          "Identity documents matched. Application under review (3 working days)."
        else -> s.statusMessage
      }
      s.copy(status = updatedStatus, statusMessage = toast)
    }
  }

  fun openGetVerified(
    profileCountryIso: String?,
    locationHint: String = "",
    fullName: String = "",
    username: String = "",
    avatarUrl: String = "",
    initialVerifiedTier: String = "none",
  ) {
    val iso = TelefamStarsCatalog.resolveCountryIso(profileCountryIso, locationHint)
    val tutorialSeen = prefs.getBoolean(KEY_TUTORIAL_SEEN, false)
    _state.update { s ->
      val existingReview = s.status.reviewStatus
      val initialRoute = when {
        initialVerifiedTier != "none" && initialVerifiedTier.isNotBlank() -> VerificationRoute.STATUS_REVIEW
        existingReview == "under_review" || existingReview == "verified" -> VerificationRoute.STATUS_REVIEW
        else -> VerificationRoute.TIERS_PAGER
      }
      s.copy(
        isOpen = true,
        routeStack = listOf(initialRoute),
        showFirstOpenTutorial = !tutorialSeen && initialRoute == VerificationRoute.TIERS_PAGER,
        countryIso = iso,
        profileFullName = fullName.ifBlank { s.profileFullName },
        profileUsername = username.ifBlank { s.profileUsername },
        profileAvatarUrl = avatarUrl.ifBlank { s.profileAvatarUrl },
        errorMessage = null,
      )
    }
    refreshScreenCaptureFlag()
    refreshStatus(silent = false)
  }

  fun dismissFirstOpenTutorial() {
    prefs.edit().putBoolean(KEY_TUTORIAL_SEEN, true).apply()
    _state.update { it.copy(showFirstOpenTutorial = false) }
  }

  fun reopenTutorial() {
    _state.update { it.copy(showFirstOpenTutorial = true) }
  }

  fun selectTier(tier: VerificationTierKind) {
    val defaultDoc = TelefamVerificationCatalog.documentOptionsForTier(tier).first()
    _state.update { it.copy(selectedTier = tier, selectedDocumentType = defaultDoc, errorMessage = null) }
  }

  fun toggleBillingCycle(annual: Boolean) {
    _state.update {
      it.copy(
        billingCycle = if (annual) VerificationBillingCycle.ANNUAL else VerificationBillingCycle.MONTHLY,
        errorMessage = null,
      )
    }
  }

  fun selectPaymentProvider(provider: VerificationPaymentProvider) {
    _state.update { it.copy(selectedProvider = provider, errorMessage = null) }
  }

  fun selectDocumentType(docType: VerificationDocumentType) {
    _state.update { it.copy(selectedDocumentType = docType, errorMessage = null) }
  }

  fun openApplyOrSubscribe(tier: VerificationTierKind) {
    val st = _state.value.status
    val hasActivePlan = st.planStatus == "trial_active" || st.planStatus == "subscribed"
    val nextRoute = when {
      st.reviewStatus == "under_review" || st.reviewStatus == "verified" -> VerificationRoute.STATUS_REVIEW
      hasActivePlan && st.livenessStatus == "passed" -> VerificationRoute.ID_GUIDANCE_AND_SELECT
      hasActivePlan -> VerificationRoute.SELFIE_GUIDANCE
      else -> VerificationRoute.TRIAL_AND_PAYMENT
    }
    _state.update { s ->
      s.copy(
        selectedTier = tier,
        routeStack = (s.routeStack + nextRoute).distinctLastDuplicate(),
        errorMessage = null,
      )
    }
  }

  fun proceedFromSelfieGuidanceToLiveCamera() {
    if (refreshScreenCaptureFlag()) {
      _state.update {
        it.copy(errorMessage = "Disable screen recording or mirroring before starting live selfie verification.")
      }
      return
    }
    val snap = _state.value
    val api = repository.getApiServiceOrNull()
    if (api == null) {
      _state.update { it.copy(errorMessage = "Network unavailable. Connect to the internet to verify.") }
      return
    }
    _state.update { it.copy(isSubmittingLiveness = true, errorMessage = null) }
    scope.launch(Dispatchers.IO) {
      val token = repository.sessionFlow.first().accessToken.orEmpty()
      if (token.isBlank()) {
        _state.update { it.copy(isSubmittingLiveness = false, errorMessage = "Please sign in first") }
        return@launch
      }
      val fp = repository.getDeviceFingerprint()
      val resp = runCatching {
        api.issueLivenessSession(
          "Bearer $token",
          fp,
          LivenessSessionRequestDto(
            tier = snap.selectedTier.wire,
            screenCaptureActive = refreshScreenCaptureFlag(),
          ),
        )
      }.getOrNull()
      if (resp?.isSuccessful == true) {
        val body = resp.body()
        if (body != null && body.success && body.challengeId.isNotBlank()) {
          val seq = body.sequence.ifEmpty { listOf("left", "right", "up", "left", "right", "center") }
          _state.update { s ->
            s.copy(
              isSubmittingLiveness = false,
              livenessChallengeId = body.challengeId,
              livenessChallengeToken = body.challengeToken,
              livenessSequence = seq,
              livenessExpiresAtMs = body.expiresAtMs,
              currentLivenessStepIndex = 0,
              currentLivenessScore = 0,
              liveMovementPrompt = TelefamVerificationCatalog.stepDirectionLabel(seq.first()),
              routeStack = (s.routeStack + VerificationRoute.SELFIE_LIVENESS).distinctLastDuplicate(),
            )
          }
          return@launch
        }
      }
      val msg = parseErrorMessage(resp?.errorBody()?.string(), resp?.code() ?: 0)
      _state.update { it.copy(isSubmittingLiveness = false, errorMessage = msg) }
    }
  }

  fun updateLiveSelfieProgress(
    stepIndex: Int,
    scorePct: Int,
    faceVisible: Boolean,
    lightOk: Boolean,
    promptText: String,
  ) {
    _state.update {
      it.copy(
        currentLivenessStepIndex = stepIndex,
        currentLivenessScore = scorePct.coerceIn(0, 100),
        liveFaceVisible = faceVisible,
        liveLightOk = lightOk,
        liveMovementPrompt = promptText,
      )
    }
  }

  fun submitAutoCapturedLiveness(
    jpegBytes: ByteArray,
    steps: List<LivenessStepTelemetryDto>,
  ) {
    val snap = _state.value
    if (snap.isSubmittingLiveness) return
    if (refreshScreenCaptureFlag()) {
      _state.update {
        it.copy(errorMessage = "Screen recording detected. Live capture blocked for your security.")
      }
      return
    }
    val api = repository.getApiServiceOrNull()
    if (api == null) {
      _state.update { it.copy(errorMessage = "Network unavailable") }
      return
    }
    _state.update { it.copy(isSubmittingLiveness = true, currentLivenessScore = 100, errorMessage = null) }
    val b64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)

    scope.launch(Dispatchers.IO) {
      val token = repository.sessionFlow.first().accessToken.orEmpty()
      val fp = repository.getDeviceFingerprint()
      val resp = runCatching {
        api.submitLiveness(
          "Bearer $token",
          fp,
          LivenessSubmitRequestDto(
            challengeId = snap.livenessChallengeId,
            challengeToken = snap.livenessChallengeToken,
            screenCaptureActive = detectActiveScreenMirrorOrRecording(),
            selfieImageB64 = b64,
            steps = steps,
          ),
        )
      }.getOrNull()

      if (resp?.isSuccessful == true) {
        val body = resp.body()
        if (body != null && body.success && body.passed) {
          _state.update { s ->
            s.copy(
              isSubmittingLiveness = false,
              status = body.status,
              statusMessage = body.message,
              routeStack = (s.routeStack + VerificationRoute.ID_GUIDANCE_AND_SELECT).distinctLastDuplicate(),
            )
          }
          return@launch
        } else if (body != null) {
          _state.update { s ->
            s.copy(
              isSubmittingLiveness = false,
              currentLivenessStepIndex = 0,
              currentLivenessScore = body.livenessScore,
              errorMessage = body.message.ifBlank { "Face liveness check did not reach 100%. Please try again." },
            )
          }
          return@launch
        }
      }
      val msg = parseErrorMessage(resp?.errorBody()?.string(), resp?.code() ?: 0)
      _state.update {
        it.copy(
          isSubmittingLiveness = false,
          currentLivenessStepIndex = 0,
          currentLivenessScore = 0,
          errorMessage = msg,
        )
      }
    }
  }

  fun startLiveIdCapture() {
    if (refreshScreenCaptureFlag()) {
      _state.update {
        it.copy(errorMessage = "Disable screen recording or mirroring before scanning your ID.")
      }
      return
    }
    val snap = _state.value
    val api = repository.getApiServiceOrNull()
    if (api == null) {
      _state.update { it.copy(errorMessage = "Network unavailable") }
      return
    }
    _state.update { it.copy(isSubmittingDocument = true, errorMessage = null) }
    scope.launch(Dispatchers.IO) {
      val token = repository.sessionFlow.first().accessToken.orEmpty()
      if (token.isBlank()) {
        _state.update { it.copy(isSubmittingDocument = false, errorMessage = "Please sign in first") }
        return@launch
      }
      val fp = repository.getDeviceFingerprint()
      val resp = runCatching {
        api.issueDocumentSession(
          "Bearer $token",
          fp,
          DocumentSessionRequestDto(
            documentType = snap.selectedDocumentType.wire,
            screenCaptureActive = detectActiveScreenMirrorOrRecording(),
          ),
        )
      }.getOrNull()
      if (resp?.isSuccessful == true) {
        val body = resp.body()
        if (body != null && body.success && body.challengeId.isNotBlank()) {
          _state.update { s ->
            s.copy(
              isSubmittingDocument = false,
              documentChallengeId = body.challengeId,
              documentChallengeToken = body.challengeToken,
              documentExpiresAtMs = body.expiresAtMs,
              docCornersReady = false,
              docSharpReady = false,
              docLightingReady = false,
              docGlareFreeReady = false,
              docOcrPreviewReady = false,
              docQualityReadinessPct = 0,
              routeStack = (s.routeStack + VerificationRoute.ID_LIVE_CAPTURE).distinctLastDuplicate(),
            )
          }
          return@launch
        }
      }
      val msg = parseErrorMessage(resp?.errorBody()?.string(), resp?.code() ?: 0)
      _state.update { it.copy(isSubmittingDocument = false, errorMessage = msg) }
    }
  }

  fun updateLiveDocumentReadiness(
    cornersReady: Boolean,
    sharpReady: Boolean,
    lightingReady: Boolean,
    glareFreeReady: Boolean,
    ocrReady: Boolean,
    readinessPct: Int,
  ) {
    _state.update {
      it.copy(
        docCornersReady = cornersReady,
        docSharpReady = sharpReady,
        docLightingReady = lightingReady,
        docGlareFreeReady = glareFreeReady,
        docOcrPreviewReady = ocrReady,
        docQualityReadinessPct = readinessPct.coerceIn(0, 100),
      )
    }
  }

  fun submitAutoCapturedDocument(
    jpegBytes: ByteArray,
    ocrRawText: String,
    sharpnessScore: Double,
    luminanceMean: Double,
    glareRatio: Double,
    allCornersDetected: Boolean,
  ) {
    val snap = _state.value
    if (snap.isSubmittingDocument) return
    if (refreshScreenCaptureFlag()) {
      _state.update {
        it.copy(errorMessage = "Screen recording detected. Document capture blocked for your security.")
      }
      return
    }
    val api = repository.getApiServiceOrNull()
    if (api == null) {
      _state.update { it.copy(errorMessage = "Network unavailable") }
      return
    }
    _state.update { it.copy(isSubmittingDocument = true, errorMessage = null) }
    val b64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)

    scope.launch(Dispatchers.IO) {
      val token = repository.sessionFlow.first().accessToken.orEmpty()
      val fp = repository.getDeviceFingerprint()
      val resp = runCatching {
        api.submitDocument(
          "Bearer $token",
          fp,
          DocumentSubmitRequestDto(
            challengeId = snap.documentChallengeId,
            challengeToken = snap.documentChallengeToken,
            documentType = snap.selectedDocumentType.wire,
            screenCaptureActive = detectActiveScreenMirrorOrRecording(),
            documentImageB64 = b64,
            clientOcrRawText = ocrRawText,
            sharpnessScore = sharpnessScore,
            luminanceMean = luminanceMean,
            glareRatio = glareRatio,
            allCornersDetected = allCornersDetected,
          ),
        )
      }.getOrNull()

      if (resp?.isSuccessful == true) {
        val body = resp.body()
        if (body != null && body.success && body.accepted) {
          _state.update { s ->
            s.copy(
              isSubmittingDocument = false,
              status = body.status,
              statusMessage = body.message,
              routeStack = listOf(VerificationRoute.STATUS_REVIEW),
            )
          }
          return@launch
        } else if (body != null) {
          _state.update {
            it.copy(
              isSubmittingDocument = false,
              errorMessage = body.message.ifBlank { "Document details could not be validated. Retake a clear capture." },
            )
          }
          return@launch
        }
      }
      val msg = parseErrorMessage(resp?.errorBody()?.string(), resp?.code() ?: 0)
      _state.update { it.copy(isSubmittingDocument = false, errorMessage = msg) }
    }
  }

  fun activateFreeTrial() {
    val snap = _state.value
    if (snap.isActivatingTrial) return
    val api = repository.getApiServiceOrNull()
    if (api == null) {
      _state.update { it.copy(errorMessage = "Network unavailable") }
      return
    }
    if (trialIdempKey.isBlank()) {
      trialIdempKey = UUID.randomUUID().toString()
    }
    _state.update { it.copy(isActivatingTrial = true, errorMessage = null) }
    scope.launch(Dispatchers.IO) {
      val token = repository.sessionFlow.first().accessToken.orEmpty()
      if (token.isBlank()) {
        _state.update { it.copy(isActivatingTrial = false, errorMessage = "Please sign in first") }
        return@launch
      }
      val fp = repository.getDeviceFingerprint()
      val resp = runCatching {
        api.activateVerificationTrial(
          "Bearer $token",
          fp,
          trialIdempKey,
          TrialActivateRequestDto(
            tier = snap.selectedTier.wire,
            billingCycle = snap.billingCycle.wire,
            idempotencyKey = trialIdempKey,
          ),
        )
      }.getOrNull()
      if (resp?.isSuccessful == true) {
        val body = resp.body()
        if (body != null && body.success) {
          trialIdempKey = ""
          _state.update { s ->
            s.copy(
              isActivatingTrial = false,
              status = body,
              statusMessage = "First week free trial activated. Complete live selfie next.",
              routeStack = (s.routeStack + VerificationRoute.SELFIE_GUIDANCE).distinctLastDuplicate(),
            )
          }
          return@launch
        }
      }
      val msg = parseErrorMessage(resp?.errorBody()?.string(), resp?.code() ?: 0)
      _state.update { it.copy(isActivatingTrial = false, errorMessage = msg) }
    }
  }

  fun proceedToSubscribePayment() {
    val snap = _state.value
    if (snap.isInitiatingCheckout) return
    val api = repository.getApiServiceOrNull()
    if (api == null) {
      _state.update { it.copy(errorMessage = "Network unavailable") }
      return
    }
    val sig = "${snap.selectedProvider.wire}:${snap.selectedTier.wire}:${snap.billingCycle.wire}:${snap.countryIso}"
    val idempKey = if (sig == lastCheckoutSig && lastCheckoutIdempKey.isNotBlank()) {
      lastCheckoutIdempKey
    } else {
      UUID.randomUUID().toString().also {
        lastCheckoutSig = sig
        lastCheckoutIdempKey = it
      }
    }
    _state.update { it.copy(isInitiatingCheckout = true, errorMessage = null) }

    scope.launch(Dispatchers.IO) {
      val token = repository.sessionFlow.first().accessToken.orEmpty()
      if (token.isBlank()) {
        _state.update { it.copy(isInitiatingCheckout = false, errorMessage = "Please sign in first") }
        return@launch
      }
      val fp = repository.getDeviceFingerprint()
      val resp = runCatching {
        api.initVerificationCheckout(
          "Bearer $token",
          fp,
          idempKey,
          VerificationCheckoutInitRequestDto(
            tier = snap.selectedTier.wire,
            billingCycle = snap.billingCycle.wire,
            provider = snap.selectedProvider.wire,
            countryIso = snap.countryIso,
            idempotencyKey = idempKey,
          ),
        )
      }.getOrNull()

      if (resp?.isSuccessful == true) {
        val body = resp.body()
        if (body != null && body.success && body.payment.checkoutUrl.isNotBlank()) {
          _state.update {
            it.copy(
              isInitiatingCheckout = false,
              activePayment = body.payment,
              activeCheckoutUrl = body.payment.checkoutUrl,
              activeVerifyIdempKey = UUID.randomUUID().toString(),
              status = body.status,
            )
          }
          return@launch
        }
      }
      val msg = parseErrorMessage(resp?.errorBody()?.string(), resp?.code() ?: 0)
      _state.update { it.copy(isInitiatingCheckout = false, errorMessage = msg) }
    }
  }

  fun dismissCheckoutWebView(verifyIfPending: Boolean = true) {
    val active = _state.value.activePayment
    _state.update { it.copy(activeCheckoutUrl = null) }
    if (verifyIfPending && active != null && active.status == "pending") {
      verifyActivePayment(
        paymentId = active.paymentId,
        reference = active.providerReference,
        paypalOrderId = active.providerOrderId,
      )
    }
  }

  fun handlePaymentReturnDeepLink(
    paymentId: String = "",
    reference: String = "",
    paypalOrderId: String = "",
    status: String = "",
  ) {
    _state.update { it.copy(activeCheckoutUrl = null) }
    if (status.equals("cancelled", ignoreCase = true)) {
      _state.update { it.copy(statusMessage = "Subscription checkout cancelled") }
      return
    }
    verifyActivePayment(
      paymentId = paymentId.ifBlank { _state.value.activePayment?.paymentId.orEmpty() },
      reference = reference.ifBlank { _state.value.activePayment?.providerReference.orEmpty() },
      paypalOrderId = paypalOrderId.ifBlank { _state.value.activePayment?.providerOrderId.orEmpty() },
    )
  }

  fun verifyActivePayment(
    paymentId: String,
    reference: String = "",
    paypalOrderId: String = "",
  ) {
    if (paymentId.isBlank() && reference.isBlank() && paypalOrderId.isBlank()) return
    val api = repository.getApiServiceOrNull() ?: return
    val snap = _state.value
    if (snap.isVerifyingPayment) return
    val verifyIdemp = snap.activeVerifyIdempKey ?: UUID.randomUUID().toString()

    _state.update { it.copy(isVerifyingPayment = true, activeCheckoutUrl = null, errorMessage = null) }

    scope.launch(Dispatchers.IO) {
      val token = repository.sessionFlow.first().accessToken.orEmpty()
      if (token.isBlank()) {
        _state.update { it.copy(isVerifyingPayment = false) }
        return@launch
      }
      val fp = repository.getDeviceFingerprint()
      val resp = runCatching {
        api.verifyVerificationCheckout(
          "Bearer $token",
          fp,
          verifyIdemp,
          VerificationCheckoutVerifyRequestDto(
            paymentId = paymentId,
            reference = reference,
            paypalOrderId = paypalOrderId,
            idempotencyKey = verifyIdemp,
          ),
        )
      }.getOrNull()

      if (resp?.isSuccessful == true) {
        val body = resp.body()
        if (body != null && body.success) {
          lastCheckoutSig = ""
          lastCheckoutIdempKey = ""
          _state.update { s ->
            s.copy(
              isVerifyingPayment = false,
              activePayment = body.payment,
              status = body.status,
              statusMessage = "Subscription confirmed. Complete your live selfie check.",
              routeStack = (s.routeStack + VerificationRoute.SELFIE_GUIDANCE).distinctLastDuplicate(),
            )
          }
          return@launch
        }
      }
      val msg = parseErrorMessage(resp?.errorBody()?.string(), resp?.code() ?: 0)
      _state.update { it.copy(isVerifyingPayment = false, errorMessage = msg) }
    }
  }

  fun refreshStatus(silent: Boolean = false) {
    val api = repository.getApiServiceOrNull() ?: return
    if (!silent) {
      _state.update { it.copy(isLoadingStatus = true, errorMessage = null) }
    }
    scope.launch(Dispatchers.IO) {
      val token = repository.sessionFlow.first().accessToken.orEmpty()
      if (token.isBlank()) {
        if (!silent) _state.update { it.copy(isLoadingStatus = false) }
        return@launch
      }
      val fp = repository.getDeviceFingerprint()
      val country = _state.value.countryIso
      val resp = runCatching {
        api.getVerificationStatus("Bearer $token", fp, country)
      }.getOrNull()
      if (resp?.isSuccessful == true) {
        val body = resp.body()
        if (body != null && body.success) {
          _state.update { s ->
            val autoRoute = when {
              (body.reviewStatus == "under_review" || body.reviewStatus == "verified") &&
                s.currentRoute == VerificationRoute.TIERS_PAGER -> listOf(VerificationRoute.STATUS_REVIEW)
              else -> s.routeStack
            }
            s.copy(
              isLoadingStatus = false,
              status = body,
              countryIso = body.countryIso.ifBlank { s.countryIso },
              routeStack = autoRoute,
            )
          }
          return@launch
        }
      }
      if (!silent) {
        _state.update { it.copy(isLoadingStatus = false) }
      }
    }
  }

  fun openTiersExplorer() {
    _state.update { s ->
      s.copy(routeStack = listOf(VerificationRoute.TIERS_PAGER), errorMessage = null)
    }
  }

  fun navigateBack() {
    _state.update { s ->
      if (s.routeStack.size <= 1) {
        s.copy(
          isOpen = false,
          routeStack = listOf(VerificationRoute.TIERS_PAGER),
          activeCheckoutUrl = null,
          errorMessage = null,
        )
      } else {
        s.copy(
          routeStack = s.routeStack.dropLast(1),
          errorMessage = null,
        )
      }
    }
  }

  fun closeAll() {
    _state.update {
      it.copy(
        isOpen = false,
        routeStack = listOf(VerificationRoute.TIERS_PAGER),
        activeCheckoutUrl = null,
        errorMessage = null,
      )
    }
  }

  fun dismissStatusMessage() {
    _state.update { it.copy(statusMessage = null, errorMessage = null) }
  }

  fun dispose() {
    runCatching { displayManager?.unregisterDisplayListener(displayListener) }
  }

  private fun parseErrorMessage(raw: String?, code: Int): String {
    if (!raw.isNullOrBlank()) {
      val parsed = runCatching { JSONObject(raw).optString("message") }.getOrNull()
      if (!parsed.isNullOrBlank()) return parsed
    }
    return when (code) {
      401 -> "Session expired. Please sign in again."
      402 -> "Activate the one-week free trial or subscribe first."
      403 -> "Screen recording or mirroring is blocked during verification."
      409 -> "Your one-week free trial was already used. Subscribe with Paystack or PayPal."
      410 -> "Verification session expired. Please start again."
      429 -> "Too many verification attempts. Please wait a few minutes."
      503 -> "Payment gateway is not configured on this server."
      else -> "Verification request could not be completed. Please try again."
    }
  }

  companion object {
    private const val KEY_TUTORIAL_SEEN = "ver_first_open_tutorial_seen"

    private fun <T> List<T>.distinctLastDuplicate(): List<T> {
      if (size < 2) return this
      return if (this[size - 1] == this[size - 2]) dropLast(1) else this
    }
  }
}
