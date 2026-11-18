package fr.russki.app.domain.cyrillic

/**
 * Élément de lecture (syllabe ou mot) avec prérequis de lettres.
 */
enum class CyrillicReadingKind {
    Syllable,
    Word,
}

data class CyrillicReadingItem(
    val id: String,
    val textRu: String,
    val translationFr: String,
    val kind: CyrillicReadingKind,
    /** Ids [fr.russki.app.domain.content.AlphabetLetter.id] requis avant présentation. */
    val requiredLetterIds: Set<String>,
    val readingAid: String,
    val noteFr: String? = null,
) {
    init {
        require(id.isNotBlank())
        require(textRu.isNotBlank())
        require(translationFr.isNotBlank())
        require(requiredLetterIds.isNotEmpty()) { "Prérequis lettres requis pour $id" }
        require(readingAid.isNotBlank())
    }
}

object CyrillicReadingGate {
    /**
     * true si toutes les lettres requises sont dans [taughtLetterIds].
     * Les exercices marqués diagnostic peuvent contourner via [allowDiagnostic].
     */
    fun canPresent(
        item: CyrillicReadingItem,
        taughtLetterIds: Set<String>,
        allowDiagnostic: Boolean = false,
    ): Boolean {
        if (allowDiagnostic) return true
        return item.requiredLetterIds.all { it in taughtLetterIds }
    }

    fun missingLetters(
        item: CyrillicReadingItem,
        taughtLetterIds: Set<String>,
    ): Set<String> = item.requiredLetterIds - taughtLetterIds
}
