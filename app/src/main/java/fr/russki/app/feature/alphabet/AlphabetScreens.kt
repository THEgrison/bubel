package fr.russki.app.feature.alphabet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.russki.app.core.ui.components.AnswerChoiceButton
import fr.russki.app.core.ui.components.AnswerFeedbackBanner
import fr.russki.app.core.ui.components.AnswerFeedbackState
import fr.russki.app.core.ui.components.CyrillicSample
import fr.russki.app.core.ui.components.EmptyState
import fr.russki.app.core.ui.components.ErrorState
import fr.russki.app.core.ui.components.FadeInContent
import fr.russki.app.core.ui.components.LoadingState
import fr.russki.app.core.ui.components.MetaChip
import fr.russki.app.core.ui.components.PrimaryButton
import fr.russki.app.core.ui.components.ScreenScaffold
import fr.russki.app.core.ui.components.SecondaryButton
import fr.russki.app.core.ui.components.SectionLabel
import fr.russki.app.core.ui.components.SoftPanel
import fr.russki.app.core.ui.components.SpacerH
import fr.russki.app.core.ui.components.feedbackStateFrom
import fr.russki.app.domain.content.AlphabetExercise
import fr.russki.app.domain.content.AlphabetLetter
import fr.russki.app.domain.model.LoadState

@Composable
fun AlphabetRoute(
    onBack: () -> Unit,
    onOpenLetter: (String) -> Unit,
    onOpenExercises: () -> Unit,
    viewModel: AlphabetViewModel = viewModel(factory = AlphabetViewModel.Factory()),
) {
    val state by viewModel.overview.collectAsStateWithLifecycle()
    AlphabetOverviewScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::loadOverview,
        onOpenLetter = onOpenLetter,
        onOpenExercises = onOpenExercises,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlphabetOverviewScreen(
    state: LoadState<AlphabetOverview>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenLetter: (String) -> Unit,
    onOpenExercises: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
        }
        ScreenScaffold(
            title = "Alphabet russe",
            subtitle = "33 lettres · hors ligne",
        ) {
            when (state) {
                LoadState.Loading -> LoadingState()
                is LoadState.Error -> ErrorState(message = state.message, onRetry = onRetry)
                is LoadState.Empty -> EmptyState(title = "Alphabet vide", body = state.message)
                is LoadState.Ready -> FadeInContent(visible = true) {
                    val data = state.value
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        SoftPanel {
                            Text(data.description, style = MaterialTheme.typography.bodyLarge)
                            SpacerH(10)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MetaChip("v${data.version}")
                                MetaChip("${data.letters.size} lettres")
                                MetaChip("${data.exerciseCount} exercices")
                            }
                        }
                        PrimaryButton(
                            text = "S'exercer",
                            onClick = onOpenExercises,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        SectionLabel("Lettres")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            data.letters.forEach { letter ->
                                FilterChip(
                                    selected = false,
                                    onClick = { onOpenLetter(letter.id) },
                                    label = {
                                        Text("${letter.uppercase} ${letter.lowercase}")
                                    },
                                )
                            }
                        }
                        Text(
                            text = "Aucun enregistrement audio natif n'est encore embarqué pour ces lettres.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SpacerH(12)
                    }
                }
            }
        }
    }
}

@Composable
fun LetterDetailRoute(
    letterId: String,
    onBack: () -> Unit,
    viewModel: AlphabetViewModel = viewModel(factory = AlphabetViewModel.Factory()),
) {
    LaunchedEffect(letterId) { viewModel.loadLetter(letterId) }
    val state by viewModel.letter.collectAsStateWithLifecycle()
    LetterDetailScreen(
        state = state,
        onBack = onBack,
        onRetry = { viewModel.loadLetter(letterId) },
    )
}

@Composable
fun LetterDetailScreen(
    state: LoadState<LetterDetail>,
    onBack: () -> Unit,
    onRetry: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize()) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
        }
        when (state) {
            LoadState.Loading -> LoadingState()
            is LoadState.Error -> ErrorState(message = state.message, onRetry = onRetry)
            is LoadState.Empty -> EmptyState(title = "Introuvable", body = state.message)
            is LoadState.Ready -> {
                val detail = state.value
                ScreenScaffold(
                    title = "${detail.letter.uppercase} ${detail.letter.lowercase}",
                    subtitle = "Lettre ${detail.letter.order} · ${detail.letter.nameRu}",
                ) {
                    LetterBody(detail.letter, hasAudio = detail.audio != null)
                }
            }
        }
    }
}

