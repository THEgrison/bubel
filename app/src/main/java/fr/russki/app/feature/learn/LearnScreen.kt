package fr.russki.app.feature.learn

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.russki.app.core.ui.components.CyrillicSample
import fr.russki.app.core.ui.components.EmptyState
import fr.russki.app.core.ui.components.ErrorState
import fr.russki.app.core.ui.components.FadeInContent
import fr.russki.app.core.ui.components.LoadingState
import fr.russki.app.core.ui.components.MetaChip
import fr.russki.app.core.ui.components.ProgressLine
import fr.russki.app.core.ui.components.ScreenScaffold
import fr.russki.app.core.ui.components.SoftPanel
import fr.russki.app.core.ui.components.SpacerH
import fr.russki.app.domain.model.LearningModule
import fr.russki.app.domain.model.LoadState
import fr.russki.app.domain.model.ModuleKind

@Composable
fun LearnRoute(
    viewModel: LearnViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LearnScreen(state = state, onRetry = viewModel::refresh)
}

@Composable
fun LearnScreen(
    state: LoadState<List<LearningModule>>,
    onRetry: () -> Unit,
) {
    ScreenScaffold(
        title = "Apprendre",
        subtitle = "Modules pour débutants, utilisables hors ligne.",
    ) {
        when (state) {
            LoadState.Loading -> LoadingState()
            is LoadState.Error -> ErrorState(message = state.message, onRetry = onRetry)
            is LoadState.Empty -> EmptyState(
                title = "Modules indisponibles",
                body = state.message,
                actionLabel = "Actualiser",
                onAction = onRetry,
            )
            is LoadState.Ready -> FadeInContent(visible = true) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    state.value.forEach { module ->
                        ModuleCard(module)
                    }
                    Text(
                        text = "Contenu provisoire — sera remplacé par Room",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                    SpacerH(8)
                }
            }
        }
    }
}

@Composable
private fun ModuleCard(module: LearningModule) {
    SoftPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = kindLabel(module.kind),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SpacerH(4)
                Text(
                    text = module.title,
                    style = MaterialTheme.typography.titleLarge,
                )
                SpacerH(6)
                Text(
                    text = module.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MetaChip("${module.completedLessonCount}/${module.lessonCount}")
        }
        SpacerH(14)
        CyrillicSample(text = module.cyrillicSample)
        SpacerH(14)
        ProgressLine(
            progress = module.completedLessonCount.toFloat() /
                module.lessonCount.coerceAtLeast(1).toFloat(),
        )
    }
}

private fun kindLabel(kind: ModuleKind): String = when (kind) {
    ModuleKind.Alphabet -> "Fondations"
    ModuleKind.Vocabulary -> "Vocabulaire"
    ModuleKind.Phrases -> "Expression"
    ModuleKind.Mixed -> "Parcours"
}
