package fr.russki.app.data.placement

import fr.russki.app.domain.exercise.InteractiveExerciseType
import fr.russki.app.domain.placement.PlacementIds
import fr.russki.app.domain.placement.PlacementSkill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlacementQuestionBankTest {

    @Test
    fun buildDefault_hasExpectedCountAndUniqueIds() {
        val questions = PlacementQuestionBank.buildDefault()
        assertEquals(PlacementQuestionBank.EXPECTED_COUNT, questions.size)
        assertEquals(questions.size, questions.map { it.id }.toSet().size)
        assertTrue(questions.all { it.id.startsWith(PlacementIds.QUESTION_PREFIX) })
        assertTrue(questions.none { it.id.startsWith("a1-lesson-") })
        assertTrue(questions.none { it.id.startsWith("a1-ex-") })
        assertTrue(questions.none { it.exercise.type == InteractiveExerciseType.WordOrder })
        assertTrue(questions.all { it.exercise.choices.isNotEmpty() })
    }

    @Test
    fun coversAllCurriculumSkills() {
        val skills = PlacementQuestionBank.buildDefault().map { it.skill }.toSet()
        assertEquals(PlacementSkill.entries.toSet(), skills)
    }

    @Test
    fun filterForAvailableLessons_dropsUnavailableSkills() {
        val all = PlacementQuestionBank.buildDefault()
        val filtered = PlacementQuestionBank.filterForAvailableLessons(
            all,
            setOf("a1-lesson-alphabet", "a1-lesson-greetings"),
        )
        assertTrue(filtered.isNotEmpty())
        assertTrue(
            filtered.all {
                it.skill.recommendedLessonId in setOf(
                    "a1-lesson-alphabet",
                    "a1-lesson-greetings",
                )
            },
        )
        assertFalse(filtered.any { it.skill == PlacementSkill.FoodOrders })
    }

    @Test
    fun filter_emptyCurriculum_yieldsEmpty() {
        assertTrue(
            PlacementQuestionBank.filterForAvailableLessons(
                PlacementQuestionBank.buildDefault(),
                emptySet(),
            ).isEmpty(),
        )
    }
}
