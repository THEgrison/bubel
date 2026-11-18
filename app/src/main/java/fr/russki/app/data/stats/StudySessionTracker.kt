package fr.russki.app.data.stats

/**
 * Chronomètre de segments pédagogiques au premier plan.
 * Appelé depuis les écrans Compose (DisposableEffect) — pas depuis l'arrière-plan.
 */
class StudySessionTracker(
    private val store: LocalStatsStore,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) {
    @Volatile
    private var segmentStartMs: Long? = null

    fun startSegment() {
        if (segmentStartMs == null) {
            segmentStartMs = nowMs()
        }
    }

    fun stopSegment() {
        val start = segmentStartMs ?: return
        segmentStartMs = null
        val elapsed = nowMs() - start
        if (elapsed > 0) {
            store.addStudyMs(store.today(), elapsed)
        }
    }
}
