package telefam.company.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.telefam.company.app.R
import telefam.company.core.SmartTextDetector
import telefam.company.data.network.LivenessStepTelemetryDto
import telefam.company.data.network.TierPricingDto
import telefam.company.ui.components.PayPalVectorLogo
import telefam.company.ui.components.PaystackVectorLogo
import telefam.company.ui.components.TelefamVerifiedBadgeIcon
import telefam.company.ui.components.VerificationBenefitVectorIcon
import telefam.company.ui.theme.LocalTelefamDarkTheme
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamCrimsonBright
import telefam.company.verification.TelefamVerificationCatalog
import telefam.company.verification.TelefamVerificationUiState
import telefam.company.verification.VerificationBenefitItem
import telefam.company.verification.VerificationBillingCycle
import telefam.company.verification.VerificationDocumentType
import telefam.company.verification.VerificationPaymentProvider
import telefam.company.verification.VerificationRoute
import telefam.company.verification.VerificationTierKind

@Composable
fun TelefamVerificationHostScreen(
  state: TelefamVerificationUiState,
  onBack: () -> Unit,
  onDismissTutorial: () -> Unit,
  onReopenTutorial: () -> Unit,
  onSelectTier: (VerificationTierKind) -> Unit,
  onToggleBillingCycle: (Boolean) -> Unit,
  onApplyTier: (VerificationTierKind) -> Unit,
  onActivateFreeTrial: () -> Unit,
  onSelectProvider: (VerificationPaymentProvider) -> Unit,
  onProceedToSubscribePay: () -> Unit,
  onCloseCheckoutWebView: (Boolean) -> Unit,
  onPaymentReturnCaptured: (paymentId: String, reference: String, paypalOrderId: String, status: String) -> Unit,
  onProceedFromSelfieGuidance: () -> Unit,
  onLiveSelfieProgress: (Int, Int, Boolean, Boolean, String) -> Unit,
  onSubmitLiveSelfie: (ByteArray, List<LivenessStepTelemetryDto>) -> Unit,
  onSelectDocType: (VerificationDocumentType) -> Unit,
  onStartLiveIdCapture: () -> Unit,
  onLiveDocReadiness: (Boolean, Boolean, Boolean, Boolean, Boolean, Int) -> Unit,
  onSubmitLiveDocument: (ByteArray, String, Double, Double, Double, Boolean) -> Unit,
  onRefreshStatus: () -> Unit,
  onOpenTiersExplorer: () -> Unit,
  onDismissBanner: () -> Unit,
) {
  BackHandler(enabled = true) {
    if (!state.activeCheckoutUrl.isNullOrBlank()) {
      onCloseCheckoutWebView(true)
    } else {
      onBack()
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .testTag("verification_host_screen"),
  ) {
    when (state.currentRoute) {
      VerificationRoute.TIERS_PAGER -> VerificationTiersPagerScreen(
        state = state,
        onBack = onBack,
        onDismissTutorial = onDismissTutorial,
        onReopenTutorial = onReopenTutorial,
        onSelectTier = onSelectTier,
        onToggleBillingCycle = onToggleBillingCycle,
        onApplyTier = onApplyTier,
      )

      VerificationRoute.TRIAL_AND_PAYMENT -> VerificationTrialAndCheckoutScreen(
        state = state,
        onBack = onBack,
        onToggleBillingCycle = onToggleBillingCycle,
        onActivateFreeTrial = onActivateFreeTrial,
        onSelectProvider = onSelectProvider,
        onSubscribePay = onProceedToSubscribePay,
      )

      VerificationRoute.SELFIE_GUIDANCE -> VerificationSelfieGuidanceScreen(
        state = state,
        onBack = onBack,
        onStartCamera = onProceedFromSelfieGuidance,
      )

      VerificationRoute.SELFIE_LIVENESS -> VerificationSelfieLivenessScreen(
        state = state,
        onBack = onBack,
        onProgress = onLiveSelfieProgress,
        onAutoCaptured = onSubmitLiveSelfie,
      )

      VerificationRoute.ID_GUIDANCE_AND_SELECT -> VerificationIdGuidanceAndSelectScreen(
        state = state,
        onBack = onBack,
        onSelectDocType = onSelectDocType,
        onStartIdScan = onStartLiveIdCapture,
      )

      VerificationRoute.ID_LIVE_CAPTURE -> VerificationIdLiveCaptureScreen(
        state = state,
        onBack = onBack,
        onReadiness = onLiveDocReadiness,
        onAutoCaptured = onSubmitLiveDocument,
      )

      VerificationRoute.STATUS_REVIEW -> VerificationStatusReviewScreen(
        state = state,
        onBack = onBack,
        onRefresh = onRefreshStatus,
        onResumeSteps = onOpenTiersExplorer,
      )
    }

    val checkoutUrl = state.activeCheckoutUrl
    if (!checkoutUrl.isNullOrBlank()) {
      VerificationPaymentWebViewOverlay(
        checkoutUrl = checkoutUrl,
        providerName = if (state.selectedProvider == VerificationPaymentProvider.PAYSTACK) {
          stringResource(R.string.stars_paystack_title)
        } else {
          stringResource(R.string.stars_paypal_title)
        },
        onReturnUrlIntercepted = onPaymentReturnCaptured,
        onClose = { onCloseCheckoutWebView(true) },
      )
    }

    if (state.isVerifyingPayment) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.56f))
          .clickable(enabled = false) {},
        contentAlignment = Alignment.Center,
      ) {
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
          ) {
            CircularProgressIndicator(color = TelefamCrimson, modifier = Modifier.size(34.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.stars_verifying_payment), fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }

    LaunchedEffect(state.statusMessage, state.errorMessage) {
      if (state.errorMessage.isNullOrBlank() && !state.statusMessage.isNullOrBlank()) {
        kotlinx.coroutines.delay(3_000L)
        onDismissBanner()
      }
    }

    val banner = state.errorMessage ?: state.statusMessage
    if (!banner.isNullOrBlank()) {
      val isErr = !state.errorMessage.isNullOrBlank()
      Surface(
        onClick = onDismissBanner,
        shape = CircleShape,
        color = if (isErr) Color(0xFFD91E2A) else Color(0xEB151821),
        shadowElevation = 6.dp,
        modifier = Modifier
          .align(Alignment.TopCenter)
          .statusBarsPadding()
          .padding(start = 20.dp, end = 20.dp, top = 74.dp)
          .testTag("verification_status_banner"),
      ) {
        Text(
          text = banner,
          color = Color.White,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
        )
      }
    }
  }
}

