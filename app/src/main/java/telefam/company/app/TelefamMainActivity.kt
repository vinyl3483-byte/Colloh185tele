package telefam.company.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.DisposableEffect
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.telefam.company.app.R
import telefam.company.calls.IncomingCallPushPayload
import telefam.company.calls.TelefamCallType
import telefam.company.core.ChatPrivacyRoute
import telefam.company.core.SmartEntity
import telefam.company.ui.components.EditBackgroundsBottomSheet
import telefam.company.ui.components.EditProfileModalSheet
import telefam.company.ui.components.LocalSmartTextActions
import telefam.company.ui.components.SmartEmailActionSheet
import telefam.company.ui.components.SmartPhoneActionSheet
import telefam.company.ui.components.SmartTextActions
import telefam.company.ui.components.TelefamAnimatedSplashOverlay
import telefam.company.ui.screens.DeviceApprovalWaitScreen
import telefam.company.ui.screens.HomeGlobalSearchOverlay
import telefam.company.ui.screens.IncomingLoginRequestFullScreenOverlay
import telefam.company.ui.screens.LoginApprovedFullScreenOverlay
import telefam.company.ui.screens.ReusableVideoFeedScreen
import telefam.company.ui.screens.TelefamAuthScreen
import telefam.company.ui.screens.TelefamCallOverlayHost
import telefam.company.notifications.TelefamNotificationManager
import telefam.company.ui.screens.TelefamChatPrivacyHostScreen
import telefam.company.ui.screens.TelefamHomeScreen
import telefam.company.ui.screens.TelefamInAppBrowserScreen
import telefam.company.ui.screens.TelefamNotificationsSheet
import telefam.company.ui.screens.TelefamOneOnOneChatScreen
import telefam.company.ui.screens.TelefamPostScreen
import telefam.company.ui.screens.TelefamProfileSetupScreen
import telefam.company.ui.screens.TelefamSearchScreen
import telefam.company.ui.screens.TelefamSettingsScreen
import telefam.company.ui.screens.TelefamUserProfileScreen
import telefam.company.ui.theme.TelefamTheme
import telefam.company.ui.theme.ThemeMode
import telefam.company.ui.viewmodel.AuthStep
import telefam.company.ui.viewmodel.HomeTab
import telefam.company.ui.viewmodel.TelefamViewModel

private enum class AppRootRoute {
  AUTH,
  PROFILE_SETUP,
  HOME,
}

class TelefamMainActivity : ComponentActivity() {
  private val viewModel: TelefamViewModel by viewModels {
    TelefamViewModel.Factory((application as TelefamApplication).repository)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.light(
        android.graphics.Color.TRANSPARENT,
        android.graphics.Color.TRANSPARENT,
      ),
      navigationBarStyle = SystemBarStyle.light(
        android.graphics.Color.TRANSPARENT,
        android.graphics.Color.TRANSPARENT,
      ),
    )

    handleVerificationIntent(intent)

