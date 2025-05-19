package com.rudderstack.integration.kotlin.firebase

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.StandardIntegration
import com.rudderstack.sdk.kotlin.android.utils.getArray
import com.rudderstack.sdk.kotlin.android.utils.getDouble
import com.rudderstack.sdk.kotlin.android.utils.getLong
import com.rudderstack.sdk.kotlin.android.utils.getString
import com.rudderstack.sdk.kotlin.android.utils.isDouble
import com.rudderstack.sdk.kotlin.android.utils.isKeyEmpty
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceEvents
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceParamNames
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

internal const val FIREBASE_KEY = "Firebase"

private const val USER_ID_KEY = "userId"
private const val PRODUCT_ID_KEY = "product_id"
private const val APPLICATION_OPENED = "Application Opened"
private const val CART_ID_KEY = "cart_id"
private const val PRODUCT = "product"
private const val CART = "cart"
private const val ORDER_ID_KEY = "order_id"
private const val CURRENCY_KEY = "currency"
private const val DEFAULT_CURRENCY = "USD"
private const val NAME_KEY = "name"
private const val REVENUE_KEY = "revenue"
private const val VALUE_KEY = "value"
private const val TOTAL_KEY = "total"
private const val SHIPPING_KEY = "shipping"
private const val TAX_KEY = "tax"

/**
 * Firebase Integration Plugin. See [IntegrationPlugin] for more info.
 */
@Suppress("TooManyFunctions")
class FirebaseIntegration : StandardIntegration, IntegrationPlugin() {

    private var firebaseAnalytics: FirebaseAnalytics? = null

    override val key: String
        get() = FIREBASE_KEY

    public override fun create(destinationConfig: JsonObject) {
        if (firebaseAnalytics == null) {
            firebaseAnalytics = provideFirebaseAnalyticsInstance()
        }
    }

    internal fun provideFirebaseAnalyticsInstance(): FirebaseAnalytics {
        return Firebase.analytics
    }

    override fun getDestinationInstance(): Any? {
        return firebaseAnalytics
    }

    override fun identify(payload: IdentifyEvent) {
        payload.userId
            .takeIf { it.isNotEmpty() }
            ?.let {
                firebaseAnalytics?.setUserId(it)
            }

        analytics.traits?.also { traits ->
            traits.keys
                .filterNot { it in IDENTIFY_RESERVED_KEYWORDS || it == USER_ID_KEY }
                .forEach { key ->
                    val firebaseCompatibleKey = formatFirebaseKey(key)
                    val value = getString(traits[key], maxLength = MAX_TRAITS_VALUE_LENGTH)
                    firebaseAnalytics?.setUserProperty(firebaseCompatibleKey, value)
                }
        }
    }

