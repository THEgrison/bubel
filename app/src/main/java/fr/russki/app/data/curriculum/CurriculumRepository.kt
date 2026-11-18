package fr.russki.app.data.curriculum

import fr.russki.app.data.content.seed.A1Unit1Seed
import fr.russki.app.data.progress.LessonProgressRepository
import fr.russki.app.domain.curriculum.CourseUnit
import fr.russki.app.domain.curriculum.LessonOutline
import fr.russki.app.domain.curriculum.LessonProgressSummary
import fr.russki.app.domain.exercise.InteractiveExercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class UnitWithProgress(
    val unit: CourseUnit,
    val lessonProgress: Map<String, LessonProgressSummary>,
)

class CurriculumRepository(
    private val progressRepository: LessonProgressRepository,
) {

    fun a1Units(): List<CourseUnit> = listOf(A1Unit1Seed.unit)

    fun lesson(lessonId: String): LessonOutline? =
        a1Units().flatMap { it.lessons }.find { it.id == lessonId }

    fun exercises(lessonId: String): List<InteractiveExercise> =
        A1Unit1Seed.exercisesFor(lessonId)

    suspend fun loadA1Path(): UnitWithProgress = withContext(Dispatchers.IO) {
        val unit = A1Unit1Seed.unit
        val stored = progressRepository.getAll().associateBy { it.lessonId }
        val progress = unit.lessons.associate { lesson ->
            val row = stored[lesson.id]
            lesson.id to LessonProgressSummary(
                lessonId = lesson.id,
                completedExerciseCount = row?.completedExerciseCount ?: 0,
                totalExerciseCount = lesson.exerciseCount,
                completedAtEpochMs = row?.completedAtEpochMs,
            )
        }
        UnitWithProgress(unit = unit, lessonProgress = progress)
    }

    suspend fun markLessonOpened(lessonId: String) {
        progressRepository.markOpened(lessonId)
    }

    suspend fun recordExercise(lessonId: String, totalExercises: Int) {
        progressRepository.recordExerciseCompleted(lessonId, totalExercises)
    }

    suspend fun markLessonCompleted(lessonId: String, totalExercises: Int) {
        progressRepository.completeLesson(lessonId, totalExercises)
    }
}
