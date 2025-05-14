package com.rudderstack.sdk.kotlin.core.internals.network

import com.rudderstack.sdk.kotlin.core.internals.utils.Result

/**
 * `EventUploadResult` is a sealed interface representing the result of an event upload attempt.
 * It can either be a success or an error.
 */
internal sealed interface EventUploadResult

/**
 * `Success` represents a successful event upload.
 * @property response The response from the server.
 */
internal data class Success(val response: String) : EventUploadResult

/**
 * `EventUploadError` is a sealed interface representing an error that occurred during event upload.
 * It can be either a retry able error or a non-retry able error.
 *
 * @property statusCode The HTTP stats code associated with the error, if available.
 */
internal sealed interface EventUploadError : EventUploadResult {

    val statusCode: Int?
}

/**
 * `RetryAbleError` is a sealed interface representing an event upload error that can be retried.
 */
internal sealed interface RetryAbleError : EventUploadError

/**
 * `NonRetryAbleError` is a sealed interface representing an event upload error that cannot be retried.
 */
internal sealed interface NonRetryAbleError : EventUploadError

/**
 * `RetryAbleEventUploadError` is an sealed class representing the different types of retry able event upload errors.
 *
 * @property statusCode The HTTP status code associated with the error, if available.
 */
internal sealed class RetryAbleEventUploadError(override val statusCode: Int? = null) : RetryAbleError {

    /**
     * `ErrorRetry` represents a retry able error with a specific HTTP status code.
     * @property statusCode The HTTP status code associated with the error, if available.
     */
    internal data class ErrorRetry(override val statusCode: Int? = null) : RetryAbleEventUploadError(statusCode)

    /**
     * `ErrorNetworkUnavailable` represents a retry able error that occurs when the network is unavailable.
     */
    internal data object ErrorNetworkUnavailable : RetryAbleEventUploadError(null)

    /**
     * `ErrorUnknown` represents an unknown but retry able error.
     */
    internal data object ErrorUnknown : RetryAbleEventUploadError(null)
}

/**
 * `NonRetryAbleEventUploadError` is an enum class representing the different types of non-retry able event upload errors.
 *  @property ERROR_400 An error indicating that the request was invalid (e.g., missing or malformed body).
 *  @property ERROR_401 An error indicating that the request was unauthorized.
 *  @property ERROR_404 An error indicating that the resource was not found (e.g., the source is disabled).
 *  @property ERROR_413 An error indicating that the payload size exceeds the maximum allowed limit.
 */
internal enum class NonRetryAbleEventUploadError(override val statusCode: Int) : NonRetryAbleError {

    ERROR_400(statusCode = HTTP_400),
    ERROR_401(statusCode = HTTP_401),
    ERROR_404(statusCode = HTTP_404),
    ERROR_413(statusCode = HTTP_413)
}

/**
 * Extension function to convert a `NetworkResult` to an `EventUploadResult`.
 * @return An `EventUploadResult` representing the result of the network operation.
 */
internal fun NetworkResult.toEventUploadResult(): EventUploadResult {
    return when (this) {
        is Result.Success -> {
            Success(response)
        }

        is Result.Failure -> {
            when (val error = this.error) {
                is NetworkErrorStatus.ErrorRetry -> RetryAbleEventUploadError.ErrorRetry(error.statusCode)
                NetworkErrorStatus.ErrorNetworkUnavailable -> RetryAbleEventUploadError.ErrorNetworkUnavailable
                NetworkErrorStatus.ErrorUnknown -> RetryAbleEventUploadError.ErrorUnknown
                NetworkErrorStatus.Error400 -> NonRetryAbleEventUploadError.ERROR_400
                NetworkErrorStatus.Error401 -> NonRetryAbleEventUploadError.ERROR_401
                NetworkErrorStatus.Error404 -> NonRetryAbleEventUploadError.ERROR_404
                NetworkErrorStatus.Error413 -> NonRetryAbleEventUploadError.ERROR_413
            }
        }
    }
}

/**
 * Extension function that formats the error's status code as a message string.
 * @return A string representation of the status code, or "Not available" if the code is null.
 */
internal fun EventUploadError.formatStatusCodeMessage(): String {
    val statusCode = this.statusCode ?: "Not available"
    return "Status code: $statusCode"
}
