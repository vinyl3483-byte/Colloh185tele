package telefam.company

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import retrofit2.Response
import telefam.company.data.local.ChatConversationEntity
import telefam.company.data.local.DeviceIdentityStore
import telefam.company.data.local.TelefamDatabase
import telefam.company.data.network.NetworkQualityMonitor
import telefam.company.data.network.SendStarsRequestDto
import telefam.company.data.network.SendStarsResponseDto
import telefam.company.data.network.StarLedgerItemDto
import telefam.company.data.network.StarPaymentDto
import telefam.company.data.network.StarSenderSummaryDto
import telefam.company.data.network.StarWalletDto
import telefam.company.data.network.StarsCheckoutInitRequestDto
import telefam.company.data.network.StarsCheckoutInitResponseDto
import telefam.company.data.network.StarsCheckoutVerifyRequestDto
import telefam.company.data.network.StarsCheckoutVerifyResponseDto
import telefam.company.data.network.StarsSummaryResponseDto
import telefam.company.data.repository.TelefamRepository
import telefam.company.notifications.TelefamFcmService
import telefam.company.stars.SendStarsTargetPeer
import telefam.company.stars.StarPaymentProvider
import telefam.company.stars.TelefamStarsCatalog
import telefam.company.stars.TelefamStarsController
import telefam.company.stars.TelefamStarsRoute
import telefam.company.stars.TelefamStarsUiState
import telefam.company.ui.screens.TelefamStarsHostScreen
import telefam.company.ui.theme.TelefamTheme
import telefam.company.ui.theme.ThemeMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h915dp")
class TelefamStarsRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var db: TelefamDatabase
  private lateinit var identityStore: DeviceIdentityStore
  private lateinit var networkMonitor: NetworkQualityMonitor
  private lateinit var fakeApi: FakeStarsApi
  private lateinit var repository: TelefamRepository
  private lateinit var controllerScope: CoroutineScope
  private lateinit var starsController: TelefamStarsController

  @Before
  fun setUp(): Unit = runBlocking {
    context = ApplicationProvider.getApplicationContext()
    shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    db = Room.inMemoryDatabaseBuilder(context, TelefamDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    identityStore = DeviceIdentityStore(context)
    identityStore.saveEncryptedSession(
      userId = "user_alice",
      email = "alice@telefam.test",
      accessToken = "acc_alice",
      refreshToken = "ref_alice",
      accessExpiresInSec = 900L,
      refreshExpiresInSec = 2_592_000L,
      profileCompleted = true,
    )
    networkMonitor = NetworkQualityMonitor(context)
    fakeApi = FakeStarsApi()
    repository = TelefamRepository(
      appContext = context,
      dao = db.telefamDao(),
      identityStore = identityStore,
      networkMonitor = networkMonitor,
      apiService = fakeApi,
    )
    controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    starsController = TelefamStarsController(controllerScope, repository)
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    shadowOf(nm).setNotificationsEnabled(true)
  }

  @After
  fun tearDown() {
    controllerScope.cancel()
    runBlocking { repository.chatRepository.clearLocalChatSession() }
    shadowOf(Looper.getMainLooper()).idle()
    db.close()
  }

  private fun awaitCondition(timeoutMs: Long = 3_000L, predicate: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
      shadowOf(Looper.getMainLooper()).idle()
      if (predicate()) return
      Thread.sleep(20L)
    }
    shadowOf(Looper.getMainLooper()).idle()
    assertTrue("Timed out waiting for stars state condition", predicate())
  }

  private fun sampleConversation(): ChatConversationEntity {
    val now = System.currentTimeMillis()
    return ChatConversationEntity(
      conversationId = "conv_janedoe",
      peerUserId = "user_janedoe",
      peerFullName = "JaneDoe",
      peerUsername = "janedoe_254",
      peerAvatarUrl = "",
      lastMessageId = "msg_1",
      lastMessageType = "text",
      lastSenderId = "user_janedoe",
      lastMessagePreviewPlain = "Hey!",
      lastDeliveryStatus = "read",
      lastMessageAtMs = now,
      unreadCount = 0,
      isArchived = false,
      isMuted = false,
      isDeletedLocally = false,
      isOnline = true,
      lastSeenMs = now,
      lastSeenLabel = "online",
      canCall = true,
      canScreenshot = true,
      canShareChats = true,
      canCopyMessages = true,
      canDownloadMedia = true,
      showReadReceipts = true,
      showTypingIndicator = true,
      showOnlineIndicator = true,
      isBlocked = false,
      draftText = "",
      updatedAtMs = now,
    )
  }

  @Test
  fun catalogCurrencyConversionAndAnimationScalingMatchRequirements() {
    assertEquals("Ksh 0", TelefamStarsCatalog.formatCurrencyForStars(0L, "KE"))
    assertEquals("Ksh 5", TelefamStarsCatalog.formatCurrencyForStars(5L, "KE"))
    assertEquals("Ksh 100", TelefamStarsCatalog.formatCurrencyForStars(100L, "KE"))
    assertEquals("Ksh 50,000", TelefamStarsCatalog.formatCurrencyForStars(50_000L, "KE"))
    assertEquals("$0.80", TelefamStarsCatalog.formatCurrencyForStars(100L, "US"))
    assertEquals("\u20A61,200", TelefamStarsCatalog.formatCurrencyForStars(100L, "NG"))
    assertEquals("\u00A30.64", TelefamStarsCatalog.formatCurrencyForStars(100L, "GB"))

    assertEquals(1, TelefamStarsCatalog.animationTierForStars(5L))
    assertEquals(2, TelefamStarsCatalog.animationTierForStars(50L))
    assertEquals(3, TelefamStarsCatalog.animationTierForStars(200L))
    assertEquals(4, TelefamStarsCatalog.animationTierForStars(1_000L))
    assertEquals(5, TelefamStarsCatalog.animationTierForStars(10_000L))
    assertEquals(5, TelefamStarsCatalog.animationTierForStars(50_000L))

    val parsed = TelefamStarsCatalog.parseStarGiftMeta(
      """{"stars":500,"sender_user_id":"u1","sender_name":"Alice","recipient_user_id":"u2","recipient_name":"JaneDoe","animation_tier":3,"ledger_entry_id":"led_9"}""",
    )
    assertNotNull(parsed)
    assertEquals(500L, parsed!!.stars)
    assertEquals(3, parsed.animationTier)
    assertEquals("Sent 500 Stars", TelefamStarsCatalog.formatSummaryLine("""{"stars":500}"""))
  }

  @Test
  fun zeroBalanceInboxGiftRedirectsToBuyAndCompletesVerifiedIdempotentCheckoutAndSend() {
    val conv = sampleConversation()
    starsController.openSendStarsFromChat(conv, profileCountryIso = "KE")

    awaitCondition {
      starsController.state.value.isOpen &&
        starsController.state.value.currentRoute == TelefamStarsRoute.BUY_STARS
    }
    assertEquals("KE", starsController.state.value.countryIso)

    // Choose 100 Stars package -> navigates to Checkout
    starsController.selectPackageForCheckout(100L)
    assertEquals(TelefamStarsRoute.CHECKOUT, starsController.state.value.currentRoute)
    assertEquals(100L, starsController.state.value.selectedPackageStars)

    // Initiate Paystack checkout with idempotency key
    starsController.selectPaymentProvider(StarPaymentProvider.PAYSTACK)
    starsController.proceedToPay()

    awaitCondition {
      !starsController.state.value.isInitiatingCheckout &&
        starsController.state.value.activeCheckoutUrl == "https://checkout.paystack.com/telefam_test_ref"
    }
    assertEquals(1, fakeApi.recordedInitIdempKeys.size)
    assertTrue(fakeApi.recordedInitIdempKeys.first().isNotBlank())

    // Simulate payment return deep link -> verify payment -> returns to SEND_STARS with 100 balance
    starsController.handlePaymentReturnDeepLink(
      paymentId = "pay_100",
      reference = "ps_ref_100",
      status = "success",
    )

    awaitCondition {
      !starsController.state.value.isVerifyingPayment &&
        starsController.state.value.wallet.balanceStars == 100L &&
        starsController.state.value.currentRoute == TelefamStarsRoute.SEND_STARS
    }
    assertEquals(1, fakeApi.recordedVerifyIdempKeys.size)
    assertTrue(fakeApi.recordedVerifyIdempKeys.first().isNotBlank())

    // Now tap Send 500 (> 100 balance) -> redirects to Buy Stars with pendingReturnSendStarsAmount = 500
    starsController.sendStarsTier(500L)
    assertEquals(TelefamStarsRoute.BUY_STARS, starsController.state.value.currentRoute)
    assertEquals(500L, starsController.state.value.pendingReturnSendStarsAmount)

    // Buy 500 Stars via PayPal -> verify auto-resumes sending 500 Stars in chat
    starsController.selectPackageForCheckout(500L)
    starsController.selectPaymentProvider(StarPaymentProvider.PAYPAL)
    starsController.proceedToPay()
    awaitCondition {
      !starsController.state.value.isInitiatingCheckout &&
        starsController.state.value.activeCheckoutUrl == "https://www.sandbox.paypal.com/checkoutnow?token=pp_order_500"
    }

    starsController.handlePaymentReturnDeepLink(
      paymentId = "pay_500",
      paypalOrderId = "pp_order_500",
      status = "success",
    )

    awaitCondition {
      !starsController.state.value.isVerifyingPayment &&
        !starsController.state.value.isSendingStars &&
        starsController.state.value.celebrationStarsBurst == 500L
    }
    assertEquals(100L, starsController.state.value.wallet.balanceStars)
    assertEquals(500L, starsController.state.value.wallet.starsSentTotal)
    assertEquals(1, fakeApi.recordedSendIdempKeys.size)
    assertTrue(fakeApi.recordedSendIdempKeys.first().isNotBlank())
    assertFalse(starsController.state.value.isOpen)
  }

  @Test
  fun starsHubUiDisplaysBalanceSendersHistoryAndSkipsPayouts() {
    var hubTab by mutableIntStateOf(0)
    var currentRoute by mutableStateOf(TelefamStarsRoute.HUB)
    var selectedProvider by mutableStateOf(StarPaymentProvider.PAYSTACK)

    val uiState = TelefamStarsUiState(
      isOpen = true,
      routeStack = listOf(TelefamStarsRoute.HUB),
      countryIso = "KE",
      wallet = StarWalletDto(
        userId = "user_alice",
        balanceStars = 2_450L,
        starsReceivedTotal = 900L,
        starsSentTotal = 150L,
      ),
      senders = listOf(
        StarSenderSummaryDto(
          senderUserId = "user_bob",
          senderFullName = "Bob Supporter",
          senderUsername = "bob254",
          totalStarsSent = 900L,
          giftCount = 3,
        ),
      ),
      history = listOf(
        StarLedgerItemDto(
          entryId = "led_1",
          entryType = "received",
          deltaStars = 900L,
          balanceAfter = 2_450L,
          counterpartyUserId = "user_bob",
          counterpartyName = "Bob Supporter",
          provider = "wallet",
          createdAtMs = 1_732_000_000_000L,
        ),
      ),
      targetPeer = SendStarsTargetPeer(
        peerUserId = "user_janedoe",
        peerName = "JaneDoe",
        peerUsername = "janedoe_254",
        peerAvatarUrl = "",
        isOnline = true,
        conversationId = "conv_janedoe",
      ),
      selectedPackageStars = 100L,
    )

    composeTestRule.setContent {
      TelefamTheme(themeMode = ThemeMode.DARK) {
        val composedState = uiState.copy(
          routeStack = listOf(currentRoute),
          hubTabIndex = hubTab,
          selectedProvider = selectedProvider,
        )
        TelefamStarsHostScreen(
          state = composedState,
          onBack = {},
          onRefresh = {},
          onSelectHubTab = { hubTab = it },
          onOpenBuyStars = { currentRoute = TelefamStarsRoute.BUY_STARS },
          onSelectPackageForCheckout = { currentRoute = TelefamStarsRoute.CHECKOUT },
          onSendStarsTier = {},
          onSelectProvider = { selectedProvider = it },
          onProceedToPay = {},
          onCloseCheckoutWebView = {},
          onPaymentReturnCaptured = { _, _, _, _ -> },
          onOpenPeerProfile = {},
          onDismissToast = {},
        )
      }
    }

    composeTestRule.onNodeWithTag("stars_hub_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("stars_available_balance_text").assertIsDisplayed()
    composeTestRule.onNodeWithText("2,450 Stars").assertIsDisplayed()
    composeTestRule.onNodeWithText("Ksh 2,450").assertIsDisplayed()
    composeTestRule.onNodeWithTag("stars_received_tile").assertIsDisplayed()
    composeTestRule.onNodeWithTag("stars_sent_tile").assertIsDisplayed()
    composeTestRule.onNodeWithTag("stars_tab_senders").assertIsDisplayed()
    composeTestRule.onNodeWithTag("stars_tab_history").assertIsDisplayed()
    composeTestRule.onNodeWithText("Bob Supporter").assertIsDisplayed()

    // Confirm payouts are skipped cleanly
    assertTrue(composeTestRule.onAllNodesWithText("Payout", substring = true, ignoreCase = true).fetchSemanticsNodes().isEmpty())
    assertTrue(composeTestRule.onAllNodesWithText("Withdraw", substring = true, ignoreCase = true).fetchSemanticsNodes().isEmpty())

    // Navigate to Buy Stars -> Check Popular badge -> Checkout -> Paystack & PayPal cards
    composeTestRule.onNodeWithTag("stars_hub_buy_button").performClick()
    composeTestRule.onNodeWithTag("buy_stars_screen").assertIsDisplayed()
    composeTestRule.onNodeWithText("Popular").assertIsDisplayed()
    composeTestRule.onNodeWithTag("buy_star_btn_100").performScrollTo().performClick()

    composeTestRule.onNodeWithTag("stars_checkout_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("checkout_method_paystack").assertIsDisplayed()
    composeTestRule.onNodeWithTag("checkout_method_paypal").assertIsDisplayed()
    composeTestRule.onNodeWithTag("checkout_proceed_to_pay_button").assertIsDisplayed()
  }

  @Test
  fun starReceivedPushNotificationPersistsLocallyWithStarKind() = runBlocking {
    val fcmService = Robolectric.buildService(TelefamFcmService::class.java).create().get()
    val notifId = "notif_star_test_1"
    fcmService.handlePushPayload(
      repo = repository,
      data = mapOf(
        "type" to "star_received",
        "notification_id" to notifId,
        "recipient_user_id" to "user_alice",
        "actor_user_id" to "user_bob",
        "actor_name" to "Bob Supporter",
        "actor_username" to "bob254",
        "stars_amount" to "500",
        "conversation_id" to "conv_bob",
        "message_id" to "msg_star_1",
      ),
    )

    val notifications = db.telefamDao().observeNotifications("user_alice").first()
    val saved = notifications.firstOrNull { it.id == notifId }
    assertNotNull(saved)
    assertEquals("star_received", saved!!.kind)
    assertTrue(saved.body.contains("500"))
    assertEquals("conv_bob", saved.conversationId)
  }

  private class FakeStarsApi : AbstractTestChatApiStub() {
    var balanceStars: Long = 0L
    var starsReceivedTotal: Long = 0L
    var starsSentTotal: Long = 0L
    val recordedInitIdempKeys = mutableListOf<String>()
    val recordedVerifyIdempKeys = mutableListOf<String>()
    val recordedSendIdempKeys = mutableListOf<String>()
    private val pendingPackages = mutableMapOf<String, Long>()

    private fun walletDto() = StarWalletDto(
      userId = "user_alice",
      balanceStars = balanceStars,
      starsReceivedTotal = starsReceivedTotal,
      starsSentTotal = starsSentTotal,
      updatedAtMs = System.currentTimeMillis(),
    )

    override suspend fun getStarsSummary(
      bearer: String,
      deviceFingerprint: String,
      countryIso: String,
    ): Response<StarsSummaryResponseDto> {
      return Response.success(
        StarsSummaryResponseDto(
          success = true,
          wallet = walletDto(),
          countryIso = countryIso.ifBlank { "KE" },
          serverEpochMs = System.currentTimeMillis(),
        ),
      )
    }

    override suspend fun initStarsCheckout(
      bearer: String,
      deviceFingerprint: String,
      idempotencyKey: String,
      body: StarsCheckoutInitRequestDto,
    ): Response<StarsCheckoutInitResponseDto> {
      recordedInitIdempKeys.add(idempotencyKey)
      val payId = "pay_${body.packageStars}"
      pendingPackages[payId] = body.packageStars
      val url = if (body.provider == "paypal") {
        "https://www.sandbox.paypal.com/checkoutnow?token=pp_order_${body.packageStars}"
      } else {
        "https://checkout.paystack.com/telefam_test_ref"
      }
      return Response.success(
        StarsCheckoutInitResponseDto(
          success = true,
          payment = StarPaymentDto(
            paymentId = payId,
            provider = body.provider,
            providerReference = "ps_ref_${body.packageStars}",
            providerOrderId = if (body.provider == "paypal") "pp_order_${body.packageStars}" else "",
            packageStars = body.packageStars,
            expectedAmountMinor = body.packageStars * 100L,
            expectedCurrency = "KES",
            status = "pending",
            checkoutUrl = url,
            purpose = body.purpose,
            peerUserId = body.peerUserId,
            conversationId = body.conversationId,
          ),
          wallet = walletDto(),
          returnUrl = "telefam://stars/payment-return",
        ),
      )
    }

    override suspend fun verifyStarsCheckout(
      bearer: String,
      deviceFingerprint: String,
      idempotencyKey: String,
      body: StarsCheckoutVerifyRequestDto,
    ): Response<StarsCheckoutVerifyResponseDto> {
      recordedVerifyIdempKeys.add(idempotencyKey)
      val pkg = pendingPackages[body.paymentId] ?: 100L
      balanceStars += pkg
      return Response.success(
        StarsCheckoutVerifyResponseDto(
          success = true,
          credited = true,
          payment = StarPaymentDto(
            paymentId = body.paymentId,
            packageStars = pkg,
            status = "succeeded",
            verifiedVia = "api_verify",
          ),
          wallet = walletDto(),
          serverEpochMs = System.currentTimeMillis(),
        ),
      )
    }

    override suspend fun sendStarsInChat(
      bearer: String,
      deviceFingerprint: String,
      idempotencyKey: String,
      body: SendStarsRequestDto,
    ): Response<SendStarsResponseDto> {
      recordedSendIdempKeys.add(idempotencyKey)
      if (balanceStars < body.stars) {
        return Response.error(
          402,
          """{"error":"Insufficient balance"}""".toResponseBody("application/json".toMediaType()),
        )
      }
      balanceStars -= body.stars
      starsSentTotal += body.stars
      return Response.success(
        SendStarsResponseDto(
          success = true,
          messageId = "msg_star_${body.stars}",
          conversationId = body.conversationId,
          starsSent = body.stars,
          senderWallet = walletDto(),
          serverEpochMs = System.currentTimeMillis(),
        ),
      )
    }
  }
}
