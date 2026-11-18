package fr.russki.app.data.content.seed

import fr.russki.app.domain.content.AlphabetModule
import fr.russki.app.domain.content.ContentPack

/**
 * Paquet de contenu pédagogique embarqué (hors ligne), versionné.
 *
 * Source de vérité actuelle pour l'alphabet et le vocabulaire de salutations.
 * Un miroir JSON versionné est fourni sous `assets/content/v1/` pour
 * évolution / import futurs sans mélanger ces données à l'UI.
 */
object SeedContentPack {

    const val VERSION: String = "1.0.0"
    const val ALPHABET_MODULE_ID: String = "alphabet-ru"
    const val EXPECTED_LETTER_COUNT: Int = 33

    val pack: ContentPack = ContentPack(
        version = VERSION,
        localeUi = "fr",
        targetLanguage = "ru",
        alphabet = AlphabetModule(
            id = ALPHABET_MODULE_ID,
            titleFr = "Alphabet russe",
            descriptionFr = "Les 33 lettres, dans l'ordre, avec guides de prononciation " +
                "et exercices — entièrement utilisable hors ligne.",
            letters = AlphabetLettersSeed.letters,
            exercises = AlphabetExercisesSeed.exercises,
        ),
        vocabularyModules = listOf(GreetingsVocabularySeed.module),
    )
}
