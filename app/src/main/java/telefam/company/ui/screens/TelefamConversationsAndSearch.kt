package telefam.company.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Markunread
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.telefam.company.app.R
import telefam.company.chat.TelefamChatUiState
import telefam.company.core.ChatPrivacySettingsState
import telefam.company.core.TelefamChatPrivacyCatalog
import telefam.company.core.TelefamContactsRules
import telefam.company.data.local.ChatConversationEntity
import telefam.company.data.network.ChatSearchPersonDto
import telefam.company.ui.theme.LocalTelefamDarkTheme
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamPureWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TelefamConversationsListPane(
  chatState: TelefamChatUiState,
  chatPrivacy: ChatPrivacySettingsState,
  currentUserId: String,
  blushBg: Color,
  bottomPadding: androidx.compose.ui.unit.Dp,
  onStartChatClick: () -> Unit,
  onOpenConversation: (ChatConversationEntity) -> Unit,
  onEnterMultiSelect: (String) -> Unit,
  onToggleSelection: (String) -> Unit,
  onSelectAll: () -> Unit,
  onExitMultiSelect: () -> Unit,
  onBatchAction: (String) -> Unit,
  onUndoBatch: () -> Unit,
  onDismissUndo: () -> Unit,
) {
  val isDark = LocalTelefamDarkTheme.current
  val activeBubble = TelefamChatPrivacyCatalog.bubbleColorById(chatPrivacy.bubbleColorId)
  val accentColor = Color(if (isDark) activeBubble.darkEndHex else activeBubble.lightEndHex)
  val conversations = chatState.conversations

  Box(modifier = Modifier.fillMaxSize()) {
    if (conversations.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(bottom = bottomPadding),
        contentAlignment = Alignment.Center,
      ) {
        Surface(
          modifier = Modifier
            .padding(horizontal = 24.dp)
            .widthIn(max = 380.dp),
          shape = RoundedCornerShape(24.dp),
          color = Color.Transparent,
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Box(
              modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(blushBg),
              contentAlignment = Alignment.Center,
            ) {
              Icon(Icons.Outlined.Forum, contentDescription = null, tint = TelefamCrimson, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
              text = stringResource(R.string.home_start_conversation),
              style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onBackground,
              textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = stringResource(R.string.home_start_conversation_desc),
              style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(22.dp))
            Button(
              onClick = onStartChatClick,
              shape = RoundedCornerShape(24.dp),
              colors = ButtonDefaults.buttonColors(containerColor = TelefamCrimson, contentColor = TelefamPureWhite),
              contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
              elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
              modifier = Modifier
                .height(44.dp)
                .testTag("start_chat_button"),
            ) {
              Icon(Icons.Outlined.AddComment, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(stringResource(R.string.home_start_chat), style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp))
            }
          }
        }
      }
    } else {
      Column(modifier = Modifier.fillMaxSize()) {
        if (chatState.isMultiSelectMode) {
          BackHandler(enabled = true) { onExitMultiSelect() }
          Surface(
            color = TelefamCrimson,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("chat_multiselect_bar"),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              IconButton(onClick = onExitMultiSelect) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = TelefamPureWhite)
              }
              Text(
                text = "${chatState.selectedConversationIds.size}",
                color = TelefamPureWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f),
              )
              IconButton(onClick = onSelectAll, modifier = Modifier.testTag("batch_select_all")) {
                Icon(Icons.Filled.SelectAll, contentDescription = stringResource(R.string.chat_action_select_all), tint = TelefamPureWhite)
              }
              IconButton(onClick = { onBatchAction("archive") }, modifier = Modifier.testTag("batch_archive")) {
                Icon(Icons.Filled.Archive, contentDescription = stringResource(R.string.chat_action_archive), tint = TelefamPureWhite)
              }
              IconButton(onClick = { onBatchAction("mute") }, modifier = Modifier.testTag("batch_mute")) {
                Icon(Icons.Filled.NotificationsOff, contentDescription = stringResource(R.string.chat_action_mute), tint = TelefamPureWhite)
              }
              IconButton(onClick = { onBatchAction("mark_read") }, modifier = Modifier.testTag("batch_mark_read")) {
                Icon(Icons.Filled.DoneAll, contentDescription = stringResource(R.string.chat_action_mark_read), tint = TelefamPureWhite)
              }
              IconButton(onClick = { onBatchAction("mark_unread") }, modifier = Modifier.testTag("batch_mark_unread")) {
                Icon(Icons.Filled.Markunread, contentDescription = stringResource(R.string.chat_action_mark_unread), tint = TelefamPureWhite)
              }
              IconButton(onClick = { onBatchAction("block") }, modifier = Modifier.testTag("batch_block")) {
                Icon(Icons.Filled.Block, contentDescription = stringResource(R.string.chat_action_block), tint = TelefamPureWhite)
              }
              IconButton(onClick = { onBatchAction("delete") }, modifier = Modifier.testTag("batch_delete")) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.chat_action_delete), tint = TelefamPureWhite)
              }
            }
          }
        }

        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .testTag("chats_conversation_list"),
          contentPadding = PaddingValues(top = 6.dp, bottom = bottomPadding + 14.dp),
        ) {
          items(conversations, key = { it.conversationId }) { conv ->
            val selected = chatState.selectedConversationIds.contains(conv.conversationId)
            val isActiveSplit = chatState.activeConversationId == conv.conversationId
            val typingMode = chatState.typingModesByConversation[conv.conversationId].orEmpty()
            val timeStr = remember(conv.lastMessageAtMs) {
              if (conv.lastMessageAtMs <= 0L) "" else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(conv.lastMessageAtMs))
            }

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(
                  when {
                    selected -> TelefamCrimson.copy(alpha = 0.14f)
                    isActiveSplit -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    else -> Color.Transparent
                  },
                )
                .combinedClickable(
                  onClick = {
                    if (chatState.isMultiSelectMode) onToggleSelection(conv.conversationId) else onOpenConversation(conv)
                  },
                  onLongClick = {
                    if (!chatState.isMultiSelectMode) onEnterMultiSelect(conv.conversationId) else onToggleSelection(conv.conversationId)
                  },
                )
                .padding(horizontal = 16.dp, vertical = 11.dp)
                .testTag("conv_row_${conv.conversationId}"),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(modifier = Modifier.size(52.dp)) {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(accentColor),
                  contentAlignment = Alignment.Center,
                ) {
                  if (conv.peerAvatarUrl.isNotBlank()) {
                    AsyncImage(
                      model = conv.peerAvatarUrl,
                      contentDescription = conv.peerFullName,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier.fillMaxSize(),
                    )
                  } else {
                    Text(
                      text = TelefamContactsRules.initialsFor(conv.peerFullName, conv.peerUsername),
                      color = TelefamPureWhite,
                      fontWeight = FontWeight.Bold,
                      fontSize = 16.sp,
                    )
                  }
                }
                if (selected) {
                  Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = TelefamCrimson,
                    modifier = Modifier
                      .size(20.dp)
                      .align(Alignment.BottomEnd)
                      .clip(CircleShape)
                      .background(TelefamPureWhite),
                  )
                } else if (conv.showOnlineIndicator && conv.isOnline) {
                  Box(
                    modifier = Modifier
                      .size(13.dp)
                      .align(Alignment.BottomEnd)
                      .clip(CircleShape)
                      .background(Color(0xFF22C55E))
                      .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                  )
                }
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = conv.peerFullName.ifBlank { conv.peerUsername.ifBlank { "Telefam" } },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.5.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                  )
                  if (conv.isMuted) {
                    Icon(
                      imageVector = Icons.Filled.NotificationsOff,
                      contentDescription = "Muted",
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier
                        .padding(end = 4.dp)
                        .size(14.dp),
                    )
                  }
                  Text(
                    text = timeStr,
                    fontSize = 12.sp,
                    color = if (conv.unreadCount > 0) TelefamCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  val preview = when {
                    conv.showTypingIndicator && typingMode == "recording_voice" -> stringResource(R.string.chat_status_recording)
                    conv.showTypingIndicator && typingMode == "typing" -> stringResource(R.string.chat_status_typing)
                    else -> conv.lastMessagePreviewPlain.ifBlank { stringResource(R.string.chat_e2ee_badge) }
                  }
                  if (conv.lastSenderId == currentUserId && conv.lastMessageId.isNotBlank() && typingMode.isBlank()) {
                    OutgoingStatusIndicator(
                      status = conv.lastDeliveryStatus,
                      peerName = conv.peerFullName,
                      peerAvatarUrl = conv.peerAvatarUrl,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                  }
                  Text(
                    text = preview,
                    fontSize = 13.5.sp,
                    color = if (typingMode.isNotBlank() && conv.showTypingIndicator) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                  )
                  if (conv.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                      modifier = Modifier
                        .clip(CircleShape)
                        .background(TelefamCrimson)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                        .testTag("conv_unread_${conv.conversationId}"),
                      contentAlignment = Alignment.Center,
                    ) {
                      Text(
                        text = conv.unreadCount.toString(),
                        color = TelefamPureWhite,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                      )
                    }
                  }
                }
              }
            }
            HorizontalDivider(
              modifier = Modifier.padding(start = 80.dp),
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            )
          }
        }
      }
    }

    // Undo Snackbar after batch action
    val undo = chatState.undoBatchState
    if (undo != null) {
      Surface(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(horizontal = 16.dp, vertical = bottomPadding + 12.dp)
          .fillMaxWidth()
          .testTag("chat_undo_snackbar"),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1F242D),
        shadowElevation = 8.dp,
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = undo.label,
            color = TelefamPureWhite,
            fontSize = 13.5.sp,
            modifier = Modifier.weight(1f),
          )
          TextButton(onClick = onUndoBatch, modifier = Modifier.testTag("chat_undo_button")) {
            Text(stringResource(R.string.chat_action_undo), color = TelefamCrimson, fontWeight = FontWeight.Bold)
          }
          IconButton(onClick = onDismissUndo, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Filled.Close, contentDescription = null, tint = Color(0xFF9EA5B2), modifier = Modifier.size(16.dp))
          }
        }
      }
    }
  }
}

