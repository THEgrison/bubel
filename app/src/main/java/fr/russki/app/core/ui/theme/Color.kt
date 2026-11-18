package fr.russki.app.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette Russki — bleu nuit / crème / accent marque rouge discret.
 * Vert et rouge forts réservés au feedback pédagogique.
 */

// —— Clair ——
val Ink = Color(0xFF142033)
val InkMuted = Color(0xFF5A6578)
val Paper = Color(0xFFF7F5F1)
val PaperElevated = Color(0xFFFFFFFF)
val Mist = Color(0xFFE4E8EF)
val BrandBlue = Color(0xFF1B3A5F)
val BrandBlueSoft = Color(0xFFD6E2F2)
val BrandOn = Color(0xFFF5F8FC)
val BrandRed = Color(0xFFB23A3A)
val BrandRedSoft = Color(0xFFF6E0E0)

// Feedback pédagogique (clair)
val Correct = Color(0xFF1F7A4D)
val CorrectContainer = Color(0xFFD9F2E5)
val OnCorrectContainer = Color(0xFF0C3D26)
val Incorrect = Color(0xFFB3261E)
val IncorrectContainer = Color(0xFFF9DEDC)
val OnIncorrectContainer = Color(0xFF410E0B)

// —— Sombre ——
val NightBg = Color(0xFF0E1520)
val NightSurface = Color(0xFF172133)
val NightElevated = Color(0xFF212D42)
val NightText = Color(0xFFE8EDF5)
val NightMuted = Color(0xFFA4B0C4)
val NightBlue = Color(0xFF7BA3D4)
val NightBlueSoft = Color(0xFF24344D)
val NightMist = Color(0xFF2A364C)
val NightBrandRed = Color(0xFFE08A8A)

val NightCorrect = Color(0xFF6FCF97)
val NightCorrectContainer = Color(0xFF1A3D2C)
val NightOnCorrectContainer = Color(0xFFD9F2E5)
val NightIncorrect = Color(0xFFF2B8B5)
val NightIncorrectContainer = Color(0xFF5C221E)
val NightOnIncorrectContainer = Color(0xFFF9DEDC)

/** Rayons du design system. */
object RusskiRadii {
    val Sm = 10
    val Md = 16
    val Lg = 22
    val Pill = 999
}

/** Espacements de base (dp). */
object RusskiSpace {
    val Xs = 4
    val Sm = 8
    val Md = 12
    val Lg = 16
    val Xl = 24
}
