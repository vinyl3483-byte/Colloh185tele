package telefam.company.subscriptions

import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

enum class SubscriptionBillingCycle(val wireValue: String) {
  MONTHLY("monthly"),
  ANNUAL("annual"),
  ;

  companion object {
    fun fromWire(value: String?): SubscriptionBillingCycle {
      return if (value.equals("annual", ignoreCase = true)) ANNUAL else MONTHLY
    }
  }
}

enum class SubscriptionPaymentProvider(val wireValue: String, val displayName: String) {
  PAYSTACK("paystack", "Paystack"),
  PAYPAL("paypal", "PayPal"),
}

enum class SubscriptionsHubTab(val id: String) {
  SUBSCRIBERS("subscribers"),
  ACTIVE_PLANS("active_plans"),
  GOAL_PRICING("goal_pricing"),
}

enum class SubscriptionsRoute {
  HUB,
  CREATOR_SUBSCRIBE,
  CHECKOUT_WEBVIEW,
}

data class SubscriptionPricingModel(
  val monthlyUsdCents: Long = 500L,
  val annualUsdCents: Long = 4800L,
  val annualDiscountPct: Int = 20,
  val currencyCode: String = "USD",
  val currencySymbol: String = "$",
  val monthlyDisplay: String = "$5.00",
  val annualDisplay: String = "$48.00",
  val annualOriginalDisplay: String = "$60.00",
  val paystackMonthlyMinor: Long = 500L,
  val paystackAnnualMinor: Long = 4800L,
  val paystackCurrency: String = "USD",
  val paypalMonthlyValue: String = "5.00",
  val paypalAnnualValue: String = "48.00",
  val paypalCurrency: String = "USD",
)

data class GoalPresetItem(
  val amountUsdCents: Long,
  val usdLabel: String,
  val localDisplay: String,
)

data class CreatorPlanModel(
  val creatorUserId: String = "",
  val enabled: Boolean = true,
  val monthlyUsdCents: Long = 500L,
  val annualDiscountPct: Int = 20,
  val monthlyGoalUsdCents: Long = 10000L,
  val goalUsdDisplay: String = "$100.00",
  val goalLocalDisplay: String = "$100.00",
  val currentMrrUsdCents: Long = 0L,
  val currentMrrUsdDisplay: String = "$0.00",
  val currentMrrLocalDisplay: String = "$0.00",
  val goalProgressPct: Float = 0f,
  val goalReached: Boolean = false,
  val activeSubscriberCount: Int = 0,
  val perksSummary: String = "Exclusive subscriber updates, priority inbox access, and supporter badge.",
  val pricing: SubscriptionPricingModel = SubscriptionPricingModel(),
  val updatedAtMs: Long = 0L,
)

data class SubscriptionRecordModel(
  val subscriptionId: String,
  val subscriberUserId: String,
  val subscriberName: String,
  val subscriberUsername: String,
  val subscriberAvatarUrl: String,
  val subscriberVerifiedTier: String,
  val creatorUserId: String,
  val creatorName: String,
  val creatorUsername: String,
  val creatorAvatarUrl: String,
  val creatorVerifiedTier: String,
  val billingCycle: SubscriptionBillingCycle,
  val status: String,
  val autoRenew: Boolean,
  val amountUsdCents: Long,
  val amountDisplay: String,
  val provider: String,
  val startedAtMs: Long,
  val currentPeriodEndMs: Long,
  val cancelledAtMs: Long,
  val canCancel: Boolean,
  val canRenew: Boolean,
)

data class CreatorOfferingModel(
  val creatorUserId: String = "",
  val creatorName: String = "",
  val creatorUsername: String = "",
  val creatorAvatarUrl: String = "",
  val creatorVerifiedTier: String = "none",
  val countryIso: String = "US",
  val plan: CreatorPlanModel = CreatorPlanModel(),
  val viewerSubscription: SubscriptionRecordModel? = null,
  val isOwner: Boolean = false,
)

