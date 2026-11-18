package fr.russki.app.domain.quiz

import java.util.UUID

/**
 * Moteur de session de quiz, pur et testable.
 * Horloge injectable pour les timestamps.
 */
class QuizEngine(
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
) {

    fun startSession(
        questions: List<QuizQuestion>,
        sessionId: String = idGenerator(),
    ): QuizSessionState {
        require(questions.isNotEmpty()) { "Une session nécessite au moins une question." }
        require(questions.map { it.id }.toSet().size == questions.size) {
            "Les identifiants de questions doivent être uniques."
        }
        return QuizSessionState(
            sessionId = sessionId,
            questions = questions,
            answers = emptyMap(),
            currentIndex = 0,
            status = QuizSessionStatus.InProgress,
            startedAtEpochMs = clock(),
        )
    }

    /**
     * Enregistre une réponse pour la question courante.
     * Idempotent : une seconde tentative (ex. rotation) ne modifie ni score ni map.
     */
    fun answer(session: QuizSessionState, userAnswer: String): QuizGradeResult {
        check(session.status == QuizSessionStatus.InProgress) {
            "Session non active (${session.status})."
        }
        val question = session.currentQuestion
            ?: error("Aucune question courante.")
        session.answers[question.id]?.let { existing ->
            return QuizGradeResult(
                session = session,
                record = existing,
                alreadyAnswered = true,
            )
        }

        val record = QuizAnswerRecord(
            questionId = question.id,
            userAnswer = userAnswer,
            isCorrect = evaluateAnswer(question, userAnswer),
            answeredAtEpochMs = clock(),
        )
        val updated = session.copy(
            answers = session.answers + (question.id to record),
        )
        return QuizGradeResult(session = updated, record = record, alreadyAnswered = false)
    }

    fun advance(session: QuizSessionState): QuizSessionState {
        check(session.isCurrentAnswered) {
            "Impossible d'avancer sans avoir répondu à la question courante."
        }
        val next = session.currentIndex + 1
        return if (next >= session.questions.size) {
            session.copy(
                currentIndex = next,
                status = QuizSessionStatus.Completed,
            )
        } else {
            session.copy(currentIndex = next)
        }
    }

    fun abandon(session: QuizSessionState): QuizSessionState =
        session.copy(status = QuizSessionStatus.Abandoned)

    fun resume(session: QuizSessionState): QuizSessionState {
        require(
            session.status == QuizSessionStatus.InProgress ||
                session.status == QuizSessionStatus.Abandoned,
        ) {
            "Impossible de reprendre une session ${session.status}."
        }
        val index = session.questions.indexOfFirst { !session.answers.containsKey(it.id) }
            .takeIf { it >= 0 }
            ?: session.questions.size
        val status = if (index >= session.questions.size) {
            QuizSessionStatus.Completed
        } else {
            QuizSessionStatus.InProgress
        }
        return session.copy(currentIndex = index, status = status)
    }

    private fun evaluateAnswer(question: QuizQuestion, userAnswer: String): Boolean {
        val expected = normalize(question.expectedAnswer)
        val user = normalize(userAnswer)
        if (user == expected) return true
        if (question.choices.any { normalize(it) == user && normalize(it) == expected }) {
            return true
        }
        userAnswer.trim().toIntOrNull()?.let { index ->
            if (index in question.choices.indices) {
                return normalize(question.choices[index]) == expected
            }
        }
        return false
    }

    private fun normalize(value: String): String =
        value.trim().lowercase().replace(Regex("\\s+"), " ")
}
