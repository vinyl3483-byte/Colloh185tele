package telefam.company.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import telefam.company.stars.TelefamStarsCatalog
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamCrimsonBright
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 3D perspective-spinning Golden Star emblem with animated orbital sparkles and halo rays.
 * Size, sparkle density, and orbital rings scale organically with [starsAmount].
 */
@Composable
fun TelefamAnimated3DStarEmblem(
  starsAmount: Long = 100L,
  baseSize: Dp = 64.dp,
  showTelefamWordmark: Boolean = true,
  isDarkTheme: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val tier = TelefamStarsCatalog.animationTierForStars(starsAmount)
  val scaleBoost = when (tier) {
    5 -> 1.48f
    4 -> 1.32f
    3 -> 1.18f
    2 -> 1.08f
    else -> 1.0f
  }
  val effectiveSize = (baseSize.value * scaleBoost).dp
  val sparkleCount = 4 + tier * 3

  val transition = rememberInfiniteTransition(label = "telefam_3d_star")
  val spinPhase by transition.animateFloat(
    initialValue = 0f,
    targetValue = (2.0 * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(
        durationMillis = (3400 - tier * 320).coerceAtLeast(1700),
        easing = LinearEasing,
      ),
      repeatMode = RepeatMode.Restart,
    ),
    label = "star_spin_phase",
  )
  val pulse by transition.animateFloat(
    initialValue = 0.88f,
    targetValue = 1.12f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1250, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "star_pulse",
  )

  val wordRed = if (isDarkTheme) Color(0xFFEF4444) else TelefamCrimson

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = modifier,
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.size(effectiveSize),
    ) {
      Canvas(modifier = Modifier.size(effectiveSize)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = size.minDimension * 0.44f

        // 1. Warm golden volumetric glow behind the spinning star
        val glowRadius = maxR * (1.28f + tier * 0.14f) * pulse
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(
              Color(0xFFFFD54F).copy(alpha = 0.34f + tier * 0.05f),
              Color(0xFFFF8F00).copy(alpha = 0.14f),
              Color.Transparent,
            ),
            center = Offset(cx, cy),
            radius = glowRadius,
          ),
          radius = glowRadius,
          center = Offset(cx, cy),
        )

        // Higher amounts unlock orbital aura rings
        if (tier >= 3) {
          drawCircle(
            color = Color(0xFFFFCA28).copy(alpha = 0.25f * pulse),
            radius = maxR * 1.12f,
            center = Offset(cx, cy),
            style = Stroke(width = (1.3f + tier * 0.25f)),
          )
        }
        if (tier >= 5) {
          drawCircle(
            color = TelefamCrimsonBright.copy(alpha = 0.22f * pulse),
            radius = maxR * 1.32f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.8f),
          )
        }

        // 2. Animated 4-point twinkling sparkles orbiting in 3D phase
        for (idx in 0 until sparkleCount) {
          val baseAngle = (idx.toFloat() / sparkleCount.toFloat()) * (2.0 * PI).toFloat()
          val angle = baseAngle + spinPhase * (if (idx % 2 == 0) 0.45f else -0.35f)
          val dist = maxR * (0.86f + 0.28f * ((idx % 3) / 2f) + 0.08f * sin(spinPhase * 2f + idx))
          val sx = cx + cos(angle) * dist
          val sy = cy + sin(angle) * dist * 0.84f
          val twinkle = ((sin(spinPhase * 3f + idx * 1.3f) + 1f) * 0.5f).coerceIn(0.25f, 1f)
          val spR = (size.minDimension * (0.045f + tier * 0.008f)) * twinkle
          val sparkleColor = if (idx % 4 == 0 && tier >= 3) Color(0xFFFF6B6B) else Color(0xFFFFE57F)

          val spPath = Path().apply {
            moveTo(sx, sy - spR)
            quadraticTo(sx, sy, sx + spR, sy)
            quadraticTo(sx, sy, sx, sy + spR)
            quadraticTo(sx, sy, sx - spR, sy)
            quadraticTo(sx, sy, sx, sy - spR)
            close()
          }
          drawPath(spPath, color = sparkleColor.copy(alpha = twinkle * 0.92f))
        }

        // 3. True 3D perspective-projected 5-point Star
        val cosYaw = cos(spinPhase)
        val sinYaw = sin(spinPhase)
        val perspX = if (abs(cosYaw) < 0.16f) {
          if (cosYaw >= 0f) 0.16f else -0.16f
        } else {
          cosYaw
        }
        val depthThickness = size.minDimension * 0.085f
        val extrudeX = -sinYaw * depthThickness
        val extrudeY = size.minDimension * 0.018f

        val outerR = maxR * 0.80f
        val innerR = maxR * 0.37f
        val bobY = sin(spinPhase * 2f) * (size.minDimension * 0.025f)

        fun buildStarPath(dx: Float, dy: Float): Path {
          val p = Path()
          for (i in 0 until 10) {
            val a = -PI / 2.0 + i * (PI / 5.0)
            val r = if (i % 2 == 0) outerR else innerR
            val px = cx + dx + (r * cos(a)).toFloat() * perspX
            val py = cy + dy + bobY + (r * sin(a)).toFloat()
            if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
          }
          p.close()
          return p
        }

        // Back 3D extruded bevel face (deep amber bronze)
        val backPath = buildStarPath(extrudeX, extrudeY)
        drawPath(
          path = backPath,
          brush = Brush.linearGradient(
            colors = listOf(Color(0xFFB45309), Color(0xFF78350F)),
          ),
        )

        // Front 3D faceted face (metallic gold gradient shifting subtly with angle)
        val frontPath = buildStarPath(0f, 0f)
        val shimmerOffset = (sinYaw + 1f) * 0.5f
        val lightGold = Color(0xFFFFF59D)
        val midGold = Color(0xFFFFCA28)
        val deepGold = Color(0xFFF59E0B)
        drawPath(
          path = frontPath,
          brush = Brush.linearGradient(
            colors = if (shimmerOffset > 0.5f) {
              listOf(lightGold, midGold, deepGold)
            } else {
              listOf(midGold, lightGold, deepGold)
            },
            start = Offset(cx - outerR, cy - outerR),
            end = Offset(cx + outerR, cy + outerR),
          ),
        )

        // Specular highlight facets toward the center apex
        val facetPath = Path().apply {
          moveTo(cx, cy + bobY - outerR * 0.85f)
          lineTo(cx + innerR * 0.65f * perspX, cy + bobY)
          lineTo(cx, cy + bobY)
          close()
        }
        drawPath(
          path = facetPath,
          color = Color.White.copy(alpha = 0.28f),
        )
      }
    }

    if (showTelefamWordmark) {
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "Telefam",
        color = wordRed,
        fontSize = when {
          tier >= 4 -> 14.sp
          tier >= 2 -> 12.5.sp
          else -> 11.5.sp
        },
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.4.sp,
      )
    }
  }
}

