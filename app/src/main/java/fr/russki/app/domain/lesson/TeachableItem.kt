package fr.russki.app.domain.lesson

/**
 * Élément enseigné **avant** les exercices (phase découverte).
 * Données pédagogiques pures — pas de Room.
 */
enum class SpeechRegister {
    Informal,
    Formal,
    Neutral,
}

data class TeachableItem(
    val id: String,
    val textRu: String,
    val translationFr: String,
    /** Translittération avec accent tonique si connu (convention [fr.russki.app.domain.pronunciation.RussianPronunciation]). */
    val transliteration: String,
    val stressUnknown: Boolean = false,
    val usageFr: String? = null,
    val exampleRu: String? = null,
    val exampleFr: String? = null,
    val audioAssetPath: String? = null,
    val register: SpeechRegister = SpeechRegister.Neutral,
    /** Confusion fréquente / piège pour francophones (optionnel). */
    val confusionFr: String? = null,
    /** Petite note grammaticale débutant (optionnel). */
    val grammarNoteFr: String? = null,
) {
    init {
        require(id.isNotBlank())
        require(textRu.isNotBlank())
        require(translationFr.isNotBlank())
        require(transliteration.isNotBlank())
    }
}

enum class PedagogyStage {
    /** Reconnaissance guidée (choix, indices). */
    Guided,
    /** Rappel actif. */
    Active,
    /** Test final de leçon. */
    Final,
}
