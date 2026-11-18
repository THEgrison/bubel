package fr.russki.app.feature.audio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import fr.russki.app.core.ui.components.SecondaryButton
import fr.russki.app.data.audio.LocalAudioPlayer
import fr.russki.app.data.audio.PlaybackState
import fr.russki.app.domain.content.ResolvedAudio

/**
 * Contrôles lecture / pause / répétition pour un [ResolvedAudio] local.
 * Affiche « Audio indisponible » si [audio] est null — sans crash.
 * Une seule source à la fois dans ce contrôle ; le coordinateur de lecture
 * empêche les lectures concurrentes entre contrôles.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocalAudioControls(
    audio: ResolvedAudio?,
    unavailableMessage: String = "Audio indisponible",
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val player = remember { LocalAudioPlayer(context) }
    var playback by remember { mutableStateOf(PlaybackState.Idle) }

    DisposableEffect(player, lifecycleOwner) {
        val listener: (PlaybackState) -> Unit = { playback = it }
        player.addStateListener(listener)
        lifecycleOwner.lifecycle.addObserver(player)
        onDispose {
            player.removeStateListener(listener)
            lifecycleOwner.lifecycle.removeObserver(player)
            player.release()
        }
    }

    DisposableEffect(audio?.assetPath) {
        player.stop()
        onDispose { }
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

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when (playback) {
            PlaybackState.Playing -> {
                SecondaryButton(
                    text = "Pause",
                    onClick = { player.pause() },
                    compact = true,
                )
            }
            PlaybackState.Paused -> {
                SecondaryButton(
                    text = "Reprendre",
                    onClick = { player.resume() },
                    compact = true,
                )
            }
            else -> {
                SecondaryButton(
                    text = "Écouter",
                    onClick = { player.play(audio) },
                    compact = true,
                )
            }
        }
        TextButton(onClick = { player.replay() }) {
            Text("Répéter")
        }
        if (playback == PlaybackState.Playing || playback == PlaybackState.Paused) {
            TextButton(onClick = { player.stop() }) {
                Text("Arrêter")
            }
        }
    }
}
