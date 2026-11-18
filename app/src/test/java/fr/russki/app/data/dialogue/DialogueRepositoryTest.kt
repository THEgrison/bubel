package fr.russki.app.data.dialogue

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.russki.app.data.content.NoOpAudioAvailability
import fr.russki.app.data.content.seed.A1DialoguesSeed
import fr.russki.app.data.local.RusskiDatabase
import fr.russki.app.data.progress.LessonProgressRepository
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
class DialogueRepositoryTest {

    private lateinit var context: Context
    private lateinit var db: RusskiDatabase
    private lateinit var repository: DialogueRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = RusskiDatabase.createInMemory(context)
        repository = DialogueRepository(
            progressRepository = LessonProgressRepository(db.userLessonProgressDao()),
            audioAvailability = NoOpAudioAvailability,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun catalog_listsThreeDialoguesWithStableIds() = runBlocking {
        val catalog = repository.loadCatalog()
        assertEquals(3, catalog.size)
        assertEquals(
            listOf(A1DialoguesSeed.ID_MEET, A1DialoguesSeed.ID_DRINK, A1DialoguesSeed.ID_ASK),
            catalog.map { it.outline.id },
        )
        catalog.forEach { assertFalse(it.progress.isCompleted) }
    }

    @Test
    fun progress_persistsAcrossReload_andResetClearsOnlyThatDialogue() = runBlocking {
        val id = A1DialoguesSeed.ID_MEET
        val other = A1DialoguesSeed.ID_DRINK
        repository.markCompleted(id, totalActivities = 3)
        repository.recordActivityCompleted(other, totalActivities = 3)

        val after = repository.loadCatalog().associateBy { it.outline.id }
        assertTrue(after.getValue(id).progress.isCompleted)
        assertEquals(1, after.getValue(other).progress.completedActivityCount)

        // Simule un « redémarrage » : nouveau repository, même base
        val reloaded = DialogueRepository(
            progressRepository = LessonProgressRepository(db.userLessonProgressDao()),
            audioAvailability = NoOpAudioAvailability,
        )
        val again = reloaded.loadCatalog().associateBy { it.outline.id }
        assertTrue(again.getValue(id).progress.isCompleted)
        assertEquals(1, again.getValue(other).progress.completedActivityCount)

        reloaded.reset(id)
        val reset = reloaded.loadCatalog().associateBy { it.outline.id }
        assertFalse(reset.getValue(id).progress.isCompleted)
        assertEquals(0, reset.getValue(id).progress.completedActivityCount)
        assertEquals(1, reset.getValue(other).progress.completedActivityCount)
    }

    @Test
    fun missingAudio_resolvesToNull() {
        val script = repository.script(A1DialoguesSeed.ID_ASK)!!
        script.lines.forEach { line ->
            assertNull(repository.resolveAudio(line.audioAssetPath))
        }
        assertNull(repository.resolveAudio("audio/does-not-exist.ogg"))
        assertNull(repository.resolveAudio(null))
    }

    @Test
    fun lessonProgress_ofCurriculum_notWipedByDialogueReset() = runBlocking {
        val lessonProgress = LessonProgressRepository(db.userLessonProgressDao())
        lessonProgress.completeLesson("a1-lesson-alphabet", totalExercises = 3)
        repository.markCompleted(A1DialoguesSeed.ID_MEET, totalActivities = 3)
        repository.reset(A1DialoguesSeed.ID_MEET)

        assertTrue(lessonProgress.get("a1-lesson-alphabet")?.completedAtEpochMs != null)
        assertNull(lessonProgress.get(A1DialoguesSeed.ID_MEET)?.completedAtEpochMs)
    }
}
