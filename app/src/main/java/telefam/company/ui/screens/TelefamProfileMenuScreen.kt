package telefam.company.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telefam.company.app.R
import kotlinx.coroutines.delay
import telefam.company.ui.theme.LocalTelefamDarkTheme
import telefam.company.ui.theme.TelefamCrimson
import telefam.company.ui.theme.TelefamCrimsonBright
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class ProfileMenuFeatureKind(
  val id: String,
) {
  DASHBOARD("dashboard"),
  STARS("stars"),
  SUBSCRIPTION("subscription"),
  MONETIZATION("monetization"),
  WALLET("wallet"),
  LINKED_DEVICE("linked_device"),
  CREATE_CAMPAIGN("create_campaign"),
  GET_VERIFIED("get_verified"),
}

private data class ProfileMenuEntry(
  val kind: ProfileMenuFeatureKind,
  val titleRes: Int,
  val subtitleRes: Int,
)

private val ProfileMenuEntries = listOf(
  ProfileMenuEntry(
    kind = ProfileMenuFeatureKind.DASHBOARD,
    titleRes = R.string.profile_menu_dashboard_title,
    subtitleRes = R.string.profile_menu_dashboard_sub,
  ),
  ProfileMenuEntry(
    kind = ProfileMenuFeatureKind.STARS,
    titleRes = R.string.profile_menu_stars_title,
    subtitleRes = R.string.profile_menu_stars_sub,
  ),
  ProfileMenuEntry(
    kind = ProfileMenuFeatureKind.SUBSCRIPTION,
    titleRes = R.string.profile_menu_subscription_title,
    subtitleRes = R.string.profile_menu_subscription_sub,
  ),
  ProfileMenuEntry(
    kind = ProfileMenuFeatureKind.MONETIZATION,
    titleRes = R.string.profile_menu_monetization_title,
    subtitleRes = R.string.profile_menu_monetization_sub,
  ),
  ProfileMenuEntry(
    kind = ProfileMenuFeatureKind.WALLET,
    titleRes = R.string.profile_menu_wallet_title,
    subtitleRes = R.string.profile_menu_wallet_sub,
  ),
  ProfileMenuEntry(
    kind = ProfileMenuFeatureKind.LINKED_DEVICE,
    titleRes = R.string.profile_menu_linked_device_title,
    subtitleRes = R.string.profile_menu_linked_device_sub,
  ),
  ProfileMenuEntry(
    kind = ProfileMenuFeatureKind.CREATE_CAMPAIGN,
    titleRes = R.string.profile_menu_create_campaign_title,
    subtitleRes = R.string.profile_menu_create_campaign_sub,
  ),
  ProfileMenuEntry(
    kind = ProfileMenuFeatureKind.GET_VERIFIED,
    titleRes = R.string.profile_menu_get_verified_title,
    subtitleRes = R.string.profile_menu_get_verified_sub,
  ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelefamProfileMenuScreen(
  onBack: () -> Unit,
  onOpenStars: () -> Unit = {},
  onOpenSubscriptions: () -> Unit = {},
  onOpenGetVerified: () -> Unit = {},
  onFeatureComingSoon: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  val isDark = LocalTelefamDarkTheme.current
  val accentRed = if (isDark) TelefamCrimsonBright else TelefamCrimson
  val pageBackground = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF4F6FA)
  val cardSurface = if (isDark) Color(0xFF171A21) else Color(0xFFFFFFFF)
  val cardBorder = if (isDark) Color(0xFF282D38) else Color(0xFFEAECEF)
  val titleColor = if (isDark) Color(0xFFF3F5F8) else Color(0xFF141821)
  val subtitleColor = if (isDark) Color(0xFF9EA6B6) else Color(0xFF6B7280)
  val chevronColor = if (isDark) Color(0xFF8C94A5) else Color(0xFF64748B)

  var localComingSoonBanner by remember { mutableStateOf<String?>(null) }
  val comingSoonSuffix = stringResource(R.string.coming_soon_title)

  LaunchedEffect(localComingSoonBanner) {
    if (localComingSoonBanner != null) {
      delay(2_000L)
      localComingSoonBanner = null
    }
  }

  val triggerItemComingSoon: (String) -> Unit = { title ->
    onFeatureComingSoon(title)
    localComingSoonBanner = "$title — $comingSoonSuffix"
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .testTag("profile_menu_screen"),
    containerColor = pageBackground,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = stringResource(R.string.profile_menu_title),
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            color = titleColor,
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("profile_menu_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.cd_back),
              tint = titleColor,
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = pageBackground,
          titleContentColor = titleColor,
          navigationIconContentColor = titleColor,
        ),
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 660.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        items(ProfileMenuEntries, key = { it.kind.id }) { entry ->
          val title = stringResource(entry.titleRes)
          val subtitle = stringResource(entry.subtitleRes)
          val isVerifiedCard = entry.kind == ProfileMenuFeatureKind.GET_VERIFIED

          Surface(
            onClick = {
              when (entry.kind) {
                ProfileMenuFeatureKind.STARS -> onOpenStars()
                ProfileMenuFeatureKind.SUBSCRIPTION -> onOpenSubscriptions()
                ProfileMenuFeatureKind.GET_VERIFIED -> onOpenGetVerified()
                else -> triggerItemComingSoon(title)
              }
            },
            shape = RoundedCornerShape(16.dp),
            color = cardSurface,
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = if (isDark) 0.dp else 1.dp,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("profile_menu_item_${entry.kind.id}"),
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
                ProfileMenuFeatureSvgIcon(
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
                  text = title,
                  fontSize = 15.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = titleColor,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = subtitle,
                  fontSize = 13.sp,
                  lineHeight = 17.5.sp,
                  color = subtitleColor,
                  maxLines = 3,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              Spacer(modifier = Modifier.width(10.dp))

              if (isVerifiedCard) {
                Surface(
                  onClick = onOpenGetVerified,
                  shape = CircleShape,
                  color = accentRed,
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("profile_menu_get_verified_cta"),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    ProfileMenuFeatureSvgIcon(
                      kind = ProfileMenuFeatureKind.GET_VERIFIED,
                      badgeBgColor = accentRed,
                      iconColor = Color.White,
                      size = 16.dp,
                    )
                    Text(
                      text = stringResource(R.string.profile_menu_get_verified_cta),
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
        item(key = "menu_bottom_spacer") {
          Spacer(modifier = Modifier.height(64.dp))
        }
      }

      val toastMsg = localComingSoonBanner
      if (toastMsg != null) {
        Box(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 24.dp, vertical = 18.dp)
            .clip(CircleShape)
            .background(Color(0xE0111318))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        ) {
          Text(
            text = toastMsg,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }
  }
}

@Composable
fun ProfileMenuFeatureSvgIcon(
  kind: ProfileMenuFeatureKind,
  badgeBgColor: Color,
  iconColor: Color,
  size: Dp = 24.dp,
) {
  Canvas(modifier = Modifier.size(size)) {
    val w = this.size.width
    val h = this.size.height
    when (kind) {
      ProfileMenuFeatureKind.DASHBOARD -> {
        val barW = w * 0.21f
        val radius = CornerRadius(barW * 0.45f, barW * 0.45f)
        val gap = w * 0.085f
        val startX = (w - (barW * 3f + gap * 2f)) / 2f
        val baseY = h * 0.84f
        val h1 = h * 0.28f
        val h2 = h * 0.48f
        val h3 = h * 0.68f
        drawRoundRect(
          color = iconColor,
          topLeft = Offset(startX, baseY - h1),
          size = Size(barW, h1),
          cornerRadius = radius,
        )
        drawRoundRect(
          color = iconColor,
          topLeft = Offset(startX + barW + gap, baseY - h2),
          size = Size(barW, h2),
          cornerRadius = radius,
        )
        drawRoundRect(
          color = iconColor,
          topLeft = Offset(startX + (barW + gap) * 2f, baseY - h3),
          size = Size(barW, h3),
          cornerRadius = radius,
        )
      }

      ProfileMenuFeatureKind.STARS -> {
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

      ProfileMenuFeatureKind.SUBSCRIPTION -> {
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
        drawRoundRect(
          color = iconColor,
          topLeft = Offset(w * 0.14f, h * 0.75f),
          size = Size(w * 0.72f, h * 0.10f),
          cornerRadius = CornerRadius(h * 0.05f, h * 0.05f),
        )
      }

      ProfileMenuFeatureKind.MONETIZATION -> {
        val cx = w * 0.50f
        val cy = h * 0.50f
        val r = w * 0.40f
        val coinPath = Path().apply {
          addOval(Rect(cx - r, cy - r, cx + r, cy + r))
        }
        drawPath(path = coinPath, color = iconColor)
        val tail = Path().apply {
          moveTo(w * 0.18f, h * 0.74f)
          lineTo(w * 0.12f, h * 0.88f)
          lineTo(w * 0.30f, h * 0.82f)
          close()
        }
        drawPath(path = tail, color = iconColor)
        val strokeW = w * 0.085f
        val dollarPath = Path().apply {
          moveTo(w * 0.61f, h * 0.35f)
          cubicTo(w * 0.58f, h * 0.28f, w * 0.39f, h * 0.28f, w * 0.39f, h * 0.39f)
          cubicTo(w * 0.39f, h * 0.50f, w * 0.62f, h * 0.48f, w * 0.62f, h * 0.60f)
          cubicTo(w * 0.62f, h * 0.71f, w * 0.41f, h * 0.72f, w * 0.38f, h * 0.63f)
        }
        drawPath(
          path = dollarPath,
          color = badgeBgColor,
          style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawLine(
          color = badgeBgColor,
          start = Offset(cx, h * 0.24f),
          end = Offset(cx, h * 0.76f),
          strokeWidth = strokeW * 0.9f,
          cap = StrokeCap.Round,
        )
      }

      ProfileMenuFeatureKind.WALLET -> {
        drawRoundRect(
          color = iconColor,
          topLeft = Offset(w * 0.12f, h * 0.22f),
          size = Size(w * 0.76f, h * 0.58f),
          cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
        )
        drawRoundRect(
          color = badgeBgColor,
          topLeft = Offset(w * 0.20f, h * 0.28f),
          size = Size(w * 0.56f, h * 0.08f),
          cornerRadius = CornerRadius(h * 0.04f, h * 0.04f),
        )
        drawRoundRect(
          color = badgeBgColor,
          topLeft = Offset(w * 0.54f, h * 0.44f),
          size = Size(w * 0.36f, h * 0.20f),
          cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
        )
        drawRoundRect(
          color = iconColor,
          topLeft = Offset(w * 0.57f, h * 0.47f),
          size = Size(w * 0.31f, h * 0.14f),
          cornerRadius = CornerRadius(w * 0.07f, w * 0.07f),
        )
        drawCircle(
          color = badgeBgColor,
          radius = w * 0.045f,
          center = Offset(w * 0.66f, h * 0.54f),
        )
      }

      ProfileMenuFeatureKind.LINKED_DEVICE -> {
        val strokeW = w * 0.08f
        drawRoundRect(
          color = iconColor,
          topLeft = Offset(w * 0.10f, h * 0.16f),
          size = Size(w * 0.46f, h * 0.54f),
          cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
          style = Stroke(width = strokeW),
        )
        drawRoundRect(
          color = badgeBgColor,
          topLeft = Offset(w * 0.42f, h * 0.30f),
          size = Size(w * 0.46f, h * 0.54f),
          cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
        )
        drawRoundRect(
          color = iconColor,
          topLeft = Offset(w * 0.45f, h * 0.33f),
          size = Size(w * 0.40f, h * 0.48f),
          cornerRadius = CornerRadius(w * 0.07f, w * 0.07f),
          style = Stroke(width = strokeW),
        )
        drawLine(
          color = iconColor,
          start = Offset(w * 0.60f, h * 0.72f),
          end = Offset(w * 0.70f, h * 0.72f),
          strokeWidth = strokeW * 0.85f,
          cap = StrokeCap.Round,
        )
      }

      ProfileMenuFeatureKind.CREATE_CAMPAIGN -> {
        val horn = Path().apply {
          moveTo(w * 0.16f, h * 0.46f)
          lineTo(w * 0.56f, h * 0.25f)
          lineTo(w * 0.68f, h * 0.68f)
          lineTo(w * 0.20f, h * 0.60f)
          close()
        }
        drawPath(path = horn, color = iconColor)
        val handle = Path().apply {
          moveTo(w * 0.26f, h * 0.60f)
          lineTo(w * 0.33f, h * 0.82f)
          lineTo(w * 0.45f, h * 0.79f)
          lineTo(w * 0.38f, h * 0.62f)
          close()
        }
        drawPath(path = handle, color = iconColor)
        drawLine(
          color = iconColor,
          start = Offset(w * 0.74f, h * 0.38f),
          end = Offset(w * 0.86f, h * 0.32f),
          strokeWidth = w * 0.08f,
          cap = StrokeCap.Round,
        )
        drawLine(
          color = iconColor,
          start = Offset(w * 0.76f, h * 0.52f),
          end = Offset(w * 0.88f, h * 0.54f),
          strokeWidth = w * 0.08f,
          cap = StrokeCap.Round,
        )
      }

      ProfileMenuFeatureKind.GET_VERIFIED -> {
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
        drawPath(path = seal, color = iconColor)
        val check = Path().apply {
          moveTo(w * 0.33f, h * 0.51f)
          lineTo(w * 0.45f, h * 0.63f)
          lineTo(w * 0.68f, h * 0.38f)
        }
        drawPath(
          path = check,
          color = badgeBgColor,
          style = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
      }
    }
  }
}
