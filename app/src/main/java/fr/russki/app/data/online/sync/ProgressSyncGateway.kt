package fr.russki.app.data.online.sync

import fr.russki.app.data.local.RusskiDatabase
import fr.russki.app.data.online.OnlineError
import fr.russki.app.data.online.OnlineResult
import fr.russki.app.data.online.api.LessonProgressDto
import fr.russki.app.data.online.api.ProgressSyncAck
import fr.russki.app.data.online.api.ProgressSyncSnapshot
import fr.russki.app.data.online.api.RusskiOnlineApi
import fr.russki.app.data.online.api.SrsCardDto
import fr.russki.app.data.progress.ProgressContentIds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Synchronisation facultative de la progression.
 *
 * - Désactivée tant que l’utilisateur n’a pas consenti (`userOptIn`).
 * - Sans jeton / sans serveur : retourne une erreur explicite — **jamais** un faux succès.
 * - N’envoie que des identifiants pédagogiques et compteurs SRS (pas de nom, e-mail, etc.).
 */
class ProgressSyncGateway(
    private val api: RusskiOnlineApi,
    private val db: RusskiDatabase,
    private val preferences: SyncPreferences,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    val isServerConfigured: Boolean get() = api.isConfigured

    fun isUserOptedIn(): Boolean = preferences.syncOptIn

    fun setUserOptIn(enabled: Boolean) {
        preferences.syncOptIn = enabled
    }

    /**
     * Jeton d’auth sync. Non fourni dans cette version : le flux d’émission
     * (OAuth / appareil) reste à déployer côté serveur — voir docs/DEPLOYMENT.md.
     */
    fun authTokenOrEmpty(): String = preferences.syncBearerToken.orEmpty()

    suspend fun pushIfAllowed(): OnlineResult<ProgressSyncAck> = withContext(Dispatchers.IO) {
        if (!isUserOptedIn()) {
            return@withContext OnlineResult.Err(
                OnlineError(
                    OnlineError.SYNC_OPTED_OUT,
                    "Synchronisation désactivée. Activez-la dans Profil après lecture de l’information.",
                ),
            )
        }
        if (!api.isConfigured) {
            return@withContext OnlineResult.Err(OnlineError.disabled())
        }
        val token = authTokenOrEmpty()
        if (token.isBlank()) {
            return@withContext OnlineResult.Err(
                OnlineError(
                    OnlineError.SYNC_UNAUTHORIZED,
                    "Serveur de sync non déployé : aucun jeton. " +
                        "La progression locale est conservée intacte.",
                ),
            )
        }
        api.pushProgress(token, buildSnapshot())
    }

    suspend fun buildSnapshot(): ProgressSyncSnapshot {
        // Exclut dialogue-* : ne pas les compter comme leçons ordinaires côté sync.
        val lessons = db.userLessonProgressDao().getAll()
            .filter { ProgressContentIds.isCurriculumLesson(it.lessonId) }
            .map {
                LessonProgressDto(
                    lessonId = it.lessonId,
                    completedExerciseCount = it.completedExerciseCount,
                    completedAtEpochMs = it.completedAtEpochMs,
                )
            }
        val cards = db.srsCardDao().getAll().map {
            SrsCardDto(
                cardId = it.cardId,
                nextReviewEpochMs = it.nextReviewEpochMs,
                intervalDays = it.intervalDays,
                easeFactor = it.easeFactor,
                repetitionCount = it.repetitionCount,
                lapseCount = it.lapseCount,
                learningState = it.learningState,
            )
        }
        return ProgressSyncSnapshot(
            schemaVersion = 1,
            deviceInstallId = preferences.deviceInstallId,
            exportedAtEpochMs = clock(),
            lessonProgress = lessons,
            srsCards = cards,
        )
    }
}

/** Préférences sync en mémoire process (+ DataStore ultérieur). */
class SyncPreferences(
    initialDeviceId: String = UUID.randomUUID().toString(),
) {
    @Volatile
    var syncOptIn: Boolean = false

    @Volatile
    var syncBearerToken: String? = null

    @Volatile
    var deviceInstallId: String = initialDeviceId
}
