package fr.russki.app.feature.placement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.russki.app.data.placement.PlacementRepository
import fr.russki.app.di.ServiceLocator
import fr.russki.app.domain.model.LoadState
import fr.russki.app.domain.placement.PlacementEngine
import fr.russki.app.domain.placement.PlacementPhase
import fr.russki.app.domain.placement.PlacementRecommender
import fr.russki.app.domain.placement.PlacementSessionState
import fr.russki.app.domain.placement.PlacementStoredResults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlacementUi(
    val session: PlacementSessionState?,
    val stored: PlacementStoredResults,
    val starting: Boolean = false,
)

class PlacementViewModel(
    private val repository: PlacementRepository =
        ServiceLocator.require().placementRepository,
    private val engine: PlacementEngine = PlacementEngine(),
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<PlacementUi>>(LoadState.Loading)
    val state: StateFlow<LoadState<PlacementUi>> = _state.asStateFlow()

    init {
        refreshHub()
    }

    fun refreshHub() {
        viewModelScope.launch {
            _state.value = LoadState.Loading
            runCatching {
                PlacementUi(
                    session = null,
                    stored = repository.loadStored(),
                )
            }.onSuccess { _state.value = LoadState.Ready(it) }
                .onFailure {
                    _state.value = LoadState.Error(
                        it.message ?: "Chargement du test de placement impossible.",
                    )
                }
        }
    }

    fun startTest() {
        viewModelScope.launch {
            updateReady { it.copy(starting = true) }
            runCatching {
                val questions = repository.loadQuestions()
                require(questions.isNotEmpty()) {
                    "Aucune question disponible pour le curriculum actuel."
                }
                engine.start(questions)
            }.onSuccess { session ->
                updateReady {
                    it.copy(session = session, starting = false)
                }
            }.onFailure { err ->
                _state.value = LoadState.Error(
                    err.message ?: "Impossible de démarrer le test.",
                )
            }
        }
    }

    fun beginQuestions() {
        mutateSession { engine.beginQuestions(it) }
    }

    fun submitAnswer(answer: String) {
        mutateSession { engine.submitAnswer(it, answer) }
    }

    fun advanceAfterFeedback() {
        val current = (state.value as? LoadState.Ready)?.value?.session ?: return
        if (current.phase != PlacementPhase.Question) return
        val advanced = engine.advanceAfterFeedback(current)
        if (advanced.phase != PlacementPhase.Result) {
            updateReady { it.copy(session = advanced) }
            return
        }
        viewModelScope.launch {
            runCatching {
                repository.completeAndPersist(
                    questions = advanced.questions,
                    grades = advanced.grades,
                )
            }.onSuccess { result ->
                val withResult = engine.attachResult(advanced, result)
                updateReady {
                    it.copy(
                        session = withResult,
                        stored = repository.loadStored(),
                    )
                }
            }.onFailure {
                val fallback = PlacementRecommender.buildResult(
                    questions = advanced.questions,
                    grades = advanced.grades,
                    availableLessonIds = advanced.questions
                        .map { q -> q.skill.recommendedLessonId }
                        .toSet(),
                    lessonTitles = emptyMap(),
                    completedAtEpochMs = System.currentTimeMillis(),
                )
                updateReady {
                    it.copy(session = engine.attachResult(advanced, fallback))
                }
            }
        }
    }

    fun abandonToHub() {
        updateReady {
            it.copy(
                session = null,
                stored = repository.loadStored(),
                starting = false,
            )
        }
    }

    private fun mutateSession(block: (PlacementSessionState) -> PlacementSessionState) {
        val session = (state.value as? LoadState.Ready)?.value?.session ?: return
        updateReady { it.copy(session = block(session)) }
    }

    private fun updateReady(transform: (PlacementUi) -> PlacementUi) {
        _state.update { current ->
            if (current is LoadState.Ready) LoadState.Ready(transform(current.value))
            else current
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlacementViewModel() as T
    }
}
