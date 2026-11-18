package fr.russki.app.data.stats

import android.content.Context
import android.content.SharedPreferences
import fr.russki.app.domain.stats.StreakCalculator
import fr.russki.app.domain.stats.StudyTimePolicy
import org.json.JSONObject
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/**
 * Persistance locale des stats (SharedPreferences) — **pas de migration Room**.
 *
 * Justification : jours d'activité + minutes d'étude + objectif quotidien
 * n'existaient pas en base ; une table Room forcerait v3→v4 alors que
 * ces compteurs ne sont pas liés au schéma curriculum.
 */
class LocalStatsStore(
    context: Context,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
) {

    fun today(): LocalDate = LocalDate.now(clock)

    fun zoneId(): ZoneId = clock.zone

    fun dailyGoalMinutes(): Int =
        StudyTimePolicy.clampGoalMinutes(
            prefs.getInt(KEY_DAILY_GOAL, StudyTimePolicy.goalChoicesMinutes[1]),
        )

    fun setDailyGoalMinutes(minutes: Int) {
        prefs.edit()
            .putInt(KEY_DAILY_GOAL, StudyTimePolicy.clampGoalMinutes(minutes))
            .apply()
    }

    fun activeDays(): Set<String> =
        prefs.getStringSet(KEY_ACTIVE_DAYS, emptySet())?.toSet().orEmpty()

    /** Enregistre le jour courant comme actif (idempotent). */
    fun markActiveToday() {
        markActiveDay(today())
    }

    fun markActiveDay(day: LocalDate) {
        val key = StreakCalculator.formatDay(day)
        val next = activeDays().toMutableSet().apply { add(key) }
        prefs.edit().putStringSet(KEY_ACTIVE_DAYS, next).apply()
    }

    fun studyMsForDay(day: LocalDate): Long {
        val map = readStudyMap()
        return map.optLong(StreakCalculator.formatDay(day), 0L)
    }

    fun addStudyMs(day: LocalDate, rawElapsedMs: Long) {
        val clamped = StudyTimePolicy.clampSegmentMs(rawElapsedMs)
        if (clamped <= 0L) return
        val key = StreakCalculator.formatDay(day)
        val map = readStudyMap()
        val updated = map.optLong(key, 0L) + clamped
        map.put(key, updated)
        prefs.edit().putString(KEY_STUDY_MS, map.toString()).apply()
        markActiveDay(day)
    }

    fun studiedMinutesToday(): Int =
        StudyTimePolicy.msToWholeMinutes(studyMsForDay(today()))

    private fun readStudyMap(): JSONObject {
        val raw = prefs.getString(KEY_STUDY_MS, null) ?: return JSONObject()
        return runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
    }

    companion object {
        const val PREFS_NAME = "russki_local_stats"
        private const val KEY_DAILY_GOAL = "daily_goal_minutes"
        private const val KEY_ACTIVE_DAYS = "active_days"
        private const val KEY_STUDY_MS = "study_ms_by_day"
    }
}
