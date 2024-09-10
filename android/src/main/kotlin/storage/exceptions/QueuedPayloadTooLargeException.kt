package com.rudderstack.android.storage.exceptions

/**
 * `InvalidConfigurationException` is a custom exception that is thrown when an invalid
 * configuration is detected in the application.
 *
 * This exception provides additional context about the configuration error, allowing developers
 * to understand what went wrong during the setup or runtime.
 *
 * @param message A detailed message explaining the cause of the exception.
 * @param cause The original exception that caused this exception to be thrown (optional).
 */
internal class QueuedPayloadTooLargeException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
