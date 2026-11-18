package fr.russki.app.feature.oral

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.russki.app.core.ui.components.CyrillicSample
import fr.russki.app.core.ui.components.ErrorState
import fr.russki.app.core.ui.components.FadeInContent
import fr.russki.app.core.ui.components.LoadingState
import fr.russki.app.core.ui.components.MetaChip
import fr.russki.app.core.ui.components.PrimaryButton
import fr.russki.app.core.ui.components.ScreenScaffold
import fr.russki.app.core.ui.components.SecondaryButton
import fr.russki.app.core.ui.components.SectionLabel
import fr.russki.app.core.ui.components.SoftPanel
import fr.russki.app.core.ui.components.SpacerH
import fr.russki.app.data.audio.LocalVoiceRecorder
import fr.russki.app.domain.model.LoadState
import fr.russki.app.feature.audio.LocalAudioControls
import fr.russki.app.feature.stats.TrackStudyTime

@Composable
fun OralPracticeRoute(
    onBack: () -> Unit,
    viewModel: OralPracticeViewModel =
        viewModel(factory = OralPracticeViewModel.Factory()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TrackStudyTime()
    Column(Modifier.fillMaxSize()) {
        IconButton(onClick = {
            viewModel.clearRecordings()
            onBack()
        }) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
        }
        OralPracticeScreen(
            state = state,
            recorder = viewModel.voiceRecorder,
            onRetry = viewModel::refresh,
            onNext = viewModel::nextPrompt,
            onPrev = viewModel::prevPrompt,
        )
    }
}

@Composable
fun OralPracticeScreen(
    state: LoadState<OralPracticeUi>,
    recorder: LocalVoiceRecorder,
    onRetry: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
) {
    ScreenScaffold(
        title = "Entraînement oral",
        subtitle = "Répétez à voix haute — sans notation automatique ni envoi serveur.",
    ) {
        when (state) {
            LoadState.Loading -> LoadingState()
            is LoadState.Error -> ErrorState(message = state.message, onRetry = onRetry)
            is LoadState.Empty -> ErrorState(message = state.message, onRetry = onRetry)
            is LoadState.Ready -> FadeInContent(visible = true) {
                OralContent(
                    ui = state.value,
                    recorder = recorder,
                    onNext = onNext,
                    onPrev = onPrev,
                )
            }
        }
    }
}

@Composable
private fun OralContent(
    ui: OralPracticeUi,
    recorder: LocalVoiceRecorder,
    onNext: () -> Unit,
    onPrev: () -> Unit,
) {
    val context = LocalContext.current
    var micState by remember { mutableStateOf(recorder.state) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(recorder) {
        onDispose { recorder.clearSession() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            statusMessage = null
            val ok = recorder.startRecording()
            micState = recorder.state
            if (!ok) statusMessage = recorder.lastError ?: "Enregistrement impossible."
        } else {
            statusMessage = "Permission microphone refusée — l’entraînement texte reste disponible."
            micState = LocalVoiceRecorder.State.Error
        }
    }

    fun refreshMic() {
        micState = recorder.state
        statusMessage = recorder.lastError ?: statusMessage
    }

    val prompt = ui.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SoftPanel {
            SectionLabel("Consigne")
            Text(
                text = "Lisez la phrase, écoutez le modèle s’il est disponible, " +
                    "puis enregistrez-vous pour vous réécouter. " +
                    "Aucune note de prononciation n’est calculée.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SoftPanel {
            MetaChip("${ui.index + 1} / ${ui.prompts.size}")
            SpacerH(8)
            CyrillicSample(prompt.textRu)
            SpacerH(8)
            Text(prompt.textFr, style = MaterialTheme.typography.bodyLarge)
            SpacerH(12)
            LocalAudioControls(
                audio = prompt.modelAudio,
                unavailableMessage = "Audio modèle indisponible — utilisez le texte.",
            )
        }
        SoftPanel {
            SectionLabel("Microphone")
            Text(
                text = micLabel(micState),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            statusMessage?.let {
                SpacerH(6)
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            SpacerH(12)
            when (micState) {
                LocalVoiceRecorder.State.Recording -> {
                    PrimaryButton(
                        text = "Arrêter l'enregistrement",
                        onClick = {
                            recorder.stopRecording()
                            refreshMic()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                LocalVoiceRecorder.State.Recorded,
                LocalVoiceRecorder.State.PlayingBack,
                -> {
                    SecondaryButton(
                        text = "Réécouter mon enregistrement",
                        onClick = {
                            recorder.playRecording()
                            refreshMic()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SpacerH(8)
                    SecondaryButton(
                        text = "Supprimer et recommencer",
                        onClick = {
                            recorder.deleteRecording()
                            statusMessage = null
                            refreshMic()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> {
                    PrimaryButton(
                        text = "Enregistrer ma voix",
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                val ok = recorder.startRecording()
                                refreshMic()
                                if (!ok) {
                                    statusMessage =
                                        recorder.lastError ?: "Enregistrement impossible."
                                }
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        SecondaryButton(
            text = "Phrase précédente",
            onClick = {
                recorder.deleteRecording()
                statusMessage = null
                refreshMic()
                onPrev()
            },
            enabled = ui.index > 0,
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton(
            text = "Phrase suivante",
            onClick = {
                recorder.deleteRecording()
                statusMessage = null
                refreshMic()
                onNext()
            },
            enabled = ui.index < ui.prompts.lastIndex,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(
            onClick = {
                recorder.clearSession()
                statusMessage = "Enregistrements temporaires supprimés."
                refreshMic()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Effacer le cache oral") }
        SpacerH(8)
    }
}

private fun micLabel(state: LocalVoiceRecorder.State): String = when (state) {
    LocalVoiceRecorder.State.Idle -> "Micro prêt (inactif)"
    LocalVoiceRecorder.State.Recording -> "Enregistrement en cours…"
    LocalVoiceRecorder.State.Recorded -> "Enregistrement prêt à réécouter"
    LocalVoiceRecorder.State.PlayingBack -> "Lecture de votre enregistrement…"
    LocalVoiceRecorder.State.Error -> "Micro / enregistrement en erreur"
}
