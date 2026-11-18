package fr.russki.app.domain.srs

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Planificateur SRS inspiré d'Anki, version simple et déterministe.
 *
 * Règles v1 :
 * - Une bonne réponse allonge l'intervalle mais planifie toujours une prochaine révision
 *   (jamais de sortie immédiate du cycle).
 * - Again / Hard ramènent plus tôt que Good / Easy.
 * - Les dates sont calculées via une horloge injectable (epoch ms).
 */
class SrsScheduler(
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val msPerDay: Long = DAY_MS,
) {

    fun newCard(cardId: String, now: Long = clock()): SrsCardState =
        SrsCardState(
            cardId = cardId,
            learningState = SrsLearningState.New,
            lastReviewEpochMs = null,
            nextReviewEpochMs = now,
            repetitionCount = 0,
            lapseCount = 0,
            intervalDays = 0.0,
            easeFactor = SrsCardState.DEFAULT_EASE,
        )

    fun review(
        card: SrsCardState,
        rating: SrsRating,
        now: Long = clock(),
    ): SrsScheduleResult {
        val updated = when (card.learningState) {
            SrsLearningState.New, SrsLearningState.Learning, SrsLearningState.Relearning ->
                scheduleLearning(card, rating, now)
            SrsLearningState.Review ->
                scheduleReview(card, rating, now)
        }
        return SrsScheduleResult(previous = card, updated = updated, rating = rating)
    }

    fun isDue(card: SrsCardState, now: Long = clock()): Boolean =
        card.nextReviewEpochMs <= now

    private fun scheduleLearning(
        card: SrsCardState,
        rating: SrsRating,
        now: Long,
    ): SrsCardState {
        return when (rating) {
            SrsRating.Again -> card.copy(
                learningState = SrsLearningState.Learning,
                lastReviewEpochMs = now,
                nextReviewEpochMs = now + minutes(10),
                repetitionCount = 0,
                lapseCount = card.lapseCount + 1,
                intervalDays = 0.0,
            )
            SrsRating.Hard -> card.copy(
                learningState = SrsLearningState.Learning,
                lastReviewEpochMs = now,
                nextReviewEpochMs = now + minutes(30),
                repetitionCount = card.repetitionCount,
                intervalDays = 0.0,
            )
            SrsRating.Good -> {
                // Première sortie vers Review avec intervalle court — toujours une suite.
                val interval = 1.0
                card.copy(
                    learningState = SrsLearningState.Review,
                    lastReviewEpochMs = now,
                    nextReviewEpochMs = now + days(interval),
                    repetitionCount = max(1, card.repetitionCount + 1),
                    intervalDays = interval,
                )
            }
            SrsRating.Easy -> {
                val interval = 4.0
                card.copy(
                    learningState = SrsLearningState.Review,
                    lastReviewEpochMs = now,
                    nextReviewEpochMs = now + days(interval),
                    repetitionCount = max(1, card.repetitionCount + 1),
                    intervalDays = interval,
                    easeFactor = (card.easeFactor + 0.15).coerceAtMost(3.0),
                )
            }
        }
    }

    private fun scheduleReview(
        card: SrsCardState,
        rating: SrsRating,
        now: Long,
    ): SrsCardState {
        return when (rating) {
            SrsRating.Again -> card.copy(
                learningState = SrsLearningState.Relearning,
                lastReviewEpochMs = now,
                nextReviewEpochMs = now + minutes(10),
                repetitionCount = 0,
                lapseCount = card.lapseCount + 1,
                intervalDays = 0.0,
                easeFactor = (card.easeFactor - 0.2).coerceAtLeast(SrsCardState.MIN_EASE),
            )
            SrsRating.Hard -> {
                val interval = max(1.0, card.intervalDays * 1.2)
                card.copy(
                    learningState = SrsLearningState.Review,
                    lastReviewEpochMs = now,
                    nextReviewEpochMs = now + days(interval),
                    repetitionCount = card.repetitionCount + 1,
                    intervalDays = interval,
                    easeFactor = (card.easeFactor - 0.15).coerceAtLeast(SrsCardState.MIN_EASE),
                )
            }
            SrsRating.Good -> {
                val interval = max(1.0, card.intervalDays * card.easeFactor)
                card.copy(
                    learningState = SrsLearningState.Review,
                    lastReviewEpochMs = now,
                    nextReviewEpochMs = now + days(interval),
                    repetitionCount = card.repetitionCount + 1,
                    intervalDays = interval,
                )
            }
            SrsRating.Easy -> {
                val interval = max(1.0, card.intervalDays * card.easeFactor * 1.3)
                card.copy(
                    learningState = SrsLearningState.Review,
                    lastReviewEpochMs = now,
                    nextReviewEpochMs = now + days(interval),
                    repetitionCount = card.repetitionCount + 1,
                    intervalDays = interval,
                    easeFactor = (card.easeFactor + 0.15).coerceAtMost(3.0),
                )
            }
        }
    }

    private fun minutes(value: Long): Long = value * 60_000L
    private fun days(value: Double): Long = (value * msPerDay).roundToInt().toLong()

    companion object {
        const val DAY_MS: Long = 24L * 60L * 60L * 1000L
    }
}
