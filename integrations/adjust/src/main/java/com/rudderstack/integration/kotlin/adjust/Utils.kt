package com.rudderstack.integration.kotlin.adjust

import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.models.AnalyticsContext
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

private const val ERROR_NULL_VALUE = "Value cannot be null"
private const val ERROR_UNSUPPORTED_JSON_ELEMENT = "Unsupported JsonElement type"
private const val ERROR_UNSUPPORTED_TYPE = "Unsupported type"

/**
 * Parses the [JsonObject] to the specified type [T].
 */
@OptIn(InternalRudderApi::class)
internal inline fun <reified T> JsonObject.parseConfig(logger: Logger): T? {
    return this.takeIf { it.isNotEmpty() }?.let {
        LenientJson.decodeFromJsonElement<T>(this)
    } ?: run {
        logger.debug("AdjustIntegration: The configuration is empty.")
        null
    }
}

/**
 * Extracts a string value from the [JsonObject] and returns it.
 * If the value is not present or is not a string, it returns null.
 *
 * @param key The key to extract the value from.
 * @return The string value if present, else null.
 */
internal fun JsonObject.getStringOrNull(key: String, logger: Logger): String? = runCatching {
    convertToString(this[key])
}.getOrElse {
    logErrorMessageAndReturnNull(this[key], logger)
}

private fun convertToString(value: Any?): String = when (value) {
    is JsonPrimitive -> value.content
    is JsonObject -> Json.encodeToString(value)
    is JsonArray -> value.toString()
    null -> throw NullPointerException(ERROR_NULL_VALUE) // NOTE: This will not catch JsonNull
    else -> throw UnsupportedOperationException("$ERROR_UNSUPPORTED_JSON_ELEMENT: ${value::class}")
}

/**
 * Extracts an integer value from the [JsonObject] and returns it.
 * If the value is not present or is not an integer, it returns null.
 *
 * @param key The key to extract the value from.
 * @return The integer value if present, else null.
 */
internal fun JsonObject.getIntOrNull(key: String, logger: Logger): Int? = runCatching {
    convertToInt(this[key])
}.getOrElse {
    logErrorMessageAndReturnNull(this[key], logger)
}

private fun convertToInt(value: Any?): Int = when (value) {
    is JsonPrimitive -> when {
        // We need to explicitly check and convert the value to Int
        value.intOrNull != null -> value.int
        value.longOrNull != null -> value.long.toInt()
        value.doubleOrNull != null -> value.double.toInt()
        value.isString -> value.content.toInt()
        else -> throw IllegalArgumentException("$ERROR_UNSUPPORTED_TYPE: ${value::class}")
    }

    null -> throw NullPointerException(ERROR_NULL_VALUE)
    else -> throw IllegalArgumentException("$ERROR_UNSUPPORTED_TYPE: ${value::class}")
}

/**
 * Extracts a long value from the [JsonObject] and returns it.
 * If the value is not present or is not a long, it returns null.
 *
 * @param key The key to extract the value from.
 * @return The long value if present, else null.
 */
internal fun JsonObject.getLongOrNull(key: String, logger: Logger): Long? = runCatching {
    convertToLong(this[key])
}.getOrElse {
    logErrorMessageAndReturnNull(this[key], logger)
}

private fun convertToLong(value: Any?): Long = when (value) {
    is JsonPrimitive -> when {
        // We need to explicitly check and convert the value to Long
        value.intOrNull != null -> value.int.toLong()
        value.longOrNull != null -> value.long
        value.doubleOrNull != null -> value.double.toLong()
        value.isString -> value.content.toLong()
        else -> throw IllegalArgumentException("$ERROR_UNSUPPORTED_TYPE: ${value::class}")
    }

    null -> throw NullPointerException(ERROR_NULL_VALUE)
    else -> throw IllegalArgumentException("$ERROR_UNSUPPORTED_TYPE: ${value::class}")
}

/**
 * Extracts a double value from the [JsonObject] and returns it.
 * If the value is not present or is not a double, it returns null.
 *
 * @param key The key to extract the value from.
 * @return The double value if present, else null.
 */
internal fun JsonObject.getDoubleOrNull(key: String, logger: Logger): Double? = runCatching {
    convertToDouble(this[key])
}.getOrElse {
    logErrorMessageAndReturnNull(this[key], logger)
}

private fun convertToDouble(value: Any?): Double = when (value) {
    is JsonPrimitive -> when {
        // We need to explicitly check and convert the value to Double
        value.intOrNull != null -> value.int.toDouble()
        value.longOrNull != null -> value.long.toDouble()
        value.doubleOrNull != null -> value.double
        value.isString -> value.content.toDouble()
        else -> throw IllegalArgumentException("$ERROR_UNSUPPORTED_TYPE: ${value::class}")
    }

    null -> throw NullPointerException(ERROR_NULL_VALUE)
    else -> throw IllegalArgumentException("$ERROR_UNSUPPORTED_TYPE: ${value::class}")
}

private inline fun <reified T> logErrorMessageAndReturnNull(value: Any?, logger: Logger): T? {
    logger.error("AdjustIntegration: Failed to parse value: $value of type: ${T::class.simpleName}")
    return null
}

/**
 * Converts the [AnalyticsContext] to a [JsonObject] and extracts the value associated with the key.
 *
 * @param key The key to extract the value from.
 * @return The [JsonObject] value if present, else an empty [JsonObject].
 */
internal fun AnalyticsContext.toJsonObject(key: String): JsonObject {
    return this[key] as? JsonObject ?: emptyJsonObject
}

/**
 * Constants used in the Adjust integration.
 */
internal object PropertiesConstants {

    /**
     * The Revenue key.
     */
    const val REVENUE = "revenue"

    /**
     * The Currency key.
     */
    const val CURRENCY = "currency"

    /**
     * The Traits key.
     */
    const val TRAITS = "traits"
}

/**
 * Extracts the token associated with the event from the list of [EventToTokenMapping].
 *
 * @param event The event name.
 * @return The token if present, else null.
 */
internal fun List<EventToTokenMapping>.getTokenOrNull(event: String): String? =
    this.find { it.event == event }?.token?.takeUnless { it.isBlank() }

/**
 * Data transfer object for campaign attribution data.
 */
@Serializable
internal data class CampaignDto(
    val source: String? = null,
    val name: String? = null,
    val content: String? = null,
    val adCreative: String? = null,
    val adGroup: String? = null
) {

    /**
     * Checks if this campaign has any meaningful data (at least one non-null & non-empty field).
     */
    val hasData: Boolean
        get() = !source.isNullOrBlank() ||
            !name.isNullOrBlank() ||
            !content.isNullOrBlank() ||
            !adCreative.isNullOrBlank() ||
            !adGroup.isNullOrBlank()
}

/**
 * Data transfer object for install attribution event properties.
 */
@Serializable
internal data class InstallAttributionDto(
    val provider: String,
    val trackerToken: String? = null,
    val trackerName: String? = null,
    val campaign: CampaignDto? = null
)

/**
 * Converts the [InstallAttributionDto] to a [JsonObject] using the lenient JSON serializer.
 */
@OptIn(InternalRudderApi::class)
internal fun InstallAttributionDto.toJsonObject(): JsonObject {
    return LenientJson.encodeToJsonElement(this) as JsonObject
}
