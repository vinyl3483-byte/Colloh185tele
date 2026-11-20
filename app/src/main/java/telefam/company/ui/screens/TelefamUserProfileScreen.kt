package telefam.company.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import coil.compose.AsyncImage
import com.telefam.company.app.BuildConfig
import com.telefam.company.app.R
import kotlinx.coroutines.delay
import telefam.company.core.ContactRelationshipState
import telefam.company.core.OwnerProfileTab
import telefam.company.core.PostMode
import telefam.company.core.PostPrivacy
import telefam.company.core.ProfileBackgroundItem
import telefam.company.core.ProfilePostFilter
import telefam.company.core.ProfilePostSort
import telefam.company.core.ProfileReportCategory
import telefam.company.core.ProfileReportOptions
import telefam.company.core.TelefamContactsRules
import telefam.company.core.TelefamPostRules
import telefam.company.core.formatProfileMetricCount
import telefam.company.data.local.SavedLinkEntity
import telefam.company.ui.components.PostThemeCanvasBox
import telefam.company.ui.components.SmartInteractiveText
import telefam.company.ui.components.postCardTextStyle
import telefam.company.ui.theme.LocalTelefamDarkTheme
import telefam.company.ui.theme.TelefamBlushWave
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamCrimsonBright
import telefam.company.ui.theme.TelefamDarkBlush
import telefam.company.ui.theme.TelefamPureWhite
import telefam.company.ui.viewmodel.ContactUiItem
import telefam.company.ui.viewmodel.FeedUiItem
import telefam.company.ui.viewmodel.UserProfileScreenState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelefamUserProfileScreen(
  profile: UserProfileScreenState,
  isEmbeddedOwnProfileTab: Boolean = false,
  bottomChromePadding: Dp = 0.dp,
  onBack: () -> Unit,
  onToggleFollow: () -> Unit,
  onMessageClick: () -> Unit,
  onSubscribeClick: () -> Unit,
  onOpenConnections: (isFollowers: Boolean) -> Unit,
  onDismissConnections: () -> Unit,
  onLoadMoreConnections: () -> Unit,
  onToggleConnectionFollow: (String) -> Unit,
  onOpenUserProfile: (String) -> Unit,
  onSelectOwnerTab: (OwnerProfileTab) -> Unit = {},
  onOpenEditProfile: () -> Unit = {},
  onOpenBackgrounds: () -> Unit = {},
  onOpenSettings: () -> Unit = {},
  onOpenMenu: () -> Unit = {},
  onSelectFilter: (ProfilePostFilter) -> Unit,
  onSelectSort: (ProfilePostSort) -> Unit,
  onOpenPostAtIndex: (List<FeedUiItem>, Int) -> Unit,
  onBlockUser: () -> Unit,
  onUnblockUser: () -> Unit,
  onMuteToggle: () -> Unit = {},
  onReportUser: (reason: String, details: String) -> Unit,
  onShowToast: (String) -> Unit,
  onToggleTheme: () -> Unit = {},
  onSignOut: () -> Unit = {},
  savedLinks: List<SavedLinkEntity> = emptyList(),
  savedLinksSearchQuery: String = "",
  onSavedLinksSearchQueryChanged: (String) -> Unit = {},
  onOpenSavedLink: (SavedLinkEntity) -> Unit = {},
  onDeleteSavedLink: (String) -> Unit = {},
) {
  val context = LocalContext.current
  val isDark = LocalTelefamDarkTheme.current
  val accentCrimson = if (isDark) TelefamCrimsonBright else TelefamCrimson
  val isPrivateRestricted = !profile.isOwner && profile.isPrivateAccount && !profile.isFollowing && !profile.isBlocked

  if (!isEmbeddedOwnProfileTab) {
    BackHandler(enabled = true) { onBack() }
  }

  var showMoreMenu by remember { mutableStateOf(false) }
  var showFilterMenu by remember { mutableStateOf(false) }
  var showSortMenu by remember { mutableStateOf(false) }
  var showReportCategoryDialog by remember { mutableStateOf(false) }
  var selectedReportCategory by remember { mutableStateOf<ProfileReportCategory?>(null) }
  var reportDetails by remember { mutableStateOf("") }
  var savedSubTab by remember { mutableIntStateOf(0) } // 0 = Posts, 1 = Links

  val showHeroBanner = !profile.isBlocked && (profile.isOwner || profile.backgrounds.isNotEmpty())

  val profileLink = remember(profile.username, profile.userId) {
    val configuredBase = BuildConfig.TELEFAM_API_BASE_URL.trim().trimEnd('/')
    val handle = profile.username.ifBlank { profile.userId }
    if (configuredBase.startsWith("https://") || configuredBase.startsWith("http://")) {
      "$configuredBase/u/$handle"
    } else {
      "telefam://u/$handle"
    }
  }

  val copyProfileLink: () -> Unit = {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Telefam Profile Link", profileLink))
    onShowToast(context.getString(R.string.feed_link_copied))
  }

  val inviteFriendsShare: () -> Unit = {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(
        Intent.EXTRA_TEXT,
        "${context.getString(R.string.profile_invite_text)} $profileLink".trim(),
      )
    }
    context.startActivity(Intent.createChooser(sendIntent, null))
  }

  val filteredPosts = remember(
    profile.activeOwnerTab,
    profile.posts,
    profile.savedPosts,
    profile.repostedPosts,
    profile.lockedPosts,
    profile.selectedFilter,
    profile.selectedSort,
  ) {
    profile.filteredAndSortedPosts
  }

  Surface(
    modifier = Modifier
      .fillMaxSize()
      .testTag(if (isEmbeddedOwnProfileTab) "own_profile_screen" else "user_profile_screen_${profile.userId}"),
    color = MaterialTheme.colorScheme.background,
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      if (!showHeroBanner) {
        // Flat Top Bar for visitors without background photos or blocked users
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .then(if (!isEmbeddedOwnProfileTab) Modifier.statusBarsPadding() else Modifier.statusBarsPadding())
            .height(54.dp)
            .padding(horizontal = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = onBack,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("profile_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.cd_back),
              tint = MaterialTheme.colorScheme.onBackground,
            )
          }

          Text(
            text = profile.fullName.ifBlank { "@${profile.username.ifBlank { profile.userId }}" },
            style = MaterialTheme.typography.titleMedium.copy(
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
              .weight(1f)
              .testTag("profile_display_name"),
          )

          Box(
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .clip(RoundedCornerShape(10.dp))
              .clickable(onClick = inviteFriendsShare)
              .testTag("profile_invite_badge"),
            contentAlignment = Alignment.Center,
          ) {
            ProfileInviteBadgeSvgIcon(
              badgeColor = accentCrimson,
              modifier = Modifier.size(width = 30.dp, height = 27.dp),
            )
          }

          VisitorMoreMenuButton(
            expanded = showMoreMenu,
            isBlocked = profile.isBlocked,
            isMuted = profile.isMuted,
            accentCrimson = accentCrimson,
            iconTint = MaterialTheme.colorScheme.onBackground,
            onOpen = { showMoreMenu = true },
            onDismiss = { showMoreMenu = false },
            onCopyLink = copyProfileLink,
            onInviteFriends = inviteFriendsShare,
            onMuteToggle = onMuteToggle,
            onBlockToggle = { if (profile.isBlocked) onUnblockUser() else onBlockUser() },
            onReport = { showReportCategoryDialog = true },
          )
        }
      }

      BoxWithConstraints(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
      ) {
        val gridColumns = when {
          maxWidth >= 960.dp -> 5
          maxWidth >= 680.dp -> 4
          else -> 3
        }

        LazyVerticalGrid(
          columns = GridCells.Fixed(gridColumns),
          modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 920.dp)
            .testTag("profile_posts_grid"),
          contentPadding = PaddingValues(bottom = 24.dp + bottomChromePadding),
          horizontalArrangement = Arrangement.spacedBy(2.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          item(
            key = "profile_header",
            span = { GridItemSpan(maxLineSpan) },
          ) {
            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              if (showHeroBanner) {
                ProfileFadingHeroWithAvatar(
                  profile = profile,
                  accentCrimson = accentCrimson,
                  onBack = onBack,
                  onSubscribeClick = onSubscribeClick,
                  onOpenSettings = onOpenSettings,
                  onOpenMenu = onOpenMenu,
                  onOpenBackgrounds = onOpenBackgrounds,
                  onInviteBadgeClick = inviteFriendsShare,
                  showMoreMenu = showMoreMenu,
                  onOpenMoreMenu = { showMoreMenu = true },
                  onDismissMoreMenu = { showMoreMenu = false },
                  onCopyLink = copyProfileLink,
                  onInviteFriends = inviteFriendsShare,
                  onMuteToggle = onMuteToggle,
                  onBlockToggle = { if (profile.isBlocked) onUnblockUser() else onBlockUser() },
                  onReport = { showReportCategoryDialog = true },
                )
              } else {
                Spacer(modifier = Modifier.height(6.dp))
                ProfileGradientAvatar(
                  avatarUrl = profile.avatarUrl,
                  fullName = profile.fullName,
                  username = profile.username,
                )
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Centered @username
              Text(
                text = "@${profile.username.ifBlank { profile.userId }}",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontSize = 15.5.sp,
                  fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("profile_username"),
              )

              Spacer(modifier = Modifier.height(14.dp))

              // 3-Column Stats Bar (Following | Followers | Likes)
              Row(
                modifier = Modifier
                  .widthIn(max = 360.dp)
                  .fillMaxWidth()
                  .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                ProfileStatColumn(
                  countText = formatProfileMetricCount(profile.followingCount),
                  label = stringResource(R.string.profile_stat_following),
                  onClick = {
                    when {
                      isPrivateRestricted -> onShowToast(context.getString(R.string.profile_private_account_title))
                      !profile.isOwner && profile.followingVisibility == "only_me" ->
                        onShowToast(context.getString(R.string.profile_following_private_toast))
                      else -> onOpenConnections(false)
                    }
                  },
                  testTag = "profile_stat_following",
                  modifier = Modifier.weight(1f),
                )
                Box(
                  modifier = Modifier
                    .width(1.dp)
                    .height(26.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
                )
                ProfileStatColumn(
                  countText = formatProfileMetricCount(profile.followersCount),
                  label = stringResource(R.string.profile_stat_followers),
                  onClick = {
                    if (isPrivateRestricted) {
                      onShowToast(context.getString(R.string.profile_private_account_title))
                    } else {
                      onOpenConnections(true)
                    }
                  },
                  testTag = "profile_stat_followers",
                  modifier = Modifier.weight(1f),
                )
                Box(
                  modifier = Modifier
                    .width(1.dp)
                    .height(26.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
                )
                ProfileStatColumn(
                  countText = formatProfileMetricCount(profile.likesCount),
                  label = stringResource(R.string.profile_stat_likes),
                  onClick = null,
                  testTag = "profile_stat_likes",
                  modifier = Modifier.weight(1f),
                )
              }

              Spacer(modifier = Modifier.height(16.dp))

              // Primary Actions Row
              if (profile.isBlocked) {
                Column(
                  modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Text(
                    text = stringResource(R.string.profile_blocked_notice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Button(
                    onClick = onUnblockUser,
                    colors = ButtonDefaults.buttonColors(
                      containerColor = accentCrimson,
                      contentColor = TelefamPureWhite,
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(42.dp)
                      .testTag("profile_unblock_button"),
                  ) {
                    Text(
                      text = stringResource(R.string.profile_action_unblock),
                      fontWeight = FontWeight.SemiBold,
                    )
                  }
                }
              } else if (!profile.isOwner) {
                Row(
                  modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  ProfileRelationshipActionButton(
                    state = profile.relationshipState,
                    onClick = onToggleFollow,
                    modifier = Modifier.weight(1.08f),
                  )

                  Surface(
                    onClick = onMessageClick,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                      .weight(1f)
                      .height(40.dp)
                      .testTag("profile_message_button"),
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                      horizontalArrangement = Arrangement.Center,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      ProfilePaperPlaneSvgIcon(
                        tint = MaterialTheme.colorScheme.onSurface,
                        size = 16.dp,
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = stringResource(R.string.profile_action_message),
                        style = MaterialTheme.typography.labelLarge.copy(
                          fontSize = 13.5.sp,
                          fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                      )
                    }
                  }

                  Surface(
                    onClick = onSubscribeClick,
                    shape = RoundedCornerShape(8.dp),
                    color = accentCrimson,
                    modifier = Modifier
                      .weight(1.05f)
                      .height(40.dp)
                      .testTag("profile_subscribe_button"),
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                      horizontalArrangement = Arrangement.Center,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      ProfileSubscribeCrownSvgIcon(
                        tint = TelefamPureWhite,
                        size = 16.dp,
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = stringResource(R.string.profile_action_subscribe),
                        style = MaterialTheme.typography.labelLarge.copy(
                          fontSize = 13.5.sp,
                          fontWeight = FontWeight.SemiBold,
                        ),
                        color = TelefamPureWhite,
                        maxLines = 1,
                      )
                    }
                  }
                }
              } else {
                // Owner Action Buttons matching screenshot: [Pencil SVG] Edit profile  |  [Share SVG] Share profile
                Row(
                  modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Surface(
                    onClick = onOpenEditProfile,
                    shape = RoundedCornerShape(22.dp),
                    color = accentCrimson,
                    modifier = Modifier
                      .weight(1f)
                      .height(44.dp)
                      .testTag("profile_edit_button"),
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                      horizontalArrangement = Arrangement.Center,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      ProfileEditPencilSvgIcon(
                        tint = TelefamPureWhite,
                        size = 15.dp,
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = stringResource(R.string.profile_action_edit),
                        style = MaterialTheme.typography.labelLarge.copy(
                          fontSize = 14.sp,
                          fontWeight = FontWeight.Bold,
                        ),
                        color = TelefamPureWhite,
                        maxLines = 1,
                      )
                    }
                  }

                  Surface(
                    onClick = inviteFriendsShare,
                    shape = RoundedCornerShape(22.dp),
                    color = if (isDark) Color(0xFF171A21) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                      1.dp,
                      if (isDark) Color(0xFF282C36) else MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier
                      .weight(1f)
                      .height(44.dp)
                      .testTag("profile_share_button"),
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                      horizontalArrangement = Arrangement.Center,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      ProfileShareNodesSvgIcon(
                        tint = MaterialTheme.colorScheme.onSurface,
                        size = 15.dp,
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = stringResource(R.string.profile_action_share),
                        style = MaterialTheme.typography.labelLarge.copy(
                          fontSize = 14.sp,
                          fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                      )
                    }
                  }
                }
              }

              // Location & Bio
              if (profile.location.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center,
                  modifier = Modifier.padding(horizontal = 24.dp),
                ) {
                  ProfileLocationPinSvgIcon(
                    tint = accentCrimson,
                    size = 15.dp,
                  )
                  Spacer(modifier = Modifier.width(5.dp))
                  Text(
                    text = profile.location,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("profile_location"),
                  )
                }
              }

              if (profile.bio.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                SmartInteractiveText(
                  text = profile.bio,
                  source = "profile",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                  ),
                  color = MaterialTheme.colorScheme.onBackground,
                  linkColor = accentCrimson,
                  textAlign = TextAlign.Center,
                  showOtpPills = false,
                  modifier = Modifier
                    .widthIn(max = 360.dp)
                    .padding(horizontal = 24.dp)
                    .testTag("profile_bio"),
                )
              }

              Spacer(modifier = Modifier.height(16.dp))

              // Tabs Bar: 4 Tabs for Owner (Posts <count>, Saved, Reposts, Locked) or Posts for Visitor
              if (profile.isOwner) {
                OwnerProfileTabsRow(
                  activeTab = profile.activeOwnerTab,
                  postsCount = profile.posts.size,
                  accentCrimson = accentCrimson,
                  onSelectTab = onSelectOwnerTab,
                )
              } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(
                      modifier = Modifier.weight(0.45f),
                      horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                      Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                      ) {
                        ProfilePostsGridSvgIcon(
                          tint = MaterialTheme.colorScheme.onBackground,
                          size = 17.dp,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          text = stringResource(R.string.profile_posts_tab),
                          style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                          ),
                          color = MaterialTheme.colorScheme.onBackground,
                        )
                      }
                      Box(
                        modifier = Modifier
                          .fillMaxWidth()
                          .height(2.5.dp)
                          .background(accentCrimson),
                      )
                    }
                    Box(modifier = Modifier.weight(0.55f))
                  }
                  HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                  )
                }
              }

              // Filter & Sort Toolbar ("All posts v"   |   Sliders Icon)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Box {
                  Row(
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .clickable { showFilterMenu = true }
                      .padding(horizontal = 4.dp, vertical = 4.dp)
                      .testTag("profile_post_filter_button"),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    val filterLabel = when (profile.selectedFilter) {
                      ProfilePostFilter.ALL -> stringResource(R.string.profile_filter_all)
                      ProfilePostFilter.VIDEOS -> stringResource(R.string.profile_filter_videos)
                      ProfilePostFilter.PHOTOS -> stringResource(R.string.profile_filter_photos)
                      ProfilePostFilter.TEXT -> stringResource(R.string.profile_filter_text)
                    }
                    Text(
                      text = filterLabel,
                      style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                      ),
                      color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                      imageVector = Icons.Filled.KeyboardArrowDown,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onBackground,
                      modifier = Modifier.size(18.dp),
                    )
                  }

                  DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                  ) {
                    ProfilePostFilter.entries.forEach { option ->
                      DropdownMenuItem(
                        text = {
                          Text(
                            text = option.label,
                            fontWeight = if (option == profile.selectedFilter) FontWeight.Bold else FontWeight.Normal,
                            color = if (option == profile.selectedFilter) accentCrimson else MaterialTheme.colorScheme.onSurface,
                          )
                        },
                        onClick = {
                          showFilterMenu = false
                          onSelectFilter(option)
                        },
                      )
                    }
                  }
                }

                Box {
                  IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier
                      .size(36.dp)
                      .testTag("profile_post_sort_button"),
                  ) {
                    ProfileFilterSlidersSvgIcon(
                      tint = MaterialTheme.colorScheme.onBackground,
                      size = 18.dp,
                    )
                  }

                  DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                  ) {
                    ProfilePostSort.entries.forEach { sortOpt ->
                      DropdownMenuItem(
                        text = {
                          Text(
                            text = sortOpt.label,
                            fontWeight = if (sortOpt == profile.selectedSort) FontWeight.Bold else FontWeight.Normal,
                            color = if (sortOpt == profile.selectedSort) accentCrimson else MaterialTheme.colorScheme.onSurface,
                          )
                        },
                        onClick = {
                          showSortMenu = false
                          onSelectSort(sortOpt)
                        },
                      )
                    }
                  }
                }
              }
            }
          }

          if (profile.isOwner && profile.activeOwnerTab == OwnerProfileTab.SAVED && !profile.isBlocked) {
            item(
              key = "profile_saved_subtab_selector",
              span = { GridItemSpan(maxLineSpan) },
            ) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 6.dp),
              ) {
                listOf(
                  0 to stringResource(R.string.profile_saved_posts),
                  1 to stringResource(R.string.profile_saved_links),
                ).forEach { (idx, title) ->
                  val selected = savedSubTab == idx
                  Surface(
                    onClick = { savedSubTab = idx },
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) accentCrimson else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.testTag(if (idx == 0) "profile_saved_subtab_posts" else "profile_saved_subtab_links"),
                  ) {
                    Text(
                      text = title,
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.SemiBold,
                      color = if (selected) TelefamPureWhite else MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                  }
                }
              }
            }
          }

          if (profile.isOwner && profile.activeOwnerTab == OwnerProfileTab.SAVED && savedSubTab == 1 && !profile.isBlocked) {
            item(
              key = "profile_saved_links_pane",
              span = { GridItemSpan(maxLineSpan) },
            ) {
              val q = savedLinksSearchQuery.trim().lowercase()
              val filteredLinks = remember(savedLinks, q) {
                if (q.isEmpty()) savedLinks
                else savedLinks.filter {
                  it.title.lowercase().contains(q) ||
                    it.domain.lowercase().contains(q) ||
                    it.normalizedUrl.lowercase().contains(q)
                }
              }
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(460.dp),
              ) {
                SavedLinksListPane(
                  links = filteredLinks,
                  searchQuery = savedLinksSearchQuery,
                  onSearchQueryChanged = onSavedLinksSearchQueryChanged,
                  onOpenLink = onOpenSavedLink,
                  onDeleteLink = onDeleteSavedLink,
                )
              }
            }
          } else if (profile.isLoading && filteredPosts.isEmpty()) {
            item(
              key = "profile_loading",
              span = { GridItemSpan(maxLineSpan) },
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
              ) {
                CircularProgressIndicator(
                  color = accentCrimson,
                  strokeWidth = 2.5.dp,
                  modifier = Modifier.size(28.dp),
                )
              }
            }
          } else if (isPrivateRestricted) {
            item(
              key = "profile_private_account",
              span = { GridItemSpan(maxLineSpan) },
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 44.dp, horizontal = 24.dp)
                  .testTag("profile_private_account_notice"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(accentCrimson.copy(alpha = 0.12f)),
                  contentAlignment = Alignment.Center,
                ) {
                  ProfileLockSvgIcon(tint = accentCrimson, size = 24.dp)
                }
                Text(
                  text = stringResource(R.string.profile_private_account_title),
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                  text = stringResource(R.string.profile_private_account_subtitle),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center,
                )
              }
            }
          } else if (!profile.isBlocked && filteredPosts.isEmpty()) {
            item(
              key = "profile_empty_posts",
              span = { GridItemSpan(maxLineSpan) },
            ) {
              val emptyRes = when (if (profile.isOwner) profile.activeOwnerTab else OwnerProfileTab.POSTS) {
                OwnerProfileTab.POSTS -> R.string.profile_posts_empty
                OwnerProfileTab.SAVED -> R.string.profile_saved_empty
                OwnerProfileTab.REPOSTS -> R.string.profile_reposts_empty
                OwnerProfileTab.LOCKED -> R.string.profile_locked_empty
              }
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 42.dp, horizontal = 24.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = stringResource(emptyRes),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center,
                )
              }
            }
          } else if (!profile.isBlocked) {
            itemsIndexed(
              items = filteredPosts,
              key = { _, item -> "${profile.activeOwnerTab.name}_${item.postId}" },
            ) { idx, post ->
              ProfilePostGridCard(
                item = post,
                showPrivacyBadge = profile.isOwner && post.privacy != PostPrivacy.PUBLIC,
                onClick = { onOpenPostAtIndex(filteredPosts, idx) },
              )
            }
          }
        }
      }
    }
  }

  // Clickable Followers / Following Modal Bottom Sheet
  if (profile.activeConnectionsType != null) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = onDismissConnections,
      sheetState = sheetState,
      containerColor = MaterialTheme.colorScheme.surface,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .height(460.dp),
      ) {
        Text(
          text = if (profile.activeConnectionsType == "followers") {
            stringResource(R.string.profile_stat_followers)
          } else {
            stringResource(R.string.profile_stat_following)
          },
          style = MaterialTheme.typography.titleMedium.copy(
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
          ),
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .testTag("profile_connections_sheet_title"),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (profile.isLoadingConnections && profile.connections.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = accentCrimson, strokeWidth = 2.5.dp)
          }
        } else if (profile.connections.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
              text = stringResource(R.string.profile_connections_empty),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .testTag("profile_connections_list"),
            contentPadding = PaddingValues(vertical = 8.dp),
          ) {
            items(profile.connections, key = { it.userId }) { conn ->
              ProfileConnectionRow(
                item = conn,
                onClickRow = {
                  onDismissConnections()
                  onOpenUserProfile(conn.userId)
                },
                onToggleFollow = { onToggleConnectionFollow(conn.userId) },
              )
            }
            if (profile.connectionsHasMore) {
              item(key = "conn_load_more") {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  TextButton(onClick = onLoadMoreConnections) {
                    Text(stringResource(R.string.nav_contacts), color = accentCrimson)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Report Category Sub-Menu Dialog -> Details & Submit Dialog
  if (showReportCategoryDialog) {
    AlertDialog(
      onDismissRequest = { showReportCategoryDialog = false },
      title = {
        Text(
          text = stringResource(R.string.profile_report_title),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          ProfileReportOptions.categories.forEach { cat ->
            OutlinedButton(
              onClick = {
                showReportCategoryDialog = false
                selectedReportCategory = cat
                reportDetails = ""
              },
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_report_cat_${cat.code}"),
            ) {
              Text(
                text = cat.label,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showReportCategoryDialog = false }) {
          Text(stringResource(R.string.profile_crop_cancel))
        }
      },
    )
  }

  selectedReportCategory?.let { cat ->
    AlertDialog(
      onDismissRequest = { selectedReportCategory = null },
      title = {
        Text(
          text = cat.label,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
      },
      text = {
        OutlinedTextField(
          value = reportDetails,
          onValueChange = { reportDetails = it.take(300) },
          placeholder = { Text(stringResource(R.string.profile_report_details_hint)) },
          minLines = 3,
          maxLines = 5,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_report_details_input"),
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onReportUser(cat.label, reportDetails.trim())
            selectedReportCategory = null
            reportDetails = ""
          },
          colors = ButtonDefaults.buttonColors(containerColor = accentCrimson, contentColor = TelefamPureWhite),
          modifier = Modifier.testTag("profile_report_submit_button"),
        ) {
          Text(stringResource(R.string.profile_report_submit))
        }
      },
      dismissButton = {
        TextButton(onClick = { selectedReportCategory = null }) {
          Text(stringResource(R.string.profile_crop_cancel))
        }
      },
    )
  }
}

@Composable
private fun VisitorMoreMenuButton(
  expanded: Boolean,
  isBlocked: Boolean,
  isMuted: Boolean,
  accentCrimson: Color,
  iconTint: Color,
  onOpen: () -> Unit,
  onDismiss: () -> Unit,
  onCopyLink: () -> Unit,
  onInviteFriends: () -> Unit,
  onMuteToggle: () -> Unit,
  onBlockToggle: () -> Unit,
  onReport: () -> Unit,
) {
  Box {
    IconButton(
      onClick = onOpen,
      modifier = Modifier
        .minimumInteractiveComponentSize()
        .testTag("profile_more_button"),
    ) {
      ProfileThreeDotsSvgIcon(tint = iconTint, size = 24.dp)
    }

    DropdownMenu(
      expanded = expanded,
      onDismissRequest = onDismiss,
      containerColor = MaterialTheme.colorScheme.surface,
    ) {
      DropdownMenuItem(
        text = { Text(stringResource(R.string.profile_action_copy_link)) },
        onClick = {
          onDismiss()
          onCopyLink()
        },
        modifier = Modifier.testTag("profile_menu_copy_link"),
      )
      DropdownMenuItem(
        text = { Text(stringResource(R.string.profile_action_invite_friends)) },
        onClick = {
          onDismiss()
          onInviteFriends()
        },
        modifier = Modifier.testTag("profile_menu_invite"),
      )
      DropdownMenuItem(
        text = {
          Text(
            text = if (isMuted) stringResource(R.string.profile_action_unmute) else stringResource(R.string.profile_action_mute),
          )
        },
        onClick = {
          onDismiss()
          onMuteToggle()
        },
        modifier = Modifier.testTag("profile_menu_mute_toggle"),
      )
      DropdownMenuItem(
        text = {
          Text(
            text = if (isBlocked) stringResource(R.string.profile_action_unblock) else stringResource(R.string.profile_action_block),
            color = if (!isBlocked) accentCrimson else MaterialTheme.colorScheme.onSurface,
          )
        },
        onClick = {
          onDismiss()
          onBlockToggle()
        },
        modifier = Modifier.testTag("profile_menu_block_toggle"),
      )
      DropdownMenuItem(
        text = {
          Text(
            text = stringResource(R.string.profile_action_report),
            color = accentCrimson,
          )
        },
        onClick = {
          onDismiss()
          onReport()
        },
        modifier = Modifier.testTag("profile_menu_report"),
      )
    }
  }
}

@Composable
private fun ProfileFadingHeroWithAvatar(
  profile: UserProfileScreenState,
  accentCrimson: Color,
  onBack: () -> Unit,
  onSubscribeClick: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenMenu: () -> Unit,
  onOpenBackgrounds: () -> Unit,
  onInviteBadgeClick: () -> Unit,
  showMoreMenu: Boolean,
  onOpenMoreMenu: () -> Unit,
  onDismissMoreMenu: () -> Unit,
  onCopyLink: () -> Unit,
  onInviteFriends: () -> Unit,
  onMuteToggle: () -> Unit,
  onBlockToggle: () -> Unit,
  onReport: () -> Unit,
) {
  val bgColor = MaterialTheme.colorScheme.background
  val backgrounds = profile.backgrounds
  var slideIndex by remember(backgrounds.size) { mutableIntStateOf(0) }

  LaunchedEffect(backgrounds) {
    slideIndex = 0
    if (backgrounds.size > 1) {
      while (true) {
        delay(5_000L)
        slideIndex = (slideIndex + 1) % backgrounds.size
      }
    }
  }

  val activeBg: ProfileBackgroundItem? = backgrounds.getOrNull(slideIndex.coerceIn(0, (backgrounds.size - 1).coerceAtLeast(0)))

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("profile_hero_banner"),
  ) {
    // Full-bleed Hero Media Box (232.dp) fading smoothly into background
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(232.dp)
        .align(Alignment.TopCenter),
    ) {
      if (backgrounds.isNotEmpty() && activeBg != null) {
        AnimatedContent(
          targetState = activeBg,
          transitionSpec = {
            fadeIn(animationSpec = tween(900)) togetherWith fadeOut(animationSpec = tween(900))
          },
          label = "profile_hero_crossfade",
          modifier = Modifier.fillMaxSize(),
        ) { item ->
          val localBmp = remember(item.localFilePath) {
            if (item.localFilePath.isNotBlank() && File(item.localFilePath).exists()) {
              BitmapFactory.decodeFile(item.localFilePath)?.asImageBitmap()
            } else {
              null
            }
          }
          if (localBmp != null) {
            Image(
              bitmap = localBmp,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            AsyncImage(
              model = item.url,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
          }
        }
      } else {
        // Sleek atmospheric backdrop when no background image has been uploaded yet
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              Brush.linearGradient(
                colors = listOf(
                  Color(0xFF281822),
                  Color(0xFF141822),
                  Color(0xFF0E1016),
                ),
              ),
            ),
        )
      }

      // Top dark scrim for crisp white overlay controls & display name
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(102.dp)
          .align(Alignment.TopCenter)
          .background(
            Brush.verticalGradient(
              colors = listOf(Color.Black.copy(alpha = 0.62f), Color.Transparent),
            ),
          ),
      )

      // Smooth bottom fade into screen background
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(126.dp)
          .align(Alignment.BottomCenter)
          .background(
            Brush.verticalGradient(
              colorStops = arrayOf(
                0.0f to Color.Transparent,
                0.45f to bgColor.copy(alpha = 0.55f),
                0.85f to bgColor.copy(alpha = 0.94f),
                1.0f to bgColor,
              ),
            ),
          ),
      )

      // Top Overlay Controls Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .align(Alignment.TopCenter),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        HeroCircleActionButton(
          onClick = onBack,
          testTag = "profile_back_button",
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.cd_back),
            tint = Color.White,
            modifier = Modifier.size(20.dp),
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
          text = profile.fullName.ifBlank { "@${profile.username.ifBlank { profile.userId }}" },
          style = MaterialTheme.typography.titleMedium.copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
          ),
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = if (profile.isOwner) TextAlign.Center else TextAlign.Center,
          modifier = Modifier
            .weight(1f)
            .testTag("profile_display_name"),
        )

        if (profile.isOwner) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            HeroCircleActionButton(
              onClick = onSubscribeClick,
              testTag = "profile_owner_subscribers_button",
            ) {
              ProfileSubscribeCrownSvgIcon(
                tint = Color.White,
                size = 18.dp,
              )
            }
            HeroCircleActionButton(
              onClick = onOpenSettings,
              testTag = "profile_owner_settings_button",
            ) {
              ProfileSettingsGearSvgIcon(
                tint = Color.White,
                size = 18.dp,
              )
            }
            HeroCircleActionButton(
              onClick = onOpenMenu,
              testTag = "profile_owner_menu_button",
            ) {
              ProfileThreeDashesMenuSvgIcon(
                tint = Color.White,
                size = 18.dp,
              )
            }
          }
        } else {
          Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onInviteBadgeClick)
                .testTag("profile_invite_badge"),
              contentAlignment = Alignment.Center,
            ) {
              ProfileInviteBadgeSvgIcon(
                badgeColor = accentCrimson,
                modifier = Modifier.size(width = 30.dp, height = 27.dp),
              )
            }
            VisitorMoreMenuButton(
              expanded = showMoreMenu,
              isBlocked = profile.isBlocked,
              isMuted = profile.isMuted,
              accentCrimson = accentCrimson,
              iconTint = Color.White,
              onOpen = onOpenMoreMenu,
              onDismiss = onDismissMoreMenu,
              onCopyLink = onCopyLink,
              onInviteFriends = onInviteFriends,
              onMuteToggle = onMuteToggle,
              onBlockToggle = onBlockToggle,
              onReport = onReport,
            )
          }
        }
      }

      // Slide indicator dots (subtle bottom-start inside hero when > 1 slide)
      if (backgrounds.size > 1) {
        Row(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(top = 148.dp, start = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          backgrounds.forEachIndexed { idx, _ ->
            Box(
              modifier = Modifier
                .size(width = if (idx == slideIndex) 16.dp else 6.dp, height = 6.dp)
                .clip(CircleShape)
                .background(
                  if (idx == slideIndex) Color.White else Color.White.copy(alpha = 0.45f),
                ),
            )
          }
        }
      }

      // Owner-only "[Camera SVG] Edit background" pill sitting above the right edge of avatar
      if (profile.isOwner) {
        Surface(
          onClick = onOpenBackgrounds,
          shape = RoundedCornerShape(20.dp),
          color = Color(0xCC16181F),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
          modifier = Modifier
            .align(Alignment.TopEnd)
            .statusBarsPadding()
            .padding(top = 98.dp, end = 14.dp)
            .height(34.dp)
            .testTag("profile_edit_background_button"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            ProfileCameraSvgIcon(tint = Color.White, size = 15.dp)
            Text(
              text = stringResource(R.string.profile_edit_background),
              color = Color.White,
              style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
              ),
              maxLines = 1,
            )
          }
        }
      }
    }

    // Centered overlapping 94.dp Circular Avatar positioned smoothly over hero bottom edge
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 172.dp),
      contentAlignment = Alignment.TopCenter,
    ) {
      ProfileGradientAvatar(
        avatarUrl = profile.avatarUrl,
        fullName = profile.fullName,
        username = profile.username,
      )
    }
  }
}

