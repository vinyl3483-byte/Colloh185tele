package telefam.company.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.telefam.company.app.R
import telefam.company.core.PostPrivacy
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.viewmodel.FeedUiItem
import kotlin.math.cos
import kotlin.math.sin

val FeedHeartActiveColor = Color(0xFFFF3358)
val FeedBookmarkActiveColor = Color(0xFFFFC107)
val FeedLoaderGreen = Color(0xFF16A34A)

/**
 * Custom 3-dot spinning loader in Red, White, and Green for poor internet or video buffering.
 */
@Composable
fun TelefamTricolorSpinner(
  modifier: Modifier = Modifier,
  size: Dp = 42.dp,
  reducedMotion: Boolean = false,
) {
  val infinite = rememberInfiniteTransition(label = "tricolor_spinner")
  val animatedAngle by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 920, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "tricolor_rotation",
  )
  val angle = if (reducedMotion) 0f else animatedAngle

  Canvas(
    modifier = modifier
      .size(size)
      .rotate(angle)
      .testTag("feed_tricolor_spinner"),
  ) {
    val cx = this.size.width / 2f
    val cy = this.size.height / 2f
    val orbitRadius = this.size.minDimension * 0.31f
    val dotRadius = this.size.minDimension * 0.135f
    val colors = listOf(TelefamCrimson, Color.White, FeedLoaderGreen)

    for (i in 0 until 3) {
      val rad = Math.toRadians((i * 120.0) - 90.0)
      val x = cx + (orbitRadius * cos(rad)).toFloat()
      val y = cy + (orbitRadius * sin(rad)).toFloat()
      drawCircle(
        color = colors[i].copy(alpha = 0.28f),
        radius = dotRadius * 1.35f,
        center = Offset(x, y),
      )
      drawCircle(
        color = colors[i],
        radius = dotRadius,
        center = Offset(x, y),
      )
    }
  }
}

/**
 * Custom LIVE TV vector badge matching reference screenshot top-left.
 */
@Composable
fun FeedLiveTvIcon(
  tint: Color = Color.White,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier.size(28.dp)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = w * 0.085f

    // Dual antennae
    drawLine(
      color = tint,
      start = Offset(w * 0.34f, h * 0.08f),
      end = Offset(w * 0.48f, h * 0.25f),
      strokeWidth = stroke,
      cap = StrokeCap.Round,
    )
    drawLine(
      color = tint,
      start = Offset(w * 0.66f, h * 0.08f),
      end = Offset(w * 0.52f, h * 0.25f),
      strokeWidth = stroke,
      cap = StrokeCap.Round,
    )
    // Rounded TV monitor frame
    drawRoundRect(
      color = tint,
      topLeft = Offset(w * 0.08f, h * 0.26f),
      size = Size(w * 0.84f, h * 0.64f),
      cornerRadius = CornerRadius(w * 0.16f, w * 0.16f),
      style = Stroke(width = stroke),
    )
  }
}

/**
 * Custom Sparkle emblem for "You might like this" pill matching screenshot.
 */
@Composable
fun TelefamSparkleEmblem(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .size(18.dp)
      .clip(CircleShape)
      .background(
        Brush.linearGradient(listOf(Color(0xFFE11D48), TelefamCrimson)),
      ),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.size(11.dp)) {
      val cx = size.width / 2f
      val cy = size.height / 2f
      val path = Path().apply {
        moveTo(cx, 0f)
        quadraticTo(cx, cy, size.width, cy)
        quadraticTo(cx, cy, cx, size.height)
        quadraticTo(cx, cy, 0f, cy)
        quadraticTo(cx, cy, cx, 0f)
        close()
      }
      drawPath(path, color = Color.White)
    }
  }
}

/**
 * Custom animated 3-bar equalizer SVG beside song/audio row.
 */
