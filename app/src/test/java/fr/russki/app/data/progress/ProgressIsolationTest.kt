package fr.russki.app.data.progress

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.russki.app.data.content.ContentSeedInitializer
import fr.russki.app.data.content.NoOpAudioAvailability
import fr.russki.app.data.content.seed.A1DialoguesSeed
import fr.russki.app.data.content.seed.A1Unit1Seed
import fr.russki.app.data.curriculum.CurriculumRepository
import fr.russki.app.data.dialogue.DialogueRepository
import fr.russki.app.data.local.RusskiDatabase
import fr.russki.app.data.online.api.DisabledOnlineApi
import fr.russki.app.data.online.sync.ProgressSyncGateway
import fr.russki.app.data.online.sync.SyncPreferences
import fr.russki.app.domain.dialogue.DialogueEngine
import fr.russki.app.domain.dialogue.DialoguePhase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressIsolationTest {

    private lateinit var context: Context
    private lateinit var db: RusskiDatabase
    private lateinit var progress: LessonProgressRepository
    private lateinit var curriculum: CurriculumRepository
    private lateinit var dialogues: DialogueRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = RusskiDatabase.createInMemory(context)
        progress = LessonProgressRepository(db.userLessonProgressDao())
        curriculum = CurriculumRepository(db, progress) {
            ContentSeedInitializer(db).ensureSeeded()
        }
        dialogues = DialogueRepository(progress, NoOpAudioAvailability)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun ids_dialoguesDistinctFromCurriculumLessons() {
        val lessonIds = A1Unit1Seed.unit.lessons.map { it.id }.toSet()
        val dialogueIds = A1DialoguesSeed.all.map { it.outline.id }.toSet()
        assertTrue(lessonIds.none { ProgressContentIds.isDialogue(it) })
        assertTrue(dialogueIds.all { ProgressContentIds.isDialogue(it) })
        assertTrue(lessonIds.intersect(dialogueIds).isEmpty())
    }

    @Test
    fun learnPath_ignoresDialogueProgressRows() = runBlocking {
        dialogues.markCompleted(A1DialoguesSeed.ID_MEET, totalActivities = 3)
        progress.completeLesson(A1Unit1Seed.LESSON_GREETINGS, totalExercises = 3)

        val path = curriculum.loadA1Path()
        assertFalse(path.lessonProgress.keys.any { ProgressContentIds.isDialogue(it) })
        assertEquals(3, path.unit.lessons.size)
        assertTrue(path.lessonProgress.getValue(A1Unit1Seed.LESSON_GREETINGS).isCompleted)
        // Dialogue terminé n'apparaît pas comme leçon
        assertNull(path.lessonProgress[A1DialoguesSeed.ID_MEET])
    }

    @Test
    fun resetDialogue_doesNotEraseLessonProgress() = runBlocking {
        progress.completeLesson(A1Unit1Seed.LESSON_ALPHABET, totalExercises = 3)
        dialogues.markCompleted(A1DialoguesSeed.ID_DRINK, totalActivities = 3)
        dialogues.reset(A1DialoguesSeed.ID_DRINK)

        assertTrue(progress.get(A1Unit1Seed.LESSON_ALPHABET)?.completedAtEpochMs != null)
        assertNull(progress.get(A1DialoguesSeed.ID_DRINK)?.completedAtEpochMs)
        assertEquals(0, progress.get(A1DialoguesSeed.ID_DRINK)?.completedExerciseCount)
    }

    @Test
    fun syncSnapshot_excludesDialogueIdsFromLessonProgress() = runBlocking {
        progress.completeLesson(A1Unit1Seed.LESSON_INTRODUCE, totalExercises = 3)
        dialogues.markCompleted(A1DialoguesSeed.ID_ASK, totalActivities = 3)

        val gateway = ProgressSyncGateway(
            api = DisabledOnlineApi(),
            db = db,
            preferences = SyncPreferences(),
        )
        val snapshot = gateway.buildSnapshot()
        assertTrue(snapshot.lessonProgress.any { it.lessonId == A1Unit1Seed.LESSON_INTRODUCE })
        assertTrue(snapshot.lessonProgress.none { ProgressContentIds.isDialogue(it.lessonId) })
    }

    @Test
    fun dialogue_endToEndOfflineWithoutAudio() {
        val script = A1DialoguesSeed.byId(A1DialoguesSeed.ID_ASK)!!
        assertEquals("Demander son chemin", script.outline.titleFr)
        assertTrue(script.lines.all { it.audioAssetPath == null })
        assertNull(dialogues.resolveAudio(null))

        val engine = DialogueEngine()
        var session = engine.start(script)
        session = engine.begin(session)
        while (session.phase != DialoguePhase.Summary) {
            session = when (session.phase) {
                DialoguePhase.Line -> engine.advanceFromLine(session)
                DialoguePhase.Activity -> {
                    val answer = session.currentActivity!!.acceptedAnswers.first()
                    var s = engine.submitAnswer(session, answer)
                    assertTrue(s.currentGrade!!.isCorrect)
                    engine.advanceAfterFeedback(s)
                }
                else -> error("phase: ${session.phase}")
            }
        }
        session = engine.complete(session)
        assertEquals(DialoguePhase.Completed, session.phase)
        assertEquals(script.activities.size, session.correctCount)
    }

    @Test
    fun requireDialogueId_rejectsLessonId() {
        try {
            ProgressContentIds.requireDialogueId(A1Unit1Seed.LESSON_ALPHABET)
            throw AssertionError("attendu IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // ok
        }
        try {
            ProgressContentIds.requireCurriculumLessonId(A1DialoguesSeed.ID_MEET)
            throw AssertionError("attendu IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // ok
        }
    }
}
