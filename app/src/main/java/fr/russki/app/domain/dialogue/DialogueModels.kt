package fr.russki.app.domain.dialogue

import fr.russki.app.domain.curriculum.CefrLevel

/**
 * Dialogues scriptés (contenu fixe, aucune génération).
 * `audioAssetPath` est déclaratif : l'UI ne propose la lecture que si le fichier existe.
 */

data class DialogueOutline(
    val id: String,
    val titleFr: String,
    val cefrLevel: CefrLevel,
    val situationFr: String,
    val objectiveFr: String,
    val lineCount: Int,
    val activityCount: Int,
)

data class DialogueSpeaker(
    val id: String,
    val labelFr: String,
)

data class DialogueLine(
    val id: String,
    val sortOrder: Int,
    val speakerId: String,
    val textRu: String,
    val textFr: String,
    /** Null ou chemin déclaré — jamais présenté comme disponible sans fichier réel. */
    val audioAssetPath: String?,
)

enum class DialogueActivityType {
    /** Lire / « écouter » une réplique puis choisir la traduction FR. */
    ChooseTranslation,
    /** Choisir la réplique suivante appropriée. */
    ChooseReply,
    /** Compléter le mot manquant dans une réplique. */
    FillBlank,
}

data class DialogueActivity(
    val id: String,
    val type: DialogueActivityType,
    /** Index 0-based de la réplique après laquelle l'activité apparaît. */
    val afterLineIndex: Int,
    val promptFr: String,
    val stimulusRu: String,
    val choices: List<String>,
    val acceptedAnswers: List<String>,
    val explanationFr: String,
) {
    init {
        require(id.isNotBlank())
        require(acceptedAnswers.isNotEmpty())
        require(choices.isNotEmpty())
        require(afterLineIndex >= 0)
    }
}

data class DialogueScript(
    val outline: DialogueOutline,
    val speakers: List<DialogueSpeaker>,
    val lines: List<DialogueLine>,
    val activities: List<DialogueActivity>,
) {
    init {
        require(lines.isNotEmpty()) { "Dialogue ${outline.id} sans répliques." }
        require(lines.map { it.id }.toSet().size == lines.size)
        require(activities.map { it.id }.toSet().size == activities.size)
        val speakerIds = speakers.map { it.id }.toSet()
        require(lines.all { it.speakerId in speakerIds })
        require(activities.all { it.afterLineIndex < lines.size })
        require(outline.lineCount == lines.size)
        require(outline.activityCount == activities.size)
    }

    fun speakerLabel(speakerId: String): String =
        speakers.find { it.id == speakerId }?.labelFr ?: speakerId

    fun activitiesAfter(lineIndex: Int): List<DialogueActivity> =
        activities.filter { it.afterLineIndex == lineIndex }.sortedBy { it.id }
}

data class DialogueProgressSummary(
    val dialogueId: String,
    val completedActivityCount: Int,
    val totalActivityCount: Int,
    val completedAtEpochMs: Long?,
) {
    val isCompleted: Boolean
        get() = completedAtEpochMs != null ||
            (totalActivityCount > 0 && completedActivityCount >= totalActivityCount)

    val fraction: Float
        get() = if (totalActivityCount <= 0) {
            0f
        } else {
            (completedActivityCount.toFloat() / totalActivityCount).coerceIn(0f, 1f)
        }
}
