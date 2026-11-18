package fr.russki.app.data.listening

import fr.russki.app.domain.listening.ListeningItem

/**
 * Banque d'écoute — salutations, présentations, extraits des dialogues A1.
 * Les clés audio pointent vers [fr.russki.app.data.audio.PedagogicalAudioCatalog] ;
 * sans fichier, l'UI affiche le texte russe (repli).
 */
object ListeningExerciseBank {

    fun items(): List<ListeningItem> = listOf(
        ListeningItem(
            id = "listen-greet-zdravstvuyte",
            audioKey = "vocab-greet-002",
            promptFr = "Écoutez (ou lisez) puis choisissez la traduction.",
            textRuFallback = "здравствуйте",
            choices = listOf(
                "bonjour (vouvoiement, poli)",
                "salut (tutoiement, informel)",
                "au revoir",
                "merci",
            ),
            acceptedAnswers = listOf(
                "bonjour (vouvoiement, poli)",
                "bonjour",
            ),
            explanationFr = "здравствуйте = bonjour poli (vouvoiement).",
        ),
        ListeningItem(
            id = "listen-greet-privet",
            audioKey = "vocab-greet-001",
            promptFr = "Écoutez (ou lisez) puis choisissez la traduction.",
            textRuFallback = "привет",
            choices = listOf(
                "salut (tutoiement, informel)",
                "bonjour (vouvoiement, poli)",
                "s'il vous plaît",
                "bonne nuit",
            ),
            acceptedAnswers = listOf("salut (tutoiement, informel)", "salut"),
            explanationFr = "привет = salut informel.",
        ),
        ListeningItem(
            id = "listen-greet-spasibo",
            audioKey = "vocab-greet-009",
            promptFr = "Écoutez (ou lisez) puis choisissez la traduction.",
            textRuFallback = "спасибо",
            choices = listOf("merci", "excusez-moi", "au revoir", "bonjour"),
            acceptedAnswers = listOf("merci", "Merci"),
            explanationFr = "спасибо = merci.",
        ),
        ListeningItem(
            id = "listen-intro-menya-zovut",
            audioKey = "vocab-greet-014",
            promptFr = "Écoutez (ou lisez) : que signifie cette formule ?",
            textRuFallback = "меня зовут",
            choices = listOf(
                "je m'appelle",
                "comment vous appelez-vous ?",
                "enchanté",
                "je suis de…",
            ),
            acceptedAnswers = listOf("je m'appelle", "Je m'appelle"),
            explanationFr = "Меня зовут… = Je m'appelle…",
        ),
        ListeningItem(
            id = "listen-intro-ochen",
            audioKey = "vocab-greet-015",
            promptFr = "Écoutez (ou lisez) puis choisissez la traduction.",
            textRuFallback = "очень приятно",
            choices = listOf(
                "enchanté(e) / ravi(e) de faire votre connaissance",
                "au revoir",
                "merci beaucoup",
                "comment ça va ?",
            ),
            acceptedAnswers = listOf(
                "enchanté(e) / ravi(e) de faire votre connaissance",
                "enchanté",
            ),
            explanationFr = "очень приятно = enchanté(e).",
        ),
        ListeningItem(
            id = "listen-meet-l1",
            audioKey = "meet-l1",
            promptFr = "Réplique du dialogue « Faire connaissance ». Que dit Anna ?",
            textRuFallback = "Здравствуйте!",
            choices = listOf(
                "Bonjour ! (vouvoiement)",
                "Salut ! (tutoiement)",
                "Merci !",
                "Au revoir !",
            ),
            acceptedAnswers = listOf("Bonjour ! (vouvoiement)", "Bonjour"),
            explanationFr = "Première réplique : salutation polie.",
        ),
        ListeningItem(
            id = "listen-drink-tea",
            audioKey = "drink-l2",
            promptFr = "Réplique du dialogue café. Que demande le client ?",
            textRuFallback = "Мне один чай, пожалуйста.",
            choices = listOf(
                "Un thé, s'il vous plaît",
                "L'addition, s'il vous plaît",
                "Où est le métro ?",
                "Je m'appelle Ivan",
            ),
            acceptedAnswers = listOf(
                "Un thé, s'il vous plaît",
                "un thé, s'il vous plaît",
            ),
            explanationFr = "Мне один чай, пожалуйста = Un thé, s'il vous plaît.",
        ),
        ListeningItem(
            id = "listen-ask-metro",
            audioKey = "ask-l1",
            promptFr = "Réplique « Demander son chemin ». Que demande le touriste ?",
            textRuFallback = "Извините, где метро?",
            choices = listOf(
                "Excusez-moi, où est le métro ?",
                "Combien ça coûte ?",
                "Un café, s'il vous plaît",
                "Comment vous appelez-vous ?",
            ),
            acceptedAnswers = listOf(
                "Excusez-moi, où est le métro ?",
                "où est le métro",
            ),
            explanationFr = "Извините, где метро? = Excusez-moi, où est le métro ?",
        ),
    )
}
