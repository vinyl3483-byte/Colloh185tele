package telefam.company.desktop

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Date
import java.util.Locale

data class DesktopDeviceContextSnapshot(
  val deviceName: String,
  val platform: String,
  val osVersion: String,
  val localeCountryIso: String,
  val timezoneId: String,
  val approximateLocation: String,
  val capturedAtMs: Long,
)

object DesktopDeviceContextCollector {
  @Volatile
  private var cachedHostName: String? = null

  fun formatRealDeviceName(): String {
    val host = cachedHostName ?: resolveHostName().also { cachedHostName = it }
    val osName = System.getProperty("os.name", "Desktop").trim()
    val label = when {
      osName.contains("Mac", ignoreCase = true) -> "Mac"
      osName.contains("Win", ignoreCase = true) -> "Windows PC"
      osName.contains("Linux", ignoreCase = true) -> "Linux Workstation"
      else -> osName
    }
    return if (host.isNotBlank() && !host.equals("localhost", ignoreCase = true)) {
      "$label ($host)".take(64)
    } else {
      label.take(64)
    }
  }

  private fun resolveHostName(): String {
    val envHost = System.getenv("COMPUTERNAME")?.takeIf { it.isNotBlank() }
      ?: System.getenv("HOSTNAME")?.takeIf { it.isNotBlank() }
    if (!envHost.isNullOrBlank()) return envHost.trim().substringBefore('.')
    return runCatching { InetAddress.getLocalHost().hostName.trim().substringBefore('.') }.getOrDefault("")
  }

  fun collectSnapshot(): DesktopDeviceContextSnapshot {
    val locale = Locale.getDefault()
    val countryIso = locale.country.uppercase(Locale.US).takeIf { it.length == 2 }.orEmpty()
    val tz = runCatching { ZoneId.systemDefault().id }.getOrDefault("")
    val cityHint = tz.substringAfterLast('/', "").replace('_', ' ').trim()
    val countryName = if (countryIso.isNotBlank()) {
      Locale("", countryIso).displayCountry
    } else ""
    val approx = listOf(cityHint, countryName).filter { it.isNotBlank() }.distinct().joinToString(", ")
    return DesktopDeviceContextSnapshot(
      deviceName = formatRealDeviceName(),
      platform = "desktop",
      osVersion = "${System.getProperty("os.name", "Desktop")} ${System.getProperty("os.version", "")}".trim(),
      localeCountryIso = countryIso,
      timezoneId = tz,
      approximateLocation = approx,
      capturedAtMs = System.currentTimeMillis(),
    )
  }

  fun toHeaders(snap: DesktopDeviceContextSnapshot = collectSnapshot()): Map<String, String> = mapOf(
    "X-Telefam-Device-Name" to snap.deviceName,
    "X-Telefam-Device-Platform" to snap.platform,
    "X-Telefam-OS-Version" to snap.osVersion,
    "X-Telefam-Locale-Country" to snap.localeCountryIso,
    "X-Telefam-Timezone" to snap.timezoneId,
  )
}

data class DesktopDeviceApprovalWaitState(
  val email: String,
  val requestId: String,
  val pollToken: String,
  val targetDeviceName: String,
  val targetLocation: String,
  val requestTimeMs: Long,
  val remainingSeconds: Long,
  val isDeniedBlocked: Boolean = false,
)

data class DesktopIncomingLoginRequestItem(
  val requestId: String,
  val deviceName: String,
  val location: String,
  val ipAddressMasked: String,
  val requestedAtMs: Long,
  val remainingSeconds: Long,
)

data class DesktopLoginApprovedState(
  val deviceName: String,
  val location: String,
  val requestedAtMs: Long,
)

private val DeskSecBg = Color(0xFF070405)
private val DeskSecCardBg = Color(0xFF130B0D)
private val DeskSecBorder = Color(0x28FFFFFF)
private val DeskCrimsonGlow = Color(0xFFEF233C)
private val DeskCrimsonRich = Color(0xFFD91E2A)
private val DeskCrimsonDeep = Color(0xFF7D0914)
private val DeskSoftWhite = Color(0xFFF9FAFB)
private val DeskMuted = Color(0xFF9CA3AF)
private val DeskGreenSuccess = Color(0xFF22C55E)

