package com.rudderstack.integration.kotlin.facebook

import com.facebook.appevents.AppEventsConstants
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceEvents
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

internal const val PRICE = "price"
internal const val PRODUCT_ID = "product_id"
internal const val CURRENCY = "currency"
internal const val REVENUE = "revenue"
internal const val ORDER_ID = "order_id"
internal const val RATING = "rating"
internal const val VALUE = "value"
internal const val QUERY = "query"
internal const val PROMOTION_NAME = "name"
internal const val DESCRIPTION = "description"

@Serializable
data class Address(
    val city: String,
    val country: String,
    @SerialName("postalcode") val postalCode: String,
    val state: String,
    val street: String
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
    PRODUCT_ID,
    RATING,
    PROMOTION_NAME,
    ORDER_ID,
    CURRENCY,
    DESCRIPTION,
    QUERY,
    VALUE,
    PRICE,
    REVENUE
)
