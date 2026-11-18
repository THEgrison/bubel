package fr.russki.app.feature.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import fr.russki.app.core.ui.components.MetaChip
import fr.russki.app.core.ui.components.ProgressLine
import fr.russki.app.core.ui.components.ScreenScaffold
import fr.russki.app.core.ui.components.SectionLabel
import fr.russki.app.core.ui.components.SoftPanel
import fr.russki.app.core.ui.components.SpacerH
import fr.russki.app.domain.model.LoadState
import fr.russki.app.domain.stats.LearningStatsSnapshot
import fr.russki.app.domain.stats.StudyTimePolicy

@Composable
fun StatsRoute(
    onBack: () -> Unit,
    viewModel: StatsViewModel = viewModel(factory = StatsViewModel.Factory()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
        }
        StatsScreen(
            state = state,
            onRetry = viewModel::refresh,
            onGoalChange = viewModel::setDailyGoal,
        )
    }
}

@Composable
fun StatsScreen(
    state: LoadState<LearningStatsSnapshot>,
    onRetry: () -> Unit,
    onGoalChange: (Int) -> Unit,
) {
    ScreenScaffold(
        title = "Statistiques",
        subtitle = "Suivi local hors ligne — sans synchronisation ni notifications.",
    ) {
        when (state) {
            LoadState.Loading -> LoadingState()
            is LoadState.Error -> ErrorState(message = state.message, onRetry = onRetry)
            is LoadState.Empty -> ErrorState(message = state.message, onRetry = onRetry)
            is LoadState.Ready -> FadeInContent(visible = true) {
                StatsContent(snapshot = state.value, onGoalChange = onGoalChange)
            }
        }
    }
}

@Composable
private fun StatsContent(
    snapshot: LearningStatsSnapshot,
    onGoalChange: (Int) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SoftPanel {
            SectionLabel("Parcours leçons")
            Text(
                text = "${snapshot.path.completedLessons} / ${snapshot.path.totalLessons} leçons terminées",
                style = MaterialTheme.typography.titleMedium,
            )
            SpacerH(8)
            ProgressLine(progress = snapshot.path.fraction)
            SpacerH(6)
            Text(
                text = "${snapshot.path.percentLabel} % du curriculum chargé",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SoftPanel {
            SectionLabel("Par unité")
            snapshot.path.units.forEach { unit ->
                SpacerH(10)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(unit.titleFr, style = MaterialTheme.typography.bodyLarge)
                    MetaChip("${unit.completedLessons}/${unit.totalLessons}")
                }
                SpacerH(6)
                ProgressLine(progress = unit.fraction)
            }
        }

        SoftPanel {
            SectionLabel("Dialogues (séparés)")
            Text(
                text = "${snapshot.dialogues.completedDialogues} / ${snapshot.dialogues.totalDialogues} dialogues terminés",
                style = MaterialTheme.typography.titleMedium,
            )
            SpacerH(8)
            ProgressLine(progress = snapshot.dialogues.fraction)
            SpacerH(6)
            Text(
                text = "Les dialogues (ids dialogue-*) ne sont pas comptés comme des leçons.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SoftPanel {
            SectionLabel("Objectif du jour")
            Text(
                text = "${snapshot.daily.studiedMinutesToday} / ${snapshot.daily.goalMinutes} min",
                style = MaterialTheme.typography.titleMedium,
            )
            SpacerH(8)
            ProgressLine(progress = snapshot.daily.fraction)
            SpacerH(8)
            if (snapshot.daily.goalReached) {
                Text(
                    text = "Objectif atteint pour aujourd'hui.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    text = "Temps chronométré sur les écrans de leçon et de dialogue au premier plan " +
                        "(pas le temps passé ailleurs dans l'app).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SpacerH(12)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StudyTimePolicy.goalChoicesMinutes.forEach { minutes ->
                    FilterChip(
                        selected = snapshot.daily.goalMinutes == minutes,
                        onClick = { onGoalChange(minutes) },
                        label = { Text("$minutes min") },
                    )
                }
            }
        }

        SoftPanel {
            SectionLabel("Régularité")
            Text(
                text = "Série actuelle : ${snapshot.regularity.currentStreak} jour(s)",
                style = MaterialTheme.typography.titleMedium,
            )
            SpacerH(6)
            Text(
                text = "Meilleure série : ${snapshot.regularity.bestStreak} · " +
                    "Jours actifs : ${snapshot.regularity.activeDaysCount}",
                style = MaterialTheme.typography.bodyMedium,
            )
            SpacerH(6)
            Text(
                text = if (snapshot.regularity.activeToday) {
                    "Activité enregistrée aujourd'hui."
                } else {
                    "Pas encore d'activité pédagogique aujourd'hui."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SpacerH(8)
            Text(
                text = "Un même jour ne compte qu'une fois. La série s'appuie sur la date locale " +
                    "du appareil ; un changement de fuseau peut décaler la frontière de jour.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SoftPanel {
            SectionLabel("Compteurs disponibles")
            Text(
                text = "Exercices de leçon enregistrés (compteurs) : " +
                    "${snapshot.lessonExerciseCompletionsRecorded}",
                style = MaterialTheme.typography.bodyMedium,
            )
            SpacerH(6)
            Text(
                text = "Réponses quiz libre : ${snapshot.quizCorrectAnswers} / " +
                    "${snapshot.quizAnswersRecorded} correctes",
                style = MaterialTheme.typography.bodyMedium,
            )
            SpacerH(6)
            Text(
                text = "Il n'existe pas d'historique détaillé des tentatives par exercice de leçon : " +
                    "seul le compteur de progression est stocké.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SpacerH(12)
    }
}