data class SubscriptionCheckoutSession(
  val paymentId: String = "",
  val creatorUserId: String = "",
  val billingCycle: SubscriptionBillingCycle = SubscriptionBillingCycle.MONTHLY,
  val provider: SubscriptionPaymentProvider = SubscriptionPaymentProvider.PAYSTACK,
  val providerReference: String = "",
  val providerOrderId: String = "",
  val expectedCurrency: String = "USD",
  val expectedAmountDisplay: String = "",
  val checkoutUrl: String = "",
  val returnUrl: String = "",
  val isVerifying: Boolean = false,
)

data class TelefamSubscriptionsUiState(
  val isOpen: Boolean = false,
  val currentRoute: SubscriptionsRoute = SubscriptionsRoute.HUB,
  val activeHubTab: SubscriptionsHubTab = SubscriptionsHubTab.SUBSCRIBERS,
  val countryIso: String = "US",
  val currencyCode: String = "USD",
  val isLoading: Boolean = false,
  val isSavingPlan: Boolean = false,
  val isMutating: Boolean = false,
  val myPlan: CreatorPlanModel = CreatorPlanModel(),
  val draftPlanEnabled: Boolean = true,
  val draftMonthlyUsdCents: Long = 500L,
  val draftMonthlyGoalUsdCents: Long = 10000L,
  val draftAnnualDiscountPct: Int = 20,
  val draftPerksSummary: String = "Exclusive subscriber updates, priority inbox access, and supporter badge.",
  val subscribers: List<SubscriptionRecordModel> = emptyList(),
  val activeSubscriptions: List<SubscriptionRecordModel> = emptyList(),
  val goalPresets: List<GoalPresetItem> = TelefamSubscriptionsCatalog.defaultGoalPresets("US"),
  val creatorOffering: CreatorOfferingModel? = null,
  val selectedCycle: SubscriptionBillingCycle = SubscriptionBillingCycle.MONTHLY,
  val selectedProvider: SubscriptionPaymentProvider = SubscriptionPaymentProvider.PAYSTACK,
  val activeCheckout: SubscriptionCheckoutSession? = null,
  val statusMessage: String? = null,
  val errorMessage: String? = null,
)

object TelefamSubscriptionsCatalog {
  const val MIN_MONTHLY_USD_CENTS: Long = 50L    // $0.50
  const val MAX_MONTHLY_USD_CENTS: Long = 10_000L // $100.00
  const val MIN_GOAL_USD_CENTS: Long = 50L       // $0.50
  const val MAX_GOAL_SLIDER_CENTS: Long = 10_000L // $100.00 primary slider

  val goalPresetCents: List<Long> = listOf(50L, 100L, 200L, 500L, 1_000L, 2_500L, 5_000L, 10_000L)

  private data class CurrencySpec(
    val code: String,
    val symbol: String,
    val rateFromUsd: Double,
    val zeroDecimal: Boolean,
  )

  private fun specFor(countryIso: String): CurrencySpec {
    return when (countryIso.trim().uppercase(Locale.US)) {
      "KE" -> CurrencySpec("KES", "Ksh ", 129.0, false)
      "NG" -> CurrencySpec("NGN", "NGN ", 1550.0, false)
      "GH" -> CurrencySpec("GHS", "GHS ", 15.2, false)
      "ZA" -> CurrencySpec("ZAR", "R ", 18.2, false)
      "UG" -> CurrencySpec("UGX", "UGX ", 3700.0, true)
      "TZ" -> CurrencySpec("TZS", "TZS ", 2650.0, true)
      "RW" -> CurrencySpec("RWF", "RWF ", 1350.0, true)
      "GB" -> CurrencySpec("GBP", "GBP ", 0.79, false)
      "DE", "FR", "ES", "IT", "NL", "IE", "BE", "PT", "AT", "FI" -> CurrencySpec("EUR", "EUR ", 0.92, false)
      "CA" -> CurrencySpec("CAD", "CA$", 1.38, false)
      "AU" -> CurrencySpec("AUD", "A$", 1.52, false)
      "IN" -> CurrencySpec("INR", "INR ", 84.5, false)
      else -> CurrencySpec("USD", "$", 1.0, false)
    }
  }

