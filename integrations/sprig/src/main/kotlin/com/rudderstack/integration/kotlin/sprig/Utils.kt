package com.rudderstack.integration.kotlin.sprig

import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import com.userleap.Sprig
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
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
 * Extracts primitive values from [JsonPrimitive] elements, preserving their native types:
 * String, Boolean, Int (for integers in Int range), Long (for integers beyond Int range),
 * and Double (for decimals or integers outside the Long range).
 * [JsonNull] entries are omitted from the resulting map to avoid sending the literal
 * string "null" as a property value. Non-primitive [JsonElement] types are converted
 * to their string representation.
 */
internal fun JsonObject.toStringMap(): Map<String, Any> {
    return entries.mapNotNull { (key, value) ->
        when (value) {
            is JsonNull -> null
            is JsonPrimitive -> key to when {
                value.isString -> value.content
                else ->
                    value.booleanOrNull
                        ?: value.intOrNull
                        ?: value.longOrNull
                        ?: value.doubleOrNull
                        ?: value.content
            }
            else -> key to value.toString()
        }
    }.toMap()
}

/**
 * Trait keys handled by dedicated Sprig setters (e.g. [Sprig.setEmailAddress]) rather than
 * the generic [Sprig.setVisitorAttribute]. Keys listed here are skipped by the custom-trait
 * loop so they are not also sent as visitor attributes.
 */
private val STANDARD_TRAIT_KEYS = setOf(EMAIL_KEY)

internal fun setSprigAttributes(sprig: Sprig, attributes: JsonObject, logger: Logger) {
    setStandardTraits(sprig, attributes)
    setCustomTraits(sprig, attributes, logger)
}

private fun setStandardTraits(sprig: Sprig, attributes: JsonObject) {
    attributes[EMAIL_KEY]?.let { element ->
        (element as? JsonPrimitive)?.toStringOrNull()?.let { email ->
            sprig.setEmailAddress(email)
        }
    }
}

private fun setCustomTraits(sprig: Sprig, attributes: JsonObject, logger: Logger) {
    for ((key, value) in attributes) {
        if (key !in STANDARD_TRAIT_KEYS) {
            setVisitorAttribute(sprig, key, value, logger)
        }
    }
}

private fun setVisitorAttribute(sprig: Sprig, key: String, value: JsonElement, logger: Logger) {
    if (key.startsWith("!")) {
        logger.warn(
            "SprigIntegration: '$key' is not a valid property name. " +
                "Property names cannot start with '!'. Ignoring property"
        )
        return
    }

    val normalizedKey = key.takeIf { it.length < MAX_ATTRIBUTE_KEY_LENGTH }
        ?: key.take(MAX_ATTRIBUTE_KEY_LENGTH - 1).also { trimmed ->
            logger.warn(
                "SprigIntegration: property name '$key' exceeds ${MAX_ATTRIBUTE_KEY_LENGTH - 1} characters. " +
                    "Trimming to '$trimmed'"
            )
        }

    val primitiveValue = value as? JsonPrimitive ?: run {
        logger.warn(
            "SprigIntegration: '$normalizedKey' has an unsupported value type. " +
                "Only String, Int, and Boolean are accepted. Ignoring property"
        )
        return
    }

    primitiveValue.toStringOrNull()?.also { sprig.setVisitorAttribute(normalizedKey, it) }
        ?: primitiveValue.toBooleanOrNull()?.also { sprig.setVisitorAttribute(normalizedKey, it) }
        ?: primitiveValue.toIntOrNull()?.also { sprig.setVisitorAttribute(normalizedKey, it) }
        ?: logger.warn(
            "SprigIntegration: '${primitiveValue.content}' is not a valid property value. " +
                "Only String, Int, and Boolean are accepted. Ignoring property"
        )
}
