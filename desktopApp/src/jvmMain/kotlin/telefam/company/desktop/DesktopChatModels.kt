package telefam.company.desktop

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class DesktopChatPeerCapabilities(
  val canCall: Boolean = true,
  val canScreenshot: Boolean = false,
  val canShareChats: Boolean = false,
  val canCopyMessages: Boolean = false,
  val canDownloadMedia: Boolean = true,
  val showReadReceipts: Boolean = true,
  val showTypingIndicator: Boolean = false,
  val showOnlineIndicator: Boolean = false,
  val isBlocked: Boolean = false,
  val isMuted: Boolean = false,
)

data class DesktopChatConversation(
  val conversationId: String,
  val peerUserId: String,
  val peerFullName: String,
  val peerUsername: String,
  val peerAvatarUrl: String = "",
  val lastMessageId: String = "",
  val lastMessageType: String = "",
  val lastSenderId: String = "",
  val lastMessagePreviewPlain: String = "",
  val lastDeliveryStatus: String = "sent",
  val lastMessageAtMs: Long = 0L,
  val unreadCount: Int = 0,
  val isArchived: Boolean = false,
  val isMuted: Boolean = false,
  val isDeletedLocally: Boolean = false,
  val isOnline: Boolean = false,
  val lastSeenMs: Long = 0L,
  val lastSeenLabel: String = "last seen recently",
  val capabilities: DesktopChatPeerCapabilities = DesktopChatPeerCapabilities(),
  val draftText: String = "",
  val createdAtMs: Long = 0L,
  val updatedAtMs: Long = 0L,
)

data class DesktopChatMessage(
  val messageId: String,
  val conversationId: String,
  val senderUserId: String,
  val recipientUserId: String,
  val clientOpId: String = "",
  val messageType: String = "text", // text | sticker | gif | clip | voice | voice_once
  val compressionCodec: String = "zlib",
  val encryptionScheme: String = "telefam_ecdh_v1",
  val ciphertextB64: String = "",
  val senderCiphertextB64: String = "",
  val signalMsgType: Int = 3,
  val plainText: String = "",
  val mediaUrl: String = "",
  val mediaLocalPath: String = "",
  val mediaId: String = "",
  val voiceDurationMs: Long = 0L,
  val viewOnceTtlSec: Int = 0,
  val viewOnceOpenedAtMs: Long = 0L,
  val viewOnceExpiresAtMs: Long = 0L,
  val forwardedFromUserId: String = "",
  val deliveryStatus: String = "sent", // sending | sent | delivered | seen
  val syncState: String = "synced", // pending | synced
  val editedAtMs: Long = 0L,
  val createdAtMs: Long = 0L,
)

data class DesktopChatSecurityFlag(
  val flagId: String,
  val conversationId: String,
  val messageId: String = "",
  val actorUserId: String = "",
  val targetUserId: String = "",
  val flagType: String,
  val details: String = "",
  val isSynced: Boolean = true,
  val createdAtMs: Long = 0L,
)

data class DesktopGiflyMediaItem(
  val id: Long = 0L,
  val slug: String = "",
  val title: String = "",
  val type: String = "sticker",
  val tags: List<String> = emptyList(),
  val preview: String = "",
  val previewWebp: String = "",
  val url: String = "",
  val urlHd: String = "",
  val mp4: String = "",
  val width: Int = 200,
  val height: Int = 200,
) {
  val bestAnimatedUrl: String
    get() = url.ifBlank { previewWebp }.ifBlank { preview }.ifBlank { urlHd }
}

data class DesktopChatSearchPerson(
  val userId: String,
  val fullName: String,
  val username: String,
  val avatarUrl: String = "",
  val location: String = "",
  val conversationId: String = "",
)

data class DesktopChatSearchBundle(
  val conversations: List<DesktopChatConversation> = emptyList(),
  val matchingMessages: List<DesktopChatMessage> = emptyList(),
  val people: List<DesktopChatSearchPerson> = emptyList(),
)

