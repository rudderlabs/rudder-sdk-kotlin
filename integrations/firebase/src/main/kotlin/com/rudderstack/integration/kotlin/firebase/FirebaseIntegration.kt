package com.rudderstack.integration.kotlin.firebase

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.android.utils.getArray
import com.rudderstack.sdk.kotlin.android.utils.getDouble
import com.rudderstack.sdk.kotlin.android.utils.getLong
import com.rudderstack.sdk.kotlin.android.utils.getString
import com.rudderstack.sdk.kotlin.android.utils.isDouble
import com.rudderstack.sdk.kotlin.android.utils.isKeyEmpty
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceEvents
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceParamNames
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

internal const val FIREBASE_KEY = "Firebase"

/**
 * Firebase Integration Plugin. See [IntegrationPlugin] for more info.
 */
@Suppress("TooManyFunctions")
class FirebaseIntegration : IntegrationPlugin() {

    private var firebaseAnalytics: FirebaseAnalytics? = null

    override val key: String
        get() = FIREBASE_KEY

    override fun create(destinationConfig: JsonObject) {
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

    override fun identify(payload: IdentifyEvent): Event? {
        firebaseAnalytics?.takeIf { payload.userId.isNotEmpty() }?.setUserId(payload.userId)

        analytics.traits?.keys
            ?.map(::getTrimmedKey)
            ?.filterNot { it in IDENTIFY_RESERVED_KEYWORDS || it == "userId" }
            ?.forEach { key ->
                firebaseAnalytics?.setUserProperty(key, analytics.traits?.getString(key))
            }

        return payload
    }

    override fun screen(payload: ScreenEvent): Event? {
        if (payload.screenName.isNotEmpty()) {
            Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, payload.screenName)
                attachAllCustomProperties(this, payload.properties)
                firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, this)
            }
        }
        return payload
    }

    override fun track(payload: TrackEvent): Event? {
        val eventName = payload.event.takeIf { it.isNotEmpty() } ?: return payload

        when (eventName) {
            "Application Opened" -> handleApplicationOpenedEvent(payload.properties)
            in ECOMMERCE_EVENTS_MAPPING -> handleECommerceEvent(eventName, payload.properties)
            else -> handleCustomEvent(eventName, payload.properties)
        }

        return payload
    }

    override fun reset() {
        firebaseAnalytics?.setUserId(null)
    }

    private fun handleApplicationOpenedEvent(properties: JsonObject?) {
        val firebaseEvent = FirebaseAnalytics.Event.APP_OPEN
        val params = Bundle()
        makeFirebaseEvent(firebaseEvent, params, properties)
    }

    private fun handleECommerceEvent(eventName: String, properties: JsonObject?) {
        val firebaseEvent = ECOMMERCE_EVENTS_MAPPING[eventName] ?: return

        val params = Bundle()
        if (firebaseEvent.isNotEmpty() && !properties.isNullOrEmpty()) {
            when (firebaseEvent) {
                FirebaseAnalytics.Event.SHARE -> {
                    params.putIfNotEmpty(FirebaseAnalytics.Param.ITEM_ID, properties, "cart_id", "product_id")
                }

                FirebaseAnalytics.Event.VIEW_PROMOTION, FirebaseAnalytics.Event.SELECT_PROMOTION -> {
                    params.putIfNotEmpty(FirebaseAnalytics.Param.PROMOTION_NAME, properties, "name")
                }

                FirebaseAnalytics.Event.SELECT_CONTENT -> {
                    params.putIfNotEmpty(FirebaseAnalytics.Param.ITEM_ID, properties, "product_id")
                    params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "product")
                }
            }

            addConstantParamsForECommerceEvent(params, eventName)
            handleECommerceEventProperties(params, properties, firebaseEvent)
        }

        makeFirebaseEvent(firebaseEvent, params, properties)
    }

    private fun handleCustomEvent(eventName: String, properties: JsonObject?) {
        val params = Bundle()
        val firebaseEvent: String = getTrimmedKey(eventName)
        makeFirebaseEvent(firebaseEvent, params, properties)
    }

    private fun addConstantParamsForECommerceEvent(params: Bundle, eventName: String) {
        if (eventName == ECommerceEvents.PRODUCT_SHARED) {
            params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "product")
        } else if (eventName == ECommerceEvents.CART_SHARED) {
            params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "cart")
        }
    }

    private fun handleECommerceEventProperties(params: Bundle, properties: JsonObject, firebaseEvent: String) {
        putDoubleIfValid(params, FirebaseAnalytics.Param.VALUE, properties, listOf("revenue", "value", "total"))
        putDoubleIfValid(params, FirebaseAnalytics.Param.SHIPPING, properties, listOf("shipping"))
        putDoubleIfValid(params, FirebaseAnalytics.Param.TAX, properties, listOf("tax"))

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
            properties.getString("currency")?.takeIf { !properties.isKeyEmpty("currency") } ?: "USD"
        )

        properties.getString("order_id")?.takeIf { !properties.isKeyEmpty("order_id") }?.let {
            params.putString(FirebaseAnalytics.Param.TRANSACTION_ID, it)
            params.putString("order_id", it) // For backward compatibility
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
        val bundle = Bundle()
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
        LoggerAnalytics.debug("FirebaseIntegration: Logged \"$firebaseEvent\" to Firebase and properties: $properties")
        firebaseAnalytics?.logEvent(firebaseEvent, params)
    }
}

private fun Bundle.putIfNotEmpty(key: String, properties: JsonObject, vararg possibleKeys: String) {
    possibleKeys.firstOrNull { !properties.isKeyEmpty(it) }?.let {
        this.putString(key, properties.getString(it))
    }
}

private fun Any.asJsonObjectOrNull(): JsonObject? = this as? JsonObject
