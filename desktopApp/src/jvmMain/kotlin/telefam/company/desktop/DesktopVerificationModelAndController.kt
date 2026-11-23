package telefam.company.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.NumberFormat
import java.util.Base64
import java.util.Locale
import java.util.UUID
import java.util.prefs.Preferences
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

enum class DesktopVerificationTier(
  val wire: String,
  val headerTitle: String,
  val headerSubtitle: String,
  val baseMonthlyKes: Long,
) {
  BUSINESS("business", "Business Verification", "Get the Business Badge", 1_200L),
  GOVERNMENT("government", "Government Verification", "Get the Government Badge", 700L),
  BLUE("blue", "Blue Verification", "Get the Blue Badge", 700L);

  companion object {
    fun fromWire(value: String): DesktopVerificationTier = when (value.trim().lowercase(Locale.US)) {
      "business" -> BUSINESS
      "government" -> GOVERNMENT
      else -> BLUE
    }
  }
}

enum class DesktopVerificationBillingCycle(val wire: String) {
  MONTHLY("monthly"),
  ANNUAL("annual"),
}

enum class DesktopVerificationProvider(val wire: String, val label: String) {
  PAYSTACK("paystack", "Paystack"),
  PAYPAL("paypal", "PayPal"),
}

enum class DesktopVerificationDocumentType(
  val wire: String,
  val label: String,
  val subtitle: String,
) {
  NATIONAL_ID("national_id", "National ID", "Government-issued national identity card"),
  PASSPORT("passport", "Passport", "International biometric passport photo page"),
  DRIVERS_LICENSE("drivers_license", "Driver's License", "Valid government driver's license"),
  MILITARY_ID("military_id", "Military ID", "Official armed forces identification card"),
  BUSINESS_LICENSE("business_license", "Business Registration", "Certificate of incorporation or business permit"),
  GOV_WARRANT("gov_warrant", "Government Credential", "Official ministry or agency warrant / commission"),
}

enum class DesktopVerificationRoute {
  TIERS_PAGER,
  TRIAL_AND_PAYMENT,
  SELFIE_GUIDANCE,
  SELFIE_LIVENESS,
  ID_GUIDANCE_AND_SELECT,
  ID_LIVE_CAPTURE,
  STATUS_REVIEW,
}

data class DesktopVerificationBenefit(
  val iconKey: String,
  val title: String,
  val description: String,
)

data class DesktopIdGuidanceExample(
  val key: String,
  val label: String,
)

data class DesktopTierPricing(
  val tier: String = "",
  val title: String = "",
  val badgeTitle: String = "",
  val currencyCode: String = "USD",
  val currencySymbol: String = "$",
  val monthlyDisplay: String = "",
  val annualDisplay: String = "",
  val annualOriginalDisplay: String = "",
  val discountPercent: Int = 20,
)

data class DesktopVerificationStatus(
  val verifiedTier: String = "none",
  val pendingTier: String = "none",
  val billingCycle: String = "monthly",
  val planStatus: String = "none",
  val trialEligible: Boolean = true,
  val trialEndsAtMs: Long = 0L,
  val subscriptionEndsAtMs: Long = 0L,
  val livenessStatus: String = "not_started",
  val livenessScore: Int = 0,
  val documentType: String = "",
  val documentStatus: String = "not_started",
  val docLast4: String = "",
  val docExpiryIso: String = "",
  val extractedFullName: String = "",
  val mergedToProfile: Boolean = false,
  val reviewStatus: String = "not_started",
  val reviewEtaWorkingDays: Int = 3,
  val reviewSubmittedAtMs: Long = 0L,
  val reviewDueAtMs: Long = 0L,
  val verifiedAtMs: Long = 0L,
  val documentsPurged: Boolean = false,
  val documentsPurgedAtMs: Long = 0L,
  val rejectionReason: String = "",
  val countryIso: String = "US",
  val tiers: List<DesktopTierPricing> = emptyList(),
)

data class DesktopVerificationPayment(
  val paymentId: String = "",
  val provider: String = "",
  val providerReference: String = "",
  val providerOrderId: String = "",
  val status: String = "",
  val checkoutUrl: String = "",
)

data class DesktopLivenessTelemetryStep(
  val step: String,
  val yaw: Double,
  val pitch: Double,
  val leftEyeOpen: Double,
  val rightEyeOpen: Double,
  val luma: Double,
  val capturedAtMs: Long,
)

data class DesktopVerificationUiState(
  val isOpen: Boolean = false,
  val routeStack: List<DesktopVerificationRoute> = listOf(DesktopVerificationRoute.TIERS_PAGER),
  val showFirstOpenTutorial: Boolean = false,
  val selectedTier: DesktopVerificationTier = DesktopVerificationTier.BUSINESS,
  val billingCycle: DesktopVerificationBillingCycle = DesktopVerificationBillingCycle.MONTHLY,
  val selectedProvider: DesktopVerificationProvider = DesktopVerificationProvider.PAYSTACK,
  val selectedDocumentType: DesktopVerificationDocumentType = DesktopVerificationDocumentType.NATIONAL_ID,
  val countryIso: String = "US",
  val profileFullName: String = "",
  val profileUsername: String = "",
  val status: DesktopVerificationStatus = DesktopVerificationStatus(),
  val activePayment: DesktopVerificationPayment? = null,
  val awaitingBrowserReturn: Boolean = false,
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
  val currentRoute: DesktopVerificationRoute
    get() = routeStack.lastOrNull() ?: DesktopVerificationRoute.TIERS_PAGER
}

object DesktopVerificationCatalog {
  private data class CurrencyRate(val code: String, val symbol: String, val rateFromKes: Double, val zeroDecimal: Boolean)

