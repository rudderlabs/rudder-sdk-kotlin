package com.rudderstack.sdk.kotlin.android.storage.exceptions

internal class QueuedPayloadTooLargeException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
