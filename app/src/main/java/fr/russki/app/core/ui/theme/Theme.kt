package fr.russki.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import fr.russki.app.domain.model.ThemePreference

@Immutable
data class RusskiFeedbackColors(
    val correct: Color,
    val onCorrect: Color,
    val correctContainer: Color,
    val onCorrectContainer: Color,
    val incorrect: Color,
    val onIncorrect: Color,
    val incorrectContainer: Color,
    val onIncorrectContainer: Color,
    val brandAccent: Color,
)

val LocalRusskiFeedback = staticCompositionLocalOf {
    RusskiFeedbackColors(
        correct = Correct,
        onCorrect = Color.White,
        correctContainer = CorrectContainer,
        onCorrectContainer = OnCorrectContainer,
        incorrect = Incorrect,
        onIncorrect = Color.White,
        incorrectContainer = IncorrectContainer,
        onIncorrectContainer = OnIncorrectContainer,
        brandAccent = BrandRed,
    )
}

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = BrandOn,
    primaryContainer = BrandBlueSoft,
    onPrimaryContainer = Ink,
    secondary = InkMuted,
    onSecondary = Paper,
    secondaryContainer = Mist,
    onSecondaryContainer = Ink,
    tertiary = BrandRed,
    onTertiary = Color.White,
    tertiaryContainer = BrandRedSoft,
    onTertiaryContainer = Ink,
    background = Paper,
    onBackground = Ink,
    surface = PaperElevated,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = InkMuted,
    outline = Color(0xFFC5CCD8),
    outlineVariant = Mist,
    error = Incorrect,
    onError = Color.White,
    errorContainer = IncorrectContainer,
    onErrorContainer = OnIncorrectContainer,
)

private val DarkColors = darkColorScheme(
    primary = NightBlue,
    onPrimary = NightBg,
    primaryContainer = NightBlueSoft,
    onPrimaryContainer = NightText,
    secondary = NightMuted,
    onSecondary = NightBg,
    secondaryContainer = NightMist,
    onSecondaryContainer = NightText,
    tertiary = NightBrandRed,
    onTertiary = NightBg,
    tertiaryContainer = NightIncorrectContainer,
    onTertiaryContainer = NightText,
    background = NightBg,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightElevated,
    onSurfaceVariant = NightMuted,
    outline = NightMist,
    outlineVariant = NightMist,
    error = NightIncorrect,
    onError = NightBg,
    errorContainer = NightIncorrectContainer,
    onErrorContainer = NightOnIncorrectContainer,
)

private val LightFeedback = RusskiFeedbackColors(
    correct = Correct,
    onCorrect = Color.White,
    correctContainer = CorrectContainer,
    onCorrectContainer = OnCorrectContainer,
    incorrect = Incorrect,
    onIncorrect = Color.White,
    incorrectContainer = IncorrectContainer,
    onIncorrectContainer = OnIncorrectContainer,
    brandAccent = BrandRed,
)

private val DarkFeedback = RusskiFeedbackColors(
    correct = NightCorrect,
    onCorrect = NightBg,
    correctContainer = NightCorrectContainer,
    onCorrectContainer = NightOnCorrectContainer,
    incorrect = NightIncorrect,
    onIncorrect = NightBg,
    incorrectContainer = NightIncorrectContainer,
    onIncorrectContainer = NightOnIncorrectContainer,
    brandAccent = NightBrandRed,
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
    val feedback = if (dark) DarkFeedback else LightFeedback

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
        }
    }

    CompositionLocalProvider(LocalRusskiFeedback provides feedback) {
        MaterialTheme(
            colorScheme = colors,
            typography = RusskiTypography,
            content = content,
        )
    }
}

object RusskiThemeExtras {
    val feedback: RusskiFeedbackColors
        @Composable
        get() = LocalRusskiFeedback.current
}
