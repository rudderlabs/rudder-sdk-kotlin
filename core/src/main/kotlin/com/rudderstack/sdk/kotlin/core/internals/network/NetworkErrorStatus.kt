package com.rudderstack.sdk.kotlin.core.internals.network

private const val BAD_REQUEST_CODE = 400
private const val UNAUTHORIZED_CODE = 401
private const val RESOURCE_NOT_FOUND_CODE = 404
private const val PAYLOAD_TOO_LARGE_CODE = 413

private const val HTTP_ERROR_START = 400
private const val HTTP_ERROR_END = 599
private val HTTP_RETRY_RANGE = HTTP_ERROR_START..HTTP_ERROR_END

/**
 * Sealed class representing various error statuses that can occur during network operations.
 *
 * This sealed class encapsulates different types of errors that may arise, providing meaningful names
 * for common HTTP status codes as well as handling dynamic error codes for retry able errors.
 *
 * @param responseCode The HTTP status code associated with the error, if applicable.
 */
sealed class NetworkErrorStatus(open val responseCode: Int?) {

    /**
     * Indicates a HTTP status code 400.
     */
    data object Error400 : NetworkErrorStatus(BAD_REQUEST_CODE)

    /**
     * Indicates HTTP status code 401.
     */
    data object Error401 : NetworkErrorStatus(UNAUTHORIZED_CODE)

    /**
     * Indicates HTTP status code 404.
     */
    data object Error404 : NetworkErrorStatus(RESOURCE_NOT_FOUND_CODE)

    /**
     * Indicates HTTP status code 413.
     */
    data object Error413 : NetworkErrorStatus(PAYLOAD_TOO_LARGE_CODE)

    /**
     * Indicates a retry able error.
     * This accepts a dynamic error code for cases when the code is not one of the predefined ones.
     *
     * The value will be null if the error code is not available e.g., in case of IO exception.
     *
     * @param responseCode The HTTP status code associated with the error, if applicable.
     */
    data class ErrorRetry(override val responseCode: Int? = null) : NetworkErrorStatus(responseCode)

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
        fun fromErrorCode(errorCode: Int?): NetworkErrorStatus = when (errorCode) {
            BAD_REQUEST_CODE -> Error400
            UNAUTHORIZED_CODE -> Error401
            RESOURCE_NOT_FOUND_CODE -> Error404
            PAYLOAD_TOO_LARGE_CODE -> Error413
            in HTTP_RETRY_RANGE -> ErrorRetry(errorCode)
            else -> ErrorUnknown
        }
    }
}

/**
 * Extension function to format the response code message for a NetworkErrorStatus.
 * @return A string representation of the response code, or "Not available" if the code is null.
 */
internal fun NetworkErrorStatus.formatResponseCodeMessage(): String {
    val responseCode = this.responseCode ?: "Not available"
    return "Response code: $responseCode"
}
