package telefam.company.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "rate_limit_state")
data class RateLimitStateEntity(
  @PrimaryKey val compositeKey: String,
  val accountHash: String,
  val deviceFp: String,
  val sendCount: Int,
  val nextAllowedEpochMs: Long,
  val activeCooldownDurationSeconds: Long,
  val lastUpdatedEpochMs: Long,
)

@Entity(
  tableName = "rate_limit_hits",
  indices = [Index(value = ["dimensionKey", "timestampMs"])],
)
data class RateLimitHitEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val dimensionKey: String,
  val timestampMs: Long,
)

@Entity(
  tableName = "user_profiles",
  indices = [Index(value = ["usernameLower"], unique = true)],
)
data class UserProfileEntity(
  @PrimaryKey val userId: String,
  val email: String,
  val fullName: String,
  val username: String,
  val usernameLower: String,
  val phoneE164: String,
  val countryIso: String,
  val gender: String,
  val dateOfBirthIso: String,
  val location: String,
  val bio: String,
  val avatarMediaId: String?,
  val avatarLocalPath: String?,
  val avatarSha256: String?,
  val avatarDHash: String?,
  val usernameChangedAtMs: Long = 0L,
  val backgroundsJson: String = "[]",
  val updatedAtMs: Long,
)

@Entity(tableName = "media_uploads")
data class MediaUploadEntity(
  @PrimaryKey val mediaId: String,
  val ownerUserId: String,
  val storageObjectKey: String,
  val mimeType: String,
  val sha256Fingerprint: String,
  val dHashFingerprint: String,
  val byteSize: Int,
  val localFilePath: String,
  val createdAtMs: Long,
)

@Entity(tableName = "refresh_token_records")
data class RefreshTokenRecordEntity(
  @PrimaryKey val tokenHash: String,
  val familyId: String,
  val userId: String,
  val deviceFp: String,
  val expiresAtMs: Long,
  val revoked: Boolean,
  val createdAtMs: Long,
)

@Entity(
  tableName = "audit_logs",
  indices = [Index(value = ["userId", "createdAtMs"])],
)
data class AuditLogEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val userId: String,
  val action: String,
  val deviceFp: String,
  val ipFp: String,
  val metadataJson: String,
  val createdAtMs: Long,
)

@Entity(tableName = "family_circles")
data class FamilyCircleEntity(
  @PrimaryKey val id: String,
  val name: String,
  val memberSummary: String,
  val lastMessage: String,
  val lastTimestampMs: Long,
)

@Entity(
  tableName = "family_messages",
  indices = [Index(value = ["circleId", "createdAtMs"])],
)
data class FamilyMessageEntity(
  @PrimaryKey val id: String,
  val circleId: String,
  val senderLabel: String,
  val body: String,
  val createdAtMs: Long,
  val isMine: Boolean,
  val syncStatus: String,
)

@Entity(tableName = "offline_actions")
data class OfflineActionEntity(
  @PrimaryKey val clientOpId: String,
  val circleId: String,
  val kind: String,
  val payloadJson: String,
  val createdAtMs: Long,
  val retryCount: Int,
  val nextRetryAtMs: Long,
)

@Entity(
  tableName = "cached_contacts",
  indices = [Index(value = ["usernameLower"]), Index(value = ["fullName"])],
)
data class CachedContactEntity(
  @PrimaryKey val userId: String,
  val fullName: String,
  val username: String,
  val usernameLower: String,
  val location: String,
  val countryIso: String,
  val avatarUrl: String?,
  val avatarLocalPath: String?,
  val isFollowing: Boolean,
  val followsYou: Boolean,
  val riskScore: Double,
  val updatedAtMs: Long,
  val cachedAtMs: Long,
)

@Entity(tableName = "contact_sync_actions")
data class ContactSyncActionEntity(
  @PrimaryKey val clientOpId: String,
  val targetUserId: String,
  val wantFollow: Boolean,
  val createdAtMs: Long,
  val retryCount: Int,
  val nextRetryAtMs: Long,
)

