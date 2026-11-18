package fr.russki.app.feature.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.russki.app.data.quiz.QuizRepository
import fr.russki.app.di.ServiceLocator
import fr.russki.app.domain.model.LoadState
import fr.russki.app.domain.quiz.QuizGradeResult
import fr.russki.app.domain.quiz.QuizSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QuizUiState(
    val session: QuizSessionState,
    val lastGrade: QuizGradeResult? = null,
    val showAnswerReveal: Boolean = false,
)

class QuizViewModel(
    private val repository: QuizRepository = ServiceLocator.require().quizRepository,
    private val defaultQuestionCount: Int = 8,
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<QuizUiState>>(LoadState.Loading)
    val state: StateFlow<LoadState<QuizUiState>> = _state.asStateFlow()

    fun startNew(count: Int = defaultQuestionCount) {
        viewModelScope.launch {
            _state.value = LoadState.Loading
            runCatching { repository.startSession(count) }
                .onSuccess { session ->
                    _state.value = LoadState.Ready(QuizUiState(session = session))
                }
                .onFailure {
                    _state.value = LoadState.Error(it.message ?: "Impossible de démarrer le quiz.")
                }
        }
    }

    fun resumeInterrupted() {
        viewModelScope.launch {
            _state.value = LoadState.Loading
            val interrupted = repository.findInterruptedSession()
            if (interrupted == null) {
                startNew()
            } else {
                _state.value = LoadState.Ready(QuizUiState(session = interrupted))
            }
        }
    }

    fun submitAnswer(answer: String) {
        val current = (_state.value as? LoadState.Ready)?.value ?: return
        viewModelScope.launch {
            runCatching { repository.submitAnswer(current.session, answer) }
                .onSuccess { (grade, session) ->
                    _state.value = LoadState.Ready(
                        QuizUiState(
                            session = session,
                            lastGrade = grade,
                            showAnswerReveal = true,
                        ),
                    )
                }
                .onFailure {
                    _state.value = LoadState.Error(it.message ?: "Erreur d'enregistrement.")
                }
        }
    }

    fun next() {
        val current = (_state.value as? LoadState.Ready)?.value ?: return
        viewModelScope.launch {
            runCatching { repository.advance(current.session) }
                .onSuccess { session ->
                    _state.value = LoadState.Ready(
                        QuizUiState(session = session, lastGrade = null, showAnswerReveal = false),
                    )
                }
                .onFailure {
                    _state.value = LoadState.Error(it.message ?: "Impossible de continuer.")
                }
        }
    }

    fun abandon() {
        val current = (_state.value as? LoadState.Ready)?.value ?: return
        viewModelScope.launch {
            repository.abandon(current.session)
        }
    }

    class Factory(
        private val repository: QuizRepository = ServiceLocator.require().quizRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            QuizViewModel(repository) as T
    }
}
