@file:JvmName("JsonUtils")

package com.rudderstack.core.internals.utils

import kotlinx.serialization.json.Json

/**
 * `LenientJson` is a predefined instance of the `Json` class configured with lenient settings to handle JSON serialization and deserialization.
 * This instance is particularly useful when working with JSON data that may have unknown keys or might not strictly conform to the standard JSON format.
 * It provides flexibility and robustness by ignoring unknown keys and allowing non-strict JSON parsing.
 *
 * This configuration can be beneficial when working with APIs or data sources that might evolve over time or provide inconsistent JSON data.
 *
 * @property ignoreUnknownKeys A flag that, when set to true, instructs the JSON parser to ignore any unknown keys found in the JSON input. This allows for forward compatibility with future changes in JSON structures.
 * @property isLenient A flag that, when set to true, allows the JSON parser to be lenient in parsing the JSON input. This enables the parser to handle malformed or non-standard JSON without throwing an exception.
 */
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
}
