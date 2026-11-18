package fr.russki.app.feature.dialogue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.russki.app.core.ui.components.ErrorState
import fr.russki.app.core.ui.components.FadeInContent
import fr.russki.app.core.ui.components.LoadingState
import fr.russki.app.core.ui.components.MetaChip
import fr.russki.app.core.ui.components.ProgressLine
import fr.russki.app.core.ui.components.ScreenScaffold
import fr.russki.app.core.ui.components.SectionLabel
import fr.russki.app.core.ui.components.SoftPanel
import fr.russki.app.core.ui.components.SpacerH
import fr.russki.app.data.dialogue.DialogueCatalogItem
import fr.russki.app.domain.model.LoadState

@Composable
fun DialogueCatalogRoute(
    onOpenDialogue: (String) -> Unit,
    viewModel: DialogueCatalogViewModel = viewModel(factory = DialogueCatalogViewModel.Factory()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DialogueCatalogScreen(
        state = state,
        onRetry = viewModel::refresh,
        onOpenDialogue = onOpenDialogue,
    )
}

@Composable
fun DialogueCatalogScreen(
    state: LoadState<List<DialogueCatalogItem>>,
    onRetry: () -> Unit,
    onOpenDialogue: (String) -> Unit,
) {
    ScreenScaffold(
        title = "Parler",
        subtitle = "Dialogues A1 scriptés — hors ligne, sans micro ni IA.",
    ) {
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
                        SectionLabel("Mode texte")
                        Text(
                            text = "Les enregistrements natifs seront proposés uniquement " +
                                "lorsqu'un fichier audio licencié sera présent. " +
                                "En attendant, chaque dialogue se suit entièrement à l'écrit.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    state.value.forEach { item ->
                        DialogueCard(item = item, onOpen = { onOpenDialogue(item.outline.id) })
                    }
                    SpacerH(16)
                }
            }
        }
    }
}

@Composable
private fun DialogueCard(
    item: DialogueCatalogItem,
    onOpen: () -> Unit,
) {
    SoftPanel(
        panelModifier = Modifier.clickable(role = Role.Button, onClick = onOpen),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.outline.titleFr,
                style = MaterialTheme.typography.titleMedium,
            )
            MetaChip(item.outline.cefrLevel.name)
        }
        SpacerH(8)
        Text(
            text = item.outline.situationFr,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SpacerH(8)
        Text(
            text = item.outline.objectiveFr,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SpacerH(12)
        ProgressLine(progress = item.progress.fraction)
        SpacerH(6)
        Text(
            text = if (item.progress.isCompleted) {
                "Terminé"
            } else {
                "${item.progress.completedActivityCount}/${item.progress.totalActivityCount} activités"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
