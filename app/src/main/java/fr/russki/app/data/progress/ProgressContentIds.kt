package fr.russki.app.data.progress

/**
 * Espace de noms des ids stockés dans `user_lesson_progress`.
 * Une seule table Room (pas de migration) ; la sémantique est portée par le préfixe.
 *
 * - leçons curriculum : `a1-lesson-…` (et futurs `a2-lesson-…`)
 * - dialogues : `dialogue-…` — **jamais** traités comme leçons ordinaires
 */
object ProgressContentIds {
    const val DIALOGUE_PREFIX = "dialogue-"

    fun isDialogue(id: String): Boolean =
        id.startsWith(DIALOGUE_PREFIX)

    fun isCurriculumLesson(id: String): Boolean =
        id.isNotBlank() && !isDialogue(id)

    fun requireDialogueId(id: String): String {
        require(isDialogue(id)) {
            "Identifiant dialogue attendu (préfixe '$DIALOGUE_PREFIX'), reçu: $id"
        }
        return id
    }

    fun requireCurriculumLessonId(id: String): String {
        require(isCurriculumLesson(id)) {
            "Identifiant leçon curriculum attendu (hors '$DIALOGUE_PREFIX'), reçu: $id"
        }
        return id
    }
}
