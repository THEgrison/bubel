package fr.russki.app.feature.dialogue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import fr.russki.app.core.ui.components.AnswerChoiceButton
import fr.russki.app.core.ui.components.AnswerFeedbackBanner
import fr.russki.app.core.ui.components.AnswerFeedbackState
import fr.russki.app.core.ui.components.CyrillicSample
import fr.russki.app.core.ui.components.ErrorState
import fr.russki.app.core.ui.components.FadeInContent
import fr.russki.app.core.ui.components.LoadingState
import fr.russki.app.core.ui.components.MetaChip
import fr.russki.app.core.ui.components.PrimaryButton
import fr.russki.app.core.ui.components.ProgressLine
import fr.russki.app.core.ui.components.ScreenScaffold
import fr.russki.app.core.ui.components.SecondaryButton
import fr.russki.app.core.ui.components.SectionLabel
import fr.russki.app.core.ui.components.SoftPanel
import fr.russki.app.core.ui.components.SpacerH
import fr.russki.app.core.ui.components.feedbackStateFrom
import fr.russki.app.domain.dialogue.DialogueActivityType
import fr.russki.app.domain.exercise.AnswerMatcher
import fr.russki.app.domain.dialogue.DialoguePhase
import fr.russki.app.domain.model.LoadState
import fr.russki.app.feature.audio.LocalAudioControls
import fr.russki.app.feature.stats.TrackStudyTime

@Composable
fun DialogueReaderRoute(
    dialogueId: String,
    onBack: () -> Unit,
    viewModel: DialogueReaderViewModel =
        viewModel(factory = DialogueReaderViewModel.Factory(dialogueId)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TrackStudyTime()
    Column(Modifier.fillMaxSize()) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
        }
        DialogueReaderScreen(
            state = state,
            onRetry = viewModel::start,
            onBegin = viewModel::begin,
            onReveal = viewModel::revealTranslation,
            onAdvanceLine = viewModel::advanceFromLine,
            onSubmit = viewModel::submit,
            onContinue = viewModel::continueAfterFeedback,
            onFinish = {
                viewModel.finish()
                onBack()
            },
            onRestart = viewModel::restart,
        )
    }
}

@Composable
fun DialogueReaderScreen(
    state: LoadState<DialogueReaderUi>,
    onRetry: () -> Unit,
    onBegin: () -> Unit,
    onReveal: () -> Unit,
    onAdvanceLine: () -> Unit,
    onSubmit: (String) -> Unit,
    onContinue: () -> Unit,
    onFinish: () -> Unit,
    onRestart: () -> Unit,
) {
    when (state) {
        LoadState.Loading -> LoadingState()
        is LoadState.Error -> ErrorState(message = state.message, onRetry = onRetry)
        is LoadState.Empty -> ErrorState(message = state.message, onRetry = onRetry)
        is LoadState.Ready -> FadeInContent(visible = true) {
            val ui = state.value
            val session = ui.session
            ScreenScaffold(
                title = session.script.outline.titleFr,
                subtitle = session.script.outline.objectiveFr,
            ) {
                ProgressLine(progress = session.progressFraction)
                SpacerH(16)
                when (session.phase) {
                    DialoguePhase.Introduction -> IntroPhase(ui = ui, onBegin = onBegin)
                    DialoguePhase.Line -> LinePhase(
                        ui = ui,
                        onReveal = onReveal,
                        onAdvance = onAdvanceLine,
                    )
                    DialoguePhase.Activity -> ActivityPhase(
                        ui = ui,
                        onSubmit = onSubmit,
                        onContinue = onContinue,
                    )
                    DialoguePhase.Summary, DialoguePhase.Completed -> SummaryPhase(
                        ui = ui,
                        onFinish = onFinish,
                        onRestart = onRestart,
                    )
                }
            }
        }
    }
}

