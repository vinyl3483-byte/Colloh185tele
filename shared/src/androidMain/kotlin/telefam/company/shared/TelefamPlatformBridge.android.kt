package telefam.company.shared

import java.security.MessageDigest

actual class PlatformBridge actual constructor() {
  actual val platform: TelefamPlatform = TelefamPlatform.ANDROID
  actual val supportsAppleSignIn: Boolean = false

  actual fun computeDeviceFingerprint(): String {
    val raw = "android|${android.os.Build.MANUFACTURER}|${android.os.Build.MODEL}|${android.os.Build.VERSION.RELEASE}"
    val digest = MessageDigest.getInstance("SHA-256").digest(raw.lowercase().toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
  }

  actual fun currentIpFingerprintHint(): String = "android-net-hint"

  actual fun currentNetworkTier(): NetworkTier = NetworkTier.GOOD

  actual fun sanitizeError(httpStatus: Int, rawBody: String?): String {
    return if (httpStatus == 429) {
      "An error has occurred. Please try again later."
    } else {
      "An error has occurred. Please try again."
    }
  }
}
