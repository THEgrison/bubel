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
import androidx.compose.ui.platform.LocalContext
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
import fr.russki.app.core.ui.components.SectionLabel
import fr.russki.app.core.ui.components.SoftPanel
import fr.russki.app.core.ui.components.SpacerH
import fr.russki.app.data.content.AssetAudioAvailability
import fr.russki.app.domain.content.ResolvedAudio
import fr.russki.app.domain.flashcard.FlashcardDifficulty
import fr.russki.app.domain.flashcard.FlashcardFace
import fr.russki.app.domain.flashcard.FlashcardPromptMode
import fr.russki.app.domain.model.LoadState
import fr.russki.app.domain.srs.SrsRating
import fr.russki.app.feature.audio.LocalAudioControls

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
        onSelectDifficulty = viewModel::selectDifficulty,
        onFlip = viewModel::flip,
        onGrade = viewModel::grade,
        resolveAudio = viewModel::resolveAudio,
    )
}

@Composable
fun FlashcardsScreen(
    state: LoadState<FlashcardsUiState>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSelectDifficulty: (FlashcardDifficulty) -> Unit,
    onFlip: () -> Unit,
    onGrade: (SrsRating) -> Unit,
    resolveAudio: (String?) -> ResolvedAudio?,
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
                when {
                    ui.selectingDifficulty -> DifficultyPicker(
                        current = ui.difficulty,
                        dueTotal = ui.dueTotal,
                        onSelect = onSelectDifficulty,
                    )
                    ui.finished -> ScreenScaffold(title = "Session terminée", subtitle = "Progression SRS enregistrée") {
                        SoftPanel {
                            Text(
                                "Cartes notées : ${ui.gradedThisSession}",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            SpacerH(8)
                            Text(
                                "La difficulté choisie n’a pas modifié l’historique SRS — seulement la forme des questions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    else -> {
                        val card = ui.current!!
                        ScreenScaffold(
                            title = "Flashcards",
                            subtitle = "${difficultyLabel(ui.difficulty)} · ${ui.index + 1} / ${ui.dueTotal}",
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
                                        when (ui.promptMode) {
                                            FlashcardPromptMode.RecognizeRuToFr -> {
                                                CyrillicSample(card.stressedRu ?: card.frontRu)
                                                if (ui.hintsEnabled) {
                                                    card.transliteration?.let {
                                                        SpacerH(8)
                                                        Text(it, style = MaterialTheme.typography.bodyMedium)
                                                    }
                                                }
                                            }
                                            FlashcardPromptMode.RecallFrToRu -> {
                                                Text(card.backFr, style = MaterialTheme.typography.titleLarge)
                                                SpacerH(8)
                                                Text(
                                                    "Retrouvez le russe, puis retournez.",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                            FlashcardPromptMode.ListenOrRecall -> {
                                                Text(
                                                    "Écoutez (si audio) ou retrouvez le sens, puis retournez.",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                )
                                                SpacerH(10)
                                                LocalAudioControls(audio = resolveAudio(card.audioAssetPath))
                                                if (resolveAudio(card.audioAssetPath) == null) {
                                                    SpacerH(8)
                                                    Text(card.backFr, style = MaterialTheme.typography.titleMedium)
                                                }
                                            }
                                        }
                                    } else {
                                        Text(card.backFr, style = MaterialTheme.typography.titleLarge)
                                        SpacerH(10)
                                        CyrillicSample(card.frontRu)
                                        card.transliteration?.let {
                                            SpacerH(6)
                                            Text("Prononciation : $it", style = MaterialTheme.typography.bodyMedium)
                                        }
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
                                    if (resolveAudio(card.audioAssetPath) != null) {
                                        MetaChip("Audio natif disponible")
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.AutoMirrored.Outlined.VolumeOff,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Text(
                                                "  Audio indisponible",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }

                                if (ui.face == FlashcardFace.Back) {
                                    Text("Évaluez votre souvenir", style = MaterialTheme.typography.titleMedium)
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
}

@Composable
private fun DifficultyPicker(
    current: FlashcardDifficulty,
    dueTotal: Int,
    onSelect: (FlashcardDifficulty) -> Unit,
) {
    ScreenScaffold(
        title = "Difficulté des cartes",
        subtitle = "$dueTotal carte(s) due(s) — le SRS décide du moment, pas de la forme.",
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SoftPanel {
                SectionLabel("Choisir un mode")
                Text(
                    "Facile : reconnaissance RU→FR avec indices. " +
                        "Normal : rappel FR→RU. " +
                        "Difficile : écoute ou rappel sans indices. " +
                        "Toutes : mélange.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FlashcardDifficulty.entries.forEach { level ->
                val selected = level == current
                PrimaryButton(
                    text = difficultyLabel(level) + if (selected) " (actuel)" else "",
                    onClick = { onSelect(level) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun difficultyLabel(d: FlashcardDifficulty): String = when (d) {
    FlashcardDifficulty.Easy -> "Facile"
    FlashcardDifficulty.Normal -> "Normal"
    FlashcardDifficulty.Hard -> "Difficile"
    FlashcardDifficulty.All -> "Toutes les difficultés"
}
