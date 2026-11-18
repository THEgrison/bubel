package fr.russki.app.data.online.api

import fr.russki.app.data.online.OnlineResult
import java.io.File

/**
 * Port réseau Russki — implémentations :
 * - [DisabledOnlineApi] : URL absente (prod par défaut)
 * - [HttpRusskiOnlineApi] : client HTTPS réel
 * - tests : MockWebServer derrière [HttpRusskiOnlineApi]
 *
 * Aucune implémentation ne prétend qu’un backend est opérationnel
 * si [fr.russki.app.data.online.OnlineConfig.isConfigured] est false.
 */
interface RusskiOnlineApi {
    val isConfigured: Boolean

    suspend fun fetchCatalog(): OnlineResult<RemoteCatalog>

    suspend fun fetchPackManifest(packId: String, manifestPath: String): OnlineResult<RemotePackManifest>

    /**
     * Télécharge (ou reprend) un asset vers [destinationPartial].
     * Utilise HTTP Range si le fichier partiel existe déjà.
     */
    suspend fun downloadAssetResumable(
        downloadPath: String,
        destinationPartial: File,
        expectedBytes: Long,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> },
    ): OnlineResult<File>

    suspend fun pushProgress(
        bearerToken: String,
        snapshot: ProgressSyncSnapshot,
    ): OnlineResult<ProgressSyncAck>

    suspend fun pullProgress(bearerToken: String): OnlineResult<ProgressSyncSnapshot>
}
