package com.rudderstack.sdk.kotlin.core.internals.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.jetbrains.annotations.VisibleForTesting

/**
 * The name and version of a RudderStack SDK that wraps this SDK, for example the Flutter SDK.
 *
 * When set, every event of every instance in this process carries it as `context.wrapperLibrary`.
 * `context.library` keeps this SDK's own name and version.
 */
@InternalRudderApi
object WrapperLibrary {

    @Volatile
    internal var info: JsonElement? = null
        private set

    /**
     * Sets the wrapper's [name] and [version]. Call it before the first event to cover every event.
     */
    fun set(name: String, version: String) {
        info = LenientJson.encodeToJsonElement(WrapperLibraryInfo(name, version))
    }

    @VisibleForTesting
    internal fun clear() {
        info = null
    }
}

@Serializable
private data class WrapperLibraryInfo(val name: String, val version: String)