@Entity(tableName = "cached_hashtags")
data class CachedHashtagEntity(
  @PrimaryKey val tag: String,
  val postCount: Long,
  val updatedAtMs: Long,
)

@Entity(tableName = "pending_post_uploads")
data class PendingPostUploadEntity(
  @PrimaryKey val clientOpId: String,
  val mode: String,
  val textContent: String,
  val caption: String,
  val themeId: String,
  val fontId: String,
  val songJson: String?,
  val taggedUserIdsJson: String,
  val hashtagsJson: String,
  val localPhotoPathsJson: String,
  val localVideoPath: String?,
  val localThumbnailPath: String?,
  val frameHashesJson: String,
  val durationMs: Long,
  val trimStartMs: Long,
  val trimEndMs: Long,
  val width: Int,
  val height: Int,
  val muteOriginalAudio: Boolean,
  val contentId: String,
  val status: String,
  val progressPct: Int,
  val retryCount: Int,
  val nextRetryAtMs: Long,
  val createdAtMs: Long,
)

@Entity(
  tableName = "cached_feed_posts",
  indices = [
    Index(value = ["tabKey", "sortOrder"]),
    Index(value = ["authorUserId"]),
  ],
)
data class CachedFeedPostEntity(
  @PrimaryKey val postId: String,
  val authorUserId: String,
  val authorUsername: String,
  val authorFullName: String,
  val authorAvatarUrl: String?,
  val isFollowingAuthor: Boolean,
  val isOwner: Boolean,
  val mode: String,
  val textContent: String,
  val caption: String,
  val themeId: String,
  val fontId: String,
  val songJson: String?,
  val hashtagsJson: String,
  val photoUrlsJson: String,
  val videoUrl: String,
  val thumbnailUrl: String,
  val renditionsJson: String,
  val width: Int,
  val height: Int,
  val durationMs: Long,
  val muteOriginalAudio: Boolean,
  val contentId: String,
  val likesCount: Long,
  val commentsCount: Long,
  val sharesCount: Long,
  val favouritesCount: Long,
  val repostsCount: Long,
  val isLiked: Boolean,
  val isFavourited: Boolean,
  val isReposted: Boolean,
  val repostedByUsername: String,
  val recommendationLabel: String,
  val relevanceScore: Double,
  val tabKey: String,
  val sortOrder: Int,
  val isHiddenByUser: Boolean,
  val viewsCount: Long = 0L,
  val privacy: String = "public",
  val createdAtMs: Long,
  val cachedAtMs: Long,
)

@Entity(
  tableName = "cached_user_profiles",
  indices = [Index(value = ["username"])],
)
data class CachedUserProfileEntity(
  @PrimaryKey val userId: String,
  val fullName: String,
  val username: String,
  val avatarUrl: String?,
  val avatarLocalPath: String?,
  val bio: String,
  val location: String,
  val countryIso: String,
  val followingCount: Long,
  val followersCount: Long,
  val likesCount: Long,
  val isFollowing: Boolean,
  val followsYou: Boolean,
  val isOwner: Boolean,
  val isBlocked: Boolean,
  val riskScore: Double,
  val relationshipState: String,
  val gender: String = "",
  val dateOfBirth: String = "",
  val phoneNumber: String = "",
  val usernameChangedAtMs: Long = 0L,
  val nextUsernameChangeMs: Long = 0L,
  val backgroundUrlsJson: String = "[]",
  val backgroundsJson: String = "[]",
  val postsJson: String,
  val savedPostsJson: String = "[]",
  val repostedPostsJson: String = "[]",
  val lockedPostsJson: String = "[]",
  val cachedAtMs: Long,
)

@Entity(
  tableName = "cached_user_connections",
  indices = [Index(value = ["ownerUserId", "listType", "sortOrder"])],
)
data class CachedUserConnectionEntity(
  @PrimaryKey val compositeKey: String, // "$ownerUserId|$listType|$targetUserId"
  val ownerUserId: String,
  val listType: String, // "followers" or "following"
  val targetUserId: String,
  val fullName: String,
  val username: String,
  val location: String,
  val countryIso: String,
  val avatarUrl: String?,
  val avatarLocalPath: String?,
  val isFollowing: Boolean,
  val followsYou: Boolean,
  val relationshipState: String,
  val sortOrder: Int,
  val cachedAtMs: Long,
)

