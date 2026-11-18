package fr.russki.app.data.local.entity

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
    indices = [Index("sessionId"), Index("answeredAtEpochMs")],
)
data class QuizAnswerEntity(
    val sessionId: String,
    val questionId: String,
    val userAnswer: String,
    val isCorrect: Boolean,
    val answeredAtEpochMs: Long,
)

/**
 * Progression flashcard / SRS (utilisateur).
 * Ne doit jamais être écrasée par le seed de contenu.
 */
@Entity(
    tableName = "srs_cards",
    indices = [
        Index("nextReviewEpochMs"),
        Index("contentRefId"),
    ],
)
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

@Entity(
    tableName = "flashcard_review_history",
    indices = [Index("cardId"), Index("reviewedAtEpochMs")],
)
data class FlashcardReviewHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: String,
    val rating: String,
    val reviewedAtEpochMs: Long,
    val intervalDaysAfter: Double,
    val nextReviewEpochMs: Long,
)

@Entity(tableName = "user_lesson_progress")
data class UserLessonProgressEntity(
    @PrimaryKey val lessonId: String,
    val completedExerciseCount: Int,
    val lastOpenedEpochMs: Long,
    val completedAtEpochMs: Long?,
)

/**
 * État d'installation d'un pack (téléchargé ou embarqué).
 * La suppression d'un pack optionnel n'affecte pas quiz/srs/progression.
 */
@Entity(tableName = "installed_packs")
data class InstalledPackEntity(
    @PrimaryKey val packId: String,
    /** NotInstalled | Downloading | Installed | Failed */
    val state: String,
    val downloadedBytes: Long,
    val installedAtEpochMs: Long?,
    val localDir: String?,
    val lastError: String?,
)
