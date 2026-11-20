package telefam.company.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telefam.company.app.R
import telefam.company.core.ChatAudienceOption
import telefam.company.core.ChatBubbleColorOption
import telefam.company.core.ChatPatternKind
import telefam.company.core.ChatPrivacyRoute
import telefam.company.core.ChatPrivacySettingsState
import telefam.company.core.ChatThemeSpec
import telefam.company.core.TelefamChatPrivacyCatalog
import telefam.company.ui.theme.LocalTelefamDarkTheme
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamPureWhite
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class ChatAudienceField(
  val testTag: String,
  val allowContacts: Boolean,
) {
  MESSAGE_REQUESTS("row_message_requests", true),
  WHO_CAN_CALL("row_who_can_call", true),
  WHO_CAN_SCREENSHOT("row_who_can_screenshot", false),
  WHO_CAN_SHARE_CHATS("row_who_can_share_chats", false),
  WHO_CAN_COPY_MESSAGES("row_who_can_copy_messages", false),
  WHO_CAN_DOWNLOAD_MEDIA("row_who_can_download_media", false),
  READ_RECEIPTS("row_read_receipts", false),
  TYPING_INDICATORS("row_typing_indicators", false),
  ONLINE_INDICATORS("row_online_indicators", false),
}

@Composable
fun TelefamChatPrivacyHostScreen(
  route: ChatPrivacyRoute,
  state: ChatPrivacySettingsState,
  previewSenderName: String = "Telefam",
  onNavigateBack: () -> Unit,
  onUpdatePrivacy: (ChatPrivacySettingsState) -> Unit,
  onOpenChatsTheme: () -> Unit,
  onOpenBubbleColors: () -> Unit,
  onSelectTheme: (String) -> Unit,
  onSelectBubbleColor: (String) -> Unit,
) {
  BackHandler(enabled = true) {
    onNavigateBack()
  }

  when (route) {
    ChatPrivacyRoute.PRIVACY -> TelefamChatPrivacyScreen(
      state = state,
      onBack = onNavigateBack,
      onUpdatePrivacy = onUpdatePrivacy,
      onOpenChatsTheme = onOpenChatsTheme,
      onOpenBubbleColors = onOpenBubbleColors,
    )

    ChatPrivacyRoute.CHATS_THEME -> TelefamChatsThemeScreen(
      selectedThemeId = state.chatThemeId,
      selectedBubbleColorId = state.bubbleColorId,
      previewSenderName = previewSenderName,
      onBack = onNavigateBack,
      onSelectTheme = onSelectTheme,
    )

    ChatPrivacyRoute.BUBBLE_COLORS -> TelefamBubbleColorsScreen(
      selectedThemeId = state.chatThemeId,
      selectedBubbleColorId = state.bubbleColorId,
      previewSenderName = previewSenderName,
      onBack = onNavigateBack,
      onSelectBubbleColor = onSelectBubbleColor,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelefamChatPrivacyScreen(
  state: ChatPrivacySettingsState,
  onBack: () -> Unit,
  onUpdatePrivacy: (ChatPrivacySettingsState) -> Unit,
  onOpenChatsTheme: () -> Unit,
  onOpenBubbleColors: () -> Unit,
) {
  val isDark = LocalTelefamDarkTheme.current
  val sheetBg = if (isDark) Color(0xFF0E0F13) else Color(0xFFF6F7FA)
  val cardBg = if (isDark) Color(0xFF17181E) else TelefamPureWhite
  val cardBorder = if (isDark) Color(0xFF262831) else Color(0xFFE5E7EB)
  val titleColor = if (isDark) Color(0xFFF4F5F8) else Color(0xFF14151A)
  val subtitleColor = if (isDark) Color(0xFF9EA4B0) else Color(0xFF5D6472)
  val badgeBg = if (isDark) Color(0xFF331116) else Color(0xFFFDECEC)
  val badgeTint = if (isDark) Color(0xFFEA2E37) else TelefamCrimson

  var pickingField by remember { mutableStateOf<ChatAudienceField?>(null) }

  val activeTheme = TelefamChatPrivacyCatalog.themeById(state.chatThemeId)
  val activeBubble = TelefamChatPrivacyCatalog.bubbleColorById(state.bubbleColorId)
  val bubbleSwatchColor = Color(if (isDark) activeBubble.darkStartHex else activeBubble.lightStartHex)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(TelefamCrimson),
  ) {
    // Crimson header matching Image 1
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 18.dp),
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier
          .size(48.dp)
          .testTag("chat_privacy_back"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = stringResource(R.string.cd_back),
          tint = TelefamPureWhite,
        )
      }
      Text(
        text = stringResource(R.string.chat_privacy_title),
        style = MaterialTheme.typography.headlineSmall.copy(
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold,
        ),
        color = TelefamPureWhite,
        modifier = Modifier.padding(start = 12.dp, top = 2.dp),
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = stringResource(R.string.chat_privacy_subtitle),
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
        color = TelefamPureWhite.copy(alpha = 0.92f),
        modifier = Modifier.padding(start = 12.dp),
      )
    }

    Surface(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
      color = sheetBg,
    ) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
      ) {
        LazyColumn(
          modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxSize()
            .navigationBarsPadding()
            .testTag("chat_privacy_list"),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          item {
            ChatPrivacyActionRow(
              icon = Icons.Outlined.Sms,
              title = stringResource(R.string.chat_privacy_message_requests),
              subtitle = stringResource(R.string.chat_privacy_message_requests_sub),
              valueLabel = state.messageRequests.label,
              cardBg = cardBg,
              cardBorder = cardBorder,
              titleColor = titleColor,
              subtitleColor = subtitleColor,
              badgeBg = badgeBg,
              badgeTint = badgeTint,
              testTag = "chat_privacy_message_requests",
              onClick = { pickingField = ChatAudienceField.MESSAGE_REQUESTS },
            )
          }
          item {
            ChatPrivacyActionRow(
              icon = Icons.Outlined.Call,
              title = stringResource(R.string.chat_privacy_who_can_call),
              subtitle = stringResource(R.string.chat_privacy_who_can_call_sub),
              valueLabel = state.whoCanCall.label,
              cardBg = cardBg,
              cardBorder = cardBorder,
              titleColor = titleColor,
              subtitleColor = subtitleColor,
              badgeBg = badgeBg,
              badgeTint = badgeTint,
              testTag = "chat_privacy_who_can_call",
              onClick = { pickingField = ChatAudienceField.WHO_CAN_CALL },
            )
          }
          item {
            ChatPrivacyActionRow(
              icon = Icons.Outlined.Smartphone,
              title = stringResource(R.string.chat_privacy_screenshot),
              subtitle = stringResource(R.string.chat_privacy_screenshot_sub),
              valueLabel = state.whoCanScreenshot.label,
              cardBg = cardBg,
              cardBorder = cardBorder,
              titleColor = titleColor,
              subtitleColor = subtitleColor,
              badgeBg = badgeBg,
              badgeTint = badgeTint,
              testTag = "chat_privacy_screenshot",
              onClick = { pickingField = ChatAudienceField.WHO_CAN_SCREENSHOT },
            )
          }
          item {
            ChatPrivacyActionRow(
              icon = Icons.Outlined.Share,
              title = stringResource(R.string.chat_privacy_share_chats),
              subtitle = stringResource(R.string.chat_privacy_share_chats_sub),
              valueLabel = state.whoCanShareChats.label,
              cardBg = cardBg,
              cardBorder = cardBorder,
              titleColor = titleColor,
              subtitleColor = subtitleColor,
              badgeBg = badgeBg,
              badgeTint = badgeTint,
              testTag = "chat_privacy_share_chats",
              onClick = { pickingField = ChatAudienceField.WHO_CAN_SHARE_CHATS },
            )
          }
          item {
            ChatPrivacyActionRow(
              icon = Icons.Outlined.ContentCopy,
              title = stringResource(R.string.chat_privacy_copy_messages),
              subtitle = stringResource(R.string.chat_privacy_copy_messages_sub),
              valueLabel = state.whoCanCopyMessages.label,
              cardBg = cardBg,
              cardBorder = cardBorder,
              titleColor = titleColor,
              subtitleColor = subtitleColor,
              badgeBg = badgeBg,
              badgeTint = badgeTint,
              testTag = "chat_privacy_copy_messages",
              onClick = { pickingField = ChatAudienceField.WHO_CAN_COPY_MESSAGES },
            )
          }
          item {
            ChatPrivacyActionRow(
              icon = Icons.Outlined.Download,
              title = stringResource(R.string.chat_privacy_download_media),
              subtitle = stringResource(R.string.chat_privacy_download_media_sub),
              valueLabel = state.whoCanDownloadMedia.label,
              cardBg = cardBg,
              cardBorder = cardBorder,
              titleColor = titleColor,
              subtitleColor = subtitleColor,
              badgeBg = badgeBg,
              badgeTint = badgeTint,
              testTag = "chat_privacy_download_media",
              onClick = { pickingField = ChatAudienceField.WHO_CAN_DOWNLOAD_MEDIA },
            )
          }
          item {
            ChatPrivacyCustomRow(
              icon = Icons.Outlined.Palette,
              title = stringResource(R.string.chat_privacy_theme),
              subtitle = stringResource(R.string.chat_privacy_theme_sub),
              cardBg = cardBg,
              cardBorder = cardBorder,
              titleColor = titleColor,
              subtitleColor = subtitleColor,
              badgeBg = badgeBg,
              badgeTint = badgeTint,
              testTag = "chat_privacy_theme",
              onClick = onOpenChatsTheme,
              trailing = {
                if (activeTheme.themeId == "none") {
                  NoThemeBadge(accent = badgeTint)
                } else {
                  Box(
                    modifier = Modifier
                      .size(28.dp)
                      .clip(CircleShape)
                      .border(1.5.dp, Color(activeTheme.selectionHex), CircleShape),
                  ) {
                    ChatWallpaperCanvas(
                      theme = activeTheme,
                      isDark = isDark,
                      modifier = Modifier.fillMaxSize(),
                    )
                  }
                }
              },
            )
          }
          item {
            ChatPrivacyCustomRow(
              icon = Icons.Filled.Title,
              title = stringResource(R.string.chat_privacy_bubble_colour),
              subtitle = stringResource(R.string.chat_privacy_bubble_colour_sub),
              cardBg = cardBg,
              cardBorder = cardBorder,
              titleColor = titleColor,
              subtitleColor = subtitleColor,
              badgeBg = badgeBg,
              badgeTint = badgeTint,
              testTag = "chat_privacy_bubble_colour",
              onClick = onOpenBubbleColors,
              trailing = {
                Box(
                  modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(bubbleSwatchColor)
                    .border(1.dp, TelefamPureWhite.copy(alpha = 0.28f), CircleShape),
                )
              },
            )
          }
          item {
            ChatPrivacyActionRow(
              icon = Icons.Outlined.DoneAll,
              title = stringResource(R.string.chat_privacy_read_receipts),
              subtitle = stringResource(R.string.chat_privacy_read_receipts_sub),
              valueLabel = state.readReceipts.label,
              cardBg = cardBg,
              cardBorder = cardBorder,
              titleColor = titleColor,
              subtitleColor = subtitleColor,
              badgeBg = badgeBg,
              badgeTint = badgeTint,
              testTag = "chat_privacy_read_receipts",
              onClick = { pickingField = ChatAudienceField.READ_RECEIPTS },
            )
          }
          item {
            ChatPrivacyActionRow(
              icon = Icons.Outlined.MoreHoriz,
              title = stringResource(R.string.chat_privacy_typing_indicators),
              subtitle = stringResource(R.string.chat_privacy_typing_indicators_sub),
              valueLabel = state.typingIndicators.label,
              cardBg = cardBg,
              cardBorder = cardBorder,
              titleColor = titleColor,
              subtitleColor = subtitleColor,
              badgeBg = badgeBg,
              badgeTint = badgeTint,
              testTag = "chat_privacy_typing_indicators",
              onClick = { pickingField = ChatAudienceField.TYPING_INDICATORS },
            )
          }
          item {
            ChatPrivacyActionRow(
              icon = Icons.Outlined.Visibility,
              title = stringResource(R.string.chat_privacy_online_indicators),
              subtitle = stringResource(R.string.chat_privacy_online_indicators_sub),
              valueLabel = state.onlineIndicators.label,
              cardBg = cardBg,
              cardBorder = cardBorder,
              titleColor = titleColor,
              subtitleColor = subtitleColor,
              badgeBg = badgeBg,
              badgeTint = badgeTint,
              testTag = "chat_privacy_online_indicators",
              onClick = { pickingField = ChatAudienceField.ONLINE_INDICATORS },
            )
          }
          item { Spacer(modifier = Modifier.height(12.dp)) }
        }
      }
    }
  }

  val activePicker = pickingField
  if (activePicker != null) {
    val (sheetTitle, currentOption) = when (activePicker) {
      ChatAudienceField.MESSAGE_REQUESTS -> stringResource(R.string.chat_privacy_message_requests) to state.messageRequests
      ChatAudienceField.WHO_CAN_CALL -> stringResource(R.string.chat_privacy_who_can_call) to state.whoCanCall
      ChatAudienceField.WHO_CAN_SCREENSHOT -> stringResource(R.string.chat_privacy_screenshot) to state.whoCanScreenshot
      ChatAudienceField.WHO_CAN_SHARE_CHATS -> stringResource(R.string.chat_privacy_share_chats) to state.whoCanShareChats
      ChatAudienceField.WHO_CAN_COPY_MESSAGES -> stringResource(R.string.chat_privacy_copy_messages) to state.whoCanCopyMessages
      ChatAudienceField.WHO_CAN_DOWNLOAD_MEDIA -> stringResource(R.string.chat_privacy_download_media) to state.whoCanDownloadMedia
      ChatAudienceField.READ_RECEIPTS -> stringResource(R.string.chat_privacy_read_receipts) to state.readReceipts
      ChatAudienceField.TYPING_INDICATORS -> stringResource(R.string.chat_privacy_typing_indicators) to state.typingIndicators
      ChatAudienceField.ONLINE_INDICATORS -> stringResource(R.string.chat_privacy_online_indicators) to state.onlineIndicators
    }
    val options = if (activePicker.allowContacts) {
      TelefamChatPrivacyCatalog.audienceOptions4
    } else {
      TelefamChatPrivacyCatalog.audienceOptions3
    }

    ModalBottomSheet(
      onDismissRequest = { pickingField = null },
      containerColor = if (isDark) Color(0xFF17181E) else TelefamPureWhite,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .padding(bottom = 26.dp),
      ) {
        Text(
          text = sheetTitle,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = titleColor,
        )
        Spacer(modifier = Modifier.height(10.dp))
        options.forEach { option ->
          val selected = option == currentOption
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable {
                val updated = when (activePicker) {
                  ChatAudienceField.MESSAGE_REQUESTS -> state.copy(messageRequests = option)
                  ChatAudienceField.WHO_CAN_CALL -> state.copy(whoCanCall = option)
                  ChatAudienceField.WHO_CAN_SCREENSHOT -> state.copy(whoCanScreenshot = option)
                  ChatAudienceField.WHO_CAN_SHARE_CHATS -> state.copy(whoCanShareChats = option)
                  ChatAudienceField.WHO_CAN_COPY_MESSAGES -> state.copy(whoCanCopyMessages = option)
                  ChatAudienceField.WHO_CAN_DOWNLOAD_MEDIA -> state.copy(whoCanDownloadMedia = option)
                  ChatAudienceField.READ_RECEIPTS -> state.copy(readReceipts = option)
                  ChatAudienceField.TYPING_INDICATORS -> state.copy(typingIndicators = option)
                  ChatAudienceField.ONLINE_INDICATORS -> state.copy(onlineIndicators = option)
                }
                onUpdatePrivacy(updated)
                pickingField = null
              }
              .padding(horizontal = 8.dp, vertical = 12.dp)
              .testTag("audience_option_${option.apiValue}"),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(
              selected = selected,
              onClick = null,
              colors = RadioButtonDefaults.colors(selectedColor = TelefamCrimson),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = option.label,
              style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
              ),
              color = titleColor,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ChatPrivacyActionRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  valueLabel: String,
  cardBg: Color,
  cardBorder: Color,
  titleColor: Color,
  subtitleColor: Color,
  badgeBg: Color,
  badgeTint: Color,
  testTag: String,
  onClick: () -> Unit,
) {
  ChatPrivacyCustomRow(
    icon = icon,
    title = title,
    subtitle = subtitle,
    cardBg = cardBg,
    cardBorder = cardBorder,
    titleColor = titleColor,
    subtitleColor = subtitleColor,
    badgeBg = badgeBg,
    badgeTint = badgeTint,
    testTag = testTag,
    onClick = onClick,
    trailing = {
      Text(
        text = valueLabel,
        style = MaterialTheme.typography.bodyMedium.copy(
          fontSize = 13.5.sp,
          fontWeight = FontWeight.Medium,
        ),
        color = badgeTint,
      )
    },
  )
}

@Composable
private fun ChatPrivacyCustomRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  cardBg: Color,
  cardBorder: Color,
  titleColor: Color,
  subtitleColor: Color,
  badgeBg: Color,
  badgeTint: Color,
  testTag: String,
  onClick: () -> Unit,
  trailing: @Composable () -> Unit,
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
      .clickable(onClick = onClick)
      .testTag(testTag),
    shape = RoundedCornerShape(18.dp),
    color = cardBg,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 13.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(badgeBg),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = badgeTint,
          modifier = Modifier.size(20.dp),
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall.copy(
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
          ),
          color = titleColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
          color = subtitleColor,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      trailing()

      Spacer(modifier = Modifier.width(4.dp))

      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = subtitleColor,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

@Composable
private fun NoThemeBadge(accent: Color) {
  Box(
    modifier = Modifier.size(28.dp),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = Icons.Filled.Palette,
      contentDescription = null,
      tint = Color(0xFF9EA4B0),
      modifier = Modifier.size(18.dp),
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
      val stroke = 2.2.dp.toPx()
      val r = size.minDimension / 2f - stroke
      drawCircle(
        color = accent,
        radius = r,
        style = Stroke(width = stroke),
      )
      drawLine(
        color = accent,
        start = Offset(size.width * 0.24f, size.height * 0.24f),
        end = Offset(size.width * 0.76f, size.height * 0.76f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
      )
    }
  }
}

@Composable
fun TelefamChatsThemeScreen(
  selectedThemeId: String,
  selectedBubbleColorId: String,
  previewSenderName: String = "Telefam",
  onBack: () -> Unit,
  onSelectTheme: (String) -> Unit,
) {
  val isDark = LocalTelefamDarkTheme.current
  val pageBg = if (isDark) Color(0xFF0A0B0E) else Color(0xFFF3F5F8)
  val cardBg = if (isDark) Color(0xFF1A1C20) else TelefamPureWhite
  val activeTheme = TelefamChatPrivacyCatalog.themeById(selectedThemeId)
  val activeBubble = TelefamChatPrivacyCatalog.bubbleColorById(selectedBubbleColorId)
  val headerTint = if (activeTheme.themeId == "none") {
    Color(0xFF7ED957)
  } else {
    Color(activeTheme.selectionHex)
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(pageBg)
      .statusBarsPadding()
      .navigationBarsPadding(),
  ) {
    // Minimal header bar with Back + screen title + quick Reset to Default
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier
          .size(48.dp)
          .testTag("chat_theme_back"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = stringResource(R.string.cd_back),
          tint = if (isDark) TelefamPureWhite else Color(0xFF14151A),
        )
      }
      Text(
        text = stringResource(R.string.chat_privacy_theme),
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = if (isDark) TelefamPureWhite else Color(0xFF14151A),
        modifier = Modifier.weight(1f),
      )
      if (activeTheme.themeId != "none") {
        OutlinedButton(
          onClick = { onSelectTheme("none") },
          shape = RoundedCornerShape(20.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.testTag("chat_theme_reset_button"),
        ) {
          Text(
            text = stringResource(R.string.chat_theme_reset),
            fontSize = 12.sp,
          )
        }
      }
    }

    BoxWithConstraints(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 14.dp, vertical = 6.dp),
      contentAlignment = Alignment.TopCenter,
    ) {
      val gridColumns = when {
        maxWidth >= 900.dp -> 5
        maxWidth >= 600.dp -> 4
        else -> 3
      }
      val themes = TelefamChatPrivacyCatalog.themes

      Column(
        modifier = Modifier
          .widthIn(max = 860.dp)
          .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Select theme rounded grid card matching Image 2
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .border(
              1.dp,
              if (isDark) Color(0xFF282B31) else Color(0xFFE2E6ED),
              RoundedCornerShape(22.dp),
            ),
          shape = RoundedCornerShape(22.dp),
          color = cardBg,
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 16.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = stringResource(R.string.chat_theme_select_title),
                style = MaterialTheme.typography.titleMedium.copy(
                  fontSize = 16.sp,
                  fontWeight = FontWeight.SemiBold,
                ),
                color = headerTint,
              )
              Text(
                text = activeTheme.title,
                style = MaterialTheme.typography.labelMedium,
                color = if (isDark) Color(0xFF9EA4B0) else Color(0xFF5D6472),
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val rows = themes.chunked(gridColumns)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              rows.forEach { rowItems ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  rowItems.forEach { spec ->
                    ThemePreviewTile(
                      theme = spec,
                      isDark = isDark,
                      selected = spec.themeId == activeTheme.themeId,
                      onClick = { onSelectTheme(spec.themeId) },
                      modifier = Modifier.weight(1f),
                    )
                  }
                  repeat(gridColumns - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                  }
                }
              }
            }
          }
        }

        // Live Chat Preview using the user's real profile name + localized preview dialog
        LiveChatThemePreviewCard(
          theme = activeTheme,
          bubbleColor = activeBubble,
          useThemeAccentBubble = activeTheme.themeId != "none",
          isDark = isDark,
          previewSenderName = previewSenderName,
        )

        Spacer(modifier = Modifier.height(8.dp))
      }
    }
  }
}

