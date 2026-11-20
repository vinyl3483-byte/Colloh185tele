package telefam.company.ui.screens

import android.content.Context
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.StopScreenShare
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
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
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import telefam.company.calls.ActiveCallUiState
import telefam.company.calls.CallQualityTier
import telefam.company.calls.SwitchVideoPromptState
import telefam.company.calls.TelefamCallMetaParser
import telefam.company.calls.TelefamCallPhase
import telefam.company.calls.TelefamCallType
import telefam.company.calls.VideoCallFilter
import telefam.company.core.TelefamContactsRules
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamPureWhite

@Composable
fun TelefamCallOverlayHost(
  callState: ActiveCallUiState,
  eglBaseContext: EglBase.Context?,
  onAttachLocalRenderer: (SurfaceViewRenderer?) -> Unit,
  onAttachRemoteRenderer: (SurfaceViewRenderer?) -> Unit,
  onAcceptIncoming: () -> Unit,
  onDeclineIncoming: () -> Unit,
  onHangUp: () -> Unit,
  onToggleMic: () -> Unit,
  onToggleSpeaker: () -> Unit,
  onToggleCamera: () -> Unit,
  onFlipCamera: () -> Unit,
  onSelectFilter: (VideoCallFilter) -> Unit,
  onSelectQuality: (CallQualityTier) -> Unit,
  onRequestScreenShare: () -> Unit,
  onStopScreenShare: () -> Unit,
  onRequestSwitchToVideo: () -> Unit,
  onRespondSwitchToVideo: (Boolean) -> Unit,
  onMinimizeToChat: () -> Unit,
  onExpandFromMinimized: () -> Unit,
  onDismissNotice: () -> Unit,
) {
  if (!callState.isSessionVisible) return

  Box(modifier = Modifier.fillMaxSize()) {
    AnimatedVisibility(
      visible = !callState.isMinimizedToChat,
      enter = fadeIn() + slideInVertically(initialOffsetY = { it / 10 }),
      exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 10 }),
    ) {
      TelefamActiveCallScreen(
        callState = callState,
        eglBaseContext = eglBaseContext,
        onAttachLocalRenderer = onAttachLocalRenderer,
        onAttachRemoteRenderer = onAttachRemoteRenderer,
        onAcceptIncoming = onAcceptIncoming,
        onDeclineIncoming = onDeclineIncoming,
        onHangUp = onHangUp,
        onToggleMic = onToggleMic,
        onToggleSpeaker = onToggleSpeaker,
        onToggleCamera = onToggleCamera,
        onFlipCamera = onFlipCamera,
        onSelectFilter = onSelectFilter,
        onSelectQuality = onSelectQuality,
        onRequestScreenShare = onRequestScreenShare,
        onStopScreenShare = onStopScreenShare,
        onRequestSwitchToVideo = onRequestSwitchToVideo,
        onRespondSwitchToVideo = onRespondSwitchToVideo,
        onMinimizeToChat = onMinimizeToChat,
        onDismissNotice = onDismissNotice,
      )
    }

    AnimatedVisibility(
      visible = callState.isMinimizedToChat,
      enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
      exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
      modifier = Modifier
        .align(Alignment.TopCenter)
        .statusBarsPadding()
        .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
      TelefamMinimizedCallBar(
        callState = callState,
        onExpand = onExpandFromMinimized,
        onToggleMic = onToggleMic,
        onHangUp = onHangUp,
      )
    }
  }
}