@Entity(
  tableName = "pending_feed_interactions",
  indices = [Index(value = ["nextRetryAtMs"])],
)
data class PendingFeedInteractionEntity(
  @PrimaryKey val clientOpId: String,
  val postId: String,
  val action: String,
  val watchMs: Long,
  val durationMs: Long,
  val reason: String,
  val details: String,
  val createdAtMs: Long,
  val retryCount: Int,
  val nextRetryAtMs: Long,
)

@Entity(tableName = "cached_settings")
data class CachedSettingsEntity(
  @PrimaryKey val userId: String,
  val privacyJson: String,
  val notificationsJson: String,
  val contentPrefsJson: String,
  val updatedAtMs: Long,
)

@Entity(
  tableName = "cached_restricted_accounts",
  indices = [Index(value = ["ownerUserId", "kind", "createdAtMs"])],
)
data class CachedRestrictedAccountEntity(
  @PrimaryKey val compositeKey: String, // "$ownerUserId|$kind|$targetUserId"
  val ownerUserId: String,
  val kind: String, // "blocked" or "muted"
  val targetUserId: String,
  val fullName: String,
  val username: String,
  val avatarUrl: String?,
  val avatarLocalPath: String?,
  val createdAtMs: Long,
)

@Entity(
  tableName = "support_tickets",
  indices = [Index(value = ["userId", "createdAtMs"])],
)
data class SupportTicketEntity(
  @PrimaryKey val ticketId: String,
  val userId: String,
  val referenceNumber: String,
  val category: String,
  val description: String,
  val screenshotLocalPath: String?,
  val screenshotSha256: String,
  val status: String,
  val resolutionNote: String,
  val isSynced: Boolean,
  val createdAtMs: Long,
)

@Entity(
  tableName = "pending_settings_ops",
  indices = [Index(value = ["nextRetryAtMs"])],
)
data class PendingSettingsOpEntity(
  @PrimaryKey val opId: String,
  val userId: String,
  val opKind: String, // "privacy", "notifications", "content_prefs", "chat_privacy", "chat_theme", "mute", "unmute", "block", "unblock", "report_problem"
  val payloadJson: String,
  val createdAtMs: Long,
  val retryCount: Int,
  val nextRetryAtMs: Long,
)

@Entity(tableName = "cached_chat_privacy")
data class CachedChatPrivacyEntity(
  @PrimaryKey val userId: String,
  val messageRequests: String,
  val whoCanCall: String,
  val whoCanScreenshot: String,
  val whoCanShareChats: String,
  val whoCanCopyMessages: String,
  val whoCanDownloadMedia: String,
  val chatThemeId: String,
  val bubbleColorId: String,
  val readReceipts: String,
  val typingIndicators: String,
  val onlineIndicators: String,
  val updatedAtMs: Long,
)

@Entity(tableName = "signal_key_states")
data class SignalKeyStateEntity(
  @PrimaryKey val ownerUserId: String,
  val serializedStoreJson: String,
  val bundleUploaded: Boolean,
  val updatedAtMs: Long,
)

@Entity(
  tableName = "chat_conversations",
  indices = [Index(value = ["peerUserId"]), Index(value = ["updatedAtMs"])],
)
data class ChatConversationEntity(
  @PrimaryKey val conversationId: String,
  val peerUserId: String,
  val peerFullName: String,
  val peerUsername: String,
  val peerAvatarUrl: String,
  val lastMessageId: String,
  val lastMessageType: String,
  val lastSenderId: String,
  val lastMessagePreviewPlain: String,
  val lastDeliveryStatus: String,
  val lastMessageAtMs: Long,
  val unreadCount: Int,
  val isArchived: Boolean,
  val isMuted: Boolean,
  val isDeletedLocally: Boolean,
  val isOnline: Boolean,
  val lastSeenMs: Long,
  val lastSeenLabel: String,
  val canCall: Boolean,
  val canScreenshot: Boolean,
  val canShareChats: Boolean,
  val canCopyMessages: Boolean,
  val canDownloadMedia: Boolean,
  val showReadReceipts: Boolean,
  val showTypingIndicator: Boolean,
  val showOnlineIndicator: Boolean,
  val isBlocked: Boolean,
  val draftText: String,
  val updatedAtMs: Long,
)

