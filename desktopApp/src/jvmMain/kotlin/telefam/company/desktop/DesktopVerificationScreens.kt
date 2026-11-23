package telefam.company.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DesktopVerifiedBadgeIcon(
  tier: String,
  size: Dp = 18.dp,
  isDarkTheme: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val norm = tier.trim().lowercase()
  if (norm != "business" && norm != "government" && norm != "blue") return
  val desc = when (norm) {
    "business" -> "Verified business badge"
    "government" -> "Verified government badge"
    else -> "Verified blue badge"
  }
  val halo = if (isDarkTheme) Color(0x55FFFFFF) else Color(0x330F172A)

  Canvas(modifier = modifier.size(size).semantics { contentDescription = desc }) {
    val w = this.size.width
    val h = this.size.height
    val cx = w / 2f
    val cy = h / 2f
    val seal = Path()
    val pts = 24
    val outerR = w * 0.48f
    val innerR = w * 0.39f
    for (i in 0 until pts) {
      val a = -PI / 2.0 + i * (2.0 * PI / pts)
      val r = if (i % 2 == 0) outerR else innerR
      val x = cx + (r * cos(a)).toFloat()
      val y = cy + (r * sin(a)).toFloat()
      if (i == 0) seal.moveTo(x, y) else seal.lineTo(x, y)
    }
    seal.close()

    when (norm) {
      "business" -> {
        val brush = if (isDarkTheme) {
          Brush.linearGradient(listOf(Color(0xFFFFE15A), Color(0xFFF5A600)))
        } else {
          Brush.linearGradient(listOf(Color(0xFFF9C42D), Color(0xFFDF8E00)))
        }
        drawPath(seal, brush = brush)
        drawPath(seal, color = halo, style = Stroke(w * 0.03f))
        val markColor = if (isDarkTheme) Color(0xFF1A1405) else Color.White
        val check = Path().apply {
          moveTo(w * 0.31f, h * 0.51f)
          lineTo(w * 0.44f, h * 0.64f)
          lineTo(w * 0.70f, h * 0.37f)
        }
        drawPath(check, color = markColor, style = Stroke(w * 0.115f, cap = StrokeCap.Round, join = StrokeJoin.Round))
      }
      "government" -> {
        val brush = if (isDarkTheme) {
          Brush.linearGradient(listOf(Color(0xFFFF383E), Color(0xFFD90E16)))
        } else {
          Brush.linearGradient(listOf(Color(0xFFE01119), Color(0xFFB3070C)))
        }
        drawPath(seal, brush = brush)
        drawPath(seal, color = halo, style = Stroke(w * 0.03f))
        val shield = Path().apply {
          moveTo(w * 0.50f, h * 0.25f)
          lineTo(w * 0.72f, h * 0.34f)
          lineTo(w * 0.72f, h * 0.53f)
          cubicTo(w * 0.72f, h * 0.67f, w * 0.50f, h * 0.77f, w * 0.50f, h * 0.77f)
          cubicTo(w * 0.50f, h * 0.77f, w * 0.28f, h * 0.67f, w * 0.28f, h * 0.53f)
          lineTo(w * 0.28f, h * 0.34f)
          close()
        }
        drawPath(shield, Color.White, style = Stroke(w * 0.065f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val check = Path().apply {
          moveTo(w * 0.39f, h * 0.51f)
          lineTo(w * 0.47f, h * 0.59f)
          lineTo(w * 0.62f, h * 0.43f)
        }
        drawPath(check, Color.White, style = Stroke(w * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round))
      }
      else -> {
        val brush = if (isDarkTheme) {
          Brush.linearGradient(listOf(Color(0xFF47B5FF), Color(0xFF1683D8)))
        } else {
          Brush.linearGradient(listOf(Color(0xFF23A6FF), Color(0xFF0E7AD6)))
        }
        drawPath(seal, brush = brush)
        drawPath(seal, color = halo, style = Stroke(w * 0.03f))
        val check = Path().apply {
          moveTo(w * 0.31f, h * 0.51f)
          lineTo(w * 0.44f, h * 0.64f)
          lineTo(w * 0.70f, h * 0.37f)
        }
        drawPath(check, Color.White, style = Stroke(w * 0.115f, cap = StrokeCap.Round, join = StrokeJoin.Round))
      }
    }
  }
}

@Composable
private fun DesktopBenefitSvgIcon(iconKey: String, tint: Color, size: Dp = 18.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = Stroke(w * 0.10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (iconKey) {
      "badge", "security" -> {
        val shield = Path().apply {
          moveTo(w * 0.50f, h * 0.12f)
          lineTo(w * 0.84f, h * 0.26f)
          lineTo(w * 0.84f, h * 0.54f)
          cubicTo(w * 0.84f, h * 0.76f, w * 0.50f, h * 0.88f, w * 0.50f, h * 0.88f)
          cubicTo(w * 0.50f, h * 0.88f, w * 0.16f, h * 0.76f, w * 0.16f, h * 0.54f)
          lineTo(w * 0.16f, h * 0.26f)
          close()
        }
        drawPath(shield, tint)
        val check = Path().apply {
          moveTo(w * 0.34f, h * 0.50f)
          lineTo(w * 0.46f, h * 0.62f)
          lineTo(w * 0.68f, h * 0.38f)
        }
        drawPath(check, Color.White, style = stroke)
      }
      "link" -> {
        drawRoundRect(tint, Offset(w * 0.12f, h * 0.36f), Size(w * 0.44f, h * 0.28f), CornerRadius(h * 0.14f, h * 0.14f), style = stroke)
        drawRoundRect(tint, Offset(w * 0.44f, h * 0.36f), Size(w * 0.44f, h * 0.28f), CornerRadius(h * 0.14f, h * 0.14f), style = stroke)
      }
      "group" -> {
        drawCircle(tint, w * 0.14f, Offset(w * 0.38f, h * 0.34f))
        drawCircle(tint, w * 0.11f, Offset(w * 0.68f, h * 0.38f))
        drawRoundRect(tint, Offset(w * 0.16f, h * 0.56f), Size(w * 0.44f, h * 0.24f), CornerRadius(w * 0.12f, w * 0.12f))
        drawRoundRect(tint.copy(alpha = 0.75f), Offset(w * 0.54f, h * 0.58f), Size(w * 0.32f, h * 0.22f), CornerRadius(w * 0.10f, w * 0.10f))
      }
      "analytics" -> {
        val bw = w * 0.18f
        drawRoundRect(tint, Offset(w * 0.16f, h * 0.54f), Size(bw, h * 0.30f), CornerRadius(bw / 2f, bw / 2f))
        drawRoundRect(tint, Offset(w * 0.42f, h * 0.36f), Size(bw, h * 0.48f), CornerRadius(bw / 2f, bw / 2f))
        drawRoundRect(tint, Offset(w * 0.68f, h * 0.18f), Size(bw, h * 0.66f), CornerRadius(bw / 2f, bw / 2f))
      }
      "payments" -> {
        drawRoundRect(tint, Offset(w * 0.12f, h * 0.22f), Size(w * 0.76f, h * 0.56f), CornerRadius(w * 0.12f, w * 0.12f))
        drawLine(Color.White, Offset(w * 0.16f, h * 0.40f), Offset(w * 0.84f, h * 0.40f), strokeWidth = w * 0.08f)
      }
      else -> {
        val star = Path()
        val cx = w / 2f
        val cy = h / 2f
        for (i in 0 until 10) {
          val a = -PI / 2.0 + i * PI / 5.0
          val r = if (i % 2 == 0) w * 0.42f else w * 0.19f
          val x = cx + (r * cos(a)).toFloat()
          val y = cy + (r * sin(a)).toFloat()
          if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
        }
        star.close()
        drawPath(star, tint)
      }
    }
  }
}

@Composable
fun DesktopVerificationHostScreen(
  controller: DesktopVerificationController,
  isDark: Boolean,
) {
  val state by controller.state.collectAsState()
  if (!state.isOpen) return

  Box(modifier = Modifier.fillMaxSize()) {
    when (state.currentRoute) {
      DesktopVerificationRoute.TIERS_PAGER -> DesktopVerificationTiersView(
        state = state,
        isDark = isDark,
        onBack = { controller.navigateBack() },
        onSelectTier = { controller.selectTier(it) },
        onToggleBillingCycle = { controller.toggleBillingCycle(it) },
        onDismissTutorial = { controller.dismissTutorial() },
        onReopenTutorial = { controller.reopenTutorial() },
        onApplyTier = { controller.openApplyOrSubscribe(it) },
      )
      DesktopVerificationRoute.TRIAL_AND_PAYMENT -> DesktopTrialAndPaymentView(
        state = state,
        isDark = isDark,
        onBack = { controller.navigateBack() },
        onToggleBillingCycle = { controller.toggleBillingCycle(it) },
        onActivateTrial = { controller.activateFreeTrial() },
        onSelectProvider = { controller.selectProvider(it) },
        onProceedToPay = { controller.proceedToSubscribePayment() },
        onVerifyPayment = { controller.verifyActivePayment() },
      )
      DesktopVerificationRoute.SELFIE_GUIDANCE -> DesktopSelfieGuidanceView(
        state = state,
        isDark = isDark,
        onBack = { controller.navigateBack() },
        onStartCamera = { controller.proceedFromSelfieGuidanceToLiveCamera() },
      )
      DesktopVerificationRoute.SELFIE_LIVENESS -> DesktopSelfieLiveCaptureView(
        state = state,
        isDark = isDark,
        onBack = { controller.navigateBack() },
        onProgress = { idx, pct, visible, light, prompt ->
          controller.updateLiveSelfieProgress(idx, pct, visible, light, prompt)
        },
        onAutoCaptured = { bytes, steps -> controller.submitAutoCapturedLiveness(bytes, steps) },
      )
      DesktopVerificationRoute.ID_GUIDANCE_AND_SELECT -> DesktopIdGuidanceAndSelectView(
        state = state,
        isDark = isDark,
        onBack = { controller.navigateBack() },
        onSelectDoc = { controller.selectDocType(it) },
        onStartScan = { controller.startLiveIdCapture() },
      )
      DesktopVerificationRoute.ID_LIVE_CAPTURE -> DesktopIdLiveCaptureView(
        state = state,
        isDark = isDark,
        onBack = { controller.navigateBack() },
        onReadiness = { c, sh, li, gl, ocr, pct -> controller.updateLiveDocReadiness(c, sh, li, gl, ocr, pct) },
        onSubmitDocument = { bytes, ocr, sh, li, gl, corners ->
          controller.submitAutoCapturedDocument(bytes, ocr, sh, li, gl, corners)
        },
      )
      DesktopVerificationRoute.STATUS_REVIEW -> DesktopVerificationStatusView(
        state = state,
        isDark = isDark,
        onBack = { controller.navigateBack() },
        onRefresh = { controller.refreshStatus(silent = false) },
        onBrowseTiers = { controller.openTiersExplorer() },
      )
    }

    val banner = state.errorMessage ?: state.statusMessage
    if (!banner.isNullOrBlank()) {
      val isErr = !state.errorMessage.isNullOrBlank()
      Surface(
        onClick = { controller.dismissBanner() },
        shape = CircleShape,
        color = if (isErr) Color(0xFFD91E2A) else Color(0xEB151821),
        shadowElevation = 6.dp,
        modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 24.dp, vertical = 20.dp),
      ) {
        Text(banner, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
      }
    }
  }
}

@Composable
private fun DesktopVerificationHeader(title: String, subtitle: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
  Surface(color = DesktopCrimson, modifier = Modifier.fillMaxWidth()) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
      }
      Spacer(modifier = Modifier.width(6.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
        Text(subtitle, color = Color.White.copy(alpha = 0.88f), fontSize = 12.5.sp)
      }
      trailing()
    }
  }
}

