@file:Suppress("TooManyFunctions")

package com.rudderstack.integration.kotlin.appsflyer

import com.appsflyer.AFInAppEventParameterName
import com.appsflyer.AFInAppEventType
import com.rudderstack.sdk.kotlin.android.utils.getArray
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceEvents
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceParamNames
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull

internal const val CREATIVE = "creative"

// Reserved keywords for filtering custom properties
internal val TRACK_RESERVED_KEYWORDS = listOf(
    ECommerceParamNames.QUERY, ECommerceParamNames.PRICE, ECommerceParamNames.PRODUCT_ID,
    ECommerceParamNames.CATEGORY, ECommerceParamNames.CURRENCY, ECommerceParamNames.PRODUCTS,
    ECommerceParamNames.QUANTITY, ECommerceParamNames.TOTAL, ECommerceParamNames.REVENUE,
    ECommerceParamNames.ORDER_ID, ECommerceParamNames.SHARE_MESSAGE, CREATIVE,
    ECommerceParamNames.RATING
)

private const val FIRST_PURCHASE = "first_purchase"
private const val REMOVE_FROM_CART = "remove_from_cart"
private const val AF_ORDER_ID = "af_order_id"
private const val PRODUCT_ID = "product_id"
private const val CATEGORY = "category"
private const val QUANTITY = "quantity"

/**
 * Maps RudderStack event to AppsFlyer event name and properties
 */
internal fun mapEventToAppsFlyer(eventName: String, properties: JsonObject?): Pair<String, MutableMap<String, Any>> {
    val appsFlyerEventProps = mutableMapOf<String, Any>()
    mapPropertiesToAppsFlyer(eventName = eventName, properties = properties, appsFlyerEventProps = appsFlyerEventProps)

    val appsFlyerEventName = getAppsFlyerEventName(eventName)

    return Pair(appsFlyerEventName, appsFlyerEventProps)
}

/**
 * Determines the AppsFlyer event name for a given RudderStack event
 */
@Suppress("CyclomaticComplexMethod")
private fun getAppsFlyerEventName(eventName: String): String {
    return when (eventName) {
        ECommerceEvents.PRODUCTS_SEARCHED -> AFInAppEventType.SEARCH
        ECommerceEvents.PRODUCT_VIEWED -> AFInAppEventType.CONTENT_VIEW
        ECommerceEvents.PRODUCT_LIST_VIEWED -> AFInAppEventType.LIST_VIEW
        ECommerceEvents.PRODUCT_ADDED_TO_WISH_LIST -> AFInAppEventType.ADD_TO_WISH_LIST
        ECommerceEvents.PRODUCT_ADDED -> AFInAppEventType.ADD_TO_CART
        ECommerceEvents.CHECKOUT_STARTED -> AFInAppEventType.INITIATED_CHECKOUT
        ECommerceEvents.ORDER_COMPLETED -> AFInAppEventType.PURCHASE
        FIRST_PURCHASE -> FIRST_PURCHASE
        ECommerceEvents.PRODUCT_REMOVED -> REMOVE_FROM_CART
        ECommerceEvents.PROMOTION_VIEWED -> AFInAppEventType.AD_VIEW
        ECommerceEvents.PROMOTION_CLICKED -> AFInAppEventType.AD_CLICK
        ECommerceEvents.PAYMENT_INFO_ENTERED -> AFInAppEventType.ADD_PAYMENT_INFO
        ECommerceEvents.PRODUCT_SHARED, ECommerceEvents.CART_SHARED -> AFInAppEventType.SHARE
        ECommerceEvents.PRODUCT_REVIEWED -> AFInAppEventType.RATE
        else -> eventName.lowercase().replace(" ", "_")
    }
}

/**
 * Maps properties from RudderStack format to AppsFlyer format based on event type
 */
