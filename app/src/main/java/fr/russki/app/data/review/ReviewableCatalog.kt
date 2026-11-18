package fr.russki.app.data.review

import fr.russki.app.data.content.seed.A1CurriculumSeed
import fr.russki.app.data.content.seed.A1DialoguesSeed
import fr.russki.app.data.content.seed.A1Unit1Seed
import fr.russki.app.data.content.seed.AlphabetLettersSeed
import fr.russki.app.data.content.seed.GreetingsVocabularySeed
import fr.russki.app.data.progress.ProgressContentIds
import fr.russki.app.domain.dialogue.DialogueActivityType
import fr.russki.app.domain.exercise.InteractiveExercise
import fr.russki.app.domain.review.ReviewIds
import fr.russki.app.domain.review.ReviewItem
import fr.russki.app.domain.review.ReviewItemKind

/**
 * Transforme les notions **déjà étudiées** en éléments révisables.
 * Aucun historique d'erreurs inventé : seul le déblocage par leçon/dialogue terminé compte.
 */
object ReviewableCatalog {

    /**
     * @param completedContentIds ids de leçons / dialogues avec `completedAtEpochMs != null`
     */
    fun itemsForCompleted(completedContentIds: Set<String>): List<ReviewItem> {
        if (completedContentIds.isEmpty()) return emptyList()
        val out = linkedMapOf<String, ReviewItem>()

        fun add(item: ReviewItem) {
            if (item.unlockContentId in completedContentIds) {
                out.putIfAbsent(item.id, item)
            }
        }

        // —— Unité 1 ——
        if (A1Unit1Seed.LESSON_ALPHABET in completedContentIds) {
            AlphabetLettersSeed.letters.take(8).forEach { letter ->
                add(
                    ReviewItem(
                        id = ReviewIds.letter(letter.id),
                        kind = ReviewItemKind.Vocabulary,
                        unlockContentId = A1Unit1Seed.LESSON_ALPHABET,
                        contentRefId = letter.id,
                        promptFr = "Quel est le nom russe de la lettre ${letter.uppercase} ?",
                        stimulus = "${letter.uppercase} ${letter.lowercase}",
                        choices = listOf(
                            letter.nameRu,
                            "бэ",
                            "вэ",
                            "гэ",
                        ).distinct(),
                        acceptedAnswers = listOf(letter.nameRu),
                        explanationFr = "${letter.uppercase} s'appelle « ${letter.nameRu} ».",
                    ),
                )
            }
            A1Unit1Seed.exercisesFor(A1Unit1Seed.LESSON_ALPHABET).forEach { add(fromExercise(it, A1Unit1Seed.LESSON_ALPHABET)) }
        }

        if (A1Unit1Seed.LESSON_GREETINGS in completedContentIds) {
            GreetingsVocabularySeed.module.entries
                .filter { it.category == "salutations" || it.category == "formules" }
                .forEach { entry ->
                    add(
                        ReviewItem(
                            id = ReviewIds.vocab(entry.id),
                            kind = ReviewItemKind.Vocabulary,
                            unlockContentId = A1Unit1Seed.LESSON_GREETINGS,
                            contentRefId = entry.id,
                            promptFr = "Que signifie « ${entry.textRu} » ?",
                            stimulus = entry.textRu,
                            choices = distractorsFr(entry.translationFr, GreetingsVocabularySeed.module.entries.map { it.translationFr }),
                            acceptedAnswers = listOf(entry.translationFr) +
                                entry.translationFr.substringBefore("(").trim().let { listOf(it) },
                            explanationFr = "${entry.textRu} = ${entry.translationFr}.",
                        ),
                    )
                }
            A1Unit1Seed.exercisesFor(A1Unit1Seed.LESSON_GREETINGS).forEach {
                add(fromExercise(it, A1Unit1Seed.LESSON_GREETINGS))
            }
        }

        if (A1Unit1Seed.LESSON_INTRODUCE in completedContentIds) {
            GreetingsVocabularySeed.module.entries
                .filter { it.category == "présentation" }
                .forEach { entry ->
                    add(
                        ReviewItem(
                            id = ReviewIds.vocab(entry.id),
                            kind = ReviewItemKind.Expression,
                            unlockContentId = A1Unit1Seed.LESSON_INTRODUCE,
                            contentRefId = entry.id,
                            promptFr = "Que signifie « ${entry.textRu} » ?",
                            stimulus = entry.textRu,
                            choices = distractorsFr(
                                entry.translationFr,
                                GreetingsVocabularySeed.module.entries.map { it.translationFr },
                            ),
                            acceptedAnswers = listOf(entry.translationFr),
                            explanationFr = "${entry.textRu} = ${entry.translationFr}.",
                        ),
                    )
                }
            A1Unit1Seed.exercisesFor(A1Unit1Seed.LESSON_INTRODUCE).forEach {
                add(fromExercise(it, A1Unit1Seed.LESSON_INTRODUCE))
            }
        }

        // —— Leçons curriculum A1 (exercices existants) ——
        A1CurriculumSeed.allLessonIds().forEach { lessonId ->
            if (lessonId in completedContentIds &&
                lessonId !in setOf(
                    A1Unit1Seed.LESSON_ALPHABET,
                    A1Unit1Seed.LESSON_GREETINGS,
                    A1Unit1Seed.LESSON_INTRODUCE,
                )
            ) {
                A1CurriculumSeed.exercisesFor(lessonId).forEach { ex ->
                    add(fromExercise(ex, lessonId))
                }
            }
        }

        // —— Dialogues terminés ——
        A1DialoguesSeed.all.forEach { script ->
            val dialogueId = script.outline.id
            if (dialogueId in completedContentIds) {
                script.activities
                    .filter {
                        it.type == DialogueActivityType.ChooseTranslation ||
                            it.type == DialogueActivityType.ChooseReply ||
                            it.type == DialogueActivityType.FillBlank
                    }
                    .forEach { activity ->
                        add(
                            ReviewItem(
                                id = ReviewIds.dialogue(activity.id),
                                kind = ReviewItemKind.DialoguePrompt,
                                unlockContentId = dialogueId,
                                contentRefId = activity.id,
                                promptFr = activity.promptFr,
                                stimulus = activity.stimulusRu,
                                choices = activity.choices,
                                acceptedAnswers = activity.acceptedAnswers,
                                explanationFr = activity.explanationFr,
                            ),
                        )
                    }
            }
        }

        return out.values.toList()
    }

    fun itemById(id: String, completedContentIds: Set<String>): ReviewItem? =
        itemsForCompleted(completedContentIds).find { it.id == id }

    private fun fromExercise(ex: InteractiveExercise, lessonId: String): ReviewItem =
        ReviewItem(
            id = ReviewIds.exercise(ex.id),
            kind = ReviewItemKind.LessonExercise,
            unlockContentId = lessonId,
            contentRefId = ex.id,
            promptFr = ex.promptFr,
            stimulus = ex.stimulus,
            choices = ex.choices,
            acceptedAnswers = ex.acceptedAnswers,
            explanationFr = ex.explanationFr,
        )

    private fun distractorsFr(correct: String, pool: List<String>): List<String> {
        val others = pool.filter { it != correct }.distinct().shuffled()
        return (listOf(correct) + others).distinct().take(4)
    }

    /** Contenu non étudié → aucun élément (invariant testé). */
    fun assertNoItemsWithoutStudy(): Boolean =
        itemsForCompleted(emptySet()).isEmpty() &&
            itemsForCompleted(setOf("unknown-lesson")).isEmpty() &&
            ProgressContentIds.isCurriculumLesson(A1Unit1Seed.LESSON_GREETINGS)
}
