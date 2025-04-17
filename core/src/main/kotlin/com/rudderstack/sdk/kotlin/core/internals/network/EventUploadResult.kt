package com.rudderstack.sdk.kotlin.core.internals.network

import com.rudderstack.sdk.kotlin.core.internals.utils.Result

/**
 * `EventUploadResult` is a sealed interface representing the result of an event upload attempt.
 * It can either be a success or an error.
 */
internal sealed interface EventUploadResult

/**
 * `EventUploadSuccess` represents a successful event upload.
 * @property response The response from the server.
 */
internal data class EventUploadSuccess(val response: String) : EventUploadResult

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
 * `RetryAbleEventUploadError` is an enum class representing the different types of retryable event upload errors.
 *  @property ERROR_RETRY A generic error that can be retried.
 *  @property ERROR_NETWORK_UNAVAILABLE An error indicating that the network is unavailable, and the upload can be retried.
 *  @property ERROR_UNKNOWN An unknown error occurred, and the upload can be retried.
 */
internal enum class RetryAbleEventUploadError : RetryAbleError {
    ERROR_RETRY,
    ERROR_NETWORK_UNAVAILABLE,
    ERROR_UNKNOWN
}

/**
 * `NonRetryAbleEventUploadError` is an enum class representing the different types of non-retryable event upload errors.
 *  @property ERROR_400 An error indicating that the request was invalid (e.g., missing or malformed body).
 *  @property ERROR_401 An error indicating that the request was unauthorized.
 *  @property ERROR_404 An error indicating that the resource was not found (e.g., the source is disabled).
 *  @property ERROR_413 An error indicating that the payload size exceeds the maximum allowed limit.
 */
internal enum class NonRetryAbleEventUploadError : NonRetryAbleError {
    ERROR_400,
    ERROR_401,
    ERROR_404,
    ERROR_413
}

/**
 * Extension function to convert a `NetworkResult` to an `EventUploadResult`.
 * @return An `EventUploadResult` representing the result of the network operation.
 */
internal fun NetworkResult.toEventUploadError(): EventUploadResult {
    return when (this) {
        is Result.Success -> {
            return EventUploadSuccess(response)
        }

        is Result.Failure -> {
            when (this.error) {
                NetworkErrorStatus.ERROR_RETRY -> RetryAbleEventUploadError.ERROR_RETRY
                NetworkErrorStatus.ERROR_NETWORK_UNAVAILABLE -> RetryAbleEventUploadError.ERROR_NETWORK_UNAVAILABLE
                NetworkErrorStatus.ERROR_UNKNOWN -> RetryAbleEventUploadError.ERROR_UNKNOWN
                NetworkErrorStatus.ERROR_400 -> NonRetryAbleEventUploadError.ERROR_400
                NetworkErrorStatus.ERROR_401 -> NonRetryAbleEventUploadError.ERROR_401
                NetworkErrorStatus.ERROR_404 -> NonRetryAbleEventUploadError.ERROR_404
                NetworkErrorStatus.ERROR_413 -> NonRetryAbleEventUploadError.ERROR_413
            }
        }
    }
}
