package fr.russki.app.feature.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.russki.app.data.content.OfflineContentRepository
import fr.russki.app.data.content.seed.GreetingsVocabularySeed
import fr.russki.app.data.content.seed.SeedContentPack
import fr.russki.app.domain.model.LearningModule
import fr.russki.app.domain.model.LoadState
import fr.russki.app.domain.model.ModuleKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LearnViewModel(
    private val repository: OfflineContentRepository = OfflineContentRepository.get(),
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<List<LearningModule>>>(LoadState.Loading)
    val state: StateFlow<LoadState<List<LearningModule>>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = LoadState.Loading
            runCatching {
                val alphabet = repository.alphabetModule()
                val greetings = repository.vocabularyModule(GreetingsVocabularySeed.MODULE_ID)
                listOf(
                    LearningModule(
                        id = SeedContentPack.ALPHABET_MODULE_ID,
                        title = alphabet.titleFr,
                        subtitle = alphabet.descriptionFr,
                        cyrillicSample = alphabet.letters.take(5)
                            .joinToString(" ") { it.uppercase },
                        lessonCount = alphabet.letters.size,
                        completedLessonCount = 0,
                        kind = ModuleKind.Alphabet,
                    ),
                    LearningModule(
                        id = GreetingsVocabularySeed.MODULE_ID,
                        title = greetings?.titleFr ?: "Salutations",
                        subtitle = greetings?.descriptionFr
                            ?: "Expressions de politesse",
                        cyrillicSample = greetings?.entries?.take(2)
                            ?.joinToString(" · ") { it.textRu }
                            ?: "привет",
                        lessonCount = greetings?.entries?.size ?: 0,
                        completedLessonCount = 0,
                        kind = ModuleKind.Vocabulary,
                    ),
                )
            }.onSuccess { modules ->
                _state.value = if (modules.isEmpty()) {
                    LoadState.Empty("Aucun module hors ligne.")
                } else {
                    LoadState.Ready(modules)
                }
            }.onFailure { error ->
                _state.value = LoadState.Error(
                    error.message ?: "Chargement des modules impossible.",
                )
            }
        }
    }
}
