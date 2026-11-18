package fr.russki.app.domain.exercise

import fr.russki.app.domain.lesson.PedagogyStage

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
    /** Étape pédagogique (guidée / active / test final). Défaut : Guided. */
    val pedagogyStage: PedagogyStage = PedagogyStage.Guided,
    /** Lien vers l’élément enseigné en phase découverte. */
    val teachableId: String? = null,
    /** Traduction FR pour le feedback (si connue). */
    val translationFr: String? = null,
    /** Translittération avec accent (si connue). */
    val transliteration: String? = null,
    /** Chemin audio exact, ou null. */
    val audioAssetPath: String? = null,
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
    /** true si l’utilisateur a révélé la solution avant de répondre. */
    val revealedBeforeAnswer: Boolean = false,
    val translationFr: String? = null,
    val transliteration: String? = null,
    val audioAssetPath: String? = null,
    val stimulusRu: String? = null,
) {
    /** Compte comme maîtrise session uniquement si correct et non révélé. */
    val countsAsMastered: Boolean get() = isCorrect && !revealedBeforeAnswer
}

class ExerciseGrader(
    private val matcher: AnswerMatcher = AnswerMatcher(),
) {
    fun grade(
        exercise: InteractiveExercise,
        userAnswer: String,
        revealedBeforeAnswer: Boolean = false,
    ): ExerciseGrade {
        val correct = matcher.matches(userAnswer, exercise.acceptedAnswers)
        return ExerciseGrade(
            exerciseId = exercise.id,
            userAnswer = userAnswer,
            isCorrect = correct,
            explanationFr = exercise.explanationFr,
            acceptedAnswers = exercise.acceptedAnswers,
            revealedBeforeAnswer = revealedBeforeAnswer,
            translationFr = exercise.translationFr,
            transliteration = exercise.transliteration,
            audioAssetPath = exercise.audioAssetPath,
            stimulusRu = exercise.stimulus,
        )
    }
}