@Composable
private fun VerificationTiersPagerScreen(
  state: TelefamVerificationUiState,
  onBack: () -> Unit,
  onDismissTutorial: () -> Unit,
  onReopenTutorial: () -> Unit,
  onSelectTier: (VerificationTierKind) -> Unit,
  onToggleBillingCycle: (Boolean) -> Unit,
  onApplyTier: (VerificationTierKind) -> Unit,
) {
  val tiers = remember {
    listOf(VerificationTierKind.BUSINESS, VerificationTierKind.GOVERNMENT, VerificationTierKind.BLUE)
  }
  val initialIdx = tiers.indexOf(state.selectedTier).coerceAtLeast(0)
  val pagerState = rememberPagerState(initialPage = initialIdx, pageCount = { tiers.size })

  LaunchedEffect(pagerState.currentPage) {
    val active = tiers[pagerState.currentPage]
    if (active != state.selectedTier) onSelectTier(active)
  }

  val activeTier = tiers[pagerState.currentPage]
  val isDark = LocalTelefamDarkTheme.current
  val pageBg = if (isDark) Color(0xFF0E1015) else Color(0xFFF6F7FA)

  Scaffold(
    containerColor = pageBg,
    modifier = Modifier.fillMaxSize(),
    topBar = {
      VerificationCrimsonHeader(
        title = stringResource(R.string.ver_title),
        subtitle = activeTier.headerSubtitle,
        onBack = onBack,
        trailingContent = {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
              tiers.forEachIndexed { idx, _ ->
                Box(
                  modifier = Modifier
                    .size(width = if (idx == pagerState.currentPage) 16.dp else 6.dp, height = 6.dp)
                    .clip(CircleShape)
                    .background(if (idx == pagerState.currentPage) Color.White else Color.White.copy(alpha = 0.45f)),
                )
              }
            }
            IconButton(
              onClick = onReopenTutorial,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("verification_reopen_tutorial"),
            ) {
              Icon(Icons.Filled.Info, contentDescription = "Verification guide", tint = Color.White)
            }
          }
        },
      )
    },
  ) { inner ->
    Box(modifier = Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.TopCenter) {
      HorizontalPager(
        state = pagerState,
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 680.dp)
          .testTag("verification_tier_pager"),
      ) { pageIdx ->
        val tier = tiers[pageIdx]
        val pricing = TelefamVerificationCatalog.pricingForTier(tier, state.countryIso, state.status.tiers)
        VerificationTierPage(
          tier = tier,
          pricing = pricing,
          isAnnual = state.billingCycle == VerificationBillingCycle.ANNUAL,
          profileFullName = state.profileFullName,
          profileUsername = state.profileUsername,
          profileAvatarUrl = state.profileAvatarUrl,
          onToggleAnnual = onToggleBillingCycle,
          onApplyOrSubscribe = { onApplyTier(tier) },
        )
      }

      if (state.showFirstOpenTutorial) {
        VerificationFirstOpenTutorialOverlay(onDismiss = onDismissTutorial)
      }
    }
  }
}

