package fr.russki.app.feature.alphabet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.russki.app.data.content.OfflineContentRepository
import fr.russki.app.domain.content.AlphabetExercise
import fr.russki.app.domain.content.AlphabetLetter
import fr.russki.app.domain.content.ResolvedAudio
import fr.russki.app.domain.model.LoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AlphabetOverview(
    val title: String,
    val description: String,
    val version: String,
    val letters: List<AlphabetLetter>,
    val exerciseCount: Int,
)

data class LetterDetail(
    val letter: AlphabetLetter,
    val audio: ResolvedAudio?,
)

data class ExerciseSession(
    val exercises: List<AlphabetExercise>,
    val index: Int,
    val score: Int,
    val answered: Boolean,
    val selectedIndex: Int?,
    val finished: Boolean,
) {
    val current: AlphabetExercise? = exercises.getOrNull(index)
}

class AlphabetViewModel(
    private val repository: OfflineContentRepository = OfflineContentRepository.get(),
) : ViewModel() {

    private val _overview = MutableStateFlow<LoadState<AlphabetOverview>>(LoadState.Loading)
    val overview: StateFlow<LoadState<AlphabetOverview>> = _overview.asStateFlow()

    private val _letter = MutableStateFlow<LoadState<LetterDetail>>(LoadState.Loading)
    val letter: StateFlow<LoadState<LetterDetail>> = _letter.asStateFlow()

    private val _session = MutableStateFlow<LoadState<ExerciseSession>>(LoadState.Loading)
    val session: StateFlow<LoadState<ExerciseSession>> = _session.asStateFlow()

    init {
        loadOverview()
    }

    fun loadOverview() {
        viewModelScope.launch {
            _overview.value = LoadState.Loading
            runCatching {
                val module = repository.alphabetModule()
                AlphabetOverview(
                    title = module.titleFr,
                    description = module.descriptionFr,
                    version = repository.contentVersion(),
                    letters = repository.letters(),
                    exerciseCount = repository.exercises().size,
                )
            }.onSuccess { _overview.value = LoadState.Ready(it) }
                .onFailure {
                    _overview.value = LoadState.Error(
                        it.message ?: "Impossible de charger l'alphabet.",
                    )
                }
        }
    }

    fun loadLetter(id: String) {
        viewModelScope.launch {
            _letter.value = LoadState.Loading
            val found = repository.letter(id)
            _letter.value = if (found == null) {
                LoadState.Empty("Lettre introuvable.")
            } else {
                LoadState.Ready(
                    LetterDetail(
                        letter = found,
                        audio = repository.resolveAudio(found.audioAssetPath),
                    ),
                )
            }
        }
    }

    fun startExercises() {
        viewModelScope.launch {
            val exercises = repository.exercises()
            _session.value = if (exercises.isEmpty()) {
                LoadState.Empty("Aucun exercice disponible hors ligne.")
            } else {
                LoadState.Ready(
                    ExerciseSession(
                        exercises = exercises,
                        index = 0,
                        score = 0,
                        answered = false,
                        selectedIndex = null,
                        finished = false,
                    ),
                )
            }
        }
    }

    fun selectOption(optionIndex: Int) {
        val current = (_session.value as? LoadState.Ready)?.value ?: return
        if (current.answered || current.finished) return
        val exercise = current.current ?: return
        val correct = optionIndex == exercise.correctIndex
        _session.value = LoadState.Ready(
            current.copy(
                answered = true,
                selectedIndex = optionIndex,
                score = current.score + if (correct) 1 else 0,
            ),
        )
    }

    fun nextExercise() {
        val current = (_session.value as? LoadState.Ready)?.value ?: return
        if (!current.answered) return
        val nextIndex = current.index + 1
        _session.value = if (nextIndex >= current.exercises.size) {
            LoadState.Ready(current.copy(finished = true))
        } else {
            LoadState.Ready(
                current.copy(
                    index = nextIndex,
                    answered = false,
                    selectedIndex = null,
                    finished = false,
                ),
            )
        }
    }

    class Factory(
        private val repository: OfflineContentRepository = OfflineContentRepository.get(),
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AlphabetViewModel(repository) as T
        }
    }
}
