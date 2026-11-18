package fr.russki.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.russki.app.data.content.OfflineContentRepository
import fr.russki.app.domain.model.ContinueLesson
import fr.russki.app.domain.model.HomeContent
import fr.russki.app.domain.model.LearningProgress
import fr.russki.app.domain.model.LoadState
import fr.russki.app.domain.model.ReviewSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: OfflineContentRepository = OfflineContentRepository.get(),
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<HomeContent>>(LoadState.Loading)
    val state: StateFlow<LoadState<HomeContent>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = LoadState.Loading
            runCatching {
                val alphabet = repository.alphabetModule()
                val first = alphabet.letters.first()
                HomeContent.Dashboard(
                    progress = LearningProgress(
                        completedLessons = 0,
                        totalLessonsInPath = alphabet.letters.size +
                            repository.vocabularyModules().sumOf { it.entries.size },
                        streakDays = 0,
                        wordsSeen = 0,
                    ),
                    continueLesson = ContinueLesson(
                        id = first.id,
                        moduleTitle = alphabet.titleFr,
                        lessonTitle = "Lettre ${first.uppercase} ${first.lowercase}",
                        cyrillicPreview = alphabet.letters.take(4)
                            .joinToString(" · ") { "${it.uppercase}${it.lowercase}" },
                        progressFraction = 0f,
                    ),
                    review = ReviewSummary(
                        dueCount = 0,
                        nextReviewLabel = "Aucune carte due pour l'instant",
                    ),
                )
            }.onSuccess { content ->
                _state.value = LoadState.Ready(content)
            }.onFailure { error ->
                _state.value = LoadState.Error(
                    error.message ?: "Chargement de l'accueil impossible.",
                )
            }
        }
    }
}
