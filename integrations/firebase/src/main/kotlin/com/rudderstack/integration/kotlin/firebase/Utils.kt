package com.rudderstack.integration.kotlin.firebase

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.rudderstack.sdk.kotlin.android.utils.getBoolean
import com.rudderstack.sdk.kotlin.android.utils.getDouble
import com.rudderstack.sdk.kotlin.android.utils.getInt
import com.rudderstack.sdk.kotlin.android.utils.getLong
import com.rudderstack.sdk.kotlin.android.utils.isBoolean
import com.rudderstack.sdk.kotlin.android.utils.isDouble
import com.rudderstack.sdk.kotlin.android.utils.isInt
import com.rudderstack.sdk.kotlin.android.utils.isKeyEmpty
import com.rudderstack.sdk.kotlin.android.utils.isLong
import com.rudderstack.sdk.kotlin.android.utils.isString
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceEvents
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceParamNames
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

private const val MAX_KEY_LENGTH = 40
private const val MAX_PROPERTY_VALUE_LENGTH = 100
internal const val MAX_TRAITS_VALUE_LENGTH = 36

internal val IDENTIFY_RESERVED_KEYWORDS = listOf("age", "gender", "interest")

internal val RESERVED_EVENTS_KEYWORDS = listOf(
    "product_id", "name", "category", "quantity", "price", "currency", "value", "revenue", "total",
    "tax", "shipping", "coupon", "cart_id", "payment_method", "query", "list_id", "promotion_id", "creative",
    "affiliation", "share_via", "order_id", ECommerceParamNames.PRODUCTS, FirebaseAnalytics.Param.SCREEN_NAME
)

internal val ECOMMERCE_EVENTS_MAPPING = mapOf(
    ECommerceEvents.PAYMENT_INFO_ENTERED to FirebaseAnalytics.Event.ADD_PAYMENT_INFO,
    ECommerceEvents.PRODUCT_ADDED to FirebaseAnalytics.Event.ADD_TO_CART,
    ECommerceEvents.PRODUCT_ADDED_TO_WISH_LIST to FirebaseAnalytics.Event.ADD_TO_WISHLIST,
    ECommerceEvents.CHECKOUT_STARTED to FirebaseAnalytics.Event.BEGIN_CHECKOUT,
    ECommerceEvents.ORDER_COMPLETED to FirebaseAnalytics.Event.PURCHASE,
    ECommerceEvents.ORDER_REFUNDED to FirebaseAnalytics.Event.REFUND,
    ECommerceEvents.PRODUCTS_SEARCHED to FirebaseAnalytics.Event.SEARCH,
    ECommerceEvents.CART_SHARED to FirebaseAnalytics.Event.SHARE,
    ECommerceEvents.PRODUCT_SHARED to FirebaseAnalytics.Event.SHARE,
    ECommerceEvents.PRODUCT_VIEWED to FirebaseAnalytics.Event.VIEW_ITEM,
    ECommerceEvents.PRODUCT_LIST_VIEWED to FirebaseAnalytics.Event.VIEW_ITEM_LIST,
    ECommerceEvents.PRODUCT_REMOVED to FirebaseAnalytics.Event.REMOVE_FROM_CART,
    ECommerceEvents.PRODUCT_CLICKED to FirebaseAnalytics.Event.SELECT_CONTENT,
    ECommerceEvents.PROMOTION_VIEWED to FirebaseAnalytics.Event.VIEW_PROMOTION,
    ECommerceEvents.PROMOTION_CLICKED to FirebaseAnalytics.Event.SELECT_PROMOTION,
    ECommerceEvents.CART_VIEWED to FirebaseAnalytics.Event.VIEW_CART
)

internal val EVENT_WITH_PRODUCTS_ARRAY = listOf(
    FirebaseAnalytics.Event.BEGIN_CHECKOUT,
    FirebaseAnalytics.Event.PURCHASE,
    FirebaseAnalytics.Event.REFUND,
    FirebaseAnalytics.Event.VIEW_ITEM_LIST,
    FirebaseAnalytics.Event.VIEW_CART
)

