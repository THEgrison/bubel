package fr.russki.app.feature.learn

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.russki.app.core.ui.components.ErrorState
import fr.russki.app.core.ui.components.FadeInContent
import fr.russki.app.core.ui.components.LoadingState
import fr.russki.app.core.ui.components.MetaChip
import fr.russki.app.core.ui.components.ProgressLine
import fr.russki.app.core.ui.components.ScreenScaffold
import fr.russki.app.core.ui.components.SecondaryButton
import fr.russki.app.core.ui.components.SectionLabel
import fr.russki.app.core.ui.components.SoftPanel
import fr.russki.app.core.ui.components.SpacerH
import fr.russki.app.data.curriculum.PathWithProgress
import fr.russki.app.data.curriculum.UnitWithProgress
import fr.russki.app.domain.curriculum.LessonOutline
import fr.russki.app.domain.curriculum.LessonProgressSummary
import fr.russki.app.domain.model.LoadState

@Composable
fun LearnRoute(
    onOpenLesson: (String) -> Unit,
    onOpenPlacement: () -> Unit = {},
    viewModel: LearnViewModel = viewModel(factory = LearnViewModel.Factory()),
) {
    // Le ViewModel survit dans le back stack (saveState) : recharger après une leçon.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LearnScreen(
        state = state,
        onRetry = viewModel::refresh,
        onOpenLesson = onOpenLesson,
        onOpenPlacement = onOpenPlacement,
    )
}

@Composable
fun LearnScreen(
    state: LoadState<PathWithProgress>,
    onRetry: () -> Unit,
    onOpenLesson: (String) -> Unit,
    onOpenPlacement: () -> Unit = {},
) {
    ScreenScaffold(
        title = "Russki",
        subtitle = "Apprendre · parcours débutant A1 hors ligne — couverture progressive, non certifiante.",
    ) {
        when (state) {
            LoadState.Loading -> LoadingState()
            is LoadState.Error -> ErrorState(message = state.message, onRetry = onRetry)
            is LoadState.Empty -> ErrorState(message = state.message, onRetry = onRetry)
            is LoadState.Ready -> FadeInContent(visible = true) {
                val path = state.value
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    SoftPanel {
                        MetaChip(path.cefrLevel.name)
                        SpacerH(8)
                        Text(
                            text = "${path.units.size} unités · contenu embarqué",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SpacerH(8)
                        SecondaryButton(
                            text = "Test de placement (estimation)",
                            onClick = onOpenPlacement,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    path.units.forEach { unitBlock ->
                        UnitSection(
                            block = unitBlock,
                            onOpenLesson = onOpenLesson,
                        )
                    }
                    Text(
                        text = "Ce parcours ne constitue pas un niveau A1 CECRL complet. " +
                            "Les dialogues sont dans l’onglet Parler.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    )
                    SpacerH(8)
                }
            }
        }
    }
}

@Composable
private fun UnitSection(
    block: UnitWithProgress,
    onOpenLesson: (String) -> Unit,
) {
    SoftPanel {
        val completed = block.unit.lessons.count { block.lessonProgress[it.id]?.isCompleted == true }
        val total = block.unit.lessons.size
        SectionLabel(
            if (block.unit.sortOrder <= 0) {
                block.unit.titleFr
            } else {
                "Unité ${block.unit.sortOrder}"
            },
        )
        SpacerH(6)
        Text(block.unit.titleFr, style = MaterialTheme.typography.titleLarge)
        SpacerH(6)
        Text(
            block.unit.descriptionFr,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SpacerH(8)
        MetaChip("$completed / $total leçons terminées")
        SpacerH(8)
        ProgressLine(
            progress = if (total <= 0) 0f else completed.toFloat() / total.toFloat(),
        )
    }
    block.unit.lessons.forEach { lesson ->
        val progress = block.lessonProgress[lesson.id]
        LessonCard(
            lesson = lesson,
            progress = progress,
            onClick = { onOpenLesson(lesson.id) },
        )
    }
}

@Composable
private fun LessonCard(
    lesson: LessonOutline,
    progress: LessonProgressSummary?,
    onClick: () -> Unit,
) {
    SoftPanel(
        panelModifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Leçon ${lesson.sortOrder}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            when {
                progress?.isCompleted == true -> MetaChip("Terminée")
                (progress?.completedExerciseCount ?: 0) > 0 -> MetaChip("En cours")
                else -> MetaChip("${lesson.exerciseCount} exercices")
            }
        }
        SpacerH(8)
        Text(lesson.titleFr, style = MaterialTheme.typography.titleMedium)
        SpacerH(6)
        Text(
            lesson.objectiveFr,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SpacerH(12)
        ProgressLine(progress = progress?.fraction ?: 0f)
    }
}
