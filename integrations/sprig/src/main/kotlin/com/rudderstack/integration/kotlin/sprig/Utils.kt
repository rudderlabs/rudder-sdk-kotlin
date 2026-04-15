package com.rudderstack.integration.kotlin.sprig

import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull

/**
 * Parses the [JsonObject] to the specified type [T].
 */
@OptIn(InternalRudderApi::class)
internal inline fun <reified T> JsonObject.parseConfig(logger: Logger): T? {
    return this.takeIf { it.isNotEmpty() }?.let {
        LenientJson.decodeFromJsonElement<T>(this)
    } ?: run {
        logger.debug("SprigIntegration: The configuration is empty.")
        null
    }
}

/**
 * Extracts a string value from the [JsonPrimitive] and returns it.
 */
internal fun JsonPrimitive.toStringOrNull(): String? = if (isString) content else null

/**
 * Extracts an int value from the [JsonPrimitive] and returns it.
 */
internal fun JsonPrimitive.toIntOrNull(): Int? = intOrNull

/**
 * Extracts a boolean value from the [JsonPrimitive] and returns it.
 */
internal fun JsonPrimitive.toBooleanOrNull(): Boolean? = booleanOrNull
