package telefam.company.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.telefam.company.app.R
import telefam.company.core.SmartTextDetector
import telefam.company.data.network.StarLedgerItemDto
import telefam.company.data.network.StarSenderSummaryDto
import telefam.company.stars.StarPaymentProvider
import telefam.company.stars.TelefamStarsCatalog
import telefam.company.stars.TelefamStarsRoute
import telefam.company.stars.TelefamStarsUiState
import telefam.company.ui.components.PayPalVectorLogo
import telefam.company.ui.components.PaystackVectorLogo
import telefam.company.ui.components.TelefamAnimated3DStarEmblem
import telefam.company.ui.components.TelefamGoldStarBadgeIcon
import telefam.company.ui.theme.LocalTelefamDarkTheme
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamCrimsonBright
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TelefamStarsHostScreen(
  state: TelefamStarsUiState,
  onBack: () -> Unit,
  onRefresh: () -> Unit,
  onSelectHubTab: (Int) -> Unit,
  onOpenBuyStars: () -> Unit,
  onSelectPackageForCheckout: (Long) -> Unit,
  onSendStarsTier: (Long) -> Unit,
  onSelectProvider: (StarPaymentProvider) -> Unit,
  onProceedToPay: () -> Unit,
  onCloseCheckoutWebView: (Boolean) -> Unit,
  onPaymentReturnCaptured: (paymentId: String, reference: String, paypalOrderId: String, status: String) -> Unit,
  onOpenPeerProfile: (String) -> Unit,
  onDismissToast: () -> Unit,
) {
  BackHandler(enabled = true) {
    if (!state.activeCheckoutUrl.isNullOrBlank()) {
      onCloseCheckoutWebView(true)
    } else {
      onBack()
    }
  }

  val route = state.currentRoute ?: TelefamStarsRoute.HUB
  Box(modifier = Modifier.fillMaxSize()) {
    when (route) {
      TelefamStarsRoute.HUB -> StarsHubScreen(
        state = state,
        onBack = onBack,
        onRefresh = onRefresh,
        onSelectTab = onSelectHubTab,
        onOpenBuyStars = onOpenBuyStars,
      )

      TelefamStarsRoute.SEND_STARS -> SendStarsScreen(
        state = state,
        onBack = onBack,
        onSelectPeer = {
          state.targetPeer?.peerUserId?.takeIf { it.isNotBlank() }?.let(onOpenPeerProfile)
        },
        onSendStars = onSendStarsTier,
        onOpenBuyStars = onOpenBuyStars,
      )

      TelefamStarsRoute.BUY_STARS -> BuyStarsScreen(
        state = state,
        onBack = onBack,
        onBuyPackage = onSelectPackageForCheckout,
      )

      TelefamStarsRoute.CHECKOUT -> StarsCheckoutScreen(
        state = state,
        onBack = onBack,
        onSelectProvider = onSelectProvider,
        onProceedToPay = onProceedToPay,
      )
    }

    val checkoutUrl = state.activeCheckoutUrl
    if (!checkoutUrl.isNullOrBlank()) {
      StarsPaymentWebViewOverlay(
        checkoutUrl = checkoutUrl,
        providerName = if (state.selectedProvider == StarPaymentProvider.PAYSTACK) {
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
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 6.dp,
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
          ) {
            CircularProgressIndicator(color = TelefamCrimson, modifier = Modifier.size(34.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = stringResource(R.string.stars_verifying_payment),
              fontWeight = FontWeight.SemiBold,
              fontSize = 14.5.sp,
            )
          }
        }
      }
    }

    val banner = state.errorMessage ?: state.statusMessage
    if (!banner.isNullOrBlank()) {
      val isErr = !state.errorMessage.isNullOrBlank()
      Surface(
        onClick = onDismissToast,
        shape = CircleShape,
        color = if (isErr) Color(0xFFD91E2A) else Color(0xEB151821),
        shadowElevation = 6.dp,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .navigationBarsPadding()
          .padding(horizontal = 20.dp, vertical = 18.dp)
          .testTag("stars_status_banner"),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StarsHubScreen(
  state: TelefamStarsUiState,
  onBack: () -> Unit,
  onRefresh: () -> Unit,
  onSelectTab: (Int) -> Unit,
  onOpenBuyStars: () -> Unit,
) {
  val isDark = LocalTelefamDarkTheme.current
  val pageBg = if (isDark) Color(0xFF0E1015) else Color(0xFFF5F6FA)
  val cardBg = if (isDark) Color(0xFF171A21) else Color(0xFFFFFFFF)
  val innerCardBg = if (isDark) Color(0xFF1E222B) else Color(0xFFF6F7FB)
  val borderColor = if (isDark) Color(0xFF2B303C) else Color(0xFFE5E8EF)
  val primaryText = if (isDark) Color(0xFFF5F7FB) else Color(0xFF131620)
  val mutedText = if (isDark) Color(0xFF949CAD) else Color(0xFF6B7280)
  val crimson = if (isDark) TelefamCrimsonBright else TelefamCrimson

  val currencyLabel = remember(state.wallet.balanceStars, state.countryIso) {
    TelefamStarsCatalog.formatCurrencyForStars(state.wallet.balanceStars, state.countryIso)
  }

  Scaffold(
    containerColor = pageBg,
    modifier = Modifier
      .fillMaxSize()
      .testTag("stars_hub_screen"),
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = pageBg),
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("stars_hub_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.cd_back),
              tint = primaryText,
            )
          }
        },
        title = {
          Text(
            text = stringResource(R.string.stars_title),
            color = primaryText,
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
          )
        },
        actions = {
          OutlinedButton(
            onClick = onRefresh,
            shape = CircleShape,
            border = BorderStroke(1.dp, borderColor),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            modifier = Modifier
              .padding(end = 12.dp)
              .minimumInteractiveComponentSize()
              .testTag("stars_hub_refresh_button"),
          ) {
            if (state.isLoadingSummary) {
              CircularProgressIndicator(
                color = primaryText,
                strokeWidth = 2.dp,
                modifier = Modifier.size(14.dp),
              )
            } else {
              Text(
                text = stringResource(R.string.stars_refresh),
                color = primaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
        },
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
          .widthIn(max = 660.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Hero Available Balance Card (Matches Screenshot 1)
        item(key = "stars_hero_card") {
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = cardBg,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
              ) {
                TelefamAnimated3DStarEmblem(
                  starsAmount = state.wallet.balanceStars.coerceAtLeast(100L),
                  baseSize = 54.dp,
                  showTelefamWordmark = true,
                  isDarkTheme = isDark,
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = stringResource(R.string.stars_available_balance),
                    color = mutedText,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "${TelefamStarsCatalog.formatStarsCount(state.wallet.balanceStars)} ${stringResource(R.string.stars_unit_plural)}",
                    color = primaryText,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.testTag("stars_available_balance_text"),
                  )
                  Text(
                    text = currencyLabel,
                    color = mutedText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                  )
                }

                Button(
                  onClick = onOpenBuyStars,
                  shape = CircleShape,
                  colors = ButtonDefaults.buttonColors(containerColor = crimson, contentColor = Color.White),
                  contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp),
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("stars_hub_buy_button"),
                ) {
                  Text(
                    text = stringResource(R.string.stars_buy_button),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                  )
                }
              }

              // Two inner stat tiles: Stars Received (green) & Stars Sent (crimson)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                StarMetricTile(
                  label = stringResource(R.string.stars_received_label),
                  count = state.wallet.starsReceivedTotal,
                  valueColor = Color(0xFF22C55E),
                  bgColor = innerCardBg,
                  borderColor = borderColor,
                  labelColor = mutedText,
                  modifier = Modifier
                    .weight(1f)
                    .testTag("stars_received_tile"),
                )
                StarMetricTile(
                  label = stringResource(R.string.stars_sent_label),
                  count = state.wallet.starsSentTotal,
                  valueColor = crimson,
                  bgColor = innerCardBg,
                  borderColor = borderColor,
                  labelColor = mutedText,
                  modifier = Modifier
                    .weight(1f)
                    .testTag("stars_sent_tile"),
                )
              }
            }
          }
        }

        // Segmented pill toggle: Senders (N) | History (M)
        item(key = "stars_tabs_toggle") {
          Surface(
            shape = CircleShape,
            color = cardBg,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            ) {
              val tabs = listOf(
                "${stringResource(R.string.stars_senders_tab)} (${state.senders.size})",
                "${stringResource(R.string.stars_history_tab)} (${state.history.size})",
              )
              tabs.forEachIndexed { index, title ->
                val selected = state.hubTabIndex == index
                Surface(
                  onClick = { onSelectTab(index) },
                  shape = CircleShape,
                  color = if (selected) crimson else Color.Transparent,
                  modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag(if (index == 0) "stars_tab_senders" else "stars_tab_history"),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      text = title,
                      color = if (selected) Color.White else mutedText,
                      fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                      fontSize = 13.5.sp,
                      maxLines = 1,
                    )
                  }
                }
              }
            }
          }
        }

        if (state.hubTabIndex == 0) {
          if (state.senders.isEmpty()) {
            item(key = "stars_senders_empty") {
              StarsEmptyStateCard(
                title = stringResource(R.string.stars_no_senders_title),
                subtitle = stringResource(R.string.stars_no_senders_sub),
                cardBg = cardBg,
                borderColor = borderColor,
                titleColor = primaryText,
                subtitleColor = mutedText,
                isDark = isDark,
              )
            }
          } else {
            items(state.senders, key = { it.senderUserId }) { sender ->
              StarSenderRowCard(
                sender = sender,
                cardBg = cardBg,
                borderColor = borderColor,
                primaryText = primaryText,
                mutedText = mutedText,
                crimson = crimson,
              )
            }
          }
        } else {
          if (state.history.isEmpty()) {
            item(key = "stars_history_empty") {
              StarsEmptyStateCard(
                title = stringResource(R.string.stars_no_history_title),
                subtitle = stringResource(R.string.stars_no_history_sub),
                cardBg = cardBg,
                borderColor = borderColor,
                titleColor = primaryText,
                subtitleColor = mutedText,
                isDark = isDark,
              )
            }
          } else {
            items(state.history, key = { it.entryId }) { item ->
              StarHistoryRowCard(
                item = item,
                cardBg = cardBg,
                borderColor = borderColor,
                primaryText = primaryText,
                mutedText = mutedText,
                crimson = crimson,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StarMetricTile(
  label: String,
  count: Long,
  valueColor: Color,
  bgColor: Color,
  borderColor: Color,
  labelColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = bgColor,
    border = BorderStroke(1.dp, borderColor),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = label,
        color = labelColor,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        TinySolidStarIcon(color = Color(0xFFF5B700), size = 16.dp)
        Text(
          text = TelefamStarsCatalog.formatStarsCount(count),
          color = valueColor,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
private fun StarsEmptyStateCard(
  title: String,
  subtitle: String,
  cardBg: Color,
  borderColor: Color,
  titleColor: Color,
  subtitleColor: Color,
  isDark: Boolean,
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = cardBg,
    border = BorderStroke(1.dp, borderColor),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
      TelefamAnimated3DStarEmblem(
        starsAmount = 100L,
        baseSize = 58.dp,
        showTelefamWordmark = false,
        isDarkTheme = isDark,
      )
      Text(
        text = title,
        color = titleColor,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
      )
      Text(
        text = subtitle,
        color = subtitleColor,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        lineHeight = 18.sp,
      )
    }
  }
}

@Composable
private fun StarSenderRowCard(
  sender: StarSenderSummaryDto,
  cardBg: Color,
  borderColor: Color,
  primaryText: Color,
  mutedText: Color,
  crimson: Color,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = cardBg,
    border = BorderStroke(1.dp, borderColor),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
      if (sender.senderAvatarUrl.isNotBlank()) {
        AsyncImage(
          model = sender.senderAvatarUrl,
          contentDescription = sender.senderFullName,
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape),
        )
      } else {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(crimson.copy(alpha = 0.14f)),
        ) {
          Text(
            text = sender.senderFullName.take(1).uppercase(Locale.getDefault()),
            color = crimson,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
          )
        }
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = sender.senderFullName.ifBlank { "Telefam Supporter" },
          color = primaryText,
          fontWeight = FontWeight.Bold,
          fontSize = 14.5.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (sender.senderUsername.isNotBlank()) {
          Text(
            text = "@${sender.senderUsername.removePrefix("@")} · ${sender.giftCount} gifts",
            color = mutedText,
            fontSize = 12.sp,
          )
        }
      }
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        TinySolidStarIcon(color = Color(0xFFF5B700), size = 16.dp)
        Text(
          text = TelefamStarsCatalog.formatStarsCount(sender.totalStarsSent),
          color = Color(0xFF22C55E),
          fontWeight = FontWeight.Bold,
          fontSize = 14.5.sp,
        )
      }
    }
  }
}

