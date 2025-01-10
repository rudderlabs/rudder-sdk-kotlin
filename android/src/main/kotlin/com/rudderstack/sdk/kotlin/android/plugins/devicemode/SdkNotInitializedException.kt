package com.rudderstack.sdk.kotlin.android.plugins.devicemode

/**
 * Exception to be thrown when a destination fails to initialise in [IntegrationPlugin].
 *
 * @param message The message for the exception
 */
class SdkNotInitializedException(
    message: String = "SDK is not initialized."
) : IllegalStateException(message)
