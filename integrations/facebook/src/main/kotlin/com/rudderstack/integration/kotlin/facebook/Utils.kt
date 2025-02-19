package com.rudderstack.integration.kotlin.facebook

import android.os.Bundle
import com.facebook.appevents.AppEventsConstants
import com.rudderstack.sdk.kotlin.android.utils.getDouble
import com.rudderstack.sdk.kotlin.android.utils.getInt
import com.rudderstack.sdk.kotlin.android.utils.getLong
import com.rudderstack.sdk.kotlin.android.utils.getString
import com.rudderstack.sdk.kotlin.android.utils.isDouble
import com.rudderstack.sdk.kotlin.android.utils.isInt
import com.rudderstack.sdk.kotlin.android.utils.isLong
import com.rudderstack.sdk.kotlin.android.utils.isString
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceEvents
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceParamNames
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

internal const val COMPLETE_REGISTRATION = "Complete Registration"
internal const val ACHIEVE_LEVEL = "Achieve Level"
internal const val COMPLETE_TUTORIAL = "Complete Tutorial"
internal const val UNLOCK_ACHIEVEMENT = "Unlock Achievement"
internal const val SUBSCRIBE = "Subscribe"
internal const val START_TRIAL = "Start Trial"
internal const val SPEND_CREDITS = "Spend Credits"

internal const val CURRENCY = "currency"
internal const val REVENUE = "revenue"
internal const val RATING = "rating"
internal const val VALUE = "value"
internal const val PROMOTION_NAME = "name"
internal const val DESCRIPTION = "description"

private const val EMPTY_STRING = ""

@Serializable
internal data class Address(
    val city: String = EMPTY_STRING,
    val country: String = EMPTY_STRING,
    @SerialName("postalcode") val postalCode: String = EMPTY_STRING,
    val state: String = EMPTY_STRING,
    val street: String = EMPTY_STRING
)

internal inline fun <reified T> JsonObject.parseConfig() = LenientJson.decodeFromJsonElement<T>(this)

internal val FACEBOOK_EVENTS_MAPPING = mapOf(
    ECommerceEvents.PRODUCTS_SEARCHED to AppEventsConstants.EVENT_NAME_SEARCHED,
    ECommerceEvents.PRODUCT_VIEWED to AppEventsConstants.EVENT_NAME_VIEWED_CONTENT,
    ECommerceEvents.PRODUCT_ADDED to AppEventsConstants.EVENT_NAME_ADDED_TO_CART,
    ECommerceEvents.PRODUCT_ADDED_TO_WISH_LIST to AppEventsConstants.EVENT_NAME_ADDED_TO_WISHLIST,
    ECommerceEvents.PAYMENT_INFO_ENTERED to AppEventsConstants.EVENT_NAME_ADDED_PAYMENT_INFO,
    ECommerceEvents.CHECKOUT_STARTED to AppEventsConstants.EVENT_NAME_INITIATED_CHECKOUT,
    COMPLETE_REGISTRATION to AppEventsConstants.EVENT_NAME_COMPLETED_REGISTRATION,
    ACHIEVE_LEVEL to AppEventsConstants.EVENT_NAME_ACHIEVED_LEVEL,
    COMPLETE_TUTORIAL to AppEventsConstants.EVENT_NAME_COMPLETED_TUTORIAL,
    UNLOCK_ACHIEVEMENT to AppEventsConstants.EVENT_NAME_UNLOCKED_ACHIEVEMENT,
    SUBSCRIBE to AppEventsConstants.EVENT_NAME_SUBSCRIBE,
    START_TRIAL to AppEventsConstants.EVENT_NAME_START_TRIAL,
    ECommerceEvents.PROMOTION_CLICKED to AppEventsConstants.EVENT_NAME_AD_CLICK,
    ECommerceEvents.PROMOTION_VIEWED to AppEventsConstants.EVENT_NAME_AD_IMPRESSION,
    SPEND_CREDITS to AppEventsConstants.EVENT_NAME_SPENT_CREDITS,
    ECommerceEvents.PRODUCT_REVIEWED to AppEventsConstants.EVENT_NAME_RATED
)

internal val TRACK_RESERVED_KEYWORDS = setOf(
    ECommerceParamNames.PRODUCT_ID,
    RATING,
    PROMOTION_NAME,
    ECommerceParamNames.ORDER_ID,
    ECommerceParamNames.CURRENCY,
    DESCRIPTION,
    ECommerceParamNames.QUERY,
    VALUE,
    ECommerceParamNames.PRICE,
    ECommerceParamNames.REVENUE
)

internal fun getCurrency(eventProperties: JsonObject): String {
    return eventProperties.getString(CURRENCY) ?: "USD"
}

internal fun getRevenue(eventProperties: JsonObject): Double? {
    return eventProperties.getDouble(REVENUE)
}

internal fun getValueToSum(properties: JsonObject, propertyKey: String?): Double? {
    return properties.entries
        .find { it.key.equals(propertyKey, ignoreCase = true) }
        ?.value?.toString()?.toDoubleOrNull()
}

internal fun addPropertiesToBundle(properties: JsonObject, params: Bundle, isScreenEvent: Boolean) {
    for (key in properties.keys) {
        if (!isScreenEvent && TRACK_RESERVED_KEYWORDS.contains(key)) {
            continue
        }
        when {
            properties.isString(key) -> params.putString(key, properties.getString(key))
            properties.isInt(key) -> params.putInt(key, properties.getInt(key) ?: 0)
            properties.isLong(key) -> params.putLong(key, properties.getLong(key) ?: 0)
            properties.isDouble(key) -> params.putDouble(key, properties.getDouble(key) ?: 0.0)
            else -> params.putString(key, properties[key].toString())
        }
    }
}
