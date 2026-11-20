package telefam.company.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
  SYSTEM,
  LIGHT,
  DARK,
}

val LocalTelefamDarkTheme = staticCompositionLocalOf { false }

private val TelefamLightColorScheme = lightColorScheme(
  primary = TelefamCrimson,
  onPrimary = TelefamPureWhite,
  primaryContainer = TelefamBlushWave,
  onPrimaryContainer = TelefamCrimsonDeep,
  secondary = TelefamCrimsonBright,
  onSecondary = TelefamPureWhite,
  background = TelefamPureWhite,
  onBackground = TelefamTextPrimary,
  surface = TelefamPureWhite,
  onSurface = TelefamTextPrimary,
  surfaceVariant = TelefamSoftSurface,
  onSurfaceVariant = TelefamTextMuted,
  outline = TelefamInputBorder,
  outlineVariant = TelefamDivider,
  error = TelefamCrimsonDeep,
  onError = TelefamPureWhite,
)

private val TelefamDarkColorScheme = darkColorScheme(
  primary = TelefamCrimsonDarkMode,
  onPrimary = TelefamPureWhite,
  primaryContainer = TelefamDarkBlush,
  onPrimaryContainer = TelefamBlushWave,
  secondary = TelefamCrimsonBright,
  onSecondary = TelefamPureWhite,
  background = TelefamDarkBg,
  onBackground = TelefamDarkTextPrimary,
  surface = TelefamDarkSurface,
  onSurface = TelefamDarkTextPrimary,
  surfaceVariant = TelefamDarkSurfaceVariant,
  onSurfaceVariant = TelefamDarkTextMuted,
  outline = TelefamDarkBorder,
  outlineVariant = TelefamDarkBorder,
  error = Color(0xFFFF6B6B),
  onError = TelefamDarkBg,
)

@Composable
fun TelefamTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  content: @Composable () -> Unit,
) {
  val isDark = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
  }
  val colorScheme = if (isDark) TelefamDarkColorScheme else TelefamLightColorScheme

  CompositionLocalProvider(LocalTelefamDarkTheme provides isDark) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = TelefamTypography,
      content = content,
    )
  }
}
