package fr.russki.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fr.russki.app.data.content.ContentMetaKeys
import fr.russki.app.data.content.ContentSeedInitializer
import fr.russki.app.data.content.InstalledPackState
import fr.russki.app.data.content.OfflineContentRepository
import fr.russki.app.data.content.PackIds
import fr.russki.app.data.content.seed.SeedContentPack
import fr.russki.app.data.flashcard.FlashcardRepository
import fr.russki.app.data.local.entity.ContentMetaEntity
import fr.russki.app.data.local.entity.InstalledPackEntity
import fr.russki.app.data.local.entity.QuizSessionEntity
import fr.russki.app.data.local.entity.SrsCardEntity
import fr.russki.app.data.local.migration.RusskiMigrations
import fr.russki.app.data.pack.PackRepository
import fr.russki.app.data.progress.LessonProgressRepository
import fr.russki.app.data.quiz.QuizRepository
import fr.russki.app.domain.srs.SrsRating
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflinePersistenceTest {

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
    fun firstInstall_seedsCatalogIdempotently() = runBlocking {
        val seed = ContentSeedInitializer(db)
        val first = seed.ensureSeeded()
        assertFalse(first.alreadySeeded)
        assertEquals(33, first.letterCount)
        assertTrue(first.wordCount >= 15)
        assertTrue(first.phraseCount >= 8)
        assertEquals(3, first.lessonCount)

        val second = seed.ensureSeeded()
        assertTrue(second.alreadySeeded)
        assertEquals(first.letterCount, second.letterCount)
        assertEquals(first.wordCount, second.wordCount)
        assertEquals(first.phraseCount, db.contentPhraseDao().count())
    }

    @Test
    fun restart_doesNotDuplicateOrWipeProgress() = runBlocking {
        val seed = ContentSeedInitializer(db)
        seed.ensureSeeded()

        val progress = LessonProgressRepository(db.userLessonProgressDao())
        progress.markOpened("lesson-alphabet")
        progress.recordExerciseCompleted("lesson-alphabet", totalExercises = 10)

        db.quizSessionDao().upsertSession(
            QuizSessionEntity(
                sessionId = "quiz-1",
                status = "Completed",
                startedAtEpochMs = 1L,
                updatedAtEpochMs = 2L,
                questionCount = 5,
                score = 4,
                currentIndex = 5,
                questionsJson = "[]",
            ),
        )
        db.srsCardDao().upsert(
            SrsCardEntity(
                cardId = "fc-test",
                contentRefId = "vocab-greet-001",
                learningState = "Review",
                lastReviewEpochMs = 10L,
                nextReviewEpochMs = 99L,
                repetitionCount = 2,
                lapseCount = 0,
                intervalDays = 3.0,
                easeFactor = 2.5,
                frontRu = "привет",
                backFr = "salut",
                transliteration = null,
                stressedRu = null,
                exampleRu = null,
                exampleFr = null,
                audioAssetPath = null,
            ),
        )

        seed.ensureSeeded()

        assertEquals(1, progress.getAll().size)
        assertEquals(1, progress.get("lesson-alphabet")?.completedExerciseCount)
        assertNotNull(db.quizSessionDao().getSession("quiz-1"))
        assertEquals(99L, db.srsCardDao().get("fc-test")?.nextReviewEpochMs)
        assertEquals(33, db.contentLetterDao().count())
    }

    @Test
    fun appUpdate_bumpsMetaWithoutLosingProgress() = runBlocking {
        val seed = ContentSeedInitializer(db)
        seed.ensureSeeded()
        LessonProgressRepository(db.userLessonProgressDao()).markOpened("lesson-greetings")

        db.contentMetaDao().upsert(
            ContentMetaEntity(
                key = ContentMetaKeys.BUNDLED_VERSION,
                value = "0.9.0",
            ),
        )
        val afterUpdate = seed.ensureSeeded()
        assertFalse(afterUpdate.alreadySeeded)
        assertEquals(SeedContentPack.VERSION, afterUpdate.bundledVersion)
        assertNotNull(db.userLessonProgressDao().get("lesson-greetings"))
    }

    @Test
    fun offlineEssentials_quizFlashcardsLessonsWithoutNetwork() = runBlocking {
        ContentSeedInitializer(db).ensureSeeded()
        val content = OfflineContentRepository()
        assertEquals(33, content.letters().size)
        assertTrue(content.exercises().isNotEmpty())
        assertNotNull(content.vocabularyModule("vocab-greetings"))

        val quiz = QuizRepository(db.quizSessionDao(), content)
        val session = quiz.startSession(5)
        assertEquals(5, session.questions.size)
        assertTrue(session.questions.all { it.promptFr.isNotBlank() })

        val progress = LessonProgressRepository(db.userLessonProgressDao())
        progress.completeLesson("a1-lesson-greetings", 3)
        val review = fr.russki.app.data.review.ReviewRepository(
            progressRepository = progress,
            srsDao = db.srsCardDao(),
            historyDao = db.flashcardHistoryDao(),
        )
        val flash = FlashcardRepository(
            reviewRepository = review,
            historyDao = db.flashcardHistoryDao(),
            contentRepository = content,
        )
        flash.ensureDeckSeeded()
        assertTrue(flash.dueCount() > 0)
        val due = flash.dueFlashcards().first()
        flash.grade(due.id, SrsRating.Good)
        assertTrue(flash.reviewHistoryCount() >= 1)
        assertNotNull(flash.cardState(due.id)?.nextReviewEpochMs)

        assertEquals(
            InstalledPackState.INSTALLED,
            db.installedPackDao().get(PackIds.CORE_BUNDLED)?.state,
        )
        assertTrue(db.contentLessonDao().getAll().isNotEmpty())
        assertTrue(db.audioAssetDao().getAll().isNotEmpty())
    }

    @Test
    fun deleteOptionalPack_keepsUserProgress() = runBlocking {
        ContentSeedInitializer(db).ensureSeeded()
        LessonProgressRepository(db.userLessonProgressDao()).markOpened("lesson-alphabet")
        db.srsCardDao().upsert(
            SrsCardEntity(
                cardId = "fc-keep",
                contentRefId = "x",
                learningState = "New",
                lastReviewEpochMs = null,
                nextReviewEpochMs = 1L,
                repetitionCount = 0,
                lapseCount = 0,
                intervalDays = 0.0,
                easeFactor = 2.5,
                frontRu = "А",
                backFr = "a",
                transliteration = null,
                stressedRu = null,
                exampleRu = null,
                exampleFr = null,
                audioAssetPath = null,
            ),
        )

        val packs = PackRepository(context, db)
        db.installedPackDao().upsert(
            InstalledPackEntity(
                packId = PackIds.AUDIO_DEMO_OPTIONAL,
                state = InstalledPackState.INSTALLED,
                downloadedBytes = 128L,
                installedAtEpochMs = 1L,
                localDir = context.filesDir.resolve("packs/audio-demo-v1").absolutePath,
                lastError = null,
            ),
        )
        val deleted = packs.deleteOptionalPack(PackIds.AUDIO_DEMO_OPTIONAL)
        assertTrue(deleted.isSuccess)
        assertEquals(
            InstalledPackState.NOT_INSTALLED,
            db.installedPackDao().get(PackIds.AUDIO_DEMO_OPTIONAL)?.state,
        )
        assertNotNull(db.userLessonProgressDao().get("lesson-alphabet"))
        assertNotNull(db.srsCardDao().get("fc-keep"))
        assertEquals(33, db.contentLetterDao().count())
    }

    @Test
    fun schemaMigration_1_to_2_preservesQuizAndSrs() {
        val name = "migrate-1-2-test.db"
        context.deleteDatabase(name)

        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { sqlite ->
            createV1Schema(sqlite)
            sqlite.execSQL(
                """
                INSERT INTO quiz_sessions
                (sessionId, status, startedAtEpochMs, updatedAtEpochMs, questionCount, score, currentIndex, questionsJson)
                VALUES ('legacy-quiz', 'InProgress', 5, 6, 3, 1, 1, '[]')
                """.trimIndent(),
            )
            sqlite.execSQL(
                """
                INSERT INTO srs_cards
                (cardId, contentRefId, learningState, lastReviewEpochMs, nextReviewEpochMs,
                 repetitionCount, lapseCount, intervalDays, easeFactor,
                 frontRu, backFr, transliteration, stressedRu, exampleRu, exampleFr, audioAssetPath)
                VALUES ('legacy-card', 'c', 'New', NULL, 42, 0, 0, 0.0, 2.5,
                        'Б', 'b', NULL, NULL, NULL, NULL, NULL)
                """.trimIndent(),
            )
            sqlite.execSQL("PRAGMA user_version = 1")
        }

        val migrated = Room.databaseBuilder(context, RusskiDatabase::class.java, name)
            .allowMainThreadQueries()
            .addMigrations(*RusskiMigrations.ALL)
            .build()

        runBlocking {
            assertNotNull(migrated.quizSessionDao().getSession("legacy-quiz"))
            assertEquals(42L, migrated.srsCardDao().get("legacy-card")?.nextReviewEpochMs)
            assertNull(migrated.contentMetaDao().get(ContentMetaKeys.BUNDLED_VERSION))
            val seeded = ContentSeedInitializer(migrated).ensureSeeded()
            assertEquals(33, migrated.contentLetterDao().count())
            assertEquals(42L, migrated.srsCardDao().get("legacy-card")?.nextReviewEpochMs)
            assertEquals(10, seeded.curriculumUnitCount)
            assertEquals(28, seeded.curriculumLessonCount)
        }
        migrated.close()
        context.deleteDatabase(name)
    }

    private fun createV1Schema(db: SQLiteDatabase) {
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
