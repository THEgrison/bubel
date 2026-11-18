package fr.russki.app.data.online

sealed class OnlineResult<out T> {
    data class Ok<T>(val value: T) : OnlineResult<T>()
    data class Err(val error: OnlineError) : OnlineResult<Nothing>()

    val isOk: Boolean get() = this is Ok
    fun getOrNull(): T? = (this as? Ok)?.value
}

data class OnlineError(
    val code: String,
    val message: String,
    val httpStatus: Int? = null,
    val cause: Throwable? = null,
) {
    companion object {
        const val ONLINE_DISABLED = "ONLINE_DISABLED"
        const val NETWORK = "NETWORK"
        const val INVALID_PAYLOAD = "INVALID_PAYLOAD"
        const val NOT_FOUND = "NOT_FOUND"
        const val SYNC_OPTED_OUT = "SYNC_OPTED_OUT"
        const val SYNC_UNAUTHORIZED = "SYNC_UNAUTHORIZED"
        const val CHECKSUM = "CHECKSUM"
        const val CANCELLED = "CANCELLED"

        fun disabled(): OnlineError = OnlineError(
            code = ONLINE_DISABLED,
            message = "Services en ligne non configurés (RUSSKI_API_BASE_URL vide). " +
                "Les leçons locales restent disponibles.",
        )
    }
}
