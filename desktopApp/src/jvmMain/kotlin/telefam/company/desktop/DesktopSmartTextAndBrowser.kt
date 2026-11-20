package telefam.company.desktop

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import telefam.company.shared.SmartEntity
import telefam.company.shared.SmartTextDetector
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.InetAddress
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.Date
import java.util.Locale
import javax.swing.JEditorPane
import javax.swing.JScrollPane
import javax.swing.SwingUtilities
import javax.swing.event.HyperlinkEvent
import kotlin.math.sin

enum class DesktopSmartSvg {
  COPY,
  CHECK,
  PHONE,
  MAIL,
  MESSAGE,
  CONTACT_ADD,
  GLOBE,
  LOCK_CLOSED,
  LOCK_OPEN,
  BOOKMARK_FILLED,
  BOOKMARK_OUTLINE,
  OPEN_EXTERNAL,
  BACK,
  FORWARD,
  REFRESH,
  CLOSE,
  TRASH,
  CLOUD_OFF,
}

@Composable
fun DesktopSmartSvgIcon(
  kind: DesktopSmartSvg,
  tint: Color,
  size: Dp = 18.dp,
) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val sw = (w * 0.095f).coerceAtLeast(1.4f)
    val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (kind) {
      DesktopSmartSvg.COPY -> {
        drawRoundRect(tint, Offset(w * 0.32f, h * 0.14f), Size(w * 0.52f, h * 0.56f), CornerRadius(w * 0.08f, w * 0.08f), style = stroke)
        drawRoundRect(tint, Offset(w * 0.16f, h * 0.30f), Size(w * 0.52f, h * 0.56f), CornerRadius(w * 0.08f, w * 0.08f), style = stroke)
      }
      DesktopSmartSvg.CHECK -> {
        val p = Path().apply {
          moveTo(w * 0.18f, h * 0.52f)
          lineTo(w * 0.42f, h * 0.74f)
          lineTo(w * 0.84f, h * 0.28f)
        }
        drawPath(p, tint, style = stroke)
      }
      DesktopSmartSvg.PHONE -> {
        val p = Path().apply {
          moveTo(w * 0.24f, h * 0.18f)
          lineTo(w * 0.38f, h * 0.18f)
          lineTo(w * 0.44f, h * 0.34f)
          lineTo(w * 0.34f, h * 0.42f)
          cubicTo(w * 0.40f, h * 0.54f, w * 0.46f, h * 0.60f, w * 0.58f, h * 0.66f)
          lineTo(w * 0.66f, h * 0.56f)
          lineTo(w * 0.82f, h * 0.62f)
          lineTo(w * 0.82f, h * 0.78f)
          cubicTo(w * 0.60f, h * 0.86f, w * 0.16f, h * 0.42f, w * 0.24f, h * 0.18f)
          close()
        }
        drawPath(p, tint, style = stroke)
      }
      DesktopSmartSvg.MAIL -> {
        drawRoundRect(tint, Offset(w * 0.12f, h * 0.22f), Size(w * 0.76f, h * 0.56f), CornerRadius(w * 0.10f, w * 0.10f), style = stroke)
        val p = Path().apply {
          moveTo(w * 0.14f, h * 0.28f)
          lineTo(w * 0.50f, h * 0.55f)
          lineTo(w * 0.86f, h * 0.28f)
        }
        drawPath(p, tint, style = stroke)
      }
      DesktopSmartSvg.MESSAGE -> {
        val p = Path().apply {
          moveTo(w * 0.14f, h * 0.20f)
          lineTo(w * 0.86f, h * 0.20f)
          lineTo(w * 0.86f, h * 0.68f)
          lineTo(w * 0.42f, h * 0.68f)
          lineTo(w * 0.22f, h * 0.84f)
          lineTo(w * 0.24f, h * 0.68f)
          lineTo(w * 0.14f, h * 0.68f)
          close()
        }
        drawPath(p, tint, style = stroke)
      }
      DesktopSmartSvg.CONTACT_ADD -> {
        drawCircle(tint, radius = w * 0.16f, center = Offset(w * 0.42f, h * 0.34f), style = stroke)
        val body = Path().apply {
          moveTo(w * 0.16f, h * 0.78f)
          cubicTo(w * 0.16f, h * 0.58f, w * 0.68f, h * 0.58f, w * 0.68f, h * 0.78f)
        }
        drawPath(body, tint, style = stroke)
        drawLine(tint, Offset(w * 0.78f, h * 0.34f), Offset(w * 0.78f, h * 0.58f), sw, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.66f, h * 0.46f), Offset(w * 0.90f, h * 0.46f), sw, StrokeCap.Round)
      }
      DesktopSmartSvg.GLOBE -> {
        drawCircle(tint, radius = w * 0.38f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
        drawLine(tint, Offset(w * 0.14f, h * 0.50f), Offset(w * 0.86f, h * 0.50f), sw, StrokeCap.Round)
        drawOval(tint, Offset(w * 0.33f, h * 0.12f), Size(w * 0.34f, h * 0.76f), style = stroke)
      }
      DesktopSmartSvg.LOCK_CLOSED -> {
        drawRoundRect(tint, Offset(w * 0.22f, h * 0.44f), Size(w * 0.56f, h * 0.42f), CornerRadius(w * 0.10f, w * 0.10f), style = stroke)
        val arch = Path().apply {
          moveTo(w * 0.34f, h * 0.44f)
          lineTo(w * 0.34f, h * 0.30f)
          cubicTo(w * 0.34f, h * 0.14f, w * 0.66f, h * 0.14f, w * 0.66f, h * 0.30f)
          lineTo(w * 0.66f, h * 0.44f)
        }
        drawPath(arch, tint, style = stroke)
      }
      DesktopSmartSvg.LOCK_OPEN -> {
        drawRoundRect(tint, Offset(w * 0.22f, h * 0.44f), Size(w * 0.56f, h * 0.42f), CornerRadius(w * 0.10f, w * 0.10f), style = stroke)
        val arch = Path().apply {
          moveTo(w * 0.34f, h * 0.44f)
          lineTo(w * 0.34f, h * 0.30f)
          cubicTo(w * 0.34f, h * 0.14f, w * 0.66f, h * 0.14f, w * 0.66f, h * 0.28f)
        }
        drawPath(arch, tint, style = stroke)
      }
      DesktopSmartSvg.BOOKMARK_FILLED -> {
        val p = Path().apply {
          moveTo(w * 0.24f, h * 0.14f)
          lineTo(w * 0.76f, h * 0.14f)
          lineTo(w * 0.76f, h * 0.86f)
          lineTo(w * 0.50f, h * 0.66f)
          lineTo(w * 0.24f, h * 0.86f)
          close()
        }
        drawPath(p, tint)
      }
      DesktopSmartSvg.BOOKMARK_OUTLINE -> {
        val p = Path().apply {
          moveTo(w * 0.24f, h * 0.14f)
          lineTo(w * 0.76f, h * 0.14f)
          lineTo(w * 0.76f, h * 0.86f)
          lineTo(w * 0.50f, h * 0.66f)
          lineTo(w * 0.24f, h * 0.86f)
          close()
        }
        drawPath(p, tint, style = stroke)
      }
      DesktopSmartSvg.OPEN_EXTERNAL -> {
        val box = Path().apply {
          moveTo(w * 0.50f, h * 0.20f)
          lineTo(w * 0.20f, h * 0.20f)
          lineTo(w * 0.20f, h * 0.80f)
          lineTo(w * 0.80f, h * 0.80f)
          lineTo(w * 0.80f, h * 0.50f)
        }
        drawPath(box, tint, style = stroke)
        drawLine(tint, Offset(w * 0.48f, h * 0.52f), Offset(w * 0.82f, h * 0.18f), sw, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.60f, h * 0.18f), Offset(w * 0.82f, h * 0.18f), sw, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.82f, h * 0.18f), Offset(w * 0.82f, h * 0.40f), sw, StrokeCap.Round)
      }
      DesktopSmartSvg.BACK -> {
        drawLine(tint, Offset(w * 0.80f, h * 0.50f), Offset(w * 0.22f, h * 0.50f), sw, StrokeCap.Round)
        val p = Path().apply {
          moveTo(w * 0.46f, h * 0.24f)
          lineTo(w * 0.20f, h * 0.50f)
          lineTo(w * 0.46f, h * 0.76f)
        }
        drawPath(p, tint, style = stroke)
      }
      DesktopSmartSvg.FORWARD -> {
        drawLine(tint, Offset(w * 0.20f, h * 0.50f), Offset(w * 0.78f, h * 0.50f), sw, StrokeCap.Round)
        val p = Path().apply {
          moveTo(w * 0.54f, h * 0.24f)
          lineTo(w * 0.80f, h * 0.50f)
          lineTo(w * 0.54f, h * 0.76f)
        }
        drawPath(p, tint, style = stroke)
      }
      DesktopSmartSvg.REFRESH -> {
        drawArc(
          color = tint,
          startAngle = -50f,
          sweepAngle = 290f,
          useCenter = false,
          topLeft = Offset(w * 0.18f, h * 0.18f),
          size = Size(w * 0.64f, h * 0.64f),
          style = stroke,
        )
        val arrow = Path().apply {
          moveTo(w * 0.66f, h * 0.18f)
          lineTo(w * 0.82f, h * 0.22f)
          lineTo(w * 0.74f, h * 0.36f)
        }
        drawPath(arrow, tint, style = stroke)
      }
      DesktopSmartSvg.CLOSE -> {
        drawLine(tint, Offset(w * 0.22f, h * 0.22f), Offset(w * 0.78f, h * 0.78f), sw, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.78f, h * 0.22f), Offset(w * 0.22f, h * 0.78f), sw, StrokeCap.Round)
      }
      DesktopSmartSvg.TRASH -> {
        drawLine(tint, Offset(w * 0.18f, h * 0.26f), Offset(w * 0.82f, h * 0.26f), sw, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.38f, h * 0.16f), Offset(w * 0.62f, h * 0.16f), sw, StrokeCap.Round)
        drawRoundRect(tint, Offset(w * 0.26f, h * 0.26f), Size(w * 0.48f, h * 0.58f), CornerRadius(w * 0.08f, w * 0.08f), style = stroke)
        drawLine(tint, Offset(w * 0.42f, h * 0.42f), Offset(w * 0.42f, h * 0.70f), sw, StrokeCap.Round)
        drawLine(tint, Offset(w * 0.58f, h * 0.42f), Offset(w * 0.58f, h * 0.70f), sw, StrokeCap.Round)
      }
      DesktopSmartSvg.CLOUD_OFF -> {
        val cloud = Path().apply {
          moveTo(w * 0.28f, h * 0.70f)
          lineTo(w * 0.74f, h * 0.70f)
          cubicTo(w * 0.88f, h * 0.70f, w * 0.88f, h * 0.46f, w * 0.73f, h * 0.45f)
          cubicTo(w * 0.71f, h * 0.26f, w * 0.43f, h * 0.24f, w * 0.37f, h * 0.42f)
          cubicTo(w * 0.18f, h * 0.42f, w * 0.14f, h * 0.70f, w * 0.28f, h * 0.70f)
          close()
        }
        drawPath(cloud, tint, style = stroke)
        drawLine(tint, Offset(w * 0.16f, h * 0.16f), Offset(w * 0.84f, h * 0.84f), sw, StrokeCap.Round)
      }
    }
  }
}

