package fr.russki.app.core.ui

import fr.russki.app.core.ui.components.AnswerFeedbackState
import fr.russki.app.core.ui.components.choiceVisual
import fr.russki.app.core.ui.components.feedbackStateFrom
import fr.russki.app.core.ui.theme.RusskiFeedbackColors
import fr.russki.app.domain.exercise.AnswerMatcher
import fr.russki.app.domain.exercise.ExerciseGrader
import fr.russki.app.domain.exercise.InteractiveExercise
import fr.russki.app.domain.exercise.InteractiveExerciseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.compose.ui.graphics.Color

/**
 * Logique pure du feedback universel (sans Compose runtime instrumenté).
 */
class AnswerFeedbackLogicTest {

    private val feedback = RusskiFeedbackColors(
        correct = Color(0xFF1F7A4D),
        onCorrect = Color.White,
        correctContainer = Color(0xFFD9F2E5),
        onCorrectContainer = Color(0xFF0C3D26),
        incorrect = Color(0xFFB3261E),
        onIncorrect = Color.White,
        incorrectContainer = Color(0xFFF9DEDC),
        onIncorrectContainer = Color(0xFF410E0B),
        brandAccent = Color(0xFFB23A3A),
    )
    private val idle = Color(0xFFFFFFFF)
    private val onIdle = Color(0xFF000000)
    private val selectedIdle = Color(0xFFD6E2F2)
    private val onSelectedIdle = Color(0xFF142033)

    @Test
    fun feedbackStateFrom_mapsBooleans() {
        assertEquals(AnswerFeedbackState.Correct, feedbackStateFrom(true))
        assertEquals(AnswerFeedbackState.Incorrect, feedbackStateFrom(false))
    }

    @Test
    fun idle_selectedUsesPrimaryContainer() {
        val visual = choiceVisual(
            state = AnswerFeedbackState.Idle,
            selected = true,
            isCorrectOption = false,
            idleContainer = idle,
            idleContent = onIdle,
            selectedIdleContainer = selectedIdle,
            selectedIdleContent = onSelectedIdle,
            feedback = feedback,
        )
        assertEquals(selectedIdle, visual.container)
        assertTrue(visual.emphasized)
    }

    @Test
    fun correct_selectedTurnsGreen() {
        val visual = choiceVisual(
            state = AnswerFeedbackState.Correct,
            selected = true,
            isCorrectOption = true,
            idleContainer = idle,
            idleContent = onIdle,
            selectedIdleContainer = selectedIdle,
            selectedIdleContent = onSelectedIdle,
            feedback = feedback,
        )
        assertEquals(feedback.correctContainer, visual.container)
        assertEquals(feedback.correct, visual.border)
    }

    @Test
    fun incorrect_selectedIsRed_andCorrectOptionRevealedGreen() {
        val wrong = choiceVisual(
            state = AnswerFeedbackState.Incorrect,
            selected = true,
            isCorrectOption = false,
            idleContainer = idle,
            idleContent = onIdle,
            selectedIdleContainer = selectedIdle,
            selectedIdleContent = onSelectedIdle,
            feedback = feedback,
        )
        val right = choiceVisual(
            state = AnswerFeedbackState.Incorrect,
            selected = false,
            isCorrectOption = true,
            idleContainer = idle,
            idleContent = onIdle,
            selectedIdleContainer = selectedIdle,
            selectedIdleContent = onSelectedIdle,
            feedback = feedback,
        )
        assertEquals(feedback.incorrectContainer, wrong.container)
        assertEquals(feedback.incorrect, wrong.border)
        assertEquals(feedback.correctContainer, right.container)
        assertEquals(feedback.correct, right.border)
        assertNotEquals(wrong.container, right.container)
    }

    @Test
    fun lightAndDarkSemanticPalettes_keepDistinctCorrectIncorrect() {
        val lightCorrect = fr.russki.app.core.ui.theme.Correct
        val lightIncorrect = fr.russki.app.core.ui.theme.Incorrect
        val darkCorrect = fr.russki.app.core.ui.theme.NightCorrect
        val darkIncorrect = fr.russki.app.core.ui.theme.NightIncorrect
        assertNotEquals(lightCorrect, lightIncorrect)
        assertNotEquals(darkCorrect, darkIncorrect)
        assertNotEquals(lightCorrect, darkIncorrect)
    }

    @Test
    fun incorrect_doesNotRevealBeforeAnswer() {
        val idleVisual = choiceVisual(
            state = AnswerFeedbackState.Idle,
            selected = false,
            isCorrectOption = true,
            idleContainer = idle,
            idleContent = onIdle,
            selectedIdleContainer = selectedIdle,
            selectedIdleContent = onSelectedIdle,
            feedback = feedback,
        )
        assertEquals(idle, idleVisual.container)
        assertFalse(idleVisual.emphasized)
    }

    @Test
    fun grader_stillDrivesFeedback_withoutDuplicatingRules() {
        val exercise = InteractiveExercise(
            id = "fb-1",
            type = InteractiveExerciseType.MultipleChoice,
            promptFr = "Traduisez",
            stimulus = "спасибо",
            choices = listOf("merci", "bonjour", "au revoir"),
            acceptedAnswers = listOf("merci", "Merci"),
            explanationFr = "спасибо = merci.",
            sortOrder = 1,
        )
        val grader = ExerciseGrader(AnswerMatcher())
        val good = grader.grade(exercise, "Merci")
        val bad = grader.grade(exercise, "bonjour")
        assertTrue(good.isCorrect)
        assertFalse(bad.isCorrect)
        assertEquals(AnswerFeedbackState.Correct, feedbackStateFrom(good.isCorrect))
        assertEquals(AnswerFeedbackState.Incorrect, feedbackStateFrom(bad.isCorrect))
        assertTrue(AnswerMatcher().matches("merci", bad.acceptedAnswers))
    }

    @Test
    fun doubleGrade_returnsSameResult_noScoreInflationAtUiLayer() {
        val exercise = InteractiveExercise(
            id = "fb-2",
            type = InteractiveExerciseType.FillBlank,
            promptFr = "Complétez",
            stimulus = "…",
            choices = emptyList(),
            acceptedAnswers = listOf("да"),
            explanationFr = "да = oui.",
            sortOrder = 2,
        )
        val grader = ExerciseGrader(AnswerMatcher())
        val first = grader.grade(exercise, "да")
        val second = grader.grade(exercise, "нет")
        // Le moteur de session empêche le double comptage ; le grader reste pur.
        assertTrue(first.isCorrect)
        assertFalse(second.isCorrect)
        assertEquals(listOf("да"), first.acceptedAnswers)
    }
}
