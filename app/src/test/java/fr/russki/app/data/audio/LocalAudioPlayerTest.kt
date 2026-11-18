package fr.russki.app.data.audio

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalAudioPlayerTest {

    @Test
    fun missingAsset_doesNotCrash_returnsFalse() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val player = LocalAudioPlayer(context)
        assertFalse(player.playAsset("audio/does-not-exist.ogg"))
        assertFalse(player.isPlaying)
        player.release()
    }

    @Test
    fun missingFile_doesNotCrash_returnsFalse() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val player = LocalAudioPlayer(context)
        val missing = File(context.filesDir, "packs/missing/nope.ogg")
        assertFalse(player.playFile(missing))
        assertFalse(player.isPlaying)
        player.pause()
        player.resume()
        player.stop()
        player.release()
    }
}
