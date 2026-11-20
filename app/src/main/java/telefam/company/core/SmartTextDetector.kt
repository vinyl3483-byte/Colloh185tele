package telefam.company.core

import androidx.collection.LruCache

sealed interface SmartEntity {
  val start: Int
  val end: Int
  val rawText: String

  data class Email(
    override val start: Int,
    override val end: Int,
    override val rawText: String,
    val emailAddress: String,
  ) : SmartEntity

  data class Url(
    override val start: Int,
    override val end: Int,
    override val rawText: String,
    val normalizedUrl: String,
    val domain: String,
  ) : SmartEntity

  data class OtpCode(
    override val start: Int,
    override val end: Int,
    override val rawText: String,
    val code: String,
  ) : SmartEntity

  data class PhoneNumber(
    override val start: Int,
    override val end: Int,
    override val rawText: String,
    val dialNumber: String,
    val formattedDisplay: String,
  ) : SmartEntity
}

data class SmartTextAnalysis(
  val text: String,
  val entities: List<SmartEntity>,
  val urls: List<SmartEntity.Url>,
  val otpCodes: List<SmartEntity.OtpCode>,
)

object SmartTextDetector {
  private val analysisCache = LruCache<String, SmartTextAnalysis>(512)

  private val dangerousSchemes = listOf(
    "javascript:",
    "data:",
    "file:",
    "vbscript:",
    "about:",
    "content:",
    "intent:",
    "blob:",
    "ftp:",
    "tel:",
    "mailto:",
  )

  // Common recognized web TLDs (gTLDs & ccTLDs) to prevent matching filenames/versions like "v2.0" or "photo.jpg"
  private val recognizedTlds = setOf(
    "com", "org", "net", "edu", "gov", "io", "co", "app", "dev", "me", "tv", "ai",
    "gg", "ly", "be", "to", "ke", "uk", "us", "ca", "de", "fr", "jp", "cn", "in",
    "au", "nz", "za", "ng", "ug", "tz", "rw", "et", "gh", "eg", "ma", "info", "biz",
    "xyz", "tech", "online", "site", "store", "cloud", "news", "media", "studio",
    "company", "pro", "live", "page", "link", "click", "wiki", "blog", "shop", "club",
    "cc", "fm", "so", "it", "es", "nl", "ch", "se", "no", "dk", "fi", "pl", "ua",
    "br", "mx", "ar", "cl", "sg", "hk", "tw", "kr", "id", "my", "ph", "th", "vn",
    "ae", "sa", "il", "tr", "ac", "int", "mil", "name", "mobi", "africa", "ke",
  )

  private val emailRegex = Regex(
    """(?<![A-Za-z0-9._%+\-])([A-Za-z0-9][A-Za-z0-9._%+\-]{0,63}@[A-Za-z0-9](?:[A-Za-z0-9\-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9\-]{0,61}[A-Za-z0-9])?)+)"""
  )

  // Captures explicit http(s) URLs or scheme-less domains/paths embedded inside natural sentences
  private val urlRegex = Regex(
    """(?i)(?<![A-Za-z0-9@._\-/])((?:https?://)?(?:www\.)?(?:[A-Za-z0-9](?:[A-Za-z0-9\-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]{2,24}(?::\d{2,5})?(?:/[^\s<>"{}|\\^`\[\]]*)?(?:\?[^\s<>"{}|\\^`\[\]]*)?(?:#[^\s<>"{}|\\^`\[\]]*)?)"""
  )

  // Explicitly detect dangerous scheme prefixes so we never accidentally linkify their domain tail (e.g. "file:///etc/passwd" or "javascript:foo.bar()")
  private val unsafeSchemeSpanRegex = Regex(
    """(?i)\b(?:javascript|data|file|vbscript|about|content|intent|blob):[^\s]*"""
  )

  // Context-aware OTP phrases: code after keyword OR keyword after code ("Use 543218 to verify")
  private val otpPrefixContextRegex = Regex(
    """(?i)(?:verification\s*code|login\s*code|security\s*code|one[-\s]*time\s*(?:password|passcode|pin|code)|auth(?:entication)?\s*code|confirmation\s*code|access\s*code|reset\s*code|2fa\s*code|mfa\s*code|backup\s*code|passcode|otp|pin(?:\s*code)?|code)\s*(?:is|=|:|-)?\s*([0-9]{4,8})\b"""
  )

  private val otpSuffixContextRegex = Regex(
    """(?i)(?:use|enter|input|type|is)\s+([0-9]{4,8})\s+(?:to\s+(?:verify|confirm|log\s*in|login|sign\s*in|authenticate|complete|unlock|activate)|as\s+(?:your\s+)?(?:otp|code|verification|passcode))"""
  )

