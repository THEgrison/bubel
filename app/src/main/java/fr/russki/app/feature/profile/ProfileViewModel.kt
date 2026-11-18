package fr.russki.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.russki.app.data.temporary.TemporaryContentSource
import fr.russki.app.data.temporary.TemporaryPreferencesStore
import fr.russki.app.domain.model.LoadState
import fr.russki.app.domain.model.ProfileSettings
import fr.russki.app.domain.model.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val _state = MutableStateFlow<LoadState<ProfileSettings>>(LoadState.Loading)
    val state: StateFlow<LoadState<ProfileSettings>> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = LoadState.Loading
            runCatching { TemporaryContentSource.loadProfile() }
                .onSuccess { settings ->
                    val merged = settings.copy(
                        darkThemePreference = TemporaryPreferencesStore.themePreference.value,
                    )
                    _state.value = LoadState.Ready(merged)
                }
                .onFailure { error ->
                    _state.value = LoadState.Error(
                        error.message ?: "Chargement du profil impossible.",
                    )
                }
        }
    }

    fun setDailyGoal(minutes: Int) {
        updateReady { it.copy(dailyGoalMinutes = minutes.coerceIn(5, 60)) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        updateReady { it.copy(soundEnabled = enabled) }
    }

    fun setOfflinePacksOnly(enabled: Boolean) {
        updateReady { it.copy(offlinePacksOnly = enabled) }
    }

    fun setThemePreference(preference: ThemePreference) {
        TemporaryPreferencesStore.setThemePreference(preference)
        updateReady { it.copy(darkThemePreference = preference) }
    }

    private fun updateReady(transform: (ProfileSettings) -> ProfileSettings) {
        _state.update { current ->
            when (current) {
                is LoadState.Ready -> LoadState.Ready(transform(current.value))
                else -> current
            }
        }
    }
}
