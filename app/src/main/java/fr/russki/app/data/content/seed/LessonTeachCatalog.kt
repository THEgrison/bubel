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

    private fun item(
        id: String,
        ru: String,
        fr: String,
        usage: String? = null,
        exampleRu: String? = null,
        exampleFr: String? = null,
        audio: String? = null,
        register: SpeechRegister = SpeechRegister.Neutral,
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
        )
    }

    private val greetings = listOf(
        item(
            "teach-privet", "привет", "salut (informel)",
            usage = "Tutoiement entre proches. Ne remplace pas здравствуйте.",
            exampleRu = "Привет! Как дела?", exampleFr = "Salut ! Ça va ?",
            audio = "content/audio/ru/privet.ogg", register = SpeechRegister.Informal,
        ),
        item(
            "teach-zdravstvuyte", "здравствуйте", "bonjour (vouvoiement, poli)",
            usage = "Salutation formelle. Plus polie que привет.",
            exampleRu = "Здравствуйте!", exampleFr = "Bonjour !",
            audio = "content/audio/ru/zdravstvuyte.ogg", register = SpeechRegister.Formal,
        ),
        item(
            "teach-spasibo", "спасибо", "merci",
            exampleRu = "Спасибо большое!", exampleFr = "Merci beaucoup !",
            audio = "content/audio/ru/spasibo.ogg",
        ),
        item(
            "teach-do-svidaniya", "до свидания", "au revoir",
            usage = "Au revoir poli. Пока est l’équivalent informel.",
            exampleRu = "До свидания!", exampleFr = "Au revoir !",
            audio = "content/audio/ru/do-svidaniya.ogg", register = SpeechRegister.Formal,
        ),
        item(
            "teach-poka", "пока", "salut / à plus (informel)",
            usage = "Au revoir informel — pas pour un contexte très formel.",
            audio = "content/audio/ru/poka.ogg", register = SpeechRegister.Informal,
        ),
        item(
            "teach-pozhaluysta", "пожалуйста", "s'il vous plaît / de rien",
            usage = "Selon le contexte : demande polie ou réponse à merci.",
            audio = "content/audio/ru/pozhaluysta.ogg",
        ),
    )

    private val introduce = listOf(
        item(
            "teach-menya-zovut", "меня зовут", "je m'appelle",
            exampleRu = "Меня зовут Анна.", exampleFr = "Je m'appelle Anna.",
        ),
        item(
            "teach-kak-vas-zovut", "как вас зовут?", "comment vous appelez-vous ?",
            usage = "Question polie (vouvoiement).",
        ),
        item(
            "teach-ochen-priyatno", "очень приятно", "enchanté(e)",
            usage = "Formule après une présentation.",
        ),
    )

    private val alphabetSample = listOf(
        item("teach-letter-a", "А а", "lettre a", usage = "Voyelle ; nom russe « а »."),
        item("teach-letter-b", "Б б", "lettre бэ", usage = "À ne pas confondre avec le B latin."),
        item("teach-letter-r", "Р р", "lettre эр (r)", usage = "Ressemble au P latin mais se lit autrement."),
    )

    private val pronouns = listOf(
        item("teach-ya", "я", "je"),
        item("teach-ty", "ты", "tu", usage = "Tutoiement."),
        item("teach-on", "он", "il"),
        item("teach-ona", "она", "elle"),
        item("teach-my", "мы", "nous"),
        item("teach-vy", "вы", "vous", usage = "Pluriel ou vouvoiement poli."),
    )

    private val simpleSentences = listOf(
        item("teach-eto", "это", "c'est / ceci", exampleRu = "Это книга.", exampleFr = "C'est un livre."),
        item("teach-est", "есть", "il y a / avoir", exampleRu = "У меня есть брат.", exampleFr = "J'ai un frère."),
        item("teach-kniga", "книга", "livre"),
    )

    private val numbers = listOf(
        item("teach-odin", "один", "un"),
        item("teach-dva", "два", "deux"),
        item("teach-tri", "три", "trois"),
        item("teach-pyat", "пять", "cinq"),
    )

    private val age = listOf(
        item("teach-mne", "мне", "à moi (datif)", usage = "Мне … лет = j'ai … ans."),
        item("teach-let", "лет", "ans (après 5–20)", usage = "Мне 20 лет."),
    )

    private val countries = listOf(
        item("teach-ya-iz", "я из", "je suis de", exampleRu = "Я из России.", exampleFr = "Je suis de Russie."),
        item("teach-frantsii", "франции", "France (génitif)", usage = "Я из Франции."),
        item("teach-germaniya", "германия", "Allemagne"),
    )

    private val languages = listOf(
        item("teach-po-russki", "по-русски", "en russe", exampleRu = "Я говорю по-русски.", exampleFr = "Je parle russe."),
        item("teach-po-frantsuzski", "по-французски", "en français"),
        item("teach-po-angliyski", "по-английски", "en anglais"),
    )

    private val family = listOf(
        item("teach-mama", "мама", "maman"),
        item("teach-papa", "папа", "papa"),
        item("teach-brat", "брат", "frère"),
        item("teach-sestra", "сестра", "sœur"),
    )

    private val describe = listOf(
        item("teach-vysokiy", "высокий", "grand (taille, masculin)"),
        item("teach-dobraya", "добрая", "gentille (féminin)"),
        item("teach-drug", "друг", "ami"),
    )

    private val food = listOf(
        item("teach-voda", "вода", "eau", audio = null),
        item("teach-khleb", "хлеб", "pain"),
        item("teach-chay", "чай", "thé", audio = null),
        item("teach-kofe", "кофе", "café"),
    )

    private val order = listOf(
        item("teach-schyot", "счёт", "l'addition", exampleRu = "Счёт, пожалуйста.", exampleFr = "L'addition, s'il vous plaît."),
        item("teach-mne-chay", "мне … пожалуйста", "… s'il vous plaît (commande)", usage = "Мне один чай, пожалуйста."),
    )

    private val city = listOf(
        item("teach-ulitsa", "улица", "rue"),
        item("teach-magazin", "магазин", "magasin"),
        item("teach-muzey", "музей", "musée"),
    )

    private val transport = listOf(
        item("teach-metro", "метро", "métro"),
        item("teach-avtobus", "автобус", "bus"),
        item("teach-na-metro", "на метро", "en métro"),
    )

    private val shopping = listOf(
        item("teach-skolko", "сколько это стоит?", "combien ça coûte ?"),
        item("teach-vozmu", "я возьму это", "je prends ceci"),
    )

    private val prices = listOf(
        item("teach-rubl", "рубль", "rouble"),
        item("teach-rublya", "рубля", "roubles (2–4)"),
        item("teach-rubley", "рублей", "roubles (5+)"),
    )

    private val reviewVocab = numbers.take(2) + family.take(2) + city.take(1)
    private val reviewPhrases = greetings.take(2) + introduce.take(1)
}
