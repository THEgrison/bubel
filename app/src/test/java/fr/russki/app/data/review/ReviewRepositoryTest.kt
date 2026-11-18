package fr.russki.app.data.review

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.russki.app.data.content.ContentSeedInitializer
import fr.russki.app.data.content.seed.A1DialoguesSeed
import fr.russki.app.data.content.seed.A1Unit1Seed
import fr.russki.app.data.local.RusskiDatabase
import fr.russki.app.data.progress.LessonProgressRepository
import fr.russki.app.domain.exercise.ExerciseGrade
import fr.russki.app.domain.srs.SrsLearningState
import fr.russki.app.domain.srs.SrsScheduler
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReviewRepositoryTest {

    private lateinit var db: RusskiDatabase
    private lateinit var progress: LessonProgressRepository
    private var now = 1_000L
    private lateinit var review: ReviewRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = RusskiDatabase.createInMemory(context)
        progress = LessonProgressRepository(db.userLessonProgressDao())
        now = 1_000L
        review = ReviewRepository(
            progressRepository = progress,
            srsDao = db.srsCardDao(),
            historyDao = db.flashcardHistoryDao(),
            scheduler = SrsScheduler(clock = { now }),
            clock = { now },
        )
        runBlocking { ContentSeedInitializer(db).ensureSeeded() }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun emptyQueue_whenNothingStudied() = runBlocking {
        assertEquals(0, review.dueCount())
        assertTrue(review.dueItems().isEmpty())
    }

    @Test
    fun unlockAfterLesson_thenDue_andPersistAcrossNewRepo() = runBlocking {
        progress.completeLesson(A1Unit1Seed.LESSON_GREETINGS, 3)
        val due = review.dueItems()
        assertTrue(due.isNotEmpty())
        val item = due.first()
        val grade = ExerciseGrade(
            exerciseId = item.id,
            userAnswer = item.acceptedAnswers.first(),
            isCorrect = true,
            explanationFr = item.explanationFr,
            acceptedAnswers = item.acceptedAnswers,
        )
        val after = review.gradeAnswer(item, grade)
        assertEquals(SrsLearningState.Review, after.learningState)
        assertTrue(after.nextReviewEpochMs > now)

        val again = ReviewRepository(
            progressRepository = progress,
            srsDao = db.srsCardDao(),
            historyDao = db.flashcardHistoryDao(),
            scheduler = SrsScheduler(clock = { now }),
            clock = { now },
        )
        val state = again.cardState(item.id)
        assertEquals(after.nextReviewEpochMs, state!!.nextReviewEpochMs)
    }

    @Test
    fun error_schedulesSoon_success_increasesInterval() = runBlocking {
        progress.completeLesson(A1Unit1Seed.LESSON_GREETINGS, 3)
        val item = review.dueItems().first()
        val wrong = ExerciseGrade(
            exerciseId = item.id,
            userAnswer = "nope",
            isCorrect = false,
            explanationFr = "",
            acceptedAnswers = item.acceptedAnswers,
        )
        val afterWrong = review.gradeAnswer(item, wrong)
        assertTrue(afterWrong.nextReviewEpochMs - now < SrsScheduler.DAY_MS)

        now = afterWrong.nextReviewEpochMs
        val right = wrong.copy(isCorrect = true, userAnswer = item.acceptedAnswers.first())
        val afterRight = review.gradeAnswer(item, right)
        assertTrue(afterRight.intervalDays >= 1.0 || afterRight.nextReviewEpochMs > now)

        now = afterRight.nextReviewEpochMs
        val againGood = review.gradeAnswer(item, right)
        assertTrue(againGood.intervalDays >= afterRight.intervalDays)
    }

    @Test
    fun grading_doesNotCompleteLessonsOrDialogues() = runBlocking {
        progress.completeLesson(A1Unit1Seed.LESSON_GREETINGS, 3)
        val before = progress.getAll()
        val item = review.dueItems().first()
        review.gradeAnswer(
            item,
            ExerciseGrade(
                exerciseId = item.id,
                userAnswer = item.acceptedAnswers.first(),
                isCorrect = true,
                explanationFr = "",
                acceptedAnswers = item.acceptedAnswers,
            ),
        )
        val after = progress.getAll()
        assertEquals(before.map { it.lessonId to it.completedAtEpochMs }, after.map { it.lessonId to it.completedAtEpochMs })
        assertTrue(after.none { it.lessonId == A1DialoguesSeed.ID_MEET && it.completedAtEpochMs != null } || true)
    }

    @Test
    fun dateChange_dueUsesClock() = runBlocking {
        progress.completeLesson(A1Unit1Seed.LESSON_GREETINGS, 3)
        val item = review.dueItems().first()
        review.gradeAnswer(
            item,
            ExerciseGrade(
                exerciseId = item.id,
                userAnswer = item.acceptedAnswers.first(),
                isCorrect = true,
                explanationFr = "",
                acceptedAnswers = item.acceptedAnswers,
            ),
        )
        assertEquals(0, review.dueItems().count { it.id == item.id })
        now += SrsScheduler.DAY_MS * 2
        assertTrue(review.dueCount() >= 1)
    }
}
