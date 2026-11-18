package fr.russki.app.feature.stats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import fr.russki.app.data.stats.StudySessionTracker
import fr.russki.app.di.ServiceLocator

/**
 * Chronomètre le temps passé sur un écran pédagogique au premier plan.
 * S'arrête à la sortie (navigation / arrière-plan Compose).
 */
@Composable
fun TrackStudyTime(
    tracker: StudySessionTracker = ServiceLocator.require().studySessionTracker,
) {
    DisposableEffect(tracker) {
        tracker.startSegment()
        onDispose { tracker.stopSegment() }
    }
}
