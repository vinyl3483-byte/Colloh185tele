package telefam.company.desktop

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToLong
import kotlin.math.sin

enum class DesktopStarProvider(val wire: String, val label: String) {
  PAYSTACK("paystack", "Paystack"),
  PAYPAL("paypal", "PayPal"),
}

enum class DesktopStarsRoute {
  HUB,
  SEND_STARS,
  BUY_STARS,
  CHECKOUT,
}

data class DesktopStarWallet(
  val balanceStars: Long = 0L,
  val starsReceivedTotal: Long = 0L,
  val starsSentTotal: Long = 0L,
  val updatedAtMs: Long = 0L,
)

data class DesktopStarSenderSummary(
  val senderUserId: String,
  val senderName: String,
  val senderUsername: String,
  val senderAvatarUrl: String,
  val totalStars: Long,
  val giftCount: Long,
  val lastSentAtMs: Long,
)

data class DesktopStarLedgerItem(
  val entryId: String,
  val entryType: String,
  val deltaStars: Long,
  val counterpartyUserId: String = "",
  val counterpartyName: String = "",
  val counterpartyUsername: String = "",
  val note: String = "",
  val createdAtMs: Long = 0L,
)

data class DesktopStarPayment(
  val paymentId: String,
  val provider: String,
  val status: String,
  val packageStars: Long,
  val currencyCode: String,
  val displayAmount: String,
  val checkoutUrl: String,
  val providerReference: String,
  val providerOrderId: String,
)

data class DesktopStarPeerTarget(
  val peerUserId: String,
  val peerName: String,
  val peerUsername: String,
  val peerAvatarUrl: String,
  val isOnline: Boolean,
  val conversationId: String,
)

data class DesktopStarGiftMeta(
  val stars: Long,
  val senderUserId: String,
  val senderName: String,
  val recipientUserId: String,
  val recipientName: String,
  val animationTier: Int,
  val ledgerEntryId: String,
)

data class DesktopStarsUiState(
  val isOpen: Boolean = false,
  val routeStack: List<DesktopStarsRoute> = listOf(DesktopStarsRoute.HUB),
  val countryIso: String = "US",
  val wallet: DesktopStarWallet = DesktopStarWallet(),
  val senders: List<DesktopStarSenderSummary> = emptyList(),
  val history: List<DesktopStarLedgerItem> = emptyList(),
  val hubTabIndex: Int = 0,
  val targetPeer: DesktopStarPeerTarget? = null,
  val pendingReturnSendStarsAmount: Long? = null,
  val selectedPackageStars: Long = 100L,
  val selectedProvider: DesktopStarProvider = DesktopStarProvider.PAYSTACK,
  val activePayment: DesktopStarPayment? = null,
  val awaitingBrowserReturn: Boolean = false,
  val activeVerifyIdempKey: String? = null,
  val isLoadingSummary: Boolean = false,
  val isSendingStars: Boolean = false,
  val isInitiatingCheckout: Boolean = false,
  val isVerifyingPayment: Boolean = false,
  val celebrationBurstStars: Long? = null,
  val statusMessage: String? = null,
  val errorMessage: String? = null,
) {
  val currentRoute: DesktopStarsRoute?
    get() = if (!isOpen) null else routeStack.lastOrNull() ?: DesktopStarsRoute.HUB
}

data class DesktopCountryCurrencySpec(
  val countryIso: String,
  val currencyCode: String,
  val symbol: String,
  val ratePerStar: Double,
  val zeroDecimal: Boolean = false,
)

object DesktopStarsCatalog {
  val presetPackages: List<Long> = listOf(
    5L, 10L, 20L, 50L, 100L, 200L, 500L, 1_000L, 2_000L, 5_000L, 10_000L, 50_000L,
  )

  private val specs: Map<String, DesktopCountryCurrencySpec> = mapOf(
    "KE" to DesktopCountryCurrencySpec("KE", "KES", "Ksh ", 1.0, true),
    "NG" to DesktopCountryCurrencySpec("NG", "NGN", "\u20A6", 12.0, true),
    "GH" to DesktopCountryCurrencySpec("GH", "GHS", "GH\u20B5 ", 0.12, false),
    "ZA" to DesktopCountryCurrencySpec("ZA", "ZAR", "R ", 0.15, false),
    "UG" to DesktopCountryCurrencySpec("UG", "UGX", "UGX ", 30.0, true),
    "TZ" to DesktopCountryCurrencySpec("TZ", "TZS", "TZS ", 20.0, true),
    "RW" to DesktopCountryCurrencySpec("RW", "RWF", "RWF ", 10.0, true),
    "GB" to DesktopCountryCurrencySpec("GB", "GBP", "\u00A3", 0.0064, false),
    "DE" to DesktopCountryCurrencySpec("DE", "EUR", "\u20AC", 0.0074, false),
    "FR" to DesktopCountryCurrencySpec("FR", "EUR", "\u20AC", 0.0074, false),
    "ES" to DesktopCountryCurrencySpec("ES", "EUR", "\u20AC", 0.0074, false),
    "IT" to DesktopCountryCurrencySpec("IT", "EUR", "\u20AC", 0.0074, false),
    "NL" to DesktopCountryCurrencySpec("NL", "EUR", "\u20AC", 0.0074, false),
    "CA" to DesktopCountryCurrencySpec("CA", "CAD", "CA$", 0.011, false),
    "AU" to DesktopCountryCurrencySpec("AU", "AUD", "A$", 0.012, false),
    "IN" to DesktopCountryCurrencySpec("IN", "INR", "\u20B9", 0.68, false),
    "US" to DesktopCountryCurrencySpec("US", "USD", "$", 0.008, false),
  )

  fun resolveCountryIso(locationHint: String = ""): String {
    val lower = locationHint.lowercase(Locale.US)
    if (lower.contains("kenya") || lower.contains("nairobi") || lower.contains("mombasa")) return "KE"
    if (lower.contains("nigeria") || lower.contains("lagos") || lower.contains("abuja")) return "NG"
    if (lower.contains("ghana") || lower.contains("accra")) return "GH"
    if (lower.contains("south africa") || lower.contains("johannesburg")) return "ZA"
    if (lower.contains("united kingdom") || lower.contains("london")) return "GB"
    val localeIso = Locale.getDefault().country.trim().uppercase(Locale.US)
    return if (localeIso.length == 2 && localeIso.all { it in 'A'..'Z' }) localeIso else "US"
  }

  fun specForCountry(countryIso: String): DesktopCountryCurrencySpec =
    specs[countryIso.trim().uppercase(Locale.US)] ?: specs.getValue("US")