    setContent {
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()
      val callUiState by viewModel.callController.uiState.collectAsStateWithLifecycle()
      val systemDark = isSystemInDarkTheme()
      val useDarkBars = when (uiState.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
      }

      val route = when {
        !uiState.isAuthenticated -> AppRootRoute.AUTH
        !uiState.profileCompleted -> AppRootRoute.PROFILE_SETUP
        else -> AppRootRoute.HOME
      }

      LaunchedEffect(
        useDarkBars,
        route,
        uiState.authStep,
        uiState.activeHomeTab,
        uiState.feedsState.isSearchOpen,
        uiState.feedsState.searchViewerItems != null,
        uiState.activeUserProfile != null,
        uiState.profileViewerItems != null,
        uiState.postComposer.isOpen,
        uiState.activeSettingsDestination != null,
        uiState.activeChatPrivacyRoute,
        uiState.incomingLoginRequest != null,
        uiState.loginApprovedOverlay != null,
        uiState.inAppBrowserState != null,
        callUiState.isSessionVisible && !callUiState.isMinimizedToChat,
      ) {
        val isApprovalOverlayVisible =
          route == AppRootRoute.HOME && (uiState.incomingLoginRequest != null || uiState.loginApprovedOverlay != null)
        val isWaitScreenVisible =
          route == AppRootRoute.AUTH && (uiState.authStep == AuthStep.DEVICE_APPROVAL_WAIT || uiState.authStep == AuthStep.DEVICE_APPROVAL_DENIED)
        val isFullCallVisible = callUiState.isSessionVisible && !callUiState.isMinimizedToChat
        val statusBarIsDark = when {
          isFullCallVisible || isApprovalOverlayVisible || isWaitScreenVisible -> true
          route == AppRootRoute.HOME && uiState.inAppBrowserState != null -> useDarkBars
          uiState.postComposer.isOpen -> true
          route == AppRootRoute.HOME && uiState.showNotificationsSheet -> true
          route == AppRootRoute.HOME && uiState.activeSettingsDestination != null -> true
          route == AppRootRoute.HOME && uiState.activeChatPrivacyRoute != null -> {
            if (uiState.activeChatPrivacyRoute == ChatPrivacyRoute.CHATS_THEME) useDarkBars else true
          }
          route == AppRootRoute.HOME && uiState.profileViewerItems != null -> true
          route == AppRootRoute.HOME && uiState.activeUserProfile != null -> {
            val activeProf = uiState.activeUserProfile
            if (activeProf != null && !activeProf.isBlocked && (activeProf.isOwner || activeProf.backgrounds.isNotEmpty())) {
              true
            } else {
              useDarkBars
            }
          }
          route == AppRootRoute.HOME && uiState.feedsState.isSearchOpen -> {
            if (uiState.feedsState.searchViewerItems != null) true else useDarkBars
          }
          route == AppRootRoute.HOME && uiState.activeHomeTab == HomeTab.FEEDS -> true
          route == AppRootRoute.HOME && uiState.activeHomeTab == HomeTab.PROFILE -> true
          route == AppRootRoute.AUTH -> useDarkBars
          else -> true
        }
        val navBarIsDark = useDarkBars || isApprovalOverlayVisible || isWaitScreenVisible || (
          route == AppRootRoute.HOME && (
            uiState.profileViewerItems != null ||
              (uiState.activeHomeTab == HomeTab.FEEDS && !uiState.feedsState.isSearchOpen && uiState.activeUserProfile == null)
          )
        )
        enableEdgeToEdge(
          statusBarStyle = if (statusBarIsDark) {
            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
          } else {
            SystemBarStyle.light(
              android.graphics.Color.TRANSPARENT,
              android.graphics.Color.TRANSPARENT,
            )
          },
          navigationBarStyle = if (navBarIsDark) {
            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
          } else {
            SystemBarStyle.light(
              android.graphics.Color.TRANSPARENT,
              android.graphics.Color.TRANSPARENT,
            )
          },
        )
      }

      var showSplash by remember { mutableStateOf(true) }
      LaunchedEffect(uiState.isSessionInitialized) {
        if (uiState.isSessionInitialized) {
          delay(420L)
          showSplash = false
        }
      }

      val activeChatConv = uiState.chatState.activeConversation
      val shouldBlockScreenshots = route == AppRootRoute.HOME && (
        uiState.chatState.activeViewOnceOverlay != null ||
          (activeChatConv != null && (uiState.chatPrivacy.blocksScreenshots || !activeChatConv.canScreenshot)) ||
          (uiState.chatPrivacy.blocksScreenshots && (uiState.activeHomeTab == HomeTab.CHATS || uiState.activeChatPrivacyRoute != null))
        )
      DisposableEffect(shouldBlockScreenshots) {
        if (shouldBlockScreenshots) {
          window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
          window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
          window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
      }

      // Register screenshot capture detection (API 34+ ScreenCaptureCallback + fallback MediaStore observer)
      val watchCapture = route == AppRootRoute.HOME && activeChatConv != null
      DisposableEffect(watchCapture, activeChatConv?.conversationId) {
        if (!watchCapture) {
          onDispose { }
        } else {
          val captureCb: Any? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val cb = ScreenCaptureCallback {
              viewModel.chatController.recordScreenshotOrSecurityCapture()
            }
            runCatching { registerScreenCaptureCallback(mainExecutor, cb) }
            cb
          } else {
            null
          }
          val mediaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
              if (uri?.toString()?.contains("screenshot", ignoreCase = true) == true) {
                viewModel.chatController.recordScreenshotOrSecurityCapture()
              }
            }
          }
          runCatching {
            contentResolver.registerContentObserver(
              MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
              true,
              mediaObserver,
            )
          }
          onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && captureCb is ScreenCaptureCallback) {
              runCatching { unregisterScreenCaptureCallback(captureCb) }
            }
            runCatching { contentResolver.unregisterContentObserver(mediaObserver) }
          }
        }
      }

      val context = LocalContext.current
      val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
      ) { granted ->
        if (granted) {
          viewModel.openNotificationsSheet().also { viewModel.closeNotificationsSheet() }
        }
      }
      var hasRequestedNotifPermission by remember { mutableStateOf(false) }
      LaunchedEffect(route, uiState.isSessionInitialized) {
        if (route == AppRootRoute.HOME && uiState.isSessionInitialized && !hasRequestedNotifPermission) {
          hasRequestedNotifPermission = true
          TelefamNotificationManager.ensureChannels(context)
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
          ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          }
        }
      }

      DisposableEffect(this@TelefamMainActivity) {
        val observer = LifecycleEventObserver { _, event ->
          when (event) {
            Lifecycle.Event.ON_START -> viewModel.callController.onAppForegroundChanged(true)
            Lifecycle.Event.ON_STOP -> viewModel.callController.onAppForegroundChanged(false)
            else -> Unit
          }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
      }

      var pendingCallPermissionCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
      val callPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
      ) { grants ->
        val micGranted = grants[Manifest.permission.RECORD_AUDIO] == true ||
          ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (micGranted) {
          pendingCallPermissionCallback?.invoke()
        } else {
          Toast.makeText(context, "Microphone permission is required for calls", Toast.LENGTH_SHORT).show()
        }
        pendingCallPermissionCallback = null
      }

      val screenShareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
      ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
          viewModel.callController.startScreenShare(data)
        }
      }

      val ensureCallPermissionsAndRun: (TelefamCallType, () -> Unit) -> Unit = { callType, block ->
        val needed = buildList {
          if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            add(Manifest.permission.RECORD_AUDIO)
          }
          if (callType == TelefamCallType.VIDEO &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
          ) {
            add(Manifest.permission.CAMERA)
          }
        }
        if (needed.isEmpty()) {
          block()
        } else {
          pendingCallPermissionCallback = block
          callPermissionsLauncher.launch(needed.toTypedArray())
        }
      }
      var activePhoneAction by remember { mutableStateOf<SmartEntity.PhoneNumber?>(null) }
      var activeEmailAction by remember { mutableStateOf<SmartEntity.Email?>(null) }
      val savedLinkUrls = remember(uiState.savedLinks) {
        uiState.savedLinks.map { it.normalizedUrl }.toSet()
      }
      val smartActions = remember(uiState.linkPreviews, savedLinkUrls) {
        SmartTextActions(
          onOpenUrl = { url, source -> viewModel.openInAppBrowser(url, source) },
          onPhoneClick = { phone -> activePhoneAction = phone },
          onEmailClick = { email -> activeEmailAction = email },
          onOtpCopy = { code ->
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText("OTP", code))
            viewModel.showSmartActionToast(context.getString(R.string.smart_otp_copied))
          },
          onRequestLinkPreview = viewModel::requestLinkPreview,
          onToggleSaveLink = { url, source -> viewModel.toggleBookmarkLink(url, source) },
          linkPreviews = uiState.linkPreviews,
          savedLinkUrls = savedLinkUrls,
        )
      }

      LaunchedEffect(uiState.smartActionToast) {
        if (!uiState.smartActionToast.isNullOrBlank()) {
          delay(1_800L)
          viewModel.dismissSmartActionToast()
        }
      }

      TelefamTheme(themeMode = uiState.themeMode) {
        CompositionLocalProvider(LocalSmartTextActions provides smartActions) {
          BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideSplitHost = maxWidth >= 680.dp
            AnimatedContent(
              targetState = route,
              transitionSpec = { fadeIn() togetherWith fadeOut() },
              label = "root_route_transition",
            ) { activeRoute ->
            when (activeRoute) {
              AppRootRoute.AUTH -> {
                TelefamAuthScreen(
                  uiState = uiState,
                  onEmailChanged = viewModel::onEmailChanged,
                  onSendOtp = viewModel::sendOtp,
                  onOtpChanged = viewModel::onOtpChanged,
                  onVerifyOtp = viewModel::verifyOtp,
                  onResendOtp = viewModel::resendOtp,
                  onChangeEmail = viewModel::changeEmail,
                  onGoogleTokenSuccess = viewModel::onGoogleSignInSuccess,
                  onGoogleTokenError = viewModel::onGoogleSignInError,
                  onToggleTheme = viewModel::cycleThemeMode,
                  onPrepareTryAnotherWay = viewModel::prepareTryAnotherWayGoogleSignIn,
                )
              }

              AppRootRoute.PROFILE_SETUP -> {
                TelefamProfileSetupScreen(
                  formState = uiState.profileForm,
                  themeMode = uiState.themeMode,
                  onToggleTheme = viewModel::cycleThemeMode,
                  onBackSignOut = viewModel::signOut,
                  onPhotoPickedForCrop = viewModel::onPhotoPickedForCrop,
                  onDismissCrop = viewModel::dismissPhotoCropper,
                  onConfirmCrop = viewModel::confirmCroppedPhoto,
                  onFullNameChanged = viewModel::onProfileFullNameChanged,
                  onUsernameChanged = viewModel::onProfileUsernameChanged,
                  onCountrySelected = viewModel::onProfileCountrySelected,
                  onPhoneDigitsChanged = viewModel::onProfilePhoneDigitsChanged,
                  onGenderSelected = viewModel::onProfileGenderSelected,
                  onDobSelected = viewModel::onProfileDobSelected,
                  onLocationChanged = viewModel::onProfileLocationChanged,
                  onBioChanged = viewModel::onProfileBioChanged,
                  onContinue = viewModel::submitProfileSetup,
                )
              }

              AppRootRoute.HOME -> {
                TelefamHomeScreen(
                  uiState = uiState,
                  onSelectTab = viewModel::selectHomeTab,
                  onTriggerComingSoon = viewModel::triggerComingSoon,
                  onDismissComingSoon = viewModel::dismissComingSoon,
                  onToggleTheme = viewModel::cycleThemeMode,
                  onContactsSearchChanged = viewModel::onContactsSearchChanged,
                  onContactsFilterSelected = viewModel::onContactsFilterSelected,
                  onToggleContactFollow = viewModel::onToggleContactFollow,
                  onLoadNextContactsPage = viewModel::loadNextContactsPage,
                  onContactsScrollVisibilityChanged = viewModel::onContactsScrollVisibilityChanged,
                  onDismissContactsToast = viewModel::dismissContactsToast,
                  onSelectFeedTab = viewModel::selectFeedTab,
                  onOpenFeedSearch = { viewModel.openFeedSearch("") },
                  onLoadNextFeedPage = { viewModel.loadNextFeedPage() },
                  onRefreshFeeds = { viewModel.refreshFeeds() },
                  onToggleFeedAuthorFollow = viewModel::onToggleFeedAuthorFollow,
                  onToggleFeedLike = viewModel::onToggleFeedLike,
                  onDoubleTapFeedLike = viewModel::onDoubleTapFeedLike,
                  onToggleFeedFavourite = viewModel::onToggleFeedFavourite,
                  onShareFeedPost = viewModel::onShareFeedPost,
                  onToggleFeedRepost = viewModel::onToggleFeedRepost,
                  onDownloadFeedVideo = { item -> viewModel.onDownloadFeedVideo(this@TelefamMainActivity, item) },
                  onNotInterestedFeedPost = viewModel::onNotInterestedFeedPost,
                  onReportFeedPost = { postId, reason -> viewModel.onReportFeedPost(postId, reason) },
                  onBlockFeedAuthor = viewModel::onBlockFeedAuthor,
                  onMuteFeedAuthor = viewModel::muteUserAccount,
                  onEditOwnerFeedPost = viewModel::openPostForEdit,
                  onChangeFeedPostPrivacy = viewModel::onChangeFeedPostPrivacy,
                  onDeleteOwnerFeedPost = viewModel::onDeleteOwnerFeedPost,
                  onTapFeedHashtag = viewModel::openPostWithHashtag,
                  onTapFeedAudio = viewModel::openPostWithSong,
                  onRecordFeedWatchProgress = viewModel::onRecordFeedWatchProgress,
                  onShowFeedToast = viewModel::showFeedToast,
                  onDismissFeedToast = viewModel::dismissFeedToast,
                  onOpenUserProfile = viewModel::openUserProfile,
                  onToggleProfileFollow = viewModel::toggleFollowOnProfile,
                  onOpenProfileConnections = viewModel::openProfileConnections,
                  onDismissProfileConnections = viewModel::dismissProfileConnections,
                  onLoadMoreProfileConnections = viewModel::loadMoreProfileConnections,
                  onToggleProfileConnectionFollow = viewModel::toggleFollowInConnections,
                  onSelectOwnerProfileTab = viewModel::selectOwnerProfileTab,
                  onOpenEditProfile = viewModel::openEditProfileModal,
                  onOpenProfileBackgrounds = viewModel::openOwnerBackgroundsModal,
                  onOpenProfileSettings = viewModel::openOwnerSettingsModal,
                  onMenuClick = { viewModel.triggerComingSoon("Menu", "profile_menu_tapped") },
                  onSelectProfileFilter = viewModel::selectProfilePostFilter,
                  onSelectProfileSort = viewModel::selectProfileSortOrder,
                  onOpenProfilePostViewer = viewModel::openProfilePostViewer,
                  onBlockProfileUser = viewModel::blockUserFromProfile,
                  onUnblockProfileUser = viewModel::unblockUserFromProfile,
                  onReportProfileUser = viewModel::reportUserFromProfile,
                  onShowProfileToast = viewModel::showProfileToast,
                  onDismissProfileToast = viewModel::dismissProfileToast,
                  onSignOut = viewModel::signOut,
                  onOpenChatPrivacy = viewModel::openChatPrivacy,
                  onOpenHomeChatSearch = viewModel.chatController::openHomeChatSearch,
                  onOpenChatConversation = { conv ->
                    viewModel.chatController.openConversation(conv.conversationId, conv.peerUserId)
                  },
                  onEnterChatMultiSelect = viewModel.chatController::enterConversationMultiSelect,
                  onToggleChatSelection = viewModel.chatController::toggleConversationSelection,
                  onSelectAllChats = viewModel.chatController::selectAllConversations,
                  onExitChatMultiSelect = viewModel.chatController::exitConversationMultiSelect,
                  onBatchChatAction = viewModel.chatController::performBatchConversationAction,
                  onUndoChatBatch = viewModel.chatController::undoLastBatchAction,
                  onDismissChatBatchUndo = viewModel.chatController::dismissUndoBatch,
                  onSavedLinksSearchQueryChanged = viewModel::onSavedLinksSearchQueryChanged,
                  onOpenSavedLink = { saved ->
                    viewModel.openInAppBrowser(saved.normalizedUrl, saved.source, saved.title)
                  },
                  onDeleteSavedLink = viewModel::deleteSavedLink,
                  onOpenNotifications = viewModel::openNotificationsSheet,
                  embeddedSplitChatContent = {
                    val splitConv = uiState.chatState.activeConversation
                    if (splitConv != null) {
                      TelefamOneOnOneChatScreen(
                        conversation = splitConv,
                        chatState = uiState.chatState,
                        chatPrivacy = uiState.chatPrivacy,
                        currentUserId = uiState.userId,
                        onBack = viewModel.chatController::closeActiveConversation,
                        onOpenPeerProfile = { peerUid -> viewModel.openUserProfile(peerUid) },
                        onComposerTextChanged = viewModel.chatController::onComposerTextChanged,
                        onSendTextOrEdit = viewModel.chatController::sendCurrentTextOrSaveEdit,
                        onSendGreetingSticker = viewModel.chatController::sendGreetingSticker,
                        onStartEditMessage = viewModel.chatController::startEditingMessage,
                        onCancelEditMessage = viewModel.chatController::cancelEditingMessage,
                        onUnsendMessage = viewModel.chatController::unsendMessageWithSparkles,
                        onDeleteMessageForSelf = viewModel.chatController::deleteMessageForSelf,
                        onStartForwardMessage = viewModel.chatController::startForwardMessage,
                        onDismissForwardMessage = viewModel.chatController::dismissForwardMessage,
                        onConfirmForwardMessage = viewModel.chatController::confirmForwardMessage,
                        onStartVoiceRecording = viewModel.chatController::startVoiceRecording,
                        onStopVoiceRecording = viewModel.chatController::stopVoiceRecordingForPreview,
                        onDiscardVoiceRecording = viewModel.chatController::discardRecordedVoiceNote,
                        onToggleVoicePlayback = viewModel.chatController::toggleVoicePlayback,
                        onToggleVoicePreviewViewOnce = viewModel.chatController::setVoicePreviewViewOnce,
                        onSelectVoicePreviewTtl = viewModel.chatController::setVoicePreviewTtlSec,
                        onSendVoiceNote = viewModel.chatController::sendRecordedVoiceNote,
                        onOpenViewOnceVoice = viewModel.chatController::openViewOnceVoiceMessage,
                        onBurnViewOnceVoice = viewModel.chatController::closeAndBurnActiveViewOnce,
                        onOpenGiflyPicker = { viewModel.chatController.openGiflyPicker("stickers") },
                        onCloseGiflyPicker = viewModel.chatController::closeGiflyPicker,
                        onSelectGiflyTab = viewModel.chatController::selectGiflyTab,
                        onGiflyQueryChanged = viewModel.chatController::onGiflyQueryChanged,
                        onLoadMoreGifly = viewModel.chatController::loadNextGiflyPage,
                        onPreviewGiflyItem = viewModel.chatController::previewGiflyItem,
                        onSendPreviewGifly = viewModel.chatController::sendPreviewedGiflyItem,
                        onToggleMuteConversation = viewModel.chatController::toggleMuteActiveConversation,
                        onBlockPeer = viewModel.chatController::blockPeerFromActiveChat,
                        onReportPeer = viewModel.chatController::reportPeerFromActiveChat,
                        onOpenSharedMediaTab = viewModel.chatController::openSharedMediaTab,
                        onTriggerComingSoon = viewModel::triggerComingSoon,
                        onDismissToast = viewModel.chatController::dismissChatToast,
                        onStartAudioCall = {
                          ensureCallPermissionsAndRun(TelefamCallType.AUDIO) {
                            viewModel.callController.startCall(
                              peerUserId = splitConv.peerUserId,
                              peerName = splitConv.peerFullName,
                              peerUsername = splitConv.peerUsername,
                              peerAvatarUrl = splitConv.peerAvatarUrl,
                              conversationId = splitConv.conversationId,
                              callType = TelefamCallType.AUDIO,
                            )
                          }
                        },
                        onStartVideoCall = {
                          ensureCallPermissionsAndRun(TelefamCallType.VIDEO) {
                            viewModel.callController.startCall(
                              peerUserId = splitConv.peerUserId,
                              peerName = splitConv.peerFullName,
                              peerUsername = splitConv.peerUsername,
                              peerAvatarUrl = splitConv.peerAvatarUrl,
                              conversationId = splitConv.conversationId,
                              callType = TelefamCallType.VIDEO,
                            )
                          }
                        },
                        onCallBackFromLog = { cType ->
                          ensureCallPermissionsAndRun(cType) {
                            viewModel.callController.startCall(
                              peerUserId = splitConv.peerUserId,
                              peerName = splitConv.peerFullName,
                              peerUsername = splitConv.peerUsername,
                              peerAvatarUrl = splitConv.peerAvatarUrl,
                              conversationId = splitConv.conversationId,
                              callType = cType,
                            )
                          }
                        },
                        showEmbeddedSplitHeader = true,
                      )
                    }
                  },
                )
              }
            }
          }

          // Compact Handheld Full-Screen 1-on-1 E2EE Chat Overlay (Wide screens show it split-screen inside Home)
          AnimatedVisibility(
            visible = route == AppRootRoute.HOME && !isWideSplitHost && uiState.chatState.activeConversation != null,
            enter = slideInVertically(initialOffsetY = { it / 6 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 6 }) + fadeOut(),
          ) {
            val activeConv = uiState.chatState.activeConversation
            if (activeConv != null) {
              TelefamOneOnOneChatScreen(
                conversation = activeConv,
                chatState = uiState.chatState,
                chatPrivacy = uiState.chatPrivacy,
                currentUserId = uiState.userId,
                onBack = viewModel.chatController::closeActiveConversation,
                onOpenPeerProfile = { peerUid -> viewModel.openUserProfile(peerUid) },
                onComposerTextChanged = viewModel.chatController::onComposerTextChanged,
                onSendTextOrEdit = viewModel.chatController::sendCurrentTextOrSaveEdit,
                onSendGreetingSticker = viewModel.chatController::sendGreetingSticker,
                onStartEditMessage = viewModel.chatController::startEditingMessage,
                onCancelEditMessage = viewModel.chatController::cancelEditingMessage,
                onUnsendMessage = viewModel.chatController::unsendMessageWithSparkles,
                onDeleteMessageForSelf = viewModel.chatController::deleteMessageForSelf,
                onStartForwardMessage = viewModel.chatController::startForwardMessage,
                onDismissForwardMessage = viewModel.chatController::dismissForwardMessage,
                onConfirmForwardMessage = viewModel.chatController::confirmForwardMessage,
                onStartVoiceRecording = viewModel.chatController::startVoiceRecording,
                onStopVoiceRecording = viewModel.chatController::stopVoiceRecordingForPreview,
                onDiscardVoiceRecording = viewModel.chatController::discardRecordedVoiceNote,
                onToggleVoicePlayback = viewModel.chatController::toggleVoicePlayback,
                onToggleVoicePreviewViewOnce = viewModel.chatController::setVoicePreviewViewOnce,
                onSelectVoicePreviewTtl = viewModel.chatController::setVoicePreviewTtlSec,
                onSendVoiceNote = viewModel.chatController::sendRecordedVoiceNote,
                onOpenViewOnceVoice = viewModel.chatController::openViewOnceVoiceMessage,
                onBurnViewOnceVoice = viewModel.chatController::closeAndBurnActiveViewOnce,
                onOpenGiflyPicker = { viewModel.chatController.openGiflyPicker("stickers") },
                onCloseGiflyPicker = viewModel.chatController::closeGiflyPicker,
                onSelectGiflyTab = viewModel.chatController::selectGiflyTab,
                onGiflyQueryChanged = viewModel.chatController::onGiflyQueryChanged,
                onLoadMoreGifly = viewModel.chatController::loadNextGiflyPage,
                onPreviewGiflyItem = viewModel.chatController::previewGiflyItem,
                onSendPreviewGifly = viewModel.chatController::sendPreviewedGiflyItem,
                onToggleMuteConversation = viewModel.chatController::toggleMuteActiveConversation,
                onBlockPeer = viewModel.chatController::blockPeerFromActiveChat,
                onReportPeer = viewModel.chatController::reportPeerFromActiveChat,
                onOpenSharedMediaTab = viewModel.chatController::openSharedMediaTab,
                onTriggerComingSoon = viewModel::triggerComingSoon,
                onDismissToast = viewModel.chatController::dismissChatToast,
                onStartAudioCall = {
                  ensureCallPermissionsAndRun(TelefamCallType.AUDIO) {
                    viewModel.callController.startCall(
                      peerUserId = activeConv.peerUserId,
                      peerName = activeConv.peerFullName,
                      peerUsername = activeConv.peerUsername,
                      peerAvatarUrl = activeConv.peerAvatarUrl,
                      conversationId = activeConv.conversationId,
                      callType = TelefamCallType.AUDIO,
                    )
                  }
                },
                onStartVideoCall = {
                  ensureCallPermissionsAndRun(TelefamCallType.VIDEO) {
                    viewModel.callController.startCall(
                      peerUserId = activeConv.peerUserId,
                      peerName = activeConv.peerFullName,
                      peerUsername = activeConv.peerUsername,
                      peerAvatarUrl = activeConv.peerAvatarUrl,
                      conversationId = activeConv.conversationId,
                      callType = TelefamCallType.VIDEO,
                    )
                  }
                },
                onCallBackFromLog = { cType ->
                  ensureCallPermissionsAndRun(cType) {
                    viewModel.callController.startCall(
                      peerUserId = activeConv.peerUserId,
                      peerName = activeConv.peerFullName,
                      peerUsername = activeConv.peerUsername,
                      peerAvatarUrl = activeConv.peerAvatarUrl,
                      conversationId = activeConv.conversationId,
                      callType = cType,
                    )
                  }
                },
                showEmbeddedSplitHeader = false,
              )
            }
          }

          // Home Global Search Overlay (Conversations + Local Decrypted Messages + People)
          AnimatedVisibility(
            visible = route == AppRootRoute.HOME && uiState.chatState.isHomeSearchOpen,
            enter = slideInVertically(initialOffsetY = { it / 8 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 8 }) + fadeOut(),
          ) {
            HomeGlobalSearchOverlay(
              chatState = uiState.chatState,
              onQueryChanged = viewModel.chatController::onHomeChatSearchQueryChanged,
              onOpenConversation = { conv ->
                viewModel.chatController.closeHomeChatSearch()
                viewModel.selectHomeTab(HomeTab.CHATS)
                viewModel.chatController.openConversation(conv.conversationId, conv.peerUserId)
              },
              onOpenPersonChat = { person ->
                viewModel.chatController.closeHomeChatSearch()
                viewModel.selectHomeTab(HomeTab.CHATS)
                viewModel.chatController.openDirectChat(
                  peerUserId = person.userId,
                  peerFullName = person.fullName,
                  peerUsername = person.username,
                  peerAvatarUrl = person.avatarUrl,
                )
              },
              onClose = viewModel.chatController::closeHomeChatSearch,
            )
          }

          AnimatedVisibility(
            visible = route == AppRootRoute.HOME && uiState.feedsState.isSearchOpen,
            enter = slideInVertically(initialOffsetY = { it / 6 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 6 }) + fadeOut(),
          ) {
            TelefamSearchScreen(
              feedsState = uiState.feedsState,
              networkTier = uiState.effectiveNetworkTierForFeeds,
              isHostActive = uiState.activeUserProfile == null && !uiState.postComposer.isOpen && uiState.activeSettingsDestination == null,
              onQueryChanged = viewModel::onFeedSearchQueryChanged,
              onShortcutSelected = viewModel::onFeedSearchShortcutSelected,
              onOpenCardAtIndex = viewModel::openSearchResultViewer,
              onCloseViewer = viewModel::closeSearchResultViewer,
              onBack = viewModel::closeFeedSearch,
              onToggleFollowAuthor = viewModel::onToggleFeedAuthorFollow,
              onToggleLike = viewModel::onToggleFeedLike,
              onDoubleTapLike = viewModel::onDoubleTapFeedLike,
              onCommentClick = { viewModel.triggerComingSoon("Comments", "feed_comments_tapped") },
              onToggleFavourite = viewModel::onToggleFeedFavourite,
              onShareRecorded = viewModel::onShareFeedPost,
              onToggleRepost = viewModel::onToggleFeedRepost,
              onDownloadVideo = { item -> viewModel.onDownloadFeedVideo(this@TelefamMainActivity, item) },
              onNotInterested = viewModel::onNotInterestedFeedPost,
              onReportPost = { postId, reason -> viewModel.onReportFeedPost(postId, reason) },
              onBlockAuthor = viewModel::onBlockFeedAuthor,
              onMuteAuthor = viewModel::muteUserAccount,
              onEditOwnerPost = viewModel::openPostForEdit,
              onChangePostPrivacy = viewModel::onChangeFeedPostPrivacy,
              onDeleteOwnerPost = viewModel::onDeleteOwnerFeedPost,
              onTapHashtag = viewModel::openPostWithHashtag,
              onTapAudio = viewModel::openPostWithSong,
              onRecordWatchProgress = viewModel::onRecordFeedWatchProgress,
              onOpenAuthorProfile = { uid ->
                viewModel.closeSearchResultViewer()
                viewModel.openUserProfile(uid)
              },
              autoplayVideos = uiState.settings.contentPreferences.autoplayVideos,
              captionsEnabled = uiState.settings.contentPreferences.captionsEnabled,
              reducedMotion = uiState.settings.contentPreferences.reducedMotion,
              onShowToast = viewModel::showFeedToast,
            )
          }

          AnimatedVisibility(
            visible = route == AppRootRoute.HOME && uiState.activeUserProfile != null,
            enter = slideInVertically(initialOffsetY = { it / 8 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 8 }) + fadeOut(),
          ) {
            val activeProfile = uiState.activeUserProfile
            if (activeProfile != null) {
              LaunchedEffect(uiState.profileToastMessage) {
                val toastMsg = uiState.profileToastMessage
                if (!toastMsg.isNullOrBlank()) {
                  Toast.makeText(this@TelefamMainActivity, toastMsg, Toast.LENGTH_SHORT).show()
                  viewModel.dismissProfileToast()
                }
              }
              TelefamUserProfileScreen(
                profile = activeProfile,
                isEmbeddedOwnProfileTab = false,
                bottomChromePadding = 0.dp,
                onBack = viewModel::closeTopUserProfile,
                onToggleFollow = { viewModel.toggleFollowOnProfile(activeProfile.userId) },
                onMessageClick = {
                  viewModel.closeTopUserProfile()
                  viewModel.selectHomeTab(HomeTab.CHATS)
                  viewModel.chatController.openDirectChat(
                    peerUserId = activeProfile.userId,
                    peerFullName = activeProfile.fullName,
                    peerUsername = activeProfile.username,
                    peerAvatarUrl = activeProfile.avatarUrl.orEmpty(),
                  )
                },
                onSubscribeClick = { viewModel.showProfileToast("Subscribe — Coming soon") },
                onOpenConnections = { isFollowers -> viewModel.openProfileConnections(activeProfile.userId, isFollowers) },
                onDismissConnections = { viewModel.dismissProfileConnections(activeProfile.userId) },
                onLoadMoreConnections = { viewModel.loadMoreProfileConnections(activeProfile.userId) },
                onToggleConnectionFollow = { targetId -> viewModel.toggleFollowInConnections(activeProfile.userId, targetId) },
                onOpenUserProfile = viewModel::openUserProfile,
                onSelectOwnerTab = { tab -> viewModel.selectOwnerProfileTab(activeProfile.userId, tab) },
                onOpenEditProfile = viewModel::openEditProfileModal,
                onOpenBackgrounds = viewModel::openOwnerBackgroundsModal,
                onOpenSettings = viewModel::openOwnerSettingsModal,
                onOpenMenu = { viewModel.triggerComingSoon("Menu", "profile_menu_tapped") },
                onSelectFilter = { filter -> viewModel.selectProfilePostFilter(activeProfile.userId, filter) },
                onSelectSort = { sort -> viewModel.selectProfileSortOrder(activeProfile.userId, sort) },
                onOpenPostAtIndex = viewModel::openProfilePostViewer,
                onBlockUser = { viewModel.blockUserFromProfile(activeProfile.userId) },
                onUnblockUser = { viewModel.unblockUserFromProfile(activeProfile.userId) },
                onMuteToggle = {
                  if (activeProfile.isMuted) {
                    viewModel.unmuteAccountFromSettings(activeProfile.userId)
                  } else {
                    viewModel.muteUserAccount(activeProfile.userId)
                  }
                },
                onReportUser = { reason, details -> viewModel.reportUserFromProfile(activeProfile.userId, reason, details) },
                onShowToast = viewModel::showProfileToast,
                onToggleTheme = viewModel::cycleThemeMode,
                onSignOut = viewModel::signOut,
                savedLinks = uiState.savedLinks,
                savedLinksSearchQuery = uiState.savedLinksSearchQuery,
                onSavedLinksSearchQueryChanged = viewModel::onSavedLinksSearchQueryChanged,
                onOpenSavedLink = { saved ->
                  viewModel.openInAppBrowser(saved.normalizedUrl, saved.source, saved.title)
                },
                onDeleteSavedLink = viewModel::deleteSavedLink,
              )
            }
          }

          AnimatedVisibility(
            visible = route == AppRootRoute.HOME && uiState.profileViewerItems != null,
            enter = fadeIn(),
            exit = fadeOut(),
          ) {
            val viewerList = uiState.profileViewerItems
            if (!viewerList.isNullOrEmpty()) {
              androidx.activity.compose.BackHandler(enabled = true) {
                viewModel.closeProfilePostViewer()
              }
              ReusableVideoFeedScreen(
                items = viewerList,
                initialIndex = uiState.profileViewerInitialIndex,
                isStandaloneViewer = true,
                isHostActive = !uiState.postComposer.isOpen && uiState.activeSettingsDestination == null,
                networkTier = uiState.effectiveNetworkTierForFeeds,
                streamAccessToken = uiState.feedsState.streamAccessToken,
                streamDeviceFp = uiState.feedsState.streamDeviceFp,
                bottomChromePadding = 0.dp,
                onSearchClick = {
                  viewModel.closeProfilePostViewer()
                  viewModel.openFeedSearch("")
                },
                onBackClick = viewModel::closeProfilePostViewer,
                onToggleFollowAuthor = viewModel::onToggleFeedAuthorFollow,
                onToggleLike = viewModel::onToggleFeedLike,
                onDoubleTapLike = viewModel::onDoubleTapFeedLike,
                onCommentClick = { viewModel.triggerComingSoon("Comments", "feed_comments_tapped") },
                onToggleFavourite = viewModel::onToggleFeedFavourite,
                onShareRecorded = viewModel::onShareFeedPost,
                onToggleRepost = viewModel::onToggleFeedRepost,
                onDownloadVideo = { item -> viewModel.onDownloadFeedVideo(this@TelefamMainActivity, item) },
                onNotInterested = viewModel::onNotInterestedFeedPost,
                onReportPost = { postId, reason -> viewModel.onReportFeedPost(postId, reason) },
                onBlockAuthor = viewModel::onBlockFeedAuthor,
                onMuteAuthor = viewModel::muteUserAccount,
                onEditOwnerPost = { item ->
                  viewModel.closeProfilePostViewer()
                  viewModel.openPostForEdit(item)
                },
                onChangePostPrivacy = viewModel::onChangeFeedPostPrivacy,
                onDeleteOwnerPost = { postId ->
                  viewModel.onDeleteOwnerFeedPost(postId)
                  viewModel.closeProfilePostViewer()
                },
                onTapHashtag = { tag, mode ->
                  viewModel.closeProfilePostViewer()
                  viewModel.openPostWithHashtag(tag, mode)
                },
                onTapAudio = { song, mode ->
                  viewModel.closeProfilePostViewer()
                  viewModel.openPostWithSong(song, mode)
                },
                onRecordWatchProgress = viewModel::onRecordFeedWatchProgress,
                onOpenAuthorProfile = { uid ->
                  viewModel.closeProfilePostViewer()
                  viewModel.openUserProfile(uid)
                },
                autoplayVideos = uiState.settings.contentPreferences.autoplayVideos,
                captionsEnabled = uiState.settings.contentPreferences.captionsEnabled,
                reducedMotion = uiState.settings.contentPreferences.reducedMotion,
                onShowToast = viewModel::showFeedToast,
              )
            }
          }

          AnimatedVisibility(
            visible = route == AppRootRoute.HOME && uiState.postComposer.isOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
          ) {
            TelefamPostScreen(
              state = uiState.postComposer,
              onClose = viewModel::closePostComposer,
              onSelectMode = viewModel::onPostModeSelected,
              onTextChanged = viewModel::onPostTextChanged,
              onCaptionChanged = viewModel::onPostCaptionChanged,
              onSelectTheme = viewModel::onPostThemeSelected,
              onSelectFont = viewModel::onPostFontSelected,
              onPhotosPicked = viewModel::onPostPhotosPicked,
              onRemovePhoto = viewModel::removePostPhoto,
              onVideoPicked = viewModel::onPostVideoPicked,
              onClearVideo = viewModel::clearPostVideo,
              onTrimChanged = viewModel::onVideoTrimChanged,
              onSelectThumbnailFrame = viewModel::onSelectVideoThumbnailFrame,
              onToggleMuteOriginalAudio = viewModel::onToggleMuteOriginalAudio,
              onOpenSongSheet = viewModel::openSongSheet,
              onDismissSongSheet = viewModel::dismissSongSheet,
              onSongQueryChanged = viewModel::onSongSearchChanged,
              onSelectSong = viewModel::selectSongTrack,
              onToggleSongPreview = viewModel::toggleSongPreview,
              onClearSong = viewModel::clearSelectedSong,
              onOpenTagPeopleSheet = viewModel::openTagPeopleSheet,
              onDismissTagPeopleSheet = viewModel::dismissTagPeopleSheet,
              onTagPeopleQueryChanged = viewModel::onTagPeopleSearchChanged,
              onTogglePendingTagPerson = viewModel::togglePendingTaggedPerson,
              onConfirmTaggedPeople = viewModel::confirmTaggedPeople,
              onOpenHashtagsSheet = viewModel::openHashtagsSheet,
              onDismissHashtagsSheet = viewModel::dismissHashtagsSheet,
              onHashtagQueryChanged = viewModel::onHashtagSearchChanged,
              onToggleHashtag = viewModel::toggleHashtagSelection,
              onShare = viewModel::sharePost,
            )
          }

          val activeOwnerProfileForModals = uiState.activeUserProfile?.takeIf { it.isOwner } ?: uiState.ownProfileState

          AnimatedVisibility(
            visible = route == AppRootRoute.HOME && uiState.activeSettingsDestination != null,
            enter = slideInVertically(initialOffsetY = { it / 6 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 6 }) + fadeOut(),
          ) {
            TelefamSettingsScreen(
              uiState = uiState,
              onNavigateBack = viewModel::navigateSettingsBack,
              onOpenDestination = viewModel::navigateToSettings,
              onSearchQueryChanged = viewModel::onSettingsSearchQueryChanged,
              onToggleTheme = viewModel::cycleThemeMode,
              onUpdatePrivacy = viewModel::updatePrivacySettings,
              onUpdateNotifications = viewModel::updateNotificationSettings,
              onUpdateContentPreferences = viewModel::updateContentPreferences,
              onUpdateSyncPreferences = viewModel::updateSyncPreferences,
              onTriggerManualSync = viewModel::triggerManualSyncNow,
              onRefreshSyncStatus = viewModel::refreshOfflineSyncStatus,
              onRestrictedTabSelected = viewModel::onRestrictedAccountsTabSelected,
              onRestrictedQueryChanged = viewModel::onRestrictedAccountsQueryChanged,
              onUnblockAccount = viewModel::unblockAccountFromSettings,
              onUnmuteAccount = viewModel::unmuteAccountFromSettings,
              onSubmitProblemReport = viewModel::submitProblemReport,
              onClearLastCreatedTicket = viewModel::clearLastCreatedTicket,
              onSearchFaq = viewModel::searchFaqs,
              onRefreshTickets = viewModel::refreshSupportTickets,
              onRefreshStatus = viewModel::refreshSystemStatus,
              onSelectLegalDoc = viewModel::selectLegalDocument,
              onCheckAppUpdates = viewModel::checkAppUpdates,
              onSavedLinksSearchQueryChanged = viewModel::onSavedLinksSearchQueryChanged,
              onOpenSavedLink = { saved ->
                viewModel.openInAppBrowser(saved.normalizedUrl, saved.source, saved.title)
              },
              onDeleteSavedLink = viewModel::deleteSavedLink,
              onShowToast = viewModel::showProfileToast,
              onRequestLogout = viewModel::requestLogoutConfirmation,
              onDismissLogoutDialog = viewModel::dismissLogoutConfirmation,
              onToggleLogoutAllDevices = viewModel::setLogoutAllDevicesChoice,
              onConfirmLogout = viewModel::confirmSignOut,
            )
          }

          AnimatedVisibility(
            visible = route == AppRootRoute.HOME && uiState.activeChatPrivacyRoute != null,
            enter = slideInVertically(initialOffsetY = { it / 6 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 6 }) + fadeOut(),
          ) {
            val chatRoute = uiState.activeChatPrivacyRoute
            if (chatRoute != null) {
              val ownerName = uiState.ownProfileState?.fullName?.trim()?.takeIf { it.isNotBlank() }
                ?: uiState.profileForm.fullName.trim().takeIf { it.isNotBlank() }
                ?: uiState.ownProfileState?.username?.trim()?.takeIf { it.isNotBlank() }?.let { "@$it" }
                ?: uiState.profileForm.username.trim().takeIf { it.isNotBlank() }?.let { "@$it" }
                ?: "Telefam"
              TelefamChatPrivacyHostScreen(
                route = chatRoute,
                state = uiState.chatPrivacy,
                previewSenderName = ownerName,
                onNavigateBack = viewModel::navigateChatPrivacyBack,
                onUpdatePrivacy = viewModel::updateChatPrivacy,
                onOpenChatsTheme = viewModel::openChatsThemePicker,
                onOpenBubbleColors = viewModel::openBubbleColorPicker,
                onSelectTheme = viewModel::selectChatTheme,
                onSelectBubbleColor = viewModel::selectBubbleColor,
              )
            }
          }

          if (route == AppRootRoute.HOME && uiState.showBackgroundsModal) {
            EditBackgroundsBottomSheet(
              backgrounds = activeOwnerProfileForModals?.backgrounds ?: emptyList(),
              isUploading = uiState.isUploadingBackground,
              errorMessage = uiState.backgroundError,
              onAddUris = viewModel::uploadOwnerBackgrounds,
              onDeleteBackground = viewModel::deleteOwnerBackground,
              onReorderBackgrounds = viewModel::reorderOwnerBackgrounds,
              onDismiss = viewModel::dismissOwnerBackgroundsModal,
            )
          }

          if (route == AppRootRoute.HOME && uiState.showEditProfileModal && activeOwnerProfileForModals != null) {
            EditProfileModalSheet(
              profile = activeOwnerProfileForModals,
              formState = uiState.profileForm,
              cropBitmap = uiState.editProfileCropBitmap,
              isSaving = uiState.isSavingEditedProfile,
              errorMessage = uiState.editProfileError,
              onPhotoPicked = viewModel::onEditProfilePhotoPicked,
              onDismissCrop = viewModel::dismissEditProfileCropper,
              onConfirmCrop = viewModel::confirmEditProfileCroppedPhoto,
              onSaveProfile = viewModel::saveEditedProfile,
              onDismiss = viewModel::dismissEditProfileModal,
            )
          }

          AnimatedVisibility(
            visible = route == AppRootRoute.HOME && uiState.incomingLoginRequest != null && uiState.loginApprovedOverlay == null,
            enter = fadeIn(),
            exit = fadeOut(),
          ) {
            val incoming = uiState.incomingLoginRequest
            if (incoming != null) {
              IncomingLoginRequestFullScreenOverlay(
                request = incoming,
                isResponding = uiState.isRespondingToLoginRequest,
                actionError = uiState.loginRequestActionError,
                onDeny = { viewModel.respondToIncomingLoginRequest(accept = false) },
                onAccept = { viewModel.respondToIncomingLoginRequest(accept = true) },
              )
            }
          }

          AnimatedVisibility(
            visible = route == AppRootRoute.HOME && uiState.loginApprovedOverlay != null,
            enter = fadeIn(),
            exit = fadeOut(),
          ) {
            val approved = uiState.loginApprovedOverlay
            if (approved != null) {
              LoginApprovedFullScreenOverlay(
                approved = approved,
                onDone = viewModel::dismissLoginApprovedOverlay,
              )
            }
          }

          // Push & Social Notification Inbox Sheet
          AnimatedVisibility(
            visible = route == AppRootRoute.HOME && uiState.showNotificationsSheet,
            enter = slideInVertically(initialOffsetY = { it / 6 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 6 }) + fadeOut(),
          ) {
            TelefamNotificationsSheet(
              notifications = uiState.notifications,
              onClose = viewModel::closeNotificationsSheet,
              onMarkAllRead = viewModel::markAllNotificationsRead,
              onNotificationClick = viewModel::openNotificationItem,
            )
          }

          // In-App Browser overlay
          AnimatedVisibility(
            visible = route == AppRootRoute.HOME && uiState.inAppBrowserState != null,
            enter = slideInVertically(initialOffsetY = { it / 6 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 6 }) + fadeOut(),
          ) {
            val browser = uiState.inAppBrowserState
            if (browser != null) {
              TelefamInAppBrowserScreen(
                initialUrl = browser.url,
                titleHint = browser.titleHint,
                isSaved = browser.url in savedLinkUrls,
                savedUrls = savedLinkUrls,
                onToggleSave = { currentUrl, currentTitle ->
                  viewModel.toggleBookmarkLink(
                    rawUrl = currentUrl,
                    source = browser.source,
                    titleHint = currentTitle,
                  )
                },
                onClose = viewModel::closeInAppBrowser,
              )
            }
          }

          activePhoneAction?.let { phone ->
            SmartPhoneActionSheet(
              phone = phone,
              onDismiss = { activePhoneAction = null },
              onCopied = {
                viewModel.showSmartActionToast(context.getString(R.string.smart_copied))
              },
            )
          }

          activeEmailAction?.let { email ->
            SmartEmailActionSheet(
              email = email,
              onDismiss = { activeEmailAction = null },
              onCopied = {
                viewModel.showSmartActionToast(context.getString(R.string.smart_copied))
              },
            )
          }

          AnimatedVisibility(
            visible = !uiState.smartActionToast.isNullOrBlank(),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .navigationBarsPadding()
              .padding(bottom = 86.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(20.dp),
              color = MaterialTheme.colorScheme.inverseSurface,
              shadowElevation = 6.dp,
              modifier = Modifier.testTag("smart_action_toast"),
            ) {
              Text(
                text = uiState.smartActionToast.orEmpty(),
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
              )
            }
          }

          if (route == AppRootRoute.HOME) {
            TelefamCallOverlayHost(
              callState = callUiState,
              eglBaseContext = viewModel.callController.eglBaseContext,
              onAttachLocalRenderer = viewModel.callController::attachLocalRenderer,
              onAttachRemoteRenderer = viewModel.callController::attachRemoteRenderer,
              onAcceptIncoming = {
                ensureCallPermissionsAndRun(callUiState.callType) {
                  viewModel.callController.acceptIncomingCall()
                }
              },
              onDeclineIncoming = viewModel.callController::declineIncomingCall,
              onHangUp = { viewModel.callController.hangUp("hangup") },
              onToggleMic = viewModel.callController::toggleMic,
              onToggleSpeaker = viewModel.callController::toggleSpeaker,
              onToggleCamera = viewModel.callController::toggleCamera,
              onFlipCamera = viewModel.callController::flipCamera,
              onSelectFilter = viewModel.callController::setVideoFilter,
              onSelectQuality = viewModel.callController::setQualityTier,
              onRequestScreenShare = {
                val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
                if (mgr != null) {
                  screenShareLauncher.launch(mgr.createScreenCaptureIntent())
                }
              },
              onStopScreenShare = viewModel.callController::stopScreenShare,
              onRequestSwitchToVideo = {
                ensureCallPermissionsAndRun(TelefamCallType.VIDEO) {
                  viewModel.callController.requestSwitchToVideo()
                }
              },
              onRespondSwitchToVideo = { accept ->
                if (accept) {
                  ensureCallPermissionsAndRun(TelefamCallType.VIDEO) {
                    viewModel.callController.respondSwitchToVideo(true)
                  }
                } else {
                  viewModel.callController.respondSwitchToVideo(false)
                }
              },
              onMinimizeToChat = {
                viewModel.callController.minimizeToChat { convId, peerId ->
                  viewModel.selectHomeTab(HomeTab.CHATS)
                  if (convId.isNotBlank()) {
                    viewModel.chatController.openConversation(convId, peerId)
                  } else if (peerId.isNotBlank()) {
                    viewModel.chatController.openDirectChat(
                      peerUserId = peerId,
                      peerFullName = callUiState.peerName,
                      peerUsername = callUiState.peerUsername,
                      peerAvatarUrl = callUiState.peerAvatarUrl,
                    )
                  }
                }
              },
              onExpandFromMinimized = viewModel.callController::expandFromMinimized,
              onDismissNotice = viewModel.callController::dismissEndedNotice,
            )
          }

          TelefamAnimatedSplashOverlay(visible = showSplash)
        }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleVerificationIntent(intent)
  }

  private fun handleVerificationIntent(intent: Intent?) {
    if (intent == null) return
    val data: Uri? = intent.data
    val token = data?.getQueryParameter("token")
    if (!token.isNullOrBlank()) {
      viewModel.handleVerificationDeepLink(token)
    }
    val notifRoute = intent.getStringExtra(TelefamNotificationManager.EXTRA_NOTIF_ROUTE).orEmpty()
    if (notifRoute == "incoming_call") {
      val callId = intent.getStringExtra(TelefamNotificationManager.EXTRA_CALL_ID).orEmpty()
      if (callId.isNotBlank()) {
        val payload = IncomingCallPushPayload(
          callId = callId,
          conversationId = intent.getStringExtra(TelefamNotificationManager.EXTRA_CONVERSATION_ID).orEmpty(),
          callType = TelefamCallType.fromWire(intent.getStringExtra(TelefamNotificationManager.EXTRA_CALL_TYPE)),
          callerUserId = intent.getStringExtra(TelefamNotificationManager.EXTRA_PEER_USER_ID).orEmpty(),
          callerName = intent.getStringExtra(TelefamNotificationManager.EXTRA_CALLER_NAME).orEmpty(),
          callerUsername = intent.getStringExtra(TelefamNotificationManager.EXTRA_CALLER_USERNAME).orEmpty(),
          callerAvatarUrl = intent.getStringExtra(TelefamNotificationManager.EXTRA_CALLER_AVATAR_URL).orEmpty(),
          expiresAtMs = intent.getLongExtra(TelefamNotificationManager.EXTRA_CALL_EXPIRES_AT_MS, System.currentTimeMillis() + 25_000L),
          autoAccept = intent.getBooleanExtra(TelefamNotificationManager.EXTRA_CALL_AUTO_ACCEPT, false),
        )
        viewModel.handleIncomingCallIntent(payload)
      }
      intent.removeExtra(TelefamNotificationManager.EXTRA_NOTIF_ROUTE)
    } else if (notifRoute.isNotBlank()) {
      viewModel.handleNotificationDeepLink(
        route = notifRoute,
        notificationId = intent.getStringExtra(TelefamNotificationManager.EXTRA_NOTIFICATION_ID).orEmpty(),
        conversationId = intent.getStringExtra(TelefamNotificationManager.EXTRA_CONVERSATION_ID).orEmpty(),
        peerUserId = intent.getStringExtra(TelefamNotificationManager.EXTRA_PEER_USER_ID).orEmpty(),
        targetUserId = intent.getStringExtra(TelefamNotificationManager.EXTRA_TARGET_USER_ID).orEmpty(),
        postId = intent.getStringExtra(TelefamNotificationManager.EXTRA_POST_ID).orEmpty(),
      )
      intent.removeExtra(TelefamNotificationManager.EXTRA_NOTIF_ROUTE)
    }
  }
}
