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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
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
import fr.russki.app.core.ui.components.pedagogicalExtrasFrom
import fr.russki.app.data.content.AssetAudioAvailability
import fr.russki.app.domain.exercise.AnswerMatcher
import fr.russki.app.domain.exercise.InteractiveExerciseType
import fr.russki.app.domain.lesson.LessonPhase
import fr.russki.app.domain.lesson.LessonSessionState
import fr.russki.app.domain.lesson.SpeechRegister
import fr.russki.app.domain.lesson.TeachableItem
import fr.russki.app.domain.model.LoadState
import fr.russki.app.feature.audio.LocalAudioControls
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
            onBeginDiscovery = viewModel::beginDiscovery,
            onBeginPractice = viewModel::beginPractice,
            onReveal = viewModel::revealHint,
            onSubmit = viewModel::submit,
            onContinue = viewModel::continueAfterFeedback,
            onBeginFinal = viewModel::beginFinalCheck,
            onSkipFinal = viewModel::skipToSummary,
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
    onBeginDiscovery: () -> Unit,
    onBeginPractice: () -> Unit,
    onReveal: () -> Unit,
    onSubmit: (String) -> Unit,
    onContinue: () -> Unit,
    onBeginFinal: () -> Unit,
    onSkipFinal: () -> Unit,
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
                subtitle = phaseSubtitle(session),
            ) {
                ProgressLine(progress = session.progressFraction)
                SpacerH(16)
                when (session.phase) {
                    LessonPhase.Introduction -> IntroPhase(
                        session = session,
                        onBeginDiscovery = onBeginDiscovery,
                        onOpenLegacyAlphabet = onOpenLegacyAlphabet,
                        onOpenLegacyGreetings = onOpenLegacyGreetings,
                    )
                    LessonPhase.Discovery -> DiscoveryPhase(
                        session = session,
                        onBeginPractice = onBeginPractice,
                    )
                    LessonPhase.Practice, LessonPhase.Rematch, LessonPhase.FinalCheck -> ExercisePhase(
                        session = session,
                        onSubmit = onSubmit,
                        onContinue = onContinue,
                        onReveal = onReveal,
                    )
                    LessonPhase.Recap -> RecapPhase(
                        session = session,
                        onBeginFinal = onBeginFinal,
                        onSkipFinal = onSkipFinal,
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

private fun phaseSubtitle(session: LessonSessionState): String = when (session.phase) {
    LessonPhase.Introduction -> session.lesson.objectiveFr
    LessonPhase.Discovery -> "Découverte — écoutez et lisez avant les exercices"
    LessonPhase.Practice -> "Pratique"
    LessonPhase.Rematch -> "Rattrapage (${session.rematchPendingCount} restant(s))"
    LessonPhase.Recap -> "Récapitulation"
    LessonPhase.FinalCheck -> "Test final (sans indices)"
    LessonPhase.Summary, LessonPhase.Completed -> "Bilan de la leçon"
}

@Composable
private fun IntroPhase(
    session: LessonSessionState,
    onBeginDiscovery: () -> Unit,
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
            MetaChip("${session.teachables.size} notions · ${session.practiceExercises.size} exercices")
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
            text = if (session.teachables.isEmpty()) {
                "Passer à la pratique"
            } else {
                "Voir ce que vous allez apprendre"
            },
            onClick = onBeginDiscovery,
            modifier = Modifier.fillMaxWidth(),
        )
        SpacerH(12)
    }
}

@Composable
private fun DiscoveryPhase(
    session: LessonSessionState,
    onBeginPractice: () -> Unit,
) {
    val context = LocalContext.current
    val availability = remember { AssetAudioAvailability(context) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SoftPanel {
            SectionLabel("À apprendre")
            Text(
                "Lisez et écoutez chaque élément avant les exercices. " +
                    "Vous ne serez interrogé que sur ce contenu.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (session.teachables.isEmpty()) {
            SoftPanel {
                Text(
                    "Pas de fiche de découverte dédiée — les exercices portent sur le thème de la leçon.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            session.teachables.forEach { item ->
                TeachableCard(item = item, availability = availability)
            }
        }
        PrimaryButton(
            text = "Commencer les exercices",
            onClick = onBeginPractice,
            modifier = Modifier.fillMaxWidth(),
        )
        SpacerH(12)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TeachableCard(
    item: TeachableItem,
    availability: AssetAudioAvailability,
) {
    SoftPanel {
        CyrillicSample(item.textRu)
        SpacerH(6)
        Text(item.translationFr, style = MaterialTheme.typography.titleMedium)
        SpacerH(4)
        Text(
            text = buildString {
                append("Aide de lecture : ")
                append(item.transliteration)
                if (item.stressUnknown) append(" (accent non marqué)")
                append(" — translittération, pas une transcription phonétique exacte")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SpacerH(6)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (item.register) {
                SpeechRegister.Informal -> MetaChip("Informel")
                SpeechRegister.Formal -> MetaChip("Formel / poli")
                SpeechRegister.Neutral -> MetaChip("Neutre")
            }
        }
        item.usageFr?.let {
            SpacerH(8)
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
        item.grammarNoteFr?.let {
            SpacerH(6)
            Text("Grammaire : $it", style = MaterialTheme.typography.bodySmall)
        }
        item.confusionFr?.let {
            SpacerH(6)
            Text("Attention : $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        if (item.exampleRu != null) {
            SpacerH(8)
            Text(item.exampleRu, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            item.exampleFr?.let { fr ->
                SpacerH(2)
                Text(fr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        SpacerH(10)
        LocalAudioControls(audio = availability.isAvailable(item.audioAssetPath))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExercisePhase(
    session: LessonSessionState,
    onSubmit: (String) -> Unit,
    onContinue: () -> Unit,
    onReveal: () -> Unit,
) {
    val exercise = session.currentExercise ?: return
    val matcher = remember { AnswerMatcher() }
    val context = LocalContext.current
    val availability = remember { AssetAudioAvailability(context) }
    var selected by remember(exercise.id, session.phase) { mutableStateOf<String?>(null) }
    var showSheets by remember { mutableStateOf(false) }
    var orderTokens by remember(exercise.id, session.phase) {
        mutableStateOf(
            if (exercise.type == InteractiveExerciseType.WordOrder) {
                exercise.choices.shuffled()
            } else {
                emptyList()
            },
        )
    }
    var builtOrder by remember(exercise.id, session.phase) { mutableStateOf(listOf<String>()) }
    val grade = session.currentGrade
    val feedbackState = when {
        grade == null -> AnswerFeedbackState.Idle
        grade.revealedBeforeAnswer -> AnswerFeedbackState.Incorrect
        else -> feedbackStateFrom(grade.isCorrect)
    }
    val queueSize = session.currentPracticeQueue.size
    val phaseLabel = when (session.phase) {
        LessonPhase.Rematch -> "Rattrapage"
        LessonPhase.FinalCheck -> "Test final"
        else -> "Exercice"
    }

    if (showSheets && session.teachables.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showSheets = false },
            confirmButton = {
                TextButton(onClick = { showSheets = false }) {
                    Text("Reprendre l'exercice")
                }
            },
            title = { Text("Fiches de la leçon") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "Consultez sans perdre votre progression.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    session.teachables.forEach { item ->
                        TeachableCard(item = item, availability = availability)
                    }
                }
            },
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetaChip("$phaseLabel ${session.exerciseIndex + 1} / $queueSize")
        if (session.teachables.isNotEmpty() && session.phase != LessonPhase.FinalCheck) {
            SecondaryButton(
                text = "Revoir les fiches",
                onClick = { showSheets = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SoftPanel {
            Text(exercise.promptFr, style = MaterialTheme.typography.titleMedium)
            SpacerH(10)
            val hideStimulus = session.phase == LessonPhase.FinalCheck &&
                exercise.type != InteractiveExerciseType.LetterRecognition &&
                grade == null
            if (!hideStimulus && exercise.stimulus.isNotBlank() && exercise.stimulus != "…") {
                CyrillicSample(exercise.stimulus)
            } else if (hideStimulus) {
                Text(
                    "Répondez sans regarder la solution. Le russe s’affichera après votre tentative.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                    Text("Touchez les mots dans le bon ordre", style = MaterialTheme.typography.bodyMedium)
                    SpacerH(8)
                    Text(
                        builtOrder.joinToString(" ").ifBlank { "…" },
                        style = MaterialTheme.typography.titleLarge,
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
                    val showChoices = session.phase != LessonPhase.FinalCheck ||
                        exercise.choices.isNotEmpty()
                    if (showChoices && exercise.choices.isNotEmpty()) {
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
        }

        if (grade != null) {
            val pedagogy = pedagogicalExtrasFrom(exercise, grade)
            AnswerFeedbackBanner(
                state = feedbackState,
                correctAnswerLabel = grade.acceptedAnswers.first(),
                explanation = grade.explanationFr,
                continueLabel = "Continuer",
                onContinue = onContinue,
                pedagogy = pedagogy,
                customMessage = when {
                    grade.revealedBeforeAnswer -> null
                    grade.isCorrect -> "Correct !"
                    else -> "Incorrect"
                },
                audioContent = {
                    LocalAudioControls(
                        audio = availability.isAvailable(
                            grade.audioAssetPath ?: exercise.audioAssetPath,
                        ),
                    )
                },
            )
        } else {
            if (session.phase != LessonPhase.FinalCheck) {
                SecondaryButton(
                    text = if (session.revealedForCurrent) "Solution affichée" else "Voir la solution (ne compte pas)",
                    onClick = onReveal,
                    enabled = !session.revealedForCurrent,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (session.revealedForCurrent) {
                    SoftPanel {
                        Text("Réponse attendue : ${exercise.primaryExpected}", style = MaterialTheme.typography.titleMedium)
                        exercise.translationFr?.let {
                            SpacerH(4)
                            Text("Traduction : $it", style = MaterialTheme.typography.bodyMedium)
                        }
                        exercise.transliteration?.let {
                            SpacerH(4)
                            Text(
                                "Aide de lecture : $it — translittération, pas une transcription phonétique exacte",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        SpacerH(8)
                        LocalAudioControls(audio = availability.isAvailable(exercise.audioAssetPath))
                    }
                }
            }
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
private fun RecapPhase(
    session: LessonSessionState,
    onBeginFinal: () -> Unit,
    onSkipFinal: () -> Unit,
) {
    val context = LocalContext.current
    val availability = remember { AssetAudioAvailability(context) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SoftPanel {
            SectionLabel("Récapitulatif")
            Text(session.lesson.summaryFr, style = MaterialTheme.typography.bodyLarge)
            SpacerH(8)
            Text(
                "Première passe : ${session.correctCount} / ${session.practiceExercises.size} maîtrisés",
                style = MaterialTheme.typography.titleMedium,
            )
            if (session.initiallyFailedIds.isNotEmpty()) {
                SpacerH(6)
                Text(
                    "Corrigés en rattrapage : ${session.recoveredIds.size} · " +
                        "Encore difficiles : ${session.stillHardIds.size}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            session.rematchDiagnostic?.let { diag ->
                SpacerH(8)
                Text(
                    "Diagnostic rattrapage :\n$diag",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        session.teachables.forEach { item ->
            TeachableCard(item = item, availability = availability)
        }
        PrimaryButton(
            text = "Passer le test final",
            onClick = onBeginFinal,
            modifier = Modifier.fillMaxWidth(),
        )
        SecondaryButton(
            text = "Terminer sans test final",
            onClick = onSkipFinal,
            modifier = Modifier.fillMaxWidth(),
        )
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
            SectionLabel("Leçon terminée")
            Text(session.lesson.summaryFr, style = MaterialTheme.typography.bodyLarge)
            SpacerH(12)
            Text(
                "Score première passe : ${session.correctCount} / ${session.practiceExercises.size}",
                style = MaterialTheme.typography.titleMedium,
            )
            if (session.finalGrades.isNotEmpty()) {
                SpacerH(6)
                Text(
                    "Test final : ${session.finalCorrectCount} / ${session.finalExercises.size}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            SpacerH(6)
            Text(
                "Rattrapage réussi : ${session.recoveredIds.size} · " +
                    "À consolider (SRS) : ${session.stillHardIds.size}",
                style = MaterialTheme.typography.bodyMedium,
            )
            SpacerH(8)
            Text(
                "La fin du cours n’équivaut pas à une maîtrise durable — les révisions SRS s’en chargent.",
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
