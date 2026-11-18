package fr.russki.app.core.ui.components

import fr.russki.app.domain.exercise.ExerciseGrade
import fr.russki.app.domain.exercise.InteractiveExercise
import fr.russki.app.domain.pedagogy.PedagogicalFeedbackSupport

fun PedagogicalFeedbackSupport.Snapshot.toExtras(): PedagogicalFeedbackExtras =
    PedagogicalFeedbackExtras(
        translationFr = translationFr,
        transliteration = transliteration,
        stimulusRu = stimulusRu,
        revealedNote = revealedNote,
        stressUnknown = stressUnknown,
    )

fun pedagogicalExtrasFrom(grade: ExerciseGrade): PedagogicalFeedbackExtras =
    PedagogicalFeedbackSupport.fromGrade(grade).toExtras()

fun pedagogicalExtrasFrom(exercise: InteractiveExercise, grade: ExerciseGrade): PedagogicalFeedbackExtras =
    PedagogicalFeedbackSupport.fromExercise(exercise, grade).toExtras()

fun pedagogicalExtrasFromRussian(
    textRu: String?,
    translationFr: String? = null,
    explanationHint: String? = null,
): PedagogicalFeedbackExtras =
    PedagogicalFeedbackSupport.fromRussian(
        textRu = textRu,
        translationFr = translationFr,
        explanationFr = explanationHint,
    ).toExtras()