  // International E.164 (+254 712 345 678, +1 (650) 253-0000), Kenyan national (07XX XXX XXX, 01XX XXX XXX), or spaced/hyphenated national phone numbers
  private val phoneRegex = Regex(
    """(?<![A-Za-z0-9+.\-])(\+[1-9](?:[\s\-().]*\d){6,14}|\b0[17](?:[\s\-]*\d){8}\b|\b\(?[0-9]{3}\)?[\s\-][0-9]{3}[\s\-][0-9]{4}\b)"""
  )

  private val currencyPrefixRegex = Regex("""(?i)(?:[$€£¥]|kes|ksh|usd|eur|gbp|ugx|tzs|ngn|zar)\s*$""")
  private val ipv4LiteralRegex = Regex("""^\d{1,3}(?:\.\d{1,3}){3}$""")

  fun analyze(text: String): SmartTextAnalysis {
    if (text.isEmpty()) {
      return SmartTextAnalysis(text = text, entities = emptyList(), urls = emptyList(), otpCodes = emptyList())
    }
    analysisCache.get(text)?.let { return it }

    val taken = mutableListOf<IntRange>()
    val entities = mutableListOf<SmartEntity>()

    fun overlaps(start: Int, endExclusive: Int): Boolean {
      return taken.any { start < it.last + 1 && endExclusive > it.first }
    }

    fun claim(start: Int, endExclusive: Int) {
      if (endExclusive > start) {
        taken += start until endExclusive
      }
    }

    // Reserve unsafe scheme spans first so nothing inside "javascript:foo.com()" is falsely treated as URL
    unsafeSchemeSpanRegex.findAll(text).forEach { m ->
      claim(m.range.first, m.range.last + 1)
    }

    // 1. Emails (highest interactive entity priority)
    emailRegex.findAll(text).forEach { m ->
      val start = m.range.first
      val raw = trimTrailingSentencePunctuation(m.value)
      val end = start + raw.length
      if (end > start && !overlaps(start, end) && isValidEmail(raw)) {
        claim(start, end)
        entities += SmartEntity.Email(
          start = start,
          end = end,
          rawText = raw,
          emailAddress = raw,
        )
      }
    }

    // 2. Public/Safe HTTP(S) URLs
    urlRegex.findAll(text).forEach { m ->
      val start = m.range.first
      if (start > 0 && text[start - 1] == '@') return@forEach
      val trimmed = trimUrlTrailingPunctuation(m.value)
      val end = start + trimmed.length
      if (end <= start || overlaps(start, end)) return@forEach

      val hasExplicitScheme = trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true)
      val normalized = normalizeSafeHttpUrl(trimmed, requireKnownTld = !hasExplicitScheme) ?: return@forEach
      val domain = extractCleanDomain(normalized)
      if (domain.isBlank()) return@forEach

      claim(start, end)
      entities += SmartEntity.Url(
        start = start,
        end = end,
        rawText = trimmed,
        normalizedUrl = normalized,
        domain = domain,
      )
    }

    // 3. Context-aware OTP / Verification Codes (4–8 digits)
    val otpCandidates = mutableListOf<Pair<IntRange, String>>()
    otpPrefixContextRegex.findAll(text).forEach { m ->
      val g = m.groups[1] ?: return@forEach
      otpCandidates += (g.range to g.value)
    }
    otpSuffixContextRegex.findAll(text).forEach { m ->
      val g = m.groups[1] ?: return@forEach
      otpCandidates += (g.range to g.value)
    }
    for ((range, digits) in otpCandidates) {
      val start = range.first
      val end = range.last + 1
      if (overlaps(start, end)) continue
      if (!isPlausibleOtpContext(text, start, end, digits)) continue
      claim(start, end)
      entities += SmartEntity.OtpCode(
        start = start,
        end = end,
        rawText = digits,
        code = digits,
      )
    }

    // 4. Phone numbers (Kenyan mobile/landline + international formats)
    phoneRegex.findAll(text).forEach { m ->
      val raw = trimTrailingSentencePunctuation(m.value)
      val start = m.range.first
      val end = start + raw.length
      if (end <= start || overlaps(start, end)) return@forEach

      val normalizedDial = normalizeDialPhone(raw) ?: return@forEach
      // Prevent matching a price or date fragment
      if (looksLikePriceOrDateFragment(text, start, end)) return@forEach

      claim(start, end)
      entities += SmartEntity.PhoneNumber(
        start = start,
        end = end,
        rawText = raw,
        dialNumber = normalizedDial,
        formattedDisplay = formatPhoneForDisplay(raw),
      )
    }

