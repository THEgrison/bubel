package fr.russki.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        QuizSessionEntity::class,
        QuizAnswerEntity::class,
        SrsCardEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class RusskiDatabase : RoomDatabase() {
    abstract fun quizSessionDao(): QuizSessionDao
    abstract fun srsCardDao(): SrsCardDao

    companion object {
        @Volatile
        private var instance: RusskiDatabase? = null

        fun get(context: Context): RusskiDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RusskiDatabase::class.java,
                    "russki.db",
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }

        fun createInMemory(context: Context): RusskiDatabase =
            Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                RusskiDatabase::class.java,
            ).allowMainThreadQueries().build()
    }
}
