package telefam.company.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.telefam.company.app.R
import org.json.JSONObject
import telefam.company.calls.TelefamCallMetaParser
import telefam.company.calls.TelefamCallType
import telefam.company.chat.ViewOnceActiveOverlayState
import telefam.company.chat.VoicePlaybackState
import telefam.company.chat.VoiceRecordingState
import telefam.company.core.SmartTextDetector
import telefam.company.core.TelefamContactsRules
import telefam.company.data.local.ChatConversationEntity
import telefam.company.data.local.ChatMessageEntity
import telefam.company.ui.components.LinkPreviewStack
import telefam.company.ui.components.LocalSmartTextActions
import telefam.company.ui.components.SmartInteractiveText
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamPureWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageRowItem(
  msg: ChatMessageEntity,
  isOwn: Boolean,
  isUnsending: Boolean,
  peerName: String,
  peerAvatarUrl: String,
  outgoingStart: Color,
  outgoingEnd: Color,
  isDark: Boolean,
  canCopy: Boolean,
  canForward: Boolean,
  playback: VoicePlaybackState,
  onCopy: () -> Unit,
  onForward: () -> Unit,
  onEdit: () -> Unit,
  onUnsend: () -> Unit,
  onDeleteForSelf: () -> Unit,
  onTogglePlayVoice: () -> Unit,
  onOpenViewOnce: () -> Unit,
  onCallBack: (TelefamCallType) -> Unit = {},
) {
  var showMenu by remember { mutableStateOf(false) }
  var isExpanded by remember(msg.messageId) { mutableStateOf(false) }

  val dissolveAlpha by animateFloatAsState(
    targetValue = if (isUnsending) 0.08f else 1f,
    animationSpec = tween(420),
    label = "unsend_dissolve",
  )

  val timeLabel = remember(msg.createdAtMs) {
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.createdAtMs))
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 3.dp)
      .alpha(dissolveAlpha)
      .testTag("chat_msg_${msg.messageId}"),
    contentAlignment = if (isOwn) Alignment.CenterEnd else Alignment.CenterStart,
  ) {
    Box {
      when (msg.messageType) {
        "sticker" -> {
          val animUrl = extractGiflyMediaUrl(msg)
          Column(
            horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start,
            modifier = Modifier.combinedClickable(
              onClick = {},
              onLongClick = { showMenu = true },
            ),
          ) {
            AsyncImage(
              model = animUrl,
              contentDescription = "Sticker",
              contentScale = ContentScale.Fit,
              modifier = Modifier
                .size(144.dp)
                .background(Color.Transparent),
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.42f))
                .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
              Text(text = timeLabel, fontSize = 10.5.sp, color = TelefamPureWhite)
              if (isOwn) {
                OutgoingStatusIndicator(
                  status = msg.deliveryStatus,
                  peerName = peerName,
                  peerAvatarUrl = peerAvatarUrl,
                )
              }
            }
          }
        }

        "gif", "clip" -> {
          val animUrl = extractGiflyMediaUrl(msg)
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.Black.copy(alpha = 0.25f),
            modifier = Modifier
              .widthIn(max = 240.dp)
              .combinedClickable(onClick = {}, onLongClick = { showMenu = true }),
          ) {
            Box {
              AsyncImage(
                model = animUrl,
                contentDescription = msg.messageType,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(172.dp),
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                  .align(Alignment.BottomEnd)
                  .padding(8.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(Color.Black.copy(alpha = 0.55f))
                  .padding(horizontal = 7.dp, vertical = 3.dp),
              ) {
                Text(text = timeLabel, fontSize = 10.5.sp, color = TelefamPureWhite)
                if (isOwn) {
                  OutgoingStatusIndicator(msg.deliveryStatus, peerName, peerAvatarUrl)
                }
              }
            }
          }
        }

        "voice_once" -> {
          // Circular View-Once Voice Bubble
          val accentGrad = Brush.linearGradient(listOf(outgoingStart, outgoingEnd))
          val borderBrush = if (isOwn) accentGrad else Brush.linearGradient(listOf(Color(0xFFEF4444), outgoingEnd))
          Column(
            horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start,
            modifier = Modifier
              .combinedClickable(
                onClick = { onOpenViewOnce() },
                onLongClick = { showMenu = true },
              )
              .testTag("voice_once_bubble_${msg.messageId}"),
          ) {
            Box(
              modifier = Modifier
                .size(106.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF1C2027).copy(alpha = 0.94f) else TelefamPureWhite.copy(alpha = 0.95f))
                .border(2.5.dp, borderBrush, CircleShape)
                .padding(10.dp),
              contentAlignment = Alignment.Center,
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  imageVector = Icons.Filled.Timelapse,
                  contentDescription = "View once",
                  tint = if (isOwn) outgoingEnd else TelefamCrimson,
                  modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                  text = if (msg.viewOnceTtlSec > 0) "${msg.viewOnceTtlSec}s Once" else "Once",
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isDark) TelefamPureWhite else Color(0xFF14151A),
                )
                Text(
                  text = formatDurationLabel(msg.voiceDurationMs),
                  fontSize = 10.sp,
                  color = if (isDark) Color(0xFFA0A7B4) else Color(0xFF5F6674),
                )
              }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.40f))
                .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
              Text(text = timeLabel, fontSize = 10.5.sp, color = TelefamPureWhite)
              if (isOwn) {
                OutgoingStatusIndicator(msg.deliveryStatus, peerName, peerAvatarUrl)
              }
            }
          }
        }

        "voice" -> {
          val isPlayingThis = playback.activeMessageId == msg.messageId && playback.isPlaying
          val incomingBg = if (isDark) Color(0xFF21242B).copy(alpha = 0.94f) else TelefamPureWhite.copy(alpha = 0.96f)
          val textColor = if (isOwn) TelefamPureWhite else if (isDark) Color(0xFFF3F4F6) else Color(0xFF16181F)
          val subColor = if (isOwn) TelefamPureWhite.copy(alpha = 0.8f) else if (isDark) Color(0xFFA0A7B4) else Color(0xFF6B7280)
          val bubbleShape = if (isOwn) {
            RoundedCornerShape(18.dp, 18.dp, 5.dp, 18.dp)
          } else {
            RoundedCornerShape(18.dp, 18.dp, 18.dp, 5.dp)
          }
          Box(
            modifier = Modifier
              .widthIn(min = 210.dp, max = 285.dp)
              .clip(bubbleShape)
              .then(
                if (isOwn) Modifier.background(Brush.horizontalGradient(listOf(outgoingStart, outgoingEnd)))
                else Modifier.background(incomingBg),
              )
              .combinedClickable(
                onClick = onTogglePlayVoice,
                onLongClick = { showMenu = true },
              )
              .padding(horizontal = 12.dp, vertical = 10.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(if (isOwn) TelefamPureWhite.copy(alpha = 0.22f) else outgoingEnd.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = if (isPlayingThis) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                  contentDescription = "Play voice note",
                  tint = if (isOwn) TelefamPureWhite else outgoingEnd,
                  modifier = Modifier.size(22.dp),
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                val progress = if (isPlayingThis && playback.durationMs > 0L) {
                  (playback.positionMs.toFloat() / playback.durationMs.toFloat()).coerceIn(0f, 1f)
                } else {
                  0f
                }
                VoiceWaveformBars(progress = progress, activeColor = textColor, inactiveColor = textColor.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = formatDurationLabel(if (isPlayingThis) playback.positionMs else msg.voiceDurationMs),
                    fontSize = 11.sp,
                    color = subColor,
                  )
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = timeLabel, fontSize = 10.5.sp, color = subColor)
                    if (isOwn) {
                      OutgoingStatusIndicator(msg.deliveryStatus, peerName, peerAvatarUrl)
                    }
                  }
                }
              }
            }
          }
        }

        "call_log", "call_request" -> {
          val meta = remember(msg.plainText) { TelefamCallMetaParser.parseCallMeta(msg.plainText) }
          val callType = meta?.callType ?: TelefamCallType.AUDIO
          val isVideoCall = callType == TelefamCallType.VIDEO || (meta?.videoUpgraded == true)
          val outcome = meta?.outcome.orEmpty()
          val isMissedStyle = outcome == "missed" || outcome == "cancelled" || outcome == "declined" || outcome == "busy" || outcome == "not_allowed"
          val isCallRequest = msg.messageType == "call_request" || outcome == "not_allowed"
          val incomingBg = if (isDark) Color(0xFF21242B).copy(alpha = 0.94f) else TelefamPureWhite.copy(alpha = 0.96f)
          val textColor = if (isOwn) TelefamPureWhite else if (isDark) Color(0xFFF3F4F6) else Color(0xFF16181F)
          val subColor = if (isOwn) TelefamPureWhite.copy(alpha = 0.82f) else if (isDark) Color(0xFFA0A7B4) else Color(0xFF6B7280)
          val bubbleShape = if (isOwn) {
            RoundedCornerShape(18.dp, 18.dp, 5.dp, 18.dp)
          } else {
            RoundedCornerShape(18.dp, 18.dp, 18.dp, 5.dp)
          }
          val headline = when {
            isCallRequest -> if (isVideoCall) "Video call request" else "Call me request"
            outcome == "ended" -> if (isVideoCall) "Video call" else "Audio call"
            outcome == "declined" -> if (isVideoCall) "Video call declined" else "Audio call declined"
            outcome == "busy" -> if (isVideoCall) "Video call · Busy" else "Audio call · Busy"
            else -> if (isVideoCall) "Missed video call" else "Missed audio call"
          }
          val detailLine = when {
            outcome == "ended" && (meta?.durationSec ?: 0L) > 0L -> TelefamCallMetaParser.formatDuration(meta!!.durationSec)
            !meta?.text.isNullOrBlank() -> meta!!.text
            isCallRequest -> "Calls not allowed · Please call me"
            isMissedStyle -> stringResource(R.string.call_tried_to_call_you)
            else -> timeLabel
          }

          Box(
            modifier = Modifier
              .widthIn(min = 216.dp, max = 300.dp)
              .clip(bubbleShape)
              .then(
                if (isOwn) Modifier.background(Brush.horizontalGradient(listOf(outgoingStart, outgoingEnd)))
                else Modifier.background(incomingBg),
              )
              .combinedClickable(
                onClick = { onCallBack(callType) },
                onLongClick = { showMenu = true },
              )
              .padding(horizontal = 12.dp, vertical = 10.dp)
              .testTag("chat_call_log_card_${msg.messageId}"),
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                      when {
                        isMissedStyle -> TelefamCrimson.copy(alpha = if (isOwn) 0.35f else 0.14f)
                        isOwn -> TelefamPureWhite.copy(alpha = 0.22f)
                        else -> outgoingEnd.copy(alpha = 0.16f)
                      },
                    ),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = when {
                      isMissedStyle -> Icons.Filled.CallEnd
                      isVideoCall -> Icons.Filled.Videocam
                      else -> Icons.Filled.Call
                    },
                    contentDescription = headline,
                    tint = when {
                      isMissedStyle -> if (isOwn) Color(0xFFFECACA) else TelefamCrimson
                      isOwn -> TelefamPureWhite
                      else -> outgoingEnd
                    },
                    modifier = Modifier.size(20.dp),
                  )
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = headline,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = detailLine,
                    fontSize = 12.sp,
                    color = subColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = if (isOwn) TelefamPureWhite.copy(alpha = 0.20f) else outgoingEnd.copy(alpha = 0.14f),
                  modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onCallBack(callType) }
                    .testTag("chat_call_back_button_${msg.messageId}"),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                  ) {
                    Icon(
                      imageVector = if (isVideoCall) Icons.Filled.Videocam else Icons.Filled.Call,
                      contentDescription = null,
                      tint = if (isOwn) TelefamPureWhite else outgoingEnd,
                      modifier = Modifier.size(14.dp),
                    )
                    Text(
                      text = stringResource(R.string.call_action_call_back),
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = if (isOwn) TelefamPureWhite else outgoingEnd,
                    )
                  }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(text = timeLabel, fontSize = 10.5.sp, color = subColor)
                  if (isOwn) {
                    OutgoingStatusIndicator(msg.deliveryStatus, peerName, peerAvatarUrl)
                  }
                }
              }
            }
          }
        }

        else -> {
          val incomingBg = if (isDark) Color(0xFF21242B).copy(alpha = 0.94f) else TelefamPureWhite.copy(alpha = 0.96f)
          val textColor = if (isOwn) TelefamPureWhite else if (isDark) Color(0xFFF3F4F6) else Color(0xFF16181F)
          val subColor = if (isOwn) TelefamPureWhite.copy(alpha = 0.80f) else if (isDark) Color(0xFFA0A7B4) else Color(0xFF6B7280)
          val bubbleShape = if (isOwn) {
            RoundedCornerShape(18.dp, 18.dp, 5.dp, 18.dp)
          } else {
            RoundedCornerShape(18.dp, 18.dp, 18.dp, 5.dp)
          }
          val lines = msg.plainText.lines()
          val isLongText = msg.plainText.length > 220 || lines.size > 5

          Box(
            modifier = Modifier
              .widthIn(min = 96.dp, max = 310.dp)
              .clip(bubbleShape)
              .then(
                if (isOwn) Modifier.background(Brush.horizontalGradient(listOf(outgoingStart, outgoingEnd)))
                else Modifier.background(incomingBg),
              )
              .combinedClickable(
                onClick = { if (isLongText) isExpanded = !isExpanded },
                onLongClick = { showMenu = true },
              )
              .padding(horizontal = 13.dp, vertical = 9.dp)
              .testTag("chat_msg_bubble_${msg.messageId}"),
          ) {
            Column {
              if (msg.forwardedFromUserId.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.Reply,
                    contentDescription = null,
                    tint = subColor,
                    modifier = Modifier.size(13.dp),
                  )
                  Text(
                    text = "Forwarded",
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    color = subColor,
                  )
                }
                Spacer(modifier = Modifier.height(3.dp))
              }

              AdaptiveDecoratedMessageText(
                rawText = msg.plainText,
                expanded = isExpanded || !isLongText,
                baseColor = textColor,
                accentColor = if (isOwn) TelefamPureWhite else outgoingEnd,
                subtitleBg = if (isOwn) TelefamPureWhite.copy(alpha = 0.14f) else outgoingEnd.copy(alpha = 0.10f),
              )

              val detectedUrls = remember(msg.plainText) {
                SmartTextDetector.analyze(msg.plainText).urls
              }
              if (detectedUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                LinkPreviewStack(
                  urls = detectedUrls,
                  source = "chat",
                )
              }

              if (isLongText) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = if (isExpanded) stringResource(R.string.feed_read_less) else stringResource(R.string.feed_read_more),
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = if (isOwn) TelefamPureWhite else outgoingEnd,
                  modifier = Modifier
                    .clickable { isExpanded = !isExpanded }
                    .testTag("chat_read_more_toggle_${msg.messageId}"),
                )
              }

              Spacer(modifier = Modifier.height(3.dp))
              Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                if (msg.editedAtMs > 0L) {
                  Text(text = "edited", fontSize = 10.sp, fontStyle = FontStyle.Italic, color = subColor)
                }
                Text(text = timeLabel, fontSize = 10.5.sp, color = subColor)
                if (isOwn) {
                  OutgoingStatusIndicator(msg.deliveryStatus, peerName, peerAvatarUrl)
                }
              }
            }
          }
        }
      }

      // Context menu strictly respecting Copy & Forward privacy flags
      DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
      ) {
        if (canCopy && msg.messageType == "text") {
          DropdownMenuItem(
            text = { Text(stringResource(R.string.chat_action_copy)) },
            leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)) },
            onClick = {
              showMenu = false
              onCopy()
            },
            modifier = Modifier.testTag("msg_action_copy"),
          )
        }
        if (canForward && (msg.messageType == "text" || msg.messageType == "sticker" || msg.messageType == "gif" || msg.messageType == "clip")) {
          DropdownMenuItem(
            text = { Text(stringResource(R.string.chat_action_forward)) },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, modifier = Modifier.size(18.dp)) },
            onClick = {
              showMenu = false
              onForward()
            },
            modifier = Modifier.testTag("msg_action_forward"),
          )
        }
        if (isOwn && msg.messageType == "text") {
          DropdownMenuItem(
            text = { Text(stringResource(R.string.chat_action_edit)) },
            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
            onClick = {
              showMenu = false
              onEdit()
            },
            modifier = Modifier.testTag("msg_action_edit"),
          )
        }
        if (isOwn) {
          DropdownMenuItem(
            text = { Text(stringResource(R.string.chat_action_unsend), color = TelefamCrimson) },
            leadingIcon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = TelefamCrimson, modifier = Modifier.size(18.dp)) },
            onClick = {
              showMenu = false
              onUnsend()
            },
            modifier = Modifier.testTag("msg_action_unsend"),
          )
        }
        DropdownMenuItem(
          text = { Text(stringResource(R.string.chat_action_delete)) },
          leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp)) },
          onClick = {
            showMenu = false
            onDeleteForSelf()
          },
          modifier = Modifier.testTag("msg_action_delete"),
        )
      }
    }

    if (isUnsending) {
      SparkleDissolveOverlay(
        modifier = Modifier.matchParentSize(),
      )
    }
  }
}

