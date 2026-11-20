package telefam.company.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.telefam.company.app.BuildConfig
import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.telefam.company.app.R
import kotlinx.coroutines.delay
import telefam.company.core.AdaptiveStreamSelector
import telefam.company.core.FeedTab
import telefam.company.core.NetworkTier
import telefam.company.core.PostMode
import telefam.company.core.PostPrivacy
import telefam.company.core.PostSongTrack
import telefam.company.core.SmartTextDetector
import telefam.company.core.TelefamPostRules
import telefam.company.core.formatCompactCount
import telefam.company.media.TelefamVideoCacheManager
import telefam.company.ui.components.AnimatedAudioBarsIcon
import telefam.company.ui.components.FeedBookmarkSvgIcon
import telefam.company.ui.components.FeedCommentBubbleSvgIcon
import telefam.company.ui.components.FeedCreatorAvatarWithFollow
import telefam.company.ui.components.FeedDoubleTapHeartOverlay
import telefam.company.ui.components.FeedHeartSvgIcon
import telefam.company.ui.components.FeedLiveTvIcon
import telefam.company.ui.components.FeedMoreDotsSvgIcon
import telefam.company.ui.components.FeedMoreOptionsSheet
import telefam.company.ui.components.FeedMusicNoteSvgIcon
import telefam.company.ui.components.FeedPaperPlaneSvgIcon
import telefam.company.ui.components.FeedReportReasonDialog
import telefam.company.ui.components.FeedSearchSvgIcon
import telefam.company.ui.components.FeedShareBottomSheet
import telefam.company.ui.components.LinkPreviewStack
import telefam.company.ui.components.PostThemeCanvasBox
import telefam.company.ui.components.SmartInteractiveText
import telefam.company.ui.components.TelefamSparkleEmblem
import telefam.company.ui.components.TelefamTricolorSpinner
import telefam.company.ui.components.postCardTextStyle
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamPureWhite
import telefam.company.ui.viewmodel.FeedUiItem

/**
 * Reusable full-screen VerticalPager feed used identically in Home -> Feeds
 * and when opening any Search video card, across phones, foldables, tablets, and desktops.
 */
