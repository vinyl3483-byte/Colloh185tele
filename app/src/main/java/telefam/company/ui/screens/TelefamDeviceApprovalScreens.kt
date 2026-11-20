package telefam.company.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import telefam.company.core.TelefamCoreEngine
import telefam.company.ui.viewmodel.DeviceApprovalWaitState
import telefam.company.ui.viewmodel.IncomingLoginRequestState
import telefam.company.ui.viewmodel.LoginApprovedOverlayState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DarkSecurityBg = Color(0xFF070405)
private val DarkSecurityCardBg = Color(0xFF130B0D)
private val DarkSecurityBorder = Color(0x28FFFFFF)
private val CrimsonGlow = Color(0xFFEF233C)
private val CrimsonRich = Color(0xFFD91E2A)
private val CrimsonDeep = Color(0xFF7D0914)
private val SoftWhite = Color(0xFFF9FAFB)
private val MutedText = Color(0xFF9CA3AF)
private val GreenSuccess = Color(0xFF22C55E)

@Composable
fun DeviceApprovalWaitScreen(
  waitState: DeviceApprovalWaitState,
  isDeniedBlocked: Boolean,
  isLoading: Boolean,
  errorMessage: String?,
  onBackChangeEmail: () -> Unit,
  onTryAnotherWayGoogle: () -> Unit,
) {
  BackHandler(enabled = true) {
    onBackChangeEmail()
  }

  var showMenu by remember { mutableStateOf(false) }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkSecurityBg)
      .testTag(if (isDeniedBlocked) "device_approval_denied_screen" else "device_approval_wait_screen"),
  ) {
    val compactHeight = maxHeight < 680.dp
    val wideScreen = maxWidth >= 600.dp

    SecurityRadialBackdrop(showWorldDots = false)

    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = if (wideScreen) 32.dp else 20.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 440.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Top Bar: Back chevron, Brand header centered below/inline, and 3-dot security menu
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = onBackChangeEmail,
            modifier = Modifier
              .size(48.dp)
              .testTag("device_approval_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
              contentDescription = "Back to email sign in",
              tint = SoftWhite,
              modifier = Modifier.size(30.dp),
            )
          }

          TelefamRoundedAppBadge(size = if (compactHeight) 50.dp else 56.dp)

          Box {
            IconButton(
              onClick = { showMenu = true },
              modifier = Modifier
                .size(48.dp)
                .testTag("device_approval_more_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Approval options",
                tint = SoftWhite,
              )
            }
            DropdownMenu(
              expanded = showMenu,
              onDismissRequest = { showMenu = false },
            ) {
              DropdownMenuItem(
                text = { Text("Verify with Google") },
                onClick = {
                  showMenu = false
                  onTryAnotherWayGoogle()
                },
              )
              DropdownMenuItem(
                text = { Text("Use another email") },
                onClick = {
                  showMenu = false
                  onBackChangeEmail()
                },
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Telefam",
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 21.sp,
            letterSpacing = 0.2.sp,
          ),
          color = SoftWhite,
        )

        Spacer(modifier = Modifier.height(if (compactHeight) 14.dp else 22.dp))

        DeviceToClockVectorIllustration(
          isDenied = isDeniedBlocked,
          modifier = Modifier
            .fillMaxWidth()
            .height(if (compactHeight) 124.dp else 148.dp),
        )

        Spacer(modifier = Modifier.height(if (compactHeight) 14.dp else 20.dp))

        Text(
          text = if (isDeniedBlocked) "Login request denied" else "We sent a request to",
          style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
          ),
          color = SoftWhite,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = waitState.targetDeviceName.ifBlank { "Your trusted device" },
          style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
          ),
          color = CrimsonGlow,
          textAlign = TextAlign.Center,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.testTag("device_approval_target_name"),
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = buildAnnotatedString {
            if (isDeniedBlocked) {
              append("Your trusted device denied access and blocked this sign-in attempt. Confirm ownership via Google to continue.")
            } else {
              append("Please click ")
              withStyle(SpanStyle(color = SoftWhite, fontWeight = FontWeight.Bold)) {
                append("Accept")
              }
              append(" in another device\nto confirm it's you.")
            }
          },
          style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
          color = MutedText,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 12.dp),
        )

        Spacer(modifier = Modifier.height(if (compactHeight) 16.dp else 22.dp))

        // Request Metadata Card
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("device_approval_info_card"),
          shape = RoundedCornerShape(18.dp),
          color = DarkSecurityCardBg.copy(alpha = 0.90f),
          border = BorderStroke(1.dp, DarkSecurityBorder),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            ApprovalDetailRow(
              icon = Icons.Outlined.Smartphone,
              label = "Device",
              value = waitState.targetDeviceName.ifBlank { "Trusted Device" },
              useCircularRedBadge = false,
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            ApprovalDetailRow(
              icon = Icons.Rounded.LocationOn,
              label = "Location",
              value = waitState.targetLocation.ifBlank { "Detected automatically" },
              useCircularRedBadge = false,
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            ApprovalDetailRow(
              icon = Icons.Outlined.Schedule,
              label = "Time",
              value = buildString {
                append(formatRequestTimestamp(waitState.requestTimeMs))
                if (!isDeniedBlocked && waitState.remainingSeconds > 0L) {
                  append("  •  ")
                  append(TelefamCoreEngine.formatCountdown(waitState.remainingSeconds))
                }
              },
              useCircularRedBadge = false,
            )
          }
        }

        if (!errorMessage.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(12.dp))
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF2B0B0F),
            border = BorderStroke(1.dp, CrimsonGlow.copy(alpha = 0.55f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = "Security notice",
                tint = Color(0xFFFCA5A5),
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFEE2E2),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(if (compactHeight) 16.dp else 24.dp))

        // Outlined Crimson Glow Pill Button: "Try Another Way"
        OutlinedButton(
          onClick = onTryAnotherWayGoogle,
          enabled = !isLoading,
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("device_approval_try_another_way_button"),
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.5.dp, CrimsonRich),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF170709),
            contentColor = SoftWhite,
          ),
          contentPadding = PaddingValues(horizontal = 18.dp),
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              color = CrimsonGlow,
              strokeWidth = 2.dp,
            )
          } else {
            Box(
              modifier = Modifier
                .size(26.dp)
                .border(1.5.dp, CrimsonGlow, CircleShape)
                .background(CrimsonDeep.copy(alpha = 0.35f), CircleShape),
              contentAlignment = Alignment.Center,
            ) {
              Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(3) {
                  Box(
                    modifier = Modifier
                      .size(3.dp)
                      .clip(CircleShape)
                      .background(Color(0xFFFCA5A5)),
                  )
                }
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "Try Another Way",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
              ),
              color = SoftWhite,
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Rounded.VerifiedUser,
            contentDescription = "Account security warning",
            tint = MutedText,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "If you didn't request this,\nyour account may be at risk.",
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
            color = MutedText,
            textAlign = TextAlign.Center,
          )
        }

        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }
}

