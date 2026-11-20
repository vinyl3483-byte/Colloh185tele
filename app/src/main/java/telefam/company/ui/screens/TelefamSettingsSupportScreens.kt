package telefam.company.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.telefam.company.app.BuildConfig
import com.telefam.company.app.R
import telefam.company.core.AppVersionCheckResult
import telefam.company.core.FaqEntry
import telefam.company.core.LegalDocumentItem
import telefam.company.core.NetworkTier
import telefam.company.core.OfflineSyncStatusSnapshot
import telefam.company.core.RestrictedAccountItem
import telefam.company.core.SupportTicketItem
import telefam.company.core.SyncPreferencesState
import telefam.company.core.SystemServiceStatusItem
import telefam.company.core.TelefamSettingsContentCatalog
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamCrimsonBright
import telefam.company.ui.theme.TelefamPureWhite
import telefam.company.ui.theme.TelefamStatusOnline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SettingsSvgIconKind {
  ACTIVITY_PULSE,
  SHARE_CARD,
  BELL,
  HEART,
  CLOUD_SYNC,
  LOCK,
  BLOCK_MINUS,
  FLAG,
  SHIELD_CHECK,
  HELP_QUESTION,
  DOCUMENT_LINES,
  INFO_CIRCLE,
  BOOKMARK_LINK,
}

