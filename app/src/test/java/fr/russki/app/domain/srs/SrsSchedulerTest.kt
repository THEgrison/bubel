package fr.russki.app.domain.srs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SrsSchedulerTest {

    private val day = SrsScheduler.DAY_MS
    private var now = 0L
    private val scheduler = SrsScheduler(clock = { now }, msPerDay = day)

    @Test
    fun goodAnswer_schedulesFutureReview_notRemoved() {
        now = 0L
        val card = scheduler.newCard("c1", now)
        val afterGood = scheduler.review(card, SrsRating.Good, now).updated
        assertEquals(SrsLearningState.Review, afterGood.learningState)
        assertTrue(afterGood.nextReviewEpochMs > now)
        assertTrue(afterGood.repetitionCount >= 1)

        now = afterGood.nextReviewEpochMs
        assertTrue(scheduler.isDue(afterGood, now))
        val again = scheduler.review(afterGood, SrsRating.Good, now).updated
        assertTrue(again.nextReviewEpochMs > now)
        assertTrue(again.intervalDays >= afterGood.intervalDays)
    }

    @Test
    fun hardComesBackSoonerThanEasy() {
        now = 0L
        val base = scheduler.review(scheduler.newCard("c2"), SrsRating.Good, now).updated
        now = base.nextReviewEpochMs
        val hard = scheduler.review(base, SrsRating.Hard, now).updated
        val easy = scheduler.review(base, SrsRating.Easy, now).updated
        assertTrue(hard.nextReviewEpochMs < easy.nextReviewEpochMs)
    }

    @Test
    fun again_resetsToRelearning_andIncrementsLapses() {
        now = 0L
        var card = scheduler.review(scheduler.newCard("c3"), SrsRating.Good, now).updated
        now = card.nextReviewEpochMs
        card = scheduler.review(card, SrsRating.Again, now).updated
        assertEquals(SrsLearningState.Relearning, card.learningState)
        assertEquals(1, card.lapseCount)
        assertEquals(0, card.repetitionCount)
        assertTrue(card.nextReviewEpochMs > now)
        assertTrue(card.nextReviewEpochMs - now < day)
    }

    @Test
    fun datesAreDeterministicWithInjectedClock() {
        now = 1_000L
        val a = scheduler.newCard("x", now)
        assertFalse(scheduler.isDue(a.copy(nextReviewEpochMs = now + 1), now))
        assertTrue(scheduler.isDue(a.copy(nextReviewEpochMs = now), now))
    }
}
