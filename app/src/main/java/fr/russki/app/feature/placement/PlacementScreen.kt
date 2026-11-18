package fr.russki.app.feature.placement

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
import androidx.compose.material3.TextButton
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
import fr.russki.app.domain.model.LoadState
import fr.russki.app.domain.placement.PlacementPhase
import fr.russki.app.domain.placement.PlacementResult
import fr.russki.app.domain.placement.PlacementSessionState
import fr.russki.app.domain.placement.PlacementStoredResults
import fr.russki.app.feature.stats.TrackStudyTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PlacementRoute(
    onBack: () -> Unit,
    onOpenRecommendedLesson: (String) -> Unit = {},
    viewModel: PlacementViewModel = viewModel(factory = PlacementViewModel.Factory()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TrackStudyTime()
    Column(Modifier.fillMaxSize()) {
        IconButton(onClick = {
            val session = (state as? LoadState.Ready)?.value?.session
            if (session != null && session.phase != PlacementPhase.Result) {
                viewModel.abandonToHub()
            } else {
                onBack()
            }
        }) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
        }
        PlacementScreen(
            state = state,
            onRetry = viewModel::refreshHub,
            onStart = viewModel::startTest,
            onBegin = viewModel::beginQuestions,
            onSubmit = viewModel::submitAnswer,
            onContinue = viewModel::advanceAfterFeedback,
            onQuit = viewModel::abandonToHub,
            onOpenLesson = onOpenRecommendedLesson,
            onBackToProfile = onBack,
        )
    }
}

@Composable
fun PlacementScreen(
    state: LoadState<PlacementUi>,
    onRetry: () -> Unit,
    onStart: () -> Unit,
    onBegin: () -> Unit,
    onSubmit: (String) -> Unit,
    onContinue: () -> Unit,
    onQuit: () -> Unit,
    onOpenLesson: (String) -> Unit,
    onBackToProfile: () -> Unit,
) {
    ScreenScaffold(
        title = "Test de placement",
        subtitle = "Estimation indicative hors ligne — pas une certification CECRL.",
    ) {
        when (state) {
            LoadState.Loading -> LoadingState()
            is LoadState.Error -> ErrorState(message = state.message, onRetry = onRetry)
            is LoadState.Empty -> ErrorState(message = state.message, onRetry = onRetry)
            is LoadState.Ready -> FadeInContent(visible = true) {
                val ui = state.value
                val session = ui.session
                when {
                    session == null -> HubPhase(
                        stored = ui.stored,
                        starting = ui.starting,
                        onStart = onStart,
                        onBack = onBackToProfile,
                    )
                    session.phase == PlacementPhase.Introduction -> IntroPhase(
                        questionCount = session.questions.size,
                        onBegin = onBegin,
                        onQuit = onQuit,
                    )
                    session.phase == PlacementPhase.Question -> QuestionPhase(
                        session = session,
                        onSubmit = onSubmit,
                        onContinue = onContinue,
                        onQuit = onQuit,
                    )
                    session.phase == PlacementPhase.Result -> ResultPhase(
                        session = session,
                        stored = ui.stored,
                        onOpenLesson = onOpenLesson,
                        onRetake = onStart,
                        onDone = onBackToProfile,
                    )
                }
            }
        }
    }
}

