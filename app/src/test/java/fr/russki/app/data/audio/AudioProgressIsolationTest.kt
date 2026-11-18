package fr.russki.app.data.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.russki.app.data.content.AssetAudioAvailability
import fr.russki.app.data.content.ContentSeedInitializer
import fr.russki.app.data.content.seed.A1DialoguesSeed
import fr.russki.app.data.content.seed.A1Unit1Seed
import fr.russki.app.data.local.RusskiDatabase
import fr.russki.app.data.progress.LessonProgressRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AudioProgressIsolationTest {

    private lateinit var context: Context
    private lateinit var db: RusskiDatabase
    private lateinit var progress: LessonProgressRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = RusskiDatabase.createInMemory(context)
        progress = LessonProgressRepository(db.userLessonProgressDao())
        runBlocking { ContentSeedInitializer(db).ensureSeeded() }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun resolvingAudioAndVoiceCache_doNotMutateLessonOrDialogueProgress() = runBlocking {
        progress.completeLesson(A1Unit1Seed.LESSON_GREETINGS, 3)
        progress.completeLesson(A1DialoguesSeed.ID_MEET, 3)
        val before = progress.getAll().associate { it.lessonId to it.completedAtEpochMs }

        val resolver = AudioResolver(AssetAudioAvailability(context))
        A1DialoguesSeed.byId(A1DialoguesSeed.ID_MEET)!!.lines.forEach { line ->
            resolver.resolveDialogueLine(line.id, line.audioAssetPath)
        }
        resolver.resolveVocabulary("vocab-greet-001")

        val recorder = LocalVoiceRecorder(context)
        recorder.startRecording()
        recorder.clearSession()

        val after = progress.getAll().associate { it.lessonId to it.completedAtEpochMs }
        assertEquals(before, after)
    }
}