@OptIn(UnstableApi::class)
@Composable
fun ReusableVideoFeedScreen(
  items: List<FeedUiItem>,
  initialIndex: Int = 0,
  activeTab: FeedTab = FeedTab.FOR_YOU,
  isStandaloneViewer: Boolean = false,
  isHostActive: Boolean = true,
  isLoading: Boolean = false,
  hasMore: Boolean = false,
  networkTier: NetworkTier = NetworkTier.GOOD,
  streamAccessToken: String = "",
  streamDeviceFp: String = "",
  bottomChromePadding: Dp = 0.dp,
  onSelectFeedTab: (FeedTab) -> Unit = {},
  onLiveClick: () -> Unit = {},
  onSearchClick: () -> Unit = {},
  onBackClick: () -> Unit = {},
  onLoadMore: () -> Unit = {},
  onRefresh: () -> Unit = {},
  onToggleFollowAuthor: (authorUserId: String, currentlyFollowing: Boolean) -> Unit,
  onToggleLike: (postId: String, currentlyLiked: Boolean) -> Unit,
  onDoubleTapLike: (postId: String) -> Unit,
  onCommentClick: (FeedUiItem) -> Unit,
  onToggleFavourite: (postId: String, currentlyFavourited: Boolean) -> Unit,
  onShareRecorded: (postId: String) -> Unit,
  onToggleRepost: (postId: String, currentlyReposted: Boolean) -> Unit,
  onDownloadVideo: (FeedUiItem) -> Unit,
  onNotInterested: (postId: String) -> Unit,
  onReportPost: (postId: String, reason: String) -> Unit,
  onBlockAuthor: (postId: String, authorUserId: String) -> Unit,
  onMuteAuthor: (authorUserId: String) -> Unit = {},
  onEditOwnerPost: (FeedUiItem) -> Unit,
  onChangePostPrivacy: (postId: String, privacy: PostPrivacy) -> Unit = { _, _ -> },
  onDeleteOwnerPost: (postId: String) -> Unit,
  onTapHashtag: (tag: String, mode: PostMode) -> Unit,
  onTapAudio: (song: PostSongTrack, mode: PostMode) -> Unit,
  onRecordWatchProgress: (postId: String, watchMs: Long, durationMs: Long) -> Unit,
  onOpenAuthorProfile: (userId: String) -> Unit = {},
  autoplayVideos: Boolean = true,
  captionsEnabled: Boolean = false,
  reducedMotion: Boolean = false,
  onShowToast: (String) -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  LaunchedEffect(streamAccessToken, streamDeviceFp) {
    TelefamVideoCacheManager.updateCredentials(streamAccessToken, streamDeviceFp)
  }

  // Shared ExoPlayer instance tuned with 512MB SimpleCache & play-ahead buffering
  val exoPlayer = remember(context) {
    TelefamVideoCacheManager.buildFeedExoPlayer(
      context = context,
      networkTier = networkTier,
      bearerToken = streamAccessToken,
      deviceFingerprint = streamDeviceFp,
      startMuted = false,
    )
  }

  // Dedicated looping audio ExoPlayer so posts (VIDEO / PHOTOS / TEXT) play attached songs smoothly
  val songPlayer = remember(context) {
    TelefamVideoCacheManager.buildFeedAudioExoPlayer(
      context = context,
      repeatOne = true,
    )
  }

  var isPlayerBuffering by remember { mutableStateOf(false) }
  var isVideoPlaybackFailed by remember { mutableStateOf(false) }
  var fallbackPrimaryUrl by remember { mutableStateOf("") }
  var activeStreamUrl by remember { mutableStateOf("") }
  var userPausedCurrent by remember { mutableStateOf(false) }
  var isFastForward2x by remember { mutableStateOf(false) }

  DisposableEffect(exoPlayer, songPlayer) {
    val listener = object : Player.Listener {
      override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
          Player.STATE_BUFFERING -> isPlayerBuffering = true
          Player.STATE_READY -> {
            isPlayerBuffering = false
            isVideoPlaybackFailed = false
          }
          Player.STATE_ENDED, Player.STATE_IDLE -> isPlayerBuffering = false
        }
      }

      override fun onPlayerError(error: PlaybackException) {
        val fallback = fallbackPrimaryUrl
        if (fallback.isNotBlank() && fallback != activeStreamUrl) {
          activeStreamUrl = fallback
          isVideoPlaybackFailed = false
          exoPlayer.setMediaItem(TelefamVideoCacheManager.createMediaItem(fallback))
          exoPlayer.prepare()
          exoPlayer.playWhenReady = !userPausedCurrent
        } else {
          isPlayerBuffering = false
          isVideoPlaybackFailed = true
        }
      }
    }
    exoPlayer.addListener(listener)
    onDispose {
      exoPlayer.removeListener(listener)
      exoPlayer.release()
      songPlayer.release()
    }
  }

  DisposableEffect(lifecycleOwner, exoPlayer, songPlayer, isHostActive) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
          exoPlayer.playWhenReady = false
          songPlayer.playWhenReady = false
        }
        Lifecycle.Event.ON_RESUME -> {
          if (isHostActive && !userPausedCurrent) {
            if (exoPlayer.mediaItemCount > 0) exoPlayer.playWhenReady = true
            if (songPlayer.mediaItemCount > 0) songPlayer.playWhenReady = true
          }
        }
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  LaunchedEffect(isHostActive) {
    if (!isHostActive) {
      exoPlayer.playWhenReady = false
      exoPlayer.pause()
      songPlayer.playWhenReady = false
      songPlayer.pause()
    } else if (!userPausedCurrent) {
      if (exoPlayer.mediaItemCount > 0) exoPlayer.playWhenReady = true
      if (songPlayer.mediaItemCount > 0) songPlayer.playWhenReady = true
    }
  }

  LaunchedEffect(autoplayVideos) {
    if (!autoplayVideos) {
      userPausedCurrent = true
      exoPlayer.playWhenReady = false
      exoPlayer.pause()
      songPlayer.playWhenReady = false
      songPlayer.pause()
    } else if (isHostActive) {
      userPausedCurrent = false
      if (exoPlayer.mediaItemCount > 0) exoPlayer.playWhenReady = true
      if (songPlayer.mediaItemCount > 0) songPlayer.playWhenReady = true
    }
  }

  LaunchedEffect(networkTier) {
    TelefamVideoCacheManager.applyAdaptiveBitrate(exoPlayer, networkTier)
  }

  var shareSheetItem by remember { mutableStateOf<FeedUiItem?>(null) }
  var moreSheetItem by remember { mutableStateOf<FeedUiItem?>(null) }
  var reportTargetPostId by remember { mutableStateOf<String?>(null) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF07080B))
      .testTag("reusable_video_feed_root"),
  ) {

    if (items.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(bottom = bottomChromePadding),
        contentAlignment = Alignment.Center,
      ) {
        if (isLoading || networkTier == NetworkTier.WEAK) {
          TelefamTricolorSpinner(size = 46.dp, reducedMotion = reducedMotion)
        } else {
          Column(
            modifier = Modifier
              .padding(horizontal = 32.dp)
              .widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Box(
              modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(TelefamCrimson.copy(alpha = 0.16f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Outlined.Movie,
                contentDescription = null,
                tint = TelefamCrimson,
                modifier = Modifier.size(30.dp),
              )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
              text = stringResource(R.string.feed_empty_title),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = Color.White,
              textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = stringResource(R.string.feed_empty_subtitle),
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White.copy(alpha = 0.72f),
              textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
              onClick = onRefresh,
              shape = RoundedCornerShape(22.dp),
              colors = ButtonDefaults.buttonColors(containerColor = TelefamCrimson, contentColor = Color.White),
              modifier = Modifier.height(44.dp),
            ) {
              Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(text = stringResource(R.string.nav_feeds), fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
    } else {
      val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, items.lastIndex),
        pageCount = { items.size },
      )
      val currentItem = items.getOrNull(pagerState.currentPage)
      val latestRecordWatch by rememberUpdatedState(onRecordWatchProgress)

      // Switch media item smoothly + preload next 5 upcoming videos + sync attached song + record watch telemetry
      LaunchedEffect(pagerState.currentPage, items, networkTier, streamAccessToken) {
        val pageIdx = pagerState.currentPage
        val active = items.getOrNull(pageIdx) ?: return@LaunchedEffect
        userPausedCurrent = !autoplayVideos
        isFastForward2x = false
        isVideoPlaybackFailed = false
        exoPlayer.setPlaybackParameters(PlaybackParameters(1.0f))

        val baseApiUrl = BuildConfig.TELEFAM_API_BASE_URL
        val resolvedSongUrl = active.song?.previewUrl?.trim().orEmpty().let { raw ->
          if (raw.isBlank()) "" else AdaptiveStreamSelector.resolveMediaUrl(baseApiUrl, raw)
        }

        if (active.mode == PostMode.VIDEO && active.primaryStreamUrl.isNotBlank()) {
          val resolvedPrimary = AdaptiveStreamSelector.resolveMediaUrl(baseApiUrl, active.primaryStreamUrl)
          val streamUrl = AdaptiveStreamSelector.selectRenditionUrl(
            baseUrl = baseApiUrl,
            primaryVideoUrl = active.primaryStreamUrl,
            renditions = active.renditions,
            networkTier = networkTier,
          ).ifBlank { resolvedPrimary }
          fallbackPrimaryUrl = resolvedPrimary
          activeStreamUrl = streamUrl
          exoPlayer.volume = if (active.muteOriginalAudio || resolvedSongUrl.isNotBlank()) 0f else 1f
          exoPlayer.setMediaItem(TelefamVideoCacheManager.createMediaItem(streamUrl))
          exoPlayer.prepare()
          exoPlayer.playWhenReady = isHostActive && autoplayVideos
        } else {
          fallbackPrimaryUrl = ""
          activeStreamUrl = ""
          exoPlayer.pause()
          exoPlayer.clearMediaItems()
        }

        if (resolvedSongUrl.isNotBlank()) {
          songPlayer.volume = 1f
          songPlayer.setMediaItem(TelefamVideoCacheManager.createMediaItem(resolvedSongUrl))
          songPlayer.prepare()
          songPlayer.playWhenReady = isHostActive && autoplayVideos
        } else {
          songPlayer.pause()
          songPlayer.clearMediaItems()
        }

        // Preload next 5 upcoming videos into disk LRU SimpleCache
        val nextFiveUrls = items.drop(pageIdx + 1)
          .asSequence()
          .filter { it.mode == PostMode.VIDEO && it.primaryStreamUrl.isNotBlank() }
          .take(5)
          .map { item ->
            AdaptiveStreamSelector.selectRenditionUrl(
              baseUrl = baseApiUrl,
              primaryVideoUrl = item.primaryStreamUrl,
              renditions = item.renditions,
              networkTier = networkTier,
            ).ifBlank { AdaptiveStreamSelector.resolveMediaUrl(baseApiUrl, item.primaryStreamUrl) }
          }
          .toList()
        if (nextFiveUrls.isNotEmpty()) {
          TelefamVideoCacheManager.preloadNextFiveVideos(
            context = context,
            upcomingUrls = nextFiveUrls,
            networkTier = networkTier,
            bearerToken = streamAccessToken,
            deviceFingerprint = streamDeviceFp,
          )
        }

        if (hasMore && pageIdx >= items.size - 3) {
          onLoadMore()
        }

        if (isHostActive) {
          val startMs = System.currentTimeMillis()
          while (true) {
            delay(2_000L)
            val elapsed = System.currentTimeMillis() - startMs
            if (elapsed >= 4_000L) {
              latestRecordWatch(active.postId, elapsed, active.durationMs.coerceAtLeast(5_000L))
              break
            }
          }
        }
      }

      VerticalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        modifier = Modifier
          .fillMaxSize()
          .testTag("feed_vertical_pager"),
      ) { pageIndex ->
        val item = items[pageIndex]
        val isActivePage = pagerState.currentPage == pageIndex
        val hasSongAudio = !item.song?.previewUrl.isNullOrBlank()
        var showDoubleTapHeart by remember(item.postId) { mutableStateOf(false) }

        LaunchedEffect(showDoubleTapHeart) {
          if (showDoubleTapHeart) {
            delay(650L)
            showDoubleTapHeart = false
          }
        }

        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomChromePadding),
        ) {
          // Full-screen Media Stage: FIT + blurred ambient background preserves ALL aspect ratios cleanly
          Box(
            modifier = Modifier
              .fillMaxSize()
              .pointerInput(item.postId, isActivePage, hasSongAudio) {
                detectTapGestures(
                  onDoubleTap = {
                    showDoubleTapHeart = true
                    onDoubleTapLike(item.postId)
                  },
                  onTap = {
                    if (isActivePage && (item.mode == PostMode.VIDEO || hasSongAudio)) {
                      userPausedCurrent = !userPausedCurrent
                      if (item.mode == PostMode.VIDEO) {
                        exoPlayer.playWhenReady = !userPausedCurrent
                      }
                      if (hasSongAudio) {
                        songPlayer.playWhenReady = !userPausedCurrent
                      }
                    }
                  },
                  onLongPress = {
                    if (isActivePage && item.mode == PostMode.VIDEO) {
                      isFastForward2x = true
                      exoPlayer.setPlaybackParameters(PlaybackParameters(2.0f))
                    }
                  },
                )
              }
              .pointerInput(item.postId, isActivePage) {
                awaitEachGesture {
                  while (true) {
                    val event = awaitPointerEvent()
                    val anyPressed = event.changes.any { it.pressed }
                    if ((!anyPressed || event.type == PointerEventType.Release) && isFastForward2x) {
                      isFastForward2x = false
                      exoPlayer.setPlaybackParameters(PlaybackParameters(1.0f))
                    }
                    if (!anyPressed) break
                  }
                }
              },
            contentAlignment = Alignment.Center,
          ) {
            when (item.mode) {
              PostMode.VIDEO -> {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF07080B)),
                  contentAlignment = Alignment.Center,
                ) {
                  // 1) Full-screen blurred ambient background behind FIT video bars
                  if (item.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                      model = item.thumbnailUrl,
                      contentDescription = null,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier
                        .fillMaxSize()
                        .blur(28.dp),
                    )
                    Box(
                      modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.44f)),
                    )
                    // Crisp FIT thumbnail preview before first frame renders / on non-active pages
                    AsyncImage(
                      model = item.thumbnailUrl,
                      contentDescription = item.caption.ifBlank { item.authorFullName },
                      contentScale = ContentScale.Fit,
                      modifier = Modifier.fillMaxSize(),
                    )
                  }
                  // 2) Full-screen PlayerView (TextureView + RESIZE_MODE_FIT + transparent shutter)
                  if (isActivePage && item.primaryStreamUrl.isNotBlank() && !isVideoPlaybackFailed) {
                    AndroidView(
                      factory = { ctx ->
                        (LayoutInflater.from(ctx).inflate(R.layout.view_feed_player, null) as PlayerView).apply {
                          resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                          useController = false
                          setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                          setBackgroundColor(android.graphics.Color.TRANSPARENT)
                          player = exoPlayer
                        }
                      },
                      update = { pv ->
                        pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        if (pv.player !== exoPlayer) pv.player = exoPlayer
                      },
                      modifier = Modifier.fillMaxSize(),
                    )
                  }
                }
              }

              PostMode.PHOTOS -> {
                if (item.photoUrls.isNotEmpty()) {
                  val photoPager = rememberPagerState(pageCount = { item.photoUrls.size })
                  val currentBackdrop = item.photoUrls.getOrNull(photoPager.currentPage).orEmpty()
                  Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .background(Color(0xFF07080B)),
                    contentAlignment = Alignment.Center,
                  ) {
                    if (currentBackdrop.isNotBlank()) {
                      AsyncImage(
                        model = currentBackdrop,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                          .fillMaxSize()
                          .blur(28.dp),
                      )
                      Box(
                        modifier = Modifier
                          .fillMaxSize()
                          .background(Color.Black.copy(alpha = 0.44f)),
                      )
                    }
                    HorizontalPager(
                      state = photoPager,
                      modifier = Modifier.fillMaxSize(),
                    ) { pIdx ->
                      AsyncImage(
                        model = item.photoUrls[pIdx],
                        contentDescription = item.caption.ifBlank { item.authorFullName },
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                      )
                    }
                    if (item.photoUrls.size > 1) {
                      Row(
                        modifier = Modifier
                          .align(Alignment.BottomCenter)
                          .padding(bottom = 14.dp)
                          .background(Color.Black.copy(alpha = 0.48f), RoundedCornerShape(10.dp))
                          .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                      ) {
                        repeat(item.photoUrls.size) { d ->
                          Box(
                            modifier = Modifier
                              .size(6.dp)
                              .clip(CircleShape)
                              .background(if (photoPager.currentPage == d) Color.White else Color.White.copy(alpha = 0.4f)),
                          )
                        }
                      }
                    }
                  }
                }
              }

              PostMode.TEXT -> {
                val theme = TelefamPostRules.findTheme(item.themeId)
                val ink = if (theme.isLightInk) Color.White else Color(0xFF1C1F26)
                PostThemeCanvasBox(
                  theme = theme,
                  modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(24.dp)),
                ) {
                  Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .padding(24.dp),
                    contentAlignment = Alignment.Center,
                  ) {
                    SmartInteractiveText(
                      text = item.textContent,
                      source = "feed",
                      style = postCardTextStyle(item.fontOption, ink).copy(textAlign = TextAlign.Center),
                      color = ink,
                      linkColor = if (theme.isLightInk) Color(0xFFFFD1DC) else TelefamCrimson,
                      textAlign = TextAlign.Center,
                      showOtpPills = false,
                    )
                  }
                }
              }
            }

            // Bottom subtle readability gradient behind overlay metadata
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .align(Alignment.BottomCenter)
                .background(
                  Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                  ),
                ),
            )

            // Pause emblem
            AnimatedVisibility(
              visible = isActivePage && userPausedCurrent && (item.mode == PostMode.VIDEO || hasSongAudio),
              enter = fadeIn(),
              exit = fadeOut(),
            ) {
              Box(
                modifier = Modifier
                  .size(66.dp)
                  .clip(CircleShape)
                  .background(Color.Black.copy(alpha = 0.48f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Filled.PlayArrow,
                  contentDescription = stringResource(R.string.feed_paused),
                  tint = Color.White,
                  modifier = Modifier.size(38.dp),
                )
              }
            }

            // 2x Fast-forward discrete pill during long press
            AnimatedVisibility(
              visible = isActivePage && isFastForward2x && item.mode == PostMode.VIDEO,
              enter = fadeIn(),
              exit = fadeOut(),
              modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 64.dp),
            ) {
              Surface(
                color = Color.Black.copy(alpha = 0.68f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
              ) {
                Text(
                  text = ">> ${stringResource(R.string.feed_speed_2x)}",
                  color = Color.White,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
              }
            }

            // Red/White/Green 3-dot spinner when buffering or on weak internet
            if (isActivePage && item.mode == PostMode.VIDEO && !isVideoPlaybackFailed && (isPlayerBuffering || (networkTier == NetworkTier.WEAK && !userPausedCurrent && !exoPlayer.isPlaying))) {
              TelefamTricolorSpinner(size = 44.dp, reducedMotion = reducedMotion)
            }

            // Double-tap animated heart burst
            FeedDoubleTapHeartOverlay(visible = showDoubleTapHeart)
          }

          // Live Closed Captions banner when Accessibility -> Captions is enabled
          if (captionsEnabled && isActivePage) {
            val ccText = item.caption.ifBlank {
              item.song?.let { "${it.title} — ${it.artist}" } ?: item.textContent
            }
            if (ccText.isNotBlank()) {
              Surface(
                color = Color.Black.copy(alpha = 0.72f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .align(Alignment.BottomCenter)
                  .navigationBarsPadding()
                  .padding(horizontal = 24.dp, vertical = 152.dp + bottomChromePadding)
                  .widthIn(max = 420.dp)
                  .testTag("feed_live_captions"),
              ) {
                Text(
                  text = ccText,
                  color = Color.White,
                  fontSize = 13.sp,
                  lineHeight = 18.sp,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                )
              }
            }
          }

          // Bottom-Left Metadata Column (non-overlapping with right rail & bottom nav)
          FeedBottomLeftMetadata(
            item = item,
            isPlayingAudio = isActivePage && !userPausedCurrent,
            reducedMotion = reducedMotion,
            onClickAuthor = { onOpenAuthorProfile(item.authorUserId) },
            onTapHashtag = { tag -> onTapHashtag(tag, item.mode) },
            onTapAudio = { song -> onTapAudio(song, item.mode) },
            modifier = Modifier
              .align(Alignment.BottomStart)
              .navigationBarsPadding()
              .padding(start = 14.dp, end = 86.dp, bottom = 14.dp + bottomChromePadding)
              .widthIn(max = 460.dp),
          )

          // Bottom-Right Vertical Action Rail (safe above bottom nav)
          FeedRightActionRail(
            item = item,
            onOpenAuthorProfile = { onOpenAuthorProfile(item.authorUserId) },
            onToggleFollow = { onToggleFollowAuthor(item.authorUserId, item.isFollowingAuthor) },
            onToggleLike = { onToggleLike(item.postId, item.isLiked) },
            onCommentClick = { onCommentClick(item) },
            onShareClick = { shareSheetItem = item },
            onToggleFavourite = { onToggleFavourite(item.postId, item.isFavourited) },
            onMoreClick = { moreSheetItem = item },
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .navigationBarsPadding()
              .padding(end = 10.dp, bottom = 14.dp + bottomChromePadding),
          )
        }
      }

      if (currentItem == null && isLoading) {
        TelefamTricolorSpinner(
          modifier = Modifier.align(Alignment.Center),
          size = 44.dp,
          reducedMotion = reducedMotion,
        )
      }
    }

    // Top Safe Overlay Bar matching reference image: [LIVE]   Following  |  For You   [Search]
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.verticalGradient(
            colors = listOf(Color.Black.copy(alpha = 0.56f), Color.Transparent),
          ),
        )
        .statusBarsPadding()
        .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
      if (isStandaloneViewer) {
        IconButton(
          onClick = onBackClick,
          modifier = Modifier
            .align(Alignment.CenterStart)
            .minimumInteractiveComponentSize()
            .testTag("feed_viewer_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.cd_back),
            tint = Color.White,
          )
        }
      } else {
        // LIVE TV badge button
        Box(
          modifier = Modifier
            .align(Alignment.CenterStart)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onLiveClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag("feed_live_button"),
          contentAlignment = Alignment.Center,
        ) {
          Box(contentAlignment = Alignment.Center) {
            FeedLiveTvIcon(tint = Color.White, modifier = Modifier.size(30.dp))
            Text(
              text = stringResource(R.string.feed_live),
              color = Color.White,
              fontSize = 7.sp,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 0.3.sp,
              modifier = Modifier.padding(top = 5.dp),
            )
          }
        }

        // Center Tabs: Following | For You
        Row(
          modifier = Modifier.align(Alignment.Center),
          horizontalArrangement = Arrangement.spacedBy(22.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          FeedTab.entries.forEach { tab ->
            val selected = activeTab == tab
            Column(
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { onSelectFeedTab(tab) }
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .testTag("feed_tab_${tab.apiParam}"),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text(
                text = if (tab == FeedTab.FOLLOWING) {
                  stringResource(R.string.feed_tab_following)
                } else {
                  stringResource(R.string.feed_tab_for_you)
                },
                color = if (selected) Color.White else Color.White.copy(alpha = 0.72f),
                fontSize = 16.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Box(
                modifier = Modifier
                  .width(if (selected) 26.dp else 0.dp)
                  .height(2.5.dp)
                  .clip(RoundedCornerShape(2.dp))
                  .background(if (selected) Color.White else Color.Transparent),
              )
            }
          }
        }
      }

      // Top-right Search Icon
      IconButton(
        onClick = onSearchClick,
        modifier = Modifier
          .align(Alignment.CenterEnd)
          .minimumInteractiveComponentSize()
          .testTag("feed_search_button"),
      ) {
        FeedSearchSvgIcon(tint = Color.White, size = 24.dp)
      }
    }
  }

  // Share Sheet (App Chooser + Copy Link + Repost)
  shareSheetItem?.let { item ->
    val configuredBase = BuildConfig.TELEFAM_API_BASE_URL.trim().trimEnd('/')
    val shareLink = if (configuredBase.startsWith("https://") || configuredBase.startsWith("http://")) {
      "$configuredBase/post/${item.postId}"
    } else {
      "telefam://post/${item.postId}"
    }
    FeedShareBottomSheet(
      item = item,
      onShareViaApps = {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
          type = "text/plain"
          putExtra(Intent.EXTRA_TEXT, "${item.caption.ifBlank { item.authorFullName }} $shareLink".trim())
        }
        context.startActivity(Intent.createChooser(sendIntent, null))
        onShareRecorded(item.postId)
      },
      onCopyLink = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Telefam Link", shareLink))
        onShareRecorded(item.postId)
        onShowToast(context.getString(R.string.feed_link_copied))
      },
      onToggleRepost = { onToggleRepost(item.postId, item.isReposted) },
      onDismiss = { shareSheetItem = null },
    )
  }

  // 3-Dots Context Options Sheet
  moreSheetItem?.let { item ->
    FeedMoreOptionsSheet(
      item = item,
      onRepost = { onToggleRepost(item.postId, item.isReposted) },
      onDownload = { onDownloadVideo(item) },
      onNotInterested = { onNotInterested(item.postId) },
      onMute = { onMuteAuthor(item.authorUserId) },
      onReport = { reportTargetPostId = item.postId },
      onBlock = { onBlockAuthor(item.postId, item.authorUserId) },
      onEdit = { onEditOwnerPost(item) },
      onChangePrivacy = { newPrivacy -> onChangePostPrivacy(item.postId, newPrivacy) },
      onDelete = { onDeleteOwnerPost(item.postId) },
      onDismiss = { moreSheetItem = null },
    )
  }

  reportTargetPostId?.let { targetId ->
    FeedReportReasonDialog(
      onSelectReason = { reason -> onReportPost(targetId, reason) },
      onDismiss = { reportTargetPostId = null },
    )
  }
}

