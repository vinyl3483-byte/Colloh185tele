package telefam.company.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.telefam.company.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import telefam.company.core.AdaptiveStreamSelector
import telefam.company.core.AppLanguageOption
import telefam.company.core.AppVersionCheckResult
import telefam.company.core.AudienceOption
import telefam.company.core.ChatAudienceOption
import telefam.company.core.ChatPrivacySettingsState
import telefam.company.core.ContactFilterTab
import telefam.company.core.ContentPreferencesState
import telefam.company.core.DiscoverabilityOption
import telefam.company.core.FaqEntry
import telefam.company.core.FeedInteractionAction
import telefam.company.core.FeedSearchShortcut
import telefam.company.core.FeedTab
import telefam.company.core.FeedVideoRendition
import telefam.company.core.HashtagItem
import telefam.company.core.LegalDocumentItem
import telefam.company.core.NetworkTier
import telefam.company.core.NotificationSettingsState
import telefam.company.core.OfflineSyncStatusSnapshot
import telefam.company.core.PostMode
import telefam.company.core.PostPrivacy
import telefam.company.core.PostSongTrack
import telefam.company.core.PrivacySettingsState
import telefam.company.core.ProfileBackgroundItem
import telefam.company.core.RestrictedAccountItem
import telefam.company.core.SettingsSnapshot
import telefam.company.core.SmartTextDetector
import telefam.company.core.SupportTicketItem
import telefam.company.core.SyncPreferencesState
import telefam.company.core.SystemServiceStatusItem
import telefam.company.core.TelefamChatPrivacyCatalog
import telefam.company.core.TelefamContactsRules
import telefam.company.core.TelefamCoreEngine
import telefam.company.core.TelefamPostRules
import telefam.company.core.TelefamProfileSanitizer
import telefam.company.core.TelefamSettingsContentCatalog
import telefam.company.core.VideoQualityOption
import telefam.company.core.VisibilityOption
import telefam.company.data.local.AuditLogEntity
import telefam.company.data.local.CachedChatPrivacyEntity
import telefam.company.data.local.CachedContactEntity
import telefam.company.data.local.CachedFeedPostEntity
import telefam.company.data.local.CachedHashtagEntity
import telefam.company.data.local.CachedLinkPreviewEntity
import telefam.company.data.local.CachedNotificationEntity
import telefam.company.data.local.CachedRestrictedAccountEntity
import telefam.company.data.local.CachedSettingsEntity
import telefam.company.data.local.CachedUserConnectionEntity
import telefam.company.data.local.CachedUserProfileEntity
import telefam.company.data.local.ContactSyncActionEntity
import telefam.company.data.local.DeviceIdentityStore
import telefam.company.data.local.FamilyCircleEntity
import telefam.company.data.local.FamilyMessageEntity
import telefam.company.data.local.MediaUploadEntity
import telefam.company.data.local.OfflineActionEntity
import telefam.company.data.local.PendingFeedInteractionEntity
import telefam.company.data.local.PendingPostUploadEntity
import telefam.company.data.local.PendingSettingsOpEntity
import telefam.company.data.local.RateLimitHitEntity
import telefam.company.data.local.RateLimitStateEntity
import telefam.company.data.local.RefreshTokenRecordEntity
import telefam.company.data.local.SavedLinkEntity
import telefam.company.data.local.StoredSession
import telefam.company.data.local.SupportTicketEntity
import telefam.company.data.local.TelefamDao
import telefam.company.data.local.UserProfileEntity
import telefam.company.data.network.AuditEventDto
import telefam.company.data.network.ChatPrivacySettingsDto
import telefam.company.data.network.ContactActionDto
import telefam.company.data.network.ContactItemDto
import telefam.company.data.network.ContactSyncActionDto
import telefam.company.data.network.ContactSyncBatchRequestDto
import telefam.company.data.network.ContentPreferencesDto
import telefam.company.data.network.CreatePostRequestDto
import telefam.company.data.network.DeviceContextTelemetryDto
import telefam.company.data.network.EmailCheckResponseDto
import telefam.company.data.network.FeedInteractionBatchRequestDto
import telefam.company.data.network.FeedInteractionRequestDto
import telefam.company.data.network.FeedItemDto
import telefam.company.data.network.FeedVideoRenditionDto
import telefam.company.data.network.LogoutRequestDto
import telefam.company.data.network.ManualSyncRequestDto
import telefam.company.data.network.MarkNotificationReadRequestDto
import telefam.company.data.network.NetworkQualityMonitor
import telefam.company.data.network.NotificationSettingsDto
import telefam.company.data.network.OAuthDto
import telefam.company.data.network.PendingLoginRequestItemDto
import telefam.company.data.network.PrivacySettingsDto
import telefam.company.data.network.ProblemReportRequestDto
import telefam.company.data.network.ProfileBackgroundItemDto
import telefam.company.data.network.ProfileBackgroundReorderDto
import telefam.company.data.network.ProfileSetupDto
import telefam.company.data.network.ProfileUpdateRequestDto
import telefam.company.data.network.ProgressFileRequestBody
import telefam.company.data.network.RefreshTokenDto
import telefam.company.data.network.RegisterPushTokenRequestDto
import telefam.company.data.network.RequestOtpDto
import telefam.company.data.network.RespondLoginRequestDto
import telefam.company.data.network.SaveLinkRequestDto
import telefam.company.data.network.SongTrackDto
import telefam.company.data.network.SyncBatchRequestDto
import telefam.company.data.network.SyncOperationDto
import telefam.company.data.network.SyncPreferencesDto
import telefam.company.data.network.TelefamApiService
import telefam.company.data.network.UpdateChatThemeRequestDto
import telefam.company.data.network.UpdatePostRequestDto
import telefam.company.data.network.UserProfileResponseDto
import telefam.company.data.network.UserReportRequestDto
import telefam.company.data.network.VerifyLinkDto
import telefam.company.data.network.VerifyOtpDto
import telefam.company.media.MediaUploadProcessor
import telefam.company.media.ProcessedAvatarMedia
import telefam.company.media.ProcessedPostPhoto
import telefam.company.media.VideoPrepareResult
import telefam.company.notifications.TelefamNotificationManager
import telefam.company.security.DeviceContextCollector
import telefam.company.security.DeviceContextSnapshot
import telefam.company.security.EncryptedTokenVault
import telefam.company.security.SecurityIntegrityGuard
import telefam.company.ui.theme.ThemeMode
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed class AuthActionResult {
  data class OtpSent(
    val email: String,
    val cooldownSeconds: Long,
    val totalCooldownDurationSeconds: Long,
  ) : AuthActionResult()

  data class RequiresDeviceApproval(
    val email: String,
    val requestId: String,
    val pollToken: String,
    val targetDeviceName: String,
    val targetLocation: String,
    val requestTimeMs: Long,
    val expiresInSeconds: Long,
    val tryAnotherWayProvider: String,
  ) : AuthActionResult()

  data class Verified(
    val userId: String,
    val email: String,
    val profileCompleted: Boolean,
  ) : AuthActionResult()

  data class SanitizedFailure(
    val message: String,
    val remainingCooldownSeconds: Long = 0L,
    val isDeviceBlocked: Boolean = false,
  ) : AuthActionResult()
}

sealed class LoginApprovalPollOutcome {
  data class Pending(
    val remainingSeconds: Long,
    val targetDeviceName: String,
    val targetLocation: String,
  ) : LoginApprovalPollOutcome()

  data class Approved(
    val verified: AuthActionResult.Verified,
  ) : LoginApprovalPollOutcome()

  data object DeniedBlocked : LoginApprovalPollOutcome()
  data object Expired : LoginApprovalPollOutcome()
  data class Error(val message: String) : LoginApprovalPollOutcome()
}

data class RespondLoginApprovalResult(
  val success: Boolean,
  val status: String = "",
  val deviceName: String = "",
  val location: String = "",
  val requestedAtMs: Long = 0L,
  val message: String? = null,
)

sealed class ProfileActionResult {
  data object Saved : ProfileActionResult()
  data class Error(val message: String, val nextUsernameChangeMs: Long = 0L) : ProfileActionResult()
}

sealed class ProfileBackgroundResult {
  data class Updated(val backgrounds: List<ProfileBackgroundItemDto>) : ProfileBackgroundResult()
  data class Error(val message: String) : ProfileBackgroundResult()
}

data class ContactsPageFetchResult(
  val page: Int,
  val hasMore: Boolean,
  val fromCache: Boolean,
  val pageItems: List<CachedContactEntity>,
)

sealed class ContactToggleResult {
  data object Synced : ContactToggleResult()
  data object QueuedOffline : ContactToggleResult()
  data class RateLimited(val message: String = TelefamContactsRules.RATE_LIMIT_TOAST_MESSAGE) :
    ContactToggleResult()
}

data class PostSubmissionInput(
  val mode: PostMode,
  val textContent: String,
  val caption: String,
  val themeId: String,
  val fontId: String,
  val song: PostSongTrack?,
  val taggedUserIds: List<String>,
  val hashtags: List<String>,
  val localPhotoPaths: List<String>,
  val localVideoPath: String?,
  val localThumbnailPath: String?,
  val frameHashes: List<String>,
  val durationMs: Long,
  val trimStartMs: Long,
  val trimEndMs: Long,
  val width: Int,
  val height: Int,
  val muteOriginalAudio: Boolean,
  val contentId: String,
)

sealed class PostPublishResult {
  data class Uploaded(val postId: String, val contentId: String) : PostPublishResult()
  data class QueuedOffline(val clientOpId: String) : PostPublishResult()
  data class Rejected(val message: String) : PostPublishResult()
}

data class FeedPageFetchResult(
  val page: Int,
  val hasMore: Boolean,
  val fromCache: Boolean,
  val items: List<CachedFeedPostEntity>,
)

data class FeedSearchFetchResult(
  val page: Int,
  val hasMore: Boolean,
  val shortcuts: List<FeedSearchShortcut>,
  val items: List<CachedFeedPostEntity>,
)

data class UserConnectionsPageResult(
  val items: List<ContactItemDto>,
  val page: Int,
  val hasMore: Boolean,
)

