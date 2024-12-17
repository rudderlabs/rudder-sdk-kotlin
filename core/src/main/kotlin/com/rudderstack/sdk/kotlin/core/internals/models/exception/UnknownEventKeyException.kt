package com.rudderstack.sdk.kotlin.core.internals.models.exception

internal class UnknownEventKeyException(
    message: String = "Unknown event key, 'type' not found or does not match any message type",
    cause: Throwable? = null
) : Exception(message, cause)
