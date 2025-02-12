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
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceEvents
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceParamNames
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import org.json.JSONException

internal const val FIREBASE_KEY = "Firebase"

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
        if (payload.userId.isNotEmpty()) {
            firebaseAnalytics?.setUserId(payload.userId)
        }

        val traits = analytics.traits

        traits?.keys?.forEach { key ->
            val firebaseKey = getTrimmedKey(key)
            if (!IDENTIFY_RESERVED_KEYWORDS.contains(firebaseKey) && firebaseKey != "userId") {
                firebaseAnalytics?.setUserProperty(key, traits.getString(key))
            }
        }

        return payload
    }

    override fun screen(payload: ScreenEvent): Event? {
        val screenName = payload.screenName

        if (screenName.isEmpty()) {
            return payload
        }
        val params = Bundle()
        params.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        attachAllCustomProperties(params, payload.properties)

        firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params)
        return payload
    }

    override fun track(payload: TrackEvent): Event? {
        val eventName = payload.event

        if (eventName.isEmpty()) {
            return payload
        }

        if (eventName == "Application Opened") {
            handleApplicationOpenedEvent(payload.properties)
        } else if (ECOMMERCE_EVENTS_MAPPING.contains(eventName)) {
            // handle e-commerce events
            handleECommerceEvent(eventName, payload.properties)
        } else {
            // handle custom events
            handleCustomEvent(eventName, payload.properties)
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
        val params = Bundle()
        val firebaseEvent: String = ECOMMERCE_EVENTS_MAPPING[eventName] ?: return
        if (firebaseEvent.isNotEmpty() && !properties.isNullOrEmpty()) {
            if (firebaseEvent == FirebaseAnalytics.Event.SHARE) {
                if (!properties.isKeyEmpty("cart_id")) {
                    params.putString(FirebaseAnalytics.Param.ITEM_ID, properties.getString("cart_id"))
                } else if (!properties.isKeyEmpty("product_id")) {
                    params.putString(FirebaseAnalytics.Param.ITEM_ID, properties.getString("product_id"))
                }
            }
            if (firebaseEvent == FirebaseAnalytics.Event.VIEW_PROMOTION || firebaseEvent == FirebaseAnalytics.Event.SELECT_PROMOTION) {
                if (!properties.isKeyEmpty("name")) {
                    params.putString(FirebaseAnalytics.Param.PROMOTION_NAME, properties.getString("name"))
                }
            }
            if (firebaseEvent == FirebaseAnalytics.Event.SELECT_CONTENT) {
                if (!properties.isKeyEmpty("product_id")) {
                    params.putString(FirebaseAnalytics.Param.ITEM_ID, properties.getString("product_id"))
                }
                params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "product")
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
        if (!properties.isKeyEmpty("revenue") && properties.isDouble("revenue")) {
            params.putDouble(FirebaseAnalytics.Param.VALUE, properties.getDouble("revenue") ?: 0.0)
        } else if (!properties.isKeyEmpty("value") && properties.isDouble("value")) {
            params.putDouble(FirebaseAnalytics.Param.VALUE, properties.getDouble("value") ?: 0.0)
        } else if (!properties.isKeyEmpty("total") && properties.isDouble("total")) {
            params.putDouble(FirebaseAnalytics.Param.VALUE, properties.getDouble("total") ?: 0.0)
        }
        if (EVENT_WITH_PRODUCTS_ARRAY.contains(firebaseEvent) && !properties.isKeyEmpty(ECommerceParamNames.PRODUCTS)) {
            handleProducts(params, properties, true)
        }
        if (EVENT_WITH_PRODUCTS_AT_ROOT.contains(firebaseEvent)) {
            handleProducts(params, properties, false)
        }
        for (propertyKey in properties.keys) {
            if (ECOMMERCE_PROPERTY_MAPPING.containsKey(propertyKey) && !properties.isKeyEmpty(propertyKey)) {
                params.putString(ECOMMERCE_PROPERTY_MAPPING[propertyKey], properties.getString(propertyKey))
            }
        }
        // Set default Currency to USD, if it is not present in the payload
        if (properties.containsKey("currency") && !properties.isKeyEmpty("currency")) {
            params.putString(FirebaseAnalytics.Param.CURRENCY, properties.getString("currency"))
        } else {
            params.putString(FirebaseAnalytics.Param.CURRENCY, "USD")
        }
        if (!properties.isKeyEmpty("shipping") && properties.isDouble("shipping")) {
            params.putDouble(FirebaseAnalytics.Param.SHIPPING, properties.getDouble("shipping") ?: 0.0)
        }
        if (!properties.isKeyEmpty("tax") && properties.isDouble("tax")) {
            params.putDouble(FirebaseAnalytics.Param.TAX, properties.getDouble("tax") ?: 0.0)
        }
        // order_id is being mapped to FirebaseAnalytics.Param.TRANSACTION_ID.∂
        if (!properties.isKeyEmpty("order_id")) {
            params.putString(FirebaseAnalytics.Param.TRANSACTION_ID, properties.getString("order_id"))
            // As this change is made at version 2.0.2. So to have the backward compatibility, we're inserting order_id properties as well.
            params.putString("order_id", properties.getString("order_id"))
        }
    }

    private fun handleProducts(params: Bundle, properties: JsonObject, isProductsArray: Boolean) {
        // If Products array is present
        if (isProductsArray) {
            val products = properties[ECommerceParamNames.PRODUCTS] as? JsonArray ?: return
            if (products.isNotEmpty()) {
                val mappedProducts = ArrayList<Bundle>()
                for (i in 0 until products.size) {
                    try {
                        val product = products[i] as JsonObject
                        val productBundle = Bundle()
                        for (key in PRODUCT_PROPERTIES_MAPPING.keys) {
                            if (!product.isKeyEmpty(key)) {
                                putProductValue(productBundle, PRODUCT_PROPERTIES_MAPPING[key]!!, product[key])
                            }
                        }
                        if (!productBundle.isEmpty) {
                            mappedProducts.add(productBundle)
                        }
                    } catch (e: JSONException) {
                        LoggerAnalytics.debug("Error while getting Products: $products")
                    } catch (e: ClassCastException) {
                        // If products contains list of null value
                        LoggerAnalytics.debug("Error while getting Products: $products")
                    }
                }
                if (mappedProducts.isNotEmpty()) {
                    params.putParcelableArrayList(FirebaseAnalytics.Param.ITEMS, mappedProducts)
                }
            }
        } else {
            val productBundle = Bundle()
            for (key in PRODUCT_PROPERTIES_MAPPING.keys) {
                if (properties.containsKey(key)) {
                    putProductValue(
                        productBundle,
                        PRODUCT_PROPERTIES_MAPPING[key]!!,
                        properties[key]
                    )
                }
            }
            if (!productBundle.isEmpty) {
                params.putParcelableArray(FirebaseAnalytics.Param.ITEMS, arrayOf(productBundle))
            }
        }
    }

    private fun putProductValue(params: Bundle, firebaseKey: String, value: Any?) {
        if (value == null) {
            return
        }
        when (firebaseKey) {
            FirebaseAnalytics.Param.ITEM_ID, FirebaseAnalytics.Param.ITEM_NAME, FirebaseAnalytics.Param.ITEM_CATEGORY -> {
                params.putString(firebaseKey, value.toString())
                return
            }

            FirebaseAnalytics.Param.QUANTITY -> {
                (value as? Number)?.let {
                    params.putLong(firebaseKey, it.toLong())
                }
                return
            }

            FirebaseAnalytics.Param.PRICE -> {
                (value as? Number)?.let {
                    params.putDouble(firebaseKey, it.toDouble())
                }
                return
            }

            else -> LoggerAnalytics.debug("Product value is not of expected type")
        }
    }

    private fun makeFirebaseEvent(firebaseEvent: String, params: Bundle, properties: JsonObject?) {
        attachAllCustomProperties(params, properties)
        LoggerAnalytics.debug("Logged \"$firebaseEvent\" to Firebase and properties: $properties")
        firebaseAnalytics?.logEvent(firebaseEvent, params)
    }
}
