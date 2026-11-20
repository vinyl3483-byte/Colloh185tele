package telefam.company.calls

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

enum class TelefamCallType(val wireValue: String) {
  AUDIO("audio"),
  VIDEO("video");

  companion object {
    fun fromWire(value: String?): TelefamCallType =
      if (value.equals("video", ignoreCase = true)) VIDEO else AUDIO
  }
}

enum class TelefamCallPhase {
  IDLE,
  OUTGOING_CALLING,
  OUTGOING_RINGING,
  INCOMING_RINGING,
  CONNECTING,
  CONNECTED,
  ENDED,
  NOT_ALLOWED,
}

enum class VideoCallFilter(val wireValue: String, val label: String) {
  NATURAL("natural", "Natural"),
  WARM("warm", "Warm"),
  COOL("cool", "Cool"),
  NOIR("noir", "B&W"),
  VIVID("vivid", "Vivid"),
  SOFT("soft", "Studio"),
}

enum class CallQualityTier(
  val wireValue: String,
  val label: String,
  val maxBitrateBps: Int,
  val targetWidth: Int,
  val targetHeight: Int,
  val targetFps: Int,
) {
  AUTO("auto", "Auto", 1_400_000, 1280, 720, 30),
  HD_720P("hd_720p", "HD 720p", 1_600_000, 1280, 720, 30),
  BALANCED_480P("balanced_480p", "480p", 750_000, 854, 480, 24),
  LOW_240P("low_240p", "Data Saver", 260_000, 426, 240, 15),
  AUDIO_PRIORITY("audio_priority", "Audio Priority", 140_000, 320, 180, 12);
}

enum class SwitchVideoPromptState {
  NONE,
  OUTGOING_PENDING,
  INCOMING_REQUEST,
}

data class ActiveCallUiState(
  val phase: TelefamCallPhase = TelefamCallPhase.IDLE,
  val callId: String = "",
  val conversationId: String = "",
  val peerUserId: String = "",
  val peerName: String = "",
  val peerUsername: String = "",
  val peerAvatarUrl: String = "",
  val callType: TelefamCallType = TelefamCallType.AUDIO,
  val isIncoming: Boolean = false,
  val isMinimizedToChat: Boolean = false,
  val statusText: String = "",
  val connectedElapsedSec: Long = 0L,
  val remainingRingSec: Int = 25,
  val micEnabled: Boolean = true,
  val speakerEnabled: Boolean = false,
  val localCamEnabled: Boolean = false,
  val localInAppActive: Boolean = true,
  val localScreenSharing: Boolean = false,
  val peerCamEnabled: Boolean = false,
  val peerInAppActive: Boolean = true,
  val peerScreenSharing: Boolean = false,
  val peerMicEnabled: Boolean = true,
  val videoFilter: VideoCallFilter = VideoCallFilter.NATURAL,
  val selectedQualityMode: CallQualityTier = CallQualityTier.AUTO,
  val effectiveQualityTier: CallQualityTier = CallQualityTier.HD_720P,
  val lowBandwidthWarning: Boolean = false,
  val switchVideoPrompt: SwitchVideoPromptState = SwitchVideoPromptState.NONE,
  val endReason: String = "",
  val errorNotice: String = "",
  val autoCallMeRequestSent: Boolean = false,
) {
  val isSessionVisible: Boolean
    get() = phase != TelefamCallPhase.IDLE

  val shouldShowPeerAvatar: Boolean
    get() = callType == TelefamCallType.AUDIO || (!peerScreenSharing && (!peerCamEnabled || !peerInAppActive))

  val shouldShowLocalVideoPreview: Boolean
    get() = callType == TelefamCallType.VIDEO && (localScreenSharing || (localCamEnabled && localInAppActive))
}

data class ParsedCallLogMeta(
  val callId: String,
  val callType: TelefamCallType,
  val outcome: String,
  val endReason: String,
  val callerUserId: String,
  val calleeUserId: String,
  val durationSec: Long,
  val videoUpgraded: Boolean,
  val text: String,
  val timestampMs: Long,
)