@Composable
private fun TelefamMinimizedCallBar(
  callState: ActiveCallUiState,
  onExpand: () -> Unit,
  onToggleMic: () -> Unit,
  onHangUp: () -> Unit,
) {
  val statusLabel = when (callState.phase) {
    TelefamCallPhase.CONNECTED -> TelefamCallMetaParser.formatDuration(callState.connectedElapsedSec)
    else -> callState.statusText.ifBlank { stringResource(R.string.call_status_connecting) }
  }
  Surface(
    shape = RoundedCornerShape(24.dp),
    color = Color(0xEB102A20),
    shadowElevation = 10.dp,
    modifier = Modifier
      .widthIn(max = 520.dp)
      .fillMaxWidth()
      .border(1.dp, Color(0xFF22C55E).copy(alpha = 0.45f), RoundedCornerShape(24.dp))
      .clickable(onClick = onExpand)
      .testTag("call_minimized_bar"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Box(
        modifier = Modifier
          .size(34.dp)
          .clip(CircleShape)
          .background(Color(0xFF22C55E).copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = if (callState.callType == TelefamCallType.VIDEO) Icons.Filled.Videocam else Icons.Filled.Call,
          contentDescription = null,
          tint = Color(0xFF4ADE80),
          modifier = Modifier.size(18.dp),
        )
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = callState.peerName.ifBlank { "Telefam" },
          color = TelefamPureWhite,
          fontSize = 13.5.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "$statusLabel · ${stringResource(R.string.call_minimized_return)}",
          color = Color(0xFFA7F3D0),
          fontSize = 11.5.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      IconButton(
        onClick = onToggleMic,
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(TelefamPureWhite.copy(alpha = 0.14f))
          .testTag("call_minimized_mic_button"),
      ) {
        Icon(
          imageVector = if (callState.micEnabled) Icons.Filled.Mic else Icons.Filled.MicOff,
          contentDescription = "Microphone",
          tint = TelefamPureWhite,
          modifier = Modifier.size(18.dp),
        )
      }
      IconButton(
        onClick = onHangUp,
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(TelefamCrimson)
          .testTag("call_minimized_end_button"),
      ) {
        Icon(
          imageVector = Icons.Filled.CallEnd,
          contentDescription = stringResource(R.string.call_action_hangup),
          tint = TelefamPureWhite,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

@Composable
private fun TelefamActiveCallScreen(
  callState: ActiveCallUiState,
  eglBaseContext: EglBase.Context?,
  onAttachLocalRenderer: (SurfaceViewRenderer?) -> Unit,
  onAttachRemoteRenderer: (SurfaceViewRenderer?) -> Unit,
  onAcceptIncoming: () -> Unit,
  onDeclineIncoming: () -> Unit,
  onHangUp: () -> Unit,
  onToggleMic: () -> Unit,
  onToggleSpeaker: () -> Unit,
  onToggleCamera: () -> Unit,
  onFlipCamera: () -> Unit,
  onSelectFilter: (VideoCallFilter) -> Unit,
  onSelectQuality: (CallQualityTier) -> Unit,
  onRequestScreenShare: () -> Unit,
  onStopScreenShare: () -> Unit,
  onRequestSwitchToVideo: () -> Unit,
  onRespondSwitchToVideo: (Boolean) -> Unit,
  onMinimizeToChat: () -> Unit,
  onDismissNotice: () -> Unit,
) {
  var showFilterTray by remember { mutableStateOf(false) }
  var showQualityMenu by remember { mutableStateOf(false) }

  BackHandler(enabled = true) {
    when {
      callState.phase == TelefamCallPhase.ENDED || callState.phase == TelefamCallPhase.NOT_ALLOWED -> onDismissNotice()
      callState.phase == TelefamCallPhase.INCOMING_RINGING -> onDeclineIncoming()
      else -> onMinimizeToChat()
    }
  }

  val statusLine = when (callState.phase) {
    TelefamCallPhase.OUTGOING_CALLING -> stringResource(R.string.call_status_calling)
    TelefamCallPhase.OUTGOING_RINGING -> "${stringResource(R.string.call_status_ringing)} · ${callState.remainingRingSec}s"
    TelefamCallPhase.INCOMING_RINGING -> callState.statusText.ifBlank {
      if (callState.callType == TelefamCallType.VIDEO) {
        stringResource(R.string.call_incoming_video)
      } else {
        stringResource(R.string.call_incoming_audio)
      }
    }
    TelefamCallPhase.CONNECTING -> stringResource(R.string.call_status_connecting)
    TelefamCallPhase.CONNECTED -> TelefamCallMetaParser.formatDuration(callState.connectedElapsedSec)
    TelefamCallPhase.NOT_ALLOWED -> stringResource(R.string.call_status_not_allowed)
    TelefamCallPhase.ENDED -> callState.statusText.ifBlank { stringResource(R.string.call_status_ended) }
    TelefamCallPhase.IDLE -> ""
  }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(Color(0xFF0A141E), Color(0xFF10202C), Color(0xFF080E14)),
        ),
      )
      .testTag("telefam_call_screen"),
  ) {
    val isWideLayout = maxWidth >= 640.dp
    val showRemoteVideo = callState.callType == TelefamCallType.VIDEO &&
      !callState.shouldShowPeerAvatar &&
      (callState.phase == TelefamCallPhase.CONNECTED || callState.phase == TelefamCallPhase.CONNECTING)

    // Layer 1: Remote Video Stream OR Peer Avatar Aura Stage
    if (showRemoteVideo && eglBaseContext != null) {
      WebRtcSurfaceView(
        eglBaseContext = eglBaseContext,
        mirror = false,
        isOverlay = false,
        onAttach = onAttachRemoteRenderer,
        modifier = Modifier
          .fillMaxSize()
          .testTag("call_remote_video_surface"),
      )
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors = listOf(
                Color.Black.copy(alpha = 0.56f),
                Color.Transparent,
                Color.Transparent,
                Color.Black.copy(alpha = 0.72f),
              ),
            ),
          ),
      )
    } else {
      CallAvatarCenterStage(
        callState = callState,
        statusLine = statusLine,
        isCompactHeight = maxHeight < 560.dp,
        modifier = Modifier.fillMaxSize(),
      )
    }

    // Layer 2: Top Header Bar (E2EE badge + Quality Menu + Peer title when video active)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 14.dp, vertical = 10.dp)
        .align(Alignment.TopCenter),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Black.copy(alpha = 0.42f),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = Color(0xFF4ADE80),
            modifier = Modifier.size(13.dp),
          )
          Text(
            text = "E2EE P2P",
            color = TelefamPureWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }

      if (showRemoteVideo) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .weight(1f)
            .padding(horizontal = 8.dp),
        ) {
          Text(
            text = callState.peerName.ifBlank { "Telefam" },
            color = TelefamPureWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = statusLine,
            color = Color(0xFFD1FAE5),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
          )
        }
      } else {
        Spacer(modifier = Modifier.weight(1f))
      }

      if (callState.phase == TelefamCallPhase.CONNECTED || callState.phase == TelefamCallPhase.CONNECTING) {
        Box {
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (callState.lowBandwidthWarning) Color(0xB892400E) else Color.Black.copy(alpha = 0.42f),
            modifier = Modifier
              .clickable { showQualityMenu = true }
              .testTag("call_quality_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.SignalCellularAlt,
                contentDescription = stringResource(R.string.call_action_quality),
                tint = if (callState.lowBandwidthWarning) Color(0xFFFCD34D) else Color(0xFF4ADE80),
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = if (callState.selectedQualityMode == CallQualityTier.AUTO) {
                  "Auto · ${callState.effectiveQualityTier.label}"
                } else {
                  callState.selectedQualityMode.label
                },
                color = TelefamPureWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
          DropdownMenu(
            expanded = showQualityMenu,
            onDismissRequest = { showQualityMenu = false },
          ) {
            CallQualityTier.entries.forEach { tier ->
              DropdownMenuItem(
                text = {
                  val selected = callState.selectedQualityMode == tier
                  Text(
                    text = if (selected) "${tier.label} (Active)" else tier.label,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                  )
                },
                onClick = {
                  showQualityMenu = false
                  onSelectQuality(tier)
                },
                modifier = Modifier.testTag("call_quality_tier_${tier.wireValue}"),
              )
            }
          }
        }
      }
    }

    // Layer 3: Local PiP Preview (Video / Screen Share)
    val showLocalPip = callState.shouldShowLocalVideoPreview &&
      eglBaseContext != null &&
      callState.phase != TelefamCallPhase.ENDED &&
      callState.phase != TelefamCallPhase.NOT_ALLOWED
    if (showLocalPip && eglBaseContext != null) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0E1621),
        shadowElevation = 8.dp,
        modifier = Modifier
          .align(if (isWideLayout) Alignment.TopEnd else Alignment.BottomEnd)
          .then(
            if (isWideLayout) {
              Modifier.statusBarsPadding().padding(top = 60.dp, end = 16.dp)
            } else {
              Modifier.navigationBarsPadding().padding(bottom = 148.dp, end = 14.dp)
            },
          )
          .width(if (isWideLayout) 156.dp else 112.dp)
          .height(if (isWideLayout) 216.dp else 160.dp)
          .border(1.5.dp, TelefamPureWhite.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
          .testTag("call_local_pip_container"),
      ) {
        Box(modifier = Modifier.fillMaxSize()) {
          WebRtcSurfaceView(
            eglBaseContext = eglBaseContext,
            mirror = !callState.localScreenSharing,
            isOverlay = true,
            onAttach = onAttachLocalRenderer,
            modifier = Modifier.fillMaxSize(),
          )
          if (callState.videoFilter != VideoCallFilter.NATURAL && !callState.localScreenSharing) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color.Black.copy(alpha = 0.56f),
              modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp),
            ) {
              Text(
                text = callState.videoFilter.label,
                color = TelefamPureWhite,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }
      }
    }

    // Layer 4: Middle Banners (Low Network + Switch Audio->Video Prompt + "Calls Not Allowed")
    Column(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .statusBarsPadding()
        .padding(top = 56.dp, start = 16.dp, end = 16.dp)
        .widthIn(max = 520.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (callState.lowBandwidthWarning && callState.phase == TelefamCallPhase.CONNECTED) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color(0xCC78350F),
          modifier = Modifier.testTag("call_low_bandwidth_banner"),
        ) {
          Text(
            text = stringResource(R.string.call_low_network_notice),
            color = Color(0xFFFEF3C7),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          )
        }
      }

      if (callState.switchVideoPrompt == SwitchVideoPromptState.INCOMING_REQUEST) {
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = Color(0xE6142536),
          shadowElevation = 8.dp,
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .testTag("call_switch_video_prompt"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Videocam,
              contentDescription = null,
              tint = Color(0xFF38BDF8),
              modifier = Modifier.size(22.dp),
            )
            Text(
              text = stringResource(R.string.call_switch_video_prompt),
              color = TelefamPureWhite,
              fontSize = 13.5.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.weight(1f),
            )
            Button(
              onClick = { onRespondSwitchToVideo(false) },
              colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.16f)),
              modifier = Modifier.testTag("call_switch_video_decline"),
            ) {
              Text(stringResource(R.string.call_action_decline), color = TelefamPureWhite, fontSize = 12.sp)
            }
            Button(
              onClick = { onRespondSwitchToVideo(true) },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
              modifier = Modifier.testTag("call_switch_video_accept"),
            ) {
              Text(stringResource(R.string.call_action_accept), color = TelefamPureWhite, fontSize = 12.sp)
            }
          }
        }
      } else if (callState.switchVideoPrompt == SwitchVideoPromptState.OUTGOING_PENDING) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color.Black.copy(alpha = 0.56f),
        ) {
          Text(
            text = "Requesting video…",
            color = TelefamPureWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          )
        }
      }
    }

    // Layer 5: Bottom Filter Tray + Action Controls
    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 14.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (callState.phase == TelefamCallPhase.NOT_ALLOWED) {
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = Color(0xEB2A1215),
          modifier = Modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .border(1.dp, TelefamCrimson.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .testTag("call_not_allowed_card"),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.Block,
              contentDescription = null,
              tint = Color(0xFFFCA5A5),
              modifier = Modifier.size(32.dp),
            )
            Text(
              text = stringResource(R.string.call_status_not_allowed),
              color = TelefamPureWhite,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = callState.errorNotice.ifBlank { stringResource(R.string.call_not_allowed_detail) },
              color = Color(0xFFFECACA),
              fontSize = 13.sp,
              textAlign = TextAlign.Center,
            )
            Button(
              onClick = onDismissNotice,
              colors = ButtonDefaults.buttonColors(containerColor = TelefamCrimson),
              modifier = Modifier.testTag("call_not_allowed_close"),
            ) {
              Text(stringResource(R.string.close_button), color = TelefamPureWhite)
            }
          }
        }
      } else if (callState.phase == TelefamCallPhase.INCOMING_RINGING) {
        Row(
          modifier = Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .padding(bottom = 14.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          CallCircleButton(
            icon = Icons.Filled.CallEnd,
            label = stringResource(R.string.call_action_decline),
            containerColor = TelefamCrimson,
            iconTint = TelefamPureWhite,
            sizeDp = 64,
            onClick = onDeclineIncoming,
            testTag = "call_incoming_decline_button",
          )
          CallCircleButton(
            icon = if (callState.callType == TelefamCallType.VIDEO) Icons.Filled.Videocam else Icons.Filled.Call,
            label = stringResource(R.string.call_action_accept),
            containerColor = Color(0xFF16A34A),
            iconTint = TelefamPureWhite,
            sizeDp = 64,
            onClick = onAcceptIncoming,
            testTag = "call_incoming_accept_button",
          )
        }
      } else if (callState.phase != TelefamCallPhase.ENDED) {
        AnimatedVisibility(
          visible = showFilterTray && callState.callType == TelefamCallType.VIDEO,
          enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
          exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
        ) {
          Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xDD111827),
            modifier = Modifier
              .widthIn(max = 580.dp)
              .testTag("call_filter_strip"),
          ) {
            Row(
              modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              VideoCallFilter.entries.forEach { filter ->
                val selected = callState.videoFilter == filter
                Surface(
                  shape = RoundedCornerShape(16.dp),
                  color = if (selected) Color(0xFF22C55E) else Color.White.copy(alpha = 0.12f),
                  modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectFilter(filter) }
                    .testTag("call_filter_${filter.wireValue}"),
                ) {
                  Text(
                    text = filter.label,
                    color = if (selected) Color(0xFF042F16) else TelefamPureWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                  )
                }
              }
            }
          }
        }

        Surface(
          shape = RoundedCornerShape(30.dp),
          color = Color(0xD9111827),
          shadowElevation = 10.dp,
          modifier = Modifier
            .widthIn(max = 620.dp)
            .fillMaxWidth()
            .border(1.dp, TelefamPureWhite.copy(alpha = 0.12f), RoundedCornerShape(30.dp)),
        ) {
          Row(
            modifier = Modifier
              .horizontalScroll(rememberScrollState())
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            CallCircleButton(
              icon = if (callState.micEnabled) Icons.Filled.Mic else Icons.Filled.MicOff,
              label = if (callState.micEnabled) "Mute" else "Unmute",
              containerColor = if (callState.micEnabled) Color.White.copy(alpha = 0.15f) else TelefamPureWhite,
              iconTint = if (callState.micEnabled) TelefamPureWhite else Color(0xFF111827),
              onClick = onToggleMic,
              testTag = "call_mic_button",
            )

            if (callState.callType == TelefamCallType.VIDEO) {
              CallCircleButton(
                icon = if (callState.localCamEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                label = "Camera",
                containerColor = if (callState.localCamEnabled) Color.White.copy(alpha = 0.15f) else TelefamPureWhite,
                iconTint = if (callState.localCamEnabled) TelefamPureWhite else Color(0xFF111827),
                onClick = onToggleCamera,
                testTag = "call_camera_button",
              )
              CallCircleButton(
                icon = Icons.Filled.Cameraswitch,
                label = stringResource(R.string.call_action_flip_camera),
                containerColor = Color.White.copy(alpha = 0.15f),
                iconTint = TelefamPureWhite,
                onClick = onFlipCamera,
                testTag = "call_flip_camera_button",
              )
              CallCircleButton(
                icon = if (callState.localScreenSharing) Icons.Filled.StopScreenShare else Icons.Filled.ScreenShare,
                label = if (callState.localScreenSharing) stringResource(R.string.call_action_stop_share) else stringResource(R.string.call_action_share_screen),
                containerColor = if (callState.localScreenSharing) Color(0xFF0284C7) else Color.White.copy(alpha = 0.15f),
                iconTint = TelefamPureWhite,
                onClick = {
                  if (callState.localScreenSharing) onStopScreenShare() else onRequestScreenShare()
                },
                testTag = "call_screen_share_button",
              )
              CallCircleButton(
                icon = Icons.Filled.AutoAwesome,
                label = stringResource(R.string.call_action_filters),
                containerColor = if (showFilterTray || callState.videoFilter != VideoCallFilter.NATURAL) {
                  Color(0xFF22C55E).copy(alpha = 0.30f)
                } else {
                  Color.White.copy(alpha = 0.15f)
                },
                iconTint = TelefamPureWhite,
                onClick = { showFilterTray = !showFilterTray },
                testTag = "call_filters_toggle_button",
              )
            } else {
              CallCircleButton(
                icon = if (callState.speakerEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                label = "Speaker",
                containerColor = if (callState.speakerEnabled) TelefamPureWhite else Color.White.copy(alpha = 0.15f),
                iconTint = if (callState.speakerEnabled) Color(0xFF111827) else TelefamPureWhite,
                onClick = onToggleSpeaker,
                testTag = "call_speaker_button",
              )
              CallCircleButton(
                icon = Icons.Filled.Videocam,
                label = stringResource(R.string.call_action_switch_video),
                containerColor = Color.White.copy(alpha = 0.15f),
                iconTint = TelefamPureWhite,
                onClick = onRequestSwitchToVideo,
                testTag = "call_switch_to_video_button",
              )
            }

            CallCircleButton(
              icon = Icons.AutoMirrored.Filled.Chat,
              label = stringResource(R.string.call_action_message),
              containerColor = Color.White.copy(alpha = 0.15f),
              iconTint = TelefamPureWhite,
              onClick = onMinimizeToChat,
              testTag = "call_message_button",
            )

            CallCircleButton(
              icon = Icons.Filled.CallEnd,
              label = stringResource(R.string.call_action_hangup),
              containerColor = TelefamCrimson,
              iconTint = TelefamPureWhite,
              onClick = onHangUp,
              testTag = "call_hangup_button",
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CallAvatarCenterStage(
  callState: ActiveCallUiState,
  statusLine: String,
  isCompactHeight: Boolean,
  modifier: Modifier = Modifier,
) {
  val ringAnim = rememberInfiniteTransition(label = "call_ring_pulse")
  val pulse by ringAnim.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(1700, easing = LinearEasing), RepeatMode.Restart),
    label = "pulse_progress",
  )

  val isRingingWave = callState.phase == TelefamCallPhase.OUTGOING_CALLING ||
    callState.phase == TelefamCallPhase.OUTGOING_RINGING ||
    callState.phase == TelefamCallPhase.INCOMING_RINGING

  val avatarSize = if (isCompactHeight) 100.dp else 132.dp

  Column(
    modifier = modifier.padding(horizontal = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Box(
      modifier = Modifier.size(avatarSize + 68.dp),
      contentAlignment = Alignment.Center,
    ) {
      if (isRingingWave) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val center = this.center
          val baseR = (avatarSize.toPx() / 2f) + 6.dp.toPx()
          val maxDelta = 30.dp.toPx()
          drawCircle(
            color = Color(0xFF22C55E).copy(alpha = (1f - pulse) * 0.45f),
            radius = baseR + maxDelta * pulse,
            center = center,
            style = Stroke(width = 2.5.dp.toPx()),
          )
          val p2 = (pulse + 0.5f) % 1f
          drawCircle(
            color = Color(0xFF4ADE80).copy(alpha = (1f - p2) * 0.35f),
            radius = baseR + maxDelta * p2,
            center = center,
            style = Stroke(width = 2.dp.toPx()),
          )
        }
      }

      Box(
        modifier = Modifier
          .size(avatarSize)
          .clip(CircleShape)
          .background(
            Brush.linearGradient(
              colors = listOf(Color(0xFF0EA5E9), Color(0xFF10B981)),
            ),
          )
          .border(3.dp, TelefamPureWhite.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        if (callState.peerAvatarUrl.isNotBlank()) {
          AsyncImage(
            model = callState.peerAvatarUrl,
            contentDescription = callState.peerName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Text(
            text = TelefamContactsRules.initialsFor(callState.peerName, callState.peerUsername),
            color = TelefamPureWhite,
            fontSize = if (isCompactHeight) 32.sp else 40.sp,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
      text = callState.peerName.ifBlank { "Telefam" },
      color = TelefamPureWhite,
      fontSize = if (isCompactHeight) 22.sp else 26.sp,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = statusLine,
      color = if (callState.phase == TelefamCallPhase.NOT_ALLOWED) Color(0xFFFCA5A5) else Color(0xFFA7F3D0),
      fontSize = 15.sp,
      fontWeight = FontWeight.Medium,
      textAlign = TextAlign.Center,
      modifier = Modifier.testTag("call_status_text"),
    )

    if (callState.callType == TelefamCallType.VIDEO &&
      !callState.peerInAppActive &&
      callState.phase == TelefamCallPhase.CONNECTED
    ) {
      Spacer(modifier = Modifier.height(10.dp))
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.45f),
        modifier = Modifier.testTag("call_peer_away_avatar_badge"),
      ) {
        Text(
          text = stringResource(R.string.call_peer_away_avatar),
          color = Color(0xFFE5E7EB),
          fontSize = 12.sp,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
      }
    }
  }
}

@Composable
private fun CallCircleButton(
  icon: ImageVector,
  label: String,
  containerColor: Color,
  iconTint: Color,
  onClick: () -> Unit,
  testTag: String,
  sizeDp: Int = 52,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
    modifier = Modifier.padding(horizontal = 4.dp),
  ) {
    IconButton(
      onClick = onClick,
      modifier = Modifier
        .size(sizeDp.dp)
        .clip(CircleShape)
        .background(containerColor)
        .testTag(testTag),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = iconTint,
        modifier = Modifier.size((sizeDp * 0.44f).dp),
      )
    }
    Text(
      text = label,
      color = Color(0xFFE5E7EB),
      fontSize = 10.5.sp,
      fontWeight = FontWeight.Medium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun WebRtcSurfaceView(
  eglBaseContext: EglBase.Context,
  mirror: Boolean,
  isOverlay: Boolean,
  onAttach: (SurfaceViewRenderer?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val rendererHolder = remember { mutableStateOf<SurfaceViewRenderer?>(null) }

  DisposableEffect(eglBaseContext) {
    onDispose {
      onAttach(null)
      runCatching { rendererHolder.value?.release() }
      rendererHolder.value = null
    }
  }

  AndroidView(
    factory = { ctx: Context ->
      SurfaceViewRenderer(ctx).apply {
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT,
        )
        runCatching {
          init(eglBaseContext, null)
          setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
          setEnableHardwareScaler(true)
          setMirror(mirror)
          if (isOverlay) {
            setZOrderMediaOverlay(true)
          }
        }
        rendererHolder.value = this
        onAttach(this)
      }
    },
    update = { view ->
      runCatching { view.setMirror(mirror) }
      onAttach(view)
    },
    modifier = modifier,
  )
}
