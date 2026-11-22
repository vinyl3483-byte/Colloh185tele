package telefam.company.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import com.telefam.company.app.BuildConfig
import java.net.URI
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class RequestOtpDto(val email: String)

@JsonClass(generateAdapter = true)
data class EmailCheckResponseDto(
  val success: Boolean = false,
  val exists: Boolean = false,
  @Json(name = "profile_completed") val profileCompleted: Boolean = false,
  @Json(name = "requires_device_approval") val requiresDeviceApproval: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class RequestOtpResponseDto(
  val success: Boolean,
  @Json(name = "cooldown_seconds") val cooldownSeconds: Long = 60L,
  @Json(name = "requires_device_approval") val requiresDeviceApproval: Boolean = false,
  @Json(name = "request_id") val requestId: String = "",
  @Json(name = "poll_token") val pollToken: String = "",
  @Json(name = "target_device_name") val targetDeviceName: String = "",
  @Json(name = "target_location") val targetLocation: String = "",
  @Json(name = "request_time_ms") val requestTimeMs: Long = 0L,
  @Json(name = "expires_in_seconds") val expiresInSeconds: Long = 60L,
  @Json(name = "try_another_way_provider") val tryAnotherWayProvider: String = "google",
)

@JsonClass(generateAdapter = true)
data class LoginApprovalStatusResponseDto(
  val success: Boolean = false,
  val status: String = "pending",
  val blocked: Boolean = false,
  @Json(name = "remaining_seconds") val remainingSeconds: Long = 0L,
  @Json(name = "target_device_name") val targetDeviceName: String = "",
  @Json(name = "target_location") val targetLocation: String = "",
  @Json(name = "user_id") val userId: String = "",
  @Json(name = "session_token") val sessionToken: String = "",
  @Json(name = "refresh_token") val refreshToken: String = "",
  @Json(name = "access_expires_in_seconds") val accessExpiresInSec: Long = 900L,
  @Json(name = "refresh_expires_in_seconds") val refreshExpiresInSec: Long = 2_592_000L,
  val email: String = "",
  @Json(name = "suggested_full_name") val suggestedFullName: String = "",
  @Json(name = "suggested_username") val suggestedUsername: String = "",
  @Json(name = "profile_completed") val profileCompleted: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class PendingLoginRequestItemDto(
  @Json(name = "request_id") val requestId: String = "",
  @Json(name = "device_name") val deviceName: String = "",
  val location: String = "",
  @Json(name = "ip_address_masked") val ipAddressMasked: String = "",
  @Json(name = "requested_at_ms") val requestedAtMs: Long = 0L,
  @Json(name = "expires_at_ms") val expiresAtMs: Long = 0L,
  @Json(name = "remaining_seconds") val remainingSeconds: Long = 60L,
)

@JsonClass(generateAdapter = true)
data class PendingLoginRequestsResponseDto(
  val success: Boolean = false,
  val requests: List<PendingLoginRequestItemDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class RespondLoginRequestDto(
  val action: String, // "accept" or "deny"
)

@JsonClass(generateAdapter = true)
data class RespondLoginRequestResponseDto(
  val success: Boolean = false,
  @Json(name = "request_id") val requestId: String = "",
  val status: String = "",
  @Json(name = "device_name") val deviceName: String = "",
  val location: String = "",
  @Json(name = "requested_at_ms") val requestedAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class DeviceContextTelemetryDto(
  @Json(name = "device_name") val deviceName: String = "",
  val platform: String = "android",
  @Json(name = "hardware_model") val hardwareModel: String = "",
  @Json(name = "carrier_country_iso") val carrierCountryIso: String = "",
  @Json(name = "sim_country_iso") val simCountryIso: String = "",
  @Json(name = "gps_country_iso") val gpsCountryIso: String = "",
  @Json(name = "gps_city") val gpsCity: String = "",
  @Json(name = "locale_country_iso") val localeCountryIso: String = "",
  val timezone: String = "",
  @Json(name = "lat_bucket") val latBucket: Double = 0.0,
  @Json(name = "lon_bucket") val lonBucket: Double = 0.0,
  @Json(name = "signal_source") val signalSource: String = "",
  @Json(name = "captured_at_ms") val capturedAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class DeviceContextTelemetryResponseDto(
  val success: Boolean = false,
  @Json(name = "effective_country_iso") val effectiveCountryIso: String = "",
  @Json(name = "monetization_country") val monetizationCountry: String = "",
  @Json(name = "server_epoch_ms") val serverEpochMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class VerifyOtpDto(val email: String, val otp: String)

@JsonClass(generateAdapter = true)
data class VerifyLinkDto(val token: String)

@JsonClass(generateAdapter = true)
data class RefreshTokenDto(@Json(name = "refresh_token") val refreshToken: String)

@JsonClass(generateAdapter = true)
data class OAuthDto(
  val provider: String,
  @Json(name = "identity_token") val identityToken: String,
  val nonce: String? = null,
  @Json(name = "expected_email") val expectedEmail: String? = null,
  @Json(name = "approval_request_id") val approvalRequestId: String? = null,
)

@JsonClass(generateAdapter = true)
data class AuthSessionResponseDto(
  val success: Boolean,
  @Json(name = "user_id") val userId: String = "",
  @Json(name = "session_token") val sessionToken: String = "",
  @Json(name = "refresh_token") val refreshToken: String = "",
  @Json(name = "access_expires_in_seconds") val accessExpiresInSec: Long = 900L,
  @Json(name = "refresh_expires_in_seconds") val refreshExpiresInSec: Long = 2_592_000L,
  val email: String = "",
  @Json(name = "suggested_full_name") val suggestedFullName: String = "",
  @Json(name = "suggested_username") val suggestedUsername: String = "",
  @Json(name = "profile_completed") val profileCompleted: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class UsernameCheckResponseDto(val available: Boolean = false)

@JsonClass(generateAdapter = true)
data class ProfileSetupDto(
  @Json(name = "full_name") val fullName: String,
  val username: String,
  @Json(name = "phone_number") val phoneNumber: String,
  @Json(name = "country_iso") val countryIso: String,
  val gender: String,
  @Json(name = "date_of_birth") val dateOfBirth: String,
  val location: String,
  val bio: String,
  @Json(name = "avatar_media_id") val avatarMediaId: String? = null,
)

@JsonClass(generateAdapter = true)
data class VideoRenditionDto(
  @Json(name = "quality_label") val qualityLabel: String = "",
  val width: Int = 0,
  val height: Int = 0,
  @Json(name = "bitrate_kbps") val bitrateKbps: Int = 0,
  @Json(name = "storage_object_key") val storageObjectKey: String = "",
  @Json(name = "byte_size") val byteSize: Int = 0,
)

@JsonClass(generateAdapter = true)
data class MediaUploadResponseDto(
  val success: Boolean = false,
  @Json(name = "media_id") val mediaId: String = "",
  @Json(name = "sha256_fingerprint") val sha256Fingerprint: String = "",
  @Json(name = "dhash_fingerprint") val dHashFingerprint: String = "",
  @Json(name = "content_id") val contentId: String = "",
  @Json(name = "thumbnail_media_id") val thumbnailMediaId: String = "",
  @Json(name = "per_frame_hashes") val perFrameHashes: List<String> = emptyList(),
  val renditions: List<VideoRenditionDto> = emptyList(),
  @Json(name = "mime_type") val mimeType: String = "",
  @Json(name = "byte_size") val byteSize: Int = 0,
)

@JsonClass(generateAdapter = true)
data class SongTrackDto(
  @Json(name = "track_id") val trackId: String = "",
  val title: String = "",
  val artist: String = "",
  val album: String = "",
  @Json(name = "artwork_url") val artworkUrl: String = "",
  @Json(name = "preview_url") val previewUrl: String = "",
  @Json(name = "duration_ms") val durationMs: Long = 0L,
  val source: String = "",
)

@JsonClass(generateAdapter = true)
data class SongsSearchResponseDto(
  val success: Boolean = false,
  val items: List<SongTrackDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class HashtagItemDto(
  val tag: String = "",
  @Json(name = "post_count") val postCount: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class HashtagsSearchResponseDto(
  val success: Boolean = false,
  val items: List<HashtagItemDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class CreatePostRequestDto(
  @Json(name = "client_op_id") val clientOpId: String,
  val mode: String,
  val privacy: String = "public",
  @Json(name = "text_content") val textContent: String = "",
  val caption: String = "",
  @Json(name = "theme_id") val themeId: String = "crimson",
  @Json(name = "font_id") val fontId: String = "classic",
  val song: SongTrackDto? = null,
  @Json(name = "tagged_user_ids") val taggedUserIds: List<String> = emptyList(),
  val hashtags: List<String> = emptyList(),
  @Json(name = "photo_media_ids") val photoMediaIds: List<String> = emptyList(),
  @Json(name = "video_media_id") val videoMediaId: String = "",
  @Json(name = "thumbnail_media_id") val thumbnailMediaId: String = "",
  @Json(name = "trim_start_ms") val trimStartMs: Long = 0L,
  @Json(name = "trim_end_ms") val trimEndMs: Long = 0L,
  @Json(name = "mute_original_audio") val muteOriginalAudio: Boolean = false,
  @Json(name = "content_id") val contentId: String = "",
)

@JsonClass(generateAdapter = true)
data class CreatedPostDto(
  @Json(name = "post_id") val postId: String = "",
  @Json(name = "client_op_id") val clientOpId: String = "",
  val mode: String = "",
  @Json(name = "content_id") val contentId: String = "",
  @Json(name = "created_at_ms") val createdAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class CreatePostResponseDto(
  val success: Boolean = false,
  val post: CreatedPostDto? = null,
)

@JsonClass(generateAdapter = true)
data class AuditEventDto(
  val action: String,
  val metadata: Map<String, String> = emptyMap(),
)

@JsonClass(generateAdapter = true)
data class GenericSuccessDto(val success: Boolean = false)

@JsonClass(generateAdapter = true)
data class SyncOperationDto(
  @Json(name = "client_op_id") val clientOpId: String,
  @Json(name = "room_id") val roomId: String,
  val kind: String,
  val payload: String,
  @Json(name = "created_at") val createdAt: Long,
)

@JsonClass(generateAdapter = true)
data class SyncBatchRequestDto(val operations: List<SyncOperationDto>)

@JsonClass(generateAdapter = true)
data class SyncBatchResponseDto(
  val success: Boolean,
  @Json(name = "acked_op_ids") val ackedOpIds: List<String> = emptyList(),
  @Json(name = "server_epoch_ms") val serverEpochMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class ContactItemDto(
  @Json(name = "user_id") val userId: String,
  @Json(name = "full_name") val fullName: String,
  val username: String,
  val location: String = "",
  @Json(name = "country_iso") val countryIso: String = "",
  @Json(name = "avatar_url") val avatarUrl: String? = null,
  @Json(name = "is_following") val isFollowing: Boolean = false,
  @Json(name = "follows_you") val followsYou: Boolean = false,
  @Json(name = "relationship_state") val relationshipState: String = "FOLLOW",
  @Json(name = "risk_score") val riskScore: Double = 0.0,
  @Json(name = "updated_at_ms") val updatedAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class ContactsPageResponseDto(
  val success: Boolean = false,
  val items: List<ContactItemDto> = emptyList(),
  val page: Int = 1,
  @Json(name = "page_size") val pageSize: Int = 20,
  @Json(name = "total_count") val totalCount: Int = 0,
  @Json(name = "has_more") val hasMore: Boolean = false,
  @Json(name = "server_epoch_ms") val serverEpochMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class ContactActionDto(
  @Json(name = "target_user_id") val targetUserId: String,
)

@JsonClass(generateAdapter = true)
data class FollowMutationResponseDto(
  val success: Boolean = false,
  @Json(name = "target_user_id") val targetUserId: String = "",
  @Json(name = "is_following") val isFollowing: Boolean = false,
  @Json(name = "follows_you") val followsYou: Boolean = false,
  @Json(name = "relationship_state") val relationshipState: String = "FOLLOW",
  @Json(name = "caller_risk_score") val callerRiskScore: Double = 0.0,
  @Json(name = "server_epoch_ms") val serverEpochMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class ContactSyncActionDto(
  @Json(name = "client_op_id") val clientOpId: String,
  @Json(name = "target_user_id") val targetUserId: String,
  val action: String,
  @Json(name = "created_at_ms") val createdAtMs: Long,
)

@JsonClass(generateAdapter = true)
data class UserProfileVideoDto(
  @Json(name = "post_id") val postId: String = "",
  @Json(name = "thumbnail_url") val thumbnailUrl: String = "",
  @Json(name = "video_url") val videoUrl: String = "",
  @Json(name = "views_count") val viewsCount: Long = 0L,
  @Json(name = "duration_ms") val durationMs: Long = 0L,
  val caption: String = "",
  @Json(name = "created_at_ms") val createdAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class ProfileBackgroundItemDto(
  @Json(name = "media_id") val mediaId: String = "",
  val url: String = "",
  @Json(name = "sha256_fingerprint") val sha256Fingerprint: String = "",
  @Json(name = "dhash_fingerprint") val dHashFingerprint: String = "",
  val width: Int = 0,
  val height: Int = 0,
  @Json(name = "byte_size") val byteSize: Int = 0,
  @Json(name = "created_at_ms") val createdAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class ProfileBackgroundUploadResponseDto(
  val success: Boolean = false,
  @Json(name = "media_id") val mediaId: String = "",
  @Json(name = "sha256_fingerprint") val sha256Fingerprint: String = "",
  @Json(name = "dhash_fingerprint") val dHashFingerprint: String = "",
  val background: ProfileBackgroundItemDto? = null,
  val backgrounds: List<ProfileBackgroundItemDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class ProfileBackgroundListResponseDto(
  val success: Boolean = false,
  val backgrounds: List<ProfileBackgroundItemDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class ProfileBackgroundAttachDto(
  @Json(name = "media_id") val mediaId: String,
)

@JsonClass(generateAdapter = true)
data class ProfileBackgroundReorderDto(
  @Json(name = "media_ids") val mediaIds: List<String>,
)

@JsonClass(generateAdapter = true)
data class ProfileUpdateRequestDto(
  @Json(name = "full_name") val fullName: String,
  val username: String,
  @Json(name = "phone_number") val phoneNumber: String = "",
  @Json(name = "country_iso") val countryIso: String = "",
  val gender: String = "",
  @Json(name = "date_of_birth") val dateOfBirth: String = "",
  val location: String,
  val bio: String,
  @Json(name = "avatar_media_id") val avatarMediaId: String? = null,
)

@JsonClass(generateAdapter = true)
data class ProfileUpdateResponseDto(
  val success: Boolean = false,
  val error: String = "",
  val username: String = "",
  @Json(name = "full_name") val fullName: String = "",
  @Json(name = "username_changed_at_ms") val usernameChangedAtMs: Long = 0L,
  @Json(name = "next_username_change_ms") val nextUsernameChangeMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class UserProfileResponseDto(
  val success: Boolean = false,
  @Json(name = "user_id") val userId: String = "",
  @Json(name = "full_name") val fullName: String = "",
  val username: String = "",
  @Json(name = "avatar_url") val avatarUrl: String = "",
  val bio: String = "",
  val location: String = "",
  @Json(name = "country_iso") val countryIso: String = "",
  val gender: String = "",
  @Json(name = "date_of_birth") val dateOfBirth: String = "",
  @Json(name = "phone_number") val phoneNumber: String = "",
  @Json(name = "username_changed_at_ms") val usernameChangedAtMs: Long = 0L,
  @Json(name = "next_username_change_ms") val nextUsernameChangeMs: Long = 0L,
  @Json(name = "background_urls") val backgroundUrls: List<String> = emptyList(),
  val backgrounds: List<ProfileBackgroundItemDto> = emptyList(),
  @Json(name = "following_count") val followingCount: Long = 0L,
  @Json(name = "followers_count") val followersCount: Long = 0L,
  @Json(name = "likes_count") val likesCount: Long = 0L,
  @Json(name = "is_following") val isFollowing: Boolean = false,
  @Json(name = "follows_you") val followsYou: Boolean = false,
  @Json(name = "is_owner") val isOwner: Boolean = false,
  @Json(name = "relationship_state") val relationshipState: String = "FOLLOW",
  @Json(name = "is_blocked") val isBlocked: Boolean = false,
  @Json(name = "is_muted") val isMuted: Boolean = false,
  @Json(name = "is_private_account") val isPrivateAccount: Boolean = false,
  @Json(name = "who_can_comment") val whoCanComment: String = "everyone",
  @Json(name = "who_can_mention") val whoCanMention: String = "everyone",
  @Json(name = "likes_visibility") val likesVisibility: String = "everyone",
  @Json(name = "following_visibility") val followingVisibility: String = "everyone",
  @Json(name = "show_activity_status") val showActivityStatus: Boolean = true,
  @Json(name = "risk_score") val riskScore: Double = 0.0,
  val videos: List<UserProfileVideoDto> = emptyList(),
  val posts: List<FeedItemDto> = emptyList(),
  @Json(name = "saved_posts") val savedPosts: List<FeedItemDto> = emptyList(),
  @Json(name = "reposted_posts") val repostedPosts: List<FeedItemDto> = emptyList(),
  @Json(name = "locked_posts") val lockedPosts: List<FeedItemDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class UserConnectionsResponseDto(
  val success: Boolean = false,
  val items: List<ContactItemDto> = emptyList(),
  val page: Int = 1,
  @Json(name = "has_more") val hasMore: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class UserBlockResponseDto(
  val success: Boolean = false,
  @Json(name = "target_user_id") val targetUserId: String = "",
  @Json(name = "is_blocked") val isBlocked: Boolean = false,
  @Json(name = "is_following") val isFollowing: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class UserReportRequestDto(
  val reason: String,
  val details: String = "",
)

@JsonClass(generateAdapter = true)
data class UserReportResponseDto(
  val success: Boolean = false,
  @Json(name = "target_user_id") val targetUserId: String = "",
  val reason: String = "",
)

@JsonClass(generateAdapter = true)
data class ContactSyncBatchRequestDto(
  val actions: List<ContactSyncActionDto>,
)

@JsonClass(generateAdapter = true)
data class UpdatePostRequestDto(
  @Json(name = "text_content") val textContent: String? = null,
  val caption: String? = null,
  val privacy: String? = null,
  @Json(name = "theme_id") val themeId: String? = null,
  @Json(name = "font_id") val fontId: String? = null,
  val song: SongTrackDto? = null,
  @Json(name = "tagged_user_ids") val taggedUserIds: List<String>? = null,
  val hashtags: List<String>? = null,
  @Json(name = "mute_original_audio") val muteOriginalAudio: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class FeedVideoRenditionDto(
  @Json(name = "quality_label") val qualityLabel: String = "",
  val width: Int = 0,
  val height: Int = 0,
  @Json(name = "bitrate_kbps") val bitrateKbps: Int = 0,
  val url: String = "",
  @Json(name = "byte_size") val byteSize: Int = 0,
)

@JsonClass(generateAdapter = true)
data class FeedItemDto(
  @Json(name = "post_id") val postId: String = "",
  @Json(name = "author_user_id") val authorUserId: String = "",
  @Json(name = "author_username") val authorUsername: String = "",
  @Json(name = "author_full_name") val authorFullName: String = "",
  @Json(name = "author_avatar_url") val authorAvatarUrl: String = "",
  @Json(name = "is_following_author") val isFollowingAuthor: Boolean = false,
  @Json(name = "is_owner") val isOwner: Boolean = false,
  val privacy: String = "public",
  val mode: String = "video",
  @Json(name = "text_content") val textContent: String = "",
  val caption: String = "",
  @Json(name = "theme_id") val themeId: String = "",
  @Json(name = "font_id") val fontId: String = "",
  val song: SongTrackDto? = null,
  val hashtags: List<String> = emptyList(),
  @Json(name = "photo_urls") val photoUrls: List<String> = emptyList(),
  @Json(name = "video_url") val videoUrl: String = "",
  @Json(name = "thumbnail_url") val thumbnailUrl: String = "",
  val renditions: List<FeedVideoRenditionDto> = emptyList(),
  val width: Int = 0,
  val height: Int = 0,
  @Json(name = "duration_ms") val durationMs: Long = 0L,
  @Json(name = "mute_original_audio") val muteOriginalAudio: Boolean = false,
  @Json(name = "content_id") val contentId: String = "",
  @Json(name = "likes_count") val likesCount: Long = 0L,
  @Json(name = "comments_count") val commentsCount: Long = 0L,
  @Json(name = "shares_count") val sharesCount: Long = 0L,
  @Json(name = "favourites_count") val favouritesCount: Long = 0L,
  @Json(name = "reposts_count") val repostsCount: Long = 0L,
  @Json(name = "views_count") val viewsCount: Long = 0L,
  @Json(name = "is_liked") val isLiked: Boolean = false,
  @Json(name = "is_favourited") val isFavourited: Boolean = false,
  @Json(name = "is_reposted") val isReposted: Boolean = false,
  @Json(name = "reposted_by_username") val repostedByUsername: String = "",
  @Json(name = "recommendation_label") val recommendationLabel: String = "",
  @Json(name = "relevance_score") val relevanceScore: Double = 0.0,
  @Json(name = "created_at_ms") val createdAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class FeedPageResponseDto(
  val success: Boolean = false,
  val tab: String = "for_you",
  val items: List<FeedItemDto> = emptyList(),
  val page: Int = 1,
  @Json(name = "page_size") val pageSize: Int = 15,
  @Json(name = "has_more") val hasMore: Boolean = false,
  @Json(name = "server_epoch_ms") val serverEpochMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class SearchShortcutDto(
  val key: String = "",
  val label: String = "",
  val count: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class FeedSearchResponseDto(
  val success: Boolean = false,
  val query: String = "",
  val shortcut: String = "",
  val shortcuts: List<SearchShortcutDto> = emptyList(),
  val items: List<FeedItemDto> = emptyList(),
  val page: Int = 1,
  @Json(name = "page_size") val pageSize: Int = 24,
  @Json(name = "has_more") val hasMore: Boolean = false,
  @Json(name = "server_epoch_ms") val serverEpochMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class FeedInteractionRequestDto(
  @Json(name = "client_op_id") val clientOpId: String = "",
  @Json(name = "post_id") val postId: String,
  val action: String,
  @Json(name = "watch_ms") val watchMs: Long = 0L,
  @Json(name = "duration_ms") val durationMs: Long = 0L,
  val reason: String = "",
  val details: String = "",
)

@JsonClass(generateAdapter = true)
data class FeedInteractionResponseDto(
  val success: Boolean = false,
  @Json(name = "post_id") val postId: String = "",
  val action: String = "",
  @Json(name = "likes_count") val likesCount: Long = 0L,
  @Json(name = "favourites_count") val favouritesCount: Long = 0L,
  @Json(name = "reposts_count") val repostsCount: Long = 0L,
  @Json(name = "shares_count") val sharesCount: Long = 0L,
  @Json(name = "is_liked") val isLiked: Boolean = false,
  @Json(name = "is_favourited") val isFavourited: Boolean = false,
  @Json(name = "is_reposted") val isReposted: Boolean = false,
  @Json(name = "server_epoch_ms") val serverEpochMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class FeedInteractionBatchRequestDto(
  val operations: List<FeedInteractionRequestDto>,
)

@JsonClass(generateAdapter = true)
data class PrivacySettingsDto(
  @Json(name = "is_private_account") val isPrivateAccount: Boolean = false,
  @Json(name = "who_can_comment") val whoCanComment: String = "everyone",
  @Json(name = "who_can_mention") val whoCanMention: String = "everyone",
  @Json(name = "who_can_find_you") val whoCanFindYou: String = "everyone",
  @Json(name = "show_activity_status") val showActivityStatus: Boolean = true,
  @Json(name = "likes_visibility") val likesVisibility: String = "everyone",
  @Json(name = "following_visibility") val followingVisibility: String = "everyone",
  @Json(name = "data_sharing_personalization") val dataSharingPersonalization: Boolean = true,
  @Json(name = "analytics_sharing") val analyticsSharing: Boolean = true,
  @Json(name = "essential_cookies") val essentialCookies: Boolean = true,
  @Json(name = "personalization_cookies") val personalizationCookies: Boolean = true,
  @Json(name = "updated_at_ms") val updatedAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class NotificationSettingsDto(
  @Json(name = "master_pause") val masterPause: Boolean = false,
  @Json(name = "channel_push") val channelPush: Boolean = true,
  @Json(name = "channel_email") val channelEmail: Boolean = true,
  @Json(name = "channel_sms") val channelSms: Boolean = false,
  @Json(name = "type_messages") val typeMessages: Boolean = true,
  @Json(name = "type_comments") val typeComments: Boolean = true,
  @Json(name = "type_followers") val typeFollowers: Boolean = true,
  @Json(name = "type_product_updates") val typeProductUpdates: Boolean = false,
  @Json(name = "quiet_hours_enabled") val quietHoursEnabled: Boolean = false,
  @Json(name = "quiet_start_minutes") val quietStartMinutes: Int = 1320,
  @Json(name = "quiet_end_minutes") val quietEndMinutes: Int = 420,
  @Json(name = "updated_at_ms") val updatedAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class ContentPreferencesDto(
  @Json(name = "suggested_content_enabled") val suggestedContentEnabled: Boolean = true,
  @Json(name = "not_interested_topics") val notInterestedTopics: List<String> = emptyList(),
  @Json(name = "language_code") val languageCode: String = "en",
  @Json(name = "autoplay_videos") val autoplayVideos: Boolean = true,
  @Json(name = "video_quality") val videoQuality: String = "auto",
  @Json(name = "data_saver") val dataSaver: Boolean = false,
  @Json(name = "captions_enabled") val captionsEnabled: Boolean = false,
  @Json(name = "reduced_motion") val reducedMotion: Boolean = false,
  @Json(name = "reset_suggestions") val resetSuggestions: Boolean = false,
  @Json(name = "updated_at_ms") val updatedAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class ChatPrivacySettingsDto(
  @Json(name = "message_requests") val messageRequests: String = "anyone",
  @Json(name = "who_can_call") val whoCanCall: String = "anyone",
  @Json(name = "who_can_screenshot") val whoCanScreenshot: String = "nobody",
  @Json(name = "who_can_share_chats") val whoCanShareChats: String = "nobody",
  @Json(name = "who_can_copy_messages") val whoCanCopyMessages: String = "nobody",
  @Json(name = "who_can_download_media") val whoCanDownloadMedia: String = "anyone",
  @Json(name = "chat_theme_id") val chatThemeId: String = "none",
  @Json(name = "bubble_color_id") val bubbleColorId: String = "cyan",
  @Json(name = "read_receipts") val readReceipts: String = "anyone",
  @Json(name = "typing_indicators") val typingIndicators: String = "nobody",
  @Json(name = "online_indicators") val onlineIndicators: String = "nobody",
  @Json(name = "updated_at_ms") val updatedAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class ChatThemeCatalogItemDto(
  @Json(name = "theme_id") val themeId: String = "",
  val title: String = "",
  @Json(name = "pattern_kind") val patternKind: String = "",
  @Json(name = "light_bg_hex") val lightBgHex: String = "",
  @Json(name = "dark_bg_hex") val darkBgHex: String = "",
  @Json(name = "accent_start_hex") val accentStartHex: String = "",
  @Json(name = "accent_end_hex") val accentEndHex: String = "",
  @Json(name = "selection_hex") val selectionHex: String = "",
)

@JsonClass(generateAdapter = true)
data class ChatBubbleColorDto(
  @Json(name = "color_id") val colorId: String = "",
  val label: String = "",
  @Json(name = "light_start_hex") val lightStartHex: String = "",
  @Json(name = "light_end_hex") val lightEndHex: String = "",
  @Json(name = "dark_start_hex") val darkStartHex: String = "",
  @Json(name = "dark_end_hex") val darkEndHex: String = "",
)

@JsonClass(generateAdapter = true)
data class ChatPrivacyResponseDto(
  val success: Boolean = false,
  @Json(name = "chat_privacy") val chatPrivacy: ChatPrivacySettingsDto? = null,
  val themes: List<ChatThemeCatalogItemDto> = emptyList(),
  @Json(name = "bubble_colors") val bubbleColors: List<ChatBubbleColorDto> = emptyList(),
  @Json(name = "server_epoch_ms") val serverEpochMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class UpdateChatThemeRequestDto(
  @Json(name = "chat_theme_id") val chatThemeId: String = "",
  @Json(name = "bubble_color_id") val bubbleColorId: String = "",
)

@JsonClass(generateAdapter = true)
data class SyncPreferencesDto(
  @Json(name = "auto_sync_enabled") val autoSyncEnabled: Boolean = true,
  @Json(name = "sync_over_wifi_only") val syncOverWifiOnly: Boolean = false,
  @Json(name = "background_sync_enabled") val backgroundSyncEnabled: Boolean = true,
  @Json(name = "cache_feeds_offline") val cacheFeedsOffline: Boolean = true,
  @Json(name = "cache_chat_media_offline") val cacheChatMediaOffline: Boolean = true,
  @Json(name = "last_synced_at_ms") val lastSyncedAtMs: Long = 0L,
  @Json(name = "last_sync_status") val lastSyncStatus: String = "idle",
  @Json(name = "total_synced_ops") val totalSyncedOps: Long = 0L,
  @Json(name = "updated_at_ms") val updatedAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class SyncSettingsResponseDto(
  val success: Boolean = false,
  @Json(name = "sync_preferences") val syncPreferences: SyncPreferencesDto? = null,
  @Json(name = "server_epoch_ms") val serverEpochMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class ManualSyncRequestDto(
  @Json(name = "client_sync_id") val clientSyncId: String = "",
  val operations: List<SyncOperationDto> = emptyList(),
  val privacy: PrivacySettingsDto? = null,
  val notifications: NotificationSettingsDto? = null,
  @Json(name = "content_preferences") val contentPreferences: ContentPreferencesDto? = null,
  @Json(name = "chat_privacy") val chatPrivacy: ChatPrivacySettingsDto? = null,
  @Json(name = "sync_preferences") val syncPreferences: SyncPreferencesDto? = null,
)

@JsonClass(generateAdapter = true)
data class ManualSyncResponseDto(
  val success: Boolean = false,
  @Json(name = "synced_ops_count") val syncedOpsCount: Int = 0,
  @Json(name = "acked_op_ids") val ackedOpIds: List<String> = emptyList(),
  @Json(name = "sync_preferences") val syncPreferences: SyncPreferencesDto? = null,
  val privacy: PrivacySettingsDto? = null,
  val notifications: NotificationSettingsDto? = null,
  @Json(name = "content_preferences") val contentPreferences: ContentPreferencesDto? = null,
  @Json(name = "chat_privacy") val chatPrivacy: ChatPrivacySettingsDto? = null,
  @Json(name = "server_epoch_ms") val serverEpochMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class SettingsBundleResponseDto(
  val success: Boolean = false,
  val privacy: PrivacySettingsDto? = null,
  val notifications: NotificationSettingsDto? = null,
  @Json(name = "content_preferences") val contentPreferences: ContentPreferencesDto? = null,
  @Json(name = "chat_privacy") val chatPrivacy: ChatPrivacySettingsDto? = null,
  @Json(name = "sync_preferences") val syncPreferences: SyncPreferencesDto? = null,
  @Json(name = "server_epoch_ms") val serverEpochMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class RestrictedAccountItemDto(
  @Json(name = "user_id") val userId: String = "",
  @Json(name = "full_name") val fullName: String = "",
  val username: String = "",
  @Json(name = "avatar_url") val avatarUrl: String = "",
  @Json(name = "created_at_ms") val createdAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class RestrictedAccountsResponseDto(
  val success: Boolean = false,
  val items: List<RestrictedAccountItemDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class UserMuteResponseDto(
  val success: Boolean = false,
  @Json(name = "user_id") val userId: String = "",
  @Json(name = "is_muted") val isMuted: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class ProblemReportRequestDto(
  @Json(name = "client_op_id") val clientOpId: String = "",
  val category: String,
  val description: String,
  @Json(name = "screenshot_media_id") val screenshotMediaId: String = "",
  @Json(name = "screenshot_sha256") val screenshotSha256: String = "",
)

@JsonClass(generateAdapter = true)
data class SupportTicketDto(
  @Json(name = "ticket_id") val ticketId: String = "",
  @Json(name = "reference_number") val referenceNumber: String = "",
  val category: String = "",
  val description: String = "",
  @Json(name = "screenshot_media_id") val screenshotMediaId: String = "",
  @Json(name = "screenshot_sha256") val screenshotSha256: String = "",
  val status: String = "open",
  @Json(name = "resolution_note") val resolutionNote: String = "",
  @Json(name = "created_at_ms") val createdAtMs: Long = 0L,
  @Json(name = "updated_at_ms") val updatedAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class ProblemReportResponseDto(
  val success: Boolean = false,
  @Json(name = "reference_number") val referenceNumber: String = "",
  val ticket: SupportTicketDto? = null,
)

@JsonClass(generateAdapter = true)
data class SupportTicketsResponseDto(
  val success: Boolean = false,
  val items: List<SupportTicketDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class FaqItemDto(
  val id: String = "",
  val category: String = "",
  val question: String = "",
  val answer: String = "",
)

@JsonClass(generateAdapter = true)
data class FaqListResponseDto(
  val success: Boolean = false,
  val items: List<FaqItemDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class SystemServiceStatusDto(
  @Json(name = "service_name") val serviceName: String = "",
  val status: String = "operational",
  @Json(name = "latency_ms") val latencyMs: Int = 0,
  val summary: String = "",
)

@JsonClass(generateAdapter = true)
data class SystemStatusResponseDto(
  val success: Boolean = false,
  @Json(name = "overall_status") val overallStatus: String = "operational",
  val services: List<SystemServiceStatusDto> = emptyList(),
  @Json(name = "checked_at_ms") val checkedAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class LegalDocumentDto(
  val id: String = "",
  val title: String = "",
  @Json(name = "last_updated") val lastUpdated: String = "",
  val sections: List<String> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class LegalDocumentsResponseDto(
  val success: Boolean = false,
  val items: List<LegalDocumentDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class AppVersionResponseDto(
  val success: Boolean = false,
  @Json(name = "latest_version") val latestVersion: String = "1.0",
  @Json(name = "min_supported_version") val minSupportedVersion: String = "1.0",
  @Json(name = "build_number") val buildNumber: Int = 1,
  @Json(name = "update_available") val updateAvailable: Boolean = false,
  @Json(name = "release_notes") val releaseNotes: String = "",
)

@JsonClass(generateAdapter = true)
data class LogoutRequestDto(
  @Json(name = "all_devices") val allDevices: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class OneTimePreKeyDto(
  @Json(name = "key_id") val keyId: Int,
  @Json(name = "public_key_b64") val publicKeyB64: String,
)

@JsonClass(generateAdapter = true)
data class E2eeKeyBundleUploadDto(
  @Json(name = "registration_id") val registrationId: Int,
  @Json(name = "identity_pub_b64") val identityPubB64: String,
  @Json(name = "signed_prekey_id") val signedPreKeyId: Int,
  @Json(name = "signed_prekey_pub_b64") val signedPreKeyPubB64: String,
  @Json(name = "signed_prekey_sig_b64") val signedPreKeySigB64: String,
  @Json(name = "kyber_prekey_id") val kyberPreKeyId: Int,
  @Json(name = "kyber_prekey_pub_b64") val kyberPreKeyPubB64: String,
  @Json(name = "kyber_prekey_sig_b64") val kyberPreKeySigB64: String,
  @Json(name = "one_time_prekeys") val oneTimePreKeys: List<OneTimePreKeyDto>,
)

@JsonClass(generateAdapter = true)
data class E2eeKeyBundleResponseDto(
  val success: Boolean = false,
  @Json(name = "user_id") val userId: String = "",
  @Json(name = "device_id") val deviceId: Int = 1,
  @Json(name = "registration_id") val registrationId: Int = 0,
  @Json(name = "identity_pub_b64") val identityPubB64: String = "",
  @Json(name = "signed_prekey_id") val signedPreKeyId: Int = 0,
  @Json(name = "signed_prekey_pub_b64") val signedPreKeyPubB64: String = "",
  @Json(name = "signed_prekey_sig_b64") val signedPreKeySigB64: String = "",
  @Json(name = "kyber_prekey_id") val kyberPreKeyId: Int = 0,
  @Json(name = "kyber_prekey_pub_b64") val kyberPreKeyPubB64: String = "",
  @Json(name = "kyber_prekey_sig_b64") val kyberPreKeySigB64: String = "",
  @Json(name = "prekey_id") val preKeyId: Int = 0,
  @Json(name = "prekey_pub_b64") val preKeyPubB64: String = "",
)

@JsonClass(generateAdapter = true)
data class ChatPeerCapabilitiesDto(
  @Json(name = "can_call") val canCall: Boolean = true,
  @Json(name = "can_screenshot") val canScreenshot: Boolean = false,
  @Json(name = "can_share_chats") val canShareChats: Boolean = false,
  @Json(name = "can_copy_messages") val canCopyMessages: Boolean = false,
  @Json(name = "can_download_media") val canDownloadMedia: Boolean = true,
  @Json(name = "show_read_receipts") val showReadReceipts: Boolean = true,
  @Json(name = "show_typing_indicator") val showTypingIndicator: Boolean = false,
  @Json(name = "show_online_indicator") val showOnlineIndicator: Boolean = false,
  @Json(name = "is_blocked") val isBlocked: Boolean = false,
  @Json(name = "is_muted") val isMuted: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class ChatConversationDto(
  @Json(name = "conversation_id") val conversationId: String = "",
  @Json(name = "peer_user_id") val peerUserId: String = "",
  @Json(name = "peer_full_name") val peerFullName: String = "",
  @Json(name = "peer_username") val peerUsername: String = "",
  @Json(name = "peer_avatar_url") val peerAvatarUrl: String = "",
  @Json(name = "last_message_id") val lastMessageId: String = "",
  @Json(name = "last_message_type") val lastMessageType: String = "",
  @Json(name = "last_sender_id") val lastSenderId: String = "",
  @Json(name = "last_ciphertext_b64") val lastCiphertextB64: String = "",
  @Json(name = "last_sender_ciphertext_b64") val lastSenderCiphertextB64: String = "",
  @Json(name = "last_encryption_scheme") val lastEncryptionScheme: String = "",
  @Json(name = "last_signal_msg_type") val lastSignalMsgType: Int = 0,
  @Json(name = "last_delivery_status") val lastDeliveryStatus: String = "sent",
  @Json(name = "last_message_at_ms") val lastMessageAtMs: Long = 0L,
  @Json(name = "unread_count") val unreadCount: Int = 0,
  @Json(name = "is_archived") val isArchived: Boolean = false,
  @Json(name = "is_muted") val isMuted: Boolean = false,
  @Json(name = "is_online") val isOnline: Boolean = false,
  @Json(name = "last_seen_ms") val lastSeenMs: Long = 0L,
  @Json(name = "last_seen_label") val lastSeenLabel: String = "last seen recently",
  val permissions: ChatPeerCapabilitiesDto = ChatPeerCapabilitiesDto(),
  @Json(name = "created_at_ms") val createdAtMs: Long = 0L,
  @Json(name = "updated_at_ms") val updatedAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class ChatConversationsResponseDto(
  val success: Boolean = false,
  val conversations: List<ChatConversationDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class OpenDirectConversationResponseDto(
  val success: Boolean = false,
  val conversation: ChatConversationDto = ChatConversationDto(),
)

@JsonClass(generateAdapter = true)
data class ChatMessageDto(
  @Json(name = "message_id") val messageId: String = "",
  @Json(name = "conversation_id") val conversationId: String = "",
  @Json(name = "sender_user_id") val senderUserId: String = "",
  @Json(name = "recipient_user_id") val recipientUserId: String = "",
  @Json(name = "client_op_id") val clientOpId: String = "",
  @Json(name = "message_type") val messageType: String = "text",
  @Json(name = "compression_codec") val compressionCodec: String = "zlib",
  @Json(name = "encryption_scheme") val encryptionScheme: String = "libsignal_v1",
  @Json(name = "ciphertext_b64") val ciphertextB64: String = "",
  @Json(name = "sender_ciphertext_b64") val senderCiphertextB64: String = "",
  @Json(name = "signal_msg_type") val signalMsgType: Int = 3,
  @Json(name = "media_url") val mediaUrl: String = "",
  @Json(name = "media_id") val mediaId: String = "",
  @Json(name = "voice_duration_ms") val voiceDurationMs: Long = 0L,
  @Json(name = "view_once_ttl_sec") val viewOnceTtlSec: Int = 0,
  @Json(name = "view_once_opened_at_ms") val viewOnceOpenedAtMs: Long = 0L,
  @Json(name = "view_once_expires_at_ms") val viewOnceExpiresAtMs: Long = 0L,
  @Json(name = "forwarded_from_user_id") val forwardedFromUserId: String = "",
  @Json(name = "delivery_status") val deliveryStatus: String = "sent",
  @Json(name = "edited_at_ms") val editedAtMs: Long = 0L,
  @Json(name = "created_at_ms") val createdAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class LinkPreviewDto(
  @Json(name = "normalized_url") val normalizedUrl: String = "",
  val title: String = "",
  val description: String = "",
  @Json(name = "image_url") val imageUrl: String = "",
  @Json(name = "favicon_url") val faviconUrl: String = "",
  val domain: String = "",
  val status: String = "fallback",
  @Json(name = "fetched_at_ms") val fetchedAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class LinkPreviewResponseDto(
  val success: Boolean = false,
  val preview: LinkPreviewDto? = null,
)

@JsonClass(generateAdapter = true)
data class SavedLinkDto(
  val id: String = "",
  @Json(name = "user_id") val userId: String = "",
  val url: String = "",
  @Json(name = "normalized_url") val normalizedUrl: String = "",
  val title: String = "",
  val description: String = "",
  @Json(name = "image_url") val imageUrl: String = "",
  @Json(name = "favicon_url") val faviconUrl: String = "",
  val domain: String = "",
  val source: String = "browser",
  @Json(name = "saved_at_ms") val savedAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class SaveLinkRequestDto(
  val url: String,
  val title: String = "",
  val description: String = "",
  @Json(name = "image_url") val imageUrl: String = "",
  @Json(name = "favicon_url") val faviconUrl: String = "",
  val domain: String = "",
  val source: String = "browser",
)

@JsonClass(generateAdapter = true)
data class SavedLinksListResponseDto(
  val success: Boolean = false,
  val items: List<SavedLinkDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class SaveLinkResponseDto(
  val success: Boolean = false,
  val item: SavedLinkDto? = null,
)

@JsonClass(generateAdapter = true)
data class ChatSecurityFlagDto(
  @Json(name = "flag_id") val flagId: String = "",
  @Json(name = "conversation_id") val conversationId: String = "",
  @Json(name = "message_id") val messageId: String = "",
  @Json(name = "actor_user_id") val actorUserId: String = "",
  @Json(name = "target_user_id") val targetUserId: String = "",
  @Json(name = "flag_type") val flagType: String = "",
  val details: String = "",
  @Json(name = "created_at_ms") val createdAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class ChatMessagesListResponseDto(
  val success: Boolean = false,
  val conversation: ChatConversationDto = ChatConversationDto(),
  val messages: List<ChatMessageDto> = emptyList(),
  @Json(name = "security_flags") val securityFlags: List<ChatSecurityFlagDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class SendChatMessageRequestDto(
  @Json(name = "client_op_id") val clientOpId: String,
  @Json(name = "message_type") val messageType: String,
  @Json(name = "compression_codec") val compressionCodec: String = "zlib",
  @Json(name = "encryption_scheme") val encryptionScheme: String = "libsignal_v1",
  @Json(name = "ciphertext_b64") val ciphertextB64: String,
  @Json(name = "sender_ciphertext_b64") val senderCiphertextB64: String,
  @Json(name = "signal_msg_type") val signalMsgType: Int,
  @Json(name = "media_url") val mediaUrl: String = "",
  @Json(name = "media_id") val mediaId: String = "",
  @Json(name = "voice_duration_ms") val voiceDurationMs: Long = 0L,
  @Json(name = "view_once_ttl_sec") val viewOnceTtlSec: Int = 0,
  @Json(name = "forwarded_from_user_id") val forwardedFromUserId: String = "",
)

@JsonClass(generateAdapter = true)
data class ChatMessageSingleResponseDto(
  val success: Boolean = false,
  val message: ChatMessageDto = ChatMessageDto(),
)

@JsonClass(generateAdapter = true)
data class EditChatMessageRequestDto(
  @Json(name = "ciphertext_b64") val ciphertextB64: String,
  @Json(name = "sender_ciphertext_b64") val senderCiphertextB64: String,
  @Json(name = "signal_msg_type") val signalMsgType: Int,
)

@JsonClass(generateAdapter = true)
data class ChatReceiptRequestDto(
  val status: String,
)

@JsonClass(generateAdapter = true)
data class BatchConversationActionRequestDto(
  @Json(name = "conversation_ids") val conversationIds: List<String>,
  val action: String,
)

@JsonClass(generateAdapter = true)
data class VoiceMediaUploadResponseDto(
  val success: Boolean = false,
  @Json(name = "media_id") val mediaId: String = "",
  @Json(name = "media_url") val mediaUrl: String = "",
  @Json(name = "sha256_fingerprint") val sha256Fingerprint: String = "",
  @Json(name = "byte_size") val byteSize: Int = 0,
)

@JsonClass(generateAdapter = true)
data class ViewOnceOpenResponseDto(
  val success: Boolean = false,
  val message: ChatMessageDto = ChatMessageDto(),
  @Json(name = "security_flag") val securityFlag: ChatSecurityFlagDto = ChatSecurityFlagDto(),
)

@JsonClass(generateAdapter = true)
data class RecordSecurityFlagRequestDto(
  @Json(name = "message_id") val messageId: String = "",
  @Json(name = "flag_type") val flagType: String,
  val details: String = "",
)

@JsonClass(generateAdapter = true)
data class SecurityFlagResponseDto(
  val success: Boolean = false,
  val flag: ChatSecurityFlagDto = ChatSecurityFlagDto(),
)

@JsonClass(generateAdapter = true)
data class SecurityFlagsListResponseDto(
  val success: Boolean = false,
  val flags: List<ChatSecurityFlagDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class RegisterPushTokenRequestDto(
  val token: String,
  val platform: String = "android",
  @Json(name = "app_bundle") val appBundle: String = "com.telefam.company.app",
)

@JsonClass(generateAdapter = true)
data class TelefamNotificationDto(
  val id: String = "",
  @Json(name = "recipient_user_id") val recipientUserId: String = "",
  @Json(name = "actor_user_id") val actorUserId: String = "",
  @Json(name = "actor_name") val actorName: String = "",
  @Json(name = "actor_username") val actorUsername: String = "",
  @Json(name = "actor_avatar_url") val actorAvatarUrl: String = "",
  val kind: String = "",
  val title: String = "",
  val body: String = "",
  @Json(name = "conversation_id") val conversationId: String = "",
  @Json(name = "message_id") val messageId: String = "",
  @Json(name = "post_id") val postId: String = "",
  @Json(name = "target_user_id") val targetUserId: String = "",
  @Json(name = "is_view_once") val isViewOnce: Boolean = false,
  @Json(name = "is_read") val isRead: Boolean = false,
  @Json(name = "created_at_ms") val createdAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class NotificationsListResponseDto(
  val success: Boolean = false,
  val items: List<TelefamNotificationDto> = emptyList(),
  @Json(name = "unread_count") val unreadCount: Int = 0,
)

@JsonClass(generateAdapter = true)
data class MarkNotificationReadRequestDto(
  @Json(name = "notification_id") val notificationId: String = "",
  @Json(name = "conversation_id") val conversationId: String = "",
)

@JsonClass(generateAdapter = true)
data class NotificationUnreadCountResponseDto(
  val success: Boolean = false,
  @Json(name = "unread_count") val unreadCount: Int = 0,
)

@JsonClass(generateAdapter = true)
data class GiflyMediaItemDto(
  val id: Long = 0L,
  val slug: String = "",
  val title: String = "",
  val type: String = "sticker",
  val tags: List<String> = emptyList(),
  val preview: String = "",
  @Json(name = "preview_webp") val previewWebp: String = "",
  val url: String = "",
  @Json(name = "url_hd") val urlHd: String = "",
  val mp4: String? = null,
  val width: Int = 200,
  val height: Int = 200,
)

@JsonClass(generateAdapter = true)
data class GiflyGreetingResponseDto(
  val success: Boolean = false,
  val item: GiflyMediaItemDto = GiflyMediaItemDto(),
)

@JsonClass(generateAdapter = true)
data class GiflySearchResponseDto(
  val success: Boolean = false,
  val type: String = "stickers",
  val query: String = "",
  val page: Int = 1,
  @Json(name = "per_page") val perPage: Int = 24,
  @Json(name = "has_next") val hasNext: Boolean = false,
  val count: Int = 0,
  val items: List<GiflyMediaItemDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class ChatSearchPersonDto(
  @Json(name = "user_id") val userId: String = "",
  @Json(name = "full_name") val fullName: String = "",
  val username: String = "",
  @Json(name = "avatar_url") val avatarUrl: String = "",
  val location: String = "",
  @Json(name = "conversation_id") val conversationId: String = "",
)

@JsonClass(generateAdapter = true)
data class ChatGlobalSearchResponseDto(
  val success: Boolean = false,
  val conversations: List<ChatConversationDto> = emptyList(),
  val people: List<ChatSearchPersonDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class IceServerDto(
  val urls: List<String> = emptyList(),
  val username: String = "",
  val credential: String = "",
)

@JsonClass(generateAdapter = true)
data class IceConfigResponseDto(
  val success: Boolean = false,
  @Json(name = "ice_servers") val iceServers: List<IceServerDto> = emptyList(),
  @Json(name = "timeout_sec") val timeoutSec: Int = 25,
)

@JsonClass(generateAdapter = true)
data class CallRecordDto(
  @Json(name = "call_id") val callId: String = "",
  @Json(name = "conversation_id") val conversationId: String = "",
  @Json(name = "caller_user_id") val callerUserId: String = "",
  @Json(name = "callee_user_id") val calleeUserId: String = "",
  @Json(name = "caller_name") val callerName: String = "",
  @Json(name = "caller_username") val callerUsername: String = "",
  @Json(name = "caller_avatar") val callerAvatar: String = "",
  @Json(name = "callee_name") val calleeName: String = "",
  @Json(name = "callee_username") val calleeUsername: String = "",
  @Json(name = "callee_avatar") val calleeAvatar: String = "",
  @Json(name = "call_type") val callType: String = "audio",
  val state: String = "initiated",
  @Json(name = "end_reason") val endReason: String = "",
  @Json(name = "video_upgraded") val videoUpgraded: Boolean = false,
  @Json(name = "log_message_id") val logMessageId: String = "",
  @Json(name = "started_at_ms") val startedAtMs: Long = 0L,
  @Json(name = "ringing_at_ms") val ringingAtMs: Long = 0L,
  @Json(name = "answered_at_ms") val answeredAtMs: Long = 0L,
  @Json(name = "ended_at_ms") val endedAtMs: Long = 0L,
  @Json(name = "duration_sec") val durationSec: Long = 0L,
  @Json(name = "expires_at_ms") val expiresAtMs: Long = 0L,
  @Json(name = "server_epoch_ms") val serverEpochMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class StarWalletDto(
  @Json(name = "user_id") val userId: String = "",
  @Json(name = "balance_stars") val balanceStars: Long = 0L,
  @Json(name = "stars_received_total") val starsReceivedTotal: Long = 0L,
  @Json(name = "stars_sent_total") val starsSentTotal: Long = 0L,
  @Json(name = "updated_at_ms") val updatedAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class StarPackageDto(
  val stars: Long = 5L,
  @Json(name = "is_popular") val isPopular: Boolean = false,
  @Json(name = "display_amount") val displayAmount: String = "",
  @Json(name = "display_currency") val displayCurrency: String = "USD",
  @Json(name = "paystack_amount_minor") val paystackAmountMinor: Long = 0L,
  @Json(name = "paystack_currency") val paystackCurrency: String = "USD",
  @Json(name = "paypal_amount_value") val paypalAmountValue: String = "0.05",
  @Json(name = "paypal_currency") val paypalCurrency: String = "USD",
)

@JsonClass(generateAdapter = true)
data class StarSenderSummaryDto(
  @Json(name = "sender_user_id") val senderUserId: String = "",
  @Json(name = "sender_full_name") val senderFullName: String = "",
  @Json(name = "sender_username") val senderUsername: String = "",
  @Json(name = "sender_avatar_url") val senderAvatarUrl: String = "",
  @Json(name = "total_stars_sent") val totalStarsSent: Long = 0L,
  @Json(name = "gift_count") val giftCount: Int = 0,
  @Json(name = "last_sent_at_ms") val lastSentAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class StarLedgerItemDto(
  @Json(name = "entry_id") val entryId: String = "",
  @Json(name = "entry_type") val entryType: String = "",
  @Json(name = "delta_stars") val deltaStars: Long = 0L,
  @Json(name = "balance_after") val balanceAfter: Long = 0L,
  @Json(name = "counterparty_user_id") val counterpartyUserId: String = "",
  @Json(name = "counterparty_name") val counterpartyName: String = "",
  @Json(name = "counterparty_username") val counterpartyUsername: String = "",
  @Json(name = "counterparty_avatar") val counterpartyAvatar: String = "",
  val provider: String = "",
  @Json(name = "amount_minor") val amountMinor: Long = 0L,
  val currency: String = "",
  @Json(name = "payment_id") val paymentId: String = "",
  @Json(name = "message_id") val messageId: String = "",
  @Json(name = "conversation_id") val conversationId: String = "",
  @Json(name = "created_at_ms") val createdAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class StarsSummaryResponseDto(
  val success: Boolean = false,
  val wallet: StarWalletDto = StarWalletDto(),
  @Json(name = "country_iso") val countryIso: String = "US",
  @Json(name = "packages") val packages: List<StarPackageDto> = emptyList(),
  val senders: List<StarSenderSummaryDto> = emptyList(),
  val history: List<StarLedgerItemDto> = emptyList(),
  @Json(name = "server_epoch_ms") val serverEpochMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class StarPaymentDto(
  @Json(name = "payment_id") val paymentId: String = "",
  val provider: String = "",
  @Json(name = "provider_reference") val providerReference: String = "",
  @Json(name = "provider_order_id") val providerOrderId: String = "",
  @Json(name = "package_stars") val packageStars: Long = 0L,
  @Json(name = "expected_amount_minor") val expectedAmountMinor: Long = 0L,
  @Json(name = "expected_currency") val expectedCurrency: String = "",
  @Json(name = "paid_amount_minor") val paidAmountMinor: Long = 0L,
  @Json(name = "paid_currency") val paidCurrency: String = "",
  val status: String = "",
  @Json(name = "checkout_url") val checkoutUrl: String = "",
  val purpose: String = "topup",
  @Json(name = "peer_user_id") val peerUserId: String = "",
  @Json(name = "conversation_id") val conversationId: String = "",
  @Json(name = "verified_via") val verifiedVia: String = "",
  @Json(name = "created_at_ms") val createdAtMs: Long = 0L,
  @Json(name = "completed_at_ms") val completedAtMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class StarsCheckoutInitRequestDto(
  val provider: String,
  @Json(name = "package_stars") val packageStars: Long,
  @Json(name = "country_iso") val countryIso: String,
  @Json(name = "idempotency_key") val idempotencyKey: String,
  val purpose: String = "topup",
  @Json(name = "peer_user_id") val peerUserId: String = "",
  @Json(name = "conversation_id") val conversationId: String = "",
)

@JsonClass(generateAdapter = true)
data class StarsCheckoutInitResponseDto(
  val success: Boolean = false,
  val payment: StarPaymentDto = StarPaymentDto(),
  val wallet: StarWalletDto = StarWalletDto(),
  @Json(name = "return_url") val returnUrl: String = "",
)

@JsonClass(generateAdapter = true)
data class StarsCheckoutVerifyRequestDto(
  @Json(name = "payment_id") val paymentId: String = "",
  val reference: String = "",
  @Json(name = "paypal_order_id") val paypalOrderId: String = "",
  @Json(name = "idempotency_key") val idempotencyKey: String,
)

@JsonClass(generateAdapter = true)
data class StarsCheckoutVerifyResponseDto(
  val success: Boolean = false,
  val credited: Boolean = false,
  val payment: StarPaymentDto = StarPaymentDto(),
  val wallet: StarWalletDto = StarWalletDto(),
  @Json(name = "server_epoch_ms") val serverEpochMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class SendStarsRequestDto(
  @Json(name = "recipient_user_id") val recipientUserId: String,
  @Json(name = "conversation_id") val conversationId: String = "",
  val stars: Long,
  @Json(name = "idempotency_key") val idempotencyKey: String,
)

@JsonClass(generateAdapter = true)
data class SendStarsResponseDto(
  val success: Boolean = false,
  @Json(name = "message_id") val messageId: String = "",
  @Json(name = "conversation_id") val conversationId: String = "",
  @Json(name = "stars_sent") val starsSent: Long = 0L,
  @Json(name = "sender_wallet") val senderWallet: StarWalletDto = StarWalletDto(),
  @Json(name = "insufficient_balance") val insufficientBalance: Boolean = false,
  @Json(name = "required_buy_stars") val requiredBuyStars: Long = 0L,
  @Json(name = "server_epoch_ms") val serverEpochMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class StartCallRequestDto(
  @Json(name = "peer_user_id") val peerUserId: String,
  @Json(name = "conversation_id") val conversationId: String = "",
  @Json(name = "call_type") val callType: String = "audio",
  @Json(name = "sdp_offer") val sdpOffer: String = "",
)

@JsonClass(generateAdapter = true)
data class StartCallResponseDto(
  val success: Boolean = false,
  val allowed: Boolean = false,
  val code: String = "",
  val message: String = "",
  @Json(name = "peer_online") val peerOnline: Boolean = false,
  @Json(name = "auto_request_sent") val autoRequestSent: Boolean = false,
  val call: CallRecordDto = CallRecordDto(),
  @Json(name = "ice_servers") val iceServers: List<IceServerDto> = emptyList(),
  @Json(name = "timeout_sec") val timeoutSec: Int = 25,
)

@JsonClass(generateAdapter = true)
data class CallSignalRequestDto(
  val action: String,
  val sdp: String = "",
  @Json(name = "sdp_type") val sdpType: String = "",
  val candidate: String = "",
  @Json(name = "sdp_mid") val sdpMid: String = "",
  @Json(name = "sdp_mline_index") val sdpMLineIndex: Int = 0,
  val reason: String = "",
  @Json(name = "cam_enabled") val camEnabled: Boolean? = null,
  @Json(name = "mic_enabled") val micEnabled: Boolean? = null,
  @Json(name = "in_app_active") val inAppActive: Boolean? = null,
  @Json(name = "screen_sharing") val screenSharing: Boolean? = null,
  @Json(name = "video_filter") val videoFilter: String = "",
  @Json(name = "network_quality") val networkQuality: String = "",
)

@JsonClass(generateAdapter = true)
data class BufferedSignalEventDto(
  val seq: Long = 0L,
  @Json(name = "from_user_id") val fromUserId: String = "",
  val action: String = "",
  val sdp: String = "",
  @Json(name = "sdp_type") val sdpType: String = "",
  val candidate: String = "",
  @Json(name = "sdp_mid") val sdpMid: String = "",
  @Json(name = "sdp_mline_index") val sdpMLineIndex: Int = 0,
  @Json(name = "cam_enabled") val camEnabled: Boolean? = null,
  @Json(name = "mic_enabled") val micEnabled: Boolean? = null,
  @Json(name = "in_app_active") val inAppActive: Boolean? = null,
  @Json(name = "screen_sharing") val screenSharing: Boolean? = null,
  @Json(name = "video_filter") val videoFilter: String = "",
  @Json(name = "network_quality") val networkQuality: String = "",
  @Json(name = "timestamp_ms") val timestampMs: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class CallStateResponseDto(
  val success: Boolean = false,
  val call: CallRecordDto = CallRecordDto(),
  @Json(name = "sdp_offer") val sdpOffer: String = "",
  val signals: List<BufferedSignalEventDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class CallActionResponseDto(
  val success: Boolean = false,
  val call: CallRecordDto = CallRecordDto(),
)

interface TelefamApiService {
  @GET("v1/auth/check-email")
  suspend fun checkEmail(
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Query("email") email: String,
  ): Response<EmailCheckResponseDto>

  @POST("v1/auth/otp/request")
  suspend fun requestOtp(
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Header("X-Telefam-Client-IP-Hint") ipFingerprint: String,
    @Body body: RequestOtpDto,
  ): Response<RequestOtpResponseDto>

  @POST("v1/auth/otp/verify")
  suspend fun verifyOtp(
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Header("X-Telefam-Client-IP-Hint") ipFingerprint: String,
    @Body body: VerifyOtpDto,
  ): Response<AuthSessionResponseDto>

  @POST("v1/auth/verify-link")
  suspend fun verifyLink(
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Header("X-Telefam-Client-IP-Hint") ipFingerprint: String,
    @Body body: VerifyLinkDto,
  ): Response<AuthSessionResponseDto>

  @POST("v1/auth/token/refresh")
  suspend fun refreshToken(
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: RefreshTokenDto,
  ): Response<AuthSessionResponseDto>

  @POST("v1/auth/oauth/google")
  suspend fun verifyGoogleOAuth(
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Header("X-Telefam-Client-IP-Hint") ipFingerprint: String,
    @Body body: OAuthDto,
  ): Response<AuthSessionResponseDto>

  @GET("v1/auth/login-requests/pending")
  suspend fun getPendingLoginRequests(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
  ): Response<PendingLoginRequestsResponseDto>

  @GET("v1/auth/login-requests/{id}/status")
  suspend fun getLoginRequestStatus(
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Header("X-Telefam-Poll-Token") pollToken: String,
    @Path("id") requestId: String,
  ): Response<LoginApprovalStatusResponseDto>

  @POST("v1/auth/login-requests/{id}/respond")
  suspend fun respondToLoginRequest(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("id") requestId: String,
    @Body body: RespondLoginRequestDto,
  ): Response<RespondLoginRequestResponseDto>

  @POST("v1/telemetry/device-context")
  suspend fun syncDeviceContext(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: DeviceContextTelemetryDto,
  ): Response<DeviceContextTelemetryResponseDto>

  @GET("v1/profile/check-username")
  suspend fun checkUsername(
    @Query("u") username: String,
  ): Response<UsernameCheckResponseDto>

  @POST("v1/media/avatar")
  suspend fun uploadAvatar(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Query("cx") cropCenterX: Float,
    @Query("cy") cropCenterY: Float,
    @Query("scale") cropScale: Float,
    @Body imageBody: RequestBody,
  ): Response<MediaUploadResponseDto>

  @POST("v1/media/background")
  suspend fun uploadProfileBackground(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body imageBody: RequestBody,
  ): Response<ProfileBackgroundUploadResponseDto>

  @POST("v1/media/photo")
  suspend fun uploadPostPhoto(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body imageBody: RequestBody,
  ): Response<MediaUploadResponseDto>

  @Multipart
  @POST("v1/media/video")
  suspend fun uploadPostVideo(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Header("X-Telefam-Frame-Hashes") frameHashesCsv: String,
    @Query("duration_ms") durationMs: Long,
    @Query("trim_start_ms") trimStartMs: Long,
    @Query("trim_end_ms") trimEndMs: Long,
    @Query("width") width: Int,
    @Query("height") height: Int,
    @Part videoPart: MultipartBody.Part,
    @Part thumbnailPart: MultipartBody.Part? = null,
  ): Response<MediaUploadResponseDto>

  @GET("v1/songs/search")
  suspend fun searchSongs(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Query("q") query: String,
  ): Response<SongsSearchResponseDto>

  @GET("v1/hashtags/search")
  suspend fun searchHashtags(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Query("q") query: String,
  ): Response<HashtagsSearchResponseDto>

  @POST("v1/posts")
  suspend fun createPost(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: CreatePostRequestDto,
  ): Response<CreatePostResponseDto>

  @PATCH("v1/posts/{id}")
  suspend fun updatePost(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("id") postId: String,
    @Body body: UpdatePostRequestDto,
  ): Response<CreatePostResponseDto>

  @DELETE("v1/posts/{id}")
  suspend fun deletePost(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("id") postId: String,
  ): Response<GenericSuccessDto>

  @GET("v1/feeds")
  suspend fun getFeeds(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Query("tab") tab: String,
    @Query("page") page: Int,
    @Query("page_size") pageSize: Int,
  ): Response<FeedPageResponseDto>

  @GET("v1/feeds/search")
  suspend fun searchFeeds(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Query("q") query: String,
    @Query("shortcut") shortcut: String,
    @Query("page") page: Int,
    @Query("page_size") pageSize: Int,
  ): Response<FeedSearchResponseDto>

  @POST("v1/feeds/interact")
  suspend fun interactFeed(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: FeedInteractionRequestDto,
  ): Response<FeedInteractionResponseDto>

  @POST("v1/feeds/interact/batch")
  suspend fun interactFeedBatch(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: FeedInteractionBatchRequestDto,
  ): Response<SyncBatchResponseDto>

  @POST("v1/profile/setup")
  suspend fun setupProfile(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: ProfileSetupDto,
  ): Response<GenericSuccessDto>

  @PATCH("v1/profile")
  suspend fun updateProfile(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: ProfileUpdateRequestDto,
  ): Response<ProfileUpdateResponseDto>

  @POST("v1/profile/backgrounds")
  suspend fun attachProfileBackground(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: ProfileBackgroundAttachDto,
  ): Response<ProfileBackgroundListResponseDto>

  @PUT("v1/profile/backgrounds")
  suspend fun reorderProfileBackgrounds(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: ProfileBackgroundReorderDto,
  ): Response<ProfileBackgroundListResponseDto>

  @DELETE("v1/profile/backgrounds/{id}")
  suspend fun deleteProfileBackground(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("id") mediaId: String,
  ): Response<ProfileBackgroundListResponseDto>

  @POST("v1/audit/event")
  suspend fun recordAuditEvent(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: AuditEventDto,
  ): Response<GenericSuccessDto>

  @POST("v1/sync/batch")
  suspend fun syncBatch(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: SyncBatchRequestDto,
  ): Response<SyncBatchResponseDto>

  @GET("v1/contacts")
  suspend fun listContacts(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Query("filter") filter: String,
    @Query("q") query: String,
    @Query("page") page: Int,
    @Query("page_size") pageSize: Int,
  ): Response<ContactsPageResponseDto>

  @POST("v1/contacts/follow")
  suspend fun followContact(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: ContactActionDto,
  ): Response<FollowMutationResponseDto>

  @POST("v1/contacts/unfollow")
  suspend fun unfollowContact(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: ContactActionDto,
  ): Response<FollowMutationResponseDto>

  @POST("v1/contacts/sync")
  suspend fun syncContactActions(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: ContactSyncBatchRequestDto,
  ): Response<SyncBatchResponseDto>

  @GET("v1/users/{id}/profile")
  suspend fun getUserProfile(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("id") userId: String,
  ): Response<UserProfileResponseDto>

  @GET("v1/users/{id}/connections")
  suspend fun getUserConnections(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("id") userId: String,
    @Query("type") type: String, // "followers" or "following"
    @Query("page") page: Int = 1,
    @Query("page_size") pageSize: Int = 30,
  ): Response<UserConnectionsResponseDto>

  @GET("v1/users/{id}/followers")
  suspend fun getUserFollowers(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("id") userId: String,
    @Query("page") page: Int = 1,
    @Query("page_size") pageSize: Int = 30,
  ): Response<UserConnectionsResponseDto>

  @GET("v1/users/{id}/following")
  suspend fun getUserFollowing(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("id") userId: String,
    @Query("page") page: Int = 1,
    @Query("page_size") pageSize: Int = 30,
  ): Response<UserConnectionsResponseDto>

  @POST("v1/users/{id}/block")
  suspend fun blockUser(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("id") userId: String,
  ): Response<UserBlockResponseDto>

  @POST("v1/users/{id}/unblock")
  suspend fun unblockUser(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("id") userId: String,
  ): Response<UserBlockResponseDto>

  @POST("v1/users/{id}/report")
  suspend fun reportUser(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("id") userId: String,
    @Body body: UserReportRequestDto,
  ): Response<UserReportResponseDto>

  @POST("v1/users/{id}/mute")
  suspend fun muteUser(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("id") userId: String,
  ): Response<UserMuteResponseDto>

  @POST("v1/users/{id}/unmute")
  suspend fun unmuteUser(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("id") userId: String,
  ): Response<UserMuteResponseDto>

  @GET("v1/settings")
  suspend fun getSettings(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
  ): Response<SettingsBundleResponseDto>

  @PUT("v1/settings/privacy")
  suspend fun updatePrivacySettings(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: PrivacySettingsDto,
  ): Response<SettingsBundleResponseDto>

  @PUT("v1/settings/notifications")
  suspend fun updateNotificationSettings(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: NotificationSettingsDto,
  ): Response<SettingsBundleResponseDto>

  @PUT("v1/settings/content-preferences")
  suspend fun updateContentPreferences(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: ContentPreferencesDto,
  ): Response<SettingsBundleResponseDto>

  @GET("v1/settings/blocked-accounts")
  suspend fun listBlockedAccounts(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Query("q") query: String = "",
  ): Response<RestrictedAccountsResponseDto>

  @GET("v1/settings/muted-accounts")
  suspend fun listMutedAccounts(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Query("q") query: String = "",
  ): Response<RestrictedAccountsResponseDto>

  @POST("v1/support/reports")
  suspend fun submitProblemReport(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: ProblemReportRequestDto,
  ): Response<ProblemReportResponseDto>

  @GET("v1/support/tickets")
  suspend fun listSupportTickets(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
  ): Response<SupportTicketsResponseDto>

  @GET("v1/support/faqs")
  suspend fun searchFaqs(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Query("q") query: String = "",
  ): Response<FaqListResponseDto>

  @GET("v1/settings/system-status")
  suspend fun getSystemStatus(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
  ): Response<SystemStatusResponseDto>

  @GET("v1/legal/documents")
  suspend fun getLegalDocuments(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
  ): Response<LegalDocumentsResponseDto>

  @GET("v1/app/version")
  suspend fun checkAppVersion(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Query("version") version: String,
  ): Response<AppVersionResponseDto>

  @POST("v1/auth/logout")
  suspend fun logout(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: LogoutRequestDto,
  ): Response<GenericSuccessDto>

  @GET("v1/chat/privacy")
  suspend fun getChatPrivacy(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
  ): Response<ChatPrivacyResponseDto>

  @PUT("v1/chat/privacy")
  suspend fun updateChatPrivacy(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: ChatPrivacySettingsDto,
  ): Response<ChatPrivacyResponseDto>

  @PUT("v1/chat/theme")
  suspend fun updateChatTheme(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: UpdateChatThemeRequestDto,
  ): Response<ChatPrivacyResponseDto>

  @GET("v1/settings/sync")
  suspend fun getSyncSettings(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
  ): Response<SyncSettingsResponseDto>

  @PUT("v1/settings/sync")
  suspend fun updateSyncSettings(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: SyncPreferencesDto,
  ): Response<SyncSettingsResponseDto>

  @POST("v1/sync/now")
  suspend fun syncNow(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: ManualSyncRequestDto,
  ): Response<ManualSyncResponseDto>

  // 1-on-1 E2EE Chat, Signal Key Bundles, Voice Notes, View-Once, Security Flags & Gifly Hub
  @PUT("v1/chat/e2ee/keys")
  suspend fun uploadE2eeKeys(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: E2eeKeyBundleUploadDto,
  ): Response<GenericSuccessDto>

  @GET("v1/chat/e2ee/keys/{userId}")
  suspend fun fetchPeerE2eeKeys(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("userId") userId: String,
  ): Response<E2eeKeyBundleResponseDto>

  @GET("v1/chat/conversations")
  suspend fun listChatConversations(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Query("archived") archived: Boolean = false,
  ): Response<ChatConversationsResponseDto>

  @POST("v1/chat/conversations/direct/{userId}")
  suspend fun openDirectConversation(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("userId") userId: String,
  ): Response<OpenDirectConversationResponseDto>

  @POST("v1/chat/conversations/batch-action")
  suspend fun batchConversationAction(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: BatchConversationActionRequestDto,
  ): Response<ChatConversationsResponseDto>

  @GET("v1/chat/conversations/{id}/messages")
  suspend fun listChatMessages(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("id") conversationId: String,
    @Query("since_ms") sinceMs: Long? = null,
  ): Response<ChatMessagesListResponseDto>

  @POST("v1/chat/conversations/{id}/messages")
  suspend fun sendChatMessage(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("id") conversationId: String,
    @Body body: SendChatMessageRequestDto,
  ): Response<ChatMessageSingleResponseDto>

  @PATCH("v1/chat/messages/{id}")
  suspend fun editChatMessage(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("id") messageId: String,
    @Body body: EditChatMessageRequestDto,
  ): Response<ChatMessageSingleResponseDto>

  @DELETE("v1/chat/messages/{id}")
  suspend fun deleteChatMessage(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("id") messageId: String,
    @Query("mode") mode: String = "unsend",
  ): Response<GenericSuccessDto>

  @POST("v1/chat/messages/{id}/receipt")
  suspend fun sendChatReceipt(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("id") messageId: String,
    @Body body: ChatReceiptRequestDto,
  ): Response<ChatMessageSingleResponseDto>

  @POST("v1/chat/messages/{id}/view-once/open")
  suspend fun openViewOnceVoice(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("id") messageId: String,
  ): Response<ViewOnceOpenResponseDto>

  @POST("v1/chat/messages/{id}/view-once/consume")
  suspend fun consumeViewOnceVoice(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("id") messageId: String,
  ): Response<GenericSuccessDto>

  @Multipart
  @POST("v1/chat/media/voice")
  suspend fun uploadVoiceMedia(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Part("conversation_id") conversationId: RequestBody,
    @Part("duration_ms") durationMs: RequestBody,
    @Part("view_once") viewOnce: RequestBody,
    @Part file: MultipartBody.Part,
  ): Response<VoiceMediaUploadResponseDto>

  @POST("v1/chat/conversations/{id}/flags")
  suspend fun recordSecurityFlag(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("id") conversationId: String,
    @Body body: RecordSecurityFlagRequestDto,
  ): Response<SecurityFlagResponseDto>

  @GET("v1/chat/conversations/{id}/flags")
  suspend fun listSecurityFlags(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("id") conversationId: String,
  ): Response<SecurityFlagsListResponseDto>

  @GET("v1/chat/stickers/greeting")
  suspend fun getGreetingSticker(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Query("peer_id") peerId: String = "",
  ): Response<GiflyGreetingResponseDto>

  @GET("v1/chat/stickers/search")
  suspend fun searchGiflyMedia(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Query("type") type: String = "stickers",
    @Query("q") query: String = "",
    @Query("page") page: Int = 1,
    @Query("per_page") perPage: Int = 24,
  ): Response<GiflySearchResponseDto>

  @GET("v1/chat/search")
  suspend fun searchChatsAndPeople(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Query("q") query: String = "",
  ): Response<ChatGlobalSearchResponseDto>

  @GET("v1/links/preview")
  suspend fun getLinkPreview(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Query("url") url: String,
  ): Response<LinkPreviewResponseDto>

  @GET("v1/links/saved")
  suspend fun listSavedLinks(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Query("q") query: String = "",
  ): Response<SavedLinksListResponseDto>

  @POST("v1/links/saved")
  suspend fun saveLink(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: SaveLinkRequestDto,
  ): Response<SaveLinkResponseDto>

  @DELETE("v1/links/saved/{id}")
  suspend fun deleteSavedLink(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("id") id: String,
  ): Response<GenericSuccessDto>

  @POST("v1/notifications/push-token")
  suspend fun registerPushToken(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: RegisterPushTokenRequestDto,
  ): Response<GenericSuccessDto>

  @DELETE("v1/notifications/push-token")
  suspend fun unregisterPushToken(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Query("all_devices") allDevices: Boolean = false,
  ): Response<GenericSuccessDto>

  @GET("v1/notifications")
  suspend fun listNotifications(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Query("limit") limit: Int = 50,
  ): Response<NotificationsListResponseDto>

  @POST("v1/notifications/read")
  suspend fun markNotificationRead(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: MarkNotificationReadRequestDto,
  ): Response<NotificationUnreadCountResponseDto>

  @POST("v1/notifications/read-all")
  suspend fun markAllNotificationsRead(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
  ): Response<NotificationUnreadCountResponseDto>

  @GET("v1/calls/ice-config")
  suspend fun getCallIceConfig(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
  ): Response<IceConfigResponseDto>

  @POST("v1/calls")
  suspend fun startCall(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Body body: StartCallRequestDto,
  ): Response<StartCallResponseDto>

  @GET("v1/calls/{callId}")
  suspend fun getCallState(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("callId") callId: String,
    @Query("since_seq") sinceSeq: Long = 0L,
  ): Response<CallStateResponseDto>

  @POST("v1/calls/{callId}/signal")
  suspend fun sendCallSignal(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Path("callId") callId: String,
    @Body body: CallSignalRequestDto,
  ): Response<CallActionResponseDto>

  @GET("v1/stars/summary")
  suspend fun getStarsSummary(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Query("country_iso") countryIso: String = "",
  ): Response<StarsSummaryResponseDto>

  @POST("v1/stars/checkout/init")
  suspend fun initStarsCheckout(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Header("Idempotency-Key") idempotencyKey: String,
    @Body body: StarsCheckoutInitRequestDto,
  ): Response<StarsCheckoutInitResponseDto>

  @POST("v1/stars/checkout/verify")
  suspend fun verifyStarsCheckout(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Header("Idempotency-Key") idempotencyKey: String,
    @Body body: StarsCheckoutVerifyRequestDto,
  ): Response<StarsCheckoutVerifyResponseDto>

  @POST("v1/stars/send")
  suspend fun sendStarsInChat(
    @Header("Authorization") bearer: String,
    @Header("X-Telefam-Device-FP") deviceFingerprint: String,
    @Header("Idempotency-Key") idempotencyKey: String,
    @Body body: SendStarsRequestDto,
  ): Response<SendStarsResponseDto>

  @GET
  suspend fun downloadMedia(@Url url: String): Response<ResponseBody>

  companion object {
    fun isAllowedBaseUrl(baseUrl: String, enforceHttps: Boolean): Boolean {
      val trimmed = baseUrl.trim()
      if (trimmed.isBlank()) return false
      val parsed = runCatching { URI(trimmed) }.getOrNull() ?: return false
      val scheme = parsed.scheme?.lowercase() ?: return false
      val host = parsed.host?.lowercase() ?: return false
      if (scheme == "https") return true
      if (scheme == "http") {
        val isLoopback = host == "127.0.0.1" || host == "localhost" || host == "10.0.2.2" || host == "::1"
        return !enforceHttps || isLoopback
      }
      return false
    }

    fun create(
      baseUrl: String,
      enforceHttps: Boolean = !BuildConfig.DEBUG,
      deviceHeaderProvider: (() -> Map<String, String>)? = null,
    ): TelefamApiService? {
      val trimmed = baseUrl.trim()
      if (!isAllowedBaseUrl(trimmed, enforceHttps)) return null
      val normalizedUrl = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
      val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
      val clientBuilder = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
      if (deviceHeaderProvider != null) {
        clientBuilder.addInterceptor { chain ->
          val reqBuilder = chain.request().newBuilder()
          deviceHeaderProvider().forEach { (k, v) ->
            if (v.isNotBlank() && chain.request().header(k) == null) {
              reqBuilder.header(k, v)
            }
          }
          chain.proceed(reqBuilder.build())
        }
      }
      // Aggressively gzip-compress JSON request bodies (>= 256 bytes) for weak connections
      clientBuilder.addInterceptor { chain ->
        val orig = chain.request()
        val body = orig.body
        val mediaSubtype = body?.contentType()?.subtype?.lowercase().orEmpty()
        if (body != null &&
          orig.header("Content-Encoding") == null &&
          mediaSubtype.contains("json")
        ) {
          val rawLen = runCatching { body.contentLength() }.getOrDefault(-1L)
          if (rawLen < 0L || rawLen >= 256L) {
            val buffer = okio.Buffer()
            body.writeTo(buffer)
            if (buffer.size >= 256L) {
              val compressed = okio.Buffer()
              okio.GzipSink(compressed).use { gzipSink ->
                gzipSink.write(buffer, buffer.size)
              }
              val gzipBody = object : RequestBody() {
                override fun contentType() = body.contentType()
                override fun contentLength() = compressed.size
                override fun writeTo(sink: okio.BufferedSink) {
                  sink.writeAll(compressed.clone())
                }
              }
              val gzippedReq = orig.newBuilder()
                .header("Content-Encoding", "gzip")
                .method(orig.method, gzipBody)
                .build()
              return@addInterceptor chain.proceed(gzippedReq)
            }
          }
        }
        chain.proceed(orig)
      }
      val okHttp = clientBuilder.build()

      return Retrofit.Builder()
        .baseUrl(normalizedUrl)
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(TelefamApiService::class.java)
    }
  }
}
