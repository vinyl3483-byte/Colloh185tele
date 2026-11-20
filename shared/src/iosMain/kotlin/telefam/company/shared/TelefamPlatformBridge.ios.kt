package telefam.company.shared

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.Foundation.NSUUID
import platform.UIKit.UIDevice
import telefam.company.cinterop.telefam_compute_device_fingerprint
import telefam.company.cinterop.telefam_sanitize_error

actual class PlatformBridge actual constructor() {
  actual val platform: TelefamPlatform = TelefamPlatform.IOS
  actual val supportsAppleSignIn: Boolean = true

  @OptIn(ExperimentalForeignApi::class)
  actual fun computeDeviceFingerprint(): String {
    val device = UIDevice.currentDevice
    val vendorId = device.identifierForVendor?.UUIDString ?: NSUUID().UUIDString
    val buf = ByteArray(128)
    buf.usePinned { pinned ->
      telefam_compute_device_fingerprint(
        platform = "ios",
        model = device.model,
        os_version = device.systemVersion,
        install_id = vendorId,
        out_buf = pinned.addressOf(0),
        out_buf_len = buf.size.toULong()
      )
    }
    return buf.decodeToString().trimEnd('\u0000')
  }

  actual fun currentIpFingerprintHint(): String = "ios-nwpath-hint"

  actual fun currentNetworkTier(): NetworkTier = NetworkTier.GOOD

  @OptIn(ExperimentalForeignApi::class)
  actual fun sanitizeError(httpStatus: Int, rawBody: String?): String {
    val buf = ByteArray(128)
    buf.usePinned { pinned ->
      telefam_sanitize_error(
        http_status = httpStatus,
        raw_body = rawBody ?: "",
        out_buf = pinned.addressOf(0),
        out_buf_len = buf.size.toULong()
      )
    }
    return buf.decodeToString().trimEnd('\u0000')
  }
}
