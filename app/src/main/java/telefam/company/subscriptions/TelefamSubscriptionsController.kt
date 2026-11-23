package telefam.company.subscriptions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import telefam.company.data.network.CancelSubscriptionRequestDto
import telefam.company.data.network.CreatorOfferingResponseDto
import telefam.company.data.network.CreatorPlanDto
import telefam.company.data.network.RenewSubscriptionRequestDto
import telefam.company.data.network.SaveCreatorPlanRequestDto
import telefam.company.data.network.SubscriptionCheckoutInitRequestDto
import telefam.company.data.network.SubscriptionCheckoutVerifyRequestDto
import telefam.company.data.network.SubscriptionRecordDto
import telefam.company.data.network.SubscriptionsHubResponseDto
import telefam.company.data.repository.TelefamRepository
import java.util.UUID

class TelefamSubscriptionsController(
  private val scope: CoroutineScope,
  private val repository: TelefamRepository,
) {
  private val _state = MutableStateFlow(TelefamSubscriptionsUiState())
  val state: StateFlow<TelefamSubscriptionsUiState> = _state.asStateFlow()

  private var pollJob: Job? = null
  private val routeBackStack = mutableListOf<SubscriptionsRoute>()

  init {
    scope.launch {
      repository.chatRepository.wsEvents.collect { ev ->
        if (ev.event == "subscription.updated") {
          onRealtimeSubscriptionUpdated()
        }
      }
    }
  }

  fun openHub(
    initialTab: SubscriptionsHubTab = SubscriptionsHubTab.SUBSCRIBERS,
    profileCountryIso: String?,
    locationHint: String?,
  ) {
    val resolvedCountry = TelefamSubscriptionsCatalog.resolveCountryIso(profileCountryIso, locationHint)
    routeBackStack.clear()
    _state.update {
      it.copy(
        isOpen = true,
        currentRoute = SubscriptionsRoute.HUB,
        activeHubTab = initialTab,
        countryIso = resolvedCountry,
        goalPresets = TelefamSubscriptionsCatalog.defaultGoalPresets(resolvedCountry),
        creatorOffering = null,
        errorMessage = null,
        statusMessage = null,
      )
    }
    refreshHub(silent = false)
  }

  fun openCreatorSubscribe(
    creatorUserId: String,
    creatorName: String,
    creatorUsername: String,
    creatorAvatarUrl: String,
    creatorVerifiedTier: String,
    viewerCountryIso: String?,
    locationHint: String?,
  ) {
    val cleanId = creatorUserId.trim()
    if (cleanId.isBlank()) return
    val resolvedCountry = TelefamSubscriptionsCatalog.resolveCountryIso(viewerCountryIso, locationHint)
    if (_state.value.isOpen && _state.value.currentRoute == SubscriptionsRoute.HUB) {
      routeBackStack.add(SubscriptionsRoute.HUB)
    } else {
      routeBackStack.clear()
    }
    _state.update {
      it.copy(
        isOpen = true,
        currentRoute = SubscriptionsRoute.CREATOR_SUBSCRIBE,
        countryIso = resolvedCountry,
        isLoading = true,
        errorMessage = null,
        statusMessage = null,
        creatorOffering = CreatorOfferingModel(
          creatorUserId = cleanId,
          creatorName = creatorName,
          creatorUsername = creatorUsername,
          creatorAvatarUrl = creatorAvatarUrl,
          creatorVerifiedTier = creatorVerifiedTier,
          countryIso = resolvedCountry,
        ),
      )
    }
    loadCreatorOffering(cleanId)
  }

  fun closeAll() {
    pollJob?.cancel()
    routeBackStack.clear()
    _state.update {
      it.copy(
        isOpen = false,
        activeCheckout = null,
        isLoading = false,
        isSavingPlan = false,
        isMutating = false,
        errorMessage = null,
      )
    }
  }

  fun navigateBack() {
    val cur = _state.value
    when (cur.currentRoute) {
      SubscriptionsRoute.CHECKOUT_WEBVIEW -> {
        pollJob?.cancel()
        val prev = routeBackStack.removeLastOrNull() ?: SubscriptionsRoute.CREATOR_SUBSCRIBE
        _state.update { it.copy(currentRoute = prev, activeCheckout = null) }
      }
      SubscriptionsRoute.CREATOR_SUBSCRIBE -> {
        val prev = routeBackStack.removeLastOrNull()
        if (prev != null) {
          _state.update { it.copy(currentRoute = prev, creatorOffering = null) }
          refreshHub(silent = true)
        } else {
          closeAll()
        }
      }
      SubscriptionsRoute.HUB -> closeAll()
    }
  }

  fun selectHubTab(tab: SubscriptionsHubTab) {
    _state.update { it.copy(activeHubTab = tab, errorMessage = null) }
  }

  fun updateDraftEnabled(enabled: Boolean) {
    _state.update { it.copy(draftPlanEnabled = enabled) }
  }

  fun updateDraftMonthlyPriceCents(cents: Long) {
    val clamped = cents.coerceIn(
      TelefamSubscriptionsCatalog.MIN_MONTHLY_USD_CENTS,
      TelefamSubscriptionsCatalog.MAX_MONTHLY_USD_CENTS,
    )
    _state.update { it.copy(draftMonthlyUsdCents = clamped) }
  }

  fun updateDraftGoalUsdCents(cents: Long) {
    val clamped = cents.coerceIn(
      TelefamSubscriptionsCatalog.MIN_GOAL_USD_CENTS,
      1_000_000L,
    )
    _state.update { it.copy(draftMonthlyGoalUsdCents = clamped) }
  }

  fun updateDraftPerksSummary(perks: String) {
    _state.update { it.copy(draftPerksSummary = perks.take(220)) }
  }

  fun selectBillingCycle(cycle: SubscriptionBillingCycle) {
    _state.update { it.copy(selectedCycle = cycle, errorMessage = null) }
  }

  fun selectPaymentProvider(provider: SubscriptionPaymentProvider) {
    _state.update { it.copy(selectedProvider = provider, errorMessage = null) }
  }

  fun dismissStatusMessage() {
    _state.update { it.copy(statusMessage = null, errorMessage = null) }
  }

  fun dismissCheckoutWebView() {
    pollJob?.cancel()
    val prev = routeBackStack.removeLastOrNull() ?: SubscriptionsRoute.CREATOR_SUBSCRIBE
    _state.update { it.copy(currentRoute = prev, activeCheckout = null) }
  }

  fun refreshHub(silent: Boolean = false) {
    val api = repository.getApiServiceOrNull() ?: return
    val country = _state.value.countryIso
    scope.launch {
      if (!silent) {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
      }
      val token = repository.getStreamCredentials().first
      val fp = repository.getDeviceFingerprint()
      if (token.isBlank()) {
        _state.update { it.copy(isLoading = false) }
        return@launch
      }
      val resp = runCatching {
        api.getSubscriptionsHub(
          bearer = "Bearer $token",
          deviceFingerprint = fp,
          countryIso = country,
        )
      }.getOrNull()
      val body = resp?.body()
      if (resp?.isSuccessful == true && body != null && body.success) {
        applyHubResponse(body)
      } else if (!silent) {
        val msg = parseServerError(resp?.errorBody()?.string()) ?: "Unable to load subscriptions right now."
        _state.update { it.copy(isLoading = false, errorMessage = msg) }
      } else {
        _state.update { it.copy(isLoading = false) }
      }
    }
  }

  fun loadCreatorOffering(creatorUserId: String) {
    val api = repository.getApiServiceOrNull() ?: return
    val country = _state.value.countryIso
    scope.launch {
      _state.update { it.copy(isLoading = true, errorMessage = null) }
      val token = repository.getStreamCredentials().first
      val fp = repository.getDeviceFingerprint()
      if (token.isBlank()) {
        _state.update { it.copy(isLoading = false) }
        return@launch
      }
      val resp = runCatching {
        api.getCreatorSubscriptionOffering(
          bearer = "Bearer $token",
          deviceFingerprint = fp,
          creatorUserId = creatorUserId,
          countryIso = country,
        )
      }.getOrNull()
      val body = resp?.body()
      if (resp?.isSuccessful == true && body != null && body.success) {
        _state.update {
          it.copy(
            isLoading = false,
            creatorOffering = body.toModel(),
            selectedCycle = body.viewerSubscription?.billingCycle?.let(SubscriptionBillingCycle::fromWire)
              ?: it.selectedCycle,
          )
        }
      } else {
        val msg = parseServerError(resp?.errorBody()?.string()) ?: "Unable to load creator membership plan."
        _state.update { it.copy(isLoading = false, errorMessage = msg) }
      }
    }
  }

  fun saveCreatorPlan() {
    val api = repository.getApiServiceOrNull() ?: return
    val cur = _state.value
    scope.launch {
      _state.update { it.copy(isSavingPlan = true, errorMessage = null, statusMessage = null) }
      val token = repository.getStreamCredentials().first
      val fp = repository.getDeviceFingerprint()
      if (token.isBlank()) {
        _state.update { it.copy(isSavingPlan = false) }
        return@launch
      }
      val resp = runCatching {
        api.saveCreatorSubscriptionPlan(
          bearer = "Bearer $token",
          deviceFingerprint = fp,
          body = SaveCreatorPlanRequestDto(
            enabled = cur.draftPlanEnabled,
            monthlyUsdCents = cur.draftMonthlyUsdCents,
            annualDiscountPct = cur.draftAnnualDiscountPct,
            monthlyGoalUsdCents = cur.draftMonthlyGoalUsdCents,
            perksSummary = cur.draftPerksSummary.trim(),
            countryIso = cur.countryIso,
          ),
        )
      }.getOrNull()
      val body = resp?.body()
      if (resp?.isSuccessful == true && body != null && body.success) {
        applyHubResponse(body)
        _state.update {
          it.copy(
            isSavingPlan = false,
            statusMessage = "Subscription plan & monthly goal saved.",
          )
        }
      } else {
        val msg = parseServerError(resp?.errorBody()?.string()) ?: "Unable to save subscription plan."
        _state.update { it.copy(isSavingPlan = false, errorMessage = msg) }
      }
    }
  }

  fun startSubscribeCheckout() {
    val cur = _state.value
    val creatorId = cur.creatorOffering?.creatorUserId.orEmpty()
    if (creatorId.isBlank()) return
    startCheckoutForCreator(
      creatorUserId = creatorId,
      cycle = cur.selectedCycle,
      provider = cur.selectedProvider,
    )
  }

  private fun startCheckoutForCreator(
    creatorUserId: String,
    cycle: SubscriptionBillingCycle,
    provider: SubscriptionPaymentProvider,
  ) {
    val api = repository.getApiServiceOrNull() ?: return
    val cur = _state.value
    scope.launch {
      _state.update { it.copy(isMutating = true, errorMessage = null, statusMessage = null) }
      val token = repository.getStreamCredentials().first
      val fp = repository.getDeviceFingerprint()
      if (token.isBlank()) {
        _state.update { it.copy(isMutating = false) }
        return@launch
      }
      val idempKey = "subpay_${UUID.randomUUID().toString().replace("-", "").take(20)}"
      val resp = runCatching {
        api.initSubscriptionCheckout(
          bearer = "Bearer $token",
          deviceFingerprint = fp,
          idempotencyKey = idempKey,
          body = SubscriptionCheckoutInitRequestDto(
            creatorUserId = creatorUserId,
            billingCycle = cycle.wireValue,
            provider = provider.wireValue,
            countryIso = cur.countryIso,
            idempotencyKey = idempKey,
          ),
        )
      }.getOrNull()
      val body = resp?.body()
      if (resp?.isSuccessful == true && body != null && body.success && body.payment.checkoutUrl.isNotBlank()) {
        val pricing = body.offering.plan.pricing
        val displayAmt = if (cycle == SubscriptionBillingCycle.ANNUAL) pricing.annualDisplay else pricing.monthlyDisplay
        val session = SubscriptionCheckoutSession(
          paymentId = body.payment.paymentId,
          creatorUserId = creatorUserId,
          billingCycle = cycle,
          provider = provider,
          providerReference = body.payment.providerReference,
          providerOrderId = body.payment.providerOrderId,
          expectedCurrency = body.payment.expectedCurrency,
          expectedAmountDisplay = displayAmt,
          checkoutUrl = body.payment.checkoutUrl,
          returnUrl = body.returnUrl,
        )
        routeBackStack.add(_state.value.currentRoute)
        _state.update {
          it.copy(
            isMutating = false,
            creatorOffering = body.offering.toModel(),
            activeCheckout = session,
            currentRoute = SubscriptionsRoute.CHECKOUT_WEBVIEW,
          )
        }
        startVerificationPolling(session)
      } else {
        val msg = parseServerError(resp?.errorBody()?.string()) ?: "Unable to start payment checkout."
        _state.update { it.copy(isMutating = false, errorMessage = msg) }
      }
    }
  }

  fun cancelCreatorSubscription(creatorUserId: String) {
    val api = repository.getApiServiceOrNull() ?: return
    val cur = _state.value
    scope.launch {
      _state.update { it.copy(isMutating = true, errorMessage = null, statusMessage = null) }
      val token = repository.getStreamCredentials().first
      val fp = repository.getDeviceFingerprint()
      if (token.isBlank()) {
        _state.update { it.copy(isMutating = false) }
        return@launch
      }
      val resp = runCatching {
        api.cancelSubscription(
          bearer = "Bearer $token",
          deviceFingerprint = fp,
          body = CancelSubscriptionRequestDto(
            creatorUserId = creatorUserId,
            countryIso = cur.countryIso,
          ),
        )
      }.getOrNull()
      val body = resp?.body()
      if (resp?.isSuccessful == true && body != null && body.success) {
        val updatedModel = body.subscription?.toModel()
        _state.update { state ->
          val nextOffering = state.creatorOffering?.let { off ->
            if (off.creatorUserId == creatorUserId && updatedModel != null) {
              off.copy(viewerSubscription = updatedModel)
            } else {
              off
            }
          }
          val nextActive = if (updatedModel != null) {
            state.activeSubscriptions.map { if (it.creatorUserId == creatorUserId) updatedModel else it }
          } else {
            state.activeSubscriptions
          }
          state.copy(
            isMutating = false,
            creatorOffering = nextOffering,
            activeSubscriptions = nextActive,
            statusMessage = body.message.ifBlank { "Auto-renewal cancelled." },
          )
        }
        refreshHub(silent = true)
      } else {
        val msg = parseServerError(resp?.errorBody()?.string()) ?: "Unable to cancel subscription."
        _state.update { it.copy(isMutating = false, errorMessage = msg) }
      }
    }
  }

  fun renewCreatorSubscription(
    creatorUserId: String,
    billingCycle: SubscriptionBillingCycle? = null,
    provider: SubscriptionPaymentProvider? = null,
  ) {
    val api = repository.getApiServiceOrNull() ?: return
    val cur = _state.value
    scope.launch {
      _state.update { it.copy(isMutating = true, errorMessage = null, statusMessage = null) }
      val token = repository.getStreamCredentials().first
      val fp = repository.getDeviceFingerprint()
      if (token.isBlank()) {
        _state.update { it.copy(isMutating = false) }
        return@launch
      }
      val idempKey = "subrenew_${UUID.randomUUID().toString().replace("-", "").take(20)}"
      val resp = runCatching {
        api.renewSubscription(
          bearer = "Bearer $token",
          deviceFingerprint = fp,
          idempotencyKey = idempKey,
          body = RenewSubscriptionRequestDto(
            creatorUserId = creatorUserId,
            billingCycle = billingCycle?.wireValue.orEmpty(),
            provider = provider?.wireValue.orEmpty(),
            countryIso = cur.countryIso,
            idempotencyKey = idempKey,
          ),
        )
      }.getOrNull()
      val body = resp?.body()
      if (resp?.isSuccessful == true && body != null && body.success) {
        if (body.requiresCheckout && body.checkout != null && body.checkout.payment.checkoutUrl.isNotBlank()) {
          val checkout = body.checkout
          val cycle = SubscriptionBillingCycle.fromWire(checkout.payment.billingCycle)
          val prov = if (checkout.payment.provider.equals("paypal", ignoreCase = true)) {
            SubscriptionPaymentProvider.PAYPAL
          } else {
            SubscriptionPaymentProvider.PAYSTACK
          }
          val pricing = checkout.offering.plan.pricing
          val displayAmt = if (cycle == SubscriptionBillingCycle.ANNUAL) pricing.annualDisplay else pricing.monthlyDisplay
          val session = SubscriptionCheckoutSession(
            paymentId = checkout.payment.paymentId,
            creatorUserId = creatorUserId,
            billingCycle = cycle,
            provider = prov,
            providerReference = checkout.payment.providerReference,
            providerOrderId = checkout.payment.providerOrderId,
            expectedCurrency = checkout.payment.expectedCurrency,
            expectedAmountDisplay = displayAmt,
            checkoutUrl = checkout.payment.checkoutUrl,
            returnUrl = checkout.returnUrl,
          )
          routeBackStack.add(_state.value.currentRoute)
          _state.update {
            it.copy(
              isMutating = false,
              creatorOffering = checkout.offering.toModel(),
              activeCheckout = session,
              currentRoute = SubscriptionsRoute.CHECKOUT_WEBVIEW,
            )
          }
          startVerificationPolling(session)
        } else {
          val updatedModel = body.subscription?.toModel()
          _state.update { state ->
            val nextOffering = state.creatorOffering?.let { off ->
              if (off.creatorUserId == creatorUserId && updatedModel != null) {
                off.copy(viewerSubscription = updatedModel)
              } else {
                off
              }
            }
            val nextActive = if (updatedModel != null) {
              state.activeSubscriptions.map { if (it.creatorUserId == creatorUserId) updatedModel else it }
            } else {
              state.activeSubscriptions
            }
            state.copy(
              isMutating = false,
              creatorOffering = nextOffering,
              activeSubscriptions = nextActive,
              statusMessage = body.message.ifBlank { "Subscription renewed." },
            )
          }
          refreshHub(silent = true)
        }
      } else {
        val msg = parseServerError(resp?.errorBody()?.string()) ?: "Unable to renew subscription."
        _state.update { it.copy(isMutating = false, errorMessage = msg) }
      }
    }
  }

  fun handlePaymentReturnDeepLink(
    paymentId: String = "",
    reference: String = "",
    paypalOrderId: String = "",
    status: String = "",
  ) {
    val active = _state.value.activeCheckout
    val effectivePaymentId = paymentId.ifBlank { active?.paymentId.orEmpty() }
    val effectiveRef = reference.ifBlank { active?.providerReference.orEmpty() }
    val effectiveOrder = paypalOrderId.ifBlank { active?.providerOrderId.orEmpty() }
    if (effectivePaymentId.isBlank() && effectiveRef.isBlank() && effectiveOrder.isBlank()) {
      return
    }
    if (status.equals("cancelled", ignoreCase = true)) {
      pollJob?.cancel()
      val prev = routeBackStack.removeLastOrNull() ?: SubscriptionsRoute.CREATOR_SUBSCRIBE
      _state.update {
        it.copy(
          currentRoute = prev,
          activeCheckout = null,
          statusMessage = "Payment cancelled before completion.",
        )
      }
      return
    }
    verifyCheckoutNow(effectivePaymentId, effectiveRef, effectiveOrder)
  }

  private fun verifyCheckoutNow(paymentId: String, reference: String, paypalOrderId: String) {
    val api = repository.getApiServiceOrNull() ?: return
    pollJob?.cancel()
    _state.update {
      it.copy(
        activeCheckout = it.activeCheckout?.copy(isVerifying = true),
        errorMessage = null,
      )
    }
    scope.launch {
      val token = repository.getStreamCredentials().first
      val fp = repository.getDeviceFingerprint()
      if (token.isBlank()) return@launch
      val verifyKey = "subver_${paymentId.ifBlank { reference.ifBlank { paypalOrderId } }}"
      val resp = runCatching {
        api.verifySubscriptionCheckout(
          bearer = "Bearer $token",
          deviceFingerprint = fp,
          idempotencyKey = verifyKey,
          body = SubscriptionCheckoutVerifyRequestDto(
            paymentId = paymentId,
            reference = reference,
            paypalOrderId = paypalOrderId,
            idempotencyKey = verifyKey,
          ),
        )
      }.getOrNull()
      val body = resp?.body()
      if (resp?.isSuccessful == true && body != null && body.success && body.payment.status == "completed") {
        val prev = routeBackStack.removeLastOrNull() ?: SubscriptionsRoute.CREATOR_SUBSCRIBE
        _state.update {
          it.copy(
            currentRoute = prev,
            activeCheckout = null,
            creatorOffering = body.offering.toModel(),
            statusMessage = "Subscription activated.",
            errorMessage = null,
          )
        }
        refreshHub(silent = true)
      } else {
        val msg = parseServerError(resp?.errorBody()?.string()) ?: "Unable to verify subscription payment."
        val prev = routeBackStack.removeLastOrNull() ?: SubscriptionsRoute.CREATOR_SUBSCRIBE
        _state.update {
          it.copy(
            currentRoute = prev,
            activeCheckout = null,
            errorMessage = msg,
          )
        }
      }
    }
  }

  private fun startVerificationPolling(session: SubscriptionCheckoutSession) {
    pollJob?.cancel()
    pollJob = scope.launch {
      val api = repository.getApiServiceOrNull() ?: return@launch
      repeat(24) {
        delay(3_000L)
        if (_state.value.currentRoute != SubscriptionsRoute.CHECKOUT_WEBVIEW) return@launch
        val token = repository.getStreamCredentials().first
        val fp = repository.getDeviceFingerprint()
        if (token.isBlank()) return@launch
        val verifyKey = "subpoll_${session.paymentId}"
        val resp = runCatching {
          api.verifySubscriptionCheckout(
            bearer = "Bearer $token",
            deviceFingerprint = fp,
            idempotencyKey = verifyKey,
            body = SubscriptionCheckoutVerifyRequestDto(
              paymentId = session.paymentId,
              reference = session.providerReference,
              paypalOrderId = session.providerOrderId,
              idempotencyKey = verifyKey,
            ),
          )
        }.getOrNull()
        val body = resp?.body()
        if (resp?.isSuccessful == true && body != null && body.success && body.payment.status == "completed") {
          val prev = routeBackStack.removeLastOrNull() ?: SubscriptionsRoute.CREATOR_SUBSCRIBE
          _state.update {
            it.copy(
              currentRoute = prev,
              activeCheckout = null,
              creatorOffering = body.offering.toModel(),
              statusMessage = "Subscription activated.",
            )
          }
          refreshHub(silent = true)
          return@launch
        }
      }
    }
  }

  fun onRealtimeSubscriptionUpdated() {
    if (_state.value.isOpen) {
      refreshHub(silent = true)
      val off = _state.value.creatorOffering
      if (off != null && _state.value.currentRoute == SubscriptionsRoute.CREATOR_SUBSCRIBE) {
        loadCreatorOffering(off.creatorUserId)
      }
    }
  }

  private fun applyHubResponse(body: SubscriptionsHubResponseDto) {
    val plan = body.myPlan.toModel()
    _state.update {
      it.copy(
        isLoading = false,
        countryIso = body.countryIso.ifBlank { it.countryIso },
        currencyCode = body.currencyCode.ifBlank { it.currencyCode },
        myPlan = plan,
        draftPlanEnabled = plan.enabled,
        draftMonthlyUsdCents = plan.monthlyUsdCents,
        draftMonthlyGoalUsdCents = plan.monthlyGoalUsdCents,
        draftAnnualDiscountPct = plan.annualDiscountPct,
        draftPerksSummary = plan.perksSummary.ifBlank { it.draftPerksSummary },
        subscribers = body.subscribers.map { sub -> sub.toModel() },
        activeSubscriptions = body.activeSubscriptions.map { sub -> sub.toModel() },
        goalPresets = body.goalPresets.map { p ->
          GoalPresetItem(p.amountUsdCents, p.usdLabel, p.localDisplay)
        }.ifEmpty { TelefamSubscriptionsCatalog.defaultGoalPresets(body.countryIso) },
      )
    }
  }

  private fun parseServerError(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return runCatching {
      val obj = JSONObject(raw)
      obj.optString("error").ifBlank { obj.optString("message").ifBlank { null } }
    }.getOrNull()
  }

  fun dispose() {
    pollJob?.cancel()
  }
}

