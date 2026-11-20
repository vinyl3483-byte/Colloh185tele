package telefam.company.desktop

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import telefam.company.shared.TelefamCooldownSchedule
import java.awt.FileDialog
import java.awt.Frame
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import org.jetbrains.skia.Image as SkiaImage

internal val AuthCrimson = Color(0xFFC80A0F)
private val AuthCrimsonDeep = Color(0xFFA0070B)

@Composable
fun DesktopLogoBadge(
  size: Dp = 74.dp,
  animated: Boolean = true,
) {
  val floatY = if (animated) {
    val transition = rememberInfiniteTransition()
    val y by transition.animateFloat(
      initialValue = -3f,
      targetValue = 3f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 2200, easing = EaseInOutSine),
        repeatMode = RepeatMode.Reverse,
      ),
    )
    y
  } else {
    0f
  }

  Box(
    modifier = Modifier
      .size(size)
      .clip(RoundedCornerShape(22.dp))
      .background(Brush.linearGradient(listOf(Color(0xFFD61318), AuthCrimsonDeep))),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(
      modifier = Modifier
        .fillMaxSize(0.58f)
        .graphicsLayer { translationY = floatY },
    ) {
      val w = this.size.width
      val h = this.size.height
      val stroke = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)
      val plane = Path().apply {
        moveTo(w * 0.90f, h * 0.14f)
        lineTo(w * 0.08f, h * 0.48f)
        lineTo(w * 0.42f, h * 0.59f)
        lineTo(w * 0.54f, h * 0.90f)
        close()
      }
      drawPath(plane, color = Color.White, style = stroke)
      drawLine(
        color = Color.White,
        start = Offset(w * 0.90f, h * 0.14f),
        end = Offset(w * 0.42f, h * 0.59f),
        strokeWidth = w * 0.075f,
        cap = StrokeCap.Round,
      )
    }
  }
}

@Composable
fun DesktopBackgroundWaves(isDark: Boolean) {
  val blush = if (isDark) Color(0xFF3A1416) else Color(0xFFFCE8E8)
  Canvas(modifier = Modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    val topWave = Path().apply {
      moveTo(0f, 0f)
      lineTo(w, 0f)
      lineTo(w, h * 0.22f)
      cubicTo(w * 0.72f, h * 0.30f, w * 0.32f, h * 0.14f, 0f, h * 0.25f)
      close()
    }
    drawPath(topWave, color = blush.copy(alpha = if (isDark) 0.32f else 0.72f))

    val bottomWave = Path().apply {
      moveTo(0f, h * 0.84f)
      cubicTo(w * 0.28f, h * 0.76f, w * 0.72f, h * 0.90f, w, h * 0.81f)
      lineTo(w, h)
      lineTo(0f, h)
      close()
    }
    drawPath(bottomWave, color = blush.copy(alpha = if (isDark) 0.26f else 0.52f))
  }
}