@Composable
private fun VerificationTierPage(
  tier: VerificationTierKind,
  pricing: TierPricingDto,
  isAnnual: Boolean,
  profileFullName: String,
  profileUsername: String,
  profileAvatarUrl: String,
  onToggleAnnual: (Boolean) -> Unit,
  onApplyOrSubscribe: () -> Unit,
) {
  val isDark = LocalTelefamDarkTheme.current
  val cardBg = if (isDark) Color(0xFF171A21) else Color(0xFFFFFFFF)
  val tintBg = if (isDark) Color(0xFF1E171B) else Color(0xFFFCF3F4)
  val border = if (isDark) Color(0xFF2B303C) else Color(0xFFF0E4E6)
  val primaryText = if (isDark) Color(0xFFF4F6FA) else Color(0xFF141821)
  val mutedText = if (isDark) Color(0xFF9BA4B5) else Color(0xFF667085)
  val crimson = if (isDark) TelefamCrimsonBright else TelefamCrimson
  var readMoreExpanded by remember(tier) { mutableStateOf(false) }

  val defaultName = when (tier) {
    VerificationTierKind.BUSINESS -> profileFullName.ifBlank { "Telefam Business" }
    VerificationTierKind.GOVERNMENT -> profileFullName.ifBlank { "Government Official" }
    VerificationTierKind.BLUE -> profileFullName.ifBlank { "Telefam Member" }
  }
  val defaultHandle = when {
    profileUsername.isNotBlank() -> "@${profileUsername.removePrefix("@")}"
    tier == VerificationTierKind.BUSINESS -> "@business_account"
    tier == VerificationTierKind.GOVERNMENT -> "@gov_official"
    else -> "@verified_member"
  }
  val tagline = when (tier) {
    VerificationTierKind.BUSINESS -> "Trusted. Professional. Always Open.\nYour business, our priority."
    VerificationTierKind.GOVERNMENT -> "Verified government account. Serving the people, building a better tomorrow."
    VerificationTierKind.BLUE -> "Verified for a safer, more trusted experience."
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("verification_tier_list_${tier.wire}"),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Tier identity hero
    item(key = "hero") {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        TierPreviewAvatar(
          tier = tier,
          avatarUrl = profileAvatarUrl,
          isDark = isDark,
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = defaultName,
              color = primaryText,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 18.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )
            TelefamVerifiedBadgeIcon(tier = tier.wire, size = 20.dp, isDarkTheme = isDark)
          }
          Text(text = defaultHandle, color = mutedText, fontSize = 13.sp)
          Spacer(modifier = Modifier.height(4.dp))
          Text(text = tagline, color = mutedText, fontSize = 12.5.sp, lineHeight = 17.sp)
        }
      }
    }

    // Pricing + Annual 20% OFF toggle card matching screenshots
    item(key = "pricing") {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = tintBg,
        border = BorderStroke(1.dp, border),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
          val emblemTint = when (tier) {
            VerificationTierKind.BUSINESS -> Color(0xFFE0A300)
            VerificationTierKind.GOVERNMENT -> crimson
            VerificationTierKind.BLUE -> Color(0xFF1995ED)
          }
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              VerificationBenefitVectorIcon(
                iconKey = if (tier == VerificationTierKind.BUSINESS) "star" else "badge",
                tint = emblemTint,
                size = 22.dp,
              )
              Text(
                text = if (isAnnual) "${pricing.annualDisplay} / year" else "${pricing.monthlyDisplay} / month",
                color = primaryText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = if (isAnnual) "Billed annually (${pricing.currencyCode})" else stringResource(R.string.ver_billed_monthly),
              color = mutedText,
              fontSize = 12.sp,
            )
          }
          Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Surface(shape = CircleShape, color = crimson) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                ) {
                  Text(stringResource(R.string.ver_annual), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                  Spacer(modifier = Modifier.width(4.dp))
                  Switch(
                    checked = isAnnual,
                    onCheckedChange = onToggleAnnual,
                    colors = SwitchDefaults.colors(
                      checkedThumbColor = Color.White,
                      checkedTrackColor = Color(0xFF9E0B10),
                      uncheckedThumbColor = Color.White,
                      uncheckedTrackColor = Color.White.copy(alpha = 0.4f),
                    ),
                    modifier = Modifier.height(26.dp).testTag("verification_annual_toggle"),
                  )
                }
              }
              Surface(shape = CircleShape, color = crimson) {
                Text(
                  text = stringResource(R.string.ver_off_20),
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp,
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                )
              }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "${pricing.annualDisplay} / year",
              color = primaryText,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
            )
            Text(
              text = pricing.annualOriginalDisplay,
              color = mutedText,
              fontSize = 11.5.sp,
              textDecoration = TextDecoration.LineThrough,
            )
          }
        }
      }
    }

    // Benefits header & items
    item(key = "benefits_header") {
      val sectionTitle = when (tier) {
        VerificationTierKind.BUSINESS -> "Business Verification Benefits"
        VerificationTierKind.GOVERNMENT -> "Government Verification Benefits"
        VerificationTierKind.BLUE -> "Verification Benefits"
      }
      Text(text = sectionTitle, color = primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
    }

    if (tier == VerificationTierKind.BLUE) {
      items(TelefamVerificationCatalog.blueBenefits.size, key = { "blue_$it" }) { idx ->
        val item = TelefamVerificationCatalog.blueBenefits[idx]
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = cardBg,
          border = BorderStroke(1.dp, border),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(tintBg),
            ) {
              VerificationBenefitVectorIcon(iconKey = item.iconKey, tint = crimson, size = 19.dp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(item.title, color = primaryText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
              Text(item.description, color = mutedText, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = mutedText)
          }
        }
      }
    } else {
      val list = if (tier == VerificationTierKind.BUSINESS) {
        TelefamVerificationCatalog.businessBenefits
      } else {
        TelefamVerificationCatalog.governmentBenefits
      }
      val rows = list.chunked(2)
      items(rows.size, key = { "grid_${tier.wire}_$it" }) { rowIdx ->
        val pair = rows[rowIdx]
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
          pair.forEach { b ->
            BenefitGridCell(b, tintBg, crimson, primaryText, mutedText, modifier = Modifier.weight(1f))
          }
          if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
        }
      }

      item(key = "why_accordion") {
        val whyTitle = if (tier == VerificationTierKind.BUSINESS) {
          "Why Go Business?"
        } else {
          "Why Verify as a Government Official?"
        }
        val whySummary = if (tier == VerificationTierKind.BUSINESS) {
          "Build trust, grow your brand, and connect with more customers with a verified business badge."
        } else {
          "Build trust, increase transparency, and connect with citizens with an official verified badge."
        }
        Surface(
          onClick = { readMoreExpanded = !readMoreExpanded },
          shape = RoundedCornerShape(14.dp),
          color = tintBg,
          border = BorderStroke(1.dp, border),
          modifier = Modifier.fillMaxWidth().testTag("verification_read_more"),
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(30.dp).clip(CircleShape).background(cardBg),
              ) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = crimson, modifier = Modifier.size(18.dp))
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(whyTitle, color = primaryText, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                Text(whySummary, color = mutedText, fontSize = 11.5.sp, lineHeight = 15.5.sp)
              }
              Spacer(modifier = Modifier.width(6.dp))
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Read More", color = crimson, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                Icon(
                  if (readMoreExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                  contentDescription = null,
                  tint = crimson,
                  modifier = Modifier.size(16.dp),
                )
              }
            }
            AnimatedVisibility(visible = readMoreExpanded) {
              Text(
                text = "Includes a 7-day free trial, server-verified live facial check, official document OCR matching, and full secure deletion of captured verification photos after processing.",
                color = mutedText,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 10.dp),
              )
            }
          }
        }
      }
    }

    // Action CTA
    item(key = "cta") {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
      ) {
        Button(
          onClick = onApplyOrSubscribe,
          shape = CircleShape,
          colors = ButtonDefaults.buttonColors(containerColor = crimson, contentColor = Color.White),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag(if (tier == VerificationTierKind.BLUE) "verification_subscribe_button" else "verification_apply_button"),
        ) {
          Text(
            text = if (tier == VerificationTierKind.BLUE) stringResource(R.string.ver_subscribe_now) else "${stringResource(R.string.ver_apply_now)} ->",
            fontWeight = FontWeight.Bold,
            fontSize = 15.5.sp,
          )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = if (tier == VerificationTierKind.BLUE) {
            stringResource(R.string.ver_cancel_anytime)
          } else if (tier == VerificationTierKind.BUSINESS) {
            "First week free · Secure · Trusted · Professional"
          } else {
            "First week free · Secure · Official · Trusted"
          },
          color = mutedText,
          fontSize = 11.5.sp,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(14.dp))
      }
    }
  }
}

