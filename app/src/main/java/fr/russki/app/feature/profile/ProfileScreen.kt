package fr.russki.app.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.russki.app.core.ui.components.ErrorState
import fr.russki.app.core.ui.components.FadeInContent
import fr.russki.app.core.ui.components.LoadingState
import fr.russki.app.core.ui.components.ScreenScaffold
import fr.russki.app.core.ui.components.SectionLabel
import fr.russki.app.core.ui.components.SoftPanel
import fr.russki.app.core.ui.components.SpacerH
import fr.russki.app.domain.model.LoadState
import fr.russki.app.domain.model.ProfileSettings
import fr.russki.app.domain.model.ThemePreference

@Composable
fun ProfileRoute(
    viewModel: ProfileViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProfileScreen(
        state = state,
        onRetry = viewModel::refresh,
        onDailyGoalChange = viewModel::setDailyGoal,
        onSoundChange = viewModel::setSoundEnabled,
        onOfflineOnlyChange = viewModel::setOfflinePacksOnly,
        onThemeChange = viewModel::setThemePreference,
    )
}

@Composable
fun ProfileScreen(
    state: LoadState<ProfileSettings>,
    onRetry: () -> Unit,
    onDailyGoalChange: (Int) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onOfflineOnlyChange: (Boolean) -> Unit,
    onThemeChange: (ThemePreference) -> Unit,
) {
    ScreenScaffold(
        title = "Profil",
        subtitle = "Préférences locales — aucun compte requis.",
    ) {
        when (state) {
            LoadState.Loading -> LoadingState()
            is LoadState.Error -> ErrorState(message = state.message, onRetry = onRetry)
            is LoadState.Empty -> ErrorState(
                message = state.message,
                onRetry = onRetry,
            )
            is LoadState.Ready -> FadeInContent(visible = true) {
                ProfileContent(
                    settings = state.value,
                    onDailyGoalChange = onDailyGoalChange,
                    onSoundChange = onSoundChange,
                    onOfflineOnlyChange = onOfflineOnlyChange,
                    onThemeChange = onThemeChange,
                )
            }
        }
    }
}

@Composable
private fun ProfileContent(
    settings: ProfileSettings,
    onDailyGoalChange: (Int) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onOfflineOnlyChange: (Boolean) -> Unit,
    onThemeChange: (ThemePreference) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SoftPanel {
            SectionLabel("Apprentissage")
            Text(
                text = settings.displayNamePlaceholder,
                style = MaterialTheme.typography.titleLarge,
            )
            SpacerH(4)
            Text(
                text = "Profil local de démonstration — pas un compte utilisateur.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SpacerH(16)
            Text(
                text = "Objectif quotidien",
                style = MaterialTheme.typography.titleMedium,
            )
            SpacerH(8)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf(5, 10, 15, 20).forEach { minutes ->
                    FilterChip(
                        selected = settings.dailyGoalMinutes == minutes,
                        onClick = { onDailyGoalChange(minutes) },
                        label = { Text("$minutes min") },
                    )
                }
            }
        }

        SoftPanel {
            SectionLabel("Apparence")
            Text(
                text = "Thème",
                style = MaterialTheme.typography.titleMedium,
            )
            SpacerH(8)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemePreference.entries.forEach { preference ->
                    FilterChip(
                        selected = settings.darkThemePreference == preference,
                        onClick = { onThemeChange(preference) },
                        label = { Text(themeLabel(preference)) },
                    )
                }
            }
        }

        SoftPanel {
            SectionLabel("Contenu & audio")
            SettingSwitchRow(
                title = "Sons activés",
                subtitle = "Lecture des enregistrements natifs lorsque disponibles",
                checked = settings.soundEnabled,
                onCheckedChange = onSoundChange,
            )
            SpacerH(8)
            SettingSwitchRow(
                title = "Priorité hors ligne",
                subtitle = "Utiliser d'abord le contenu embarqué ; packs en ligne facultatifs",
                checked = settings.offlinePacksOnly,
                onCheckedChange = onOfflineOnlyChange,
            )
        }

        Text(
            text = "Les réglages sont gardés en mémoire pour cette session. " +
                "DataStore les persisttera dans une phase ultérieure.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        )
        TextButton(onClick = { /* phase suivante */ }) {
            Text("À propos de Russki")
        }
        SpacerH(8)
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            SpacerH(4)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun themeLabel(preference: ThemePreference): String = when (preference) {
    ThemePreference.System -> "Système"
    ThemePreference.Light -> "Clair"
    ThemePreference.Dark -> "Sombre"
}