/**
 * Clean circular golden badge Star icon matching the Send Stars & Buy Stars lists in Screenshots 2 & 3.
 */
@Composable
fun TelefamGoldStarBadgeIcon(
  size: Dp = 36.dp,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier.size(size)) {
    val cx = this.size.width / 2f
    val cy = this.size.height / 2f
    val circleR = this.size.minDimension / 2f

    drawCircle(
      color = Color(0xFFFFF3D1),
      radius = circleR,
      center = Offset(cx, cy),
    )

    val outerR = this.size.minDimension * 0.34f
    val innerR = this.size.minDimension * 0.16f
    val path = Path()
    for (i in 0 until 10) {
      val a = -PI / 2.0 + i * (PI / 5.0)
      val r = if (i % 2 == 0) outerR else innerR
      val x = cx + (r * cos(a)).toFloat()
      val y = cy + (r * sin(a)).toFloat() + this.size.minDimension * 0.015f
      if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(
      path = path,
      brush = Brush.linearGradient(
        colors = listOf(Color(0xFFFFD54F), Color(0xFFF59E0B)),
      ),
    )
  }
}

/**
 * Custom SVG-style vector Paystack logo (stacked cyan-blue bars matching Screenshot 3).
 */
@Composable
fun PaystackVectorLogo(
  size: Dp = 28.dp,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    val barH = h * 0.15f
    val radius = CornerRadius(barH * 0.5f, barH * 0.5f)
    val left = w * 0.12f
    val color = Color(0xFF00C3F7)

    drawRoundRect(color, Offset(left, h * 0.16f), Size(w * 0.76f, barH), radius)
    drawRoundRect(color, Offset(left, h * 0.37f), Size(w * 0.76f, barH), radius)
    drawRoundRect(color, Offset(left, h * 0.58f), Size(w * 0.52f, barH), radius)
    drawRoundRect(color, Offset(left, h * 0.78f), Size(w * 0.30f, barH), radius)
  }
}

/**
 * Custom SVG-style vector PayPal emblem (overlapping navy + azure Ps matching Screenshot 3).
 */
@Composable
fun PayPalVectorLogo(
  size: Dp = 28.dp,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height

    val backP = Path().apply {
      moveTo(w * 0.24f, h * 0.14f)
      lineTo(w * 0.56f, h * 0.14f)
      cubicTo(w * 0.78f, h * 0.14f, w * 0.80f, h * 0.52f, w * 0.54f, h * 0.54f)
      lineTo(w * 0.38f, h * 0.54f)
      lineTo(w * 0.33f, h * 0.82f)
      lineTo(w * 0.15f, h * 0.82f)
      close()
    }
    drawPath(backP, color = Color(0xFF003087))

    val frontP = Path().apply {
      moveTo(w * 0.36f, h * 0.28f)
      lineTo(w * 0.66f, h * 0.28f)
      cubicTo(w * 0.86f, h * 0.28f, w * 0.86f, h * 0.64f, w * 0.60f, h * 0.66f)
      lineTo(w * 0.46f, h * 0.66f)
      lineTo(w * 0.42f, h * 0.90f)
      lineTo(w * 0.26f, h * 0.90f)
      close()
    }
    drawPath(frontP, color = Color(0xFF009CDE))
  }
}
