package fr.russki.app.feature.packs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.russki.app.data.pack.PackRepository
import fr.russki.app.data.pack.PackStatusUi
import fr.russki.app.di.ServiceLocator
import fr.russki.app.domain.model.LoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PacksViewModel(
    private val packRepository: PackRepository = ServiceLocator.require().packRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<List<PackStatusUi>>>(LoadState.Loading)
    val state: StateFlow<LoadState<List<PackStatusUi>>> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = LoadState.Loading
            runCatching { packRepository.listPacks() }
                .onSuccess { packs ->
                    _state.value = if (packs.isEmpty()) {
                        LoadState.Empty("Aucun pack catalogue.")
                    } else {
                        LoadState.Ready(packs)
                    }
                }
                .onFailure { error ->
                    _state.value = LoadState.Error(
                        error.message ?: "Impossible de charger les packs.",
                    )
                }
        }
    }

    fun install(packId: String) {
        viewModelScope.launch {
            val result = packRepository.installOptionalPack(packId)
            _message.value = result.fold(
                onSuccess = { "Pack installé hors ligne." },
                onFailure = { it.message ?: "Installation impossible." },
            )
            refresh()
        }
    }

    fun delete(packId: String) {
        viewModelScope.launch {
            val result = packRepository.deleteOptionalPack(packId)
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
