package telefam.company.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import telefam.company.core.AppLanguageOption
import telefam.company.core.AudienceOption
import telefam.company.core.ContentPreferencesState
import telefam.company.core.DiscoverabilityOption
import telefam.company.core.NotificationSettingsState
import telefam.company.core.PrivacySettingsState
import telefam.company.core.SettingsDestination
import telefam.company.core.SyncPreferencesState
import telefam.company.core.TelefamQrEncoder
import telefam.company.core.TelefamSettingsContentCatalog
import telefam.company.core.VideoQualityOption
import telefam.company.core.VisibilityOption
import telefam.company.data.local.SavedLinkEntity
import telefam.company.ui.components.CompactThemeToggleChip
import telefam.company.ui.theme.LocalTelefamDarkTheme
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamPureWhite
import telefam.company.ui.viewmodel.TelefamUiState
import java.io.File

private data class SettingsHubRowModel(
  val id: String,
  val section: String,
  val title: String,
  val subtitle: String,
  val iconKind: SettingsSvgIconKind,
  val destination: SettingsDestination,
  val keywords: String = "",
)

@Composable
fun TelefamSettingsScreen(
  uiState: TelefamUiState,
  onNavigateBack: () -> Unit,
  onOpenDestination: (SettingsDestination) -> Unit,
  onSearchQueryChanged: (String) -> Unit,
  onToggleTheme: () -> Unit,
  onUpdatePrivacy: (PrivacySettingsState) -> Unit,
  onUpdateNotifications: (NotificationSettingsState) -> Unit,
  onUpdateContentPreferences: (ContentPreferencesState, Boolean) -> Unit,
  onUpdateSyncPreferences: (SyncPreferencesState) -> Unit = {},
  onTriggerManualSync: () -> Unit = {},
  onRefreshSyncStatus: () -> Unit = {},
  onRestrictedTabSelected: (Int) -> Unit,
  onRestrictedQueryChanged: (String) -> Unit,
  onUnblockAccount: (String) -> Unit,
  onUnmuteAccount: (String) -> Unit,
  onSubmitProblemReport: (category: String, description: String, screenshotUri: Uri?) -> Unit,
  onClearLastCreatedTicket: () -> Unit,
  onSearchFaq: (String) -> Unit,
  onRefreshTickets: () -> Unit,
  onRefreshStatus: () -> Unit,
  onSelectLegalDoc: (String) -> Unit,
  onCheckAppUpdates: () -> Unit,
  onSavedLinksSearchQueryChanged: (String) -> Unit = {},
  onOpenSavedLink: (SavedLinkEntity) -> Unit = {},
  onDeleteSavedLink: (String) -> Unit = {},
  onShowToast: (String) -> Unit,
  onRequestLogout: () -> Unit,
  onDismissLogoutDialog: () -> Unit,
  onToggleLogoutAllDevices: (Boolean) -> Unit,
  onConfirmLogout: () -> Unit,
) {
  BackHandler(enabled = true) { onNavigateBack() }

  val isDark = LocalTelefamDarkTheme.current
  val pageBg = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF3F5F8)
  val destination = uiState.activeSettingsDestination ?: SettingsDestination.HUB

  val fullName = uiState.ownProfileState?.fullName?.takeIf { it.isNotBlank() }
    ?: uiState.profileForm.fullName.ifBlank { "Telefam" }
  val username = uiState.ownProfileState?.username?.takeIf { it.isNotBlank() }
    ?: uiState.profileForm.username.ifBlank { "user" }
  val avatarSource = uiState.ownProfileState?.avatarUrl?.takeIf { it.isNotBlank() }
    ?: uiState.savedAvatarLocalPath

  val destinationTitle = when (destination) {
    SettingsDestination.HUB -> stringResource(R.string.settings_privacy_title)
    SettingsDestination.PRIVACY -> "Privacy"
    SettingsDestination.SHARE_PROFILE -> "Share profile"
    SettingsDestination.NOTIFICATIONS -> "Notifications"
    SettingsDestination.CONTENT_PREFERENCES -> "Content preferences"
    SettingsDestination.OFFLINE_SYNC -> "Offline sync"
    SettingsDestination.SAVED_LINKS -> stringResource(R.string.saved_links_title)
    SettingsDestination.BLOCKED_ACCOUNTS -> "Blocked & muted"
    SettingsDestination.REPORT_PROBLEM -> "Report a problem"
    SettingsDestination.SAFETY_CENTER -> "Safety Center"
    SettingsDestination.HELP_SUPPORT -> "Help & support"
    SettingsDestination.TERMS_POLICIES -> "Terms and policies"
    SettingsDestination.ABOUT -> "About"
  }

  Surface(
    modifier = Modifier
      .fillMaxSize()
      .testTag("settings_root_screen"),
    color = pageBg,
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val isWide = maxWidth >= 840.dp

      Column(
        modifier = Modifier
          .fillMaxSize()
          .navigationBarsPadding(),
      ) {
        // Curved crimson header matching reference PNG
        Surface(
          color = TelefamCrimson,
          shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
          shadowElevation = 4.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .statusBarsPadding()
              .padding(horizontal = 8.dp, vertical = 14.dp),
          ) {
            IconButton(
              onClick = onNavigateBack,
              modifier = Modifier
                .align(Alignment.CenterStart)
                .size(48.dp)
                .testTag("settings_back_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                tint = TelefamPureWhite,
              )
            }

            Text(
              text = if (isWide) stringResource(R.string.settings_privacy_title) else destinationTitle,
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = TelefamPureWhite,
              ),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.align(Alignment.Center),
            )
          }
        }

        if (isWide) {
          val activeRight = if (destination == SettingsDestination.HUB) SettingsDestination.PRIVACY else destination
          Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Box(
              modifier = Modifier
                .width(380.dp)
                .fillMaxHeight(),
            ) {
              SettingsHubBody(
                fullName = fullName,
                username = username,
                avatarSource = avatarSource,
                themeMode = uiState.themeMode,
                searchQuery = uiState.settingsSearchQuery,
                selectedDestination = activeRight,
                pendingSyncCount = uiState.syncStatus.totalPendingCount,
                onSearchQueryChanged = onSearchQueryChanged,
                onToggleTheme = onToggleTheme,
                onSelectDestination = onOpenDestination,
                onRequestLogout = onRequestLogout,
              )
            }
            HorizontalDivider(
              modifier = Modifier.width(1.dp).fillMaxHeight(),
              color = MaterialTheme.colorScheme.outlineVariant,
            )
            Box(
              modifier = Modifier.weight(1f).fillMaxHeight(),
              contentAlignment = Alignment.TopCenter,
            ) {
              Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxSize()) {
                Text(
                  text = when (activeRight) {
                    SettingsDestination.PRIVACY -> "Privacy"
                    SettingsDestination.SHARE_PROFILE -> "Share profile"
                    SettingsDestination.NOTIFICATIONS -> "Notifications"
                    SettingsDestination.CONTENT_PREFERENCES -> "Content preferences"
                    SettingsDestination.OFFLINE_SYNC -> "Offline sync"
                    SettingsDestination.SAVED_LINKS -> stringResource(R.string.saved_links_title)
                    SettingsDestination.BLOCKED_ACCOUNTS -> "Blocked & muted accounts"
                    SettingsDestination.REPORT_PROBLEM -> "Report a problem"
                    SettingsDestination.SAFETY_CENTER -> "Safety Center"
                    SettingsDestination.HELP_SUPPORT -> "Help & support"
                    SettingsDestination.TERMS_POLICIES -> "Terms and policies"
                    SettingsDestination.ABOUT -> "About"
                    SettingsDestination.HUB -> "Privacy"
                  },
                  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                  modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                )
                SettingsDetailHost(
                  destination = activeRight,
                  uiState = uiState,
                  fullName = fullName,
                  username = username,
                  avatarSource = avatarSource,
                  onUpdatePrivacy = onUpdatePrivacy,
                  onUpdateNotifications = onUpdateNotifications,
                  onUpdateContentPreferences = onUpdateContentPreferences,
                  onUpdateSyncPreferences = onUpdateSyncPreferences,
                  onTriggerManualSync = onTriggerManualSync,
                  onRefreshSyncStatus = onRefreshSyncStatus,
                  onRestrictedTabSelected = onRestrictedTabSelected,
                  onRestrictedQueryChanged = onRestrictedQueryChanged,
                  onUnblockAccount = onUnblockAccount,
                  onUnmuteAccount = onUnmuteAccount,
                  onSubmitProblemReport = onSubmitProblemReport,
                  onClearLastCreatedTicket = onClearLastCreatedTicket,
                  onSearchFaq = onSearchFaq,
                  onRefreshTickets = onRefreshTickets,
                  onRefreshStatus = onRefreshStatus,
                  onSelectLegalDoc = onSelectLegalDoc,
                  onCheckAppUpdates = onCheckAppUpdates,
                  onSavedLinksSearchQueryChanged = onSavedLinksSearchQueryChanged,
                  onOpenSavedLink = onOpenSavedLink,
                  onDeleteSavedLink = onDeleteSavedLink,
                  onOpenDestination = onOpenDestination,
                  onShowToast = onShowToast,
                )
              }
            }
          }
        } else {
          Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
          ) {
            if (destination == SettingsDestination.HUB) {
              SettingsHubBody(
                fullName = fullName,
                username = username,
                avatarSource = avatarSource,
                themeMode = uiState.themeMode,
                searchQuery = uiState.settingsSearchQuery,
                selectedDestination = null,
                pendingSyncCount = uiState.syncStatus.totalPendingCount,
                onSearchQueryChanged = onSearchQueryChanged,
                onToggleTheme = onToggleTheme,
                onSelectDestination = onOpenDestination,
                onRequestLogout = onRequestLogout,
              )
            } else {
              SettingsDetailHost(
                destination = destination,
                uiState = uiState,
                fullName = fullName,
                username = username,
                avatarSource = avatarSource,
                onUpdatePrivacy = onUpdatePrivacy,
                onUpdateNotifications = onUpdateNotifications,
                onUpdateContentPreferences = onUpdateContentPreferences,
                onUpdateSyncPreferences = onUpdateSyncPreferences,
                onTriggerManualSync = onTriggerManualSync,
                onRefreshSyncStatus = onRefreshSyncStatus,
                onRestrictedTabSelected = onRestrictedTabSelected,
                onRestrictedQueryChanged = onRestrictedQueryChanged,
                onUnblockAccount = onUnblockAccount,
                onUnmuteAccount = onUnmuteAccount,
                onSubmitProblemReport = onSubmitProblemReport,
                onClearLastCreatedTicket = onClearLastCreatedTicket,
                onSearchFaq = onSearchFaq,
                onRefreshTickets = onRefreshTickets,
                onRefreshStatus = onRefreshStatus,
                onSelectLegalDoc = onSelectLegalDoc,
                onCheckAppUpdates = onCheckAppUpdates,
                onSavedLinksSearchQueryChanged = onSavedLinksSearchQueryChanged,
                onOpenSavedLink = onOpenSavedLink,
                onDeleteSavedLink = onDeleteSavedLink,
                onOpenDestination = onOpenDestination,
                onShowToast = onShowToast,
              )
            }
          }
        }
      }

      if (uiState.showLogoutConfirmDialog) {
        LogoutConfirmationDialog(
          logoutAllDevices = uiState.logoutAllDevicesChoice,
          onToggleLogoutAllDevices = onToggleLogoutAllDevices,
          onConfirmLogout = onConfirmLogout,
          onDismiss = onDismissLogoutDialog,
        )
      }
    }
  }
}

