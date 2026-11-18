package fr.russki.app.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fr.russki.app.feature.alphabet.AlphabetExercisesRoute
import fr.russki.app.feature.alphabet.AlphabetRoute
import fr.russki.app.feature.alphabet.LetterDetailRoute
import fr.russki.app.feature.flashcards.FlashcardsRoute
import fr.russki.app.feature.home.HomeRoute
import fr.russki.app.feature.learn.LearnRoute
import fr.russki.app.feature.packs.PacksRoute
import fr.russki.app.feature.profile.ProfileRoute
import fr.russki.app.feature.quiz.QuizRoute
import fr.russki.app.feature.review.ReviewRoute
import fr.russki.app.feature.vocabulary.GreetingsVocabularyRoute

private object LessonRoutes {
    const val Alphabet = "lesson/alphabet"
    const val AlphabetExercises = "lesson/alphabet/exercises"
    const val Letter = "lesson/alphabet/letter/{letterId}"
    const val Greetings = "lesson/vocabulary/greetings"
    const val Quiz = "lesson/quiz"
    const val Flashcards = "lesson/flashcards"
    const val Packs = "profile/packs"

    fun letter(id: String) = "lesson/alphabet/letter/$id"
}

@Composable
fun RusskiAppShell() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in RusskiDestination.entries.map { it.route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
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
                LearnRoute(
                    onOpenAlphabet = { navController.navigate(LessonRoutes.Alphabet) },
                    onOpenGreetings = { navController.navigate(LessonRoutes.Greetings) },
                )
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
                    onOpenFlashcards = { navController.navigate(LessonRoutes.Flashcards) },
                    onOpenQuiz = { navController.navigate(LessonRoutes.Quiz) },
                )
            }
            composable(RusskiDestination.Profile.route) {
                ProfileRoute(
                    onOpenPacks = { navController.navigate(LessonRoutes.Packs) },
                )
            }
            composable(LessonRoutes.Packs) {
                PacksRoute(onBack = { navController.popBackStack() })
            }
            composable(LessonRoutes.Alphabet) {
                AlphabetRoute(
                    onBack = { navController.popBackStack() },
                    onOpenLetter = { id -> navController.navigate(LessonRoutes.letter(id)) },
                    onOpenExercises = { navController.navigate(LessonRoutes.AlphabetExercises) },
                )
            }
            composable(LessonRoutes.AlphabetExercises) {
                AlphabetExercisesRoute(onBack = { navController.popBackStack() })
            }
            composable(
                route = LessonRoutes.Letter,
                arguments = listOf(navArgument("letterId") { type = NavType.StringType }),
            ) { entry ->
                val letterId = entry.arguments?.getString("letterId").orEmpty()
                LetterDetailRoute(
                    letterId = letterId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(LessonRoutes.Greetings) {
                GreetingsVocabularyRoute(onBack = { navController.popBackStack() })
            }
            composable(LessonRoutes.Quiz) {
                QuizRoute(onBack = { navController.popBackStack() })
            }
            composable(LessonRoutes.Flashcards) {
                FlashcardsRoute(onBack = { navController.popBackStack() })
            }
        }
    }
}
