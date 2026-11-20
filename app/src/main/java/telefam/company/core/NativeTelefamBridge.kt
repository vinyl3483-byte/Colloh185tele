package telefam.company.core

/**
 * JNI bridge into cpp/telefam/company/bridge/TelefamJniBridge.cpp.
 * Falls back cleanly to the deterministic Kotlin core engine when native NDK
 * shared libraries are not packaged in the current build environment.
 */
object NativeTelefamBridge {
  val isNativeLoaded: Boolean = runCatching {
    System.loadLibrary("telefam_jni")
    true
  }.getOrDefault(false)

  external fun nativeDeviceFingerprint(
    platform: String,
    model: String,
    osVersion: String,
    installId: String,
  ): String

  external fun nativeIpFingerprint(ipAddress: String): String

  external fun nativeEvaluateOtpSend(
    email: String,
    deviceFp: String,
    ipFp: String,
    nowEpochSeconds: Long,
  ): Long

  external fun nativeSanitizeError(httpStatus: Int, rawBody: String): String

  external fun nativeComputeVideoContentId(
    joinedFrameHashes: String,
    durationMs: Long,
    trimStartMs: Long,
    trimEndMs: Long,
  ): String
}
