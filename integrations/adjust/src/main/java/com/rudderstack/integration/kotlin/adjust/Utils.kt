package com.rudderstack.integration.kotlin.adjust

import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.AnalyticsContext
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

// TODO("Extract it to a Util module")
@OptIn(InternalRudderApi::class)
internal inline fun <reified T> JsonObject.parseConfig() = LenientJson.decodeFromJsonElement<T>(this)

/**
 * TODO
 */
// TODO("Util method")
val DEFAULT_STRING = String.empty()

/**
 * TODO
 */
const val DEFAULT_INT: Int = 0

/**
 * TODO
 */
const val DEFAULT_LONG: Long = 0L

/**
 * TODO
 */
const val DEFAULT_DOUBLE: Double = 0.0

/**
 * TODO
 */
fun JsonObject.getString(key: String, defaultValue: String = DEFAULT_STRING): String {
    return runCatching {
        convertString(this[key], defaultValue)
    }.getOrElse {
        logErrorMessageAndReturnDefaultValue(this[key], defaultValue)
    }
}

private fun convertString(value: Any?, defaultValue: String) = when (value) {
    is JsonPrimitive -> value.content
    is JsonObject -> Json.encodeToString(value)
    is JsonArray -> value.toString()
    else -> logErrorMessageAndReturnDefaultValue(value, defaultValue)
}

internal fun JsonObject.getInt(key: String, defaultValue: Int = DEFAULT_INT): Int {
    return runCatching {
        convertInt(this[key], defaultValue)
    }.getOrElse {
        logErrorMessageAndReturnDefaultValue(this[key], defaultValue)
    }
}

private fun convertInt(value: Any?, defaultValue: Int): Int = when (value) {
    is JsonPrimitive -> when {
        // We need to explicitly check and convert the value to Int
        value.intOrNull != null -> value.int
        value.longOrNull != null -> value.long.toInt()
        value.doubleOrNull != null -> value.double.toInt()
        value.isString -> value.content.toInt()
        else -> logErrorMessageAndReturnDefaultValue(value, defaultValue)
    }

    else -> logErrorMessageAndReturnDefaultValue(value, defaultValue)
}

internal fun JsonObject.getLong(key: String, defaultValue: Long = DEFAULT_INT.toLong()): Long {
    return runCatching {
        convertLong(this[key], defaultValue)
    }.getOrElse {
        logErrorMessageAndReturnDefaultValue(this[key], defaultValue)
    }
}

private fun convertLong(value: Any?, defaultValue: Long): Long = when (value) {
    is JsonPrimitive -> when {
        value.intOrNull != null -> value.int.toLong()
        value.longOrNull != null -> value.long
        value.doubleOrNull != null -> value.double.toLong()
        value.isString -> value.content.toLong()
        else -> logErrorMessageAndReturnDefaultValue(value, defaultValue)
    }

    else -> logErrorMessageAndReturnDefaultValue(value, defaultValue)
}

internal fun JsonObject.getDouble(key: String, defaultValue: Double = DEFAULT_DOUBLE): Double {
    return runCatching {
        convertDouble(this[key], defaultValue)
    }.getOrElse {
        logErrorMessageAndReturnDefaultValue(this[key], defaultValue)
    }
}

private fun convertDouble(value: Any?, defaultValue: Double): Double = when (value) {
    is JsonPrimitive -> when {
        value.intOrNull != null -> value.int.toDouble()
        value.longOrNull != null -> value.long.toDouble()
        value.doubleOrNull != null -> value.double
        value.isString -> value.content.toDoubleOrNull() ?: logErrorMessageAndReturnDefaultValue(value, defaultValue)
        else -> logErrorMessageAndReturnDefaultValue(value, defaultValue)
    }

    else -> logErrorMessageAndReturnDefaultValue(value, defaultValue)
}

private fun <T> logErrorMessageAndReturnDefaultValue(value: Any?, defaultValue: T): T {
    // TODO: Remove this println statement
    println("Error while converting ($value) to the required type. Using default value: $defaultValue.")
    LoggerAnalytics.error("Error while converting [$value] to the required type. Using default value: $defaultValue.")
    return defaultValue
}

/**
 * TODO
 */
fun AnalyticsContext.toJsonObject(key: String): JsonObject {
    return this[key]?.let {
        it as? JsonObject
    } ?: emptyJsonObject
}

/**
 * Constants used in the Adjust integration.
 */
// TODO("Move these keys in the core SDK.")
object Constants {

    /**
     * TODO
     */
    const val REVENUE = "revenue"

    /**
     * TODO
     */
    const val CURRENCY = "currency"

    /**
     * TODO
     */
    const val TRAITS = "traits"
}