@Composable
private fun StarHistoryRowCard(
  item: StarLedgerItemDto,
  cardBg: Color,
  borderColor: Color,
  primaryText: Color,
  mutedText: Color,
  crimson: Color,
) {
  val isCredit = item.deltaStars >= 0L
  val title = when (item.entryType) {
    "purchase" -> "Bought via ${item.provider.replaceFirstChar { it.uppercase() }}"
    "received" -> "Received from ${item.counterpartyName.ifBlank { "Supporter" }}"
    else -> "Sent to ${item.counterpartyName.ifBlank { "Creator" }}"
  }
  val timeText = remember(item.createdAtMs) {
    if (item.createdAtMs <= 0L) "" else SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(item.createdAtMs))
  }
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = cardBg,
    border = BorderStroke(1.dp, borderColor),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
      TelefamGoldStarBadgeIcon(size = 38.dp)
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          color = primaryText,
          fontWeight = FontWeight.SemiBold,
          fontSize = 14.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (timeText.isNotBlank()) {
          Text(
            text = timeText,
            color = mutedText,
            fontSize = 11.5.sp,
          )
        }
      }
      val deltaPrefix = if (isCredit) "+" else ""
      Text(
        text = "$deltaPrefix${TelefamStarsCatalog.formatStarsCount(item.deltaStars)}",
        color = if (isCredit) Color(0xFF22C55E) else crimson,
        fontWeight = FontWeight.Bold,
        fontSize = 14.5.sp,
      )
    }
  }
}

