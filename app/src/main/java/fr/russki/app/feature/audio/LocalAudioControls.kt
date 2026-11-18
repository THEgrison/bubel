package fr.russki.app.feature.audio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fr.russki.app.data.audio.LocalAudioPlayer
import fr.russki.app.data.audio.PlaybackState
import fr.russki.app.domain.content.ResolvedAudio

/**
 * Contrôles lecture / pause / répétition pour un [ResolvedAudio] local.
 * Affiche « Audio indisponible » si [audio] est null — sans crash.
 */
@Composable
fun LocalAudioControls(
    audio: ResolvedAudio?,
    unavailableMessage: String = "Audio indisponible",
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val player = remember { LocalAudioPlayer(context) }
    var playback by remember { mutableStateOf(PlaybackState.Idle) }

    DisposableEffect(player) {
        val listener: (PlaybackState) -> Unit = { playback = it }
        player.addStateListener(listener)
        onDispose {
            player.removeStateListener(listener)
            player.release()
        }
    }

    if (audio == null) {
        Text(
            text = unavailableMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (playback) {
            PlaybackState.Playing -> {
                OutlinedButton(onClick = { player.pause() }) { Text("Pause") }
            }
            PlaybackState.Paused -> {
                OutlinedButton(onClick = { player.resume() }) { Text("Reprendre") }
            }
            else -> {
                OutlinedButton(onClick = { player.play(audio) }) { Text("Écouter") }
            }
        }
        TextButton(onClick = { player.replay() }) {
            Text("Répéter")
        }
    }
}
