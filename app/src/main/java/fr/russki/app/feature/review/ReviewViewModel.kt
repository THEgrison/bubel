package fr.russki.app.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.russki.app.data.quiz.QuizRepository
import fr.russki.app.data.review.ReviewRepository
import fr.russki.app.di.ServiceLocator
import fr.russki.app.domain.model.LoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReviewUiModel(
    val dueCount: Int,
    val unlockedCount: Int,
    val hasInterruptedQuiz: Boolean,
)

class ReviewViewModel(
    private val reviewRepository: ReviewRepository =
        ServiceLocator.require().reviewRepository,
    private val quiz: QuizRepository = ServiceLocator.require().quizRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<ReviewUiModel>>(LoadState.Loading)
    val state: StateFlow<LoadState<ReviewUiModel>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = LoadState.Loading
            runCatching {
                val unlocked = reviewRepository.syncUnlockedCards()
                ReviewUiModel(
                    dueCount = reviewRepository.dueCount(),
                    unlockedCount = unlocked.size,
                    hasInterruptedQuiz = quiz.findInterruptedSession() != null,
                )
            }.onSuccess { model ->
                _state.value = if (
                    model.dueCount == 0 &&
                    model.unlockedCount == 0 &&
                    !model.hasInterruptedQuiz
                ) {
                    LoadState.Empty(
                        "Aucune notion débloquée pour la révision. " +
                            "Terminez au moins une leçon ou un dialogue dans Apprendre / Parler.",
                    )
                } else {
                    LoadState.Ready(model)
                }
            }.onFailure {
                _state.value = LoadState.Error(it.message ?: "Chargement impossible.")
            }
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ReviewViewModel() as T
    }
}