  private val rates: Map<String, CurrencyRate> = mapOf(
    "KE" to CurrencyRate("KES", "Ksh ", 1.0, true),
    "NG" to CurrencyRate("NGN", "NGN ", 12.0, true),
    "GH" to CurrencyRate("GHS", "GHS ", 0.12, false),
    "ZA" to CurrencyRate("ZAR", "R ", 0.15, false),
    "UG" to CurrencyRate("UGX", "UGX ", 30.0, true),
    "TZ" to CurrencyRate("TZS", "TZS ", 20.0, true),
    "RW" to CurrencyRate("RWF", "RWF ", 10.0, true),
    "GB" to CurrencyRate("GBP", "\u00A3", 0.0064, false),
    "DE" to CurrencyRate("EUR", "\u20AC", 0.0074, false),
    "FR" to CurrencyRate("EUR", "\u20AC", 0.0074, false),
    "ES" to CurrencyRate("EUR", "\u20AC", 0.0074, false),
    "IT" to CurrencyRate("EUR", "\u20AC", 0.0074, false),
    "NL" to CurrencyRate("EUR", "\u20AC", 0.0074, false),
    "CA" to CurrencyRate("CAD", "CA$", 0.011, false),
    "AU" to CurrencyRate("AUD", "A$", 0.012, false),
    "IN" to CurrencyRate("INR", "\u20B9", 0.68, false),
    "US" to CurrencyRate("USD", "$", 0.008325, false),
  )

  fun pricingForTier(
    tier: DesktopVerificationTier,
    countryIso: String,
    serverTiers: List<DesktopTierPricing> = emptyList(),
  ): DesktopTierPricing {
    serverTiers.firstOrNull { it.tier.equals(tier.wire, ignoreCase = true) }?.let { dto ->
      if (dto.monthlyDisplay.isNotBlank() && dto.annualDisplay.isNotBlank()) return dto
    }
    val spec = rates[countryIso.trim().uppercase(Locale.US)] ?: rates.getValue("US")
    val monthlyKes = tier.baseMonthlyKes.toDouble()
    val fullAnnualKes = monthlyKes * 12.0
    val discountedAnnualKes = fullAnnualKes * 0.80

    fun fmt(kes: Double): String {
      val v = kes * spec.rateFromKes
      return if (spec.zeroDecimal) {
        "${spec.symbol}${NumberFormat.getIntegerInstance(Locale.US).format(v.roundToLong())}"
      } else {
        String.format(Locale.US, "%s%.2f", spec.symbol, v)
      }
    }

    return DesktopTierPricing(
      tier = tier.wire,
      title = tier.headerTitle,
      badgeTitle = tier.headerSubtitle,
      currencyCode = spec.code,
      currencySymbol = spec.symbol,
      monthlyDisplay = fmt(monthlyKes),
      annualDisplay = fmt(discountedAnnualKes),
      annualOriginalDisplay = fmt(fullAnnualKes),
      discountPercent = 20,
    )
  }

  val businessBenefits = listOf(
    DesktopVerificationBenefit("badge", "Business Badge", "Show your verified business badge next to your name."),
    DesktopVerificationBenefit("link", "Custom Links", "Add your website, store, and social media links."),
    DesktopVerificationBenefit("group", "Business Profile", "Get a professional business profile with custom details, links and media."),
    DesktopVerificationBenefit("verified_doc", "Verified Contact Info", "Display your business phone, email and location."),
    DesktopVerificationBenefit("support", "Priority Support", "Get faster, dedicated support for your business needs."),
    DesktopVerificationBenefit("media", "Featured Media", "Showcase your products, services and latest updates with media."),
    DesktopVerificationBenefit("security", "Enhanced Security", "Advanced protection against impersonation, fraud and fake accounts."),
    DesktopVerificationBenefit("messages", "Business Messages", "Send automated replies and quick responses."),
    DesktopVerificationBenefit("analytics", "Advanced Analytics", "Track profile visits, engagement and audience insights."),
    DesktopVerificationBenefit("group", "Group Verification", "Create and manage verified business groups and communities."),
    DesktopVerificationBenefit("payments", "Payment Integration", "Accept payments directly through the app (M-Pesa, PayPal, etc)."),
    DesktopVerificationBenefit("star", "Exclusive Features", "Get early access to new features, promotions and premium tools."),
  )

  val governmentBenefits = listOf(
    DesktopVerificationBenefit("badge", "Official Government Badge", "Show your verified government status with a red shield badge."),
    DesktopVerificationBenefit("verified_doc", "Verified Contact Info", "Display your official phone, email and location."),
    DesktopVerificationBenefit("security", "Enhanced Security", "Additional protection against impersonation and fraud."),
    DesktopVerificationBenefit("media", "Featured Media", "Showcase your department, initiatives and latest updates."),
    DesktopVerificationBenefit("support", "Priority Support", "Get faster assistance from our support team."),
    DesktopVerificationBenefit("group", "Group Verification", "Create and manage verified official groups and communities."),
    DesktopVerificationBenefit("analytics", "Advanced Analytics", "Access detailed insights on your account performance and reach."),
    DesktopVerificationBenefit("star", "Exclusive Features", "Early access to new tools, reports and beta features."),
    DesktopVerificationBenefit("payments", "Payment Integration", "Accept payments directly through the app (M-Pesa, PayPal, etc)."),
  )

  val blueBenefits = listOf(
    DesktopVerificationBenefit("group", "Group Verification", "Create and manage verified groups with a blue badge."),
    DesktopVerificationBenefit("stickers", "New Stickers", "Access exclusive stickers and animated reactions."),
    DesktopVerificationBenefit("security", "Enhanced Protection", "Priority support, stronger security and fraud protection."),
    DesktopVerificationBenefit("badge", "Badge Next to Your Name", "Show your verified status with a blue badge."),
    DesktopVerificationBenefit("calls", "HD Calls", "Enjoy crystal clear audio and video calls."),
    DesktopVerificationBenefit("messages", "Send Paid Messages", "Charge for your messages and earn from your content."),
  )

  val correctIdRules = listOf(
    "All corners visible",
    "Not blurry",
    "No glare / reflection",
    "All details readable",
  )

