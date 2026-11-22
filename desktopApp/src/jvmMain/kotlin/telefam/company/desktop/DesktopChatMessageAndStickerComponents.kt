package telefam.company.desktop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image as SkiaImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class DesktopChatSvg {
  SEND_PLANE,
  MIC,
  GIFT,
  PAPERCLIP,
  STICKER_SMILE,
  PHONE_CALL,
  VIDEO_CAM,
  MORE_VERT,
  BACK,
  CLOCK_SENDING,
  CHECK_SINGLE,
  CHECK_DOUBLE,
  PLAY,
  PAUSE,
  SHIELD_LOCK,
  EYE_ONCE,
  COPY,
  FORWARD,
  EDIT,
  TRASH,
  ARCHIVE,
  MUTE,
  UNMUTE,
  MARK_UNREAD,
  BLOCK,
  CLOSE,
  CALL_END,
  MIC_OFF,
  VIDEO_OFF,
  SCREEN_SHARE,
  SPEAKER_ON,
  FILTERS,
  FLIP_CAMERA,
  EXPAND,
  QUALITY_SIGNAL,
  MISSED_CALL,
}

@Composable
fun DesktopChatVectorIcon(
  kind: DesktopChatSvg,
  tint: Color,
  size: Dp = 18.dp,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = Stroke(width = (w * 0.085f).coerceAtLeast(1.4f), cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (kind) {
      DesktopChatSvg.SEND_PLANE -> {
        val p = Path().apply {
          moveTo(w * 0.14f, h * 0.48f)
          lineTo(w * 0.88f, h * 0.14f)
          lineTo(w * 0.62f, h * 0.88f)
          lineTo(w * 0.46f, h * 0.56f)
          close()
        }
        drawPath(p, tint)
      }
      DesktopChatSvg.MIC -> {
        drawRoundRect(tint, topLeft = Offset(w * 0.36f, h * 0.12f), size = Size(w * 0.28f, h * 0.44f), cornerRadius = CornerRadius(w * 0.14f))
        val arc = Path().apply {
          moveTo(w * 0.24f, h * 0.46f)
          cubicTo(w * 0.24f, h * 0.72f, w * 0.76f, h * 0.72f, w * 0.76f, h * 0.46f)
        }
        drawPath(arc, tint, style = stroke)
        drawLine(tint, Offset(w * 0.50f, h * 0.68f), Offset(w * 0.50f, h * 0.86f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.34f, h * 0.86f), Offset(w * 0.66f, h * 0.86f), strokeWidth = stroke.width, cap = StrokeCap.Round)
      }
      DesktopChatSvg.GIFT -> {
        drawRoundRect(tint, topLeft = Offset(w * 0.16f, h * 0.34f), size = Size(w * 0.68f, h * 0.16f), cornerRadius = CornerRadius(w * 0.04f), style = stroke)
        drawRoundRect(tint, topLeft = Offset(w * 0.20f, h * 0.50f), size = Size(w * 0.60f, h * 0.36f), cornerRadius = CornerRadius(w * 0.04f), style = stroke)
        drawLine(tint, Offset(w * 0.50f, h * 0.34f), Offset(w * 0.50f, h * 0.86f), strokeWidth = stroke.width)
        drawCircle(tint, radius = w * 0.10f, center = Offset(w * 0.38f, h * 0.24f), style = stroke)
        drawCircle(tint, radius = w * 0.10f, center = Offset(w * 0.62f, h * 0.24f), style = stroke)
      }
      DesktopChatSvg.PAPERCLIP -> {
        val p = Path().apply {
          moveTo(w * 0.68f, h * 0.32f)
          lineTo(w * 0.34f, h * 0.66f)
          cubicTo(w * 0.24f, h * 0.76f, w * 0.38f, h * 0.90f, w * 0.48f, h * 0.80f)
          lineTo(w * 0.80f, h * 0.48f)
          cubicTo(w * 0.92f, h * 0.36f, w * 0.72f, h * 0.14f, w * 0.58f, h * 0.26f)
          lineTo(w * 0.24f, h * 0.60f)
        }
        drawPath(p, tint, style = stroke)
      }
      DesktopChatSvg.STICKER_SMILE -> {
        drawCircle(tint, radius = w * 0.38f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
        drawCircle(tint, radius = w * 0.045f, center = Offset(w * 0.38f, h * 0.42f))
        drawCircle(tint, radius = w * 0.045f, center = Offset(w * 0.62f, h * 0.42f))
        val smile = Path().apply {
          moveTo(w * 0.35f, h * 0.57f)
          quadraticTo(w * 0.50f, h * 0.70f, w * 0.65f, h * 0.57f)
        }
        drawPath(smile, tint, style = stroke)
      }
      DesktopChatSvg.PHONE_CALL -> {
        val p = Path().apply {
          moveTo(w * 0.24f, h * 0.20f)
          lineTo(w * 0.38f, h * 0.20f)
          lineTo(w * 0.44f, h * 0.36f)
          lineTo(w * 0.34f, h * 0.46f)
          quadraticTo(w * 0.42f, h * 0.60f, w * 0.54f, h * 0.66f)
          lineTo(w * 0.64f, h * 0.56f)
          lineTo(w * 0.80f, h * 0.62f)
          lineTo(w * 0.80f, h * 0.76f)
          quadraticTo(w * 0.52f, h * 0.88f, w * 0.20f, h * 0.44f)
          close()
        }
        drawPath(p, tint, style = stroke)
      }
      DesktopChatSvg.VIDEO_CAM -> {
        drawRoundRect(tint, topLeft = Offset(w * 0.12f, h * 0.28f), size = Size(w * 0.52f, h * 0.44f), cornerRadius = CornerRadius(w * 0.08f), style = stroke)
        val tri = Path().apply {
          moveTo(w * 0.66f, h * 0.42f)
          lineTo(w * 0.88f, h * 0.30f)
          lineTo(w * 0.88f, h * 0.70f)
          lineTo(w * 0.66f, h * 0.58f)
          close()
        }
        drawPath(tri, tint, style = stroke)
      }
      DesktopChatSvg.MORE_VERT -> {
        drawCircle(tint, w * 0.07f, Offset(w * 0.50f, h * 0.24f))
        drawCircle(tint, w * 0.07f, Offset(w * 0.50f, h * 0.50f))
        drawCircle(tint, w * 0.07f, Offset(w * 0.50f, h * 0.76f))
      }
      DesktopChatSvg.BACK -> {
        val p = Path().apply {
          moveTo(w * 0.76f, h * 0.50f)
          lineTo(w * 0.24f, h * 0.50f)
          moveTo(w * 0.46f, h * 0.26f)
          lineTo(w * 0.24f, h * 0.50f)
          lineTo(w * 0.46f, h * 0.74f)
        }
        drawPath(p, tint, style = stroke)
      }
      DesktopChatSvg.CLOCK_SENDING -> {
        drawCircle(tint, radius = w * 0.36f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
        drawLine(tint, Offset(w * 0.50f, h * 0.50f), Offset(w * 0.50f, h * 0.28f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.50f, h * 0.50f), Offset(w * 0.68f, h * 0.54f), strokeWidth = stroke.width, cap = StrokeCap.Round)
      }
      DesktopChatSvg.CHECK_SINGLE -> {
        val p = Path().apply {
          moveTo(w * 0.20f, h * 0.52f)
          lineTo(w * 0.42f, h * 0.72f)
          lineTo(w * 0.82f, h * 0.28f)
        }
        drawPath(p, tint, style = stroke)
      }
      DesktopChatSvg.CHECK_DOUBLE -> {
        val p1 = Path().apply {
          moveTo(w * 0.10f, h * 0.54f)
          lineTo(w * 0.30f, h * 0.74f)
          lineTo(w * 0.64f, h * 0.30f)
        }
        val p2 = Path().apply {
          moveTo(w * 0.42f, h * 0.60f)
          lineTo(w * 0.56f, h * 0.74f)
          lineTo(w * 0.90f, h * 0.30f)
        }
        drawPath(p1, tint, style = stroke)
        drawPath(p2, tint, style = stroke)
      }
      DesktopChatSvg.PLAY -> {
        val p = Path().apply {
          moveTo(w * 0.28f, h * 0.20f)
          lineTo(w * 0.80f, h * 0.50f)
          lineTo(w * 0.28f, h * 0.80f)
          close()
        }
        drawPath(p, tint)
      }
      DesktopChatSvg.PAUSE -> {
        drawRoundRect(tint, Offset(w * 0.26f, h * 0.22f), Size(w * 0.16f, h * 0.56f), CornerRadius(w * 0.04f))
        drawRoundRect(tint, Offset(w * 0.58f, h * 0.22f), Size(w * 0.16f, h * 0.56f), CornerRadius(w * 0.04f))
      }
      DesktopChatSvg.SHIELD_LOCK -> {
        val p = Path().apply {
          moveTo(w * 0.50f, h * 0.12f)
          lineTo(w * 0.84f, h * 0.26f)
          lineTo(w * 0.84f, h * 0.54f)
          quadraticTo(w * 0.84f, h * 0.78f, w * 0.50f, h * 0.90f)
          quadraticTo(w * 0.16f, h * 0.78f, w * 0.16f, h * 0.54f)
          lineTo(w * 0.16f, h * 0.26f)
          close()
        }
        drawPath(p, tint, style = stroke)
      }
      DesktopChatSvg.EYE_ONCE -> {
        drawCircle(tint, radius = w * 0.38f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
        drawLine(tint, Offset(w * 0.50f, h * 0.32f), Offset(w * 0.50f, h * 0.68f), strokeWidth = stroke.width, cap = StrokeCap.Round)
      }
      DesktopChatSvg.COPY -> {
        drawRoundRect(tint, Offset(w * 0.32f, h * 0.16f), Size(w * 0.50f, h * 0.56f), CornerRadius(w * 0.06f), style = stroke)
        drawRoundRect(tint, Offset(w * 0.18f, h * 0.30f), Size(w * 0.50f, h * 0.56f), CornerRadius(w * 0.06f), style = stroke)
      }
      DesktopChatSvg.FORWARD -> {
        val p = Path().apply {
          moveTo(w * 0.56f, h * 0.22f)
          lineTo(w * 0.86f, h * 0.46f)
          lineTo(w * 0.56f, h * 0.70f)
          moveTo(w * 0.86f, h * 0.46f)
          lineTo(w * 0.36f, h * 0.46f)
          quadraticTo(w * 0.16f, h * 0.46f, w * 0.16f, h * 0.76f)
        }
        drawPath(p, tint, style = stroke)
      }
      DesktopChatSvg.EDIT -> {
        val p = Path().apply {
          moveTo(w * 0.20f, h * 0.80f)
          lineTo(w * 0.28f, h * 0.58f)
          lineTo(w * 0.68f, h * 0.18f)
          lineTo(w * 0.82f, h * 0.32f)
          lineTo(w * 0.42f, h * 0.72f)
          close()
        }
        drawPath(p, tint, style = stroke)
      }
      DesktopChatSvg.TRASH -> {
        drawLine(tint, Offset(w * 0.20f, h * 0.28f), Offset(w * 0.80f, h * 0.28f), strokeWidth = stroke.width)
        drawRoundRect(tint, Offset(w * 0.26f, h * 0.28f), Size(w * 0.48f, h * 0.56f), CornerRadius(w * 0.06f), style = stroke)
        drawLine(tint, Offset(w * 0.38f, h * 0.16f), Offset(w * 0.62f, h * 0.16f), strokeWidth = stroke.width)
      }
      DesktopChatSvg.ARCHIVE -> {
        drawRoundRect(tint, Offset(w * 0.14f, h * 0.20f), Size(w * 0.72f, h * 0.20f), CornerRadius(w * 0.04f), style = stroke)
        drawRoundRect(tint, Offset(w * 0.20f, h * 0.40f), Size(w * 0.60f, h * 0.42f), CornerRadius(w * 0.05f), style = stroke)
        drawLine(tint, Offset(w * 0.40f, h * 0.56f), Offset(w * 0.60f, h * 0.56f), strokeWidth = stroke.width)
      }
      DesktopChatSvg.MUTE, DesktopChatSvg.UNMUTE -> {
        val p = Path().apply {
          moveTo(w * 0.18f, h * 0.40f)
          lineTo(w * 0.36f, h * 0.40f)
          lineTo(w * 0.56f, h * 0.22f)
          lineTo(w * 0.56f, h * 0.78f)
          lineTo(w * 0.36f, h * 0.60f)
          lineTo(w * 0.18f, h * 0.60f)
          close()
        }
        drawPath(p, tint, style = stroke)
        if (kind == DesktopChatSvg.MUTE) {
          drawLine(tint, Offset(w * 0.66f, h * 0.36f), Offset(w * 0.86f, h * 0.64f), strokeWidth = stroke.width)
          drawLine(tint, Offset(w * 0.86f, h * 0.36f), Offset(w * 0.66f, h * 0.64f), strokeWidth = stroke.width)
        } else {
          drawCircle(tint, radius = w * 0.12f, center = Offset(w * 0.74f, h * 0.50f), style = stroke)
        }
      }
      DesktopChatSvg.MARK_UNREAD -> {
        drawCircle(tint, radius = w * 0.26f, center = Offset(w * 0.50f, h * 0.50f))
      }
      DesktopChatSvg.BLOCK -> {
        drawCircle(tint, radius = w * 0.36f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
        drawLine(tint, Offset(w * 0.26f, h * 0.26f), Offset(w * 0.74f, h * 0.74f), strokeWidth = stroke.width)
      }
      DesktopChatSvg.CLOSE -> {
        drawLine(tint, Offset(w * 0.24f, h * 0.24f), Offset(w * 0.76f, h * 0.76f), strokeWidth = stroke.width)
        drawLine(tint, Offset(w * 0.76f, h * 0.24f), Offset(w * 0.24f, h * 0.76f), strokeWidth = stroke.width)
      }
      DesktopChatSvg.CALL_END -> {
        val p = Path().apply {
          moveTo(w * 0.14f, h * 0.56f)
          quadraticTo(w * 0.50f, h * 0.30f, w * 0.86f, h * 0.56f)
          lineTo(w * 0.82f, h * 0.70f)
          lineTo(w * 0.66f, h * 0.66f)
          lineTo(w * 0.66f, h * 0.54f)
          quadraticTo(w * 0.50f, h * 0.46f, w * 0.34f, h * 0.54f)
          lineTo(w * 0.34f, h * 0.66f)
          lineTo(w * 0.18f, h * 0.70f)
          close()
        }
        drawPath(p, tint)
      }
      DesktopChatSvg.MIC_OFF -> {
        drawRoundRect(tint, topLeft = Offset(w * 0.36f, h * 0.14f), size = Size(w * 0.28f, h * 0.42f), cornerRadius = CornerRadius(w * 0.14f), style = stroke)
        drawLine(tint, Offset(w * 0.50f, h * 0.68f), Offset(w * 0.50f, h * 0.86f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.18f, h * 0.20f), Offset(w * 0.82f, h * 0.82f), strokeWidth = stroke.width * 1.15f, cap = StrokeCap.Round)
      }
      DesktopChatSvg.VIDEO_OFF -> {
        drawRoundRect(tint, topLeft = Offset(w * 0.12f, h * 0.28f), size = Size(w * 0.52f, h * 0.44f), cornerRadius = CornerRadius(w * 0.08f), style = stroke)
        drawLine(tint, Offset(w * 0.16f, h * 0.20f), Offset(w * 0.84f, h * 0.80f), strokeWidth = stroke.width * 1.15f, cap = StrokeCap.Round)
      }
      DesktopChatSvg.SCREEN_SHARE -> {
        drawRoundRect(tint, topLeft = Offset(w * 0.14f, h * 0.20f), size = Size(w * 0.72f, h * 0.50f), cornerRadius = CornerRadius(w * 0.06f), style = stroke)
        drawLine(tint, Offset(w * 0.34f, h * 0.82f), Offset(w * 0.66f, h * 0.82f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        val arr = Path().apply {
          moveTo(w * 0.50f, h * 0.58f)
          lineTo(w * 0.50f, h * 0.32f)
          moveTo(w * 0.38f, h * 0.42f)
          lineTo(w * 0.50f, h * 0.30f)
          lineTo(w * 0.62f, h * 0.42f)
        }
        drawPath(arr, tint, style = stroke)
      }
      DesktopChatSvg.SPEAKER_ON -> {
        val p = Path().apply {
          moveTo(w * 0.16f, h * 0.40f)
          lineTo(w * 0.34f, h * 0.40f)
          lineTo(w * 0.54f, h * 0.22f)
          lineTo(w * 0.54f, h * 0.78f)
          lineTo(w * 0.34f, h * 0.60f)
          lineTo(w * 0.16f, h * 0.60f)
          close()
        }
        drawPath(p, tint, style = stroke)
        val arc1 = Path().apply {
          moveTo(w * 0.66f, h * 0.36f)
          quadraticTo(w * 0.76f, h * 0.50f, w * 0.66f, h * 0.64f)
        }
        val arc2 = Path().apply {
          moveTo(w * 0.76f, h * 0.26f)
          quadraticTo(w * 0.90f, h * 0.50f, w * 0.76f, h * 0.74f)
        }
        drawPath(arc1, tint, style = stroke)
        drawPath(arc2, tint, style = stroke)
      }
      DesktopChatSvg.FILTERS -> {
        drawCircle(tint, radius = w * 0.20f, center = Offset(w * 0.36f, h * 0.56f), style = stroke)
        drawCircle(tint, radius = w * 0.20f, center = Offset(w * 0.64f, h * 0.56f), style = stroke)
        drawCircle(tint, radius = w * 0.20f, center = Offset(w * 0.50f, h * 0.32f), style = stroke)
      }
      DesktopChatSvg.FLIP_CAMERA -> {
        val p = Path().apply {
          moveTo(w * 0.22f, h * 0.44f)
          quadraticTo(w * 0.50f, h * 0.18f, w * 0.78f, h * 0.44f)
          moveTo(w * 0.78f, h * 0.56f)
          quadraticTo(w * 0.50f, h * 0.82f, w * 0.22f, h * 0.56f)
        }
        drawPath(p, tint, style = stroke)
        drawCircle(tint, radius = w * 0.08f, center = Offset(w * 0.50f, h * 0.50f))
      }
      DesktopChatSvg.EXPAND -> {
        val p = Path().apply {
          moveTo(w * 0.58f, h * 0.20f); lineTo(w * 0.80f, h * 0.20f); lineTo(w * 0.80f, h * 0.42f)
          moveTo(w * 0.42f, h * 0.80f); lineTo(w * 0.20f, h * 0.80f); lineTo(w * 0.20f, h * 0.58f)
        }
        drawPath(p, tint, style = stroke)
      }
      DesktopChatSvg.QUALITY_SIGNAL -> {
        drawLine(tint, Offset(w * 0.20f, h * 0.76f), Offset(w * 0.20f, h * 0.62f), strokeWidth = stroke.width * 1.2f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.44f, h * 0.76f), Offset(w * 0.44f, h * 0.48f), strokeWidth = stroke.width * 1.2f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.68f, h * 0.76f), Offset(w * 0.68f, h * 0.32f), strokeWidth = stroke.width * 1.2f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.88f, h * 0.76f), Offset(w * 0.88f, h * 0.20f), strokeWidth = stroke.width * 1.2f, cap = StrokeCap.Round)
      }
      DesktopChatSvg.MISSED_CALL -> {
        val p = Path().apply {
          moveTo(w * 0.24f, h * 0.24f)
          lineTo(w * 0.50f, h * 0.52f)
          lineTo(w * 0.78f, h * 0.24f)
          moveTo(w * 0.60f, h * 0.24f)
          lineTo(w * 0.78f, h * 0.24f)
          lineTo(w * 0.78f, h * 0.42f)
        }
        drawPath(p, tint, style = stroke)
        drawRoundRect(tint, Offset(w * 0.22f, h * 0.66f), Size(w * 0.56f, h * 0.12f), CornerRadius(w * 0.06f))
      }
    }
  }
}

private object DesktopMediaBytesCache {
  val bytes = ConcurrentHashMap<String, ByteArray>()
}

/**
 * Animated sticker / GIF / clip decoder supporting animated GIF/WebP frames via Skia Codec.
 * Always keeps a transparent background so stickers float directly on the chat wallpaper.
 */
@Composable
fun DesktopAnimatedGiflyMedia(
  apiClient: DesktopApiClient,
  url: String,
  contentDescription: String,
  modifier: Modifier = Modifier,
) {
  var frameBitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }

  LaunchedEffect(url) {
    if (url.isBlank()) return@LaunchedEffect
    val rawBytes = DesktopMediaBytesCache.bytes[url] ?: withContext(Dispatchers.IO) {
      apiClient.downloadMediaBytes(url)?.also { DesktopMediaBytesCache.bytes[url] = it }
    } ?: return@LaunchedEffect

    val codec = runCatching { Codec.makeFromData(Data.makeFromBytes(rawBytes)) }.getOrNull()
    if (codec == null) {
      val img = runCatching { SkiaImage.makeFromEncoded(rawBytes).toComposeImageBitmap() }.getOrNull()
      frameBitmap = img
      return@LaunchedEffect
    }

    val frameCount = codec.frameCount.coerceAtLeast(1)
    if (frameCount == 1) {
      val bmp = Bitmap().apply { allocPixels(codec.imageInfo) }
      runCatching { codec.readPixels(bmp, 0) }
      frameBitmap = runCatching { SkiaImage.makeFromBitmap(bmp).toComposeImageBitmap() }.getOrNull()
    } else {
      val infos = codec.framesInfo
      var idx = 0
      while (isActive) {
        val bmp = Bitmap().apply { allocPixels(codec.imageInfo) }
        runCatching { codec.readPixels(bmp, idx) }
        frameBitmap = runCatching { SkiaImage.makeFromBitmap(bmp).toComposeImageBitmap() }.getOrNull()
        val durMs = (infos.getOrNull(idx)?.duration ?: 75).coerceIn(30, 300).toLong()
        delay(durMs)
        idx = (idx + 1) % frameCount
      }
    }
  }

  if (frameBitmap != null) {
    Image(
      bitmap = frameBitmap!!,
      contentDescription = contentDescription,
      contentScale = ContentScale.Fit,
      modifier = modifier,
    )
  } else {
    Box(modifier = modifier)
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DesktopChatMessageBubble(
  msg: DesktopChatMessage,
  isOwn: Boolean,
  isDissolving: Boolean,
  isPlayingVoice: Boolean,
  bubbleGradient: List<Color>,
  canCopy: Boolean,
  canForward: Boolean,
  peerName: String,
  peerAvatarUrl: String,
  apiClient: DesktopApiClient,
  onToggleVoice: () -> Unit,
  onCopy: (String) -> Unit,
  onForward: () -> Unit,
  onEdit: () -> Unit,
  onUnsend: () -> Unit,
  onDeleteForMe: () -> Unit,
  onCallBack: (DesktopCallType) -> Unit = {},
) {
  val isDark = MaterialTheme.colorScheme.background.red < 0.2f
  var expandedText by remember(msg.messageId) { mutableStateOf(false) }
  var showMenu by remember(msg.messageId) { mutableStateOf(false) }
  val clipboard = LocalClipboardManager.current

  val dissolveAlpha = remember { Animatable(1f) }
  val dissolveScale = remember { Animatable(1f) }
  LaunchedEffect(isDissolving) {
    if (isDissolving) {
      dissolveScale.animateTo(1.08f, tween(180))
      dissolveAlpha.animateTo(0f, tween(250))
    }
  }

  val isStickerOrMedia = msg.messageType == "sticker" || msg.messageType == "gif" || msg.messageType == "clip"
  val longText = msg.plainText.length > 360 || msg.plainText.count { it == '\n' } > 6
  val shownText = if (!longText || expandedText) msg.plainText else msg.plainText.take(360) + "..."
  val formattedTime = remember(msg.createdAtMs) {
    if (msg.createdAtMs <= 0L) "" else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.createdAtMs))
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp)
      .alpha(dissolveAlpha.value)
      .scale(dissolveScale.value),
    horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
  ) {
    Box {
      val bubbleShape = if (isOwn) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 5.dp)
      } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 5.dp, bottomEnd = 18.dp)
      }

      val clickModifier = Modifier
        .widthIn(max = 420.dp)
        .combinedClickable(
          onClick = {
            if (msg.messageType == "voice" || msg.messageType == "voice_once") onToggleVoice()
          },
          onLongClick = { showMenu = true },
        )
        .pointerInput(msg.messageId) {
          awaitPointerEventScope {
            while (true) {
              val ev = awaitPointerEvent()
              if (ev.buttons.isSecondaryPressed) {
                showMenu = true
              }
            }
          }
        }

      if (isStickerOrMedia) {
        Column(
          modifier = clickModifier.padding(4.dp),
          horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start,
        ) {
          DesktopAnimatedGiflyMedia(
            apiClient = apiClient,
            url = msg.mediaUrl,
            contentDescription = msg.plainText.ifBlank { msg.messageType },
            modifier = Modifier.size(if (msg.messageType == "sticker") 156.dp else 210.dp),
          )
          Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.45f),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(formattedTime, color = Color.White, fontSize = 10.sp)
              if (isOwn) DesktopDeliveryStatusIcon(msg.deliveryStatus, peerAvatarUrl, apiClient)
            }
          }
        }
      } else {
        val incomingBg = if (isDark) Color(0xFF1E222A) else Color.White
        val bubbleBrush = if (isOwn) {
          Brush.linearGradient(bubbleGradient)
        } else {
          SolidColor(incomingBg)
        }
        val textColor = if (isOwn) Color.White else if (isDark) Color(0xFFF3F4F6) else Color(0xFF141619)
        val metaColor = if (isOwn) Color.White.copy(alpha = 0.78f) else Color(0xFF8B929E)

        Box(
          modifier = clickModifier
            .clip(bubbleShape)
            .background(bubbleBrush)
            .then(
              if (!isOwn && !isDark) Modifier.border(0.8.dp, Color(0xFFE5E7EB), bubbleShape) else Modifier,
            )
            .padding(horizontal = 13.dp, vertical = 9.dp),
        ) {
          Column {
            if (msg.forwardedFromUserId.isNotBlank()) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DesktopChatVectorIcon(DesktopChatSvg.FORWARD, metaColor, 12.dp)
                Text("Forwarded", color = metaColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
              }
              Spacer(modifier = Modifier.height(3.dp))
            }

            if (msg.messageType == "voice" || msg.messageType == "voice_once") {
              val isViewOnce = msg.messageType == "voice_once"
              val sec = (msg.voiceDurationMs / 1000L).coerceAtLeast(1L)
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Surface(
                  shape = CircleShape,
                  color = if (isOwn) Color.White.copy(alpha = 0.22f) else bubbleGradient.first().copy(alpha = 0.16f),
                  modifier = Modifier.size(36.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    DesktopChatVectorIcon(
                      kind = when {
                        isViewOnce -> DesktopChatSvg.EYE_ONCE
                        isPlayingVoice -> DesktopChatSvg.PAUSE
                        else -> DesktopChatSvg.PLAY
                      },
                      tint = if (isOwn) Color.White else bubbleGradient.first(),
                      size = 16.dp,
                    )
                  }
                }
                Column {
                  Text(
                    text = when {
                      isViewOnce && isOwn -> "View-once voice (${sec}s)"
                      isViewOnce -> "Tap to listen (${if (msg.viewOnceTtlSec > 0) "${msg.viewOnceTtlSec}s" else "once"})"
                      else -> "Voice note · ${sec}s"
                    },
                    color = textColor,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                  )
                  if (isViewOnce && isOwn) {
                    Text(
                      text = "Receiver only",
                      color = metaColor,
                      fontSize = 11.sp,
                    )
                  }
                }
              }
            } else if (msg.messageType == "call_log" || msg.messageType == "call_request") {
              val meta = DesktopCallMetaParser.parse(msg.plainText)
              val targetType = meta?.callType ?: DesktopCallType.AUDIO
              val isMissedOrDeclined = meta?.outcome in setOf("missed", "declined", "cancelled", "busy", "not_allowed")
              val isRequest = msg.messageType == "call_request" || meta?.outcome == "call_me_request"
              val headline = when {
                isRequest -> "Call me"
                meta?.outcome == "ended" -> if (targetType == DesktopCallType.VIDEO || meta.videoUpgraded) "Video call" else "Audio call"
                meta?.outcome == "declined" -> "Call declined"
                meta?.outcome == "busy" -> "Busy"
                meta?.outcome == "not_allowed" -> "Calls not allowed"
                else -> if (targetType == DesktopCallType.VIDEO) "Missed video call" else "Missed audio call"
              }
              val subtitle = when {
                isRequest -> meta?.text?.ifBlank { "I tried to call you — call me when you're free." }
                  ?: "I tried to call you — call me when you're free."
                meta?.outcome == "ended" && meta.durationSec > 0L ->
                  "Duration ${DesktopCallMetaParser.formatDuration(meta.durationSec)}"
                else -> meta?.text?.ifBlank { "I tried to call you" } ?: "I tried to call you"
              }
              val iconKind = when {
                isMissedOrDeclined && !isRequest -> DesktopChatSvg.MISSED_CALL
                targetType == DesktopCallType.VIDEO -> DesktopChatSvg.VIDEO_CAM
                else -> DesktopChatSvg.PHONE_CALL
              }
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Surface(
                    shape = CircleShape,
                    color = if (isOwn) {
                      Color.White.copy(alpha = 0.22f)
                    } else if (isMissedOrDeclined) {
                      DesktopCrimson.copy(alpha = 0.14f)
                    } else {
                      bubbleGradient.first().copy(alpha = 0.16f)
                    },
                    modifier = Modifier.size(38.dp),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      DesktopChatVectorIcon(
                        kind = iconKind,
                        tint = if (isOwn) Color.White else if (isMissedOrDeclined) DesktopCrimson else bubbleGradient.first(),
                        size = 18.dp,
                      )
                    }
                  }
                  Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                      text = headline,
                      color = textColor,
                      fontSize = 14.sp,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = subtitle,
                      color = metaColor,
                      fontSize = 12.5.sp,
                      lineHeight = 17.sp,
                    )
                  }
                }
                Surface(
                  onClick = { onCallBack(targetType) },
                  shape = CircleShape,
                  color = if (isOwn) Color.White.copy(alpha = 0.20f) else bubbleGradient.first().copy(alpha = 0.14f),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    DesktopChatVectorIcon(
                      kind = if (targetType == DesktopCallType.VIDEO) DesktopChatSvg.VIDEO_CAM else DesktopChatSvg.PHONE_CALL,
                      tint = if (isOwn) Color.White else bubbleGradient.first(),
                      size = 14.dp,
                    )
                    Text(
                      text = if (isRequest) "Call now" else "Call back",
                      color = if (isOwn) Color.White else bubbleGradient.first(),
                      fontSize = 12.5.sp,
                      fontWeight = FontWeight.SemiBold,
                    )
                  }
                }
              }
            } else if (msg.messageType == "star_gift") {
              val meta = remember(msg.plainText) { DesktopStarsCatalog.parseGiftMeta(msg.plainText) }
              val starsCount = meta?.stars ?: 5L
              val tier = DesktopStarsCatalog.animationTier(starsCount)
              val emblemSize = when (tier) {
                5 -> 86.dp
                4 -> 76.dp
                3 -> 68.dp
                2 -> 60.dp
                else -> 52.dp
              }
              Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                DesktopAnimated3DStarEmblem(
                  starsAmount = starsCount,
                  baseSize = emblemSize,
                  showTelefamWordmark = true,
                  isDarkTheme = isDark || isOwn,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = if (isOwn) {
                    "You sent ${DesktopStarsCatalog.formatCount(starsCount)} Stars"
                  } else {
                    "Received ${DesktopStarsCatalog.formatCount(starsCount)} Stars"
                  },
                  color = textColor,
                  fontSize = (13.5 + tier * 0.8).sp,
                  fontWeight = FontWeight.ExtraBold,
                )
              }
            } else {
              DesktopSmartInteractiveText(
                text = shownText,
                source = "chat",
                color = textColor,
                linkColor = if (isOwn) Color(0xFFFFD6DF) else bubbleGradient.first(),
                otpHighlightBg = if (isOwn) Color.White.copy(alpha = 0.22f) else bubbleGradient.first().copy(alpha = 0.14f),
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontSize = 14.5.sp,
                  lineHeight = 20.sp,
                ),
              )
              if (longText) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = if (expandedText) "Read less" else "Read more",
                  color = if (isOwn) Color.White else bubbleGradient.first(),
                  fontSize = 12.5.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.clickable { expandedText = !expandedText },
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              DesktopLinkPreviewStack(
                text = msg.plainText,
                source = "chat",
                isDarkBubble = isOwn,
              )
            }

            Spacer(modifier = Modifier.height(3.dp))
            Row(
              modifier = Modifier.align(Alignment.End),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              if (msg.editedAtMs > 0L) {
                Text("edited", color = metaColor, fontSize = 10.sp)
              }
              Text(formattedTime, color = metaColor, fontSize = 10.5.sp)
              if (isOwn) {
                DesktopDeliveryStatusIcon(msg.deliveryStatus, peerAvatarUrl, apiClient)
              }
            }
          }
        }
      }

      if (isDissolving) {
        Canvas(modifier = Modifier.matchParentSize()) {
          val pts = listOf(0.15f to 0.25f, 0.42f to 0.15f, 0.72f to 0.30f, 0.28f to 0.68f, 0.85f to 0.72f)
          pts.forEach { (px, py) ->
            drawCircle(Color(0xFFFFD54F), radius = 3.5f, center = Offset(size.width * px, size.height * py))
          }
        }
      }

      DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
        if (canCopy && msg.plainText.isNotBlank() && msg.messageType == "text") {
          DropdownMenuItem(
            text = { Text("Copy") },
            leadingIcon = { DesktopChatVectorIcon(DesktopChatSvg.COPY, MaterialTheme.colorScheme.onSurface, 16.dp) },
            onClick = {
              showMenu = false
              clipboard.setText(AnnotatedString(msg.plainText))
              onCopy(msg.plainText)
            },
          )
        }
        if (canForward && msg.messageType != "voice_once") {
          DropdownMenuItem(
            text = { Text("Forward") },
            leadingIcon = { DesktopChatVectorIcon(DesktopChatSvg.FORWARD, MaterialTheme.colorScheme.onSurface, 16.dp) },
            onClick = {
              showMenu = false
              onForward()
            },
          )
        }
        if (isOwn && msg.messageType == "text") {
          DropdownMenuItem(
            text = { Text("Edit") },
            leadingIcon = { DesktopChatVectorIcon(DesktopChatSvg.EDIT, MaterialTheme.colorScheme.onSurface, 16.dp) },
            onClick = {
              showMenu = false
              onEdit()
            },
          )
        }
        if (isOwn) {
          DropdownMenuItem(
            text = { Text("Unsend", color = DesktopCrimson) },
            leadingIcon = { DesktopChatVectorIcon(DesktopChatSvg.TRASH, DesktopCrimson, 16.dp) },
            onClick = {
              showMenu = false
              onUnsend()
            },
          )
        }
        DropdownMenuItem(
          text = { Text("Delete for me") },
          leadingIcon = { DesktopChatVectorIcon(DesktopChatSvg.TRASH, MaterialTheme.colorScheme.onSurfaceVariant, 16.dp) },
          onClick = {
            showMenu = false
            onDeleteForMe()
          },
        )
      }
    }
  }
}

