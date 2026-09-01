package com.rudderstack.sdk.kotlin.core.internals.models

import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi

/**
 * Context keys the SDK owns.
 *
 * [reservedKeys] are re-asserted at the terminal boundary; overriding a key in
 * [baseKeys] is deprecated and logs a warning, but the value is left untouched.
 *
 * Which base keys are actually stamped depends on the platform: the core SDK stamps
 * [coreBaseKeys], and the android module's context plugins add [mobileBaseKeys].
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

        /** Base keys stamped on every platform. */
        val coreBaseKeys: List<SDKManagedContextKey> = listOf(LIBRARY)

        /** Base keys stamped only by the android module's context plugins. */
        val mobileBaseKeys: List<SDKManagedContextKey> = listOf(
            APP,
            DEVICE,
            LOCALE,
            NETWORK,
            OS,
            SCREEN,
            TIMEZONE,
            SESSION_ID
        )

        /** Every overridable SDK-stamped key — a customer override triggers a deprecation warning. */
        val baseKeys: List<SDKManagedContextKey> = coreBaseKeys + mobileBaseKeys
    }
}
