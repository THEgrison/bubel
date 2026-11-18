package fr.russki.app.domain.exercise

/**
 * Spécification d'exercice extensible — le moteur de leçon ne dépend pas de Compose.
 */

enum class InteractiveExerciseType {
    MultipleChoice,
    MatchTranslation,
    FillBlank,
    WordOrder,
    LetterRecognition,
}

data class InteractiveExercise(
    val id: String,
    val type: InteractiveExerciseType,
    val promptFr: String,
    val stimulus: String,
    val choices: List<String>,
    /** Variantes acceptées (au moins une). */
    val acceptedAnswers: List<String>,
    val explanationFr: String,
    val sortOrder: Int,
) {
    init {
        require(id.isNotBlank())
        require(acceptedAnswers.isNotEmpty()) { "acceptedAnswers requis pour $id" }
        require(promptFr.isNotBlank())
    }

    val primaryExpected: String get() = acceptedAnswers.first()
}

data class ExerciseGrade(
    val exerciseId: String,
    val userAnswer: String,
    val isCorrect: Boolean,
    val explanationFr: String,
    val acceptedAnswers: List<String>,
)

class ExerciseGrader(
    private val matcher: AnswerMatcher = AnswerMatcher(),
) {
    fun grade(exercise: InteractiveExercise, userAnswer: String): ExerciseGrade {
        val correct = matcher.matches(userAnswer, exercise.acceptedAnswers)
        return ExerciseGrade(
            exerciseId = exercise.id,
            userAnswer = userAnswer,
            isCorrect = correct,
            explanationFr = exercise.explanationFr,
            acceptedAnswers = exercise.acceptedAnswers,
        )
    }
}