    val sorted = entities.sortedBy { it.start }
    val analysis = SmartTextAnalysis(
      text = text,
      entities = sorted,
      urls = sorted.filterIsInstance<SmartEntity.Url>(),
      otpCodes = sorted.filterIsInstance<SmartEntity.OtpCode>(),
    )
    analysisCache.put(text, analysis)
    return analysis
  }

  fun extractUrls(text: String): List<SmartEntity.Url> = analyze(text).urls

  fun normalizeSafeHttpUrl(raw: String, requireKnownTld: Boolean = false): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty() || trimmed.length > 2048 || trimmed.any { it.isWhitespace() }) return null
    val lower = trimmed.lowercase()
    if (dangerousSchemes.any { lower.startsWith(it) }) return null

    val hasScheme = lower.startsWith("http://") || lower.startsWith("https://")
    if (!hasScheme && trimmed.contains("://")) return null

    val candidate = if (hasScheme) trimmed else "https://$trimmed"
    val afterScheme = candidate.substringAfter("://")
    if (afterScheme.isEmpty() || afterScheme.contains("@")) return null

    val authority = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
    if (authority.isEmpty()) return null
    val hostPort = authority.lowercase()
    val host = hostPort.substringBefore(':').trimEnd('.')
    if (host.isEmpty() || host.contains("..") || host.startsWith("-") || host.endsWith("-")) return null

    // Reject localhost, .local, .internal, private/metadata IPs
    if (isForbiddenHost(host)) return null

    if (ipv4LiteralRegex.matches(host)) {
      if (!isPublicIPv4Literal(host)) return null
    } else {
      val labels = host.split('.')
      if (labels.size < 2) return null
      if (labels.any { it.isEmpty() || it.length > 63 }) return null
      val tld = labels.last()
      if (tld.length < 2 || !tld.all { it.isLetter() }) return null
      if (requireKnownTld && tld !in recognizedTlds) return null
    }

    val schemePrefix = if (lower.startsWith("http://")) "http://" else "https://"
    val rest = candidate.substringAfter("://").let { rawRest ->
      val idx = rawRest.indexOfFirst { it == '/' || it == '?' || it == '#' }
      if (idx == -1) hostPort else hostPort + rawRest.substring(idx)
    }
    return schemePrefix + rest
  }

  fun extractCleanDomain(normalizedUrl: String): String {
    val afterScheme = normalizedUrl.substringAfter("://", normalizedUrl)
    val authority = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
    val host = authority.substringBefore(':').lowercase()
    return host.removePrefix("www.")
  }

  fun isValidEmail(raw: String): Boolean {
    val trimmed = raw.trim()
    val atIdx = trimmed.indexOf('@')
    if (atIdx <= 0 || atIdx != trimmed.lastIndexOf('@')) return false
    val local = trimmed.substring(0, atIdx)
    val domain = trimmed.substring(atIdx + 1).lowercase()
    if (local.length > 64 || domain.length > 253) return false
    if (local.startsWith('.') || local.endsWith('.') || local.contains("..")) return false
    if (domain.startsWith('.') || domain.endsWith('.') || domain.contains("..")) return false
    val labels = domain.split('.')
    if (labels.size < 2) return false
    if (labels.any { it.isEmpty() || it.startsWith('-') || it.endsWith('-') }) return false
    val tld = labels.last()
    return tld.length in 2..24 && tld.all { it.isLetter() }
  }

  fun formatPhoneForDisplay(raw: String): String {
    val trimmed = raw.trim()
    val hasPlus = trimmed.startsWith("+")
    val digits = trimmed.filter { it.isDigit() }
    if (digits.isEmpty()) return trimmed

    // Kenyan +254 7XX XXX XXX or +254 1XX XXX XXX (12 digits total)
    if (hasPlus && digits.startsWith("254") && digits.length == 12) {
      return "+254 ${digits.substring(3, 6)} ${digits.substring(6, 9)} ${digits.substring(9, 12)}"
    }
    // Kenyan local 07XX XXX XXX or 01XX XXX XXX (10 digits total)
    if (!hasPlus && digits.length == 10 && (digits.startsWith("07") || digits.startsWith("01"))) {
      return "${digits.substring(0, 4)} ${digits.substring(4, 7)} ${digits.substring(7, 10)}"
    }
    // US/Canada +1 XXX XXX XXXX
    if (hasPlus && digits.startsWith("1") && digits.length == 11) {
      return "+1 ${digits.substring(1, 4)} ${digits.substring(4, 7)} ${digits.substring(7, 11)}"
    }
    // General international E.164 grouping: preserve + and chunk digits legibly
    if (hasPlus) {
      val ccLen = when {
        digits.startsWith("1") || digits.startsWith("7") -> 1
        digits.length >= 11 && common3DigitCc.any { digits.startsWith(it) } -> 3
        else -> 2
      }.coerceAtMost((digits.length - 4).coerceAtLeast(1))
      val cc = digits.substring(0, ccLen)
      val rest = digits.substring(ccLen).chunked(3).joinToString(" ")
      return "+$cc $rest"
    }
    return digits.chunked(3).joinToString(" ")
  }

  private val common3DigitCc = setOf(
    "254", "255", "256", "250", "257", "251", "252", "234", "233", "212", "213", "216",
    "260", "263", "264", "265", "266", "267", "268", "351", "352", "353", "354", "358",
    "370", "371", "372", "380", "420", "421", "852", "855", "880", "886", "966", "971",
    "972", "974", "977", "994", "995",
  )

  private fun normalizeDialPhone(raw: String): String? {
    val trimmed = raw.trim()
    val hasPlus = trimmed.startsWith("+")
    val digits = trimmed.filter { it.isDigit() }
    if (hasPlus) {
      if (digits.length !in 7..15) return null
      return "+$digits"
    }
    // Local Kenyan format 07XX / 01XX or standard 10-digit local format
    if (digits.length == 10 && digits.startsWith("0")) {
      return digits
    }
    if (digits.length == 10) {
      return digits
    }
    return null
  }

  private fun isPlausibleOtpContext(text: String, start: Int, end: Int, digits: String): Boolean {
    if (digits.length !in 4..8) return false
    // Must not sit inside a larger digit string, decimal (e.g. 12.3456), date (e.g. 12/2026), or hyphenated phone
    val prevDecimalOrSlash = start >= 2 && (text[start - 1] == '.' || text[start - 1] == '/') && text[start - 2].isDigit()
    val nextDecimalOrSlash = end + 1 < text.length && (text[end] == '.' || text[end] == '/') && text[end + 1].isDigit()
    if (start > 0 && (text[start - 1].isDigit() || prevDecimalOrSlash)) return false
    if (end < text.length && (text[end].isDigit() || nextDecimalOrSlash || text[end] == '-')) return false

    val prefixWindow = text.substring((start - 20).coerceAtLeast(0), start)
    if (currencyPrefixRegex.containsMatchIn(prefixWindow)) return false

    // Reject if immediately followed by a phone-like chunk ("712 345 678")
    val afterWindow = text.substring(end, (end + 6).coerceAtMost(text.length))
    if (Regex("""^\s+\d{3}[\s\-]\d{3,4}\b""").containsMatchIn(afterWindow)) return false
    return true
  }

  private fun looksLikePriceOrDateFragment(text: String, start: Int, end: Int): Boolean {
    val before = text.substring((start - 12).coerceAtLeast(0), start)
    if (currencyPrefixRegex.containsMatchIn(before)) return true
    if (start > 0 && (text[start - 1] == '.' || text[start - 1] == '/')) return true
    if (end < text.length && text[end] == '/') return true
    return false
  }

  private fun trimTrailingSentencePunctuation(raw: String): String {
    var s = raw
    while (s.isNotEmpty() && s.last() in charArrayOf('.', ',', '!', '?', ':', ';', '\'', '"', '…', ')', ']', '}')) {
      s = s.dropLast(1)
    }
    return s
  }

  private fun trimUrlTrailingPunctuation(raw: String): String {
    var s = raw
    while (s.isNotEmpty()) {
      val last = s.last()
      when {
        last in charArrayOf('.', ',', '!', '?', ':', ';', '\'', '"', '…') -> s = s.dropLast(1)
        last == ')' && s.count { it == ')' } > s.count { it == '(' } -> s = s.dropLast(1)
        last == ']' && s.count { it == ']' } > s.count { it == '[' } -> s = s.dropLast(1)
        last == '}' && s.count { it == '}' } > s.count { it == '{' } -> s = s.dropLast(1)
        else -> break
      }
    }
    return s
  }

  private fun isForbiddenHost(host: String): Boolean {
    return host == "localhost" ||
      host.endsWith(".localhost") ||
      host.endsWith(".local") ||
      host.endsWith(".internal") ||
      host.endsWith(".localdomain") ||
      host.endsWith(".home.arpa") ||
      host == "metadata.google.internal" ||
      host == "metadata"
  }

  private fun isPublicIPv4Literal(host: String): Boolean {
    val parts = host.split('.').mapNotNull { it.toIntOrNull() }
    if (parts.size != 4 || parts.any { it !in 0..255 }) return false
    val a = parts[0]
    val b = parts[1]
    if (a == 0 || a == 10 || a == 127) return false
    if (a == 169 && b == 254) return false
    if (a == 172 && b in 16..31) return false
    if (a == 192 && b == 168) return false
    if (a == 100 && b in 64..127) return false
    if (a >= 224) return false
    return true
  }
}
