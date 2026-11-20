package telefam.company.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telefam.company.app.R
import telefam.company.core.PostBackgroundTheme
import telefam.company.core.PostFontOption
import telefam.company.core.PostSongTrack
import telefam.company.core.PostThemeKind
import telefam.company.ui.theme.CaveatHandFontFamily
import telefam.company.ui.theme.CourierTypewriterFontFamily
import telefam.company.ui.theme.FredokaRoundedFontFamily
import telefam.company.ui.theme.LocalTelefamDarkTheme
import telefam.company.ui.theme.MerriweatherItalicFontFamily
import telefam.company.ui.theme.MontserratBoldFontFamily
import telefam.company.ui.theme.OswaldDisplayFontFamily
import telefam.company.ui.theme.PlayfairSerifFontFamily
import telefam.company.ui.theme.PlusJakartaFontFamily
import telefam.company.ui.theme.SpaceMonoFontFamily
import telefam.company.ui.theme.TelefamBlushWave
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamDarkBlush

fun postFontFamily(option: PostFontOption): FontFamily = when (option) {
  PostFontOption.CLASSIC -> PlusJakartaFontFamily
  PostFontOption.SERIF -> PlayfairSerifFontFamily
  PostFontOption.HAND -> CaveatHandFontFamily
  PostFontOption.TYPEWRITER -> CourierTypewriterFontFamily
  PostFontOption.BOLD -> MontserratBoldFontFamily
  PostFontOption.MONO -> SpaceMonoFontFamily
  PostFontOption.DISPLAY -> OswaldDisplayFontFamily
  PostFontOption.ROUNDED -> FredokaRoundedFontFamily
  PostFontOption.ITALIC -> MerriweatherItalicFontFamily
}

fun postCardTextStyle(option: PostFontOption, inkColor: Color): TextStyle = when (option) {
  PostFontOption.CLASSIC -> TextStyle(
    fontFamily = PlusJakartaFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 25.sp,
    lineHeight = 32.sp,
    color = inkColor,
  )
  PostFontOption.SERIF -> TextStyle(
    fontFamily = PlayfairSerifFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 26.sp,
    lineHeight = 34.sp,
    color = inkColor,
  )
  PostFontOption.HAND -> TextStyle(
    fontFamily = CaveatHandFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 31.sp,
    lineHeight = 36.sp,
    color = inkColor,
  )
  PostFontOption.TYPEWRITER -> TextStyle(
    fontFamily = CourierTypewriterFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 30.sp,
    color = inkColor,
  )
  PostFontOption.BOLD -> TextStyle(
    fontFamily = MontserratBoldFontFamily,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 25.sp,
    lineHeight = 32.sp,
    letterSpacing = 0.4.sp,
    color = inkColor,
  )
  PostFontOption.MONO -> TextStyle(
    fontFamily = SpaceMonoFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 21.sp,
    lineHeight = 29.sp,
    color = inkColor,
  )
  PostFontOption.DISPLAY -> TextStyle(
    fontFamily = OswaldDisplayFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 34.sp,
    letterSpacing = 0.6.sp,
    color = inkColor,
  )
  PostFontOption.ROUNDED -> TextStyle(
    fontFamily = FredokaRoundedFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 26.sp,
    lineHeight = 33.sp,
    color = inkColor,
  )
  PostFontOption.ITALIC -> TextStyle(
    fontFamily = MerriweatherItalicFontFamily,
    fontWeight = FontWeight.Medium,
    fontStyle = FontStyle.Italic,
    fontSize = 24.sp,
    lineHeight = 32.sp,
    color = inkColor,
  )
}

