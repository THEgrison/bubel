package fr.russki.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import fr.russki.app.data.local.entity.FlashcardReviewHistoryEntity
import fr.russki.app.data.local.entity.InstalledPackEntity
import fr.russki.app.data.local.entity.QuizAnswerEntity
import fr.russki.app.data.local.entity.QuizSessionEntity
import fr.russki.app.data.local.entity.SrsCardEntity
import fr.russki.app.data.local.entity.UserLessonProgressEntity

@Dao
interface QuizSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: QuizSessionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAnswer(answer: QuizAnswerEntity): Long

    @Query("SELECT * FROM quiz_sessions WHERE sessionId = :id LIMIT 1")
    suspend fun getSession(id: String): QuizSessionEntity?

    @Query("SELECT * FROM quiz_answers WHERE sessionId = :sessionId")
    suspend fun getAnswers(sessionId: String): List<QuizAnswerEntity>

    @Query(
        """
        SELECT * FROM quiz_sessions
        WHERE status = 'InProgress' OR status = 'Abandoned'
        ORDER BY updatedAtEpochMs DESC
        LIMIT 1
        """,
    )
    suspend fun getInterruptedSession(): QuizSessionEntity?

    @Update
    suspend fun updateSession(session: QuizSessionEntity)

    @Transaction
    suspend fun saveAnswerOnce(
        session: QuizSessionEntity,
        answer: QuizAnswerEntity,
    ): Boolean {
        val inserted = insertAnswer(answer)
        if (inserted == -1L) return false
        updateSession(session)
        return true
    }
}

@Dao
interface SrsCardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(card: SrsCardEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(card: SrsCardEntity): Long

    @Query("SELECT * FROM srs_cards WHERE cardId = :id LIMIT 1")
    suspend fun get(id: String): SrsCardEntity?

    @Query("SELECT * FROM srs_cards ORDER BY nextReviewEpochMs ASC")
    suspend fun getAll(): List<SrsCardEntity>

    @Query(
        """
        SELECT * FROM srs_cards
        WHERE nextReviewEpochMs <= :now
        ORDER BY nextReviewEpochMs ASC
        """,
    )
    suspend fun getDue(now: Long): List<SrsCardEntity>

    @Query("SELECT COUNT(*) FROM srs_cards WHERE nextReviewEpochMs <= :now")
    suspend fun countDue(now: Long): Int
}

@Dao
interface FlashcardHistoryDao {
    @Insert
    suspend fun insert(entry: FlashcardReviewHistoryEntity): Long

    @Query("SELECT * FROM flashcard_review_history WHERE cardId = :cardId ORDER BY reviewedAtEpochMs DESC")
    suspend fun forCard(cardId: String): List<FlashcardReviewHistoryEntity>

    @Query("SELECT COUNT(*) FROM flashcard_review_history")
    suspend fun count(): Int
}

@Dao
interface UserLessonProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: UserLessonProgressEntity)

    @Query("SELECT * FROM user_lesson_progress WHERE lessonId = :lessonId LIMIT 1")
    suspend fun get(lessonId: String): UserLessonProgressEntity?

    @Query("SELECT * FROM user_lesson_progress")
    suspend fun getAll(): List<UserLessonProgressEntity>
}

@Dao
interface InstalledPackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pack: InstalledPackEntity)

    @Query("SELECT * FROM installed_packs WHERE packId = :packId LIMIT 1")
    suspend fun get(packId: String): InstalledPackEntity?

    @Query("SELECT * FROM installed_packs")
    suspend fun getAll(): List<InstalledPackEntity>

    @Query("DELETE FROM installed_packs WHERE packId = :packId")
    suspend fun delete(packId: String)
}
