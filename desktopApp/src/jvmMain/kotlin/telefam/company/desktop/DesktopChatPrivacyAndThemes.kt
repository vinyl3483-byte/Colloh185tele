package telefam.company.desktop

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class DesktopChatAudienceField(
  val allowContacts: Boolean,
  val title: String,
  val subtitle: String,
) {
  MESSAGE_REQUESTS(true, "Message Requests", "Set who can send you message requests."),
  WHO_CAN_CALL(true, "Who Can Call Me", "Control who can call you."),
  WHO_CAN_SCREENSHOT(false, "Who Can Screenshot Chats", "Choose who can take screenshots of your chats."),
  WHO_CAN_SHARE_CHATS(false, "Who Can ShareChats", "Control who can share your chats."),
  WHO_CAN_COPY_MESSAGES(false, "Who Can Copy Messages", "Control who can copy your messages."),
  WHO_CAN_DOWNLOAD_MEDIA(false, "Who Can Download My Media", "Control who can download your photos, videos and files."),
  READ_RECEIPTS(false, "Read Receipts", "Control who can see if you read messages."),
  TYPING_INDICATORS(false, "Typing Indicators", "Control who can see when you are typing."),
  ONLINE_INDICATORS(false, "Online Indicators", "Control who can see when you are online."),
}

private enum class DesktopChatPrivacySvg {
  BACK_ARROW,
  CHEVRON_RIGHT,
  MESSAGE_BUBBLE,
  CALL_PHONE,
  SCREENSHOT_PHONE,
  SHARE_NODES,
  COPY_DOCS,
  DOWNLOAD_ARROW,
  PALETTE,
  LETTER_T,
  DOUBLE_CHECK,
  TYPING_DOTS,
  ONLINE_EYE,
  SLASH_PALETTE,
  HOME_MOTIF,
  MEADOW_MOTIF,
  WINTER_MOTIF,
  OCEAN_MOTIF,
  MATH_MOTIF,
  BOTANICAL_MOTIF,
  ROMANCE_MOTIF,
  FIRESIDE_MOTIF,
  ARCADE_MOTIF,
  COSMIC_MOTIF,
  SAFARI_MOTIF,
  TELEFAM_MOTIF,
}

@Composable
fun DesktopChatPrivacyHostScreen(
  route: DesktopChatPrivacyRoute,
  isDark: Boolean,
  state: DesktopChatPrivacySettings,
  previewSenderName: String = "Telefam",
  onNavigateBack: () -> Unit,
  onUpdatePrivacy: (DesktopChatPrivacySettings) -> Unit,
  onOpenChatsTheme: () -> Unit,
  onOpenBubbleColors: () -> Unit,
  onSelectTheme: (String) -> Unit,
  onSelectBubbleColor: (String) -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .onPreviewKeyEvent { ev ->
        if (ev.type == KeyEventType.KeyDown && ev.key == Key.Escape) {
          onNavigateBack()
          true
        } else {
          false
        }
      },
  ) {
    when (route) {
      DesktopChatPrivacyRoute.PRIVACY -> DesktopChatPrivacyMainScreen(
        isDark = isDark,
        state = state,
        onBack = onNavigateBack,
        onUpdatePrivacy = onUpdatePrivacy,
        onOpenChatsTheme = onOpenChatsTheme,
        onOpenBubbleColors = onOpenBubbleColors,
      )

      DesktopChatPrivacyRoute.CHATS_THEME -> DesktopChatsThemePickerScreen(
        isDark = isDark,
        selectedThemeId = state.chatThemeId,
        selectedBubbleColorId = state.bubbleColorId,
        previewSenderName = previewSenderName,
        onBack = onNavigateBack,
        onSelectTheme = onSelectTheme,
      )

      DesktopChatPrivacyRoute.BUBBLE_COLORS -> DesktopBubbleColorsPickerScreen(
        isDark = isDark,
        selectedThemeId = state.chatThemeId,
        selectedBubbleColorId = state.bubbleColorId,
        previewSenderName = previewSenderName,
        onBack = onNavigateBack,
        onSelectBubbleColor = onSelectBubbleColor,
      )
    }
  }
}

