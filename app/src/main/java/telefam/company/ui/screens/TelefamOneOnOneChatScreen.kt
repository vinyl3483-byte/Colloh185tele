package telefam.company.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PermMedia
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.telefam.company.app.R
import telefam.company.calls.TelefamCallType
import telefam.company.chat.TelefamChatUiState
import telefam.company.core.ChatPrivacySettingsState
import telefam.company.core.TelefamChatPrivacyCatalog
import telefam.company.core.TelefamContactsRules
import telefam.company.data.local.ChatConversationEntity
import telefam.company.data.local.ChatMessageEntity
import telefam.company.data.network.GiflyMediaItemDto
import telefam.company.ui.theme.LocalTelefamDarkTheme
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamPureWhite

@Composable
fun TelefamOneOnOneChatScreen(
  conversation: ChatConversationEntity,
  chatState: TelefamChatUiState,
  chatPrivacy: ChatPrivacySettingsState,
  currentUserId: String,
  onBack: () -> Unit,
  onOpenPeerProfile: (String) -> Unit,
  onComposerTextChanged: (String) -> Unit,
  onSendTextOrEdit: () -> Unit,
  onSendGreetingSticker: () -> Unit,
  onStartEditMessage: (ChatMessageEntity) -> Unit,
  onCancelEditMessage: () -> Unit,
  onUnsendMessage: (ChatMessageEntity) -> Unit,
  onDeleteMessageForSelf: (ChatMessageEntity) -> Unit,
  onStartForwardMessage: (ChatMessageEntity) -> Unit,
  onDismissForwardMessage: () -> Unit,
  onConfirmForwardMessage: (ChatConversationEntity) -> Unit,
  onStartVoiceRecording: () -> Unit,
  onStopVoiceRecording: () -> Unit,
  onDiscardVoiceRecording: () -> Unit,
  onToggleVoicePlayback: (String, String) -> Unit,
  onToggleVoicePreviewViewOnce: (Boolean) -> Unit,
  onSelectVoicePreviewTtl: (Int) -> Unit,
  onSendVoiceNote: () -> Unit,
  onOpenViewOnceVoice: (ChatMessageEntity) -> Unit,
  onBurnViewOnceVoice: () -> Unit,
  onOpenGiflyPicker: () -> Unit,
  onCloseGiflyPicker: () -> Unit,
  onSelectGiflyTab: (String) -> Unit,
  onGiflyQueryChanged: (String) -> Unit,
  onLoadMoreGifly: () -> Unit,
  onPreviewGiflyItem: (GiflyMediaItemDto?) -> Unit,
  onSendPreviewGifly: () -> Unit,
  onToggleMuteConversation: () -> Unit,
  onBlockPeer: () -> Unit,
  onReportPeer: (String, String) -> Unit,
  onOpenSharedMediaTab: (String?) -> Unit,
  onTriggerComingSoon: (String, String) -> Unit,
  onDismissToast: () -> Unit,
  onStartAudioCall: () -> Unit = {},
  onStartVideoCall: () -> Unit = {},
  onCallBackFromLog: (TelefamCallType) -> Unit = {},
  showEmbeddedSplitHeader: Boolean = false,
) {
  val context = LocalContext.current
  val isDark = LocalTelefamDarkTheme.current
  val activeTheme = TelefamChatPrivacyCatalog.themeById(chatPrivacy.chatThemeId)
  val activeBubble = TelefamChatPrivacyCatalog.bubbleColorById(chatPrivacy.bubbleColorId)

  val outStart = if (activeTheme.themeId != "none") Color(activeTheme.accentStartHex) else Color(if (isDark) activeBubble.darkStartHex else activeBubble.lightStartHex)
  val outEnd = if (activeTheme.themeId != "none") Color(activeTheme.accentEndHex) else Color(if (isDark) activeBubble.darkEndHex else activeBubble.lightEndHex)

  val pillSurface = if (isDark) Color(0xCC1C1E24) else Color(0xE8FFFFFF)
  val pillContent = if (isDark) TelefamPureWhite else Color(0xFF14151A)
  val pillSecondary = if (isDark) Color(0xFFA0A7B4) else Color(0xFF5D6472)

  var showTopMenu by remember { mutableStateOf(false) }
  var showReportDialog by remember { mutableStateOf(false) }
  var reportDetails by remember { mutableStateOf("") }

  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    if (granted) {
      onStartVoiceRecording()
    } else {
      Toast.makeText(context, "Microphone permission is required for voice messages", Toast.LENGTH_SHORT).show()
    }
  }

  LaunchedEffect(chatState.toastMessage) {
    val msg = chatState.toastMessage
    if (!msg.isNullOrBlank()) {
      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
      onDismissToast()
    }
  }

  BackHandler(enabled = !showEmbeddedSplitHeader || chatState.giflyPickerOpen || chatState.sharedMediaTab != null) {
    when {
      chatState.activeViewOnceOverlay != null -> onBurnViewOnceVoice()
      chatState.giflyPreviewItem != null -> onPreviewGiflyItem(null)
      chatState.giflyPickerOpen -> onCloseGiflyPicker()
      chatState.sharedMediaTab != null -> onOpenSharedMediaTab(null)
      else -> onBack()
    }
  }

  val messages = chatState.activeMessages
  val listState = rememberLazyListState()
  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.lastIndex)
    }
  }

  val typingMode = chatState.typingModesByConversation[conversation.conversationId].orEmpty()
  val subtitleText = when {
    conversation.isBlocked -> "blocked"
    conversation.showTypingIndicator && typingMode == "recording_voice" -> stringResource(R.string.chat_status_recording)
    conversation.showTypingIndicator && typingMode == "typing" -> stringResource(R.string.chat_status_typing)
    conversation.showOnlineIndicator && conversation.isOnline -> stringResource(R.string.chat_status_online)
    conversation.showOnlineIndicator && conversation.lastSeenLabel.isNotBlank() -> conversation.lastSeenLabel
    else -> stringResource(R.string.chat_status_last_seen_recent)
  }

  val canCopy = chatPrivacy.whoCanCopyMessages != telefam.company.core.ChatAudienceOption.NOBODY && conversation.canCopyMessages
  val canForward = chatPrivacy.whoCanShareChats != telefam.company.core.ChatAudienceOption.NOBODY && conversation.canShareChats
  val securityFlags = chatState.activeSecurityFlags

  val composerFocusRequester = remember { FocusRequester() }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .pointerInput(Unit) {
        awaitPointerEventScope {
          while (true) {
            awaitPointerEvent(PointerEventPass.Main).changes.forEach { it.consume() }
          }
        }
      }
      .testTag("one_on_one_chat_screen"),
  ) {
    // Full-bleed Wallpaper matching user's chosen Chats Theme (or Clean fallback)
    ChatWallpaperCanvas(
      theme = activeTheme,
      isDark = isDark,
      modifier = Modifier.fillMaxSize(),
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .imePadding(),
    ) {
      // Top Floating Translucent Pills Header matching Screenshot_20261117_112133_Telegram
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (!showEmbeddedSplitHeader) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(pillSurface)
              .clickable(onClick = onBack)
              .testTag("chat_back_button"),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.cd_back),
              tint = pillContent,
              modifier = Modifier.size(21.dp),
            )
          }
        }

        // Center Capsule: Avatar + Peer Name + Live Status
        Row(
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(pillSurface)
            .clickable { onOpenPeerProfile(conversation.peerUserId) }
            .padding(start = 6.dp, end = 14.dp)
            .testTag("chat_header_peer_pill"),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(modifier = Modifier.size(36.dp)) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(outStart),
              contentAlignment = Alignment.Center,
            ) {
              if (conversation.peerAvatarUrl.isNotBlank()) {
                AsyncImage(
                  model = conversation.peerAvatarUrl,
                  contentDescription = conversation.peerFullName,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize(),
                )
              } else {
                Text(
                  text = TelefamContactsRules.initialsFor(conversation.peerFullName, conversation.peerUsername),
                  color = TelefamPureWhite,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
            if (conversation.showOnlineIndicator && conversation.isOnline) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .align(Alignment.BottomEnd)
                  .clip(CircleShape)
                  .background(Color(0xFF22C55E))
                  .border(1.5.dp, pillSurface, CircleShape),
              )
            }
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = conversation.peerFullName.ifBlank { conversation.peerUsername.ifBlank { "Telefam" } },
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = pillContent,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = subtitleText,
              fontSize = 12.sp,
              color = if (typingMode.isNotBlank() && conversation.showTypingIndicator) outEnd else pillSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.testTag("chat_header_status"),
            )
          }
        }

        // Right Capsule: Audio Call + Video Call + 3-dots menu
        Box {
          Row(
            modifier = Modifier
              .height(44.dp)
              .clip(RoundedCornerShape(22.dp))
              .background(pillSurface)
              .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(
              onClick = onStartAudioCall,
              modifier = Modifier
                .size(38.dp)
                .testTag("chat_audio_call_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Call,
                contentDescription = stringResource(R.string.call_action_audio),
                tint = pillContent,
                modifier = Modifier.size(19.dp),
              )
            }
            IconButton(
              onClick = onStartVideoCall,
              modifier = Modifier
                .size(38.dp)
                .testTag("chat_video_call_button"),
            ) {
              Icon(
                imageVector = Icons.Outlined.Videocam,
                contentDescription = stringResource(R.string.call_action_video),
                tint = pillContent,
                modifier = Modifier.size(20.dp),
              )
            }
            IconButton(
              onClick = { showTopMenu = true },
              modifier = Modifier
                .size(36.dp)
                .testTag("chat_more_menu_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.cd_more_options),
                tint = pillContent,
                modifier = Modifier.size(19.dp),
              )
            }
          }

          DropdownMenu(
            expanded = showTopMenu,
            onDismissRequest = { showTopMenu = false },
          ) {
            DropdownMenuItem(
              text = { Text(stringResource(R.string.chat_action_media)) },
              leadingIcon = { Icon(Icons.Outlined.PermMedia, contentDescription = null, modifier = Modifier.size(18.dp)) },
              onClick = {
                showTopMenu = false
                onOpenSharedMediaTab("media")
              },
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.chat_action_links)) },
              leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(18.dp)) },
              onClick = {
                showTopMenu = false
                onOpenSharedMediaTab("links")
              },
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.chat_action_files_docs)) },
              leadingIcon = { Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(18.dp)) },
              onClick = {
                showTopMenu = false
                onOpenSharedMediaTab("files")
              },
            )
            DropdownMenuItem(
              text = { Text(if (conversation.isMuted) stringResource(R.string.chat_action_unmute) else stringResource(R.string.chat_action_mute)) },
              leadingIcon = { Icon(Icons.Outlined.NotificationsOff, contentDescription = null, modifier = Modifier.size(18.dp)) },
              onClick = {
                showTopMenu = false
                onToggleMuteConversation()
              },
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.chat_action_share_profile)) },
              leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp)) },
              onClick = {
                showTopMenu = false
                val handle = conversation.peerUsername.ifBlank { conversation.peerUserId }
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                  type = "text/plain"
                  putExtra(Intent.EXTRA_TEXT, "Connect with @${handle} on Telefam: https://telefam.company/@$handle")
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share profile"))
              },
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.chat_action_block), color = TelefamCrimson) },
              leadingIcon = { Icon(Icons.Outlined.Block, contentDescription = null, tint = TelefamCrimson, modifier = Modifier.size(18.dp)) },
              onClick = {
                showTopMenu = false
                onBlockPeer()
              },
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.chat_action_report), color = TelefamCrimson) },
              leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null, tint = TelefamCrimson, modifier = Modifier.size(18.dp)) },
              onClick = {
                showTopMenu = false
                showReportDialog = true
              },
            )
          }
        }
      }

      // Persistent Security & Flagged Screen Banner (survives offline restart)
      if (securityFlags.isNotEmpty() || chatPrivacy.blocksScreenshots || !conversation.canScreenshot) {
        val latestFlag = securityFlags.firstOrNull()
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp)
            .testTag("chat_security_flag_banner"),
          shape = RoundedCornerShape(14.dp),
          color = if (latestFlag != null) Color(0xD97F1D1D) else Color(0xAA171A21),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
          ) {
            Icon(
              imageVector = if (latestFlag != null) Icons.Filled.Shield else Icons.Filled.Lock,
              contentDescription = null,
              tint = TelefamPureWhite,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = latestFlag?.details ?: stringResource(R.string.chat_security_screenshot_banner),
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Medium,
              color = TelefamPureWhite,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      // Main Message Area or Empty Greeting Card
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
      ) {
        if (chatState.isLoadingMessages && messages.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = outEnd, modifier = Modifier.size(30.dp))
          }
        } else if (messages.isEmpty()) {
          EmptyChatGreetingCard(
            greetingSticker = chatState.greetingSticker,
            onTapGreetingSticker = onSendGreetingSticker,
            modifier = Modifier.align(Alignment.Center),
          )
        } else {
          LazyColumn(
            state = listState,
            modifier = Modifier
              .fillMaxSize()
              .testTag("chat_messages_list"),
            contentPadding = PaddingValues(vertical = 10.dp),
          ) {
            items(messages, key = { it.clientOpId.ifBlank { it.messageId } }) { msg ->
              val isOwn = msg.senderUserId == currentUserId
              val isUnsending = chatState.unsendingMessageIds.contains(msg.messageId)
              ChatMessageRowItem(
                msg = msg,
                isOwn = isOwn,
                isUnsending = isUnsending,
                peerName = conversation.peerFullName.ifBlank { conversation.peerUsername },
                peerAvatarUrl = conversation.peerAvatarUrl,
                outgoingStart = outStart,
                outgoingEnd = outEnd,
                isDark = isDark,
                canCopy = canCopy,
                canForward = canForward,
                playback = chatState.voicePlayback,
                onCopy = {
                  val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  cb.setPrimaryClip(ClipData.newPlainText("Telefam message", msg.plainText))
                  Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                },
                onForward = { onStartForwardMessage(msg) },
                onEdit = { onStartEditMessage(msg) },
                onUnsend = { onUnsendMessage(msg) },
                onDeleteForSelf = { onDeleteMessageForSelf(msg) },
                onTogglePlayVoice = {
                  onToggleVoicePlayback(msg.messageId, msg.mediaLocalPath.ifBlank { msg.mediaUrl })
                },
                onOpenViewOnce = { onOpenViewOnceVoice(msg) },
                onCallBack = onCallBackFromLog,
              )
            }
          }
        }
      }

      // Edit Banner
      if (!chatState.editingMessageId.isNullOrBlank()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(pillSurface)
            .padding(horizontal = 14.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(Icons.Filled.Edit, contentDescription = null, tint = outEnd, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Editing message",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = pillContent,
            modifier = Modifier.weight(1f),
          )
          IconButton(onClick = onCancelEditMessage, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Close, contentDescription = null, tint = pillSecondary, modifier = Modifier.size(16.dp))
          }
        }
      }

      // Voice Recording / Preview Bar OR Standard Bottom Composer Bar
      val rec = chatState.voiceRecording
      val isBlockedChat = conversation.isBlocked
      if (!isBlockedChat && (rec.isRecording || rec.recordedFilePath.isNotBlank())) {
        VoiceRecorderAndPreviewBar(
          recording = rec,
          playback = chatState.voicePlayback,
          viewOnce = chatState.voicePreviewViewOnce,
          ttlSec = chatState.voicePreviewTtlSec,
          accentColor = outEnd,
          isDark = isDark,
          onStopRecording = onStopVoiceRecording,
          onDiscard = onDiscardVoiceRecording,
          onTogglePreviewPlay = { onToggleVoicePlayback("preview_voice", rec.recordedFilePath) },
          onToggleViewOnce = onToggleVoicePreviewViewOnce,
          onSelectTtlSec = onSelectVoicePreviewTtl,
          onSendVoiceNote = onSendVoiceNote,
        )
      } else {
        // Bottom Composer Pill matching Screenshot_20261117_112133_Telegram
        val hasText = chatState.composerText.isNotBlank() && !isBlockedChat
        val actionAccent = if (isBlockedChat) pillSecondary.copy(alpha = 0.45f) else if (activeTheme.themeId == "none") Color(0xFF7BC93F) else outEnd
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(pillSurface)
                .pointerInput(isBlockedChat) {
                  if (!isBlockedChat) {
                    detectTapGestures {
                      runCatching { composerFocusRequester.requestFocus() }
                    }
                  }
                }
                .padding(horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              IconButton(
                onClick = {
                  if (!isBlockedChat) {
                    if (chatState.giflyPickerOpen) onCloseGiflyPicker() else onOpenGiflyPicker()
                  }
                },
                enabled = !isBlockedChat,
                modifier = Modifier
                  .size(36.dp)
                  .testTag("chat_sticker_button"),
              ) {
                Icon(
                  imageVector = Icons.Outlined.SentimentSatisfied,
                  contentDescription = stringResource(R.string.cd_stickers),
                  tint = if (chatState.giflyPickerOpen && !isBlockedChat) actionAccent else pillSecondary,
                  modifier = Modifier.size(22.dp),
                )
              }

              Box(
                modifier = Modifier
                  .weight(1f)
                  .padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart,
              ) {
                if (chatState.composerText.isEmpty() || isBlockedChat) {
                  Text(
                    text = stringResource(R.string.chat_composer_placeholder),
                    fontSize = 15.sp,
                    color = pillSecondary.copy(alpha = if (isBlockedChat) 0.55f else 1f),
                  )
                }
                if (!isBlockedChat) {
                  BasicTextField(
                    value = chatState.composerText,
                    onValueChange = onComposerTextChanged,
                    maxLines = 4,
                    textStyle = TextStyle(fontSize = 15.sp, color = pillContent),
                    cursorBrush = SolidColor(actionAccent),
                    modifier = Modifier
                      .fillMaxWidth()
                      .focusRequester(composerFocusRequester)
                      .testTag("chat_message_input"),
                  )
                }
              }

              // Hide Gift and Paperclip smoothly when user starts typing to free input space!
              if (!hasText) {
                IconButton(
                  onClick = { if (!isBlockedChat) onTriggerComingSoon("Gift", "chat_gift_tapped") },
                  enabled = !isBlockedChat,
                  modifier = Modifier
                    .size(36.dp)
                    .testTag("chat_gift_button"),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.CardGiftcard,
                    contentDescription = stringResource(R.string.cd_gift),
                    tint = pillSecondary.copy(alpha = if (isBlockedChat) 0.45f else 1f),
                    modifier = Modifier.size(20.dp),
                  )
                }
                IconButton(
                  onClick = { if (!isBlockedChat) onTriggerComingSoon("File sending", "chat_file_tapped") },
                  enabled = !isBlockedChat,
                  modifier = Modifier
                    .size(36.dp)
                    .testTag("chat_attach_button"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.AttachFile,
                    contentDescription = stringResource(R.string.cd_attach_file),
                    tint = pillSecondary.copy(alpha = if (isBlockedChat) 0.45f else 1f),
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
            }

            // Circular Mic (empty text) -> Send Button (while typing)
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(actionAccent)
                .clickable(enabled = !isBlockedChat) {
                  if (hasText) {
                    onSendTextOrEdit()
                  } else {
                    val granted = ContextCompat.checkSelfPermission(
                      context,
                      Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                      onStartVoiceRecording()
                    } else {
                      micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                  }
                }
                .testTag(if (hasText) "chat_send_button" else "chat_mic_button"),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = if (hasText) Icons.AutoMirrored.Filled.Send else Icons.Filled.Mic,
                contentDescription = if (hasText) stringResource(R.string.cd_send_message) else stringResource(R.string.cd_voice_mic),
                tint = TelefamPureWhite,
                modifier = Modifier.size(22.dp),
              )
            }
          }

          if (isBlockedChat) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
              color = pillSurface,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("chat_blocked_notice"),
            ) {
              Text(
                text = stringResource(R.string.chat_blocked_composer_notice),
                color = TelefamCrimson,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 7.dp),
              )
            }
          }
        }
      }

      AnimatedVisibility(
        visible = chatState.giflyPickerOpen,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
      ) {
        GiflyStickerPickerPanel(
          activeTab = chatState.giflyActiveTab,
          query = chatState.giflyQuery,
          items = chatState.giflyItems,
          hasNext = chatState.giflyHasNext,
          isLoading = chatState.giflyIsLoading,
          isLoadingMore = chatState.giflyIsLoadingMore,
          previewItem = chatState.giflyPreviewItem,
          accentColor = outEnd,
          isDark = isDark,
          onSelectTab = onSelectGiflyTab,
          onQueryChanged = onGiflyQueryChanged,
          onLoadNextPage = onLoadMoreGifly,
          onPreviewItem = onPreviewGiflyItem,
          onSendPreviewItem = onSendPreviewGifly,
          onClose = onCloseGiflyPicker,
        )
      }
    }

    // Active View-Once Auto-Burning Overlay
    val activeVo = chatState.activeViewOnceOverlay
    if (activeVo != null) {
      ViewOnceVoicePlayerOverlay(
        overlay = activeVo,
        onBurnAndClose = onBurnViewOnceVoice,
      )
    }

    // Shared Media / Links / Files Sheet
    val mediaTab = chatState.sharedMediaTab
    if (mediaTab != null) {
      ChatSharedMediaBottomSheet(
        tab = mediaTab,
        messages = messages,
        onSelectTab = { onOpenSharedMediaTab(it) },
        onDismiss = { onOpenSharedMediaTab(null) },
      )
    }

    // Forward Message Picker Sheet
    if (chatState.forwardingMessage != null) {
      ForwardConversationPickerSheet(
        conversations = chatState.conversations,
        onSelectTarget = onConfirmForwardMessage,
        onDismiss = onDismissForwardMessage,
      )
    }

    if (showReportDialog) {
      AlertDialog(
        onDismissRequest = { showReportDialog = false },
        title = { Text(stringResource(R.string.profile_report_title)) },
        text = {
          OutlinedTextField(
            value = reportDetails,
            onValueChange = { reportDetails = it },
            placeholder = { Text(stringResource(R.string.profile_report_details_hint)) },
            modifier = Modifier.fillMaxWidth(),
          )
        },
        confirmButton = {
          Button(
            onClick = {
              onReportPeer("chat_abuse", reportDetails.trim())
              reportDetails = ""
              showReportDialog = false
            },
            colors = ButtonDefaults.buttonColors(containerColor = TelefamCrimson, contentColor = TelefamPureWhite),
          ) {
            Text(stringResource(R.string.profile_report_submit))
          }
        },
        dismissButton = {
          TextButton(onClick = { showReportDialog = false }) {
            Text(stringResource(R.string.settings_cancel))
          }
        },
      )
    }
  }
}

