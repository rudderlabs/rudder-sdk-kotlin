package com.rudderstack.sdk.kotlin.core.internals.models

import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi

/**
 * Context keys the SDK stamps on every event.
 *
 * [reservedKeys] are re-asserted at the terminal boundary; overriding a key in
 * [baseKeys] is deprecated and logs a warning, but the value is left untouched.
 *
 * @property key The key name as it appears in the event context.
 */
@InternalRudderApi
enum class SDKManagedContextKey(val key: String) {
    APP("app"),
    DEVICE("device"),
    LIBRARY("library"),
    LOCALE("locale"),
    NETWORK("network"),
    OS("os"),
    SCREEN("screen"),
    TIMEZONE("timezone"),
    SESSION_ID("sessionId"),
    CONSENT_MANAGEMENT("consentManagement");

    companion object {
        /** Keys re-asserted at the terminal boundary. */
        val reservedKeys: List<SDKManagedContextKey> = listOf(CONSENT_MANAGEMENT)

        /** Overridable SDK-stamped keys — a customer override triggers a deprecation warning. */
        val baseKeys: List<SDKManagedContextKey> = listOf(
            APP, DEVICE, LIBRARY, LOCALE, NETWORK, OS, SCREEN, TIMEZONE, SESSION_ID
        )
    }
}
