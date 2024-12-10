package com.rudderstack.kotlin.core.internals.storage.exception

internal class UnsupportedTypeException(
    message: String = "Unsupported type for write operation",
    cause: Throwable? = null
) : Exception(message, cause)