@Composable
private fun SendStarsScreen(
  state: TelefamStarsUiState,
  onBack: () -> Unit,
  onSelectPeer: () -> Unit,
  onSendStars: (Long) -> Unit,
  onOpenBuyStars: () -> Unit,
) {
  val isDark = LocalTelefamDarkTheme.current
  val pageBg = if (isDark) Color(0xFF101218) else Color(0xFFF6F7FA)
  val cardBg = if (isDark) Color(0xFF171A21) else Color(0xFFFFFFFF)
  val borderColor = if (isDark) Color(0xFF2B303C) else Color(0xFFECEFF4)
  val primaryText = if (isDark) Color(0xFFF5F7FB) else Color(0xFF1A2233)
  val mutedText = if (isDark) Color(0xFF9BA3B4) else Color(0xFF6B7280)
  val crimson = TelefamCrimson
  val peer = state.targetPeer

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(pageBg)
      .testTag("send_stars_screen"),
  ) {
    // Crimson Top Bar (matches Screenshot 2)
    Surface(
      color = crimson,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 8.dp, vertical = 10.dp),
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("send_stars_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.cd_back),
            tint = Color.White,
          )
        }
        Text(
          text = stringResource(R.string.stars_send_title),
          color = Color.White,
          fontSize = 18.5.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f),
        )
        Surface(
          onClick = onOpenBuyStars,
          shape = CircleShape,
          color = Color.Black.copy(alpha = 0.24f),
          modifier = Modifier
            .padding(end = 6.dp)
            .minimumInteractiveComponentSize()
            .testTag("send_stars_balance_pill"),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
          ) {
            TinySolidStarIcon(color = Color(0xFFFFD54F), size = 14.dp)
            Text(
              text = stringResource(
                R.string.stars_balance_pill,
                TelefamStarsCatalog.formatStarsCount(state.wallet.balanceStars),
              ),
              color = Color.White,
              fontSize = 12.5.sp,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }

    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .navigationBarsPadding(),
      contentAlignment = Alignment.TopCenter,
    ) {
      val useTwoColumns = maxWidth >= 600.dp
      val rows = remember(useTwoColumns) {
        if (useTwoColumns) TelefamStarsCatalog.presetPackages.chunked(2)
        else TelefamStarsCatalog.presetPackages.map { listOf(it) }
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 760.dp),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (peer != null) {
          item(key = "peer_card") {
            Surface(
              onClick = onSelectPeer,
              shape = RoundedCornerShape(14.dp),
              color = if (isDark) Color(0xFF1D1B22) else Color(0xFFFCF3F4),
              border = BorderStroke(1.dp, if (isDark) Color(0xFF33262B) else Color(0xFFF6DFE2)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 14.dp, vertical = 12.dp),
              ) {
                Box(modifier = Modifier.size(46.dp)) {
                  if (peer.peerAvatarUrl.isNotBlank()) {
                    AsyncImage(
                      model = peer.peerAvatarUrl,
                      contentDescription = peer.peerName,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    )
                  } else {
                    Box(
                      contentAlignment = Alignment.Center,
                      modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(crimson.copy(alpha = 0.16f)),
                    ) {
                      Text(
                        text = peer.peerName.take(1).uppercase(Locale.getDefault()),
                        color = crimson,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                      )
                    }
                  }
                  if (peer.isOnline) {
                    Box(
                      modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E))
                        .border(2.dp, cardBg, CircleShape),
                    )
                  }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = peer.peerName,
                    color = primaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  if (peer.peerUsername.isNotBlank()) {
                    Text(
                      text = "@${peer.peerUsername.removePrefix("@")}",
                      color = mutedText,
                      fontSize = 12.5.sp,
                    )
                  }
                }
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                  contentDescription = null,
                  tint = crimson,
                )
              }
            }
          }
        }

        item(key = "select_stars_banner") {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = cardBg,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            ) {
              TelefamGoldStarBadgeIcon(size = 42.dp)
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = stringResource(R.string.stars_select_to_send),
                  color = primaryText,
                  fontSize = 14.5.sp,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = stringResource(R.string.stars_send_support_sub),
                  color = mutedText,
                  fontSize = 12.sp,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                  shape = CircleShape,
                  color = if (isDark) Color(0xFF361B20) else Color(0xFFFDE8EA),
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  ) {
                    TinySolidStarIcon(color = Color(0xFFF5B700), size = 12.dp)
                    Text(
                      text = stringResource(R.string.stars_one_star_badge),
                      color = crimson,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.SemiBold,
                    )
                  }
                }
              }
            }
          }
        }

        items(rows, key = { it.first() }) { pair ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            pair.forEach { amount ->
              val converted = remember(amount, state.countryIso) {
                TelefamStarsCatalog.formatCurrencyForStars(amount, state.countryIso)
              }
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = cardBg,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier
                  .weight(1f)
                  .testTag("send_star_row_$amount"),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                  TelefamGoldStarBadgeIcon(size = 36.dp)
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = TelefamStarsCatalog.formatStarsCount(amount),
                      color = primaryText,
                      fontSize = 15.sp,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = converted,
                      color = mutedText,
                      fontSize = 12.sp,
                    )
                  }
                  Button(
                    onClick = { onSendStars(amount) },
                    enabled = !state.isSendingStars,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = crimson, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                    modifier = Modifier
                      .minimumInteractiveComponentSize()
                      .testTag("send_star_btn_$amount"),
                  ) {
                    Text(
                      text = stringResource(R.string.stars_send_cta),
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp,
                    )
                  }
                }
              }
            }
            if (useTwoColumns && pair.size == 1) {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun BuyStarsScreen(
  state: TelefamStarsUiState,
  onBack: () -> Unit,
  onBuyPackage: (Long) -> Unit,
) {
  val isDark = LocalTelefamDarkTheme.current
  val pageBg = if (isDark) Color(0xFF101218) else Color(0xFFF6F7FA)
  val cardBg = if (isDark) Color(0xFF171A21) else Color(0xFFFFFFFF)
  val borderColor = if (isDark) Color(0xFF2B303C) else Color(0xFFECEFF4)
  val primaryText = if (isDark) Color(0xFFF5F7FB) else Color(0xFF1A2233)
  val mutedText = if (isDark) Color(0xFF9BA3B4) else Color(0xFF6B7280)
  val crimson = TelefamCrimson

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(pageBg)
      .testTag("buy_stars_screen"),
  ) {
    // Crimson Top Bar (matches Screenshot 3 left)
    Surface(color = crimson, modifier = Modifier.fillMaxWidth()) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 8.dp, vertical = 10.dp),
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("buy_stars_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.cd_back),
            tint = Color.White,
          )
        }
        Text(
          text = stringResource(R.string.stars_buy_title),
          color = Color.White,
          fontSize = 18.5.sp,
          fontWeight = FontWeight.Bold,
        )
      }
    }

    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .navigationBarsPadding(),
      contentAlignment = Alignment.TopCenter,
    ) {
      val useTwoColumns = maxWidth >= 600.dp
      val rows = remember(useTwoColumns) {
        if (useTwoColumns) TelefamStarsCatalog.presetPackages.chunked(2)
        else TelefamStarsCatalog.presetPackages.map { listOf(it) }
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 760.dp),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        item(key = "buy_promo_banner") {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isDark) Color(0xFF1E191E) else Color(0xFFFDF2F3),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF3A2429) else Color(0xFFF7DDE0)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            ) {
              TelefamGoldStarBadgeIcon(size = 42.dp)
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = stringResource(R.string.stars_buy_banner_title),
                  color = primaryText,
                  fontSize = 14.5.sp,
                  fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                  text = stringResource(R.string.stars_buy_banner_sub),
                  color = mutedText,
                  fontSize = 12.sp,
                  lineHeight = 16.5.sp,
                )
              }
            }
          }
        }

        item(key = "buy_section_header") {
          Text(
            text = stringResource(R.string.stars_choose_package),
            color = primaryText,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
          )
        }

        items(rows, key = { it.first() }) { pair ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            pair.forEach { pkgStars ->
              val isPopular = pkgStars == 5L
              val converted = remember(pkgStars, state.countryIso) {
                TelefamStarsCatalog.formatCurrencyForStars(pkgStars, state.countryIso)
              }
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = cardBg,
                border = BorderStroke(
                  width = if (isPopular) 1.5.dp else 1.dp,
                  color = if (isPopular) crimson else borderColor,
                ),
                modifier = Modifier
                  .weight(1f)
                  .testTag("buy_star_row_$pkgStars"),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                  TelefamGoldStarBadgeIcon(size = 36.dp)
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Text(
                        text = TelefamStarsCatalog.formatStarsCount(pkgStars),
                        color = primaryText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                      )
                      if (isPopular) {
                        Surface(
                          shape = CircleShape,
                          color = crimson,
                        ) {
                          Text(
                            text = stringResource(R.string.stars_popular_badge),
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                          )
                        }
                      }
                    }
                    Text(
                      text = converted,
                      color = mutedText,
                      fontSize = 12.sp,
                    )
                  }
                  Button(
                    onClick = { onBuyPackage(pkgStars) },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = crimson, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    modifier = Modifier
                      .minimumInteractiveComponentSize()
                      .testTag("buy_star_btn_$pkgStars"),
                  ) {
                    Text(
                      text = stringResource(R.string.stars_buy_cta),
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp,
                    )
                  }
                }
              }
            }
            if (useTwoColumns && pair.size == 1) {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StarsCheckoutScreen(
  state: TelefamStarsUiState,
  onBack: () -> Unit,
  onSelectProvider: (StarPaymentProvider) -> Unit,
  onProceedToPay: () -> Unit,
) {
  val isDark = LocalTelefamDarkTheme.current
  val pageBg = if (isDark) Color(0xFF101218) else Color(0xFFF6F7FA)
  val cardBg = if (isDark) Color(0xFF171A21) else Color(0xFFFFFFFF)
  val borderColor = if (isDark) Color(0xFF2B303C) else Color(0xFFE5E8EF)
  val primaryText = if (isDark) Color(0xFFF5F7FB) else Color(0xFF1A2233)
  val mutedText = if (isDark) Color(0xFF9BA3B4) else Color(0xFF6B7280)
  val crimson = TelefamCrimson

  val converted = remember(state.selectedPackageStars, state.countryIso) {
    TelefamStarsCatalog.formatCurrencyForStars(state.selectedPackageStars, state.countryIso)
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(pageBg)
      .testTag("stars_checkout_screen"),
  ) {
    Surface(color = crimson, modifier = Modifier.fillMaxWidth()) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 8.dp, vertical = 10.dp),
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("checkout_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.cd_back),
            tint = Color.White,
          )
        }
        Text(
          text = stringResource(R.string.stars_checkout_title),
          color = Color.White,
          fontSize = 18.5.sp,
          fontWeight = FontWeight.Bold,
        )
      }
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .navigationBarsPadding(),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 600.dp)
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Package Summary Card (e.g. "100 Stars / Ksh 100")
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = cardBg,
          border = BorderStroke(1.dp, borderColor),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 16.dp),
          ) {
            TelefamGoldStarBadgeIcon(size = 46.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
              Text(
                text = "${TelefamStarsCatalog.formatStarsCount(state.selectedPackageStars)} ${stringResource(R.string.stars_unit_plural)}",
                color = primaryText,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = converted,
                color = mutedText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
              )
            }
          }
        }

        Text(
          text = stringResource(R.string.stars_payment_method),
          color = primaryText,
          fontSize = 15.5.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(top = 4.dp),
        )

        PaymentMethodOptionCard(
          title = stringResource(R.string.stars_paystack_title),
          subtitle = stringResource(R.string.stars_paystack_sub),
          selected = state.selectedProvider == StarPaymentProvider.PAYSTACK,
          cardBg = cardBg,
          borderColor = borderColor,
          accentColor = crimson,
          primaryText = primaryText,
          mutedText = mutedText,
          icon = { PaystackVectorLogo(size = 28.dp) },
          onClick = { onSelectProvider(StarPaymentProvider.PAYSTACK) },
          testTag = "checkout_method_paystack",
        )

        PaymentMethodOptionCard(
          title = stringResource(R.string.stars_paypal_title),
          subtitle = stringResource(R.string.stars_paypal_sub),
          selected = state.selectedProvider == StarPaymentProvider.PAYPAL,
          cardBg = cardBg,
          borderColor = borderColor,
          accentColor = crimson,
          primaryText = primaryText,
          mutedText = mutedText,
          icon = { PayPalVectorLogo(size = 28.dp) },
          onClick = { onSelectProvider(StarPaymentProvider.PAYPAL) },
          testTag = "checkout_method_paypal",
        )

        // Security shield card (matches Screenshot 3 right)
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = if (isDark) Color(0xFF20181D) else Color(0xFFFCF2F3),
          border = BorderStroke(1.dp, if (isDark) Color(0xFF3A2429) else Color(0xFFF6DCE0)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Shield,
              contentDescription = null,
              tint = crimson,
              modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = stringResource(R.string.stars_security_title),
                color = primaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = stringResource(R.string.stars_security_sub),
                color = mutedText,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
              )
            }
          }
        }

        Button(
          onClick = onProceedToPay,
          enabled = !state.isInitiatingCheckout && !state.isVerifyingPayment,
          shape = CircleShape,
          colors = ButtonDefaults.buttonColors(containerColor = crimson, contentColor = Color.White),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("checkout_proceed_to_pay_button"),
        ) {
          if (state.isInitiatingCheckout) {
            CircularProgressIndicator(
              color = Color.White,
              strokeWidth = 2.dp,
              modifier = Modifier.size(20.dp),
            )
          } else {
            Text(
              text = stringResource(R.string.stars_proceed_to_pay),
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PaymentMethodOptionCard(
  title: String,
  subtitle: String,
  selected: Boolean,
  cardBg: Color,
  borderColor: Color,
  accentColor: Color,
  primaryText: Color,
  mutedText: Color,
  icon: @Composable () -> Unit,
  onClick: () -> Unit,
  testTag: String,
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
        .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
      icon()
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          color = primaryText,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = subtitle,
          color = mutedText,
          fontSize = 12.sp,
          lineHeight = 16.sp,
        )
      }
      RadioButton(
        selected = selected,
        onClick = onClick,
        colors = RadioButtonDefaults.colors(
          selectedColor = accentColor,
          unselectedColor = mutedText,
        ),
      )
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun StarsPaymentWebViewOverlay(
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
      .testTag("stars_payment_webview"),
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
                  if (url.startsWith("telefam://stars/payment-return", ignoreCase = true) ||
                    url.contains("/v1/stars/payment-return", ignoreCase = true)
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

@Composable
private fun TinySolidStarIcon(
  color: Color,
  size: androidx.compose.ui.unit.Dp,
) {
  Canvas(modifier = Modifier.size(size)) {
    val cx = this.size.width / 2f
    val cy = this.size.height / 2f
    val outerR = this.size.minDimension * 0.48f
    val innerR = this.size.minDimension * 0.22f
    val p = Path()
    for (i in 0 until 10) {
      val a = -PI / 2.0 + i * (PI / 5.0)
      val r = if (i % 2 == 0) outerR else innerR
      val x = cx + (r * cos(a)).toFloat()
      val y = cy + (r * sin(a)).toFloat()
      if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawPath(p, color = color)
  }
}
