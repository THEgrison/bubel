package fr.russki.app.data.content

import fr.russki.app.data.content.seed.SeedContentPack
import fr.russki.app.domain.content.AlphabetExercise
import fr.russki.app.domain.content.AlphabetLetter
import fr.russki.app.domain.content.AlphabetModule
import fr.russki.app.domain.content.AudioAvailability
import fr.russki.app.domain.content.ContentPack
import fr.russki.app.domain.content.ResolvedAudio
import fr.russki.app.domain.content.VocabularyModule

/**
 * Contenu pédagogique embarqué, 100 % hors ligne.
 * Remplacera progressivement [fr.russki.app.data.temporary.TemporaryContentSource].
 */
class OfflineContentRepository(
    private val audioAvailability: AudioAvailability = NoOpAudioAvailability,
    private val pack: ContentPack = SeedContentPack.pack,
) {

    init {
        ContentValidator.requireValid(pack)
    }

    fun contentVersion(): String = pack.version

    fun contentPack() = pack

    fun alphabetModule(): AlphabetModule = pack.alphabet

    fun letters(): List<AlphabetLetter> = pack.alphabet.letters.sortedBy { it.order }

    fun letter(id: String): AlphabetLetter? = pack.alphabet.letters.find { it.id == id }

    fun exercises(): List<AlphabetExercise> = pack.alphabet.exercises

    fun vocabularyModules(): List<VocabularyModule> = pack.vocabularyModules

    fun vocabularyModule(id: String): VocabularyModule? =
        pack.vocabularyModules.find { it.id == id }

    fun resolveAudio(path: String?): ResolvedAudio? = audioAvailability.isAvailable(path)

    companion object {
        @Volatile
        private var defaultInstance: OfflineContentRepository? = null

        fun get(): OfflineContentRepository {
            return defaultInstance ?: synchronized(this) {
                defaultInstance ?: OfflineContentRepository().also { defaultInstance = it }
            }
        }

        /** Permet d'injecter la résolution d'assets Android une fois le Context disponible. */
        fun install(repository: OfflineContentRepository) {
            defaultInstance = repository
        }
    }
}
