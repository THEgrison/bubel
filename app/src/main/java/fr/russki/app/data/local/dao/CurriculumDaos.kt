package fr.russki.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.russki.app.data.local.entity.CurriculumExerciseEntity
import fr.russki.app.data.local.entity.CurriculumLessonEntity
import fr.russki.app.data.local.entity.CurriculumUnitEntity

@Dao
interface CurriculumUnitDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(items: List<CurriculumUnitEntity>): List<Long>

    @Query("SELECT * FROM curriculum_units ORDER BY sortOrder ASC")
    suspend fun getAll(): List<CurriculumUnitEntity>

    @Query("SELECT * FROM curriculum_units WHERE id = :id LIMIT 1")
    suspend fun get(id: String): CurriculumUnitEntity?

    @Query("SELECT * FROM curriculum_units WHERE cefrLevel = :level ORDER BY sortOrder ASC")
    suspend fun byCefrLevel(level: String): List<CurriculumUnitEntity>

    @Query("SELECT COUNT(*) FROM curriculum_units")
    suspend fun count(): Int
}

@Dao
interface CurriculumLessonDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(items: List<CurriculumLessonEntity>): List<Long>

    @Query("SELECT * FROM curriculum_lessons WHERE unitId = :unitId ORDER BY sortOrder ASC")
    suspend fun forUnit(unitId: String): List<CurriculumLessonEntity>

    @Query("SELECT * FROM curriculum_lessons WHERE id = :id LIMIT 1")
    suspend fun get(id: String): CurriculumLessonEntity?

    @Query("SELECT COUNT(*) FROM curriculum_lessons")
    suspend fun count(): Int
}

@Dao
interface CurriculumExerciseDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(items: List<CurriculumExerciseEntity>): List<Long>

    @Query("SELECT * FROM curriculum_exercises WHERE lessonId = :lessonId ORDER BY sortOrder ASC")
    suspend fun forLesson(lessonId: String): List<CurriculumExerciseEntity>

    @Query("SELECT COUNT(*) FROM curriculum_exercises")
    suspend fun count(): Int
}
