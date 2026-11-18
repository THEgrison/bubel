package fr.russki.app.data.flashcard

import fr.russki.app.data.content.OfflineContentRepository
import fr.russki.app.data.local.dao.FlashcardHistoryDao
import fr.russki.app.data.review.ReviewRepository
import fr.russki.app.domain.flashcard.Flashcard
import fr.russki.app.domain.review.ReviewItem
import fr.russki.app.domain.srs.SrsCardState
import fr.russki.app.domain.srs.SrsRating

/**
 * Facade flashcards — la file due est limitée aux notions débloquées
 * (leçons / dialogues terminés) via [ReviewRepository].
 */
class FlashcardRepository(
    private val reviewRepository: ReviewRepository,
    private val historyDao: FlashcardHistoryDao,
    private val contentRepository: OfflineContentRepository = OfflineContentRepository.get(),
) {

    suspend fun ensureDeckSeeded() {
        reviewRepository.syncUnlockedCards()
    }

    suspend fun dueFlashcards(): List<Flashcard> =
        reviewRepository.dueItems(limit = 50).map { it.toFlashcard() }

    suspend fun dueCount(): Int = reviewRepository.dueCount()

    suspend fun grade(cardId: String, rating: SrsRating): SrsCardState =
        reviewRepository.gradeSelfRating(cardId, rating)

    suspend fun cardState(cardId: String): SrsCardState? =
        reviewRepository.cardState(cardId)

    suspend fun reviewHistoryCount(): Int = historyDao.count()

    private fun ReviewItem.toFlashcard(): Flashcard =
        Flashcard(
            id = id,
            contentRefId = contentRefId,
            frontRu = stimulus.ifBlank { promptFr },
            backFr = primaryAnswer,
            transliteration = null,
            stressedRu = null,
            exampleRu = null,
            exampleFr = explanationFr,
            audioAssetPath = null,
        )
}
