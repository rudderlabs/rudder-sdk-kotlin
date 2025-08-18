package com.rudderstack.integration.kotlin.appsflyer

import com.appsflyer.AFInAppEventParameterName
import com.appsflyer.AFInAppEventType
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceEvents
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceParamNames
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
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

        assert(eventName == expectedAppsFlyerEvent)
    }

    @Test
    fun `given custom event name, when mapEventToAppsFlyer is called, then returns lowercase with underscores`() {
        val customEvent = "My Custom Event"

        val (eventName, _) = mapEventToAppsFlyer(customEvent, null)

        assert(eventName == "my_custom_event")
    }

    // ===== PROPERTY MAPPING TESTING =====

    @Test
    fun `given products searched event with query, when mapEventToAppsFlyer is called, then search string is mapped correctly`() {
        val properties = buildJsonObject {
            put(ECommerceParamNames.QUERY, "laptop")
        }

        val (eventName, eventProps) = mapEventToAppsFlyer(ECommerceEvents.PRODUCTS_SEARCHED, properties)

        assert(eventName == AFInAppEventType.SEARCH)
        assert(eventProps[AFInAppEventParameterName.SEARCH_STRING] == "laptop")
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

        assert(eventName == AFInAppEventType.CONTENT_VIEW)
        assert(eventProps[AFInAppEventParameterName.CONTENT_ID] == "prod123")
        assert(eventProps[AFInAppEventParameterName.PRICE] == 29.99)
        assert(eventProps[AFInAppEventParameterName.CONTENT_TYPE] == "Electronics")
        assert(eventProps[AFInAppEventParameterName.CURRENCY] == "USD")
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

        assert(eventName == AFInAppEventType.PURCHASE)
        assert(eventProps[AFInAppEventParameterName.RECEIPT_ID] == "order123")
        assert(eventProps["af_order_id"] == "order123")
        assert(eventProps[AFInAppEventParameterName.PRICE] == 99.99)
        assert(eventProps[AFInAppEventParameterName.REVENUE] == 89.99)
        assert(eventProps[AFInAppEventParameterName.CURRENCY] == "USD")
    }

    // ===== COMPREHENSIVE PROPERTY MAPPING TESTING =====

    @Test
    fun `given first purchase event, when mapEventToAppsFlyer is called, then returns first_purchase event name`() {
        val properties = buildJsonObject {
            put(ECommerceParamNames.TOTAL, 49.99)
        }

        val (eventName, eventProps) = mapEventToAppsFlyer("first_purchase", properties)

        assert(eventName == "first_purchase")
        assert(eventProps[AFInAppEventParameterName.PRICE] == 49.99)
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
            put(ECommerceParamNames.RATING, 4.5)
        }

        val (eventName, eventProps) = mapEventToAppsFlyer(ECommerceEvents.PRODUCT_REVIEWED, properties)

        assert(eventName == AFInAppEventType.RATE)
        assert(eventProps[AFInAppEventParameterName.CONTENT_ID] == "prod123")
        assert(eventProps[AFInAppEventParameterName.RATING_VALUE] == 4.5)
    }

    @Test
    fun `given product added event with quantity, when mapEventToAppsFlyer is called, then quantity is included`() {
        val properties = buildJsonObject {
            put(ECommerceParamNames.PRODUCT_ID, "prod123")
            put(ECommerceParamNames.QUANTITY, 2)
            put(ECommerceParamNames.PRICE, 29.99)
        }

        val (eventName, eventProps) = mapEventToAppsFlyer(ECommerceEvents.PRODUCT_ADDED, properties)

        assert(eventName == AFInAppEventType.ADD_TO_CART)
        assert(eventProps[AFInAppEventParameterName.CONTENT_ID] == "prod123")
        assert(eventProps[AFInAppEventParameterName.QUANTITY] == 2)
        assert(eventProps[AFInAppEventParameterName.PRICE] == 29.99)
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
            assert(TRACK_RESERVED_KEYWORDS.contains(keyword)) {
                "Expected '$keyword' to be in TRACK_RESERVED_KEYWORDS"
            }
        }

        assert(TRACK_RESERVED_KEYWORDS.size == expectedKeywords.size) {
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

        assert(eventProps.isEmpty()) {
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
            assert(!eventProps.containsKey(keyword)) {
                "Reserved keyword '$keyword' should not be present in eventProps"
            }
        }

        // Assert custom properties ARE present
        assert(eventProps["custom_field"] == "custom_value")
        assert(eventProps["user_preference"] == "dark_mode")
        assert(eventProps["campaign_source"] == "email")
        assert(eventProps["special_offer"] == true)
        assert(eventProps["discount_amount"] == 5.00)

        assert(eventProps.size == 5) {
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

            assert(!eventProps.containsKey(keyword)) {
                "Reserved keyword '$keyword' should be filtered out"
            }
            assert(eventProps["custom_prop"] == "custom_value") {
                "Custom property should be included when testing keyword '$keyword'"
            }
            assert(eventProps.size == 1) {
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
        assert(!eventProps.containsKey(ECommerceParamNames.PRODUCTS)) {
            "PRODUCTS should be filtered as it's a reserved keyword"
        }
        assert(eventProps["custom_metadata"] == "test_value") {
            "Custom property should be included"
        }
        assert(eventProps.size == 1) {
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

        assert(!eventProps.containsKey(ECommerceParamNames.PRICE))
        assert(!eventProps.containsKey(ECommerceParamNames.CURRENCY))
        assert(eventProps["empty_string"] == "") // Empty strings are preserved in current implementation
        assert(eventProps["valid_prop"] == "valid_value")
        assert(eventProps.size == 2) // valid_prop + empty_string
    }

    // ===== EXISTING TESTS CONTINUE =====

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

        assert(productIds.size == 2)
        assert(productIds.contains("prod1"))
        assert(productIds.contains("prod2"))
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

        assert(map["string_prop"] == "value")
        assert(map["number_prop"] == 42)
        assert(map["long_prop"] == 15000000000L)
        assert(map["boolean_prop"] == true)
        assert(map["double_prop"] == 19.99)
        assert(map["nested_object"] is Map<*, *>)
        val nestedObject = map["nested_object"] as Map<*, *>
        assert(nestedObject["nested_key"] == "nested_value")
        assert(nestedObject["nested_number"] == 100)
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
        assert(stringArray.size == 2)
        assert(stringArray[0] == "item1")
        assert(stringArray[1] == "item2")

        val numberArray = map["number_array"] as List<*>
        assert(numberArray.size == 3)
        assert(numberArray.containsAll(listOf(1, 2, 3)))
    }

    // ===== EDGE CASE TESTING =====

    @Test
    fun `given null or empty properties, when mapEventToAppsFlyer is called, then handles gracefully`() {
        val (eventName1, eventProps1) = mapEventToAppsFlyer("custom_event", null)
        val (eventName2, eventProps2) = mapEventToAppsFlyer("custom_event", buildJsonObject { })

        assert(eventName1 == "custom_event")
        assert(eventProps1.isEmpty())
        assert(eventName2 == "custom_event")
        assert(eventProps2.isEmpty())
    }

    @Test
    fun `given event with missing required fields, when mapEventToAppsFlyer is called, then handles gracefully without errors`() {
        val properties = buildJsonObject {
            put("irrelevant_field", "value")
        }

        val (eventName, eventProps) = mapEventToAppsFlyer(ECommerceEvents.PRODUCT_VIEWED, properties)

        assert(eventName == AFInAppEventType.CONTENT_VIEW)
        // Should not contain the mapping fields since they weren't provided
        assert(!eventProps.containsKey(AFInAppEventParameterName.CONTENT_ID))
        assert(!eventProps.containsKey(AFInAppEventParameterName.PRICE))
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
            assert(eventName == expected) { "Expected '$expected' but got '$eventName' for input '$input'" }
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
        assert(productIds.size == 2)
        assert(productIds.contains("prod1"))
        assert(productIds.contains("prod2"))
    }

}
