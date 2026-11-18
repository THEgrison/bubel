package fr.russki.app.data.flashcard

import fr.russki.app.data.content.OfflineContentRepository
import fr.russki.app.data.local.dao.FlashcardHistoryDao
import fr.russki.app.data.local.dao.SrsCardDao
import fr.russki.app.data.local.entity.FlashcardReviewHistoryEntity
import fr.russki.app.data.local.entity.SrsCardEntity
import fr.russki.app.domain.flashcard.Flashcard
import fr.russki.app.domain.srs.SrsCardState
import fr.russki.app.domain.srs.SrsLearningState
import fr.russki.app.domain.srs.SrsRating
import fr.russki.app.domain.srs.SrsScheduler

class FlashcardRepository(
    private val dao: SrsCardDao,
    private val historyDao: FlashcardHistoryDao,
    private val contentRepository: OfflineContentRepository = OfflineContentRepository.get(),
    private val scheduler: SrsScheduler = SrsScheduler(),
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    /** Assure que chaque entrée pédagogique a une carte SRS (idempotent). */
    suspend fun ensureDeckSeeded() {
        val cards = buildDeckFromContent()
        cards.forEach { card ->
            val existing = dao.get(card.id)
            if (existing == null) {
                val srs = scheduler.newCard(card.id, clock())
                dao.insertIgnore(card.toEntity(srs))
            }
        }
    }

    suspend fun dueFlashcards(): List<Flashcard> {
        ensureDeckSeeded()
        val now = clock()
        return dao.getDue(now).map { it.toFlashcard() }
    }

    suspend fun dueCount(): Int {
        ensureDeckSeeded()
        return dao.countDue(clock())
    }

    suspend fun grade(cardId: String, rating: SrsRating): SrsCardState {
        val entity = dao.get(cardId) ?: error("Carte inconnue: $cardId")
        val current = entity.toSrs()
        val result = scheduler.review(current, rating, clock())
        dao.upsert(entity.copySrs(result.updated))
        historyDao.insert(
            FlashcardReviewHistoryEntity(
                cardId = cardId,
                rating = rating.name,
                reviewedAtEpochMs = clock(),
                intervalDaysAfter = result.updated.intervalDays,
                nextReviewEpochMs = result.updated.nextReviewEpochMs,
            ),
        )
        return result.updated
    }

    suspend fun cardState(cardId: String): SrsCardState? =
        dao.get(cardId)?.toSrs()

    suspend fun reviewHistoryCount(): Int = historyDao.count()

    private fun buildDeckFromContent(): List<Flashcard> {
        val pack = contentRepository.contentPack()
        val fromVocab = pack.vocabularyModules.flatMap { module ->
            module.entries.map { entry ->
                Flashcard(
                    id = "fc-${entry.id}",
                    contentRefId = entry.id,
                    frontRu = entry.textRu,
                    backFr = entry.translationFr,
                    transliteration = null,
                    stressedRu = null,
                    exampleRu = entry.exampleRu,
                    exampleFr = entry.exampleFr,
                    audioAssetPath = entry.audioAssetPath,
                )
            }
        }
        val fromLetters = pack.alphabet.letters.map { letter ->
            Flashcard(
                id = "fc-${letter.id}",
                contentRefId = letter.id,
                frontRu = "${letter.uppercase} ${letter.lowercase}",
                backFr = "Nom : ${letter.nameRu} — ${letter.exampleWordFr}",
                transliteration = null,
                stressedRu = null,
                exampleRu = letter.exampleWordRu,
                exampleFr = letter.exampleWordFr,
                audioAssetPath = letter.audioAssetPath,
            )
        }
        return fromVocab + fromLetters
    }

    private fun Flashcard.toEntity(srs: SrsCardState): SrsCardEntity =
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
            frontRu = frontRu,
            backFr = backFr,
            transliteration = transliteration,
            stressedRu = stressedRu,
            exampleRu = exampleRu,
            exampleFr = exampleFr,
            audioAssetPath = audioAssetPath,
        )

    private fun SrsCardEntity.toFlashcard(): Flashcard =
        Flashcard(
            id = cardId,
            contentRefId = contentRefId,
            frontRu = frontRu,
            backFr = backFr,
            transliteration = transliteration,
            stressedRu = stressedRu,
            exampleRu = exampleRu,
            exampleFr = exampleFr,
            audioAssetPath = audioAssetPath,
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
}
