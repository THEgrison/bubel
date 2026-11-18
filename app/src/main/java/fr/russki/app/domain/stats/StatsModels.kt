package fr.russki.app.domain.stats

/**
 * Statistiques dérivées des données réellement disponibles.
 * Pas d'invention d'historique de tentatives de leçons (non stocké).
 */

data class UnitProgressStat(
    val unitId: String,
    val titleFr: String,
    val completedLessons: Int,
    val totalLessons: Int,
) {
    val fraction: Float
        get() = if (totalLessons <= 0) 0f
        else (completedLessons.toFloat() / totalLessons).coerceIn(0f, 1f)
}

data class PathProgressStat(
    val completedLessons: Int,
    val totalLessons: Int,
    val units: List<UnitProgressStat>,
) {
    val fraction: Float
        get() = if (totalLessons <= 0) 0f
        else (completedLessons.toFloat() / totalLessons).coerceIn(0f, 1f)

    val percentLabel: Int get() = (fraction * 100).toInt()
}

data class DialogueProgressStat(
    val completedDialogues: Int,
    val totalDialogues: Int,
) {
    val fraction: Float
        get() = if (totalDialogues <= 0) 0f
        else (completedDialogues.toFloat() / totalDialogues).coerceIn(0f, 1f)
}

/**
 * Temps d'étude du jour — somme des segments chronométrés en avant-plan
 * sur les écrans pédagogiques (voir [StudyTimePolicy]).
 */
data class DailyGoalStat(
    val goalMinutes: Int,
    val studiedMinutesToday: Int,
    val goalReached: Boolean,
) {
    val fraction: Float
        get() = if (goalMinutes <= 0) 0f
        else (studiedMinutesToday.toFloat() / goalMinutes).coerceIn(0f, 1f)
}

data class RegularityStat(
    val activeDaysCount: Int,
    val currentStreak: Int,
    val bestStreak: Int,
    val activeToday: Boolean,
    /** Jours ISO `yyyy-MM-dd` (fuseau système au moment de l'enregistrement). */
    val recentActiveDays: List<String>,
)

data class LearningStatsSnapshot(
    val path: PathProgressStat,
    val dialogues: DialogueProgressStat,
    val daily: DailyGoalStat,
    val regularity: RegularityStat,
    /**
     * Exercices de leçon : seuls les compteurs `completedExerciseCount` existent
     * (pas d'historique de tentatives individuelles).
     */
    val lessonExerciseCompletionsRecorded: Int,
    /** Réponses de quiz SRS/quiz libre — table `quiz_answers`, distincte des leçons. */
    val quizAnswersRecorded: Int,
    val quizCorrectAnswers: Int,
)
