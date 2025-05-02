package com.rudderstack.sdk.kotlin.core.javacompat

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlin.jvm.Throws

/**
 * Helper object for interoperability between Java Maps and Kotlin's JsonObject.
 *
 * Provides utility functions to convert between standard Java/Kotlin data structures and
 * kotlinx.serialization's JSON types.
 */
internal object JsonInteropHelper {

    /**
     * Converts a Map with String keys and Any? values to a JsonObject.
     *
     * @param map The map to convert to a JsonObject
     * @return A JsonObject representation of the input map
     */
    @JvmStatic
    @Throws(IllegalArgumentException::class)
    internal fun Map<String, Any?>.toJsonObject(): JsonObject {
        val content = this.mapValues { (_, value) -> toJsonElement(value) }
        return JsonObject(content)
    }

    /**
     * Recursively converts various types to their appropriate JsonElement representation.
     *
     * @param value The value to convert to a JsonElement
     * @return The corresponding JsonElement for the input value
     * @throws IllegalArgumentException if the value type is not supported
     */
    private fun toJsonElement(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is Boolean -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is String -> JsonPrimitive(value)

        is Map<*, *> -> {
            val stringKeyMap = value.entries
                .filter { it.key is String }
                .associate { it.key as String to toJsonElement(it.value) }
            JsonObject(stringKeyMap)
        }

        is List<*> -> {
            JsonArray(value.map { toJsonElement(it) })
        }

        else -> throw IllegalArgumentException("Unsupported type: ${value::class}")
    }

    internal fun JsonObject.toRawMap(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()

        for ((key, jsonElement) in this) {
            result[key] = extractValue(jsonElement)
        }

        return result
    }

    private fun extractValue(element: JsonElement?): Any? {
        return when (element) {
            JsonNull, null -> null

            is JsonPrimitive -> when {
                element.isString -> element.content
                element.booleanOrNull != null -> element.boolean
                element.intOrNull != null -> element.int
                element.longOrNull != null -> element.long
                element.doubleOrNull != null -> element.double
                else -> element.content
            }

            is JsonObject -> element.toRawMap()

            is JsonArray -> element.map { extractValue(it) }
        }
    }
}