class TelefamRepository(
  private val appContext: Context,
  private val dao: TelefamDao,
  private val identityStore: DeviceIdentityStore,
  private val networkMonitor: NetworkQualityMonitor,
  private val apiService: TelefamApiService? = null,
) {
  private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val stringListAdapter = moshi.adapter<List<String>>(
    Types.newParameterizedType(List::class.java, String::class.java),
  )
  private val renditionsListAdapter = moshi.adapter<List<FeedVideoRenditionDto>>(
    Types.newParameterizedType(List::class.java, FeedVideoRenditionDto::class.java),
  )
  private val feedItemListAdapter = moshi.adapter<List<FeedItemDto>>(
    Types.newParameterizedType(List::class.java, FeedItemDto::class.java),
  )
  private val profileBgListAdapter = moshi.adapter<List<ProfileBackgroundItemDto>>(
    Types.newParameterizedType(List::class.java, ProfileBackgroundItemDto::class.java),
  )
  private val deviceContextAdapter = moshi.adapter(DeviceContextSnapshot::class.java)
  private val songAdapter = moshi.adapter(SongTrackDto::class.java)
  private val telemetryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  val networkTier: StateFlow<NetworkTier> = networkMonitor.networkTier
  val sessionFlow: Flow<StoredSession> = identityStore.sessionFlow
  val circlesFlow: Flow<List<FamilyCircleEntity>> = dao.observeCircles()
  val pendingOfflineActionsCount: Flow<Int> = dao.observePendingActionCount()
  val pendingPostUploadsFlow: Flow<List<PendingPostUploadEntity>> = dao.observePendingPostUploads()

  val chatRepository: TelefamChatRepository by lazy {
    TelefamChatRepository(
      appContext = appContext,
      dao = dao,
      identityStore = identityStore,
      networkMonitor = networkMonitor,
      apiService = apiService,
      baseHttpUrl = BuildConfig.TELEFAM_API_BASE_URL,
      deviceFingerprintProvider = ::buildDeviceFingerprint,
    )
  }

  fun createCallController(): telefam.company.calls.TelefamCallController {
    return telefam.company.calls.TelefamCallController(
      appContext = appContext,
      chatRepository = chatRepository,
      networkMonitor = networkMonitor,
    )
  }

  fun createStarsController(scope: kotlinx.coroutines.CoroutineScope): telefam.company.stars.TelefamStarsController {
    return telefam.company.stars.TelefamStarsController(
      scope = scope,
      repository = this,
    )
  }

  fun createVerificationController(scope: kotlinx.coroutines.CoroutineScope): telefam.company.verification.TelefamVerificationController {
    return telefam.company.verification.TelefamVerificationController(
      appContext = appContext,
      scope = scope,
      repository = this,
    )
  }

  fun createSubscriptionsController(scope: kotlinx.coroutines.CoroutineScope): telefam.company.subscriptions.TelefamSubscriptionsController {
    return telefam.company.subscriptions.TelefamSubscriptionsController(
      scope = scope,
      repository = this,
    )
  }

  internal fun getApiServiceOrNull(): TelefamApiService? = apiService

  internal suspend fun getDeviceFingerprint(): String = buildDeviceFingerprint()

  suspend fun getStreamCredentials(): Pair<String, String> {
    val access = identityStore.sessionFlow.first().accessToken.orEmpty()
    val fp = buildDeviceFingerprint()
    return access to fp
  }

  fun observeFeedPosts(tab: FeedTab): Flow<List<CachedFeedPostEntity>> =
    dao.observeFeedPosts(tab.apiParam)

  fun observeAllVisibleFeedPosts(): Flow<List<CachedFeedPostEntity>> =
    dao.observeAllVisibleFeedPosts()

  fun observeProfile(userId: String): Flow<UserProfileEntity?> = dao.observeProfile(userId)

  fun observeMessages(circleId: String): Flow<List<FamilyMessageEntity>> =
    dao.observeMessagesForCircle(circleId)

  fun observeCachedContacts(
    filter: ContactFilterTab,
    query: String,
    excludeUserId: String,
  ): Flow<List<CachedContactEntity>> {
    return dao.observeAllCachedContacts().map { all ->
      filterAndSortContacts(all, filter, query, excludeUserId)
    }
  }

  suspend fun loadContactsPage(
    filter: ContactFilterTab,
    query: String,
    page: Int,
    pageSize: Int = TelefamContactsRules.PAGE_SIZE,
  ): ContactsPageFetchResult {
    val cleanPage = page.coerceAtLeast(1)
    val cleanQuery = TelefamProfileSanitizer.sanitizePlainText(query, 64, singleLine = true)
    val session = identityStore.sessionFlow.first()
    val currentUserId = session.userId.orEmpty()
    val accessToken = session.accessToken.orEmpty()
    val api = apiService

    if (api != null && accessToken.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val deviceFp = buildDeviceFingerprint()
      val resp = executeWithWeakNetworkRetry {
        api.listContacts(
          bearer = "Bearer $accessToken",
          deviceFingerprint = deviceFp,
          filter = filter.apiParam,
          query = cleanQuery,
          page = cleanPage,
          pageSize = pageSize,
        )
      }
      val body = if (resp != null && resp.isSuccessful) resp.body() else null
      if (body != null && body.success) {
        val nowMs = System.currentTimeMillis()
        val pendingByTarget = dao.getAllPendingContactSyncActions().associateBy { it.targetUserId }
        val existingById = body.items.mapNotNull { dao.getCachedContact(it.userId) }.associateBy { it.userId }
        val entities = body.items.map { item ->
          val pending = pendingByTarget[item.userId]
          val existing = existingById[item.userId]
          CachedContactEntity(
            userId = item.userId,
            fullName = item.fullName,
            username = item.username,
            usernameLower = item.username.lowercase(),
            location = item.location,
            countryIso = item.countryIso,
            avatarUrl = item.avatarUrl,
            avatarLocalPath = existing?.avatarLocalPath?.takeIf { File(it).exists() },
            isFollowing = pending?.wantFollow ?: item.isFollowing,
            followsYou = item.followsYou,
            riskScore = item.riskScore,
            updatedAtMs = item.updatedAtMs.takeIf { it > 0L } ?: nowMs,
            cachedAtMs = nowMs,
          )
        }
        if (entities.isNotEmpty()) {
          dao.upsertCachedContacts(entities)
          cacheContactAvatarsIfNeeded(api, entities)
        }
        return ContactsPageFetchResult(
          page = body.page.coerceAtLeast(cleanPage),
          hasMore = body.hasMore,
          fromCache = false,
          pageItems = entities.filter { it.userId != currentUserId },
        )
      }
    }

    // Offline or weak-network fallback from aggressive Room cache
    val cachedAll = dao.observeAllCachedContacts().first()
    val filtered = filterAndSortContacts(cachedAll, filter, cleanQuery, currentUserId)
    val offset = (cleanPage - 1) * pageSize
    val slice = if (offset >= filtered.size) emptyList() else filtered.subList(offset, minOf(filtered.size, offset + pageSize))
    val hasMore = offset + slice.size < filtered.size
    return ContactsPageFetchResult(
      page = cleanPage,
      hasMore = hasMore,
      fromCache = true,
      pageItems = slice,
    )
  }

  suspend fun toggleContactFollow(targetUserId: String): ContactToggleResult {
    val session = identityStore.sessionFlow.first()
    val userId = session.userId.orEmpty()
    if (userId.isBlank() || targetUserId == userId) {
      return ContactToggleResult.RateLimited(TelefamContactsRules.RATE_LIMIT_TOAST_MESSAGE)
    }
    val existing = dao.getCachedContact(targetUserId) ?: return ContactToggleResult.Synced
    val nowMs = System.currentTimeMillis()
    val deviceFp = buildDeviceFingerprint()
    val cutoff1m = nowMs - TelefamContactsRules.ONE_MINUTE_MS
    dao.pruneExpiredHits(nowMs - TelefamCoreEngine.WINDOW_24H_MS)

    val acctHits = dao.countHitsSince("contact_act:$userId", cutoff1m)
    val devHits = dao.countHitsSince("contact_dev:$deviceFp", cutoff1m)
    if (acctHits >= TelefamContactsRules.MAX_ACTIONS_PER_MINUTE ||
      devHits >= TelefamContactsRules.MAX_ACTIONS_PER_MINUTE
    ) {
      logAudit(userId, "contact_rate_limited", "{\"target\":\"$targetUserId\"}")
      return ContactToggleResult.RateLimited(TelefamContactsRules.RATE_LIMIT_TOAST_MESSAGE)
    }

    val wantFollow = !existing.isFollowing
    dao.insertHit(RateLimitHitEntity(dimensionKey = "contact_act:$userId", timestampMs = nowMs))
    dao.insertHit(RateLimitHitEntity(dimensionKey = "contact_dev:$deviceFp", timestampMs = nowMs))

    // Optimistic update in Room so UI switches immediately (even offline)
    dao.updateCachedContactRelationship(
      userId = targetUserId,
      isFollowing = wantFollow,
      followsYou = existing.followsYou,
      updatedAtMs = nowMs,
      cachedAtMs = nowMs,
    )
    dao.updateFeedAuthorFollowing(targetUserId, wantFollow)

    val api = apiService
    val accessToken = session.accessToken.orEmpty()
    if (api != null && accessToken.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val resp = executeWithWeakNetworkRetry {
        val body = ContactActionDto(targetUserId = targetUserId)
        if (wantFollow) {
          api.followContact("Bearer $accessToken", deviceFp, body)
        } else {
          api.unfollowContact("Bearer $accessToken", deviceFp, body)
        }
      }
      if (resp?.code() == 429) {
        // Revert optimistic local flip when server denies due to rate limit
        dao.updateCachedContactRelationship(
          userId = targetUserId,
          isFollowing = existing.isFollowing,
          followsYou = existing.followsYou,
          updatedAtMs = existing.updatedAtMs,
          cachedAtMs = System.currentTimeMillis(),
        )
        logAudit(userId, "contact_rate_limited", "{\"target\":\"$targetUserId\",\"src\":\"server\"}")
        return ContactToggleResult.RateLimited(TelefamContactsRules.RATE_LIMIT_TOAST_MESSAGE)
      }
      val body = if (resp != null && resp.isSuccessful) resp.body() else null
      if (body != null && body.success) {
        dao.deletePendingContactSyncForTarget(targetUserId)
        dao.updateCachedContactRelationship(
          userId = targetUserId,
          isFollowing = body.isFollowing,
          followsYou = body.followsYou,
          updatedAtMs = body.serverEpochMs.takeIf { it > 0L } ?: nowMs,
          cachedAtMs = nowMs,
        )
        logAudit(
          userId,
          if (wantFollow) "contact_follow" else "contact_unfollow",
          "{\"target\":\"$targetUserId\",\"risk\":${body.callerRiskScore}}",
        )
        return ContactToggleResult.Synced
      }
    }

    // Queue offline for automatic replay when internet returns
    dao.deletePendingContactSyncForTarget(targetUserId)
    dao.enqueueContactSyncAction(
      ContactSyncActionEntity(
        clientOpId = "cst_${UUID.randomUUID()}",
        targetUserId = targetUserId,
        wantFollow = wantFollow,
        createdAtMs = nowMs,
        retryCount = 0,
        nextRetryAtMs = nowMs,
      ),
    )
    logAudit(
      userId,
      if (wantFollow) "contact_follow_queued_offline" else "contact_unfollow_queued_offline",
      "{\"target\":\"$targetUserId\"}",
    )
    return ContactToggleResult.QueuedOffline
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    identityStore.setThemeMode(mode)
  }

  suspend fun getCurrentCooldownForEmail(email: String): Pair<Long, Long> {
    val cleanEmail = email.trim().lowercase()
    if (cleanEmail.isBlank()) return 0L to 60L
    val deviceFp = buildDeviceFingerprint()
    val accountHash = TelefamCoreEngine.hashAccountEmail(cleanEmail)
    val compositeKey = "$accountHash:$deviceFp"
    val state = dao.getRateLimitState(compositeKey) ?: return 0L to 60L
    val nowMs = System.currentTimeMillis()
    if (nowMs - state.lastUpdatedEpochMs >= TelefamCoreEngine.WINDOW_24H_MS) {
      dao.clearRateLimitState(compositeKey)
      return 0L to 60L
    }
    val remainingSec = ((state.nextAllowedEpochMs - nowMs + 999L) / 1000L).coerceAtLeast(0L)
    return remainingSec to state.activeCooldownDurationSeconds.coerceAtLeast(60L)
  }

  suspend fun savePendingOtpEmail(email: String?) {
    identityStore.savePendingOtpEmail(email)
  }

  suspend fun ensureSessionFresh(): Boolean {
    val session = identityStore.sessionFlow.first()
    val userId = session.userId ?: return false
    val refreshToken = session.refreshToken ?: return false
    val nowMs = System.currentTimeMillis()

    if (nowMs > session.refreshExpiresAtMs) {
      identityStore.clearSession()
      return false
    }
    // If short-lived access token is still valid, keep current session
    if (!session.accessToken.isNullOrBlank() && nowMs < session.accessExpiresAtMs - 30_000L) {
      return true
    }

    // Rotate short-lived access + long-lived refresh token silently with reuse detection
    val deviceFp = buildDeviceFingerprint()
    val ipFp = buildIpFingerprint()
    val tokenHash = EncryptedTokenVault.sha256Hex(refreshToken)
    val record = dao.getRefreshTokenRecord(tokenHash)

    if (record != null && (record.revoked || record.deviceFp != deviceFp || nowMs > record.expiresAtMs)) {
      if (record.revoked) {
        dao.revokeTokenFamily(record.familyId)
        logAudit(userId, "token_reuse_attack_revoked", "{\"family\":\"${record.familyId}\"}")
      }
      identityStore.clearSession()
      return false
    }

    val api = apiService
    if (api != null) {
      val resp = executeWithWeakNetworkRetry {
        api.refreshToken(deviceFp, RefreshTokenDto(refreshToken))
      }
      if (resp == null || !resp.isSuccessful || resp.body()?.success != true) {
        if (resp?.code() == 401) identityStore.clearSession()
        return resp?.code() != 401
      }
      val body = resp.body()!!
      if (record != null) dao.revokeRefreshToken(tokenHash)
      val newRefreshHash = EncryptedTokenVault.sha256Hex(body.refreshToken)
      dao.insertRefreshTokenRecord(
        RefreshTokenRecordEntity(
          tokenHash = newRefreshHash,
          familyId = record?.familyId ?: UUID.randomUUID().toString(),
          userId = userId,
          deviceFp = deviceFp,
          expiresAtMs = nowMs + (body.refreshExpiresInSec * 1000L),
          revoked = false,
          createdAtMs = nowMs,
        ),
      )
      identityStore.saveEncryptedSession(
        userId = userId,
        email = session.email.orEmpty(),
        accessToken = body.sessionToken,
        refreshToken = body.refreshToken,
        accessExpiresInSec = body.accessExpiresInSec,
        refreshExpiresInSec = body.refreshExpiresInSec,
        profileCompleted = session.profileCompleted,
      )
      logAudit(userId, "token_rotated", "{\"device\":\"${deviceFp.take(12)}\"}")
      return true
    }

    // Standalone local rotation with reuse tracking
    if (record != null) dao.revokeRefreshToken(tokenHash)
    val newAccess = "tf_acc_${UUID.randomUUID()}"
    val newRefresh = "tfr_${UUID.randomUUID()}"
    val familyId = record?.familyId ?: UUID.randomUUID().toString()
    dao.insertRefreshTokenRecord(
      RefreshTokenRecordEntity(
        tokenHash = EncryptedTokenVault.sha256Hex(newRefresh),
        familyId = familyId,
        userId = userId,
        deviceFp = deviceFp,
        expiresAtMs = nowMs + 2_592_000_000L,
        revoked = false,
        createdAtMs = nowMs,
      ),
    )
    identityStore.saveEncryptedSession(
      userId = userId,
      email = session.email.orEmpty(),
      accessToken = newAccess,
      refreshToken = newRefresh,
      accessExpiresInSec = 900L,
      refreshExpiresInSec = 2_592_000L,
      profileCompleted = session.profileCompleted,
    )
    dao.insertAuditLog(
      AuditLogEntity(
        userId = userId,
        action = "token_rotated",
        deviceFp = deviceFp,
        ipFp = ipFp,
        metadataJson = "{\"family\":\"$familyId\"}",
        createdAtMs = nowMs,
      ),
    )
    return true
  }

  suspend fun checkEmailStatus(rawEmail: String): EmailCheckResponseDto? {
    val cleanEmail = rawEmail.trim().lowercase()
    if (cleanEmail.isBlank()) return null
    val api = apiService ?: return null
    if (networkTier.value == NetworkTier.OFFLINE) return null
    val deviceFp = buildDeviceFingerprint()
    val resp = runCatching { api.checkEmail(deviceFp, cleanEmail) }.getOrNull()
    val body = if (resp != null && resp.isSuccessful) resp.body() else null
    return if (body?.success == true) body else null
  }

  suspend fun requestOtp(rawEmail: String): AuthActionResult {
    val email = rawEmail.trim().lowercase()
    val nowMs = System.currentTimeMillis()
    val deviceFp = buildDeviceFingerprint()
    val ipFp = buildIpFingerprint()
    val accountHash = TelefamCoreEngine.hashAccountEmail(email)
    val compositeKey = "$accountHash:$deviceFp"

    dao.pruneExpiredHits(nowMs - TelefamCoreEngine.WINDOW_24H_MS)
    val existingState = dao.getRateLimitState(compositeKey)
    if (existingState != null) {
      if (nowMs - existingState.lastUpdatedEpochMs >= TelefamCoreEngine.WINDOW_24H_MS) {
        dao.clearRateLimitState(compositeKey)
      } else if (nowMs < existingState.nextAllowedEpochMs) {
        val remSec = ((existingState.nextAllowedEpochMs - nowMs + 999L) / 1000L).coerceAtLeast(1L)
        return AuthActionResult.SanitizedFailure(
          message = TelefamCoreEngine.sanitizeServerError(429),
          remainingCooldownSeconds = remSec,
        )
      }
    }

    val cutoff24h = nowMs - TelefamCoreEngine.WINDOW_24H_MS
    val acctHits = dao.countHitsSince("acct:$accountHash", cutoff24h)
    val devHits = dao.countHitsSince("dev:$deviceFp", cutoff24h)
    val ipHits = dao.countHitsSince("ip:$ipFp", cutoff24h)
    if (acctHits >= TelefamCoreEngine.MAX_ACCOUNT_REQUESTS_24H ||
      devHits >= TelefamCoreEngine.MAX_DEVICE_REQUESTS_24H ||
      ipHits >= TelefamCoreEngine.MAX_IP_REQUESTS_24H
    ) {
      logAudit(accountHash, "otp_rate_limited", "{}")
      return AuthActionResult.SanitizedFailure(TelefamCoreEngine.sanitizeServerError(429))
    }

    val nextSendCount = (existingState?.sendCount ?: 0) + 1
    var serverCooldown = TelefamCoreEngine.cooldownSecondsForSendCount(nextSendCount)

    val api = apiService
      ?: return AuthActionResult.SanitizedFailure(TelefamCoreEngine.sanitizeServerError(503))

    if (networkTier.value == NetworkTier.OFFLINE) {
      return AuthActionResult.SanitizedFailure(TelefamCoreEngine.sanitizeServerError(503))
    }
    val remoteResult = executeWithWeakNetworkRetry {
      api.requestOtp(deviceFp, ipFp, RequestOtpDto(email))
    } ?: return AuthActionResult.SanitizedFailure(TelefamCoreEngine.sanitizeServerError(503))

    if (!remoteResult.isSuccessful) {
      if (remoteResult.code() == 403) {
        return AuthActionResult.SanitizedFailure(
          message = "This sign-in attempt was denied from your trusted device. Try another way to verify.",
          isDeviceBlocked = true,
        )
      }
      return AuthActionResult.SanitizedFailure(
        TelefamCoreEngine.sanitizeServerError(remoteResult.code(), remoteResult.errorBody()?.string()),
      )
    }
    val body = remoteResult.body()
    if (body == null || !body.success) {
      return AuthActionResult.SanitizedFailure(TelefamCoreEngine.sanitizeServerError(500))
    }

    if (body.requiresDeviceApproval && body.requestId.isNotBlank() && body.pollToken.isNotBlank()) {
      identityStore.savePendingOtpEmail(null)
      logAudit(accountHash, "device_login_approval_requested", "{\"request_id\":\"${body.requestId}\"}")
      return AuthActionResult.RequiresDeviceApproval(
        email = email,
        requestId = body.requestId,
        pollToken = body.pollToken,
        targetDeviceName = body.targetDeviceName.ifBlank { "Your trusted device" },
        targetLocation = body.targetLocation.ifBlank { "Unknown location" },
        requestTimeMs = body.requestTimeMs.takeIf { it > 0L } ?: nowMs,
        expiresInSeconds = body.expiresInSeconds.coerceAtLeast(15L),
        tryAnotherWayProvider = body.tryAnotherWayProvider.ifBlank { "google" },
      )
    }

    if (body.cooldownSeconds > 0L) {
      serverCooldown = maxOf(serverCooldown, body.cooldownSeconds)
    }

    dao.insertHit(RateLimitHitEntity(dimensionKey = "acct:$accountHash", timestampMs = nowMs))
    dao.insertHit(RateLimitHitEntity(dimensionKey = "dev:$deviceFp", timestampMs = nowMs))
    dao.insertHit(RateLimitHitEntity(dimensionKey = "ip:$ipFp", timestampMs = nowMs))

    val nextAllowedMs = nowMs + (serverCooldown * 1000L)
    dao.upsertRateLimitState(
      RateLimitStateEntity(
        compositeKey = compositeKey,
        accountHash = accountHash,
        deviceFp = deviceFp,
        sendCount = nextSendCount,
        nextAllowedEpochMs = nextAllowedMs,
        activeCooldownDurationSeconds = serverCooldown,
        lastUpdatedEpochMs = nowMs,
      ),
    )
    identityStore.savePendingOtpEmail(email)
    logAudit(accountHash, "otp_requested", "{\"tier\":$nextSendCount}")

    return AuthActionResult.OtpSent(
      email = email,
      cooldownSeconds = serverCooldown,
      totalCooldownDurationSeconds = serverCooldown,
    )
  }

  suspend fun pollLoginRequestStatus(
    requestId: String,
    pollToken: String,
    email: String,
  ): LoginApprovalPollOutcome {
    val cleanId = requestId.trim()
    val cleanToken = pollToken.trim()
    val cleanEmail = email.trim().lowercase()
    if (cleanId.isBlank() || cleanToken.isBlank()) {
      return LoginApprovalPollOutcome.Error("Missing verification request reference.")
    }
    val api = apiService ?: return LoginApprovalPollOutcome.Error(TelefamCoreEngine.sanitizeServerError(503))
    networkMonitor.refreshStatus()
    if (networkTier.value == NetworkTier.OFFLINE) {
      return LoginApprovalPollOutcome.Pending(remainingSeconds = 60L, targetDeviceName = "", targetLocation = "")
    }
    val deviceFp = buildDeviceFingerprint()
    val resp = runCatching { api.getLoginRequestStatus(deviceFp, cleanToken, cleanId) }.getOrNull()
      ?: return LoginApprovalPollOutcome.Error("Unable to check approval status.")
    if (!resp.isSuccessful) {
      return if (resp.code() == 404 || resp.code() == 410) {
        LoginApprovalPollOutcome.Expired
      } else if (resp.code() == 403) {
        LoginApprovalPollOutcome.DeniedBlocked
      } else {
        LoginApprovalPollOutcome.Error(TelefamCoreEngine.sanitizeServerError(resp.code()))
      }
    }
    val body = resp.body() ?: return LoginApprovalPollOutcome.Error("Empty response")
    return when (body.status.lowercase()) {
      "pending" -> LoginApprovalPollOutcome.Pending(
        remainingSeconds = body.remainingSeconds.coerceAtLeast(0L),
        targetDeviceName = body.targetDeviceName,
        targetLocation = body.targetLocation,
      )
      "denied" -> LoginApprovalPollOutcome.DeniedBlocked
      "expired" -> LoginApprovalPollOutcome.Expired
      "approved" -> {
        val resolvedEmail = body.email.ifBlank { cleanEmail }
        val resolvedUserId = body.userId.ifBlank { TelefamCoreEngine.hashAccountEmail(resolvedEmail) }
        val existingProfile = dao.getProfileByUserId(resolvedUserId)
        val profileDone = body.profileCompleted || existingProfile != null
        val refreshTok = body.refreshToken.ifBlank { "tfr_${UUID.randomUUID()}" }
        completeSignIn(
          userId = resolvedUserId,
          email = resolvedEmail,
          accessToken = body.sessionToken,
          refreshToken = refreshTok,
          accessExpSec = body.accessExpiresInSec,
          refreshExpSec = body.refreshExpiresInSec,
          compositeKey = "$resolvedUserId:$deviceFp",
          profileCompleted = profileDone,
        )
        if (profileDone) {
          hydrateExistingProfileAfterSignIn(resolvedUserId, resolvedEmail)
        }
        captureAndSyncDeviceContext(force = true)
        LoginApprovalPollOutcome.Approved(
          AuthActionResult.Verified(
            userId = resolvedUserId,
            email = resolvedEmail,
            profileCompleted = profileDone,
          ),
        )
      }
      else -> LoginApprovalPollOutcome.Error("Verification request status unavailable.")
    }
  }

  suspend fun fetchPendingIncomingLoginRequests(): List<PendingLoginRequestItemDto> {
    val session = identityStore.sessionFlow.first()
    val token = session.accessToken.orEmpty()
    val api = apiService
    if (api == null || token.isBlank() || networkTier.value == NetworkTier.OFFLINE) {
      return emptyList()
    }
    val deviceFp = buildDeviceFingerprint()
    val resp = runCatching { api.getPendingLoginRequests("Bearer $token", deviceFp) }.getOrNull()
    val body = if (resp != null && resp.isSuccessful) resp.body() else null
    return if (body?.success == true) body.requests else emptyList()
  }

  suspend fun respondToIncomingLoginRequest(
    requestId: String,
    accept: Boolean,
  ): RespondLoginApprovalResult {
    val cleanId = requestId.trim()
    if (cleanId.isBlank()) {
      return RespondLoginApprovalResult(success = false, message = "Invalid request.")
    }
    val session = identityStore.sessionFlow.first()
    val token = session.accessToken.orEmpty()
    val api = apiService ?: return RespondLoginApprovalResult(success = false, message = "Network unavailable.")
    if (token.isBlank() || networkTier.value == NetworkTier.OFFLINE) {
      return RespondLoginApprovalResult(success = false, message = "Internet connection required to respond.")
    }
    val deviceFp = buildDeviceFingerprint()
    val action = if (accept) "accept" else "deny"
    val resp = executeWithWeakNetworkRetry {
      api.respondToLoginRequest("Bearer $token", deviceFp, cleanId, RespondLoginRequestDto(action))
    }
    if (resp == null || !resp.isSuccessful || resp.body()?.success != true) {
      return RespondLoginApprovalResult(
        success = false,
        message = TelefamCoreEngine.sanitizeServerError(resp?.code() ?: 503),
      )
    }
    val body = resp.body()!!
    val userId = session.userId.orEmpty()
    if (userId.isNotBlank()) {
      logAudit(userId, if (accept) "device_login_request_accepted" else "device_login_request_denied", "{\"request_id\":\"$cleanId\"}")
    }
    return RespondLoginApprovalResult(
      success = true,
      status = body.status.ifBlank { if (accept) "approved" else "denied" },
      deviceName = body.deviceName,
      location = body.location,
      requestedAtMs = body.requestedAtMs,
    )
  }

  suspend fun verifyOtp(rawEmail: String, code: String): AuthActionResult {
    val email = rawEmail.trim().lowercase()
    val cleanCode = code.trim()
    val nowMs = System.currentTimeMillis()
    val deviceFp = buildDeviceFingerprint()
    val ipFp = buildIpFingerprint()
    val accountHash = TelefamCoreEngine.hashAccountEmail(email)

    val cutoff15m = nowMs - TelefamCoreEngine.WINDOW_15M_MS
    val acctVerifyHits = dao.countHitsSince("v_acct:$accountHash", cutoff15m)
    val devVerifyHits = dao.countHitsSince("v_dev:$deviceFp", cutoff15m)
    val ipVerifyHits = dao.countHitsSince("v_ip:$ipFp", cutoff15m)
    if (acctVerifyHits >= TelefamCoreEngine.MAX_VERIFY_ATTEMPTS_15M ||
      devVerifyHits >= TelefamCoreEngine.MAX_VERIFY_ATTEMPTS_15M ||
      ipVerifyHits >= TelefamCoreEngine.MAX_VERIFY_ATTEMPTS_15M
    ) {
      logAudit(accountHash, "otp_verify_rate_limited", "{}")
      return AuthActionResult.SanitizedFailure(TelefamCoreEngine.sanitizeServerError(429))
    }

    if (cleanCode.length != 6 || !cleanCode.all { it.isDigit() }) {
      return AuthActionResult.SanitizedFailure(TelefamCoreEngine.sanitizeServerError(400))
    }

    val api = apiService
      ?: return AuthActionResult.SanitizedFailure(TelefamCoreEngine.sanitizeServerError(503))

    if (networkTier.value == NetworkTier.OFFLINE) {
      return AuthActionResult.SanitizedFailure(TelefamCoreEngine.sanitizeServerError(503))
    }

    dao.insertHit(RateLimitHitEntity(dimensionKey = "v_acct:$accountHash", timestampMs = nowMs))
    dao.insertHit(RateLimitHitEntity(dimensionKey = "v_dev:$deviceFp", timestampMs = nowMs))
    dao.insertHit(RateLimitHitEntity(dimensionKey = "v_ip:$ipFp", timestampMs = nowMs))

    val resp = executeWithWeakNetworkRetry {
      api.verifyOtp(deviceFp, ipFp, VerifyOtpDto(email, cleanCode))
    }
    if (resp == null || !resp.isSuccessful || resp.body()?.success != true) {
      logAudit(accountHash, "otp_verify_failed", "{}")
      return AuthActionResult.SanitizedFailure(TelefamCoreEngine.sanitizeServerError(resp?.code() ?: 503))
    }
    val body = resp.body()!!
    val accessToken = body.sessionToken
    val refreshToken = body.refreshToken.ifBlank { "tfr_${UUID.randomUUID()}" }
    val accessExp = body.accessExpiresInSec
    val refreshExp = body.refreshExpiresInSec
    val resolvedUserId = body.userId.ifBlank { accountHash }

    val existingProfile = dao.getProfileByUserId(resolvedUserId) ?: dao.getProfileByUserId(accountHash)
    val profileDone = body.profileCompleted || existingProfile != null
    completeSignIn(
      userId = resolvedUserId,
      email = email,
      accessToken = accessToken,
      refreshToken = refreshToken,
      accessExpSec = accessExp,
      refreshExpSec = refreshExp,
      compositeKey = "$accountHash:$deviceFp",
      profileCompleted = profileDone,
    )
    if (profileDone) {
      hydrateExistingProfileAfterSignIn(resolvedUserId, email)
    }
    captureAndSyncDeviceContext(force = true)
    return AuthActionResult.Verified(
      userId = resolvedUserId,
      email = email,
      profileCompleted = profileDone,
    )
  }

  suspend fun verifyMagicLink(token: String, fallbackEmail: String?): AuthActionResult {
    val cleanToken = token.trim()
    if (cleanToken.isBlank()) {
      return AuthActionResult.SanitizedFailure(TelefamCoreEngine.sanitizeServerError(400))
    }
    val api = apiService
      ?: return AuthActionResult.SanitizedFailure(TelefamCoreEngine.sanitizeServerError(503))
    if (networkTier.value == NetworkTier.OFFLINE) {
      return AuthActionResult.SanitizedFailure(TelefamCoreEngine.sanitizeServerError(503))
    }

    val deviceFp = buildDeviceFingerprint()
    val ipFp = buildIpFingerprint()

    val resp = executeWithWeakNetworkRetry { api.verifyLink(deviceFp, ipFp, VerifyLinkDto(cleanToken)) }
    if (resp == null || !resp.isSuccessful || resp.body()?.success != true) {
      return AuthActionResult.SanitizedFailure(TelefamCoreEngine.sanitizeServerError(resp?.code() ?: 503))
    }
    val body = resp.body()!!
    val accessToken = body.sessionToken
    val refreshToken = body.refreshToken.ifBlank { "tfr_${UUID.randomUUID()}" }
    val resolvedEmail = body.email.ifBlank { fallbackEmail.orEmpty() }
    if (resolvedEmail.isBlank()) {
      return AuthActionResult.SanitizedFailure(TelefamCoreEngine.sanitizeServerError(401))
    }

    val accountHash = TelefamCoreEngine.hashAccountEmail(resolvedEmail)
    val resolvedUserId = body.userId.ifBlank { accountHash }
    val existingProfile = dao.getProfileByUserId(resolvedUserId) ?: dao.getProfileByUserId(accountHash)
    val profileDone = body.profileCompleted || existingProfile != null
    completeSignIn(
      userId = resolvedUserId,
      email = resolvedEmail,
      accessToken = accessToken,
      refreshToken = refreshToken,
      accessExpSec = body.accessExpiresInSec,
      refreshExpSec = body.refreshExpiresInSec,
      compositeKey = "$accountHash:$deviceFp",
      profileCompleted = profileDone,
    )
    if (profileDone) {
      hydrateExistingProfileAfterSignIn(resolvedUserId, resolvedEmail)
    }
    captureAndSyncDeviceContext(force = true)
    return AuthActionResult.Verified(
      userId = resolvedUserId,
      email = resolvedEmail,
      profileCompleted = profileDone,
    )
  }

  suspend fun authenticateWithGoogle(
    idToken: String,
    email: String,
    expectedEmail: String? = null,
    approvalRequestId: String? = null,
  ): AuthActionResult {
    val cleanEmail = email.trim().lowercase()
    val cleanExpected = expectedEmail?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    if (cleanEmail.isBlank() || idToken.isBlank()) {
      return AuthActionResult.SanitizedFailure(TelefamCoreEngine.sanitizeServerError(400))
    }
    if (cleanExpected != null && !cleanEmail.equals(cleanExpected, ignoreCase = true)) {
      return AuthActionResult.SanitizedFailure(
        "Please sign in with the Google account for $cleanExpected to verify this device.",
      )
    }
    val api = apiService
      ?: return AuthActionResult.SanitizedFailure(TelefamCoreEngine.sanitizeServerError(503))
    if (networkTier.value == NetworkTier.OFFLINE) {
      return AuthActionResult.SanitizedFailure(TelefamCoreEngine.sanitizeServerError(503))
    }

    val deviceFp = buildDeviceFingerprint()
    val ipFp = buildIpFingerprint()
    val accountHash = TelefamCoreEngine.hashAccountEmail(cleanEmail)

    val resp = executeWithWeakNetworkRetry {
      api.verifyGoogleOAuth(
        deviceFp,
        ipFp,
        OAuthDto(
          provider = "google",
          identityToken = idToken,
          expectedEmail = cleanExpected,
          approvalRequestId = approvalRequestId?.takeIf { it.isNotBlank() },
        ),
      )
    }
    if (resp == null || !resp.isSuccessful || resp.body()?.success != true) {
      if (resp?.code() == 403 && cleanExpected != null) {
        return AuthActionResult.SanitizedFailure(
          "Signed-in Google account does not match $cleanExpected.",
        )
      }
      return AuthActionResult.SanitizedFailure(TelefamCoreEngine.sanitizeServerError(resp?.code() ?: 503))
    }
    val body = resp.body()!!
    val accessToken = body.sessionToken
    val refreshToken = body.refreshToken.ifBlank { "tfr_${UUID.randomUUID()}" }
    val resolvedEmail = body.email.ifBlank { cleanEmail }
    val resolvedUserId = body.userId.ifBlank { accountHash }

    val existingProfile = dao.getProfileByUserId(resolvedUserId) ?: dao.getProfileByUserId(accountHash)
    val profileDone = body.profileCompleted || existingProfile != null
    completeSignIn(
      userId = resolvedUserId,
      email = resolvedEmail,
      accessToken = accessToken,
      refreshToken = refreshToken,
      accessExpSec = body.accessExpiresInSec,
      refreshExpSec = body.refreshExpiresInSec,
      compositeKey = "$accountHash:$deviceFp",
      profileCompleted = profileDone,
    )
    if (profileDone) {
      hydrateExistingProfileAfterSignIn(resolvedUserId, resolvedEmail)
    }
    captureAndSyncDeviceContext(force = true)
    return AuthActionResult.Verified(
      userId = resolvedUserId,
      email = resolvedEmail,
      profileCompleted = profileDone,
    )
  }

  private suspend fun hydrateExistingProfileAfterSignIn(userId: String, email: String) {
    val profileDto = runCatching { fetchUserProfile(userId) }.getOrNull() ?: return
    if (profileDto.username.isBlank() && profileDto.fullName.isBlank()) return
    val nowMs = System.currentTimeMillis()
    dao.upsertProfile(
      UserProfileEntity(
        userId = profileDto.userId.ifBlank { userId },
        email = email,
        fullName = profileDto.fullName,
        username = profileDto.username,
        usernameLower = profileDto.username.lowercase(),
        phoneE164 = profileDto.phoneNumber,
        countryIso = profileDto.countryIso.ifBlank { "KE" },
        gender = profileDto.gender,
        dateOfBirthIso = profileDto.dateOfBirth,
        location = profileDto.location,
        bio = profileDto.bio,
        avatarMediaId = null,
        avatarLocalPath = profileDto.avatarUrl.takeIf { it.isNotBlank() },
        avatarSha256 = null,
        avatarDHash = null,
        usernameChangedAtMs = profileDto.usernameChangedAtMs,
        updatedAtMs = nowMs,
      ),
    )
    identityStore.markProfileCompleted(true)
  }

  fun onDeviceLocationModeChangedAsync() {
    telemetryScope.launch {
      captureAndSyncDeviceContext(force = true)
    }
  }

  suspend fun captureAndSyncDeviceContext(force: Boolean = false): String {
    val snapshot = DeviceContextCollector.collectSnapshot(appContext)
    val snapshotJson = runCatching { deviceContextAdapter.toJson(snapshot) }.getOrNull()
    if (!snapshotJson.isNullOrBlank()) {
      identityStore.savePendingDeviceContextJson(snapshotJson)
    }
    return flushPendingDeviceContext(snapshot = snapshot, force = force)
  }

  suspend fun flushPendingDeviceContext(
    snapshot: DeviceContextSnapshot? = null,
    force: Boolean = false,
  ): String {
    val pendingJson = identityStore.getPendingDeviceContextJson()
    val payloadSnap = snapshot ?: pendingJson?.let {
      runCatching { deviceContextAdapter.fromJson(it) }.getOrNull()
    } ?: if (force) DeviceContextCollector.collectSnapshot(appContext) else return ""

    val session = identityStore.sessionFlow.first()
    val userId = session.userId.orEmpty()
    val token = session.accessToken.orEmpty()
    val api = apiService
    networkMonitor.refreshStatus()
    if (userId.isBlank() || token.isBlank() || api == null || networkTier.value == NetworkTier.OFFLINE) {
      return identityStore.getLastSyncedCountryIso()
    }

    val resp = runCatching {
      api.syncDeviceContext(
        bearer = "Bearer $token",
        deviceFingerprint = buildDeviceFingerprint(),
        body = DeviceContextTelemetryDto(
          deviceName = payloadSnap.deviceName,
          platform = payloadSnap.platform,
          hardwareModel = payloadSnap.hardwareModel,
          carrierCountryIso = payloadSnap.carrierCountryIso,
          simCountryIso = payloadSnap.simCountryIso,
          gpsCountryIso = payloadSnap.gpsCountryIso,
          gpsCity = payloadSnap.gpsCity,
          localeCountryIso = payloadSnap.localeCountryIso,
          timezone = payloadSnap.timezone,
          latBucket = payloadSnap.latBucket,
          lonBucket = payloadSnap.lonBucket,
          signalSource = payloadSnap.signalSource,
          capturedAtMs = payloadSnap.capturedAtMs,
        ),
      )
    }.getOrNull()

    val body = if (resp != null && resp.isSuccessful) resp.body() else null
    if (body?.success == true) {
      identityStore.savePendingDeviceContextJson(null)
      val effective = body.effectiveCountryIso.trim().uppercase()
      if (effective.length == 2) {
        identityStore.saveLastSyncedCountryIso(effective)
        val localProf = dao.getProfileByUserId(userId)
        if (localProf != null && localProf.countryIso != effective) {
          dao.upsertProfile(localProf.copy(countryIso = effective, updatedAtMs = System.currentTimeMillis()))
        }
      }
      return effective
    }
    return identityStore.getLastSyncedCountryIso()
  }

  suspend fun isUsernameAvailable(rawUsername: String, currentUserId: String): Boolean {
    val clean = TelefamProfileSanitizer.sanitizeUsernameInput(rawUsername)
    if (!TelefamProfileSanitizer.isValidUsername(clean)) return false
    val localTaken = dao.countOtherUsersWithUsername(clean, currentUserId) > 0
    if (localTaken) return false
    val api = apiService
    if (api != null && networkTier.value != NetworkTier.OFFLINE) {
      val resp = runCatching { api.checkUsername(clean) }.getOrNull()
      if (resp != null && resp.isSuccessful) {
        return resp.body()?.available == true
      }
    }
    return true
  }

  suspend fun processAndUploadAvatar(
    userId: String,
    sourceBitmap: Bitmap,
    centerX: Float,
    centerY: Float,
    zoomScale: Float,
  ): ProcessedAvatarMedia {
    val processed = MediaUploadProcessor.cropEncodeAndStore(
      context = appContext,
      sourceBitmap = sourceBitmap,
      normalizedCenterX = centerX,
      normalizedCenterY = centerY,
      zoomScale = zoomScale,
    )

    var finalMediaId = processed.mediaId
    val api = apiService
    val accessToken = identityStore.sessionFlow.first().accessToken
    if (api != null && !accessToken.isNullOrBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val bytes = File(processed.localFilePath).readBytes()
      val reqBody = bytes.toRequestBody("image/jpeg".toMediaType())
      val uploadResp = runCatching {
        api.uploadAvatar("Bearer $accessToken", buildDeviceFingerprint(), centerX, centerY, zoomScale, reqBody)
      }.getOrNull()
      val serverMediaId = if (uploadResp != null && uploadResp.isSuccessful) {
        uploadResp.body()?.mediaId?.trim()?.takeIf { it.isNotBlank() }
      } else {
        null
      }
      if (serverMediaId != null) {
        finalMediaId = serverMediaId
      }
    }

    val updated = processed.copy(mediaId = finalMediaId)
    dao.insertMediaUpload(
      MediaUploadEntity(
        mediaId = updated.mediaId,
        ownerUserId = userId,
        storageObjectKey = updated.storageObjectKey,
        mimeType = updated.mimeType,
        sha256Fingerprint = updated.sha256Fingerprint,
        dHashFingerprint = updated.dHashFingerprint,
        byteSize = updated.byteSize,
        localFilePath = updated.localFilePath,
        createdAtMs = System.currentTimeMillis(),
      ),
    )

    logAudit(
      userId,
      "media_avatar_uploaded",
      "{\"media_id\":\"${updated.mediaId}\",\"dhash\":\"${updated.dHashFingerprint}\"}",
    )
    return updated
  }

  suspend fun saveProfileSetup(
    userId: String,
    email: String,
    fullNameRaw: String,
    usernameRaw: String,
    dialCode: String,
    phoneDigitsRaw: String,
    countryIso: String,
    genderRaw: String,
    dobYear: Int,
    dobMonthZeroBased: Int,
    dobDay: Int,
    locationRaw: String,
    bioRaw: String,
    avatar: ProcessedAvatarMedia?,
  ): ProfileActionResult {
    val fullName = TelefamProfileSanitizer.sanitizePlainText(fullNameRaw, 64, singleLine = true)
    val username = TelefamProfileSanitizer.sanitizeUsernameInput(usernameRaw)
    val phoneDigits = phoneDigitsRaw.filter { it.isDigit() }
    val phoneE164 = "$dialCode$phoneDigits"
    val location = TelefamProfileSanitizer.sanitizePlainText(locationRaw, 80, singleLine = true)
    val bio = TelefamProfileSanitizer.sanitizePlainText(bioRaw, 150, singleLine = false)
    val gender = genderRaw.trim()

    if (fullName.isBlank() || phoneDigits.length < 5 || gender.isBlank() || location.isBlank()) {
      return ProfileActionResult.Error("Please complete all required profile fields.")
    }
    if (!TelefamProfileSanitizer.isValidUsername(username)) {
      return ProfileActionResult.Error("Username must be 3 to 24 letters, numbers, or underscores.")
    }
    if (!TelefamProfileSanitizer.isAtLeast13YearsOld(dobYear, dobMonthZeroBased, dobDay)) {
      logAudit(userId, "profile_age_rejected", "{}")
      return ProfileActionResult.Error("You must be at least 13 years old to use Telefam.")
    }
    if (!isUsernameAvailable(username, userId)) {
      return ProfileActionResult.Error("This username is already taken.")
    }

    val dobIso = String.format("%04d-%02d-%02d", dobYear, dobMonthZeroBased + 1, dobDay)
    val api = apiService
    val accessToken = identityStore.sessionFlow.first().accessToken
    if (api != null && !accessToken.isNullOrBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val resp = executeWithWeakNetworkRetry {
        api.setupProfile(
          bearer = "Bearer $accessToken",
          deviceFingerprint = buildDeviceFingerprint(),
          body = ProfileSetupDto(
            fullName = fullName,
            username = username,
            phoneNumber = phoneE164,
            countryIso = countryIso,
            gender = gender,
            dateOfBirth = dobIso,
            location = location,
            bio = bio,
            avatarMediaId = avatar?.mediaId,
          ),
        )
      }
      if (resp != null && !resp.isSuccessful) {
        return ProfileActionResult.Error(TelefamCoreEngine.sanitizeServerError(resp.code()))
      }
    }

    val nowMs = System.currentTimeMillis()
    dao.upsertProfile(
      UserProfileEntity(
        userId = userId,
        email = email,
        fullName = fullName,
        username = username,
        usernameLower = username.lowercase(),
        phoneE164 = phoneE164,
        countryIso = countryIso,
        gender = gender,
        dateOfBirthIso = dobIso,
        location = location,
        bio = bio,
        avatarMediaId = avatar?.mediaId,
        avatarLocalPath = avatar?.localFilePath,
        avatarSha256 = avatar?.sha256Fingerprint,
        avatarDHash = avatar?.dHashFingerprint,
        usernameChangedAtMs = nowMs,
        updatedAtMs = nowMs,
      ),
    )
    identityStore.markProfileCompleted(true)
    logAudit(userId, "profile_completed", "{\"username\":\"$username\",\"country\":\"$countryIso\"}")
    return ProfileActionResult.Saved
  }

  suspend fun uploadProfileBackground(uri: Uri): ProfileBackgroundResult = withContext(Dispatchers.IO) {
    val session = identityStore.sessionFlow.first()
    val userId = session.userId.orEmpty()
    val token = session.accessToken.orEmpty()
    if (userId.isBlank()) return@withContext ProfileBackgroundResult.Error("Sign in required.")

    val cachedOwner = dao.getCachedUserProfile(userId)
    val localBgs = cachedOwner?.let {
      runCatching { profileBgListAdapter.fromJson(it.backgroundsJson).orEmpty() }.getOrDefault(emptyList())
    } ?: emptyList()
    if (localBgs.size >= 5) {
      return@withContext ProfileBackgroundResult.Error("You can upload up to 5 background photos.")
    }

    val processed = MediaUploadProcessor.compressProfileBackground(appContext, uri)
      ?: return@withContext ProfileBackgroundResult.Error("Unsupported or invalid image.")

    var resultingList = localBgs + ProfileBackgroundItemDto(
      mediaId = processed.mediaId,
      url = processed.localFilePath,
      sha256Fingerprint = processed.sha256Fingerprint,
      dHashFingerprint = processed.dHashFingerprint,
      width = processed.width,
      height = processed.height,
      byteSize = processed.byteSize,
      createdAtMs = System.currentTimeMillis(),
    )

    val api = apiService
    var syncedMediaId = processed.mediaId
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val bytes = File(processed.localFilePath).readBytes()
      val reqBody = bytes.toRequestBody("image/jpeg".toMediaType())
      val resp = executeWithWeakNetworkRetry {
        api.uploadProfileBackground("Bearer $token", buildDeviceFingerprint(), reqBody)
      }
      if (resp != null && !resp.isSuccessful) {
        if (resp.code() == 409) {
          return@withContext ProfileBackgroundResult.Error("Maximum 5 background photos allowed.")
        }
        return@withContext ProfileBackgroundResult.Error(TelefamCoreEngine.sanitizeServerError(resp.code()))
      }
      val body = resp?.body()
      if (body != null && body.success) {
        syncedMediaId = body.mediaId.ifBlank { processed.mediaId }
        if (body.backgrounds.isNotEmpty()) {
          resultingList = body.backgrounds
        }
      }
    }

    dao.insertMediaUpload(
      MediaUploadEntity(
        mediaId = syncedMediaId,
        ownerUserId = userId,
        storageObjectKey = "media/bg/$syncedMediaId.jpg",
        mimeType = "image/jpeg",
        sha256Fingerprint = processed.sha256Fingerprint,
        dHashFingerprint = processed.dHashFingerprint,
        byteSize = processed.byteSize,
        localFilePath = processed.localFilePath,
        createdAtMs = System.currentTimeMillis(),
      ),
    )

    persistBackgroundListToCache(userId, resultingList)
    logAudit(
      userId,
      "profile_background_uploaded",
      "{\"media_id\":\"$syncedMediaId\",\"sha256\":\"${processed.sha256Fingerprint}\",\"dhash\":\"${processed.dHashFingerprint}\"}",
    )
    ProfileBackgroundResult.Updated(resultingList.take(5))
  }

  suspend fun deleteProfileBackground(mediaId: String): ProfileBackgroundResult = withContext(Dispatchers.IO) {
    val cleanId = mediaId.trim()
    val session = identityStore.sessionFlow.first()
    val userId = session.userId.orEmpty()
    val token = session.accessToken.orEmpty()
    if (userId.isBlank() || cleanId.isBlank()) {
      return@withContext ProfileBackgroundResult.Error("Invalid background selection.")
    }

    val api = apiService
    var updatedList: List<ProfileBackgroundItemDto>? = null
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val resp = executeWithWeakNetworkRetry {
        api.deleteProfileBackground("Bearer $token", buildDeviceFingerprint(), cleanId)
      }
      if (resp != null && !resp.isSuccessful) {
        return@withContext ProfileBackgroundResult.Error(TelefamCoreEngine.sanitizeServerError(resp.code()))
      }
      val body = resp?.body()
      if (body != null && body.success) {
        updatedList = body.backgrounds
      }
    }

    val resolvedList = updatedList ?: run {
      val cached = dao.getCachedUserProfile(userId)
      val cur = cached?.let {
        runCatching { profileBgListAdapter.fromJson(it.backgroundsJson).orEmpty() }.getOrDefault(emptyList())
      } ?: emptyList()
      cur.filterNot { it.mediaId == cleanId }
    }
    persistBackgroundListToCache(userId, resolvedList)
    logAudit(userId, "profile_background_deleted", "{\"media_id\":\"$cleanId\"}")
    ProfileBackgroundResult.Updated(resolvedList)
  }

  suspend fun reorderProfileBackgrounds(mediaIds: List<String>): ProfileBackgroundResult = withContext(Dispatchers.IO) {
    val session = identityStore.sessionFlow.first()
    val userId = session.userId.orEmpty()
    val token = session.accessToken.orEmpty()
    if (userId.isBlank()) return@withContext ProfileBackgroundResult.Error("Sign in required.")

    val api = apiService
    var updatedList: List<ProfileBackgroundItemDto>? = null
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val resp = executeWithWeakNetworkRetry {
        api.reorderProfileBackgrounds("Bearer $token", buildDeviceFingerprint(), ProfileBackgroundReorderDto(mediaIds))
      }
      if (resp != null && resp.isSuccessful && resp.body()?.success == true) {
        updatedList = resp.body()!!.backgrounds
      }
    }

    val resolved = updatedList ?: run {
      val cached = dao.getCachedUserProfile(userId)
      val cur = cached?.let {
        runCatching { profileBgListAdapter.fromJson(it.backgroundsJson).orEmpty() }.getOrDefault(emptyList())
      } ?: emptyList()
      val byId = cur.associateBy { it.mediaId }
      mediaIds.mapNotNull { byId[it] } + cur.filterNot { mediaIds.contains(it.mediaId) }
    }
    persistBackgroundListToCache(userId, resolved)
    ProfileBackgroundResult.Updated(resolved)
  }

  suspend fun updateUserProfile(
    fullNameRaw: String,
    usernameRaw: String,
    dialCode: String,
    phoneDigitsRaw: String,
    countryIso: String,
    genderRaw: String,
    dobYear: Int,
    dobMonthZeroBased: Int,
    dobDay: Int,
    locationRaw: String,
    bioRaw: String,
    avatar: ProcessedAvatarMedia?,
  ): ProfileActionResult = withContext(Dispatchers.IO) {
    val session = identityStore.sessionFlow.first()
    val userId = session.userId.orEmpty()
    val token = session.accessToken.orEmpty()
    if (userId.isBlank()) return@withContext ProfileActionResult.Error("Sign in required.")

    val fullName = TelefamProfileSanitizer.sanitizePlainText(fullNameRaw, 64, singleLine = true)
    val username = TelefamProfileSanitizer.sanitizeUsernameInput(usernameRaw)
    val phoneDigits = phoneDigitsRaw.filter { it.isDigit() }
    val phoneE164 = if (phoneDigits.isNotBlank()) "$dialCode$phoneDigits" else ""
    val location = TelefamProfileSanitizer.sanitizePlainText(locationRaw, 80, singleLine = true)
    val bio = TelefamProfileSanitizer.sanitizePlainText(bioRaw, 150, singleLine = false)
    val gender = genderRaw.trim()

    if (fullName.isBlank() || location.isBlank()) {
      return@withContext ProfileActionResult.Error("Name and location are required.")
    }
    if (!TelefamProfileSanitizer.isValidUsername(username)) {
      return@withContext ProfileActionResult.Error("Username must be 3 to 24 letters, numbers, or underscores.")
    }
    if (phoneDigits.isNotBlank() && phoneDigits.length < 5) {
      return@withContext ProfileActionResult.Error("Enter a valid phone number.")
    }
    if (!TelefamProfileSanitizer.isAtLeast13YearsOld(dobYear, dobMonthZeroBased, dobDay)) {
      return@withContext ProfileActionResult.Error("You must be at least 13 years old.")
    }

    val nowMs = System.currentTimeMillis()
    val existingLocal = dao.getProfileByUserId(userId)
    val existingCached = dao.getCachedUserProfile(userId)
    val prevUsername = existingLocal?.username ?: existingCached?.username.orEmpty()
    val prevChangedAt = maxOf(existingLocal?.usernameChangedAtMs ?: 0L, existingCached?.usernameChangedAtMs ?: 0L)
    val twoWeeksMs = 14L * 24L * 60L * 60L * 1000L
    val isChangingUsername = prevUsername.isNotBlank() && !prevUsername.equals(username, ignoreCase = true)

    if (isChangingUsername && prevChangedAt > 0L && (nowMs - prevChangedAt) < twoWeeksMs) {
      val nextAllowed = prevChangedAt + twoWeeksMs
      return@withContext ProfileActionResult.Error(
        message = "Username can only be changed once every 2 weeks.",
        nextUsernameChangeMs = nextAllowed,
      )
    }

    val dobIso = String.format("%04d-%02d-%02d", dobYear, dobMonthZeroBased + 1, dobDay)
    val api = apiService
    var finalChangedAt = if (isChangingUsername) nowMs else prevChangedAt
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val resp = executeWithWeakNetworkRetry {
        api.updateProfile(
          bearer = "Bearer $token",
          deviceFingerprint = buildDeviceFingerprint(),
          body = ProfileUpdateRequestDto(
            fullName = fullName,
            username = username,
            phoneNumber = phoneE164,
            countryIso = countryIso,
            gender = gender,
            dateOfBirth = dobIso,
            location = location,
            bio = bio,
            avatarMediaId = avatar?.mediaId ?: existingLocal?.avatarMediaId,
          ),
        )
      }
      if (resp != null && !resp.isSuccessful) {
        return@withContext when (resp.code()) {
          429 -> ProfileActionResult.Error(
            message = "Username can only be changed once every 2 weeks.",
            nextUsernameChangeMs = (prevChangedAt + twoWeeksMs).takeIf { prevChangedAt > 0L } ?: (nowMs + twoWeeksMs),
          )
          409 -> ProfileActionResult.Error("This username is already taken.")
          else -> ProfileActionResult.Error(TelefamCoreEngine.sanitizeServerError(resp.code()))
        }
      }
      resp?.body()?.let { body ->
        if (body.usernameChangedAtMs > 0L) {
          finalChangedAt = body.usernameChangedAtMs
        }
      }
    }

    val finalPhone = phoneE164.ifBlank { existingLocal?.phoneE164.orEmpty() }
    val finalCountry = countryIso.ifBlank { existingLocal?.countryIso ?: existingCached?.countryIso ?: "KE" }
    val finalGender = gender.ifBlank { existingLocal?.gender.orEmpty() }
    val finalAvatarMediaId = avatar?.mediaId ?: existingLocal?.avatarMediaId
    val finalAvatarPath = avatar?.localFilePath ?: existingLocal?.avatarLocalPath ?: existingCached?.avatarLocalPath
    val finalAvatarUrl = avatar?.mediaId?.let { "/v1/media/avatar/$it" }
      ?: existingCached?.avatarUrl
      ?: finalAvatarMediaId?.let { "/v1/media/avatar/$it" }

    dao.upsertProfile(
      UserProfileEntity(
        userId = userId,
        email = existingLocal?.email ?: session.email.orEmpty(),
        fullName = fullName,
        username = username,
        usernameLower = username.lowercase(),
        phoneE164 = finalPhone,
        countryIso = finalCountry,
        gender = finalGender,
        dateOfBirthIso = dobIso,
        location = location,
        bio = bio,
        avatarMediaId = finalAvatarMediaId,
        avatarLocalPath = finalAvatarPath,
        avatarSha256 = avatar?.sha256Fingerprint ?: existingLocal?.avatarSha256,
        avatarDHash = avatar?.dHashFingerprint ?: existingLocal?.avatarDHash,
        usernameChangedAtMs = finalChangedAt,
        backgroundsJson = existingLocal?.backgroundsJson ?: existingCached?.backgroundsJson ?: "[]",
        updatedAtMs = nowMs,
      ),
    )

    if (existingCached != null) {
      val nextMs = if (finalChangedAt > 0L) finalChangedAt + twoWeeksMs else 0L
      dao.upsertCachedUserProfile(
        existingCached.copy(
          fullName = fullName,
          username = username,
          avatarUrl = finalAvatarUrl,
          avatarLocalPath = finalAvatarPath,
          bio = bio,
          location = location,
          countryIso = finalCountry,
          gender = finalGender,
          dateOfBirth = dobIso,
          phoneNumber = finalPhone,
          usernameChangedAtMs = finalChangedAt,
          nextUsernameChangeMs = nextMs,
          cachedAtMs = nowMs,
        ),
      )
    }

    logAudit(userId, "profile_updated", "{\"username\":\"$username\",\"country\":\"$finalCountry\"}")
    ProfileActionResult.Saved
  }

  private suspend fun persistBackgroundListToCache(userId: String, list: List<ProfileBackgroundItemDto>) {
    val serialized = runCatching { profileBgListAdapter.toJson(list.take(5)) }.getOrDefault("[]")
    val urlsSerialized = runCatching { stringListAdapter.toJson(list.take(5).map { it.url }) }.getOrDefault("[]")
    dao.getProfileByUserId(userId)?.let { prof ->
      dao.upsertProfile(prof.copy(backgroundsJson = serialized, updatedAtMs = System.currentTimeMillis()))
    }
    dao.getCachedUserProfile(userId)?.let { cached ->
      dao.upsertCachedUserProfile(
        cached.copy(
          backgroundUrlsJson = urlsSerialized,
          backgroundsJson = serialized,
          cachedAtMs = System.currentTimeMillis(),
        ),
      )
    }
  }

  suspend fun logAudit(userId: String, action: String, metadataJson: String = "{}") {
    val cleanUser = userId.ifBlank { "anonymous" }
    val cleanAction = TelefamProfileSanitizer.sanitizePlainText(action, 64, singleLine = true)
    val deviceFp = buildDeviceFingerprint()
    val ipFp = buildIpFingerprint()
    val nowMs = System.currentTimeMillis()
    dao.insertAuditLog(
      AuditLogEntity(
        userId = cleanUser,
        action = cleanAction,
        deviceFp = deviceFp,
        ipFp = ipFp,
        metadataJson = metadataJson,
        createdAtMs = nowMs,
      ),
    )
    val api = apiService
    val accessToken = identityStore.sessionFlow.first().accessToken
    if (api != null && !accessToken.isNullOrBlank() && networkTier.value != NetworkTier.OFFLINE) {
      runCatching {
        api.recordAuditEvent("Bearer $accessToken", deviceFp, AuditEventDto(cleanAction))
      }
    }
  }

  suspend fun flushOfflineQueue() {
    val tier = networkTier.value
    val batchLimit = TelefamCoreEngine.maxBatchSizeForNetwork(tier)
    if (batchLimit <= 0) return

    val nowMs = System.currentTimeMillis()
    flushPendingContactActions(nowMs, batchLimit)
    flushPendingFeedInteractions(nowMs, batchLimit)
    flushPendingPostUploads(nowMs, batchLimit.coerceAtMost(5))
    flushPendingSettingsOps()
    chatRepository.flushPendingChatOps()

    val ready = dao.getReadyOfflineActions(nowMs, batchLimit)
    if (ready.isEmpty()) return

    val accessToken = identityStore.sessionFlow.first().accessToken ?: "local-session"
    val deviceFp = buildDeviceFingerprint()
    val api = apiService
    if (api != null) {
      ready.forEach { dao.updateMessageSyncStatus(it.clientOpId, "SYNCING") }
      val dtoList = ready.map {
        SyncOperationDto(
          clientOpId = it.clientOpId,
          roomId = it.circleId,
          kind = it.kind,
          payload = it.payloadJson,
          createdAt = it.createdAtMs,
        )
      }
      val resp = runCatching {
        api.syncBatch("Bearer $accessToken", deviceFp, SyncBatchRequestDto(dtoList))
      }.getOrNull()
      if (resp != null && resp.isSuccessful && resp.body()?.success == true) {
        val acked = resp.body()!!.ackedOpIds
        if (acked.isNotEmpty()) {
          dao.deleteOfflineActions(acked)
          acked.forEach { dao.updateMessageSyncStatus(it, "SYNCED") }
        }
      } else {
        ready.forEach { action ->
          val nextRetry = nowMs + TelefamCoreEngine.computeBackoffMs(action.retryCount + 1, tier)
          dao.scheduleActionRetry(action.clientOpId, action.retryCount + 1, nextRetry)
          dao.updateMessageSyncStatus(action.clientOpId, "QUEUED")
        }
      }
    } else {
      if (tier == NetworkTier.WEAK) delay(400L)
      val acked = ready.map { it.clientOpId }
      dao.deleteOfflineActions(acked)
      acked.forEach { dao.updateMessageSyncStatus(it, "SYNCED") }
    }
  }

  private suspend fun flushPendingContactActions(nowMs: Long, batchLimit: Int) {
    val api = apiService ?: return
    val accessToken = identityStore.sessionFlow.first().accessToken.orEmpty()
    if (accessToken.isBlank()) return
    val ready = dao.getReadyContactSyncActions(nowMs, batchLimit)
    if (ready.isEmpty()) return

    val tier = networkTier.value
    val deviceFp = buildDeviceFingerprint()
    val dtos = ready.map {
      ContactSyncActionDto(
        clientOpId = it.clientOpId,
        targetUserId = it.targetUserId,
        action = if (it.wantFollow) "follow" else "unfollow",
        createdAtMs = it.createdAtMs,
      )
    }
    val resp = runCatching {
      api.syncContactActions("Bearer $accessToken", deviceFp, ContactSyncBatchRequestDto(dtos))
    }.getOrNull()
    if (resp != null && resp.isSuccessful && resp.body()?.success == true) {
      val acked = resp.body()!!.ackedOpIds
      if (acked.isNotEmpty()) {
        dao.deleteContactSyncActions(acked)
      }
    } else {
      ready.forEach { action ->
        val nextRetry = nowMs + TelefamCoreEngine.computeBackoffMs(action.retryCount + 1, tier)
        dao.scheduleContactSyncRetry(action.clientOpId, action.retryCount + 1, nextRetry)
      }
    }
  }

  suspend fun searchSongs(query: String): List<PostSongTrack> {
    val cleanQuery = TelefamProfileSanitizer.sanitizePlainText(query, 80, singleLine = true)
    val api = apiService ?: return emptyList()
    val token = identityStore.sessionFlow.first().accessToken.orEmpty()
    if (token.isBlank() || networkTier.value == NetworkTier.OFFLINE) return emptyList()
    val deviceFp = buildDeviceFingerprint()
    val resp = executeWithWeakNetworkRetry {
      api.searchSongs("Bearer $token", deviceFp, cleanQuery)
    } ?: return emptyList()
    val body = if (resp.isSuccessful) resp.body() else null
    if (body == null || !body.success) return emptyList()
    return body.items.map { dto ->
      PostSongTrack(
        trackId = dto.trackId,
        title = dto.title,
        artist = dto.artist,
        album = dto.album,
        artworkUrl = dto.artworkUrl,
        previewUrl = dto.previewUrl,
        durationMs = dto.durationMs,
        source = dto.source,
      )
    }
  }

  suspend fun searchHashtags(query: String): List<HashtagItem> {
    val cleanTag = TelefamPostRules.sanitizeHashtag(query)
    val api = apiService
    val token = identityStore.sessionFlow.first().accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val deviceFp = buildDeviceFingerprint()
      val resp = executeWithWeakNetworkRetry {
        api.searchHashtags("Bearer $token", deviceFp, cleanTag)
      }
      val body = if (resp != null && resp.isSuccessful) resp.body() else null
      if (body != null && body.success) {
        val nowMs = System.currentTimeMillis()
        val entities = body.items
          .filter { it.postCount > 0L }
          .map { CachedHashtagEntity(tag = it.tag, postCount = it.postCount, updatedAtMs = nowMs) }
        if (entities.isNotEmpty()) {
          dao.upsertCachedHashtags(entities)
        }
        return body.items.map { HashtagItem(tag = it.tag, postCount = it.postCount) }
      }
    }

    val cached = dao.searchCachedHashtags(cleanTag).map { HashtagItem(it.tag, it.postCount) }
    return if (cleanTag.isNotBlank() && cached.none { it.tag == cleanTag }) {
      cached + HashtagItem(cleanTag, 0L)
    } else {
      cached
    }
  }

  suspend fun compressPostPhotos(uris: List<Uri>, existingCount: Int): List<ProcessedPostPhoto> =
    withContext(Dispatchers.IO) {
      val remainingSlots = (TelefamPostRules.MAX_PHOTOS_COUNT - existingCount).coerceAtLeast(0)
      if (remainingSlots <= 0) return@withContext emptyList()
      uris.take(remainingSlots).mapNotNull { uri ->
        MediaUploadProcessor.compressPostPhoto(appContext, uri)
      }
    }

  suspend fun preparePostVideo(uri: Uri): VideoPrepareResult = withContext(Dispatchers.IO) {
    MediaUploadProcessor.inspectAndExtractVideoFrames(appContext, uri)
  }

  suspend fun submitOrQueuePost(
    input: PostSubmissionInput,
    onProgress: (Int) -> Unit = {},
  ): PostPublishResult {
    val cleanText = TelefamProfileSanitizer.sanitizePlainText(input.textContent, TelefamPostRules.MAX_TEXT_POST_CHARS, singleLine = false)
    val cleanCaption = TelefamProfileSanitizer.sanitizePlainText(input.caption, TelefamPostRules.MAX_CAPTION_CHARS, singleLine = false)
    val cleanHashtags = input.hashtags.mapNotNull { TelefamPostRules.sanitizeHashtag(it).ifBlank { null } }
      .distinct().take(TelefamPostRules.MAX_HASHTAGS)
    val cleanTagged = input.taggedUserIds.distinct().take(TelefamPostRules.MAX_TAGGED_PEOPLE)

    if (input.mode == PostMode.TEXT && cleanText.isBlank()) {
      return PostPublishResult.Rejected("Write something to share")
    }
    if (input.mode == PostMode.PHOTOS && input.localPhotoPaths.isEmpty()) {
      return PostPublishResult.Rejected("Select up to 10 photos")
    }
    if (input.mode == PostMode.VIDEO && input.localVideoPath.isNullOrBlank()) {
      return PostPublishResult.Rejected("Select a video to share")
    }

    val nowMs = System.currentTimeMillis()
    val clientOpId = "post_${UUID.randomUUID()}"
    val songDto = input.song?.let {
      SongTrackDto(
        trackId = it.trackId,
        title = it.title,
        artist = it.artist,
        album = it.album,
        artworkUrl = it.artworkUrl,
        previewUrl = it.previewUrl,
        durationMs = it.durationMs,
        source = it.source,
      )
    }

    val entity = PendingPostUploadEntity(
      clientOpId = clientOpId,
      mode = input.mode.apiValue,
      textContent = cleanText,
      caption = cleanCaption,
      themeId = input.themeId,
      fontId = input.fontId,
      songJson = songDto?.let { songAdapter.toJson(it) },
      taggedUserIdsJson = stringListAdapter.toJson(cleanTagged),
      hashtagsJson = stringListAdapter.toJson(cleanHashtags),
      localPhotoPathsJson = stringListAdapter.toJson(input.localPhotoPaths),
      localVideoPath = input.localVideoPath,
      localThumbnailPath = input.localThumbnailPath,
      frameHashesJson = stringListAdapter.toJson(input.frameHashes),
      durationMs = input.durationMs,
      trimStartMs = input.trimStartMs,
      trimEndMs = input.trimEndMs,
      width = input.width,
      height = input.height,
      muteOriginalAudio = input.muteOriginalAudio,
      contentId = input.contentId,
      status = "QUEUED",
      progressPct = 0,
      retryCount = 0,
      nextRetryAtMs = nowMs,
      createdAtMs = nowMs,
    )
    dao.upsertPendingPostUpload(entity)

    val session = identityStore.sessionFlow.first()
    val userId = session.userId.orEmpty()
    val accessToken = session.accessToken.orEmpty()
    val api = apiService
    if (api == null || accessToken.isBlank() || networkTier.value == NetworkTier.OFFLINE) {
      if (userId.isNotBlank()) {
        logAudit(userId, "post_queued_offline", "{\"mode\":\"${input.mode.apiValue}\"}")
      }
      return PostPublishResult.QueuedOffline(clientOpId)
    }

    return executePendingPostUpload(entity, api, accessToken, userId, onProgress)
  }

  private suspend fun flushPendingPostUploads(nowMs: Long, batchLimit: Int) {
    val api = apiService ?: return
    val session = identityStore.sessionFlow.first()
    val token = session.accessToken.orEmpty()
    val userId = session.userId.orEmpty()
    if (token.isBlank()) return

    val ready = dao.getReadyPendingPostUploads(nowMs, batchLimit)
    for (item in ready) {
      if (networkTier.value == NetworkTier.OFFLINE) break
      executePendingPostUpload(item, api, token, userId)
    }
  }

  private suspend fun executePendingPostUpload(
    entity: PendingPostUploadEntity,
    api: TelefamApiService,
    accessToken: String,
    userId: String,
    onProgress: (Int) -> Unit = {},
  ): PostPublishResult {
    val deviceFp = buildDeviceFingerprint()
    val bearer = "Bearer $accessToken"
    val photoPaths = runCatching { stringListAdapter.fromJson(entity.localPhotoPathsJson) }.getOrNull().orEmpty()
    val taggedIds = runCatching { stringListAdapter.fromJson(entity.taggedUserIdsJson) }.getOrNull().orEmpty()
    val hashtags = runCatching { stringListAdapter.fromJson(entity.hashtagsJson) }.getOrNull().orEmpty()
    val frameHashes = runCatching { stringListAdapter.fromJson(entity.frameHashesJson) }.getOrNull().orEmpty()
    val songDto = entity.songJson?.let { runCatching { songAdapter.fromJson(it) }.getOrNull() }

    dao.updatePendingPostUploadProgress(entity.clientOpId, "UPLOADING", 2)
    onProgress(2)

    val uploadedPhotoIds = mutableListOf<String>()
    var uploadedVideoMediaId = ""
    var uploadedThumbMediaId = ""
    var verifiedContentId = entity.contentId

    when (entity.mode) {
      PostMode.PHOTOS.apiValue -> {
        val total = photoPaths.size.coerceAtLeast(1)
        for ((index, path) in photoPaths.withIndex()) {
          val file = File(path)
          if (!file.exists()) continue
          val reqBody = ProgressFileRequestBody(file, "image/jpeg".toMediaType()) { frac ->
            val overall = (((index + frac) / total.toFloat()) * 85f).toInt().coerceIn(5, 88)
            onProgress(overall)
          }
          val resp = runCatching { api.uploadPostPhoto(bearer, deviceFp, reqBody) }.getOrNull()
          val body = if (resp != null && resp.isSuccessful) resp.body() else null
          if (body == null || !body.success || body.mediaId.isBlank()) {
            return scheduleRetryForPendingPost(entity)
          }
          uploadedPhotoIds += body.mediaId
          if (verifiedContentId.isBlank()) verifiedContentId = body.contentId
          val pct = (((index + 1).toFloat() / total.toFloat()) * 88f).toInt().coerceIn(10, 90)
          dao.updatePendingPostUploadProgress(entity.clientOpId, "UPLOADING", pct)
          onProgress(pct)
        }
        if (uploadedPhotoIds.isEmpty()) {
          dao.deletePendingPostUpload(entity.clientOpId)
          return PostPublishResult.Rejected("Photos no longer available")
        }
      }

      PostMode.VIDEO.apiValue -> {
        val videoFile = entity.localVideoPath?.let { File(it) }
        if (videoFile == null || !videoFile.exists()) {
          dao.deletePendingPostUpload(entity.clientOpId)
          return PostPublishResult.Rejected("Video file unavailable")
        }
        val videoReqBody = ProgressFileRequestBody(videoFile, "video/mp4".toMediaType()) { frac ->
          val pct = (frac * 90f).toInt().coerceIn(3, 92)
          onProgress(pct)
        }
        val videoPart = MultipartBody.Part.createFormData("video", "media.mp4", videoReqBody)
        val thumbPart = entity.localThumbnailPath
          ?.takeIf { it.isNotBlank() && File(it).exists() }
          ?.let { path ->
            val tf = File(path)
            MultipartBody.Part.createFormData(
              "thumbnail",
              "thumb.jpg",
              ProgressFileRequestBody(tf, "image/jpeg".toMediaType()) {},
            )
          }
        val resp = runCatching {
          api.uploadPostVideo(
            bearer = bearer,
            deviceFingerprint = deviceFp,
            frameHashesCsv = frameHashes.joinToString(","),
            durationMs = entity.durationMs,
            trimStartMs = entity.trimStartMs,
            trimEndMs = entity.trimEndMs,
            width = entity.width,
            height = entity.height,
            videoPart = videoPart,
            thumbnailPart = thumbPart,
          )
        }.getOrNull()
        val body = if (resp != null && resp.isSuccessful) resp.body() else null
        if (body == null || !body.success || body.mediaId.isBlank()) {
          return scheduleRetryForPendingPost(entity)
        }
        uploadedVideoMediaId = body.mediaId
        uploadedThumbMediaId = body.thumbnailMediaId
        if (body.contentId.isNotBlank()) verifiedContentId = body.contentId
        dao.updatePendingPostUploadProgress(entity.clientOpId, "UPLOADING", 95)
        onProgress(95)
      }
    }

    val createResp = executeWithWeakNetworkRetry {
      api.createPost(
        bearer = bearer,
        deviceFingerprint = deviceFp,
        body = CreatePostRequestDto(
          clientOpId = entity.clientOpId,
          mode = entity.mode,
          textContent = entity.textContent,
          caption = entity.caption,
          themeId = entity.themeId,
          fontId = entity.fontId,
          song = songDto,
          taggedUserIds = taggedIds,
          hashtags = hashtags,
          photoMediaIds = uploadedPhotoIds,
          videoMediaId = uploadedVideoMediaId,
          thumbnailMediaId = uploadedThumbMediaId,
          trimStartMs = entity.trimStartMs,
          trimEndMs = entity.trimEndMs,
          muteOriginalAudio = entity.muteOriginalAudio,
          contentId = verifiedContentId,
        ),
      )
    }
    val createBody = if (createResp != null && createResp.isSuccessful) createResp.body() else null
    if (createBody == null || !createBody.success || createBody.post == null) {
      return scheduleRetryForPendingPost(entity)
    }

    val nowMs = System.currentTimeMillis()
    hashtags.forEach { tag ->
      val existing = dao.searchCachedHashtags(tag).firstOrNull { it.tag == tag }
      if (existing != null) {
        dao.incrementCachedHashtagCount(tag, nowMs)
      } else {
        dao.upsertCachedHashtags(listOf(CachedHashtagEntity(tag = tag, postCount = 1L, updatedAtMs = nowMs)))
      }
    }
    dao.deletePendingPostUpload(entity.clientOpId)
    onProgress(100)

    if (userId.isNotBlank()) {
      logAudit(
        userId,
        "post_created",
        "{\"post_id\":\"${createBody.post.postId}\",\"mode\":\"${entity.mode}\",\"content_id\":\"$verifiedContentId\"}",
      )
    }

    // Surface newly created post into local feed cache immediately
    val myProfile = dao.getProfileByUserId(userId)
    dao.upsertFeedPosts(
      listOf(
        CachedFeedPostEntity(
          postId = createBody.post.postId,
          authorUserId = userId,
          authorUsername = myProfile?.username ?: "me",
          authorFullName = myProfile?.fullName ?: "Me",
          authorAvatarUrl = myProfile?.avatarLocalPath,
          isFollowingAuthor = false,
          isOwner = true,
          mode = entity.mode,
          textContent = entity.textContent,
          caption = entity.caption,
          themeId = entity.themeId,
          fontId = entity.fontId,
          songJson = entity.songJson,
          hashtagsJson = entity.hashtagsJson,
          photoUrlsJson = entity.localPhotoPathsJson,
          videoUrl = entity.localVideoPath.orEmpty(),
          thumbnailUrl = entity.localThumbnailPath.orEmpty(),
          renditionsJson = "[]",
          width = entity.width,
          height = entity.height,
          durationMs = entity.durationMs,
          muteOriginalAudio = entity.muteOriginalAudio,
          contentId = verifiedContentId,
          likesCount = 0L,
          commentsCount = 0L,
          sharesCount = 0L,
          favouritesCount = 0L,
          repostsCount = 0L,
          isLiked = false,
          isFavourited = false,
          isReposted = false,
          repostedByUsername = "",
          recommendationLabel = "",
          relevanceScore = 10.0,
          tabKey = FeedTab.FOR_YOU.apiParam,
          sortOrder = 0,
          isHiddenByUser = false,
          createdAtMs = createBody.post.createdAtMs.takeIf { it > 0L } ?: nowMs,
          cachedAtMs = nowMs,
        ),
      ),
    )

    return PostPublishResult.Uploaded(
      postId = createBody.post.postId,
      contentId = verifiedContentId,
    )
  }

  // Feeds loading, dynamic search, offline interactions queue, and owner edit/delete
  suspend fun loadFeedPage(
    tab: FeedTab,
    page: Int,
    pageSize: Int = 15,
  ): FeedPageFetchResult {
    val cleanPage = page.coerceAtLeast(1)
    val session = identityStore.sessionFlow.first()
    val token = session.accessToken.orEmpty()
    val api = apiService
    val nowMs = System.currentTimeMillis()

    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val deviceFp = buildDeviceFingerprint()
      val resp = executeWithWeakNetworkRetry {
        api.getFeeds(
          bearer = "Bearer $token",
          deviceFingerprint = deviceFp,
          tab = tab.apiParam,
          page = cleanPage,
          pageSize = pageSize,
        )
      }
      val body = if (resp != null && resp.isSuccessful) resp.body() else null
      if (body != null && body.success) {
        val entities = body.items.mapIndexed { idx, dto ->
          dto.toCachedEntity(
            tabKey = tab.apiParam,
            sortOrder = ((cleanPage - 1) * pageSize) + idx,
            nowMs = nowMs,
          )
        }
        if (entities.isNotEmpty()) {
          dao.upsertFeedPosts(entities)
        }
        return FeedPageFetchResult(
          page = body.page.coerceAtLeast(cleanPage),
          hasMore = body.hasMore,
          fromCache = false,
          items = entities,
        )
      }
    }

    val cached = dao.observeFeedPosts(tab.apiParam).first()
    val offset = (cleanPage - 1) * pageSize
    val slice = if (offset >= cached.size) emptyList() else cached.subList(offset, minOf(cached.size, offset + pageSize))
    return FeedPageFetchResult(
      page = cleanPage,
      hasMore = offset + slice.size < cached.size,
      fromCache = true,
      items = slice,
    )
  }

  suspend fun searchFeeds(
    query: String,
    shortcut: String,
    page: Int = 1,
    pageSize: Int = 24,
  ): FeedSearchFetchResult {
    val cleanQuery = TelefamProfileSanitizer.sanitizePlainText(query, 80, singleLine = true)
    val cleanShortcut = TelefamPostRules.sanitizeHashtag(shortcut)
    val cleanPage = page.coerceAtLeast(1)
    val session = identityStore.sessionFlow.first()
    val token = session.accessToken.orEmpty()
    val api = apiService
    val nowMs = System.currentTimeMillis()

    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val deviceFp = buildDeviceFingerprint()
      val resp = executeWithWeakNetworkRetry {
        api.searchFeeds(
          bearer = "Bearer $token",
          deviceFingerprint = deviceFp,
          query = cleanQuery,
          shortcut = cleanShortcut,
          page = cleanPage,
          pageSize = pageSize,
        )
      }
      val body = if (resp != null && resp.isSuccessful) resp.body() else null
      if (body != null && body.success) {
        val shortcuts = body.shortcuts.map { FeedSearchShortcut(it.key, it.label, it.count) }
        val entities = body.items.mapIndexed { idx, dto ->
          dto.toCachedEntity(
            tabKey = FeedTab.FOR_YOU.apiParam,
            sortOrder = ((cleanPage - 1) * pageSize) + idx,
            nowMs = nowMs,
          )
        }
        if (entities.isNotEmpty()) {
          dao.upsertFeedPosts(entities)
        }
        return FeedSearchFetchResult(
          page = body.page.coerceAtLeast(cleanPage),
          hasMore = body.hasMore,
          shortcuts = shortcuts,
          items = entities,
        )
      }
    }

    // Local fallback across cached feed posts + hashtag counts
    val cachedPosts = dao.observeAllVisibleFeedPosts().first()
    val qLower = cleanQuery.lowercase()
    val tagCounts = mutableMapOf<String, Long>()
    val matched = cachedPosts.filter { post ->
      val tags = parseStringList(post.hashtagsJson)
      tags.forEach { t -> tagCounts[t] = (tagCounts[t] ?: 0L) + 1L }
      val matchesShortcut = cleanShortcut.isBlank() || tags.any { it.equals(cleanShortcut, ignoreCase = true) }
      val matchesQuery = qLower.isBlank() ||
        post.caption.lowercase().contains(qLower) ||
        post.textContent.lowercase().contains(qLower) ||
        post.authorUsername.lowercase().contains(qLower) ||
        post.authorFullName.lowercase().contains(qLower) ||
        tags.any { it.lowercase().contains(qLower) } ||
        (post.songJson?.lowercase()?.contains(qLower) == true)
      matchesShortcut && matchesQuery
    }
    val shortcuts = tagCounts.entries
      .sortedByDescending { it.value }
      .take(10)
      .map { FeedSearchShortcut(key = it.key, label = "#${it.key}", count = it.value) }
    val offset = (cleanPage - 1) * pageSize
    val slice = if (offset >= matched.size) emptyList() else matched.subList(offset, minOf(matched.size, offset + pageSize))
    return FeedSearchFetchResult(
      page = cleanPage,
      hasMore = offset + slice.size < matched.size,
      shortcuts = shortcuts,
      items = slice,
    )
  }

  suspend fun performFeedInteraction(
    postId: String,
    action: FeedInteractionAction,
    watchMs: Long = 0L,
    durationMs: Long = 0L,
    reason: String = "",
    details: String = "",
  ) {
    val existing = dao.getFeedPost(postId)
    val nowMs = System.currentTimeMillis()
    val session = identityStore.sessionFlow.first()
    val viewerId = session.userId.orEmpty()
    val myProfile = if (viewerId.isNotBlank()) dao.getProfileByUserId(viewerId) else null

    // Optimistic update in Room first
    if (existing != null) {
      val updated = when (action) {
        FeedInteractionAction.LIKE -> if (!existing.isLiked) {
          existing.copy(isLiked = true, likesCount = existing.likesCount + 1L)
        } else existing
        FeedInteractionAction.UNLIKE -> if (existing.isLiked) {
          existing.copy(isLiked = false, likesCount = (existing.likesCount - 1L).coerceAtLeast(0L))
        } else existing
        FeedInteractionAction.FAVOURITE -> if (!existing.isFavourited) {
          existing.copy(isFavourited = true, favouritesCount = existing.favouritesCount + 1L)
        } else existing
        FeedInteractionAction.UNFAVOURITE -> if (existing.isFavourited) {
          existing.copy(isFavourited = false, favouritesCount = (existing.favouritesCount - 1L).coerceAtLeast(0L))
        } else existing
        FeedInteractionAction.REPOST -> if (!existing.isReposted) {
          existing.copy(
            isReposted = true,
            repostsCount = existing.repostsCount + 1L,
            repostedByUsername = myProfile?.username ?: "you",
          )
        } else existing
        FeedInteractionAction.UNREPOST -> if (existing.isReposted) {
          existing.copy(
            isReposted = false,
            repostsCount = (existing.repostsCount - 1L).coerceAtLeast(0L),
            repostedByUsername = "",
          )
        } else existing
        FeedInteractionAction.SHARE -> existing.copy(sharesCount = existing.sharesCount + 1L)
        FeedInteractionAction.NOT_INTERESTED -> existing.copy(isHiddenByUser = true)
        FeedInteractionAction.BLOCK -> existing
        FeedInteractionAction.VIEW -> existing.copy(viewsCount = existing.viewsCount + 1L)
        FeedInteractionAction.REPORT, FeedInteractionAction.WATCH, FeedInteractionAction.SKIP -> existing
      }
      if (action == FeedInteractionAction.BLOCK && existing.authorUserId.isNotBlank()) {
        dao.hideFeedPostsByAuthor(existing.authorUserId)
      } else if (action == FeedInteractionAction.NOT_INTERESTED) {
        dao.hideFeedPost(postId)
      } else {
        dao.upsertFeedPosts(listOf(updated))
      }
    }

    val cleanReason = TelefamProfileSanitizer.sanitizePlainText(reason, 64, singleLine = true)
    val cleanDetails = TelefamProfileSanitizer.sanitizePlainText(details, 300, singleLine = false)
    val clientOpId = "fi_${UUID.randomUUID()}"

    val api = apiService
    val token = session.accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val deviceFp = buildDeviceFingerprint()
      val resp = executeWithWeakNetworkRetry {
        api.interactFeed(
          bearer = "Bearer $token",
          deviceFingerprint = deviceFp,
          body = FeedInteractionRequestDto(
            clientOpId = clientOpId,
            postId = postId,
            action = action.apiValue,
            watchMs = watchMs,
            durationMs = durationMs,
            reason = cleanReason,
            details = cleanDetails,
          ),
        )
      }
      val body = if (resp != null && resp.isSuccessful) resp.body() else null
      if (body != null && body.success) {
        val current = dao.getFeedPost(postId)
        if (current != null && !current.isHiddenByUser) {
          dao.upsertFeedPosts(
            listOf(
              current.copy(
                likesCount = body.likesCount,
                favouritesCount = body.favouritesCount,
                repostsCount = body.repostsCount,
                sharesCount = body.sharesCount,
                isLiked = body.isLiked,
                isFavourited = body.isFavourited,
                isReposted = body.isReposted,
              ),
            ),
          )
        }
        return
      }
    }

    // Queue offline for automatic replay when connectivity returns
    dao.enqueueFeedInteraction(
      PendingFeedInteractionEntity(
        clientOpId = clientOpId,
        postId = postId,
        action = action.apiValue,
        watchMs = watchMs,
        durationMs = durationMs,
        reason = cleanReason,
        details = cleanDetails,
        createdAtMs = nowMs,
        retryCount = 0,
        nextRetryAtMs = nowMs,
      ),
    )
  }

  suspend fun toggleFollowAuthorFromFeed(authorUserId: String, currentlyFollowing: Boolean): ContactToggleResult {
    val session = identityStore.sessionFlow.first()
    val userId = session.userId.orEmpty()
    if (userId.isBlank() || authorUserId.isBlank() || authorUserId == userId) {
      return ContactToggleResult.Synced
    }
    if (dao.getCachedContact(authorUserId) != null) {
      return toggleContactFollow(authorUserId)
    }

    val nowMs = System.currentTimeMillis()
    val deviceFp = buildDeviceFingerprint()
    val cutoff1m = nowMs - TelefamContactsRules.ONE_MINUTE_MS
    val acctHits = dao.countHitsSince("contact_act:$userId", cutoff1m)
    if (acctHits >= TelefamContactsRules.MAX_ACTIONS_PER_MINUTE) {
      return ContactToggleResult.RateLimited(TelefamContactsRules.RATE_LIMIT_TOAST_MESSAGE)
    }

    val wantFollow = !currentlyFollowing
    dao.insertHit(RateLimitHitEntity(dimensionKey = "contact_act:$userId", timestampMs = nowMs))
    dao.updateFeedAuthorFollowing(authorUserId, wantFollow)

    val api = apiService
    val token = session.accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val resp = executeWithWeakNetworkRetry {
        val body = ContactActionDto(targetUserId = authorUserId)
        if (wantFollow) api.followContact("Bearer $token", deviceFp, body)
        else api.unfollowContact("Bearer $token", deviceFp, body)
      }
      if (resp?.code() == 429) {
        dao.updateFeedAuthorFollowing(authorUserId, currentlyFollowing)
        return ContactToggleResult.RateLimited(TelefamContactsRules.RATE_LIMIT_TOAST_MESSAGE)
      }
      if (resp != null && resp.isSuccessful && resp.body()?.success == true) {
        dao.updateFeedAuthorFollowing(authorUserId, resp.body()!!.isFollowing)
        return ContactToggleResult.Synced
      }
    }

    dao.deletePendingContactSyncForTarget(authorUserId)
    dao.enqueueContactSyncAction(
      ContactSyncActionEntity(
        clientOpId = "cst_${UUID.randomUUID()}",
        targetUserId = authorUserId,
        wantFollow = wantFollow,
        createdAtMs = nowMs,
        retryCount = 0,
        nextRetryAtMs = nowMs,
      ),
    )
    return ContactToggleResult.QueuedOffline
  }

  suspend fun updateOwnerPost(
    postId: String,
    textContent: String,
    caption: String,
    themeId: String,
    fontId: String,
    song: PostSongTrack?,
    taggedUserIds: List<String>,
    hashtags: List<String>,
    muteOriginalAudio: Boolean,
  ): PostPublishResult {
    val cleanText = TelefamProfileSanitizer.sanitizePlainText(textContent, TelefamPostRules.MAX_TEXT_POST_CHARS, singleLine = false)
    val cleanCaption = TelefamProfileSanitizer.sanitizePlainText(caption, TelefamPostRules.MAX_CAPTION_CHARS, singleLine = false)
    val cleanTags = hashtags.mapNotNull { TelefamPostRules.sanitizeHashtag(it).ifBlank { null } }
      .distinct().take(TelefamPostRules.MAX_HASHTAGS)
    val cleanTagged = taggedUserIds.distinct().take(TelefamPostRules.MAX_TAGGED_PEOPLE)

    val songDto = song?.let {
      SongTrackDto(
        trackId = it.trackId,
        title = it.title,
        artist = it.artist,
        album = it.album,
        artworkUrl = it.artworkUrl,
        previewUrl = it.previewUrl,
        durationMs = it.durationMs,
        source = it.source,
      )
    }

    val existing = dao.getFeedPost(postId)
    if (existing != null) {
      dao.upsertFeedPosts(
        listOf(
          existing.copy(
            textContent = cleanText,
            caption = cleanCaption,
            themeId = themeId,
            fontId = fontId,
            songJson = songDto?.let { songAdapter.toJson(it) },
            hashtagsJson = stringListAdapter.toJson(cleanTags),
            muteOriginalAudio = muteOriginalAudio,
            cachedAtMs = System.currentTimeMillis(),
          ),
        ),
      )
    }

    val api = apiService
    val token = identityStore.sessionFlow.first().accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val deviceFp = buildDeviceFingerprint()
      val resp = executeWithWeakNetworkRetry {
        api.updatePost(
          bearer = "Bearer $token",
          deviceFingerprint = deviceFp,
          postId = postId,
          body = UpdatePostRequestDto(
            textContent = cleanText,
            caption = cleanCaption,
            themeId = themeId,
            fontId = fontId,
            song = songDto,
            taggedUserIds = cleanTagged,
            hashtags = cleanTags,
            muteOriginalAudio = muteOriginalAudio,
          ),
        )
      }
      if (resp != null && !resp.isSuccessful) {
        return PostPublishResult.Rejected(TelefamCoreEngine.sanitizeServerError(resp.code()))
      }
    }
    return PostPublishResult.Uploaded(postId = postId, contentId = existing?.contentId.orEmpty())
  }

  suspend fun updatePostPrivacy(postId: String, privacy: PostPrivacy): Boolean {
    val cleanId = postId.trim()
    if (cleanId.isBlank()) return false
    dao.updateCachedFeedPostPrivacy(cleanId, privacy.apiValue)
    val api = apiService
    val token = identityStore.sessionFlow.first().accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val deviceFp = buildDeviceFingerprint()
      val resp = executeWithWeakNetworkRetry {
        api.updatePost(
          bearer = "Bearer $token",
          deviceFingerprint = deviceFp,
          postId = cleanId,
          body = UpdatePostRequestDto(privacy = privacy.apiValue),
        )
      }
      if (resp != null && !resp.isSuccessful) {
        return false
      }
    }
    val uid = identityStore.sessionFlow.first().userId.orEmpty()
    if (uid.isNotBlank()) {
      logAudit(uid, "post_privacy_updated", "{\"post_id\":\"$cleanId\",\"privacy\":\"${privacy.apiValue}\"}")
    }
    return true
  }

  suspend fun deleteOwnerPost(postId: String): Boolean {
    dao.deleteFeedPost(postId)
    val api = apiService
    val token = identityStore.sessionFlow.first().accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val deviceFp = buildDeviceFingerprint()
      val resp = executeWithWeakNetworkRetry {
        api.deletePost("Bearer $token", deviceFp, postId)
      }
      if (resp != null && resp.code() == 403) return false
    }
    val uid = identityStore.sessionFlow.first().userId.orEmpty()
    if (uid.isNotBlank()) {
      logAudit(uid, "post_deleted", "{\"post_id\":\"$postId\"}")
    }
    return true
  }

  private suspend fun flushPendingFeedInteractions(nowMs: Long, batchLimit: Int) {
    val api = apiService ?: return
    val token = identityStore.sessionFlow.first().accessToken.orEmpty()
    if (token.isBlank()) return
    val ready = dao.getReadyFeedInteractions(nowMs, batchLimit)
    if (ready.isEmpty()) return

    val tier = networkTier.value
    val deviceFp = buildDeviceFingerprint()
    val ops = ready.map { item ->
      FeedInteractionRequestDto(
        clientOpId = item.clientOpId,
        postId = item.postId,
        action = item.action,
        watchMs = item.watchMs,
        durationMs = item.durationMs,
        reason = item.reason,
        details = item.details,
      )
    }
    val resp = runCatching {
      api.interactFeedBatch("Bearer $token", deviceFp, FeedInteractionBatchRequestDto(ops))
    }.getOrNull()
    if (resp != null && resp.isSuccessful && resp.body()?.success == true) {
      val acked = resp.body()!!.ackedOpIds
      if (acked.isNotEmpty()) {
        dao.deleteFeedInteractions(acked)
      }
    } else {
      ready.forEach { item ->
        val nextRetry = nowMs + TelefamCoreEngine.computeBackoffMs(item.retryCount + 1, tier)
        dao.scheduleFeedInteractionRetry(item.clientOpId, item.retryCount + 1, nextRetry)
      }
    }
  }

  fun parseStringList(json: String?): List<String> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching { stringListAdapter.fromJson(json).orEmpty() }.getOrDefault(emptyList())
  }

  fun parseSongTrack(json: String?): PostSongTrack? {
    if (json.isNullOrBlank()) return null
    val dto = runCatching { songAdapter.fromJson(json) }.getOrNull() ?: return null
    return PostSongTrack(
      trackId = dto.trackId,
      title = dto.title,
      artist = dto.artist,
      album = dto.album,
      artworkUrl = dto.artworkUrl,
      previewUrl = dto.previewUrl,
      durationMs = dto.durationMs,
      source = dto.source,
    )
  }

  fun parseRenditions(json: String?): List<FeedVideoRendition> {
    if (json.isNullOrBlank()) return emptyList()
    val list = runCatching { renditionsListAdapter.fromJson(json).orEmpty() }.getOrDefault(emptyList())
    return list.map {
      FeedVideoRendition(
        qualityLabel = it.qualityLabel,
        width = it.width,
        height = it.height,
        bitrateKbps = it.bitrateKbps,
        url = it.url,
        byteSize = it.byteSize,
      )
    }
  }

  private fun FeedItemDto.toCachedEntity(
    tabKey: String,
    sortOrder: Int,
    nowMs: Long,
  ): CachedFeedPostEntity {
    return CachedFeedPostEntity(
      postId = postId,
      authorUserId = authorUserId,
      authorUsername = authorUsername,
      authorFullName = authorFullName,
      authorAvatarUrl = authorAvatarUrl.ifBlank { null },
      isFollowingAuthor = isFollowingAuthor,
      isOwner = isOwner,
      mode = mode,
      textContent = textContent,
      caption = caption,
      themeId = themeId,
      fontId = fontId,
      songJson = song?.let { songAdapter.toJson(it) },
      hashtagsJson = stringListAdapter.toJson(hashtags),
      photoUrlsJson = stringListAdapter.toJson(photoUrls),
      videoUrl = videoUrl,
      thumbnailUrl = thumbnailUrl,
      renditionsJson = renditionsListAdapter.toJson(renditions),
      width = width,
      height = height,
      durationMs = durationMs,
      muteOriginalAudio = muteOriginalAudio,
      contentId = contentId,
      likesCount = likesCount,
      commentsCount = commentsCount,
      sharesCount = sharesCount,
      favouritesCount = favouritesCount,
      repostsCount = repostsCount,
      isLiked = isLiked,
      isFavourited = isFavourited,
      isReposted = isReposted,
      repostedByUsername = repostedByUsername,
      recommendationLabel = recommendationLabel,
      relevanceScore = relevanceScore,
      tabKey = tabKey,
      sortOrder = sortOrder,
      isHiddenByUser = false,
      viewsCount = viewsCount,
      privacy = privacy.ifBlank { "public" },
      authorVerifiedTier = authorVerifiedTier.ifBlank { "none" },
      createdAtMs = createdAtMs,
      cachedAtMs = nowMs,
    )
  }

  private fun CachedFeedPostEntity.toFeedItemDto(): FeedItemDto {
    val parsedSong = songJson?.let { runCatching { songAdapter.fromJson(it) }.getOrNull() }
    val parsedRenditions = runCatching { renditionsListAdapter.fromJson(renditionsJson).orEmpty() }.getOrDefault(emptyList())
    return FeedItemDto(
      postId = postId,
      authorUserId = authorUserId,
      authorUsername = authorUsername,
      authorFullName = authorFullName,
      authorAvatarUrl = authorAvatarUrl.orEmpty(),
      authorVerifiedTier = authorVerifiedTier.ifBlank { "none" },
      isFollowingAuthor = isFollowingAuthor,
      isOwner = isOwner,
      privacy = privacy.ifBlank { "public" },
      mode = mode,
      textContent = textContent,
      caption = caption,
      themeId = themeId,
      fontId = fontId,
      song = parsedSong,
      hashtags = parseStringList(hashtagsJson),
      photoUrls = parseStringList(photoUrlsJson),
      videoUrl = videoUrl,
      thumbnailUrl = thumbnailUrl,
      renditions = parsedRenditions,
      width = width,
      height = height,
      durationMs = durationMs,
      muteOriginalAudio = muteOriginalAudio,
      contentId = contentId,
      likesCount = likesCount,
      commentsCount = commentsCount,
      sharesCount = sharesCount,
      favouritesCount = favouritesCount,
      repostsCount = repostsCount,
      viewsCount = viewsCount,
      isLiked = isLiked,
      isFavourited = isFavourited,
      isReposted = isReposted,
      repostedByUsername = repostedByUsername,
      recommendationLabel = recommendationLabel,
      relevanceScore = relevanceScore,
      createdAtMs = createdAtMs,
    )
  }

  fun feedPostDtoToEntity(dto: FeedItemDto, tabKey: String = "profile", sortOrder: Int = 0): CachedFeedPostEntity {
    return dto.toCachedEntity(tabKey = tabKey, sortOrder = sortOrder, nowMs = System.currentTimeMillis())
  }

  private suspend fun scheduleRetryForPendingPost(entity: PendingPostUploadEntity): PostPublishResult.QueuedOffline {
    val tier = networkTier.value
    val nextRetry = System.currentTimeMillis() + TelefamCoreEngine.computeBackoffMs(entity.retryCount + 1, tier)
    dao.schedulePendingPostRetry(entity.clientOpId, "QUEUED", entity.retryCount + 1, nextRetry)
    return PostPublishResult.QueuedOffline(entity.clientOpId)
  }

  private suspend fun cacheContactAvatarsIfNeeded(
    api: TelefamApiService,
    contacts: List<CachedContactEntity>,
  ) {
    val cacheDir = File(appContext.filesDir, "telefam_media/contacts_avatars").apply {
      if (!exists()) mkdirs()
    }
    for (contact in contacts) {
      val remoteUrl = contact.avatarUrl ?: continue
      val existingPath = contact.avatarLocalPath
      if (!existingPath.isNullOrBlank() && File(existingPath).exists()) continue
      val safeId = contact.userId.replace(Regex("[^A-Za-z0-9_-]"), "_")
      val targetFile = File(cacheDir, "$safeId.jpg")
      if (targetFile.exists() && targetFile.length() > 0L) {
        dao.updateCachedContactAvatarPath(contact.userId, targetFile.absolutePath)
        continue
      }
      runCatching {
        val response = api.downloadMedia(remoteUrl)
        if (response.isSuccessful) {
          val bytes = response.body()?.bytes()
          if (bytes != null && bytes.isNotEmpty() && MediaUploadProcessor.isSupportedImageMagicBytes(bytes)) {
            targetFile.writeBytes(bytes)
            dao.updateCachedContactAvatarPath(contact.userId, targetFile.absolutePath)
          }
        }
      }
    }
  }

  private fun filterAndSortContacts(
    items: List<CachedContactEntity>,
    filter: ContactFilterTab,
    query: String,
    excludeUserId: String,
  ): List<CachedContactEntity> {
    val cleanQuery = query.trim().lowercase()
    val filtered = items.asSequence()
      .filter { it.userId != excludeUserId }
      .filter { item ->
        when (filter) {
          ContactFilterTab.ALL -> true
          ContactFilterTab.SUGGESTIONS -> !item.isFollowing && item.riskScore < 85.0
          ContactFilterTab.FOLLOWERS -> item.followsYou
          ContactFilterTab.FOLLOWING -> item.isFollowing
          ContactFilterTab.FRIENDS -> item.isFollowing && item.followsYou
        }
      }
      .filter { item ->
        cleanQuery.isBlank() ||
          item.fullName.lowercase().contains(cleanQuery) ||
          item.usernameLower.contains(cleanQuery)
      }
      .toList()

    return when (filter) {
      ContactFilterTab.SUGGESTIONS -> filtered.sortedWith(
        compareByDescending<CachedContactEntity> { it.followsYou }
          .thenBy { it.riskScore }
          .thenBy { it.fullName.lowercase() },
      )
      else -> filtered.sortedBy { it.fullName.lowercase() }
    }
  }

  suspend fun fetchUserProfile(userId: String): UserProfileResponseDto? {
    val cleanId = userId.trim()
    if (cleanId.isBlank()) return null
    val session = identityStore.sessionFlow.first()
    val token = session.accessToken.orEmpty()
    val callerId = session.userId.orEmpty()
    val isCaller = cleanId == callerId
    val api = apiService
    val nowMs = System.currentTimeMillis()

    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val deviceFp = buildDeviceFingerprint()
      val resp = executeWithWeakNetworkRetry {
        api.getUserProfile("Bearer $token", deviceFp, cleanId)
      }
      val body = if (resp != null && resp.isSuccessful) resp.body() else null
      if (body != null && body.success) {
        val existingCached = dao.getCachedUserProfile(cleanId)
        val localAvatarPath = cacheProfileAvatarIfNeeded(
          api = api,
          userId = cleanId,
          avatarUrl = body.avatarUrl,
          existingLocalPath = existingCached?.avatarLocalPath,
        )
        val serializedPosts = runCatching { feedItemListAdapter.toJson(body.posts) }.getOrDefault("[]")
        val serializedSaved = runCatching { feedItemListAdapter.toJson(body.savedPosts) }.getOrDefault("[]")
        val serializedReposted = runCatching { feedItemListAdapter.toJson(body.repostedPosts) }.getOrDefault("[]")
        val serializedLocked = runCatching { feedItemListAdapter.toJson(body.lockedPosts) }.getOrDefault("[]")
        val serializedBgUrls = runCatching { stringListAdapter.toJson(body.backgroundUrls) }.getOrDefault("[]")
        val serializedBgs = runCatching { profileBgListAdapter.toJson(body.backgrounds) }.getOrDefault("[]")
        dao.upsertCachedUserProfile(
          CachedUserProfileEntity(
            userId = body.userId.ifBlank { cleanId },
            fullName = body.fullName,
            username = body.username,
            avatarUrl = body.avatarUrl.ifBlank { null },
            avatarLocalPath = localAvatarPath,
            bio = body.bio,
            location = body.location,
            countryIso = body.countryIso,
            followingCount = body.followingCount,
            followersCount = body.followersCount,
            likesCount = body.likesCount,
            isFollowing = body.isFollowing,
            followsYou = body.followsYou,
            isOwner = body.isOwner || isCaller,
            isBlocked = body.isBlocked,
            riskScore = body.riskScore,
            relationshipState = body.relationshipState,
            gender = body.gender,
            dateOfBirth = body.dateOfBirth,
            phoneNumber = body.phoneNumber,
            usernameChangedAtMs = body.usernameChangedAtMs,
            nextUsernameChangeMs = body.nextUsernameChangeMs,
            backgroundUrlsJson = serializedBgUrls,
            backgroundsJson = serializedBgs,
            postsJson = serializedPosts,
            savedPostsJson = serializedSaved,
            repostedPostsJson = serializedReposted,
            lockedPostsJson = serializedLocked,
            verifiedTier = body.verifiedTier.ifBlank { "none" },
            cachedAtMs = nowMs,
          ),
        )
        val effectiveAvatar = localAvatarPath?.takeIf { File(it).exists() } ?: body.avatarUrl
        return body.copy(avatarUrl = effectiveAvatar)
      }
    }

    // Local / Offline fallback from dedicated Room profile cache + Room contacts/posts
    val cachedProfile = dao.getCachedUserProfile(cleanId)
    val myProfile = if (isCaller) dao.getProfileByUserId(callerId) else null
    val cachedContact = dao.getCachedContact(cleanId)
    val pendingSync = dao.getAllPendingContactSyncActions().firstOrNull { it.targetUserId == cleanId }
    val authorPosts = dao.getFeedPostsByAuthor(cleanId)
    val localPostDtos = authorPosts.map { it.toFeedItemDto() }

    if (cachedProfile != null) {
      val cachedPosts = runCatching { feedItemListAdapter.fromJson(cachedProfile.postsJson).orEmpty() }
        .getOrDefault(emptyList())
      val cachedSaved = runCatching { feedItemListAdapter.fromJson(cachedProfile.savedPostsJson).orEmpty() }
        .getOrDefault(emptyList())
      val cachedReposted = runCatching { feedItemListAdapter.fromJson(cachedProfile.repostedPostsJson).orEmpty() }
        .getOrDefault(emptyList())
      val cachedLocked = runCatching { feedItemListAdapter.fromJson(cachedProfile.lockedPostsJson).orEmpty() }
        .getOrDefault(emptyList())
      val cachedBgUrls = parseStringList(cachedProfile.backgroundUrlsJson)
      val cachedBgs = runCatching { profileBgListAdapter.fromJson(cachedProfile.backgroundsJson).orEmpty() }
        .getOrDefault(emptyList())

      val byLocalId = localPostDtos.associateBy { it.postId }
      val effPosts = if (cachedPosts.isNotEmpty()) {
        cachedPosts.map { byLocalId[it.postId] ?: it }.filter { it.privacy != "private" }
      } else {
        localPostDtos.filter { it.privacy != "private" }
      }
      val effLocked = if (isCaller) {
        val localPrivate = localPostDtos.filter { it.privacy == "private" }
        (cachedLocked.map { byLocalId[it.postId] ?: it }.filter { it.privacy == "private" } + localPrivate)
          .distinctBy { it.postId }
      } else emptyList()

      val effFollowing = pendingSync?.wantFollow ?: cachedProfile.isFollowing
      val effFollowsYou = cachedContact?.followsYou ?: cachedProfile.followsYou
      val relState = computeRelationshipStateString(effFollowing, effFollowsYou)
      val avatarResolved = cachedProfile.avatarLocalPath?.takeIf { File(it).exists() }
        ?: myProfile?.avatarLocalPath?.takeIf { File(it).exists() }
        ?: cachedProfile.avatarUrl.orEmpty()
      val effChangedAt = maxOf(cachedProfile.usernameChangedAtMs, myProfile?.usernameChangedAtMs ?: 0L)
      val effNextChange = cachedProfile.nextUsernameChangeMs.takeIf { it > 0L }
        ?: if (effChangedAt > 0L) effChangedAt + (14L * 24L * 60L * 60L * 1000L) else 0L

      return UserProfileResponseDto(
        success = true,
        userId = cachedProfile.userId,
        fullName = myProfile?.fullName?.ifBlank { null } ?: cachedProfile.fullName,
        username = myProfile?.username?.ifBlank { null } ?: cachedProfile.username,
        avatarUrl = avatarResolved,
        bio = myProfile?.bio?.ifBlank { null } ?: cachedProfile.bio,
        location = myProfile?.location?.ifBlank { null } ?: cachedProfile.location,
        countryIso = myProfile?.countryIso?.ifBlank { null } ?: cachedProfile.countryIso,
        gender = myProfile?.gender?.ifBlank { null } ?: cachedProfile.gender,
        dateOfBirth = myProfile?.dateOfBirthIso?.ifBlank { null } ?: cachedProfile.dateOfBirth,
        phoneNumber = myProfile?.phoneE164?.ifBlank { null } ?: cachedProfile.phoneNumber,
        usernameChangedAtMs = effChangedAt,
        nextUsernameChangeMs = effNextChange,
        backgroundUrls = if (cachedBgUrls.isNotEmpty()) cachedBgUrls else cachedBgs.map { it.url },
        backgrounds = cachedBgs,
        followingCount = cachedProfile.followingCount,
        followersCount = cachedProfile.followersCount,
        likesCount = maxOf(cachedProfile.likesCount, authorPosts.sumOf { it.likesCount }),
        isFollowing = effFollowing,
        followsYou = effFollowsYou,
        isOwner = cachedProfile.isOwner || isCaller,
        relationshipState = relState,
        isBlocked = cachedProfile.isBlocked,
        riskScore = cachedProfile.riskScore,
        verifiedTier = cachedProfile.verifiedTier.ifBlank { "none" },
        posts = effPosts,
        savedPosts = cachedSaved,
        repostedPosts = cachedReposted,
        lockedPosts = effLocked,
      )
    }

    if (myProfile == null && cachedContact == null && localPostDtos.isEmpty()) {
      return null
    }

    val fullName = myProfile?.fullName ?: cachedContact?.fullName ?: authorPosts.firstOrNull()?.authorFullName ?: "User"
    val username = myProfile?.username ?: cachedContact?.username ?: authorPosts.firstOrNull()?.authorUsername ?: cleanId
    val avatarUrl = myProfile?.avatarLocalPath ?: cachedContact?.avatarLocalPath ?: cachedContact?.avatarUrl ?: authorPosts.firstOrNull()?.authorAvatarUrl.orEmpty()
    val location = myProfile?.location ?: cachedContact?.location ?: ""
    val bio = myProfile?.bio ?: ""
    val isFollowing = pendingSync?.wantFollow ?: cachedContact?.isFollowing ?: authorPosts.firstOrNull()?.isFollowingAuthor ?: false
    val followsYou = cachedContact?.followsYou ?: false
    val relState = computeRelationshipStateString(isFollowing, followsYou)
    val cachedContactsAll = dao.observeAllCachedContacts().first()
    val offlineFollowingCount = if (isCaller) cachedContactsAll.count { it.isFollowing }.toLong() else dao.getCachedUserConnections(cleanId, "following").size.toLong()
    val offlineFollowersCount = if (isCaller) cachedContactsAll.count { it.followsYou }.toLong() else dao.getCachedUserConnections(cleanId, "followers").size.toLong()
    val localBgs = myProfile?.backgroundsJson?.let {
      runCatching { profileBgListAdapter.fromJson(it).orEmpty() }.getOrDefault(emptyList())
    } ?: emptyList()

    return UserProfileResponseDto(
      success = true,
      userId = cleanId,
      fullName = fullName,
      username = username,
      avatarUrl = avatarUrl,
      bio = bio,
      location = location,
      countryIso = myProfile?.countryIso ?: cachedContact?.countryIso ?: "",
      gender = myProfile?.gender.orEmpty(),
      dateOfBirth = myProfile?.dateOfBirthIso.orEmpty(),
      phoneNumber = myProfile?.phoneE164.orEmpty(),
      usernameChangedAtMs = myProfile?.usernameChangedAtMs ?: 0L,
      nextUsernameChangeMs = if ((myProfile?.usernameChangedAtMs ?: 0L) > 0L) myProfile!!.usernameChangedAtMs + (14L * 24L * 60L * 60L * 1000L) else 0L,
      backgroundUrls = localBgs.map { it.url },
      backgrounds = localBgs,
      followingCount = offlineFollowingCount,
      followersCount = offlineFollowersCount,
      likesCount = authorPosts.sumOf { it.likesCount },
      isFollowing = isFollowing,
      followsYou = followsYou,
      isOwner = isCaller,
      relationshipState = relState,
      isBlocked = false,
      riskScore = cachedContact?.riskScore ?: 0.0,
      posts = localPostDtos.filter { it.privacy != "private" },
      lockedPosts = if (isCaller) localPostDtos.filter { it.privacy == "private" } else emptyList(),
    )
  }

  suspend fun fetchUserConnections(
    userId: String,
    isFollowers: Boolean,
    page: Int = 1,
    pageSize: Int = 30,
  ): UserConnectionsPageResult {
    val cleanId = userId.trim()
    if (cleanId.isBlank()) return UserConnectionsPageResult(items = emptyList(), page = 1, hasMore = false)
    val cleanPage = page.coerceAtLeast(1)
    val listType = if (isFollowers) "followers" else "following"
    val session = identityStore.sessionFlow.first()
    val token = session.accessToken.orEmpty()
    val callerId = session.userId.orEmpty()
    val api = apiService
    val nowMs = System.currentTimeMillis()

    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val deviceFp = buildDeviceFingerprint()
      val resp = executeWithWeakNetworkRetry {
        if (isFollowers) {
          api.getUserFollowers("Bearer $token", deviceFp, cleanId, cleanPage, pageSize)
        } else {
          api.getUserFollowing("Bearer $token", deviceFp, cleanId, cleanPage, pageSize)
        }
      }
      val body = if (resp != null && resp.isSuccessful) resp.body() else null
      if (body != null && body.success) {
        if (cleanPage == 1) {
          dao.deleteCachedUserConnectionsFor(cleanId, listType)
        }
        val offset = (cleanPage - 1) * pageSize
        val cachedEntities = body.items.mapIndexed { idx, item ->
          CachedUserConnectionEntity(
            compositeKey = "$cleanId|$listType|${item.userId}",
            ownerUserId = cleanId,
            listType = listType,
            targetUserId = item.userId,
            fullName = item.fullName,
            username = item.username,
            location = item.location,
            countryIso = item.countryIso,
            avatarUrl = item.avatarUrl,
            avatarLocalPath = dao.getCachedContact(item.userId)?.avatarLocalPath,
            isFollowing = item.isFollowing,
            followsYou = item.followsYou,
            relationshipState = item.relationshipState,
            sortOrder = offset + idx,
            cachedAtMs = nowMs,
          )
        }
        if (cachedEntities.isNotEmpty()) {
          dao.insertCachedUserConnections(cachedEntities)
        }
        return UserConnectionsPageResult(
          items = body.items,
          page = body.page.coerceAtLeast(cleanPage),
          hasMore = body.hasMore,
        )
      }
    }

    val cachedList = dao.getCachedUserConnections(cleanId, listType)
    if (cachedList.isNotEmpty()) {
      val mapped = cachedList.map { c ->
        ContactItemDto(
          userId = c.targetUserId,
          fullName = c.fullName,
          username = c.username,
          location = c.location,
          countryIso = c.countryIso,
          avatarUrl = c.avatarLocalPath ?: c.avatarUrl,
          isFollowing = c.isFollowing,
          followsYou = c.followsYou,
          relationshipState = c.relationshipState,
        )
      }
      val offset = (cleanPage - 1) * pageSize
      val slice = if (offset >= mapped.size) emptyList() else mapped.subList(offset, minOf(mapped.size, offset + pageSize))
      return UserConnectionsPageResult(
        items = slice,
        page = cleanPage,
        hasMore = offset + slice.size < mapped.size,
      )
    }

    if (cleanId == callerId) {
      val cachedContacts = dao.observeAllCachedContacts().first()
      val filtered = cachedContacts.filter { if (isFollowers) it.followsYou else it.isFollowing }
      return UserConnectionsPageResult(
        items = filtered.map { c ->
          ContactItemDto(
            userId = c.userId,
            fullName = c.fullName,
            username = c.username,
            location = c.location,
            countryIso = c.countryIso,
            avatarUrl = c.avatarLocalPath ?: c.avatarUrl,
            isFollowing = c.isFollowing,
            followsYou = c.followsYou,
            relationshipState = computeRelationshipStateString(c.isFollowing, c.followsYou),
            riskScore = c.riskScore,
            updatedAtMs = c.updatedAtMs,
          )
        },
        page = 1,
        hasMore = false,
      )
    }

    return UserConnectionsPageResult(items = emptyList(), page = 1, hasMore = false)
  }

  suspend fun recordPostView(postId: String) {
    if (postId.isBlank()) return
    dao.incrementCachedFeedPostView(postId)
    performFeedInteraction(postId = postId, action = FeedInteractionAction.VIEW)
  }

  suspend fun blockUserProfile(targetUserId: String): Boolean {
    val cleanId = targetUserId.trim()
    if (cleanId.isBlank()) return false
    val session = identityStore.sessionFlow.first()
    val callerId = session.userId.orEmpty()
    if (cleanId == callerId) return false

    dao.hideFeedPostsByAuthor(cleanId)
    dao.updateCachedUserProfileBlocked(cleanId, isBlocked = true, isFollowing = false, relationshipState = "FOLLOW")
    dao.getCachedContact(cleanId)?.let { existing ->
      dao.updateCachedContactRelationship(
        userId = cleanId,
        isFollowing = false,
        followsYou = existing.followsYou,
        updatedAtMs = System.currentTimeMillis(),
        cachedAtMs = System.currentTimeMillis(),
      )
    }
    dao.updateCachedUserConnectionFollow(cleanId, isFollowing = false, relationshipState = "FOLLOW")

    val cachedProf = dao.getCachedUserProfile(cleanId)
    val cachedContact = dao.getCachedContact(cleanId)
    dao.upsertRestrictedAccount(
      CachedRestrictedAccountEntity(
        compositeKey = "$callerId|blocked|$cleanId",
        ownerUserId = callerId,
        kind = "blocked",
        targetUserId = cleanId,
        fullName = cachedProf?.fullName ?: cachedContact?.fullName ?: cleanId,
        username = cachedProf?.username ?: cachedContact?.username ?: cleanId,
        avatarUrl = cachedProf?.avatarUrl ?: cachedContact?.avatarUrl,
        avatarLocalPath = cachedProf?.avatarLocalPath ?: cachedContact?.avatarLocalPath,
        createdAtMs = System.currentTimeMillis(),
      ),
    )

    val api = apiService
    val token = session.accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val resp = executeWithWeakNetworkRetry {
        api.blockUser("Bearer $token", buildDeviceFingerprint(), cleanId)
      }
      if (resp != null && resp.isSuccessful) {
        if (callerId.isNotBlank()) {
          logAudit(callerId, "user_blocked", "{\"target\":\"$cleanId\"}")
        }
        return true
      }
    }
    if (callerId.isNotBlank()) {
      enqueueSettingsOp(callerId, "block", cleanId)
    }
    return true
  }

  suspend fun unblockUserProfile(targetUserId: String): Boolean {
    val cleanId = targetUserId.trim()
    if (cleanId.isBlank()) return false
    val session = identityStore.sessionFlow.first()
    val callerId = session.userId.orEmpty()
    dao.updateCachedUserProfileBlocked(cleanId, isBlocked = false, isFollowing = false, relationshipState = "FOLLOW")
    if (callerId.isNotBlank()) {
      dao.deleteRestrictedAccount(callerId, "blocked", cleanId)
    }

    val api = apiService
    val token = session.accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val resp = executeWithWeakNetworkRetry {
        api.unblockUser("Bearer $token", buildDeviceFingerprint(), cleanId)
      }
      if (resp != null && resp.isSuccessful) {
        if (callerId.isNotBlank()) {
          logAudit(callerId, "user_unblocked", "{\"target\":\"$cleanId\"}")
        }
        return true
      }
    }
    if (callerId.isNotBlank()) {
      enqueueSettingsOp(callerId, "unblock", cleanId)
    }
    return true
  }

  suspend fun reportUserProfile(targetUserId: String, reason: String, details: String): Boolean {
    val cleanId = targetUserId.trim()
    if (cleanId.isBlank()) return false
    val cleanReason = TelefamProfileSanitizer.sanitizePlainText(reason, 64, singleLine = true).ifBlank { "other" }
    val cleanDetails = TelefamProfileSanitizer.sanitizePlainText(details, 300, singleLine = false)
    val session = identityStore.sessionFlow.first()
    val callerId = session.userId.orEmpty()

    if (callerId.isNotBlank()) {
      logAudit(callerId, "user_reported", "{\"target\":\"$cleanId\",\"reason\":\"$cleanReason\"}")
    }

    val api = apiService
    val token = session.accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val resp = executeWithWeakNetworkRetry {
        api.reportUser(
          bearer = "Bearer $token",
          deviceFingerprint = buildDeviceFingerprint(),
          userId = cleanId,
          body = UserReportRequestDto(reason = cleanReason, details = cleanDetails),
        )
      }
      return resp != null && resp.isSuccessful && (resp.body()?.success == true)
    }
    return false
  }

  private suspend fun cacheProfileAvatarIfNeeded(
    api: TelefamApiService,
    userId: String,
    avatarUrl: String,
    existingLocalPath: String?,
  ): String? {
    val cleanUrl = avatarUrl.trim()
    if (cleanUrl.isBlank()) return existingLocalPath?.takeIf { File(it).exists() }
    if (!existingLocalPath.isNullOrBlank() && File(existingLocalPath).exists()) return existingLocalPath
    val cacheDir = File(appContext.filesDir, "telefam_media/profile_avatars").apply {
      if (!exists()) mkdirs()
    }
    val safeId = userId.replace(Regex("[^A-Za-z0-9_-]"), "_")
    val targetFile = File(cacheDir, "$safeId.jpg")
    if (targetFile.exists() && targetFile.length() > 0L) {
      return targetFile.absolutePath
    }
    val resolvedUrl = AdaptiveStreamSelector.resolveMediaUrl(BuildConfig.TELEFAM_API_BASE_URL, cleanUrl)
    runCatching {
      val response = api.downloadMedia(resolvedUrl)
      if (response.isSuccessful) {
        val bytes = response.body()?.bytes()
        if (bytes != null && bytes.isNotEmpty() && MediaUploadProcessor.isSupportedImageMagicBytes(bytes)) {
          targetFile.writeBytes(bytes)
          return targetFile.absolutePath
        }
      }
    }
    return null
  }

  private fun computeRelationshipStateString(isFollowing: Boolean, followsYou: Boolean): String = when {
    isFollowing && followsYou -> "FRIENDS"
    isFollowing -> "FOLLOWING"
    followsYou -> "FOLLOW_BACK"
    else -> "FOLLOW"
  }

  fun applicationContext(): Context = appContext

  suspend fun signOut(allDevices: Boolean = false) {
    val session = identityStore.sessionFlow.first()
    val userId = session.userId.orEmpty()
    val token = session.accessToken.orEmpty()
    val fp = buildDeviceFingerprint()
    val api = apiService
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      runCatching {
        api.unregisterPushToken("Bearer $token", fp, allDevices)
      }
      runCatching {
        api.logout("Bearer $token", fp, LogoutRequestDto(allDevices = allDevices))
      }
    }
    if (userId.isNotBlank()) {
      logAudit(userId, if (allDevices) "user_signed_out_all_devices" else "user_signed_out", "{}")
    }
    TelefamNotificationManager.cancelAllNotifications(appContext)
    chatRepository.clearLocalChatSession()
    runCatching {
      File(appContext.filesDir, "avatars").takeIf { it.exists() }?.deleteRecursively()
      File(appContext.cacheDir, "telefam_media_cache").takeIf { it.exists() }?.deleteRecursively()
    }
    runCatching { dao.clearAllUserScopedData() }
    identityStore.clearSession()
  }

  // Offline-first Settings, Privacy, Notifications, Content Prefs, Offline Sync, Mutes, Support Tickets & System Info
  fun isWifiOrUnmetered(): Boolean = networkMonitor.isWifiOrUnmetered()

  suspend fun loadCachedSettings(): SettingsSnapshot {
    val callerId = identityStore.sessionFlow.first().userId.orEmpty()
    if (callerId.isBlank()) return SettingsSnapshot()
    val entity = dao.getCachedSettings(callerId)
    val base = entity?.let(::mapEntityToSettingsSnapshot) ?: SettingsSnapshot()
    return base.copy(syncPreferences = loadCachedSyncPreferences(callerId))
  }

  suspend fun fetchSettings(): SettingsSnapshot {
    val session = identityStore.sessionFlow.first()
    val callerId = session.userId.orEmpty()
    if (callerId.isBlank()) return SettingsSnapshot()
    val cachedBase = dao.getCachedSettings(callerId)?.let(::mapEntityToSettingsSnapshot) ?: SettingsSnapshot()
    val local = cachedBase.copy(syncPreferences = loadCachedSyncPreferences(callerId))

    val api = apiService
    val token = session.accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      flushPendingSettingsOps()
      val resp = executeWithWeakNetworkRetry { api.getSettings("Bearer $token", buildDeviceFingerprint()) }
      val body = resp?.body()
      if (resp != null && resp.isSuccessful && body?.success == true) {
        val merged = SettingsSnapshot(
          privacy = body.privacy?.let(::mapPrivacyDtoToState) ?: local.privacy,
          notifications = body.notifications?.let(::mapNotificationDtoToState) ?: local.notifications,
          contentPreferences = body.contentPreferences?.let(::mapContentPrefsDtoToState) ?: local.contentPreferences,
          syncPreferences = body.syncPreferences?.let(::mapSyncPrefsDtoToState) ?: local.syncPreferences,
        )
        persistSettingsSnapshot(callerId, merged)
        persistSyncPreferences(callerId, merged.syncPreferences)
        body.chatPrivacy?.let { cpDto ->
          dao.upsertCachedChatPrivacy(mapChatPrivacyStateToEntity(callerId, mapChatPrivacyDtoToState(cpDto)))
        }
        return merged
      }
    }
    return local
  }

  suspend fun loadCachedChatPrivacy(): ChatPrivacySettingsState {
    val callerId = identityStore.sessionFlow.first().userId.orEmpty()
    if (callerId.isBlank()) return ChatPrivacySettingsState()
    return dao.getCachedChatPrivacy(callerId)?.let(::mapChatPrivacyEntityToState) ?: ChatPrivacySettingsState()
  }

  suspend fun fetchChatPrivacy(): ChatPrivacySettingsState {
    val session = identityStore.sessionFlow.first()
    val callerId = session.userId.orEmpty()
    if (callerId.isBlank()) return ChatPrivacySettingsState()
    val local = dao.getCachedChatPrivacy(callerId)?.let(::mapChatPrivacyEntityToState) ?: ChatPrivacySettingsState()

    val api = apiService
    val token = session.accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      flushPendingSettingsOps()
      val resp = executeWithWeakNetworkRetry { api.getChatPrivacy("Bearer $token", buildDeviceFingerprint()) }
      val body = resp?.body()
      if (resp != null && resp.isSuccessful && body?.success == true && body.chatPrivacy != null) {
        val remote = mapChatPrivacyDtoToState(body.chatPrivacy)
        dao.upsertCachedChatPrivacy(mapChatPrivacyStateToEntity(callerId, remote))
        return remote
      }
    }
    return local
  }

  suspend fun saveChatPrivacy(next: ChatPrivacySettingsState): ChatPrivacySettingsState {
    val session = identityStore.sessionFlow.first()
    val callerId = session.userId.orEmpty()
    val stamped = next.copy(
      chatThemeId = TelefamChatPrivacyCatalog.themeById(next.chatThemeId).themeId,
      bubbleColorId = TelefamChatPrivacyCatalog.bubbleColorById(next.bubbleColorId).colorId,
      updatedAtMs = System.currentTimeMillis(),
    )
    if (callerId.isNotBlank()) {
      dao.upsertCachedChatPrivacy(mapChatPrivacyStateToEntity(callerId, stamped))
    }

    val dto = mapChatPrivacyStateToDto(stamped)
    val api = apiService
    val token = session.accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val resp = executeWithWeakNetworkRetry { api.updateChatPrivacy("Bearer $token", buildDeviceFingerprint(), dto) }
      if (resp != null && resp.isSuccessful && resp.body()?.success == true) {
        resp.body()?.chatPrivacy?.let { srv ->
          val merged = mapChatPrivacyDtoToState(srv)
          if (callerId.isNotBlank()) {
            dao.upsertCachedChatPrivacy(mapChatPrivacyStateToEntity(callerId, merged))
          }
          return merged
        }
        return stamped
      }
    }
    if (callerId.isNotBlank()) {
      val json = moshi.adapter(ChatPrivacySettingsDto::class.java).toJson(dto)
      enqueueSettingsOp(callerId, "chat_privacy", json)
    }
    return stamped
  }

  suspend fun saveChatThemeAndBubbleColor(themeId: String? = null, bubbleColorId: String? = null): ChatPrivacySettingsState {
    val session = identityStore.sessionFlow.first()
    val callerId = session.userId.orEmpty()
    val current = if (callerId.isNotBlank()) loadCachedChatPrivacy() else ChatPrivacySettingsState()
    val nextTheme = themeId?.let { TelefamChatPrivacyCatalog.themeById(it).themeId } ?: current.chatThemeId
    val nextBubble = bubbleColorId?.let { TelefamChatPrivacyCatalog.bubbleColorById(it).colorId } ?: current.bubbleColorId
    val updated = current.copy(
      chatThemeId = nextTheme,
      bubbleColorId = nextBubble,
      updatedAtMs = System.currentTimeMillis(),
    )
    if (callerId.isNotBlank()) {
      dao.upsertCachedChatPrivacy(mapChatPrivacyStateToEntity(callerId, updated))
    }

    val req = UpdateChatThemeRequestDto(
      chatThemeId = nextTheme,
      bubbleColorId = nextBubble,
    )
    val api = apiService
    val token = session.accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val resp = executeWithWeakNetworkRetry { api.updateChatTheme("Bearer $token", buildDeviceFingerprint(), req) }
      if (resp != null && resp.isSuccessful && resp.body()?.success == true) {
        resp.body()?.chatPrivacy?.let { srv ->
          val merged = mapChatPrivacyDtoToState(srv)
          if (callerId.isNotBlank()) {
            dao.upsertCachedChatPrivacy(mapChatPrivacyStateToEntity(callerId, merged))
          }
          return merged
        }
        return updated
      }
    }
    if (callerId.isNotBlank()) {
      val json = moshi.adapter(UpdateChatThemeRequestDto::class.java).toJson(req)
      enqueueSettingsOp(callerId, "chat_theme", json)
    }
    return updated
  }

  suspend fun savePrivacySettings(next: PrivacySettingsState): SettingsSnapshot {
    val session = identityStore.sessionFlow.first()
    val callerId = session.userId.orEmpty()
    val stamped = next.copy(updatedAtMs = System.currentTimeMillis())
    val current = if (callerId.isNotBlank()) loadCachedSettings() else SettingsSnapshot()
    val updated = current.copy(privacy = stamped)
    if (callerId.isNotBlank()) {
      persistSettingsSnapshot(callerId, updated)
    }

    val dto = mapPrivacyStateToDto(stamped)
    val api = apiService
    val token = session.accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val resp = executeWithWeakNetworkRetry { api.updatePrivacySettings("Bearer $token", buildDeviceFingerprint(), dto) }
      if (resp != null && resp.isSuccessful) {
        return updated
      }
    }
    if (callerId.isNotBlank()) {
      val json = moshi.adapter(PrivacySettingsDto::class.java).toJson(dto)
      enqueueSettingsOp(callerId, "privacy", json)
    }
    return updated
  }

  suspend fun saveNotificationSettings(next: NotificationSettingsState): SettingsSnapshot {
    val session = identityStore.sessionFlow.first()
    val callerId = session.userId.orEmpty()
    val stamped = next.copy(updatedAtMs = System.currentTimeMillis())
    val current = if (callerId.isNotBlank()) loadCachedSettings() else SettingsSnapshot()
    val updated = current.copy(notifications = stamped)
    if (callerId.isNotBlank()) {
      persistSettingsSnapshot(callerId, updated)
    }

    val dto = mapNotificationStateToDto(stamped)
    val api = apiService
    val token = session.accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val resp = executeWithWeakNetworkRetry { api.updateNotificationSettings("Bearer $token", buildDeviceFingerprint(), dto) }
      if (resp != null && resp.isSuccessful) {
        return updated
      }
    }
    if (callerId.isNotBlank()) {
      val json = moshi.adapter(NotificationSettingsDto::class.java).toJson(dto)
      enqueueSettingsOp(callerId, "notifications", json)
    }
    return updated
  }

  suspend fun saveContentPreferences(next: ContentPreferencesState, resetSuggestions: Boolean = false): SettingsSnapshot {
    val session = identityStore.sessionFlow.first()
    val callerId = session.userId.orEmpty()
    val stamped = next.copy(updatedAtMs = System.currentTimeMillis())
    val current = if (callerId.isNotBlank()) loadCachedSettings() else SettingsSnapshot()
    val updated = current.copy(contentPreferences = stamped)
    if (callerId.isNotBlank()) {
      persistSettingsSnapshot(callerId, updated)
    }

    val dto = mapContentPrefsStateToDto(stamped, resetSuggestions)
    val api = apiService
    val token = session.accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val resp = executeWithWeakNetworkRetry { api.updateContentPreferences("Bearer $token", buildDeviceFingerprint(), dto) }
      if (resp != null && resp.isSuccessful) {
        return updated
      }
    }
    if (callerId.isNotBlank()) {
      val json = moshi.adapter(ContentPreferencesDto::class.java).toJson(dto)
      enqueueSettingsOp(callerId, "content_prefs", json)
    }
    return updated
  }

  suspend fun saveSyncPreferences(next: SyncPreferencesState): SettingsSnapshot {
    val session = identityStore.sessionFlow.first()
    val callerId = session.userId.orEmpty()
    val stamped = next.copy(updatedAtMs = System.currentTimeMillis())
    val current = if (callerId.isNotBlank()) loadCachedSettings() else SettingsSnapshot()
    if (callerId.isNotBlank()) {
      persistSyncPreferences(callerId, stamped)
    }
    val updated = current.copy(syncPreferences = stamped)

    val dto = mapSyncPrefsStateToDto(stamped)
    val api = apiService
    val token = session.accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val resp = executeWithWeakNetworkRetry { api.updateSyncSettings("Bearer $token", buildDeviceFingerprint(), dto) }
      val srvPrefs = resp?.body()?.syncPreferences
      if (resp != null && resp.isSuccessful && resp.body()?.success == true && srvPrefs != null) {
        val synced = mapSyncPrefsDtoToState(srvPrefs)
        if (callerId.isNotBlank()) persistSyncPreferences(callerId, synced)
        return updated.copy(syncPreferences = synced)
      }
    }
    if (callerId.isNotBlank()) {
      val json = moshi.adapter(SyncPreferencesDto::class.java).toJson(dto)
      enqueueSettingsOp(callerId, "sync_prefs", json)
    }
    return updated
  }

  suspend fun getOfflineSyncStatus(
    isSyncing: Boolean = false,
    lastResultMessage: String = "",
  ): OfflineSyncStatusSnapshot {
    val callerId = identityStore.sessionFlow.first().userId.orEmpty()
    if (callerId.isBlank()) {
      return OfflineSyncStatusSnapshot(isSyncing = isSyncing, lastResultMessage = lastResultMessage)
    }
    val syncPrefs = loadCachedSyncPreferences(callerId)
    return OfflineSyncStatusSnapshot(
      pendingSettingsOps = dao.countPendingSettingsOps(callerId),
      pendingContactOps = dao.countPendingContactSyncActions(),
      pendingFeedOps = dao.countPendingFeedInteractions(),
      pendingPostUploads = dao.countPendingPostUploads(),
      pendingActionOps = dao.countPendingOfflineActions(),
      isSyncing = isSyncing,
      lastResultMessage = lastResultMessage,
      lastSyncedAtMs = syncPrefs.lastSyncedAtMs,
    )
  }

  suspend fun performManualServerSyncNow(): Pair<SettingsSnapshot, OfflineSyncStatusSnapshot> {
    val session = identityStore.sessionFlow.first()
    val callerId = session.userId.orEmpty()
    val current = if (callerId.isNotBlank()) loadCachedSettings() else SettingsSnapshot()
    if (callerId.isBlank()) {
      return current to OfflineSyncStatusSnapshot(lastResultMessage = "Sign in required to sync")
    }

    if (networkTier.value == NetworkTier.OFFLINE) {
      return current to getOfflineSyncStatus(
        isSyncing = false,
        lastResultMessage = "Device offline — queued changes will sync when back online",
      )
    }

    if (current.syncPreferences.syncOverWifiOnly && !networkMonitor.isWifiOrUnmetered()) {
      return current to getOfflineSyncStatus(
        isSyncing = false,
        lastResultMessage = "Waiting for Wi-Fi connection (Wi-Fi only mode enabled)",
      )
    }

    // First flush all ready device queues to backend
    runCatching { flushOfflineQueue() }
    runCatching { flushPendingSettingsOps() }
    runCatching { flushPendingDeviceContext(force = true) }

    val token = session.accessToken.orEmpty()
    val api = apiService
    if (api == null || token.isBlank()) {
      return current to getOfflineSyncStatus(
        isSyncing = false,
        lastResultMessage = "Server connection unavailable — changes saved locally",
      )
    }

    val remainingActions = dao.getReadyOfflineActions(Long.MAX_VALUE, 50)
    val opDtos = remainingActions.map {
      SyncOperationDto(
        clientOpId = it.clientOpId,
        roomId = it.circleId,
        kind = it.kind,
        payload = it.payloadJson,
        createdAt = it.createdAtMs,
      )
    }
    val localChatPrivacy = loadCachedChatPrivacy()
    val req = ManualSyncRequestDto(
      clientSyncId = UUID.randomUUID().toString(),
      operations = opDtos,
      privacy = mapPrivacyStateToDto(current.privacy),
      notifications = mapNotificationStateToDto(current.notifications),
      contentPreferences = mapContentPrefsStateToDto(current.contentPreferences, resetSuggestions = false),
      chatPrivacy = mapChatPrivacyStateToDto(localChatPrivacy),
      syncPreferences = mapSyncPrefsStateToDto(current.syncPreferences),
    )

    val resp = executeWithWeakNetworkRetry { api.syncNow("Bearer $token", buildDeviceFingerprint(), req) }
    val body = resp?.body()
    if (resp != null && resp.isSuccessful && body?.success == true) {
      if (body.ackedOpIds.isNotEmpty()) {
        dao.deleteOfflineActions(body.ackedOpIds)
      }
      val resolvedSyncPrefs = body.syncPreferences?.let(::mapSyncPrefsDtoToState)
        ?: current.syncPreferences.copy(
          lastSyncedAtMs = if (body.serverEpochMs > 0L) body.serverEpochMs else System.currentTimeMillis(),
          lastSyncStatus = "synced",
        )
      val merged = SettingsSnapshot(
        privacy = body.privacy?.let(::mapPrivacyDtoToState) ?: current.privacy,
        notifications = body.notifications?.let(::mapNotificationDtoToState) ?: current.notifications,
        contentPreferences = body.contentPreferences?.let(::mapContentPrefsDtoToState) ?: current.contentPreferences,
        syncPreferences = resolvedSyncPrefs,
      )
      persistSettingsSnapshot(callerId, merged)
      persistSyncPreferences(callerId, resolvedSyncPrefs)
      body.chatPrivacy?.let { cpDto ->
        dao.upsertCachedChatPrivacy(mapChatPrivacyStateToEntity(callerId, mapChatPrivacyDtoToState(cpDto)))
      }
      val postStatus = getOfflineSyncStatus(
        isSyncing = false,
        lastResultMessage = if (body.syncedOpsCount > 0) {
          "Synced ${body.syncedOpsCount} operation(s) to server"
        } else {
          "All data up to date with server"
        },
      )
      return merged to postStatus
    }

    return current to getOfflineSyncStatus(
      isSyncing = false,
      lastResultMessage = "Server sync queued — will retry automatically",
    )
  }

  suspend fun listRestrictedAccounts(isMuted: Boolean, query: String = ""): List<RestrictedAccountItem> {
    val session = identityStore.sessionFlow.first()
    val callerId = session.userId.orEmpty()
    if (callerId.isBlank()) return emptyList()
    val kind = if (isMuted) "muted" else "blocked"
    val cleanQuery = query.trim().lowercase()

    val api = apiService
    val token = session.accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      flushPendingSettingsOps()
      val resp = executeWithWeakNetworkRetry {
        if (isMuted) api.listMutedAccounts("Bearer $token", buildDeviceFingerprint(), "")
        else api.listBlockedAccounts("Bearer $token", buildDeviceFingerprint(), "")
      }
      val body = resp?.body()
      if (resp != null && resp.isSuccessful && body?.success == true) {
        dao.clearRestrictedAccountsByKind(callerId, kind)
        val entities = body.items.map { item ->
          CachedRestrictedAccountEntity(
            compositeKey = "$callerId|$kind|${item.userId}",
            ownerUserId = callerId,
            kind = kind,
            targetUserId = item.userId,
            fullName = item.fullName,
            username = item.username,
            avatarUrl = item.avatarUrl.takeIf { it.isNotBlank() },
            avatarLocalPath = null,
            createdAtMs = item.createdAtMs,
          )
        }
        if (entities.isNotEmpty()) dao.upsertRestrictedAccounts(entities)
      }
    }

    return dao.getRestrictedAccounts(callerId, kind)
      .filter {
        cleanQuery.isBlank() ||
          it.fullName.lowercase().contains(cleanQuery) ||
          it.username.lowercase().contains(cleanQuery)
      }
      .map {
        RestrictedAccountItem(
          userId = it.targetUserId,
          fullName = it.fullName,
          username = it.username,
          avatarUrl = it.avatarUrl,
          avatarLocalPath = it.avatarLocalPath,
          isMuted = isMuted,
          createdAtMs = it.createdAtMs,
        )
      }
  }

  suspend fun muteUserAccount(targetUserId: String): Boolean {
    val cleanId = targetUserId.trim()
    val session = identityStore.sessionFlow.first()
    val callerId = session.userId.orEmpty()
    if (cleanId.isBlank() || callerId.isBlank() || cleanId == callerId) return false

    dao.hideFeedPostsByAuthor(cleanId)
    val cachedProf = dao.getCachedUserProfile(cleanId)
    val cachedContact = dao.getCachedContact(cleanId)
    dao.upsertRestrictedAccount(
      CachedRestrictedAccountEntity(
        compositeKey = "$callerId|muted|$cleanId",
        ownerUserId = callerId,
        kind = "muted",
        targetUserId = cleanId,
        fullName = cachedProf?.fullName ?: cachedContact?.fullName ?: cleanId,
        username = cachedProf?.username ?: cachedContact?.username ?: cleanId,
        avatarUrl = cachedProf?.avatarUrl ?: cachedContact?.avatarUrl,
        avatarLocalPath = cachedProf?.avatarLocalPath ?: cachedContact?.avatarLocalPath,
        createdAtMs = System.currentTimeMillis(),
      ),
    )

    val api = apiService
    val token = session.accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val resp = executeWithWeakNetworkRetry { api.muteUser("Bearer $token", buildDeviceFingerprint(), cleanId) }
      if (resp != null && resp.isSuccessful) {
        logAudit(callerId, "user_muted", "{\"target\":\"$cleanId\"}")
        return true
      }
    }
    enqueueSettingsOp(callerId, "mute", cleanId)
    return true
  }

  suspend fun unmuteUserAccount(targetUserId: String): Boolean {
    val cleanId = targetUserId.trim()
    val session = identityStore.sessionFlow.first()
    val callerId = session.userId.orEmpty()
    if (cleanId.isBlank() || callerId.isBlank()) return false

    dao.deleteRestrictedAccount(callerId, "muted", cleanId)
    val api = apiService
    val token = session.accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val resp = executeWithWeakNetworkRetry { api.unmuteUser("Bearer $token", buildDeviceFingerprint(), cleanId) }
      if (resp != null && resp.isSuccessful) {
        logAudit(callerId, "user_unmuted", "{\"target\":\"$cleanId\"}")
        return true
      }
    }
    enqueueSettingsOp(callerId, "unmute", cleanId)
    return true
  }

  suspend fun submitProblemReport(
    category: String,
    description: String,
    screenshotUri: Uri?,
  ): SupportTicketItem? {
    val session = identityStore.sessionFlow.first()
    val callerId = session.userId.orEmpty()
    val cleanCat = TelefamProfileSanitizer.sanitizePlainText(category, 64, singleLine = true)
    val cleanDesc = TelefamProfileSanitizer.sanitizePlainText(description, 2000, singleLine = false)
    if (callerId.isBlank() || cleanCat.isBlank() || cleanDesc.length < 5) return null

    var processedShot: ProcessedPostPhoto? = null
    if (screenshotUri != null) {
      processedShot = withContext(Dispatchers.IO) {
        MediaUploadProcessor.compressPostPhoto(appContext, screenshotUri)
      }
    }

    val opId = UUID.randomUUID().toString()
    val nowMs = System.currentTimeMillis()
    val dateStamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(nowMs))
    val localRef = "TF-$dateStamp-${opId.take(6).uppercase()}"

    var mediaId = ""
    var sha256 = processedShot?.sha256Fingerprint.orEmpty()
    val api = apiService
    val token = session.accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      if (processedShot != null && File(processedShot.localFilePath).exists()) {
        val bytes = File(processedShot.localFilePath).readBytes()
        val uploadResp = runCatching {
          api.uploadPostPhoto(
            bearer = "Bearer $token",
            deviceFingerprint = buildDeviceFingerprint(),
            imageBody = bytes.toRequestBody("image/jpeg".toMediaType()),
          )
        }.getOrNull()
        if (uploadResp != null && uploadResp.isSuccessful) {
          mediaId = uploadResp.body()?.mediaId.orEmpty()
          sha256 = uploadResp.body()?.sha256Fingerprint?.ifBlank { sha256 } ?: sha256
        }
      }

      val reqDto = ProblemReportRequestDto(
        clientOpId = opId,
        category = cleanCat,
        description = cleanDesc,
        screenshotMediaId = mediaId,
        screenshotSha256 = sha256,
      )
      val resp = executeWithWeakNetworkRetry { api.submitProblemReport("Bearer $token", buildDeviceFingerprint(), reqDto) }
      val body = resp?.body()
      if (resp != null && resp.isSuccessful && body?.success == true) {
        val srvTicket = body.ticket
        val item = SupportTicketItem(
          ticketId = srvTicket?.ticketId?.ifBlank { opId } ?: opId,
          referenceNumber = body.referenceNumber.ifBlank { localRef },
          category = srvTicket?.category?.ifBlank { cleanCat } ?: cleanCat,
          description = srvTicket?.description?.ifBlank { cleanDesc } ?: cleanDesc,
          screenshotLocalPath = processedShot?.localFilePath,
          screenshotSha256 = sha256,
          status = srvTicket?.status ?: "open",
          resolutionNote = srvTicket?.resolutionNote ?: "Submitted to Trust & Support",
          isSynced = true,
          createdAtMs = srvTicket?.createdAtMs ?: nowMs,
        )
        dao.upsertSupportTicket(
          SupportTicketEntity(
            ticketId = item.ticketId,
            userId = callerId,
            referenceNumber = item.referenceNumber,
            category = item.category,
            description = item.description,
            screenshotLocalPath = item.screenshotLocalPath,
            screenshotSha256 = item.screenshotSha256,
            status = item.status,
            resolutionNote = item.resolutionNote,
            isSynced = true,
            createdAtMs = item.createdAtMs,
          ),
        )
        return item
      }
    }

    // Offline-first fallback: save ticket immediately with local reference and enqueue outbox sync
    val offlineItem = SupportTicketItem(
      ticketId = opId,
      referenceNumber = localRef,
      category = cleanCat,
      description = cleanDesc,
      screenshotLocalPath = processedShot?.localFilePath,
      screenshotSha256 = sha256,
      status = "queued_offline",
      resolutionNote = "Saved offline. Will synchronize automatically when back online.",
      isSynced = false,
      createdAtMs = nowMs,
    )
    dao.upsertSupportTicket(
      SupportTicketEntity(
        ticketId = offlineItem.ticketId,
        userId = callerId,
        referenceNumber = offlineItem.referenceNumber,
        category = offlineItem.category,
        description = offlineItem.description,
        screenshotLocalPath = offlineItem.screenshotLocalPath,
        screenshotSha256 = offlineItem.screenshotSha256,
        status = offlineItem.status,
        resolutionNote = offlineItem.resolutionNote,
        isSynced = false,
        createdAtMs = offlineItem.createdAtMs,
      ),
    )
    val reqJson = moshi.adapter(ProblemReportRequestDto::class.java).toJson(
      ProblemReportRequestDto(
        clientOpId = opId,
        category = cleanCat,
        description = cleanDesc,
        screenshotMediaId = mediaId,
        screenshotSha256 = sha256,
      ),
    )
    enqueueSettingsOp(callerId, "report_problem", reqJson)
    return offlineItem
  }

  suspend fun fetchSupportTickets(): List<SupportTicketItem> {
    val session = identityStore.sessionFlow.first()
    val callerId = session.userId.orEmpty()
    if (callerId.isBlank()) return emptyList()

    val api = apiService
    val token = session.accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      flushPendingSettingsOps()
      val resp = executeWithWeakNetworkRetry { api.listSupportTickets("Bearer $token", buildDeviceFingerprint()) }
      val body = resp?.body()
      if (resp != null && resp.isSuccessful && body?.success == true) {
        val entities = body.items.map { t ->
          SupportTicketEntity(
            ticketId = t.ticketId.ifBlank { t.referenceNumber },
            userId = callerId,
            referenceNumber = t.referenceNumber,
            category = t.category,
            description = t.description,
            screenshotLocalPath = null,
            screenshotSha256 = t.screenshotSha256,
            status = t.status,
            resolutionNote = t.resolutionNote,
            isSynced = true,
            createdAtMs = t.createdAtMs,
          )
        }
        if (entities.isNotEmpty()) dao.upsertSupportTickets(entities)
      }
    }

    return dao.getSupportTickets(callerId).map { e ->
      SupportTicketItem(
        ticketId = e.ticketId,
        referenceNumber = e.referenceNumber,
        category = e.category,
        description = e.description,
        screenshotLocalPath = e.screenshotLocalPath,
        screenshotSha256 = e.screenshotSha256,
        status = e.status,
        resolutionNote = e.resolutionNote,
        isSynced = e.isSynced,
        createdAtMs = e.createdAtMs,
      )
    }
  }

  suspend fun fetchFaqs(query: String): List<FaqEntry> {
    val cleanQuery = query.trim().lowercase()
    val session = identityStore.sessionFlow.first()
    val api = apiService
    val token = session.accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val resp = runCatching { api.searchFaqs("Bearer $token", buildDeviceFingerprint(), cleanQuery) }.getOrNull()
      val items = resp?.body()?.items
      if (resp != null && resp.isSuccessful && !items.isNullOrEmpty()) {
        return items.map { FaqEntry(it.id, it.category, it.question, it.answer) }
      }
    }
    return TelefamSettingsContentCatalog.defaultFaqs.filter {
      cleanQuery.isBlank() ||
        it.question.lowercase().contains(cleanQuery) ||
        it.answer.lowercase().contains(cleanQuery) ||
        it.category.lowercase().contains(cleanQuery)
    }
  }

  suspend fun fetchSystemStatus(): List<SystemServiceStatusItem> {
    val session = identityStore.sessionFlow.first()
    val api = apiService
    val token = session.accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val resp = runCatching { api.getSystemStatus("Bearer $token", buildDeviceFingerprint()) }.getOrNull()
      val services = resp?.body()?.services
      if (resp != null && resp.isSuccessful && !services.isNullOrEmpty()) {
        return services.map { SystemServiceStatusItem(it.serviceName, it.status, it.latencyMs, it.summary) }
      }
    }
    val offline = networkTier.value == NetworkTier.OFFLINE
    return listOf(
      SystemServiceStatusItem(
        serviceName = "Device Sync & Connectivity",
        status = if (offline) "offline" else "degraded",
        latencyMs = 0,
        summary = if (offline) "No internet connection. Local encrypted cache active." else "Unable to reach server right now.",
      ),
    )
  }

  suspend fun fetchLegalDocuments(): List<LegalDocumentItem> {
    val session = identityStore.sessionFlow.first()
    val api = apiService
    val token = session.accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val resp = runCatching { api.getLegalDocuments("Bearer $token", buildDeviceFingerprint()) }.getOrNull()
      val items = resp?.body()?.items
      if (resp != null && resp.isSuccessful && !items.isNullOrEmpty()) {
        return items.map { LegalDocumentItem(it.id, it.title, it.lastUpdated, it.sections) }
      }
    }
    return TelefamSettingsContentCatalog.legalDocuments
  }

  suspend fun checkAppForUpdates(): AppVersionCheckResult {
    val nowMs = System.currentTimeMillis()
    val installedVer = BuildConfig.VERSION_NAME
    val installedCode = BuildConfig.VERSION_CODE
    val session = identityStore.sessionFlow.first()
    val api = apiService
    val token = session.accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val resp = runCatching {
        api.checkAppVersion("Bearer $token", buildDeviceFingerprint(), installedVer)
      }.getOrNull()
      val body = resp?.body()
      if (resp != null && resp.isSuccessful && body?.success == true) {
        return AppVersionCheckResult(
          installedVersion = installedVer,
          installedBuild = installedCode,
          latestVersion = body.latestVersion,
          updateAvailable = body.updateAvailable,
          releaseNotes = body.releaseNotes,
          checkedAtMs = nowMs,
        )
      }
    }
    return AppVersionCheckResult(
      installedVersion = installedVer,
      installedBuild = installedCode,
      latestVersion = installedVer,
      updateAvailable = false,
      releaseNotes = if (networkTier.value == NetworkTier.OFFLINE) "Connect to check server for newer builds." else "You are running the latest version.",
      checkedAtMs = nowMs,
    )
  }

  suspend fun flushPendingSettingsOps() {
    val session = identityStore.sessionFlow.first()
    val callerId = session.userId.orEmpty()
    val token = session.accessToken.orEmpty()
    val api = apiService ?: return
    if (callerId.isBlank() || token.isBlank() || networkTier.value == NetworkTier.OFFLINE) return

    val nowMs = System.currentTimeMillis()
    val ready = dao.getReadySettingsOps(callerId, nowMs, 25)
    for (op in ready) {
      val ok = runCatching {
        val dfp = buildDeviceFingerprint()
        when (op.opKind) {
          "privacy" -> {
            val dto = moshi.adapter(PrivacySettingsDto::class.java).fromJson(op.payloadJson) ?: return@runCatching true
            api.updatePrivacySettings("Bearer $token", dfp, dto).isSuccessful
          }
          "notifications" -> {
            val dto = moshi.adapter(NotificationSettingsDto::class.java).fromJson(op.payloadJson) ?: return@runCatching true
            api.updateNotificationSettings("Bearer $token", dfp, dto).isSuccessful
          }
          "content_prefs" -> {
            val dto = moshi.adapter(ContentPreferencesDto::class.java).fromJson(op.payloadJson) ?: return@runCatching true
            api.updateContentPreferences("Bearer $token", dfp, dto).isSuccessful
          }
          "chat_privacy" -> {
            val dto = moshi.adapter(ChatPrivacySettingsDto::class.java).fromJson(op.payloadJson) ?: return@runCatching true
            api.updateChatPrivacy("Bearer $token", dfp, dto).isSuccessful
          }
          "sync_prefs" -> {
            val dto = moshi.adapter(SyncPreferencesDto::class.java).fromJson(op.payloadJson) ?: return@runCatching true
            val resp = api.updateSyncSettings("Bearer $token", dfp, dto)
            val srv = resp.body()?.syncPreferences
            if (resp.isSuccessful && srv != null) {
              persistSyncPreferences(callerId, mapSyncPrefsDtoToState(srv))
            }
            resp.isSuccessful
          }
          "chat_theme" -> {
            val dto = moshi.adapter(UpdateChatThemeRequestDto::class.java).fromJson(op.payloadJson) ?: return@runCatching true
            api.updateChatTheme("Bearer $token", dfp, dto).isSuccessful
          }
          "mute" -> api.muteUser("Bearer $token", dfp, op.payloadJson).isSuccessful
          "unmute" -> api.unmuteUser("Bearer $token", dfp, op.payloadJson).isSuccessful
          "block" -> api.blockUser("Bearer $token", dfp, op.payloadJson).isSuccessful
          "unblock" -> api.unblockUser("Bearer $token", dfp, op.payloadJson).isSuccessful
          "report_problem" -> {
            val dto = moshi.adapter(ProblemReportRequestDto::class.java).fromJson(op.payloadJson) ?: return@runCatching true
            val resp = api.submitProblemReport("Bearer $token", dfp, dto)
            val body = resp.body()
            if (resp.isSuccessful && body?.success == true && body.ticket != null) {
              val t = body.ticket
              dao.upsertSupportTicket(
                SupportTicketEntity(
                  ticketId = t.ticketId.ifBlank { dto.clientOpId },
                  userId = callerId,
                  referenceNumber = body.referenceNumber,
                  category = t.category,
                  description = t.description,
                  screenshotLocalPath = null,
                  screenshotSha256 = t.screenshotSha256,
                  status = t.status,
                  resolutionNote = t.resolutionNote,
                  isSynced = true,
                  createdAtMs = t.createdAtMs,
                ),
              )
            }
            resp.isSuccessful
          }
          else -> true
        }
      }.getOrDefault(false)

      if (ok) {
        dao.deleteSettingsOp(op.opId)
      } else {
        val nextAttempt = op.retryCount + 1
        dao.scheduleSettingsOpRetry(op.opId, nextAttempt, System.currentTimeMillis() + (nextAttempt * 5_000L).coerceAtMost(60_000L))
      }
    }
  }

  private suspend fun enqueueSettingsOp(userId: String, kind: String, payloadJson: String) {
    val nowMs = System.currentTimeMillis()
    dao.enqueueSettingsOp(
      PendingSettingsOpEntity(
        opId = "${kind}_${UUID.randomUUID()}",
        userId = userId,
        opKind = kind,
        payloadJson = payloadJson,
        createdAtMs = nowMs,
        retryCount = 0,
        nextRetryAtMs = nowMs,
      ),
    )
  }

  private suspend fun persistSettingsSnapshot(userId: String, snapshot: SettingsSnapshot) {
    val pJson = moshi.adapter(PrivacySettingsDto::class.java).toJson(mapPrivacyStateToDto(snapshot.privacy))
    val nJson = moshi.adapter(NotificationSettingsDto::class.java).toJson(mapNotificationStateToDto(snapshot.notifications))
    val cJson = moshi.adapter(ContentPreferencesDto::class.java).toJson(mapContentPrefsStateToDto(snapshot.contentPreferences, false))
    dao.upsertCachedSettings(
      CachedSettingsEntity(
        userId = userId,
        privacyJson = pJson,
        notificationsJson = nJson,
        contentPrefsJson = cJson,
        updatedAtMs = System.currentTimeMillis(),
      ),
    )
  }

  private fun mapEntityToSettingsSnapshot(entity: CachedSettingsEntity): SettingsSnapshot {
    val pDto = runCatching { moshi.adapter(PrivacySettingsDto::class.java).fromJson(entity.privacyJson) }.getOrNull()
    val nDto = runCatching { moshi.adapter(NotificationSettingsDto::class.java).fromJson(entity.notificationsJson) }.getOrNull()
    val cDto = runCatching { moshi.adapter(ContentPreferencesDto::class.java).fromJson(entity.contentPrefsJson) }.getOrNull()
    return SettingsSnapshot(
      privacy = pDto?.let(::mapPrivacyDtoToState) ?: PrivacySettingsState(),
      notifications = nDto?.let(::mapNotificationDtoToState) ?: NotificationSettingsState(),
      contentPreferences = cDto?.let(::mapContentPrefsDtoToState) ?: ContentPreferencesState(),
    )
  }

  private fun mapPrivacyDtoToState(dto: PrivacySettingsDto) = PrivacySettingsState(
    isPrivateAccount = dto.isPrivateAccount,
    whoCanComment = AudienceOption.fromApi(dto.whoCanComment),
    whoCanMention = AudienceOption.fromApi(dto.whoCanMention),
    whoCanFindYou = DiscoverabilityOption.fromApi(dto.whoCanFindYou),
    showActivityStatus = dto.showActivityStatus,
    likesVisibility = VisibilityOption.fromApi(dto.likesVisibility),
    followingVisibility = VisibilityOption.fromApi(dto.followingVisibility),
    dataSharingPersonalization = dto.dataSharingPersonalization,
    analyticsSharing = dto.analyticsSharing,
    essentialCookies = dto.essentialCookies,
    personalizationCookies = dto.personalizationCookies,
    updatedAtMs = dto.updatedAtMs,
  )

  private fun mapPrivacyStateToDto(s: PrivacySettingsState) = PrivacySettingsDto(
    isPrivateAccount = s.isPrivateAccount,
    whoCanComment = s.whoCanComment.apiValue,
    whoCanMention = s.whoCanMention.apiValue,
    whoCanFindYou = s.whoCanFindYou.apiValue,
    showActivityStatus = s.showActivityStatus,
    likesVisibility = s.likesVisibility.apiValue,
    followingVisibility = s.followingVisibility.apiValue,
    dataSharingPersonalization = s.dataSharingPersonalization,
    analyticsSharing = s.analyticsSharing,
    essentialCookies = true,
    personalizationCookies = s.personalizationCookies,
    updatedAtMs = s.updatedAtMs,
  )

  private fun mapNotificationDtoToState(dto: NotificationSettingsDto) = NotificationSettingsState(
    masterPause = dto.masterPause,
    channelPush = dto.channelPush,
    channelEmail = dto.channelEmail,
    channelSms = dto.channelSms,
    typeMessages = dto.typeMessages,
    typeComments = dto.typeComments,
    typeFollowers = dto.typeFollowers,
    typeProductUpdates = dto.typeProductUpdates,
    quietHoursEnabled = dto.quietHoursEnabled,
    quietStartMinutes = dto.quietStartMinutes,
    quietEndMinutes = dto.quietEndMinutes,
    updatedAtMs = dto.updatedAtMs,
  )

  private fun mapNotificationStateToDto(s: NotificationSettingsState) = NotificationSettingsDto(
    masterPause = s.masterPause,
    channelPush = s.channelPush,
    channelEmail = s.channelEmail,
    channelSms = s.channelSms,
    typeMessages = s.typeMessages,
    typeComments = s.typeComments,
    typeFollowers = s.typeFollowers,
    typeProductUpdates = s.typeProductUpdates,
    quietHoursEnabled = s.quietHoursEnabled,
    quietStartMinutes = s.quietStartMinutes,
    quietEndMinutes = s.quietEndMinutes,
    updatedAtMs = s.updatedAtMs,
  )

  private fun mapContentPrefsDtoToState(dto: ContentPreferencesDto) = ContentPreferencesState(
    suggestedContentEnabled = dto.suggestedContentEnabled,
    notInterestedTopics = dto.notInterestedTopics,
    language = AppLanguageOption.fromCode(dto.languageCode),
    autoplayVideos = dto.autoplayVideos,
    videoQuality = VideoQualityOption.fromApi(dto.videoQuality),
    dataSaver = dto.dataSaver,
    captionsEnabled = dto.captionsEnabled,
    reducedMotion = dto.reducedMotion,
    updatedAtMs = dto.updatedAtMs,
  )

  private fun mapContentPrefsStateToDto(s: ContentPreferencesState, resetSuggestions: Boolean) = ContentPreferencesDto(
    suggestedContentEnabled = s.suggestedContentEnabled,
    notInterestedTopics = s.notInterestedTopics,
    languageCode = s.language.code,
    autoplayVideos = s.autoplayVideos,
    videoQuality = s.videoQuality.apiValue,
    dataSaver = s.dataSaver,
    captionsEnabled = s.captionsEnabled,
    reducedMotion = s.reducedMotion,
    resetSuggestions = resetSuggestions,
    updatedAtMs = s.updatedAtMs,
  )

  private suspend fun loadCachedSyncPreferences(userId: String): SyncPreferencesState {
    val raw = identityStore.getEncryptedSyncPrefsJson(userId) ?: return SyncPreferencesState()
    val dto = runCatching { moshi.adapter(SyncPreferencesDto::class.java).fromJson(raw) }.getOrNull()
      ?: return SyncPreferencesState()
    return mapSyncPrefsDtoToState(dto)
  }

  private suspend fun persistSyncPreferences(userId: String, s: SyncPreferencesState) {
    val json = moshi.adapter(SyncPreferencesDto::class.java).toJson(mapSyncPrefsStateToDto(s))
    identityStore.saveEncryptedSyncPrefsJson(userId, json)
  }

  private fun mapSyncPrefsDtoToState(dto: SyncPreferencesDto) = SyncPreferencesState(
    autoSyncEnabled = dto.autoSyncEnabled,
    syncOverWifiOnly = dto.syncOverWifiOnly,
    backgroundSyncEnabled = dto.backgroundSyncEnabled,
    cacheFeedsOffline = dto.cacheFeedsOffline,
    cacheChatMediaOffline = dto.cacheChatMediaOffline,
    lastSyncedAtMs = dto.lastSyncedAtMs,
    lastSyncStatus = dto.lastSyncStatus.ifBlank { "idle" },
    totalSyncedOps = dto.totalSyncedOps,
    updatedAtMs = dto.updatedAtMs,
  )

  private fun mapSyncPrefsStateToDto(s: SyncPreferencesState) = SyncPreferencesDto(
    autoSyncEnabled = s.autoSyncEnabled,
    syncOverWifiOnly = s.syncOverWifiOnly,
    backgroundSyncEnabled = s.backgroundSyncEnabled,
    cacheFeedsOffline = s.cacheFeedsOffline,
    cacheChatMediaOffline = s.cacheChatMediaOffline,
    lastSyncedAtMs = s.lastSyncedAtMs,
    lastSyncStatus = s.lastSyncStatus,
    totalSyncedOps = s.totalSyncedOps,
    updatedAtMs = s.updatedAtMs,
  )

  private fun mapChatPrivacyDtoToState(dto: ChatPrivacySettingsDto) = ChatPrivacySettingsState(
    messageRequests = ChatAudienceOption.fromApi(dto.messageRequests, allowContacts = true, fallback = ChatAudienceOption.ANYONE),
    whoCanCall = ChatAudienceOption.fromApi(dto.whoCanCall, allowContacts = true, fallback = ChatAudienceOption.ANYONE),
    whoCanScreenshot = ChatAudienceOption.fromApi(dto.whoCanScreenshot, allowContacts = false, fallback = ChatAudienceOption.NOBODY),
    whoCanShareChats = ChatAudienceOption.fromApi(dto.whoCanShareChats, allowContacts = false, fallback = ChatAudienceOption.NOBODY),
    whoCanCopyMessages = ChatAudienceOption.fromApi(dto.whoCanCopyMessages, allowContacts = false, fallback = ChatAudienceOption.NOBODY),
    whoCanDownloadMedia = ChatAudienceOption.fromApi(dto.whoCanDownloadMedia, allowContacts = false, fallback = ChatAudienceOption.ANYONE),
    chatThemeId = TelefamChatPrivacyCatalog.themeById(dto.chatThemeId).themeId,
    bubbleColorId = TelefamChatPrivacyCatalog.bubbleColorById(dto.bubbleColorId).colorId,
    readReceipts = ChatAudienceOption.fromApi(dto.readReceipts, allowContacts = false, fallback = ChatAudienceOption.ANYONE),
    typingIndicators = ChatAudienceOption.fromApi(dto.typingIndicators, allowContacts = false, fallback = ChatAudienceOption.NOBODY),
    onlineIndicators = ChatAudienceOption.fromApi(dto.onlineIndicators, allowContacts = false, fallback = ChatAudienceOption.NOBODY),
    updatedAtMs = dto.updatedAtMs,
  )

  private fun mapChatPrivacyStateToDto(s: ChatPrivacySettingsState) = ChatPrivacySettingsDto(
    messageRequests = s.messageRequests.apiValue,
    whoCanCall = s.whoCanCall.apiValue,
    whoCanScreenshot = s.whoCanScreenshot.apiValue,
    whoCanShareChats = s.whoCanShareChats.apiValue,
    whoCanCopyMessages = s.whoCanCopyMessages.apiValue,
    whoCanDownloadMedia = s.whoCanDownloadMedia.apiValue,
    chatThemeId = TelefamChatPrivacyCatalog.themeById(s.chatThemeId).themeId,
    bubbleColorId = TelefamChatPrivacyCatalog.bubbleColorById(s.bubbleColorId).colorId,
    readReceipts = s.readReceipts.apiValue,
    typingIndicators = s.typingIndicators.apiValue,
    onlineIndicators = s.onlineIndicators.apiValue,
    updatedAtMs = s.updatedAtMs,
  )

  private fun mapChatPrivacyStateToEntity(userId: String, s: ChatPrivacySettingsState) = CachedChatPrivacyEntity(
    userId = userId,
    messageRequests = s.messageRequests.apiValue,
    whoCanCall = s.whoCanCall.apiValue,
    whoCanScreenshot = s.whoCanScreenshot.apiValue,
    whoCanShareChats = s.whoCanShareChats.apiValue,
    whoCanCopyMessages = s.whoCanCopyMessages.apiValue,
    whoCanDownloadMedia = s.whoCanDownloadMedia.apiValue,
    chatThemeId = s.chatThemeId,
    bubbleColorId = s.bubbleColorId,
    readReceipts = s.readReceipts.apiValue,
    typingIndicators = s.typingIndicators.apiValue,
    onlineIndicators = s.onlineIndicators.apiValue,
    updatedAtMs = s.updatedAtMs,
  )

  private fun mapChatPrivacyEntityToState(e: CachedChatPrivacyEntity) = ChatPrivacySettingsState(
    messageRequests = ChatAudienceOption.fromApi(e.messageRequests, allowContacts = true, fallback = ChatAudienceOption.ANYONE),
    whoCanCall = ChatAudienceOption.fromApi(e.whoCanCall, allowContacts = true, fallback = ChatAudienceOption.ANYONE),
    whoCanScreenshot = ChatAudienceOption.fromApi(e.whoCanScreenshot, allowContacts = false, fallback = ChatAudienceOption.NOBODY),
    whoCanShareChats = ChatAudienceOption.fromApi(e.whoCanShareChats, allowContacts = false, fallback = ChatAudienceOption.NOBODY),
    whoCanCopyMessages = ChatAudienceOption.fromApi(e.whoCanCopyMessages, allowContacts = false, fallback = ChatAudienceOption.NOBODY),
    whoCanDownloadMedia = ChatAudienceOption.fromApi(e.whoCanDownloadMedia, allowContacts = false, fallback = ChatAudienceOption.ANYONE),
    chatThemeId = TelefamChatPrivacyCatalog.themeById(e.chatThemeId).themeId,
    bubbleColorId = TelefamChatPrivacyCatalog.bubbleColorById(e.bubbleColorId).colorId,
    readReceipts = ChatAudienceOption.fromApi(e.readReceipts, allowContacts = false, fallback = ChatAudienceOption.ANYONE),
    typingIndicators = ChatAudienceOption.fromApi(e.typingIndicators, allowContacts = false, fallback = ChatAudienceOption.NOBODY),
    onlineIndicators = ChatAudienceOption.fromApi(e.onlineIndicators, allowContacts = false, fallback = ChatAudienceOption.NOBODY),
    updatedAtMs = e.updatedAtMs,
  )

  private suspend fun completeSignIn(
    userId: String,
    email: String,
    accessToken: String,
    refreshToken: String,
    accessExpSec: Long,
    refreshExpSec: Long,
    compositeKey: String,
    profileCompleted: Boolean,
  ) {
    val nowMs = System.currentTimeMillis()
    val deviceFp = buildDeviceFingerprint()
    dao.clearRateLimitState(compositeKey)
    dao.insertRefreshTokenRecord(
      RefreshTokenRecordEntity(
        tokenHash = EncryptedTokenVault.sha256Hex(refreshToken),
        familyId = UUID.randomUUID().toString(),
        userId = userId,
        deviceFp = deviceFp,
        expiresAtMs = nowMs + (refreshExpSec * 1000L),
        revoked = false,
        createdAtMs = nowMs,
      ),
    )
    identityStore.saveEncryptedSession(
      userId = userId,
      email = email,
      accessToken = accessToken,
      refreshToken = refreshToken,
      accessExpiresInSec = accessExpSec,
      refreshExpiresInSec = refreshExpSec,
      profileCompleted = profileCompleted,
    )
    logAudit(userId, "auth_session_issued", "{\"profile_completed\":$profileCompleted}")
  }

  @SuppressLint("HardwareIds")
  private suspend fun buildDeviceFingerprint(): String {
    val androidId = runCatching {
      Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
    }.getOrNull().orEmpty().trim()
    val stableSeed = if (androidId.isNotBlank() && androidId != "9774d56d682e549c") {
      "aid_${appContext.packageName}_$androidId"
    } else {
      identityStore.getOrCreateInstallId()
    }
    val integrity = SecurityIntegrityGuard.inspect(appContext)
    val integrityFlags = "${integrity.signatureDigest.take(8)}_${if (integrity.isTampered) 1 else 0}${if (integrity.hasRootBinary) 1 else 0}${if (integrity.hasHookFramework) 1 else 0}${if (integrity.isDebuggerAttached) 1 else 0}"
    return TelefamCoreEngine.computeDeviceFingerprint(
      platform = "android",
      model = "${Build.MANUFACTURER}_${Build.MODEL}_$integrityFlags",
      osVersion = Build.VERSION.RELEASE ?: "unknown",
      installId = stableSeed,
    )
  }

  private fun buildIpFingerprint(): String {
    return TelefamCoreEngine.computeIpFingerprint(networkMonitor.currentIpAddressHint())
  }

  private suspend fun <T> executeWithWeakNetworkRetry(block: suspend () -> T): T? {
    val maxAttempts = if (networkTier.value == NetworkTier.WEAK) 3 else 2
    var attempt = 0
    while (attempt < maxAttempts) {
      val result = runCatching { block() }.getOrNull()
      if (result != null) return result
      attempt++
      if (attempt < maxAttempts) {
        delay(TelefamCoreEngine.computeBackoffMs(attempt, networkTier.value))
      }
    }
    return null
  }

  // Link Previews & Saved Links
  suspend fun getCachedLinkPreview(rawUrl: String): CachedLinkPreviewEntity? {
    val normalized = SmartTextDetector.normalizeSafeHttpUrl(rawUrl) ?: return null
    return dao.getCachedLinkPreview(normalized)
  }

  suspend fun fetchLinkPreview(rawUrl: String): CachedLinkPreviewEntity? {
    val normalized = SmartTextDetector.normalizeSafeHttpUrl(rawUrl) ?: return null
    val domain = SmartTextDetector.extractCleanDomain(normalized)
    val nowMs = System.currentTimeMillis()
    val cached = dao.getCachedLinkPreview(normalized)
    if (cached != null) {
      val ttlMs = if (cached.status == "ok") 24 * 60 * 60 * 1000L else 5 * 60 * 1000L
      if (nowMs - cached.fetchedAtMs < ttlMs) {
        return cached
      }
    }

    val fallback = CachedLinkPreviewEntity(
      normalizedUrl = normalized,
      title = domain,
      description = normalized,
      imageUrl = "",
      faviconUrl = "",
      domain = domain,
      status = "fallback",
      fetchedAtMs = nowMs,
    )

    val api = apiService
    val session = identityStore.sessionFlow.first()
    val token = session.accessToken.orEmpty()
    if (api == null || token.isBlank() || networkTier.value == NetworkTier.OFFLINE) {
      return cached ?: fallback
    }

    val resp = executeWithWeakNetworkRetry {
      api.getLinkPreview("Bearer $token", buildDeviceFingerprint(), normalized)
    }
    val dto = resp?.body()?.takeIf { resp.isSuccessful && it.success }?.preview
    if (dto != null) {
      val safeImage = SmartTextDetector.normalizeSafeHttpUrl(dto.imageUrl).orEmpty()
      val safeFavicon = SmartTextDetector.normalizeSafeHttpUrl(dto.faviconUrl).orEmpty()
      val entity = CachedLinkPreviewEntity(
        normalizedUrl = SmartTextDetector.normalizeSafeHttpUrl(dto.normalizedUrl) ?: normalized,
        title = dto.title.ifBlank { domain },
        description = dto.description,
        imageUrl = safeImage,
        faviconUrl = safeFavicon,
        domain = dto.domain.ifBlank { domain },
        status = if (dto.status == "ok") "ok" else "fallback",
        fetchedAtMs = if (dto.fetchedAtMs > 0L) dto.fetchedAtMs else nowMs,
      )
      dao.upsertCachedLinkPreview(entity)
      return entity
    }

    dao.upsertCachedLinkPreview(fallback)
    return cached ?: fallback
  }

  fun observeSavedLinks(userId: String, query: String = ""): Flow<List<SavedLinkEntity>> {
    return dao.observeSavedLinks(userId, query.trim())
  }

  suspend fun refreshSavedLinks(query: String = "") {
    val session = identityStore.sessionFlow.first()
    val callerId = session.userId.orEmpty()
    val token = session.accessToken.orEmpty()
    val api = apiService
    if (callerId.isBlank() || token.isBlank() || api == null || networkTier.value == NetworkTier.OFFLINE) {
      return
    }
    val resp = executeWithWeakNetworkRetry {
      api.listSavedLinks("Bearer $token", buildDeviceFingerprint(), query.trim())
    }
    val body = resp?.body()
    if (resp != null && resp.isSuccessful && body?.success == true) {
      val entities = body.items.mapNotNull { item ->
        val safeUrl = SmartTextDetector.normalizeSafeHttpUrl(item.normalizedUrl.ifBlank { item.url })
          ?: return@mapNotNull null
        val domain = item.domain.ifBlank { SmartTextDetector.extractCleanDomain(safeUrl) }
        SavedLinkEntity(
          id = item.id.ifBlank { EncryptedTokenVault.sha256Hex("$callerId|$safeUrl").take(24) },
          userId = callerId,
          url = safeUrl,
          normalizedUrl = safeUrl,
          title = item.title.ifBlank { domain },
          description = item.description,
          imageUrl = SmartTextDetector.normalizeSafeHttpUrl(item.imageUrl).orEmpty(),
          faviconUrl = SmartTextDetector.normalizeSafeHttpUrl(item.faviconUrl).orEmpty(),
          domain = domain,
          source = item.source.ifBlank { "browser" },
          savedAtMs = if (item.savedAtMs > 0L) item.savedAtMs else System.currentTimeMillis(),
        )
      }
      if (entities.isNotEmpty()) {
        dao.upsertSavedLinks(entities)
      }
    }
  }

  suspend fun saveLink(
    rawUrl: String,
    title: String = "",
    description: String = "",
    imageUrl: String = "",
    faviconUrl: String = "",
    domain: String = "",
    source: String = "browser",
  ): SavedLinkEntity? {
    val session = identityStore.sessionFlow.first()
    val callerId = session.userId.orEmpty()
    if (callerId.isBlank()) return null
    val normalized = SmartTextDetector.normalizeSafeHttpUrl(rawUrl) ?: return null
    val cleanDomain = domain.ifBlank { SmartTextDetector.extractCleanDomain(normalized) }
    val cleanTitle = TelefamProfileSanitizer.sanitizePlainText(title, 180, singleLine = true).ifBlank { cleanDomain }
    val cleanDesc = TelefamProfileSanitizer.sanitizePlainText(description, 320, singleLine = true)
    val safeImage = SmartTextDetector.normalizeSafeHttpUrl(imageUrl).orEmpty()
    val safeFavicon = SmartTextDetector.normalizeSafeHttpUrl(faviconUrl).orEmpty()
    val existing = dao.getSavedLinkByUrl(callerId, normalized)
    val localId = existing?.id ?: "lnk_${EncryptedTokenVault.sha256Hex("$callerId|$normalized").take(16)}"
    val nowMs = System.currentTimeMillis()

    val localEntity = SavedLinkEntity(
      id = localId,
      userId = callerId,
      url = normalized,
      normalizedUrl = normalized,
      title = cleanTitle,
      description = cleanDesc,
      imageUrl = safeImage,
      faviconUrl = safeFavicon,
      domain = cleanDomain,
      source = source,
      savedAtMs = nowMs,
    )
    dao.upsertSavedLink(localEntity)

    val api = apiService
    val token = session.accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      val resp = executeWithWeakNetworkRetry {
        api.saveLink(
          "Bearer $token",
          buildDeviceFingerprint(),
          SaveLinkRequestDto(
            url = normalized,
            title = cleanTitle,
            description = cleanDesc,
            imageUrl = safeImage,
            faviconUrl = safeFavicon,
            domain = cleanDomain,
            source = source,
          ),
        )
      }
      val srv = resp?.body()?.takeIf { resp.isSuccessful && it.success }?.item
      if (srv != null && srv.id.isNotBlank()) {
        if (srv.id != localId) {
          dao.deleteSavedLinkById(callerId, localId)
        }
        val synced = localEntity.copy(
          id = srv.id,
          title = srv.title.ifBlank { cleanTitle },
          description = srv.description.ifBlank { cleanDesc },
          imageUrl = SmartTextDetector.normalizeSafeHttpUrl(srv.imageUrl) ?: safeImage,
          faviconUrl = SmartTextDetector.normalizeSafeHttpUrl(srv.faviconUrl) ?: safeFavicon,
          savedAtMs = if (srv.savedAtMs > 0L) srv.savedAtMs else nowMs,
        )
        dao.upsertSavedLink(synced)
        return synced
      }
    }
    return localEntity
  }

  suspend fun removeSavedLinkById(id: String) {
    val session = identityStore.sessionFlow.first()
    val callerId = session.userId.orEmpty()
    if (callerId.isBlank() || id.isBlank()) return
    dao.deleteSavedLinkById(callerId, id)
    val api = apiService
    val token = session.accessToken.orEmpty()
    if (api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      runCatching { api.deleteSavedLink("Bearer $token", buildDeviceFingerprint(), id) }
    }
  }

  suspend fun removeSavedLinkByUrl(rawUrl: String) {
    val session = identityStore.sessionFlow.first()
    val callerId = session.userId.orEmpty()
    val normalized = SmartTextDetector.normalizeSafeHttpUrl(rawUrl) ?: return
    if (callerId.isBlank()) return
    val existing = dao.getSavedLinkByUrl(callerId, normalized)
    dao.deleteSavedLinkByUrl(callerId, normalized)
    val api = apiService
    val token = session.accessToken.orEmpty()
    if (existing != null && api != null && token.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      runCatching { api.deleteSavedLink("Bearer $token", buildDeviceFingerprint(), existing.id) }
    }
  }

  // Push Notifications, FCM Token Registration & Notification Inbox
  fun observeNotifications(userId: String): Flow<List<CachedNotificationEntity>> =
    dao.observeNotifications(userId)

  fun observeUnreadNotificationCount(userId: String): Flow<Int> =
    dao.observeUnreadNotificationCount(userId)

  suspend fun upsertLocalNotification(item: CachedNotificationEntity) {
    dao.upsertNotification(item)
  }

  suspend fun deleteNotificationsByMessageIdLocal(messageId: String) {
    val uid = identityStore.sessionFlow.first().userId.orEmpty()
    if (uid.isBlank() || messageId.isBlank()) return
    dao.deleteNotificationsByMessageId(uid, messageId)
  }

  suspend fun markConversationNotificationsReadLocal(conversationId: String) {
    val uid = identityStore.sessionFlow.first().userId.orEmpty()
    if (uid.isBlank() || conversationId.isBlank()) return
    dao.markConversationNotificationsRead(uid, conversationId)
  }

  fun registerPushTokenAsync(fcmToken: String) {
    telemetryScope.launch {
      registerPushToken(fcmToken)
    }
  }

  suspend fun registerPushToken(fcmToken: String): Boolean {
    val cleanToken = fcmToken.trim()
    if (cleanToken.isBlank()) return false
    val session = identityStore.sessionFlow.first()
    val access = session.accessToken.orEmpty()
    val uid = session.userId.orEmpty()
    val api = apiService ?: return false
    if (access.isBlank() || uid.isBlank() || networkTier.value == NetworkTier.OFFLINE) return false

    val resp = executeWithWeakNetworkRetry {
      api.registerPushToken(
        "Bearer $access",
        buildDeviceFingerprint(),
        RegisterPushTokenRequestDto(
          platform = "android",
          token = cleanToken,
          appBundle = appContext.packageName,
        ),
      )
    }
    return resp?.isSuccessful == true && resp.body()?.success == true
  }

  suspend fun syncFcmTokenIfAuthenticated() {
    val session = identityStore.sessionFlow.first()
    if (session.userId.isNullOrBlank() || session.accessToken.isNullOrBlank()) return
    TelefamNotificationManager.ensureChannels(appContext)
    TelefamNotificationManager.fetchAndRegisterFcmTokenIfAvailable(appContext) { token ->
      registerPushTokenAsync(token)
    }
  }

  suspend fun refreshNotifications(limit: Int = 60): List<CachedNotificationEntity> {
    val session = identityStore.sessionFlow.first()
    val uid = session.userId.orEmpty()
    val access = session.accessToken.orEmpty()
    val api = apiService ?: return emptyList()
    if (uid.isBlank() || access.isBlank() || networkTier.value == NetworkTier.OFFLINE) return emptyList()

    val resp = executeWithWeakNetworkRetry {
      api.listNotifications("Bearer $access", buildDeviceFingerprint(), limit = limit)
    }
    val body = resp?.body() ?: return emptyList()
    if (!resp.isSuccessful || !body.success) return emptyList()

    val entities = body.items.map { dto ->
      val safeAvatar = dto.actorAvatarUrl.takeIf { it.isNotBlank() }?.let {
        AdaptiveStreamSelector.resolveMediaUrl(BuildConfig.TELEFAM_API_BASE_URL, it)
      }.orEmpty()
      val safeBody = if (dto.isViewOnce) {
        "Sent a view-once voice message"
      } else {
        dto.body
      }
      CachedNotificationEntity(
        id = dto.id,
        userId = uid,
        actorUserId = dto.actorUserId,
        actorName = dto.actorName,
        actorUsername = dto.actorUsername,
        actorAvatarUrl = safeAvatar,
        kind = dto.kind,
        title = dto.title,
        body = safeBody,
        conversationId = dto.conversationId,
        messageId = dto.messageId,
        postId = dto.postId,
        targetUserId = dto.targetUserId,
        isViewOnce = dto.isViewOnce,
        isRead = dto.isRead,
        createdAtMs = dto.createdAtMs,
      )
    }
    if (entities.isNotEmpty()) {
      dao.upsertNotifications(entities)
    }
    return entities
  }

  suspend fun markNotificationRead(notificationId: String) {
    val session = identityStore.sessionFlow.first()
    val uid = session.userId.orEmpty()
    val access = session.accessToken.orEmpty()
    if (uid.isBlank() || notificationId.isBlank()) return
    dao.markNotificationRead(uid, notificationId)
    TelefamNotificationManager.cancelSocialNotification(appContext, notificationId)
    val api = apiService ?: return
    if (access.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      runCatching {
        api.markNotificationRead(
          "Bearer $access",
          buildDeviceFingerprint(),
          MarkNotificationReadRequestDto(notificationId = notificationId),
        )
      }
    }
  }

  suspend fun markAllNotificationsRead() {
    val session = identityStore.sessionFlow.first()
    val uid = session.userId.orEmpty()
    val access = session.accessToken.orEmpty()
    if (uid.isBlank()) return
    dao.markAllNotificationsRead(uid)
    TelefamNotificationManager.cancelAllNotifications(appContext)
    val api = apiService ?: return
    if (access.isNotBlank() && networkTier.value != NetworkTier.OFFLINE) {
      runCatching {
        api.markAllNotificationsRead("Bearer $access", buildDeviceFingerprint())
      }
    }
  }
}
