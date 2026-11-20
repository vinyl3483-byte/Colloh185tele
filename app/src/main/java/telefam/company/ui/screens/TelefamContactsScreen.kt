package telefam.company.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.telefam.company.app.R
import kotlinx.coroutines.flow.distinctUntilChanged
import telefam.company.core.ContactFilterTab
import telefam.company.core.ContactRelationshipState
import telefam.company.ui.components.CompactThemeToggleChip
import telefam.company.ui.theme.LocalTelefamDarkTheme
import telefam.company.ui.theme.TelefamBlushWave
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamCrimsonBright
import telefam.company.ui.theme.TelefamCrimsonDarkMode
import telefam.company.ui.theme.TelefamDarkBlush
import telefam.company.ui.theme.TelefamPureWhite
import telefam.company.ui.theme.ThemeMode
import telefam.company.ui.viewmodel.ContactUiItem
import telefam.company.ui.viewmodel.ContactsUiState
import java.io.File
import kotlin.math.abs

@Composable
fun ContactsTopHeader(
  searchQuery: String,
  activeFilter: ContactFilterTab,
  themeMode: ThemeMode,
  onSearchChanged: (String) -> Unit,
  onFilterSelected: (ContactFilterTab) -> Unit,
  onToggleTheme: () -> Unit,
) {
  val isDark = LocalTelefamDarkTheme.current
  val focusManager = LocalFocusManager.current
  val searchContainerColor = if (isDark) {
    Color(0xFF111824)
  } else {
    TelefamPureWhite
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(TelefamCrimson)
      .statusBarsPadding()
      .padding(top = 10.dp, bottom = 12.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        modifier = Modifier
          .weight(1f)
          .height(44.dp)
          .testTag("contacts_search_bar"),
        color = searchContainerColor,
        shape = RoundedCornerShape(22.dp),
        border = if (isDark) BorderStroke(1.dp, Color(0xFF1E293B)) else null,
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
            modifier = Modifier.size(19.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
          ) {
            if (searchQuery.isEmpty()) {
              Text(
                text = stringResource(R.string.contacts_search_placeholder),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            BasicTextField(
              value = searchQuery,
              onValueChange = onSearchChanged,
              singleLine = true,
              textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
              ),
              cursorBrush = SolidColor(TelefamCrimson),
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
              keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("contacts_search_input"),
            )
          }
          if (searchQuery.isNotEmpty()) {
            IconButton(
              onClick = { onSearchChanged("") },
              modifier = Modifier
                .size(32.dp)
                .testTag("contacts_search_clear"),
            ) {
              Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.cd_clear_search),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp),
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      CompactThemeToggleChip(
        themeMode = themeMode,
        onToggle = onToggleTheme,
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ContactFilterTab.entries.forEach { tab ->
        val selected = tab == activeFilter
        val labelRes = when (tab) {
          ContactFilterTab.ALL -> R.string.contacts_tab_all
          ContactFilterTab.SUGGESTIONS -> R.string.contacts_tab_suggestions
          ContactFilterTab.FOLLOWERS -> R.string.contacts_tab_followers
          ContactFilterTab.FOLLOWING -> R.string.contacts_tab_following
          ContactFilterTab.FRIENDS -> R.string.contacts_tab_friends
        }
        val pillBg by animateColorAsState(
          targetValue = if (selected) TelefamPureWhite else Color.Transparent,
          label = "filter_pill_bg",
        )
        val textColor by animateColorAsState(
          targetValue = if (selected) TelefamCrimson else TelefamPureWhite,
          label = "filter_pill_fg",
        )

        Surface(
          color = pillBg,
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onFilterSelected(tab) }
            .testTag("contacts_filter_${tab.apiParam}"),
        ) {
          Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelLarge.copy(
              fontSize = 14.sp,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            ),
            color = textColor,
            maxLines = 1,
            modifier = Modifier.padding(
              horizontal = if (selected) 16.dp else 10.dp,
              vertical = 7.dp,
            ),
          )
        }
      }
    }
  }
}

