package telefam.company.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.telefam.company.app.R

val PlusJakartaFontFamily = FontFamily(
  Font(R.font.plus_jakarta_sans, FontWeight.Normal),
  Font(R.font.plus_jakarta_sans, FontWeight.Medium),
  Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
  Font(R.font.plus_jakarta_sans, FontWeight.Bold),
)

val InterFontFamily = FontFamily(
  Font(R.font.inter, FontWeight.Normal),
  Font(R.font.inter, FontWeight.Medium),
  Font(R.font.inter, FontWeight.SemiBold),
  Font(R.font.inter, FontWeight.Bold),
)

// Text post font catalog backed by locally bundled Google Fonts
val PlayfairSerifFontFamily = FontFamily(
  Font(R.font.playfair_display, FontWeight.SemiBold),
  Font(R.font.playfair_display, FontWeight.Bold),
)

val CaveatHandFontFamily = FontFamily(
  Font(R.font.caveat, FontWeight.Normal),
  Font(R.font.caveat, FontWeight.Bold),
)

val CourierTypewriterFontFamily = FontFamily(
  Font(R.font.courier_prime, FontWeight.Normal),
  Font(R.font.courier_prime, FontWeight.Bold),
)

val MontserratBoldFontFamily = FontFamily(
  Font(R.font.montserrat, FontWeight.Bold),
  Font(R.font.montserrat, FontWeight.ExtraBold),
)

val SpaceMonoFontFamily = FontFamily(
  Font(R.font.space_mono, FontWeight.Normal),
  Font(R.font.space_mono, FontWeight.Bold),
)

val OswaldDisplayFontFamily = FontFamily(
  Font(R.font.oswald, FontWeight.Bold),
)

val FredokaRoundedFontFamily = FontFamily(
  Font(R.font.fredoka, FontWeight.SemiBold),
  Font(R.font.fredoka, FontWeight.Bold),
)

val MerriweatherItalicFontFamily = FontFamily(
  Font(R.font.merriweather, FontWeight.Medium, FontStyle.Italic),
)

val TelefamTypography = Typography(
  displaySmall = TextStyle(
    fontFamily = PlusJakartaFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 32.sp,
    lineHeight = 38.sp,
    letterSpacing = (-0.5).sp,
  ),
  headlineSmall = TextStyle(
    fontFamily = PlusJakartaFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
    letterSpacing = (-0.25).sp,
  ),
  titleMedium = TextStyle(
    fontFamily = PlusJakartaFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
  ),
  bodyLarge = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp,
  ),
  bodyMedium = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
  ),
  labelLarge = TextStyle(
    fontFamily = PlusJakartaFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    lineHeight = 20.sp,
  ),
  labelSmall = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
  ),
)
