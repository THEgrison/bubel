package fr.russki.app.domain.pronunciation

/**
 * Convention unique de translittération pédagogique (francophones).
 *
 * Règles :
 * - Le **cyrillique** reste la forme de référence.
 * - La translittération est une aide de lecture, **pas** une transcription IPA.
 * - L’accent tonique est marqué par un accent aigu sur la voyelle accentuée (á, é, í, ó, ú, ý).
 * - `ё` s’écrit toujours `yo` / `yó` (toujours sous l’accent en russe standard).
 * - Ne jamais inventer un accent : si inconnu, translittérer sans marque
 *   et indiquer [stressUnknown]=true.
 *
 * Voyelles : a e yo i j o u y e yu ya (ы → y ; э → e ; ю → yu ; я → ya).
 * Consonnes usuelles : b v g d zh z k l m n p r s t f kh ts ch sh shch
 * ъ → " ; ь → ' (marque de palatalisation).
 */
object RussianPronunciation {

    data class Result(
        val transliteration: String,
        /** true si aucun accent n’a pu être placé de façon fiable. */
        val stressUnknown: Boolean,
        val noteFr: String? = null,
    )

    /**
     * Accent connu pour des lemmes A1 (forme citation, minuscules, sans ponctuation).
     * Clé = russe normalisé (ё conservé).
     */
    private val knownStress: Map<String, String> = mapOf(
        // salutations / formules
        "привет" to "privét",
        "здравствуйте" to "zdrávstvuyte",
        "доброе утро" to "dóbroe útro",
        "добрый день" to "dóbryy dén'",
        "добрый вечер" to "dóbryy vécher",
        "спокойной ночи" to "spokóynoy nóchi",
        "пока" to "poká",
        "до свидания" to "do svidániya",
        "спасибо" to "spasíbo",
        "пожалуйста" to "pozhalúysta",
        "извините" to "izviníte",
        "как дела" to "kak delá",
        "как дела?" to "kak delá?",
        "хорошо" to "khoroshó",
        "меня зовут" to "menyá zovút",
        "очень приятно" to "óchen' priyátno",
        "как вас зовут" to "kak vas zovút",
        "как вас зовут?" to "kak vas zovút?",
        // présentation / phrases
        "я" to "ya",
        "ты" to "ty",
        "он" to "on",
        "она" to "oná",
        "мы" to "my",
        "вы" to "vy",
        "это" to "éto",
        "есть" to "yest'",
        "книга" to "kníga",
        "кафе" to "kafé",
        "один" to "odín",
        "два" to "dva",
        "три" to "tri",
        "четыре" to "chetýre",
        "пять" to "pyat'",
        "мне" to "mne",
        "лет" to "let",
        "года" to "góda",
        "год" to "god",
        "мама" to "máma",
        "папа" to "pápa",
        "брат" to "brat",
        "сестра" to "sestrá",
        "друг" to "drug",
        "вода" to "vodá",
        "хлеб" to "khleb",
        "чай" to "chay",
        "кофе" to "kófe",
        "суп" to "sup",
        "улица" to "úlitsa",
        "магазин" to "magazín",
        "музей" to "muzéy",
        "метро" to "metró",
        "автобус" to "avtóbus",
        "на метро" to "na metró",
        "рубль" to "rubl'",
        "рубля" to "rublyá",
        "рублей" to "rubliéy",
        "высокий" to "vysókiy",
        "добрая" to "dóbraya",
        "добрый" to "dóbryy",
        "франции" to "frántsii",
        "россии" to "rossíi",
        "германия" to "germániya",
        "зовут" to "zovút",
        "анна" to "ánna",
        "иван" to "iván",
        "по-русски" to "po-rússki",
        "по-французски" to "po-frantsúzski",
        "по-английски" to "po-anglíyski",
        "сколько это стоит" to "skól'ko éto stóit",
        "сколько это стоит?" to "skól'ko éto stóit?",
        "я возьму это" to "ya voz'mú éto",
        "возьму" to "voz'mú",
        "счёт" to "schyot",
        "счёт, пожалуйста" to "schyot, pozhalúysta",
        "мне 20 лет" to "mne dvádtsat' let",
        "мне 20 лет." to "mne dvádtsat' let.",
        "я из россии" to "ya iz rossíi",
        "я из россии." to "ya iz rossíi.",
        "это книга" to "éto kníga",
        "это книга." to "éto kníga.",
        "это кафе" to "éto kafé",
        "это кафе." to "éto kafé.",
        "он высокий" to "on vysókiy",
        "он высокий." to "on vysókiy.",
        "я говорю по-русски" to "ya govor'ú po-rússki",
        "я говорю по-русски." to "ya govor'ú po-rússki.",
        "я не говорю по-английски" to "ya ne govor'ú po-anglíyski",
        // mots / exemples alphabet (clés absentes plus haut)
        "дом" to "dom",
        "кот" to "kot",
        "мир" to "mir",
        "рука" to "ruká",
        "сын" to "syn",
        "жук" to "zhuk",
        "ёлка" to "yólka",
        "банк" to "bank",
        "город" to "górod",
        "журнал" to "zhurnál",
        "зима" to "zimá",
        "имя" to "ímya",
        "май" to "may",
        "лампа" to "lámpa",
        "нога" to "nogá",
        "окно" to "oknó",
        "телефон" to "telefon",
        "фильм" to "fil'm",
        "центр" to "tsentr",
        "час" to "chas",
        "школа" to "shkóla",
        "щи" to "shchi",
        "объект" to "ob\"ékt",
        "мать" to "mat'",
        "юбка" to "yúbka",
        "яблоко" to "yábloko",
    )

