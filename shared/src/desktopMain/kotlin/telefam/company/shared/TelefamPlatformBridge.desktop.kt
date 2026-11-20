package telefam.company.shared

import java.net.InetAddress
import java.security.MessageDigest

actual class PlatformBridge actual constructor() {
  actual val platform: TelefamPlatform = TelefamPlatform.DESKTOP
  actual val supportsAppleSignIn: Boolean = false

  actual fun computeDeviceFingerprint(): String {
    val raw = "desktop|${System.getProperty("os.name")}|${System.getProperty("os.arch")}|${System.getProperty("user.name")}"
    val digest = MessageDigest.getInstance("SHA-256").digest(raw.lowercase().toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
  }

  actual fun currentIpFingerprintHint(): String {
    return runCatching {
      val host = InetAddress.getLocalHost().hostAddress ?: "loopback"
      MessageDigest.getInstance("SHA-256").digest(host.toByteArray())
        .joinToString("") { "%02x".format(it) }
    }.getOrDefault("desktop-offline")
  }

  actual fun currentNetworkTier(): NetworkTier = NetworkTier.GOOD

  actual fun sanitizeError(httpStatus: Int, rawBody: String?): String {
    return if (httpStatus == 429) {
      "An error has occurred. Please try again later."
    } else {
      "An error has occurred. Please try again."
    }
  }
}