/**
 * Dynamically adapts font weight, family, and title/subtitle block decoration directly from message structure.
 */
@Composable
private fun AdaptiveDecoratedMessageText(
  rawText: String,
  expanded: Boolean,
  baseColor: Color,
  accentColor: Color,
  subtitleBg: Color,
) {
  val clean = if (!expanded && rawText.length > 220) {
    rawText.take(216).trimEnd() + "..."
  } else {
    rawText
  }
  val lines = clean.split("\n")
  val hasStructuredHeader = lines.size >= 2 && (lines[0].startsWith("# ") || (lines[0].length in 2..48 && lines[1].startsWith("> ")))

  if (hasStructuredHeader) {
    val titleLine = lines[0].removePrefix("# ").trim()
    val hasSubtitleQuote = lines[1].startsWith("> ")
    val subtitleLine = if (hasSubtitleQuote) lines[1].removePrefix("> ").trim() else ""
    val bodyText = lines.drop(if (hasSubtitleQuote) 2 else 1).joinToString("\n").trim()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = titleLine,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = baseColor,
      )
      if (subtitleLine.isNotBlank()) {
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(subtitleBg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier = Modifier
              .width(3.dp)
              .height(16.dp)
              .clip(RoundedCornerShape(2.dp))
              .background(accentColor),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = subtitleLine,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            fontStyle = FontStyle.Italic,
            color = baseColor.copy(alpha = 0.9f),
          )
        }
      }
      if (bodyText.isNotBlank()) {
        SmartInteractiveText(
          text = bodyText,
          source = "chat",
          style = androidx.compose.ui.text.TextStyle(
            fontSize = 14.5.sp,
            lineHeight = 20.sp,
          ),
          color = baseColor,
          linkColor = accentColor,
          otpHighlightBg = accentColor.copy(alpha = 0.18f),
        )
      }
    }
  } else {
    val adaptiveFamily = when {
      clean.startsWith("```") -> FontFamily.Monospace
      clean.startsWith("\"") && clean.endsWith("\"") -> FontFamily.Serif
      else -> FontFamily.Default
    }
    SmartInteractiveText(
      text = clean.removeSurrounding("```").trim(),
      source = "chat",
      style = androidx.compose.ui.text.TextStyle(
        fontSize = 14.5.sp,
        lineHeight = 20.sp,
        fontFamily = adaptiveFamily,
      ),
      color = baseColor,
      linkColor = accentColor,
      otpHighlightBg = accentColor.copy(alpha = 0.18f),
    )
  }
}