@Composable
fun PostThemeCanvasBox(
  theme: PostBackgroundTheme,
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit = {},
) {
  val start = Color(theme.startColorArgb)
  val end = Color(theme.endColorArgb)
  val accent = Color(theme.accentColorArgb)

  Box(
    modifier = modifier.background(
      brush = Brush.linearGradient(colors = listOf(start, end)),
    ),
  ) {
    if (theme.kind == PostThemeKind.IMAGE_SCENE) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        when (theme.id) {
          "scene_aurora" -> {
            val aurora = Path().apply {
              moveTo(0f, h * 0.42f)
              cubicTo(w * 0.25f, h * 0.12f, w * 0.65f, h * 0.56f, w, h * 0.24f)
              lineTo(w, h * 0.48f)
              cubicTo(w * 0.65f, h * 0.72f, w * 0.30f, h * 0.32f, 0f, h * 0.60f)
              close()
            }
            drawPath(aurora, color = accent.copy(alpha = 0.32f))
            val ridge = Path().apply {
              moveTo(0f, h)
              lineTo(0f, h * 0.78f)
              lineTo(w * 0.28f, h * 0.60f)
              lineTo(w * 0.55f, h * 0.74f)
              lineTo(w * 0.82f, h * 0.56f)
              lineTo(w, h * 0.70f)
              lineTo(w, h)
              close()
            }
            drawPath(ridge, color = Color(0xFF06101B).copy(alpha = 0.65f))
          }
          "scene_crimson_silk" -> {
            val wave1 = Path().apply {
              moveTo(0f, h * 0.30f)
              cubicTo(w * 0.35f, h * 0.05f, w * 0.65f, h * 0.55f, w, h * 0.20f)
              lineTo(w, h)
              lineTo(0f, h)
              close()
            }
            drawPath(wave1, color = accent.copy(alpha = 0.20f))
            val wave2 = Path().apply {
              moveTo(0f, h * 0.62f)
              cubicTo(w * 0.40f, h * 0.38f, w * 0.75f, h * 0.86f, w, h * 0.52f)
              lineTo(w, h)
              lineTo(0f, h)
              close()
            }
            drawPath(wave2, color = Color.Black.copy(alpha = 0.18f))
          }
          "scene_dunes" -> {
            drawCircle(
              color = accent.copy(alpha = 0.50f),
              radius = w * 0.14f,
              center = Offset(w * 0.76f, h * 0.26f),
            )
            val dune1 = Path().apply {
              moveTo(0f, h * 0.68f)
              cubicTo(w * 0.30f, h * 0.52f, w * 0.70f, h * 0.80f, w, h * 0.60f)
              lineTo(w, h)
              lineTo(0f, h)
              close()
            }
            drawPath(dune1, color = Color(0xFF531814).copy(alpha = 0.55f))
            val dune2 = Path().apply {
              moveTo(0f, h * 0.84f)
              cubicTo(w * 0.45f, h * 0.72f, w * 0.78f, h * 0.92f, w, h * 0.76f)
              lineTo(w, h)
              lineTo(0f, h)
              close()
            }
            drawPath(dune2, color = Color(0xFF330D0B).copy(alpha = 0.65f))
          }
          "scene_waves" -> {
            for (idx in 0..2) {
              val base = h * (0.48f + idx * 0.16f)
              val p = Path().apply {
                moveTo(0f, base)
                cubicTo(w * 0.33f, base - h * 0.10f, w * 0.66f, base + h * 0.10f, w, base - h * 0.04f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
              }
              drawPath(p, color = accent.copy(alpha = 0.16f + idx * 0.08f))
            }
          }
          "scene_stars" -> {
            val pts = listOf(
              Offset(w * 0.16f, h * 0.20f),
              Offset(w * 0.32f, h * 0.14f),
              Offset(w * 0.82f, h * 0.22f),
              Offset(w * 0.72f, h * 0.36f),
              Offset(w * 0.20f, h * 0.76f),
              Offset(w * 0.84f, h * 0.78f),
              Offset(w * 0.54f, h * 0.16f),
            )
            pts.forEachIndexed { idx, pt ->
              drawCircle(
                color = accent.copy(alpha = if (idx % 2 == 0) 0.75f else 0.45f),
                radius = (2.5f + (idx % 3) * 1.4f),
                center = pt,
              )
            }
            drawCircle(
              color = accent.copy(alpha = 0.22f),
              radius = w * 0.18f,
              center = Offset(w * 0.84f, h * 0.18f),
            )
          }
          "scene_botanical" -> {
            drawCircle(
              color = accent.copy(alpha = 0.18f),
              radius = w * 0.26f,
              center = Offset(w * 0.12f, h * 0.86f),
            )
            drawCircle(
              color = accent.copy(alpha = 0.16f),
              radius = w * 0.22f,
              center = Offset(w * 0.88f, h * 0.14f),
            )
          }
          "scene_paper" -> {
            drawRoundRect(
              color = accent.copy(alpha = 0.55f),
              topLeft = Offset(w * 0.05f, h * 0.05f),
              size = androidx.compose.ui.geometry.Size(w * 0.90f, h * 0.90f),
              cornerRadius = CornerRadius(24f, 24f),
              style = Stroke(width = 2.5f),
            )
          }
        }
      }
    }
    content()
  }
}

