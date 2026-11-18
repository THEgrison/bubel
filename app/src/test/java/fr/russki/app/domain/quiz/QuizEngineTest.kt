package fr.russki.app.domain.quiz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizEngineTest {

    private val engine = QuizEngine(clock = { 1_000L })

    private fun sampleQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = "q1",
            type = QuizQuestionType.MultipleChoice,
            contentRefId = "c1",
            promptFr = "Traduction ?",
            stimulus = "привет",
            choices = listOf("salut", "merci", "eau"),
            expectedAnswer = "salut",
            explanationFr = "привет = salut",
            difficulty = QuizDifficulty.Easy,
            audioAssetPath = null,
        ),
        QuizQuestion(
            id = "q2",
            type = QuizQuestionType.Cloze,
            contentRefId = "c2",
            promptFr = "Trous",
            stimulus = "____!",
            choices = emptyList(),
            expectedAnswer = "пока",
            explanationFr = "пока",
            difficulty = QuizDifficulty.Medium,
            audioAssetPath = null,
        ),
    )

    @Test
    fun answer_updatesScoreOnce() {
        var session = engine.startSession(sampleQuestions(), sessionId = "s1")
        val first = engine.answer(session, "salut")
        assertFalse(first.alreadyAnswered)
        assertTrue(first.record.isCorrect)
        assertEquals(1, first.session.score)

        val second = engine.answer(first.session, "merci")
        assertTrue(second.alreadyAnswered)
        assertEquals(1, second.session.score)
        assertEquals("salut", second.record.userAnswer)
    }

    @Test
    fun advance_completesSession() {
        var session = engine.startSession(sampleQuestions(), sessionId = "s2")
        session = engine.answer(session, "salut").session
        session = engine.advance(session)
        assertEquals(1, session.currentIndex)
        session = engine.answer(session, "пока").session
        session = engine.advance(session)
        assertEquals(QuizSessionStatus.Completed, session.status)
        assertEquals(2, session.score)
    }

    @Test
    fun resume_skipsAnsweredQuestions() {
        var session = engine.startSession(sampleQuestions(), sessionId = "s3")
        session = engine.answer(session, "salut").session
        session = engine.abandon(session)
        val resumed = engine.resume(session)
        assertEquals(QuizSessionStatus.InProgress, resumed.status)
        assertEquals(1, resumed.currentIndex)
        assertEquals("q2", resumed.currentQuestion?.id)
    }

    @Test
    fun choiceIndex_isAccepted() {
        val session = engine.startSession(sampleQuestions().take(1), sessionId = "s4")
        val grade = engine.answer(session, "0") // index of "salut" depends on order - fix by known choices
        // choices order fixed in sample: salut is index 0
        assertTrue(grade.record.isCorrect)
    }
}
