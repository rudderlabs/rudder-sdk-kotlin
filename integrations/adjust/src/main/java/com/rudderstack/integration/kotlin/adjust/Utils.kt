package com.rudderstack.integration.kotlin.adjust

import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.AnalyticsContext
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
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
internal fun JsonObject.getStringOrNull(key: String): String? = runCatching {
    convertToString(this[key])
}.getOrElse {
    logErrorMessageAndReturnNull(this[key])
}

private fun convertToString(value: Any?): String? = when (value) {
    is JsonPrimitive -> value.content
    is JsonObject -> Json.encodeToString(value)
    is JsonArray -> value.toString()
    else -> logErrorMessageAndReturnNull(value)
}

internal fun JsonObject.getIntOrNull(key: String): Int? = runCatching {
    convertToInt(this[key])
}.getOrElse {
    logErrorMessageAndReturnNull(this[key])
}

private fun convertToInt(value: Any?): Int? = when (value) {
    is JsonPrimitive -> when {
        // We need to explicitly check and convert the value to Int
        value.intOrNull != null -> value.int
        value.longOrNull != null -> value.long.toInt()
        value.doubleOrNull != null -> value.double.toInt()
        value.isString -> value.content.toInt()
        else -> logErrorMessageAndReturnNull(value)
    }

    else -> logErrorMessageAndReturnNull(value)
}

internal fun JsonObject.getLongOrNull(key: String): Long? = runCatching {
    convertToLong(this[key])
}.getOrElse {
    logErrorMessageAndReturnNull(this[key])
}

private fun convertToLong(value: Any?): Long? = when (value) {
    is JsonPrimitive -> when {
        // We need to explicitly check and convert the value to Long
        value.intOrNull != null -> value.int.toLong()
        value.longOrNull != null -> value.long
        value.doubleOrNull != null -> value.double.toLong()
        value.isString -> value.content.toLong()
        else -> logErrorMessageAndReturnNull(value)
    }

    else -> logErrorMessageAndReturnNull(value)
}

internal fun JsonObject.getDoubleOrNull(key: String): Double? = runCatching {
    convertToDouble(this[key])
}.getOrElse {
    logErrorMessageAndReturnNull(this[key])
}

private fun convertToDouble(value: Any?): Double? = when (value) {
    is JsonPrimitive -> when {
        // We need to explicitly check and convert the value to Double
        value.intOrNull != null -> value.int.toDouble()
        value.longOrNull != null -> value.long.toDouble()
        value.doubleOrNull != null -> value.double
        value.isString -> value.content.toDoubleOrNull() ?: logErrorMessageAndReturnNull(value)
        else -> logErrorMessageAndReturnNull(value)
    }

    else -> logErrorMessageAndReturnNull(value)
}

private inline fun <reified T> logErrorMessageAndReturnNull(value: Any?): T? {
    // TODO: Remove this println statement
    println("Error while converting ($value) to the ${T::class} type.")
    LoggerAnalytics.debug("Integration: Error while converting [$value] to the ${T::class} type.")
    return null
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

internal fun List<EventToTokenMapping>.getTokenOrNull(event: String): String? =
    this.find { it.event == event }?.token?.takeUnless { it.isBlank() }
