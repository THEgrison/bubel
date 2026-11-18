package fr.russki.app.domain.listening

import fr.russki.app.data.listening.ListeningExerciseBank
import fr.russki.app.domain.exercise.ExerciseGrader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningExerciseBankTest {

    private val grader = ExerciseGrader()

    @Test
    fun bank_hasItems_withChoicesAndAcceptedAnswers() {
        val items = ListeningExerciseBank.items()
        assertTrue(items.size >= 6)
        items.forEach { item ->
            assertTrue(item.choices.isNotEmpty())
            assertTrue(item.acceptedAnswers.isNotEmpty())
            assertTrue(item.textRuFallback.isNotBlank())
        }
    }

    @Test
    fun grading_worksWithoutAudio_viaTextFallback() {
        val item = ListeningExerciseBank.items().first()
        val ok = grader.grade(item.asExercise(), item.acceptedAnswers.first())
        assertTrue(ok.isCorrect)
        val ko = grader.grade(item.asExercise(), "réponse inventée")
        assertFalse(ko.isCorrect)
        assertEquals(item.id, ok.exerciseId)
    }
}