  fun formatCurrencyForStars(stars: Long, countryIso: String): String {
    val spec = specForCountry(countryIso)
    val safe = max(0L, stars)
    if (safe == 0L) {
      return if (spec.zeroDecimal) "${spec.symbol}0" else "${spec.symbol}0.00"
    }
    val amt = safe * spec.ratePerStar
    return if (spec.zeroDecimal || (spec.ratePerStar >= 1.0 && abs(amt - amt.roundToLong()) < 0.001)) {
      "${spec.symbol}${formatCount(amt.roundToLong())}"
    } else {
      String.format(Locale.US, "%s%.2f", spec.symbol, max(0.05, amt))
    }
  }

  fun formatCount(stars: Long): String = NumberFormat.getIntegerInstance(Locale.US).format(stars)

  fun animationTier(stars: Long): Int = when {
    stars >= 10_000L -> 5
    stars >= 1_000L -> 4
    stars >= 200L -> 3
    stars >= 50L -> 2
    else -> 1
  }

  fun decodeB64Json(b64: String): String {
    if (b64.isBlank()) return ""
    return runCatching {
      String(Base64.getDecoder().decode(b64), Charsets.UTF_8)
    }.getOrDefault("")
  }

  fun parseGiftMeta(rawJson: String): DesktopStarGiftMeta? {
    if (rawJson.isBlank()) return null
    val map = runCatching { DesktopJson.parseObject(rawJson) }.getOrNull() ?: return null
    val stars = DesktopJson.long(map, "stars", 0L)
    if (stars <= 0L) return null
    return DesktopStarGiftMeta(
      stars = stars,
      senderUserId = DesktopJson.str(map, "sender_user_id"),
      senderName = DesktopJson.str(map, "sender_name"),
      recipientUserId = DesktopJson.str(map, "recipient_user_id"),
      recipientName = DesktopJson.str(map, "recipient_name"),
      animationTier = DesktopJson.long(map, "animation_tier", animationTier(stars).toLong()).toInt(),
      ledgerEntryId = DesktopJson.str(map, "ledger_entry_id"),
    )
  }

  fun formatSummaryLine(rawJson: String): String {
    val meta = parseGiftMeta(rawJson)
    return if (meta != null) {
      val unit = if (meta.stars == 1L) "Star" else "Stars"
      "Sent ${formatCount(meta.stars)} $unit"
    } else {
      "Sent Stars"
    }
  }
}

