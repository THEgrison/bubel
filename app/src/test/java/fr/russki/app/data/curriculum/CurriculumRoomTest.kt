package fr.russki.app.data.curriculum

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import fr.russki.app.data.content.ContentMetaKeys
import fr.russki.app.data.content.ContentSeedInitializer
import fr.russki.app.data.content.CurriculumVersions
import fr.russki.app.data.content.seed.A1CurriculumSeed
import fr.russki.app.data.content.seed.A1Unit1Seed
import fr.russki.app.data.local.RusskiDatabase
import fr.russki.app.data.local.migration.RusskiMigrations
import fr.russki.app.data.progress.LessonProgressRepository
import fr.russki.app.domain.curriculum.CefrLevel
import fr.russki.app.domain.exercise.InteractiveExerciseType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CurriculumRoomTest {

    private lateinit var context: Context
    private lateinit var db: RusskiDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = RusskiDatabase.createInMemory(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun seed_writesA1CurriculumIdempotently() = runBlocking {
        val seed = ContentSeedInitializer(db)
        val first = seed.ensureSeeded()
        assertEquals(9, first.curriculumUnitCount)
        assertEquals(19, first.curriculumLessonCount)
        assertEquals(57, first.curriculumExerciseCount)
        assertEquals(
            CurriculumVersions.A1_PATH,
            db.contentMetaDao().get(ContentMetaKeys.CURRICULUM_VERSION),
        )

        val second = seed.ensureSeeded()
        assertTrue(second.alreadySeeded)
        assertEquals(9, db.curriculumUnitDao().count())
        assertEquals(19, db.curriculumLessonDao().count())
        assertEquals(57, db.curriculumExerciseDao().count())
    }

    @Test
    fun repository_readsRoomNotMemoryOnly() = runBlocking {
        ContentSeedInitializer(db).ensureSeeded()
        val repo = CurriculumRepository(db, LessonProgressRepository(db.userLessonProgressDao()))

        val path = repo.loadA1Path()
        assertEquals(CefrLevel.A1, path.cefrLevel)
        assertEquals(9, path.units.size)
        assertEquals(A1Unit1Seed.UNIT_ID, path.units.first().unit.id)
        assertEquals(
            A1CurriculumSeed.units.map { it.id },
            path.units.map { it.unit.id },
        )

        val lessonId = A1Unit1Seed.LESSON_GREETINGS
        val exercises = repo.exercises(lessonId)
        assertEquals(3, exercises.size)
        assertTrue(exercises.any { it.type == InteractiveExerciseType.FillBlank })
        assertTrue(exercises.first().acceptedAnswers.isNotEmpty())

        repo.markLessonCompleted(lessonId, totalExercises = 3)
        val after = repo.loadA1Path()
        val greetingsProgress = after.units
            .flatMap { it.lessonProgress.entries }
            .associate { it.toPair() }
        assertTrue(greetingsProgress.getValue(lessonId).isCompleted)
        // Nouvelles leçons non marquées terminées artificiellement
        assertFalse(
            greetingsProgress.getValue(A1CurriculumSeed.LESSON_NUMBERS).isCompleted,
        )
    }

    @Test
    fun upgradeSeed_preservesExistingLessonProgress() = runBlocking {
        val progress = LessonProgressRepository(db.userLessonProgressDao())
        // Simule une base déjà seedée « unité 1 » + progression
        ContentSeedInitializer(db).ensureSeeded()
        progress.completeLesson(A1Unit1Seed.LESSON_ALPHABET, totalExercises = 3)

        // Re-seed (idempotent) : progression conservée, nouvelles leçons à 0
        ContentSeedInitializer(db).ensureSeeded()
        assertNotNull(progress.get(A1Unit1Seed.LESSON_ALPHABET)?.completedAtEpochMs)
        assertEquals(
            null,
            progress.get(A1CurriculumSeed.LESSON_FAMILY)?.completedAtEpochMs,
        )
    }

    @Test
    fun migration_2_to_3_thenSeed_preservesProgress() {
        val name = "migrate-2-3-curriculum.db"
        context.deleteDatabase(name)

        // Base à la version 2 (schéma 1→2 officiel) + progression utilisateur.
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        createV1Tables(db)
                        RusskiMigrations.MIGRATION_1_2.migrate(db)
                        db.execSQL(
                            """
                            INSERT INTO user_lesson_progress
                            (lessonId, completedExerciseCount, lastOpenedEpochMs, completedAtEpochMs)
                            VALUES ('${A1Unit1Seed.LESSON_ALPHABET}', 2, 100, NULL)
                            """.trimIndent(),
                        )
                    }

                    override fun onUpgrade(
                        db: SupportSQLiteDatabase,
                        oldVersion: Int,
                        newVersion: Int,
                    ) = Unit
                },
            )
            .build()
        FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase.close()

        val migrated = Room.databaseBuilder(context, RusskiDatabase::class.java, name)
            .allowMainThreadQueries()
            .addMigrations(*RusskiMigrations.ALL)
            .build()

        runBlocking {
            assertEquals(
                2,
                migrated.userLessonProgressDao().get(A1Unit1Seed.LESSON_ALPHABET)
                    ?.completedExerciseCount,
            )
            assertEquals(0, migrated.curriculumUnitDao().count())

            val result = ContentSeedInitializer(migrated).ensureSeeded()
            assertEquals(9, result.curriculumUnitCount)
            assertEquals(19, result.curriculumLessonCount)
            assertEquals(57, result.curriculumExerciseCount)
            assertFalse(result.alreadySeeded)

            assertEquals(
                2,
                migrated.userLessonProgressDao().get(A1Unit1Seed.LESSON_ALPHABET)
                    ?.completedExerciseCount,
            )
            assertNotNull(migrated.curriculumLessonDao().get(A1Unit1Seed.LESSON_GREETINGS))
        }
        migrated.close()
        context.deleteDatabase(name)
    }

    private fun createV1Tables(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS quiz_sessions (
                sessionId TEXT NOT NULL PRIMARY KEY,
                status TEXT NOT NULL,
                startedAtEpochMs INTEGER NOT NULL,
                updatedAtEpochMs INTEGER NOT NULL,
                questionCount INTEGER NOT NULL,
                score INTEGER NOT NULL,
                currentIndex INTEGER NOT NULL,
                questionsJson TEXT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS quiz_answers (
                sessionId TEXT NOT NULL,
                questionId TEXT NOT NULL,
                userAnswer TEXT NOT NULL,
                isCorrect INTEGER NOT NULL,
                answeredAtEpochMs INTEGER NOT NULL,
                PRIMARY KEY(sessionId, questionId),
                FOREIGN KEY(sessionId) REFERENCES quiz_sessions(sessionId) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_quiz_answers_sessionId ON quiz_answers(sessionId)",
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS srs_cards (
                cardId TEXT NOT NULL PRIMARY KEY,
                contentRefId TEXT NOT NULL,
                learningState TEXT NOT NULL,
                lastReviewEpochMs INTEGER,
                nextReviewEpochMs INTEGER NOT NULL,
                repetitionCount INTEGER NOT NULL,
                lapseCount INTEGER NOT NULL,
                intervalDays REAL NOT NULL,
                easeFactor REAL NOT NULL,
                frontRu TEXT NOT NULL,
                backFr TEXT NOT NULL,
                transliteration TEXT,
                stressedRu TEXT,
                exampleRu TEXT,
                exampleFr TEXT,
                audioAssetPath TEXT
            )
            """.trimIndent(),
        )
    }
}
