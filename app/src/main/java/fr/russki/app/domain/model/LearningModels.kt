package fr.russki.app.domain.model

/**
 * Modèles métier partagés par l'UI.
 * Les instances concrètes de cette phase viennent de [fr.russki.app.data.temporary].
 */
data class LearningProgress(
    val completedLessons: Int,
    val totalLessonsInPath: Int,
    val streakDays: Int,
    val wordsSeen: Int,
)

data class ContinueLesson(
    val id: String,
    val moduleTitle: String,
    val lessonTitle: String,
    val cyrillicPreview: String,
    val progressFraction: Float,
)

data class ReviewSummary(
    val dueCount: Int,
    val nextReviewLabel: String?,
)

data class LearningModule(
    val id: String,
    val title: String,
    val subtitle: String,
    val cyrillicSample: String,
    val lessonCount: Int,
    val completedLessonCount: Int,
    val kind: ModuleKind,
)

enum class ModuleKind {
    Alphabet,
    Vocabulary,
    Phrases,
    Mixed,
}

data class ProfileSettings(
    val displayNamePlaceholder: String,
    val dailyGoalMinutes: Int,
    val soundEnabled: Boolean,
    val offlinePacksOnly: Boolean,
    val darkThemePreference: ThemePreference,
)

enum class ThemePreference {
    System,
    Light,
    Dark,
}

sealed interface HomeContent {
    data object EmptyPath : HomeContent

    data class Dashboard(
        val progress: LearningProgress,
        val continueLesson: ContinueLesson?,
        val review: ReviewSummary,
    ) : HomeContent
}

sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Ready<T>(val value: T) : LoadState<T>
    data class Empty(val message: String) : LoadState<Nothing>
    data class Error(val message: String) : LoadState<Nothing>
}