@Composable
private fun HeroCircleActionButton(
  onClick: () -> Unit,
  testTag: String,
  content: @Composable () -> Unit,
) {
  Box(
    modifier = Modifier
      .size(38.dp)
      .clip(CircleShape)
      .background(Color.Black.copy(alpha = 0.42f))
      .clickable(onClick = onClick)
      .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    content()
  }
}

@Composable
private fun OwnerProfileTabsRow(
  activeTab: OwnerProfileTab,
  postsCount: Int,
  accentCrimson: Color,
  onSelectTab: (OwnerProfileTab) -> Unit,
) {
  val tabs = listOf(
    OwnerProfileTab.POSTS to "${stringResource(R.string.profile_tab_posts)} $postsCount",
    OwnerProfileTab.SAVED to stringResource(R.string.profile_tab_saved),
    OwnerProfileTab.REPOSTS to stringResource(R.string.profile_tab_reposts),
    OwnerProfileTab.LOCKED to stringResource(R.string.profile_tab_locked),
  )

  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      tabs.forEach { (tab, label) ->
        val selected = tab == activeTab
        val ink = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
        Column(
          modifier = Modifier
            .weight(1f)
            .clickable { onSelectTab(tab) }
            .testTag("profile_tab_${tab.name.lowercase()}"),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            when (tab) {
              OwnerProfileTab.POSTS -> ProfilePostsGridSvgIcon(tint = ink, size = 15.dp)
              OwnerProfileTab.SAVED -> ProfileBookmarkSvgIcon(tint = ink, size = 15.dp)
              OwnerProfileTab.REPOSTS -> ProfileRepostSvgIcon(tint = ink, size = 15.dp)
              OwnerProfileTab.LOCKED -> ProfileLockSvgIcon(tint = ink, size = 15.dp)
            }
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = label,
              style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
              ),
              color = ink,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Box(
            modifier = Modifier
              .fillMaxWidth(0.75f)
              .height(2.5.dp)
              .background(if (selected) accentCrimson else Color.Transparent),
          )
        }
      }
    }
    HorizontalDivider(
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
    )
  }
}