@Suppress("CyclomaticComplexMethod", "LongMethod")
private fun mapPropertiesToAppsFlyer(
    eventName: String,
    properties: JsonObject?,
    appsFlyerEventProps: MutableMap<String, Any>
) {
    when (eventName) {
        ECommerceEvents.PRODUCTS_SEARCHED ->
            properties?.getTypedValue(ECommerceParamNames.QUERY)?.let { query ->
                appsFlyerEventProps[AFInAppEventParameterName.SEARCH_STRING] = query
            }

        ECommerceEvents.PRODUCT_VIEWED -> mapProductEvent(properties, appsFlyerEventProps)
        ECommerceEvents.PRODUCT_LIST_VIEWED -> mapProductListViewedEvent(properties, appsFlyerEventProps)
        ECommerceEvents.PRODUCT_ADDED_TO_WISH_LIST -> mapProductEvent(properties, appsFlyerEventProps)

        ECommerceEvents.PRODUCT_ADDED -> {
            mapProductEvent(properties, appsFlyerEventProps)
            properties?.getTypedValue(ECommerceParamNames.QUANTITY)?.let { quantity ->
                appsFlyerEventProps[AFInAppEventParameterName.QUANTITY] = quantity
            }
        }

        ECommerceEvents.CHECKOUT_STARTED -> mapCheckoutEvent(properties, appsFlyerEventProps)
        ECommerceEvents.ORDER_COMPLETED -> mapOrderCompletedEvent(properties, appsFlyerEventProps)
        FIRST_PURCHASE -> mapOrderCompletedEvent(properties, appsFlyerEventProps)

        ECommerceEvents.PRODUCT_REMOVED -> {
            properties?.getTypedValue(ECommerceParamNames.PRODUCT_ID)?.let { productId ->
                appsFlyerEventProps[AFInAppEventParameterName.CONTENT_ID] = productId
            }
            properties?.getTypedValue(ECommerceParamNames.CATEGORY)?.let { category ->
                appsFlyerEventProps[AFInAppEventParameterName.CONTENT_TYPE] = category
            }
        }

        ECommerceEvents.PROMOTION_VIEWED -> mapPromotionEvent(properties, appsFlyerEventProps)
        ECommerceEvents.PROMOTION_CLICKED -> mapPromotionEvent(properties, appsFlyerEventProps)

        ECommerceEvents.PRODUCT_SHARED, ECommerceEvents.CART_SHARED ->
            properties?.getTypedValue(ECommerceParamNames.SHARE_MESSAGE)?.let { message ->
                appsFlyerEventProps[AFInAppEventParameterName.DESCRIPTION] = message
            }

        ECommerceEvents.PRODUCT_REVIEWED -> {
            properties?.getTypedValue(ECommerceParamNames.PRODUCT_ID)?.let { productId ->
                appsFlyerEventProps[AFInAppEventParameterName.CONTENT_ID] = productId
            }
            properties?.getTypedValue(ECommerceParamNames.RATING)?.let { rating ->
                appsFlyerEventProps[AFInAppEventParameterName.RATING_VALUE] = rating
            }
        }
        // Custom events don't have specific property mapping logic
        else -> {
            // NO-OP
        }
    }
    // handling custom properties
    mapCustomPropertiesToAppsFlyer(properties, appsFlyerEventProps)
}

/**
 * Maps basic product properties to AppsFlyer parameters
 */
private fun mapProductEvent(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>) {
    properties?.getTypedValue(ECommerceParamNames.PRICE)?.let { price ->
        appsFlyerEventProps[AFInAppEventParameterName.PRICE] = price
    }
    properties?.getTypedValue(ECommerceParamNames.PRODUCT_ID)?.let { productId ->
        appsFlyerEventProps[AFInAppEventParameterName.CONTENT_ID] = productId
    }
    properties?.getTypedValue(ECommerceParamNames.CATEGORY)?.let { category ->
        appsFlyerEventProps[AFInAppEventParameterName.CONTENT_TYPE] = category
    }
    properties?.getTypedValue(ECommerceParamNames.CURRENCY)?.let { currency ->
        appsFlyerEventProps[AFInAppEventParameterName.CURRENCY] = currency
    }
}

/**
 * Maps product list viewed event properties to AppsFlyer parameters
 */
private fun mapProductListViewedEvent(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>) {
    properties?.getTypedValue(ECommerceParamNames.CATEGORY)?.let { category ->
        appsFlyerEventProps[AFInAppEventParameterName.CONTENT_TYPE] = category
    }

    extractProductIds(properties, appsFlyerEventProps)
}

private fun extractProductIds(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>) {
    val products = properties?.getArray(ECommerceParamNames.PRODUCTS) ?: return

    val productIds = mutableListOf<Any>()
    products.forEach { product ->
        val productObj = product as? JsonObject ?: return@forEach
        productObj.getTypedValue(PRODUCT_ID)?.let { productId ->
            productIds.add(productId)
        }
    }

    if (productIds.isNotEmpty()) {
        appsFlyerEventProps[AFInAppEventParameterName.CONTENT_LIST] = productIds.toTypedArray()
    }
}

/**
 * Maps checkout event properties to AppsFlyer parameters
 */
private fun mapCheckoutEvent(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>) {
    properties?.getTypedValue(ECommerceParamNames.TOTAL)?.let { total ->
        appsFlyerEventProps[AFInAppEventParameterName.PRICE] = total
    }
    properties?.getTypedValue(ECommerceParamNames.CURRENCY)?.let { currency ->
        appsFlyerEventProps[AFInAppEventParameterName.CURRENCY] = currency
    }
    handleProducts(properties, appsFlyerEventProps)
}

/**
 * Maps order completed event properties to AppsFlyer parameters
 */
