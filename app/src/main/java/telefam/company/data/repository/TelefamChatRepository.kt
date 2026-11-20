package telefam.company.data.repository

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import telefam.company.calls.TelefamCallMetaParser
import telefam.company.chat.ChatWebSocketClient
import telefam.company.chat.ChatWsIncomingEvent
import telefam.company.chat.PeerBundleInput
import telefam.company.chat.SignalE2eeManager
import telefam.company.core.AdaptiveStreamSelector
import telefam.company.data.local.ChatConversationEntity
import telefam.company.data.local.ChatMessageEntity
import telefam.company.data.local.ChatSecurityFlagEntity
import telefam.company.data.local.DeviceIdentityStore
import telefam.company.data.local.PendingChatOpEntity
import telefam.company.data.local.SignalKeyStateEntity
import telefam.company.data.local.TelefamDao
import telefam.company.data.network.BatchConversationActionRequestDto
import telefam.company.data.network.CallRecordDto
import telefam.company.data.network.CallSignalRequestDto
import telefam.company.data.network.CallStateResponseDto
import telefam.company.data.network.ChatConversationDto
import telefam.company.data.network.ChatMessageDto
import telefam.company.data.network.ChatReceiptRequestDto
import telefam.company.data.network.ChatSearchPersonDto
import telefam.company.data.network.ChatSecurityFlagDto
import telefam.company.data.network.E2eeKeyBundleUploadDto
import telefam.company.data.network.EditChatMessageRequestDto
import telefam.company.data.network.GiflyMediaItemDto
import telefam.company.data.network.GiflySearchResponseDto
import telefam.company.data.network.IceConfigResponseDto
import telefam.company.data.network.NetworkQualityMonitor
import telefam.company.data.network.OneTimePreKeyDto
import telefam.company.data.network.RecordSecurityFlagRequestDto
import telefam.company.data.network.SendChatMessageRequestDto
import telefam.company.data.network.StartCallRequestDto
import telefam.company.data.network.StartCallResponseDto
import telefam.company.data.network.TelefamApiService
import java.io.File
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit

data class ChatSearchBundleResult(
  val conversations: List<ChatConversationEntity>,
  val matchingMessages: List<ChatMessageEntity>,
  val people: List<ChatSearchPersonDto>,
)

data class PushDecryptedMessage(
  val conversationId: String,
  val messageId: String,
  val peerUserId: String,
  val previewText: String,
  val isViewOnce: Boolean,
  val isMuted: Boolean,
  val canReply: Boolean,
  val mentionsOwner: Boolean,
)

