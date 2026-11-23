package telefam.company.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.telefam.company.app.R
import telefam.company.core.SmartTextDetector
import telefam.company.core.TelefamContactsRules
import telefam.company.subscriptions.CreatorOfferingModel
import telefam.company.subscriptions.CreatorPlanModel
import telefam.company.subscriptions.GoalPresetItem
import telefam.company.subscriptions.SubscriptionBillingCycle
import telefam.company.subscriptions.SubscriptionPaymentProvider
import telefam.company.subscriptions.SubscriptionRecordModel
import telefam.company.subscriptions.SubscriptionsHubTab
import telefam.company.subscriptions.SubscriptionsRoute
import telefam.company.subscriptions.TelefamSubscriptionsCatalog
import telefam.company.subscriptions.TelefamSubscriptionsUiState
import telefam.company.ui.components.PayPalVectorLogo
import telefam.company.ui.components.PaystackVectorLogo
import telefam.company.ui.theme.LocalTelefamDarkTheme
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamCrimsonBright
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun TelefamSubscriptionsHostScreen(
  state: TelefamSubscriptionsUiState,
  onBack: () -> Unit,
  onRefresh: () -> Unit,
  onSelectHubTab: (SubscriptionsHubTab) -> Unit,
  onUpdatePlanEnabled: (Boolean) -> Unit,
  onUpdateDraftMonthlyCents: (Long) -> Unit,
  onUpdateDraftGoalCents: (Long) -> Unit,
  onUpdateDraftPerks: (String) -> Unit,
  onSavePlan: () -> Unit,
  onSelectBillingCycle: (SubscriptionBillingCycle) -> Unit,
  onSelectProvider: (SubscriptionPaymentProvider) -> Unit,
  onProceedToSubscribePay: () -> Unit,
  onCancelSubscription: (String) -> Unit,
  onRenewSubscription: (String, SubscriptionBillingCycle?, SubscriptionPaymentProvider?) -> Unit,
  onCloseCheckoutWebView: () -> Unit,
  onPaymentReturnCaptured: (paymentId: String, reference: String, paypalOrderId: String, status: String) -> Unit,
  onOpenUserProfile: (String) -> Unit,
  onDismissBanner: () -> Unit,
) {
  BackHandler(enabled = true) { onBack() }

  val isDark = LocalTelefamDarkTheme.current
  val accentRed = if (isDark) TelefamCrimsonBright else TelefamCrimson

  Box(modifier = Modifier.fillMaxSize()) {
    AnimatedContent(
      targetState = state.currentRoute,
      transitionSpec = {
        (fadeIn(tween(240)) + slideInVertically(tween(260)) { it / 18 }) togetherWith
          (fadeOut(tween(180)) + slideOutVertically(tween(200)) { -it / 24 })
      },
      label = "subscriptions_route_anim",
    ) { route ->
      when (route) {
        SubscriptionsRoute.HUB -> {
          SubscriptionsHubScreen(
            state = state,
            accentRed = accentRed,
            isDark = isDark,
            onBack = onBack,
            onRefresh = onRefresh,
            onSelectTab = onSelectHubTab,
            onUpdatePlanEnabled = onUpdatePlanEnabled,
            onUpdateDraftMonthlyCents = onUpdateDraftMonthlyCents,
            onUpdateDraftGoalCents = onUpdateDraftGoalCents,
            onUpdateDraftPerks = onUpdateDraftPerks,
            onSavePlan = onSavePlan,
            onCancelSubscription = onCancelSubscription,
            onRenewSubscription = onRenewSubscription,
            onOpenUserProfile = onOpenUserProfile,
          )
        }

        SubscriptionsRoute.CREATOR_SUBSCRIBE -> {
          val offering = state.creatorOffering
          if (offering != null) {
            CreatorSubscribeScreen(
              state = state,
              offering = offering,
              accentRed = accentRed,
              isDark = isDark,
              onBack = onBack,
              onSelectCycle = onSelectBillingCycle,
              onSelectProvider = onSelectProvider,
              onSubscribeNow = onProceedToSubscribePay,
              onCancelSubscription = { onCancelSubscription(offering.creatorUserId) },
              onRenewSubscription = { cycle, prov ->
                onRenewSubscription(offering.creatorUserId, cycle, prov)
              },
            )
          }
        }

        SubscriptionsRoute.CHECKOUT_WEBVIEW -> {
          val session = state.activeCheckout
          if (session != null) {
            SubscriptionsPaymentWebViewOverlay(
              checkoutUrl = session.checkoutUrl,
              providerName = session.provider.displayName,
              onReturnUrlIntercepted = onPaymentReturnCaptured,
              onClose = onCloseCheckoutWebView,
            )
          }
        }
      }
    }

    val bannerText = state.errorMessage ?: state.statusMessage
    AnimatedVisibility(
      visible = bannerText != null,
      enter = slideInVertically { it / 2 } + fadeIn(),
      exit = slideOutVertically { it / 2 } + fadeOut(),
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
      if (bannerText != null) {
        val isError = state.errorMessage != null
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = if (isError) Color(0xFF991B1B) else Color(0xFF141821),
          shadowElevation = 6.dp,
          modifier = Modifier.testTag("subscriptions_status_banner"),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .widthIn(max = 520.dp)
              .padding(horizontal = 16.dp, vertical = 10.dp),
          ) {
            Text(
              text = bannerText,
              color = Color.White,
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismissBanner) {
              Text("OK", color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionsHubScreen(
  state: TelefamSubscriptionsUiState,
  accentRed: Color,
  isDark: Boolean,
  onBack: () -> Unit,
  onRefresh: () -> Unit,
  onSelectTab: (SubscriptionsHubTab) -> Unit,
  onUpdatePlanEnabled: (Boolean) -> Unit,
  onUpdateDraftMonthlyCents: (Long) -> Unit,
  onUpdateDraftGoalCents: (Long) -> Unit,
  onUpdateDraftPerks: (String) -> Unit,
  onSavePlan: () -> Unit,
  onCancelSubscription: (String) -> Unit,
  onRenewSubscription: (String, SubscriptionBillingCycle?, SubscriptionPaymentProvider?) -> Unit,
  onOpenUserProfile: (String) -> Unit,
) {
  val pageBg = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF4F6FA)
  val cardBg = if (isDark) Color(0xFF171A21) else Color(0xFFFFFFFF)
  val border = if (isDark) Color(0xFF282D38) else Color(0xFFE6EAF0)
  val titleColor = if (isDark) Color(0xFFF3F5F8) else Color(0xFF141821)
  val mutedColor = if (isDark) Color(0xFF9EA6B6) else Color(0xFF64748B)

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .testTag("subscriptions_hub_screen"),
    containerColor = pageBg,
    topBar = {
      TopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            SubscriptionCrownSvgIcon(tint = accentRed, size = 20.dp)
            Text(
              text = stringResource(R.string.sub_hub_title),
              fontWeight = FontWeight.Bold,
              fontSize = 19.sp,
              color = titleColor,
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("subscriptions_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.cd_back),
              tint = titleColor,
            )
          }
        },
        actions = {
          IconButton(
            onClick = onRefresh,
            enabled = !state.isLoading,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("subscriptions_refresh_button"),
          ) {
            if (state.isLoading) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = accentRed)
            } else {
              Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Refresh subscriptions", tint = titleColor)
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = pageBg),
      )
    },
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      val isWide = maxWidth >= 760.dp

      if (isWide) {
        Row(
          modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 1040.dp)
            .padding(horizontal = 20.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
          Column(
            modifier = Modifier.weight(0.42f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            GoalOverviewHeroCard(
              plan = state.myPlan,
              activeOthersCount = state.activeSubscriptions.count { it.status == "active" },
              accentRed = accentRed,
              cardBg = cardBg,
              border = border,
              titleColor = titleColor,
              mutedColor = mutedColor,
            )
            HubTabBar(
              activeTab = state.activeHubTab,
              subscribersCount = state.subscribers.size,
              activeCount = state.activeSubscriptions.size,
              accentRed = accentRed,
              cardBg = cardBg,
              border = border,
              titleColor = titleColor,
              onSelectTab = onSelectTab,
            )
          }
          LazyColumn(
            modifier = Modifier.weight(0.58f),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            hubTabItems(
              state = state,
              accentRed = accentRed,
              cardBg = cardBg,
              border = border,
              titleColor = titleColor,
              mutedColor = mutedColor,
              isDark = isDark,
              onUpdatePlanEnabled = onUpdatePlanEnabled,
              onUpdateDraftMonthlyCents = onUpdateDraftMonthlyCents,
              onUpdateDraftGoalCents = onUpdateDraftGoalCents,
              onUpdateDraftPerks = onUpdateDraftPerks,
              onSavePlan = onSavePlan,
              onCancelSubscription = onCancelSubscription,
              onRenewSubscription = onRenewSubscription,
              onOpenUserProfile = onOpenUserProfile,
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 660.dp),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item(key = "goal_hero") {
            GoalOverviewHeroCard(
              plan = state.myPlan,
              activeOthersCount = state.activeSubscriptions.count { it.status == "active" },
              accentRed = accentRed,
              cardBg = cardBg,
              border = border,
              titleColor = titleColor,
              mutedColor = mutedColor,
            )
          }
          item(key = "hub_tabs") {
            HubTabBar(
              activeTab = state.activeHubTab,
              subscribersCount = state.subscribers.size,
              activeCount = state.activeSubscriptions.size,
              accentRed = accentRed,
              cardBg = cardBg,
              border = border,
              titleColor = titleColor,
              onSelectTab = onSelectTab,
            )
          }
          hubTabItems(
            state = state,
            accentRed = accentRed,
            cardBg = cardBg,
            border = border,
            titleColor = titleColor,
            mutedColor = mutedColor,
            isDark = isDark,
            onUpdatePlanEnabled = onUpdatePlanEnabled,
            onUpdateDraftMonthlyCents = onUpdateDraftMonthlyCents,
            onUpdateDraftGoalCents = onUpdateDraftGoalCents,
            onUpdateDraftPerks = onUpdateDraftPerks,
            onSavePlan = onSavePlan,
            onCancelSubscription = onCancelSubscription,
            onRenewSubscription = onRenewSubscription,
            onOpenUserProfile = onOpenUserProfile,
          )
          item(key = "bottom_pad") {
            Spacer(modifier = Modifier.height(48.dp))
          }
        }
      }
    }
  }
}

private fun androidx.compose.foundation.lazy.LazyListScope.hubTabItems(
  state: TelefamSubscriptionsUiState,
  accentRed: Color,
  cardBg: Color,
  border: Color,
  titleColor: Color,
  mutedColor: Color,
  isDark: Boolean,
  onUpdatePlanEnabled: (Boolean) -> Unit,
  onUpdateDraftMonthlyCents: (Long) -> Unit,
  onUpdateDraftGoalCents: (Long) -> Unit,
  onUpdateDraftPerks: (String) -> Unit,
  onSavePlan: () -> Unit,
  onCancelSubscription: (String) -> Unit,
  onRenewSubscription: (String, SubscriptionBillingCycle?, SubscriptionPaymentProvider?) -> Unit,
  onOpenUserProfile: (String) -> Unit,
) {
  when (state.activeHubTab) {
    SubscriptionsHubTab.SUBSCRIBERS -> {
      if (state.subscribers.isEmpty()) {
        item(key = "empty_subs") {
          EmptySubscriptionStateCard(
            title = stringResource(R.string.sub_no_subscribers_title),
            subtitle = stringResource(R.string.sub_no_subscribers_sub),
            accentRed = accentRed,
            cardBg = cardBg,
            border = border,
            titleColor = titleColor,
            mutedColor = mutedColor,
          )
        }
      } else {
        items(state.subscribers, key = { it.subscriptionId }) { sub ->
          SubscriberRowCard(
            sub = sub,
            accentRed = accentRed,
            cardBg = cardBg,
            border = border,
            titleColor = titleColor,
            mutedColor = mutedColor,
            isDark = isDark,
            onOpenProfile = { onOpenUserProfile(sub.subscriberUserId) },
          )
        }
      }
    }

    SubscriptionsHubTab.ACTIVE_PLANS -> {
      if (state.activeSubscriptions.isEmpty()) {
        item(key = "empty_active") {
          EmptySubscriptionStateCard(
            title = stringResource(R.string.sub_no_active_title),
            subtitle = stringResource(R.string.sub_no_active_sub),
            accentRed = accentRed,
            cardBg = cardBg,
            border = border,
            titleColor = titleColor,
            mutedColor = mutedColor,
          )
        }
      } else {
        items(state.activeSubscriptions, key = { it.subscriptionId }) { sub ->
          ActiveSubscriptionManageCard(
            sub = sub,
            isMutating = state.isMutating,
            accentRed = accentRed,
            cardBg = cardBg,
            border = border,
            titleColor = titleColor,
            mutedColor = mutedColor,
            isDark = isDark,
            onOpenProfile = { onOpenUserProfile(sub.creatorUserId) },
            onCancel = { onCancelSubscription(sub.creatorUserId) },
            onRenew = { cycle, prov -> onRenewSubscription(sub.creatorUserId, cycle, prov) },
          )
        }
      }
    }

    SubscriptionsHubTab.GOAL_PRICING -> {
      item(key = "goal_pricing_editor") {
        GoalAndPricingConfigCard(
          state = state,
          accentRed = accentRed,
          cardBg = cardBg,
          border = border,
          titleColor = titleColor,
          mutedColor = mutedColor,
          onUpdatePlanEnabled = onUpdatePlanEnabled,
          onUpdateDraftMonthlyCents = onUpdateDraftMonthlyCents,
          onUpdateDraftGoalCents = onUpdateDraftGoalCents,
          onUpdateDraftPerks = onUpdateDraftPerks,
          onSavePlan = onSavePlan,
        )
      }
    }
  }
}

@Composable
private fun GoalOverviewHeroCard(
  plan: CreatorPlanModel,
  activeOthersCount: Int,
  accentRed: Color,
  cardBg: Color,
  border: Color,
  titleColor: Color,
  mutedColor: Color,
) {
  val animatedSweep by animateFloatAsState(
    targetValue = (plan.goalProgressPct / 100f).coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
    label = "goal_ring_sweep",
  )

  Surface(
    shape = RoundedCornerShape(20.dp),
    color = cardBg,
    border = BorderStroke(1.dp, border),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("subscriptions_goal_hero_card"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Box(
        modifier = Modifier.size(86.dp),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val stroke = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
          val inset = 6.dp.toPx()
          val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
          drawArc(
            color = accentRed.copy(alpha = 0.14f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = stroke,
          )
          drawArc(
            brush = Brush.sweepGradient(listOf(accentRed, Color(0xFFFF6B6B), accentRed)),
            startAngle = -90f,
            sweepAngle = animatedSweep * 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = stroke,
          )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "${plan.goalProgressPct.toInt()}%",
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = titleColor,
          )
          Text(
            text = "Goal",
            fontSize = 11.sp,
            color = mutedColor,
          )
        }
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Text(
          text = "Monthly Goal: ${plan.goalLocalDisplay} (${plan.goalUsdDisplay})",
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = titleColor,
        )
        Text(
          text = "Current recurring: ${plan.currentMrrLocalDisplay} (${plan.currentMrrUsdDisplay})",
          fontSize = 13.sp,
          color = mutedColor,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          StatPillBadge(
            text = "${plan.activeSubscriberCount} Subscribers",
            accentRed = accentRed,
          )
          StatPillBadge(
            text = "$activeOthersCount Subscribed",
            accentRed = Color(0xFF0EA5E9),
          )
        }
      }
    }
  }
}

@Composable
private fun StatPillBadge(text: String, accentRed: Color) {
  Surface(
    shape = CircleShape,
    color = accentRed.copy(alpha = 0.11f),
  ) {
    Text(
      text = text,
      fontSize = 11.5.sp,
      fontWeight = FontWeight.SemiBold,
      color = accentRed,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
private fun HubTabBar(
  activeTab: SubscriptionsHubTab,
  subscribersCount: Int,
  activeCount: Int,
  accentRed: Color,
  cardBg: Color,
  border: Color,
  titleColor: Color,
  onSelectTab: (SubscriptionsHubTab) -> Unit,
) {
  val tabs = listOf(
    SubscriptionsHubTab.SUBSCRIBERS to "Subscribers ($subscribersCount)",
    SubscriptionsHubTab.ACTIVE_PLANS to "Subscribed ($activeCount)",
    SubscriptionsHubTab.GOAL_PRICING to "Goal & Pricing",
  )
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = cardBg,
    border = BorderStroke(1.dp, border),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(5.dp),
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      tabs.forEach { (tab, label) ->
        val selected = activeTab == tab
        val pillScale by animateFloatAsState(
          targetValue = if (selected) 1f else 0.97f,
          animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
          label = "hub_tab_scale",
        )
        Surface(
          onClick = { onSelectTab(tab) },
          shape = RoundedCornerShape(10.dp),
          color = if (selected) accentRed else Color.Transparent,
          modifier = Modifier
            .weight(1f)
            .scale(pillScale)
            .height(40.dp)
            .testTag("sub_hub_tab_${tab.id}"),
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
              text = label,
              fontSize = 12.5.sp,
              fontWeight = FontWeight.Bold,
              color = if (selected) Color.White else titleColor,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SubscriberRowCard(
  sub: SubscriptionRecordModel,
  accentRed: Color,
  cardBg: Color,
  border: Color,
  titleColor: Color,
  mutedColor: Color,
  isDark: Boolean,
  onOpenProfile: () -> Unit,
) {
  Surface(
    onClick = onOpenProfile,
    shape = RoundedCornerShape(16.dp),
    color = cardBg,
    border = BorderStroke(1.dp, border),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("subscriber_card_${sub.subscriberUserId}"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      SubscriptionAvatar(
        name = sub.subscriberName,
        username = sub.subscriberUsername,
        avatarUrl = sub.subscriberAvatarUrl,
        accentRed = accentRed,
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
          Text(
            text = sub.subscriberName.ifBlank { "@${sub.subscriberUsername}" },
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (sub.subscriberVerifiedTier != "none" && sub.subscriberVerifiedTier.isNotBlank()) {
            TelefamVerifiedBadgeIcon(tier = sub.subscriberVerifiedTier, size = 15.dp, isDarkTheme = isDark)
          }
        }
        Text(
          text = "@${sub.subscriberUsername} • ${sub.billingCycle.wireValue.replaceFirstChar { it.uppercase() }}",
          fontSize = 12.sp,
          color = mutedColor,
          maxLines = 1,
        )
        Text(
          text = "Renews ${formatShortDate(sub.currentPeriodEndMs)}",
          fontSize = 11.5.sp,
          color = mutedColor,
        )
      }
      Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = sub.amountDisplay.ifBlank { TelefamSubscriptionsCatalog.formatUsd(sub.amountUsdCents) },
          fontSize = 14.sp,
          fontWeight = FontWeight.ExtraBold,
          color = accentRed,
        )
        StatusChip(status = sub.status, accentRed = accentRed)
      }
    }
  }
}

@Composable
private fun ActiveSubscriptionManageCard(
  sub: SubscriptionRecordModel,
  isMutating: Boolean,
  accentRed: Color,
  cardBg: Color,
  border: Color,
  titleColor: Color,
  mutedColor: Color,
  isDark: Boolean,
  onOpenProfile: () -> Unit,
  onCancel: () -> Unit,
  onRenew: (SubscriptionBillingCycle?, SubscriptionPaymentProvider?) -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = cardBg,
    border = BorderStroke(1.dp, border),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("active_sub_card_${sub.creatorUserId}"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(onClick = onOpenProfile),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        SubscriptionAvatar(
          name = sub.creatorName,
          username = sub.creatorUsername,
          avatarUrl = sub.creatorAvatarUrl,
          accentRed = accentRed,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
              text = sub.creatorName.ifBlank { "@${sub.creatorUsername}" },
              fontSize = 14.5.sp,
              fontWeight = FontWeight.Bold,
              color = titleColor,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (sub.creatorVerifiedTier != "none" && sub.creatorVerifiedTier.isNotBlank()) {
              TelefamVerifiedBadgeIcon(tier = sub.creatorVerifiedTier, size = 15.dp, isDarkTheme = isDark)
            }
          }
          Text(
            text = "${sub.billingCycle.wireValue.replaceFirstChar { it.uppercase() }} • ${sub.provider.replaceFirstChar { it.uppercase() }} • ${sub.amountDisplay}",
            fontSize = 12.sp,
            color = mutedColor,
          )
          Text(
            text = if (sub.autoRenew) {
              "Next renewal ${formatShortDate(sub.currentPeriodEndMs)}"
            } else {
              "Ends ${formatShortDate(sub.currentPeriodEndMs)}"
            },
            fontSize = 11.5.sp,
            color = mutedColor,
          )
        }
        StatusChip(status = sub.status, accentRed = accentRed)
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (sub.canCancel) {
          OutlinedButton(
            onClick = onCancel,
            enabled = !isMutating,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, border),
            modifier = Modifier
              .weight(1f)
              .height(44.dp)
              .testTag("sub_cancel_${sub.creatorUserId}"),
          ) {
            Text(
              text = stringResource(R.string.sub_cancel_cta),
              fontSize = 12.5.sp,
              fontWeight = FontWeight.SemiBold,
              color = titleColor,
              maxLines = 1,
            )
          }
        }
        if (sub.canRenew) {
          Button(
            onClick = { onRenew(sub.billingCycle, null) },
            enabled = !isMutating,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentRed, contentColor = Color.White),
            modifier = Modifier
              .weight(1f)
              .height(44.dp)
              .testTag("sub_renew_${sub.creatorUserId}"),
          ) {
            Text(
              text = if (sub.status == "cancelled") {
                stringResource(R.string.sub_restore_autorenew_cta)
              } else {
                stringResource(R.string.sub_renew_cta)
              },
              fontSize = 12.5.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun GoalAndPricingConfigCard(
  state: TelefamSubscriptionsUiState,
  accentRed: Color,
  cardBg: Color,
  border: Color,
  titleColor: Color,
  mutedColor: Color,
  onUpdatePlanEnabled: (Boolean) -> Unit,
  onUpdateDraftMonthlyCents: (Long) -> Unit,
  onUpdateDraftGoalCents: (Long) -> Unit,
  onUpdateDraftPerks: (String) -> Unit,
  onSavePlan: () -> Unit,
) {
  val preview = remember(state.draftMonthlyUsdCents, state.draftAnnualDiscountPct, state.countryIso) {
    TelefamSubscriptionsCatalog.previewPricing(
      monthlyUsdCents = state.draftMonthlyUsdCents,
      discountPct = state.draftAnnualDiscountPct,
      countryIso = state.countryIso,
    )
  }

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = cardBg,
    border = BorderStroke(1.dp, border),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("goal_pricing_config_card"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.sub_enable_creator_subs),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor,
          )
          Text(
            text = "Fans can choose Monthly or Annual (20% off)",
            fontSize = 12.sp,
            color = mutedColor,
          )
        }
        Switch(
          checked = state.draftPlanEnabled,
          onCheckedChange = onUpdatePlanEnabled,
          colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentRed),
          modifier = Modifier.testTag("sub_plan_enabled_switch"),
        )
      }

      // Membership Price $0.50 -> $100.00
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = stringResource(R.string.sub_price_title),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor,
          )
          Text(
            text = "${preview.monthlyDisplay}/mo (${TelefamSubscriptionsCatalog.formatUsd(state.draftMonthlyUsdCents)})",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = accentRed,
          )
        }
        Slider(
          value = state.draftMonthlyUsdCents.toFloat(),
          onValueChange = { raw ->
            val stepped = ((raw / 50f).roundToLong() * 50L).coerceIn(50L, 10_000L)
            onUpdateDraftMonthlyCents(stepped)
          },
          valueRange = 50f..10_000f,
          colors = SliderDefaults.colors(thumbColor = accentRed, activeTrackColor = accentRed),
          modifier = Modifier.testTag("sub_price_slider"),
        )
        AmountPresetsRow(
          presets = state.goalPresets,
          selectedCents = state.draftMonthlyUsdCents,
          accentRed = accentRed,
          border = border,
          titleColor = titleColor,
          tagPrefix = "price_preset",
          onSelect = onUpdateDraftMonthlyCents,
        )
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = accentRed.copy(alpha = 0.08f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Monthly: ${preview.monthlyDisplay}", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = titleColor)
            Text(
              "Annual (20% off): ${preview.annualDisplay}",
              fontSize = 12.5.sp,
              fontWeight = FontWeight.Bold,
              color = accentRed,
            )
          }
        }
      }

      // Monthly Revenue Goal starting $0.50 to $100.00+
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = stringResource(R.string.sub_goal_title),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor,
          )
          Text(
            text = "${TelefamSubscriptionsCatalog.formatLocalFromUsdCents(state.draftMonthlyGoalUsdCents, state.countryIso)} (${TelefamSubscriptionsCatalog.formatUsd(state.draftMonthlyGoalUsdCents)})",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = accentRed,
          )
        }
        Text(
          text = stringResource(R.string.sub_goal_subtitle),
          fontSize = 12.sp,
          color = mutedColor,
        )
        Slider(
          value = state.draftMonthlyGoalUsdCents.coerceIn(50L, 10_000L).toFloat(),
          onValueChange = { raw ->
            val stepped = ((raw / 50f).roundToLong() * 50L).coerceIn(50L, 10_000L)
            onUpdateDraftGoalCents(stepped)
          },
          valueRange = 50f..10_000f,
          colors = SliderDefaults.colors(thumbColor = accentRed, activeTrackColor = accentRed),
          modifier = Modifier.testTag("sub_goal_slider"),
        )
        AmountPresetsRow(
          presets = state.goalPresets,
          selectedCents = state.draftMonthlyGoalUsdCents,
          accentRed = accentRed,
          border = border,
          titleColor = titleColor,
          tagPrefix = "goal_preset",
          onSelect = onUpdateDraftGoalCents,
        )
      }

      OutlinedTextField(
        value = state.draftPerksSummary,
        onValueChange = onUpdateDraftPerks,
        label = { Text("Subscriber perks") },
        maxLines = 3,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("sub_perks_input"),
      )

      Button(
        onClick = onSavePlan,
        enabled = !state.isSavingPlan,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = accentRed, contentColor = Color.White),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("sub_save_plan_button"),
      ) {
        if (state.isSavingPlan) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
          Spacer(modifier = Modifier.width(8.dp))
          Text(stringResource(R.string.sub_saving_plan), fontWeight = FontWeight.Bold)
        } else {
          Text(stringResource(R.string.sub_save_plan_cta), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
      }
    }
  }
}

