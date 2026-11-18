package fr.russki.app.data.content

import android.content.Context
import fr.russki.app.domain.content.AudioAvailability
import fr.russki.app.domain.content.ResolvedAudio

/**
 * Ne signale un audio comme disponible que si l'asset existe réellement.
 * Les chemins null ou absents → pas de lecture proposée dans l'UI.
 */
class AssetAudioAvailability(
    private val context: Context,
) : AudioAvailability {

    override fun isAvailable(assetPath: String?): ResolvedAudio? {
        if (assetPath.isNullOrBlank()) return null
        return try {
            context.assets.open(assetPath).use { /* exists */ }
            ResolvedAudio(assetPath)
        } catch (_: Exception) {
            null
        }
    }
}

/** Pour tests JVM / prévisualisation sans assets : tout audio déclaré reste indisponible. */
object NoOpAudioAvailability : AudioAvailability {
    override fun isAvailable(assetPath: String?): ResolvedAudio? = null
}
