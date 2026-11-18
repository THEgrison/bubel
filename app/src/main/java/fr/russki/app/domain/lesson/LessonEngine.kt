package fr.russki.app.domain.lesson

import fr.russki.app.domain.curriculum.LessonOutline
import fr.russki.app.domain.exercise.ExerciseGrade
import fr.russki.app.domain.exercise.ExerciseGrader
import fr.russki.app.domain.exercise.InteractiveExercise

enum class LessonPhase {
    Introduction,
    Exercise,
    Summary,
    Completed,
}

data class LessonSessionState(
    val lesson: LessonOutline,
    val exercises: List<InteractiveExercise>,
    val phase: LessonPhase,
    val exerciseIndex: Int,
    val grades: Map<String, ExerciseGrade>,
    val currentGrade: ExerciseGrade?,
) {
    val currentExercise: InteractiveExercise?
        get() = exercises.getOrNull(exerciseIndex)

    val correctCount: Int get() = grades.values.count { it.isCorrect }

    val answeredCount: Int get() = grades.size

    val progressFraction: Float
        get() = when (phase) {
            LessonPhase.Introduction -> 0f
            LessonPhase.Completed, LessonPhase.Summary -> 1f
            LessonPhase.Exercise -> {
                if (exercises.isEmpty()) 1f
                else (answeredCount.toFloat() / exercises.size).coerceIn(0f, 1f)
            }
        }
}

/**
 * Orchestre intro → exercices → résumé. Pur / testable.
 */
class LessonEngine(
    private val grader: ExerciseGrader = ExerciseGrader(),
) {

    fun start(lesson: LessonOutline, exercises: List<InteractiveExercise>): LessonSessionState {
        require(exercises.isNotEmpty()) { "Une leçon interactive nécessite des exercices." }
        require(exercises.map { it.id }.toSet().size == exercises.size)
        return LessonSessionState(
            lesson = lesson,
            exercises = exercises.sortedBy { it.sortOrder },
            phase = LessonPhase.Introduction,
            exerciseIndex = 0,
            grades = emptyMap(),
            currentGrade = null,
        )
    }

    fun beginExercises(session: LessonSessionState): LessonSessionState {
        check(session.phase == LessonPhase.Introduction)
        return session.copy(phase = LessonPhase.Exercise, currentGrade = null)
    }

    fun submitAnswer(session: LessonSessionState, userAnswer: String): LessonSessionState {
        check(session.phase == LessonPhase.Exercise)
        val exercise = session.currentExercise ?: error("Pas d'exercice courant")
        session.grades[exercise.id]?.let {
            return session.copy(currentGrade = it)
        }
        val grade = grader.grade(exercise, userAnswer)
        return session.copy(
            grades = session.grades + (exercise.id to grade),
            currentGrade = grade,
        )
    }

    fun advanceAfterFeedback(session: LessonSessionState): LessonSessionState {
        check(session.phase == LessonPhase.Exercise)
        check(session.currentGrade != null) { "Corriger avant d'avancer." }
        val next = session.exerciseIndex + 1
        return if (next >= session.exercises.size) {
            session.copy(
                phase = LessonPhase.Summary,
                exerciseIndex = next,
                currentGrade = null,
            )
        } else {
            session.copy(
                exerciseIndex = next,
                currentGrade = null,
            )
        }
    }

    fun complete(session: LessonSessionState): LessonSessionState {
        check(session.phase == LessonPhase.Summary || session.phase == LessonPhase.Completed)
        return session.copy(phase = LessonPhase.Completed)
    }
}
