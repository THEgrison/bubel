package fr.russki.app.domain.placement

import fr.russki.app.domain.exercise.ExerciseGrader

/**
 * Orchestre intro → questions → résultat. Pur / testable.
 * Ne touche jamais à la progression des leçons.
 */
class PlacementEngine(
    private val grader: ExerciseGrader = ExerciseGrader(),
) {

    fun start(questions: List<PlacementQuestion>): PlacementSessionState {
        require(questions.isNotEmpty()) { "Le test de placement nécessite des questions." }
        require(questions.map { it.id }.toSet().size == questions.size)
        return PlacementSessionState(
            questions = questions.sortedBy { it.sortOrder },
            phase = PlacementPhase.Introduction,
            questionIndex = 0,
            grades = emptyMap(),
            currentGrade = null,
            result = null,
        )
    }

    fun beginQuestions(session: PlacementSessionState): PlacementSessionState {
        check(session.phase == PlacementPhase.Introduction)
        return session.copy(phase = PlacementPhase.Question, currentGrade = null)
    }

    fun submitAnswer(session: PlacementSessionState, userAnswer: String): PlacementSessionState {
        check(session.phase == PlacementPhase.Question)
        val question = session.currentQuestion ?: error("Pas de question courante")
        session.grades[question.id]?.let {
            return session.copy(currentGrade = it)
        }
        val grade = grader.grade(question.exercise, userAnswer)
        return session.copy(
            grades = session.grades + (question.id to grade),
            currentGrade = grade,
        )
    }

    fun advanceAfterFeedback(session: PlacementSessionState): PlacementSessionState {
        check(session.phase == PlacementPhase.Question)
        check(session.currentGrade != null) { "Corriger avant d'avancer." }
        val next = session.questionIndex + 1
        return if (next >= session.questions.size) {
            session.copy(
                phase = PlacementPhase.Result,
                questionIndex = next,
                currentGrade = null,
            )
        } else {
            session.copy(
                questionIndex = next,
                currentGrade = null,
            )
        }
    }

    fun attachResult(
        session: PlacementSessionState,
        result: PlacementResult,
    ): PlacementSessionState {
        check(session.phase == PlacementPhase.Result)
        return session.copy(result = result)
    }
}
