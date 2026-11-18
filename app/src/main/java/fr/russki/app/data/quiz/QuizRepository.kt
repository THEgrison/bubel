package fr.russki.app.data.quiz

import fr.russki.app.data.content.OfflineContentRepository
import fr.russki.app.data.local.QuizAnswerEntity
import fr.russki.app.data.local.QuizSessionDao
import fr.russki.app.data.local.QuizSessionEntity
import fr.russki.app.domain.quiz.QuestionGenerator
import fr.russki.app.domain.quiz.QuizAnswerRecord
import fr.russki.app.domain.quiz.QuizEngine
import fr.russki.app.domain.quiz.QuizQuestion
import fr.russki.app.domain.quiz.QuizSessionState
import fr.russki.app.domain.quiz.QuizSessionStatus

class QuizRepository(
    private val dao: QuizSessionDao,
    private val contentRepository: OfflineContentRepository = OfflineContentRepository.get(),
    private val generator: QuestionGenerator = QuestionGenerator(),
    private val engine: QuizEngine = QuizEngine(),
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    fun generateQuestions(count: Int): List<QuizQuestion> =
        generator.generate(contentRepository.contentPack(), count)

    suspend fun startSession(questionCount: Int): QuizSessionState {
        val questions = generateQuestions(questionCount)
        val state = engine.startSession(questions)
        persistSession(state)
        return state
    }

    suspend fun loadSession(sessionId: String): QuizSessionState? {
        val entity = dao.getSession(sessionId) ?: return null
        val answers = dao.getAnswers(sessionId).associate { it.questionId to it.toRecord() }
        val questions = QuizQuestionJson.decodeList(entity.questionsJson)
        return QuizSessionState(
            sessionId = entity.sessionId,
            questions = questions,
            answers = answers,
            currentIndex = entity.currentIndex,
            status = QuizSessionStatus.valueOf(entity.status),
            startedAtEpochMs = entity.startedAtEpochMs,
        )
    }

    suspend fun findInterruptedSession(): QuizSessionState? {
        val entity = dao.getInterruptedSession() ?: return null
        return loadSession(entity.sessionId)?.let { engine.resume(it) }
    }

    /**
     * Enregistre la réponse. Retourne l'état ; [QuizGradeResult.alreadyAnswered]
     * reste true si Room ou le moteur détecte un doublon (rotation).
     */
    suspend fun submitAnswer(
        session: QuizSessionState,
        userAnswer: String,
    ): Pair<fr.russki.app.domain.quiz.QuizGradeResult, QuizSessionState> {
        val graded = engine.answer(session, userAnswer)
        if (!graded.alreadyAnswered) {
            val inserted = dao.saveAnswerOnce(
                session = graded.session.toEntity(),
                answer = graded.record.toEntity(graded.session.sessionId),
            )
            if (!inserted) {
                // Course Room : déjà présent — recharger
                val reloaded = loadSession(session.sessionId) ?: graded.session
                return graded.copy(alreadyAnswered = true, session = reloaded) to reloaded
            }
        }
        return graded to graded.session
    }

    suspend fun advance(session: QuizSessionState): QuizSessionState {
        val next = engine.advance(session)
        persistSession(next)
        return next
    }

    suspend fun abandon(session: QuizSessionState): QuizSessionState {
        val abandoned = engine.abandon(session)
        persistSession(abandoned)
        return abandoned
    }

    private suspend fun persistSession(state: QuizSessionState) {
        dao.upsertSession(state.toEntity())
    }

    private fun QuizSessionState.toEntity(): QuizSessionEntity =
        QuizSessionEntity(
            sessionId = sessionId,
            status = status.name,
            startedAtEpochMs = startedAtEpochMs,
            updatedAtEpochMs = clock(),
            questionCount = questions.size,
            score = score,
            currentIndex = currentIndex,
            questionsJson = QuizQuestionJson.encodeList(questions),
        )

    private fun QuizAnswerRecord.toEntity(sessionId: String): QuizAnswerEntity =
        QuizAnswerEntity(
            sessionId = sessionId,
            questionId = questionId,
            userAnswer = userAnswer,
            isCorrect = isCorrect,
            answeredAtEpochMs = answeredAtEpochMs,
        )

    private fun QuizAnswerEntity.toRecord(): QuizAnswerRecord =
        QuizAnswerRecord(
            questionId = questionId,
            userAnswer = userAnswer,
            isCorrect = isCorrect,
            answeredAtEpochMs = answeredAtEpochMs,
        )
}
