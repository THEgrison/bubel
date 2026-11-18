package fr.russki.app.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.russki.app.data.review.ReviewRepository
import fr.russki.app.di.ServiceLocator
import fr.russki.app.domain.model.LoadState
import fr.russki.app.domain.review.ReviewEngine
import fr.russki.app.domain.review.ReviewPhase
import fr.russki.app.domain.review.ReviewSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReviewSessionViewModel(
    private val repository: ReviewRepository =
        ServiceLocator.require().reviewRepository,
    private val engine: ReviewEngine = ReviewEngine(),
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<ReviewSessionState>>(LoadState.Loading)
    val state: StateFlow<LoadState<ReviewSessionState>> = _state.asStateFlow()

    init {
        start()
    }

    fun start() {
        viewModelScope.launch {
            _state.value = LoadState.Loading
            runCatching {
                val due = repository.dueItems()
                if (due.isEmpty()) {
                    LoadState.Empty(
                        "Aucun élément à réviser pour le moment. " +
                            "Terminez des leçons ou dialogues, ou revenez plus tard.",
                    )
                } else {
                    LoadState.Ready(engine.start(due))
                }
            }.onSuccess { _state.value = it }
                .onFailure {
                    _state.value = LoadState.Error(it.message ?: "Chargement impossible.")
                }
        }
    }

    fun submit(answer: String) {
        val ready = _state.value as? LoadState.Ready ?: return
        val session = ready.value
        if (session.phase != ReviewPhase.Prompt) return
        val item = session.current ?: return
        // Verrouille immédiatement la phase pour éviter un double gradeAnswer
        // si Valider est tapé deux fois avant la fin de la coroutine.
        val withGrade = engine.submit(session, answer)
        val grade = withGrade.currentGrade ?: return
        _state.value = LoadState.Ready(withGrade)
        viewModelScope.launch {
            runCatching {
                val srs = repository.gradeAnswer(item, grade)
                engine.recordSrs(withGrade, srs)
            }.onSuccess { _state.value = LoadState.Ready(it) }
                .onFailure {
                    _state.value = LoadState.Error(it.message ?: "Correction impossible.")
                }
        }
    }

    fun advance() {
        val ready = _state.value as? LoadState.Ready ?: return
        _state.value = LoadState.Ready(engine.advance(ready.value))
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ReviewSessionViewModel() as T
    }
}