@Composable
private fun ProfileGradientAvatar(
  avatarUrl: String?,
  fullName: String,
  username: String,
) {
  val ringBrush = remember {
    Brush.linearGradient(
      colors = listOf(
        Color(0xFFD946EF),
        TelefamCrimson,
        Color(0xFFF97316),
      ),
    )
  }
  val localBitmap = remember(avatarUrl) {
    if (!avatarUrl.isNullOrBlank() && !avatarUrl.startsWith("http") && File(avatarUrl).exists()) {
      BitmapFactory.decodeFile(avatarUrl)?.asImageBitmap()
    } else {
      null
    }
  }
  val initials = remember(fullName, username) {
    TelefamContactsRules.initialsFor(fullName, username)
  }

  Box(
    modifier = Modifier
      .size(94.dp)
      .clip(CircleShape)
      .background(MaterialTheme.colorScheme.background)
      .border(width = 2.6.dp, brush = ringBrush, shape = CircleShape)
      .padding(4.dp)
      .clip(CircleShape)
      .background(MaterialTheme.colorScheme.surfaceVariant),
    contentAlignment = Alignment.Center,
  ) {
    if (localBitmap != null) {
      Image(
        bitmap = localBitmap,
        contentDescription = stringResource(R.string.cd_profile_avatar),
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      Text(
        text = initials,
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        color = TelefamCrimson,
      )
      if (!avatarUrl.isNullOrBlank()) {
        AsyncImage(
          model = avatarUrl,
          contentDescription = stringResource(R.string.cd_profile_avatar),
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }
    }
  }
}

@Composable
private fun ProfileStatColumn(
  countText: String,
  label: String,
  onClick: (() -> Unit)?,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
      .padding(vertical = 4.dp, horizontal = 4.dp)
      .testTag(testTag),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = countText,
      style = MaterialTheme.typography.titleMedium.copy(
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
      ),
      color = MaterialTheme.colorScheme.onBackground,
      maxLines = 1,
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
    )
  }
}

