package fr.russki.app.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
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
import fr.russki.app.core.ui.components.CyrillicSample
import fr.russki.app.core.ui.components.EmptyState
import fr.russki.app.core.ui.components.ErrorState
import fr.russki.app.core.ui.components.FadeInContent
import fr.russki.app.core.ui.components.LoadingState
import fr.russki.app.core.ui.components.MetaChip
import fr.russki.app.core.ui.components.ProgressLine
import fr.russki.app.core.ui.components.ScreenScaffold
import fr.russki.app.core.ui.components.SectionLabel
import fr.russki.app.core.ui.components.SoftPanel
import fr.russki.app.core.ui.components.SpacerH
import fr.russki.app.domain.model.ContinueLesson
import fr.russki.app.domain.model.HomeContent
import fr.russki.app.domain.model.LearningProgress
import fr.russki.app.domain.model.LoadState
import fr.russki.app.domain.model.ReviewSummary

@Composable
fun HomeRoute(
    onOpenLearn: () -> Unit,
    onOpenReview: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onRetry = viewModel::refresh,
        onContinueLesson = onOpenLearn,
        onOpenReview = onOpenReview,
    )
}

@Composable
fun HomeScreen(
    state: LoadState<HomeContent>,
    onRetry: () -> Unit,
    onContinueLesson: () -> Unit,
    onOpenReview: () -> Unit,
) {
    ScreenScaffold(
        title = "Russki",
        subtitle = "Apprenez le russe, à votre rythme.",
    ) {
        when (state) {
            LoadState.Loading -> LoadingState()
            is LoadState.Error -> ErrorState(message = state.message, onRetry = onRetry)
            is LoadState.Empty -> EmptyState(
                title = "Parcours à démarrer",
                body = state.message,
                actionLabel = "Voir les modules",
                onAction = onContinueLesson,
            )
            is LoadState.Ready -> {
                FadeInContent(visible = true) {
                    when (val content = state.value) {
                        is HomeContent.EmptyPath -> EmptyState(
                            title = "Parcours à démarrer",
                            body = "Choisissez un module pour commencer.",
                            actionLabel = "Apprendre",
                            onAction = onContinueLesson,
                        )
                        is HomeContent.Dashboard -> DashboardContent(
                            content = content,
                            onContinueLesson = onContinueLesson,
                            onOpenReview = onOpenReview,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    content: HomeContent.Dashboard,
    onContinueLesson: () -> Unit,
    onOpenReview: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ProgressCard(content.progress)
        content.continueLesson?.let { lesson ->
            ContinueCard(lesson = lesson, onClick = onContinueLesson)
        }
        ReviewCard(summary = content.review, onClick = onOpenReview)
        Text(
            text = "Données de démonstration — source temporaire",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
        SpacerH(8)
    }
}

@Composable
private fun ProgressCard(progress: LearningProgress) {
    SoftPanel {
        SectionLabel("Votre progression")
        Text(
            text = "${progress.completedLessons} / ${progress.totalLessonsInPath} leçons",
            style = MaterialTheme.typography.titleMedium,
        )
        SpacerH(12)
        ProgressLine(
            progress = progress.completedLessons.toFloat() /
                progress.totalLessonsInPath.coerceAtLeast(1).toFloat(),
        )
        SpacerH(14)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetaChip("${progress.wordsSeen} mots aperçus")
            if (progress.streakDays > 0) {
                MetaChip("${progress.streakDays} jours d'affilée")
            }
        }
    }
}

@Composable
private fun ContinueCard(
    lesson: ContinueLesson,
    onClick: () -> Unit,
) {
    SoftPanel(
        panelModifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    ) {
        SectionLabel("Continuer")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.moduleTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SpacerH(4)
                Text(
                    text = lesson.lessonTitle,
                    style = MaterialTheme.typography.titleLarge,
                )
                SpacerH(8)
                CyrillicSample(text = lesson.cyrillicPreview)
            }
            Icon(
                imageVector = Icons.Outlined.PlayCircle,
                contentDescription = "Continuer la leçon",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        SpacerH(14)
        ProgressLine(progress = lesson.progressFraction)
    }
}

@Composable
private fun ReviewCard(
    summary: ReviewSummary,
    onClick: () -> Unit,
) {
    SoftPanel(
        panelModifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    ) {
        SectionLabel("Révisions prévues")
        if (summary.dueCount == 0) {
            Text(
                text = summary.nextReviewLabel ?: "Rien à réviser pour le moment.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = "${summary.dueCount} carte${if (summary.dueCount > 1) "s" else ""} à revoir",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
