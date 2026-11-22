package telefam.company.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Patterns
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.telefam.company.app.BuildConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import telefam.company.calls.IncomingCallPushPayload
import telefam.company.calls.TelefamCallController
import telefam.company.chat.ChatFeatureController
import telefam.company.chat.TelefamChatUiState
import telefam.company.core.AdaptiveStreamSelector
import telefam.company.core.AppVersionCheckResult
import telefam.company.core.ChatAudienceOption
import telefam.company.core.ChatPrivacyRoute
import telefam.company.core.ChatPrivacySettingsState
import telefam.company.core.ContactFilterTab
import telefam.company.core.TelefamChatPrivacyCatalog
import telefam.company.core.ContactRelationshipState
import telefam.company.core.ContentPreferencesState
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
import telefam.company.core.OwnerProfileTab
import telefam.company.core.SyncPreferencesState
import telefam.company.core.PostFontOption
import telefam.company.core.PostMode
import telefam.company.core.PostPrivacy
import telefam.company.core.PostSongTrack
import telefam.company.core.PostUploadStatus
import telefam.company.core.PrivacySettingsState
import telefam.company.core.ProfileBackgroundItem
import telefam.company.core.ProfilePostFilter
import telefam.company.core.ProfilePostSort
import telefam.company.core.RestrictedAccountItem
import telefam.company.core.SettingsDestination
import telefam.company.core.SettingsSnapshot
import telefam.company.core.SmartTextDetector
import telefam.company.core.SupportTicketItem
import telefam.company.core.SystemServiceStatusItem
import telefam.company.core.TelefamContactsRules
import telefam.company.core.TelefamCoreEngine
import telefam.company.core.TelefamPostRules
import telefam.company.core.TelefamProfileSanitizer
import telefam.company.core.TelefamSettingsContentCatalog
import telefam.company.core.VideoQualityOption
import telefam.company.data.local.CachedFeedPostEntity
import telefam.company.data.local.CachedLinkPreviewEntity
import telefam.company.data.local.CachedNotificationEntity
import telefam.company.data.local.SavedLinkEntity
import telefam.company.data.model.CountryCatalog
import telefam.company.data.model.CountryDialOption
import telefam.company.data.network.ContactItemDto
import telefam.company.data.network.FeedItemDto
import telefam.company.data.network.ProfileBackgroundItemDto
import telefam.company.data.network.UserProfileResponseDto
import telefam.company.data.repository.AuthActionResult
import telefam.company.data.repository.ContactToggleResult
import telefam.company.data.repository.LoginApprovalPollOutcome
import telefam.company.data.repository.PostPublishResult
import telefam.company.data.repository.PostSubmissionInput
import telefam.company.data.repository.ProfileActionResult
import telefam.company.data.repository.ProfileBackgroundResult
import telefam.company.data.repository.TelefamRepository
import telefam.company.media.PreparedPostVideo
import telefam.company.media.ProcessedAvatarMedia
import telefam.company.media.ProcessedPostPhoto
import telefam.company.media.TelefamVideoCacheManager
import telefam.company.media.TelefamVideoDownloader
import telefam.company.media.VideoDownloadResult
import telefam.company.media.VideoPrepareResult
import telefam.company.ui.theme.ThemeMode
import java.io.File

enum class AuthStep {
  EMAIL_INPUT,
  OTP_VERIFICATION,
  DEVICE_APPROVAL_WAIT,
  DEVICE_APPROVAL_DENIED,
}

data class DeviceApprovalWaitState(
  val email: String,
  val requestId: String,
  val pollToken: String,
  val targetDeviceName: String,
  val targetLocation: String,
  val requestTimeMs: Long,
  val remainingSeconds: Long,
  val expiresInSeconds: Long = 60L,
  val tryAnotherWayProvider: String = "google",
)

data class IncomingLoginRequestState(
  val requestId: String,
  val deviceName: String,
  val location: String,
  val ipAddressMasked: String,
  val requestedAtMs: Long,
  val remainingSeconds: Long,
)

data class LoginApprovedOverlayState(
  val deviceName: String,
  val location: String,
  val requestedAtMs: Long,
)

enum class HomeTab {
  CHATS,
  CONTACTS,
  POST,
  FEEDS,
  PROFILE,
}

data class ProfileFormState(
  val fullName: String = "",
  val username: String = "",
  val isUsernameAvailable: Boolean? = null,
  val selectedCountry: CountryDialOption = CountryCatalog.defaultCountry,
  val phoneDigits: String = "",
  val gender: String = "",
  val dobYear: Int? = null,
  val dobMonthZeroBased: Int? = null,
  val dobDay: Int? = null,
  val dobDisplay: String = "",
  val location: String = "",
  val bio: String = "",
  val rawBitmapForCrop: Bitmap? = null,
  val processedAvatar: ProcessedAvatarMedia? = null,
  val isSaving: Boolean = false,
  val errorMessage: String? = null,
)

data class ContactUiItem(
  val userId: String,
  val fullName: String,
  val username: String,
  val countryDisplay: String,
  val avatarLocalPath: String?,
  val isFollowing: Boolean,
  val followsYou: Boolean,
  val relationshipState: ContactRelationshipState,
  val initials: String,
)

data class ContactsUiState(
  val activeFilter: ContactFilterTab = ContactFilterTab.ALL,
  val searchQuery: String = "",
  val items: List<ContactUiItem> = emptyList(),
  val isLoading: Boolean = false,
  val isLoadingMore: Boolean = false,
  val hasMore: Boolean = false,
  val currentPage: Int = 1,
  val toastMessage: String? = null,
)

data class FeedUiItem(
  val postId: String,
  val authorUserId: String,
  val authorUsername: String,
  val authorFullName: String,
  val authorAvatarUrl: String?,
  val isFollowingAuthor: Boolean,
  val isOwner: Boolean,
  val privacy: PostPrivacy = PostPrivacy.PUBLIC,
  val mode: PostMode,
  val textContent: String,
  val caption: String,
  val themeId: String,
  val fontOption: PostFontOption,
  val song: PostSongTrack?,
  val hashtags: List<String>,
  val photoUrls: List<String>,
  val primaryStreamUrl: String,
  val rawDownloadUrl: String,
  val thumbnailUrl: String,
  val renditions: List<FeedVideoRendition>,
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
  val viewsCount: Long = 0L,
  val isLiked: Boolean,
  val isFavourited: Boolean,
  val isReposted: Boolean,
  val repostedByUsername: String,
  val recommendationLabel: String,
  val createdAtMs: Long,
)

data class UserProfileScreenState(
  val userId: String,
  val fullName: String = "",
  val username: String = "",
  val avatarUrl: String? = null,
  val bio: String = "",
  val location: String = "",
  val countryIso: String = "",
  val gender: String = "",
  val dateOfBirth: String = "",
  val phoneNumber: String = "",
  val usernameChangedAtMs: Long = 0L,
  val nextUsernameChangeMs: Long = 0L,
  val backgrounds: List<ProfileBackgroundItem> = emptyList(),
  val followingCount: Long = 0L,
  val followersCount: Long = 0L,
  val likesCount: Long = 0L,
  val isFollowing: Boolean = false,
  val followsYou: Boolean = false,
  val isOwner: Boolean = false,
  val isBlocked: Boolean = false,
  val isMuted: Boolean = false,
  val isPrivateAccount: Boolean = false,
  val whoCanComment: String = "everyone",
  val whoCanMention: String = "everyone",
  val likesVisibility: String = "everyone",
  val followingVisibility: String = "everyone",
  val showActivityStatus: Boolean = true,
  val relationshipState: ContactRelationshipState = ContactRelationshipState.FOLLOW,
  val activeOwnerTab: OwnerProfileTab = OwnerProfileTab.POSTS,
  val posts: List<FeedUiItem> = emptyList(),
  val savedPosts: List<FeedUiItem> = emptyList(),
  val repostedPosts: List<FeedUiItem> = emptyList(),
  val lockedPosts: List<FeedUiItem> = emptyList(),
  val selectedFilter: ProfilePostFilter = ProfilePostFilter.ALL,
  val selectedSort: ProfilePostSort = ProfilePostSort.LATEST,
  val isLoading: Boolean = true,
  val activeConnectionsType: String? = null,
  val connections: List<ContactUiItem> = emptyList(),
  val isLoadingConnections: Boolean = false,
  val connectionsPage: Int = 1,
  val connectionsHasMore: Boolean = false,
) {
  val activeTabPosts: List<FeedUiItem>
    get() = when {
      !isOwner -> posts
      activeOwnerTab == OwnerProfileTab.POSTS -> posts
      activeOwnerTab == OwnerProfileTab.SAVED -> savedPosts
      activeOwnerTab == OwnerProfileTab.REPOSTS -> repostedPosts
      activeOwnerTab == OwnerProfileTab.LOCKED -> lockedPosts
      else -> posts
    }

  val filteredAndSortedPosts: List<FeedUiItem>
    get() {
      val base = activeTabPosts
      val filtered = when (selectedFilter) {
        ProfilePostFilter.ALL -> base
        ProfilePostFilter.VIDEOS -> base.filter { it.mode == PostMode.VIDEO }
        ProfilePostFilter.PHOTOS -> base.filter { it.mode == PostMode.PHOTOS }
        ProfilePostFilter.TEXT -> base.filter { it.mode == PostMode.TEXT }
      }
      return when (selectedSort) {
        ProfilePostSort.LATEST -> filtered.sortedByDescending { it.createdAtMs }
        ProfilePostSort.MOST_VIEWED -> filtered.sortedByDescending { it.viewsCount }
        ProfilePostSort.MOST_LIKED -> filtered.sortedByDescending { it.likesCount }
      }
    }
}

data class FeedsUiState(
  val activeTab: FeedTab = FeedTab.FOR_YOU,
  val followingItems: List<FeedUiItem> = emptyList(),
  val forYouItems: List<FeedUiItem> = emptyList(),
  val isLoading: Boolean = false,
  val isLoadingMore: Boolean = false,
  val followingPage: Int = 1,
  val forYouPage: Int = 1,
  val hasMoreFollowing: Boolean = false,
  val hasMoreForYou: Boolean = false,
  val streamAccessToken: String = "",
  val streamDeviceFp: String = "",
  val isSearchOpen: Boolean = false,
  val searchQuery: String = "",
  val activeShortcut: String = "",
  val searchShortcuts: List<FeedSearchShortcut> = emptyList(),
  val searchResults: List<FeedUiItem> = emptyList(),
  val isSearching: Boolean = false,
  val searchViewerItems: List<FeedUiItem>? = null,
  val searchViewerInitialIndex: Int = 0,
  val toastMessage: String? = null,
) {
  val activeItems: List<FeedUiItem>
    get() = if (activeTab == FeedTab.FOLLOWING) followingItems else forYouItems

  val hasMoreActive: Boolean
    get() = if (activeTab == FeedTab.FOLLOWING) hasMoreFollowing else hasMoreForYou
}

data class PostComposerUiState(
  val isOpen: Boolean = false,
  val editingPostId: String? = null,
  val editingThumbnailUrl: String = "",
  val mode: PostMode = PostMode.TEXT,
  val textContent: String = "",
  val caption: String = "",
  val selectedThemeId: String = "crimson",
  val selectedFont: PostFontOption = PostFontOption.CLASSIC,
  val photos: List<ProcessedPostPhoto> = emptyList(),
  val video: PreparedPostVideo? = null,
  val videoTrimStartMs: Long = 0L,
  val videoTrimEndMs: Long = 0L,
  val muteOriginalAudio: Boolean = false,
  val selectedSong: PostSongTrack? = null,
  val taggedPeople: List<ContactUiItem> = emptyList(),
  val selectedHashtags: List<String> = emptyList(),
  val showTagPeopleSheet: Boolean = false,
  val tagPeopleQuery: String = "",
  val allDirectoryPeople: List<ContactUiItem> = emptyList(),
  val pendingTaggedIds: Set<String> = emptySet(),
  val showSongSheet: Boolean = false,
  val songQuery: String = "",
  val songResults: List<PostSongTrack> = emptyList(),
  val isSearchingSongs: Boolean = false,
  val playingPreviewTrackId: String? = null,
  val showHashtagsSheet: Boolean = false,
  val hashtagQuery: String = "",
  val hashtagResults: List<HashtagItem> = emptyList(),
  val isSearchingHashtags: Boolean = false,
  val uploadStatus: PostUploadStatus = PostUploadStatus.IDLE,
  val uploadProgressPct: Int = 0,
  val pendingQueueCount: Int = 0,
  val errorMessage: String? = null,
) {
  val canShare: Boolean
    get() = uploadStatus != PostUploadStatus.PREPARING &&
      uploadStatus != PostUploadStatus.UPLOADING &&
      when (mode) {
        PostMode.TEXT -> textContent.trim().isNotEmpty()
        PostMode.PHOTOS -> photos.isNotEmpty() || editingPostId != null
        PostMode.VIDEO -> (video != null && videoTrimEndMs > videoTrimStartMs) || editingPostId != null
      }
}

sealed interface LinkPreviewUiState {
  data class Loading(val normalizedUrl: String, val domain: String) : LinkPreviewUiState
  data class Loaded(val preview: CachedLinkPreviewEntity) : LinkPreviewUiState
}

data class InAppBrowserSessionState(
  val url: String,
  val source: String = "chat",
  val titleHint: String = "",
)

