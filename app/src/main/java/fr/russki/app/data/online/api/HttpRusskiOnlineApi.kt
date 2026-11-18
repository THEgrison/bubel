package fr.russki.app.data.online.api

import fr.russki.app.data.online.OnlineConfig
import fr.russki.app.data.online.OnlineError
import fr.russki.app.data.online.OnlineResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class HttpRusskiOnlineApi(
    private val config: OnlineConfig,
    private val client: OkHttpClient = defaultClient(config),
) : RusskiOnlineApi {

    override val isConfigured: Boolean = config.isConfigured

    private val base: String = config.apiBaseUrl.trimEnd('/')

    override suspend fun fetchCatalog(): OnlineResult<RemoteCatalog> =
        getJson("$base/v1/catalog") { RemoteJson.parseCatalog(it) }

    override suspend fun fetchPackManifest(
        packId: String,
        manifestPath: String,
    ): OnlineResult<RemotePackManifest> {
        val path = if (manifestPath.startsWith("http")) {
            manifestPath
        } else {
            base + (if (manifestPath.startsWith("/")) manifestPath else "/$manifestPath")
        }
        return getJson(path) { RemoteJson.parseManifest(it) }
    }

    override suspend fun downloadAssetResumable(
        downloadPath: String,
        destinationPartial: File,
        expectedBytes: Long,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ): OnlineResult<File> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext OnlineResult.Err(OnlineError.disabled())
        val url = resolveUrl(downloadPath)
        destinationPartial.parentFile?.mkdirs()
        val existing = if (destinationPartial.exists()) destinationPartial.length() else 0L
        if (existing > expectedBytes) {
            destinationPartial.delete()
        }
        val start = if (destinationPartial.exists()) destinationPartial.length() else 0L
        if (start == expectedBytes && expectedBytes > 0L) {
            onProgress(start, expectedBytes)
            return@withContext OnlineResult.Ok(destinationPartial)
        }

        val requestBuilder = Request.Builder().url(url).get()
        if (start > 0L) {
            requestBuilder.header("Range", "bytes=$start-")
        }

        try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                when (response.code) {
                    200, 206 -> {
                        val append = response.code == 206
                        if (!append && destinationPartial.exists()) {
                            destinationPartial.delete()
                        }
                        val body = response.body ?: return@withContext OnlineResult.Err(
                            OnlineError(OnlineError.NETWORK, "Corps de réponse vide", response.code),
                        )
                        body.byteStream().use { input ->
                            java.io.FileOutputStream(destinationPartial, append).use { output ->
                                val buffer = ByteArray(DEFAULT_BUFFER)
                                var downloaded = if (append) start else 0L
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read <= 0) break
                                    output.write(buffer, 0, read)
                                    downloaded += read
                                    onProgress(downloaded, expectedBytes)
                                }
                                output.flush()
                            }
                        }
                        val finalSize = destinationPartial.length()
                        if (finalSize != expectedBytes) {
                            return@withContext OnlineResult.Err(
                                OnlineError(
                                    OnlineError.INVALID_PAYLOAD,
                                    "Taille téléchargée ($finalSize) ≠ attendue ($expectedBytes)",
                                    response.code,
                                ),
                            )
                        }
                        OnlineResult.Ok(destinationPartial)
                    }
                    404 -> OnlineResult.Err(
                        OnlineError(OnlineError.NOT_FOUND, "Asset introuvable", 404),
                    )
                    else -> OnlineResult.Err(
                        OnlineError(
                            OnlineError.NETWORK,
                            "HTTP ${response.code} lors du téléchargement",
                            response.code,
                        ),
                    )
                }
            }
        } catch (io: IOException) {
            OnlineResult.Err(
                OnlineError(OnlineError.NETWORK, io.message ?: "Échec réseau", cause = io),
            )
        }
    }

    override suspend fun pushProgress(
        bearerToken: String,
        snapshot: ProgressSyncSnapshot,
    ): OnlineResult<ProgressSyncAck> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext OnlineResult.Err(OnlineError.disabled())
        if (bearerToken.isBlank()) {
            return@withContext OnlineResult.Err(
                OnlineError(
                    OnlineError.SYNC_UNAUTHORIZED,
                    "Aucun jeton sync — flux d’auth non déployé.",
                ),
            )
        }
        val body = RemoteJson.encodeSnapshot(snapshot)
            .toRequestBody(JSON_MEDIA)
        val request = Request.Builder()
            .url("$base/v1/sync/progress")
            .header("Authorization", "Bearer $bearerToken")
            .post(body)
            .build()
        executeJson(request) { RemoteJson.parseSyncAck(it) }
    }

    override suspend fun pullProgress(bearerToken: String): OnlineResult<ProgressSyncSnapshot> =
        withContext(Dispatchers.IO) {
            if (!isConfigured) return@withContext OnlineResult.Err(OnlineError.disabled())
            if (bearerToken.isBlank()) {
                return@withContext OnlineResult.Err(
                    OnlineError(
                        OnlineError.SYNC_UNAUTHORIZED,
                        "Aucun jeton sync — flux d’auth non déployé.",
                    ),
                )
            }
            val request = Request.Builder()
                .url("$base/v1/sync/progress")
                .header("Authorization", "Bearer $bearerToken")
                .get()
                .build()
            executeJson(request) { RemoteJson.parseSnapshot(it) }
        }

    private suspend fun <T> getJson(
        url: String,
        parse: (String) -> OnlineResult<T>,
    ): OnlineResult<T> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext OnlineResult.Err(OnlineError.disabled())
        val request = Request.Builder().url(url).get().build()
        executeJson(request, parse)
    }

    private fun <T> executeJson(
        request: Request,
        parse: (String) -> OnlineResult<T>,
    ): OnlineResult<T> {
        return try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                when {
                    response.code == 404 -> OnlineResult.Err(
                        OnlineError(OnlineError.NOT_FOUND, "Ressource introuvable", 404),
                    )
                    response.code == 401 || response.code == 403 -> OnlineResult.Err(
                        OnlineError(
                            OnlineError.SYNC_UNAUTHORIZED,
                            "Non autorisé",
                            response.code,
                        ),
                    )
                    !response.isSuccessful -> OnlineResult.Err(
                        OnlineError(
                            OnlineError.NETWORK,
                            "HTTP ${response.code}",
                            response.code,
                        ),
                    )
                    raw.isBlank() -> OnlineResult.Err(
                        OnlineError(OnlineError.INVALID_PAYLOAD, "Réponse vide", response.code),
                    )
                    else -> parse(raw)
                }
            }
        } catch (io: IOException) {
            OnlineResult.Err(
                OnlineError(OnlineError.NETWORK, io.message ?: "Échec réseau", cause = io),
            )
        }
    }

    private fun resolveUrl(path: String): String =
        if (path.startsWith("http://") || path.startsWith("https://")) {
            path
        } else {
            base + (if (path.startsWith("/")) path else "/$path")
        }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
        private const val DEFAULT_BUFFER = 64 * 1024

        fun defaultClient(config: OnlineConfig): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(config.connectTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(config.readTimeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(config.writeTimeoutMs, TimeUnit.MILLISECONDS)
                .followRedirects(true)
                .build()
    }
}