@Composable
fun AnimatedAudioBarsIcon(
  isPlaying: Boolean,
  tint: Color = Color.White,
  reducedMotion: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val infinite = rememberInfiniteTransition(label = "eq_bars")
  val b1 by infinite.animateFloat(
    initialValue = 0.35f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
    label = "bar1",
  )
  val b2 by infinite.animateFloat(
    initialValue = 1f,
    targetValue = 0.3f,
    animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
    label = "bar2",
  )
  val b3 by infinite.animateFloat(
    initialValue = 0.45f,
    targetValue = 0.9f,
    animationSpec = infiniteRepeatable(tween(470), RepeatMode.Reverse),
    label = "bar3",
  )

  val animate = isPlaying && !reducedMotion
  val h1 = if (animate) b1 else 0.5f
  val h2 = if (animate) b2 else 0.85f
  val h3 = if (animate) b3 else 0.45f

  Canvas(modifier = modifier.size(16.dp)) {
    val w = size.width
    val h = size.height
    val barW = w * 0.2f
    val gap = w * 0.16f
    val heights = listOf(h1, h2, h3)
    heights.forEachIndexed { idx, frac ->
      val x = idx * (barW + gap) + barW * 0.3f
      val barH = h * frac
      drawRoundRect(
        color = tint,
        topLeft = Offset(x, (h - barH) / 2f),
        size = Size(barW, barH),
        cornerRadius = CornerRadius(barW / 2f, barW / 2f),
      )
    }
  }
}

@Composable
fun FeedSearchSvgIcon(tint: Color = Color.White, size: Dp = 25.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val stroke = w * 0.095f
    drawCircle(
      color = tint,
      radius = w * 0.32f,
      center = Offset(w * 0.43f, w * 0.43f),
      style = Stroke(width = stroke),
    )
    drawLine(
      color = tint,
      start = Offset(w * 0.66f, w * 0.66f),
      end = Offset(w * 0.88f, w * 0.88f),
      strokeWidth = stroke,
      cap = StrokeCap.Round,
    )
  }
}

@Composable
fun FeedHeartSvgIcon(liked: Boolean, size: Dp = 32.dp) {
  val scale by animateFloatAsState(
    targetValue = if (liked) 1.12f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "heart_pop",
  )
  val tint = if (liked) FeedHeartActiveColor else Color.White

  Canvas(modifier = Modifier.size(size).scale(scale)) {
    val w = this.size.width
    val h = this.size.height
    val path = Path().apply {
      moveTo(w * 0.50f, h * 0.86f)
      cubicTo(w * 0.14f, h * 0.64f, w * 0.04f, h * 0.35f, w * 0.24f, h * 0.18f)
      cubicTo(w * 0.36f, h * 0.08f, w * 0.48f, h * 0.18f, w * 0.50f, h * 0.28f)
      cubicTo(w * 0.52f, h * 0.18f, w * 0.64f, h * 0.08f, w * 0.76f, h * 0.18f)
      cubicTo(w * 0.96f, h * 0.35f, w * 0.86f, h * 0.64f, w * 0.50f, h * 0.86f)
      close()
    }
    if (liked) {
      drawPath(path, color = tint)
    } else {
      drawPath(path, color = tint.copy(alpha = 0.94f), style = Stroke(width = w * 0.085f, join = StrokeJoin.Round))
    }
  }
}

@Composable
fun FeedCommentBubbleSvgIcon(size: Dp = 31.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val bubble = Path().apply {
      moveTo(w * 0.14f, h * 0.74f)
      lineTo(w * 0.10f, h * 0.90f)
      lineTo(w * 0.30f, h * 0.82f)
      cubicTo(w * 0.58f, h * 0.88f, w * 0.90f, h * 0.74f, w * 0.90f, h * 0.46f)
      cubicTo(w * 0.90f, h * 0.22f, w * 0.68f, h * 0.12f, w * 0.48f, h * 0.12f)
      cubicTo(w * 0.24f, h * 0.12f, w * 0.08f, h * 0.26f, w * 0.08f, h * 0.48f)
      cubicTo(w * 0.08f, h * 0.58f, w * 0.11f, h * 0.67f, w * 0.14f, h * 0.74f)
      close()
    }
    drawPath(bubble, color = Color.White)
    val dotR = w * 0.052f
    drawCircle(color = Color(0xFF1A1C1E), radius = dotR, center = Offset(w * 0.34f, h * 0.48f))
    drawCircle(color = Color(0xFF1A1C1E), radius = dotR, center = Offset(w * 0.50f, h * 0.48f))
    drawCircle(color = Color(0xFF1A1C1E), radius = dotR, center = Offset(w * 0.66f, h * 0.48f))
  }
}

@Composable
fun FeedPaperPlaneSvgIcon(tint: Color = Color.White, size: Dp = 30.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val plane = Path().apply {
      moveTo(w * 0.90f, h * 0.14f)
      lineTo(w * 0.10f, h * 0.44f)
      lineTo(w * 0.46f, h * 0.56f)
      lineTo(w * 0.62f, h * 0.88f)
      close()
    }
    drawPath(plane, color = tint, style = stroke)
    drawLine(
      color = tint,
      start = Offset(w * 0.46f, h * 0.56f),
      end = Offset(w * 0.88f, h * 0.16f),
      strokeWidth = w * 0.075f,
      cap = StrokeCap.Round,
    )
  }
}

