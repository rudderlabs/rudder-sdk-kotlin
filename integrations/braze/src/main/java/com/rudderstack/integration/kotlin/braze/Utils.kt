package com.rudderstack.integration.kotlin.braze

import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.ExternalId
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Parses the [JsonObject] to the specified type [T].
 */
@OptIn(InternalRudderApi::class)
internal inline fun <reified T> JsonObject.parse() = LenientJson.decodeFromJsonElement<T>(this)

/**
 * Extension function to parse a JsonObject into StandardProperties.
 *
 * @return StandardProperties object parsed from the JsonObject.
 */
internal fun JsonObject.getStandardProperties(): StandardProperties {
    return this.parse<StandardProperties>()
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
 * Extension function to get the custom properties from the [JsonObject].
 *
 * **NOTE**: If there are multiple keys with the same name in the `products` array, only the last one will be considered.
 *
 * @param filteredRootKeys The list of keys to be filtered from the root level.
 * @param filteredProductKeys The list of keys to be filtered from the products.
 * @return CustomProperties object parsed from the JsonObject.
 */
internal fun JsonObject.getCustomProperties(
    filteredRootKeys: List<String> = StandardProperties.getKeysAsList(),
    filteredProductKeys: List<String> = Product.getKeysAsList(),
): JsonObject {
    val filteredRootProperties: JsonObject = this.filterKeys(filteredRootKeys)
    val filteredProductProperties: JsonObject =
        this["products"]?.jsonArray
            ?.filterKeys(filteredProductKeys)
            ?: JsonObject(emptyMap())

    return JsonObject(filteredRootProperties + filteredProductProperties)
}

/**
 * Extension function to filter the keys from the [JsonObject].
 *
 * @param filterKeys The list of keys to be filtered.
 * @return JsonObject with the filtered keys.
 */
internal fun <T : Iterable<*>> JsonObject.filterKeys(filterKeys: T): JsonObject = this.filter { it.key !in filterKeys }
    .let(::JsonObject)

/**
 * Extension function to filter the keys from the [JsonArray].
 *
 * **NOTE**: If there are multiple keys with the same name in the array, only the last one will be considered.
 *
 * @param filterKeys The list of keys to be filtered.
 * @return JsonObject with the filtered keys.
 */
internal fun <T : Iterable<*>> JsonArray.filterKeys(filterKeys: T): JsonObject = this.mapNotNull { it.jsonObject }
    .flatMap { it.entries }
    .filterNot { it.key in filterKeys }
    .associate { it.key to it.value }
    .let(::JsonObject)

/**
 * Extension function to get the `brazeExternalId` if it exists, otherwise the userId.
 */
internal fun IdentifyTraits.getExternalIdOrUserId() = context.brazeExternalId?.ifEmpty { userId }

/**
 * Extension function to get the [IdentifyTraits] object from the [IdentifyEvent].
 *
 * @return The [IdentifyTraits] object parsed from the [IdentifyEvent].
 */
internal fun IdentifyEvent.getIdentifyTraits(): IdentifyTraits {
    val context = this.context.parse<Context>()
    return IdentifyTraits(
        context = context,
        userId = this.userId
    )
}

/**
 * Returns a new [IdentifyTraits] object with only the traits that have changed between the current and previous traits.
 *
 * @param currentTraits The current traits to compare against
 * @param previousTraits The previous traits to compare against
 * @return The new traits object with only the changed traits
 */
internal fun getDeDupedIdentifyTraits(currentTraits: IdentifyTraits, previousTraits: IdentifyTraits?): IdentifyTraits {
    if (previousTraits == null) return currentTraits

    return IdentifyTraits(
        userId = TraitsMatcher.UserId.getNewTraitsIfUpdatedOrNull(currentTraits, previousTraits),
        context = Context(
            traits = Traits(
                email = TraitsMatcher.Email.getNewTraitsIfUpdatedOrNull(currentTraits, previousTraits),
                firstName = TraitsMatcher.FirstName.getNewTraitsIfUpdatedOrNull(currentTraits, previousTraits),
                lastName = TraitsMatcher.LastName.getNewTraitsIfUpdatedOrNull(currentTraits, previousTraits),
                gender = TraitsMatcher.LastName.getNewTraitsIfUpdatedOrNull(currentTraits, previousTraits),
                phone = TraitsMatcher.LastName.getNewTraitsIfUpdatedOrNull(currentTraits, previousTraits),
                address = TraitsMatcher.Address.getNewTraitsIfUpdatedOrNull(currentTraits, previousTraits),
                birthday = TraitsMatcher.Birthday.getNewTraitsIfUpdatedOrNull(currentTraits, previousTraits),
            ),
            externalId = TraitsMatcher.ListOfExternalId.getNewTraitsIfUpdatedOrNull(currentTraits, previousTraits),
        )
    )
}

/**
 * A sealed class representing different trait matchers for identifying changes in user traits.
 *
 * @param T The type of the trait value being matched
 * @param extractor A function that extracts the trait value from [IdentifyTraits]
 * @param key The string key identifying this trait matcher
 */
internal sealed class TraitsMatcher<T>(
    private val extractor: (IdentifyTraits) -> T?,
    val key: String,
) {

    /**
     * Compares trait values between new and old traits, returning the new traits only if it has changed.
     *
     * @param newTraits The new traits to check
     * @param oldTraits The previous traits to compare against
     * @return The new trait if it differs from the old traits, null otherwise
     */
    internal fun getNewTraitsIfUpdatedOrNull(newTraits: IdentifyTraits, oldTraits: IdentifyTraits): T? {
        val newValue = extractor(newTraits)
        val oldValue = extractor(oldTraits)

        return when (newValue == oldValue) {
            true -> null
            false -> newValue
        }
    }

    data object UserId : TraitsMatcher<String>({ it.userId }, "userId")
    data object Email : TraitsMatcher<String>({ it.context.traits.email }, "email")
    data object FirstName : TraitsMatcher<String>({ it.context.traits.firstName }, "firstName")
    data object LastName : TraitsMatcher<String>({ it.context.traits.lastName }, "lastName")
    data object Gender : TraitsMatcher<String>({ it.context.traits.gender }, "gender")
    data object Phone : TraitsMatcher<String>({ it.context.traits.phone }, "phone")
    data object Address :
        TraitsMatcher<com.rudderstack.integration.kotlin.braze.Address>({ it.context.traits.address }, "address")

    data object Birthday : TraitsMatcher<Calendar>({ it.context.traits.birthday }, "birthday")
    data object ListOfExternalId : TraitsMatcher<List<ExternalId>>({ it.context.externalId }, "externalId")

    companion object {

        private val standardTraits = setOf(
            UserId,
            Email,
            FirstName,
            LastName,
            Gender,
            Phone,
            Address,
            Birthday,
            ListOfExternalId,
        )

        /**
         * The standard trait keys.
         */
        val standardTraitKeys = standardTraits.map { it.key }.toSet()
    }
}

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