@Composable
fun DesktopAuthScreen(
  apiClient: DesktopApiClient,
  isDark: Boolean,
  themeMode: DesktopThemeMode,
  onToggleTheme: () -> Unit,
  onAuthenticated: () -> Unit,
  onToast: (String) -> Unit,
) {
  val scope = rememberCoroutineScope()
  var email by remember { mutableStateOf(apiClient.session.email) }
  var otpCode by remember { mutableStateOf("") }
  var magicLinkToken by remember { mutableStateOf("") }
  var googleTokenInput by remember { mutableStateOf("") }
  var showGoogleSheet by remember { mutableStateOf(false) }
  var showMagicLinkInput by remember { mutableStateOf(false) }
  var showServerDialog by remember { mutableStateOf(false) }
  var serverUrlDraft by remember { mutableStateOf(apiClient.session.serverBaseUrl) }

  var isOtpStep by remember { mutableStateOf(false) }
  var approvalWaitState by remember { mutableStateOf<DesktopDeviceApprovalWaitState?>(null) }
  var tryAnotherWayRequestId by remember { mutableStateOf("") }
  var tryAnotherWayExpectedEmail by remember { mutableStateOf("") }
  var sendCount by remember { mutableStateOf(0) }
  var cooldownSec by remember { mutableStateOf(0L) }
  var isBusy by remember { mutableStateOf(false) }
  var errorText by remember { mutableStateOf<String?>(null) }
  var emailAccountNotice by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(email) {
    emailAccountNotice = null
    val trimmed = email.trim().lowercase()
    val atIdx = trimmed.indexOf('@')
    if (atIdx <= 0 || trimmed.indexOf('.', atIdx) <= atIdx + 1) return@LaunchedEffect
    if (!apiClient.isConfigured) return@LaunchedEffect
    delay(380L)
    val status = withContext(Dispatchers.IO) { apiClient.checkEmail(trimmed) }
    if (status != null && status.exists && email.trim().equals(trimmed, ignoreCase = true)) {
      emailAccountNotice = if (status.profileCompleted) {
        "Existing Telefam account — sign in to restore your profile"
      } else {
        "Account found — continue to complete your sign in"
      }
    }
  }

  LaunchedEffect(cooldownSec) {
    if (cooldownSec > 0L) {
      delay(1000L)
      cooldownSec--
    }
  }

  LaunchedEffect(approvalWaitState?.requestId, approvalWaitState?.isDeniedBlocked) {
    val active = approvalWaitState ?: return@LaunchedEffect
    if (active.isDeniedBlocked) return@LaunchedEffect
    var pollTick = 0
    while (approvalWaitState?.requestId == active.requestId && approvalWaitState?.isDeniedBlocked == false) {
      delay(1000L)
      val cur = approvalWaitState ?: break
      val nextRem = (cur.remainingSeconds - 1L).coerceAtLeast(0L)
      approvalWaitState = cur.copy(remainingSeconds = nextRem)
      pollTick++
      if (pollTick % 2 == 0 || nextRem == 0L) {
        val statusRes = withContext(Dispatchers.IO) {
          apiClient.pollLoginRequestStatus(cur.requestId, cur.pollToken)
        }
        if (statusRes is DesktopApiOutcome.Ok) {
          when (statusRes.data.status) {
            "approved" -> {
              if (statusRes.data.authenticatedSession != null) {
                approvalWaitState = null
                onAuthenticated()
                break
              }
            }
            "denied" -> {
              approvalWaitState = cur.copy(isDeniedBlocked = true, remainingSeconds = 0L)
              errorText = "Your trusted device denied access and blocked this device."
              break
            }
            "expired" -> {
              approvalWaitState = null
              errorText = "Approval request expired. Try again or use another method."
              break
            }
            else -> {
              approvalWaitState = cur.copy(
                remainingSeconds = statusRes.data.remainingSeconds.coerceAtLeast(0L),
                targetDeviceName = statusRes.data.targetDeviceName.ifBlank { cur.targetDeviceName },
                targetLocation = statusRes.data.targetLocation.ifBlank { cur.targetLocation },
              )
            }
          }
        }
      }
    }
  }

  val submitSendOtp: () -> Unit = {
    val clean = email.trim().lowercase()
    if (!clean.contains("@") || !clean.contains(".")) {
      errorText = "Enter a valid email address."
    } else if (!apiClient.isConfigured) {
      showServerDialog = true
    } else {
      isBusy = true
      errorText = null
      scope.launch {
        val res = withContext(Dispatchers.IO) { apiClient.requestOtp(clean) }
        isBusy = false
        when (res) {
          is DesktopApiOutcome.Ok -> {
            if (res.data.requiresDeviceApproval) {
              approvalWaitState = DesktopDeviceApprovalWaitState(
                email = clean,
                requestId = res.data.requestId,
                pollToken = res.data.pollToken,
                targetDeviceName = res.data.targetDeviceName.ifBlank { "Trusted Device" },
                targetLocation = res.data.targetLocation.ifBlank { "Approximate location" },
                requestTimeMs = res.data.requestTimeMs,
                remainingSeconds = res.data.expiresInSeconds.coerceAtLeast(1L),
                isDeniedBlocked = false,
              )
            } else {
              sendCount += 1
              cooldownSec = res.data.cooldownSeconds.coerceAtLeast(TelefamCooldownSchedule.secondsForSendCount(sendCount))
              isOtpStep = true
            }
          }
          is DesktopApiOutcome.Fail -> {
            if (res.statusCode == 403) {
              approvalWaitState = DesktopDeviceApprovalWaitState(
                email = clean,
                requestId = "",
                pollToken = "",
                targetDeviceName = "Trusted Device",
                targetLocation = "Blocked for security",
                requestTimeMs = System.currentTimeMillis(),
                remainingSeconds = 0L,
                isDeniedBlocked = true,
              )
              errorText = res.message
            } else {
              errorText = res.message
            }
          }
        }
      }
    }
  }

  val submitVerifyOtp: () -> Unit = {
    val digits = otpCode.filter { it.isDigit() }
    if (digits.length != 6) {
      errorText = "Enter the 6-digit verification code."
    } else {
      isBusy = true
      errorText = null
      scope.launch {
        val res = withContext(Dispatchers.IO) { apiClient.verifyOtp(email.trim().lowercase(), digits) }
        isBusy = false
        when (res) {
          is DesktopApiOutcome.Ok -> onAuthenticated()
          is DesktopApiOutcome.Fail -> errorText = res.message
        }
      }
    }
  }

  val activeWait = approvalWaitState
  if (activeWait != null) {
    Box(modifier = Modifier.fillMaxSize()) {
      DesktopDeviceApprovalWaitScreen(
        waitState = activeWait,
        isBusy = isBusy,
        errorText = errorText,
        onBackChangeEmail = {
          approvalWaitState = null
          tryAnotherWayRequestId = ""
          tryAnotherWayExpectedEmail = ""
          errorText = null
        },
        onTryAnotherWayGoogle = {
          tryAnotherWayExpectedEmail = activeWait.email
          tryAnotherWayRequestId = activeWait.requestId
          showGoogleSheet = true
        },
      )

      if (showGoogleSheet) {
        DesktopModalOverlay(onDismiss = { showGoogleSheet = false }) {
          Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Verify via Google (${activeWait.email})", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(
              "Sign in with the Google account matching ${activeWait.email} and paste the ID token below.",
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DesktopCleanTextField(
              value = googleTokenInput,
              onValueChange = { googleTokenInput = it },
              placeholder = "Google ID Token (JWT)",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
              OutlinedButton(onClick = { showGoogleSheet = false }, modifier = Modifier.weight(1f)) {
                Text("Cancel")
              }
              Button(
                onClick = {
                  if (googleTokenInput.isBlank()) return@Button
                  isBusy = true
                  scope.launch {
                    val res = withContext(Dispatchers.IO) {
                      apiClient.verifyGoogleOAuth(
                        identityToken = googleTokenInput,
                        expectedEmail = tryAnotherWayExpectedEmail,
                        approvalRequestId = tryAnotherWayRequestId,
                      )
                    }
                    isBusy = false
                    when (res) {
                      is DesktopApiOutcome.Ok -> {
                        showGoogleSheet = false
                        approvalWaitState = null
                        onAuthenticated()
                      }
                      is DesktopApiOutcome.Fail -> onToast(res.message)
                    }
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AuthCrimson),
                modifier = Modifier.weight(1f),
              ) {
                Text("Verify", color = Color.White)
              }
            }
          }
        }
      }
    }
    return
  }

  Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    DesktopBackgroundWaves(isDark = isDark)

    Row(
      modifier = Modifier.align(Alignment.TopEnd).padding(20.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Surface(
        onClick = { showServerDialog = true },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Text(
          text = if (apiClient.isConfigured) "Server Connected" else "Configure Server",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = if (apiClient.isConfigured) Color(0xFF16A34A) else AuthCrimson,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
      }
      Surface(
        onClick = onToggleTheme,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Text(
          text = themeMode.name.lowercase().replaceFirstChar { it.uppercase() },
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
      }
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      DesktopLogoBadge(size = 76.dp, animated = true)
      Spacer(modifier = Modifier.height(14.dp))
      Text("Telefam", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = AuthCrimson)
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        "Family Moments & Private Messaging",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(24.dp))

      Surface(
        modifier = Modifier.fillMaxWidth().widthIn(max = 430.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 12.dp,
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          if (!isOtpStep) {
            Text("Sign in", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
              "Sign in with your email address or Google account",
              fontSize = 13.5.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )

            DesktopCleanTextField(
              value = email,
              onValueChange = { email = it.take(120); errorText = null; emailAccountNotice = null },
              placeholder = "you@example.com",
            )

            if (emailAccountNotice != null) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = AuthCrimson.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, AuthCrimson.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(
                  text = emailAccountNotice.orEmpty(),
                  color = AuthCrimson,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
              }
            }

            if (errorText != null) {
              Text(errorText.orEmpty(), color = AuthCrimson, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
            }

            Button(
              onClick = submitSendOtp,
              enabled = !isBusy,
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = AuthCrimson),
              modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
              if (isBusy) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
              } else {
                Text("Continue with email", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
              HorizontalDivider(modifier = Modifier.weight(1f))
              Text("or", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp))
              HorizontalDivider(modifier = Modifier.weight(1f))
            }

            OutlinedButton(
              onClick = { showGoogleSheet = true },
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
              Text("Continue with Google", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            }

            TextButton(onClick = { showMagicLinkInput = !showMagicLinkInput }) {
              Text("Have a verification link token?", color = AuthCrimson, fontSize = 13.sp)
            }

            if (showMagicLinkInput) {
              DesktopCleanTextField(
                value = magicLinkToken,
                onValueChange = { magicLinkToken = it },
                placeholder = "Paste verification token",
              )
              Button(
                onClick = {
                  if (magicLinkToken.isBlank()) return@Button
                  isBusy = true
                  scope.launch {
                    val res = withContext(Dispatchers.IO) { apiClient.verifyLinkToken(magicLinkToken) }
                    isBusy = false
                    when (res) {
                      is DesktopApiOutcome.Ok -> onAuthenticated()
                      is DesktopApiOutcome.Fail -> errorText = res.message
                    }
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AuthCrimson),
                modifier = Modifier.fillMaxWidth().height(44.dp),
              ) {
                Text("Verify link token", color = Color.White, fontWeight = FontWeight.SemiBold)
              }
            }
          } else {
            Text("Verify your email", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Surface(
              shape = RoundedCornerShape(18.dp),
              color = if (isDark) Color(0xFF3A1416) else Color(0xFFFDECEC),
            ) {
              Text(
                text = email.trim(),
                color = AuthCrimson,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
              )
            }

            // 6-box OTP entry matching mobile
            DesktopCleanTextField(
              value = otpCode,
              onValueChange = { raw ->
                otpCode = raw.filter { it.isDigit() }.take(6)
                errorText = null
              },
              placeholder = "Enter 6-digit code",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              for (i in 0 until 6) {
                val ch = otpCode.getOrNull(i)?.toString().orEmpty()
                val active = otpCode.length == i
                Box(
                  modifier = Modifier
                    .size(width = 46.dp, height = 52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(
                      width = if (active || ch.isNotEmpty()) 1.6.dp else 1.dp,
                      color = if (active || ch.isNotEmpty()) AuthCrimson else MaterialTheme.colorScheme.outlineVariant,
                      shape = RoundedCornerShape(12.dp),
                    ),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(ch, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
              }
            }

            if (errorText != null) {
              Text(errorText.orEmpty(), color = AuthCrimson, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
            }

            Button(
              onClick = submitVerifyOtp,
              enabled = !isBusy && otpCode.length == 6,
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = AuthCrimson),
              modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
              if (isBusy) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
              } else {
                Text("Verify", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
              }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              TextButton(onClick = { isOtpStep = false; otpCode = ""; errorText = null }) {
                Text("Change email", color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              TextButton(
                enabled = cooldownSec == 0L && !isBusy,
                onClick = submitSendOtp,
              ) {
                Text(
                  text = if (cooldownSec > 0L) "Resend in ${formatCooldown(cooldownSec)}" else "Resend code",
                  color = if (cooldownSec == 0L) AuthCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }
          }
        }
      }
    }

    if (showGoogleSheet) {
      DesktopModalOverlay(onDismiss = { showGoogleSheet = false }) {
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Continue with Google", fontSize = 18.sp, fontWeight = FontWeight.Bold)
          Text(
            "Paste the Google ID token returned from your OAuth consent flow.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          DesktopCleanTextField(
            value = googleTokenInput,
            onValueChange = { googleTokenInput = it },
            placeholder = "Google ID Token (JWT)",
          )
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { showGoogleSheet = false }, modifier = Modifier.weight(1f)) {
              Text("Cancel")
            }
            Button(
              onClick = {
                if (googleTokenInput.isBlank()) return@Button
                isBusy = true
                scope.launch {
                  val res = withContext(Dispatchers.IO) {
                    apiClient.verifyGoogleOAuth(
                      identityToken = googleTokenInput,
                      expectedEmail = tryAnotherWayExpectedEmail,
                      approvalRequestId = tryAnotherWayRequestId,
                    )
                  }
                  isBusy = false
                  when (res) {
                    is DesktopApiOutcome.Ok -> {
                      showGoogleSheet = false
                      onAuthenticated()
                    }
                    is DesktopApiOutcome.Fail -> {
                      onToast(res.message)
                    }
                  }
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = AuthCrimson),
              modifier = Modifier.weight(1f),
            ) {
              Text("Sign in", color = Color.White)
            }
          }
        }
      }
    }

    if (showServerDialog) {
      DesktopModalOverlay(onDismiss = { showServerDialog = false }) {
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Backend Endpoint", fontSize = 18.sp, fontWeight = FontWeight.Bold)
          Text(
            "Enter HTTPS server base URL (or http://127.0.0.1:8080 for local testing).",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          DesktopCleanTextField(
            value = serverUrlDraft,
            onValueChange = { serverUrlDraft = it },
            placeholder = "https://api.example.com",
          )
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { showServerDialog = false }, modifier = Modifier.weight(1f)) {
              Text("Cancel")
            }
            Button(
              onClick = {
                if (!DesktopSecurityGuard.isAllowedServerUrl(serverUrlDraft)) {
                  onToast("HTTPS required for remote endpoints")
                } else {
                  apiClient.updateServerBaseUrl(serverUrlDraft)
                  showServerDialog = false
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = AuthCrimson),
              modifier = Modifier.weight(1f),
            ) {
              Text("Save", color = Color.White)
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesktopProfileSetupScreen(
  apiClient: DesktopApiClient,
  isDark: Boolean,
  onCompleted: () -> Unit,
  onSignOut: () -> Unit,
  onToast: (String) -> Unit,
) {
  val scope = rememberCoroutineScope()
  var fullName by remember { mutableStateOf(apiClient.session.suggestedFullName) }
  var username by remember { mutableStateOf(apiClient.session.suggestedUsername) }
  var selectedCountry by remember { mutableStateOf(DesktopCountryCatalog.defaultCountry) }
  var phoneDigits by remember { mutableStateOf("") }
  var gender by remember { mutableStateOf("Prefer not to say") }
  var dob by remember { mutableStateOf("") }
  var location by remember { mutableStateOf("") }
  var bio by remember { mutableStateOf("") }
  var usernameAvailable by remember { mutableStateOf<Boolean?>(null) }

  var pendingRawAvatarFile by remember { mutableStateOf<File?>(null) }
  var uploadedAvatarMediaId by remember { mutableStateOf<String?>(null) }
  var avatarPreviewBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
  var showCountryPicker by remember { mutableStateOf(false) }
  var countrySearch by remember { mutableStateOf("") }
  var isSaving by remember { mutableStateOf(false) }

  LaunchedEffect(username) {
    val clean = username.trim().lowercase()
    if (clean.length < 3) {
      usernameAvailable = null
      return@LaunchedEffect
    }
    delay(350L)
    usernameAvailable = withContext(Dispatchers.IO) { apiClient.checkUsernameAvailable(clean) }
  }

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Row(
      modifier = Modifier.fillMaxWidth().background(AuthCrimson).padding(horizontal = 24.dp, vertical = 18.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text("Complete your profile", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Set up how family and friends find you", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
      }
      TextButton(onClick = onSignOut) {
        Text("Sign out", color = Color.White)
      }
    }

    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 620.dp)
          .verticalScroll(rememberScrollState())
          .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Avatar selector + crop preview
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          Box(
            modifier = Modifier
              .size(84.dp)
              .clip(CircleShape)
              .background(if (isDark) Color(0xFF3A1416) else Color(0xFFFDE8E8))
              .border(2.dp, AuthCrimson, CircleShape)
              .clickable {
                val dlg = FileDialog(null as Frame?, "Select profile photo", FileDialog.LOAD).apply {
                  setFilenameFilter { _, name ->
                    val low = name.lowercase()
                    low.endsWith(".jpg") || low.endsWith(".jpeg") || low.endsWith(".png")
                  }
                  isVisible = true
                }
                dlg.files?.firstOrNull()?.let { pendingRawAvatarFile = it }
              },
            contentAlignment = Alignment.Center,
          ) {
            val bmp = avatarPreviewBitmap
            if (bmp != null) {
              Image(bmp, contentDescription = "Avatar", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
              Text(
                text = fullName.trim().take(2).uppercase().ifBlank { "TF" },
                color = AuthCrimson,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
              )
            }
          }
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Profile photo", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
              text = if (uploadedAvatarMediaId != null) "Uploaded" else "Choose photo & circular crop",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 13.sp,
            )
          }
        }

        DesktopLabeledField("Full name", fullName, { fullName = it.take(64) }, "Full name")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          DesktopLabeledField("Username", username, { username = it.lowercase().take(32) }, "username")
          usernameAvailable?.let { avail ->
            Text(
              text = if (avail) "Available" else "Username already taken",
              color = if (avail) Color(0xFF16A34A) else AuthCrimson,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
            )
          }
        }

        // Phone + searchable Country Dial code
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Phone number", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
              onClick = { showCountryPicker = true },
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              modifier = Modifier.height(44.dp),
            ) {
              Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                Text("${selectedCountry.flagEmoji} ${selectedCountry.dialCode}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
              }
            }
            Box(modifier = Modifier.weight(1f)) {
              DesktopCleanTextField(
                value = phoneDigits,
                onValueChange = { phoneDigits = it.filter { c -> c.isDigit() }.take(15) },
                placeholder = "Phone number",
              )
            }
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Gender", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Female", "Male", "Non-binary", "Prefer not to say").forEach { option ->
              val sel = gender == option
              Surface(
                onClick = { gender = option },
                shape = RoundedCornerShape(18.dp),
                color = if (sel) AuthCrimson else MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, if (sel) AuthCrimson else MaterialTheme.colorScheme.outlineVariant),
              ) {
                Text(
                  text = option,
                  color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Medium,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                )
              }
            }
          }
        }

        DesktopLabeledField("Date of birth (YYYY-MM-DD)", dob, { dob = it.take(10) }, "2000-05-15")
        DesktopLabeledField("City / Country", location, { location = it.take(80) }, "Nairobi, Kenya")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          DesktopLabeledField("Bio", bio, { bio = it.take(150) }, "About you...")
          Text("${bio.length}/150", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.End))
        }

        Button(
          onClick = {
            if (fullName.isBlank() || username.isBlank() || phoneDigits.length < 5 || location.isBlank()) {
              onToast("Complete all required fields.")
              return@Button
            }
            if (!DesktopCountryCatalog.isAtLeast13(dob)) {
              onToast("You must be at least 13 years old.")
              return@Button
            }
            isSaving = true
            scope.launch {
              val res = withContext(Dispatchers.IO) {
                apiClient.setupProfile(
                  fullName = fullName.trim(),
                  username = username.trim().lowercase(),
                  phoneNumber = "${selectedCountry.dialCode}$phoneDigits",
                  countryIso = selectedCountry.iso,
                  gender = gender,
                  dateOfBirth = dob.trim(),
                  location = location.trim(),
                  bio = bio.trim(),
                  avatarMediaId = uploadedAvatarMediaId,
                )
              }
              isSaving = false
              when (res) {
                is DesktopApiOutcome.Ok -> onCompleted()
                is DesktopApiOutcome.Fail -> onToast(res.message)
              }
            }
          },
          enabled = !isSaving,
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(containerColor = AuthCrimson),
          modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
          if (isSaving) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
          } else {
            Text("Complete setup", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
          }
        }
      }
    }

    // Circular drag & zoom cropper dialog matching mobile
    pendingRawAvatarFile?.let { file ->
      DesktopAvatarCropDialog(
        imageFile = file,
        onCancel = { pendingRawAvatarFile = null },
        onConfirm = { croppedBytes, previewBmp, cx, cy, scale ->
          pendingRawAvatarFile = null
          avatarPreviewBitmap = previewBmp
          scope.launch {
            val res = withContext(Dispatchers.IO) { apiClient.uploadAvatar(croppedBytes, cx, cy, scale) }
            when (res) {
              is DesktopApiOutcome.Ok -> uploadedAvatarMediaId = res.data.mediaId
              is DesktopApiOutcome.Fail -> onToast(res.message)
            }
          }
        },
      )
    }

    if (showCountryPicker) {
      DesktopModalOverlay(onDismiss = { showCountryPicker = false }) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Select country", fontSize = 17.sp, fontWeight = FontWeight.Bold)
          DesktopCleanTextField(countrySearch, { countrySearch = it }, "Search country or dial code")
          val q = countrySearch.trim().lowercase()
          val list = DesktopCountryCatalog.allCountries.filter {
            q.isEmpty() || it.name.lowercase().contains(q) || it.dialCode.contains(q) || it.iso.lowercase().contains(q)
          }
          LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
            items(list, key = { it.iso }) { opt ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable {
                    selectedCountry = opt
                    showCountryPicker = false
                  }
                  .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text("${opt.flagEmoji}  ${opt.name}", fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(opt.dialCode, color = AuthCrimson, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
internal fun DesktopAvatarCropDialog(
  imageFile: File,
  onCancel: () -> Unit,
  onConfirm: (ByteArray, ImageBitmap, Float, Float, Float) -> Unit,
) {
  val sourceImg = remember(imageFile) { runCatching { ImageIO.read(imageFile) }.getOrNull() }
  val composeBmp = remember(imageFile) {
    runCatching { SkiaImage.makeFromEncoded(imageFile.readBytes()).toComposeImageBitmap() }.getOrNull()
  }
  var offsetX by remember { mutableStateOf(0f) }
  var offsetY by remember { mutableStateOf(0f) }
  var zoom by remember { mutableStateOf(1f) }

  DesktopModalOverlay(onDismiss = onCancel) {
    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text("Crop avatar", fontWeight = FontWeight.Bold, fontSize = 18.sp)
      Box(
        modifier = Modifier
          .size(220.dp)
          .clip(CircleShape)
          .background(Color(0xFF141820))
          .border(2.dp, AuthCrimson, CircleShape)
          .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
              change.consume()
              offsetX = (offsetX + dragAmount.x).coerceIn(-120f, 120f)
              offsetY = (offsetY + dragAmount.y).coerceIn(-120f, 120f)
            }
          },
        contentAlignment = Alignment.Center,
      ) {
        if (composeBmp != null) {
          Image(
            bitmap = composeBmp,
            contentDescription = "Avatar crop preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer {
              translationX = offsetX
              translationY = offsetY
              scaleX = zoom
              scaleY = zoom
            },
          )
        }
      }
      Slider(
        value = zoom,
        onValueChange = { zoom = it },
        valueRange = 1f..2.6f,
        colors = SliderDefaults.colors(thumbColor = AuthCrimson, activeTrackColor = AuthCrimson),
      )
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
          Text("Cancel")
        }
        Button(
          onClick = {
            if (sourceImg != null && composeBmp != null) {
              val scaled = BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB)
              val g = scaled.createGraphics()
              g.drawImage(sourceImg, 0, 0, 400, 400, null)
              g.dispose()
              val baos = ByteArrayOutputStream()
              ImageIO.write(scaled, "jpg", baos)
              val cx = (0.5f - offsetX / 440f).coerceIn(0f, 1f)
              val cy = (0.5f - offsetY / 440f).coerceIn(0f, 1f)
              onConfirm(baos.toByteArray(), composeBmp, cx, cy, zoom)
            } else {
              onCancel()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = AuthCrimson),
          modifier = Modifier.weight(1f),
        ) {
          Text("Use photo", color = Color.White)
        }
      }
    }
  }
}

@Composable
internal fun DesktopLabeledField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
) {
  Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
    Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    DesktopCleanTextField(value = value, onValueChange = onValueChange, placeholder = placeholder)
  }
}

@Composable
internal fun DesktopCleanTextField(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.background,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth().height(44.dp),
  ) {
    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
      if (value.isEmpty()) {
        Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
      }
      BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
        cursorBrush = SolidColor(AuthCrimson),
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
internal fun DesktopModalOverlay(
  onDismiss: () -> Unit,
  content: @Composable () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.55f))
      .clickable(onClick = onDismiss),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier
        .widthIn(min = 360.dp, max = 440.dp)
        .clickable(enabled = false) {},
    ) {
      content()
    }
  }
}

private fun formatCooldown(sec: Long): String {
  val m = sec / 60L
  val s = sec % 60L
  return if (m > 0L) "%d:%02d".format(m, s) else "${s}s"
}