@Composable
fun FeedBookmarkSvgIcon(saved: Boolean, size: Dp = 29.dp) {
  val tint = if (saved) FeedBookmarkActiveColor else Color.White
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val path = Path().apply {
      moveTo(w * 0.22f, h * 0.14f)
      lineTo(w * 0.78f, h * 0.14f)
      lineTo(w * 0.78f, h * 0.88f)
      lineTo(w * 0.50f, h * 0.68f)
      lineTo(w * 0.22f, h * 0.88f)
      close()
    }
    if (saved) {
      drawPath(path, color = tint)
    } else {
      drawPath(path, color = tint, style = Stroke(width = w * 0.085f, join = StrokeJoin.Round))
    }
  }
}

@Composable
fun FeedMoreDotsSvgIcon(tint: Color = Color.White, size: Dp = 28.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val cy = this.size.height / 2f
    val r = w * 0.085f
    drawCircle(color = tint, radius = r, center = Offset(w * 0.22f, cy))
    drawCircle(color = tint, radius = r, center = Offset(w * 0.50f, cy))
    drawCircle(color = tint, radius = r, center = Offset(w * 0.78f, cy))
  }
}

@Composable
fun FeedMusicNoteSvgIcon(tint: Color = Color.White, size: Dp = 16.dp) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    drawCircle(color = tint, radius = w * 0.14f, center = Offset(w * 0.25f, h * 0.78f))
    drawCircle(color = tint, radius = w * 0.14f, center = Offset(w * 0.72f, h * 0.70f))
    val stem = Path().apply {
      moveTo(w * 0.36f, h * 0.78f)
      lineTo(w * 0.36f, h * 0.20f)
      lineTo(w * 0.84f, h * 0.12f)
      lineTo(w * 0.84f, h * 0.70f)
    }
    drawPath(stem, color = tint, style = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round))
  }
}

/**
 * Creator Avatar with crimson "+" follow badge matching uploaded screenshot.
 */
@Composable
fun FeedCreatorAvatarWithFollow(
  avatarUrl: String?,
  authorFullName: String,
  isFollowing: Boolean,
  isOwner: Boolean,
  onClickFollow: () -> Unit,
  onClickProfile: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.size(width = 50.dp, height = 58.dp),
    contentAlignment = Alignment.TopCenter,
  ) {
    Box(
      modifier = Modifier
        .size(46.dp)
        .clip(CircleShape)
        .border(1.8.dp, Color.White, CircleShape)
        .background(Color(0xFF242833))
        .clickable(onClick = onClickProfile)
        .testTag("feed_author_avatar"),
      contentAlignment = Alignment.Center,
    ) {
      val initial = authorFullName.trim().firstOrNull()?.uppercase() ?: "T"
      Text(
        text = initial,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
      )
      if (!avatarUrl.isNullOrBlank()) {
        AsyncImage(
          model = avatarUrl,
          contentDescription = authorFullName,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }
    }

    if (!isOwner) {
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .offset(y = (-2).dp)
          .size(22.dp)
          .clip(CircleShape)
          .background(if (isFollowing) Color(0xFF1F2937) else TelefamCrimson)
          .border(1.2.dp, Color.White, CircleShape)
          .clickable(onClick = onClickFollow)
          .testTag("feed_follow_badge"),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = if (isFollowing) Icons.Filled.Check else Icons.Filled.Add,
          contentDescription = if (isFollowing) stringResource(R.string.contacts_action_following) else stringResource(R.string.contacts_action_follow),
          tint = Color.White,
          modifier = Modifier.size(13.dp),
        )
      }
    }
  }
}

