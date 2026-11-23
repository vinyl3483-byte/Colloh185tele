package telefam.company.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class DesktopProfileMenuKind {
  DASHBOARD,
  STARS,
  SUBSCRIPTION,
  MONETIZATION,
  WALLET,
  LINKED_DEVICE,
  CREATE_CAMPAIGN,
  GET_VERIFIED,
}

private data class DesktopProfileMenuEntry(
  val kind: DesktopProfileMenuKind,
  val title: String,
  val subtitle: String,
)

private val DesktopProfileMenuEntries = listOf(
  DesktopProfileMenuEntry(
    kind = DesktopProfileMenuKind.DASHBOARD,
    title = "Dashboard",
    subtitle = "View your performance, analytics and insights",
  ),
  DesktopProfileMenuEntry(
    kind = DesktopProfileMenuKind.STARS,
    title = "Stars",
    subtitle = "Get support from your fans",
  ),
  DesktopProfileMenuEntry(
    kind = DesktopProfileMenuKind.SUBSCRIPTION,
    title = "Subscription",
    subtitle = "Manage your subscriptions and exclusive content",
  ),
  DesktopProfileMenuEntry(
    kind = DesktopProfileMenuKind.MONETIZATION,
    title = "Monetization",
    subtitle = "Earn from your content and creator programs",
  ),
  DesktopProfileMenuEntry(
    kind = DesktopProfileMenuKind.WALLET,
    title = "Wallet",
    subtitle = "View your balance and transaction history",
  ),
  DesktopProfileMenuEntry(
    kind = DesktopProfileMenuKind.LINKED_DEVICE,
    title = "Linked device",
    subtitle = "Manage your linked devices",
  ),
  DesktopProfileMenuEntry(
    kind = DesktopProfileMenuKind.CREATE_CAMPAIGN,
    title = "Create campaign",
    subtitle = "Collaborate with brands and grow your reach",
  ),
  DesktopProfileMenuEntry(
    kind = DesktopProfileMenuKind.GET_VERIFIED,
    title = "Get Verified",
    subtitle = "Apply for a verified badge and build more trust with your audience.",
  ),
)