@Composable
private fun HubPhase(
    stored: PlacementStoredResults,
    starting: Boolean,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SoftPanel {
            SectionLabel("À propos")
            Text(
                text = "Ce test estime un point de départ dans le parcours débutant " +
                    "à partir de notions déjà présentes dans l’application. " +
                    "Ce n’est ni un examen officiel ni un niveau A1 CECRL.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        stored.latest?.let { latest ->
            SoftPanel {
                SectionLabel("Dernier résultat affiché")
                ResultSummary(latest)
                stored.previous?.let { prev ->
                    SpacerH(10)
                    Text(
                        text = "Résultat précédent conservé : " +
                            "${prev.correctCount}/${prev.totalQuestions} " +
                            "(${formatWhen(prev.completedAtEpochMs)}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        PrimaryButton(
            text = if (stored.latest == null) "Commencer le test" else "Repasser le test",
            onClick = onStart,
            enabled = !starting,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Retour")
        }
        SpacerH(8)
    }
}

@Composable
private fun IntroPhase(
    questionCount: Int,
    onBegin: () -> Unit,
    onQuit: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SoftPanel {
            SectionLabel("Introduction")
            Text(
                text = "Environ $questionCount questions, une à la fois. " +
                    "Répondez selon vos connaissances actuelles. " +
                    "Vous pouvez quitter à tout moment : la progression des leçons " +
                    "et dialogues n’est pas modifiée.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        PrimaryButton(
            text = "Démarrer les questions",
            onClick = onBegin,
            modifier = Modifier.fillMaxWidth(),
        )
        SecondaryButton(
            text = "Quitter sans enregistrer",
            onClick = onQuit,
            modifier = Modifier.fillMaxWidth(),
        )
        SpacerH(8)
    }
}

@Composable
private fun QuestionPhase(
    session: PlacementSessionState,
    onSubmit: (String) -> Unit,
    onContinue: () -> Unit,
    onQuit: () -> Unit,
) {
    val question = session.currentQuestion ?: return
    val exercise = question.exercise
    var selected by remember(question.id) { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetaChip("Question ${session.questionIndex + 1} / ${session.questions.size}")
        ProgressLine(session.progressFraction)
        SoftPanel {
            Text(
                text = question.skill.titleFr,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            SpacerH(8)
            Text(exercise.promptFr, style = MaterialTheme.typography.titleMedium)
            SpacerH(10)
            if (exercise.stimulus.isNotBlank() && exercise.stimulus != "…") {
                CyrillicSample(exercise.stimulus)
            }
            SpacerH(6)
            Text(
                text = typeLabel(exercise.type),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val grade = session.currentGrade
        val feedbackState = grade?.let { feedbackStateFrom(it.isCorrect) } ?: AnswerFeedbackState.Idle
        val matcher = remember { AnswerMatcher() }
        SoftPanel {
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
        if (grade != null) {
            AnswerFeedbackBanner(
                state = feedbackState,
                correctAnswerLabel = grade.acceptedAnswers.first(),
                explanation = grade.explanationFr,
                onContinue = onContinue,
                pedagogy = fr.russki.app.core.ui.components.pedagogicalExtrasFrom(exercise, grade),
            )
        } else {
            PrimaryButton(
                text = "Valider",
                onClick = { onSubmit(selected.orEmpty()) },
                enabled = selected != null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        TextButton(onClick = onQuit, modifier = Modifier.fillMaxWidth()) {
            Text("Quitter (progression des leçons intacte)")
        }
        SpacerH(8)
    }
}

@Composable
private fun ResultPhase(
    session: PlacementSessionState,
    stored: PlacementStoredResults,
    onOpenLesson: (String) -> Unit,
    onRetake: () -> Unit,
    onDone: () -> Unit,
) {
    val result = session.result
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SoftPanel {
            SectionLabel("Résultat (estimation)")
            if (result == null) {
                Text("Calcul du résultat…", style = MaterialTheme.typography.bodyLarge)
            } else {
                ResultSummary(result)
            }
        }
        result?.let { r ->
            SoftPanel {
                SectionLabel("Compétences évaluées")
                r.skillScores.forEach { score ->
                    Text(
                        text = "${score.skill.titleFr} : ${score.correct}/${score.answered}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    SpacerH(4)
                }
            }
            SoftPanel {
                SectionLabel("Recommandation")
                Text(
                    r.recommendation.lessonTitleFr,
                    style = MaterialTheme.typography.titleMedium,
                )
                SpacerH(6)
                Text(
                    r.recommendation.rationaleFr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SpacerH(8)
                Text(
                    text = "Ce résultat n’est pas une certification CECRL.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            stored.previous?.let { prev ->
                SoftPanel {
                    SectionLabel("Résultat précédent conservé")
                    Text(
                        text = "${prev.correctCount}/${prev.totalQuestions} — " +
                            formatWhen(prev.completedAtEpochMs),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (r.recommendation.lessonId.isNotBlank()) {
                PrimaryButton(
                    text = "Ouvrir la leçon recommandée",
                    onClick = { onOpenLesson(r.recommendation.lessonId) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        SecondaryButton(
            text = "Repasser le test",
            onClick = onRetake,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Terminer")
        }
        SpacerH(8)
    }
}

@Composable
private fun ResultSummary(result: PlacementResult) {
    Text(
        text = "Score : ${result.correctCount} / ${result.totalQuestions} (${result.percent} %)",
        style = MaterialTheme.typography.titleMedium,
    )
    SpacerH(6)
    Text(
        text = "Enregistré le ${formatWhen(result.completedAtEpochMs)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SpacerH(8)
    Text(
        text = "Point de départ suggéré : ${result.recommendation.lessonTitleFr}",
        style = MaterialTheme.typography.bodyLarge,
    )
}

private fun formatWhen(epochMs: Long): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        .withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(epochMs))
}

private fun typeLabel(type: InteractiveExerciseType): String = when (type) {
    InteractiveExerciseType.MultipleChoice -> "QCM"
    InteractiveExerciseType.MatchTranslation -> "Association / traduction"
    InteractiveExerciseType.FillBlank -> "Texte à trous"
    InteractiveExerciseType.WordOrder -> "Remise en ordre"
    InteractiveExerciseType.LetterRecognition -> "Reconnaissance de lettre"
}