@Composable
private fun DesktopChatPrivacyMainScreen(
  isDark: Boolean,
  state: DesktopChatPrivacySettings,
  onBack: () -> Unit,
  onUpdatePrivacy: (DesktopChatPrivacySettings) -> Unit,
  onOpenChatsTheme: () -> Unit,
  onOpenBubbleColors: () -> Unit,
) {
  val crimson = Color(0xFFD91E2A)
  val sheetBg = if (isDark) Color(0xFF0E0F13) else Color(0xFFF6F7FA)
  val cardBg = if (isDark) Color(0xFF17181E) else Color.White
  val cardBorder = if (isDark) Color(0xFF262831) else Color(0xFFE5E7EB)
  val titleColor = if (isDark) Color(0xFFF4F5F8) else Color(0xFF14151A)
  val subtitleColor = if (isDark) Color(0xFF9EA4B0) else Color(0xFF5D6472)
  val badgeBg = if (isDark) Color(0xFF331116) else Color(0xFFFDECEC)
  val badgeTint = if (isDark) Color(0xFFEA2E37) else crimson

  var pickingField by remember { mutableStateOf<DesktopChatAudienceField?>(null) }

  val activeTheme = DesktopChatPrivacyCatalog.themeById(state.chatThemeId)
  val activeBubble = DesktopChatPrivacyCatalog.bubbleColorById(state.bubbleColorId)
  val bubbleSwatchColor = Color(if (isDark) activeBubble.darkStartHex else activeBubble.lightStartHex)

  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(crimson),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onBack),
          contentAlignment = Alignment.Center,
        ) {
          DesktopChatPrivacyVectorIcon(DesktopChatPrivacySvg.BACK_ARROW, Color.White, 22.dp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Privacy",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = "Control who can interact with you and your messages.",
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 13.5.sp,
          )
        }
      }

      Surface(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = sheetBg,
      ) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.TopCenter,
        ) {
          LazyColumn(
            modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            item {
              DesktopChatPrivacyRowCard(
                icon = DesktopChatPrivacySvg.MESSAGE_BUBBLE,
                title = "Message Requests",
                subtitle = "Set who can send you message requests.",
                valueLabel = state.messageRequests.label,
                cardBg = cardBg,
                cardBorder = cardBorder,
                titleColor = titleColor,
                subtitleColor = subtitleColor,
                badgeBg = badgeBg,
                badgeTint = badgeTint,
                crimson = crimson,
                onClick = { pickingField = DesktopChatAudienceField.MESSAGE_REQUESTS },
              )
            }
            item {
              DesktopChatPrivacyRowCard(
                icon = DesktopChatPrivacySvg.CALL_PHONE,
                title = "Who Can Call Me",
                subtitle = "Control who can call you.",
                valueLabel = state.whoCanCall.label,
                cardBg = cardBg,
                cardBorder = cardBorder,
                titleColor = titleColor,
                subtitleColor = subtitleColor,
                badgeBg = badgeBg,
                badgeTint = badgeTint,
                crimson = crimson,
                onClick = { pickingField = DesktopChatAudienceField.WHO_CAN_CALL },
              )
            }
            item {
              DesktopChatPrivacyRowCard(
                icon = DesktopChatPrivacySvg.SCREENSHOT_PHONE,
                title = "Who Can Screenshot Chats",
                subtitle = "Choose who can take screenshots of your chats.",
                valueLabel = state.whoCanScreenshot.label,
                cardBg = cardBg,
                cardBorder = cardBorder,
                titleColor = titleColor,
                subtitleColor = subtitleColor,
                badgeBg = badgeBg,
                badgeTint = badgeTint,
                crimson = crimson,
                onClick = { pickingField = DesktopChatAudienceField.WHO_CAN_SCREENSHOT },
              )
            }
            item {
              DesktopChatPrivacyRowCard(
                icon = DesktopChatPrivacySvg.SHARE_NODES,
                title = "Who Can ShareChats",
                subtitle = "Control who can share your chats.",
                valueLabel = state.whoCanShareChats.label,
                cardBg = cardBg,
                cardBorder = cardBorder,
                titleColor = titleColor,
                subtitleColor = subtitleColor,
                badgeBg = badgeBg,
                badgeTint = badgeTint,
                crimson = crimson,
                onClick = { pickingField = DesktopChatAudienceField.WHO_CAN_SHARE_CHATS },
              )
            }
            item {
              DesktopChatPrivacyRowCard(
                icon = DesktopChatPrivacySvg.COPY_DOCS,
                title = "Who Can Copy Messages",
                subtitle = "Control who can copy your messages.",
                valueLabel = state.whoCanCopyMessages.label,
                cardBg = cardBg,
                cardBorder = cardBorder,
                titleColor = titleColor,
                subtitleColor = subtitleColor,
                badgeBg = badgeBg,
                badgeTint = badgeTint,
                crimson = crimson,
                onClick = { pickingField = DesktopChatAudienceField.WHO_CAN_COPY_MESSAGES },
              )
            }
            item {
              DesktopChatPrivacyRowCard(
                icon = DesktopChatPrivacySvg.DOWNLOAD_ARROW,
                title = "Who Can Download My Media",
                subtitle = "Control who can download your photos, videos and files.",
                valueLabel = state.whoCanDownloadMedia.label,
                cardBg = cardBg,
                cardBorder = cardBorder,
                titleColor = titleColor,
                subtitleColor = subtitleColor,
                badgeBg = badgeBg,
                badgeTint = badgeTint,
                crimson = crimson,
                onClick = { pickingField = DesktopChatAudienceField.WHO_CAN_DOWNLOAD_MEDIA },
              )
            }
            item {
              DesktopChatPrivacyThemeRowCard(
                activeTheme = activeTheme,
                cardBg = cardBg,
                cardBorder = cardBorder,
                titleColor = titleColor,
                subtitleColor = subtitleColor,
                badgeBg = badgeBg,
                badgeTint = badgeTint,
                crimson = crimson,
                onClick = onOpenChatsTheme,
              )
            }
            item {
              DesktopChatPrivacyBubbleRowCard(
                bubbleSwatchColor = bubbleSwatchColor,
                cardBg = cardBg,
                cardBorder = cardBorder,
                titleColor = titleColor,
                subtitleColor = subtitleColor,
                badgeBg = badgeBg,
                badgeTint = badgeTint,
                onClick = onOpenBubbleColors,
              )
            }
            item {
              DesktopChatPrivacyRowCard(
                icon = DesktopChatPrivacySvg.DOUBLE_CHECK,
                title = "Read Receipts",
                subtitle = "Control who can see if you read messages.",
                valueLabel = state.readReceipts.label,
                cardBg = cardBg,
                cardBorder = cardBorder,
                titleColor = titleColor,
                subtitleColor = subtitleColor,
                badgeBg = badgeBg,
                badgeTint = badgeTint,
                crimson = crimson,
                onClick = { pickingField = DesktopChatAudienceField.READ_RECEIPTS },
              )
            }
            item {
              DesktopChatPrivacyRowCard(
                icon = DesktopChatPrivacySvg.TYPING_DOTS,
                title = "Typing Indicators",
                subtitle = "Control who can see when you are typing.",
                valueLabel = state.typingIndicators.label,
                cardBg = cardBg,
                cardBorder = cardBorder,
                titleColor = titleColor,
                subtitleColor = subtitleColor,
                badgeBg = badgeBg,
                badgeTint = badgeTint,
                crimson = crimson,
                onClick = { pickingField = DesktopChatAudienceField.TYPING_INDICATORS },
              )
            }
            item {
              DesktopChatPrivacyRowCard(
                icon = DesktopChatPrivacySvg.ONLINE_EYE,
                title = "Online Indicators",
                subtitle = "Control who can see when you are online.",
                valueLabel = state.onlineIndicators.label,
                cardBg = cardBg,
                cardBorder = cardBorder,
                titleColor = titleColor,
                subtitleColor = subtitleColor,
                badgeBg = badgeBg,
                badgeTint = badgeTint,
                crimson = crimson,
                onClick = { pickingField = DesktopChatAudienceField.ONLINE_INDICATORS },
              )
            }
          }
        }
      }
    }

    val field = pickingField
    if (field != null) {
      val currentChoice = when (field) {
        DesktopChatAudienceField.MESSAGE_REQUESTS -> state.messageRequests
        DesktopChatAudienceField.WHO_CAN_CALL -> state.whoCanCall
        DesktopChatAudienceField.WHO_CAN_SCREENSHOT -> state.whoCanScreenshot
        DesktopChatAudienceField.WHO_CAN_SHARE_CHATS -> state.whoCanShareChats
        DesktopChatAudienceField.WHO_CAN_COPY_MESSAGES -> state.whoCanCopyMessages
        DesktopChatAudienceField.WHO_CAN_DOWNLOAD_MEDIA -> state.whoCanDownloadMedia
        DesktopChatAudienceField.READ_RECEIPTS -> state.readReceipts
        DesktopChatAudienceField.TYPING_INDICATORS -> state.typingIndicators
        DesktopChatAudienceField.ONLINE_INDICATORS -> state.onlineIndicators
      }
      val options = if (field.allowContacts) {
        DesktopChatPrivacyCatalog.audienceOptions4
      } else {
        DesktopChatPrivacyCatalog.audienceOptions3
      }
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.56f))
          .clickable { pickingField = null },
        contentAlignment = Alignment.Center,
      ) {
        Surface(
          modifier = Modifier
            .widthIn(min = 320.dp, max = 440.dp)
            .padding(24.dp)
            .clickable(enabled = false, onClick = {}),
          shape = RoundedCornerShape(22.dp),
          color = cardBg,
          border = BorderStroke(1.dp, cardBorder),
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Text(
              text = field.title,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = titleColor,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = field.subtitle,
              fontSize = 13.sp,
              color = subtitleColor,
            )
            Spacer(modifier = Modifier.height(14.dp))
            options.forEach { opt ->
              val selected = opt == currentChoice
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable {
                    val updated = when (field) {
                      DesktopChatAudienceField.MESSAGE_REQUESTS -> state.copy(messageRequests = opt)
                      DesktopChatAudienceField.WHO_CAN_CALL -> state.copy(whoCanCall = opt)
                      DesktopChatAudienceField.WHO_CAN_SCREENSHOT -> state.copy(whoCanScreenshot = opt)
                      DesktopChatAudienceField.WHO_CAN_SHARE_CHATS -> state.copy(whoCanShareChats = opt)
                      DesktopChatAudienceField.WHO_CAN_COPY_MESSAGES -> state.copy(whoCanCopyMessages = opt)
                      DesktopChatAudienceField.WHO_CAN_DOWNLOAD_MEDIA -> state.copy(whoCanDownloadMedia = opt)
                      DesktopChatAudienceField.READ_RECEIPTS -> state.copy(readReceipts = opt)
                      DesktopChatAudienceField.TYPING_INDICATORS -> state.copy(typingIndicators = opt)
                      DesktopChatAudienceField.ONLINE_INDICATORS -> state.copy(onlineIndicators = opt)
                    }
                    onUpdatePrivacy(updated)
                    pickingField = null
                  }
                  .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                RadioButton(
                  selected = selected,
                  onClick = null,
                  colors = RadioButtonDefaults.colors(selectedColor = crimson),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = opt.label,
                  fontSize = 15.sp,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  color = if (selected) crimson else titleColor,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DesktopChatPrivacyRowCard(
  icon: DesktopChatPrivacySvg,
  title: String,
  subtitle: String,
  valueLabel: String,
  cardBg: Color,
  cardBorder: Color,
  titleColor: Color,
  subtitleColor: Color,
  badgeBg: Color,
  badgeTint: Color,
  crimson: Color,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(18.dp),
    color = cardBg,
    border = BorderStroke(1.dp, cardBorder),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier.size(42.dp).clip(CircleShape).background(badgeBg),
        contentAlignment = Alignment.Center,
      ) {
        DesktopChatPrivacyVectorIcon(icon, badgeTint, 21.dp)
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(title, fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = titleColor)
        Spacer(modifier = Modifier.height(2.dp))
        Text(subtitle, fontSize = 12.5.sp, lineHeight = 16.5.sp, color = subtitleColor)
      }
      Spacer(modifier = Modifier.width(10.dp))
      Text(valueLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = crimson)
      Spacer(modifier = Modifier.width(6.dp))
      DesktopChatPrivacyVectorIcon(DesktopChatPrivacySvg.CHEVRON_RIGHT, subtitleColor, 18.dp)
    }
  }
}

@Composable
private fun DesktopChatPrivacyThemeRowCard(
  activeTheme: DesktopChatThemeSpec,
  cardBg: Color,
  cardBorder: Color,
  titleColor: Color,
  subtitleColor: Color,
  badgeBg: Color,
  badgeTint: Color,
  crimson: Color,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(18.dp),
    color = cardBg,
    border = BorderStroke(1.dp, cardBorder),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier.size(42.dp).clip(CircleShape).background(badgeBg),
        contentAlignment = Alignment.Center,
      ) {
        DesktopChatPrivacyVectorIcon(DesktopChatPrivacySvg.PALETTE, badgeTint, 21.dp)
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text("Chats Theme", fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = titleColor)
        Spacer(modifier = Modifier.height(2.dp))
        Text("Choose your chat background theme.", fontSize = 12.5.sp, color = subtitleColor)
      }
      Spacer(modifier = Modifier.width(10.dp))
      if (activeTheme.themeId == "none") {
        DesktopChatPrivacyVectorIcon(DesktopChatPrivacySvg.SLASH_PALETTE, crimson, 24.dp)
      } else {
        Box(
          modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(activeTheme.accentStartHex), Color(activeTheme.accentEndHex))))
            .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape),
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      DesktopChatPrivacyVectorIcon(DesktopChatPrivacySvg.CHEVRON_RIGHT, subtitleColor, 18.dp)
    }
  }
}