@Composable
fun IncomingLoginRequestFullScreenOverlay(
  request: IncomingLoginRequestState,
  isResponding: Boolean,
  actionError: String?,
  onDeny: () -> Unit,
  onAccept: () -> Unit,
) {
  BackHandler(enabled = true) {
    // Explicit security gate: user must Deny or Accept on the trusted device.
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkSecurityBg)
      .testTag("incoming_login_request_overlay"),
  ) {
    val compactHeight = maxHeight < 700.dp
    val wideScreen = maxWidth >= 600.dp

    SecurityRadialBackdrop(showWorldDots = true)

    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = if (wideScreen) 32.dp else 20.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 440.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        TelefamRoundedAppBadge(size = if (compactHeight) 46.dp else 54.dp)

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Telefam",
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
          ),
          color = SoftWhite,
        )

        Spacer(modifier = Modifier.height(if (compactHeight) 8.dp else 12.dp))

        // Glowing Crimson Shield + Padlock Emblem
        ShieldLockVectorEmblem(
          size = if (compactHeight) 54.dp else 64.dp,
        )

        Spacer(modifier = Modifier.height(if (compactHeight) 8.dp else 12.dp))

        Text(
          text = "New login request",
          style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 23.sp,
          ),
          color = SoftWhite,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = buildAnnotatedString {
            append("Someone is trying to log in to your\n")
            withStyle(SpanStyle(color = CrimsonGlow, fontWeight = FontWeight.SemiBold)) {
              append("Telefam")
            }
            append(" account on another device.")
          },
          style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 19.sp),
          color = MutedText,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(if (compactHeight) 10.dp else 14.dp))

        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("incoming_login_details_card"),
          shape = RoundedCornerShape(18.dp),
          color = DarkSecurityCardBg.copy(alpha = 0.90f),
          border = BorderStroke(1.dp, DarkSecurityBorder),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            ApprovalDetailRow(
              icon = Icons.Outlined.Smartphone,
              label = "Device",
              value = request.deviceName.ifBlank { "Unknown Device" },
              useCircularRedBadge = true,
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            ApprovalDetailRow(
              icon = Icons.Rounded.LocationOn,
              label = "Location",
              value = "${request.location.ifBlank { "Approximate location unavailable" }} (Approximate)",
              useCircularRedBadge = true,
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            ApprovalDetailRow(
              icon = Icons.Outlined.Schedule,
              label = "Time",
              value = formatRequestTimestamp(request.requestedAtMs),
              useCircularRedBadge = true,
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            ApprovalDetailRow(
              icon = Icons.Outlined.Language,
              label = "IP Address",
              value = request.ipAddressMasked.ifBlank { "Hidden" },
              useCircularRedBadge = true,
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Security Action Warning Card
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF1A090B),
          border = BorderStroke(1.dp, CrimsonGlow.copy(alpha = 0.40f)),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(CrimsonRich.copy(alpha = 0.25f))
                .border(1.dp, CrimsonGlow.copy(alpha = 0.65f), CircleShape),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.VerifiedUser,
                contentDescription = "Security tip",
                tint = CrimsonGlow,
                modifier = Modifier.size(17.dp),
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = buildAnnotatedString {
                withStyle(SpanStyle(color = SoftWhite, fontWeight = FontWeight.SemiBold)) {
                  append("If this wasn't you,")
                }
                append(" you can deny this request and secure your account.")
              },
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 17.sp),
              color = Color(0xFFE5E7EB),
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Expiration Countdown Banner
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          color = DarkSecurityCardBg.copy(alpha = 0.90f),
          border = BorderStroke(1.dp, DarkSecurityBorder),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp, horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = "Expiry countdown",
                tint = CrimsonGlow,
                modifier = Modifier.size(15.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Request expires in",
                style = MaterialTheme.typography.labelMedium,
                color = MutedText,
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = TelefamCoreEngine.formatCountdown(request.remainingSeconds),
              style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                letterSpacing = 1.sp,
              ),
              color = SoftWhite,
              modifier = Modifier.testTag("incoming_login_countdown"),
            )
          }
        }

        if (!actionError.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = actionError,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFFCA5A5),
            textAlign = TextAlign.Center,
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Deny / Accept Pill Action Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          OutlinedButton(
            onClick = onDeny,
            enabled = !isResponding,
            modifier = Modifier
              .weight(1f)
              .height(50.dp)
              .testTag("incoming_login_deny_button"),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, CrimsonGlow.copy(alpha = 0.75f)),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = Color(0xFF170709),
              contentColor = CrimsonGlow,
            ),
          ) {
            Icon(
              imageVector = Icons.Outlined.Cancel,
              contentDescription = "Deny login",
              tint = CrimsonGlow,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Deny",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
              color = Color(0xFFF87171),
            )
          }

          Button(
            onClick = onAccept,
            enabled = !isResponding,
            modifier = Modifier
              .weight(1f)
              .height(50.dp)
              .testTag("incoming_login_accept_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = CrimsonRich,
              contentColor = SoftWhite,
            ),
          ) {
            if (isResponding) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = SoftWhite,
                strokeWidth = 2.dp,
              )
            } else {
              Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Accept login",
                tint = SoftWhite,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Accept",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = SoftWhite,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = "Private security",
            tint = MutedText,
            modifier = Modifier.size(14.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "We'll never share your information",
            style = MaterialTheme.typography.labelSmall,
            color = MutedText,
          )
        }

        Spacer(modifier = Modifier.height(8.dp))
      }
    }
  }
}

