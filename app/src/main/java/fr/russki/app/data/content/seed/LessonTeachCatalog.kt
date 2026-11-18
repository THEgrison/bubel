package fr.russki.app.data.content.seed

import fr.russki.app.domain.lesson.SpeechRegister
import fr.russki.app.domain.lesson.TeachableItem
import fr.russki.app.domain.pronunciation.RussianPronunciation

/**
 * Contenu de découverte par leçon — dérivé des seeds existants (pas de Room).
 * Chaque entrée est présentée **avant** les exercices correspondants.
 */
object LessonTeachCatalog {

    fun forLesson(lessonId: String): List<TeachableItem> = when (lessonId) {
        A1Unit1Seed.LESSON_GREETINGS -> greetings
        A1Unit1Seed.LESSON_INTRODUCE -> introduce
        A1Unit1Seed.LESSON_ALPHABET -> alphabetSample
        A1CurriculumSeed.LESSON_PRONOUNS -> pronouns
        A1CurriculumSeed.LESSON_SIMPLE -> simpleSentences
        A1CurriculumSeed.LESSON_NUMBERS -> numbers
        A1CurriculumSeed.LESSON_AGE -> age
        A1CurriculumSeed.LESSON_COUNTRIES -> countries
        A1CurriculumSeed.LESSON_LANGUAGES -> languages
        A1CurriculumSeed.LESSON_FAMILY -> family
        A1CurriculumSeed.LESSON_DESCRIBE -> describe
        A1CurriculumSeed.LESSON_FOOD -> food
        A1CurriculumSeed.LESSON_ORDER -> order
        A1CurriculumSeed.LESSON_CITY -> city
        A1CurriculumSeed.LESSON_TRANSPORT -> transport
        A1CurriculumSeed.LESSON_SHOPPING -> shopping
        A1CurriculumSeed.LESSON_PRICES -> prices
        A1CurriculumSeed.LESSON_REVIEW_VOCAB -> reviewVocab
        A1CurriculumSeed.LESSON_REVIEW_PHRASES -> reviewPhrases
        else -> emptyList()
    }

    /** Tous les ids de leçons A1 couverts par ce catalogue. */
    fun coveredLessonIds(): Set<String> = setOf(
        A1Unit1Seed.LESSON_ALPHABET,
        A1Unit1Seed.LESSON_GREETINGS,
        A1Unit1Seed.LESSON_INTRODUCE,
        A1CurriculumSeed.LESSON_PRONOUNS,
        A1CurriculumSeed.LESSON_SIMPLE,
        A1CurriculumSeed.LESSON_NUMBERS,
        A1CurriculumSeed.LESSON_AGE,
        A1CurriculumSeed.LESSON_COUNTRIES,
        A1CurriculumSeed.LESSON_LANGUAGES,
        A1CurriculumSeed.LESSON_FAMILY,
        A1CurriculumSeed.LESSON_DESCRIBE,
        A1CurriculumSeed.LESSON_FOOD,
        A1CurriculumSeed.LESSON_ORDER,
        A1CurriculumSeed.LESSON_CITY,
        A1CurriculumSeed.LESSON_TRANSPORT,
        A1CurriculumSeed.LESSON_SHOPPING,
        A1CurriculumSeed.LESSON_PRICES,
        A1CurriculumSeed.LESSON_REVIEW_VOCAB,
        A1CurriculumSeed.LESSON_REVIEW_PHRASES,
    )

    private fun item(
        id: String,
        ru: String,
        fr: String,
        usage: String? = null,
        exampleRu: String? = null,
        exampleFr: String? = null,
        audio: String? = null,
        register: SpeechRegister = SpeechRegister.Neutral,
        confusion: String? = null,
        grammar: String? = null,
    ): TeachableItem {
        val pron = RussianPronunciation.lookupOrTransliterate(ru)
        return TeachableItem(
            id = id,
            textRu = ru,
            translationFr = fr,
            transliteration = pron.transliteration,
            stressUnknown = pron.stressUnknown,
            usageFr = usage ?: pron.noteFr,
            exampleRu = exampleRu,
            exampleFr = exampleFr,
            audioAssetPath = audio,
            register = register,
            confusionFr = confusion,
            grammarNoteFr = grammar,
        )
    }

