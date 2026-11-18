package fr.russki.app.feature.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.russki.app.data.temporary.TemporaryContentSource
import fr.russki.app.domain.model.LearningModule
import fr.russki.app.domain.model.LoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LearnViewModel : ViewModel() {

    private val _state = MutableStateFlow<LoadState<List<LearningModule>>>(LoadState.Loading)
    val state: StateFlow<LoadState<List<LearningModule>>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = LoadState.Loading
            runCatching { TemporaryContentSource.loadModules() }
                .onSuccess { modules ->
                    _state.value = if (modules.isEmpty()) {
                        LoadState.Empty("Aucun module n'est encore disponible hors ligne.")
                    } else {
                        LoadState.Ready(modules)
                    }
                }
                .onFailure { error ->
                    _state.value = LoadState.Error(
                        error.message ?: "Chargement des modules impossible.",
                    )
                }
        }
    }
}
