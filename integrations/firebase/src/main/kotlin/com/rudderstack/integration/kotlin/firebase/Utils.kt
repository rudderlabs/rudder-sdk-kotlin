package com.rudderstack.integration.kotlin.firebase

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.rudderstack.sdk.kotlin.android.utils.getBoolean
import com.rudderstack.sdk.kotlin.android.utils.getDouble
import com.rudderstack.sdk.kotlin.android.utils.getInt
import com.rudderstack.sdk.kotlin.android.utils.getLong
import com.rudderstack.sdk.kotlin.android.utils.getString
import com.rudderstack.sdk.kotlin.android.utils.isBoolean
import com.rudderstack.sdk.kotlin.android.utils.isDouble
import com.rudderstack.sdk.kotlin.android.utils.isInt
import com.rudderstack.sdk.kotlin.android.utils.isKeyEmpty
import com.rudderstack.sdk.kotlin.android.utils.isLong
import com.rudderstack.sdk.kotlin.android.utils.isString
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceEvents
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceParamNames
import kotlinx.serialization.json.JsonObject
import java.util.Locale

private const val MAX_KEY_LENGTH = 40
private const val MAX_VALUE_LENGTH = 100

internal val IDENTIFY_RESERVED_KEYWORDS = listOf("age", "gender", "interest")

internal val TRACK_RESERVED_KEYWORDS = listOf(
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

internal val EVENT_WITH_PRODUCTS_AT_ROOT = listOf(
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

internal fun getTrimmedKey(key: String): String {
    return key.lowercase(Locale.getDefault())
        .trim()
        .replace(" ", "_")
        .take(MAX_KEY_LENGTH)
}

internal fun attachAllCustomProperties(params: Bundle, properties: JsonObject?) {
    properties?.takeIf { it.isNotEmpty() }?.keys?.forEach { key ->
        val firebaseKey = getTrimmedKey(key)
        if (!isValidProperty(key, firebaseKey, properties)) return@forEach
        addPropertyToBundle(params, firebaseKey, key, properties)
    }
}

private fun isValidProperty(key: String, firebaseKey: String, properties: JsonObject): Boolean {
    return !(firebaseKey in TRACK_RESERVED_KEYWORDS || properties.isKeyEmpty(key))
}

private fun addPropertyToBundle(params: Bundle, firebaseKey: String, key: String, properties: JsonObject) {
    when {
        properties.isString(key) -> params.putString(firebaseKey, properties.getString(key).orEmpty().take(MAX_VALUE_LENGTH))
        properties.isInt(key) -> params.putInt(firebaseKey, properties.getInt(key) ?: 0)
        properties.isLong(key) -> params.putLong(firebaseKey, properties.getLong(key) ?: 0)
        properties.isDouble(key) -> params.putDouble(firebaseKey, properties.getDouble(key) ?: 0.0)
        properties.isBoolean(key) -> params.putBoolean(firebaseKey, properties.getBoolean(key) ?: false)
        else -> properties[key]?.toString()?.takeIf { it.length <= MAX_VALUE_LENGTH }?.let {
            params.putString(
                firebaseKey,
                it
            )
        }
    }
}