  val incorrectIdExamples = listOf(
    DesktopIdGuidanceExample("blurry", "Too blurry"),
    DesktopIdGuidanceExample("glare", "Glare / Reflection"),
    DesktopIdGuidanceExample("corners", "Not all corners visible"),
    DesktopIdGuidanceExample("aligned", "Not aligned"),
    DesktopIdGuidanceExample("too_far", "Too far"),
    DesktopIdGuidanceExample("too_close", "Too close"),
    DesktopIdGuidanceExample("too_dark", "Too dark"),
    DesktopIdGuidanceExample("obstructed", "Obstructed"),
  )

  fun documentOptionsForTier(tier: DesktopVerificationTier): List<DesktopVerificationDocumentType> = when (tier) {
    DesktopVerificationTier.BUSINESS -> listOf(
      DesktopVerificationDocumentType.NATIONAL_ID,
      DesktopVerificationDocumentType.PASSPORT,
      DesktopVerificationDocumentType.DRIVERS_LICENSE,
      DesktopVerificationDocumentType.MILITARY_ID,
      DesktopVerificationDocumentType.BUSINESS_LICENSE,
    )
    DesktopVerificationTier.GOVERNMENT -> listOf(
      DesktopVerificationDocumentType.GOV_WARRANT,
      DesktopVerificationDocumentType.MILITARY_ID,
      DesktopVerificationDocumentType.NATIONAL_ID,
      DesktopVerificationDocumentType.PASSPORT,
      DesktopVerificationDocumentType.DRIVERS_LICENSE,
    )
    DesktopVerificationTier.BLUE -> listOf(
      DesktopVerificationDocumentType.NATIONAL_ID,
      DesktopVerificationDocumentType.PASSPORT,
      DesktopVerificationDocumentType.DRIVERS_LICENSE,
      DesktopVerificationDocumentType.MILITARY_ID,
    )
  }

  fun stepDirectionLabel(step: String): String = when (step.trim().lowercase(Locale.US)) {
    "left" -> "Turn head slowly LEFT"
    "right" -> "Turn head slowly RIGHT"
    "up" -> "Tilt head slightly UP"
    else -> "Look straight at camera (CENTER)"
  }
}

object DesktopVerificationFrameAnalyzer {
  data class DocumentMetrics(
    val jpegBytes: ByteArray,
    val luminanceMean: Double,
    val sharpnessScore: Double,
    val glareRatio: Double,
    val allCornersDetected: Boolean,
    val readinessPct: Int,
  )

  private val recorderProcessTokens = listOf(
    "obs", "obs64", "wf-recorder", "wl-screenrec", "simplescreenrecorder",
    "kazam", "recordmydesktop", "screencapture", "vokoscreen",
  )

  fun detectActiveScreenRecorderProcess(): Boolean {
    return runCatching {
      ProcessHandle.allProcesses().anyMatch { ph ->
        val cmd = ph.info().command().orElse("").lowercase(Locale.US)
        val args = ph.info().arguments().orElse(emptyArray()).joinToString(" ").lowercase(Locale.US)
        if (recorderProcessTokens.any { tok -> cmd.endsWith(tok) || cmd.contains("/$tok") || cmd.contains("\\$tok") }) {
          return@anyMatch true
        }
        cmd.contains("ffmpeg") && (args.contains("x11grab") || args.contains("gdigrab") || args.contains("avfoundation"))
      }
    }.getOrDefault(false)
  }

  fun openWebcamMjpegProcess(): Process? {
    val os = System.getProperty("os.name", "").lowercase(Locale.US)
    val cmd = when {
      os.contains("linux") -> listOf("ffmpeg", "-hide_banner", "-loglevel", "error", "-f", "v4l2", "-video_size", "640x480", "-i", "/dev/video0", "-f", "mpjpeg", "-q:v", "6", "pipe:1")
      os.contains("mac") -> listOf("ffmpeg", "-hide_banner", "-loglevel", "error", "-f", "avfoundation", "-framerate", "24", "-video_size", "640x480", "-i", "0", "-f", "mpjpeg", "-q:v", "6", "pipe:1")
      os.contains("win") -> listOf("ffmpeg", "-hide_banner", "-loglevel", "error", "-f", "dshow", "-video_size", "640x480", "-i", "video=Integrated Camera", "-f", "mpjpeg", "-q:v", "6", "pipe:1")
      else -> return null
    }
    return runCatching { ProcessBuilder(cmd).redirectErrorStream(false).start() }.getOrNull()
  }

  fun analyzeFaceRegion(img: BufferedImage): Triple<Double, Double, Double> {
    // Returns (meanLuminance, normalizedHorizontalCentroid [-1..1], normalizedVerticalCentroid [-1..1])
    val w = img.width.coerceAtLeast(1)
    val h = img.height.coerceAtLeast(1)
    var sumLuma = 0.0
    var count = 0
    var skinWeightSum = 0.0
    var weightedX = 0.0
    var weightedY = 0.0
    val stepX = max(1, w / 48)
    val stepY = max(1, h / 36)
    for (y in 0 until h step stepY) {
      for (x in 0 until w step stepX) {
        val rgb = img.getRGB(x, y)
        val r = (rgb ushr 16) and 0xFF
        val g = (rgb ushr 8) and 0xFF
        val b = rgb and 0xFF
        val luma = 0.299 * r + 0.587 * g + 0.114 * b
        sumLuma += luma
        count++
        if (r > 55 && g > 35 && b > 20 && r >= g && g >= b && (r - b) >= 12) {
          val nx = (x.toDouble() / w) * 2.0 - 1.0
          val ny = (y.toDouble() / h) * 2.0 - 1.0
          weightedX += nx
          weightedY += ny
          skinWeightSum += 1.0
        }
      }
    }
    val meanLuma = if (count > 0) sumLuma / count else 0.0
    val cx = if (skinWeightSum > 25.0) (weightedX / skinWeightSum) else 0.0
    val cy = if (skinWeightSum > 25.0) (weightedY / skinWeightSum) else 0.0
    return Triple(meanLuma, cx, cy)
  }

