package fr.russki.app.data.content.seed

import fr.russki.app.domain.exercise.ExerciseGrader
import fr.russki.app.domain.exercise.InteractiveExerciseType
import fr.russki.app.domain.curriculum.CefrLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class A1CurriculumSeedTest {

    @Test
    fun units_areOrderedAndAllA1() {
        val units = A1CurriculumSeed.units
        assertEquals(9, units.size)
        assertEquals((1..9).toList(), units.map { it.sortOrder })
        assertTrue(units.all { it.cefrLevel == CefrLevel.A1 })
        assertEquals(A1Unit1Seed.UNIT_ID, units.first().id)
    }

    @Test
    fun lessonIds_stableUniqueAndNotDialogues() {
        val ids = A1CurriculumSeed.allLessonIds()
        assertEquals(ids.toSet().size, ids.size)
        assertTrue(ids.all { it.startsWith("a1-lesson-") })
        assertTrue(ids.none { it.startsWith("dialogue-") })
        // Unité 1 conservée
        assertTrue(ids.contains(A1Unit1Seed.LESSON_ALPHABET))
        assertTrue(ids.contains(A1Unit1Seed.LESSON_GREETINGS))
        assertTrue(ids.contains(A1Unit1Seed.LESSON_INTRODUCE))
    }

    @Test
    fun eachLesson_hasThreeExercises_withExplanations() {
        A1CurriculumSeed.units.forEach { unit ->
            unit.lessons.forEach { lesson ->
                val ex = A1CurriculumSeed.exercisesFor(lesson.id)
                assertEquals(
                    "Leçon ${lesson.id}",
                    3,
                    ex.size,
                )
                assertEquals(lesson.exerciseCount, ex.size)
                assertTrue(ex.all { it.explanationFr.isNotBlank() })
                assertTrue(ex.all { it.acceptedAnswers.isNotEmpty() })
                assertEquals(ex.map { it.id }.toSet().size, ex.size)
            }
        }
    }

    @Test
    fun exerciseTypes_coverCoreSet() {
        val types = A1CurriculumSeed.units
            .flatMap { it.lessons }
            .flatMap { A1CurriculumSeed.exercisesFor(it.id) }
            .map { it.type }
            .toSet()
        assertTrue(types.contains(InteractiveExerciseType.MultipleChoice))
        assertTrue(types.contains(InteractiveExerciseType.MatchTranslation))
        assertTrue(types.contains(InteractiveExerciseType.FillBlank))
        assertTrue(types.contains(InteractiveExerciseType.WordOrder))
        assertTrue(types.contains(InteractiveExerciseType.LetterRecognition))
    }

    @Test
    fun grader_acceptsVariants_andRejectsWrong() {
        val grader = ExerciseGrader()
        val fill = A1CurriculumSeed.exercisesFor(A1CurriculumSeed.LESSON_PRONOUNS)
            .first { it.type == InteractiveExerciseType.FillBlank }
        assertTrue(grader.grade(fill, "она").isCorrect)
        assertTrue(grader.grade(fill, "  Она  ").isCorrect)
        assertFalse(grader.grade(fill, "он").isCorrect)
    }

    @Test
    fun counts_nineteenLessonsFiftySevenExercises() {
        val lessons = A1CurriculumSeed.allLessonIds()
        assertEquals(19, lessons.size)
        val exercises = lessons.sumOf { A1CurriculumSeed.exercisesFor(it).size }
        assertEquals(57, exercises)
    }
}