private fun CreatorPlanDto.toModel(): CreatorPlanModel {
  return CreatorPlanModel(
    creatorUserId = creatorUserId,
    enabled = enabled,
    monthlyUsdCents = monthlyUsdCents,
    annualDiscountPct = annualDiscountPct,
    monthlyGoalUsdCents = monthlyGoalUsdCents,
    goalUsdDisplay = goalUsdDisplay,
    goalLocalDisplay = goalLocalDisplay,
    currentMrrUsdCents = currentMrrUsdCents,
    currentMrrUsdDisplay = currentMrrUsdDisplay,
    currentMrrLocalDisplay = currentMrrLocalDisplay,
    goalProgressPct = goalProgressPct.toFloat(),
    goalReached = goalReached,
    activeSubscriberCount = activeSubscriberCount,
    perksSummary = perksSummary,
    pricing = SubscriptionPricingModel(
      monthlyUsdCents = pricing.monthlyUsdCents,
      annualUsdCents = pricing.annualUsdCents,
      annualDiscountPct = pricing.annualDiscountPct,
      currencyCode = pricing.currencyCode,
      currencySymbol = pricing.currencySymbol,
      monthlyDisplay = pricing.monthlyDisplay,
      annualDisplay = pricing.annualDisplay,
      annualOriginalDisplay = pricing.annualOriginalDisplay,
      paystackMonthlyMinor = pricing.paystackMonthlyMinor,
      paystackAnnualMinor = pricing.paystackAnnualMinor,
      paystackCurrency = pricing.paystackCurrency,
      paypalMonthlyValue = pricing.paypalMonthlyValue,
      paypalAnnualValue = pricing.paypalAnnualValue,
      paypalCurrency = pricing.paypalCurrency,
    ),
    updatedAtMs = updatedAtMs,
  )
}

