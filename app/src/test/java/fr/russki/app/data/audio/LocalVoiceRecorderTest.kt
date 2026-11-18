package fr.russki.app.data.audio

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalVoiceRecorderTest {

    private lateinit var recorder: LocalVoiceRecorder
    private lateinit var cacheDir: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        recorder = LocalVoiceRecorder(context)
        cacheDir = File(context.cacheDir, LocalVoiceRecorder.CACHE_DIR)
        recorder.clearSession()
    }

    @Test
    fun clearSession_removesTemporaryFiles() {
        cacheDir.mkdirs()
        val leftover = File(cacheDir, "oral-leftover.m4a")
        leftover.writeText("fake")
        assertTrue(leftover.exists())
        recorder.clearSession()
        assertFalse(leftover.exists())
        assertEquals(LocalVoiceRecorder.State.Idle, recorder.state)
        assertNull(recorder.currentFile())
    }

    @Test
    fun startRecording_withoutMic_setsErrorOrFailsGracefully() {
        // Sur Robolectric, MediaRecorder échoue souvent sans hardware micro.
        val started = recorder.startRecording()
        if (started) {
            recorder.stopRecording()
            recorder.deleteRecording()
        } else {
            assertEquals(LocalVoiceRecorder.State.Error, recorder.state)
            assertTrue(recorder.lastError != null)
        }
        recorder.clearSession()
        assertEquals(LocalVoiceRecorder.State.Idle, recorder.state)
    }

    @Test
    fun deleteRecording_idempotent() {
        recorder.deleteRecording()
        recorder.deleteRecording()
        assertNull(recorder.currentFile())
    }
}
