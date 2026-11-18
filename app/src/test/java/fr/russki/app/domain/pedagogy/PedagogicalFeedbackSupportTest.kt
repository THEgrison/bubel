package fr.russki.app.domain.pedagogy

import fr.russki.app.domain.exercise.ExerciseGrade
import fr.russki.app.domain.exercise.InteractiveExercise
import fr.russki.app.domain.exercise.InteractiveExerciseType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PedagogicalFeedbackSupportTest {

    @Test
    fun fromRussian_providesTransliterationNotAsIpaClaim() {
        val snap = PedagogicalFeedbackSupport.fromRussian(
            textRu = "привет",
            translationFr = "salut",
        )
        assertNotNull(snap.transliteration)
        assertTrue(snap.transliteration!!.isNotBlank())
        assertFalse(snap.stressUnknown)
        assertTrue(snap.translationFr == "salut")
    }

    @Test
    fun fromGrade_marksRevealed() {
        val grade = ExerciseGrade(
            exerciseId = "x",
            userAnswer = "ok",
            isCorrect = true,
            explanationFr = "e",
            acceptedAnswers = listOf("ok"),
            revealedBeforeAnswer = true,
            stimulusRu = "спасибо",
            translationFr = "merci",
        )
        val snap = PedagogicalFeedbackSupport.fromGrade(grade)
        assertNotNull(snap.revealedNote)
        assertTrue(snap.revealedNote!!.contains("maîtrise"))
        assertNotNull(snap.transliteration)
    }

    @Test
    fun enrichExercise_fillsMissingFields() {
        val ex = InteractiveExercise(
            id = "e1",
            type = InteractiveExerciseType.MatchTranslation,
            promptFr = "Sens ?",
            stimulus = "метро",
            choices = listOf("métro", "bus"),
            acceptedAnswers = listOf("métro"),
            explanationFr = "metro",
            sortOrder = 1,
        )
        val enriched = PedagogicalFeedbackSupport.enrichExercise(ex)
        assertNotNull(enriched.transliteration)
        assertTrue(enriched.translationFr == "métro")
    }
}
