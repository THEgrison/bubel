package fr.russki.app.domain.flashcard

import fr.russki.app.domain.srs.SrsRating

data class Flashcard(
    val id: String,
    val contentRefId: String,
    val frontRu: String,
    val backFr: String,
    val transliteration: String?,
    /** Accent tonique marqué dans le texte russe si pertinent, sinon null. */
    val stressedRu: String?,
    val exampleRu: String?,
    val exampleFr: String?,
    val audioAssetPath: String?,
)

enum class FlashcardFace {
    Front,
    Back,
}

data class FlashcardReviewUi(
    val card: Flashcard,
    val face: FlashcardFace,
    val dueCount: Int,
)

typealias FlashcardGrade = SrsRating
