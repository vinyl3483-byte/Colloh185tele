package telefam.company.stars

import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToLong

enum class StarPaymentProvider(val wire: String) {
  PAYSTACK("paystack"),
  PAYPAL("paypal"),
}

enum class TelefamStarsRoute {
  HUB,
  SEND_STARS,
  BUY_STARS,
  CHECKOUT,
}

data class StarGiftChatMeta(
  val stars: Long,
  val senderUserId: String,
  val senderName: String,
  val recipientUserId: String,
  val recipientName: String,
  val animationTier: Int,
  val ledgerEntryId: String,
)

data class CountryCurrencySpec(
  val countryIso: String,
  val currencyCode: String,
  val symbol: String,
  val ratePerStar: Double,
  val zeroDecimal: Boolean = false,
)

object TelefamStarsCatalog {
  val presetPackages: List<Long> = listOf(
    5L, 10L, 20L, 50L, 100L, 200L, 500L, 1_000L, 2_000L, 5_000L, 10_000L, 50_000L,
  )

  private val countrySpecs: Map<String, CountryCurrencySpec> = mapOf(
    "KE" to CountryCurrencySpec("KE", "KES", "Ksh ", 1.0, zeroDecimal = true),
    "NG" to CountryCurrencySpec("NG", "NGN", "\u20A6", 12.0, zeroDecimal = true),
    "GH" to CountryCurrencySpec("GH", "GHS", "GH\u20B5 ", 0.12, zeroDecimal = false),
    "ZA" to CountryCurrencySpec("ZA", "ZAR", "R ", 0.15, zeroDecimal = false),
    "UG" to CountryCurrencySpec("UG", "UGX", "UGX ", 30.0, zeroDecimal = true),
    "TZ" to CountryCurrencySpec("TZ", "TZS", "TZS ", 20.0, zeroDecimal = true),
    "RW" to CountryCurrencySpec("RW", "RWF", "RWF ", 10.0, zeroDecimal = true),
    "GB" to CountryCurrencySpec("GB", "GBP", "\u00A3", 0.0064, zeroDecimal = false),
    "DE" to CountryCurrencySpec("DE", "EUR", "\u20AC", 0.0074, zeroDecimal = false),
    "FR" to CountryCurrencySpec("FR", "EUR", "\u20AC", 0.0074, zeroDecimal = false),
    "ES" to CountryCurrencySpec("ES", "EUR", "\u20AC", 0.0074, zeroDecimal = false),
    "IT" to CountryCurrencySpec("IT", "EUR", "\u20AC", 0.0074, zeroDecimal = false),
    "NL" to CountryCurrencySpec("NL", "EUR", "\u20AC", 0.0074, zeroDecimal = false),
    "IE" to CountryCurrencySpec("IE", "EUR", "\u20AC", 0.0074, zeroDecimal = false),
    "CA" to CountryCurrencySpec("CA", "CAD", "CA$", 0.011, zeroDecimal = false),
    "AU" to CountryCurrencySpec("AU", "AUD", "A$", 0.012, zeroDecimal = false),
    "IN" to CountryCurrencySpec("IN", "INR", "\u20B9", 0.68, zeroDecimal = false),
    "US" to CountryCurrencySpec("US", "USD", "$", 0.008, zeroDecimal = false),
  )

  fun resolveCountryIso(profileCountryIso: String?, locationHint: String = ""): String {
    val cleanProfile = profileCountryIso.orEmpty().trim().uppercase(Locale.US)
    if (cleanProfile.length == 2 && cleanProfile.all { it in 'A'..'Z' }) {
      return cleanProfile
    }
    val lowerLoc = locationHint.lowercase(Locale.US)
    if (lowerLoc.contains("kenya") || lowerLoc.contains("nairobi") || lowerLoc.contains("mombasa")) return "KE"
    if (lowerLoc.contains("nigeria") || lowerLoc.contains("lagos") || lowerLoc.contains("abuja")) return "NG"
    if (lowerLoc.contains("ghana") || lowerLoc.contains("accra")) return "GH"
    if (lowerLoc.contains("south africa") || lowerLoc.contains("johannesburg")) return "ZA"
    if (lowerLoc.contains("united kingdom") || lowerLoc.contains("london")) return "GB"
    val deviceIso = Locale.getDefault().country.trim().uppercase(Locale.US)
    if (deviceIso.length == 2 && deviceIso.all { it in 'A'..'Z' }) {
      return deviceIso
    }
    return "US"
  }

  fun specForCountry(countryIso: String): CountryCurrencySpec {
    return countrySpecs[countryIso.trim().uppercase(Locale.US)]
      ?: countrySpecs.getValue("US")
  }

  fun formatCurrencyForStars(stars: Long, countryIso: String): String {
    val spec = specForCountry(countryIso)
    val safeStars = max(0L, stars)
    if (safeStars == 0L) {
      return if (spec.zeroDecimal) "${spec.symbol}0" else "${spec.symbol}0.00"
    }
    val amount = safeStars * spec.ratePerStar
    return if (spec.zeroDecimal || (spec.ratePerStar >= 1.0 && kotlin.math.abs(amount - amount.roundToLong()) < 0.001)) {
      "${spec.symbol}${formatStarsCount(amount.roundToLong())}"
    } else {
      String.format(Locale.US, "%s%.2f", spec.symbol, max(0.05, amount))
    }
  }

  fun formatStarsCount(stars: Long): String =
    NumberFormat.getIntegerInstance(Locale.US).format(stars)

  fun animationTierForStars(stars: Long): Int = when {
    stars >= 10_000L -> 5
    stars >= 1_000L -> 4
    stars >= 200L -> 3
    stars >= 50L -> 2
    else -> 1
  }

  fun parseStarGiftMeta(rawJson: String): StarGiftChatMeta? {
    if (rawJson.isBlank()) return null
    return runCatching {
      val obj = JSONObject(rawJson)
      val stars = obj.optLong("stars", 0L)
      if (stars <= 0L) return null
      StarGiftChatMeta(
        stars = stars,
        senderUserId = obj.optString("sender_user_id", ""),
        senderName = obj.optString("sender_name", ""),
        recipientUserId = obj.optString("recipient_user_id", ""),
        recipientName = obj.optString("recipient_name", ""),
        animationTier = obj.optInt("animation_tier", animationTierForStars(stars)),
        ledgerEntryId = obj.optString("ledger_entry_id", ""),
      )
    }.getOrNull()
  }

  fun formatSummaryLine(rawJson: String): String {
    val meta = parseStarGiftMeta(rawJson)
    return if (meta != null) {
      val unit = if (meta.stars == 1L) "Star" else "Stars"
      "Sent ${formatStarsCount(meta.stars)} $unit"
    } else {
      "Sent Stars"
    }
  }
}
