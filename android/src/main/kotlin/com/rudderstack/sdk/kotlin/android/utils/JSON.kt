package com.rudderstack.sdk.kotlin.android.utils

import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
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

/**
 * Returns a JsonPrimitive if the JsonElement is a JsonPrimitive, null otherwise.
 */
@InternalRudderApi
val JsonElement.safeJsonPrimitive get() = this as? JsonPrimitive

// Utility function to retrieve a boolean value from a jsonObject
internal fun JsonObject.getBoolean(key: String): Boolean? = this[key]?.safeJsonPrimitive?.booleanOrNull

// Utility function to retrieve a string value from a jsonObject
internal fun JsonObject.getString(key: String): String? = this[key]?.safeJsonPrimitive?.contentOrNull

/**
 * Utility function to retrieve a string value from a jsonElement
 */
@InternalRudderApi
fun JsonElement.toContentString(): String? = (this as? JsonPrimitive)?.contentOrNull

/**
 * Utility function to retrieve a boolean value from a jsonElement
 */
@InternalRudderApi
fun JsonElement.toBoolean(): Boolean? = (this as? JsonPrimitive)?.booleanOrNull

/**
 * Utility function to retrieve an integer value from a jsonElement
 */
@InternalRudderApi
fun JsonElement.toInt(): Int? = (this as? JsonPrimitive)?.intOrNull

/**
 * Utility function to retrieve a long value from a jsonElement
 */
@InternalRudderApi
fun JsonElement.toLong(): Long? = (this as? JsonPrimitive)?.longOrNull

/**
 * Utility function to retrieve a double value from a jsonElement
 */
@InternalRudderApi
fun JsonElement.toDouble(): Double? = (this as? JsonPrimitive)?.doubleOrNull