data class TelefamUiState(
  val isSessionInitialized: Boolean = false,
  val isAuthenticated: Boolean = false,
  val profileCompleted: Boolean = false,
  val userId: String = "",
  val userEmail: String = "",
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val authStep: AuthStep = AuthStep.EMAIL_INPUT,
  val emailInput: String = "",
  val emailAccountNotice: String? = null,
  val otpCode: String = "",
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
  val cooldownRemainingSeconds: Long = 0L,
  val activeCooldownDurationSeconds: Long = 60L,
  val networkTier: NetworkTier = NetworkTier.GOOD,
  val profileForm: ProfileFormState = ProfileFormState(),
  val savedAvatarLocalPath: String? = null,
  val activeHomeTab: HomeTab = HomeTab.CHATS,
  val isBottomBarVisible: Boolean = true,
  val contactsState: ContactsUiState = ContactsUiState(),
  val feedsState: FeedsUiState = FeedsUiState(),
  val postComposer: PostComposerUiState = PostComposerUiState(),
  val profileBackStack: List<UserProfileScreenState> = emptyList(),
  val ownProfileState: UserProfileScreenState? = null,
  val profileViewerItems: List<FeedUiItem>? = null,
  val profileViewerInitialIndex: Int = 0,
  val profileToastMessage: String? = null,
  val comingSoonMessage: String? = null,
  val showSettingsModal: Boolean = false,
  val showProfileMenuModal: Boolean = false,
  val settings: SettingsSnapshot = SettingsSnapshot(),
  val syncStatus: OfflineSyncStatusSnapshot = OfflineSyncStatusSnapshot(),
  val isWifiConnected: Boolean = true,
  val settingsBackStack: List<SettingsDestination> = emptyList(),
  val settingsSearchQuery: String = "",
  val restrictedAccountsTab: Int = 0,
  val restrictedAccountsQuery: String = "",
  val blockedAccounts: List<RestrictedAccountItem> = emptyList(),
  val mutedAccounts: List<RestrictedAccountItem> = emptyList(),
  val isLoadingRestrictedAccounts: Boolean = false,
  val supportTickets: List<SupportTicketItem> = emptyList(),
  val isLoadingSupportTickets: Boolean = false,
  val faqSearchQuery: String = "",
  val faqItems: List<FaqEntry> = TelefamSettingsContentCatalog.defaultFaqs,
  val systemStatuses: List<SystemServiceStatusItem> = emptyList(),
  val isLoadingSystemStatus: Boolean = false,
  val legalDocuments: List<LegalDocumentItem> = TelefamSettingsContentCatalog.legalDocuments,
  val selectedLegalDocId: String = "terms",
  val appVersionCheck: AppVersionCheckResult? = null,
  val isCheckingAppUpdates: Boolean = false,
  val isSubmittingProblemReport: Boolean = false,
  val lastCreatedTicket: SupportTicketItem? = null,
  val showLogoutConfirmDialog: Boolean = false,
  val logoutAllDevicesChoice: Boolean = false,
  val showBackgroundsModal: Boolean = false,
  val isUploadingBackground: Boolean = false,
  val backgroundError: String? = null,
  val showEditProfileModal: Boolean = false,
  val isSavingEditedProfile: Boolean = false,
  val editProfileError: String? = null,
  val editProfileCropBitmap: Bitmap? = null,
  val editProfileProcessedAvatar: ProcessedAvatarMedia? = null,
  val pendingLoginApprovalWait: DeviceApprovalWaitState? = null,
  val incomingLoginRequest: IncomingLoginRequestState? = null,
  val loginApprovedOverlay: LoginApprovedOverlayState? = null,
  val isRespondingToLoginRequest: Boolean = false,
  val loginRequestActionError: String? = null,
  val tryAnotherWayGoogleExpectedEmail: String? = null,
  val tryAnotherWayApprovalRequestId: String? = null,
  val chatPrivacy: ChatPrivacySettingsState = ChatPrivacySettingsState(),
  val chatPrivacyBackStack: List<ChatPrivacyRoute> = emptyList(),
  val chatState: TelefamChatUiState = TelefamChatUiState(),
  val linkPreviews: Map<String, LinkPreviewUiState> = emptyMap(),
  val savedLinks: List<SavedLinkEntity> = emptyList(),
  val savedLinksSearchQuery: String = "",
  val inAppBrowserState: InAppBrowserSessionState? = null,
  val smartActionToast: String? = null,
  val notifications: List<CachedNotificationEntity> = emptyList(),
  val unreadNotificationCount: Int = 0,
  val showNotificationsSheet: Boolean = false,
) {
  val activeUserProfile: UserProfileScreenState?
    get() = profileBackStack.lastOrNull()

  val activeSettingsDestination: SettingsDestination?
    get() = settingsBackStack.lastOrNull() ?: if (showSettingsModal) SettingsDestination.HUB else null

  val activeChatPrivacyRoute: ChatPrivacyRoute?
    get() = chatPrivacyBackStack.lastOrNull()

  val effectiveNetworkTierForFeeds: NetworkTier
    get() {
      val prefs = settings.contentPreferences
      return if (prefs.dataSaver || prefs.videoQuality == VideoQualityOption.DATA_SAVER) {
        if (networkTier == NetworkTier.OFFLINE) NetworkTier.OFFLINE else NetworkTier.WEAK
      } else {
        networkTier
      }
    }
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class TelefamViewModel(
  private val repository: TelefamRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(TelefamUiState())
  val uiState: StateFlow<TelefamUiState> = _uiState.asStateFlow()

  private var countdownJob: Job? = null
  private var deviceApprovalPollJob: Job? = null
  private var incomingLoginPollerJob: Job? = null
  private var incomingCountdownJob: Job? = null
  private var emailCheckJob: Job? = null
  private var usernameCheckJob: Job? = null
  private var contactsSearchJob: Job? = null
  private var feedSearchJob: Job? = null
  private var songSearchJob: Job? = null
  private var hashtagSearchJob: Job? = null
  private var previewPlayer: ExoPlayer? = null
  private var lastInitializedSettingsUid: String = ""
  private val profileMutationEpoch = mutableMapOf<String, Long>()
  private val inFlightLinkPreviews = mutableSetOf<String>()

  val chatController = ChatFeatureController(viewModelScope, repository, _uiState)
  val callController: TelefamCallController = repository.createCallController()
  val starsController: telefam.company.stars.TelefamStarsController = repository.createStarsController(viewModelScope)

  init {
    viewModelScope.launch {
      callController.callConversationUpdated.collect { convId ->
        repository.chatRepository.refreshConversations()
        if (_uiState.value.chatState.activeConversationId == convId) {
          repository.chatRepository.loadConversationMessages(convId, markAsRead = false)
        }
      }
    }
    viewModelScope.launch {
      // Refresh short-lived access token silently on launch if long-lived refresh token is valid
      repository.ensureSessionFresh()

      combine(repository.sessionFlow, repository.networkTier) { session, tier ->
        session to tier
      }.collect { (session, tier) ->
        val wasOffline = _uiState.value.networkTier == NetworkTier.OFFLINE
        val isAuthenticated = !session.userId.isNullOrBlank() && !session.accessToken.isNullOrBlank()
        val email = session.email.orEmpty()
        val userId = session.userId.orEmpty()

        val savedProfile = if (isAuthenticated && userId.isNotBlank()) {
          repository.observeProfile(userId).first()
        } else {
          null
        }

        val streamCreds = if (isAuthenticated) {
          repository.getStreamCredentials().also { (tok, fp) ->
            TelefamVideoCacheManager.updateCredentials(tok, fp)
          }
        } else {
          TelefamVideoCacheManager.updateCredentials("", "")
          "" to ""
        }

        _uiState.update { current ->
          val shouldAutofill = isAuthenticated && !session.profileCompleted &&
            current.profileForm.fullName.isBlank() && email.isNotBlank()
          val updatedForm = if (shouldAutofill) {
            val (autoName, autoUsername) = TelefamProfileSanitizer.autofillFromEmail(email)
            current.profileForm.copy(
              fullName = autoName,
              username = autoUsername,
              isUsernameAvailable = true,
            )
          } else {
            current.profileForm
          }

          current.copy(
            isSessionInitialized = true,
            isAuthenticated = isAuthenticated,
            profileCompleted = session.profileCompleted,
            userId = userId,
            userEmail = email,
            themeMode = session.themeMode,
            networkTier = tier,
            profileForm = updatedForm,
            savedAvatarLocalPath = savedProfile?.avatarLocalPath ?: current.savedAvatarLocalPath,
            feedsState = current.feedsState.copy(
              streamAccessToken = streamCreds.first,
              streamDeviceFp = streamCreds.second,
            ),
          )
        }

        if (!isAuthenticated && !session.pendingOtpEmail.isNullOrBlank() &&
          _uiState.value.authStep == AuthStep.EMAIL_INPUT &&
          _uiState.value.emailInput.isBlank()
        ) {
          val pendingEmail = session.pendingOtpEmail
          val (rem, total) = repository.getCurrentCooldownForEmail(pendingEmail)
          _uiState.update {
            it.copy(
              emailInput = pendingEmail,
              authStep = AuthStep.OTP_VERIFICATION,
              cooldownRemainingSeconds = rem,
              activeCooldownDurationSeconds = total,
            )
          }
          if (rem > 0L) startCountdown(rem)
        }

        if (isAuthenticated && userId.isNotBlank() && lastInitializedSettingsUid != userId) {
          lastInitializedSettingsUid = userId
          val cachedSettings = runCatching { repository.loadCachedSettings() }.getOrDefault(_uiState.value.settings)
          val cachedChatPrivacy = runCatching { repository.loadCachedChatPrivacy() }.getOrDefault(_uiState.value.chatPrivacy)
          val cachedBlocked = runCatching { repository.listRestrictedAccounts(isMuted = false) }.getOrDefault(emptyList())
          val cachedMuted = runCatching { repository.listRestrictedAccounts(isMuted = true) }.getOrDefault(emptyList())
          _uiState.update {
            it.copy(
              settings = cachedSettings,
              chatPrivacy = cachedChatPrivacy,
              blockedAccounts = cachedBlocked,
              mutedAccounts = cachedMuted,
            )
          }
          if (tier != NetworkTier.OFFLINE) {
            launch { refreshAllSettingsFromServer() }
            launch { repository.captureAndSyncDeviceContext(force = true) }
            launch { repository.refreshNotifications() }
          } else {
            launch { repository.captureAndSyncDeviceContext(force = false) }
          }
          launch { repository.syncFcmTokenIfAuthenticated() }
          startIncomingLoginRequestPoller()
        } else if (!isAuthenticated) {
          lastInitializedSettingsUid = ""
          incomingLoginPollerJob?.cancel()
          incomingLoginPollerJob = null
          incomingCountdownJob?.cancel()
        }

        if (wasOffline && tier != NetworkTier.OFFLINE && isAuthenticated) {
          val syncPrefs = _uiState.value.settings.syncPreferences
          val wifiOk = !syncPrefs.syncOverWifiOnly || repository.isWifiOrUnmetered()
          if (syncPrefs.autoSyncEnabled && wifiOk) {
            repository.flushOfflineQueue()
            repository.flushPendingSettingsOps()
            launch { repository.flushPendingDeviceContext(force = true) }
            refreshAllSettingsFromServer()
          }
          launch { repository.syncFcmTokenIfAuthenticated() }
          launch { repository.refreshNotifications() }
          val latestStatus = runCatching { repository.getOfflineSyncStatus() }.getOrDefault(_uiState.value.syncStatus)
          _uiState.update { it.copy(syncStatus = latestStatus, isWifiConnected = repository.isWifiOrUnmetered()) }
          refreshIncomingLoginRequests()
          when (_uiState.value.activeHomeTab) {
            HomeTab.CONTACTS -> refreshContacts()
            HomeTab.FEEDS -> refreshFeeds()
            HomeTab.PROFILE -> loadOwnProfile()
            else -> Unit
          }
        }
      }
    }

    // Reactive Contacts cache observer keyed on filter tab, search query, and authenticated userId
    viewModelScope.launch {
      _uiState
        .map { Triple(it.contactsState.activeFilter, it.contactsState.searchQuery, it.userId to it.isAuthenticated) }
        .distinctUntilChanged()
        .flatMapLatest { (filter, query, auth) ->
          val (uid, authed) = auth
          if (!authed || uid.isBlank()) {
            flowOf(emptyList())
          } else {
            repository.observeCachedContacts(filter, query, uid)
          }
        }
        .collect { cached ->
          val countryByIso = CountryCatalog.allCountries.associateBy { it.iso.uppercase() }
          val baseUrl = BuildConfig.TELEFAM_API_BASE_URL
          val uiItems = cached.map { entity ->
            val countryName = countryByIso[entity.countryIso.uppercase()]?.name
              ?: entity.location.ifBlank { entity.countryIso }
            val resolvedAvatar = entity.avatarLocalPath?.takeIf { File(it).exists() }
              ?: entity.avatarUrl?.trim()?.takeIf { it.isNotBlank() }?.let {
                AdaptiveStreamSelector.resolveMediaUrl(baseUrl, it)
              }
            ContactUiItem(
              userId = entity.userId,
              fullName = entity.fullName,
              username = entity.username,
              countryDisplay = countryName,
              avatarLocalPath = resolvedAvatar,
              isFollowing = entity.isFollowing,
              followsYou = entity.followsYou,
              relationshipState = ContactRelationshipState.compute(entity.isFollowing, entity.followsYou),
              initials = TelefamContactsRules.initialsFor(entity.fullName, entity.username),
            )
          }
          _uiState.update { state ->
            state.copy(contactsState = state.contactsState.copy(items = uiItems))
          }
        }
    }

    // Reactive full directory observer for Post "Tag people" sheet
    viewModelScope.launch {
      _uiState
        .map { it.userId to it.isAuthenticated }
        .distinctUntilChanged()
        .flatMapLatest { (uid, authed) ->
          if (!authed || uid.isBlank()) {
            flowOf(emptyList())
          } else {
            repository.observeCachedContacts(ContactFilterTab.ALL, "", uid)
          }
        }
        .collect { cachedAll ->
          val countryByIso = CountryCatalog.allCountries.associateBy { it.iso.uppercase() }
          val baseUrl = BuildConfig.TELEFAM_API_BASE_URL
          val items = cachedAll.map { entity ->
            val countryName = countryByIso[entity.countryIso.uppercase()]?.name
              ?: entity.location.ifBlank { entity.countryIso }
            val resolvedAvatar = entity.avatarLocalPath?.takeIf { File(it).exists() }
              ?: entity.avatarUrl?.trim()?.takeIf { it.isNotBlank() }?.let {
                AdaptiveStreamSelector.resolveMediaUrl(baseUrl, it)
              }
            ContactUiItem(
              userId = entity.userId,
              fullName = entity.fullName,
              username = entity.username,
              countryDisplay = countryName,
              avatarLocalPath = resolvedAvatar,
              isFollowing = entity.isFollowing,
              followsYou = entity.followsYou,
              relationshipState = ContactRelationshipState.compute(entity.isFollowing, entity.followsYou),
              initials = TelefamContactsRules.initialsFor(entity.fullName, entity.username),
            )
          }
          _uiState.update { state ->
            val selectedIds = state.postComposer.taggedPeople.map { it.userId }.toSet()
            state.copy(
              postComposer = state.postComposer.copy(
                allDirectoryPeople = items,
                taggedPeople = items.filter { it.userId in selectedIds },
              ),
            )
          }
        }
    }

    // Observe background queued post uploads count
    viewModelScope.launch {
      repository.pendingPostUploadsFlow.collect { pending ->
        _uiState.update { state ->
          state.copy(postComposer = state.postComposer.copy(pendingQueueCount = pending.size))
        }
      }
    }

    // Reactive Saved Links observer scoped to authenticated userId & search query
    viewModelScope.launch {
      _uiState
        .map { Triple(it.userId, it.isAuthenticated, it.savedLinksSearchQuery) }
        .distinctUntilChanged()
        .flatMapLatest { (uid, authed, query) ->
          if (!authed || uid.isBlank()) {
            flowOf(emptyList())
          } else {
            repository.observeSavedLinks(uid, query)
          }
        }
        .collect { items ->
          _uiState.update { it.copy(savedLinks = items) }
        }
    }

    // Reactive Notifications Inbox & Unread Count observer
    viewModelScope.launch {
      _uiState
        .map { it.userId to it.isAuthenticated }
        .distinctUntilChanged()
        .flatMapLatest { (uid, authed) ->
          if (!authed || uid.isBlank()) {
            flowOf(emptyList())
          } else {
            repository.observeNotifications(uid)
          }
        }
        .collect { list ->
          _uiState.update { it.copy(notifications = list) }
        }
    }

    viewModelScope.launch {
      _uiState
        .map { it.userId to it.isAuthenticated }
        .distinctUntilChanged()
        .flatMapLatest { (uid, authed) ->
          if (!authed || uid.isBlank()) {
            flowOf(0)
          } else {
            repository.observeUnreadNotificationCount(uid)
          }
        }
        .collect { unread ->
          _uiState.update { it.copy(unreadNotificationCount = unread) }
        }
    }

    // Observe cached For You + Following feeds adaptively with live content-preference & mute/block filtering
    viewModelScope.launch {
      combine(
        _uiState.map {
          FeedFilterSignal(
            userId = it.userId,
            isAuthenticated = it.isAuthenticated,
            tier = it.effectiveNetworkTierForFeeds,
            notInterestedTopics = it.settings.contentPreferences.notInterestedTopics
              .map { t -> t.trim().lowercase().removePrefix("#") }
              .filter { t -> t.isNotBlank() }
              .toSet(),
            restrictedUserIds = (it.mutedAccounts.map { m -> m.userId } + it.blockedAccounts.map { b -> b.userId }).toSet(),
          )
        }.distinctUntilChanged(),
        repository.observeFeedPosts(FeedTab.FOR_YOU),
        repository.observeFeedPosts(FeedTab.FOLLOWING),
      ) { signal, forYouEntities, followingEntities ->
        if (!signal.isAuthenticated) {
          Triple(emptyList<FeedUiItem>(), emptyList<FeedUiItem>(), signal.tier)
        } else {
          fun matchesFilters(item: FeedUiItem): Boolean {
            if (!item.isOwner && item.authorUserId in signal.restrictedUserIds) return false
            if (signal.notInterestedTopics.isNotEmpty()) {
              val itemTags = item.hashtags.map { it.trim().lowercase().removePrefix("#") }
              val captionLower = item.caption.lowercase()
              if (itemTags.any { it in signal.notInterestedTopics } ||
                signal.notInterestedTopics.any { t -> captionLower.contains("#$t") || captionLower.contains(" $t ") }
              ) {
                return false
              }
            }
            return true
          }
          val forYouUi = forYouEntities.map { mapFeedEntityToUi(it, signal.tier) }.filter(::matchesFilters)
          val followingUi = followingEntities.map { mapFeedEntityToUi(it, signal.tier) }.filter(::matchesFilters)
          Triple(forYouUi, followingUi, signal.tier)
        }
      }.collect { (forYouUi, followingUi, _) ->
        _uiState.update { state ->
          val currentSearchViewer = state.feedsState.searchViewerItems?.mapNotNull { old ->
            val updated = (forYouUi + followingUi).firstOrNull { it.postId == old.postId }
            updated ?: old.takeIf { _ -> forYouUi.isEmpty() && followingUi.isEmpty() } ?: updated
          }
          state.copy(
            feedsState = state.feedsState.copy(
              forYouItems = forYouUi,
              followingItems = followingUi,
              searchViewerItems = currentSearchViewer,
            ),
          )
        }
      }
    }
  }

  fun cycleThemeMode() {
    val next = when (_uiState.value.themeMode) {
      ThemeMode.SYSTEM -> ThemeMode.LIGHT
      ThemeMode.LIGHT -> ThemeMode.DARK
      ThemeMode.DARK -> ThemeMode.SYSTEM
    }
    viewModelScope.launch {
      repository.setThemeMode(next)
      val uid = _uiState.value.userId
      if (uid.isNotBlank()) {
        repository.logAudit(uid, "theme_changed", "{\"mode\":\"${next.name}\"}")
      }
    }
  }

  fun onEmailChanged(value: String) {
    _uiState.update { it.copy(emailInput = value, errorMessage = null, emailAccountNotice = null) }
    emailCheckJob?.cancel()
    val trimmed = value.trim().lowercase()
    if (!isValidEmail(trimmed)) return
    emailCheckJob = viewModelScope.launch {
      delay(380L)
      val status = repository.checkEmailStatus(trimmed)
      if (status != null && status.exists && _uiState.value.emailInput.trim().equals(trimmed, ignoreCase = true)) {
        _uiState.update {
          it.copy(
            emailAccountNotice = if (status.profileCompleted) {
              "Existing Telefam account — sign in to restore your profile"
            } else {
              "Account found — continue to complete your sign in"
            },
          )
        }
      }
    }
  }

  fun onOtpChanged(value: String) {
    val digitsOnly = value.filter { it.isDigit() }.take(6)
    _uiState.update { it.copy(otpCode = digitsOnly, errorMessage = null) }
  }

  fun sendOtp() {
    val email = _uiState.value.emailInput.trim()
    if (!isValidEmail(email)) {
      _uiState.update { it.copy(errorMessage = "Please enter a valid email address.") }
      return
    }
    if (_uiState.value.isLoading) return

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      when (val result = repository.requestOtp(email)) {
        is AuthActionResult.OtpSent -> {
          _uiState.update {
            it.copy(
              isLoading = false,
              authStep = AuthStep.OTP_VERIFICATION,
              emailInput = result.email,
              otpCode = "",
              errorMessage = null,
              cooldownRemainingSeconds = result.cooldownSeconds,
              activeCooldownDurationSeconds = result.totalCooldownDurationSeconds,
              pendingLoginApprovalWait = null,
            )
          }
          startCountdown(result.cooldownSeconds)
        }

        is AuthActionResult.RequiresDeviceApproval -> {
          val waitState = DeviceApprovalWaitState(
            email = result.email,
            requestId = result.requestId,
            pollToken = result.pollToken,
            targetDeviceName = result.targetDeviceName,
            targetLocation = result.targetLocation,
            requestTimeMs = result.requestTimeMs,
            remainingSeconds = result.expiresInSeconds,
            expiresInSeconds = result.expiresInSeconds,
            tryAnotherWayProvider = result.tryAnotherWayProvider,
          )
          _uiState.update {
            it.copy(
              isLoading = false,
              authStep = AuthStep.DEVICE_APPROVAL_WAIT,
              emailInput = result.email,
              otpCode = "",
              errorMessage = null,
              pendingLoginApprovalWait = waitState,
              tryAnotherWayGoogleExpectedEmail = result.email,
              tryAnotherWayApprovalRequestId = result.requestId,
            )
          }
          startDeviceApprovalPolling(waitState)
        }

        is AuthActionResult.SanitizedFailure -> {
          val rem = result.remainingCooldownSeconds
          val (_, total) = repository.getCurrentCooldownForEmail(email)
          _uiState.update {
            val nextStep = when {
              result.isDeviceBlocked -> AuthStep.DEVICE_APPROVAL_DENIED
              rem > 0L -> AuthStep.OTP_VERIFICATION
              else -> it.authStep
            }
            val blockedWait = if (result.isDeviceBlocked && it.pendingLoginApprovalWait == null) {
              DeviceApprovalWaitState(
                email = email,
                requestId = it.tryAnotherWayApprovalRequestId.orEmpty(),
                pollToken = "",
                targetDeviceName = "Your trusted device",
                targetLocation = "",
                requestTimeMs = System.currentTimeMillis(),
                remainingSeconds = 0L,
              )
            } else {
              it.pendingLoginApprovalWait
            }
            it.copy(
              isLoading = false,
              errorMessage = if (rem > 0L) null else result.message,
              authStep = nextStep,
              cooldownRemainingSeconds = if (rem > 0L) rem else it.cooldownRemainingSeconds,
              activeCooldownDurationSeconds = total,
              pendingLoginApprovalWait = blockedWait,
              tryAnotherWayGoogleExpectedEmail = if (result.isDeviceBlocked) email else it.tryAnotherWayGoogleExpectedEmail,
            )
          }
          if (rem > 0L) startCountdown(rem)
        }

        is AuthActionResult.Verified -> _uiState.update { it.copy(isLoading = false) }
      }
    }
  }

  private fun startDeviceApprovalPolling(waitState: DeviceApprovalWaitState) {
    deviceApprovalPollJob?.cancel()
    deviceApprovalPollJob = viewModelScope.launch {
      var tick = 0
      while (true) {
        delay(1_000L)
        tick++
        val cur = _uiState.value
        if (cur.isAuthenticated || cur.authStep != AuthStep.DEVICE_APPROVAL_WAIT) break
        val currentWait = cur.pendingLoginApprovalWait ?: break

        val decremented = (currentWait.remainingSeconds - 1L).coerceAtLeast(0L)
        _uiState.update {
          it.copy(pendingLoginApprovalWait = it.pendingLoginApprovalWait?.copy(remainingSeconds = decremented))
        }

        if (tick % 2 == 0 || decremented == 0L) {
          when (
            val poll = repository.pollLoginRequestStatus(
              requestId = currentWait.requestId,
              pollToken = currentWait.pollToken,
              email = currentWait.email,
            )
          ) {
            is LoginApprovalPollOutcome.Pending -> {
              _uiState.update {
                it.copy(
                  pendingLoginApprovalWait = it.pendingLoginApprovalWait?.copy(
                    remainingSeconds = poll.remainingSeconds,
                    targetDeviceName = poll.targetDeviceName.ifBlank { currentWait.targetDeviceName },
                    targetLocation = poll.targetLocation.ifBlank { currentWait.targetLocation },
                  ),
                )
              }
            }
            is LoginApprovalPollOutcome.Approved -> {
              onAuthVerified(poll.verified)
              break
            }
            LoginApprovalPollOutcome.DeniedBlocked -> {
              _uiState.update {
                it.copy(
                  authStep = AuthStep.DEVICE_APPROVAL_DENIED,
                  errorMessage = "Your trusted device denied this login request.",
                )
              }
              break
            }
            LoginApprovalPollOutcome.Expired -> {
              _uiState.update {
                it.copy(
                  authStep = AuthStep.EMAIL_INPUT,
                  pendingLoginApprovalWait = null,
                  tryAnotherWayGoogleExpectedEmail = null,
                  tryAnotherWayApprovalRequestId = null,
                  errorMessage = "The approval request timed out. Please try again.",
                )
              }
              break
            }
            is LoginApprovalPollOutcome.Error -> {
              if (decremented <= 0L) {
                _uiState.update { it.copy(errorMessage = poll.message) }
              }
            }
          }
        }
      }
    }
  }

  fun resendOtp() {
    if (_uiState.value.cooldownRemainingSeconds > 0L || _uiState.value.isLoading) return
    sendOtp()
  }

  fun verifyOtp() {
    val state = _uiState.value
    if (state.otpCode.length != 6) {
      _uiState.update { it.copy(errorMessage = "Please enter the 6-digit verification code.") }
      return
    }
    if (state.isLoading) return

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      when (val result = repository.verifyOtp(state.emailInput, state.otpCode)) {
        is AuthActionResult.Verified -> onAuthVerified(result)
        is AuthActionResult.SanitizedFailure -> {
          _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
        }
        is AuthActionResult.OtpSent, is AuthActionResult.RequiresDeviceApproval -> Unit
      }
    }
  }

  fun handleVerificationDeepLink(token: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      when (val result = repository.verifyMagicLink(token, _uiState.value.emailInput)) {
        is AuthActionResult.Verified -> onAuthVerified(result)
        is AuthActionResult.SanitizedFailure -> {
          _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
        }
        is AuthActionResult.OtpSent, is AuthActionResult.RequiresDeviceApproval -> Unit
      }
    }
  }

  fun prepareTryAnotherWayGoogleSignIn() {
    val cur = _uiState.value
    val expected = cur.pendingLoginApprovalWait?.email?.ifBlank { cur.emailInput } ?: cur.emailInput
    val reqId = cur.pendingLoginApprovalWait?.requestId
    _uiState.update {
      it.copy(
        tryAnotherWayGoogleExpectedEmail = expected.trim().lowercase().takeIf { e -> e.isNotBlank() },
        tryAnotherWayApprovalRequestId = reqId?.takeIf { id -> id.isNotBlank() },
        errorMessage = null,
      )
    }
  }

  fun onGoogleSignInSuccess(idToken: String, email: String) {
    viewModelScope.launch {
      val cur = _uiState.value
      val expectedEmail = if (cur.authStep == AuthStep.DEVICE_APPROVAL_WAIT || cur.authStep == AuthStep.DEVICE_APPROVAL_DENIED) {
        cur.tryAnotherWayGoogleExpectedEmail ?: cur.pendingLoginApprovalWait?.email ?: cur.emailInput
      } else {
        null
      }
      val approvalReqId = if (cur.authStep == AuthStep.DEVICE_APPROVAL_WAIT || cur.authStep == AuthStep.DEVICE_APPROVAL_DENIED) {
        cur.tryAnotherWayApprovalRequestId ?: cur.pendingLoginApprovalWait?.requestId
      } else {
        null
      }
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      when (
        val result = repository.authenticateWithGoogle(
          idToken = idToken,
          email = email,
          expectedEmail = expectedEmail,
          approvalRequestId = approvalReqId,
        )
      ) {
        is AuthActionResult.Verified -> onAuthVerified(result)
        is AuthActionResult.SanitizedFailure -> {
          _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
        }
        is AuthActionResult.OtpSent, is AuthActionResult.RequiresDeviceApproval -> Unit
      }
    }
  }

  fun onGoogleSignInError() {
    _uiState.update {
      it.copy(isLoading = false, errorMessage = TelefamCoreEngine.sanitizeServerError(401))
    }
  }

  fun changeEmail() {
    countdownJob?.cancel()
    deviceApprovalPollJob?.cancel()
    viewModelScope.launch { repository.savePendingOtpEmail(null) }
    _uiState.update {
      it.copy(
        authStep = AuthStep.EMAIL_INPUT,
        otpCode = "",
        errorMessage = null,
        cooldownRemainingSeconds = 0L,
        pendingLoginApprovalWait = null,
        tryAnotherWayGoogleExpectedEmail = null,
        tryAnotherWayApprovalRequestId = null,
      )
    }
  }

  private fun startIncomingLoginRequestPoller() {
    if (incomingLoginPollerJob?.isActive == true) return
    incomingLoginPollerJob = viewModelScope.launch {
      while (true) {
        if (_uiState.value.isAuthenticated && _uiState.value.profileCompleted) {
          refreshIncomingLoginRequests()
        }
        delay(4_000L)
      }
    }
  }

  fun refreshIncomingLoginRequests() {
    viewModelScope.launch {
      val list = repository.fetchPendingIncomingLoginRequests()
      val top = list.firstOrNull()
      if (top == null) {
        if (_uiState.value.incomingLoginRequest != null && !_uiState.value.isRespondingToLoginRequest) {
          incomingCountdownJob?.cancel()
          _uiState.update { it.copy(incomingLoginRequest = null) }
        }
        return@launch
      }
      val next = IncomingLoginRequestState(
        requestId = top.requestId,
        deviceName = top.deviceName.ifBlank { "New Device" },
        location = top.location.ifBlank { "Unknown location" },
        ipAddressMasked = top.ipAddressMasked.ifBlank { "XX.XX.XXX.XX" },
        requestedAtMs = top.requestedAtMs.takeIf { it > 0L } ?: System.currentTimeMillis(),
        remainingSeconds = top.remainingSeconds.coerceAtLeast(1L),
      )
      val prevId = _uiState.value.incomingLoginRequest?.requestId
      _uiState.update {
        it.copy(
          incomingLoginRequest = next,
          loginRequestActionError = if (prevId != next.requestId) null else it.loginRequestActionError,
        )
      }
      if (prevId != next.requestId || incomingCountdownJob?.isActive != true) {
        startIncomingRequestCountdown(next.requestId, next.remainingSeconds)
      }
    }
  }

  private fun startIncomingRequestCountdown(requestId: String, initialSeconds: Long) {
    incomingCountdownJob?.cancel()
    incomingCountdownJob = viewModelScope.launch {
      var remaining = initialSeconds
      while (remaining > 0L) {
        delay(1_000L)
        remaining--
        val cur = _uiState.value.incomingLoginRequest
        if (cur == null || cur.requestId != requestId) break
        _uiState.update {
          it.copy(incomingLoginRequest = cur.copy(remainingSeconds = remaining))
        }
      }
      val latest = _uiState.value.incomingLoginRequest
      if (latest != null && latest.requestId == requestId && remaining <= 0L) {
        _uiState.update { it.copy(incomingLoginRequest = null) }
      }
    }
  }

  fun respondToIncomingLoginRequest(accept: Boolean) {
    val req = _uiState.value.incomingLoginRequest ?: return
    if (_uiState.value.isRespondingToLoginRequest) return
    viewModelScope.launch {
      _uiState.update { it.copy(isRespondingToLoginRequest = true, loginRequestActionError = null) }
      val res = repository.respondToIncomingLoginRequest(req.requestId, accept)
      if (!res.success) {
        _uiState.update {
          it.copy(
            isRespondingToLoginRequest = false,
            loginRequestActionError = res.message ?: "Unable to process request.",
          )
        }
        return@launch
      }
      incomingCountdownJob?.cancel()
      if (accept) {
        _uiState.update {
          it.copy(
            isRespondingToLoginRequest = false,
            incomingLoginRequest = null,
            loginRequestActionError = null,
            loginApprovedOverlay = LoginApprovedOverlayState(
              deviceName = res.deviceName.ifBlank { req.deviceName },
              location = res.location.ifBlank { req.location },
              requestedAtMs = res.requestedAtMs.takeIf { ts -> ts > 0L } ?: req.requestedAtMs,
            ),
          )
        }
      } else {
        _uiState.update {
          it.copy(
            isRespondingToLoginRequest = false,
            incomingLoginRequest = null,
            loginRequestActionError = null,
            profileToastMessage = "Login request denied and device blocked.",
          )
        }
      }
    }
  }

  fun dismissLoginApprovedOverlay() {
    _uiState.update { it.copy(loginApprovedOverlay = null) }
    refreshIncomingLoginRequests()
  }

  // Profile Setup handlers
  fun onProfileFullNameChanged(value: String) {
    _uiState.update {
      it.copy(profileForm = it.profileForm.copy(fullName = value.take(64), errorMessage = null))
    }
  }

  fun onProfileUsernameChanged(value: String) {
    val clean = TelefamProfileSanitizer.sanitizeUsernameInput(value)
    _uiState.update {
      it.copy(
        profileForm = it.profileForm.copy(
          username = clean,
          isUsernameAvailable = null,
          errorMessage = null,
        ),
      )
    }
    usernameCheckJob?.cancel()
    if (TelefamProfileSanitizer.isValidUsername(clean)) {
      usernameCheckJob = viewModelScope.launch {
        delay(250L)
        val avail = repository.isUsernameAvailable(clean, _uiState.value.userId)
        _uiState.update {
          it.copy(profileForm = it.profileForm.copy(isUsernameAvailable = avail))
        }
      }
    }
  }

  fun onProfileCountrySelected(country: CountryDialOption) {
    _uiState.update {
      it.copy(profileForm = it.profileForm.copy(selectedCountry = country, errorMessage = null))
    }
  }

  fun onProfilePhoneDigitsChanged(value: String) {
    val digits = value.filter { it.isDigit() }.take(15)
    _uiState.update {
      it.copy(profileForm = it.profileForm.copy(phoneDigits = digits, errorMessage = null))
    }
  }

  fun onProfileGenderSelected(gender: String) {
    _uiState.update {
      it.copy(profileForm = it.profileForm.copy(gender = gender, errorMessage = null))
    }
  }

  fun onProfileDobSelected(year: Int, monthZeroBased: Int, day: Int) {
    val formatted = String.format("%04d-%02d-%02d", year, monthZeroBased + 1, day)
    val ageError = if (!TelefamProfileSanitizer.isAtLeast13YearsOld(year, monthZeroBased, day)) {
      "You must be at least 13 years old to use Telefam."
    } else {
      null
    }
    _uiState.update {
      it.copy(
        profileForm = it.profileForm.copy(
          dobYear = year,
          dobMonthZeroBased = monthZeroBased,
          dobDay = day,
          dobDisplay = formatted,
          errorMessage = ageError,
        ),
      )
    }
  }

  fun onProfileLocationChanged(value: String) {
    _uiState.update {
      it.copy(profileForm = it.profileForm.copy(location = value.take(80), errorMessage = null))
    }
  }

  fun onProfileBioChanged(value: String) {
    _uiState.update {
      it.copy(profileForm = it.profileForm.copy(bio = value.take(150), errorMessage = null))
    }
  }

  fun onPhotoPickedForCrop(bitmap: Bitmap) {
    _uiState.update {
      it.copy(profileForm = it.profileForm.copy(rawBitmapForCrop = bitmap, errorMessage = null))
    }
  }

  fun dismissPhotoCropper() {
    _uiState.update {
      it.copy(profileForm = it.profileForm.copy(rawBitmapForCrop = null))
    }
  }

  fun confirmCroppedPhoto(bitmap: Bitmap, centerX: Float, centerY: Float, zoomScale: Float) {
    val userId = _uiState.value.userId.ifBlank { "local_user" }
    _uiState.update {
      it.copy(profileForm = it.profileForm.copy(rawBitmapForCrop = null, isSaving = true))
    }
    viewModelScope.launch {
      val processed = repository.processAndUploadAvatar(userId, bitmap, centerX, centerY, zoomScale)
      _uiState.update {
        it.copy(
          profileForm = it.profileForm.copy(
            processedAvatar = processed,
            isSaving = false,
            errorMessage = null,
          ),
          savedAvatarLocalPath = processed.localFilePath,
        )
      }
    }
  }

  fun submitProfileSetup() {
    val state = _uiState.value
    val form = state.profileForm
    if (form.isSaving) return

    val dobYear = form.dobYear
    val dobMonth = form.dobMonthZeroBased
    val dobDay = form.dobDay
    if (dobYear == null || dobMonth == null || dobDay == null) {
      _uiState.update {
        it.copy(profileForm = form.copy(errorMessage = "Please complete all required profile fields."))
      }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(profileForm = form.copy(isSaving = true, errorMessage = null)) }
      val res = repository.saveProfileSetup(
        userId = state.userId,
        email = state.userEmail,
        fullNameRaw = form.fullName,
        usernameRaw = form.username,
        dialCode = form.selectedCountry.dialCode,
        phoneDigitsRaw = form.phoneDigits,
        countryIso = form.selectedCountry.iso,
        genderRaw = form.gender,
        dobYear = dobYear,
        dobMonthZeroBased = dobMonth,
        dobDay = dobDay,
        locationRaw = form.location,
        bioRaw = form.bio,
        avatar = form.processedAvatar,
      )
      when (res) {
        is ProfileActionResult.Saved -> {
          _uiState.update {
            it.copy(
              profileCompleted = true,
              profileForm = it.profileForm.copy(isSaving = false, errorMessage = null),
            )
          }
        }
        is ProfileActionResult.Error -> {
          _uiState.update {
            it.copy(profileForm = it.profileForm.copy(isSaving = false, errorMessage = res.message))
          }
        }
      }
    }
  }

  // Home screen & Contacts / Feeds / Profile interactions + audit logging
  fun selectHomeTab(tab: HomeTab) {
    val cur = _uiState.value
    if (!cur.isAuthenticated ||
      !cur.profileCompleted ||
      cur.chatState.activeConversation != null ||
      cur.chatState.isHomeSearchOpen ||
      cur.feedsState.isSearchOpen ||
      cur.activeUserProfile != null ||
      cur.profileViewerItems != null ||
      cur.activeSettingsDestination != null ||
      cur.showProfileMenuModal ||
      cur.activeChatPrivacyRoute != null ||
      cur.showNotificationsSheet ||
      cur.postComposer.isOpen
    ) {
      return
    }
    if (tab == HomeTab.POST) {
      openPostComposer()
      return
    }
    _uiState.update {
      it.copy(
        activeHomeTab = tab,
        isBottomBarVisible = true,
        comingSoonMessage = null,
      )
    }
    when (tab) {
      HomeTab.CONTACTS -> refreshContacts()
      HomeTab.FEEDS -> {
        viewModelScope.launch {
          val (tok, fp) = repository.getStreamCredentials()
          _uiState.update { s ->
            s.copy(feedsState = s.feedsState.copy(streamAccessToken = tok, streamDeviceFp = fp))
          }
        }
        refreshFeeds()
      }
      HomeTab.PROFILE -> {
        viewModelScope.launch {
          val (tok, fp) = repository.getStreamCredentials()
          _uiState.update { s ->
            s.copy(feedsState = s.feedsState.copy(streamAccessToken = tok, streamDeviceFp = fp))
          }
        }
        loadOwnProfile()
      }
      else -> Unit
    }
    logClientAction("nav_tab_${tab.name.lowercase()}")
  }

  // Feeds & Reusable Search Viewer handlers
  fun selectFeedTab(tab: FeedTab) {
    _uiState.update {
      it.copy(feedsState = it.feedsState.copy(activeTab = tab))
    }
    refreshFeeds(tab)
    logClientAction("feed_tab_${tab.apiParam}")
  }

  fun refreshFeeds(tab: FeedTab = _uiState.value.feedsState.activeTab) {
    if (!_uiState.value.isAuthenticated) return
    viewModelScope.launch {
      _uiState.update { it.copy(feedsState = it.feedsState.copy(isLoading = true)) }
      val res = repository.loadFeedPage(tab = tab, page = 1, pageSize = 15)
      _uiState.update { cur ->
        cur.copy(
          feedsState = if (tab == FeedTab.FOLLOWING) {
            cur.feedsState.copy(
              isLoading = false,
              followingPage = res.page,
              hasMoreFollowing = res.hasMore,
            )
          } else {
            cur.feedsState.copy(
              isLoading = false,
              forYouPage = res.page,
              hasMoreForYou = res.hasMore,
            )
          },
        )
      }
    }
  }

  fun loadNextFeedPage(tab: FeedTab = _uiState.value.feedsState.activeTab) {
    val fs = _uiState.value.feedsState
    if (!_uiState.value.isAuthenticated || fs.isLoading || fs.isLoadingMore) return
    val hasMore = if (tab == FeedTab.FOLLOWING) fs.hasMoreFollowing else fs.hasMoreForYou
    if (!hasMore) return
    val nextPage = (if (tab == FeedTab.FOLLOWING) fs.followingPage else fs.forYouPage) + 1
    viewModelScope.launch {
      _uiState.update { it.copy(feedsState = it.feedsState.copy(isLoadingMore = true)) }
      val res = repository.loadFeedPage(tab = tab, page = nextPage, pageSize = 15)
      _uiState.update { cur ->
        cur.copy(
          feedsState = if (tab == FeedTab.FOLLOWING) {
            cur.feedsState.copy(
              isLoadingMore = false,
              followingPage = res.page,
              hasMoreFollowing = res.hasMore,
            )
          } else {
            cur.feedsState.copy(
              isLoadingMore = false,
              forYouPage = res.page,
              hasMoreForYou = res.hasMore,
            )
          },
        )
      }
    }
  }

  fun onToggleFeedLike(postId: String, currentlyLiked: Boolean) {
    val action = if (currentlyLiked) FeedInteractionAction.UNLIKE else FeedInteractionAction.LIKE
    mutateSearchResultOptimistically(postId) { item ->
      item.copy(
        isLiked = !currentlyLiked,
        likesCount = if (currentlyLiked) (item.likesCount - 1L).coerceAtLeast(0L) else item.likesCount + 1L,
      )
    }
    viewModelScope.launch { repository.performFeedInteraction(postId, action) }
  }

  fun onDoubleTapFeedLike(postId: String) {
    val item = (_uiState.value.feedsState.activeItems + _uiState.value.feedsState.searchResults)
      .firstOrNull { it.postId == postId }
    if (item != null && !item.isLiked) {
      onToggleFeedLike(postId, currentlyLiked = false)
    }
  }

  fun onToggleFeedFavourite(postId: String, currentlyFavourited: Boolean) {
    val action = if (currentlyFavourited) FeedInteractionAction.UNFAVOURITE else FeedInteractionAction.FAVOURITE
    mutateSearchResultOptimistically(postId) { item ->
      item.copy(
        isFavourited = !currentlyFavourited,
        favouritesCount = if (currentlyFavourited) (item.favouritesCount - 1L).coerceAtLeast(0L) else item.favouritesCount + 1L,
      )
    }
    viewModelScope.launch { repository.performFeedInteraction(postId, action) }
  }

  fun onToggleFeedRepost(postId: String, currentlyReposted: Boolean) {
    val action = if (currentlyReposted) FeedInteractionAction.UNREPOST else FeedInteractionAction.REPOST
    mutateSearchResultOptimistically(postId) { item ->
      item.copy(
        isReposted = !currentlyReposted,
        repostsCount = if (currentlyReposted) (item.repostsCount - 1L).coerceAtLeast(0L) else item.repostsCount + 1L,
      )
    }
    viewModelScope.launch {
      repository.performFeedInteraction(postId, action)
      if (!currentlyReposted) {
        _uiState.update { it.copy(feedsState = it.feedsState.copy(toastMessage = "Reposted")) }
      }
    }
  }

  fun onShareFeedPost(postId: String) {
    mutateSearchResultOptimistically(postId) { item ->
      item.copy(sharesCount = item.sharesCount + 1L)
    }
    viewModelScope.launch { repository.performFeedInteraction(postId, FeedInteractionAction.SHARE) }
  }

  fun onNotInterestedFeedPost(postId: String) {
    removeFromSearchLists(postId)
    viewModelScope.launch {
      repository.performFeedInteraction(postId, FeedInteractionAction.NOT_INTERESTED)
    }
  }

  fun onReportFeedPost(postId: String, reason: String, details: String = "") {
    viewModelScope.launch {
      repository.performFeedInteraction(
        postId = postId,
        action = FeedInteractionAction.REPORT,
        reason = reason,
        details = details,
      )
      _uiState.update { it.copy(feedsState = it.feedsState.copy(toastMessage = "Report submitted")) }
    }
  }

  fun onBlockFeedAuthor(postId: String, authorUserId: String) {
    _uiState.update { s ->
      s.copy(
        feedsState = s.feedsState.copy(
          searchResults = s.feedsState.searchResults.filterNot { it.authorUserId == authorUserId },
          searchViewerItems = s.feedsState.searchViewerItems?.filterNot { it.authorUserId == authorUserId },
        ),
      )
    }
    viewModelScope.launch {
      repository.performFeedInteraction(postId, FeedInteractionAction.BLOCK)
    }
  }

  fun onToggleFeedAuthorFollow(authorUserId: String, currentlyFollowing: Boolean) {
    _uiState.update { s ->
      s.copy(
        feedsState = s.feedsState.copy(
          searchResults = s.feedsState.searchResults.map {
            if (it.authorUserId == authorUserId) it.copy(isFollowingAuthor = !currentlyFollowing) else it
          },
          searchViewerItems = s.feedsState.searchViewerItems?.map {
            if (it.authorUserId == authorUserId) it.copy(isFollowingAuthor = !currentlyFollowing) else it
          },
        ),
      )
    }
    viewModelScope.launch {
      when (val res = repository.toggleFollowAuthorFromFeed(authorUserId, currentlyFollowing)) {
        is ContactToggleResult.RateLimited -> {
          _uiState.update { it.copy(feedsState = it.feedsState.copy(toastMessage = res.message)) }
        }
        else -> Unit
      }
    }
  }

  fun onRecordFeedWatchProgress(postId: String, watchMs: Long, durationMs: Long) {
    if (watchMs <= 0L) return
    val action = if (watchMs < 1500L && durationMs > 4000L) {
      FeedInteractionAction.SKIP
    } else {
      FeedInteractionAction.WATCH
    }
    viewModelScope.launch {
      repository.performFeedInteraction(postId, action, watchMs = watchMs, durationMs = durationMs)
    }
  }

  fun onDownloadFeedVideo(context: Context, item: FeedUiItem) {
    viewModelScope.launch {
      val (token, fp) = repository.getStreamCredentials()
      val targetUrl = item.rawDownloadUrl.ifBlank { item.primaryStreamUrl }
      val result = TelefamVideoDownloader.downloadVideoToDevice(
        context = context,
        postId = item.postId,
        sourceUrlOrPath = targetUrl,
        bearerToken = token,
        deviceFingerprint = fp,
      )
      val msg = when (result) {
        is VideoDownloadResult.Saved -> "Saved to Downloads"
        is VideoDownloadResult.Failed -> result.reason
      }
      _uiState.update { it.copy(feedsState = it.feedsState.copy(toastMessage = msg)) }
      if (result is VideoDownloadResult.Saved) {
        logClientAction("feed_video_downloaded_${item.postId}")
      }
    }
  }

  fun onDeleteOwnerFeedPost(postId: String) {
    removeFromSearchLists(postId)
    _uiState.update { s ->
      s.copy(
        profileBackStack = s.profileBackStack.map { p ->
          p.copy(
            posts = p.posts.filterNot { it.postId == postId },
            savedPosts = p.savedPosts.filterNot { it.postId == postId },
            repostedPosts = p.repostedPosts.filterNot { it.postId == postId },
            lockedPosts = p.lockedPosts.filterNot { it.postId == postId },
          )
        },
        ownProfileState = s.ownProfileState?.let { p ->
          p.copy(
            posts = p.posts.filterNot { it.postId == postId },
            savedPosts = p.savedPosts.filterNot { it.postId == postId },
            repostedPosts = p.repostedPosts.filterNot { it.postId == postId },
            lockedPosts = p.lockedPosts.filterNot { it.postId == postId },
          )
        },
      )
    }
    viewModelScope.launch {
      repository.deleteOwnerPost(postId)
    }
  }

  fun onChangeFeedPostPrivacy(postId: String, privacy: PostPrivacy) {
    mutateSearchResultOptimistically(postId) { it.copy(privacy = privacy) }
    _uiState.update { s ->
      fun movePostForPrivacy(p: UserProfileScreenState): UserProfileScreenState {
        val target = (p.posts + p.lockedPosts).firstOrNull { it.postId == postId }?.copy(privacy = privacy)
          ?: return p
        return if (privacy == PostPrivacy.PRIVATE) {
          p.copy(
            posts = p.posts.filterNot { it.postId == postId },
            lockedPosts = (listOf(target) + p.lockedPosts.filterNot { it.postId == postId }),
          )
        } else {
          p.copy(
            lockedPosts = p.lockedPosts.filterNot { it.postId == postId },
            posts = (listOf(target) + p.posts.filterNot { it.postId == postId })
              .sortedByDescending { it.createdAtMs },
          )
        }
      }
      s.copy(
        ownProfileState = s.ownProfileState?.let(::movePostForPrivacy),
        profileBackStack = s.profileBackStack.map(::movePostForPrivacy),
        feedsState = s.feedsState.copy(toastMessage = "Privacy: ${privacy.label}"),
      )
    }
    viewModelScope.launch {
      val ok = repository.updatePostPrivacy(postId, privacy)
      if (!ok) {
        _uiState.update { it.copy(feedsState = it.feedsState.copy(toastMessage = "Unable to update privacy")) }
      }
      if (privacy == PostPrivacy.PRIVATE) {
        refreshFeeds()
      }
      _uiState.value.userId.takeIf { it.isNotBlank() }?.let { uid -> refreshUserProfile(uid) }
    }
  }

  fun openFeedSearch(initialQuery: String = "") {
    val cleanInit = TelefamProfileSanitizer.sanitizePlainText(initialQuery, 80, singleLine = true)
    _uiState.update {
      it.copy(
        feedsState = it.feedsState.copy(
          isSearchOpen = true,
          searchQuery = cleanInit,
          searchViewerItems = null,
        ),
      )
    }
    triggerFeedSearch(cleanInit, _uiState.value.feedsState.activeShortcut)
    logClientAction("feed_search_opened")
  }

  fun closeFeedSearch() {
    feedSearchJob?.cancel()
    _uiState.update {
      it.copy(
        feedsState = it.feedsState.copy(
          isSearchOpen = false,
          searchViewerItems = null,
        ),
      )
    }
  }

  fun onFeedSearchQueryChanged(query: String) {
    val clean = query.take(80)
    _uiState.update {
      it.copy(feedsState = it.feedsState.copy(searchQuery = clean))
    }
    feedSearchJob?.cancel()
    feedSearchJob = viewModelScope.launch {
      delay(260L)
      triggerFeedSearch(clean, _uiState.value.feedsState.activeShortcut)
    }
  }

  fun onFeedSearchShortcutSelected(shortcutKey: String) {
    val next = if (_uiState.value.feedsState.activeShortcut == shortcutKey) "" else shortcutKey
    _uiState.update {
      it.copy(feedsState = it.feedsState.copy(activeShortcut = next))
    }
    triggerFeedSearch(_uiState.value.feedsState.searchQuery, next)
  }

  fun openSearchResultViewer(index: Int) {
    val results = _uiState.value.feedsState.searchResults
    if (results.isEmpty()) return
    val safeIndex = index.coerceIn(0, results.lastIndex)
    _uiState.update {
      it.copy(
        feedsState = it.feedsState.copy(
          searchViewerItems = results,
          searchViewerInitialIndex = safeIndex,
        ),
      )
    }
  }

  fun closeSearchResultViewer() {
    _uiState.update {
      it.copy(feedsState = it.feedsState.copy(searchViewerItems = null))
    }
  }

  fun showFeedToast(message: String) {
    _uiState.update { it.copy(feedsState = it.feedsState.copy(toastMessage = message)) }
  }

  fun dismissFeedToast() {
    _uiState.update { it.copy(feedsState = it.feedsState.copy(toastMessage = null)) }
  }

  fun openPostWithHashtag(rawTag: String, preferredMode: PostMode = PostMode.VIDEO) {
    val clean = TelefamPostRules.sanitizeHashtag(rawTag)
    if (clean.isBlank()) {
      openPostComposer()
      return
    }
    _uiState.update { s ->
      s.copy(
        postComposer = PostComposerUiState(
          isOpen = true,
          editingPostId = null,
          mode = preferredMode,
          selectedHashtags = listOf(clean),
          allDirectoryPeople = s.postComposer.allDirectoryPeople,
          pendingQueueCount = s.postComposer.pendingQueueCount,
        ),
      )
    }
    logClientAction("post_autofill_hashtag_$clean")
  }

  fun openPostWithSong(song: PostSongTrack, preferredMode: PostMode = PostMode.VIDEO) {
    _uiState.update { s ->
      s.copy(
        postComposer = PostComposerUiState(
          isOpen = true,
          editingPostId = null,
          mode = preferredMode,
          selectedSong = song,
          allDirectoryPeople = s.postComposer.allDirectoryPeople,
          pendingQueueCount = s.postComposer.pendingQueueCount,
        ),
      )
    }
    logClientAction("post_autofill_song_${song.trackId}")
  }

  fun openPostForEdit(item: FeedUiItem) {
    _uiState.update { s ->
      s.copy(
        postComposer = PostComposerUiState(
          isOpen = true,
          editingPostId = item.postId,
          editingThumbnailUrl = item.thumbnailUrl,
          mode = item.mode,
          textContent = item.textContent,
          caption = item.caption,
          selectedThemeId = item.themeId.ifBlank { "crimson" },
          selectedFont = item.fontOption,
          selectedSong = item.song,
          selectedHashtags = item.hashtags,
          muteOriginalAudio = item.muteOriginalAudio,
          allDirectoryPeople = s.postComposer.allDirectoryPeople,
          pendingQueueCount = s.postComposer.pendingQueueCount,
        ),
      )
    }
    logClientAction("post_edit_opened_${item.postId}")
  }

  private fun triggerFeedSearch(query: String, shortcut: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(feedsState = it.feedsState.copy(isSearching = true)) }
      val tier = _uiState.value.networkTier
      val res = repository.searchFeeds(query = query, shortcut = shortcut, page = 1, pageSize = 24)
      val uiResults = res.items.map { mapFeedEntityToUi(it, tier) }
      _uiState.update { cur ->
        cur.copy(
          feedsState = cur.feedsState.copy(
            isSearching = false,
            searchShortcuts = if (res.shortcuts.isNotEmpty()) res.shortcuts else cur.feedsState.searchShortcuts,
            searchResults = uiResults,
          ),
        )
      }
    }
  }

  private fun mutateSearchResultOptimistically(postId: String, transform: (FeedUiItem) -> FeedUiItem) {
    _uiState.update { s ->
      s.copy(
        feedsState = s.feedsState.copy(
          searchResults = s.feedsState.searchResults.map { if (it.postId == postId) transform(it) else it },
          searchViewerItems = s.feedsState.searchViewerItems?.map { if (it.postId == postId) transform(it) else it },
        ),
        profileViewerItems = s.profileViewerItems?.map { if (it.postId == postId) transform(it) else it },
        profileBackStack = s.profileBackStack.map { p ->
          p.copy(
            posts = p.posts.map { if (it.postId == postId) transform(it) else it },
            savedPosts = p.savedPosts.map { if (it.postId == postId) transform(it) else it },
            repostedPosts = p.repostedPosts.map { if (it.postId == postId) transform(it) else it },
            lockedPosts = p.lockedPosts.map { if (it.postId == postId) transform(it) else it },
          )
        },
        ownProfileState = s.ownProfileState?.let { p ->
          p.copy(
            posts = p.posts.map { if (it.postId == postId) transform(it) else it },
            savedPosts = p.savedPosts.map { if (it.postId == postId) transform(it) else it },
            repostedPosts = p.repostedPosts.map { if (it.postId == postId) transform(it) else it },
            lockedPosts = p.lockedPosts.map { if (it.postId == postId) transform(it) else it },
          )
        },
      )
    }
  }

  private fun removeFromSearchLists(postId: String) {
    _uiState.update { s ->
      s.copy(
        feedsState = s.feedsState.copy(
          searchResults = s.feedsState.searchResults.filterNot { it.postId == postId },
          searchViewerItems = s.feedsState.searchViewerItems?.filterNot { it.postId == postId },
        ),
      )
    }
  }

  private fun mapFeedEntityToUi(entity: CachedFeedPostEntity, tier: NetworkTier): FeedUiItem {
    val baseUrl = BuildConfig.TELEFAM_API_BASE_URL
    val renditions = repository.parseRenditions(entity.renditionsJson)
      .map { it.copy(url = AdaptiveStreamSelector.resolveMediaUrl(baseUrl, it.url)) }
    val streamUrl = AdaptiveStreamSelector.selectRenditionUrl(
      baseUrl = baseUrl,
      primaryVideoUrl = entity.videoUrl,
      renditions = renditions,
      networkTier = tier,
    )
    val rawDownload = if (entity.postId.isNotBlank() && baseUrl.isNotBlank()) {
      AdaptiveStreamSelector.resolveMediaUrl(baseUrl, "/v1/feeds/${entity.postId}/download")
    } else {
      streamUrl
    }
    return FeedUiItem(
      postId = entity.postId,
      authorUserId = entity.authorUserId,
      authorUsername = entity.authorUsername,
      authorFullName = entity.authorFullName,
      authorAvatarUrl = entity.authorAvatarUrl?.trim()?.takeIf { it.isNotBlank() }?.let {
        AdaptiveStreamSelector.resolveMediaUrl(baseUrl, it)
      },
      isFollowingAuthor = entity.isFollowingAuthor,
      isOwner = entity.isOwner,
      privacy = PostPrivacy.fromApi(entity.privacy),
      mode = PostMode.entries.firstOrNull { it.apiValue == entity.mode } ?: PostMode.VIDEO,
      textContent = entity.textContent,
      caption = entity.caption,
      themeId = entity.themeId.ifBlank { "crimson" },
      fontOption = TelefamPostRules.findFont(entity.fontId),
      song = repository.parseSongTrack(entity.songJson),
      hashtags = repository.parseStringList(entity.hashtagsJson),
      photoUrls = repository.parseStringList(entity.photoUrlsJson)
        .map { AdaptiveStreamSelector.resolveMediaUrl(baseUrl, it) },
      primaryStreamUrl = streamUrl,
      rawDownloadUrl = rawDownload,
      thumbnailUrl = AdaptiveStreamSelector.resolveMediaUrl(baseUrl, entity.thumbnailUrl),
      renditions = renditions,
      width = entity.width,
      height = entity.height,
      durationMs = entity.durationMs,
      muteOriginalAudio = entity.muteOriginalAudio,
      contentId = entity.contentId,
      likesCount = entity.likesCount,
      commentsCount = entity.commentsCount,
      sharesCount = entity.sharesCount,
      favouritesCount = entity.favouritesCount,
      repostsCount = entity.repostsCount,
      viewsCount = entity.viewsCount,
      isLiked = entity.isLiked,
      isFavourited = entity.isFavourited,
      isReposted = entity.isReposted,
      repostedByUsername = entity.repostedByUsername,
      recommendationLabel = entity.recommendationLabel,
      createdAtMs = entity.createdAtMs,
    )
  }

  private fun mapBackgroundDtos(
    bgs: List<ProfileBackgroundItemDto>,
    urls: List<String>,
  ): List<ProfileBackgroundItem> {
    val baseUrl = BuildConfig.TELEFAM_API_BASE_URL
    if (bgs.isNotEmpty()) {
      return bgs.take(5).map { b ->
        val raw = b.url.trim()
        val resolved = if (raw.isBlank()) "" else AdaptiveStreamSelector.resolveMediaUrl(baseUrl, raw)
        val localPath = if (raw.startsWith("/") && !raw.startsWith("/v1/") && !raw.startsWith("/media/") && File(raw).exists()) raw else ""
        ProfileBackgroundItem(
          mediaId = b.mediaId,
          url = resolved,
          localFilePath = localPath,
          sha256Fingerprint = b.sha256Fingerprint,
          dHashFingerprint = b.dHashFingerprint,
          width = b.width,
          height = b.height,
          byteSize = b.byteSize,
          createdAtMs = b.createdAtMs,
        )
      }
    }
    return urls.filter { it.isNotBlank() }.take(5).mapIndexed { idx, raw ->
      val resolved = AdaptiveStreamSelector.resolveMediaUrl(baseUrl, raw)
      val localPath = if (raw.startsWith("/") && !raw.startsWith("/v1/") && !raw.startsWith("/media/") && File(raw).exists()) raw else ""
      ProfileBackgroundItem(
        mediaId = "bg_$idx",
        url = resolved,
        localFilePath = localPath,
      )
    }
  }

  private fun mapProfileDtoToState(
    dto: UserProfileResponseDto,
    existing: UserProfileScreenState?,
    tier: NetworkTier,
  ): UserProfileScreenState {
    val baseUrl = BuildConfig.TELEFAM_API_BASE_URL
    val posts = if (dto.posts.isNotEmpty()) {
      dto.posts.map { postDto ->
        mapFeedEntityToUi(repository.feedPostDtoToEntity(postDto), tier)
      }
    } else {
      dto.videos.map { v ->
        FeedUiItem(
          postId = v.postId,
          authorUserId = dto.userId,
          authorUsername = dto.username,
          authorFullName = dto.fullName,
          authorAvatarUrl = dto.avatarUrl.ifBlank { null }?.let { AdaptiveStreamSelector.resolveMediaUrl(baseUrl, it) },
          isFollowingAuthor = dto.isFollowing,
          isOwner = dto.isOwner,
          privacy = PostPrivacy.PUBLIC,
          mode = PostMode.VIDEO,
          textContent = "",
          caption = v.caption,
          themeId = "crimson",
          fontOption = PostFontOption.CLASSIC,
          song = null,
          hashtags = emptyList(),
          photoUrls = emptyList(),
          primaryStreamUrl = AdaptiveStreamSelector.resolveMediaUrl(baseUrl, v.videoUrl),
          rawDownloadUrl = AdaptiveStreamSelector.resolveMediaUrl(baseUrl, v.videoUrl),
          thumbnailUrl = AdaptiveStreamSelector.resolveMediaUrl(baseUrl, v.thumbnailUrl),
          renditions = emptyList(),
          width = 720,
          height = 1280,
          durationMs = v.durationMs,
          muteOriginalAudio = false,
          contentId = "",
          likesCount = 0L,
          commentsCount = 0L,
          sharesCount = 0L,
          favouritesCount = 0L,
          repostsCount = 0L,
          viewsCount = v.viewsCount,
          isLiked = false,
          isFavourited = false,
          isReposted = false,
          repostedByUsername = "",
          recommendationLabel = "",
          createdAtMs = v.createdAtMs,
        )
      }
    }
    val savedPosts = dto.savedPosts.map { mapFeedEntityToUi(repository.feedPostDtoToEntity(it, "saved"), tier) }
    val repostedPosts = dto.repostedPosts.map { mapFeedEntityToUi(repository.feedPostDtoToEntity(it, "reposts"), tier) }
    val lockedPosts = dto.lockedPosts.map { mapFeedEntityToUi(repository.feedPostDtoToEntity(it, "locked"), tier) }
    val resolvedBgs = mapBackgroundDtos(dto.backgrounds, dto.backgroundUrls)
    val avatarResolved = dto.avatarUrl.trim().takeIf { it.isNotBlank() }?.let {
      AdaptiveStreamSelector.resolveMediaUrl(baseUrl, it)
    }
    val isOwnerProfile = dto.isOwner || dto.userId == _uiState.value.userId
    val ownerPrivacy = _uiState.value.settings.privacy
    return UserProfileScreenState(
      userId = dto.userId,
      fullName = dto.fullName.ifBlank { existing?.fullName ?: "User" },
      username = dto.username.ifBlank { existing?.username ?: dto.userId },
      avatarUrl = avatarResolved ?: existing?.avatarUrl,
      bio = dto.bio,
      location = dto.location,
      countryIso = dto.countryIso.ifBlank { existing?.countryIso.orEmpty() },
      gender = dto.gender.ifBlank { existing?.gender.orEmpty() },
      dateOfBirth = dto.dateOfBirth.ifBlank { existing?.dateOfBirth.orEmpty() },
      phoneNumber = dto.phoneNumber.ifBlank { existing?.phoneNumber.orEmpty() },
      usernameChangedAtMs = maxOf(dto.usernameChangedAtMs, existing?.usernameChangedAtMs ?: 0L),
      nextUsernameChangeMs = maxOf(dto.nextUsernameChangeMs, existing?.nextUsernameChangeMs ?: 0L),
      backgrounds = if (resolvedBgs.isNotEmpty() || !dto.isBlocked) resolvedBgs else existing?.backgrounds.orEmpty(),
      followingCount = dto.followingCount,
      followersCount = dto.followersCount,
      likesCount = dto.likesCount,
      isFollowing = dto.isFollowing,
      followsYou = dto.followsYou,
      isOwner = isOwnerProfile,
      isBlocked = dto.isBlocked,
      isMuted = dto.isMuted,
      isPrivateAccount = if (isOwnerProfile) ownerPrivacy.isPrivateAccount else dto.isPrivateAccount,
      whoCanComment = if (isOwnerProfile) ownerPrivacy.whoCanComment.apiValue else dto.whoCanComment,
      whoCanMention = if (isOwnerProfile) ownerPrivacy.whoCanMention.apiValue else dto.whoCanMention,
      likesVisibility = if (isOwnerProfile) ownerPrivacy.likesVisibility.apiValue else dto.likesVisibility,
      followingVisibility = if (isOwnerProfile) ownerPrivacy.followingVisibility.apiValue else dto.followingVisibility,
      showActivityStatus = if (isOwnerProfile) ownerPrivacy.showActivityStatus else dto.showActivityStatus,
      relationshipState = ContactRelationshipState.compute(dto.isFollowing, dto.followsYou),
      activeOwnerTab = existing?.activeOwnerTab ?: OwnerProfileTab.POSTS,
      posts = posts,
      savedPosts = savedPosts,
      repostedPosts = repostedPosts,
      lockedPosts = lockedPosts,
      selectedFilter = existing?.selectedFilter ?: ProfilePostFilter.ALL,
      selectedSort = existing?.selectedSort ?: ProfilePostSort.LATEST,
      isLoading = false,
      activeConnectionsType = existing?.activeConnectionsType,
      connections = existing?.connections.orEmpty(),
      isLoadingConnections = existing?.isLoadingConnections ?: false,
      connectionsPage = existing?.connectionsPage ?: 1,
      connectionsHasMore = existing?.connectionsHasMore ?: false,
    )
  }

  // New Post composer handlers
  fun openPostComposer() {
    _uiState.update {
      it.copy(
        postComposer = it.postComposer.copy(
          isOpen = true,
          errorMessage = null,
          uploadStatus = if (it.postComposer.uploadStatus == PostUploadStatus.UPLOADED) {
            PostUploadStatus.IDLE
          } else {
            it.postComposer.uploadStatus
          },
        ),
      )
    }
    logClientAction("post_composer_opened")
  }

  fun closePostComposer() {
    stopSongPreview()
    _uiState.update {
      it.copy(
        postComposer = it.postComposer.copy(
          isOpen = false,
          showTagPeopleSheet = false,
          showSongSheet = false,
          showHashtagsSheet = false,
          errorMessage = null,
        ),
      )
    }
  }

  fun onPostModeSelected(mode: PostMode) {
    _uiState.update {
      it.copy(postComposer = it.postComposer.copy(mode = mode, errorMessage = null))
    }
  }

  fun onPostTextChanged(value: String) {
    val limited = value.take(TelefamPostRules.MAX_TEXT_POST_CHARS)
    _uiState.update {
      it.copy(postComposer = it.postComposer.copy(textContent = limited, errorMessage = null))
    }
  }

  fun onPostCaptionChanged(value: String) {
    val limited = value.take(TelefamPostRules.MAX_CAPTION_CHARS)
    _uiState.update {
      it.copy(postComposer = it.postComposer.copy(caption = limited, errorMessage = null))
    }
  }

  fun onPostThemeSelected(themeId: String) {
    _uiState.update {
      it.copy(postComposer = it.postComposer.copy(selectedThemeId = themeId))
    }
  }

  fun onPostFontSelected(font: PostFontOption) {
    _uiState.update {
      it.copy(postComposer = it.postComposer.copy(selectedFont = font))
    }
  }

  fun onPostPhotosPicked(uris: List<Uri>) {
    if (uris.isEmpty()) return
    val existingCount = _uiState.value.postComposer.photos.size
    if (existingCount >= TelefamPostRules.MAX_PHOTOS_COUNT) {
      _uiState.update {
        it.copy(postComposer = it.postComposer.copy(errorMessage = "Up to 10 photos allowed"))
      }
      return
    }
    _uiState.update {
      it.copy(postComposer = it.postComposer.copy(uploadStatus = PostUploadStatus.PREPARING, errorMessage = null))
    }
    viewModelScope.launch {
      val compressed = repository.compressPostPhotos(uris, existingCount)
      _uiState.update { state ->
        val merged = (state.postComposer.photos + compressed).take(TelefamPostRules.MAX_PHOTOS_COUNT)
        state.copy(
          postComposer = state.postComposer.copy(
            photos = merged,
            uploadStatus = PostUploadStatus.IDLE,
            errorMessage = if (compressed.isEmpty()) "Unable to process selected photos" else null,
          ),
        )
      }
    }
  }

  fun removePostPhoto(localId: String) {
    _uiState.update { state ->
      state.copy(
        postComposer = state.postComposer.copy(
          photos = state.postComposer.photos.filterNot { it.localId == localId },
        ),
      )
    }
  }

  fun onPostVideoPicked(uri: Uri?) {
    if (uri == null) return
    _uiState.update {
      it.copy(postComposer = it.postComposer.copy(uploadStatus = PostUploadStatus.PREPARING, errorMessage = null))
    }
    viewModelScope.launch {
      when (val res = repository.preparePostVideo(uri)) {
        is VideoPrepareResult.Ready -> {
          val vid = res.video
          _uiState.update { state ->
            state.copy(
              postComposer = state.postComposer.copy(
                video = vid,
                videoTrimStartMs = 0L,
                videoTrimEndMs = vid.durationMs,
                uploadStatus = PostUploadStatus.IDLE,
                errorMessage = null,
              ),
            )
          }
        }
        is VideoPrepareResult.Rejected -> {
          _uiState.update { state ->
            state.copy(
              postComposer = state.postComposer.copy(
                uploadStatus = PostUploadStatus.IDLE,
                errorMessage = res.reason,
              ),
            )
          }
        }
      }
    }
  }

  fun onVideoTrimChanged(startMs: Long, endMs: Long) {
    val video = _uiState.value.postComposer.video ?: return
    val clampedStart = startMs.coerceIn(0L, (video.durationMs - 500L).coerceAtLeast(0L))
    val clampedEnd = endMs.coerceIn(clampedStart + 500L, video.durationMs.coerceAtLeast(clampedStart + 500L))
    val updatedContentId = TelefamPostRules.computeVideoContentId(
      frameHashes = video.frames.map { it.dHashHex },
      durationMs = video.durationMs,
      trimStartMs = clampedStart,
      trimEndMs = clampedEnd,
    )
    _uiState.update { state ->
      state.copy(
        postComposer = state.postComposer.copy(
          video = video.copy(contentId = updatedContentId),
          videoTrimStartMs = clampedStart,
          videoTrimEndMs = clampedEnd,
        ),
      )
    }
  }

  fun onSelectVideoThumbnailFrame(frameIndex: Int) {
    val video = _uiState.value.postComposer.video ?: return
    val targetFrame = video.frames.firstOrNull { it.index == frameIndex } ?: return
    _uiState.update { state ->
      state.copy(
        postComposer = state.postComposer.copy(
          video = video.copy(
            selectedThumbnailIndex = targetFrame.index,
            selectedThumbnailPath = targetFrame.previewPath,
          ),
        ),
      )
    }
  }

  fun onToggleMuteOriginalAudio() {
    _uiState.update { state ->
      state.copy(
        postComposer = state.postComposer.copy(
          muteOriginalAudio = !state.postComposer.muteOriginalAudio,
        ),
      )
    }
  }

  fun clearPostVideo() {
    _uiState.update { state ->
      state.copy(
        postComposer = state.postComposer.copy(
          video = null,
          videoTrimStartMs = 0L,
          videoTrimEndMs = 0L,
        ),
      )
    }
  }

  // Tag people modal controls
  fun openTagPeopleSheet() {
    _uiState.update { state ->
      state.copy(
        postComposer = state.postComposer.copy(
          showTagPeopleSheet = true,
          tagPeopleQuery = "",
          pendingTaggedIds = state.postComposer.taggedPeople.map { it.userId }.toSet(),
        ),
      )
    }
    if (_uiState.value.postComposer.allDirectoryPeople.isEmpty()) {
      viewModelScope.launch {
        repository.loadContactsPage(ContactFilterTab.ALL, "", page = 1, pageSize = 50)
      }
    }
  }

  fun onTagPeopleSearchChanged(query: String) {
    _uiState.update { state ->
      state.copy(postComposer = state.postComposer.copy(tagPeopleQuery = query.take(64)))
    }
  }

  fun togglePendingTaggedPerson(userId: String) {
    _uiState.update { state ->
      val current = state.postComposer.pendingTaggedIds
      val next = if (userId in current) {
        current - userId
      } else if (current.size < TelefamPostRules.MAX_TAGGED_PEOPLE) {
        current + userId
      } else {
        current
      }
      state.copy(postComposer = state.postComposer.copy(pendingTaggedIds = next))
    }
  }

  fun confirmTaggedPeople() {
    _uiState.update { state ->
      val chosen = state.postComposer.pendingTaggedIds
      val selected = state.postComposer.allDirectoryPeople.filter { it.userId in chosen }
      state.copy(
        postComposer = state.postComposer.copy(
          taggedPeople = selected,
          showTagPeopleSheet = false,
        ),
      )
    }
  }

  fun dismissTagPeopleSheet() {
    _uiState.update { state ->
      state.copy(postComposer = state.postComposer.copy(showTagPeopleSheet = false))
    }
  }

  // Song search + preview modal controls
  fun openSongSheet() {
    _uiState.update { state ->
      state.copy(postComposer = state.postComposer.copy(showSongSheet = true))
    }
    if (_uiState.value.postComposer.songResults.isEmpty()) {
      triggerSongSearch(_uiState.value.postComposer.songQuery)
    }
  }

  fun onSongSearchChanged(query: String) {
    val clean = query.take(80)
    _uiState.update { state ->
      state.copy(postComposer = state.postComposer.copy(songQuery = clean))
    }
    songSearchJob?.cancel()
    songSearchJob = viewModelScope.launch {
      delay(320L)
      triggerSongSearch(clean)
    }
  }

  private fun triggerSongSearch(query: String) {
    _uiState.update { state ->
      state.copy(postComposer = state.postComposer.copy(isSearchingSongs = true))
    }
    viewModelScope.launch {
      val tracks = repository.searchSongs(query)
      _uiState.update { state ->
        state.copy(
          postComposer = state.postComposer.copy(
            songResults = tracks,
            isSearchingSongs = false,
          ),
        )
      }
    }
  }

  fun selectSongTrack(track: PostSongTrack) {
    stopSongPreview()
    _uiState.update { state ->
      state.copy(
        postComposer = state.postComposer.copy(
          selectedSong = track,
          showSongSheet = false,
        ),
      )
    }
  }

  fun clearSelectedSong() {
    stopSongPreview()
    _uiState.update { state ->
      state.copy(postComposer = state.postComposer.copy(selectedSong = null))
    }
  }

  fun dismissSongSheet() {
    stopSongPreview()
    _uiState.update { state ->
      state.copy(postComposer = state.postComposer.copy(showSongSheet = false))
    }
  }

  @OptIn(UnstableApi::class)
  fun toggleSongPreview(track: PostSongTrack) {
    if (track.previewUrl.isBlank()) return
    val currentPlaying = _uiState.value.postComposer.playingPreviewTrackId
    if (currentPlaying == track.trackId) {
      stopSongPreview()
      return
    }
    stopSongPreview()
    _uiState.update { state ->
      state.copy(postComposer = state.postComposer.copy(playingPreviewTrackId = track.trackId))
    }
    runCatching {
      val player = TelefamVideoCacheManager.buildFeedAudioExoPlayer(
        context = repository.applicationContext(),
        repeatOne = false,
      )
      player.addListener(
        object : Player.Listener {
          override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
              _uiState.update { s -> s.copy(postComposer = s.postComposer.copy(playingPreviewTrackId = null)) }
            }
          }

          override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            _uiState.update { s -> s.copy(postComposer = s.postComposer.copy(playingPreviewTrackId = null)) }
          }
        },
      )
      player.setMediaItem(TelefamVideoCacheManager.createMediaItem(track.previewUrl))
      player.prepare()
      player.playWhenReady = true
      previewPlayer = player
    }.onFailure {
      _uiState.update { s -> s.copy(postComposer = s.postComposer.copy(playingPreviewTrackId = null)) }
    }
  }

  fun stopSongPreview() {
    runCatching {
      previewPlayer?.stop()
      previewPlayer?.release()
    }
    previewPlayer = null
    _uiState.update { state ->
      state.copy(postComposer = state.postComposer.copy(playingPreviewTrackId = null))
    }
  }

  // Hashtags search & post count modal controls
  fun openHashtagsSheet() {
    _uiState.update { state ->
      state.copy(postComposer = state.postComposer.copy(showHashtagsSheet = true))
    }
    triggerHashtagSearch(_uiState.value.postComposer.hashtagQuery)
  }

  fun onHashtagSearchChanged(query: String) {
    val clean = query.take(50)
    _uiState.update { state ->
      state.copy(postComposer = state.postComposer.copy(hashtagQuery = clean))
    }
    hashtagSearchJob?.cancel()
    hashtagSearchJob = viewModelScope.launch {
      delay(220L)
      triggerHashtagSearch(clean)
    }
  }

  private fun triggerHashtagSearch(query: String) {
    _uiState.update { state ->
      state.copy(postComposer = state.postComposer.copy(isSearchingHashtags = true))
    }
    viewModelScope.launch {
      val items = repository.searchHashtags(query)
      _uiState.update { state ->
        state.copy(
          postComposer = state.postComposer.copy(
            hashtagResults = items,
            isSearchingHashtags = false,
          ),
        )
      }
    }
  }

  fun toggleHashtagSelection(rawTag: String) {
    val clean = TelefamPostRules.sanitizeHashtag(rawTag)
    if (clean.isBlank()) return
    _uiState.update { state ->
      val cur = state.postComposer.selectedHashtags
      val next = if (clean in cur) {
        cur - clean
      } else if (cur.size < TelefamPostRules.MAX_HASHTAGS) {
        cur + clean
      } else {
        cur
      }
      state.copy(postComposer = state.postComposer.copy(selectedHashtags = next))
    }
  }

  fun dismissHashtagsSheet() {
    _uiState.update { state ->
      state.copy(postComposer = state.postComposer.copy(showHashtagsSheet = false))
    }
  }

  fun sharePost() {
    val post = _uiState.value.postComposer
    if (!post.canShare) return
    stopSongPreview()

    _uiState.update {
      it.copy(
        postComposer = it.postComposer.copy(
          uploadStatus = PostUploadStatus.UPLOADING,
          uploadProgressPct = if (post.editingPostId != null) 70 else 1,
          errorMessage = null,
        ),
      )
    }

    val editingId = post.editingPostId
    if (editingId != null) {
      viewModelScope.launch {
        val result = repository.updateOwnerPost(
          postId = editingId,
          textContent = post.textContent,
          caption = post.caption,
          themeId = post.selectedThemeId,
          fontId = post.selectedFont.id,
          song = post.selectedSong,
          taggedUserIds = post.taggedPeople.map { it.userId },
          hashtags = post.selectedHashtags,
          muteOriginalAudio = post.muteOriginalAudio,
        )
        when (result) {
          is PostPublishResult.Uploaded -> {
            _uiState.update { s ->
              s.copy(
                postComposer = PostComposerUiState(
                  isOpen = false,
                  allDirectoryPeople = s.postComposer.allDirectoryPeople,
                  pendingQueueCount = s.postComposer.pendingQueueCount,
                ),
                feedsState = s.feedsState.copy(toastMessage = "Updated"),
              )
            }
            refreshFeeds()
          }
          is PostPublishResult.Rejected -> {
            _uiState.update { s ->
              s.copy(
                postComposer = s.postComposer.copy(
                  uploadStatus = PostUploadStatus.FAILED,
                  errorMessage = result.message,
                ),
              )
            }
          }
          is PostPublishResult.QueuedOffline -> Unit
        }
      }
      return
    }

    val vid = post.video
    val input = PostSubmissionInput(
      mode = post.mode,
      textContent = post.textContent,
      caption = post.caption,
      themeId = post.selectedThemeId,
      fontId = post.selectedFont.id,
      song = post.selectedSong,
      taggedUserIds = post.taggedPeople.map { it.userId },
      hashtags = post.selectedHashtags,
      localPhotoPaths = post.photos.map { it.localFilePath },
      localVideoPath = vid?.localVideoPath,
      localThumbnailPath = vid?.selectedThumbnailPath,
      frameHashes = vid?.frames?.map { it.dHashHex }.orEmpty(),
      durationMs = vid?.durationMs ?: 0L,
      trimStartMs = post.videoTrimStartMs,
      trimEndMs = post.videoTrimEndMs,
      width = vid?.width ?: 1280,
      height = vid?.height ?: 720,
      muteOriginalAudio = post.muteOriginalAudio,
      contentId = vid?.contentId ?: post.photos.firstOrNull()?.contentId.orEmpty(),
    )

    viewModelScope.launch {
      val result = repository.submitOrQueuePost(input) { pct ->
        _uiState.update { s ->
          s.copy(
            postComposer = s.postComposer.copy(
              uploadStatus = PostUploadStatus.UPLOADING,
              uploadProgressPct = pct,
            ),
          )
        }
      }

      when (result) {
        is PostPublishResult.Uploaded -> {
          _uiState.update { s ->
            s.copy(
              postComposer = s.postComposer.copy(
                uploadStatus = PostUploadStatus.UPLOADED,
                uploadProgressPct = 100,
              ),
            )
          }
          delay(650L)
          _uiState.update { s ->
            s.copy(
              postComposer = PostComposerUiState(
                isOpen = false,
                allDirectoryPeople = s.postComposer.allDirectoryPeople,
                pendingQueueCount = s.postComposer.pendingQueueCount,
              ),
              comingSoonMessage = "Uploaded",
            )
          }
        }

        is PostPublishResult.QueuedOffline -> {
          _uiState.update { s ->
            s.copy(
              postComposer = PostComposerUiState(
                isOpen = false,
                allDirectoryPeople = s.postComposer.allDirectoryPeople,
                pendingQueueCount = s.postComposer.pendingQueueCount,
              ),
              comingSoonMessage = "Queued",
            )
          }
        }

        is PostPublishResult.Rejected -> {
          _uiState.update { s ->
            s.copy(
              postComposer = s.postComposer.copy(
                uploadStatus = PostUploadStatus.FAILED,
                errorMessage = result.message,
              ),
            )
          }
        }
      }
    }
  }

  fun onContactsFilterSelected(filter: ContactFilterTab) {
    if (_uiState.value.contactsState.activeFilter == filter) return
    _uiState.update {
      it.copy(
        isBottomBarVisible = true,
        contactsState = it.contactsState.copy(
          activeFilter = filter,
          currentPage = 1,
          hasMore = false,
        ),
      )
    }
    logClientAction("contacts_filter_${filter.apiParam}")
    refreshContacts()
  }

  fun onContactsSearchChanged(query: String) {
    val limited = query.take(64)
    _uiState.update {
      it.copy(
        contactsState = it.contactsState.copy(
          searchQuery = limited,
          currentPage = 1,
          hasMore = false,
        ),
      )
    }
    contactsSearchJob?.cancel()
    contactsSearchJob = viewModelScope.launch {
      delay(280L)
      refreshContacts()
    }
  }

  fun refreshContacts() {
    val state = _uiState.value
    if (!state.isAuthenticated) return
    val filter = state.contactsState.activeFilter
    val query = state.contactsState.searchQuery
    viewModelScope.launch {
      _uiState.update { it.copy(contactsState = it.contactsState.copy(isLoading = true)) }
      val result = repository.loadContactsPage(
        filter = filter,
        query = query,
        page = 1,
        pageSize = TelefamContactsRules.PAGE_SIZE,
      )
      _uiState.update { cur ->
        if (cur.contactsState.activeFilter != filter || cur.contactsState.searchQuery != query) {
          cur.copy(contactsState = cur.contactsState.copy(isLoading = false))
        } else {
          cur.copy(
            contactsState = cur.contactsState.copy(
              isLoading = false,
              isLoadingMore = false,
              currentPage = result.page,
              hasMore = result.hasMore,
            ),
          )
        }
      }
    }
  }

  fun loadNextContactsPage() {
    val state = _uiState.value
    val contacts = state.contactsState
    if (!state.isAuthenticated || contacts.isLoading || contacts.isLoadingMore || !contacts.hasMore) {
      return
    }
    val nextPage = contacts.currentPage + 1
    val filter = contacts.activeFilter
    val query = contacts.searchQuery
    viewModelScope.launch {
      _uiState.update { it.copy(contactsState = it.contactsState.copy(isLoadingMore = true)) }
      val result = repository.loadContactsPage(
        filter = filter,
        query = query,
        page = nextPage,
        pageSize = TelefamContactsRules.PAGE_SIZE,
      )
      _uiState.update { cur ->
        if (cur.contactsState.activeFilter != filter || cur.contactsState.searchQuery != query) {
          cur.copy(contactsState = cur.contactsState.copy(isLoadingMore = false))
        } else {
          cur.copy(
            contactsState = cur.contactsState.copy(
              isLoadingMore = false,
              currentPage = result.page,
              hasMore = result.hasMore,
            ),
          )
        }
      }
    }
  }

  fun onToggleContactFollow(targetUserId: String) {
    viewModelScope.launch {
      when (val result = repository.toggleContactFollow(targetUserId)) {
        is ContactToggleResult.RateLimited -> {
          _uiState.update {
            it.copy(contactsState = it.contactsState.copy(toastMessage = result.message))
          }
        }
        ContactToggleResult.Synced, ContactToggleResult.QueuedOffline -> Unit
      }
    }
  }

  fun onContactsScrollVisibilityChanged(visible: Boolean) {
    if (_uiState.value.isBottomBarVisible != visible) {
      _uiState.update { it.copy(isBottomBarVisible = visible) }
    }
  }

  fun dismissContactsToast() {
    _uiState.update { it.copy(contactsState = it.contactsState.copy(toastMessage = null)) }
  }

  // User Profile Navigation, Actions, Followers/Following, Block, Report & Offline Cache
  fun loadOwnProfile() {
    val uid = _uiState.value.userId.trim()
    if (uid.isBlank() || !_uiState.value.isAuthenticated) return
    val existing = _uiState.value.ownProfileState ?: UserProfileScreenState(
      userId = uid,
      fullName = _uiState.value.profileForm.fullName,
      username = _uiState.value.profileForm.username,
      avatarUrl = _uiState.value.savedAvatarLocalPath,
      bio = _uiState.value.profileForm.bio,
      location = _uiState.value.profileForm.location,
      isOwner = true,
      isLoading = true,
    )
    _uiState.update { it.copy(ownProfileState = existing) }
    refreshUserProfile(uid)
  }

  fun openUserProfile(userId: String) {
    val cleanId = userId.trim()
    if (cleanId.isBlank() || !_uiState.value.isAuthenticated) return

    // Single own-profile invariant: navigating to self always switches to the single bottom-tab Own Profile
    if (cleanId == _uiState.value.userId) {
      _uiState.update { s ->
        s.copy(
          activeHomeTab = HomeTab.PROFILE,
          isBottomBarVisible = true,
          profileBackStack = emptyList(),
          profileViewerItems = null,
          showSettingsModal = false,
          showProfileMenuModal = false,
          settingsBackStack = emptyList(),
          feedsState = s.feedsState.copy(
            isSearchOpen = false,
            searchViewerItems = null,
          ),
          ownProfileState = s.ownProfileState?.copy(activeConnectionsType = null),
        )
      }
      loadOwnProfile()
      logClientAction("own_profile_focused")
      return
    }

    val currentTop = _uiState.value.profileBackStack.lastOrNull()
    if (currentTop?.userId == cleanId) return

    // Seed initial preview immediately from contacts or feed items while loading fresh server/cached data
    val contactSeed = _uiState.value.contactsState.items.firstOrNull { it.userId == cleanId }
    val feedSeed = (_uiState.value.feedsState.activeItems + _uiState.value.feedsState.searchResults)
      .firstOrNull { it.authorUserId == cleanId }
    val seedProfile = UserProfileScreenState(
      userId = cleanId,
      fullName = contactSeed?.fullName ?: feedSeed?.authorFullName ?: "",
      username = contactSeed?.username ?: feedSeed?.authorUsername ?: "",
      avatarUrl = contactSeed?.avatarLocalPath ?: feedSeed?.authorAvatarUrl,
      location = contactSeed?.countryDisplay.orEmpty(),
      isFollowing = contactSeed?.isFollowing ?: feedSeed?.isFollowingAuthor ?: false,
      followsYou = contactSeed?.followsYou ?: false,
      isOwner = false,
      relationshipState = contactSeed?.relationshipState ?: if (feedSeed?.isFollowingAuthor == true) {
        ContactRelationshipState.FOLLOWING
      } else {
        ContactRelationshipState.FOLLOW
      },
      isLoading = true,
    )

    _uiState.update { s ->
      s.copy(
        profileBackStack = s.profileBackStack + seedProfile,
        profileViewerItems = null,
      )
    }
    logClientAction("profile_opened_$cleanId")
    refreshUserProfile(cleanId)
  }

  fun closeTopUserProfile() {
    _uiState.update { s ->
      if (s.profileViewerItems != null) {
        s.copy(profileViewerItems = null)
      } else if (s.profileBackStack.isNotEmpty()) {
        s.copy(profileBackStack = s.profileBackStack.dropLast(1))
      } else {
        s
      }
    }
  }

  fun refreshUserProfile(userId: String) {
    val cleanId = userId.trim()
    if (cleanId.isBlank()) return
    val startEpoch = profileMutationEpoch[cleanId] ?: 0L
    viewModelScope.launch {
      val (tok, fp) = repository.getStreamCredentials()
      val fetched = repository.fetchUserProfile(cleanId)
      if ((profileMutationEpoch[cleanId] ?: 0L) != startEpoch) {
        return@launch
      }
      val tier = _uiState.value.networkTier
      _uiState.update { s ->
        val updatedStack = s.profileBackStack.map { entry ->
          if (entry.userId == cleanId) {
            if (fetched != null) mapProfileDtoToState(fetched, entry, tier)
            else entry.copy(isLoading = false)
          } else entry
        }
        val updatedOwn = if (s.ownProfileState?.userId == cleanId) {
          if (fetched != null) mapProfileDtoToState(fetched, s.ownProfileState, tier)
          else s.ownProfileState.copy(isLoading = false)
        } else {
          s.ownProfileState
        }
        s.copy(
          profileBackStack = updatedStack,
          ownProfileState = updatedOwn,
          feedsState = s.feedsState.copy(streamAccessToken = tok, streamDeviceFp = fp),
        )
      }
    }
  }

  fun toggleFollowOnProfile(userId: String) {
    val profile = (_uiState.value.profileBackStack.lastOrNull { it.userId == userId }
      ?: _uiState.value.ownProfileState?.takeIf { it.userId == userId }) ?: return
    if (profile.isOwner || profile.isBlocked) return
    val currentlyFollowing = profile.isFollowing
    val nextFollowing = !currentlyFollowing
    val delta = if (nextFollowing) 1L else -1L
    val nextRel = ContactRelationshipState.compute(nextFollowing, profile.followsYou)

    updateProfileInState(userId) { p ->
      p.copy(
        isFollowing = nextFollowing,
        followersCount = (p.followersCount + delta).coerceAtLeast(0L),
        relationshipState = nextRel,
      )
    }

    val cleanId = userId.trim()
    val startEpoch = profileMutationEpoch[cleanId] ?: 0L

    viewModelScope.launch {
      when (val res = repository.toggleFollowAuthorFromFeed(cleanId, currentlyFollowing)) {
        is ContactToggleResult.RateLimited -> {
          updateProfileInState(cleanId) { p ->
            p.copy(
              isFollowing = currentlyFollowing,
              followersCount = (p.followersCount - delta).coerceAtLeast(0L),
              relationshipState = ContactRelationshipState.compute(currentlyFollowing, p.followsYou),
            )
          }
          _uiState.update { it.copy(profileToastMessage = res.message) }
        }
        else -> {
          if ((profileMutationEpoch[cleanId] ?: 0L) == startEpoch) {
            refreshUserProfile(cleanId)
          }
        }
      }
    }
  }

  fun openProfileConnections(userId: String, isFollowers: Boolean) {
    val cleanId = userId.trim()
    if (cleanId.isBlank()) return
    val listType = if (isFollowers) "followers" else "following"
    updateProfileInState(cleanId) { p ->
      p.copy(
        activeConnectionsType = listType,
        connections = emptyList(),
        isLoadingConnections = true,
        connectionsPage = 1,
        connectionsHasMore = false,
      )
    }
    viewModelScope.launch {
      val res = repository.fetchUserConnections(cleanId, isFollowers = isFollowers, page = 1, pageSize = 30)
      val mapped = mapConnectionDtosToUi(res.items)
      updateProfileInState(cleanId) { p ->
        p.copy(
          connections = mapped,
          isLoadingConnections = false,
          connectionsPage = res.page,
          connectionsHasMore = res.hasMore,
        )
      }
    }
  }

  fun loadMoreProfileConnections(userId: String) {
    val profile = (_uiState.value.profileBackStack.lastOrNull { it.userId == userId }
      ?: _uiState.value.ownProfileState?.takeIf { it.userId == userId }) ?: return
    val listType = profile.activeConnectionsType ?: return
    if (profile.isLoadingConnections || !profile.connectionsHasMore) return
    val nextPage = profile.connectionsPage + 1
    updateProfileInState(userId) { it.copy(isLoadingConnections = true) }
    viewModelScope.launch {
      val res = repository.fetchUserConnections(
        userId = userId,
        isFollowers = listType == "followers",
        page = nextPage,
        pageSize = 30,
      )
      val mapped = mapConnectionDtosToUi(res.items)
      updateProfileInState(userId) { p ->
        val existingIds = p.connections.map { it.userId }.toSet()
        p.copy(
          connections = p.connections + mapped.filterNot { it.userId in existingIds },
          isLoadingConnections = false,
          connectionsPage = res.page,
          connectionsHasMore = res.hasMore,
        )
      }
    }
  }

  fun dismissProfileConnections(userId: String) {
    updateProfileInState(userId) { p ->
      p.copy(activeConnectionsType = null, connections = emptyList(), isLoadingConnections = false)
    }
  }

  fun toggleFollowInConnections(ownerUserId: String, targetUserId: String) {
    val profile = (_uiState.value.profileBackStack.lastOrNull { it.userId == ownerUserId }
      ?: _uiState.value.ownProfileState?.takeIf { it.userId == ownerUserId }) ?: return
    val row = profile.connections.firstOrNull { it.userId == targetUserId } ?: return
    if (targetUserId == _uiState.value.userId) return
    val nextFollowing = !row.isFollowing
    val nextRel = ContactRelationshipState.compute(nextFollowing, row.followsYou)

    updateProfileInState(ownerUserId) { p ->
      p.copy(
        connections = p.connections.map { c ->
          if (c.userId == targetUserId) c.copy(isFollowing = nextFollowing, relationshipState = nextRel) else c
        },
      )
    }

    viewModelScope.launch {
      when (val res = repository.toggleFollowAuthorFromFeed(targetUserId, row.isFollowing)) {
        is ContactToggleResult.RateLimited -> {
          updateProfileInState(ownerUserId) { p ->
            p.copy(
              connections = p.connections.map { c ->
                if (c.userId == targetUserId) c.copy(isFollowing = row.isFollowing, relationshipState = row.relationshipState) else c
              },
            )
          }
          _uiState.update { it.copy(profileToastMessage = res.message) }
        }
        else -> Unit
      }
    }
  }

  fun selectProfilePostFilter(userId: String, filter: ProfilePostFilter) {
    updateProfileInState(userId) { it.copy(selectedFilter = filter) }
  }

  fun selectProfileSortOrder(userId: String, sort: ProfilePostSort) {
    updateProfileInState(userId) { it.copy(selectedSort = sort) }
  }

  fun selectOwnerProfileTab(userId: String, tab: OwnerProfileTab) {
    updateProfileInState(userId) { it.copy(activeOwnerTab = tab) }
  }

  fun openOwnerSettingsModal() {
    _uiState.update {
      it.copy(
        showSettingsModal = true,
        settingsBackStack = listOf(SettingsDestination.HUB),
        settingsSearchQuery = "",
      )
    }
    viewModelScope.launch {
      refreshAllSettingsFromServer()
    }
  }

  fun dismissOwnerSettingsModal() {
    _uiState.update {
      it.copy(
        showSettingsModal = false,
        settingsBackStack = emptyList(),
        settingsSearchQuery = "",
        lastCreatedTicket = null,
      )
    }
  }

  fun openOwnerMenuModal() {
    _uiState.update { it.copy(showProfileMenuModal = true) }
  }

  fun dismissOwnerMenuModal() {
    _uiState.update { it.copy(showProfileMenuModal = false) }
  }

  fun navigateToSettings(destination: SettingsDestination) {
    _uiState.update { s ->
      val current = s.settingsBackStack.ifEmpty { listOf(SettingsDestination.HUB) }
      val next = if (destination == SettingsDestination.HUB) {
        listOf(SettingsDestination.HUB)
      } else if (current.lastOrNull() == destination) {
        current
      } else {
        current + destination
      }
      s.copy(
        showSettingsModal = true,
        settingsBackStack = next,
        lastCreatedTicket = null,
      )
    }
    when (destination) {
      SettingsDestination.BLOCKED_ACCOUNTS -> refreshRestrictedAccounts()
      SettingsDestination.OFFLINE_SYNC -> refreshOfflineSyncStatus()
      SettingsDestination.SAVED_LINKS -> viewModelScope.launch { repository.refreshSavedLinks(_uiState.value.savedLinksSearchQuery) }
      SettingsDestination.HELP_SUPPORT -> {
        refreshSupportTickets()
        searchFaqs(_uiState.value.faqSearchQuery)
        refreshSystemStatus()
      }
      SettingsDestination.TERMS_POLICIES -> loadLegalDocuments()
      SettingsDestination.ABOUT -> checkAppUpdates()
      else -> Unit
    }
  }

  fun navigateSettingsBack() {
    _uiState.update { s ->
      if (s.settingsBackStack.size > 1) {
        s.copy(settingsBackStack = s.settingsBackStack.dropLast(1), lastCreatedTicket = null)
      } else {
        s.copy(showSettingsModal = false, settingsBackStack = emptyList(), lastCreatedTicket = null)
      }
    }
  }

  fun onSettingsSearchQueryChanged(query: String) {
    _uiState.update { it.copy(settingsSearchQuery = query.take(64)) }
  }

  private suspend fun refreshAllSettingsFromServer() {
    val fetched = repository.fetchSettings()
    val fetchedChatPrivacy = repository.fetchChatPrivacy()
    val blocked = repository.listRestrictedAccounts(isMuted = false, query = _uiState.value.restrictedAccountsQuery)
    val muted = repository.listRestrictedAccounts(isMuted = true, query = _uiState.value.restrictedAccountsQuery)
    val latestSyncStatus = repository.getOfflineSyncStatus(
      isSyncing = _uiState.value.syncStatus.isSyncing,
      lastResultMessage = _uiState.value.syncStatus.lastResultMessage,
    )
    val wifiConnected = repository.isWifiOrUnmetered()
    _uiState.update { s ->
      s.copy(
        settings = fetched,
        chatPrivacy = fetchedChatPrivacy,
        syncStatus = latestSyncStatus,
        isWifiConnected = wifiConnected,
        blockedAccounts = blocked,
        mutedAccounts = muted,
        ownProfileState = s.ownProfileState?.copy(
          isPrivateAccount = fetched.privacy.isPrivateAccount,
          whoCanComment = fetched.privacy.whoCanComment.apiValue,
          whoCanMention = fetched.privacy.whoCanMention.apiValue,
          likesVisibility = fetched.privacy.likesVisibility.apiValue,
          followingVisibility = fetched.privacy.followingVisibility.apiValue,
          showActivityStatus = fetched.privacy.showActivityStatus,
        ),
      )
    }
  }

  fun openChatPrivacy() {
    _uiState.update {
      it.copy(chatPrivacyBackStack = listOf(ChatPrivacyRoute.PRIVACY))
    }
    viewModelScope.launch {
      val fetched = repository.fetchChatPrivacy()
      _uiState.update { it.copy(chatPrivacy = fetched) }
    }
  }

  fun openChatsThemePicker() {
    _uiState.update { s ->
      val base = if (s.chatPrivacyBackStack.isEmpty()) listOf(ChatPrivacyRoute.PRIVACY) else s.chatPrivacyBackStack
      val next = if (base.lastOrNull() == ChatPrivacyRoute.CHATS_THEME) base else base + ChatPrivacyRoute.CHATS_THEME
      s.copy(chatPrivacyBackStack = next)
    }
  }

  fun openBubbleColorPicker() {
    _uiState.update { s ->
      val base = if (s.chatPrivacyBackStack.isEmpty()) listOf(ChatPrivacyRoute.PRIVACY) else s.chatPrivacyBackStack
      val next = if (base.lastOrNull() == ChatPrivacyRoute.BUBBLE_COLORS) base else base + ChatPrivacyRoute.BUBBLE_COLORS
      s.copy(chatPrivacyBackStack = next)
    }
  }

  fun navigateChatPrivacyBack() {
    _uiState.update { s ->
      if (s.chatPrivacyBackStack.size > 1) {
        s.copy(chatPrivacyBackStack = s.chatPrivacyBackStack.dropLast(1))
      } else {
        s.copy(chatPrivacyBackStack = emptyList())
      }
    }
  }

  fun closeAllChatPrivacy() {
    _uiState.update { it.copy(chatPrivacyBackStack = emptyList()) }
  }

  fun updateChatPrivacy(next: ChatPrivacySettingsState) {
    val stamped = next.copy(
      chatThemeId = TelefamChatPrivacyCatalog.themeById(next.chatThemeId).themeId,
      bubbleColorId = TelefamChatPrivacyCatalog.bubbleColorById(next.bubbleColorId).colorId,
      updatedAtMs = System.currentTimeMillis(),
    )
    _uiState.update { it.copy(chatPrivacy = stamped) }
    viewModelScope.launch {
      val persisted = repository.saveChatPrivacy(stamped)
      _uiState.update { it.copy(chatPrivacy = persisted) }
    }
  }

  fun selectChatTheme(themeId: String) {
    val cleanId = TelefamChatPrivacyCatalog.themeById(themeId).themeId
    _uiState.update { s ->
      s.copy(
        chatPrivacy = s.chatPrivacy.copy(
          chatThemeId = cleanId,
          updatedAtMs = System.currentTimeMillis(),
        ),
      )
    }
    viewModelScope.launch {
      val persisted = repository.saveChatThemeAndBubbleColor(themeId = cleanId)
      _uiState.update { it.copy(chatPrivacy = persisted) }
    }
  }

  fun selectBubbleColor(colorId: String) {
    val cleanId = TelefamChatPrivacyCatalog.bubbleColorById(colorId).colorId
    _uiState.update { s ->
      s.copy(
        chatPrivacy = s.chatPrivacy.copy(
          bubbleColorId = cleanId,
          updatedAtMs = System.currentTimeMillis(),
        ),
      )
    }
    viewModelScope.launch {
      val persisted = repository.saveChatThemeAndBubbleColor(bubbleColorId = cleanId)
      _uiState.update { it.copy(chatPrivacy = persisted) }
    }
  }

  fun updatePrivacySettings(next: PrivacySettingsState) {
    val stamped = next.copy(updatedAtMs = System.currentTimeMillis())
    _uiState.update { s ->
      s.copy(
        settings = s.settings.copy(privacy = stamped),
        ownProfileState = s.ownProfileState?.copy(
          isPrivateAccount = stamped.isPrivateAccount,
          whoCanComment = stamped.whoCanComment.apiValue,
          whoCanMention = stamped.whoCanMention.apiValue,
          likesVisibility = stamped.likesVisibility.apiValue,
          followingVisibility = stamped.followingVisibility.apiValue,
          showActivityStatus = stamped.showActivityStatus,
        ),
      )
    }
    viewModelScope.launch {
      repository.savePrivacySettings(stamped)
    }
  }

  fun updateNotificationSettings(next: NotificationSettingsState) {
    val stamped = next.copy(updatedAtMs = System.currentTimeMillis())
    _uiState.update { s ->
      s.copy(settings = s.settings.copy(notifications = stamped))
    }
    viewModelScope.launch {
      repository.saveNotificationSettings(stamped)
    }
  }

  fun updateContentPreferences(next: ContentPreferencesState, resetSuggestions: Boolean = false) {
    val stamped = next.copy(updatedAtMs = System.currentTimeMillis())
    _uiState.update { s ->
      s.copy(
        settings = s.settings.copy(contentPreferences = stamped),
        profileToastMessage = if (resetSuggestions) "Suggestions reset" else s.profileToastMessage,
      )
    }
    viewModelScope.launch {
      repository.saveContentPreferences(stamped, resetSuggestions = resetSuggestions)
      refreshOfflineSyncStatus()
      if (resetSuggestions) {
        refreshFeeds()
      }
    }
  }

  fun refreshOfflineSyncStatus() {
    viewModelScope.launch {
      val currentMsg = _uiState.value.syncStatus.lastResultMessage
      val status = repository.getOfflineSyncStatus(isSyncing = _uiState.value.syncStatus.isSyncing, lastResultMessage = currentMsg)
      _uiState.update { it.copy(syncStatus = status, isWifiConnected = repository.isWifiOrUnmetered()) }
    }
  }

  fun updateSyncPreferences(next: SyncPreferencesState) {
    val stamped = next.copy(updatedAtMs = System.currentTimeMillis())
    _uiState.update { s ->
      s.copy(settings = s.settings.copy(syncPreferences = stamped))
    }
    viewModelScope.launch {
      val updatedSnapshot = repository.saveSyncPreferences(stamped)
      val status = repository.getOfflineSyncStatus(lastResultMessage = _uiState.value.syncStatus.lastResultMessage)
      _uiState.update { s ->
        s.copy(
          settings = updatedSnapshot,
          syncStatus = status,
          isWifiConnected = repository.isWifiOrUnmetered(),
        )
      }
    }
  }

  fun triggerManualSyncNow() {
    if (_uiState.value.syncStatus.isSyncing) return
    _uiState.update { s ->
      s.copy(
        syncStatus = s.syncStatus.copy(
          isSyncing = true,
          lastResultMessage = "Synchronizing queued changes with server...",
        ),
      )
    }
    viewModelScope.launch {
      val (updatedSettings, updatedStatus) = repository.performManualServerSyncNow()
      _uiState.update { s ->
        s.copy(
          settings = updatedSettings,
          syncStatus = updatedStatus,
          isWifiConnected = repository.isWifiOrUnmetered(),
          profileToastMessage = updatedStatus.lastResultMessage.takeIf { it.isNotBlank() } ?: s.profileToastMessage,
        )
      }
    }
  }

  fun onRestrictedAccountsTabSelected(tab: Int) {
    _uiState.update { it.copy(restrictedAccountsTab = tab.coerceIn(0, 1)) }
    refreshRestrictedAccounts()
  }

  fun onRestrictedAccountsQueryChanged(query: String) {
    _uiState.update { it.copy(restrictedAccountsQuery = query.take(64)) }
    refreshRestrictedAccounts()
  }

  fun refreshRestrictedAccounts() {
    val query = _uiState.value.restrictedAccountsQuery
    _uiState.update { it.copy(isLoadingRestrictedAccounts = true) }
    viewModelScope.launch {
      val blocked = repository.listRestrictedAccounts(isMuted = false, query = query)
      val muted = repository.listRestrictedAccounts(isMuted = true, query = query)
      _uiState.update {
        it.copy(
          blockedAccounts = blocked,
          mutedAccounts = muted,
          isLoadingRestrictedAccounts = false,
        )
      }
    }
  }

  fun unblockAccountFromSettings(userId: String) {
    val cleanId = userId.trim()
    if (cleanId.isBlank()) return
    _uiState.update { s ->
      s.copy(
        blockedAccounts = s.blockedAccounts.filterNot { it.userId == cleanId },
        profileToastMessage = "Account unblocked",
      )
    }
    updateProfileInState(cleanId) { it.copy(isBlocked = false) }
    viewModelScope.launch {
      repository.unblockUserProfile(cleanId)
      refreshFeeds()
    }
  }

  fun muteUserAccount(userId: String) {
    val cleanId = userId.trim()
    if (cleanId.isBlank() || cleanId == _uiState.value.userId) return
    updateProfileInState(cleanId) { it.copy(isMuted = true) }
    _uiState.update { s ->
      val existing = s.mutedAccounts.any { it.userId == cleanId }
      val targetProfile = s.profileBackStack.lastOrNull { it.userId == cleanId }
      val appended = if (!existing) {
        s.mutedAccounts + RestrictedAccountItem(
          userId = cleanId,
          fullName = targetProfile?.fullName.orEmpty().ifBlank { cleanId },
          username = targetProfile?.username.orEmpty().ifBlank { cleanId },
          avatarUrl = targetProfile?.avatarUrl,
          isMuted = true,
          createdAtMs = System.currentTimeMillis(),
        )
      } else {
        s.mutedAccounts
      }
      s.copy(
        mutedAccounts = appended,
        feedsState = s.feedsState.copy(
          searchResults = s.feedsState.searchResults.filterNot { it.authorUserId == cleanId },
          searchViewerItems = s.feedsState.searchViewerItems?.filterNot { it.authorUserId == cleanId },
          toastMessage = "Account muted",
        ),
        profileToastMessage = "Account muted",
      )
    }
    viewModelScope.launch {
      repository.muteUserAccount(cleanId)
      refreshRestrictedAccounts()
    }
  }

  fun unmuteAccountFromSettings(userId: String) {
    val cleanId = userId.trim()
    if (cleanId.isBlank()) return
    _uiState.update { s ->
      s.copy(
        mutedAccounts = s.mutedAccounts.filterNot { it.userId == cleanId },
        profileToastMessage = "Account unmuted",
      )
    }
    updateProfileInState(cleanId) { it.copy(isMuted = false) }
    viewModelScope.launch {
      repository.unmuteUserAccount(cleanId)
      refreshFeeds()
    }
  }

  fun submitProblemReport(category: String, description: String, screenshotUri: Uri?) {
    if (_uiState.value.isSubmittingProblemReport) return
    _uiState.update { it.copy(isSubmittingProblemReport = true) }
    viewModelScope.launch {
      val created = repository.submitProblemReport(category, description, screenshotUri)
      val tickets = repository.fetchSupportTickets()
      _uiState.update { s ->
        s.copy(
          isSubmittingProblemReport = false,
          lastCreatedTicket = created,
          supportTickets = tickets,
          profileToastMessage = if (created == null) "Enter at least 5 characters" else null,
        )
      }
    }
  }

  fun clearLastCreatedTicket() {
    _uiState.update { it.copy(lastCreatedTicket = null) }
  }

  fun refreshSupportTickets() {
    _uiState.update { it.copy(isLoadingSupportTickets = true) }
    viewModelScope.launch {
      val tickets = repository.fetchSupportTickets()
      _uiState.update { it.copy(supportTickets = tickets, isLoadingSupportTickets = false) }
    }
  }

  fun searchFaqs(query: String) {
    _uiState.update { it.copy(faqSearchQuery = query) }
    viewModelScope.launch {
      val faqs = repository.fetchFaqs(query)
      _uiState.update { it.copy(faqItems = faqs) }
    }
  }

  fun refreshSystemStatus() {
    _uiState.update { it.copy(isLoadingSystemStatus = true) }
    viewModelScope.launch {
      val status = repository.fetchSystemStatus()
      _uiState.update { it.copy(systemStatuses = status, isLoadingSystemStatus = false) }
    }
  }

  fun loadLegalDocuments() {
    viewModelScope.launch {
      val docs = repository.fetchLegalDocuments()
      _uiState.update { it.copy(legalDocuments = docs) }
    }
  }

  fun selectLegalDocument(docId: String) {
    _uiState.update { it.copy(selectedLegalDocId = docId) }
  }

  fun checkAppUpdates() {
    _uiState.update { it.copy(isCheckingAppUpdates = true) }
    viewModelScope.launch {
      val res = repository.checkAppForUpdates()
      _uiState.update { it.copy(appVersionCheck = res, isCheckingAppUpdates = false) }
    }
  }

  fun requestLogoutConfirmation() {
    _uiState.update { it.copy(showLogoutConfirmDialog = true, logoutAllDevicesChoice = false) }
  }

  fun dismissLogoutConfirmation() {
    _uiState.update { it.copy(showLogoutConfirmDialog = false) }
  }

  fun setLogoutAllDevicesChoice(allDevices: Boolean) {
    _uiState.update { it.copy(logoutAllDevicesChoice = allDevices) }
  }

  fun confirmSignOut() {
    val allDevices = _uiState.value.logoutAllDevicesChoice
    _uiState.update { it.copy(showLogoutConfirmDialog = false) }
    signOut(allDevices = allDevices)
  }

  fun openOwnerBackgroundsModal() {
    _uiState.update { it.copy(showBackgroundsModal = true, backgroundError = null) }
  }

  fun dismissOwnerBackgroundsModal() {
    _uiState.update { it.copy(showBackgroundsModal = false, backgroundError = null) }
  }

  fun openEditProfileModal() {
    _uiState.update {
      it.copy(
        showEditProfileModal = true,
        editProfileError = null,
        editProfileCropBitmap = null,
        editProfileProcessedAvatar = null,
      )
    }
  }

  fun dismissEditProfileModal() {
    _uiState.update {
      it.copy(
        showEditProfileModal = false,
        editProfileError = null,
        editProfileCropBitmap = null,
      )
    }
  }

  fun onEditProfilePhotoPicked(bitmap: Bitmap) {
    _uiState.update { it.copy(editProfileCropBitmap = bitmap, editProfileError = null) }
  }

  fun dismissEditProfileCropper() {
    _uiState.update { it.copy(editProfileCropBitmap = null) }
  }

  fun confirmEditProfileCroppedPhoto(bitmap: Bitmap, centerX: Float, centerY: Float, zoomScale: Float) {
    val uid = _uiState.value.userId.ifBlank { "local_user" }
    _uiState.update { it.copy(editProfileCropBitmap = null, isSavingEditedProfile = true) }
    viewModelScope.launch {
      val processed = repository.processAndUploadAvatar(uid, bitmap, centerX, centerY, zoomScale)
      _uiState.update { s ->
        s.copy(
          editProfileProcessedAvatar = processed,
          isSavingEditedProfile = false,
          savedAvatarLocalPath = processed.localFilePath,
        )
      }
      updateProfileInState(uid) { p -> p.copy(avatarUrl = processed.localFilePath) }
    }
  }

  fun uploadOwnerBackgrounds(uris: List<Uri>) {
    if (uris.isEmpty()) return
    val uid = _uiState.value.userId
    if (uid.isBlank()) return
    val currentCount = _uiState.value.ownProfileState?.backgrounds?.size
      ?: _uiState.value.activeUserProfile?.takeIf { it.isOwner }?.backgrounds?.size ?: 0
    val remaining = (5 - currentCount).coerceAtLeast(0)
    if (remaining <= 0) {
      _uiState.update { it.copy(backgroundError = "You can upload up to 5 background photos.") }
      return
    }
    _uiState.update { it.copy(isUploadingBackground = true, backgroundError = null) }
    viewModelScope.launch {
      var lastError: String? = null
      var latestList: List<ProfileBackgroundItemDto>? = null
      for (uri in uris.take(remaining)) {
        when (val res = repository.uploadProfileBackground(uri)) {
          is ProfileBackgroundResult.Updated -> latestList = res.backgrounds
          is ProfileBackgroundResult.Error -> {
            lastError = res.message
            break
          }
        }
      }
      latestList?.let { dtos ->
        val mapped = mapBackgroundDtos(dtos, emptyList())
        updateProfileInState(uid) { it.copy(backgrounds = mapped) }
      }
      _uiState.update {
        it.copy(
          isUploadingBackground = false,
          backgroundError = lastError,
          profileToastMessage = if (lastError == null) "Background updated" else null,
        )
      }
      if (lastError == null) {
        refreshUserProfile(uid)
      }
    }
  }

  fun deleteOwnerBackground(mediaId: String) {
    val uid = _uiState.value.userId
    if (uid.isBlank()) return
    _uiState.update { it.copy(isUploadingBackground = true, backgroundError = null) }
    viewModelScope.launch {
      when (val res = repository.deleteProfileBackground(mediaId)) {
        is ProfileBackgroundResult.Updated -> {
          val mapped = mapBackgroundDtos(res.backgrounds, emptyList())
          updateProfileInState(uid) { it.copy(backgrounds = mapped) }
          _uiState.update { it.copy(isUploadingBackground = false, profileToastMessage = "Background removed") }
        }
        is ProfileBackgroundResult.Error -> {
          _uiState.update { it.copy(isUploadingBackground = false, backgroundError = res.message) }
        }
      }
    }
  }

  fun reorderOwnerBackgrounds(mediaIds: List<String>) {
    val uid = _uiState.value.userId
    if (uid.isBlank() || mediaIds.isEmpty()) return
    _uiState.update { it.copy(isUploadingBackground = true, backgroundError = null) }
    viewModelScope.launch {
      when (val res = repository.reorderProfileBackgrounds(mediaIds)) {
        is ProfileBackgroundResult.Updated -> {
          val mapped = mapBackgroundDtos(res.backgrounds, emptyList())
          updateProfileInState(uid) { it.copy(backgrounds = mapped) }
          _uiState.update { it.copy(isUploadingBackground = false) }
        }
        is ProfileBackgroundResult.Error -> {
          _uiState.update { it.copy(isUploadingBackground = false, backgroundError = res.message) }
        }
      }
    }
  }

  fun saveEditedProfile(
    fullNameRaw: String,
    usernameRaw: String,
    country: CountryDialOption,
    phoneDigitsRaw: String,
    genderRaw: String,
    dobYear: Int,
    dobMonthZeroBased: Int,
    dobDay: Int,
    locationRaw: String,
    bioRaw: String,
  ) {
    val state = _uiState.value
    val uid = state.userId
    if (uid.isBlank() || state.isSavingEditedProfile) return
    _uiState.update { it.copy(isSavingEditedProfile = true, editProfileError = null) }
    viewModelScope.launch {
      val avatarToUse = state.editProfileProcessedAvatar
      when (
        val res = repository.updateUserProfile(
          fullNameRaw = fullNameRaw,
          usernameRaw = usernameRaw,
          dialCode = country.dialCode,
          phoneDigitsRaw = phoneDigitsRaw,
          countryIso = country.iso,
          genderRaw = genderRaw,
          dobYear = dobYear,
          dobMonthZeroBased = dobMonthZeroBased,
          dobDay = dobDay,
          locationRaw = locationRaw,
          bioRaw = bioRaw,
          avatar = avatarToUse,
        )
      ) {
        is ProfileActionResult.Saved -> {
          val cleanFullName = TelefamProfileSanitizer.sanitizePlainText(fullNameRaw, 64, singleLine = true)
          val cleanUsername = TelefamProfileSanitizer.sanitizeUsernameInput(usernameRaw)
          val cleanLocation = TelefamProfileSanitizer.sanitizePlainText(locationRaw, 80, singleLine = true)
          val cleanBio = TelefamProfileSanitizer.sanitizePlainText(bioRaw, 150, singleLine = false)
          val dobIso = String.format("%04d-%02d-%02d", dobYear, dobMonthZeroBased + 1, dobDay)
          val cleanDigits = phoneDigitsRaw.filter { it.isDigit() }
          val phoneE164 = if (cleanDigits.isNotBlank()) "${country.dialCode}$cleanDigits" else ""
          updateProfileInState(uid) { p ->
            p.copy(
              fullName = cleanFullName,
              username = cleanUsername,
              location = cleanLocation,
              bio = cleanBio,
              countryIso = country.iso,
              gender = genderRaw,
              dateOfBirth = dobIso,
              phoneNumber = phoneE164.ifBlank { p.phoneNumber },
              avatarUrl = avatarToUse?.localFilePath ?: p.avatarUrl,
            )
          }
          _uiState.update { s ->
            s.copy(
              isSavingEditedProfile = false,
              showEditProfileModal = false,
              editProfileError = null,
              editProfileProcessedAvatar = null,
              savedAvatarLocalPath = avatarToUse?.localFilePath ?: s.savedAvatarLocalPath,
              profileToastMessage = "Profile updated",
            )
          }
          refreshUserProfile(uid)
        }
        is ProfileActionResult.Error -> {
          _uiState.update {
            it.copy(
              isSavingEditedProfile = false,
              editProfileError = res.message,
            )
          }
        }
      }
    }
  }

  fun openProfilePostViewer(posts: List<FeedUiItem>, index: Int) {
    if (posts.isEmpty()) return
    val safeIndex = index.coerceIn(0, posts.lastIndex)
    val clicked = posts[safeIndex]
    val updatedList = posts.mapIndexed { idx, item ->
      if (idx == safeIndex) item.copy(viewsCount = item.viewsCount + 1L) else item
    }
    _uiState.update { s ->
      s.copy(
        profileViewerItems = updatedList,
        profileViewerInitialIndex = safeIndex,
        profileBackStack = s.profileBackStack.map { p ->
          p.copy(posts = p.posts.map { if (it.postId == clicked.postId) it.copy(viewsCount = it.viewsCount + 1L) else it })
        },
        ownProfileState = s.ownProfileState?.let { p ->
          p.copy(posts = p.posts.map { if (it.postId == clicked.postId) it.copy(viewsCount = it.viewsCount + 1L) else it })
        },
      )
    }
    viewModelScope.launch {
      repository.recordPostView(clicked.postId)
    }
  }

  fun closeProfilePostViewer() {
    _uiState.update { it.copy(profileViewerItems = null) }
  }

  fun blockUserFromProfile(userId: String) {
    val cleanId = userId.trim()
    if (cleanId.isBlank() || cleanId == _uiState.value.userId) return
    profileMutationEpoch[cleanId] = (profileMutationEpoch[cleanId] ?: 0L) + 1L
    updateProfileInState(cleanId) { p ->
      p.copy(
        isBlocked = true,
        isFollowing = false,
        relationshipState = ContactRelationshipState.FOLLOW,
        posts = emptyList(),
      )
    }
    _uiState.update { s ->
      s.copy(
        feedsState = s.feedsState.copy(
          searchResults = s.feedsState.searchResults.filterNot { it.authorUserId == cleanId },
          searchViewerItems = s.feedsState.searchViewerItems?.filterNot { it.authorUserId == cleanId },
        ),
        profileToastMessage = "User blocked",
      )
    }
    viewModelScope.launch {
      repository.blockUserProfile(cleanId)
      refreshFeeds()
    }
  }

  fun unblockUserFromProfile(userId: String) {
    val cleanId = userId.trim()
    if (cleanId.isBlank()) return
    profileMutationEpoch[cleanId] = (profileMutationEpoch[cleanId] ?: 0L) + 1L
    updateProfileInState(cleanId) { p -> p.copy(isBlocked = false, isLoading = true) }
    viewModelScope.launch {
      repository.unblockUserProfile(cleanId)
      _uiState.update { it.copy(profileToastMessage = "User unblocked") }
      refreshUserProfile(cleanId)
      refreshFeeds()
    }
  }

  fun reportUserFromProfile(userId: String, reason: String, details: String) {
    val cleanId = userId.trim()
    if (cleanId.isBlank()) return
    viewModelScope.launch {
      val ok = repository.reportUserProfile(cleanId, reason, details)
      _uiState.update {
        it.copy(profileToastMessage = if (ok) "Report submitted" else "Unable to submit report right now")
      }
    }
  }

  fun showProfileToast(message: String) {
    _uiState.update { it.copy(profileToastMessage = message) }
  }

  fun dismissProfileToast() {
    _uiState.update { it.copy(profileToastMessage = null) }
  }

  private fun updateProfileInState(
    userId: String,
    transform: (UserProfileScreenState) -> UserProfileScreenState,
  ) {
    _uiState.update { s ->
      s.copy(
        profileBackStack = s.profileBackStack.map { p -> if (p.userId == userId) transform(p) else p },
        ownProfileState = s.ownProfileState?.let { p -> if (p.userId == userId) transform(p) else p },
      )
    }
  }

  private fun mapConnectionDtosToUi(items: List<ContactItemDto>): List<ContactUiItem> {
    val countryByIso = CountryCatalog.allCountries.associateBy { it.iso.uppercase() }
    val baseUrl = BuildConfig.TELEFAM_API_BASE_URL
    return items.map { dto ->
      val countryName = countryByIso[dto.countryIso.uppercase()]?.name
        ?: dto.location.ifBlank { dto.countryIso }
      val avatar = dto.avatarUrl?.takeIf { it.isNotBlank() }?.let {
        AdaptiveStreamSelector.resolveMediaUrl(baseUrl, it)
      }
      ContactUiItem(
        userId = dto.userId,
        fullName = dto.fullName,
        username = dto.username,
        countryDisplay = countryName,
        avatarLocalPath = avatar,
        isFollowing = dto.isFollowing,
        followsYou = dto.followsYou,
        relationshipState = ContactRelationshipState.compute(dto.isFollowing, dto.followsYou),
        initials = TelefamContactsRules.initialsFor(dto.fullName, dto.username),
      )
    }
  }

  fun triggerComingSoon(featureLabel: String, actionKey: String) {
    _uiState.update { it.copy(comingSoonMessage = "$featureLabel — Coming soon") }
    logClientAction(actionKey)
  }

  fun dismissComingSoon() {
    _uiState.update { it.copy(comingSoonMessage = null) }
  }

  fun signOut() = signOut(allDevices = false)

  fun signOut(allDevices: Boolean) {
    countdownJob?.cancel()
    deviceApprovalPollJob?.cancel()
    incomingLoginPollerJob?.cancel()
    incomingCountdownJob?.cancel()
    emailCheckJob?.cancel()
    usernameCheckJob?.cancel()
    contactsSearchJob?.cancel()
    feedSearchJob?.cancel()
    callController.hangUp("logout")
    chatController.cleanup()
    lastInitializedSettingsUid = ""
    viewModelScope.launch {
      repository.signOut(allDevices = allDevices)
      _uiState.update {
        TelefamUiState(isSessionInitialized = true, themeMode = it.themeMode, networkTier = it.networkTier)
      }
    }
  }

  private fun onAuthVerified(result: AuthActionResult.Verified) {
    countdownJob?.cancel()
    deviceApprovalPollJob?.cancel()
    val (autoName, autoUser) = TelefamProfileSanitizer.autofillFromEmail(result.email)
    _uiState.update {
      it.copy(
        isLoading = false,
        isAuthenticated = true,
        profileCompleted = result.profileCompleted,
        userId = result.userId,
        userEmail = result.email,
        authStep = AuthStep.EMAIL_INPUT,
        errorMessage = null,
        otpCode = "",
        cooldownRemainingSeconds = 0L,
        pendingLoginApprovalWait = null,
        tryAnotherWayGoogleExpectedEmail = null,
        tryAnotherWayApprovalRequestId = null,
        profileForm = if (result.profileCompleted) {
          it.profileForm
        } else {
          it.profileForm.copy(
            fullName = autoName,
            username = autoUser,
            isUsernameAvailable = true,
          )
        },
      )
    }
    if (result.profileCompleted) {
      viewModelScope.launch {
        loadOwnProfile()
        refreshFeeds()
        refreshContacts()
        refreshAllSettingsFromServer()
        repository.refreshSavedLinks()
        repository.refreshNotifications()
        repository.syncFcmTokenIfAuthenticated()
        repository.captureAndSyncDeviceContext(force = true)
      }
    }
  }

  // Smart Text Link Previews, In-App Browser & Saved Links
  fun requestLinkPreview(rawUrl: String) {
    val normalized = SmartTextDetector.normalizeSafeHttpUrl(rawUrl) ?: return
    val current = _uiState.value.linkPreviews[normalized]
    if (current is LinkPreviewUiState.Loaded || !inFlightLinkPreviews.add(normalized)) return

    val domain = SmartTextDetector.extractCleanDomain(normalized)
    _uiState.update { s ->
      if (s.linkPreviews.containsKey(normalized)) s
      else s.copy(linkPreviews = s.linkPreviews + (normalized to LinkPreviewUiState.Loading(normalized, domain)))
    }
    viewModelScope.launch {
      try {
        val fetched = repository.fetchLinkPreview(normalized)
        if (fetched != null) {
          _uiState.update { s ->
            s.copy(linkPreviews = s.linkPreviews + (normalized to LinkPreviewUiState.Loaded(fetched)))
          }
        }
      } finally {
        inFlightLinkPreviews.remove(normalized)
      }
    }
  }

  fun openInAppBrowser(rawUrl: String, source: String = "chat", titleHint: String = "") {
    val normalized = SmartTextDetector.normalizeSafeHttpUrl(rawUrl) ?: run {
      showSmartActionToast("Blocked unsafe URL")
      return
    }
    requestLinkPreview(normalized)
    _uiState.update {
      it.copy(
        inAppBrowserState = InAppBrowserSessionState(
          url = normalized,
          source = source,
          titleHint = titleHint,
        ),
      )
    }
  }

  fun closeInAppBrowser() {
    _uiState.update { it.copy(inAppBrowserState = null) }
  }

  fun toggleBookmarkLink(
    rawUrl: String,
    source: String = "browser",
    titleHint: String = "",
    descriptionHint: String = "",
    imageUrlHint: String = "",
    faviconUrlHint: String = "",
  ) {
    val normalized = SmartTextDetector.normalizeSafeHttpUrl(rawUrl) ?: return
    val existing = _uiState.value.savedLinks.firstOrNull { it.normalizedUrl == normalized }
    viewModelScope.launch {
      if (existing != null) {
        repository.removeSavedLinkById(existing.id)
        showSmartActionToast("Removed from Saved links")
      } else {
        val preview = (_uiState.value.linkPreviews[normalized] as? LinkPreviewUiState.Loaded)?.preview
        repository.saveLink(
          rawUrl = normalized,
          title = titleHint.ifBlank { preview?.title.orEmpty() },
          description = descriptionHint.ifBlank { preview?.description.orEmpty() },
          imageUrl = imageUrlHint.ifBlank { preview?.imageUrl.orEmpty() },
          faviconUrl = faviconUrlHint.ifBlank { preview?.faviconUrl.orEmpty() },
          domain = preview?.domain.orEmpty(),
          source = source,
        )
        showSmartActionToast("Saved")
      }
    }
  }

  fun deleteSavedLink(id: String) {
    viewModelScope.launch {
      repository.removeSavedLinkById(id)
    }
  }

  fun onSavedLinksSearchQueryChanged(query: String) {
    _uiState.update { it.copy(savedLinksSearchQuery = query.take(80)) }
  }

  fun showSmartActionToast(message: String) {
    _uiState.update { it.copy(smartActionToast = message) }
  }

  fun dismissSmartActionToast() {
    _uiState.update { it.copy(smartActionToast = null) }
  }

  // Push Notifications Inbox & Deep-Link Routing
  fun openNotificationsSheet() {
    _uiState.update { it.copy(showNotificationsSheet = true) }
    viewModelScope.launch {
      repository.refreshNotifications()
    }
  }

  fun closeNotificationsSheet() {
    _uiState.update { it.copy(showNotificationsSheet = false) }
  }

  fun markAllNotificationsRead() {
    viewModelScope.launch {
      repository.markAllNotificationsRead()
    }
  }

  fun openNotificationItem(item: CachedNotificationEntity) {
    viewModelScope.launch {
      repository.markNotificationRead(item.id)
    }
    _uiState.update { it.copy(showNotificationsSheet = false) }
    val route = when {
      item.conversationId.isNotBlank() -> "chat"
      item.postId.isNotBlank() -> "post"
      item.targetUserId.isNotBlank() || item.actorUserId.isNotBlank() -> "profile"
      else -> "inbox"
    }
    routeNotificationTarget(
      route = route,
      conversationId = item.conversationId,
      peerUserId = item.actorUserId,
      targetUserId = item.targetUserId.ifBlank { item.actorUserId },
      postId = item.postId,
    )
  }

  fun handleNotificationDeepLink(
    route: String,
    notificationId: String = "",
    conversationId: String = "",
    peerUserId: String = "",
    targetUserId: String = "",
    postId: String = "",
  ) {
    if (notificationId.isNotBlank()) {
      viewModelScope.launch {
        repository.markNotificationRead(notificationId)
      }
    }
    routeNotificationTarget(
      route = route,
      conversationId = conversationId,
      peerUserId = peerUserId,
      targetUserId = targetUserId,
      postId = postId,
    )
  }

  private fun routeNotificationTarget(
    route: String,
    conversationId: String,
    peerUserId: String,
    targetUserId: String,
    postId: String,
  ) {
    val state = _uiState.value
    if (!state.isAuthenticated || !state.profileCompleted) return
    when (route.lowercase()) {
      "chat" -> {
        _uiState.update { it.copy(activeHomeTab = HomeTab.CHATS, showNotificationsSheet = false) }
        if (conversationId.isNotBlank()) {
          chatController.openConversation(conversationId, peerUserId)
        } else if (peerUserId.isNotBlank()) {
          chatController.openDirectChat(peerUserId, "", "", "")
        }
      }
      "post" -> {
        val allLoadedPosts = state.feedsState.forYouItems +
          state.feedsState.followingItems +
          state.feedsState.searchResults +
          state.ownProfileState?.posts.orEmpty()
        val match = allLoadedPosts.firstOrNull { it.postId == postId }
        if (match != null) {
          openProfilePostViewer(listOf(match), 0)
        } else if (targetUserId.isNotBlank() || peerUserId.isNotBlank()) {
          openUserProfile(targetUserId.ifBlank { peerUserId })
        } else {
          _uiState.update { it.copy(activeHomeTab = HomeTab.FEEDS) }
          refreshFeeds()
        }
      }
      "profile" -> {
        val uid = targetUserId.ifBlank { peerUserId }
        if (uid.isNotBlank()) {
          openUserProfile(uid)
        }
      }
      "inbox" -> {
        openNotificationsSheet()
      }
      "stars" -> {
        val country = state.ownProfileState?.countryIso?.takeIf { it.isNotBlank() }
          ?: state.profileForm.selectedCountry.iso
        starsController.openStarsHub(country, state.profileForm.location)
      }
      "active_call" -> {
        callController.expandFromMinimized()
      }
    }
  }

  fun handleIncomingCallIntent(payload: IncomingCallPushPayload) {
    callController.handleIncomingCallPush(payload)
  }

  private fun logClientAction(action: String) {
    val uid = _uiState.value.userId
    if (uid.isBlank()) return
    viewModelScope.launch {
      repository.logAudit(uid, action, "{}")
    }
  }

  private fun startCountdown(seconds: Long) {
    countdownJob?.cancel()
    if (seconds <= 0L) return
    countdownJob = viewModelScope.launch {
      var remaining = seconds
      while (remaining > 0L) {
        delay(1_000L)
        remaining--
        _uiState.update { it.copy(cooldownRemainingSeconds = remaining) }
      }
    }
  }

  private fun isValidEmail(email: String): Boolean {
    return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
  }

  override fun onCleared() {
    super.onCleared()
    stopSongPreview()
    callController.dispose()
    chatController.cleanup()
  }

  class Factory(private val repository: TelefamRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return TelefamViewModel(repository) as T
    }
  }
}

private data class FeedFilterSignal(
  val userId: String,
  val isAuthenticated: Boolean,
  val tier: NetworkTier,
  val notInterestedTopics: Set<String>,
  val restrictedUserIds: Set<String>,
)