@Composable
fun HomeGlobalSearchOverlay(
  chatState: TelefamChatUiState,
  onQueryChanged: (String) -> Unit,
  onOpenConversation: (ChatConversationEntity) -> Unit,
  onOpenPersonChat: (ChatSearchPersonDto) -> Unit,
  onClose: () -> Unit,
) {
  BackHandler(enabled = true) { onClose() }
  val isDark = LocalTelefamDarkTheme.current
  val bg = if (isDark) Color(0xFF0E1015) else Color(0xFFF6F7FA)
  val cardBg = if (isDark) Color(0xFF171A21) else TelefamPureWhite
  val textPrimary = MaterialTheme.colorScheme.onBackground
  val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(bg)
      .statusBarsPadding()
      .navigationBarsPadding()
      .testTag("home_global_search_overlay"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(TelefamCrimson)
        .padding(horizontal = 10.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onClose, modifier = Modifier.testTag("home_search_close")) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = TelefamPureWhite)
      }
      Row(
        modifier = Modifier
          .weight(1f)
          .height(42.dp)
          .clip(RoundedCornerShape(21.dp))
          .background(cardBg)
          .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = textSecondary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
          if (chatState.homeSearchQuery.isEmpty()) {
            Text(stringResource(R.string.chat_search_hint), fontSize = 14.sp, color = textSecondary, maxLines = 1)
          }
          BasicTextField(
            value = chatState.homeSearchQuery,
            onValueChange = onQueryChanged,
            singleLine = true,
            textStyle = TextStyle(fontSize = 14.5.sp, color = textPrimary),
            cursorBrush = SolidColor(TelefamCrimson),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("home_search_input"),
          )
        }
        if (chatState.homeSearchQuery.isNotEmpty()) {
          IconButton(onClick = { onQueryChanged("") }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_clear_search), tint = textSecondary, modifier = Modifier.size(16.dp))
          }
        }
      }
    }

    if (chatState.isSearchingHomeChats) {
      Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = TelefamCrimson, modifier = Modifier.size(26.dp))
      }
    }

    val results = chatState.homeSearchResults
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (results.conversations.isNotEmpty()) {
        item {
          Text("Conversations", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textSecondary)
        }
        items(results.conversations, key = { "sc_${it.conversationId}" }) { conv ->
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = cardBg,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onOpenConversation(conv) },
          ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(TelefamCrimson),
                contentAlignment = Alignment.Center,
              ) {
                if (conv.peerAvatarUrl.isNotBlank()) {
                  AsyncImage(model = conv.peerAvatarUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                  Text(TelefamContactsRules.initialsFor(conv.peerFullName, conv.peerUsername), color = TelefamPureWhite, fontWeight = FontWeight.Bold)
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(conv.peerFullName.ifBlank { conv.peerUsername }, fontWeight = FontWeight.SemiBold, color = textPrimary)
                Text(conv.lastMessagePreviewPlain, fontSize = 12.5.sp, color = textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
              }
            }
          }
        }
      }

      if (results.matchingMessages.isNotEmpty()) {
        item {
          Spacer(modifier = Modifier.height(6.dp))
          Text("Messages", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textSecondary)
        }
        items(results.matchingMessages, key = { "sm_${it.messageId}" }) { msg ->
          val conv = chatState.conversations.firstOrNull { it.conversationId == msg.conversationId }
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = cardBg,
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                if (conv != null) onOpenConversation(conv)
              },
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(conv?.peerFullName ?: "Conversation", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TelefamCrimson)
              Text(msg.plainText, fontSize = 13.5.sp, color = textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
          }
        }
      }

      if (results.people.isNotEmpty()) {
        item {
          Spacer(modifier = Modifier.height(6.dp))
          Text("People", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textSecondary)
        }
        items(results.people, key = { "sp_${it.userId}" }) { person ->
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = cardBg,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onOpenPersonChat(person) }
              .testTag("search_person_${person.userId}"),
          ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF0EA5E9)),
                contentAlignment = Alignment.Center,
              ) {
                if (person.avatarUrl.isNotBlank()) {
                  AsyncImage(model = person.avatarUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                  Text(TelefamContactsRules.initialsFor(person.fullName, person.username), color = TelefamPureWhite, fontWeight = FontWeight.Bold)
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(person.fullName.ifBlank { person.username }, fontWeight = FontWeight.SemiBold, color = textPrimary)
                Text("@${person.username}", fontSize = 12.5.sp, color = textSecondary)
              }
              Text("Chat", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = TelefamCrimson)
            }
          }
        }
      }
    }
  }
}