class TelefamChatRepository(
  private val appContext: Context,
  private val dao: TelefamDao,
  private val identityStore: DeviceIdentityStore,
  private val networkMonitor: NetworkQualityMonitor,
  private val apiService: TelefamApiService?,
  private val baseHttpUrl: String,
  private val deviceFingerprintProvider: suspend () -> String,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val signalMutex = Mutex()
  private var loadedOwnerId: String = ""
  private var signalManager: SignalE2eeManager? = null

  private val wsHttpClient = OkHttpClient.Builder()
    .connectTimeout(12, TimeUnit.SECONDS)
    .readTimeout(0, TimeUnit.MILLISECONDS)
    .pingInterval(25, TimeUnit.SECONDS)
    .retryOnConnectionFailure(true)
    .build()

  private val wsClient = ChatWebSocketClient(wsHttpClient, baseHttpUrl)
  val wsEvents: SharedFlow<ChatWsIncomingEvent> = wsClient.events

  init {
    scope.launch {
      wsClient.events.collect { ev ->
        handleIncomingWsEvent(ev)
      }
    }
  }

  fun observeActiveConversations(): Flow<List<ChatConversationEntity>> = dao.observeActiveConversations()

  fun observeConversation(conversationId: String): Flow<ChatConversationEntity?> = dao.observeConversation(conversationId)

  fun observeMessagesForConversation(conversationId: String): Flow<List<ChatMessageEntity>> =
    dao.observeMessagesForConversation(conversationId)

  fun observeSecurityFlags(conversationId: String): Flow<List<ChatSecurityFlagEntity>> =
    dao.observeSecurityFlags(conversationId)

  suspend fun connectWebSocket() {
    val session = identityStore.sessionFlow.first()
    val token = session.accessToken.orEmpty()
    if (token.isBlank()) return
    val fp = deviceFingerprintProvider()
    wsClient.connect(token, fp)
  }

  fun disconnectWebSocket() {
    wsClient.disconnect()
  }

  suspend fun clearLocalChatSession() = withContext(Dispatchers.IO) {
    wsClient.disconnect()
    signalMutex.withLock {
      signalManager = null
      loadedOwnerId = ""
    }
    val voiceDir = File(appContext.cacheDir, "telefam_voice_notes")
    if (voiceDir.exists()) {
      runCatching { voiceDir.deleteRecursively() }
    }
  }

  suspend fun resolveLocalVoiceFilePath(
    messageId: String,
    localPath: String,
    remoteUrl: String,
  ): String? = withContext(Dispatchers.IO) {
    val cleanLocal = localPath.trim()
    if (cleanLocal.isNotBlank()) {
      val f = File(cleanLocal)
      if (f.exists() && f.length() > 32L) return@withContext f.absolutePath
    }
    val cleanRemote = remoteUrl.trim()
    if (cleanRemote.isBlank()) return@withContext null
    if (!cleanRemote.startsWith("http://") && !cleanRemote.startsWith("https://")) {
      val f = File(cleanRemote)
      return@withContext if (f.exists() && f.length() > 32L) f.absolutePath else null
    }
    val resolvedUrl = AdaptiveStreamSelector.resolveMediaUrl(baseHttpUrl, cleanRemote)
    val session = identityStore.sessionFlow.first()
    val token = session.accessToken.orEmpty()
    val fp = deviceFingerprintProvider()
    val reqBuilder = okhttp3.Request.Builder().url(resolvedUrl)
    if (token.isNotBlank()) reqBuilder.header("Authorization", "Bearer $token")
    if (fp.isNotBlank()) reqBuilder.header("X-Telefam-Device-FP", fp)

    val bytes = runCatching {
      wsHttpClient.newCall(reqBuilder.build()).execute().use { resp ->
        if (!resp.isSuccessful) null else resp.body?.bytes()
      }
    }.getOrNull() ?: return@withContext null
    if (bytes.size < 32) return@withContext null

    val isWav = bytes.size >= 4 &&
      bytes[0] == 'R'.code.toByte() &&
      bytes[1] == 'I'.code.toByte() &&
      bytes[2] == 'F'.code.toByte() &&
      bytes[3] == 'F'.code.toByte()
    val ext = if (isWav) "wav" else "m4a"
    val voiceDir = File(appContext.cacheDir, "telefam_voice_notes").apply { mkdirs() }
    val safeId = messageId.ifBlank { UUID.randomUUID().toString() }.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    val outFile = File(voiceDir, "vn_$safeId.$ext")
    runCatching {
      outFile.writeBytes(bytes)
    }.getOrElse { return@withContext null }
    if (messageId.isNotBlank()) {
      runCatching { dao.updateChatMessageMediaLocalPath(messageId, outFile.absolutePath) }
    }
    outFile.absolutePath
  }

  fun sendTypingIndicator(conversationId: String, mode: String) {
    wsClient.sendTyping(conversationId, mode)
  }

  fun sendCallSignalViaWs(callId: String, payloadJson: JSONObject): Boolean =
    wsClient.sendCallSignal(callId, payloadJson)

  fun resolveCallAvatarUrl(rawUrl: String): String {
    val clean = rawUrl.trim()
    if (clean.isBlank()) return ""
    return AdaptiveStreamSelector.resolveMediaUrl(baseHttpUrl, clean)
  }

  suspend fun currentOwnerUserId(): String =
    identityStore.sessionFlow.first().userId.orEmpty().trim()

  suspend fun fetchIceConfig(): IceConfigResponseDto? = withContext(Dispatchers.IO) {
    val api = apiService ?: return@withContext null
    val token = identityStore.sessionFlow.first().accessToken.orEmpty()
    if (token.isBlank()) return@withContext null
    val fp = deviceFingerprintProvider()
    val resp = runCatching { api.getCallIceConfig("Bearer $token", fp) }.getOrNull()
    if (resp?.isSuccessful == true) resp.body() else null
  }

  suspend fun startCall(
    peerUserId: String,
    conversationId: String = "",
    callType: String,
    sdpOffer: String = "",
  ): StartCallResponseDto? = withContext(Dispatchers.IO) {
    val api = apiService ?: return@withContext null
    val token = identityStore.sessionFlow.first().accessToken.orEmpty()
    if (token.isBlank()) return@withContext null
    val fp = deviceFingerprintProvider()
    val resp = runCatching {
      api.startCall(
        bearer = "Bearer $token",
        deviceFingerprint = fp,
        body = StartCallRequestDto(
          peerUserId = peerUserId,
          conversationId = conversationId,
          callType = callType,
          sdpOffer = sdpOffer,
        ),
      )
    }.getOrNull()
    resp?.body()
  }

  suspend fun getCallState(callId: String, sinceSeq: Long = 0L): CallStateResponseDto? = withContext(Dispatchers.IO) {
    val api = apiService ?: return@withContext null
    val token = identityStore.sessionFlow.first().accessToken.orEmpty()
    if (token.isBlank() || callId.isBlank()) return@withContext null
    val fp = deviceFingerprintProvider()
    val resp = runCatching { api.getCallState("Bearer $token", fp, callId, sinceSeq) }.getOrNull()
    if (resp?.isSuccessful == true) resp.body() else null
  }

  suspend fun sendCallSignal(callId: String, request: CallSignalRequestDto): CallRecordDto? = withContext(Dispatchers.IO) {
    val api = apiService ?: return@withContext null
    val token = identityStore.sessionFlow.first().accessToken.orEmpty()
    if (token.isBlank() || callId.isBlank()) return@withContext null
    val fp = deviceFingerprintProvider()
    val resp = runCatching { api.sendCallSignal("Bearer $token", fp, callId, request) }.getOrNull()
    if (resp?.isSuccessful == true) resp.body()?.call else null
  }

  suspend fun ensureSignalEngineReady(uploadIfNeeded: Boolean = true): SignalE2eeManager? = withContext(Dispatchers.IO) {
    val uid = identityStore.sessionFlow.first().userId.orEmpty().trim()
    if (uid.isBlank()) return@withContext null
    var bundleUploaded = false
    val engine = signalMutex.withLock {
      val existing = signalManager
      val saved = dao.getSignalKeyState(uid)
      bundleUploaded = saved?.bundleUploaded ?: false
      if (existing != null && loadedOwnerId == uid) {
        existing
      } else {
        val created = SignalE2eeManager(uid, saved?.serializedStoreJson.orEmpty())
        signalManager = created
        loadedOwnerId = uid
        dao.upsertSignalKeyState(
          SignalKeyStateEntity(
            ownerUserId = uid,
            serializedStoreJson = created.exportStateJson(uid),
            bundleUploaded = bundleUploaded,
            updatedAtMs = System.currentTimeMillis(),
          ),
        )
        created
      }
    }
    if (uploadIfNeeded && !bundleUploaded) {
      uploadSignalBundle(uid, engine)
    }
    engine
  }

  private suspend fun persistSignalState(uid: String, engine: SignalE2eeManager, uploaded: Boolean? = null) {
    val existing = dao.getSignalKeyState(uid)
    dao.upsertSignalKeyState(
      SignalKeyStateEntity(
        ownerUserId = uid,
        serializedStoreJson = engine.exportStateJson(uid),
        bundleUploaded = uploaded ?: existing?.bundleUploaded ?: false,
        updatedAtMs = System.currentTimeMillis(),
      ),
    )
  }

  private suspend fun uploadSignalBundle(uid: String, engine: SignalE2eeManager): Boolean {
    val api = apiService ?: return false
    val token = identityStore.sessionFlow.first().accessToken.orEmpty()
    if (token.isBlank()) return false
    val fp = deviceFingerprintProvider()
    val snap = engine.exportPublicBundle()
    val dto = E2eeKeyBundleUploadDto(
      registrationId = snap.registrationId,
      identityPubB64 = snap.identityPubB64,
      signedPreKeyId = snap.signedPreKeyId,
      signedPreKeyPubB64 = snap.signedPreKeyPubB64,
      signedPreKeySigB64 = snap.signedPreKeySigB64,
      kyberPreKeyId = snap.kyberPreKeyId,
      kyberPreKeyPubB64 = snap.kyberPreKeyPubB64,
      kyberPreKeySigB64 = snap.kyberPreKeySigB64,
      oneTimePreKeys = snap.oneTimePreKeys.map { (id, pub) -> OneTimePreKeyDto(id, pub) },
    )
    val resp = runCatching { api.uploadE2eeKeys("Bearer $token", fp, dto) }.getOrNull()
    if (resp?.isSuccessful == true && resp.body()?.success == true) {
      persistSignalState(uid, engine, uploaded = true)
      return true
    }
    return false
  }

  private suspend fun ensurePeerSignalSession(engine: SignalE2eeManager, ownerUid: String, peerUserId: String): Boolean {
    val alreadyHas = signalMutex.withLock { engine.hasSession(peerUserId) }
    if (alreadyHas) return true
    val api = apiService ?: return false
    val token = identityStore.sessionFlow.first().accessToken.orEmpty()
    if (token.isBlank()) return false
    val fp = deviceFingerprintProvider()
    val resp = runCatching { api.fetchPeerE2eeKeys("Bearer $token", fp, peerUserId) }.getOrNull() ?: return false
    val body = resp.body()
    if (!resp.isSuccessful || body == null || !body.success || body.identityPubB64.isBlank()) return false
    val changedPeers = signalMutex.withLock {
      runCatching {
        engine.initSessionWithPeer(
          PeerBundleInput(
            peerUserId = peerUserId,
            deviceId = body.deviceId.coerceAtLeast(1),
            registrationId = body.registrationId,
            identityPubB64 = body.identityPubB64,
            signedPreKeyId = body.signedPreKeyId,
            signedPreKeyPubB64 = body.signedPreKeyPubB64,
            signedPreKeySigB64 = body.signedPreKeySigB64,
            kyberPreKeyId = body.kyberPreKeyId,
            kyberPreKeyPubB64 = body.kyberPreKeyPubB64,
            kyberPreKeySigB64 = body.kyberPreKeySigB64,
            preKeyId = body.preKeyId,
            preKeyPubB64 = body.preKeyPubB64,
          ),
        )
        persistSignalState(ownerUid, engine)
      }
      engine.consumeIdentityChangedPeers()
    }
    if (changedPeers.contains(peerUserId)) {
      dao.getConversationByPeer(peerUserId)?.let { conv ->
        recordSecurityFlag(
          conversationId = conv.conversationId,
          messageId = "",
          flagType = "peer_identity_changed",
          details = "Encryption security key refreshed for ${conv.peerFullName.ifBlank { peerUserId }}",
        )
      }
    }
    return signalMutex.withLock { engine.hasSession(peerUserId) }
  }

  private suspend fun checkAndReplenishPreKeys(ownerUid: String, engine: SignalE2eeManager?) {
    if (engine == null || ownerUid.isBlank()) return
    val replenished = signalMutex.withLock {
      if (engine.replenishPreKeysIfLow()) {
        persistSignalState(ownerUid, engine, uploaded = false)
        true
      } else {
        false
      }
    }
    if (replenished) {
      uploadSignalBundle(ownerUid, engine)
    }
  }

  suspend fun refreshConversations(): List<ChatConversationEntity> = withContext(Dispatchers.IO) {
    val local = dao.getAllActiveConversations()
    val api = apiService ?: return@withContext local
    val session = identityStore.sessionFlow.first()
    val token = session.accessToken.orEmpty()
    val ownerUid = session.userId.orEmpty()
    if (token.isBlank() || ownerUid.isBlank()) return@withContext local

    val engine = ensureSignalEngineReady(true)
    val fp = deviceFingerprintProvider()
    val resp = runCatching { api.listChatConversations("Bearer $token", fp, false) }.getOrNull()
    val body = resp?.body()
    if (resp?.isSuccessful == true && body?.success == true) {
      val existingById = body.conversations.associate { it.conversationId to dao.getConversation(it.conversationId) }
      val cachedMsgById = body.conversations.mapNotNull { dto ->
        dto.lastMessageId.takeIf { it.isNotBlank() }?.let { id -> dao.getChatMessage(id)?.let { id to it } }
      }.toMap()
      val mapped = signalMutex.withLock {
        val list = body.conversations.map { dto ->
          val existing = existingById[dto.conversationId]
          val cachedMsg = cachedMsgById[dto.lastMessageId]
          val decodedPreview = decryptConversationPreviewLocked(engine, ownerUid, dto, existing, cachedMsg)
          mapConversationDto(dto, existing, decodedPreview)
        }
        if (engine != null) persistSignalState(ownerUid, engine)
        list
      }
      dao.upsertConversations(mapped)
      checkAndReplenishPreKeys(ownerUid, engine)
      return@withContext dao.getAllActiveConversations()
    }
    local
  }

  suspend fun openDirectChat(
    peerUserId: String,
    peerFullName: String = "",
    peerUsername: String = "",
    peerAvatarUrl: String = "",
  ): ChatConversationEntity? = withContext(Dispatchers.IO) {
    val cleanPeer = peerUserId.trim()
    if (cleanPeer.isBlank()) return@withContext null
    val session = identityStore.sessionFlow.first()
    val ownerUid = session.userId.orEmpty()
    val token = session.accessToken.orEmpty()
    if (ownerUid.isBlank()) return@withContext null

    val localByPeer = dao.getConversationByPeer(cleanPeer)
    val optimisticId = localByPeer?.conversationId ?: deterministicDirectConvId(ownerUid, cleanPeer)
    val nowMs = System.currentTimeMillis()
    val optimistic = localByPeer ?: ChatConversationEntity(
      conversationId = optimisticId,
      peerUserId = cleanPeer,
      peerFullName = peerFullName.ifBlank { peerUsername.ifBlank { "Telefam User" } },
      peerUsername = peerUsername,
      peerAvatarUrl = peerAvatarUrl,
      lastMessageId = "",
      lastMessageType = "",
      lastSenderId = "",
      lastMessagePreviewPlain = "",
      lastDeliveryStatus = "sent",
      lastMessageAtMs = 0L,
      unreadCount = 0,
      isArchived = false,
      isMuted = false,
      isDeletedLocally = false,
      isOnline = false,
      lastSeenMs = 0L,
      lastSeenLabel = "last seen recently",
      canCall = true,
      canScreenshot = false,
      canShareChats = false,
      canCopyMessages = false,
      canDownloadMedia = true,
      showReadReceipts = true,
      showTypingIndicator = false,
      showOnlineIndicator = false,
      isBlocked = false,
      draftText = "",
      updatedAtMs = nowMs,
    )
    dao.upsertConversation(optimistic.copy(unreadCount = 0, isDeletedLocally = false))

    val api = apiService
    if (api != null && token.isNotBlank()) {
      val engine = ensureSignalEngineReady(true)
      val fp = deviceFingerprintProvider()
      val resp = runCatching { api.openDirectConversation("Bearer $token", fp, cleanPeer) }.getOrNull()
      val dto = resp?.body()?.conversation
      if (resp?.isSuccessful == true && resp.body()?.success == true && dto != null && dto.conversationId.isNotBlank()) {
        if (optimisticId != dto.conversationId) {
          dao.setConversationsDeletedLocally(listOf(optimisticId), true)
        }
        val entity = mapConversationDto(dto, optimistic, optimistic.lastMessagePreviewPlain).copy(unreadCount = 0)
        dao.upsertConversation(entity)
        if (engine != null) {
          ensurePeerSignalSession(engine, ownerUid, cleanPeer)
        }
        return@withContext entity
      }
    }
    dao.getConversation(optimisticId)
  }

  suspend fun loadConversationMessages(
    conversationId: String,
    markAsRead: Boolean = false,
  ) = withContext(Dispatchers.IO) {
    val cleanId = conversationId.trim()
    if (cleanId.isBlank()) return@withContext
    if (markAsRead) {
      dao.clearConversationUnread(cleanId)
    }

    val api = apiService ?: return@withContext
    val session = identityStore.sessionFlow.first()
    val token = session.accessToken.orEmpty()
    val ownerUid = session.userId.orEmpty()
    if (token.isBlank() || ownerUid.isBlank()) return@withContext

    val engine = ensureSignalEngineReady(true)
    val fp = deviceFingerprintProvider()
    val resp = runCatching { api.listChatMessages("Bearer $token", fp, cleanId) }.getOrNull() ?: return@withContext
    val body = resp.body()
    if (!resp.isSuccessful || body == null || !body.success) return@withContext

    val convDto = body.conversation
    if (convDto.conversationId.isNotBlank()) {
      val existingConv = dao.getConversation(cleanId)
      val baseConv = mapConversationDto(convDto, existingConv, existingConv?.lastMessagePreviewPlain.orEmpty())
      val preservedUnread = if (markAsRead) 0 else convDto.unreadCount
      dao.upsertConversation(baseConv.copy(unreadCount = preservedUnread))
      if (engine != null) {
        ensurePeerSignalSession(engine, ownerUid, convDto.peerUserId)
      }
    }

    val existingMessages = dao.getMessagesForConversation(cleanId)
    val localById = existingMessages.associateBy { it.messageId }
    val localByClientOpId = existingMessages.filter { it.clientOpId.isNotBlank() }.associateBy { it.clientOpId }
    val remoteIds = body.messages.map { it.messageId }.toSet()
    val remoteClientOpIds = body.messages.map { it.clientOpId }.filter { it.isNotBlank() }.toSet()

    // Remove synced messages removed remotely AND any local optimistic row now superseded by a server message
    localById.values.filter {
      (it.syncState == "synced" && !remoteIds.contains(it.messageId)) ||
        (it.syncState != "synced" && !remoteIds.contains(it.messageId) && it.clientOpId.isNotBlank() && remoteClientOpIds.contains(it.clientOpId))
    }.forEach { removed ->
      if (removed.syncState == "synced") {
        deleteLocalVoiceFileIfAny(removed.mediaLocalPath)
      }
      dao.deleteChatMessagePermanently(removed.messageId)
    }

    val updatedMessages = signalMutex.withLock {
      val list = body.messages.map { dto ->
        val cached = localById[dto.messageId] ?: localByClientOpId[dto.clientOpId]
        val plain = decryptOrReusePlaintext(engine, ownerUid, dto, cached)
        mapMessageDto(dto, plain, cached?.mediaLocalPath.orEmpty())
      }
      if (engine != null) persistSignalState(ownerUid, engine)
      list
    }
    if (updatedMessages.isNotEmpty()) {
      dao.upsertChatMessages(updatedMessages)
      val conv = dao.getConversation(cleanId)
      val latest = updatedMessages.last()
      val effectiveUnread = if (markAsRead) 0 else (conv?.unreadCount ?: convDto.unreadCount)
      if (conv != null) {
        dao.upsertConversation(
          conv.copy(
            lastMessageId = latest.messageId,
            lastMessageType = latest.messageType,
            lastSenderId = latest.senderUserId,
            lastMessagePreviewPlain = previewTextForMessage(latest),
            lastDeliveryStatus = latest.deliveryStatus,
            lastMessageAtMs = latest.createdAtMs,
            unreadCount = effectiveUnread,
            updatedAtMs = latest.createdAtMs.coerceAtLeast(conv.updatedAtMs),
          ),
        )
      }

      val showReceipts = conv?.showReadReceipts ?: true
      val incomingMsgs = updatedMessages.filter { it.senderUserId != ownerUid }
      if (markAsRead) {
        val latestUnseen = incomingMsgs.lastOrNull { it.deliveryStatus != "seen" }
        val latestUndelivered = incomingMsgs.lastOrNull { it.deliveryStatus == "sent" }
        if (showReceipts && latestUnseen != null) {
          val recResp = runCatching {
            api.sendChatReceipt("Bearer $token", fp, latestUnseen.messageId, ChatReceiptRequestDto("seen"))
          }.getOrNull()
          val confirmedStatus = recResp?.body()?.message?.deliveryStatus?.takeIf { it.isNotBlank() } ?: "seen"
          if (recResp?.isSuccessful == true) {
            incomingMsgs.filter { it.deliveryStatus != "seen" }.forEach { msg ->
              dao.updateChatMessageDeliveryStatus(msg.messageId, confirmedStatus)
            }
          }
        } else if (!showReceipts && (latestUndelivered != null || (conv?.unreadCount ?: 0) > 0 || convDto.unreadCount > 0)) {
          if (latestUndelivered != null) {
            val recResp = runCatching {
              api.sendChatReceipt("Bearer $token", fp, latestUndelivered.messageId, ChatReceiptRequestDto("delivered"))
            }.getOrNull()
            if (recResp?.isSuccessful == true) {
              incomingMsgs.filter { it.deliveryStatus == "sent" }.forEach { msg ->
                dao.updateChatMessageDeliveryStatus(msg.messageId, "delivered")
              }
            }
          }
          runCatching {
            api.batchConversationAction("Bearer $token", fp, BatchConversationActionRequestDto(listOf(cleanId), "mark_read"))
          }
        }
      } else {
        val latestUndelivered = incomingMsgs.lastOrNull { it.deliveryStatus == "sent" }
        if (latestUndelivered != null) {
          val recResp = runCatching {
            api.sendChatReceipt("Bearer $token", fp, latestUndelivered.messageId, ChatReceiptRequestDto("delivered"))
          }.getOrNull()
          if (recResp?.isSuccessful == true) {
            incomingMsgs.filter { it.deliveryStatus == "sent" }.forEach { msg ->
              dao.updateChatMessageDeliveryStatus(msg.messageId, "delivered")
            }
          }
        }
      }
    }

    val flags = body.securityFlags.map { mapFlagDto(it, isSynced = true) }
    if (flags.isNotEmpty()) dao.upsertSecurityFlags(flags)
    checkAndReplenishPreKeys(ownerUid, engine)
  }

  suspend fun sendTextMessage(
    conversationId: String,
    peerUserId: String,
    text: String,
    forwardedFromUserId: String = "",
  ): ChatMessageEntity? = withContext(Dispatchers.IO) {
    val cleanText = text.trim()
    if (cleanText.isBlank()) return@withContext null
    sendEncryptedEnvelopeInternal(
      conversationId = conversationId,
      peerUserId = peerUserId,
      messageType = "text",
      plainText = cleanText,
      mediaUrl = "",
      mediaLocalPath = "",
      mediaId = "",
      voiceDurationMs = 0L,
      viewOnceTtlSec = 0,
      forwardedFromUserId = forwardedFromUserId,
    )
  }

  suspend fun sendGiflyMediaMessage(
    conversationId: String,
    peerUserId: String,
    item: GiflyMediaItemDto,
    messageType: String,
  ): ChatMessageEntity? = withContext(Dispatchers.IO) {
    val bestUrl = when {
      messageType == "clip" && !item.mp4.isNullOrBlank() -> item.mp4
      item.urlHd.isNotBlank() -> item.urlHd
      item.url.isNotBlank() -> item.url
      item.previewWebp.isNotBlank() -> item.previewWebp
      else -> item.preview
    }.trim()
    if (bestUrl.isBlank()) return@withContext null
    val metaPayload = JSONObject()
      .put("title", item.title.ifBlank { item.slug })
      .put("slug", item.slug)
      .put("url", bestUrl)
      .put("preview", item.previewWebp.ifBlank { item.preview })
      .put("mp4", item.mp4.orEmpty())
      .toString()
    sendEncryptedEnvelopeInternal(
      conversationId = conversationId,
      peerUserId = peerUserId,
      messageType = messageType, // sticker | gif | clip
      plainText = metaPayload,
      mediaUrl = bestUrl,
      mediaLocalPath = "",
      mediaId = item.id.toString(),
      voiceDurationMs = 0L,
      viewOnceTtlSec = 0,
      forwardedFromUserId = "",
    )
  }

  suspend fun sendVoiceNoteMessage(
    conversationId: String,
    peerUserId: String,
    localFilePath: String,
    durationMs: Long,
    viewOnce: Boolean,
    viewOnceTtlSec: Int,
  ): ChatMessageEntity? = withContext(Dispatchers.IO) {
    val file = File(localFilePath)
    if (!file.exists() || file.length() == 0L) return@withContext null
    val msgType = if (viewOnce) "voice_once" else "voice"
    val ttl = if (viewOnce) viewOnceTtlSec.coerceIn(0, 120) else 0
    val label = if (viewOnce) "View-once voice message" else "Voice message"
    sendEncryptedEnvelopeInternal(
      conversationId = conversationId,
      peerUserId = peerUserId,
      messageType = msgType,
      plainText = label,
      mediaUrl = "",
      mediaLocalPath = localFilePath,
      mediaId = "",
      voiceDurationMs = durationMs,
      viewOnceTtlSec = ttl,
      forwardedFromUserId = "",
    )
  }

  private suspend fun sendEncryptedEnvelopeInternal(
    conversationId: String,
    peerUserId: String,
    messageType: String,
    plainText: String,
    mediaUrl: String,
    mediaLocalPath: String,
    mediaId: String,
    voiceDurationMs: Long,
    viewOnceTtlSec: Int,
    forwardedFromUserId: String,
  ): ChatMessageEntity? {
    val existingConv = dao.getConversation(conversationId)
    if (existingConv?.isBlocked == true) return null
    val session = identityStore.sessionFlow.first()
    val ownerUid = session.userId.orEmpty()
    if (ownerUid.isBlank()) return null

    val clientOpId = UUID.randomUUID().toString()
    val nowMs = System.currentTimeMillis()
    val localMsg = ChatMessageEntity(
      messageId = clientOpId,
      conversationId = conversationId,
      senderUserId = ownerUid,
      recipientUserId = peerUserId,
      clientOpId = clientOpId,
      messageType = messageType,
      plainText = plainText,
      mediaUrl = mediaUrl,
      mediaLocalPath = mediaLocalPath,
      mediaId = mediaId,
      voiceDurationMs = voiceDurationMs,
      viewOnceTtlSec = viewOnceTtlSec,
      viewOnceOpenedAtMs = 0L,
      viewOnceExpiresAtMs = 0L,
      forwardedFromUserId = forwardedFromUserId,
      deliveryStatus = "sending",
      syncState = "pending",
      editedAtMs = 0L,
      isPendingUnsend = false,
      createdAtMs = nowMs,
    )

    // 1. Instant local persistence FIRST without blocking on network
    dao.upsertChatMessage(localMsg)
    dao.getConversation(conversationId)?.let { conv ->
      dao.upsertConversation(
        conv.copy(
          lastMessageId = localMsg.messageId,
          lastMessageType = messageType,
          lastSenderId = ownerUid,
          lastMessagePreviewPlain = previewTextForMessage(localMsg),
          lastDeliveryStatus = "sending",
          lastMessageAtMs = nowMs,
          draftText = "",
          updatedAtMs = nowMs,
        ),
      )
    }

    // 2. Attempt remote encrypt + upload + send immediately when online; on offline/failure queue cleanly to outbox
    val isOnline = networkMonitor.networkTier.value != telefam.company.core.NetworkTier.OFFLINE
    val sentRemote = if (isOnline) tryDeliverSingleMessage(localMsg) else false
    if (!sentRemote) {
      val opPayload = JSONObject()
        .put("peer_user_id", peerUserId)
        .put("message_type", messageType)
        .put("plain_text", plainText)
        .put("media_url", mediaUrl)
        .put("media_local_path", mediaLocalPath)
        .put("media_id", mediaId)
        .put("voice_duration_ms", voiceDurationMs)
        .put("view_once_ttl_sec", viewOnceTtlSec)
        .put("forwarded_from_user_id", forwardedFromUserId)
        .toString()
      dao.enqueueChatOp(
        PendingChatOpEntity(
          opId = clientOpId,
          opKind = "send",
          conversationId = conversationId,
          targetMessageId = clientOpId,
          payloadJson = opPayload,
          createdAtMs = nowMs,
          retryCount = 0,
          nextRetryAtMs = if (isOnline) nowMs + 2_500L else nowMs,
        ),
      )
    }
    return dao.getChatMessage(localMsg.messageId) ?: localMsg
  }

  private suspend fun tryDeliverSingleMessage(localMsg: ChatMessageEntity): Boolean {
    val api = apiService ?: return false
    val session = identityStore.sessionFlow.first()
    val token = session.accessToken.orEmpty()
    val ownerUid = session.userId.orEmpty()
    if (token.isBlank() || ownerUid.isBlank()) return false
    val fp = deviceFingerprintProvider()
    val engine = ensureSignalEngineReady(true) ?: return false

    var resolvedMediaId = localMsg.mediaId
    var resolvedMediaUrl = localMsg.mediaUrl
    if ((localMsg.messageType == "voice" || localMsg.messageType == "voice_once") &&
      resolvedMediaUrl.isBlank() &&
      localMsg.mediaLocalPath.isNotBlank()
    ) {
      val file = File(localMsg.mediaLocalPath)
      if (!file.exists()) return false
      val convPart = localMsg.conversationId.toRequestBody("text/plain".toMediaTypeOrNull())
      val durPart = localMsg.voiceDurationMs.toString().toRequestBody("text/plain".toMediaTypeOrNull())
      val voPart = (localMsg.messageType == "voice_once").toString().toRequestBody("text/plain".toMediaTypeOrNull())
      val filePart = MultipartBody.Part.createFormData("file", file.name, file.asRequestBody("audio/mp4".toMediaTypeOrNull()))
      val upResp = runCatching { api.uploadVoiceMedia("Bearer $token", fp, convPart, durPart, voPart, filePart) }.getOrNull()
      val upBody = upResp?.body()
      if (upResp?.isSuccessful != true || upBody == null || !upBody.success || upBody.mediaUrl.isBlank()) {
        return false
      }
      resolvedMediaId = upBody.mediaId
      resolvedMediaUrl = AdaptiveStreamSelector.resolveMediaUrl(baseHttpUrl, upBody.mediaUrl)
    }

    // Ensure conversation exists remotely
    var remoteConvId = localMsg.conversationId
    if (!ensurePeerSignalSession(engine, ownerUid, localMsg.recipientUserId)) {
      return false
    }
    val convResp = runCatching { api.openDirectConversation("Bearer $token", fp, localMsg.recipientUserId) }.getOrNull()
    val remoteConv = convResp?.body()?.conversation
    if (convResp?.isSuccessful == true && remoteConv != null && remoteConv.conversationId.isNotBlank()) {
      remoteConvId = remoteConv.conversationId
    }

    val env = signalMutex.withLock {
      runCatching {
        engine.compressAndEncrypt(localMsg.recipientUserId, localMsg.plainText)
      }.getOrNull()?.also {
        persistSignalState(ownerUid, engine)
      }
    } ?: return false

    val req = SendChatMessageRequestDto(
      clientOpId = localMsg.clientOpId,
      messageType = localMsg.messageType,
      compressionCodec = env.compressionCodec,
      encryptionScheme = env.encryptionScheme,
      ciphertextB64 = env.ciphertextB64,
      senderCiphertextB64 = env.senderCiphertextB64,
      signalMsgType = env.signalMsgType,
      mediaUrl = resolvedMediaUrl,
      mediaId = resolvedMediaId,
      voiceDurationMs = localMsg.voiceDurationMs,
      viewOnceTtlSec = localMsg.viewOnceTtlSec,
      forwardedFromUserId = localMsg.forwardedFromUserId,
    )
    val sendResp = runCatching { api.sendChatMessage("Bearer $token", fp, remoteConvId, req) }.getOrNull()
    val dto = sendResp?.body()?.message
    if (sendResp?.isSuccessful == true && sendResp.body()?.success == true && dto != null && dto.messageId.isNotBlank()) {
      dao.deleteChatMessagePermanently(localMsg.messageId)
      val persisted = mapMessageDto(dto, localMsg.plainText, localMsg.mediaLocalPath).copy(syncState = "synced")
      dao.upsertChatMessage(persisted)
      val targetConvId = dto.conversationId.ifBlank { remoteConvId }
      val targetConv = dao.getConversation(targetConvId)
        ?: dao.getConversation(localMsg.conversationId)?.copy(conversationId = targetConvId)
        ?: remoteConv?.let { mapConversationDto(it, null, previewTextForMessage(persisted)) }
      if (localMsg.conversationId != targetConvId) {
        dao.setConversationsDeletedLocally(listOf(localMsg.conversationId), true)
      }
      targetConv?.let { conv ->
        dao.upsertConversation(
          conv.copy(
            lastMessageId = persisted.messageId,
            lastMessageType = persisted.messageType,
            lastSenderId = persisted.senderUserId,
            lastDeliveryStatus = persisted.deliveryStatus,
            lastMessagePreviewPlain = previewTextForMessage(persisted),
            lastMessageAtMs = persisted.createdAtMs,
            updatedAtMs = persisted.createdAtMs.coerceAtLeast(conv.updatedAtMs),
          ),
        )
      }
      return true
    }
    return false
  }

  suspend fun editMessage(
    messageId: String,
    conversationId: String,
    peerUserId: String,
    newPlainText: String,
  ) = withContext(Dispatchers.IO) {
    val clean = newPlainText.trim()
    if (clean.isBlank()) return@withContext
    val nowMs = System.currentTimeMillis()
    dao.updateChatMessageEditedText(messageId, clean, nowMs)
    refreshConversationLastPreview(conversationId)

    val isOnline = networkMonitor.networkTier.value != telefam.company.core.NetworkTier.OFFLINE
    val ok = if (isOnline) sendRemoteEdit(messageId, peerUserId, clean) else false
    if (!ok) {
      val payload = JSONObject().put("peer_user_id", peerUserId).put("plain_text", clean).toString()
      dao.enqueueChatOp(
        PendingChatOpEntity(
          opId = "edit_$messageId",
          opKind = "edit",
          conversationId = conversationId,
          targetMessageId = messageId,
          payloadJson = payload,
          createdAtMs = nowMs,
          retryCount = 0,
          nextRetryAtMs = if (isOnline) nowMs + 2_500L else nowMs,
        ),
      )
    }
  }

  private suspend fun sendRemoteEdit(messageId: String, peerUserId: String, plainText: String): Boolean {
    val api = apiService ?: return false
    val session = identityStore.sessionFlow.first()
    val token = session.accessToken.orEmpty()
    val ownerUid = session.userId.orEmpty()
    if (token.isBlank() || ownerUid.isBlank()) return false
    val engine = ensureSignalEngineReady(false) ?: return false
    if (!ensurePeerSignalSession(engine, ownerUid, peerUserId)) return false
    val env = signalMutex.withLock {
      runCatching { engine.compressAndEncrypt(peerUserId, plainText) }.getOrNull()?.also {
        persistSignalState(ownerUid, engine)
      }
    } ?: return false
    val fp = deviceFingerprintProvider()
    val resp = runCatching {
      api.editChatMessage(
        "Bearer $token",
        fp,
        messageId,
        EditChatMessageRequestDto(env.ciphertextB64, env.senderCiphertextB64, env.signalMsgType),
      )
    }.getOrNull()
    return resp?.isSuccessful == true && resp.body()?.success == true
  }

  suspend fun unsendMessage(messageId: String, conversationId: String) = withContext(Dispatchers.IO) {
    val existing = dao.getChatMessage(messageId)
    deleteLocalVoiceFileIfAny(existing?.mediaLocalPath.orEmpty())
    dao.deleteChatMessagePermanently(messageId)
    dao.deleteChatMessageByClientOpId(messageId)
    refreshConversationLastPreview(conversationId)

    val api = apiService
    val token = identityStore.sessionFlow.first().accessToken.orEmpty()
    val isOnline = networkMonitor.networkTier.value != telefam.company.core.NetworkTier.OFFLINE
    var synced = false
    if (isOnline && api != null && token.isNotBlank()) {
      val fp = deviceFingerprintProvider()
      val resp = runCatching { api.deleteChatMessage("Bearer $token", fp, messageId, "unsend") }.getOrNull()
      synced = resp?.isSuccessful == true || resp?.code() == 404
    }
    if (!synced) {
      val nowMs = System.currentTimeMillis()
      dao.enqueueChatOp(
        PendingChatOpEntity(
          opId = "unsend_$messageId",
          opKind = "unsend",
          conversationId = conversationId,
          targetMessageId = messageId,
          payloadJson = "{}",
          createdAtMs = nowMs,
          retryCount = 0,
          nextRetryAtMs = if (isOnline) nowMs + 2_500L else nowMs,
        ),
      )
    }
  }

  suspend fun deleteMessageForSelf(messageId: String, conversationId: String) = withContext(Dispatchers.IO) {
    val existing = dao.getChatMessage(messageId)
    deleteLocalVoiceFileIfAny(existing?.mediaLocalPath.orEmpty())
    dao.deleteChatMessagePermanently(messageId)
    refreshConversationLastPreview(conversationId)

    val api = apiService
    val token = identityStore.sessionFlow.first().accessToken.orEmpty()
    val isOnline = networkMonitor.networkTier.value != telefam.company.core.NetworkTier.OFFLINE
    var synced = false
    if (isOnline && api != null && token.isNotBlank()) {
      val fp = deviceFingerprintProvider()
      val resp = runCatching { api.deleteChatMessage("Bearer $token", fp, messageId, "self") }.getOrNull()
      synced = resp?.isSuccessful == true || resp?.code() == 404
    }
    if (!synced) {
      val nowMs = System.currentTimeMillis()
      dao.enqueueChatOp(
        PendingChatOpEntity(
          opId = "del_self_$messageId",
          opKind = "delete_self",
          conversationId = conversationId,
          targetMessageId = messageId,
          payloadJson = "{}",
          createdAtMs = nowMs,
          retryCount = 0,
          nextRetryAtMs = nowMs + 2_500L,
        ),
      )
    }
  }

  suspend fun batchConversationAction(
    conversationIds: List<String>,
    action: String,
  ): List<ChatConversationEntity> = withContext(Dispatchers.IO) {
    if (conversationIds.isEmpty()) return@withContext emptyList()
    val snapshot = conversationIds.mapNotNull { dao.getConversation(it) }
    when (action) {
      "delete" -> dao.setConversationsDeletedLocally(conversationIds, true)
      "archive" -> dao.setConversationsArchived(conversationIds, true)
      "unarchive" -> dao.setConversationsArchived(conversationIds, false)
      "mute" -> dao.setConversationsMuted(conversationIds, true)
      "unmute" -> dao.setConversationsMuted(conversationIds, false)
      "mark_read" -> conversationIds.forEach { dao.setConversationUnread(it, 0) }
      "mark_unread" -> conversationIds.forEach { id ->
        val c = dao.getConversation(id)
        if (c != null && c.unreadCount == 0) dao.setConversationUnread(id, 1)
      }
      "block" -> conversationIds.forEach { id ->
        dao.getConversation(id)?.let { dao.upsertConversation(it.copy(isBlocked = true)) }
      }
    }
    val api = apiService
    val token = identityStore.sessionFlow.first().accessToken.orEmpty()
    var synced = false
    if (api != null && token.isNotBlank()) {
      val fp = deviceFingerprintProvider()
      val resp = runCatching {
        api.batchConversationAction("Bearer $token", fp, BatchConversationActionRequestDto(conversationIds, action))
      }.getOrNull()
      synced = resp?.isSuccessful == true && resp.body()?.success == true
    }
    if (!synced) {
      val nowMs = System.currentTimeMillis()
      val payload = JSONObject()
        .put("ids", JSONArray(conversationIds))
        .put("action", action)
        .toString()
      dao.enqueueChatOp(
        PendingChatOpEntity(
          opId = "batch_${UUID.randomUUID()}",
          opKind = "batch_conv",
          conversationId = conversationIds.first(),
          targetMessageId = action,
          payloadJson = payload,
          createdAtMs = nowMs,
          retryCount = 0,
          nextRetryAtMs = nowMs + 2_500L,
        ),
      )
    }
    snapshot
  }

  suspend fun restoreConversationsSnapshot(snapshot: List<ChatConversationEntity>, previousAction: String) = withContext(Dispatchers.IO) {
    if (snapshot.isEmpty()) return@withContext
    dao.upsertConversations(snapshot)
    val inverse = when (previousAction) {
      "archive" -> "unarchive"
      "mute" -> "unmute"
      "mark_read" -> "mark_unread"
      "mark_unread" -> "mark_read"
      "delete" -> "unarchive"
      else -> null
    }
    if (inverse != null) {
      val ids = snapshot.map { it.conversationId }
      val api = apiService
      val token = identityStore.sessionFlow.first().accessToken.orEmpty()
      if (api != null && token.isNotBlank()) {
        val fp = deviceFingerprintProvider()
        runCatching { api.batchConversationAction("Bearer $token", fp, BatchConversationActionRequestDto(ids, inverse)) }
      }
    }
  }

  suspend fun openViewOnceVoice(messageId: String, conversationId: String): ChatMessageEntity? = withContext(Dispatchers.IO) {
    val ownerUid = identityStore.sessionFlow.first().userId.orEmpty()
    val msg = dao.getChatMessage(messageId) ?: return@withContext null
    if (msg.senderUserId == ownerUid) return@withContext null // Sender cannot open own view-once

    val nowMs = System.currentTimeMillis()
    val expiresAtMs = if (msg.viewOnceTtlSec > 0) nowMs + msg.viewOnceTtlSec * 1000L else 0L
    dao.markChatMessageViewOnceOpened(messageId, nowMs, expiresAtMs)
    recordSecurityFlag(
      conversationId = conversationId,
      messageId = messageId,
      flagType = "view_once_opened",
      details = "View-once voice note opened (${if (msg.viewOnceTtlSec > 0) "${msg.viewOnceTtlSec}s timer" else "play once"})",
    )

    val api = apiService
    val token = identityStore.sessionFlow.first().accessToken.orEmpty()
    if (api != null && token.isNotBlank()) {
      val fp = deviceFingerprintProvider()
      val resp = runCatching { api.openViewOnceVoice("Bearer $token", fp, messageId) }.getOrNull()
      val remoteMsg = resp?.body()?.message
      if (resp?.isSuccessful == true && remoteMsg != null && remoteMsg.messageId.isNotBlank()) {
        val updated = mapMessageDto(remoteMsg, msg.plainText, msg.mediaLocalPath)
        dao.upsertChatMessage(updated)
        return@withContext updated
      }
    }
    dao.getChatMessage(messageId)
  }

  suspend fun consumeViewOnceVoice(messageId: String, conversationId: String) = withContext(Dispatchers.IO) {
    val msg = dao.getChatMessage(messageId)
    deleteLocalVoiceFileIfAny(msg?.mediaLocalPath.orEmpty())
    dao.deleteChatMessagePermanently(messageId)
    refreshConversationLastPreview(conversationId)

    val api = apiService
    val token = identityStore.sessionFlow.first().accessToken.orEmpty()
    var synced = false
    if (api != null && token.isNotBlank()) {
      val fp = deviceFingerprintProvider()
      val resp = runCatching { api.consumeViewOnceVoice("Bearer $token", fp, messageId) }.getOrNull()
      synced = resp?.isSuccessful == true || resp?.code() == 404
    }
    if (!synced) {
      val nowMs = System.currentTimeMillis()
      dao.enqueueChatOp(
        PendingChatOpEntity(
          opId = "consume_vo_$messageId",
          opKind = "consume_view_once",
          conversationId = conversationId,
          targetMessageId = messageId,
          payloadJson = "{}",
          createdAtMs = nowMs,
          retryCount = 0,
          nextRetryAtMs = nowMs + 2_000L,
        ),
      )
    }
  }

  suspend fun recordSecurityFlag(
    conversationId: String,
    messageId: String,
    flagType: String,
    details: String,
  ): ChatSecurityFlagEntity = withContext(Dispatchers.IO) {
    val session = identityStore.sessionFlow.first()
    val ownerUid = session.userId.orEmpty()
    val conv = dao.getConversation(conversationId)
    val flag = ChatSecurityFlagEntity(
      flagId = UUID.randomUUID().toString(),
      conversationId = conversationId,
      messageId = messageId,
      actorUserId = ownerUid,
      targetUserId = conv?.peerUserId.orEmpty(),
      flagType = flagType,
      details = details,
      isSynced = false,
      createdAtMs = System.currentTimeMillis(),
    )
    // Persisted locally immediately in Room so offline restarts & refreshes always preserve the security flag
    dao.upsertSecurityFlag(flag)

    val api = apiService
    val token = session.accessToken.orEmpty()
    if (api != null && token.isNotBlank()) {
      val fp = deviceFingerprintProvider()
      val resp = runCatching {
        api.recordSecurityFlag("Bearer $token", fp, conversationId, RecordSecurityFlagRequestDto(messageId, flagType, details))
      }.getOrNull()
      if (resp?.isSuccessful == true && resp.body()?.success == true) {
        dao.markSecurityFlagSynced(flag.flagId)
      }
    }
    flag
  }

  suspend fun fetchGreetingSticker(peerUserId: String): GiflyMediaItemDto? = withContext(Dispatchers.IO) {
    val api = apiService ?: return@withContext null
    val token = identityStore.sessionFlow.first().accessToken.orEmpty()
    if (token.isBlank()) return@withContext null
    val fp = deviceFingerprintProvider()
    val resp = runCatching { api.getGreetingSticker("Bearer $token", fp, peerUserId) }.getOrNull()
    val item = resp?.body()?.item
    if (resp?.isSuccessful == true && resp.body()?.success == true && item != null && item.url.isNotBlank()) item else null
  }

  suspend fun searchGiflyMedia(
    type: String,
    query: String,
    page: Int,
    perPage: Int = 24,
  ): GiflySearchResponseDto = withContext(Dispatchers.IO) {
    val api = apiService ?: return@withContext GiflySearchResponseDto(success = false, type = type, query = query, page = page)
    val token = identityStore.sessionFlow.first().accessToken.orEmpty()
    if (token.isBlank()) return@withContext GiflySearchResponseDto(success = false, type = type, query = query, page = page)
    val fp = deviceFingerprintProvider()
    val resp = runCatching { api.searchGiflyMedia("Bearer $token", fp, type, query, page, perPage) }.getOrNull()
    resp?.body() ?: GiflySearchResponseDto(success = false, type = type, query = query, page = page)
  }

  suspend fun searchChatsAndPeople(query: String): ChatSearchBundleResult = withContext(Dispatchers.IO) {
    val clean = query.trim()
    val allConvs = dao.getAllActiveConversations()
    val filteredLocal = if (clean.isBlank()) {
      allConvs
    } else {
      allConvs.filter {
        it.peerFullName.contains(clean, ignoreCase = true) ||
          it.peerUsername.contains(clean, ignoreCase = true) ||
          it.lastMessagePreviewPlain.contains(clean, ignoreCase = true)
      }
    }
    val matchingMsgs = if (clean.isBlank()) emptyList() else dao.searchDecryptedMessages(clean)
    val api = apiService
    val token = identityStore.sessionFlow.first().accessToken.orEmpty()
    val people = if (api != null && token.isNotBlank()) {
      val fp = deviceFingerprintProvider()
      val resp = runCatching { api.searchChatsAndPeople("Bearer $token", fp, clean) }.getOrNull()
      val body = resp?.body()
      if (resp?.isSuccessful == true && body?.success == true) {
        body.people
      } else {
        emptyList()
      }
    } else {
      emptyList()
    }
    ChatSearchBundleResult(
      conversations = filteredLocal,
      matchingMessages = matchingMsgs,
      people = people,
    )
  }

  suspend fun flushPendingChatOps() = withContext(Dispatchers.IO) {
    val api = apiService ?: return@withContext
    val session = identityStore.sessionFlow.first()
    val token = session.accessToken.orEmpty()
    if (token.isBlank()) return@withContext
    val fp = deviceFingerprintProvider()

    // 1. Flush unsynced security flags first so audit trail reaches backend immediately
    dao.getUnsyncedSecurityFlags().forEach { flag ->
      val resp = runCatching {
        api.recordSecurityFlag(
          "Bearer $token",
          fp,
          flag.conversationId,
          RecordSecurityFlagRequestDto(flag.messageId, flag.flagType, flag.details),
        )
      }.getOrNull()
      if (resp?.isSuccessful == true && resp.body()?.success == true) {
        dao.markSecurityFlagSynced(flag.flagId)
      }
    }

    // 2. Flush pending chat outbox ops
    val nowMs = System.currentTimeMillis()
    val ready = dao.getReadyChatOps(nowMs, limit = 25)
    for (op in ready) {
      val succeeded = when (op.opKind) {
        "send" -> {
          val msg = dao.getChatMessage(op.targetMessageId)
          if (msg == null) true else tryDeliverSingleMessage(msg)
        }
        "edit" -> {
          val obj = runCatching { JSONObject(op.payloadJson) }.getOrNull()
          val peer = obj?.optString("peer_user_id").orEmpty()
          val txt = obj?.optString("plain_text").orEmpty()
          if (peer.isBlank() || txt.isBlank()) true else sendRemoteEdit(op.targetMessageId, peer, txt)
        }
        "unsend" -> {
          val resp = runCatching { api.deleteChatMessage("Bearer $token", fp, op.targetMessageId, "unsend") }.getOrNull()
          resp?.isSuccessful == true || resp?.code() == 404
        }
        "delete_self" -> {
          val resp = runCatching { api.deleteChatMessage("Bearer $token", fp, op.targetMessageId, "self") }.getOrNull()
          resp?.isSuccessful == true || resp?.code() == 404
        }
        "consume_view_once" -> {
          val resp = runCatching { api.consumeViewOnceVoice("Bearer $token", fp, op.targetMessageId) }.getOrNull()
          resp?.isSuccessful == true || resp?.code() == 404
        }
        "batch_conv" -> {
          val obj = runCatching { JSONObject(op.payloadJson) }.getOrNull()
          val arr = obj?.optJSONArray("ids")
          val ids = buildList {
            if (arr != null) for (i in 0 until arr.length()) add(arr.getString(i))
          }
          val action = obj?.optString("action").orEmpty()
          if (ids.isEmpty() || action.isBlank()) true else {
            val resp = runCatching {
              api.batchConversationAction("Bearer $token", fp, BatchConversationActionRequestDto(ids, action))
            }.getOrNull()
            resp?.isSuccessful == true && resp.body()?.success == true
          }
        }
        else -> true
      }

      if (succeeded) {
        dao.deleteChatOp(op.opId)
      } else {
        val nextRetry = op.retryCount + 1
        val delayMs = (3_000L shl nextRetry.coerceAtMost(5)).coerceAtMost(120_000L)
        dao.scheduleChatOpRetry(op.opId, nextRetry, System.currentTimeMillis() + delayMs)
      }
    }
  }

  private suspend fun handleIncomingWsEvent(ev: ChatWsIncomingEvent) {
    val ownerUid = identityStore.sessionFlow.first().userId.orEmpty()
    when (ev.event) {
      "presence" -> {
        val conv = dao.getConversationByPeer(ev.userId) ?: return
        dao.upsertConversation(
          conv.copy(
            isOnline = ev.isOnline,
            lastSeenMs = if (ev.lastSeenMs > 0L) ev.lastSeenMs else conv.lastSeenMs,
            lastSeenLabel = ev.lastSeenLabel.ifBlank { conv.lastSeenLabel },
          ),
        )
      }
      "new_message", "message_new", "message_edited" -> {
        if (ev.conversationId.isNotBlank()) {
          loadConversationMessages(ev.conversationId, markAsRead = false)
          refreshConversations()
        } else {
          refreshConversations()
        }
      }
      "call_ended" -> {
        if (ev.conversationId.isNotBlank()) {
          loadConversationMessages(ev.conversationId, markAsRead = false)
        }
        refreshConversations()
      }
      "conversation_updated" -> {
        refreshConversations()
        if (ev.conversationId.isNotBlank()) {
          loadConversationMessages(ev.conversationId, markAsRead = false)
        }
      }
      "message_unsent", "view_once_burned", "view_once_consumed" -> {
        if (ev.messageId.isNotBlank()) {
          val existing = dao.getChatMessage(ev.messageId)
          deleteLocalVoiceFileIfAny(existing?.mediaLocalPath.orEmpty())
          dao.deleteChatMessagePermanently(ev.messageId)
          if (ev.conversationId.isNotBlank()) refreshConversationLastPreview(ev.conversationId)
        }
      }
      "message_receipt" -> {
        if (ev.messageId.isNotBlank() && ev.status.isNotBlank()) {
          dao.updateChatMessageDeliveryStatus(ev.messageId, ev.status)
          dao.getConversation(ev.conversationId)?.let { conv ->
            if (conv.lastMessageId == ev.messageId) {
              dao.upsertConversation(conv.copy(lastDeliveryStatus = ev.status))
            }
          }
        }
      }
      "view_once_opened" -> {
        if (ev.messageId.isNotBlank()) {
          val nowMs = ev.timestampMs.takeIf { it > 0L } ?: System.currentTimeMillis()
          dao.markChatMessageViewOnceOpened(ev.messageId, nowMs, 0L)
          if (ev.conversationId.isNotBlank() && ev.userId != ownerUid) {
            dao.upsertSecurityFlag(
              ChatSecurityFlagEntity(
                flagId = "ws_vo_${ev.messageId}",
                conversationId = ev.conversationId,
                messageId = ev.messageId,
                actorUserId = ev.userId,
                targetUserId = ownerUid,
                flagType = "view_once_opened",
                details = "Recipient opened view-once voice message",
                isSynced = true,
                createdAtMs = nowMs,
              ),
            )
          }
        }
      }
    }
  }

  private suspend fun refreshConversationLastPreview(conversationId: String) {
    val conv = dao.getConversation(conversationId) ?: return
    val msgs = dao.getMessagesForConversation(conversationId).filter { !it.isPendingUnsend }
    val last = msgs.lastOrNull()
    dao.upsertConversation(
      conv.copy(
        lastMessageId = last?.messageId.orEmpty(),
        lastMessageType = last?.messageType.orEmpty(),
        lastSenderId = last?.senderUserId.orEmpty(),
        lastMessagePreviewPlain = if (last != null) previewTextForMessage(last) else "",
        lastDeliveryStatus = last?.deliveryStatus ?: "sent",
        lastMessageAtMs = last?.createdAtMs ?: 0L,
      ),
    )
  }

  suspend fun decryptIncomingPushMessage(
    conversationId: String,
    messageId: String,
    senderUserId: String,
    senderName: String,
    senderUsername: String,
    senderAvatarUrl: String,
    messageType: String,
    compressionCodec: String,
    encryptionScheme: String,
    ciphertextB64: String,
    signalMsgType: Int,
    isViewOnce: Boolean,
    createdAtMs: Long,
    ownerUsername: String = "",
  ): PushDecryptedMessage? = withContext(Dispatchers.IO) {
    val session = identityStore.sessionFlow.first()
    val ownerUid = session.userId.orEmpty()
    if (ownerUid.isBlank() || senderUserId == ownerUid || conversationId.isBlank() || messageId.isBlank()) {
      return@withContext null
    }

    val existingConv = dao.getConversation(conversationId)
    if (existingConv?.isBlocked == true) return@withContext null

    if (isViewOnce || messageType.equals("voice_once", ignoreCase = true)) {
      val preview = "Sent a view-once voice message"
      val msgEntity = dao.getChatMessage(messageId) ?: ChatMessageEntity(
        messageId = messageId,
        conversationId = conversationId,
        senderUserId = senderUserId,
        recipientUserId = ownerUid,
        clientOpId = "",
        messageType = "voice_once",
        plainText = preview,
        mediaUrl = "",
        mediaLocalPath = "",
        mediaId = "",
        voiceDurationMs = 0L,
        viewOnceTtlSec = 10,
        viewOnceOpenedAtMs = 0L,
        viewOnceExpiresAtMs = 0L,
        forwardedFromUserId = "",
        deliveryStatus = "delivered",
        syncState = "synced",
        editedAtMs = 0L,
        isPendingUnsend = false,
        createdAtMs = createdAtMs.takeIf { it > 0L } ?: System.currentTimeMillis(),
      )
      dao.upsertChatMessage(msgEntity)
      upsertConversationFromPush(
        existingConv = existingConv,
        conversationId = conversationId,
        senderUserId = senderUserId,
        senderName = senderName,
        senderUsername = senderUsername,
        senderAvatarUrl = senderAvatarUrl,
        messageId = messageId,
        messageType = "voice_once",
        preview = preview,
        createdAtMs = msgEntity.createdAtMs,
      )
      return@withContext PushDecryptedMessage(
        conversationId = conversationId,
        messageId = messageId,
        peerUserId = senderUserId,
        previewText = preview,
        isViewOnce = true,
        isMuted = existingConv?.isMuted ?: false,
        canReply = false,
        mentionsOwner = false,
      )
    }

    val existingMsg = dao.getChatMessage(messageId)
    val engine = ensureSignalEngineReady(true)
    val plain = if (existingMsg != null && existingMsg.plainText.isNotBlank()) {
      existingMsg.plainText
    } else if (ciphertextB64.isNotBlank() && engine != null) {
      signalMutex.withLock {
        engine.decryptAndDecompress(
          peerUserId = senderUserId,
          isOutgoingFromSelf = false,
          ciphertextB64 = ciphertextB64,
          senderCiphertextB64 = "",
          signalMsgType = signalMsgType.takeIf { it > 0 } ?: 3,
          compressionCodec = compressionCodec.ifBlank { "zlib" },
          encryptionScheme = encryptionScheme.ifBlank { "libsignal_v1" },
        ).also {
          persistSignalState(ownerUid, engine)
        }
      }
    } else {
      null
    }

    val displayPreview = when (messageType) {
      "sticker" -> "Sent a sticker"
      "gif" -> "Sent a GIF"
      "clip" -> "Sent a clip"
      "voice" -> "Sent a voice message"
      else -> plain?.takeIf { it.isNotBlank() }?.lineSequence()?.firstOrNull().orEmpty().take(120)
        .ifBlank { "New encrypted message" }
    }

    val ts = createdAtMs.takeIf { it > 0L } ?: System.currentTimeMillis()
    if (existingMsg == null) {
      dao.upsertChatMessage(
        ChatMessageEntity(
          messageId = messageId,
          conversationId = conversationId,
          senderUserId = senderUserId,
          recipientUserId = ownerUid,
          clientOpId = "",
          messageType = messageType.ifBlank { "text" },
          plainText = plain ?: displayPreview,
          mediaUrl = "",
          mediaLocalPath = "",
          mediaId = "",
          voiceDurationMs = 0L,
          viewOnceTtlSec = 0,
          viewOnceOpenedAtMs = 0L,
          viewOnceExpiresAtMs = 0L,
          forwardedFromUserId = "",
          deliveryStatus = "delivered",
          syncState = "synced",
          editedAtMs = 0L,
          isPendingUnsend = false,
          createdAtMs = ts,
        ),
      )
    }
    upsertConversationFromPush(
      existingConv = existingConv,
      conversationId = conversationId,
      senderUserId = senderUserId,
      senderName = senderName,
      senderUsername = senderUsername,
      senderAvatarUrl = senderAvatarUrl,
      messageId = messageId,
      messageType = messageType.ifBlank { "text" },
      preview = displayPreview,
      createdAtMs = ts,
    )

    // Acknowledge delivered status to server
    val api = apiService
    val token = session.accessToken.orEmpty()
    if (api != null && token.isNotBlank()) {
      val fp = deviceFingerprintProvider()
      runCatching { api.sendChatReceipt("Bearer $token", fp, messageId, ChatReceiptRequestDto("delivered")) }
    }
    checkAndReplenishPreKeys(ownerUid, engine)

    val cleanOwnerHandle = ownerUsername.trim().removePrefix("@").lowercase()
    val mentionsOwner = cleanOwnerHandle.isNotBlank() && !plain.isNullOrBlank() &&
      Regex("""(?:^|[^a-zA-Z0-9_.@])@${Regex.escape(cleanOwnerHandle)}\b""", RegexOption.IGNORE_CASE).containsMatchIn(plain)

    PushDecryptedMessage(
      conversationId = conversationId,
      messageId = messageId,
      peerUserId = senderUserId,
      previewText = displayPreview,
      isViewOnce = false,
      isMuted = existingConv?.isMuted ?: false,
      canReply = existingConv?.isBlocked != true,
      mentionsOwner = mentionsOwner,
    )
  }

  private suspend fun upsertConversationFromPush(
    existingConv: ChatConversationEntity?,
    conversationId: String,
    senderUserId: String,
    senderName: String,
    senderUsername: String,
    senderAvatarUrl: String,
    messageId: String,
    messageType: String,
    preview: String,
    createdAtMs: Long,
  ) {
    val resolvedAvatar = senderAvatarUrl.takeIf { it.isNotBlank() }?.let {
      AdaptiveStreamSelector.resolveMediaUrl(baseHttpUrl, it)
    } ?: existingConv?.peerAvatarUrl.orEmpty()

    if (existingConv != null) {
      val bumpedUnread = if (existingConv.lastMessageId == messageId) existingConv.unreadCount else existingConv.unreadCount + 1
      dao.upsertConversation(
        existingConv.copy(
          peerFullName = senderName.ifBlank { existingConv.peerFullName },
          peerUsername = senderUsername.ifBlank { existingConv.peerUsername },
          peerAvatarUrl = resolvedAvatar,
          lastMessageId = messageId,
          lastMessageType = messageType,
          lastSenderId = senderUserId,
          lastMessagePreviewPlain = preview,
          lastDeliveryStatus = "delivered",
          lastMessageAtMs = createdAtMs,
          unreadCount = bumpedUnread,
          isArchived = false,
          isDeletedLocally = false,
          updatedAtMs = createdAtMs.coerceAtLeast(existingConv.updatedAtMs),
        ),
      )
    } else {
      dao.upsertConversation(
        ChatConversationEntity(
          conversationId = conversationId,
          peerUserId = senderUserId,
          peerFullName = senderName.ifBlank { senderUsername.ifBlank { "Telefam User" } },
          peerUsername = senderUsername,
          peerAvatarUrl = resolvedAvatar,
          lastMessageId = messageId,
          lastMessageType = messageType,
          lastSenderId = senderUserId,
          lastMessagePreviewPlain = preview,
          lastDeliveryStatus = "delivered",
          lastMessageAtMs = createdAtMs,
          unreadCount = 1,
          isArchived = false,
          isMuted = false,
          isDeletedLocally = false,
          isOnline = false,
          lastSeenMs = 0L,
          lastSeenLabel = "last seen recently",
          canCall = true,
          canScreenshot = false,
          canShareChats = false,
          canCopyMessages = false,
          canDownloadMedia = true,
          showReadReceipts = true,
          showTypingIndicator = false,
          showOnlineIndicator = false,
          isBlocked = false,
          draftText = "",
          updatedAtMs = createdAtMs,
        ),
      )
    }
  }

  suspend fun handleRemoteMessageCancel(conversationId: String, messageId: String) = withContext(Dispatchers.IO) {
    if (messageId.isBlank()) return@withContext
    val existing = dao.getChatMessage(messageId)
    deleteLocalVoiceFileIfAny(existing?.mediaLocalPath.orEmpty())
    dao.deleteChatMessagePermanently(messageId)
    if (conversationId.isNotBlank()) {
      refreshConversationLastPreview(conversationId)
    }
  }

  suspend fun sendQuickReplyFromNotification(
    conversationId: String,
    peerUserId: String,
    replyText: String,
  ): Boolean = withContext(Dispatchers.IO) {
    val clean = replyText.trim()
    if (clean.isBlank() || conversationId.isBlank() || peerUserId.isBlank()) return@withContext false
    val session = identityStore.sessionFlow.first()
    val ownerUid = session.userId.orEmpty()
    if (ownerUid.isBlank()) return@withContext false

    markConversationReadFromNotification(conversationId)
    val sent = sendTextMessage(conversationId, peerUserId, clean)
    sent != null
  }

  suspend fun markConversationReadFromNotification(conversationId: String): Boolean = withContext(Dispatchers.IO) {
    val cleanId = conversationId.trim()
    if (cleanId.isBlank()) return@withContext false
    val session = identityStore.sessionFlow.first()
    val ownerUid = session.userId.orEmpty()
    val token = session.accessToken.orEmpty()
    if (ownerUid.isBlank()) return@withContext false

    dao.clearConversationUnread(cleanId)
    val conv = dao.getConversation(cleanId)
    val api = apiService ?: return@withContext true
    if (token.isBlank()) return@withContext true
    val fp = deviceFingerprintProvider()
    val incoming = dao.getMessagesForConversation(cleanId).filter { it.senderUserId != ownerUid }
    val showReceipts = conv?.showReadReceipts ?: true
    if (showReceipts) {
      val latestUnseen = incoming.lastOrNull { it.deliveryStatus != "seen" }
      if (latestUnseen != null) {
        val resp = runCatching {
          api.sendChatReceipt("Bearer $token", fp, latestUnseen.messageId, ChatReceiptRequestDto("seen"))
        }.getOrNull()
        if (resp?.isSuccessful == true) {
          incoming.filter { it.deliveryStatus != "seen" }.forEach { msg ->
            dao.updateChatMessageDeliveryStatus(msg.messageId, "seen")
          }
        }
      }
    } else {
      val latestUndelivered = incoming.lastOrNull { it.deliveryStatus == "sent" }
      if (latestUndelivered != null) {
        val resp = runCatching {
          api.sendChatReceipt("Bearer $token", fp, latestUndelivered.messageId, ChatReceiptRequestDto("delivered"))
        }.getOrNull()
        if (resp?.isSuccessful == true) {
          incoming.filter { it.deliveryStatus == "sent" }.forEach { msg ->
            dao.updateChatMessageDeliveryStatus(msg.messageId, "delivered")
          }
        }
      }
    }
    runCatching {
      api.batchConversationAction("Bearer $token", fp, BatchConversationActionRequestDto(listOf(cleanId), "mark_read"))
    }
    runCatching {
      api.markNotificationRead(
        "Bearer $token",
        fp,
        telefam.company.data.network.MarkNotificationReadRequestDto(conversationId = cleanId),
      )
    }
    true
  }

  private fun decryptConversationPreviewLocked(
    engine: SignalE2eeManager?,
    ownerUid: String,
    dto: ChatConversationDto,
    existing: ChatConversationEntity?,
    cachedMsg: ChatMessageEntity?,
  ): String {
    if (dto.lastMessageId.isBlank()) return ""
    if (existing != null && existing.lastMessageId == dto.lastMessageId && existing.lastMessagePreviewPlain.isNotBlank()) {
      return existing.lastMessagePreviewPlain
    }
    if (cachedMsg != null && cachedMsg.plainText.isNotBlank()) {
      return previewTextForMessage(cachedMsg)
    }
    if (dto.lastMessageType == "call_log" || dto.lastMessageType == "call_request" ||
      dto.lastEncryptionScheme == "telefam_call_meta_v1"
    ) {
      val b64 = dto.lastCiphertextB64.ifBlank { dto.lastSenderCiphertextB64 }
      val decoded = decodeCallMetaB64(b64)
      if (decoded.isNotBlank()) {
        return TelefamCallMetaParser.formatSummaryLine(dto.lastMessageType, decoded)
      }
    }
    return when (dto.lastMessageType) {
      "sticker" -> "Sticker"
      "gif" -> "GIF"
      "clip" -> "Clip"
      "voice" -> "Voice message"
      "voice_once" -> "View-once voice message"
      "call_log" -> "Call"
      "call_request" -> "Call me request"
      else -> {
        val dec = engine?.decryptAndDecompress(
          peerUserId = dto.peerUserId,
          isOutgoingFromSelf = dto.lastSenderId == ownerUid,
          ciphertextB64 = dto.lastCiphertextB64,
          senderCiphertextB64 = dto.lastSenderCiphertextB64,
          signalMsgType = dto.lastSignalMsgType,
          compressionCodec = "zlib",
          encryptionScheme = dto.lastEncryptionScheme.ifBlank { "libsignal_v1" },
        )
        dec ?: existing?.lastMessagePreviewPlain?.takeIf { it.isNotBlank() } ?: "Encrypted message"
      }
    }
  }

  private fun decryptOrReusePlaintext(
    engine: SignalE2eeManager?,
    ownerUid: String,
    dto: ChatMessageDto,
    cached: ChatMessageEntity?,
  ): String {
    if (dto.encryptionScheme == "telefam_call_meta_v1" ||
      dto.messageType == "call_log" ||
      dto.messageType == "call_request"
    ) {
      val decoded = decodeCallMetaB64(dto.ciphertextB64.ifBlank { dto.senderCiphertextB64 })
      if (decoded.isNotBlank()) return decoded
    }
    // Double Ratchet forward secrecy deletes single-use message keys after first decryption.
    // Always preserve cached plaintext for same messageId unless message was edited remotely!
    if (cached != null && cached.plainText.isNotBlank() && dto.editedAtMs <= cached.editedAtMs) {
      return cached.plainText
    }
    val dec = engine?.decryptAndDecompress(
      peerUserId = if (dto.senderUserId == ownerUid) dto.recipientUserId else dto.senderUserId,
      isOutgoingFromSelf = dto.senderUserId == ownerUid,
      ciphertextB64 = dto.ciphertextB64,
      senderCiphertextB64 = dto.senderCiphertextB64,
      signalMsgType = dto.signalMsgType,
      compressionCodec = dto.compressionCodec,
      encryptionScheme = dto.encryptionScheme,
    )
    return dec ?: cached?.plainText ?: when (dto.messageType) {
      "sticker" -> "Sticker"
      "gif" -> "GIF"
      "clip" -> "Clip"
      "voice" -> "Voice message"
      "voice_once" -> "View-once voice message"
      "call_log" -> "Call"
      "call_request" -> "Call me request"
      else -> ""
    }
  }

  private fun mapConversationDto(
    dto: ChatConversationDto,
    existing: ChatConversationEntity?,
    previewPlain: String,
  ): ChatConversationEntity {
    val avatar = dto.peerAvatarUrl.takeIf { it.isNotBlank() }?.let {
      AdaptiveStreamSelector.resolveMediaUrl(baseHttpUrl, it)
    }.orEmpty()
    return ChatConversationEntity(
      conversationId = dto.conversationId,
      peerUserId = dto.peerUserId,
      peerFullName = dto.peerFullName.ifBlank { existing?.peerFullName.orEmpty() },
      peerUsername = dto.peerUsername.ifBlank { existing?.peerUsername.orEmpty() },
      peerAvatarUrl = avatar,
      lastMessageId = dto.lastMessageId,
      lastMessageType = dto.lastMessageType,
      lastSenderId = dto.lastSenderId,
      lastMessagePreviewPlain = previewPlain,
      lastDeliveryStatus = dto.lastDeliveryStatus,
      lastMessageAtMs = dto.lastMessageAtMs,
      unreadCount = dto.unreadCount,
      isArchived = dto.isArchived,
      isMuted = dto.isMuted || dto.permissions.isMuted,
      isDeletedLocally = existing?.isDeletedLocally ?: false,
      isOnline = dto.isOnline,
      lastSeenMs = dto.lastSeenMs,
      lastSeenLabel = dto.lastSeenLabel.ifBlank { "last seen recently" },
      canCall = dto.permissions.canCall,
      canScreenshot = dto.permissions.canScreenshot,
      canShareChats = dto.permissions.canShareChats,
      canCopyMessages = dto.permissions.canCopyMessages,
      canDownloadMedia = dto.permissions.canDownloadMedia,
      showReadReceipts = dto.permissions.showReadReceipts,
      showTypingIndicator = dto.permissions.showTypingIndicator,
      showOnlineIndicator = dto.permissions.showOnlineIndicator,
      isBlocked = dto.permissions.isBlocked,
      draftText = existing?.draftText.orEmpty(),
      updatedAtMs = dto.updatedAtMs.coerceAtLeast(dto.lastMessageAtMs),
    )
  }

  private fun mapMessageDto(
    dto: ChatMessageDto,
    plainText: String,
    localMediaPath: String,
  ): ChatMessageEntity {
    val url = dto.mediaUrl.takeIf { it.isNotBlank() }?.let {
      if (it.startsWith("http://") || it.startsWith("https://")) it else AdaptiveStreamSelector.resolveMediaUrl(baseHttpUrl, it)
    }.orEmpty()
    return ChatMessageEntity(
      messageId = dto.messageId,
      conversationId = dto.conversationId,
      senderUserId = dto.senderUserId,
      recipientUserId = dto.recipientUserId,
      clientOpId = dto.clientOpId,
      messageType = dto.messageType,
      plainText = plainText,
      mediaUrl = url,
      mediaLocalPath = localMediaPath,
      mediaId = dto.mediaId,
      voiceDurationMs = dto.voiceDurationMs,
      viewOnceTtlSec = dto.viewOnceTtlSec,
      viewOnceOpenedAtMs = dto.viewOnceOpenedAtMs,
      viewOnceExpiresAtMs = dto.viewOnceExpiresAtMs,
      forwardedFromUserId = dto.forwardedFromUserId,
      deliveryStatus = dto.deliveryStatus,
      syncState = "synced",
      editedAtMs = dto.editedAtMs,
      isPendingUnsend = false,
      createdAtMs = dto.createdAtMs,
    )
  }

  private fun mapFlagDto(dto: ChatSecurityFlagDto, isSynced: Boolean): ChatSecurityFlagEntity =
    ChatSecurityFlagEntity(
      flagId = dto.flagId,
      conversationId = dto.conversationId,
      messageId = dto.messageId,
      actorUserId = dto.actorUserId,
      targetUserId = dto.targetUserId,
      flagType = dto.flagType,
      details = dto.details,
      isSynced = isSynced,
      createdAtMs = dto.createdAtMs,
    )

  private fun previewTextForMessage(msg: ChatMessageEntity): String = when (msg.messageType) {
    "sticker" -> "Sticker"
    "gif" -> "GIF"
    "clip" -> "Clip"
    "voice" -> "Voice message"
    "voice_once" -> "View-once voice message"
    "call_log", "call_request" -> TelefamCallMetaParser.formatSummaryLine(msg.messageType, msg.plainText)
    else -> msg.plainText.lineSequence().firstOrNull().orEmpty().take(90)
  }

  private fun decodeCallMetaB64(b64: String): String {
    val clean = b64.trim()
    if (clean.isBlank()) return ""
    return runCatching {
      String(Base64.getDecoder().decode(clean), Charsets.UTF_8)
    }.getOrDefault("")
  }

  private fun deleteLocalVoiceFileIfAny(path: String) {
    if (path.isNotBlank()) runCatching { File(path).delete() }
  }

  private fun deterministicDirectConvId(a: String, b: String): String {
    val pair = listOf(a, b).sorted()
    return "local_dm_${pair[0]}_${pair[1]}"
  }
}
