package fr.russki.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

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