@Composable
private fun BenefitGridCell(
  item: VerificationBenefitItem,
  tintBg: Color,
  crimson: Color,
  primaryText: Color,
  mutedText: Color,
  modifier: Modifier = Modifier,
) {
  Row(verticalAlignment = Alignment.Top, modifier = modifier) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(tintBg),
    ) {
      VerificationBenefitVectorIcon(iconKey = item.iconKey, tint = crimson, size = 18.dp)
    }
    Spacer(modifier = Modifier.width(8.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(item.title, color = primaryText, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
      Text(item.description, color = mutedText, fontSize = 11.sp, lineHeight = 14.5.sp)
    }
  }
}

@Composable
private fun TierPreviewAvatar(
  tier: VerificationTierKind,
  avatarUrl: String,
  isDark: Boolean,
) {
  val ringColor = when (tier) {
    VerificationTierKind.BUSINESS -> Color(0xFFE0A300)
    VerificationTierKind.GOVERNMENT -> TelefamCrimson
    VerificationTierKind.BLUE -> Color(0xFF1995ED)
  }
  Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.size(72.dp)) {
    Surface(
      shape = CircleShape,
      color = if (isDark) Color(0xFF1E222B) else Color(0xFF151821),
      border = BorderStroke(2.5.dp, ringColor),
      modifier = Modifier.fillMaxSize(),
    ) {
      if (avatarUrl.isNotBlank() && tier == VerificationTierKind.BLUE) {
        AsyncImage(
          model = avatarUrl,
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
          VerificationBenefitVectorIcon(
            iconKey = when (tier) {
              VerificationTierKind.BUSINESS -> "star"
              VerificationTierKind.GOVERNMENT -> "security"
              VerificationTierKind.BLUE -> "badge"
            },
            tint = ringColor,
            size = 30.dp,
          )
        }
      }
    }
    TelefamVerifiedBadgeIcon(tier = tier.wire, size = 22.dp, isDarkTheme = isDark)
  }
}

