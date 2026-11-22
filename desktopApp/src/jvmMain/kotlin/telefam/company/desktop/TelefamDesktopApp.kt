package telefam.company.desktop

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import telefam.company.shared.ContactFilterTab
import telefam.company.shared.ContactRelationshipState
import telefam.company.shared.SharedFeedAction
import telefam.company.shared.SharedFeedMath
import telefam.company.shared.SharedFeedTab
import telefam.company.shared.SharedHashtagItem
import telefam.company.shared.SharedOwnerProfileTab
import telefam.company.shared.SharedPostFont
import telefam.company.shared.SharedPostMode
import telefam.company.shared.SharedPostPrivacy
import telefam.company.shared.SharedPostRules
import telefam.company.shared.SharedPostTheme
import telefam.company.shared.SharedPostThemeKind
import telefam.company.shared.SharedSongTrack
import telefam.company.shared.TelefamContactsRules
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import java.security.MessageDigest
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private val DesktopCrimson = Color(0xFFC80A0F)
private val DesktopBlush = Color(0xFFFCE8E8)
private val DesktopDarkBlush = Color(0xFF3A1416)
private val DesktopDarkBg = Color(0xFF0E1014)
private val DesktopDarkSurface = Color(0xFF171A21)
private val DesktopDarkSearch = Color(0xFF111824)
private val DesktopDarkBorder = Color(0xFF2D323F)

enum class DesktopThemeMode { SYSTEM, LIGHT, DARK }

data class DesktopContactItem(
  val userId: String,
  val fullName: String,
  val username: String,
  val countryDisplay: String,
  val isFollowing: Boolean,
  val followsYou: Boolean,
  val riskScore: Double,
) {
  val relationship: ContactRelationshipState
    get() = ContactRelationshipState.compute(isFollowing, followsYou)
}

data class DesktopSelectedPhoto(
  val id: String,
  val fileName: String,
  val sourceFile: File,
  val previewBitmap: ImageBitmap?,
  val byteSize: Long,
)

data class DesktopSelectedVideo(
  val id: String,
  val fileName: String,
  val sourceFile: File,
  val byteSize: Long,
  val durationMs: Long,
  val trimStartMs: Long,
  val trimEndMs: Long,
  val selectedThumbIndex: Int,
  val frameHashes: List<String>,
  val contentId: String,
)

data class DesktopPendingContactOp(
  val targetUserId: String,
  val wantFollow: Boolean,
)

data class DesktopFeedPost(
  val postId: String,
  val authorUserId: String,
  val authorUsername: String,
  val authorFullName: String,
  val authorAvatarUrl: String = "",
  val isFollowingAuthor: Boolean,
  val isOwner: Boolean,
  val privacy: String = "public",
  val mode: SharedPostMode,
  val textContent: String,
  val caption: String,
  val themeId: String,
  val fontOption: SharedPostFont,
  val song: SharedSongTrack?,
  val hashtags: List<String>,
  val photoUrls: List<String> = emptyList(),
  val videoUrl: String = "",
  val thumbnailUrl: String = "",
  val mediaWidth: Int = 1080,
  val mediaHeight: Int = 1920,
  val likesCount: Long = 0L,
  val commentsCount: Long = 0L,
  val sharesCount: Long = 0L,
  val favouritesCount: Long = 0L,
  val repostsCount: Long = 0L,
  val viewsCount: Long = 0L,
  val isLiked: Boolean = false,
  val isFavourited: Boolean = false,
  val isReposted: Boolean = false,
  val repostedByUsername: String = "",
  val recommendationLabel: String = "",
  val muteOriginalAudio: Boolean = false,
  val createdAtMs: Long = 0L,
)

private sealed interface DesktopPostLaunchTarget {
  data object CreateNew : DesktopPostLaunchTarget
  data class PrefillHashtag(val tag: String, val mode: SharedPostMode) : DesktopPostLaunchTarget
  data class PrefillSong(val song: SharedSongTrack, val mode: SharedPostMode) : DesktopPostLaunchTarget
  data class EditPost(val item: DesktopFeedPost) : DesktopPostLaunchTarget
}

private data class DesktopProfileViewerState(
  val items: List<DesktopFeedPost>,
  val initialIndex: Int,
)