  fun formatUsd(usdCents: Long): String {
    return String.format(Locale.US, "$%.2f", usdCents / 100.0)
  }

  fun formatLocalFromUsdCents(usdCents: Long, countryIso: String): String {
    val spec = specFor(countryIso)
    val raw = (usdCents / 100.0) * spec.rateFromUsd
    if (spec.zeroDecimal || (spec.rateFromUsd >= 20.0 && raw >= 100.0)) {
      val rounded = raw.roundToLong()
      val formatted = DecimalFormat("#,###").format(rounded)
      return "${spec.symbol}$formatted"
    }
    val rounded2 = (raw * 100.0).roundToInt() / 100.0
    return String.format(Locale.US, "%s%.2f", spec.symbol, rounded2)
  }

  fun computeAnnualUsdCents(monthlyUsdCents: Long, discountPct: Int): Long {
    val safeMonthly = monthlyUsdCents.coerceIn(MIN_MONTHLY_USD_CENTS, MAX_MONTHLY_USD_CENTS)
    val safeDisc = discountPct.coerceIn(0, 50)
    val full = safeMonthly * 12L
    return ((full * (100 - safeDisc)) / 100.0).roundToLong().coerceAtLeast(MIN_MONTHLY_USD_CENTS)
  }

  fun previewPricing(monthlyUsdCents: Long, discountPct: Int, countryIso: String): SubscriptionPricingModel {
    val safeMonthly = monthlyUsdCents.coerceIn(MIN_MONTHLY_USD_CENTS, MAX_MONTHLY_USD_CENTS)
    val safeDisc = discountPct.coerceIn(0, 50)
    val annualFull = safeMonthly * 12L
    val annualDisc = computeAnnualUsdCents(safeMonthly, safeDisc)
    val spec = specFor(countryIso)
    return SubscriptionPricingModel(
      monthlyUsdCents = safeMonthly,
      annualUsdCents = annualDisc,
      annualDiscountPct = safeDisc,
      currencyCode = spec.code,
      currencySymbol = spec.symbol,
      monthlyDisplay = formatLocalFromUsdCents(safeMonthly, countryIso),
      annualDisplay = formatLocalFromUsdCents(annualDisc, countryIso),
      annualOriginalDisplay = formatLocalFromUsdCents(annualFull, countryIso),
    )
  }

  fun defaultGoalPresets(countryIso: String): List<GoalPresetItem> {
    return goalPresetCents.map { cents ->
      GoalPresetItem(
        amountUsdCents = cents,
        usdLabel = formatUsd(cents),
        localDisplay = formatLocalFromUsdCents(cents, countryIso),
      )
    }
  }

  fun resolveCountryIso(profileCountryIso: String?, locationHint: String?): String {
    val rawIso = profileCountryIso.orEmpty().trim().uppercase(Locale.US)
    if (rawIso.length == 2) return rawIso
    val loc = locationHint.orEmpty().lowercase(Locale.US)
    return when {
      "kenya" in loc || "nairobi" in loc || "mombasa" in loc -> "KE"
      "nigeria" in loc || "lagos" in loc || "abuja" in loc -> "NG"
      "ghana" in loc || "accra" in loc -> "GH"
      "south africa" in loc || "johannesburg" in loc -> "ZA"
      "uganda" in loc || "kampala" in loc -> "UG"
      "tanzania" in loc || "dar es salaam" in loc -> "TZ"
      "rwanda" in loc || "kigali" in loc -> "RW"
      "united kingdom" in loc || "london" in loc -> "GB"
      "canada" in loc || "toronto" in loc -> "CA"
      "australia" in loc || "sydney" in loc -> "AU"
      "india" in loc || "mumbai" in loc -> "IN"
      else -> Locale.getDefault().country.takeIf { it.length == 2 } ?: "US"
    }
  }
}