@Composable
private fun ThemePreviewTile(
  theme: ChatThemeSpec,
  isDark: Boolean,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val borderColor = if (selected) {
    Color(theme.selectionHex)
  } else if (isDark) {
    Color(0xFF2B2E36)
  } else {
    Color(0xFFDCE1EA)
  }
  val incomingColor = if (isDark) Color(0xFF2B2F36).copy(alpha = 0.92f) else TelefamPureWhite.copy(alpha = 0.94f)
  val outgoingBrush = Brush.horizontalGradient(
    listOf(Color(theme.accentStartHex), Color(theme.accentEndHex)),
  )

  Box(
    modifier = modifier
      .aspectRatio(0.76f)
      .clip(RoundedCornerShape(16.dp))
      .border(
        width = if (selected) 2.5.dp else 1.dp,
        color = borderColor,
        shape = RoundedCornerShape(16.dp),
      )
      .clickable(onClick = onClick)
      .testTag("theme_tile_${theme.themeId}"),
  ) {
    ChatWallpaperCanvas(
      theme = theme,
      isDark = isDark,
      modifier = Modifier.fillMaxSize(),
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 8.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top mini chat bubbles matching Image 2
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
          modifier = Modifier
            .fillMaxWidth(0.76f)
            .align(Alignment.End)
            .height(20.dp)
            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 10.dp, bottomEnd = 3.dp))
            .background(outgoingBrush),
        )
        Box(
          modifier = Modifier
            .fillMaxWidth(0.72f)
            .align(Alignment.Start)
            .height(18.dp)
            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomEnd = 10.dp, bottomStart = 3.dp))
            .background(incomingColor),
        )
      }

      // Bottom-center crisp SVG vector emblem + title
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
          modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(
              if (isDark) Color(0xFF15181E).copy(alpha = 0.85f) else TelefamPureWhite.copy(alpha = 0.9f),
            )
            .border(1.dp, Color(theme.selectionHex).copy(alpha = 0.55f), CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = themeEmblemVector(theme.patternKind),
            contentDescription = theme.title,
            tint = Color(theme.accentEndHex),
            modifier = Modifier.size(18.dp),
          )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = theme.title,
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
          ),
          color = if (isDark) TelefamPureWhite.copy(alpha = 0.9f) else Color(0xFF1E222B),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