@Composable
fun LoginApprovedFullScreenOverlay(
  approved: LoginApprovedOverlayState,
  onDone: () -> Unit,
) {
  BackHandler(enabled = true) {
    onDone()
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkSecurityBg)
      .testTag("login_approved_overlay"),
  ) {
    val compactHeight = maxHeight < 680.dp
    val wideScreen = maxWidth >= 600.dp

    SecurityRadialBackdrop(showWorldDots = false)

    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = if (wideScreen) 32.dp else 20.dp, vertical = 18.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 440.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Spacer(modifier = Modifier.height(8.dp))

        TelefamRoundedAppBadge(size = if (compactHeight) 56.dp else 64.dp)

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "Telefam",
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
          ),
          color = SoftWhite,
        )

        Spacer(modifier = Modifier.height(if (compactHeight) 18.dp else 28.dp))

        ApprovedCheckVectorEmblem(
          size = if (compactHeight) 104.dp else 124.dp,
        )

        Spacer(modifier = Modifier.height(if (compactHeight) 16.dp else 24.dp))

        Text(
          text = "Login Approved!",
          style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 25.sp,
          ),
          color = SoftWhite,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "You have successfully approved the login\non another device.",
          style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, lineHeight = 20.sp),
          color = MutedText,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(if (compactHeight) 18.dp else 24.dp))

        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          color = DarkSecurityCardBg.copy(alpha = 0.90f),
          border = BorderStroke(1.dp, DarkSecurityBorder),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            ApprovalDetailRow(
              icon = Icons.Outlined.Smartphone,
              label = "Device",
              value = approved.deviceName.ifBlank { "Approved Device" },
              useCircularRedBadge = false,
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            ApprovalDetailRow(
              icon = Icons.Rounded.LocationOn,
              label = "Location",
              value = approved.location.ifBlank { "Approximate location" },
              useCircularRedBadge = false,
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            ApprovalDetailRow(
              icon = Icons.Outlined.Schedule,
              label = "Time",
              value = formatRequestTimestamp(approved.requestedAtMs),
              useCircularRedBadge = false,
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF0B2212),
          border = BorderStroke(1.dp, GreenSuccess.copy(alpha = 0.45f)),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Rounded.VerifiedUser,
              contentDescription = "All set",
              tint = GreenSuccess,
              modifier = Modifier.size(30.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "All set!",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = SoftWhite,
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "The new device will now be able to access your Telefam account.",
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = Color(0xFFD1D5DB),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(if (compactHeight) 18.dp else 26.dp))

        Button(
          onClick = onDone,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("login_approved_done_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = CrimsonRich,
            contentColor = SoftWhite,
          ),
        ) {
          Text(
            text = "Done",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = SoftWhite,
          )
        }

        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }
}

