package fr.russki.app.feature.lesson

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import fr.russki.app.domain.exercise.AnswerMatcher
import fr.russki.app.domain.exercise.InteractiveExerciseType
import fr.russki.app.domain.lesson.LessonPhase
import fr.russki.app.domain.lesson.LessonSessionState
import fr.russki.app.domain.model.LoadState
import fr.russki.app.feature.stats.TrackStudyTime

@Composable
fun LessonRoute(
    lessonId: String,
    onBack: () -> Unit,
    onOpenLegacyAlphabet: () -> Unit,
    onOpenLegacyGreetings: () -> Unit,
    viewModel: LessonViewModel = viewModel(factory = LessonViewModel.Factory(lessonId)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TrackStudyTime()
    Column(Modifier.fillMaxSize()) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
        }
        LessonScreen(
            state = state,
            onRetry = viewModel::start,
            onBegin = viewModel::beginExercises,
            onSubmit = viewModel::submit,
            onContinue = viewModel::continueAfterFeedback,
            onFinish = {
                viewModel.finish()
                onBack()
            },
            onOpenLegacyAlphabet = onOpenLegacyAlphabet,
            onOpenLegacyGreetings = onOpenLegacyGreetings,
        )
    }
}

@Composable
fun LessonScreen(
    state: LoadState<LessonSessionState>,
    onRetry: () -> Unit,
    onBegin: () -> Unit,
    onSubmit: (String) -> Unit,
    onContinue: () -> Unit,
    onFinish: () -> Unit,
    onOpenLegacyAlphabet: () -> Unit,
    onOpenLegacyGreetings: () -> Unit,
) {
    when (state) {
        LoadState.Loading -> LoadingState()
        is LoadState.Error -> ErrorState(message = state.message, onRetry = onRetry)
        is LoadState.Empty -> ErrorState(message = state.message, onRetry = onRetry)
        is LoadState.Ready -> FadeInContent(visible = true) {
            val session = state.value
            ScreenScaffold(
                title = session.lesson.titleFr,
                subtitle = session.lesson.objectiveFr,
            ) {
                ProgressLine(progress = session.progressFraction)
                SpacerH(16)
                when (session.phase) {
                    LessonPhase.Introduction -> IntroPhase(
                        session = session,
                        onBegin = onBegin,
                        onOpenLegacyAlphabet = onOpenLegacyAlphabet,
                        onOpenLegacyGreetings = onOpenLegacyGreetings,
                    )
                    LessonPhase.Exercise -> ExercisePhase(
                        session = session,
                        onSubmit = onSubmit,
                        onContinue = onContinue,
                    )
                    LessonPhase.Summary, LessonPhase.Completed -> SummaryPhase(
                        session = session,
                        onFinish = onFinish,
                    )
                }
            }
        }
    }
}

