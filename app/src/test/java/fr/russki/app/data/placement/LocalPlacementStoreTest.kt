package fr.russki.app.data.placement

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.russki.app.domain.placement.PlacementRecommendation
import fr.russki.app.domain.placement.PlacementResult
import fr.russki.app.domain.placement.PlacementSkill
import fr.russki.app.domain.placement.SkillScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalPlacementStoreTest {

    private lateinit var context: Context
    private lateinit var store: LocalPlacementStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(LocalPlacementStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
        store = LocalPlacementStore(context)
    }

    @Test
    fun empty_hasNoResults() {
        val loaded = store.load()
        assertNull(loaded.latest)
        assertNull(loaded.previous)
    }

    @Test
    fun save_persistsLatest() {
        store.saveCompleted(sample(1, 10, 18))
        val again = LocalPlacementStore(context).load()
        assertNotNull(again.latest)
        assertEquals(10, again.latest!!.correctCount)
        assertEquals("a1-lesson-pronouns", again.latest!!.recommendation.lessonId)
        assertNull(again.previous)
    }

    @Test
    fun retake_keepsPrevious_andShowsLatest() {
        store.saveCompleted(sample(1, 8, 18))
        store.saveCompleted(sample(2, 15, 18))
        val loaded = store.load()
        assertEquals(15, loaded.latest!!.correctCount)
        assertEquals(2L, loaded.latest!!.completedAtEpochMs)
        assertEquals(8, loaded.previous!!.correctCount)
        assertEquals(1L, loaded.previous!!.completedAtEpochMs)
    }

    private fun sample(epoch: Long, correct: Int, total: Int) = PlacementResult(
        completedAtEpochMs = epoch,
        correctCount = correct,
        totalQuestions = total,
        answeredCount = total,
        skillScores = listOf(
            SkillScore(PlacementSkill.Alphabet, correct = 2, answered = 2),
        ),
        recommendation = PlacementRecommendation(
            lessonId = "a1-lesson-pronouns",
            lessonTitleFr = "Pronoms personnels",
            rationaleFr = "test",
            isFallbackStart = false,
        ),
    )
}