@Composable
private fun DesktopChatPrivacyBubbleRowCard(
  bubbleSwatchColor: Color,
  cardBg: Color,
  cardBorder: Color,
  titleColor: Color,
  subtitleColor: Color,
  badgeBg: Color,
  badgeTint: Color,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(18.dp),
    color = cardBg,
    border = BorderStroke(1.dp, cardBorder),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier.size(42.dp).clip(CircleShape).background(badgeBg),
        contentAlignment = Alignment.Center,
      ) {
        DesktopChatPrivacyVectorIcon(DesktopChatPrivacySvg.LETTER_T, badgeTint, 20.dp)
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text("Message Bubbles Colour", fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = titleColor)
        Spacer(modifier = Modifier.height(2.dp))
        Text("Choose the colour of your message bubbles.", fontSize = 12.5.sp, color = subtitleColor)
      }
      Spacer(modifier = Modifier.width(10.dp))
      Box(
        modifier = Modifier
          .size(26.dp)
          .clip(CircleShape)
          .background(bubbleSwatchColor),
      )
      Spacer(modifier = Modifier.width(8.dp))
      DesktopChatPrivacyVectorIcon(DesktopChatPrivacySvg.CHEVRON_RIGHT, subtitleColor, 18.dp)
    }
  }
}