@Composable
private fun VerificationFirstOpenTutorialOverlay(onDismiss: () -> Unit) {
  var step by remember { mutableIntStateOf(0) }
  val steps = remember {
    listOf(
      "Swipe sideways across tiers" to "Browse Business, Government, and Blue badges with automatic currency conversion for your country.",
      "Unlock 1 week free" to "Start with a 7-day free trial or subscribe securely via Paystack or PayPal.",
      "Live selfie & ID verification" to "Complete a guided head movement selfie check and live document scan. Images are purged securely after verification.",
    )
  }
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.62f))
      .padding(22.dp)
      .testTag("verification_tutorial_overlay"),
  ) {
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(22.dp),
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          TelefamVerifiedBadgeIcon("business", size = 28.dp)
          TelefamVerifiedBadgeIcon("government", size = 28.dp)
          TelefamVerifiedBadgeIcon("blue", size = 28.dp)
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = steps[step].first,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 18.sp,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = steps[step].second,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 13.5.sp,
          lineHeight = 19.sp,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          repeat(steps.size) { i ->
            Box(
              modifier = Modifier
                .size(width = if (i == step) 18.dp else 7.dp, height = 7.dp)
                .clip(CircleShape)
                .background(if (i == step) TelefamCrimson else Color.Gray.copy(alpha = 0.35f)),
            )
          }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          OutlinedButton(
            onClick = onDismiss,
            shape = CircleShape,
            modifier = Modifier.weight(1f).height(48.dp).testTag("verification_tutorial_skip"),
          ) {
            Text("Skip")
          }
          Button(
            onClick = { if (step < steps.lastIndex) step += 1 else onDismiss() },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = TelefamCrimson, contentColor = Color.White),
            modifier = Modifier.weight(1f).height(48.dp).testTag("verification_tutorial_dismiss"),
          ) {
            Text(if (step < steps.lastIndex) "Next" else stringResource(R.string.ver_tutorial_got_it))
          }
        }
      }
    }
  }
}

