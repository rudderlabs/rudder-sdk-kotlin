package com.rudderstack.integration.kotlin.sprig

import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import com.userleap.Sprig
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

/**
 * Parses the [JsonObject] to the specified type [T].
 */
@OptIn(InternalRudderApi::class)
internal inline fun <reified T> JsonObject.parseConfig(logger: Logger): T? {
    return this.takeIf { it.isNotEmpty() }?.let {
        LenientJson.decodeFromJsonElement<T>(this)
    } ?: run {
        logger.debug("SprigIntegration: The configuration is empty")
        null
    }
}

/**
 * Extracts a string value from the [JsonPrimitive] and returns it.
 */
internal fun JsonPrimitive.toStringOrNull(): String? = if (isString) content else null

/**
 * Coerces a numeric [JsonPrimitive] to [Int], mirroring Java's `Number.intValue()`.
 * Accepts both integer and decimal JSON numbers; decimals are truncated toward zero,
 * and values outside the Int range wrap per [Long.toInt] / [Double.toInt].
 * Returns null if the primitive is a string, boolean, or null.
 */
internal fun JsonPrimitive.toIntOrNull(): Int? {
    if (isString) return null
    return longOrNull?.toInt() ?: doubleOrNull?.toInt()
}

/**
 * Extracts a boolean value from the [JsonPrimitive] and returns it.
 */
internal fun JsonPrimitive.toBooleanOrNull(): Boolean? = booleanOrNull

/**
 * Extension property that safely accesses the traits JsonObject from an IdentifyEvent.
 * Retrieves the "traits" object from the event's context and converts it to a JsonObject.
 *
 * @return JsonObject containing the traits if present in context, null otherwise
 */
internal val IdentifyEvent.traits: JsonObject?
    get() = this.context["traits"]?.jsonObject

/**
 * Converts a [JsonObject] to a [Map] of String keys to Any values.
 * Extracts primitive values (String, Boolean, Number) from [JsonPrimitive] elements,
 * and converts other [JsonElement] types to their string representation.
 */
internal fun JsonObject.toStringMap(): Map<String, Any> {
    return entries.associate { (key, value) ->
        key to when (value) {
            is JsonPrimitive -> when {
                value.isString -> value.content
                value.booleanOrNull != null -> value.booleanOrNull!!
                value.intOrNull != null -> value.intOrNull!!
                else -> value.content
            }
            else -> value.toString()
        }
    }
}

internal fun setSprigAttributes(sprig: Sprig, attributes: JsonObject, logger: Logger) {
    attributes[EMAIL_KEY]?.let { element ->
        (element as? JsonPrimitive)?.toStringOrNull()?.let { email ->
            sprig.setEmailAddress(email)
        }
    }

    for ((key, value) in attributes) {
        if (key != EMAIL_KEY) {
            setVisitorAttribute(sprig, key, value, logger)
        }
    }
}

private fun setVisitorAttribute(sprig: Sprig, key: String, value: JsonElement, logger: Logger) {
    if (key.length >= MAX_ATTRIBUTE_KEY_LENGTH || key.startsWith("!")) {
        logger.warn(
            "SprigIntegration: '$key' is not a valid property name. " +
                "Property names must be less than $MAX_ATTRIBUTE_KEY_LENGTH characters " +
                "and cannot start with '!'. Ignoring property"
        )
        return
    }

    val primitive = value as? JsonPrimitive ?: run {
        logger.warn(
            "SprigIntegration: '$key' has an unsupported value type. " +
                "Only String, Int, and Boolean are accepted. Ignoring property"
        )
        return
    }

    when {
        primitive.toStringOrNull() != null -> sprig.setVisitorAttribute(key, primitive.content)
        primitive.toIntOrNull() != null -> sprig.setVisitorAttribute(key, primitive.toIntOrNull()!!)
        primitive.toBooleanOrNull() != null -> sprig.setVisitorAttribute(key, primitive.toBooleanOrNull()!!)
        else -> logger.warn(
            "SprigIntegration: '${primitive.content}' is not a valid property value. " +
                "Only String, Int, and Boolean are accepted. Ignoring property"
        )
    }
}