/**
 * Outgoing delivery status indicator:
 * - sending: animated ticking clock
 * - sent: crisp single tick
 * - delivered: small red icon with white checkmark
 * - seen: small circular receiver avatar
 */
@Composable
fun OutgoingStatusIndicator(
  status: String,
  peerName: String,
  peerAvatarUrl: String,
) {
  when (status.lowercase()) {
    "sending" -> {
      val inf = rememberInfiniteTransition(label = "sending_clock")
      val angle by inf.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "clock_hand",
      )
      Canvas(
        modifier = Modifier
          .size(13.dp)
          .testTag("status_sending_clock"),
      ) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f - 1.dp.toPx()
        drawCircle(TelefamPureWhite.copy(alpha = 0.88f), r, c, style = Stroke(1.3.dp.toPx()))
        val rad = (angle - 90f) * (PI / 180.0)
        drawLine(
          color = TelefamPureWhite,
          start = c,
          end = Offset(c.x + (cos(rad) * r * 0.65f).toFloat(), c.y + (sin(rad) * r * 0.65f).toFloat()),
          strokeWidth = 1.4.dp.toPx(),
          cap = StrokeCap.Round,
        )
      }
    }

    "delivered" -> {
      // Small red icon with checkmark per user specification
      Box(
        modifier = Modifier
          .size(14.dp)
          .clip(CircleShape)
          .background(TelefamCrimson)
          .testTag("status_delivered_red_check"),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Filled.Check,
          contentDescription = "Delivered",
          tint = TelefamPureWhite,
          modifier = Modifier.size(9.dp),
        )
      }
    }

    "seen" -> {
      // Small circular receiver avatar
      Box(
        modifier = Modifier
          .size(15.dp)
          .clip(CircleShape)
          .background(Color(0xFF334155))
          .border(0.8.dp, TelefamPureWhite, CircleShape)
          .testTag("status_seen_receiver_avatar"),
        contentAlignment = Alignment.Center,
      ) {
        if (peerAvatarUrl.isNotBlank()) {
          AsyncImage(
            model = peerAvatarUrl,
            contentDescription = "Seen by $peerName",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Text(
            text = TelefamContactsRules.initialsFor(peerName, peerName).take(1),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = TelefamPureWhite,
          )
        }
      }
    }

    else -> {
      Icon(
        imageVector = Icons.Filled.Check,
        contentDescription = "Sent",
        tint = TelefamPureWhite.copy(alpha = 0.90f),
        modifier = Modifier
          .size(13.dp)
          .testTag("status_sent_tick"),
      )
    }
  }
}

