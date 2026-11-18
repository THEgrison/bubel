package fr.russki.app.feature.flashcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.russki.app.data.content.OfflineContentRepository
import fr.russki.app.data.flashcard.FlashcardRepository
import fr.russki.app.di.ServiceLocator
import fr.russki.app.domain.flashcard.Flashcard
import fr.russki.app.domain.flashcard.FlashcardFace
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
) {
    val current: Flashcard? get() = queue.getOrNull(index)
    val finished: Boolean get() = queue.isEmpty() || index >= queue.size
}

class FlashcardsViewModel(
    private val repository: FlashcardRepository = ServiceLocator.require().flashcardRepository,
    private val contentRepository: OfflineContentRepository =
        ServiceLocator.require().contentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<FlashcardsUiState>>(LoadState.Loading)
    val state: StateFlow<LoadState<FlashcardsUiState>> = _state.asStateFlow()

    fun start() {
        viewModelScope.launch {
            _state.value = LoadState.Loading
            runCatching {
                val due = repository.dueFlashcards()
                FlashcardsUiState(
                    queue = due,
                    index = 0,
                    face = FlashcardFace.Front,
                    dueTotal = due.size,
                    gradedThisSession = 0,
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

    fun flip() {
        val current = (_state.value as? LoadState.Ready)?.value ?: return
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
        val card = current.current ?: return
        viewModelScope.launch {
            runCatching { repository.grade(card.id, rating) }
                .onSuccess {
                    val nextIndex = current.index + 1
                    _state.value = LoadState.Ready(
                        current.copy(
                            index = nextIndex,
                            face = FlashcardFace.Front,
                            gradedThisSession = current.gradedThisSession + 1,
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
