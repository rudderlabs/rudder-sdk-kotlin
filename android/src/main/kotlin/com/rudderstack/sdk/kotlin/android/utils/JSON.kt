package com.rudderstack.sdk.kotlin.android.utils

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

// Utility function to put value only if it is not null
internal fun JsonObjectBuilder.putIfNotNull(key: String, value: CharSequence?): JsonElement? = if (!value.isNullOrEmpty()) {
    put(key, value.toString())
} else {
    null
}

/**
 * Merges the current JSON object with another JSON object, giving higher priority to the other JSON object.
 *
 * @param other The JSON object to merge with the current JSON object.
 */
internal infix fun JsonObject.mergeWithHigherPriorityTo(other: JsonObject): JsonObject {
    return JsonObject(this.toMap() + other.toMap())
}

internal val JsonElement.safeJsonPrimitive get() = this as? JsonPrimitive

// Utility function to retrieve a boolean value from a jsonObject
internal fun JsonObject.getBoolean(key: String): Boolean? = this[key]?.safeJsonPrimitive?.booleanOrNull

// Utility function to retrieve a string value from a jsonObject
internal fun JsonObject.getString(key: String): String? = this[key]?.safeJsonPrimitive?.contentOrNull