@Composable
private fun AmountPresetsRow(
  presets: List<GoalPresetItem>,
  selectedCents: Long,
  accentRed: Color,
  border: Color,
  titleColor: Color,
  tagPrefix: String,
  onSelect: (Long) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    presets.forEach { preset ->
      val active = preset.amountUsdCents == selectedCents
      Surface(
        onClick = { onSelect(preset.amountUsdCents) },
        shape = CircleShape,
        color = if (active) accentRed else Color.Transparent,
        border = BorderStroke(1.dp, if (active) accentRed else border),
        modifier = Modifier
          .height(36.dp)
          .testTag("${tagPrefix}_${preset.amountUsdCents}"),
      ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
          Text(
            text = preset.usdLabel,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) Color.White else titleColor,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatorSubscribeScreen(
  state: TelefamSubscriptionsUiState,
  offering: CreatorOfferingModel,
  accentRed: Color,
  isDark: Boolean,
  onBack: () -> Unit,
  onSelectCycle: (SubscriptionBillingCycle) -> Unit,
  onSelectProvider: (SubscriptionPaymentProvider) -> Unit,
  onSubscribeNow: () -> Unit,
  onCancelSubscription: () -> Unit,
  onRenewSubscription: (SubscriptionBillingCycle, SubscriptionPaymentProvider) -> Unit,
) {
  val pageBg = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF4F6FA)
  val cardBg = if (isDark) Color(0xFF171A21) else Color(0xFFFFFFFF)
  val border = if (isDark) Color(0xFF282D38) else Color(0xFFE6EAF0)
  val titleColor = if (isDark) Color(0xFFF3F5F8) else Color(0xFF141821)
  val mutedColor = if (isDark) Color(0xFF9EA6B6) else Color(0xFF64748B)

  val viewerSub = offering.viewerSubscription
  val isAnnual = state.selectedCycle == SubscriptionBillingCycle.ANNUAL
  val pricing = offering.plan.pricing

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .testTag("creator_subscribe_screen"),
    containerColor = pageBg,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Subscribe",
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            color = titleColor,
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("creator_subscribe_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.cd_back),
              tint = titleColor,
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = pageBg),
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 640.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Creator Header Card
        item(key = "creator_header") {
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = cardBg,
            border = BorderStroke(1.dp, border),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              SubscriptionAvatar(
                name = offering.creatorName,
                username = offering.creatorUsername,
                avatarUrl = offering.creatorAvatarUrl,
                accentRed = accentRed,
                size = 72.dp,
              )
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = offering.creatorName.ifBlank { "@${offering.creatorUsername}" },
                  fontSize = 19.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = titleColor,
                )
                if (offering.creatorVerifiedTier != "none" && offering.creatorVerifiedTier.isNotBlank()) {
                  TelefamVerifiedBadgeIcon(tier = offering.creatorVerifiedTier, size = 18.dp, isDarkTheme = isDark)
                }
              }
              Text(
                text = "@${offering.creatorUsername}",
                fontSize = 13.sp,
                color = mutedColor,
              )
              if (offering.plan.perksSummary.isNotBlank()) {
                Text(
                  text = offering.plan.perksSummary,
                  fontSize = 13.5.sp,
                  color = mutedColor,
                  lineHeight = 19.sp,
                  modifier = Modifier.padding(top = 4.dp),
                )
              }
            }
          }
        }

        // Active Membership Status Card if subscribed/cancelled
        if (viewerSub != null && viewerSub.status != "expired") {
          item(key = "active_membership") {
            Surface(
              shape = RoundedCornerShape(18.dp),
              color = cardBg,
              border = BorderStroke(1.5.dp, accentRed.copy(alpha = 0.45f)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("creator_active_membership_card"),
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = Color(0xFF16A34A))
                    Text("Current Membership", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = titleColor)
                  }
                  StatusChip(status = viewerSub.status, accentRed = accentRed)
                }
                Text(
                  text = "Plan: ${viewerSub.billingCycle.wireValue.replaceFirstChar { it.uppercase() }} • ${viewerSub.amountDisplay} via ${viewerSub.provider.replaceFirstChar { it.uppercase() }}",
                  fontSize = 13.sp,
                  color = mutedColor,
                )
                Text(
                  text = if (viewerSub.autoRenew) {
                    "Renews on ${formatShortDate(viewerSub.currentPeriodEndMs)}"
                  } else {
                    "Active until ${formatShortDate(viewerSub.currentPeriodEndMs)}"
                  },
                  fontSize = 12.5.sp,
                  color = mutedColor,
                )
                if (viewerSub.canCancel) {
                  OutlinedButton(
                    onClick = onCancelSubscription,
                    enabled = !state.isMutating,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, border),
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(46.dp)
                      .testTag("creator_subscribe_cancel_button"),
                  ) {
                    Text(stringResource(R.string.sub_cancel_cta), color = titleColor, fontWeight = FontWeight.Bold)
                  }
                } else if (viewerSub.canRenew) {
                  Button(
                    onClick = { onRenewSubscription(state.selectedCycle, state.selectedProvider) },
                    enabled = !state.isMutating,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentRed, contentColor = Color.White),
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(46.dp)
                      .testTag("creator_subscribe_renew_button"),
                  ) {
                    Text(stringResource(R.string.sub_restore_autorenew_cta), fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }

        // Billing Cycle Selector (Monthly vs Annual)
        item(key = "billing_cycle") {
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = cardBg,
            border = BorderStroke(1.dp, border),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Text(
                text = "Choose billing cycle",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                CycleOptionTile(
                  title = stringResource(R.string.sub_monthly),
                  priceDisplay = pricing.monthlyDisplay,
                  usdSub = "${TelefamSubscriptionsCatalog.formatUsd(pricing.monthlyUsdCents)} / mo",
                  strikeOriginal = null,
                  badge = null,
                  selected = !isAnnual,
                  accentRed = accentRed,
                  border = border,
                  titleColor = titleColor,
                  mutedColor = mutedColor,
                  testTag = "sub_cycle_monthly",
                  modifier = Modifier.weight(1f),
                  onClick = { onSelectCycle(SubscriptionBillingCycle.MONTHLY) },
                )
                CycleOptionTile(
                  title = stringResource(R.string.sub_annual),
                  priceDisplay = pricing.annualDisplay,
                  usdSub = "${TelefamSubscriptionsCatalog.formatUsd(pricing.annualUsdCents)} / yr",
                  strikeOriginal = pricing.annualOriginalDisplay,
                  badge = "${pricing.annualDiscountPct}% OFF",
                  selected = isAnnual,
                  accentRed = accentRed,
                  border = border,
                  titleColor = titleColor,
                  mutedColor = mutedColor,
                  testTag = "sub_cycle_annual",
                  modifier = Modifier.weight(1f),
                  onClick = { onSelectCycle(SubscriptionBillingCycle.ANNUAL) },
                )
              }
            }
          }
        }

        // Payment Provider Selector (Paystack & PayPal)
        item(key = "payment_method") {
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = cardBg,
            border = BorderStroke(1.dp, border),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text(
                text = stringResource(R.string.stars_payment_method),
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
              )
              SubProviderTile(
                title = stringResource(R.string.stars_paystack_title),
                subtitle = stringResource(R.string.stars_paystack_sub),
                selected = state.selectedProvider == SubscriptionPaymentProvider.PAYSTACK,
                accentColor = accentRed,
                cardBg = cardBg,
                borderColor = border,
                primaryText = titleColor,
                mutedText = mutedColor,
                testTag = "sub_provider_paystack",
                icon = { PaystackVectorLogo(size = 28.dp) },
                onClick = { onSelectProvider(SubscriptionPaymentProvider.PAYSTACK) },
              )
              SubProviderTile(
                title = stringResource(R.string.stars_paypal_title),
                subtitle = stringResource(R.string.stars_paypal_sub),
                selected = state.selectedProvider == SubscriptionPaymentProvider.PAYPAL,
                accentColor = accentRed,
                cardBg = cardBg,
                borderColor = border,
                primaryText = titleColor,
                mutedText = mutedColor,
                testTag = "sub_provider_paypal",
                icon = { PayPalVectorLogo(size = 28.dp) },
                onClick = { onSelectProvider(SubscriptionPaymentProvider.PAYPAL) },
              )
              Text(
                text = stringResource(R.string.sub_deposit_pay_note),
                fontSize = 11.5.sp,
                color = mutedColor,
              )
            }
          }
        }

        // Proceed / Subscribe / Switch Plan CTA
        item(key = "subscribe_cta") {
          val ctaLabel = when {
            viewerSub == null || viewerSub.status == "expired" -> stringResource(R.string.sub_subscribe_cta)
            viewerSub.billingCycle != state.selectedCycle -> "Switch to ${state.selectedCycle.wireValue.replaceFirstChar { it.uppercase() }}"
            else -> "Pay with ${state.selectedProvider.displayName} to Extend"
          }
          Button(
            onClick = {
              if (viewerSub == null || viewerSub.status == "expired") {
                onSubscribeNow()
              } else {
                onRenewSubscription(state.selectedCycle, state.selectedProvider)
              }
            },
            enabled = !state.isMutating && offering.plan.enabled && !offering.isOwner,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentRed, contentColor = Color.White),
            modifier = Modifier
              .fillMaxWidth()
              .height(54.dp)
              .testTag("creator_subscribe_pay_button"),
          ) {
            if (state.isMutating) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Preparing secure checkout...", fontWeight = FontWeight.Bold)
            } else {
              SubscriptionCrownSvgIcon(tint = Color.White, size = 18.dp)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "$ctaLabel • ${if (isAnnual) pricing.annualDisplay else pricing.monthlyDisplay}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
              )
            }
          }
          Spacer(modifier = Modifier.height(36.dp))
        }
      }
    }
  }
}

