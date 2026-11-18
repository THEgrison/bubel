package fr.russki.app.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import fr.russki.app.R

/**
 * IBM Plex Sans : lisibilité UI + excellent rendu cyrillique.
 * Source Serif 4 : titres et échantillons russes.
 */
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val plexSansName = GoogleFont("IBM Plex Sans")
private val sourceSerifName = GoogleFont("Source Serif 4")

val RusskiSans = FontFamily(
    Font(googleFont = plexSansName, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = plexSansName, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = plexSansName, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = plexSansName, fontProvider = provider, weight = FontWeight.Bold),
)

val RusskiSerif = FontFamily(
    Font(googleFont = sourceSerifName, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = sourceSerifName, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = sourceSerifName, fontProvider = provider, weight = FontWeight.SemiBold),
)

val RusskiTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = RusskiSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = RusskiSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = RusskiSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = RusskiSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = RusskiSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = RusskiSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = RusskiSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = RusskiSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = RusskiSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = RusskiSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)
