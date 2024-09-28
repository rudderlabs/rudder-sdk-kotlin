package com.rudderstack.kotlin.internals.models.exception

internal class UnknownMessageKeyException(
    message: String = "Unknown message key, 'type' not found or does not match any message type",
    cause: Throwable? = null
) : Exception(message, cause)
