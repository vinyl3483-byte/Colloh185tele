package telefam.company.core

import java.security.MessageDigest
import java.util.Locale

enum class NetworkTier {
  OFFLINE,
  WEAK,
  GOOD,
}

data class OtpRateDecision(
  val allowed: Boolean,
  val cooldownSeconds: Long,
  val tierIndex: Int,
)

object TelefamCoreEngine {
  // Escalating cooldown schedule:
  // Sends 1, 2, 3 -> 1 minute (60s)
  // Send 4        -> 15 minutes (900s)
  // Send 5        -> 1 hour (3600s)
  // Send 6+       -> 1 day (86400s)
  private val ESCALATING_SCHEDULE_SECONDS = longArrayOf(
    60L,
    60L,
    60L,
    900L,
    3600L,
    86400L,
  )

  const val MAX_ACCOUNT_REQUESTS_24H = 12
  const val MAX_DEVICE_REQUESTS_24H = 20
  const val MAX_IP_REQUESTS_24H = 40
  const val MAX_VERIFY_ATTEMPTS_15M = 15
  const val WINDOW_24H_MS = 86_400_000L
  const val WINDOW_15M_MS = 900_000L

  fun cooldownSecondsForSendCount(sendCount: Int): Long {
    if (sendCount <= 0) return ESCALATING_SCHEDULE_SECONDS[0]
    val index = (sendCount - 1).coerceAtMost(ESCALATING_SCHEDULE_SECONDS.lastIndex)
    return ESCALATING_SCHEDULE_SECONDS[index]
  }

  fun computeDeviceFingerprint(
    platform: String,
    model: String,
    osVersion: String,
    installId: String,
  ): String {
    if (NativeTelefamBridge.isNativeLoaded) {
      return NativeTelefamBridge.nativeDeviceFingerprint(platform, model, osVersion, installId)
    }
    val payload = listOf(platform, model, osVersion, installId)
      .joinToString("|", prefix = "tf_dev|") { it.trim().lowercase(Locale.US) }
    return sha256Hex(payload)
  }

  fun computeIpFingerprint(ipAddress: String): String {
    if (NativeTelefamBridge.isNativeLoaded) {
      return NativeTelefamBridge.nativeIpFingerprint(ipAddress)
    }
    val clean = ipAddress.trim().lowercase(Locale.US).ifEmpty { "offline" }
    return sha256Hex("tf_ip|$clean")
  }

  fun hashAccountEmail(email: String): String {
    val clean = email.trim().lowercase(Locale.US)
    return sha256Hex("tf_acct|$clean")
  }

  fun sanitizeServerError(httpStatus: Int, rawBody: String? = null): String {
    if (NativeTelefamBridge.isNativeLoaded) {
      return NativeTelefamBridge.nativeSanitizeError(httpStatus, rawBody.orEmpty())
    }
    return if (httpStatus == 429) {
      "An error has occurred. Please try again later."
    } else {
      "An error has occurred. Please try again."
    }
  }

  fun computeBackoffMs(retryCount: Int, networkTier: NetworkTier): Long {
    val clamped = retryCount.coerceIn(0, 7)
    val baseMs = if (networkTier == NetworkTier.WEAK) 2500L else 1000L
    return (baseMs shl clamped).coerceAtMost(120_000L)
  }

  fun maxBatchSizeForNetwork(networkTier: NetworkTier): Int = when (networkTier) {
    NetworkTier.OFFLINE -> 0
    NetworkTier.WEAK -> 3
    NetworkTier.GOOD -> 20
  }

  fun formatCountdown(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val hours = safe / 3600L
    val mins = (safe % 3600L) / 60L
    val secs = safe % 60L
    return if (hours > 0L) {
      String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
    } else {
      String.format(Locale.US, "%02d:%02d", mins, secs)
    }
  }

  private fun sha256Hex(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
  }
}
