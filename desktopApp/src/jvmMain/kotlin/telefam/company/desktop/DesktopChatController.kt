package telefam.company.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class DesktopIncomingMessageBanner(
  val conversationId: String,
  val messageId: String,
  val peerUserId: String,
  val senderName: String,
  val previewText: String,
  val isViewOnce: Boolean,
  val canReply: Boolean,
  val createdAtMs: Long,
)

class DesktopChatController(
  private val apiClient: DesktopApiClient,
  private val soundsEnabledProvider: () -> Boolean,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  val audioEngine = DesktopChatAudioEngine()
  private val wsClient = DesktopChatWebSocketClient(apiClient)

  private var ownerUid: String = ""
  private var e2ee: DesktopE2eeEngine? = null
  private var localStore: DesktopChatLocalStore? = null
  private var maintenanceJob: Job? = null
  private var viewOnceBurnJob: Job? = null

  private val _conversations = MutableStateFlow<List<DesktopChatConversation>>(emptyList())
  val conversations: StateFlow<List<DesktopChatConversation>> = _conversations.asStateFlow()

  private val _archivedConversations = MutableStateFlow<List<DesktopChatConversation>>(emptyList())
  val archivedConversations: StateFlow<List<DesktopChatConversation>> = _archivedConversations.asStateFlow()

  private val _activeConversationId = MutableStateFlow<String?>(null)
  val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

  private val _activeMessages = MutableStateFlow<List<DesktopChatMessage>>(emptyList())
  val activeMessages: StateFlow<List<DesktopChatMessage>> = _activeMessages.asStateFlow()

  private val _activeSecurityFlags = MutableStateFlow<List<DesktopChatSecurityFlag>>(emptyList())
  val activeSecurityFlags: StateFlow<List<DesktopChatSecurityFlag>> = _activeSecurityFlags.asStateFlow()

  private val _typingModeByConv = MutableStateFlow<Map<String, String>>(emptyMap())
  val typingModeByConv: StateFlow<Map<String, String>> = _typingModeByConv.asStateFlow()

  private val _greetingSticker = MutableStateFlow<DesktopGiflyMediaItem?>(null)
  val greetingSticker: StateFlow<DesktopGiflyMediaItem?> = _greetingSticker.asStateFlow()

  private val _searchBundle = MutableStateFlow(DesktopChatSearchBundle())
  val searchBundle: StateFlow<DesktopChatSearchBundle> = _searchBundle.asStateFlow()

  private val _undoBanner = MutableStateFlow<DesktopChatUndoBanner?>(null)
  val undoBanner: StateFlow<DesktopChatUndoBanner?> = _undoBanner.asStateFlow()

  private val _dissolvingMessageIds = MutableStateFlow<Set<String>>(emptySet())
  val dissolvingMessageIds: StateFlow<Set<String>> = _dissolvingMessageIds.asStateFlow()

  private val _playingMessageId = MutableStateFlow<String?>(null)
  val playingMessageId: StateFlow<String?> = _playingMessageId.asStateFlow()

  private val _activeViewOnceMsg = MutableStateFlow<DesktopChatMessage?>(null)
  val activeViewOnceMsg: StateFlow<DesktopChatMessage?> = _activeViewOnceMsg.asStateFlow()

  private val _viewOnceRemainingSec = MutableStateFlow(0)
  val viewOnceRemainingSec: StateFlow<Int> = _viewOnceRemainingSec.asStateFlow()

  private val _incomingBanner = MutableStateFlow<DesktopIncomingMessageBanner?>(null)
  val incomingBanner: StateFlow<DesktopIncomingMessageBanner?> = _incomingBanner.asStateFlow()

  val wsEvents = wsClient.events

  fun sendCallSignalViaWs(callId: String, fieldsJson: String): Boolean =
    wsClient.sendCallSignal(callId, fieldsJson)

  fun refreshConversationNow(conversationId: String) {
    val clean = conversationId.trim()
    if (clean.isBlank()) return
    scope.launch {
      syncConversationMessages(clean, markAsRead = (_activeConversationId.value == clean))
      refreshConversations(false)
    }
  }

  init {
    scope.launch {
      wsClient.events.collect { ev -> handleWsEvent(ev) }
    }
  }

  fun attachSession(userId: String) {
    val clean = userId.trim()
    if (clean.isBlank()) {
      detachSession()
      return
    }
    if (ownerUid == clean && localStore != null && e2ee != null) return
    ownerUid = clean
    e2ee = DesktopE2eeEngine(clean)
    localStore = DesktopChatLocalStore(clean)
    publishConversationsFromStore()
    wsClient.connect()
    startMaintenanceLoop()
    scope.launch {
      ensureKeysUploaded()
      refreshConversations(archived = false)
      refreshConversations(archived = true)
      flushPendingOps()
    }
  }

  fun detachSession() {
    maintenanceJob?.cancel()
    maintenanceJob = null
    viewOnceBurnJob?.cancel()
    viewOnceBurnJob = null
    wsClient.disconnect()
    audioEngine.stopPlayback()
    audioEngine.stopRecording(discard = true)
    ownerUid = ""
    e2ee = null
    localStore = null
    _conversations.value = emptyList()
    _archivedConversations.value = emptyList()
    _activeConversationId.value = null
    _activeMessages.value = emptyList()
    _activeSecurityFlags.value = emptyList()
    _typingModeByConv.value = emptyMap()
    _greetingSticker.value = null
    _searchBundle.value = DesktopChatSearchBundle()
    _undoBanner.value = null
    _dissolvingMessageIds.value = emptySet()
    _playingMessageId.value = null
    _activeViewOnceMsg.value = null
    _viewOnceRemainingSec.value = 0
    _incomingBanner.value = null
  }

  fun currentConversation(): DesktopChatConversation? {
    val cid = _activeConversationId.value ?: return null
    val store = localStore ?: return null
    return store.conversations[cid]
  }

  fun openConversation(conversationId: String) {
    val clean = conversationId.trim()
    if (clean.isBlank()) return
    _activeConversationId.value = clean
    localStore?.let { store ->
      store.conversations[clean]?.let { conv ->
        if (conv.unreadCount > 0) {
          store.conversations[clean] = conv.copy(unreadCount = 0)
          store.save()
          publishConversationsFromStore()
        }
      }
    }
    publishMessagesFromStore(clean)
    scope.launch {
      val conv = localStore?.conversations?.get(clean)
      if (conv != null) fetchGreeting(conv.peerUserId)
      syncConversationMessages(clean, markAsRead = true)
    }
  }

  fun dismissIncomingBanner() {
    _incomingBanner.value = null
  }

  fun markConversationReadFromBanner(conversationId: String) {
    _incomingBanner.value = null
    val store = localStore ?: return
    store.conversations[conversationId]?.let { cur ->
      store.conversations[conversationId] = cur.copy(unreadCount = 0)
      store.save()
      publishConversationsFromStore()
    }
    scope.launch {
      val showReceipts = store.conversations[conversationId]?.capabilities?.showReadReceipts ?: true
      val incoming = store.messages.values
        .filter { it.conversationId == conversationId && it.senderUserId != ownerUid && it.deliveryStatus != "seen" }
        .sortedBy { it.createdAtMs }
      val target = incoming.lastOrNull()
      if (target != null) {
        val status = if (showReceipts) "seen" else "delivered"
        apiClient.executeChatRaw(
          "POST",
          "/v1/chat/messages/${DesktopChatCodec.urlEncode(target.messageId)}/receipt",
          "{\"status\":\"$status\"}",
        )
        if (showReceipts) {
          incoming.forEach { m -> store.messages[m.messageId] = m.copy(deliveryStatus = "seen") }
          store.save()
        }
      }
      apiClient.executeChatRaw(
        "POST",
        "/v1/chat/conversations/batch-action",
        "{\"conversation_ids\":[\"${DesktopJson.escape(conversationId)}\"],\"action\":\"mark_read\"}",
      )
      apiClient.markNotificationRead(conversationId = conversationId)
    }
  }

  fun sendQuickReplyFromBanner(conversationId: String, peerUserId: String, text: String) {
    val clean = text.trim()
    if (clean.isBlank()) return
    _incomingBanner.value = null
    markConversationReadFromBanner(conversationId)
    sendTextMessage(conversationId, peerUserId, clean)
  }

  fun closeActiveConversation() {
    val prev = _activeConversationId.value.orEmpty()
    if (prev.isNotBlank()) wsClient.sendTyping(prev, "idle")
    audioEngine.stopPlayback()
    _playingMessageId.value = null
    _activeConversationId.value = null
    _activeMessages.value = emptyList()
    _activeSecurityFlags.value = emptyList()
  }

  fun openDirectConversationByUser(
    peerUserId: String,
    peerName: String,
    peerUsername: String,
    peerAvatar: String,
    onOpened: (String) -> Unit,
  ) {
    val store = localStore ?: return
    val uid = peerUserId.trim()
    if (uid.isBlank()) return
    scope.launch {
      val existing = store.conversations.values.firstOrNull { it.peerUserId == uid && !it.isDeletedLocally }
      val resp = apiClient.executeChatRaw("POST", "/v1/chat/conversations/direct/${DesktopChatCodec.urlEncode(uid)}")
      val convId = if (resp != null && resp.first in 200..299) {
        val m = DesktopJson.parseObject(resp.second)
        val cObj = DesktopJson.objOrNull(m, "conversation") ?: m
        val parsed = parseConversationDto(cObj)
        if (parsed.conversationId.isNotBlank()) {
          store.conversations[parsed.conversationId] = parsed
          store.save()
          publishConversationsFromStore()
          parsed.conversationId
        } else {
          existing?.conversationId ?: createLocalPlaceholderConv(uid, peerName, peerUsername, peerAvatar)
        }
      } else {
        existing?.conversationId ?: createLocalPlaceholderConv(uid, peerName, peerUsername, peerAvatar)
      }
      withContext(Dispatchers.Main) {
        openConversation(convId)
        onOpened(convId)
      }
    }
  }

  private fun createLocalPlaceholderConv(
    peerUserId: String,
    peerName: String,
    peerUsername: String,
    peerAvatar: String,
  ): String {
    val store = localStore ?: return "conv_$peerUserId"
    val id = "local_direct_$peerUserId"
    if (!store.conversations.containsKey(id)) {
      store.conversations[id] = DesktopChatConversation(
        conversationId = id,
        peerUserId = peerUserId,
        peerFullName = peerName.ifBlank { peerUsername.ifBlank { "Telefam user" } },
        peerUsername = peerUsername,
        peerAvatarUrl = peerAvatar,
        updatedAtMs = System.currentTimeMillis(),
      )
      store.save()
      publishConversationsFromStore()
    }
    return id
  }

  fun notifyTyping(conversationId: String, mode: String) {
    wsClient.sendTyping(conversationId, mode)
  }

  fun sendTextMessage(conversationId: String, peerUserId: String, text: String, forwardedFrom: String = "") {
    val clean = text.trim()
    if (clean.isBlank()) return
    sendEnvelope(conversationId, peerUserId, "text", clean, forwardedFrom = forwardedFrom)
  }

  fun sendGiflyMediaMessage(
    conversationId: String,
    peerUserId: String,
    item: DesktopGiflyMediaItem,
    tabKind: DesktopGiflyTab,
  ) {
    val msgType = when (tabKind) {
      DesktopGiflyTab.STICKERS -> "sticker"
      DesktopGiflyTab.GIFS -> "gif"
      DesktopGiflyTab.CLIPS -> "clip"
    }
    sendEnvelope(
      conversationId = conversationId,
      peerUserId = peerUserId,
      messageType = msgType,
      plainText = item.title.ifBlank { msgType.replaceFirstChar { it.uppercase() } },
      mediaUrl = item.bestAnimatedUrl,
      mediaId = item.slug.ifBlank { item.id.toString() },
    )
  }

  fun sendVoiceMessage(
    conversationId: String,
    peerUserId: String,
    wavFile: File,
    durationMs: Long,
    isViewOnce: Boolean,
    ttlSec: Int,
  ) {
    sendEnvelope(
      conversationId = conversationId,
      peerUserId = peerUserId,
      messageType = if (isViewOnce) "voice_once" else "voice",
      plainText = if (isViewOnce) "View-once voice note" else "Voice note",
      mediaLocalPath = wavFile.absolutePath,
      voiceDurationMs = durationMs,
      viewOnceTtlSec = if (isViewOnce) ttlSec else 0,
    )
  }

  private fun sendEnvelope(
    conversationId: String,
    peerUserId: String,
    messageType: String,
    plainText: String,
    mediaUrl: String = "",
    mediaLocalPath: String = "",
    mediaId: String = "",
    voiceDurationMs: Long = 0L,
    viewOnceTtlSec: Int = 0,
    forwardedFrom: String = "",
  ) {
    val store = localStore ?: return
    val conv = store.conversations[conversationId]
    if (conv?.capabilities?.isBlocked == true) return
    val nowMs = System.currentTimeMillis()
    val opId = "op_${UUID.randomUUID()}"
    val localMsg = DesktopChatMessage(
      messageId = opId,
      conversationId = conversationId,
      senderUserId = ownerUid,
      recipientUserId = peerUserId,
      clientOpId = opId,
      messageType = messageType,
      plainText = plainText,
      mediaUrl = mediaUrl,
      mediaLocalPath = mediaLocalPath,
      mediaId = mediaId,
      voiceDurationMs = voiceDurationMs,
      viewOnceTtlSec = viewOnceTtlSec,
      forwardedFromUserId = forwardedFrom,
      deliveryStatus = "sending",
      syncState = "pending",
      createdAtMs = nowMs,
    )
    // Instant local commit so offline or slow network feels instantaneous
    store.messages[localMsg.messageId] = localMsg
    if (conv != null) {
      store.conversations[conversationId] = conv.copy(
        lastMessageId = localMsg.messageId,
        lastMessageType = messageType,
        lastSenderId = ownerUid,
        lastMessagePreviewPlain = previewTextFor(localMsg),
        lastDeliveryStatus = "sending",
        lastMessageAtMs = nowMs,
        updatedAtMs = nowMs,
      )
    }
    store.save()
    publishConversationsFromStore()
    if (_activeConversationId.value == conversationId) publishMessagesFromStore(conversationId)
    audioEngine.playSentSound(soundsEnabledProvider() && conv?.isMuted != true)

    scope.launch {
      val delivered = tryDeliverMessage(localMsg)
      if (!delivered) {
        store.pendingOps[opId] = DesktopPendingChatOp(
          opId = opId,
          opKind = "send",
          conversationId = conversationId,
          targetMessageId = localMsg.messageId,
          payloadJson = "{}",
          createdAtMs = nowMs,
          nextRetryAtMs = nowMs + 2_000L,
        )
        store.save()
      }
    }
  }

  private fun tryDeliverMessage(msg: DesktopChatMessage): Boolean {
    val store = localStore ?: return false
    val engine = e2ee ?: return false
    ensureKeysUploaded()

    var resolvedMediaId = msg.mediaId
    var resolvedMediaUrl = msg.mediaUrl
    if ((msg.messageType == "voice" || msg.messageType == "voice_once") &&
      resolvedMediaUrl.isBlank() && msg.mediaLocalPath.isNotBlank()
    ) {
      val f = File(msg.mediaLocalPath)
      if (!f.exists()) return false
      val up = apiClient.uploadVoiceMediaMultipart(
        conversationId = msg.conversationId,
        durationMs = msg.voiceDurationMs,
        viewOnce = msg.messageType == "voice_once",
        audioBytes = f.readBytes(),
        filename = f.name,
      ) ?: return false
      if (up.first !in 200..299) return false
      val m = DesktopJson.parseObject(up.second)
      resolvedMediaId = DesktopJson.str(m, "media_id")
      resolvedMediaUrl = DesktopJson.str(m, "media_url")
      if (resolvedMediaUrl.isBlank()) return false
    }

    val peerKey = fetchPeerEncryptionKey(msg.recipientUserId) ?: return false
    var targetConvId = msg.conversationId
    if (targetConvId.startsWith("local_direct_")) {
      val convResp = apiClient.executeChatRaw("POST", "/v1/chat/conversations/direct/${DesktopChatCodec.urlEncode(msg.recipientUserId)}")
      if (convResp != null && convResp.first in 200..299) {
        val parsed = parseConversationDto(DesktopJson.objOrNull(DesktopJson.parseObject(convResp.second), "conversation") ?: emptyMap())
        if (parsed.conversationId.isNotBlank()) {
          targetConvId = parsed.conversationId
          store.conversations.remove(msg.conversationId)
          store.conversations[targetConvId] = parsed
        }
      }
    }

    val env = runCatching { engine.compressAndEncrypt(peerKey, msg.plainText) }.getOrNull() ?: return false
    val body = buildString {
      append('{')
      append("\"client_op_id\":\"").append(DesktopJson.escape(msg.clientOpId)).append("\",")
      append("\"message_type\":\"").append(DesktopJson.escape(msg.messageType)).append("\",")
      append("\"compression_codec\":\"").append(env.compressionCodec).append("\",")
      append("\"encryption_scheme\":\"").append(env.encryptionScheme).append("\",")
      append("\"ciphertext_b64\":\"").append(DesktopJson.escape(env.ciphertextB64)).append("\",")
      append("\"sender_ciphertext_b64\":\"").append(DesktopJson.escape(env.senderCiphertextB64)).append("\",")
      append("\"signal_msg_type\":").append(env.signalMsgType).append(',')
      append("\"media_url\":\"").append(DesktopJson.escape(resolvedMediaUrl)).append("\",")
      append("\"media_id\":\"").append(DesktopJson.escape(resolvedMediaId)).append("\",")
      append("\"voice_duration_ms\":").append(msg.voiceDurationMs).append(',')
      append("\"view_once_ttl_sec\":").append(msg.viewOnceTtlSec).append(',')
      append("\"forwarded_from_user_id\":\"").append(DesktopJson.escape(msg.forwardedFromUserId)).append('"')
      append('}')
    }
    val sendResp = apiClient.executeChatRaw(
      method = "POST",
      path = "/v1/chat/conversations/${DesktopChatCodec.urlEncode(targetConvId)}/messages",
      jsonBody = body,
    ) ?: return false
    if (sendResp.first !in 200..299) return false
    val mObj = DesktopJson.objOrNull(DesktopJson.parseObject(sendResp.second), "message") ?: return false
    val dto = parseMessageDto(mObj, msg.plainText, msg.mediaLocalPath).copy(syncState = "synced")
    if (dto.messageId.isBlank()) return false

    if (targetConvId != msg.conversationId) {
      store.messages.entries
        .filter { it.value.conversationId == msg.conversationId }
        .toList()
        .forEach { (mid, existingMsg) ->
          store.messages[mid] = existingMsg.copy(conversationId = targetConvId)
        }
    }
    store.messages.remove(msg.messageId)
    store.messages[dto.messageId] = dto
    store.conversations[targetConvId]?.let { c ->
      store.conversations[targetConvId] = c.copy(
        lastMessageId = dto.messageId,
        lastMessageType = dto.messageType,
        lastSenderId = dto.senderUserId,
        lastDeliveryStatus = dto.deliveryStatus,
        lastMessagePreviewPlain = previewTextFor(dto),
        lastMessageAtMs = dto.createdAtMs,
        updatedAtMs = dto.createdAtMs,
      )
    }
    store.save()
    publishConversationsFromStore()
    if (_activeConversationId.value == targetConvId || _activeConversationId.value == msg.conversationId) {
      _activeConversationId.value = targetConvId
      publishMessagesFromStore(targetConvId)
    }
    return true
  }

  fun editMessage(msg: DesktopChatMessage, newPlainText: String) {
    val store = localStore ?: return
    val clean = newPlainText.trim()
    if (clean.isBlank()) return
    val nowMs = System.currentTimeMillis()
    store.messages[msg.messageId] = msg.copy(plainText = clean, editedAtMs = nowMs)
    refreshConvPreview(msg.conversationId)
    store.save()
    publishConversationsFromStore()
    if (_activeConversationId.value == msg.conversationId) publishMessagesFromStore(msg.conversationId)

    scope.launch {
      val ok = sendRemoteEdit(msg.messageId, msg.recipientUserId, clean)
      if (!ok) {
        store.pendingOps["edit_${msg.messageId}"] = DesktopPendingChatOp(
          opId = "edit_${msg.messageId}",
          opKind = "edit",
          conversationId = msg.conversationId,
          targetMessageId = msg.messageId,
          payloadJson = "{\"peer_user_id\":\"${DesktopJson.escape(msg.recipientUserId)}\",\"plain_text\":\"${DesktopJson.escape(clean)}\"}",
          createdAtMs = nowMs,
          nextRetryAtMs = nowMs + 2_500L,
        )
        store.save()
      }
    }
  }

  private fun sendRemoteEdit(messageId: String, peerUserId: String, plainText: String): Boolean {
    val engine = e2ee ?: return false
    val peerKey = fetchPeerEncryptionKey(peerUserId) ?: return false
    val env = runCatching { engine.compressAndEncrypt(peerKey, plainText) }.getOrNull() ?: return false
    val payload = "{\"ciphertext_b64\":\"${DesktopJson.escape(env.ciphertextB64)}\",\"sender_ciphertext_b64\":\"${DesktopJson.escape(env.senderCiphertextB64)}\",\"signal_msg_type\":${env.signalMsgType}}"
    val resp = apiClient.executeChatRaw("PATCH", "/v1/chat/messages/${DesktopChatCodec.urlEncode(messageId)}", payload)
    return resp != null && resp.first in 200..299
  }

  fun unsendMessage(msg: DesktopChatMessage) {
    val store = localStore ?: return
    _dissolvingMessageIds.value = _dissolvingMessageIds.value + msg.messageId
    scope.launch {
      delay(440L) // Sparkle dissolve animation before full permanent purge
      if (msg.mediaLocalPath.isNotBlank()) runCatching { File(msg.mediaLocalPath).delete() }
      store.messages.remove(msg.messageId)
      refreshConvPreview(msg.conversationId)
      store.save()
      publishConversationsFromStore()
      if (_activeConversationId.value == msg.conversationId) publishMessagesFromStore(msg.conversationId)
      _dissolvingMessageIds.value = _dissolvingMessageIds.value - msg.messageId

      val resp = apiClient.executeChatRaw("DELETE", "/v1/chat/messages/${DesktopChatCodec.urlEncode(msg.messageId)}?mode=unsend")
      if (resp == null || (resp.first !in 200..299 && resp.first != 404)) {
        val now = System.currentTimeMillis()
        store.pendingOps["unsend_${msg.messageId}"] = DesktopPendingChatOp(
          opId = "unsend_${msg.messageId}",
          opKind = "unsend",
          conversationId = msg.conversationId,
          targetMessageId = msg.messageId,
          createdAtMs = now,
          nextRetryAtMs = now + 2_500L,
        )
        store.save()
      }
    }
  }

  fun deleteMessageForSelf(msg: DesktopChatMessage) {
    val store = localStore ?: return
    if (msg.mediaLocalPath.isNotBlank()) runCatching { File(msg.mediaLocalPath).delete() }
    store.messages.remove(msg.messageId)
    refreshConvPreview(msg.conversationId)
    store.save()
    publishConversationsFromStore()
    if (_activeConversationId.value == msg.conversationId) publishMessagesFromStore(msg.conversationId)

    scope.launch {
      val resp = apiClient.executeChatRaw("DELETE", "/v1/chat/messages/${DesktopChatCodec.urlEncode(msg.messageId)}?mode=self")
      if (resp == null || (resp.first !in 200..299 && resp.first != 404)) {
        val now = System.currentTimeMillis()
        store.pendingOps["delself_${msg.messageId}"] = DesktopPendingChatOp(
          opId = "delself_${msg.messageId}",
          opKind = "delete_self",
          conversationId = msg.conversationId,
          targetMessageId = msg.messageId,
          createdAtMs = now,
          nextRetryAtMs = now + 2_500L,
        )
        store.save()
      }
    }
  }

  fun batchConversationAction(ids: Set<String>, action: String) {
    val store = localStore ?: return
    val idList = ids.toList()
    if (idList.isEmpty()) return
    val snapshot = idList.mapNotNull { store.conversations[it] }
    idList.forEach { cid ->
      val cur = store.conversations[cid] ?: return@forEach
      store.conversations[cid] = when (action) {
        "delete" -> cur.copy(isDeletedLocally = true)
        "archive" -> cur.copy(isArchived = true)
        "unarchive" -> cur.copy(isArchived = false)
        "mute" -> cur.copy(isMuted = true)
        "unmute" -> cur.copy(isMuted = false)
        "mark_read" -> cur.copy(unreadCount = 0)
        "mark_unread" -> cur.copy(unreadCount = if (cur.unreadCount == 0) 1 else cur.unreadCount)
        "block" -> cur.copy(capabilities = cur.capabilities.copy(isBlocked = true))
        else -> cur
      }
    }
    store.save()
    publishConversationsFromStore()

    val label = when (action) {
      "delete" -> "Deleted ${idList.size} chat(s)"
      "archive" -> "Archived ${idList.size} chat(s)"
      "mute" -> "Muted ${idList.size} chat(s)"
      "mark_read" -> "Marked read"
      "mark_unread" -> "Marked unread"
      "block" -> "Blocked ${idList.size} contact(s)"
      else -> "Updated ${idList.size} chat(s)"
    }
    if (action != "block") {
      _undoBanner.value = DesktopChatUndoBanner(label, action, snapshot)
    }
    scope.launch {
      val idsJson = idList.joinToString(",") { "\"${DesktopJson.escape(it)}\"" }
      apiClient.executeChatRaw("POST", "/v1/chat/conversations/batch-action", "{\"conversation_ids\":[$idsJson],\"action\":\"$action\"}")
    }
  }

  fun undoBatchAction() {
    val banner = _undoBanner.value ?: return
    val store = localStore ?: return
    _undoBanner.value = null
    banner.snapshot.forEach { store.conversations[it.conversationId] = it }
    store.save()
    publishConversationsFromStore()
    val inverse = when (banner.previousAction) {
      "archive" -> "unarchive"
      "mute" -> "unmute"
      "mark_read" -> "mark_unread"
      "mark_unread" -> "mark_read"
      "delete" -> "unarchive"
      else -> null
    } ?: return
    scope.launch {
      val idsJson = banner.snapshot.joinToString(",") { "\"${DesktopJson.escape(it.conversationId)}\"" }
      apiClient.executeChatRaw("POST", "/v1/chat/conversations/batch-action", "{\"conversation_ids\":[$idsJson],\"action\":\"$inverse\"}")
    }
  }

  fun dismissUndoBanner() {
    _undoBanner.value = null
  }

  fun togglePlayVoice(msg: DesktopChatMessage) {
    if (msg.messageType == "voice_once") {
      openViewOnceVoice(msg)
      return
    }
    if (_playingMessageId.value == msg.messageId) {
      audioEngine.stopPlayback()
      _playingMessageId.value = null
      return
    }
    scope.launch {
      val file = ensureLocalVoiceFile(msg) ?: return@launch
      _playingMessageId.value = msg.messageId
      val ok = audioEngine.playVoiceFile(msg.messageId, file) {
        _playingMessageId.value = null
      }
      if (!ok) _playingMessageId.value = null
    }
  }

  fun openViewOnceVoice(msg: DesktopChatMessage) {
    if (msg.senderUserId == ownerUid) return // Sender cannot open own view-once
    val store = localStore ?: return
    scope.launch {
      val file = ensureLocalVoiceFile(msg) ?: return@launch
      val nowMs = System.currentTimeMillis()
      val expires = if (msg.viewOnceTtlSec > 0) nowMs + msg.viewOnceTtlSec * 1000L else 0L
      val updated = msg.copy(viewOnceOpenedAtMs = nowMs, viewOnceExpiresAtMs = expires)
      store.messages[msg.messageId] = updated
      recordSecurityFlagInternal(
        convId = msg.conversationId,
        msgId = msg.messageId,
        flagType = "view_once_opened",
        details = "View-once voice note opened (${if (msg.viewOnceTtlSec > 0) "${msg.viewOnceTtlSec}s timer" else "play once"})",
      )
      apiClient.executeChatRaw("POST", "/v1/chat/messages/${DesktopChatCodec.urlEncode(msg.messageId)}/view-once/open", "{}")

      _activeViewOnceMsg.value = updated
      _playingMessageId.value = updated.messageId
      audioEngine.playVoiceFile(updated.messageId, file) {
        consumeViewOnceVoice(updated)
      }

      viewOnceBurnJob?.cancel()
      if (updated.viewOnceTtlSec > 0) {
        viewOnceBurnJob = scope.launch {
          for (sec in updated.viewOnceTtlSec downTo 1) {
            _viewOnceRemainingSec.value = sec
            delay(1000L)
          }
          _viewOnceRemainingSec.value = 0
          consumeViewOnceVoice(updated)
        }
      } else {
        _viewOnceRemainingSec.value = 0
      }
    }
  }

  fun consumeViewOnceVoice(msg: DesktopChatMessage) {
    viewOnceBurnJob?.cancel()
    viewOnceBurnJob = null
    audioEngine.stopPlayback()
    _playingMessageId.value = null
    _activeViewOnceMsg.value = null
    val store = localStore ?: return
    if (msg.mediaLocalPath.isNotBlank()) runCatching { File(msg.mediaLocalPath).delete() }
    store.messages.remove(msg.messageId)
    refreshConvPreview(msg.conversationId)
    store.save()
    publishConversationsFromStore()
    if (_activeConversationId.value == msg.conversationId) publishMessagesFromStore(msg.conversationId)

    scope.launch {
      val resp = apiClient.executeChatRaw("POST", "/v1/chat/messages/${DesktopChatCodec.urlEncode(msg.messageId)}/view-once/consume", "{}")
      if (resp == null || (resp.first !in 200..299 && resp.first != 404)) {
        val now = System.currentTimeMillis()
        store.pendingOps["consume_${msg.messageId}"] = DesktopPendingChatOp(
          opId = "consume_${msg.messageId}",
          opKind = "consume_view_once",
          conversationId = msg.conversationId,
          targetMessageId = msg.messageId,
          createdAtMs = now,
          nextRetryAtMs = now + 2_000L,
        )
        store.save()
      }
    }
  }

  fun recordSecurityFlag(convId: String, msgId: String, flagType: String, details: String) {
    scope.launch { recordSecurityFlagInternal(convId, msgId, flagType, details) }
  }

  private fun recordSecurityFlagInternal(convId: String, msgId: String, flagType: String, details: String) {
    val store = localStore ?: return
    val target = store.conversations[convId]?.peerUserId.orEmpty()
    val flag = DesktopChatSecurityFlag(
      flagId = UUID.randomUUID().toString(),
      conversationId = convId,
      messageId = msgId,
      actorUserId = ownerUid,
      targetUserId = target,
      flagType = flagType,
      details = details,
      isSynced = false,
      createdAtMs = System.currentTimeMillis(),
    )
    store.flags[flag.flagId] = flag
    store.save()
    if (_activeConversationId.value == convId) publishMessagesFromStore(convId)

    val payload = "{\"message_id\":\"${DesktopJson.escape(msgId)}\",\"flag_type\":\"${DesktopJson.escape(flagType)}\",\"details\":\"${DesktopJson.escape(details)}\"}"
    val resp = apiClient.executeChatRaw("POST", "/v1/chat/conversations/${DesktopChatCodec.urlEncode(convId)}/flags", payload)
    if (resp != null && resp.first in 200..299) {
      store.flags[flag.flagId] = flag.copy(isSynced = true)
      store.save()
    }
  }

  fun runGlobalSearch(query: String) {
    val store = localStore ?: return
    val clean = query.trim()
    val convMatches = store.conversations.values.filter { !it.isDeletedLocally }.filter {
      clean.isBlank() || it.peerFullName.contains(clean, true) ||
        it.peerUsername.contains(clean, true) || it.lastMessagePreviewPlain.contains(clean, true)
    }.sortedByDescending { it.lastMessageAtMs }
    val msgMatches = if (clean.isBlank()) emptyList() else {
      store.messages.values.filter { it.plainText.contains(clean, true) }.sortedByDescending { it.createdAtMs }.take(25)
    }
    _searchBundle.value = DesktopChatSearchBundle(conversations = convMatches, matchingMessages = msgMatches)
    if (clean.isNotBlank()) {
      scope.launch {
        val resp = apiClient.executeChatRaw("GET", "/v1/chat/search?q=${DesktopChatCodec.urlEncode(clean)}")
        if (resp != null && resp.first in 200..299) {
          val m = DesktopJson.parseObject(resp.second)
          val people = DesktopJson.objList(m, "people").map { p ->
            DesktopChatSearchPerson(
              userId = DesktopJson.str(p, "user_id"),
              fullName = DesktopJson.str(p, "full_name"),
              username = DesktopJson.str(p, "username"),
              avatarUrl = DesktopJson.str(p, "avatar_url"),
              location = DesktopJson.str(p, "location"),
              conversationId = DesktopJson.str(p, "conversation_id"),
            )
          }
          _searchBundle.value = _searchBundle.value.copy(people = people)
        }
      }
    }
  }

  suspend fun searchGifly(tab: DesktopGiflyTab, query: String, page: Int): Pair<List<DesktopGiflyMediaItem>, Boolean> =
    withContext(Dispatchers.IO) {
      val path = "/v1/chat/stickers/search?type=${tab.apiValue}&q=${DesktopChatCodec.urlEncode(query.trim())}&page=$page&per_page=24"
      val resp = apiClient.executeChatRaw("GET", path) ?: return@withContext emptyList<DesktopGiflyMediaItem>() to false
      if (resp.first !in 200..299) return@withContext emptyList<DesktopGiflyMediaItem>() to false
      val m = DesktopJson.parseObject(resp.second)
      val items = DesktopJson.objList(m, "items").map { DesktopChatCodec.parseGiflyItem(it) }
      items to DesktopJson.bool(m, "has_next")
    }

  private fun fetchGreeting(peerUserId: String) {
    val resp = apiClient.executeChatRaw("GET", "/v1/chat/stickers/greeting?peer_id=${DesktopChatCodec.urlEncode(peerUserId)}") ?: return
    if (resp.first in 200..299) {
      val item = DesktopJson.objOrNull(DesktopJson.parseObject(resp.second), "item") ?: return
      val parsed = DesktopChatCodec.parseGiflyItem(item)
      if (parsed.bestAnimatedUrl.isNotBlank()) _greetingSticker.value = parsed
    }
  }

  private fun ensureKeysUploaded() {
    val engine = e2ee ?: return
    if (engine.keysUploaded) return
    val resp = apiClient.executeChatRaw("PUT", "/v1/chat/e2ee/keys", engine.exportUploadJson())
    if (resp != null && resp.first in 200..299) engine.markUploaded()
  }

  private fun fetchPeerEncryptionKey(peerUserId: String): String? {
    val store = localStore ?: return null
    store.peerBundles[peerUserId]?.let { b ->
      return b.signedPreKeyPubB64.ifBlank { b.identityPubB64 }
    }
    val resp = apiClient.executeChatRaw("GET", "/v1/chat/e2ee/keys/${DesktopChatCodec.urlEncode(peerUserId)}") ?: return null
    if (resp.first !in 200..299) return null
    val m = DesktopJson.parseObject(resp.second)
    val b = DesktopPeerE2eeBundle(
      userId = DesktopJson.str(m, "user_id").ifBlank { peerUserId },
      deviceId = DesktopJson.long(m, "device_id", 1L).toInt(),
      registrationId = DesktopJson.long(m, "registration_id", 0L).toInt(),
      identityPubB64 = DesktopJson.str(m, "identity_pub_b64"),
      signedPreKeyId = DesktopJson.long(m, "signed_prekey_id", 1L).toInt(),
      signedPreKeyPubB64 = DesktopJson.str(m, "signed_prekey_pub_b64"),
      signedPreKeySigB64 = DesktopJson.str(m, "signed_prekey_sig_b64"),
      kyberPreKeyPubB64 = DesktopJson.str(m, "kyber_prekey_pub_b64"),
      preKeyId = DesktopJson.long(m, "prekey_id", 0L).toInt(),
      preKeyPubB64 = DesktopJson.str(m, "prekey_pub_b64"),
    )
    store.peerBundles[peerUserId] = b
    return b.signedPreKeyPubB64.ifBlank { b.identityPubB64 }.takeIf { it.isNotBlank() }
  }

  private fun refreshConversations(archived: Boolean) {
    val store = localStore ?: return
    val resp = apiClient.executeChatRaw("GET", "/v1/chat/conversations?archived=$archived") ?: return
    if (resp.first !in 200..299) return
    val arr = DesktopJson.objList(DesktopJson.parseObject(resp.second), "conversations")
    var changed = false
    arr.forEach { m ->
      val parsed = parseConversationDto(m)
      if (parsed.conversationId.isNotBlank()) {
        val prev = store.conversations[parsed.conversationId]
        store.conversations[parsed.conversationId] = parsed.copy(
          lastMessagePreviewPlain = parsed.lastMessagePreviewPlain.ifBlank { prev?.lastMessagePreviewPlain.orEmpty() },
          isDeletedLocally = prev?.isDeletedLocally == true && parsed.lastMessageAtMs <= prev.lastMessageAtMs,
        )
        changed = true
      }
    }
    if (changed) {
      store.save()
      publishConversationsFromStore()
    }
  }

  private fun syncConversationMessages(convId: String, markAsRead: Boolean = false) {
    val store = localStore ?: return
    val sinceMs = store.messages.values
      .filter { it.conversationId == convId && it.syncState == "synced" }
      .maxOfOrNull { maxOf(it.createdAtMs, it.editedAtMs) } ?: 0L
    val query = if (sinceMs > 0L) "?since_ms=$sinceMs" else ""
    val resp = apiClient.executeChatRaw("GET", "/v1/chat/conversations/${DesktopChatCodec.urlEncode(convId)}/messages$query") ?: return
    if (resp.first !in 200..299) return
    val root = DesktopJson.parseObject(resp.second)
    DesktopJson.objOrNull(root, "conversation")?.let { cObj ->
      val prev = store.conversations[convId]
      val parsed = parseConversationDto(cObj)
      if (parsed.conversationId.isNotBlank()) {
        store.conversations[parsed.conversationId] = parsed.copy(
          unreadCount = if (markAsRead) 0 else parsed.unreadCount,
          lastMessagePreviewPlain = parsed.lastMessagePreviewPlain.ifBlank { prev?.lastMessagePreviewPlain.orEmpty() },
        )
      }
    }
    var newIncoming = false
    val ackDeliveredIds = mutableListOf<String>()
    val ackSeenIds = mutableListOf<String>()

    DesktopJson.objList(root, "messages").forEach { m ->
      val serverId = DesktopJson.str(m, "message_id")
      val clientOpId = DesktopJson.str(m, "client_op_id")
      val matchingPending = if (clientOpId.isNotBlank()) {
        store.messages.values.firstOrNull {
          it.messageId != serverId && (it.messageId == clientOpId || it.clientOpId == clientOpId)
        }
      } else null
      val existing = store.messages[serverId] ?: matchingPending
      val dto = parseMessageDto(
        m,
        fallbackPlain = existing?.plainText.orEmpty(),
        localPath = existing?.mediaLocalPath.orEmpty(),
      )
      if (dto.messageId.isNotBlank()) {
        if (matchingPending != null) {
          store.messages.remove(matchingPending.messageId)
        }
        if (existing == null && dto.senderUserId != ownerUid) newIncoming = true
        store.messages[dto.messageId] = dto
        if (dto.senderUserId != ownerUid && dto.deliveryStatus != "seen") {
          if (markAsRead) {
            ackSeenIds += dto.messageId
          } else if (dto.deliveryStatus == "sent") {
            ackDeliveredIds += dto.messageId
          }
        }
      }
    }

    if (markAsRead) {
      val showReceipts = store.conversations[convId]?.capabilities?.showReadReceipts ?: true
      val targets = if (showReceipts) {
        (ackSeenIds + store.messages.values
          .filter { it.conversationId == convId && it.senderUserId != ownerUid && it.deliveryStatus != "seen" }
          .map { it.messageId })
          .distinct()
      } else {
        (ackDeliveredIds + store.messages.values
          .filter { it.conversationId == convId && it.senderUserId != ownerUid && it.deliveryStatus == "sent" }
          .map { it.messageId })
          .distinct()
      }
      val desiredStatus = if (showReceipts) "seen" else "delivered"
      targets.forEach { mid ->
        val r = apiClient.executeChatRaw("POST", "/v1/chat/messages/${DesktopChatCodec.urlEncode(mid)}/receipt", "{\"status\":\"$desiredStatus\"}")
        if (r != null && r.first in 200..299) {
          val updatedStatus = DesktopJson.str(DesktopJson.parseObject(r.second), "delivery_status").ifBlank { desiredStatus }
          store.messages[mid]?.let { cur -> store.messages[mid] = cur.copy(deliveryStatus = updatedStatus) }
        }
      }
      if (!showReceipts) {
        apiClient.executeChatRaw(
          "POST",
          "/v1/chat/conversations/batch-action",
          "{\"conversation_ids\":[\"${DesktopJson.escape(convId)}\"],\"action\":\"mark_read\"}",
        )
      }
    } else if (ackDeliveredIds.isNotEmpty()) {
      ackDeliveredIds.distinct().forEach { mid ->
        val r = apiClient.executeChatRaw("POST", "/v1/chat/messages/${DesktopChatCodec.urlEncode(mid)}/receipt", "{\"status\":\"delivered\"}")
        if (r != null && r.first in 200..299) {
          val updatedStatus = DesktopJson.str(DesktopJson.parseObject(r.second), "delivery_status").ifBlank { "delivered" }
          store.messages[mid]?.let { cur -> store.messages[mid] = cur.copy(deliveryStatus = updatedStatus) }
        }
      }
    }

    DesktopJson.objList(root, "security_flags").forEach { fm ->
      val f = DesktopChatCodec.parseSecurityFlag(fm, synced = true)
      if (f.flagId.isNotBlank()) store.flags[f.flagId] = f
    }
    refreshConvPreview(convId)
    if (markAsRead) {
      store.conversations[convId]?.let { c ->
        if (c.unreadCount != 0) store.conversations[convId] = c.copy(unreadCount = 0)
      }
    }
    store.save()
    publishConversationsFromStore()
    if (_activeConversationId.value == convId) publishMessagesFromStore(convId)
    if (newIncoming) {
      val conv = store.conversations[convId]
      val muted = conv?.isMuted == true
      audioEngine.playReceivedSound(soundsEnabledProvider() && !muted)
      val latestIncoming = store.messages.values
        .filter { it.conversationId == convId && it.senderUserId != ownerUid }
        .maxByOrNull { it.createdAtMs }
      if (!muted && _activeConversationId.value != convId && latestIncoming != null) {
        val isVo = latestIncoming.messageType == "voice_once"
        val preview = when (latestIncoming.messageType) {
          "voice_once" -> "Sent a view-once voice message"
          "voice" -> "Sent a voice message"
          "sticker" -> "Sent a sticker"
          "gif" -> "Sent a GIF"
          "clip" -> "Sent a clip"
          "star_gift" -> DesktopStarsCatalog.formatSummaryLine(latestIncoming.plainText)
          "call_log", "call_request" -> DesktopCallMetaParser.formatSummaryLine(latestIncoming.messageType, latestIncoming.plainText)
          else -> latestIncoming.plainText.lineSequence().firstOrNull().orEmpty().take(120)
            .ifBlank { "New encrypted message" }
        }
        _incomingBanner.value = DesktopIncomingMessageBanner(
          conversationId = convId,
          messageId = latestIncoming.messageId,
          peerUserId = latestIncoming.senderUserId,
          senderName = conv?.peerFullName?.ifBlank { conv.peerUsername }.orEmpty().ifBlank { "Telefam" },
          previewText = preview,
          isViewOnce = isVo,
          canReply = !isVo && conv?.capabilities?.isBlocked != true,
          createdAtMs = latestIncoming.createdAtMs,
        )
      }
    }
  }

  private fun parseConversationDto(m: Map<String, Any?>): DesktopChatConversation {
    val convId = DesktopJson.str(m, "conversation_id")
    val lastMessageId = DesktopJson.str(m, "last_message_id")
    val lastSender = DesktopJson.str(m, "last_sender_id")
    val lastCipher = DesktopJson.str(m, "last_ciphertext_b64")
    val lastSelfCipher = DesktopJson.str(m, "last_sender_ciphertext_b64")
    val lastType = DesktopJson.str(m, "last_message_type")
    val lastScheme = DesktopJson.str(m, "last_encryption_scheme")
    val decryptedOrCached = if (lastType == "star_gift" || lastScheme == "telefam_star_meta_v1") {
      DesktopStarsCatalog.decodeB64Json(lastCipher.ifBlank { lastSelfCipher })
        .takeIf { it.isNotBlank() }
    } else {
      e2ee?.decryptAndDecompress(lastSender == ownerUid, lastCipher, lastSelfCipher, "zlib")
    } ?: localStore?.messages?.get(lastMessageId)?.plainText?.takeIf { it.isNotBlank() && it != "[Encrypted message]" }
    val plain = when {
      lastType == "star_gift" || lastScheme == "telefam_star_meta_v1" ->
        DesktopStarsCatalog.formatSummaryLine(decryptedOrCached ?: "")
      lastType == "call_log" || lastType == "call_request" ->
        DesktopCallMetaParser.formatSummaryLine(lastType, decryptedOrCached ?: "")
      else -> decryptedOrCached ?: when (lastType) {
        "voice", "voice_once" -> "Voice message"
        "sticker" -> "Sticker"
        "gif" -> "GIF"
        "clip" -> "Clip"
        else -> ""
      }
    }
    return DesktopChatConversation(
      conversationId = convId,
      peerUserId = DesktopJson.str(m, "peer_user_id"),
      peerFullName = DesktopJson.str(m, "peer_full_name"),
      peerUsername = DesktopJson.str(m, "peer_username"),
      peerAvatarUrl = DesktopJson.str(m, "peer_avatar_url"),
      lastMessageId = lastMessageId,
      lastMessageType = DesktopJson.str(m, "last_message_type"),
      lastSenderId = lastSender,
      lastMessagePreviewPlain = plain,
      lastDeliveryStatus = DesktopJson.str(m, "last_delivery_status").ifBlank { "sent" },
      lastMessageAtMs = DesktopJson.long(m, "last_message_at_ms", 0L),
      unreadCount = DesktopJson.long(m, "unread_count", 0L).toInt(),
      isArchived = DesktopJson.bool(m, "is_archived"),
      isMuted = DesktopJson.bool(m, "is_muted"),
      isOnline = DesktopJson.bool(m, "is_online"),
      lastSeenMs = DesktopJson.long(m, "last_seen_ms", 0L),
      lastSeenLabel = DesktopJson.str(m, "last_seen_label").ifBlank { "last seen recently" },
      capabilities = DesktopChatCodec.parsePeerCapabilities(DesktopJson.obj(m, "permissions")),
      createdAtMs = DesktopJson.long(m, "created_at_ms", 0L),
      updatedAtMs = DesktopJson.long(m, "updated_at_ms", 0L),
    )
  }

  private fun parseMessageDto(m: Map<String, Any?>, fallbackPlain: String, localPath: String): DesktopChatMessage {
    val senderId = DesktopJson.str(m, "sender_user_id")
    val cipher = DesktopJson.str(m, "ciphertext_b64")
    val selfCipher = DesktopJson.str(m, "sender_ciphertext_b64")
    val codec = DesktopJson.str(m, "compression_codec").ifBlank { "zlib" }
    val msgType = DesktopJson.str(m, "message_type").ifBlank { "text" }
    val scheme = DesktopJson.str(m, "encryption_scheme").ifBlank { "telefam_ecdh_v1" }
    val decoded = if (scheme == "telefam_star_meta_v1" || msgType == "star_gift") {
      DesktopStarsCatalog.decodeB64Json(cipher.ifBlank { selfCipher }).ifBlank { fallbackPlain }
    } else {
      e2ee?.decryptAndDecompress(senderId == ownerUid, cipher, selfCipher, codec)
        ?: fallbackPlain.ifBlank { "[Encrypted message]" }
    }
    return DesktopChatMessage(
      messageId = DesktopJson.str(m, "message_id"),
      conversationId = DesktopJson.str(m, "conversation_id"),
      senderUserId = senderId,
      recipientUserId = DesktopJson.str(m, "recipient_user_id"),
      clientOpId = DesktopJson.str(m, "client_op_id"),
      messageType = msgType,
      compressionCodec = codec,
      encryptionScheme = scheme,
      ciphertextB64 = cipher,
      senderCiphertextB64 = selfCipher,
      signalMsgType = DesktopJson.long(m, "signal_msg_type", 3L).toInt(),
      plainText = decoded,
      mediaUrl = DesktopJson.str(m, "media_url"),
      mediaLocalPath = localPath,
      mediaId = DesktopJson.str(m, "media_id"),
      voiceDurationMs = DesktopJson.long(m, "voice_duration_ms", 0L),
      viewOnceTtlSec = DesktopJson.long(m, "view_once_ttl_sec", 0L).toInt(),
      viewOnceOpenedAtMs = DesktopJson.long(m, "view_once_opened_at_ms", 0L),
      viewOnceExpiresAtMs = DesktopJson.long(m, "view_once_expires_at_ms", 0L),
      forwardedFromUserId = DesktopJson.str(m, "forwarded_from_user_id"),
      deliveryStatus = DesktopJson.str(m, "delivery_status").ifBlank { "sent" },
      syncState = "synced",
      editedAtMs = DesktopJson.long(m, "edited_at_ms", 0L),
      createdAtMs = DesktopJson.long(m, "created_at_ms", 0L),
    )
  }

  private fun ensureLocalVoiceFile(msg: DesktopChatMessage): File? {
    if (msg.mediaLocalPath.isNotBlank()) {
      val f = File(msg.mediaLocalPath)
      if (f.exists()) return f
    }
    if (msg.mediaUrl.isBlank()) return null
    val bytes = apiClient.downloadMediaBytes(msg.mediaUrl) ?: return null
    val dir = File(System.getProperty("user.home"), ".telefam/voices").apply { if (!exists()) mkdirs() }
    val ext = if (bytes.size >= 4 && bytes[0] == 'R'.code.toByte()) "wav" else "m4a"
    val target = File(dir, "dl_${msg.messageId}.$ext")
    target.writeBytes(bytes)
    localStore?.let { store ->
      store.messages[msg.messageId] = msg.copy(mediaLocalPath = target.absolutePath)
      store.save()
    }
    return target
  }

  private fun handleWsEvent(ev: DesktopChatWsEvent) {
    val store = localStore ?: return
    when (ev.event) {
      "presence" -> {
        val conv = store.conversations.values.firstOrNull { it.peerUserId == ev.userId } ?: return
        store.conversations[conv.conversationId] = conv.copy(
          isOnline = ev.isOnline,
          lastSeenMs = if (ev.lastSeenMs > 0L) ev.lastSeenMs else conv.lastSeenMs,
          lastSeenLabel = ev.lastSeenLabel.ifBlank { conv.lastSeenLabel },
        )
        publishConversationsFromStore()
      }
      "typing" -> {
        if (ev.conversationId.isNotBlank() && ev.userId != ownerUid) {
          val mode = ev.typingMode
          _typingModeByConv.value = _typingModeByConv.value + (ev.conversationId to mode)
          if (mode == "typing") {
            val muted = store.conversations[ev.conversationId]?.isMuted == true
            audioEngine.playTypingTickSound(soundsEnabledProvider() && !muted)
          }
          scope.launch {
            delay(4_000L)
            if (_typingModeByConv.value[ev.conversationId] == mode) {
              _typingModeByConv.value = _typingModeByConv.value - ev.conversationId
            }
          }
        }
      }
      "new_message", "message_new", "message_edited", "conversation_updated", "call_ended" -> {
        if (ev.conversationId.isNotBlank()) {
          syncConversationMessages(ev.conversationId, markAsRead = (_activeConversationId.value == ev.conversationId))
        }
        refreshConversations(false)
      }
      "message_unsent", "view_once_burned", "view_once_consumed" -> {
        if (ev.messageId.isNotBlank()) {
          if (_incomingBanner.value?.messageId == ev.messageId) {
            _incomingBanner.value = null
          }
          store.messages.remove(ev.messageId)?.let { old ->
            if (old.mediaLocalPath.isNotBlank()) runCatching { File(old.mediaLocalPath).delete() }
          }
          if (ev.conversationId.isNotBlank()) refreshConvPreview(ev.conversationId)
          store.save()
          publishConversationsFromStore()
          if (_activeConversationId.value == ev.conversationId) publishMessagesFromStore(ev.conversationId)
        }
      }
      "message_receipt" -> {
        val msg = store.messages[ev.messageId]
        if (msg != null && ev.status.isNotBlank()) {
          store.messages[msg.messageId] = msg.copy(deliveryStatus = ev.status)
          store.conversations[msg.conversationId]?.let { c ->
            if (c.lastMessageId == msg.messageId) {
              store.conversations[c.conversationId] = c.copy(lastDeliveryStatus = ev.status)
            }
          }
          store.save()
          publishConversationsFromStore()
          if (_activeConversationId.value == msg.conversationId) publishMessagesFromStore(msg.conversationId)
        }
      }
    }
  }

  private fun startMaintenanceLoop() {
    maintenanceJob?.cancel()
    maintenanceJob = scope.launch {
      while (isActive) {
        delay(5_000L)
        flushPendingOps()
        val cid = _activeConversationId.value
        if (!cid.isNullOrBlank()) syncConversationMessages(cid, markAsRead = true)
      }
    }
  }

  private fun flushPendingOps() {
    val store = localStore ?: return
    val now = System.currentTimeMillis()
    store.flags.values.filter { !it.isSynced }.forEach { f ->
      val payload = "{\"message_id\":\"${DesktopJson.escape(f.messageId)}\",\"flag_type\":\"${DesktopJson.escape(f.flagType)}\",\"details\":\"${DesktopJson.escape(f.details)}\"}"
      val r = apiClient.executeChatRaw("POST", "/v1/chat/conversations/${DesktopChatCodec.urlEncode(f.conversationId)}/flags", payload)
      if (r != null && r.first in 200..299) {
        store.flags[f.flagId] = f.copy(isSynced = true)
      }
    }
    store.pendingOps.values.filter { it.nextRetryAtMs <= now }.toList().forEach { op ->
      val ok = when (op.opKind) {
        "send" -> store.messages[op.targetMessageId]?.let { tryDeliverMessage(it) } ?: true
        "edit" -> {
          val m = DesktopJson.parseObject(op.payloadJson)
          sendRemoteEdit(op.targetMessageId, DesktopJson.str(m, "peer_user_id"), DesktopJson.str(m, "plain_text"))
        }
        "unsend" -> {
          val r = apiClient.executeChatRaw("DELETE", "/v1/chat/messages/${DesktopChatCodec.urlEncode(op.targetMessageId)}?mode=unsend")
          r != null && (r.first in 200..299 || r.first == 404)
        }
        "delete_self" -> {
          val r = apiClient.executeChatRaw("DELETE", "/v1/chat/messages/${DesktopChatCodec.urlEncode(op.targetMessageId)}?mode=self")
          r != null && (r.first in 200..299 || r.first == 404)
        }
        "consume_view_once" -> {
          val r = apiClient.executeChatRaw("POST", "/v1/chat/messages/${DesktopChatCodec.urlEncode(op.targetMessageId)}/view-once/consume", "{}")
          r != null && (r.first in 200..299 || r.first == 404)
        }
        else -> true
      }
      if (ok) store.pendingOps.remove(op.opId) else {
        val next = op.retryCount + 1
        store.pendingOps[op.opId] = op.copy(retryCount = next, nextRetryAtMs = now + (3000L shl next.coerceAtMost(4)))
      }
    }
    store.save()
  }

  private fun refreshConvPreview(convId: String) {
    val store = localStore ?: return
    val conv = store.conversations[convId] ?: return
    val latest = store.messages.values.filter { it.conversationId == convId }.maxByOrNull { it.createdAtMs }
    store.conversations[convId] = if (latest == null) {
      conv.copy(lastMessageId = "", lastMessagePreviewPlain = "")
    } else {
      conv.copy(
        lastMessageId = latest.messageId,
        lastMessageType = latest.messageType,
        lastSenderId = latest.senderUserId,
        lastMessagePreviewPlain = previewTextFor(latest),
        lastDeliveryStatus = latest.deliveryStatus,
        lastMessageAtMs = latest.createdAtMs,
      )
    }
  }

  private fun publishConversationsFromStore() {
    val store = localStore ?: return
    val all = store.conversations.values.filter { !it.isDeletedLocally }.sortedByDescending { maxOf(it.lastMessageAtMs, it.updatedAtMs) }
    _conversations.value = all.filter { !it.isArchived }
    _archivedConversations.value = all.filter { it.isArchived }
  }

  private fun publishMessagesFromStore(convId: String) {
    val store = localStore ?: return
    _activeMessages.value = store.messages.values.filter { it.conversationId == convId }.sortedBy { it.createdAtMs }
    _activeSecurityFlags.value = store.flags.values.filter { it.conversationId == convId }.sortedByDescending { it.createdAtMs }
  }

  private fun previewTextFor(msg: DesktopChatMessage): String = when (msg.messageType) {
    "sticker" -> "Sticker"
    "gif" -> "GIF"
    "clip" -> "Clip"
    "voice" -> "Voice message"
    "voice_once" -> "View-once voice"
    "star_gift" -> DesktopStarsCatalog.formatSummaryLine(msg.plainText)
    "call_log", "call_request" -> DesktopCallMetaParser.formatSummaryLine(msg.messageType, msg.plainText)
    else -> msg.plainText
  }
}