@Composable
private fun SettingsHubBody(
  fullName: String,
  username: String,
  avatarSource: String?,
  themeMode: telefam.company.ui.theme.ThemeMode,
  searchQuery: String,
  selectedDestination: SettingsDestination?,
  pendingSyncCount: Int,
  onSearchQueryChanged: (String) -> Unit,
  onToggleTheme: () -> Unit,
  onSelectDestination: (SettingsDestination) -> Unit,
  onRequestLogout: () -> Unit,
) {
  val syncSubtitle = if (pendingSyncCount > 0) {
    "$pendingSyncCount item(s) queued for server sync"
  } else {
    "Auto-sync, Wi-Fi only, up to date"
  }
  val allRows = remember(syncSubtitle) {
    listOf(
      SettingsHubRowModel(
        id = "account_privacy",
        section = "Account",
        title = "Privacy",
        subtitle = "Private account, blocked accounts, activity",
        iconKind = SettingsSvgIconKind.ACTIVITY_PULSE,
        destination = SettingsDestination.PRIVACY,
        keywords = "private account discovery activity comment mention",
      ),
      SettingsHubRowModel(
        id = "share_profile",
        section = "Account",
        title = "Share profile",
        subtitle = "QR code, link, copy profile URL",
        iconKind = SettingsSvgIconKind.SHARE_CARD,
        destination = SettingsDestination.SHARE_PROFILE,
        keywords = "qr code link url invite",
      ),
      SettingsHubRowModel(
        id = "notifications",
        section = "Content & activity",
        title = "Notifications",
        subtitle = "Push, email, SMS",
        iconKind = SettingsSvgIconKind.BELL,
        destination = SettingsDestination.NOTIFICATIONS,
        keywords = "push email sms quiet hours pause alerts messages comments",
      ),
      SettingsHubRowModel(
        id = "content_prefs",
        section = "Content & activity",
        title = "Content preferences",
        subtitle = "Suggested content, language, video quality",
        iconKind = SettingsSvgIconKind.HEART,
        destination = SettingsDestination.CONTENT_PREFERENCES,
        keywords = "autoplay captions reduced motion data saver topics language",
      ),
      SettingsHubRowModel(
        id = "offline_sync",
        section = "Content & activity",
        title = "Offline sync",
        subtitle = syncSubtitle,
        iconKind = SettingsSvgIconKind.CLOUD_SYNC,
        destination = SettingsDestination.OFFLINE_SYNC,
        keywords = "offline sync queue server wifi cache background retry",
      ),
      SettingsHubRowModel(
        id = "saved_links",
        section = "Content & activity",
        title = "Saved links",
        subtitle = "Bookmarked pages & domains",
        iconKind = SettingsSvgIconKind.BOOKMARK_LINK,
        destination = SettingsDestination.SAVED_LINKS,
        keywords = "saved links bookmarks web browser url",
      ),
      SettingsHubRowModel(
        id = "safety_privacy",
        section = "Privacy and safety",
        title = "Privacy",
        subtitle = "Who can see your videos, likes, comments, following",
        iconKind = SettingsSvgIconKind.LOCK,
        destination = SettingsDestination.PRIVACY,
        keywords = "likes following visibility cookies data sharing",
      ),
      SettingsHubRowModel(
        id = "blocked_accounts",
        section = "Privacy and safety",
        title = "Blocked accounts",
        subtitle = "Manage blocked users",
        iconKind = SettingsSvgIconKind.BLOCK_MINUS,
        destination = SettingsDestination.BLOCKED_ACCOUNTS,
        keywords = "unblock muted mute restrict",
      ),
      SettingsHubRowModel(
        id = "report_problem",
        section = "Privacy and safety",
        title = "Report a problem",
        subtitle = "Send feedback, report abuse",
        iconKind = SettingsSvgIconKind.FLAG,
        destination = SettingsDestination.REPORT_PROBLEM,
        keywords = "bug feedback screenshot ticket reference",
      ),
      SettingsHubRowModel(
        id = "safety_center",
        section = "Privacy and safety",
        title = "Safety Center",
        subtitle = "Tips, tools, resources",
        iconKind = SettingsSvgIconKind.SHIELD_CHECK,
        destination = SettingsDestination.SAFETY_CENTER,
        keywords = "parents guardians emergency helpline abuse reporting",
      ),
      SettingsHubRowModel(
        id = "help_support",
        section = "Support & about",
        title = "Help & support",
        subtitle = "FAQs, contact us, report issue",
        iconKind = SettingsSvgIconKind.HELP_QUESTION,
        destination = SettingsDestination.HELP_SUPPORT,
        keywords = "faq tickets system status outage",
      ),
      SettingsHubRowModel(
        id = "terms_policies",
        section = "Support & about",
        title = "Terms and policies",
        subtitle = "Terms of Service, Privacy Policy, Community Guidelines",
        iconKind = SettingsSvgIconKind.DOCUMENT_LINES,
        destination = SettingsDestination.TERMS_POLICIES,
        keywords = "legal cookies licenses privacy policy guidelines",
      ),
      SettingsHubRowModel(
        id = "about",
        section = "Support & about",
        title = "About",
        subtitle = "Version, copyright, open source",
        iconKind = SettingsSvgIconKind.INFO_CIRCLE,
        destination = SettingsDestination.ABOUT,
        keywords = "version build update check open source license company",
      ),
    )
  }

  val q = searchQuery.trim().lowercase()
  val filtered = remember(q, allRows) {
    if (q.isEmpty()) allRows else allRows.filter {
      it.title.lowercase().contains(q) ||
        it.subtitle.lowercase().contains(q) ||
        it.section.lowercase().contains(q) ||
        it.keywords.contains(q)
    }
  }
  val sections = remember(filtered) {
    filtered.groupBy { it.section }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("settings_hub_list"),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Top profile identity card
    item {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 1.dp,
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          SettingsAvatarBadge(avatarUrl = avatarSource, displayName = fullName, size = 48.dp)
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = fullName,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "@$username",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
            )
          }
          CompactThemeToggleChip(themeMode = themeMode, onToggle = onToggleTheme)
        }
      }
    }

    // Search settings pill bar
    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
        singleLine = true,
        leadingIcon = {
          Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { onSearchQueryChanged("") }) {
              Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_clear_search))
            }
          }
        },
        placeholder = { Text(stringResource(R.string.settings_search_hint)) },
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surface,
          unfocusedContainerColor = MaterialTheme.colorScheme.surface,
          focusedBorderColor = TelefamCrimson,
          unfocusedBorderColor = Color.Transparent,
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("settings_search_input"),
      )
    }

    if (sections.isEmpty()) {
      item {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
          Text("No matching settings", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    } else {
      sections.forEach { (sectionTitle, rows) ->
        item(key = "section_$sectionTitle") {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = sectionTitle,
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              ),
              modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
            Surface(
              color = MaterialTheme.colorScheme.surface,
              shape = RoundedCornerShape(20.dp),
              shadowElevation = 1.dp,
            ) {
              Column(modifier = Modifier.fillMaxWidth()) {
                rows.forEachIndexed { idx, row ->
                  val isSelected = selectedDestination == row.destination
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .background(if (isSelected) TelefamCrimson.copy(alpha = 0.06f) else Color.Transparent)
                      .clickable { onSelectDestination(row.destination) }
                      .padding(horizontal = 16.dp, vertical = 14.dp)
                      .testTag("settings_row_${row.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    SettingsRedCircleBadge(kind = row.iconKind, size = 42.dp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = row.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                          fontWeight = FontWeight.SemiBold,
                          fontSize = 15.5.sp,
                        ),
                      )
                      Spacer(modifier = Modifier.height(2.dp))
                      Text(
                        text = row.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    SettingsChevronRight()
                  }
                  if (idx < rows.lastIndex) {
                    HorizontalDivider(
                      modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // Prominent crimson pill Log out button at bottom matching reference PNG
    item {
      Spacer(modifier = Modifier.height(6.dp))
      Button(
        onClick = onRequestLogout,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TelefamCrimson, contentColor = TelefamPureWhite),
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("settings_logout_button"),
      ) {
        SettingsLogoutDoorSvg(color = TelefamPureWhite)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = stringResource(R.string.settings_logout),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
        )
      }
      Spacer(modifier = Modifier.height(14.dp))
    }
  }
}

@Composable
private fun SettingsDetailHost(
  destination: SettingsDestination,
  uiState: TelefamUiState,
  fullName: String,
  username: String,
  avatarSource: String?,
  onUpdatePrivacy: (PrivacySettingsState) -> Unit,
  onUpdateNotifications: (NotificationSettingsState) -> Unit,
  onUpdateContentPreferences: (ContentPreferencesState, Boolean) -> Unit,
  onUpdateSyncPreferences: (SyncPreferencesState) -> Unit,
  onTriggerManualSync: () -> Unit,
  onRefreshSyncStatus: () -> Unit,
  onRestrictedTabSelected: (Int) -> Unit,
  onRestrictedQueryChanged: (String) -> Unit,
  onUnblockAccount: (String) -> Unit,
  onUnmuteAccount: (String) -> Unit,
  onSubmitProblemReport: (category: String, description: String, screenshotUri: Uri?) -> Unit,
  onClearLastCreatedTicket: () -> Unit,
  onSearchFaq: (String) -> Unit,
  onRefreshTickets: () -> Unit,
  onRefreshStatus: () -> Unit,
  onSelectLegalDoc: (String) -> Unit,
  onCheckAppUpdates: () -> Unit,
  onSavedLinksSearchQueryChanged: (String) -> Unit,
  onOpenSavedLink: (SavedLinkEntity) -> Unit,
  onDeleteSavedLink: (String) -> Unit,
  onOpenDestination: (SettingsDestination) -> Unit,
  onShowToast: (String) -> Unit,
) {
  when (destination) {
    SettingsDestination.PRIVACY -> PrivacySettingsPane(
      state = uiState.settings.privacy,
      onChange = onUpdatePrivacy,
    )
    SettingsDestination.SHARE_PROFILE -> ShareProfileQrPane(
      fullName = fullName,
      username = username,
      avatarSource = avatarSource,
      onShowToast = onShowToast,
    )
    SettingsDestination.NOTIFICATIONS -> NotificationsSettingsPane(
      state = uiState.settings.notifications,
      onChange = onUpdateNotifications,
    )
    SettingsDestination.CONTENT_PREFERENCES -> ContentPreferencesPane(
      state = uiState.settings.contentPreferences,
      onChange = onUpdateContentPreferences,
    )
    SettingsDestination.OFFLINE_SYNC -> OfflineSyncPane(
      prefs = uiState.settings.syncPreferences,
      status = uiState.syncStatus,
      networkTier = uiState.networkTier,
      isWifiConnected = uiState.isWifiConnected,
      onUpdatePrefs = onUpdateSyncPreferences,
      onSyncNow = onTriggerManualSync,
      onRefreshStatus = onRefreshSyncStatus,
    )
    SettingsDestination.SAVED_LINKS -> {
      val q = uiState.savedLinksSearchQuery.trim().lowercase()
      val filteredLinks = remember(uiState.savedLinks, q) {
        if (q.isEmpty()) uiState.savedLinks
        else uiState.savedLinks.filter {
          it.title.lowercase().contains(q) ||
            it.domain.lowercase().contains(q) ||
            it.normalizedUrl.lowercase().contains(q)
        }
      }
      SavedLinksListPane(
        links = filteredLinks,
        searchQuery = uiState.savedLinksSearchQuery,
        onSearchQueryChanged = onSavedLinksSearchQueryChanged,
        onOpenLink = onOpenSavedLink,
        onDeleteLink = onDeleteSavedLink,
        modifier = Modifier.fillMaxSize(),
      )
    }
    SettingsDestination.BLOCKED_ACCOUNTS -> BlockedMutedAccountsPane(
      selectedTab = uiState.restrictedAccountsTab,
      query = uiState.restrictedAccountsQuery,
      blocked = uiState.blockedAccounts,
      muted = uiState.mutedAccounts,
      isLoading = uiState.isLoadingRestrictedAccounts,
      onSelectTab = onRestrictedTabSelected,
      onQueryChanged = onRestrictedQueryChanged,
      onUnblock = onUnblockAccount,
      onUnmute = onUnmuteAccount,
    )
    SettingsDestination.REPORT_PROBLEM -> ReportProblemPane(
      isSubmitting = uiState.isSubmittingProblemReport,
      createdTicket = uiState.lastCreatedTicket,
      onSubmitReport = onSubmitProblemReport,
      onClearCreated = onClearLastCreatedTicket,
      onOpenTicketHistory = { onOpenDestination(SettingsDestination.HELP_SUPPORT) },
    )
    SettingsDestination.SAFETY_CENTER -> SafetyCenterPane()
    SettingsDestination.HELP_SUPPORT -> HelpSupportPane(
      faqQuery = uiState.faqSearchQuery,
      faqs = uiState.faqItems,
      tickets = uiState.supportTickets,
      systemStatuses = uiState.systemStatuses,
      isLoadingTickets = uiState.isLoadingSupportTickets,
      isLoadingStatus = uiState.isLoadingSystemStatus,
      onSearchFaq = onSearchFaq,
      onRefreshTickets = onRefreshTickets,
      onRefreshStatus = onRefreshStatus,
    )
    SettingsDestination.TERMS_POLICIES -> TermsPoliciesPane(
      documents = uiState.legalDocuments,
      selectedDocId = uiState.selectedLegalDocId,
      onSelectDoc = onSelectLegalDoc,
      onOpenAboutLicenses = { onOpenDestination(SettingsDestination.ABOUT) },
    )
    SettingsDestination.ABOUT -> AboutSettingsPane(
      versionCheck = uiState.appVersionCheck,
      isCheckingUpdates = uiState.isCheckingAppUpdates,
      onCheckUpdates = onCheckAppUpdates,
    )
    SettingsDestination.HUB -> Unit
  }
}

@Composable
private fun PrivacySettingsPane(
  state: PrivacySettingsState,
  onChange: (PrivacySettingsState) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      SettingsToggleGroupCard(
        title = "Account privacy",
        items = listOf(
          ToggleRowSpec(
            label = "Private account",
            subtitle = "Only approved followers can see your posts, followers, and following",
            checked = state.isPrivateAccount,
            tag = "privacy_private_account_switch",
            onCheckedChange = { onChange(state.copy(isPrivateAccount = it)) },
          ),
          ToggleRowSpec(
            label = "Activity status",
            subtitle = "Show when you were recently active",
            checked = state.showActivityStatus,
            onCheckedChange = { onChange(state.copy(showActivityStatus = it)) },
          ),
        ),
      )
    }

    item {
      Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          Text("Interactions & visibility", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
          ChipSelectorRow(
            label = "Who can comment",
            options = AudienceOption.entries.map { it.label to (state.whoCanComment == it) },
            onSelect = { idx -> onChange(state.copy(whoCanComment = AudienceOption.entries[idx])) },
          )
          ChipSelectorRow(
            label = "Who can mention",
            options = AudienceOption.entries.map { it.label to (state.whoCanMention == it) },
            onSelect = { idx -> onChange(state.copy(whoCanMention = AudienceOption.entries[idx])) },
          )
          ChipSelectorRow(
            label = "Who can find you",
            options = DiscoverabilityOption.entries.map { it.label to (state.whoCanFindYou == it) },
            onSelect = { idx -> onChange(state.copy(whoCanFindYou = DiscoverabilityOption.entries[idx])) },
          )
          ChipSelectorRow(
            label = "Visibility of likes",
            options = VisibilityOption.entries.map { it.label to (state.likesVisibility == it) },
            onSelect = { idx -> onChange(state.copy(likesVisibility = VisibilityOption.entries[idx])) },
          )
          ChipSelectorRow(
            label = "Visibility of following",
            options = VisibilityOption.entries.map { it.label to (state.followingVisibility == it) },
            onSelect = { idx -> onChange(state.copy(followingVisibility = VisibilityOption.entries[idx])) },
          )
        }
      }
    }

    item {
      SettingsToggleGroupCard(
        title = "Data & personalization",
        items = listOf(
          ToggleRowSpec(
            label = "Personalized recommendations",
            subtitle = "Use watch history and likes to rank For You",
            checked = state.dataSharingPersonalization,
            onCheckedChange = { onChange(state.copy(dataSharingPersonalization = it)) },
          ),
          ToggleRowSpec(
            label = "Diagnostics & analytics",
            subtitle = "Share playback reliability telemetry",
            checked = state.analyticsSharing,
            onCheckedChange = { onChange(state.copy(analyticsSharing = it)) },
          ),
          ToggleRowSpec(
            label = "Personalization cookies",
            subtitle = "Remember topic and search affinities",
            checked = state.personalizationCookies,
            onCheckedChange = { onChange(state.copy(personalizationCookies = it)) },
          ),
        ),
      )
    }
  }
}

@Composable
private fun ShareProfileQrPane(
  fullName: String,
  username: String,
  avatarSource: String?,
  onShowToast: (String) -> Unit,
) {
  val context = LocalContext.current
  val profileUrl = remember(username) {
    val base = BuildConfig.TELEFAM_API_BASE_URL.trim().trimEnd('/')
    if (base.startsWith("http://") || base.startsWith("https://")) "$base/u/$username" else "https://telefam.app/u/$username"
  }
  val matrix = remember(profileUrl) { TelefamQrEncoder.encodeToMatrix(profileUrl) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          SettingsAvatarBadge(avatarUrl = avatarSource, displayName = fullName, size = 64.dp)
          Spacer(modifier = Modifier.height(10.dp))
          Text(fullName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
          Text("@$username", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Spacer(modifier = Modifier.height(18.dp))
          Surface(
            color = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
              .size(220.dp)
              .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
              .padding(14.dp)
              .testTag("profile_qr_code"),
          ) {
            Canvas(modifier = Modifier.fillMaxSize().aspectRatio(1f)) {
              val n = matrix.size
              if (n <= 0) return@Canvas
              val cellW = size.width / n
              val cellH = size.height / n
              for (r in 0 until n) {
                for (c in 0 until n) {
                  if (matrix[r][c]) {
                    drawRect(
                      color = Color(0xFF141518),
                      topLeft = Offset(c * cellW, r * cellH),
                      size = Size(cellW + 0.4f, cellH + 0.4f),
                    )
                  }
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = profileUrl,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
        }
      }
    }

    item {
      Row(
        modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedButton(
          onClick = {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Telefam Profile URL", profileUrl))
            onShowToast(context.getString(R.string.feed_link_copied))
          },
          shape = RoundedCornerShape(22.dp),
          modifier = Modifier.weight(1f).height(48.dp),
        ) {
          Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Copy URL")
        }
        Button(
          onClick = {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_TEXT, "$fullName (@$username) on Telefam: $profileUrl")
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share profile"))
          },
          colors = ButtonDefaults.buttonColors(containerColor = TelefamCrimson, contentColor = TelefamPureWhite),
          shape = RoundedCornerShape(22.dp),
          modifier = Modifier.weight(1f).height(48.dp),
        ) {
          Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Share")
        }
      }
    }
  }
}

@Composable
private fun NotificationsSettingsPane(
  state: NotificationSettingsState,
  onChange: (NotificationSettingsState) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      SettingsToggleGroupCard(
        title = "Master control",
        items = listOf(
          ToggleRowSpec(
            label = "Master pause",
            subtitle = "Silence all Telefam notifications temporarily",
            checked = state.masterPause,
            tag = "notifications_master_pause_switch",
            onCheckedChange = { onChange(state.copy(masterPause = it)) },
          ),
        ),
      )
    }

    item {
      SettingsToggleGroupCard(
        title = "Channels",
        enabled = !state.masterPause,
        items = listOf(
          ToggleRowSpec("Push notifications", null, state.channelPush) { onChange(state.copy(channelPush = it)) },
          ToggleRowSpec("Email notifications", null, state.channelEmail) { onChange(state.copy(channelEmail = it)) },
          ToggleRowSpec("SMS alerts", null, state.channelSms) { onChange(state.copy(channelSms = it)) },
        ),
      )
    }

    item {
      SettingsToggleGroupCard(
        title = "Notification types",
        enabled = !state.masterPause,
        items = listOf(
          ToggleRowSpec("Messages", null, state.typeMessages) { onChange(state.copy(typeMessages = it)) },
          ToggleRowSpec("Comments", null, state.typeComments) { onChange(state.copy(typeComments = it)) },
          ToggleRowSpec("Followers", null, state.typeFollowers) { onChange(state.copy(typeFollowers = it)) },
          ToggleRowSpec("Product updates", null, state.typeProductUpdates) { onChange(state.copy(typeProductUpdates = it)) },
        ),
      )
    }

    item {
      Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Quiet hours", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
              Text("Mute alerts on a daily schedule", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
              checked = state.quietHoursEnabled,
              enabled = !state.masterPause,
              onCheckedChange = { onChange(state.copy(quietHoursEnabled = it)) },
              colors = SwitchDefaults.colors(checkedThumbColor = TelefamPureWhite, checkedTrackColor = TelefamCrimson),
            )
          }
          if (state.quietHoursEnabled) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            QuietTimeRow(
              label = "Start",
              formatted = state.formatTime(state.quietStartMinutes),
              onMinus = { onChange(state.copy(quietStartMinutes = (state.quietStartMinutes - 30 + 1440) % 1440)) },
              onPlus = { onChange(state.copy(quietStartMinutes = (state.quietStartMinutes + 30) % 1440)) },
            )
            QuietTimeRow(
              label = "End",
              formatted = state.formatTime(state.quietEndMinutes),
              onMinus = { onChange(state.copy(quietEndMinutes = (state.quietEndMinutes - 30 + 1440) % 1440)) },
              onPlus = { onChange(state.copy(quietEndMinutes = (state.quietEndMinutes + 30) % 1440)) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun QuietTimeRow(
  label: String,
  formatted: String,
  onMinus: () -> Unit,
  onPlus: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedButton(onClick = onMinus, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)) { Text("-30m") }
      Text(formatted, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TelefamCrimson))
      OutlinedButton(onClick = onPlus, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)) { Text("+30m") }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContentPreferencesPane(
  state: ContentPreferencesState,
  onChange: (ContentPreferencesState, Boolean) -> Unit,
) {
  var topicInput by rememberSaveable { mutableStateOf("") }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Suggested content", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
              Text("Recommend new creators in For You", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
              checked = state.suggestedContentEnabled,
              onCheckedChange = { onChange(state.copy(suggestedContentEnabled = it), false) },
              colors = SwitchDefaults.colors(checkedThumbColor = TelefamPureWhite, checkedTrackColor = TelefamCrimson),
            )
          }
          OutlinedButton(
            onClick = { onChange(state.copy(notInterestedTopics = emptyList()), true) },
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("Reset suggestions", color = TelefamCrimson, fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }

    item {
      Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Not interested topics", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = topicInput,
              onValueChange = { topicInput = it.take(30) },
              singleLine = true,
              placeholder = { Text("Add topic to hide") },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.weight(1f),
            )
            IconButton(
              onClick = {
                val clean = topicInput.trim().lowercase().removePrefix("#")
                if (clean.isNotEmpty() && !state.notInterestedTopics.contains(clean)) {
                  onChange(state.copy(notInterestedTopics = state.notInterestedTopics + clean), false)
                }
                topicInput = ""
              },
              modifier = Modifier.size(48.dp).clip(CircleShape).background(TelefamCrimson),
            ) {
              Icon(Icons.Filled.Add, contentDescription = "Add topic", tint = TelefamPureWhite)
            }
          }
          if (state.notInterestedTopics.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              state.notInterestedTopics.forEach { topic ->
                FilterChip(
                  selected = true,
                  onClick = {
                    onChange(state.copy(notInterestedTopics = state.notInterestedTopics.filterNot { it == topic }), false)
                  },
                  label = { Text("#$topic ×") },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TelefamCrimson,
                    selectedLabelColor = TelefamPureWhite,
                  ),
                )
              }
            }
          }
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TelefamSettingsContentCatalog.quickTopicTags
              .filterNot { it in state.notInterestedTopics }
              .forEach { tag ->
                FilterChip(
                  selected = false,
                  onClick = { onChange(state.copy(notInterestedTopics = state.notInterestedTopics + tag), false) },
                  label = { Text("+#$tag", fontSize = 12.sp) },
                )
              }
          }
        }
      }
    }

    item {
      Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          ChipSelectorRow(
            label = "Language",
            options = AppLanguageOption.entries.map { it.label to (state.language == it) },
            onSelect = { idx -> onChange(state.copy(language = AppLanguageOption.entries[idx]), false) },
          )
          ChipSelectorRow(
            label = "Video quality",
            options = VideoQualityOption.entries.map { it.label to (state.videoQuality == it) },
            onSelect = { idx -> onChange(state.copy(videoQuality = VideoQualityOption.entries[idx]), false) },
          )
        }
      }
    }

    item {
      SettingsToggleGroupCard(
        title = "Playback & accessibility",
        items = listOf(
          ToggleRowSpec("Autoplay videos", null, state.autoplayVideos, tag = "content_autoplay_switch") {
            onChange(state.copy(autoplayVideos = it), false)
          },
          ToggleRowSpec("Data saver", "Lower video bitrate on feeds", state.dataSaver, tag = "content_datasaver_switch") {
            onChange(state.copy(dataSaver = it), false)
          },
          ToggleRowSpec("Captions", "Show video captions overlay", state.captionsEnabled, tag = "content_captions_switch") {
            onChange(state.copy(captionsEnabled = it), false)
          },
          ToggleRowSpec("Reduced motion", "Minimize animations", state.reducedMotion, tag = "content_reduced_motion_switch") {
            onChange(state.copy(reducedMotion = it), false)
          },
        ),
      )
    }
  }
}

