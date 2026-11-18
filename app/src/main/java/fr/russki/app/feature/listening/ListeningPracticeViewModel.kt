package fr.russki.app.feature.listening

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.russki.app.data.audio.AudioResolver
import fr.russki.app.data.listening.ListeningExerciseBank
import fr.russki.app.di.ServiceLocator
import fr.russki.app.domain.content.ResolvedAudio
import fr.russki.app.domain.exercise.ExerciseGrade
import fr.russki.app.domain.exercise.ExerciseGrader
import fr.russki.app.domain.listening.ListeningItem
import fr.russki.app.domain.model.LoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ListeningPracticeUi(
    val items: List<ListeningItem>,
    val index: Int,
    val grades: Map<String, ExerciseGrade>,
    val grade: ExerciseGrade?,
    val currentAudio: ResolvedAudio?,
    val finished: Boolean,
) {
    val currentItem: ListeningItem? get() = items.getOrNull(index)
    val correctCount: Int get() = grades.values.count { it.isCorrect }
}

class ListeningPracticeViewModel(
    private val resolver: AudioResolver = ServiceLocator.require().audioResolver,
    private val grader: ExerciseGrader = ExerciseGrader(),
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<ListeningPracticeUi>>(LoadState.Loading)
    val state: StateFlow<LoadState<ListeningPracticeUi>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val items = ListeningExerciseBank.items()
            _state.value = LoadState.Ready(buildUi(items, 0, emptyMap(), null, false))
        }
    }

    fun restart() = refresh()

    fun submit(answer: String) {
        val ready = _state.value as? LoadState.Ready ?: return
        val ui = ready.value
        val item = ui.currentItem ?: return
        if (ui.grade != null) return
        val grade = grader.grade(item.asExercise(), answer)
        _state.value = LoadState.Ready(
            buildUi(
                items = ui.items,
                index = ui.index,
                grades = ui.grades + (item.id to grade),
                grade = grade,
                finished = false,
            ),
        )
    }

    fun advance() {
        val ready = _state.value as? LoadState.Ready ?: return
        val ui = ready.value
        val next = ui.index + 1
        if (next >= ui.items.size) {
            _state.value = LoadState.Ready(
                ui.copy(finished = true, grade = null, currentAudio = null),
            )
        } else {
            _state.value = LoadState.Ready(
                buildUi(ui.items, next, ui.grades, null, false),
            )
        }
    }

    private fun buildUi(
        items: List<ListeningItem>,
        index: Int,
        grades: Map<String, ExerciseGrade>,
        grade: ExerciseGrade?,
        finished: Boolean,
    ): ListeningPracticeUi {
        val item = items.getOrNull(index)
        val audio = item?.let { resolver.resolveCatalogKey(it.audioKey) }
        return ListeningPracticeUi(
            items = items,
            index = index,
            grades = grades,
            grade = grade,
            currentAudio = audio,
            finished = finished,
        )
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ListeningPracticeViewModel() as T
    }
}
