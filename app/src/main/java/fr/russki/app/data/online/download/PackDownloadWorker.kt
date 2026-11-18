package fr.russki.app.data.online.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import fr.russki.app.data.online.OnlineResult
import fr.russki.app.di.ServiceLocator

/**
 * Tâche différée WorkManager pour télécharger un pack.
 * Une seule chaîne `russki-pack-downloads` limite le parallélisme global ;
 * le [DownloadLimiter] borne encore les downloads HTTP simultanés.
 */
class PackDownloadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val packId = inputData.getString(KEY_PACK_ID) ?: return Result.failure()
        val manifestPath = inputData.getString(KEY_MANIFEST_PATH) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE).orEmpty()
        val description = inputData.getString(KEY_DESCRIPTION).orEmpty()
        val version = inputData.getString(KEY_VERSION).orEmpty()

        val engine = ServiceLocator.require().packDownloadEngine
        return when (
            val outcome = engine.downloadAndInstall(
                packId = packId,
                manifestPath = manifestPath,
                packTitleFr = title,
                packDescriptionFr = description,
                packVersion = version,
            )
        ) {
            is OnlineResult.Ok -> Result.success()
            is OnlineResult.Err -> {
                val data = workDataOf(KEY_ERROR to outcome.error.message)
                if (outcome.error.code == fr.russki.app.data.online.OnlineError.NETWORK) {
                    Result.retry()
                } else {
                    Result.failure(data)
                }
            }
        }
    }

    companion object {
        const val UNIQUE_QUEUE = "russki-pack-downloads"
        const val KEY_PACK_ID = "packId"
        const val KEY_MANIFEST_PATH = "manifestPath"
        const val KEY_TITLE = "title"
        const val KEY_DESCRIPTION = "description"
        const val KEY_VERSION = "version"
        const val KEY_ERROR = "error"

        fun enqueue(
            context: Context,
            packId: String,
            manifestPath: String,
            titleFr: String,
            descriptionFr: String,
            version: String,
        ) {
            val data = Data.Builder()
                .putString(KEY_PACK_ID, packId)
                .putString(KEY_MANIFEST_PATH, manifestPath)
                .putString(KEY_TITLE, titleFr)
                .putString(KEY_DESCRIPTION, descriptionFr)
                .putString(KEY_VERSION, version)
                .build()
            val request = OneTimeWorkRequestBuilder<PackDownloadWorker>()
                .setInputData(data)
                .addTag("pack:$packId")
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_QUEUE,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request,
            )
        }
    }
}
