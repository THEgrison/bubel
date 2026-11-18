package fr.russki.app.data.temporary

import fr.russki.app.domain.model.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Préférences **temporaires** en mémoire (session).
 * Remplacer par DataStore dans une phase ultérieure.
 */
object TemporaryPreferencesStore {

    const val SOURCE_TAG: String = "temporary-preferences"

    private val _themePreference = MutableStateFlow(ThemePreference.System)
    val themePreference: StateFlow<ThemePreference> = _themePreference.asStateFlow()

    @Volatile
    var offlinePacksOnly: Boolean = false
        private set

    fun setThemePreference(preference: ThemePreference) {
        _themePreference.update { preference }
    }

    fun setOfflinePacksOnly(enabled: Boolean) {
        offlinePacksOnly = enabled
    }
}
