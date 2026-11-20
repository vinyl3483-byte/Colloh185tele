package telefam.company.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.telefam.company.app.R
import telefam.company.core.SmartEntity
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.viewmodel.LinkPreviewUiState

@Composable
fun LinkPreviewStack(
  urls: List<SmartEntity.Url>,
  source: String,
  modifier: Modifier = Modifier,
  maxCards: Int = 3,
) {
  if (urls.isEmpty()) return
  val distinctUrls = remember(urls) {
    urls.distinctBy { it.normalizedUrl }.take(maxCards)
  }
  Column(
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier.fillMaxWidth(),
  ) {
    distinctUrls.forEach { urlEntity ->
      LinkPreviewCard(url = urlEntity, source = source)
    }
  }
}

@Composable
fun LinkPreviewCard(
  url: SmartEntity.Url,
  source: String,
  modifier: Modifier = Modifier,
) {
  val actions = LocalSmartTextActions.current
  val previewState = actions.linkPreviews[url.normalizedUrl]
  val isSaved = url.normalizedUrl in actions.savedLinkUrls

  LaunchedEffect(url.normalizedUrl) {
    if (previewState == null) {
      actions.onRequestLinkPreview(url.normalizedUrl)
    }
  }

  when (previewState) {
    null, is LinkPreviewUiState.Loading -> {
      LinkPreviewLoadingSkeleton(
        domain = url.domain,
        modifier = modifier.clickable { actions.onOpenUrl(url.normalizedUrl, source) },
      )
    }
    is LinkPreviewUiState.Loaded -> {
      val data = previewState.preview
      if (data.status == "fallback" && data.imageUrl.isBlank() && data.title == data.domain) {
        CompactFallbackLinkCard(
          url = url.normalizedUrl,
          domain = data.domain,
          isSaved = isSaved,
          onOpen = { actions.onOpenUrl(url.normalizedUrl, source) },
          onToggleSave = { actions.onToggleSaveLink(url.normalizedUrl, source) },
          modifier = modifier,
        )
      } else {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
          modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { actions.onOpenUrl(url.normalizedUrl, source) }
            .testTag("link_preview_card_${data.domain}"),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            var hideHeroImage by remember(data.imageUrl) { mutableStateOf(false) }
            if (data.imageUrl.isNotBlank() && !hideHeroImage) {
              AsyncImage(
                model = data.imageUrl,
                contentDescription = data.title,
                contentScale = ContentScale.Crop,
                onError = { hideHeroImage = true },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(144.dp),
              )
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  FaviconBadge(faviconUrl = data.faviconUrl, domain = data.domain)
                  Text(
                    text = data.domain,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                  text = data.title.ifBlank { data.domain },
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
                if (data.description.isNotBlank() && data.description != data.title) {
                  Spacer(Modifier.height(2.dp))
                  Text(
                    text = data.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }

              IconButton(
                onClick = { actions.onToggleSaveLink(url.normalizedUrl, source) },
                modifier = Modifier
                  .size(48.dp)
                  .testTag("link_preview_bookmark_${data.domain}"),
              ) {
                Icon(
                  imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                  contentDescription = if (isSaved) {
                    stringResource(R.string.browser_saved)
                  } else {
                    stringResource(R.string.browser_save_link)
                  },
                  tint = if (isSaved) TelefamCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CompactFallbackLinkCard(
  url: String,
  domain: String,
  isSaved: Boolean,
  onOpen: () -> Unit,
  onToggleSave: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onOpen)
      .testTag("link_preview_fallback_$domain"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
    ) {
      Surface(
        shape = CircleShape,
        color = TelefamCrimson.copy(alpha = 0.12f),
        modifier = Modifier.size(34.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Rounded.Public,
            contentDescription = domain,
            tint = TelefamCrimson,
            modifier = Modifier.size(18.dp),
          )
        }
      }
      Spacer(Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = domain,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = url,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      IconButton(onClick = onToggleSave, modifier = Modifier.size(48.dp)) {
        Icon(
          imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
          contentDescription = stringResource(R.string.browser_save_link),
          tint = if (isSaved) TelefamCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
        contentDescription = stringResource(R.string.link_preview_fallback),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
          .padding(end = 8.dp)
          .size(18.dp),
      )
    }
  }
}

@Composable
private fun LinkPreviewLoadingSkeleton(
  domain: String,
  modifier: Modifier = Modifier,
) {
  val transition = rememberInfiniteTransition(label = "link_preview_skeleton")
  val shift by transition.animateFloat(
    initialValue = 0.25f,
    targetValue = 0.7f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 850, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "skeleton_alpha",
  )
  val shimmerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = shift * 0.24f)

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    modifier = modifier
      .fillMaxWidth()
      .testTag("link_preview_skeleton_$domain"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.Public,
          contentDescription = domain,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(14.dp),
        )
        Text(
          text = domain,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Box(
        modifier = Modifier
          .fillMaxWidth(0.75f)
          .height(14.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(Brush.horizontalGradient(listOf(shimmerColor, shimmerColor.copy(alpha = 0.08f)))),
      )
      Box(
        modifier = Modifier
          .fillMaxWidth(0.55f)
          .height(11.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(shimmerColor),
      )
    }
  }
}

@Composable
fun FaviconBadge(
  faviconUrl: String,
  domain: String,
  modifier: Modifier = Modifier,
) {
  var failed by remember(faviconUrl) { mutableStateOf(false) }
  if (faviconUrl.isNotBlank() && !failed) {
    AsyncImage(
      model = faviconUrl,
      contentDescription = domain,
      onError = { failed = true },
      modifier = modifier
        .size(16.dp)
        .clip(RoundedCornerShape(4.dp)),
    )
  } else {
    Icon(
      imageVector = Icons.Rounded.Public,
      contentDescription = domain,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = modifier.size(15.dp),
    )
  }
}