@Composable
private fun CycleOptionTile(
  title: String,
  priceDisplay: String,
  usdSub: String,
  strikeOriginal: String?,
  badge: String?,
  selected: Boolean,
  accentRed: Color,
  border: Color,
  titleColor: Color,
  mutedColor: Color,
  testTag: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(14.dp),
    color = if (selected) accentRed.copy(alpha = 0.08f) else Color.Transparent,
    border = BorderStroke(if (selected) 1.8.dp else 1.dp, if (selected) accentRed else border),
    modifier = modifier.testTag(testTag),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(text = title, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = titleColor)
        if (badge != null) {
          Surface(shape = CircleShape, color = accentRed) {
            Text(
              text = badge,
              fontSize = 10.sp,
              fontWeight = FontWeight.ExtraBold,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            )
          }
        }
      }
      Text(text = priceDisplay, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = accentRed)
      Text(text = usdSub, fontSize = 11.5.sp, color = mutedColor)
      if (strikeOriginal != null) {
        Text(
          text = strikeOriginal,
          fontSize = 11.sp,
          color = mutedColor,
          textDecoration = TextDecoration.LineThrough,
        )
      }
    }
  }
}

@Composable
private fun SubProviderTile(
  title: String,
  subtitle: String,
  selected: Boolean,
  accentColor: Color,
  cardBg: Color,
  borderColor: Color,
  primaryText: Color,
  mutedText: Color,
  testTag: String,
  icon: @Composable () -> Unit,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(14.dp),
    color = cardBg,
    border = BorderStroke(
      width = if (selected) 1.5.dp else 1.dp,
      color = if (selected) accentColor else borderColor,
    ),
    modifier = Modifier
      .fillMaxWidth()
      .testTag(testTag),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
      icon()
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(text = title, color = primaryText, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
        Text(text = subtitle, color = mutedText, fontSize = 12.sp, lineHeight = 16.sp)
      }
      RadioButton(
        selected = selected,
        onClick = onClick,
        colors = RadioButtonDefaults.colors(selectedColor = accentColor, unselectedColor = mutedText),
      )
    }
  }
}

