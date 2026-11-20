package telefam.company.shared

enum class TelefamPlatform {
  ANDROID,
  IOS,
  DESKTOP
}

enum class NetworkTier {
  OFFLINE,
  WEAK,
  GOOD
}

enum class SocialAuthProvider {
  GOOGLE,
  APPLE
}

expect class PlatformBridge() {
  val platform: TelefamPlatform
  val supportsAppleSignIn: Boolean
  fun computeDeviceFingerprint(): String
  fun currentIpFingerprintHint(): String
  fun currentNetworkTier(): NetworkTier
  fun sanitizeError(httpStatus: Int, rawBody: String?): String
}

object TelefamCooldownSchedule {
  // 1 minute x3 -> 15 minutes -> 1 hour -> 1 day
  private val scheduleSeconds = longArrayOf(60L, 60L, 60L, 900L, 3600L, 86400L)

  fun secondsForSendCount(sendCount: Int): Long {
    if (sendCount <= 0) return scheduleSeconds[0]
    val idx = (sendCount - 1).coerceAtMost(scheduleSeconds.lastIndex)
    return scheduleSeconds[idx]
  }
}
