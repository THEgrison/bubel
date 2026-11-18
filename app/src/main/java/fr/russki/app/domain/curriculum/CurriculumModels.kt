package fr.russki.app.domain.curriculum

/**
 * Parcours CECRL — modèles purs, indépendants de Compose / Room.
 */

enum class CefrLevel {
    A1,
    A2,
    B1,
    B2,
}

data class CourseUnit(
    val id: String,
    val cefrLevel: CefrLevel,
    val titleFr: String,
    val descriptionFr: String,
    val sortOrder: Int,
    val lessons: List<LessonOutline>,
)

data class LessonOutline(
    val id: String,
    val unitId: String,
    val titleFr: String,
    val objectiveFr: String,
    val introductionFr: String,
    val summaryFr: String,
    val sortOrder: Int,
    val exerciseCount: Int,
    /** Route legacy optionnelle (alphabet UI, vocab UI) si la leçon y renvoie. */
    val legacyRouteHint: String? = null,
)

data class LessonProgressSummary(
    val lessonId: String,
    val completedExerciseCount: Int,
    val totalExerciseCount: Int,
    val completedAtEpochMs: Long?,
) {
    /**
     * Terminée uniquement si `completedAtEpochMs` est posé par [fr.russki.app.data.progress.LessonProgressRepository.completeLesson].
     * Le seul compteur d’exercices ne suffit plus (évite un « Terminée » avant rattrapage / bilan).
     */
    val isCompleted: Boolean
        get() = completedAtEpochMs != null

    val fraction: Float
        get() = when {
            isCompleted -> 1f
            totalExerciseCount <= 0 -> 0f
            else -> (completedExerciseCount.toFloat() / totalExerciseCount).coerceIn(0f, 1f)
        }
}
