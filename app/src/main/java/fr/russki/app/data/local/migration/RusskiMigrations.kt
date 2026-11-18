package fr.russki.app.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migrations explicites — jamais de suppression destructive en production.
 *
 * v1 : quiz_sessions, quiz_answers, srs_cards
 * v2 : catalogue pédagogique + packs + historique flashcards + progression leçons
 */
object RusskiMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS content_meta (
                    `key` TEXT NOT NULL,
                    `value` TEXT NOT NULL,
                    PRIMARY KEY(`key`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS content_letters (
                    id TEXT NOT NULL,
                    sortOrder INTEGER NOT NULL,
                    uppercase TEXT NOT NULL,
                    lowercase TEXT NOT NULL,
                    nameRu TEXT NOT NULL,
                    pronunciationGuideFr TEXT NOT NULL,
                    soundInWordsFr TEXT NOT NULL,
                    exampleWordRu TEXT NOT NULL,
                    exampleWordFr TEXT NOT NULL,
                    audioAssetId TEXT,
                    source TEXT NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_content_letters_sortOrder ON content_letters(sortOrder)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS content_words (
                    id TEXT NOT NULL,
                    textRu TEXT NOT NULL,
                    translationFr TEXT NOT NULL,
                    category TEXT NOT NULL,
                    level TEXT NOT NULL,
                    exampleRu TEXT,
                    exampleFr TEXT,
                    audioAssetId TEXT,
                    source TEXT NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_content_words_category ON content_words(category)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_content_words_level ON content_words(level)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS content_phrases (
                    id TEXT NOT NULL,
                    textRu TEXT NOT NULL,
                    translationFr TEXT NOT NULL,
                    category TEXT NOT NULL,
                    level TEXT NOT NULL,
                    audioAssetId TEXT,
                    source TEXT NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_content_phrases_category ON content_phrases(category)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS content_lessons (
                    id TEXT NOT NULL,
                    moduleId TEXT NOT NULL,
                    titleFr TEXT NOT NULL,
                    descriptionFr TEXT NOT NULL,
                    kind TEXT NOT NULL,
                    sortOrder INTEGER NOT NULL,
                    source TEXT NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_content_lessons_moduleId ON content_lessons(moduleId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_content_lessons_sortOrder ON content_lessons(sortOrder)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS content_lesson_exercises (
                    id TEXT NOT NULL,
                    lessonId TEXT NOT NULL,
                    type TEXT NOT NULL,
                    promptFr TEXT NOT NULL,
                    stimulus TEXT NOT NULL,
                    optionsJson TEXT NOT NULL,
                    correctIndex INTEGER NOT NULL,
                    explanationFr TEXT NOT NULL,
                    relatedContentId TEXT,
                    sortOrder INTEGER NOT NULL,
                    PRIMARY KEY(id),
                    FOREIGN KEY(lessonId) REFERENCES content_lessons(id) ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_content_lesson_exercises_lessonId ON content_lesson_exercises(lessonId)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_content_lesson_exercises_lessonId_sortOrder ON content_lesson_exercises(lessonId, sortOrder)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS audio_assets (
                    id TEXT NOT NULL,
                    relativePath TEXT NOT NULL,
                    titleFr TEXT NOT NULL,
                    byteSize INTEGER NOT NULL,
                    durationMs INTEGER,
                    contentRefId TEXT,
                    requiredPackId TEXT,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS content_packs (
                    id TEXT NOT NULL,
                    titleFr TEXT NOT NULL,
                    descriptionFr TEXT NOT NULL,
                    isOptional INTEGER NOT NULL,
                    isBundled INTEGER NOT NULL,
                    totalBytes INTEGER NOT NULL,
                    version TEXT NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS content_pack_members (
                    packId TEXT NOT NULL,
                    memberType TEXT NOT NULL,
                    memberId TEXT NOT NULL,
                    PRIMARY KEY(packId, memberType, memberId),
                    FOREIGN KEY(packId) REFERENCES content_packs(id) ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_content_pack_members_packId ON content_pack_members(packId)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS flashcard_review_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    cardId TEXT NOT NULL,
                    rating TEXT NOT NULL,
                    reviewedAtEpochMs INTEGER NOT NULL,
                    intervalDaysAfter REAL NOT NULL,
                    nextReviewEpochMs INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_flashcard_review_history_cardId ON flashcard_review_history(cardId)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_flashcard_review_history_reviewedAtEpochMs ON flashcard_review_history(reviewedAtEpochMs)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS user_lesson_progress (
                    lessonId TEXT NOT NULL,
                    completedExerciseCount INTEGER NOT NULL,
                    lastOpenedEpochMs INTEGER NOT NULL,
                    completedAtEpochMs INTEGER,
                    PRIMARY KEY(lessonId)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS installed_packs (
                    packId TEXT NOT NULL,
                    state TEXT NOT NULL,
                    downloadedBytes INTEGER NOT NULL,
                    installedAtEpochMs INTEGER,
                    localDir TEXT,
                    lastError TEXT,
                    PRIMARY KEY(packId)
                )
                """.trimIndent(),
            )
            // Index utiles sur tables v1 (si absents)
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_quiz_answers_answeredAtEpochMs ON quiz_answers(answeredAtEpochMs)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_srs_cards_nextReviewEpochMs ON srs_cards(nextReviewEpochMs)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_srs_cards_contentRefId ON srs_cards(contentRefId)",
            )
        }
    }

    val ALL = arrayOf(MIGRATION_1_2)
}
