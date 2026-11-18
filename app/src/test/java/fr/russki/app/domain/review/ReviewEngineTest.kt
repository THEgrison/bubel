package fr.russki.app.domain.review

import fr.russki.app.data.review.ReviewableCatalog
import fr.russki.app.data.content.seed.A1Unit1Seed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewEngineTest {

    private val engine = ReviewEngine()
    private val items = ReviewableCatalog.itemsForCompleted(
        setOf(A1Unit1Seed.LESSON_GREETINGS),
    ).take(3)

    @Test
    fun gradesWithAnswerMatcher_andAdvancesToSummary() {
        var session = engine.start(items)
        assertEquals(ReviewPhase.Prompt, session.phase)
        items.forEach { item ->
            session = engine.submit(session, item.acceptedAnswers.first())
            assertTrue(session.currentGrade!!.isCorrect)
            assertEquals(ReviewPhase.Feedback, session.phase)
            session = engine.advance(session)
        }
        assertEquals(ReviewPhase.Summary, session.phase)
        assertEquals(items.size, session.correctCount)
    }

    @Test
    fun wrongAnswer_isIncorrect_butStillAdvances() {
        var session = engine.start(listOf(items.first()))
        session = engine.submit(session, "réponse inventée")
        assertFalse(session.currentGrade!!.isCorrect)
        session = engine.advance(session)
        assertTrue(session.finished)
    }

    @Test
    fun submit_twiceOnSameItem_keepsFirstGrade_noDoubleCount() {
        val item = items.first()
        var session = engine.start(listOf(item))
        session = engine.submit(session, "mauvaise")
        assertFalse(session.currentGrade!!.isCorrect)
        assertEquals(0, session.correctCount)
        assertEquals(1, session.grades.size)

        // Deuxième submit en Feedback : l'engine refuse (phase != Prompt).
        try {
            engine.submit(session, item.acceptedAnswers.first())
            assertTrue("doit lever si phase Feedback", false)
        } catch (_: IllegalStateException) {
            // attendu
        }
        assertEquals(0, session.correctCount)
        assertEquals(1, session.grades.size)
    }
}