@Composable
fun FeedDoubleTapHeartOverlay(visible: Boolean, modifier: Modifier = Modifier) {
  AnimatedVisibility(
    visible = visible,
    enter = scaleIn(initialScale = 0.45f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
    exit = scaleOut(targetScale = 1.45f, animationSpec = tween(260, easing = FastOutSlowInEasing)) + fadeOut(),
    modifier = modifier,
  ) {
    FeedHeartSvgIcon(liked = true, size = 96.dp)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedShareBottomSheet(
  item: FeedUiItem,
  onShareViaApps: () -> Unit,
  onCopyLink: () -> Unit,
  onToggleRepost: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = stringResource(R.string.feed_share_title),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
      )
      SheetActionRow(
        icon = Icons.Filled.Share,
        label = stringResource(R.string.feed_share_apps),
        onClick = {
          onShareViaApps()
          onDismiss()
        },
        testTag = "feed_share_apps_row",
      )
      SheetActionRow(
        icon = Icons.Filled.ContentCopy,
        label = stringResource(R.string.feed_copy_link),
        onClick = {
          onCopyLink()
          onDismiss()
        },
        testTag = "feed_copy_link_row",
      )
      SheetActionRow(
        icon = Icons.Filled.Repeat,
        label = if (item.isReposted) stringResource(R.string.feed_action_unrepost) else stringResource(R.string.feed_action_repost),
        onClick = {
          onToggleRepost()
          onDismiss()
        },
        testTag = "feed_share_repost_row",
      )
      Spacer(modifier = Modifier.height(14.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedMoreOptionsSheet(
  item: FeedUiItem,
  onRepost: () -> Unit,
  onDownload: () -> Unit,
  onNotInterested: () -> Unit,
  onMute: () -> Unit = {},
  onReport: () -> Unit,
  onBlock: () -> Unit,
  onEdit: () -> Unit,
  onChangePrivacy: (PostPrivacy) -> Unit = {},
  onDelete: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var showPrivacySubmenu by remember(item.postId) { mutableStateOf(false) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (item.isOwner) {
        SheetActionRow(
          icon = Icons.Filled.Edit,
          label = stringResource(R.string.feed_action_edit_video),
          onClick = {
            onEdit()
            onDismiss()
          },
          testTag = "feed_menu_edit",
        )

        // Expandable Privacy Submenu: Public / Friends / Private
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { showPrivacySubmenu = !showPrivacySubmenu }
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .testTag("feed_menu_privacy"),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          FeedPrivacySvgIcon(
            privacy = item.privacy,
            tint = MaterialTheme.colorScheme.onSurface,
            size = 21.dp,
          )
          Spacer(modifier = Modifier.width(14.dp))
          Text(
            text = stringResource(R.string.feed_action_privacy),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
          )
          Surface(
            color = TelefamCrimson.copy(alpha = 0.14f),
            shape = RoundedCornerShape(10.dp),
          ) {
            Text(
              text = when (item.privacy) {
                PostPrivacy.PUBLIC -> stringResource(R.string.feed_privacy_public)
                PostPrivacy.FRIENDS -> stringResource(R.string.feed_privacy_friends)
                PostPrivacy.PRIVATE -> stringResource(R.string.feed_privacy_private)
              },
              color = TelefamCrimson,
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }

        AnimatedVisibility(visible = showPrivacySubmenu) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 18.dp, end = 4.dp, bottom = 4.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
              .padding(vertical = 4.dp),
          ) {
            PostPrivacy.entries.forEach { option ->
              val selected = item.privacy == option
              val optionLabel = when (option) {
                PostPrivacy.PUBLIC -> stringResource(R.string.feed_privacy_public)
                PostPrivacy.FRIENDS -> stringResource(R.string.feed_privacy_friends)
                PostPrivacy.PRIVATE -> stringResource(R.string.feed_privacy_private)
              }
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    onChangePrivacy(option)
                    onDismiss()
                  }
                  .padding(horizontal = 14.dp, vertical = 10.dp)
                  .testTag("feed_privacy_option_${option.apiValue}"),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                FeedPrivacySvgIcon(
                  privacy = option,
                  tint = if (selected) TelefamCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                  size = 18.dp,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text = optionLabel,
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  ),
                  color = if (selected) TelefamCrimson else MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.weight(1f),
                )
                if (selected) {
                  Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = TelefamCrimson,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
            }
          }
        }

        SheetActionRow(
          icon = Icons.Filled.DeleteOutline,
          label = stringResource(R.string.feed_action_delete_video),
          tint = TelefamCrimson,
          onClick = {
            onDelete()
            onDismiss()
          },
          testTag = "feed_menu_delete",
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      }

      SheetActionRow(
        icon = Icons.Filled.Repeat,
        label = if (item.isReposted) stringResource(R.string.feed_action_unrepost) else stringResource(R.string.feed_action_repost),
        onClick = {
          onRepost()
          onDismiss()
        },
        testTag = "feed_menu_repost",
      )
      if (item.mode == telefam.company.core.PostMode.VIDEO && item.primaryStreamUrl.isNotBlank()) {
        SheetActionRow(
          icon = Icons.Filled.Download,
          label = stringResource(R.string.feed_action_download),
          onClick = {
            onDownload()
            onDismiss()
          },
          testTag = "feed_menu_download",
        )
      }
      if (!item.isOwner) {
        SheetActionRow(
          icon = Icons.Filled.VisibilityOff,
          label = stringResource(R.string.feed_action_not_interested),
          onClick = {
            onNotInterested()
            onDismiss()
          },
          testTag = "feed_menu_not_interested",
        )
        SheetActionRow(
          icon = Icons.Filled.VisibilityOff,
          label = stringResource(R.string.feed_action_mute_user),
          onClick = {
            onMute()
            onDismiss()
          },
          testTag = "feed_menu_mute",
        )
        SheetActionRow(
          icon = Icons.Filled.Flag,
          label = stringResource(R.string.feed_action_report),
          onClick = {
            onReport()
            onDismiss()
          },
          testTag = "feed_menu_report",
        )
        SheetActionRow(
          icon = Icons.Outlined.Block,
          label = stringResource(R.string.feed_action_block),
          tint = TelefamCrimson,
          onClick = {
            onBlock()
            onDismiss()
          },
          testTag = "feed_menu_block",
        )
      }
      Spacer(modifier = Modifier.height(14.dp))
    }
  }
}

@Composable
fun FeedReportReasonDialog(
  onSelectReason: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val reasons = listOf(
    stringResource(R.string.feed_report_reason_spam),
    stringResource(R.string.feed_report_reason_harmful),
    stringResource(R.string.feed_report_reason_copyright),
    stringResource(R.string.feed_report_reason_other),
  )
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.feed_action_report),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        reasons.forEach { reason ->
          OutlinedButton(
            onClick = {
              onSelectReason(reason)
              onDismiss()
            },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = reason,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.profile_crop_cancel))
      }
    },
  )
}

