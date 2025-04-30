package com.rudderstack.sdk.kotlin.core.javacompat

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.jvm.Throws

/**
 * Helper object for interoperability between Java Maps and Kotlin's JsonObject.
 *
 * Provides utility functions to convert between standard Java/Kotlin data structures and
 * kotlinx.serialization's JSON types.
 */
object JsonInteropHelper {

    /**
     * Converts a Map with String keys and Any values to a JsonObject.
     *
     * This static method is accessible from Java code to enable seamless conversion
     * from Java Map objects to Kotlin's serializable JsonObject type.
     *
     * @param map The map to convert to a JsonObject
     * @return A JsonObject representation of the input map
     */
    @JvmStatic
    @Throws(IllegalArgumentException::class)
    fun fromMap(map: Map<String, Any>): JsonObject {
        val content = map.mapValues { (_, value) -> toJsonElement(value) }
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
}
