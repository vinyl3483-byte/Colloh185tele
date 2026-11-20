package telefam.company.security

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import telefam.company.data.model.CountryCatalog
import java.util.Locale
import java.util.TimeZone
import kotlin.math.round

data class DeviceContextSnapshot(
  val deviceName: String,
  val platform: String = "android",
  val hardwareModel: String,
  val carrierCountryIso: String,
  val simCountryIso: String,
  val gpsCountryIso: String,
  val gpsCity: String,
  val localeCountryIso: String,
  val timezone: String,
  val locationHint: String,
  val latBucket: Double = 0.0,
  val lonBucket: Double = 0.0,
  val signalSource: String = "hardware",
  val capturedAtMs: Long = System.currentTimeMillis(),
)

object DeviceContextCollector {

  private val isoToNameMap: Map<String, String> by lazy {
    CountryCatalog.allCountries.associate { it.iso.uppercase() to it.name }
  }

  fun formatRealDeviceName(): String {
    val man = Build.MANUFACTURER?.trim().orEmpty()
    val model = Build.MODEL?.trim().orEmpty()
    val combined = when {
      model.isBlank() && man.isBlank() -> "Android Device"
      model.isBlank() -> titleCaseWord(man)
      man.isBlank() -> model
      model.lowercase().startsWith(man.lowercase()) -> model
      else -> "${titleCaseWord(man)} $model"
    }
    return combined.take(80)
  }

  @SuppressLint("MissingPermission")
  fun collectSnapshot(context: Context): DeviceContextSnapshot {
    val deviceName = formatRealDeviceName()
    val hardwareModel = Build.MODEL?.trim().orEmpty().ifBlank { "Android" }.take(64)
    val localeCountry = sanitizeIso(Locale.getDefault().country)
    val timezone = TimeZone.getDefault().id.orEmpty().take(64)

    val (carrierIso, simIso) = readTelephonyCountries(context)
    val (gpsIso, gpsCity, latBucket, lonBucket, gpsUsed) = readPassiveCoarseLocation(context)

    val effectiveIso = listOf(gpsIso, carrierIso, simIso, localeCountry)
      .firstOrNull { it.isNotBlank() }.orEmpty()

    val countryName = isoToNameMap[effectiveIso] ?: ""
    val locationHint = when {
      gpsCity.isNotBlank() && countryName.isNotBlank() -> "$gpsCity, $countryName"
      gpsCity.isNotBlank() -> gpsCity
      countryName.isNotBlank() -> countryName
      else -> ""
    }.take(96)

    val signalSource = when {
      gpsUsed && gpsIso.isNotBlank() -> "gps_passive"
      carrierIso.isNotBlank() -> "carrier"
      simIso.isNotBlank() -> "sim"
      localeCountry.isNotBlank() -> "locale"
      else -> "hardware"
    }

    return DeviceContextSnapshot(
      deviceName = deviceName,
      platform = "android",
      hardwareModel = hardwareModel,
      carrierCountryIso = carrierIso,
      simCountryIso = simIso,
      gpsCountryIso = gpsIso,
      gpsCity = gpsCity,
      localeCountryIso = localeCountry,
      timezone = timezone,
      locationHint = locationHint,
      latBucket = latBucket,
      lonBucket = lonBucket,
      signalSource = signalSource,
      capturedAtMs = System.currentTimeMillis(),
    )
  }

  fun toHttpHeaders(snapshot: DeviceContextSnapshot): Map<String, String> {
    val map = linkedMapOf<String, String>()
    if (snapshot.deviceName.isNotBlank()) map["X-Telefam-Device-Name"] = snapshot.deviceName
    map["X-Telefam-Platform"] = snapshot.platform
    if (snapshot.locationHint.isNotBlank()) map["X-Telefam-Location-Hint"] = snapshot.locationHint
    if (snapshot.carrierCountryIso.isNotBlank()) map["X-Telefam-Carrier-ISO"] = snapshot.carrierCountryIso
    if (snapshot.simCountryIso.isNotBlank()) map["X-Telefam-Sim-ISO"] = snapshot.simCountryIso
    if (snapshot.gpsCountryIso.isNotBlank()) map["X-Telefam-Gps-Country"] = snapshot.gpsCountryIso
    if (snapshot.localeCountryIso.isNotBlank()) map["X-Telefam-Locale-Country"] = snapshot.localeCountryIso
    return map
  }

  fun registerLocationModeReceiver(
    context: Context,
    onLocationEnabledOrChanged: () -> Unit,
  ): BroadcastReceiver? {
    return runCatching {
      val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
          if (intent?.action == LocationManager.MODE_CHANGED_ACTION ||
            intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION
          ) {
            onLocationEnabledOrChanged()
          }
        }
      }
      val filter = IntentFilter().apply {
        addAction(LocationManager.MODE_CHANGED_ACTION)
        addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
      }
      ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
      receiver
    }.getOrNull()
  }

  private fun readTelephonyCountries(context: Context): Pair<String, String> {
    return runCatching {
      val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
      val netIso = sanitizeIso(tm?.networkCountryIso)
      val simIso = sanitizeIso(tm?.simCountryIso)
      netIso to simIso
    }.getOrDefault("" to "")
  }

  private data class PassiveGeoResult(
    val countryIso: String,
    val city: String,
    val latBucket: Double,
    val lonBucket: Double,
    val resolved: Boolean,
  )

  @SuppressLint("MissingPermission")
  private fun readPassiveCoarseLocation(context: Context): PassiveGeoResult {
    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!hasCoarse && !hasFine) {
      return PassiveGeoResult("", "", 0.0, 0.0, false)
    }

    return runCatching {
      val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return PassiveGeoResult("", "", 0.0, 0.0, false)
      val enabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        lm.isLocationEnabled
      } else {
        true
      }
      if (!enabled) {
        return PassiveGeoResult("", "", 0.0, 0.0, false)
      }

      val providers = listOf(
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER,
        LocationManager.GPS_PROVIDER,
      )
      var bestLoc: Location? = null
      for (provider in providers) {
        val loc = runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
        if (loc != null && (bestLoc == null || loc.time > bestLoc.time)) {
          bestLoc = loc
        }
      }
      val chosen = bestLoc ?: return PassiveGeoResult("", "", 0.0, 0.0, false)
      // Bucket to ~11km (0.1 degree) for privacy minimization
      val latBucket = round(chosen.latitude * 10.0) / 10.0
      val lonBucket = round(chosen.longitude * 10.0) / 10.0

      var iso = ""
      var city = ""
      if (Geocoder.isPresent()) {
        @Suppress("DEPRECATION")
        val addrs = runCatching {
          Geocoder(context, Locale.ENGLISH).getFromLocation(chosen.latitude, chosen.longitude, 1)
        }.getOrNull()
        val first = addrs?.firstOrNull()
        if (first != null) {
          iso = sanitizeIso(first.countryCode)
          city = (first.locality ?: first.adminArea ?: "").trim().take(48)
        }
      }
      PassiveGeoResult(iso, city, latBucket, lonBucket, true)
    }.getOrDefault(PassiveGeoResult("", "", 0.0, 0.0, false))
  }

  private fun sanitizeIso(raw: String?): String {
    val clean = raw?.trim()?.uppercase().orEmpty()
    if (clean.length != 2 || clean == "XX" || clean == "ZZ") return ""
    return if (clean.all { it in 'A'..'Z' }) clean else ""
  }

  private fun titleCaseWord(word: String): String {
    val trimmed = word.trim()
    if (trimmed.isEmpty()) return ""
    return trimmed.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
  }
}