fun main() = application {
  val vault = remember { DesktopEncryptedSessionVault() }
  var session by remember { mutableStateOf(vault.load()) }
  val apiClient = remember {
    DesktopApiClient(
      vault = vault,
      initialSession = session,
      onSessionUpdated = { updated -> session = updated },
    )
  }

  val scope = rememberCoroutineScope()
  var splashVisible by remember { mutableStateOf(true) }
  var activeTab by remember { mutableStateOf("contacts") }
  var postLaunchTarget by remember { mutableStateOf<DesktopPostLaunchTarget?>(null) }
  var showFeedSearch by remember { mutableStateOf(false) }
  var activeFeedTab by remember { mutableStateOf(SharedFeedTab.FOR_YOU) }
  var feedSearchQuery by remember { mutableStateOf("") }
  var feedActiveShortcut by remember { mutableStateOf("") }
  var feedSearchShortcuts by remember { mutableStateOf<List<DesktopSearchShortcut>>(emptyList()) }
  var feedSearchResults by remember { mutableStateOf<List<DesktopFeedPost>>(emptyList()) }

  var filter by remember { mutableStateOf(ContactFilterTab.ALL) }
  var searchQuery by remember { mutableStateOf("") }
  var contactsPage by remember { mutableStateOf(1) }
  var contactsHasMore by remember { mutableStateOf(false) }
  var feedPage by remember { mutableStateOf(1) }
  var feedHasMore by remember { mutableStateOf(false) }

  val contacts = remember { mutableStateListOf<DesktopContactItem>() }
  val pendingContactOps = remember { mutableStateListOf<DesktopPendingContactOp>() }
  val feedPosts = remember { mutableStateListOf<DesktopFeedPost>() }
  val knownHashtags = remember { mutableStateListOf<SharedHashtagItem>() }
  val recentActionEpochsMs = remember { mutableStateListOf<Long>() }
  var toastMessage by remember { mutableStateOf<String?>(null) }
  var showChatsRail by remember { mutableStateOf(true) }
  val profileBackStack = remember { mutableStateListOf<String>() }
  var profileViewerState by remember { mutableStateOf<DesktopProfileViewerState?>(null) }
  var showSettingsScreen by remember { mutableStateOf(false) }
  val chatPrivacyStack = remember { mutableStateListOf<DesktopChatPrivacyRoute>() }
  var settingsBundle by remember { mutableStateOf(apiClient.loadCachedSettingsBundle()) }
  var incomingLoginRequest by remember { mutableStateOf<DesktopIncomingLoginRequestItem?>(null) }
  var loginApprovedState by remember { mutableStateOf<DesktopLoginApprovedState?>(null) }
  var isRespondingToLoginReq by remember { mutableStateOf(false) }
  var incomingLoginError by remember { mutableStateOf<String?>(null) }
  val chatController = remember { DesktopChatController(apiClient) { settingsBundle.notifications.inAppSounds } }
  val callController = remember { DesktopCallController(apiClient, chatController) }
  val starsController = remember { DesktopStarsController(apiClient, chatController) }
  val starsState by starsController.state.collectAsState()
  val smartController = remember {
    DesktopSmartActionController(apiClient = apiClient, scope = scope) { toastMessage = it }
  }
  val chatConversations by chatController.conversations.collectAsState()
  val unreadChatCount = remember(chatConversations) {
    chatConversations.sumOf { it.unreadCount.coerceAtLeast(0) }
  }
  val desktopNotifications = remember { mutableStateListOf<DesktopNotificationItem>() }
  var unreadNotificationsCount by remember { mutableStateOf(0) }
  var showNotificationsModal by remember { mutableStateOf(false) }
  val incomingMsgBanner by chatController.incomingBanner.collectAsState()
  var bannerReplyDraft by remember(incomingMsgBanner?.messageId) { mutableStateOf("") }

  val openUserProfile: (String) -> Unit = { uid ->
    val clean = uid.trim()
    if (clean.isNotBlank()) {
      if (clean == "me" || (session.userId.isNotBlank() && clean == session.userId)) {
        profileBackStack.clear()
        profileViewerState = null
        showFeedSearch = false
        showSettingsScreen = false
        activeTab = "profile"
      } else if (profileBackStack.lastOrNull() != clean) {
        profileBackStack.add(clean)
      }
    }
  }

  LaunchedEffect(Unit) {
    delay(700L)
    splashVisible = false
  }

  LaunchedEffect(session.isAuthenticated, session.userId) {
    if (session.isAuthenticated) {
      chatController.attachSession(session.userId)
      callController.attachSession(session.userId)
      starsController.attachSession(session.userId)
      smartController.onAuthenticated()
      val fetched = withContext(Dispatchers.IO) { apiClient.getSettings() }
      settingsBundle = fetched
      withContext(Dispatchers.IO) {
        apiClient.captureAndSyncDeviceContext()
        apiClient.registerPushToken()
        apiClient.listNotifications()?.let { (items, unread) ->
          withContext(Dispatchers.Main) {
            desktopNotifications.clear()
            desktopNotifications.addAll(items)
            unreadNotificationsCount = unread
          }
        }
      }
      var notifTick = 0
      while (isActive && session.isAuthenticated) {
        val pendingRes = withContext(Dispatchers.IO) { apiClient.getPendingLoginRequests() }
        if (pendingRes is DesktopApiOutcome.Ok) {
          withContext(Dispatchers.IO) { apiClient.flushPendingDeviceContext() }
          val head = pendingRes.data.firstOrNull { it.remainingSeconds > 0L }
          if (head == null) {
            if (!isRespondingToLoginReq) incomingLoginRequest = null
          } else if (incomingLoginRequest?.requestId != head.requestId) {
            incomingLoginRequest = head
            incomingLoginError = null
          }
        }
        notifTick++
        if (notifTick % 2 == 0) {
          val fetchedNotifs = withContext(Dispatchers.IO) { apiClient.listNotifications() }
          if (fetchedNotifs != null) {
            desktopNotifications.clear()
            desktopNotifications.addAll(fetchedNotifs.first)
            unreadNotificationsCount = fetchedNotifs.second
          }
        }
        delay(4000L)
      }
    } else {
      callController.detachSession()
      chatController.detachSession()
      smartController.onLogout()
      activeTab = "contacts"
      searchQuery = ""
      contacts.clear()
      pendingContactOps.clear()
      feedPosts.clear()
      feedSearchResults = emptyList()
      feedSearchShortcuts = emptyList()
      showFeedSearch = false
      profileBackStack.clear()
      profileViewerState = null
      showSettingsScreen = false
      chatPrivacyStack.clear()
      postLaunchTarget = null
      settingsBundle = DesktopSettingsBundle()
      incomingLoginRequest = null
      loginApprovedState = null
      desktopNotifications.clear()
      unreadNotificationsCount = 0
      showNotificationsModal = false
    }
  }

  LaunchedEffect(incomingLoginRequest?.requestId) {
    val active = incomingLoginRequest ?: return@LaunchedEffect
    while (incomingLoginRequest?.requestId == active.requestId) {
      delay(1000L)
      val cur = incomingLoginRequest ?: break
      val next = cur.remainingSeconds - 1L
      if (next <= 0L) {
        incomingLoginRequest = null
        break
      }
      incomingLoginRequest = cur.copy(remainingSeconds = next)
    }
  }

  LaunchedEffect(toastMessage) {
    if (toastMessage != null) {
      delay(2200L)
      toastMessage = null
    }
  }

  // Load paginated contacts from Go backend whenever authenticated + filter/query changes
  LaunchedEffect(session.isAuthenticated, session.profileCompleted, filter, searchQuery) {
    if (!session.isAuthenticated || !session.profileCompleted) return@LaunchedEffect
    delay(180L)
    contactsPage = 1
    val res = withContext(Dispatchers.IO) { apiClient.listContacts(filter, searchQuery, 1) }
    if (res is DesktopApiOutcome.Ok) {
      contacts.clear()
      contacts.addAll(res.data.items)
      contactsHasMore = res.data.hasMore
    }
  }

  // Retry any queued offline follow/unfollow ops periodically
  LaunchedEffect(session.isAuthenticated, pendingContactOps.size) {
    if (!session.isAuthenticated || pendingContactOps.isEmpty()) return@LaunchedEffect
    while (pendingContactOps.isNotEmpty()) {
      val op = pendingContactOps.first()
      val outcome = withContext(Dispatchers.IO) { apiClient.setFollowContact(op.targetUserId, op.wantFollow) }
      if (outcome is DesktopApiOutcome.Ok) {
        pendingContactOps.removeAt(0)
      } else {
        delay(5000L)
      }
    }
  }

  // Load Feeds from Go backend whenever authenticated + tab changes
  LaunchedEffect(session.isAuthenticated, session.profileCompleted, activeTab, activeFeedTab) {
    if (!session.isAuthenticated || !session.profileCompleted || activeTab != "feeds") return@LaunchedEffect
    feedPage = 1
    val res = withContext(Dispatchers.IO) { apiClient.getFeeds(activeFeedTab, 1) }
    if (res is DesktopApiOutcome.Ok) {
      feedPosts.clear()
      feedPosts.addAll(res.data.items)
      feedHasMore = res.data.hasMore
    }
  }

  // Search Feeds from Go backend when search overlay is open
  LaunchedEffect(showFeedSearch, feedSearchQuery, feedActiveShortcut) {
    if (!showFeedSearch) return@LaunchedEffect
    delay(220L)
    val res = withContext(Dispatchers.IO) {
      apiClient.searchFeeds(feedSearchQuery, feedActiveShortcut, 1)
    }
    if (res is DesktopApiOutcome.Ok) {
      feedSearchShortcuts = res.data.shortcuts
      feedSearchResults = res.data.items
    } else {
      val q = feedSearchQuery.trim().lowercase()
      val sc = feedActiveShortcut.trim().lowercase()
      feedSearchResults = feedPosts.filter { post ->
        val matchQ = q.isEmpty() ||
          post.caption.lowercase().contains(q) ||
          post.textContent.lowercase().contains(q) ||
          post.authorUsername.lowercase().contains(q) ||
          post.hashtags.any { it.lowercase().contains(q) } ||
          (post.song?.title?.lowercase()?.contains(q) == true)
        val matchSc = sc.isEmpty() || post.hashtags.any { it.lowercase() == sc }
        matchQ && matchSc
      }
    }
  }

  val systemDark = isSystemInDarkTheme()
  val isDark = when (session.themeMode) {
    DesktopThemeMode.SYSTEM -> systemDark
    DesktopThemeMode.LIGHT -> false
    DesktopThemeMode.DARK -> true
  }
  val scheme = if (isDark) {
    darkColorScheme(
      primary = DesktopCrimson,
      background = DesktopDarkBg,
      surface = DesktopDarkSurface,
      onBackground = Color(0xFFF3F4F6),
      onSurface = Color(0xFFF3F4F6),
      onSurfaceVariant = Color(0xFF9CA3AF),
      outline = DesktopDarkBorder,
      outlineVariant = DesktopDarkBorder,
    )
  } else {
    lightColorScheme(
      primary = DesktopCrimson,
      background = Color.White,
      surface = Color.White,
      onBackground = Color(0xFF1A1C1E),
      onSurface = Color(0xFF1A1C1E),
      onSurfaceVariant = Color(0xFF6B7280),
      outline = Color(0xFFE2E4E9),
      outlineVariant = Color(0xFFE5E7EB),
    )
  }

  val toggleTheme: () -> Unit = {
    val next = when (session.themeMode) {
      DesktopThemeMode.SYSTEM -> DesktopThemeMode.LIGHT
      DesktopThemeMode.LIGHT -> DesktopThemeMode.DARK
      DesktopThemeMode.DARK -> DesktopThemeMode.SYSTEM
    }
    apiClient.updateThemeMode(next)
  }

  Window(
    onCloseRequest = ::exitApplication,
    title = "Telefam",
    state = WindowState(size = DpSize(1080.dp, 740.dp)),
    onPreviewKeyEvent = { ev ->
      if (ev.type == KeyEventType.KeyDown && ev.key == Key.Escape) {
        when {
          chatPrivacyStack.isNotEmpty() -> {
            chatPrivacyStack.removeAt(chatPrivacyStack.lastIndex)
            true
          }
          showSettingsScreen -> {
            showSettingsScreen = false
            true
          }
          else -> false
        }
      } else {
        false
      }
    },
  ) {
    val windowInfo = LocalWindowInfo.current
    LaunchedEffect(windowInfo.isWindowFocused) {
      callController.onWindowFocusChanged(windowInfo.isWindowFocused)
    }
    val callUiState by callController.uiState.collectAsState()
    LaunchedEffect(callUiState.isMinimizedToChat, callUiState.conversationId) {
      if (callUiState.isMinimizedToChat && callUiState.conversationId.isNotBlank()) {
        activeTab = "chats"
      }
    }
    CompositionLocalProvider(LocalDesktopSmartActions provides smartController) {
      MaterialTheme(colorScheme = scheme) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (splashVisible) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              DesktopLogoBadge(size = 86.dp, animated = true)
              Spacer(modifier = Modifier.height(14.dp))
              Text("Telefam", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = DesktopCrimson)
            }
          }
        } else if (!session.isAuthenticated) {
          DesktopAuthScreen(
            apiClient = apiClient,
            isDark = isDark,
            themeMode = session.themeMode,
            onToggleTheme = toggleTheme,
            onAuthenticated = {},
            onToast = { toastMessage = it },
          )
        } else if (!session.profileCompleted) {
          DesktopProfileSetupScreen(
            apiClient = apiClient,
            isDark = isDark,
            onCompleted = { activeTab = "contacts" },
            onSignOut = { apiClient.signOut() },
            onToast = { toastMessage = it },
          )
        } else {
          Column(modifier = Modifier.fillMaxSize()) {
            // Top Crimson Header matching mobile/tablet (hidden during fullscreen Feeds & Profile)
            if (activeTab != "feeds" && activeTab != "profile") {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(DesktopCrimson)
                  .padding(horizontal = 20.dp, vertical = 12.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Surface(
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(21.dp),
                    color = if (isDark) DesktopDarkSearch else Color.White,
                  ) {
                    Row(
                      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      DesktopSvgIcon(DesktopIconKind.SEARCH, MaterialTheme.colorScheme.onSurfaceVariant, 17.dp)
                      Spacer(modifier = Modifier.width(10.dp))
                      Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        val hint = if (activeTab == "contacts") "Search by name or username" else "Search chats, people, groups"
                        if (searchQuery.isEmpty()) {
                          Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        }
                        BasicTextField(
                          value = searchQuery,
                          onValueChange = { searchQuery = it.take(64) },
                          singleLine = true,
                          textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                          ),
                          cursorBrush = SolidColor(DesktopCrimson),
                          modifier = Modifier.fillMaxWidth(),
                        )
                      }
                    }
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Surface(
                    onClick = toggleTheme,
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.2f),
                  ) {
                    Text(
                      text = session.themeMode.name.lowercase().replaceFirstChar { it.uppercase() },
                      color = Color.White,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold,
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Surface(
                    onClick = {
                      showNotificationsModal = true
                      scope.launch {
                        val fetched = withContext(Dispatchers.IO) { apiClient.listNotifications() }
                        if (fetched != null) {
                          desktopNotifications.clear()
                          desktopNotifications.addAll(fetched.first)
                          unreadNotificationsCount = fetched.second
                        }
                      }
                    },
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(38.dp),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      DesktopSvgIcon(DesktopIconKind.BELL, Color.White, 19.dp)
                      if (unreadNotificationsCount > 0) {
                        Box(
                          modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF151821)),
                        )
                      }
                    }
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Surface(
                    onClick = { chatPrivacyStack.add(DesktopChatPrivacyRoute.PRIVACY) },
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(38.dp),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      DesktopSvgIcon(DesktopIconKind.MORE_VERT, Color.White, 19.dp)
                    }
                  }
                }

                if (activeTab == "contacts") {
                  Spacer(modifier = Modifier.height(10.dp))
                  Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    ContactFilterTab.entries.forEach { tab ->
                      val selected = tab == filter
                      Surface(
                        onClick = { filter = tab },
                        shape = RoundedCornerShape(20.dp),
                        color = if (selected) Color.White else Color.Transparent,
                      ) {
                        Text(
                          text = tab.label,
                          color = if (selected) DesktopCrimson else Color.White,
                          fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                          fontSize = 14.sp,
                          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                      }
                    }
                  }
                }
              }
            }

            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
              AnimatedVisibility(
                visible = activeTab != "contacts" || showChatsRail,
                enter = fadeIn(),
                exit = fadeOut(),
              ) {
                Row(modifier = Modifier.fillMaxHeight()) {
                  Column(
                    modifier = Modifier
                      .width(96.dp)
                      .fillMaxHeight()
                      .background(MaterialTheme.colorScheme.surface)
                      .padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                  ) {
                    DesktopRailButton("Chats", DesktopIconKind.CHATS, activeTab == "chats", badgeCount = unreadChatCount) { activeTab = "chats" }
                    DesktopRailButton("Contacts", DesktopIconKind.CONTACTS, activeTab == "contacts") {
                      activeTab = "contacts"
                      showChatsRail = true
                    }

                    // Center Crimson + Post FAB matching mobile & tablet
                    Column(
                      modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { postLaunchTarget = DesktopPostLaunchTarget.CreateNew }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                      horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                      Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(DesktopCrimson),
                        contentAlignment = Alignment.Center,
                      ) {
                        DesktopSvgIcon(DesktopIconKind.PLUS, Color.White, 22.dp)
                      }
                      Spacer(modifier = Modifier.height(4.dp))
                      Text("Post", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DesktopCrimson)
                    }

                    DesktopRailButton("Feeds", DesktopIconKind.FEEDS, activeTab == "feeds") { activeTab = "feeds" }
                    DesktopRailButton("Profile", DesktopIconKind.PROFILE, activeTab == "profile") { activeTab = "profile" }
                    DesktopRailButton("Alerts", DesktopIconKind.BELL, showNotificationsModal, badgeCount = unreadNotificationsCount) {
                      showNotificationsModal = true
                    }
                  }
                  HorizontalDivider(modifier = Modifier.width(1.dp).fillMaxHeight())
                }
              }

              Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                when (activeTab) {
                  "contacts" -> {
                    val gridState = rememberLazyGridState()
                    LaunchedEffect(gridState) {
                      var lastIdx = 0
                      var lastOffset = 0
                      snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
                        .collect { (idx, off) ->
                          val scrollingDown = idx > lastIdx || (idx == lastIdx && off > lastOffset + 12)
                          val scrollingUp = idx < lastIdx || (idx == lastIdx && off < lastOffset - 12)
                          if (scrollingDown && gridState.isScrollInProgress) {
                            showChatsRail = false
                          } else if (scrollingUp || idx == 0) {
                            showChatsRail = true
                          }
                          lastIdx = idx
                          lastOffset = off
                        }
                    }

                    if (contacts.isEmpty()) {
                      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                          text = "No people found",
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          fontSize = 15.sp,
                        )
                      }
                    } else {
                      LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 340.dp),
                        contentPadding = PaddingValues(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize().widthIn(max = 1180.dp),
                      ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                          Text(
                            text = filter.sectionSubtitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                          )
                        }
                        items(contacts, key = { it.userId }) { contact ->
                          DesktopContactCard(
                            contact = contact,
                            isDark = isDark,
                            onOpenProfile = { openUserProfile(contact.userId) },
                            onToggle = {
                              val now = System.currentTimeMillis()
                              recentActionEpochsMs.removeAll { now - it >= TelefamContactsRules.ONE_MINUTE_MS }
                              if (recentActionEpochsMs.size >= TelefamContactsRules.MAX_ACTIONS_PER_MINUTE) {
                                toastMessage = TelefamContactsRules.RATE_LIMIT_TOAST_MESSAGE
                              } else {
                                recentActionEpochsMs.add(now)
                                val wantFollow = !contact.isFollowing
                                val idx = contacts.indexOfFirst { it.userId == contact.userId }
                                if (idx >= 0) {
                                  contacts[idx] = contact.copy(isFollowing = wantFollow)
                                }
                                scope.launch {
                                  val res = withContext(Dispatchers.IO) {
                                    apiClient.setFollowContact(contact.userId, wantFollow)
                                  }
                                  when (res) {
                                    is DesktopApiOutcome.Ok -> {
                                      val i2 = contacts.indexOfFirst { it.userId == contact.userId }
                                      if (i2 >= 0) contacts[i2] = contacts[i2].copy(isFollowing = res.data)
                                    }
                                    is DesktopApiOutcome.Fail -> {
                                      if (res.statusCode == 429) {
                                        val i2 = contacts.indexOfFirst { it.userId == contact.userId }
                                        if (i2 >= 0) contacts[i2] = contact
                                        toastMessage = TelefamContactsRules.RATE_LIMIT_TOAST_MESSAGE
                                      } else {
                                        pendingContactOps.add(DesktopPendingContactOp(contact.userId, wantFollow))
                                      }
                                    }
                                  }
                                }
                              }
                            },
                          )
                        }
                        if (contactsHasMore) {
                          item(span = { GridItemSpan(maxLineSpan) }) {
                            LaunchedEffect(contactsPage) {
                              val nextPage = contactsPage + 1
                              val res = withContext(Dispatchers.IO) {
                                apiClient.listContacts(filter, searchQuery, nextPage)
                              }
                              if (res is DesktopApiOutcome.Ok) {
                                contactsPage = nextPage
                                res.data.items.forEach { item ->
                                  if (contacts.none { it.userId == item.userId }) contacts.add(item)
                                }
                                contactsHasMore = res.data.hasMore
                              }
                            }
                          }
                        }
                      }
                    }
                  }

                  "feeds" -> {
                    val onInteractFeed: (String, SharedFeedAction, String) -> Unit = { pid, act, reason ->
                      scope.launch {
                        withContext(Dispatchers.IO) {
                          apiClient.interactFeed(postId = pid, action = act, reason = reason)
                        }
                      }
                    }
                    val shareHandler: (DesktopFeedPost) -> Unit = { post ->
                      val url = apiClient.shareUrlForPost(post.postId)
                      runCatching {
                        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(url), null)
                      }
                      val idx = feedPosts.indexOfFirst { it.postId == post.postId }
                      if (idx >= 0) {
                        val cur = feedPosts[idx]
                        feedPosts[idx] = cur.copy(sharesCount = cur.sharesCount + 1)
                      }
                      onInteractFeed(post.postId, SharedFeedAction.SHARE, "")
                      toastMessage = "Link copied"
                    }

                    val notInterestedTopics = settingsBundle.contentPreferences.notInterestedTopics
                    val filteredFeedPosts = remember(feedPosts.toList(), notInterestedTopics) {
                      if (notInterestedTopics.isEmpty()) {
                        feedPosts.toList()
                      } else {
                        val blocked = notInterestedTopics.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
                        feedPosts.filter { p ->
                          p.isOwner || blocked.none { t ->
                            p.hashtags.any { it.lowercase() == t } ||
                              p.caption.lowercase().contains(t) ||
                              p.textContent.lowercase().contains(t)
                          }
                        }
                      }
                    }

                    DesktopReusableFeedStage(
                      apiClient = apiClient,
                      items = filteredFeedPosts,
                      initialIndex = 0,
                      activeTab = activeFeedTab,
                      isStandaloneViewer = false,
                      isHostActive = !showFeedSearch && profileBackStack.isEmpty() && profileViewerState == null && postLaunchTarget == null && !showSettingsScreen && chatPrivacyStack.isEmpty(),
                      contentPreferences = settingsBundle.contentPreferences,
                      onSelectTab = { activeFeedTab = it },
                      onLiveClick = { toastMessage = "LIVE — Coming soon" },
                      onSearchClick = { showFeedSearch = true },
                      onBackClick = {},
                      onCommentClick = { toastMessage = "Comments — Coming soon" },
                      onToggleAuthorFollow = { authorId, currentlyFollowing ->
                        val wantFollow = !currentlyFollowing
                        for (i in feedPosts.indices) {
                          if (feedPosts[i].authorUserId == authorId) {
                            feedPosts[i] = feedPosts[i].copy(isFollowingAuthor = wantFollow)
                          }
                        }
                        scope.launch {
                          withContext(Dispatchers.IO) { apiClient.setFollowContact(authorId, wantFollow) }
                        }
                      },
                      onToggleLike = { pid ->
                        val idx = feedPosts.indexOfFirst { it.postId == pid }
                        if (idx >= 0) {
                          val cur = feedPosts[idx]
                          val next = !cur.isLiked
                          feedPosts[idx] = cur.copy(
                            isLiked = next,
                            likesCount = if (next) cur.likesCount + 1 else (cur.likesCount - 1).coerceAtLeast(0),
                          )
                          onInteractFeed(pid, if (next) SharedFeedAction.LIKE else SharedFeedAction.UNLIKE, "")
                        }
                      },
                      onToggleFavourite = { pid ->
                        val idx = feedPosts.indexOfFirst { it.postId == pid }
                        if (idx >= 0) {
                          val cur = feedPosts[idx]
                          val next = !cur.isFavourited
                          feedPosts[idx] = cur.copy(
                            isFavourited = next,
                            favouritesCount = if (next) cur.favouritesCount + 1 else (cur.favouritesCount - 1).coerceAtLeast(0),
                          )
                          onInteractFeed(pid, if (next) SharedFeedAction.FAVOURITE else SharedFeedAction.UNFAVOURITE, "")
                        }
                      },
                      onToggleRepost = { pid ->
                        val idx = feedPosts.indexOfFirst { it.postId == pid }
                        if (idx >= 0) {
                          val cur = feedPosts[idx]
                          val next = !cur.isReposted
                          feedPosts[idx] = cur.copy(
                            isReposted = next,
                            repostsCount = if (next) cur.repostsCount + 1 else (cur.repostsCount - 1).coerceAtLeast(0),
                          )
                          onInteractFeed(pid, if (next) SharedFeedAction.REPOST else SharedFeedAction.UNREPOST, "")
                          if (next) toastMessage = "Reposted"
                        }
                      },
                      onCopyShareLink = shareHandler,
                      onDownloadVideo = { post ->
                        scope.launch {
                          val outFile = File(System.getProperty("user.home"), "Downloads/telefam_${post.postId}.mp4")
                          val res = withContext(Dispatchers.IO) { apiClient.downloadFeedVideo(post.postId, outFile) }
                          toastMessage = when (res) {
                            is DesktopApiOutcome.Ok -> "Saved to Downloads"
                            is DesktopApiOutcome.Fail -> res.message
                          }
                        }
                      },
                      onNotInterested = { pid ->
                        feedPosts.removeAll { it.postId == pid }
                        onInteractFeed(pid, SharedFeedAction.NOT_INTERESTED, "")
                      },
                      onReport = { pid, reason ->
                        onInteractFeed(pid, SharedFeedAction.REPORT, reason)
                        toastMessage = "Report submitted"
                      },
                      onBlock = { pid, uid ->
                        feedPosts.removeAll { it.authorUserId == uid }
                        onInteractFeed(pid, SharedFeedAction.BLOCK, "")
                      },
                      onMuteAuthor = { _, uid ->
                        feedPosts.removeAll { it.authorUserId == uid }
                        scope.launch {
                          withContext(Dispatchers.IO) { apiClient.muteUser(uid) }
                          toastMessage = "Account muted"
                        }
                      },
                      onEditOwnerPost = { item -> postLaunchTarget = DesktopPostLaunchTarget.EditPost(item) },
                      onChangePostPrivacy = { pid, priv ->
                        for (i in feedPosts.indices) {
                          if (feedPosts[i].postId == pid) {
                            if (priv == SharedPostPrivacy.PRIVATE.apiValue) {
                              feedPosts.removeAt(i)
                              break
                            } else {
                              feedPosts[i] = feedPosts[i].copy(privacy = priv)
                            }
                          }
                        }
                        scope.launch {
                          val res = withContext(Dispatchers.IO) { apiClient.updatePostPrivacy(pid, priv) }
                          toastMessage = when (res) {
                            is DesktopApiOutcome.Ok -> if (priv == "private") "Moved to Locked" else "Privacy updated to ${SharedPostPrivacy.fromApi(priv).label}"
                            is DesktopApiOutcome.Fail -> res.message
                          }
                        }
                      },
                      onDeleteOwnerPost = { pid ->
                        feedPosts.removeAll { it.postId == pid }
                        scope.launch { withContext(Dispatchers.IO) { apiClient.deletePost(pid) } }
                      },
                      onTapHashtag = { tag, mode -> postLaunchTarget = DesktopPostLaunchTarget.PrefillHashtag(tag, mode) },
                      onTapAudio = { song, mode -> postLaunchTarget = DesktopPostLaunchTarget.PrefillSong(song, mode) },
                      onOpenAuthorProfile = { uid -> openUserProfile(uid) },
                      onLoadMore = {
                        if (feedHasMore) {
                          scope.launch {
                            val nextP = feedPage + 1
                            val res = withContext(Dispatchers.IO) { apiClient.getFeeds(activeFeedTab, nextP) }
                            if (res is DesktopApiOutcome.Ok) {
                              feedPage = nextP
                              res.data.items.forEach { item ->
                                if (feedPosts.none { it.postId == item.postId }) feedPosts.add(item)
                              }
                              feedHasMore = res.data.hasMore
                            }
                          }
                        }
                      },
                      onCreateFirstPost = { postLaunchTarget = DesktopPostLaunchTarget.CreateNew },
                      onToast = { toastMessage = it },
                    )
                  }

                  "profile" -> {
                    DesktopUserProfileScreen(
                      apiClient = apiClient,
                      targetUserId = session.userId.ifBlank { "me" },
                      isDark = isDark,
                      isEmbeddedOwnProfileTab = true,
                      onBack = {},
                      onOpenUserProfile = { uid -> openUserProfile(uid) },
                      onOpenPostAtIndex = { list, idx ->
                        val picked = list.getOrNull(idx)
                        if (picked != null) {
                          scope.launch {
                            withContext(Dispatchers.IO) { apiClient.interactFeed(picked.postId, SharedFeedAction.VIEW) }
                          }
                        }
                        profileViewerState = DesktopProfileViewerState(list, idx)
                      },
                      onToggleTheme = toggleTheme,
                      onOpenSettings = { showSettingsScreen = true },
                      onOpenStars = { starsController.openStarsHub(session.location) },
                      onSignOut = { showSettingsScreen = true },
                      onMessageUser = { targetProf ->
                        chatController.openDirectConversationByUser(
                          peerUserId = targetProf.userId,
                          peerName = targetProf.fullName,
                          peerUsername = targetProf.username,
                          peerAvatar = targetProf.avatarUrl,
                        ) {
                          activeTab = "chats"
                        }
                      },
                      onToast = { toastMessage = it },
                    )
                  }

                  else -> {
                    DesktopChatsSplitScreen(
                      controller = chatController,
                      apiClient = apiClient,
                      ownerUserId = session.userId,
                      isDark = isDark,
                      chatPrivacy = settingsBundle.chatPrivacy,
                      searchQuery = searchQuery,
                      onOpenUserProfile = { uid -> openUserProfile(uid) },
                      onOpenPrivacyAndThemes = { chatPrivacyStack.add(DesktopChatPrivacyRoute.PRIVACY) },
                      onStartCall = { conv, callType ->
                        callController.startCall(
                          peerUserId = conv.peerUserId,
                          peerName = conv.peerFullName,
                          peerUsername = conv.peerUsername,
                          peerAvatarUrl = conv.peerAvatarUrl,
                          conversationId = conv.conversationId,
                          callType = callType,
                        )
                      },
                      onOpenSendStars = { conv ->
                        starsController.openSendStarsFromChat(conv, session.location)
                      },
                      onToast = { toastMessage = it },
                    )
                  }
                }
              }
            }
          }

          if (showFeedSearch) {
            DesktopFeedSearchOverlay(
              apiClient = apiClient,
              results = feedSearchResults,
              shortcuts = feedSearchShortcuts,
              query = feedSearchQuery,
              activeShortcut = feedActiveShortcut,
              contentPreferences = settingsBundle.contentPreferences,
              onQueryChanged = { feedSearchQuery = it },
              onShortcutSelected = { sc -> feedActiveShortcut = if (feedActiveShortcut == sc) "" else sc },
              onClose = { showFeedSearch = false },
              onCommentClick = { toastMessage = "Comments — Coming soon" },
              onToggleAuthorFollow = { authorId, currentlyFollowing ->
                val wantFollow = !currentlyFollowing
                feedSearchResults = feedSearchResults.map {
                  if (it.authorUserId == authorId) it.copy(isFollowingAuthor = wantFollow) else it
                }
                scope.launch { withContext(Dispatchers.IO) { apiClient.setFollowContact(authorId, wantFollow) } }
              },
              onToggleLike = { pid ->
                feedSearchResults = feedSearchResults.map {
                  if (it.postId == pid) {
                    val next = !it.isLiked
                    it.copy(isLiked = next, likesCount = if (next) it.likesCount + 1 else (it.likesCount - 1).coerceAtLeast(0))
                  } else it
                }
                val item = feedSearchResults.firstOrNull { it.postId == pid }
                scope.launch {
                  withContext(Dispatchers.IO) {
                    apiClient.interactFeed(pid, if (item?.isLiked == true) SharedFeedAction.LIKE else SharedFeedAction.UNLIKE)
                  }
                }
              },
              onToggleFavourite = { pid ->
                feedSearchResults = feedSearchResults.map {
                  if (it.postId == pid) {
                    val next = !it.isFavourited
                    it.copy(isFavourited = next, favouritesCount = if (next) it.favouritesCount + 1 else (it.favouritesCount - 1).coerceAtLeast(0))
                  } else it
                }
                val item = feedSearchResults.firstOrNull { it.postId == pid }
                scope.launch {
                  withContext(Dispatchers.IO) {
                    apiClient.interactFeed(pid, if (item?.isFavourited == true) SharedFeedAction.FAVOURITE else SharedFeedAction.UNFAVOURITE)
                  }
                }
              },
              onToggleRepost = { pid ->
                feedSearchResults = feedSearchResults.map {
                  if (it.postId == pid) {
                    val next = !it.isReposted
                    it.copy(isReposted = next, repostsCount = if (next) it.repostsCount + 1 else (it.repostsCount - 1).coerceAtLeast(0))
                  } else it
                }
                val item = feedSearchResults.firstOrNull { it.postId == pid }
                if (item?.isReposted == true) toastMessage = "Reposted"
                scope.launch {
                  withContext(Dispatchers.IO) {
                    apiClient.interactFeed(pid, if (item?.isReposted == true) SharedFeedAction.REPOST else SharedFeedAction.UNREPOST)
                  }
                }
              },
              onCopyShareLink = { post ->
                val url = apiClient.shareUrlForPost(post.postId)
                runCatching { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(url), null) }
                toastMessage = "Link copied"
              },
              onDownloadVideo = { post ->
                scope.launch {
                  val outFile = File(System.getProperty("user.home"), "Downloads/telefam_${post.postId}.mp4")
                  val res = withContext(Dispatchers.IO) { apiClient.downloadFeedVideo(post.postId, outFile) }
                  toastMessage = if (res is DesktopApiOutcome.Ok) "Saved to Downloads" else "Download unavailable"
                }
              },
              onNotInterested = { pid -> feedSearchResults = feedSearchResults.filterNot { it.postId == pid } },
              onReport = { pid, reason ->
                scope.launch { withContext(Dispatchers.IO) { apiClient.interactFeed(pid, SharedFeedAction.REPORT, reason) } }
                toastMessage = "Report submitted"
              },
              onBlock = { pid, uid ->
                feedSearchResults = feedSearchResults.filterNot { it.authorUserId == uid }
                scope.launch { withContext(Dispatchers.IO) { apiClient.interactFeed(pid, SharedFeedAction.BLOCK) } }
              },
              onMuteAuthor = { _, uid ->
                feedSearchResults = feedSearchResults.filterNot { it.authorUserId == uid }
                feedPosts.removeAll { it.authorUserId == uid }
                scope.launch {
                  withContext(Dispatchers.IO) { apiClient.muteUser(uid) }
                  toastMessage = "Account muted"
                }
              },
              onEditOwnerPost = { item ->
                showFeedSearch = false
                postLaunchTarget = DesktopPostLaunchTarget.EditPost(item)
              },
              onChangePostPrivacy = { pid, priv ->
                feedSearchResults = if (priv == SharedPostPrivacy.PRIVATE.apiValue) {
                  feedSearchResults.filterNot { it.postId == pid }
                } else {
                  feedSearchResults.map { if (it.postId == pid) it.copy(privacy = priv) else it }
                }
                scope.launch {
                  val res = withContext(Dispatchers.IO) { apiClient.updatePostPrivacy(pid, priv) }
                  toastMessage = when (res) {
                    is DesktopApiOutcome.Ok -> if (priv == "private") "Moved to Locked" else "Privacy updated to ${SharedPostPrivacy.fromApi(priv).label}"
                    is DesktopApiOutcome.Fail -> res.message
                  }
                }
              },
              onDeleteOwnerPost = { pid ->
                feedSearchResults = feedSearchResults.filterNot { it.postId == pid }
                scope.launch { withContext(Dispatchers.IO) { apiClient.deletePost(pid) } }
              },
              onTapHashtag = { tag, mode ->
                showFeedSearch = false
                postLaunchTarget = DesktopPostLaunchTarget.PrefillHashtag(tag, mode)
              },
              onTapAudio = { song, mode ->
                showFeedSearch = false
                postLaunchTarget = DesktopPostLaunchTarget.PrefillSong(song, mode)
              },
              onOpenAuthorProfile = { uid ->
                showFeedSearch = false
                openUserProfile(uid)
              },
              onToast = { toastMessage = it },
            )
          }

          val activeProfUid = profileBackStack.lastOrNull()
          if (activeProfUid != null) {
            DesktopUserProfileScreen(
              apiClient = apiClient,
              targetUserId = activeProfUid,
              isDark = isDark,
              isEmbeddedOwnProfileTab = false,
              onBack = {
                if (profileBackStack.isNotEmpty()) {
                  profileBackStack.removeAt(profileBackStack.lastIndex)
                }
              },
              onOpenUserProfile = { uid -> openUserProfile(uid) },
              onOpenPostAtIndex = { list, idx ->
                val picked = list.getOrNull(idx)
                if (picked != null) {
                  scope.launch {
                    withContext(Dispatchers.IO) { apiClient.interactFeed(picked.postId, SharedFeedAction.VIEW) }
                  }
                }
                profileViewerState = DesktopProfileViewerState(list, idx)
              },
              onToggleTheme = toggleTheme,
              onOpenSettings = { showSettingsScreen = true },
              onOpenStars = { starsController.openStarsHub(session.location) },
              onSignOut = {
                showSettingsScreen = true
              },
              onMessageUser = { targetProf ->
                profileBackStack.clear()
                chatController.openDirectConversationByUser(
                  peerUserId = targetProf.userId,
                  peerName = targetProf.fullName,
                  peerUsername = targetProf.username,
                  peerAvatar = targetProf.avatarUrl,
                ) {
                  activeTab = "chats"
                }
              },
              onToast = { toastMessage = it },
            )
          }

          val viewerState = profileViewerState
          if (viewerState != null) {
            DesktopReusableFeedStage(
              apiClient = apiClient,
              items = viewerState.items,
              initialIndex = viewerState.initialIndex,
              activeTab = SharedFeedTab.FOR_YOU,
              isStandaloneViewer = true,
              isHostActive = postLaunchTarget == null && !showSettingsScreen,
              contentPreferences = settingsBundle.contentPreferences,
              onSelectTab = {},
              onLiveClick = {},
              onSearchClick = { profileViewerState = null; showFeedSearch = true },
              onBackClick = { profileViewerState = null },
              onCommentClick = { toastMessage = "Comments — Coming soon" },
              onToggleAuthorFollow = { authorId, currentlyFollowing ->
                val wantFollow = !currentlyFollowing
                profileViewerState = viewerState.copy(
                  items = viewerState.items.map {
                    if (it.authorUserId == authorId) it.copy(isFollowingAuthor = wantFollow) else it
                  },
                )
                scope.launch { withContext(Dispatchers.IO) { apiClient.setFollowContact(authorId, wantFollow) } }
              },
              onToggleLike = { pid ->
                profileViewerState = viewerState.copy(
                  items = viewerState.items.map {
                    if (it.postId == pid) {
                      val next = !it.isLiked
                      it.copy(isLiked = next, likesCount = if (next) it.likesCount + 1 else (it.likesCount - 1).coerceAtLeast(0))
                    } else it
                  },
                )
                val target = viewerState.items.firstOrNull { it.postId == pid }
                scope.launch {
                  withContext(Dispatchers.IO) {
                    apiClient.interactFeed(pid, if (target?.isLiked != true) SharedFeedAction.LIKE else SharedFeedAction.UNLIKE)
                  }
                }
              },
              onToggleFavourite = { pid ->
                profileViewerState = viewerState.copy(
                  items = viewerState.items.map {
                    if (it.postId == pid) {
                      val next = !it.isFavourited
                      it.copy(isFavourited = next, favouritesCount = if (next) it.favouritesCount + 1 else (it.favouritesCount - 1).coerceAtLeast(0))
                    } else it
                  },
                )
              },
              onToggleRepost = { pid ->
                profileViewerState = viewerState.copy(
                  items = viewerState.items.map {
                    if (it.postId == pid) {
                      val next = !it.isReposted
                      it.copy(isReposted = next, repostsCount = if (next) it.repostsCount + 1 else (it.repostsCount - 1).coerceAtLeast(0))
                    } else it
                  },
                )
                toastMessage = "Reposted"
              },
              onCopyShareLink = { post ->
                val url = apiClient.shareUrlForPost(post.postId)
                runCatching { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(url), null) }
                toastMessage = "Link copied"
              },
              onDownloadVideo = { post ->
                scope.launch {
                  val outFile = File(System.getProperty("user.home"), "Downloads/telefam_${post.postId}.mp4")
                  val res = withContext(Dispatchers.IO) { apiClient.downloadFeedVideo(post.postId, outFile) }
                  toastMessage = if (res is DesktopApiOutcome.Ok) "Saved to Downloads" else "Download unavailable"
                }
              },
              onNotInterested = { pid ->
                val remaining = viewerState.items.filterNot { it.postId == pid }
                profileViewerState = if (remaining.isEmpty()) null else viewerState.copy(items = remaining, initialIndex = 0)
              },
              onReport = { pid, reason ->
                scope.launch { withContext(Dispatchers.IO) { apiClient.interactFeed(pid, SharedFeedAction.REPORT, reason) } }
                toastMessage = "Report submitted"
              },
              onBlock = { _, uid ->
                profileViewerState = null
                scope.launch { withContext(Dispatchers.IO) { apiClient.blockUser(uid) } }
              },
              onMuteAuthor = { _, uid ->
                profileViewerState = null
                feedPosts.removeAll { it.authorUserId == uid }
                scope.launch {
                  withContext(Dispatchers.IO) { apiClient.muteUser(uid) }
                  toastMessage = "Account muted"
                }
              },
              onEditOwnerPost = { item ->
                profileViewerState = null
                postLaunchTarget = DesktopPostLaunchTarget.EditPost(item)
              },
              onChangePostPrivacy = { pid, priv ->
                profileViewerState = viewerState.copy(
                  items = viewerState.items.map { if (it.postId == pid) it.copy(privacy = priv) else it },
                )
                scope.launch {
                  val res = withContext(Dispatchers.IO) { apiClient.updatePostPrivacy(pid, priv) }
                  toastMessage = when (res) {
                    is DesktopApiOutcome.Ok -> if (priv == "private") "Moved to Locked" else "Privacy updated to ${SharedPostPrivacy.fromApi(priv).label}"
                    is DesktopApiOutcome.Fail -> res.message
                  }
                }
              },
              onDeleteOwnerPost = { pid ->
                val remaining = viewerState.items.filterNot { it.postId == pid }
                profileViewerState = if (remaining.isEmpty()) null else viewerState.copy(items = remaining, initialIndex = 0)
                scope.launch { withContext(Dispatchers.IO) { apiClient.deletePost(pid) } }
              },
              onTapHashtag = { tag, mode ->
                profileViewerState = null
                postLaunchTarget = DesktopPostLaunchTarget.PrefillHashtag(tag, mode)
              },
              onTapAudio = { song, mode ->
                profileViewerState = null
                postLaunchTarget = DesktopPostLaunchTarget.PrefillSong(song, mode)
              },
              onOpenAuthorProfile = { uid ->
                profileViewerState = null
                openUserProfile(uid)
              },
              onLoadMore = {},
              onCreateFirstPost = { profileViewerState = null; postLaunchTarget = DesktopPostLaunchTarget.CreateNew },
              onToast = { toastMessage = it },
            )
          }

          val currentTarget = postLaunchTarget
          if (currentTarget != null) {
            DesktopPostComposerOverlay(
              apiClient = apiClient,
              isDark = isDark,
              contacts = contacts,
              knownHashtags = knownHashtags,
              launchTarget = currentTarget,
              onDismiss = { postLaunchTarget = null },
              onPublished = { publishedPost ->
                val existingIdx = feedPosts.indexOfFirst { it.postId == publishedPost.postId }
                if (existingIdx >= 0) {
                  feedPosts[existingIdx] = publishedPost
                } else {
                  feedPosts.add(0, publishedPost)
                }
                scope.launch {
                  delay(450L)
                  postLaunchTarget = null
                }
              },
              onToast = { msg -> toastMessage = msg },
            )
          }

          if (showSettingsScreen) {
            DesktopSettingsAndPrivacyScreen(
              apiClient = apiClient,
              isDark = isDark,
              settingsBundle = settingsBundle,
              avatarUrl = "",
              onSettingsUpdated = { settingsBundle = it },
              onToggleTheme = toggleTheme,
              onLoggedOut = { allDevices ->
                showSettingsScreen = false
                profileBackStack.clear()
                profileViewerState = null
                callController.detachSession()
                chatController.detachSession()
                starsController.detachSession()
                smartController.onLogout()
                scope.launch {
                  withContext(Dispatchers.IO) { apiClient.signOut(allDevices) }
                }
              },
              onClose = { showSettingsScreen = false },
              onToast = { toastMessage = it },
            )
          }

          val topChatPrivacyRoute = chatPrivacyStack.lastOrNull()
          if (topChatPrivacyRoute != null) {
            DesktopChatPrivacyHostScreen(
              route = topChatPrivacyRoute,
              isDark = isDark,
              state = settingsBundle.chatPrivacy,
              previewSenderName = apiClient.session.fullName.trim().ifBlank {
                apiClient.session.username.trim().ifBlank { "Telefam" }
              },
              onNavigateBack = {
                if (chatPrivacyStack.isNotEmpty()) chatPrivacyStack.removeAt(chatPrivacyStack.lastIndex)
              },
              onUpdatePrivacy = { nextChat ->
                settingsBundle = settingsBundle.copy(chatPrivacy = nextChat)
                scope.launch {
                  val synced = withContext(Dispatchers.IO) { apiClient.updateChatPrivacySettings(nextChat) }
                  settingsBundle = synced
                }
              },
              onOpenChatsTheme = { chatPrivacyStack.add(DesktopChatPrivacyRoute.CHATS_THEME) },
              onOpenBubbleColors = { chatPrivacyStack.add(DesktopChatPrivacyRoute.BUBBLE_COLORS) },
              onSelectTheme = { themeId ->
                settingsBundle = settingsBundle.copy(
                  chatPrivacy = settingsBundle.chatPrivacy.copy(chatThemeId = themeId),
                )
                if (chatPrivacyStack.isNotEmpty()) chatPrivacyStack.removeAt(chatPrivacyStack.lastIndex)
                scope.launch {
                  val synced = withContext(Dispatchers.IO) { apiClient.updateChatThemeAndBubble(themeId = themeId) }
                  settingsBundle = synced
                }
                toastMessage = "Chat theme applied"
              },
              onSelectBubbleColor = { colorId ->
                settingsBundle = settingsBundle.copy(
                  chatPrivacy = settingsBundle.chatPrivacy.copy(bubbleColorId = colorId),
                )
                if (chatPrivacyStack.isNotEmpty()) chatPrivacyStack.removeAt(chatPrivacyStack.lastIndex)
                scope.launch {
                  val synced = withContext(Dispatchers.IO) { apiClient.updateChatThemeAndBubble(bubbleColorId = colorId) }
                  settingsBundle = synced
                }
                toastMessage = "Message bubble colour updated"
              },
            )
          }
        }

        val pendingReq = incomingLoginRequest
        if (pendingReq != null && loginApprovedState == null) {
          DesktopIncomingLoginRequestOverlay(
            request = pendingReq,
            isResponding = isRespondingToLoginReq,
            actionError = incomingLoginError,
            onDeny = {
              if (!isRespondingToLoginReq) {
                isRespondingToLoginReq = true
                incomingLoginError = null
                scope.launch {
                  val res = withContext(Dispatchers.IO) {
                    apiClient.respondToLoginRequest(pendingReq.requestId, accept = false)
                  }
                  isRespondingToLoginReq = false
                  when (res) {
                    is DesktopApiOutcome.Ok -> {
                      incomingLoginRequest = null
                      toastMessage = "Login request denied and device blocked."
                    }
                    is DesktopApiOutcome.Fail -> {
                      incomingLoginError = res.message
                    }
                  }
                }
              }
            },
            onAccept = {
              if (!isRespondingToLoginReq) {
                isRespondingToLoginReq = true
                incomingLoginError = null
                scope.launch {
                  val res = withContext(Dispatchers.IO) {
                    apiClient.respondToLoginRequest(pendingReq.requestId, accept = true)
                  }
                  isRespondingToLoginReq = false
                  when (res) {
                    is DesktopApiOutcome.Ok -> {
                      loginApprovedState = DesktopLoginApprovedState(
                        deviceName = pendingReq.deviceName,
                        location = pendingReq.location,
                        requestedAtMs = pendingReq.requestedAtMs,
                      )
                      incomingLoginRequest = null
                    }
                    is DesktopApiOutcome.Fail -> {
                      incomingLoginError = res.message
                    }
                  }
                }
              }
            },
          )
        }

        val approved = loginApprovedState
        if (approved != null) {
          DesktopLoginApprovedOverlay(
            approved = approved,
            onDone = { loginApprovedState = null },
          )
        }

        smartController.activeBrowserUrl?.let { browserUrl ->
          DesktopInAppBrowserModal(
            initialUrl = browserUrl,
            titleHint = smartController.activeBrowserTitleHint,
            source = smartController.activeBrowserSource,
            controller = smartController,
            onClose = { smartController.closeBrowser() },
          )
        }

        smartController.activePhoneEntity?.let { phone ->
          DesktopSmartPhoneActionDialog(
            phone = phone,
            controller = smartController,
            onDismiss = { smartController.activePhoneEntity = null },
          )
        }

        smartController.activeEmailEntity?.let { email ->
          DesktopSmartEmailActionDialog(
            email = email,
            controller = smartController,
            onDismiss = { smartController.activeEmailEntity = null },
          )
        }

        if (showNotificationsModal && session.isAuthenticated && session.profileCompleted) {
          DesktopNotificationsInboxModal(
            items = desktopNotifications.toList(),
            onDismiss = { showNotificationsModal = false },
            onMarkAllRead = {
              for (i in desktopNotifications.indices) {
                desktopNotifications[i] = desktopNotifications[i].copy(isRead = true)
              }
              unreadNotificationsCount = 0
              scope.launch { withContext(Dispatchers.IO) { apiClient.markAllNotificationsRead() } }
            },
            onSelectItem = { item ->
              val idx = desktopNotifications.indexOfFirst { it.id == item.id }
              if (idx >= 0 && !desktopNotifications[idx].isRead) {
                desktopNotifications[idx] = desktopNotifications[idx].copy(isRead = true)
                unreadNotificationsCount = (unreadNotificationsCount - 1).coerceAtLeast(0)
              }
              showNotificationsModal = false
              scope.launch { withContext(Dispatchers.IO) { apiClient.markNotificationRead(notificationId = item.id) } }
              when {
                item.conversationId.isNotBlank() -> {
                  activeTab = "chats"
                  chatController.openConversation(item.conversationId)
                }
                item.kind.equals("star_received", ignoreCase = true) -> {
                  starsController.openStarsHub(session.location)
                }
                item.postId.isNotBlank() -> {
                  val post = feedPosts.firstOrNull { it.postId == item.postId }
                  if (post != null) {
                    profileViewerState = listOf(post) to 0
                  } else if (item.targetUserId.isNotBlank() || item.actorUserId.isNotBlank()) {
                    openUserProfile(item.targetUserId.ifBlank { item.actorUserId })
                  } else {
                    activeTab = "feeds"
                  }
                }
                item.targetUserId.isNotBlank() || item.actorUserId.isNotBlank() -> {
                  openUserProfile(item.targetUserId.ifBlank { item.actorUserId })
                }
              }
            },
          )
        }

        if (starsState.isOpen && session.isAuthenticated && session.profileCompleted) {
          DesktopStarsHostScreen(
            controller = starsController,
            isDark = isDark,
            onOpenUserProfile = { uid -> openUserProfile(uid) },
          )
        }

        val burstStars = starsState.celebrationBurstStars
        LaunchedEffect(burstStars) {
          if (burstStars != null) {
            delay(2_200L)
            starsController.dismissCelebrationBurst()
          }
        }
        if (burstStars != null) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f), shadowElevation = 12.dp) {
              Box(modifier = Modifier.padding(horizontal = 30.dp, vertical = 22.dp)) {
                DesktopAnimated3DStarEmblem(
                  starsAmount = burstStars,
                  baseSize = if (burstStars >= 1_000L) 108.dp else 88.dp,
                  showTelefamWordmark = true,
                  isDarkTheme = isDark,
                )
              }
            }
          }
        }

        val banner = incomingMsgBanner
        val pushPrefs = settingsBundle.notifications
        val nowMinutes = remember(banner?.messageId) {
          val cal = java.util.Calendar.getInstance()
          cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        }
        val quietActive = pushPrefs.quietHoursEnabled && run {
          val s = ((pushPrefs.quietStartMinutes % 1440) + 1440) % 1440
          val e = ((pushPrefs.quietEndMinutes % 1440) + 1440) % 1440
          when {
            s == e -> false
            s < e -> nowMinutes in s until e
            else -> nowMinutes >= s || nowMinutes < e
          }
        }
        val showBanner = banner != null && !pushPrefs.masterPause && pushPrefs.channelPush && pushPrefs.typeMessages && !quietActive
        if (showBanner && banner != null) {
          Surface(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(top = 64.dp, end = 20.dp)
              .widthIn(max = 360.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 10.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                DesktopSvgIcon(DesktopIconKind.BELL, DesktopCrimson, 16.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = banner.senderName,
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.weight(1f),
                )
                Box(
                  modifier = Modifier
                    .clip(CircleShape)
                    .clickable { chatController.dismissIncomingBanner() }
                    .padding(4.dp),
                ) {
                  DesktopSvgIcon(DesktopIconKind.CLOSE, MaterialTheme.colorScheme.onSurfaceVariant, 14.dp)
                }
              }
              Text(
                text = banner.previewText,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
              )
              if (banner.canReply && !banner.isViewOnce) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Surface(
                    modifier = Modifier.weight(1f).height(34.dp),
                    shape = RoundedCornerShape(17.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                  ) {
                    Box(
                      modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                      contentAlignment = Alignment.CenterStart,
                    ) {
                      if (bannerReplyDraft.isEmpty()) {
                        Text("Write a reply...", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                      BasicTextField(
                        value = bannerReplyDraft,
                        onValueChange = { bannerReplyDraft = it.take(500) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                          color = MaterialTheme.colorScheme.onSurface,
                          fontSize = 12.5.sp,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                      )
                    }
                  }
                  TextButton(
                    onClick = {
                      if (bannerReplyDraft.isNotBlank()) {
                        chatController.sendQuickReplyFromBanner(
                          banner.conversationId,
                          banner.peerUserId,
                          bannerReplyDraft,
                        )
                        bannerReplyDraft = ""
                      }
                    },
                  ) {
                    Text("Send", color = DesktopCrimson, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                  }
                }
              }
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(
                  onClick = { chatController.markConversationReadFromBanner(banner.conversationId) },
                ) {
                  Text("Mark as read", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(
                  onClick = {
                    chatController.dismissIncomingBanner()
                    activeTab = "chats"
                    chatController.openConversation(banner.conversationId)
                  },
                ) {
                  Text("Open chat", fontSize = 12.sp, color = DesktopCrimson, fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }
        }

        DesktopCallOverlayHost(
          controller = callController,
          apiClient = apiClient,
          modifier = Modifier.fillMaxSize(),
        )

        if (toastMessage != null) {
          Surface(
            color = Color.Black.copy(alpha = 0.85f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
          ) {
            Text(
              text = toastMessage.orEmpty(),
              color = Color.White,
              fontSize = 13.5.sp,
              modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            )
          }
        }
      }
    }
    }
  }
}

@Composable
private fun DesktopRailButton(
  label: String,
  icon: DesktopIconKind,
  selected: Boolean,
  badgeCount: Int = 0,
  onClick: () -> Unit,
) {
  val tint = if (selected) DesktopCrimson else MaterialTheme.colorScheme.onSurfaceVariant
  Column(
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Box(contentAlignment = Alignment.TopEnd) {
      DesktopSvgIcon(icon, tint, 21.dp)
      if (badgeCount > 0) {
        Surface(
          shape = CircleShape,
          color = DesktopCrimson,
          modifier = Modifier.padding(start = 12.dp),
        ) {
          Text(
            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
            color = Color.White,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp),
          )
        }
      }
    }
    Text(
      text = label,
      fontSize = 12.sp,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      color = tint,
    )
  }
}

private enum class DesktopIconKind {
  CLOSE, PLUS, TEXT_MODE, PHOTOS_MODE, VIDEO_MODE, MUSIC, TAG_PEOPLE, HASHTAG,
  MUTE, UNMUTE, CHECK, CHEVRON_RIGHT, HEART, HEART_FILLED, COMMENT, SHARE_ARROW,
  BOOKMARK, BOOKMARK_FILLED, MORE_DOTS, MORE_VERT, CHEVRON_UP, CHEVRON_DOWN, BACK_ARROW,
  SEARCH, LIVE_TV, SPARKLE, CHATS, CONTACTS, FEEDS, PROFILE,
  INVITE_T_BADGE, LOCATION_PIN, GRID_2X2, FILTER_SLIDERS, PLAY_OUTLINE, CROWN, PERSON_ADD,
  GEAR, THREE_DASHES, CAMERA, EDIT_PENCIL, SHARE_NODES, LOCK, REPOST, FRIENDS_GROUP, BELL
}

@Composable
private fun DesktopSvgIcon(
  kind: DesktopIconKind,
  tint: Color,
  size: Dp = 18.dp,
) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = Stroke(width = w * 0.095f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (kind) {
      DesktopIconKind.CLOSE -> {
        drawLine(tint, Offset(w * 0.22f, h * 0.22f), Offset(w * 0.78f, h * 0.78f), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.78f, h * 0.22f), Offset(w * 0.22f, h * 0.78f), stroke.width, StrokeCap.Round)
      }
      DesktopIconKind.PLUS -> {
        drawLine(tint, Offset(w * 0.5f, h * 0.2f), Offset(w * 0.5f, h * 0.8f), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.8f, h * 0.5f), stroke.width, StrokeCap.Round)
      }
      DesktopIconKind.TEXT_MODE -> {
        drawLine(tint, Offset(w * 0.2f, h * 0.25f), Offset(w * 0.8f, h * 0.25f), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.5f, h * 0.25f), Offset(w * 0.5f, h * 0.8f), stroke.width, StrokeCap.Round)
      }
      DesktopIconKind.PHOTOS_MODE -> {
        drawRoundRect(tint, Offset(w * 0.16f, h * 0.2f), Size(w * 0.68f, h * 0.6f), CornerRadius(w * 0.1f), style = stroke)
        drawCircle(tint, radius = w * 0.08f, center = Offset(w * 0.34f, h * 0.38f))
        val path = Path().apply {
          moveTo(w * 0.22f, h * 0.72f)
          lineTo(w * 0.44f, h * 0.50f)
          lineTo(w * 0.60f, h * 0.64f)
          lineTo(w * 0.78f, h * 0.48f)
        }
        drawPath(path, tint, style = stroke)
      }
      DesktopIconKind.VIDEO_MODE -> {
        drawRoundRect(tint, Offset(w * 0.14f, h * 0.22f), Size(w * 0.72f, h * 0.56f), CornerRadius(w * 0.1f), style = stroke)
        val tri = Path().apply {
          moveTo(w * 0.42f, h * 0.38f)
          lineTo(w * 0.64f, h * 0.50f)
          lineTo(w * 0.42f, h * 0.62f)
          close()
        }
        drawPath(tri, tint)
      }
      DesktopIconKind.MUSIC -> {
        drawCircle(tint, radius = w * 0.14f, center = Offset(w * 0.34f, h * 0.72f))
        drawLine(tint, Offset(w * 0.46f, h * 0.72f), Offset(w * 0.46f, h * 0.22f), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.46f, h * 0.22f), Offset(w * 0.76f, h * 0.30f), stroke.width, StrokeCap.Round)
      }
      DesktopIconKind.TAG_PEOPLE, DesktopIconKind.CONTACTS -> {
        drawCircle(tint, radius = w * 0.16f, center = Offset(w * 0.42f, h * 0.34f), style = stroke)
        val arc = Path().apply {
          moveTo(w * 0.18f, h * 0.78f)
          cubicTo(w * 0.18f, h * 0.56f, w * 0.66f, h * 0.56f, w * 0.66f, h * 0.78f)
        }
        drawPath(arc, tint, style = stroke)
        if (kind == DesktopIconKind.TAG_PEOPLE) {
          drawLine(tint, Offset(w * 0.78f, h * 0.34f), Offset(w * 0.78f, h * 0.54f), stroke.width, StrokeCap.Round)
          drawLine(tint, Offset(w * 0.68f, h * 0.44f), Offset(w * 0.88f, h * 0.44f), stroke.width, StrokeCap.Round)
        }
      }
      DesktopIconKind.HASHTAG -> {
        drawLine(tint, Offset(w * 0.38f, h * 0.18f), Offset(w * 0.30f, h * 0.82f), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.68f, h * 0.18f), Offset(w * 0.60f, h * 0.82f), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.18f, h * 0.38f), Offset(w * 0.82f, h * 0.38f), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.16f, h * 0.64f), Offset(w * 0.80f, h * 0.64f), stroke.width, StrokeCap.Round)
      }
      DesktopIconKind.MUTE -> {
        val spk = Path().apply {
          moveTo(w * 0.2f, h * 0.42f)
          lineTo(w * 0.38f, h * 0.42f)
          lineTo(w * 0.56f, h * 0.24f)
          lineTo(w * 0.56f, h * 0.76f)
          lineTo(w * 0.38f, h * 0.58f)
          lineTo(w * 0.2f, h * 0.58f)
          close()
        }
        drawPath(spk, tint)
        drawLine(tint, Offset(w * 0.68f, h * 0.38f), Offset(w * 0.86f, h * 0.62f), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.86f, h * 0.38f), Offset(w * 0.68f, h * 0.62f), stroke.width, StrokeCap.Round)
      }
      DesktopIconKind.UNMUTE -> {
        val spk = Path().apply {
          moveTo(w * 0.2f, h * 0.42f)
          lineTo(w * 0.38f, h * 0.42f)
          lineTo(w * 0.56f, h * 0.24f)
          lineTo(w * 0.56f, h * 0.76f)
          lineTo(w * 0.38f, h * 0.58f)
          lineTo(w * 0.2f, h * 0.58f)
          close()
        }
        drawPath(spk, tint)
        drawLine(tint, Offset(w * 0.70f, h * 0.34f), Offset(w * 0.70f, h * 0.66f), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.84f, h * 0.24f), Offset(w * 0.84f, h * 0.76f), stroke.width, StrokeCap.Round)
      }
      DesktopIconKind.CHECK -> {
        val chk = Path().apply {
          moveTo(w * 0.22f, h * 0.52f)
          lineTo(w * 0.42f, h * 0.72f)
          lineTo(w * 0.78f, h * 0.28f)
        }
        drawPath(chk, tint, style = stroke)
      }
      DesktopIconKind.CHEVRON_RIGHT -> {
        val chev = Path().apply {
          moveTo(w * 0.36f, h * 0.24f)
          lineTo(w * 0.64f, h * 0.50f)
          lineTo(w * 0.36f, h * 0.76f)
        }
        drawPath(chev, tint, style = stroke)
      }
      DesktopIconKind.HEART, DesktopIconKind.HEART_FILLED -> {
        val heart = Path().apply {
          moveTo(w * 0.50f, h * 0.84f)
          cubicTo(w * 0.14f, h * 0.60f, w * 0.10f, h * 0.28f, w * 0.32f, h * 0.20f)
          cubicTo(w * 0.42f, h * 0.16f, w * 0.49f, h * 0.24f, w * 0.50f, h * 0.30f)
          cubicTo(w * 0.51f, h * 0.24f, w * 0.58f, h * 0.16f, w * 0.68f, h * 0.20f)
          cubicTo(w * 0.90f, h * 0.28f, w * 0.86f, h * 0.60f, w * 0.50f, h * 0.84f)
          close()
        }
        if (kind == DesktopIconKind.HEART_FILLED) drawPath(heart, tint) else drawPath(heart, tint, style = stroke)
      }
      DesktopIconKind.COMMENT, DesktopIconKind.CHATS -> {
        val bubble = Path().apply {
          moveTo(w * 0.16f, h * 0.22f)
          lineTo(w * 0.84f, h * 0.22f)
          lineTo(w * 0.84f, h * 0.70f)
          lineTo(w * 0.42f, h * 0.70f)
          lineTo(w * 0.24f, h * 0.84f)
          lineTo(w * 0.24f, h * 0.70f)
          lineTo(w * 0.16f, h * 0.70f)
          close()
        }
        drawPath(bubble, tint, style = stroke)
      }
      DesktopIconKind.SHARE_ARROW -> {
        val plane = Path().apply {
          moveTo(w * 0.88f, h * 0.16f)
          lineTo(w * 0.12f, h * 0.46f)
          lineTo(w * 0.44f, h * 0.58f)
          lineTo(w * 0.56f, h * 0.88f)
          close()
        }
        drawPath(plane, tint, style = stroke)
      }
      DesktopIconKind.BOOKMARK, DesktopIconKind.BOOKMARK_FILLED -> {
        val bm = Path().apply {
          moveTo(w * 0.26f, h * 0.16f)
          lineTo(w * 0.74f, h * 0.16f)
          lineTo(w * 0.74f, h * 0.86f)
          lineTo(w * 0.50f, h * 0.68f)
          lineTo(w * 0.26f, h * 0.86f)
          close()
        }
        if (kind == DesktopIconKind.BOOKMARK_FILLED) drawPath(bm, tint) else drawPath(bm, tint, style = stroke)
      }
      DesktopIconKind.MORE_DOTS -> {
        listOf(0.24f, 0.50f, 0.76f).forEach { rx ->
          drawCircle(tint, radius = w * 0.08f, center = Offset(w * rx, h * 0.50f))
        }
      }
      DesktopIconKind.MORE_VERT -> {
        listOf(0.24f, 0.50f, 0.76f).forEach { ry ->
          drawCircle(tint, radius = w * 0.08f, center = Offset(w * 0.50f, h * ry))
        }
      }
      DesktopIconKind.CHEVRON_UP -> {
        val p = Path().apply {
          moveTo(w * 0.24f, h * 0.62f)
          lineTo(w * 0.50f, h * 0.36f)
          lineTo(w * 0.76f, h * 0.62f)
        }
        drawPath(p, tint, style = stroke)
      }
      DesktopIconKind.CHEVRON_DOWN -> {
        val p = Path().apply {
          moveTo(w * 0.24f, h * 0.38f)
          lineTo(w * 0.50f, h * 0.64f)
          lineTo(w * 0.76f, h * 0.38f)
        }
        drawPath(p, tint, style = stroke)
      }
      DesktopIconKind.BACK_ARROW -> {
        val p = Path().apply {
          moveTo(w * 0.58f, h * 0.22f)
          lineTo(w * 0.28f, h * 0.50f)
          lineTo(w * 0.58f, h * 0.78f)
        }
        drawPath(p, tint, style = stroke)
        drawLine(tint, Offset(w * 0.30f, h * 0.50f), Offset(w * 0.82f, h * 0.50f), stroke.width, StrokeCap.Round)
      }
      DesktopIconKind.SEARCH -> {
        drawCircle(tint, radius = w * 0.26f, center = Offset(w * 0.42f, h * 0.42f), style = stroke)
        drawLine(tint, Offset(w * 0.61f, h * 0.61f), Offset(w * 0.84f, h * 0.84f), stroke.width, StrokeCap.Round)
      }
      DesktopIconKind.LIVE_TV -> {
        drawRoundRect(tint, Offset(w * 0.14f, h * 0.28f), Size(w * 0.72f, h * 0.54f), CornerRadius(w * 0.12f), style = stroke)
        drawLine(tint, Offset(w * 0.36f, h * 0.14f), Offset(w * 0.50f, h * 0.28f), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.64f, h * 0.14f), Offset(w * 0.50f, h * 0.28f), stroke.width, StrokeCap.Round)
      }
      DesktopIconKind.SPARKLE -> {
        val sp = Path().apply {
          moveTo(w * 0.50f, h * 0.12f)
          lineTo(w * 0.60f, h * 0.40f)
          lineTo(w * 0.88f, h * 0.50f)
          lineTo(w * 0.60f, h * 0.60f)
          lineTo(w * 0.50f, h * 0.88f)
          lineTo(w * 0.40f, h * 0.60f)
          lineTo(w * 0.12f, h * 0.50f)
          lineTo(w * 0.40f, h * 0.40f)
          close()
        }
        drawPath(sp, tint)
      }
      DesktopIconKind.FEEDS -> {
        drawRoundRect(tint, Offset(w * 0.20f, h * 0.14f), Size(w * 0.60f, h * 0.72f), CornerRadius(w * 0.1f), style = stroke)
        val tri = Path().apply {
          moveTo(w * 0.42f, h * 0.38f)
          lineTo(w * 0.64f, h * 0.50f)
          lineTo(w * 0.42f, h * 0.62f)
          close()
        }
        drawPath(tri, tint)
      }
      DesktopIconKind.PROFILE -> {
        drawCircle(tint, radius = w * 0.18f, center = Offset(w * 0.50f, h * 0.34f), style = stroke)
        val shoulders = Path().apply {
          moveTo(w * 0.20f, h * 0.82f)
          cubicTo(w * 0.20f, h * 0.58f, w * 0.80f, h * 0.58f, w * 0.80f, h * 0.82f)
        }
        drawPath(shoulders, tint, style = stroke)
      }
      DesktopIconKind.INVITE_T_BADGE -> {
        val tail = Path().apply {
          moveTo(w * 0.42f, h * 0.78f)
          lineTo(w * 0.50f, h * 0.94f)
          lineTo(w * 0.58f, h * 0.78f)
          close()
        }
        drawRoundRect(tint, Offset(w * 0.08f, h * 0.08f), Size(w * 0.84f, h * 0.72f), CornerRadius(w * 0.22f))
        drawPath(tail, tint)
        val ts = w * 0.11f
        drawLine(Color.White, Offset(w * 0.33f, h * 0.34f), Offset(w * 0.67f, h * 0.34f), ts, StrokeCap.Round)
        drawLine(Color.White, Offset(w * 0.50f, h * 0.34f), Offset(w * 0.50f, h * 0.62f), ts, StrokeCap.Round)
      }
      DesktopIconKind.LOCATION_PIN -> {
        val pin = Path().apply {
          moveTo(w * 0.50f, h * 0.88f)
          cubicTo(w * 0.22f, h * 0.62f, w * 0.20f, h * 0.44f, w * 0.20f, h * 0.38f)
          cubicTo(w * 0.20f, h * 0.20f, w * 0.34f, h * 0.10f, w * 0.50f, h * 0.10f)
          cubicTo(w * 0.66f, h * 0.10f, w * 0.80f, h * 0.20f, w * 0.80f, h * 0.38f)
          cubicTo(w * 0.80f, h * 0.44f, w * 0.78f, h * 0.62f, w * 0.50f, h * 0.88f)
          close()
        }
        drawPath(pin, tint, style = stroke)
        drawCircle(tint, radius = w * 0.11f, center = Offset(w * 0.50f, h * 0.38f), style = stroke)
      }
      DesktopIconKind.GRID_2X2 -> {
        val box = w * 0.30f
        val r = CornerRadius(w * 0.06f)
        drawRoundRect(tint, Offset(w * 0.14f, h * 0.14f), Size(box, box), r, style = stroke)
        drawRoundRect(tint, Offset(w * 0.56f, h * 0.14f), Size(box, box), r, style = stroke)
        drawRoundRect(tint, Offset(w * 0.14f, h * 0.56f), Size(box, box), r, style = stroke)
        drawRoundRect(tint, Offset(w * 0.56f, h * 0.56f), Size(box, box), r, style = stroke)
      }
      DesktopIconKind.FILTER_SLIDERS -> {
        listOf(0.28f, 0.50f, 0.72f).forEach { y ->
          drawLine(tint, Offset(w * 0.14f, h * y), Offset(w * 0.86f, h * y), stroke.width, StrokeCap.Round)
        }
        drawCircle(tint, radius = w * 0.09f, center = Offset(w * 0.36f, h * 0.28f))
        drawCircle(tint, radius = w * 0.09f, center = Offset(w * 0.66f, h * 0.50f))
        drawCircle(tint, radius = w * 0.09f, center = Offset(w * 0.42f, h * 0.72f))
      }
      DesktopIconKind.PLAY_OUTLINE -> {
        val tri = Path().apply {
          moveTo(w * 0.26f, h * 0.18f)
          lineTo(w * 0.82f, h * 0.50f)
          lineTo(w * 0.26f, h * 0.82f)
          close()
        }
        drawPath(tri, tint, style = stroke)
      }
      DesktopIconKind.CROWN -> {
        val cr = Path().apply {
          moveTo(w * 0.16f, h * 0.76f)
          lineTo(w * 0.14f, h * 0.32f)
          lineTo(w * 0.36f, h * 0.52f)
          lineTo(w * 0.50f, h * 0.22f)
          lineTo(w * 0.64f, h * 0.52f)
          lineTo(w * 0.86f, h * 0.32f)
          lineTo(w * 0.84f, h * 0.76f)
          close()
        }
        drawPath(cr, tint, style = stroke)
      }
      DesktopIconKind.PERSON_ADD -> {
        drawCircle(tint, radius = w * 0.16f, center = Offset(w * 0.40f, h * 0.34f), style = stroke)
        val arc = Path().apply {
          moveTo(w * 0.16f, h * 0.78f)
          cubicTo(w * 0.16f, h * 0.56f, w * 0.64f, h * 0.56f, w * 0.64f, h * 0.78f)
        }
        drawPath(arc, tint, style = stroke)
        drawLine(tint, Offset(w * 0.78f, h * 0.34f), Offset(w * 0.78f, h * 0.56f), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.67f, h * 0.45f), Offset(w * 0.89f, h * 0.45f), stroke.width, StrokeCap.Round)
      }
      DesktopIconKind.GEAR -> {
        drawCircle(tint, radius = w * 0.16f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
        for (i in 0 until 8) {
          val ang = i * (PI / 4.0)
          val x1 = w * 0.50f + (w * 0.25f * cos(ang)).toFloat()
          val y1 = h * 0.50f + (h * 0.25f * sin(ang)).toFloat()
          val x2 = w * 0.50f + (w * 0.38f * cos(ang)).toFloat()
          val y2 = h * 0.50f + (h * 0.38f * sin(ang)).toFloat()
          drawLine(tint, Offset(x1, y1), Offset(x2, y2), stroke.width * 1.1f, StrokeCap.Round)
        }
      }
      DesktopIconKind.THREE_DASHES -> {
        listOf(0.28f, 0.50f, 0.72f).forEach { y ->
          drawLine(tint, Offset(w * 0.16f, h * y), Offset(w * 0.84f, h * y), stroke.width * 1.05f, StrokeCap.Round)
        }
      }
      DesktopIconKind.CAMERA -> {
        drawRoundRect(tint, Offset(w * 0.14f, h * 0.28f), Size(w * 0.72f, h * 0.52f), CornerRadius(w * 0.12f), style = stroke)
        val top = Path().apply {
          moveTo(w * 0.32f, h * 0.28f)
          lineTo(w * 0.40f, h * 0.16f)
          lineTo(w * 0.60f, h * 0.16f)
          lineTo(w * 0.68f, h * 0.28f)
        }
        drawPath(top, tint, style = stroke)
        drawCircle(tint, radius = w * 0.14f, center = Offset(w * 0.50f, h * 0.54f), style = stroke)
      }
      DesktopIconKind.EDIT_PENCIL -> {
        val body = Path().apply {
          moveTo(w * 0.18f, h * 0.82f)
          lineTo(w * 0.24f, h * 0.58f)
          lineTo(w * 0.68f, h * 0.14f)
          lineTo(w * 0.86f, h * 0.32f)
          lineTo(w * 0.42f, h * 0.76f)
          close()
        }
        drawPath(body, tint, style = stroke)
      }
      DesktopIconKind.SHARE_NODES -> {
        val c1 = Offset(w * 0.74f, h * 0.24f)
        val c2 = Offset(w * 0.24f, h * 0.50f)
        val c3 = Offset(w * 0.74f, h * 0.76f)
        drawLine(tint, c2, c1, stroke.width, StrokeCap.Round)
        drawLine(tint, c2, c3, stroke.width, StrokeCap.Round)
        drawCircle(tint, radius = w * 0.11f, center = c1, style = stroke)
        drawCircle(tint, radius = w * 0.11f, center = c2, style = stroke)
        drawCircle(tint, radius = w * 0.11f, center = c3, style = stroke)
      }
      DesktopIconKind.LOCK -> {
        drawRoundRect(tint, Offset(w * 0.22f, h * 0.44f), Size(w * 0.56f, h * 0.42f), CornerRadius(w * 0.10f), style = stroke)
        val shackle = Path().apply {
          moveTo(w * 0.34f, h * 0.44f)
          lineTo(w * 0.34f, h * 0.30f)
          cubicTo(w * 0.34f, h * 0.12f, w * 0.66f, h * 0.12f, w * 0.66f, h * 0.30f)
          lineTo(w * 0.66f, h * 0.44f)
        }
        drawPath(shackle, tint, style = stroke)
      }
      DesktopIconKind.REPOST -> {
        val top = Path().apply {
          moveTo(w * 0.20f, h * 0.46f)
          lineTo(w * 0.20f, h * 0.28f)
          lineTo(w * 0.78f, h * 0.28f)
        }
        drawPath(top, tint, style = stroke)
        drawLine(tint, Offset(w * 0.66f, h * 0.16f), Offset(w * 0.80f, h * 0.28f), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.66f, h * 0.40f), Offset(w * 0.80f, h * 0.28f), stroke.width, StrokeCap.Round)
        val bot = Path().apply {
          moveTo(w * 0.80f, h * 0.54f)
          lineTo(w * 0.80f, h * 0.72f)
          lineTo(w * 0.22f, h * 0.72f)
        }
        drawPath(bot, tint, style = stroke)
        drawLine(tint, Offset(w * 0.34f, h * 0.60f), Offset(w * 0.20f, h * 0.72f), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.34f, h * 0.84f), Offset(w * 0.20f, h * 0.72f), stroke.width, StrokeCap.Round)
      }
      DesktopIconKind.FRIENDS_GROUP -> {
        drawCircle(tint, radius = w * 0.14f, center = Offset(w * 0.36f, h * 0.34f), style = stroke)
        drawCircle(tint, radius = w * 0.12f, center = Offset(w * 0.68f, h * 0.38f), style = stroke)
        val leftArc = Path().apply {
          moveTo(w * 0.14f, h * 0.78f)
          cubicTo(w * 0.14f, h * 0.56f, w * 0.58f, h * 0.56f, w * 0.58f, h * 0.78f)
        }
        drawPath(leftArc, tint, style = stroke)
      }
      DesktopIconKind.BELL -> {
        val bell = Path().apply {
          moveTo(w * 0.24f, h * 0.68f)
          lineTo(w * 0.30f, h * 0.38f)
          cubicTo(w * 0.30f, h * 0.18f, w * 0.70f, h * 0.18f, w * 0.70f, h * 0.38f)
          lineTo(w * 0.76f, h * 0.68f)
          close()
        }
        drawPath(bell, tint, style = stroke)
        drawLine(tint, Offset(w * 0.18f, h * 0.68f), Offset(w * 0.82f, h * 0.68f), stroke.width, StrokeCap.Round)
        drawCircle(tint, radius = w * 0.08f, center = Offset(w * 0.50f, h * 0.80f))
      }
    }
  }
}

