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
    return key
        .lowercase(Locale.getDefault())
        .trim()
        .replace(" ", "_")
        .apply {
            if (this.length > 40) {
                this.substring(0, 40)
            }
        }
}

internal fun attachAllCustomProperties(params: Bundle, properties: JsonObject?) {
    if (properties.isNullOrEmpty()) {
        return
    }
    for (key in properties.keys) {
        val firebaseKey: String = getTrimmedKey(key)
        if (TRACK_RESERVED_KEYWORDS.contains(firebaseKey) || properties.isKeyEmpty(key)) {
            continue
        }

        when {
            properties.isString(key) -> {
                var stringVal = properties.getString(key).orEmpty()
                if (stringVal.length > 100) {
                    stringVal = stringVal.substring(0, 100)
                }
                params.putString(firebaseKey, stringVal)
            }
            properties.isInt(key) -> params.putInt(firebaseKey, properties.getInt(key) ?: 0)
            properties.isLong(key) -> params.putLong(firebaseKey, properties.getLong(key) ?: 0)
            properties.isDouble(key) -> params.putDouble(firebaseKey, properties.getDouble(key) ?: 0.0)
            properties.isBoolean(key) -> params.putBoolean(firebaseKey, properties.getBoolean(key) ?: false)
            else -> {
                val stringValue = properties[key].toString()
                // if length exceeds 100, don't send the property
                if (stringValue.length <= 100) {
                    params.putString(firebaseKey, stringValue)
                }
            }
        }
    }
}
