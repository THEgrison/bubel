package fr.russki.app.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

/**
 * Enregistrement vocal local optionnel — cache privé uniquement.
 * Aucune reconnaissance, aucun envoi réseau, suppression explicite via [deleteRecording]
 * / [clearSession].
 */
class LocalVoiceRecorder(
    private val context: Context,
) {

    enum class State {
        Idle,
        Recording,
        Recorded,
        PlayingBack,
        Error,
    }

    private var recorder: MediaRecorder? = null
    private var playbackPlayer: LocalAudioPlayer? = null
    private var outputFile: File? = null

    @Volatile
    var state: State = State.Idle
        private set

    @Volatile
    var lastError: String? = null
        private set

    fun currentFile(): File? = outputFile?.takeIf { it.exists() && it.length() > 0L }

    /**
     * Démarre un enregistrement dans le cache privé de l'app.
     * @throws SecurityException si la permission micro n'est pas accordée (côté UI).
     */
    fun startRecording(): Boolean {
        stopPlaybackOnly()
        deleteRecording()
        lastError = null
        val dir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
        val file = File(dir, "oral-${System.currentTimeMillis()}.m4a")
        return try {
            val mr = createRecorder()
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(96_000)
            mr.setAudioSamplingRate(44_100)
            mr.setOutputFile(file.absolutePath)
            mr.prepare()
            mr.start()
            recorder = mr
            outputFile = file
            state = State.Recording
            true
        } catch (se: SecurityException) {
            lastError = "Permission microphone refusée."
            state = State.Error
            cleanupFailedFile(file)
            false
        } catch (io: IOException) {
            lastError = io.message ?: "Erreur d'enregistrement."
            state = State.Error
            cleanupFailedFile(file)
            false
        } catch (t: Throwable) {
            lastError = t.message ?: "Erreur d'enregistrement."
            state = State.Error
            cleanupFailedFile(file)
            false
        }
    }

    fun stopRecording(): Boolean {
        val mr = recorder ?: return false
        return try {
            mr.stop()
            true
        } catch (t: Throwable) {
            lastError = t.message ?: "Arrêt de l'enregistrement impossible."
            state = State.Error
            false
        } finally {
            runCatching { mr.reset(); mr.release() }
            recorder = null
            val file = outputFile
            state = if (file != null && file.exists() && file.length() > 0L) {
                State.Recorded
            } else {
                State.Error
            }
        }
    }

    fun playRecording(player: LocalAudioPlayer = playbackPlayer ?: LocalAudioPlayer(context).also {
        playbackPlayer = it
    }): Boolean {
        val file = currentFile() ?: return false
        val ok = player.playFile(file)
        if (ok) state = State.PlayingBack
        return ok
    }

    fun stopPlayback() {
        stopPlaybackOnly()
        if (currentFile() != null) state = State.Recorded else state = State.Idle
    }

    fun deleteRecording() {
        stopPlaybackOnly()
        runCatching { recorder?.reset(); recorder?.release() }
        recorder = null
        outputFile?.let { runCatching { if (it.exists()) it.delete() } }
        outputFile = null
        if (state != State.Error || lastError == null) {
            state = State.Idle
        }
    }

    /** Nettoie toute la session (fichiers temporaires du cache oral). */
    fun clearSession() {
        deleteRecording()
        lastError = null
        state = State.Idle
        val dir = File(context.cacheDir, CACHE_DIR)
        dir.listFiles()?.forEach { runCatching { it.delete() } }
    }

    fun release() {
        clearSession()
        playbackPlayer?.release()
        playbackPlayer = null
    }

    private fun stopPlaybackOnly() {
        playbackPlayer?.stop()
    }

    private fun cleanupFailedFile(file: File) {
        runCatching { if (file.exists()) file.delete() }
        outputFile = null
        runCatching { recorder?.reset(); recorder?.release() }
        recorder = null
    }

    private fun createRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

    companion object {
        const val CACHE_DIR: String = "oral_recordings"
    }
}
