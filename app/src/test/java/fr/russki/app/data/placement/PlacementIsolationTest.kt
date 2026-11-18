package fr.russki.app.data.placement

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.russki.app.data.content.ContentSeedInitializer
import fr.russki.app.data.content.seed.A1DialoguesSeed
import fr.russki.app.data.content.seed.A1Unit1Seed
import fr.russki.app.data.curriculum.CurriculumRepository
import fr.russki.app.data.local.RusskiDatabase
import fr.russki.app.data.progress.LessonProgressRepository
import fr.russki.app.data.progress.ProgressContentIds
import fr.russki.app.domain.exercise.ExerciseGrade
import fr.russki.app.domain.placement.PlacementIds
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlacementIsolationTest {

    private lateinit var context: Context
    private lateinit var db: RusskiDatabase
    private lateinit var progress: LessonProgressRepository
    private lateinit var curriculum: CurriculumRepository
    private lateinit var store: LocalPlacementStore
    private lateinit var repository: PlacementRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(LocalPlacementStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().commit()
        db = RusskiDatabase.createInMemory(context)
        progress = LessonProgressRepository(db.userLessonProgressDao())
        curriculum = CurriculumRepository(db, progress) {
            ContentSeedInitializer(db).ensureSeeded()
        }
        store = LocalPlacementStore(context)
        repository = PlacementRepository(curriculum, progress, store)
        runBlocking { ContentSeedInitializer(db).ensureSeeded() }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun completeAndPersist_doesNotTouchLessonOrDialogueProgress() = runBlocking {
        progress.completeLesson(A1Unit1Seed.LESSON_GREETINGS, 3)
        progress.completeLesson(A1DialoguesSeed.ID_MEET, 3)
        val before = progress.getAll()
        assertEquals(2, before.size)

        val questions = repository.loadQuestions()
        assertTrue(questions.isNotEmpty())
        assertTrue(questions.all { it.id.startsWith(PlacementIds.QUESTION_PREFIX) })

        val grades = questions.associate { q ->
            q.id to ExerciseGrade(
                exerciseId = q.id,
                userAnswer = q.exercise.acceptedAnswers.first(),
                isCorrect = true,
                explanationFr = q.exercise.explanationFr,
                acceptedAnswers = q.exercise.acceptedAnswers,
            )
        }
        repository.completeAndPersist(questions, grades, completedAtEpochMs = 99L)

        val after = progress.getAll()
        assertEquals(before.map { it.lessonId }.toSet(), after.map { it.lessonId }.toSet())
        assertEquals(before.size, after.size)
        assertFalse(after.any { it.lessonId.startsWith(PlacementIds.QUESTION_PREFIX) })
        assertTrue(after.any { ProgressContentIds.isDialogue(it.lessonId) })
        assertTrue(after.any { ProgressContentIds.isCurriculumLesson(it.lessonId) })

        val stored = repository.loadStored()
        assertEquals(99L, stored.latest!!.completedAtEpochMs)
        assertEquals(questions.size, stored.latest!!.correctCount)
    }
}
