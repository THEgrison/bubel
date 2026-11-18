package fr.russki.app.domain.flashcard

/**
 * Forme d’exercice pour les cartes — distinct de l’état SRS (quand réviser).
 */
enum class FlashcardDifficulty {
    Easy,
    Normal,
    Hard,
    All,
}

/**
 * Mode de présentation d’une carte selon la difficulté.
 */
enum class FlashcardPromptMode {
    /** Face russe → retour français ; indices (translittération) autorisés. */
    RecognizeRuToFr,
    /** Face française → retrouver le russe (rappel actif). */
    RecallFrToRu,
    /** Audio / stimulus seul si possible, sinon RecallFrToRu. */
    ListenOrRecall,
}

fun FlashcardDifficulty.promptModeFor(card: Flashcard, index: Int): FlashcardPromptMode =
    when (this) {
        FlashcardDifficulty.Easy -> FlashcardPromptMode.RecognizeRuToFr
        FlashcardDifficulty.Normal -> FlashcardPromptMode.RecallFrToRu
        FlashcardDifficulty.Hard -> {
            if (!card.audioAssetPath.isNullOrBlank()) {
                FlashcardPromptMode.ListenOrRecall
            } else {
                FlashcardPromptMode.RecallFrToRu
            }
        }
        FlashcardDifficulty.All -> when (index % 3) {
            0 -> FlashcardPromptMode.RecognizeRuToFr
            1 -> FlashcardPromptMode.RecallFrToRu
            else -> if (!card.audioAssetPath.isNullOrBlank()) {
                FlashcardPromptMode.ListenOrRecall
            } else {
                FlashcardPromptMode.RecallFrToRu
            }
        }
    }