    fun lookupOrTransliterate(textRu: String): Result {
        val key = normalizeKey(textRu)
        knownStress[key]?.let { stressed ->
            return Result(
                transliteration = stressed,
                stressUnknown = false,
                noteFr = noteFor(key),
            )
        }
        // Essayer sans ponctuation finale
        val stripped = key.trimEnd('?', '!', '.', ',', '…')
        knownStress[stripped]?.let { stressed ->
            val punct = textRu.trim().lastOrNull()?.takeIf { it in "?!" }?.toString().orEmpty()
            return Result(
                transliteration = stressed + punct,
                stressUnknown = false,
                noteFr = noteFor(stripped),
            )
        }
        return Result(
            transliteration = transliterateUnstressed(textRu),
            stressUnknown = true,
            noteFr = "Accent tonique non indiqué : forme non listée dans la table pédagogique.",
        )
    }

    fun noteFor(normalizedKey: String): String? = when (normalizedKey) {
        "привет" -> "Salutation informelle (tutoiement) — pas interchangeable avec здравствуйте."
        "здравствуйте" -> "Salutation polie (vouvoiement). Plus formelle que привет."
        "пожалуйста" -> "Selon le contexte : « s'il vous plaît », « de rien » ou « je vous en prie »."
        "есть" -> "Homographe : « il y a » / infinitif « manger » — le contexte tranche."
        "вы" -> "Pluriel ou vouvoiement poli."
        else -> null
    }

    fun normalizeKey(textRu: String): String =
        textRu.trim().lowercase().replace('Ё', 'ё')

    /** Translittération lettre à lettre sans placement d’accent. */
    fun transliterateUnstressed(textRu: String): String {
        val sb = StringBuilder()
        val s = textRu.trim()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            val lower = c.lowercaseChar()
            val mapped = when (lower) {
                'а' -> "a"
                'б' -> "b"
                'в' -> "v"
                'г' -> "g"
                'д' -> "d"
                'е' -> "e"
                'ё' -> "yo"
                'ж' -> "zh"
                'з' -> "z"
                'и' -> "i"
                'й' -> "y"
                'к' -> "k"
                'л' -> "l"
                'м' -> "m"
                'н' -> "n"
                'о' -> "o"
                'п' -> "p"
                'р' -> "r"
                'с' -> "s"
                'т' -> "t"
                'у' -> "u"
                'ф' -> "f"
                'х' -> "kh"
                'ц' -> "ts"
                'ч' -> "ch"
                'ш' -> "sh"
                'щ' -> "shch"
                'ъ' -> "\""
                'ы' -> "y"
                'ь' -> "'"
                'э' -> "e"
                'ю' -> "yu"
                'я' -> "ya"
                else -> c.toString()
            }
            sb.append(mapped)
            i++
        }
        return sb.toString()
    }
}