@Composable
private fun ApprovalDetailRow(
  icon: ImageVector,
  label: String,
  value: String,
  useCircularRedBadge: Boolean,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (useCircularRedBadge) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(CrimsonDeep.copy(alpha = 0.45f))
          .border(1.dp, CrimsonGlow.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = Color(0xFFFCA5A5),
          modifier = Modifier.size(18.dp),
        )
      }
    } else {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = SoftWhite.copy(alpha = 0.85f),
        modifier = Modifier.size(22.dp),
      )
    }

    Spacer(modifier = Modifier.width(14.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
        color = MutedText,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium.copy(
          fontWeight = FontWeight.Medium,
          fontSize = 14.5.sp,
        ),
        color = SoftWhite,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun TelefamRoundedAppBadge(size: Dp) {
  Box(
    modifier = Modifier
      .size(size)
      .semantics { contentDescription = "Telefam icon" }
      .shadow(
        elevation = 16.dp,
        shape = RoundedCornerShape(16.dp),
        ambientColor = CrimsonGlow.copy(alpha = 0.45f),
        spotColor = CrimsonGlow.copy(alpha = 0.55f),
      )
      .clip(RoundedCornerShape(16.dp))
      .background(
        Brush.linearGradient(
          colors = listOf(Color(0xFFF43F5E), CrimsonRich, Color(0xFF991B1B)),
        ),
      ),
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
      val fold = Path().apply {
        moveTo(cw * 0.38f, ch * 0.62f)
        lineTo(cw * 0.38f, ch * 0.86f)
        lineTo(cw * 0.52f, ch * 0.72f)
        close()
      }
      drawPath(fold, color = Color(0xFFE5E7EB))
    }
  }
}

@Composable
private fun SecurityRadialBackdrop(showWorldDots: Boolean) {
  Canvas(modifier = Modifier.fillMaxSize()) {
    drawRect(color = DarkSecurityBg)
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(CrimsonRich.copy(alpha = 0.22f), Color.Transparent),
        center = Offset(size.width * 0.5f, size.height * 0.18f),
        radius = size.width * 0.85f,
      ),
      radius = size.width * 0.85f,
      center = Offset(size.width * 0.5f, size.height * 0.18f),
    )
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(CrimsonDeep.copy(alpha = 0.34f), Color.Transparent),
        center = Offset(size.width * 0.86f, size.height * 0.94f),
        radius = size.width * 0.65f,
      ),
      radius = size.width * 0.65f,
      center = Offset(size.width * 0.86f, size.height * 0.94f),
    )

    if (showWorldDots) {
      val topBandHeight = size.height * 0.38f
      val cols = 42
      val rows = 18
      val stepX = size.width / cols
      val stepY = topBandHeight / rows
      for (r in 0 until rows) {
        for (c in 0 until cols) {
          val nx = c.toFloat() / cols
          val ny = r.toFloat() / rows
          val inAmericas = nx in 0.08f..0.28f && ((ny in 0.16f..0.46f) || (nx in 0.18f..0.27f && ny in 0.48f..0.82f))
          val inEurasiaAfrica = (nx in 0.42f..0.84f && ny in 0.12f..0.48f) ||
            (nx in 0.44f..0.60f && ny in 0.48f..0.82f)
          val inOceania = nx in 0.74f..0.88f && ny in 0.60f..0.78f
          if (inAmericas || inEurasiaAfrica || inOceania) {
            drawCircle(
              color = CrimsonRich.copy(alpha = 0.22f),
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
private fun DeviceToClockVectorIllustration(
  isDenied: Boolean,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "approval_vector_pulse")
  val pulse by infiniteTransition.animateFloat(
    initialValue = 0.75f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "pulse",
  )

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val accent = if (isDenied) Color(0xFFF87171) else CrimsonGlow

    // Ambient radial glow under smartphone and clock
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(accent.copy(alpha = 0.30f * pulse), Color.Transparent),
        center = Offset(w * 0.42f, h * 0.52f),
        radius = h * 0.62f,
      ),
      radius = h * 0.62f,
      center = Offset(w * 0.42f, h * 0.52f),
    )

    // Tilted Smartphone
    val phoneW = h * 0.52f
    val phoneH = h * 0.84f
    val phoneCenter = Offset(w * 0.41f, h * 0.50f)
    rotate(degrees = -9f, pivot = phoneCenter) {
      val topLeft = Offset(phoneCenter.x - phoneW / 2f, phoneCenter.y - phoneH / 2f)
      drawRoundRect(
        color = Color(0xFF3F1D22),
        topLeft = Offset(topLeft.x - 3.dp.toPx(), topLeft.y + 3.dp.toPx()),
        size = Size(phoneW, phoneH),
        cornerRadius = CornerRadius(14.dp.toPx()),
      )
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
        style = Stroke(width = 1.5.dp.toPx()),
      )
      // Phone emblem square
      val badgeSize = phoneW * 0.50f
      val badgeTopLeft = Offset(phoneCenter.x - badgeSize / 2f, phoneCenter.y - badgeSize / 2f)
      drawRoundRect(
        brush = Brush.linearGradient(listOf(CrimsonGlow, CrimsonDeep)),
        topLeft = badgeTopLeft,
        size = Size(badgeSize, badgeSize),
        cornerRadius = CornerRadius(10.dp.toPx()),
      )
      val plane = Path().apply {
        val bx = badgeTopLeft.x
        val by = badgeTopLeft.y
        moveTo(bx + badgeSize * 0.78f, by + badgeSize * 0.24f)
        lineTo(bx + badgeSize * 0.20f, by + badgeSize * 0.48f)
        lineTo(bx + badgeSize * 0.44f, by + badgeSize * 0.58f)
        lineTo(bx + badgeSize * 0.72f, by + badgeSize * 0.30f)
        lineTo(bx + badgeSize * 0.50f, by + badgeSize * 0.62f)
        lineTo(bx + badgeSize * 0.66f, by + badgeSize * 0.78f)
        close()
      }
      drawPath(plane, color = Color.White)
    }

    // Dotted Curved Transmission Arc to Clock
    val arcPath = Path().apply {
      moveTo(w * 0.56f, h * 0.16f)
      quadraticTo(w * 0.67f, h * 0.08f, w * 0.75f, h * 0.24f)
    }
    drawPath(
      path = arcPath,
      color = SoftWhite.copy(alpha = 0.85f),
      style = Stroke(
        width = 2.dp.toPx(),
        cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 6.dp.toPx()), 0f),
      ),
    )

    // Clock Circle on the right
    val clockCenter = Offset(w * 0.77f, h * 0.46f)
    val clockRadius = h * 0.21f
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(accent.copy(alpha = 0.32f), Color.Transparent),
        center = clockCenter,
        radius = clockRadius * 1.9f,
      ),
      radius = clockRadius * 1.9f,
      center = clockCenter,
    )
    drawCircle(
      color = Color(0xFF151213),
      radius = clockRadius,
      center = clockCenter,
    )
    drawCircle(
      brush = Brush.linearGradient(listOf(Color(0xFFE5E7EB), accent)),
      radius = clockRadius,
      center = clockCenter,
      style = Stroke(width = 3.dp.toPx()),
    )
    // Clock Hands (L-shape pointing ~12 & 3)
    drawLine(
      color = SoftWhite,
      start = clockCenter,
      end = Offset(clockCenter.x, clockCenter.y - clockRadius * 0.52f),
      strokeWidth = 2.5.dp.toPx(),
      cap = StrokeCap.Round,
    )
    drawLine(
      color = SoftWhite,
      start = clockCenter,
      end = Offset(clockCenter.x + clockRadius * 0.48f, clockCenter.y),
      strokeWidth = 2.5.dp.toPx(),
      cap = StrokeCap.Round,
    )
    drawCircle(color = SoftWhite, radius = 2.dp.toPx(), center = clockCenter)

    // 4-point Vector Sparkles
    val sparkles = listOf(
      Offset(w * 0.21f, h * 0.26f) to 6.dp.toPx(),
      Offset(w * 0.85f, h * 0.14f) to 5.dp.toPx(),
    )
    sparkles.forEach { (center, r) ->
      val star = Path().apply {
        moveTo(center.x, center.y - r)
        lineTo(center.x + r * 0.28f, center.y - r * 0.28f)
        lineTo(center.x + r, center.y)
        lineTo(center.x + r * 0.28f, center.y + r * 0.28f)
        lineTo(center.x, center.y + r)
        lineTo(center.x - r * 0.28f, center.y + r * 0.28f)
        lineTo(center.x - r, center.y)
        lineTo(center.x - r * 0.28f, center.y - r * 0.28f)
        close()
      }
      drawPath(star, color = CrimsonGlow.copy(alpha = 0.8f * pulse))
    }
  }
}