@Composable
fun TelefamContactsContent(
  contactsState: ContactsUiState,
  onToggleFollow: (String) -> Unit,
  onLoadNextPage: () -> Unit,
  onScrollVisibilityChanged: (Boolean) -> Unit,
  onOpenUserProfile: (String) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
  ) {
    val isMultiColumn = maxWidth >= 600.dp

    if (contactsState.isLoading && contactsState.items.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(
          color = TelefamCrimson,
          strokeWidth = 2.5.dp,
        )
      }
    } else if (contactsState.items.isEmpty()) {
      ContactsEmptyView(activeFilter = contactsState.activeFilter)
    } else if (isMultiColumn) {
      ContactsMultiColumnGrid(
        contactsState = contactsState,
        columnsCount = if (maxWidth >= 980.dp) 3 else 2,
        onToggleFollow = onToggleFollow,
        onLoadNextPage = onLoadNextPage,
        onScrollVisibilityChanged = onScrollVisibilityChanged,
        onOpenUserProfile = onOpenUserProfile,
      )
    } else {
      ContactsSingleColumnList(
        contactsState = contactsState,
        onToggleFollow = onToggleFollow,
        onLoadNextPage = onLoadNextPage,
        onScrollVisibilityChanged = onScrollVisibilityChanged,
        onOpenUserProfile = onOpenUserProfile,
      )
    }
  }
}

