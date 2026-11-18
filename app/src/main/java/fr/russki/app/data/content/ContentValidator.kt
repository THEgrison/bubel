package fr.russki.app.data.content

import fr.russki.app.data.content.seed.SeedContentPack
import fr.russki.app.domain.content.AlphabetExerciseType
import fr.russki.app.domain.content.ContentPack

/**
 * Validation d'intégrité du contenu pédagogique.
 * À exécuter au démarrage des tests (et avant publication d'un pack).
 */
object ContentValidator {

    data class Report(
        val ok: Boolean,
        val errors: List<String>,
    )

    fun validate(pack: ContentPack = SeedContentPack.pack): Report {
        val errors = mutableListOf<String>()

        if (pack.version.isBlank()) {
            errors += "version vide"
        }
        if (pack.localeUi != "fr") {
            errors += "locale UI attendue: fr (reçu: ${pack.localeUi})"
        }

        validateAlphabet(pack, errors)
        validateVocabulary(pack, errors)

        return Report(ok = errors.isEmpty(), errors = errors)
    }

    fun requireValid(pack: ContentPack = SeedContentPack.pack) {
        val report = validate(pack)
        check(report.ok) {
            "Contenu pédagogique invalide:\n" + report.errors.joinToString("\n") { " - $it" }
        }
    }

    private fun validateAlphabet(pack: ContentPack, errors: MutableList<String>) {
        val letters = pack.alphabet.letters
        if (letters.size != SeedContentPack.EXPECTED_LETTER_COUNT) {
            errors += "alphabet: ${letters.size} lettres (attendu ${SeedContentPack.EXPECTED_LETTER_COUNT})"
        }

        val expectedOrder = (1..SeedContentPack.EXPECTED_LETTER_COUNT).toList()
        val actualOrder = letters.map { it.order }
        if (actualOrder != expectedOrder) {
            errors += "alphabet: ordre incorrect ($actualOrder)"
        }

        val expectedGlyphs = listOf(
            "Аа", "Бб", "Вв", "Гг", "Дд", "Ее", "Ёё", "Жж", "Зз", "Ии",
            "Йй", "Кк", "Лл", "Мм", "Нн", "Оо", "Пп", "Рр", "Сс", "Тт",
            "Уу", "Фф", "Хх", "Цц", "Чч", "Шш", "Щщ", "Ъъ", "Ыы", "Ьь",
            "Ээ", "Юю", "Яя",
        )
        letters.forEachIndexed { index, letter ->
            val pair = letter.uppercase + letter.lowercase
            if (index < expectedGlyphs.size && pair != expectedGlyphs[index]) {
                errors += "alphabet: position ${index + 1} = $pair (attendu ${expectedGlyphs[index]})"
            }
        }

        val ids = mutableListOf<String>()
        letters.forEach { letter ->
            ids += letter.id
            if (letter.id.isBlank()) errors += "lettre sans id"
            if (letter.uppercase.isBlank() || letter.lowercase.isBlank()) {
                errors += "lettre ${letter.id}: glyphe vide"
            }
            if (letter.nameRu.isBlank()) errors += "lettre ${letter.id}: nameRu vide"
            if (letter.pronunciationGuideFr.isBlank()) {
                errors += "lettre ${letter.id}: pronunciationGuideFr vide"
            }
            if (letter.soundInWordsFr.isBlank()) {
                errors += "lettre ${letter.id}: soundInWordsFr vide"
            }
            if (letter.exampleWordRu.isBlank()) {
                errors += "lettre ${letter.id}: exampleWordRu vide"
            }
            if (letter.exampleWordFr.isBlank()) {
                errors += "lettre ${letter.id}: exampleWordFr vide (traduction interdite vide)"
            }
            if (presentsApproximationAsExact(letter.pronunciationGuideFr) ||
                presentsApproximationAsExact(letter.soundInWordsFr)
            ) {
                errors += "lettre ${letter.id}: formulation qui présente une approximation comme exacte"
            }
        }

        pack.alphabet.exercises.forEach { exercise ->
            ids += exercise.id
            if (exercise.promptFr.isBlank()) errors += "exercice ${exercise.id}: prompt vide"
            if (exercise.options.size < 2) errors += "exercice ${exercise.id}: options insuffisantes"
            if (exercise.correctIndex !in exercise.options.indices) {
                errors += "exercice ${exercise.id}: correctIndex hors limites"
            }
            if (exercise.explanationFr.isBlank()) {
                errors += "exercice ${exercise.id}: explanationFr vide"
            }
            exercise.relatedLetterId?.let { related ->
                if (letters.none { it.id == related }) {
                    errors += "exercice ${exercise.id}: relatedLetterId inconnu ($related)"
                }
            }
        }

        val requiredTypes = AlphabetExerciseType.entries.toSet()
        val presentTypes = pack.alphabet.exercises.map { it.type }.toSet()
        val missing = requiredTypes - presentTypes
        if (missing.isNotEmpty()) {
            errors += "exercices: types manquants $missing"
        }

        findDuplicates(ids).forEach { dup ->
            errors += "id alphabet/exercices en double: $dup"
        }
    }

    private fun validateVocabulary(pack: ContentPack, errors: MutableList<String>) {
        val ids = mutableListOf<String>()
        pack.vocabularyModules.forEach { module ->
            ids += module.id
            if (module.titleFr.isBlank()) errors += "module ${module.id}: titre vide"
            if (module.entries.isEmpty()) errors += "module ${module.id}: aucune entrée"
            module.entries.forEach { entry ->
                ids += entry.id
                if (entry.textRu.isBlank()) errors += "vocab ${entry.id}: textRu vide"
                if (entry.translationFr.isBlank()) {
                    errors += "vocab ${entry.id}: translationFr vide"
                }
                if (entry.category.isBlank()) errors += "vocab ${entry.id}: category vide"
            }
        }
        findDuplicates(ids).forEach { dup ->
            errors += "id vocabulaire en double: $dup"
        }
    }

    private fun findDuplicates(ids: List<String>): List<String> =
        ids.groupingBy { it }.eachCount().filter { it.value > 1 }.keys.sorted()

    /**
     * Heuristique : refuse les formulations qui affirment une équivalence phonétique exacte.
     * Les négations explicites (« pas une transcription… », « non exacte ») sont autorisées.
     */
    internal fun presentsApproximationAsExact(text: String): Boolean {
        val normalized = text.lowercase()
        val positiveClaims = listOf(
            "se prononce exactement",
            "prononciation exacte :",
            "transcription exacte :",
            "équivalent exact de",
            "identique au français",
            "exactement comme en français",
            "c'est la même chose qu'en français",
        )
        if (positiveClaims.any { it in normalized }) return true

        // « phonétique exacte » / « transcription exacte » sans négation dans la même phrase.
        val risky = listOf("phonétique exacte", "transcription exacte", "prononciation exacte")
        return risky.any { phrase ->
            val idx = normalized.indexOf(phrase)
            if (idx < 0) return@any false
            val windowStart = (idx - 24).coerceAtLeast(0)
            val window = normalized.substring(windowStart, idx)
            val negated = listOf("pas une ", "pas de ", "non ", "jamais ", "sans ", "aucune ")
                .any { it in window }
            !negated
        }
    }
}
