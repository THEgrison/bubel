package fr.russki.app.data.content.seed

import fr.russki.app.domain.content.ContentLevel
import fr.russki.app.domain.content.VocabularyEntry
import fr.russki.app.domain.content.VocabularyModule

/**
 * Module vocabulaire : salutations et expressions courantes.
 * Traductions vérifiées pour un usage débutant.
 * `audioAssetPath` n'est rempli que si le fichier Ogg licencié est réellement embarqué
 * (voir docs/AUDIO.md) — jamais de chemin fantôme.
 */
internal object GreetingsVocabularySeed {

    const val MODULE_ID: String = "vocab-greetings"

    val module: VocabularyModule = VocabularyModule(
        id = MODULE_ID,
        titleFr = "Salutations et formules courantes",
        descriptionFr = "Dire bonjour, remercier et prendre congé — premier contact en russe.",
        category = "salutations",
        level = ContentLevel.Beginner,
        entries = listOf(
            VocabularyEntry(
                id = "vocab-greet-001",
                textRu = "привет",
                translationFr = "salut (tutoiement, informel)",
                category = "salutations",
                level = ContentLevel.Beginner,
                exampleRu = "Привет! Как дела?",
                exampleFr = "Salut ! Ça va ?",
                audioAssetPath = "content/audio/ru/privet.ogg",
            ),
            VocabularyEntry(
                id = "vocab-greet-002",
                textRu = "здравствуйте",
                translationFr = "bonjour (vouvoiement, poli)",
                category = "salutations",
                level = ContentLevel.Beginner,
                exampleRu = "Здравствуйте!",
                exampleFr = "Bonjour !",
                audioAssetPath = "content/audio/ru/zdravstvuyte.ogg",
            ),
            VocabularyEntry(
                id = "vocab-greet-003",
                textRu = "доброе утро",
                translationFr = "bonjour (le matin)",
                category = "salutations",
                level = ContentLevel.Beginner,
                exampleRu = "Доброе утро!",
                exampleFr = "Bonjour ! (le matin)",
                audioAssetPath = "content/audio/ru/dobroe-utro.ogg",
            ),
            VocabularyEntry(
                id = "vocab-greet-004",
                textRu = "добрый день",
                translationFr = "bonjour / bonne journée",
                category = "salutations",
                level = ContentLevel.Beginner,
                exampleRu = "Добрый день!",
                exampleFr = "Bonjour !",
                audioAssetPath = null,
            ),
            VocabularyEntry(
                id = "vocab-greet-005",
                textRu = "добрый вечер",
                translationFr = "bonsoir",
                category = "salutations",
                level = ContentLevel.Beginner,
                exampleRu = "Добрый вечер!",
                exampleFr = "Bonsoir !",
                audioAssetPath = "content/audio/ru/dobryy-vecher.ogg",
            ),
            VocabularyEntry(
                id = "vocab-greet-006",
                textRu = "спокойной ночи",
                translationFr = "bonne nuit",
                category = "salutations",
                level = ContentLevel.Beginner,
                exampleRu = "Спокойной ночи!",
                exampleFr = "Bonne nuit !",
                audioAssetPath = null,
            ),
            VocabularyEntry(
                id = "vocab-greet-007",
                textRu = "пока",
                translationFr = "salut / à plus (au revoir informel)",
                category = "salutations",
                level = ContentLevel.Beginner,
                exampleRu = "Пока!",
                exampleFr = "Salut ! (en partant)",
                audioAssetPath = "content/audio/ru/poka.ogg",
            ),
            VocabularyEntry(
                id = "vocab-greet-008",
                textRu = "до свидания",
                translationFr = "au revoir",
                category = "salutations",
                level = ContentLevel.Beginner,
                exampleRu = "До свидания!",
                exampleFr = "Au revoir !",
                audioAssetPath = "content/audio/ru/do-svidaniya.ogg",
            ),
            VocabularyEntry(
                id = "vocab-greet-009",
                textRu = "спасибо",
                translationFr = "merci",
                category = "formules",
                level = ContentLevel.Beginner,
                exampleRu = "Спасибо большое!",
                exampleFr = "Merci beaucoup !",
                audioAssetPath = "content/audio/ru/spasibo.ogg",
            ),
            VocabularyEntry(
                id = "vocab-greet-010",
                textRu = "пожалуйста",
                translationFr = "s'il vous plaît / de rien / je vous en prie",
                category = "formules",
                level = ContentLevel.Beginner,
                exampleRu = "Пожалуйста.",
                exampleFr = "Je vous en prie. / S'il vous plaît.",
                audioAssetPath = "content/audio/ru/pozhaluysta.ogg",
            ),
            VocabularyEntry(
                id = "vocab-greet-011",
                textRu = "извините",
                translationFr = "excusez-moi / pardon (poli)",
                category = "formules",
                level = ContentLevel.Beginner,
                exampleRu = "Извините, пожалуйста.",
                exampleFr = "Excusez-moi, s'il vous plaît.",
                audioAssetPath = null,
            ),
            VocabularyEntry(
                id = "vocab-greet-012",
                textRu = "как дела?",
                translationFr = "comment ça va ?",
                category = "formules",
                level = ContentLevel.Beginner,
                exampleRu = "Привет! Как дела?",
                exampleFr = "Salut ! Comment ça va ?",
                audioAssetPath = null,
            ),
            VocabularyEntry(
                id = "vocab-greet-013",
                textRu = "хорошо",
                translationFr = "bien / d'accord",
                category = "formules",
                level = ContentLevel.Beginner,
                exampleRu = "Хорошо, спасибо.",
                exampleFr = "Bien, merci.",
                audioAssetPath = "content/audio/ru/khorosho.ogg",
            ),
            VocabularyEntry(
                id = "vocab-greet-014",
                textRu = "меня зовут",
                translationFr = "je m'appelle",
                category = "présentation",
                level = ContentLevel.Beginner,
                exampleRu = "Меня зовут Анна.",
                exampleFr = "Je m'appelle Anna.",
                audioAssetPath = null,
            ),
            VocabularyEntry(
                id = "vocab-greet-015",
                textRu = "очень приятно",
                translationFr = "enchanté(e) / ravi(e) de faire votre connaissance",
                category = "présentation",
                level = ContentLevel.Beginner,
                exampleRu = "Очень приятно!",
                exampleFr = "Enchanté(e) !",
                audioAssetPath = null,
            ),
        ),
    )
}
