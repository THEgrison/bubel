package fr.russki.app.feature.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Style
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
import fr.russki.app.core.ui.components.PrimaryButton
import fr.russki.app.core.ui.components.ScreenScaffold
import fr.russki.app.core.ui.components.SecondaryButton
import fr.russki.app.core.ui.components.SectionLabel
import fr.russki.app.core.ui.components.SoftPanel
import fr.russki.app.core.ui.components.SpacerH
import fr.russki.app.domain.model.LoadState

@Composable
fun ReviewRoute(
    onGoLearn: () -> Unit,
    onOpenReviewSession: () -> Unit,
    onOpenFlashcards: () -> Unit,
    onOpenQuiz: () -> Unit,
    viewModel: ReviewViewModel = viewModel(factory = ReviewViewModel.Factory()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReviewScreen(
        state = state,
        onRetry = viewModel::refresh,
        onGoLearn = onGoLearn,
        onOpenReviewSession = onOpenReviewSession,
        onOpenFlashcards = onOpenFlashcards,
        onOpenQuiz = onOpenQuiz,
    )
}

@Composable
fun ReviewScreen(
    state: LoadState<ReviewUiModel>,
    onRetry: () -> Unit,
    onGoLearn: () -> Unit,
    onOpenReviewSession: () -> Unit,
    onOpenFlashcards: () -> Unit,
    onOpenQuiz: () -> Unit,
) {
    ScreenScaffold(
        title = "Réviser",
        subtitle = "Répétition espacée locale — uniquement les notions déjà étudiées.",
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
                    PrimaryButton(
                        text = "Lancer un quiz",
                        onClick = onOpenQuiz,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            is LoadState.Ready -> FadeInContent(visible = true) {
                val model = state.value
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SoftPanel {
                        SectionLabel("File du jour")
                        Text(
                            text = if (model.dueCount == 0) {
                                "Aucun élément échu"
                            } else {
                                "${model.dueCount} élément${if (model.dueCount > 1) "s" else ""} " +
                                    "échu${if (model.dueCount > 1) "s" else ""}"
                            },
                            style = MaterialTheme.typography.titleLarge,
                        )
                        SpacerH(6)
                        Text(
                            text = "${model.unlockedCount} notion${if (model.unlockedCount > 1) "s" else ""} " +
                                "débloquée${if (model.unlockedCount > 1) "s" else ""} " +
                                "(leçons / dialogues terminés).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SpacerH(8)
                        Text(
                            text = "Erreur → révision rapprochée (~10 min). " +
                                "Succès → intervalle croissant (jours). " +
                                "Règles déterministes locales, pas un modèle optimal scientifique.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SpacerH(12)
                        PrimaryButton(
                            text = if (model.dueCount > 0) {
                                "Démarrer une session courte"
                            } else {
                                "Rien à réviser pour l’instant"
                            },
                            onClick = onOpenReviewSession,
                            enabled = model.dueCount > 0,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        SpacerH(8)
                        SecondaryButton(
                            text = "Flashcards (auto-évaluation)",
                            onClick = onOpenFlashcards,
                            enabled = model.dueCount > 0,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    SoftPanel {
                        Text("Quiz", style = MaterialTheme.typography.titleMedium)
                        SpacerH(8)
                        Text(
                            if (model.hasInterruptedQuiz) {
                                "Une session de quiz est en cours — vous pouvez la reprendre."
                            } else {
                                "Entraînement libre généré depuis le contenu hors ligne (hors file SRS)."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SpacerH(12)
                        PrimaryButton(
                            text = if (model.hasInterruptedQuiz) {
                                "Reprendre le quiz"
                            } else {
                                "Nouveau quiz"
                            },
                            onClick = onOpenQuiz,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
