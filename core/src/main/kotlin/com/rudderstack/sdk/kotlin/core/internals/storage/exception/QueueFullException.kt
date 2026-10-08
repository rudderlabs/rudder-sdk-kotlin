package com.rudderstack.sdk.kotlin.core.internals.storage.exception

internal class QueueFullException(
    message: String = "The event queue is full",
) : Exception(message)
