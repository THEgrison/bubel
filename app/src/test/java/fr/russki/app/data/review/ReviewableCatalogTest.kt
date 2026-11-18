package fr.russki.app.data.review

import fr.russki.app.data.content.seed.A1DialoguesSeed
import fr.russki.app.data.content.seed.A1Unit1Seed
import fr.russki.app.data.content.seed.CyrillicPathSeed
import fr.russki.app.domain.review.ReviewIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewableCatalogTest {

    @Test
    fun unstudied_yieldsEmpty() {
        assertTrue(ReviewableCatalog.itemsForCompleted(emptySet()).isEmpty())
        assertTrue(ReviewableCatalog.itemsForCompleted(setOf("a1-lesson-unknown")).isEmpty())
        assertTrue(ReviewableCatalog.assertNoItemsWithoutStudy())
    }

    @Test
    fun greetingsLesson_unlocksStableVocabIds() {
        val items = ReviewableCatalog.itemsForCompleted(setOf(A1Unit1Seed.LESSON_GREETINGS))
        assertTrue(items.isNotEmpty())
        assertTrue(items.all { it.id.startsWith(ReviewIds.PREFIX) })
        assertTrue(items.any { it.id.startsWith("rv-vocab-") || it.id.startsWith("rv-ex-") })
        assertEquals(items.size, items.map { it.id }.toSet().size)
        assertTrue(items.all { it.unlockContentId == A1Unit1Seed.LESSON_GREETINGS })
    }

    @Test
    fun dialogueCompletion_unlocksDialoguePromptsOnly() {
        val items = ReviewableCatalog.itemsForCompleted(setOf(A1DialoguesSeed.ID_MEET))
        assertTrue(items.isNotEmpty())
        assertTrue(items.all { it.id.startsWith("rv-dlg-") })
        assertFalse(items.any { it.unlockContentId == A1Unit1Seed.LESSON_GREETINGS })
    }

    @Test
    fun ids_areStableAcrossCalls() {
        val a = ReviewableCatalog.itemsForCompleted(setOf(A1Unit1Seed.LESSON_ALPHABET)).map { it.id }
        val b = ReviewableCatalog.itemsForCompleted(setOf(A1Unit1Seed.LESSON_ALPHABET)).map { it.id }
        assertEquals(a, b)
    }

    @Test
    fun alphabetLetterReviews_alwaysOfferFourDistinctChoices() {
        val items = ReviewableCatalog.itemsForCompleted(setOf(CyrillicPathSeed.LESSON_G1))
            .filter { it.id.startsWith("rv-letter-") }
        assertTrue(items.isNotEmpty())
        items.forEach { item ->
            assertEquals("Choix distincts pour ${item.id}", 4, item.choices.distinct().size)
            assertTrue(item.acceptedAnswers.first() in item.choices)
        }
    }
}
