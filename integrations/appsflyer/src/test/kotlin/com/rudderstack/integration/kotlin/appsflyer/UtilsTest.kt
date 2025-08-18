package com.rudderstack.integration.kotlin.appsflyer

import com.appsflyer.AFInAppEventParameterName
import com.appsflyer.AFInAppEventType
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceEvents
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceParamNames
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class UtilsTest {

    // ===== EVENT MAPPING LOGIC TESTING =====

    @ParameterizedTest
    @MethodSource("provideEcommerceEventMappings")
    fun `given ecommerce events, when mapEventToAppsFlyer is called, then correct AppsFlyer events are returned`(
        rudderEvent: String,
        expectedAppsFlyerEvent: String
    ) {
        val (eventName, _) = mapEventToAppsFlyer(rudderEvent, null)

        Assertions.assertEquals(expectedAppsFlyerEvent, eventName)
    }

    @Test
    fun `given custom event name, when mapEventToAppsFlyer is called, then returns lowercase with underscores`() {
        val customEvent = "My Custom Event"

        val (eventName, _) = mapEventToAppsFlyer(customEvent, null)

        Assertions.assertEquals("my_custom_event", eventName)
    }

    // ===== PROPERTY MAPPING TESTING =====

    @Test
    fun `given products searched event with query, when mapEventToAppsFlyer is called, then search string is mapped correctly`() {
        val properties = buildJsonObject {
            put(ECommerceParamNames.QUERY, "laptop")
        }

        val (eventName, eventProps) = mapEventToAppsFlyer(ECommerceEvents.PRODUCTS_SEARCHED, properties)

        Assertions.assertEquals(AFInAppEventType.SEARCH, eventName)
        Assertions.assertEquals("laptop", eventProps[AFInAppEventParameterName.SEARCH_STRING])
    }

    @Test
    fun `given product viewed event with details, when mapEventToAppsFlyer is called, then all properties are mapped correctly`() {
        val properties = buildJsonObject {
            put(ECommerceParamNames.PRODUCT_ID, "prod123")
            put(ECommerceParamNames.PRICE, 29.99)
            put(ECommerceParamNames.CATEGORY, "Electronics")
            put(ECommerceParamNames.CURRENCY, "USD")
        }

        val (eventName, eventProps) = mapEventToAppsFlyer(ECommerceEvents.PRODUCT_VIEWED, properties)

        Assertions.assertEquals(AFInAppEventType.CONTENT_VIEW, eventName)
        Assertions.assertEquals("prod123", eventProps[AFInAppEventParameterName.CONTENT_ID])
        Assertions.assertEquals(29.99, eventProps[AFInAppEventParameterName.PRICE])
        Assertions.assertEquals("Electronics", eventProps[AFInAppEventParameterName.CONTENT_TYPE])
        Assertions.assertEquals("USD", eventProps[AFInAppEventParameterName.CURRENCY])
    }

    @Test
    fun `given order completed event with order details, when mapEventToAppsFlyer is called, then order fields are mapped correctly`() {
        val properties = buildJsonObject {
            put(ECommerceParamNames.ORDER_ID, "order123")
            put(ECommerceParamNames.TOTAL, 99.99)
            put(ECommerceParamNames.REVENUE, 89.99)
            put(ECommerceParamNames.CURRENCY, "USD")
        }

        val (eventName, eventProps) = mapEventToAppsFlyer(ECommerceEvents.ORDER_COMPLETED, properties)

        Assertions.assertEquals(AFInAppEventType.PURCHASE, eventName)
        Assertions.assertEquals("order123", eventProps[AFInAppEventParameterName.RECEIPT_ID])
        Assertions.assertEquals("order123", eventProps["af_order_id"])
        Assertions.assertEquals(99.99, eventProps[AFInAppEventParameterName.PRICE])
        Assertions.assertEquals(89.99, eventProps[AFInAppEventParameterName.REVENUE])
        Assertions.assertEquals("USD", eventProps[AFInAppEventParameterName.CURRENCY])
    }

    // ===== COMPREHENSIVE PROPERTY MAPPING TESTING =====

    @Test
    fun `given first purchase event, when mapEventToAppsFlyer is called, then returns first_purchase event name`() {
        val properties = buildJsonObject {
            put(ECommerceParamNames.TOTAL, 49.99)
        }

        val (eventName, eventProps) = mapEventToAppsFlyer("first_purchase", properties)

        Assertions.assertEquals("first_purchase", eventName)
        Assertions.assertEquals(49.99, eventProps[AFInAppEventParameterName.PRICE])
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

        Assertions.assertEquals(AFInAppEventType.LIST_VIEW, eventName)
        Assertions.assertEquals("Electronics", eventProps[AFInAppEventParameterName.CONTENT_TYPE])
        val contentList = eventProps[AFInAppEventParameterName.CONTENT_LIST] as Array<*>
        Assertions.assertEquals(2, contentList.size)
        Assertions.assertEquals("prod1", contentList[0])
        Assertions.assertEquals("prod2", contentList[1])
    }

    @Test
    fun `given promotion viewed event, when mapEventToAppsFlyer is called, then creative and currency are set`() {
        val properties = buildJsonObject {
            put(CREATIVE, "banner_ad")
            put(ECommerceParamNames.CURRENCY, "USD")
        }

        val (eventName, eventProps) = mapEventToAppsFlyer(ECommerceEvents.PROMOTION_VIEWED, properties)

        Assertions.assertEquals(AFInAppEventType.AD_VIEW, eventName)
        Assertions.assertEquals("banner_ad", eventProps[AFInAppEventParameterName.AD_REVENUE_AD_TYPE])
        Assertions.assertEquals("USD", eventProps[AFInAppEventParameterName.CURRENCY])
    }

    @Test
    fun `given product shared event, when mapEventToAppsFlyer is called, then share message is set`() {
        val properties = buildJsonObject {
            put(ECommerceParamNames.SHARE_MESSAGE, "Check out this product!")
        }

        val (eventName, eventProps) = mapEventToAppsFlyer(ECommerceEvents.PRODUCT_SHARED, properties)

        Assertions.assertEquals(AFInAppEventType.SHARE, eventName)
        Assertions.assertEquals("Check out this product!", eventProps[AFInAppEventParameterName.DESCRIPTION])
    }

    @Test
    fun `given product reviewed event, when mapEventToAppsFlyer is called, then product id and rating are set`() {
        val properties = buildJsonObject {
            put(ECommerceParamNames.PRODUCT_ID, "prod123")
            put(ECommerceParamNames.RATING, 4.5)
        }

        val (eventName, eventProps) = mapEventToAppsFlyer(ECommerceEvents.PRODUCT_REVIEWED, properties)

        Assertions.assertEquals(AFInAppEventType.RATE, eventName)
        Assertions.assertEquals("prod123", eventProps[AFInAppEventParameterName.CONTENT_ID])
        Assertions.assertEquals(4.5, eventProps[AFInAppEventParameterName.RATING_VALUE])
    }

    @Test
    fun `given product added event with quantity, when mapEventToAppsFlyer is called, then quantity is included`() {
        val properties = buildJsonObject {
            put(ECommerceParamNames.PRODUCT_ID, "prod123")
            put(ECommerceParamNames.QUANTITY, 2)
            put(ECommerceParamNames.PRICE, 29.99)
        }

        val (eventName, eventProps) = mapEventToAppsFlyer(ECommerceEvents.PRODUCT_ADDED, properties)

        Assertions.assertEquals(AFInAppEventType.ADD_TO_CART, eventName)
        Assertions.assertEquals("prod123", eventProps[AFInAppEventParameterName.CONTENT_ID])
        Assertions.assertEquals(2, eventProps[AFInAppEventParameterName.QUANTITY])
        Assertions.assertEquals(29.99, eventProps[AFInAppEventParameterName.PRICE])
    }

    // ===== TRACK_RESERVED_KEYWORDS TESTING =====

    @Test
    fun `given TRACK_RESERVED_KEYWORDS, when accessed, then contains all expected reserved property names`() {
        val expectedKeywords = listOf(
            ECommerceParamNames.QUERY,
            ECommerceParamNames.PRICE,
            ECommerceParamNames.PRODUCT_ID,
            ECommerceParamNames.CATEGORY,
            ECommerceParamNames.CURRENCY,
            ECommerceParamNames.PRODUCTS,
            ECommerceParamNames.QUANTITY,
            ECommerceParamNames.TOTAL,
            ECommerceParamNames.REVENUE,
            ECommerceParamNames.ORDER_ID,
            ECommerceParamNames.SHARE_MESSAGE,
            CREATIVE,
            ECommerceParamNames.RATING
        )

        expectedKeywords.forEach { keyword ->
            Assertions.assertTrue(TRACK_RESERVED_KEYWORDS.contains(keyword)) {
                "Expected '$keyword' to be in TRACK_RESERVED_KEYWORDS"
            }
        }

        Assertions.assertEquals(expectedKeywords.size, TRACK_RESERVED_KEYWORDS.size) {
            "TRACK_RESERVED_KEYWORDS size mismatch. Expected ${expectedKeywords.size}, got ${TRACK_RESERVED_KEYWORDS.size}"
        }
    }

    @Test
    fun `given properties with only reserved keywords, when mapCustomPropertiesToAppsFlyer is called, then no properties are added`() {
        val properties = buildJsonObject {
            put(ECommerceParamNames.QUERY, "search_term")
            put(ECommerceParamNames.PRICE, 29.99)
            put(ECommerceParamNames.PRODUCT_ID, "prod123")
            put(ECommerceParamNames.CATEGORY, "Electronics")
            put(ECommerceParamNames.CURRENCY, "USD")
            put(ECommerceParamNames.QUANTITY, 2)
            put(ECommerceParamNames.TOTAL, 59.98)
            put(ECommerceParamNames.REVENUE, 54.98)
            put(ECommerceParamNames.ORDER_ID, "order123")
            put(ECommerceParamNames.SHARE_MESSAGE, "Check this out!")
            put(CREATIVE, "banner_ad")
            put(ECommerceParamNames.RATING, 4.5)
        }
        val eventProps = mutableMapOf<String, Any>()

        mapCustomPropertiesToAppsFlyer(properties, eventProps)

        Assertions.assertTrue(eventProps.isEmpty()) {
            "Expected no properties to be added, but found: ${eventProps.keys}"
        }
    }

    @Test
    fun `given properties with mix of reserved and custom keywords, when mapCustomPropertiesToAppsFlyer is called, then only custom properties are added`() {
        val properties = buildJsonObject {
            // Reserved keywords - should be filtered
            put(ECommerceParamNames.PRICE, 29.99)
            put(ECommerceParamNames.CURRENCY, "USD")
            put(ECommerceParamNames.PRODUCT_ID, "prod123")
            put(CREATIVE, "banner_ad")

            // Custom properties - should be included
            put("custom_field", "custom_value")
            put("user_preference", "dark_mode")
            put("campaign_source", "email")
            put("special_offer", true)
            put("discount_amount", 5.00)
        }
        val eventProps = mutableMapOf<String, Any>()

        mapCustomPropertiesToAppsFlyer(properties, eventProps)

        // Assert reserved keywords are NOT present
        TRACK_RESERVED_KEYWORDS.forEach { keyword ->
            Assertions.assertFalse(eventProps.containsKey(keyword)) {
                "Reserved keyword '$keyword' should not be present in eventProps"
            }
        }

        // Assert custom properties ARE present
        Assertions.assertEquals("custom_value", eventProps["custom_field"])
        Assertions.assertEquals("dark_mode", eventProps["user_preference"])
        Assertions.assertEquals("email", eventProps["campaign_source"])
        Assertions.assertEquals(true, eventProps["special_offer"])
        Assertions.assertEquals(5.00, eventProps["discount_amount"])

        Assertions.assertEquals(5, eventProps.size) {
            "Expected 5 custom properties, but found ${eventProps.size}"
        }
    }

    @Test
    fun `given properties with all reserved keywords individually, when mapCustomPropertiesToAppsFlyer is called, then each keyword is properly filtered`() {
        TRACK_RESERVED_KEYWORDS.forEach { keyword ->
            val properties = buildJsonObject {
                put(keyword, "test_value")
                put("custom_prop", "custom_value") // Add one custom property for comparison
            }
            val eventProps = mutableMapOf<String, Any>()

            mapCustomPropertiesToAppsFlyer(properties, eventProps)

            Assertions.assertFalse(eventProps.containsKey(keyword)) {
                "Reserved keyword '$keyword' should be filtered out"
            }
            Assertions.assertEquals("custom_value", eventProps["custom_prop"]) {
                "Custom property should be included when testing keyword '$keyword'"
            }
            Assertions.assertEquals(1, eventProps.size) {
                "Expected only 1 custom property when testing keyword '$keyword', but found ${eventProps.size}"
            }
        }
    }

    @Test
    fun `given properties with products array containing reserved keywords, when mapCustomPropertiesToAppsFlyer is called, then reserved keywords in products are not filtered at top level`() {
        val products = buildJsonArray {
            add(buildJsonObject {
                put(ECommerceParamNames.PRODUCT_ID, "prod1") // This is inside products array, not top-level
                put(ECommerceParamNames.PRICE, 29.99)
            })
        }
        val properties = buildJsonObject {
            put(ECommerceParamNames.PRODUCTS, products) // This is a reserved keyword at top level
            put("custom_metadata", "test_value") // This should be included
        }
        val eventProps = mutableMapOf<String, Any>()

        mapCustomPropertiesToAppsFlyer(properties, eventProps)

        // The PRODUCTS key itself should be filtered as it's a reserved keyword
        Assertions.assertFalse(eventProps.containsKey(ECommerceParamNames.PRODUCTS)) {
            "PRODUCTS should be filtered as it's a reserved keyword"
        }
        Assertions.assertEquals("test_value", eventProps["custom_metadata"]) {
            "Custom property should be included"
        }
        Assertions.assertEquals(1, eventProps.size) {
            "Expected only 1 custom property, but found ${eventProps.size}"
        }
    }

    @Test
    fun `given properties with empty and null values along with reserved keywords, when mapCustomPropertiesToAppsFlyer is called, then reserved keywords are filtered but empty strings are preserved`() {
        val properties = buildJsonObject {
            put(ECommerceParamNames.PRICE, 29.99) // Reserved keyword - should be filtered
            put("empty_string", "") // Empty value - should be preserved (only null values are filtered)
            put("valid_prop", "valid_value") // Valid custom property - should be included
            put(ECommerceParamNames.CURRENCY, "USD") // Reserved keyword - should be filtered
            // Note: JsonObject doesn't allow null values directly in buildJsonObject
        }
        val eventProps = mutableMapOf<String, Any>()

        mapCustomPropertiesToAppsFlyer(properties, eventProps)

        Assertions.assertFalse(eventProps.containsKey(ECommerceParamNames.PRICE))
        Assertions.assertFalse(eventProps.containsKey(ECommerceParamNames.CURRENCY))
        Assertions.assertEquals("", eventProps["empty_string"]) // Empty strings are preserved in current implementation
        Assertions.assertEquals("valid_value", eventProps["valid_prop"])
        Assertions.assertEquals(2, eventProps.size) // valid_prop + empty_string
    }

    // ===== EXISTING TESTS CONTINUE =====

    @Test
    fun `given null JsonObject, when toMutableMap is called, then returns empty map`() {
        val map = null.toMutableMap()

        Assertions.assertTrue(map.isEmpty())
    }

    @Test
    fun `given checkout event with products, when handleProducts is called, then product arrays are set`() {
        val products = buildJsonArray {
            add(buildJsonObject {
                put("product_id", "prod1")
                put("category", "Electronics")
                put("quantity", 1)
            })
            add(buildJsonObject {
                put("product_id", "prod2")
                put("category", "Books")
                put("quantity", 2)
            })
        }
        val properties = buildJsonObject {
            put(ECommerceParamNames.PRODUCTS, products)
        }
        val eventProps = mutableMapOf<String, Any>()

        handleProducts(properties, eventProps)

        val productIds = eventProps[AFInAppEventParameterName.CONTENT_ID] as Array<*>

        Assertions.assertEquals(2, productIds.size)
        Assertions.assertTrue(productIds.contains("prod1"))
        Assertions.assertTrue(productIds.contains("prod2"))
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

    // ===== DATA CONVERSION UTILITY TESTING =====

    @Test
    fun `given JsonObject with various primitive types, when toMutableMap is called, then all types are correctly converted`() {
        val jsonObject = buildJsonObject {
            put("string_prop", "value")
            put("number_prop", 42)
            put("long_prop", 15000000000L)
            put("boolean_prop", true)
            put("double_prop", 19.99)
            put("nested_object", buildJsonObject {
                put("nested_key", "nested_value")
                put("nested_number", 100)
            })
        }

        val map = jsonObject.toMutableMap()

        Assertions.assertEquals("value", map["string_prop"])
        Assertions.assertEquals(42, map["number_prop"])
        Assertions.assertEquals(15000000000L, map["long_prop"])
        Assertions.assertEquals(true, map["boolean_prop"])
        Assertions.assertEquals(19.99, map["double_prop"])
        Assertions.assertTrue(map["nested_object"] is Map<*, *>)
        val nestedObject = map["nested_object"] as Map<*, *>
        Assertions.assertEquals("nested_value", nestedObject["nested_key"])
        Assertions.assertEquals(100, nestedObject["nested_number"])
    }

    @Test
    fun `given JsonObject with arrays, when toMutableMap is called, then arrays are converted to lists`() {
        val jsonObject = buildJsonObject {
            put("string_array", buildJsonArray {
                add(JsonPrimitive("item1"))
                add(JsonPrimitive("item2"))
            })
            put("number_array", buildJsonArray {
                add(JsonPrimitive(1))
                add(JsonPrimitive(2))
                add(JsonPrimitive(3))
            })
        }

        val map = jsonObject.toMutableMap()

        val stringArray = map["string_array"] as List<*>
        Assertions.assertEquals(2, stringArray.size)
        Assertions.assertEquals("item1", stringArray[0])
        Assertions.assertEquals("item2", stringArray[1])

        val numberArray = map["number_array"] as List<*>
        Assertions.assertEquals(3, numberArray.size)
        Assertions.assertTrue(numberArray.containsAll(listOf(1, 2, 3)))
    }

    // ===== EDGE CASE TESTING =====

    @Test
    fun `given null or empty properties, when mapEventToAppsFlyer is called, then handles gracefully`() {
        val (eventName1, eventProps1) = mapEventToAppsFlyer("custom_event", null)
        val (eventName2, eventProps2) = mapEventToAppsFlyer("custom_event", buildJsonObject { })

        Assertions.assertEquals("custom_event", eventName1)
        Assertions.assertTrue(eventProps1.isEmpty())
        Assertions.assertEquals("custom_event", eventName2)
        Assertions.assertTrue(eventProps2.isEmpty())
    }

    @Test
    fun `given event with missing required fields, when mapEventToAppsFlyer is called, then handles gracefully without errors`() {
        val properties = buildJsonObject {
            put("irrelevant_field", "value")
        }

        val (eventName, eventProps) = mapEventToAppsFlyer(ECommerceEvents.PRODUCT_VIEWED, properties)

        Assertions.assertEquals(AFInAppEventType.CONTENT_VIEW, eventName)
        // Should not contain the mapping fields since they weren't provided
        Assertions.assertFalse(eventProps.containsKey(AFInAppEventParameterName.CONTENT_ID))
        Assertions.assertFalse(eventProps.containsKey(AFInAppEventParameterName.PRICE))
    }

    // ===== SPECIAL CHARACTER AND EVENT NAME TESTING =====

    @Test
    fun `given event names with various formatting, when mapEventToAppsFlyer is called, then names are normalized correctly`() {
        val testCases = listOf(
            "Event With Spaces" to "event_with_spaces",
            "Event-With-Hyphens" to "event-with-hyphens",
            "UPPERCASE EVENT" to "uppercase_event",
            "MixedCase Event" to "mixedcase_event"
        )

        testCases.forEach { (input, expected) ->
            val (eventName, _) = mapEventToAppsFlyer(input, null)
            Assertions.assertEquals(expected, eventName) { "Expected '$expected' but got '$eventName' for input '$input'" }
        }
    }

    // ===== PRODUCT ARRAY HANDLING TESTING =====

    @Test
    fun `given products with complete data, when handleProducts is called, then product arrays are set correctly`() {
        val products = buildJsonArray {
            add(buildJsonObject {
                put("product_id", "prod1")
                put("category", "Electronics")
                put("quantity", 1)
            })
            add(buildJsonObject {
                put("product_id", "prod2")
                put("category", "Books")
                put("quantity", 2)
            })
        }
        val properties = buildJsonObject {
            put(ECommerceParamNames.PRODUCTS, products)
        }
        val eventProps = mutableMapOf<String, Any>()

        handleProducts(properties, eventProps)

        val productIds = eventProps[AFInAppEventParameterName.CONTENT_ID] as Array<*>
        Assertions.assertEquals(2, productIds.size)
        Assertions.assertTrue(productIds.contains("prod1"))
        Assertions.assertTrue(productIds.contains("prod2"))
    }

}