@Composable
fun DesktopDeliveryStatusIcon(
  status: String,
  peerAvatarUrl: String,
  apiClient: DesktopApiClient,
) {
  when (status.lowercase()) {
    "sending" -> DesktopChatVectorIcon(DesktopChatSvg.CLOCK_SENDING, Color.White.copy(alpha = 0.85f), 12.dp)
    "sent" -> DesktopChatVectorIcon(DesktopChatSvg.CHECK_SINGLE, Color.White.copy(alpha = 0.9f), 12.dp)
    "delivered" -> DesktopChatVectorIcon(DesktopChatSvg.CHECK_DOUBLE, Color(0xFFFF4D5A), 13.dp)
    "seen" -> {
      if (peerAvatarUrl.isNotBlank()) {
        DesktopChatAvatar(peerAvatarUrl, "S", apiClient, size = 13.dp)
      } else {
        DesktopChatVectorIcon(DesktopChatSvg.CHECK_DOUBLE, Color(0xFF4DD0E1), 13.dp)
      }
    }
    else -> DesktopChatVectorIcon(DesktopChatSvg.CHECK_SINGLE, Color.White.copy(alpha = 0.9f), 12.dp)
  }
}

@Composable
fun DesktopChatAvatar(
  avatarUrl: String,
  fallbackName: String,
  apiClient: DesktopApiClient,
  size: Dp = 42.dp,
) {
  var bitmap by remember(avatarUrl) { mutableStateOf<ImageBitmap?>(null) }
  LaunchedEffect(avatarUrl) {
    if (avatarUrl.isBlank()) return@LaunchedEffect
    val bytes = DesktopMediaBytesCache.bytes[avatarUrl] ?: withContext(Dispatchers.IO) {
      apiClient.downloadMediaBytes(avatarUrl)?.also { DesktopMediaBytesCache.bytes[avatarUrl] = it }
    }
    if (bytes != null) {
      bitmap = runCatching { SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
    }
  }
  Box(
    modifier = Modifier
      .size(size)
      .clip(CircleShape)
      .background(DesktopCrimson.copy(alpha = 0.16f)),
    contentAlignment = Alignment.Center,
  ) {
    if (bitmap != null) {
      Image(
        bitmap = bitmap!!,
        contentDescription = fallbackName,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      val initial = fallbackName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "T"
      Text(initial, color = DesktopCrimson, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp)
    }
  }
}

@Composable
fun DesktopGiflyPickerPanel(
  controller: DesktopChatController,
  apiClient: DesktopApiClient,
  accentColor: Color,
  onSendItem: (DesktopGiflyMediaItem, DesktopGiflyTab) -> Unit,
  onClose: () -> Unit,
) {
  var activeTab by remember { mutableStateOf(DesktopGiflyTab.STICKERS) }
  var query by remember { mutableStateOf("") }
  var page by remember { mutableStateOf(1) }
  var hasNext by remember { mutableStateOf(false) }
  var loading by remember { mutableStateOf(true) }
  var previewItem by remember { mutableStateOf<DesktopGiflyMediaItem?>(null) }
  val items = remember { mutableStateListOf<DesktopGiflyMediaItem>() }

  LaunchedEffect(activeTab, query) {
    loading = true
    page = 1
    previewItem = null
    delay(160L)
    val (res, more) = controller.searchGifly(activeTab, query, 1)
    items.clear()
    items.addAll(res)
    hasNext = more
    loading = false
  }

  Surface(
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
    modifier = Modifier.fillMaxWidth().height(340.dp),
  ) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DesktopGiflyTab.entries.forEach { tab ->
          val selected = tab == activeTab
          Surface(
            onClick = { activeTab = tab },
            shape = RoundedCornerShape(16.dp),
            color = if (selected) accentColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          ) {
            Text(
              text = tab.label,
              color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
              fontSize = 12.5.sp,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
          }
        }
        Spacer(modifier = Modifier.weight(1f))
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
          modifier = Modifier.width(200.dp).height(34.dp),
        ) {
          Box(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
              Text("Search ${activeTab.label.lowercase()}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            BasicTextField(
              value = query,
              onValueChange = { query = it.take(40) },
              singleLine = true,
              textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
              cursorBrush = SolidColor(accentColor),
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
        Surface(onClick = onClose, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(30.dp)) {
          Box(contentAlignment = Alignment.Center) {
            DesktopChatVectorIcon(DesktopChatSvg.CLOSE, MaterialTheme.colorScheme.onSurface, 14.dp)
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      val sel = previewItem
      if (sel != null) {
        Row(
          modifier = Modifier.fillMaxSize(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            DesktopAnimatedGiflyMedia(
              apiClient = apiClient,
              url = sel.bestAnimatedUrl,
              contentDescription = sel.title,
              modifier = Modifier.size(160.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              OutlinedButton(onClick = { previewItem = null }) {
                Text("Back")
              }
              Button(
                onClick = { onSendItem(sel, activeTab) },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
              ) {
                Text("Send", color = Color.White)
              }
            }
          }
        }
      } else if (loading) {
        val shimmer = rememberInfiniteTransition()
        val alpha by shimmer.animateFloat(
          initialValue = 0.25f,
          targetValue = 0.6f,
          animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        )
        LazyVerticalGrid(
          columns = GridCells.Adaptive(84.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxSize(),
        ) {
          items(12) {
            Box(
              modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.22f)),
            )
          }
        }
      } else {
        LazyVerticalGrid(
          columns = GridCells.Adaptive(84.dp),
          contentPadding = PaddingValues(bottom = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          items(items, key = { "${it.id}_${it.slug}" }) { item ->
            Box(
              modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable { previewItem = item }
                .padding(4.dp),
              contentAlignment = Alignment.Center,
            ) {
              DesktopAnimatedGiflyMedia(
                apiClient = apiClient,
                url = item.bestAnimatedUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
              )
            }
          }
          if (hasNext) {
            item {
              LaunchedEffect(page) {
                val nextP = page + 1
                val (moreItems, more) = controller.searchGifly(activeTab, query, nextP)
                items.addAll(moreItems)
                page = nextP
                hasNext = more
              }
              Box(modifier = Modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
                Text("...", color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }
    }
  }
}