private fun mapOrderCompletedEvent(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>) {
    properties?.getTypedValue(ECommerceParamNames.TOTAL)?.let { total ->
        appsFlyerEventProps[AFInAppEventParameterName.PRICE] = total
    }
    properties?.getTypedValue(ECommerceParamNames.REVENUE)?.let { revenue ->
        appsFlyerEventProps[AFInAppEventParameterName.REVENUE] = revenue
    }
    properties?.getTypedValue(ECommerceParamNames.CURRENCY)?.let { currency ->
        appsFlyerEventProps[AFInAppEventParameterName.CURRENCY] = currency
    }
    properties?.getTypedValue(ECommerceParamNames.ORDER_ID)?.let { orderId ->
        appsFlyerEventProps[AFInAppEventParameterName.RECEIPT_ID] = orderId
        appsFlyerEventProps[AF_ORDER_ID] = orderId
    }
    handleProducts(properties, appsFlyerEventProps)
}

/**
 * Maps promotion event properties to AppsFlyer parameters
 */
private fun mapPromotionEvent(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>) {
    properties?.getTypedValue(CREATIVE)?.let { creative ->
        appsFlyerEventProps[AFInAppEventParameterName.AD_REVENUE_AD_TYPE] = creative
    }
    properties?.getTypedValue(ECommerceParamNames.CURRENCY)?.let { currency ->
        appsFlyerEventProps[AFInAppEventParameterName.CURRENCY] = currency
    }
}

/**
 * Handles product arrays and maps them to AppsFlyer parameters
 */
internal fun handleProducts(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>) {
    val products = properties?.getArray(ECommerceParamNames.PRODUCTS) ?: return

    val productIds = mutableListOf<Any>()
    val categories = mutableListOf<Any>()
    val quantities = mutableListOf<Any>()

    products.forEach { product ->
        val productObj = product as? JsonObject ?: return@forEach
        if (productObj.containsKey(PRODUCT_ID) && productObj.containsKey(CATEGORY) && productObj.containsKey(QUANTITY)) {
            productObj.getTypedValue(PRODUCT_ID)?.let { productIds.add(it) }
            productObj.getTypedValue(CATEGORY)?.let { categories.add(it) }
            productObj.getTypedValue(QUANTITY)?.let { quantities.add(it) }
        }
    }

    setProductArrays(appsFlyerEventProps, productIds, categories, quantities)
}

private fun setProductArrays(
    props: MutableMap<String, Any>,
    productIds: List<Any>,
    categories: List<Any>,
    quantities: List<Any>
) {
    if (productIds.isNotEmpty()) {
        props[AFInAppEventParameterName.CONTENT_ID] = productIds.toTypedArray()
    }
    if (categories.isNotEmpty()) {
        props[AFInAppEventParameterName.CONTENT_TYPE] = categories.toTypedArray()
    }
    if (quantities.isNotEmpty()) {
        props[AFInAppEventParameterName.QUANTITY] = quantities.toTypedArray()
    }
}

/**
 * Attaches all custom properties while filtering reserved keywords
 */
internal fun mapCustomPropertiesToAppsFlyer(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>) {
    properties?.let { props ->
        props.keys.forEach { key ->
            processCustomProperty(key, props[key], appsFlyerEventProps)
        }
    }
}

private fun processCustomProperty(key: String, value: JsonElement?, appsFlyerEventProps: MutableMap<String, Any>) {
    if (shouldIncludeProperty(key, value)) {
        val processedValue = extractValue(value)
        appsFlyerEventProps[key] = processedValue ?: String.empty()
    }
}

private fun shouldIncludeProperty(key: String, value: JsonElement?): Boolean {
    return key !in TRACK_RESERVED_KEYWORDS && key.isNotEmpty() && value != null
}

private fun JsonObject.toRawMap(): Map<String, Any?> {
    val result = mutableMapOf<String, Any?>()

    for ((key, jsonElement) in this) {
        result[key] = extractValue(jsonElement)
    }

    return result
}

/**
 * Extracts typed value from JsonElement while preserving data types for primitives.
 * Complex objects (JsonObject) are stringified for AppsFlyer compatibility.
 * Based on JsonInteropHelper.extractValue() for consistency.
 */
private fun extractValue(element: JsonElement?): Any? {
    return when (element) {
        JsonNull, null -> null

        is JsonPrimitive -> when {
            element.isString -> element.content
            element.booleanOrNull != null -> element.boolean
            element.intOrNull != null -> element.int
            element.longOrNull != null -> element.long
            element.doubleOrNull != null -> element.double
            else -> element.content
        }

        is JsonObject -> element.toRawMap()

        is JsonArray -> element.map { extractValue(it) }
    }
}

/**
 * Gets typed value from JsonObject property while preserving data types.
 */
private fun JsonObject?.getTypedValue(key: String): Any? {
    return this?.get(key)?.let { extractValue(it) }
}

/**
 * Convert JsonObject properties to MutableMap for AppsFlyer compatibility.
 * Preserves primitive data types but stringifies nested objects.
 */
internal fun JsonObject?.toMutableMap(): MutableMap<String, Any> {
    val map = mutableMapOf<String, Any>()
    this?.keys?.forEach { key ->
        val extractedValue = extractValue(this[key])
        if (extractedValue != null) {
            map[key] = extractedValue
        }
    }
    return map
}
