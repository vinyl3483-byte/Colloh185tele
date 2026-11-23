package telefam.company.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import telefam.company.ui.theme.LocalTelefamDarkTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Theme-adaptive vector/SVG Telefam Verified badge icon.
 * Unlocked strictly when server-verified tier is "business", "government", or "blue".
 */
@Composable
fun TelefamVerifiedBadgeIcon(
  tier: String,
  size: Dp = 18.dp,
  isDarkTheme: Boolean = LocalTelefamDarkTheme.current,
  modifier: Modifier = Modifier,
) {
  val normalized = tier.trim().lowercase()
  if (normalized != "business" && normalized != "government" && normalized != "blue") return

  val description = when (normalized) {
    "business" -> "Verified business badge"
    "government" -> "Verified government badge"
    else -> "Verified blue badge"
  }

  val haloRing = if (isDarkTheme) Color(0x55FFFFFF) else Color(0x330F172A)

  Canvas(
    modifier = modifier
      .size(size)
      .semantics { contentDescription = description }
      .testTag("verified_badge_$normalized"),
  ) {
    val w = this.size.width
    val h = this.size.height
    val cx = w / 2f
    val cy = h / 2f

    // 16-point scalloped sunburst seal
    val sealPath = Path()
    val points = 24
    val outerR = w * 0.48f
    val innerR = w * 0.39f
    for (i in 0 until points) {
      val angle = -PI / 2.0 + i * (2.0 * PI / points)
      val r = if (i % 2 == 0) outerR else innerR
      val x = cx + (r * cos(angle)).toFloat()
      val y = cy + (r * sin(angle)).toFloat()
      if (i == 0) sealPath.moveTo(x, y) else sealPath.lineTo(x, y)
    }
    sealPath.close()

    when (normalized) {
      "business" -> {
        val fillBrush = if (isDarkTheme) {
          Brush.linearGradient(listOf(Color(0xFFFFE15A), Color(0xFFF5A600)))
        } else {
          Brush.linearGradient(listOf(Color(0xFFF9C42D), Color(0xFFDF8E00)))
        }
        drawPath(sealPath, brush = fillBrush)
        drawPath(sealPath, color = haloRing, style = Stroke(width = w * 0.03f))

        // Crisp inner white crown-shield check accent matching Business badge
        val innerMarkColor = if (isDarkTheme) Color(0xFF1A1405) else Color(0xFFFFFFFF)
        val check = Path().apply {
          moveTo(w * 0.31f, h * 0.51f)
          lineTo(w * 0.44f, h * 0.64f)
          lineTo(w * 0.70f, h * 0.37f)
        }
        drawPath(
          path = check,
          color = innerMarkColor,
          style = Stroke(width = w * 0.115f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
      }

      "government" -> {
        val fillBrush = if (isDarkTheme) {
          Brush.linearGradient(listOf(Color(0xFFFF383E), Color(0xFFD90E16)))
        } else {
          Brush.linearGradient(listOf(Color(0xFFE01119), Color(0xFFB3070C)))
        }
        drawPath(sealPath, brush = fillBrush)
        drawPath(sealPath, color = haloRing, style = Stroke(width = w * 0.03f))

        // Inner white shield outline with checkmark matching Government badge
        val shield = Path().apply {
          moveTo(w * 0.50f, h * 0.25f)
          lineTo(w * 0.72f, h * 0.34f)
          lineTo(w * 0.72f, h * 0.53f)
          cubicTo(w * 0.72f, h * 0.67f, w * 0.50f, h * 0.77f, w * 0.50f, h * 0.77f)
          cubicTo(w * 0.50f, h * 0.77f, w * 0.28f, h * 0.67f, w * 0.28f, h * 0.53f)
          lineTo(w * 0.28f, h * 0.34f)
          close()
        }
        drawPath(
          path = shield,
          color = Color.White,
          style = Stroke(width = w * 0.065f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        val check = Path().apply {
          moveTo(w * 0.39f, h * 0.51f)
          lineTo(w * 0.47f, h * 0.59f)
          lineTo(w * 0.62f, h * 0.43f)
        }
        drawPath(
          path = check,
          color = Color.White,
          style = Stroke(width = w * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
      }

      else -> {
        val fillBrush = if (isDarkTheme) {
          Brush.linearGradient(listOf(Color(0xFF47B5FF), Color(0xFF1683D8)))
        } else {
          Brush.linearGradient(listOf(Color(0xFF23A6FF), Color(0xFF0E7AD6)))
        }
        drawPath(sealPath, brush = fillBrush)
        drawPath(sealPath, color = haloRing, style = Stroke(width = w * 0.03f))

        val check = Path().apply {
          moveTo(w * 0.31f, h * 0.51f)
          lineTo(w * 0.44f, h * 0.64f)
          lineTo(w * 0.70f, h * 0.37f)
        }
        drawPath(
          path = check,
          color = Color.White,
          style = Stroke(width = w * 0.115f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
      }
    }
  }
}

/**
 * Vector SVG emblem for tier header / plan card.
 */
@Composable
fun VerificationBenefitVectorIcon(
  iconKey: String,
  tint: Color,
  size: Dp = 19.dp,
) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val stroke = Stroke(width = w * 0.10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (iconKey) {
      "badge" -> {
        val p = Path().apply {
          moveTo(w * 0.50f, h * 0.14f)
          lineTo(w * 0.84f, h * 0.28f)
          lineTo(w * 0.84f, h * 0.54f)
          cubicTo(w * 0.84f, h * 0.74f, w * 0.50f, h * 0.88f, w * 0.50f, h * 0.88f)
          cubicTo(w * 0.50f, h * 0.88f, w * 0.16f, h * 0.74f, w * 0.16f, h * 0.54f)
          lineTo(w * 0.16f, h * 0.28f)
          close()
        }
        drawPath(p, color = tint)
        val check = Path().apply {
          moveTo(w * 0.34f, h * 0.50f)
          lineTo(w * 0.46f, h * 0.62f)
          lineTo(w * 0.68f, h * 0.38f)
        }
        drawPath(check, color = Color.White, style = stroke)
      }
      "link" -> {
        drawRoundRect(
          color = tint,
          topLeft = Offset(w * 0.12f, h * 0.36f),
          size = Size(w * 0.44f, h * 0.28f),
          cornerRadius = CornerRadius(h * 0.14f, h * 0.14f),
          style = stroke,
        )
        drawRoundRect(
          color = tint,
          topLeft = Offset(w * 0.44f, h * 0.36f),
          size = Size(w * 0.44f, h * 0.28f),
          cornerRadius = CornerRadius(h * 0.14f, h * 0.14f),
          style = stroke,
        )
      }
      "group" -> {
        drawCircle(tint, radius = w * 0.14f, center = Offset(w * 0.38f, h * 0.34f))
        drawCircle(tint, radius = w * 0.11f, center = Offset(w * 0.68f, h * 0.38f))
        val body = Path().apply {
          moveTo(w * 0.14f, h * 0.82f)
          quadraticTo(w * 0.38f, h * 0.54f, w * 0.62f, h * 0.82f)
          close()
        }
        drawPath(body, color = tint)
        val body2 = Path().apply {
          moveTo(w * 0.54f, h * 0.82f)
          quadraticTo(w * 0.72f, h * 0.58f, w * 0.88f, h * 0.82f)
          close()
        }
        drawPath(body2, color = tint.copy(alpha = 0.82f))
      }
      "verified_doc" -> {
        drawRoundRect(
          color = tint,
          topLeft = Offset(w * 0.18f, h * 0.12f),
          size = Size(w * 0.64f, h * 0.76f),
          cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
        )
        val check = Path().apply {
          moveTo(w * 0.33f, h * 0.52f)
          lineTo(w * 0.45f, h * 0.64f)
          lineTo(w * 0.68f, h * 0.40f)
        }
        drawPath(check, color = Color.White, style = stroke)
      }
      "support" -> {
        val horn = Path().apply {
          moveTo(w * 0.16f, h * 0.48f)
          lineTo(w * 0.66f, h * 0.22f)
          lineTo(w * 0.66f, h * 0.74f)
          close()
        }
        drawPath(horn, color = tint)
        drawLine(tint, Offset(w * 0.74f, h * 0.34f), Offset(w * 0.88f, h * 0.28f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.74f, h * 0.50f), Offset(w * 0.88f, h * 0.50f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
      }
      "media" -> {
        drawRoundRect(
          color = tint,
          topLeft = Offset(w * 0.12f, h * 0.18f),
          size = Size(w * 0.76f, h * 0.64f),
          cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
          style = stroke,
        )
        val mtn = Path().apply {
          moveTo(w * 0.20f, h * 0.72f)
          lineTo(w * 0.42f, h * 0.46f)
          lineTo(w * 0.58f, h * 0.62f)
          lineTo(w * 0.70f, h * 0.50f)
          lineTo(w * 0.82f, h * 0.72f)
          close()
        }
        drawPath(mtn, color = tint)
      }
      "security" -> {
        val shield = Path().apply {
          moveTo(w * 0.50f, h * 0.12f)
          lineTo(w * 0.84f, h * 0.26f)
          lineTo(w * 0.84f, h * 0.54f)
          cubicTo(w * 0.84f, h * 0.76f, w * 0.50f, h * 0.88f, w * 0.50f, h * 0.88f)
          cubicTo(w * 0.50f, h * 0.88f, w * 0.16f, h * 0.76f, w * 0.16f, h * 0.54f)
          lineTo(w * 0.16f, h * 0.26f)
          close()
        }
        drawPath(shield, color = tint)
        drawCircle(Color.White, radius = w * 0.10f, center = Offset(w * 0.50f, h * 0.50f))
      }
      "messages" -> {
        drawRoundRect(
          color = tint,
          topLeft = Offset(w * 0.12f, h * 0.18f),
          size = Size(w * 0.76f, h * 0.54f),
          cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
        )
        drawLine(Color.White, Offset(w * 0.28f, h * 0.38f), Offset(w * 0.72f, h * 0.38f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(w * 0.28f, h * 0.54f), Offset(w * 0.58f, h * 0.54f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
      }
      "analytics" -> {
        val bW = w * 0.18f
        drawRoundRect(tint, topLeft = Offset(w * 0.16f, h * 0.54f), size = Size(bW, h * 0.30f), cornerRadius = CornerRadius(bW / 2f, bW / 2f))
        drawRoundRect(tint, topLeft = Offset(w * 0.42f, h * 0.36f), size = Size(bW, h * 0.48f), cornerRadius = CornerRadius(bW / 2f, bW / 2f))
        drawRoundRect(tint, topLeft = Offset(w * 0.68f, h * 0.18f), size = Size(bW, h * 0.66f), cornerRadius = CornerRadius(bW / 2f, bW / 2f))
      }
      "payments" -> {
        drawRoundRect(
          color = tint,
          topLeft = Offset(w * 0.12f, h * 0.22f),
          size = Size(w * 0.76f, h * 0.56f),
          cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
        )
        drawLine(Color.White, Offset(w * 0.16f, h * 0.40f), Offset(w * 0.84f, h * 0.40f), strokeWidth = w * 0.08f)
      }
      "stickers" -> {
        drawCircle(tint, radius = w * 0.38f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
        drawCircle(tint, radius = w * 0.06f, center = Offset(w * 0.38f, h * 0.42f))
        drawCircle(tint, radius = w * 0.06f, center = Offset(w * 0.62f, h * 0.42f))
        val smile = Path().apply {
          moveTo(w * 0.35f, h * 0.58f)
          quadraticTo(w * 0.50f, h * 0.70f, w * 0.65f, h * 0.58f)
        }
        drawPath(smile, color = tint, style = stroke)
      }
      "calls" -> {
        drawRoundRect(
          color = tint,
          topLeft = Offset(w * 0.12f, h * 0.28f),
          size = Size(w * 0.52f, h * 0.44f),
          cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
        )
        val lens = Path().apply {
          moveTo(w * 0.66f, h * 0.42f)
          lineTo(w * 0.88f, h * 0.28f)
          lineTo(w * 0.88f, h * 0.72f)
          lineTo(w * 0.66f, h * 0.58f)
          close()
        }
        drawPath(lens, color = tint)
      }
      else -> {
        val star = Path()
        val cx = w / 2f
        val cy = h / 2f
        val outerR = w * 0.42f
        val innerR = w * 0.19f
        for (i in 0 until 10) {
          val a = -PI / 2.0 + i * PI / 5.0
          val r = if (i % 2 == 0) outerR else innerR
          val x = cx + (r * cos(a)).toFloat()
          val y = cy + (r * sin(a)).toFloat()
          if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
        }
        star.close()
        drawPath(star, color = tint)
      }
    }
  }
}
