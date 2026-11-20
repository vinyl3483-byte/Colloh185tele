package telefam.company.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import telefam.company.core.NetworkTier
import java.net.Inet4Address

class NetworkQualityMonitor(context: Context) {
  private val connectivityManager =
    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

  private val _networkTier = MutableStateFlow(evaluateCurrentNetworkTier())
  val networkTier: StateFlow<NetworkTier> = _networkTier.asStateFlow()

  private val callback = object : ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
      _networkTier.value = evaluateCurrentNetworkTier()
    }

    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
      _networkTier.value = evaluateCapabilities(networkCapabilities)
    }

    override fun onLost(network: Network) {
      _networkTier.value = evaluateCurrentNetworkTier()
    }
  }

  init {
    runCatching {
      connectivityManager.registerDefaultNetworkCallback(callback)
    }
  }

  fun refreshStatus() {
    _networkTier.value = evaluateCurrentNetworkTier()
  }

  fun currentIpAddressHint(): String {
    return runCatching {
      val active = connectivityManager.activeNetwork ?: return "offline"
      val linkProps = connectivityManager.getLinkProperties(active) ?: return "unknown"
      val ipv4 = linkProps.linkAddresses
        .map { it.address }
        .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
      ipv4?.hostAddress
        ?: linkProps.linkAddresses.firstOrNull()?.address?.hostAddress
        ?: "unknown"
    }.getOrDefault("unknown")
  }

  fun isWifiOrUnmetered(): Boolean {
    return runCatching {
      val active = connectivityManager.activeNetwork ?: return false
      val caps = connectivityManager.getNetworkCapabilities(active) ?: return false
      caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }.getOrDefault(false)
  }

  private fun evaluateCurrentNetworkTier(): NetworkTier {
    return runCatching {
      val active = connectivityManager.activeNetwork
      if (active != null) {
        val caps = connectivityManager.getNetworkCapabilities(active)
        if (caps != null) {
          return evaluateCapabilities(caps)
        }
      }
      @Suppress("DEPRECATION")
      val legacyConnected = connectivityManager.activeNetworkInfo?.isConnected == true
      if (legacyConnected) NetworkTier.WEAK else NetworkTier.OFFLINE
    }.getOrDefault(NetworkTier.OFFLINE)
  }

  private fun evaluateCapabilities(caps: NetworkCapabilities): NetworkTier {
    val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    if (!hasInternet) return NetworkTier.OFFLINE
    val isValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    val downstreamKbps = caps.linkDownstreamBandwidthKbps
    val isWeakBandwidth = downstreamKbps in 1..600
    val isCellularMetered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) &&
      caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    return when {
      !isValidated -> NetworkTier.WEAK
      isWeakBandwidth -> NetworkTier.WEAK
      isCellularMetered && downstreamKbps < 1500 -> NetworkTier.WEAK
      else -> NetworkTier.GOOD
    }
  }
}
