package fr.russki.app.data.online.api

import fr.russki.app.data.online.OnlineError
import fr.russki.app.data.online.OnlineResult
import fr.russki.app.data.online.download.PathSafety
import org.json.JSONArray
import org.json.JSONObject

/** Parsing + validation stricte des contrats API v1. */
object RemoteJson {

    private val SHA256_HEX = Regex("^[a-fA-F0-9]{64}$")

    fun parseCatalog(raw: String): OnlineResult<RemoteCatalog> =
        runCatching {
            val o = JSONObject(raw)
            val apiVersion = o.reqString("apiVersion")
            val catalogVersion = o.reqString("catalogVersion")
            val generatedAt = o.getLong("generatedAtEpochMs")
            require(generatedAt > 0L) { "generatedAtEpochMs invalide" }
            val lessons = o.getJSONArray("lessons").mapObjects { parseLesson(it) }
            val packs = o.getJSONArray("packs").mapObjects { parsePackSummary(it) }
            RemoteCatalog(
                apiVersion = apiVersion,
                catalogVersion = catalogVersion,
                generatedAtEpochMs = generatedAt,
                lessons = lessons,
                packs = packs,
            )
        }.toPayloadResult()

    fun parseManifest(raw: String): OnlineResult<RemotePackManifest> =
        runCatching {
            val o = JSONObject(raw)
            val packId = o.reqString("packId")
            val version = o.reqString("version")
            val titleFr = o.reqString("titleFr")
            val totalBytes = o.getLong("totalBytes")
            require(totalBytes >= 0L) { "totalBytes négatif" }
            val shaManifest = if (o.isNull("sha256OfManifest")) {
                null
            } else {
                o.optString("sha256OfManifest").trim()
                    .takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
            }
            if (shaManifest != null) requireSha(shaManifest)
            val assets = o.getJSONArray("assets").mapObjects { parseAsset(it) }
            require(assets.isNotEmpty()) { "manifeste sans assets" }
            require(assets.map { it.id }.toSet().size == assets.size) { "ids d'assets non uniques" }
            val sum = assets.sumOf { it.byteSize }
            require(sum == totalBytes) {
                "totalBytes ($totalBytes) ≠ somme des assets ($sum)"
            }
            RemotePackManifest(
                packId = packId,
                version = version,
                titleFr = titleFr,
                totalBytes = totalBytes,
                sha256OfManifest = shaManifest,
                assets = assets,
            )
        }.toPayloadResult()

    fun parseSyncAck(raw: String): OnlineResult<ProgressSyncAck> =
        runCatching {
            val o = JSONObject(raw)
            ProgressSyncAck(
                accepted = o.getBoolean("accepted"),
                serverRevision = o.optString("serverRevision").takeIf { it.isNotBlank() },
            )
        }.toPayloadResult()

    fun encodeSnapshot(snapshot: ProgressSyncSnapshot): String {
        val root = JSONObject()
            .put("schemaVersion", snapshot.schemaVersion)
            .put("deviceInstallId", snapshot.deviceInstallId)
            .put("exportedAtEpochMs", snapshot.exportedAtEpochMs)
        val lessons = JSONArray()
        snapshot.lessonProgress.forEach { lp ->
            lessons.put(
                JSONObject()
                    .put("lessonId", lp.lessonId)
                    .put("completedExerciseCount", lp.completedExerciseCount)
                    .put("completedAtEpochMs", lp.completedAtEpochMs),
            )
        }
        val cards = JSONArray()
        snapshot.srsCards.forEach { c ->
            cards.put(
                JSONObject()
                    .put("cardId", c.cardId)
                    .put("nextReviewEpochMs", c.nextReviewEpochMs)
                    .put("intervalDays", c.intervalDays)
                    .put("easeFactor", c.easeFactor)
                    .put("repetitionCount", c.repetitionCount)
                    .put("lapseCount", c.lapseCount)
                    .put("learningState", c.learningState),
            )
        }
        root.put("lessonProgress", lessons)
        root.put("srsCards", cards)
        return root.toString()
    }