@Composable
fun DesktopDeviceApprovalWaitScreen(
  waitState: DesktopDeviceApprovalWaitState,
  isBusy: Boolean,
  errorText: String?,
  onBackChangeEmail: () -> Unit,
  onTryAnotherWayGoogle: () -> Unit,
) {
  var menuExpanded by remember { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxSize().background(DeskSecBg)) {
    DesktopSecurityBackdrop(showWorldDots = false)

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            onClick = onBackChangeEmail,
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier.size(48.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              SecurityVectorGlyph(SecurityGlyphKind.BACK_CHEVRON, DeskSoftWhite, 22.dp)
            }
          }

          DesktopRoundedSecurityLogo(size = 56.dp)

          Box {
            Surface(
              onClick = { menuExpanded = true },
              shape = CircleShape,
              color = Color.Transparent,
              modifier = Modifier.size(48.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                SecurityVectorGlyph(SecurityGlyphKind.MORE_DOTS, DeskSoftWhite, 20.dp)
              }
            }
            DropdownMenu(
              expanded = menuExpanded,
              onDismissRequest = { menuExpanded = false },
            ) {
              DropdownMenuItem(
                text = { Text("Verify with Google") },
                onClick = {
                  menuExpanded = false
                  onTryAnotherWayGoogle()
                },
              )
              DropdownMenuItem(
                text = { Text("Use another email") },
                onClick = {
                  menuExpanded = false
                  onBackChangeEmail()
                },
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Telefam", color = DeskSoftWhite, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))

        DesktopPhoneToClockIllustration(
          isDenied = waitState.isDeniedBlocked,
          modifier = Modifier.fillMaxWidth().height(140.dp),
        )

        Spacer(modifier = Modifier.height(18.dp))
        Text(
          text = if (waitState.isDeniedBlocked) "Login request denied" else "We sent a request to",
          color = DeskSoftWhite,
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = waitState.targetDeviceName.ifBlank { "Your trusted device" },
          color = DeskCrimsonGlow,
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = buildAnnotatedString {
            if (waitState.isDeniedBlocked) {
              append("Your trusted device denied access and blocked this sign-in attempt. Confirm ownership via Google to continue.")
            } else {
              append("Please click ")
              withStyle(SpanStyle(color = DeskSoftWhite, fontWeight = FontWeight.Bold)) { append("Accept") }
              append(" in another device to confirm it's you.")
            }
          },
          color = DeskMuted,
          fontSize = 14.5.sp,
          lineHeight = 21.sp,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(22.dp))

        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          color = DeskSecCardBg.copy(alpha = 0.90f),
          border = BorderStroke(1.dp, DeskSecBorder),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            DesktopApprovalDetailRow(
              kind = SecurityGlyphKind.PHONE,
              label = "Device",
              value = waitState.targetDeviceName.ifBlank { "Trusted Device" },
              redBadge = false,
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            DesktopApprovalDetailRow(
              kind = SecurityGlyphKind.PIN,
              label = "Location",
              value = waitState.targetLocation.ifBlank { "Detected automatically" },
              redBadge = false,
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            DesktopApprovalDetailRow(
              kind = SecurityGlyphKind.CLOCK,
              label = "Time",
              value = buildString {
                append(formatDesktopRequestTime(waitState.requestTimeMs))
                if (!waitState.isDeniedBlocked && waitState.remainingSeconds > 0L) {
                  append("  •  ")
                  append(formatDesktopCountdown(waitState.remainingSeconds))
                }
              },
              redBadge = false,
            )
          }
        }

        if (!errorText.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(12.dp))
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF2B0B0F),
            border = BorderStroke(1.dp, DeskCrimsonGlow.copy(alpha = 0.55f)),
          ) {
            Text(
              text = errorText,
              color = Color(0xFFFEE2E2),
              fontSize = 13.sp,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(22.dp))

        OutlinedButton(
          onClick = onTryAnotherWayGoogle,
          enabled = !isBusy,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.5.dp, DeskCrimsonRich),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF170709),
            contentColor = DeskSoftWhite,
          ),
          modifier = Modifier.fillMaxWidth().height(54.dp),
          contentPadding = PaddingValues(horizontal = 18.dp),
        ) {
          if (isBusy) {
            CircularProgressIndicator(color = DeskCrimsonGlow, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
          } else {
            Box(
              modifier = Modifier
                .size(26.dp)
                .border(1.5.dp, DeskCrimsonGlow, CircleShape)
                .background(DeskCrimsonDeep.copy(alpha = 0.35f), CircleShape),
              contentAlignment = Alignment.Center,
            ) {
              Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(3) {
                  Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Color(0xFFFCA5A5)))
                }
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("Try Another Way", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = DeskSoftWhite)
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
          SecurityVectorGlyph(SecurityGlyphKind.SHIELD, DeskMuted, 17.dp)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "If you didn't request this, your account may be at risk.",
            color = DeskMuted,
            fontSize = 12.5.sp,
          )
        }
      }
    }
  }
}

