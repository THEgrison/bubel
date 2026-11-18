package fr.russki.app.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.russki.app.data.temporary.TemporaryContentSource
import fr.russki.app.domain.model.LoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReviewUiModel(
    val dueCount: Int,
)

class ReviewViewModel : ViewModel() {

    private val _state = MutableStateFlow<LoadState<ReviewUiModel>>(LoadState.Loading)
    val state: StateFlow<LoadState<ReviewUiModel>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = LoadState.Loading
            runCatching { TemporaryContentSource.loadReviewDueCount() }
                .onSuccess { due ->
                    _state.value = if (due == 0) {
                        LoadState.Empty(
                            "Les flashcards dues apparaîtront ici. " +
                                "Continuez une leçon pour constituer votre paquet.",
                        )
                    } else {
                        LoadState.Ready(ReviewUiModel(dueCount = due))
                    }
                }
                .onFailure { error ->
                    _state.value = LoadState.Error(
                        error.message ?: "Chargement des révisions impossible.",
                    )
                }
        }
    }
}
