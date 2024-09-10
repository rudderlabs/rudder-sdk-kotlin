package com.rudderstack.core.internals.network

private const val BAD_REQUEST_CODE = 400
private const val INVALID_WRITE_KEY_CODE = 401
private const val RESOURCE_NOT_FOUND_CODE = 404
private const val TOO_MANY_REQUESTS_CODE = 429
private const val SERVER_ERROR_CODE = 500
private const val NETWORK_CONNECTION_TIMEOUT_ERROR_CODE = 599

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
    BAD_REQUEST,

    /**
     * Indicates an invalid write key error, typically associated with HTTP status code 401.
     */
    INVALID_WRITE_KEY,

    /**
     * Indicates that the requested resource was not found, typically associated with HTTP status code 404.
     */
    RESOURCE_NOT_FOUND,

    /**
     * Indicates that the rate limit has been exceeded, typically associated with HTTP status code 429.
     */
    TOO_MANY_REQUESTS,

    /**
     * Indicates a server error, typically associated with HTTP status code 500.
     */
    SERVER_ERROR,

    /**
     * Indicates a network connection timeout error, typically associated with HTTP status code 599.
     */
    NETWORK_CONNECTION_TIMEOUT_ERROR,

    /**
     * Indicates that a retry operation should be attempted due to a transient error condition.
     */
    RETRY_ERROR,

    /**
     * Indicates a general error that does not fall into the specific categories listed above.
     */
    GENERAL_ERROR;

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
            BAD_REQUEST_CODE -> BAD_REQUEST
            INVALID_WRITE_KEY_CODE -> INVALID_WRITE_KEY
            RESOURCE_NOT_FOUND_CODE -> RESOURCE_NOT_FOUND
            TOO_MANY_REQUESTS_CODE -> TOO_MANY_REQUESTS
            SERVER_ERROR_CODE -> SERVER_ERROR
            NETWORK_CONNECTION_TIMEOUT_ERROR_CODE -> NETWORK_CONNECTION_TIMEOUT_ERROR
            in SERVER_ERROR_CODE..NETWORK_CONNECTION_TIMEOUT_ERROR_CODE -> RETRY_ERROR
            else -> GENERAL_ERROR
        }
    }
}
