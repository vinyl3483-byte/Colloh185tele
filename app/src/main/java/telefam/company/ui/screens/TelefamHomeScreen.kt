package telefam.company.ui.screens

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telefam.company.app.R
import telefam.company.core.ContactFilterTab
import telefam.company.core.FeedTab
import telefam.company.core.OwnerProfileTab
import telefam.company.core.PostMode
import telefam.company.core.PostPrivacy
import telefam.company.core.PostSongTrack
import telefam.company.core.ProfilePostFilter
import telefam.company.core.ProfilePostSort
import telefam.company.ui.components.CompactThemeToggleChip
import telefam.company.ui.theme.LocalTelefamDarkTheme
import telefam.company.ui.theme.TelefamBlushWave
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamDarkBlush
import telefam.company.ui.theme.TelefamPureWhite
import telefam.company.ui.viewmodel.FeedUiItem
import telefam.company.ui.viewmodel.HomeTab
import telefam.company.ui.viewmodel.TelefamUiState
import telefam.company.ui.viewmodel.UserProfileScreenState
import java.io.File

@Composable
fun TelefamHomeScreen(
  uiState: TelefamUiState,
  onSelectTab: (HomeTab) -> Unit,
  onTriggerComingSoon: (featureLabel: String, actionKey: String) -> Unit,
  onDismissComingSoon: () -> Unit,
  onToggleTheme: () -> Unit,
  onContactsSearchChanged: (String) -> Unit = {},
  onContactsFilterSelected: (ContactFilterTab) -> Unit = {},
  onToggleContactFollow: (String) -> Unit = {},
  onLoadNextContactsPage: () -> Unit = {},
  onContactsScrollVisibilityChanged: (Boolean) -> Unit = {},
  onDismissContactsToast: () -> Unit = {},
  onSelectFeedTab: (FeedTab) -> Unit = {},
  onOpenFeedSearch: () -> Unit = {},
  onLoadNextFeedPage: () -> Unit = {},
  onRefreshFeeds: () -> Unit = {},
  onToggleFeedAuthorFollow: (String, Boolean) -> Unit = { _, _ -> },
  onToggleFeedLike: (String, Boolean) -> Unit = { _, _ -> },
  onDoubleTapFeedLike: (String) -> Unit = {},
  onToggleFeedFavourite: (String, Boolean) -> Unit = { _, _ -> },
  onShareFeedPost: (String) -> Unit = {},
  onToggleFeedRepost: (String, Boolean) -> Unit = { _, _ -> },
  onDownloadFeedVideo: (FeedUiItem) -> Unit = {},
  onNotInterestedFeedPost: (String) -> Unit = {},
  onReportFeedPost: (String, String) -> Unit = { _, _ -> },
  onBlockFeedAuthor: (String, String) -> Unit = { _, _ -> },
  onMuteFeedAuthor: (String) -> Unit = {},
  onEditOwnerFeedPost: (FeedUiItem) -> Unit = {},
  onChangeFeedPostPrivacy: (String, PostPrivacy) -> Unit = { _, _ -> },
  onDeleteOwnerFeedPost: (String) -> Unit = {},
  onTapFeedHashtag: (String, PostMode) -> Unit = { _, _ -> },
  onTapFeedAudio: (PostSongTrack, PostMode) -> Unit = { _, _ -> },
  onRecordFeedWatchProgress: (String, Long, Long) -> Unit = { _, _, _ -> },
  onShowFeedToast: (String) -> Unit = {},
  onDismissFeedToast: () -> Unit = {},
  onOpenUserProfile: (String) -> Unit = {},
  onToggleProfileFollow: (String) -> Unit = {},
  onOpenProfileConnections: (String, Boolean) -> Unit = { _, _ -> },
  onDismissProfileConnections: (String) -> Unit = {},
  onLoadMoreProfileConnections: (String) -> Unit = {},
  onToggleProfileConnectionFollow: (String, String) -> Unit = { _, _ -> },
  onSelectOwnerProfileTab: (String, OwnerProfileTab) -> Unit = { _, _ -> },
  onOpenEditProfile: () -> Unit = {},
  onOpenProfileBackgrounds: () -> Unit = {},
  onOpenProfileSettings: () -> Unit = {},
  onMenuClick: () -> Unit = {},
  onOwnerSubscribersClick: () -> Unit = {},
  onSelectProfileFilter: (String, ProfilePostFilter) -> Unit = { _, _ -> },
  onSelectProfileSort: (String, ProfilePostSort) -> Unit = { _, _ -> },
  onOpenProfilePostViewer: (List<FeedUiItem>, Int) -> Unit = { _, _ -> },
  onBlockProfileUser: (String) -> Unit = {},
  onUnblockProfileUser: (String) -> Unit = {},
  onReportProfileUser: (String, String, String) -> Unit = { _, _, _ -> },
  onShowProfileToast: (String) -> Unit = {},
  onDismissProfileToast: () -> Unit = {},
  onSignOut: () -> Unit = {},
  onOpenChatPrivacy: () -> Unit = {},
  onOpenHomeChatSearch: () -> Unit = {},
  onOpenChatConversation: (telefam.company.data.local.ChatConversationEntity) -> Unit = {},
  onEnterChatMultiSelect: (String) -> Unit = {},
  onToggleChatSelection: (String) -> Unit = {},
  onSelectAllChats: () -> Unit = {},
  onExitChatMultiSelect: () -> Unit = {},
  onBatchChatAction: (String) -> Unit = {},
  onUndoChatBatch: () -> Unit = {},
  onDismissChatBatchUndo: () -> Unit = {},
  onSavedLinksSearchQueryChanged: (String) -> Unit = {},
  onOpenSavedLink: (telefam.company.data.local.SavedLinkEntity) -> Unit = {},
  onDeleteSavedLink: (String) -> Unit = {},
  onOpenNotifications: () -> Unit = {},
  embeddedSplitChatContent: (@Composable () -> Unit)? = null,
) {
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(uiState.comingSoonMessage) {
    val msg = uiState.comingSoonMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      onDismissComingSoon()
    }
  }

  LaunchedEffect(uiState.contactsState.toastMessage) {
    val toastMsg = uiState.contactsState.toastMessage
    if (!toastMsg.isNullOrBlank()) {
      Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
      onDismissContactsToast()
    }
  }

  LaunchedEffect(uiState.feedsState.toastMessage) {
    val feedToast = uiState.feedsState.toastMessage
    if (!feedToast.isNullOrBlank()) {
      Toast.makeText(context, feedToast, Toast.LENGTH_SHORT).show()
      onDismissFeedToast()
    }
  }

  LaunchedEffect(uiState.profileToastMessage) {
    val profToast = uiState.profileToastMessage
    if (!profToast.isNullOrBlank()) {
      Toast.makeText(context, profToast, Toast.LENGTH_SHORT).show()
      onDismissProfileToast()
    }
  }

  val isDark = LocalTelefamDarkTheme.current
  val blushBg = if (isDark) TelefamDarkBlush else TelefamBlushWave
  val unreadChatCount = remember(uiState.chatState.conversations) {
    uiState.chatState.conversations.filter { !it.isDeletedLocally && !it.isArchived }.sumOf { it.unreadCount }
  }
  val isContactsTab = uiState.activeHomeTab == HomeTab.CONTACTS
  val isFeedsTab = uiState.activeHomeTab == HomeTab.FEEDS
  val isProfileTab = uiState.activeHomeTab == HomeTab.PROFILE

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        when {
          isFeedsTab -> Color(0xFF07080B)
          isProfileTab -> MaterialTheme.colorScheme.background
          else -> TelefamCrimson
        },
      ),
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      if (isContactsTab) {
        ContactsTopHeader(
          searchQuery = uiState.contactsState.searchQuery,
          activeFilter = uiState.contactsState.activeFilter,
          themeMode = uiState.themeMode,
          onSearchChanged = onContactsSearchChanged,
          onFilterSelected = onContactsFilterSelected,
          onToggleTheme = onToggleTheme,
        )
      } else if (!isFeedsTab && !isProfileTab) {
        // Red Top Bar matching Image 2 (pill Search Bar + 3-dots menu)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            modifier = Modifier
              .weight(1f)
              .height(42.dp)
              .clip(RoundedCornerShape(24.dp))
              .clickable { onOpenHomeChatSearch() }
              .testTag("home_search_bar"),
            color = if (isDark) MaterialTheme.colorScheme.surface else TelefamPureWhite,
            shape = RoundedCornerShape(24.dp),
          ) {
            Row(
              modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = stringResource(R.string.home_search_placeholder),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          Spacer(modifier = Modifier.width(6.dp))

          CompactThemeToggleChip(
            themeMode = uiState.themeMode,
            onToggle = onToggleTheme,
          )

          NotificationBellAction(
            unreadCount = uiState.unreadNotificationCount,
            tint = TelefamPureWhite,
            onClick = onOpenNotifications,
            testTag = "home_notifications_button",
          )

          IconButton(
            onClick = onOpenChatPrivacy,
            modifier = Modifier.testTag("home_more_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.MoreVert,
              contentDescription = stringResource(R.string.cd_more_options),
              tint = TelefamPureWhite,
            )
          }
        }
      }

      // Main surface + adaptive bottom bar / tablet-desktop rail
      Surface(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        shape = if (isContactsTab || isFeedsTab || isProfileTab) {
          RoundedCornerShape(0.dp)
        } else {
          RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        },
        color = if (isFeedsTab) Color(0xFF07080B) else MaterialTheme.colorScheme.background,
      ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
          val isWideScreen = maxWidth >= 680.dp

          if (isWideScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
              TelefamHomeSideRail(
                activeTab = uiState.activeHomeTab,
                avatarPath = uiState.savedAvatarLocalPath,
                unreadChatCount = unreadChatCount,
                unreadNotificationCount = uiState.unreadNotificationCount,
                onSelectTab = onSelectTab,
                onOpenNotifications = onOpenNotifications,
              )
              HorizontalDivider(
                modifier = Modifier
                  .width(1.dp)
                  .fillMaxHeight(),
                color = MaterialTheme.colorScheme.outlineVariant,
              )
              Box(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight()
                  .navigationBarsPadding(),
                contentAlignment = Alignment.Center,
              ) {
                HomeTabContent(
                  uiState = uiState,
                  blushBg = blushBg,
                  feedBottomPadding = 0.dp,
                  isWideSplitScreen = true,
                  onStartChatClick = onOpenHomeChatSearch,
                  onReturnToChats = { onSelectTab(HomeTab.CHATS) },
                  onToggleContactFollow = onToggleContactFollow,
                  onLoadNextContactsPage = onLoadNextContactsPage,
                  onContactsScrollVisibilityChanged = onContactsScrollVisibilityChanged,
                  onSelectFeedTab = onSelectFeedTab,
                  onLiveClick = { onTriggerComingSoon("LIVE", "feed_live_tapped") },
                  onOpenFeedSearch = onOpenFeedSearch,
                  onLoadNextFeedPage = onLoadNextFeedPage,
                  onRefreshFeeds = onRefreshFeeds,
                  onToggleFeedAuthorFollow = onToggleFeedAuthorFollow,
                  onToggleFeedLike = onToggleFeedLike,
                  onDoubleTapFeedLike = onDoubleTapFeedLike,
                  onCommentClick = { onTriggerComingSoon("Comments", "feed_comments_tapped") },
                  onToggleFeedFavourite = onToggleFeedFavourite,
                  onShareFeedPost = onShareFeedPost,
                  onToggleFeedRepost = onToggleFeedRepost,
                  onDownloadFeedVideo = onDownloadFeedVideo,
                  onNotInterestedFeedPost = onNotInterestedFeedPost,
                  onReportFeedPost = onReportFeedPost,
                  onBlockFeedAuthor = onBlockFeedAuthor,
                  onMuteFeedAuthor = onMuteFeedAuthor,
                  onEditOwnerFeedPost = onEditOwnerFeedPost,
                  onChangeFeedPostPrivacy = onChangeFeedPostPrivacy,
                  onDeleteOwnerFeedPost = onDeleteOwnerFeedPost,
                  onTapFeedHashtag = onTapFeedHashtag,
                  onTapFeedAudio = onTapFeedAudio,
                  onRecordFeedWatchProgress = onRecordFeedWatchProgress,
                  onShowFeedToast = onShowFeedToast,
                  onOpenUserProfile = onOpenUserProfile,
                  onToggleProfileFollow = onToggleProfileFollow,
                  onOpenProfileConnections = onOpenProfileConnections,
                  onDismissProfileConnections = onDismissProfileConnections,
                  onLoadMoreProfileConnections = onLoadMoreProfileConnections,
                  onToggleProfileConnectionFollow = onToggleProfileConnectionFollow,
                  onSelectOwnerProfileTab = onSelectOwnerProfileTab,
                  onOpenEditProfile = onOpenEditProfile,
                  onOpenProfileBackgrounds = onOpenProfileBackgrounds,
                  onOpenProfileSettings = onOpenProfileSettings,
                  onMenuClick = onMenuClick,
                  onSelectProfileFilter = onSelectProfileFilter,
                  onSelectProfileSort = onSelectProfileSort,
                  onOpenProfilePostViewer = onOpenProfilePostViewer,
                  onBlockProfileUser = onBlockProfileUser,
                  onUnblockProfileUser = onUnblockProfileUser,
                  onReportProfileUser = onReportProfileUser,
                  onShowProfileToast = onShowProfileToast,
                  onToggleTheme = onToggleTheme,
                  onSignOut = onSignOut,
                  onOpenChatConversation = onOpenChatConversation,
                  onEnterChatMultiSelect = onEnterChatMultiSelect,
                  onToggleChatSelection = onToggleChatSelection,
                  onSelectAllChats = onSelectAllChats,
                  onExitChatMultiSelect = onExitChatMultiSelect,
                  onBatchChatAction = onBatchChatAction,
                  onUndoChatBatch = onUndoChatBatch,
                  onDismissChatBatchUndo = onDismissChatBatchUndo,
                  onSavedLinksSearchQueryChanged = onSavedLinksSearchQueryChanged,
                  onOpenSavedLink = onOpenSavedLink,
                  onDeleteSavedLink = onDeleteSavedLink,
                  embeddedSplitChatContent = embeddedSplitChatContent,
                )
              }
            }
          } else {
            Box(modifier = Modifier.fillMaxSize()) {
              Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
              ) {
                HomeTabContent(
                  uiState = uiState,
                  blushBg = blushBg,
                  feedBottomPadding = 68.dp,
                  isWideSplitScreen = false,
                  onStartChatClick = onOpenHomeChatSearch,
                  onReturnToChats = { onSelectTab(HomeTab.CHATS) },
                  onToggleContactFollow = onToggleContactFollow,
                  onLoadNextContactsPage = onLoadNextContactsPage,
                  onContactsScrollVisibilityChanged = onContactsScrollVisibilityChanged,
                  onSelectFeedTab = onSelectFeedTab,
                  onLiveClick = { onTriggerComingSoon("LIVE", "feed_live_tapped") },
                  onOpenFeedSearch = onOpenFeedSearch,
                  onLoadNextFeedPage = onLoadNextFeedPage,
                  onRefreshFeeds = onRefreshFeeds,
                  onToggleFeedAuthorFollow = onToggleFeedAuthorFollow,
                  onToggleFeedLike = onToggleFeedLike,
                  onDoubleTapFeedLike = onDoubleTapFeedLike,
                  onCommentClick = { onTriggerComingSoon("Comments", "feed_comments_tapped") },
                  onToggleFeedFavourite = onToggleFeedFavourite,
                  onShareFeedPost = onShareFeedPost,
                  onToggleFeedRepost = onToggleFeedRepost,
                  onDownloadFeedVideo = onDownloadFeedVideo,
                  onNotInterestedFeedPost = onNotInterestedFeedPost,
                  onReportFeedPost = onReportFeedPost,
                  onBlockFeedAuthor = onBlockFeedAuthor,
                  onMuteFeedAuthor = onMuteFeedAuthor,
                  onEditOwnerFeedPost = onEditOwnerFeedPost,
                  onChangeFeedPostPrivacy = onChangeFeedPostPrivacy,
                  onDeleteOwnerFeedPost = onDeleteOwnerFeedPost,
                  onTapFeedHashtag = onTapFeedHashtag,
                  onTapFeedAudio = onTapFeedAudio,
                  onRecordFeedWatchProgress = onRecordFeedWatchProgress,
                  onShowFeedToast = onShowFeedToast,
                  onOpenUserProfile = onOpenUserProfile,
                  onToggleProfileFollow = onToggleProfileFollow,
                  onOpenProfileConnections = onOpenProfileConnections,
                  onDismissProfileConnections = onDismissProfileConnections,
                  onLoadMoreProfileConnections = onLoadMoreProfileConnections,
                  onToggleProfileConnectionFollow = onToggleProfileConnectionFollow,
                  onSelectOwnerProfileTab = onSelectOwnerProfileTab,
                  onOpenEditProfile = onOpenEditProfile,
                  onOpenProfileBackgrounds = onOpenProfileBackgrounds,
                  onOpenProfileSettings = onOpenProfileSettings,
                  onMenuClick = onMenuClick,
                  onSelectProfileFilter = onSelectProfileFilter,
                  onSelectProfileSort = onSelectProfileSort,
                  onOpenProfilePostViewer = onOpenProfilePostViewer,
                  onBlockProfileUser = onBlockProfileUser,
                  onUnblockProfileUser = onUnblockProfileUser,
                  onReportProfileUser = onReportProfileUser,
                  onShowProfileToast = onShowProfileToast,
                  onToggleTheme = onToggleTheme,
                  onSignOut = onSignOut,
                  onOpenChatConversation = onOpenChatConversation,
                  onEnterChatMultiSelect = onEnterChatMultiSelect,
                  onToggleChatSelection = onToggleChatSelection,
                  onSelectAllChats = onSelectAllChats,
                  onExitChatMultiSelect = onExitChatMultiSelect,
                  onBatchChatAction = onBatchChatAction,
                  onUndoChatBatch = onUndoChatBatch,
                  onDismissChatBatchUndo = onDismissChatBatchUndo,
                  onSavedLinksSearchQueryChanged = onSavedLinksSearchQueryChanged,
                  onOpenSavedLink = onOpenSavedLink,
                  onDeleteSavedLink = onDeleteSavedLink,
                  embeddedSplitChatContent = null,
                )
              }

              androidx.compose.animation.AnimatedVisibility(
                visible = !isContactsTab || uiState.isBottomBarVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
              ) {
                TelefamHomeBottomBar(
                  activeTab = uiState.activeHomeTab,
                  avatarPath = uiState.savedAvatarLocalPath,
                  unreadChatCount = unreadChatCount,
                  onSelectTab = onSelectTab,
                )
              }
            }
          }
        }
      }
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .padding(bottom = 76.dp, start = 16.dp, end = 16.dp),
    )
  }
}