class DesktopStarsController(
  private val apiClient: DesktopApiClient,
  private val chatController: DesktopChatController,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val _state = MutableStateFlow(DesktopStarsUiState())
  val state: StateFlow<DesktopStarsUiState> = _state.asStateFlow()

  private var ownerUid: String = ""
  private var lastCheckoutSig: String = ""
  private var lastCheckoutIdempKey: String = ""

  init {
    scope.launch {
      chatController.wsEvents.collect { ev ->
        if (ev.event == "stars.updated") {
          val map = runCatching { DesktopJson.parseObject(ev.rawJson) }.getOrNull()
          if (map != null) {
            val bal = DesktopJson.long(map, "balance_stars", -1L)
            val rec = DesktopJson.long(map, "stars_received", -1L)
            val sent = DesktopJson.long(map, "stars_sent", -1L)
            val delta = DesktopJson.long(map, "delta", 0L)
            val entryType = DesktopJson.str(map, "entry_type")
            _state.update { s ->
              s.copy(
                wallet = s.wallet.copy(
                  balanceStars = if (bal >= 0L) bal else s.wallet.balanceStars,
                  starsReceivedTotal = if (rec >= 0L) rec else s.wallet.starsReceivedTotal,
                  starsSentTotal = if (sent >= 0L) sent else s.wallet.starsSentTotal,
                  updatedAtMs = System.currentTimeMillis(),
                ),
                statusMessage = if (entryType == "received" && delta > 0L) {
                  "Received ${DesktopStarsCatalog.formatCount(delta)} Telefam Stars"
                } else {
                  s.statusMessage
                },
              )
            }
          }
          refreshSummary(silent = true)
        }
      }
    }
  }

  fun attachSession(userId: String) {
    ownerUid = userId.trim()
    if (ownerUid.isNotBlank()) refreshSummary(silent = true)
  }

  fun detachSession() {
    ownerUid = ""
    _state.value = DesktopStarsUiState()
  }

  fun openStarsHub(locationHint: String = "") {
    val iso = DesktopStarsCatalog.resolveCountryIso(locationHint)
    _state.update {
      it.copy(
        isOpen = true,
        routeStack = listOf(DesktopStarsRoute.HUB),
        countryIso = iso,
        targetPeer = null,
        pendingReturnSendStarsAmount = null,
        errorMessage = null,
      )
    }
    refreshSummary(silent = false)
  }

  fun openSendStarsFromChat(conv: DesktopChatConversation, locationHint: String = "") {
    val iso = DesktopStarsCatalog.resolveCountryIso(locationHint)
    val peer = DesktopStarPeerTarget(
      peerUserId = conv.peerUserId,
      peerName = conv.peerFullName.ifBlank { conv.peerUsername.ifBlank { "Telefam" } },
      peerUsername = conv.peerUsername,
      peerAvatarUrl = conv.peerAvatarUrl,
      isOnline = conv.isOnline,
      conversationId = conv.conversationId,
    )
    _state.update { s ->
      val stack = if (s.wallet.balanceStars > 0L || s.isLoadingSummary) {
        listOf(DesktopStarsRoute.SEND_STARS)
      } else {
        listOf(DesktopStarsRoute.SEND_STARS, DesktopStarsRoute.BUY_STARS)
      }
      s.copy(
        isOpen = true,
        routeStack = stack,
        countryIso = iso,
        targetPeer = peer,
        pendingReturnSendStarsAmount = null,
        errorMessage = null,
      )
    }
    scope.launch {
      refreshSummarySync(silent = true)
      val cur = _state.value
      if (cur.isOpen && cur.targetPeer != null && cur.wallet.balanceStars <= 0L &&
        cur.currentRoute == DesktopStarsRoute.SEND_STARS
      ) {
        _state.update {
          it.copy(
            routeStack = listOf(DesktopStarsRoute.SEND_STARS, DesktopStarsRoute.BUY_STARS),
            statusMessage = "Buy Stars to send gifts to ${peer.peerName}",
          )
        }
      }
    }
  }

  fun openBuyStars(suggestedAmount: Long? = null, pendingSendAmount: Long? = null) {
    _state.update { s ->
      val stack = if (s.routeStack.lastOrNull() == DesktopStarsRoute.BUY_STARS) {
        s.routeStack
      } else {
        s.routeStack + DesktopStarsRoute.BUY_STARS
      }
      s.copy(
        isOpen = true,
        routeStack = stack,
        selectedPackageStars = suggestedAmount ?: s.selectedPackageStars,
        pendingReturnSendStarsAmount = pendingSendAmount ?: s.pendingReturnSendStarsAmount,
        errorMessage = null,
      )
    }
  }

  fun selectPackageForCheckout(stars: Long) {
    _state.update { s ->
      val stack = if (s.routeStack.lastOrNull() == DesktopStarsRoute.CHECKOUT) {
        s.routeStack
      } else {
        s.routeStack + DesktopStarsRoute.CHECKOUT
      }
      s.copy(
        routeStack = stack,
        selectedPackageStars = stars,
        awaitingBrowserReturn = false,
        errorMessage = null,
      )
    }
  }

  fun selectProvider(provider: DesktopStarProvider) {
    _state.update { it.copy(selectedProvider = provider, errorMessage = null) }
  }

  fun selectHubTab(index: Int) {
    _state.update { it.copy(hubTabIndex = index.coerceIn(0, 1)) }
  }

  fun navigateBack() {
    _state.update { s ->
      if (s.routeStack.size <= 1) {
        s.copy(
          isOpen = false,
          routeStack = listOf(DesktopStarsRoute.HUB),
          awaitingBrowserReturn = false,
          errorMessage = null,
        )
      } else {
        s.copy(
          routeStack = s.routeStack.dropLast(1),
          awaitingBrowserReturn = false,
          errorMessage = null,
        )
      }
    }
  }

  fun closeAll() {
    _state.update {
      it.copy(
        isOpen = false,
        routeStack = listOf(DesktopStarsRoute.HUB),
        awaitingBrowserReturn = false,
        errorMessage = null,
      )
    }
  }

  fun dismissStatus() {
    _state.update { it.copy(statusMessage = null, errorMessage = null) }
  }

  fun dismissCelebrationBurst() {
    _state.update { it.copy(celebrationBurstStars = null) }
  }

  fun refreshSummary(silent: Boolean = false) {
    scope.launch { refreshSummarySync(silent) }
  }

  private fun refreshSummarySync(silent: Boolean) {
    if (!silent) {
      _state.update { it.copy(isLoadingSummary = true, errorMessage = null) }
    }
    val country = _state.value.countryIso
    val resp = apiClient.executeChatRaw("GET", "/v1/stars/summary?country=${DesktopChatCodec.urlEncode(country)}")
    if (resp != null && resp.first in 200..299) {
      val root = DesktopJson.parseObject(resp.second)
      val wallet = parseWallet(DesktopJson.obj(root, "wallet"))
      val senders = DesktopJson.objList(root, "senders").map { m ->
        DesktopStarSenderSummary(
          senderUserId = DesktopJson.str(m, "sender_user_id"),
          senderName = DesktopJson.str(m, "sender_name"),
          senderUsername = DesktopJson.str(m, "sender_username"),
          senderAvatarUrl = DesktopJson.str(m, "sender_avatar_url"),
          totalStars = DesktopJson.long(m, "total_stars", 0L),
          giftCount = DesktopJson.long(m, "gift_count", 0L),
          lastSentAtMs = DesktopJson.long(m, "last_sent_at_ms", 0L),
        )
      }
      val history = DesktopJson.objList(root, "history").map { m ->
        DesktopStarLedgerItem(
          entryId = DesktopJson.str(m, "entry_id"),
          entryType = DesktopJson.str(m, "entry_type"),
          deltaStars = DesktopJson.long(m, "delta_stars", 0L),
          counterpartyUserId = DesktopJson.str(m, "counterparty_user_id"),
          counterpartyName = DesktopJson.str(m, "counterparty_name"),
          counterpartyUsername = DesktopJson.str(m, "counterparty_username"),
          note = DesktopJson.str(m, "note"),
          createdAtMs = DesktopJson.long(m, "created_at_ms", 0L),
        )
      }
      _state.update {
        it.copy(
          wallet = wallet,
          senders = senders,
          history = history,
          countryIso = DesktopJson.str(root, "country_iso").ifBlank { it.countryIso },
          isLoadingSummary = false,
        )
      }
    } else if (!silent) {
      _state.update { it.copy(isLoadingSummary = false) }
    }
  }

  fun proceedToPay() {
    val snap = _state.value
    if (snap.isInitiatingCheckout) return
    val sig = "${snap.selectedProvider.wire}:${snap.selectedPackageStars}:${snap.countryIso}:${snap.targetPeer?.peerUserId.orEmpty()}"
    val idemp = if (sig == lastCheckoutSig && lastCheckoutIdempKey.isNotBlank()) {
      lastCheckoutIdempKey
    } else {
      UUID.randomUUID().toString().also {
        lastCheckoutSig = sig
        lastCheckoutIdempKey = it
      }
    }
    _state.update { it.copy(isInitiatingCheckout = true, errorMessage = null) }

    scope.launch {
      val purpose = if (snap.targetPeer != null) "send_gift" else "topup"
      val body = buildString {
        append('{')
        append("\"provider\":\"").append(DesktopJson.escape(snap.selectedProvider.wire)).append("\",")
        append("\"package_stars\":").append(snap.selectedPackageStars).append(',')
        append("\"country_iso\":\"").append(DesktopJson.escape(snap.countryIso)).append("\",")
        append("\"idempotency_key\":\"").append(DesktopJson.escape(idemp)).append("\",")
        append("\"purpose\":\"").append(purpose).append("\",")
        append("\"peer_user_id\":\"").append(DesktopJson.escape(snap.targetPeer?.peerUserId.orEmpty())).append("\",")
        append("\"conversation_id\":\"").append(DesktopJson.escape(snap.targetPeer?.conversationId.orEmpty())).append('"')
        append('}')
      }
      val resp = apiClient.executeChatRaw(
        method = "POST",
        path = "/v1/stars/checkout/init",
        jsonBody = body,
        extraHeaders = mapOf("Idempotency-Key" to idemp),
      )
      if (resp != null && resp.first in 200..299) {
        val root = DesktopJson.parseObject(resp.second)
        val pObj = DesktopJson.obj(root, "payment")
        val payment = DesktopStarPayment(
          paymentId = DesktopJson.str(pObj, "payment_id"),
          provider = DesktopJson.str(pObj, "provider"),
          status = DesktopJson.str(pObj, "status"),
          packageStars = DesktopJson.long(pObj, "package_stars", snap.selectedPackageStars),
          currencyCode = DesktopJson.str(pObj, "currency_code"),
          displayAmount = DesktopJson.str(pObj, "display_amount"),
          checkoutUrl = DesktopJson.str(pObj, "checkout_url"),
          providerReference = DesktopJson.str(pObj, "provider_reference"),
          providerOrderId = DesktopJson.str(pObj, "provider_order_id"),
        )
        if (payment.checkoutUrl.isNotBlank()) {
          runCatching {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
              Desktop.getDesktop().browse(URI(payment.checkoutUrl))
            }
          }
          _state.update {
            it.copy(
              isInitiatingCheckout = false,
              activePayment = payment,
              awaitingBrowserReturn = true,
              activeVerifyIdempKey = UUID.randomUUID().toString(),
              wallet = parseWallet(DesktopJson.obj(root, "wallet")),
            )
          }
          return@launch
        }
      }
      _state.update {
        it.copy(
          isInitiatingCheckout = false,
          errorMessage = parseError(resp?.first ?: 0, resp?.second.orEmpty()),
        )
      }
    }
  }

  fun verifyActivePayment() {
    val snap = _state.value
    val active = snap.activePayment ?: return
    if (snap.isVerifyingPayment) return
    val idemp = snap.activeVerifyIdempKey ?: UUID.randomUUID().toString()
    _state.update { it.copy(isVerifyingPayment = true, errorMessage = null) }

    scope.launch {
      val body = buildString {
        append('{')
        append("\"payment_id\":\"").append(DesktopJson.escape(active.paymentId)).append("\",")
        append("\"reference\":\"").append(DesktopJson.escape(active.providerReference)).append("\",")
        append("\"paypal_order_id\":\"").append(DesktopJson.escape(active.providerOrderId)).append("\",")
        append("\"idempotency_key\":\"").append(DesktopJson.escape(idemp)).append('"')
        append('}')
      }
      val resp = apiClient.executeChatRaw(
        method = "POST",
        path = "/v1/stars/checkout/verify",
        jsonBody = body,
        extraHeaders = mapOf("Idempotency-Key" to idemp),
      )
      if (resp != null && resp.first in 200..299) {
        val root = DesktopJson.parseObject(resp.second)
        val wallet = parseWallet(DesktopJson.obj(root, "wallet"))
        val pObj = DesktopJson.obj(root, "payment")
        val pkgStars = DesktopJson.long(pObj, "package_stars", active.packageStars)
        lastCheckoutSig = ""
        lastCheckoutIdempKey = ""
        val pendingSend = _state.value.pendingReturnSendStarsAmount
        val peer = _state.value.targetPeer

        _state.update { s ->
          val nextStack = if (peer != null && s.routeStack.contains(DesktopStarsRoute.SEND_STARS)) {
            listOf(DesktopStarsRoute.SEND_STARS)
          } else {
            listOf(DesktopStarsRoute.HUB)
          }
          s.copy(
            isVerifyingPayment = false,
            awaitingBrowserReturn = false,
            wallet = wallet,
            routeStack = nextStack,
            pendingReturnSendStarsAmount = null,
            statusMessage = "${DesktopStarsCatalog.formatCount(pkgStars)} Stars added to your balance",
          )
        }
        refreshSummarySync(silent = true)
        if (peer != null && pendingSend != null && wallet.balanceStars >= pendingSend) {
          sendStarsTier(pendingSend)
        }
        return@launch
      }
      _state.update {
        it.copy(
          isVerifyingPayment = false,
          errorMessage = parseError(resp?.first ?: 0, resp?.second.orEmpty()),
        )
      }
    }
  }

  fun sendStarsTier(stars: Long) {
    val snap = _state.value
    val peer = snap.targetPeer ?: return
    if (snap.isSendingStars) return

    if (snap.wallet.balanceStars < stars) {
      _state.update { s ->
        val stack = if (s.routeStack.contains(DesktopStarsRoute.BUY_STARS)) {
          s.routeStack
        } else {
          s.routeStack + DesktopStarsRoute.BUY_STARS
        }
        s.copy(
          routeStack = stack,
          selectedPackageStars = stars,
          pendingReturnSendStarsAmount = stars,
          statusMessage = "Insufficient balance. Buy ${DesktopStarsCatalog.formatCount(stars)} Stars to continue.",
          errorMessage = null,
        )
      }
      return
    }

    val idemp = UUID.randomUUID().toString()
    _state.update { it.copy(isSendingStars = true, errorMessage = null) }

    scope.launch {
      val body = buildString {
        append('{')
        append("\"recipient_user_id\":\"").append(DesktopJson.escape(peer.peerUserId)).append("\",")
        append("\"conversation_id\":\"").append(DesktopJson.escape(peer.conversationId)).append("\",")
        append("\"stars\":").append(stars).append(',')
        append("\"idempotency_key\":\"").append(DesktopJson.escape(idemp)).append('"')
        append('}')
      }
      val resp = apiClient.executeChatRaw(
        method = "POST",
        path = "/v1/stars/send",
        jsonBody = body,
        extraHeaders = mapOf("Idempotency-Key" to idemp),
      )
      if (resp != null && resp.first in 200..299) {
        val root = DesktopJson.parseObject(resp.second)
        val wallet = parseWallet(DesktopJson.obj(root, "sender_wallet"))
        val convId = DesktopJson.str(root, "conversation_id").ifBlank { peer.conversationId }
        _state.update {
          it.copy(
            isSendingStars = false,
            wallet = wallet,
            isOpen = false,
            routeStack = listOf(DesktopStarsRoute.HUB),
            celebrationBurstStars = stars,
            statusMessage = "Sent ${DesktopStarsCatalog.formatCount(stars)} Stars to ${peer.peerName}",
          )
        }
        if (convId.isNotBlank()) {
          chatController.refreshConversationNow(convId)
        }
        refreshSummarySync(silent = true)
        return@launch
      }
      if (resp?.first == 402) {
        _state.update {
          it.copy(
            isSendingStars = false,
            routeStack = listOf(DesktopStarsRoute.SEND_STARS, DesktopStarsRoute.BUY_STARS),
            selectedPackageStars = stars,
            pendingReturnSendStarsAmount = stars,
            statusMessage = "Buy Stars to send this gift",
          )
        }
        return@launch
      }
      _state.update {
        it.copy(
          isSendingStars = false,
          errorMessage = parseError(resp?.first ?: 0, resp?.second.orEmpty()),
        )
      }
    }
  }

  private fun parseWallet(m: Map<String, Any?>): DesktopStarWallet = DesktopStarWallet(
    balanceStars = DesktopJson.long(m, "balance_stars", 0L),
    starsReceivedTotal = DesktopJson.long(m, "stars_received_total", 0L),
    starsSentTotal = DesktopJson.long(m, "stars_sent_total", 0L),
    updatedAtMs = DesktopJson.long(m, "updated_at_ms", 0L),
  )

  private fun parseError(code: Int, raw: String): String {
    val err = runCatching { DesktopJson.str(DesktopJson.parseObject(raw), "error") }.getOrDefault("")
    if (err.isNotBlank()) return err
    return when (code) {
      429 -> "Rate limit active for account security. Please wait."
      503 -> "Payment gateway is awaiting server configuration."
      else -> "Request could not be completed. Please try again."
    }
  }
}

