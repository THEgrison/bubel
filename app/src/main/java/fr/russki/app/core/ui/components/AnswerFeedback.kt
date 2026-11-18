package fr.russki.app.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.russki.app.core.ui.theme.RusskiFeedbackColors
import fr.russki.app.core.ui.theme.RusskiRadii
import fr.russki.app.core.ui.theme.RusskiThemeExtras

/**
 * État de correction partagé par tous les exercices évalués.
 * Affiche le résultat de [fr.russki.app.domain.exercise.ExerciseGrader] — ne duplique pas les règles.
 */
enum class AnswerFeedbackState {
    Idle,
    Correct,
    Incorrect,
}

data class AnswerChoiceVisual(
    val container: Color,
    val content: Color,
    val border: Color?,
    val emphasized: Boolean,
)

fun choiceVisual(
    state: AnswerFeedbackState,
    selected: Boolean,
    isCorrectOption: Boolean,
    idleContainer: Color,
    idleContent: Color,
    selectedIdleContainer: Color,
    selectedIdleContent: Color,
    feedback: RusskiFeedbackColors,
): AnswerChoiceVisual = when (state) {
    AnswerFeedbackState.Idle -> if (selected) {
        AnswerChoiceVisual(selectedIdleContainer, selectedIdleContent, null, true)
    } else {
        AnswerChoiceVisual(idleContainer, idleContent, idleContent.copy(alpha = 0.18f), false)
    }
    AnswerFeedbackState.Correct -> when {
        selected || isCorrectOption -> AnswerChoiceVisual(
            feedback.correctContainer,
            feedback.onCorrectContainer,
            feedback.correct,
            true,
        )
        else -> AnswerChoiceVisual(
            idleContainer.copy(alpha = 0.55f),
            idleContent.copy(alpha = 0.55f),
            null,
            false,
        )
    }
    AnswerFeedbackState.Incorrect -> when {
        isCorrectOption -> AnswerChoiceVisual(
            feedback.correctContainer,
            feedback.onCorrectContainer,
            feedback.correct,
            true,
        )
        selected -> AnswerChoiceVisual(
            feedback.incorrectContainer,
            feedback.onIncorrectContainer,
            feedback.incorrect,
            true,
        )
        else -> AnswerChoiceVisual(
            idleContainer.copy(alpha = 0.55f),
            idleContent.copy(alpha = 0.55f),
            null,
            false,
        )
    }
}

@Composable
fun AnswerChoiceButton(
    label: String,
    selected: Boolean,
    isCorrectOption: Boolean,
    feedbackState: AnswerFeedbackState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val feedback = RusskiThemeExtras.feedback
    val visual = choiceVisual(
        state = feedbackState,
        selected = selected,
        isCorrectOption = isCorrectOption,
        idleContainer = scheme.surface,
        idleContent = scheme.onSurface,
        selectedIdleContainer = scheme.primaryContainer,
        selectedIdleContent = scheme.onPrimaryContainer,
        feedback = feedback,
    )
    val shape = RoundedCornerShape(RusskiRadii.Md.dp)
    val a11y = buildString {
        append(label)
        if (!supporting.isNullOrBlank()) append(", ").append(supporting)
        when {
            feedbackState == AnswerFeedbackState.Correct && (selected || isCorrectOption) ->
                append(", bonne réponse")
            feedbackState == AnswerFeedbackState.Incorrect && selected && !isCorrectOption ->
                append(", mauvaise réponse")
            feedbackState == AnswerFeedbackState.Incorrect && isCorrectOption ->
                append(", réponse correcte")
        }
    }
    Surface(
        onClick = onClick,
        enabled = enabled && feedbackState == AnswerFeedbackState.Idle,
        shape = shape,
        color = visual.container,
        contentColor = visual.content,
        border = visual.border?.let { BorderStroke(if (visual.emphasized) 2.dp else 1.dp, it) },
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = a11y },
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (visual.emphasized) FontWeight.SemiBold else FontWeight.Medium,
                )
                if (!supporting.isNullOrBlank()) {
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.bodySmall,
                        color = visual.content.copy(alpha = 0.8f),
                    )
                }
            }
            when {
                feedbackState != AnswerFeedbackState.Idle && (selected || isCorrectOption) &&
                    (feedbackState == AnswerFeedbackState.Correct || isCorrectOption) -> {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = feedback.correct,
                        modifier = Modifier.size(22.dp),
                    )
                }
                feedbackState == AnswerFeedbackState.Incorrect && selected && !isCorrectOption -> {
                    Icon(
                        imageVector = Icons.Rounded.Cancel,
                        contentDescription = null,
                        tint = feedback.incorrect,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun AnswerFeedbackBanner(
    state: AnswerFeedbackState,
    correctAnswerLabel: String? = null,
    explanation: String? = null,
    continueLabel: String = "Continuer",
    onContinue: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    customMessage: String? = null,
) {
    val feedback = RusskiThemeExtras.feedback
    val visible = state != AnswerFeedbackState.Idle
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it / 4 },
        exit = fadeOut(),
        modifier = modifier,
    ) {
        val isCorrect = state == AnswerFeedbackState.Correct
        val container = if (isCorrect) feedback.correctContainer else feedback.incorrectContainer
        val onContainer = if (isCorrect) feedback.onCorrectContainer else feedback.onIncorrectContainer
        val icon = if (isCorrect) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel
        val title = customMessage ?: if (isCorrect) "Correct !" else "Incorrect"
        val announce = buildString {
            append(title)
            if (!isCorrect && !correctAnswerLabel.isNullOrBlank()) {
                append(". Bonne réponse : ").append(correctAnswerLabel)
            }
            if (!explanation.isNullOrBlank()) append(". ").append(explanation)
        }
        Surface(
            shape = RoundedCornerShape(RusskiRadii.Md.dp),
            color = container,
            contentColor = onContainer,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = announce
                    liveRegion = LiveRegionMode.Assertive
                },
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isCorrect) feedback.correct else feedback.incorrect,
                        modifier = Modifier.size(26.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (!isCorrect && !correctAnswerLabel.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Bonne réponse : $correctAnswerLabel",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = feedback.correct,
                    )
                }
                if (!explanation.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = explanation,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (onContinue != null) {
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton(
                        text = continueLabel,
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

fun feedbackStateFrom(isCorrect: Boolean): AnswerFeedbackState =
    if (isCorrect) AnswerFeedbackState.Correct else AnswerFeedbackState.Incorrect
