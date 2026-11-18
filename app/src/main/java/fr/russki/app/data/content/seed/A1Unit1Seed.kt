package fr.russki.app.data.content.seed

import fr.russki.app.domain.curriculum.CefrLevel
import fr.russki.app.domain.curriculum.CourseUnit
import fr.russki.app.domain.curriculum.LessonOutline
import fr.russki.app.domain.exercise.InteractiveExercise
import fr.russki.app.domain.exercise.InteractiveExerciseType

/**
 * Unité A1 « Premiers pas » — contenu embarqué versionné.
 * Les exercices sont des données pédagogiques, pas des maquettes UI.
 */
object A1Unit1Seed {

    const val UNIT_ID = "a1-unit-premiers-pas"
    const val LESSON_ALPHABET = "a1-lesson-alphabet"
    const val LESSON_GREETINGS = "a1-lesson-greetings"
    const val LESSON_INTRODUCE = "a1-lesson-introduce"

    val unit: CourseUnit = CourseUnit(
        id = UNIT_ID,
        cefrLevel = CefrLevel.A1,
        titleFr = "Premiers pas",
        descriptionFr = "Alphabet, salutations et se présenter — niveau A1 débutant.",
        sortOrder = 1,
        lessons = listOf(
            LessonOutline(
                id = LESSON_ALPHABET,
                unitId = UNIT_ID,
                titleFr = "Alphabet cyrillique",
                objectiveFr = "Reconnaître et nommer les principales lettres russes utiles au quotidien.",
                introductionFr = "Le russe s'écrit en cyrillique. Cette leçon reprend les lettres " +
                    "et des exercices de reconnaissance — entièrement hors ligne.",
                summaryFr = "Vous savez distinguer des lettres cyrilliques et les associer à leur nom.",
                sortOrder = 1,
                exerciseCount = 3,
                legacyRouteHint = "lesson/alphabet",
            ),
            LessonOutline(
                id = LESSON_GREETINGS,
                unitId = UNIT_ID,
                titleFr = "Salutations",
                objectiveFr = "Comprendre et choisir les formules de politesse de base.",
                introductionFr = "Dire bonjour, remercier et prendre congé : premières " +
                    "expressions pour un contact simple en russe.",
                summaryFr = "Vous reconnaissez привет, здравствуйте, спасибо et до свидания.",
                sortOrder = 2,
                exerciseCount = 3,
                legacyRouteHint = "lesson/vocabulary/greetings",
            ),
            LessonOutline(
                id = LESSON_INTRODUCE,
                unitId = UNIT_ID,
                titleFr = "Se présenter",
                objectiveFr = "Dire son prénom et répondre à « comment vous appelez-vous ? ».",
                introductionFr = "Après les salutations, on apprend à se nommer. " +
                    "Plusieurs formulations sont acceptées (ё/е, espaces).",
                summaryFr = "Vous savez utiliser « Меня зовут… » et reconnaître la question associée.",
                sortOrder = 3,
                exerciseCount = 3,
                legacyRouteHint = null,
            ),
        ),
    )

    fun exercisesFor(lessonId: String): List<InteractiveExercise> = when (lessonId) {
        LESSON_ALPHABET -> alphabetExercises
        LESSON_GREETINGS -> greetingsExercises
        LESSON_INTRODUCE -> introduceExercises
        else -> emptyList()
    }

    private val alphabetExercises = listOf(
        InteractiveExercise(
            id = "a1-ex-alpha-01",
            type = InteractiveExerciseType.LetterRecognition,
            promptFr = "Quelle lettre majuscule correspond au son proche de « a » français ?",
            stimulus = "А",
            choices = listOf("А", "Б", "В", "Г"),
            acceptedAnswers = listOf("А", "а"),
            explanationFr = "А / а se rapproche du « a » français dans papa — ce n'est pas une transcription exacte.",
            sortOrder = 1,
        ),
        InteractiveExercise(
            id = "a1-ex-alpha-02",
            type = InteractiveExerciseType.MultipleChoice,
            promptFr = "Quel est le nom russe de la lettre Б ?",
            stimulus = "Б б",
            choices = listOf("бэ", "вэ", "гэ", "дэ"),
            acceptedAnswers = listOf("бэ", "Бе", "бэ."),
            explanationFr = "Б s'appelle « бэ ». Attention à ne pas la confondre avec le B latin.",
            sortOrder = 2,
        ),
        InteractiveExercise(
            id = "a1-ex-alpha-03",
            type = InteractiveExerciseType.MatchTranslation,
            promptFr = "Associez : quelle lettre ressemble au « P » latin mais se lit autrement ?",
            stimulus = "Р",
            choices = listOf("эр (r roulé)", "пэ", "эс", "эн"),
            acceptedAnswers = listOf("эр (r roulé)", "эр"),
            explanationFr = "Р se lit comme un r (souvent roulé), pas comme le P français.",
            sortOrder = 3,
        ),
    )