fun LiveChatThemePreviewCard(
  theme: ChatThemeSpec,
  bubbleColor: ChatBubbleColorOption,
  useThemeAccentBubble: Boolean,
  isDark: Boolean,
  modifier: Modifier = Modifier,
  previewSenderName: String = "Telefam",
) {
  val resolvedSenderName = previewSenderName.trim().ifBlank { stringResource(R.string.chat_theme_preview_sender) }
  val incomingBubbleBg = if (isDark) Color(0xFF23252A).copy(alpha = 0.94f) else TelefamPureWhite.copy(alpha = 0.95f)
  val incomingQuoteBg = if (isDark) {
    Color(theme.accentStartHex).copy(alpha = 0.16f)
  } else {
    Color(theme.accentStartHex).copy(alpha = 0.11f)
  }
  val incomingTextColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF171A20)
  val incomingTimeColor = if (isDark) Color(0xFFA0A7B4) else Color(0xFF6B7280)

  val outStart = if (useThemeAccentBubble) {
    Color(theme.accentStartHex)
  } else {
    Color(if (isDark) bubbleColor.darkStartHex else bubbleColor.lightStartHex)
  }
  val outEnd = if (useThemeAccentBubble) {
    Color(theme.accentEndHex)
  } else {
    Color(if (isDark) bubbleColor.darkEndHex else bubbleColor.lightEndHex)
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .border(
        1.dp,
        if (isDark) Color(0xFF282B31) else Color(0xFFDCE1EA),
        RoundedCornerShape(22.dp),
      )
      .testTag("chat_live_preview"),
    shape = RoundedCornerShape(22.dp),
    color = Color.Transparent,
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(22.dp)),
    ) {
      ChatWallpaperCanvas(
        theme = theme,
        isDark = isDark,
        modifier = Modifier.matchParentSize(),
      )

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Incoming bubble with quoted block showing user's real profile name
        Surface(
          modifier = Modifier
            .widthIn(max = 310.dp)
            .align(Alignment.Start),
          shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 6.dp),
          color = incomingBubbleBg,
          shadowElevation = 2.dp,
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(incomingQuoteBg)
                .padding(end = 10.dp, top = 6.dp, bottom = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier = Modifier
                  .width(3.5.dp)
                  .height(34.dp)
                  .clip(RoundedCornerShape(2.dp))
                  .background(Color(theme.accentEndHex)),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = resolvedSenderName,
                  style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                  ),
                  color = Color(theme.accentEndHex),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.testTag("chat_live_preview_sender"),
                )
                Text(
                  text = stringResource(R.string.chat_theme_preview_line1),
                  style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                  color = incomingTextColor,
                )
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              verticalAlignment = Alignment.Bottom,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = stringResource(R.string.chat_theme_preview_line2),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
                color = incomingTextColor,
                modifier = Modifier.weight(1f, fill = false),
              )
              Text(
                text = "14:05",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = incomingTimeColor,
              )
            }
          }
        }

        // Outgoing bubble matching Image 2
        Box(
          modifier = Modifier
            .align(Alignment.End)
            .widthIn(max = 280.dp)
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 6.dp))
            .background(Brush.horizontalGradient(listOf(outStart, outEnd)))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = stringResource(R.string.chat_theme_preview_reply),
              style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
              ),
              color = TelefamPureWhite,
            )
            Text(
              text = "14:20",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
              color = TelefamPureWhite.copy(alpha = 0.85f),
            )
            Icon(
              imageVector = Icons.Filled.DoneAll,
              contentDescription = null,
              tint = TelefamPureWhite,
              modifier = Modifier.size(15.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
fun TelefamBubbleColorsScreen(
  selectedThemeId: String,
  selectedBubbleColorId: String,
  previewSenderName: String = "Telefam",
  onBack: () -> Unit,
  onSelectBubbleColor: (String) -> Unit,
) {
  val isDark = LocalTelefamDarkTheme.current
  val sheetBg = if (isDark) Color(0xFF0E0F13) else Color(0xFFF6F7FA)
  val cardBg = if (isDark) Color(0xFF17181E) else TelefamPureWhite
  val cardBorder = if (isDark) Color(0xFF262831) else Color(0xFFE5E7EB)
  val titleColor = if (isDark) Color(0xFFF4F5F8) else Color(0xFF14151A)
  val subtitleColor = if (isDark) Color(0xFF9EA4B0) else Color(0xFF5D6472)

  val activeTheme = TelefamChatPrivacyCatalog.themeById(selectedThemeId)
  val activeBubble = TelefamChatPrivacyCatalog.bubbleColorById(selectedBubbleColorId)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(TelefamCrimson),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 16.dp),
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier
          .size(48.dp)
          .testTag("bubble_color_back"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = stringResource(R.string.cd_back),
          tint = TelefamPureWhite,
        )
      }
      Text(
        text = stringResource(R.string.chat_bubble_select_title),
        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
        color = TelefamPureWhite,
        modifier = Modifier.padding(start = 12.dp),
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = stringResource(R.string.chat_bubble_select_sub),
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
        color = TelefamPureWhite.copy(alpha = 0.92f),
        modifier = Modifier.padding(start = 12.dp),
      )
    }

    Surface(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
      color = sheetBg,
    ) {
      BoxWithConstraints(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .navigationBarsPadding()
          .padding(horizontal = 16.dp, vertical = 18.dp),
        contentAlignment = Alignment.TopCenter,
      ) {
        val cols = if (maxWidth >= 680.dp) 3 else 2
        val colors = TelefamChatPrivacyCatalog.bubbleColors

        Column(
          modifier = Modifier
            .widthIn(max = 780.dp)
            .fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          LiveChatThemePreviewCard(
            theme = activeTheme,
            bubbleColor = activeBubble,
            useThemeAccentBubble = false,
            isDark = isDark,
            previewSenderName = previewSenderName,
          )

          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            colors.chunked(cols).forEach { rowColors ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                rowColors.forEach { option ->
                  val selected = option.colorId == activeBubble.colorId
                  val start = Color(if (isDark) option.darkStartHex else option.lightStartHex)
                  val end = Color(if (isDark) option.darkEndHex else option.lightEndHex)
                  val animatedBorder by animateColorAsState(
                    if (selected) end else cardBorder,
                    label = "bubble_card_border",
                  )
                  Surface(
                    modifier = Modifier
                      .weight(1f)
                      .clip(RoundedCornerShape(18.dp))
                      .border(if (selected) 2.dp else 1.dp, animatedBorder, RoundedCornerShape(18.dp))
                      .clickable { onSelectBubbleColor(option.colorId) }
                      .testTag("bubble_color_card_${option.colorId}"),
                    shape = RoundedCornerShape(18.dp),
                    color = cardBg,
                  ) {
                    Column(
                      modifier = Modifier.padding(14.dp),
                      verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Box(
                          modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(start, end))),
                          contentAlignment = Alignment.Center,
                        ) {
                          if (selected) {
                            Icon(
                              imageVector = Icons.Filled.Check,
                              contentDescription = null,
                              tint = TelefamPureWhite,
                              modifier = Modifier.size(18.dp),
                            )
                          }
                        }
                        if (selected) {
                          Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = end,
                            modifier = Modifier.size(20.dp),
                          )
                        }
                      }

                      Box(
                        modifier = Modifier
                          .fillMaxWidth()
                          .height(26.dp)
                          .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 4.dp))
                          .background(Brush.horizontalGradient(listOf(start, end))),
                      )

                      Text(
                        text = option.label,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      Text(
                        text = if (isDark) "Dark adaptive tone" else "Light adaptive tone",
                        style = MaterialTheme.typography.labelSmall,
                        color = subtitleColor,
                      )
                    }
                  }
                }
                repeat(cols - rowColors.size) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }
      }
    }
  }
}

