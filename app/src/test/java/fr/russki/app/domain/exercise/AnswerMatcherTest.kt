package fr.russki.app.domain.exercise

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerMatcherTest {

    private val matcher = AnswerMatcher()

    @Test
    fun acceptsCaseAndSpacingVariants() {
        assertTrue(matcher.matches("  Спасибо  ", listOf("спасибо")))
        assertTrue(matcher.matches("Меня зовут", listOf("меня зовут")))
    }

    @Test
    fun treatsYoAsYe() {
        assertTrue(matcher.matchesExpected("елка", "ёлка"))
        assertTrue(matcher.matches("всё", listOf("все")))
    }

    @Test
    fun rejectsDifferentAnswers() {
        assertFalse(matcher.matches("пока", listOf("привет")))
        assertFalse(matcher.matches("", listOf("спасибо")))
    }

    @Test
    fun grader_usesAcceptedVariants() {
        val exercise = InteractiveExercise(
            id = "t1",
            type = InteractiveExerciseType.FillBlank,
            promptFr = "x",
            stimulus = "",
            choices = listOf("Меня зовут"),
            acceptedAnswers = listOf("Меня зовут", "меня зовут"),
            explanationFr = "ok",
            sortOrder = 1,
        )
        val grade = ExerciseGrader(matcher).grade(exercise, "МЕНЯ ЗОВУТ")
        assertTrue(grade.isCorrect)
    }
}
