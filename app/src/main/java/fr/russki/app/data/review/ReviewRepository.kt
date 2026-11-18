package fr.russki.app.data.review

import fr.russki.app.data.local.dao.FlashcardHistoryDao
import fr.russki.app.data.local.dao.SrsCardDao
import fr.russki.app.data.local.entity.FlashcardReviewHistoryEntity
import fr.russki.app.data.local.entity.SrsCardEntity
import fr.russki.app.data.progress.LessonProgressRepository
import fr.russki.app.data.progress.ProgressContentIds
import fr.russki.app.domain.exercise.ExerciseGrade
import fr.russki.app.domain.review.ReviewItem
import fr.russki.app.domain.review.ReviewSessionPolicy
import fr.russki.app.domain.review.toSrsRating
import fr.russki.app.domain.srs.SrsCardState
import fr.russki.app.domain.srs.SrsLearningState
import fr.russki.app.domain.srs.SrsRating
import fr.russki.app.domain.srs.SrsScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Révisions adaptatives — réutilise la table Room `srs_cards` (déjà en v3).
 * **Pas de nouvelle migration** : les éléments `rv-*` cohabitent avec d'éventuelles
 * anciennes cartes `fc-*`, mais seules les notions débloquées par une leçon/dialogue
 * terminé alimentent la file.
 *
 * Isolation : aucune écriture dans `user_lesson_progress`.
 */
class ReviewRepository(
    private val progressRepository: LessonProgressRepository,
    private val srsDao: SrsCardDao,
    private val historyDao: FlashcardHistoryDao,
    private val scheduler: SrsScheduler = SrsScheduler(),
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    suspend fun completedContentIds(): Set<String> = withContext(Dispatchers.IO) {
        progressRepository.getAll()
            .filter { it.completedAtEpochMs != null }
            .map { it.lessonId }
            .toSet()
    }

    suspend fun unlockedItems(): List<ReviewItem> = withContext(Dispatchers.IO) {
        ReviewableCatalog.itemsForCompleted(completedContentIds())
    }

    /** Insère les cartes SRS manquantes pour le contenu débloqué (idempotent). */
    suspend fun syncUnlockedCards(): List<ReviewItem> = withContext(Dispatchers.IO) {
        val items = unlockedItems()
        val now = clock()
        items.forEach { item ->
            if (srsDao.get(item.id) == null) {
                val srs = scheduler.newCard(item.id, now)
                srsDao.insertIgnore(item.toEntity(srs))
            }
        }
        items
    }

    suspend fun dueItems(limit: Int = ReviewSessionPolicy.MAX_ITEMS_PER_SESSION): List<ReviewItem> =
        withContext(Dispatchers.IO) {
            val unlocked = syncUnlockedCards()
            if (unlocked.isEmpty()) return@withContext emptyList()
            val byId = unlocked.associateBy { it.id }
            val now = clock()
            srsDao.getDue(now)
                .mapNotNull { byId[it.cardId] }
                .take(limit)
        }

    suspend fun dueCount(): Int = withContext(Dispatchers.IO) {
        val unlockedIds = syncUnlockedCards().map { it.id }.toSet()
        if (unlockedIds.isEmpty()) return@withContext 0
        val now = clock()
        srsDao.getDue(now).count { it.cardId in unlockedIds }
    }

    suspend fun gradeAnswer(item: ReviewItem, grade: ExerciseGrade): SrsCardState =
        withContext(Dispatchers.IO) {
            val beforeProgress = progressRepository.getAll()
            ensureCard(item)
            val entity = srsDao.get(item.id) ?: error("Carte absente: ${item.id}")
            val rating = grade.toSrsRating()
            val result = scheduler.review(entity.toSrs(), rating, clock())
            srsDao.upsert(entity.copySrs(result.updated))
            historyDao.insert(
                FlashcardReviewHistoryEntity(
                    cardId = item.id,
                    rating = rating.name,
                    reviewedAtEpochMs = clock(),
                    intervalDaysAfter = result.updated.intervalDays,
                    nextReviewEpochMs = result.updated.nextReviewEpochMs,
                ),
            )
            val afterProgress = progressRepository.getAll()
            check(beforeProgress.map { it.lessonId to it.completedAtEpochMs } ==
                afterProgress.map { it.lessonId to it.completedAtEpochMs }) {
                "La révision ne doit pas modifier user_lesson_progress"
            }
            result.updated
        }

    suspend fun gradeSelfRating(cardId: String, rating: SrsRating): SrsCardState =
        withContext(Dispatchers.IO) {
            val entity = srsDao.get(cardId) ?: error("Carte inconnue: $cardId")
            val result = scheduler.review(entity.toSrs(), rating, clock())
            srsDao.upsert(entity.copySrs(result.updated))
            historyDao.insert(
                FlashcardReviewHistoryEntity(
                    cardId = cardId,
                    rating = rating.name,
                    reviewedAtEpochMs = clock(),
                    intervalDaysAfter = result.updated.intervalDays,
                    nextReviewEpochMs = result.updated.nextReviewEpochMs,
                ),
            )
            result.updated
        }

    suspend fun cardState(cardId: String): SrsCardState? =
        withContext(Dispatchers.IO) { srsDao.get(cardId)?.toSrs() }

    private suspend fun ensureCard(item: ReviewItem) {
        if (srsDao.get(item.id) == null) {
            srsDao.insertIgnore(item.toEntity(scheduler.newCard(item.id, clock())))
        }
    }

    private fun ReviewItem.toEntity(srs: SrsCardState): SrsCardEntity =
        SrsCardEntity(
            cardId = id,
            contentRefId = contentRefId,
            learningState = srs.learningState.name,
            lastReviewEpochMs = srs.lastReviewEpochMs,
            nextReviewEpochMs = srs.nextReviewEpochMs,
            repetitionCount = srs.repetitionCount,
            lapseCount = srs.lapseCount,
            intervalDays = srs.intervalDays,
            easeFactor = srs.easeFactor,
            frontRu = stimulus.ifBlank { promptFr },
            backFr = primaryAnswer,
            transliteration = null,
            stressedRu = null,
            exampleRu = null,
            exampleFr = explanationFr,
            audioAssetPath = null,
        )

    private fun SrsCardEntity.toSrs(): SrsCardState =
        SrsCardState(
            cardId = cardId,
            learningState = SrsLearningState.valueOf(learningState),
            lastReviewEpochMs = lastReviewEpochMs,
            nextReviewEpochMs = nextReviewEpochMs,
            repetitionCount = repetitionCount,
            lapseCount = lapseCount,
            intervalDays = intervalDays,
            easeFactor = easeFactor,
        )

    private fun SrsCardEntity.copySrs(srs: SrsCardState): SrsCardEntity =
        copy(
            learningState = srs.learningState.name,
            lastReviewEpochMs = srs.lastReviewEpochMs,
            nextReviewEpochMs = srs.nextReviewEpochMs,
            repetitionCount = srs.repetitionCount,
            lapseCount = srs.lapseCount,
            intervalDays = srs.intervalDays,
            easeFactor = srs.easeFactor,
        )

    companion object {
        fun isReviewCardId(id: String): Boolean = id.startsWith(fr.russki.app.domain.review.ReviewIds.PREFIX)

        fun isLessonProgressId(id: String): Boolean =
            ProgressContentIds.isCurriculumLesson(id) || ProgressContentIds.isDialogue(id)
    }
}
