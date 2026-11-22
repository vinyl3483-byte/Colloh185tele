package telefam.company.stars

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import telefam.company.data.local.ChatConversationEntity
import telefam.company.data.network.SendStarsRequestDto
import telefam.company.data.network.StarLedgerItemDto
import telefam.company.data.network.StarPaymentDto
import telefam.company.data.network.StarSenderSummaryDto
import telefam.company.data.network.StarWalletDto
import telefam.company.data.network.StarsCheckoutInitRequestDto
import telefam.company.data.network.StarsCheckoutVerifyRequestDto
import telefam.company.data.repository.TelefamRepository
import java.util.UUID

data class SendStarsTargetPeer(
  val peerUserId: String,
  val peerName: String,
  val peerUsername: String,
  val peerAvatarUrl: String,
  val isOnline: Boolean,
  val conversationId: String,
)

data class TelefamStarsUiState(
  val isOpen: Boolean = false,
  val routeStack: List<TelefamStarsRoute> = listOf(TelefamStarsRoute.HUB),
  val countryIso: String = "US",
  val wallet: StarWalletDto = StarWalletDto(),
  val senders: List<StarSenderSummaryDto> = emptyList(),
  val history: List<StarLedgerItemDto> = emptyList(),
  val hubTabIndex: Int = 0, // 0 = Senders, 1 = History
  val targetPeer: SendStarsTargetPeer? = null,
  val pendingReturnSendStarsAmount: Long? = null,
  val selectedPackageStars: Long = 100L,
  val selectedProvider: StarPaymentProvider = StarPaymentProvider.PAYSTACK,
  val activePayment: StarPaymentDto? = null,
  val activeCheckoutUrl: String? = null,
  val activeCheckoutReturnUrl: String? = null,
  val activeVerifyIdempKey: String? = null,
  val isLoadingSummary: Boolean = false,
  val isSendingStars: Boolean = false,
  val isInitiatingCheckout: Boolean = false,
  val isVerifyingPayment: Boolean = false,
  val celebrationStarsBurst: Long? = null,
  val statusMessage: String? = null,
  val errorMessage: String? = null,
) {
  val currentRoute: TelefamStarsRoute?
    get() = if (!isOpen) null else routeStack.lastOrNull() ?: TelefamStarsRoute.HUB
}

