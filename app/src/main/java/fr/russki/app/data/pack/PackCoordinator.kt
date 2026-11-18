package fr.russki.app.data.pack

import android.content.Context
import fr.russki.app.data.content.InstalledPackState
import fr.russki.app.data.local.RusskiDatabase
import fr.russki.app.data.online.OnlineResult
import fr.russki.app.data.online.api.RemotePackSummary
import fr.russki.app.data.online.catalog.RemoteCatalogRepository
import fr.russki.app.data.online.download.PackDownloadEngine
import fr.russki.app.data.online.download.PackDownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fusionne packs locaux et catalogue distant.
 * L’échec réseau n’empêche jamais la liste / l’usage des packs déjà installés.
 */
class PackCoordinator(
    private val context: Context,
    private val local: PackRepository,
    private val remoteCatalog: RemoteCatalogRepository,
    private val downloadEngine: PackDownloadEngine,
    private val preferOfflineOnly: () -> Boolean = { false },
) {

    data class PackListResult(
        val packs: List<PackStatusUi>,
        val remoteNote: String?,
    )

    suspend fun listPacksMerged(): PackListResult = withContext(Dispatchers.IO) {
        val localPacks = local.listPacks().associateBy { it.packId }.toMutableMap()
        var remoteNote: String? = null

        if (!preferOfflineOnly()) {
            when (val remote = remoteCatalog.refresh()) {
                is OnlineResult.Ok -> {
                    remote.value.packs.forEach { summary ->
                        localPacks[summary.id] = mergeRemote(summary, localPacks[summary.id])
                    }
                    remoteNote = "Catalogue distant v${remote.value.catalogVersion}"
                }
                is OnlineResult.Err -> {
                    val cached = remoteCatalog.cachedOrNull()
                    if (cached != null) {
                        cached.packs.forEach { summary ->
                            localPacks[summary.id] = mergeRemote(summary, localPacks[summary.id])
                        }
                        remoteNote = "Catalogue distant (cache) — ${remote.error.message}"
                    } else {
                        remoteNote = remote.error.message
                    }
                }
            }
        } else {
            remoteNote = "Mode priorité hors ligne : catalogue distant ignoré."
        }

        PackListResult(
            packs = localPacks.values.sortedWith(
                compareByDescending<PackStatusUi> { it.isBundled }
                    .thenBy { it.titleFr },
            ),
            remoteNote = remoteNote,
        )
    }

    /**
     * Installe depuis assets locaux si possible, sinon enqueue WorkManager (réseau).
     */
    suspend fun install(pack: PackStatusUi): String = withContext(Dispatchers.IO) {
        if (pack.availableLocally && pack.manifestPath == null) {
            val result = local.installOptionalPack(pack.packId)
            return@withContext result.fold(
                onSuccess = { "Pack installé hors ligne (assets)." },
                onFailure = { it.message ?: "Installation locale impossible." },
            )
        }
        val manifestPath = pack.manifestPath
            ?: return@withContext "Pack distant sans manifeste."
        PackDownloadWorker.enqueue(
            context = context,
            packId = pack.packId,
            manifestPath = manifestPath,
            titleFr = pack.titleFr,
            descriptionFr = pack.descriptionFr,
            version = pack.version,
        )
        "Téléchargement planifié (WorkManager). Le pack ne sera disponible qu’après validation."
    }

    /** Installation synchrone (tests / avant-plan). */
    suspend fun installNow(pack: PackStatusUi): OnlineResult<Unit> {
        val manifestPath = pack.manifestPath
            ?: return OnlineResult.Err(
                fr.russki.app.data.online.OnlineError(
                    fr.russki.app.data.online.OnlineError.INVALID_PAYLOAD,
                    "Manifeste manquant",
                ),
            )
        return downloadEngine.downloadAndInstall(
            packId = pack.packId,
            manifestPath = manifestPath,
            packTitleFr = pack.titleFr,
            packDescriptionFr = pack.descriptionFr,
            packVersion = pack.version,
        )
    }

    suspend fun delete(packId: String): Result<Unit> {
        downloadEngine.cleanupInterrupted(packId)
        return local.deleteOptionalPack(packId)
    }

    private fun mergeRemote(summary: RemotePackSummary, existing: PackStatusUi?): PackStatusUi {
        return PackStatusUi(
            packId = summary.id,
            titleFr = summary.titleFr,
            descriptionFr = summary.descriptionFr,
            isOptional = summary.isOptional,
            isBundled = false,
            totalBytes = summary.totalBytes,
            version = summary.version,
            state = existing?.state ?: InstalledPackState.NOT_INSTALLED,
            downloadedBytes = existing?.downloadedBytes ?: 0L,
            installedAtEpochMs = existing?.installedAtEpochMs,
            lastError = existing?.lastError,
            availableLocally = existing?.state == InstalledPackState.INSTALLED,
            manifestPath = summary.manifestPath,
            remoteSha256 = summary.sha256,
            isRemote = true,
        )
    }
}
