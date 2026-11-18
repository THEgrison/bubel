package fr.russki.app.data.audio

import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests du coordinateur — sans dépendre d'un MediaPlayer Robolectric fiable
 * pour Ogg (lecture réelle = checklist appareil).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalPlaybackCoordinatorTest {

    @Before
    fun setUp() {
        LocalPlaybackCoordinator.clearForTests()
    }

    @After
    fun tearDown() {
        LocalPlaybackCoordinator.stopAll()
        LocalPlaybackCoordinator.clearForTests()
    }

    @Test
    fun register_and_unregister_onRelease() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val player = LocalAudioPlayer(context)
        assertTrue(LocalPlaybackCoordinator.registeredCount() >= 1)
        player.release()
        assertEquals(0, LocalPlaybackCoordinator.registeredCount())
    }

    @Test
    fun requestExclusive_doesNotCrash_andLeavesPeersIdle() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val a = LocalAudioPlayer(context)
        val b = LocalAudioPlayer(context)
        assertEquals(2, LocalPlaybackCoordinator.registeredCount())

        // Aucune piste chargée : handoff doit rester sans faux état Paused.
        LocalPlaybackCoordinator.requestExclusive(b)
        assertEquals(PlaybackState.Idle, a.playbackState)
        assertEquals(PlaybackState.Idle, b.playbackState)
        assertFalse(a.isPlaying)
        assertFalse(b.isPlaying)

        a.release()
        b.release()
    }

    @Test
    fun pauseWithoutPlayer_staysIdle_notResumeUi() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val player = LocalAudioPlayer(context)
        player.pause()
        assertEquals(PlaybackState.Idle, player.playbackState)
        player.release()
    }

    @Test
    fun stopForHandoff_whenIdle_isNoOp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val player = LocalAudioPlayer(context)
        player.stopForHandoff()
        assertEquals(PlaybackState.Idle, player.playbackState)
        player.release()
    }

    @Test
    fun stopAll_safeWithMultiplePlayers() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val players = List(3) { LocalAudioPlayer(context) }
        LocalPlaybackCoordinator.stopAll()
        players.forEach {
            assertFalse(it.isPlaying)
            assertEquals(PlaybackState.Idle, it.playbackState)
            it.release()
        }
    }
}