@kotlin.OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeedBottomLeftMetadata(
  item: FeedUiItem,
  isPlayingAudio: Boolean,
  reducedMotion: Boolean = false,
  onClickAuthor: () -> Unit,
  onTapHashtag: (String) -> Unit,
  onTapAudio: (PostSongTrack) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    // Repost badge OR dynamic "You might like this" pill matching uploaded screenshot
    when {
      item.repostedByUsername.isNotBlank() || item.isReposted -> {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color.Black.copy(alpha = 0.56f),
          border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.24f)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Filled.Repeat,
              contentDescription = null,
              tint = TelefamCrimson,
              modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            val who = item.repostedByUsername.ifBlank { item.authorUsername }
            Text(
              text = if (who.isNotBlank()) "${stringResource(R.string.feed_reposted)} • @$who" else stringResource(R.string.feed_reposted),
              color = Color.White,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
            )
          }
        }
      }

      item.recommendationLabel.isNotBlank() -> {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color.Black.copy(alpha = 0.54f),
          border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.22f)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            TelefamSparkleEmblem()
            Spacer(modifier = Modifier.width(7.dp))
            Text(
              text = item.recommendationLabel,
              color = Color.White,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
            )
          }
        }
      }
    }

    // Username matching screenshot bold style (tappable to view author profile)
    Text(
      text = item.authorUsername.ifBlank { item.authorFullName },
      color = Color.White,
      fontSize = 16.sp,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier
        .clip(RoundedCornerShape(6.dp))
        .clickable(onClick = onClickAuthor)
        .padding(vertical = 2.dp)
        .testTag("feed_author_username"),
    )

    val captionText = item.caption.ifBlank {
      if (item.mode != PostMode.TEXT) item.textContent else ""
    }
    if (captionText.isNotBlank()) {
      var isCaptionExpanded by remember(item.postId) { mutableStateOf(false) }
      var hasOverflow by remember(item.postId) { mutableStateOf(false) }
      val showToggle = hasOverflow || isCaptionExpanded || captionText.length > 85 || captionText.contains('\n')

      val captionUrls = remember(captionText, item.textContent) {
        val raw = if (captionText.isNotBlank()) captionText else item.textContent
        SmartTextDetector.analyze(raw).urls
      }
      Column(modifier = Modifier.fillMaxWidth()) {
        if (isCaptionExpanded) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 220.dp)
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            SmartInteractiveText(
              text = captionText,
              source = "feed",
              style = androidx.compose.ui.text.TextStyle(
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
              ),
              color = Color.White.copy(alpha = 0.96f),
              linkColor = Color(0xFFFFB3C1),
              showOtpPills = true,
              modifier = Modifier.testTag("feed_caption_text"),
            )
            if (captionUrls.isNotEmpty()) {
              LinkPreviewStack(
                urls = captionUrls,
                source = "feed",
                maxCards = 2,
              )
            }
          }
        } else {
          SmartInteractiveText(
            text = captionText,
            source = "feed",
            style = androidx.compose.ui.text.TextStyle(
              fontSize = 13.5.sp,
              lineHeight = 18.sp,
            ),
            color = Color.White.copy(alpha = 0.96f),
            linkColor = Color(0xFFFFB3C1),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            showOtpPills = false,
            onTextLayout = { res ->
              if (res.hasVisualOverflow) hasOverflow = true
            },
            modifier = Modifier.testTag("feed_caption_text"),
          )
        }
        if (showToggle || captionUrls.isNotEmpty()) {
          Text(
            text = if (isCaptionExpanded) {
              stringResource(R.string.feed_read_less)
            } else {
              stringResource(R.string.feed_read_more)
            },
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .clickable { isCaptionExpanded = !isCaptionExpanded }
              .padding(top = 3.dp, bottom = 2.dp, end = 8.dp)
              .testTag("feed_caption_toggle"),
          )
        }
      }
    }

    // Tappable #hashtags -> autofills Post Screen with zero mistakes
    if (item.hashtags.isNotEmpty()) {
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        item.hashtags.forEach { tag ->
          val clean = tag.removePrefix("#")
          Text(
            text = "#$clean",
            color = TelefamCrimson,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .clickable { onTapHashtag(clean) }
              .padding(vertical = 2.dp)
              .testTag("feed_hashtag_$clean"),
          )
        }
      }
    }

    // Tappable Audio / Song Row -> autofills Post Screen with zero mistakes
    val trackForRow = item.song ?: PostSongTrack(
      trackId = "orig_${item.postId}",
      title = stringResource(R.string.feed_original_sound),
      artist = item.authorUsername.ifBlank { item.authorFullName },
      durationMs = item.durationMs,
      source = "original",
    )
    Row(
      modifier = Modifier
        .clip(RoundedCornerShape(10.dp))
        .clickable { onTapAudio(trackForRow) }
        .padding(vertical = 4.dp, horizontal = 2.dp)
        .testTag("feed_audio_row"),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      FeedMusicNoteSvgIcon(tint = Color.White, size = 15.dp)
      Spacer(modifier = Modifier.width(8.dp))
      val label = if (item.song != null) {
        if (item.song.artist.isNotBlank()) "${item.song.title} • ${item.song.artist}" else item.song.title
      } else {
        stringResource(R.string.feed_original_sound)
      }
      Text(
        text = label,
        color = Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.widthIn(max = 220.dp),
      )
      Spacer(modifier = Modifier.width(10.dp))
      AnimatedAudioBarsIcon(isPlaying = isPlayingAudio, tint = Color.White, reducedMotion = reducedMotion)
    }
  }
}

