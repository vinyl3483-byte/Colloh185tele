package telefam.company.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DesktopCallOverlayHost(
  controller: DesktopCallController,
  apiClient: DesktopApiClient,
  modifier: Modifier = Modifier,
) {
  val ui by controller.uiState.collectAsState()
  val localFrame by controller.localPreviewFrame.collectAsState()
  val remoteFrame by controller.remotePreviewFrame.collectAsState()

  if (!ui.isSessionVisible) return

  Box(modifier = modifier.fillMaxSize()) {
    when {
      ui.phase == DesktopCallPhase.NOT_ALLOWED -> {
        DesktopCallsNotAllowedOverlay(
          ui = ui,
          apiClient = apiClient,
          onDismiss = { controller.dismissNotAllowed() },
        )
      }

      ui.isMinimizedToChat -> {
        DesktopMinimizedCallBar(
          ui = ui,
          apiClient = apiClient,
          onExpand = { controller.expandFromMinimized() },
          onToggleMic = { controller.toggleMic() },
          onHangUp = { controller.hangUp() },
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 12.dp, start = 16.dp, end = 16.dp),
        )
      }

      ui.phase == DesktopCallPhase.INCOMING_RINGING -> {
        DesktopIncomingCallOverlay(
          ui = ui,
          apiClient = apiClient,
          onAccept = { controller.acceptIncomingCall() },
          onDecline = { controller.declineIncomingCall() },
        )
      }

      else -> {
        DesktopActiveCallOverlay(
          ui = ui,
          apiClient = apiClient,
          localFrame = localFrame,
          remoteFrame = remoteFrame,
          onToggleMic = { controller.toggleMic() },
          onToggleSpeaker = { controller.toggleSpeaker() },
          onToggleCamera = { controller.toggleCamera() },
          onFlipCamera = { controller.flipCamera() },
          onToggleScreenShare = { controller.toggleScreenShare() },
          onSelectFilter = { controller.selectFilter(it) },
          onSelectQuality = { controller.setQualityTier(it) },
          onRequestSwitchToVideo = { controller.requestSwitchToVideo() },
          onAcceptSwitchToVideo = { controller.acceptSwitchToVideo() },
          onDeclineSwitchToVideo = { controller.declineSwitchToVideo() },
          onOpenChatMessage = { controller.minimizeToChat() },
          onHangUp = { controller.hangUp() },
        )
      }
    }
  }
}

@Composable
private fun DesktopCallsNotAllowedOverlay(
  ui: DesktopActiveCallState,
  apiClient: DesktopApiClient,
  onDismiss: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.72f))
      .padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = Color(0xFF171A21),
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
      modifier = Modifier.widthIn(max = 380.dp).fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        DesktopChatAvatar(ui.peerAvatarUrl, ui.peerName, apiClient, size = 72.dp)
        Text(
          text = ui.peerName,
          color = Color.White,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Surface(
          shape = CircleShape,
          color = DesktopCrimson.copy(alpha = 0.18f),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            DesktopChatVectorIcon(DesktopChatSvg.BLOCK, DesktopCrimson, 15.dp)
            Text(
              text = "Calls not allowed",
              color = Color(0xFFFF8A95),
              fontSize = 13.5.sp,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
        Text(
          text = ui.errorNotice.ifBlank { "A 'Call me' request was sent automatically in chat." },
          color = Color.White.copy(alpha = 0.76f),
          fontSize = 13.sp,
          lineHeight = 18.sp,
        )
        Button(
          onClick = onDismiss,
          shape = CircleShape,
          colors = ButtonDefaults.buttonColors(containerColor = DesktopCrimson),
          modifier = Modifier.height(48.dp).fillMaxWidth(),
        ) {
          Text("OK", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }
}

@Composable
private fun DesktopMinimizedCallBar(
  ui: DesktopActiveCallState,
  apiClient: DesktopApiClient,
  onExpand: () -> Unit,
  onToggleMic: () -> Unit,
  onHangUp: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onExpand,
    shape = RoundedCornerShape(28.dp),
    color = Color(0xFF141820),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
    shadowElevation = 10.dp,
    modifier = modifier.widthIn(max = 460.dp).fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      DesktopChatAvatar(ui.peerAvatarUrl, ui.peerName, apiClient, size = 36.dp)
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = ui.peerName,
          color = Color.White,
          fontSize = 13.5.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        val label = if (ui.phase == DesktopCallPhase.CONNECTED) {
          DesktopCallMetaParser.formatDuration(ui.connectedElapsedSec)
        } else {
          ui.statusText.ifBlank { "Connecting…" }
        }
        Text(
          text = label,
          color = Color(0xFF4ADE80),
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
        )
      }
      Surface(
        onClick = onToggleMic,
        shape = CircleShape,
        color = if (ui.micEnabled) Color.White.copy(alpha = 0.14f) else Color.White,
        modifier = Modifier.size(48.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          DesktopChatVectorIcon(
            kind = if (ui.micEnabled) DesktopChatSvg.MIC else DesktopChatSvg.MIC_OFF,
            tint = if (ui.micEnabled) Color.White else Color(0xFF141820),
            size = 18.dp,
          )
        }
      }
      Surface(
        onClick = onExpand,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.14f),
        modifier = Modifier.size(48.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          DesktopChatVectorIcon(DesktopChatSvg.EXPAND, Color.White, 18.dp)
        }
      }
      Surface(
        onClick = onHangUp,
        shape = CircleShape,
        color = Color(0xFFE11D48),
        modifier = Modifier.size(48.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          DesktopChatVectorIcon(DesktopChatSvg.CALL_END, Color.White, 19.dp)
        }
      }
    }
  }
}

