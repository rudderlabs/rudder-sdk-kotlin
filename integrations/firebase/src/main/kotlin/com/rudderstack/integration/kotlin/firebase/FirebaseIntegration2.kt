package com.rudderstack.integration.kotlin.firebase

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.IntegrationPlugin
import com.rudderstack.sdk.kotlin.android.utils.getDouble
import com.rudderstack.sdk.kotlin.android.utils.getString
import com.rudderstack.sdk.kotlin.android.utils.isDouble
import com.rudderstack.sdk.kotlin.android.utils.isKeyEmpty
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceParamNames
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

// internal const val FIREBASE_KEY = "Firebase"

class FirebaseIntegration2 : IntegrationPlugin() {

    private var firebaseAnalytics: FirebaseAnalytics? = null

    override val key: String get() = FIREBASE_KEY

    override fun create(destinationConfig: JsonObject) {
        if (firebaseAnalytics == null) {
            firebaseAnalytics = provideFirebaseAnalyticsInstance()
        }
    }

    internal fun provideFirebaseAnalyticsInstance(): FirebaseAnalytics {
        return Firebase.analytics
    }

    override fun getDestinationInstance(): Any? = firebaseAnalytics

    override fun identify(payload: IdentifyEvent): Event? {
        firebaseAnalytics?.setUserId(payload.userId.takeIf { it.isNotEmpty() })
        analytics.traits?.let { setUserProperties(it) }
        return payload
    }

    override fun screen(payload: ScreenEvent): Event? {
        if (payload.screenName.isEmpty()) return payload
        logFirebaseEvent(FirebaseAnalytics.Event.SCREEN_VIEW, payload.screenName, payload.properties)
        return payload
    }

    override fun track(payload: TrackEvent): Event? {
        if (payload.event.isEmpty()) return payload
        when (payload.event) {
            "Application Opened" -> handleApplicationOpenedEvent(payload.properties)
            in ECOMMERCE_EVENTS_MAPPING -> handleECommerceEvent(payload.event, payload.properties)
            else -> handleCustomEvent(payload.event, payload.properties)
        }
        return payload
    }

    override fun reset() {
        firebaseAnalytics?.setUserId(null)
    }

    private fun setUserProperties(traits: JsonObject) {
        traits.keys.forEach { key ->
            val firebaseKey = getTrimmedKey(key)
            if (firebaseKey !in IDENTIFY_RESERVED_KEYWORDS && firebaseKey != "userId") {
                firebaseAnalytics?.setUserProperty(firebaseKey, traits[firebaseKey]?.toString())
            }
        }
    }

    private fun logFirebaseEvent(event: String, screenName: String, properties: JsonObject?, params: Bundle = Bundle()) {
        val finalParams = params.apply {
            if (screenName.isNotEmpty()) {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            }
            attachAllCustomProperties(this, properties)
        }
        firebaseAnalytics?.logEvent(event, finalParams)
    }

    private fun handleApplicationOpenedEvent(properties: JsonObject?) {
        logFirebaseEvent(FirebaseAnalytics.Event.APP_OPEN, String.empty(), properties)
    }

    private fun handleECommerceEvent(eventName: String, properties: JsonObject?) {
        val firebaseEvent = ECOMMERCE_EVENTS_MAPPING[eventName] ?: return
        val params = Bundle().apply {
            handleECommerceParams(this, eventName, properties)
        }
        logFirebaseEvent(firebaseEvent, String.empty(), properties, params)
    }

    private fun handleCustomEvent(eventName: String, properties: JsonObject?) {
        logFirebaseEvent(getTrimmedKey(eventName), String.empty(), properties)
    }

    private fun handleECommerceParams(params: Bundle, eventName: String, properties: JsonObject?) {
        properties?.let {
            addRevenueAndCurrency(params, it)
            addMappedECommerceProperties(params, it)
            addECommerceProducts(params, it, eventName)
        }
    }

    private fun addRevenueAndCurrency(params: Bundle, properties: JsonObject) {
        listOf("revenue", "value", "total").forEach { key ->
            if (!properties.isKeyEmpty(key) && properties.isDouble(key)) {
                params.putDouble(FirebaseAnalytics.Param.VALUE, properties.getDouble(key) ?: 0.0)
                return
            }
        }
        params.putString(FirebaseAnalytics.Param.CURRENCY, properties.getString("currency") ?: "USD")
    }

    private fun addMappedECommerceProperties(params: Bundle, properties: JsonObject) {
        ECOMMERCE_PROPERTY_MAPPING.forEach { (key, firebaseKey) ->
            if (!properties.isKeyEmpty(key)) {
                params.putString(firebaseKey, properties.getString(key))
            }
        }
    }

    private fun addECommerceProducts(params: Bundle, properties: JsonObject, eventName: String) {
        when {
            EVENT_WITH_PRODUCTS_ARRAY.contains(eventName) -> handleProductsArray(params, properties)
            EVENT_WITH_PRODUCTS_AT_ROOT.contains(eventName) -> handleSingleProduct(params, properties)
        }
    }

    private fun handleProductsArray(params: Bundle, properties: JsonObject) {
        (properties[ECommerceParamNames.PRODUCTS] as? JsonArray)?.let { products ->
            val mappedProducts = products.mapNotNull { product ->
                (product as? JsonObject)?.let { createProductBundle(it) }
            }
            if (mappedProducts.isNotEmpty()) {
                params.putParcelableArrayList(FirebaseAnalytics.Param.ITEMS, ArrayList(mappedProducts))
            }
        }
    }

    private fun handleSingleProduct(params: Bundle, properties: JsonObject) {
        createProductBundle(properties)?.let {
            params.putParcelableArray(FirebaseAnalytics.Param.ITEMS, arrayOf(it))
        }
    }

    private fun createProductBundle(product: JsonObject): Bundle? {
        return Bundle().apply {
            PRODUCT_PROPERTIES_MAPPING.forEach { (key, firebaseKey) ->
                if (!product.isKeyEmpty(key)) {
                    putProductValue(this, firebaseKey, product[key])
                }
            }
        }
    }

    private fun putProductValue(params: Bundle, firebaseKey: String, value: Any?) {
        when (firebaseKey) {
            FirebaseAnalytics.Param.ITEM_ID, FirebaseAnalytics.Param.ITEM_NAME, FirebaseAnalytics.Param.ITEM_CATEGORY ->
                params.putString(firebaseKey, value.toString())

            FirebaseAnalytics.Param.QUANTITY ->
                (value as? Number)?.let { params.putLong(firebaseKey, it.toLong()) }

            FirebaseAnalytics.Param.PRICE ->
                (value as? Number)?.let { params.putDouble(firebaseKey, it.toDouble()) }

            else -> LoggerAnalytics.debug("Product value is not of expected type")
        }
    }
}