    private val greetingsExercises = listOf(
        InteractiveExercise(
            id = "a1-ex-greet-01",
            type = InteractiveExerciseType.MatchTranslation,
            promptFr = "Que signifie « привет » ?",
            stimulus = "привет",
            choices = listOf(
                "salut (informel)",
                "au revoir poli",
                "merci",
                "s'il vous plaît",
            ),
            acceptedAnswers = listOf("salut (informel)", "salut", "Salut"),
            explanationFr = "привет est un salut informel (tutoiement), pas un au revoir.",
            sortOrder = 1,
        ),
        InteractiveExercise(
            id = "a1-ex-greet-02",
            type = InteractiveExerciseType.MultipleChoice,
            promptFr = "Quelle formule convient pour saluer poliment (vouvoiement) ?",
            stimulus = "…",
            choices = listOf("здравствуйте", "пока", "привет", "спокойной ночи"),
            acceptedAnswers = listOf("здравствуйте", "Здравствуйте"),
            explanationFr = "здравствуйте est la salutation polie standard.",
            sortOrder = 2,
        ),
        InteractiveExercise(
            id = "a1-ex-greet-03",
            type = InteractiveExerciseType.FillBlank,
            promptFr = "Complétez : pour dire merci, on dit « _____ ».",
            stimulus = "_____",
            choices = listOf("спасибо", "пожалуйста", "извините", "хорошо"),
            acceptedAnswers = listOf("спасибо", "Спасибо"),
            explanationFr = "спасибо = merci. пожалуйста sert aussi de « de rien / s'il vous plaît ».",
            sortOrder = 3,
        ),
    )

    private val introduceExercises = listOf(
        InteractiveExercise(
            id = "a1-ex-intro-01",
            type = InteractiveExerciseType.MatchTranslation,
            promptFr = "Que signifie « Меня зовут Анна » ?",
            stimulus = "Меня зовут Анна.",
            choices = listOf(
                "Je m'appelle Anna",
                "Comment vous appelez-vous ?",
                "Enchanté",
                "Bonsoir Anna",
            ),
            acceptedAnswers = listOf("Je m'appelle Anna", "je m'appelle anna"),
            explanationFr = "Меня зовут… introduit le prénom.",
            sortOrder = 1,
        ),
        InteractiveExercise(
            id = "a1-ex-intro-02",
            type = InteractiveExerciseType.FillBlank,
            promptFr = "Écrivez ou choisissez la formule « je m'appelle » en russe.",
            stimulus = "…",
            choices = listOf("Меня зовут", "Как вас зовут", "Очень приятно", "Добрый день"),
            acceptedAnswers = listOf(
                "Меня зовут",
                "меня зовут",
                "Меня зовут.",
            ),
            explanationFr = "La tournure de base est « Меня зовут… » suivie du prénom.",
            sortOrder = 2,
        ),
        InteractiveExercise(
            id = "a1-ex-intro-03",
            type = InteractiveExerciseType.WordOrder,
            promptFr = "Remettez la question dans l'ordre : « Comment vous appelez-vous ? »",
            stimulus = "Как вас зовут?",
            choices = listOf("Как", "вас", "зовут?"),
            acceptedAnswers = listOf(
                "Как вас зовут?",
                "Как вас зовут",
                "как вас зовут",
            ),
            explanationFr = "Как вас зовут? est la question polie pour demander le nom.",
            sortOrder = 3,
        ),
    )
}