@Composable
fun DesktopAnimated3DStarEmblem(
  starsAmount: Long = 100L,
  baseSize: Dp = 64.dp,
  showTelefamWordmark: Boolean = true,
  isDarkTheme: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val tier = DesktopStarsCatalog.animationTier(starsAmount)
  val scaleBoost = when (tier) {
    5 -> 1.48f
    4 -> 1.32f
    3 -> 1.18f
    2 -> 1.08f
    else -> 1.0f
  }
  val effectiveSize = (baseSize.value * scaleBoost).dp
  val sparkleCount = 4 + tier * 3

  val transition = rememberInfiniteTransition()
  val spinPhase by transition.animateFloat(
    initialValue = 0f,
    targetValue = (2.0 * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween((3400 - tier * 320).coerceAtLeast(1700), easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
  )
  val pulse by transition.animateFloat(
    initialValue = 0.88f,
    targetValue = 1.12f,
    animationSpec = infiniteRepeatable(
      animation = tween(1250, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
  )

  val wordRed = if (isDarkTheme) Color(0xFFEF4444) else DesktopCrimson

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = modifier,
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(effectiveSize)) {
      Canvas(modifier = Modifier.size(effectiveSize)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = size.minDimension * 0.44f

        val glowRadius = maxR * (1.28f + tier * 0.14f) * pulse
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              Color(0xFFFFD54F).copy(alpha = 0.34f + tier * 0.05f),
              Color(0xFFFF8F00).copy(alpha = 0.14f),
              Color.Transparent,
            ),
            center = Offset(cx, cy),
            radius = glowRadius,
          ),
          radius = glowRadius,
          center = Offset(cx, cy),
        )

        if (tier >= 3) {
          drawCircle(
            color = Color(0xFFFFCA28).copy(alpha = 0.25f * pulse),
            radius = maxR * 1.12f,
            center = Offset(cx, cy),
            style = Stroke(width = (1.3f + tier * 0.25f)),
          )
        }
        if (tier >= 5) {
          drawCircle(
            color = DesktopCrimson.copy(alpha = 0.26f * pulse),
            radius = maxR * 1.32f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.8f),
          )
        }

        for (idx in 0 until sparkleCount) {
          val baseAngle = (idx.toFloat() / sparkleCount.toFloat()) * (2.0 * PI).toFloat()
          val angle = baseAngle + spinPhase * (if (idx % 2 == 0) 0.45f else -0.35f)
          val dist = maxR * (0.86f + 0.28f * ((idx % 3) / 2f) + 0.08f * sin(spinPhase * 2f + idx))
          val sx = cx + cos(angle) * dist
          val sy = cy + sin(angle) * dist * 0.84f
          val twinkle = ((sin(spinPhase * 3f + idx * 1.3f) + 1f) * 0.5f).coerceIn(0.25f, 1f)
          val spR = (size.minDimension * (0.045f + tier * 0.008f)) * twinkle
          val sparkleColor = if (idx % 4 == 0 && tier >= 3) Color(0xFFFF6B6B) else Color(0xFFFFE57F)

          val spPath = Path().apply {
            moveTo(sx, sy - spR)
            quadraticTo(sx, sy, sx + spR, sy)
            quadraticTo(sx, sy, sx, sy + spR)
            quadraticTo(sx, sy, sx - spR, sy)
            quadraticTo(sx, sy, sx, sy - spR)
            close()
          }
          drawPath(spPath, color = sparkleColor.copy(alpha = twinkle * 0.92f))
        }

        val cosYaw = cos(spinPhase)
        val sinYaw = sin(spinPhase)
        val perspX = if (abs(cosYaw) < 0.16f) if (cosYaw >= 0f) 0.16f else -0.16f else cosYaw
        val extrudeX = -sinYaw * (size.minDimension * 0.085f)
        val extrudeY = size.minDimension * 0.018f
        val outerR = maxR * 0.80f
        val innerR = maxR * 0.37f
        val bobY = sin(spinPhase * 2f) * (size.minDimension * 0.025f)

        fun starPath(dx: Float, dy: Float): Path {
          val p = Path()
          for (i in 0 until 10) {
            val a = -PI / 2.0 + i * (PI / 5.0)
            val r = if (i % 2 == 0) outerR else innerR
            val px = cx + dx + (r * cos(a)).toFloat() * perspX
            val py = cy + dy + bobY + (r * sin(a)).toFloat()
            if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
          }
          p.close()
          return p
        }

        drawPath(
          path = starPath(extrudeX, extrudeY),
          brush = Brush.linearGradient(listOf(Color(0xFFB45309), Color(0xFF78350F))),
        )
        drawPath(
          path = starPath(0f, 0f),
          brush = Brush.linearGradient(
            colors = listOf(Color(0xFFFFF59D), Color(0xFFFFCA28), Color(0xFFF59E0B)),
            start = Offset(cx - outerR, cy - outerR),
            end = Offset(cx + outerR, cy + outerR),
          ),
        )
      }
    }
    if (showTelefamWordmark) {
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "Telefam",
        color = wordRed,
        fontSize = when {
          tier >= 4 -> 14.sp
          tier >= 2 -> 12.5.sp
          else -> 11.5.sp
        },
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.4.sp,
      )
    }
  }
}

