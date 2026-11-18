package fr.russki.app.domain.placement

import fr.russki.app.data.placement.PlacementQuestionBank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlacementEngineTest {

    private val engine = PlacementEngine()
    private val questions = PlacementQuestionBank.buildDefault()

    @Test
    fun questionOrder_matchesSortOrder() {
        val session = engine.start(questions)
        assertEquals(PlacementQuestionBank.EXPECTED_COUNT, session.questions.size)
        assertEquals(
            session.questions.map { it.sortOrder },
            session.questions.map { it.sortOrder }.sorted(),
        )
        assertTrue(session.questions.all { it.id.startsWith(PlacementIds.QUESTION_PREFIX) })
    }

    @Test
    fun grading_usesAcceptedAnswers_andDoesNotDoubleCount() {
        var session = engine.start(questions)
        session = engine.beginQuestions(session)
        val q = session.currentQuestion!!
        val correct = q.exercise.acceptedAnswers.first()
        session = engine.submitAnswer(session, correct)
        assertTrue(session.currentGrade!!.isCorrect)
        assertEquals(1, session.answeredCount)
        // Resubmit ignored
        session = engine.submitAnswer(session, "wrong")
        assertTrue(session.currentGrade!!.isCorrect)
        assertEquals(1, session.answeredCount)
    }

    @Test
    fun missingAnswer_isIncorrect() {
        var session = engine.start(listOf(questions.first()))
        session = engine.beginQuestions(session)
        session = engine.submitAnswer(session, "")
        assertFalse(session.currentGrade!!.isCorrect)
    }

    @Test
    fun advance_reachesResultPhase() {
        var session = engine.start(questions.take(2))
        session = engine.beginQuestions(session)
        repeat(2) {
            val q = session.currentQuestion!!
            session = engine.submitAnswer(session, q.exercise.acceptedAnswers.first())
            session = engine.advanceAfterFeedback(session)
        }
        assertEquals(PlacementPhase.Result, session.phase)
        assertNull(session.currentGrade)
        assertEquals(2, session.correctCount)
    }

    @Test
    fun attachResult_keepsScore() {
        var session = engine.start(questions.take(1))
        session = engine.beginQuestions(session)
        session = engine.submitAnswer(session, questions.first().exercise.acceptedAnswers.first())
        session = engine.advanceAfterFeedback(session)
        val result = PlacementRecommender.buildResult(
            questions = session.questions,
            grades = session.grades,
            availableLessonIds = setOf("a1-lesson-alphabet", "a1-lesson-greetings"),
            lessonTitles = mapOf("a1-lesson-alphabet" to "Alphabet cyrillique"),
            completedAtEpochMs = 1L,
        )
        session = engine.attachResult(session, result)
        assertNotNull(session.result)
        assertEquals(1, session.result!!.correctCount)
    }
}
