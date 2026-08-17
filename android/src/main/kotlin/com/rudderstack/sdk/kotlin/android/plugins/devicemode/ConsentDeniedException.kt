package com.rudderstack.sdk.kotlin.android.plugins.devicemode

/**
 * Exception indicating that a destination is denied by user consent and will not be initialized.
 */
class ConsentDeniedException(message: String) : Exception(message)
