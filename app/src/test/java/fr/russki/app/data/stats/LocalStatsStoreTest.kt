package fr.russki.app.data.stats

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.russki.app.domain.stats.StudyTimePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalStatsStoreTest {

    private lateinit var context: Context
    private val zone = ZoneId.of("Europe/Paris")
    private val fixedDay = LocalDate.of(2026, 11, 18)
    private val clock: Clock =
        Clock.fixed(fixedDay.atStartOfDay(zone).toInstant(), zone)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(LocalStatsStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun dailyGoal_persistsAndClampsToAllowedValues() {
        val store = LocalStatsStore(context, clock)
        store.setDailyGoalMinutes(7)
        assertEquals(5, store.dailyGoalMinutes())
        store.setDailyGoalMinutes(12)
        assertEquals(10, store.dailyGoalMinutes())
        store.setDailyGoalMinutes(40)
        assertEquals(15, store.dailyGoalMinutes())

        val reloaded = LocalStatsStore(context, clock)
        assertEquals(15, reloaded.dailyGoalMinutes())
    }

    @Test
    fun markActiveToday_isIdempotent() {
        val store = LocalStatsStore(context, clock)
        store.markActiveToday()
        store.markActiveToday()
        assertEquals(1, store.activeDays().size)
        assertTrue(store.activeDays().contains("2026-11-18"))
    }

    @Test
    fun studyTime_accumulatesAndCapsSegment() {
        val store = LocalStatsStore(context, clock)
        store.addStudyMs(fixedDay, 90_000L) // 1.5 min
        assertEquals(1, store.studiedMinutesToday())
        store.addStudyMs(fixedDay, StudyTimePolicy.MAX_SEGMENT_MS + 5_000L)
        // +30 min max
        assertEquals(31, store.studiedMinutesToday())
        assertTrue(store.activeDays().contains("2026-11-18"))
    }

    @Test
    fun dateChange_usesClockToday() {
        val store = LocalStatsStore(context, clock)
        store.markActiveDay(fixedDay.minusDays(1))
        store.markActiveToday()
        assertEquals(2, store.activeDays().size)
    }

    @Test
    fun persistenceAcrossNewStoreInstance() {
        LocalStatsStore(context, clock).apply {
            setDailyGoalMinutes(5)
            markActiveToday()
            addStudyMs(fixedDay, 120_000L)
        }
        val again = LocalStatsStore(context, clock)
        assertEquals(5, again.dailyGoalMinutes())
        assertEquals(1, again.activeDays().size)
        assertEquals(2, again.studiedMinutesToday())
    }
}