@Composable
private fun HomeTabContent(
  uiState: TelefamUiState,
  blushBg: Color,
  feedBottomPadding: Dp,
  onStartChatClick: () -> Unit,
  onReturnToChats: () -> Unit,
  onToggleContactFollow: (String) -> Unit,
  onLoadNextContactsPage: () -> Unit,
  onContactsScrollVisibilityChanged: (Boolean) -> Unit,
  onSelectFeedTab: (FeedTab) -> Unit,
  onLiveClick: () -> Unit,
  onOpenFeedSearch: () -> Unit,
  onLoadNextFeedPage: () -> Unit,
  onRefreshFeeds: () -> Unit,
  onToggleFeedAuthorFollow: (String, Boolean) -> Unit,
  onToggleFeedLike: (String, Boolean) -> Unit,
  onDoubleTapFeedLike: (String) -> Unit,
  onCommentClick: () -> Unit,
  onToggleFeedFavourite: (String, Boolean) -> Unit,
  onShareFeedPost: (String) -> Unit,
  onToggleFeedRepost: (String, Boolean) -> Unit,
  onDownloadFeedVideo: (FeedUiItem) -> Unit,
  onNotInterestedFeedPost: (String) -> Unit,
  onReportFeedPost: (String, String) -> Unit,
  onBlockFeedAuthor: (String, String) -> Unit,
  onMuteFeedAuthor: (String) -> Unit,
  onEditOwnerFeedPost: (FeedUiItem) -> Unit,
  onChangeFeedPostPrivacy: (String, PostPrivacy) -> Unit,
  onDeleteOwnerFeedPost: (String) -> Unit,
  onTapFeedHashtag: (String, PostMode) -> Unit,
  onTapFeedAudio: (PostSongTrack, PostMode) -> Unit,
  onRecordFeedWatchProgress: (String, Long, Long) -> Unit,
  onShowFeedToast: (String) -> Unit,
  onOpenUserProfile: (String) -> Unit,
  onToggleProfileFollow: (String) -> Unit,
  onOpenProfileConnections: (String, Boolean) -> Unit,
  onDismissProfileConnections: (String) -> Unit,
  onLoadMoreProfileConnections: (String) -> Unit,
  onToggleProfileConnectionFollow: (String, String) -> Unit,
  onSelectOwnerProfileTab: (String, OwnerProfileTab) -> Unit,
  onOpenEditProfile: () -> Unit,
  onOpenProfileBackgrounds: () -> Unit,
  onOpenProfileSettings: () -> Unit,
  onMenuClick: () -> Unit,
  onSelectProfileFilter: (String, ProfilePostFilter) -> Unit,
  onSelectProfileSort: (String, ProfilePostSort) -> Unit,
  onOpenProfilePostViewer: (List<FeedUiItem>, Int) -> Unit,
  onBlockProfileUser: (String) -> Unit,
  onUnblockProfileUser: (String) -> Unit,
  onReportProfileUser: (String, String, String) -> Unit,
  onShowProfileToast: (String) -> Unit,
  onToggleTheme: () -> Unit,
  onSignOut: () -> Unit,
  isWideSplitScreen: Boolean = false,
  onOpenChatConversation: (telefam.company.data.local.ChatConversationEntity) -> Unit = {},
  onEnterChatMultiSelect: (String) -> Unit = {},
  onToggleChatSelection: (String) -> Unit = {},
  onSelectAllChats: () -> Unit = {},
  onExitChatMultiSelect: () -> Unit = {},
  onBatchChatAction: (String) -> Unit = {},
  onUndoChatBatch: () -> Unit = {},
  onDismissChatBatchUndo: () -> Unit = {},
  onSavedLinksSearchQueryChanged: (String) -> Unit = {},
  onOpenSavedLink: (telefam.company.data.local.SavedLinkEntity) -> Unit = {},
  onDeleteSavedLink: (String) -> Unit = {},
  embeddedSplitChatContent: (@Composable () -> Unit)? = null,
) {
  val activeTab = uiState.activeHomeTab

  AnimatedVisibility(
    visible = activeTab == HomeTab.CHATS,
    enter = fadeIn(),
    exit = fadeOut(),
  ) {
    val isDark = LocalTelefamDarkTheme.current
    val chatTheme = telefam.company.core.TelefamChatPrivacyCatalog.themeById(uiState.chatPrivacy.chatThemeId)

    val listPane: @Composable (Modifier) -> Unit = { paneModifier ->
      Box(modifier = paneModifier) {
        TelefamConversationsListPane(
          chatState = uiState.chatState,
          chatPrivacy = uiState.chatPrivacy,
          currentUserId = uiState.userId,
          blushBg = blushBg,
          bottomPadding = feedBottomPadding,
          onStartChatClick = onStartChatClick,
          onOpenConversation = onOpenChatConversation,
          onEnterMultiSelect = onEnterChatMultiSelect,
          onToggleSelection = onToggleChatSelection,
          onSelectAll = onSelectAllChats,
          onExitMultiSelect = onExitChatMultiSelect,
          onBatchAction = onBatchChatAction,
          onUndoBatch = onUndoChatBatch,
          onDismissUndo = onDismissChatBatchUndo,
        )
      }
    }

    if (isWideSplitScreen) {
      Row(modifier = Modifier.fillMaxSize().testTag("chats_split_layout")) {
        listPane(Modifier.width(340.dp).fillMaxHeight())
        HorizontalDivider(
          modifier = Modifier.width(1.dp).fillMaxHeight(),
          color = MaterialTheme.colorScheme.outlineVariant,
        )
        Box(modifier = Modifier.weight(1f).fillMaxHeight().testTag("chats_split_detail_pane")) {
          if (uiState.chatState.activeConversation != null && embeddedSplitChatContent != null) {
            embeddedSplitChatContent()
          } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              if (chatTheme.themeId != "none") {
                ChatWallpaperCanvas(
                  theme = chatTheme,
                  isDark = isDark,
                  modifier = Modifier.fillMaxSize(),
                )
              }
              Surface(
                modifier = Modifier
                  .padding(horizontal = 32.dp)
                  .widthIn(max = 360.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
              ) {
                Column(
                  modifier = Modifier.padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Box(
                    modifier = Modifier
                      .size(56.dp)
                      .clip(CircleShape)
                      .background(blushBg),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Forum,
                      contentDescription = null,
                      tint = TelefamCrimson,
                      modifier = Modifier.size(26.dp),
                    )
                  }
                  Spacer(modifier = Modifier.height(14.dp))
                  Text(
                    text = stringResource(R.string.chat_split_select_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = stringResource(R.string.chat_split_select_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                  )
                }
              }
            }
          }
        }
      }
    } else {
      listPane(Modifier.fillMaxSize())
    }
  }

  AnimatedVisibility(
    visible = activeTab == HomeTab.CONTACTS,
    enter = fadeIn(),
    exit = fadeOut(),
  ) {
    TelefamContactsContent(
      contactsState = uiState.contactsState,
      onToggleFollow = onToggleContactFollow,
      onLoadNextPage = onLoadNextContactsPage,
      onScrollVisibilityChanged = onContactsScrollVisibilityChanged,
      onOpenUserProfile = onOpenUserProfile,
    )
  }

  AnimatedVisibility(
    visible = activeTab == HomeTab.FEEDS,
    enter = fadeIn(),
    exit = fadeOut(),
  ) {
    val feeds = uiState.feedsState
    val chromeInset = if (feedBottomPadding > 0.dp) {
      feedBottomPadding + 6.dp
    } else {
      0.dp
    }
    Box(
      modifier = Modifier
        .fillMaxSize()
        .let { m -> if (feedBottomPadding > 0.dp) m else m },
    ) {
      ReusableVideoFeedScreen(
        items = feeds.activeItems,
        initialIndex = 0,
        activeTab = feeds.activeTab,
        isStandaloneViewer = false,
        isHostActive = activeTab == HomeTab.FEEDS &&
          !feeds.isSearchOpen &&
          uiState.activeUserProfile == null &&
          uiState.profileViewerItems == null &&
          uiState.activeSettingsDestination == null &&
          uiState.activeChatPrivacyRoute == null &&
          !uiState.postComposer.isOpen,
        isLoading = feeds.isLoading,
        hasMore = feeds.hasMoreActive,
        networkTier = uiState.effectiveNetworkTierForFeeds,
        streamAccessToken = feeds.streamAccessToken,
        streamDeviceFp = feeds.streamDeviceFp,
        bottomChromePadding = chromeInset,
        onSelectFeedTab = onSelectFeedTab,
        onLiveClick = onLiveClick,
        onSearchClick = onOpenFeedSearch,
        onLoadMore = onLoadNextFeedPage,
        onRefresh = onRefreshFeeds,
        onToggleFollowAuthor = onToggleFeedAuthorFollow,
        onToggleLike = onToggleFeedLike,
        onDoubleTapLike = onDoubleTapFeedLike,
        onCommentClick = { onCommentClick() },
        onToggleFavourite = onToggleFeedFavourite,
        onShareRecorded = onShareFeedPost,
        onToggleRepost = onToggleFeedRepost,
        onDownloadVideo = onDownloadFeedVideo,
        onNotInterested = onNotInterestedFeedPost,
        onReportPost = onReportFeedPost,
        onBlockAuthor = onBlockFeedAuthor,
        onMuteAuthor = onMuteFeedAuthor,
        onEditOwnerPost = onEditOwnerFeedPost,
        onChangePostPrivacy = onChangeFeedPostPrivacy,
        onDeleteOwnerPost = onDeleteOwnerFeedPost,
        onTapHashtag = onTapFeedHashtag,
        onTapAudio = onTapFeedAudio,
        onRecordWatchProgress = onRecordFeedWatchProgress,
        onOpenAuthorProfile = onOpenUserProfile,
        autoplayVideos = uiState.settings.contentPreferences.autoplayVideos,
        captionsEnabled = uiState.settings.contentPreferences.captionsEnabled,
        reducedMotion = uiState.settings.contentPreferences.reducedMotion,
        onShowToast = onShowFeedToast,
      )
    }
  }

  AnimatedVisibility(
    visible = activeTab == HomeTab.PROFILE,
    enter = fadeIn(),
    exit = fadeOut(),
  ) {
    val ownProfile = uiState.ownProfileState ?: UserProfileScreenState(
      userId = uiState.userId,
      fullName = uiState.profileForm.fullName,
      username = uiState.profileForm.username,
      avatarUrl = uiState.savedAvatarLocalPath,
      bio = uiState.profileForm.bio,
      location = uiState.profileForm.location,
      isOwner = true,
      isLoading = true,
    )
    TelefamUserProfileScreen(
      profile = ownProfile,
      isEmbeddedOwnProfileTab = true,
      bottomChromePadding = if (feedBottomPadding > 0.dp) feedBottomPadding + 6.dp else 0.dp,
      onBack = onReturnToChats,
      onToggleFollow = { onToggleProfileFollow(ownProfile.userId) },
      onMessageClick = { onReturnToChats() },
      onSubscribeClick = onOwnerSubscribersClick,
      onOwnerSubscribersClick = onOwnerSubscribersClick,
      onOpenConnections = { isFollowers -> onOpenProfileConnections(ownProfile.userId, isFollowers) },
      onDismissConnections = { onDismissProfileConnections(ownProfile.userId) },
      onLoadMoreConnections = { onLoadMoreProfileConnections(ownProfile.userId) },
      onToggleConnectionFollow = { targetId -> onToggleProfileConnectionFollow(ownProfile.userId, targetId) },
      onOpenUserProfile = onOpenUserProfile,
      onSelectOwnerTab = { tab -> onSelectOwnerProfileTab(ownProfile.userId, tab) },
      onOpenEditProfile = onOpenEditProfile,
      onOpenBackgrounds = onOpenProfileBackgrounds,
      onOpenSettings = onOpenProfileSettings,
      onOpenMenu = onMenuClick,
      onSelectFilter = { filter -> onSelectProfileFilter(ownProfile.userId, filter) },
      onSelectSort = { sort -> onSelectProfileSort(ownProfile.userId, sort) },
      onOpenPostAtIndex = onOpenProfilePostViewer,
      onBlockUser = { onBlockProfileUser(ownProfile.userId) },
      onUnblockUser = { onUnblockProfileUser(ownProfile.userId) },
      onReportUser = { reason, details -> onReportProfileUser(ownProfile.userId, reason, details) },
      onShowToast = onShowProfileToast,
      onToggleTheme = onToggleTheme,
      onSignOut = onSignOut,
      savedLinks = uiState.savedLinks,
      savedLinksSearchQuery = uiState.savedLinksSearchQuery,
      onSavedLinksSearchQueryChanged = onSavedLinksSearchQueryChanged,
      onOpenSavedLink = onOpenSavedLink,
      onDeleteSavedLink = onDeleteSavedLink,
    )
  }

  AnimatedVisibility(
    visible = activeTab != HomeTab.CHATS && activeTab != HomeTab.CONTACTS && activeTab != HomeTab.FEEDS && activeTab != HomeTab.PROFILE,
    enter = fadeIn(),
    exit = fadeOut(),
  ) {
    Column(
      modifier = Modifier
        .padding(horizontal = 32.dp)
        .widthIn(max = 360.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        modifier = Modifier
          .size(62.dp)
          .clip(CircleShape)
          .background(blushBg),
        contentAlignment = Alignment.Center,
      ) {
        val tabIcon = when (activeTab) {
          HomeTab.CONTACTS -> Icons.Outlined.Group
          HomeTab.POST -> Icons.Filled.Add
          HomeTab.FEEDS -> Icons.Outlined.DynamicFeed
          HomeTab.PROFILE -> Icons.Outlined.Person
          HomeTab.CHATS -> Icons.AutoMirrored.Outlined.Chat
        }
        Icon(
          imageVector = tabIcon,
          contentDescription = null,
          tint = TelefamCrimson,
          modifier = Modifier.size(28.dp),
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = stringResource(R.string.coming_soon_title),
        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = stringResource(R.string.coming_soon_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(18.dp))
      Button(
        onClick = onReturnToChats,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = TelefamCrimson,
          contentColor = TelefamPureWhite,
        ),
        modifier = Modifier.height(40.dp),
      ) {
        Text(stringResource(R.string.nav_chats), fontSize = 13.sp)
      }
    }
  }
}

@Composable
private fun TelefamHomeBottomBar(
  activeTab: HomeTab,
  avatarPath: String?,
  unreadChatCount: Int = 0,
  onSelectTab: (HomeTab) -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shadowElevation = 6.dp,
  ) {
    Column(modifier = Modifier.navigationBarsPadding()) {
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(68.dp)
          .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround,
      ) {
        BottomNavItem(
          label = stringResource(R.string.nav_chats),
          icon = if (activeTab == HomeTab.CHATS) Icons.Filled.ChatBubble else Icons.AutoMirrored.Outlined.Chat,
          selected = activeTab == HomeTab.CHATS,
          badgeCount = unreadChatCount,
          onClick = { onSelectTab(HomeTab.CHATS) },
          testTag = "nav_chats",
        )
        BottomNavItem(
          label = stringResource(R.string.nav_contacts),
          icon = if (activeTab == HomeTab.CONTACTS) Icons.Filled.Group else Icons.Outlined.Group,
          selected = activeTab == HomeTab.CONTACTS,
          onClick = { onSelectTab(HomeTab.CONTACTS) },
          testTag = "nav_contacts",
        )

        // Elevated circular crimson Post "+" button matching Image 2
        Column(
          modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onSelectTab(HomeTab.POST) }
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .testTag("nav_post"),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .shadow(6.dp, CircleShape)
              .clip(CircleShape)
              .background(TelefamCrimson),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.Add,
              contentDescription = stringResource(R.string.nav_post),
              tint = TelefamPureWhite,
              modifier = Modifier.size(22.dp),
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = stringResource(R.string.nav_post),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
            color = if (activeTab == HomeTab.POST) TelefamCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        BottomNavItem(
          label = stringResource(R.string.nav_feeds),
          icon = Icons.Outlined.DynamicFeed,
          selected = activeTab == HomeTab.FEEDS,
          onClick = { onSelectTab(HomeTab.FEEDS) },
          testTag = "nav_feeds",
        )

        // Profile avatar nav item matching Image 2
        Column(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelectTab(HomeTab.PROFILE) }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("nav_profile"),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          AvatarThumb(
            avatarPath = avatarPath,
            selected = activeTab == HomeTab.PROFILE,
            size = 24.dp,
          )
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = stringResource(R.string.nav_profile),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
            color = if (activeTab == HomeTab.PROFILE) TelefamCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun TelefamHomeSideRail(
  activeTab: HomeTab,
  avatarPath: String?,
  unreadChatCount: Int = 0,
  unreadNotificationCount: Int = 0,
  onSelectTab: (HomeTab) -> Unit,
  onOpenNotifications: () -> Unit = {},
) {
  Column(
    modifier = Modifier
      .width(86.dp)
      .fillMaxHeight()
      .background(MaterialTheme.colorScheme.surface)
      .navigationBarsPadding()
      .padding(vertical = 20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    BottomNavItem(
      label = stringResource(R.string.nav_chats),
      icon = if (activeTab == HomeTab.CHATS) Icons.Filled.ChatBubble else Icons.AutoMirrored.Outlined.Chat,
      selected = activeTab == HomeTab.CHATS,
      badgeCount = unreadChatCount,
      onClick = { onSelectTab(HomeTab.CHATS) },
      testTag = "rail_chats",
    )
    BottomNavItem(
      label = stringResource(R.string.nav_contacts),
      icon = if (activeTab == HomeTab.CONTACTS) Icons.Filled.Group else Icons.Outlined.Group,
      selected = activeTab == HomeTab.CONTACTS,
      onClick = { onSelectTab(HomeTab.CONTACTS) },
      testTag = "rail_contacts",
    )
    Box(
      modifier = Modifier
        .size(44.dp)
        .shadow(6.dp, CircleShape)
        .clip(CircleShape)
        .background(TelefamCrimson)
        .clickable { onSelectTab(HomeTab.POST) }
        .testTag("rail_post"),
      contentAlignment = Alignment.Center,
    ) {
      Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.nav_post), tint = TelefamPureWhite)
    }
    BottomNavItem(
      label = stringResource(R.string.nav_feeds),
      icon = Icons.Outlined.DynamicFeed,
      selected = activeTab == HomeTab.FEEDS,
      onClick = { onSelectTab(HomeTab.FEEDS) },
      testTag = "rail_feeds",
    )
    Column(
      modifier = Modifier
        .clip(RoundedCornerShape(12.dp))
        .clickable { onSelectTab(HomeTab.PROFILE) }
        .padding(8.dp)
        .testTag("rail_profile"),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      AvatarThumb(avatarPath = avatarPath, selected = activeTab == HomeTab.PROFILE, size = 28.dp)
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = stringResource(R.string.nav_profile),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
        color = if (activeTab == HomeTab.PROFILE) TelefamCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    BottomNavItem(
      label = stringResource(R.string.notif_inbox_title),
      icon = Icons.Outlined.Notifications,
      selected = false,
      badgeCount = unreadNotificationCount,
      onClick = onOpenNotifications,
      testTag = "rail_notifications",
    )
  }
}

@Composable
private fun NotificationBellAction(
  unreadCount: Int,
  tint: Color,
  onClick: () -> Unit,
  testTag: String,
) {
  IconButton(
    onClick = onClick,
    modifier = Modifier.testTag(testTag),
  ) {
    Box(contentAlignment = Alignment.TopEnd) {
      Icon(
        imageVector = if (unreadCount > 0) Icons.Filled.Notifications else Icons.Outlined.Notifications,
        contentDescription = stringResource(R.string.cd_notifications),
        tint = tint,
        modifier = Modifier.padding(2.dp),
      )
      if (unreadCount > 0) {
        Box(
          modifier = Modifier
            .clip(CircleShape)
            .background(Color(0xFF151821))
            .border(1.dp, TelefamPureWhite, CircleShape)
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .testTag("${testTag}_badge"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
            color = TelefamPureWhite,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@Composable
private fun BottomNavItem(
  label: String,
  icon: ImageVector,
  selected: Boolean,
  badgeCount: Int = 0,
  onClick: () -> Unit,
  testTag: String,
) {
  val tint = if (selected) TelefamCrimson else MaterialTheme.colorScheme.onSurfaceVariant
  Column(
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 6.dp)
      .testTag(testTag),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(contentAlignment = Alignment.TopEnd) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = tint,
        modifier = Modifier
          .padding(horizontal = 4.dp)
          .size(22.dp),
      )
      if (badgeCount > 0) {
        Box(
          modifier = Modifier
            .clip(CircleShape)
            .background(TelefamCrimson)
            .padding(horizontal = 5.dp, vertical = 1.dp)
            .testTag("${testTag}_badge"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
            color = TelefamPureWhite,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
    Spacer(modifier = Modifier.height(3.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.5.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
      ),
      color = tint,
    )
  }
}

@Composable
private fun AvatarThumb(
  avatarPath: String?,
  selected: Boolean,
  size: androidx.compose.ui.unit.Dp,
) {
  val bmp = remember(avatarPath) {
    if (!avatarPath.isNullOrBlank() && File(avatarPath).exists()) {
      BitmapFactory.decodeFile(avatarPath)?.asImageBitmap()
    } else {
      null
    }
  }

  Box(
    modifier = Modifier
      .size(size)
      .clip(CircleShape)
      .border(
        width = if (selected) 1.5.dp else 1.dp,
        color = if (selected) TelefamCrimson else MaterialTheme.colorScheme.outline,
        shape = CircleShape,
      )
      .background(MaterialTheme.colorScheme.surfaceVariant),
    contentAlignment = Alignment.Center,
  ) {
    if (bmp != null) {
      Image(
        bitmap = bmp,
        contentDescription = stringResource(R.string.nav_profile),
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      Icon(
        imageVector = Icons.Filled.Person,
        contentDescription = stringResource(R.string.nav_profile),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(size * 0.68f),
      )
    }
  }
}
