package fr.russki.app.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import fr.russki.app.domain.model.ThemePreference
import fr.russki.app.domain.stats.StudyTimePolicy

@Composable
fun ProfileRoute(
    onOpenPacks: () -> Unit = {},
    onOpenStats: () -> Unit = {},
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
        onOpenPacks = onOpenPacks,
        onOpenStats = onOpenStats,
        onSyncOptInChange = viewModel::setSyncOptIn,
        onSyncPush = viewModel::requestSyncPush,
    )
}

@Composable
fun ProfileScreen(
    state: LoadState<ProfileUi>,
    onRetry: () -> Unit,
    onDailyGoalChange: (Int) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onOfflineOnlyChange: (Boolean) -> Unit,
    onThemeChange: (ThemePreference) -> Unit,
    onOpenPacks: () -> Unit = {},
    onOpenStats: () -> Unit = {},
    onSyncOptInChange: (Boolean) -> Unit = {},
    onSyncPush: () -> Unit = {},
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
                    ui = state.value,
                    onDailyGoalChange = onDailyGoalChange,
                    onSoundChange = onSoundChange,
                    onOfflineOnlyChange = onOfflineOnlyChange,
                    onThemeChange = onThemeChange,
                    onOpenPacks = onOpenPacks,
                    onOpenStats = onOpenStats,
                    onSyncOptInChange = onSyncOptInChange,
                    onSyncPush = onSyncPush,
                )
            }
        }
    }
}

@Composable
private fun ProfileContent(
    ui: ProfileUi,
    onDailyGoalChange: (Int) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onOfflineOnlyChange: (Boolean) -> Unit,
    onThemeChange: (ThemePreference) -> Unit,
    onOpenPacks: () -> Unit,
    onOpenStats: () -> Unit,
    onSyncOptInChange: (Boolean) -> Unit,
    onSyncPush: () -> Unit,
) {
    val settings = ui.settings
    Column(
        Modifier
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
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StudyTimePolicy.goalChoicesMinutes.forEach { minutes ->
                    FilterChip(
                        selected = settings.dailyGoalMinutes == minutes,
                        onClick = { onDailyGoalChange(minutes) },
                        label = { Text("$minutes min") },
                    )
                }
            }
            SpacerH(12)
            TextButton(onClick = onOpenStats) {
                Text("Voir les statistiques")
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
            SpacerH(12)
            TextButton(onClick = onOpenPacks) {
                Text("Gérer les packs")
            }
        }

        SoftPanel {
            SectionLabel("Synchronisation (facultative)")
            Text(
                text = "En cas d’activation, Russki n’envoie que des identifiants de leçons " +
                    "et des compteurs SRS — pas de nom ni d’e-mail. " +
                    "Le serveur de sync n’est pas encore déployé : sans jeton, " +
                    "aucune donnée ne part et la progression locale reste intacte.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SpacerH(12)
            SettingSwitchRow(
                title = "Autoriser la sync",
                subtitle = if (ui.syncServerConfigured) {
                    "API configurée — jeton d’auth encore requis (non déployé)"
                } else {
                    "API non configurée (RUSSKI_API_BASE_URL)"
                },
                checked = ui.syncOptIn,
                onCheckedChange = onSyncOptInChange,
            )
            SpacerH(8)
            Button(
                onClick = onSyncPush,
                enabled = ui.syncOptIn,
            ) {
                Text("Tenter une synchronisation")
            }
            ui.syncMessage?.let { msg ->
                SpacerH(8)
                Text(text = msg, style = MaterialTheme.typography.bodySmall)
            }
        }

        Text(
            text = "Le catalogue pédagogique, les quiz, flashcards et révisions " +
                "sont stockés localement (Room). Aucune fonctionnalité essentielle " +
                "n'attend le réseau.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        )
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
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
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