@Composable
fun SettingsRedCircleBadge(
  kind: SettingsSvgIconKind,
  modifier: Modifier = Modifier,
  size: Dp = 40.dp,
) {
  Box(
    modifier = modifier
      .size(size)
      .clip(CircleShape)
      .background(TelefamCrimson),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(modifier = Modifier.size(size * 0.5f)) {
      val w = this.size.width
      val h = this.size.height
      val stroke = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
      val white = Color.White

      when (kind) {
        SettingsSvgIconKind.ACTIVITY_PULSE -> {
          val p = Path().apply {
            moveTo(w * 0.06f, h * 0.54f)
            lineTo(w * 0.28f, h * 0.54f)
            lineTo(w * 0.42f, h * 0.2f)
            lineTo(w * 0.58f, h * 0.8f)
            lineTo(w * 0.72f, h * 0.5f)
            lineTo(w * 0.94f, h * 0.5f)
          }
          drawPath(p, color = white, style = stroke)
        }
        SettingsSvgIconKind.SHARE_CARD -> {
          drawRoundRect(
            color = white,
            topLeft = Offset(w * 0.1f, h * 0.2f),
            size = Size(w * 0.8f, h * 0.6f),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
            style = stroke,
          )
          drawLine(white, Offset(w * 0.12f, h * 0.44f), Offset(w * 0.88f, h * 0.44f), strokeWidth = w * 0.11f)
        }
        SettingsSvgIconKind.BELL -> {
          val p = Path().apply {
            moveTo(w * 0.5f, h * 0.14f)
            cubicTo(w * 0.27f, h * 0.14f, w * 0.23f, h * 0.42f, w * 0.2f, h * 0.66f)
            lineTo(w * 0.12f, h * 0.74f)
            lineTo(w * 0.88f, h * 0.74f)
            lineTo(w * 0.8f, h * 0.66f)
            cubicTo(w * 0.77f, h * 0.42f, w * 0.73f, h * 0.14f, w * 0.5f, h * 0.14f)
          }
          drawPath(p, color = white, style = stroke)
          drawArc(
            color = white,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.38f, h * 0.74f),
            size = Size(w * 0.24f, h * 0.14f),
            style = stroke,
          )
        }
        SettingsSvgIconKind.HEART -> {
          val p = Path().apply {
            moveTo(w * 0.5f, h * 0.84f)
            cubicTo(w * 0.12f, h * 0.6f, w * 0.1f, h * 0.24f, w * 0.32f, h * 0.22f)
            cubicTo(w * 0.43f, h * 0.21f, w * 0.49f, h * 0.32f, w * 0.5f, h * 0.36f)
            cubicTo(w * 0.51f, h * 0.32f, w * 0.57f, h * 0.21f, w * 0.68f, h * 0.22f)
            cubicTo(w * 0.9f, h * 0.24f, w * 0.88f, h * 0.6f, w * 0.5f, h * 0.84f)
            close()
          }
          drawPath(p, color = white, style = stroke)
        }
        SettingsSvgIconKind.CLOUD_SYNC -> {
          drawArc(
            color = white,
            startAngle = 210f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(w * 0.16f, h * 0.16f),
            size = Size(w * 0.68f, h * 0.68f),
            style = stroke,
          )
          drawArc(
            color = white,
            startAngle = 30f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(w * 0.16f, h * 0.16f),
            size = Size(w * 0.68f, h * 0.68f),
            style = stroke,
          )
          val arrowTop = Path().apply {
            moveTo(w * 0.66f, h * 0.14f)
            lineTo(w * 0.82f, h * 0.26f)
            lineTo(w * 0.64f, h * 0.34f)
          }
          drawPath(arrowTop, color = white, style = stroke)
          val arrowBot = Path().apply {
            moveTo(w * 0.34f, h * 0.86f)
            lineTo(w * 0.18f, h * 0.74f)
            lineTo(w * 0.36f, h * 0.66f)
          }
          drawPath(arrowBot, color = white, style = stroke)
        }
        SettingsSvgIconKind.LOCK -> {
          drawRoundRect(
            color = white,
            topLeft = Offset(w * 0.2f, h * 0.44f),
            size = Size(w * 0.6f, h * 0.42f),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
            style = stroke,
          )
          drawArc(
            color = white,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.32f, h * 0.16f),
            size = Size(w * 0.36f, h * 0.44f),
            style = stroke,
          )
        }
        SettingsSvgIconKind.BLOCK_MINUS -> {
          drawCircle(white, radius = w * 0.38f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
          drawLine(white, Offset(w * 0.32f, h * 0.5f), Offset(w * 0.68f, h * 0.5f), strokeWidth = w * 0.11f, cap = StrokeCap.Round)
        }
        SettingsSvgIconKind.FLAG -> {
          drawLine(white, Offset(w * 0.24f, h * 0.16f), Offset(w * 0.24f, h * 0.88f), strokeWidth = w * 0.11f, cap = StrokeCap.Round)
          val p = Path().apply {
            moveTo(w * 0.24f, h * 0.2f)
            lineTo(w * 0.78f, h * 0.2f)
            lineTo(w * 0.66f, h * 0.38f)
            lineTo(w * 0.78f, h * 0.54f)
            lineTo(w * 0.24f, h * 0.54f)
          }
          drawPath(p, color = white, style = stroke)
        }
        SettingsSvgIconKind.SHIELD_CHECK -> {
          val p = Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.84f, h * 0.25f)
            lineTo(w * 0.84f, h * 0.54f)
            cubicTo(w * 0.84f, h * 0.74f, w * 0.64f, h * 0.85f, w * 0.5f, h * 0.9f)
            cubicTo(w * 0.36f, h * 0.85f, w * 0.16f, h * 0.74f, w * 0.16f, h * 0.54f)
            lineTo(w * 0.16f, h * 0.25f)
            close()
          }
          drawPath(p, color = white, style = stroke)
          val check = Path().apply {
            moveTo(w * 0.34f, h * 0.52f)
            lineTo(w * 0.45f, h * 0.64f)
            lineTo(w * 0.68f, h * 0.4f)
          }
          drawPath(check, color = white, style = stroke)
        }
        SettingsSvgIconKind.HELP_QUESTION -> {
          drawCircle(white, radius = w * 0.38f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
          val q = Path().apply {
            moveTo(w * 0.39f, h * 0.38f)
            cubicTo(w * 0.39f, h * 0.28f, w * 0.62f, h * 0.28f, w * 0.6f, h * 0.43f)
            cubicTo(w * 0.59f, h * 0.5f, w * 0.5f, h * 0.52f, w * 0.5f, h * 0.6f)
          }
          drawPath(q, color = white, style = stroke)
          drawCircle(white, radius = w * 0.055f, center = Offset(w * 0.5f, h * 0.72f))
        }
        SettingsSvgIconKind.DOCUMENT_LINES -> {
          drawRoundRect(
            color = white,
            topLeft = Offset(w * 0.2f, h * 0.14f),
            size = Size(w * 0.6f, h * 0.72f),
            cornerRadius = CornerRadius(w * 0.1f, w * 0.1f),
            style = stroke,
          )
          drawLine(white, Offset(w * 0.34f, h * 0.38f), Offset(w * 0.66f, h * 0.38f), strokeWidth = w * 0.095f, cap = StrokeCap.Round)
          drawLine(white, Offset(w * 0.34f, h * 0.53f), Offset(w * 0.66f, h * 0.53f), strokeWidth = w * 0.095f, cap = StrokeCap.Round)
          drawLine(white, Offset(w * 0.34f, h * 0.68f), Offset(w * 0.54f, h * 0.68f), strokeWidth = w * 0.095f, cap = StrokeCap.Round)
        }
        SettingsSvgIconKind.INFO_CIRCLE -> {
          drawCircle(white, radius = w * 0.38f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
          drawCircle(white, radius = w * 0.055f, center = Offset(w * 0.5f, h * 0.33f))
          drawLine(white, Offset(w * 0.5f, h * 0.48f), Offset(w * 0.5f, h * 0.72f), strokeWidth = w * 0.11f, cap = StrokeCap.Round)
        }
        SettingsSvgIconKind.BOOKMARK_LINK -> {
          val p = Path().apply {
            moveTo(w * 0.26f, h * 0.16f)
            lineTo(w * 0.74f, h * 0.16f)
            lineTo(w * 0.74f, h * 0.86f)
            lineTo(w * 0.50f, h * 0.66f)
            lineTo(w * 0.26f, h * 0.86f)
            close()
          }
          drawPath(p, color = white, style = stroke)
        }
      }
    }
  }
}

