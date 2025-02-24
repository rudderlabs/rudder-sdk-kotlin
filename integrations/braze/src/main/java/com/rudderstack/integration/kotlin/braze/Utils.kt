package com.rudderstack.integration.kotlin.braze

import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Parses the [JsonObject] to the specified type [T].
 */
@OptIn(InternalRudderApi::class)
internal inline fun <reified T> JsonObject.parse(): T? {
    return this.takeIf { it.isNotEmpty() }?.let {
        LenientJson.decodeFromJsonElement<T>(this)
    } ?: run {
        LoggerAnalytics.debug("AdjustIntegration: The configuration is empty.")
        null
    }
}

/**
 * Extension function to parse a JsonObject into StandardProperties.
 *
 * @return StandardProperties object parsed from the JsonObject.
 */
internal fun JsonObject.getStandardProperties(): StandardProperties {
    return this.parse<StandardProperties>() ?: StandardProperties()
}

/**
 * Extension function to filter the standard properties from the [JsonObject].
 *
 * **NOTE**: If there are multiple keys with the same name in the `products` array, only the last one will be considered.
 *
 * @param rootKeys The list of keys to be filtered from the root level.
 * @param productKeys The list of keys to be filtered from the products array.
 * @return JsonObject with the filtered values.
 */
internal fun JsonObject.filter(rootKeys: List<String>, productKeys: List<String> = emptyList()): JsonObject {
    val filteredRootProperties: JsonObject = this.filterKeys(rootKeys)
    val filteredProductProperties: JsonObject =
        this["products"]?.jsonArray
            ?.filterKeys(productKeys)
            ?: JsonObject(emptyMap())

    return JsonObject(filteredRootProperties + filteredProductProperties)
}

/**
 * Extension function to filter the keys from the [JsonObject].
 *
 * @param keys The list of keys to be filtered.
 * @return JsonObject with the filtered keys.
 */
private fun <T : Iterable<*>> JsonObject.filterKeys(keys: T): JsonObject = this.filterNot { it.key in keys }
    .let(::JsonObject)

/**
 * Extension function to filter the keys from the [JsonArray].
 *
 * **NOTE**: If there are multiple keys with the same name in the array, only the last one will be considered.
 *
 * @param keys The list of keys to be filtered.
 * @return JsonObject with the filtered keys.
 */
private fun <T : Iterable<*>> JsonArray.filterKeys(keys: T): JsonObject = this.mapNotNull { it.jsonObject }
    .flatMap { it.entries }
    .filterNot { it.key in keys }
    .associate { it.key to it.value }
    .let(::JsonObject)

/**
 * Extension function to get the `brazeExternalId` if it exists, otherwise the userId.
 */
internal fun IdentifyTraits.getExternalIdOrUserId() = this.context.brazeExternalId?.takeIf { it.isNotEmpty() } ?: userId

/**
 * Extension function to get the [IdentifyTraits] object from the [IdentifyEvent].
 *
 * @return The [IdentifyTraits] object parsed from the [IdentifyEvent].
 */
internal fun IdentifyEvent.toIdentifyTraits(): IdentifyTraits {
    val context = this.context.parse<Context>() ?: Context()

    val customTraits = this.traits?.filter(rootKeys = Traits.getKeysAsList())
    return IdentifyTraits(
        context = context,
        userId = this.userId,
        customTraits = customTraits,
    )
}

/**
 * Extension property that safely accesses the traits JsonObject from an IdentifyEvent.
 * Retrieves the "traits" object from the event's context and converts it to a JsonObject.
 *
 * @return JsonObject containing the traits if present in context, null otherwise
 */
internal val IdentifyEvent.traits: JsonObject?
    get() = this.context["traits"]?.jsonObject

/**
 * Returns a new [IdentifyTraits] object with updated traits or null if no they are the same.
 *
 * @param previousTraits The previous traits to compare against
 * @return The new traits object with de-duped values
 */
internal infix fun IdentifyTraits.deDupe(previousTraits: IdentifyTraits?): IdentifyTraits {
    val currentTraits = this
    if (previousTraits == null) return currentTraits

    return IdentifyTraits(
        userId = currentTraits.userId takeIfDifferent previousTraits.userId,
        context = Context(
            traits = with(currentTraits.context.traits) {
                Traits(
                    email = email takeIfDifferent previousTraits.context.traits.email,
                    firstName = firstName takeIfDifferent previousTraits.context.traits.firstName,
                    lastName = lastName takeIfDifferent previousTraits.context.traits.lastName,
                    gender = gender takeIfDifferent previousTraits.context.traits.gender,
                    phone = phone takeIfDifferent previousTraits.context.traits.phone,
                    address = address takeIfDifferent previousTraits.context.traits.address,
                    birthday = birthday takeIfDifferent previousTraits.context.traits.birthday
                )
            },
            externalId = context.externalId takeIfDifferent previousTraits.context.externalId
        )
    )
}

/**
 * Returns the new value if it is different from the old value, otherwise null.
 */
private infix fun <T> T.takeIfDifferent(old: T): T? = this.takeIf { this != old }

private val iso8601DateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

/**
 * Tries to convert the given [value] to a [Long] representing the time in milliseconds.
 *
 * @param value The value to be converted.
 * @return The time in milliseconds if the conversion is successful, otherwise `null`.
 */
internal fun tryDateConversion(value: String): Long? {
    return runCatching {
        iso8601DateFormatter.parse(value)?.time
    }.getOrNull()
}

internal fun logUnsupportedType(key: String, value: Any) {
    LoggerAnalytics.error("BrazeIntegration: Unsupported type for custom trait $key: $value")
}

internal fun getDeDupedCustomTraits(
    deDupeEnabled: Boolean,
    newCustomTraits: JsonObject?,
    oldCustomTraits: JsonObject?
): JsonObject {
    if (newCustomTraits == null) return JsonObject(emptyMap())
    if (oldCustomTraits == null) return newCustomTraits
    if (!deDupeEnabled) return newCustomTraits
    return buildJsonObject {
        newCustomTraits.forEach { (key, value) ->
            val oldValue = oldCustomTraits[key]
            if (oldValue != value) {
                put(key, value)
            }
        }
    }
}

internal fun JsonObject.toJSONObject(): JSONObject {
    return JSONObject(this.toString())
}
