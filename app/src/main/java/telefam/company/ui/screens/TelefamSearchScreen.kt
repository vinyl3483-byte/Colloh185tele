package telefam.company.ui.screens

import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.telefam.company.app.R
import telefam.company.core.AdaptiveStreamSelector
import telefam.company.core.NetworkTier
import telefam.company.core.PostMode
import telefam.company.core.PostPrivacy
import telefam.company.core.PostSongTrack
import telefam.company.core.TelefamPostRules
import telefam.company.core.formatCompactCount
import telefam.company.media.TelefamVideoCacheManager
import telefam.company.ui.components.PostThemeCanvasBox
import telefam.company.ui.components.SmartInteractiveText
import telefam.company.ui.components.TelefamTricolorSpinner
import telefam.company.ui.components.postCardTextStyle
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamPureWhite
import telefam.company.ui.viewmodel.FeedUiItem
import telefam.company.ui.viewmodel.FeedsUiState

/**
 * Full Feed Search overlay screen with independent search bar, dynamic trending hashtag subtitles,
 * muted autoplay small video cards, and reusable fullscreen feed viewer when tapped.
 */
@Composable
fun TelefamSearchScreen(
  feedsState: FeedsUiState,
  networkTier: NetworkTier,
  isHostActive: Boolean = true,
  onQueryChanged: (String) -> Unit,
  onShortcutSelected: (String) -> Unit,
  onOpenCardAtIndex: (Int) -> Unit,
  onCloseViewer: () -> Unit,
  onBack: () -> Unit,
  onToggleFollowAuthor: (String, Boolean) -> Unit,
  onToggleLike: (String, Boolean) -> Unit,
  onDoubleTapLike: (String) -> Unit,
  onCommentClick: (FeedUiItem) -> Unit,
  onToggleFavourite: (String, Boolean) -> Unit,
  onShareRecorded: (String) -> Unit,
  onToggleRepost: (String, Boolean) -> Unit,
  onDownloadVideo: (FeedUiItem) -> Unit,
  onNotInterested: (String) -> Unit,
  onReportPost: (String, String) -> Unit,
  onBlockAuthor: (String, String) -> Unit,
  onMuteAuthor: (String) -> Unit = {},
  onEditOwnerPost: (FeedUiItem) -> Unit,
  onChangePostPrivacy: (String, PostPrivacy) -> Unit = { _, _ -> },
  onDeleteOwnerPost: (String) -> Unit,
  onTapHashtag: (String, PostMode) -> Unit,
  onTapAudio: (PostSongTrack, PostMode) -> Unit,
  onRecordWatchProgress: (String, Long, Long) -> Unit,
  onOpenAuthorProfile: (String) -> Unit = {},
  autoplayVideos: Boolean = true,
  captionsEnabled: Boolean = false,
  reducedMotion: Boolean = false,
  onShowToast: (String) -> Unit,
) {
  val viewerItems = feedsState.searchViewerItems
  BackHandler(enabled = true) {
    if (viewerItems != null) {
      onCloseViewer()
    } else {
      onBack()
    }
  }

  if (viewerItems != null) {
    ReusableVideoFeedScreen(
      items = viewerItems,
      initialIndex = feedsState.searchViewerInitialIndex,
      isStandaloneViewer = true,
      isHostActive = isHostActive,
      networkTier = networkTier,
      streamAccessToken = feedsState.streamAccessToken,
      streamDeviceFp = feedsState.streamDeviceFp,
      bottomChromePadding = 0.dp,
      onSearchClick = onCloseViewer,
      onBackClick = onCloseViewer,
      onToggleFollowAuthor = onToggleFollowAuthor,
      onToggleLike = onToggleLike,
      onDoubleTapLike = onDoubleTapLike,
      onCommentClick = onCommentClick,
      onToggleFavourite = onToggleFavourite,
      onShareRecorded = onShareRecorded,
      onToggleRepost = onToggleRepost,
      onDownloadVideo = onDownloadVideo,
      onNotInterested = onNotInterested,
      onReportPost = onReportPost,
      onBlockAuthor = onBlockAuthor,
      onMuteAuthor = onMuteAuthor,
      onEditOwnerPost = onEditOwnerPost,
      onChangePostPrivacy = onChangePostPrivacy,
      onDeleteOwnerPost = onDeleteOwnerPost,
      onTapHashtag = onTapHashtag,
      onTapAudio = onTapAudio,
      onRecordWatchProgress = onRecordWatchProgress,
      onOpenAuthorProfile = onOpenAuthorProfile,
      autoplayVideos = autoplayVideos,
      captionsEnabled = captionsEnabled,
      reducedMotion = reducedMotion,
      onShowToast = onShowToast,
    )
    return
  }

  Surface(
    modifier = Modifier
      .fillMaxSize()
      .testTag("feed_search_screen"),
    color = MaterialTheme.colorScheme.background,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .navigationBarsPadding(),
    ) {
      // Independent Top Search Header + Dynamic Shortcut Subtitles
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
          .statusBarsPadding()
          .padding(bottom = 10.dp),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = onBack,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("feed_search_back"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.cd_back),
              tint = MaterialTheme.colorScheme.onSurface,
            )
          }

          Surface(
            modifier = Modifier
              .weight(1f)
              .height(44.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Row(
              modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (feedsState.searchQuery.isEmpty()) {
                  Text(
                    text = stringResource(R.string.feed_search_hint),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                BasicTextField(
                  value = feedsState.searchQuery,
                  onValueChange = onQueryChanged,
                  singleLine = true,
                  textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                  ),
                  cursorBrush = SolidColor(TelefamCrimson),
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feed_search_input"),
                )
              }
              if (feedsState.searchQuery.isNotEmpty()) {
                IconButton(
                  onClick = { onQueryChanged("") },
                  modifier = Modifier.size(28.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_clear_search),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.width(10.dp))
        }

        // Dynamic Shortcut Subtitles Strip
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ShortcutChip(
            label = stringResource(R.string.feed_search_all),
            selected = feedsState.activeShortcut.isBlank(),
            onClick = { onShortcutSelected("") },
          )
          feedsState.searchShortcuts.forEach { shortcut ->
            val selected = feedsState.activeShortcut == shortcut.key
            ShortcutChip(
              label = if (shortcut.count > 0) "${shortcut.label} (${formatCompactCount(shortcut.count)})" else shortcut.label,
              selected = selected,
              onClick = { onShortcutSelected(shortcut.key) },
            )
          }
        }
      }

      // Responsive Autoplay Muted Search Cards Grid
      BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
        val columns = when {
          maxWidth >= 960.dp -> 4
          maxWidth >= 620.dp -> 3
          else -> 2
        }

        if (feedsState.isSearching && feedsState.searchResults.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TelefamTricolorSpinner(size = 42.dp, reducedMotion = reducedMotion)
          }
        } else if (feedsState.searchResults.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
              Text(
                text = stringResource(R.string.feed_empty_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = stringResource(R.string.feed_search_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        } else {
          LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().testTag("feed_search_grid"),
          ) {
            itemsIndexed(
              items = feedsState.searchResults,
              key = { _, item -> item.postId },
            ) { index, item ->
              SearchMutedAutoplayCard(
                item = item,
                enableLivePreviewPlayer = index < 4 && autoplayVideos,
                networkTier = networkTier,
                streamAccessToken = feedsState.streamAccessToken,
                streamDeviceFp = feedsState.streamDeviceFp,
                onClickAuthor = { onOpenAuthorProfile(item.authorUserId) },
                onClick = { onOpenCardAtIndex(index) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ShortcutChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(18.dp),
    color = if (selected) TelefamCrimson else MaterialTheme.colorScheme.surfaceVariant,
    border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Text(
      text = label,
      color = if (selected) TelefamPureWhite else MaterialTheme.colorScheme.onSurface,
      fontSize = 13.sp,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
    )
  }
}

@OptIn(UnstableApi::class)
@Composable
private fun SearchMutedAutoplayCard(
  item: FeedUiItem,
  enableLivePreviewPlayer: Boolean,
  networkTier: NetworkTier,
  streamAccessToken: String,
  streamDeviceFp: String,
  onClickAuthor: () -> Unit,
  onClick: () -> Unit,
) {
  val context = LocalContext.current

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(MaterialTheme.colorScheme.surface)
      .clickable(onClick = onClick)
      .testTag("search_card_${item.postId}"),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(9f / 14f)
        .clip(RoundedCornerShape(14.dp))
        .background(Color(0xFF0E1016)),
      contentAlignment = Alignment.Center,
    ) {
      when (item.mode) {
        PostMode.VIDEO -> {
          if (item.thumbnailUrl.isNotBlank()) {
            AsyncImage(
              model = item.thumbnailUrl,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize().blur(18.dp),
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f)))
            AsyncImage(
              model = item.thumbnailUrl,
              contentDescription = item.caption,
              contentScale = ContentScale.Fit,
              modifier = Modifier.fillMaxSize(),
            )
          }

          if (enableLivePreviewPlayer && item.primaryStreamUrl.isNotBlank() && networkTier != NetworkTier.OFFLINE) {
            var previewFailed by remember(item.postId) { mutableStateOf(false) }
            val previewPlayer = remember(item.postId, streamAccessToken) {
              TelefamVideoCacheManager.buildFeedExoPlayer(
                context = context,
                networkTier = NetworkTier.WEAK, // Low-bandwidth muted preview stream
                bearerToken = streamAccessToken,
                deviceFingerprint = streamDeviceFp,
                startMuted = true,
              ).apply {
                addListener(
                  object : androidx.media3.common.Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                      previewFailed = true
                    }
                  },
                )
                val url = AdaptiveStreamSelector.selectRenditionUrl(
                  baseUrl = com.telefam.company.app.BuildConfig.TELEFAM_API_BASE_URL,
                  primaryVideoUrl = item.primaryStreamUrl,
                  renditions = item.renditions,
                  networkTier = NetworkTier.WEAK,
                ).ifBlank { item.primaryStreamUrl }
                setMediaItem(TelefamVideoCacheManager.createMediaItem(url))
                prepare()
                playWhenReady = true
              }
            }
            DisposableEffect(previewPlayer) {
              onDispose { previewPlayer.release() }
            }
            if (!previewFailed) {
              AndroidView(
                factory = { ctx ->
                  (LayoutInflater.from(ctx).inflate(R.layout.view_feed_player, null) as PlayerView).apply {
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    useController = false
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    player = previewPlayer
                  }
                },
                modifier = Modifier.fillMaxSize(),
              )
            }
          }
        }

        PostMode.PHOTOS -> {
          val first = item.photoUrls.firstOrNull().orEmpty()
          if (first.isNotBlank()) {
            AsyncImage(
              model = first,
              contentDescription = null,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize().blur(18.dp),
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f)))
            AsyncImage(
              model = first,
              contentDescription = item.caption,
              contentScale = ContentScale.Fit,
              modifier = Modifier.fillMaxSize(),
            )
          }
        }

        PostMode.TEXT -> {
          val theme = TelefamPostRules.findTheme(item.themeId)
          val ink = if (theme.isLightInk) Color.White else Color(0xFF1C1F26)
          PostThemeCanvasBox(theme = theme, modifier = Modifier.fillMaxSize()) {
            Text(
              text = item.textContent,
              style = postCardTextStyle(item.fontOption, ink).copy(fontSize = 15.sp, lineHeight = 20.sp),
              maxLines = 5,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.align(Alignment.Center).padding(12.dp),
            )
          }
        }
      }

      // Bottom gradient & compact stats
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(64.dp)
          .align(Alignment.BottomCenter)
          .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))),
      )

      Row(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.Filled.PlayArrow,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
          text = item.authorUsername,
          color = Color.White,
          fontSize = 11.5.sp,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClickAuthor)
            .padding(vertical = 2.dp),
        )
        Icon(
          imageVector = Icons.Outlined.FavoriteBorder,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(13.dp),
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
          text = formatCompactCount(item.likesCount),
          color = Color.White,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }

    if (item.caption.isNotBlank() || item.hashtags.isNotEmpty()) {
      SmartInteractiveText(
        text = item.caption.ifBlank { item.hashtags.joinToString(" ") { "#$it" } },
        source = "search",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurface,
        linkColor = TelefamCrimson,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        showOtpPills = false,
        onPlainClickFallback = onClick,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
      )
    }
  }
}
