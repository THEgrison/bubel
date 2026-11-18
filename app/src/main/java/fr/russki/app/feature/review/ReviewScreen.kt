package fr.russki.app.feature.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.russki.app.core.ui.components.EmptyState
import fr.russki.app.core.ui.components.ErrorState
import fr.russki.app.core.ui.components.FadeInContent
import fr.russki.app.core.ui.components.LoadingState
import fr.russki.app.core.ui.components.ScreenScaffold
import fr.russki.app.core.ui.components.SoftPanel
import fr.russki.app.core.ui.components.SpacerH
import fr.russki.app.domain.model.LoadState

@Composable
fun ReviewRoute(
    onGoLearn: () -> Unit,
    onOpenFlashcards: () -> Unit,
    onOpenQuiz: () -> Unit,
    viewModel: ReviewViewModel = viewModel(factory = ReviewViewModel.Factory()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReviewScreen(
        state = state,
        onRetry = viewModel::refresh,
        onGoLearn = onGoLearn,
        onOpenFlashcards = onOpenFlashcards,
        onOpenQuiz = onOpenQuiz,
    )
}

@Composable
fun ReviewScreen(
    state: LoadState<ReviewUiModel>,
    onRetry: () -> Unit,
    onGoLearn: () -> Unit,
    onOpenFlashcards: () -> Unit,
    onOpenQuiz: () -> Unit,
) {
    ScreenScaffold(
        title = "Réviser",
        subtitle = "Quiz, flashcards et répétition espacée.",
    ) {
        when (state) {
            LoadState.Loading -> LoadingState()
            is LoadState.Error -> ErrorState(message = state.message, onRetry = onRetry)
            is LoadState.Empty -> FadeInContent(visible = true) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EmptyState(
                        title = "Rien à réviser pour l'instant",
                        body = state.message,
                        icon = Icons.Outlined.Style,
                        actionLabel = "Aller à Apprendre",
                        onAction = onGoLearn,
                    )
                    Button(onClick = onOpenQuiz, modifier = Modifier.fillMaxWidth()) {
                        Text("Lancer un quiz")
                    }
                }
            }
            is LoadState.Ready -> FadeInContent(visible = true) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SoftPanel {
                        Text(
                            text = "${state.value.dueCount} carte${if (state.value.dueCount > 1) "s" else ""} due${if (state.value.dueCount > 1) "s" else ""}",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        SpacerH(8)
                        Text(
                            text = "Les cartes difficiles reviennent plus tôt ; une bonne réponse " +
                                "planifie toujours une prochaine révision.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SpacerH(12)
                        Button(
                            onClick = onOpenFlashcards,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state.value.dueCount > 0,
                        ) {
                            Text("Réviser les flashcards")
                        }
                    }
                    SoftPanel {
                        Text("Quiz", style = MaterialTheme.typography.titleMedium)
                        SpacerH(8)
                        Text(
                            if (state.value.hasInterruptedQuiz) {
                                "Une session de quiz est en cours — vous pouvez la reprendre."
                            } else {
                                "Entraînez-vous avec des questions générées depuis le contenu hors ligne."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SpacerH(12)
                        Button(onClick = onOpenQuiz, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                if (state.value.hasInterruptedQuiz) {
                                    "Reprendre le quiz"
                                } else {
                                    "Nouveau quiz"
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