@Composable
private fun ShieldLockVectorEmblem(size: Dp) {
  Box(
    modifier = Modifier
      .size(size)
      .semantics { contentDescription = "Login verification shield" },
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = this.size.width
      val h = this.size.height
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(CrimsonGlow.copy(alpha = 0.35f), Color.Transparent),
          center = Offset(w / 2f, h / 2f),
          radius = w * 0.65f,
        ),
        radius = w * 0.65f,
        center = Offset(w / 2f, h / 2f),
      )
      val shield = Path().apply {
        moveTo(w * 0.50f, h * 0.08f)
        lineTo(w * 0.88f, h * 0.24f)
        lineTo(w * 0.86f, h * 0.55f)
        cubicTo(w * 0.83f, h * 0.76f, w * 0.68f, h * 0.89f, w * 0.50f, h * 0.94f)
        cubicTo(w * 0.32f, h * 0.89f, w * 0.17f, h * 0.76f, w * 0.14f, h * 0.55f)
        lineTo(w * 0.12f, h * 0.24f)
        close()
      }
      drawPath(
        path = shield,
        brush = Brush.verticalGradient(listOf(CrimsonRich.copy(alpha = 0.75f), CrimsonDeep.copy(alpha = 0.9f))),
      )
      drawPath(
        path = shield,
        color = CrimsonGlow,
        style = Stroke(width = 2.dp.toPx()),
      )
    }
    Icon(
      imageVector = Icons.Rounded.Lock,
      contentDescription = null,
      tint = SoftWhite,
      modifier = Modifier.size(size * 0.38f),
    )
  }
}

