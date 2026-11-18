package fr.russki.app.domain.content

/**
 * Modèle pédagogique versionnable — indépendant de l'UI Compose.
 * Les instances concrètes vivent dans [fr.russki.app.data.content.seed].
 */

data class ContentPack(
    val version: String,
    val localeUi: String,
    val targetLanguage: String,
    val alphabet: AlphabetModule,
    val vocabularyModules: List<VocabularyModule>,
)

data class AlphabetModule(
    val id: String,
    val titleFr: String,
    val descriptionFr: String,
    val letters: List<AlphabetLetter>,
    val exercises: List<AlphabetExercise>,
)

/**
 * Une lettre de l'alphabet russe.
 *
 * [pronunciationGuideFr] explique la prononciation en français sans jamais
 * présenter une approximation comme une transcription phonétique exacte.
 * [soundInWordsFr] décrit le rôle/son de la lettre dans les mots, distinctement.
 */
data class AlphabetLetter(
    val id: String,
    val order: Int,
    val uppercase: String,
    val lowercase: String,
    val nameRu: String,
    val pronunciationGuideFr: String,
    val soundInWordsFr: String,
    val exampleWordRu: String,
    val exampleWordFr: String,
    /** Chemin d'asset audio, ou null si aucun enregistrement n'est fourni. */
    val audioAssetPath: String?,
)

enum class AlphabetExerciseType {
    RecognizeLetter,
    MatchLetterToName,
    ChooseLetterForSound,
    DistinguishLatinLookalikes,
    IdentifyCase,
}

data class AlphabetExercise(
    val id: String,
    val type: AlphabetExerciseType,
    val promptFr: String,
    /** Affichage principal (lettre, nom, description de son, etc.). */
    val stimulus: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanationFr: String,
    /** Lettre liée, si pertinent. */
    val relatedLetterId: String?,
)

data class VocabularyModule(
    val id: String,
    val titleFr: String,
    val descriptionFr: String,
    val category: String,
    val level: ContentLevel,
    val entries: List<VocabularyEntry>,
)

enum class ContentLevel {
    Beginner,
    Elementary,
    Intermediate,
}

data class VocabularyEntry(
    val id: String,
    val textRu: String,
    val translationFr: String,
    val category: String,
    val level: ContentLevel,
    val exampleRu: String?,
    val exampleFr: String?,
    val audioAssetPath: String?,
)

/** Référence audio résolue : seule une ressource réellement présente est exposée. */
data class ResolvedAudio(
    val assetPath: String,
)

interface AudioAvailability {
    fun isAvailable(assetPath: String?): ResolvedAudio?
}
