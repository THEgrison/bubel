package fr.russki.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.russki.app.data.temporary.TemporaryContentSource
import fr.russki.app.domain.model.HomeContent
import fr.russki.app.domain.model.LoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val _state = MutableStateFlow<LoadState<HomeContent>>(LoadState.Loading)
    val state: StateFlow<LoadState<HomeContent>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = LoadState.Loading
            runCatching { TemporaryContentSource.loadHome() }
                .onSuccess { content ->
                    _state.value = when (content) {
                        is HomeContent.EmptyPath -> LoadState.Empty(
                            "Commencez par le module Alphabet pour ouvrir votre parcours.",
                        )
                        is HomeContent.Dashboard -> LoadState.Ready(content)
                    }
                }
                .onFailure { error ->
                    _state.value = LoadState.Error(
                        error.message ?: "Chargement de l'accueil impossible.",
                    )
                }
        }
    }
}
