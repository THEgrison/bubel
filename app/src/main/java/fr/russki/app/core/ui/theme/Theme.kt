package fr.russki.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import fr.russki.app.domain.model.ThemePreference

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = AccentOn,
    primaryContainer = AccentSoft,
    onPrimaryContainer = Ink,
    secondary = InkMuted,
    onSecondary = Paper,
    secondaryContainer = Mist,
    onSecondaryContainer = Ink,
    tertiary = Accent,
    background = Paper,
    onBackground = Ink,
    surface = PaperElevated,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = InkMuted,
    outline = Mist,
    outlineVariant = Mist,
    error = Danger,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = NightAccent,
    onPrimary = NightBg,
    primaryContainer = NightAccentSoft,
    onPrimaryContainer = NightText,
    secondary = NightMuted,
    onSecondary = NightBg,
    secondaryContainer = NightMist,
    onSecondaryContainer = NightText,
    tertiary = NightAccent,
    background = NightBg,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightElevated,
    onSurfaceVariant = NightMuted,
    outline = NightMist,
    outlineVariant = NightMist,
    error = Color(0xFFE08A8A),
    onError = NightBg,
)

@Composable
fun RusskiTheme(
    preference: ThemePreference = ThemePreference.System,
    content: @Composable () -> Unit,
) {
    val dark = when (preference) {
        ThemePreference.System -> isSystemInDarkTheme()
        ThemePreference.Light -> false
        ThemePreference.Dark -> true
    }
    val colors = if (dark) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = RusskiTypography,
        content = content,
    )
}
