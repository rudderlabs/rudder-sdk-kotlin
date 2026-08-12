@file:Suppress("TooManyFunctions")

package com.rudderstack.integration.kotlin.clevertap

import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val NAME = "name"
private const val PHONE = "phone"
private const val EMAIL = "email"
private const val ID = "id"
private const val CLEVERTAP_NAME = "Name"
private const val CLEVERTAP_PHONE = "Phone"
private const val CLEVERTAP_EMAIL = "Email"
private const val CLEVERTAP_IDENTITY = "Identity"
private const val ADDRESS = "address"
private const val ANONYMOUS_ID = "anonymousId"
private const val COMPANY = "company"
private const val COMPANY_ID = "companyId"
private const val COMPANY_NAME = "companyName"
private const val GENDER = "gender"
private const val CLEVERTAP_GENDER = "Gender"
private const val BIRTHDAY = "birthday"
private const val CLEVERTAP_DOB = "DOB"
private const val MALE = "M"
private const val MALE_FULL = "MALE"
private const val FEMALE = "F"
private const val FEMALE_FULL = "FEMALE"
private const val ORDER_COMPLETED = "Order Completed"
private const val PRODUCTS = "products"
private const val REVENUE = "revenue"
private const val AMOUNT = "Amount"
private const val ORDER_ID = "order_id"
private const val CHARGED_ID = "Charged ID"
private const val PRODUCT_ID = "product_id"
private const val CLEVERTAP_PRODUCT_ID = "id"
private const val SCREEN_VIEWED_PREFIX = "Screen Viewed:"
private const val BIRTHDAY_FORMAT = "yyyy-MM-dd"

private val rudderTraitToCleverTapTrait = mapOf(
    NAME to CLEVERTAP_NAME,
    PHONE to CLEVERTAP_PHONE,
    EMAIL to CLEVERTAP_EMAIL,
    ID to CLEVERTAP_IDENTITY,
)

/**
 * Parses the [JsonObject] to the specified type [T].
 */
@OptIn(InternalRudderApi::class)
internal inline fun <reified T> JsonObject.parseConfig(logger: Logger): T? {
    return this.takeIf { it.isNotEmpty() }?.let {
        LenientJson.decodeFromJsonElement<T>(this)
    } ?: run {
        logger.debug("CleverTapIntegration: The configuration is empty.")
        null
    }
}

internal fun CleverTapDestinationConfig.hasValidCredentials(): Boolean = accountId.isNotBlank() && accountToken.isNotBlank()

internal fun CleverTapDestinationConfig.hasRegion(): Boolean =
    region.isNotBlank() && !region.equals(DEFAULT_REGION, ignoreCase = true)

internal val IdentifyEvent.traits: JsonObject?
    get() = this.context["traits"]?.jsonObject

internal fun IdentifyEvent.toCleverTapProfile(logger: Logger): Map<String, Any> {
    val profile = traits.toAnyMap().transformTraits(logger).toMutableMap()
    val explicitIdentity = profile[CLEVERTAP_IDENTITY]
    val hasExplicitIdentity = explicitIdentity != null &&
        (explicitIdentity !is String || explicitIdentity.isNotBlank())
    if (userId.isNotBlank() && !hasExplicitIdentity) {
        profile[CLEVERTAP_IDENTITY] = userId
    }
    if (ANONYMOUS_ID !in profile && anonymousId.isNotBlank()) {
        profile[ANONYMOUS_ID] = anonymousId
    }
    return profile
}

internal fun JsonObject?.toAnyMap(): Map<String, Any> {
    if (this == null) return emptyMap()
    val map = mutableMapOf<String, Any>()
    for ((key, element) in this) {
        extractValue(element)?.let { map[key] = it }
    }
    return map
}

private fun extractValue(element: JsonElement?): Any? = when (element) {
    JsonNull, null -> null

    is JsonPrimitive -> when {
        element.isString -> element.content
        element.booleanOrNull != null -> element.boolean
        element.intOrNull != null -> element.int
        element.longOrNull != null -> element.long
        element.doubleOrNull != null -> element.double
        else -> element.content
    }

    is JsonObject -> element.toAnyMap()

    is JsonArray -> element.mapNotNull { extractValue(it) }
}

