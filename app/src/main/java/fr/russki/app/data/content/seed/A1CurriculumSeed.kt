package fr.russki.app.data.content.seed

import fr.russki.app.domain.curriculum.CefrLevel
import fr.russki.app.domain.curriculum.CourseUnit
import fr.russki.app.domain.curriculum.LessonOutline
import fr.russki.app.domain.exercise.InteractiveExercise
import fr.russki.app.domain.exercise.InteractiveExerciseType
import fr.russki.app.domain.exercise.InteractiveExerciseType as T

/**
 * Parcours A1 débutant enrichi — hors ligne.
 * Unité 1 = [A1Unit1Seed] (aucun doublon d'exercices).
 * Couverture progressive : **pas** un A1 CECRL exhaustif ni certifiant.
 */
object A1CurriculumSeed {

    val units: List<CourseUnit> by lazy {
        listOf(
            A1Unit1Seed.unit,
            unitPronouns,
            unitNumbers,
            unitCountries,
            unitFamily,
            unitFood,
            unitCity,
            unitShopping,
            unitReview,
        )
    }

    fun exercisesFor(lessonId: String): List<InteractiveExercise> {
        val fromUnit1 = A1Unit1Seed.exercisesFor(lessonId)
        if (fromUnit1.isNotEmpty()) return fromUnit1
        return extras[lessonId].orEmpty()
    }

    fun allLessonIds(): List<String> = units.flatMap { u -> u.lessons.map { it.id } }

    // region Unités

    private const val U2 = "a1-unit-pronoms"
    const val LESSON_PRONOUNS = "a1-lesson-pronouns"
    const val LESSON_SIMPLE = "a1-lesson-simple-sentences"

    private val unitPronouns = courseUnit(
        U2, "Pronoms et phrases simples",
        "Je, tu, il/elle et premières phrases avec « это » / « есть ».", 2,
        listOf(
            les(
                LESSON_PRONOUNS, U2, 1, "Pronoms personnels",
                "Reconnaître я, ты, он, она, мы, вы.",
                "вы sert aussi de vouvoiement poli (comme « vous »).",
                "Vous distinguez les pronoms de base et le vouvoiement.",
            ),
            les(
                LESSON_SIMPLE, U2, 2, "Phrases avec это et есть",
                "Dire « c'est… » et « il y a… ».",
                "Это + nom présente. Есть indique l'existence (« il y a »).",
                "Vous savez former Это книга et У меня есть…",
            ),
        ),
    )

    private const val U3 = "a1-unit-nombres"
    const val LESSON_NUMBERS = "a1-lesson-numbers"
    const val LESSON_AGE = "a1-lesson-age"

    private val unitNumbers = courseUnit(
        U3, "Nombres et âge", "Compter de 1 à 10 et dire son âge.", 3,
        listOf(
            les(
                LESSON_NUMBERS, U3, 1, "Nombres de 1 à 10",
                "Lire et reconnaître один… десять.",
                "Forme de citation один (masculin) pour commencer.",
                "Vous reconnaissez les chiffres 1–10 en russe.",
            ),
            les(
                LESSON_AGE, U3, 2, "Dire son âge",
                "Utiliser Мне … лет.",
                "Мне 20 лет (datif). Après 5–20 : généralement лет.",
                "Vous comprenez la structure Мне … лет.",
            ),
        ),
    )

    private const val U4 = "a1-unit-pays"
    const val LESSON_COUNTRIES = "a1-lesson-countries"
    const val LESSON_LANGUAGES = "a1-lesson-languages"

    private val unitCountries = courseUnit(
        U4, "Pays et langues", "Dire d'où l'on vient et quelle langue on parle.", 4,
        listOf(
            les(
                LESSON_COUNTRIES, U4, 1, "Pays et origines",
                "Utiliser Я из…",
                "Из + génitif pour l'origine (Я из Франции).",
                "Vous savez dire votre pays d'origine.",
            ),
            les(
                LESSON_LANGUAGES, U4, 2, "Langues",
                "Dire Я говорю по-русски / по-французски.",
                "По-…ски forme l'adverbe de langue.",
                "Vous exprimez les langues que vous parlez (ou non).",
            ),
        ),
    )

