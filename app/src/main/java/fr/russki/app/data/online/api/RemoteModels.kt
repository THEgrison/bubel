package fr.russki.app.data.online.api

data class RemoteCatalog(
    val apiVersion: String,
    val catalogVersion: String,
    val generatedAtEpochMs: Long,
    val lessons: List<RemoteLesson>,
    val packs: List<RemotePackSummary>,
)

data class RemoteLesson(
    val id: String,
    val titleFr: String,
    val descriptionFr: String,
    val kind: String,
    val moduleId: String,
    val sortOrder: Int,
    val minAppVersionCode: Int,
    val packIds: List<String>,
)

data class RemotePackSummary(
    val id: String,
    val titleFr: String,
    val descriptionFr: String,
    val version: String,
    val totalBytes: Long,
    val sha256: String,
    val isOptional: Boolean,
    val manifestPath: String,
)

data class RemotePackManifest(
    val packId: String,
    val version: String,
    val titleFr: String,
    val totalBytes: Long,
    val sha256OfManifest: String?,
    val assets: List<RemotePackAsset>,
)

data class RemotePackAsset(
    val id: String,
    val relativePath: String,
    val byteSize: Long,
    val sha256: String,
    val downloadPath: String,
)

data class ProgressSyncSnapshot(
    val schemaVersion: Int,
    val deviceInstallId: String,
    val exportedAtEpochMs: Long,
    val lessonProgress: List<LessonProgressDto>,
    val srsCards: List<SrsCardDto>,
)

data class LessonProgressDto(
    val lessonId: String,
    val completedExerciseCount: Int,
    val completedAtEpochMs: Long?,
)

data class SrsCardDto(
    val cardId: String,
    val nextReviewEpochMs: Long,
    val intervalDays: Double,
    val easeFactor: Double,
    val repetitionCount: Int,
    val lapseCount: Int,
    val learningState: String,
)

data class ProgressSyncAck(
    val accepted: Boolean,
    val serverRevision: String?,
)
