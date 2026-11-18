package fr.russki.app.domain.placement

import fr.russki.app.data.placement.PlacementQuestionBank
import fr.russki.app.domain.exercise.ExerciseGrade
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlacementRecommenderTest {

    private val all = PlacementQuestionBank.buildDefault()
    private val available = all.map { it.skill.recommendedLessonId }.toSet() +
        setOf(
            "a1-lesson-alphabet",
            "a1-lesson-greetings",
            "a1-lesson-introduce",
            "a1-lesson-pronouns",
            "a1-lesson-numbers",
            "a1-lesson-countries",
            "a1-lesson-family",
            "a1-lesson-food",
            "a1-lesson-city",
            "a1-lesson-review-vocab",
        )

    @Test
    fun exactScore_countsOnlyCorrectGrades() {
        val subset = all.take(4)
        val grades = mapOf(
            subset[0].id to grade(subset[0].id, true),
            subset[1].id to grade(subset[1].id, false),
            subset[2].id to grade(subset[2].id, true),
        )
        val result = PlacementRecommender.buildResult(
            questions = subset,
            grades = grades,
            availableLessonIds = available,
            lessonTitles = emptyMap(),
            completedAtEpochMs = 10L,
        )
        assertEquals(2, result.correctCount)
        assertEquals(3, result.answeredCount)
        assertEquals(4, result.totalQuestions)
        assertEquals(50, result.percent)
    }

    @Test
    fun missingAnswers_triggerFallbackStart() {
        val grades = mapOf(all.first().id to grade(all.first().id, true))
        val rec = PlacementRecommender.recommend(all, grades, available)
        assertTrue(rec.isFallbackStart)
        assertEquals("a1-lesson-alphabet", rec.lessonId)
    }

    @Test
    fun weakEarlySkill_recommendsThatLesson() {
        val grades = all.associate { q ->
            val ok = q.skill != PlacementSkill.Greetings
            q.id to grade(q.id, ok)
        }
        val rec = PlacementRecommender.recommend(all, grades, available)
        assertEquals(PlacementSkill.Greetings.recommendedLessonId, rec.lessonId)
        assertTrue(rec.rationaleFr.contains("Salutations"))
    }

    @Test
    fun allSkillsStrong_recommendsReview() {
        val grades = all.associate { it.id to grade(it.id, true) }
        val rec = PlacementRecommender.recommend(all, grades, available)
        assertEquals("a1-lesson-review-vocab", rec.lessonId)
        assertTrue(!rec.isFallbackStart)
    }

    @Test
    fun incompleteCurriculum_fallsBackToAvailableLesson() {
        val tiny = setOf("a1-lesson-numbers")
        val grades = all.associate { it.id to grade(it.id, false) }
        val rec = PlacementRecommender.recommend(all, grades, tiny)
        assertEquals("a1-lesson-numbers", rec.lessonId)
    }

    @Test
    fun emptyCurriculum_returnsEmptyOrSafeFallback() {
        val rec = PlacementRecommender.recommend(all, emptyMap(), emptySet())
        assertTrue(rec.isFallbackStart)
        assertEquals("", rec.lessonId)
    }

    @Test
    fun recommendation_targetsRealLessonIdsOnly() {
        val grades = all.associate { it.id to grade(it.id, it.skill.order > 4) }
        val rec = PlacementRecommender.recommend(all, grades, available)
        assertTrue(rec.lessonId in available)
    }

    private fun grade(id: String, correct: Boolean) = ExerciseGrade(
        exerciseId = id,
        userAnswer = if (correct) "ok" else "ko",
        isCorrect = correct,
        explanationFr = "",
        acceptedAnswers = listOf("ok"),
    )
}
