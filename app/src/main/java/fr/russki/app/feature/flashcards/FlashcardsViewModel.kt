package fr.russki.app.feature.flashcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.russki.app.data.content.OfflineContentRepository
import fr.russki.app.data.flashcard.FlashcardDifficultyStore
import fr.russki.app.data.flashcard.FlashcardRepository
import fr.russki.app.di.ServiceLocator
import fr.russki.app.domain.flashcard.Flashcard
import fr.russki.app.domain.flashcard.FlashcardDifficulty
import fr.russki.app.domain.flashcard.FlashcardFace
import fr.russki.app.domain.flashcard.FlashcardPromptMode
import fr.russki.app.domain.flashcard.promptModeFor
import fr.russki.app.domain.model.LoadState
import fr.russki.app.domain.srs.SrsRating
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FlashcardsUiState(
    val queue: List<Flashcard>,
    val index: Int,
    val face: FlashcardFace,
    val dueTotal: Int,
    val gradedThisSession: Int,
    val difficulty: FlashcardDifficulty,
    val selectingDifficulty: Boolean,
    val promptMode: FlashcardPromptMode,
    val hintsEnabled: Boolean,
) {
    val current: Flashcard? get() = queue.getOrNull(index)
    val finished: Boolean get() = !selectingDifficulty && (queue.isEmpty() || index >= queue.size)
}

class FlashcardsViewModel(
    private val repository: FlashcardRepository = ServiceLocator.require().flashcardRepository,
    private val contentRepository: OfflineContentRepository =
        ServiceLocator.require().contentRepository,
    private val difficultyStore: FlashcardDifficultyStore =
        ServiceLocator.require().flashcardDifficultyStore,
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<FlashcardsUiState>>(LoadState.Loading)
    val state: StateFlow<LoadState<FlashcardsUiState>> = _state.asStateFlow()

    fun start() {
        viewModelScope.launch {
            _state.value = LoadState.Loading
            runCatching {
                val due = repository.dueFlashcards()
                val difficulty = difficultyStore.get()
                FlashcardsUiState(
                    queue = due,
                    index = 0,
                    face = FlashcardFace.Front,
                    dueTotal = due.size,
                    gradedThisSession = 0,
                    difficulty = difficulty,
                    selectingDifficulty = true,
                    promptMode = FlashcardPromptMode.RecognizeRuToFr,
                    hintsEnabled = difficulty == FlashcardDifficulty.Easy,
                )
            }.onSuccess { ui ->
                _state.value = if (ui.queue.isEmpty()) {
                    LoadState.Empty("Aucune carte due pour le moment. Revenez plus tard.")
                } else {
                    LoadState.Ready(ui)
                }
            }.onFailure {
                _state.value = LoadState.Error(it.message ?: "Chargement des cartes impossible.")
            }
        }
    }

    fun selectDifficulty(difficulty: FlashcardDifficulty) {
        val current = (_state.value as? LoadState.Ready)?.value ?: return
        difficultyStore.set(difficulty)
        val card = current.queue.firstOrNull()
        val mode = card?.let { difficulty.promptModeFor(it, 0) }
            ?: FlashcardPromptMode.RecognizeRuToFr
        _state.value = LoadState.Ready(
            current.copy(
                difficulty = difficulty,
                selectingDifficulty = false,
                promptMode = mode,
                hintsEnabled = difficulty == FlashcardDifficulty.Easy,
                face = FlashcardFace.Front,
                index = 0,
            ),
        )
    }

    fun flip() {
        val current = (_state.value as? LoadState.Ready)?.value ?: return
        if (current.selectingDifficulty) return
        _state.value = LoadState.Ready(
            current.copy(
                face = if (current.face == FlashcardFace.Front) {
                    FlashcardFace.Back
                } else {
                    FlashcardFace.Front
                },
            ),
        )
    }

    fun grade(rating: SrsRating) {
        val current = (_state.value as? LoadState.Ready)?.value ?: return
        if (current.selectingDifficulty) return
        val card = current.current ?: return
        viewModelScope.launch {
            runCatching { repository.grade(card.id, rating) }
                .onSuccess {
                    val nextIndex = current.index + 1
                    val nextCard = current.queue.getOrNull(nextIndex)
                    val mode = nextCard?.let {
                        current.difficulty.promptModeFor(it, nextIndex)
                    } ?: current.promptMode
                    _state.value = LoadState.Ready(
                        current.copy(
                            index = nextIndex,
                            face = FlashcardFace.Front,
                            gradedThisSession = current.gradedThisSession + 1,
                            promptMode = mode,
                        ),
                    )
                }
                .onFailure {
                    _state.value = LoadState.Error(it.message ?: "Notation impossible.")
                }
        }
    }

    fun resolveAudio(path: String?) =
        contentRepository.resolveAudio(path)

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FlashcardsViewModel() as T
    }
}
