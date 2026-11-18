package fr.russki.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import fr.russki.app.data.local.dao.AudioAssetDao
import fr.russki.app.data.local.dao.ContentLessonDao
import fr.russki.app.data.local.dao.ContentLessonExerciseDao
import fr.russki.app.data.local.dao.ContentLetterDao
import fr.russki.app.data.local.dao.ContentMetaDao
import fr.russki.app.data.local.dao.ContentPackDao
import fr.russki.app.data.local.dao.ContentPhraseDao
import fr.russki.app.data.local.dao.ContentWordDao
import fr.russki.app.data.local.dao.CurriculumExerciseDao
import fr.russki.app.data.local.dao.CurriculumLessonDao
import fr.russki.app.data.local.dao.CurriculumUnitDao
import fr.russki.app.data.local.dao.FlashcardHistoryDao
import fr.russki.app.data.local.dao.InstalledPackDao
import fr.russki.app.data.local.dao.QuizSessionDao
import fr.russki.app.data.local.dao.SrsCardDao
import fr.russki.app.data.local.dao.UserLessonProgressDao
import fr.russki.app.data.local.entity.AudioAssetEntity
import fr.russki.app.data.local.entity.ContentLetterEntity
import fr.russki.app.data.local.entity.ContentLessonEntity
import fr.russki.app.data.local.entity.ContentLessonExerciseEntity
import fr.russki.app.data.local.entity.ContentMetaEntity
import fr.russki.app.data.local.entity.ContentPackEntity
import fr.russki.app.data.local.entity.ContentPackMemberEntity
import fr.russki.app.data.local.entity.ContentPhraseEntity
import fr.russki.app.data.local.entity.ContentWordEntity
import fr.russki.app.data.local.entity.CurriculumExerciseEntity
import fr.russki.app.data.local.entity.CurriculumLessonEntity
import fr.russki.app.data.local.entity.CurriculumUnitEntity
import fr.russki.app.data.local.entity.FlashcardReviewHistoryEntity
import fr.russki.app.data.local.entity.InstalledPackEntity
import fr.russki.app.data.local.entity.QuizAnswerEntity
import fr.russki.app.data.local.entity.QuizSessionEntity
import fr.russki.app.data.local.entity.SrsCardEntity
import fr.russki.app.data.local.entity.UserLessonProgressEntity
import fr.russki.app.data.local.migration.RusskiMigrations

@Database(
    entities = [
        ContentMetaEntity::class,
        ContentLetterEntity::class,
        ContentWordEntity::class,
        ContentPhraseEntity::class,
        ContentLessonEntity::class,
        ContentLessonExerciseEntity::class,
        AudioAssetEntity::class,
        ContentPackEntity::class,
        ContentPackMemberEntity::class,
        QuizSessionEntity::class,
        QuizAnswerEntity::class,
        SrsCardEntity::class,
        FlashcardReviewHistoryEntity::class,
        UserLessonProgressEntity::class,
        InstalledPackEntity::class,
        CurriculumUnitEntity::class,
        CurriculumLessonEntity::class,
        CurriculumExerciseEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class RusskiDatabase : RoomDatabase() {
    abstract fun contentMetaDao(): ContentMetaDao
    abstract fun contentLetterDao(): ContentLetterDao
    abstract fun contentWordDao(): ContentWordDao
    abstract fun contentPhraseDao(): ContentPhraseDao
    abstract fun contentLessonDao(): ContentLessonDao
    abstract fun contentLessonExerciseDao(): ContentLessonExerciseDao
    abstract fun audioAssetDao(): AudioAssetDao
    abstract fun contentPackDao(): ContentPackDao
    abstract fun quizSessionDao(): QuizSessionDao
    abstract fun srsCardDao(): SrsCardDao
    abstract fun flashcardHistoryDao(): FlashcardHistoryDao
    abstract fun userLessonProgressDao(): UserLessonProgressDao
    abstract fun installedPackDao(): InstalledPackDao
    abstract fun curriculumUnitDao(): CurriculumUnitDao
    abstract fun curriculumLessonDao(): CurriculumLessonDao
    abstract fun curriculumExerciseDao(): CurriculumExerciseDao

    companion object {
        const val NAME = "russki.db"

        @Volatile
        private var instance: RusskiDatabase? = null

        fun get(context: Context): RusskiDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RusskiDatabase::class.java,
                    NAME,
                )
                    .addMigrations(*RusskiMigrations.ALL)
                    .build()
                    .also { instance = it }
            }
        }

        fun createInMemory(context: Context): RusskiDatabase =
            Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                RusskiDatabase::class.java,
            ).allowMainThreadQueries().build()

        /** Base fichier dédiée aux tests (migrations, seed). */
        fun createForTests(
            context: Context,
            name: String,
            allowDestructiveFallback: Boolean = false,
        ): RusskiDatabase {
            val builder = Room.databaseBuilder(
                context.applicationContext,
                RusskiDatabase::class.java,
                name,
            ).allowMainThreadQueries()
                .addMigrations(*RusskiMigrations.ALL)
            if (allowDestructiveFallback) {
                builder.fallbackToDestructiveMigration(dropAllTables = true)
            }
            return builder.build()
        }

        fun clearInstance() {
            instance = null
        }
    }
}