@Composable
fun DashedMediaUploadCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  testTag: String = "post_media_upload_box",
) {
  val isDark = LocalTelefamDarkTheme.current
  val blush = if (isDark) TelefamDarkBlush else TelefamBlushWave
  val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)
  val radius = 22.dp

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(310.dp)
      .clip(RoundedCornerShape(radius))
      .background(MaterialTheme.colorScheme.surface)
      .drawBehind {
        drawRoundRect(
          color = borderColor,
          style = Stroke(
            width = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f), 0f),
          ),
          cornerRadius = CornerRadius(radius.toPx(), radius.toPx()),
        )
      }
      .clickable(onClick = onClick)
      .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(CircleShape)
          .background(blush),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = TelefamCrimson,
          modifier = Modifier.size(26.dp),
        )
      }
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun PostMetadataRowsCard(
  selectedSong: PostSongTrack?,
  isPlayingSongPreview: Boolean,
  taggedCount: Int,
  taggedPreviewNames: String,
  selectedHashtags: List<String>,
  onSongClick: () -> Unit,
  onToggleSongPreview: () -> Unit,
  onClearSong: () -> Unit,
  onTagPeopleClick: () -> Unit,
  onHashtagsClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = LocalTelefamDarkTheme.current
  val blush = if (isDark) TelefamDarkBlush else TelefamBlushWave

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column {
      // 1. Song row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(onClick = onSongClick)
          .padding(horizontal = 14.dp, vertical = 12.dp)
          .testTag("post_row_song"),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(blush),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = TelefamCrimson,
            modifier = Modifier.size(18.dp),
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = selectedSong?.title ?: stringResource(R.string.post_song),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (selectedSong != null) {
            Text(
              text = selectedSong.artist,
              style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        if (selectedSong != null && selectedSong.previewUrl.isNotBlank()) {
          IconButton(
            onClick = onToggleSongPreview,
            modifier = Modifier.size(36.dp),
          ) {
            Icon(
              imageVector = if (isPlayingSongPreview) Icons.Filled.Pause else Icons.Filled.PlayArrow,
              contentDescription = null,
              tint = TelefamCrimson,
              modifier = Modifier.size(20.dp),
            )
          }
        }
        if (selectedSong != null) {
          IconButton(
            onClick = onClearSong,
            modifier = Modifier.size(36.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Close,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
          }
        } else {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = stringResource(R.string.post_add),
              style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))

      // 2. Tag people row
      MetadataSimpleRow(
        icon = Icons.Filled.PersonAddAlt1,
        blush = blush,
        title = stringResource(R.string.post_tag_people),
        valueSummary = if (taggedCount > 0) taggedPreviewNames else stringResource(R.string.post_add),
        highlightValue = taggedCount > 0,
        onClick = onTagPeopleClick,
        testTag = "post_row_tag_people",
      )

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))

      // 3. Hashtags row
      val tagsSummary = if (selectedHashtags.isNotEmpty()) {
        selectedHashtags.joinToString(" ") { "#$it" }
      } else {
        stringResource(R.string.post_add)
      }
      MetadataSimpleRow(
        icon = Icons.Filled.Tag,
        blush = blush,
        title = stringResource(R.string.post_hashtags),
        valueSummary = tagsSummary,
        highlightValue = selectedHashtags.isNotEmpty(),
        onClick = onHashtagsClick,
        testTag = "post_row_hashtags",
      )
    }
  }
}

@Composable
private fun MetadataSimpleRow(
  icon: ImageVector,
  blush: Color,
  title: String,
  valueSummary: String,
  highlightValue: Boolean,
  onClick: () -> Unit,
  testTag: String,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 12.dp)
      .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .size(34.dp)
        .clip(CircleShape)
        .background(blush),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = TelefamCrimson,
        modifier = Modifier.size(18.dp),
      )
    }
    Spacer(modifier = Modifier.width(12.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(1f),
    )
    Text(
      text = valueSummary,
      style = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 13.5.sp,
        fontWeight = if (highlightValue) FontWeight.SemiBold else FontWeight.Normal,
      ),
      color = if (highlightValue) TelefamCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Spacer(modifier = Modifier.width(4.dp))
    Icon(
      imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(18.dp),
    )
  }
}