    private val greetings = listOf(
        item(
            "teach-privet", "привет", "salut (informel)",
            usage = "Entre amis / proches (tutoiement).",
            exampleRu = "Привет! Как дела?", exampleFr = "Salut ! Ça va ?",
            audio = "content/audio/ru/privet.ogg", register = SpeechRegister.Informal,
            confusion = "Ne remplace pas здравствуйте en situation polie.",
        ),
        item(
            "teach-zdravstvuyte", "здравствуйте", "bonjour (vouvoiement, poli)",
            usage = "Salutation formelle standard.",
            exampleRu = "Здравствуйте!", exampleFr = "Bonjour !",
            audio = "content/audio/ru/zdravstvuyte.ogg", register = SpeechRegister.Formal,
            confusion = "Plus formel que привет — pas interchangeables.",
        ),
        item(
            "teach-dobroe-utro", "доброе утро", "bonjour (le matin)",
            exampleRu = "Доброе утро!", exampleFr = "Bonjour ! (le matin)",
            audio = "content/audio/ru/dobroe-utro.ogg",
            grammar = "доброе s’accorde avec утро (neutre).",
        ),
        item(
            "teach-dobryy-vecher", "добрый вечер", "bonsoir",
            exampleRu = "Добрый вечер!", exampleFr = "Bonsoir !",
            audio = "content/audio/ru/dobryy-vecher.ogg",
        ),
        item(
            "teach-spasibo", "спасибо", "merci",
            exampleRu = "Спасибо большое!", exampleFr = "Merci beaucoup !",
            audio = "content/audio/ru/spasibo.ogg",
        ),
        item(
            "teach-pozhaluysta", "пожалуйста", "s'il vous plaît / de rien",
            usage = "Demande polie, ou réponse à « спасибо ».",
            audio = "content/audio/ru/pozhaluysta.ogg",
            confusion = "Un seul mot pour plusieurs sens français selon le contexte.",
        ),
        item(
            "teach-do-svidaniya", "до свидания", "au revoir",
            usage = "Au revoir poli.",
            exampleRu = "До свидания!", exampleFr = "Au revoir !",
            audio = "content/audio/ru/do-svidaniya.ogg", register = SpeechRegister.Formal,
            confusion = "Пока = au revoir informel.",
        ),
        item(
            "teach-poka", "пока", "à plus (informel)",
            usage = "Au revoir entre proches.",
            audio = "content/audio/ru/poka.ogg", register = SpeechRegister.Informal,
        ),
        item(
            "teach-khorosho", "хорошо", "bien / d'accord",
            exampleRu = "Хорошо, спасибо.", exampleFr = "Bien, merci.",
            audio = "content/audio/ru/khorosho.ogg",
        ),
    )

    private val introduce = listOf(
        item(
            "teach-menya-zovut", "меня зовут", "je m'appelle",
            exampleRu = "Меня зовут Анна.", exampleFr = "Je m'appelle Anna.",
            grammar = "Construction fixe : Меня зовут + prénom (pas Я зовут).",
            confusion = "Ne pas dire « Я зовут… ».",
        ),
        item(
            "teach-kak-vas-zovut", "как вас зовут?", "comment vous appelez-vous ?",
            usage = "Question polie (вас = vouvoiement).",
            confusion = "Как тебя зовут? = tutoiement.",
        ),
        item(
            "teach-ochen-priyatno", "очень приятно", "enchanté(e)",
            usage = "Après une présentation.",
            exampleRu = "Очень приятно!", exampleFr = "Enchanté(e) !",
        ),
    )

    private val alphabetSample = listOf(
        item(
            "teach-letter-a", "А а", "lettre « а »",
            usage = "Voyelle proche du a français (approximation).",
            confusion = "Ne pas confondre avec le A latin hors contexte cyrillique.",
        ),
        item(
            "teach-letter-b", "Б б", "lettre « бэ »",
            usage = "Son proche de b ; nom russe бэ.",
            confusion = "Le B latin ressemble à В russe (вэ), pas à Б.",
        ),
        item(
            "teach-letter-m", "М м", "lettre « эм » (m)",
            usage = "Son proche de m français.",
            confusion = "Ne pas confondre avec Н (эн).",
        ),
        item(
            "teach-letter-r", "Р р", "lettre « эр » (r)",
            usage = "Vibrante ; approximation d’un r roulé.",
            confusion = "Ressemble au P latin mais se lit autrement.",
        ),
    )

