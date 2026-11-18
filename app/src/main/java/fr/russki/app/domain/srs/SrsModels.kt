package fr.russki.app.domain.srs

/**
 * Répétition espacée — modèles purs, indépendants de l'UI.
 */

enum class SrsLearningState {
    New,
    Learning,
    Review,
    Relearning,
}

enum class SrsRating {
    Again, // À revoir
    Hard, // Difficile
    Good, // Bien
    Easy, // Facile
}

data class SrsCardState(
    val cardId: String,
    val learningState: SrsLearningState,
    val lastReviewEpochMs: Long?,
    val nextReviewEpochMs: Long,
    val repetitionCount: Int,
    val lapseCount: Int,
    val intervalDays: Double,
    val easeFactor: Double = DEFAULT_EASE,
) {
    companion object {
        const val DEFAULT_EASE: Double = 2.5
        const val MIN_EASE: Double = 1.3
    }
}

data class SrsScheduleResult(
    val previous: SrsCardState,
    val updated: SrsCardState,
    val rating: SrsRating,
)