@Composable
private fun ProfileRelationshipActionButton(
  state: ContactRelationshipState,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = LocalTelefamDarkTheme.current
  val accentCrimson = if (isDark) TelefamCrimsonBright else TelefamCrimson
  val isFilled = state == ContactRelationshipState.FOLLOW || state == ContactRelationshipState.FOLLOW_BACK
  val containerColor = when (state) {
    ContactRelationshipState.FOLLOW, ContactRelationshipState.FOLLOW_BACK -> accentCrimson
    ContactRelationshipState.FOLLOWING -> MaterialTheme.colorScheme.surface
    ContactRelationshipState.FRIENDS -> if (isDark) TelefamDarkBlush else TelefamBlushWave
  }
  val contentColor = when (state) {
    ContactRelationshipState.FOLLOW, ContactRelationshipState.FOLLOW_BACK -> TelefamPureWhite
    ContactRelationshipState.FOLLOWING -> MaterialTheme.colorScheme.onSurface
    ContactRelationshipState.FRIENDS -> accentCrimson
  }
  val labelRes = when (state) {
    ContactRelationshipState.FOLLOW -> R.string.contacts_action_follow
    ContactRelationshipState.FOLLOWING -> R.string.contacts_action_following
    ContactRelationshipState.FOLLOW_BACK -> R.string.contacts_action_follow_back
    ContactRelationshipState.FRIENDS -> R.string.contacts_action_friends
  }

  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(8.dp),
    color = containerColor,
    border = if (isFilled) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier
      .height(40.dp)
      .testTag("profile_follow_button"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 8.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (isFilled) {
        ProfilePersonAddSvgIcon(tint = contentColor, size = 16.dp)
      } else {
        ProfileCheckSvgIcon(tint = contentColor, size = 16.dp)
      }
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.labelLarge.copy(
          fontSize = 13.5.sp,
          fontWeight = FontWeight.SemiBold,
        ),
        color = contentColor,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun ProfilePostGridCard(
  item: FeedUiItem,
  showPrivacyBadge: Boolean = false,
  onClick: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .aspectRatio(0.75f)
      .clip(RoundedCornerShape(3.dp))
      .background(Color(0xFF141820))
      .clickable(onClick = onClick)
      .testTag("profile_post_tile_${item.postId}"),
  ) {
    when (item.mode) {
      PostMode.VIDEO -> {
        val thumb = item.thumbnailUrl
        if (thumb.isNotBlank()) {
          AsyncImage(
            model = thumb,
            contentDescription = item.caption.ifBlank { item.authorFullName },
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
      PostMode.PHOTOS -> {
        val firstPhoto = item.photoUrls.firstOrNull().orEmpty()
        if (firstPhoto.isNotBlank()) {
          AsyncImage(
            model = firstPhoto,
            contentDescription = item.caption.ifBlank { item.authorFullName },
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        }
        if (item.photoUrls.size > 1) {
          Box(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(6.dp),
          ) {
            ProfilePhotoStackSvgIcon(tint = Color.White, size = 15.dp)
          }
        }
      }
      PostMode.TEXT -> {
        val theme = TelefamPostRules.findTheme(item.themeId)
        val ink = if (theme.isLightInk) Color.White else Color(0xFF1C1F26)
        PostThemeCanvasBox(theme = theme, modifier = Modifier.fillMaxSize()) {
          Text(
            text = item.textContent,
            style = postCardTextStyle(item.fontOption, ink).copy(
              fontSize = 12.sp,
              lineHeight = 15.sp,
            ),
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
              .align(Alignment.Center)
              .padding(8.dp),
          )
        }
      }
    }

    if (showPrivacyBadge) {
      Box(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(6.dp)
          .size(22.dp)
          .clip(CircleShape)
          .background(Color.Black.copy(alpha = 0.58f)),
        contentAlignment = Alignment.Center,
      ) {
        if (item.privacy == PostPrivacy.PRIVATE) {
          ProfileLockSvgIcon(tint = Color.White, size = 12.dp)
        } else {
          ProfileFriendsSvgIcon(tint = Color.White, size = 12.dp)
        }
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(44.dp)
        .align(Alignment.BottomCenter)
        .background(
          Brush.verticalGradient(
            listOf(Color.Transparent, Color.Black.copy(alpha = 0.62f)),
          ),
        ),
    )

    Row(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(horizontal = 7.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ProfilePlayOutlineSvgIcon(tint = Color.White, size = 13.dp)
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = formatProfileMetricCount(item.viewsCount),
        color = Color.White,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
private fun ProfileConnectionRow(
  item: ContactUiItem,
  onClickRow: () -> Unit,
  onToggleFollow: () -> Unit,
) {
  val isDark = LocalTelefamDarkTheme.current
  val accentCrimson = if (isDark) TelefamCrimsonBright else TelefamCrimson
  val bitmap = remember(item.avatarLocalPath) {
    val p = item.avatarLocalPath
    if (!p.isNullOrBlank() && !p.startsWith("http") && File(p).exists()) {
      BitmapFactory.decodeFile(p)?.asImageBitmap()
    } else {
      null
    }
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClickRow)
      .padding(horizontal = 18.dp, vertical = 10.dp)
      .testTag("profile_connection_row_${item.userId}"),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .size(44.dp)
        .clip(CircleShape)
        .background(if (isDark) TelefamDarkBlush else TelefamBlushWave),
      contentAlignment = Alignment.Center,
    ) {
      if (bitmap != null) {
        Image(
          bitmap = bitmap,
          contentDescription = item.fullName,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      } else if (!item.avatarLocalPath.isNullOrBlank() && item.avatarLocalPath.startsWith("http")) {
        AsyncImage(
          model = item.avatarLocalPath,
          contentDescription = item.fullName,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        Text(
          text = item.initials,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = accentCrimson,
        )
      }
    }

    Spacer(modifier = Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = item.fullName.ifBlank { item.username },
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = if (item.countryDisplay.isNotBlank()) "@${item.username} · ${item.countryDisplay}" else "@${item.username}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Spacer(modifier = Modifier.width(10.dp))

    val isFilled = item.relationshipState == ContactRelationshipState.FOLLOW ||
      item.relationshipState == ContactRelationshipState.FOLLOW_BACK
    val labelRes = when (item.relationshipState) {
      ContactRelationshipState.FOLLOW -> R.string.contacts_action_follow
      ContactRelationshipState.FOLLOWING -> R.string.contacts_action_following
      ContactRelationshipState.FOLLOW_BACK -> R.string.contacts_action_follow_back
      ContactRelationshipState.FRIENDS -> R.string.contacts_action_friends
    }
    Surface(
      onClick = onToggleFollow,
      shape = RoundedCornerShape(18.dp),
      color = if (isFilled) accentCrimson else MaterialTheme.colorScheme.surfaceVariant,
      border = if (isFilled) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier = Modifier
        .height(34.dp)
        .testTag("profile_connection_follow_${item.userId}"),
    ) {
      Box(
        modifier = Modifier.padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = stringResource(labelRes),
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          color = if (isFilled) TelefamPureWhite else MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

// --- Custom Crisp Canvas SVG Vector Icons ---

@Composable
private fun ProfileInviteBadgeSvgIcon(
  badgeColor: Color,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    drawRoundRect(
      color = badgeColor,
      topLeft = Offset.Zero,
      size = Size(w, h * 0.90f),
      cornerRadius = CornerRadius(w * 0.24f, w * 0.24f),
    )
    val bubble = Path().apply {
      moveTo(w * 0.22f, h * 0.20f)
      lineTo(w * 0.78f, h * 0.20f)
      quadraticTo(w * 0.84f, h * 0.20f, w * 0.84f, h * 0.28f)
      lineTo(w * 0.84f, h * 0.62f)
      quadraticTo(w * 0.84f, h * 0.70f, w * 0.78f, h * 0.70f)
      lineTo(w * 0.42f, h * 0.70f)
      lineTo(w * 0.28f, h * 0.82f)
      lineTo(w * 0.28f, h * 0.70f)
      lineTo(w * 0.22f, h * 0.70f)
      quadraticTo(w * 0.16f, h * 0.70f, w * 0.16f, h * 0.62f)
      lineTo(w * 0.16f, h * 0.28f)
      quadraticTo(w * 0.16f, h * 0.20f, w * 0.22f, h * 0.20f)
      close()
    }
    drawPath(bubble, color = Color.White)

    val strokeW = w * 0.11f
    drawLine(
      color = badgeColor,
      start = Offset(w * 0.36f, h * 0.34f),
      end = Offset(w * 0.64f, h * 0.34f),
      strokeWidth = strokeW,
      cap = StrokeCap.Round,
    )
    drawLine(
      color = badgeColor,
      start = Offset(w * 0.50f, h * 0.34f),
      end = Offset(w * 0.50f, h * 0.59f),
      strokeWidth = strokeW,
      cap = StrokeCap.Round,
    )
  }
}

@Composable
private fun ProfileThreeDotsSvgIcon(tint: Color, size: Dp = 24.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val cy = this.size.height / 2f
    val r = w * 0.085f
    drawCircle(tint, radius = r, center = Offset(w * 0.22f, cy))
    drawCircle(tint, radius = r, center = Offset(w * 0.50f, cy))
    drawCircle(tint, radius = r, center = Offset(w * 0.78f, cy))
  }
}

@Composable
private fun ProfileThreeDashesMenuSvgIcon(tint: Color, size: Dp = 18.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = w * 0.105f
    drawLine(tint, Offset(w * 0.18f, h * 0.28f), Offset(w * 0.82f, h * 0.28f), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.18f, h * 0.50f), Offset(w * 0.82f, h * 0.50f), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.18f, h * 0.72f), Offset(w * 0.82f, h * 0.72f), strokeWidth = stroke, cap = StrokeCap.Round)
  }
}

@Composable
private fun ProfileSettingsGearSvgIcon(tint: Color, size: Dp = 18.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val cx = w / 2f
    val cy = h / 2f
    val stroke = w * 0.095f
    drawCircle(tint, radius = w * 0.17f, center = Offset(cx, cy), style = Stroke(width = stroke))
    drawCircle(tint, radius = w * 0.30f, center = Offset(cx, cy), style = Stroke(width = stroke * 0.9f))
    val toothLen = w * 0.10f
    for (i in 0 until 8) {
      val angle = Math.toRadians((i * 45).toDouble())
      val cos = kotlin.math.cos(angle).toFloat()
      val sin = kotlin.math.sin(angle).toFloat()
      val rIn = w * 0.31f
      val rOut = rIn + toothLen
      drawLine(
        color = tint,
        start = Offset(cx + cos * rIn, cy + sin * rIn),
        end = Offset(cx + cos * rOut, cy + sin * rOut),
        strokeWidth = stroke * 1.1f,
        cap = StrokeCap.Round,
      )
    }
  }
}

@Composable
private fun ProfileCameraSvgIcon(tint: Color, size: Dp = 15.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = Stroke(width = w * 0.10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val body = Path().apply {
      moveTo(w * 0.12f, h * 0.32f)
      lineTo(w * 0.28f, h * 0.32f)
      lineTo(w * 0.36f, h * 0.20f)
      lineTo(w * 0.64f, h * 0.20f)
      lineTo(w * 0.72f, h * 0.32f)
      lineTo(w * 0.88f, h * 0.32f)
      lineTo(w * 0.88f, h * 0.80f)
      lineTo(w * 0.12f, h * 0.80f)
      close()
    }
    drawPath(body, color = tint, style = stroke)
    drawCircle(tint, radius = w * 0.15f, center = Offset(w * 0.50f, h * 0.56f), style = stroke)
  }
}

@Composable
private fun ProfileEditPencilSvgIcon(tint: Color, size: Dp = 15.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val path = Path().apply {
      moveTo(w * 0.16f, h * 0.84f)
      lineTo(w * 0.22f, h * 0.62f)
      lineTo(w * 0.72f, h * 0.14f)
      lineTo(w * 0.86f, h * 0.28f)
      lineTo(w * 0.36f, h * 0.78f)
      close()
    }
    drawPath(path, color = tint, style = stroke)
  }
}

@Composable
private fun ProfileShareNodesSvgIcon(tint: Color, size: Dp = 15.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = w * 0.095f
    val r = w * 0.11f
    val left = Offset(w * 0.22f, h * 0.50f)
    val top = Offset(w * 0.76f, h * 0.22f)
    val bottom = Offset(w * 0.76f, h * 0.78f)
    drawLine(tint, left, top, strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(tint, left, bottom, strokeWidth = stroke, cap = StrokeCap.Round)
    drawCircle(tint, radius = r, center = left, style = Stroke(width = stroke))
    drawCircle(tint, radius = r, center = top, style = Stroke(width = stroke))
    drawCircle(tint, radius = r, center = bottom, style = Stroke(width = stroke))
  }
}

@Composable
private fun ProfileBookmarkSvgIcon(tint: Color, size: Dp = 15.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = Stroke(width = w * 0.105f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val p = Path().apply {
      moveTo(w * 0.24f, h * 0.14f)
      lineTo(w * 0.76f, h * 0.14f)
      lineTo(w * 0.76f, h * 0.86f)
      lineTo(w * 0.50f, h * 0.66f)
      lineTo(w * 0.24f, h * 0.86f)
      close()
    }
    drawPath(p, color = tint, style = stroke)
  }
}

@Composable
private fun ProfileRepostSvgIcon(tint: Color, size: Dp = 15.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = w * 0.10f
    // Top loop -> right
    drawLine(tint, Offset(w * 0.20f, h * 0.34f), Offset(w * 0.76f, h * 0.34f), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.62f, h * 0.20f), Offset(w * 0.78f, h * 0.34f), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.62f, h * 0.48f), Offset(w * 0.78f, h * 0.34f), strokeWidth = stroke, cap = StrokeCap.Round)
    // Bottom loop -> left
    drawLine(tint, Offset(w * 0.80f, h * 0.68f), Offset(w * 0.24f, h * 0.68f), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.38f, h * 0.54f), Offset(w * 0.22f, h * 0.68f), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.38f, h * 0.82f), Offset(w * 0.22f, h * 0.68f), strokeWidth = stroke, cap = StrokeCap.Round)
  }
}

@Composable
private fun ProfileLockSvgIcon(tint: Color, size: Dp = 15.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = Stroke(width = w * 0.105f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawRoundRect(
      color = tint,
      topLeft = Offset(w * 0.20f, h * 0.44f),
      size = Size(w * 0.60f, h * 0.44f),
      cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
      style = stroke,
    )
    val shackle = Path().apply {
      moveTo(w * 0.32f, h * 0.44f)
      lineTo(w * 0.32f, h * 0.30f)
      quadraticTo(w * 0.32f, h * 0.14f, w * 0.50f, h * 0.14f)
      quadraticTo(w * 0.68f, h * 0.14f, w * 0.68f, h * 0.30f)
      lineTo(w * 0.68f, h * 0.44f)
    }
    drawPath(shackle, color = tint, style = stroke)
  }
}

@Composable
private fun ProfileFriendsSvgIcon(tint: Color, size: Dp = 12.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = w * 0.11f
    drawCircle(tint, radius = w * 0.16f, center = Offset(w * 0.36f, h * 0.34f), style = Stroke(width = stroke))
    drawCircle(tint, radius = w * 0.14f, center = Offset(w * 0.70f, h * 0.38f), style = Stroke(width = stroke))
    val p = Path().apply {
      moveTo(w * 0.12f, h * 0.82f)
      quadraticTo(w * 0.36f, h * 0.58f, w * 0.60f, h * 0.82f)
    }
    drawPath(p, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
  }
}

@Composable
private fun ProfilePersonAddSvgIcon(tint: Color, size: Dp = 16.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = w * 0.10f
    drawCircle(
      color = tint,
      radius = w * 0.17f,
      center = Offset(w * 0.40f, h * 0.32f),
      style = Stroke(width = stroke),
    )
    val shoulders = Path().apply {
      moveTo(w * 0.14f, h * 0.82f)
      quadraticTo(w * 0.18f, h * 0.58f, w * 0.40f, h * 0.58f)
      quadraticTo(w * 0.62f, h * 0.58f, w * 0.66f, h * 0.82f)
    }
    drawPath(shoulders, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
    drawLine(tint, Offset(w * 0.78f, h * 0.36f), Offset(w * 0.78f, h * 0.60f), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.66f, h * 0.48f), Offset(w * 0.90f, h * 0.48f), strokeWidth = stroke, cap = StrokeCap.Round)
  }
}

@Composable
private fun ProfileCheckSvgIcon(tint: Color, size: Dp = 16.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val path = Path().apply {
      moveTo(w * 0.20f, h * 0.52f)
      lineTo(w * 0.42f, h * 0.74f)
      lineTo(w * 0.82f, h * 0.28f)
    }
    drawPath(path, color = tint, style = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round))
  }
}

