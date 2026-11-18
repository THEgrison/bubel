package fr.russki.app.domain.pedagogy

import fr.russki.app.domain.exercise.ExerciseGrade
import fr.russki.app.domain.exercise.InteractiveExercise
import fr.russki.app.domain.pronunciation.RussianPronunciation

/**
 * Enrichit le feedback pédagogique sans inventer d’audio ni d’IPA.
 * La translittération est une **aide de lecture**, pas une transcription phonétique exacte.
 */
object PedagogicalFeedbackSupport {

    data class Snapshot(
        val stimulusRu: String?,
        val translationFr: String?,
        val transliteration: String?,
        val stressUnknown: Boolean,
        val audioAssetPath: String?,
        val explanationFr: String?,
        val revealedNote: String?,
    )

    fun fromGrade(grade: ExerciseGrade, revealedNote: String? = null): Snapshot {
        val ru = grade.stimulusRu?.takeIf { looksCyrillic(it) && it != "…" && it != "_____" }
        val tr = grade.transliteration
            ?: ru?.let { RussianPronunciation.lookupOrTransliterate(it).transliteration }
        val stressUnknown = ru?.let { RussianPronunciation.lookupOrTransliterate(it).stressUnknown } == true
        return Snapshot(
            stimulusRu = ru,
            translationFr = grade.translationFr,
            transliteration = tr,
            stressUnknown = stressUnknown,
            audioAssetPath = grade.audioAssetPath,
            explanationFr = grade.explanationFr,
            revealedNote = revealedNote ?: if (grade.revealedBeforeAnswer) {
                "Solution consultée — ne compte pas comme maîtrise"
            } else {
                null
            },
        )
    }

    fun fromExercise(exercise: InteractiveExercise, grade: ExerciseGrade): Snapshot {
        val base = fromGrade(grade)
        return base.copy(
            stimulusRu = base.stimulusRu
                ?: exercise.stimulus.takeIf { looksCyrillic(it) && it != "…" && !it.contains("_____") },
            translationFr = base.translationFr ?: exercise.translationFr,
            transliteration = base.transliteration ?: exercise.transliteration,
            audioAssetPath = base.audioAssetPath ?: exercise.audioAssetPath,
        )
    }

    fun fromRussian(
        textRu: String?,
        translationFr: String? = null,
        audioAssetPath: String? = null,
        explanationFr: String? = null,
    ): Snapshot {
        val ru = textRu?.takeIf { looksCyrillic(it) && it.isNotBlank() }
        val pron = ru?.let { RussianPronunciation.lookupOrTransliterate(it) }
        return Snapshot(
            stimulusRu = ru,
            translationFr = translationFr,
            transliteration = pron?.transliteration,
            stressUnknown = pron?.stressUnknown == true,
            audioAssetPath = audioAssetPath,
            explanationFr = explanationFr,
            revealedNote = null,
        )
    }

    /** Enrichit un exercice avec traduction / aide de lecture si absentes. */
    fun enrichExercise(exercise: InteractiveExercise): InteractiveExercise {
        if (exercise.translationFr != null && exercise.transliteration != null) return exercise
        val stim = exercise.stimulus
        val pron = if (looksCyrillic(stim)) RussianPronunciation.lookupOrTransliterate(stim) else null
        val guessedFr = exercise.translationFr ?: guessFrenchTranslation(exercise)
        return exercise.copy(
            translationFr = guessedFr,
            transliteration = exercise.transliteration
                ?: pron?.transliteration?.takeIf { looksCyrillic(stim) },
        )
    }

    private fun guessFrenchTranslation(ex: InteractiveExercise): String? {
        val ans = ex.acceptedAnswers.firstOrNull() ?: return null
        return if (looksLatin(ans)) ans else null
    }

    fun looksCyrillic(s: String): Boolean =
        s.any { it in '\u0400'..'\u04FF' }

    fun looksLatin(s: String): Boolean =
        s.any { it in 'a'..'z' || it in 'A'..'Z' || it in "àâäéèêëïîôùûüç'" }
}
