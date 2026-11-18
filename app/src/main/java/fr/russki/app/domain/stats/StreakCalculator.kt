package fr.russki.app.domain.stats

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Séries de jours d'activité — déterministe, testable.
 *
 * Règles :
 * - Un jour = une date locale ISO (`yyyy-MM-dd`) enregistrée au plus une fois.
 * - Série actuelle : chaîne consécutive se terminant à **aujourd'hui** ou **hier**
 *   (si pas encore d'activité aujourd'hui). Sinon 0.
 * - Meilleure série : plus longue chaîne consécutive dans l'historique.
 * - Minuit : nouvelle date locale → nouveau jour.
 * - Changement de fuseau : les dates déjà stockées restent ; « aujourd'hui »
 *   suit le fuseau système courant (peut décaler la frontière de jour).
 */
object StreakCalculator {

    private val iso: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun parseDay(isoDate: String): LocalDate = LocalDate.parse(isoDate, iso)

    fun formatDay(date: LocalDate): String = date.format(iso)

    fun currentStreak(activeDays: Set<String>, today: LocalDate): Int {
        if (activeDays.isEmpty()) return 0
        val days = activeDays.map { parseDay(it) }.toSortedSet()
        val anchor = when {
            today in days -> today
            today.minusDays(1) in days -> today.minusDays(1)
            else -> return 0
        }
        var streak = 0
        var cursor = anchor
        while (cursor in days) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun bestStreak(activeDays: Set<String>): Int {
        if (activeDays.isEmpty()) return 0
        val sorted = activeDays.map { parseDay(it) }.sorted()
        var best = 1
        var run = 1
        for (i in 1 until sorted.size) {
            if (sorted[i] == sorted[i - 1].plusDays(1)) {
                run++
                best = maxOf(best, run)
            } else if (sorted[i] != sorted[i - 1]) {
                run = 1
            }
        }
        return best
    }

    fun isActiveOn(activeDays: Set<String>, day: LocalDate): Boolean =
        formatDay(day) in activeDays
}
