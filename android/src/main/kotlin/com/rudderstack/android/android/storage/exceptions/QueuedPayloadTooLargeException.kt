package com.rudderstack.android.android.storage.exceptions

internal class QueuedPayloadTooLargeException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
