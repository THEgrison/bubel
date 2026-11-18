package fr.russki.app.feature.lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.russki.app.data.content.seed.LessonExerciseEnricher
import fr.russki.app.data.content.seed.LessonTeachCatalog
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
                val raw = curriculumRepository.exercises(lessonId)
                if (raw.isEmpty()) error("Aucun exercice pour cette leçon.")
                val exercises = LessonExerciseEnricher.enrich(lessonId, raw)
                val teachables = LessonTeachCatalog.forLesson(lessonId)
                curriculumRepository.markLessonOpened(lessonId)
                onPedagogicalActivity()
                engine.start(lesson, teachables, exercises)
            }.onSuccess {
                _state.value = LoadState.Ready(it)
            }.onFailure {
                _state.value = LoadState.Error(it.message ?: "Chargement impossible.")
            }
        }
    }

    fun beginDiscovery() = updateSession { engine.beginDiscovery(it) }

    fun beginPractice() = updateSession { engine.beginPractice(it) }

    fun revealHint() = updateSession { engine.revealHint(it) }

    fun submit(answer: String) {
        val current = (_state.value as? LoadState.Ready)?.value ?: return
        if (current.currentGrade != null) return
        if (current.phase != LessonPhase.Practice &&
            current.phase != LessonPhase.Rematch &&
            current.phase != LessonPhase.FinalCheck
        ) {
            return
        }
        val next = engine.submitAnswer(current, answer)
        _state.value = LoadState.Ready(next)
        // Un seul crédit progression : première réussite non révélée en pratique
        val grade = next.currentGrade
        if (current.phase == LessonPhase.Practice && grade?.countsAsMastered == true) {
            viewModelScope.launch {
                curriculumRepository.recordExercise(
                    lessonId = current.lesson.id,
                    totalExercises = current.practiceExercises.size,
                )
                onPedagogicalActivity()
            }
        }
    }

    fun continueAfterFeedback() {
        updateSession { engine.advanceAfterFeedback(it) }
        val session = (_state.value as? LoadState.Ready)?.value ?: return
        if (session.phase == LessonPhase.Summary) {
            markCompleted(session)
        }
    }

    fun beginFinalCheck() {
        updateSession { engine.beginFinalCheck(it) }
        val session = (_state.value as? LoadState.Ready)?.value ?: return
        if (session.phase == LessonPhase.Summary) {
            markCompleted(session)
        }
    }

    fun skipToSummary() {
        updateSession { engine.skipFinalToSummary(it) }
        val session = (_state.value as? LoadState.Ready)?.value ?: return
        if (session.phase == LessonPhase.Summary) {
            markCompleted(session)
        }
    }

    fun finish() = updateSession { engine.complete(it) }

    private fun markCompleted(session: LessonSessionState) {
        viewModelScope.launch {
            curriculumRepository.markLessonCompleted(
                lessonId = session.lesson.id,
                totalExercises = session.practiceExercises.size,
            )
            onPedagogicalActivity()
        }
    }

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
