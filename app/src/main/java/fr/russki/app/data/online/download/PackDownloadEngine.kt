package fr.russki.app.data.online.download

import android.content.Context
import fr.russki.app.data.content.InstalledPackState
import fr.russki.app.data.local.RusskiDatabase
import fr.russki.app.data.local.entity.AudioAssetEntity
import fr.russki.app.data.local.entity.ContentPackEntity
import fr.russki.app.data.local.entity.ContentPackMemberEntity
import fr.russki.app.data.local.entity.InstalledPackEntity
import fr.russki.app.data.online.OnlineError
import fr.russki.app.data.online.OnlineResult
import fr.russki.app.data.online.api.RemotePackManifest
import fr.russki.app.data.online.api.RusskiOnlineApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Télécharge un pack audio : manifeste → fichiers temp → checksum → install atomique.
 * Ne marque le pack « Installed » qu’après validation complète de tous les assets.
 */
class PackDownloadEngine(
    private val context: Context,
    private val db: RusskiDatabase,
    private val api: RusskiOnlineApi,
    private val limiter: DownloadLimiter,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    data class Progress(
        val packId: String,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val currentAssetId: String?,
    )

    suspend fun downloadAndInstall(
        packId: String,
        manifestPath: String,
        packTitleFr: String,
        packDescriptionFr: String,
        packVersion: String,
        onProgress: (Progress) -> Unit = {},
    ): OnlineResult<Unit> = withContext(Dispatchers.IO) {
        if (!api.isConfigured) return@withContext OnlineResult.Err(OnlineError.disabled())

        markState(
            packId = packId,
            state = InstalledPackState.DOWNLOADING,
            downloaded = 0L,
            error = null,
        )

        val manifestResult = api.fetchPackManifest(packId, manifestPath)
        val manifest = when (manifestResult) {
            is OnlineResult.Ok -> manifestResult.value
            is OnlineResult.Err -> {
                markState(packId, InstalledPackState.FAILED, 0L, manifestResult.error.message)
                return@withContext manifestResult
            }
        }

        val stagingRoot = stagingDir(packId)
        stagingRoot.mkdirs()
        val installRoot = installDir(packId)
        var downloadedTotal = 0L

        try {
            for (asset in manifest.assets) {
                if (!PathSafety.isSafeAssetId(asset.id) ||
                    !PathSafety.isSafeRelativePath(asset.relativePath)
                ) {
                    val err = OnlineError(
                        OnlineError.INVALID_PAYLOAD,
                        "Chemin d'asset non fiable: ${asset.id} / ${asset.relativePath}",
                    )
                    markState(packId, InstalledPackState.FAILED, downloadedTotal, err.message)
                    stagingRoot.deleteRecursively()
                    return@withContext OnlineResult.Err(err)
                }
                val partial = File(stagingRoot, "${asset.id}.partial")
                val download = limiter.withPermit {
                    api.downloadAssetResumable(
                        downloadPath = asset.downloadPath,
                        destinationPartial = partial,
                        expectedBytes = asset.byteSize,
                    ) { down, _ ->
                        onProgress(
                            Progress(
                                packId = packId,
                                downloadedBytes = downloadedTotal + down,
                                totalBytes = manifest.totalBytes,
                                currentAssetId = asset.id,
                            ),
                        )
                    }
                }
                when (download) {
                    is OnlineResult.Err -> {
                        markState(packId, InstalledPackState.FAILED, downloadedTotal, download.error.message)
                        return@withContext download
                    }
                    is OnlineResult.Ok -> {
                        if (!Checksums.matches(partial, asset.sha256, asset.byteSize)) {
                            partial.delete()
                            val err = OnlineError(
                                OnlineError.CHECKSUM,
                                "Checksum invalide pour ${asset.id}",
                            )
                            markState(packId, InstalledPackState.FAILED, downloadedTotal, err.message)
                            return@withContext OnlineResult.Err(err)
                        }
                        downloadedTotal += asset.byteSize
                        markState(
                            packId = packId,
                            state = InstalledPackState.DOWNLOADING,
                            downloaded = downloadedTotal,
                            error = null,
                        )
                    }
                }
            }

            // Install atomique : préparer un dossier neuf, déplacer, puis basculer.
            val tmpInstall = File(context.filesDir, "packs/.installing-$packId-${clock()}")
            if (tmpInstall.exists()) tmpInstall.deleteRecursively()
            tmpInstall.mkdirs()

            for (asset in manifest.assets) {
                val partial = File(stagingRoot, "${asset.id}.partial")
                val target = PathSafety.resolveUnder(tmpInstall, asset.relativePath)
                if (target == null) {
                    tmpInstall.deleteRecursively()
                    val err = OnlineError(
                        OnlineError.INVALID_PAYLOAD,
                        "Chemin d'install refusé: ${asset.relativePath}",
                    )
                    markState(packId, InstalledPackState.FAILED, downloadedTotal, err.message)
                    return@withContext OnlineResult.Err(err)
                }
                target.parentFile?.mkdirs()
                if (!partial.renameTo(target)) {
                    partial.copyTo(target, overwrite = true)
                    partial.delete()
                }
                if (!Checksums.matches(target, asset.sha256, asset.byteSize)) {
                    tmpInstall.deleteRecursively()
                    val err = OnlineError(
                        OnlineError.CHECKSUM,
                        "Checksum post-install invalide: ${asset.id}",
                    )
                    markState(packId, InstalledPackState.FAILED, downloadedTotal, err.message)
                    return@withContext OnlineResult.Err(err)
                }
            }

            if (installRoot.exists()) installRoot.deleteRecursively()
            if (!tmpInstall.renameTo(installRoot)) {
                tmpInstall.copyRecursively(installRoot, overwrite = true)
                tmpInstall.deleteRecursively()
            }
            stagingRoot.deleteRecursively()

            persistCatalog(manifest, packTitleFr, packDescriptionFr, packVersion, installRoot)
            markState(
                packId = packId,
                state = InstalledPackState.INSTALLED,
                downloaded = manifest.totalBytes,
                error = null,
                localDir = installRoot.absolutePath,
            )
            OnlineResult.Ok(Unit)
        } catch (t: Throwable) {
            val err = OnlineError(OnlineError.NETWORK, t.message ?: "Téléchargement interrompu", cause = t)
            markState(packId, InstalledPackState.FAILED, downloadedTotal, err.message)
            OnlineResult.Err(err)
        }
    }

    /** Nettoie les téléchargements interrompus d’un pack (fichiers .partial). */
    suspend fun cleanupInterrupted(packId: String) = withContext(Dispatchers.IO) {
        stagingDir(packId).deleteRecursively()
        val installed = db.installedPackDao().get(packId)
        if (installed?.state == InstalledPackState.DOWNLOADING ||
            installed?.state == InstalledPackState.FAILED
        ) {
            markState(packId, InstalledPackState.NOT_INSTALLED, 0L, null)
        }
    }

    suspend fun cleanupAllOrphans() = withContext(Dispatchers.IO) {
        val root = File(context.filesDir, "downloads")
        if (!root.exists()) return@withContext
        root.listFiles()?.forEach { packDir ->
            val state = db.installedPackDao().get(packDir.name)?.state
            if (state != InstalledPackState.DOWNLOADING) {
                // garder les partials seulement si download en cours
                if (state == InstalledPackState.FAILED || state == InstalledPackState.NOT_INSTALLED) {
                    packDir.deleteRecursively()
                }
            }
        }
    }

    private suspend fun persistCatalog(
        manifest: RemotePackManifest,
        titleFr: String,
        descriptionFr: String,
        version: String,
        installRoot: File,
    ) {
        db.contentPackDao().insertPackWithMembers(
            pack = ContentPackEntity(
                id = manifest.packId,
                titleFr = titleFr.ifBlank { manifest.titleFr },
                descriptionFr = descriptionFr,
                isOptional = true,
                isBundled = false,
                totalBytes = manifest.totalBytes,
                version = version.ifBlank { manifest.version },
            ),
            members = manifest.assets.map { asset ->
                ContentPackMemberEntity(
                    packId = manifest.packId,
                    memberType = "audio",
                    memberId = asset.id,
                )
            },
        )
        db.audioAssetDao().insertIgnore(
            manifest.assets.map { asset ->
                AudioAssetEntity(
                    id = asset.id,
                    relativePath = File(installRoot, asset.relativePath).absolutePath,
                    titleFr = asset.id,
                    byteSize = asset.byteSize,
                    durationMs = null,
                    contentRefId = null,
                    requiredPackId = manifest.packId,
                )
            },
        )
    }

    private suspend fun markState(
        packId: String,
        state: String,
        downloaded: Long,
        error: String?,
        localDir: String? = null,
    ) {
        val existing = db.installedPackDao().get(packId)
        db.installedPackDao().upsert(
            InstalledPackEntity(
                packId = packId,
                state = state,
                downloadedBytes = downloaded,
                installedAtEpochMs = if (state == InstalledPackState.INSTALLED) {
                    clock()
                } else {
                    existing?.installedAtEpochMs
                },
                localDir = localDir ?: existing?.localDir,
                lastError = error,
            ),
        )
    }

    private fun stagingDir(packId: String): File =
        File(context.filesDir, "downloads/$packId")

    private fun installDir(packId: String): File =
        File(context.filesDir, "packs/$packId")
}
