package fr.russki.app.feature.dialogue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.russki.app.data.dialogue.DialogueRepository
import fr.russki.app.di.ServiceLocator
import fr.russki.app.domain.content.ResolvedAudio
import fr.russki.app.domain.dialogue.DialogueEngine
import fr.russki.app.domain.dialogue.DialoguePhase
import fr.russki.app.domain.dialogue.DialogueSessionState
import fr.russki.app.domain.model.LoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DialogueReaderUi(
    val session: DialogueSessionState,
    val currentAudio: ResolvedAudio?,
)

class DialogueReaderViewModel(
    private val dialogueId: String,
    private val repository: DialogueRepository =
        ServiceLocator.require().dialogueRepository,
    private val engine: DialogueEngine = DialogueEngine(),
    private val onPedagogicalActivity: () -> Unit = {
        ServiceLocator.require().statsRepository.markPedagogicalActivity()
    },
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<DialogueReaderUi>>(LoadState.Loading)
    val state: StateFlow<LoadState<DialogueReaderUi>> = _state.asStateFlow()

    init {
        start()
    }

    fun start() {
        viewModelScope.launch {
            _state.value = LoadState.Loading
            runCatching {
                val script = repository.script(dialogueId)
                    ?: error("Dialogue introuvable: $dialogueId")
                repository.markOpened(dialogueId)
                onPedagogicalActivity()
                wrap(engine.start(script))
            }.onSuccess {
                _state.value = LoadState.Ready(it)
            }.onFailure {
                _state.value = LoadState.Error(it.message ?: "Chargement impossible.")
            }
        }
    }

    fun begin() = update { engine.begin(it) }

    fun revealTranslation() = update { engine.revealTranslation(it) }

    fun advanceFromLine() = update { engine.advanceFromLine(it) }

    fun submit(answer: String) {
        val current = (_state.value as? LoadState.Ready)?.value ?: return
        if (current.session.phase != DialoguePhase.Activity ||
            current.session.currentGrade != null
        ) {
            return
        }
        val next = engine.submitAnswer(current.session, answer)
        _state.value = LoadState.Ready(wrap(next))
        if (next.currentGrade?.isCorrect == true) {
            viewModelScope.launch {
                repository.recordActivityCompleted(
                    dialogueId = dialogueId,
                    totalActivities = next.script.activities.size,
                )
                onPedagogicalActivity()
            }
        }
    }

    fun continueAfterFeedback() {
        update { engine.advanceAfterFeedback(it) }
        val session = (_state.value as? LoadState.Ready)?.value?.session ?: return
        if (session.phase == DialoguePhase.Summary) {
            viewModelScope.launch {
                repository.markCompleted(
                    dialogueId = dialogueId,
                    totalActivities = session.script.activities.size,
                )
                onPedagogicalActivity()
            }
        }
    }

    fun finish() = update { engine.complete(it) }

    fun restart() {
        viewModelScope.launch {
            repository.reset(dialogueId)
            val script = repository.script(dialogueId) ?: return@launch
            repository.markOpened(dialogueId)
            onPedagogicalActivity()
            _state.value = LoadState.Ready(wrap(engine.restart(script)))
        }
    }

    private fun update(transform: (DialogueSessionState) -> DialogueSessionState) {
        val current = (_state.value as? LoadState.Ready)?.value ?: return
        runCatching { transform(current.session) }
            .onSuccess { _state.value = LoadState.Ready(wrap(it)) }
            .onFailure {
                _state.value = LoadState.Error(it.message ?: "Erreur de dialogue.")
            }
    }

    private fun wrap(session: DialogueSessionState): DialogueReaderUi {
        val path = session.currentLine?.audioAssetPath
        return DialogueReaderUi(
            session = session,
            currentAudio = repository.resolveAudio(path),
        )
    }

    class Factory(private val dialogueId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DialogueReaderViewModel(dialogueId) as T
    }
}
