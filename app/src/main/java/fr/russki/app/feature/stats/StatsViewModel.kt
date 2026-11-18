package fr.russki.app.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.russki.app.data.stats.StatsRepository
import fr.russki.app.di.ServiceLocator
import fr.russki.app.domain.model.LoadState
import fr.russki.app.domain.stats.LearningStatsSnapshot
import fr.russki.app.domain.stats.StudyTimePolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StatsViewModel(
    private val statsRepository: StatsRepository =
        ServiceLocator.require().statsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<LearningStatsSnapshot>>(LoadState.Loading)
    val state: StateFlow<LoadState<LearningStatsSnapshot>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = LoadState.Loading
            runCatching { statsRepository.loadSnapshot() }
                .onSuccess { _state.value = LoadState.Ready(it) }
                .onFailure {
                    _state.value = LoadState.Error(
                        it.message ?: "Chargement des statistiques impossible.",
                    )
                }
        }
    }

    fun setDailyGoal(minutes: Int) {
        statsRepository.setDailyGoalMinutes(StudyTimePolicy.clampGoalMinutes(minutes))
        refresh()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            StatsViewModel() as T
    }
}