    private val pronouns = listOf(
        item("teach-ya", "я", "je", exampleRu = "Я читаю.", exampleFr = "Je lis."),
        item("teach-ty", "ты", "tu", usage = "Tutoiement."),
        item("teach-on", "он", "il"),
        item(
            "teach-ona", "она", "elle",
            exampleRu = "Она читает.", exampleFr = "Elle lit.",
            grammar = "Le verbe s’accorde : она читает.",
        ),
        item("teach-my", "мы", "nous"),
        item(
            "teach-vy", "вы", "vous",
            usage = "Pluriel ou vouvoiement poli.",
            confusion = "Comme le français « vous » : politesse ou plusieurs personnes.",
        ),
    )

    private val simpleSentences = listOf(
        item(
            "teach-eto", "это", "c'est / ceci",
            exampleRu = "Это книга.", exampleFr = "C'est un livre.",
            grammar = "Это + nom (pas de verbe « être » au présent).",
        ),
        item(
            "teach-est", "есть", "il y a / (j’)ai",
            exampleRu = "У меня есть брат.", exampleFr = "J'ai un frère.",
            usage = "У меня есть… = j’ai…",
            confusion = "Homographe avec « manger » — ici : existence / possession.",
        ),
        item("teach-kniga", "книга", "livre"),
        item("teach-kafe", "кафе", "café (lieu)", exampleRu = "Это кафе.", exampleFr = "C'est un café."),
    )

    private val numbers = listOf(
        item("teach-odin", "один", "un (masc. citation)", grammar = "Forme de base pour commencer ; s’accorde plus tard."),
        item("teach-dva", "два", "deux"),
        item("teach-tri", "три", "trois"),
        item("teach-chetyre", "четыре", "quatre"),
        item("teach-pyat", "пять", "cinq"),
    )

    private val age = listOf(
        item(
            "teach-mne", "мне", "à moi (datif)",
            usage = "Мне … лет / года / год = j’ai … ans.",
            exampleRu = "Мне 20 лет.", exampleFr = "J'ai 20 ans.",
            confusion = "Ce n’est pas Я 20 лет.",
            grammar = "Datif мне, pas nominatif я.",
        ),
        item(
            "teach-god", "год", "an (après 1)",
            usage = "Мне 1 год.",
            grammar = "1 → год ; 2–4 → года ; 5–20 → лет.",
        ),
        item(
            "teach-goda", "года", "ans (après 2–4)",
            usage = "Мне 2 года / 3 года / 4 года.",
        ),
        item(
            "teach-let", "лет", "ans (après 5–20)",
            usage = "Мне 5 лет ; Мне 20 лет.",
            exampleRu = "Мне 20 лет.", exampleFr = "J'ai 20 ans.",
        ),
    )

    private val countries = listOf(
        item(
            "teach-ya-iz", "я из…", "je suis de…",
            exampleRu = "Я из России.", exampleFr = "Je suis de Russie.",
            grammar = "из + génitif (России, Франции).",
        ),
        item("teach-rossii", "россии", "Russie (génitif)", usage = "Я из России."),
        item("teach-frantsii", "франции", "France (génitif)", usage = "Я из Франции."),
        item("teach-germaniya", "германия", "Allemagne", usage = "Nom du pays (nominatif)."),
    )

    private val languages = listOf(
        item(
            "teach-po-russki", "по-русски", "en russe",
            exampleRu = "Я говорю по-русски.", exampleFr = "Je parle russe.",
            grammar = "Adverbe по-…ски (pas l’adjectif русский seul ici).",
        ),
        item("teach-po-frantsuzski", "по-французски", "en français"),
        item(
            "teach-po-angliyski", "по-английски", "en anglais",
            exampleRu = "Я не говорю по-английски.", exampleFr = "Je ne parle pas anglais.",
            grammar = "Négation : не devant le verbe.",
        ),
    )

