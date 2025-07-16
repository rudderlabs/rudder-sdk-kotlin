@file:Suppress("TooManyFunctions")

package com.rudderstack.integration.kotlin.appsflyer

import com.appsflyer.AFInAppEventParameterName
import com.appsflyer.AFInAppEventType
import com.rudderstack.sdk.kotlin.android.utils.getArray
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceEvents
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceParamNames
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

// Reserved keywords for filtering custom properties - equivalent to Java TRACK_RESERVED_KEYWORDS
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
    val appsFlyerEventName = getAppsFlyerEventName(eventName, properties, appsFlyerEventProps)
    return Pair(appsFlyerEventName, appsFlyerEventProps)
}

@Suppress("CyclomaticComplexMethod")
private fun getAppsFlyerEventName(
    eventName: String,
    properties: JsonObject?,
    appsFlyerEventProps: MutableMap<String, Any>
): String {
    return when (eventName) {
        ECommerceEvents.PRODUCTS_SEARCHED -> handleProductsSearched(properties, appsFlyerEventProps)
        ECommerceEvents.PRODUCT_VIEWED -> handleProductViewed(properties, appsFlyerEventProps)
        ECommerceEvents.PRODUCT_LIST_VIEWED -> handleProductListViewed(properties, appsFlyerEventProps)
        ECommerceEvents.PRODUCT_ADDED_TO_WISH_LIST -> handleProductWishList(properties, appsFlyerEventProps)
        ECommerceEvents.PRODUCT_ADDED -> handleProductAdded(properties, appsFlyerEventProps)
        ECommerceEvents.CHECKOUT_STARTED -> handleCheckoutStarted(properties, appsFlyerEventProps)
        ECommerceEvents.ORDER_COMPLETED -> handleOrderCompleted(properties, appsFlyerEventProps)
        FIRST_PURCHASE -> handleFirstPurchase(properties, appsFlyerEventProps)
        ECommerceEvents.PRODUCT_REMOVED -> handleProductRemoved(properties, appsFlyerEventProps)
        ECommerceEvents.PROMOTION_VIEWED -> handlePromotionViewed(properties, appsFlyerEventProps)
        ECommerceEvents.PROMOTION_CLICKED -> handlePromotionClicked(properties, appsFlyerEventProps)
        ECommerceEvents.PAYMENT_INFO_ENTERED -> AFInAppEventType.ADD_PAYMENT_INFO
        ECommerceEvents.PRODUCT_SHARED, ECommerceEvents.CART_SHARED -> handleShared(properties, appsFlyerEventProps)
        ECommerceEvents.PRODUCT_REVIEWED -> handleProductReviewed(properties, appsFlyerEventProps)
        else -> eventName.lowercase().replace(" ", "_")
    }
}

private fun handleProductsSearched(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>): String {
    properties?.getTypedValue(ECommerceParamNames.QUERY)?.let { query ->
        appsFlyerEventProps[AFInAppEventParameterName.SEARCH_STRING] = query
    }
    return AFInAppEventType.SEARCH
}

private fun handleProductViewed(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>): String {
    mapProductEvent(properties, appsFlyerEventProps)
    return AFInAppEventType.CONTENT_VIEW
}

private fun handleProductListViewed(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>): String {
    mapProductListViewedEvent(properties, appsFlyerEventProps)
    return AFInAppEventType.LIST_VIEW
}

private fun handleProductWishList(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>): String {
    mapProductEvent(properties, appsFlyerEventProps)
    return AFInAppEventType.ADD_TO_WISH_LIST
}

private fun handleProductAdded(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>): String {
    mapProductEvent(properties, appsFlyerEventProps)
    properties?.getTypedValue(ECommerceParamNames.QUANTITY)?.let { quantity ->
        appsFlyerEventProps[AFInAppEventParameterName.QUANTITY] = quantity
    }
    return AFInAppEventType.ADD_TO_CART
}

private fun handleCheckoutStarted(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>): String {
    mapCheckoutEvent(properties, appsFlyerEventProps)
    return AFInAppEventType.INITIATED_CHECKOUT
}

private fun handleOrderCompleted(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>): String {
    mapOrderCompletedEvent(properties, appsFlyerEventProps)
    return AFInAppEventType.PURCHASE
}

private fun handleFirstPurchase(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>): String {
    mapOrderCompletedEvent(properties, appsFlyerEventProps)
    return FIRST_PURCHASE
}

private fun handleProductRemoved(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>): String {
    properties?.getTypedValue(ECommerceParamNames.PRODUCT_ID)?.let { productId ->
        appsFlyerEventProps[AFInAppEventParameterName.CONTENT_ID] = productId
    }
    properties?.getTypedValue(ECommerceParamNames.CATEGORY)?.let { category ->
        appsFlyerEventProps[AFInAppEventParameterName.CONTENT_TYPE] = category
    }
    return REMOVE_FROM_CART
}

private fun handlePromotionViewed(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>): String {
    mapPromotionEvent(properties, appsFlyerEventProps)
    return AFInAppEventType.AD_VIEW
}

private fun handlePromotionClicked(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>): String {
    mapPromotionEvent(properties, appsFlyerEventProps)
    return AFInAppEventType.AD_CLICK
}

private fun handleShared(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>): String {
    properties?.getTypedValue(ECommerceParamNames.SHARE_MESSAGE)?.let { message ->
        appsFlyerEventProps[AFInAppEventParameterName.DESCRIPTION] = message
    }
    return AFInAppEventType.SHARE
}

private fun handleProductReviewed(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>): String {
    properties?.getTypedValue(ECommerceParamNames.PRODUCT_ID)?.let { productId ->
        appsFlyerEventProps[AFInAppEventParameterName.CONTENT_ID] = productId
    }
    properties?.getTypedValue(ECommerceParamNames.RATING)?.let { rating ->
        appsFlyerEventProps[AFInAppEventParameterName.RATING_VALUE] = rating
    }
    return AFInAppEventType.RATE
}

/**
 * Maps basic product properties to AppsFlyer parameters
 */
internal fun mapProductEvent(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>) {
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
internal fun mapProductListViewedEvent(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>) {
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
internal fun mapCheckoutEvent(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>) {
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
internal fun mapOrderCompletedEvent(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>) {
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
internal fun mapPromotionEvent(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>) {
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
internal fun attachAllCustomProperties(appsFlyerEventProps: MutableMap<String, Any>, properties: JsonObject?) {
    properties?.let { props ->
        props.keys.forEach { key ->
            processCustomProperty(key, props[key], appsFlyerEventProps)
        }
    }
}

private fun processCustomProperty(key: String, value: JsonElement?, appsFlyerEventProps: MutableMap<String, Any>) {
    if (shouldIncludeProperty(key, value)) {
        val processedValue = extractValue(value)
        appsFlyerEventProps[key] = processedValue ?: ""
    }
}

private fun shouldIncludeProperty(key: String, value: Any?): Boolean {
    return key !in TRACK_RESERVED_KEYWORDS && key.isNotEmpty() && value != null
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
            element.isString -> {
                // Return the string content as-is for proper quote handling
                element.content
            }
            element.booleanOrNull != null -> element.boolean
            element.intOrNull != null -> element.int
            element.longOrNull != null -> element.long
            element.doubleOrNull != null -> element.double
            else -> element.content
        }

        is JsonObject -> element.toString() // Stringify nested objects

        is JsonArray -> element.map { extractValue(it) } // Convert to list for general use
    }
}

/**
 * Gets typed value from JsonObject property while preserving data types.
 * This replaces getString() calls for type preservation.
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
