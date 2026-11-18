package fr.russki.app.feature.oral

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.russki.app.data.audio.AudioResolver
import fr.russki.app.data.audio.LocalVoiceRecorder
import fr.russki.app.data.content.seed.GreetingsVocabularySeed
import fr.russki.app.di.ServiceLocator
import fr.russki.app.domain.content.ResolvedAudio
import fr.russki.app.domain.model.LoadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OralPrompt(
    val id: String,
    val textRu: String,
    val textFr: String,
    val modelAudio: ResolvedAudio?,
)

data class OralPracticeUi(
    val prompts: List<OralPrompt>,
    val index: Int,
) {
    val current: OralPrompt get() = prompts[index]
}

class OralPracticeViewModel(
    private val resolver: AudioResolver = ServiceLocator.require().audioResolver,
    val voiceRecorder: LocalVoiceRecorder = ServiceLocator.require().voiceRecorder,
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<OralPracticeUi>>(LoadState.Loading)
    val state: StateFlow<LoadState<OralPracticeUi>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val prompts = GreetingsVocabularySeed.module.entries.map { entry ->
                OralPrompt(
                    id = entry.id,
                    textRu = entry.textRu,
                    textFr = entry.translationFr,
                    modelAudio = resolver.resolveVocabulary(entry.id),
                )
            }
            _state.value = LoadState.Ready(OralPracticeUi(prompts = prompts, index = 0))
        }
    }

    fun nextPrompt() = shift(+1)

    fun prevPrompt() = shift(-1)

    fun clearRecordings() {
        voiceRecorder.clearSession()
    }

    private fun shift(delta: Int) {
        val ready = _state.value as? LoadState.Ready ?: return
        val ui = ready.value
        val next = (ui.index + delta).coerceIn(0, ui.prompts.lastIndex)
        _state.value = LoadState.Ready(ui.copy(index = next))
    }

    override fun onCleared() {
        voiceRecorder.clearSession()
        super.onCleared()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            OralPracticeViewModel() as T
    }
}