@Composable
fun DesktopProfileMenuScreen(
  isDark: Boolean,
  onBack: () -> Unit,
  onOpenStars: () -> Unit = {},
  onOpenGetVerified: () -> Unit = {},
  onFeatureComingSoon: (String) -> Unit,
) {
  val accentRed = DesktopCrimson
  val pageBg = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF4F6FA)
  val cardSurface = if (isDark) Color(0xFF171A21) else Color.White
  val cardBorder = if (isDark) Color(0xFF282D38) else Color(0xFFEAECEF)
  val titleColor = if (isDark) Color(0xFFF3F5F8) else Color(0xFF141821)
  val subtitleColor = if (isDark) Color(0xFF9EA6B6) else Color(0xFF6B7280)
  val chevronColor = if (isDark) Color(0xFF8C94A5) else Color(0xFF64748B)

  Surface(modifier = Modifier.fillMaxSize(), color = pageBg) {
    Column(modifier = Modifier.fillMaxSize()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = titleColor,
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Menu",
          fontSize = 19.sp,
          fontWeight = FontWeight.Bold,
          color = titleColor,
        )
      }

      Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
          modifier = Modifier.fillMaxSize().widthIn(max = 680.dp),
          contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(DesktopProfileMenuEntries, key = { it.kind.name }) { entry ->
            val isVerified = entry.kind == DesktopProfileMenuKind.GET_VERIFIED
            Surface(
              onClick = {
                when (entry.kind) {
                  DesktopProfileMenuKind.STARS -> onOpenStars()
                  DesktopProfileMenuKind.GET_VERIFIED -> onOpenGetVerified()
                  else -> onFeatureComingSoon(entry.title)
                }
              },
              shape = RoundedCornerShape(16.dp),
              color = cardSurface,
              border = BorderStroke(1.dp, cardBorder),
              shadowElevation = if (isDark) 0.dp else 1.dp,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Box(
                  modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentRed),
                  contentAlignment = Alignment.Center,
                ) {
                  DesktopProfileMenuSvgIcon(
                    kind = entry.kind,
                    badgeBgColor = accentRed,
                    iconColor = Color.White,
                    size = 24.dp,
                  )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                  Text(
                    text = entry.title,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = entry.subtitle,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = subtitleColor,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                  )
                }

                Spacer(modifier = Modifier.width(10.dp))

                if (isVerified) {
                  Surface(
                    onClick = onOpenGetVerified,
                    shape = CircleShape,
                    color = accentRed,
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      DesktopProfileMenuSvgIcon(
                        kind = DesktopProfileMenuKind.GET_VERIFIED,
                        badgeBgColor = accentRed,
                        iconColor = Color.White,
                        size = 16.dp,
                      )
                      Text(
                        text = "Get Verified",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                      )
                      Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                      )
                    }
                  }
                } else {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = chevronColor,
                    modifier = Modifier.size(20.dp),
                  )
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
private fun DesktopProfileMenuSvgIcon(
  kind: DesktopProfileMenuKind,
  badgeBgColor: Color,
  iconColor: Color,
  size: Dp = 24.dp,
) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    when (kind) {
      DesktopProfileMenuKind.DASHBOARD -> {
        val barW = w * 0.21f
        val radius = CornerRadius(barW * 0.45f, barW * 0.45f)
        val gap = w * 0.085f
        val startX = (w - (barW * 3f + gap * 2f)) / 2f
        val baseY = h * 0.84f
        val h1 = h * 0.28f
        val h2 = h * 0.48f
        val h3 = h * 0.68f
        drawRoundRect(iconColor, Offset(startX, baseY - h1), Size(barW, h1), radius)
        drawRoundRect(iconColor, Offset(startX + barW + gap, baseY - h2), Size(barW, h2), radius)
        drawRoundRect(iconColor, Offset(startX + (barW + gap) * 2f, baseY - h3), Size(barW, h3), radius)
      }

      DesktopProfileMenuKind.STARS -> {
        val path = Path()
        val cx = w / 2f
        val cy = h / 2f + h * 0.02f
        val outerR = w * 0.42f
        val innerR = w * 0.19f
        for (i in 0 until 10) {
          val angle = -PI / 2.0 + i * PI / 5.0
          val r = if (i % 2 == 0) outerR else innerR
          val x = cx + (r * cos(angle)).toFloat()
          val y = cy + (r * sin(angle)).toFloat()
          if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path = path, color = iconColor)
      }

      DesktopProfileMenuKind.SUBSCRIPTION -> {
        val path = Path().apply {
          moveTo(w * 0.14f, h * 0.70f)
          lineTo(w * 0.12f, h * 0.30f)
          lineTo(w * 0.33f, h * 0.48f)
          lineTo(w * 0.50f, h * 0.22f)
          lineTo(w * 0.67f, h * 0.48f)
          lineTo(w * 0.88f, h * 0.30f)
          lineTo(w * 0.86f, h * 0.70f)
          close()
        }
        drawPath(path = path, color = iconColor)
        drawRoundRect(iconColor, Offset(w * 0.14f, h * 0.75f), Size(w * 0.72f, h * 0.10f), CornerRadius(h * 0.05f, h * 0.05f))
      }

      DesktopProfileMenuKind.MONETIZATION -> {
        val cx = w * 0.50f
        val cy = h * 0.50f
        val r = w * 0.40f
        val coinPath = Path().apply { addOval(Rect(cx - r, cy - r, cx + r, cy + r)) }
        drawPath(coinPath, iconColor)
        val tail = Path().apply {
          moveTo(w * 0.18f, h * 0.74f)
          lineTo(w * 0.12f, h * 0.88f)
          lineTo(w * 0.30f, h * 0.82f)
          close()
        }
        drawPath(tail, iconColor)
        val strokeW = w * 0.085f
        val dollarPath = Path().apply {
          moveTo(w * 0.61f, h * 0.35f)
          cubicTo(w * 0.58f, h * 0.28f, w * 0.39f, h * 0.28f, w * 0.39f, h * 0.39f)
          cubicTo(w * 0.39f, h * 0.50f, w * 0.62f, h * 0.48f, w * 0.62f, h * 0.60f)
          cubicTo(w * 0.62f, h * 0.71f, w * 0.41f, h * 0.72f, w * 0.38f, h * 0.63f)
        }
        drawPath(dollarPath, badgeBgColor, style = Stroke(strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(badgeBgColor, Offset(cx, h * 0.24f), Offset(cx, h * 0.76f), strokeWidth = strokeW * 0.9f, cap = StrokeCap.Round)
      }

      DesktopProfileMenuKind.WALLET -> {
        drawRoundRect(iconColor, Offset(w * 0.12f, h * 0.22f), Size(w * 0.76f, h * 0.58f), CornerRadius(w * 0.12f, w * 0.12f))
        drawRoundRect(badgeBgColor, Offset(w * 0.20f, h * 0.28f), Size(w * 0.56f, h * 0.08f), CornerRadius(h * 0.04f, h * 0.04f))
        drawRoundRect(badgeBgColor, Offset(w * 0.54f, h * 0.44f), Size(w * 0.36f, h * 0.20f), CornerRadius(w * 0.10f, w * 0.10f))
        drawRoundRect(iconColor, Offset(w * 0.57f, h * 0.47f), Size(w * 0.31f, h * 0.14f), CornerRadius(w * 0.07f, w * 0.07f))
        drawCircle(badgeBgColor, w * 0.045f, Offset(w * 0.66f, h * 0.54f))
      }

      DesktopProfileMenuKind.LINKED_DEVICE -> {
        val strokeW = w * 0.08f
        drawRoundRect(iconColor, Offset(w * 0.10f, h * 0.16f), Size(w * 0.46f, h * 0.54f), CornerRadius(w * 0.08f, w * 0.08f), style = Stroke(strokeW))
        drawRoundRect(badgeBgColor, Offset(w * 0.42f, h * 0.30f), Size(w * 0.46f, h * 0.54f), CornerRadius(w * 0.08f, w * 0.08f))
        drawRoundRect(iconColor, Offset(w * 0.45f, h * 0.33f), Size(w * 0.40f, h * 0.48f), CornerRadius(w * 0.07f, w * 0.07f), style = Stroke(strokeW))
        drawLine(iconColor, Offset(w * 0.60f, h * 0.72f), Offset(w * 0.70f, h * 0.72f), strokeWidth = strokeW * 0.85f, cap = StrokeCap.Round)
      }

      DesktopProfileMenuKind.CREATE_CAMPAIGN -> {
        val horn = Path().apply {
          moveTo(w * 0.16f, h * 0.46f)
          lineTo(w * 0.56f, h * 0.25f)
          lineTo(w * 0.68f, h * 0.68f)
          lineTo(w * 0.20f, h * 0.60f)
          close()
        }
        drawPath(horn, iconColor)
        val handle = Path().apply {
          moveTo(w * 0.26f, h * 0.60f)
          lineTo(w * 0.33f, h * 0.82f)
          lineTo(w * 0.45f, h * 0.79f)
          lineTo(w * 0.38f, h * 0.62f)
          close()
        }
        drawPath(handle, iconColor)
        drawLine(iconColor, Offset(w * 0.74f, h * 0.38f), Offset(w * 0.86f, h * 0.32f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
        drawLine(iconColor, Offset(w * 0.76f, h * 0.52f), Offset(w * 0.88f, h * 0.54f), strokeWidth = w * 0.08f, cap = StrokeCap.Round)
      }

      DesktopProfileMenuKind.GET_VERIFIED -> {
        val cx = w / 2f
        val cy = h / 2f
        val seal = Path()
        val points = 16
        val outerR = w * 0.44f
        val innerR = w * 0.35f
        for (i in 0 until points) {
          val a = -PI / 2.0 + i * (2.0 * PI / points)
          val r = if (i % 2 == 0) outerR else innerR
          val x = cx + (r * cos(a)).toFloat()
          val y = cy + (r * sin(a)).toFloat()
          if (i == 0) seal.moveTo(x, y) else seal.lineTo(x, y)
        }
        seal.close()
        drawPath(seal, iconColor)
        val check = Path().apply {
          moveTo(w * 0.33f, h * 0.51f)
          lineTo(w * 0.45f, h * 0.63f)
          lineTo(w * 0.68f, h * 0.38f)
        }
        drawPath(check, badgeBgColor, style = Stroke(w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round))
      }
    }
  }
}
