package fr.russki.app.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.russki.app.data.flashcard.FlashcardRepository
import fr.russki.app.data.quiz.QuizRepository
import fr.russki.app.di.ServiceLocator
import fr.russki.app.domain.model.LoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReviewUiModel(
    val dueCount: Int,
    val hasInterruptedQuiz: Boolean,
)

class ReviewViewModel(
    private val flashcards: FlashcardRepository = ServiceLocator.require().flashcardRepository,
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
                ReviewUiModel(
                    dueCount = flashcards.dueCount(),
                    hasInterruptedQuiz = quiz.findInterruptedSession() != null,
                )
            }.onSuccess { model ->
                _state.value = if (model.dueCount == 0 && !model.hasInterruptedQuiz) {
                    LoadState.Empty(
                        "Aucune carte due et aucune session de quiz en cours. " +
                            "Apprenez du vocabulaire ou lancez un quiz pour alimenter vos révisions.",
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