@Composable
private fun IntroPhase(
    session: LessonSessionState,
    onBegin: () -> Unit,
    onOpenLegacyAlphabet: () -> Unit,
    onOpenLegacyGreetings: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SoftPanel {
            SectionLabel("Introduction")
            Text(session.lesson.introductionFr, style = MaterialTheme.typography.bodyLarge)
        }
        SoftPanel {
            SectionLabel("Objectif")
            Text(session.lesson.objectiveFr, style = MaterialTheme.typography.bodyMedium)
            SpacerH(8)
            MetaChip("${session.exercises.size} exercices")
        }
        when (session.lesson.legacyRouteHint) {
            "lesson/alphabet" -> SecondaryButton(
                text = "Ouvrir le référentiel alphabet (hors leçon)",
                onClick = onOpenLegacyAlphabet,
                modifier = Modifier.fillMaxWidth(),
            )
            "lesson/vocabulary/greetings" -> SecondaryButton(
                text = "Ouvrir la liste salutations (hors leçon)",
                onClick = onOpenLegacyGreetings,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        PrimaryButton(
            text = "Commencer les exercices",
            onClick = onBegin,
            modifier = Modifier.fillMaxWidth(),
        )
        SpacerH(12)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExercisePhase(
    session: LessonSessionState,
    onSubmit: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val exercise = session.currentExercise ?: return
    val matcher = remember { AnswerMatcher() }
    var selected by remember(exercise.id) { mutableStateOf<String?>(null) }
    var orderTokens by remember(exercise.id) {
        mutableStateOf(
            if (exercise.type == InteractiveExerciseType.WordOrder) {
                exercise.choices.shuffled()
            } else {
                emptyList()
            },
        )
    }
    var builtOrder by remember(exercise.id) { mutableStateOf(listOf<String>()) }
    val grade = session.currentGrade
    val feedbackState = grade?.let { feedbackStateFrom(it.isCorrect) } ?: AnswerFeedbackState.Idle

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetaChip("Exercice ${session.exerciseIndex + 1} / ${session.exercises.size}")
        SoftPanel {
            Text(exercise.promptFr, style = MaterialTheme.typography.titleMedium)
            SpacerH(10)
            if (exercise.stimulus.isNotBlank() && exercise.stimulus != "…") {
                CyrillicSample(exercise.stimulus)
            }
            SpacerH(8)
            Text(
                text = typeLabel(exercise.type),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SoftPanel {
            when (exercise.type) {
                InteractiveExerciseType.WordOrder -> {
                    Text(
                        "Touchez les mots dans le bon ordre",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    SpacerH(8)
                    Text(
                        builtOrder.joinToString(" ").ifBlank { "…" },
                        style = MaterialTheme.typography.titleLarge,
                        color = when (feedbackState) {
                            AnswerFeedbackState.Correct ->
                                MaterialTheme.colorScheme.primary
                            AnswerFeedbackState.Incorrect ->
                                MaterialTheme.colorScheme.error
                            AnswerFeedbackState.Idle ->
                                MaterialTheme.colorScheme.onSurface
                        },
                    )
                    SpacerH(8)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        orderTokens.forEach { token ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    if (grade == null) {
                                        builtOrder = builtOrder + token
                                        orderTokens = orderTokens - token
                                    }
                                },
                                enabled = grade == null,
                                label = { Text(token) },
                            )
                        }
                    }
                    if (builtOrder.isNotEmpty() && grade == null) {
                        SecondaryButton(
                            text = "Réinitialiser l'ordre",
                            onClick = {
                                orderTokens = exercise.choices.shuffled()
                                builtOrder = emptyList()
                            },
                        )
                    }
                }
                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        exercise.choices.forEach { choice ->
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
            }
        }

        if (grade != null) {
            AnswerFeedbackBanner(
                state = feedbackState,
                correctAnswerLabel = grade.acceptedAnswers.first(),
                explanation = grade.explanationFr,
                continueLabel = "Continuer",
                onContinue = onContinue,
            )
        } else {
            val canSubmit = when (exercise.type) {
                InteractiveExerciseType.WordOrder ->
                    builtOrder.size == exercise.choices.size
                else -> selected != null
            }
            PrimaryButton(
                text = "Valider",
                onClick = {
                    val answer = when (exercise.type) {
                        InteractiveExerciseType.WordOrder -> builtOrder.joinToString(" ")
                        else -> selected.orEmpty()
                    }
                    onSubmit(answer)
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SpacerH(12)
    }
}

@Composable
private fun SummaryPhase(
    session: LessonSessionState,
    onFinish: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SoftPanel {
            SectionLabel("Acquis")
            Text(session.lesson.summaryFr, style = MaterialTheme.typography.bodyLarge)
            SpacerH(12)
            Text(
                text = "Score : ${session.correctCount} / ${session.exercises.size}",
                style = MaterialTheme.typography.titleMedium,
            )
            SpacerH(6)
            Text(
                text = "Ce score n'est pas une certification officielle.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PrimaryButton(
            text = "Terminer la leçon",
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
        )
        SpacerH(12)
    }
}

private fun typeLabel(type: InteractiveExerciseType): String = when (type) {
    InteractiveExerciseType.MultipleChoice -> "QCM"
    InteractiveExerciseType.MatchTranslation -> "Association / traduction"
    InteractiveExerciseType.FillBlank -> "Texte à trous"
    InteractiveExerciseType.WordOrder -> "Remise en ordre"
    InteractiveExerciseType.LetterRecognition -> "Reconnaissance de lettre"
}