@Composable
private fun ProfilePaperPlaneSvgIcon(tint: Color, size: Dp = 16.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = Stroke(width = w * 0.10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val plane = Path().apply {
      moveTo(w * 0.88f, h * 0.14f)
      lineTo(w * 0.12f, h * 0.46f)
      lineTo(w * 0.46f, h * 0.58f)
      lineTo(w * 0.60f, h * 0.88f)
      close()
    }
    drawPath(plane, color = tint, style = stroke)
    drawLine(tint, Offset(w * 0.46f, h * 0.58f), Offset(w * 0.86f, h * 0.16f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
  }
}

@Composable
private fun ProfileSubscribeCrownSvgIcon(tint: Color, size: Dp = 16.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = Stroke(width = w * 0.095f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val crown = Path().apply {
      moveTo(w * 0.14f, h * 0.76f)
      lineTo(w * 0.14f, h * 0.32f)
      lineTo(w * 0.35f, h * 0.52f)
      lineTo(w * 0.50f, h * 0.22f)
      lineTo(w * 0.65f, h * 0.52f)
      lineTo(w * 0.86f, h * 0.32f)
      lineTo(w * 0.86f, h * 0.76f)
      close()
    }
    drawPath(crown, color = tint, style = stroke)
  }
}

@Composable
private fun ProfileLocationPinSvgIcon(tint: Color, size: Dp = 15.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = w * 0.105f
    val pin = Path().apply {
      moveTo(w * 0.50f, h * 0.90f)
      cubicTo(w * 0.22f, h * 0.64f, w * 0.18f, h * 0.48f, w * 0.18f, h * 0.38f)
      cubicTo(w * 0.18f, h * 0.18f, w * 0.32f, h * 0.10f, w * 0.50f, h * 0.10f)
      cubicTo(w * 0.68f, h * 0.10f, w * 0.82f, h * 0.18f, w * 0.82f, h * 0.38f)
      cubicTo(w * 0.82f, h * 0.48f, w * 0.78f, h * 0.64f, w * 0.50f, h * 0.90f)
      close()
    }
    drawPath(pin, color = tint, style = Stroke(width = stroke, join = StrokeJoin.Round))
    drawCircle(color = tint, radius = w * 0.10f, center = Offset(w * 0.50f, h * 0.38f))
  }
}

@Composable
private fun ProfilePostsGridSvgIcon(tint: Color, size: Dp = 17.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val cell = w * 0.34f
    val gap = w * 0.14f
    val start = (w - (cell * 2 + gap)) / 2f
    val stroke = Stroke(width = w * 0.10f, join = StrokeJoin.Round)
    val radius = CornerRadius(w * 0.08f, w * 0.08f)
    for (r in 0..1) {
      for (c in 0..1) {
        drawRoundRect(
          color = tint,
          topLeft = Offset(start + c * (cell + gap), start + r * (cell + gap)),
          size = Size(cell, cell),
          cornerRadius = radius,
          style = stroke,
        )
      }
    }
  }
}

