package telefam.company.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telefam.company.app.R
import telefam.company.ui.theme.LocalTelefamDarkTheme
import telefam.company.ui.theme.TelefamBlushWave
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamCrimsonBright
import telefam.company.ui.theme.TelefamCrimsonDeep
import telefam.company.ui.theme.TelefamDarkBlush
import telefam.company.ui.theme.ThemeMode

@Composable
fun TelefamBackgroundWaves(modifier: Modifier = Modifier) {
  val isDark = LocalTelefamDarkTheme.current
  val bg = MaterialTheme.colorScheme.background
  val blush = if (isDark) TelefamDarkBlush else TelefamBlushWave

  Canvas(
    modifier = modifier
      .fillMaxSize()
      .background(bg),
  ) {
    val w = size.width
    val h = size.height

    val topRightPath = Path().apply {
      moveTo(w * 0.62f, 0f)
      cubicTo(w * 0.72f, h * 0.045f, w * 0.86f, h * 0.03f, w, h * 0.11f)
      lineTo(w, 0f)
      close()
    }
    drawPath(path = topRightPath, color = blush)

    val bottomBlushPath = Path().apply {
      moveTo(0f, h * 0.845f)
      cubicTo(w * 0.28f, h * 0.79f, w * 0.66f, h * 0.90f, w, h * 0.835f)
      lineTo(w, h)
      lineTo(0f, h)
      close()
    }
    drawPath(path = bottomBlushPath, color = blush.copy(alpha = 0.8f))

    val bottomCrimsonPath = Path().apply {
      moveTo(0f, h * 0.865f)
      cubicTo(w * 0.25f, h * 0.815f, w * 0.62f, h * 0.925f, w, h * 0.855f)
      lineTo(w, h)
      lineTo(0f, h)
      close()
    }
    drawPath(
      path = bottomCrimsonPath,
      brush = Brush.verticalGradient(
        colors = listOf(TelefamCrimson, TelefamCrimsonDeep),
        startY = h * 0.815f,
        endY = h,
      ),
    )
  }
}

@Composable
fun TelefamLogoBadge(
  modifier: Modifier = Modifier,
  size: Dp = 74.dp,
  animated: Boolean = true,
  contentDescriptionText: String = "Telefam logo",
) {
  var appeared by remember { mutableStateOf(!animated) }
  LaunchedEffect(Unit) { appeared = true }

  val entryScale by animateFloatAsState(
    targetValue = if (appeared) 1f else 0.75f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    label = "logo_entry_scale",
  )

  val infiniteTransition = rememberInfiniteTransition(label = "plane_float")
  val floatOffset by infiniteTransition.animateFloat(
    initialValue = -1.5f,
    targetValue = 1.5f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "plane_float_y",
  )

  Box(
    modifier = modifier
      .size(size)
      .scale(entryScale)
      .semantics { contentDescription = contentDescriptionText }
      .shadow(
        elevation = 12.dp,
        shape = CircleShape,
        ambientColor = TelefamCrimson.copy(alpha = 0.35f),
        spotColor = TelefamCrimson.copy(alpha = 0.35f),
      )
      .background(
        brush = Brush.linearGradient(
          colors = listOf(TelefamCrimsonBright, TelefamCrimson, TelefamCrimsonDeep),
        ),
        shape = CircleShape,
      ),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(
      modifier = Modifier
        .size(size * 0.54f)
        .offset(y = if (animated) floatOffset.dp else 0.dp),
    ) {
      val cw = this.size.width
      val ch = this.size.height

      val outerWing = Path().apply {
        moveTo(cw * 0.92f, ch * 0.12f)
        lineTo(cw * 0.06f, ch * 0.46f)
        lineTo(cw * 0.38f, ch * 0.60f)
        lineTo(cw * 0.86f, ch * 0.20f)
        lineTo(cw * 0.46f, ch * 0.66f)
        lineTo(cw * 0.72f, ch * 0.90f)
        close()
      }
      drawPath(path = outerWing, color = Color.White)

      val tailFold = Path().apply {
        moveTo(cw * 0.38f, ch * 0.60f)
        lineTo(cw * 0.38f, ch * 0.86f)
        lineTo(cw * 0.52f, ch * 0.72f)
        close()
      }
      drawPath(path = tailFold, color = Color(0xFFF3F4F6))
    }
  }
}

@Composable
fun TelefamAnimatedSplashOverlay(
  visible: Boolean,
) {
  val alpha by animateFloatAsState(
    targetValue = if (visible) 1f else 0f,
    animationSpec = tween(durationMillis = 280),
    label = "splash_alpha",
  )
  if (!visible && alpha <= 0.01f) return

  Box(
    modifier = Modifier
      .fillMaxSize()
      .alpha(alpha)
      .background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.Center,
  ) {
    TelefamLogoBadge(size = 92.dp, animated = true)
  }
}

@Composable
fun CompactThemeToggleChip(
  themeMode: ThemeMode,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val icon = when (themeMode) {
    ThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
    ThemeMode.LIGHT -> Icons.Outlined.LightMode
    ThemeMode.DARK -> Icons.Outlined.DarkMode
  }
  val label = when (themeMode) {
    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
    ThemeMode.DARK -> stringResource(R.string.theme_dark)
  }

  Surface(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onToggle)
      .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
      .testTag("theme_toggle_chip"),
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
    shape = RoundedCornerShape(16.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = stringResource(R.string.cd_theme_toggle),
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(15.dp),
      )
      Spacer(modifier = Modifier.width(5.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
fun GoogleGLogo(
  modifier: Modifier = Modifier,
  size: Dp = 20.dp,
) {
  Canvas(modifier = modifier.size(size)) {
    val stroke = this.size.width * 0.19f
    val inset = stroke / 2f
    val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
    val topLeft = Offset(inset, inset)

    drawArc(
      color = Color(0xFFEA4335),
      startAngle = -145f,
      sweepAngle = 105f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt, join = StrokeJoin.Miter),
    )
    drawArc(
      color = Color(0xFFFBBC05),
      startAngle = 140f,
      sweepAngle = 75f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt),
    )
    drawArc(
      color = Color(0xFF34A853),
      startAngle = 42f,
      sweepAngle = 98f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt),
    )
    drawArc(
      color = Color(0xFF4285F4),
      startAngle = -5f,
      sweepAngle = 52f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt),
    )
    drawLine(
      color = Color(0xFF4285F4),
      start = Offset(this.size.width * 0.50f, this.size.height * 0.50f),
      end = Offset(this.size.width * 0.92f, this.size.height * 0.50f),
      strokeWidth = stroke,
      cap = StrokeCap.Butt,
    )
  }
}
