package fr.russki.app.feature.packs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.russki.app.data.pack.PackCoordinator
import fr.russki.app.data.pack.PackStatusUi
import fr.russki.app.di.ServiceLocator
import fr.russki.app.domain.model.LoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PacksUiModel(
    val packs: List<PackStatusUi>,
    val remoteNote: String?,
    val onlineConfigured: Boolean,
)

class PacksViewModel(
    private val coordinator: PackCoordinator = ServiceLocator.require().packCoordinator,
    private val onlineConfigured: Boolean = ServiceLocator.require().onlineApi.isConfigured,
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<PacksUiModel>>(LoadState.Loading)
    val state: StateFlow<LoadState<PacksUiModel>> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = LoadState.Loading
            runCatching {
                coordinator.listPacksMerged()
            }.onSuccess { result ->
                _state.value = if (result.packs.isEmpty()) {
                    LoadState.Empty("Aucun pack catalogue.")
                } else {
                    LoadState.Ready(
                        PacksUiModel(
                            packs = result.packs,
                            remoteNote = result.remoteNote,
                            onlineConfigured = onlineConfigured,
                        ),
                    )
                }
            }.onFailure { error ->
                _state.value = LoadState.Error(
                    error.message ?: "Impossible de charger les packs.",
                )
            }
        }
    }

    fun install(packId: String) {
        viewModelScope.launch {
            val current = (_state.value as? LoadState.Ready)?.value?.packs
                ?.find { it.packId == packId }
            if (current == null) {
                _message.value = "Pack introuvable."
                return@launch
            }
            _message.value = coordinator.install(current)
            refresh()
        }
    }

    fun delete(packId: String) {
        viewModelScope.launch {
            val result = coordinator.delete(packId)
            _message.value = result.fold(
                onSuccess = { "Pack supprimé. Votre progression est intacte." },
                onFailure = { it.message ?: "Suppression impossible." },
            )
            refresh()
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
