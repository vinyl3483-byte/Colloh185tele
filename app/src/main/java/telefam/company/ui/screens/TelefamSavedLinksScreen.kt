package telefam.company.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.telefam.company.app.R
import telefam.company.data.local.SavedLinkEntity
import telefam.company.ui.components.FaviconBadge
import telefam.company.ui.theme.TelefamCrimson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelefamSavedLinksSettingsScreen(
  links: List<SavedLinkEntity>,
  searchQuery: String,
  onSearchQueryChanged: (String) -> Unit,
  onOpenLink: (SavedLinkEntity) -> Unit,
  onDeleteLink: (String) -> Unit,
  onBack: () -> Unit,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = stringResource(R.string.saved_links_title),
            fontWeight = FontWeight.SemiBold,
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier
              .size(48.dp)
              .testTag("saved_links_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.cd_back),
            )
          }
        },
      )
    },
  ) { innerPadding ->
    Box(
      contentAlignment = Alignment.TopCenter,
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      SavedLinksListPane(
        links = links,
        searchQuery = searchQuery,
        onSearchQueryChanged = onSearchQueryChanged,
        onOpenLink = onOpenLink,
        onDeleteLink = onDeleteLink,
        modifier = Modifier
          .widthIn(max = 720.dp)
          .fillMaxSize(),
      )
    }
  }
}

@Composable
fun SavedLinksListPane(
  links: List<SavedLinkEntity>,
  searchQuery: String,
  onSearchQueryChanged: (String) -> Unit,
  onOpenLink: (SavedLinkEntity) -> Unit,
  onDeleteLink: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .testTag("saved_links_pane"),
  ) {
    OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchQueryChanged,
      placeholder = { Text(stringResource(R.string.saved_links_search_hint)) },
      leadingIcon = {
        Icon(
          imageVector = Icons.Rounded.Search,
          contentDescription = stringResource(R.string.saved_links_search_hint),
        )
      },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { onSearchQueryChanged("") }) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = stringResource(R.string.cd_clear_search),
            )
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .testTag("saved_links_search_input"),
    )

    if (links.isEmpty()) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp)
          .testTag("saved_links_empty_state"),
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Surface(
            shape = CircleShape,
            color = TelefamCrimson.copy(alpha = 0.12f),
            modifier = Modifier.size(56.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Rounded.Bookmark,
                contentDescription = stringResource(R.string.saved_links_empty),
                tint = TelefamCrimson,
                modifier = Modifier.size(26.dp),
              )
            }
          }
          Spacer(Modifier.height(12.dp))
          Text(
            text = stringResource(R.string.saved_links_empty),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    } else {
      LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
      ) {
        items(links, key = { it.id }) { item ->
          SavedLinkRowCard(
            item = item,
            dateText = remember(item.savedAtMs) { dateFormatter.format(Date(item.savedAtMs)) },
            onOpen = { onOpenLink(item) },
            onDelete = { onDeleteLink(item.id) },
          )
        }
      }
    }
  }
}

@Composable
fun SavedLinkRowCard(
  item: SavedLinkEntity,
  dateText: String,
  onOpen: () -> Unit,
  onDelete: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onOpen)
      .testTag("saved_link_card_${item.id}"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
    ) {
      if (item.imageUrl.isNotBlank()) {
        AsyncImage(
          model = item.imageUrl,
          contentDescription = item.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(58.dp)
            .clip(RoundedCornerShape(10.dp)),
        )
      } else {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = TelefamCrimson.copy(alpha = 0.1f),
          modifier = Modifier.size(58.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.Public,
              contentDescription = item.domain,
              tint = TelefamCrimson,
              modifier = Modifier.size(24.dp),
            )
          }
        }
      }

      Spacer(Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = item.title.ifBlank { item.domain },
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          FaviconBadge(faviconUrl = item.faviconUrl, domain = item.domain)
          Text(
            text = item.domain,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Spacer(Modifier.height(4.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = dateText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = TelefamCrimson.copy(alpha = 0.10f),
          ) {
            Text(
              text = item.source.replaceFirstChar { it.uppercase() },
              style = MaterialTheme.typography.labelSmall,
              color = TelefamCrimson,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
        }
      }

      IconButton(
        onClick = onDelete,
        modifier = Modifier
          .size(48.dp)
          .testTag("saved_link_delete_${item.id}"),
      ) {
        Icon(
          imageVector = Icons.Rounded.DeleteOutline,
          contentDescription = stringResource(R.string.chat_action_delete),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
