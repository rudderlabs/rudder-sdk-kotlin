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
 */
internal sealed interface EventUploadError : EventUploadResult

/**
 * `RetryAbleError` is a sealed interface representing an event upload error that can be retried.
 */
internal sealed interface RetryAbleError : EventUploadError

/**
 * `NonRetryAbleError` is a sealed interface representing an event upload error that cannot be retried.
 */
internal sealed interface NonRetryAbleError : EventUploadError

/**
 * `RetryAbleEventUploadError` is an enum class representing the different types of retry able event upload errors.
 *  @property ERROR_RETRY A generic error that can be retried.
 *  @property ERROR_NETWORK_UNAVAILABLE An error indicating that the network is unavailable, and the upload can be retried.
 *  @property ERROR_UNKNOWN An unknown error occurred, and the upload can be retried.
 */
internal enum class RetryAbleEventUploadError : RetryAbleError {

    ERROR_RETRY,
    ERROR_NETWORK_UNAVAILABLE,
    ERROR_UNKNOWN;

    var responseCode: Int? = null
}

/**
 * `NonRetryAbleEventUploadError` is an enum class representing the different types of non-retry able event upload errors.
 *  @property ERROR_400 An error indicating that the request was invalid (e.g., missing or malformed body).
 *  @property ERROR_401 An error indicating that the request was unauthorized.
 *  @property ERROR_404 An error indicating that the resource was not found (e.g., the source is disabled).
 *  @property ERROR_413 An error indicating that the payload size exceeds the maximum allowed limit.
 */
internal enum class NonRetryAbleEventUploadError(val responseCode: Int) : NonRetryAbleError {

    ERROR_400(responseCode = 400),
    ERROR_401(responseCode = 401),
    ERROR_404(responseCode = 404),
    ERROR_413(responseCode = 413)
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
                is NetworkErrorStatus.ErrorRetry -> RetryAbleEventUploadError.ERROR_RETRY.apply {
                    this.responseCode = error.responseCode
                }

                NetworkErrorStatus.ErrorNetworkUnavailable -> RetryAbleEventUploadError.ERROR_NETWORK_UNAVAILABLE
                NetworkErrorStatus.ErrorUnknown -> RetryAbleEventUploadError.ERROR_UNKNOWN
                NetworkErrorStatus.Error400 -> NonRetryAbleEventUploadError.ERROR_400
                NetworkErrorStatus.Error401 -> NonRetryAbleEventUploadError.ERROR_401
                NetworkErrorStatus.Error404 -> NonRetryAbleEventUploadError.ERROR_404
                NetworkErrorStatus.Error413 -> NonRetryAbleEventUploadError.ERROR_413
            }
        }
    }
}

/**
 * Extension function that retrieves the response code from a RetryAbleError.
 * @return The HTTP response code associated with this error, or null if not available.
 */
internal fun EventUploadError.getResponseCode(): Int? {
    return when (this) {
        is RetryAbleEventUploadError -> this.responseCode
        is NonRetryAbleEventUploadError -> this.responseCode
    }
}

/**
 * Extension function that formats the error's response code as a message string.
 * @return A string representation of the response code, or "Not available" if the code is null.
 */
internal fun EventUploadError.formatResponseCodeMessage(): String {
    val responseCode = this.getResponseCode() ?: "Not available"
    return "Response code: $responseCode"
}
