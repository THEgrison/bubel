package fr.russki.app.core.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import fr.russki.app.feature.home.HomeRoute
import fr.russki.app.feature.learn.LearnRoute
import fr.russki.app.feature.profile.ProfileRoute
import fr.russki.app.feature.review.ReviewRoute

@Composable
fun RusskiAppShell() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                RusskiDestination.entries.forEach { destination ->
                    val selected = destination.route == currentRoute
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) {
                                    destination.selectedIcon
                                } else {
                                    destination.unselectedIcon
                                },
                                contentDescription = destination.label,
                            )
                        },
                        label = {
                            Text(
                                text = destination.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = RusskiDestination.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(160)) },
        ) {
            composable(RusskiDestination.Home.route) {
                HomeRoute(
                    onOpenLearn = {
                        navController.navigate(RusskiDestination.Learn.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenReview = {
                        navController.navigate(RusskiDestination.Review.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(RusskiDestination.Learn.route) {
                LearnRoute()
            }
            composable(RusskiDestination.Review.route) {
                ReviewRoute(
                    onGoLearn = {
                        navController.navigate(RusskiDestination.Learn.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(RusskiDestination.Profile.route) {
                ProfileRoute()
            }
        }
    }
}
