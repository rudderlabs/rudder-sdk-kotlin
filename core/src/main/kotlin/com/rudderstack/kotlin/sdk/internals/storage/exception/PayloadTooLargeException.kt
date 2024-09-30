package com.rudderstack.kotlin.sdk.internals.storage.exception

internal class PayloadTooLargeException(
    message: String = "Enqueued payload is too large",
    cause: Throwable? = null
) : Exception(message, cause)
