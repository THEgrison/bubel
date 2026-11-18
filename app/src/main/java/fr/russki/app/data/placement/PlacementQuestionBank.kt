package fr.russki.app.data.placement

import fr.russki.app.data.content.seed.A1CurriculumSeed
import fr.russki.app.data.content.seed.A1Unit1Seed
import fr.russki.app.domain.exercise.InteractiveExercise
import fr.russki.app.domain.exercise.InteractiveExerciseType
import fr.russki.app.domain.placement.PlacementIds
import fr.russki.app.domain.placement.PlacementQuestion
import fr.russki.app.domain.placement.PlacementSkill

/**
 * Banque de questions du test de placement — contenu réutilisé depuis le curriculum,
 * avec des **ids dédiés** `placement-q-*` (jamais écrits dans `user_lesson_progress`).
 *
 * Uniquement des types à choix sélectionnables (pas de WordOrder) pour éviter
 * l'ambiguïté de saisie libre hors choix.
 */
object PlacementQuestionBank {

    /** Nombre attendu de questions dans le parcours complet actuel. */
    const val EXPECTED_COUNT: Int = 18

    fun buildDefault(): List<PlacementQuestion> {
        val specs = listOf(
            Spec("01", PlacementSkill.Alphabet, A1Unit1Seed.LESSON_ALPHABET, "a1-ex-alpha-01"),
            Spec("02", PlacementSkill.Alphabet, A1Unit1Seed.LESSON_ALPHABET, "a1-ex-alpha-02"),
            Spec("03", PlacementSkill.Greetings, A1Unit1Seed.LESSON_GREETINGS, "a1-ex-greet-01"),
            Spec("04", PlacementSkill.Greetings, A1Unit1Seed.LESSON_GREETINGS, "a1-ex-greet-02"),
            Spec("05", PlacementSkill.Introduce, A1Unit1Seed.LESSON_INTRODUCE, "a1-ex-intro-01"),
            Spec("06", PlacementSkill.Introduce, A1Unit1Seed.LESSON_INTRODUCE, "a1-ex-intro-02"),
            Spec("07", PlacementSkill.PronounsSimple, A1CurriculumSeed.LESSON_PRONOUNS, "a1-ex-pron-01"),
            Spec("08", PlacementSkill.PronounsSimple, A1CurriculumSeed.LESSON_PRONOUNS, "a1-ex-pron-02"),
            Spec("09", PlacementSkill.NumbersAge, A1CurriculumSeed.LESSON_NUMBERS, "a1-ex-num-01"),
            Spec("10", PlacementSkill.NumbersAge, A1CurriculumSeed.LESSON_AGE, "a1-ex-age-01"),
            Spec("11", PlacementSkill.CountriesLanguages, A1CurriculumSeed.LESSON_COUNTRIES, "a1-ex-cty-01"),
            Spec("12", PlacementSkill.CountriesLanguages, A1CurriculumSeed.LESSON_LANGUAGES, "a1-ex-lng-01"),
            Spec("13", PlacementSkill.Family, A1CurriculumSeed.LESSON_FAMILY, "a1-ex-fam-01"),
            Spec("14", PlacementSkill.Family, A1CurriculumSeed.LESSON_FAMILY, "a1-ex-fam-02"),
            Spec("15", PlacementSkill.FoodOrders, A1CurriculumSeed.LESSON_FOOD, "a1-ex-food-01"),
            Spec("16", PlacementSkill.FoodOrders, A1CurriculumSeed.LESSON_ORDER, "a1-ex-ord-01"),
            Spec("17", PlacementSkill.CityShopping, A1CurriculumSeed.LESSON_CITY, "a1-ex-city-01"),
            Spec("18", PlacementSkill.CityShopping, A1CurriculumSeed.LESSON_SHOPPING, "a1-ex-shop-01"),
        )
        return specs.mapIndexed { index, spec ->
            val source = requireNotNull(
                A1CurriculumSeed.exercisesFor(spec.sourceLessonId)
                    .firstOrNull { it.id == spec.sourceExerciseId },
            ) { "Exercice source introuvable: ${spec.sourceExerciseId}" }
            require(source.type != InteractiveExerciseType.WordOrder) {
                "WordOrder exclu du placement: ${spec.sourceExerciseId}"
            }
            require(source.choices.isNotEmpty()) {
                "Choix requis pour le placement: ${spec.sourceExerciseId}"
            }
            val id = PlacementIds.QUESTION_PREFIX + spec.suffix
            PlacementQuestion(
                id = id,
                skill = spec.skill,
                sortOrder = index + 1,
                exercise = remapped(source, id, index + 1),
            )
        }
    }

    /**
     * Filtre les questions dont la leçon recommandée de compétence
     * (et leçons sources) existent dans le curriculum chargé.
     */
    fun filterForAvailableLessons(
        questions: List<PlacementQuestion>,
        availableLessonIds: Set<String>,
    ): List<PlacementQuestion> {
        if (availableLessonIds.isEmpty()) return emptyList()
        return questions.filter { q ->
            q.skill.recommendedLessonId in availableLessonIds
        }
    }

    private fun remapped(
        source: InteractiveExercise,
        newId: String,
        order: Int,
    ): InteractiveExercise = InteractiveExercise(
        id = newId,
        type = source.type,
        promptFr = source.promptFr,
        stimulus = source.stimulus,
        choices = source.choices,
        acceptedAnswers = source.acceptedAnswers,
        explanationFr = source.explanationFr,
        sortOrder = order,
    )

    private data class Spec(
        val suffix: String,
        val skill: PlacementSkill,
        val sourceLessonId: String,
        val sourceExerciseId: String,
    )
}