@Composable
fun DesktopGoldStarBadgeIcon(size: Dp = 36.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val cx = this.size.width / 2f
    val cy = this.size.height / 2f
    drawCircle(Color(0xFFFFF3D1), radius = this.size.minDimension / 2f, center = Offset(cx, cy))
    val outerR = this.size.minDimension * 0.34f
    val innerR = this.size.minDimension * 0.16f
    val path = Path()
    for (i in 0 until 10) {
      val a = -PI / 2.0 + i * (PI / 5.0)
      val r = if (i % 2 == 0) outerR else innerR
      val x = cx + (r * cos(a)).toFloat()
      val y = cy + (r * sin(a)).toFloat() + this.size.minDimension * 0.015f
      if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFF59E0B))))
  }
}

@Composable
private fun DesktopPaystackLogo(size: Dp = 28.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val barH = h * 0.15f
    val radius = CornerRadius(barH * 0.5f, barH * 0.5f)
    val left = w * 0.12f
    val c = Color(0xFF00C3F7)
    drawRoundRect(c, Offset(left, h * 0.16f), Size(w * 0.76f, barH), radius)
    drawRoundRect(c, Offset(left, h * 0.37f), Size(w * 0.76f, barH), radius)
    drawRoundRect(c, Offset(left, h * 0.58f), Size(w * 0.52f, barH), radius)
    drawRoundRect(c, Offset(left, h * 0.78f), Size(w * 0.30f, barH), radius)
  }
}

@Composable
private fun DesktopPayPalLogo(size: Dp = 28.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val backP = Path().apply {
      moveTo(w * 0.24f, h * 0.14f)
      lineTo(w * 0.56f, h * 0.14f)
      cubicTo(w * 0.78f, h * 0.14f, w * 0.80f, h * 0.52f, w * 0.54f, h * 0.54f)
      lineTo(w * 0.38f, h * 0.54f)
      lineTo(w * 0.33f, h * 0.82f)
      lineTo(w * 0.15f, h * 0.82f)
      close()
    }
    drawPath(backP, Color(0xFF003087))
    val frontP = Path().apply {
      moveTo(w * 0.36f, h * 0.28f)
      lineTo(w * 0.66f, h * 0.28f)
      cubicTo(w * 0.86f, h * 0.28f, w * 0.86f, h * 0.64f, w * 0.60f, h * 0.66f)
      lineTo(w * 0.46f, h * 0.66f)
      lineTo(w * 0.42f, h * 0.90f)
      lineTo(w * 0.26f, h * 0.90f)
      close()
    }
    drawPath(frontP, Color(0xFF009CDE))
  }
}

