package telefam.company.calls

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.PeerConnection
import org.webrtc.SurfaceViewRenderer
import telefam.company.core.NetworkTier
import telefam.company.data.network.BufferedSignalEventDto
import telefam.company.data.network.CallRecordDto
import telefam.company.data.network.CallSignalRequestDto
import telefam.company.data.network.IceServerDto
import telefam.company.data.network.NetworkQualityMonitor
import telefam.company.data.repository.TelefamChatRepository
import telefam.company.notifications.TelefamNotificationManager

/**
 * Coordinates end-to-end WhatsApp-style P2P Voice & Video Calls:
 * - Privacy enforcement ("Calls not allowed" + auto "Call me" request in chat)
 * - Real-time Calling -> Ringing presence updates & 25-second timeout
 * - Adaptive quality from NetworkQualityMonitor + WebRTC getStats() + ICE restarts
 * - Live video filters, camera flip, real MediaProjection screen sharing
 * - Auto-avatar switch when leaving app or opening chat ("Message" button) and instant video restore on return
 * - Audio -> Video upgrade negotiation
 * - Call duration computed & persisted strictly on backend, logged inside individual chat only
 */
class TelefamCallController(
  private val appContext: Context,
  private val chatRepository: TelefamChatRepository,
  private val networkMonitor: NetworkQualityMonitor,
) : WebRtcSignalingCallbacks {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val soundEngine = TelefamCallSoundEngine(appContext)
  private val webRtcEngine = WebRtcCallEngine(appContext, this)

  private val _uiState = MutableStateFlow(ActiveCallUiState())
  val uiState: StateFlow<ActiveCallUiState> = _uiState.asStateFlow()

  private val _callConversationUpdated = MutableSharedFlow<String>(extraBufferCapacity = 16)
  val callConversationUpdated: SharedFlow<String> = _callConversationUpdated.asSharedFlow()

  private var pollJob: Job? = null
  private var tickerJob: Job? = null
  private var statsJob: Job? = null
  private var ringTimeoutJob: Job? = null
  private var lastSignalSeq: Long = 0L
  private var cachedIceServers: List<IceServerDto> = emptyList()
  private var pendingRemoteOfferSdp: String = ""
  private var answeredEpochMs: Long = 0L
  private var isOriginatingCall = false
  private var desiredCamEnabledByUser = true
  private var isAppInForeground = true

  init {
    scope.launch {
      chatRepository.wsEvents.collect { ev ->
        handleWsCallEvent(ev.event, ev.rawJson)
      }
    }
    scope.launch {
      IncomingCallPushBridge.commands.collect { cmd ->
        when (cmd) {
          is CallBridgeCommand.IncomingPush -> handleIncomingCallPush(cmd.payload)
          is CallBridgeCommand.CancelledPush -> handleRemoteCallCancelled(cmd.callId, cmd.reason)
          is CallBridgeCommand.HangupFromNotification -> {
            if (_uiState.value.callId == cmd.callId || cmd.callId.isBlank()) {
              hangUp("hangup_from_notification")
            }
          }
        }
      }
    }
    scope.launch {
      networkMonitor.networkTier.collect { tier ->
        if (_uiState.value.phase == TelefamCallPhase.CONNECTED || _uiState.value.phase == TelefamCallPhase.CONNECTING) {
          applyAutoQualityForNetwork(tier)
        }
      }
    }
  }

  fun recommendedQualityForNetwork(tier: NetworkTier): CallQualityTier = when (tier) {
    NetworkTier.GOOD -> CallQualityTier.HD_720P
    NetworkTier.WEAK -> CallQualityTier.LOW_240P
    NetworkTier.OFFLINE -> CallQualityTier.AUDIO_PRIORITY
  }

  val eglBaseContext: org.webrtc.EglBase.Context?
    get() {
      webRtcEngine.initEngine()
      return webRtcEngine.eglBase?.eglBaseContext
    }

  fun attachLocalRenderer(renderer: SurfaceViewRenderer?) {
    webRtcEngine.attachLocalRenderer(renderer)
  }

  fun attachRemoteRenderer(renderer: SurfaceViewRenderer?) {
    webRtcEngine.attachRemoteRenderer(renderer)
  }

  fun startCall(
    peerUserId: String,
    peerName: String,
    peerUsername: String,
    peerAvatarUrl: String,
    conversationId: String,
    callType: TelefamCallType,
  ) {
    if (peerUserId.isBlank()) return
    val current = _uiState.value
    if (current.phase != TelefamCallPhase.IDLE &&
      current.phase != TelefamCallPhase.ENDED &&
      current.phase != TelefamCallPhase.NOT_ALLOWED
    ) {
      return
    }

    isOriginatingCall = true
    desiredCamEnabledByUser = callType == TelefamCallType.VIDEO
    lastSignalSeq = 0L
    pendingRemoteOfferSdp = ""
    answeredEpochMs = 0L

    val initialQuality = recommendedQualityForNetwork(networkMonitor.networkTier.value)
    _uiState.value = ActiveCallUiState(
      phase = TelefamCallPhase.OUTGOING_CALLING,
      conversationId = conversationId,
      peerUserId = peerUserId,
      peerName = peerName.ifBlank { peerUsername.ifBlank { "Telefam" } },
      peerUsername = peerUsername,
      peerAvatarUrl = chatRepository.resolveCallAvatarUrl(peerAvatarUrl),
      callType = callType,
      isIncoming = false,
      statusText = "Calling…",
      remainingRingSec = 25,
      micEnabled = true,
      speakerEnabled = callType == TelefamCallType.VIDEO,
      localCamEnabled = desiredCamEnabledByUser,
      localInAppActive = true,
      peerCamEnabled = callType == TelefamCallType.VIDEO,
      peerInAppActive = true,
      effectiveQualityTier = initialQuality,
      lowBandwidthWarning = networkMonitor.networkTier.value == NetworkTier.WEAK,
    )

    soundEngine.configureAudioRouting(callType == TelefamCallType.VIDEO, callType == TelefamCallType.VIDEO)
    soundEngine.startOutgoingCallingTone()

    scope.launch {
      val iceResp = chatRepository.fetchIceConfig()
      if (iceResp?.iceServers?.isNotEmpty() == true) {
        cachedIceServers = iceResp.iceServers
      }
      val startResp = chatRepository.startCall(
        peerUserId = peerUserId,
        conversationId = conversationId,
        callType = callType.wireValue,
      )

      if (startResp == null) {
        finishCallLocally("network_error", "Connection unavailable")
        return@launch
      }

      if (!startResp.allowed || startResp.code == "calls_not_allowed") {
        soundEngine.playBusyOrRestrictedCue()
        _uiState.update {
          it.copy(
            phase = TelefamCallPhase.NOT_ALLOWED,
            callId = startResp.call.callId,
            conversationId = startResp.call.conversationId.ifBlank { it.conversationId },
            statusText = "Calls not allowed",
            endReason = "calls_not_allowed",
            errorNotice = startResp.message.ifBlank {
              "Calls are disabled by privacy settings. A call request was sent."
            },
            autoCallMeRequestSent = startResp.autoRequestSent,
          )
        }
        val resolvedConvId = startResp.call.conversationId.ifBlank { conversationId }
        if (resolvedConvId.isNotBlank()) {
          chatRepository.loadConversationMessages(resolvedConvId, markAsRead = false)
          _callConversationUpdated.tryEmit(resolvedConvId)
        }
        scheduleAutoResetToIdle(3_200L)
        return@launch
      }

      if (startResp.code == "peer_busy" || startResp.call.state == "busy") {
        soundEngine.playBusyOrRestrictedCue()
        _uiState.update {
          it.copy(
            phase = TelefamCallPhase.ENDED,
            callId = startResp.call.callId,
            conversationId = startResp.call.conversationId.ifBlank { it.conversationId },
            statusText = "Busy",
            endReason = "peer_busy",
          )
        }
        val resolvedConvId = startResp.call.conversationId.ifBlank { conversationId }
        if (resolvedConvId.isNotBlank()) {
          chatRepository.loadConversationMessages(resolvedConvId, markAsRead = false)
          _callConversationUpdated.tryEmit(resolvedConvId)
        }
        scheduleAutoResetToIdle(2_200L)
        return@launch
      }

      if (startResp.iceServers.isNotEmpty()) {
        cachedIceServers = startResp.iceServers
      }
      val record = startResp.call
      val isRingingNow = startResp.peerOnline || record.state == "ringing"
      _uiState.update {
        it.copy(
          phase = if (isRingingNow) TelefamCallPhase.OUTGOING_RINGING else TelefamCallPhase.OUTGOING_CALLING,
          callId = record.callId,
          conversationId = record.conversationId.ifBlank { it.conversationId },
          statusText = if (isRingingNow) "Ringing…" else "Calling…",
          remainingRingSec = startResp.timeoutSec.coerceIn(5, 60),
        )
      }
      if (isRingingNow) {
        soundEngine.startOutgoingRingingTone()
      }

      TelefamCallForegroundService.startOrUpdate(
        context = appContext,
        callId = record.callId,
        conversationId = record.conversationId,
        peerUserId = peerUserId,
        peerName = _uiState.value.peerName,
        isVideo = callType == TelefamCallType.VIDEO,
        isScreenSharing = false,
      )

      webRtcEngine.startSession(
        iceServers = cachedIceServers,
        enableVideo = callType == TelefamCallType.VIDEO,
        quality = initialQuality,
      )
      webRtcEngine.createOffer(iceRestart = false)
      startRingCountdownAndPollers(record.callId, timeoutSec = startResp.timeoutSec.coerceIn(5, 60))
    }
  }

  fun handleIncomingCallPush(payload: IncomingCallPushPayload) {
    if (payload.callId.isBlank()) return
    val current = _uiState.value
    if (current.callId == payload.callId && current.phase != TelefamCallPhase.IDLE) {
      if (payload.autoAccept && current.phase == TelefamCallPhase.INCOMING_RINGING) {
        acceptIncomingCall()
      }
      return
    }
    if (current.phase != TelefamCallPhase.IDLE &&
      current.phase != TelefamCallPhase.ENDED &&
      current.phase != TelefamCallPhase.NOT_ALLOWED
    ) {
      return
    }

    isOriginatingCall = false
    desiredCamEnabledByUser = payload.callType == TelefamCallType.VIDEO
    lastSignalSeq = 0L
    pendingRemoteOfferSdp = ""
    answeredEpochMs = 0L

    val nowMs = System.currentTimeMillis()
    val remain = if (payload.expiresAtMs > nowMs) {
      ((payload.expiresAtMs - nowMs) / 1000L).toInt().coerceIn(5, 25)
    } else {
      25
    }

    _uiState.value = ActiveCallUiState(
      phase = TelefamCallPhase.INCOMING_RINGING,
      callId = payload.callId,
      conversationId = payload.conversationId,
      peerUserId = payload.callerUserId,
      peerName = payload.callerName.ifBlank { payload.callerUsername.ifBlank { "Telefam" } },
      peerUsername = payload.callerUsername,
      peerAvatarUrl = chatRepository.resolveCallAvatarUrl(payload.callerAvatarUrl),
      callType = payload.callType,
      isIncoming = true,
      statusText = if (payload.callType == TelefamCallType.VIDEO) "Incoming video call" else "Incoming audio call",
      remainingRingSec = remain,
      micEnabled = true,
      speakerEnabled = payload.callType == TelefamCallType.VIDEO,
      localCamEnabled = desiredCamEnabledByUser,
      localInAppActive = true,
      peerCamEnabled = payload.callType == TelefamCallType.VIDEO,
      peerInAppActive = true,
      effectiveQualityTier = recommendedQualityForNetwork(networkMonitor.networkTier.value),
    )

    if (payload.autoAccept) {
      acceptIncomingCall()
    } else {
      soundEngine.startIncomingRingtone()
      scope.launch {
        sendSignalBothChannels(payload.callId, CallSignalRequestDto(action = "ringing"))
        startRingCountdownAndPollers(payload.callId, remain)
      }
    }
  }

  fun acceptIncomingCall() {
    val current = _uiState.value
    if (current.callId.isBlank() || current.phase != TelefamCallPhase.INCOMING_RINGING) return

    TelefamNotificationManager.cancelCallNotification(appContext, current.callId)
    IncomingCallPushBridge.clearPending(current.callId)
    ringTimeoutJob?.cancel()
    soundEngine.stopLoopingTones()
    soundEngine.configureAudioRouting(
      isVideoCall = current.callType == TelefamCallType.VIDEO,
      speakerEnabled = current.speakerEnabled,
    )

    val quality = recommendedQualityForNetwork(networkMonitor.networkTier.value)
    _uiState.update {
      it.copy(
        phase = TelefamCallPhase.CONNECTING,
        statusText = "Connecting…",
        effectiveQualityTier = quality,
      )
    }

    TelefamCallForegroundService.startOrUpdate(
      context = appContext,
      callId = current.callId,
      conversationId = current.conversationId,
      peerUserId = current.peerUserId,
      peerName = current.peerName,
      isVideo = current.callType == TelefamCallType.VIDEO,
      isScreenSharing = false,
    )

    scope.launch {
      if (cachedIceServers.isEmpty()) {
        cachedIceServers = chatRepository.fetchIceConfig()?.iceServers.orEmpty()
      }
      webRtcEngine.startSession(
        iceServers = cachedIceServers,
        enableVideo = current.callType == TelefamCallType.VIDEO,
        quality = quality,
      )
      val stateResp = chatRepository.getCallState(current.callId, lastSignalSeq)
      if (stateResp != null) {
        if (stateResp.sdpOffer.isNotBlank() && pendingRemoteOfferSdp.isBlank()) {
          pendingRemoteOfferSdp = stateResp.sdpOffer
        }
        processBufferedSignals(stateResp.signals)
      }
      if (pendingRemoteOfferSdp.isNotBlank()) {
        webRtcEngine.handleRemoteOfferAndCreateAnswer(pendingRemoteOfferSdp)
      } else {
        // Tell backend immediately that callee accepted so timer stops & state becomes connected
        val updated = chatRepository.sendCallSignal(current.callId, CallSignalRequestDto(action = "accept"))
        if (updated != null && updated.state == "connected") {
          transitionToConnected(updated.answeredAtMs)
        }
      }
    }
  }

  fun declineIncomingCall() {
    val current = _uiState.value
    if (current.callId.isBlank()) return
    TelefamNotificationManager.cancelCallNotification(appContext, current.callId)
    IncomingCallPushBridge.clearPending(current.callId)
    val callId = current.callId
    val convId = current.conversationId
    scope.launch {
      chatRepository.sendCallSignal(callId, CallSignalRequestDto(action = "decline", reason = "declined"))
      finishCallLocally("declined", "Declined", convId)
    }
  }

  fun hangUp(reason: String = "hangup") {
    val current = _uiState.value
    val callId = current.callId
    val convId = current.conversationId
    if (callId.isBlank()) {
      resetToIdle()
      return
    }
    TelefamNotificationManager.cancelCallNotification(appContext, callId)
    IncomingCallPushBridge.clearPending(callId)
    scope.launch {
      sendSignalBothChannels(callId, CallSignalRequestDto(action = "end", reason = reason))
      val label = if (current.phase == TelefamCallPhase.CONNECTED) "Call ended" else "Cancelled"
      finishCallLocally(reason, label, convId)
    }
  }

  fun toggleMic() {
    val next = !_uiState.value.micEnabled
    _uiState.update { it.copy(micEnabled = next) }
    webRtcEngine.setMicEnabled(next)
    publishMediaState()
  }

  fun toggleSpeaker() {
    val next = !_uiState.value.speakerEnabled
    _uiState.update { it.copy(speakerEnabled = next) }
    soundEngine.setSpeakerEnabled(next)
  }

  fun toggleCamera() {
    val current = _uiState.value
    if (current.callType != TelefamCallType.VIDEO) return
    val next = !current.localCamEnabled
    desiredCamEnabledByUser = next
    _uiState.update { it.copy(localCamEnabled = next) }
    webRtcEngine.setCameraEnabled(next && current.localInAppActive)
    publishMediaState()
  }

  fun flipCamera() {
    if (_uiState.value.callType != TelefamCallType.VIDEO || _uiState.value.localScreenSharing) return
    webRtcEngine.flipCamera()
  }

  fun setVideoFilter(filter: VideoCallFilter) {
    _uiState.update { it.copy(videoFilter = filter) }
    webRtcEngine.setVideoFilter(filter)
    publishMediaState()
  }

  fun setQualityTier(tier: CallQualityTier) {
    val resolved = if (tier == CallQualityTier.AUTO) {
      recommendedQualityForNetwork(networkMonitor.networkTier.value)
    } else {
      tier
    }
    _uiState.update {
      it.copy(
        selectedQualityMode = tier,
        effectiveQualityTier = resolved,
      )
    }
    webRtcEngine.applyQualityParameters(resolved)
    publishMediaState()
  }

  fun startScreenShare(mediaProjectionData: Intent) {
    val current = _uiState.value
    if (current.callId.isBlank()) return
    // Start MediaProjection typed ForegroundService BEFORE creating ScreenCapturerAndroid (Android 14+ policy)
    TelefamCallForegroundService.startOrUpdate(
      context = appContext,
      callId = current.callId,
      conversationId = current.conversationId,
      peerUserId = current.peerUserId,
      peerName = current.peerName,
      isVideo = true,
      isScreenSharing = true,
    )
    if (current.callType == TelefamCallType.AUDIO) {
      _uiState.update { it.copy(callType = TelefamCallType.VIDEO) }
    }
    val started = webRtcEngine.startScreenShare(mediaProjectionData)
    if (started) {
      _uiState.update { it.copy(localScreenSharing = true) }
      publishMediaState()
    }
  }

  fun stopScreenShare() {
    val current = _uiState.value
    if (!current.localScreenSharing) return
    val shouldResumeCam = desiredCamEnabledByUser && current.localInAppActive
    webRtcEngine.stopScreenShare(resumeCamera = shouldResumeCam)
    _uiState.update { it.copy(localScreenSharing = false) }
    TelefamCallForegroundService.startOrUpdate(
      context = appContext,
      callId = current.callId,
      conversationId = current.conversationId,
      peerUserId = current.peerUserId,
      peerName = current.peerName,
      isVideo = current.callType == TelefamCallType.VIDEO,
      isScreenSharing = false,
    )
    publishMediaState()
  }

  fun requestSwitchToVideo() {
    val current = _uiState.value
    if (current.callId.isBlank() || current.callType == TelefamCallType.VIDEO) return
    _uiState.update { it.copy(switchVideoPrompt = SwitchVideoPromptState.OUTGOING_PENDING) }
    scope.launch {
      sendSignalBothChannels(
        current.callId,
        CallSignalRequestDto(action = "switch_video_request"),
      )
    }
  }

  fun respondSwitchToVideo(accept: Boolean) {
    val current = _uiState.value
    if (current.callId.isBlank()) return
    _uiState.update { it.copy(switchVideoPrompt = SwitchVideoPromptState.NONE) }
    if (accept) {
      desiredCamEnabledByUser = true
      _uiState.update {
        it.copy(
          callType = TelefamCallType.VIDEO,
          speakerEnabled = true,
          localCamEnabled = current.localInAppActive,
          peerCamEnabled = true,
        )
      }
      soundEngine.setSpeakerEnabled(true)
      webRtcEngine.setCameraEnabled(current.localInAppActive)
      TelefamCallForegroundService.startOrUpdate(
        context = appContext,
        callId = current.callId,
        conversationId = current.conversationId,
        peerUserId = current.peerUserId,
        peerName = current.peerName,
        isVideo = true,
        isScreenSharing = current.localScreenSharing,
      )
      scope.launch {
        sendSignalBothChannels(
          current.callId,
          CallSignalRequestDto(
            action = "switch_video_accept",
            camEnabled = current.localInAppActive,
            inAppActive = current.localInAppActive,
          ),
        )
      }
    } else {
      scope.launch {
        sendSignalBothChannels(
          current.callId,
          CallSignalRequestDto(action = "switch_video_decline"),
        )
      }
    }
  }

  fun minimizeToChat(onOpenChat: (conversationId: String, peerUserId: String) -> Unit) {
    val current = _uiState.value
    if (current.callId.isBlank()) return
    val convId = current.conversationId
    val peerId = current.peerUserId
    // Leaving the call screen while staying on audio swaps camera off to Avatar on peer side
    _uiState.update { it.copy(isMinimizedToChat = true, localInAppActive = false) }
    if (current.callType == TelefamCallType.VIDEO && !current.localScreenSharing) {
      webRtcEngine.setCameraEnabled(false)
    }
    publishMediaState()
    if (peerId.isNotBlank()) {
      onOpenChat(convId, peerId)
    }
  }

  fun expandFromMinimized() {
    val current = _uiState.value
    if (!current.isMinimizedToChat) return
    val restoreInApp = isAppInForeground
    _uiState.update {
      it.copy(
        isMinimizedToChat = false,
        localInAppActive = restoreInApp,
      )
    }
    if (current.callType == TelefamCallType.VIDEO && !current.localScreenSharing) {
      webRtcEngine.setCameraEnabled(desiredCamEnabledByUser && restoreInApp)
    }
    publishMediaState()
  }

  fun onAppForegroundChanged(inForeground: Boolean) {
    isAppInForeground = inForeground
    val current = _uiState.value
    if (current.phase != TelefamCallPhase.CONNECTED && current.phase != TelefamCallPhase.CONNECTING) {
      return
    }
    val activeOnCallScreen = inForeground && !current.isMinimizedToChat
    if (current.localInAppActive == activeOnCallScreen) return

    _uiState.update { it.copy(localInAppActive = activeOnCallScreen) }
    if (current.callType == TelefamCallType.VIDEO && !current.localScreenSharing) {
      webRtcEngine.setCameraEnabled(desiredCamEnabledByUser && activeOnCallScreen)
    }
    publishMediaState()
  }

  fun dismissEndedNotice() {
    val phase = _uiState.value.phase
    if (phase == TelefamCallPhase.ENDED || phase == TelefamCallPhase.NOT_ALLOWED) {
      resetToIdle()
    }
  }

  // ---- WebRTC Callbacks ----

  override fun onLocalSdpCreated(type: String, sdp: String) {
    val current = _uiState.value
    if (current.callId.isBlank()) return
    val action = if (type == "answer") "accept" else "offer"
    scope.launch {
      val updated = chatRepository.sendCallSignal(
        current.callId,
        CallSignalRequestDto(action = action, sdp = sdp, sdpType = type),
      )
      if (action == "accept" && updated != null) {
        transitionToConnected(updated.answeredAtMs)
      }
    }
  }

  override fun onLocalIceCandidate(candidate: String, sdpMid: String, sdpMLineIndex: Int) {
    val current = _uiState.value
    if (current.callId.isBlank()) return
    scope.launch {
      sendSignalBothChannels(
        current.callId,
        CallSignalRequestDto(
          action = "ice",
          candidate = candidate,
          sdpMid = sdpMid,
          sdpMLineIndex = sdpMLineIndex,
        ),
      )
    }
  }

  override fun onIceConnectionStateChanged(state: PeerConnection.IceConnectionState) {
    scope.launch {
      when (state) {
        PeerConnection.IceConnectionState.CONNECTED,
        PeerConnection.IceConnectionState.COMPLETED -> {
          val cur = _uiState.value
          if (cur.phase == TelefamCallPhase.CONNECTING || cur.phase == TelefamCallPhase.OUTGOING_RINGING || cur.phase == TelefamCallPhase.OUTGOING_CALLING) {
            transitionToConnected(System.currentTimeMillis())
          } else if (cur.phase == TelefamCallPhase.CONNECTED) {
            _uiState.update { it.copy(statusText = "") }
          }
        }
        PeerConnection.IceConnectionState.DISCONNECTED -> {
          val cur = _uiState.value
          if (cur.phase == TelefamCallPhase.CONNECTED) {
            _uiState.update { it.copy(statusText = "Reconnecting…", lowBandwidthWarning = true) }
            webRtcEngine.createOffer(iceRestart = true)
          }
        }
        PeerConnection.IceConnectionState.FAILED -> {
          if (_uiState.value.phase == TelefamCallPhase.CONNECTED || _uiState.value.phase == TelefamCallPhase.CONNECTING) {
            webRtcEngine.createOffer(iceRestart = true)
          }
        }
        else -> Unit
      }
    }
  }

  override fun onStatsSample(packetLossPct: Float, rttMs: Double) {
    scope.launch {
      val cur = _uiState.value
      if (cur.phase != TelefamCallPhase.CONNECTED || cur.selectedQualityMode != CallQualityTier.AUTO) return@launch
      val nextTier = when {
        packetLossPct >= 7f || rttMs >= 650.0 -> CallQualityTier.LOW_240P
        packetLossPct >= 3f || rttMs >= 320.0 -> CallQualityTier.BALANCED_480P
        else -> recommendedQualityForNetwork(networkMonitor.networkTier.value)
      }
      val weak = packetLossPct >= 5f || rttMs >= 500.0 || networkMonitor.networkTier.value == NetworkTier.WEAK
      if (nextTier != cur.effectiveQualityTier || weak != cur.lowBandwidthWarning) {
        _uiState.update {
          it.copy(
            effectiveQualityTier = nextTier,
            lowBandwidthWarning = weak,
          )
        }
        webRtcEngine.applyQualityParameters(nextTier)
      }
    }
  }

  override fun onScreenShareStoppedBySystem() {
    scope.launch { stopScreenShare() }
  }

  // ---- Internal Signaling, Polling & Timeouts ----

  private fun applyAutoQualityForNetwork(tier: NetworkTier) {
    val cur = _uiState.value
    if (cur.selectedQualityMode != CallQualityTier.AUTO) return
    val next = recommendedQualityForNetwork(tier)
    val weak = tier == NetworkTier.WEAK || tier == NetworkTier.OFFLINE
    if (next != cur.effectiveQualityTier || weak != cur.lowBandwidthWarning) {
      _uiState.update {
        it.copy(
          effectiveQualityTier = next,
          lowBandwidthWarning = weak,
        )
      }
      webRtcEngine.applyQualityParameters(next)
      publishMediaState()
    }
  }

  private fun publishMediaState() {
    val cur = _uiState.value
    if (cur.callId.isBlank()) return
    scope.launch {
      sendSignalBothChannels(
        cur.callId,
        CallSignalRequestDto(
          action = "media_state",
          camEnabled = cur.localCamEnabled,
          micEnabled = cur.micEnabled,
          inAppActive = cur.localInAppActive,
          screenSharing = cur.localScreenSharing,
          videoFilter = cur.videoFilter.wireValue,
          networkQuality = cur.effectiveQualityTier.wireValue,
        ),
      )
    }
  }

  private suspend fun sendSignalBothChannels(callId: String, req: CallSignalRequestDto) {
    val json = JSONObject().put("action", req.action).apply {
      if (req.sdp.isNotBlank()) put("sdp", req.sdp)
      if (req.sdpType.isNotBlank()) put("sdp_type", req.sdpType)
      if (req.candidate.isNotBlank()) put("candidate", req.candidate)
      if (req.sdpMid.isNotBlank()) put("sdp_mid", req.sdpMid)
      if (req.sdpMLineIndex != 0) put("sdp_mline_index", req.sdpMLineIndex)
      if (req.reason.isNotBlank()) put("reason", req.reason)
      req.camEnabled?.let { put("cam_enabled", it) }
      req.micEnabled?.let { put("mic_enabled", it) }
      req.inAppActive?.let { put("in_app_active", it) }
      req.screenSharing?.let { put("screen_sharing", it) }
      if (req.videoFilter.isNotBlank()) put("video_filter", req.videoFilter)
      if (req.networkQuality.isNotBlank()) put("network_quality", req.networkQuality)
    }
    val sentWs = chatRepository.sendCallSignalViaWs(callId, json)
    if (!sentWs || req.action in setOf("accept", "answer", "decline", "end", "cancel", "offer", "ringing")) {
      chatRepository.sendCallSignal(callId, req)
    }
  }

  private fun startRingCountdownAndPollers(callId: String, timeoutSec: Int) {
    ringTimeoutJob?.cancel()
    ringTimeoutJob = scope.launch {
      var remaining = timeoutSec.coerceIn(5, 30)
      while (isActive && remaining > 0) {
        val phase = _uiState.value.phase
        if (phase != TelefamCallPhase.OUTGOING_CALLING &&
          phase != TelefamCallPhase.OUTGOING_RINGING &&
          phase != TelefamCallPhase.INCOMING_RINGING
        ) {
          return@launch
        }
        _uiState.update { it.copy(remainingRingSec = remaining) }
        delay(1_000L)
        remaining--
      }
      val phase = _uiState.value.phase
      if (phase == TelefamCallPhase.OUTGOING_CALLING ||
        phase == TelefamCallPhase.OUTGOING_RINGING ||
        phase == TelefamCallPhase.INCOMING_RINGING
      ) {
        sendSignalBothChannels(callId, CallSignalRequestDto(action = "end", reason = "timeout_25s"))
        finishCallLocally("timeout_25s", "No answer", _uiState.value.conversationId)
      }
    }

    pollJob?.cancel()
    pollJob = scope.launch {
      while (isActive && _uiState.value.callId == callId) {
        val resp = chatRepository.getCallState(callId, lastSignalSeq)
        if (resp != null && resp.success) {
          if (resp.sdpOffer.isNotBlank() && pendingRemoteOfferSdp.isBlank()) {
            pendingRemoteOfferSdp = resp.sdpOffer
          }
          if (resp.signals.isNotEmpty()) {
            processBufferedSignals(resp.signals)
          }
          syncCallRecordState(resp.call)
        }
        delay(1_400L)
      }
    }
  }

  private fun handleWsCallEvent(event: String, rawJson: String) {
    if (rawJson.isBlank()) return
    val obj = runCatching { JSONObject(rawJson) }.getOrNull() ?: return
    val callObj = obj.optJSONObject("call")
    when (event) {
      "call_incoming" -> {
        if (callObj == null) return
        val callId = callObj.optString("call_id")
        val callerId = callObj.optString("caller_user_id")
        val convId = callObj.optString("conversation_id")
        val callType = TelefamCallType.fromWire(callObj.optString("call_type"))
        val offer = obj.optString("sdp_offer")
        if (offer.isNotBlank()) pendingRemoteOfferSdp = offer
        handleIncomingCallPush(
          IncomingCallPushPayload(
            callId = callId,
            conversationId = convId,
            callType = callType,
            callerUserId = callerId,
            callerName = callObj.optString("caller_name"),
            callerUsername = callObj.optString("caller_username"),
            callerAvatarUrl = callObj.optString("caller_avatar"),
            expiresAtMs = callObj.optLong("expires_at_ms", System.currentTimeMillis() + 25_000L),
            autoAccept = false,
          ),
        )
      }

      "call_ringing" -> {
        val callId = callObj?.optString("call_id").orEmpty()
        if (callId == _uiState.value.callId && _uiState.value.phase == TelefamCallPhase.OUTGOING_CALLING) {
          _uiState.update { it.copy(phase = TelefamCallPhase.OUTGOING_RINGING, statusText = "Ringing…") }
          soundEngine.startOutgoingRingingTone()
        }
      }

      "call_accepted" -> {
        val callId = callObj?.optString("call_id").orEmpty()
        if (callId == _uiState.value.callId) {
          transitionToConnected(callObj?.optLong("answered_at_ms") ?: System.currentTimeMillis())
        }
      }

      "call_signal" -> {
        val callId = callObj?.optString("call_id").orEmpty()
        if (callId != _uiState.value.callId) return
        val sig = obj.optJSONObject("signal") ?: return
        val dto = BufferedSignalEventDto(
          seq = sig.optLong("seq", 0L),
          fromUserId = sig.optString("from_user_id"),
          action = sig.optString("action"),
          sdp = sig.optString("sdp"),
          sdpType = sig.optString("sdp_type"),
          candidate = sig.optString("candidate"),
          sdpMid = sig.optString("sdp_mid"),
          sdpMLineIndex = sig.optInt("sdp_mline_index", 0),
          camEnabled = if (sig.has("cam_enabled")) sig.optBoolean("cam_enabled") else null,
          micEnabled = if (sig.has("mic_enabled")) sig.optBoolean("mic_enabled") else null,
          inAppActive = if (sig.has("in_app_active")) sig.optBoolean("in_app_active") else null,
          screenSharing = if (sig.has("screen_sharing")) sig.optBoolean("screen_sharing") else null,
          videoFilter = sig.optString("video_filter"),
          networkQuality = sig.optString("network_quality"),
          timestampMs = sig.optLong("timestamp_ms", 0L),
        )
        processBufferedSignals(listOf(dto))
      }

      "call_ended" -> {
        val callId = callObj?.optString("call_id").orEmpty()
        val convId = obj.optString("conversation_id").ifBlank { callObj?.optString("conversation_id").orEmpty() }
        val state = callObj?.optString("state").orEmpty()
        val reason = callObj?.optString("end_reason").orEmpty()
        if (callId == _uiState.value.callId) {
          val label = when (state) {
            "declined" -> "Declined"
            "missed" -> "No answer"
            "busy" -> "Busy"
            else -> "Call ended"
          }
          finishCallLocally(reason.ifBlank { state }, label, convId)
        }
      }
    }
  }

  private fun processBufferedSignals(signals: List<BufferedSignalEventDto>) {
    for (sig in signals) {
      if (sig.seq <= lastSignalSeq) continue
      lastSignalSeq = sig.seq
      when (sig.action.lowercase()) {
        "offer" -> {
          if (sig.sdp.isNotBlank()) {
            pendingRemoteOfferSdp = sig.sdp
            if (_uiState.value.phase == TelefamCallPhase.CONNECTING || _uiState.value.phase == TelefamCallPhase.CONNECTED) {
              webRtcEngine.handleRemoteOfferAndCreateAnswer(sig.sdp)
            }
          }
        }
        "answer" -> {
          if (sig.sdp.isNotBlank()) {
            webRtcEngine.handleRemoteAnswer(sig.sdp)
          }
        }
        "ice" -> {
          webRtcEngine.addRemoteIceCandidate(sig.candidate, sig.sdpMid, sig.sdpMLineIndex)
        }
        "media_state" -> {
          _uiState.update { cur ->
            cur.copy(
              peerCamEnabled = sig.camEnabled ?: cur.peerCamEnabled,
              peerMicEnabled = sig.micEnabled ?: cur.peerMicEnabled,
              peerInAppActive = sig.inAppActive ?: cur.peerInAppActive,
              peerScreenSharing = sig.screenSharing ?: cur.peerScreenSharing,
            )
          }
        }
        "switch_video_request" -> {
          _uiState.update { it.copy(switchVideoPrompt = SwitchVideoPromptState.INCOMING_REQUEST) }
        }
        "switch_video_accept" -> {
          desiredCamEnabledByUser = true
          _uiState.update { cur ->
            cur.copy(
              callType = TelefamCallType.VIDEO,
              speakerEnabled = true,
              switchVideoPrompt = SwitchVideoPromptState.NONE,
              localCamEnabled = cur.localInAppActive,
              peerCamEnabled = sig.camEnabled ?: true,
              peerInAppActive = sig.inAppActive ?: true,
            )
          }
          soundEngine.setSpeakerEnabled(true)
          webRtcEngine.setCameraEnabled(_uiState.value.localInAppActive)
        }
        "switch_video_decline" -> {
          _uiState.update {
            it.copy(
              switchVideoPrompt = SwitchVideoPromptState.NONE,
              statusText = "Video switch declined",
            )
          }
        }
      }
    }
  }

  private fun syncCallRecordState(record: CallRecordDto) {
    if (record.callId != _uiState.value.callId) return
    when (record.state) {
      "ringing" -> {
        if (_uiState.value.phase == TelefamCallPhase.OUTGOING_CALLING) {
          _uiState.update { it.copy(phase = TelefamCallPhase.OUTGOING_RINGING, statusText = "Ringing…") }
          soundEngine.startOutgoingRingingTone()
        }
      }
      "connected" -> {
        transitionToConnected(record.answeredAtMs)
        // Use authoritative server duration whenever available
        if (record.durationSec > _uiState.value.connectedElapsedSec) {
          _uiState.update { it.copy(connectedElapsedSec = record.durationSec) }
        }
      }
      "ended", "missed", "declined", "busy", "cancelled" -> {
        val label = when (record.state) {
          "declined" -> "Declined"
          "missed" -> "No answer"
          "busy" -> "Busy"
          else -> "Call ended"
        }
        finishCallLocally(record.endReason.ifBlank { record.state }, label, record.conversationId)
      }
    }
  }

  private fun transitionToConnected(answeredAtMs: Long) {
    val cur = _uiState.value
    if (cur.phase == TelefamCallPhase.CONNECTED) return
    ringTimeoutJob?.cancel()
    soundEngine.playConnectedCue()
    answeredEpochMs = if (answeredAtMs > 0L) answeredAtMs else System.currentTimeMillis()
    _uiState.update {
      it.copy(
        phase = TelefamCallPhase.CONNECTED,
        statusText = "",
      )
    }
    tickerJob?.cancel()
    tickerJob = scope.launch {
      while (isActive && _uiState.value.phase == TelefamCallPhase.CONNECTED) {
        val elapsed = ((System.currentTimeMillis() - answeredEpochMs) / 1000L).coerceAtLeast(0L)
        _uiState.update { it.copy(connectedElapsedSec = elapsed) }
        delay(1_000L)
      }
    }
    statsJob?.cancel()
    statsJob = scope.launch {
      while (isActive && _uiState.value.phase == TelefamCallPhase.CONNECTED) {
        delay(3_000L)
        webRtcEngine.pollStatsSample()
      }
    }
  }

  private fun handleRemoteCallCancelled(callId: String, reason: String) {
    if (_uiState.value.callId != callId) return
    val label = if (reason == "timeout_25s" || reason == "missed") "No answer" else "Call ended"
    finishCallLocally(reason, label, _uiState.value.conversationId)
  }

  private fun finishCallLocally(reason: String, statusLabel: String, convId: String = _uiState.value.conversationId) {
    ringTimeoutJob?.cancel()
    pollJob?.cancel()
    tickerJob?.cancel()
    statsJob?.cancel()
    val callId = _uiState.value.callId
    if (callId.isNotBlank()) {
      TelefamNotificationManager.cancelCallNotification(appContext, callId)
    }
    TelefamCallForegroundService.stop(appContext)
    webRtcEngine.disposeSession()
    soundEngine.playEndedCue()
    soundEngine.releaseAll()

    _uiState.update {
      it.copy(
        phase = TelefamCallPhase.ENDED,
        isMinimizedToChat = false,
        localScreenSharing = false,
        statusText = statusLabel,
        endReason = reason,
        switchVideoPrompt = SwitchVideoPromptState.NONE,
      )
    }
    if (convId.isNotBlank()) {
      scope.launch {
        chatRepository.loadConversationMessages(convId, markAsRead = false)
        _callConversationUpdated.tryEmit(convId)
      }
    }
    scheduleAutoResetToIdle(2_200L)
  }

  private fun scheduleAutoResetToIdle(delayMs: Long) {
    val endedCallId = _uiState.value.callId
    scope.launch {
      delay(delayMs)
      val cur = _uiState.value
      if ((cur.phase == TelefamCallPhase.ENDED || cur.phase == TelefamCallPhase.NOT_ALLOWED) &&
        cur.callId == endedCallId
      ) {
        resetToIdle()
      }
    }
  }

  private fun resetToIdle() {
    ringTimeoutJob?.cancel()
    pollJob?.cancel()
    tickerJob?.cancel()
    statsJob?.cancel()
    TelefamCallForegroundService.stop(appContext)
    webRtcEngine.disposeSession()
    soundEngine.releaseAll()
    _uiState.value = ActiveCallUiState()
  }

  fun dispose() {
    resetToIdle()
    webRtcEngine.shutdownAll()
  }
}
