package com.rudderstack.sdk.kotlin.core.internals.network

private const val BAD_REQUEST_CODE = 400
private const val UNAUTHORIZED_CODE = 401
private const val RESOURCE_NOT_FOUND_CODE = 404
private const val PAYLOAD_TOO_LARGE_CODE = 413
private const val TOO_MANY_REQUESTS_CODE = 429

/**
 * Enum class representing various error statuses that can occur during an operation.
 *
 * This enum encapsulates the different types of errors that may arise, providing meaningful names for common HTTP status codes
 * and other error conditions. It helps in categorizing and handling errors in a structured manner.
 */
enum class ErrorStatus {

    /**
     * Indicates a bad request error, typically associated with HTTP status code 400.
     */
    ERROR_400,

    /**
     * Indicates an invalid write key error, typically associated with HTTP status code 401.
     */
    ERROR_401,

    /**
     * Indicates that the requested resource was not found, typically associated with HTTP status code 404.
     */
    ERROR_404,

    /**
     * Indicates that the request entity is too large, typically associated with HTTP status code 413.
     */
    ERROR_413,

    /**
     * Indicates that the rate limit has been exceeded, typically associated with HTTP status code 429.
     */
    ERROR_429,

    /**
     * Indicates a retry able error, typically associated with HTTP status code 4xx-5xx, excluding other error listed above.
     */
    ERROR_RETRY;

    companion object {
        /**
         * Converts an HTTP status code to a corresponding `ErrorStatus` enum value.
         *
         * This method maps HTTP status codes and other specific error codes to the appropriate `ErrorStatus` enum values
         * for easier handling of different error conditions.
         *
         * @param errorCode The HTTP status code or error code to be mapped to an `ErrorStatus`.
         * @return The corresponding `ErrorStatus` enum value.
         */
        fun toErrorStatus(errorCode: Int): ErrorStatus = when (errorCode) {
            BAD_REQUEST_CODE -> ERROR_400
            UNAUTHORIZED_CODE -> ERROR_401
            RESOURCE_NOT_FOUND_CODE -> ERROR_404
            PAYLOAD_TOO_LARGE_CODE -> ERROR_413
            TOO_MANY_REQUESTS_CODE -> ERROR_429
            else -> ERROR_RETRY
        }
    }
}
