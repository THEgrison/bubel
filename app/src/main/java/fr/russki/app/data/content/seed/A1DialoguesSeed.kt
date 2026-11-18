package fr.russki.app.data.content.seed

import fr.russki.app.domain.curriculum.CefrLevel
import fr.russki.app.domain.dialogue.DialogueActivity
import fr.russki.app.domain.dialogue.DialogueActivityType
import fr.russki.app.domain.dialogue.DialogueLine
import fr.russki.app.domain.dialogue.DialogueOutline
import fr.russki.app.domain.dialogue.DialogueScript
import fr.russki.app.domain.dialogue.DialogueSpeaker

/**
 * Trois dialogues A1 embarqués — contenu fixe, hors ligne.
 * Aucun chemin audio n'est présenté comme disponible (tous `null`).
 */
object A1DialoguesSeed {

    const val ID_MEET = "dialogue-a1-meet"
    const val ID_DRINK = "dialogue-a1-drink"
    const val ID_ASK = "dialogue-a1-ask-info"

    val all: List<DialogueScript> by lazy {
        listOf(meetScript, drinkScript, askInfoScript)
    }

    fun byId(id: String): DialogueScript? = all.find { it.outline.id == id }

    fun outlines(): List<DialogueOutline> = all.map { it.outline }

    // —— 1. Faire connaissance ——

    private val meetScript = DialogueScript(
        outline = DialogueOutline(
            id = ID_MEET,
            titleFr = "Faire connaissance",
            cefrLevel = CefrLevel.A1,
            situationFr = "Deux personnes se rencontrent pour la première fois dans un café.",
            objectiveFr = "Saluer, se présenter et répondre poliment.",
            lineCount = 6,
            activityCount = 3,
        ),
        speakers = listOf(
            DialogueSpeaker("anna", "Anna"),
            DialogueSpeaker("ivan", "Ivan"),
        ),
        lines = listOf(
            line("meet-l1", 0, "anna", "Здравствуйте!", "Bonjour ! (vouvoiement)", null),
            line("meet-l2", 1, "ivan", "Здравствуйте! Меня зовут Иван.", "Bonjour ! Je m'appelle Ivan.", null),
            line("meet-l3", 2, "anna", "Очень приятно. Меня зовут Анна.", "Enchanté. Je m'appelle Anna.", null),
            line("meet-l4", 3, "ivan", "Очень приятно, Анна. Вы из Франции?", "Enchanté, Anna. Vous êtes de France ?", null),
            line("meet-l5", 4, "anna", "Да, я из Франции. А вы?", "Oui, je suis de France. Et vous ?", null),
            line("meet-l6", 5, "ivan", "Я из России.", "Je suis de Russie.", null),
        ),
        activities = listOf(
            DialogueActivity(
                id = "meet-a1",
                type = DialogueActivityType.ChooseTranslation,
                afterLineIndex = 0,
                promptFr = "Que signifie « Здравствуйте » ?",
                stimulusRu = "Здравствуйте!",
                choices = listOf(
                    "Bonjour ! (vouvoiement)",
                    "Salut ! (tutoiement)",
                    "Au revoir",
                    "Merci",
                ),
                acceptedAnswers = listOf("Bonjour ! (vouvoiement)", "Bonjour", "bonjour"),
                explanationFr = "Здравствуйте est la salutation polie (vouvoiement), plus formelle que привет.",
            ),
            DialogueActivity(
                id = "meet-a2",
                type = DialogueActivityType.FillBlank,
                afterLineIndex = 2,
                promptFr = "Complétez la formule d'Anna : « Меня _____ Анна. »",
                stimulusRu = "Меня _____ Анна.",
                choices = listOf("зовут", "есть", "хочу", "знаю"),
                acceptedAnswers = listOf("зовут", "Зовут"),
                explanationFr = "Меня зовут… = Je m'appelle…",
            ),
            DialogueActivity(
                id = "meet-a3",
                type = DialogueActivityType.ChooseReply,
                afterLineIndex = 3,
                promptFr = "Ivan demande si Anna est de France. Quelle réponse convient ?",
                stimulusRu = "Вы из Франции?",
                choices = listOf(
                    "Да, я из Франции.",
                    "До свидания!",
                    "Мне один чай, пожалуйста.",
                    "Сколько это стоит?",
                ),
                acceptedAnswers = listOf("Да, я из Франции.", "Да, я из Франции"),
                explanationFr = "On répond à la question sur l'origine : Да, я из…",
            ),
        ),
    )

    // —— 2. Commander une boisson ——

