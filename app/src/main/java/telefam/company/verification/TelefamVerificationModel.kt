package telefam.company.verification

import telefam.company.data.network.TierPricingDto
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

enum class VerificationTierKind(
  val wire: String,
  val headerTitle: String,
  val headerSubtitle: String,
  val baseMonthlyKes: Long,
) {
  BUSINESS(
    wire = "business",
    headerTitle = "Business Verification",
    headerSubtitle = "Get the Business Badge",
    baseMonthlyKes = 1_200L,
  ),
  GOVERNMENT(
    wire = "government",
    headerTitle = "Government Verification",
    headerSubtitle = "Get the Government Badge",
    baseMonthlyKes = 700L,
  ),
  BLUE(
    wire = "blue",
    headerTitle = "Blue Verification",
    headerSubtitle = "Get the Blue Badge",
    baseMonthlyKes = 700L,
  );

  companion object {
    fun fromWire(value: String): VerificationTierKind = when (value.trim().lowercase()) {
      "business" -> BUSINESS
      "government" -> GOVERNMENT
      else -> BLUE
    }
  }
}

enum class VerificationBillingCycle(val wire: String) {
  MONTHLY("monthly"),
  ANNUAL("annual"),
}

enum class VerificationPaymentProvider(val wire: String) {
  PAYSTACK("paystack"),
  PAYPAL("paypal"),
}

