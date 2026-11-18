package fr.russki.app.feature.vocabulary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.russki.app.core.ui.components.CyrillicSample
import fr.russki.app.core.ui.components.EmptyState
import fr.russki.app.core.ui.components.ErrorState
import fr.russki.app.core.ui.components.FadeInContent
import fr.russki.app.core.ui.components.LoadingState
import fr.russki.app.core.ui.components.MetaChip
import fr.russki.app.core.ui.components.ScreenScaffold
import fr.russki.app.core.ui.components.SoftPanel
import fr.russki.app.core.ui.components.SpacerH
import fr.russki.app.data.content.OfflineContentRepository
import fr.russki.app.data.content.seed.GreetingsVocabularySeed
import fr.russki.app.domain.content.ResolvedAudio
import fr.russki.app.domain.content.VocabularyEntry
import fr.russki.app.domain.content.VocabularyModule
import fr.russki.app.domain.model.LoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VocabularyUi(
    val module: VocabularyModule,
    val audios: Map<String, ResolvedAudio?>,
)

class VocabularyViewModel(
    private val moduleId: String,
    private val repository: OfflineContentRepository = OfflineContentRepository.get(),
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<VocabularyUi>>(LoadState.Loading)
    val state: StateFlow<LoadState<VocabularyUi>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = LoadState.Loading
            val module = repository.vocabularyModule(moduleId)
            _state.value = if (module == null) {
                LoadState.Empty("Module de vocabulaire introuvable.")
            } else {
                val audios = module.entries.associate { entry ->
                    entry.id to repository.resolveAudio(entry.audioAssetPath)
                }
                LoadState.Ready(VocabularyUi(module = module, audios = audios))
            }
        }
    }

    class Factory(
        private val moduleId: String,
        private val repository: OfflineContentRepository = OfflineContentRepository.get(),
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VocabularyViewModel(moduleId, repository) as T
        }
    }
}

@Composable
fun GreetingsVocabularyRoute(
    onBack: () -> Unit,
    viewModel: VocabularyViewModel = viewModel(
        factory = VocabularyViewModel.Factory(GreetingsVocabularySeed.MODULE_ID),
    ),
) {
    LaunchedEffect(Unit) { viewModel.refresh() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    VocabularyScreen(state = state, onBack = onBack, onRetry = viewModel::refresh)
}

@Composable
fun VocabularyScreen(
    state: LoadState<VocabularyUi>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
        }
        when (state) {
            LoadState.Loading -> LoadingState()
            is LoadState.Error -> ErrorState(message = state.message, onRetry = onRetry)
            is LoadState.Empty -> EmptyState(title = "Module absent", body = state.message)
            is LoadState.Ready -> FadeInContent(visible = true) {
                val ui = state.value
                ScreenScaffold(
                    title = ui.module.titleFr,
                    subtitle = "${ui.module.category} · ${ui.module.level.name} · hors ligne",
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SoftPanel {
                            Text(ui.module.descriptionFr, style = MaterialTheme.typography.bodyLarge)
                            SpacerH(8)
                            MetaChip("${ui.module.entries.size} expressions")
                        }
                        ui.module.entries.forEach { entry ->
                            VocabCard(
                                entry = entry,
                                hasAudio = ui.audios[entry.id] != null,
                            )
                        }
                        Text(
                            text = "Aucune de ces entrées n'embarque encore d'enregistrement de locuteur natif.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SpacerH(8)
                    }
                }
            }
        }
    }
}

@Composable
private fun VocabCard(entry: VocabularyEntry, hasAudio: Boolean) {
    SoftPanel {
        CyrillicSample(entry.textRu)
        SpacerH(6)
        Text(entry.translationFr, style = MaterialTheme.typography.bodyLarge)
        SpacerH(8)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetaChip(entry.category)
            MetaChip(entry.level.name)
        }
        if (entry.exampleRu != null) {
            SpacerH(10)
            Text(
                text = entry.exampleRu,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (entry.exampleFr != null) {
                SpacerH(4)
                Text(
                    text = entry.exampleFr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SpacerH(10)
        if (hasAudio) {
            MetaChip("Audio natif disponible")
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Outlined.VolumeOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "  Audio non disponible",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