@Composable
private fun SheetActionRow(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  tint: Color = MaterialTheme.colorScheme.onSurface,
  testTag: String,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 12.dp)
      .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = tint,
      modifier = Modifier.size(22.dp),
    )
    Spacer(modifier = Modifier.width(14.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
      color = tint,
    )
  }
}

@Composable
private fun FeedPrivacySvgIcon(
  privacy: PostPrivacy,
  tint: Color,
  size: Dp = 20.dp,
) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val strokeW = w * 0.095f
    val stroke = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (privacy) {
      PostPrivacy.PUBLIC -> {
        drawCircle(color = tint, radius = w * 0.38f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
        drawOval(
          color = tint,
          topLeft = Offset(w * 0.30f, h * 0.12f),
          size = Size(w * 0.40f, h * 0.76f),
          style = Stroke(width = strokeW * 0.85f),
        )
        drawLine(tint, Offset(w * 0.13f, h * 0.50f), Offset(w * 0.87f, h * 0.50f), strokeWidth = strokeW * 0.85f)
      }
      PostPrivacy.FRIENDS -> {
        drawCircle(color = tint, radius = w * 0.14f, center = Offset(w * 0.36f, h * 0.34f), style = stroke)
        val p1 = Path().apply {
          moveTo(w * 0.12f, h * 0.82f)
          quadraticTo(w * 0.16f, h * 0.58f, w * 0.36f, h * 0.58f)
          quadraticTo(w * 0.56f, h * 0.58f, w * 0.60f, h * 0.82f)
        }
        drawPath(p1, color = tint, style = stroke)
        drawCircle(color = tint, radius = w * 0.11f, center = Offset(w * 0.70f, h * 0.38f), style = stroke)
        val p2 = Path().apply {
          moveTo(w * 0.62f, h * 0.60f)
          quadraticTo(w * 0.82f, h * 0.60f, w * 0.88f, h * 0.82f)
        }
        drawPath(p2, color = tint, style = stroke)
      }
      PostPrivacy.PRIVATE -> {
        drawRoundRect(
          color = tint,
          topLeft = Offset(w * 0.20f, h * 0.44f),
          size = Size(w * 0.60f, h * 0.44f),
          cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
          style = stroke,
        )
        val shackle = Path().apply {
          moveTo(w * 0.32f, h * 0.44f)
          lineTo(w * 0.32f, h * 0.30f)
          cubicTo(w * 0.32f, h * 0.12f, w * 0.68f, h * 0.12f, w * 0.68f, h * 0.30f)
          lineTo(w * 0.68f, h * 0.44f)
        }
        drawPath(shackle, color = tint, style = stroke)
      }
    }
  }
}
