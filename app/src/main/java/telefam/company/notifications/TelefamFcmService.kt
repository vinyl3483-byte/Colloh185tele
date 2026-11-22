package telefam.company.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import telefam.company.app.TelefamApplication
import telefam.company.calls.IncomingCallPushBridge
import telefam.company.calls.IncomingCallPushPayload
import telefam.company.calls.TelefamCallType
import telefam.company.data.local.CachedNotificationEntity
import telefam.company.data.network.CallSignalRequestDto
import telefam.company.data.repository.TelefamRepository

class TelefamFcmService : FirebaseMessagingService() {

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    val repo = (applicationContext as? TelefamApplication)?.repository ?: return
    repo.registerPushTokenAsync(token)
  }

  override fun onMessageReceived(message: RemoteMessage) {
    super.onMessageReceived(message)
    val repo = (applicationContext as? TelefamApplication)?.repository ?: return
    val data = message.data
    if (data.isEmpty()) return

    runBlocking {
      handlePushPayload(repo, data)
    }
  }

  internal suspend fun handlePushPayload(
    repo: TelefamRepository,
    data: Map<String, String>,
  ) {
    val session = repo.sessionFlow.first()
    val ownerUserId = session.userId.orEmpty().trim()
    if (ownerUserId.isBlank() || session.accessToken.isNullOrBlank()) return

    val targetRecipient = data["recipient_user_id"].orEmpty().trim()
    if (targetRecipient.isNotBlank() && targetRecipient != ownerUserId) {
      return
    }

    val type = data["type"].orEmpty().trim().lowercase()
    val settings = runCatching { repo.loadCachedSettings().notifications }.getOrNull()

    when (type) {
      "chat_message" -> {
        val conversationId = data["conversation_id"].orEmpty().trim()
        val messageId = data["message_id"].orEmpty().trim()
        val senderUserId = data["sender_user_id"].orEmpty().trim()
        val senderName = data["sender_name"].orEmpty().trim()
        val senderUsername = data["sender_username"].orEmpty().trim()
        val senderAvatarUrl = data["sender_avatar_url"].orEmpty().trim()
        val messageType = data["message_type"].orEmpty().trim().ifBlank { "text" }
        val compressionCodec = data["compression_codec"].orEmpty().trim().ifBlank { "zlib" }
        val encryptionScheme = data["encryption_scheme"].orEmpty().trim().ifBlank { "libsignal_v1" }
        val ciphertextB64 = data["ciphertext_b64"].orEmpty()
        val signalMsgType = data["signal_msg_type"]?.toIntOrNull() ?: 3
        val isViewOnce = data["is_view_once"] == "true" || messageType.equals("voice_once", ignoreCase = true)
        val createdAtMs = data["created_at_ms"]?.toLongOrNull() ?: System.currentTimeMillis()
        val ownerUsername = runCatching { repo.observeProfile(ownerUserId).first()?.username.orEmpty() }.getOrDefault("")

        val decrypted = repo.chatRepository.decryptIncomingPushMessage(
          conversationId = conversationId,
          messageId = messageId,
          senderUserId = senderUserId,
          senderName = senderName,
          senderUsername = senderUsername,
          senderAvatarUrl = senderAvatarUrl,
          messageType = messageType,
          compressionCodec = compressionCodec,
          encryptionScheme = encryptionScheme,
          ciphertextB64 = ciphertextB64,
          signalMsgType = signalMsgType,
          isViewOnce = isViewOnce,
          createdAtMs = createdAtMs,
          ownerUsername = ownerUsername,
        ) ?: return

        val notifAllowed = settings == null || TelefamNotificationManager.shouldDeliverPushNotification(settings, "chat_message")
        if (!decrypted.isMuted && notifAllowed) {
          val displaySender = senderName.ifBlank {
            senderUsername.takeIf { it.isNotBlank() }?.let { "@$it" }.orEmpty()
          }
          TelefamNotificationManager.showChatMessageNotification(
            context = applicationContext,
            conversationId = decrypted.conversationId,
            messageId = decrypted.messageId,
            peerUserId = decrypted.peerUserId,
            senderDisplayName = displaySender,
            previewText = decrypted.previewText,
            isViewOnce = decrypted.isViewOnce,
            canReply = decrypted.canReply,
            timestampMs = createdAtMs,
          )
        }
      }

      "message_cancel" -> {
        val conversationId = data["conversation_id"].orEmpty().trim()
        val messageId = data["message_id"].orEmpty().trim()
        repo.chatRepository.handleRemoteMessageCancel(conversationId, messageId)
        repo.deleteNotificationsByMessageIdLocal(messageId)
        if (conversationId.isNotBlank()) {
          TelefamNotificationManager.cancelConversationNotification(applicationContext, conversationId)
        }
      }

      "follow", "like", "mention", "star_received" -> {
        val notifId = data["notification_id"].orEmpty().trim()
          .ifBlank { "fcm_${type}_${data["actor_user_id"].orEmpty()}_${data["post_id"].orEmpty()}_${System.currentTimeMillis()}" }
        val actorUserId = data["actor_user_id"].orEmpty().trim()
        if (actorUserId == ownerUserId) return
        val actorName = data["actor_name"].orEmpty().trim()
        val actorUsername = data["actor_username"].orEmpty().trim()
        val actorAvatarUrl = data["actor_avatar_url"].orEmpty().trim()
        val title = data["title"].orEmpty().trim().ifBlank {
          actorName.ifBlank { actorUsername.ifBlank { "Telefam" } }
        }
        val starsAmount = data["stars_amount"]?.toLongOrNull() ?: 0L
        val body = data["body"].orEmpty().trim().ifBlank {
          when (type) {
            "follow" -> "Started following you"
            "like" -> "Liked your post"
            "star_received" -> if (starsAmount > 0L) "Sent you $starsAmount Telefam Stars" else "Sent you Telefam Stars"
            else -> "Mentioned you"
          }
        }
        val postId = data["post_id"].orEmpty().trim()
        val conversationId = data["conversation_id"].orEmpty().trim()
        val targetUserId = data["target_user_id"].orEmpty().trim().ifBlank { actorUserId }
        val createdAtMs = data["created_at_ms"]?.toLongOrNull() ?: System.currentTimeMillis()

        repo.upsertLocalNotification(
          CachedNotificationEntity(
            id = notifId,
            userId = ownerUserId,
            actorUserId = actorUserId,
            actorName = actorName,
            actorUsername = actorUsername,
            actorAvatarUrl = actorAvatarUrl,
            kind = type,
            title = title,
            body = body,
            conversationId = conversationId,
            messageId = data["message_id"].orEmpty().trim(),
            postId = postId,
            targetUserId = targetUserId,
            isViewOnce = false,
            isRead = false,
            createdAtMs = createdAtMs,
          ),
        )
        if (type == "star_received" && conversationId.isNotBlank()) {
          runCatching { repo.chatRepository.loadConversationMessages(conversationId, markAsRead = false) }
        }

        val notifAllowed = settings == null || TelefamNotificationManager.shouldDeliverPushNotification(settings, type)
        if (notifAllowed) {
          TelefamNotificationManager.showSocialNotification(
            context = applicationContext,
            notificationId = notifId,
            kind = type,
            title = title,
            body = body,
            actorUserId = actorUserId,
            postId = postId,
            conversationId = conversationId,
            timestampMs = createdAtMs,
          )
        }
      }

      "incoming_call" -> {
        val callId = data["call_id"].orEmpty().trim()
        val convId = data["conversation_id"].orEmpty().trim()
        val callerUserId = data["caller_user_id"].orEmpty().trim()
        if (callId.isBlank() || callerUserId.isBlank() || callerUserId == ownerUserId) return

        val notifAllowed = settings == null || (!settings.masterPause && settings.channelPush)
        if (!notifAllowed) return

        val callType = TelefamCallType.fromWire(data["call_type"])
        val callerName = data["caller_name"].orEmpty().trim()
        val callerUsername = data["caller_username"].orEmpty().trim()
        val callerAvatarUrl = repo.chatRepository.resolveCallAvatarUrl(data["caller_avatar_url"].orEmpty())
        val expiresAtMs = data["expires_at_ms"]?.toLongOrNull() ?: (System.currentTimeMillis() + 25_000L)

        // Acknowledge ringing in real time so the caller's "Calling..." switches to "Ringing..."
        runCatching {
          repo.chatRepository.sendCallSignal(callId, CallSignalRequestDto(action = "ringing"))
        }

        TelefamNotificationManager.showIncomingCallNotification(
          context = applicationContext,
          callId = callId,
          conversationId = convId,
          callType = callType.wireValue,
          callerUserId = callerUserId,
          callerName = callerName,
          callerUsername = callerUsername,
          callerAvatarUrl = callerAvatarUrl,
          expiresAtMs = expiresAtMs,
        )
        IncomingCallPushBridge.emitIncoming(
          IncomingCallPushPayload(
            callId = callId,
            conversationId = convId,
            callType = callType,
            callerUserId = callerUserId,
            callerName = callerName.ifBlank { callerUsername.ifBlank { "Telefam" } },
            callerUsername = callerUsername,
            callerAvatarUrl = callerAvatarUrl,
            expiresAtMs = expiresAtMs,
            autoAccept = false,
          ),
        )
      }

      "call_cancel" -> {
        val callId = data["call_id"].orEmpty().trim()
        val convId = data["conversation_id"].orEmpty().trim()
        val reason = data["reason"].orEmpty().trim()
        if (callId.isNotBlank()) {
          TelefamNotificationManager.cancelCallNotification(applicationContext, callId)
          IncomingCallPushBridge.emitCancelled(callId, reason)
        }
        if (convId.isNotBlank()) {
          runCatching { repo.chatRepository.loadConversationMessages(convId, markAsRead = false) }
        }
      }

      "missed_call" -> {
        val callId = data["call_id"].orEmpty().trim()
        val convId = data["conversation_id"].orEmpty().trim()
        val actorUserId = data["actor_user_id"].orEmpty().trim()
        val actorName = data["actor_name"].orEmpty().trim()
        val title = data["title"].orEmpty().trim().ifBlank { actorName.ifBlank { "Telefam" } }
        val body = data["body"].orEmpty().trim().ifBlank { "Missed call" }
        val createdAtMs = data["created_at_ms"]?.toLongOrNull() ?: System.currentTimeMillis()
        if (callId.isNotBlank()) {
          TelefamNotificationManager.cancelCallNotification(applicationContext, callId)
        }
        val notifAllowed = settings == null || TelefamNotificationManager.shouldDeliverPushNotification(settings, "chat_message")
        if (notifAllowed) {
          TelefamNotificationManager.showSocialNotification(
            context = applicationContext,
            notificationId = data["notification_id"].orEmpty().ifBlank { "missed_$callId" },
            kind = "missed_call",
            title = title,
            body = body,
            actorUserId = actorUserId,
            conversationId = convId,
            timestampMs = createdAtMs,
          )
        }
        if (convId.isNotBlank()) {
          runCatching { repo.chatRepository.loadConversationMessages(convId, markAsRead = false) }
        }
      }
    }
  }
}