  fun analyzeDocumentImage(img: BufferedImage): DocumentMetrics {
    val w = img.width.coerceAtLeast(1)
    val h = img.height.coerceAtLeast(1)
    var sumLuma = 0.0
    var brightPixels = 0
    var edgeAcc = 0.0
    var count = 0
    var cornerBorderAcc = 0.0
    var cornerSamples = 0
    val stepX = max(1, w / 80)
    val stepY = max(1, h / 60)

    fun lumaAt(x: Int, y: Int): Double {
      val rgb = img.getRGB(x.coerceIn(0, w - 1), y.coerceIn(0, h - 1))
      val r = (rgb ushr 16) and 0xFF
      val g = (rgb ushr 8) and 0xFF
      val b = rgb and 0xFF
      return 0.299 * r + 0.587 * g + 0.114 * b
    }

    for (y in stepY until (h - stepY) step stepY) {
      for (x in stepX until (w - stepX) step stepX) {
        val c = lumaAt(x, y)
        sumLuma += c
        if (c >= 248.0) brightPixels++
        val grad = abs(c - lumaAt(x + stepX, y)) + abs(c - lumaAt(x, y + stepY))
        edgeAcc += grad
        count++
        val inMarginBand = (x < w * 0.18 || x > w * 0.82) && (y < h * 0.22 || y > h * 0.78)
        if (inMarginBand) {
          cornerBorderAcc += grad
          cornerSamples++
        }
      }
    }
    val luma = if (count > 0) sumLuma / count else 0.0
    val glare = if (count > 0) brightPixels.toDouble() / count else 0.0
    val sharp = if (count > 0) ((edgeAcc / count) / 36.0).coerceIn(0.0, 1.0) else 0.0
    val cornersOk = w >= 480 && h >= 300 && (if (cornerSamples > 0) cornerBorderAcc / cornerSamples else 0.0) >= 4.0
    val lightOk = luma >= 40.0
    val glareOk = glare <= 0.35
    val sharpOk = sharp >= 0.35
    var pct = 0
    if (cornersOk) pct += 30
    if (sharpOk) pct += 25
    if (lightOk) pct += 25
    if (glareOk) pct += 20

    val outImg = if (w < 640 || h < 400) {
      val sw = max(640, w)
      val sh = max(400, h)
      BufferedImage(sw, sh, BufferedImage.TYPE_INT_RGB).apply {
        val g2 = createGraphics()
        g2.drawImage(img, 0, 0, sw, sh, null)
        g2.dispose()
      }
    } else {
      BufferedImage(w, h, BufferedImage.TYPE_INT_RGB).apply {
        val g2 = createGraphics()
        g2.drawImage(img, 0, 0, null)
        g2.dispose()
      }
    }
    val baos = ByteArrayOutputStream()
    val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
    val param = writer.defaultWriteParam.apply {
      compressionMode = ImageWriteParam.MODE_EXPLICIT
      compressionQuality = 0.88f
    }
    ImageIO.createImageOutputStream(baos).use { ios ->
      writer.output = ios
      writer.write(null, IIOImage(outImg, null, null), param)
    }
    writer.dispose()

    return DocumentMetrics(
      jpegBytes = baos.toByteArray(),
      luminanceMean = luma,
      sharpnessScore = sharp,
      glareRatio = glare,
      allCornersDetected = cornersOk,
      readinessPct = pct.coerceIn(0, 100),
    )
  }

  fun encodeRgbJpeg(img: BufferedImage): ByteArray {
    val rgbImg = BufferedImage(max(320, img.width), max(320, img.height), BufferedImage.TYPE_INT_RGB)
    val g2 = rgbImg.createGraphics()
    g2.drawImage(img, 0, 0, rgbImg.width, rgbImg.height, null)
    g2.dispose()
    val baos = ByteArrayOutputStream()
    ImageIO.write(rgbImg, "jpg", baos)
    return baos.toByteArray()
  }
}