    private val drinkScript = DialogueScript(
        outline = DialogueOutline(
            id = ID_DRINK,
            titleFr = "Commander une boisson",
            cefrLevel = CefrLevel.A1,
            situationFr = "Dans un café, un client commande une boisson au serveur.",
            objectiveFr = "Demander poliment une boisson et remercier.",
            lineCount = 6,
            activityCount = 3,
        ),
        speakers = listOf(
            DialogueSpeaker("client", "Client"),
            DialogueSpeaker("waiter", "Serveur"),
        ),
        lines = listOf(
            line("drink-l1", 0, "waiter", "Добрый день! Что вы будете?", "Bonjour ! Que désirez-vous ?", null),
            line(
                "drink-l2",
                1,
                "client",
                "Здравствуйте. Мне один чай, пожалуйста.",
                "Bonjour. Un thé, s'il vous plaît.",
                null,
            ),
            line("drink-l3", 2, "waiter", "С сахаром или без сахара?", "Avec du sucre ou sans sucre ?", null),
            line("drink-l4", 3, "client", "Без сахара, пожалуйста.", "Sans sucre, s'il vous plaît.", null),
            line("drink-l5", 4, "waiter", "Хорошо. Вот ваш чай.", "Très bien. Voici votre thé.", null),
            line("drink-l6", 5, "client", "Спасибо!", "Merci !", null),
        ),
        activities = listOf(
            DialogueActivity(
                id = "drink-a1",
                type = DialogueActivityType.ChooseTranslation,
                afterLineIndex = 1,
                promptFr = "Que demande le client ?",
                stimulusRu = "Мне один чай, пожалуйста.",
                choices = listOf(
                    "Un thé, s'il vous plaît",
                    "L'addition, s'il vous plaît",
                    "Où sont les toilettes ?",
                    "Je ne comprends pas",
                ),
                acceptedAnswers = listOf(
                    "Un thé, s'il vous plaît",
                    "un thé, s'il vous plaît",
                ),
                explanationFr = "Мне один чай, пожалуйста — formule simple pour commander une boisson.",
            ),
            DialogueActivity(
                id = "drink-a2",
                type = DialogueActivityType.ChooseReply,
                afterLineIndex = 2,
                promptFr = "Le serveur propose sucre ou non. Quelle réponse convient pour « sans sucre » ?",
                stimulusRu = "С сахаром или без сахара?",
                choices = listOf(
                    "Без сахара, пожалуйста.",
                    "Меня зовут Иван.",
                    "Я не говорю по-русски.",
                    "Сколько времени?",
                ),
                acceptedAnswers = listOf("Без сахара, пожалуйста.", "Без сахара, пожалуйста"),
                explanationFr = "Без сахара = sans sucre ; пожалуйста reste poli.",
            ),
            DialogueActivity(
                id = "drink-a3",
                type = DialogueActivityType.FillBlank,
                afterLineIndex = 5,
                promptFr = "Pour remercier, on dit « _____ ».",
                stimulusRu = "_____!",
                choices = listOf("Спасибо", "Пожалуйста", "Здравствуйте", "Извините"),
                acceptedAnswers = listOf("Спасибо", "спасибо"),
                explanationFr = "Спасибо = merci. Пожалуйста sert aussi de « de rien / s'il vous plaît ».",
            ),
        ),
    )

    // —— 3. Demander un renseignement ——

    private val askInfoScript = DialogueScript(
        outline = DialogueOutline(
            id = ID_ASK,
            titleFr = "Demander un renseignement",
            cefrLevel = CefrLevel.A1,
            situationFr = "Dans la rue, un touriste demande le chemin de la station de métro.",
            objectiveFr = "Demander poliment un renseignement simple et remercier.",
            lineCount = 6,
            activityCount = 3,
        ),
        speakers = listOf(
            DialogueSpeaker("tourist", "Touriste"),
            DialogueSpeaker("passerby", "Passant"),
        ),
        lines = listOf(
            line(
                "ask-l1",
                0,
                "tourist",
                "Извините, где метро?",
                "Excusez-moi, où est le métro ?",
                null,
            ),
            line(
                "ask-l2",
                1,
                "passerby",
                "Метро прямо, потом налево.",
                "Le métro est tout droit, puis à gauche.",
                null,
            ),
            line("ask-l3", 2, "tourist", "Далеко?", "C'est loin ?", null),
            line(
                "ask-l4",
                3,
                "passerby",
                "Нет, близко. Пять минут пешком.",
                "Non, c'est près. Cinq minutes à pied.",
                null,
            ),
            line("ask-l5", 4, "tourist", "Спасибо большое!", "Merci beaucoup !", null),
            line("ask-l6", 5, "passerby", "Пожалуйста. Удачи!", "De rien. Bonne chance !", null),
        ),
        activities = listOf(
            DialogueActivity(
                id = "ask-a1",
                type = DialogueActivityType.ChooseTranslation,
                afterLineIndex = 0,
                promptFr = "Que demande le touriste ?",
                stimulusRu = "Извините, где метро?",
                choices = listOf(
                    "Excusez-moi, où est le métro ?",
                    "Combien coûte le billet ?",
                    "Quelle heure est-il ?",
                    "Parlez-vous anglais ?",
                ),
                acceptedAnswers = listOf(
                    "Excusez-moi, où est le métro ?",
                    "où est le métro",
                ),
                explanationFr = "Извините attire l'attention poliment ; где… ? = où est… ?",
            ),
            DialogueActivity(
                id = "ask-a2",
                type = DialogueActivityType.FillBlank,
                afterLineIndex = 1,
                promptFr = "Dans « Метро прямо, потом _____ », quel mot signifie « à gauche » ?",
                stimulusRu = "Метро прямо, потом _____.",
                choices = listOf("налево", "направо", "прямо", "далеко"),
                acceptedAnswers = listOf("налево", "Налево"),
                explanationFr = "налево = à gauche ; направо = à droite ; прямо = tout droit.",
            ),
            DialogueActivity(
                id = "ask-a3",
                type = DialogueActivityType.ChooseReply,
                afterLineIndex = 3,
                promptFr = "Après l'indication, que dit-on pour remercier chaleureusement ?",
                stimulusRu = "Нет, близко. Пять минут пешком.",
                choices = listOf(
                    "Спасибо большое!",
                    "Мне один кофе.",
                    "Как вас зовут?",
                    "Я не знаю.",
                ),
                acceptedAnswers = listOf("Спасибо большое!", "Спасибо большое"),
                explanationFr = "Спасибо большое = merci beaucoup — adapté après une aide.",
            ),
        ),
    )

    private fun line(
        id: String,
        order: Int,
        speakerId: String,
        ru: String,
        fr: String,
        audio: String?,
    ) = DialogueLine(
        id = id,
        sortOrder = order,
        speakerId = speakerId,
        textRu = ru,
        textFr = fr,
        audioAssetPath = audio,
    )
}
