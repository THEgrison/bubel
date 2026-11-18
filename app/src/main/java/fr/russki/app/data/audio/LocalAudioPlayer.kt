package fr.russki.app.data.audio

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.io.File

/**
 * Lecteur local minimal — pause / stop sur interruption de cycle de vie.
 * Ne lance jamais une lecture si le fichier / asset est absent.
 */
class LocalAudioPlayer(
    private val context: Context,
) : DefaultLifecycleObserver {

    private var player: MediaPlayer? = null

    @Volatile
    private var playingFlag: Boolean = false

    val isPlaying: Boolean get() = playingFlag

    fun playAsset(assetPath: String): Boolean {
        return runCatching {
            stopInternal()
            context.assets.openFd(assetPath).use { afd ->
                val mp = MediaPlayer()
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                mp.setOnCompletionListener {
                    playingFlag = false
                    releasePlayer()
                }
                mp.setOnErrorListener { _, _, _ ->
                    playingFlag = false
                    releasePlayer()
                    true
                }
                mp.prepare()
                mp.start()
                player = mp
                playingFlag = true
            }
            true
        }.getOrDefault(false)
    }

    fun playFile(file: File): Boolean {
        if (!file.exists() || !file.isFile) return false
        return runCatching {
            stopInternal()
            val mp = MediaPlayer()
            mp.setDataSource(file.absolutePath)
            mp.setOnCompletionListener {
                playingFlag = false
                releasePlayer()
            }
            mp.setOnErrorListener { _, _, _ ->
                playingFlag = false
                releasePlayer()
                true
            }
            mp.prepare()
            mp.start()
            player = mp
            playingFlag = true
            true
        }.getOrDefault(false)
    }

    fun pause() {
        runCatching {
            player?.takeIf { it.isPlaying }?.pause()
            playingFlag = false
        }
    }

    fun resume() {
        runCatching {
            player?.start()
            playingFlag = player?.isPlaying == true
        }
    }

    fun stop() = stopInternal()

    override fun onPause(owner: LifecycleOwner) {
        pause()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        release()
    }

    fun release() {
        stopInternal()
    }

    private fun stopInternal() {
        playingFlag = false
        releasePlayer()
    }

    private fun releasePlayer() {
        runCatching {
            player?.reset()
            player?.release()
        }
        player = null
    }
}