object TelefamCallMetaParser {
  fun parseCallMeta(rawJson: String): ParsedCallLogMeta? {
    if (!rawJson.trimStart().startsWith("{")) return null
    val obj = runCatching { JSONObject(rawJson) }.getOrNull() ?: return null
    val callId = obj.optString("call_id")
    val callType = TelefamCallType.fromWire(obj.optString("call_type"))
    val outcome = obj.optString("outcome")
    val endReason = obj.optString("end_reason")
    val callerUserId = obj.optString("caller_user_id")
    val calleeUserId = obj.optString("callee_user_id")
    val durationSec = obj.optLong("duration_sec", 0L).coerceAtLeast(0L)
    val videoUpgraded = obj.optBoolean("video_upgraded", false)
    val text = obj.optString("text")
    val ts = obj.optLong("timestamp_ms", 0L)
    if (callId.isBlank() && text.isBlank()) return null
    return ParsedCallLogMeta(
      callId = callId,
      callType = callType,
      outcome = outcome,
      endReason = endReason,
      callerUserId = callerUserId,
      calleeUserId = calleeUserId,
      durationSec = durationSec,
      videoUpgraded = videoUpgraded,
      text = text,
      timestampMs = ts,
    )
  }

  fun formatSummaryLine(messageType: String, rawJson: String): String {
    val meta = parseCallMeta(rawJson) ?: return if (messageType == "call_request") "Call request" else "Call"
    val kindLabel = if (meta.callType == TelefamCallType.VIDEO || meta.videoUpgraded) "Video call" else "Audio call"
    if (messageType == "call_request") {
      return "Call me request"
    }
    return when (meta.outcome) {
      "ended" -> if (meta.durationSec > 0L) "$kindLabel · ${formatDuration(meta.durationSec)}" else kindLabel
      "missed", "cancelled" -> "Missed ${kindLabel.lowercase()} · I tried to call you"
      "declined" -> "$kindLabel declined"
      "busy" -> "$kindLabel · Busy"
      "not_allowed" -> "Calls not allowed"
      else -> meta.text.ifBlank { kindLabel }
    }
  }

  fun formatDuration(totalSec: Long): String {
    val safe = totalSec.coerceAtLeast(0L)
    val h = safe / 3600L
    val m = (safe % 3600L) / 60L
    val s = safe % 60L
    return if (h > 0L) {
      String.format("%d:%02d:%02d", h, m, s)
    } else {
      String.format("%02d:%02d", m, s)
    }
  }
}

data class IncomingCallPushPayload(
  val callId: String,
  val conversationId: String,
  val callType: TelefamCallType,
  val callerUserId: String,
  val callerName: String,
  val callerUsername: String,
  val callerAvatarUrl: String,
  val expiresAtMs: Long,
  val autoAccept: Boolean = false,
)

sealed interface CallBridgeCommand {
  data class IncomingPush(val payload: IncomingCallPushPayload) : CallBridgeCommand
  data class CancelledPush(val callId: String, val reason: String) : CallBridgeCommand
  data class HangupFromNotification(val callId: String) : CallBridgeCommand
}

object IncomingCallPushBridge {
  private val _commands = MutableSharedFlow<CallBridgeCommand>(extraBufferCapacity = 32)
  val commands: SharedFlow<CallBridgeCommand> = _commands.asSharedFlow()

  private val _pendingIncoming = MutableStateFlow<IncomingCallPushPayload?>(null)
  val pendingIncoming: StateFlow<IncomingCallPushPayload?> = _pendingIncoming.asStateFlow()

  fun emitIncoming(payload: IncomingCallPushPayload) {
    _pendingIncoming.value = payload
    _commands.tryEmit(CallBridgeCommand.IncomingPush(payload))
  }

  fun emitCancelled(callId: String, reason: String = "") {
    if (_pendingIncoming.value?.callId == callId) {
      _pendingIncoming.value = null
    }
    _commands.tryEmit(CallBridgeCommand.CancelledPush(callId, reason))
  }

  fun emitHangup(callId: String) {
    _commands.tryEmit(CallBridgeCommand.HangupFromNotification(callId))
  }

  fun clearPending(callId: String? = null) {
    if (callId == null || _pendingIncoming.value?.callId == callId) {
      _pendingIncoming.value = null
    }
  }
}
