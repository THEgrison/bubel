package fr.russki.app.data.content.seed

/**
 * Phrases embarquées (débutant) — distinctes des entrées vocabulaire uniques.
 * Source : bundled. Jamais mélangées à la progression utilisateur.
 */
internal object PhrasesSeed {

    data class Phrase(
        val id: String,
        val textRu: String,
        val translationFr: String,
        val category: String,
        val level: String = "Beginner",
        val audioAssetId: String? = null,
    )

    val phrases: List<Phrase> = listOf(
        Phrase(
            id = "phrase-001",
            textRu = "Как вас зовут?",
            translationFr = "Comment vous appelez-vous ?",
            category = "présentation",
        ),
        Phrase(
            id = "phrase-002",
            textRu = "Меня зовут…",
            translationFr = "Je m'appelle…",
            category = "présentation",
        ),
        Phrase(
            id = "phrase-003",
            textRu = "Очень приятно познакомиться.",
            translationFr = "Enchanté(e) de faire votre connaissance.",
            category = "présentation",
        ),
        Phrase(
            id = "phrase-004",
            textRu = "Как дела?",
            translationFr = "Comment ça va ?",
            category = "salutations",
        ),
        Phrase(
            id = "phrase-005",
            textRu = "Спасибо, хорошо.",
            translationFr = "Merci, ça va bien.",
            category = "salutations",
        ),
        Phrase(
            id = "phrase-006",
            textRu = "Извините, я не понимаю.",
            translationFr = "Excusez-moi, je ne comprends pas.",
            category = "formules",
        ),
        Phrase(
            id = "phrase-007",
            textRu = "Повторите, пожалуйста.",
            translationFr = "Répétez, s'il vous plaît.",
            category = "formules",
        ),
        Phrase(
            id = "phrase-008",
            textRu = "До завтра!",
            translationFr = "À demain !",
            category = "salutations",
        ),
    )
}