class DesktopVerificationController(
  private val apiClient: DesktopApiClient,
  private val chatController: DesktopChatController,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val prefs: Preferences = Preferences.userNodeForPackage(DesktopVerificationController::class.java)
  private val _state = MutableStateFlow(DesktopVerificationUiState())
  val state: StateFlow<DesktopVerificationUiState> = _state.asStateFlow()

  private var lastCheckoutSig: String = ""
  private var lastCheckoutIdempKey: String = ""
  private var trialIdempKey: String = ""

  init {
    scope.launch {
      chatController.wsEvents.collect { ev ->
        if (ev.event == "verification.updated") {
          val map = runCatching { DesktopJson.parseObject(ev.rawJson) }.getOrNull()
          if (map != null) {
            val tier = DesktopJson.str(map, "verified_tier")
            val rev = DesktopJson.str(map, "review_status")
            _state.update { s ->
              val updated = s.status.copy(
                verifiedTier = tier.ifBlank { s.status.verifiedTier },
                reviewStatus = rev.ifBlank { s.status.reviewStatus },
              )
              val msg = when {
                rev == "verified" && tier != "none" && tier.isNotBlank() ->
                  "Telefam ${DesktopVerificationTier.fromWire(tier).headerTitle} badge unlocked"
                rev == "under_review" ->
                  "Identity documents matched. Application under review (3 working days)."
                else -> s.statusMessage
              }
              s.copy(status = updated, statusMessage = msg)
            }
          }
          refreshStatus(silent = true)
        }
      }
    }
  }

  fun attachSession(userId: String) {
    if (userId.isNotBlank()) refreshStatus(silent = true)
  }

  fun detachSession() {
    _state.value = DesktopVerificationUiState()
  }

  fun openGetVerified(
    profileCountryIso: String = "",
    locationHint: String = "",
    fullName: String = "",
    username: String = "",
    initialVerifiedTier: String = "none",
  ) {
    val iso = if (profileCountryIso.trim().length == 2) {
      profileCountryIso.trim().uppercase(Locale.US)
    } else {
      DesktopStarsCatalog.resolveCountryIso(locationHint)
    }
    val seen = prefs.getBoolean("ver_first_open_tutorial_seen", false)
    _state.update { s ->
      val existingRev = s.status.reviewStatus
      val startRoute = when {
        initialVerifiedTier != "none" && initialVerifiedTier.isNotBlank() -> DesktopVerificationRoute.STATUS_REVIEW
        existingRev == "under_review" || existingRev == "verified" -> DesktopVerificationRoute.STATUS_REVIEW
        else -> DesktopVerificationRoute.TIERS_PAGER
      }
      s.copy(
        isOpen = true,
        routeStack = listOf(startRoute),
        showFirstOpenTutorial = !seen && startRoute == DesktopVerificationRoute.TIERS_PAGER,
        countryIso = iso,
        profileFullName = fullName.ifBlank { s.profileFullName },
        profileUsername = username.ifBlank { s.profileUsername },
        screenCaptureBlocked = DesktopVerificationFrameAnalyzer.detectActiveScreenRecorderProcess(),
        errorMessage = null,
      )
    }
    refreshStatus(silent = false)
  }

  fun dismissTutorial() {
    prefs.putBoolean("ver_first_open_tutorial_seen", true)
    _state.update { it.copy(showFirstOpenTutorial = false) }
  }

  fun reopenTutorial() {
    _state.update { it.copy(showFirstOpenTutorial = true) }
  }

  fun selectTier(tier: DesktopVerificationTier) {
    val doc = DesktopVerificationCatalog.documentOptionsForTier(tier).first()
    _state.update { it.copy(selectedTier = tier, selectedDocumentType = doc, errorMessage = null) }
  }

  fun toggleBillingCycle(annual: Boolean) {
    _state.update {
      it.copy(billingCycle = if (annual) DesktopVerificationBillingCycle.ANNUAL else DesktopVerificationBillingCycle.MONTHLY, errorMessage = null)
    }
  }

  fun selectProvider(provider: DesktopVerificationProvider) {
    _state.update { it.copy(selectedProvider = provider, errorMessage = null) }
  }

  fun selectDocType(docType: DesktopVerificationDocumentType) {
    _state.update { it.copy(selectedDocumentType = docType, errorMessage = null) }
  }

  fun openApplyOrSubscribe(tier: DesktopVerificationTier) {
    val st = _state.value.status
    val hasActivePlan = st.planStatus == "trial_active" || st.planStatus == "subscribed"
    val next = when {
      st.reviewStatus == "under_review" || st.reviewStatus == "verified" -> DesktopVerificationRoute.STATUS_REVIEW
      hasActivePlan && st.livenessStatus == "passed" -> DesktopVerificationRoute.ID_GUIDANCE_AND_SELECT
      hasActivePlan -> DesktopVerificationRoute.SELFIE_GUIDANCE
      else -> DesktopVerificationRoute.TRIAL_AND_PAYMENT
    }
    _state.update { s ->
      s.copy(selectedTier = tier, routeStack = (s.routeStack + next).distinctLast(), errorMessage = null)
    }
  }

  fun refreshStatus(silent: Boolean = false) {
    if (!silent) _state.update { it.copy(isLoadingStatus = true, errorMessage = null) }
    scope.launch {
      val encIso = URLEncoder.encode(_state.value.countryIso, StandardCharsets.UTF_8)
      val resp = apiClient.executeChatRaw("GET", "/v1/verification/status?country_iso=$encIso")
      if (resp != null && resp.first in 200..299) {
        val map = runCatching { DesktopJson.parseObject(resp.second) }.getOrNull()
        if (map != null && DesktopJson.bool(map, "success")) {
          val parsed = parseStatus(map)
          _state.update { s ->
            val route = if ((parsed.reviewStatus == "under_review" || parsed.reviewStatus == "verified") &&
              s.currentRoute == DesktopVerificationRoute.TIERS_PAGER
            ) listOf(DesktopVerificationRoute.STATUS_REVIEW) else s.routeStack
            s.copy(
              isLoadingStatus = false,
              status = parsed,
              countryIso = parsed.countryIso.ifBlank { s.countryIso },
              routeStack = route,
            )
          }
          return@launch
        }
      }
      if (!silent) _state.update { it.copy(isLoadingStatus = false) }
    }
  }

  fun activateFreeTrial() {
    val snap = _state.value
    if (snap.isActivatingTrial) return
    if (trialIdempKey.isBlank()) trialIdempKey = UUID.randomUUID().toString()
    _state.update { it.copy(isActivatingTrial = true, errorMessage = null) }
    scope.launch {
      val body = buildString {
        append('{')
        append("\"tier\":\"").append(snap.selectedTier.wire).append("\",")
        append("\"billing_cycle\":\"").append(snap.billingCycle.wire).append("\",")
        append("\"idempotency_key\":\"").append(DesktopJson.escape(trialIdempKey)).append('"')
        append('}')
      }
      val resp = apiClient.executeChatRaw(
        method = "POST",
        path = "/v1/verification/trial/activate",
        jsonBody = body,
        extraHeaders = mapOf("Idempotency-Key" to trialIdempKey),
      )
      if (resp != null && resp.first in 200..299) {
        val map = runCatching { DesktopJson.parseObject(resp.second) }.getOrNull()
        if (map != null && DesktopJson.bool(map, "success")) {
          trialIdempKey = ""
          val st = parseStatus(map)
          _state.update { s ->
            s.copy(
              isActivatingTrial = false,
              status = st,
              statusMessage = "First week free trial activated. Complete live selfie next.",
              routeStack = (s.routeStack + DesktopVerificationRoute.SELFIE_GUIDANCE).distinctLast(),
            )
          }
          return@launch
        }
      }
      _state.update { it.copy(isActivatingTrial = false, errorMessage = extractErr(resp)) }
    }
  }

  fun proceedToSubscribePayment() {
    val snap = _state.value
    if (snap.isInitiatingCheckout) return
    val sig = "${snap.selectedProvider.wire}:${snap.selectedTier.wire}:${snap.billingCycle.wire}:${snap.countryIso}"
    val key = if (sig == lastCheckoutSig && lastCheckoutIdempKey.isNotBlank()) {
      lastCheckoutIdempKey
    } else {
      UUID.randomUUID().toString().also { lastCheckoutSig = sig; lastCheckoutIdempKey = it }
    }
    _state.update { it.copy(isInitiatingCheckout = true, errorMessage = null) }
    scope.launch {
      val body = buildString {
        append('{')
        append("\"tier\":\"").append(snap.selectedTier.wire).append("\",")
        append("\"billing_cycle\":\"").append(snap.billingCycle.wire).append("\",")
        append("\"provider\":\"").append(snap.selectedProvider.wire).append("\",")
        append("\"country_iso\":\"").append(DesktopJson.escape(snap.countryIso)).append("\",")
        append("\"idempotency_key\":\"").append(DesktopJson.escape(key)).append('"')
        append('}')
      }
      val resp = apiClient.executeChatRaw(
        method = "POST",
        path = "/v1/verification/checkout/init",
        jsonBody = body,
        extraHeaders = mapOf("Idempotency-Key" to key),
      )
      if (resp != null && resp.first in 200..299) {
        val root = runCatching { DesktopJson.parseObject(resp.second) }.getOrNull()
        val payObj = root?.let { DesktopJson.objOrNull(it, "payment") }
        val stObj = root?.let { DesktopJson.objOrNull(it, "status") }
        val pay = payObj?.let(::parsePayment)
        if (root != null && DesktopJson.bool(root, "success") && pay != null && pay.checkoutUrl.isNotBlank()) {
          openInBrowser(pay.checkoutUrl)
          _state.update { s ->
            s.copy(
              isInitiatingCheckout = false,
              activePayment = pay,
              awaitingBrowserReturn = true,
              activeVerifyIdempKey = UUID.randomUUID().toString(),
              status = stObj?.let(::parseStatus) ?: s.status,
              statusMessage = "Complete checkout in your browser, then click Verify Payment.",
            )
          }
          return@launch
        }
      }
      _state.update { it.copy(isInitiatingCheckout = false, errorMessage = extractErr(resp)) }
    }
  }

  fun verifyActivePayment() {
    val snap = _state.value
    val pay = snap.activePayment ?: return
    if (snap.isVerifyingPayment) return
    val key = snap.activeVerifyIdempKey ?: UUID.randomUUID().toString()
    _state.update { it.copy(isVerifyingPayment = true, errorMessage = null) }
    scope.launch {
      val body = buildString {
        append('{')
        append("\"payment_id\":\"").append(DesktopJson.escape(pay.paymentId)).append("\",")
        append("\"reference\":\"").append(DesktopJson.escape(pay.providerReference)).append("\",")
        append("\"paypal_order_id\":\"").append(DesktopJson.escape(pay.providerOrderId)).append("\",")
        append("\"idempotency_key\":\"").append(DesktopJson.escape(key)).append('"')
        append('}')
      }
      val resp = apiClient.executeChatRaw(
        method = "POST",
        path = "/v1/verification/checkout/verify",
        jsonBody = body,
        extraHeaders = mapOf("Idempotency-Key" to key),
      )
      if (resp != null && resp.first in 200..299) {
        val root = runCatching { DesktopJson.parseObject(resp.second) }.getOrNull()
        val stObj = root?.let { DesktopJson.objOrNull(it, "status") }
        if (root != null && DesktopJson.bool(root, "success") && stObj != null) {
          lastCheckoutSig = ""
          lastCheckoutIdempKey = ""
          _state.update { s ->
            s.copy(
              isVerifyingPayment = false,
              awaitingBrowserReturn = false,
              status = parseStatus(stObj),
              statusMessage = "Subscription verified. Complete your live selfie next.",
              routeStack = (s.routeStack + DesktopVerificationRoute.SELFIE_GUIDANCE).distinctLast(),
            )
          }
          return@launch
        }
      }
      _state.update { it.copy(isVerifyingPayment = false, errorMessage = extractErr(resp)) }
    }
  }

  fun proceedFromSelfieGuidanceToLiveCamera() {
    val blocked = DesktopVerificationFrameAnalyzer.detectActiveScreenRecorderProcess()
    _state.update { it.copy(screenCaptureBlocked = blocked) }
    if (blocked) {
      _state.update { it.copy(errorMessage = "Screen recorder detected. Close recording software before verification.") }
      return
    }
    val snap = _state.value
    _state.update { it.copy(isSubmittingLiveness = true, errorMessage = null) }
    scope.launch {
      val body = "{\"tier\":\"${snap.selectedTier.wire}\",\"screen_capture_active\":$blocked}"
      val resp = apiClient.executeChatRaw("POST", "/v1/verification/liveness/session", body)
      if (resp != null && resp.first in 200..299) {
        val map = runCatching { DesktopJson.parseObject(resp.second) }.getOrNull()
        if (map != null && DesktopJson.bool(map, "success")) {
          val seq = DesktopJson.stringList(map, "sequence").ifEmpty {
            listOf("left", "right", "up", "left", "right", "center")
          }
          _state.update { s ->
            s.copy(
              isSubmittingLiveness = false,
              livenessChallengeId = DesktopJson.str(map, "challenge_id"),
              livenessChallengeToken = DesktopJson.str(map, "challenge_token"),
              livenessSequence = seq,
              livenessExpiresAtMs = DesktopJson.long(map, "expires_at_ms", 0L),
              currentLivenessStepIndex = 0,
              currentLivenessScore = 0,
              liveMovementPrompt = DesktopVerificationCatalog.stepDirectionLabel(seq.first()),
              routeStack = (s.routeStack + DesktopVerificationRoute.SELFIE_LIVENESS).distinctLast(),
            )
          }
          return@launch
        }
      }
      _state.update { it.copy(isSubmittingLiveness = false, errorMessage = extractErr(resp)) }
    }
  }

  fun updateLiveSelfieProgress(stepIdx: Int, scorePct: Int, faceVisible: Boolean, lightOk: Boolean, prompt: String) {
    _state.update {
      it.copy(
        currentLivenessStepIndex = stepIdx,
        currentLivenessScore = scorePct.coerceIn(0, 100),
        liveFaceVisible = faceVisible,
        liveLightOk = lightOk,
        liveMovementPrompt = prompt,
      )
    }
  }

  fun submitAutoCapturedLiveness(jpegBytes: ByteArray, steps: List<DesktopLivenessTelemetryStep>) {
    val snap = _state.value
    if (snap.isSubmittingLiveness) return
    val blocked = DesktopVerificationFrameAnalyzer.detectActiveScreenRecorderProcess()
    if (blocked) {
      _state.update { it.copy(screenCaptureBlocked = true, errorMessage = "Screen recording detected.") }
      return
    }
    _state.update { it.copy(isSubmittingLiveness = true, currentLivenessScore = 100, errorMessage = null) }
    val b64 = Base64.getEncoder().encodeToString(jpegBytes)
    scope.launch {
      val stepsJson = steps.joinToString(",") { st ->
        buildString {
          append('{')
          append("\"step\":\"").append(DesktopJson.escape(st.step)).append("\",")
          append("\"yaw\":").append(st.yaw).append(',')
          append("\"pitch\":").append(st.pitch).append(',')
          append("\"left_eye_open\":").append(st.leftEyeOpen).append(',')
          append("\"right_eye_open\":").append(st.rightEyeOpen).append(',')
          append("\"luma\":").append(st.luma).append(',')
          append("\"captured_at_ms\":").append(st.capturedAtMs)
          append('}')
        }
      }
      val body = buildString {
        append('{')
        append("\"challenge_id\":\"").append(DesktopJson.escape(snap.livenessChallengeId)).append("\",")
        append("\"challenge_token\":\"").append(DesktopJson.escape(snap.livenessChallengeToken)).append("\",")
        append("\"screen_capture_active\":false,")
        append("\"selfie_image_b64\":\"").append(b64).append("\",")
        append("\"steps\":[").append(stepsJson).append(']')
        append('}')
      }
      val resp = apiClient.executeChatRaw("POST", "/v1/verification/liveness/submit", body)
      if (resp != null && resp.first in 200..299) {
        val map = runCatching { DesktopJson.parseObject(resp.second) }.getOrNull()
        val stObj = map?.let { DesktopJson.objOrNull(it, "status") }
        if (map != null && DesktopJson.bool(map, "success") && DesktopJson.bool(map, "passed")) {
          _state.update { s ->
            s.copy(
              isSubmittingLiveness = false,
              status = stObj?.let(::parseStatus) ?: s.status,
              statusMessage = DesktopJson.str(map, "message").ifBlank { "Live selfie verified" },
              routeStack = (s.routeStack + DesktopVerificationRoute.ID_GUIDANCE_AND_SELECT).distinctLast(),
            )
          }
          return@launch
        }
      }
      _state.update {
        it.copy(isSubmittingLiveness = false, currentLivenessStepIndex = 0, currentLivenessScore = 0, errorMessage = extractErr(resp))
      }
    }
  }

  fun startLiveIdCapture() {
    val blocked = DesktopVerificationFrameAnalyzer.detectActiveScreenRecorderProcess()
    if (blocked) {
      _state.update { it.copy(screenCaptureBlocked = true, errorMessage = "Screen recording detected.") }
      return
    }
    val snap = _state.value
    _state.update { it.copy(isSubmittingDocument = true, errorMessage = null) }
    scope.launch {
      val body = "{\"document_type\":\"${snap.selectedDocumentType.wire}\",\"screen_capture_active\":false}"
      val resp = apiClient.executeChatRaw("POST", "/v1/verification/document/session", body)
      if (resp != null && resp.first in 200..299) {
        val map = runCatching { DesktopJson.parseObject(resp.second) }.getOrNull()
        if (map != null && DesktopJson.bool(map, "success")) {
          _state.update { s ->
            s.copy(
              isSubmittingDocument = false,
              documentChallengeId = DesktopJson.str(map, "challenge_id"),
              documentChallengeToken = DesktopJson.str(map, "challenge_token"),
              documentExpiresAtMs = DesktopJson.long(map, "expires_at_ms", 0L),
              docQualityReadinessPct = 0,
              routeStack = (s.routeStack + DesktopVerificationRoute.ID_LIVE_CAPTURE).distinctLast(),
            )
          }
          return@launch
        }
      }
      _state.update { it.copy(isSubmittingDocument = false, errorMessage = extractErr(resp)) }
    }
  }

  fun updateLiveDocReadiness(corners: Boolean, sharp: Boolean, light: Boolean, glareFree: Boolean, ocrReady: Boolean, pct: Int) {
    _state.update {
      it.copy(
        docCornersReady = corners,
        docSharpReady = sharp,
        docLightingReady = light,
        docGlareFreeReady = glareFree,
        docOcrPreviewReady = ocrReady,
        docQualityReadinessPct = pct.coerceIn(0, 100),
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
    _state.update { it.copy(isSubmittingDocument = true, errorMessage = null) }
    val b64 = Base64.getEncoder().encodeToString(jpegBytes)
    scope.launch {
      val body = buildString {
        append('{')
        append("\"challenge_id\":\"").append(DesktopJson.escape(snap.documentChallengeId)).append("\",")
        append("\"challenge_token\":\"").append(DesktopJson.escape(snap.documentChallengeToken)).append("\",")
        append("\"document_type\":\"").append(snap.selectedDocumentType.wire).append("\",")
        append("\"screen_capture_active\":false,")
        append("\"document_image_b64\":\"").append(b64).append("\",")
        append("\"client_ocr_raw_text\":\"").append(DesktopJson.escape(ocrRawText)).append("\",")
        append("\"sharpness_score\":").append(sharpnessScore).append(',')
        append("\"luminance_mean\":").append(luminanceMean).append(',')
        append("\"glare_ratio\":").append(glareRatio).append(',')
        append("\"all_corners_detected\":").append(allCornersDetected)
        append('}')
      }
      val resp = apiClient.executeChatRaw("POST", "/v1/verification/document/submit", body)
      if (resp != null && resp.first in 200..299) {
        val map = runCatching { DesktopJson.parseObject(resp.second) }.getOrNull()
        val stObj = map?.let { DesktopJson.objOrNull(it, "status") }
        if (map != null && DesktopJson.bool(map, "success") && DesktopJson.bool(map, "accepted")) {
          _state.update { s ->
            s.copy(
              isSubmittingDocument = false,
              status = stObj?.let(::parseStatus) ?: s.status,
              statusMessage = DesktopJson.str(map, "message").ifBlank { "Under review (up to 3 working days)" },
              routeStack = listOf(DesktopVerificationRoute.STATUS_REVIEW),
            )
          }
          return@launch
        }
      }
      _state.update { it.copy(isSubmittingDocument = false, errorMessage = extractErr(resp)) }
    }
  }

  fun openTiersExplorer() {
    _state.update { it.copy(routeStack = listOf(DesktopVerificationRoute.TIERS_PAGER), errorMessage = null) }
  }

  fun navigateBack() {
    _state.update { s ->
      if (s.routeStack.size <= 1) {
        s.copy(isOpen = false, routeStack = listOf(DesktopVerificationRoute.TIERS_PAGER), errorMessage = null)
      } else {
        s.copy(routeStack = s.routeStack.dropLast(1), errorMessage = null)
      }
    }
  }

  fun dismissBanner() {
    _state.update { it.copy(statusMessage = null, errorMessage = null) }
  }

  private fun parseStatus(m: Map<String, Any?>): DesktopVerificationStatus {
    val tiers = DesktopJson.objList(m, "tiers").map { t ->
      DesktopTierPricing(
        tier = DesktopJson.str(t, "tier"),
        title = DesktopJson.str(t, "title"),
        badgeTitle = DesktopJson.str(t, "badge_title"),
        currencyCode = DesktopJson.str(t, "currency_code").ifBlank { "USD" },
        currencySymbol = DesktopJson.str(t, "currency_symbol").ifBlank { "$" },
        monthlyDisplay = DesktopJson.str(t, "monthly_display"),
        annualDisplay = DesktopJson.str(t, "annual_display"),
        annualOriginalDisplay = DesktopJson.str(t, "annual_original_display"),
        discountPercent = DesktopJson.long(t, "discount_percent", 20L).toInt(),
      )
    }
    return DesktopVerificationStatus(
      verifiedTier = DesktopJson.str(m, "verified_tier").ifBlank { "none" },
      pendingTier = DesktopJson.str(m, "pending_tier").ifBlank { "none" },
      billingCycle = DesktopJson.str(m, "billing_cycle").ifBlank { "monthly" },
      planStatus = DesktopJson.str(m, "plan_status").ifBlank { "none" },
      trialEligible = if (m.containsKey("trial_eligible")) DesktopJson.bool(m, "trial_eligible") else true,
      trialEndsAtMs = DesktopJson.long(m, "trial_ends_at_ms", 0L),
      subscriptionEndsAtMs = DesktopJson.long(m, "subscription_ends_at_ms", 0L),
      livenessStatus = DesktopJson.str(m, "liveness_status").ifBlank { "not_started" },
      livenessScore = DesktopJson.long(m, "liveness_score", 0L).toInt(),
      documentType = DesktopJson.str(m, "document_type"),
      documentStatus = DesktopJson.str(m, "document_status").ifBlank { "not_started" },
      docLast4 = DesktopJson.str(m, "doc_last4"),
      docExpiryIso = DesktopJson.str(m, "doc_expiry_iso"),
      extractedFullName = DesktopJson.str(m, "extracted_full_name"),
      mergedToProfile = DesktopJson.bool(m, "merged_to_profile"),
      reviewStatus = DesktopJson.str(m, "review_status").ifBlank { "not_started" },
      reviewEtaWorkingDays = DesktopJson.long(m, "review_eta_working_days", 3L).toInt(),
      reviewSubmittedAtMs = DesktopJson.long(m, "review_submitted_at_ms", 0L),
      reviewDueAtMs = DesktopJson.long(m, "review_due_at_ms", 0L),
      verifiedAtMs = DesktopJson.long(m, "verified_at_ms", 0L),
      documentsPurged = DesktopJson.bool(m, "documents_purged"),
      documentsPurgedAtMs = DesktopJson.long(m, "documents_purged_at_ms", 0L),
      rejectionReason = DesktopJson.str(m, "rejection_reason"),
      countryIso = DesktopJson.str(m, "country_iso").ifBlank { "US" },
      tiers = tiers,
    )
  }

  private fun parsePayment(m: Map<String, Any?>) = DesktopVerificationPayment(
    paymentId = DesktopJson.str(m, "payment_id"),
    provider = DesktopJson.str(m, "provider"),
    providerReference = DesktopJson.str(m, "provider_reference"),
    providerOrderId = DesktopJson.str(m, "provider_order_id"),
    status = DesktopJson.str(m, "status"),
    checkoutUrl = DesktopJson.str(m, "checkout_url"),
  )

  private fun extractErr(resp: Pair<Int, String>?): String {
    if (resp == null) return "Network unavailable"
    val parsed = runCatching { DesktopJson.parseObject(resp.second) }.getOrNull()
    val msg = parsed?.let { DesktopJson.str(it, "message") }.orEmpty()
    if (msg.isNotBlank()) return msg
    return when (resp.first) {
      403 -> "Screen recording or mirroring is blocked during verification."
      409 -> "Free trial was already used. Subscribe via Paystack or PayPal."
      429 -> "Too many verification attempts. Please wait a few minutes."
      else -> "Verification request failed (${resp.first})"
    }
  }

  private fun openInBrowser(url: String) {
    runCatching {
      if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI(url))
    }
  }

  private fun <T> List<T>.distinctLast(): List<T> =
    if (size >= 2 && this[size - 1] == this[size - 2]) dropLast(1) else this
}
