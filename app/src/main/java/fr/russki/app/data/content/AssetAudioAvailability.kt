package fr.russki.app.data.content

import android.content.Context
import fr.russki.app.domain.content.AudioAvailability
import fr.russki.app.domain.content.ResolvedAudio
import java.io.File

/**
 * Ne signale un audio comme disponible que si la ressource existe réellement
 * (asset embarqué **ou** fichier installé sous filesDir).
 */
class AssetAudioAvailability(
    private val context: Context,
) : AudioAvailability {

    override fun isAvailable(assetPath: String?): ResolvedAudio? {
        if (assetPath.isNullOrBlank()) return null
        // Fichier absolu (pack téléchargé)
        if (assetPath.startsWith("/")) {
            val file = File(assetPath)
            val filesRoot = context.filesDir.canonicalFile
            val canon = runCatching { file.canonicalFile }.getOrNull() ?: return null
            val prefix = filesRoot.path.trimEnd('/') + "/"
            if (!canon.path.startsWith(prefix) || !canon.isFile) return null
            return ResolvedAudio(canon.absolutePath)
        }
        // Asset relatif
        return try {
            context.assets.open(assetPath).use { /* exists */ }
            ResolvedAudio(assetPath)
        } catch (_: Exception) {
            // Relatif sous filesDir/packs
            val underPacks = File(context.filesDir, assetPath)
            val filesRoot = context.filesDir.canonicalFile
            val canon = runCatching { underPacks.canonicalFile }.getOrNull() ?: return null
            val prefix = filesRoot.path.trimEnd('/') + "/"
            if (canon.path.startsWith(prefix) && canon.isFile) {
                ResolvedAudio(canon.absolutePath)
            } else {
                null
            }
        }
    }
}

/** Pour tests JVM / prévisualisation sans assets : tout audio déclaré reste indisponible. */
object NoOpAudioAvailability : AudioAvailability {
    override fun isAvailable(assetPath: String?): ResolvedAudio? = null
}
