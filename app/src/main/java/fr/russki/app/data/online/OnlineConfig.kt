package fr.russki.app.data.online

import fr.russki.app.BuildConfig

/**
 * Configuration réseau publique (pas de secrets).
 * L’URL de base se règle via `-PRUSSKI_API_BASE_URL=https://…` au build
 * ou injection de test.
 */
data class OnlineConfig(
    val apiBaseUrl: String,
    val connectTimeoutMs: Long = 10_000L,
    val readTimeoutMs: Long = 30_000L,
    val writeTimeoutMs: Long = 30_000L,
    val maxConcurrentDownloads: Int = 2,
    /** Autorise http:// uniquement pour MockWebServer / instrumentation. */
    val allowCleartextForTests: Boolean = false,
) {
    val isConfigured: Boolean
        get() = when {
            apiBaseUrl.isBlank() -> false
            apiBaseUrl.startsWith("https://") -> true
            allowCleartextForTests && apiBaseUrl.startsWith("http://") -> true
            else -> false
        }

    companion object {
        fun fromBuildConfig(): OnlineConfig =
            OnlineConfig(
                apiBaseUrl = BuildConfig.RUSSKI_API_BASE_URL.trim(),
                allowCleartextForTests = false,
            )
    }
}
