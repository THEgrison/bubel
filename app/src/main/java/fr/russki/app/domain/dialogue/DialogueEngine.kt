package fr.russki.app.domain.dialogue

import fr.russki.app.domain.exercise.AnswerMatcher
import fr.russki.app.domain.exercise.ExerciseGrade

enum class DialoguePhase {
    Introduction,
    Line,
    Activity,
    Summary,
    Completed,
}

data class DialogueSessionState(
    val script: DialogueScript,
    val phase: DialoguePhase,
    val lineIndex: Int,
    /** Index dans la file d'activités pour la réplique courante. */
    val activityQueueIndex: Int,
    val translationRevealed: Boolean,
    val grades: Map<String, ExerciseGrade>,
    val currentGrade: ExerciseGrade?,
) {
    val currentLine: DialogueLine?
        get() = script.lines.getOrNull(lineIndex)

    val pendingActivities: List<DialogueActivity>
        get() = if (lineIndex < 0) emptyList() else script.activitiesAfter(lineIndex)

    val currentActivity: DialogueActivity?
        get() = pendingActivities.getOrNull(activityQueueIndex)

    val correctCount: Int get() = grades.values.count { it.isCorrect }

    val answeredCount: Int get() = grades.size

    val progressFraction: Float
        get() = when (phase) {
            DialoguePhase.Introduction -> 0f
            DialoguePhase.Summary, DialoguePhase.Completed -> 1f
            DialoguePhase.Line, DialoguePhase.Activity -> {
                val totalSteps = script.lines.size + script.activities.size
                if (totalSteps == 0) 1f
                else {
                    val linesDone = lineIndex.coerceAtLeast(0)
                    val activitiesDone = answeredCount
                    ((linesDone + activitiesDone).toFloat() / totalSteps).coerceIn(0f, 0.99f)
                }
            }
        }
}

/**
 * Orchestre intro → répliques → activités → résumé.
 * Correction via [AnswerMatcher] (pas d'IA).
 */
class DialogueEngine(
    private val matcher: AnswerMatcher = AnswerMatcher(),
) {

    fun start(script: DialogueScript): DialogueSessionState =
        DialogueSessionState(
            script = script,
            phase = DialoguePhase.Introduction,
            lineIndex = -1,
            activityQueueIndex = 0,
            translationRevealed = false,
            grades = emptyMap(),
            currentGrade = null,
        )

    fun begin(session: DialogueSessionState): DialogueSessionState {
        check(session.phase == DialoguePhase.Introduction)
        return showLine(session, 0)
    }

    fun revealTranslation(session: DialogueSessionState): DialogueSessionState {
        check(session.phase == DialoguePhase.Line)
        return session.copy(translationRevealed = true)
    }

    fun advanceFromLine(session: DialogueSessionState): DialogueSessionState {
        check(session.phase == DialoguePhase.Line)
        val activities = session.script.activitiesAfter(session.lineIndex)
        return if (activities.isNotEmpty()) {
            session.copy(
                phase = DialoguePhase.Activity,
                activityQueueIndex = 0,
                currentGrade = null,
            )
        } else {
            nextLineOrSummary(session)
        }
    }

    fun submitAnswer(session: DialogueSessionState, userAnswer: String): DialogueSessionState {
        check(session.phase == DialoguePhase.Activity)
        val activity = session.currentActivity ?: error("Aucune activité courante")
        session.grades[activity.id]?.let { return session.copy(currentGrade = it) }
        val correct = matcher.matches(userAnswer, activity.acceptedAnswers)
        val grade = ExerciseGrade(
            exerciseId = activity.id,
            userAnswer = userAnswer,
            isCorrect = correct,
            explanationFr = activity.explanationFr,
            acceptedAnswers = activity.acceptedAnswers,
        )
        return session.copy(
            grades = session.grades + (activity.id to grade),
            currentGrade = grade,
        )
    }

    fun advanceAfterFeedback(session: DialogueSessionState): DialogueSessionState {
        check(session.phase == DialoguePhase.Activity)
        check(session.currentGrade != null)
        val nextQueue = session.activityQueueIndex + 1
        return if (nextQueue < session.pendingActivities.size) {
            session.copy(
                activityQueueIndex = nextQueue,
                currentGrade = null,
            )
        } else {
            nextLineOrSummary(session)
        }
    }

    fun complete(session: DialogueSessionState): DialogueSessionState {
        check(session.phase == DialoguePhase.Summary || session.phase == DialoguePhase.Completed)
        return session.copy(phase = DialoguePhase.Completed)
    }

    fun restart(script: DialogueScript): DialogueSessionState = start(script)

    private fun showLine(session: DialogueSessionState, index: Int): DialogueSessionState {
        require(index in session.script.lines.indices)
        return session.copy(
            phase = DialoguePhase.Line,
            lineIndex = index,
            activityQueueIndex = 0,
            translationRevealed = false,
            currentGrade = null,
        )
    }

    private fun nextLineOrSummary(session: DialogueSessionState): DialogueSessionState {
        val next = session.lineIndex + 1
        return if (next >= session.script.lines.size) {
            session.copy(
                phase = DialoguePhase.Summary,
                currentGrade = null,
            )
        } else {
            showLine(session, next)
        }
    }
}
