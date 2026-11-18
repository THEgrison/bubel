package fr.russki.app.feature.quiz

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.russki.app.core.ui.components.CyrillicSample
import fr.russki.app.core.ui.components.EmptyState
import fr.russki.app.core.ui.components.ErrorState
import fr.russki.app.core.ui.components.LoadingState
import fr.russki.app.core.ui.components.MetaChip
import fr.russki.app.core.ui.components.ScreenScaffold
import fr.russki.app.core.ui.components.SoftPanel
import fr.russki.app.core.ui.components.SpacerH
import fr.russki.app.domain.model.LoadState
import fr.russki.app.domain.quiz.QuizQuestion
import fr.russki.app.domain.quiz.QuizQuestionType
import fr.russki.app.domain.quiz.QuizSessionStatus

@Composable
fun QuizRoute(
    onBack: () -> Unit,
    resumeIfPossible: Boolean = true,
    viewModel: QuizViewModel = viewModel(factory = QuizViewModel.Factory()),
) {
    LaunchedEffect(Unit) {
        if (resumeIfPossible) viewModel.resumeInterrupted() else viewModel.startNew()
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    QuizScreen(
        state = state,
        onBack = {
            viewModel.abandon()
            onBack()
        },
        onRetry = { viewModel.startNew() },
        onSubmit = viewModel::submitAnswer,
        onNext = viewModel::next,
        onRestart = { viewModel.startNew() },
    )
}

@Composable
fun QuizScreen(
    state: LoadState<QuizUiState>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSubmit: (String) -> Unit,
    onNext: () -> Unit,
    onRestart: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
        }
        when (state) {
            LoadState.Loading -> LoadingState()
            is LoadState.Error -> ErrorState(message = state.message, onRetry = onRetry)
            is LoadState.Empty -> EmptyState(title = "Quiz vide", body = state.message)
            is LoadState.Ready -> {
                val ui = state.value
                val session = ui.session
                if (session.status == QuizSessionStatus.Completed || session.isFinished) {
                    ScreenScaffold(title = "Quiz terminé", subtitle = "Session enregistrée") {
                        SoftPanel {
                            Text(
                                "Score : ${session.score} / ${session.totalCount}",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            SpacerH(12)
                            Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
                                Text("Nouvelle session")
                            }
                            SpacerH(8)
                            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                                Text("Retour")
                            }
                        }
                    }
                } else {
                    val question = session.currentQuestion
                    if (question == null) {
                        EmptyState(title = "Fin", body = "Plus de question.")
                    } else {
                        ScreenScaffold(
                            title = "Quiz",
                            subtitle = "Question ${session.currentIndex + 1} / ${session.totalCount} · score ${session.score}",
                        ) {
                            QuestionPane(
                                question = question,
                                revealed = ui.showAnswerReveal,
                                lastCorrect = ui.lastGrade?.record?.isCorrect,
                                explanation = if (ui.showAnswerReveal) question.explanationFr else null,
                                expected = if (ui.showAnswerReveal) question.expectedAnswer else null,
                                onSubmit = onSubmit,
                                onNext = onNext,
                                answered = session.isCurrentAnswered,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionPane(
    question: QuizQuestion,
    revealed: Boolean,
    lastCorrect: Boolean?,
    explanation: String?,
    expected: String?,
    onSubmit: (String) -> Unit,
    onNext: () -> Unit,
    answered: Boolean,
) {
    var textAnswer by rememberSaveable(question.id) { mutableStateOf("") }
    var selectedTokens by androidx.compose.runtime.remember(question.id) {
        mutableStateOf(listOf<String>())
    }
    var remainingTokens by androidx.compose.runtime.remember(question.id) {
        mutableStateOf(question.shuffledTokens)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetaChip(question.type.name)
        SoftPanel {
            Text(question.promptFr, style = MaterialTheme.typography.titleMedium)
            SpacerH(10)
            if (!(question.hideAnswerUntilGraded && !revealed)) {
                CyrillicSample(question.stimulus)
            } else {
                Text(
                    "Stimulus masqué jusqu'à la correction (compréhension orale).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SpacerH(8)
            if (question.audioAssetPath == null) {
                MetaChip("Pas d'audio natif pour cette question")
            } else {
                MetaChip("Audio référencé — lecture si fichier présent")
            }
        }

        when (question.type) {
            QuizQuestionType.WordOrder -> {
                SoftPanel {
                    Text(
                        selectedTokens.joinToString(" ").ifBlank { "…" },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    SpacerH(8)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        remainingTokens.forEach { token ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    if (!answered) {
                                        selectedTokens = selectedTokens + token
                                        remainingTokens = remainingTokens - token
                                    }
                                },
                                label = { Text(token) },
                            )
                        }
                    }
                    if (!answered) {
                        SpacerH(8)
                        OutlinedButton(
                            onClick = {
                                selectedTokens = emptyList()
                                remainingTokens = question.shuffledTokens
                            },
                        ) { Text("Réinitialiser") }
                    }
                }
            }
            QuizQuestionType.Cloze -> {
                OutlinedTextField(
                    value = textAnswer,
                    onValueChange = { if (!answered) textAnswer = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !answered,
                    label = { Text("Votre réponse") },
                )
            }
            else -> {
                if (question.choices.isNotEmpty()) {
                    question.choices.forEach { choice ->
                        SoftPanel(
                            panelModifier = Modifier.clickable(
                                enabled = !answered,
                                role = Role.Button,
                                onClick = { onSubmit(choice) },
                            ),
                        ) {
                            Text(choice, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = textAnswer,
                        onValueChange = { if (!answered) textAnswer = it },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !answered,
                        label = { Text("Votre réponse") },
                    )
                }
            }
        }

        if (!answered) {
            val canSubmit = when (question.type) {
                QuizQuestionType.WordOrder -> selectedTokens.size == question.shuffledTokens.size
                QuizQuestionType.Cloze -> textAnswer.isNotBlank()
                else -> question.choices.isEmpty() && textAnswer.isNotBlank()
            }
            if (canSubmit || question.type == QuizQuestionType.Cloze ||
                (question.choices.isEmpty() && question.type != QuizQuestionType.WordOrder)
            ) {
                Button(
                    onClick = {
                        val answer = when (question.type) {
                            QuizQuestionType.WordOrder -> selectedTokens.joinToString(" ")
                            else -> textAnswer
                        }
                        if (answer.isNotBlank()) onSubmit(answer)
                    },
                    enabled = when (question.type) {
                        QuizQuestionType.WordOrder ->
                            selectedTokens.size == question.shuffledTokens.size
                        else -> textAnswer.isNotBlank()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Valider") }
            }
        }

        if (revealed) {
            SoftPanel {
                Text(
                    if (lastCorrect == true) "Correct" else "Incorrect",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (lastCorrect == true) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                if (expected != null) {
                    SpacerH(6)
                    Text("Réponse attendue : $expected", style = MaterialTheme.typography.bodyLarge)
                }
                if (explanation != null) {
                    SpacerH(8)
                    Text(explanation, style = MaterialTheme.typography.bodyMedium)
                }
                SpacerH(8)
                Icon(
                    Icons.AutoMirrored.Outlined.VolumeOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Prononciation : disponible uniquement si un fichier audio natif est embarqué.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text("Question suivante")
            }
        }
        SpacerH(12)
    }
}
