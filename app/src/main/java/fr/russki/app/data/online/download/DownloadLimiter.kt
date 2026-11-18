package fr.russki.app.data.online.download

import kotlinx.coroutines.sync.Semaphore

/** Limite les téléchargements simultanés (WorkManager + appels directs). */
class DownloadLimiter(maxConcurrent: Int) {
    private val semaphore = Semaphore(permits = maxConcurrent.coerceAtLeast(1))

    suspend fun <T> withPermit(block: suspend () -> T): T {
        semaphore.acquire()
        return try {
            block()
        } finally {
            semaphore.release()
        }
    }
}