@Composable
private fun DesktopVerificationTiersView(
  state: DesktopVerificationUiState,
  isDark: Boolean,
  onBack: () -> Unit,
  onSelectTier: (DesktopVerificationTier) -> Unit,
  onToggleBillingCycle: (Boolean) -> Unit,
  onDismissTutorial: () -> Unit,
  onReopenTutorial: () -> Unit,
  onApplyTier: (DesktopVerificationTier) -> Unit,
) {
  val tiers = listOf(DesktopVerificationTier.BUSINESS, DesktopVerificationTier.GOVERNMENT, DesktopVerificationTier.BLUE)
  val pageBg = if (isDark) Color(0xFF0E1015) else Color(0xFFF6F7FA)
  val cardBg = if (isDark) Color(0xFF171A21) else Color.White
  val tintBg = if (isDark) Color(0xFF1E171B) else Color(0xFFFCF3F4)
  val border = if (isDark) Color(0xFF2B303C) else Color(0xFFEAECEF)
  val primaryText = if (isDark) Color(0xFFF4F6FA) else Color(0xFF141821)
  val mutedText = if (isDark) Color(0xFF9BA4B5) else Color(0xFF667085)
  val tier = state.selectedTier
  val pricing = DesktopVerificationCatalog.pricingForTier(tier, state.countryIso, state.status.tiers)
  val isAnnual = state.billingCycle == DesktopVerificationBillingCycle.ANNUAL
  var readMore by remember(tier) { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxSize().background(pageBg)) {
    Column(modifier = Modifier.fillMaxSize()) {
      DesktopVerificationHeader(
        title = "Verification",
        subtitle = tier.headerSubtitle,
        onBack = onBack,
        trailing = {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tiers.forEach { t ->
              val sel = t == tier
              Surface(
                onClick = { onSelectTier(t) },
                shape = CircleShape,
                color = if (sel) Color.White else Color.White.copy(alpha = 0.20f),
              ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  DesktopVerifiedBadgeIcon(t.wire, 15.dp, isDarkTheme = !sel)
                  Text(t.name.lowercase().replaceFirstChar { it.uppercase() }, color = if (sel) DesktopCrimson else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
              }
            }
            IconButton(onClick = onReopenTutorial, modifier = Modifier.size(48.dp)) {
              Icon(Icons.Filled.Info, contentDescription = "Tutorial", tint = Color.White)
            }
          }
        },
      )

      Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(modifier = Modifier.fillMaxSize().widthIn(max = 720.dp), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
              Surface(shape = CircleShape, color = if (isDark) Color(0xFF1E222B) else Color(0xFF151821), border = BorderStroke(2.5.dp, DesktopCrimson), modifier = Modifier.size(72.dp)) {
                Box(contentAlignment = Alignment.Center) { DesktopVerifiedBadgeIcon(tier.wire, 34.dp, isDarkTheme = isDark) }
              }
              Spacer(modifier = Modifier.width(14.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(state.profileFullName.ifBlank { tier.headerTitle }, color = primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                  DesktopVerifiedBadgeIcon(tier.wire, 20.dp, isDarkTheme = isDark)
                }
                Text(if (state.profileUsername.isNotBlank()) "@${state.profileUsername}" else "@verified_account", color = mutedText, fontSize = 13.sp)
              }
            }
          }

          item {
            Surface(shape = RoundedCornerShape(16.dp), color = tintBg, border = BorderStroke(1.dp, border), modifier = Modifier.fillMaxWidth()) {
              Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(if (isAnnual) "${pricing.annualDisplay} / year" else "${pricing.monthlyDisplay} / month", color = primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                  Text(if (isAnnual) "Billed annually (${pricing.currencyCode})" else "Billed monthly", color = mutedText, fontSize = 12.5.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = CircleShape, color = DesktopCrimson) {
                      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 2.dp, bottom = 2.dp)) {
                        Text("Annual", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(checked = isAnnual, onCheckedChange = onToggleBillingCycle, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF9E0B10)))
                      }
                    }
                    Surface(shape = CircleShape, color = DesktopCrimson) {
                      Text("20% OFF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text("${pricing.annualDisplay} / year", color = primaryText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                  Text(pricing.annualOriginalDisplay, color = mutedText, fontSize = 11.5.sp, textDecoration = TextDecoration.LineThrough)
                }
              }
            }
          }

          item {
            Text(
              text = when (tier) {
                DesktopVerificationTier.BUSINESS -> "Business Verification Benefits"
                DesktopVerificationTier.GOVERNMENT -> "Government Verification Benefits"
                DesktopVerificationTier.BLUE -> "Verification Benefits"
              },
              color = primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp,
            )
          }

          if (tier == DesktopVerificationTier.BLUE) {
            items(DesktopVerificationCatalog.blueBenefits.size) { idx ->
              val b = DesktopVerificationCatalog.blueBenefits[idx]
              Surface(shape = RoundedCornerShape(14.dp), color = cardBg, border = BorderStroke(1.dp, border), modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                  Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(tintBg)) {
                    DesktopBenefitSvgIcon(b.iconKey, DesktopCrimson, 19.dp)
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(b.title, color = primaryText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(b.description, color = mutedText, fontSize = 12.sp)
                  }
                  Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = mutedText)
                }
              }
            }
          } else {
            val list = if (tier == DesktopVerificationTier.BUSINESS) DesktopVerificationCatalog.businessBenefits else DesktopVerificationCatalog.governmentBenefits
            val pairs = list.chunked(2)
            items(pairs.size) { r ->
              Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                pairs[r].forEach { b ->
                  Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(tintBg)) {
                      DesktopBenefitSvgIcon(b.iconKey, DesktopCrimson, 18.dp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                      Text(b.title, color = primaryText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                      Text(b.description, color = mutedText, fontSize = 11.5.sp, lineHeight = 15.sp)
                    }
                  }
                }
                if (pairs[r].size == 1) Spacer(modifier = Modifier.weight(1f))
              }
            }
            item {
              Surface(onClick = { readMore = !readMore }, shape = RoundedCornerShape(14.dp), color = tintBg, border = BorderStroke(1.dp, border), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = DesktopCrimson, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(if (tier == DesktopVerificationTier.BUSINESS) "Why Go Business?" else "Why Verify as a Government Official?", color = primaryText, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
                    Text("Read More", color = DesktopCrimson, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Icon(if (readMore) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = DesktopCrimson)
                  }
                  AnimatedVisibility(visible = readMore) {
                    Text("Includes a 7-day free trial, server-verified live selfie liveness, official document OCR matching, and secure automatic photo deletion after processing.", color = mutedText, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                  }
                }
              }
            }
          }

          item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
              Button(
                onClick = { onApplyTier(tier) },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = DesktopCrimson, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(50.dp),
              ) {
                Text(if (tier == DesktopVerificationTier.BLUE) "Subscribe Now" else "Apply Now ->", fontWeight = FontWeight.Bold, fontSize = 15.sp)
              }
              Spacer(modifier = Modifier.height(6.dp))
              Text(if (tier == DesktopVerificationTier.BLUE) "Cancel anytime. Secure payment." else "First week free · Secure · Trusted", color = mutedText, fontSize = 12.sp)
            }
          }
        }
      }
    }

    if (state.showFirstOpenTutorial) {
      var step by remember { mutableIntStateOf(0) }
      val steps = listOf(
        "Browse 3 Verification Tiers" to "Switch between Business, Government, and Blue badges with country-specific pricing.",
        "First Week Free + How It Works" to "Unlock a 7-day free trial or subscribe via Paystack or PayPal.",
        "Live Selfie & ID Check" to "Complete a guided head movement selfie check and ID scan. Photos are deleted securely after verification.",
      )
      Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.62f)), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(22.dp), color = cardBg, modifier = Modifier.widthIn(max = 460.dp).padding(24.dp)) {
          Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              DesktopVerifiedBadgeIcon("business", 28.dp, isDarkTheme = isDark)
              DesktopVerifiedBadgeIcon("government", 28.dp, isDarkTheme = isDark)
              DesktopVerifiedBadgeIcon("blue", 28.dp, isDarkTheme = isDark)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(steps[step].first, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = primaryText, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text(steps[step].second, fontSize = 13.5.sp, color = mutedText, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
              OutlinedButton(onClick = onDismissTutorial, shape = CircleShape, modifier = Modifier.weight(1f).height(46.dp)) { Text("Skip") }
              Button(
                onClick = { if (step < steps.lastIndex) step++ else onDismissTutorial() },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = DesktopCrimson, contentColor = Color.White),
                modifier = Modifier.weight(1f).height(46.dp),
              ) { Text(if (step < steps.lastIndex) "Next" else "Get Started") }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DesktopTrialAndPaymentView(
  state: DesktopVerificationUiState,
  isDark: Boolean,
  onBack: () -> Unit,
  onToggleBillingCycle: (Boolean) -> Unit,
  onActivateTrial: () -> Unit,
  onSelectProvider: (DesktopVerificationProvider) -> Unit,
  onProceedToPay: () -> Unit,
  onVerifyPayment: () -> Unit,
) {
  val pageBg = if (isDark) Color(0xFF0E1015) else Color(0xFFF6F7FA)
  val cardBg = if (isDark) Color(0xFF171A21) else Color.White
  val border = if (isDark) Color(0xFF2B303C) else Color(0xFFE5E8EF)
  val primaryText = if (isDark) Color(0xFFF4F6FA) else Color(0xFF141821)
  val mutedText = if (isDark) Color(0xFF9BA4B5) else Color(0xFF667085)
  val pricing = DesktopVerificationCatalog.pricingForTier(state.selectedTier, state.countryIso, state.status.tiers)
  val isAnnual = state.billingCycle == DesktopVerificationBillingCycle.ANNUAL

  Column(modifier = Modifier.fillMaxSize().background(pageBg)) {
    DesktopVerificationHeader(title = state.selectedTier.headerTitle, subtitle = "How verification works", onBack = onBack)
    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
      LazyColumn(modifier = Modifier.fillMaxSize().widthIn(max = 680.dp), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
          Surface(shape = RoundedCornerShape(18.dp), color = if (isDark) Color(0xFF1F161A) else Color(0xFFFFF5F6), border = BorderStroke(1.5.dp, DesktopCrimson), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DesktopVerifiedBadgeIcon(state.selectedTier.wire, 28.dp, isDarkTheme = isDark)
                Column(modifier = Modifier.weight(1f)) {
                  Text("First Week Free Offer", color = primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                  Text(if (state.status.trialEligible) "Enjoy 7 days free before billing begins. Complete live identity checks right after unlocking." else "One-week free trial already used on this account. Subscribe below to verify.", color = mutedText, fontSize = 12.5.sp)
                }
              }
              if (state.status.trialEligible) {
                Button(onClick = onActivateTrial, enabled = !state.isActivatingTrial, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = DesktopCrimson, contentColor = Color.White), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                  if (state.isActivatingTrial) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp)) else Text("Unlock First Week Free", fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
        item {
          Surface(shape = RoundedCornerShape(16.dp), color = cardBg, border = BorderStroke(1.dp, border), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text("How verification works", color = primaryText, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
              Text("1. Start your 7-day free trial or subscribe via Paystack or PayPal.", color = mutedText, fontSize = 13.sp)
              Text("2. Complete live guided selfie verification (screen recording blocked).", color = mutedText, fontSize = 13.sp)
              Text("3. Capture a valid, unexpired government or business ID for server OCR.", color = mutedText, fontSize = 13.sp)
              Text("4. Application enters review (up to 3 working days). All captured photos are deleted securely.", color = mutedText, fontSize = 13.sp)
            }
          }
        }
        item {
          Surface(shape = RoundedCornerShape(16.dp), color = cardBg, border = BorderStroke(1.dp, border), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(if (isAnnual) "${pricing.annualDisplay} / year" else "${pricing.monthlyDisplay} / month", color = primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                  Text(if (isAnnual) "20% annual discount (${pricing.currencyCode})" else "Monthly billing (${pricing.currencyCode})", color = mutedText, fontSize = 12.sp)
                }
                Switch(checked = isAnnual, onCheckedChange = onToggleBillingCycle, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = DesktopCrimson))
              }
              listOf(
                Triple(DesktopVerificationProvider.PAYSTACK, "Paystack", "Pay securely with M-Pesa, cards and bank transfers"),
                Triple(DesktopVerificationProvider.PAYPAL, "PayPal", "Pay with your PayPal account or card"),
              ).forEach { (p, title, sub) ->
                val sel = state.selectedProvider == p
                Surface(onClick = { onSelectProvider(p) }, shape = RoundedCornerShape(14.dp), color = cardBg, border = BorderStroke(if (sel) 1.6.dp else 1.dp, if (sel) DesktopCrimson else border), modifier = Modifier.fillMaxWidth()) {
                  Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (p == DesktopVerificationProvider.PAYSTACK) DesktopPaystackLogo(30.dp) else DesktopPayPalLogo(30.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(title, fontWeight = FontWeight.Bold, color = primaryText, fontSize = 14.sp)
                      Text(sub, color = mutedText, fontSize = 12.sp)
                    }
                    RadioButton(selected = sel, onClick = { onSelectProvider(p) }, colors = RadioButtonDefaults.colors(selectedColor = DesktopCrimson))
                  }
                }
              }
              if (state.awaitingBrowserReturn && state.activePayment != null) {
                OutlinedButton(onClick = onVerifyPayment, shape = CircleShape, border = BorderStroke(1.2.dp, DesktopCrimson), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                  Text("Verify Payment", color = DesktopCrimson, fontWeight = FontWeight.Bold)
                }
              }
              Button(onClick = onProceedToPay, enabled = !state.isInitiatingCheckout && !state.isVerifyingPayment, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = DesktopCrimson, contentColor = Color.White), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                if (state.isInitiatingCheckout) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                else Text("Proceed to Pay", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DesktopSelfieGuidanceView(
  state: DesktopVerificationUiState,
  isDark: Boolean,
  onBack: () -> Unit,
  onStartCamera: () -> Unit,
) {
  val pageBg = if (isDark) Color(0xFF0E1015) else Color(0xFFF6F7FA)
  val cardBg = if (isDark) Color(0xFF171A21) else Color.White
  val primaryText = if (isDark) Color(0xFFF4F6FA) else Color(0xFF141821)
  val mutedText = if (isDark) Color(0xFF9BA4B5) else Color(0xFF667085)

  Column(modifier = Modifier.fillMaxSize().background(pageBg)) {
    DesktopVerificationHeader(title = "Live Selfie Check", subtitle = state.selectedTier.headerTitle, onBack = onBack)
    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
      Column(modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(shape = RoundedCornerShape(18.dp), color = cardBg, modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.Shield, contentDescription = null, tint = DesktopCrimson, modifier = Modifier.size(40.dp))
            Text("Follow live movement prompts", color = primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text("Look into the camera and follow prompts: Left, Right, Up, Left, Right, Center. Auto-captures at 100%.", color = mutedText, fontSize = 13.sp, textAlign = TextAlign.Center)
            Surface(shape = RoundedCornerShape(12.dp), color = if (isDark) Color(0xFF14221D) else Color(0xFFECFDF5), modifier = Modifier.fillMaxWidth()) {
              Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Verification selfie photos are securely purged immediately after review.", color = if (isDark) Color(0xFFD1FAE5) else Color(0xFF065F46), fontSize = 12.sp)
              }
            }
            Button(onClick = onStartCamera, enabled = !state.isSubmittingLiveness, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = DesktopCrimson, contentColor = Color.White), modifier = Modifier.fillMaxWidth().height(50.dp)) {
              if (state.isSubmittingLiveness) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
              else Text("Start Live Selfie", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DesktopSelfieLiveCaptureView(
  state: DesktopVerificationUiState,
  isDark: Boolean,
  onBack: () -> Unit,
  onProgress: (Int, Int, Boolean, Boolean, String) -> Unit,
  onAutoCaptured: (ByteArray, List<DesktopLivenessTelemetryStep>) -> Unit,
) {
  var previewBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
  var cameraError by remember { mutableStateOf<String?>(null) }
  val capturedSteps = remember(state.livenessChallengeId) { mutableStateListOf<DesktopLivenessTelemetryStep>() }
  val seq = state.livenessSequence.ifEmpty { listOf("left", "right", "up", "left", "right", "center") }

  LaunchedEffect(state.livenessChallengeId, state.screenCaptureBlocked) {
    if (state.screenCaptureBlocked) return@LaunchedEffect
    val proc = withContext(Dispatchers.IO) { DesktopVerificationFrameAnalyzer.openWebcamMjpegProcess() }
    if (proc == null) {
      cameraError = "No active webcam detected on this desktop. Connect a webcam or finish live selfie from Telefam mobile."
      return@LaunchedEffect
    }
    var stepIdx = 0
    var lastStepTs = 0L
    var autoSubmitted = false
    try {
      withContext(Dispatchers.IO) {
        val inp = proc.inputStream
        val buf = ByteArray(4096)
        val frameStream = ByteArrayOutputStream()
        var inFrame = false
        var prev = -1
        while (isActive && !autoSubmitted) {
          val n = inp.read(buf)
          if (n <= 0) break
          for (i in 0 until n) {
            val b = buf[i].toInt() and 0xFF
            if (prev == 0xFF && b == 0xD8) {
              frameStream.reset()
              frameStream.write(0xFF)
              frameStream.write(0xD8)
              inFrame = true
            } else if (inFrame) {
              frameStream.write(b)
              if (prev == 0xFF && b == 0xD9) {
                inFrame = false
                val jpg = frameStream.toByteArray()
                val img = runCatching { ImageIO.read(ByteArrayInputStream(jpg)) }.getOrNull()
                if (img != null) {
                  val bmp = img.toComposeImageBitmap()
                  withContext(Dispatchers.Main) { previewBitmap = bmp }
                  val (luma, cx, cy) = DesktopVerificationFrameAnalyzer.analyzeFaceRegion(img)
                  val now = System.currentTimeMillis()
                  val lightOk = luma >= 35.0
                  val faceOk = lightOk && img.width >= 240 && img.height >= 240
                  if (stepIdx < seq.size) {
                    val expected = seq[stepIdx]
                    val yawDeg = cx * 32.0
                    val pitchDeg = -cy * 26.0
                    val matched = faceOk && when (expected) {
                      "left" -> yawDeg <= -11.0
                      "right" -> yawDeg >= 11.0
                      "up" -> pitchDeg >= 8.0
                      else -> abs(yawDeg) <= 10.0 && abs(pitchDeg) <= 10.0
                    }
                    if (matched && now - lastStepTs >= 420L) {
                      lastStepTs = now
                      capturedSteps.add(DesktopLivenessTelemetryStep(expected, yawDeg, pitchDeg, 0.92, 0.92, luma, now))
                      stepIdx++
                    }
                    val pct = (stepIdx * 100) / seq.size
                    val prompt = if (stepIdx < seq.size) DesktopVerificationCatalog.stepDirectionLabel(seq[stepIdx]) else "Auto-capturing..."
                    withContext(Dispatchers.Main) { onProgress(stepIdx, pct, faceOk, lightOk, prompt) }
                    if (stepIdx >= seq.size && !autoSubmitted) {
                      autoSubmitted = true
                      val encoded = DesktopVerificationFrameAnalyzer.encodeRgbJpeg(img)
                      withContext(Dispatchers.Main) { onAutoCaptured(encoded, capturedSteps.toList()) }
                    }
                  }
                }
              }
            }
            prev = b
          }
        }
      }
    } finally {
      runCatching { proc.destroyForcibly() }
    }
  }

  Column(modifier = Modifier.fillMaxSize().background(Color(0xFF080A0E))) {
    DesktopVerificationHeader(title = "Live Selfie Liveness", subtitle = "${state.currentLivenessScore}% complete", onBack = onBack)
    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
      val bmp = previewBitmap
      if (bmp != null) {
        Image(bitmap = bmp, contentDescription = "Live camera", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
      } else if (cameraError != null) {
        Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF171A21), modifier = Modifier.widthIn(max = 460.dp).padding(24.dp)) {
          Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(cameraError ?: "", color = Color.White, textAlign = TextAlign.Center, fontSize = 14.sp)
            OutlinedButton(onClick = onBack, shape = CircleShape) { Text("Go Back", color = Color.White) }
          }
        }
      }
      Column(modifier = Modifier.align(Alignment.BottomCenter).widthIn(max = 460.dp).padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.Black.copy(alpha = 0.72f), modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(state.liveMovementPrompt, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            LinearProgressIndicator(progress = { state.currentLivenessScore / 100f }, color = Color(0xFF28D17C), trackColor = Color.White.copy(alpha = 0.25f), modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape))
          }
        }
      }
    }
  }
}

@Composable
private fun DesktopIdGuidanceAndSelectView(
  state: DesktopVerificationUiState,
  isDark: Boolean,
  onBack: () -> Unit,
  onSelectDoc: (DesktopVerificationDocumentType) -> Unit,
  onStartScan: () -> Unit,
) {
  val pageBg = if (isDark) Color(0xFF0E1015) else Color(0xFFF6F7FA)
  val cardBg = if (isDark) Color(0xFF171A21) else Color.White
  val border = if (isDark) Color(0xFF2B303C) else Color(0xFFE5E8EF)
  val primaryText = if (isDark) Color(0xFFF4F6FA) else Color(0xFF141821)
  val mutedText = if (isDark) Color(0xFF9BA4B5) else Color(0xFF667085)
  val docs = DesktopVerificationCatalog.documentOptionsForTier(state.selectedTier)

  Column(modifier = Modifier.fillMaxSize().background(pageBg)) {
    DesktopVerificationHeader(title = "Select Verification ID", subtitle = state.selectedTier.headerTitle, onBack = onBack)
    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
      LazyColumn(modifier = Modifier.fillMaxSize().widthIn(max = 740.dp), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
          Surface(shape = RoundedCornerShape(16.dp), color = cardBg, border = BorderStroke(1.dp, border), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text("Choose your identification document", color = primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 15.5.sp)
              docs.forEach { d ->
                val sel = state.selectedDocumentType == d
                Surface(onClick = { onSelectDoc(d) }, shape = RoundedCornerShape(12.dp), color = if (sel) DesktopCrimson.copy(alpha = 0.12f) else pageBg, border = BorderStroke(if (sel) 1.5.dp else 1.dp, if (sel) DesktopCrimson else border), modifier = Modifier.fillMaxWidth()) {
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(d.label, color = primaryText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                      Text(d.subtitle, color = mutedText, fontSize = 11.5.sp)
                    }
                    RadioButton(selected = sel, onClick = { onSelectDoc(d) }, colors = RadioButtonDefaults.colors(selectedColor = DesktopCrimson))
                  }
                }
              }
            }
          }
        }
        item {
          Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF0E1B1A), border = BorderStroke(1.2.dp, Color(0xFF1B6E52)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text("Use a valid, government-issued ID", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
              Text("The ID must be original, unexpired, and clearly readable.", color = Color(0xFFB5C5C2), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF28D17C), modifier = Modifier.size(18.dp))
                    Text("Correct ID", color = Color(0xFF28D17C), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                  }
                  DesktopVerificationCatalog.correctIdRules.forEach { r ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                      Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF28D17C), modifier = Modifier.size(13.dp))
                      Text(r, color = Color(0xFFE5EFEA), fontSize = 12.sp)
                    }
                  }
                }
                DesktopMiniIdCardVector("valid", Modifier.weight(1.1f).height(96.dp))
              }
            }
          }
        }
        item {
          Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF151822), border = BorderStroke(1.dp, Color(0xFF3A2429)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(18.dp))
                Text("Incorrect ID — Avoid these", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold, fontSize = 14.sp)
              }
              val rows = DesktopVerificationCatalog.incorrectIdExamples.chunked(4)
              rows.forEach { rList ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                  rList.forEach { ex ->
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF1D2230), modifier = Modifier.weight(1f)) {
                      Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DesktopMiniIdCardVector(ex.key, Modifier.fillMaxWidth().height(54.dp))
                        Text(ex.label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                      }
                    }
                  }
                }
              }
            }
          }
        }
        item {
          Surface(shape = RoundedCornerShape(14.dp), color = if (isDark) Color(0xFF14221D) else Color(0xFFECFDF5), border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.45f)), modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
              Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(10.dp))
              Text("Your verification photos and ID scans are deleted automatically and securely after verification is processed.", color = if (isDark) Color(0xFFD1FAE5) else Color(0xFF065F46), fontSize = 12.sp)
            }
          }
        }
        item {
          Button(onClick = onStartScan, enabled = !state.isSubmittingDocument, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = DesktopCrimson, contentColor = Color.White), modifier = Modifier.fillMaxWidth().height(50.dp)) {
            if (state.isSubmittingDocument) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            else Text("Start Live ID Scan", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun DesktopMiniIdCardVector(exampleKey: String, modifier: Modifier = Modifier) {
  val cardAlpha = when (exampleKey) {
    "too_dark" -> 0.42f
    "blurry" -> 0.62f
    else -> 1f
  }
  val rot = if (exampleKey == "aligned") -8f else 0f
  Box(modifier = modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF10131C)), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize().padding(if (exampleKey == "too_far") 14.dp else 4.dp).rotate(rot).alpha(cardAlpha)) {
      val w = size.width
      val h = size.height
      drawRoundRect(Color(0xFFE9EDF5), Offset.Zero, Size(w, h), CornerRadius(6f, 6f))
      drawRect(Color(0xFF2B3548), Offset(w * 0.08f, h * 0.24f), Size(w * 0.22f, h * 0.46f))
      drawLine(Color(0xFF475569), Offset(w * 0.36f, h * 0.30f), Offset(w * 0.86f, h * 0.30f), strokeWidth = 3f)
      drawLine(Color(0xFF64748B), Offset(w * 0.36f, h * 0.48f), Offset(w * 0.78f, h * 0.48f), strokeWidth = 2.5f)
      if (exampleKey == "glare") {
        drawCircle(Color.White.copy(alpha = 0.85f), radius = w * 0.14f, center = Offset(w * 0.60f, h * 0.44f))
      }
      if (exampleKey == "obstructed") {
        drawCircle(Color(0xFFD99B78), radius = w * 0.17f, center = Offset(w * 0.82f, h * 0.56f))
      }
      if (exampleKey == "valid") {
        val bracket = Color(0xFF28D17C)
        val b = w * 0.08f
        drawLine(bracket, Offset(0f, 0f), Offset(b, 0f), strokeWidth = 3f)
        drawLine(bracket, Offset(0f, 0f), Offset(0f, b), strokeWidth = 3f)
        drawLine(bracket, Offset(w, h), Offset(w - b, h), strokeWidth = 3f)
        drawLine(bracket, Offset(w, h), Offset(w, h - b), strokeWidth = 3f)
      }
    }
  }
}