data class DesktopPendingChatOp(
  val opId: String,
  val opKind: String, // send | edit | unsend | delete_self | consume_view_once | batch_conv | security_flag
  val conversationId: String,
  val targetMessageId: String = "",
  val payloadJson: String = "{}",
  val createdAtMs: Long = 0L,
  val retryCount: Int = 0,
  val nextRetryAtMs: Long = 0L,
)

data class DesktopChatUndoBanner(
  val message: String,
  val previousAction: String,
  val snapshot: List<DesktopChatConversation>,
)

data class DesktopChatWsEvent(
  val event: String,
  val conversationId: String = "",
  val userId: String = "",
  val messageId: String = "",
  val status: String = "",
  val typingMode: String = "",
  val isOnline: Boolean = false,
  val lastSeenMs: Long = 0L,
  val lastSeenLabel: String = "",
  val rawJson: String = "",
  val timestampMs: Long = 0L,
)

data class DesktopPeerE2eeBundle(
  val userId: String,
  val deviceId: Int,
  val registrationId: Int,
  val identityPubB64: String,
  val signedPreKeyId: Int,
  val signedPreKeyPubB64: String,
  val signedPreKeySigB64: String,
  val kyberPreKeyPubB64: String,
  val preKeyId: Int,
  val preKeyPubB64: String,
)

enum class DesktopGiflyTab(val apiValue: String, val label: String) {
  STICKERS("stickers", "Stickers"),
  GIFS("gifs", "GIFs"),
  CLIPS("clips", "Clips"),
}

internal object DesktopChatCodec {
  fun urlEncode(raw: String): String = URLEncoder.encode(raw, StandardCharsets.UTF_8)

  fun parsePeerCapabilities(m: Map<String, Any?>): DesktopChatPeerCapabilities =
    DesktopChatPeerCapabilities(
      canCall = if (m.containsKey("can_call")) DesktopJson.bool(m, "can_call") else true,
      canScreenshot = DesktopJson.bool(m, "can_screenshot"),
      canShareChats = DesktopJson.bool(m, "can_share_chats"),
      canCopyMessages = DesktopJson.bool(m, "can_copy_messages"),
      canDownloadMedia = if (m.containsKey("can_download_media")) DesktopJson.bool(m, "can_download_media") else true,
      showReadReceipts = if (m.containsKey("show_read_receipts")) DesktopJson.bool(m, "show_read_receipts") else true,
      showTypingIndicator = DesktopJson.bool(m, "show_typing_indicator"),
      showOnlineIndicator = DesktopJson.bool(m, "show_online_indicator"),
      isBlocked = DesktopJson.bool(m, "is_blocked"),
      isMuted = DesktopJson.bool(m, "is_muted"),
    )

  fun parseGiflyItem(m: Map<String, Any?>): DesktopGiflyMediaItem = DesktopGiflyMediaItem(
    id = DesktopJson.long(m, "id", 0L),
    slug = DesktopJson.str(m, "slug"),
    title = DesktopJson.str(m, "title"),
    type = DesktopJson.str(m, "type").ifBlank { "sticker" },
    tags = DesktopJson.stringList(m, "tags"),
    preview = DesktopJson.str(m, "preview"),
    previewWebp = DesktopJson.str(m, "preview_webp"),
    url = DesktopJson.str(m, "url"),
    urlHd = DesktopJson.str(m, "url_hd"),
    mp4 = DesktopJson.str(m, "mp4"),
    width = DesktopJson.long(m, "width", 200L).toInt().coerceAtLeast(1),
    height = DesktopJson.long(m, "height", 200L).toInt().coerceAtLeast(1),
  )

  fun parseSecurityFlag(m: Map<String, Any?>, synced: Boolean = true): DesktopChatSecurityFlag =
    DesktopChatSecurityFlag(
      flagId = DesktopJson.str(m, "flag_id"),
      conversationId = DesktopJson.str(m, "conversation_id"),
      messageId = DesktopJson.str(m, "message_id"),
      actorUserId = DesktopJson.str(m, "actor_user_id"),
      targetUserId = DesktopJson.str(m, "target_user_id"),
      flagType = DesktopJson.str(m, "flag_type"),
      details = DesktopJson.str(m, "details"),
      isSynced = synced,
      createdAtMs = DesktopJson.long(m, "created_at_ms", 0L),
    )
}
