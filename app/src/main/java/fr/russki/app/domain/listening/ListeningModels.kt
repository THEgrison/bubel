package fr.russki.app.domain.listening

import fr.russki.app.domain.exercise.InteractiveExercise
import fr.russki.app.domain.exercise.InteractiveExerciseType

/**
 * Exercice d'écoute : lecture audio si disponible, sinon repli texte (stimulus).
 * Correction via [fr.russki.app.domain.exercise.ExerciseGrader] / AnswerMatcher.
 */
data class ListeningItem(
    val id: String,
    /** Clé catalogue ([fr.russki.app.data.audio.PedagogicalAudioCatalog]) ou chemin. */
    val audioKey: String,
    val promptFr: String,
    val textRuFallback: String,
    val choices: List<String>,
    val acceptedAnswers: List<String>,
    val explanationFr: String,
) {
    fun asExercise(): InteractiveExercise = InteractiveExercise(
        id = id,
        type = InteractiveExerciseType.MatchTranslation,
        promptFr = promptFr,
        stimulus = textRuFallback,
        choices = choices,
        acceptedAnswers = acceptedAnswers,
        explanationFr = explanationFr,
        sortOrder = 0,
    )
}