    private const val U5 = "a1-unit-famille"
    const val LESSON_FAMILY = "a1-lesson-family"
    const val LESSON_DESCRIBE = "a1-lesson-describe-person"

    private val unitFamily = courseUnit(
        U5, "Famille et personnes", "Membres de la famille et descriptions simples.", 5,
        listOf(
            les(
                LESSON_FAMILY, U5, 1, "La famille",
                "Nommer мама, папа, брат, сестра, друг.",
                "У меня есть брат = j'ai un frère.",
                "Vous reconnaissez les mots de famille essentiels.",
            ),
            les(
                LESSON_DESCRIBE, U5, 2, "Décrire une personne",
                "Utiliser он / она + adjectif simple.",
                "Les adjectifs s'accordent (добрый / добрая).",
                "Vous comprenez une description courte.",
            ),
        ),
    )

    private const val U6 = "a1-unit-nourriture"
    const val LESSON_FOOD = "a1-lesson-food"
    const val LESSON_ORDER = "a1-lesson-cafe-order"

    private val unitFood = courseUnit(
        U6, "Nourriture et café",
        "Boissons, plats et commande polie (complète le dialogue café).", 6,
        listOf(
            les(
                LESSON_FOOD, U6, 1, "Boissons et plats",
                "Reconnaître чай, кофе, вода, хлеб, суп.",
                "Vocabulaire du café et du repas simple.",
                "Vous connaissez cinq mots alimentaires de base.",
            ),
            les(
                LESSON_ORDER, U6, 2, "Commander poliment",
                "Мне … пожалуйста et Счёт, пожалуйста.",
                "Enrichit le dialogue café avec la demande d'addition.",
                "Vous savez commander et demander l'addition.",
            ),
        ),
    )

    private const val U7 = "a1-unit-ville"
    const val LESSON_CITY = "a1-lesson-city"
    const val LESSON_TRANSPORT = "a1-lesson-transport"

    private val unitCity = courseUnit(
        U7, "Ville et transports", "Lieux, directions et moyens de transport.", 7,
        listOf(
            les(
                LESSON_CITY, U7, 1, "En ville",
                "Reconnaître улица, площадь, магазин, музей.",
                "Complète le dialogue « Demander son chemin ».",
                "Vous identifiez des lieux urbains courants.",
            ),
            les(
                LESSON_TRANSPORT, U7, 2, "Transports",
                "Nommer метро, автобус, такси ; dire на метро.",
                "На + moyen de transport (на метро).",
                "Vous savez parler d'un trajet simple.",
            ),
        ),
    )

    private const val U8 = "a1-unit-achats"
    const val LESSON_SHOPPING = "a1-lesson-shopping"
    const val LESSON_PRICES = "a1-lesson-prices"

    private val unitShopping = courseUnit(
        U8, "Achats et prix", "Situations au magasin : acheter et lire un prix.", 8,
        listOf(
            les(
                LESSON_SHOPPING, U8, 1, "Au magasin",
                "Сколько это стоит? et Я возьму это.",
                "Formules d'achat de base.",
                "Vous savez demander un prix et indiquer votre choix.",
            ),
            les(
                LESSON_PRICES, U8, 2, "Comprendre un prix",
                "Lire рубль / рубля / рублей dans des exemples.",
                "Les formes changent selon le nombre (2 рубля, 5 рублей).",
                "Vous reconnaissez un prix en roubles.",
            ),
        ),
    )

    private const val U9 = "a1-unit-revisions"
    const val LESSON_REVIEW_VOCAB = "a1-lesson-review-vocab"
    const val LESSON_REVIEW_PHRASES = "a1-lesson-review-phrases"

    private val unitReview = courseUnit(
        U9, "Révisions débutant",
        "Synthèse des unités 1–8 — pas un examen de certification.", 9,
        listOf(
            les(
                LESSON_REVIEW_VOCAB, U9, 1, "Révision vocabulaire",
                "Revoir nombres, famille et lieux déjà vus.",
                "Mélange de thèmes pour consolider.",
                "Vous réactivez le vocabulaire précédent.",
            ),
            les(
                LESSON_REVIEW_PHRASES, U9, 2, "Révision phrases utiles",
                "Traduire et ordonner des phrases du quotidien.",
                "Saluer, se présenter, commander, demander son chemin.",
                "Vous enchaînez des phrases déjà rencontrées.",
            ),
        ),
    )

