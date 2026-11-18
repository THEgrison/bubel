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

    /**
     * Incrémente le compteur d’exercices réussis (pratique).
     * Ne marque **jamais** la leçon terminée : seul [completeLesson] pose `completedAtEpochMs`
     * (après rattrapage / test final / bilan selon le moteur pédagogique).
     */
    suspend fun recordExerciseCompleted(lessonId: String, totalExercises: Int) {
        val existing = dao.get(lessonId)
        val cappedTotal = totalExercises.coerceAtLeast(0)
        val completed = ((existing?.completedExerciseCount ?: 0) + 1).let { next ->
            if (cappedTotal > 0) next.coerceAtMost(cappedTotal) else next
        }
        // Pas de double crédit si déjà au plafond (recomposition / double clic).
        if (existing != null &&
            cappedTotal > 0 &&
            existing.completedExerciseCount >= cappedTotal
        ) {
            dao.upsert(
                existing.copy(lastOpenedEpochMs = clock()),
            )
            return
        }
        dao.upsert(
            UserLessonProgressEntity(
                lessonId = lessonId,
                completedExerciseCount = completed,
                lastOpenedEpochMs = clock(),
                completedAtEpochMs = existing?.completedAtEpochMs,
            ),
        )
    }

    /** Marque la leçon terminée sans sur-incrémenter le compteur au-delà du total. */
    suspend fun completeLesson(lessonId: String, totalExercises: Int) {
        val existing = dao.get(lessonId)
        val total = totalExercises.coerceAtLeast(0)
        dao.upsert(
            UserLessonProgressEntity(
                lessonId = lessonId,
                completedExerciseCount = maxOf(
                    existing?.completedExerciseCount ?: 0,
                    total,
                ),
                lastOpenedEpochMs = clock(),
                completedAtEpochMs = existing?.completedAtEpochMs ?: clock(),
            ),
        )
    }

    /**
     * Remet à zéro la progression d'un id (leçon ou dialogue) sans toucher aux autres.
     * Pas de migration : réutilise `user_lesson_progress`.
     */
    suspend fun resetProgress(contentId: String) {
        dao.upsert(
            UserLessonProgressEntity(
                lessonId = contentId,
                completedExerciseCount = 0,
                lastOpenedEpochMs = clock(),
                completedAtEpochMs = null,
            ),
        )
    }
}
