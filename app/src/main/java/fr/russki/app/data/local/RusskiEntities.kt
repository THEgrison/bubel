package fr.russki.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_sessions")
data class QuizSessionEntity(
    @PrimaryKey val sessionId: String,
    val status: String,
    val startedAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val questionCount: Int,
    val score: Int,
    val currentIndex: Int,
    /** JSON sérialisé minimal des questions (ids + payload). */
    val questionsJson: String,
)

@Entity(
    tableName = "quiz_answers",
    primaryKeys = ["sessionId", "questionId"],
    foreignKeys = [
        ForeignKey(
            entity = QuizSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class QuizAnswerEntity(
    val sessionId: String,
    val questionId: String,
    val userAnswer: String,
    val isCorrect: Boolean,
    val answeredAtEpochMs: Long,
)

@Entity(tableName = "srs_cards")
data class SrsCardEntity(
    @PrimaryKey val cardId: String,
    val contentRefId: String,
    val learningState: String,
    val lastReviewEpochMs: Long?,
    val nextReviewEpochMs: Long,
    val repetitionCount: Int,
    val lapseCount: Int,
    val intervalDays: Double,
    val easeFactor: Double,
    val frontRu: String,
    val backFr: String,
    val transliteration: String?,
    val stressedRu: String?,
    val exampleRu: String?,
    val exampleFr: String?,
    val audioAssetPath: String?,
)