@Composable
private fun VerificationTrialAndCheckoutScreen(
  state: TelefamVerificationUiState,
  onBack: () -> Unit,
  onToggleBillingCycle: (Boolean) -> Unit,
  onActivateFreeTrial: () -> Unit,
  onSelectProvider: (VerificationPaymentProvider) -> Unit,
  onSubscribePay: () -> Unit,
) {
  val isDark = LocalTelefamDarkTheme.current
  val pageBg = if (isDark) Color(0xFF0E1015) else Color(0xFFF6F7FA)
  val cardBg = if (isDark) Color(0xFF171A21) else Color(0xFFFFFFFF)
  val border = if (isDark) Color(0xFF2B303C) else Color(0xFFE5E8EF)
  val primaryText = if (isDark) Color(0xFFF4F6FA) else Color(0xFF141821)
  val mutedText = if (isDark) Color(0xFF9BA4B5) else Color(0xFF667085)
  val crimson = if (isDark) TelefamCrimsonBright else TelefamCrimson
  val pricing = TelefamVerificationCatalog.pricingForTier(state.selectedTier, state.countryIso, state.status.tiers)
  val isAnnual = state.billingCycle == VerificationBillingCycle.ANNUAL

  Scaffold(
    containerColor = pageBg,
    topBar = {
      VerificationCrimsonHeader(
        title = state.selectedTier.headerTitle,
        subtitle = stringResource(R.string.ver_how_it_works),
        onBack = onBack,
      )
    },
  ) { inner ->
    Box(modifier = Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.TopCenter) {
      LazyColumn(
        modifier = Modifier.fillMaxSize().widthIn(max = 680.dp).testTag("verification_trial_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // First Week Free Card
        item(key = "trial_card") {
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isDark) Color(0xFF1F161A) else Color(0xFFFFF5F6),
            border = BorderStroke(1.5.dp, crimson),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TelefamVerifiedBadgeIcon(tier = state.selectedTier.wire, size = 28.dp, isDarkTheme = isDark)
                Column(modifier = Modifier.weight(1f)) {
                  Text(stringResource(R.string.ver_trial_offer_title), color = primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                  Text(
                    text = if (state.status.trialEligible) {
                      stringResource(R.string.ver_trial_offer_sub)
                    } else {
                      "One-week trial already used on this account. Subscribe below to verify."
                    },
                    color = mutedText,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                  )
                }
              }
              if (state.status.trialEligible) {
                Button(
                  onClick = onActivateFreeTrial,
                  enabled = !state.isActivatingTrial,
                  shape = CircleShape,
                  colors = ButtonDefaults.buttonColors(containerColor = crimson, contentColor = Color.White),
                  modifier = Modifier.fillMaxWidth().height(50.dp).testTag("verification_trial_activate_button"),
                ) {
                  if (state.isActivatingTrial) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                  } else {
                    Text(stringResource(R.string.ver_activate_trial), fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                  }
                }
              }
            }
          }
        }

        // How verification works guidance
        item(key = "how_works") {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = cardBg,
            border = BorderStroke(1.dp, border),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text(stringResource(R.string.ver_how_it_works), color = primaryText, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
              val steps = listOf(
                "1. Start your 7-day free trial or subscribe via Paystack or PayPal.",
                "2. Complete live guided head-movement selfie verification (screen recording blocked).",
                "3. Scan a valid, unexpired government or business ID with live auto-capture.",
                "4. Your profile updates under review (up to 3 working days). All captured photos are deleted securely.",
              )
              steps.forEach { s ->
                Text(s, color = mutedText, fontSize = 13.sp, lineHeight = 18.sp)
              }
            }
          }
        }

        // Billing cycle + Provider selection
        item(key = "checkout_options") {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = cardBg,
            border = BorderStroke(1.dp, border),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = if (isAnnual) "${pricing.annualDisplay} / year" else "${pricing.monthlyDisplay} / month",
                    color = primaryText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                  )
                  Text(
                    text = if (isAnnual) "Includes 20% annual discount (${pricing.currencyCode})" else "Monthly billing (${pricing.currencyCode})",
                    color = mutedText,
                    fontSize = 12.sp,
                  )
                }
                Switch(
                  checked = isAnnual,
                  onCheckedChange = onToggleBillingCycle,
                  colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = crimson),
                )
              }

              VerificationProviderCard(
                title = stringResource(R.string.stars_paystack_title),
                subtitle = stringResource(R.string.stars_paystack_sub),
                selected = state.selectedProvider == VerificationPaymentProvider.PAYSTACK,
                cardBg = cardBg,
                border = border,
                crimson = crimson,
                primaryText = primaryText,
                mutedText = mutedText,
                icon = { PaystackVectorLogo(modifier = Modifier.size(34.dp)) },
                onClick = { onSelectProvider(VerificationPaymentProvider.PAYSTACK) },
                tag = "verification_provider_paystack",
              )

              VerificationProviderCard(
                title = stringResource(R.string.stars_paypal_title),
                subtitle = stringResource(R.string.stars_paypal_sub),
                selected = state.selectedProvider == VerificationPaymentProvider.PAYPAL,
                cardBg = cardBg,
                border = border,
                crimson = crimson,
                primaryText = primaryText,
                mutedText = mutedText,
                icon = { PayPalVectorLogo(modifier = Modifier.size(34.dp)) },
                onClick = { onSelectProvider(VerificationPaymentProvider.PAYPAL) },
                tag = "verification_provider_paypal",
              )

              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = crimson, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.stars_security_sub), color = mutedText, fontSize = 11.5.sp)
              }

              Button(
                onClick = onSubscribePay,
                enabled = !state.isInitiatingCheckout && !state.isVerifyingPayment,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = crimson, contentColor = Color.White),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(50.dp)
                  .navigationBarsPadding()
                  .testTag("verification_checkout_pay_button"),
              ) {
                if (state.isInitiatingCheckout) {
                  CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                  Text(stringResource(R.string.stars_proceed_to_pay), fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
private fun VerificationProviderCard(
  title: String,
  subtitle: String,
  selected: Boolean,
  cardBg: Color,
  border: Color,
  crimson: Color,
  primaryText: Color,
  mutedText: Color,
  icon: @Composable () -> Unit,
  onClick: () -> Unit,
  tag: String,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(14.dp),
    color = cardBg,
    border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) crimson else border),
    modifier = Modifier.fillMaxWidth().testTag(tag),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
      icon()
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(title, color = primaryText, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
        Text(subtitle, color = mutedText, fontSize = 11.5.sp, lineHeight = 15.sp)
      }
      RadioButton(
        selected = selected,
        onClick = onClick,
        colors = RadioButtonDefaults.colors(selectedColor = crimson, unselectedColor = mutedText),
      )
    }
  }
}