@Composable
fun SettingsChevronRight(color: Color = TelefamCrimson, modifier: Modifier = Modifier) {
  Canvas(modifier = modifier.size(16.dp)) {
    val p = Path().apply {
      moveTo(size.width * 0.34f, size.height * 0.2f)
      lineTo(size.width * 0.68f, size.height * 0.5f)
      lineTo(size.width * 0.34f, size.height * 0.8f)
    }
    drawPath(p, color = color, style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
  }
}

@Composable
fun SettingsLogoutDoorSvg(color: Color = Color.White, modifier: Modifier = Modifier) {
  Canvas(modifier = modifier.size(20.dp)) {
    val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val frame = Path().apply {
      moveTo(size.width * 0.52f, size.height * 0.18f)
      lineTo(size.width * 0.22f, size.height * 0.18f)
      lineTo(size.width * 0.22f, size.height * 0.82f)
      lineTo(size.width * 0.52f, size.height * 0.82f)
    }
    drawPath(frame, color = color, style = stroke)
    drawLine(color, Offset(size.width * 0.42f, size.height * 0.5f), Offset(size.width * 0.86f, size.height * 0.5f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
    val arrow = Path().apply {
      moveTo(size.width * 0.7f, size.height * 0.34f)
      lineTo(size.width * 0.86f, size.height * 0.5f)
      lineTo(size.width * 0.7f, size.height * 0.66f)
    }
    drawPath(arrow, color = color, style = stroke)
  }
}

@Composable
internal fun BlockedMutedAccountsPane(
  selectedTab: Int,
  query: String,
  blocked: List<RestrictedAccountItem>,
  muted: List<RestrictedAccountItem>,
  isLoading: Boolean,
  onSelectTab: (Int) -> Unit,
  onQueryChanged: (String) -> Unit,
  onUnblock: (String) -> Unit,
  onUnmute: (String) -> Unit,
) {
  val list = if (selectedTab == 0) blocked else muted
  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      FilterChip(
        selected = selectedTab == 0,
        onClick = { onSelectTab(0) },
        label = { Text("Blocked (${blocked.size})", fontWeight = FontWeight.SemiBold) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = TelefamCrimson,
          selectedLabelColor = TelefamPureWhite,
        ),
        modifier = Modifier.weight(1f).testTag("blocked_tab_chip"),
      )
      FilterChip(
        selected = selectedTab == 1,
        onClick = { onSelectTab(1) },
        label = { Text("Muted accounts (${muted.size})", fontWeight = FontWeight.SemiBold) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = TelefamCrimson,
          selectedLabelColor = TelefamPureWhite,
        ),
        modifier = Modifier.weight(1f).testTag("muted_tab_chip"),
      )
    }

    OutlinedTextField(
      value = query,
      onValueChange = onQueryChanged,
      singleLine = true,
      leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
      trailingIcon = {
        if (query.isNotEmpty()) {
          IconButton(onClick = { onQueryChanged("") }) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_clear_search))
          }
        }
      },
      placeholder = { Text(if (selectedTab == 0) "Search blocked accounts" else "Search muted accounts") },
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .testTag("restricted_accounts_search"),
    )

    Spacer(modifier = Modifier.height(8.dp))

    if (isLoading && list.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = TelefamCrimson)
      }
    } else if (list.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
          text = if (selectedTab == 0) "No blocked accounts" else "No muted accounts",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(list, key = { "${it.isMuted}_${it.userId}" }) { acct ->
          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              SettingsAvatarBadge(
                avatarUrl = acct.avatarLocalPath ?: acct.avatarUrl,
                displayName = acct.fullName.ifBlank { acct.username },
                size = 42.dp,
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = acct.fullName.ifBlank { acct.username },
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "@${acct.username}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                )
              }
              OutlinedButton(
                onClick = { if (selectedTab == 0) onUnblock(acct.userId) else onUnmute(acct.userId) },
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.testTag("restricted_action_${acct.userId}"),
              ) {
                Text(
                  text = if (selectedTab == 0) "Unblock" else "Unmute",
                  color = TelefamCrimson,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 12.sp,
                )
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
internal fun ReportProblemPane(
  isSubmitting: Boolean,
  createdTicket: SupportTicketItem?,
  onSubmitReport: (category: String, description: String, screenshotUri: Uri?) -> Unit,
  onClearCreated: () -> Unit,
  onOpenTicketHistory: () -> Unit,
) {
  var selectedCategory by rememberSaveable { mutableStateOf(TelefamSettingsContentCatalog.reportCategories.first()) }
  var description by rememberSaveable { mutableStateOf("") }
  var screenshotUriStr by rememberSaveable { mutableStateOf<String?>(null) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) screenshotUriStr = uri.toString()
  }

  if (createdTicket != null) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = Icons.Filled.CheckCircle,
        contentDescription = null,
        tint = TelefamStatusOnline,
        modifier = Modifier.size(64.dp),
      )
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "Report received",
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
      )
      Spacer(modifier = Modifier.height(8.dp))
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = "Reference number",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = createdTicket.referenceNumber,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TelefamCrimson),
            modifier = Modifier.testTag("report_reference_number"),
          )
          if (!createdTicket.isSynced) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Queued offline for automatic sync",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(20.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
          onClick = {
            description = ""
            screenshotUriStr = null
            onClearCreated()
          },
          shape = RoundedCornerShape(20.dp),
        ) {
          Text("New report")
        }
        Button(
          onClick = {
            description = ""
            screenshotUriStr = null
            onClearCreated()
            onOpenTicketHistory()
          },
          colors = ButtonDefaults.buttonColors(containerColor = TelefamCrimson, contentColor = TelefamPureWhite),
          shape = RoundedCornerShape(20.dp),
        ) {
          Text("View tickets")
        }
      }
    }
    return
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Text(
        text = "Category",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
      )
      Spacer(modifier = Modifier.height(8.dp))
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        TelefamSettingsContentCatalog.reportCategories.forEach { cat ->
          FilterChip(
            selected = selectedCategory == cat,
            onClick = { selectedCategory = cat },
            label = { Text(cat, fontSize = 12.5.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = TelefamCrimson,
              selectedLabelColor = TelefamPureWhite,
            ),
          )
        }
      }
    }

    item {
      OutlinedTextField(
        value = description,
        onValueChange = { description = it.take(1200) },
        label = { Text("Describe the issue") },
        minLines = 4,
        maxLines = 8,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("report_description_input"),
      )
    }

    item {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "Screenshot attachment",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
            OutlinedButton(
              onClick = {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
              },
              shape = RoundedCornerShape(16.dp),
            ) {
              Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (screenshotUriStr == null) "Attach" else "Replace")
            }
          }
          val uriString = screenshotUriStr
          if (!uriString.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
              AsyncImage(
                model = Uri.parse(uriString),
                contentDescription = "Attached screenshot",
                modifier = Modifier
                  .fillMaxSize()
                  .clip(RoundedCornerShape(12.dp)),
              )
              IconButton(
                onClick = { screenshotUriStr = null },
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(6.dp)
                  .size(30.dp)
                  .clip(CircleShape)
                  .background(Color.Black.copy(alpha = 0.65f)),
              ) {
                Icon(Icons.Filled.Close, contentDescription = "Remove screenshot", tint = Color.White, modifier = Modifier.size(16.dp))
              }
            }
          }
        }
      }
    }

    item {
      Button(
        onClick = {
          onSubmitReport(
            selectedCategory,
            description,
            screenshotUriStr?.let { Uri.parse(it) },
          )
        },
        enabled = description.trim().length >= 5 && !isSubmitting,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TelefamCrimson, contentColor = TelefamPureWhite),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("submit_problem_report_button"),
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TelefamPureWhite, strokeWidth = 2.dp)
        } else {
          Text("Submit report", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
internal fun SafetyCenterPane() {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    items(TelefamSettingsContentCatalog.safetySections, key = { it.id }) { section ->
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
          Text(
            text = section.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TelefamCrimson,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = section.summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(10.dp))
          section.bulletPoints.forEach { bullet ->
            Text(
              text = "• $bullet",
              style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
              modifier = Modifier.padding(vertical = 3.dp),
            )
          }
        }
      }
    }

    item {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
          Text(
            text = "Emergency contacts",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TelefamCrimson,
          )
          Spacer(modifier = Modifier.height(8.dp))
          TelefamSettingsContentCatalog.emergencyContacts.forEachIndexed { idx, ec ->
            if (idx > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Top,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("${ec.region} · ${ec.serviceName}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                Text(ec.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Spacer(modifier = Modifier.width(8.dp))
              Text(ec.contact, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = TelefamCrimson))
            }
          }
        }
      }
    }
  }
}

