package fr.russki.app.data.online.catalog

import android.content.Context
import fr.russki.app.data.online.OnlineError
import fr.russki.app.data.online.OnlineResult
import fr.russki.app.data.online.api.RemoteCatalog
import fr.russki.app.data.online.api.RemoteJson
import fr.russki.app.data.online.api.RusskiOnlineApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Catalogue distant avec cache disque local.
 * En cas d’échec réseau, retourne le cache s’il existe — sans toucher aux leçons embarquées.
 */
class RemoteCatalogRepository(
    private val context: Context,
    private val api: RusskiOnlineApi,
) {

    suspend fun refresh(): OnlineResult<RemoteCatalog> = withContext(Dispatchers.IO) {
        when (val remote = api.fetchCatalog()) {
            is OnlineResult.Ok -> {
                cacheFile().writeText(encodeForCache(remote.value))
                remote
            }
            is OnlineResult.Err -> {
                val cached = readCache()
                if (cached != null) {
                    OnlineResult.Ok(cached)
                } else {
                    remote
                }
            }
        }
    }

    suspend fun cachedOrNull(): RemoteCatalog? = withContext(Dispatchers.IO) { readCache() }

    suspend fun loadPreferCache(): OnlineResult<RemoteCatalog> = withContext(Dispatchers.IO) {
        readCache()?.let { return@withContext OnlineResult.Ok(it) }
        refresh()
    }

    private fun readCache(): RemoteCatalog? {
        val file = cacheFile()
        if (!file.exists()) return null
        return when (val parsed = RemoteJson.parseCatalog(file.readText())) {
            is OnlineResult.Ok -> parsed.value
            is OnlineResult.Err -> null
        }
    }

    private fun cacheFile(): File =
        File(context.filesDir, "cache/remote_catalog_v1.json").also {
            it.parentFile?.mkdirs()
        }

    private fun encodeForCache(catalog: RemoteCatalog): String {
        // Réutilise le format API pour relecture via RemoteJson.parseCatalog
        val lessons = catalog.lessons.joinToString(",") { lesson ->
            """{"id":${jsonStr(lesson.id)},"titleFr":${jsonStr(lesson.titleFr)},"descriptionFr":${jsonStr(lesson.descriptionFr)},"kind":${jsonStr(lesson.kind)},"moduleId":${jsonStr(lesson.moduleId)},"sortOrder":${lesson.sortOrder},"minAppVersionCode":${lesson.minAppVersionCode},"packIds":[${lesson.packIds.joinToString(",") { jsonStr(it) }}]}"""
        }
        val packs = catalog.packs.joinToString(",") { pack ->
            """{"id":${jsonStr(pack.id)},"titleFr":${jsonStr(pack.titleFr)},"descriptionFr":${jsonStr(pack.descriptionFr)},"version":${jsonStr(pack.version)},"totalBytes":${pack.totalBytes},"sha256":${jsonStr(pack.sha256)},"isOptional":${pack.isOptional},"manifestPath":${jsonStr(pack.manifestPath)}}"""
        }
        return """{"apiVersion":${jsonStr(catalog.apiVersion)},"catalogVersion":${jsonStr(catalog.catalogVersion)},"generatedAtEpochMs":${catalog.generatedAtEpochMs},"lessons":[$lessons],"packs":[$packs]}"""
    }

    private fun jsonStr(value: String): String =
        org.json.JSONObject.quote(value)

    fun unavailableMessage(): String =
        OnlineError.disabled().message
}