internal val EVENT_WITH_SINGLE_PRODUCT = listOf(
    FirebaseAnalytics.Event.ADD_TO_CART,
    FirebaseAnalytics.Event.ADD_TO_WISHLIST,
    FirebaseAnalytics.Event.VIEW_ITEM,
    FirebaseAnalytics.Event.REMOVE_FROM_CART
)

internal val ECOMMERCE_PROPERTY_MAPPING = mapOf(
    "payment_method" to FirebaseAnalytics.Param.PAYMENT_TYPE,
    "coupon" to FirebaseAnalytics.Param.COUPON,
    "query" to FirebaseAnalytics.Param.SEARCH_TERM,
    "list_id" to FirebaseAnalytics.Param.ITEM_LIST_ID,
    "promotion_id" to FirebaseAnalytics.Param.PROMOTION_ID,
    "creative" to FirebaseAnalytics.Param.CREATIVE_NAME,
    "affiliation" to FirebaseAnalytics.Param.AFFILIATION,
    "share_via" to FirebaseAnalytics.Param.METHOD
)

internal val PRODUCT_PROPERTIES_MAPPING = mapOf(
    "product_id" to FirebaseAnalytics.Param.ITEM_ID,
    "name" to FirebaseAnalytics.Param.ITEM_NAME,
    "category" to FirebaseAnalytics.Param.ITEM_CATEGORY,
    "quantity" to FirebaseAnalytics.Param.QUANTITY,
    "price" to FirebaseAnalytics.Param.PRICE
)

internal fun formatFirebaseKey(key: String): String {
    return key
        .trim()
        .replace(" ", "_")
        .replace("-", "_")
        .take(MAX_KEY_LENGTH)
}

internal fun attachAllCustomProperties(params: Bundle, properties: JsonObject?, isEcommerceEvent: Boolean) {
    properties?.takeIf { it.isNotEmpty() }?.keys?.forEach { key ->
        val firebaseKey = formatFirebaseKey(key)
        if ((isEcommerceEvent && firebaseKey.lowercase() in RESERVED_EVENTS_KEYWORDS) || properties.isKeyEmpty(key)) {
            return@forEach
        }
        addPropertyToBundle(params, firebaseKey, key, properties)
    }
}

private fun addPropertyToBundle(params: Bundle, firebaseKey: String, key: String, properties: JsonObject) {
    when {
        properties.isString(key) -> {
            val value = getString(value = properties[key], maxLength = MAX_PROPERTY_VALUE_LENGTH)
            params.putString(firebaseKey, value)
        }

        properties.isInt(key) -> params.putInt(firebaseKey, properties.getInt(key) ?: 0)
        properties.isLong(key) -> params.putLong(firebaseKey, properties.getLong(key) ?: 0)
        properties.isDouble(key) -> params.putDouble(firebaseKey, properties.getDouble(key) ?: 0.0)
        properties.isBoolean(key) -> params.putBoolean(firebaseKey, properties.getBoolean(key) ?: false)
        else -> properties[key]?.toString()?.take(MAX_PROPERTY_VALUE_LENGTH)?.let {
            params.putString(
                firebaseKey,
                it
            )
        }
    }
}

internal fun getBundle(): Bundle {
    return Bundle()
}

internal fun getString(value: JsonElement?, maxLength: Int): String {
    val stringValue = when (value) {
        is JsonPrimitive -> value.content
        is JsonArray, is JsonObject -> try {
            Json.encodeToString(value)
        } catch (e: Exception) {
            LoggerAnalytics.error("FirebaseIntegration: Error while converting JsonElement to String.", e)
            value.toString()
        }

        else -> value.toString()
    }

    return stringValue.take(maxLength)
}

/**
 * Extension property that safely accesses the traits JsonObject from an IdentifyEvent.
 * Retrieves the "traits" object from the event's context and converts it to a JsonObject.
 *
 * @return JsonObject containing the traits if present in context, null otherwise
 */
internal val IdentifyEvent.traits: JsonObject?
    get() = this.context["traits"]?.jsonObject
