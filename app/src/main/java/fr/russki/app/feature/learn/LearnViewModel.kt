package fr.russki.app.feature.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.russki.app.data.curriculum.CurriculumRepository
import fr.russki.app.data.curriculum.PathWithProgress
import fr.russki.app.di.ServiceLocator
import fr.russki.app.domain.model.LoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LearnViewModel(
    private val curriculumRepository: CurriculumRepository =
        ServiceLocator.require().curriculumRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<PathWithProgress>>(LoadState.Loading)
    val state: StateFlow<LoadState<PathWithProgress>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = LoadState.Loading
            runCatching { curriculumRepository.loadA1Path() }
                .onSuccess { path ->
                    _state.value = LoadState.Ready(path)
                }
                .onFailure { error ->
                    _state.value = LoadState.Error(
                        error.message ?: "Chargement du parcours impossible.",
                    )
                }
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LearnViewModel() as T
    }
}
