package telefam.company.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DesktopChatsSplitScreen(
  controller: DesktopChatController,
  apiClient: DesktopApiClient,
  ownerUserId: String,
  isDark: Boolean,
  chatPrivacy: DesktopChatPrivacySettings,
  searchQuery: String,
  onOpenUserProfile: (String) -> Unit,
  onOpenPrivacyAndThemes: () -> Unit,
  onStartCall: (DesktopChatConversation, DesktopCallType) -> Unit = { _, _ -> },
  onOpenSendStars: (DesktopChatConversation) -> Unit = {},
  onToast: (String) -> Unit,
) {
  val conversations by controller.conversations.collectAsState()
  val archived by controller.archivedConversations.collectAsState()
  val activeConvId by controller.activeConversationId.collectAsState()
  val typingMap by controller.typingModeByConv.collectAsState()
  val searchBundle by controller.searchBundle.collectAsState()
  val undoBanner by controller.undoBanner.collectAsState()

  var showArchived by remember { mutableStateOf(false) }
  val selectedIds = remember { mutableStateListOf<String>() }

  LaunchedEffect(searchQuery) {
    if (searchQuery.isNotBlank()) controller.runGlobalSearch(searchQuery)
  }

  val activeTheme = DesktopChatPrivacyCatalog.themeById(chatPrivacy.chatThemeId)
  val activeBubble = DesktopChatPrivacyCatalog.bubbleColorById(chatPrivacy.bubbleColorId)

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isSplit = maxWidth >= 680.dp

    if (isSplit) {
      Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.width(340.dp).fillMaxHeight()) {
          DesktopConversationListPane(
            conversations = if (showArchived) archived else conversations,
            activeConvId = activeConvId,
            showArchived = showArchived,
            searchQuery = searchQuery,
            searchBundle = searchBundle,
            typingMap = typingMap,
            selectedIds = selectedIds,
            apiClient = apiClient,
            ownerUserId = ownerUserId,
            onToggleArchived = { showArchived = !showArchived },
            onSelectConversation = { cid -> controller.openConversation(cid) },
            onSelectPerson = { p ->
              controller.openDirectConversationByUser(p.userId, p.fullName, p.username, p.avatarUrl) {}
            },
            onToggleSelection = { cid ->
              if (selectedIds.contains(cid)) selectedIds.remove(cid) else selectedIds.add(cid)
            },
            onBatchAction = { action ->
              controller.batchConversationAction(selectedIds.toSet(), action)
              selectedIds.clear()
            },
            onClearSelection = { selectedIds.clear() },
            undoBanner = undoBanner,
            onUndo = { controller.undoBatchAction() },
            onDismissUndo = { controller.dismissUndoBanner() },
          )
        }
        Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
          val curConv = (conversations + archived).firstOrNull { it.conversationId == activeConvId }
          if (curConv != null) {
            DesktopOneOnOneChatPane(
              conv = curConv,
              allConvs = conversations,
              controller = controller,
              apiClient = apiClient,
              ownerUserId = ownerUserId,
              isDark = isDark,
              activeTheme = activeTheme,
              activeBubble = activeBubble,
              typingMode = typingMap[curConv.conversationId].orEmpty(),
              showBackButton = false,
              onBack = { controller.closeActiveConversation() },
              onOpenUserProfile = onOpenUserProfile,
              onOpenPrivacyAndThemes = onOpenPrivacyAndThemes,
              onStartCall = { type -> onStartCall(curConv, type) },
              onOpenSendStars = { onOpenSendStars(curConv) },
              onToast = onToast,
            )
          } else {
            DesktopChatEmptyPlaceholder(activeTheme, activeBubble, isDark, onOpenPrivacyAndThemes)
          }
        }
      }
    } else {
      val curConv = (conversations + archived).firstOrNull { it.conversationId == activeConvId }
      if (curConv != null) {
        DesktopOneOnOneChatPane(
          conv = curConv,
          allConvs = conversations,
          controller = controller,
          apiClient = apiClient,
          ownerUserId = ownerUserId,
          isDark = isDark,
          activeTheme = activeTheme,
          activeBubble = activeBubble,
          typingMode = typingMap[curConv.conversationId].orEmpty(),
          showBackButton = true,
          onBack = { controller.closeActiveConversation() },
          onOpenUserProfile = onOpenUserProfile,
          onOpenPrivacyAndThemes = onOpenPrivacyAndThemes,
          onStartCall = { type -> onStartCall(curConv, type) },
          onOpenSendStars = { onOpenSendStars(curConv) },
          onToast = onToast,
        )
      } else {
        DesktopConversationListPane(
          conversations = if (showArchived) archived else conversations,
          activeConvId = activeConvId,
          showArchived = showArchived,
          searchQuery = searchQuery,
          searchBundle = searchBundle,
          typingMap = typingMap,
          selectedIds = selectedIds,
          apiClient = apiClient,
          ownerUserId = ownerUserId,
          onToggleArchived = { showArchived = !showArchived },
          onSelectConversation = { cid -> controller.openConversation(cid) },
          onSelectPerson = { p ->
            controller.openDirectConversationByUser(p.userId, p.fullName, p.username, p.avatarUrl) {}
          },
          onToggleSelection = { cid ->
            if (selectedIds.contains(cid)) selectedIds.remove(cid) else selectedIds.add(cid)
          },
          onBatchAction = { action ->
            controller.batchConversationAction(selectedIds.toSet(), action)
            selectedIds.clear()
          },
          onClearSelection = { selectedIds.clear() },
          undoBanner = undoBanner,
          onUndo = { controller.undoBatchAction() },
          onDismissUndo = { controller.dismissUndoBanner() },
        )
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DesktopConversationListPane(
  conversations: List<DesktopChatConversation>,
  activeConvId: String?,
  showArchived: Boolean,
  searchQuery: String,
  searchBundle: DesktopChatSearchBundle,
  typingMap: Map<String, String>,
  selectedIds: List<String>,
  apiClient: DesktopApiClient,
  ownerUserId: String,
  onToggleArchived: () -> Unit,
  onSelectConversation: (String) -> Unit,
  onSelectPerson: (DesktopChatSearchPerson) -> Unit,
  onToggleSelection: (String) -> Unit,
  onBatchAction: (String) -> Unit,
  onClearSelection: () -> Unit,
  undoBanner: DesktopChatUndoBanner?,
  onUndo: () -> Unit,
  onDismissUndo: () -> Unit,
) {
  Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Column(modifier = Modifier.fillMaxSize()) {
      if (selectedIds.isNotEmpty()) {
        Surface(color = DesktopCrimson.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text("${selectedIds.size} selected", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Surface(onClick = { onBatchAction(if (showArchived) "unarchive" else "archive") }, shape = CircleShape, color = Color.Transparent, modifier = Modifier.size(32.dp)) {
              Box(contentAlignment = Alignment.Center) { DesktopChatVectorIcon(DesktopChatSvg.ARCHIVE, MaterialTheme.colorScheme.onSurface, 16.dp) }
            }
            Surface(onClick = { onBatchAction("mute") }, shape = CircleShape, color = Color.Transparent, modifier = Modifier.size(32.dp)) {
              Box(contentAlignment = Alignment.Center) { DesktopChatVectorIcon(DesktopChatSvg.MUTE, MaterialTheme.colorScheme.onSurface, 16.dp) }
            }
            Surface(onClick = { onBatchAction("mark_unread") }, shape = CircleShape, color = Color.Transparent, modifier = Modifier.size(32.dp)) {
              Box(contentAlignment = Alignment.Center) { DesktopChatVectorIcon(DesktopChatSvg.MARK_UNREAD, DesktopCrimson, 14.dp) }
            }
            Surface(onClick = { onBatchAction("delete") }, shape = CircleShape, color = Color.Transparent, modifier = Modifier.size(32.dp)) {
              Box(contentAlignment = Alignment.Center) { DesktopChatVectorIcon(DesktopChatSvg.TRASH, DesktopCrimson, 16.dp) }
            }
            Surface(onClick = onClearSelection, shape = CircleShape, color = Color.Transparent, modifier = Modifier.size(32.dp)) {
              Box(contentAlignment = Alignment.Center) { DesktopChatVectorIcon(DesktopChatSvg.CLOSE, MaterialTheme.colorScheme.onSurfaceVariant, 15.dp) }
            }
          }
        }
      } else {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(if (showArchived) "Archived Chats" else "Messages", fontWeight = FontWeight.Bold, fontSize = 16.sp)
          Surface(
            onClick = onToggleArchived,
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          ) {
            Text(
              text = if (showArchived) "All chats" else "Archived",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      if (searchQuery.isNotBlank()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
          if (searchBundle.conversations.isNotEmpty()) {
            item {
              Text("Conversations", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp, 8.dp))
            }
            items(searchBundle.conversations, key = { "sc_${it.conversationId}" }) { c ->
              DesktopConversationRow(
                conv = c,
                isActive = c.conversationId == activeConvId,
                isSelected = selectedIds.contains(c.conversationId),
                selectionMode = selectedIds.isNotEmpty(),
                typingMode = typingMap[c.conversationId].orEmpty(),
                ownerUserId = ownerUserId,
                apiClient = apiClient,
                onClick = { onSelectConversation(c.conversationId) },
                onLongClick = { onToggleSelection(c.conversationId) },
              )
            }
          }
          if (searchBundle.matchingMessages.isNotEmpty()) {
            item {
              Text("Messages", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp, 8.dp))
            }
            items(searchBundle.matchingMessages, key = { "sm_${it.messageId}" }) { m ->
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onSelectConversation(m.conversationId) }
                  .padding(horizontal = 14.dp, vertical = 8.dp),
              ) {
                Text(m.plainText, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                Text("Open conversation", fontSize = 11.sp, color = DesktopCrimson)
              }
            }
          }
          if (searchBundle.people.isNotEmpty()) {
            item {
              Text("People", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp, 8.dp))
            }
            items(searchBundle.people, key = { "sp_${it.userId}" }) { person ->
              Row(
                modifier = Modifier.fillMaxWidth().clickable { onSelectPerson(person) }.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                DesktopChatAvatar(person.avatarUrl, person.fullName.ifBlank { person.username }, apiClient, 38.dp)
                Column {
                  Text(person.fullName.ifBlank { person.username }, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                  Text("@${person.username}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
              }
            }
          }
        }
      } else if (conversations.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
          Text(
            text = if (showArchived) "No archived chats" else "No conversations yet. Search for people above or open a profile to message.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
          )
        }
      } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
          items(conversations, key = { it.conversationId }) { conv ->
            DesktopConversationRow(
              conv = conv,
              isActive = conv.conversationId == activeConvId,
              isSelected = selectedIds.contains(conv.conversationId),
              selectionMode = selectedIds.isNotEmpty(),
              typingMode = typingMap[conv.conversationId].orEmpty(),
              ownerUserId = ownerUserId,
              apiClient = apiClient,
              onClick = {
                if (selectedIds.isNotEmpty()) onToggleSelection(conv.conversationId)
                else onSelectConversation(conv.conversationId)
              },
              onLongClick = { onToggleSelection(conv.conversationId) },
            )
          }
        }
      }
    }

    if (undoBanner != null) {
      Surface(
        color = Color(0xFF1E222A),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp).fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(undoBanner.message, color = Color.White, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
          TextButton(onClick = onUndo) { Text("UNDO", color = Color(0xFFFF8A80), fontWeight = FontWeight.Bold) }
          TextButton(onClick = onDismissUndo) { Text("OK", color = Color.White.copy(alpha = 0.7f)) }
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DesktopConversationRow(
  conv: DesktopChatConversation,
  isActive: Boolean,
  isSelected: Boolean,
  selectionMode: Boolean,
  typingMode: String,
  ownerUserId: String,
  apiClient: DesktopApiClient,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
) {
  val bg = when {
    isSelected -> DesktopCrimson.copy(alpha = 0.14f)
    isActive -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    else -> Color.Transparent
  }
  val timeLabel = remember(conv.lastMessageAtMs) {
    if (conv.lastMessageAtMs <= 0L) "" else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(conv.lastMessageAtMs))
  }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(bg)
      .combinedClickable(onClick = onClick, onLongClick = onLongClick)
      .padding(horizontal = 14.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    if (selectionMode) {
      Checkbox(checked = isSelected, onCheckedChange = { onLongClick() })
    }
    Box {
      DesktopChatAvatar(conv.peerAvatarUrl, conv.peerFullName.ifBlank { conv.peerUsername }, apiClient, 44.dp)
      if (conv.isOnline && conv.capabilities.showOnlineIndicator) {
        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .size(12.dp)
            .clip(CircleShape)
            .background(Color(0xFF22C55E))
            .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
        )
      }
    }
    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = conv.peerFullName.ifBlank { conv.peerUsername.ifBlank { "Telefam" } },
          fontWeight = FontWeight.SemiBold,
          fontSize = 14.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        if (conv.isMuted) DesktopChatVectorIcon(DesktopChatSvg.MUTE, MaterialTheme.colorScheme.onSurfaceVariant, 13.dp)
        Text(timeLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Spacer(modifier = Modifier.height(2.dp))
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        val subtitle = when (typingMode) {
          "recording" -> "recording voice..."
          "typing" -> "typing..."
          else -> conv.lastMessagePreviewPlain.ifBlank { "Tap to chat" }
        }
        val subColor = if (typingMode == "typing" || typingMode == "recording") DesktopCrimson else MaterialTheme.colorScheme.onSurfaceVariant
        if (conv.lastSenderId == ownerUserId && conv.lastMessageId.isNotBlank() && typingMode.isBlank()) {
          DesktopDeliveryStatusIcon(conv.lastDeliveryStatus, conv.peerAvatarUrl, apiClient)
        }
        Text(subtitle, color = subColor, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (conv.unreadCount > 0) {
          Surface(shape = CircleShape, color = DesktopCrimson) {
            Text(
              text = conv.unreadCount.toString(),
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DesktopChatEmptyPlaceholder(
  activeTheme: DesktopChatThemeSpec,
  activeBubble: DesktopChatBubbleColorOption,
  isDark: Boolean,
  onOpenPrivacyAndThemes: () -> Unit,
) {
  val bubbleAccent = Color(if (isDark) activeBubble.darkStartHex else activeBubble.lightStartHex)
  Box(modifier = Modifier.fillMaxSize()) {
    DesktopChatWallpaperCanvas(theme = activeTheme, isDark = isDark, modifier = Modifier.fillMaxSize())
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
      modifier = Modifier.align(Alignment.Center).widthIn(max = 380.dp).padding(24.dp),
    ) {
      Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Select a conversation", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          "Theme: ${activeTheme.title} | Bubble: ${activeBubble.label}",
          fontSize = 12.5.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(
          onClick = onOpenPrivacyAndThemes,
          colors = ButtonDefaults.buttonColors(containerColor = bubbleAccent),
        ) {
          Text("Privacy & Themes", color = Color.White)
        }
      }
    }
  }
}

@Composable
private fun DesktopOneOnOneChatPane(
  conv: DesktopChatConversation,
  allConvs: List<DesktopChatConversation>,
  controller: DesktopChatController,
  apiClient: DesktopApiClient,
  ownerUserId: String,
  isDark: Boolean,
  activeTheme: DesktopChatThemeSpec,
  activeBubble: DesktopChatBubbleColorOption,
  typingMode: String,
  showBackButton: Boolean,
  onBack: () -> Unit,
  onOpenUserProfile: (String) -> Unit,
  onOpenPrivacyAndThemes: () -> Unit,
  onStartCall: (DesktopCallType) -> Unit = {},
  onOpenSendStars: () -> Unit = {},
  onToast: (String) -> Unit,
) {
  val messages by controller.activeMessages.collectAsState()
  val flags by controller.activeSecurityFlags.collectAsState()
  val greeting by controller.greetingSticker.collectAsState()
  val dissolving by controller.dissolvingMessageIds.collectAsState()
  val playingMsgId by controller.playingMessageId.collectAsState()
  val activeViewOnce by controller.activeViewOnceMsg.collectAsState()
  val viewOnceRemaining by controller.viewOnceRemainingSec.collectAsState()

  val bubbleGradient = if (isDark) {
    listOf(Color(activeBubble.darkStartHex), Color(activeBubble.darkEndHex))
  } else {
    listOf(Color(activeBubble.lightStartHex), Color(activeBubble.lightEndHex))
  }
  val accentColor = bubbleGradient.first()

  var composerText by remember(conv.conversationId) { mutableStateOf(conv.draftText) }
  var editingMsg by remember(conv.conversationId) { mutableStateOf<DesktopChatMessage?>(null) }
  var forwardMsg by remember { mutableStateOf<DesktopChatMessage?>(null) }
  var showOverflowMenu by remember { mutableStateOf(false) }
  var showMediaDialog by remember { mutableStateOf(false) }
  var showStickerPicker by remember(conv.conversationId) { mutableStateOf(false) }

  // Voice recording & preview state
  var isRecording by remember(conv.conversationId) { mutableStateOf(false) }
  var previewVoiceFile by remember(conv.conversationId) { mutableStateOf<File?>(null) }
  var previewVoiceDurationMs by remember(conv.conversationId) { mutableStateOf(0L) }
  var viewOnceEnabled by remember(conv.conversationId) { mutableStateOf(false) }
  var viewOnceTtlSec by remember(conv.conversationId) { mutableStateOf(0) } // 0 = Play Once

  val listState = rememberLazyListState()
  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.lastIndex)
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    DesktopChatWallpaperCanvas(theme = activeTheme, isDark = isDark, modifier = Modifier.fillMaxSize())

    Column(modifier = Modifier.fillMaxSize()) {
      // Translucent top header pills
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (showBackButton) {
          Surface(
            onClick = onBack,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            modifier = Modifier.size(40.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              DesktopChatVectorIcon(DesktopChatSvg.BACK, MaterialTheme.colorScheme.onSurface, 18.dp)
            }
          }
        }
        Surface(
          onClick = { onOpenUserProfile(conv.peerUserId) },
          shape = RoundedCornerShape(24.dp),
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
          modifier = Modifier.weight(1f),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            DesktopChatAvatar(conv.peerAvatarUrl, conv.peerFullName.ifBlank { conv.peerUsername }, apiClient, 34.dp)
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = conv.peerFullName.ifBlank { conv.peerUsername },
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              val statusText = when {
                conv.capabilities.isBlocked -> "Blocked"
                typingMode == "recording" -> "recording voice note..."
                typingMode == "typing" -> "typing..."
                conv.isOnline && conv.capabilities.showOnlineIndicator -> "online"
                else -> conv.lastSeenLabel.ifBlank { "last seen recently" }
              }
              val statusColor = if (typingMode.isNotBlank() || (conv.isOnline && conv.capabilities.showOnlineIndicator)) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
              Text(statusText, fontSize = 11.5.sp, color = statusColor, maxLines = 1)
            }
          }
        }

        Surface(
          shape = RoundedCornerShape(22.dp),
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        ) {
          Row(modifier = Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
              onClick = { onStartCall(DesktopCallType.AUDIO) },
              shape = CircleShape,
              color = Color.Transparent,
              modifier = Modifier.size(48.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                DesktopChatVectorIcon(DesktopChatSvg.PHONE_CALL, MaterialTheme.colorScheme.onSurface, 18.dp)
              }
            }
            Surface(
              onClick = { onStartCall(DesktopCallType.VIDEO) },
              shape = CircleShape,
              color = Color.Transparent,
              modifier = Modifier.size(48.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                DesktopChatVectorIcon(DesktopChatSvg.VIDEO_CAM, MaterialTheme.colorScheme.onSurface, 18.dp)
              }
            }
            Box {
              Surface(
                onClick = { showOverflowMenu = true },
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier.size(38.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  DesktopChatVectorIcon(DesktopChatSvg.MORE_VERT, MaterialTheme.colorScheme.onSurface, 17.dp)
                }
              }
              DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                DropdownMenuItem(text = { Text("View Profile") }, onClick = { showOverflowMenu = false; onOpenUserProfile(conv.peerUserId) })
                DropdownMenuItem(text = { Text("Media, Links & Docs") }, onClick = { showOverflowMenu = false; showMediaDialog = true })
                DropdownMenuItem(
                  text = { Text(if (conv.isMuted) "Unmute" else "Mute") },
                  onClick = {
                    showOverflowMenu = false
                    controller.batchConversationAction(setOf(conv.conversationId), if (conv.isMuted) "unmute" else "mute")
                  },
                )
                DropdownMenuItem(text = { Text("Themes & Privacy") }, onClick = { showOverflowMenu = false; onOpenPrivacyAndThemes() })
                DropdownMenuItem(
                  text = { Text(if (conv.capabilities.isBlocked) "Blocked" else "Block User", color = DesktopCrimson) },
                  onClick = {
                    showOverflowMenu = false
                    if (!conv.capabilities.isBlocked) controller.batchConversationAction(setOf(conv.conversationId), "block")
                  },
                )
              }
            }
          }
        }
      }

      if (flags.isNotEmpty()) {
        val latest = flags.first()
        Surface(
          color = DesktopCrimson.copy(alpha = 0.16f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            DesktopChatVectorIcon(DesktopChatSvg.SHIELD_LOCK, DesktopCrimson, 14.dp)
            Text(latest.details.ifBlank { latest.flagType }, color = DesktopCrimson, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
          }
        }
      }

      // Chat Messages Area
      Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        if (messages.isEmpty()) {
          Surface(
            color = Color.Black.copy(alpha = if (isDark) 0.36f else 0.28f),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.align(Alignment.Center).widthIn(max = 280.dp),
          ) {
            Column(
              modifier = Modifier.padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text("No messages here yet...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                "Send a message or tap\nthe greeting below.",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.5.sp,
                textAlign = TextAlign.Center,
              )
              Spacer(modifier = Modifier.height(14.dp))
              val gr = greeting
              if (gr != null && gr.bestAnimatedUrl.isNotBlank()) {
                DesktopAnimatedGiflyMedia(
                  apiClient = apiClient,
                  url = gr.bestAnimatedUrl,
                  contentDescription = "Greeting sticker",
                  modifier = Modifier.size(144.dp).clickable {
                    controller.sendGiflyMediaMessage(conv.conversationId, conv.peerUserId, gr, DesktopGiflyTab.STICKERS)
                  },
                )
              } else {
                Surface(
                  onClick = { controller.sendTextMessage(conv.conversationId, conv.peerUserId, "Hello!") },
                  shape = CircleShape,
                  color = accentColor,
                ) {
                  Text("Say Hello", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp))
                }
              }
            }
          }
        } else {
          LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = 10.dp),
            modifier = Modifier.fillMaxSize(),
          ) {
            items(messages, key = { it.messageId }) { msg ->
              DesktopChatMessageBubble(
                msg = msg,
                isOwn = msg.senderUserId == ownerUserId,
                isDissolving = dissolving.contains(msg.messageId),
                isPlayingVoice = playingMsgId == msg.messageId,
                bubbleGradient = bubbleGradient,
                canCopy = conv.capabilities.canCopyMessages,
                canForward = conv.capabilities.canShareChats,
                peerName = conv.peerFullName,
                peerAvatarUrl = conv.peerAvatarUrl,
                apiClient = apiClient,
                onToggleVoice = { controller.togglePlayVoice(msg) },
                onCopy = { onToast("Copied message") },
                onForward = { forwardMsg = msg },
                onEdit = {
                  editingMsg = msg
                  composerText = msg.plainText
                },
                onUnsend = { controller.unsendMessage(msg) },
                onDeleteForMe = { controller.deleteMessageForSelf(msg) },
                onCallBack = { callType -> onStartCall(callType) },
              )
            }
          }
        }
      }

      if (showStickerPicker) {
        DesktopGiflyPickerPanel(
          controller = controller,
          apiClient = apiClient,
          accentColor = accentColor,
          onSendItem = { item, tab ->
            showStickerPicker = false
            controller.sendGiflyMediaMessage(conv.conversationId, conv.peerUserId, item, tab)
          },
          onClose = { showStickerPicker = false },
        )
      }

      val editTarget = editingMsg
      if (editTarget != null) {
        Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f), modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            DesktopChatVectorIcon(DesktopChatSvg.EDIT, accentColor, 15.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Editing: ${editTarget.plainText}", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
            Surface(onClick = { editingMsg = null; composerText = "" }, shape = CircleShape, color = Color.Transparent, modifier = Modifier.size(26.dp)) {
              Box(contentAlignment = Alignment.Center) { DesktopChatVectorIcon(DesktopChatSvg.CLOSE, MaterialTheme.colorScheme.onSurfaceVariant, 14.dp) }
            }
          }
        }
      }

      val voicePreview = previewVoiceFile
      if (voicePreview != null) {
        Surface(
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
          shape = RoundedCornerShape(16.dp),
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Text("Voice note · ${(previewVoiceDurationMs / 1000L).coerceAtLeast(1L)}s", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
              Text("View once", fontSize = 12.sp)
              Switch(checked = viewOnceEnabled, onCheckedChange = { viewOnceEnabled = it })
            }
            if (viewOnceEnabled) {
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0 to "Play once", 5 to "5s", 10 to "10s", 30 to "30s", 60 to "60s").forEach { (sec, label) ->
                  val sel = viewOnceTtlSec == sec
                  Surface(
                    onClick = { viewOnceTtlSec = sec },
                    shape = RoundedCornerShape(12.dp),
                    color = if (sel) accentColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  ) {
                    Text(label, color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 11.5.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                  }
                }
              }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = {
                  runCatching { voicePreview.delete() }
                  previewVoiceFile = null
                },
                modifier = Modifier.weight(1f),
              ) { Text("Discard") }
              Button(
                onClick = {
                  controller.sendVoiceMessage(
                    conversationId = conv.conversationId,
                    peerUserId = conv.peerUserId,
                    wavFile = voicePreview,
                    durationMs = previewVoiceDurationMs,
                    isViewOnce = viewOnceEnabled,
                    ttlSec = viewOnceTtlSec,
                  )
                  previewVoiceFile = null
                  viewOnceEnabled = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier.weight(1f),
              ) { Text("Send Voice", color = Color.White) }
            }
          }
        }
      }

      // Composer bar (Blocked disables composer and shows crisp notice directly below)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
          .padding(horizontal = 12.dp, vertical = 8.dp),
      ) {
        val isTyping = composerText.isNotEmpty()
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Surface(
            onClick = { if (!conv.capabilities.isBlocked) showStickerPicker = !showStickerPicker },
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier.size(38.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              DesktopChatVectorIcon(DesktopChatSvg.STICKER_SMILE, if (showStickerPicker) accentColor else MaterialTheme.colorScheme.onSurfaceVariant, 20.dp)
            }
          }

          Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
            modifier = Modifier.weight(1f).height(42.dp),
          ) {
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
              if (composerText.isEmpty()) {
                Text(
                  text = if (isRecording) "Recording voice note..." else "Message",
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 14.sp,
                )
              }
              BasicTextField(
                value = composerText,
                onValueChange = { next ->
                  if (!conv.capabilities.isBlocked) {
                    composerText = next
                    controller.notifyTyping(conv.conversationId, if (next.isNotBlank()) "typing" else "idle")
                  }
                },
                enabled = !conv.capabilities.isBlocked && !isRecording,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                cursorBrush = SolidColor(accentColor),
                modifier = Modifier.fillMaxWidth().onPreviewKeyEvent { ev ->
                  if (ev.type == KeyEventType.KeyDown && ev.key == Key.Enter && !ev.isShiftPressed && composerText.isNotBlank()) {
                    val curEdit = editingMsg
                    if (curEdit != null) {
                      controller.editMessage(curEdit, composerText)
                      editingMsg = null
                    } else {
                      controller.sendTextMessage(conv.conversationId, conv.peerUserId, composerText)
                    }
                    composerText = ""
                    controller.notifyTyping(conv.conversationId, "idle")
                    true
                  } else false
                },
              )
            }
          }

          // Hide Gift & Paperclip while typing to maximize message space
          if (!isTyping && !isRecording) {
            Surface(
              onClick = { if (!conv.capabilities.isBlocked) onOpenSendStars() },
              shape = CircleShape,
              color = Color.Transparent,
              modifier = Modifier.size(38.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                DesktopChatVectorIcon(DesktopChatSvg.GIFT, MaterialTheme.colorScheme.onSurfaceVariant, 19.dp)
              }
            }
            Surface(
              onClick = { onToast("File attachments — Coming soon") },
              shape = CircleShape,
              color = Color.Transparent,
              modifier = Modifier.size(38.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                DesktopChatVectorIcon(DesktopChatSvg.PAPERCLIP, MaterialTheme.colorScheme.onSurfaceVariant, 19.dp)
              }
            }
          }

          Surface(
            onClick = {
              if (conv.capabilities.isBlocked) return@Surface
              if (isTyping) {
                val curEdit = editingMsg
                if (curEdit != null) {
                  controller.editMessage(curEdit, composerText)
                  editingMsg = null
                } else {
                  controller.sendTextMessage(conv.conversationId, conv.peerUserId, composerText)
                }
                composerText = ""
                controller.notifyTyping(conv.conversationId, "idle")
              } else if (isRecording) {
                val (file, dur) = controller.audioEngine.stopRecording(discard = false)
                isRecording = false
                controller.notifyTyping(conv.conversationId, "idle")
                if (file != null) {
                  previewVoiceFile = file
                  previewVoiceDurationMs = dur
                } else {
                  onToast("Microphone audio capture unavailable on this device")
                }
              } else {
                val started = controller.audioEngine.startRecording {}
                if (started != null) {
                  isRecording = true
                  controller.notifyTyping(conv.conversationId, "recording")
                } else {
                  onToast("No audio input microphone detected")
                }
              }
            },
            shape = CircleShape,
            color = if (isRecording) DesktopCrimson else accentColor,
            modifier = Modifier.size(40.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              DesktopChatVectorIcon(
                kind = when {
                  isTyping -> DesktopChatSvg.SEND_PLANE
                  isRecording -> DesktopChatSvg.PAUSE
                  else -> DesktopChatSvg.MIC
                },
                tint = Color.White,
                size = 18.dp,
              )
            }
          }
        }

        if (conv.capabilities.isBlocked) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "You cannot message this user",
            color = DesktopCrimson,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }

    // View-Once Voice Auto-Burn Modal Overlay
    val activeVo = activeViewOnce
    if (activeVo != null) {
      Surface(
        color = Color.Black.copy(alpha = 0.86f),
        modifier = Modifier.fillMaxSize(),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF14171E),
            modifier = Modifier.widthIn(max = 340.dp).padding(24.dp),
          ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              DesktopChatVectorIcon(DesktopChatSvg.EYE_ONCE, accentColor, 34.dp)
              Spacer(modifier = Modifier.height(10.dp))
              Text("View-Once Voice Note", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = if (viewOnceRemaining > 0) "Auto-burning in ${viewOnceRemaining}s" else "Will delete permanently when finished",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
              )
              Spacer(modifier = Modifier.height(18.dp))
              Button(
                onClick = { controller.consumeViewOnceVoice(activeVo) },
                colors = ButtonDefaults.buttonColors(containerColor = DesktopCrimson),
              ) {
                Text("Burn & Close", color = Color.White)
              }
            }
          }
        }
      }
    }

    val fwd = forwardMsg
    if (fwd != null) {
      AlertDialog(
        onDismissRequest = { forwardMsg = null },
        title = { Text("Forward message") },
        text = {
          LazyColumn(modifier = Modifier.height(260.dp)) {
            items(allConvs, key = { it.conversationId }) { target ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    controller.sendTextMessage(target.conversationId, target.peerUserId, fwd.plainText, forwardedFrom = fwd.senderUserId)
                    forwardMsg = null
                    onToast("Forwarded to ${target.peerFullName.ifBlank { target.peerUsername }}")
                  }
                  .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                DesktopChatAvatar(target.peerAvatarUrl, target.peerFullName.ifBlank { target.peerUsername }, apiClient, 36.dp)
                Text(target.peerFullName.ifBlank { target.peerUsername }, fontWeight = FontWeight.SemiBold)
              }
            }
          }
        },
        confirmButton = { TextButton(onClick = { forwardMsg = null }) { Text("Cancel") } },
      )
    }

    if (showMediaDialog) {
      val smartController = LocalDesktopSmartActions.current
      val mediaItems = messages.filter { it.messageType != "text" }
      val extractedLinks = messages
        .flatMap { msg ->
          telefam.company.shared.SmartTextDetector.analyze(msg.plainText).urls.map { msg.messageId to it }
        }
        .distinctBy { it.second.normalizedUrl }
      AlertDialog(
        onDismissRequest = { showMediaDialog = false },
        title = { Text("Media, Links & Docs") },
        text = {
          if (mediaItems.isEmpty() && extractedLinks.isEmpty()) {
            Text("No shared media or links in this conversation.")
          } else {
            LazyColumn(modifier = Modifier.height(260.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              if (extractedLinks.isNotEmpty()) {
                item {
                  Text("Links", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DesktopCrimson)
                }
                items(extractedLinks, key = { "${it.first}_${it.second.normalizedUrl}" }) { (_, entity) ->
                  Surface(
                    onClick = {
                      showMediaDialog = false
                      smartController?.openUrl(entity.normalizedUrl, "chat")
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      DesktopSmartSvgIcon(DesktopSmartSvg.GLOBE, DesktopCrimson, 16.dp)
                      Column(modifier = Modifier.weight(1f)) {
                        Text(entity.domain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(entity.normalizedUrl, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                      }
                    }
                  }
                }
              }
              if (mediaItems.isNotEmpty()) {
                item {
                  Text("Media", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DesktopCrimson)
                }
                items(mediaItems, key = { it.messageId }) { m ->
                  Text("${m.messageType.uppercase()}: ${m.plainText.ifBlank { m.mediaUrl }}", fontSize = 12.5.sp, modifier = Modifier.padding(vertical = 2.dp))
                }
              }
            }
          }
        },
        confirmButton = { TextButton(onClick = { showMediaDialog = false }) { Text("Done") } },
      )
    }
  }
}
