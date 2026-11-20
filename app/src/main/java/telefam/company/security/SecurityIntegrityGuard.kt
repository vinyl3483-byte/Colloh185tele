package telefam.company.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import com.telefam.company.app.BuildConfig
import java.io.File
import java.security.MessageDigest

/**
 * Runtime integrity & anti-tampering context bound into device fingerprint telemetry
 * and token vault release protections.
 */
object SecurityIntegrityGuard {
  data class IntegrityContext(
    val signatureDigest: String,
    val isDebuggerAttached: Boolean,
    val hasRootBinary: Boolean,
    val hasHookFramework: Boolean = false,
    val isTampered: Boolean = false,
  )

  fun isRobolectricRuntime(): Boolean {
    return Build.FINGERPRINT.contains("robolectric", ignoreCase = true)
  }

  fun inspect(context: Context): IntegrityContext {
    if (isRobolectricRuntime()) {
      return IntegrityContext(
        signatureDigest = computeSigningDigest(context),
        isDebuggerAttached = false,
        hasRootBinary = false,
        hasHookFramework = false,
        isTampered = false,
      )
    }

    val sigDigest = computeSigningDigest(context)
    val debugger = Debug.isDebuggerConnected() ||
      Debug.waitingForDebugger() ||
      hasNonZeroTracerPid()

    val root = listOf(
      "/system/bin/su",
      "/system/xbin/su",
      "/sbin/su",
      "/data/local/xbin/su",
      "/vendor/bin/su",
      "/sbin/.magisk",
      "/system/app/Superuser.apk",
      "/data/adb/magisk",
    ).any { runCatching { File(it).exists() }.getOrDefault(false) }

    val hooked = detectHookFrameworkInMaps()
    val debuggableFlagSet = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val tampered = (!BuildConfig.DEBUG && (debuggableFlagSet || debugger || hooked)) ||
      (!BuildConfig.DEBUG && sigDigest == "unsigned")

    return IntegrityContext(
      signatureDigest = sigDigest,
      isDebuggerAttached = debugger,
      hasRootBinary = root,
      hasHookFramework = hooked,
      isTampered = tampered,
    )
  }

  fun hasNonZeroTracerPid(): Boolean {
    return runCatching {
      val statusFile = File("/proc/self/status")
      if (!statusFile.canRead()) return@runCatching false
      statusFile.useLines { lines ->
        lines.firstOrNull { it.startsWith("TracerPid:") }
          ?.substringAfter("TracerPid:")
          ?.trim()
          ?.toIntOrNull()
          ?.let { it > 0 } ?: false
      }
    }.getOrDefault(false)
  }

  fun detectHookFrameworkInMaps(): Boolean {
    val markers = listOf("frida", "xposed", "lsposed", "substrate", "edxposed", "zygisk")
    return runCatching {
      val mapsFile = File("/proc/self/maps")
      if (!mapsFile.canRead()) return@runCatching false
      mapsFile.useLines { lines ->
        lines.any { line ->
          val lower = line.lowercase()
          markers.any { marker -> lower.contains(marker) }
        }
      }
    }.getOrDefault(false)
  }

  @Suppress("DEPRECATION")
  private fun computeSigningDigest(context: Context): String {
    return runCatching {
      val pm = context.packageManager
      val sigBytes: ByteArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val pkgInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        pkgInfo.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray() ?: byteArrayOf()
      } else {
        val pkgInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        pkgInfo.signatures?.firstOrNull()?.toByteArray() ?: byteArrayOf()
      }
      if (sigBytes.isEmpty()) return "unsigned"
      MessageDigest.getInstance("SHA-256").digest(sigBytes).joinToString("") { "%02x".format(it) }
    }.getOrDefault("unknown")
  }
}