@Stable
class DesktopSmartActionController(
  val apiClient: DesktopApiClient,
  private val scope: CoroutineScope,
  private val onToast: (String) -> Unit,
) {
  val linkPreviews = mutableStateMapOf<String, DesktopLinkPreview>()
  val savedLinks = mutableStateListOf<DesktopSavedLink>()
  var activeBrowserUrl by mutableStateOf<String?>(null)
  var activeBrowserTitleHint by mutableStateOf("")
  var activeBrowserSource by mutableStateOf("browser")
  var activePhoneEntity by mutableStateOf<SmartEntity.PhoneNumber?>(null)
  var activeEmailEntity by mutableStateOf<SmartEntity.Email?>(null)
  var isLoadingSavedLinks by mutableStateOf(false)

  private val inFlightPreviewUrls = mutableSetOf<String>()

  val savedUrls: Set<String>
    get() = savedLinks.map { it.normalizedUrl }.toSet()

  fun onAuthenticated() {
    val cached = apiClient.loadCachedSavedLinks()
    savedLinks.clear()
    savedLinks.addAll(cached)
    refreshSavedLinks("")
  }

  fun onLogout() {
    linkPreviews.clear()
    inFlightPreviewUrls.clear()
    savedLinks.clear()
    activeBrowserUrl = null
    activeBrowserTitleHint = ""
    activeBrowserSource = "browser"
    activePhoneEntity = null
    activeEmailEntity = null
  }

  fun openUrl(rawUrl: String, source: String, titleHint: String = "") {
    val safeUrl = SmartTextDetector.normalizeSafeHttpUrl(rawUrl)
    if (safeUrl == null) {
      onToast("Blocked unsafe or unsupported link")
      return
    }
    requestLinkPreview(safeUrl)
    val previewTitle = titleHint.ifBlank { linkPreviews[safeUrl]?.title.orEmpty() }
    activeBrowserTitleHint = previewTitle
    activeBrowserSource = source.ifBlank { "browser" }
    activeBrowserUrl = safeUrl
  }

  fun closeBrowser() {
    activeBrowserUrl = null
    activeBrowserTitleHint = ""
  }

  fun onPhoneClick(entity: SmartEntity.PhoneNumber) {
    activePhoneEntity = entity
  }

  fun onEmailClick(entity: SmartEntity.Email) {
    activeEmailEntity = entity
  }

  fun copyOtp(code: String) {
    copyText(code)
    onToast("Code copied: $code")
  }

  fun copyText(text: String) {
    runCatching {
      Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }
  }

  fun requestLinkPreview(rawUrl: String) {
    val safeUrl = SmartTextDetector.normalizeSafeHttpUrl(rawUrl) ?: return
    val existing = linkPreviews[safeUrl]
    if (existing != null && existing.status != "failed") return
    if (!inFlightPreviewUrls.add(safeUrl)) return

    val domain = SmartTextDetector.extractCleanDomain(safeUrl)
    linkPreviews[safeUrl] = DesktopLinkPreview(
      normalizedUrl = safeUrl,
      domain = domain,
      title = domain.ifBlank { safeUrl },
      status = "loading",
    )
    scope.launch {
      val fetched = withContext(Dispatchers.IO) { apiClient.fetchLinkPreview(safeUrl) }
      linkPreviews[safeUrl] = fetched
      inFlightPreviewUrls.remove(safeUrl)
    }
  }

  fun toggleSaveLink(rawUrl: String, source: String, titleHint: String = "") {
    val safeUrl = SmartTextDetector.normalizeSafeHttpUrl(rawUrl)
    if (safeUrl == null) {
      onToast("Unable to save invalid link")
      return
    }
    val existing = savedLinks.firstOrNull { it.normalizedUrl == safeUrl }
    if (existing != null) {
      val removeKey = existing.id.ifBlank { existing.normalizedUrl }
      savedLinks.removeAll { it.normalizedUrl == safeUrl }
      scope.launch {
        val res = withContext(Dispatchers.IO) { apiClient.deleteSavedLink(removeKey) }
        if (res is DesktopApiOutcome.Fail) {
          savedLinks.add(0, existing)
          onToast(res.message)
        } else {
          onToast("Removed from Saved Links")
        }
      }
    } else {
      val preview = linkPreviews[safeUrl]
      scope.launch {
        val res = withContext(Dispatchers.IO) {
          apiClient.saveLink(
            rawUrl = safeUrl,
            titleHint = titleHint,
            source = source,
            preview = preview,
          )
        }
        when (res) {
          is DesktopApiOutcome.Ok -> {
            savedLinks.removeAll { it.normalizedUrl == res.data.normalizedUrl }
            savedLinks.add(0, res.data)
            onToast("Saved to Saved Links")
          }
          is DesktopApiOutcome.Fail -> onToast(res.message)
        }
      }
    }
  }

  fun deleteSavedLink(item: DesktopSavedLink) {
    savedLinks.removeAll { it.id == item.id && it.normalizedUrl == item.normalizedUrl }
    scope.launch {
      val res = withContext(Dispatchers.IO) {
        apiClient.deleteSavedLink(item.id.ifBlank { item.normalizedUrl })
      }
      if (res is DesktopApiOutcome.Fail) {
        savedLinks.add(0, item)
        onToast(res.message)
      } else {
        onToast("Link removed")
      }
    }
  }

  fun refreshSavedLinks(query: String = "") {
    scope.launch {
      isLoadingSavedLinks = true
      val res = withContext(Dispatchers.IO) { apiClient.listSavedLinks(query) }
      if (res is DesktopApiOutcome.Ok) {
        if (query.isBlank()) {
          savedLinks.clear()
          savedLinks.addAll(res.data)
        }
      }
      isLoadingSavedLinks = false
    }
  }

  fun openExternalBrowser(rawUrl: String) {
    val safeUrl = SmartTextDetector.normalizeSafeHttpUrl(rawUrl) ?: run {
      onToast("Blocked unsafe URL")
      return
    }
    val opened = runCatching {
      if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(URI(safeUrl))
        true
      } else {
        false
      }
    }.getOrDefault(false)
    if (!opened) {
      copyText(safeUrl)
      onToast("Browser unavailable — link copied")
    }
  }

  fun dialPhone(dialNumber: String) {
    val clean = dialNumber.filter { it.isDigit() || it == '+' }
    val launched = runCatching {
      if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().browse(URI("tel:$clean"))
        true
      } else false
    }.getOrDefault(false)
    if (!launched) {
      copyText(clean)
      onToast("Dialer unavailable — number copied")
    }
  }

  fun smsPhone(dialNumber: String) {
    val clean = dialNumber.filter { it.isDigit() || it == '+' }
    val launched = runCatching {
      if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().browse(URI("sms:$clean"))
        true
      } else false
    }.getOrDefault(false)
    if (!launched) {
      copyText(clean)
      onToast("SMS app unavailable — number copied")
    }
  }

  fun savePhoneContactVcf(phone: SmartEntity.PhoneNumber) {
    val cleanDigits = phone.dialNumber.filter { it.isDigit() || it == '+' }
    val vcfContent = buildString {
      appendLine("BEGIN:VCARD")
      appendLine("VERSION:3.0")
      appendLine("FN:${phone.formattedDisplay}")
      appendLine("TEL;TYPE=CELL:$cleanDigits")
      appendLine("END:VCARD")
    }
    val fd = FileDialog(null as Frame?, "Save Contact Card", FileDialog.SAVE)
    fd.file = "contact_${cleanDigits.replace("+", "")}.vcf"
    fd.isVisible = true
    val dir = fd.directory
    val file = fd.file
    if (!dir.isNullOrBlank() && !file.isNullOrBlank()) {
      runCatching {
        File(dir, file).writeText(vcfContent, StandardCharsets.UTF_8)
        onToast("Contact card saved")
      }.onFailure {
        copyText(cleanDigits)
        onToast("Unable to save file — number copied")
      }
    }
  }

  fun composeEmail(address: String) {
    val clean = address.trim()
    val launched = runCatching {
      if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
        Desktop.getDesktop().mail(URI("mailto:${URLEncoder.encode(clean, StandardCharsets.UTF_8).replace("%40", "@")}"))
        true
      } else false
    }.getOrDefault(false)
    if (!launched) {
      copyText(clean)
      onToast("Email app unavailable — address copied")
    }
  }
}

