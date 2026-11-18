package fr.russki.app.data.progress

import fr.russki.app.data.local.dao.UserLessonProgressDao
import fr.russki.app.data.local.entity.UserLessonProgressEntity

/** Progression leçons — table utilisateur, jamais touchée par le seed. */
class LessonProgressRepository(
    private val dao: UserLessonProgressDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    suspend fun get(lessonId: String): UserLessonProgressEntity? = dao.get(lessonId)

    suspend fun getAll(): List<UserLessonProgressEntity> = dao.getAll()

    suspend fun markOpened(lessonId: String) {
        val existing = dao.get(lessonId)
        dao.upsert(
            UserLessonProgressEntity(
                lessonId = lessonId,
                completedExerciseCount = existing?.completedExerciseCount ?: 0,
                lastOpenedEpochMs = clock(),
                completedAtEpochMs = existing?.completedAtEpochMs,
            ),
        )
    }

    suspend fun recordExerciseCompleted(lessonId: String, totalExercises: Int) {
        val existing = dao.get(lessonId)
        val completed = (existing?.completedExerciseCount ?: 0) + 1
        dao.upsert(
            UserLessonProgressEntity(
                lessonId = lessonId,
                completedExerciseCount = completed,
                lastOpenedEpochMs = clock(),
                completedAtEpochMs = if (completed >= totalExercises) {
                    clock()
                } else {
                    existing?.completedAtEpochMs
                },
            ),
        )
    }
}