@Composable
private fun LetterBody(letter: AlphabetLetter, hasAudio: Boolean) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SoftPanel {
            CyrillicSample("${letter.uppercase}  ${letter.lowercase}")
            SpacerH(8)
            Text("Nom russe : ${letter.nameRu}", style = MaterialTheme.typography.titleMedium)
            SpacerH(12)
            if (hasAudio) {
                MetaChip("Audio natif disponible")
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Outlined.VolumeOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "  Pas d'enregistrement natif pour cette lettre pour le moment.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        SoftPanel {
            SectionLabel("Guide de prononciation")
            Text(letter.pronunciationGuideFr, style = MaterialTheme.typography.bodyLarge)
        }
        SoftPanel {
            SectionLabel("Son dans les mots")
            Text(letter.soundInWordsFr, style = MaterialTheme.typography.bodyLarge)
        }
        SoftPanel {
            SectionLabel("Exemple")
            CyrillicSample(letter.exampleWordRu)
            SpacerH(6)
            Text(letter.exampleWordFr, style = MaterialTheme.typography.bodyLarge)
        }
        SpacerH(8)
    }
}

@Composable
fun AlphabetExercisesRoute(
    onBack: () -> Unit,
    viewModel: AlphabetViewModel = viewModel(factory = AlphabetViewModel.Factory()),
) {
    LaunchedEffect(Unit) { viewModel.startExercises() }
    val state by viewModel.session.collectAsStateWithLifecycle()
    AlphabetExercisesScreen(
        state = state,
        onBack = onBack,
        onSelect = viewModel::selectOption,
        onNext = viewModel::nextExercise,
        onRestart = viewModel::startExercises,
    )
}

@Composable
fun AlphabetExercisesScreen(
    state: LoadState<ExerciseSession>,
    onBack: () -> Unit,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit,
    onRestart: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
        }
        ScreenScaffold(
            title = "Exercices",
            subtitle = "Alphabet · hors ligne",
        ) {
            when (state) {
                LoadState.Loading -> LoadingState()
                is LoadState.Error -> ErrorState(message = state.message, onRetry = onRestart)
                is LoadState.Empty -> EmptyState(title = "Aucun exercice", body = state.message)
                is LoadState.Ready -> {
                    val session = state.value
                    if (session.finished) {
                        SoftPanel {
                            Text(
                                "Terminé : ${session.score} / ${session.exercises.size}",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            SpacerH(12)
                            PrimaryButton(
                                text = "Recommencer",
                                onClick = onRestart,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            SpacerH(8)
                            SecondaryButton(
                                text = "Retour à l'alphabet",
                                onClick = onBack,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    } else {
                        val exercise = session.current
                        if (exercise == null) {
                            EmptyState(title = "Fin", body = "Plus d'exercice.")
                        } else {
                            ExerciseCard(
                                exercise = exercise,
                                progressLabel = "${session.index + 1} / ${session.exercises.size}",
                                answered = session.answered,
                                selectedIndex = session.selectedIndex,
                                onSelect = onSelect,
                                onNext = onNext,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: AlphabetExercise,
    progressLabel: String,
    answered: Boolean,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetaChip(progressLabel)
        SoftPanel {
            Text(exercise.promptFr, style = MaterialTheme.typography.titleMedium)
            SpacerH(12)
            CyrillicSample(exercise.stimulus)
        }
        val feedbackState = if (!answered || selectedIndex == null) {
            AnswerFeedbackState.Idle
        } else {
            feedbackStateFrom(selectedIndex == exercise.correctIndex)
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            exercise.options.forEachIndexed { index, option ->
                AnswerChoiceButton(
                    label = option,
                    selected = selectedIndex == index,
                    isCorrectOption = answered && index == exercise.correctIndex,
                    feedbackState = feedbackState,
                    enabled = !answered,
                    onClick = { onSelect(index) },
                )
            }
        }
        if (answered && selectedIndex != null) {
            AnswerFeedbackBanner(
                state = feedbackState,
                correctAnswerLabel = exercise.options.getOrNull(exercise.correctIndex),
                explanation = exercise.explanationFr,
                onContinue = onNext,
            )
        }
        SpacerH(8)
    }
}