val LocalDesktopSmartActions = staticCompositionLocalOf<DesktopSmartActionController?> { null }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesktopSmartInteractiveText(
  text: String,
  source: String,
  modifier: Modifier = Modifier,
  style: TextStyle = MaterialTheme.typography.bodyMedium,
  color: Color = MaterialTheme.colorScheme.onSurface,
  linkColor: Color = DesktopCrimson,
  otpHighlightBg: Color = linkColor.copy(alpha = 0.14f),
  maxLines: Int = Int.MAX_VALUE,
  overflow: TextOverflow = TextOverflow.Clip,
  textAlign: TextAlign? = null,
  showOtpPills: Boolean = true,
  onTextLayout: (TextLayoutResult) -> Unit = {},
) {
  val controller = LocalDesktopSmartActions.current
  val analysis = remember(text) { SmartTextDetector.analyze(text) }

  val annotated = remember(analysis, linkColor, otpHighlightBg, controller) {
    buildAnnotatedString {
      var cursor = 0
      for (entity in analysis.entities) {
        if (entity.start > cursor) {
          append(text.substring(cursor, entity.start))
        }
        val linkStyles = TextLinkStyles(
          style = when (entity) {
            is SmartEntity.Url,
            is SmartEntity.Email,
            is SmartEntity.PhoneNumber -> SpanStyle(
              color = linkColor,
              fontWeight = FontWeight.Medium,
              textDecoration = TextDecoration.Underline,
            )
            is SmartEntity.OtpCode -> SpanStyle(
              color = linkColor,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              background = otpHighlightBg,
            )
          },
        )
        val tag = when (entity) {
          is SmartEntity.Url -> "url:${entity.normalizedUrl}"
          is SmartEntity.Email -> "email:${entity.emailAddress}"
          is SmartEntity.PhoneNumber -> "phone:${entity.dialNumber}"
          is SmartEntity.OtpCode -> "otp:${entity.code}"
        }
        val displaySegment = when (entity) {
          is SmartEntity.PhoneNumber -> entity.formattedDisplay
          else -> text.substring(entity.start, entity.end)
        }
        val startOffset = length
        append(displaySegment)
        val endOffset = length
        addLink(
          clickable = LinkAnnotation.Clickable(
            tag = tag,
            styles = linkStyles,
            linkInteractionListener = {
              when (entity) {
                is SmartEntity.Url -> controller?.openUrl(entity.normalizedUrl, source)
                is SmartEntity.Email -> controller?.onEmailClick(entity)
                is SmartEntity.PhoneNumber -> controller?.onPhoneClick(entity)
                is SmartEntity.OtpCode -> controller?.copyOtp(entity.code)
              }
            },
          ),
          start = startOffset,
          end = endOffset,
        )
        cursor = entity.end
      }
      if (cursor < text.length) {
        append(text.substring(cursor))
      }
    }
  }

  Column(modifier = modifier) {
    Text(
      text = annotated,
      style = style,
      color = color,
      maxLines = maxLines,
      overflow = overflow,
      textAlign = textAlign,
      onTextLayout = onTextLayout,
    )

    if (showOtpPills && analysis.otpCodes.isNotEmpty()) {
      var copiedKey by remember(text) { mutableStateOf<String?>(null) }
      LaunchedEffect(copiedKey) {
        if (copiedKey != null) {
          delay(1600L)
          copiedKey = null
        }
      }
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 6.dp),
      ) {
        analysis.otpCodes.forEachIndexed { idx, otp ->
          val key = "${idx}_${otp.code}"
          val isCopied = copiedKey == key
          val green = Color(0xFF16A34A)
          Surface(
            onClick = {
              controller?.copyOtp(otp.code)
              copiedKey = key
            },
            shape = RoundedCornerShape(10.dp),
            color = if (isCopied) green.copy(alpha = 0.14f) else linkColor.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, if (isCopied) green.copy(alpha = 0.45f) else linkColor.copy(alpha = 0.35f)),
            modifier = Modifier.defaultMinSize(minHeight = 36.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
              AnimatedContent(targetState = isCopied, label = "desktop_otp_copy") { copied ->
                DesktopSmartSvgIcon(
                  kind = if (copied) DesktopSmartSvg.CHECK else DesktopSmartSvg.COPY,
                  tint = if (copied) green else linkColor,
                  size = 14.dp,
                )
              }
              Spacer(Modifier.width(6.dp))
              Text(
                text = if (isCopied) "Copied" else otp.code,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                color = if (isCopied) green else color,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun DesktopLinkPreviewStack(
  text: String,
  source: String,
  isDarkBubble: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val controller = LocalDesktopSmartActions.current ?: return
  val analysis = remember(text) { SmartTextDetector.analyze(text) }
  if (analysis.urls.isEmpty()) return

  val uniqueUrls = remember(analysis) {
    analysis.urls.distinctBy { it.normalizedUrl }.take(3)
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    uniqueUrls.forEach { entity ->
      LaunchedEffect(entity.normalizedUrl) {
        controller.requestLinkPreview(entity.normalizedUrl)
      }
      val preview = controller.linkPreviews[entity.normalizedUrl]
      val isSaved = entity.normalizedUrl in controller.savedUrls
      DesktopLinkPreviewCard(
        urlEntity = entity,
        preview = preview,
        isSaved = isSaved,
        isDarkBubble = isDarkBubble,
        onOpen = {
          controller.openUrl(entity.normalizedUrl, source, preview?.title.orEmpty())
        },
        onToggleSave = {
          controller.toggleSaveLink(entity.normalizedUrl, source, preview?.title.orEmpty())
        },
      )
    }
  }
}

@Composable
private fun DesktopLinkPreviewCard(
  urlEntity: SmartEntity.Url,
  preview: DesktopLinkPreview?,
  isSaved: Boolean,
  isDarkBubble: Boolean,
  onOpen: () -> Unit,
  onToggleSave: () -> Unit,
) {
  val cardBg = if (isDarkBubble) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface
  val border = if (isDarkBubble) Color.White.copy(alpha = 0.24f) else MaterialTheme.colorScheme.outlineVariant
  val primaryText = if (isDarkBubble) Color.White else MaterialTheme.colorScheme.onSurface
  val mutedText = if (isDarkBubble) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
  val accent = if (isDarkBubble) Color.White else DesktopCrimson
  val controller = LocalDesktopSmartActions.current

  Surface(
    onClick = onOpen,
    shape = RoundedCornerShape(14.dp),
    color = cardBg,
    border = BorderStroke(1.dp, border),
    modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
  ) {
    when {
      preview == null || preview.status == "loading" -> {
        DesktopLinkPreviewSkeleton(domain = urlEntity.domain, mutedText = mutedText)
      }
      preview.status == "failed" || (preview.status == "fallback" && preview.title.isBlank()) -> {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          DesktopSmartSvgIcon(DesktopSmartSvg.GLOBE, accent, 18.dp)
          Spacer(Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = urlEntity.domain.ifBlank { urlEntity.normalizedUrl },
              color = primaryText,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = urlEntity.normalizedUrl,
              color = mutedText,
              fontSize = 11.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
              .clickable(onClick = onToggleSave),
            contentAlignment = Alignment.Center,
          ) {
            DesktopSmartSvgIcon(
              kind = if (isSaved) DesktopSmartSvg.BOOKMARK_FILLED else DesktopSmartSvg.BOOKMARK_OUTLINE,
              tint = accent,
              size = 16.dp,
            )
          }
        }
      }
      else -> {
        Column(modifier = Modifier.fillMaxWidth()) {
          if (preview.imageUrl.isNotBlank() && controller != null) {
            DesktopPreviewHttpImage(
              apiClient = controller.apiClient,
              imageUrl = preview.imageUrl,
              modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
              contentScale = ContentScale.Crop,
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (preview.faviconUrl.isNotBlank() && controller != null) {
                  DesktopPreviewHttpImage(
                    apiClient = controller.apiClient,
                    imageUrl = preview.faviconUrl,
                    modifier = Modifier.size(14.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                  )
                } else {
                  DesktopSmartSvgIcon(DesktopSmartSvg.GLOBE, mutedText, 13.dp)
                }
                Text(
                  text = preview.domain.ifBlank { urlEntity.domain },
                  color = mutedText,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Text(
                text = preview.title.ifBlank { preview.domain },
                color = primaryText,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
              if (preview.description.isNotBlank()) {
                Text(
                  text = preview.description,
                  color = mutedText,
                  fontSize = 12.sp,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
            Spacer(Modifier.width(6.dp))
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable(onClick = onToggleSave),
              contentAlignment = Alignment.Center,
            ) {
              DesktopSmartSvgIcon(
                kind = if (isSaved) DesktopSmartSvg.BOOKMARK_FILLED else DesktopSmartSvg.BOOKMARK_OUTLINE,
                tint = if (isSaved) accent else mutedText,
                size = 16.dp,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DesktopLinkPreviewSkeleton(domain: String, mutedText: Color) {
  val inf = rememberInfiniteTransition(label = "desktop_preview_skel")
  val alpha by inf.animateFloat(
    initialValue = 0.28f,
    targetValue = 0.62f,
    animationSpec = infiniteRepeatable(tween(780), RepeatMode.Reverse),
    label = "skel_alpha",
  )
  Column(
    modifier = Modifier.fillMaxWidth().padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(7.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(mutedText.copy(alpha = alpha)))
      Text(domain, color = mutedText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    Box(modifier = Modifier.fillMaxWidth(0.76f).height(13.dp).clip(RoundedCornerShape(4.dp)).background(mutedText.copy(alpha = alpha)))
    Box(modifier = Modifier.fillMaxWidth(0.55f).height(11.dp).clip(RoundedCornerShape(4.dp)).background(mutedText.copy(alpha = alpha * 0.7f)))
  }
}

@Composable
private fun DesktopPreviewHttpImage(
  apiClient: DesktopApiClient,
  imageUrl: String,
  modifier: Modifier = Modifier,
  contentScale: ContentScale = ContentScale.Crop,
) {
  var bitmap by remember(imageUrl) { mutableStateOf<ImageBitmap?>(null) }
  LaunchedEffect(imageUrl) {
    val safe = SmartTextDetector.normalizeSafeHttpUrl(imageUrl)
    if (safe == null) {
      bitmap = null
      return@LaunchedEffect
    }
    bitmap = withContext(Dispatchers.IO) {
      if (!isPublicNetworkHost(safe)) return@withContext null
      val bytes = apiClient.fetchCachedMediaBytes(safe) ?: return@withContext null
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
  }
}

@Composable
fun DesktopSmartPhoneActionDialog(
  phone: SmartEntity.PhoneNumber,
  controller: DesktopSmartActionController,
  onDismiss: () -> Unit,
) {
  DesktopModalOverlay(onDismiss = onDismiss) {
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = DesktopCrimson.copy(alpha = 0.12f), modifier = Modifier.size(40.dp)) {
          Box(contentAlignment = Alignment.Center) {
            DesktopSmartSvgIcon(DesktopSmartSvg.PHONE, DesktopCrimson, 19.dp)
          }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(phone.formattedDisplay, fontSize = 17.sp, fontWeight = FontWeight.Bold)
          Text(phone.dialNumber, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
          modifier = Modifier.size(34.dp).clip(CircleShape).clickable(onClick = onDismiss),
          contentAlignment = Alignment.Center,
        ) {
          DesktopSmartSvgIcon(DesktopSmartSvg.CLOSE, MaterialTheme.colorScheme.onSurfaceVariant, 16.dp)
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      DesktopActionMenuItem(DesktopSmartSvg.PHONE, "Call") {
        onDismiss()
        controller.dialPhone(phone.dialNumber)
      }
      DesktopActionMenuItem(DesktopSmartSvg.COPY, "Copy") {
        controller.copyText(phone.dialNumber)
        onDismiss()
      }
      DesktopActionMenuItem(DesktopSmartSvg.CONTACT_ADD, "Save contact") {
        onDismiss()
        controller.savePhoneContactVcf(phone)
      }
      DesktopActionMenuItem(DesktopSmartSvg.MESSAGE, "Send message") {
        onDismiss()
        controller.smsPhone(phone.dialNumber)
      }
    }
  }
}

@Composable
fun DesktopSmartEmailActionDialog(
  email: SmartEntity.Email,
  controller: DesktopSmartActionController,
  onDismiss: () -> Unit,
) {
  DesktopModalOverlay(onDismiss = onDismiss) {
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = DesktopCrimson.copy(alpha = 0.12f), modifier = Modifier.size(40.dp)) {
          Box(contentAlignment = Alignment.Center) {
            DesktopSmartSvgIcon(DesktopSmartSvg.MAIL, DesktopCrimson, 19.dp)
          }
        }
        Spacer(Modifier.width(12.dp))
        Text(
          text = email.emailAddress,
          fontSize = 15.5.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Box(
          modifier = Modifier.size(34.dp).clip(CircleShape).clickable(onClick = onDismiss),
          contentAlignment = Alignment.Center,
        ) {
          DesktopSmartSvgIcon(DesktopSmartSvg.CLOSE, MaterialTheme.colorScheme.onSurfaceVariant, 16.dp)
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      DesktopActionMenuItem(DesktopSmartSvg.MAIL, "Send email") {
        onDismiss()
        controller.composeEmail(email.emailAddress)
      }
      DesktopActionMenuItem(DesktopSmartSvg.COPY, "Copy") {
        controller.copyText(email.emailAddress)
        onDismiss()
      }
    }
  }
}

@Composable
private fun DesktopActionMenuItem(
  icon: DesktopSmartSvg,
  label: String,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      DesktopSmartSvgIcon(icon, DesktopCrimson, 18.dp)
      Text(label, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
    }
  }
}

@Composable
fun DesktopInAppBrowserModal(
  initialUrl: String,
  titleHint: String,
  source: String,
  controller: DesktopSmartActionController,
  onClose: () -> Unit,
) {
  val safeInitial = remember(initialUrl) {
    SmartTextDetector.normalizeSafeHttpUrl(initialUrl) ?: ""
  }

  val history = remember(safeInitial) {
    mutableStateListOf<String>().apply { if (safeInitial.isNotBlank()) add(safeInitial) }
  }
  var historyIdx by remember(safeInitial) { mutableIntStateOf(0) }
  val currentUrl = history.getOrNull(historyIdx).orEmpty()

  var pageTitle by remember(currentUrl) {
    mutableStateOf(titleHint.ifBlank { SmartTextDetector.extractCleanDomain(currentUrl) })
  }
  var htmlContent by remember(currentUrl) { mutableStateOf<String?>(null) }
  var progress by remember { mutableIntStateOf(15) }
  var isLoading by remember(currentUrl) { mutableStateOf(true) }
  var hasError by remember(currentUrl) { mutableStateOf(false) }
  var errorNote by remember(currentUrl) { mutableStateOf("") }
  var reloadTick by remember { mutableIntStateOf(0) }

  val canGoBack = historyIdx > 0
  val canGoForward = historyIdx < history.lastIndex
  val isHttps = currentUrl.startsWith("https://", ignoreCase = true)
  val isSaved = SmartTextDetector.normalizeSafeHttpUrl(currentUrl)?.let { it in controller.savedUrls } ?: false
  val displayDomain = remember(currentUrl) {
    SmartTextDetector.extractCleanDomain(currentUrl).ifBlank { currentUrl }
  }

  val navigateToUrl: (String) -> Unit = { candidate ->
    val safe = SmartTextDetector.normalizeSafeHttpUrl(candidate)
    if (safe != null) {
      while (history.lastIndex > historyIdx) {
        history.removeAt(history.lastIndex)
      }
      if (history.lastOrNull() != safe) {
        history.add(safe)
        historyIdx = history.lastIndex
      } else {
        reloadTick++
      }
    }
  }

  LaunchedEffect(currentUrl, reloadTick) {
    if (currentUrl.isBlank()) {
      isLoading = false
      hasError = true
      errorNote = "Blocked or invalid URL."
      return@LaunchedEffect
    }
    isLoading = true
    hasError = false
    progress = 22
    val result = withContext(Dispatchers.IO) { fetchDesktopWebPage(currentUrl) }
    progress = 88
    if (result.error != null) {
      hasError = true
      errorNote = result.error
      isLoading = false
    } else {
      pageTitle = result.title.ifBlank { SmartTextDetector.extractCleanDomain(currentUrl) }
      htmlContent = result.htmlBody
      progress = 100
      isLoading = false
    }
  }

  val editorPane = remember {
    JEditorPane().apply {
      isEditable = false
      contentType = "text/html"
      putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, java.lang.Boolean.TRUE)
    }
  }

  DisposableEffect(currentUrl) {
    val listener = javax.swing.event.HyperlinkListener { ev ->
      if (ev.eventType == HyperlinkEvent.EventType.ACTIVATED) {
        val rawTarget = ev.url?.toString() ?: ev.description.orEmpty()
        SwingUtilities.invokeLater {
          navigateToUrl(rawTarget)
        }
      }
    }
    editorPane.addHyperlinkListener(listener)
    onDispose {
      editorPane.removeHyperlinkListener(listener)
    }
  }

  LaunchedEffect(htmlContent) {
    val html = htmlContent ?: return@LaunchedEffect
    SwingUtilities.invokeLater {
      runCatching {
        editorPane.contentType = "text/html"
        editorPane.text = html
        editorPane.caretPosition = 0
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.60f))
      .onPreviewKeyEvent { ev ->
        if (ev.type == KeyEventType.KeyDown && ev.key == Key.Escape) {
          if (canGoBack) historyIdx-- else onClose()
          true
        } else false
      }
      .padding(20.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = MaterialTheme.colorScheme.background,
      shadowElevation = 10.dp,
      modifier = Modifier.fillMaxSize().widthIn(max = 1040.dp),
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Modern Toolbar
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              onClick = onClose,
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              modifier = Modifier.size(40.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                DesktopSmartSvgIcon(DesktopSmartSvg.CLOSE, MaterialTheme.colorScheme.onSurface, 17.dp)
              }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = pageTitle.ifBlank { displayDomain },
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                DesktopSmartSvgIcon(
                  kind = if (isHttps) DesktopSmartSvg.LOCK_CLOSED else DesktopSmartSvg.LOCK_OPEN,
                  tint = if (isHttps) Color(0xFF16A34A) else Color(0xFFD97706),
                  size = 12.dp,
                )
                Text(
                  text = displayDomain,
                  fontSize = 11.5.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }

            Spacer(Modifier.width(8.dp))

            Surface(
              onClick = {
                if (currentUrl.isNotBlank()) {
                  controller.toggleSaveLink(currentUrl, source, pageTitle)
                }
              },
              shape = CircleShape,
              color = if (isSaved) DesktopCrimson.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              modifier = Modifier.size(40.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                DesktopSmartSvgIcon(
                  kind = if (isSaved) DesktopSmartSvg.BOOKMARK_FILLED else DesktopSmartSvg.BOOKMARK_OUTLINE,
                  tint = if (isSaved) DesktopCrimson else MaterialTheme.colorScheme.onSurface,
                  size = 17.dp,
                )
              }
            }

            Spacer(Modifier.width(6.dp))

            Surface(
              onClick = { controller.openExternalBrowser(currentUrl) },
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              modifier = Modifier.size(40.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                DesktopSmartSvgIcon(DesktopSmartSvg.OPEN_EXTERNAL, MaterialTheme.colorScheme.onSurface, 17.dp)
              }
            }
          }
        }

        if (isLoading) {
          LinearProgressIndicator(
            progress = { (progress.coerceIn(8, 100)) / 100f },
            color = DesktopCrimson,
            trackColor = DesktopCrimson.copy(alpha = 0.16f),
            modifier = Modifier.fillMaxWidth().height(3.dp),
          )
        } else {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        // Content Body
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
          if (hasError) {
            Column(
              modifier = Modifier.align(Alignment.Center).padding(28.dp).widthIn(max = 380.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              DesktopSmartSvgIcon(DesktopSmartSvg.CLOUD_OFF, DesktopCrimson, 46.dp)
              Text("Unable to load page", fontSize = 18.sp, fontWeight = FontWeight.Bold)
              Text(
                text = errorNote.ifBlank { displayDomain },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
              )
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                  onClick = { reloadTick++ },
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = DesktopCrimson),
                ) {
                  Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                  onClick = { controller.openExternalBrowser(currentUrl) },
                  shape = RoundedCornerShape(12.dp),
                ) {
                  Text("Open externally")
                }
              }
            }
          } else {
            SwingPanel(
              modifier = Modifier.fillMaxSize(),
              factory = {
                JScrollPane(editorPane).apply {
                  border = null
                  verticalScrollBar.unitIncrement = 16
                }
              },
            )
          }

          AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
          ) {
            DesktopTelefamBrowserReconnectAnimation()
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Bottom Navigation Bar
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              onClick = { if (canGoBack) historyIdx-- },
              enabled = canGoBack,
              shape = CircleShape,
              color = Color.Transparent,
              modifier = Modifier.size(44.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                DesktopSmartSvgIcon(
                  DesktopSmartSvg.BACK,
                  if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                  18.dp,
                )
              }
            }

            Surface(
              onClick = { if (canGoForward) historyIdx++ },
              enabled = canGoForward,
              shape = CircleShape,
              color = Color.Transparent,
              modifier = Modifier.size(44.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                DesktopSmartSvgIcon(
                  DesktopSmartSvg.FORWARD,
                  if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                  18.dp,
                )
              }
            }

            Surface(
              onClick = { reloadTick++ },
              shape = CircleShape,
              color = Color.Transparent,
              modifier = Modifier.size(44.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                DesktopSmartSvgIcon(
                  if (isLoading) DesktopSmartSvg.CLOSE else DesktopSmartSvg.REFRESH,
                  MaterialTheme.colorScheme.onSurface,
                  18.dp,
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
fun DesktopTelefamBrowserReconnectAnimation(modifier: Modifier = Modifier) {
  val transition = rememberInfiniteTransition(label = "desktop_telefam_browser")
  val phase by transition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "break_phase",
  )

  val telefamLetters = listOf("T", "e", "l", "e", "f", "a", "m")
  val browserLetters = listOf("B", "r", "o", "w", "s", "e", "r")

  Surface(
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
    shadowElevation = 6.dp,
    border = BorderStroke(1.dp, DesktopCrimson.copy(alpha = 0.35f)),
    modifier = modifier,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
      Surface(
        shape = CircleShape,
        color = DesktopCrimson.copy(alpha = 0.14f),
        modifier = Modifier.size(28.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          DesktopSmartSvgIcon(DesktopSmartSvg.GLOBE, DesktopCrimson, 16.dp)
        }
      }
      Spacer(Modifier.width((6 + phase * 10).dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        telefamLetters.forEachIndexed { idx, ch ->
          val dx = (idx - 3f) * phase * 2.2f
          val dy = sin(idx * 1.1 + phase * 3.14).toFloat() * phase * 4f
          Text(
            text = ch,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = DesktopCrimson,
            modifier = Modifier.offset(x = dx.dp, y = dy.dp).alpha(1f - phase * 0.25f),
          )
        }
      }
      Spacer(Modifier.width((6 + phase * 14).dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        browserLetters.forEachIndexed { idx, ch ->
          val dx = (idx - 3f) * phase * 2.2f
          val dy = -sin(idx * 1.1 + phase * 3.14).toFloat() * phase * 4f
          Text(
            text = ch,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.offset(x = dx.dp, y = dy.dp).alpha(1f - phase * 0.25f),
          )
        }
      }
    }
  }
}

@Composable
fun DesktopSavedLinksPane(
  controller: DesktopSmartActionController,
  modifier: Modifier = Modifier,
) {
  var searchQuery by remember { mutableStateOf("") }
  val filtered = remember(controller.savedLinks.toList(), searchQuery) {
    val q = searchQuery.trim().lowercase()
    if (q.isEmpty()) controller.savedLinks.toList() else controller.savedLinks.filter {
      it.title.lowercase().contains(q) ||
        it.domain.lowercase().contains(q) ||
        it.url.lowercase().contains(q) ||
        it.source.lowercase().contains(q)
    }
  }
  val dateFmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

  LaunchedEffect(Unit) {
    controller.refreshSavedLinks("")
  }

  Column(modifier = modifier.fillMaxSize()) {
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).height(44.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        DesktopSmartSvgIcon(DesktopSmartSvg.GLOBE, MaterialTheme.colorScheme.onSurfaceVariant, 16.dp)
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
          if (searchQuery.isEmpty()) {
            Text("Search saved links", fontSize = 13.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          BasicTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it.take(100) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.5.sp),
            cursorBrush = SolidColor(DesktopCrimson),
            modifier = Modifier.fillMaxWidth(),
          )
        }
        if (searchQuery.isNotEmpty()) {
          Box(
            modifier = Modifier.size(26.dp).clip(CircleShape).clickable { searchQuery = "" },
            contentAlignment = Alignment.Center,
          ) {
            DesktopSmartSvgIcon(DesktopSmartSvg.CLOSE, MaterialTheme.colorScheme.onSurfaceVariant, 14.dp)
          }
        }
      }
    }

    if (filtered.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
          DesktopSmartSvgIcon(DesktopSmartSvg.BOOKMARK_OUTLINE, MaterialTheme.colorScheme.onSurfaceVariant, 34.dp)
          Text(
            text = if (searchQuery.isBlank()) "No saved links yet" else "No matching links",
            fontWeight = FontWeight.Bold,
            fontSize = 15.5.sp,
          )
          Text(
            text = "Bookmark links from chats, feeds, or the Telefam Browser.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.5.sp,
            textAlign = TextAlign.Center,
          )
        }
      }
    } else {
      LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
      ) {
        items(filtered, key = { "${it.id}_${it.normalizedUrl}" }) { item ->
          Surface(
            onClick = {
              controller.openUrl(
                rawUrl = item.normalizedUrl.ifBlank { item.url },
                source = item.source,
                titleHint = item.title,
              )
            },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              if (item.previewImageUrl.isNotBlank()) {
                DesktopPreviewHttpImage(
                  apiClient = controller.apiClient,
                  imageUrl = item.previewImageUrl,
                  modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                )
              } else {
                Box(
                  modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DesktopCrimson.copy(alpha = 0.12f)),
                  contentAlignment = Alignment.Center,
                ) {
                  DesktopSmartSvgIcon(DesktopSmartSvg.GLOBE, DesktopCrimson, 24.dp)
                }
              }

              Spacer(Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                  text = item.title.ifBlank { item.domain },
                  fontSize = 14.5.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = item.domain.ifBlank { item.normalizedUrl },
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DesktopCrimson.copy(alpha = 0.10f),
                  ) {
                    Text(
                      text = desktopFormatSourceLabel(item.source),
                      color = DesktopCrimson,
                      fontSize = 10.5.sp,
                      fontWeight = FontWeight.SemiBold,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                  }
                  if (item.savedAtMs > 0L) {
                    Text(
                      text = dateFmt.format(Date(item.savedAtMs)),
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }

              Spacer(Modifier.width(8.dp))

              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .clickable { controller.deleteSavedLink(item) },
                contentAlignment = Alignment.Center,
              ) {
                DesktopSmartSvgIcon(DesktopSmartSvg.TRASH, MaterialTheme.colorScheme.onSurfaceVariant, 17.dp)
              }
            }
          }
        }
      }
    }
  }
}

private fun desktopFormatSourceLabel(source: String): String = when (source.lowercase()) {
  "chat" -> "Chat"
  "feed" -> "Feed"
  "profile" -> "Profile"
  else -> "Browser"
}

private data class DesktopWebFetchResult(
  val title: String = "",
  val htmlBody: String = "",
  val error: String? = null,
)

private val browserHttpClient: HttpClient by lazy {
  HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .followRedirects(HttpClient.Redirect.NEVER)
    .build()
}

private fun isPublicNetworkHost(rawUrl: String): Boolean {
  val uri = runCatching { URI(rawUrl) }.getOrNull() ?: return false
  val host = uri.host ?: return false
  val addrs = runCatching { InetAddress.getAllByName(host) }.getOrNull() ?: return false
  if (addrs.isEmpty()) return false
  return addrs.none { addr ->
    addr.isAnyLocalAddress ||
      addr.isLoopbackAddress ||
      addr.isLinkLocalAddress ||
      addr.isSiteLocalAddress ||
      addr.isMulticastAddress
  }
}

private fun fetchDesktopWebPage(initialUrl: String): DesktopWebFetchResult {
  var current = SmartTextDetector.normalizeSafeHttpUrl(initialUrl)
    ?: return DesktopWebFetchResult(error = "Blocked unsafe URL")
  var hops = 0
  while (hops < 4) {
    if (!isPublicNetworkHost(current)) {
      return DesktopWebFetchResult(error = "Blocked private or local network address")
    }
    val req = runCatching {
      HttpRequest.newBuilder(URI(current))
        .timeout(Duration.ofSeconds(14))
        .header("User-Agent", "TelefamBrowser/1.0")
        .GET()
        .build()
    }.getOrNull() ?: return DesktopWebFetchResult(error = "Malformed URL")

    val res = runCatching {
      browserHttpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
    }.getOrElse { ex ->
      return DesktopWebFetchResult(error = ex.message ?: "Unable to connect")
    }

    if (res.statusCode() in 300..399) {
      val loc = res.headers().firstValue("Location").orElse("")
      val resolved = runCatching { URI(current).resolve(loc).toString() }.getOrNull().orEmpty()
      current = SmartTextDetector.normalizeSafeHttpUrl(resolved)
        ?: return DesktopWebFetchResult(error = "Redirect target blocked for security")
      hops++
      continue
    }

    if (res.statusCode() !in 200..299) {
      return DesktopWebFetchResult(error = "HTTP ${res.statusCode()}")
    }

    val rawBody = res.body().take(450_000)
    val contentType = res.headers().firstValue("Content-Type").orElse("text/html")
    val titleMatch = Regex("(?is)<title[^>]*>(.*?)</title>").find(rawBody)?.groupValues?.get(1)
      ?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
    val sanitizedHtml = if (contentType.contains("html", ignoreCase = true)) {
      val stripped = rawBody
        .replace(Regex("(?is)<script[^>]*>.*?</script>"), "")
        .replace(Regex("(?is)<style[^>]*>.*?</style>"), "")
        .replace(Regex("(?is)<iframe[^>]*>.*?</iframe>"), "")
        .replace(Regex("(?is)\\son[a-z]+\\s*=\\s*\"[^\"]*\""), "")
        .replace(Regex("(?is)\\son[a-z]+\\s*=\\s*'[^']*'"), "")
      val bodyOnly = Regex("(?is)<body[^>]*>(.*?)</body>").find(stripped)?.groupValues?.get(1) ?: stripped
      "<html><body style='font-family:sans-serif;font-size:14px;padding:18px;line-height:1.5;'>$bodyOnly</body></html>"
    } else {
      val escaped = rawBody
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
      "<html><body style='font-family:monospace;font-size:13px;padding:18px;white-space:pre-wrap;'>$escaped</body></html>"
    }
    return DesktopWebFetchResult(title = titleMatch, htmlBody = sanitizedHtml)
  }
  return DesktopWebFetchResult(error = "Too many redirects")
}