private data class ToggleRowSpec(
  val label: String,
  val subtitle: String? = null,
  val checked: Boolean,
  val tag: String? = null,
  val onCheckedChange: (Boolean) -> Unit,
)

@Composable
private fun SettingsToggleGroupCard(
  title: String,
  enabled: Boolean = true,
  items: List<ToggleRowSpec>,
) {
  Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
      items.forEachIndexed { idx, row ->
        if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(row.label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            if (!row.subtitle.isNullOrBlank()) {
              Text(row.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          Switch(
            checked = row.checked,
            enabled = enabled,
            onCheckedChange = row.onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = TelefamPureWhite, checkedTrackColor = TelefamCrimson),
            modifier = if (row.tag != null) Modifier.testTag(row.tag) else Modifier,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipSelectorRow(
  label: String,
  options: List<Pair<String, Boolean>>,
  onSelect: (Int) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      options.forEachIndexed { idx, (optLabel, selected) ->
        FilterChip(
          selected = selected,
          onClick = { onSelect(idx) },
          label = { Text(optLabel, fontSize = 12.5.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = TelefamCrimson,
            selectedLabelColor = TelefamPureWhite,
          ),
        )
      }
    }
  }
}

@Composable
internal fun SettingsAvatarBadge(
  avatarUrl: String?,
  displayName: String,
  size: Dp,
) {
  val localBmp = remember(avatarUrl) {
    if (!avatarUrl.isNullOrBlank() && !avatarUrl.startsWith("http") && File(avatarUrl).exists()) {
      BitmapFactory.decodeFile(avatarUrl)?.asImageBitmap()
    } else null
  }
  Box(
    modifier = Modifier
      .size(size)
      .clip(CircleShape)
      .background(TelefamCrimson.copy(alpha = 0.14f))
      .border(1.dp, TelefamCrimson.copy(alpha = 0.35f), CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    if (localBmp != null) {
      Image(bitmap = localBmp, contentDescription = displayName, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    } else if (!avatarUrl.isNullOrBlank() && avatarUrl.startsWith("http")) {
      AsyncImage(model = avatarUrl, contentDescription = displayName, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    } else {
      val initials = displayName.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercaseChar().toString() }
      if (initials.isNotEmpty()) {
        Text(initials, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TelefamCrimson))
      } else {
        Icon(Icons.Filled.Person, contentDescription = displayName, tint = TelefamCrimson)
      }
    }
  }
}