private fun SubscriptionRecordDto.toModel(): SubscriptionRecordModel {
  return SubscriptionRecordModel(
    subscriptionId = subscriptionId,
    subscriberUserId = subscriberUserId,
    subscriberName = subscriberName,
    subscriberUsername = subscriberUsername,
    subscriberAvatarUrl = subscriberAvatarUrl,
    subscriberVerifiedTier = subscriberVerifiedTier,
    creatorUserId = creatorUserId,
    creatorName = creatorName,
    creatorUsername = creatorUsername,
    creatorAvatarUrl = creatorAvatarUrl,
    creatorVerifiedTier = creatorVerifiedTier,
    billingCycle = SubscriptionBillingCycle.fromWire(billingCycle),
    status = status,
    autoRenew = autoRenew,
    amountUsdCents = amountUsdCents,
    amountDisplay = amountDisplay,
    provider = provider,
    startedAtMs = startedAtMs,
    currentPeriodEndMs = currentPeriodEndMs,
    cancelledAtMs = cancelledAtMs,
    canCancel = canCancel,
    canRenew = canRenew,
  )
}

private fun CreatorOfferingResponseDto.toModel(): CreatorOfferingModel {
  return CreatorOfferingModel(
    creatorUserId = creatorUserId,
    creatorName = creatorName,
    creatorUsername = creatorUsername,
    creatorAvatarUrl = creatorAvatarUrl,
    creatorVerifiedTier = creatorVerifiedTier,
    countryIso = countryIso,
    plan = plan.toModel(),
    viewerSubscription = viewerSubscription?.toModel(),
    isOwner = isOwner,
  )
}
