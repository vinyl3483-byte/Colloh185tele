package telefam.company.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.telefam.company.app.R
import telefam.company.core.TelefamContactsRules
import telefam.company.data.local.CachedNotificationEntity
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamPureWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class NotificationFilter(val kind: String?) {
  ALL(null),
  MENTIONS("mention"),
  LIKES("like"),
  FOLLOWS("follow"),
}

@Composable
fun TelefamNotificationsSheet(
  notifications: List<CachedNotificationEntity>,
  onClose: () -> Unit,
  onMarkAllRead: () -> Unit,
  onNotificationClick: (CachedNotificationEntity) -> Unit,
) {
  BackHandler(enabled = true) { onClose() }

  var activeFilter by remember { mutableStateOf(NotificationFilter.ALL) }
  val filteredItems = remember(notifications, activeFilter) {
    val targetKind = activeFilter.kind
    if (targetKind == null) notifications else notifications.filter { it.kind.equals(targetKind, ignoreCase = true) }
  }
  val hasUnread = remember(notifications) { notifications.any { !it.isRead } }

  Surface(
    modifier = Modifier
      .fillMaxSize()
      .testTag("notifications_inbox_screen"),
    color = MaterialTheme.colorScheme.background,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .navigationBarsPadding(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Top Crimson Header
      Surface(
        color = TelefamCrimson,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(
            onClick = onClose,
            modifier = Modifier.testTag("notifications_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.cd_back),
              tint = TelefamPureWhite,
            )
          }
          Text(
            text = stringResource(R.string.notif_inbox_title),
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
            ),
            color = TelefamPureWhite,
            modifier = Modifier.weight(1f),
          )
          if (hasUnread) {
            TextButton(
              onClick = onMarkAllRead,
              modifier = Modifier.testTag("notifications_mark_all_read"),
            ) {
              Text(
                text = stringResource(R.string.notif_inbox_mark_all_read),
                color = TelefamPureWhite,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
              )
            }
          }
        }
      }

      Column(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 640.dp),
      ) {
        // Filter Chips
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          val filters = listOf(
            NotificationFilter.ALL to stringResource(R.string.notif_filter_all),
            NotificationFilter.MENTIONS to stringResource(R.string.notif_filter_mentions),
            NotificationFilter.LIKES to stringResource(R.string.notif_filter_likes),
            NotificationFilter.FOLLOWS to stringResource(R.string.notif_filter_follows),
          )
          filters.forEach { (filter, label) ->
            val selected = activeFilter == filter
            FilterChip(
              selected = selected,
              onClick = { activeFilter = filter },
              label = {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelLarge,
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = TelefamCrimson.copy(alpha = 0.14f),
                selectedLabelColor = TelefamCrimson,
              ),
              modifier = Modifier.testTag("notif_filter_${filter.name.lowercase()}"),
            )
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        if (filteredItems.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(32.dp),
            contentAlignment = Alignment.Center,
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Box(
                modifier = Modifier
                  .size(64.dp)
                  .clip(CircleShape)
                  .background(TelefamCrimson.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Filled.NotificationsNone,
                  contentDescription = null,
                  tint = TelefamCrimson,
                  modifier = Modifier.size(30.dp),
                )
              }
              Spacer(modifier = Modifier.height(14.dp))
              Text(
                text = stringResource(R.string.notif_inbox_empty_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = stringResource(R.string.notif_inbox_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
          ) {
            items(filteredItems, key = { it.id }) { item ->
              NotificationRow(
                item = item,
                onClick = { onNotificationClick(item) },
              )
              HorizontalDivider(
                modifier = Modifier.padding(start = 76.dp, end = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun NotificationRow(
  item: CachedNotificationEntity,
  onClick: () -> Unit,
) {
  val bg = if (!item.isRead) {
    TelefamCrimson.copy(alpha = 0.05f)
  } else {
    Color.Transparent
  }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(bg)
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .testTag("notif_row_${item.id}"),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(modifier = Modifier.size(48.dp)) {
      val initials = remember(item.actorName, item.actorUsername) {
        TelefamContactsRules.initialsFor(item.actorName, item.actorUsername)
      }
      if (item.actorAvatarUrl.isNotBlank()) {
        AsyncImage(
          model = item.actorAvatarUrl,
          contentDescription = item.actorName.ifBlank { item.actorUsername },
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        )
      } else {
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(TelefamCrimson.copy(alpha = 0.14f)),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = initials,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = TelefamCrimson,
          )
        }
      }

      val (badgeIcon, badgeColor) = when (item.kind.lowercase()) {
        "follow" -> Icons.Filled.PersonAdd to Color(0xFF1E88E5)
        "like" -> Icons.Filled.Favorite to TelefamCrimson
        "mention" -> Icons.Filled.AlternateEmail to Color(0xFF7B1FA2)
        else -> Icons.Filled.ChatBubble to TelefamCrimson
      }
      Box(
        modifier = Modifier
          .size(20.dp)
          .align(Alignment.BottomEnd)
          .clip(CircleShape)
          .background(badgeColor)
          .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = badgeIcon,
          contentDescription = null,
          tint = TelefamPureWhite,
          modifier = Modifier.size(11.dp),
        )
      }
    }

    Spacer(modifier = Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      val displayTitle = item.title.ifBlank {
        item.actorName.ifBlank { item.actorUsername.ifBlank { stringResource(R.string.app_name) } }
      }
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = displayTitle,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.SemiBold,
          ),
          color = MaterialTheme.colorScheme.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false),
        )
        Text(
          text = formatNotificationTime(item.createdAtMs),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(modifier = Modifier.height(2.dp))
      val displayBody = if (item.isViewOnce) {
        stringResource(R.string.notif_view_once_voice_body)
      } else {
        item.body
      }
      Text(
        text = displayBody,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }

    if (!item.isRead) {
      Spacer(modifier = Modifier.width(8.dp))
      Box(
        modifier = Modifier
          .size(8.dp)
          .clip(CircleShape)
          .background(TelefamCrimson),
      )
    }
  }
}

private fun formatNotificationTime(timestampMs: Long): String {
  if (timestampMs <= 0L) return ""
  val diff = (System.currentTimeMillis() - timestampMs).coerceAtLeast(0L)
  return when {
    diff < 60_000L -> "now"
    diff < 3_600_000L -> "${diff / 60_000L}m"
    diff < 86_400_000L -> "${diff / 3_600_000L}h"
    diff < 604_800_000L -> "${diff / 86_400_000L}d"
    else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestampMs))
  }
}