@Composable
private fun StatusChip(status: String, accentRed: Color) {
  val (label, tint) = when (status.lowercase()) {
    "active" -> "Active" to Color(0xFF16A34A)
    "cancelled" -> "Cancels at end" to Color(0xFFD97706)
    else -> "Expired" to accentRed
  }
  Surface(shape = CircleShape, color = tint.copy(alpha = 0.13f)) {
    Text(
      text = label,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = tint,
      modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
    )
  }
}

@Composable
private fun EmptySubscriptionStateCard(
  title: String,
  subtitle: String,
  accentRed: Color,
  cardBg: Color,
  border: Color,
  titleColor: Color,
  mutedColor: Color,
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = cardBg,
    border = BorderStroke(1.dp, border),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Box(
        modifier = Modifier
          .size(54.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(accentRed.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
      ) {
        SubscriptionCrownSvgIcon(tint = accentRed, size = 28.dp)
      }
      Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = titleColor)
      Text(text = subtitle, fontSize = 13.sp, color = mutedColor, lineHeight = 18.sp)
    }
  }
}

@Composable
private fun SubscriptionAvatar(
  name: String,
  username: String,
  avatarUrl: String,
  accentRed: Color,
  size: Dp = 44.dp,
) {
  if (avatarUrl.isNotBlank()) {
    AsyncImage(
      model = avatarUrl,
      contentDescription = name,
      contentScale = ContentScale.Crop,
      modifier = Modifier
        .size(size)
        .clip(CircleShape)
        .border(1.5.dp, accentRed.copy(alpha = 0.35f), CircleShape),
    )
  } else {
    Box(
      modifier = Modifier
        .size(size)
        .clip(CircleShape)
        .background(accentRed.copy(alpha = 0.14f)),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = TelefamContactsRules.initialsFor(name, username),
        color = accentRed,
        fontWeight = FontWeight.Bold,
        fontSize = (size.value * 0.34f).sp,
      )
    }
  }
}