@Composable
private fun DesktopIdLiveCaptureView(
  state: DesktopVerificationUiState,
  isDark: Boolean,
  onBack: () -> Unit,
  onReadiness: (Boolean, Boolean, Boolean, Boolean, Boolean, Int) -> Unit,
  onSubmitDocument: (ByteArray, String, Double, Double, Double, Boolean) -> Unit,
) {
  var previewBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
  var cameraUnavailable by remember { mutableStateOf(false) }

  LaunchedEffect(state.documentChallengeId, state.screenCaptureBlocked) {
    if (state.screenCaptureBlocked) return@LaunchedEffect
    val proc = withContext(Dispatchers.IO) { DesktopVerificationFrameAnalyzer.openWebcamMjpegProcess() }
    if (proc == null) {
      cameraUnavailable = true
      return@LaunchedEffect
    }
    var submitted = false
    try {
      withContext(Dispatchers.IO) {
        val inp = proc.inputStream
        val buf = ByteArray(4096)
        val frameStream = ByteArrayOutputStream()
        var inFrame = false
        var prev = -1
        while (isActive && !submitted) {
          val n = inp.read(buf)
          if (n <= 0) break
          for (i in 0 until n) {
            val b = buf[i].toInt() and 0xFF
            if (prev == 0xFF && b == 0xD8) {
              frameStream.reset()
              frameStream.write(0xFF); frameStream.write(0xD8)
              inFrame = true
            } else if (inFrame) {
              frameStream.write(b)
              if (prev == 0xFF && b == 0xD9) {
                inFrame = false
                val jpg = frameStream.toByteArray()
                val img = runCatching { ImageIO.read(ByteArrayInputStream(jpg)) }.getOrNull()
                if (img != null) {
                  val bmp = img.toComposeImageBitmap()
                  withContext(Dispatchers.Main) { previewBitmap = bmp }
                  val metrics = DesktopVerificationFrameAnalyzer.analyzeDocumentImage(img)
                  withContext(Dispatchers.Main) {
                    onReadiness(metrics.allCornersDetected, metrics.sharpnessScore >= 0.35, metrics.luminanceMean >= 40.0, metrics.glareRatio <= 0.35, metrics.readinessPct >= 80, metrics.readinessPct)
                    if (metrics.readinessPct == 100 && !submitted) {
                      submitted = true
                      onSubmitDocument(metrics.jpegBytes, state.profileFullName, metrics.sharpnessScore, metrics.luminanceMean, metrics.glareRatio, metrics.allCornersDetected)
                    }
                  }
                }
              }
            }
            prev = b
          }
        }
      }
    } finally {
      runCatching { proc.destroyForcibly() }
    }
  }

  Column(modifier = Modifier.fillMaxSize().background(Color(0xFF080A0E))) {
    DesktopVerificationHeader(title = "Scan ${state.selectedDocumentType.label}", subtitle = "${state.docQualityReadinessPct}% ready", onBack = onBack)
    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
      val bmp = previewBitmap
      if (bmp != null) {
        Image(bitmap = bmp, contentDescription = "Document live stream", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
      } else if (cameraUnavailable) {
        Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF171A21), modifier = Modifier.widthIn(max = 480.dp).padding(24.dp)) {
          Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Live webcam stream unavailable on this workstation.", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center)
            Text("Attach a clear, uncropped ID scan image from your connected scanner/camera so the server can validate quality metrics and run OCR.", color = Color(0xFF9BA4B5), fontSize = 13.sp, textAlign = TextAlign.Center)
            Button(
              onClick = {
                val dlg = FileDialog(null as Frame?, "Select scanned ID image", FileDialog.LOAD).apply {
                  isMultipleMode = false
                  setFilenameFilter { _, name ->
                    val l = name.lowercase()
                    l.endsWith(".jpg") || l.endsWith(".jpeg") || l.endsWith(".png")
                  }
                  isVisible = true
                }
                val file = dlg.file?.let { File(dlg.directory, it) }
                if (file != null && file.exists()) {
                  val img = runCatching { ImageIO.read(file) }.getOrNull()
                  if (img != null) {
                    val m = DesktopVerificationFrameAnalyzer.analyzeDocumentImage(img)
                    onReadiness(m.allCornersDetected, m.sharpnessScore >= 0.35, m.luminanceMean >= 40.0, m.glareRatio <= 0.35, true, m.readinessPct)
                    onSubmitDocument(m.jpegBytes, state.profileFullName, m.sharpnessScore, m.luminanceMean, m.glareRatio, m.allCornersDetected)
                  }
                }
              },
              shape = CircleShape,
              colors = ButtonDefaults.buttonColors(containerColor = DesktopCrimson, contentColor = Color.White),
              modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("Select ID Image for Server OCR", fontWeight = FontWeight.Bold) }
          }
        }
      }
      Column(modifier = Modifier.align(Alignment.BottomCenter).widthIn(max = 460.dp).padding(20.dp)) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.Black.copy(alpha = 0.72f), modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = if (state.isSubmittingDocument) "Uploading & verifying ID with OCR..." else "Quality readiness: ${state.docQualityReadinessPct}%",
              color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            LinearProgressIndicator(progress = { state.docQualityReadinessPct / 100f }, color = Color(0xFF28D17C), trackColor = Color.White.copy(alpha = 0.25f), modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape))
          }
        }
      }
    }
  }
}

