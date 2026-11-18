package fr.russki.app.domain.review

import fr.russki.app.domain.exercise.ExerciseGrade
import fr.russki.app.domain.exercise.ExerciseGrader
import fr.russki.app.domain.exercise.InteractiveExercise
import fr.russki.app.domain.exercise.InteractiveExerciseType
import fr.russki.app.domain.pedagogy.PedagogicalFeedbackSupport
import fr.russki.app.domain.srs.SrsCardState

/**
 * Session de révision active (réponse → correction → suite).
 * La planification SRS est appliquée à l'extérieur via le repository.
 */
class ReviewEngine(
    private val grader: ExerciseGrader = ExerciseGrader(),
) {

    fun start(items: List<ReviewItem>): ReviewSessionState {
        require(items.isNotEmpty()) { "File de révision vide." }
        return ReviewSessionState(
            queue = items,
            index = 0,
            phase = ReviewPhase.Prompt,
            grades = emptyMap(),
            currentGrade = null,
            srsAfterGrade = emptyMap(),
        )
    }

    fun submit(session: ReviewSessionState, userAnswer: String): ReviewSessionState {
        check(session.phase == ReviewPhase.Prompt)
        val item = session.current ?: error("Pas d'élément courant")
        session.grades[item.id]?.let { return session.copy(currentGrade = it, phase = ReviewPhase.Feedback) }
        val exercise = PedagogicalFeedbackSupport.enrichExercise(item.asExercise())
        val grade = grader.grade(exercise, userAnswer)
        return session.copy(
            grades = session.grades + (item.id to grade),
            currentGrade = grade,
            phase = ReviewPhase.Feedback,
        )
    }

    fun recordSrs(session: ReviewSessionState, srs: SrsCardState): ReviewSessionState {
        val item = session.current ?: return session
        return session.copy(srsAfterGrade = session.srsAfterGrade + (item.id to srs))
    }

    fun advance(session: ReviewSessionState): ReviewSessionState {
        check(session.phase == ReviewPhase.Feedback)
        check(session.currentGrade != null)
        val next = session.index + 1
        return if (next >= session.queue.size) {
            session.copy(phase = ReviewPhase.Summary, index = next, currentGrade = null)
        } else {
            session.copy(
                index = next,
                phase = ReviewPhase.Prompt,
                currentGrade = null,
            )
        }
    }

    private fun ReviewItem.asExercise(): InteractiveExercise = InteractiveExercise(
        id = id,
        type = if (choices.size >= 2) {
            InteractiveExerciseType.MultipleChoice
        } else {
            InteractiveExerciseType.FillBlank
        },
        promptFr = promptFr,
        stimulus = stimulus,
        choices = choices,
        acceptedAnswers = acceptedAnswers,
        explanationFr = explanationFr,
        sortOrder = 0,
    )
}

fun ExerciseGrade.toSrsRating(): fr.russki.app.domain.srs.SrsRating =
    if (isCorrect) fr.russki.app.domain.srs.SrsRating.Good
    else fr.russki.app.domain.srs.SrsRating.Again
