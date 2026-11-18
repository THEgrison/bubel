package fr.russki.app.data.audio

import fr.russki.app.domain.content.AudioAvailability
import fr.russki.app.domain.content.ResolvedAudio

/**
 * Résout un chemin déclaré (seed / catalogue) uniquement s'il existe vraiment.
 */
class AudioResolver(
    private val availability: AudioAvailability,
) {
    fun resolveDeclared(path: String?): ResolvedAudio? =
        availability.isAvailable(path)

    fun resolveVocabulary(entryId: String): ResolvedAudio? =
        resolveDeclared(PedagogicalAudioCatalog.forVocabulary(entryId))

    fun resolveDialogueLine(lineId: String, declaredPath: String?): ResolvedAudio? =
        resolveDeclared(declaredPath)
            ?: resolveDeclared(PedagogicalAudioCatalog.forDialogueLine(lineId))

    fun resolveCatalogKey(key: String): ResolvedAudio? {
        val path = PedagogicalAudioCatalog.forVocabulary(key)
            ?: PedagogicalAudioCatalog.forDialogueLine(key)
            ?: PedagogicalAudioCatalog.forPhrase(key)
            ?: key.takeIf { it.contains('/') }
        return resolveDeclared(path)
    }
}