@Composable
internal fun HelpSupportPane(
  faqQuery: String,
  faqs: List<FaqEntry>,
  tickets: List<SupportTicketItem>,
  systemStatuses: List<SystemServiceStatusItem>,
  isLoadingTickets: Boolean,
  isLoadingStatus: Boolean,
  onSearchFaq: (String) -> Unit,
  onRefreshTickets: () -> Unit,
  onRefreshStatus: () -> Unit,
) {
  var subTab by rememberSaveable { mutableIntStateOf(0) }
  var expandedFaqId by rememberSaveable { mutableStateOf<String?>(null) }

  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      listOf("FAQs", "Tickets (${tickets.size})", "System status").forEachIndexed { idx, label ->
        FilterChip(
          selected = subTab == idx,
          onClick = { subTab = idx },
          label = { Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = TelefamCrimson,
            selectedLabelColor = TelefamPureWhite,
          ),
        )
      }
    }

    when (subTab) {
      0 -> {
        OutlinedTextField(
          value = faqQuery,
          onValueChange = onSearchFaq,
          singleLine = true,
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
          placeholder = { Text("Search FAQs") },
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(faqs, key = { it.id }) { faq ->
            val expanded = expandedFaqId == faq.id
            Surface(
              color = MaterialTheme.colorScheme.surface,
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { expandedFaqId = if (expanded) null else faq.id },
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(faq.category, style = MaterialTheme.typography.labelSmall, color = TelefamCrimson)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(faq.question, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                  }
                  Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                  )
                }
                if (expanded) {
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = faq.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }

      1 -> {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(onClick = onRefreshTickets) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Refresh")
          }
        }
        if (isLoadingTickets && tickets.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TelefamCrimson)
          }
        } else if (tickets.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No support tickets yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(tickets, key = { it.ticketId }) { t ->
              Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
              ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(t.referenceNumber, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = TelefamCrimson))
                    Text(
                      text = if (!t.isSynced) "QUEUED" else t.status.uppercase(),
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = if (t.status == "resolved") TelefamStatusOnline else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(t.category, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(t.description, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
              }
            }
          }
        }
      }

      else -> {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(onClick = onRefreshStatus) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Check status")
          }
        }
        if (isLoadingStatus && systemStatuses.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TelefamCrimson)
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(systemStatuses, key = { it.serviceName }) { svc ->
              Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Box(
                    modifier = Modifier
                      .size(10.dp)
                      .clip(CircleShape)
                      .background(if (svc.status == "operational") TelefamStatusOnline else TelefamCrimsonBright),
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(svc.serviceName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    Text(svc.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                  if (svc.latencyMs > 0) {
                    Text("${svc.latencyMs} ms", style = MaterialTheme.typography.labelMedium, color = TelefamStatusOnline)
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

@Composable
internal fun TermsPoliciesPane(
  documents: List<LegalDocumentItem>,
  selectedDocId: String,
  onSelectDoc: (String) -> Unit,
  onOpenAboutLicenses: () -> Unit,
) {
  val doc = documents.firstOrNull { it.id == selectedDocId } ?: documents.firstOrNull()
  Column(modifier = Modifier.fillMaxSize()) {
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(documents, key = { it.id }) { item ->
        FilterChip(
          selected = item.id == (doc?.id ?: ""),
          onClick = { onSelectDoc(item.id) },
          label = { Text(item.title, fontSize = 12.5.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = TelefamCrimson,
            selectedLabelColor = TelefamPureWhite,
          ),
        )
      }
    }
    if (doc != null) {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        item {
          Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
              Text(doc.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Last updated: ${doc.lastUpdated}",
                style = MaterialTheme.typography.labelMedium,
                color = TelefamCrimson,
              )
            }
          }
        }
        items(doc.sections) { paragraph ->
          Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
            Text(
              text = paragraph,
              style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
              modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
          }
        }
        if (doc.id == "licenses") {
          item {
            OutlinedButton(
              onClick = onOpenAboutLicenses,
              shape = RoundedCornerShape(20.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text("Open source licenses", color = TelefamCrimson, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
    }
  }
}

@Composable
internal fun AboutSettingsPane(
  versionCheck: AppVersionCheckResult?,
  isCheckingUpdates: Boolean,
  onCheckUpdates: () -> Unit,
) {
  var showLicenses by rememberSaveable { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp)) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text("Telefam", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = TelefamCrimson))
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Version ${versionCheck?.installedVersion ?: BuildConfig.VERSION_NAME} (Build ${versionCheck?.installedBuild ?: BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(14.dp))
          Button(
            onClick = onCheckUpdates,
            enabled = !isCheckingUpdates,
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TelefamCrimson, contentColor = TelefamPureWhite),
          ) {
            if (isCheckingUpdates) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TelefamPureWhite, strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Check for updates")
          }
          if (versionCheck != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = if (versionCheck.updateAvailable) {
                "Update available: v${versionCheck.latestVersion}"
              } else {
                "You are on the latest version (v${versionCheck.latestVersion})"
              },
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = if (versionCheck.updateAvailable) TelefamCrimson else TelefamStatusOnline,
              ),
            )
            if (versionCheck.releaseNotes.isNotBlank()) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = versionCheck.releaseNotes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }

    item {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .clickable { showLicenses = !showLicenses },
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Open source licenses", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
          Icon(
            imageVector = if (showLicenses) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
          )
        }
      }
    }

    if (showLicenses) {
      items(TelefamSettingsContentCatalog.openSourceLicenses, key = { it.library }) { lic ->
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(14.dp)) {
          Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(lic.library, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
            Text("${lic.license} · ${lic.copyright}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    }

    item {
      Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("Company & copyright", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
          Text("Telefam Technologies Ltd.", style = MaterialTheme.typography.bodyMedium)
          Text("© 2026 Telefam. All rights reserved.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  }
}

@Composable
internal fun LogoutConfirmationDialog(
  logoutAllDevices: Boolean,
  onToggleLogoutAllDevices: (Boolean) -> Unit,
  onConfirmLogout: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.settings_logout_dialog_title),
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      )
    },
    text = {
      Column {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggleLogoutAllDevices(!logoutAllDevices) }
            .padding(vertical = 6.dp)
            .testTag("logout_all_devices_toggle"),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Checkbox(
            checked = logoutAllDevices,
            onCheckedChange = onToggleLogoutAllDevices,
            colors = CheckboxDefaults.colors(checkedColor = TelefamCrimson),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.settings_logout_all_devices),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onConfirmLogout,
        colors = ButtonDefaults.buttonColors(containerColor = TelefamCrimson, contentColor = TelefamPureWhite),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.testTag("confirm_logout_button"),
      ) {
        Text(stringResource(R.string.settings_logout), fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.settings_cancel))
      }
    },
  )
}

@Composable
internal fun OfflineSyncPane(
  prefs: SyncPreferencesState,
  status: OfflineSyncStatusSnapshot,
  networkTier: NetworkTier,
  isWifiConnected: Boolean,
  onUpdatePrefs: (SyncPreferencesState) -> Unit,
  onSyncNow: () -> Unit,
  onRefreshStatus: () -> Unit,
) {
  val lastSyncLabel = remember(status.lastSyncedAtMs, prefs.lastSyncedAtMs) {
    val ts = maxOf(status.lastSyncedAtMs, prefs.lastSyncedAtMs)
    if (ts <= 0L) "Not synced yet"
    else SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(ts))
  }
  val connectionLabel = when {
    networkTier == NetworkTier.OFFLINE -> "Offline"
    isWifiConnected -> "Online (Wi-Fi)"
    else -> "Online (Cellular)"
  }
  val connectionColor = when {
    networkTier == NetworkTier.OFFLINE -> TelefamCrimsonBright
    isWifiConnected -> TelefamStatusOnline
    else -> Color(0xFFF59E0B)
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("settings_offline_sync_pane"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Server synchronization",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Last synced: $lastSyncLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Surface(
              color = connectionColor.copy(alpha = 0.14f),
              shape = RoundedCornerShape(12.dp),
            ) {
              Text(
                text = connectionLabel,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = connectionColor,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              )
            }
          }

          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(14.dp),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = "Pending queued items",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                  text = "${status.totalPendingCount}",
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (status.totalPendingCount > 0) TelefamCrimson else TelefamStatusOnline,
                  ),
                  modifier = Modifier.testTag("offline_sync_pending_count"),
                )
              }
              Text(
                text = "Settings: ${status.pendingSettingsOps} · Contacts: ${status.pendingContactOps} · Feeds: ${status.pendingFeedOps} · Uploads: ${status.pendingPostUploads} · Actions: ${status.pendingActionOps}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              if (prefs.totalSyncedOps > 0L) {
                Text(
                  text = "Total synced to server: ${prefs.totalSyncedOps}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          if (status.lastResultMessage.isNotBlank()) {
            Text(
              text = status.lastResultMessage,
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
              color = if (networkTier == NetworkTier.OFFLINE) TelefamCrimson else MaterialTheme.colorScheme.primary,
              modifier = Modifier.testTag("offline_sync_result_message"),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button(
              onClick = onSyncNow,
              enabled = !status.isSyncing,
              shape = RoundedCornerShape(22.dp),
              colors = ButtonDefaults.buttonColors(containerColor = TelefamCrimson, contentColor = TelefamPureWhite),
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("settings_offline_sync_now_button"),
            ) {
              if (status.isSyncing) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TelefamPureWhite, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Syncing...", fontWeight = FontWeight.Bold)
              } else {
                Text("Sync now", fontWeight = FontWeight.Bold)
              }
            }
            OutlinedButton(
              onClick = onRefreshStatus,
              shape = RoundedCornerShape(22.dp),
              modifier = Modifier.height(48.dp),
            ) {
              Icon(Icons.Filled.Refresh, contentDescription = "Refresh queue", modifier = Modifier.size(18.dp))
            }
          }
        }
      }
    }

    item {
      Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "Sync rules",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          )
          SyncSwitchRow(
            label = "Auto-sync when online",
            subtitle = "Automatically upload queued edits when connectivity returns",
            checked = prefs.autoSyncEnabled,
            tag = "sync_auto_sync_switch",
            onCheckedChange = { onUpdatePrefs(prefs.copy(autoSyncEnabled = it)) },
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
          SyncSwitchRow(
            label = "Sync over Wi-Fi only",
            subtitle = "Pause automatic background transfers on metered cellular data",
            checked = prefs.syncOverWifiOnly,
            tag = "sync_wifi_only_switch",
            onCheckedChange = { onUpdatePrefs(prefs.copy(syncOverWifiOnly = it)) },
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
          SyncSwitchRow(
            label = "Background sync",
            subtitle = "Keep chats, settings, and profile state reconciled periodically",
            checked = prefs.backgroundSyncEnabled,
            tag = "sync_background_switch",
            onCheckedChange = { onUpdatePrefs(prefs.copy(backgroundSyncEnabled = it)) },
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
          SyncSwitchRow(
            label = "Cache feeds offline",
            subtitle = "Store recent posts and metadata for offline viewing",
            checked = prefs.cacheFeedsOffline,
            tag = "sync_cache_feeds_switch",
            onCheckedChange = { onUpdatePrefs(prefs.copy(cacheFeedsOffline = it)) },
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
          SyncSwitchRow(
            label = "Cache chat media offline",
            subtitle = "Retain encrypted chat attachments in local storage",
            checked = prefs.cacheChatMediaOffline,
            tag = "sync_cache_media_switch",
            onCheckedChange = { onUpdatePrefs(prefs.copy(cacheChatMediaOffline = it)) },
          )
        }
      }
    }
  }
}

@Composable
private fun SyncSwitchRow(
  label: String,
  subtitle: String,
  checked: Boolean,
  tag: String,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
      Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(checkedThumbColor = TelefamPureWhite, checkedTrackColor = TelefamCrimson),
      modifier = Modifier.testTag(tag),
    )
  }
}
