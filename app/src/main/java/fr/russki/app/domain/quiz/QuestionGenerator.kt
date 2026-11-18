package fr.russki.app.domain.quiz

import fr.russki.app.domain.content.AlphabetLetter
import fr.russki.app.domain.content.ContentPack
import fr.russki.app.domain.content.VocabularyEntry
import kotlin.random.Random

/**
 * Génère des questions à partir du contenu pédagogique validé.
 * Distracteurs plausibles, jamais égaux à la bonne réponse.
 */
class QuestionGenerator(
    private val random: Random = Random.Default,
) {

    fun generate(
        pack: ContentPack,
        count: Int,
        types: Set<QuizQuestionType> = QuizQuestionType.entries.toSet(),
    ): List<QuizQuestion> {
        require(count > 0)
        val pool = buildPool(pack, types)
        require(pool.isNotEmpty()) { "Aucune question générable pour les types demandés." }
        return pool.shuffled(random).take(count.coerceAtMost(pool.size))
    }

    private fun buildPool(
        pack: ContentPack,
        types: Set<QuizQuestionType>,
    ): List<QuizQuestion> {
        val letters = pack.alphabet.letters
        val vocab = pack.vocabularyModules.flatMap { it.entries }
        val out = mutableListOf<QuizQuestion>()

        if (QuizQuestionType.LetterRecognition in types) {
            letters.forEach { letter ->
                out += letterRecognition(letter, letters)
            }
        }
        if (QuizQuestionType.MultipleChoice in types) {
            letters.forEach { letter ->
                out += letterNameMultipleChoice(letter, letters)
            }
            vocab.forEach { entry ->
                out += vocabMultipleChoice(entry, vocab)
            }
        }
        if (QuizQuestionType.RussianToFrench in types) {
            vocab.forEach { entry -> out += russianToFrench(entry, vocab) }
        }
        if (QuizQuestionType.FrenchToRussian in types) {
            vocab.forEach { entry -> out += frenchToRussian(entry, vocab) }
        }
        if (QuizQuestionType.Cloze in types) {
            vocab.filter { !it.exampleRu.isNullOrBlank() }.forEach { entry ->
                clozeFromExample(entry)?.let { out += it }
            }
        }
        if (QuizQuestionType.WordOrder in types) {
            vocab.filter { (it.exampleRu?.split(' ')?.size ?: 0) >= 2 }.forEach { entry ->
                wordOrder(entry)?.let { out += it }
            }
        }
        if (QuizQuestionType.ListeningComprehension in types) {
            // Même sans fichier audio : question prête ; l'UI masque la réponse.
            letters.forEach { letter -> out += listeningLetter(letter, letters) }
            vocab.forEach { entry -> out += listeningVocab(entry, vocab) }
        }
        return out
    }

    private fun letterRecognition(
        letter: AlphabetLetter,
        all: List<AlphabetLetter>,
    ): QuizQuestion {
        val correct = letter.uppercase
        val distractors = all
            .filter { it.id != letter.id && it.uppercase != correct }
            .shuffled(random)
            .take(3)
            .map { it.uppercase }
        val choices = (distractors + correct).shuffled(random)
        return QuizQuestion(
            id = "q-letter-rec-${letter.id}",
            type = QuizQuestionType.LetterRecognition,
            contentRefId = letter.id,
            promptFr = "Quelle lettre est affichée ?",
            stimulus = letter.uppercase,
            choices = choices,
            expectedAnswer = correct,
            explanationFr = "C'est ${letter.uppercase}/${letter.lowercase}, nom russe « ${letter.nameRu} ».",
            difficulty = QuizDifficulty.Easy,
            audioAssetPath = letter.audioAssetPath,
        )
    }

    private fun letterNameMultipleChoice(
        letter: AlphabetLetter,
        all: List<AlphabetLetter>,
    ): QuizQuestion {
        val correct = letter.nameRu
        val distractors = all
            .filter { it.id != letter.id && normalize(it.nameRu) != normalize(correct) }
            .shuffled(random)
            .take(3)
            .map { it.nameRu }
        val choices = (distractors + correct).distinctBy { normalize(it) }.shuffled(random)
        return QuizQuestion(
            id = "q-letter-name-${letter.id}",
            type = QuizQuestionType.MultipleChoice,
            contentRefId = letter.id,
            promptFr = "Quel est le nom russe de cette lettre ?",
            stimulus = letter.uppercase,
            choices = choices,
            expectedAnswer = correct,
            explanationFr = "La lettre ${letter.uppercase} s'appelle « ${letter.nameRu} ».",
            difficulty = QuizDifficulty.Medium,
            audioAssetPath = letter.audioAssetPath,
        )
    }

    private fun russianToFrench(
        entry: VocabularyEntry,
        all: List<VocabularyEntry>,
    ): QuizQuestion {
        val correct = entry.translationFr
        val distractors = all
            .filter { it.id != entry.id && normalize(it.translationFr) != normalize(correct) }
            .shuffled(random)
            .take(3)
            .map { it.translationFr }
        val choices = (distractors + correct).distinctBy { normalize(it) }.shuffled(random)
        return QuizQuestion(
            id = "q-ru-fr-${entry.id}",
            type = QuizQuestionType.RussianToFrench,
            contentRefId = entry.id,
            promptFr = "Que signifie ce mot / cette expression ?",
            stimulus = entry.textRu,
            choices = choices,
            expectedAnswer = correct,
            explanationFr = "« ${entry.textRu} » signifie « ${entry.translationFr} ».",
            difficulty = QuizDifficulty.Easy,
            audioAssetPath = entry.audioAssetPath,
        )
    }

    private fun frenchToRussian(
        entry: VocabularyEntry,
        all: List<VocabularyEntry>,
    ): QuizQuestion {
        val correct = entry.textRu
        val distractors = all
            .filter { it.id != entry.id && normalize(it.textRu) != normalize(correct) }
            .shuffled(random)
            .take(3)
            .map { it.textRu }
        val choices = (distractors + correct).distinctBy { normalize(it) }.shuffled(random)
        return QuizQuestion(
            id = "q-fr-ru-${entry.id}",
            type = QuizQuestionType.FrenchToRussian,
            contentRefId = entry.id,
            promptFr = "Comment dit-on en russe ?",
            stimulus = entry.translationFr,
            choices = choices,
            expectedAnswer = correct,
            explanationFr = "On dit « ${entry.textRu} » pour « ${entry.translationFr} ».",
            difficulty = QuizDifficulty.Medium,
            audioAssetPath = entry.audioAssetPath,
        )
    }

    private fun vocabMultipleChoice(
        entry: VocabularyEntry,
        all: List<VocabularyEntry>,
    ): QuizQuestion = russianToFrench(entry, all).copy(
        id = "q-mc-${entry.id}",
        type = QuizQuestionType.MultipleChoice,
    )

    private fun clozeFromExample(entry: VocabularyEntry): QuizQuestion? {
        val example = entry.exampleRu ?: return null
        if (!example.contains(entry.textRu, ignoreCase = true)) return null
        val blanked = example.replace(entry.textRu, "____", ignoreCase = true)
        return QuizQuestion(
            id = "q-cloze-${entry.id}",
            type = QuizQuestionType.Cloze,
            contentRefId = entry.id,
            promptFr = "Complétez le texte à trous (mot russe manquant).",
            stimulus = blanked,
            choices = emptyList(),
            expectedAnswer = entry.textRu,
            explanationFr = "La phrase complète : $example — « ${entry.translationFr} ».",
            difficulty = QuizDifficulty.Hard,
            audioAssetPath = entry.audioAssetPath,
        )
    }

    private fun wordOrder(entry: VocabularyEntry): QuizQuestion? {
        val example = entry.exampleRu ?: return null
        val tokens = example.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.size < 2) return null
        var shuffled = tokens.shuffled(random)
        var guard = 0
        while (shuffled == tokens && guard < 8) {
            shuffled = tokens.shuffled(random)
            guard++
        }
        return QuizQuestion(
            id = "q-order-${entry.id}",
            type = QuizQuestionType.WordOrder,
            contentRefId = entry.id,
            promptFr = "Remettez les mots dans le bon ordre.",
            stimulus = shuffled.joinToString(" · "),
            choices = emptyList(),
            expectedAnswer = tokens.joinToString(" "),
            explanationFr = "Ordre correct : $example",
            difficulty = QuizDifficulty.Hard,
            audioAssetPath = entry.audioAssetPath,
            shuffledTokens = shuffled,
        )
    }

    private fun listeningLetter(
        letter: AlphabetLetter,
        all: List<AlphabetLetter>,
    ): QuizQuestion {
        val base = letterRecognition(letter, all)
        return base.copy(
            id = "q-listen-letter-${letter.id}",
            type = QuizQuestionType.ListeningComprehension,
            promptFr = "Écoutez (si audio disponible) puis choisissez la lettre. " +
                "La réponse reste masquée jusqu'à la correction.",
            hideAnswerUntilGraded = true,
        )
    }

    private fun listeningVocab(
        entry: VocabularyEntry,
        all: List<VocabularyEntry>,
    ): QuizQuestion {
        val base = russianToFrench(entry, all)
        return base.copy(
            id = "q-listen-vocab-${entry.id}",
            type = QuizQuestionType.ListeningComprehension,
            promptFr = "Compréhension orale : identifiez le sens. " +
                "Le texte russe n'est révélé qu'après votre réponse.",
            hideAnswerUntilGraded = true,
        )
    }

    private fun normalize(value: String): String =
        value.trim().lowercase().replace(Regex("\\s+"), " ")
}
