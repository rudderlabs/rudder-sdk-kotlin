package com.rudderstack.sdk.kotlin.core.internals.network

internal const val HTTP_400 = 400
internal const val HTTP_401 = 401
internal const val HTTP_404 = 404
internal const val HTTP_413 = 413

private const val HTTP_ERROR_START = 400
private const val HTTP_ERROR_END = 599
private val HTTP_RETRY_RANGE = HTTP_ERROR_START..HTTP_ERROR_END

/**
 * Sealed class representing various error statuses that can occur during network operations.
 *
 * This sealed class encapsulates different types of errors that may arise, providing meaningful names
 * for common HTTP status codes as well as handling dynamic error codes for retry able errors.
 *
 * @param statusCode The HTTP status code associated with the error, if applicable.
 */
sealed class NetworkErrorStatus(open val statusCode: Int?) {

    /**
     * Indicates a HTTP status code 400.
     */
    data object Error400 : NetworkErrorStatus(HTTP_400)

    /**
     * Indicates HTTP status code 401.
     */
    data object Error401 : NetworkErrorStatus(HTTP_401)

    /**
     * Indicates HTTP status code 404.
     */
    data object Error404 : NetworkErrorStatus(HTTP_404)

    /**
     * Indicates HTTP status code 413.
     */
    data object Error413 : NetworkErrorStatus(HTTP_413)

    /**
     * Indicates a retry able error.
     * This accepts a dynamic error code for cases when the code is not one of the predefined ones.
     *
     * The value will be null if the error code is not available e.g., in case of IO exception.
     *
     * @param statusCode The HTTP status code associated with the error, if applicable.
     */
    data class ErrorRetry(override val statusCode: Int? = null) : NetworkErrorStatus(statusCode)

    /**
     * Indicates a retry able error that happens when the network is unavailable.
     */
    data object ErrorNetworkUnavailable : NetworkErrorStatus(null)

    /**
     * Indicates an unknown but retry able error.
     */
    data object ErrorUnknown : NetworkErrorStatus(null)

    companion object {

        /**
         * Converts an HTTP status code to a corresponding NetworkErrorStatus instance.
         *
         * @param errorCode The HTTP status code to be mapped.
         * @return The corresponding NetworkErrorStatus instance.
         */
        fun toErrorStatus(errorCode: Int): NetworkErrorStatus = when (errorCode) {
            HTTP_400 -> Error400
            HTTP_401 -> Error401
            HTTP_404 -> Error404
            HTTP_413 -> Error413
            in HTTP_RETRY_RANGE -> ErrorRetry(errorCode)
            else -> ErrorUnknown
        }
    }
}

/**
 * Extension function to format the status code message for a NetworkErrorStatus.
 * @return A string representation of the status code, or "Not available" if the code is null.
 */
internal fun NetworkErrorStatus.formatStatusCodeMessage(): String {
    val statusCode = this.statusCode ?: "Not available"
    return "Status code: $statusCode"
}
