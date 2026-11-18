package fr.russki.app.data.stats

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.russki.app.data.content.ContentSeedInitializer
import fr.russki.app.data.content.seed.A1CurriculumSeed
import fr.russki.app.data.content.seed.A1DialoguesSeed
import fr.russki.app.data.content.seed.A1Unit1Seed
import fr.russki.app.data.curriculum.CurriculumRepository
import fr.russki.app.data.local.RusskiDatabase
import fr.russki.app.data.progress.LessonProgressRepository
import fr.russki.app.data.progress.ProgressContentIds
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StatsRepositoryTest {

    private lateinit var context: Context
    private lateinit var db: RusskiDatabase
    private lateinit var progress: LessonProgressRepository
    private lateinit var curriculum: CurriculumRepository
    private lateinit var localStore: LocalStatsStore
    private lateinit var stats: StatsRepository

    private val zone = ZoneId.of("Europe/Paris")
    private val today = LocalDate.of(2026, 11, 18)
    private val clock = Clock.fixed(today.atStartOfDay(zone).toInstant(), zone)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(LocalStatsStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
        db = RusskiDatabase.createInMemory(context)
        progress = LessonProgressRepository(db.userLessonProgressDao())
        curriculum = CurriculumRepository(db, progress) {
            ContentSeedInitializer(db).ensureSeeded()
        }
        localStore = LocalStatsStore(context, clock)
        stats = StatsRepository(curriculum, progress, db, localStore)
        runBlocking { ContentSeedInitializer(db).ensureSeeded() }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun emptyHistory_zeroPercents_fromLoadedCurriculum() = runBlocking {
        val snap = stats.loadSnapshot()
        assertEquals(A1CurriculumSeed.allLessonIds().size, snap.path.totalLessons)
        assertEquals(0, snap.path.completedLessons)
        assertEquals(0, snap.path.percentLabel)
        assertEquals(A1DialoguesSeed.outlines().size, snap.dialogues.totalDialogues)
        assertEquals(0, snap.dialogues.completedDialogues)
        assertEquals(0, snap.regularity.activeDaysCount)
        assertEquals(0, snap.quizAnswersRecorded)
    }

    @Test
    fun percentages_useCurriculumDenominators_notHardcoded() = runBlocking {
        progress.completeLesson(A1Unit1Seed.LESSON_ALPHABET, 3)
        progress.completeLesson(A1Unit1Seed.LESSON_GREETINGS, 3)
        val snap = stats.loadSnapshot()
        assertEquals(2, snap.path.completedLessons)
        assertEquals(A1CurriculumSeed.allLessonIds().size, snap.path.totalLessons)
        val expectedPercent =
            ((2f / snap.path.totalLessons) * 100).toInt()
        assertEquals(expectedPercent, snap.path.percentLabel)
        val unit1 = snap.path.units.first { it.unitId == A1Unit1Seed.UNIT_ID }
        assertEquals(2, unit1.completedLessons)
        assertEquals(3, unit1.totalLessons)
    }

    @Test
    fun dialogues_isolatedFromLessonStats() = runBlocking {
        progress.completeLesson(A1DialoguesSeed.ID_MEET, 3)
        progress.completeLesson(A1Unit1Seed.LESSON_INTRODUCE, 3)
        val snap = stats.loadSnapshot()
        assertEquals(1, snap.path.completedLessons)
        assertEquals(1, snap.dialogues.completedDialogues)
        assertTrue(snap.path.units.none { ProgressContentIds.isDialogue(it.unitId) })
    }

    @Test
    fun multipleActivitiesSameDay_oneActiveDay() = runBlocking {
        stats.markPedagogicalActivity()
        stats.markPedagogicalActivity()
        localStore.addStudyMs(today, 60_000L)
        val snap = stats.loadSnapshot()
        assertEquals(1, snap.regularity.activeDaysCount)
        assertTrue(snap.regularity.activeToday)
        assertEquals(1, snap.regularity.currentStreak)
    }

    @Test
    fun streak_withGapAndBest() = runBlocking {
        localStore.markActiveDay(today.minusDays(5))
        localStore.markActiveDay(today.minusDays(4))
        localStore.markActiveDay(today.minusDays(3))
        localStore.markActiveToday()
        val snap = stats.loadSnapshot()
        assertEquals(1, snap.regularity.currentStreak)
        assertEquals(3, snap.regularity.bestStreak)
        assertEquals(4, snap.regularity.activeDaysCount)
    }

    @Test
    fun dailyGoal_changeReflectedInSnapshot() = runBlocking {
        stats.setDailyGoalMinutes(5)
        localStore.addStudyMs(today, 5 * 60_000L)
        val snap = stats.loadSnapshot()
        assertEquals(5, snap.daily.goalMinutes)
        assertEquals(5, snap.daily.studiedMinutesToday)
        assertTrue(snap.daily.goalReached)

        stats.setDailyGoalMinutes(15)
        val after = stats.loadSnapshot()
        assertEquals(15, after.daily.goalMinutes)
        assertFalse(after.daily.goalReached)
    }
}