@Composable
internal fun VerificationCrimsonHeader(
  title: String,
  subtitle: String,
  onBack: () -> Unit,
  trailingContent: @Composable () -> Unit = {},
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(Brush.horizontalGradient(listOf(Color(0xFFC50D12), Color(0xFFEF1A22))))
      .statusBarsPadding()
      .padding(horizontal = 8.dp, vertical = 10.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
      IconButton(
        onClick = onBack,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("verification_back_button"),
      ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = Color.White)
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        if (subtitle.isNotBlank()) {
          Text(subtitle, color = Color.White.copy(alpha = 0.90f), fontSize = 12.5.sp)
        }
      }
      trailingContent()
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun VerificationPaymentWebViewOverlay(
  checkoutUrl: String,
  providerName: String,
  onReturnUrlIntercepted: (paymentId: String, reference: String, paypalOrderId: String, status: String) -> Unit,
  onClose: () -> Unit,
) {
  val safeUrl = remember(checkoutUrl) { SmartTextDetector.normalizeSafeHttpUrl(checkoutUrl) ?: "about:blank" }
  var progress by remember { mutableIntStateOf(0) }
  var isLoading by remember { mutableStateOf(true) }
  var webViewRef by remember { mutableStateOf<WebView?>(null) }

  DisposableEffect(Unit) {
    onDispose {
      webViewRef?.apply {
        stopLoading()
        destroy()
      }
      webViewRef = null
    }
  }

  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxSize().testTag("verification_payment_webview"),
  ) {
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
      Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            IconButton(onClick = onClose, modifier = Modifier.minimumInteractiveComponentSize()) {
              Icon(Icons.Filled.Close, contentDescription = "Close checkout")
            }
            Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("$providerName Checkout", fontWeight = FontWeight.Bold, fontSize = 15.5.sp, modifier = Modifier.weight(1f))
          }
          if (isLoading) {
            LinearProgressIndicator(
              progress = { (progress / 100f).coerceIn(0.1f, 1f) },
              color = TelefamCrimson,
              modifier = Modifier.fillMaxWidth().height(3.dp),
            )
          }
        }
      }
      if (safeUrl != "about:blank") {
        AndroidView(
          factory = { ctx ->
            WebView(ctx).apply {
              settings.javaScriptEnabled = true
              settings.domStorageEnabled = true
              settings.allowFileAccess = false
              settings.allowContentAccess = false
              settings.setSupportMultipleWindows(false)
              settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
              webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                  progress = newProgress
                  isLoading = newProgress < 100
                }
              }
              webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                  val url = request?.url?.toString().orEmpty()
                  if (url.startsWith("telefam://verification/payment-return", ignoreCase = true) ||
                    url.contains("/v1/verification/payment-return", ignoreCase = true)
                  ) {
                    val uri = runCatching { Uri.parse(url) }.getOrNull()
                    onReturnUrlIntercepted(
                      uri?.getQueryParameter("payment_id").orEmpty(),
                      uri?.getQueryParameter("reference").orEmpty().ifBlank { uri?.getQueryParameter("trxref").orEmpty() },
                      uri?.getQueryParameter("paypal_order_id").orEmpty().ifBlank { uri?.getQueryParameter("token").orEmpty() },
                      uri?.getQueryParameter("status").orEmpty().ifBlank { uri?.getQueryParameter("paypal_status").orEmpty() },
                    )
                    return true
                  }
                  return SmartTextDetector.normalizeSafeHttpUrl(url) == null
                }
              }
              loadUrl(safeUrl)
              webViewRef = this
            }
          },
          modifier = Modifier.weight(1f).fillMaxWidth(),
        )
      }
    }
  }
}