@Composable
fun SubscriptionCrownSvgIcon(
  tint: Color,
  size: Dp = 20.dp,
) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val path = Path().apply {
      moveTo(w * 0.14f, h * 0.70f)
      lineTo(w * 0.12f, h * 0.30f)
      lineTo(w * 0.33f, h * 0.48f)
      lineTo(w * 0.50f, h * 0.22f)
      lineTo(w * 0.67f, h * 0.48f)
      lineTo(w * 0.88f, h * 0.30f)
      lineTo(w * 0.86f, h * 0.70f)
      close()
    }
    drawPath(path = path, color = tint)
    drawRoundRect(
      color = tint,
      topLeft = Offset(w * 0.14f, h * 0.75f),
      size = Size(w * 0.72f, h * 0.10f),
      cornerRadius = CornerRadius(h * 0.05f, h * 0.05f),
    )
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SubscriptionsPaymentWebViewOverlay(
  checkoutUrl: String,
  providerName: String,
  onReturnUrlIntercepted: (paymentId: String, reference: String, paypalOrderId: String, status: String) -> Unit,
  onClose: () -> Unit,
) {
  val safeUrl = remember(checkoutUrl) {
    SmartTextDetector.normalizeSafeHttpUrl(checkoutUrl) ?: "about:blank"
  }
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
    modifier = Modifier
      .fillMaxSize()
      .testTag("subscriptions_payment_webview"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding(),
    ) {
      Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Column {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 8.dp),
          ) {
            IconButton(
              onClick = onClose,
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(imageVector = Icons.Filled.Close, contentDescription = "Close checkout")
            }
            Icon(
              imageVector = Icons.Filled.Lock,
              contentDescription = null,
              tint = Color(0xFF16A34A),
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "$providerName Checkout",
              fontWeight = FontWeight.Bold,
              fontSize = 15.5.sp,
              modifier = Modifier.weight(1f),
            )
          }
          if (isLoading) {
            LinearProgressIndicator(
              progress = { (progress / 100f).coerceIn(0.1f, 1f) },
              color = TelefamCrimson,
              modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
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
                  if (url.startsWith("telefam://subscriptions/payment-return", ignoreCase = true) ||
                    url.contains("/v1/subscriptions/payment-return", ignoreCase = true)
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
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        )
      }
    }
  }
}

private fun formatShortDate(epochMs: Long): String {
  if (epochMs <= 0L) return "Pending"
  return SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(epochMs))
}
