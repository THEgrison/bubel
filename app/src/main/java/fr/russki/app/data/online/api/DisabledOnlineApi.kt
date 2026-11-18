package fr.russki.app.data.online.api

import fr.russki.app.data.online.OnlineError
import fr.russki.app.data.online.OnlineResult
import java.io.File

/** Implémentation no-op lorsque aucun serveur n’est configuré. */
class DisabledOnlineApi : RusskiOnlineApi {
    override val isConfigured: Boolean = false

    override suspend fun fetchCatalog(): OnlineResult<RemoteCatalog> =
        OnlineResult.Err(OnlineError.disabled())

    override suspend fun fetchPackManifest(
        packId: String,
        manifestPath: String,
    ): OnlineResult<RemotePackManifest> = OnlineResult.Err(OnlineError.disabled())

    override suspend fun downloadAssetResumable(
        downloadPath: String,
        destinationPartial: File,
        expectedBytes: Long,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ): OnlineResult<File> = OnlineResult.Err(OnlineError.disabled())

    override suspend fun pushProgress(
        bearerToken: String,
        snapshot: ProgressSyncSnapshot,
    ): OnlineResult<ProgressSyncAck> = OnlineResult.Err(OnlineError.disabled())

    override suspend fun pullProgress(bearerToken: String): OnlineResult<ProgressSyncSnapshot> =
        OnlineResult.Err(OnlineError.disabled())
}
