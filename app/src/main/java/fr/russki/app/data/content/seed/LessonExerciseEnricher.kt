package fr.russki.app.data.content.seed

import fr.russki.app.domain.exercise.InteractiveExercise
import fr.russki.app.domain.lesson.PedagogyStage
import fr.russki.app.domain.pronunciation.RussianPronunciation

/**
 * Annote les exercices existants (stages + feedback) sans changer leurs ids / réponses.
 */
object LessonExerciseEnricher {

    fun enrich(lessonId: String, exercises: List<InteractiveExercise>): List<InteractiveExercise> {
        val teachables = LessonTeachCatalog.forLesson(lessonId).associateBy { it.id }
        return exercises.mapIndexed { index, ex ->
            val stage = stageFor(lessonId, ex.id, index, exercises.size)
            val teachableId = teachableIdFor(ex)
            val teach = teachableId?.let { teachables[it] }
            val stimulusPron = RussianPronunciation.lookupOrTransliterate(ex.stimulus)
            ex.copy(
                pedagogyStage = stage,
                teachableId = teachableId,
                translationFr = teach?.translationFr ?: guessTranslation(ex),
                transliteration = teach?.transliteration
                    ?: if (looksCyrillic(ex.stimulus)) stimulusPron.transliteration else null,
                audioAssetPath = teach?.audioAssetPath ?: audioForStimulus(ex.stimulus),
            )
        }
    }

    private fun stageFor(lessonId: String, id: String, index: Int, total: Int): PedagogyStage {
        // Dernier exercice → Final ; première moitié Guided ; reste Active
        return when {
            total >= 2 && index == total - 1 -> PedagogyStage.Final
            index < (total + 1) / 2 -> PedagogyStage.Guided
            else -> PedagogyStage.Active
        }
    }