private fun themeEmblemVector(kind: ChatPatternKind): ImageVector = when (kind) {
  ChatPatternKind.CLEAN -> Icons.Outlined.Palette
  ChatPatternKind.HOME -> Icons.Filled.Home
  ChatPatternKind.MEADOW -> Icons.Filled.Spa
  ChatPatternKind.WINTER -> Icons.Filled.AcUnit
  ChatPatternKind.OCEAN -> Icons.Filled.Diamond
  ChatPatternKind.MATH -> Icons.Filled.Calculate
  ChatPatternKind.BOTANICAL -> Icons.Filled.LocalFlorist
  ChatPatternKind.ROMANCE -> Icons.Filled.Favorite
  ChatPatternKind.FIRESIDE -> Icons.Filled.LocalFireDepartment
  ChatPatternKind.ARCADE -> Icons.Filled.SportsEsports
  ChatPatternKind.COSMIC -> Icons.Filled.Public
  ChatPatternKind.SAFARI -> Icons.Filled.WbSunny
  ChatPatternKind.TELEFAM -> Icons.AutoMirrored.Filled.Send
}

/**
 * Luminous Telegram-grade vector line-art doodle wallpaper supporting all 12+1 themes across Light and Dark palettes.
 */
@Composable
fun ChatWallpaperCanvas(
  theme: ChatThemeSpec,
  isDark: Boolean,
  modifier: Modifier = Modifier,
) {
  val baseBg = Color(if (isDark) theme.darkBgHex else theme.lightBgHex)
  val accentStart = Color(theme.accentStartHex)
  val accentEnd = Color(theme.accentEndHex)
  val selectColor = Color(theme.selectionHex)

  val tintAlpha = if (isDark) 0.16f else 0.13f
  val midTintAlpha = if (isDark) 0.08f else 0.07f
  val gradTopLeft = baseBg
  val gradMid = Color(
    red = baseBg.red + (accentStart.red - baseBg.red) * midTintAlpha,
    green = baseBg.green + (accentStart.green - baseBg.green) * midTintAlpha,
    blue = baseBg.blue + (accentStart.blue - baseBg.blue) * midTintAlpha,
    alpha = 1f,
  )
  val gradBottomRight = Color(
    red = baseBg.red + (accentEnd.red - baseBg.red) * tintAlpha,
    green = baseBg.green + (accentEnd.green - baseBg.green) * tintAlpha,
    blue = baseBg.blue + (accentEnd.blue - baseBg.blue) * tintAlpha,
    alpha = 1f,
  )

  val primaryDoodleTint = if (isDark) {
    selectColor.copy(alpha = (theme.darkPatternAlpha * 1.45f).coerceIn(0.14f, 0.28f))
  } else {
    accentStart.copy(alpha = (theme.lightPatternAlpha * 1.35f).coerceIn(0.12f, 0.24f))
  }
  val secondaryDoodleTint = if (isDark) {
    accentEnd.copy(alpha = (theme.darkPatternAlpha * 1.15f).coerceIn(0.11f, 0.22f))
  } else {
    selectColor.copy(alpha = (theme.lightPatternAlpha * 1.15f).coerceIn(0.10f, 0.20f))
  }

  Canvas(modifier = modifier) {
    drawRect(
      brush = Brush.linearGradient(
        colors = listOf(gradTopLeft, gradMid, gradBottomRight),
        start = Offset.Zero,
        end = Offset(size.width, size.height),
      ),
    )

    val maxDim = maxOf(size.width, size.height).coerceAtLeast(1f)
    // Atmospheric radial corner glows for depth
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          accentEnd.copy(alpha = if (isDark) 0.22f else 0.14f),
          Color.Transparent,
        ),
        center = Offset(size.width * 0.88f, size.height * 0.14f),
        radius = maxDim * 0.65f,
      ),
      radius = maxDim * 0.65f,
      center = Offset(size.width * 0.88f, size.height * 0.14f),
    )
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          accentStart.copy(alpha = if (isDark) 0.18f else 0.11f),
          Color.Transparent,
        ),
        center = Offset(size.width * 0.14f, size.height * 0.86f),
        radius = maxDim * 0.62f,
      ),
      radius = maxDim * 0.62f,
      center = Offset(size.width * 0.14f, size.height * 0.86f),
    )

    if (theme.patternKind == ChatPatternKind.CLEAN) {
      val dotStep = 36.dp.toPx()
      val softDot = if (isDark) TelefamPureWhite.copy(alpha = 0.045f) else Color.Black.copy(alpha = 0.035f)
      val accentDot = accentEnd.copy(alpha = if (isDark) 0.08f else 0.06f)
      var row = 0
      var y = dotStep * 0.5f
      while (y < size.height + dotStep) {
        var col = 0
        var x = dotStep * 0.5f + if (row % 2 == 1) dotStep * 0.5f else 0f
        while (x < size.width + dotStep) {
          val isAccent = (row + col) % 3 == 0
          drawCircle(
            color = if (isAccent) accentDot else softDot,
            radius = if (isAccent) 2.dp.toPx() else 1.4.dp.toPx(),
            center = Offset(x, y),
          )
          x += dotStep
          col++
        }
        y += dotStep
        row++
      }
      return@Canvas
    }

    val cellSize = 62.dp.toPx()
    val cols = (size.width / cellSize).toInt() + 2
    val rows = (size.height / cellSize).toInt() + 2
    val stroke = Stroke(width = 1.35.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val fineStroke = Stroke(width = 1.05.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val rotationAngles = floatArrayOf(-18f, 12f, -9f, 22f, 6f, -24f)

    for (r in 0 until rows) {
      for (c in 0 until cols) {
        val offsetX = if (r % 2 == 1) cellSize * 0.48f else 0f
        val jitterY = if ((r + c) % 2 == 0) -cellSize * 0.06f else cellSize * 0.06f
        val cx = c * cellSize + offsetX + cellSize * 0.26f
        val cy = r * cellSize + cellSize * 0.34f + jitterY
        val idx = (r * 3 + c) % 4
        val angle = rotationAngles[(r * 5 + c) % rotationAngles.size]
        val useSecondary = (r + c) % 2 == 1
        val radius = cellSize * if ((r + c) % 3 == 0) 0.34f else 0.28f
        val tint = if (useSecondary) secondaryDoodleTint else primaryDoodleTint

        rotate(degrees = angle, pivot = Offset(cx, cy)) {
          drawDoodleMotif(theme.patternKind, idx, Offset(cx, cy), radius, tint, stroke)
        }

        // Subtle interstitial sparkle accents between motifs
        if ((r + c) % 2 == 0) {
          val sx = cx + cellSize * 0.48f
          val sy = cy + cellSize * 0.42f
          if ((r * 3 + c) % 4 == 0) {
            drawStarPath(Offset(sx, sy), cellSize * 0.075f, secondaryDoodleTint, fineStroke)
          } else {
            drawCircle(
              color = primaryDoodleTint.copy(alpha = primaryDoodleTint.alpha * 0.85f),
              radius = 1.6.dp.toPx(),
              center = Offset(sx, sy),
            )
          }
        }
      }
    }
  }
}