@Composable
private fun DesktopChatsThemePickerScreen(
  isDark: Boolean,
  selectedThemeId: String,
  selectedBubbleColorId: String,
  previewSenderName: String = "Telefam",
  onBack: () -> Unit,
  onSelectTheme: (String) -> Unit,
) {
  var draftThemeId by remember(selectedThemeId) { mutableStateOf(selectedThemeId) }
  val activeTheme = DesktopChatPrivacyCatalog.themeById(draftThemeId)
  val activeBubble = DesktopChatPrivacyCatalog.bubbleColorById(selectedBubbleColorId)

  val pageBg = if (isDark) Color(0xFF0A0B0F) else Color(0xFFF3F4F8)
  val panelBg = if (isDark) Color(0xFF17191D) else Color.White
  val panelBorder = if (isDark) Color(0xFF282B33) else Color(0xFFE5E7EB)
  val headerTitleColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF14151A)
  val sectionLabelColor = if (isDark) Color(activeTheme.selectionHex) else Color(activeTheme.accentStartHex)

  Column(
    modifier = Modifier.fillMaxSize().background(pageBg),
  ) {
    Surface(
      color = panelBg,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack),
          contentAlignment = Alignment.Center,
        ) {
          DesktopChatPrivacyVectorIcon(DesktopChatPrivacySvg.BACK_ARROW, headerTitleColor, 21.dp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          text = "Chats Theme",
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold,
          color = headerTitleColor,
        )
      }
    }
    HorizontalDivider(color = panelBorder)

    BoxWithConstraints(
      modifier = Modifier.weight(1f).fillMaxWidth(),
    ) {
      val gridColumns = when {
        maxWidth >= 900.dp -> 5
        maxWidth >= 600.dp -> 4
        else -> 3
      }
      val rows = DesktopChatPrivacyCatalog.themes.chunked(gridColumns)

      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Column(
          modifier = Modifier.widthIn(max = 920.dp).fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = panelBg,
            border = BorderStroke(1.dp, panelBorder),
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Text(
                text = "Select theme",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = sectionLabelColor,
                modifier = Modifier.padding(start = 2.dp, bottom = 14.dp),
              )

              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { rowItems ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    rowItems.forEach { theme ->
                      DesktopChatThemeTile(
                        theme = theme,
                        isDark = isDark,
                        selected = theme.themeId == draftThemeId,
                        modifier = Modifier.weight(1f),
                        onClick = { draftThemeId = theme.themeId },
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

          DesktopLiveChatPreviewCard(
            theme = activeTheme,
            bubbleColor = activeBubble,
            isDark = isDark,
            previewSenderName = previewSenderName,
          )

          Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            OutlinedButton(
              onClick = { draftThemeId = "none" },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.weight(1f).height(48.dp),
            ) {
              Text("Reset default", fontWeight = FontWeight.SemiBold)
            }
            Button(
              onClick = { onSelectTheme(draftThemeId) },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD91E2A)),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.weight(1f).height(48.dp),
            ) {
              Text("Apply Theme", color = Color.White, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DesktopChatThemeTile(
  theme: DesktopChatThemeSpec,
  isDark: Boolean,
  selected: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  val borderColor by animateColorAsState(
    targetValue = if (selected) Color(theme.selectionHex) else Color.White.copy(alpha = if (isDark) 0.14f else 0.08f),
  )
  val borderWidth = if (selected) 2.5.dp else 1.dp
  val incomingColor = if (isDark) Color(0xFF282B31) else Color(0xFFFFFFFF).copy(alpha = 0.94f)
  val accentStart = Color(theme.accentStartHex)
  val accentEnd = Color(theme.accentEndHex)

  Box(
    modifier = modifier
      .aspectRatio(0.76f)
      .clip(RoundedCornerShape(16.dp))
      .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
      .clickable(onClick = onClick),
  ) {
    DesktopChatWallpaperCanvas(
      theme = theme,
      isDark = isDark,
      modifier = Modifier.fillMaxSize(),
    )

    Column(
      modifier = Modifier.fillMaxSize().padding(10.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
          modifier = Modifier
            .align(Alignment.End)
            .fillMaxWidth(0.68f)
            .height(22.dp)
            .clip(RoundedCornerShape(topStart = 11.dp, topEnd = 11.dp, bottomStart = 11.dp, bottomEnd = 3.dp))
            .background(Brush.horizontalGradient(listOf(accentStart, accentEnd))),
        )
        Box(
          modifier = Modifier
            .align(Alignment.Start)
            .fillMaxWidth(0.68f)
            .height(22.dp)
            .clip(RoundedCornerShape(topStart = 11.dp, topEnd = 11.dp, bottomEnd = 11.dp, bottomStart = 3.dp))
            .background(incomingColor),
        )
      }

      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = if (isDark) 0.36f else 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          DesktopChatPrivacyVectorIcon(
            kind = themeMotifSvg(theme.patternKind),
            tint = if (theme.themeId == "none") {
              if (isDark) Color(0xFFE5E7EB) else Color(0xFF1F2937)
            } else {
              accentEnd
            },
            size = 19.dp,
          )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
          text = theme.title,
          fontSize = 11.sp,
          fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
          color = if (isDark) Color(0xFFF3F4F6) else Color(0xFF14151A),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun DesktopBubbleColorsPickerScreen(
  isDark: Boolean,
  selectedThemeId: String,
  selectedBubbleColorId: String,
  previewSenderName: String = "Telefam",
  onBack: () -> Unit,
  onSelectBubbleColor: (String) -> Unit,
) {
  var draftColorId by remember(selectedBubbleColorId) { mutableStateOf(selectedBubbleColorId) }
  val activeTheme = DesktopChatPrivacyCatalog.themeById(selectedThemeId)
  val activeBubble = DesktopChatPrivacyCatalog.bubbleColorById(draftColorId)

  val pageBg = if (isDark) Color(0xFF0A0B0F) else Color(0xFFF3F4F8)
  val panelBg = if (isDark) Color(0xFF17191D) else Color.White
  val panelBorder = if (isDark) Color(0xFF282B33) else Color(0xFFE5E7EB)
  val headerTitleColor = if (isDark) Color(0xFFF3F4F6) else Color(0xFF14151A)
  val mutedColor = if (isDark) Color(0xFF9EA4B0) else Color(0xFF5D6472)

  Column(modifier = Modifier.fillMaxSize().background(pageBg)) {
    Surface(color = panelBg, modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack),
          contentAlignment = Alignment.Center,
        ) {
          DesktopChatPrivacyVectorIcon(DesktopChatPrivacySvg.BACK_ARROW, headerTitleColor, 21.dp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text("Message Bubbles Colour", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = headerTitleColor)
      }
    }
    HorizontalDivider(color = panelBorder)

    BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
      val cols = if (maxWidth >= 600.dp) 3 else 2
      val rows = DesktopChatPrivacyCatalog.bubbleColors.chunked(cols)

      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Column(
          modifier = Modifier.widthIn(max = 780.dp).fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = panelBg,
            border = BorderStroke(1.dp, panelBorder),
          ) {
            Column(modifier = Modifier.padding(18.dp)) {
              Text(
                text = "6 Bubble Colours",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = headerTitleColor,
                modifier = Modifier.padding(bottom = 14.dp),
              )
              Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEach { rowColors ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                  ) {
                    rowColors.forEach { option ->
                      val picked = option.colorId == draftColorId
                      val startC = Color(if (isDark) option.darkStartHex else option.lightStartHex)
                      val endC = Color(if (isDark) option.darkEndHex else option.lightEndHex)
                      Surface(
                        onClick = { draftColorId = option.colorId },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = if (picked) startC.copy(alpha = 0.14f) else pageBg,
                        border = BorderStroke(if (picked) 2.dp else 1.dp, if (picked) startC else panelBorder),
                      ) {
                        Column(
                          modifier = Modifier.padding(vertical = 16.dp, horizontal = 10.dp),
                          horizontalAlignment = Alignment.CenterHorizontally,
                          verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                          Box(
                            modifier = Modifier
                              .size(46.dp)
                              .clip(CircleShape)
                              .background(Brush.linearGradient(listOf(startC, endC)))
                              .border(2.dp, Color.White.copy(alpha = 0.45f), CircleShape),
                          )
                          Text(
                            text = option.label,
                            fontSize = 13.sp,
                            fontWeight = if (picked) FontWeight.Bold else FontWeight.SemiBold,
                            color = headerTitleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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

          DesktopLiveChatPreviewCard(
            theme = activeTheme,
            bubbleColor = activeBubble,
            isDark = isDark,
            previewSenderName = previewSenderName,
          )

          Text(
            text = "Bubbles automatically adapt across light and dark mode.",
            fontSize = 12.5.sp,
            color = mutedColor,
            modifier = Modifier.padding(horizontal = 4.dp),
          )

          Button(
            onClick = { onSelectBubbleColor(draftColorId) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD91E2A)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp),
          ) {
            Text("Apply Bubble Colour", color = Color.White, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun DesktopLiveChatPreviewCard(
  theme: DesktopChatThemeSpec,
  bubbleColor: DesktopChatBubbleColorOption,
  isDark: Boolean,
  modifier: Modifier = Modifier,
  previewSenderName: String = "Telefam",
) {
  val resolvedSenderName = previewSenderName.trim().ifBlank { "Telefam" }
  val incomingBubbleBg = if (isDark) Color(0xFF21242B) else Color.White
  val quoteBg = if (isDark) Color(0xFF2D333E) else Color(0xFFF1F5F9)
  val quoteAccent = Color(if (isDark) theme.accentEndHex else theme.accentStartHex)
  val incomingText = if (isDark) Color(0xFFF3F4F6) else Color(0xFF14151A)
  val timeText = if (isDark) Color(0xFF9EA4B0) else Color(0xFF6B7280)

  val outgoingGradient = if (theme.themeId == "none") {
    val s = Color(if (isDark) bubbleColor.darkStartHex else bubbleColor.lightStartHex)
    val e = Color(if (isDark) bubbleColor.darkEndHex else bubbleColor.lightEndHex)
    listOf(s, e)
  } else {
    listOf(Color(theme.accentStartHex), Color(theme.accentEndHex))
  }

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.dp, if (isDark) Color(0xFF282B33) else Color(0xFFE2E8F0)),
  ) {
    Box(modifier = Modifier.fillMaxWidth().height(210.dp)) {
      DesktopChatWallpaperCanvas(theme = theme, isDark = isDark, modifier = Modifier.fillMaxSize())

      Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
      ) {
        Surface(
          shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp),
          color = incomingBubbleBg.copy(alpha = 0.96f),
          modifier = Modifier.widthIn(max = 360.dp),
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(quoteBg)
                .padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(modifier = Modifier.width(3.dp).height(30.dp).background(quoteAccent, RoundedCornerShape(2.dp)))
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(resolvedSenderName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = quoteAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Good morning!", fontSize = 12.5.sp, color = incomingText)
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Bottom,
            ) {
              Text("Do you know what time it is?", fontSize = 14.sp, color = incomingText)
              Spacer(modifier = Modifier.width(10.dp))
              Text("14:05", fontSize = 11.sp, color = timeText)
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
          modifier = Modifier
            .align(Alignment.End)
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp))
            .background(Brush.horizontalGradient(outgoingGradient))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        ) {
          Row(verticalAlignment = Alignment.Bottom) {
            Text("It's morning in Tokyo", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("14:20", fontSize = 11.sp, color = Color.White.copy(alpha = 0.88f))
            Spacer(modifier = Modifier.width(4.dp))
            DesktopChatPrivacyVectorIcon(DesktopChatPrivacySvg.DOUBLE_CHECK, Color.White, 15.dp)
          }
        }
      }
    }
  }
}

@Composable
fun DesktopChatWallpaperCanvas(
  theme: DesktopChatThemeSpec,
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

    if (theme.patternKind == DesktopChatPatternKind.CLEAN) {
      val dotStep = 38.dp.toPx()
      val softDot = if (isDark) Color.White.copy(alpha = 0.045f) else Color.Black.copy(alpha = 0.035f)
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

    val cellSize = 68.dp.toPx()
    val cols = (size.width / cellSize).toInt() + 2
    val rows = (size.height / cellSize).toInt() + 2
    val rotationAngles = floatArrayOf(-18f, 12f, -9f, 22f, 6f, -24f)
    val fineStroke = Stroke(width = 1.05.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

    for (r in 0 until rows) {
      for (c in 0 until cols) {
        val offsetX = if (r % 2 == 1) cellSize * 0.48f else 0f
        val jitterY = if ((r + c) % 2 == 0) -cellSize * 0.06f else cellSize * 0.06f
        val cx = c * cellSize + offsetX + cellSize * 0.26f
        val cy = r * cellSize + cellSize * 0.34f + jitterY
        val angle = rotationAngles[(r * 5 + c) % rotationAngles.size]
        val cInk = if ((r + c) % 2 == 1) secondaryDoodleTint else primaryDoodleTint
        val radius = cellSize * if ((r + c) % 3 == 0) 0.27f else 0.22f

        rotate(degrees = angle, pivot = Offset(cx, cy)) {
          drawDesktopWallpaperDoodle(theme.patternKind, cx, cy, radius, cInk)
        }

        if ((r + c) % 2 == 0) {
          val sx = cx + cellSize * 0.46f
          val sy = cy + cellSize * 0.40f
          if ((r * 3 + c) % 4 == 0) {
            val sr = cellSize * 0.065f
            val star = Path().apply {
              moveTo(sx, sy - sr)
              lineTo(sx + sr * 0.26f, sy - sr * 0.26f)
              lineTo(sx + sr, sy)
              lineTo(sx + sr * 0.26f, sy + sr * 0.26f)
              lineTo(sx, sy + sr)
              lineTo(sx - sr * 0.26f, sy + sr * 0.26f)
              lineTo(sx - sr, sy)
              lineTo(sx - sr * 0.26f, sy - sr * 0.26f)
              close()
            }
            drawPath(star, secondaryDoodleTint, style = fineStroke)
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

private fun DrawScope.drawDesktopWallpaperDoodle(
  kind: DesktopChatPatternKind,
  cx: Float,
  cy: Float,
  r: Float,
  color: Color,
) {
  val stroke = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
  when (kind) {
    DesktopChatPatternKind.CLEAN -> Unit
    DesktopChatPatternKind.HOME -> {
      val house = Path().apply {
        moveTo(cx, cy - r * 0.9f)
        lineTo(cx + r * 0.85f, cy - r * 0.1f)
        lineTo(cx + r * 0.6f, cy - r * 0.1f)
        lineTo(cx + r * 0.6f, cy + r * 0.8f)
        lineTo(cx - r * 0.6f, cy + r * 0.8f)
        lineTo(cx - r * 0.6f, cy - r * 0.1f)
        lineTo(cx - r * 0.85f, cy - r * 0.1f)
        close()
      }
      drawPath(house, color, style = stroke)
    }
    DesktopChatPatternKind.MEADOW -> {
      for (i in 0 until 5) {
        val ang = i * (2 * PI / 5)
        drawCircle(color, r * 0.34f, Offset(cx + cos(ang).toFloat() * r * 0.52f, cy + sin(ang).toFloat() * r * 0.52f), style = stroke)
      }
      drawCircle(color, r * 0.22f, Offset(cx, cy), style = stroke)
    }
    DesktopChatPatternKind.WINTER -> {
      for (i in 0 until 3) {
        val ang = i * (PI / 3)
        val dx = cos(ang).toFloat() * r * 0.9f
        val dy = sin(ang).toFloat() * r * 0.9f
        drawLine(color, Offset(cx - dx, cy - dy), Offset(cx + dx, cy + dy), stroke.width, StrokeCap.Round)
      }
    }
    DesktopChatPatternKind.OCEAN -> {
      val wave = Path().apply {
        moveTo(cx - r, cy)
        cubicTo(cx - r * 0.5f, cy - r * 0.55f, cx, cy + r * 0.55f, cx + r, cy)
      }
      drawPath(wave, color, style = stroke)
      drawCircle(color, r * 0.25f, Offset(cx, cy - r * 0.65f), style = stroke)
    }
    DesktopChatPatternKind.MATH -> {
      drawLine(color, Offset(cx - r * 0.6f, cy - r * 0.4f), Offset(cx + r * 0.7f, cy - r * 0.4f), stroke.width, StrokeCap.Round)
      drawLine(color, Offset(cx - r * 0.25f, cy - r * 0.4f), Offset(cx - r * 0.35f, cy + r * 0.55f), stroke.width, StrokeCap.Round)
      drawLine(color, Offset(cx + r * 0.3f, cy - r * 0.4f), Offset(cx + r * 0.35f, cy + r * 0.55f), stroke.width, StrokeCap.Round)
    }
    DesktopChatPatternKind.BOTANICAL -> {
      val leaf = Path().apply {
        moveTo(cx, cy + r * 0.85f)
        cubicTo(cx - r * 0.9f, cy + r * 0.1f, cx - r * 0.7f, cy - r * 0.75f, cx, cy - r * 0.85f)
        cubicTo(cx + r * 0.7f, cy - r * 0.75f, cx + r * 0.9f, cy + r * 0.1f, cx, cy + r * 0.85f)
      }
      drawPath(leaf, color, style = stroke)
    }
    DesktopChatPatternKind.ROMANCE -> {
      val h = Path().apply {
        moveTo(cx, cy + r * 0.75f)
        cubicTo(cx - r, cy + r * 0.05f, cx - r * 0.9f, cy - r * 0.8f, cx, cy - r * 0.25f)
        cubicTo(cx + r * 0.9f, cy - r * 0.8f, cx + r, cy + r * 0.05f, cx, cy + r * 0.75f)
      }
      drawPath(h, color, style = stroke)
    }
    DesktopChatPatternKind.FIRESIDE -> {
      val flame = Path().apply {
        moveTo(cx, cy - r * 0.85f)
        cubicTo(cx + r * 0.75f, cy - r * 0.1f, cx + r * 0.75f, cy + r * 0.8f, cx, cy + r * 0.8f)
        cubicTo(cx - r * 0.75f, cy + r * 0.8f, cx - r * 0.75f, cy - r * 0.1f, cx, cy - r * 0.85f)
      }
      drawPath(flame, color, style = stroke)
    }
    DesktopChatPatternKind.ARCADE -> {
      drawRoundRect(color, Offset(cx - r * 0.85f, cy - r * 0.45f), Size(r * 1.7f, r * 0.95f), CornerRadius(r * 0.3f), style = stroke)
      drawLine(color, Offset(cx - r * 0.5f, cy), Offset(cx - r * 0.2f, cy), stroke.width)
      drawCircle(color, r * 0.14f, Offset(cx + r * 0.42f, cy))
    }
    DesktopChatPatternKind.COSMIC -> {
      drawCircle(color, r * 0.42f, Offset(cx, cy), style = stroke)
      drawOval(color, Offset(cx - r * 0.95f, cy - r * 0.25f), Size(r * 1.9f, r * 0.5f), style = stroke)
    }
    DesktopChatPatternKind.SAFARI -> {
      drawCircle(color, r * 0.46f, Offset(cx, cy - r * 0.1f), style = stroke)
      drawLine(color, Offset(cx - r * 0.9f, cy + r * 0.4f), Offset(cx + r * 0.9f, cy + r * 0.4f), stroke.width)
    }
    DesktopChatPatternKind.TELEFAM -> {
      drawRoundRect(color, Offset(cx - r * 0.65f, cy - r * 0.65f), Size(r * 1.3f, r * 1.1f), CornerRadius(r * 0.3f), style = stroke)
      drawLine(color, Offset(cx - r * 0.32f, cy - r * 0.3f), Offset(cx + r * 0.32f, cy - r * 0.3f), stroke.width, StrokeCap.Round)
      drawLine(color, Offset(cx, cy - r * 0.3f), Offset(cx, cy + r * 0.25f), stroke.width, StrokeCap.Round)
    }
  }
}

private fun themeMotifSvg(kind: DesktopChatPatternKind): DesktopChatPrivacySvg = when (kind) {
  DesktopChatPatternKind.CLEAN -> DesktopChatPrivacySvg.PALETTE
  DesktopChatPatternKind.HOME -> DesktopChatPrivacySvg.HOME_MOTIF
  DesktopChatPatternKind.MEADOW -> DesktopChatPrivacySvg.MEADOW_MOTIF
  DesktopChatPatternKind.WINTER -> DesktopChatPrivacySvg.WINTER_MOTIF
  DesktopChatPatternKind.OCEAN -> DesktopChatPrivacySvg.OCEAN_MOTIF
  DesktopChatPatternKind.MATH -> DesktopChatPrivacySvg.MATH_MOTIF
  DesktopChatPatternKind.BOTANICAL -> DesktopChatPrivacySvg.BOTANICAL_MOTIF
  DesktopChatPatternKind.ROMANCE -> DesktopChatPrivacySvg.ROMANCE_MOTIF
  DesktopChatPatternKind.FIRESIDE -> DesktopChatPrivacySvg.FIRESIDE_MOTIF
  DesktopChatPatternKind.ARCADE -> DesktopChatPrivacySvg.ARCADE_MOTIF
  DesktopChatPatternKind.COSMIC -> DesktopChatPrivacySvg.COSMIC_MOTIF
  DesktopChatPatternKind.SAFARI -> DesktopChatPrivacySvg.SAFARI_MOTIF
  DesktopChatPatternKind.TELEFAM -> DesktopChatPrivacySvg.TELEFAM_MOTIF
}

@Composable
private fun DesktopChatPrivacyVectorIcon(
  kind: DesktopChatPrivacySvg,
  tint: Color,
  size: Dp,
) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = Stroke(width = w * 0.095f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (kind) {
      DesktopChatPrivacySvg.BACK_ARROW -> {
        val p = Path().apply {
          moveTo(w * 0.56f, h * 0.22f)
          lineTo(w * 0.24f, h * 0.50f)
          lineTo(w * 0.56f, h * 0.78f)
        }
        drawPath(p, tint, style = stroke)
        drawLine(tint, Offset(w * 0.28f, h * 0.50f), Offset(w * 0.82f, h * 0.50f), stroke.width, StrokeCap.Round)
      }
      DesktopChatPrivacySvg.CHEVRON_RIGHT -> {
        val p = Path().apply {
          moveTo(w * 0.36f, h * 0.24f)
          lineTo(w * 0.64f, h * 0.50f)
          lineTo(w * 0.36f, h * 0.76f)
        }
        drawPath(p, tint, style = stroke)
      }
      DesktopChatPrivacySvg.MESSAGE_BUBBLE -> {
        drawRoundRect(tint, Offset(w * 0.14f, h * 0.18f), Size(w * 0.72f, h * 0.54f), CornerRadius(w * 0.14f), style = stroke)
        drawLine(tint, Offset(w * 0.30f, h * 0.40f), Offset(w * 0.70f, h * 0.40f), stroke.width, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.30f, h * 0.54f), Offset(w * 0.56f, h * 0.54f), stroke.width, StrokeCap.Round)
      }
      DesktopChatPrivacySvg.CALL_PHONE -> {
        val p = Path().apply {
          moveTo(w * 0.26f, h * 0.22f)
          cubicTo(w * 0.24f, h * 0.54f, w * 0.46f, h * 0.76f, w * 0.78f, h * 0.74f)
        }
        drawPath(p, tint, style = stroke)
        drawCircle(tint, w * 0.11f, Offset(w * 0.30f, h * 0.28f))
        drawCircle(tint, w * 0.11f, Offset(w * 0.72f, h * 0.70f))
      }
      DesktopChatPrivacySvg.SCREENSHOT_PHONE -> {
        drawRoundRect(tint, Offset(w * 0.26f, h * 0.12f), Size(w * 0.48f, h * 0.76f), CornerRadius(w * 0.10f), style = stroke)
        drawLine(tint, Offset(w * 0.42f, h * 0.76f), Offset(w * 0.58f, h * 0.76f), stroke.width, StrokeCap.Round)
      }
      DesktopChatPrivacySvg.SHARE_NODES -> {
        drawCircle(tint, w * 0.11f, Offset(w * 0.26f, h * 0.50f), style = stroke)
        drawCircle(tint, w * 0.11f, Offset(w * 0.74f, h * 0.24f), style = stroke)
        drawCircle(tint, w * 0.11f, Offset(w * 0.74f, h * 0.76f), style = stroke)
        drawLine(tint, Offset(w * 0.35f, h * 0.45f), Offset(w * 0.64f, h * 0.29f), stroke.width)
        drawLine(tint, Offset(w * 0.35f, h * 0.55f), Offset(w * 0.64f, h * 0.71f), stroke.width)
      }
      DesktopChatPrivacySvg.COPY_DOCS -> {
        drawRoundRect(tint, Offset(w * 0.34f, h * 0.28f), Size(w * 0.48f, h * 0.56f), CornerRadius(w * 0.08f), style = stroke)
        val back = Path().apply {
          moveTo(w * 0.64f, h * 0.18f)
          lineTo(w * 0.20f, h * 0.18f)
          lineTo(w * 0.20f, h * 0.70f)
        }
        drawPath(back, tint, style = stroke)
      }
      DesktopChatPrivacySvg.DOWNLOAD_ARROW -> {
        drawLine(tint, Offset(w * 0.50f, h * 0.18f), Offset(w * 0.50f, h * 0.66f), stroke.width, StrokeCap.Round)
        val arrow = Path().apply {
          moveTo(w * 0.30f, h * 0.48f)
          lineTo(w * 0.50f, h * 0.68f)
          lineTo(w * 0.70f, h * 0.48f)
        }
        drawPath(arrow, tint, style = stroke)
        drawLine(tint, Offset(w * 0.20f, h * 0.82f), Offset(w * 0.80f, h * 0.82f), stroke.width, StrokeCap.Round)
      }
      DesktopChatPrivacySvg.PALETTE -> {
        drawCircle(tint, w * 0.36f, Offset(w * 0.50f, h * 0.50f), style = stroke)
        drawCircle(tint, w * 0.07f, Offset(w * 0.36f, h * 0.42f))
        drawCircle(tint, w * 0.07f, Offset(w * 0.52f, h * 0.32f))
        drawCircle(tint, w * 0.07f, Offset(w * 0.66f, h * 0.46f))
      }
      DesktopChatPrivacySvg.SLASH_PALETTE -> {
        drawCircle(tint, w * 0.40f, Offset(w * 0.50f, h * 0.50f), style = Stroke(width = w * 0.09f))
        drawLine(tint, Offset(w * 0.22f, h * 0.22f), Offset(w * 0.78f, h * 0.78f), w * 0.10f, StrokeCap.Round)
      }
      DesktopChatPrivacySvg.LETTER_T -> {
        drawLine(tint, Offset(w * 0.22f, h * 0.24f), Offset(w * 0.78f, h * 0.24f), stroke.width * 1.2f, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.50f, h * 0.24f), Offset(w * 0.50f, h * 0.80f), stroke.width * 1.2f, StrokeCap.Round)
      }
      DesktopChatPrivacySvg.DOUBLE_CHECK -> {
        val c1 = Path().apply {
          moveTo(w * 0.10f, h * 0.54f)
          lineTo(w * 0.30f, h * 0.72f)
          lineTo(w * 0.64f, h * 0.30f)
        }
        val c2 = Path().apply {
          moveTo(w * 0.38f, h * 0.58f)
          lineTo(w * 0.54f, h * 0.72f)
          lineTo(w * 0.90f, h * 0.28f)
        }
        drawPath(c1, tint, style = stroke)
        drawPath(c2, tint, style = stroke)
      }
      DesktopChatPrivacySvg.TYPING_DOTS -> {
        listOf(0.26f, 0.50f, 0.74f).forEach { rx ->
          drawCircle(tint, w * 0.09f, Offset(w * rx, h * 0.50f))
        }
      }
      DesktopChatPrivacySvg.ONLINE_EYE -> {
        val eye = Path().apply {
          moveTo(w * 0.10f, h * 0.50f)
          cubicTo(w * 0.32f, h * 0.22f, w * 0.68f, h * 0.22f, w * 0.90f, h * 0.50f)
          cubicTo(w * 0.68f, h * 0.78f, w * 0.32f, h * 0.78f, w * 0.10f, h * 0.50f)
          close()
        }
        drawPath(eye, tint, style = stroke)
        drawCircle(tint, w * 0.13f, Offset(w * 0.50f, h * 0.50f))
      }
      else -> {
        drawDesktopWallpaperDoodle(
          kind = DesktopChatPatternKind.entries.firstOrNull { themeMotifSvg(it) == kind } ?: DesktopChatPatternKind.TELEFAM,
          cx = w * 0.5f,
          cy = h * 0.5f,
          r = w * 0.38f,
          color = tint,
        )
      }
    }
  }
}
