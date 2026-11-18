package fr.russki.app.feature.packs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import fr.russki.app.data.content.InstalledPackState
import fr.russki.app.data.pack.PackStatusUi
import fr.russki.app.domain.model.LoadState

@Composable
fun PacksRoute(
    onBack: () -> Unit,
    viewModel: PacksViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    PacksScreen(
        state = state,
        message = message,
        onRetry = viewModel::refresh,
        onBack = onBack,
        onInstall = viewModel::install,
        onDelete = viewModel::delete,
        onDismissMessage = viewModel::consumeMessage,
    )
}

@Composable
fun PacksScreen(
    state: LoadState<PacksUiModel>,
    message: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onInstall: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismissMessage: () -> Unit,
) {
    ScreenScaffold(
        title = "Packs",
        subtitle = "Locaux et distants — les leçons embarquées restent utilisables hors ligne.",
    ) {
        TextButton(onClick = onBack) {
            Text("Retour")
        }
        SpacerH(8)
        if (message != null) {
            SoftPanel {
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onDismissMessage) {
                    Text("OK")
                }
            }
            SpacerH(12)
        }
        when (state) {
            LoadState.Loading -> LoadingState()
            is LoadState.Error -> ErrorState(message = state.message, onRetry = onRetry)
            is LoadState.Empty -> ErrorState(message = state.message, onRetry = onRetry)
            is LoadState.Ready -> FadeInContent(visible = true) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SoftPanel {
                        SectionLabel("Réseau")
                        Text(
                            text = if (state.value.onlineConfigured) {
                                "API configurée (HTTPS)."
                            } else {
                                "API non configurée — catalogue distant indisponible. " +
                                    "Définissez RUSSKI_API_BASE_URL au build."
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                        state.value.remoteNote?.let { note ->
                            SpacerH(6)
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    state.value.packs.forEach { pack ->
                        PackCard(
                            pack = pack,
                            onInstall = { onInstall(pack.packId) },
                            onDelete = { onDelete(pack.packId) },
                        )
                    }
                    SpacerH(16)
                    Text(
                        text = "Supprimer un pack optionnel n'efface ni les quiz, " +
                            "ni les flashcards, ni les dates de révision.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PackCard(
    pack: PackStatusUi,
    onInstall: () -> Unit,
    onDelete: () -> Unit,
) {
    SoftPanel {
        SectionLabel(
            when {
                pack.isBundled -> "Embarqué"
                pack.isRemote -> "Distant"
                else -> "Optionnel"
            },
        )
        Text(text = pack.titleFr, style = MaterialTheme.typography.titleLarge)
        SpacerH(4)
        Text(
            text = pack.descriptionFr,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SpacerH(12)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Taille : ${pack.sizeLabel}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "État : ${pack.stateLabelFr}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = when {
                        pack.state == InstalledPackState.INSTALLED -> "Disponible hors ligne"
                        pack.availableLocally -> "Installable depuis les assets"
                        pack.isRemote -> "Téléchargement HTTPS (checksum)"
                        else -> "Indisponible hors ligne"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "v${pack.version}",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        SpacerH(12)
        when {
            pack.isBundled -> {
                Text(
                    text = "Toujours installé avec l'application.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            pack.state == InstalledPackState.INSTALLED -> {
                TextButton(onClick = onDelete) {
                    Text("Supprimer le pack")
                }
            }
            pack.state == InstalledPackState.DOWNLOADING -> {
                Text("Installation en cours…", style = MaterialTheme.typography.bodyMedium)
            }
            else -> {
                val canInstall = pack.availableLocally || pack.isRemote
                Button(
                    onClick = onInstall,
                    enabled = canInstall,
                ) {
                    Text(
                        when {
                            pack.availableLocally -> "Installer hors ligne"
                            pack.isRemote -> "Télécharger (HTTPS)"
                            else -> "Indisponible"
                        },
                    )
                }
                if (pack.lastError != null) {
                    SpacerH(6)
                    Text(
                        text = pack.lastError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
