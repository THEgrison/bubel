package fr.russki.app.feature.listening

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
import fr.russki.app.feature.audio.LocalAudioControls
import fr.russki.app.feature.stats.TrackStudyTime

@Composable
fun ListeningPracticeRoute(
    onBack: () -> Unit,
    viewModel: ListeningPracticeViewModel =
        viewModel(factory = ListeningPracticeViewModel.Factory()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TrackStudyTime()
    Column(Modifier.fillMaxSize()) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
        }
        ListeningPracticeScreen(
            state = state,
            onRetry = viewModel::refresh,
            onSubmit = viewModel::submit,
            onContinue = viewModel::advance,
            onRestart = viewModel::restart,
        )
    }
}

@Composable
fun ListeningPracticeScreen(
    state: LoadState<ListeningPracticeUi>,
    onRetry: () -> Unit,
    onSubmit: (String) -> Unit,
    onContinue: () -> Unit,
    onRestart: () -> Unit,
) {
    ScreenScaffold(
        title = "Mode écoute",
        subtitle = "Écoutez si l’audio est présent, sinon répondez à partir du texte.",
    ) {
        when (state) {
            LoadState.Loading -> LoadingState()
            is LoadState.Error -> ErrorState(message = state.message, onRetry = onRetry)
            is LoadState.Empty -> ErrorState(message = state.message, onRetry = onRetry)
            is LoadState.Ready -> FadeInContent(visible = true) {
                val ui = state.value
                if (ui.finished) {
                    SoftPanel {
                        SectionLabel("Terminé")
                        Text(
                            text = "Score : ${ui.correctCount} / ${ui.items.size}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        SpacerH(8)
                        Text(
                            text = "Ce score n’évalue pas la prononciation et n’est pas un niveau CECRL.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SpacerH(12)
                        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
                            Text("Recommencer")
                        }
                    }
                } else {
                    val item = ui.currentItem ?: return@FadeInContent
                    var selected by remember(item.id) { mutableStateOf<String?>(null) }
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MetaChip("Question ${ui.index + 1} / ${ui.items.size}")
                        ProgressLine(
                            (ui.index.toFloat() / ui.items.size.coerceAtLeast(1))
                                .coerceIn(0f, 1f),
                        )
                        SoftPanel {
                            Text(item.promptFr, style = MaterialTheme.typography.titleMedium)
                            SpacerH(10)
                            LocalAudioControls(
                                audio = ui.currentAudio,
                                unavailableMessage = "Audio indisponible — mode texte actif.",
                            )
                            SpacerH(10)
                            if (ui.currentAudio == null || ui.grade != null) {
                                CyrillicSample(item.textRuFallback)
                            } else {
                                Text(
                                    text = "Stimulus texte masqué tant que l’audio est disponible. " +
                                        "Il apparaît après correction.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        SoftPanel {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                item.choices.forEach { choice ->
                                    FilterChip(
                                        selected = selected == choice,
                                        onClick = {
                                            if (ui.grade == null) selected = choice
                                        },
                                        label = { Text(choice) },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                        ui.grade?.let { grade ->
                            SoftPanel {
                                SectionLabel(if (grade.isCorrect) "Correct" else "Incorrect")
                                Text(grade.explanationFr)
                                if (!grade.isCorrect) {
                                    SpacerH(6)
                                    Text(
                                        text = "Réponse acceptée : ${grade.acceptedAnswers.first()}",
                                        color = MaterialTheme.colorScheme.primary,
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
            }
        }
    }
}
