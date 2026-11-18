package fr.russki.app.domain.stats

/**
 * Politique de comptage du temps d'apprentissage — locale et honnête.
 *
 * - On chronomètre uniquement pendant qu'un écran pédagogique (leçon / dialogue)
 *   est composé au premier plan (`DisposableEffect` ON).
 * - Pause automatique à la sortie d'écran / arrière-plan (effet disposé).
 * - Segments plafonnés pour éviter qu'un écran laissé ouvert gonfle les minutes.
 * - Conversion en minutes : floor(ms / 60_000) pour l'affichage objectif ;
 *   accumulation interne en millisecondes.
 *
 * Ce n'est **pas** une mesure cognitive du « temps d'étude réel » :
 * c'est le temps d'affichage des écrans pédagogiques actifs.
 */
object StudyTimePolicy {
    /** Plafond d'un segment continu (évite les écrans oubliés ouverts). */
    const val MAX_SEGMENT_MS: Long = 30L * 60L * 1000L

    const val ALLOWED_GOALS_MINUTES: Int = 3 // documentation ; valeurs = 5, 10, 15

    val goalChoicesMinutes: List<Int> = listOf(5, 10, 15)

    /** Ramène à la valeur autorisée la plus proche (5 / 10 / 15). */
    fun clampGoalMinutes(minutes: Int): Int =
        goalChoicesMinutes.minBy { kotlin.math.abs(it - minutes) }

    fun clampSegmentMs(elapsedMs: Long): Long =
        elapsedMs.coerceIn(0L, MAX_SEGMENT_MS)

    fun msToWholeMinutes(ms: Long): Int =
        (ms / 60_000L).toInt().coerceAtLeast(0)
}