private fun DrawScope.drawDoodleMotif(
  kind: ChatPatternKind,
  variant: Int,
  c: Offset,
  r: Float,
  color: Color,
  stroke: Stroke,
) {
  when (kind) {
    ChatPatternKind.HOME -> when (variant) {
      0 -> { // Cozy house
        val p = Path().apply {
          moveTo(c.x - r, c.y)
          lineTo(c.x, c.y - r)
          lineTo(c.x + r, c.y)
          moveTo(c.x - r * 0.72f, c.y)
          lineTo(c.x - r * 0.72f, c.y + r * 0.85f)
          lineTo(c.x + r * 0.72f, c.y + r * 0.85f)
          lineTo(c.x + r * 0.72f, c.y)
        }
        drawPath(p, color, style = stroke)
      }
      1 -> drawHeartPath(c, r * 0.75f, color, stroke)
      2 -> drawLeafPath(c, r * 0.85f, color, stroke)
      else -> drawCupPath(c, r * 0.72f, color, stroke)
    }

    ChatPatternKind.MEADOW -> when (variant) {
      0 -> { // Butterfly wings
        drawOval(color, Offset(c.x - r * 0.9f, c.y - r * 0.6f), Size(r * 0.85f, r * 1.15f), style = stroke)
        drawOval(color, Offset(c.x + r * 0.05f, c.y - r * 0.6f), Size(r * 0.85f, r * 1.15f), style = stroke)
        drawLine(color, Offset(c.x, c.y - r * 0.6f), Offset(c.x, c.y + r * 0.6f), strokeWidth = stroke.width)
      }
      1 -> drawFlowerPath(c, r * 0.78f, color, stroke)
      2 -> drawLeafPath(c, r * 0.85f, color, stroke)
      else -> { // Ladybug circle
        drawCircle(color, r * 0.68f, c, style = stroke)
        drawLine(color, Offset(c.x, c.y - r * 0.68f), Offset(c.x, c.y + r * 0.68f), strokeWidth = stroke.width)
        drawCircle(color, r * 0.14f, Offset(c.x - r * 0.28f, c.y - r * 0.12f))
        drawCircle(color, r * 0.14f, Offset(c.x + r * 0.28f, c.y + r * 0.15f))
      }
    }

    ChatPatternKind.WINTER -> when (variant) {
      0, 2 -> { // Snowflake 6-axis
        for (k in 0 until 3) {
          val ang = (k * PI / 3.0).toFloat()
          val dx = cos(ang) * r
          val dy = sin(ang) * r
          drawLine(color, Offset(c.x - dx, c.y - dy), Offset(c.x + dx, c.y + dy), strokeWidth = stroke.width)
        }
      }
      1 -> { // Snowman outline
        drawCircle(color, r * 0.36f, Offset(c.x, c.y - r * 0.36f), style = stroke)
        drawCircle(color, r * 0.54f, Offset(c.x, c.y + r * 0.44f), style = stroke)
      }
      else -> drawStarPath(c, r * 0.65f, color, stroke)
    }

    ChatPatternKind.OCEAN -> when (variant) {
      0 -> { // Wave curve
        val p = Path().apply {
          moveTo(c.x - r, c.y)
          quadraticTo(c.x - r * 0.5f, c.y - r * 0.7f, c.x, c.y)
          quadraticTo(c.x + r * 0.5f, c.y + r * 0.7f, c.x + r, c.y)
        }
        drawPath(p, color, style = stroke)
      }
      1 -> { // Diamond facet
        val p = Path().apply {
          moveTo(c.x - r * 0.5f, c.y - r * 0.45f)
          lineTo(c.x + r * 0.5f, c.y - r * 0.45f)
          lineTo(c.x + r * 0.85f, c.y)
          lineTo(c.x, c.y + r * 0.85f)
          lineTo(c.x - r * 0.85f, c.y)
          close()
        }
        drawPath(p, color, style = stroke)
      }
      2 -> { // Fish loop
        drawOval(color, Offset(c.x - r * 0.7f, c.y - r * 0.42f), Size(r * 1.15f, r * 0.84f), style = stroke)
        val tail = Path().apply {
          moveTo(c.x + r * 0.45f, c.y)
          lineTo(c.x + r * 0.92f, c.y - r * 0.42f)
          lineTo(c.x + r * 0.92f, c.y + r * 0.42f)
          close()
        }
        drawPath(tail, color, style = stroke)
      }
      else -> {
        drawCircle(color, r * 0.35f, Offset(c.x - r * 0.3f, c.y), style = stroke)
        drawCircle(color, r * 0.22f, Offset(c.x + r * 0.35f, c.y - r * 0.35f), style = stroke)
      }
    }

    ChatPatternKind.MATH -> when (variant) {
      0 -> { // Triangle + height line
        val p = Path().apply {
          moveTo(c.x, c.y - r * 0.85f)
          lineTo(c.x + r * 0.82f, c.y + r * 0.68f)
          lineTo(c.x - r * 0.82f, c.y + r * 0.68f)
          close()
        }
        drawPath(p, color, style = stroke)
      }
      1 -> { // Radical / square root + axis
        val p = Path().apply {
          moveTo(c.x - r * 0.85f, c.y + r * 0.1f)
          lineTo(c.x - r * 0.45f, c.y + r * 0.65f)
          lineTo(c.x, c.y - r * 0.65f)
          lineTo(c.x + r * 0.82f, c.y - r * 0.65f)
        }
        drawPath(p, color, style = stroke)
      }
      2 -> { // Compass circles
        drawCircle(color, r * 0.62f, c, style = stroke)
        drawLine(color, Offset(c.x - r * 0.8f, c.y), Offset(c.x + r * 0.8f, c.y), strokeWidth = stroke.width)
      }
      else -> { // Plus & Sigma lines
        drawLine(color, Offset(c.x - r * 0.55f, c.y), Offset(c.x + r * 0.55f, c.y), strokeWidth = stroke.width)
        drawLine(color, Offset(c.x, c.y - r * 0.55f), Offset(c.x, c.y + r * 0.55f), strokeWidth = stroke.width)
      }
    }

    ChatPatternKind.BOTANICAL -> when (variant) {
      0 -> drawFlowerPath(c, r * 0.8f, color, stroke)
      1 -> drawLeafPath(c, r * 0.88f, color, stroke)
      2 -> drawStarPath(c, r * 0.65f, color, stroke)
      else -> { // Tulip cup
        val p = Path().apply {
          moveTo(c.x - r * 0.55f, c.y - r * 0.5f)
          lineTo(c.x - r * 0.45f, c.y + r * 0.2f)
          quadraticTo(c.x, c.y + r * 0.5f, c.x + r * 0.45f, c.y + r * 0.2f)
          lineTo(c.x + r * 0.55f, c.y - r * 0.5f)
          lineTo(c.x, c.y - r * 0.15f)
          close()
        }
        drawPath(p, color, style = stroke)
        drawLine(color, Offset(c.x, c.y + r * 0.35f), Offset(c.x, c.y + r * 0.9f), strokeWidth = stroke.width)
      }
    }

    ChatPatternKind.ROMANCE -> when (variant) {
      0, 2 -> drawHeartPath(c, r * 0.78f, color, stroke)
      1 -> { // Cassette rectangle
        drawRoundRect(color, Offset(c.x - r * 0.82f, c.y - r * 0.52f), Size(r * 1.64f, r * 1.04f), CornerRadius(6f, 6f), style = stroke)
        drawCircle(color, r * 0.2f, Offset(c.x - r * 0.34f, c.y), style = stroke)
        drawCircle(color, r * 0.2f, Offset(c.x + r * 0.34f, c.y), style = stroke)
      }
      else -> drawStarPath(c, r * 0.68f, color, stroke)
    }

    ChatPatternKind.FIRESIDE -> when (variant) {
      0 -> { // Evergreens / pine
        val p = Path().apply {
          moveTo(c.x, c.y - r * 0.88f)
          lineTo(c.x + r * 0.68f, c.y + r * 0.42f)
          lineTo(c.x - r * 0.68f, c.y + r * 0.42f)
          close()
        }
        drawPath(p, color, style = stroke)
        drawLine(color, Offset(c.x, c.y + r * 0.42f), Offset(c.x, c.y + r * 0.85f), strokeWidth = stroke.width)
      }
      1 -> { // Hearth arch
        drawRoundRect(color, Offset(c.x - r * 0.76f, c.y - r * 0.55f), Size(r * 1.52f, r * 1.1f), CornerRadius(10f, 10f), style = stroke)
      }
      2 -> drawCupPath(c, r * 0.72f, color, stroke)
      else -> drawStarPath(c, r * 0.7f, color, stroke)
    }

    ChatPatternKind.ARCADE -> when (variant) {
      0 -> { // Controller pill
        drawRoundRect(color, Offset(c.x - r * 0.85f, c.y - r * 0.48f), Size(r * 1.7f, r * 0.96f), CornerRadius(16f, 16f), style = stroke)
        drawCircle(color, r * 0.14f, Offset(c.x + r * 0.42f, c.y - r * 0.12f))
        drawCircle(color, r * 0.14f, Offset(c.x + r * 0.42f, c.y + r * 0.16f))
      }
      1 -> { // Pixel gear
        drawCircle(color, r * 0.64f, c, style = stroke)
        drawCircle(color, r * 0.25f, c, style = stroke)
      }
      2 -> { // Cabinet box
        drawRoundRect(color, Offset(c.x - r * 0.58f, c.y - r * 0.78f), Size(r * 1.16f, r * 1.56f), CornerRadius(8f, 8f), style = stroke)
      }
      else -> drawStarPath(c, r * 0.65f, color, stroke)
    }

    ChatPatternKind.COSMIC -> when (variant) {
      0, 2 -> { // Ringed planet
        drawCircle(color, r * 0.5f, c, style = stroke)
        drawOval(color, Offset(c.x - r * 0.95f, c.y - r * 0.22f), Size(r * 1.9f, r * 0.44f), style = stroke)
      }
      1 -> drawStarPath(c, r * 0.72f, color, stroke)
      else -> {
        drawCircle(color, r * 0.28f, Offset(c.x - r * 0.35f, c.y + r * 0.2f), style = stroke)
        drawCircle(color, r * 0.16f, Offset(c.x + r * 0.4f, c.y - r * 0.3f), style = stroke)
      }
    }

    ChatPatternKind.SAFARI -> when (variant) {
      0 -> { // Horizon sun
        drawCircle(color, r * 0.58f, c, style = stroke)
        drawLine(color, Offset(c.x - r * 0.95f, c.y + r * 0.58f), Offset(c.x + r * 0.95f, c.y + r * 0.58f), strokeWidth = stroke.width)
      }
      1 -> drawLeafPath(c, r * 0.82f, color, stroke)
      2 -> { // Acacia silhouette line
        drawOval(color, Offset(c.x - r * 0.85f, c.y - r * 0.6f), Size(r * 1.7f, r * 0.65f), style = stroke)
        drawLine(color, Offset(c.x, c.y + r * 0.05f), Offset(c.x, c.y + r * 0.82f), strokeWidth = stroke.width)
      }
      else -> drawStarPath(c, r * 0.6f, color, stroke)
    }

    ChatPatternKind.TELEFAM -> when (variant) {
      0, 2 -> { // Paper plane
        val p = Path().apply {
          moveTo(c.x - r * 0.85f, c.y)
          lineTo(c.x + r * 0.85f, c.y - r * 0.68f)
          lineTo(c.x + r * 0.2f, c.y + r * 0.76f)
          lineTo(c.x - r * 0.15f, c.y + r * 0.15f)
          close()
        }
        drawPath(p, color, style = stroke)
      }
      1 -> drawHeartPath(c, r * 0.72f, color, stroke)
      else -> {
        drawRoundRect(color, Offset(c.x - r * 0.7f, c.y - r * 0.5f), Size(r * 1.4f, r * 0.95f), CornerRadius(12f, 12f), style = stroke)
      }
    }

    ChatPatternKind.CLEAN -> Unit
  }
}