@Composable
private fun ProfileFilterSlidersSvgIcon(tint: Color, size: Dp = 18.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = w * 0.09f
    val ys = listOf(h * 0.24f, h * 0.50f, h * 0.76f)
    val knobXs = listOf(w * 0.35f, w * 0.68f, w * 0.42f)
    ys.forEachIndexed { i, y ->
      drawLine(tint, Offset(w * 0.12f, y), Offset(w * 0.88f, y), strokeWidth = stroke, cap = StrokeCap.Round)
      drawCircle(tint, radius = w * 0.09f, center = Offset(knobXs[i], y))
    }
  }
}

@Composable
private fun ProfilePlayOutlineSvgIcon(tint: Color, size: Dp = 13.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val triangle = Path().apply {
      moveTo(w * 0.22f, h * 0.16f)
      lineTo(w * 0.86f, h * 0.50f)
      lineTo(w * 0.22f, h * 0.84f)
      close()
    }
    drawPath(triangle, color = tint, style = Stroke(width = w * 0.12f, join = StrokeJoin.Round))
  }
}

@Composable
private fun ProfilePhotoStackSvgIcon(tint: Color, size: Dp = 15.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    drawRoundRect(
      color = tint,
      topLeft = Offset(w * 0.16f, h * 0.24f),
      size = Size(w * 0.60f, h * 0.60f),
      cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
    )
    drawLine(tint, Offset(w * 0.32f, h * 0.12f), Offset(w * 0.86f, h * 0.12f), strokeWidth = w * 0.10f, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.86f, h * 0.12f), Offset(w * 0.86f, h * 0.68f), strokeWidth = w * 0.10f, cap = StrokeCap.Round)
  }
}