@Composable
private fun ApprovedCheckVectorEmblem(size: Dp) {
  val infiniteTransition = rememberInfiniteTransition(label = "approved_glow")
  val pulse by infiniteTransition.animateFloat(
    initialValue = 0.85f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "check_pulse",
  )

  Box(
    modifier = Modifier
      .size(size)
      .semantics { contentDescription = "Login approved badge" },
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val cx = this.size.width / 2f
      val cy = this.size.height / 2f
      val r = this.size.minDimension * 0.34f
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(CrimsonGlow.copy(alpha = 0.32f * pulse), Color.Transparent),
          center = Offset(cx, cy),
          radius = r * 1.9f,
        ),
        radius = r * 1.9f,
        center = Offset(cx, cy),
      )
      // Floating vector dots
      val dots = listOf(
        Triple(Offset(cx - r * 1.65f, cy - r * 0.75f), 2.5.dp.toPx(), MutedText.copy(alpha = 0.55f)),
        Triple(Offset(cx - r * 1.45f, cy + r * 0.35f), 4.dp.toPx(), CrimsonGlow.copy(alpha = 0.8f)),
        Triple(Offset(cx - r * 1.25f, cy + r * 1.15f), 3.dp.toPx(), CrimsonRich.copy(alpha = 0.7f)),
        Triple(Offset(cx + r * 1.45f, cy - r * 0.90f), 3.dp.toPx(), CrimsonGlow.copy(alpha = 0.75f)),
        Triple(Offset(cx + r * 1.65f, cy + r * 0.20f), 4.dp.toPx(), CrimsonRich.copy(alpha = 0.85f)),
        Triple(Offset(cx + r * 1.25f, cy + r * 1.10f), 2.5.dp.toPx(), MutedText.copy(alpha = 0.55f)),
      )
      dots.forEach { (pos, rad, col) ->
        drawCircle(color = col, radius = rad, center = pos)
      }

      drawCircle(
        color = CrimsonDeep.copy(alpha = 0.45f),
        radius = r * 1.26f,
        center = Offset(cx, cy),
        style = Stroke(width = 1.5.dp.toPx()),
      )
      drawCircle(
        color = Color(0xFF1F0709),
        radius = r,
        center = Offset(cx, cy),
      )
      drawCircle(
        color = CrimsonGlow,
        radius = r,
        center = Offset(cx, cy),
        style = Stroke(width = 2.5.dp.toPx()),
      )
    }
    Icon(
      imageVector = Icons.Rounded.Check,
      contentDescription = null,
      tint = SoftWhite,
      modifier = Modifier.size(size * 0.36f),
    )
  }
}

private fun formatRequestTimestamp(timestampMs: Long): String {
  if (timestampMs <= 0L) return "Just now"
  val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
  val date = Date(timestampMs)
  val todayFmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
  val prefix = if (todayFmt.format(date) == todayFmt.format(Date())) "Today, " else {
    SimpleDateFormat("MMM d, ", Locale.getDefault()).format(date)
  }
  return prefix + fmt.format(date)
}
