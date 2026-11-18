package fr.russki.app.data.dialogue

import fr.russki.app.data.content.NoOpAudioAvailability
import fr.russki.app.data.content.seed.A1DialoguesSeed
import fr.russki.app.data.progress.LessonProgressRepository
import fr.russki.app.data.progress.ProgressContentIds
import fr.russki.app.domain.content.AudioAvailability
import fr.russki.app.domain.content.ResolvedAudio
import fr.russki.app.domain.dialogue.DialogueOutline
import fr.russki.app.domain.dialogue.DialogueProgressSummary
import fr.russki.app.domain.dialogue.DialogueScript
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DialogueCatalogItem(
    val outline: DialogueOutline,
    val progress: DialogueProgressSummary,
)

/**
 * Dialogues A1 embarqués (seed Kotlin) + progression dans `user_lesson_progress`
 * (même table, ids `dialogue-*`) — **aucune migration Room**.
 */
class DialogueRepository(
    private val progressRepository: LessonProgressRepository,
    private val audioAvailability: AudioAvailability = NoOpAudioAvailability,
) {

    fun scripts(): List<DialogueScript> = A1DialoguesSeed.all

    fun script(id: String): DialogueScript? = A1DialoguesSeed.byId(id)

    fun resolveAudio(assetPath: String?): ResolvedAudio? =
        audioAvailability.isAvailable(assetPath)

    suspend fun loadCatalog(): List<DialogueCatalogItem> = withContext(Dispatchers.IO) {
        // Ne lit que les ids dialogue-* — ignore toute ligne leçon de la même table.
        val stored = progressRepository.getAll()
            .filter { ProgressContentIds.isDialogue(it.lessonId) }
            .associateBy { it.lessonId }
        A1DialoguesSeed.outlines().map { outline ->
            ProgressContentIds.requireDialogueId(outline.id)
            val row = stored[outline.id]
            DialogueCatalogItem(
                outline = outline,
                progress = DialogueProgressSummary(
                    dialogueId = outline.id,
                    completedActivityCount = row?.completedExerciseCount ?: 0,
                    totalActivityCount = outline.activityCount,
                    completedAtEpochMs = row?.completedAtEpochMs,
                ),
            )
        }
    }

    suspend fun markOpened(dialogueId: String) {
        progressRepository.markOpened(ProgressContentIds.requireDialogueId(dialogueId))
    }

    suspend fun recordActivityCompleted(dialogueId: String, totalActivities: Int) {
        progressRepository.recordExerciseCompleted(
            ProgressContentIds.requireDialogueId(dialogueId),
            totalActivities,
        )
    }

    suspend fun markCompleted(dialogueId: String, totalActivities: Int) {
        progressRepository.completeLesson(
            ProgressContentIds.requireDialogueId(dialogueId),
            totalActivities,
        )
    }

    suspend fun reset(dialogueId: String) {
        progressRepository.resetProgress(ProgressContentIds.requireDialogueId(dialogueId))
    }
}