    fun parseSnapshot(raw: String): OnlineResult<ProgressSyncSnapshot> =
        runCatching {
            val o = JSONObject(raw)
            ProgressSyncSnapshot(
                schemaVersion = o.getInt("schemaVersion"),
                deviceInstallId = o.reqString("deviceInstallId"),
                exportedAtEpochMs = o.getLong("exportedAtEpochMs"),
                lessonProgress = o.getJSONArray("lessonProgress").mapObjects { lp ->
                    LessonProgressDto(
                        lessonId = lp.reqString("lessonId"),
                        completedExerciseCount = lp.getInt("completedExerciseCount"),
                        completedAtEpochMs = if (lp.isNull("completedAtEpochMs")) {
                            null
                        } else {
                            lp.getLong("completedAtEpochMs")
                        },
                    )
                },
                srsCards = o.getJSONArray("srsCards").mapObjects { c ->
                    SrsCardDto(
                        cardId = c.reqString("cardId"),
                        nextReviewEpochMs = c.getLong("nextReviewEpochMs"),
                        intervalDays = c.getDouble("intervalDays"),
                        easeFactor = c.getDouble("easeFactor"),
                        repetitionCount = c.getInt("repetitionCount"),
                        lapseCount = c.getInt("lapseCount"),
                        learningState = c.reqString("learningState"),
                    )
                },
            )
        }.toPayloadResult()

    fun requireSha(value: String) {
        require(SHA256_HEX.matches(value)) { "sha256 invalide: $value" }
    }

    private fun parseLesson(o: JSONObject): RemoteLesson =
        RemoteLesson(
            id = o.reqString("id"),
            titleFr = o.reqString("titleFr"),
            descriptionFr = o.reqString("descriptionFr"),
            kind = o.reqString("kind"),
            moduleId = o.reqString("moduleId"),
            sortOrder = o.getInt("sortOrder"),
            minAppVersionCode = o.optInt("minAppVersionCode", 1),
            packIds = o.optJSONArray("packIds")?.toStringList().orEmpty(),
        )

    private fun parsePackSummary(o: JSONObject): RemotePackSummary {
        val sha = o.reqString("sha256")
        requireSha(sha)
        return RemotePackSummary(
            id = o.reqString("id"),
            titleFr = o.reqString("titleFr"),
            descriptionFr = o.reqString("descriptionFr"),
            version = o.reqString("version"),
            totalBytes = o.getLong("totalBytes").also { require(it >= 0) },
            sha256 = sha.lowercase(),
            isOptional = o.optBoolean("isOptional", true),
            manifestPath = o.reqString("manifestPath"),
        )
    }

    private fun parseAsset(o: JSONObject): RemotePackAsset {
        val sha = o.reqString("sha256")
        requireSha(sha)
        val size = o.getLong("byteSize")
        require(size > 0L) { "byteSize doit être > 0" }
        val id = o.reqString("id")
        val relativePath = o.reqString("relativePath")
        require(PathSafety.isSafeAssetId(id)) { "id d'asset non fiable: $id" }
        require(PathSafety.isSafeRelativePath(relativePath)) {
            "relativePath non fiable: $relativePath"
        }
        return RemotePackAsset(
            id = id,
            relativePath = relativePath,
            byteSize = size,
            sha256 = sha.lowercase(),
            downloadPath = o.reqString("downloadPath"),
        )
    }

    private fun JSONObject.reqString(key: String): String {
        val v = optString(key, "").trim()
        require(v.isNotEmpty()) { "champ requis manquant: $key" }
        return v
    }

    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        buildList {
            for (i in 0 until length()) {
                add(transform(getJSONObject(i)))
            }
        }

    private fun JSONArray.toStringList(): List<String> =
        buildList {
            for (i in 0 until length()) add(getString(i))
        }

    private fun <T> Result<T>.toPayloadResult(): OnlineResult<T> =
        fold(
            onSuccess = { OnlineResult.Ok(it) },
            onFailure = {
                OnlineResult.Err(
                    OnlineError(
                        code = OnlineError.INVALID_PAYLOAD,
                        message = it.message ?: "Réponse API invalide",
                        cause = it,
                    ),
                )
            },
        )
}