@Composable
private fun SparkleDissolveOverlay(modifier: Modifier = Modifier) {
  val inf = rememberInfiniteTransition(label = "unsend_sparkle")
  val progress by inf.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(460, easing = LinearEasing)),
    label = "sparkle_p",
  )
  Canvas(modifier = modifier) {
    val stars = 10
    for (i in 0 until stars) {
      val angle = (i * (2.0 * PI / stars))
      val dist = size.minDimension * 0.45f * progress
      val cx = size.width * (0.2f + 0.6f * ((i % 4) / 3f)) + (cos(angle) * dist).toFloat()
      val cy = size.height * 0.5f + (sin(angle) * dist).toFloat()
      val r = (1f - progress) * 5.dp.toPx() + 2.dp.toPx()
      val p = Path().apply {
        moveTo(cx, cy - r)
        lineTo(cx + r * 0.28f, cy - r * 0.28f)
        lineTo(cx + r, cy)
        lineTo(cx + r * 0.28f, cy + r * 0.28f)
        lineTo(cx, cy + r)
        lineTo(cx - r * 0.28f, cy + r * 0.28f)
        lineTo(cx - r, cy)
        lineTo(cx - r * 0.28f, cy - r * 0.28f)
        close()
      }
      drawPath(p, color = Color(0xFFFDE68A).copy(alpha = (1f - progress).coerceIn(0f, 1f)))
    }
  }
}

