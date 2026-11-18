package fr.russki.app.domain.quiz

/**
 * Moteur de quiz — indépendant de Compose / Room.
 */

enum class QuizQuestionType {
    MultipleChoice,
    RussianToFrench,
    FrenchToRussian,
    LetterRecognition,
    Cloze,
    WordOrder,
    ListeningComprehension,
}

enum class QuizDifficulty {
    Easy,
    Medium,
    Hard,
}

data class QuizQuestion(
    val id: String,
    val type: QuizQuestionType,
    /** Référence stable vers le contenu pédagogique (lettre, entrée vocab, etc.). */
    val contentRefId: String,
    val promptFr: String,
    /** Stimulus affiché (sauf listening : masqué jusqu'à la correction). */
    val stimulus: String,
    val choices: List<String>,
    val expectedAnswer: String,
    val explanationFr: String,
    val difficulty: QuizDifficulty,
    val audioAssetPath: String?,
    /** Pour WordOrder : tokens mélangés. */
    val shuffledTokens: List<String> = emptyList(),
    /** Si true, ne pas afficher stimulus/réponse avant validation (écoute). */
    val hideAnswerUntilGraded: Boolean = false,
)

data class QuizAnswerRecord(
    val questionId: String,
    val userAnswer: String,
    val isCorrect: Boolean,
    val answeredAtEpochMs: Long,
)

enum class QuizSessionStatus {
    InProgress,
    Completed,
    Abandoned,
}

/**
 * État immuable d'une session — toute réponse produit une nouvelle instance.
 * Empêche le double comptage : [answer] est no-op si la question courante est déjà notée.
 */
data class QuizSessionState(
    val sessionId: String,
    val questions: List<QuizQuestion>,
    val answers: Map<String, QuizAnswerRecord>,
    val currentIndex: Int,
    val status: QuizSessionStatus,
    val startedAtEpochMs: Long,
) {
    val totalCount: Int get() = questions.size
    val answeredCount: Int get() = answers.size
    val score: Int get() = answers.values.count { it.isCorrect }
    val currentQuestion: QuizQuestion? get() = questions.getOrNull(currentIndex)
    val isCurrentAnswered: Boolean
        get() = currentQuestion?.let { answers.containsKey(it.id) } == true
    val isFinished: Boolean
        get() = status == QuizSessionStatus.Completed ||
            (currentIndex >= questions.size && questions.isNotEmpty())
}

data class QuizGradeResult(
    val session: QuizSessionState,
    val record: QuizAnswerRecord,
    val alreadyAnswered: Boolean,
)
