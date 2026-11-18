package fr.russki.app.data.curriculum

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.russki.app.data.content.ContentMetaKeys
import fr.russki.app.data.content.ContentSeedInitializer
import fr.russki.app.data.content.ContentSource
import fr.russki.app.data.content.CurriculumVersions
import fr.russki.app.data.content.seed.A1CurriculumSeed
import fr.russki.app.data.content.seed.A1Unit1Seed
import fr.russki.app.data.content.seed.CyrillicPathSeed
import fr.russki.app.data.content.seed.SeedContentPack
import fr.russki.app.data.local.RusskiDatabase
import fr.russki.app.data.local.entity.ContentMetaEntity
import fr.russki.app.data.local.entity.CurriculumExerciseEntity
import fr.russki.app.data.local.entity.CurriculumLessonEntity
import fr.russki.app.data.local.entity.CurriculumUnitEntity
import fr.russki.app.data.local.entity.FlashcardReviewHistoryEntity
import fr.russki.app.data.local.entity.SrsCardEntity
import fr.russki.app.data.progress.LessonProgressRepository
import fr.russki.app.data.review.ReviewableCatalog
import fr.russki.app.domain.srs.SrsLearningState
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
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

/**
 * Simule une base curriculum [CurriculumVersions.A1_PATH_1_1_0],
 * puis l’upgrade vers [CurriculumVersions.A1_PATH] (1.2.0) via [ContentSeedInitializer].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CurriculumUpgrade110to120Test {

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
    fun upgrade_1_1_0_to_1_2_0_preservesProgressSrsAndAddsCyrillicOnce() = runBlocking {
        // —— État 1.1.0 ——
        seedCurriculumSnapshot110()
        db.contentMetaDao().upsert(
            ContentMetaEntity(ContentMetaKeys.CURRICULUM_VERSION, CurriculumVersions.A1_PATH_1_1_0),
        )
        db.contentMetaDao().upsert(
            ContentMetaEntity(ContentMetaKeys.BUNDLED_VERSION, SeedContentPack.VERSION),
        )

        assertEquals(9, db.curriculumUnitDao().count())
        assertEquals(19, db.curriculumLessonDao().count())
        assertEquals(57, db.curriculumExerciseDao().count())
        assertEquals(
            CurriculumVersions.A1_PATH_1_1_0,
            db.contentMetaDao().get(ContentMetaKeys.CURRICULUM_VERSION),
        )
        assertTrue(db.curriculumUnitDao().get(CyrillicPathSeed.UNIT_ID) == null)

        val progress = LessonProgressRepository(db.userLessonProgressDao())
        progress.completeLesson(A1Unit1Seed.LESSON_GREETINGS, totalExercises = 3)
        progress.recordExerciseCompleted(A1Unit1Seed.LESSON_ALPHABET, totalExercises = 3)
        val greetingsCompletedAt = progress.get(A1Unit1Seed.LESSON_GREETINGS)!!.completedAtEpochMs
        assertNotNull(greetingsCompletedAt)

        val sampleExId = "a1-ex-greet-01"
        val sampleBefore = db.curriculumExerciseDao().forLesson(A1Unit1Seed.LESSON_GREETINGS)
            .first { it.id == sampleExId }
        val samplePrompt = sampleBefore.promptFr

        // Carte SRS + historique préexistants (échéance à conserver)
        val reviewItem = ReviewableCatalog.itemsForCompleted(setOf(A1Unit1Seed.LESSON_GREETINGS))
            .first { it.id.startsWith("rv-vocab-") }
        val nextReview = 9_999_000L
        db.srsCardDao().insertIgnore(
            SrsCardEntity(
                cardId = reviewItem.id,
                contentRefId = reviewItem.contentRefId,
                learningState = SrsLearningState.Review.name,
                lastReviewEpochMs = 1_000L,
                nextReviewEpochMs = nextReview,
                repetitionCount = 2,
                lapseCount = 0,
                intervalDays = 3.0,
                easeFactor = 2.5,
                frontRu = reviewItem.stimulus,
                backFr = reviewItem.primaryAnswer,
                transliteration = null,
                stressedRu = null,
                exampleRu = null,
                exampleFr = reviewItem.explanationFr,
                audioAssetPath = null,
            ),
        )
        db.flashcardHistoryDao().insert(
            FlashcardReviewHistoryEntity(
                cardId = reviewItem.id,
                rating = "Good",
                reviewedAtEpochMs = 1_000L,
                intervalDaysAfter = 3.0,
                nextReviewEpochMs = nextReview,
            ),
        )
        val historyBefore = db.flashcardHistoryDao().count()
        val srsCountBefore = 1

        // —— Upgrade 1.2.0 ——
        val first = ContentSeedInitializer(db).ensureSeeded()
        assertFalse(first.alreadySeeded)
        assertEquals(CurriculumVersions.A1_PATH, db.contentMetaDao().get(ContentMetaKeys.CURRICULUM_VERSION))
        assertEquals(10, first.curriculumUnitCount)
        assertEquals(28, first.curriculumLessonCount)
        assertEquals(103, first.curriculumExerciseCount)
        assertNotNull(db.curriculumUnitDao().get(CyrillicPathSeed.UNIT_ID))
        assertEquals(
            CyrillicPathSeed.allLessonIds().size,
            db.curriculumLessonDao().forUnit(CyrillicPathSeed.UNIT_ID).size,
        )

        // Progression utilisateur intacte
        assertEquals(
            greetingsCompletedAt,
            progress.get(A1Unit1Seed.LESSON_GREETINGS)?.completedAtEpochMs,
        )
        assertEquals(
            1,
            progress.get(A1Unit1Seed.LESSON_ALPHABET)?.completedExerciseCount,
        )
        assertEquals(null, progress.get(CyrillicPathSeed.LESSON_G1)?.completedAtEpochMs)

        // Ids d’exercices existants stables
        val sampleAfter = db.curriculumExerciseDao().forLesson(A1Unit1Seed.LESSON_GREETINGS)
            .first { it.id == sampleExId }
        assertEquals(samplePrompt, sampleAfter.promptFr)

        // SRS / historique conservés
        val srs = db.srsCardDao().get(reviewItem.id)!!
        assertEquals(nextReview, srs.nextReviewEpochMs)
        assertEquals(2, srs.repetitionCount)
        assertEquals(historyBefore, db.flashcardHistoryDao().count())
        assertEquals(srsCountBefore, listOf(reviewItem.id).count { db.srsCardDao().get(it) != null })

        // Ordre leçons cyrilliques en Room
        val cyrLessons = db.curriculumLessonDao().forUnit(CyrillicPathSeed.UNIT_ID)
        assertEquals(CyrillicPathSeed.unit.lessons.map { it.id }, cyrLessons.map { it.id })
        assertEquals((1..9).toList(), cyrLessons.map { it.sortOrder })

        // —— Seconde initialisation : aucun doublon ——
        val second = ContentSeedInitializer(db).ensureSeeded()
        assertTrue(second.alreadySeeded)
        assertEquals(10, db.curriculumUnitDao().count())
        assertEquals(28, db.curriculumLessonDao().count())
        assertEquals(103, db.curriculumExerciseDao().count())
        assertEquals(
            greetingsCompletedAt,
            progress.get(A1Unit1Seed.LESSON_GREETINGS)?.completedAtEpochMs,
        )
        assertEquals(nextReview, db.srsCardDao().get(reviewItem.id)!!.nextReviewEpochMs)
        assertEquals(historyBefore, db.flashcardHistoryDao().count())
    }

    @Test
    fun repository_afterUpgrade_listsCyrillicFirst_withoutResettingStatsProgress() = runBlocking {
        seedCurriculumSnapshot110()
        db.contentMetaDao().upsert(
            ContentMetaEntity(ContentMetaKeys.CURRICULUM_VERSION, CurriculumVersions.A1_PATH_1_1_0),
        )
        val progress = LessonProgressRepository(db.userLessonProgressDao())
        progress.completeLesson(A1Unit1Seed.LESSON_INTRODUCE, 3)

        ContentSeedInitializer(db).ensureSeeded()
        val repo = CurriculumRepository(db, progress)
        val path = repo.loadA1Path()
        assertEquals(CyrillicPathSeed.UNIT_ID, path.units.first().unit.id)
        assertEquals(10, path.units.size)
        assertTrue(
            path.units.flatMap { it.lessonProgress.values }
                .first { it.lessonId == A1Unit1Seed.LESSON_INTRODUCE }
                .isCompleted,
        )
        assertFalse(
            path.units.first().lessonProgress.values.any { it.isCompleted },
        )
    }

    /**
     * Insert le curriculum « 1.1.0 » (sans unité cyrillique) — miroir de l’ancien seed.
     */
    private suspend fun seedCurriculumSnapshot110() {
        val units = A1CurriculumSeed.units.filter { it.id != CyrillicPathSeed.UNIT_ID }
        require(units.size == 9)
        db.curriculumUnitDao().insertIgnore(
            units.map { unit ->
                CurriculumUnitEntity(
                    id = unit.id,
                    cefrLevel = unit.cefrLevel.name,
                    titleFr = unit.titleFr,
                    descriptionFr = unit.descriptionFr,
                    sortOrder = unit.sortOrder,
                    source = ContentSource.BUNDLED,
                )
            },
        )
        db.curriculumLessonDao().insertIgnore(
            units.flatMap { unit ->
                unit.lessons.map { lesson ->
                    CurriculumLessonEntity(
                        id = lesson.id,
                        unitId = lesson.unitId,
                        titleFr = lesson.titleFr,
                        objectiveFr = lesson.objectiveFr,
                        introductionFr = lesson.introductionFr,
                        summaryFr = lesson.summaryFr,
                        sortOrder = lesson.sortOrder,
                        exerciseCount = lesson.exerciseCount,
                        legacyRouteHint = lesson.legacyRouteHint,
                        source = ContentSource.BUNDLED,
                    )
                }
            },
        )
        val exercises = units.flatMap { unit ->
            unit.lessons.flatMap { lesson ->
                // CyrillicPathSeed n’est pas dans [units] : exercisesFor renvoie unit1 / extras 1.1.0.
                A1CurriculumSeed.exercisesFor(lesson.id).map { ex ->
                    CurriculumExerciseEntity(
                        id = ex.id,
                        lessonId = lesson.id,
                        type = ex.type.name,
                        promptFr = ex.promptFr,
                        stimulus = ex.stimulus,
                        choicesJson = JSONArray(ex.choices).toString(),
                        acceptedAnswersJson = JSONArray(ex.acceptedAnswers).toString(),
                        explanationFr = ex.explanationFr,
                        sortOrder = ex.sortOrder,
                    )
                }
            }
        }
        db.curriculumExerciseDao().insertIgnore(exercises)
        require(db.curriculumLessonDao().count() == 19) {
            "Snapshot 1.1.0 attendu 19 leçons, got ${db.curriculumLessonDao().count()}"
        }
        require(db.curriculumExerciseDao().count() == 57) {
            "Snapshot 1.1.0 attendu 57 exercices, got ${db.curriculumExerciseDao().count()}"
        }
    }
}