@Composable
private fun DesktopShieldSecurityIcon(tint: Color, size: Dp = 18.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val p = Path().apply {
      moveTo(w * 0.50f, h * 0.10f)
      lineTo(w * 0.84f, h * 0.24f)
      lineTo(w * 0.84f, h * 0.52f)
      quadraticTo(w * 0.84f, h * 0.78f, w * 0.50f, h * 0.90f)
      quadraticTo(w * 0.16f, h * 0.78f, w * 0.16f, h * 0.52f)
      lineTo(w * 0.16f, h * 0.24f)
      close()
    }
    drawPath(p, tint)
    val chk = Path().apply {
      moveTo(w * 0.35f, h * 0.52f)
      lineTo(w * 0.46f, h * 0.63f)
      lineTo(w * 0.66f, h * 0.41f)
    }
    drawPath(chk, Color.White, style = Stroke(w * 0.10f, cap = StrokeCap.Round, join = StrokeJoin.Round))
  }
}

@Composable
fun DesktopStarsHostScreen(
  controller: DesktopStarsController,
  isDark: Boolean,
  onOpenUserProfile: (String) -> Unit,
) {
  val state by controller.state.collectAsState()
  val route = state.currentRoute ?: return

  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Box(modifier = Modifier.fillMaxSize()) {
      when (route) {
        DesktopStarsRoute.HUB -> DesktopStarsHubView(
          state = state,
          isDark = isDark,
          onBack = { controller.navigateBack() },
          onRefresh = { controller.refreshSummary(false) },
          onSelectTab = { controller.selectHubTab(it) },
          onBuyStars = { controller.openBuyStars() },
        )

        DesktopStarsRoute.SEND_STARS -> DesktopSendStarsView(
          state = state,
          isDark = isDark,
          onBack = { controller.navigateBack() },
          onOpenPeer = { state.targetPeer?.peerUserId?.takeIf { it.isNotBlank() }?.let { controller.closeAll(); onOpenUserProfile(it) } },
          onSendStars = { controller.sendStarsTier(it) },
          onOpenBuyStars = { controller.openBuyStars() },
        )

        DesktopStarsRoute.BUY_STARS -> DesktopBuyStarsView(
          state = state,
          isDark = isDark,
          onBack = { controller.navigateBack() },
          onBuyPackage = { controller.selectPackageForCheckout(it) },
        )

        DesktopStarsRoute.CHECKOUT -> DesktopCheckoutView(
          state = state,
          isDark = isDark,
          onBack = { controller.navigateBack() },
          onSelectProvider = { controller.selectProvider(it) },
          onProceedToPay = { controller.proceedToPay() },
          onVerifyNow = { controller.verifyActivePayment() },
        )
      }

      if (state.isVerifyingPayment) {
        Box(
          modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.52f)),
          contentAlignment = Alignment.Center,
        ) {
          Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 10.dp) {
            Column(
              modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              CircularProgressIndicator(color = DesktopCrimson, modifier = Modifier.size(34.dp))
              Spacer(modifier = Modifier.height(12.dp))
              Text("Verifying payment...", fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
            }
          }
        }
      }

      val msg = state.errorMessage ?: state.statusMessage
      if (!msg.isNullOrBlank()) {
        val isErr = !state.errorMessage.isNullOrBlank()
        Surface(
          onClick = { controller.dismissStatus() },
          shape = CircleShape,
          color = if (isErr) DesktopCrimson else Color(0xEB151821),
          shadowElevation = 6.dp,
          modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp),
        ) {
          Text(
            text = msg,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun DesktopStarsHubView(
  state: DesktopStarsUiState,
  isDark: Boolean,
  onBack: () -> Unit,
  onRefresh: () -> Unit,
  onSelectTab: (Int) -> Unit,
  onBuyStars: () -> Unit,
) {
  val bg = if (isDark) Color(0xFF0E1015) else Color(0xFFF5F6FA)
  val cardBg = if (isDark) Color(0xFF151821) else Color.White
  val innerBg = if (isDark) Color(0xFF1A1E29) else Color(0xFFF3F5FA)
  val cardBorder = if (isDark) Color(0xFF252A36) else Color(0xFFE5E7EB)
  val titleColor = if (isDark) Color.White else Color(0xFF111827)
  val subColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)

  Column(modifier = Modifier.fillMaxSize().background(bg)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = titleColor)
      }
      Spacer(modifier = Modifier.width(6.dp))
      Text("Stars", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = titleColor, modifier = Modifier.weight(1f))
      OutlinedButton(
        onClick = onRefresh,
        shape = CircleShape,
        border = BorderStroke(1.dp, cardBorder),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
      ) {
        Text("Refresh", color = titleColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
      }
    }

    if (state.isLoadingSummary) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = DesktopCrimson)
    }

    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
      LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 660.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        item {
          Surface(
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                DesktopAnimated3DStarEmblem(starsAmount = state.wallet.balanceStars.coerceAtLeast(100L), baseSize = 58.dp, showTelefamWordmark = true, isDarkTheme = isDark)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text("Available Balance", fontSize = 12.5.sp, color = subColor)
                  Text(
                    text = "${DesktopStarsCatalog.formatCount(state.wallet.balanceStars)} Stars",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                  )
                  Text(
                    text = DesktopStarsCatalog.formatCurrencyForStars(state.wallet.balanceStars, state.countryIso),
                    fontSize = 13.sp,
                    color = subColor,
                  )
                }
                Button(
                  onClick = onBuyStars,
                  shape = CircleShape,
                  colors = ButtonDefaults.buttonColors(containerColor = DesktopCrimson, contentColor = Color.White),
                  contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp),
                ) {
                  Text("Buy Stars", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }
              }

              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(16.dp), color = innerBg, border = BorderStroke(1.dp, cardBorder), modifier = Modifier.weight(1f)) {
                  Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Stars Received", fontSize = 12.sp, color = subColor)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      DesktopGoldStarBadgeIcon(size = 20.dp)
                      Text(DesktopStarsCatalog.formatCount(state.wallet.starsReceivedTotal), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
                    }
                  }
                }
                Surface(shape = RoundedCornerShape(16.dp), color = innerBg, border = BorderStroke(1.dp, cardBorder), modifier = Modifier.weight(1f)) {
                  Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Stars Sent", fontSize = 12.sp, color = subColor)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      DesktopGoldStarBadgeIcon(size = 20.dp)
                      Text(DesktopStarsCatalog.formatCount(state.wallet.starsSentTotal), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DesktopCrimson)
                    }
                  }
                }
              }
            }
          }
        }

        item {
          Surface(shape = CircleShape, color = cardBg, border = BorderStroke(1.dp, cardBorder), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
              listOf("Senders (${state.senders.size})", "History (${state.history.size})").forEachIndexed { idx, label ->
                val selected = state.hubTabIndex == idx
                Surface(
                  onClick = { onSelectTab(idx) },
                  shape = CircleShape,
                  color = if (selected) DesktopCrimson else Color.Transparent,
                  modifier = Modifier.weight(1f),
                ) {
                  Text(
                    text = label,
                    color = if (selected) Color.White else subColor,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 11.dp),
                  )
                }
              }
            }
          }
        }

        if (state.hubTabIndex == 0) {
          if (state.senders.isEmpty()) {
            item {
              Surface(shape = RoundedCornerShape(20.dp), color = cardBg, border = BorderStroke(1.dp, cardBorder), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 34.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                  DesktopAnimated3DStarEmblem(starsAmount = 100L, baseSize = 62.dp, showTelefamWordmark = false, isDarkTheme = isDark)
                  Spacer(modifier = Modifier.height(14.dp))
                  Text("No star senders yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = titleColor)
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    "Supporters who send Stars to you will appear here in real time.",
                    fontSize = 13.sp,
                    color = subColor,
                    textAlign = TextAlign.Center,
                  )
                }
              }
            }
          } else {
            items(state.senders, key = { it.senderUserId }) { sender ->
              Surface(shape = RoundedCornerShape(16.dp), color = cardBg, border = BorderStroke(1.dp, cardBorder), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                  DesktopGoldStarBadgeIcon(size = 38.dp)
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(sender.senderName.ifBlank { sender.senderUsername.ifBlank { "Supporter" } }, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = titleColor)
                    if (sender.senderUsername.isNotBlank()) {
                      Text("@${sender.senderUsername}", fontSize = 12.sp, color = subColor)
                    }
                  }
                  Text("${DesktopStarsCatalog.formatCount(sender.totalStars)} Stars", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF22C55E))
                }
              }
            }
          }
        } else {
          if (state.history.isEmpty()) {
            item {
              Surface(shape = RoundedCornerShape(20.dp), color = cardBg, border = BorderStroke(1.dp, cardBorder), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 34.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                  DesktopAnimated3DStarEmblem(starsAmount = 100L, baseSize = 62.dp, showTelefamWordmark = false, isDarkTheme = isDark)
                  Spacer(modifier = Modifier.height(14.dp))
                  Text("No star history yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = titleColor)
                  Spacer(modifier = Modifier.height(6.dp))
                  Text("Top-ups and Star gifts sent or received will appear here.", fontSize = 13.sp, color = subColor, textAlign = TextAlign.Center)
                }
              }
            }
          } else {
            items(state.history, key = { it.entryId }) { item ->
              val isPositive = item.deltaStars > 0L
              val dateStr = if (item.createdAtMs > 0L) SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(item.createdAtMs)) else ""
              Surface(shape = RoundedCornerShape(16.dp), color = cardBg, border = BorderStroke(1.dp, cardBorder), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                  DesktopGoldStarBadgeIcon(size = 36.dp)
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      when (item.entryType) {
                        "purchase" -> "Stars Purchased"
                        "gift_received" -> "From ${item.counterpartyName.ifBlank { item.counterpartyUsername }}"
                        else -> "To ${item.counterpartyName.ifBlank { item.counterpartyUsername }}"
                      },
                      fontWeight = FontWeight.Bold,
                      fontSize = 14.sp,
                      color = titleColor,
                    )
                    Text(listOfNotNull(item.note.takeIf { it.isNotBlank() }, dateStr.takeIf { it.isNotBlank() }).joinToString(" · "), fontSize = 12.sp, color = subColor)
                  }
                  Text(
                    text = "${if (isPositive) "+" else ""}${DesktopStarsCatalog.formatCount(item.deltaStars)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = if (isPositive) Color(0xFF22C55E) else DesktopCrimson,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DesktopSendStarsView(
  state: DesktopStarsUiState,
  isDark: Boolean,
  onBack: () -> Unit,
  onOpenPeer: () -> Unit,
  onSendStars: (Long) -> Unit,
  onOpenBuyStars: () -> Unit,
) {
  val pageBg = if (isDark) Color(0xFF101218) else Color(0xFFF6F7FA)
  val cardBg = if (isDark) Color(0xFF171A22) else Color.White
  val borderColor = if (isDark) Color(0xFF272C38) else Color(0xFFEAECEF)
  val titleColor = if (isDark) Color.White else Color(0xFF161822)
  val subColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF64748B)
  val peer = state.targetPeer

  Column(modifier = Modifier.fillMaxSize().background(pageBg)) {
    Surface(color = DesktopCrimson, modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text("Send Stars", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp, modifier = Modifier.weight(1f))
        Surface(onClick = onOpenBuyStars, shape = CircleShape, color = Color.Black.copy(alpha = 0.25f)) {
          Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DesktopGoldStarBadgeIcon(size = 18.dp)
            Text("Balance: ${DesktopStarsCatalog.formatCount(state.wallet.balanceStars)}", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    if (state.isSendingStars) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = DesktopCrimson)
    }

    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
      LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 660.dp),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (peer != null) {
          item {
            Surface(onClick = onOpenPeer, shape = RoundedCornerShape(16.dp), color = if (isDark) Color(0xFF23171B) else Color(0xFFFFF5F6), border = BorderStroke(1.dp, borderColor), modifier = Modifier.fillMaxWidth()) {
              Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = DesktopCrimson, modifier = Modifier.size(46.dp)) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(peer.peerName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                  }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(peer.peerName, fontWeight = FontWeight.Bold, fontSize = 15.5.sp, color = titleColor)
                  if (peer.peerUsername.isNotBlank()) {
                    Text("@${peer.peerUsername}", fontSize = 12.5.sp, color = subColor)
                  }
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = DesktopCrimson)
              }
            }
          }
        }

        item {
          Surface(shape = RoundedCornerShape(16.dp), color = if (isDark) Color(0xFF1F161A) else Color(0xFFFFF5F6), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
              DesktopGoldStarBadgeIcon(size = 44.dp)
              Spacer(modifier = Modifier.width(12.dp))
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Select stars to send", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = titleColor)
                Text("Show your support and help them grow!", fontSize = 12.5.sp, color = subColor)
                Surface(shape = CircleShape, color = DesktopCrimson.copy(alpha = 0.12f)) {
                  Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DesktopGoldStarBadgeIcon(size = 13.dp)
                    Text("1 star = 1 appreciation", color = DesktopCrimson, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }

        items(DesktopStarsCatalog.presetPackages, key = { it }) { amount ->
          val converted = DesktopStarsCatalog.formatCurrencyForStars(amount, state.countryIso)
          Surface(shape = RoundedCornerShape(14.dp), color = cardBg, border = BorderStroke(1.dp, borderColor), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
              DesktopGoldStarBadgeIcon(size = 38.dp)
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(DesktopStarsCatalog.formatCount(amount), fontWeight = FontWeight.Bold, fontSize = 15.5.sp, color = titleColor)
                Text(converted, fontSize = 12.5.sp, color = subColor)
              }
              Button(
                onClick = { onSendStars(amount) },
                enabled = !state.isSendingStars,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = DesktopCrimson, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
              ) {
                Text("Send", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DesktopBuyStarsView(
  state: DesktopStarsUiState,
  isDark: Boolean,
  onBack: () -> Unit,
  onBuyPackage: (Long) -> Unit,
) {
  val pageBg = if (isDark) Color(0xFF101218) else Color(0xFFF6F7FA)
  val cardBg = if (isDark) Color(0xFF171A22) else Color.White
  val borderColor = if (isDark) Color(0xFF272C38) else Color(0xFFEAECEF)
  val titleColor = if (isDark) Color.White else Color(0xFF161822)
  val subColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF64748B)

  Column(modifier = Modifier.fillMaxSize().background(pageBg)) {
    Surface(color = DesktopCrimson, modifier = Modifier.fillMaxWidth()) {
      Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text("Buy Stars", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp)
      }
    }

    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
      LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 660.dp),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        item {
          Surface(shape = RoundedCornerShape(16.dp), color = if (isDark) Color(0xFF1F161A) else Color(0xFFFFF5F6), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
              DesktopGoldStarBadgeIcon(size = 46.dp)
              Spacer(modifier = Modifier.width(12.dp))
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Support your favorite creators and help them grow!", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = titleColor)
                Text("Buy Stars and send them as a gift during chats or on their content.", fontSize = 12.5.sp, color = subColor)
              }
            }
          }
        }

        item {
          Text("Choose a package", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = titleColor, modifier = Modifier.padding(vertical = 4.dp))
        }

        items(DesktopStarsCatalog.presetPackages, key = { it }) { amount ->
          val isPopular = amount == 5L
          val converted = DesktopStarsCatalog.formatCurrencyForStars(amount, state.countryIso)
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = cardBg,
            border = BorderStroke(if (isPopular) 1.5.dp else 1.dp, if (isPopular) DesktopCrimson else borderColor),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
              DesktopGoldStarBadgeIcon(size = 38.dp)
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Text(DesktopStarsCatalog.formatCount(amount), fontWeight = FontWeight.Bold, fontSize = 15.5.sp, color = titleColor)
                  if (isPopular) {
                    Surface(shape = CircleShape, color = DesktopCrimson) {
                      Text("Popular", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                  }
                }
                Text(converted, fontSize = 12.5.sp, color = subColor)
              }
              Button(
                onClick = { onBuyPackage(amount) },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = DesktopCrimson, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 8.dp),
              ) {
                Text("Buy", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DesktopCheckoutView(
  state: DesktopStarsUiState,
  isDark: Boolean,
  onBack: () -> Unit,
  onSelectProvider: (DesktopStarProvider) -> Unit,
  onProceedToPay: () -> Unit,
  onVerifyNow: () -> Unit,
) {
  val pageBg = if (isDark) Color(0xFF101218) else Color(0xFFF6F7FA)
  val cardBg = if (isDark) Color(0xFF171A22) else Color.White
  val borderColor = if (isDark) Color(0xFF272C38) else Color(0xFFE5E7EB)
  val titleColor = if (isDark) Color.White else Color(0xFF161822)
  val subColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF64748B)
  val converted = DesktopStarsCatalog.formatCurrencyForStars(state.selectedPackageStars, state.countryIso)

  Column(modifier = Modifier.fillMaxSize().background(pageBg)) {
    Surface(color = DesktopCrimson, modifier = Modifier.fillMaxWidth()) {
      Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text("Checkout", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp)
      }
    }

    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
      Column(
        modifier = Modifier.fillMaxSize().widthIn(max = 600.dp).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Surface(shape = RoundedCornerShape(16.dp), color = cardBg, border = BorderStroke(1.dp, borderColor), modifier = Modifier.fillMaxWidth()) {
          Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            DesktopGoldStarBadgeIcon(size = 44.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text("${DesktopStarsCatalog.formatCount(state.selectedPackageStars)} Stars", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = titleColor)
              Text(converted, fontSize = 13.5.sp, color = subColor)
            }
          }
        }

        Text("Payment Method", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = titleColor)

        listOf(
          Triple(DesktopStarProvider.PAYSTACK, "Paystack", "Pay securely with M-Pesa, cards and bank transfers"),
          Triple(DesktopStarProvider.PAYPAL, "PayPal", "Pay with your PayPal account or card"),
        ).forEach { (provider, title, subtitle) ->
          val selected = state.selectedProvider == provider
          Surface(
            onClick = { onSelectProvider(provider) },
            shape = RoundedCornerShape(14.dp),
            color = cardBg,
            border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) DesktopCrimson else borderColor),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
              if (provider == DesktopStarProvider.PAYSTACK) DesktopPaystackLogo(30.dp) else DesktopPayPalLogo(30.dp)
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = titleColor)
                Text(subtitle, fontSize = 12.sp, color = subColor)
              }
              RadioButton(
                selected = selected,
                onClick = { onSelectProvider(provider) },
                colors = RadioButtonDefaults.colors(selectedColor = DesktopCrimson, unselectedColor = subColor),
              )
            }
          }
        }

        Surface(shape = RoundedCornerShape(14.dp), color = if (isDark) Color(0xFF1F161A) else Color(0xFFFFF5F6), modifier = Modifier.fillMaxWidth()) {
          Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            DesktopShieldSecurityIcon(tint = DesktopCrimson, size = 22.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text("Your payment is secure and encrypted", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = titleColor)
              Text("We use industry-standard encryption to protect your information.", fontSize = 12.sp, color = subColor)
            }
          }
        }

        if (state.awaitingBrowserReturn && state.activePayment != null) {
          OutlinedButton(
            onClick = onVerifyNow,
            shape = CircleShape,
            border = BorderStroke(1.2.dp, DesktopCrimson),
            modifier = Modifier.fillMaxWidth().height(48.dp),
          ) {
            Text("Verify Payment & Credit Stars", color = DesktopCrimson, fontWeight = FontWeight.Bold, fontSize = 14.sp)
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
          onClick = onProceedToPay,
          enabled = !state.isInitiatingCheckout && !state.isVerifyingPayment,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = DesktopCrimson, contentColor = Color.White),
          modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
          if (state.isInitiatingCheckout) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
          } else {
            Text("Proceed to Pay", fontWeight = FontWeight.Bold, fontSize = 15.sp)
          }
        }
      }
    }
  }
}
