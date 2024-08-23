package com.rudderstack.core.internals.web

enum class ErrorStatus {
    INVALID_WRITE_KEY,
    ERROR,
    RESOURCE_NOT_FOUND,
    BAD_REQUEST,
    RETRY_ABLE;

    companion object {
        fun getErrorStatus(responseCode: Int): ErrorStatus = when (responseCode) {
            400 -> BAD_REQUEST
            401 -> INVALID_WRITE_KEY
            404 -> RESOURCE_NOT_FOUND
            429, in 500..599 -> RETRY_ABLE
            else -> ERROR
        }
    }
}