private fun Map<String, Any>.transformTraits(logger: Logger): Map<String, Any> {
    val transformedTraits = mutableMapOf<String, Any>()

    for ((key, value) in this) {
        when {
            key in rudderTraitToCleverTapTrait -> transformedTraits[rudderTraitToCleverTapTrait.getValue(key)] = value
            key == ADDRESS || key == COMPANY -> transformedTraits.putFlattenedNestedTraits(key, value)
            else -> transformedTraits[key] = value
        }
    }

    transformedTraits.transformGender()
    transformedTraits.transformBirthday(logger)

    return transformedTraits
}

@Suppress("UNCHECKED_CAST")
private fun MutableMap<String, Any>.putFlattenedNestedTraits(key: String, value: Any) {
    val nestedTraits = value as? Map<String, Any>
    nestedTraits?.forEach { (nestedKey, nestedValue) ->
        when (nestedKey) {
            ID -> this[COMPANY_ID] = nestedValue
            NAME -> this[COMPANY_NAME] = nestedValue
            else -> this[nestedKey] = nestedValue
        }
    } ?: set(key, value)
}

private fun MutableMap<String, Any>.transformGender() {
    val gender = this[GENDER] as? String ?: return
    when (gender.uppercase()) {
        MALE, MALE_FULL -> this[CLEVERTAP_GENDER] = MALE
        FEMALE, FEMALE_FULL -> this[CLEVERTAP_GENDER] = FEMALE
    }
    remove(GENDER)
}

private fun MutableMap<String, Any>.transformBirthday(logger: Logger) {
    (this[BIRTHDAY] as? String)?.dateFromString(logger)?.let { this[CLEVERTAP_DOB] = it }
    remove(BIRTHDAY)
}

internal fun String.dateFromString(logger: Logger): Date? = runCatching {
    SimpleDateFormat(BIRTHDAY_FORMAT, Locale.US).apply { isLenient = false }.parse(this)
}.getOrElse {
    logger.warn("CleverTapIntegration: Cannot parse birthday '$this'. Expected format $BIRTHDAY_FORMAT.")
    null
}

internal fun JsonObject.toCleverTapTrackEvent(eventName: String): CleverTapTrackEvent {
    return if (eventName == ORDER_COMPLETED) {
        val (chargeDetails, items) = toChargedEvent()
        CleverTapTrackEvent.ChargedEvent(chargeDetails = chargeDetails, items = items)
    } else {
        CleverTapTrackEvent.CustomEvent(eventName = eventName, properties = toAnyMap())
    }
}

internal fun JsonObject.toCleverTapScreenEvent(screenName: String): CleverTapTrackEvent.CustomEvent =
    CleverTapTrackEvent.CustomEvent(
        eventName = "$SCREEN_VIEWED_PREFIX $screenName",
        properties = toAnyMap(),
    )

private fun JsonObject.toChargedEvent(): Pair<HashMap<String, Any>, ArrayList<HashMap<String, Any>>> {
    val chargeDetails = hashMapOf<String, Any>()
    var items = arrayListOf<HashMap<String, Any>>()

    for ((key, element) in this) {
        when (key) {
            PRODUCTS -> items = element.toProductsList()
            REVENUE -> chargeDetails[AMOUNT] = element.toRevenue()
            ORDER_ID -> extractValue(element)?.let { chargeDetails[CHARGED_ID] = it }
            else -> extractValue(element)?.let { chargeDetails[key] = it }
        }
    }

    return chargeDetails to items
}

private fun JsonElement?.toProductsList(): ArrayList<HashMap<String, Any>> {
    val products = this as? JsonArray ?: return arrayListOf()
    return products.mapNotNullTo(arrayListOf()) { product ->
        (product as? JsonObject)?.toProductMap()?.takeIf { it.isNotEmpty() }
    }
}

private fun JsonObject.toProductMap(): HashMap<String, Any> {
    val item = hashMapOf<String, Any>()
    for ((key, element) in this) {
        extractValue(element)?.let { value ->
            if (key == PRODUCT_ID) {
                item[CLEVERTAP_PRODUCT_ID] = value
            } else {
                item[key] = value
            }
        }
    }
    return item
}

private fun JsonElement?.toRevenue(): Double = extractValue(this)?.toString()?.toDoubleOrNull() ?: 0.0

internal sealed class CleverTapTrackEvent {
    data class CustomEvent(
        val eventName: String,
        val properties: Map<String, Any>,
    ) : CleverTapTrackEvent()

    data class ChargedEvent(
        val chargeDetails: HashMap<String, Any>,
        val items: ArrayList<HashMap<String, Any>>,
    ) : CleverTapTrackEvent()
}
