package fr.russki.app.domain.lesson

import fr.russki.app.data.content.seed.A1Unit1Seed
import fr.russki.app.data.content.seed.LessonExerciseEnricher
import fr.russki.app.data.content.seed.LessonTeachCatalog
import fr.russki.app.domain.exercise.InteractiveExercise
import fr.russki.app.domain.exercise.InteractiveExerciseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonEngineTest {

    private val engine = LessonEngine()

    private fun greetingsSession(): LessonSessionState {
        val lesson = A1Unit1Seed.unit.lessons.first { it.id == A1Unit1Seed.LESSON_GREETINGS }
        val raw = A1Unit1Seed.exercisesFor(lesson.id)
        val exercises = LessonExerciseEnricher.enrich(lesson.id, raw)
        val teachables = LessonTeachCatalog.forLesson(lesson.id)
        return engine.start(lesson, teachables, exercises)
    }

    @Test
    fun startsWithIntroduction_thenDiscoveryBeforePractice() {
        var session = greetingsSession()
        assertEquals(LessonPhase.Introduction, session.phase)
        assertTrue(session.teachables.isNotEmpty())
        session = engine.beginDiscovery(session)
        assertEquals(LessonPhase.Discovery, session.phase)
        session = engine.beginPractice(session)
        assertEquals(LessonPhase.Practice, session.phase)
    }

    @Test
    fun teachablesCoverGreetingStimuli() {
        val teachables = LessonTeachCatalog.forLesson(A1Unit1Seed.LESSON_GREETINGS)
        val texts = teachables.map { it.textRu.lowercase() }
        assertTrue(texts.any { "привет" in it })
        assertTrue(texts.any { "здравствуйте" in it })
        assertTrue(teachables.all { it.transliteration.isNotBlank() })
        assertTrue(teachables.all { it.translationFr.isNotBlank() })
    }

    @Test
    fun failedQuestion_entersRematch_untilMastered() {
        var session = engine.beginPractice(engine.beginDiscovery(greetingsSession()))
        val first = session.currentExercise!!
        session = engine.submitAnswer(session, "mauvaise")
        assertFalse(session.currentGrade!!.countsAsMastered)
        assertTrue(first.id in session.rematchQueue)
        session = engine.advanceAfterFeedback(session)

        // Réussir le reste sans rattrapage encore
        while (session.phase == LessonPhase.Practice) {
            val ex = session.currentExercise!!
            session = engine.submitAnswer(session, ex.acceptedAnswers.first())
            session = engine.advanceAfterFeedback(session)
        }
        assertEquals(LessonPhase.Rematch, session.phase)
        assertTrue(session.rematchQueue.isNotEmpty())

        // Révélation → ne maîtrise pas
        session = engine.revealHint(session)
        session = engine.submitAnswer(session, session.currentExercise!!.acceptedAnswers.first())
        assertTrue(session.currentGrade!!.revealedBeforeAnswer)
        assertFalse(session.currentGrade!!.countsAsMastered)
        session = engine.advanceAfterFeedback(session)

        // Réponse correcte sans révélation
        while (session.phase == LessonPhase.Rematch) {
            val ex = session.currentExercise ?: break
            session = engine.submitAnswer(session, ex.acceptedAnswers.first())
            assertTrue(session.currentGrade!!.countsAsMastered)
            session = engine.advanceAfterFeedback(session)
        }
        assertEquals(LessonPhase.Recap, session.phase)
        assertTrue(first.id in session.rematchMastered)
    }

    @Test
    fun submitAnswer_twice_keepsFirstGrade_noDoubleCount() {
        var session = engine.beginPractice(engine.beginDiscovery(greetingsSession()))
        session = engine.submitAnswer(session, "mauvaise")
        assertEquals(0, session.correctCount)
        assertEquals(1, session.firstGrades.size)
        session = engine.submitAnswer(session, session.currentExercise!!.acceptedAnswers.first())
        assertEquals(0, session.correctCount)
        assertEquals(1, session.firstGrades.size)
        assertEquals("mauvaise", session.currentGrade!!.userAnswer)
    }

    @Test
    fun summaryReflectsFinalAndRecovery() {
        var session = engine.beginPractice(engine.beginDiscovery(greetingsSession()))
        while (session.phase == LessonPhase.Practice) {
            val ex = session.currentExercise!!
            session = engine.submitAnswer(session, ex.acceptedAnswers.first())
            session = engine.advanceAfterFeedback(session)
        }
        // Pas de rattrapage si tout OK
        assertTrue(session.phase == LessonPhase.Recap || session.phase == LessonPhase.Rematch)
        if (session.phase == LessonPhase.Rematch) {
            while (session.phase == LessonPhase.Rematch) {
                session = engine.submitAnswer(session, session.currentExercise!!.acceptedAnswers.first())
                session = engine.advanceAfterFeedback(session)
            }
        }
        session = engine.beginFinalCheck(session)
        if (session.phase == LessonPhase.FinalCheck) {
            while (session.phase == LessonPhase.FinalCheck) {
                session = engine.submitAnswer(session, session.currentExercise!!.acceptedAnswers.first())
                session = engine.advanceAfterFeedback(session)
            }
        }
        assertEquals(LessonPhase.Summary, session.phase)
        assertEquals(session.practiceExercises.size, session.correctCount)
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

    @Test
    fun a1Unit_hasThreeLessonsWithExercises() {
        val unit = A1Unit1Seed.unit
        assertEquals(3, unit.lessons.size)
        unit.lessons.forEach { lesson ->
            val ex = A1Unit1Seed.exercisesFor(lesson.id)
            assertTrue(ex.size >= 3)
        }
    }
}
