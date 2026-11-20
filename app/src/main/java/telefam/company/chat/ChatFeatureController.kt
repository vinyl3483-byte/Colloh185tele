package telefam.company.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import telefam.company.data.local.ChatConversationEntity
import telefam.company.data.local.ChatMessageEntity
import telefam.company.data.network.GiflyMediaItemDto
import telefam.company.data.repository.ChatSearchBundleResult
import telefam.company.data.repository.TelefamRepository
import telefam.company.ui.viewmodel.TelefamUiState

@OptIn(ExperimentalCoroutinesApi::class)
class ChatFeatureController(
  private val scope: CoroutineScope,
  private val repository: TelefamRepository,
  private val uiStateFlow: MutableStateFlow<TelefamUiState>,
) {
  private val chatRepo = repository.chatRepository
  private val voiceAudio = ChatVoiceAudioManager(repository.applicationContext())

  private var typingDebounceJob: Job? = null
  private var lastSentTypingMs: Long = 0L
  private var lastIncomingTypingSoundMs: Long = 0L
  private var giflySearchJob: Job? = null
  private var homeSearchJob: Job? = null
  private var viewOnceTimerJob: Job? = null
  private var undoTimeoutJob: Job? = null

  private val seenIncomingMessageIds = mutableSetOf<String>()

  init {
    // 1. Sync Signal keys & WebSocket lifecycle with authentication & network changes
    scope.launch {
      uiStateFlow
        .map { it.isAuthenticated to it.userId }
        .distinctUntilChanged()
        .collect { (authed, uid) ->
          if (authed && uid.isNotBlank()) {
            chatRepo.ensureSignalEngineReady(true)
            chatRepo.connectWebSocket()
            chatRepo.flushPendingChatOps()
            chatRepo.refreshConversations()
          } else {
            chatRepo.disconnectWebSocket()
            voiceAudio.stopPlayback()
            voiceAudio.discardRecording()
            viewOnceTimerJob?.cancel()
            updateChat { TelefamChatUiState() }
          }
        }
    }

    // 1b. Weak-network live delta sync heartbeat so incoming chats & block state update without manual refresh
    scope.launch {
      while (isActive) {
        delay(5_000L)
        val snap = uiStateFlow.value
        if (snap.isAuthenticated && snap.userId.isNotBlank()) {
          chatRepo.flushPendingChatOps()
          chatRepo.refreshConversations()
          val activeConvId = snap.chatState.activeConversationId
          if (!activeConvId.isNullOrBlank()) {
            chatRepo.loadConversationMessages(activeConvId, markAsRead = true)
          }
        }
      }
    }

    // 2. Observe active 1-on-1 conversations from Room cache
    scope.launch {
      uiStateFlow
        .map { it.isAuthenticated && it.userId.isNotBlank() }
        .distinctUntilChanged()
        .flatMapLatest { ready ->
          if (!ready) flowOf(emptyList()) else chatRepo.observeActiveConversations()
        }
        .collect { list ->
          updateChat { it.copy(conversations = list) }
        }
    }

    // 3. Observe messages & persistent security flags for active open 1-on-1 conversation
    scope.launch {
      uiStateFlow
        .map { it.chatState.activeConversationId }
        .distinctUntilChanged()
        .flatMapLatest { convId ->
          if (convId.isNullOrBlank()) flowOf(emptyList()) else chatRepo.observeMessagesForConversation(convId)
        }
        .collect { msgs ->
          val ownerUid = uiStateFlow.value.userId
          val activeConv = uiStateFlow.value.chatState.activeConversation
          msgs.filter { it.senderUserId != ownerUid }.forEach { incoming ->
            if (seenIncomingMessageIds.add(incoming.messageId)) {
              // Play chime only if live arrival while chat was already open
              if (uiStateFlow.value.chatState.activeMessages.isNotEmpty() && incoming.createdAtMs > System.currentTimeMillis() - 10_000L) {
                ChatSoundEffects.playIncomingChime(soundAllowed(activeConv?.isMuted == true))
              }
            }
          }
          updateChat { it.copy(activeMessages = msgs) }
        }
    }

    scope.launch {
      uiStateFlow
        .map { it.chatState.activeConversationId }
        .distinctUntilChanged()
        .flatMapLatest { convId ->
          if (convId.isNullOrBlank()) flowOf(emptyList()) else chatRepo.observeSecurityFlags(convId)
        }
        .collect { flags ->
          updateChat { it.copy(activeSecurityFlags = flags) }
        }
    }

    // 4. Observe WebSocket typing indicators & real-time events
    scope.launch {
      chatRepo.wsEvents.collect { ev ->
        when (ev.event) {
          "typing" -> {
            val mode = ev.typingMode
            val convId = ev.conversationId
            if (convId.isNotBlank()) {
              updateChat { s ->
                val map = s.typingModesByConversation.toMutableMap()
                if (mode.isBlank()) map.remove(convId) else map[convId] = mode
                s.copy(typingModesByConversation = map)
              }
              if (mode == "typing" && convId == uiStateFlow.value.chatState.activeConversationId) {
                val conv = uiStateFlow.value.chatState.activeConversation
                val now = System.currentTimeMillis()
                if (now - lastIncomingTypingSoundMs > 1800L) {
                  lastIncomingTypingSoundMs = now
                  ChatSoundEffects.playTypingTick(soundAllowed(conv?.isMuted == true))
                }
              }
            }
          }
          "new_message", "message_new" -> {
            val ownerUid = uiStateFlow.value.userId
            val activeConvId = uiStateFlow.value.chatState.activeConversationId
            if (ev.conversationId.isNotBlank() && ev.conversationId == activeConvId) {
              chatRepo.loadConversationMessages(ev.conversationId, markAsRead = true)
            }
            if (ev.userId != ownerUid && ev.conversationId != activeConvId) {
              val conv = uiStateFlow.value.chatState.conversations.firstOrNull { it.conversationId == ev.conversationId }
              ChatSoundEffects.playIncomingChime(soundAllowed(conv?.isMuted == true))
            }
          }
        }
      }
    }

    // 5. Observe voice recording and playback states
    scope.launch {
      voiceAudio.recording.collect { rec ->
        updateChat { it.copy(voiceRecording = rec) }
      }
    }
    scope.launch {
      voiceAudio.playback.collect { pb ->
        updateChat { it.copy(voicePlayback = pb) }
      }
    }
  }

  private fun soundAllowed(conversationMuted: Boolean): Boolean {
    if (conversationMuted) return false
    val notif = uiStateFlow.value.settings.notifications
    if (notif.masterPause) return false
    return notif.typeMessages
  }

  private inline fun updateChat(crossinline transform: (TelefamChatUiState) -> TelefamChatUiState) {
    uiStateFlow.update { current ->
      current.copy(chatState = transform(current.chatState))
    }
  }

  fun refreshConversations() {
    scope.launch { chatRepo.refreshConversations() }
  }

  fun openDirectChat(
    peerUserId: String,
    peerFullName: String = "",
    peerUsername: String = "",
    peerAvatarUrl: String = "",
  ) {
    scope.launch {
      updateChat { it.copy(isLoadingMessages = true, isHomeSearchOpen = false, isMultiSelectMode = false, selectedConversationIds = emptySet()) }
      val conv = chatRepo.openDirectChat(peerUserId, peerFullName, peerUsername, peerAvatarUrl)
      if (conv == null) {
        updateChat { it.copy(isLoadingMessages = false, toastMessage = "Unable to open conversation") }
        return@launch
      }
      openConversation(conv.conversationId, conv.peerUserId)
    }
  }

  fun openConversation(conversationId: String, peerUserId: String = "") {
    seenIncomingMessageIds.clear()
    updateChat {
      it.copy(
        activeConversationId = conversationId,
        composerText = it.conversations.firstOrNull { c -> c.conversationId == conversationId }?.draftText.orEmpty(),
        editingMessageId = null,
        giflyPickerOpen = false,
        giflyPreviewItem = null,
        isLoadingMessages = true,
      )
    }
    scope.launch {
      val resolvedPeer = peerUserId.ifBlank {
        uiStateFlow.value.chatState.conversations.firstOrNull { it.conversationId == conversationId }?.peerUserId.orEmpty()
      }
      val greeting = chatRepo.fetchGreetingSticker(resolvedPeer)
      updateChat { it.copy(greetingSticker = greeting) }
      chatRepo.loadConversationMessages(conversationId, markAsRead = true)
      updateChat { it.copy(isLoadingMessages = false) }
    }
  }

  fun closeActiveConversation() {
    val activeId = uiStateFlow.value.chatState.activeConversationId
    if (!activeId.isNullOrBlank()) {
      chatRepo.sendTypingIndicator(activeId, "")
    }
    voiceAudio.stopPlayback()
    voiceAudio.discardRecording()
    viewOnceTimerJob?.cancel()
    updateChat {
      it.copy(
        activeConversationId = null,
        activeMessages = emptyList(),
        activeSecurityFlags = emptyList(),
        composerText = "",
        editingMessageId = null,
        giflyPickerOpen = false,
        giflyPreviewItem = null,
        forwardingMessage = null,
        sharedMediaTab = null,
        activeViewOnceOverlay = null,
      )
    }
  }

  fun onComposerTextChanged(newText: String) {
    val activeId = uiStateFlow.value.chatState.activeConversationId ?: return
    val oldText = uiStateFlow.value.chatState.composerText
    updateChat { it.copy(composerText = newText) }

    val now = System.currentTimeMillis()
    if (newText.isNotBlank() && now - lastSentTypingMs > 2200L) {
      lastSentTypingMs = now
      chatRepo.sendTypingIndicator(activeId, "typing")
    } else if (newText.isBlank() && oldText.isNotBlank()) {
      chatRepo.sendTypingIndicator(activeId, "")
    }
    typingDebounceJob?.cancel()
    if (newText.isNotBlank()) {
      typingDebounceJob = scope.launch {
        delay(3500L)
        chatRepo.sendTypingIndicator(activeId, "")
      }
    }
  }

  fun sendCurrentTextOrSaveEdit() {
    val chat = uiStateFlow.value.chatState
    val conv = chat.activeConversation ?: return
    if (conv.isBlocked) {
      updateChat { it.copy(toastMessage = "You cannot message this user") }
      return
    }
    val text = chat.composerText.trim()
    if (text.isBlank()) return
    val editingId = chat.editingMessageId

    updateChat { it.copy(composerText = "", editingMessageId = null) }
    chatRepo.sendTypingIndicator(conv.conversationId, "")
    ChatSoundEffects.playSentPop(soundAllowed(conv.isMuted))

    scope.launch {
      if (!editingId.isNullOrBlank()) {
        chatRepo.editMessage(editingId, conv.conversationId, conv.peerUserId, text)
      } else {
        chatRepo.sendTextMessage(conv.conversationId, conv.peerUserId, text)
      }
    }
  }

  fun sendGreetingSticker() {
    val chat = uiStateFlow.value.chatState
    val conv = chat.activeConversation ?: return
    if (conv.isBlocked) {
      updateChat { it.copy(toastMessage = "You cannot message this user") }
      return
    }
    val greeting = chat.greetingSticker ?: return
    ChatSoundEffects.playSentPop(soundAllowed(conv.isMuted))
    scope.launch {
      chatRepo.sendGiflyMediaMessage(conv.conversationId, conv.peerUserId, greeting, "sticker")
    }
  }

  fun startEditingMessage(msg: ChatMessageEntity) {
    if (msg.messageType != "text") return
    updateChat { it.copy(editingMessageId = msg.messageId, composerText = msg.plainText) }
  }

  fun cancelEditingMessage() {
    updateChat { it.copy(editingMessageId = null, composerText = "") }
  }

  fun unsendMessageWithSparkles(msg: ChatMessageEntity) {
    val convId = msg.conversationId
    updateChat { it.copy(unsendingMessageIds = it.unsendingMessageIds + msg.messageId) }
    scope.launch {
      // Allow sparkle dissolve animation to play smoothly before removing all traces
      delay(460L)
      chatRepo.unsendMessage(msg.messageId, convId)
      updateChat { it.copy(unsendingMessageIds = it.unsendingMessageIds - msg.messageId) }
    }
  }

  fun deleteMessageForSelf(msg: ChatMessageEntity) {
    scope.launch {
      chatRepo.deleteMessageForSelf(msg.messageId, msg.conversationId)
    }
  }

  fun startForwardMessage(msg: ChatMessageEntity) {
    updateChat { it.copy(forwardingMessage = msg) }
  }

  fun dismissForwardMessage() {
    updateChat { it.copy(forwardingMessage = null) }
  }

  fun confirmForwardMessage(targetConv: ChatConversationEntity) {
    val msg = uiStateFlow.value.chatState.forwardingMessage ?: return
    updateChat { it.copy(forwardingMessage = null, toastMessage = "Forwarded to ${targetConv.peerFullName.ifBlank { targetConv.peerUsername }}") }
    ChatSoundEffects.playSentPop(soundAllowed(targetConv.isMuted))
    scope.launch {
      if (msg.messageType == "text") {
        chatRepo.sendTextMessage(targetConv.conversationId, targetConv.peerUserId, msg.plainText, forwardedFromUserId = msg.senderUserId)
      } else if (msg.messageType == "sticker" || msg.messageType == "gif" || msg.messageType == "clip") {
        chatRepo.sendGiflyMediaMessage(
          targetConv.conversationId,
          targetConv.peerUserId,
          GiflyMediaItemDto(url = msg.mediaUrl, title = msg.messageType),
          msg.messageType,
        )
      }
    }
  }

  // Voice Note Recording & View-Once Countdown Engine
  fun startVoiceRecording() {
    val activeId = uiStateFlow.value.chatState.activeConversationId ?: return
    if (voiceAudio.startRecording()) {
      chatRepo.sendTypingIndicator(activeId, "recording_voice")
    }
  }

  fun stopVoiceRecordingForPreview() {
    val activeId = uiStateFlow.value.chatState.activeConversationId
    if (!activeId.isNullOrBlank()) {
      chatRepo.sendTypingIndicator(activeId, "")
    }
    voiceAudio.finishRecording()
  }

  fun discardRecordedVoiceNote() {
    val activeId = uiStateFlow.value.chatState.activeConversationId
    if (!activeId.isNullOrBlank()) {
      chatRepo.sendTypingIndicator(activeId, "")
    }
    voiceAudio.discardRecording()
    updateChat { it.copy(voicePreviewViewOnce = false, voicePreviewTtlSec = 10) }
  }

  fun setVoicePreviewViewOnce(enabled: Boolean) {
    updateChat { it.copy(voicePreviewViewOnce = enabled) }
  }

  fun setVoicePreviewTtlSec(seconds: Int) {
    updateChat { it.copy(voicePreviewTtlSec = seconds.coerceIn(0, 120)) }
  }

  fun toggleVoicePlayback(messageId: String, source: String) {
    if (source.isBlank()) return
    val currentPlayback = uiStateFlow.value.chatState.voicePlayback
    if (currentPlayback.activeMessageId == messageId && currentPlayback.isPlaying) {
      voiceAudio.stopPlayback()
      return
    }
    if (!source.startsWith("http://") && !source.startsWith("https://")) {
      voiceAudio.playFileOrUrl(messageId, source)
      return
    }
    scope.launch {
      val localPath = chatRepo.resolveLocalVoiceFilePath(messageId, "", source)
      if (!localPath.isNullOrBlank()) {
        voiceAudio.playFileOrUrl(messageId, localPath)
      } else {
        updateChat { it.copy(toastMessage = "Unable to load voice note") }
      }
    }
  }

  fun stopVoicePlayback() {
    voiceAudio.stopPlayback()
  }

  fun sendRecordedVoiceNote() {
    val chat = uiStateFlow.value.chatState
    val conv = chat.activeConversation ?: return
    if (conv.isBlocked) {
      voiceAudio.discardRecording()
      updateChat { it.copy(voiceRecording = VoiceRecordingState(), voicePreviewViewOnce = false, toastMessage = "You cannot message this user") }
      return
    }
    val rec = chat.voiceRecording
    if (rec.recordedFilePath.isBlank()) return
    val filePath = rec.recordedFilePath
    val duration = rec.recordedDurationMs
    val viewOnce = chat.voicePreviewViewOnce
    val ttl = chat.voicePreviewTtlSec
    // Clear UI preview immediately without deleting file before repo uploads it
    updateChat { it.copy(voiceRecording = VoiceRecordingState(), voicePreviewViewOnce = false) }
    ChatSoundEffects.playSentPop(soundAllowed(conv.isMuted))

    scope.launch {
      chatRepo.sendVoiceNoteMessage(conv.conversationId, conv.peerUserId, filePath, duration, viewOnce, ttl)
    }
  }

  fun openViewOnceVoiceMessage(msg: ChatMessageEntity) {
    val ownerUid = uiStateFlow.value.userId
    if (msg.senderUserId == ownerUid) {
      updateChat { it.copy(toastMessage = "Only the recipient can open a view-once message") }
      return
    }
    val source = msg.mediaLocalPath.ifBlank { msg.mediaUrl }
    if (source.isBlank()) return

    val ttl = msg.viewOnceTtlSec.coerceAtLeast(0)
    val initialRemaining = if (ttl > 0) ttl else ((msg.voiceDurationMs / 1000L).toInt().coerceAtLeast(1) + 1)

    viewOnceTimerJob?.cancel()
    updateChat {
      it.copy(
        activeViewOnceOverlay = ViewOnceActiveOverlayState(
          messageId = msg.messageId,
          conversationId = msg.conversationId,
          mediaSource = source,
          durationMs = msg.voiceDurationMs,
          ttlSec = ttl,
          remainingSec = initialRemaining,
          startedAtMs = System.currentTimeMillis(),
        ),
      )
    }

    scope.launch {
      chatRepo.openViewOnceVoice(msg.messageId, msg.conversationId)
    }

    if (source.startsWith("http://") || source.startsWith("https://")) {
      scope.launch {
        val localPath = chatRepo.resolveLocalVoiceFilePath(msg.messageId, msg.mediaLocalPath, msg.mediaUrl)
        if (!localPath.isNullOrBlank()) {
          voiceAudio.playFileOrUrl(
            messageId = msg.messageId,
            source = localPath,
            onCompleted = {
              if (ttl == 0) {
                finishAndBurnViewOnce(msg.messageId, msg.conversationId)
              }
            },
          )
        } else {
          updateChat { it.copy(toastMessage = "Unable to load voice note") }
        }
      }
    } else {
      voiceAudio.playFileOrUrl(
        messageId = msg.messageId,
        source = source,
        onCompleted = {
          if (ttl == 0) {
            finishAndBurnViewOnce(msg.messageId, msg.conversationId)
          }
        },
      )
    }

    if (ttl > 0) {
      viewOnceTimerJob = scope.launch {
        var left = ttl
        while (isActive && left > 0) {
          delay(1000L)
          left--
          updateChat { s ->
            val ov = s.activeViewOnceOverlay
            if (ov?.messageId == msg.messageId) s.copy(activeViewOnceOverlay = ov.copy(remainingSec = left)) else s
          }
        }
        finishAndBurnViewOnce(msg.messageId, msg.conversationId)
      }
    }
  }

  fun closeAndBurnActiveViewOnce() {
    val active = uiStateFlow.value.chatState.activeViewOnceOverlay ?: return
    finishAndBurnViewOnce(active.messageId, active.conversationId)
  }

  private fun finishAndBurnViewOnce(messageId: String, conversationId: String) {
    viewOnceTimerJob?.cancel()
    viewOnceTimerJob = null
    voiceAudio.stopPlayback()
    updateChat { it.copy(activeViewOnceOverlay = null) }
    scope.launch {
      chatRepo.consumeViewOnceVoice(messageId, conversationId)
    }
  }

  fun recordScreenshotOrSecurityCapture() {
    val chat = uiStateFlow.value.chatState
    val conv = chat.activeConversation ?: return
    val activeVo = chat.activeViewOnceOverlay
    val blocksShot = uiStateFlow.value.chatPrivacy.blocksScreenshots || !conv.canScreenshot || activeVo != null
    if (!blocksShot) return

    val flagType = if (activeVo != null) "view_once_capture_attempt" else "screenshot_disallowed"
    val details = if (activeVo != null) {
      "Screen capture detected while view-once voice message was open"
    } else {
      "Screenshot capture attempt detected while screenshots are restricted"
    }
    scope.launch {
      chatRepo.recordSecurityFlag(conv.conversationId, activeVo?.messageId.orEmpty(), flagType, details)
    }
  }

  // Gifly Sticker / GIF / Clip Picker
  fun openGiflyPicker(initialTab: String = "stickers") {
    updateChat { it.copy(giflyPickerOpen = true, giflyActiveTab = initialTab, giflyPreviewItem = null) }
    if (uiStateFlow.value.chatState.giflyItems.isEmpty() || uiStateFlow.value.chatState.giflyActiveTab != initialTab) {
      loadGiflyPage(tab = initialTab, query = uiStateFlow.value.chatState.giflyQuery, page = 1, append = false)
    }
  }

  fun closeGiflyPicker() {
    updateChat { it.copy(giflyPickerOpen = false, giflyPreviewItem = null) }
  }

  fun selectGiflyTab(tab: String) {
    updateChat { it.copy(giflyActiveTab = tab, giflyPreviewItem = null) }
    loadGiflyPage(tab = tab, query = uiStateFlow.value.chatState.giflyQuery, page = 1, append = false)
  }

  fun onGiflyQueryChanged(query: String) {
    updateChat { it.copy(giflyQuery = query) }
    giflySearchJob?.cancel()
    giflySearchJob = scope.launch {
      delay(300L)
      loadGiflyPage(tab = uiStateFlow.value.chatState.giflyActiveTab, query = query, page = 1, append = false)
    }
  }

  fun loadNextGiflyPage() {
    val chat = uiStateFlow.value.chatState
    if (!chat.giflyHasNext || chat.giflyIsLoadingMore || chat.giflyIsLoading) return
    loadGiflyPage(chat.giflyActiveTab, chat.giflyQuery, chat.giflyPage + 1, append = true)
  }

  private fun loadGiflyPage(tab: String, query: String, page: Int, append: Boolean) {
    scope.launch {
      updateChat {
        if (append) it.copy(giflyIsLoadingMore = true) else it.copy(giflyIsLoading = true)
      }
      val res = chatRepo.searchGiflyMedia(tab, query, page, 24)
      updateChat { cur ->
        val merged = if (append) (cur.giflyItems + res.items).distinctBy { it.id } else res.items
        cur.copy(
          giflyItems = merged,
          giflyPage = res.page,
          giflyHasNext = res.hasNext,
          giflyIsLoading = false,
          giflyIsLoadingMore = false,
        )
      }
    }
  }

  fun previewGiflyItem(item: GiflyMediaItemDto?) {
    updateChat { it.copy(giflyPreviewItem = item) }
  }

  fun sendPreviewedGiflyItem() {
    val chat = uiStateFlow.value.chatState
    val conv = chat.activeConversation ?: return
    if (conv.isBlocked) {
      updateChat { it.copy(giflyPreviewItem = null, giflyPickerOpen = false, toastMessage = "You cannot message this user") }
      return
    }
    val item = chat.giflyPreviewItem ?: return
    val msgType = when (chat.giflyActiveTab) {
      "gifs" -> "gif"
      "clips" -> "clip"
      else -> "sticker"
    }
    updateChat { it.copy(giflyPreviewItem = null, giflyPickerOpen = false) }
    ChatSoundEffects.playSentPop(soundAllowed(conv.isMuted))
    scope.launch {
      chatRepo.sendGiflyMediaMessage(conv.conversationId, conv.peerUserId, item, msgType)
    }
  }

  // Multi-Select Home Conversations + Undo
  fun enterConversationMultiSelect(initialConvId: String) {
    updateChat { it.copy(isMultiSelectMode = true, selectedConversationIds = setOf(initialConvId)) }
  }

  fun toggleConversationSelection(convId: String) {
    updateChat { s ->
      val next = if (s.selectedConversationIds.contains(convId)) {
        s.selectedConversationIds - convId
      } else {
        s.selectedConversationIds + convId
      }
      if (next.isEmpty()) {
        s.copy(isMultiSelectMode = false, selectedConversationIds = emptySet())
      } else {
        s.copy(selectedConversationIds = next)
      }
    }
  }

  fun selectAllConversations() {
    updateChat { s ->
      s.copy(
        isMultiSelectMode = true,
        selectedConversationIds = s.conversations.map { it.conversationId }.toSet(),
      )
    }
  }

  fun exitConversationMultiSelect() {
    updateChat { it.copy(isMultiSelectMode = false, selectedConversationIds = emptySet()) }
  }

  fun performBatchConversationAction(action: String) {
    val ids = uiStateFlow.value.chatState.selectedConversationIds.toList()
    if (ids.isEmpty()) return
    exitConversationMultiSelect()
    val label = when (action) {
      "delete" -> "Deleted ${ids.size} chat(s)"
      "archive" -> "Archived ${ids.size} chat(s)"
      "mute" -> "Muted ${ids.size} chat(s)"
      "unmute" -> "Unmuted ${ids.size} chat(s)"
      "block" -> "Blocked ${ids.size} contact(s)"
      "mark_read" -> "Marked ${ids.size} read"
      "mark_unread" -> "Marked ${ids.size} unread"
      else -> "Updated"
    }
    scope.launch {
      val snapshot = chatRepo.batchConversationAction(ids, action)
      undoTimeoutJob?.cancel()
      updateChat { it.copy(undoBatchState = UndoChatBatchState(action, label, snapshot)) }
      undoTimeoutJob = scope.launch {
        delay(5_500L)
        updateChat { it.copy(undoBatchState = null) }
      }
    }
  }

  fun toggleMuteActiveConversation() {
    val conv = uiStateFlow.value.chatState.activeConversation ?: return
    val action = if (conv.isMuted) "unmute" else "mute"
    scope.launch {
      chatRepo.batchConversationAction(listOf(conv.conversationId), action)
      updateChat { it.copy(toastMessage = if (conv.isMuted) "Conversation unmuted" else "Conversation muted") }
    }
  }

  fun blockPeerFromActiveChat() {
    val conv = uiStateFlow.value.chatState.activeConversation ?: return
    scope.launch {
      repository.blockUserProfile(conv.peerUserId)
      chatRepo.refreshConversations()
      updateChat { it.copy(toastMessage = "Blocked ${conv.peerFullName}") }
    }
  }

  fun reportPeerFromActiveChat(reason: String, details: String) {
    val conv = uiStateFlow.value.chatState.activeConversation ?: return
    scope.launch {
      val ok = repository.reportUserProfile(conv.peerUserId, reason, details)
      updateChat { it.copy(toastMessage = if (ok) "Report submitted" else "Unable to submit report right now") }
    }
  }

  fun undoLastBatchAction() {
    val undo = uiStateFlow.value.chatState.undoBatchState ?: return
    undoTimeoutJob?.cancel()
    updateChat { it.copy(undoBatchState = null) }
    scope.launch {
      chatRepo.restoreConversationsSnapshot(undo.snapshot, undo.action)
    }
  }

  fun dismissUndoBatch() {
    undoTimeoutJob?.cancel()
    updateChat { it.copy(undoBatchState = null) }
  }

  // Global Home Search (Conversations, Encrypted Messages, Users/Contacts)
  fun openHomeChatSearch() {
    updateChat { it.copy(isHomeSearchOpen = true, homeSearchQuery = "") }
    onHomeChatSearchQueryChanged("")
  }

  fun closeHomeChatSearch() {
    updateChat { it.copy(isHomeSearchOpen = false, homeSearchQuery = "") }
  }

  fun onHomeChatSearchQueryChanged(query: String) {
    updateChat { it.copy(homeSearchQuery = query, isSearchingHomeChats = true) }
    homeSearchJob?.cancel()
    homeSearchJob = scope.launch {
      delay(220L)
      val res = chatRepo.searchChatsAndPeople(query)
      updateChat { it.copy(homeSearchResults = res, isSearchingHomeChats = false) }
    }
  }

  fun openSharedMediaTab(tab: String?) {
    updateChat { it.copy(sharedMediaTab = tab) }
  }

  fun dismissChatToast() {
    updateChat { it.copy(toastMessage = null) }
  }

  fun showChatToast(msg: String) {
    updateChat { it.copy(toastMessage = msg) }
  }

  fun cleanup() {
    seenIncomingMessageIds.clear()
    voiceAudio.stopPlayback()
    voiceAudio.discardRecording()
    viewOnceTimerJob?.cancel()
  }
}
