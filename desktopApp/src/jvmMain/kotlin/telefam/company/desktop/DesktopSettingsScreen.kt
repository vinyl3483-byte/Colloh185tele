package telefam.company.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import telefam.company.shared.TelefamContactsRules
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class DesktopSettingsSvgKind {
  ACTIVITY_PULSE,
  SHARE_CARD,
  BELL,
  HEART,
  BOOKMARK_LINK,
  CLOUD_SYNC,
  LOCK,
  BLOCK_MINUS,
  FLAG,
  SHIELD_CHECK,
  HELP_QUESTION,
  DOCUMENT_LINES,
  INFO_CIRCLE,
}

private data class DesktopSettingsRowModel(
  val id: String,
  val section: String,
  val title: String,
  val subtitle: String,
  val iconKind: DesktopSettingsSvgKind,
  val destination: DesktopSettingsDestination,
  val keywords: String,
)

@Composable
fun DesktopSettingsAndPrivacyScreen(
  apiClient: DesktopApiClient,
  isDark: Boolean,
  settingsBundle: DesktopSettingsBundle,
  avatarUrl: String,
  onSettingsUpdated: (DesktopSettingsBundle) -> Unit,
  onToggleTheme: () -> Unit,
  onLoggedOut: (allDevices: Boolean) -> Unit,
  onClose: () -> Unit,
  onToast: (String) -> Unit,
) {
  val scope = rememberCoroutineScope()
  val pageBg = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF3F5F8)

  val backStack = remember { mutableStateOf(listOf(DesktopSettingsDestination.HUB)) }
  var searchQuery by remember { mutableStateOf("") }
  var showLogoutDialog by remember { mutableStateOf(false) }
  var logoutAllDevices by remember { mutableStateOf(false) }

  var restrictedTab by remember { mutableStateOf(0) } // 0 = blocked, 1 = muted
  var restrictedQuery by remember { mutableStateOf("") }
  var blockedList by remember { mutableStateOf<List<DesktopRestrictedAccount>>(emptyList()) }
  var mutedList by remember { mutableStateOf<List<DesktopRestrictedAccount>>(emptyList()) }
  var restrictedLoading by remember { mutableStateOf(false) }

  var faqQuery by remember { mutableStateOf("") }
  var faqs by remember { mutableStateOf(DesktopSettingsCatalog.defaultFaqs) }
  var tickets by remember { mutableStateOf<List<DesktopSupportTicket>>(emptyList()) }
  var lastTicket by remember { mutableStateOf<DesktopSupportTicket?>(null) }
  var statusList by remember { mutableStateOf<List<DesktopSystemServiceStatus>>(emptyList()) }
  var legalDocs by remember { mutableStateOf(DesktopSettingsCatalog.legalDocuments) }
  var selectedLegalDocId by remember { mutableStateOf<String?>(null) }
  var versionCheck by remember { mutableStateOf<DesktopVersionCheckResult?>(null) }
  var pendingSyncCount by remember { mutableStateOf(0) }
  var isSyncingNow by remember { mutableStateOf(false) }
  var syncFeedback by remember { mutableStateOf("") }

  val currentDest = backStack.value.lastOrNull() ?: DesktopSettingsDestination.HUB

  val navigateBack: () -> Unit = {
    if (selectedLegalDocId != null) {
      selectedLegalDocId = null
    } else if (backStack.value.size > 1) {
      backStack.value = backStack.value.dropLast(1)
    } else {
      onClose()
    }
  }

  val openDestination: (DesktopSettingsDestination) -> Unit = { dest ->
    selectedLegalDocId = null
    val cur = backStack.value
    backStack.value = if (cur.size >= 2 && cur.last() == dest) cur else listOf(DesktopSettingsDestination.HUB, dest)
  }

  LaunchedEffect(Unit) {
    pendingSyncCount = withContext(Dispatchers.IO) { apiClient.pendingSettingsOutboxCount() }
    val fresh = withContext(Dispatchers.IO) { apiClient.getSettings() }
    onSettingsUpdated(fresh)
    pendingSyncCount = withContext(Dispatchers.IO) { apiClient.pendingSettingsOutboxCount() }
  }

  LaunchedEffect(currentDest) {
    if (currentDest == DesktopSettingsDestination.OFFLINE_SYNC || currentDest == DesktopSettingsDestination.HUB) {
      pendingSyncCount = withContext(Dispatchers.IO) { apiClient.pendingSettingsOutboxCount() }
    }
  }

  LaunchedEffect(currentDest, restrictedQuery, restrictedTab) {
    if (currentDest == DesktopSettingsDestination.BLOCKED_ACCOUNTS) {
      delay(120L)
      restrictedLoading = true
      val blk = withContext(Dispatchers.IO) { apiClient.listBlockedAccounts(restrictedQuery) }
      val mut = withContext(Dispatchers.IO) { apiClient.listMutedAccounts(restrictedQuery) }
      blockedList = blk
      mutedList = mut
      restrictedLoading = false
    }
  }

  LaunchedEffect(currentDest, faqQuery) {
    if (currentDest == DesktopSettingsDestination.HELP_SUPPORT) {
      delay(140L)
      faqs = withContext(Dispatchers.IO) { apiClient.searchFaqs(faqQuery) }
      tickets = withContext(Dispatchers.IO) { apiClient.listSupportTickets() }
      statusList = withContext(Dispatchers.IO) { apiClient.getSystemStatus() }
    } else if (currentDest == DesktopSettingsDestination.TERMS_POLICIES) {
      legalDocs = withContext(Dispatchers.IO) { apiClient.getLegalDocuments() }
    }
  }

  val fullName = apiClient.session.fullName.ifBlank { "Telefam" }
  val username = apiClient.session.username.ifBlank { "user" }

  val destTitle = when (currentDest) {
    DesktopSettingsDestination.HUB -> "Settings and privacy"
    DesktopSettingsDestination.PRIVACY -> "Privacy"
    DesktopSettingsDestination.SHARE_PROFILE -> "Share profile"
    DesktopSettingsDestination.NOTIFICATIONS -> "Notifications"
    DesktopSettingsDestination.CONTENT_PREFERENCES -> "Content preferences"
    DesktopSettingsDestination.SAVED_LINKS -> "Saved links"
    DesktopSettingsDestination.OFFLINE_SYNC -> "Offline sync"
    DesktopSettingsDestination.BLOCKED_ACCOUNTS -> "Blocked & muted"
    DesktopSettingsDestination.REPORT_PROBLEM -> "Report a problem"
    DesktopSettingsDestination.SAFETY_CENTER -> "Safety Center"
    DesktopSettingsDestination.HELP_SUPPORT -> "Help & support"
    DesktopSettingsDestination.TERMS_POLICIES -> "Terms and policies"
    DesktopSettingsDestination.ABOUT -> "About"
  }

  Surface(
    modifier = Modifier
      .fillMaxSize()
      .onPreviewKeyEvent { ev ->
        if (ev.type == KeyEventType.KeyDown && ev.key == Key.Escape) {
          if (showLogoutDialog) {
            showLogoutDialog = false
          } else {
            navigateBack()
          }
          true
        } else {
          false
        }
      },
    color = pageBg,
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 820.dp

        Column(modifier = Modifier.fillMaxSize()) {
          // Curved crimson header matching uploaded PNG
          Surface(
            color = DesktopCrimson,
            shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
              Surface(
                onClick = navigateBack,
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.16f),
                modifier = Modifier.align(Alignment.CenterStart).size(42.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  DesktopSvgIcon(DesktopIconKind.BACK_ARROW, Color.White, 20.dp)
                }
              }

              Text(
                text = if (isWide) "Settings and privacy" else destTitle,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
              )
            }
          }

          if (isWide) {
            val activeRight = if (currentDest == DesktopSettingsDestination.HUB) DesktopSettingsDestination.PRIVACY else currentDest
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
              Box(modifier = Modifier.width(380.dp).fillMaxHeight()) {
                DesktopSettingsHubBody(
                  apiClient = apiClient,
                  fullName = fullName,
                  username = username,
                  avatarUrl = avatarUrl,
                  searchQuery = searchQuery,
                  pendingSyncCount = pendingSyncCount,
                  selectedDest = activeRight,
                  onSearchQueryChanged = { searchQuery = it },
                  onToggleTheme = onToggleTheme,
                  onSelectDestination = openDestination,
                  onRequestLogout = { showLogoutDialog = true },
                )
              }
              HorizontalDivider(modifier = Modifier.width(1.dp).fillMaxHeight(), color = MaterialTheme.colorScheme.outlineVariant)
              Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxSize()) {
                  Text(
                    text = when (activeRight) {
                      DesktopSettingsDestination.PRIVACY -> "Privacy"
                      DesktopSettingsDestination.SHARE_PROFILE -> "Share profile"
                      DesktopSettingsDestination.NOTIFICATIONS -> "Notifications"
                      DesktopSettingsDestination.CONTENT_PREFERENCES -> "Content preferences"
                      DesktopSettingsDestination.SAVED_LINKS -> "Saved links"
                      DesktopSettingsDestination.OFFLINE_SYNC -> "Offline sync"
                      DesktopSettingsDestination.BLOCKED_ACCOUNTS -> "Blocked & muted accounts"
                      DesktopSettingsDestination.REPORT_PROBLEM -> "Report a problem"
                      DesktopSettingsDestination.SAFETY_CENTER -> "Safety Center"
                      DesktopSettingsDestination.HELP_SUPPORT -> "Help & support"
                      DesktopSettingsDestination.TERMS_POLICIES -> "Terms and policies"
                      DesktopSettingsDestination.ABOUT -> "About"
                      DesktopSettingsDestination.HUB -> "Privacy"
                    },
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                  )
                  DesktopSettingsDetailHost(
                    destination = activeRight,
                    apiClient = apiClient,
                    fullName = fullName,
                    username = username,
                    avatarUrl = avatarUrl,
                    bundle = settingsBundle,
                    restrictedTab = restrictedTab,
                    restrictedQuery = restrictedQuery,
                    blockedList = blockedList,
                    mutedList = mutedList,
                    restrictedLoading = restrictedLoading,
                    faqQuery = faqQuery,
                    faqs = faqs,
                    tickets = tickets,
                    lastTicket = lastTicket,
                    statusList = statusList,
                    legalDocs = legalDocs,
                    selectedLegalDocId = selectedLegalDocId,
                    versionCheck = versionCheck,
                    onUpdatePrivacy = { next ->
                      val updated = settingsBundle.copy(privacy = next)
                      onSettingsUpdated(updated)
                      scope.launch {
                        val synced = withContext(Dispatchers.IO) { apiClient.updatePrivacySettings(next) }
                        onSettingsUpdated(synced)
                      }
                    },
                    onUpdateNotifications = { next ->
                      val updated = settingsBundle.copy(notifications = next)
                      onSettingsUpdated(updated)
                      scope.launch {
                        val synced = withContext(Dispatchers.IO) { apiClient.updateNotificationSettings(next) }
                        onSettingsUpdated(synced)
                      }
                    },
                    onUpdateContentPrefs = { next, reset ->
                      val updated = settingsBundle.copy(contentPreferences = next)
                      onSettingsUpdated(updated)
                      scope.launch {
                        val synced = withContext(Dispatchers.IO) { apiClient.updateContentPreferences(next, reset) }
                        onSettingsUpdated(synced)
                      }
                      if (reset) onToast("Suggested content feed reset")
                    },
                    onRestrictedTabChanged = { restrictedTab = it },
                    onRestrictedQueryChanged = { restrictedQuery = it },
                    onUnblockUser = { uid ->
                      blockedList = blockedList.filterNot { it.userId == uid }
                      scope.launch {
                        withContext(Dispatchers.IO) { apiClient.unblockUser(uid) }
                        onToast("Account unblocked")
                      }
                    },
                    onUnmuteUser = { uid ->
                      mutedList = mutedList.filterNot { it.userId == uid }
                      scope.launch {
                        withContext(Dispatchers.IO) { apiClient.unmuteUser(uid) }
                        onToast("Account unmuted")
                      }
                    },
                    onSubmitProblemReport = { cat, desc, bytes ->
                      scope.launch {
                        val t = withContext(Dispatchers.IO) { apiClient.submitProblemReport(cat, desc, bytes) }
                        lastTicket = t
                        tickets = listOf(t) + tickets.filterNot { it.ticketId == t.ticketId }
                        onToast("Ticket ${t.referenceNumber} submitted")
                      }
                    },
                    onClearLastTicket = { lastTicket = null },
                    onFaqQueryChanged = { faqQuery = it },
                    onRefreshTickets = {
                      scope.launch {
                        tickets = withContext(Dispatchers.IO) { apiClient.listSupportTickets() }
                      }
                    },
                    onRefreshStatus = {
                      scope.launch {
                        statusList = withContext(Dispatchers.IO) { apiClient.getSystemStatus() }
                      }
                    },
                    onSelectLegalDoc = { selectedLegalDocId = it },
                    onCheckUpdates = {
                      scope.launch {
                        versionCheck = withContext(Dispatchers.IO) { apiClient.checkAppVersion() }
                      }
                    },
                    pendingSyncCount = pendingSyncCount,
                    isSyncingNow = isSyncingNow,
                    syncFeedback = syncFeedback,
                    onUpdateSyncPrefs = { next ->
                      val updated = settingsBundle.copy(syncPreferences = next)
                      onSettingsUpdated(updated)
                      scope.launch {
                        val synced = withContext(Dispatchers.IO) { apiClient.updateSyncPreferences(next) }
                        onSettingsUpdated(synced)
                        pendingSyncCount = withContext(Dispatchers.IO) { apiClient.pendingSettingsOutboxCount() }
                      }
                    },
                    onTriggerSyncNow = {
                      if (!isSyncingNow) {
                        isSyncingNow = true
                        syncFeedback = "Syncing queued changes with server..."
                        scope.launch {
                          val res = withContext(Dispatchers.IO) { apiClient.syncNowToServer() }
                          onSettingsUpdated(res.bundle)
                          pendingSyncCount = res.pendingRemaining
                          syncFeedback = res.message
                          isSyncingNow = false
                          onToast(res.message)
                        }
                      }
                    },
                    onRefreshSyncStatus = {
                      scope.launch {
                        val fresh = withContext(Dispatchers.IO) { apiClient.getSettings() }
                        onSettingsUpdated(fresh)
                        pendingSyncCount = withContext(Dispatchers.IO) { apiClient.pendingSettingsOutboxCount() }
                      }
                    },
                    onOpenDestination = openDestination,
                    onToast = onToast,
                  )
                }
              }
            }
          } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
              if (currentDest == DesktopSettingsDestination.HUB) {
                DesktopSettingsHubBody(
                  apiClient = apiClient,
                  fullName = fullName,
                  username = username,
                  avatarUrl = avatarUrl,
                  searchQuery = searchQuery,
                  pendingSyncCount = pendingSyncCount,
                  selectedDest = null,
                  onSearchQueryChanged = { searchQuery = it },
                  onToggleTheme = onToggleTheme,
                  onSelectDestination = openDestination,
                  onRequestLogout = { showLogoutDialog = true },
                )
              } else {
                DesktopSettingsDetailHost(
                  destination = currentDest,
                  apiClient = apiClient,
                  fullName = fullName,
                  username = username,
                  avatarUrl = avatarUrl,
                  bundle = settingsBundle,
                  restrictedTab = restrictedTab,
                  restrictedQuery = restrictedQuery,
                  blockedList = blockedList,
                  mutedList = mutedList,
                  restrictedLoading = restrictedLoading,
                  faqQuery = faqQuery,
                  faqs = faqs,
                  tickets = tickets,
                  lastTicket = lastTicket,
                  statusList = statusList,
                  legalDocs = legalDocs,
                  selectedLegalDocId = selectedLegalDocId,
                  versionCheck = versionCheck,
                  onUpdatePrivacy = { next ->
                    val updated = settingsBundle.copy(privacy = next)
                    onSettingsUpdated(updated)
                    scope.launch {
                      val synced = withContext(Dispatchers.IO) { apiClient.updatePrivacySettings(next) }
                      onSettingsUpdated(synced)
                    }
                  },
                  onUpdateNotifications = { next ->
                    val updated = settingsBundle.copy(notifications = next)
                    onSettingsUpdated(updated)
                    scope.launch {
                      val synced = withContext(Dispatchers.IO) { apiClient.updateNotificationSettings(next) }
                      onSettingsUpdated(synced)
                    }
                  },
                  onUpdateContentPrefs = { next, reset ->
                    val updated = settingsBundle.copy(contentPreferences = next)
                    onSettingsUpdated(updated)
                    scope.launch {
                      val synced = withContext(Dispatchers.IO) { apiClient.updateContentPreferences(next, reset) }
                      onSettingsUpdated(synced)
                    }
                    if (reset) onToast("Suggested content feed reset")
                  },
                  onRestrictedTabChanged = { restrictedTab = it },
                  onRestrictedQueryChanged = { restrictedQuery = it },
                  onUnblockUser = { uid ->
                    blockedList = blockedList.filterNot { it.userId == uid }
                    scope.launch {
                      withContext(Dispatchers.IO) { apiClient.unblockUser(uid) }
                      onToast("Account unblocked")
                    }
                  },
                  onUnmuteUser = { uid ->
                    mutedList = mutedList.filterNot { it.userId == uid }
                    scope.launch {
                      withContext(Dispatchers.IO) { apiClient.unmuteUser(uid) }
                      onToast("Account unmuted")
                    }
                  },
                  onSubmitProblemReport = { cat, desc, bytes ->
                    scope.launch {
                      val t = withContext(Dispatchers.IO) { apiClient.submitProblemReport(cat, desc, bytes) }
                      lastTicket = t
                      tickets = listOf(t) + tickets.filterNot { it.ticketId == t.ticketId }
                      onToast("Ticket ${t.referenceNumber} submitted")
                    }
                  },
                  onClearLastTicket = { lastTicket = null },
                  onFaqQueryChanged = { faqQuery = it },
                  onRefreshTickets = {
                    scope.launch {
                      tickets = withContext(Dispatchers.IO) { apiClient.listSupportTickets() }
                    }
                  },
                  onRefreshStatus = {
                    scope.launch {
                      statusList = withContext(Dispatchers.IO) { apiClient.getSystemStatus() }
                    }
                  },
                  onSelectLegalDoc = { selectedLegalDocId = it },
                  onCheckUpdates = {
                    scope.launch {
                      versionCheck = withContext(Dispatchers.IO) { apiClient.checkAppVersion() }
                    }
                  },
                  pendingSyncCount = pendingSyncCount,
                  isSyncingNow = isSyncingNow,
                  syncFeedback = syncFeedback,
                  onUpdateSyncPrefs = { next ->
                    val updated = settingsBundle.copy(syncPreferences = next)
                    onSettingsUpdated(updated)
                    scope.launch {
                      val synced = withContext(Dispatchers.IO) { apiClient.updateSyncPreferences(next) }
                      onSettingsUpdated(synced)
                      pendingSyncCount = withContext(Dispatchers.IO) { apiClient.pendingSettingsOutboxCount() }
                    }
                  },
                  onTriggerSyncNow = {
                    if (!isSyncingNow) {
                      isSyncingNow = true
                      syncFeedback = "Syncing queued changes with server..."
                      scope.launch {
                        val res = withContext(Dispatchers.IO) { apiClient.syncNowToServer() }
                        onSettingsUpdated(res.bundle)
                        pendingSyncCount = res.pendingRemaining
                        syncFeedback = res.message
                        isSyncingNow = false
                        onToast(res.message)
                      }
                    }
                  },
                  onRefreshSyncStatus = {
                    scope.launch {
                      val fresh = withContext(Dispatchers.IO) { apiClient.getSettings() }
                      onSettingsUpdated(fresh)
                      pendingSyncCount = withContext(Dispatchers.IO) { apiClient.pendingSettingsOutboxCount() }
                    }
                  },
                  onOpenDestination = openDestination,
                  onToast = onToast,
                )
              }
            }
          }
        }
      }

      if (showLogoutDialog) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.54f))
            .clickable { showLogoutDialog = false },
          contentAlignment = Alignment.Center,
        ) {
          Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.width(380.dp).clickable(enabled = false) {},
          ) {
            Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Text("Log out of this device?", fontSize = 19.sp, fontWeight = FontWeight.Bold)
              Text(
                "You can sign back in anytime using your verified email address.",
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { logoutAllDevices = !logoutAllDevices }
                  .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Checkbox(
                  checked = logoutAllDevices,
                  onCheckedChange = { logoutAllDevices = it },
                  colors = CheckboxDefaults.colors(checkedColor = DesktopCrimson),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                  Text("Log out of all devices", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                  Text(
                    "Revoke all active phones, tablets, and desktop sessions.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                  onClick = { showLogoutDialog = false },
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.weight(1f).height(44.dp),
                ) {
                  Text("Cancel")
                }
                Button(
                  onClick = {
                    val all = logoutAllDevices
                    showLogoutDialog = false
                    onLoggedOut(all)
                  },
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = DesktopCrimson),
                  modifier = Modifier.weight(1f).height(44.dp),
                ) {
                  Text("Log out", color = Color.White, fontWeight = FontWeight.Bold)
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
private fun DesktopSettingsHubBody(
  apiClient: DesktopApiClient,
  fullName: String,
  username: String,
  avatarUrl: String,
  searchQuery: String,
  pendingSyncCount: Int,
  selectedDest: DesktopSettingsDestination?,
  onSearchQueryChanged: (String) -> Unit,
  onToggleTheme: () -> Unit,
  onSelectDestination: (DesktopSettingsDestination) -> Unit,
  onRequestLogout: () -> Unit,
) {
  val syncSubtitle = if (pendingSyncCount > 0) {
    "$pendingSyncCount queued · Sync now"
  } else {
    "Auto-sync, Wi-Fi only, cache"
  }
  val allRows = remember(syncSubtitle) {
    listOf(
      DesktopSettingsRowModel("account_privacy", "Account", "Privacy", "Private account, blocked accounts, activity", DesktopSettingsSvgKind.ACTIVITY_PULSE, DesktopSettingsDestination.PRIVACY, "private account discovery activity comment mention"),
      DesktopSettingsRowModel("share_profile", "Account", "Share profile", "QR code, link, copy profile URL", DesktopSettingsSvgKind.SHARE_CARD, DesktopSettingsDestination.SHARE_PROFILE, "qr code link url invite"),
      DesktopSettingsRowModel("notifications", "Content & activity", "Notifications", "Push, email, SMS", DesktopSettingsSvgKind.BELL, DesktopSettingsDestination.NOTIFICATIONS, "push email sms quiet hours pause alerts"),
      DesktopSettingsRowModel("content_prefs", "Content & activity", "Content preferences", "Suggested content, language, video quality", DesktopSettingsSvgKind.HEART, DesktopSettingsDestination.CONTENT_PREFERENCES, "autoplay captions reduced motion data saver topics language"),
      DesktopSettingsRowModel("saved_links", "Content & activity", "Saved links", "Bookmarked web links and previews", DesktopSettingsSvgKind.BOOKMARK_LINK, DesktopSettingsDestination.SAVED_LINKS, "saved links bookmarks urls browser websites"),
      DesktopSettingsRowModel("offline_sync", "Content & activity", "Offline sync", syncSubtitle, DesktopSettingsSvgKind.CLOUD_SYNC, DesktopSettingsDestination.OFFLINE_SYNC, "offline sync queue cache server wifi background upload"),
      DesktopSettingsRowModel("safety_privacy", "Privacy and safety", "Privacy", "Who can see your videos, likes, comments, following", DesktopSettingsSvgKind.LOCK, DesktopSettingsDestination.PRIVACY, "likes following visibility cookies data sharing"),
      DesktopSettingsRowModel("blocked_accounts", "Privacy and safety", "Blocked accounts", "Manage blocked users", DesktopSettingsSvgKind.BLOCK_MINUS, DesktopSettingsDestination.BLOCKED_ACCOUNTS, "unblock muted mute restrict"),
      DesktopSettingsRowModel("report_problem", "Privacy and safety", "Report a problem", "Send feedback, report abuse", DesktopSettingsSvgKind.FLAG, DesktopSettingsDestination.REPORT_PROBLEM, "bug feedback screenshot ticket reference"),
      DesktopSettingsRowModel("safety_center", "Privacy and safety", "Safety Center", "Tips, tools, resources", DesktopSettingsSvgKind.SHIELD_CHECK, DesktopSettingsDestination.SAFETY_CENTER, "parents guardians emergency helpline abuse reporting"),
      DesktopSettingsRowModel("help_support", "Support & about", "Help & support", "FAQs, contact us, report issue", DesktopSettingsSvgKind.HELP_QUESTION, DesktopSettingsDestination.HELP_SUPPORT, "faq tickets system status outage"),
      DesktopSettingsRowModel("terms_policies", "Support & about", "Terms and policies", "Terms of Service, Privacy Policy, Community Guidelines", DesktopSettingsSvgKind.DOCUMENT_LINES, DesktopSettingsDestination.TERMS_POLICIES, "legal cookies licenses privacy policy guidelines"),
      DesktopSettingsRowModel("about", "Support & about", "About", "Version, copyright, open source", DesktopSettingsSvgKind.INFO_CIRCLE, DesktopSettingsDestination.ABOUT, "version build update check open source license"),
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
  val sections = remember(filtered) { filtered.groupBy { it.section } }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp), shadowElevation = 1.dp) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          DesktopSettingsAvatar(apiClient, avatarUrl, fullName, 48.dp)
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(fullName, fontSize = 15.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("@$username", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
          }
          Surface(
            onClick = onToggleTheme,
            shape = RoundedCornerShape(16.dp),
            color = DesktopCrimson.copy(alpha = 0.12f),
          ) {
            Text(
              text = apiClient.session.themeMode.name.lowercase().replaceFirstChar { it.uppercase() },
              color = DesktopCrimson,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
          }
        }
      }
    }

    item {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().height(46.dp),
      ) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
          DesktopSvgIcon(DesktopIconKind.SEARCH, MaterialTheme.colorScheme.onSurfaceVariant, 17.dp)
          Spacer(modifier = Modifier.width(10.dp))
          Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (searchQuery.isEmpty()) {
              Text("Search settings", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BasicTextField(
              value = searchQuery,
              onValueChange = { onSearchQueryChanged(it.take(64)) },
              singleLine = true,
              textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
              cursorBrush = SolidColor(DesktopCrimson),
              modifier = Modifier.fillMaxWidth(),
            )
          }
          if (searchQuery.isNotEmpty()) {
            Box(modifier = Modifier.size(26.dp).clip(CircleShape).clickable { onSearchQueryChanged("") }, contentAlignment = Alignment.Center) {
              DesktopSvgIcon(DesktopIconKind.CLOSE, MaterialTheme.colorScheme.onSurfaceVariant, 14.dp)
            }
          }
        }
      }
    }

    if (sections.isEmpty()) {
      item {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
          Text("No matching settings", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    } else {
      sections.forEach { (sectionTitle, rows) ->
        item(key = "sec_$sectionTitle") {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = sectionTitle,
              fontSize = 13.5.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp), shadowElevation = 1.dp) {
              Column(modifier = Modifier.fillMaxWidth()) {
                rows.forEachIndexed { idx, row ->
                  val isSel = selectedDest == row.destination
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .background(if (isSel) DesktopCrimson.copy(alpha = 0.06f) else Color.Transparent)
                      .clickable { onSelectDestination(row.destination) }
                      .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    DesktopRedCircleSvgBadge(row.iconKind, 42.dp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(row.title, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
                      Spacer(modifier = Modifier.height(2.dp))
                      Text(row.subtitle, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    DesktopSvgIcon(DesktopIconKind.CHEVRON_RIGHT, DesktopCrimson, 16.dp)
                  }
                  if (idx < rows.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp, end = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                  }
                }
              }
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(6.dp))
      Button(
        onClick = onRequestLogout,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DesktopCrimson),
        modifier = Modifier.fillMaxWidth().height(54.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Canvas(modifier = Modifier.size(20.dp)) {
            val w = size.width
            val h = size.height
            val sw = 2.dp.toPx()
            drawLine(Color.White, Offset(w * 0.2f, h * 0.2f), Offset(w * 0.2f, h * 0.8f), sw, StrokeCap.Round)
            drawLine(Color.White, Offset(w * 0.2f, h * 0.2f), Offset(w * 0.56f, h * 0.2f), sw, StrokeCap.Round)
            drawLine(Color.White, Offset(w * 0.2f, h * 0.8f), Offset(w * 0.56f, h * 0.8f), sw, StrokeCap.Round)
            drawLine(Color.White, Offset(w * 0.42f, h * 0.5f), Offset(w * 0.85f, h * 0.5f), sw, StrokeCap.Round)
            drawLine(Color.White, Offset(w * 0.68f, h * 0.34f), Offset(w * 0.85f, h * 0.5f), sw, StrokeCap.Round)
            drawLine(Color.White, Offset(w * 0.68f, h * 0.66f), Offset(w * 0.85f, h * 0.5f), sw, StrokeCap.Round)
          }
          Text("Log out", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
      }
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@Composable
private fun DesktopSettingsDetailHost(
  destination: DesktopSettingsDestination,
  apiClient: DesktopApiClient,
  fullName: String,
  username: String,
  avatarUrl: String,
  bundle: DesktopSettingsBundle,
  restrictedTab: Int,
  restrictedQuery: String,
  blockedList: List<DesktopRestrictedAccount>,
  mutedList: List<DesktopRestrictedAccount>,
  restrictedLoading: Boolean,
  faqQuery: String,
  faqs: List<DesktopFaqEntry>,
  tickets: List<DesktopSupportTicket>,
  lastTicket: DesktopSupportTicket?,
  statusList: List<DesktopSystemServiceStatus>,
  legalDocs: List<DesktopLegalDoc>,
  selectedLegalDocId: String?,
  versionCheck: DesktopVersionCheckResult?,
  onUpdatePrivacy: (DesktopPrivacySettings) -> Unit,
  onUpdateNotifications: (DesktopNotificationSettings) -> Unit,
  onUpdateContentPrefs: (DesktopContentPreferences, Boolean) -> Unit,
  onRestrictedTabChanged: (Int) -> Unit,
  onRestrictedQueryChanged: (String) -> Unit,
  onUnblockUser: (String) -> Unit,
  onUnmuteUser: (String) -> Unit,
  onSubmitProblemReport: (String, String, ByteArray?) -> Unit,
  onClearLastTicket: () -> Unit,
  onFaqQueryChanged: (String) -> Unit,
  onRefreshTickets: () -> Unit,
  onRefreshStatus: () -> Unit,
  onSelectLegalDoc: (String?) -> Unit,
  onCheckUpdates: () -> Unit,
  pendingSyncCount: Int,
  isSyncingNow: Boolean,
  syncFeedback: String,
  onUpdateSyncPrefs: (DesktopSyncPreferences) -> Unit,
  onTriggerSyncNow: () -> Unit,
  onRefreshSyncStatus: () -> Unit,
  onOpenDestination: (DesktopSettingsDestination) -> Unit,
  onToast: (String) -> Unit,
) {
  when (destination) {
    DesktopSettingsDestination.HUB,
    DesktopSettingsDestination.PRIVACY -> {
      val p = bundle.privacy
      LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        item {
          DesktopSectionCard("Account visibility") {
            DesktopToggleRow("Private account", "Only approved followers can view your posts, followers, and following lists.", p.isPrivateAccount) {
              onUpdatePrivacy(p.copy(isPrivateAccount = it))
            }
            DesktopRowDivider()
            DesktopToggleRow("Activity status", "Let mutual contacts see when you were recently active.", p.showActivityStatus) {
              onUpdatePrivacy(p.copy(showActivityStatus = it))
            }
          }
        }
        item {
          DesktopSectionCard("Interactions & discovery") {
            DesktopOptionChipsRow("Who can comment", DesktopAudienceOption.entries.map { it.label }, p.whoCanComment.ordinal) { idx ->
              onUpdatePrivacy(p.copy(whoCanComment = DesktopAudienceOption.entries[idx]))
            }
            DesktopRowDivider()
            DesktopOptionChipsRow("Who can mention you", DesktopAudienceOption.entries.map { it.label }, p.whoCanMention.ordinal) { idx ->
              onUpdatePrivacy(p.copy(whoCanMention = DesktopAudienceOption.entries[idx]))
            }
            DesktopRowDivider()
            DesktopOptionChipsRow("Who can find you", DesktopDiscoverabilityOption.entries.map { it.label }, p.whoCanFindYou.ordinal) { idx ->
              onUpdatePrivacy(p.copy(whoCanFindYou = DesktopDiscoverabilityOption.entries[idx]))
            }
          }
        }
        item {
          DesktopSectionCard("Likes & following visibility") {
            DesktopOptionChipsRow("Who can see liked posts", DesktopVisibilityOption.entries.map { it.label }, p.likesVisibility.ordinal) { idx ->
              onUpdatePrivacy(p.copy(likesVisibility = DesktopVisibilityOption.entries[idx]))
            }
            DesktopRowDivider()
            DesktopOptionChipsRow("Who can see following list", DesktopVisibilityOption.entries.map { it.label }, p.followingVisibility.ordinal) { idx ->
              onUpdatePrivacy(p.copy(followingVisibility = DesktopVisibilityOption.entries[idx]))
            }
          }
        }
        item {
          DesktopSectionCard("Data sharing & cookies") {
            DesktopToggleRow("Personalization & tailored ranking", "Use watch history and interests to personalize For You.", p.dataSharingPersonalization) {
              onUpdatePrivacy(p.copy(dataSharingPersonalization = it))
            }
            DesktopRowDivider()
            DesktopToggleRow("Analytics & playback diagnostics", "Help measure startup latency and stream stability.", p.analyticsSharing) {
              onUpdatePrivacy(p.copy(analyticsSharing = it))
            }
            DesktopRowDivider()
            DesktopToggleRow("Personalization cookies", "Essential session security remains always active.", p.personalizationCookies) {
              onUpdatePrivacy(p.copy(personalizationCookies = it))
            }
          }
        }
      }
    }

    DesktopSettingsDestination.SHARE_PROFILE -> {
      val shareUrl = apiClient.shareUrlForProfile(username)
      val qrMatrix = remember(shareUrl) { DesktopQrEncoder.encodeToMatrix(shareUrl) }
      LazyColumn(contentPadding = PaddingValues(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
        item {
          Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp), shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
              DesktopSettingsAvatar(apiClient, avatarUrl, fullName, 64.dp)
              Text(fullName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
              Text("@$username", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Box(
                modifier = Modifier
                  .size(216.dp)
                  .clip(RoundedCornerShape(18.dp))
                  .background(Color.White)
                  .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(18.dp))
                  .padding(14.dp),
              ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                  val count = qrMatrix.size
                  val cell = size.minDimension / count.toFloat()
                  for (r in 0 until count) {
                    for (c in 0 until count) {
                      if (qrMatrix[r][c]) {
                        drawRect(Color(0xFF111318), Offset(c * cell, r * cell), Size(cell + 0.4f, cell + 0.4f))
                      }
                    }
                  }
                }
              }
              Text("Scan QR code to open @$username on Telefam", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Surface(color = DesktopCrimson.copy(alpha = 0.08f), shape = RoundedCornerShape(12.dp)) {
                Text(shareUrl, color = DesktopCrimson, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
              }
              Button(
                onClick = {
                  runCatching { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(shareUrl), null) }
                  onToast("Profile link copied")
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DesktopCrimson),
                modifier = Modifier.fillMaxWidth().height(48.dp),
              ) {
                Text("Copy profile link", color = Color.White, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    DesktopSettingsDestination.NOTIFICATIONS -> {
      val n = bundle.notifications
      LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        item {
          DesktopSectionCard("Master control") {
            DesktopToggleRow("Pause all notifications", "Temporarily silence alerts across all channels.", n.masterPause) {
              onUpdateNotifications(n.copy(masterPause = it))
            }
          }
        }
        item {
          DesktopSectionCard("Delivery channels") {
            DesktopToggleRow("Push notifications", "Real-time desktop & mobile alerts", n.channelPush) { onUpdateNotifications(n.copy(channelPush = it)) }
            DesktopRowDivider()
            DesktopToggleRow("Email notifications", "Security notices & missed highlights", n.channelEmail) { onUpdateNotifications(n.copy(channelEmail = it)) }
            DesktopRowDivider()
            DesktopToggleRow("SMS notifications", "Critical sign-in security alerts", n.channelSms) { onUpdateNotifications(n.copy(channelSms = it)) }
          }
        }
        item {
          DesktopSectionCard("Alert categories") {
            DesktopToggleRow("Messages", "Direct messages and mentions", n.typeMessages) { onUpdateNotifications(n.copy(typeMessages = it)) }
            DesktopRowDivider()
            DesktopToggleRow("Comments", "Replies and comments on your posts", n.typeComments) { onUpdateNotifications(n.copy(typeComments = it)) }
            DesktopRowDivider()
            DesktopToggleRow("New followers", "When someone follows or requests you", n.typeFollowers) { onUpdateNotifications(n.copy(typeFollowers = it)) }
            DesktopRowDivider()
            DesktopToggleRow("Product updates", "New releases and safety announcements", n.typeProductUpdates) { onUpdateNotifications(n.copy(typeProductUpdates = it)) }
          }
        }
        item {
          DesktopSectionCard("Quiet hours") {
            DesktopToggleRow("Enable quiet hours", "Mute non-urgent push notifications overnight", n.quietHoursEnabled) {
              onUpdateNotifications(n.copy(quietHoursEnabled = it))
            }
            if (n.quietHoursEnabled) {
              DesktopRowDivider()
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column {
                  Text("Start time", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text(n.formatTime(n.quietStartMinutes), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onUpdateNotifications(n.copy(quietStartMinutes = (n.quietStartMinutes + 1440 - 60) % 1440)) }) { Text("-1h") }
                    OutlinedButton(onClick = { onUpdateNotifications(n.copy(quietStartMinutes = (n.quietStartMinutes + 60) % 1440)) }) { Text("+1h") }
                  }
                }
                Column(horizontalAlignment = Alignment.End) {
                  Text("End time", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text(n.formatTime(n.quietEndMinutes), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onUpdateNotifications(n.copy(quietEndMinutes = (n.quietEndMinutes + 1440 - 60) % 1440)) }) { Text("-1h") }
                    OutlinedButton(onClick = { onUpdateNotifications(n.copy(quietEndMinutes = (n.quietEndMinutes + 60) % 1440)) }) { Text("+1h") }
                  }
                }
              }
            }
          }
        }
      }
    }

    DesktopSettingsDestination.CONTENT_PREFERENCES -> {
      val cp = bundle.contentPreferences
      var topicDraft by remember { mutableStateOf("") }
      LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        item {
          DesktopSectionCard("Suggested content") {
            DesktopToggleRow("Personalized suggestions", "Tailor For You using your interests", cp.suggestedContentEnabled) {
              onUpdateContentPrefs(cp.copy(suggestedContentEnabled = it), false)
            }
            DesktopRowDivider()
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Column(modifier = Modifier.weight(1f)) {
                Text("Reset For You feed", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("Start fresh with neutral recommendations immediately", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              OutlinedButton(
                onClick = { onUpdateContentPrefs(cp.copy(notInterestedTopics = emptyList()), true) },
                border = BorderStroke(1.dp, DesktopCrimson),
              ) {
                Text("Reset", color = DesktopCrimson, fontWeight = FontWeight.SemiBold)
              }
            }
          }
        }
        item {
          DesktopSectionCard("Not interested topics") {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  color = MaterialTheme.colorScheme.background,
                  shape = RoundedCornerShape(12.dp),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                  modifier = Modifier.weight(1f).height(44.dp),
                ) {
                  Box(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                    if (topicDraft.isEmpty()) Text("Hide topic (e.g. sports)", fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    BasicTextField(
                      value = topicDraft,
                      onValueChange = { topicDraft = it.take(36) },
                      singleLine = true,
                      textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                      cursorBrush = SolidColor(DesktopCrimson),
                      modifier = Modifier.fillMaxWidth(),
                    )
                  }
                }
                Button(
                  onClick = {
                    val clean = topicDraft.trim().lowercase().removePrefix("#")
                    if (clean.isNotEmpty() && !cp.notInterestedTopics.contains(clean)) {
                      onUpdateContentPrefs(cp.copy(notInterestedTopics = cp.notInterestedTopics + clean), false)
                      topicDraft = ""
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = DesktopCrimson),
                ) { Text("Hide", color = Color.White) }
              }

              if (cp.notInterestedTopics.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  cp.notInterestedTopics.forEach { topic ->
                    FilterChip(
                      selected = true,
                      onClick = { onUpdateContentPrefs(cp.copy(notInterestedTopics = cp.notInterestedTopics - topic), false) },
                      label = { Text("#$topic ×") },
                      colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DesktopCrimson.copy(alpha = 0.14f), selectedLabelColor = DesktopCrimson),
                    )
                  }
                }
              }

              FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DesktopSettingsCatalog.quickTopicTags.filterNot { cp.notInterestedTopics.contains(it) }.take(8).forEach { tag ->
                  FilterChip(
                    selected = false,
                    onClick = { onUpdateContentPrefs(cp.copy(notInterestedTopics = cp.notInterestedTopics + tag), false) },
                    label = { Text("+ $tag") },
                  )
                }
              }
            }
          }
        }
        item {
          DesktopSectionCard("Playback, data saver & language") {
            DesktopToggleRow("Autoplay videos", "Automatically start playing feed videos", cp.autoplayVideos) {
              onUpdateContentPrefs(cp.copy(autoplayVideos = it), false)
            }
            DesktopRowDivider()
            DesktopOptionChipsRow("Video quality", DesktopVideoQualityOption.entries.map { it.label }, cp.videoQuality.ordinal) { idx ->
              onUpdateContentPrefs(cp.copy(videoQuality = DesktopVideoQualityOption.entries[idx]), false)
            }
            DesktopRowDivider()
            DesktopToggleRow("Data saver", "Prefer lower-bitrate streams & lighter preload", cp.dataSaver) {
              onUpdateContentPrefs(cp.copy(dataSaver = it), false)
            }
            DesktopRowDivider()
            DesktopOptionChipsRow("App language", DesktopAppLanguageOption.entries.map { it.label }, cp.language.ordinal) { idx ->
              onUpdateContentPrefs(cp.copy(language = DesktopAppLanguageOption.entries[idx]), false)
            }
          }
        }
        item {
          DesktopSectionCard("Accessibility") {
            DesktopToggleRow("Show captions", "Display video captions on screen automatically", cp.captionsEnabled) {
              onUpdateContentPrefs(cp.copy(captionsEnabled = it), false)
            }
            DesktopRowDivider()
            DesktopToggleRow("Reduced motion", "Minimize animations across feeds and transitions", cp.reducedMotion) {
              onUpdateContentPrefs(cp.copy(reducedMotion = it), false)
            }
          }
        }
      }
    }

    DesktopSettingsDestination.SAVED_LINKS -> {
      val smartController = LocalDesktopSmartActions.current
      if (smartController != null) {
        DesktopSavedLinksPane(controller = smartController, modifier = Modifier.fillMaxSize())
      } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text("No saved links yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }

    DesktopSettingsDestination.OFFLINE_SYNC -> {
      val sp = bundle.syncPreferences
      val lastSyncedText = remember(sp.lastSyncedAtMs) {
        if (sp.lastSyncedAtMs <= 0L) "Not synced yet"
        else SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(sp.lastSyncedAtMs))
      }
      LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        item {
          Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp), shadowElevation = 1.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                  Text("Server synchronization", fontSize = 16.5.sp, fontWeight = FontWeight.Bold)
                  Text(
                    text = if (pendingSyncCount == 0) "All changes synced · Last synced: $lastSyncedText"
                    else "$pendingSyncCount queued offline change(s) waiting to upload",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Surface(
                  color = if (pendingSyncCount == 0) Color(0xFF0E7A45).copy(alpha = 0.14f) else DesktopCrimson.copy(alpha = 0.14f),
                  shape = RoundedCornerShape(12.dp),
                ) {
                  Text(
                    text = if (pendingSyncCount == 0) sp.lastSyncStatus.uppercase() else "$pendingSyncCount PENDING",
                    color = if (pendingSyncCount == 0) Color(0xFF0E7A45) else DesktopCrimson,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  )
                }
              }

              if (sp.totalSyncedOps > 0L) {
                Text(
                  text = "Total operations synced with server: ${sp.totalSyncedOps}",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              if (syncFeedback.isNotBlank()) {
                Text(syncFeedback, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = DesktopCrimson)
              }

              Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                  enabled = !isSyncingNow,
                  onClick = onTriggerSyncNow,
                  shape = RoundedCornerShape(14.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = DesktopCrimson),
                  modifier = Modifier.weight(1f).height(46.dp),
                ) {
                  Text(if (isSyncingNow) "Syncing..." else "Sync now", color = Color.White, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                  enabled = !isSyncingNow,
                  onClick = onRefreshSyncStatus,
                  shape = RoundedCornerShape(14.dp),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                  modifier = Modifier.height(46.dp),
                ) {
                  Text("Refresh")
                }
              }
            }
          }
        }
        item {
          DesktopSectionCard("Sync automation") {
            DesktopToggleRow("Auto-sync when online", "Automatically push offline changes as soon as connectivity returns.", sp.autoSyncEnabled) {
              onUpdateSyncPrefs(sp.copy(autoSyncEnabled = it))
            }
            DesktopRowDivider()
            DesktopToggleRow("Sync over Wi-Fi only", "Defer heavy queue uploads on metered connections.", sp.syncOverWifiOnly) {
              onUpdateSyncPrefs(sp.copy(syncOverWifiOnly = it))
            }
            DesktopRowDivider()
            DesktopToggleRow("Background sync", "Keep settings, privacy rules, and chat themes aligned silently.", sp.backgroundSyncEnabled) {
              onUpdateSyncPrefs(sp.copy(backgroundSyncEnabled = it))
            }
          }
        }
        item {
          DesktopSectionCard("Offline cache") {
            DesktopToggleRow("Cache feeds offline", "Store recent feeds locally so browsing keeps working without internet.", sp.cacheFeedsOffline) {
              onUpdateSyncPrefs(sp.copy(cacheFeedsOffline = it))
            }
            DesktopRowDivider()
            DesktopToggleRow("Cache chat media offline", "Keep recent photos and audio cached for instant offline playback.", sp.cacheChatMediaOffline) {
              onUpdateSyncPrefs(sp.copy(cacheChatMediaOffline = it))
            }
          }
        }
      }
    }

    DesktopSettingsDestination.BLOCKED_ACCOUNTS -> {
      val list = if (restrictedTab == 0) blockedList else mutedList
      Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          FilterChip(
            selected = restrictedTab == 0,
            onClick = { onRestrictedTabChanged(0) },
            label = { Text("Blocked accounts (${blockedList.size})") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DesktopCrimson, selectedLabelColor = Color.White),
          )
          FilterChip(
            selected = restrictedTab == 1,
            onClick = { onRestrictedTabChanged(1) },
            label = { Text("Muted accounts (${mutedList.size})") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DesktopCrimson, selectedLabelColor = Color.White),
          )
        }
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth().height(44.dp),
        ) {
          Row(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            DesktopSvgIcon(DesktopIconKind.SEARCH, MaterialTheme.colorScheme.onSurfaceVariant, 16.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
              if (restrictedQuery.isEmpty()) Text("Search by name or @username", fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              BasicTextField(
                value = restrictedQuery,
                onValueChange = { onRestrictedQueryChanged(it.take(64)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(DesktopCrimson),
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }
        }
        if (restrictedLoading && list.isEmpty()) {
          LinearProgressIndicator(color = DesktopCrimson, modifier = Modifier.fillMaxWidth())
        }
        if (list.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
              text = if (restrictedTab == 0) "No blocked accounts" else "No muted accounts",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else {
          LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list, key = { it.userId }) { acc ->
              Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp), shadowElevation = 1.dp) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                  DesktopSettingsAvatar(apiClient, acc.avatarUrl, acc.fullName.ifBlank { acc.username }, 44.dp)
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(acc.fullName.ifBlank { acc.username }, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                    Text("@${acc.username}", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                  OutlinedButton(
                    onClick = { if (restrictedTab == 0) onUnblockUser(acc.userId) else onUnmuteUser(acc.userId) },
                    border = BorderStroke(1.dp, DesktopCrimson),
                    shape = RoundedCornerShape(10.dp),
                  ) {
                    Text(if (restrictedTab == 0) "Unblock" else "Unmute", color = DesktopCrimson, fontWeight = FontWeight.SemiBold)
                  }
                }
              }
            }
          }
        }
      }
    }

    DesktopSettingsDestination.REPORT_PROBLEM -> {
      var selectedCat by remember { mutableStateOf(DesktopSettingsCatalog.reportCategories.first()) }
      var description by remember { mutableStateOf("") }
      var screenshotBytes by remember { mutableStateOf<ByteArray?>(null) }
      var screenshotName by remember { mutableStateOf("") }

      LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        if (lastTicket != null) {
          item {
            Surface(color = Color(0xFF0E7A45).copy(alpha = 0.12f), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Color(0xFF0E7A45).copy(alpha = 0.35f))) {
              Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Report received", fontWeight = FontWeight.Bold, color = Color(0xFF0E7A45))
                Text("Reference: ${lastTicket.referenceNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(lastTicket.resolutionNote, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                  TextButton(onClick = { onOpenDestination(DesktopSettingsDestination.HELP_SUPPORT) }) { Text("View ticket history", color = DesktopCrimson) }
                  TextButton(onClick = onClearLastTicket) { Text("Dismiss") }
                }
              }
            }
          }
        }
        item {
          DesktopSectionCard("Category") {
            FlowRow(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              DesktopSettingsCatalog.reportCategories.forEach { cat ->
                FilterChip(
                  selected = selectedCat == cat,
                  onClick = { selectedCat = cat },
                  label = { Text(cat) },
                  colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DesktopCrimson, selectedLabelColor = Color.White),
                )
              }
            }
          }
        }
        item {
          DesktopSectionCard("Details & optional screenshot") {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Surface(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().height(120.dp),
              ) {
                Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                  if (description.isEmpty()) Text("Describe steps to reproduce and expected behavior", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.5.sp)
                  BasicTextField(
                    value = description,
                    onValueChange = { description = it.take(1500) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(DesktopCrimson),
                    modifier = Modifier.fillMaxSize(),
                  )
                }
              }
              OutlinedButton(
                onClick = {
                  val dlg = FileDialog(null as Frame?, "Attach screenshot", FileDialog.LOAD).apply {
                    setFilenameFilter { _, name ->
                      val low = name.lowercase()
                      low.endsWith(".png") || low.endsWith(".jpg") || low.endsWith(".jpeg") || low.endsWith(".webp")
                    }
                    isVisible = true
                  }
                  val picked = dlg.files?.firstOrNull()
                  if (picked != null && picked.exists()) {
                    screenshotBytes = runCatching { picked.readBytes() }.getOrNull()
                    screenshotName = picked.name
                  }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Text(if (screenshotName.isBlank()) "Attach screenshot (optional)" else "Attached: $screenshotName")
              }
              Button(
                enabled = description.trim().length >= 5,
                onClick = {
                  onSubmitProblemReport(selectedCat, description.trim(), screenshotBytes)
                  description = ""
                  screenshotBytes = null
                  screenshotName = ""
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DesktopCrimson),
                modifier = Modifier.fillMaxWidth().height(48.dp),
              ) {
                Text("Submit report", color = Color.White, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    DesktopSettingsDestination.SAFETY_CENTER -> {
      LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        items(DesktopSettingsCatalog.safetySections, key = { it.id }) { sec ->
          Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp), shadowElevation = 1.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(sec.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DesktopCrimson)
              Text(sec.summary, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
              sec.bulletPoints.forEach { bp ->
                Text("• $bp", fontSize = 13.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
        item {
          Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp), shadowElevation = 1.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text("Emergency contacts & helplines", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DesktopCrimson)
              DesktopSettingsCatalog.emergencyContacts.forEachIndexed { idx, ec ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text("${ec.region} · ${ec.serviceName}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                  Text(ec.contact, fontWeight = FontWeight.Bold, color = DesktopCrimson, fontSize = 14.5.sp)
                  Text(ec.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (idx < DesktopSettingsCatalog.emergencyContacts.lastIndex) DesktopRowDivider()
              }
            }
          }
        }
      }
    }

    DesktopSettingsDestination.HELP_SUPPORT -> {
      LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        item {
          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().height(44.dp),
          ) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
              DesktopSvgIcon(DesktopIconKind.SEARCH, MaterialTheme.colorScheme.onSurfaceVariant, 16.dp)
              Spacer(modifier = Modifier.width(8.dp))
              Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (faqQuery.isEmpty()) Text("Search FAQs", fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                BasicTextField(
                  value = faqQuery,
                  onValueChange = { onFaqQueryChanged(it.take(64)) },
                  singleLine = true,
                  textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                  cursorBrush = SolidColor(DesktopCrimson),
                  modifier = Modifier.fillMaxWidth(),
                )
              }
            }
          }
        }
        items(faqs, key = { it.id }) { faq ->
          var expanded by remember(faq.id) { mutableStateOf(false) }
          Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp), shadowElevation = 1.dp) {
            Column(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(faq.category, color = DesktopCrimson, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
              Text(faq.question, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
              if (expanded) Text(faq.answer, fontSize = 13.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
        item {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Ticket history (${tickets.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            TextButton(onClick = onRefreshTickets) { Text("Refresh", color = DesktopCrimson) }
          }
        }
        if (tickets.isEmpty()) {
          item { Text("No support tickets submitted yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
        } else {
          items(tickets, key = { it.ticketId }) { t ->
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp), shadowElevation = 1.dp) {
              Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                  Text(t.referenceNumber, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                  Text(t.status.uppercase(), color = DesktopCrimson, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
                Text(t.category, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text(t.description, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
              }
            }
          }
        }
        item {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("System status", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            TextButton(onClick = onRefreshStatus) { Text("Recheck", color = DesktopCrimson) }
          }
        }
        items(statusList, key = { it.serviceName }) { svc ->
          Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(14.dp), shadowElevation = 1.dp) {
            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Column(modifier = Modifier.weight(1f)) {
                Text(svc.serviceName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(svc.summary, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Text(
                text = if (svc.latencyMs > 0) "${svc.status.uppercase()} (${svc.latencyMs}ms)" else svc.status.uppercase(),
                color = if (svc.status.equals("operational", true)) Color(0xFF0E7A45) else DesktopCrimson,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
              )
            }
          }
        }
      }
    }

    DesktopSettingsDestination.TERMS_POLICIES -> {
      val selectedDoc = legalDocs.firstOrNull { it.id == selectedLegalDocId }
      if (selectedDoc != null) {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
          item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
              TextButton(onClick = { onSelectLegalDoc(null) }) { Text("All documents", color = DesktopCrimson) }
              Spacer(modifier = Modifier.weight(1f))
              Text("Last updated: ${selectedDoc.lastUpdated}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          item { Text(selectedDoc.title, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
          items(selectedDoc.sections) { sec ->
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp), shadowElevation = 1.dp) {
              Text(sec, fontSize = 13.5.sp, lineHeight = 20.sp, modifier = Modifier.fillMaxWidth().padding(16.dp))
            }
          }
        }
      } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
          items(legalDocs, key = { it.id }) { doc ->
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp), shadowElevation = 1.dp) {
              Row(
                modifier = Modifier.fillMaxWidth().clickable { onSelectLegalDoc(doc.id) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(doc.title, fontWeight = FontWeight.SemiBold, fontSize = 15.5.sp)
                  Text("Last updated: ${doc.lastUpdated}", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DesktopSvgIcon(DesktopIconKind.CHEVRON_RIGHT, DesktopCrimson, 16.dp)
              }
            }
          }
        }
      }
    }

    DesktopSettingsDestination.ABOUT -> {
      LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        item {
          DesktopSectionCard("Application build") {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text("Telefam Desktop", fontSize = 18.sp, fontWeight = FontWeight.Bold)
              Text("Version ${DesktopSettingsCatalog.APP_VERSION} (Build ${DesktopSettingsCatalog.APP_BUILD})", fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Button(
                onClick = onCheckUpdates,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DesktopCrimson),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
              ) { Text("Check for updates", color = Color.White, fontWeight = FontWeight.Bold) }
              if (versionCheck != null) {
                Text(versionCheck.releaseNotes, fontSize = 13.sp, color = DesktopCrimson, modifier = Modifier.padding(top = 6.dp))
              }
            }
          }
        }
        item {
          DesktopSectionCard("Copyright & company info") {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("Telefam Technologies Ltd.", fontWeight = FontWeight.SemiBold)
              Text("Trust, Family Messaging & Video Publishing Platform", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("© 2026 Telefam Technologies. All rights reserved.", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
        item {
          DesktopSectionCard("Open source licenses") {
            DesktopSettingsCatalog.openSourceLicenses.forEachIndexed { idx, lic ->
              Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(lic.library, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("${lic.license} · ${lic.copyright}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              if (idx < DesktopSettingsCatalog.openSourceLicenses.lastIndex) DesktopRowDivider()
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DesktopSectionCard(title: String, content: @Composable () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp), shadowElevation = 1.dp) {
      Column(modifier = Modifier.fillMaxWidth()) { content() }
    }
  }
}

@Composable
private fun DesktopRowDivider() {
  HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
}

@Composable
private fun DesktopToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
      Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(modifier = Modifier.width(12.dp))
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = DesktopCrimson),
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DesktopOptionChipsRow(title: String, labels: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit) {
  Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      labels.forEachIndexed { idx, label ->
        val sel = idx == selectedIndex
        FilterChip(
          selected = sel,
          onClick = { onSelected(idx) },
          label = { Text(label) },
          colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DesktopCrimson, selectedLabelColor = Color.White),
        )
      }
    }
  }
}

@Composable
private fun DesktopSettingsAvatar(apiClient: DesktopApiClient, avatarUrl: String, displayName: String, size: Dp) {
  val initials = TelefamContactsRules.initialsFor(displayName, displayName)
  Box(
    modifier = Modifier.size(size).clip(CircleShape).background(DesktopCrimson.copy(alpha = 0.14f)),
    contentAlignment = Alignment.Center,
  ) {
    Text(initials, color = DesktopCrimson, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    if (avatarUrl.isNotBlank()) {
      DesktopRemoteCachedImage(apiClient = apiClient, pathOrUrl = avatarUrl, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) {}
    }
  }
}

@Composable
private fun DesktopRedCircleSvgBadge(kind: DesktopSettingsSvgKind, size: Dp) {
  Box(modifier = Modifier.size(size).clip(CircleShape).background(DesktopCrimson), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.size(size * 0.50f)) {
      val w = this.size.width
      val h = this.size.height
      val sw = 2.1.dp.toPx()
      val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
      val white = Color.White
      when (kind) {
        DesktopSettingsSvgKind.ACTIVITY_PULSE -> {
          val p = Path().apply {
            moveTo(w * 0.08f, h * 0.54f)
            lineTo(w * 0.28f, h * 0.54f)
            lineTo(w * 0.42f, h * 0.22f)
            lineTo(w * 0.58f, h * 0.80f)
            lineTo(w * 0.72f, h * 0.48f)
            lineTo(w * 0.92f, h * 0.48f)
          }
          drawPath(p, white, style = stroke)
        }
        DesktopSettingsSvgKind.SHARE_CARD -> {
          drawRoundRect(white, Offset(w * 0.10f, h * 0.22f), Size(w * 0.80f, h * 0.56f), CornerRadius(w * 0.12f, w * 0.12f), style = stroke)
          drawLine(white, Offset(w * 0.10f, h * 0.44f), Offset(w * 0.90f, h * 0.44f), sw, StrokeCap.Round)
        }
        DesktopSettingsSvgKind.BELL -> {
          val p = Path().apply {
            moveTo(w * 0.26f, h * 0.44f)
            cubicTo(w * 0.26f, h * 0.16f, w * 0.74f, h * 0.16f, w * 0.74f, h * 0.44f)
            lineTo(w * 0.80f, h * 0.68f)
            lineTo(w * 0.20f, h * 0.68f)
            close()
          }
          drawPath(p, white, style = stroke)
          drawLine(white, Offset(w * 0.42f, h * 0.78f), Offset(w * 0.58f, h * 0.78f), sw, StrokeCap.Round)
        }
        DesktopSettingsSvgKind.HEART -> {
          val p = Path().apply {
            moveTo(w * 0.50f, h * 0.82f)
            cubicTo(w * 0.14f, h * 0.56f, w * 0.12f, h * 0.24f, w * 0.34f, h * 0.22f)
            cubicTo(w * 0.44f, h * 0.21f, w * 0.50f, h * 0.30f, w * 0.50f, h * 0.35f)
            cubicTo(w * 0.50f, h * 0.30f, w * 0.56f, h * 0.21f, w * 0.66f, h * 0.22f)
            cubicTo(w * 0.88f, h * 0.24f, w * 0.86f, h * 0.56f, w * 0.50f, h * 0.82f)
            close()
          }
          drawPath(p, white, style = stroke)
        }
        DesktopSettingsSvgKind.LOCK -> {
          drawRoundRect(white, Offset(w * 0.22f, h * 0.45f), Size(w * 0.56f, h * 0.42f), CornerRadius(w * 0.10f, w * 0.10f), style = stroke)
          val p = Path().apply {
            moveTo(w * 0.34f, h * 0.45f)
            lineTo(w * 0.34f, h * 0.30f)
            cubicTo(w * 0.34f, h * 0.14f, w * 0.66f, h * 0.14f, w * 0.66f, h * 0.30f)
            lineTo(w * 0.66f, h * 0.45f)
          }
          drawPath(p, white, style = stroke)
        }
        DesktopSettingsSvgKind.BLOCK_MINUS -> {
          drawCircle(white, radius = w * 0.38f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
          drawLine(white, Offset(w * 0.32f, h * 0.5f), Offset(w * 0.68f, h * 0.5f), sw, StrokeCap.Round)
        }
        DesktopSettingsSvgKind.FLAG -> {
          drawLine(white, Offset(w * 0.26f, h * 0.16f), Offset(w * 0.26f, h * 0.86f), sw, StrokeCap.Round)
          val p = Path().apply {
            moveTo(w * 0.26f, h * 0.20f)
            lineTo(w * 0.78f, h * 0.24f)
            lineTo(w * 0.72f, h * 0.54f)
            lineTo(w * 0.26f, h * 0.50f)
          }
          drawPath(p, white, style = stroke)
        }
        DesktopSettingsSvgKind.SHIELD_CHECK -> {
          val p = Path().apply {
            moveTo(w * 0.50f, h * 0.12f)
            lineTo(w * 0.84f, h * 0.26f)
            lineTo(w * 0.82f, h * 0.54f)
            cubicTo(w * 0.80f, h * 0.72f, w * 0.66f, h * 0.84f, w * 0.50f, h * 0.90f)
            cubicTo(w * 0.34f, h * 0.84f, w * 0.20f, h * 0.72f, w * 0.18f, h * 0.54f)
            lineTo(w * 0.16f, h * 0.26f)
            close()
          }
          drawPath(p, white, style = stroke)
          val chk = Path().apply {
            moveTo(w * 0.36f, h * 0.52f)
            lineTo(w * 0.46f, h * 0.62f)
            lineTo(w * 0.66f, h * 0.40f)
          }
          drawPath(chk, white, style = stroke)
        }
        DesktopSettingsSvgKind.HELP_QUESTION -> {
          drawCircle(white, radius = w * 0.38f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
          val qPath = Path().apply {
            moveTo(w * 0.39f, h * 0.38f)
            cubicTo(w * 0.39f, h * 0.27f, w * 0.61f, h * 0.27f, w * 0.61f, h * 0.39f)
            cubicTo(w * 0.61f, h * 0.48f, w * 0.50f, h * 0.50f, w * 0.50f, h * 0.58f)
          }
          drawPath(qPath, white, style = stroke)
          drawCircle(white, radius = w * 0.045f, center = Offset(w * 0.50f, h * 0.70f))
        }
        DesktopSettingsSvgKind.DOCUMENT_LINES -> {
          drawRoundRect(white, Offset(w * 0.22f, h * 0.14f), Size(w * 0.56f, h * 0.72f), CornerRadius(w * 0.10f, w * 0.10f), style = stroke)
          drawLine(white, Offset(w * 0.35f, h * 0.36f), Offset(w * 0.65f, h * 0.36f), sw, StrokeCap.Round)
          drawLine(white, Offset(w * 0.35f, h * 0.50f), Offset(w * 0.65f, h * 0.50f), sw, StrokeCap.Round)
          drawLine(white, Offset(w * 0.35f, h * 0.64f), Offset(w * 0.55f, h * 0.64f), sw, StrokeCap.Round)
        }
        DesktopSettingsSvgKind.INFO_CIRCLE -> {
          drawCircle(white, radius = w * 0.38f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
          drawCircle(white, radius = w * 0.045f, center = Offset(w * 0.50f, h * 0.32f))
          drawLine(white, Offset(w * 0.50f, h * 0.46f), Offset(w * 0.50f, h * 0.70f), sw, StrokeCap.Round)
        }
        DesktopSettingsSvgKind.CLOUD_SYNC -> {
          val cloud = Path().apply {
            moveTo(w * 0.28f, h * 0.68f)
            lineTo(w * 0.74f, h * 0.68f)
            cubicTo(w * 0.88f, h * 0.68f, w * 0.88f, h * 0.46f, w * 0.73f, h * 0.45f)
            cubicTo(w * 0.71f, h * 0.26f, w * 0.43f, h * 0.24f, w * 0.37f, h * 0.42f)
            cubicTo(w * 0.18f, h * 0.42f, w * 0.14f, h * 0.68f, w * 0.28f, h * 0.68f)
            close()
          }
          drawPath(cloud, white, style = stroke)
          drawLine(white, Offset(w * 0.50f, h * 0.46f), Offset(w * 0.50f, h * 0.62f), sw, StrokeCap.Round)
          drawLine(white, Offset(w * 0.43f, h * 0.53f), Offset(w * 0.50f, h * 0.45f), sw, StrokeCap.Round)
          drawLine(white, Offset(w * 0.57f, h * 0.53f), Offset(w * 0.50f, h * 0.45f), sw, StrokeCap.Round)
        }
        DesktopSettingsSvgKind.BOOKMARK_LINK -> {
          val p = Path().apply {
            moveTo(w * 0.25f, h * 0.14f)
            lineTo(w * 0.75f, h * 0.14f)
            lineTo(w * 0.75f, h * 0.86f)
            lineTo(w * 0.50f, h * 0.65f)
            lineTo(w * 0.25f, h * 0.86f)
            close()
          }
          drawPath(p, white, style = stroke)
        }
      }
    }
  }
}
