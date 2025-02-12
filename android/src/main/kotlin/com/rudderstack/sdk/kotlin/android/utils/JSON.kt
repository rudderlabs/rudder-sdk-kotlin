@file:Suppress("TooManyFunctions")

package com.rudderstack.sdk.kotlin.android.utils

import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import kotlinx.serialization.json.JsonArray
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
private val JsonElement.safeJsonPrimitive get() = this as? JsonPrimitive

/**
 * Utility function to retrieve a boolean value from a jsonObject
 */
@InternalRudderApi
fun JsonObject.getBoolean(key: String): Boolean? = this[key]?.safeJsonPrimitive?.booleanOrNull

/**
 * Utility function to retrieve a string value from a jsonObject
 */
@InternalRudderApi
fun JsonObject.getString(key: String): String? = this[key]?.safeJsonPrimitive?.contentOrNull

/**
 * Utility function to retrieve an integer value from a jsonObject
 */
@InternalRudderApi
fun JsonObject.getInt(key: String): Int? = this[key]?.safeJsonPrimitive?.intOrNull

/**
 * Utility function to retrieve a long value from a jsonObject
 */
@InternalRudderApi
fun JsonObject.getLong(key: String): Long? = this[key]?.safeJsonPrimitive?.longOrNull

/**
 * Utility function to retrieve a float value from a jsonObject
 */
@InternalRudderApi
fun JsonObject.getDouble(key: String): Double? = this[key]?.safeJsonPrimitive?.doubleOrNull

/**
 * Utility function to retrieve a jsonArray from a jsonObject
 */
fun JsonObject.getArray(key: String): JsonArray? = this[key] as? JsonArray

/**
 * Utility function to check whether a key has an empty value in a jsonObject
 */
@InternalRudderApi
fun JsonObject.isKeyEmpty(key: String): Boolean {
    val value = this[key] ?: return true // Key does not exist

    return when (value) {
        is JsonPrimitive -> value.contentOrNull.isNullOrEmpty()
        is JsonArray -> value.isEmpty()
        is JsonObject -> value.isEmpty()
        else -> true
    }
}

/**
 * Utility function to check whether a value corresponding to a key in a jsonObject is a string
 */
@InternalRudderApi
fun JsonObject.isString(key: String): Boolean = this[key]?.safeJsonPrimitive?.isString == true

/**
 * Utility function to check whether a value corresponding to a key in a jsonObject is a boolean
 */
@InternalRudderApi
fun JsonObject.isBoolean(key: String): Boolean = this[key]?.safeJsonPrimitive?.booleanOrNull != null

/**
 * Utility function to check whether a value corresponding to a key in a jsonObject is an integer
 */
@InternalRudderApi
fun JsonObject.isInt(key: String): Boolean = this[key]?.safeJsonPrimitive?.intOrNull != null

/**
 * Utility function to check whether a value corresponding to a key in a jsonObject is a long
 */
@InternalRudderApi
fun JsonObject.isLong(key: String): Boolean = this[key]?.safeJsonPrimitive?.longOrNull != null

/**
 * Utility function to check whether a value corresponding to a key in a jsonObject is a double
 */
@InternalRudderApi
fun JsonObject.isDouble(key: String): Boolean = this[key]?.safeJsonPrimitive?.doubleOrNull != null

/**
 *  Utility function to retrieve a string from a JsonElement
 */
@InternalRudderApi
fun JsonElement.getString(): String? = this.safeJsonPrimitive?.contentOrNull

/**
 * Utility function to retrieve a long from a JsonElement
 */
@InternalRudderApi
fun JsonElement.getLong(): Long? = this.safeJsonPrimitive?.longOrNull

/**
 * Utility function to retrieve a double from a JsonElement
 */
@InternalRudderApi
fun JsonElement.getDouble(): Double? = this.safeJsonPrimitive?.doubleOrNull
