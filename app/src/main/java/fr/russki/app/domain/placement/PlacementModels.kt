package fr.russki.app.domain.placement

import fr.russki.app.domain.exercise.ExerciseGrade
import fr.russki.app.domain.exercise.InteractiveExercise

/**
 * Compétences réellement couvertes par le curriculum embarqué.
 * Ordre = progression pédagogique du parcours.
 */
enum class PlacementSkill(
    val titleFr: String,
    /** Première leçon du thème dans le curriculum actuel. */
    val recommendedLessonId: String,
    val order: Int,
) {
    Alphabet("Alphabet cyrillique", "a1-lesson-alphabet", 1),
    Greetings("Salutations", "a1-lesson-greetings", 2),
    Introduce("Présentation", "a1-lesson-introduce", 3),
    PronounsSimple("Pronoms et phrases simples", "a1-lesson-pronouns", 4),
    NumbersAge("Nombres et âge", "a1-lesson-numbers", 5),
    CountriesLanguages("Pays et langues", "a1-lesson-countries", 6),
    Family("Famille", "a1-lesson-family", 7),
    FoodOrders("Nourriture et commandes", "a1-lesson-food", 8),
    CityShopping("Ville, transports et achats", "a1-lesson-city", 9),
}

data class PlacementQuestion(
    val id: String,
    val skill: PlacementSkill,
    val exercise: InteractiveExercise,
    val sortOrder: Int,
) {
    init {
        require(id.startsWith(PlacementIds.QUESTION_PREFIX)) {
            "Id placement attendu (${PlacementIds.QUESTION_PREFIX}…), reçu: $id"
        }
        require(exercise.id == id) {
            "L'exercice placement doit porter le même id que la question ($id ≠ ${exercise.id})"
        }
    }
}

object PlacementIds {
    const val QUESTION_PREFIX = "placement-q-"
}

enum class PlacementPhase {
    Introduction,
    Question,
    Result,
}

data class SkillScore(
    val skill: PlacementSkill,
    val correct: Int,
    val answered: Int,
) {
    val ratio: Float
        get() = if (answered <= 0) 0f else (correct.toFloat() / answered).coerceIn(0f, 1f)
}

data class PlacementRecommendation(
    val lessonId: String,
    val lessonTitleFr: String,
    val rationaleFr: String,
    /** true si les données étaient insuffisantes → début de parcours. */
    val isFallbackStart: Boolean,
)

data class PlacementResult(
    val completedAtEpochMs: Long,
    val correctCount: Int,
    val totalQuestions: Int,
    val answeredCount: Int,
    val skillScores: List<SkillScore>,
    val recommendation: PlacementRecommendation,
) {
    val percent: Int
        get() = if (totalQuestions <= 0) 0
        else ((correctCount.toFloat() / totalQuestions) * 100f).toInt().coerceIn(0, 100)
}

data class PlacementStoredResults(
    val latest: PlacementResult?,
    /** Résultat immédiatement précédent, si un nouveau passage a eu lieu. */
    val previous: PlacementResult?,
)

data class PlacementSessionState(
    val questions: List<PlacementQuestion>,
    val phase: PlacementPhase,
    val questionIndex: Int,
    val grades: Map<String, ExerciseGrade>,
    val currentGrade: ExerciseGrade?,
    val result: PlacementResult?,
) {
    val currentQuestion: PlacementQuestion?
        get() = questions.getOrNull(questionIndex)

    val correctCount: Int get() = grades.values.count { it.isCorrect }

    val answeredCount: Int get() = grades.size

    val progressFraction: Float
        get() = when (phase) {
            PlacementPhase.Introduction -> 0f
            PlacementPhase.Result -> 1f
            PlacementPhase.Question -> {
                if (questions.isEmpty()) 1f
                else (answeredCount.toFloat() / questions.size).coerceIn(0f, 1f)
            }
        }
}
