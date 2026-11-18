package fr.russki.app.feature.flashcards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import fr.russki.app.core.ui.components.LoadingState
import fr.russki.app.core.ui.components.MetaChip
import fr.russki.app.core.ui.components.PrimaryButton
import fr.russki.app.core.ui.components.ScreenScaffold
import fr.russki.app.core.ui.components.SecondaryButton
import fr.russki.app.core.ui.components.SoftPanel
import fr.russki.app.core.ui.components.SpacerH
import fr.russki.app.domain.flashcard.FlashcardFace
import fr.russki.app.domain.model.LoadState
import fr.russki.app.domain.srs.SrsRating

@Composable
fun FlashcardsRoute(
    onBack: () -> Unit,
    viewModel: FlashcardsViewModel = viewModel(factory = FlashcardsViewModel.Factory()),
) {
    LaunchedEffect(Unit) { viewModel.start() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    FlashcardsScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::start,
        onFlip = viewModel::flip,
        onGrade = viewModel::grade,
        hasAudio = { path -> viewModel.resolveAudio(path) != null },
    )
}

@Composable
fun FlashcardsScreen(
    state: LoadState<FlashcardsUiState>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onFlip: () -> Unit,
    onGrade: (SrsRating) -> Unit,
    hasAudio: (String?) -> Boolean,
) {
    Column(Modifier.fillMaxSize()) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
        }
        when (state) {
            LoadState.Loading -> LoadingState()
            is LoadState.Error -> ErrorState(message = state.message, onRetry = onRetry)
            is LoadState.Empty -> EmptyState(
                title = "Rien à réviser",
                body = state.message,
                actionLabel = "Retour",
                onAction = onBack,
            )
            is LoadState.Ready -> {
                val ui = state.value
                if (ui.finished) {
                    ScreenScaffold(title = "Session terminée", subtitle = "Progression enregistrée") {
                        SoftPanel {
                            Text(
                                "Cartes notées : ${ui.gradedThisSession}",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            SpacerH(12)
                            PrimaryButton(
                                text = "Actualiser les cartes dues",
                                onClick = onRetry,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            SpacerH(8)
                            SecondaryButton(
                                text = "Retour",
                                onClick = onBack,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                } else {
                    val card = ui.current!!
                    ScreenScaffold(
                        title = "Flashcards",
                        subtitle = "Carte ${ui.index + 1} / ${ui.dueTotal}",
                    ) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            SoftPanel(
                                panelModifier = Modifier.clickable(
                                    role = Role.Button,
                                    onClick = onFlip,
                                ),
                            ) {
                                Text(
                                    if (ui.face == FlashcardFace.Front) {
                                        "Recto — toucher pour retourner"
                                    } else {
                                        "Verso — toucher pour retourner"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                SpacerH(12)
                                if (ui.face == FlashcardFace.Front) {
                                    CyrillicSample(card.stressedRu ?: card.frontRu)
                                    card.transliteration?.let {
                                        SpacerH(8)
                                        Text(it, style = MaterialTheme.typography.bodyMedium)
                                    }
                                } else {
                                    Text(card.backFr, style = MaterialTheme.typography.titleLarge)
                                    SpacerH(10)
                                    CyrillicSample(card.frontRu)
                                    card.exampleRu?.let {
                                        SpacerH(12)
                                        Text(it, style = MaterialTheme.typography.titleMedium)
                                        card.exampleFr?.let { fr ->
                                            SpacerH(4)
                                            Text(
                                                fr,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                                SpacerH(12)
                                if (hasAudio(card.audioAssetPath)) {
                                    MetaChip("Audio natif disponible")
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.AutoMirrored.Outlined.VolumeOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            "  Pas d'enregistrement natif",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }

                            if (ui.face == FlashcardFace.Back) {
                                Text(
                                    "Évaluez votre souvenir",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    SecondaryButton(
                                        text = "À revoir",
                                        onClick = { onGrade(SrsRating.Again) },
                                        modifier = Modifier.weight(1f),
                                    )
                                    SecondaryButton(
                                        text = "Difficile",
                                        onClick = { onGrade(SrsRating.Hard) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    PrimaryButton(
                                        text = "Bien",
                                        onClick = { onGrade(SrsRating.Good) },
                                        modifier = Modifier.weight(1f),
                                    )
                                    PrimaryButton(
                                        text = "Facile",
                                        onClick = { onGrade(SrsRating.Easy) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            } else {
                                PrimaryButton(
                                    text = "Retourner la carte",
                                    onClick = onFlip,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            SpacerH(8)
                        }
                    }
                }
            }
        }
    }
}
