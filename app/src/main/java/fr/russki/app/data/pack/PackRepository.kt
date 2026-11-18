package fr.russki.app.data.pack

import android.content.Context
import fr.russki.app.data.content.ContentSource
import fr.russki.app.data.content.InstalledPackState
import fr.russki.app.data.content.PackIds
import fr.russki.app.data.local.RusskiDatabase
import fr.russki.app.data.local.entity.InstalledPackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class PackStatusUi(
    val packId: String,
    val titleFr: String,
    val descriptionFr: String,
    val isOptional: Boolean,
    val isBundled: Boolean,
    val totalBytes: Long,
    val version: String,
    val state: String,
    val downloadedBytes: Long,
    val installedAtEpochMs: Long?,
    val lastError: String?,
    val availableLocally: Boolean,
    /** Chemin manifeste API si pack distant. */
    val manifestPath: String? = null,
    val remoteSha256: String? = null,
    val isRemote: Boolean = false,
) {
    val sizeLabel: String
        get() = when {
            totalBytes <= 0L -> "inclus"
            totalBytes < 1024L -> "$totalBytes o"
            totalBytes < 1024L * 1024L -> "${totalBytes / 1024} Ko"
            else -> String.format("%.1f Mo", totalBytes / (1024.0 * 1024.0))
        }

    val stateLabelFr: String
        get() = when (state) {
            InstalledPackState.INSTALLED -> "Installé"
            InstalledPackState.DOWNLOADING -> "Installation…"
            InstalledPackState.FAILED -> "Échec"
            else -> "Non installé"
        }
}

/**
 * Gestion des packs hors ligne.
 * L'installation du pack démo copie depuis `assets/` (aucune requête réseau).
 * La suppression d'un pack optionnel n'efface jamais la progression utilisateur.
 */
class PackRepository(
    private val context: Context,
    private val db: RusskiDatabase,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    suspend fun listPacks(): List<PackStatusUi> = withContext(Dispatchers.IO) {
        val catalog = db.contentPackDao().getAll()
        val installed = db.installedPackDao().getAll().associateBy { it.packId }
        catalog.map { pack ->
            val status = installed[pack.id]
            PackStatusUi(
                packId = pack.id,
                titleFr = pack.titleFr,
                descriptionFr = pack.descriptionFr,
                isOptional = pack.isOptional,
                isBundled = pack.isBundled,
                totalBytes = pack.totalBytes,
                version = pack.version,
                state = status?.state ?: InstalledPackState.NOT_INSTALLED,
                downloadedBytes = status?.downloadedBytes ?: 0L,
                installedAtEpochMs = status?.installedAtEpochMs,
                lastError = status?.lastError,
                availableLocally = pack.isBundled || assetPackExists(pack.id),
            )
        }
    }

    /**
     * Installe un pack optionnel depuis les assets locaux (fonctionne hors ligne).
     * Ne remplace aucun contenu pédagogique déjà présent (INSERT IGNORE côté seed).
     */
    suspend fun installOptionalPack(packId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val pack = db.contentPackDao().get(packId)
            ?: return@withContext Result.failure(IllegalArgumentException("Pack inconnu: $packId"))
        if (!pack.isOptional) {
            return@withContext Result.failure(
                IllegalStateException("Le pack de base ne peut pas être réinstallé manuellement."),
            )
        }
        if (!assetPackExists(packId)) {
            return@withContext Result.failure(
                IllegalStateException(
                    "Pack indisponible hors ligne. Connectez-vous pour le télécharger " +
                        "(aucun téléchargement distant dans cette version).",
                ),
            )
        }

        db.installedPackDao().upsert(
            InstalledPackEntity(
                packId = packId,
                state = InstalledPackState.DOWNLOADING,
                downloadedBytes = 0L,
                installedAtEpochMs = null,
                localDir = null,
                lastError = null,
            ),
        )

        runCatching {
            val target = packDir(packId)
            if (target.exists()) {
                target.deleteRecursively()
            }
            target.mkdirs()
            copyAssetDir("packs/$packId", target)
            val bytes = target.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            db.installedPackDao().upsert(
                InstalledPackEntity(
                    packId = packId,
                    state = InstalledPackState.INSTALLED,
                    downloadedBytes = bytes,
                    installedAtEpochMs = clock(),
                    localDir = target.absolutePath,
                    lastError = null,
                ),
            )
        }.onFailure { error ->
            db.installedPackDao().upsert(
                InstalledPackEntity(
                    packId = packId,
                    state = InstalledPackState.FAILED,
                    downloadedBytes = 0L,
                    installedAtEpochMs = null,
                    localDir = null,
                    lastError = error.message ?: "Installation impossible",
                ),
            )
        }
    }

    /**
     * Supprime un pack optionnel (fichiers + état).
     * Ne touche pas quiz_sessions, srs_cards, flashcard_review_history, user_lesson_progress.
     */
    suspend fun deleteOptionalPack(packId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val pack = db.contentPackDao().get(packId)
            ?: return@withContext Result.failure(IllegalArgumentException("Pack inconnu: $packId"))
        if (!pack.isOptional) {
            return@withContext Result.failure(
                IllegalStateException("Le contenu de base ne peut pas être supprimé."),
            )
        }
        packDir(packId).deleteRecursively()
        db.installedPackDao().upsert(
            InstalledPackEntity(
                packId = packId,
                state = InstalledPackState.NOT_INSTALLED,
                downloadedBytes = 0L,
                installedAtEpochMs = null,
                localDir = null,
                lastError = null,
            ),
        )
        Result.success(Unit)
    }

    suspend fun isPackInstalled(packId: String): Boolean = withContext(Dispatchers.IO) {
        db.installedPackDao().get(packId)?.state == InstalledPackState.INSTALLED
    }

    fun localAudioFile(relativePath: String): File? {
        val inPacks = File(context.filesDir, relativePath)
        if (inPacks.exists()) return inPacks
        return null
    }

    private fun packDir(packId: String): File =
        File(context.filesDir, "packs/$packId")

    private fun assetPackExists(packId: String): Boolean =
        runCatching {
            context.assets.open("packs/$packId/manifest.json").use { true }
        }.getOrDefault(false)

    private fun copyAssetDir(assetPath: String, target: File) {
        val children = context.assets.list(assetPath).orEmpty()
        if (children.isEmpty()) {
            // fichier unique
            context.assets.open(assetPath).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            return
        }
        children.forEach { name ->
            val childAsset = "$assetPath/$name"
            val childTarget = File(target, name)
            val nested = context.assets.list(childAsset)
            if (nested != null && nested.isNotEmpty()) {
                childTarget.mkdirs()
                copyAssetDir(childAsset, childTarget)
            } else {
                context.assets.open(childAsset).use { input ->
                    childTarget.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }

    companion object {
        /** Marqueur documentaire : contenu téléchargé vs embarqué. */
        const val DOWNLOADED_SOURCE = ContentSource.DOWNLOADED
    }
}