@Composable
private fun ContactsSingleColumnList(
  contactsState: ContactsUiState,
  onToggleFollow: (String) -> Unit,
  onLoadNextPage: () -> Unit,
  onScrollVisibilityChanged: (Boolean) -> Unit,
  onOpenUserProfile: (String) -> Unit,
) {
  val listState = rememberLazyListState()
  var lastScrollMarker by remember { mutableIntStateOf(0) }

  LaunchedEffect(listState) {
    snapshotFlow {
      val idx = listState.firstVisibleItemIndex
      val off = listState.firstVisibleItemScrollOffset
      Triple(listState.isScrollInProgress, idx, off)
    }
      .distinctUntilChanged()
      .collect { (scrolling, idx, off) ->
        val marker = idx * 10_000 + off
        val delta = marker - lastScrollMarker
        lastScrollMarker = marker
        if (!scrolling) {
          onScrollVisibilityChanged(true)
        } else if (delta > 12) {
          onScrollVisibilityChanged(false)
        } else if (delta < -12) {
          onScrollVisibilityChanged(true)
        }
      }
  }

  LaunchedEffect(listState, contactsState.items.size, contactsState.hasMore, contactsState.isLoadingMore) {
    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
      .distinctUntilChanged()
      .collect { lastVisible ->
        if (contactsState.hasMore &&
          !contactsState.isLoadingMore &&
          contactsState.items.isNotEmpty() &&
          lastVisible >= contactsState.items.size - 4
        ) {
          onLoadNextPage()
        }
      }
  }

  LazyColumn(
    state = listState,
    modifier = Modifier
      .fillMaxSize()
      .testTag("contacts_list"),
    contentPadding = PaddingValues(top = 6.dp, bottom = 88.dp),
  ) {
    item(key = "section_subtitle") {
      ContactsSectionSubtitle(filter = contactsState.activeFilter)
    }

    items(
      items = contactsState.items,
      key = { it.userId },
    ) { contact ->
      ContactRowItem(
        contact = contact,
        onToggleFollow = { onToggleFollow(contact.userId) },
        onOpenProfile = { onOpenUserProfile(contact.userId) },
        cardStyle = false,
      )
    }

    if (contactsState.isLoadingMore) {
      item(key = "pagination_loader") {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(
            color = TelefamCrimson,
            strokeWidth = 2.dp,
            modifier = Modifier.size(22.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun ContactsMultiColumnGrid(
  contactsState: ContactsUiState,
  columnsCount: Int,
  onToggleFollow: (String) -> Unit,
  onLoadNextPage: () -> Unit,
  onScrollVisibilityChanged: (Boolean) -> Unit,
  onOpenUserProfile: (String) -> Unit,
) {
  val gridState = rememberLazyGridState()
  var lastScrollMarker by remember { mutableIntStateOf(0) }

  LaunchedEffect(gridState) {
    snapshotFlow {
      val idx = gridState.firstVisibleItemIndex
      val off = gridState.firstVisibleItemScrollOffset
      Triple(gridState.isScrollInProgress, idx, off)
    }
      .distinctUntilChanged()
      .collect { (scrolling, idx, off) ->
        val marker = idx * 10_000 + off
        val delta = marker - lastScrollMarker
        lastScrollMarker = marker
        if (!scrolling) {
          onScrollVisibilityChanged(true)
        } else if (delta > 12) {
          onScrollVisibilityChanged(false)
        } else if (delta < -12) {
          onScrollVisibilityChanged(true)
        }
      }
  }

  LaunchedEffect(gridState, contactsState.items.size, contactsState.hasMore, contactsState.isLoadingMore) {
    snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
      .distinctUntilChanged()
      .collect { lastVisible ->
        if (contactsState.hasMore &&
          !contactsState.isLoadingMore &&
          contactsState.items.isNotEmpty() &&
          lastVisible >= contactsState.items.size - 4
        ) {
          onLoadNextPage()
        }
      }
  }

  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyVerticalGrid(
      state = gridState,
      columns = GridCells.Fixed(columnsCount),
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 1180.dp)
        .testTag("contacts_grid"),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      item(
        key = "section_subtitle_grid",
        span = { GridItemSpan(maxLineSpan) },
      ) {
        ContactsSectionSubtitle(filter = contactsState.activeFilter)
      }

      items(
        items = contactsState.items,
        key = { it.userId },
      ) { contact ->
        ContactRowItem(
          contact = contact,
          onToggleFollow = { onToggleFollow(contact.userId) },
          onOpenProfile = { onOpenUserProfile(contact.userId) },
          cardStyle = true,
        )
      }

      if (contactsState.isLoadingMore) {
        item(
          key = "pagination_loader_grid",
          span = { GridItemSpan(maxLineSpan) },
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(
              color = TelefamCrimson,
              strokeWidth = 2.dp,
              modifier = Modifier.size(22.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ContactsSectionSubtitle(filter: ContactFilterTab) {
  Text(
    text = filter.subtitleUpper,
    style = MaterialTheme.typography.labelSmall.copy(
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      letterSpacing = 0.8.sp,
    ),
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .testTag("contacts_section_subtitle"),
  )
}

@Composable
private fun ContactRowItem(
  contact: ContactUiItem,
  onToggleFollow: () -> Unit,
  onOpenProfile: () -> Unit,
  cardStyle: Boolean,
) {
  val content = @Composable {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onOpenProfile)
        .padding(
          horizontal = if (cardStyle) 14.dp else 16.dp,
          vertical = if (cardStyle) 12.dp else 10.dp,
        )
        .testTag("contact_row_${contact.userId}"),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ContactAvatarCircle(
        avatarLocalPath = contact.avatarLocalPath,
        initials = contact.initials,
        userId = contact.userId,
      )

      Spacer(modifier = Modifier.width(12.dp))

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.Center,
      ) {
        Text(
          text = contact.fullName,
          style = MaterialTheme.typography.titleMedium.copy(
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Bold,
          ),
          color = MaterialTheme.colorScheme.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        val subLine = if (contact.countryDisplay.isNotBlank()) {
          "@${contact.username} · ${contact.countryDisplay}"
        } else {
          "@${contact.username}"
        }
        Text(
          text = subLine,
          style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      ContactRelationshipButton(
        state = contact.relationshipState,
        userId = contact.userId,
        onClick = onToggleFollow,
      )
    }
  }

  if (cardStyle) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
      content()
    }
  } else {
    content()
  }
}

@Composable
private fun ContactAvatarCircle(
  avatarLocalPath: String?,
  initials: String,
  userId: String,
) {
  val isDark = LocalTelefamDarkTheme.current
  val bitmap = remember(avatarLocalPath) {
    if (!avatarLocalPath.isNullOrBlank() && File(avatarLocalPath).exists()) {
      BitmapFactory.decodeFile(avatarLocalPath)?.asImageBitmap()
    } else {
      null
    }
  }
  val paletteIndex = remember(userId) { abs(userId.hashCode()) % 4 }
  val (bgColor, fgColor) = when (paletteIndex) {
    0 -> if (isDark) Color(0xFF471115) to Color(0xFFF87171) else Color(0xFFFDE8E8) to TelefamCrimson
    1 -> if (isDark) Color(0xFF2E1E4A) to Color(0xFFC4B5FD) else Color(0xFFEDE9FE) to Color(0xFF5B21B6)
    2 -> if (isDark) Color(0xFF153243) to Color(0xFF7DD3FC) else Color(0xFFE0F2FE) to Color(0xFF0369A1)
    else -> if (isDark) Color(0xFF1A3A2A) to Color(0xFF6EE7B7) else Color(0xFFD1FAE5) to Color(0xFF047857)
  }

  Box(
    modifier = Modifier
      .size(48.dp)
      .clip(CircleShape)
      .background(bgColor),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = initials,
      style = MaterialTheme.typography.titleSmall.copy(
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
      ),
      color = fgColor,
    )
    if (bitmap != null) {
      Image(
        bitmap = bitmap,
        contentDescription = stringResource(R.string.cd_contact_avatar),
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    } else if (!avatarLocalPath.isNullOrBlank()) {
      AsyncImage(
        model = avatarLocalPath,
        contentDescription = stringResource(R.string.cd_contact_avatar),
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}

@Composable
private fun ContactRelationshipButton(
  state: ContactRelationshipState,
  userId: String,
  onClick: () -> Unit,
) {
  val isDark = LocalTelefamDarkTheme.current

  val labelRes = when (state) {
    ContactRelationshipState.FOLLOW -> R.string.contacts_action_follow
    ContactRelationshipState.FOLLOWING -> R.string.contacts_action_following
    ContactRelationshipState.FOLLOW_BACK -> R.string.contacts_action_follow_back
    ContactRelationshipState.FRIENDS -> R.string.contacts_action_friends
  }
  val icon = when (state) {
    ContactRelationshipState.FOLLOW, ContactRelationshipState.FOLLOW_BACK -> Icons.Outlined.PersonAdd
    ContactRelationshipState.FOLLOWING -> Icons.Filled.Check
    ContactRelationshipState.FRIENDS -> Icons.Outlined.Group
  }
  val containerColor = when (state) {
    ContactRelationshipState.FOLLOW, ContactRelationshipState.FOLLOW_BACK ->
      if (isDark) TelefamCrimsonBright else TelefamCrimson
    ContactRelationshipState.FOLLOWING -> MaterialTheme.colorScheme.surfaceVariant
    ContactRelationshipState.FRIENDS -> if (isDark) TelefamDarkBlush else TelefamBlushWave
  }
  val contentColor = when (state) {
    ContactRelationshipState.FOLLOW, ContactRelationshipState.FOLLOW_BACK -> TelefamPureWhite
    ContactRelationshipState.FOLLOWING -> MaterialTheme.colorScheme.onSurface
    ContactRelationshipState.FRIENDS -> if (isDark) Color(0xFFFF8A8E) else TelefamCrimson
  }
  val border = when (state) {
    ContactRelationshipState.FOLLOW, ContactRelationshipState.FOLLOW_BACK -> null
    ContactRelationshipState.FOLLOWING -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ContactRelationshipState.FRIENDS -> BorderStroke(
      1.dp,
      if (isDark) TelefamCrimsonDarkMode.copy(alpha = 0.65f) else TelefamCrimson.copy(alpha = 0.45f),
    )
  }

  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(20.dp),
    color = containerColor,
    contentColor = contentColor,
    border = border,
    modifier = Modifier
      .height(38.dp)
      .testTag("contact_action_$userId"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 15.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = contentColor,
        modifier = Modifier.size(16.dp),
      )
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
private fun ContactsEmptyView(activeFilter: ContactFilterTab) {
  val isDark = LocalTelefamDarkTheme.current
  val blushBg = if (isDark) TelefamDarkBlush else TelefamBlushWave

  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .padding(horizontal = 32.dp)
        .widthIn(max = 320.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        modifier = Modifier
          .size(62.dp)
          .clip(CircleShape)
          .background(blushBg),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Outlined.Group,
          contentDescription = null,
          tint = TelefamCrimson,
          modifier = Modifier.size(28.dp),
        )
      }
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = stringResource(R.string.contacts_empty_title),
        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = stringResource(R.string.contacts_empty_subtitle),
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}
