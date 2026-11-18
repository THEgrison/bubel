package fr.russki.app.domain.review

import fr.russki.app.domain.exercise.ExerciseGrade
import fr.russki.app.domain.srs.SrsCardState

/**
 * Élément révisable — id stable, consigne, réponses acceptées, correction fiable.
 * Issu uniquement de notions déjà étudiées (voir [fr.russki.app.data.review.ReviewableCatalog]).
 */
enum class ReviewItemKind {
    Vocabulary,
    Expression,
    LessonExercise,
    DialoguePrompt,
}

data class ReviewItem(
    val id: String,
    val kind: ReviewItemKind,
    /** Leçon ou dialogue qui débloque cet élément. */
    val unlockContentId: String,
    val contentRefId: String,
    val promptFr: String,
    val stimulus: String,
    val choices: List<String>,
    val acceptedAnswers: List<String>,
    val explanationFr: String,
) {
    init {
        require(id.startsWith(ReviewIds.PREFIX)) { "Id révision attendu (${ReviewIds.PREFIX}…)" }
        require(acceptedAnswers.isNotEmpty())
        require(promptFr.isNotBlank())
    }

    val primaryAnswer: String get() = acceptedAnswers.first()
}

object ReviewIds {
    const val PREFIX = "rv-"

    fun vocab(entryId: String) = "${PREFIX}vocab-$entryId"
    fun letter(letterId: String) = "${PREFIX}letter-$letterId"
    fun exercise(exerciseId: String) = "${PREFIX}ex-$exerciseId"
    fun dialogue(activityId: String) = "${PREFIX}dlg-$activityId"
}

enum class ReviewPhase {
    Prompt,
    Feedback,
    Summary,
}

data class ReviewSessionState(
    val queue: List<ReviewItem>,
    val index: Int,
    val phase: ReviewPhase,
    val grades: Map<String, ExerciseGrade>,
    val currentGrade: ExerciseGrade?,
    val srsAfterGrade: Map<String, SrsCardState>,
) {
    val current: ReviewItem? get() = queue.getOrNull(index)
    val correctCount: Int get() = grades.values.count { it.isCorrect }
    val answeredCount: Int get() = grades.size
    val finished: Boolean get() = phase == ReviewPhase.Summary
    val progressFraction: Float
        get() = if (queue.isEmpty()) 1f
        else (answeredCount.toFloat() / queue.size).coerceIn(0f, 1f)
}

/** Taille max d'une session courte (~quelques minutes). */
object ReviewSessionPolicy {
    const val MAX_ITEMS_PER_SESSION: Int = 10
}