@Composable
private fun DesktopNotificationsInboxModal(
  items: List<DesktopNotificationItem>,
  onDismiss: () -> Unit,
  onMarkAllRead: () -> Unit,
  onSelectItem: (DesktopNotificationItem) -> Unit,
) {
  var filter by remember { mutableStateOf("all") }
  val filtered = remember(items, filter) {
    if (filter == "all") items else items.filter { it.kind.equals(filter, ignoreCase = true) }
  }
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.5f))
      .clickable(onClick = onDismiss),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier = Modifier
        .widthIn(min = 380.dp, max = 560.dp)
        .heightIn(max = 560.dp)
        .clickable(enabled = false) {},
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      shadowElevation = 14.dp,
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(DesktopCrimson)
            .padding(horizontal = 18.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          DesktopSvgIcon(DesktopIconKind.BELL, Color.White, 19.dp)
          Spacer(modifier = Modifier.width(10.dp))
          Text("Notifications", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
          if (items.any { !it.isRead }) {
            TextButton(onClick = onMarkAllRead) {
              Text("Mark all read", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
          }
          Box(modifier = Modifier.clip(CircleShape).clickable(onClick = onDismiss).padding(6.dp)) {
            DesktopSvgIcon(DesktopIconKind.CLOSE, Color.White, 16.dp)
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          listOf("all" to "All", "mention" to "Mentions", "like" to "Likes", "follow" to "Follows").forEach { (key, label) ->
            val sel = filter == key
            Surface(
              onClick = { filter = key },
              shape = RoundedCornerShape(16.dp),
              color = if (sel) DesktopCrimson.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            ) {
              Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                color = if (sel) DesktopCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              )
            }
          }
        }
        HorizontalDivider()
        if (filtered.isEmpty()) {
          Box(modifier = Modifier.fillMaxWidth().padding(36.dp), contentAlignment = Alignment.Center) {
            Text("No notifications yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
          }
        } else {
          LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
            items(filtered, key = { it.id }) { item ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(if (!item.isRead) DesktopCrimson.copy(alpha = 0.05f) else Color.Transparent)
                  .clickable { onSelectItem(item) }
                  .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                val iconKind = when (item.kind.lowercase()) {
                  "follow" -> DesktopIconKind.PERSON_ADD
                  "like" -> DesktopIconKind.HEART_FILLED
                  "mention" -> DesktopIconKind.TAG_PEOPLE
                  else -> DesktopIconKind.CHATS
                }
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DesktopCrimson.copy(alpha = 0.12f)),
                  contentAlignment = Alignment.Center,
                ) {
                  DesktopSvgIcon(iconKind, DesktopCrimson, 17.dp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = item.title.ifBlank { item.actorName.ifBlank { "Telefam" } },
                    fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = if (item.isViewOnce) "Sent a view-once voice message" else item.body,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                  )
                }
                if (!item.isRead) {
                  Spacer(modifier = Modifier.width(8.dp))
                  Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(DesktopCrimson))
                }
              }
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DesktopPostThemeCanvas(
  theme: SharedPostTheme,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit = {},
) {
  val start = Color(theme.startColorArgb)
  val end = Color(theme.endColorArgb)
  val accent = Color(theme.accentColorArgb)

  Box(modifier = modifier.background(Brush.linearGradient(colors = listOf(start, end)))) {
    if (theme.kind == SharedPostThemeKind.IMAGE_SCENE) {
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
          }
          "scene_crimson_silk" -> {
            val wave = Path().apply {
              moveTo(0f, h * 0.32f)
              cubicTo(w * 0.35f, h * 0.08f, w * 0.65f, h * 0.54f, w, h * 0.22f)
              lineTo(w, h)
              lineTo(0f, h)
              close()
            }
            drawPath(wave, color = accent.copy(alpha = 0.20f))
          }
          "scene_dunes" -> {
            drawCircle(accent.copy(alpha = 0.48f), radius = w * 0.14f, center = Offset(w * 0.76f, h * 0.26f))
            val dune = Path().apply {
              moveTo(0f, h * 0.68f)
              cubicTo(w * 0.30f, h * 0.52f, w * 0.70f, h * 0.80f, w, h * 0.60f)
              lineTo(w, h)
              lineTo(0f, h)
              close()
            }
            drawPath(dune, color = Color(0xFF531814).copy(alpha = 0.55f))
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
            listOf(
              Offset(w * 0.16f, h * 0.20f),
              Offset(w * 0.32f, h * 0.14f),
              Offset(w * 0.82f, h * 0.22f),
              Offset(w * 0.72f, h * 0.36f),
              Offset(w * 0.20f, h * 0.76f),
              Offset(w * 0.84f, h * 0.78f),
            ).forEachIndexed { idx, pt ->
              drawCircle(accent.copy(alpha = if (idx % 2 == 0) 0.75f else 0.45f), radius = 3.2f, center = pt)
            }
          }
          "scene_botanical" -> {
            drawCircle(accent.copy(alpha = 0.18f), radius = w * 0.26f, center = Offset(w * 0.12f, h * 0.86f))
            drawCircle(accent.copy(alpha = 0.16f), radius = w * 0.22f, center = Offset(w * 0.88f, h * 0.14f))
          }
          "scene_paper" -> {
            drawRoundRect(
              color = accent.copy(alpha = 0.55f),
              topLeft = Offset(w * 0.05f, h * 0.05f),
              size = Size(w * 0.90f, h * 0.90f),
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

private fun desktopPostTextStyle(font: SharedPostFont, ink: Color): TextStyle = when (font) {
  SharedPostFont.CLASSIC -> TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = ink)
  SharedPostFont.SERIF -> TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 27.sp, color = ink)
  SharedPostFont.HAND -> TextStyle(fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = ink)
  SharedPostFont.TYPEWRITER -> TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = ink)
  SharedPostFont.BOLD -> TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, letterSpacing = 0.6.sp, color = ink)
  SharedPostFont.MONO -> TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, color = ink)
  SharedPostFont.DISPLAY -> TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 28.sp, color = ink)
  SharedPostFont.ROUNDED -> TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, color = ink)
  SharedPostFont.ITALIC -> TextStyle(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 25.sp, color = ink)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DesktopPostComposerOverlay(
  apiClient: DesktopApiClient,
  isDark: Boolean,
  contacts: List<DesktopContactItem>,
  knownHashtags: MutableList<SharedHashtagItem>,
  launchTarget: DesktopPostLaunchTarget,
  onDismiss: () -> Unit,
  onPublished: (DesktopFeedPost) -> Unit,
  onToast: (String) -> Unit,
) {
  val editPost = (launchTarget as? DesktopPostLaunchTarget.EditPost)?.item
  val scope = rememberCoroutineScope()
  var mode by remember(launchTarget) {
    mutableStateOf(
      when (launchTarget) {
        DesktopPostLaunchTarget.CreateNew -> SharedPostMode.TEXT
        is DesktopPostLaunchTarget.PrefillHashtag -> launchTarget.mode
        is DesktopPostLaunchTarget.PrefillSong -> launchTarget.mode
        is DesktopPostLaunchTarget.EditPost -> launchTarget.item.mode
      },
    )
  }
  var textContent by remember(launchTarget) { mutableStateOf(editPost?.textContent.orEmpty()) }
  var caption by remember(launchTarget) { mutableStateOf(editPost?.caption.orEmpty()) }
  var selectedThemeId by remember(launchTarget) { mutableStateOf(editPost?.themeId?.ifBlank { "crimson" } ?: "crimson") }
  var selectedFont by remember(launchTarget) { mutableStateOf(editPost?.fontOption ?: SharedPostFont.CLASSIC) }
  val selectedPhotos = remember(launchTarget) { mutableStateListOf<DesktopSelectedPhoto>() }
  var selectedVideo by remember(launchTarget) { mutableStateOf<DesktopSelectedVideo?>(null) }
  var muteOriginalAudio by remember(launchTarget) { mutableStateOf(editPost?.muteOriginalAudio ?: false) }

  var selectedSong by remember(launchTarget) {
    mutableStateOf(
      when (launchTarget) {
        is DesktopPostLaunchTarget.PrefillSong -> launchTarget.song
        is DesktopPostLaunchTarget.EditPost -> launchTarget.item.song
        else -> null
      },
    )
  }
  val taggedUserIds = remember(launchTarget) { mutableStateListOf<String>() }
  val selectedHashtags = remember(launchTarget) {
    mutableStateListOf<String>().apply {
      when (launchTarget) {
        is DesktopPostLaunchTarget.PrefillHashtag -> add(launchTarget.tag)
        is DesktopPostLaunchTarget.EditPost -> addAll(launchTarget.item.hashtags)
        else -> Unit
      }
    }
  }

  var activeSheet by remember { mutableStateOf<String?>(null) }
  var songQuery by remember { mutableStateOf("") }
  var songResults by remember { mutableStateOf<List<SharedSongTrack>>(emptyList()) }
  var isSearchingSongs by remember { mutableStateOf(false) }
  var tagQuery by remember { mutableStateOf("") }
  var hashtagQuery by remember { mutableStateOf("") }

  LaunchedEffect(activeSheet, songQuery) {
    if (activeSheet != "song") return@LaunchedEffect
    val term = songQuery.trim().ifEmpty { "trending" }
    delay(250L)
    isSearchingSongs = true
    val res = withContext(Dispatchers.IO) { apiClient.searchSongs(term) }
    if (res is DesktopApiOutcome.Ok) songResults = res.data
    isSearchingSongs = false
  }

  LaunchedEffect(activeSheet, hashtagQuery) {
    if (activeSheet != "hashtags") return@LaunchedEffect
    delay(200L)
    val res = withContext(Dispatchers.IO) { apiClient.searchHashtags(hashtagQuery) }
    if (res is DesktopApiOutcome.Ok) {
      knownHashtags.clear()
      knownHashtags.addAll(res.data)
    }
  }

  var isUploading by remember { mutableStateOf(false) }
  var uploadPct by remember { mutableStateOf(0) }
  var uploadedDone by remember { mutableStateOf(false) }

  val canShare = !isUploading && !uploadedDone && when (mode) {
    SharedPostMode.TEXT -> textContent.trim().isNotEmpty()
    SharedPostMode.PHOTOS -> selectedPhotos.isNotEmpty() || editPost != null
    SharedPostMode.VIDEO -> selectedVideo != null || editPost != null
  }
  val activeTheme = SharedPostRules.themes.firstOrNull { it.id == selectedThemeId } ?: SharedPostRules.themes.first()
  val blushBg = if (isDark) DesktopDarkBlush else DesktopBlush

  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(modifier = Modifier.fillMaxSize()) {
      Column(
        modifier = Modifier.fillMaxWidth().background(DesktopCrimson).padding(horizontal = 24.dp, vertical = 14.dp),
      ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
          ) {
            DesktopSvgIcon(DesktopIconKind.CLOSE, Color.White, 20.dp)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (editPost != null) "Edit post" else "New post",
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.weight(1f))
          Surface(
            onClick = {
              if (!canShare) return@Surface
              isUploading = true
              uploadPct = 10
              scope.launch {
                val outcome: DesktopApiOutcome<DesktopFeedPost> = withContext(Dispatchers.IO) {
                  if (editPost != null) {
                    val upd = apiClient.updatePost(
                      postId = editPost.postId,
                      textContent = textContent,
                      caption = caption,
                      themeId = selectedThemeId,
                      fontId = selectedFont.id,
                      song = selectedSong,
                      hashtags = selectedHashtags.toList(),
                      muteOriginalAudio = muteOriginalAudio,
                    )
                    when (upd) {
                      is DesktopApiOutcome.Ok -> DesktopApiOutcome.Ok(
                        editPost.copy(
                          textContent = textContent,
                          caption = caption,
                          themeId = selectedThemeId,
                          fontOption = selectedFont,
                          song = selectedSong,
                          hashtags = selectedHashtags.toList(),
                          muteOriginalAudio = muteOriginalAudio,
                        ),
                      )
                      is DesktopApiOutcome.Fail -> upd
                    }
                  } else {
                    val photoIds = mutableListOf<String>()
                    var contentId = ""
                    var videoMediaId = ""
                    var thumbId = ""

                    if (mode == SharedPostMode.PHOTOS) {
                      selectedPhotos.forEachIndexed { idx, p ->
                        val compressed = compressDesktopPhotoToJpeg(p.sourceFile)
                        val up = apiClient.uploadPostPhoto(compressed)
                        if (up is DesktopApiOutcome.Ok) {
                          photoIds.add(up.data.mediaId)
                          if (contentId.isBlank()) contentId = up.data.contentId
                        } else if (up is DesktopApiOutcome.Fail) {
                          return@withContext up
                        }
                        uploadPct = 20 + ((idx + 1) * 55 / selectedPhotos.size.coerceAtLeast(1))
                      }
                    } else if (mode == SharedPostMode.VIDEO && selectedVideo != null) {
                      val vid = selectedVideo!!
                      val videoBytes = vid.sourceFile.readBytes()
                      val up = apiClient.uploadPostVideo(
                        videoBytes = videoBytes,
                        frameHashesCsv = vid.frameHashes.joinToString(","),
                        durationMs = vid.durationMs,
                        trimStartMs = vid.trimStartMs,
                        trimEndMs = vid.trimEndMs,
                        width = 1080,
                        height = 1920,
                        thumbnailBytes = null,
                      )
                      when (up) {
                        is DesktopApiOutcome.Ok -> {
                          videoMediaId = up.data.mediaId
                          thumbId = up.data.thumbnailMediaId
                          contentId = up.data.contentId.ifBlank { vid.contentId }
                          uploadPct = 82
                        }
                        is DesktopApiOutcome.Fail -> return@withContext up
                      }
                    }

                    val created = apiClient.createPost(
                      clientOpId = UUID.randomUUID().toString(),
                      mode = mode,
                      textContent = textContent,
                      caption = caption,
                      themeId = selectedThemeId,
                      fontId = selectedFont.id,
                      song = selectedSong,
                      taggedUserIds = taggedUserIds.toList(),
                      hashtags = selectedHashtags.toList(),
                      photoMediaIds = photoIds,
                      videoMediaId = videoMediaId,
                      thumbnailMediaId = thumbId,
                      trimStartMs = selectedVideo?.trimStartMs ?: 0L,
                      trimEndMs = selectedVideo?.trimEndMs ?: 0L,
                      muteOriginalAudio = muteOriginalAudio,
                      contentId = contentId,
                    )
                    when (created) {
                      is DesktopApiOutcome.Ok -> DesktopApiOutcome.Ok(
                        DesktopFeedPost(
                          postId = created.data,
                          authorUserId = apiClient.session.email,
                          authorUsername = apiClient.session.username.ifBlank { "you" },
                          authorFullName = apiClient.session.fullName.ifBlank { "You" },
                          isFollowingAuthor = false,
                          isOwner = true,
                          mode = mode,
                          textContent = textContent,
                          caption = caption,
                          themeId = selectedThemeId,
                          fontOption = selectedFont,
                          song = selectedSong,
                          hashtags = selectedHashtags.toList(),
                          muteOriginalAudio = muteOriginalAudio,
                        ),
                      )
                      is DesktopApiOutcome.Fail -> created
                    }
                  }
                }
                isUploading = false
                when (outcome) {
                  is DesktopApiOutcome.Ok -> {
                    uploadPct = 100
                    uploadedDone = true
                    onPublished(outcome.data)
                  }
                  is DesktopApiOutcome.Fail -> onToast(outcome.message)
                }
              }
            },
            shape = RoundedCornerShape(20.dp),
            color = if (canShare) Color.White else Color.White.copy(alpha = 0.72f),
          ) {
            val label = when {
              isUploading -> "$uploadPct%"
              uploadedDone -> "Uploaded"
              editPost != null -> "Save"
              else -> "Share"
            }
            Text(
              text = label,
              color = DesktopCrimson.copy(alpha = if (canShare) 1f else 0.55f),
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            )
          }
        }

        if (editPost == null) {
          Spacer(modifier = Modifier.height(10.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SharedPostMode.entries.forEach { m ->
              val selected = mode == m
              val kind = when (m) {
                SharedPostMode.TEXT -> DesktopIconKind.TEXT_MODE
                SharedPostMode.PHOTOS -> DesktopIconKind.PHOTOS_MODE
                SharedPostMode.VIDEO -> DesktopIconKind.VIDEO_MODE
              }
              Surface(
                onClick = { mode = m },
                shape = RoundedCornerShape(20.dp),
                color = if (selected) Color.White else Color.Transparent,
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  DesktopSvgIcon(kind, if (selected) DesktopCrimson else Color.White, 16.dp)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = m.label,
                    color = if (selected) DesktopCrimson else Color.White,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.5.sp,
                  )
                }
              }
            }
          }
        }
      }

      if (isUploading) {
        LinearProgressIndicator(
          progress = { (uploadPct / 100f).coerceIn(0.05f, 1f) },
          color = DesktopCrimson,
          trackColor = MaterialTheme.colorScheme.outlineVariant,
          modifier = Modifier.fillMaxWidth().height(3.dp),
        )
      }

      Row(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .widthIn(max = 1080.dp)
          .align(Alignment.CenterHorizontally)
          .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
      ) {
        Column(modifier = Modifier.weight(1.05f).verticalScroll(rememberScrollState())) {
          when (mode) {
            SharedPostMode.TEXT -> {
              val ink = if (activeTheme.isLightInk) Color.White else Color(0xFF1C1F26)
              val cardStyle = desktopPostTextStyle(selectedFont, ink).copy(textAlign = TextAlign.Center)
              DesktopPostThemeCanvas(
                theme = activeTheme,
                modifier = Modifier.fillMaxWidth().height(360.dp).clip(RoundedCornerShape(22.dp)),
              ) {
                Box(modifier = Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                  if (textContent.isEmpty()) {
                    Text("What's on your mind?", style = cardStyle.copy(color = ink.copy(alpha = 0.72f)), textAlign = TextAlign.Center)
                  }
                  BasicTextField(
                    value = textContent,
                    onValueChange = { if (it.length <= SharedPostRules.MAX_TEXT_CHARS) textContent = it },
                    textStyle = cardStyle,
                    cursorBrush = SolidColor(ink),
                    modifier = Modifier.fillMaxWidth(),
                  )
                  Text(
                    text = "${textContent.length}/${SharedPostRules.MAX_TEXT_CHARS}",
                    color = ink.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.BottomEnd),
                  )
                }
              }
            }

            SharedPostMode.PHOTOS -> {
              val pickPhotosDesktop = {
                val dlg = FileDialog(null as Frame?, "Select photos", FileDialog.LOAD).apply {
                  isMultipleMode = true
                  setFilenameFilter { _, name ->
                    val low = name.lowercase()
                    low.endsWith(".jpg") || low.endsWith(".jpeg") || low.endsWith(".png") || low.endsWith(".webp")
                  }
                  isVisible = true
                }
                val picked = dlg.files.orEmpty()
                val slots = (SharedPostRules.MAX_PHOTOS - selectedPhotos.size).coerceAtLeast(0)
                picked.take(slots).forEach { f ->
                  val preview = runCatching { SkiaImage.makeFromEncoded(f.readBytes()).toComposeImageBitmap() }.getOrNull()
                  selectedPhotos.add(
                    DesktopSelectedPhoto(
                      id = UUID.randomUUID().toString(),
                      fileName = "${UUID.randomUUID()}.jpg",
                      sourceFile = f,
                      previewBitmap = preview,
                      byteSize = f.length(),
                    ),
                  )
                }
              }

              if (selectedPhotos.isEmpty()) {
                val dashColor = MaterialTheme.colorScheme.outline
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .drawBehind {
                      drawRoundRect(
                        color = dashColor,
                        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f), 0f)),
                        cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                      )
                    }
                    .clickable(onClick = pickPhotosDesktop),
                  contentAlignment = Alignment.Center,
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(blushBg), contentAlignment = Alignment.Center) {
                      DesktopSvgIcon(DesktopIconKind.PHOTOS_MODE, DesktopCrimson, 26.dp)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Add photos", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Up to 10", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                  }
                }
              } else {
                Surface(
                  shape = RoundedCornerShape(20.dp),
                  color = MaterialTheme.colorScheme.surface,
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                      Text("${selectedPhotos.size}/${SharedPostRules.MAX_PHOTOS}", fontWeight = FontWeight.Bold)
                      Spacer(modifier = Modifier.weight(1f))
                      if (selectedPhotos.size < SharedPostRules.MAX_PHOTOS) {
                        Surface(onClick = pickPhotosDesktop, shape = RoundedCornerShape(16.dp), color = DesktopCrimson) {
                          Text("Add", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                      }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                      items(selectedPhotos, key = { it.id }) { p ->
                        Box(
                          modifier = Modifier.size(132.dp).clip(RoundedCornerShape(14.dp)).background(blushBg),
                          contentAlignment = Alignment.Center,
                        ) {
                          if (p.previewBitmap != null) {
                            Image(p.previewBitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                          }
                          Box(
                            modifier = Modifier
                              .align(Alignment.TopEnd)
                              .padding(6.dp)
                              .size(24.dp)
                              .clip(CircleShape)
                              .background(Color.Black.copy(alpha = 0.6f))
                              .clickable { selectedPhotos.removeAll { it.id == p.id } },
                            contentAlignment = Alignment.Center,
                          ) {
                            DesktopSvgIcon(DesktopIconKind.CLOSE, Color.White, 12.dp)
                          }
                        }
                      }
                    }
                  }
                }
              }
            }

            SharedPostMode.VIDEO -> {
              val pickVideoDesktop = {
                val dlg = FileDialog(null as Frame?, "Select video", FileDialog.LOAD).apply {
                  setFilenameFilter { _, name ->
                    val low = name.lowercase()
                    low.endsWith(".mp4") || low.endsWith(".mov") || low.endsWith(".webm")
                  }
                  isVisible = true
                }
                val f = dlg.files?.firstOrNull()
                if (f != null) {
                  if (f.length() > SharedPostRules.MAX_VIDEO_BYTES) {
                    onToast("Video exceeds 200 MB limit")
                  } else {
                    val digest = MessageDigest.getInstance("SHA-256")
                    val bytes = f.inputStream().use { inp ->
                      val buf = ByteArray(8192)
                      val n = inp.read(buf)
                      if (n > 0) buf.copyOf(n) else ByteArray(0)
                    }
                    val frames = (0 until 8).map { i ->
                      val h = digest.digest(bytes + byteArrayOf(i.toByte()))
                      h.take(8).joinToString("") { "%02x".format(it) }
                    }
                    val dur = 15_000L
                    val cidPayload = "tf_vid_fp_v1|${frames.joinToString(",")}|dur=$dur|trim=0:$dur"
                    val cidHex = digest.digest(cidPayload.toByteArray()).take(16).joinToString("") { "%02x".format(it) }
                    selectedVideo = DesktopSelectedVideo(
                      id = UUID.randomUUID().toString(),
                      fileName = "${UUID.randomUUID()}.mp4",
                      sourceFile = f,
                      byteSize = f.length(),
                      durationMs = dur,
                      trimStartMs = 0L,
                      trimEndMs = dur,
                      selectedThumbIndex = 1,
                      frameHashes = frames,
                      contentId = "vid_$cidHex",
                    )
                  }
                }
              }

              val vid = selectedVideo
              if (vid == null) {
                val dashColor = MaterialTheme.colorScheme.outline
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .drawBehind {
                      drawRoundRect(
                        color = dashColor,
                        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f), 0f)),
                        cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                      )
                    }
                    .clickable(onClick = pickVideoDesktop),
                  contentAlignment = Alignment.Center,
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(blushBg), contentAlignment = Alignment.Center) {
                      DesktopSvgIcon(DesktopIconKind.VIDEO_MODE, DesktopCrimson, 26.dp)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Add video", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Up to 200 MB", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                  }
                }
              } else {
                Surface(
                  shape = RoundedCornerShape(20.dp),
                  color = MaterialTheme.colorScheme.surface,
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                      modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF10131A)),
                      contentAlignment = Alignment.Center,
                    ) {
                      DesktopSvgIcon(DesktopIconKind.VIDEO_MODE, Color.White.copy(alpha = 0.7f), 42.dp)
                      Box(
                        modifier = Modifier
                          .align(Alignment.TopEnd)
                          .padding(10.dp)
                          .size(28.dp)
                          .clip(CircleShape)
                          .background(Color.Black.copy(alpha = 0.65f))
                          .clickable { selectedVideo = null },
                        contentAlignment = Alignment.Center,
                      ) {
                        DesktopSvgIcon(DesktopIconKind.CLOSE, Color.White, 14.dp)
                      }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      items(vid.frameHashes.indices.toList()) { idx ->
                        val active = idx == vid.selectedThumbIndex
                        Box(
                          modifier = Modifier
                            .size(width = 52.dp, height = 64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(blushBg)
                            .border(if (active) 2.dp else 1.dp, if (active) DesktopCrimson else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .clickable { selectedVideo = vid.copy(selectedThumbIndex = idx) },
                          contentAlignment = Alignment.Center,
                        ) {
                          Text("${idx + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DesktopCrimson)
                        }
                      }
                    }

                    RangeSlider(
                      value = vid.trimStartMs.toFloat()..vid.trimEndMs.toFloat(),
                      onValueChange = { r ->
                        val s = r.start.toLong().coerceIn(0L, vid.durationMs)
                        val e = r.endInclusive.toLong().coerceIn(s + 500L, vid.durationMs)
                        selectedVideo = vid.copy(trimStartMs = s, trimEndMs = e)
                      },
                      valueRange = 0f..vid.durationMs.toFloat(),
                      colors = SliderDefaults.colors(thumbColor = DesktopCrimson, activeTrackColor = DesktopCrimson),
                    )

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                      DesktopSvgIcon(if (muteOriginalAudio) DesktopIconKind.MUTE else DesktopIconKind.UNMUTE, if (muteOriginalAudio) DesktopCrimson else MaterialTheme.colorScheme.onSurfaceVariant, 20.dp)
                      Spacer(modifier = Modifier.width(10.dp))
                      Text("Mute original audio", modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                      Switch(
                        checked = muteOriginalAudio,
                        onCheckedChange = { muteOriginalAudio = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = DesktopCrimson),
                      )
                    }
                  }
                }
              }
            }
          }
        }

        Column(
          modifier = Modifier.weight(0.95f).verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          if (mode == SharedPostMode.TEXT) {
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              SharedPostRules.themes.forEach { t ->
                val selected = t.id == selectedThemeId
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(if (selected) 2.5.dp else 1.dp, if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .padding(if (selected) 3.dp else 0.dp)
                    .clip(CircleShape)
                    .clickable { selectedThemeId = t.id },
                ) {
                  DesktopPostThemeCanvas(theme = t, modifier = Modifier.fillMaxSize()) {
                    if (selected) {
                      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        DesktopSvgIcon(DesktopIconKind.CHECK, if (t.isLightInk) Color.White else Color(0xFF1C1F26), 16.dp)
                      }
                    }
                  }
                }
              }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              SharedPostFont.entries.forEach { fontOpt ->
                val selected = fontOpt == selectedFont
                Surface(
                  onClick = { selectedFont = fontOpt },
                  shape = RoundedCornerShape(18.dp),
                  color = if (selected) blushBg else MaterialTheme.colorScheme.surface,
                  border = BorderStroke(1.dp, if (selected) DesktopCrimson else MaterialTheme.colorScheme.outlineVariant),
                ) {
                  Text(
                    text = "${if (fontOpt == SharedPostFont.BOLD) "AA" else "Aa"}  ${fontOpt.label}",
                    color = if (selected) DesktopCrimson else MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                  )
                }
              }
            }
          } else {
            Surface(
              shape = RoundedCornerShape(18.dp),
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp),
            ) {
              Box(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                if (caption.isEmpty()) Text("Write a caption...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.5.sp)
                BasicTextField(
                  value = caption,
                  onValueChange = { if (it.length <= SharedPostRules.MAX_CAPTION_CHARS) caption = it },
                  textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.5.sp),
                  cursorBrush = SolidColor(DesktopCrimson),
                  modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                )
                Text(
                  text = "${caption.length}/${SharedPostRules.MAX_CAPTION_CHARS}",
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 12.sp,
                  modifier = Modifier.align(Alignment.BottomEnd),
                )
              }
            }
          }

          Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column {
              DesktopMetadataRow(
                icon = DesktopIconKind.MUSIC,
                blush = blushBg,
                title = selectedSong?.title ?: "Song",
                trailing = selectedSong?.artist ?: "Add",
                highlight = selectedSong != null,
                onClick = { activeSheet = "song" },
              )
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
              DesktopMetadataRow(
                icon = DesktopIconKind.TAG_PEOPLE,
                blush = blushBg,
                title = "Tag people",
                trailing = if (taggedUserIds.isEmpty()) "Add" else "${taggedUserIds.size}",
                highlight = taggedUserIds.isNotEmpty(),
                onClick = { activeSheet = "tag" },
              )
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
              DesktopMetadataRow(
                icon = DesktopIconKind.HASHTAG,
                blush = blushBg,
                title = "Hashtags",
                trailing = if (selectedHashtags.isEmpty()) "Add" else selectedHashtags.joinToString(" ") { "#$it" },
                highlight = selectedHashtags.isNotEmpty(),
                onClick = { activeSheet = "hashtags" },
              )
            }
          }
        }
      }
    }

    if (activeSheet != null) {
      Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable { activeSheet = null },
        contentAlignment = Alignment.Center,
      ) {
        Surface(
          shape = RoundedCornerShape(22.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.width(460.dp).clickable(enabled = false) {},
        ) {
          Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val sheetTitle = when (activeSheet) {
              "song" -> "Song"
              "tag" -> "Tag people"
              else -> "Hashtags"
            }
            Text(sheetTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            when (activeSheet) {
              "tag" -> {
                DesktopSheetSearchBar(tagQuery, { tagQuery = it }, "Search people", isDark)
                val q = tagQuery.trim().lowercase()
                val matches = contacts.filter { q.isEmpty() || it.fullName.lowercase().contains(q) || it.username.lowercase().contains(q) }
                LazyColumn(modifier = Modifier.heightIn(min = 120.dp, max = 260.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  items(matches, key = { it.userId }) { person ->
                    val checked = person.userId in taggedUserIds
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { if (checked) taggedUserIds.remove(person.userId) else taggedUserIds.add(person.userId) }
                        .padding(6.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text(person.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("@${person.username}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.5.sp)
                      }
                      Box(
                        modifier = Modifier
                          .size(20.dp)
                          .clip(CircleShape)
                          .background(if (checked) DesktopCrimson else Color.Transparent)
                          .border(1.5.dp, if (checked) DesktopCrimson else MaterialTheme.colorScheme.outline, CircleShape),
                        contentAlignment = Alignment.Center,
                      ) {
                        if (checked) DesktopSvgIcon(DesktopIconKind.CHECK, Color.White, 12.dp)
                      }
                    }
                  }
                }
              }
              "hashtags" -> {
                DesktopSheetSearchBar(hashtagQuery, { hashtagQuery = it }, "Search hashtags", isDark)
                val clean = SharedPostRules.sanitizeHashtag(hashtagQuery)
                val rows = buildList {
                  addAll(knownHashtags.filter { clean.isEmpty() || it.tag.contains(clean) })
                  if (clean.isNotEmpty() && none { it.tag == clean }) {
                    add(SharedHashtagItem(tag = clean, postCount = 0L))
                  }
                }
                LazyColumn(modifier = Modifier.heightIn(min = 120.dp, max = 260.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  items(rows, key = { it.tag }) { item ->
                    val chosen = item.tag in selectedHashtags
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { if (chosen) selectedHashtags.remove(item.tag) else selectedHashtags.add(item.tag) }
                        .padding(8.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text("#${item.tag}", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                        Text("${item.postCount}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                      }
                      Box(
                        modifier = Modifier
                          .size(20.dp)
                          .clip(CircleShape)
                          .background(if (chosen) DesktopCrimson else Color.Transparent)
                          .border(1.5.dp, if (chosen) DesktopCrimson else MaterialTheme.colorScheme.outline, CircleShape),
                        contentAlignment = Alignment.Center,
                      ) {
                        if (chosen) DesktopSvgIcon(DesktopIconKind.CHECK, Color.White, 12.dp)
                      }
                    }
                  }
                }
              }
              "song" -> {
                var playingTrackId by remember { mutableStateOf<String?>(null) }
                val songPreviewPlayer = remember { DesktopAudioPlayer() }
                DisposableEffect(Unit) {
                  onDispose { songPreviewPlayer.stop() }
                }
                DesktopSheetSearchBar(songQuery, { songQuery = it }, "Search songs", isDark)
                if (isSearchingSongs) {
                  LinearProgressIndicator(color = DesktopCrimson, modifier = Modifier.fillMaxWidth().height(2.dp))
                }
                LazyColumn(modifier = Modifier.heightIn(min = 120.dp, max = 260.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  items(songResults, key = { it.trackId }) { track ->
                    val chosen = selectedSong?.trackId == track.trackId
                    val isPreviewing = playingTrackId == track.trackId
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                          songPreviewPlayer.stop()
                          playingTrackId = null
                          selectedSong = if (chosen) null else track
                          activeSheet = null
                        }
                        .padding(8.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text(track.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                      }
                      if (track.previewUrl.startsWith("https://") || track.previewUrl.startsWith("http://")) {
                        Surface(
                          onClick = {
                            if (isPreviewing) {
                              songPreviewPlayer.stop()
                              playingTrackId = null
                            } else {
                              playingTrackId = track.trackId
                              scope.launch {
                                val cached = withContext(Dispatchers.IO) { apiClient.fetchCachedMediaFile(track.previewUrl) }
                                if (cached != null) {
                                  val started = withContext(Dispatchers.IO) { songPreviewPlayer.playFile(cached, loop = false) }
                                  if (!started) {
                                    playingTrackId = null
                                  }
                                } else {
                                  playingTrackId = null
                                }
                              }
                            }
                          },
                          shape = RoundedCornerShape(12.dp),
                          color = blushBg,
                        ) {
                          Text(
                            if (isPreviewing) "Stop" else "Preview",
                            color = DesktopCrimson,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                          )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                      }
                      if (chosen) DesktopSvgIcon(DesktopIconKind.CHECK, DesktopCrimson, 16.dp)
                    }
                  }
                }
              }
            }
            Button(
              onClick = { activeSheet = null },
              shape = RoundedCornerShape(22.dp),
              colors = ButtonDefaults.buttonColors(containerColor = DesktopCrimson),
              modifier = Modifier.fillMaxWidth().height(44.dp),
            ) {
              Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

private fun compressDesktopPhotoToJpeg(file: File): ByteArray {
  val src = ImageIO.read(file) ?: return file.readBytes()
  val maxDim = 1440
  val scale = if (max(src.width, src.height) > maxDim) {
    maxDim.toDouble() / max(src.width, src.height).toDouble()
  } else {
    1.0
  }
  val w = (src.width * scale).toInt().coerceAtLeast(1)
  val h = (src.height * scale).toInt().coerceAtLeast(1)
  val outImg = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
  val g = outImg.createGraphics()
  g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
  g.drawImage(src, 0, 0, w, h, null)
  g.dispose()
  val baos = ByteArrayOutputStream()
  ImageIO.write(outImg, "jpg", baos)
  return baos.toByteArray()
}

@Composable
private fun DesktopSheetSearchBar(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  isDark: Boolean,
) {
  Surface(
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.5.dp, DesktopCrimson),
    color = if (isDark) DesktopDarkSearch else MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxWidth().height(42.dp),
  ) {
    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
      DesktopSvgIcon(DesktopIconKind.SEARCH, MaterialTheme.colorScheme.onSurfaceVariant, 16.dp)
      Spacer(modifier = Modifier.width(8.dp))
      Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
        if (value.isEmpty()) Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        BasicTextField(
          value = value,
          onValueChange = onValueChange,
          singleLine = true,
          textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
          cursorBrush = SolidColor(DesktopCrimson),
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }
}

@Composable
private fun DesktopMetadataRow(
  icon: DesktopIconKind,
  blush: Color,
  title: String,
  trailing: String,
  highlight: Boolean,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(blush), contentAlignment = Alignment.Center) {
      DesktopSvgIcon(icon, DesktopCrimson, 18.dp)
    }
    Spacer(modifier = Modifier.width(12.dp))
    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
    Text(
      text = trailing,
      color = if (highlight) DesktopCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
      fontSize = 13.5.sp,
      fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Spacer(modifier = Modifier.width(4.dp))
    DesktopSvgIcon(DesktopIconKind.CHEVRON_RIGHT, MaterialTheme.colorScheme.onSurfaceVariant, 16.dp)
  }
}

@Composable
private fun DesktopContactCard(
  contact: DesktopContactItem,
  isDark: Boolean,
  onOpenProfile: () -> Unit = {},
  onToggle: () -> Unit,
) {
  val rel = contact.relationship
  val buttonLabel = when (rel) {
    ContactRelationshipState.FOLLOW -> "Follow"
    ContactRelationshipState.FOLLOWING -> "Following"
    ContactRelationshipState.FOLLOW_BACK -> "Follow back"
    ContactRelationshipState.FRIENDS -> "Friends"
  }
  Surface(
    onClick = onOpenProfile,
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
          .background(if (isDark) Color(0xFF471115) else Color(0xFFFDE8E8)),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = TelefamContactsRules.initialsFor(contact.fullName, contact.username),
          color = if (isDark) Color(0xFFF87171) else DesktopCrimson,
          fontWeight = FontWeight.Bold,
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(contact.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
          text = if (contact.countryDisplay.isNotBlank()) "@${contact.username} · ${contact.countryDisplay}" else "@${contact.username}",
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(18.dp),
        color = if (rel == ContactRelationshipState.FOLLOW || rel == ContactRelationshipState.FOLLOW_BACK) DesktopCrimson else MaterialTheme.colorScheme.background,
        border = if (rel == ContactRelationshipState.FOLLOWING || rel == ContactRelationshipState.FRIENDS) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
      ) {
        Text(
          text = buttonLabel,
          color = if (rel == ContactRelationshipState.FOLLOW || rel == ContactRelationshipState.FOLLOW_BACK) Color.White else MaterialTheme.colorScheme.onSurface,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
      }
    }
  }
}

@Composable
private fun DesktopTricolorSpinner(size: Dp = 42.dp) {
  val trans = rememberInfiniteTransition()
  val deg by trans.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(tween(950, easing = LinearEasing), RepeatMode.Restart),
  )
  Canvas(modifier = Modifier.size(size).rotate(deg)) {
    val cx = this.size.width / 2f
    val cy = this.size.height / 2f
    val orbit = this.size.minDimension * 0.28f
    val dotR = this.size.minDimension * 0.13f
    val palette = listOf(Color(0xFFC80A0F), Color.White, Color(0xFF16A34A))
    palette.forEachIndexed { idx, col ->
      val rad = (-90.0 + idx * 120.0) * PI / 180.0
      drawCircle(
        color = col,
        radius = dotR,
        center = Offset(cx + (cos(rad) * orbit).toFloat(), cy + (sin(rad) * orbit).toFloat()),
      )
    }
  }
}

@Composable
private fun DesktopAudioBars(isPlaying: Boolean) {
  val trans = rememberInfiniteTransition()
  val p1 by trans.animateFloat(0.3f, 1f, infiniteRepeatable(tween(420), RepeatMode.Reverse))
  val p2 by trans.animateFloat(1f, 0.3f, infiniteRepeatable(tween(360), RepeatMode.Reverse))
  val p3 by trans.animateFloat(0.45f, 0.95f, infiniteRepeatable(tween(500), RepeatMode.Reverse))
  Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(14.dp)) {
    listOf(p1, p2, p3).forEach { factor ->
      val h = if (isPlaying) 14f * factor else 6f
      Box(modifier = Modifier.width(2.5.dp).height(h.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
    }
  }
}

private class DesktopAudioPlayer {
  @Volatile
  private var process: Process? = null

  fun playFile(file: File, loop: Boolean = true): Boolean {
    stop()
    val commands = buildList {
      val loopArgs = if (loop) listOf("-loop", "0") else emptyList()
      add(listOf("ffplay", "-nodisp", "-autoexit", "-loglevel", "quiet") + loopArgs + listOf(file.absolutePath))
      val os = System.getProperty("os.name").orEmpty().lowercase()
      if (os.contains("mac")) {
        add(listOf("afplay", file.absolutePath))
      } else if (os.contains("linux")) {
        add(listOf("mpv", "--no-video", "--really-quiet", file.absolutePath))
      }
    }
    for (cmd in commands) {
      val proc = runCatching {
        ProcessBuilder(cmd)
          .redirectErrorStream(true)
          .start()
      }.getOrNull()
      if (proc != null) {
        process = proc
        return true
      }
    }
    return false
  }

  fun stop() {
    runCatching { process?.destroyForcibly() }
    process = null
  }
}

@Composable
private fun DesktopVideoPlaybackStage(
  apiClient: DesktopApiClient,
  videoUrl: String,
  thumbnailUrl: String,
  isActiveAndPlaying: Boolean,
  isFast2x: Boolean,
  isMuted: Boolean,
  modifier: Modifier = Modifier,
) {
  var currentFrame by remember(videoUrl) { mutableStateOf<ImageBitmap?>(null) }
  var cachedVideoFile by remember(videoUrl) { mutableStateOf<File?>(null) }
  var isBuffering by remember(videoUrl) { mutableStateOf(false) }

  LaunchedEffect(videoUrl) {
    currentFrame = null
    cachedVideoFile = null
    if (videoUrl.isBlank()) return@LaunchedEffect
    isBuffering = true
    cachedVideoFile = withContext(Dispatchers.IO) { apiClient.fetchCachedMediaFile(videoUrl) }
    isBuffering = false
  }

  // Video audio track playback (respecting muteOriginalAudio / attached song)
  val videoAudioPlayer = remember { DesktopAudioPlayer() }
  DisposableEffect(videoAudioPlayer) {
    onDispose { videoAudioPlayer.stop() }
  }
  LaunchedEffect(cachedVideoFile, isActiveAndPlaying, isMuted) {
    val vf = cachedVideoFile
    if (vf != null && isActiveAndPlaying && !isMuted) {
      withContext(Dispatchers.IO) { videoAudioPlayer.playFile(vf, loop = true) }
    } else {
      videoAudioPlayer.stop()
    }
  }

  // Decode video frames via ffmpeg MJPEG pipe into Compose ImageBitmap preserving original aspect ratio via ContentScale.Fit
  LaunchedEffect(cachedVideoFile, isActiveAndPlaying, isFast2x) {
    val vf = cachedVideoFile ?: return@LaunchedEffect
    if (!isActiveAndPlaying) return@LaunchedEffect
    withContext(Dispatchers.IO) {
      val fpsFilter = if (isFast2x) "fps=40,setpts=0.5*PTS" else "fps=24"
      while (isActive) {
        val proc = runCatching {
          ProcessBuilder(
            "ffmpeg",
            "-re",
            "-i",
            vf.absolutePath,
            "-vf",
            "scale='min(960,iw)':-2,$fpsFilter",
            "-f",
            "image2pipe",
            "-vcodec",
            "mjpeg",
            "-q:v",
            "5",
            "-",
          ).start()
        }.getOrNull() ?: break
        try {
          val stream = proc.inputStream
          val buf = ByteArray(4096)
          val frameBuf = ByteArrayOutputStream()
          var prevByte = -1
          var inFrame = false
          while (isActive) {
            val n = stream.read(buf)
            if (n <= 0) break
            for (i in 0 until n) {
              val b = buf[i].toInt() and 0xFF
              if (!inFrame) {
                if (prevByte == 0xFF && b == 0xD8) {
                  inFrame = true
                  frameBuf.reset()
                  frameBuf.write(0xFF)
                  frameBuf.write(0xD8)
                }
              } else {
                frameBuf.write(b)
                if (prevByte == 0xFF && b == 0xD9) {
                  val bytes = frameBuf.toByteArray()
                  val img = runCatching { SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
                  if (img != null) {
                    currentFrame = img
                  }
                  inFrame = false
                }
              }
              prevByte = b
            }
          }
        } finally {
          runCatching { proc.destroyForcibly() }
        }
        delay(80L)
      }
    }
  }

  Box(modifier = modifier.background(Color(0xFF07080B)), contentAlignment = Alignment.Center) {
    // 1) Full-screen blurred ambient background behind FIT video bars
    if (thumbnailUrl.isNotBlank()) {
      DesktopRemoteCachedImage(
        apiClient = apiClient,
        pathOrUrl = thumbnailUrl,
        modifier = Modifier.fillMaxSize().blur(28.dp),
        contentScale = ContentScale.Crop,
        fallback = {},
      )
      Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.44f)))
    }

    // 2) Full-screen FIT decoded video or crisp FIT thumbnail fallback (never cropped, never stretched)
    val frame = currentFrame
    if (frame != null) {
      Image(
        bitmap = frame,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize(),
      )
    } else if (thumbnailUrl.isNotBlank()) {
      DesktopRemoteCachedImage(
        apiClient = apiClient,
        pathOrUrl = thumbnailUrl,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit,
        fallback = {},
      )
    }

    if (isBuffering && isActiveAndPlaying) {
      DesktopTricolorSpinner(size = 38.dp)
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DesktopReusableFeedStage(
  apiClient: DesktopApiClient,
  items: List<DesktopFeedPost>,
  initialIndex: Int,
  activeTab: SharedFeedTab,
  isStandaloneViewer: Boolean,
  isHostActive: Boolean = true,
  contentPreferences: DesktopContentPreferences = DesktopContentPreferences(),
  onSelectTab: (SharedFeedTab) -> Unit,
  onLiveClick: () -> Unit,
  onSearchClick: () -> Unit,
  onBackClick: () -> Unit,
  onCommentClick: () -> Unit,
  onToggleAuthorFollow: (String, Boolean) -> Unit,
  onToggleLike: (String) -> Unit,
  onToggleFavourite: (String) -> Unit,
  onToggleRepost: (String) -> Unit,
  onCopyShareLink: (DesktopFeedPost) -> Unit,
  onDownloadVideo: (DesktopFeedPost) -> Unit,
  onNotInterested: (String) -> Unit,
  onReport: (String, String) -> Unit,
  onBlock: (String, String) -> Unit,
  onMuteAuthor: (String, String) -> Unit = { _, _ -> },
  onEditOwnerPost: (DesktopFeedPost) -> Unit,
  onChangePostPrivacy: (String, String) -> Unit = { _, _ -> },
  onDeleteOwnerPost: (String) -> Unit,
  onTapHashtag: (String, SharedPostMode) -> Unit,
  onTapAudio: (SharedSongTrack, SharedPostMode) -> Unit,
  onOpenAuthorProfile: (String) -> Unit = {},
  onLoadMore: () -> Unit,
  onCreateFirstPost: () -> Unit,
  onToast: (String) -> Unit,
) {
  var currentIndex by remember(initialIndex, items.size) {
    mutableStateOf(initialIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)))
  }
  var isPaused by remember(currentIndex, contentPreferences.autoplayVideos) { mutableStateOf(!contentPreferences.autoplayVideos) }
  var isFast2x by remember(currentIndex) { mutableStateOf(false) }
  var showDoubleHeart by remember { mutableStateOf(false) }
  var activeDialog by remember { mutableStateOf<String?>(null) }

  val songPlayer = remember { DesktopAudioPlayer() }
  DisposableEffect(songPlayer) {
    onDispose { songPlayer.stop() }
  }

  LaunchedEffect(currentIndex, items.size) {
    if (items.isNotEmpty() && currentIndex >= items.size - 3) {
      onLoadMore()
    }
  }

  val activePost = items.getOrNull(currentIndex)
  LaunchedEffect(activePost?.postId, activePost?.song?.previewUrl, isHostActive, isPaused) {
    val songUrl = activePost?.song?.previewUrl?.trim().orEmpty()
    if (songUrl.isNotBlank() && isHostActive && !isPaused) {
      val file = withContext(Dispatchers.IO) { apiClient.fetchCachedMediaFile(songUrl) }
      if (file != null) {
        withContext(Dispatchers.IO) { songPlayer.playFile(file, loop = true) }
      } else {
        songPlayer.stop()
      }
    } else {
      songPlayer.stop()
    }
  }

  LaunchedEffect(showDoubleHeart) {
    if (showDoubleHeart) {
      delay(650L)
      showDoubleHeart = false
    }
  }

  Box(modifier = Modifier.fillMaxSize().background(Color(0xFF07080B))) {
    // Top Safe Overlay Bar matching reference screenshot
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 18.dp, vertical = 14.dp)
        .align(Alignment.TopCenter),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (isStandaloneViewer) {
        Surface(
          onClick = onBackClick,
          shape = CircleShape,
          color = Color.White.copy(alpha = 0.14f),
        ) {
          Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) {
            DesktopSvgIcon(DesktopIconKind.BACK_ARROW, Color.White, 19.dp)
          }
        }
      } else {
        Surface(
          onClick = onLiveClick,
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.3.dp, Color.White.copy(alpha = 0.85f)),
          color = Color.Transparent,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            DesktopSvgIcon(DesktopIconKind.LIVE_TV, Color.White, 16.dp)
            Text("LIVE", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
          }
        }
      }

      Spacer(modifier = Modifier.weight(1f))

      if (!isStandaloneViewer) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
          listOf(SharedFeedTab.FOLLOWING, SharedFeedTab.FOR_YOU).forEach { tab ->
            val selected = tab == activeTab
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onSelectTab(tab) }) {
              Text(
                text = tab.label,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.65f),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 16.sp,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Box(
                modifier = Modifier
                  .width(28.dp)
                  .height(3.dp)
                  .background(if (selected) Color.White else Color.Transparent, RoundedCornerShape(2.dp)),
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.weight(1f))

      Surface(
        onClick = onSearchClick,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.14f),
      ) {
        Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) {
          DesktopSvgIcon(DesktopIconKind.SEARCH, Color.White, 19.dp)
        }
      }
    }

    val current = items.getOrNull(currentIndex)
    if (current == null) {
      Column(modifier = Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No videos yet", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Create a post to start watching in Feeds.", color = Color.White.copy(alpha = 0.72f), fontSize = 13.5.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onCreateFirstPost, colors = ButtonDefaults.buttonColors(containerColor = DesktopCrimson)) {
          Text("Post", color = Color.White)
        }
      }
      return@Box
    }

    val hasSongAudio = !current.song?.previewUrl.isNullOrBlank()
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(top = 60.dp, bottom = 12.dp, start = 14.dp, end = 14.dp)
        .pointerInput(current.postId, hasSongAudio) {
          detectTapGestures(
            onTap = {
              if (current.mode == SharedPostMode.VIDEO || hasSongAudio) {
                isPaused = !isPaused
              }
            },
            onDoubleTap = {
              showDoubleHeart = true
              if (!current.isLiked) onToggleLike(current.postId)
            },
            onLongPress = {
              if (current.mode == SharedPostMode.VIDEO) isFast2x = true
            },
            onPress = {
              try {
                awaitRelease()
              } finally {
                isFast2x = false
              }
            },
          )
        },
    ) {
      when (current.mode) {
        SharedPostMode.VIDEO -> {
          DesktopVideoPlaybackStage(
            apiClient = apiClient,
            videoUrl = current.videoUrl,
            thumbnailUrl = current.thumbnailUrl,
            isActiveAndPlaying = isHostActive && !isPaused,
            isFast2x = isFast2x,
            isMuted = current.muteOriginalAudio || hasSongAudio,
            modifier = Modifier.fillMaxSize(),
          )
        }

        SharedPostMode.PHOTOS -> {
          var photoIdx by remember(current.postId) { mutableStateOf(0) }
          val activePhotoUrl = current.photoUrls.getOrNull(photoIdx) ?: current.thumbnailUrl
          Box(modifier = Modifier.fillMaxSize().background(Color(0xFF07080B)), contentAlignment = Alignment.Center) {
            if (activePhotoUrl.isNotBlank()) {
              DesktopRemoteCachedImage(
                apiClient = apiClient,
                pathOrUrl = activePhotoUrl,
                modifier = Modifier.fillMaxSize().blur(28.dp),
                contentScale = ContentScale.Crop,
                fallback = {},
              )
              Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.44f)))
              DesktopRemoteCachedImage(
                apiClient = apiClient,
                pathOrUrl = activePhotoUrl,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                fallback = {},
              )
            }
            if (current.photoUrls.size > 1) {
              Row(
                modifier = Modifier.align(Alignment.CenterStart).padding(16.dp),
              ) {
                if (photoIdx > 0) {
                  Surface(onClick = { photoIdx-- }, shape = CircleShape, color = Color.Black.copy(alpha = 0.5f)) {
                    Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                      DesktopSvgIcon(DesktopIconKind.BACK_ARROW, Color.White, 18.dp)
                    }
                  }
                }
              }
              Row(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 82.dp),
              ) {
                if (photoIdx < current.photoUrls.lastIndex) {
                  Surface(onClick = { photoIdx++ }, shape = CircleShape, color = Color.Black.copy(alpha = 0.5f)) {
                    Box(modifier = Modifier.size(36.dp).rotate(180f), contentAlignment = Alignment.Center) {
                      DesktopSvgIcon(DesktopIconKind.BACK_ARROW, Color.White, 18.dp)
                    }
                  }
                }
              }
              Row(
                modifier = Modifier
                  .align(Alignment.BottomCenter)
                  .padding(bottom = 14.dp)
                  .background(Color.Black.copy(alpha = 0.48f), RoundedCornerShape(10.dp))
                  .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                repeat(current.photoUrls.size) { d ->
                  Box(
                    modifier = Modifier
                      .size(6.dp)
                      .clip(CircleShape)
                      .background(if (photoIdx == d) Color.White else Color.White.copy(alpha = 0.4f)),
                  )
                }
              }
            }
          }
        }

        SharedPostMode.TEXT -> {
          val theme = SharedPostRules.themes.firstOrNull { it.id == current.themeId } ?: SharedPostRules.themes.first()
          DesktopPostThemeCanvas(
            theme = theme,
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth().height(360.dp).align(Alignment.Center).clip(RoundedCornerShape(22.dp)),
          ) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
              val ink = if (theme.isLightInk) Color.White else Color(0xFF1A1C1E)
              DesktopSmartInteractiveText(
                text = current.textContent.ifBlank { current.caption.ifBlank { "@${current.authorUsername}" } },
                source = "feed",
                color = ink,
                linkColor = if (theme.isLightInk) Color(0xFFFFD6DF) else DesktopCrimson,
                style = desktopPostTextStyle(current.fontOption, ink).copy(textAlign = TextAlign.Center),
                showOtpPills = false,
              )
            }
          }
        }
      }

      if (isPaused && (current.mode == SharedPostMode.VIDEO || hasSongAudio)) {
        Surface(color = Color.Black.copy(alpha = 0.6f), shape = CircleShape, modifier = Modifier.align(Alignment.Center)) {
          Text("Paused", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
        }
      }
      if (isFast2x && current.mode == SharedPostMode.VIDEO) {
        Surface(color = Color.Black.copy(alpha = 0.65f), shape = RoundedCornerShape(14.dp), modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp)) {
          Text(">> 2x", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
        }
      }
      if (showDoubleHeart && !contentPreferences.reducedMotion) {
        Box(modifier = Modifier.align(Alignment.Center)) {
          DesktopSvgIcon(DesktopIconKind.HEART_FILLED, Color(0xFFFF5A76), 74.dp)
        }
      }

      if (contentPreferences.captionsEnabled) {
        val ccText = current.caption.ifBlank { current.textContent }.trim()
        if (ccText.isNotEmpty()) {
          Surface(
            color = Color.Black.copy(alpha = 0.72f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 148.dp, start = 24.dp, end = 86.dp),
          ) {
            Text(
              text = ccText,
              color = Color.White,
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
          }
        }
      }

      // Bottom-Left Metadata matching screenshot
      Column(
        modifier = Modifier.align(Alignment.BottomStart).widthIn(max = 460.dp).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        val badgeText = when {
          current.repostedByUsername.isNotEmpty() -> "Reposted · @${current.repostedByUsername}"
          current.isReposted -> "Reposted"
          current.recommendationLabel.isNotBlank() -> current.recommendationLabel
          else -> ""
        }
        if (badgeText.isNotEmpty()) {
          Surface(color = Color(0xFF14181F).copy(alpha = 0.78f), shape = RoundedCornerShape(14.dp)) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(DesktopCrimson), contentAlignment = Alignment.Center) {
                DesktopSvgIcon(DesktopIconKind.SPARKLE, Color.White, 10.dp)
              }
              Text(badgeText, color = Color.White, fontSize = 12.5.sp)
            }
          }
        }
        Text(
          text = current.authorUsername,
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 17.sp,
          modifier = Modifier.clickable { onOpenAuthorProfile(current.authorUserId) },
        )
        val captionText = current.caption.ifBlank {
          if (current.mode != SharedPostMode.TEXT) current.textContent else ""
        }
        if (captionText.isNotEmpty()) {
          var isCaptionExpanded by remember(current.postId) { mutableStateOf(false) }
          var hasOverflow by remember(current.postId) { mutableStateOf(false) }
          val showToggle = hasOverflow || isCaptionExpanded || captionText.length > 85 || captionText.contains('\n')
          Column(modifier = Modifier.fillMaxWidth()) {
            if (isCaptionExpanded) {
              Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                DesktopSmartInteractiveText(
                  text = captionText,
                  source = "feed",
                  color = Color.White.copy(alpha = 0.94f),
                  linkColor = Color(0xFFFFD6DF),
                  style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 18.5.sp),
                )
                DesktopLinkPreviewStack(
                  text = captionText,
                  source = "feed",
                  isDarkBubble = true,
                )
              }
            } else {
              DesktopSmartInteractiveText(
                text = captionText,
                source = "feed",
                color = Color.White.copy(alpha = 0.94f),
                linkColor = Color(0xFFFFD6DF),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 18.5.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                showOtpPills = false,
                onTextLayout = { res -> if (res.hasVisualOverflow) hasOverflow = true },
              )
            }
            if (showToggle) {
              Text(
                text = if (isCaptionExpanded) "Read less" else "Read more",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .clickable { isCaptionExpanded = !isCaptionExpanded }
                  .padding(top = 3.dp, bottom = 2.dp, end = 8.dp),
              )
            }
          }
        }
        if (current.hashtags.isNotEmpty()) {
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            current.hashtags.forEach { tag ->
              Text(
                text = "#$tag",
                color = Color(0xFFFF4D67),
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                modifier = Modifier.clickable { onTapHashtag(tag, current.mode) },
              )
            }
          }
        }
        Row(
          modifier = Modifier.clickable {
            val song = current.song ?: SharedSongTrack("orig_${current.postId}", "Original sound", current.authorUsername)
            onTapAudio(song, current.mode)
          },
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          DesktopSvgIcon(DesktopIconKind.MUSIC, Color.White, 15.dp)
          Text(
            text = current.song?.let { "${it.title} · ${it.artist}" } ?: "Original sound",
            color = Color.White,
            fontSize = 13.5.sp,
          )
          DesktopAudioBars(isPlaying = isHostActive && !isPaused)
        }
      }

      // Bottom-Right Action Rail with SVG icons & creator + follow badge
      Column(
        modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (items.size > 1) {
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(onClick = { if (currentIndex > 0) currentIndex-- }, shape = CircleShape, color = Color.White.copy(alpha = 0.16f)) {
              Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                DesktopSvgIcon(DesktopIconKind.CHEVRON_UP, Color.White, 16.dp)
              }
            }
            Surface(onClick = { if (currentIndex < items.lastIndex) currentIndex++ }, shape = CircleShape, color = Color.White.copy(alpha = 0.16f)) {
              Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                DesktopSvgIcon(DesktopIconKind.CHEVRON_DOWN, Color.White, 16.dp)
              }
            }
          }
        }

        // Author avatar + Crimson + follow button matching screenshot
        Box(modifier = Modifier.size(54.dp), contentAlignment = Alignment.TopCenter) {
          Box(
            modifier = Modifier
              .size(46.dp)
              .clip(CircleShape)
              .background(Color(0xFF1F2430))
              .border(1.5.dp, Color.White, CircleShape)
              .clickable { onOpenAuthorProfile(current.authorUserId) },
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = TelefamContactsRules.initialsFor(current.authorFullName, current.authorUsername),
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
            )
            if (current.authorAvatarUrl.isNotBlank()) {
              DesktopRemoteCachedImage(
                apiClient = apiClient,
                pathOrUrl = current.authorAvatarUrl,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                fallback = {},
              )
            }
          }
          if (!current.isOwner && !current.isFollowingAuthor) {
            Box(
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(20.dp)
                .clip(CircleShape)
                .background(DesktopCrimson)
                .clickable { onToggleAuthorFollow(current.authorUserId, current.isFollowingAuthor) },
              contentAlignment = Alignment.Center,
            ) {
              DesktopSvgIcon(DesktopIconKind.PLUS, Color.White, 12.dp)
            }
          }
        }

        DesktopFeedSvgRailAction(
          icon = if (current.isLiked) DesktopIconKind.HEART_FILLED else DesktopIconKind.HEART,
          tint = if (current.isLiked) Color(0xFFFF5A76) else Color.White,
          label = SharedFeedMath.formatCompactCount(current.likesCount),
          onClick = { onToggleLike(current.postId) },
        )
        DesktopFeedSvgRailAction(
          icon = DesktopIconKind.COMMENT,
          tint = Color.White,
          label = SharedFeedMath.formatCompactCount(current.commentsCount),
          onClick = onCommentClick,
        )
        DesktopFeedSvgRailAction(
          icon = DesktopIconKind.SHARE_ARROW,
          tint = DesktopCrimson,
          label = SharedFeedMath.formatCompactCount(current.sharesCount),
          onClick = { activeDialog = "share" },
        )
        DesktopFeedSvgRailAction(
          icon = if (current.isFavourited) DesktopIconKind.BOOKMARK_FILLED else DesktopIconKind.BOOKMARK,
          tint = DesktopCrimson,
          label = SharedFeedMath.formatCompactCount(current.favouritesCount),
          onClick = { onToggleFavourite(current.postId) },
        )
        Surface(onClick = { activeDialog = "more" }, shape = CircleShape, color = Color.White.copy(alpha = 0.14f)) {
          Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            DesktopSvgIcon(DesktopIconKind.MORE_DOTS, Color.White, 18.dp)
          }
        }
      }
    }

    if (activeDialog != null) {
      Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable { activeDialog = null },
        contentAlignment = Alignment.Center,
      ) {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.width(340.dp).clickable(enabled = false) {},
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (activeDialog) {
              "share" -> {
                Text("Share", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                DesktopDialogButton("Copy link") { onCopyShareLink(current); activeDialog = null }
                DesktopDialogButton(if (current.isReposted) "Remove repost" else "Repost") { onToggleRepost(current.postId); activeDialog = null }
              }
              "report" -> {
                Text("Report", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                listOf("Spam", "Harassment", "Inappropriate content", "Misinformation").forEach { reason ->
                  DesktopDialogButton(reason) { onReport(current.postId, reason); activeDialog = null }
                }
              }
              "privacy" -> {
                Text("Post privacy", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                val activePriv = SharedPostPrivacy.fromApi(current.privacy)
                SharedPostPrivacy.entries.forEach { privOpt ->
                  val mark = if (privOpt == activePriv) " ✓" else ""
                  DesktopDialogButton("${privOpt.label}$mark") {
                    activeDialog = null
                    onChangePostPrivacy(current.postId, privOpt.apiValue)
                  }
                }
              }
              else -> {
                Text("Options", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                DesktopDialogButton(if (current.isReposted) "Remove repost" else "Repost") { onToggleRepost(current.postId); activeDialog = null }
                DesktopDialogButton("Download") { onDownloadVideo(current); activeDialog = null }
                DesktopDialogButton("Not interested") { onNotInterested(current.postId); activeDialog = null }
                DesktopDialogButton("Report") { activeDialog = "report" }
                if (current.isOwner) {
                  val activePriv = SharedPostPrivacy.fromApi(current.privacy)
                  DesktopDialogButton("Privacy (${activePriv.label})") { activeDialog = "privacy" }
                  DesktopDialogButton("Edit video") { activeDialog = null; onEditOwnerPost(current) }
                  DesktopDialogButton("Delete video") { onDeleteOwnerPost(current.postId); activeDialog = null }
                } else {
                  DesktopDialogButton("Mute @${current.authorUsername}") { onMuteAuthor(current.postId, current.authorUserId); activeDialog = null }
                  DesktopDialogButton("Block @${current.authorUsername}") { onBlock(current.postId, current.authorUserId); activeDialog = null }
                }
              }
            }
            DesktopDialogButton("Cancel") { activeDialog = null }
          }
        }
      }
    }
  }
}

