package fr.russki.app.domain.lesson

import fr.russki.app.data.content.seed.A1CurriculumSeed
import fr.russki.app.data.content.seed.A1Unit1Seed
import fr.russki.app.data.content.seed.LessonExerciseEnricher
import fr.russki.app.data.content.seed.LessonTeachCatalog
import fr.russki.app.domain.exercise.InteractiveExercise
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Garantit qu’une notion interrogée a été présentée en découverte
 * (sauf exceptions explicites : stimulus « … » / trous sans lemme isolé).
 */
class LessonTeachCoverageTest {

    private val allLessons: List<Pair<String, List<InteractiveExercise>>> by lazy {
        val unit1 = A1Unit1Seed.unit.lessons.map { lesson ->
            lesson.id to LessonExerciseEnricher.enrich(lesson.id, A1Unit1Seed.exercisesFor(lesson.id))
        }
        val curriculum = A1CurriculumSeed.units
            .filter { it.id != A1Unit1Seed.unit.id }
            .flatMap { unit ->
                unit.lessons.map { lesson ->
                    lesson.id to LessonExerciseEnricher.enrich(
                        lesson.id,
                        A1CurriculumSeed.exercisesFor(lesson.id),
                    )
                }
            }
        unit1 + curriculum
    }

    @Test
    fun everyLesson_hasTeachablesWhenCovered() {
        LessonTeachCatalog.coveredLessonIds().forEach { id ->
            assertTrue("Catalogue vide pour $id", LessonTeachCatalog.forLesson(id).isNotEmpty())
        }
    }

    @Test
    fun practiceStimuli_areTaughtOrExempt() {
        val gaps = mutableListOf<String>()
        for ((lessonId, exercises) in allLessons) {
            val teachTexts = LessonTeachCatalog.forLesson(lessonId)
                .flatMap { listOf(it.textRu, it.exampleRu).mapNotNull { t -> t?.lowercase() } }
            val practice = exercises.filter { it.pedagogyStage != PedagogyStage.Final }
            for (ex in practice) {
                if (isExemptStimulus(ex.stimulus)) continue
                val taught = isStimulusCovered(ex.stimulus, teachTexts) ||
                    ex.teachableId != null &&
                    LessonTeachCatalog.forLesson(lessonId).any { it.id == ex.teachableId }
                if (!taught) {
                    gaps += "$lessonId / ${ex.id} stimulus=${ex.stimulus}"
                }
            }
        }
        assertTrue(
            "Notions interrogées avant enseignement :\n${gaps.joinToString("\n")}",
            gaps.isEmpty(),
        )
    }

    @Test
    fun enrichedFeedback_hasTranslationOrTransliterationWhenCyrillic() {
        for ((_, exercises) in allLessons) {
            for (ex in exercises) {
                if (ex.stimulus.any { it in '\u0400'..'\u04FF' } &&
                    ex.stimulus != "…" &&
                    "_____" !in ex.stimulus
                ) {
                    assertTrue(
                        "Pas d'aide de lecture pour ${ex.id} (${ex.stimulus})",
                        !ex.transliteration.isNullOrBlank() || !ex.translationFr.isNullOrBlank(),
                    )
                }
            }
        }
    }

    private fun isExemptStimulus(stimulus: String): Boolean {
        val s = stimulus.trim()
        if (s.isEmpty() || s == "…" || s == "_____") return true
        if (s.all { it == '_' || it.isWhitespace() || it == '…' }) return true
        if (s.any { it == '_' }) return true // trous
        if (s.all { it.isDigit() || it.isWhitespace() || it == '…' }) return true
        return false
    }

    private fun isStimulusCovered(stimulus: String, teachTexts: List<String>): Boolean {
        val s = stimulus.lowercase().trim().trimEnd('!', '.', '?')
        return teachTexts.any { t ->
            val tt = t.trim().trimEnd('!', '.', '?')
            s.contains(tt) || tt.contains(s) ||
                s.split(Regex("\\s+")).any { token -> tt.contains(token) && token.length >= 2 }
        }
    }
}
