package com.rudderstack.integration.kotlin.appsflyer

import com.appsflyer.AFInAppEventParameterName
import com.appsflyer.AFInAppEventType
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceEvents
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceParamNames
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class UtilsTest {

    @Test
    fun `given products searched event, when mapEventToAppsFlyer is called, then returns correct mapping`() {
        val properties = buildJsonObject {
            put(ECommerceParamNames.QUERY, "laptop")
        }

        val (eventName, eventProps) = mapEventToAppsFlyer(ECommerceEvents.PRODUCTS_SEARCHED, properties)

        assert(eventName == AFInAppEventType.SEARCH)
        assert(eventProps[AFInAppEventParameterName.SEARCH_STRING] == "laptop")
    }

    @Test
    fun `given product viewed event, when mapEventToAppsFlyer is called, then returns correct mapping`() {
        val properties = buildJsonObject {
            put(ECommerceParamNames.PRODUCT_ID, "prod123")
            put(ECommerceParamNames.PRICE, "29.99")
            put(ECommerceParamNames.CATEGORY, "Electronics")
            put(ECommerceParamNames.CURRENCY, "USD")
        }

        val (eventName, eventProps) = mapEventToAppsFlyer(ECommerceEvents.PRODUCT_VIEWED, properties)

        assert(eventName == AFInAppEventType.CONTENT_VIEW)
        assert(eventProps[AFInAppEventParameterName.CONTENT_ID] == "prod123")
        assert(eventProps[AFInAppEventParameterName.PRICE] == "29.99")
        assert(eventProps[AFInAppEventParameterName.CONTENT_TYPE] == "Electronics")
        assert(eventProps[AFInAppEventParameterName.CURRENCY] == "USD")
    }

    @Test
    fun `given order completed event, when mapEventToAppsFlyer is called, then returns correct mapping with order details`() {
        val properties = buildJsonObject {
            put(ECommerceParamNames.ORDER_ID, "order123")
            put(ECommerceParamNames.TOTAL, "99.99")
            put(ECommerceParamNames.REVENUE, "89.99")
            put(ECommerceParamNames.CURRENCY, "USD")
        }

        val (eventName, eventProps) = mapEventToAppsFlyer(ECommerceEvents.ORDER_COMPLETED, properties)

        assert(eventName == AFInAppEventType.PURCHASE)
        assert(eventProps[AFInAppEventParameterName.RECEIPT_ID] == "order123")
        assert(eventProps["af_order_id"] == "order123")
        assert(eventProps[AFInAppEventParameterName.PRICE] == "99.99")
        assert(eventProps[AFInAppEventParameterName.REVENUE] == "89.99")
        assert(eventProps[AFInAppEventParameterName.CURRENCY] == "USD")
    }

    @Test
    fun `given custom event, when mapEventToAppsFlyer is called, then returns lowercase with underscores`() {
        val customEvent = "My Custom Event"

        val (eventName, _) = mapEventToAppsFlyer(customEvent, null)

        assert(eventName == "my_custom_event")
    }

    @Test
    fun `given first purchase event, when mapEventToAppsFlyer is called, then returns first_purchase event name`() {
        val properties = buildJsonObject {
            put(ECommerceParamNames.TOTAL, "49.99")
        }

        val (eventName, eventProps) = mapEventToAppsFlyer("first_purchase", properties)

        assert(eventName == "first_purchase")
        assert(eventProps[AFInAppEventParameterName.PRICE] == "49.99")
    }

    @Test
    fun `given product list viewed event with products, when mapEventToAppsFlyer is called, then product list is set`() {
        val products = buildJsonArray {
            add(buildJsonObject {
                put("product_id", "prod1")
                put("name", "Product 1")
            })
            add(buildJsonObject {
                put("product_id", "prod2")
                put("name", "Product 2")
            })
        }
        val properties = buildJsonObject {
            put(ECommerceParamNames.CATEGORY, "Electronics")
            put(ECommerceParamNames.PRODUCTS, products)
        }

        val (eventName, eventProps) = mapEventToAppsFlyer(ECommerceEvents.PRODUCT_LIST_VIEWED, properties)

        assert(eventName == AFInAppEventType.LIST_VIEW)
        assert(eventProps[AFInAppEventParameterName.CONTENT_TYPE] == "Electronics")
        val contentList = eventProps[AFInAppEventParameterName.CONTENT_LIST] as Array<*>
        assert(contentList.size == 2)
        assert(contentList[0] == "prod1")
        assert(contentList[1] == "prod2")
    }

    @Test
    fun `given promotion viewed event, when mapEventToAppsFlyer is called, then creative and currency are set`() {
        val properties = buildJsonObject {
            put(CREATIVE, "banner_ad")
            put(ECommerceParamNames.CURRENCY, "USD")
        }

        val (eventName, eventProps) = mapEventToAppsFlyer(ECommerceEvents.PROMOTION_VIEWED, properties)

        assert(eventName == AFInAppEventType.AD_VIEW)
        assert(eventProps[AFInAppEventParameterName.AD_REVENUE_AD_TYPE] == "banner_ad")
        assert(eventProps[AFInAppEventParameterName.CURRENCY] == "USD")
    }

    @Test
    fun `given product shared event, when mapEventToAppsFlyer is called, then share message is set`() {
        val properties = buildJsonObject {
            put(ECommerceParamNames.SHARE_MESSAGE, "Check out this product!")
        }

        val (eventName, eventProps) = mapEventToAppsFlyer(ECommerceEvents.PRODUCT_SHARED, properties)

        assert(eventName == AFInAppEventType.SHARE)
        assert(eventProps[AFInAppEventParameterName.DESCRIPTION] == "Check out this product!")
    }

    @Test
    fun `given product reviewed event, when mapEventToAppsFlyer is called, then product id and rating are set`() {
        val properties = buildJsonObject {
            put(ECommerceParamNames.PRODUCT_ID, "prod123")
            put(ECommerceParamNames.RATING, "4.5")
        }

        val (eventName, eventProps) = mapEventToAppsFlyer(ECommerceEvents.PRODUCT_REVIEWED, properties)

        assert(eventName == AFInAppEventType.RATE)
        assert(eventProps[AFInAppEventParameterName.CONTENT_ID] == "prod123")
        assert(eventProps[AFInAppEventParameterName.RATING_VALUE] == "4.5")
    }

    @Test
    fun `given product added event with quantity, when mapEventToAppsFlyer is called, then quantity is included`() {
        val properties = buildJsonObject {
            put(ECommerceParamNames.PRODUCT_ID, "prod123")
            put(ECommerceParamNames.QUANTITY, "2")
            put(ECommerceParamNames.PRICE, "29.99")
        }

        val (eventName, eventProps) = mapEventToAppsFlyer(ECommerceEvents.PRODUCT_ADDED, properties)

        assert(eventName == AFInAppEventType.ADD_TO_CART)
        assert(eventProps[AFInAppEventParameterName.CONTENT_ID] == "prod123")
        assert(eventProps[AFInAppEventParameterName.QUANTITY] == "2")
        assert(eventProps[AFInAppEventParameterName.PRICE] == "29.99")
    }

    @Test
    fun `given properties with reserved keywords, when attachAllCustomProperties is called, then reserved keywords are filtered`() {
        val properties = buildJsonObject {
            put(ECommerceParamNames.PRICE, "29.99")
            put(ECommerceParamNames.CURRENCY, "USD")
            put("custom_prop", "value")
            put("another_prop", "another_value")
        }
        val eventProps = mutableMapOf<String, Any>()

        attachAllCustomProperties(eventProps, properties)

        assert(!eventProps.containsKey(ECommerceParamNames.PRICE))
        assert(!eventProps.containsKey(ECommerceParamNames.CURRENCY))
        assert(eventProps["custom_prop"] == "value")
        assert(eventProps["another_prop"] == "another_value")
    }

    @Test
    fun `given JsonObject with quoted strings, when toMutableMap is called, then quotes are removed`() {
        val jsonObject = buildJsonObject {
            put("string_prop", "value")
            put("number_prop", 42)
            put("boolean_prop", true)
        }

        val map = jsonObject.toMutableMap()

        assert(map["string_prop"] == "value")
        assert(map["number_prop"] == "42")
        assert(map["boolean_prop"] == "true")
    }

    @Test
    fun `given null JsonObject, when toMutableMap is called, then returns empty map`() {
        val map = null.toMutableMap()

        assert(map.isEmpty())
    }

    @Test
    fun `given checkout event with products, when handleProducts is called, then product arrays are set`() {
        val products = buildJsonArray {
            add(buildJsonObject {
                put("product_id", "prod1")
                put("category", "Electronics")
                put("quantity", "1")
            })
            add(buildJsonObject {
                put("product_id", "prod2")
                put("category", "Books")
                put("quantity", "2")
            })
        }
        val properties = buildJsonObject {
            put(ECommerceParamNames.PRODUCTS, products)
        }
        val eventProps = mutableMapOf<String, Any>()

        handleProducts(properties, eventProps)

        val productIds = eventProps[AFInAppEventParameterName.CONTENT_ID] as Array<*>
        
        assert(productIds.size == 2)
        assert(productIds.contains("prod1"))
        assert(productIds.contains("prod2"))
    }

    @ParameterizedTest
    @MethodSource("provideEcommerceEventMappings")
    fun `given ecommerce events, when mapEventToAppsFlyer is called, then correct AppsFlyer events are returned`(
        rudderEvent: String,
        expectedAppsFlyerEvent: String
    ) {
        val (eventName, _) = mapEventToAppsFlyer(rudderEvent, null)

        assert(eventName == expectedAppsFlyerEvent)
    }

    companion object {
        @JvmStatic
        fun provideEcommerceEventMappings(): List<Arguments> {
            return listOf(
                Arguments.of(ECommerceEvents.PRODUCTS_SEARCHED, AFInAppEventType.SEARCH),
                Arguments.of(ECommerceEvents.PRODUCT_VIEWED, AFInAppEventType.CONTENT_VIEW),
                Arguments.of(ECommerceEvents.PRODUCT_LIST_VIEWED, AFInAppEventType.LIST_VIEW),
                Arguments.of(ECommerceEvents.PRODUCT_ADDED_TO_WISH_LIST, AFInAppEventType.ADD_TO_WISH_LIST),
                Arguments.of(ECommerceEvents.PRODUCT_ADDED, AFInAppEventType.ADD_TO_CART),
                Arguments.of(ECommerceEvents.CHECKOUT_STARTED, AFInAppEventType.INITIATED_CHECKOUT),
                Arguments.of(ECommerceEvents.ORDER_COMPLETED, AFInAppEventType.PURCHASE),
                Arguments.of(ECommerceEvents.PRODUCT_REMOVED, "remove_from_cart"),
                Arguments.of(ECommerceEvents.PROMOTION_VIEWED, AFInAppEventType.AD_VIEW),
                Arguments.of(ECommerceEvents.PROMOTION_CLICKED, AFInAppEventType.AD_CLICK),
                Arguments.of(ECommerceEvents.PAYMENT_INFO_ENTERED, AFInAppEventType.ADD_PAYMENT_INFO),
                Arguments.of(ECommerceEvents.PRODUCT_SHARED, AFInAppEventType.SHARE),
                Arguments.of(ECommerceEvents.CART_SHARED, AFInAppEventType.SHARE),
                Arguments.of(ECommerceEvents.PRODUCT_REVIEWED, AFInAppEventType.RATE),
                Arguments.of("first_purchase", "first_purchase")
            )
        }
    }
}
