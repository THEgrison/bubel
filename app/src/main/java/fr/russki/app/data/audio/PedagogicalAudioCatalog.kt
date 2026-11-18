package fr.russki.app.data.audio

/**
 * Catalogue des chemins audio **attendus** (salutations, présentations, dialogues A1).
 *
 * Ces chemins sont déclaratifs : ils ne rendent un fichier « disponible » que si
 * [fr.russki.app.data.content.AssetAudioAvailability] le trouve réellement sous
 * `assets/` (ou filesDir). Aucun fichier n'est inventé ici.
 *
 * Convention : `content/audio/ru/<slug>.ogg` (Ogg Vorbis recommandé).
 * Voir [docs/AUDIO.md] pour licences et procédure d'ajout.
 */
object PedagogicalAudioCatalog {

    const val ROOT: String = "content/audio/ru"

    /** Entrées vocabulaire salutations → chemin relatif assets. */
    fun forVocabulary(entryId: String): String? = vocabPaths[entryId]

    /** Réplique de dialogue → chemin relatif assets. */
    fun forDialogueLine(lineId: String): String? = dialogueLinePaths[lineId]

    /** Phrase de présentation / entraînement oral. */
    fun forPhrase(phraseId: String): String? = phrasePaths[phraseId]

    /** Toutes les cibles documentées (pour inventaire / docs). */
    fun allExpectedPaths(): List<ExpectedAudio> =
        (vocabPaths.map { ExpectedAudio(it.key, "vocabulaire", it.value) } +
            dialogueLinePaths.map { ExpectedAudio(it.key, "dialogue", it.value) } +
            phrasePaths.map { ExpectedAudio(it.key, "phrase", it.value) })
            .sortedBy { it.relativePath }

    data class ExpectedAudio(
        val contentId: String,
        val kind: String,
        val relativePath: String,
    )

    private fun path(slug: String): String = "$ROOT/$slug.ogg"

    private val vocabPaths: Map<String, String> = mapOf(
        "vocab-greet-001" to path("privet"),
        "vocab-greet-002" to path("zdravstvuyte"),
        "vocab-greet-003" to path("dobroe-utro"),
        "vocab-greet-004" to path("dobryy-den"),
        "vocab-greet-005" to path("dobryy-vecher"),
        "vocab-greet-006" to path("spokoynoy-nochi"),
        "vocab-greet-007" to path("poka"),
        "vocab-greet-008" to path("do-svidaniya"),
        "vocab-greet-009" to path("spasibo"),
        "vocab-greet-010" to path("pozhaluysta"),
        "vocab-greet-011" to path("izvinite"),
        "vocab-greet-012" to path("kak-dela"),
        "vocab-greet-013" to path("khorosho"),
        "vocab-greet-014" to path("menya-zovut"),
        "vocab-greet-015" to path("ochen-priyatno"),
    )

    private val dialogueLinePaths: Map<String, String> = mapOf(
        "meet-l1" to path("dialogue-meet-l1"),
        "meet-l2" to path("dialogue-meet-l2"),
        "meet-l3" to path("dialogue-meet-l3"),
        "meet-l4" to path("dialogue-meet-l4"),
        "meet-l5" to path("dialogue-meet-l5"),
        "meet-l6" to path("dialogue-meet-l6"),
        "drink-l1" to path("dialogue-drink-l1"),
        "drink-l2" to path("dialogue-drink-l2"),
        "drink-l3" to path("dialogue-drink-l3"),
        "drink-l4" to path("dialogue-drink-l4"),
        "drink-l5" to path("dialogue-drink-l5"),
        "drink-l6" to path("dialogue-drink-l6"),
        "ask-l1" to path("dialogue-ask-l1"),
        "ask-l2" to path("dialogue-ask-l2"),
        "ask-l3" to path("dialogue-ask-l3"),
        "ask-l4" to path("dialogue-ask-l4"),
        "ask-l5" to path("dialogue-ask-l5"),
        "ask-l6" to path("dialogue-ask-l6"),
    )

    private val phrasePaths: Map<String, String> = mapOf(
        "phrase-menya-zovut" to path("menya-zovut"),
        "phrase-kak-vas-zovut" to path("kak-vas-zovut"),
        "phrase-ochen-priyatno" to path("ochen-priyatno"),
        "phrase-ya-iz-frantsii" to path("ya-iz-frantsii"),
    )
}
