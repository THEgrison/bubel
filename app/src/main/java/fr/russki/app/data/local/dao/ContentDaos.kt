package fr.russki.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import fr.russki.app.data.local.entity.AudioAssetEntity
import fr.russki.app.data.local.entity.ContentLetterEntity
import fr.russki.app.data.local.entity.ContentLessonEntity
import fr.russki.app.data.local.entity.ContentLessonExerciseEntity
import fr.russki.app.data.local.entity.ContentMetaEntity
import fr.russki.app.data.local.entity.ContentPackEntity
import fr.russki.app.data.local.entity.ContentPackMemberEntity
import fr.russki.app.data.local.entity.ContentPhraseEntity
import fr.russki.app.data.local.entity.ContentWordEntity

@Dao
interface ContentMetaDao {
    @Query("SELECT value FROM content_meta WHERE key = :key LIMIT 1")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: ContentMetaEntity)
}

@Dao
interface ContentLetterDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(items: List<ContentLetterEntity>): List<Long>

    @Query("SELECT * FROM content_letters ORDER BY sortOrder ASC")
    suspend fun getAll(): List<ContentLetterEntity>

    @Query("SELECT COUNT(*) FROM content_letters")
    suspend fun count(): Int

    @Query("SELECT * FROM content_letters WHERE id = :id LIMIT 1")
    suspend fun get(id: String): ContentLetterEntity?
}

@Dao
interface ContentWordDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(items: List<ContentWordEntity>): List<Long>

    @Query("SELECT * FROM content_words ORDER BY category ASC, id ASC")
    suspend fun getAll(): List<ContentWordEntity>

    @Query("SELECT * FROM content_words WHERE category = :category ORDER BY id ASC")
    suspend fun byCategory(category: String): List<ContentWordEntity>

    @Query("SELECT COUNT(*) FROM content_words")
    suspend fun count(): Int
}

@Dao
interface ContentPhraseDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(items: List<ContentPhraseEntity>): List<Long>

    @Query("SELECT * FROM content_phrases ORDER BY id ASC")
    suspend fun getAll(): List<ContentPhraseEntity>

    @Query("SELECT COUNT(*) FROM content_phrases")
    suspend fun count(): Int
}

@Dao
interface ContentLessonDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(items: List<ContentLessonEntity>): List<Long>

    @Query("SELECT * FROM content_lessons ORDER BY sortOrder ASC")
    suspend fun getAll(): List<ContentLessonEntity>

    @Query("SELECT * FROM content_lessons WHERE id = :id LIMIT 1")
    suspend fun get(id: String): ContentLessonEntity?

    @Query("SELECT COUNT(*) FROM content_lessons")
    suspend fun count(): Int
}

@Dao
interface ContentLessonExerciseDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(items: List<ContentLessonExerciseEntity>): List<Long>

    @Query("SELECT * FROM content_lesson_exercises WHERE lessonId = :lessonId ORDER BY sortOrder ASC")
    suspend fun forLesson(lessonId: String): List<ContentLessonExerciseEntity>
}

@Dao
interface AudioAssetDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(items: List<AudioAssetEntity>): List<Long>

    @Query("SELECT * FROM audio_assets WHERE id = :id LIMIT 1")
    suspend fun get(id: String): AudioAssetEntity?

    @Query("SELECT * FROM audio_assets")
    suspend fun getAll(): List<AudioAssetEntity>
}

@Dao
interface ContentPackDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(items: List<ContentPackEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMembersIgnore(items: List<ContentPackMemberEntity>): List<Long>

    @Query("SELECT * FROM content_packs ORDER BY isBundled DESC, titleFr ASC")
    suspend fun getAll(): List<ContentPackEntity>

    @Query("SELECT * FROM content_packs WHERE id = :id LIMIT 1")
    suspend fun get(id: String): ContentPackEntity?

    @Query("SELECT * FROM content_pack_members WHERE packId = :packId")
    suspend fun members(packId: String): List<ContentPackMemberEntity>

    @Transaction
    suspend fun insertPackWithMembers(
        pack: ContentPackEntity,
        members: List<ContentPackMemberEntity>,
    ) {
        insertIgnore(listOf(pack))
        insertMembersIgnore(members)
    }
}
