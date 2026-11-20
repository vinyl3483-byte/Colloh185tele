package telefam.company.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TelefamDao {
  // Rate limiting & escalating cooldowns
  @Query("SELECT * FROM rate_limit_state WHERE compositeKey = :compositeKey LIMIT 1")
  suspend fun getRateLimitState(compositeKey: String): RateLimitStateEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRateLimitState(state: RateLimitStateEntity)

  @Query("DELETE FROM rate_limit_state WHERE compositeKey = :compositeKey")
  suspend fun clearRateLimitState(compositeKey: String)

  @Query("DELETE FROM rate_limit_hits WHERE timestampMs < :cutoffMs")
  suspend fun pruneExpiredHits(cutoffMs: Long)

  @Query("SELECT COUNT(*) FROM rate_limit_hits WHERE dimensionKey = :dimensionKey AND timestampMs >= :sinceMs")
  suspend fun countHitsSince(dimensionKey: String, sinceMs: Long): Int

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertHit(hit: RateLimitHitEntity)

  // User Profiles & Unique Usernames
  @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
  fun observeProfile(userId: String): Flow<UserProfileEntity?>

  @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
  suspend fun getProfileByUserId(userId: String): UserProfileEntity?

  @Query("SELECT COUNT(*) FROM user_profiles WHERE usernameLower = :usernameLower AND userId != :currentUserId")
  suspend fun countOtherUsersWithUsername(usernameLower: String, currentUserId: String): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertProfile(profile: UserProfileEntity)

  // Media Uploads & Fingerprints
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMediaUpload(media: MediaUploadEntity)

  // Rotating Refresh Tokens & Reuse Detection
  @Query("SELECT * FROM refresh_token_records WHERE tokenHash = :tokenHash LIMIT 1")
  suspend fun getRefreshTokenRecord(tokenHash: String): RefreshTokenRecordEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRefreshTokenRecord(record: RefreshTokenRecordEntity)

  @Query("UPDATE refresh_token_records SET revoked = 1 WHERE tokenHash = :tokenHash")
  suspend fun revokeRefreshToken(tokenHash: String)

  @Query("UPDATE refresh_token_records SET revoked = 1 WHERE familyId = :familyId")
  suspend fun revokeTokenFamily(familyId: String)

  // Immutable Audit Logs
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertAuditLog(log: AuditLogEntity)

  // Family circles & offline-first messages
  @Query("SELECT * FROM family_circles ORDER BY lastTimestampMs DESC")
  fun observeCircles(): Flow<List<FamilyCircleEntity>>

  @Query("SELECT COUNT(*) FROM family_circles")
  suspend fun countCircles(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCircles(circles: List<FamilyCircleEntity>)

  @Query("UPDATE family_circles SET lastMessage = :lastMessage, lastTimestampMs = :timestampMs WHERE id = :circleId")
  suspend fun updateCircleLastMessage(circleId: String, lastMessage: String, timestampMs: Long)

  @Query("SELECT * FROM family_messages WHERE circleId = :circleId ORDER BY createdAtMs ASC")
  fun observeMessagesForCircle(circleId: String): Flow<List<FamilyMessageEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessage(message: FamilyMessageEntity)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertMessagesIfAbsent(messages: List<FamilyMessageEntity>)

  @Query("UPDATE family_messages SET syncStatus = :status WHERE id = :messageId")
  suspend fun updateMessageSyncStatus(messageId: String, status: String)

  // Offline action outbox
  @Query("SELECT * FROM offline_actions WHERE nextRetryAtMs <= :nowMs ORDER BY createdAtMs ASC LIMIT :limit")
  suspend fun getReadyOfflineActions(nowMs: Long, limit: Int): List<OfflineActionEntity>

  @Query("SELECT COUNT(*) FROM offline_actions")
  fun observePendingActionCount(): Flow<Int>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun enqueueOfflineAction(action: OfflineActionEntity)

  @Query("DELETE FROM offline_actions WHERE clientOpId IN (:opIds)")
  suspend fun deleteOfflineActions(opIds: List<String>)

  @Query("UPDATE offline_actions SET retryCount = :retryCount, nextRetryAtMs = :nextRetryAtMs WHERE clientOpId = :clientOpId")
  suspend fun scheduleActionRetry(clientOpId: String, retryCount: Int, nextRetryAtMs: Long)

  // Aggressively cached contacts & offline relationship sync queue
  @Query("SELECT * FROM cached_contacts ORDER BY LOWER(fullName) ASC, usernameLower ASC")
  fun observeAllCachedContacts(): Flow<List<CachedContactEntity>>

  @Query("SELECT * FROM cached_contacts WHERE userId = :userId LIMIT 1")
  suspend fun getCachedContact(userId: String): CachedContactEntity?

  @Query("SELECT * FROM cached_contacts ORDER BY LOWER(fullName) ASC, usernameLower ASC LIMIT :limit OFFSET :offset")
  suspend fun getCachedContactsPaged(limit: Int, offset: Int): List<CachedContactEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCachedContacts(contacts: List<CachedContactEntity>)

  @Query("UPDATE cached_contacts SET isFollowing = :isFollowing, followsYou = :followsYou, updatedAtMs = :updatedAtMs, cachedAtMs = :cachedAtMs WHERE userId = :userId")
  suspend fun updateCachedContactRelationship(
    userId: String,
    isFollowing: Boolean,
    followsYou: Boolean,
    updatedAtMs: Long,
    cachedAtMs: Long,
  )

  @Query("UPDATE cached_contacts SET avatarLocalPath = :localPath WHERE userId = :userId")
  suspend fun updateCachedContactAvatarPath(userId: String, localPath: String)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun enqueueContactSyncAction(action: ContactSyncActionEntity)

  @Query("SELECT * FROM contact_sync_actions")
  suspend fun getAllPendingContactSyncActions(): List<ContactSyncActionEntity>

  @Query("SELECT * FROM contact_sync_actions WHERE nextRetryAtMs <= :nowMs ORDER BY createdAtMs ASC LIMIT :limit")
  suspend fun getReadyContactSyncActions(nowMs: Long, limit: Int): List<ContactSyncActionEntity>

  @Query("DELETE FROM contact_sync_actions WHERE targetUserId = :targetUserId")
  suspend fun deletePendingContactSyncForTarget(targetUserId: String)

  @Query("DELETE FROM contact_sync_actions WHERE clientOpId IN (:opIds)")
  suspend fun deleteContactSyncActions(opIds: List<String>)

  @Query("UPDATE contact_sync_actions SET retryCount = :retryCount, nextRetryAtMs = :nextRetryAtMs WHERE clientOpId = :clientOpId")
  suspend fun scheduleContactSyncRetry(clientOpId: String, retryCount: Int, nextRetryAtMs: Long)

  // Hashtag cache with real usage counts
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCachedHashtags(tags: List<CachedHashtagEntity>)

  @Query("SELECT * FROM cached_hashtags WHERE :query = '' OR tag LIKE '%' || :query || '%' ORDER BY postCount DESC, tag ASC LIMIT 30")
  suspend fun searchCachedHashtags(query: String): List<CachedHashtagEntity>

  @Query("UPDATE cached_hashtags SET postCount = postCount + 1, updatedAtMs = :nowMs WHERE tag = :tag")
  suspend fun incrementCachedHashtagCount(tag: String, nowMs: Long)

  // Offline post upload queue & auto-retry tracking
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPendingPostUpload(post: PendingPostUploadEntity)

  @Query("SELECT * FROM pending_post_uploads ORDER BY createdAtMs DESC")
  fun observePendingPostUploads(): Flow<List<PendingPostUploadEntity>>

  @Query("SELECT * FROM pending_post_uploads WHERE nextRetryAtMs <= :nowMs AND status != 'UPLOADED' ORDER BY createdAtMs ASC LIMIT :limit")
  suspend fun getReadyPendingPostUploads(nowMs: Long, limit: Int): List<PendingPostUploadEntity>

  @Query("UPDATE pending_post_uploads SET status = :status, progressPct = :progressPct WHERE clientOpId = :clientOpId")
  suspend fun updatePendingPostUploadProgress(clientOpId: String, status: String, progressPct: Int)

  @Query("UPDATE pending_post_uploads SET status = :status, retryCount = :retryCount, nextRetryAtMs = :nextRetryAtMs WHERE clientOpId = :clientOpId")
  suspend fun schedulePendingPostRetry(clientOpId: String, status: String, retryCount: Int, nextRetryAtMs: Long)

  @Query("DELETE FROM pending_post_uploads WHERE clientOpId = :clientOpId")
  suspend fun deletePendingPostUpload(clientOpId: String)

  // Aggressively cached video feeds & offline interaction queue
  @Query("SELECT * FROM cached_feed_posts WHERE isHiddenByUser = 0 AND (:tabKey = 'all' OR tabKey = :tabKey) ORDER BY sortOrder ASC, createdAtMs DESC")
  fun observeFeedPosts(tabKey: String): Flow<List<CachedFeedPostEntity>>

  @Query("SELECT * FROM cached_feed_posts WHERE isHiddenByUser = 0 ORDER BY relevanceScore DESC, createdAtMs DESC")
  fun observeAllVisibleFeedPosts(): Flow<List<CachedFeedPostEntity>>

  @Query("SELECT * FROM cached_feed_posts WHERE postId = :postId LIMIT 1")
  suspend fun getFeedPost(postId: String): CachedFeedPostEntity?

  @Query("SELECT * FROM cached_feed_posts WHERE authorUserId = :authorUserId AND isHiddenByUser = 0 ORDER BY createdAtMs DESC")
  suspend fun getFeedPostsByAuthor(authorUserId: String): List<CachedFeedPostEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertFeedPosts(posts: List<CachedFeedPostEntity>)

  @Query("DELETE FROM cached_feed_posts WHERE postId = :postId")
  suspend fun deleteFeedPost(postId: String)

  @Query("UPDATE cached_feed_posts SET isHiddenByUser = 1 WHERE postId = :postId")
  suspend fun hideFeedPost(postId: String)

  @Query("UPDATE cached_feed_posts SET isHiddenByUser = 1 WHERE authorUserId = :authorUserId")
  suspend fun hideFeedPostsByAuthor(authorUserId: String)

  @Query("UPDATE cached_feed_posts SET isFollowingAuthor = :isFollowing WHERE authorUserId = :authorUserId")
  suspend fun updateFeedAuthorFollowing(authorUserId: String, isFollowing: Boolean)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun enqueueFeedInteraction(item: PendingFeedInteractionEntity)

  @Query("SELECT * FROM pending_feed_interactions WHERE nextRetryAtMs <= :nowMs ORDER BY createdAtMs ASC LIMIT :limit")
  suspend fun getReadyFeedInteractions(nowMs: Long, limit: Int): List<PendingFeedInteractionEntity>

  @Query("DELETE FROM pending_feed_interactions WHERE clientOpId IN (:opIds)")
  suspend fun deleteFeedInteractions(opIds: List<String>)

  @Query("UPDATE pending_feed_interactions SET retryCount = :retryCount, nextRetryAtMs = :nextRetryAtMs WHERE clientOpId = :clientOpId")
  suspend fun scheduleFeedInteractionRetry(clientOpId: String, retryCount: Int, nextRetryAtMs: Long)

  @Query("UPDATE cached_feed_posts SET viewsCount = viewsCount + 1 WHERE postId = :postId")
  suspend fun incrementCachedFeedPostView(postId: String)

  @Query("UPDATE cached_feed_posts SET privacy = :privacy WHERE postId = :postId")
  suspend fun updateCachedFeedPostPrivacy(postId: String, privacy: String)

  // Cached Full User Profiles & Followers/Following Lists (offline-first fallback)
  @Query("SELECT * FROM cached_user_profiles WHERE userId = :userId LIMIT 1")
  suspend fun getCachedUserProfile(userId: String): CachedUserProfileEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCachedUserProfile(profile: CachedUserProfileEntity)

  @Query("UPDATE cached_user_profiles SET avatarLocalPath = :localPath WHERE userId = :userId")
  suspend fun updateCachedUserProfileAvatarPath(userId: String, localPath: String)

  @Query(
    "UPDATE cached_user_profiles SET isFollowing = :isFollowing, followersCount = :followersCount, relationshipState = :relationshipState WHERE userId = :userId",
  )
  suspend fun updateCachedUserProfileFollow(
    userId: String,
    isFollowing: Boolean,
    followersCount: Long,
    relationshipState: String,
  )

  @Query("UPDATE cached_user_profiles SET isBlocked = :isBlocked, isFollowing = :isFollowing, relationshipState = :relationshipState WHERE userId = :userId")
  suspend fun updateCachedUserProfileBlocked(
    userId: String,
    isBlocked: Boolean,
    isFollowing: Boolean,
    relationshipState: String,
  )

  @Query("SELECT * FROM cached_user_connections WHERE ownerUserId = :ownerUserId AND listType = :listType ORDER BY sortOrder ASC")
  suspend fun getCachedUserConnections(ownerUserId: String, listType: String): List<CachedUserConnectionEntity>

  @Query("DELETE FROM cached_user_connections WHERE ownerUserId = :ownerUserId AND listType = :listType")
  suspend fun deleteCachedUserConnectionsFor(ownerUserId: String, listType: String)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCachedUserConnections(items: List<CachedUserConnectionEntity>)

  @Query("UPDATE cached_user_connections SET isFollowing = :isFollowing, relationshipState = :relationshipState WHERE targetUserId = :targetUserId")
  suspend fun updateCachedUserConnectionFollow(targetUserId: String, isFollowing: Boolean, relationshipState: String)

  // Settings, Blocked & Muted Accounts, Support Tickets & Offline Settings Sync Outbox
  @Query("SELECT * FROM cached_settings WHERE userId = :userId LIMIT 1")
  suspend fun getCachedSettings(userId: String): CachedSettingsEntity?

  @Query("SELECT * FROM cached_settings WHERE userId = :userId LIMIT 1")
  fun observeCachedSettings(userId: String): Flow<CachedSettingsEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCachedSettings(entity: CachedSettingsEntity)

  @Query("SELECT * FROM cached_restricted_accounts WHERE ownerUserId = :ownerUserId AND kind = :kind ORDER BY createdAtMs DESC")
  suspend fun getRestrictedAccounts(ownerUserId: String, kind: String): List<CachedRestrictedAccountEntity>

  @Query("SELECT targetUserId FROM cached_restricted_accounts WHERE ownerUserId = :ownerUserId AND kind = :kind")
  suspend fun getRestrictedAccountIds(ownerUserId: String, kind: String): List<String>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRestrictedAccount(account: CachedRestrictedAccountEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRestrictedAccounts(accounts: List<CachedRestrictedAccountEntity>)

  @Query("DELETE FROM cached_restricted_accounts WHERE ownerUserId = :ownerUserId AND kind = :kind AND targetUserId = :targetUserId")
  suspend fun deleteRestrictedAccount(ownerUserId: String, kind: String, targetUserId: String)

  @Query("DELETE FROM cached_restricted_accounts WHERE ownerUserId = :ownerUserId AND kind = :kind")
  suspend fun clearRestrictedAccountsByKind(ownerUserId: String, kind: String)

  @Query("SELECT * FROM support_tickets WHERE userId = :userId ORDER BY createdAtMs DESC")
  suspend fun getSupportTickets(userId: String): List<SupportTicketEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSupportTicket(ticket: SupportTicketEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSupportTickets(tickets: List<SupportTicketEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun enqueueSettingsOp(op: PendingSettingsOpEntity)

  @Query("SELECT * FROM pending_settings_ops WHERE userId = :userId AND nextRetryAtMs <= :nowMs ORDER BY createdAtMs ASC LIMIT :limit")
  suspend fun getReadySettingsOps(userId: String, nowMs: Long, limit: Int): List<PendingSettingsOpEntity>

  @Query("DELETE FROM pending_settings_ops WHERE opId = :opId")
  suspend fun deleteSettingsOp(opId: String)

  @Query("UPDATE pending_settings_ops SET retryCount = :retryCount, nextRetryAtMs = :nextRetryAtMs WHERE opId = :opId")
  suspend fun scheduleSettingsOpRetry(opId: String, retryCount: Int, nextRetryAtMs: Long)

  // Chat Privacy, Themes & Bubble Colour local cache
  @Query("SELECT * FROM cached_chat_privacy WHERE userId = :userId LIMIT 1")
  suspend fun getCachedChatPrivacy(userId: String): CachedChatPrivacyEntity?

  @Query("SELECT * FROM cached_chat_privacy WHERE userId = :userId LIMIT 1")
  fun observeCachedChatPrivacy(userId: String): Flow<CachedChatPrivacyEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCachedChatPrivacy(entity: CachedChatPrivacyEntity)

  // Live outbox counts for Settings -> Offline sync
  @Query("SELECT COUNT(*) FROM pending_settings_ops WHERE userId = :userId")
  suspend fun countPendingSettingsOps(userId: String): Int

  @Query("SELECT COUNT(*) FROM contact_sync_actions")
  suspend fun countPendingContactSyncActions(): Int

  @Query("SELECT COUNT(*) FROM pending_feed_interactions")
  suspend fun countPendingFeedInteractions(): Int

  @Query("SELECT COUNT(*) FROM pending_post_uploads")
  suspend fun countPendingPostUploads(): Int

  @Query("SELECT COUNT(*) FROM offline_actions")
  suspend fun countPendingOfflineActions(): Int

  @Query("SELECT COUNT(*) FROM pending_chat_ops")
  suspend fun countPendingChatOps(): Int

  // Signal Protocol Key State
  @Query("SELECT * FROM signal_key_states WHERE ownerUserId = :ownerUserId LIMIT 1")
  suspend fun getSignalKeyState(ownerUserId: String): SignalKeyStateEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSignalKeyState(state: SignalKeyStateEntity)

  // 1-on-1 Conversations (reactive list + long-press actions + search)
  @Query("SELECT * FROM chat_conversations WHERE isDeletedLocally = 0 AND isArchived = 0 ORDER BY lastMessageAtMs DESC, updatedAtMs DESC")
  fun observeActiveConversations(): Flow<List<ChatConversationEntity>>

  @Query("SELECT * FROM chat_conversations WHERE isDeletedLocally = 0 ORDER BY lastMessageAtMs DESC, updatedAtMs DESC")
  suspend fun getAllActiveConversations(): List<ChatConversationEntity>

  @Query("SELECT * FROM chat_conversations WHERE conversationId = :conversationId LIMIT 1")
  suspend fun getConversation(conversationId: String): ChatConversationEntity?

  @Query("SELECT * FROM chat_conversations WHERE conversationId = :conversationId LIMIT 1")
  fun observeConversation(conversationId: String): Flow<ChatConversationEntity?>

  @Query("SELECT * FROM chat_conversations WHERE peerUserId = :peerUserId AND isDeletedLocally = 0 LIMIT 1")
  suspend fun getConversationByPeer(peerUserId: String): ChatConversationEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertConversation(conv: ChatConversationEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertConversations(convs: List<ChatConversationEntity>)

  @Query("UPDATE chat_conversations SET unreadCount = 0 WHERE conversationId = :conversationId")
  suspend fun clearConversationUnread(conversationId: String)

  @Query("UPDATE chat_conversations SET unreadCount = :unreadCount WHERE conversationId = :conversationId")
  suspend fun setConversationUnread(conversationId: String, unreadCount: Int)

  @Query("UPDATE chat_conversations SET isArchived = :isArchived WHERE conversationId IN (:ids)")
  suspend fun setConversationsArchived(ids: List<String>, isArchived: Boolean)

  @Query("UPDATE chat_conversations SET isMuted = :isMuted WHERE conversationId IN (:ids)")
  suspend fun setConversationsMuted(ids: List<String>, isMuted: Boolean)

  @Query("UPDATE chat_conversations SET isDeletedLocally = :deleted WHERE conversationId IN (:ids)")
  suspend fun setConversationsDeletedLocally(ids: List<String>, deleted: Boolean)

  @Query("UPDATE chat_conversations SET draftText = :draftText WHERE conversationId = :conversationId")
  suspend fun updateConversationDraft(conversationId: String, draftText: String)

  // 1-on-1 Messages (instant offline-first write, reactive thread list, search, zero-trace delete)
  @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId AND isPendingUnsend = 0 ORDER BY createdAtMs ASC")
  fun observeMessagesForConversation(conversationId: String): Flow<List<ChatMessageEntity>>

  @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY createdAtMs ASC")
  suspend fun getMessagesForConversation(conversationId: String): List<ChatMessageEntity>

  @Query("SELECT * FROM chat_messages WHERE messageId = :messageId LIMIT 1")
  suspend fun getChatMessage(messageId: String): ChatMessageEntity?

  @Query("SELECT * FROM chat_messages WHERE plainText LIKE '%' || :query || '%' AND isPendingUnsend = 0 ORDER BY createdAtMs DESC LIMIT 40")
  suspend fun searchDecryptedMessages(query: String): List<ChatMessageEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertChatMessage(msg: ChatMessageEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertChatMessages(msgs: List<ChatMessageEntity>)

  @Query("DELETE FROM chat_messages WHERE messageId = :messageId")
  suspend fun deleteChatMessagePermanently(messageId: String)

  @Query("DELETE FROM chat_messages WHERE clientOpId = :clientOpId")
  suspend fun deleteChatMessageByClientOpId(clientOpId: String)

  @Query("UPDATE chat_messages SET deliveryStatus = :status WHERE messageId = :messageId")
  suspend fun updateChatMessageDeliveryStatus(messageId: String, status: String)

  @Query("UPDATE chat_messages SET mediaLocalPath = :localPath WHERE messageId = :messageId")
  suspend fun updateChatMessageMediaLocalPath(messageId: String, localPath: String)

  @Query("UPDATE chat_messages SET conversationId = :conversationId WHERE messageId = :messageId OR clientOpId = :messageId")
  suspend fun updateChatMessageConversationId(messageId: String, conversationId: String)

  @Query("UPDATE chat_messages SET plainText = :newText, editedAtMs = :editedAtMs WHERE messageId = :messageId")
  suspend fun updateChatMessageEditedText(messageId: String, newText: String, editedAtMs: Long)

  @Query("UPDATE chat_messages SET viewOnceOpenedAtMs = :openedAtMs, viewOnceExpiresAtMs = :expiresAtMs WHERE messageId = :messageId")
  suspend fun markChatMessageViewOnceOpened(messageId: String, openedAtMs: Long, expiresAtMs: Long)

  // Persistent Security Flags (survive offline restart & refresh)
  @Query("SELECT * FROM chat_security_flags WHERE conversationId = :conversationId ORDER BY createdAtMs DESC")
  fun observeSecurityFlags(conversationId: String): Flow<List<ChatSecurityFlagEntity>>

  @Query("SELECT * FROM chat_security_flags WHERE isSynced = 0 ORDER BY createdAtMs ASC")
  suspend fun getUnsyncedSecurityFlags(): List<ChatSecurityFlagEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSecurityFlag(flag: ChatSecurityFlagEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSecurityFlags(flags: List<ChatSecurityFlagEntity>)

  @Query("UPDATE chat_security_flags SET isSynced = 1 WHERE flagId = :flagId")
  suspend fun markSecurityFlagSynced(flagId: String)

  // Offline Chat Outbox Operations
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun enqueueChatOp(op: PendingChatOpEntity)

  @Query("SELECT * FROM pending_chat_ops WHERE nextRetryAtMs <= :nowMs ORDER BY createdAtMs ASC LIMIT :limit")
  suspend fun getReadyChatOps(nowMs: Long, limit: Int): List<PendingChatOpEntity>

  @Query("DELETE FROM pending_chat_ops WHERE opId = :opId")
  suspend fun deleteChatOp(opId: String)

  @Query("UPDATE pending_chat_ops SET retryCount = :retryCount, nextRetryAtMs = :nextRetryAtMs WHERE opId = :opId")
  suspend fun scheduleChatOpRetry(opId: String, retryCount: Int, nextRetryAtMs: Long)

  // Link Previews & Saved Links
  @Query("SELECT * FROM cached_link_previews WHERE normalizedUrl = :normalizedUrl LIMIT 1")
  suspend fun getCachedLinkPreview(normalizedUrl: String): CachedLinkPreviewEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCachedLinkPreview(preview: CachedLinkPreviewEntity)

  @Query(
    "SELECT * FROM saved_links WHERE userId = :userId AND (:query = '' OR LOWER(title) LIKE '%' || LOWER(:query) || '%' OR LOWER(domain) LIKE '%' || LOWER(:query) || '%' OR LOWER(url) LIKE '%' || LOWER(:query) || '%') ORDER BY savedAtMs DESC",
  )
  fun observeSavedLinks(userId: String, query: String = ""): Flow<List<SavedLinkEntity>>

  @Query("SELECT * FROM saved_links WHERE userId = :userId ORDER BY savedAtMs DESC")
  suspend fun getSavedLinksSnapshot(userId: String): List<SavedLinkEntity>

  @Query("SELECT * FROM saved_links WHERE userId = :userId AND normalizedUrl = :normalizedUrl LIMIT 1")
  suspend fun getSavedLinkByUrl(userId: String, normalizedUrl: String): SavedLinkEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSavedLink(item: SavedLinkEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSavedLinks(items: List<SavedLinkEntity>)

  @Query("DELETE FROM saved_links WHERE userId = :userId AND id = :id")
  suspend fun deleteSavedLinkById(userId: String, id: String)

  @Query("DELETE FROM saved_links WHERE userId = :userId AND normalizedUrl = :normalizedUrl")
  suspend fun deleteSavedLinkByUrl(userId: String, normalizedUrl: String)

  // Push & Social Notification Inbox
  @Query("SELECT * FROM cached_notifications WHERE userId = :userId ORDER BY createdAtMs DESC LIMIT 120")
  fun observeNotifications(userId: String): Flow<List<CachedNotificationEntity>>

  @Query("SELECT COUNT(*) FROM cached_notifications WHERE userId = :userId AND isRead = 0")
  fun observeUnreadNotificationCount(userId: String): Flow<Int>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertNotifications(items: List<CachedNotificationEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertNotification(item: CachedNotificationEntity)

  @Query("UPDATE cached_notifications SET isRead = 1 WHERE userId = :userId AND id = :notificationId")
  suspend fun markNotificationRead(userId: String, notificationId: String)

  @Query("UPDATE cached_notifications SET isRead = 1 WHERE userId = :userId AND conversationId = :conversationId")
  suspend fun markConversationNotificationsRead(userId: String, conversationId: String)

  @Query("UPDATE cached_notifications SET isRead = 1 WHERE userId = :userId")
  suspend fun markAllNotificationsRead(userId: String)

  @Query("DELETE FROM cached_notifications WHERE userId = :userId AND messageId = :messageId")
  suspend fun deleteNotificationsByMessageId(userId: String, messageId: String)

  // Complete session wipe on logout to guarantee cross-account isolation
  @Query("DELETE FROM user_profiles")
  suspend fun clearAllUserProfiles()

  @Query("DELETE FROM media_uploads")
  suspend fun clearAllMediaUploads()

  @Query("DELETE FROM refresh_token_records")
  suspend fun clearAllRefreshTokenRecords()

  @Query("DELETE FROM family_circles")
  suspend fun clearAllFamilyCircles()

  @Query("DELETE FROM family_messages")
  suspend fun clearAllFamilyMessages()

  @Query("DELETE FROM offline_actions")
  suspend fun clearAllOfflineActions()

  @Query("DELETE FROM cached_contacts")
  suspend fun clearAllCachedContacts()

  @Query("DELETE FROM contact_sync_actions")
  suspend fun clearAllContactSyncActions()

  @Query("DELETE FROM cached_hashtags")
  suspend fun clearAllCachedHashtags()

  @Query("DELETE FROM pending_post_uploads")
  suspend fun clearAllPendingPostUploads()

  @Query("DELETE FROM cached_feed_posts")
  suspend fun clearAllCachedFeedPosts()

  @Query("DELETE FROM pending_feed_interactions")
  suspend fun clearAllPendingFeedInteractions()

  @Query("DELETE FROM cached_user_profiles")
  suspend fun clearAllCachedUserProfiles()

  @Query("DELETE FROM cached_user_connections")
  suspend fun clearAllCachedUserConnections()

  @Query("DELETE FROM cached_settings")
  suspend fun clearAllCachedSettings()

  @Query("DELETE FROM cached_restricted_accounts")
  suspend fun clearAllCachedRestrictedAccounts()

  @Query("DELETE FROM support_tickets")
  suspend fun clearAllSupportTickets()

  @Query("DELETE FROM pending_settings_ops")
  suspend fun clearAllPendingSettingsOps()

  @Query("DELETE FROM cached_chat_privacy")
  suspend fun clearAllCachedChatPrivacy()

  @Query("DELETE FROM signal_key_states")
  suspend fun clearAllSignalKeyStates()

  @Query("DELETE FROM chat_conversations")
  suspend fun clearAllChatConversations()

  @Query("DELETE FROM chat_messages")
  suspend fun clearAllChatMessages()

  @Query("DELETE FROM chat_security_flags")
  suspend fun clearAllChatSecurityFlags()

  @Query("DELETE FROM pending_chat_ops")
  suspend fun clearAllPendingChatOps()

  @Query("DELETE FROM saved_links")
  suspend fun clearAllSavedLinks()

  @Query("DELETE FROM cached_notifications")
  suspend fun clearAllNotifications()

  @Transaction
  suspend fun clearAllUserScopedData() {
    clearAllUserProfiles()
    clearAllMediaUploads()
    clearAllRefreshTokenRecords()
    clearAllFamilyCircles()
    clearAllFamilyMessages()
    clearAllOfflineActions()
    clearAllCachedContacts()
    clearAllContactSyncActions()
    clearAllCachedHashtags()
    clearAllPendingPostUploads()
    clearAllCachedFeedPosts()
    clearAllPendingFeedInteractions()
    clearAllCachedUserProfiles()
    clearAllCachedUserConnections()
    clearAllCachedSettings()
    clearAllCachedRestrictedAccounts()
    clearAllSupportTickets()
    clearAllPendingSettingsOps()
    clearAllCachedChatPrivacy()
    clearAllSignalKeyStates()
    clearAllChatConversations()
    clearAllChatMessages()
    clearAllChatSecurityFlags()
    clearAllPendingChatOps()
    clearAllSavedLinks()
    clearAllNotifications()
  }
}