@Composable
fun DesktopIncomingLoginRequestOverlay(
  request: DesktopIncomingLoginRequestItem,
  isResponding: Boolean,
  actionError: String?,
  onDeny: () -> Unit,
  onAccept: () -> Unit,
) {
  Box(modifier = Modifier.fillMaxSize().background(DeskSecBg)) {
    DesktopSecurityBackdrop(showWorldDots = true)

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 22.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        DesktopRoundedSecurityLogo(size = 60.dp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Telefam", color = DeskSoftWhite, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(18.dp))

        DesktopShieldLockEmblem(size = 72.dp)
        Spacer(modifier = Modifier.height(14.dp))

        Text("New login request", color = DeskSoftWhite, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = buildAnnotatedString {
            append("Someone is trying to log in to your\n")
            withStyle(SpanStyle(color = DeskCrimsonGlow, fontWeight = FontWeight.SemiBold)) { append("Telefam") }
            append(" account on another device.")
          },
          color = DeskMuted,
          fontSize = 14.5.sp,
          lineHeight = 20.sp,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(18.dp))

        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          color = DeskSecCardBg.copy(alpha = 0.90f),
          border = BorderStroke(1.dp, DeskSecBorder),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            DesktopApprovalDetailRow(
              kind = SecurityGlyphKind.PHONE,
              label = "Device",
              value = request.deviceName.ifBlank { "Unknown Device" },
              redBadge = true,
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            DesktopApprovalDetailRow(
              kind = SecurityGlyphKind.PIN,
              label = "Location",
              value = "${request.location.ifBlank { "Approximate location unavailable" }} (Approximate)",
              redBadge = true,
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            DesktopApprovalDetailRow(
              kind = SecurityGlyphKind.CLOCK,
              label = "Time",
              value = formatDesktopRequestTime(request.requestedAtMs),
              redBadge = true,
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            DesktopApprovalDetailRow(
              kind = SecurityGlyphKind.GLOBE,
              label = "IP Address",
              value = request.ipAddressMasked.ifBlank { "Hidden" },
              redBadge = true,
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF1A090B),
          border = BorderStroke(1.dp, DeskCrimsonGlow.copy(alpha = 0.40f)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(DeskCrimsonRich.copy(alpha = 0.25f))
                .border(1.dp, DeskCrimsonGlow.copy(alpha = 0.65f), CircleShape),
              contentAlignment = Alignment.Center,
            ) {
              SecurityVectorGlyph(SecurityGlyphKind.SHIELD, DeskCrimsonGlow, 17.dp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = buildAnnotatedString {
                withStyle(SpanStyle(color = DeskSoftWhite, fontWeight = FontWeight.SemiBold)) {
                  append("If this wasn't you,")
                }
                append(" you can deny this request and secure your account.")
              },
              color = Color(0xFFE5E7EB),
              fontSize = 13.sp,
              lineHeight = 18.sp,
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          color = DeskSecCardBg.copy(alpha = 0.90f),
          border = BorderStroke(1.dp, DeskSecBorder),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              SecurityVectorGlyph(SecurityGlyphKind.CLOCK, DeskCrimsonGlow, 15.dp)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Request expires in", color = DeskMuted, fontSize = 12.5.sp)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = formatDesktopCountdown(request.remainingSeconds),
              color = DeskSoftWhite,
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp,
            )
          }
        }

        if (!actionError.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(actionError, color = Color(0xFFFCA5A5), fontSize = 12.5.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          OutlinedButton(
            onClick = onDeny,
            enabled = !isResponding,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, DeskCrimsonGlow.copy(alpha = 0.75f)),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = Color(0xFF170709),
              contentColor = DeskCrimsonGlow,
            ),
            modifier = Modifier.weight(1f).height(52.dp),
          ) {
            SecurityVectorGlyph(SecurityGlyphKind.CLOSE_CIRCLE, DeskCrimsonGlow, 18.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Deny", color = Color(0xFFF87171), fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
          }

          Button(
            onClick = onAccept,
            enabled = !isResponding,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DeskCrimsonRich, contentColor = DeskSoftWhite),
            modifier = Modifier.weight(1f).height(52.dp),
          ) {
            if (isResponding) {
              CircularProgressIndicator(color = DeskSoftWhite, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
              SecurityVectorGlyph(SecurityGlyphKind.CHECK_CIRCLE, DeskSoftWhite, 18.dp)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Accept", color = DeskSoftWhite, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          SecurityVectorGlyph(SecurityGlyphKind.LOCK, DeskMuted, 14.dp)
          Spacer(modifier = Modifier.width(6.dp))
          Text("We'll never share your information", color = DeskMuted, fontSize = 12.sp)
        }
      }
    }
  }
}

@Composable
fun DesktopLoginApprovedOverlay(
  approved: DesktopLoginApprovedState,
  onDone: () -> Unit,
) {
  Box(modifier = Modifier.fillMaxSize().background(DeskSecBg)) {
    DesktopSecurityBackdrop(showWorldDots = false)

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        DesktopRoundedSecurityLogo(size = 64.dp)
        Spacer(modifier = Modifier.height(10.dp))
        Text("Telefam", color = DeskSoftWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        DesktopApprovedCheckEmblem(size = 120.dp)
        Spacer(modifier = Modifier.height(22.dp))

        Text("Login Approved!", color = DeskSoftWhite, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "You have successfully approved the login\non another device.",
          color = DeskMuted,
          fontSize = 14.5.sp,
          lineHeight = 20.sp,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(22.dp))

        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          color = DeskSecCardBg.copy(alpha = 0.90f),
          border = BorderStroke(1.dp, DeskSecBorder),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            DesktopApprovalDetailRow(
              kind = SecurityGlyphKind.PHONE,
              label = "Device",
              value = approved.deviceName.ifBlank { "Approved Device" },
              redBadge = false,
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            DesktopApprovalDetailRow(
              kind = SecurityGlyphKind.PIN,
              label = "Location",
              value = approved.location.ifBlank { "Approximate location" },
              redBadge = false,
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            DesktopApprovalDetailRow(
              kind = SecurityGlyphKind.CLOCK,
              label = "Time",
              value = formatDesktopRequestTime(approved.requestedAtMs),
              redBadge = false,
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF0B2212),
          border = BorderStroke(1.dp, DeskGreenSuccess.copy(alpha = 0.45f)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            SecurityVectorGlyph(SecurityGlyphKind.SHIELD, DeskGreenSuccess, 26.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text("All set!", color = DeskSoftWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "The new device will now be able to access your Telefam account.",
                color = Color(0xFFD1D5DB),
                fontSize = 13.sp,
                lineHeight = 18.sp,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
          onClick = onDone,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = DeskCrimsonRich, contentColor = DeskSoftWhite),
          modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
          Text("Done", color = DeskSoftWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

private enum class SecurityGlyphKind {
  BACK_CHEVRON,
  MORE_DOTS,
  PHONE,
  PIN,
  CLOCK,
  GLOBE,
  SHIELD,
  LOCK,
  CHECK_CIRCLE,
  CLOSE_CIRCLE,
}

@Composable
private fun DesktopApprovalDetailRow(
  kind: SecurityGlyphKind,
  label: String,
  value: String,
  redBadge: Boolean,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (redBadge) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(DeskCrimsonDeep.copy(alpha = 0.45f))
          .border(1.dp, DeskCrimsonGlow.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        SecurityVectorGlyph(kind, Color(0xFFFCA5A5), 18.dp)
      }
    } else {
      SecurityVectorGlyph(kind, DeskSoftWhite.copy(alpha = 0.85f), 20.dp)
    }
    Spacer(modifier = Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(label, color = DeskMuted, fontSize = 12.sp)
      Spacer(modifier = Modifier.height(2.dp))
      Text(value, color = DeskSoftWhite, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
    }
  }
}

@Composable
private fun SecurityVectorGlyph(
  kind: SecurityGlyphKind,
  tint: Color,
  size: Dp,
) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = Stroke(width = w * 0.10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (kind) {
      SecurityGlyphKind.BACK_CHEVRON -> {
        val p = Path().apply {
          moveTo(w * 0.65f, h * 0.18f)
          lineTo(w * 0.30f, h * 0.50f)
          lineTo(w * 0.65f, h * 0.82f)
        }
        drawPath(p, color = tint, style = stroke)
      }
      SecurityGlyphKind.MORE_DOTS -> {
        drawCircle(tint, w * 0.09f, Offset(w * 0.50f, h * 0.22f))
        drawCircle(tint, w * 0.09f, Offset(w * 0.50f, h * 0.50f))
        drawCircle(tint, w * 0.09f, Offset(w * 0.50f, h * 0.78f))
      }
      SecurityGlyphKind.PHONE -> {
        drawRoundRect(
          color = tint,
          topLeft = Offset(w * 0.26f, h * 0.12f),
          size = Size(w * 0.48f, h * 0.76f),
          cornerRadius = CornerRadius(w * 0.10f),
          style = stroke,
        )
        drawCircle(tint, w * 0.05f, Offset(w * 0.50f, h * 0.76f))
      }
      SecurityGlyphKind.PIN -> {
        drawCircle(tint, w * 0.28f, Offset(w * 0.50f, h * 0.40f), style = stroke)
        drawCircle(tint, w * 0.10f, Offset(w * 0.50f, h * 0.40f))
        drawLine(tint, Offset(w * 0.30f, h * 0.58f), Offset(w * 0.50f, h * 0.88f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.70f, h * 0.58f), Offset(w * 0.50f, h * 0.88f), strokeWidth = stroke.width, cap = StrokeCap.Round)
      }
      SecurityGlyphKind.CLOCK -> {
        drawCircle(tint, w * 0.38f, Offset(w * 0.50f, h * 0.50f), style = stroke)
        drawLine(tint, Offset(w * 0.50f, h * 0.50f), Offset(w * 0.50f, h * 0.28f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.50f, h * 0.50f), Offset(w * 0.70f, h * 0.54f), strokeWidth = stroke.width, cap = StrokeCap.Round)
      }
      SecurityGlyphKind.GLOBE -> {
        drawCircle(tint, w * 0.38f, Offset(w * 0.50f, h * 0.50f), style = stroke)
        drawLine(tint, Offset(w * 0.12f, h * 0.50f), Offset(w * 0.88f, h * 0.50f), strokeWidth = stroke.width * 0.8f)
        drawOval(tint, Offset(w * 0.34f, h * 0.12f), Size(w * 0.32f, h * 0.76f), style = Stroke(stroke.width * 0.8f))
      }
      SecurityGlyphKind.SHIELD -> {
        val p = Path().apply {
          moveTo(w * 0.50f, h * 0.10f)
          lineTo(w * 0.86f, h * 0.24f)
          lineTo(w * 0.84f, h * 0.55f)
          cubicTo(w * 0.80f, h * 0.76f, w * 0.66f, h * 0.88f, w * 0.50f, h * 0.92f)
          cubicTo(w * 0.34f, h * 0.88f, w * 0.20f, h * 0.76f, w * 0.16f, h * 0.55f)
          lineTo(w * 0.14f, h * 0.24f)
          close()
        }
        drawPath(p, color = tint, style = stroke)
        drawLine(tint, Offset(w * 0.36f, h * 0.52f), Offset(w * 0.47f, h * 0.63f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.47f, h * 0.63f), Offset(w * 0.66f, h * 0.39f), strokeWidth = stroke.width, cap = StrokeCap.Round)
      }
      SecurityGlyphKind.LOCK -> {
        drawRoundRect(
          color = tint,
          topLeft = Offset(w * 0.22f, h * 0.44f),
          size = Size(w * 0.56f, h * 0.44f),
          cornerRadius = CornerRadius(w * 0.08f),
          style = stroke,
        )
        drawArc(
          color = tint,
          startAngle = 180f,
          sweepAngle = 180f,
          useCenter = false,
          topLeft = Offset(w * 0.32f, h * 0.16f),
          size = Size(w * 0.36f, h * 0.36f),
          style = stroke,
        )
      }
      SecurityGlyphKind.CHECK_CIRCLE -> {
        drawCircle(tint, w * 0.38f, Offset(w * 0.50f, h * 0.50f), style = stroke)
        drawLine(tint, Offset(w * 0.33f, h * 0.52f), Offset(w * 0.46f, h * 0.64f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.46f, h * 0.64f), Offset(w * 0.68f, h * 0.38f), strokeWidth = stroke.width, cap = StrokeCap.Round)
      }
      SecurityGlyphKind.CLOSE_CIRCLE -> {
        drawCircle(tint, w * 0.38f, Offset(w * 0.50f, h * 0.50f), style = stroke)
        drawLine(tint, Offset(w * 0.36f, h * 0.36f), Offset(w * 0.64f, h * 0.64f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.64f, h * 0.36f), Offset(w * 0.36f, h * 0.64f), strokeWidth = stroke.width, cap = StrokeCap.Round)
      }
    }
  }
}

@Composable
private fun DesktopRoundedSecurityLogo(size: Dp) {
  Box(
    modifier = Modifier
      .size(size)
      .clip(RoundedCornerShape(16.dp))
      .background(Brush.linearGradient(listOf(Color(0xFFF43F5E), DeskCrimsonRich, Color(0xFF991B1B)))),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.size(size * 0.58f)) {
      val cw = this.size.width
      val ch = this.size.height
      val wing = Path().apply {
        moveTo(cw * 0.92f, ch * 0.14f)
        lineTo(cw * 0.06f, ch * 0.48f)
        lineTo(cw * 0.38f, ch * 0.62f)
        lineTo(cw * 0.86f, ch * 0.22f)
        lineTo(cw * 0.46f, ch * 0.68f)
        lineTo(cw * 0.72f, ch * 0.90f)
        close()
      }
      drawPath(wing, color = Color.White)
    }
  }
}

@Composable
private fun DesktopSecurityBackdrop(showWorldDots: Boolean) {
  Canvas(modifier = Modifier.fillMaxSize()) {
    drawRect(color = DeskSecBg)
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(DeskCrimsonRich.copy(alpha = 0.22f), Color.Transparent),
        center = Offset(size.width * 0.5f, size.height * 0.18f),
        radius = size.width * 0.75f,
      ),
      radius = size.width * 0.75f,
      center = Offset(size.width * 0.5f, size.height * 0.18f),
    )
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(DeskCrimsonDeep.copy(alpha = 0.32f), Color.Transparent),
        center = Offset(size.width * 0.86f, size.height * 0.94f),
        radius = size.width * 0.55f,
      ),
      radius = size.width * 0.55f,
      center = Offset(size.width * 0.86f, size.height * 0.94f),
    )
    if (showWorldDots) {
      val bandH = size.height * 0.36f
      val cols = 48
      val rows = 16
      val stepX = size.width / cols
      val stepY = bandH / rows
      for (r in 0 until rows) {
        for (c in 0 until cols) {
          val nx = c.toFloat() / cols
          val ny = r.toFloat() / rows
          val visibleDot = (nx in 0.10f..0.28f && ny in 0.16f..0.75f) ||
            (nx in 0.42f..0.82f && ny in 0.14f..0.76f)
          if (visibleDot) {
            drawCircle(
              color = DeskCrimsonRich.copy(alpha = 0.22f),
              radius = 1.8.dp.toPx(),
              center = Offset(c * stepX + stepX * 0.5f, 36.dp.toPx() + r * stepY),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DesktopPhoneToClockIllustration(isDenied: Boolean, modifier: Modifier = Modifier) {
  val inf = rememberInfiniteTransition()
  val pulse by inf.animateFloat(
    initialValue = 0.75f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
  )
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val accent = if (isDenied) Color(0xFFF87171) else DeskCrimsonGlow
    val phoneW = h * 0.52f
    val phoneH = h * 0.84f
    val phoneCenter = Offset(w * 0.42f, h * 0.50f)
    rotate(-9f, pivot = phoneCenter) {
      val topLeft = Offset(phoneCenter.x - phoneW / 2f, phoneCenter.y - phoneH / 2f)
      drawRoundRect(
        color = Color(0xFF0E0B0C),
        topLeft = topLeft,
        size = Size(phoneW, phoneH),
        cornerRadius = CornerRadius(14.dp.toPx()),
      )
      drawRoundRect(
        color = Color(0x66FFFFFF),
        topLeft = topLeft,
        size = Size(phoneW, phoneH),
        cornerRadius = CornerRadius(14.dp.toPx()),
        style = Stroke(1.5.dp.toPx()),
      )
      val badgeSize = phoneW * 0.48f
      drawRoundRect(
        brush = Brush.linearGradient(listOf(DeskCrimsonGlow, DeskCrimsonDeep)),
        topLeft = Offset(phoneCenter.x - badgeSize / 2f, phoneCenter.y - badgeSize / 2f),
        size = Size(badgeSize, badgeSize),
        cornerRadius = CornerRadius(10.dp.toPx()),
      )
    }
    val arcPath = Path().apply {
      moveTo(w * 0.55f, h * 0.16f)
      quadraticTo(w * 0.66f, h * 0.08f, w * 0.74f, h * 0.25f)
    }
    drawPath(
      path = arcPath,
      color = DeskSoftWhite.copy(alpha = 0.85f),
      style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 6.dp.toPx()), 0f)),
    )
    val clockCenter = Offset(w * 0.75f, h * 0.46f)
    val clockRadius = h * 0.21f
    drawCircle(Color(0xFF151213), clockRadius, clockCenter)
    drawCircle(accent.copy(alpha = 0.85f * pulse), clockRadius, clockCenter, style = Stroke(3.dp.toPx()))
    drawLine(DeskSoftWhite, clockCenter, Offset(clockCenter.x, clockCenter.y - clockRadius * 0.52f), 2.5.dp.toPx(), StrokeCap.Round)
    drawLine(DeskSoftWhite, clockCenter, Offset(clockCenter.x + clockRadius * 0.48f, clockCenter.y), 2.5.dp.toPx(), StrokeCap.Round)
  }
}

@Composable
private fun DesktopShieldLockEmblem(size: Dp) {
  Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = this.size.width
      val h = this.size.height
      val shield = Path().apply {
        moveTo(w * 0.50f, h * 0.08f)
        lineTo(w * 0.88f, h * 0.24f)
        lineTo(w * 0.86f, h * 0.55f)
        cubicTo(w * 0.83f, h * 0.76f, w * 0.68f, h * 0.89f, w * 0.50f, h * 0.94f)
        cubicTo(w * 0.32f, h * 0.89f, w * 0.17f, h * 0.76f, w * 0.14f, h * 0.55f)
        lineTo(w * 0.12f, h * 0.24f)
        close()
      }
      drawPath(shield, Brush.verticalGradient(listOf(DeskCrimsonRich.copy(alpha = 0.75f), DeskCrimsonDeep.copy(alpha = 0.9f))))
      drawPath(shield, DeskCrimsonGlow, style = Stroke(2.dp.toPx()))
    }
    SecurityVectorGlyph(SecurityGlyphKind.LOCK, DeskSoftWhite, size * 0.38f)
  }
}

@Composable
private fun DesktopApprovedCheckEmblem(size: Dp) {
  Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = this.size.width / 2f
      val cy = this.size.height / 2f
      val r = this.size.minDimension * 0.34f
      drawCircle(Color(0xFF1F0709), r, Offset(cx, cy))
      drawCircle(DeskCrimsonGlow, r, Offset(cx, cy), style = Stroke(2.5.dp.toPx()))
    }
    Canvas(modifier = Modifier.size(size * 0.36f)) {
      val w = this.size.width
      val h = this.size.height
      drawLine(DeskSoftWhite, Offset(w * 0.18f, h * 0.52f), Offset(w * 0.42f, h * 0.76f), strokeWidth = 3.5.dp.toPx(), cap = StrokeCap.Round)
      drawLine(DeskSoftWhite, Offset(w * 0.42f, h * 0.76f), Offset(w * 0.84f, h * 0.28f), strokeWidth = 3.5.dp.toPx(), cap = StrokeCap.Round)
    }
  }
}

private fun formatDesktopRequestTime(timestampMs: Long): String {
  if (timestampMs <= 0L) return "Just now"
  val date = Date(timestampMs)
  val todayFmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
  val prefix = if (todayFmt.format(date) == todayFmt.format(Date())) "Today, " else SimpleDateFormat("MMM d, ", Locale.getDefault()).format(date)
  return prefix + SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
}

private fun formatDesktopCountdown(seconds: Long): String {
  val safe = seconds.coerceAtLeast(0L)
  val mins = safe / 60L
  val secs = safe % 60L
  return "%02d:%02d".format(mins, secs)
}