@Composable
private fun FeedRightActionRail(
  item: FeedUiItem,
  onOpenAuthorProfile: () -> Unit,
  onToggleFollow: () -> Unit,
  onToggleLike: () -> Unit,
  onCommentClick: () -> Unit,
  onShareClick: () -> Unit,
  onToggleFavourite: () -> Unit,
  onMoreClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    FeedCreatorAvatarWithFollow(
      avatarUrl = item.authorAvatarUrl,
      authorFullName = item.authorFullName,
      isFollowing = item.isFollowingAuthor,
      isOwner = item.isOwner,
      onClickFollow = onToggleFollow,
      onClickProfile = onOpenAuthorProfile,
    )

    RailActionItem(
      count = formatCompactCount(item.likesCount),
      onClick = onToggleLike,
      testTag = "feed_like_button",
      contentDescription = stringResource(R.string.cd_feed_like),
    ) {
      FeedHeartSvgIcon(liked = item.isLiked, size = 32.dp)
    }

    RailActionItem(
      count = formatCompactCount(item.commentsCount),
      onClick = onCommentClick,
      testTag = "feed_comment_button",
      contentDescription = stringResource(R.string.cd_feed_comment),
    ) {
      FeedCommentBubbleSvgIcon(size = 30.dp)
    }

    RailActionItem(
      count = formatCompactCount(item.sharesCount),
      onClick = onShareClick,
      testTag = "feed_share_button",
      contentDescription = stringResource(R.string.cd_feed_share),
    ) {
      FeedPaperPlaneSvgIcon(tint = Color.White, size = 29.dp)
    }

    RailActionItem(
      count = formatCompactCount(item.favouritesCount),
      onClick = onToggleFavourite,
      testTag = "feed_favourite_button",
      contentDescription = stringResource(R.string.cd_feed_favourite),
    ) {
      FeedBookmarkSvgIcon(saved = item.isFavourited, size = 28.dp)
    }

    Box(
      modifier = Modifier
        .minimumInteractiveComponentSize()
        .clip(CircleShape)
        .clickable(onClick = onMoreClick)
        .padding(8.dp)
        .testTag("feed_more_button"),
      contentAlignment = Alignment.Center,
    ) {
      FeedMoreDotsSvgIcon(tint = Color.White, size = 26.dp)
    }
  }
}

@Composable
private fun RailActionItem(
  count: String,
  onClick: () -> Unit,
  testTag: String,
  contentDescription: String,
  icon: @Composable () -> Unit,
) {
  Column(
    modifier = Modifier
      .minimumInteractiveComponentSize()
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 6.dp, vertical = 2.dp)
      .testTag(testTag),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    icon()
    Spacer(modifier = Modifier.height(3.dp))
    Text(
      text = count,
      color = TelefamPureWhite,
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
    )
  }
}