@Composable
private fun VoiceWaveformBars(progress: Float, activeColor: Color, inactiveColor: Color) {
  val heights = remember { floatArrayOf(0.35f, 0.7f, 0.45f, 0.9f, 0.6f, 0.85f, 0.4f, 0.75f, 0.5f, 0.95f, 0.65f, 0.42f, 0.8f, 0.55f, 0.72f, 0.38f) }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(22.dp),
    horizontalArrangement = Arrangement.spacedBy(3.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    heights.forEachIndexed { idx, ratio ->
      val filled = (idx.toFloat() / heights.size.toFloat()) <= progress
      Box(
        modifier = Modifier
          .weight(1f)
          .height((20f * ratio).dp.coerceAtLeast(4.dp))
          .clip(RoundedCornerShape(2.dp))
          .background(if (filled) activeColor else inactiveColor),
      )
    }
  }
}

@Composable
fun VoiceRecorderAndPreviewBar(
  recording: VoiceRecordingState,
  playback: VoicePlaybackState,
  viewOnce: Boolean,
  ttlSec: Int,
  accentColor: Color,
  isDark: Boolean,
  onStopRecording: () -> Unit,
  onDiscard: () -> Unit,
  onTogglePreviewPlay: () -> Unit,
  onToggleViewOnce: (Boolean) -> Unit,
  onSelectTtlSec: (Int) -> Unit,
  onSendVoiceNote: () -> Unit,
) {
  val barBg = if (isDark) Color(0xFF1A1E26) else TelefamPureWhite
  val textColor = if (isDark) TelefamPureWhite else Color(0xFF14151A)
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 10.dp, vertical = 6.dp)
      .testTag("voice_recorder_bar"),
    shape = RoundedCornerShape(22.dp),
    color = barBg,
    shadowElevation = 6.dp,
  ) {
    if (recording.isRecording) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = TelefamCrimson, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Recording  ${formatDurationLabel(recording.elapsedMs)}",
          fontWeight = FontWeight.SemiBold,
          color = textColor,
          modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDiscard, modifier = Modifier.size(40.dp)) {
          Icon(Icons.Filled.DeleteOutline, contentDescription = "Discard", tint = TelefamCrimson)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Button(
          onClick = onStopRecording,
          shape = RoundedCornerShape(18.dp),
          colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = TelefamPureWhite),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.testTag("voice_stop_button"),
        ) {
          Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Done", fontSize = 12.5.sp)
        }
      }
    } else {
      val isPlayingPreview = playback.activeMessageId == "preview_voice" && playback.isPlaying
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onTogglePreviewPlay, modifier = Modifier.size(38.dp)) {
            Icon(
              imageVector = if (isPlayingPreview) Icons.Filled.Pause else Icons.Filled.PlayArrow,
              contentDescription = "Preview",
              tint = accentColor,
            )
          }
          Text(
            text = formatDurationLabel(recording.recordedDurationMs),
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.weight(1f),
          )
          IconButton(onClick = onDiscard, modifier = Modifier.size(38.dp)) {
            Icon(Icons.Filled.DeleteOutline, contentDescription = "Discard", tint = TelefamCrimson)
          }
          Spacer(modifier = Modifier.width(6.dp))
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(accentColor)
              .clickable(onClick = onSendVoiceNote)
              .testTag("voice_send_button"),
            contentAlignment = Alignment.Center,
          ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send voice", tint = TelefamPureWhite, modifier = Modifier.size(19.dp))
          }
        }

        // View-Once Toggle + Expiry Timer Selector
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Timelapse, contentDescription = null, tint = accentColor, modifier = Modifier.size(17.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = stringResource(R.string.chat_voice_view_once), fontSize = 13.sp, color = textColor)
          }
          Switch(
            checked = viewOnce,
            onCheckedChange = onToggleViewOnce,
            colors = SwitchDefaults.colors(checkedTrackColor = accentColor),
            modifier = Modifier.testTag("voice_view_once_switch"),
          )
        }
        if (viewOnce) {
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0 to stringResource(R.string.chat_voice_play_once), 5 to "5s", 10 to "10s", 30 to "30s").forEach { (sec, lbl) ->
              val sel = ttlSec == sec
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (sel) accentColor else accentColor.copy(alpha = 0.14f),
                modifier = Modifier
                  .clickable { onSelectTtlSec(sec) }
                  .testTag("voice_ttl_$sec"),
              ) {
                Text(
                  text = lbl,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = if (sel) TelefamPureWhite else accentColor,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun ViewOnceVoicePlayerOverlay(
  overlay: ViewOnceActiveOverlayState,
  onBurnAndClose: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xF0090B10))
      .testTag("view_once_overlay"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .padding(28.dp)
        .widthIn(max = 320.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(Icons.Filled.VisibilityOff, contentDescription = null, tint = TelefamCrimson, modifier = Modifier.size(28.dp))
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "View-Once Voice Note",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = TelefamPureWhite,
      )
      Spacer(modifier = Modifier.height(20.dp))
      Box(
        modifier = Modifier
          .size(164.dp)
          .clip(CircleShape)
          .background(Color(0xFF171B24))
          .border(3.5.dp, Brush.sweepGradient(listOf(TelefamCrimson, Color(0xFF38BDF8), TelefamCrimson)), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = TelefamPureWhite, modifier = Modifier.size(44.dp))
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "${overlay.remainingSec}s",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TelefamPureWhite,
          )
        }
      }
      Spacer(modifier = Modifier.height(14.dp))
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Filled.Shield, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(15.dp))
        Text(
          text = "Auto-deletes immediately when finished or closed",
          fontSize = 12.sp,
          color = Color(0xFFCBD5E1),
        )
      }
      Spacer(modifier = Modifier.height(22.dp))
      Button(
        onClick = onBurnAndClose,
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TelefamCrimson, contentColor = TelefamPureWhite),
        modifier = Modifier
          .fillMaxWidth()
          .height(46.dp)
          .testTag("view_once_close_burn"),
      ) {
        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Close & Burn Now", fontWeight = FontWeight.SemiBold)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSharedMediaBottomSheet(
  tab: String,
  messages: List<ChatMessageEntity>,
  onSelectTab: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val smartActions = LocalSmartTextActions.current
  val mediaItems = remember(messages) {
    messages.filter { it.messageType == "sticker" || it.messageType == "gif" || it.messageType == "clip" || it.messageType == "voice" }
  }
  val extractedLinks = remember(messages) {
    messages
      .asSequence()
      .filter { it.messageType == "text" }
      .flatMap { SmartTextDetector.analyze(it.plainText).urls.asSequence() }
      .distinctBy { it.normalizedUrl }
      .toList()
  }
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 28.dp),
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
          "media" to stringResource(R.string.chat_action_media),
          "links" to stringResource(R.string.chat_action_links),
          "files" to stringResource(R.string.chat_action_files_docs),
        ).forEach { (id, title) ->
          val sel = tab == id
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (sel) TelefamCrimson else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.clickable { onSelectTab(id) },
          ) {
            Text(
              text = title,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              color = if (sel) TelefamPureWhite else MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(14.dp))
      when (tab) {
        "media" -> {
          if (mediaItems.isEmpty()) {
            Text("No shared media yet", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp))
          } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(260.dp)) {
              items(mediaItems, key = { it.messageId }) { m ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                  Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = m.messageType.uppercase() + ": " + m.plainText.take(40),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }
        "links" -> {
          if (extractedLinks.isEmpty()) {
            Text("No shared links yet", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp))
          } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(260.dp)) {
              items(extractedLinks, key = { it.normalizedUrl }) { urlItem ->
                LinkPreviewStack(
                  urls = listOf(urlItem),
                  source = "chat",
                )
              }
            }
          }
        }
        else -> {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 20.dp)) {
            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("No files & docs shared yet. File sending is coming soon.", color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardConversationPickerSheet(
  conversations: List<ChatConversationEntity>,
  onSelectTarget: (ChatConversationEntity) -> Unit,
  onDismiss: () -> Unit,
) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 26.dp),
    ) {
      Text(
        text = stringResource(R.string.chat_forward_title),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      )
      Spacer(modifier = Modifier.height(10.dp))
      LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.height(280.dp)) {
        items(conversations, key = { it.conversationId }) { conv ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .clickable { onSelectTarget(conv) }
              .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(TelefamCrimson),
              contentAlignment = Alignment.Center,
            ) {
              if (conv.peerAvatarUrl.isNotBlank()) {
                AsyncImage(
                  model = conv.peerAvatarUrl,
                  contentDescription = conv.peerFullName,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
              } else {
                Text(
                  text = TelefamContactsRules.initialsFor(conv.peerFullName, conv.peerUsername),
                  color = TelefamPureWhite,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(text = conv.peerFullName.ifBlank { conv.peerUsername }, fontWeight = FontWeight.SemiBold)
              if (conv.peerUsername.isNotBlank()) {
                Text(text = "@${conv.peerUsername}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }
    }
  }
}

fun extractGiflyMediaUrl(msg: ChatMessageEntity): String {
  if (msg.mediaUrl.isNotBlank()) return msg.mediaUrl
  return runCatching { JSONObject(msg.plainText).optString("url") }.getOrDefault("")
}

fun formatDurationLabel(ms: Long): String {
  val totalSec = (ms / 1000L).coerceAtLeast(0L)
  val m = totalSec / 60L
  val s = totalSec % 60L
  return String.format(Locale.US, "%d:%02d", m, s)
}