@Entity(
  tableName = "chat_messages",
  indices = [
    Index(value = ["conversationId", "createdAtMs"]),
    Index(value = ["clientOpId"]),
  ],
)
data class ChatMessageEntity(
  @PrimaryKey val messageId: String,
  val conversationId: String,
  val senderUserId: String,
  val recipientUserId: String,
  val clientOpId: String,
  val messageType: String, // text | sticker | gif | clip | voice | voice_once
  val plainText: String,
  val mediaUrl: String,
  val mediaLocalPath: String,
  val mediaId: String,
  val voiceDurationMs: Long,
  val viewOnceTtlSec: Int,
  val viewOnceOpenedAtMs: Long,
  val viewOnceExpiresAtMs: Long,
  val forwardedFromUserId: String,
  val deliveryStatus: String, // sending | sent | delivered | seen
  val syncState: String, // synced | pending | failed
  val editedAtMs: Long,
  val isPendingUnsend: Boolean,
  val createdAtMs: Long,
)

@Entity(
  tableName = "chat_security_flags",
  indices = [Index(value = ["conversationId", "createdAtMs"])],
)
data class ChatSecurityFlagEntity(
  @PrimaryKey val flagId: String,
  val conversationId: String,
  val messageId: String,
  val actorUserId: String,
  val targetUserId: String,
  val flagType: String, // screenshot_disallowed | view_once_opened | view_once_capture_attempt
  val details: String,
  val isSynced: Boolean,
  val createdAtMs: Long,
)

@Entity(
  tableName = "pending_chat_ops",
  indices = [Index(value = ["nextRetryAtMs"])],
)
data class PendingChatOpEntity(
  @PrimaryKey val opId: String,
  val opKind: String, // send | edit | unsend | delete_self | receipt | flag | consume_view_once | batch_conv
  val conversationId: String,
  val targetMessageId: String,
  val payloadJson: String,
  val createdAtMs: Long,
  val retryCount: Int,
  val nextRetryAtMs: Long,
)

@Entity(tableName = "cached_link_previews")
data class CachedLinkPreviewEntity(
  @PrimaryKey val normalizedUrl: String,
  val title: String,
  val description: String,
  val imageUrl: String,
  val faviconUrl: String,
  val domain: String,
  val status: String, // ok | fallback
  val fetchedAtMs: Long,
)

@Entity(
  tableName = "saved_links",
  indices = [
    Index(value = ["userId", "savedAtMs"]),
    Index(value = ["userId", "normalizedUrl"], unique = true),
  ],
)
data class SavedLinkEntity(
  @PrimaryKey val id: String,
  val userId: String,
  val url: String,
  val normalizedUrl: String,
  val title: String,
  val description: String,
  val imageUrl: String,
  val faviconUrl: String,
  val domain: String,
  val source: String, // chat | feed | profile | browser | search
  val savedAtMs: Long,
)

@Entity(
  tableName = "cached_notifications",
  indices = [
    Index(value = ["userId", "createdAtMs"]),
    Index(value = ["userId", "isRead"]),
    Index(value = ["conversationId"]),
  ],
)
data class CachedNotificationEntity(
  @PrimaryKey val id: String,
  val userId: String,
  val actorUserId: String,
  val actorName: String,
  val actorUsername: String,
  val actorAvatarUrl: String,
  val kind: String, // chat_message | follow | like | mention
  val title: String,
  val body: String,
  val conversationId: String,
  val messageId: String,
  val postId: String,
  val targetUserId: String,
  val isViewOnce: Boolean,
  val isRead: Boolean,
  val createdAtMs: Long,
)

