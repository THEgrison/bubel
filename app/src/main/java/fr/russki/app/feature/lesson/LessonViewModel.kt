package fr.russki.app.feature.lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.russki.app.data.curriculum.CurriculumRepository
import fr.russki.app.di.ServiceLocator
import fr.russki.app.domain.lesson.LessonEngine
import fr.russki.app.domain.lesson.LessonPhase
import fr.russki.app.domain.lesson.LessonSessionState
import fr.russki.app.domain.model.LoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LessonViewModel(
    private val lessonId: String,
    private val curriculumRepository: CurriculumRepository =
        ServiceLocator.require().curriculumRepository,
    private val engine: LessonEngine = LessonEngine(),
    private val onPedagogicalActivity: () -> Unit = {
        ServiceLocator.require().statsRepository.markPedagogicalActivity()
    },
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<LessonSessionState>>(LoadState.Loading)
    val state: StateFlow<LoadState<LessonSessionState>> = _state.asStateFlow()

    init {
        start()
    }

    fun start() {
        viewModelScope.launch {
            _state.value = LoadState.Loading
            runCatching {
                val lesson = curriculumRepository.lesson(lessonId)
                    ?: error("Leçon introuvable: $lessonId")
                val exercises = curriculumRepository.exercises(lessonId)
                if (exercises.isEmpty()) error("Aucun exercice pour cette leçon.")
                curriculumRepository.markLessonOpened(lessonId)
                onPedagogicalActivity()
                engine.start(lesson, exercises)
            }.onSuccess {
                _state.value = LoadState.Ready(it)
            }.onFailure {
                _state.value = LoadState.Error(it.message ?: "Chargement impossible.")
            }
        }
    }

    fun beginExercises() = updateSession { engine.beginExercises(it) }

    fun submit(answer: String) {
        val current = (_state.value as? LoadState.Ready)?.value ?: return
        if (current.phase != LessonPhase.Exercise || current.currentGrade != null) return
        val next = engine.submitAnswer(current, answer)
        _state.value = LoadState.Ready(next)
        if (next.currentGrade?.isCorrect == true) {
            viewModelScope.launch {
                curriculumRepository.recordExercise(
                    lessonId = current.lesson.id,
                    totalExercises = current.exercises.size,
                )
                onPedagogicalActivity()
            }
        }
    }

    fun continueAfterFeedback() {
        updateSession { engine.advanceAfterFeedback(it) }
        val session = (_state.value as? LoadState.Ready)?.value ?: return
        if (session.phase == LessonPhase.Summary) {
            viewModelScope.launch {
                curriculumRepository.markLessonCompleted(
                    lessonId = session.lesson.id,
                    totalExercises = session.exercises.size,
                )
                onPedagogicalActivity()
            }
        }
    }

    fun finish() = updateSession { engine.complete(it) }

    private fun updateSession(transform: (LessonSessionState) -> LessonSessionState) {
        val current = (_state.value as? LoadState.Ready)?.value ?: return
        runCatching { transform(current) }
            .onSuccess { _state.value = LoadState.Ready(it) }
            .onFailure { _state.value = LoadState.Error(it.message ?: "Erreur de leçon.") }
    }

    class Factory(private val lessonId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LessonViewModel(lessonId) as T
    }
}
