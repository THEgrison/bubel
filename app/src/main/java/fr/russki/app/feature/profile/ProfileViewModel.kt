package fr.russki.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.russki.app.data.online.OnlineResult
import fr.russki.app.data.online.sync.ProgressSyncGateway
import fr.russki.app.data.stats.StatsRepository
import fr.russki.app.data.temporary.TemporaryContentSource
import fr.russki.app.data.temporary.TemporaryPreferencesStore
import fr.russki.app.di.ServiceLocator
import fr.russki.app.domain.model.LoadState
import fr.russki.app.domain.model.ProfileSettings
import fr.russki.app.domain.model.ThemePreference
import fr.russki.app.domain.stats.StudyTimePolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUi(
    val settings: ProfileSettings,
    val syncOptIn: Boolean,
    val syncServerConfigured: Boolean,
    val syncMessage: String?,
)

class ProfileViewModel(
    private val syncGateway: ProgressSyncGateway = ServiceLocator.require().progressSyncGateway,
    private val statsRepository: StatsRepository = ServiceLocator.require().statsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<LoadState<ProfileUi>>(LoadState.Loading)
    val state: StateFlow<LoadState<ProfileUi>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = LoadState.Loading
            runCatching { TemporaryContentSource.loadProfile() }
                .onSuccess { settings ->
                    val merged = settings.copy(
                        dailyGoalMinutes = statsRepository.dailyGoalMinutes(),
                        darkThemePreference = TemporaryPreferencesStore.themePreference.value,
                        offlinePacksOnly = TemporaryPreferencesStore.offlinePacksOnly,
                    )
                    _state.value = LoadState.Ready(
                        ProfileUi(
                            settings = merged,
                            syncOptIn = syncGateway.isUserOptedIn(),
                            syncServerConfigured = syncGateway.isServerConfigured,
                            syncMessage = null,
                        ),
                    )
                }
                .onFailure { error ->
                    _state.value = LoadState.Error(
                        error.message ?: "Chargement du profil impossible.",
                    )
                }
        }
    }

    fun setDailyGoal(minutes: Int) {
        val clamped = StudyTimePolicy.clampGoalMinutes(minutes)
        statsRepository.setDailyGoalMinutes(clamped)
        updateReady { it.copy(settings = it.settings.copy(dailyGoalMinutes = clamped)) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        updateReady { it.copy(settings = it.settings.copy(soundEnabled = enabled)) }
    }

    fun setOfflinePacksOnly(enabled: Boolean) {
        TemporaryPreferencesStore.setOfflinePacksOnly(enabled)
        updateReady { it.copy(settings = it.settings.copy(offlinePacksOnly = enabled)) }
    }

    fun setThemePreference(preference: ThemePreference) {
        TemporaryPreferencesStore.setThemePreference(preference)
        updateReady { it.copy(settings = it.settings.copy(darkThemePreference = preference)) }
    }

    fun setSyncOptIn(enabled: Boolean) {
        syncGateway.setUserOptIn(enabled)
        updateReady { it.copy(syncOptIn = enabled, syncMessage = null) }
    }

    fun requestSyncPush() {
        viewModelScope.launch {
            val result = syncGateway.pushIfAllowed()
            val message = when (result) {
                is OnlineResult.Ok -> "Sync acceptée (révision ${result.value.serverRevision ?: "—"})."
                is OnlineResult.Err -> result.error.message
            }
            updateReady { it.copy(syncMessage = message) }
        }
    }

    private fun updateReady(transform: (ProfileUi) -> ProfileUi) {
        _state.update { current ->
            when (current) {
                is LoadState.Ready -> LoadState.Ready(transform(current.value))
                else -> current
            }
        }
    }
}