    private fun teachableIdFor(ex: InteractiveExercise): String? {
        val s = ex.stimulus.lowercase()
        val p = ex.promptFr.lowercase()
        return when {
            "привет" in s || "привет" in p -> "teach-privet"
            "здравствуйте" in s -> "teach-zdravstvuyte"
            "спасибо" in s || ( "merci" in p && "спасибо" in ex.choices.joinToString()) -> "teach-spasibo"
            "до свидания" in s -> "teach-do-svidaniya"
            "пока" in s && "привет" !in s -> "teach-poka"
            "пожалуйста" in s -> "teach-pozhaluysta"
            "меня зовут" in s || "je m'appelle" in p -> "teach-menya-zovut"
            "как вас зовут" in s -> "teach-kak-vas-zovut"
            "я" == s.trim() || s.trim() == "я" -> "teach-ya"
            "вы" in s && "vouvoy" in p -> "teach-vy"
            "это книга" in s || "это книга" in s -> "teach-eto"
            "есть" in s && "брат" in s -> "teach-est"
            "три" == s.trim() -> "teach-tri"
            "пять" in s || "cinq" in p -> "teach-pyat"
            "мне 20" in s || "мне … лет" in p || "20 лет" in s -> "teach-mne"
            "из россии" in s || "я из" in s -> "teach-ya-iz"
            "по-русски" in s -> "teach-po-russki"
            "мама" == s.trim() -> "teach-mama"
            ("брат" in s && "frère" in p) || (ex.choices.contains("брат") && "frère" in p) -> "teach-brat"
            "сестра" in s -> "teach-sestra"
            "вода" == s.trim() -> "teach-voda"
            "хлеб" in s || (ex.choices.contains("хлеб") && "pain" in p) -> "teach-khleb"
            "счёт" in s || "addition" in p -> "teach-schyot"
            "улица" == s.trim() -> "teach-ulitsa"
            "метро" == s.trim() || "метро" in s && "означает" in p -> "teach-metro"
            "сколько это стоит" in s -> "teach-skolko"
            "рубль" == s.trim() -> "teach-rubl"
            "высокий" in s -> "teach-vysokiy"
            "а" == s.trim() || s == "А" -> "teach-letter-a"
            "б" in s.lowercase() && "бэ" in ex.choices.joinToString() -> "teach-letter-b"
            "м" == s.trim() || s == "М" || ("м" in s.lowercase() && "m" in p) -> "teach-letter-m"
            "р" == s.trim() || s == "Р" -> "teach-letter-r"
            "доброе утро" in s -> "teach-dobroe-utro"
            "добрый вечер" in s -> "teach-dobryy-vecher"
            "хорошо" in s -> "teach-khorosho"
            "очень приятно" in s -> "teach-ochen-priyatno"
            "ты" == s.trim() -> "teach-ty"
            "он" == s.trim() -> "teach-on"
            "она" in s || "она" in p -> "teach-ona"
            "мы" == s.trim() -> "teach-my"
            "книга" in s -> "teach-kniga"
            "кафе" in s -> "teach-kafe"
            "один" == s.trim() || "один" in s && "два" in s -> "teach-odin"
            "два" == s.trim() -> "teach-dva"
            "четыре" == s.trim() -> "teach-chetyre"
            "год" == s.trim() -> "teach-god"
            "года" == s.trim() -> "teach-goda"
            "лет" == s.trim() || "лет" in s && "мне" in s -> "teach-let"
            "папа" == s.trim() -> "teach-papa"
            "чай" in s || (ex.choices.contains("чай") && "thé" in p) -> "teach-chay"
            "кофе" in s || (ex.choices.contains("кофе") && "café" in p) -> "teach-kofe"
            "суп" in s -> "teach-sup"
            "магазин" in s -> "teach-magazin"
            "музей" in s -> "teach-muzey"
            "автобус" in s -> "teach-avtobus"
            "на метро" in s || ("на" in ex.acceptedAnswers && "метро" in s) -> "teach-na-metro"
            "возьму" in s || "возьму" in ex.acceptedAnswers -> "teach-vozmu"
            "рубля" in ex.acceptedAnswers || "рубля" == s.trim() -> "teach-rublya"
            "рублей" in ex.acceptedAnswers || "рублей" in s -> "teach-rubley"
            "добрая" in s || "добрая" in ex.acceptedAnswers -> "teach-dobraya"
            "друг" in s -> "teach-drug"
            "по-французски" in s || "по-французски" in ex.acceptedAnswers -> "teach-po-frantsuzski"
            "по-английски" in s || "по-английски" in ex.acceptedAnswers.joinToString() -> "teach-po-angliyski"
            else -> null
        }
    }

    private fun guessTranslation(ex: InteractiveExercise): String? {
        // Si les choix sont en français et qu'une réponse acceptée est FR
        val ans = ex.acceptedAnswers.firstOrNull() ?: return null
        return if (ans.any { it in 'a'..'z' || it in 'A'..'Z' || it in "àâäéèêëïîôùûüç'" }) ans else null
    }

    private fun audioForStimulus(stimulus: String): String? {
        val n = stimulus.lowercase().trim().trimEnd('!', '.', '?')
        return when (n) {
            "привет" -> "content/audio/ru/privet.ogg"
            "здравствуйте", "здравствуйте!" -> "content/audio/ru/zdravstvuyte.ogg"
            "спасибо", "спасибо!" -> "content/audio/ru/spasibo.ogg"
            "до свидания", "до свидания!" -> "content/audio/ru/do-svidaniya.ogg"
            "пока", "пока!" -> "content/audio/ru/poka.ogg"
            "пожалуйста", "пожалуйста." -> "content/audio/ru/pozhaluysta.ogg"
            "доброе утро", "доброе утро!" -> "content/audio/ru/dobroe-utro.ogg"
            "добрый вечер", "добрый вечер!" -> "content/audio/ru/dobryy-vecher.ogg"
            "хорошо" -> "content/audio/ru/khorosho.ogg"
            else -> null
        }
    }

    private fun looksCyrillic(s: String): Boolean =
        s.any { it in '\u0400'..'\u04FF' }
}