@Composable
private fun DesktopVerificationStatusView(
  state: DesktopVerificationUiState,
  isDark: Boolean,
  onBack: () -> Unit,
  onRefresh: () -> Unit,
  onBrowseTiers: () -> Unit,
) {
  val st = state.status
  val isVerified = st.reviewStatus == "verified" && st.verifiedTier != "none" && st.verifiedTier.isNotBlank()
  val isUnderReview = st.reviewStatus == "under_review"
  val pageBg = if (isDark) Color(0xFF0E1015) else Color(0xFFF6F7FA)
  val cardBg = if (isDark) Color(0xFF171A21) else Color.White
  val border = if (isDark) Color(0xFF2B303C) else Color(0xFFE5E8EF)
  val primaryText = if (isDark) Color(0xFFF4F6FA) else Color(0xFF141821)
  val mutedText = if (isDark) Color(0xFF9BA4B5) else Color(0xFF667085)

  Column(modifier = Modifier.fillMaxSize().background(pageBg)) {
    DesktopVerificationHeader(title = "Verification Status", subtitle = if (isVerified) "Badge active" else if (isUnderReview) "Under review" else "Status", onBack = onBack)
    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
      Column(modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(shape = RoundedCornerShape(18.dp), color = cardBg, border = BorderStroke(1.dp, border), modifier = Modifier.fillMaxWidth()) {
          Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(22.dp)) {
            if (isVerified) {
              DesktopVerifiedBadgeIcon(st.verifiedTier, 54.dp, isDarkTheme = isDark)
              Text("${DesktopVerificationTier.fromWire(st.verifiedTier).headerTitle} Active", color = primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
              Text("Your verified badge is unlocked and visible on your Profile and Feeds.", color = mutedText, fontSize = 13.sp, textAlign = TextAlign.Center)
            } else {
              Icon(Icons.Filled.Shield, contentDescription = null, tint = DesktopCrimson, modifier = Modifier.size(46.dp))
              Text(if (isUnderReview) "Verification under review" else "Verification status updated", color = primaryText, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
              Text("It always takes 3 working days to review. You will be notified.", color = mutedText, fontSize = 13.5.sp, textAlign = TextAlign.Center)
              if (st.rejectionReason.isNotBlank()) {
                Text("Reason: ${st.rejectionReason}", color = DesktopCrimson, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
              }
            }
            if (st.documentsPurged) {
              Surface(shape = CircleShape, color = Color(0xFF10B981).copy(alpha = 0.14f)) {
                Text("Captured selfie and ID photos deleted securely after processing.", color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
              }
            }
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          OutlinedButton(onClick = onRefresh, enabled = !state.isLoadingStatus, shape = CircleShape, modifier = Modifier.weight(1f).height(48.dp)) {
            Text(if (state.isLoadingStatus) "Refreshing..." else "Refresh Status")
          }
          Button(onClick = onBrowseTiers, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = DesktopCrimson, contentColor = Color.White), modifier = Modifier.weight(1f).height(48.dp)) {
            Text(if (isVerified || isUnderReview) "View Tiers" else "Continue")
          }
        }
      }
    }
  }
}
