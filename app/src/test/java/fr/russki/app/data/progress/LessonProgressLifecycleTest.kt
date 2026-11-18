package fr.russki.app.data.progress

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.russki.app.data.content.ContentSeedInitializer
import fr.russki.app.data.content.seed.A1Unit1Seed
import fr.russki.app.data.content.seed.CyrillicPathSeed
import fr.russki.app.data.curriculum.CurriculumRepository
import fr.russki.app.data.local.RusskiDatabase
import fr.russki.app.data.review.ReviewRepository
import fr.russki.app.data.review.ReviewableCatalog
import fr.russki.app.data.stats.LocalStatsStore
import fr.russki.app.data.stats.StatsRepository
import fr.russki.app.domain.curriculum.LessonProgressSummary
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Non-régression Étape 18 — cycle progression leçons (anciennes + cyrillique).
 *
 * Reproduit les défauts :
 * - `recordExerciseCompleted` posait `completedAt` trop tôt (avant rattrapage / bilan)
 * - compteur seul = « Terminée » dans l’UI
 * - fraction < 1 alors que la leçon est terminée (exerciseCount Room ≠ pratique)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LessonProgressLifecycleTest {

    private lateinit var context: Context
    private lateinit var db: RusskiDatabase
    private lateinit var progress: LessonProgressRepository
    private lateinit var curriculum: CurriculumRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = RusskiDatabase.createInMemory(context)
        progress = LessonProgressRepository(db.userLessonProgressDao())
        curriculum = CurriculumRepository(db, progress)
        runBlocking { ContentSeedInitializer(db).ensureSeeded() }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun newReviewRepo(): ReviewRepository =
        ReviewRepository(
            progressRepository = progress,
            srsDao = db.srsCardDao(),
            historyDao = db.flashcardHistoryDao(),
        )

    @Test
    fun summary_requiresCompletedAt_notExerciseCountAlone() {
        val mid = LessonProgressSummary(
            lessonId = "x",
            completedExerciseCount = 5,
            totalExerciseCount = 5,
            completedAtEpochMs = null,
        )
        assertFalse(mid.isCompleted)
        assertEquals(1f, mid.fraction, 0.001f)

        val done = mid.copy(completedAtEpochMs = 42L)
        assertTrue(done.isCompleted)
        assertEquals(1f, done.fraction, 0.001f)

        val partialDone = LessonProgressSummary(
            lessonId = "x",
            completedExerciseCount = 4,
            totalExerciseCount = 5,
            completedAtEpochMs = 99L,
        )
        assertTrue(partialDone.isCompleted)
        assertEquals(1f, partialDone.fraction, 0.001f)
    }

    @Test
    fun recordExercise_neverSetsCompletedAt_evenAtCap() = runBlocking {
        val lessonId = A1Unit1Seed.LESSON_GREETINGS
        repeat(5) {
            progress.recordExerciseCompleted(lessonId, totalExercises = 3)
        }
        val row = progress.get(lessonId)!!
        assertEquals(3, row.completedExerciseCount)
        assertNull(row.completedAtEpochMs)

        val path = curriculum.loadA1Path()
        val summary = path.units.flatMap { it.lessonProgress.values }
            .first { it.lessonId == lessonId }
        assertFalse(summary.isCompleted)
    }

    @Test
    fun quitEarly_cyrillic_notCompleted_andNoSrsUnlock() = runBlocking {
        val lessonId = CyrillicPathSeed.LESSON_G1
        progress.markOpened(lessonId)
        progress.recordExerciseCompleted(lessonId, totalExercises = 4)
        progress.recordExerciseCompleted(lessonId, totalExercises = 4)

        assertNull(progress.get(lessonId)?.completedAtEpochMs)
        assertEquals(2, progress.get(lessonId)?.completedExerciseCount)

        val path = curriculum.loadA1Path()
        val cyrUnit = path.units.first { it.unit.id == CyrillicPathSeed.UNIT_ID }
        assertFalse(cyrUnit.lessonProgress.getValue(lessonId).isCompleted)

        val review = newReviewRepo()
        review.syncUnlockedCards()
        val expectedCards = ReviewableCatalog.itemsForCompleted(setOf(lessonId)).map { it.id }.toSet()
        assertTrue(expectedCards.isNotEmpty())
        assertTrue(expectedCards.none { db.srsCardDao().get(it) != null })
    }

    @Test
    fun completeLegacyLesson_persists_reloads_andUnlocksSrs() = runBlocking {
        val lessonId = A1Unit1Seed.LESSON_GREETINGS
        progress.markOpened(lessonId)
        repeat(3) { progress.recordExerciseCompleted(lessonId, totalExercises = 3) }
        // Simule rattrapage / test final sans crédit supplémentaire, puis bilan.
        progress.completeLesson(lessonId, totalExercises = 3)

        assertNotNull(progress.get(lessonId)?.completedAtEpochMs)

        val reloadedRepo = CurriculumRepository(db, LessonProgressRepository(db.userLessonProgressDao()))
        val path = reloadedRepo.loadA1Path()
        val summary = path.units.flatMap { it.lessonProgress.values }
            .first { it.lessonId == lessonId }
        assertTrue(summary.isCompleted)
        assertEquals(1f, summary.fraction, 0.001f)

        val review = newReviewRepo()
        val unlocked = review.syncUnlockedCards()
        assertTrue(unlocked.isNotEmpty())
        val countAfterFirst = unlocked.count { db.srsCardDao().get(it.id) != null }
        assertEquals(unlocked.size, countAfterFirst)
        val again = review.syncUnlockedCards()
        assertEquals(unlocked.map { it.id }.toSet(), again.map { it.id }.toSet())
        assertEquals(countAfterFirst, again.count { db.srsCardDao().get(it.id) != null })
    }

    @Test
    fun completeCyrillicLesson_afterRematchSimulation_updatesPathStatsAndSrs() = runBlocking {
        val lessonId = CyrillicPathSeed.LESSON_G1
        val practiceTotal = 4
        progress.markOpened(lessonId)
        repeat(practiceTotal) {
            progress.recordExerciseCompleted(lessonId, totalExercises = practiceTotal)
        }
        // Encore des appels (recomposition / double clic) : pas de sur-comptage.
        progress.recordExerciseCompleted(lessonId, totalExercises = practiceTotal)
        assertEquals(practiceTotal, progress.get(lessonId)?.completedExerciseCount)
        assertNull(progress.get(lessonId)?.completedAtEpochMs)

        progress.completeLesson(lessonId, totalExercises = practiceTotal)
        assertNotNull(progress.get(lessonId)?.completedAtEpochMs)

        val path = curriculum.loadA1Path()
        val cyr = path.units.first { it.unit.id == CyrillicPathSeed.UNIT_ID }
        assertTrue(cyr.lessonProgress.getValue(lessonId).isCompleted)
        assertEquals(1f, cyr.lessonProgress.getValue(lessonId).fraction, 0.001f)
        assertFalse(
            cyr.lessonProgress.getValue(CyrillicPathSeed.LESSON_G2).isCompleted,
        )

        val completedInUnit = cyr.lessonProgress.values.count { it.isCompleted }
        assertEquals(1, completedInUnit)

        val localStore = LocalStatsStore(
            context,
            Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneId.of("Europe/Paris")),
        )
        val stats = StatsRepository(curriculum, progress, db, localStore).loadSnapshot()
        assertTrue(stats.path.completedLessons >= 1)
        assertTrue(
            stats.path.units.any {
                it.unitId == CyrillicPathSeed.UNIT_ID && it.completedLessons >= 1
            },
        )

        val review = newReviewRepo()
        val unlocked = review.syncUnlockedCards()
        assertTrue(unlocked.isNotEmpty())
        val cards = ReviewableCatalog.itemsForCompleted(setOf(lessonId))
        assertTrue(cards.all { db.srsCardDao().get(it.id) != null })
    }

    @Test
    fun completeDoesNotAttributeProgressToSiblingLesson() = runBlocking {
        progress.completeLesson(CyrillicPathSeed.LESSON_G1, totalExercises = 4)
        progress.recordExerciseCompleted(CyrillicPathSeed.LESSON_G2, totalExercises = 4)

        assertNotNull(progress.get(CyrillicPathSeed.LESSON_G1)?.completedAtEpochMs)
        assertNull(progress.get(CyrillicPathSeed.LESSON_G2)?.completedAtEpochMs)
        assertEquals(1, progress.get(CyrillicPathSeed.LESSON_G2)?.completedExerciseCount)

        val path = curriculum.loadA1Path()
        val map = path.units.flatMap { it.lessonProgress.entries }.associate { it.toPair() }
        assertTrue(map.getValue(CyrillicPathSeed.LESSON_G1).isCompleted)
        assertFalse(map.getValue(CyrillicPathSeed.LESSON_G2).isCompleted)
        assertFalse(map.getValue(A1Unit1Seed.LESSON_GREETINGS).isCompleted)
    }

    @Test
    fun processRestart_recreatesRepository_keepsCompletion() = runBlocking {
        val lessonId = CyrillicPathSeed.LESSON_SYLLABLES
        progress.completeLesson(lessonId, totalExercises = 5)
        val completedAt = progress.get(lessonId)!!.completedAtEpochMs

        // Nouveau repository / même base Room (relance processus)
        val progress2 = LessonProgressRepository(db.userLessonProgressDao())
        val curriculum2 = CurriculumRepository(db, progress2)
        assertEquals(completedAt, progress2.get(lessonId)?.completedAtEpochMs)
        assertTrue(
            curriculum2.loadA1Path().units
                .flatMap { it.lessonProgress.values }
                .first { it.lessonId == lessonId }
                .isCompleted,
        )
    }

    @Test
    fun completeLesson_isIdempotent_onCompletedAt() = runBlocking {
        var clock = 1_000L
        val timed = LessonProgressRepository(db.userLessonProgressDao()) { clock }
        timed.completeLesson(A1Unit1Seed.LESSON_ALPHABET, totalExercises = 3)
        val first = timed.get(A1Unit1Seed.LESSON_ALPHABET)!!.completedAtEpochMs
        clock = 2_000L
        timed.completeLesson(A1Unit1Seed.LESSON_ALPHABET, totalExercises = 3)
        assertEquals(first, timed.get(A1Unit1Seed.LESSON_ALPHABET)?.completedAtEpochMs)
    }
}