private fun DrawScope.drawHeartPath(c: Offset, r: Float, color: Color, stroke: Stroke) {
  val p = Path().apply {
    moveTo(c.x, c.y + r * 0.8f)
    cubicTo(c.x - r * 1.15f, c.y + r * 0.1f, c.x - r * 0.9f, c.y - r * 0.85f, c.x, c.y - r * 0.22f)
    cubicTo(c.x + r * 0.9f, c.y - r * 0.85f, c.x + r * 1.15f, c.y + r * 0.1f, c.x, c.y + r * 0.8f)
    close()
  }
  drawPath(p, color, style = stroke)
}

private fun DrawScope.drawLeafPath(c: Offset, r: Float, color: Color, stroke: Stroke) {
  val p = Path().apply {
    moveTo(c.x - r * 0.7f, c.y + r * 0.7f)
    quadraticTo(c.x - r * 0.6f, c.y - r * 0.7f, c.x + r * 0.7f, c.y - r * 0.7f)
    quadraticTo(c.x + r * 0.6f, c.y + r * 0.7f, c.x - r * 0.7f, c.y + r * 0.7f)
    close()
  }
  drawPath(p, color, style = stroke)
  drawLine(color, Offset(c.x - r * 0.6f, c.y + r * 0.6f), Offset(c.x + r * 0.45f, c.y - r * 0.45f), strokeWidth = stroke.width)
}

