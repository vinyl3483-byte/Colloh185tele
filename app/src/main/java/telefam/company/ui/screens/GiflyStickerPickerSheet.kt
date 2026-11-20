package telefam.company.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.Gif
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.telefam.company.app.R
import telefam.company.data.network.GiflyMediaItemDto
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamPureWhite

@Composable
fun GiflyStickerPickerPanel(
  activeTab: String,
  query: String,
  items: List<GiflyMediaItemDto>,
  hasNext: Boolean,
  isLoading: Boolean,
  isLoadingMore: Boolean,
  previewItem: GiflyMediaItemDto?,
  accentColor: Color,
  isDark: Boolean,
  onSelectTab: (String) -> Unit,
  onQueryChanged: (String) -> Unit,
  onLoadNextPage: () -> Unit,
  onPreviewItem: (GiflyMediaItemDto?) -> Unit,
  onSendPreviewItem: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val panelBg = if (isDark) Color(0xFF14171D) else Color(0xFFF6F7FA)
  val pillBg = if (isDark) Color(0xFF20252E) else TelefamPureWhite
  val textPrimary = if (isDark) TelefamPureWhite else Color(0xFF14151A)
  val textSecondary = if (isDark) Color(0xFF9EA5B2) else Color(0xFF5F6674)

  val gridState = rememberLazyGridState()
  val shouldLoadMore by remember(items, hasNext, isLoadingMore) {
    derivedStateOf {
      val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
      hasNext && !isLoadingMore && items.isNotEmpty() && lastVisible >= items.size - 6
    }
  }
  LaunchedEffect(shouldLoadMore) {
    if (shouldLoadMore) onLoadNextPage()
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .height(330.dp)
      .testTag("gifly_picker_panel"),
    color = panelBg,
    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
    shadowElevation = 10.dp,
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 12.dp, vertical = 10.dp),
      ) {
        // Subtabs: Stickers | GIFs | Clips + Close
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          GiflySubtabChip(
            label = stringResource(R.string.chat_tab_stickers),
            icon = Icons.Outlined.Animation,
            selected = activeTab == "stickers",
            accent = accentColor,
            isDark = isDark,
            testTag = "gifly_tab_stickers",
            onClick = { onSelectTab("stickers") },
          )
          GiflySubtabChip(
            label = stringResource(R.string.chat_tab_gifs),
            icon = Icons.Outlined.Gif,
            selected = activeTab == "gifs",
            accent = accentColor,
            isDark = isDark,
            testTag = "gifly_tab_gifs",
            onClick = { onSelectTab("gifs") },
          )
          GiflySubtabChip(
            label = stringResource(R.string.chat_tab_clips),
            icon = Icons.Outlined.Movie,
            selected = activeTab == "clips",
            accent = accentColor,
            isDark = isDark,
            testTag = "gifly_tab_clips",
            onClick = { onSelectTab("clips") },
          )
          Spacer(modifier = Modifier.weight(1f))
          IconButton(
            onClick = onClose,
            modifier = Modifier
              .size(36.dp)
              .testTag("gifly_close_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Close,
              contentDescription = stringResource(R.string.cd_back),
              tint = textSecondary,
              modifier = Modifier.size(18.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search input inside picker
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(pillBg)
            .padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = textSecondary,
            modifier = Modifier.size(17.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
              Text(
                text = stringResource(R.string.chat_gifly_search_hint),
                fontSize = 13.sp,
                color = textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            BasicTextField(
              value = query,
              onValueChange = onQueryChanged,
              singleLine = true,
              textStyle = TextStyle(fontSize = 13.5.sp, color = textPrimary),
              cursorBrush = SolidColor(accentColor),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("gifly_search_input"),
            )
          }
          if (query.isNotEmpty()) {
            Icon(
              imageVector = Icons.Filled.Close,
              contentDescription = stringResource(R.string.cd_clear_search),
              tint = textSecondary,
              modifier = Modifier
                .size(16.dp)
                .clickable { onQueryChanged("") },
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading && items.isEmpty()) {
          GiflySkeletonGrid(isDark = isDark)
        } else if (items.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = stringResource(R.string.contacts_empty_title),
              style = MaterialTheme.typography.bodyMedium,
              color = textSecondary,
            )
          }
        } else {
          LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 86.dp),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(items, key = { "${activeTab}_${it.id}_${it.slug}" }) { item ->
              val isSticker = activeTab == "stickers"
              val animUrl = item.url.ifBlank { item.previewWebp }.ifBlank { item.preview }
              Box(
                modifier = Modifier
                  .aspectRatio(1f)
                  .clip(RoundedCornerShape(14.dp))
                  // Stickers explicitly have NO solid background box
                  .background(if (isSticker) Color.Transparent else pillBg)
                  .clickable { onPreviewItem(item) }
                  .padding(if (isSticker) 4.dp else 0.dp)
                  .testTag("gifly_item_${item.id}"),
                contentAlignment = Alignment.Center,
              ) {
                AsyncImage(
                  model = animUrl,
                  contentDescription = item.title,
                  contentScale = if (isSticker) ContentScale.Fit else ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
              }
            }
            if (isLoadingMore) {
              item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = accentColor)
                }
              }
            }
          }
        }
      }

      // Tap Preview Overlay + Main Send Button
      AnimatedVisibility(
        visible = previewItem != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize(),
      ) {
        val item = previewItem
        if (item != null) {
          val animUrl = item.urlHd.ifBlank { item.url }.ifBlank { item.previewWebp }.ifBlank { item.preview }
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color.Black.copy(alpha = 0.65f))
              .clickable { onPreviewItem(null) }
              .padding(16.dp),
            contentAlignment = Alignment.Center,
          ) {
            Surface(
              shape = RoundedCornerShape(22.dp),
              color = if (isDark) Color(0xFF1B1F27) else TelefamPureWhite,
              modifier = Modifier
                .fillMaxWidth(0.86f)
                .clickable(enabled = false) {}
                .testTag("gifly_preview_card"),
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = item.title.ifBlank { item.type.uppercase() },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                  )
                  IconButton(onClick = { onPreviewItem(null) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = textSecondary, modifier = Modifier.size(18.dp))
                  }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                  modifier = Modifier
                    .size(144.dp)
                    .background(Color.Transparent),
                  contentAlignment = Alignment.Center,
                ) {
                  AsyncImage(
                    model = animUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                  )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                  onClick = onSendPreviewItem,
                  shape = RoundedCornerShape(22.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = if (accentColor == Color.Transparent) TelefamCrimson else accentColor,
                    contentColor = TelefamPureWhite,
                  ),
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("gifly_preview_send_button"),
                ) {
                  Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(text = stringResource(R.string.cd_send_message), fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun GiflySubtabChip(
  label: String,
  icon: ImageVector,
  selected: Boolean,
  accent: Color,
  isDark: Boolean,
  testTag: String,
  onClick: () -> Unit,
) {
  val bg = if (selected) accent else if (isDark) Color(0xFF20252E) else TelefamPureWhite
  val content = if (selected) TelefamPureWhite else if (isDark) Color(0xFFB5BCC9) else Color(0xFF4B5260)
  Row(
    modifier = Modifier
      .height(34.dp)
      .clip(RoundedCornerShape(17.dp))
      .background(bg)
      .border(1.dp, if (selected) accent else Color.Transparent, RoundedCornerShape(17.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 11.dp)
      .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(imageVector = icon, contentDescription = null, tint = content, modifier = Modifier.size(15.dp))
    Spacer(modifier = Modifier.width(5.dp))
    Text(text = label, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = content)
  }
}

@Composable
private fun GiflySkeletonGrid(isDark: Boolean) {
  val trans = rememberInfiniteTransition(label = "gifly_shimmer")
  val shift by trans.animateFloat(
    initialValue = 0f,
    targetValue = 600f,
    animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
    label = "shimmer_x",
  )
  val base = if (isDark) Color(0xFF212630) else Color(0xFFE3E7EE)
  val hi = if (isDark) Color(0xFF2E3542) else Color(0xFFF3F6FA)
  val brush = Brush.linearGradient(
    colors = listOf(base, hi, base),
    start = Offset(shift - 220f, 0f),
    end = Offset(shift, 220f),
  )
  LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 86.dp),
    modifier = Modifier.fillMaxSize(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items(12) {
      Box(
        modifier = Modifier
          .aspectRatio(1f)
          .clip(RoundedCornerShape(14.dp))
          .background(brush),
      )
    }
  }
}
