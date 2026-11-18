package fr.russki.app.feature.dialogue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.russki.app.data.dialogue.DialogueCatalogItem
import fr.russki.app.data.dialogue.DialogueRepository
import fr.russki.app.di.ServiceLocator
import fr.russki.app.domain.model.LoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DialogueCatalogViewModel(
    private val repository: DialogueRepository =
        ServiceLocator.require().dialogueRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<List<DialogueCatalogItem>>>(LoadState.Loading)
    val state: StateFlow<LoadState<List<DialogueCatalogItem>>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = LoadState.Loading
            runCatching { repository.loadCatalog() }
                .onSuccess { items ->
                    _state.value = if (items.isEmpty()) {
                        LoadState.Empty("Aucun dialogue disponible.")
                    } else {
                        LoadState.Ready(items)
                    }
                }
                .onFailure {
                    _state.value = LoadState.Error(
                        it.message ?: "Chargement des dialogues impossible.",
                    )
                }
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DialogueCatalogViewModel() as T
    }
}