    // endregion

    private val extras: Map<String, List<InteractiveExercise>> by lazy {
        mapOf(
            LESSON_PRONOUNS to listOf(
                e("a1-ex-pron-01", T.MatchTranslation, 1, "Que signifie « я » ?", "я",
                    listOf("je", "tu", "il", "nous"), listOf("je", "Je"),
                    "я = je."),
                e("a1-ex-pron-02", T.MultipleChoice, 2, "Quel pronom pour vouvoyer ?", "…",
                    listOf("вы", "ты", "он", "мы"), listOf("вы", "Вы"),
                    "вы = vous (vouvoiement ou pluriel)."),
                e("a1-ex-pron-03", T.FillBlank, 3, "Complétez : « _____ читает » (elle lit).",
                    "_____ читает", listOf("Она", "Он", "Я", "Мы"), listOf("Она", "она"),
                    "она = elle."),
            ),
            LESSON_SIMPLE to listOf(
                e("a1-ex-simp-01", T.MatchTranslation, 1, "Que signifie « Это книга » ?",
                    "Это книга.",
                    listOf("C'est un livre", "J'ai un livre", "Où est le livre ?", "Lis le livre"),
                    listOf("C'est un livre", "c'est un livre"),
                    "Это + nom = c'est…"),
                e("a1-ex-simp-02", T.FillBlank, 2, "Complétez : « У меня _____ брат ».",
                    "У меня _____ брат", listOf("есть", "это", "из", "на"), listOf("есть", "Есть"),
                    "У меня есть… = j'ai…"),
                e("a1-ex-simp-03", T.WordOrder, 3, "Ordre : « C'est un café ».", "Это кафе.",
                    listOf("Это", "кафе"), listOf("Это кафе", "Это кафе.", "это кафе"),
                    "Ordre courant : Это + nom."),
            ),
            LESSON_NUMBERS to listOf(
                e("a1-ex-num-01", T.MatchTranslation, 1, "Que signifie « три » ?", "три",
                    listOf("trois", "deux", "quatre", "cinq"), listOf("trois", "Trois"),
                    "три = 3."),
                e("a1-ex-num-02", T.MultipleChoice, 2, "Quel mot signifie « cinq » ?", "5",
                    listOf("пять", "шесть", "семь", "восемь"), listOf("пять", "Пять"),
                    "пять = 5."),
                e("a1-ex-num-03", T.FillBlank, 3, "Suite : один, два, _____",
                    "один, два, _____", listOf("три", "четыре", "пять", "ноль"),
                    listOf("три", "Три"), "Après два vient три."),
            ),
            LESSON_AGE to listOf(
                e("a1-ex-age-01", T.MatchTranslation, 1, "Que signifie « Мне 20 лет » ?",
                    "Мне 20 лет.",
                    listOf("J'ai 20 ans", "Il a 20 ans", "Dans 20 ans", "20 roubles"),
                    listOf("J'ai 20 ans", "j'ai 20 ans"),
                    "Мне … лет = j'ai … ans."),
                e("a1-ex-age-02", T.FillBlank, 2, "Complétez : « _____ 30 лет ».",
                    "_____ 30 лет", listOf("Мне", "Я", "Меня", "Мной"), listOf("Мне", "мне"),
                    "C'est Мне (datif), pas Я."),
                e("a1-ex-age-03", T.MultipleChoice, 3, "Après « Мне 5 … », forme correcte ?",
                    "Мне 5 …", listOf("лет", "года", "год", "летов"), listOf("лет", "Лет"),
                    "Après 5–20 : généralement лет."),
            ),
            LESSON_COUNTRIES to listOf(
                e("a1-ex-cty-01", T.MatchTranslation, 1, "Que signifie « Я из России » ?",
                    "Я из России.",
                    listOf("Je suis de Russie", "Je parle russe", "Je vais en Russie", "La Russie"),
                    listOf("Je suis de Russie", "je suis de Russie"),
                    "Я из… = je suis de…"),
                e("a1-ex-cty-02", T.FillBlank, 2, "Complétez : « Я _____ Франции ».",
                    "Я _____ Франции", listOf("из", "в", "на", "у"), listOf("из", "Из"),
                    "из + pays pour l'origine."),
                e("a1-ex-cty-03", T.MultipleChoice, 3, "« Германия » désigne :", "Германия",
                    listOf("Allemagne", "Espagne", "Italie", "Pologne"),
                    listOf("Allemagne", "allemagne"), "Германия = Allemagne."),
            ),
            LESSON_LANGUAGES to listOf(
                e("a1-ex-lng-01", T.MatchTranslation, 1, "Que signifie « Я говорю по-русски » ?",
                    "Я говорю по-русски.",
                    listOf(
                        "Je parle russe", "Je suis russe",
                        "J'apprends le russe", "Je comprends le russe",
                    ),
                    listOf("Je parle russe", "je parle russe"),
                    "говорить по-… = parler une langue."),
                e("a1-ex-lng-02", T.FillBlank, 2, "Complétez : « Я говорю _____ » (français).",
                    "Я говорю _____",
                    listOf("по-французски", "французский", "Франция", "из Франции"),
                    listOf("по-французски", "По-французски"),
                    "Adverbe : по-французски."),
                e("a1-ex-lng-03", T.MultipleChoice, 3, "« Je ne parle pas anglais » ?",
                    "…",
                    listOf(
                        "Я не говорю по-английски", "Я из Англии",
                        "Я говорю по-английски", "Это английский",
                    ),
                    listOf("Я не говорю по-английски", "я не говорю по-английски"),
                    "Не devant le verbe : не говорю."),
            ),
            LESSON_FAMILY to listOf(
                e("a1-ex-fam-01", T.MatchTranslation, 1, "Que signifie « мама » ?", "мама",
                    listOf("maman", "papa", "sœur", "amie"), listOf("maman", "Maman", "mère"),
                    "мама = maman."),
                e("a1-ex-fam-02", T.MultipleChoice, 2, "Quel mot signifie « frère » ?", "…",
                    listOf("брат", "сестра", "папа", "друг"), listOf("брат", "Брат"),
                    "брат = frère."),
                e("a1-ex-fam-03", T.FillBlank, 3, "Complétez : « У меня есть _____ » (sœur).",
                    "У меня есть _____", listOf("сестра", "брат", "мама", "папа"),
                    listOf("сестра", "Сестра"), "сестра = sœur."),
            ),
            LESSON_DESCRIBE to listOf(
                e("a1-ex-dsc-01", T.MatchTranslation, 1, "Que signifie « Он высокий » ?",
                    "Он высокий.",
                    listOf("Il est grand", "Elle est grande", "Il est gentil", "Il est jeune"),
                    listOf("Il est grand", "il est grand"),
                    "высокий = grand (taille)."),
                e("a1-ex-dsc-02", T.FillBlank, 2, "Complétez : « Она _____ » (gentille).",
                    "Она _____", listOf("добрая", "добрый", "высокий", "новый"),
                    listOf("добрая", "Добрая"), "Forme féminine добрая."),
                e("a1-ex-dsc-03", T.MultipleChoice, 3, "« Друг » signifie :", "друг",
                    listOf("ami", "frère", "père", "voisin"), listOf("ami", "Ami"),
                    "друг = ami."),
            ),
            LESSON_FOOD to listOf(
                e("a1-ex-food-01", T.MatchTranslation, 1, "Que signifie « вода » ?", "вода",
                    listOf("eau", "thé", "pain", "soupe"), listOf("eau", "Eau"),
                    "вода = eau."),
                e("a1-ex-food-02", T.MultipleChoice, 2, "Quel mot signifie « pain » ?", "…",
                    listOf("хлеб", "суп", "чай", "кофе"), listOf("хлеб", "Хлеб"),
                    "хлеб = pain."),
                e("a1-ex-food-03", T.FillBlank, 3, "Complétez : « Мне _____, пожалуйста » (café).",
                    "Мне _____, пожалуйста", listOf("кофе", "хлеб", "суп", "воду"),
                    listOf("кофе", "Кофе"), "кофе : forme courante invariable."),
            ),
            LESSON_ORDER to listOf(
                e("a1-ex-ord-01", T.MatchTranslation, 1, "Que signifie « Счёт, пожалуйста » ?",
                    "Счёт, пожалуйста.",
                    listOf(
                        "L'addition, s'il vous plaît", "Un thé, s'il vous plaît",
                        "Au revoir", "Excusez-moi",
                    ),
                    listOf("L'addition, s'il vous plaît", "l'addition, s'il vous plaît"),
                    "счёт = l'addition."),
                e("a1-ex-ord-02", T.FillBlank, 2, "Complétez : « Мне один _____, пожалуйста » (thé).",
                    "Мне один _____, пожалуйста",
                    listOf("чай", "счёт", "хлеб", "друг"), listOf("чай", "Чай"),
                    "чай = thé."),
                e("a1-ex-ord-03", T.WordOrder, 3, "Ordre : « Un café, s'il vous plaît ».",
                    "Мне кофе, пожалуйста.",
                    listOf("Мне", "кофе,", "пожалуйста."),
                    listOf(
                        "Мне кофе, пожалуйста.",
                        "Мне кофе, пожалуйста",
                        "мне кофе, пожалуйста",
                    ),
                    "Мне + boisson + пожалуйста."),
            ),
            LESSON_CITY to listOf(
                e("a1-ex-city-01", T.MatchTranslation, 1, "Que signifie « улица » ?", "улица",
                    listOf("rue", "place", "magasin", "musée"), listOf("rue", "Rue"),
                    "улица = rue."),
                e("a1-ex-city-02", T.MultipleChoice, 2, "« Магазин » désigne :", "магазин",
                    listOf("un magasin", "un musée", "une place", "un métro"),
                    listOf("un magasin", "magasin"), "магазин = magasin."),
                e("a1-ex-city-03", T.FillBlank, 3, "Complétez : « Где _____ ? » (le musée).",
                    "Где _____?", listOf("музей", "хлеб", "мама", "чай"),
                    listOf("музей", "Музей"), "музей = musée."),
            ),
            LESSON_TRANSPORT to listOf(
                e("a1-ex-tr-01", T.MatchTranslation, 1, "Que signifie « метро » ?", "метро",
                    listOf("métro", "bus", "taxi", "gare"), listOf("métro", "Métro"),
                    "метро = métro."),
                e("a1-ex-tr-02", T.FillBlank, 2, "Complétez : « Я еду _____ метро ».",
                    "Я еду _____ метро", listOf("на", "в", "из", "у"), listOf("на", "На"),
                    "на метро = en métro."),
                e("a1-ex-tr-03", T.MultipleChoice, 3, "« Автобус » signifie :", "автобус",
                    listOf("bus", "taxi", "tram", "train"), listOf("bus", "Bus"),
                    "автобус = bus."),
            ),
            LESSON_SHOPPING to listOf(
                e("a1-ex-shop-01", T.MatchTranslation, 1, "Que signifie « Сколько это стоит? » ?",
                    "Сколько это стоит?",
                    listOf(
                        "Combien ça coûte ?", "Où est la caisse ?",
                        "Je prends ceci", "C'est cher",
                    ),
                    listOf("Combien ça coûte ?", "combien ça coûte"),
                    "Сколько это стоит? = combien ça coûte ?"),
                e("a1-ex-shop-02", T.FillBlank, 2, "Complétez : « Я _____ это » (je prends ceci).",
                    "Я _____ это", listOf("возьму", "есть", "говорю", "иду"),
                    listOf("возьму", "Возьму"), "Я возьму это = je prends ceci."),
                e("a1-ex-shop-03", T.MultipleChoice, 3, "Formule polie pour remercier au magasin ?",
                    "…", listOf("Спасибо", "Пока", "Метро", "Брат"),
                    listOf("Спасибо", "спасибо"), "Спасибо reste approprié."),
            ),
            LESSON_PRICES to listOf(
                e("a1-ex-price-01", T.MatchTranslation, 1, "Que signifie « рубль » ?", "рубль",
                    listOf("rouble", "euro", "dollar", "centime"), listOf("rouble", "Rouble"),
                    "рубль = rouble (monnaie)."),
                e("a1-ex-price-02", T.FillBlank, 2, "Complétez : « Это стоит 5 _____ ».",
                    "Это стоит 5 _____", listOf("рублей", "рубль", "рубля", "лет"),
                    listOf("рублей", "Рублей"), "Après 5 : рублей."),
                e("a1-ex-price-03", T.MultipleChoice, 3, "Après « 2 », forme correcte ?",
                    "2 …", listOf("рубля", "рублей", "рубль", "лет"),
                    listOf("рубля", "Рубля"), "2 рубля (forme fréquente)."),
            ),
            LESSON_REVIEW_VOCAB to listOf(
                e("a1-ex-rv-01", T.MatchTranslation, 1, "« Пять » = ?", "пять",
                    listOf("cinq", "frère", "rue", "thé"), listOf("cinq", "Cinq"),
                    "Révision : пять = 5."),
                e("a1-ex-rv-02", T.MultipleChoice, 2, "« Сестра » = ?", "сестра",
                    listOf("sœur", "frère", "ami", "père"), listOf("sœur", "Sœur"),
                    "Révision : сестра = sœur."),
                e("a1-ex-rv-03", T.LetterRecognition, 3,
                    "Quelle lettre majuscule pour un son proche de « m » ?",
                    "М", listOf("М", "Н", "П", "Р"), listOf("М", "м"),
                    "М / м — soutien alphabet (pas une translittération)."),
            ),
            LESSON_REVIEW_PHRASES to listOf(
                e("a1-ex-rp-01", T.MatchTranslation, 1, "Que signifie « Здравствуйте » ?",
                    "Здравствуйте!",
                    listOf(
                        "Bonjour (vouvoiement)", "Salut (tutoiement)",
                        "Merci", "Au revoir",
                    ),
                    listOf("Bonjour (vouvoiement)", "Bonjour"),
                    "Révision salutations."),
                e("a1-ex-rp-02", T.WordOrder, 2, "Ordre : « Comment vous appelez-vous ? »",
                    "Как вас зовут?",
                    listOf("Как", "вас", "зовут?"),
                    listOf("Как вас зовут?", "Как вас зовут", "как вас зовут"),
                    "Révision présentation."),
                e("a1-ex-rp-03", T.FillBlank, 3, "Complétez : « Извините, где _____? » (métro).",
                    "Извините, где _____?",
                    listOf("метро", "мама", "хлеб", "друг"), listOf("метро", "Метро"),
                    "Révision : demander son chemin."),
            ),
        )
    }

    private fun courseUnit(
        id: String,
        title: String,
        description: String,
        order: Int,
        lessons: List<LessonOutline>,
    ) = CourseUnit(
        id = id,
        cefrLevel = CefrLevel.A1,
        titleFr = title,
        descriptionFr = description,
        sortOrder = order,
        lessons = lessons,
    )

    private fun les(
        id: String,
        unitId: String,
        order: Int,
        title: String,
        objective: String,
        intro: String,
        summary: String,
    ) = LessonOutline(
        id = id,
        unitId = unitId,
        titleFr = title,
        objectiveFr = objective,
        introductionFr = intro,
        summaryFr = summary,
        sortOrder = order,
        exerciseCount = 3,
        legacyRouteHint = null,
    )

    private fun e(
        id: String,
        type: InteractiveExerciseType,
        order: Int,
        prompt: String,
        stimulus: String,
        choices: List<String>,
        accepted: List<String>,
        explanation: String,
    ) = InteractiveExercise(
        id = id,
        type = type,
        promptFr = prompt,
        stimulus = stimulus,
        choices = choices,
        acceptedAnswers = accepted,
        explanationFr = explanation,
        sortOrder = order,
    )
}
