package fr.russki.app.feature.review

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.russki.app.core.ui.components.EmptyState
import fr.russki.app.core.ui.components.ErrorState
import fr.russki.app.core.ui.components.FadeInContent
import fr.russki.app.core.ui.components.LoadingState
import fr.russki.app.core.ui.components.ScreenScaffold
import fr.russki.app.core.ui.components.SoftPanel
import fr.russki.app.core.ui.components.SpacerH
import fr.russki.app.domain.model.LoadState

@Composable
fun ReviewRoute(
    onGoLearn: () -> Unit,
    viewModel: ReviewViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReviewScreen(
        state = state,
        onRetry = viewModel::refresh,
        onGoLearn = onGoLearn,
    )
}

@Composable
fun ReviewScreen(
    state: LoadState<ReviewUiModel>,
    onRetry: () -> Unit,
    onGoLearn: () -> Unit,
) {
    ScreenScaffold(
        title = "Réviser",
        subtitle = "Flashcards et rappels espacés.",
    ) {
        when (state) {
            LoadState.Loading -> LoadingState()
            is LoadState.Error -> ErrorState(message = state.message, onRetry = onRetry)
            is LoadState.Empty -> FadeInContent(visible = true) {
                EmptyState(
                    title = "Rien à réviser",
                    body = state.message,
                    icon = Icons.Outlined.Style,
                    actionLabel = "Aller à Apprendre",
                    onAction = onGoLearn,
                )
            }
            is LoadState.Ready -> FadeInContent(visible = true) {
                SoftPanel {
                    Text(
                        text = "${state.value.dueCount} révision${if (state.value.dueCount > 1) "s" else ""} due${if (state.value.dueCount > 1) "s" else ""}",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    SpacerH(8)
                    Text(
                        text = "La session de révision sera branchée dans une prochaine phase.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
