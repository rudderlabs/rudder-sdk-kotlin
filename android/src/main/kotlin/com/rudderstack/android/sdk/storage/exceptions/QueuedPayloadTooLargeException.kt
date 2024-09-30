package com.rudderstack.android.sdk.storage.exceptions

internal class QueuedPayloadTooLargeException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