@Composable
private fun DesktopIncomingCallOverlay(
  ui: DesktopActiveCallState,
  apiClient: DesktopApiClient,
  onAccept: () -> Unit,
  onDecline: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(Color(0xFF0E131B), Color(0xFF1A1F2B), Color(0xFF10131A)),
        ),
      )
      .padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(28.dp),
      color = Color(0xFF181C26),
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
      modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Text(
          text = if (ui.callType == DesktopCallType.VIDEO) "Incoming video call" else "Incoming audio call",
          color = Color.White.copy(alpha = 0.78f),
          fontSize = 13.5.sp,
          fontWeight = FontWeight.Medium,
        )
        DesktopChatAvatar(ui.peerAvatarUrl, ui.peerName, apiClient, size = 104.dp)
        Text(
          text = ui.peerName,
          color = Color.White,
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (ui.peerUsername.isNotBlank()) {
          Text(
            text = "@${ui.peerUsername}",
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 13.sp,
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Button(
            onClick = onDecline,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
            modifier = Modifier.weight(1f).height(52.dp),
          ) {
            DesktopChatVectorIcon(DesktopChatSvg.CALL_END, Color.White, 18.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Decline", color = Color.White, fontWeight = FontWeight.SemiBold)
          }
          Button(
            onClick = onAccept,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
            modifier = Modifier.weight(1f).height(52.dp),
          ) {
            DesktopChatVectorIcon(
              kind = if (ui.callType == DesktopCallType.VIDEO) DesktopChatSvg.VIDEO_CAM else DesktopChatSvg.PHONE_CALL,
              tint = Color.White,
              size = 18.dp,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Accept", color = Color.White, fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }
  }
}

@Composable
private fun DesktopActiveCallOverlay(
  ui: DesktopActiveCallState,
  apiClient: DesktopApiClient,
  localFrame: ImageBitmap?,
  remoteFrame: ImageBitmap?,
  onToggleMic: () -> Unit,
  onToggleSpeaker: () -> Unit,
  onToggleCamera: () -> Unit,
  onFlipCamera: () -> Unit,
  onToggleScreenShare: () -> Unit,
  onSelectFilter: (DesktopVideoFilter) -> Unit,
  onSelectQuality: (DesktopCallQualityTier) -> Unit,
  onRequestSwitchToVideo: () -> Unit,
  onAcceptSwitchToVideo: () -> Unit,
  onDeclineSwitchToVideo: () -> Unit,
  onOpenChatMessage: () -> Unit,
  onHangUp: () -> Unit,
) {
  var showFilters by remember { mutableStateOf(false) }
  var showQualityMenu by remember { mutableStateOf(false) }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(Color(0xFF0C1017), Color(0xFF171B24), Color(0xFF0D1016)),
        ),
      ),
  ) {
    val isCompact = maxWidth < 680.dp

    // 1. Remote video stage OR Peer Avatar (audio call / peer cam off / peer left app)
    if (!ui.shouldShowPeerAvatar && remoteFrame != null) {
      Image(
        bitmap = remoteFrame,
        contentDescription = ui.peerName,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    } else if (!ui.shouldShowPeerAvatar && ui.callType == DesktopCallType.VIDEO && ui.phase == DesktopCallPhase.CONNECTED) {
      // Remote camera active; show peer avatar cleanly overlaid while first keyframe arrives
      DesktopPeerAvatarStage(ui = ui, apiClient = apiClient)
    } else {
      DesktopPeerAvatarStage(ui = ui, apiClient = apiClient)
    }

    // 2. Top Status & Quality Bar
    Row(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.Black.copy(alpha = 0.45f),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          DesktopChatAvatar(ui.peerAvatarUrl, ui.peerName, apiClient, size = 30.dp)
          Column {
            Text(
              text = ui.peerName,
              color = Color.White,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            val statusStr = when {
              ui.phase == DesktopCallPhase.CONNECTED -> DesktopCallMetaParser.formatDuration(ui.connectedElapsedSec)
              ui.statusText.isNotBlank() -> ui.statusText
              else -> "Connecting…"
            }
            Text(
              text = statusStr,
              color = Color.White.copy(alpha = 0.80f),
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
            )
          }
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (!ui.peerMicEnabled && ui.phase == DesktopCallPhase.CONNECTED) {
          Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.55f),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              DesktopChatVectorIcon(DesktopChatSvg.MIC_OFF, Color(0xFFFCA5A5), 14.dp)
              Text("Muted", color = Color.White, fontSize = 11.5.sp)
            }
          }
        }

        Box {
          Surface(
            onClick = { showQualityMenu = true },
            shape = CircleShape,
            color = if (ui.lowBandwidthWarning) Color(0xFFB45309).copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.45f),
            modifier = Modifier.height(48.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              DesktopChatVectorIcon(DesktopChatSvg.QUALITY_SIGNAL, Color.White, 15.dp)
              Text(
                text = if (ui.lowBandwidthWarning) "Low connection · ${ui.effectiveQualityTier.label}" else ui.effectiveQualityTier.label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }
          DropdownMenu(expanded = showQualityMenu, onDismissRequest = { showQualityMenu = false }) {
            DesktopCallQualityTier.entries.forEach { tier ->
              DropdownMenuItem(
                text = {
                  Text(
                    text = if (ui.selectedQualityMode == tier) "${tier.label} (Active)" else tier.label,
                    fontWeight = if (ui.selectedQualityMode == tier) FontWeight.Bold else FontWeight.Normal,
                  )
                },
                onClick = {
                  showQualityMenu = false
                  onSelectQuality(tier)
                },
              )
            }
          }
        }
      }
    }

    // 3. Corner Local PiP Preview (when local video or screen share is active)
    if (ui.shouldShowLocalVideoPreview && localFrame != null) {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF10141C),
        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.35f)),
        shadowElevation = 12.dp,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(top = 78.dp, end = 20.dp)
          .size(width = if (isCompact) 116.dp else 176.dp, height = if (isCompact) 160.dp else 120.dp),
      ) {
        Box {
          Image(
            bitmap = localFrame,
            contentDescription = if (ui.localScreenSharing) "Screen sharing preview" else "Self camera preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
          if (ui.videoFilter != DesktopVideoFilter.NATURAL) {
            Surface(
              shape = CircleShape,
              color = Color.Black.copy(alpha = 0.62f),
              modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
            ) {
              Text(
                text = ui.videoFilter.label,
                color = Color.White,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
              )
            }
          }
          if (ui.localScreenSharing) {
            Surface(
              shape = CircleShape,
              color = DesktopCrimson.copy(alpha = 0.90f),
              modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
            ) {
              Text(
                text = "Screen",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
              )
            }
          }
        }
      }
    }

    // 4. Bottom Dock: Switch-to-Video banner + Live Filters strip + Action Bar
    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 18.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (ui.switchVideoPrompt == DesktopSwitchVideoPromptState.INCOMING_REQUEST) {
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = Color(0xFF1F2430),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
          modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            DesktopChatVectorIcon(DesktopChatSvg.VIDEO_CAM, Color.White, 18.dp)
            Text(
              text = "${ui.peerName} wants to switch to video",
              color = Color.White,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.weight(1f),
            )
            OutlinedButton(
              onClick = onDeclineSwitchToVideo,
              shape = CircleShape,
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
              modifier = Modifier.height(40.dp),
            ) {
              Text("Decline", color = Color.White, fontSize = 12.sp)
            }
            Button(
              onClick = onAcceptSwitchToVideo,
              shape = CircleShape,
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
              modifier = Modifier.height(40.dp),
            ) {
              Text("Switch", color = Color.White, fontSize = 12.sp)
            }
          }
        }
      } else if (ui.switchVideoPrompt == DesktopSwitchVideoPromptState.OUTGOING_PENDING) {
        Surface(
          shape = CircleShape,
          color = Color.Black.copy(alpha = 0.55f),
        ) {
          Text(
            text = "Requesting video switch…",
            color = Color.White,
            fontSize = 12.5.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
          )
        }
      }

      AnimatedVisibility(visible = showFilters && ui.callType == DesktopCallType.VIDEO, enter = fadeIn(), exit = fadeOut()) {
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = Color.Black.copy(alpha = 0.62f),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
        ) {
          LazyRow(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(DesktopVideoFilter.entries) { filter ->
              val selected = ui.videoFilter == filter
              Surface(
                onClick = { onSelectFilter(filter) },
                shape = CircleShape,
                color = if (selected) DesktopCrimson else Color.White.copy(alpha = 0.12f),
                modifier = Modifier.height(40.dp),
              ) {
                Box(modifier = Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                  Text(
                    text = filter.label,
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  )
                }
              }
            }
          }
        }
      }

      Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF151922).copy(alpha = 0.92f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          DesktopCallControlPill(
            icon = if (ui.micEnabled) DesktopChatSvg.MIC else DesktopChatSvg.MIC_OFF,
            label = if (ui.micEnabled) "Mute" else "Muted",
            active = !ui.micEnabled,
            onClick = onToggleMic,
          )
          DesktopCallControlPill(
            icon = if (ui.speakerEnabled) DesktopChatSvg.SPEAKER_ON else DesktopChatSvg.MUTE,
            label = "Speaker",
            active = ui.speakerEnabled,
            onClick = onToggleSpeaker,
          )
          if (ui.callType == DesktopCallType.VIDEO) {
            DesktopCallControlPill(
              icon = if (ui.localCamEnabled) DesktopChatSvg.VIDEO_CAM else DesktopChatSvg.VIDEO_OFF,
              label = if (ui.localCamEnabled) "Cam" else "Cam off",
              active = !ui.localCamEnabled,
              onClick = onToggleCamera,
            )
            DesktopCallControlPill(
              icon = DesktopChatSvg.FLIP_CAMERA,
              label = "Flip",
              active = ui.useSecondaryCameraView,
              onClick = onFlipCamera,
            )
            DesktopCallControlPill(
              icon = DesktopChatSvg.SCREEN_SHARE,
              label = if (ui.localScreenSharing) "Stop" else "Share",
              active = ui.localScreenSharing,
              onClick = onToggleScreenShare,
            )
            DesktopCallControlPill(
              icon = DesktopChatSvg.FILTERS,
              label = "Filters",
              active = showFilters,
              onClick = { showFilters = !showFilters },
            )
          } else {
            DesktopCallControlPill(
              icon = DesktopChatSvg.VIDEO_CAM,
              label = "Video",
              active = ui.switchVideoPrompt == DesktopSwitchVideoPromptState.OUTGOING_PENDING,
              onClick = onRequestSwitchToVideo,
            )
          }
          DesktopCallControlPill(
            icon = DesktopChatSvg.SEND_PLANE,
            label = "Message",
            active = false,
            onClick = onOpenChatMessage,
          )
          Surface(
            onClick = onHangUp,
            shape = CircleShape,
            color = Color(0xFFE11D48),
            modifier = Modifier.size(54.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              DesktopChatVectorIcon(DesktopChatSvg.CALL_END, Color.White, 22.dp)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DesktopPeerAvatarStage(
  ui: DesktopActiveCallState,
  apiClient: DesktopApiClient,
) {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier = Modifier
          .size(136.dp)
          .clip(CircleShape)
          .border(2.dp, Color.White.copy(alpha = 0.22f), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        DesktopChatAvatar(ui.peerAvatarUrl, ui.peerName, apiClient, size = 132.dp)
      }
      Text(
        text = ui.peerName,
        color = Color.White,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
      )
      val sub = when {
        ui.phase == DesktopCallPhase.CONNECTED && !ui.peerInAppActive && ui.callType == DesktopCallType.VIDEO ->
          "${ui.peerName} is away · ${DesktopCallMetaParser.formatDuration(ui.connectedElapsedSec)}"
        ui.phase == DesktopCallPhase.CONNECTED ->
          DesktopCallMetaParser.formatDuration(ui.connectedElapsedSec)
        ui.statusText.isNotBlank() -> ui.statusText
        else -> "Calling…"
      }
      Text(
        text = sub,
        color = Color.White.copy(alpha = 0.78f),
        fontSize = 14.5.sp,
        fontWeight = FontWeight.Medium,
      )
    }
  }
}

@Composable
private fun DesktopCallControlPill(
  icon: DesktopChatSvg,
  label: String,
  active: Boolean,
  onClick: () -> Unit,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
    modifier = Modifier
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 4.dp, vertical = 2.dp),
  ) {
    Surface(
      shape = CircleShape,
      color = if (active) Color.White else Color.White.copy(alpha = 0.14f),
      modifier = Modifier.size(48.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        DesktopChatVectorIcon(
          kind = icon,
          tint = if (active) Color(0xFF141820) else Color.White,
          size = 19.dp,
        )
      }
    }
    Text(
      text = label,
      color = Color.White.copy(alpha = 0.85f),
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      maxLines = 1,
    )
  }
}
