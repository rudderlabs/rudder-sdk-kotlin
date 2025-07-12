@file:Suppress("TooManyFunctions")

package com.rudderstack.integration.kotlin.appsflyer

import com.appsflyer.AFInAppEventParameterName
import com.appsflyer.AFInAppEventType
import com.rudderstack.sdk.kotlin.android.utils.getArray
import com.rudderstack.sdk.kotlin.android.utils.getString
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceEvents
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceParamNames
import kotlinx.serialization.json.JsonObject

// Reserved keywords for filtering custom properties - equivalent to Java TRACK_RESERVED_KEYWORDS
internal val TRACK_RESERVED_KEYWORDS = listOf(
    ECommerceParamNames.QUERY, ECommerceParamNames.PRICE, ECommerceParamNames.PRODUCT_ID,
    ECommerceParamNames.CATEGORY, ECommerceParamNames.CURRENCY, ECommerceParamNames.PRODUCTS,
    ECommerceParamNames.QUANTITY, ECommerceParamNames.TOTAL, ECommerceParamNames.REVENUE,
    ECommerceParamNames.ORDER_ID, ECommerceParamNames.SHARE_MESSAGE, CREATIVE,
    ECommerceParamNames.RATING
)

private const val FIRST_PURCHASE = "first_purchase"

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
    properties?.getString(ECommerceParamNames.QUERY)?.let { query ->
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
    properties?.getString(ECommerceParamNames.QUANTITY)?.let { quantity ->
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
    properties?.getString(ECommerceParamNames.PRODUCT_ID)?.let { productId ->
        appsFlyerEventProps[AFInAppEventParameterName.CONTENT_ID] = productId
    }
    properties?.getString(ECommerceParamNames.CATEGORY)?.let { category ->
        appsFlyerEventProps[AFInAppEventParameterName.CONTENT_TYPE] = category
    }
    return "remove_from_cart"
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
    properties?.getString(ECommerceParamNames.SHARE_MESSAGE)?.let { message ->
        appsFlyerEventProps[AFInAppEventParameterName.DESCRIPTION] = message
    }
    return AFInAppEventType.SHARE
}

private fun handleProductReviewed(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>): String {
    properties?.getString(ECommerceParamNames.PRODUCT_ID)?.let { productId ->
        appsFlyerEventProps[AFInAppEventParameterName.CONTENT_ID] = productId
    }
    properties?.getString(ECommerceParamNames.RATING)?.let { rating ->
        appsFlyerEventProps[AFInAppEventParameterName.RATING_VALUE] = rating
    }
    return AFInAppEventType.RATE
}

/**
 * Maps basic product properties to AppsFlyer parameters
 */
internal fun mapProductEvent(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>) {
    properties?.getString(ECommerceParamNames.PRICE)?.let { price ->
        appsFlyerEventProps[AFInAppEventParameterName.PRICE] = price
    }
    properties?.getString(ECommerceParamNames.PRODUCT_ID)?.let { productId ->
        appsFlyerEventProps[AFInAppEventParameterName.CONTENT_ID] = productId
    }
    properties?.getString(ECommerceParamNames.CATEGORY)?.let { category ->
        appsFlyerEventProps[AFInAppEventParameterName.CONTENT_TYPE] = category
    }
    properties?.getString(ECommerceParamNames.CURRENCY)?.let { currency ->
        appsFlyerEventProps[AFInAppEventParameterName.CURRENCY] = currency
    }
}

/**
 * Maps product list viewed event properties to AppsFlyer parameters
 */
internal fun mapProductListViewedEvent(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>) {
    properties?.getString(ECommerceParamNames.CATEGORY)?.let { category ->
        appsFlyerEventProps[AFInAppEventParameterName.CONTENT_TYPE] = category
    }

    extractProductIds(properties, appsFlyerEventProps)
}

private fun extractProductIds(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>) {
    val products = properties?.getArray(ECommerceParamNames.PRODUCTS) ?: return

    val productIds = mutableListOf<String>()
    products.forEach { product ->
        val productObj = product as? JsonObject ?: return@forEach
        productObj.getString("product_id")?.let { productId ->
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
    properties?.getString(ECommerceParamNames.TOTAL)?.let { total ->
        appsFlyerEventProps[AFInAppEventParameterName.PRICE] = total
    }
    properties?.getString(ECommerceParamNames.CURRENCY)?.let { currency ->
        appsFlyerEventProps[AFInAppEventParameterName.CURRENCY] = currency
    }
    handleProducts(properties, appsFlyerEventProps)
}

/**
 * Maps order completed event properties to AppsFlyer parameters
 */
internal fun mapOrderCompletedEvent(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>) {
    properties?.getString(ECommerceParamNames.TOTAL)?.let { total ->
        appsFlyerEventProps[AFInAppEventParameterName.PRICE] = total
    }
    properties?.getString(ECommerceParamNames.REVENUE)?.let { revenue ->
        appsFlyerEventProps[AFInAppEventParameterName.REVENUE] = revenue
    }
    properties?.getString(ECommerceParamNames.CURRENCY)?.let { currency ->
        appsFlyerEventProps[AFInAppEventParameterName.CURRENCY] = currency
    }
    properties?.getString(ECommerceParamNames.ORDER_ID)?.let { orderId ->
        appsFlyerEventProps[AFInAppEventParameterName.RECEIPT_ID] = orderId
        appsFlyerEventProps["af_order_id"] = orderId
    }
    handleProducts(properties, appsFlyerEventProps)
}

/**
 * Maps promotion event properties to AppsFlyer parameters
 */
internal fun mapPromotionEvent(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>) {
    properties?.getString(CREATIVE)?.let { creative ->
        appsFlyerEventProps[AFInAppEventParameterName.AD_REVENUE_AD_TYPE] = creative
    }
    properties?.getString(ECommerceParamNames.CURRENCY)?.let { currency ->
        appsFlyerEventProps[AFInAppEventParameterName.CURRENCY] = currency
    }
}

/**
 * Handles product arrays and maps them to AppsFlyer parameters
 */
internal fun handleProducts(properties: JsonObject?, appsFlyerEventProps: MutableMap<String, Any>) {
    val products = properties?.getArray(ECommerceParamNames.PRODUCTS) ?: return

    val productIds = mutableListOf<String>()
    val categories = mutableListOf<String>()
    val quantities = mutableListOf<String>()

    products.forEach { product ->
        val productObj = product as? JsonObject ?: return@forEach
        productObj.getString("product_id")?.let { productIds.add(it) }
        productObj.getString("category")?.let { categories.add(it) }
        productObj.getString("quantity")?.let { quantities.add(it) }
    }

    setProductArrays(appsFlyerEventProps, productIds, categories, quantities)
}

private fun setProductArrays(
    props: MutableMap<String, Any>,
    productIds: List<String>,
    categories: List<String>,
    quantities: List<String>
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

private fun processCustomProperty(key: String, value: Any?, appsFlyerEventProps: MutableMap<String, Any>) {
    if (shouldIncludeProperty(key, value)) {
        val processedValue = processPropertyValue(value)
        appsFlyerEventProps[key] = processedValue
    }
}

private fun shouldIncludeProperty(key: String, value: Any?): Boolean {
    return key !in TRACK_RESERVED_KEYWORDS && key.isNotEmpty() && value != null
}

private fun processPropertyValue(value: Any?): String {
    val stringValue = value.toString()
    return if (stringValue.startsWith("\"") && stringValue.endsWith("\"")) {
        stringValue.removeSurrounding("\"")
    } else {
        stringValue
    }
}

/**
 * Convert JsonObject properties to MutableMap for AppsFlyer compatibility
 */
internal fun JsonObject?.toMutableMap(): MutableMap<String, Any> {
    val map = mutableMapOf<String, Any>()
    this?.keys?.forEach { key ->
        val value = this[key]
        if (value != null) {
            // Convert JsonElement to appropriate type
            map[key] = when {
                value.toString().startsWith("\"") && value.toString().endsWith("\"") -> {
                    value.toString().removeSurrounding("\"")
                }
                else -> value.toString()
            }
        }
    }
    return map
}
