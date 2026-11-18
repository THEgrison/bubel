package fr.russki.app.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Style
import androidx.compose.ui.graphics.vector.ImageVector

enum class RusskiDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Home(
        route = "home",
        label = "Accueil",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    ),
    Learn(
        route = "learn",
        label = "Apprendre",
        selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
        unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook,
    ),
    Review(
        route = "review",
        label = "Réviser",
        selectedIcon = Icons.Filled.Style,
        unselectedIcon = Icons.Outlined.Style,
    ),
    Profile(
        route = "profile",
        label = "Profil",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
    ),
}
