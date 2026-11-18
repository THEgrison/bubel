package fr.russki.app.feature.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.russki.app.core.ui.components.CyrillicSample
import fr.russki.app.core.ui.components.EmptyState
import fr.russki.app.core.ui.components.ErrorState
import fr.russki.app.core.ui.components.FadeInContent
import fr.russki.app.core.ui.components.LoadingState
import fr.russki.app.core.ui.components.MetaChip
import fr.russki.app.core.ui.components.ProgressLine
import fr.russki.app.core.ui.components.ScreenScaffold
import fr.russki.app.core.ui.components.SectionLabel
import fr.russki.app.core.ui.components.SoftPanel
import fr.russki.app.core.ui.components.SpacerH
import fr.russki.app.domain.model.LoadState
import fr.russki.app.domain.review.ReviewPhase
import fr.russki.app.domain.review.ReviewSessionState
import fr.russki.app.feature.stats.TrackStudyTime

@Composable
fun ReviewSessionRoute(
    onBack: () -> Unit,
    viewModel: ReviewSessionViewModel =
        viewModel(factory = ReviewSessionViewModel.Factory()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TrackStudyTime()
    Column(Modifier.fillMaxSize()) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
        }
        ReviewSessionScreen(
            state = state,
            onRetry = viewModel::start,
            onSubmit = viewModel::submit,
            onContinue = viewModel::advance,
            onDone = onBack,
        )
    }
}

@Composable
fun ReviewSessionScreen(
    state: LoadState<ReviewSessionState>,
    onRetry: () -> Unit,
    onSubmit: (String) -> Unit,
    onContinue: () -> Unit,
    onDone: () -> Unit,
) {
    ScreenScaffold(
        title = "Session de révision",
        subtitle = "Répétition espacée locale — sans audio ni IA.",
    ) {
        when (state) {
            LoadState.Loading -> LoadingState()
            is LoadState.Error -> ErrorState(message = state.message, onRetry = onRetry)
            is LoadState.Empty -> EmptyState(
                title = "Rien à réviser",
                body = state.message,
                actionLabel = "Retour",
                onAction = onDone,
            )
            is LoadState.Ready -> FadeInContent(visible = true) {
                val session = state.value
                when (session.phase) {
                    ReviewPhase.Summary -> Summary(session, onRetry = onRetry, onDone = onDone)
                    else -> PromptOrFeedback(
                        session = session,
                        onSubmit = onSubmit,
                        onContinue = onContinue,
                    )
                }
            }
        }
    }
}

@Composable
private fun PromptOrFeedback(
    session: ReviewSessionState,
    onSubmit: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val item = session.current ?: return
    var selected by remember(item.id) { mutableStateOf<String?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetaChip("Élément ${session.index + 1} / ${session.queue.size}")
        ProgressLine(session.progressFraction)
        SoftPanel {
            SectionLabel(item.kind.name)
            Text(item.promptFr, style = MaterialTheme.typography.titleMedium)
            SpacerH(10)
            if (item.stimulus.isNotBlank() && item.stimulus != "…") {
                CyrillicSample(item.stimulus)
            }
        }
        SoftPanel {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item.choices.forEach { choice ->
                    FilterChip(
                        selected = selected == choice,
                        onClick = {
                            if (session.phase == ReviewPhase.Prompt) selected = choice
                        },
                        label = { Text(choice) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        session.currentGrade?.let { grade ->
            SoftPanel {
                SectionLabel(if (grade.isCorrect) "Correct" else "À revoir bientôt")
                Text(grade.explanationFr, style = MaterialTheme.typography.bodyLarge)
                if (!grade.isCorrect) {
                    SpacerH(8)
                    Text(
                        text = "Réponse attendue : ${grade.acceptedAnswers.first()}",
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                session.srsAfterGrade[item.id]?.let { srs ->
                    SpacerH(8)
                    Text(
                        text = "Prochaine révision planifiée " +
                            "(intervalle ${"%.1f".format(srs.intervalDays)} j, " +
                            "état ${srs.learningState.name}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                Text("Continuer")
            }
        } ?: Button(
            onClick = { onSubmit(selected.orEmpty()) },
            enabled = selected != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Valider")
        }
        SpacerH(8)
    }
}

@Composable
private fun Summary(
    session: ReviewSessionState,
    onRetry: () -> Unit,
    onDone: () -> Unit,
) {
    SoftPanel {
        SectionLabel("Récapitulatif")
        Text(
            text = "Score : ${session.correctCount} / ${session.queue.size}",
            style = MaterialTheme.typography.titleLarge,
        )
        SpacerH(8)
        Text(
            text = "Les erreurs reviennent rapidement ; les succès espacent la prochaine échéance. " +
                "Ce n’est pas un algorithme scientifiquement optimal — règles locales déterministes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SpacerH(12)
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text("Voir s'il reste des éléments dus")
        }
        SpacerH(8)
        OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Terminer")
        }
    }
}