    private val family = listOf(
        item("teach-mama", "мама", "maman", confusion = "« Mère » plus formel : мать."),
        item("teach-papa", "папа", "papa"),
        item("teach-brat", "брат", "frère"),
        item("teach-sestra", "сестра", "sœur"),
    )

    private val describe = listOf(
        item(
            "teach-vysokiy", "высокий", "grand (taille, masculin)",
            exampleRu = "Он высокий.", exampleFr = "Il est grand.",
            grammar = "Accord : высокий / высокая / высокое.",
            confusion = "« Grand » = taille ici, pas « génial ».",
        ),
        item(
            "teach-dobraya", "добрая", "gentille (féminin)",
            exampleRu = "Она добрая.", exampleFr = "Elle est gentille.",
            confusion = "Masculin : добрый.",
        ),
        item("teach-drug", "друг", "ami", usage = "Personne proche — pas un membre de la famille."),
    )

    private val food = listOf(
        item("teach-voda", "вода", "eau", exampleRu = "Мне воду, пожалуйста.", exampleFr = "De l’eau, s’il vous plaît."),
        item("teach-khleb", "хлеб", "pain"),
        item("teach-chay", "чай", "thé", exampleRu = "Мне чай, пожалуйста.", exampleFr = "Un thé, s’il vous plaît."),
        item("teach-kofe", "кофе", "café (boisson)", grammar = "Souvent invariable au quotidien."),
        item("teach-sup", "суп", "soupe"),
    )

    private val order = listOf(
        item(
            "teach-schyot", "счёт", "l'addition",
            exampleRu = "Счёт, пожалуйста.", exampleFr = "L'addition, s'il vous plaît.",
            register = SpeechRegister.Formal,
        ),
        item(
            "teach-mne-chay", "мне один чай, пожалуйста", "un thé, s'il vous plaît",
            usage = "Formule simple pour commander.",
            exampleRu = "Мне один чай, пожалуйста.", exampleFr = "Un thé, s'il vous plaît.",
        ),
        item(
            "teach-pozhaluysta-order", "пожалуйста", "s'il vous plaît",
            usage = "Politesse en fin de commande.",
            audio = "content/audio/ru/pozhaluysta.ogg",
        ),
    )

    private val city = listOf(
        item("teach-ulitsa", "улица", "rue"),
        item("teach-magazin", "магазин", "magasin"),
        item("teach-muzey", "музей", "musée", exampleRu = "Где музей?", exampleFr = "Où est le musée?"),
    )

    private val transport = listOf(
        item("teach-metro", "метро", "métro"),
        item("teach-avtobus", "автобус", "bus"),
        item(
            "teach-na-metro", "на метро", "en métro",
            exampleRu = "Я еду на метро.", exampleFr = "Je vais en métro.",
            grammar = "на + moyen de transport.",
        ),
    )

    private val shopping = listOf(
        item(
            "teach-skolko", "сколько это стоит?", "combien ça coûte ?",
            exampleRu = "Сколько это стоит?", exampleFr = "Combien ça coûte ?",
        ),
        item(
            "teach-vozmu", "я возьму это", "je prends ceci",
            grammar = "возьму = futur de взять (prendre).",
        ),
        item(
            "teach-spasibo-shop", "спасибо", "merci",
            usage = "Formule polie aussi au magasin.",
            audio = "content/audio/ru/spasibo.ogg",
        ),
    )

    private val prices = listOf(
        item("teach-rubl", "рубль", "rouble (1)", grammar = "1 рубль ; 2–4 рубля ; 5+ рублей."),
        item("teach-rublya", "рубля", "roubles (2–4)", exampleRu = "2 рубля.", exampleFr = "2 roubles."),
        item("teach-rubley", "рублей", "roubles (5+)", exampleRu = "5 рублей.", exampleFr = "5 roubles."),
    )

    private val reviewVocab = listOf(
        numbers.first { it.id == "teach-pyat" },
        family.first { it.id == "teach-sestra" },
        city.first { it.id == "teach-ulitsa" },
        alphabetSample.first { it.id == "teach-letter-m" },
    )

    private val reviewPhrases = listOf(
        greetings.first { it.id == "teach-zdravstvuyte" },
        introduce.first { it.id == "teach-kak-vas-zovut" },
        transport.first { it.id == "teach-metro" },
    )
}
