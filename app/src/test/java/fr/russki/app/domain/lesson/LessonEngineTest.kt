package fr.russki.app.domain.lesson

import fr.russki.app.data.content.seed.A1Unit1Seed
import fr.russki.app.domain.exercise.InteractiveExerciseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonEngineTest {

    private val engine = LessonEngine()

    @Test
    fun a1Unit_hasThreeLessonsWithExercises() {
        val unit = A1Unit1Seed.unit
        assertEquals(3, unit.lessons.size)
        unit.lessons.forEach { lesson ->
            val ex = A1Unit1Seed.exercisesFor(lesson.id)
            assertTrue(ex.size >= 3)
            assertTrue(ex.map { it.type }.toSet().size >= 2)
        }
    }

    @Test
    fun session_introToSummary_withCorrection() {
        val lesson = A1Unit1Seed.unit.lessons.first { it.id == A1Unit1Seed.LESSON_GREETINGS }
        val exercises = A1Unit1Seed.exercisesFor(lesson.id)
        var session = engine.start(lesson, exercises)
        assertEquals(LessonPhase.Introduction, session.phase)

        session = engine.beginExercises(session)
        assertEquals(LessonPhase.Exercise, session.phase)

        // Mauvaise réponse puis bonne sur l'idempotence après grade
        session = engine.submitAnswer(session, "mauvaise")
        assertFalse(session.currentGrade!!.isCorrect)
        assertTrue(session.currentGrade!!.explanationFr.isNotBlank())

        session = engine.advanceAfterFeedback(session)
        session = engine.submitAnswer(session, exercises[1].acceptedAnswers.first())
        assertTrue(session.currentGrade!!.isCorrect)
        session = engine.advanceAfterFeedback(session)

        session = engine.submitAnswer(session, exercises[2].acceptedAnswers.first())
        session = engine.advanceAfterFeedback(session)
        assertEquals(LessonPhase.Summary, session.phase)

        session = engine.complete(session)
        assertEquals(LessonPhase.Completed, session.phase)
        assertEquals(2, session.correctCount)
    }

    @Test
    fun submitAnswer_twice_keepsFirstGrade_noDoubleCount() {
        val lesson = A1Unit1Seed.unit.lessons.first { it.id == A1Unit1Seed.LESSON_GREETINGS }
        val exercises = A1Unit1Seed.exercisesFor(lesson.id)
        var session = engine.beginExercises(engine.start(lesson, exercises))
        session = engine.submitAnswer(session, "mauvaise")
        assertFalse(session.currentGrade!!.isCorrect)
        assertEquals(1, session.grades.size)
        assertEquals(0, session.correctCount)

        session = engine.submitAnswer(session, exercises.first().acceptedAnswers.first())
        assertFalse(session.currentGrade!!.isCorrect)
        assertEquals(1, session.grades.size)
        assertEquals(0, session.correctCount)
        assertEquals("mauvaise", session.currentGrade!!.userAnswer)
    }

    @Test
    fun seed_coversAtLeastThreeExerciseTypes() {
        val types = A1Unit1Seed.unit.lessons
            .flatMap { A1Unit1Seed.exercisesFor(it.id) }
            .map { it.type }
            .toSet()
        assertTrue(types.contains(InteractiveExerciseType.MultipleChoice))
        assertTrue(types.contains(InteractiveExerciseType.MatchTranslation))
        assertTrue(
            types.contains(InteractiveExerciseType.FillBlank) ||
                types.contains(InteractiveExerciseType.WordOrder) ||
                types.contains(InteractiveExerciseType.LetterRecognition),
        )
    }
}