@Composable
private fun IntroPhase(ui: DialogueReaderUi, onBegin: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SoftPanel {
            SectionLabel("Situation")
            Text(ui.session.script.outline.situationFr, style = MaterialTheme.typography.bodyLarge)
            SpacerH(12)
            SectionLabel("Objectif")
            Text(ui.session.script.outline.objectiveFr, style = MaterialTheme.typography.bodyMedium)
            SpacerH(12)
            MetaChip("${ui.session.script.outline.cefrLevel.name} · ${ui.session.script.lines.size} répliques")
            SpacerH(16)
            PrimaryButton(
                text = "Commencer le dialogue",
                onClick = onBegin,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LinePhase(
    ui: DialogueReaderUi,
    onReveal: () -> Unit,
    onAdvance: () -> Unit,
) {
    val session = ui.session
    val line = session.currentLine ?: return
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SoftPanel {
            SectionLabel(session.script.speakerLabel(line.speakerId))
            CyrillicSample(line.textRu)
            SpacerH(12)
            if (session.translationRevealed) {
                Text(
                    text = line.textFr,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                SecondaryButton(
                    text = "Afficher la traduction",
                    onClick = onReveal,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            SpacerH(12)
            LocalAudioControls(
                audio = ui.currentAudio,
                unavailableMessage = "Audio indisponible — aucun enregistrement natif pour cette réplique.",
            )
        }
        PrimaryButton(
            text = "Continuer",
            onClick = onAdvance,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ActivityPhase(
    ui: DialogueReaderUi,
    onSubmit: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val session = ui.session
    val activity = session.currentActivity ?: return
    var selected by remember(activity.id) { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SoftPanel {
            MetaChip(
                when (activity.type) {
                    DialogueActivityType.ChooseTranslation -> "Traduction"
                    DialogueActivityType.ChooseReply -> "Réponse"
                    DialogueActivityType.FillBlank -> "Mot manquant"
                },
            )
            SpacerH(8)
            SectionLabel(activity.promptFr)
            if (activity.stimulusRu.isNotBlank()) {
                SpacerH(8)
                CyrillicSample(activity.stimulusRu)
            }
            SpacerH(12)
            val grade = session.currentGrade
            val feedbackState = grade?.let { feedbackStateFrom(it.isCorrect) }
                ?: AnswerFeedbackState.Idle
            val matcher = remember { AnswerMatcher() }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                activity.choices.forEach { choice ->
                    val isCorrectOption = grade != null &&
                        matcher.matches(choice, grade.acceptedAnswers)
                    AnswerChoiceButton(
                        label = choice,
                        selected = selected == choice,
                        isCorrectOption = isCorrectOption,
                        feedbackState = feedbackState,
                        enabled = grade == null,
                        onClick = { selected = choice },
                    )
                }
            }
        }
        val grade = session.currentGrade
        if (grade == null) {
            PrimaryButton(
                text = "Valider",
                onClick = { selected?.let(onSubmit) },
                enabled = selected != null,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            AnswerFeedbackBanner(
                state = feedbackStateFrom(grade.isCorrect),
                correctAnswerLabel = grade.acceptedAnswers.first(),
                explanation = grade.explanationFr,
                onContinue = onContinue,
            )
        }
    }
}

@Composable
private fun SummaryPhase(
    ui: DialogueReaderUi,
    onFinish: () -> Unit,
    onRestart: () -> Unit,
) {
    val session = ui.session
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SoftPanel {
            SectionLabel("Résumé")
            Text(
                text = "Bonnes réponses : ${session.correctCount} / ${session.script.activities.size}",
                style = MaterialTheme.typography.bodyLarge,
            )
            SpacerH(8)
            Text(
                text = session.script.outline.objectiveFr,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SpacerH(16)
            PrimaryButton(
                text = "Retour au catalogue",
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth(),
            )
            SpacerH(8)
            SecondaryButton(
                text = "Recommencer ce dialogue",
                onClick = onRestart,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