@Composable
private fun DesktopFeedSvgRailAction(
  icon: DesktopIconKind,
  tint: Color,
  label: String,
  onClick: () -> Unit,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 4.dp),
  ) {
    DesktopSvgIcon(icon, tint, 26.dp)
    Spacer(modifier = Modifier.height(2.dp))
    Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
  }
}

@Composable
private fun DesktopDialogButton(label: String, onClick: () -> Unit) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.background,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(12.dp))
  }
}

@Composable
private fun DesktopFeedSearchOverlay(
  apiClient: DesktopApiClient,
  results: List<DesktopFeedPost>,
  shortcuts: List<DesktopSearchShortcut>,
  query: String,
  activeShortcut: String,
  contentPreferences: DesktopContentPreferences = DesktopContentPreferences(),
  onQueryChanged: (String) -> Unit,
  onShortcutSelected: (String) -> Unit,
  onClose: () -> Unit,
  onCommentClick: () -> Unit,
  onToggleAuthorFollow: (String, Boolean) -> Unit,
  onToggleLike: (String) -> Unit,
  onToggleFavourite: (String) -> Unit,
  onToggleRepost: (String) -> Unit,
  onCopyShareLink: (DesktopFeedPost) -> Unit,
  onDownloadVideo: (DesktopFeedPost) -> Unit,
  onNotInterested: (String) -> Unit,
  onReport: (String, String) -> Unit,
  onBlock: (String, String) -> Unit,
  onMuteAuthor: (String, String) -> Unit = { _, _ -> },
  onEditOwnerPost: (DesktopFeedPost) -> Unit,
  onChangePostPrivacy: (String, String) -> Unit = { _, _ -> },
  onDeleteOwnerPost: (String) -> Unit,
  onTapHashtag: (String, SharedPostMode) -> Unit,
  onTapAudio: (SharedSongTrack, SharedPostMode) -> Unit,
  onOpenAuthorProfile: (String) -> Unit = {},
  onToast: (String) -> Unit,
) {
  var viewerStartIndex by remember { mutableStateOf<Int?>(null) }

  val openViewerIdx = viewerStartIndex
  if (openViewerIdx != null) {
    DesktopReusableFeedStage(
      apiClient = apiClient,
      items = results,
      initialIndex = openViewerIdx,
      activeTab = SharedFeedTab.FOR_YOU,
      isStandaloneViewer = true,
      isHostActive = true,
      contentPreferences = contentPreferences,
      onSelectTab = {},
      onLiveClick = {},
      onSearchClick = { viewerStartIndex = null },
      onBackClick = { viewerStartIndex = null },
      onCommentClick = onCommentClick,
      onToggleAuthorFollow = onToggleAuthorFollow,
      onToggleLike = onToggleLike,
      onToggleFavourite = onToggleFavourite,
      onToggleRepost = onToggleRepost,
      onCopyShareLink = onCopyShareLink,
      onDownloadVideo = onDownloadVideo,
      onNotInterested = onNotInterested,
      onReport = onReport,
      onBlock = onBlock,
      onMuteAuthor = onMuteAuthor,
      onEditOwnerPost = onEditOwnerPost,
      onChangePostPrivacy = onChangePostPrivacy,
      onDeleteOwnerPost = onDeleteOwnerPost,
      onTapHashtag = onTapHashtag,
      onTapAudio = onTapAudio,
      onOpenAuthorProfile = onOpenAuthorProfile,
      onLoadMore = {},
      onCreateFirstPost = {},
      onToast = onToast,
    )
    return
  }

  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(modifier = Modifier.fillMaxSize()) {
      Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Surface(
            onClick = onClose,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
              DesktopSvgIcon(DesktopIconKind.BACK_ARROW, MaterialTheme.colorScheme.onSurface, 18.dp)
            }
          }
          Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.weight(1f).height(44.dp),
          ) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
              DesktopSvgIcon(DesktopIconKind.SEARCH, MaterialTheme.colorScheme.onSurfaceVariant, 17.dp)
              Spacer(modifier = Modifier.width(8.dp))
              Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) Text("Search videos, hashtags, audio...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                BasicTextField(
                  value = query,
                  onValueChange = { onQueryChanged(it.take(80)) },
                  singleLine = true,
                  textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                  cursorBrush = SolidColor(DesktopCrimson),
                  modifier = Modifier.fillMaxWidth(),
                )
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item {
            val sel = activeShortcut.isEmpty()
            Surface(onClick = { onShortcutSelected("") }, shape = RoundedCornerShape(18.dp), color = if (sel) DesktopCrimson else MaterialTheme.colorScheme.background) {
              Text("All", color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
            }
          }
          items(shortcuts, key = { it.key }) { sc ->
            val sel = activeShortcut == sc.key
            Surface(onClick = { onShortcutSelected(sc.key) }, shape = RoundedCornerShape(18.dp), color = if (sel) DesktopCrimson else MaterialTheme.colorScheme.background) {
              Text("${sc.label} (${sc.count})", color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
            }
          }
        }
      }
      HorizontalDivider()
      if (results.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text("No videos found", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
        }
      } else {
        LazyVerticalGrid(
          columns = GridCells.Adaptive(minSize = 210.dp),
          contentPadding = PaddingValues(16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxSize(),
        ) {
          itemsIndexed(results, key = { _, it -> it.postId }) { idx, item ->
            val theme = SharedPostRules.themes.firstOrNull { it.id == item.themeId } ?: SharedPostRules.themes.first()
            val previewImage = item.thumbnailUrl.ifBlank { item.photoUrls.firstOrNull().orEmpty() }
            Surface(
              onClick = { viewerStartIndex = idx },
              shape = RoundedCornerShape(14.dp),
              color = Color(0xFF0E121A),
              modifier = Modifier.fillMaxWidth().aspectRatio(9f / 16f),
            ) {
              Box(modifier = Modifier.fillMaxSize()) {
                if (previewImage.isNotBlank() && item.mode != SharedPostMode.TEXT) {
                  DesktopRemoteCachedImage(
                    apiClient = apiClient,
                    pathOrUrl = previewImage,
                    modifier = Modifier.fillMaxSize().blur(18.dp),
                    contentScale = ContentScale.Crop,
                    fallback = {},
                  )
                  Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.40f)))
                  DesktopRemoteCachedImage(
                    apiClient = apiClient,
                    pathOrUrl = previewImage,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    fallback = {},
                  )
                } else {
                  DesktopPostThemeCanvas(theme = theme, modifier = Modifier.fillMaxSize()) {
                    val ink = if (theme.isLightInk) Color.White else Color(0xFF1A1C1E)
                    Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                      DesktopSmartInteractiveText(
                        text = item.textContent.ifBlank { item.caption },
                        source = "feed",
                        color = ink,
                        linkColor = if (theme.isLightInk) Color(0xFFFFD6DF) else DesktopCrimson,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        showOtpPills = false,
                        modifier = Modifier.align(Alignment.Center),
                      )
                    }
                  }
                }
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)))),
                )
                Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                  Text(
                    text = "@${item.authorUsername}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                      .align(Alignment.BottomStart)
                      .clickable { onOpenAuthorProfile(item.authorUserId) },
                  )
                  Row(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    DesktopSvgIcon(DesktopIconKind.HEART_FILLED, Color.White, 12.dp)
                    Text(
                      text = SharedFeedMath.formatCompactCount(item.likesCount),
                      color = Color.White,
                      fontSize = 11.5.sp,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

private enum class DesktopProfileFilter(val apiValue: String, val label: String) {
  ALL("all", "All posts"),
  VIDEOS("video", "Videos"),
  PHOTOS("photos", "Photos"),
  TEXT("text", "Text"),
}

private enum class DesktopProfileSort(val label: String) {
  LATEST("Latest"),
  MOST_VIEWED("Most viewed"),
  MOST_LIKED("Most liked"),
}

private val DesktopReportCategories = listOf(
  "Spam or misleading",
  "Harassment or bullying",
  "Impersonation",
  "Hate or abusive content",
  "Scam or fraud",
  "Inappropriate content",
  "Other",
)

private fun formatDesktopProfileCount(count: Long): String {
  if (count <= 0L) return "0"
  if (count >= 1_000_000L) {
    val tenths = count / 100_000L
    val w = tenths / 10L
    val r = tenths % 10L
    return if (r == 0L) "${w}M" else "$w.${r}M"
  }
  if (count >= 1_000L) {
    val tenths = count / 100L
    val w = tenths / 10L
    val r = tenths % 10L
    return if (r == 0L) "${w}K" else "$w.${r}K"
  }
  return count.toString()
}

@Composable
private fun DesktopRemoteCachedImage(
  apiClient: DesktopApiClient,
  pathOrUrl: String,
  modifier: Modifier = Modifier,
  contentScale: ContentScale = ContentScale.Crop,
  fallback: @Composable () -> Unit,
) {
  var bitmap by remember(pathOrUrl) { mutableStateOf<ImageBitmap?>(null) }
  LaunchedEffect(pathOrUrl) {
    if (pathOrUrl.isBlank()) {
      bitmap = null
      return@LaunchedEffect
    }
    bitmap = withContext(Dispatchers.IO) {
      val bytes = apiClient.fetchCachedMediaBytes(pathOrUrl) ?: return@withContext null
      runCatching { SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
    }
  }
  val bmp = bitmap
  if (bmp != null) {
    Image(
      bitmap = bmp,
      contentDescription = null,
      contentScale = contentScale,
      modifier = modifier,
    )
  } else {
    fallback()
  }
}

@Composable
private fun DesktopUserProfileScreen(
  apiClient: DesktopApiClient,
  targetUserId: String,
  isDark: Boolean,
  isEmbeddedOwnProfileTab: Boolean,
  onBack: () -> Unit,
  onOpenUserProfile: (String) -> Unit,
  onOpenPostAtIndex: (List<DesktopFeedPost>, Int) -> Unit,
  onToggleTheme: () -> Unit,
  onOpenSettings: () -> Unit = {},
  onOpenStars: () -> Unit = {},
  onSignOut: () -> Unit,
  onMessageUser: (DesktopUserProfile) -> Unit = {},
  onToast: (String) -> Unit,
) {
  val scope = rememberCoroutineScope()
  val accentCrimson = DesktopCrimson
  var profile by remember(targetUserId) { mutableStateOf<DesktopUserProfile?>(null) }
  var isLoading by remember(targetUserId) { mutableStateOf(true) }
  var selectedOwnerTab by remember(targetUserId) { mutableStateOf(SharedOwnerProfileTab.POSTS) }
  var savedSubTab by remember(targetUserId) { mutableIntStateOf(0) } // 0 = Posts, 1 = Links
  var postFilter by remember(targetUserId) { mutableStateOf(DesktopProfileFilter.ALL) }
  var postSort by remember(targetUserId) { mutableStateOf(DesktopProfileSort.LATEST) }
  var showFilterPopup by remember { mutableStateOf(false) }
  var showSortPopup by remember { mutableStateOf(false) }
  var showMoreDialog by remember { mutableStateOf(false) }
  var showBackgroundsDialog by remember { mutableStateOf(false) }
  var isUploadingBg by remember { mutableStateOf(false) }
  var showEditProfileDialog by remember { mutableStateOf(false) }
  var showProfileMenu by remember { mutableStateOf(false) }
  var showReportCategories by remember { mutableStateOf(false) }
  var pendingReportReason by remember { mutableStateOf<String?>(null) }
  var reportDetails by remember { mutableStateOf("") }

  var connectionsType by remember { mutableStateOf<String?>(null) } // "followers" or "following"
  var connectionsItems by remember { mutableStateOf<List<DesktopContactItem>>(emptyList()) }
  var connectionsPage by remember { mutableStateOf(1) }
  var connectionsHasMore by remember { mutableStateOf(false) }
  var connectionsLoading by remember { mutableStateOf(false) }

  val reloadProfile: () -> Unit = {
    scope.launch {
      val res = withContext(Dispatchers.IO) { apiClient.getUserProfile(targetUserId) }
      if (res is DesktopApiOutcome.Ok) {
        profile = res.data
      }
    }
  }

  LaunchedEffect(targetUserId) {
    isLoading = true
    val res = withContext(Dispatchers.IO) { apiClient.getUserProfile(targetUserId) }
    if (res is DesktopApiOutcome.Ok) {
      profile = res.data
    } else if (res is DesktopApiOutcome.Fail) {
      onToast(res.message)
    }
    isLoading = false
  }

  val openConnections: (Boolean) -> Unit = { isFollowers ->
    val p = profile
    if (p != null) {
      if (p.isPrivateRestricted && !p.isOwner) {
        onToast("This account is private")
      } else if (!isFollowers && !p.isOwner && p.followingVisibility.equals("only_me", ignoreCase = true)) {
        onToast("Following list is private")
      } else {
        connectionsType = if (isFollowers) "followers" else "following"
        connectionsItems = emptyList()
        connectionsPage = 1
        connectionsHasMore = false
        connectionsLoading = true
        scope.launch {
          val res = withContext(Dispatchers.IO) { apiClient.getUserConnections(p.userId, isFollowers, 1) }
          if (res is DesktopApiOutcome.Ok) {
            connectionsItems = res.data.items
            connectionsPage = res.data.page
            connectionsHasMore = res.data.hasMore
          }
          connectionsLoading = false
        }
      }
    }
  }

  val p = profile
  val showHeroBanner = p != null && !p.isBlocked && (p.isOwner || p.backgrounds.isNotEmpty())
  val heroBackgrounds = p?.backgrounds.orEmpty()
  var heroSlideIndex by remember(heroBackgrounds.size) { mutableIntStateOf(0) }
  LaunchedEffect(heroBackgrounds) {
    if (heroBackgrounds.size > 1) {
      while (true) {
        delay(5_000L)
        heroSlideIndex = (heroSlideIndex + 1) % heroBackgrounds.size
      }
    } else {
      heroSlideIndex = 0
    }
  }

  val profileLink = apiClient.shareUrlForProfile(p?.username?.ifBlank { p.userId } ?: targetUserId)
  val copyLinkAction: () -> Unit = {
    runCatching { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(profileLink), null) }
    onToast("Link copied")
  }
  val inviteAction: () -> Unit = {
    val msg = "Join me on Telefam: $profileLink"
    runCatching { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(msg), null) }
    onToast("Invite link copied")
  }

  val filteredPosts = remember(p?.posts, p?.savedPosts, p?.repostedPosts, p?.lockedPosts, p?.isOwner, selectedOwnerTab, postFilter, postSort) {
    val source = if (p?.isOwner == true) {
      when (selectedOwnerTab) {
        SharedOwnerProfileTab.POSTS -> p.posts.filterNot { it.privacy.equals("private", ignoreCase = true) }
        SharedOwnerProfileTab.SAVED -> p.savedPosts
        SharedOwnerProfileTab.REPOSTS -> p.repostedPosts
        SharedOwnerProfileTab.LOCKED -> {
          val explicit = p.lockedPosts
          val fallback = p.posts.filter { it.privacy.equals("private", ignoreCase = true) }
          (explicit + fallback.filter { fb -> explicit.none { it.postId == fb.postId } })
        }
      }
    } else {
      p?.posts.orEmpty()
    }
    val raw = source.filter { item ->
      when (postFilter) {
        DesktopProfileFilter.ALL -> true
        DesktopProfileFilter.VIDEOS -> item.mode == SharedPostMode.VIDEO
        DesktopProfileFilter.PHOTOS -> item.mode == SharedPostMode.PHOTOS
        DesktopProfileFilter.TEXT -> item.mode == SharedPostMode.TEXT
      }
    }
    when (postSort) {
      DesktopProfileSort.LATEST -> raw.sortedByDescending { it.createdAtMs }
      DesktopProfileSort.MOST_VIEWED -> raw.sortedByDescending { it.viewsCount }
      DesktopProfileSort.MOST_LIKED -> raw.sortedByDescending { it.likesCount }
    }
  }

  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Box(modifier = Modifier.fillMaxSize()) {
      Column(modifier = Modifier.fillMaxSize()) {
        if (!showHeroBanner) {
          // Flat Top Header Bar when visitor has no hero background or blocked
          Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (!isEmbeddedOwnProfileTab) {
              Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
              ) {
                DesktopSvgIcon(DesktopIconKind.BACK_ARROW, MaterialTheme.colorScheme.onBackground, 20.dp)
              }
            } else {
              Spacer(modifier = Modifier.width(44.dp))
            }

            Text(
              text = p?.fullName?.ifBlank { "@${p.username.ifBlank { p.userId }}" } ?: "Profile",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              textAlign = TextAlign.Center,
              modifier = Modifier.weight(1f),
            )

            Box(
              modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = inviteAction),
              contentAlignment = Alignment.Center,
            ) {
              DesktopSvgIcon(DesktopIconKind.INVITE_T_BADGE, accentCrimson, 26.dp)
            }

            Box(
              modifier = Modifier.size(44.dp).clip(CircleShape).clickable { showMoreDialog = true },
              contentAlignment = Alignment.Center,
            ) {
              DesktopSvgIcon(DesktopIconKind.MORE_DOTS, MaterialTheme.colorScheme.onBackground, 22.dp)
            }
          }
        }

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
          val cols = when {
            maxWidth >= 900.dp -> 5
            maxWidth >= 640.dp -> 4
            else -> 3
          }

          LazyVerticalGrid(
            columns = GridCells.Fixed(cols),
            modifier = Modifier.fillMaxSize().widthIn(max = 920.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
              Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (showHeroBanner && p != null) {
                  Box(modifier = Modifier.fillMaxWidth().height(268.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().height(232.dp)) {
                      val currentBg = heroBackgrounds.getOrNull(heroSlideIndex % heroBackgrounds.size.coerceAtLeast(1))
                      AnimatedContent(
                        targetState = currentBg,
                        transitionSpec = { fadeIn(tween(800)) togetherWith fadeOut(tween(800)) },
                      ) { bg ->
                        if (bg != null) {
                          DesktopRemoteCachedImage(
                            apiClient = apiClient,
                            pathOrUrl = bg.url,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                          ) {
                            Box(
                              modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(listOf(Color(0xFF28121B), Color(0xFF141824)))),
                            )
                          }
                        } else {
                          Box(
                            modifier = Modifier
                              .fillMaxSize()
                              .background(Brush.linearGradient(listOf(Color(0xFF26121A), Color(0xFF10131C), Color(0xFF1B1F2C)))),
                          )
                        }
                      }

                      // Top dark legibility scrim + bottom fade into screen background
                      Box(
                        modifier = Modifier
                          .fillMaxSize()
                          .background(
                            Brush.verticalGradient(
                              0.0f to Color.Black.copy(alpha = 0.48f),
                              0.36f to Color.Transparent,
                              0.68f to MaterialTheme.colorScheme.background.copy(alpha = 0.50f),
                              1.0f to MaterialTheme.colorScheme.background,
                            ),
                          ),
                      )

                      // Hero overlay top bar matching screenshot
                      Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        if (!isEmbeddedOwnProfileTab) {
                          Box(
                            modifier = Modifier
                              .size(40.dp)
                              .clip(CircleShape)
                              .background(Color.Black.copy(alpha = 0.42f))
                              .clickable(onClick = onBack),
                            contentAlignment = Alignment.Center,
                          ) {
                            DesktopSvgIcon(DesktopIconKind.BACK_ARROW, Color.White, 19.dp)
                          }
                        } else {
                          Spacer(modifier = Modifier.width(40.dp))
                        }

                        Text(
                          text = p.fullName.ifBlank { "@${p.username.ifBlank { p.userId }}" },
                          fontSize = 18.sp,
                          fontWeight = FontWeight.Bold,
                          color = Color.White,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                          textAlign = TextAlign.Center,
                          modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        )

                        if (p.isOwner) {
                          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                              modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.42f))
                                .clickable { onToast("Subscribers — Coming soon") },
                              contentAlignment = Alignment.Center,
                            ) {
                              DesktopSvgIcon(DesktopIconKind.CROWN, Color.White, 18.dp)
                            }
                            Box(
                              modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.42f))
                                .clickable { onOpenSettings() },
                              contentAlignment = Alignment.Center,
                            ) {
                              DesktopSvgIcon(DesktopIconKind.GEAR, Color.White, 18.dp)
                            }
                            Box(
                              modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.42f))
                                .clickable { showProfileMenu = true },
                              contentAlignment = Alignment.Center,
                            ) {
                              DesktopSvgIcon(DesktopIconKind.THREE_DASHES, Color.White, 18.dp)
                            }
                          }
                        } else {
                          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                              modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.42f))
                                .clickable(onClick = inviteAction),
                              contentAlignment = Alignment.Center,
                            ) {
                              DesktopSvgIcon(DesktopIconKind.INVITE_T_BADGE, accentCrimson, 22.dp)
                            }
                            Box(
                              modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.42f))
                                .clickable { showMoreDialog = true },
                              contentAlignment = Alignment.Center,
                            ) {
                              DesktopSvgIcon(DesktopIconKind.MORE_DOTS, Color.White, 18.dp)
                            }
                          }
                        }
                      }

                      if (p.isOwner) {
                        Surface(
                          onClick = { showBackgroundsDialog = true },
                          shape = RoundedCornerShape(20.dp),
                          color = Color.Black.copy(alpha = 0.62f),
                          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                          modifier = Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 52.dp),
                        ) {
                          Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                          ) {
                            DesktopSvgIcon(DesktopIconKind.CAMERA, Color.White, 15.dp)
                            Text(
                              text = if (heroBackgrounds.isEmpty()) "Edit background" else "Edit background (${heroBackgrounds.size}/5)",
                              color = Color.White,
                              fontSize = 12.sp,
                              fontWeight = FontWeight.SemiBold,
                            )
                          }
                        }
                      }
                    }

                    // Overlapping circular avatar centered along bottom of hero
                    val ringBrush = Brush.linearGradient(
                      listOf(accentCrimson, Color(0xFFE11D48), Color(0xFF991B1B)),
                    )
                    Box(
                      modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .border(2.6.dp, ringBrush, CircleShape)
                        .padding(4.dp)
                        .let { mod ->
                          if (p.isOwner) mod.clickable { showEditProfileDialog = true } else mod
                        },
                      contentAlignment = Alignment.Center,
                    ) {
                      DesktopRemoteCachedImage(
                        apiClient = apiClient,
                        pathOrUrl = p.avatarUrl,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                      ) {
                        Box(
                          modifier = Modifier.fillMaxSize().clip(CircleShape).background(if (isDark) DesktopDarkBlush else DesktopBlush),
                          contentAlignment = Alignment.Center,
                        ) {
                          Text(
                            text = TelefamContactsRules.initialsFor(p.fullName, p.username),
                            color = accentCrimson,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                          )
                        }
                      }
                    }
                  }
                } else {
                  Spacer(modifier = Modifier.height(6.dp))
                  val ringBrush = Brush.linearGradient(
                    listOf(Color(0xFFD946EF), DesktopCrimson, Color(0xFFF97316)),
                  )
                  Box(
                    modifier = Modifier.size(94.dp).clip(CircleShape).border(2.6.dp, ringBrush, CircleShape).padding(4.dp),
                    contentAlignment = Alignment.Center,
                  ) {
                    DesktopRemoteCachedImage(
                      apiClient = apiClient,
                      pathOrUrl = p?.avatarUrl.orEmpty(),
                      modifier = Modifier.fillMaxSize().clip(CircleShape),
                    ) {
                      Box(
                        modifier = Modifier.fillMaxSize().clip(CircleShape).background(if (isDark) DesktopDarkBlush else DesktopBlush),
                        contentAlignment = Alignment.Center,
                      ) {
                        Text(
                          text = TelefamContactsRules.initialsFor(p?.fullName.orEmpty(), p?.username.orEmpty()),
                          color = accentCrimson,
                          fontSize = 26.sp,
                          fontWeight = FontWeight.Bold,
                        )
                      }
                    }
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                  text = "@${p?.username?.ifBlank { p.userId } ?: targetUserId}",
                  fontSize = 15.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                  modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth().padding(horizontal = 24.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable { openConnections(false) }.padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                  ) {
                    Text(formatDesktopProfileCount(p?.followingCount ?: 0L), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Following", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                  Box(modifier = Modifier.width(1.dp).height(26.dp).background(MaterialTheme.colorScheme.outlineVariant))
                  Column(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable { openConnections(true) }.padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                  ) {
                    Text(formatDesktopProfileCount(p?.followersCount ?: 0L), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Followers", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                  Box(modifier = Modifier.width(1.dp).height(26.dp).background(MaterialTheme.colorScheme.outlineVariant))
                  Column(
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                  ) {
                    Text(formatDesktopProfileCount(p?.likesCount ?: 0L), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Likes", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }

                Spacer(modifier = Modifier.height(16.dp))
                if (p != null && p.isBlocked) {
                  Column(
                    modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Text("You blocked this account.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.5.sp)
                    Surface(
                      onClick = {
                        scope.launch {
                          val res = withContext(Dispatchers.IO) { apiClient.unblockUser(p.userId) }
                          if (res is DesktopApiOutcome.Ok) {
                            profile = p.copy(isBlocked = false)
                            onToast("Account unblocked")
                            val fresh = withContext(Dispatchers.IO) { apiClient.getUserProfile(p.userId) }
                            if (fresh is DesktopApiOutcome.Ok) profile = fresh.data
                          }
                        }
                      },
                      shape = RoundedCornerShape(8.dp),
                      color = accentCrimson,
                      modifier = Modifier.fillMaxWidth().height(40.dp),
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Text("Unblock", color = Color.White, fontWeight = FontWeight.SemiBold)
                      }
                    }
                  }
                } else if (p != null && !p.isOwner) {
                  val rel = p.relationship
                  val followLabel = when (rel) {
                    ContactRelationshipState.FOLLOW -> "Follow"
                    ContactRelationshipState.FOLLOWING -> "Following"
                    ContactRelationshipState.FOLLOW_BACK -> "Follow back"
                    ContactRelationshipState.FRIENDS -> "Friends"
                  }
                  val isPrimaryCrimson = rel == ContactRelationshipState.FOLLOW || rel == ContactRelationshipState.FOLLOW_BACK
                  Row(
                    modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth().padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Surface(
                      onClick = {
                        val wantFollow = !p.isFollowing
                        profile = p.copy(
                          isFollowing = wantFollow,
                          followersCount = if (wantFollow) p.followersCount + 1 else (p.followersCount - 1).coerceAtLeast(0L),
                        )
                        scope.launch {
                          val res = withContext(Dispatchers.IO) { apiClient.setFollowContact(p.userId, wantFollow) }
                          if (res is DesktopApiOutcome.Fail && res.statusCode == 429) {
                            profile = p
                            onToast(TelefamContactsRules.RATE_LIMIT_TOAST_MESSAGE)
                          }
                        }
                      },
                      shape = RoundedCornerShape(8.dp),
                      color = if (isPrimaryCrimson) accentCrimson else MaterialTheme.colorScheme.surface,
                      border = if (isPrimaryCrimson) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                      modifier = Modifier.weight(1.08f).height(40.dp),
                    ) {
                      Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        if (isPrimaryCrimson) {
                          DesktopSvgIcon(DesktopIconKind.PERSON_ADD, Color.White, 16.dp)
                          Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                          text = followLabel,
                          color = if (isPrimaryCrimson) Color.White else MaterialTheme.colorScheme.onSurface,
                          fontSize = 13.5.sp,
                          fontWeight = FontWeight.SemiBold,
                          maxLines = 1,
                        )
                      }
                    }

                    Surface(
                      onClick = {
                        val target = p
                        if (target != null) {
                          onMessageUser(target)
                        }
                      },
                      shape = RoundedCornerShape(8.dp),
                      color = MaterialTheme.colorScheme.surface,
                      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                      modifier = Modifier.weight(1f).height(40.dp),
                    ) {
                      Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        DesktopSvgIcon(DesktopIconKind.SHARE_ARROW, MaterialTheme.colorScheme.onSurface, 15.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Message", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                      }
                    }

                    Surface(
                      onClick = { onToast("Subscribe — Coming soon") },
                      shape = RoundedCornerShape(8.dp),
                      color = accentCrimson,
                      modifier = Modifier.weight(1.05f).height(40.dp),
                    ) {
                      Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        DesktopSvgIcon(DesktopIconKind.CROWN, Color.White, 15.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Subscribe", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                      }
                    }
                  }
                } else if (p != null) {
                  // Owner profile actions matching screenshot: crimson Edit profile + dark/surface Share profile
                  Row(
                    modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    Surface(
                      onClick = { showEditProfileDialog = true },
                      shape = RoundedCornerShape(22.dp),
                      color = accentCrimson,
                      modifier = Modifier.weight(1f).height(42.dp),
                    ) {
                      Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        DesktopSvgIcon(DesktopIconKind.EDIT_PENCIL, Color.White, 15.dp)
                        Spacer(modifier = Modifier.width(7.dp))
                        Text("Edit profile", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                      }
                    }
                    Surface(
                      onClick = copyLinkAction,
                      shape = RoundedCornerShape(22.dp),
                      color = MaterialTheme.colorScheme.surface,
                      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                      modifier = Modifier.weight(1f).height(42.dp),
                    ) {
                      Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        DesktopSvgIcon(DesktopIconKind.SHARE_NODES, MaterialTheme.colorScheme.onSurface, 15.dp)
                        Spacer(modifier = Modifier.width(7.dp))
                        Text("Share profile", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                      }
                    }
                  }
                }

                if (!p?.bio.isNullOrBlank()) {
                  Spacer(modifier = Modifier.height(14.dp))
                  DesktopSmartInteractiveText(
                    text = p!!.bio,
                    source = "profile",
                    color = MaterialTheme.colorScheme.onBackground,
                    linkColor = accentCrimson,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, lineHeight = 19.sp),
                    textAlign = TextAlign.Center,
                    showOtpPills = false,
                    modifier = Modifier.widthIn(max = 360.dp).padding(horizontal = 24.dp),
                  )
                }

                if (!p?.location.isNullOrBlank()) {
                  Spacer(modifier = Modifier.height(8.dp))
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    DesktopSvgIcon(DesktopIconKind.LOCATION_PIN, accentCrimson, 15.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(p!!.location, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                }

                Spacer(modifier = Modifier.height(14.dp))
                if (p?.isOwner == true) {
                  val publicCount = p.posts.count { !it.privacy.equals("private", ignoreCase = true) }
                  Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                      SharedOwnerProfileTab.entries.forEach { tab ->
                        val selected = selectedOwnerTab == tab
                        val iconKind = when (tab) {
                          SharedOwnerProfileTab.POSTS -> DesktopIconKind.GRID_2X2
                          SharedOwnerProfileTab.SAVED -> DesktopIconKind.BOOKMARK
                          SharedOwnerProfileTab.REPOSTS -> DesktopIconKind.REPOST
                          SharedOwnerProfileTab.LOCKED -> DesktopIconKind.LOCK
                        }
                        val tint = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                        Column(
                          modifier = Modifier
                            .weight(1f)
                            .clickable { selectedOwnerTab = tab },
                          horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                          Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                          ) {
                            DesktopSvgIcon(iconKind, tint, 15.dp)
                            Text(
                              text = if (tab == SharedOwnerProfileTab.POSTS) "Posts $publicCount" else tab.label,
                              fontSize = 13.sp,
                              fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                              color = tint,
                              maxLines = 1,
                            )
                          }
                          Box(
                            modifier = Modifier
                              .fillMaxWidth(0.75f)
                              .height(2.5.dp)
                              .background(if (selected) accentCrimson else Color.Transparent),
                          )
                        }
                      }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                  }
                } else {
                  Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                      Column(modifier = Modifier.weight(0.45f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                          modifier = Modifier.padding(vertical = 10.dp),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                          DesktopSvgIcon(DesktopIconKind.GRID_2X2, MaterialTheme.colorScheme.onBackground, 17.dp)
                          Text("Posts", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(2.5.dp).background(accentCrimson))
                      }
                      Box(modifier = Modifier.weight(0.55f))
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                  }
                }

                if (p?.isOwner == true && selectedOwnerTab == SharedOwnerProfileTab.SAVED) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    listOf("Posts", "Links").forEachIndexed { subIdx, subLabel ->
                      val subSel = savedSubTab == subIdx
                      Surface(
                        onClick = { savedSubTab = subIdx },
                        shape = RoundedCornerShape(18.dp),
                        color = if (subSel) accentCrimson else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                      ) {
                        Text(
                          text = subLabel,
                          color = if (subSel) Color.White else MaterialTheme.colorScheme.onSurface,
                          fontSize = 13.sp,
                          fontWeight = FontWeight.SemiBold,
                          modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                        )
                      }
                    }
                  }
                } else {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Row(
                      modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { showFilterPopup = true }.padding(horizontal = 4.dp, vertical = 4.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Text(postFilter.label, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                      Spacer(modifier = Modifier.width(6.dp))
                      DesktopSvgIcon(DesktopIconKind.CHEVRON_DOWN, MaterialTheme.colorScheme.onBackground, 14.dp)
                    }
                    Box(
                      modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).clickable { showSortPopup = true },
                      contentAlignment = Alignment.Center,
                    ) {
                      DesktopSvgIcon(DesktopIconKind.FILTER_SLIDERS, MaterialTheme.colorScheme.onBackground, 18.dp)
                    }
                  }
                }
              }
            }

            if (p?.isOwner == true && selectedOwnerTab == SharedOwnerProfileTab.SAVED && savedSubTab == 1) {
              item(key = "saved_links_pane", span = { GridItemSpan(maxLineSpan) }) {
                val sc = LocalDesktopSmartActions.current
                if (sc != null) {
                  Box(modifier = Modifier.fillMaxWidth().height(460.dp)) {
                    DesktopSavedLinksPane(controller = sc, modifier = Modifier.fillMaxSize())
                  }
                }
              }
            } else if (isLoading && filteredPosts.isEmpty()) {
              item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                  DesktopTricolorSpinner(size = 34.dp)
                }
              }
            } else if (p != null && p.isPrivateRestricted && !p.isOwner && !p.isBlocked) {
              item(key = "private_restricted", span = { GridItemSpan(maxLineSpan) }) {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 42.dp, horizontal = 24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  DesktopSvgIcon(DesktopIconKind.LOCK, MaterialTheme.colorScheme.onSurfaceVariant, 32.dp)
                  Text("This account is private", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                  Text(
                    text = "Follow this account to see their photos and videos.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                  )
                }
              }
            } else if (p != null && !p.isBlocked && filteredPosts.isEmpty()) {
              item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                val emptyText = when (selectedOwnerTab) {
                  SharedOwnerProfileTab.POSTS -> "No posts yet"
                  SharedOwnerProfileTab.SAVED -> "No saved posts yet"
                  SharedOwnerProfileTab.REPOSTS -> "No reposts yet"
                  SharedOwnerProfileTab.LOCKED -> "No private posts locked"
                }
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 42.dp), contentAlignment = Alignment.Center) {
                  Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
              }
            } else if (p != null && !p.isBlocked) {
              itemsIndexed(filteredPosts, key = { _, item -> item.postId }) { idx, post ->
                val theme = SharedPostRules.themes.firstOrNull { it.id == post.themeId } ?: SharedPostRules.themes.first()
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .background(Color(0xFF101319))
                    .clickable { onOpenPostAtIndex(filteredPosts, idx) },
                ) {
                  if (post.mode == SharedPostMode.TEXT) {
                    DesktopPostThemeCanvas(theme = theme, modifier = Modifier.fillMaxSize()) {
                      Box(modifier = Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
                        val ink = if (theme.isLightInk) Color.White else Color(0xFF1A1C1E)
                        Text(
                          text = post.textContent.ifBlank { post.caption },
                          color = ink,
                          fontSize = 12.sp,
                          fontWeight = FontWeight.Bold,
                          textAlign = TextAlign.Center,
                          maxLines = 4,
                          overflow = TextOverflow.Ellipsis,
                        )
                      }
                    }
                  } else {
                    DesktopRemoteCachedImage(apiClient = apiClient, pathOrUrl = post.thumbnailUrl, modifier = Modifier.fillMaxSize()) {
                      DesktopPostThemeCanvas(theme = theme, modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
                          Text(
                            text = post.caption.ifBlank { post.hashtags.firstOrNull()?.let { "#$it" }.orEmpty() },
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                          )
                        }
                      }
                    }
                  }

                  if (p.isOwner && !post.privacy.equals("public", ignoreCase = true)) {
                    Box(
                      modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.62f))
                        .padding(5.dp),
                    ) {
                      val privKind = if (post.privacy.equals("friends", ignoreCase = true)) {
                        DesktopIconKind.FRIENDS_GROUP
                      } else {
                        DesktopIconKind.LOCK
                      }
                      DesktopSvgIcon(privKind, Color.White, 12.dp)
                    }
                  }

                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(48.dp)
                      .align(Alignment.BottomCenter)
                      .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)))),
                  )

                  Row(
                    modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    DesktopSvgIcon(DesktopIconKind.PLAY_OUTLINE, Color.White, 13.dp)
                    Text(
                      text = formatDesktopProfileCount(post.viewsCount),
                      color = Color.White,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold,
                    )
                  }
                }
              }
            }
          }
        }
      }

      if (showFilterPopup) {
        Box(
          modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).clickable { showFilterPopup = false },
          contentAlignment = Alignment.Center,
        ) {
          Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.width(280.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text("Filter posts", fontWeight = FontWeight.Bold, fontSize = 16.sp)
              DesktopProfileFilter.entries.forEach { opt ->
                DesktopDialogButton(opt.label) { postFilter = opt; showFilterPopup = false }
              }
            }
          }
        }
      }

      if (showSortPopup) {
        Box(
          modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).clickable { showSortPopup = false },
          contentAlignment = Alignment.Center,
        ) {
          Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.width(280.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text("Sort posts", fontWeight = FontWeight.Bold, fontSize = 16.sp)
              DesktopProfileSort.entries.forEach { opt ->
                DesktopDialogButton(opt.label) { postSort = opt; showSortPopup = false }
              }
            }
          }
        }
      }

      // Multi-background Upload & Reorder Manager Modal (up to 5 photos)
      if (showBackgroundsDialog && p != null) {
        DesktopModalOverlay(onDismiss = { showBackgroundsDialog = false }) {
          val remainingSlots = (5 - heroBackgrounds.size).coerceAtLeast(0)
          Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
              Column(modifier = Modifier.weight(1f)) {
                Text("Profile backgrounds (${heroBackgrounds.size}/5)", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Auto-fades every 5 seconds for all visitors", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Box(modifier = Modifier.size(32.dp).clip(CircleShape).clickable { showBackgroundsDialog = false }, contentAlignment = Alignment.Center) {
                DesktopSvgIcon(DesktopIconKind.CLOSE, MaterialTheme.colorScheme.onSurfaceVariant, 16.dp)
              }
            }

            if (isUploadingBg) {
              LinearProgressIndicator(color = accentCrimson, modifier = Modifier.fillMaxWidth())
            }

            Button(
              enabled = !isUploadingBg && remainingSlots > 0,
              onClick = {
                val dlg = FileDialog(null as Frame?, "Select background photo", FileDialog.LOAD).apply {
                  isMultipleMode = remainingSlots > 1
                  setFilenameFilter { _, name ->
                    val low = name.lowercase()
                    low.endsWith(".jpg") || low.endsWith(".jpeg") || low.endsWith(".png") || low.endsWith(".webp")
                  }
                  isVisible = true
                }
                val chosen = dlg.files?.take(remainingSlots).orEmpty()
                if (chosen.isNotEmpty()) {
                  isUploadingBg = true
                  scope.launch {
                    var latestList: List<DesktopProfileBackgroundItem>? = null
                    for (file in chosen) {
                      val bytes = withContext(Dispatchers.IO) {
                        val img = ImageIO.read(file)
                        if (img != null) {
                          val scale = minOf(1f, 1600f / maxOf(img.width, img.height).coerceAtLeast(1))
                          val tw = (img.width * scale).toInt().coerceAtLeast(1)
                          val th = (img.height * scale).toInt().coerceAtLeast(1)
                          val out = BufferedImage(tw, th, BufferedImage.TYPE_INT_RGB)
                          val g = out.createGraphics()
                          g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                          g.drawImage(img, 0, 0, tw, th, null)
                          g.dispose()
                          ByteArrayOutputStream().also { ImageIO.write(out, "jpg", it) }.toByteArray()
                        } else {
                          file.readBytes()
                        }
                      }
                      val res = withContext(Dispatchers.IO) { apiClient.uploadProfileBackground(bytes) }
                      when (res) {
                        is DesktopApiOutcome.Ok -> latestList = res.data
                        is DesktopApiOutcome.Fail -> {
                          onToast(res.message)
                          break
                        }
                      }
                    }
                    isUploadingBg = false
                    if (latestList != null) {
                      profile = profile?.copy(backgrounds = latestList!!)
                      reloadProfile()
                      onToast("Backgrounds updated")
                    }
                  }
                }
              },
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = accentCrimson),
              modifier = Modifier.fillMaxWidth().height(44.dp),
            ) {
              Text("Add background photo ($remainingSlots left)", color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            if (heroBackgrounds.isEmpty()) {
              Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                Text("No backgrounds uploaded yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.5.sp)
              }
            } else {
              LazyColumn(modifier = Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(heroBackgrounds, key = { _, bg -> bg.mediaId }) { idx, bg ->
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth().padding(10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      DesktopRemoteCachedImage(
                        apiClient = apiClient,
                        pathOrUrl = bg.url,
                        modifier = Modifier.size(width = 72.dp, height = 48.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                      ) {}
                      Column(modifier = Modifier.weight(1f)) {
                        Text("Slide #${idx + 1}", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        if (bg.width > 0 && bg.height > 0) {
                          Text("${bg.width}×${bg.height} · ${(bg.byteSize / 1024).coerceAtLeast(1)} KB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (bg.sha256Fingerprint.isNotBlank()) {
                          Text(
                            text = "SHA:${bg.sha256Fingerprint.take(10)}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                          )
                        }
                      }
                      if (idx > 0) {
                        Box(
                          modifier = Modifier.size(30.dp).clip(CircleShape).clickable(enabled = !isUploadingBg) {
                            val ids = heroBackgrounds.map { it.mediaId }.toMutableList()
                            val tmp = ids[idx - 1]; ids[idx - 1] = ids[idx]; ids[idx] = tmp
                            scope.launch {
                              val res = withContext(Dispatchers.IO) { apiClient.reorderProfileBackgrounds(ids) }
                              if (res is DesktopApiOutcome.Ok) profile = profile?.copy(backgrounds = res.data)
                            }
                          },
                          contentAlignment = Alignment.Center,
                        ) { DesktopSvgIcon(DesktopIconKind.CHEVRON_UP, MaterialTheme.colorScheme.onSurface, 14.dp) }
                      }
                      if (idx < heroBackgrounds.lastIndex) {
                        Box(
                          modifier = Modifier.size(30.dp).clip(CircleShape).clickable(enabled = !isUploadingBg) {
                            val ids = heroBackgrounds.map { it.mediaId }.toMutableList()
                            val tmp = ids[idx + 1]; ids[idx + 1] = ids[idx]; ids[idx] = tmp
                            scope.launch {
                              val res = withContext(Dispatchers.IO) { apiClient.reorderProfileBackgrounds(ids) }
                              if (res is DesktopApiOutcome.Ok) profile = profile?.copy(backgrounds = res.data)
                            }
                          },
                          contentAlignment = Alignment.Center,
                        ) { DesktopSvgIcon(DesktopIconKind.CHEVRON_DOWN, MaterialTheme.colorScheme.onSurface, 14.dp) }
                      }
                      Box(
                        modifier = Modifier.size(30.dp).clip(CircleShape).clickable(enabled = !isUploadingBg) {
                          scope.launch {
                            val res = withContext(Dispatchers.IO) { apiClient.deleteProfileBackground(bg.mediaId) }
                            if (res is DesktopApiOutcome.Ok) {
                              profile = profile?.copy(backgrounds = res.data)
                              onToast("Background deleted")
                            } else if (res is DesktopApiOutcome.Fail) {
                              onToast(res.message)
                            }
                          }
                        },
                        contentAlignment = Alignment.Center,
                      ) {
                        DesktopSvgIcon(DesktopIconKind.CLOSE, accentCrimson, 14.dp)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Full Profile Edit Modal (Name, 14-day Username lock, Country, Phone, Gender, DOB, Location, Bio, DP)
      if (showEditProfileDialog && p != null) {
        DesktopEditProfileModal(
          apiClient = apiClient,
          profile = p,
          onSaved = {
            showEditProfileDialog = false
            onToast("Profile updated")
            reloadProfile()
          },
          onDismiss = { showEditProfileDialog = false },
          onToast = onToast,
        )
      }

      if (showProfileMenu) {
        DesktopProfileMenuScreen(
          isDark = isDark,
          onBack = { showProfileMenu = false },
          onOpenStars = {
            showProfileMenu = false
            onOpenStars()
          },
          onFeatureComingSoon = { title -> onToast("$title — Coming soon") },
        )
      }

      if (showMoreDialog && p != null) {
        Box(
          modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.50f)).clickable { showMoreDialog = false },
          contentAlignment = Alignment.Center,
        ) {
          Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.width(320.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text("Profile options", fontWeight = FontWeight.Bold, fontSize = 17.sp)
              DesktopDialogButton("Copy link") { showMoreDialog = false; copyLinkAction() }
              DesktopDialogButton("Invite friends") { showMoreDialog = false; inviteAction() }
              if (p.isOwner) {
                DesktopDialogButton("Switch theme") { showMoreDialog = false; onToggleTheme() }
                DesktopDialogButton("Sign out") { showMoreDialog = false; onSignOut() }
              } else {
                DesktopDialogButton(if (p.isMuted) "Unmute @${p.username}" else "Mute @${p.username}") {
                  showMoreDialog = false
                  scope.launch {
                    val res = withContext(Dispatchers.IO) {
                      if (p.isMuted) apiClient.unmuteUser(p.userId) else apiClient.muteUser(p.userId)
                    }
                    if (res is DesktopApiOutcome.Ok) {
                      profile = p.copy(isMuted = res.data)
                      onToast(if (res.data) "Account muted" else "Account unmuted")
                    }
                  }
                }
                DesktopDialogButton(if (p.isBlocked) "Unblock @${p.username}" else "Block @${p.username}") {
                  showMoreDialog = false
                  scope.launch {
                    val res = withContext(Dispatchers.IO) {
                      if (p.isBlocked) apiClient.unblockUser(p.userId) else apiClient.blockUser(p.userId)
                    }
                    if (res is DesktopApiOutcome.Ok) {
                      profile = p.copy(isBlocked = res.data, isFollowing = if (res.data) false else p.isFollowing)
                      onToast(if (res.data) "Account blocked" else "Account unblocked")
                      if (!res.data) {
                        val fresh = withContext(Dispatchers.IO) { apiClient.getUserProfile(p.userId) }
                        if (fresh is DesktopApiOutcome.Ok) profile = fresh.data
                      }
                    }
                  }
                }
                DesktopDialogButton("Report") {
                  showMoreDialog = false
                  showReportCategories = true
                }
              }
              DesktopDialogButton("Cancel") { showMoreDialog = false }
            }
          }
        }
      }

      if (showReportCategories && p != null) {
        Box(
          modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable { showReportCategories = false },
          contentAlignment = Alignment.Center,
        ) {
          Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.width(340.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text("Report account", fontWeight = FontWeight.Bold, fontSize = 17.sp)
              DesktopReportCategories.forEach { cat ->
                DesktopDialogButton(cat) {
                  showReportCategories = false
                  pendingReportReason = cat
                  reportDetails = ""
                }
              }
              DesktopDialogButton("Cancel") { showReportCategories = false }
            }
          }
        }
      }

      val activeReportReason = pendingReportReason
      if (activeReportReason != null && p != null) {
        Box(
          modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable { pendingReportReason = null },
          contentAlignment = Alignment.Center,
        ) {
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.width(360.dp).clickable(enabled = false) {},
          ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Text(activeReportReason, fontWeight = FontWeight.Bold, fontSize = 17.sp)
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().height(92.dp),
              ) {
                Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                  if (reportDetails.isEmpty()) {
                    Text("Additional details (optional)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.5.sp)
                  }
                  BasicTextField(
                    value = reportDetails,
                    onValueChange = { reportDetails = it.take(300) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.5.sp),
                    cursorBrush = SolidColor(accentCrimson),
                    modifier = Modifier.fillMaxSize(),
                  )
                }
              }
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { pendingReportReason = null }, modifier = Modifier.weight(1f)) {
                  Text("Cancel")
                }
                Button(
                  onClick = {
                    val reason = activeReportReason
                    val det = reportDetails.trim()
                    pendingReportReason = null
                    scope.launch {
                      val res = withContext(Dispatchers.IO) { apiClient.reportUser(p.userId, reason, det) }
                      onToast(if (res is DesktopApiOutcome.Ok) "Report submitted" else "Unable to submit report")
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = accentCrimson),
                  modifier = Modifier.weight(1f),
                ) {
                  Text("Submit report", color = Color.White)
                }
              }
            }
          }
        }
      }

      if (connectionsType != null && p != null) {
        Box(
          modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.50f)).clickable { connectionsType = null },
          contentAlignment = Alignment.Center,
        ) {
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.width(440.dp).heightIn(min = 320.dp, max = 520.dp).clickable(enabled = false) {},
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = if (connectionsType == "followers") "Followers" else "Following",
                  fontSize = 17.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.weight(1f),
                )
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).clickable { connectionsType = null }, contentAlignment = Alignment.Center) {
                  DesktopSvgIcon(DesktopIconKind.CLOSE, MaterialTheme.colorScheme.onSurfaceVariant, 16.dp)
                }
              }
              Spacer(modifier = Modifier.height(8.dp))
              HorizontalDivider()
              if (connectionsLoading && connectionsItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                  DesktopTricolorSpinner(size = 32.dp)
                }
              } else if (connectionsItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                  Text("No users in this list yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
              } else {
                LazyColumn(
                  modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                  contentPadding = PaddingValues(vertical = 10.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  items(connectionsItems, key = { it.userId }) { conn ->
                    DesktopContactCard(
                      contact = conn,
                      isDark = isDark,
                      onOpenProfile = {
                        connectionsType = null
                        onOpenUserProfile(conn.userId)
                      },
                      onToggle = {
                        val wantFollow = !conn.isFollowing
                        connectionsItems = connectionsItems.map {
                          if (it.userId == conn.userId) it.copy(isFollowing = wantFollow) else it
                        }
                        scope.launch { withContext(Dispatchers.IO) { apiClient.setFollowContact(conn.userId, wantFollow) } }
                      },
                    )
                  }
                  if (connectionsHasMore) {
                    item(key = "load_more") {
                      OutlinedButton(
                        onClick = {
                          val nextP = connectionsPage + 1
                          val isF = connectionsType == "followers"
                          scope.launch {
                            val res = withContext(Dispatchers.IO) { apiClient.getUserConnections(p.userId, isF, nextP) }
                            if (res is DesktopApiOutcome.Ok) {
                              connectionsItems = connectionsItems + res.data.items.filter { n -> connectionsItems.none { it.userId == n.userId } }
                              connectionsPage = res.data.page
                              connectionsHasMore = res.data.hasMore
                            }
                          }
                        },
                        modifier = Modifier.fillMaxWidth(),
                      ) {
                        Text("Load more", color = accentCrimson)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DesktopEditProfileModal(
  apiClient: DesktopApiClient,
  profile: DesktopUserProfile,
  onSaved: () -> Unit,
  onDismiss: () -> Unit,
  onToast: (String) -> Unit,
) {
  val scope = rememberCoroutineScope()
  val initialCountry = remember(profile.countryIso) {
    DesktopCountryCatalog.allCountries.firstOrNull { it.iso.equals(profile.countryIso, ignoreCase = true) }
      ?: DesktopCountryCatalog.defaultCountry
  }
  val initialPhoneDigits = remember(profile.phoneNumber, initialCountry) {
    val cleanDial = initialCountry.dialCode.filter { it.isDigit() }
    val digits = profile.phoneNumber.filter { it.isDigit() }
    if (cleanDial.isNotBlank() && digits.startsWith(cleanDial) && digits.length > cleanDial.length) {
      digits.removePrefix(cleanDial)
    } else {
      digits
    }
  }

  var fullName by remember(profile.userId) { mutableStateOf(profile.fullName) }
  var username by remember(profile.userId) { mutableStateOf(profile.username) }
  var selectedCountry by remember(profile.userId) { mutableStateOf(initialCountry) }
  var phoneDigits by remember(profile.userId) { mutableStateOf(initialPhoneDigits) }
  var gender by remember(profile.userId) { mutableStateOf(profile.gender.ifBlank { "Prefer not to say" }) }
  var dob by remember(profile.userId) { mutableStateOf(profile.dateOfBirth.ifBlank { "2000-01-01" }) }
  var location by remember(profile.userId) { mutableStateOf(profile.location) }
  var bio by remember(profile.userId) { mutableStateOf(profile.bio) }

  var pendingRawAvatarFile by remember { mutableStateOf<File?>(null) }
  var avatarPreviewBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
  var uploadedAvatarMediaId by remember { mutableStateOf<String?>(null) }
  var showCountryPicker by remember { mutableStateOf(false) }
  var countrySearch by remember { mutableStateOf("") }
  var isSaving by remember { mutableStateOf(false) }

  val twoWeeksMs = 14L * 24L * 60L * 60L * 1000L
  val nextAllowedMs = when {
    profile.nextUsernameChangeMs > 0L -> profile.nextUsernameChangeMs
    profile.usernameChangedAtMs > 0L -> profile.usernameChangedAtMs + twoWeeksMs
    else -> 0L
  }
  val nowMs = System.currentTimeMillis()
  val isUsernameLocked = nextAllowedMs > nowMs
  val remainingLockLabel = if (!isUsernameLocked) "" else {
    val rem = (nextAllowedMs - nowMs).coerceAtLeast(0L)
    val days = rem / (24L * 60L * 60L * 1000L)
    val hrs = (rem % (24L * 60L * 60L * 1000L)) / (60L * 60L * 1000L)
    if (days > 0) "Locked for ${days}d ${hrs}h (once per 2 weeks)" else "Locked for ${hrs.coerceAtLeast(1)}h (once per 2 weeks)"
  }

  DesktopModalOverlay(onDismiss = onDismiss) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 660.dp)
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Edit profile", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Box(modifier = Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
          DesktopSvgIcon(DesktopIconKind.CLOSE, MaterialTheme.colorScheme.onSurfaceVariant, 16.dp)
        }
      }

      Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Avatar DP picker & crop
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
          Box(
            modifier = Modifier
              .size(72.dp)
              .clip(CircleShape)
              .border(2.dp, DesktopCrimson, CircleShape)
              .clickable {
                val dlg = FileDialog(null as Frame?, "Select profile photo", FileDialog.LOAD).apply {
                  setFilenameFilter { _, name ->
                    val low = name.lowercase()
                    low.endsWith(".jpg") || low.endsWith(".jpeg") || low.endsWith(".png")
                  }
                  isVisible = true
                }
                dlg.files?.firstOrNull()?.let { pendingRawAvatarFile = it }
              },
            contentAlignment = Alignment.Center,
          ) {
            val preview = avatarPreviewBitmap
            if (preview != null) {
              Image(preview, contentDescription = "Profile photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
              DesktopRemoteCachedImage(
                apiClient = apiClient,
                pathOrUrl = profile.avatarUrl,
                modifier = Modifier.fillMaxSize(),
              ) {
                Box(modifier = Modifier.fillMaxSize().background(DesktopBlush), contentAlignment = Alignment.Center) {
                  Text(TelefamContactsRules.initialsFor(fullName, username), color = DesktopCrimson, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
              }
            }
          }
          Column {
            Text("Change profile photo (DP)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
              text = if (uploadedAvatarMediaId != null) "New photo ready" else "Click avatar to upload & crop",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        DesktopLabeledField("Full name", fullName, { fullName = it.take(64) }, "Full name")

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Username", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (isUsernameLocked) {
              DesktopSvgIcon(DesktopIconKind.LOCK, DesktopCrimson, 13.dp)
            }
          }
          if (isUsernameLocked) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              modifier = Modifier.fillMaxWidth().height(44.dp),
            ) {
              Box(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                Text("@$username", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
              }
            }
            Text(remainingLockLabel, color = DesktopCrimson, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
          } else {
            DesktopCleanTextField(
              value = username,
              onValueChange = { username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' || c == '.' }.take(32) },
              placeholder = "username",
            )
            Text("Username can only be changed once every 2 weeks.", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Phone & Country", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
              onClick = { showCountryPicker = true },
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.background,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              modifier = Modifier.height(44.dp),
            ) {
              Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                Text("${selectedCountry.flagEmoji} ${selectedCountry.dialCode}", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
              }
            }
            Box(modifier = Modifier.weight(1f)) {
              DesktopCleanTextField(
                value = phoneDigits,
                onValueChange = { phoneDigits = it.filter { c -> c.isDigit() }.take(15) },
                placeholder = "Phone number",
              )
            }
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Gender", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Female", "Male", "Non-binary", "Prefer not to say").forEach { opt ->
              val sel = gender == opt
              Surface(
                onClick = { gender = opt },
                shape = RoundedCornerShape(18.dp),
                color = if (sel) DesktopCrimson else MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, if (sel) DesktopCrimson else MaterialTheme.colorScheme.outlineVariant),
              ) {
                Text(
                  text = opt,
                  color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface,
                  fontSize = 12.5.sp,
                  fontWeight = FontWeight.Medium,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
              }
            }
          }
        }

        DesktopLabeledField("Date of birth (YYYY-MM-DD)", dob, { dob = it.take(10) }, "2000-05-15")
        DesktopLabeledField("Location", location, { location = it.take(80) }, "City, Country")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          DesktopLabeledField("Bio", bio, { bio = it.take(150) }, "About you...")
          Text("${bio.length}/150", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.End))
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
          Text("Cancel")
        }
        Button(
          enabled = !isSaving,
          onClick = {
            if (fullName.isBlank() || location.isBlank()) {
              onToast("Full name and location are required.")
              return@Button
            }
            if (!DesktopCountryCatalog.isAtLeast13(dob)) {
              onToast("You must be at least 13 years old.")
              return@Button
            }
            isSaving = true
            scope.launch {
              val finalUser = if (isUsernameLocked) profile.username else username.trim().lowercase()
              val phoneE164 = if (phoneDigits.isBlank()) profile.phoneNumber else "${selectedCountry.dialCode}$phoneDigits"
              val res = withContext(Dispatchers.IO) {
                apiClient.updateProfile(
                  fullName = fullName.trim(),
                  username = finalUser,
                  phoneNumber = phoneE164,
                  countryIso = selectedCountry.iso,
                  gender = gender,
                  dateOfBirth = dob.trim(),
                  location = location.trim(),
                  bio = bio.trim(),
                  avatarMediaId = uploadedAvatarMediaId,
                )
              }
              isSaving = false
              when (res) {
                is DesktopApiOutcome.Ok -> onSaved()
                is DesktopApiOutcome.Fail -> onToast(res.message)
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = DesktopCrimson),
          modifier = Modifier.weight(1f),
        ) {
          Text(if (isSaving) "Saving..." else "Save", color = Color.White, fontWeight = FontWeight.Bold)
        }
      }
    }
  }

  pendingRawAvatarFile?.let { file ->
    DesktopAvatarCropDialog(
      imageFile = file,
      onCancel = { pendingRawAvatarFile = null },
      onConfirm = { croppedBytes, previewBmp, cx, cy, scale ->
        pendingRawAvatarFile = null
        avatarPreviewBitmap = previewBmp
        scope.launch {
          val res = withContext(Dispatchers.IO) { apiClient.uploadAvatar(croppedBytes, cx, cy, scale) }
          when (res) {
            is DesktopApiOutcome.Ok -> uploadedAvatarMediaId = res.data.mediaId
            is DesktopApiOutcome.Fail -> onToast(res.message)
          }
        }
      },
    )
  }

  if (showCountryPicker) {
    DesktopModalOverlay(onDismiss = { showCountryPicker = false }) {
      Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Select country", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        DesktopCleanTextField(countrySearch, { countrySearch = it }, "Search country or dial code")
        val q = countrySearch.trim().lowercase()
        val list = DesktopCountryCatalog.allCountries.filter {
          q.isEmpty() || it.name.lowercase().contains(q) || it.dialCode.contains(q) || it.iso.lowercase().contains(q)
        }
        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
          items(list, key = { it.iso }) { opt ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                  selectedCountry = opt
                  showCountryPicker = false
                }
                .padding(horizontal = 10.dp, vertical = 9.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("${opt.flagEmoji}  ${opt.name}", fontSize = 14.sp, modifier = Modifier.weight(1f))
              Text(opt.dialCode, color = DesktopCrimson, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
            }
          }
        }
      }
    }
  }
}
