package telefam.company.core

import java.util.Calendar
import java.util.Locale

object TelefamProfileSanitizer {
  private val usernameRegex = Regex("^[a-z0-9_]{3,24}$")
  private val htmlTagRegex = Regex("<[^>]*>")

  fun sanitizePlainText(raw: String, maxLen: Int, singleLine: Boolean = true): String {
    val stripped = htmlTagRegex.replace(raw, "")
    val sb = StringBuilder(stripped.length)
    for (ch in stripped) {
      if (ch == '\u0000' || ch == '\u200B' || ch == '\u200C' || ch == '\u200D' || ch == '\uFEFF') {
        continue
      }
      if (ch.isISOControl()) {
        if (!singleLine && ch == '\n') sb.append(' ')
        continue
      }
      sb.append(ch)
      if (maxLen > 0 && sb.length >= maxLen) break
    }
    return sb.toString().trim()
  }

  fun sanitizeUsernameInput(raw: String): String {
    val lower = raw.lowercase(Locale.US).trimStart('@')
    return lower.filter { (it in 'a'..'z') || (it in '0'..'9') || it == '_' }.take(24)
  }

  fun isValidUsername(username: String): Boolean = usernameRegex.matches(username)

  fun autofillFromEmail(email: String): Pair<String, String> {
    val prefix = email.trim().substringBefore("@").ifBlank { "telefam" }
    val words = prefix.split('.', '_', '-', '+')
      .filter { it.isNotBlank() }
      .map { token ->
        token.lowercase(Locale.US).replaceFirstChar { it.titlecase(Locale.US) }
      }
    val fullName = words.joinToString(" ").ifBlank { "Telefam Member" }.take(64)

    var username = prefix.lowercase(Locale.US)
      .replace('.', '_')
      .replace('-', '_')
      .filter { (it in 'a'..'z') || (it in '0'..'9') || it == '_' }
      .trim('_')
    while (username.length < 3) username += "0"
    return fullName to username.take(24)
  }

  fun isAtLeast13YearsOld(birthYear: Int, birthMonthZeroBased: Int, birthDay: Int): Boolean {
    val now = Calendar.getInstance()
    val currentYear = now.get(Calendar.YEAR)
    val currentMonth = now.get(Calendar.MONTH)
    val currentDay = now.get(Calendar.DAY_OF_MONTH)
    if (birthYear < 1900 || birthYear > currentYear) return false
    var age = currentYear - birthYear
    if (currentMonth < birthMonthZeroBased || (currentMonth == birthMonthZeroBased && currentDay < birthDay)) {
      age -= 1
    }
    return age >= 13
  }
}