enum class VerificationDocumentType(
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

enum class VerificationRoute {
  TIERS_PAGER,
  TRIAL_AND_PAYMENT,
  SELFIE_GUIDANCE,
  SELFIE_LIVENESS,
  ID_GUIDANCE_AND_SELECT,
  ID_LIVE_CAPTURE,
  STATUS_REVIEW,
}

data class VerificationBenefitItem(
  val iconKey: String,
  val title: String,
  val description: String,
)

data class IdGuidanceExampleItem(
  val key: String,
  val label: String,
  val isValid: Boolean,
)

data class VerificationCountryCurrency(
  val currencyCode: String,
  val symbol: String,
  val rateFromKes: Double,
  val zeroDecimal: Boolean,
)

object TelefamVerificationCatalog {

  private val countryRates: Map<String, VerificationCountryCurrency> = mapOf(
    "KE" to VerificationCountryCurrency("KES", "Ksh ", 1.0, zeroDecimal = true),
    "NG" to VerificationCountryCurrency("NGN", "NGN ", 12.0, zeroDecimal = true),
    "GH" to VerificationCountryCurrency("GHS", "GHS ", 0.12, zeroDecimal = false),
    "ZA" to VerificationCountryCurrency("ZAR", "R ", 0.15, zeroDecimal = false),
    "UG" to VerificationCountryCurrency("UGX", "UGX ", 30.0, zeroDecimal = true),
    "TZ" to VerificationCountryCurrency("TZS", "TZS ", 20.0, zeroDecimal = true),
    "RW" to VerificationCountryCurrency("RWF", "RWF ", 10.0, zeroDecimal = true),
    "GB" to VerificationCountryCurrency("GBP", "\u00A3", 0.0064, zeroDecimal = false),
    "DE" to VerificationCountryCurrency("EUR", "\u20AC", 0.0074, zeroDecimal = false),
    "FR" to VerificationCountryCurrency("EUR", "\u20AC", 0.0074, zeroDecimal = false),
    "ES" to VerificationCountryCurrency("EUR", "\u20AC", 0.0074, zeroDecimal = false),
    "IT" to VerificationCountryCurrency("EUR", "\u20AC", 0.0074, zeroDecimal = false),
    "NL" to VerificationCountryCurrency("EUR", "\u20AC", 0.0074, zeroDecimal = false),
    "IE" to VerificationCountryCurrency("EUR", "\u20AC", 0.0074, zeroDecimal = false),
    "BE" to VerificationCountryCurrency("EUR", "\u20AC", 0.0074, zeroDecimal = false),
    "PT" to VerificationCountryCurrency("EUR", "\u20AC", 0.0074, zeroDecimal = false),
    "AT" to VerificationCountryCurrency("EUR", "\u20AC", 0.0074, zeroDecimal = false),
    "FI" to VerificationCountryCurrency("EUR", "\u20AC", 0.0074, zeroDecimal = false),
    "CA" to VerificationCountryCurrency("CAD", "CA$", 0.011, zeroDecimal = false),
    "AU" to VerificationCountryCurrency("AUD", "A$", 0.012, zeroDecimal = false),
    "IN" to VerificationCountryCurrency("INR", "\u20B9", 0.68, zeroDecimal = false),
    "US" to VerificationCountryCurrency("USD", "$", 0.008325, zeroDecimal = false),
  )

  fun pricingForTier(
    tier: VerificationTierKind,
    countryIso: String,
    serverTiers: List<TierPricingDto> = emptyList(),
  ): TierPricingDto {
    serverTiers.firstOrNull { it.tier.equals(tier.wire, ignoreCase = true) }?.let { dto ->
      if (dto.monthlyDisplay.isNotBlank() && dto.annualDisplay.isNotBlank()) {
        return dto
      }
    }
    val spec = countryRates[countryIso.trim().uppercase(Locale.US)]
      ?: countryRates.getValue("US")
    val monthlyKes = tier.baseMonthlyKes.toDouble()
    val fullAnnualKes = monthlyKes * 12.0
    val discountedAnnualKes = fullAnnualKes * 0.80

    fun formatMoney(kes: Double): String {
      val converted = kes * spec.rateFromKes
      return if (spec.zeroDecimal) {
        "${spec.symbol}${NumberFormat.getIntegerInstance(Locale.US).format(converted.roundToLong())}"
      } else {
        String.format(Locale.US, "%s%.2f", spec.symbol, converted)
      }
    }

    return TierPricingDto(
      tier = tier.wire,
      title = tier.headerTitle,
      badgeTitle = tier.headerSubtitle,
      currencyCode = spec.currencyCode,
      currencySymbol = spec.symbol,
      monthlyDisplay = formatMoney(monthlyKes),
      annualDisplay = formatMoney(discountedAnnualKes),
      annualOriginalDisplay = formatMoney(fullAnnualKes),
      discountPercent = 20,
    )
  }

  // Matches Business Badge reference screenshot (12 items, 2 columns)
  val businessBenefits: List<VerificationBenefitItem> = listOf(
    VerificationBenefitItem("badge", "Business Badge", "Show your verified business badge next to your name."),
    VerificationBenefitItem("link", "Custom Links", "Add your website, store, and social media links."),
    VerificationBenefitItem("group", "Business Profile", "Get a professional business profile with custom details, links and media."),
    VerificationBenefitItem("verified_doc", "Verified Contact Info", "Display your business phone, email and location."),
    VerificationBenefitItem("support", "Priority Support", "Get faster, dedicated support for your business needs."),
    VerificationBenefitItem("media", "Featured Media", "Showcase your products, services and latest updates with media."),
    VerificationBenefitItem("security", "Enhanced Security", "Advanced protection against impersonation, fraud and fake accounts."),
    VerificationBenefitItem("messages", "Business Messages", "Send automated replies and quick responses."),
    VerificationBenefitItem("analytics", "Advanced Analytics", "Track profile visits, engagement and audience insights."),
    VerificationBenefitItem("group", "Group Verification", "Create and manage verified business groups and communities."),
    VerificationBenefitItem("payments", "Payment Integration", "Accept payments directly through the app (M-Pesa, PayPal, etc)."),
    VerificationBenefitItem("star", "Exclusive Features", "Get early access to new features, promotions and premium tools."),
  )

  // Matches Government Badge reference screenshot (9 items, 2 columns)
  val governmentBenefits: List<VerificationBenefitItem> = listOf(
    VerificationBenefitItem("badge", "Official Government Badge", "Show your verified government status with a red shield badge."),
    VerificationBenefitItem("verified_doc", "Verified Contact Info", "Display your official phone, email and location."),
    VerificationBenefitItem("security", "Enhanced Security", "Additional protection against impersonation and fraud."),
    VerificationBenefitItem("media", "Featured Media", "Showcase your department, initiatives and latest updates."),
    VerificationBenefitItem("support", "Priority Support", "Get faster assistance from our support team."),
    VerificationBenefitItem("group", "Group Verification", "Create and manage verified official groups and communities."),
    VerificationBenefitItem("analytics", "Advanced Analytics", "Access detailed insights on your account performance and reach."),
    VerificationBenefitItem("star", "Exclusive Features", "Early access to new tools, reports and beta features."),
    VerificationBenefitItem("payments", "Payment Integration", "Accept payments directly through the app (M-Pesa, PayPal, etc)."),
  )

  // Matches Blue Badge reference screenshot (6 chevron list items)
  val blueBenefits: List<VerificationBenefitItem> = listOf(
    VerificationBenefitItem("group", "Group Verification", "Create and manage verified groups with a blue badge."),
    VerificationBenefitItem("stickers", "New Stickers", "Access exclusive stickers and animated reactions."),
    VerificationBenefitItem("security", "Enhanced Protection", "Priority support, stronger security and fraud protection."),
    VerificationBenefitItem("badge", "Badge Next to Your Name", "Show your verified status with a blue badge."),
    VerificationBenefitItem("calls", "HD Calls", "Enjoy crystal clear audio and video calls."),
    VerificationBenefitItem("messages", "Send Paid Messages", "Charge for your messages and earn from your content."),
  )

  // Matches Screenshot 4 ID Guidance Rules
  val correctIdRules: List<String> = listOf(
    "All corners visible",
    "Not blurry",
    "No glare / reflection",
    "All details readable",
  )

  val incorrectIdExamples: List<IdGuidanceExampleItem> = listOf(
    IdGuidanceExampleItem("blurry", "Too blurry", isValid = false),
    IdGuidanceExampleItem("glare", "Glare / Reflection", isValid = false),
    IdGuidanceExampleItem("corners", "Not all corners visible", isValid = false),
    IdGuidanceExampleItem("aligned", "Not aligned", isValid = false),
    IdGuidanceExampleItem("too_far", "Too far", isValid = false),
    IdGuidanceExampleItem("too_close", "Too close", isValid = false),
    IdGuidanceExampleItem("too_dark", "Too dark", isValid = false),
    IdGuidanceExampleItem("obstructed", "Obstructed", isValid = false),
  )

  fun documentOptionsForTier(tier: VerificationTierKind): List<VerificationDocumentType> {
    return when (tier) {
      VerificationTierKind.BUSINESS -> listOf(
        VerificationDocumentType.NATIONAL_ID,
        VerificationDocumentType.PASSPORT,
        VerificationDocumentType.DRIVERS_LICENSE,
        VerificationDocumentType.MILITARY_ID,
        VerificationDocumentType.BUSINESS_LICENSE,
      )
      VerificationTierKind.GOVERNMENT -> listOf(
        VerificationDocumentType.GOV_WARRANT,
        VerificationDocumentType.MILITARY_ID,
        VerificationDocumentType.NATIONAL_ID,
        VerificationDocumentType.PASSPORT,
        VerificationDocumentType.DRIVERS_LICENSE,
      )
      VerificationTierKind.BLUE -> listOf(
        VerificationDocumentType.NATIONAL_ID,
        VerificationDocumentType.PASSPORT,
        VerificationDocumentType.DRIVERS_LICENSE,
        VerificationDocumentType.MILITARY_ID,
      )
    }
  }

  fun stepDirectionLabel(step: String): String = when (step.trim().lowercase()) {
    "left" -> "Turn head slowly LEFT"
    "right" -> "Turn head slowly RIGHT"
    "up" -> "Tilt head slightly UP"
    else -> "Look straight at camera (CENTER)"
  }
}
