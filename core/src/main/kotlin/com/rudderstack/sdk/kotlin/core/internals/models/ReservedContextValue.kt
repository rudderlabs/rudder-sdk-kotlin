package com.rudderstack.sdk.kotlin.core.internals.models

import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import kotlinx.serialization.json.JsonElement

/**
 * Supplies the value the SDK currently asserts for a reserved context key.
 *
 * Registered against a [SDKManagedContextKey] by whichever module owns the feature behind that
 * key. Core iterates [SDKManagedContextKey.reservedKeys] and re-asserts whatever it is handed,
 * so it never needs to know what any individual key means.
 */
@InternalRudderApi
interface ReservedContextValue {

    /** Guidance appended to the override warning, so each key can explain its own migration path. */
    val overrideAdvice: String

    /** The current value for the key, or `null` while the SDK asserts none. */
    fun current(): JsonElement?
}
