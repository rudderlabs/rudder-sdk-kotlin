@file:JvmName("JsonUtils")

package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.ExternalId
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * `LenientJson` is a predefined instance of the `Json` class configured with lenient settings to handle JSON serialization and deserialization.
 * This instance is particularly useful when working with JSON data that may have unknown keys or might not strictly conform to the standard JSON format.
 * It provides flexibility and robustness by ignoring unknown keys and allowing non-strict JSON parsing.
 *
 * This configuration can be beneficial when working with APIs or data sources that might evolve over time or provide inconsistent JSON data.
 *
 */
@InternalRudderApi
val LenientJson = Json {
    /**
     * Instructs the parser to ignore any unknown keys in the JSON input.
     * This setting is useful when the data source might include additional fields that are not defined in the data model.
     */
    ignoreUnknownKeys = true

    /**
     * Enables lenient parsing mode, allowing the parser to handle non-standard JSON or malformed data.
     * This setting is useful when dealing with JSON input that might not strictly adhere to the standard format.
     */
    isLenient = true

    /**
     * Encodes the default values of the properties.
     */
    encodeDefaults = true
}

/**
 * Encodes the event to a JSON string, filtering out empty JSON objects.
 */
internal fun Event.encodeToString(): String {
    val stringEvent = LenientJson.encodeToString(this)
    val filteredEvent = LenientJson.parseToJsonElement(stringEvent)
        .jsonObject.filterNot { (k, v) ->
            (k == "properties" && v == emptyJsonObject) ||
                (k == "traits" && v == emptyJsonObject) ||
                (k == "userId" && v is JsonPrimitive && v.content == String.empty())
        }
    return LenientJson.encodeToString(filteredEvent)
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
 * Adds all key-value pairs from the given `JsonObject` to the current `JsonObjectBuilder`.
 *
 * This method iterates over all entries in the specified `JsonObject` and inserts each key-value pair
 * into the `JsonObjectBuilder`. It is typically used when constructing a new `JsonObject` that needs
 * to include values from an existing JSON object.
 *
 * @param jsonObject The `JsonObject` whose key-value pairs are to be added to the current builder.
 */
fun JsonObjectBuilder.putAll(jsonObject: JsonObject) {
    jsonObject.forEach { (key, value) ->
        put(key, value)
    }
}

/**
 * Converts a list of `ExternalIds` to a JsonObject.
 *
 * If the list is empty, the method returns an empty JSON object.
 * Else the method returns a JSON object with the following structure:
 *
 * ```
 * "externalId": [
 *     {
 *         "id": "<some-value>",
 *         "type": "brazeExternalId"
 *     },
 *     {
 *         "id": "<some-value>",
 *         "type": "ga4"
 *     }
 * ],
 * ```
 */
internal fun List<ExternalId>.toJsonObject(): JsonObject {
    if (this.isEmpty()) return emptyJsonObject
    val externalIds = LenientJson.encodeToJsonElement(this) as JsonArray
    return buildJsonObject {
        put("externalId", externalIds)
    }
}