private fun DrawScope.drawCupPath(c: Offset, r: Float, color: Color, stroke: Stroke) {
  drawRoundRect(color, Offset(c.x - r * 0.56f, c.y - r * 0.42f), Size(r * 1.0f, r * 0.98f), CornerRadius(6f, 6f), style = stroke)
  drawArc(color, -90f, 180f, false, Offset(c.x + r * 0.35f, c.y - r * 0.25f), Size(r * 0.45f, r * 0.55f), style = stroke)
}

private fun DrawScope.drawFlowerPath(c: Offset, r: Float, color: Color, stroke: Stroke) {
  for (i in 0 until 5) {
    val ang = (i * 2.0 * PI / 5.0).toFloat()
    drawCircle(color, r * 0.34f, Offset(c.x + cos(ang) * r * 0.44f, c.y + sin(ang) * r * 0.44f), style = stroke)
  }
  drawCircle(color, r * 0.2f, c, style = stroke)
}

private fun DrawScope.drawStarPath(c: Offset, r: Float, color: Color, stroke: Stroke) {
  val p = Path().apply {
    moveTo(c.x, c.y - r)
    lineTo(c.x + r * 0.24f, c.y - r * 0.24f)
    lineTo(c.x + r, c.y)
    lineTo(c.x + r * 0.24f, c.y + r * 0.24f)
    lineTo(c.x, c.y + r)
    lineTo(c.x - r * 0.24f, c.y + r * 0.24f)
    lineTo(c.x - r, c.y)
    lineTo(c.x - r * 0.24f, c.y - r * 0.24f)
    close()
  }
  drawPath(p, color, style = stroke)
}
