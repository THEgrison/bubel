package fr.russki.app.data.audio

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import fr.russki.app.domain.content.ResolvedAudio
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Lecteur local minimal — assets embarqués ou fichiers sous filesDir.
 * Ne lance jamais une lecture si la ressource est absente.
 * Libérer via [release] / cycle de vie (onDestroy).
 */
class LocalAudioPlayer(
    private val context: Context,
) : DefaultLifecycleObserver {

    private var player: MediaPlayer? = null

    @Volatile
    private var playingFlag: Boolean = false

    @Volatile
    private var state: PlaybackState = PlaybackState.Idle

    private var lastAssetPath: String? = null
    private var lastFile: File? = null

    private val listeners = CopyOnWriteArrayList<(PlaybackState) -> Unit>()

    val isPlaying: Boolean get() = playingFlag

    val playbackState: PlaybackState get() = state

    fun addStateListener(listener: (PlaybackState) -> Unit) {
        listeners += listener
        listener(state)
    }

    fun removeStateListener(listener: (PlaybackState) -> Unit) {
        listeners -= listener
    }

    fun play(resolved: ResolvedAudio): Boolean {
        val path = resolved.assetPath
        return if (path.startsWith("/")) {
            playFile(File(path))
        } else {
            playAsset(path)
        }
    }

    fun playAsset(assetPath: String): Boolean {
        return runCatching {
            stopInternal(resetLast = false)
            context.assets.openFd(assetPath).use { afd ->
                val mp = MediaPlayer()
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                wireCallbacks(mp)
                mp.prepare()
                mp.start()
                player = mp
                lastAssetPath = assetPath
                lastFile = null
                setState(PlaybackState.Playing)
            }
            true
        }.getOrElse {
            setState(PlaybackState.Error)
            false
        }
    }

    fun playFile(file: File): Boolean {
        if (!file.exists() || !file.isFile) {
            setState(PlaybackState.Error)
            return false
        }
        return runCatching {
            stopInternal(resetLast = false)
            val mp = MediaPlayer()
            mp.setDataSource(file.absolutePath)
            wireCallbacks(mp)
            mp.prepare()
            mp.start()
            player = mp
            lastFile = file
            lastAssetPath = null
            setState(PlaybackState.Playing)
            true
        }.getOrElse {
            setState(PlaybackState.Error)
            false
        }
    }

    /** Relance la dernière source réussie (répétition). */
    fun replay(): Boolean {
        lastAssetPath?.let { return playAsset(it) }
        lastFile?.let { return playFile(it) }
        return false
    }

    fun pause() {
        runCatching {
            player?.takeIf { it.isPlaying }?.pause()
            setState(PlaybackState.Paused)
        }
    }

    fun resume() {
        runCatching {
            player?.start()
            if (player?.isPlaying == true) {
                setState(PlaybackState.Playing)
            }
        }
    }

    fun stop() = stopInternal(resetLast = false)

    override fun onPause(owner: LifecycleOwner) {
        pause()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        release()
    }

    fun release() {
        stopInternal(resetLast = true)
        setState(PlaybackState.Idle)
    }

    private fun wireCallbacks(mp: MediaPlayer) {
        mp.setOnCompletionListener {
            setState(PlaybackState.Idle)
            releasePlayer()
        }
        mp.setOnErrorListener { _, _, _ ->
            setState(PlaybackState.Error)
            releasePlayer()
            true
        }
    }

    private fun stopInternal(resetLast: Boolean) {
        releasePlayer()
        if (resetLast) {
            lastAssetPath = null
            lastFile = null
        }
        if (state != PlaybackState.Error) {
            setState(PlaybackState.Idle)
        }
    }

    private fun releasePlayer() {
        playingFlag = false
        runCatching {
            player?.reset()
            player?.release()
        }
        player = null
    }

    private fun setState(next: PlaybackState) {
        state = next
        playingFlag = next == PlaybackState.Playing
        listeners.forEach { it(next) }
    }
}