    override fun screen(payload: ScreenEvent) {
        if (payload.screenName.isNotEmpty()) {
            getBundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, payload.screenName)
                attachAllCustomProperties(this, payload.properties)
                firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, this)
            }
        }
    }

    override fun track(payload: TrackEvent) {
        val eventName = payload.event.takeIf { it.isNotEmpty() } ?: return

        when (eventName) {
            APPLICATION_OPENED -> handleApplicationOpenedEvent(payload.properties)
            in ECOMMERCE_EVENTS_MAPPING -> handleECommerceEvent(eventName, payload.properties)
            else -> handleCustomEvent(eventName, payload.properties)
        }
    }

    override fun reset() {
        firebaseAnalytics?.setUserId(null)
    }

    private fun handleApplicationOpenedEvent(properties: JsonObject?) {
        val firebaseEvent = FirebaseAnalytics.Event.APP_OPEN
        val params = getBundle()
        makeFirebaseEvent(firebaseEvent, params, properties)
    }

    private fun handleECommerceEvent(eventName: String, properties: JsonObject?) {
        val firebaseEvent = ECOMMERCE_EVENTS_MAPPING[eventName] ?: return

        val params = getBundle()
        if (firebaseEvent.isNotEmpty() && !properties.isNullOrEmpty()) {
            when (firebaseEvent) {
                FirebaseAnalytics.Event.SHARE -> {
                    params.putIfNotEmpty(FirebaseAnalytics.Param.ITEM_ID, properties, CART_ID_KEY, PRODUCT_ID_KEY)
                }

                FirebaseAnalytics.Event.VIEW_PROMOTION, FirebaseAnalytics.Event.SELECT_PROMOTION -> {
                    params.putIfNotEmpty(FirebaseAnalytics.Param.PROMOTION_NAME, properties, NAME_KEY)
                }

                FirebaseAnalytics.Event.SELECT_CONTENT -> {
                    params.putIfNotEmpty(FirebaseAnalytics.Param.ITEM_ID, properties, PRODUCT_ID_KEY)
                    params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, PRODUCT)
                }
            }

            addConstantParamsForECommerceEvent(params, eventName)
            handleECommerceEventProperties(params, properties, firebaseEvent)
        }

        makeFirebaseEvent(firebaseEvent, params, properties)
    }

    private fun handleCustomEvent(eventName: String, properties: JsonObject?) {
        val params = getBundle()
        val firebaseEvent: String = formatFirebaseKey(eventName)
        makeFirebaseEvent(firebaseEvent, params, properties)
    }

    private fun addConstantParamsForECommerceEvent(params: Bundle, eventName: String) {
        if (eventName == ECommerceEvents.PRODUCT_SHARED) {
            params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, PRODUCT)
        } else if (eventName == ECommerceEvents.CART_SHARED) {
            params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, CART)
        }
    }

    private fun handleECommerceEventProperties(params: Bundle, properties: JsonObject, firebaseEvent: String) {
        putDoubleIfValid(params, FirebaseAnalytics.Param.VALUE, properties, listOf(REVENUE_KEY, VALUE_KEY, TOTAL_KEY))
        putDoubleIfValid(params, FirebaseAnalytics.Param.SHIPPING, properties, listOf(SHIPPING_KEY))
        putDoubleIfValid(params, FirebaseAnalytics.Param.TAX, properties, listOf(TAX_KEY))

        if (EVENT_WITH_PRODUCTS_ARRAY.contains(firebaseEvent) && !properties.isKeyEmpty(ECommerceParamNames.PRODUCTS)) {
            handleProductsArray(params, properties)
        }
        if (EVENT_WITH_SINGLE_PRODUCT.contains(firebaseEvent)) {
            handleSingleProduct(params, properties)
        }

        properties.keys.forEach { key ->
            ECOMMERCE_PROPERTY_MAPPING[key]?.takeIf { !properties.isKeyEmpty(key) }?.let {
                params.putString(it, properties.getString(key))
            }
        }

        params.putString(
            FirebaseAnalytics.Param.CURRENCY,
            properties.getString(CURRENCY_KEY)?.takeIf { !properties.isKeyEmpty(CURRENCY_KEY) } ?: DEFAULT_CURRENCY
        )

        properties.getString(ORDER_ID_KEY)?.takeIf { !properties.isKeyEmpty(ORDER_ID_KEY) }?.let {
            params.putString(FirebaseAnalytics.Param.TRANSACTION_ID, it)
            params.putString(ORDER_ID_KEY, it) // For backward compatibility
        }
    }

    private fun handleProductsArray(params: Bundle, properties: JsonObject) {
        val products = properties.getArray(ECommerceParamNames.PRODUCTS).orEmpty()
        val mappedProducts = products
            .mapNotNull {
                it.asJsonObjectOrNull()?.let(::mapProductProperties)
            }
            .filterNot(Bundle::isEmpty)

        if (mappedProducts.isNotEmpty()) {
            params.putParcelableArrayList(FirebaseAnalytics.Param.ITEMS, ArrayList(mappedProducts))
        }
    }

    private fun handleSingleProduct(params: Bundle, properties: JsonObject) {
        val productBundle = mapProductProperties(properties)
        if (!productBundle.isEmpty) {
            params.putParcelableArray(FirebaseAnalytics.Param.ITEMS, arrayOf(productBundle))
        }
    }

    private fun mapProductProperties(properties: JsonObject): Bundle {
        val bundle = getBundle()
        PRODUCT_PROPERTIES_MAPPING.forEach { (key, firebaseKey) ->
            properties[key]?.takeIf { !properties.isKeyEmpty(key) }?.let {
                putProductValue(bundle, firebaseKey, it)
            }
        }
        return bundle
    }

    private fun putDoubleIfValid(params: Bundle, key: String, properties: JsonObject, keysToCheck: List<String>) {
        keysToCheck.firstOrNull { !properties.isKeyEmpty(it) && properties.isDouble(it) }?.let {
            params.putDouble(key, properties.getDouble(it) ?: 0.0)
        }
    }

    private fun putProductValue(params: Bundle, firebaseKey: String, value: JsonElement?) {
        when (firebaseKey) {
            FirebaseAnalytics.Param.ITEM_ID,
            FirebaseAnalytics.Param.ITEM_NAME,
            FirebaseAnalytics.Param.ITEM_CATEGORY -> params.putString(firebaseKey, value?.getString())

            FirebaseAnalytics.Param.QUANTITY -> params.putLong(firebaseKey, value?.getLong() ?: 0)

            FirebaseAnalytics.Param.PRICE -> params.putDouble(firebaseKey, value?.getDouble() ?: 0.0)

            else -> LoggerAnalytics.debug("FirebaseIntegration: Product value is not of expected type")
        }
    }

    private fun makeFirebaseEvent(firebaseEvent: String, params: Bundle, properties: JsonObject?) {
        attachAllCustomProperties(params, properties)
        LoggerAnalytics.debug("FirebaseIntegration: Logged \"$firebaseEvent\" to Firebase")
        firebaseAnalytics?.logEvent(firebaseEvent, params)
    }
}

private fun Bundle.putIfNotEmpty(key: String, properties: JsonObject, vararg possibleKeys: String) {
    possibleKeys.firstOrNull { !properties.isKeyEmpty(it) }?.let {
        this.putString(key, properties.getString(it))
    }
}

private fun Any.asJsonObjectOrNull(): JsonObject? = this as? JsonObject