class TelefamStarsController(
  private val scope: CoroutineScope,
  private val repository: TelefamRepository,
) {
  private val _state = MutableStateFlow(TelefamStarsUiState())
  val state: StateFlow<TelefamStarsUiState> = _state.asStateFlow()

  private var lastCheckoutAttemptSig: String = ""
  private var lastCheckoutAttemptIdempKey: String = ""

  init {
    scope.launch {
      repository.chatRepository.wsEvents.collect { ev ->
        if (ev.event == "stars.updated") {
          applyRealtimeWalletUpdate(ev.rawJson)
          refreshSummary(silent = true)
        }
      }
    }
  }

  private fun applyRealtimeWalletUpdate(rawJson: String) {
    if (rawJson.isBlank()) return
    val obj = runCatching { JSONObject(rawJson) }.getOrNull() ?: return
    val bal = obj.optLong("balance_stars", -1L)
    val rec = obj.optLong("stars_received", -1L)
    val sent = obj.optLong("stars_sent", -1L)
    val delta = obj.optLong("delta", 0L)
    val entryType = obj.optString("entry_type", "")
    _state.update { s ->
      val nextWallet = s.wallet.copy(
        balanceStars = if (bal >= 0L) bal else s.wallet.balanceStars,
        starsReceivedTotal = if (rec >= 0L) rec else s.wallet.starsReceivedTotal,
        starsSentTotal = if (sent >= 0L) sent else s.wallet.starsSentTotal,
        updatedAtMs = System.currentTimeMillis(),
      )
      val toast = if (entryType == "received" && delta > 0L) {
        "Received ${TelefamStarsCatalog.formatStarsCount(delta)} Telefam Stars"
      } else {
        s.statusMessage
      }
      s.copy(wallet = nextWallet, statusMessage = toast)
    }
  }

  fun openStarsHub(profileCountryIso: String?, locationHint: String = "") {
    val iso = TelefamStarsCatalog.resolveCountryIso(profileCountryIso, locationHint)
    _state.update {
      it.copy(
        isOpen = true,
        routeStack = listOf(TelefamStarsRoute.HUB),
        countryIso = iso,
        targetPeer = null,
        pendingReturnSendStarsAmount = null,
        errorMessage = null,
      )
    }
    refreshSummary(silent = false)
  }

  fun openSendStarsFromChat(
    conversation: ChatConversationEntity,
    profileCountryIso: String?,
    locationHint: String = "",
  ) {
    val iso = TelefamStarsCatalog.resolveCountryIso(profileCountryIso, locationHint)
    val peer = SendStarsTargetPeer(
      peerUserId = conversation.peerUserId,
      peerName = conversation.peerFullName.ifBlank { conversation.peerUsername.ifBlank { "Telefam" } },
      peerUsername = conversation.peerUsername,
      peerAvatarUrl = conversation.peerAvatarUrl,
      isOnline = conversation.isOnline,
      conversationId = conversation.conversationId,
    )
    _state.update { s ->
      val hasAnyBalance = s.wallet.balanceStars > 0L
      val initialStack = if (hasAnyBalance || s.isLoadingSummary) {
        listOf(TelefamStarsRoute.SEND_STARS)
      } else {
        listOf(TelefamStarsRoute.SEND_STARS, TelefamStarsRoute.BUY_STARS)
      }
      s.copy(
        isOpen = true,
        routeStack = initialStack,
        countryIso = iso,
        targetPeer = peer,
        pendingReturnSendStarsAmount = null,
        errorMessage = null,
      )
    }
    scope.launch {
      refreshSummaryNow(silent = true)
      val cur = _state.value
      // If summary confirms balance == 0 and user came from chat gift button, redirect directly to Buy Stars
      if (cur.isOpen && cur.targetPeer != null && cur.wallet.balanceStars <= 0L &&
        cur.currentRoute == TelefamStarsRoute.SEND_STARS
      ) {
        _state.update { s ->
          s.copy(
            routeStack = listOf(TelefamStarsRoute.SEND_STARS, TelefamStarsRoute.BUY_STARS),
            statusMessage = "Buy Stars to send gifts to ${peer.peerName}",
          )
        }
      }
    }
  }

  fun openBuyStars(suggestedAmount: Long? = null, pendingSendAmount: Long? = null) {
    _state.update { s ->
      val nextPkg = suggestedAmount ?: s.selectedPackageStars
      val stack = if (s.routeStack.lastOrNull() == TelefamStarsRoute.BUY_STARS) {
        s.routeStack
      } else {
        s.routeStack + TelefamStarsRoute.BUY_STARS
      }
      s.copy(
        isOpen = true,
        routeStack = stack,
        selectedPackageStars = nextPkg,
        pendingReturnSendStarsAmount = pendingSendAmount ?: s.pendingReturnSendStarsAmount,
        errorMessage = null,
      )
    }
  }

  fun selectPackageForCheckout(stars: Long) {
    _state.update { s ->
      val nextStack = if (s.routeStack.lastOrNull() == TelefamStarsRoute.CHECKOUT) {
        s.routeStack
      } else {
        s.routeStack + TelefamStarsRoute.CHECKOUT
      }
      s.copy(
        routeStack = nextStack,
        selectedPackageStars = stars,
        errorMessage = null,
      )
    }
  }

  fun selectPaymentProvider(provider: StarPaymentProvider) {
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
          routeStack = listOf(TelefamStarsRoute.HUB),
          activeCheckoutUrl = null,
          errorMessage = null,
        )
      } else {
        s.copy(
          routeStack = s.routeStack.dropLast(1),
          errorMessage = null,
        )
      }
    }
  }

  fun closeAll() {
    _state.update {
      it.copy(
        isOpen = false,
        routeStack = listOf(TelefamStarsRoute.HUB),
        activeCheckoutUrl = null,
        errorMessage = null,
      )
    }
  }

  fun dismissStatusMessage() {
    _state.update { it.copy(statusMessage = null, errorMessage = null) }
  }

  fun dismissCelebrationBurst() {
    _state.update { it.copy(celebrationStarsBurst = null) }
  }

  fun refreshSummary(silent: Boolean = false) {
    scope.launch { refreshSummaryNow(silent) }
  }

  private suspend fun refreshSummaryNow(silent: Boolean) {
    val api = repository.getApiServiceOrNull() ?: return
    val session = repository.sessionFlow.first()
    val token = session.accessToken.orEmpty()
    if (token.isBlank()) return
    val fp = repository.getDeviceFingerprint()

    if (!silent) {
      _state.update { it.copy(isLoadingSummary = true, errorMessage = null) }
    }
    val country = _state.value.countryIso
    val resp = runCatching {
      api.getStarsSummary("Bearer $token", fp, country)
    }.getOrNull()

    if (resp?.isSuccessful == true) {
      val body = resp.body()
      if (body != null && body.success) {
        _state.update { s ->
          s.copy(
            wallet = body.wallet,
            senders = body.senders,
            history = body.history,
            countryIso = body.countryIso.ifBlank { s.countryIso },
            isLoadingSummary = false,
          )
        }
        return
      }
    }
    if (!silent) {
      _state.update { it.copy(isLoadingSummary = false) }
    }
  }

  fun proceedToPay() {
    val snap = _state.value
    if (snap.isInitiatingCheckout) return
    val api = repository.getApiServiceOrNull()
    if (api == null) {
      _state.update { it.copy(errorMessage = "Network unavailable") }
      return
    }

    val sig = "${snap.selectedProvider.wire}:${snap.selectedPackageStars}:${snap.countryIso}:${snap.targetPeer?.peerUserId.orEmpty()}"
    val idempKey = if (sig == lastCheckoutAttemptSig && lastCheckoutAttemptIdempKey.isNotBlank()) {
      lastCheckoutAttemptIdempKey
    } else {
      UUID.randomUUID().toString().also {
        lastCheckoutAttemptSig = sig
        lastCheckoutAttemptIdempKey = it
      }
    }

    _state.update { it.copy(isInitiatingCheckout = true, errorMessage = null) }

    scope.launch(Dispatchers.IO) {
      val session = repository.sessionFlow.first()
      val token = session.accessToken.orEmpty()
      if (token.isBlank()) {
        _state.update { it.copy(isInitiatingCheckout = false, errorMessage = "Please sign in first") }
        return@launch
      }
      val fp = repository.getDeviceFingerprint()
      val req = StarsCheckoutInitRequestDto(
        provider = snap.selectedProvider.wire,
        packageStars = snap.selectedPackageStars,
        countryIso = snap.countryIso,
        idempotencyKey = idempKey,
        purpose = if (snap.targetPeer != null) "send_gift" else "topup",
        peerUserId = snap.targetPeer?.peerUserId.orEmpty(),
        conversationId = snap.targetPeer?.conversationId.orEmpty(),
      )
      val resp = runCatching {
        api.initStarsCheckout("Bearer $token", fp, idempKey, req)
      }.getOrNull()

      if (resp?.isSuccessful == true) {
        val body = resp.body()
        if (body != null && body.success && body.payment.checkoutUrl.isNotBlank()) {
          _state.update { s ->
            s.copy(
              isInitiatingCheckout = false,
              activePayment = body.payment,
              activeCheckoutUrl = body.payment.checkoutUrl,
              activeCheckoutReturnUrl = body.returnUrl,
              activeVerifyIdempKey = UUID.randomUUID().toString(),
              wallet = body.wallet,
            )
          }
          return@launch
        }
      }
      val msg = parseErrorMessage(resp?.errorBody()?.string(), resp?.code() ?: 0)
      _state.update { it.copy(isInitiatingCheckout = false, errorMessage = msg) }
    }
  }

  fun dismissCheckoutWebView(verifyIfPending: Boolean = true) {
    val active = _state.value.activePayment
    _state.update { it.copy(activeCheckoutUrl = null) }
    if (verifyIfPending && active != null && active.status == "pending") {
      verifyActivePayment(
        paymentId = active.paymentId,
        reference = active.providerReference,
        paypalOrderId = active.providerOrderId,
      )
    }
  }

  fun handlePaymentReturnDeepLink(
    paymentId: String = "",
    reference: String = "",
    paypalOrderId: String = "",
    status: String = "",
  ) {
    _state.update { it.copy(activeCheckoutUrl = null) }
    if (status.equals("cancelled", ignoreCase = true)) {
      _state.update { it.copy(statusMessage = "Payment cancelled") }
      return
    }
    verifyActivePayment(
      paymentId = paymentId.ifBlank { _state.value.activePayment?.paymentId.orEmpty() },
      reference = reference.ifBlank { _state.value.activePayment?.providerReference.orEmpty() },
      paypalOrderId = paypalOrderId.ifBlank { _state.value.activePayment?.providerOrderId.orEmpty() },
    )
  }

  fun verifyActivePayment(
    paymentId: String,
    reference: String = "",
    paypalOrderId: String = "",
  ) {
    if (paymentId.isBlank() && reference.isBlank() && paypalOrderId.isBlank()) return
    val api = repository.getApiServiceOrNull() ?: return
    val snap = _state.value
    if (snap.isVerifyingPayment) return
    val verifyIdemp = snap.activeVerifyIdempKey ?: UUID.randomUUID().toString()

    _state.update { it.copy(isVerifyingPayment = true, activeCheckoutUrl = null, errorMessage = null) }

    scope.launch(Dispatchers.IO) {
      val session = repository.sessionFlow.first()
      val token = session.accessToken.orEmpty()
      if (token.isBlank()) {
        _state.update { it.copy(isVerifyingPayment = false) }
        return@launch
      }
      val fp = repository.getDeviceFingerprint()
      val resp = runCatching {
        api.verifyStarsCheckout(
          "Bearer $token",
          fp,
          verifyIdemp,
          StarsCheckoutVerifyRequestDto(
            paymentId = paymentId,
            reference = reference,
            paypalOrderId = paypalOrderId,
            idempotencyKey = verifyIdemp,
          ),
        )
      }.getOrNull()

      if (resp?.isSuccessful == true) {
        val body = resp.body()
        if (body != null && body.success) {
          lastCheckoutAttemptSig = ""
          lastCheckoutAttemptIdempKey = ""
          val starsBought = body.payment.packageStars
          val pendingSend = _state.value.pendingReturnSendStarsAmount
          val peer = _state.value.targetPeer

          _state.update { s ->
            val nextStack = when {
              peer != null && s.routeStack.contains(TelefamStarsRoute.SEND_STARS) -> listOf(TelefamStarsRoute.SEND_STARS)
              else -> listOf(TelefamStarsRoute.HUB)
            }
            s.copy(
              isVerifyingPayment = false,
              activePayment = body.payment,
              wallet = body.wallet,
              routeStack = nextStack,
              pendingReturnSendStarsAmount = null,
              statusMessage = "${TelefamStarsCatalog.formatStarsCount(starsBought)} Stars added to your balance",
            )
          }
          refreshSummaryNow(silent = true)

          // Auto-resume sending if the user originally tapped Send on a tier > balance
          if (peer != null && pendingSend != null && body.wallet.balanceStars >= pendingSend) {
            sendStarsTier(pendingSend)
          }
          return@launch
        }
      }
      val msg = parseErrorMessage(resp?.errorBody()?.string(), resp?.code() ?: 0)
      _state.update { it.copy(isVerifyingPayment = false, errorMessage = msg) }
    }
  }

  fun sendStarsTier(stars: Long) {
    val snap = _state.value
    val peer = snap.targetPeer ?: return
    if (snap.isSendingStars) return

    // Client pre-check: if 0 balance or selected tier exceeds balance, redirect immediately to Buy Stars
    if (snap.wallet.balanceStars < stars) {
      _state.update { s ->
        val nextStack = if (s.routeStack.contains(TelefamStarsRoute.BUY_STARS)) {
          s.routeStack
        } else {
          s.routeStack + TelefamStarsRoute.BUY_STARS
        }
        s.copy(
          routeStack = nextStack,
          selectedPackageStars = stars,
          pendingReturnSendStarsAmount = stars,
          statusMessage = "Insufficient balance. Buy ${TelefamStarsCatalog.formatStarsCount(stars)} Stars to continue.",
          errorMessage = null,
        )
      }
      return
    }

    val api = repository.getApiServiceOrNull()
    if (api == null) {
      _state.update { it.copy(errorMessage = "Network unavailable") }
      return
    }
    val idempKey = UUID.randomUUID().toString()
    _state.update { it.copy(isSendingStars = true, errorMessage = null) }

    scope.launch(Dispatchers.IO) {
      val session = repository.sessionFlow.first()
      val token = session.accessToken.orEmpty()
      if (token.isBlank()) {
        _state.update { it.copy(isSendingStars = false, errorMessage = "Session expired") }
        return@launch
      }
      val fp = repository.getDeviceFingerprint()
      val resp = runCatching {
        api.sendStarsInChat(
          "Bearer $token",
          fp,
          idempKey,
          SendStarsRequestDto(
            recipientUserId = peer.peerUserId,
            conversationId = peer.conversationId,
            stars = stars,
            idempotencyKey = idempKey,
          ),
        )
      }.getOrNull()

      if (resp?.isSuccessful == true) {
        val body = resp.body()
        if (body != null && body.success) {
          _state.update { s ->
            s.copy(
              isSendingStars = false,
              wallet = body.senderWallet,
              isOpen = false,
              routeStack = listOf(TelefamStarsRoute.HUB),
              celebrationStarsBurst = stars,
              statusMessage = "Sent ${TelefamStarsCatalog.formatStarsCount(stars)} Stars to ${peer.peerName}",
            )
          }
          if (body.conversationId.isNotBlank()) {
            runCatching { repository.chatRepository.loadConversationMessages(body.conversationId, markAsRead = false) }
          }
          refreshSummaryNow(silent = true)
          return@launch
        }
      }

      if (resp?.code() == 402) {
        val bodyWallet = resp.body()?.senderWallet
        _state.update { s ->
          s.copy(
            isSendingStars = false,
            wallet = bodyWallet ?: s.wallet,
            routeStack = listOf(TelefamStarsRoute.SEND_STARS, TelefamStarsRoute.BUY_STARS),
            selectedPackageStars = stars,
            pendingReturnSendStarsAmount = stars,
            statusMessage = "Buy Stars to send this gift",
          )
        }
        return@launch
      }

      val msg = parseErrorMessage(resp?.errorBody()?.string(), resp?.code() ?: 0)
      _state.update { it.copy(isSendingStars = false, errorMessage = msg) }
    }
  }

  private fun parseErrorMessage(raw: String?, code: Int): String {
    if (!raw.isNullOrBlank()) {
      val parsed = runCatching { JSONObject(raw).optString("error", "") }.getOrDefault("")
      if (parsed.isNotBlank()) return parsed
    }
    return when (code) {
      429 -> "Rate limit active for account security. Please wait a moment."
      503 -> "Payment gateway is awaiting server configuration."
      else -> "Request could not be completed. Please try again."
    }
  }
}