@Composable
private fun EmptyChatGreetingCard(
  greetingSticker: GiflyMediaItemDto?,
  onTapGreetingSticker: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val stickerUrl = greetingSticker?.url?.ifBlank { greetingSticker.previewWebp }?.ifBlank { greetingSticker.preview }.orEmpty()
  Surface(
    modifier = modifier
      .padding(horizontal = 32.dp)
      .widthIn(max = 270.dp)
      .testTag("chat_empty_greeting_card"),
    shape = RoundedCornerShape(22.dp),
    color = Color(0x9E191C22),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = stringResource(R.string.chat_empty_card_title),
        fontSize = 15.5.sp,
        fontWeight = FontWeight.Bold,
        color = TelefamPureWhite,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = stringResource(R.string.chat_empty_card_subtitle),
        fontSize = 14.sp,
        color = TelefamPureWhite.copy(alpha = 0.92f),
        textAlign = TextAlign.Center,
        lineHeight = 19.sp,
      )
      Spacer(modifier = Modifier.height(14.dp))
      Box(
        modifier = Modifier
          .size(134.dp)
          .background(Color.Transparent)
          .clickable(enabled = stickerUrl.isNotBlank(), onClick = onTapGreetingSticker)
          .testTag("chat_greeting_sticker"),
        contentAlignment = Alignment.Center,
      ) {
        if (stickerUrl.isNotBlank()) {
          AsyncImage(
            model = stickerUrl,
            contentDescription = greetingSticker?.title ?: "Greeting sticker",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp, color = TelefamPureWhite)
        }
      }
    }
  }
}
