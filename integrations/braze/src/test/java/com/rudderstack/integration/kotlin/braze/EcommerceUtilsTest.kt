package com.rudderstack.integration.kotlin.braze

import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EcommerceUtilsTest {

    private val logger: Logger = mockk(relaxed = true)

    private fun build(properties: JsonObject, brazeEvent: String, action: String? = null): JsonObject =
        buildEcommerceEventProperties(properties, brazeEvent, action, logger)

    private fun JsonObject.products(): JsonArray =
        this["products"] as? JsonArray ?: error("Expected a 'products' array but found ${this["products"]}")

    private fun JsonObject.metadata(): JsonObject =
        this["metadata"] as? JsonObject ?: error("Expected a 'metadata' object but found ${this["metadata"]}")

    // region getEcommerceMapping

    @Test
    fun `given mapped event names with varied casing and padding, when resolving, then correct mapping is returned`() {
        assertEquals(EcommerceMapping(BrazeEcommerceEvents.PRODUCT_VIEWED), getEcommerceMapping("Product Viewed"))
        assertEquals(
            EcommerceMapping(BrazeEcommerceEvents.CART_UPDATED, CartUpdatedAction.ADD),
            getEcommerceMapping("product added"),
        )
        assertEquals(
            EcommerceMapping(BrazeEcommerceEvents.CART_UPDATED, CartUpdatedAction.REMOVE),
            getEcommerceMapping("Product Removed"),
        )
        assertEquals(EcommerceMapping(BrazeEcommerceEvents.ORDER_PLACED), getEcommerceMapping("  Order Completed  "))
        assertEquals(EcommerceMapping(BrazeEcommerceEvents.CHECKOUT_STARTED), getEcommerceMapping("Checkout Started"))
        assertEquals(EcommerceMapping(BrazeEcommerceEvents.ORDER_REFUNDED), getEcommerceMapping("Order Refunded"))
        assertEquals(EcommerceMapping(BrazeEcommerceEvents.ORDER_CANCELLED), getEcommerceMapping("Order Cancelled"))
    }

    @Test
    fun `given unmapped or null event names, when resolving, then null is returned`() {
        assertNull(getEcommerceMapping("Cart Viewed"))
        assertNull(getEcommerceMapping("Cart Updated"))
        assertNull(getEcommerceMapping("Some Random Event"))
        assertNull(getEcommerceMapping(null))
    }

    // endregion

    // region product_viewed

    @Test
    fun `given a product viewed event, when built, then fields are mapped, source is android and extras go to metadata`() {
        val properties = buildJsonObject {
            put("product_id", "P1")
            put("name", "Shoe")
            put("variant", "red")
            put("price", 49.99)
            put("currency", "USD")
            put("image_url", "http://img")
            put("url", "http://prod")
            put("type", "footwear")
            put("custom_extra", "extra")
        }

        val result = build(properties, BrazeEcommerceEvents.PRODUCT_VIEWED)

        assertEquals(JsonPrimitive("P1"), result["product_id"])
        assertEquals(JsonPrimitive("Shoe"), result["product_name"])
        assertEquals(JsonPrimitive("red"), result["variant_id"])
        assertEquals(JsonPrimitive(49.99), result["price"])
        assertEquals(JsonPrimitive("USD"), result["currency"])
        assertEquals(JsonPrimitive("http://prod"), result["product_url"])
        assertEquals(JsonPrimitive("footwear"), result["type"])
        assertEquals(JsonPrimitive("android"), result["source"])
        assertFalse(result.containsKey("products"))
        assertEquals(JsonPrimitive("extra"), result.metadata()["custom_extra"])
    }

    @Test
    fun `given product viewed without product_id, when built, then sku is used as fallback for product_id and variant_id`() {
        val properties = buildJsonObject {
            put("sku", "SKU1")
            put("name", "Shoe")
            put("price", 10)
            put("currency", "USD")
        }

        val result = build(properties, BrazeEcommerceEvents.PRODUCT_VIEWED)

        assertEquals(JsonPrimitive("SKU1"), result["product_id"])
        assertEquals(JsonPrimitive("SKU1"), result["variant_id"])
    }

    // endregion

    // region cart_updated (single-product wrap)

    @Test
    fun `given product added without products array, when built, then top-level product is wrapped and action is add`() {
        val properties = buildJsonObject {
            put("cart_id", "C1")
            put("currency", "USD")
            put("total", 99.0)
            put("product_id", "P1")
            put("name", "Shoe")
            put("variant", "red")
            put("quantity", 2)
            put("price", 49.5)
            put("loyalty_points", 10)
        }

        val result = build(properties, BrazeEcommerceEvents.CART_UPDATED, CartUpdatedAction.ADD)

        assertEquals(JsonPrimitive("C1"), result["cart_id"])
        assertEquals(JsonPrimitive(99.0), result["total_value"])
        assertEquals(JsonPrimitive("add"), result["action"])
        assertEquals(JsonPrimitive("android"), result["source"])

        val product = result.products()[0].jsonObject
        assertEquals(JsonPrimitive("P1"), product["product_id"])
        assertEquals(JsonPrimitive("Shoe"), product["product_name"])
        assertEquals(JsonPrimitive("red"), product["variant_id"])
        assertEquals(JsonPrimitive(2), product["quantity"])
        assertEquals(JsonPrimitive(49.5), product["price"])
        // No per-product metadata in the wrap case.
        assertFalse(product.containsKey("metadata"))

        // Unmapped event-level key goes to metadata; consumed product keys do NOT leak there.
        assertEquals(JsonPrimitive(10), result.metadata()["loyalty_points"])
        assertFalse(result.metadata().containsKey("product_id"))
        assertFalse(result.metadata().containsKey("name"))
    }

    @Test
    fun `given product added with explicit products array, when built, then array items are mapped and top-level fields flow to metadata`() {
        val properties = buildJsonObject {
            put("cart_id", "C1")
            put("currency", "USD")
            put("product_id", "TOP")
            putJsonArray("products") {
                add(
                    buildJsonObject {
                        put("product_id", "P1")
                        put("name", "Shoe")
                        put("variant", "red")
                        put("quantity", 1)
                        put("price", 10)
                        put("color", "blue")
                    }
                )
            }
        }

        val result = build(properties, BrazeEcommerceEvents.CART_UPDATED, CartUpdatedAction.ADD)

        val product = result.products()[0].jsonObject
        assertEquals(JsonPrimitive("P1"), product["product_id"])
        assertEquals(JsonPrimitive("blue"), product.metadata()["color"])
        // With explicit products[], top-level product_id is unmapped → flows to event metadata.
        assertEquals(JsonPrimitive("TOP"), result.metadata()["product_id"])
    }

    @Test
    fun `given product added missing required currency, when built, then a warning is logged`() {
        val properties = buildJsonObject {
            put("cart_id", "C1")
            put("product_id", "P1")
            put("name", "Shoe")
            put("variant", "red")
            put("price", 10)
        }

        build(properties, BrazeEcommerceEvents.CART_UPDATED, CartUpdatedAction.ADD)

        verify { logger.warn(match { it.contains("currency") }) }
    }

    // endregion

    // region order events (iterate products)

    @Test
    fun `given order completed, when built, then total_value falls back to revenue and products are iterated`() {
        val properties = buildJsonObject {
            put("order_id", "O1")
            put("revenue", 199.0)
            put("currency", "USD")
            put("discounts", 5)
            putJsonArray("products") {
                add(
                    buildJsonObject {
                        put("product_id", "P1")
                        put("name", "Shoe")
                        put("variant", "red")
                        put("quantity", 1)
                        put("price", 199.0)
                        put("category", "apparel")
                    }
                )
            }
        }

        val result = build(properties, BrazeEcommerceEvents.ORDER_PLACED)

        assertEquals(JsonPrimitive("O1"), result["order_id"])
        assertEquals(JsonPrimitive(199.0), result["total_value"])
        assertEquals(JsonPrimitive(5), result["discounts"])
        val product = result.products()[0].jsonObject
        assertEquals(JsonPrimitive("P1"), product["product_id"])
        assertEquals(JsonPrimitive("apparel"), product.metadata()["category"])
    }

    @Test
    fun `given order refunded with no products, when built, then a warning about products is logged`() {
        val properties = buildJsonObject {
            put("order_id", "O1")
            put("total", 50.0)
            put("currency", "USD")
        }

        val result = build(properties, BrazeEcommerceEvents.ORDER_REFUNDED)

        assertFalse(result.containsKey("products"))
        verify { logger.warn(match { it.contains("products") }) }
    }

    @Test
    fun `given order cancelled without cancel_reason, when built, then a warning about cancel_reason is logged`() {
        val properties = buildJsonObject {
            put("order_id", "O1")
            put("total", 50.0)
            put("currency", "USD")
            putJsonArray("products") {
                add(
                    buildJsonObject {
                        put("product_id", "P1")
                        put("name", "Shoe")
                        put("variant", "red")
                        put("quantity", 1)
                        put("price", 50.0)
                    }
                )
            }
        }

        build(properties, BrazeEcommerceEvents.ORDER_CANCELLED)

        verify { logger.warn(match { it.contains("cancel_reason") }) }
    }

    @Test
    fun `given a malformed non-array products value, when built, then it flows to metadata instead of being dropped`() {
        val properties = buildJsonObject {
            put("order_id", "O1")
            put("total", 50.0)
            put("currency", "USD")
            put("products", "not-an-array") // malformed shape
        }

        val result = build(properties, BrazeEcommerceEvents.ORDER_REFUNDED)

        assertFalse(result.containsKey("products"))
        assertEquals(JsonPrimitive("not-an-array"), result.metadata()["products"])
    }

    @Test
    fun `given a products array containing a non-object element, when built, then the whole array flows to metadata`() {
        val malformedProducts = buildJsonArray {
            add(
                buildJsonObject {
                    put("product_id", "P1")
                    put("name", "Shoe")
                    put("variant", "red")
                    put("quantity", 1)
                    put("price", 10)
                }
            )
            add(JsonPrimitive("not-a-product"))
        }
        val properties = buildJsonObject {
            put("order_id", "O1")
            put("total", 50.0)
            put("currency", "USD")
            put("products", malformedProducts)
        }

        val result = build(properties, BrazeEcommerceEvents.ORDER_REFUNDED)

        assertFalse(result.containsKey("products"))
        assertEquals(malformedProducts, result.metadata()["products"])
    }

    @Test
    fun `given checkout started with checkout_id absent, when built, then order_id is used as fallback`() {
        val properties = buildJsonObject {
            put("order_id", "O1")
            put("total", 75.0)
            put("currency", "USD")
            putJsonArray("products") {
                add(
                    buildJsonObject {
                        put("product_id", "P1")
                        put("name", "Shoe")
                        put("variant", "red")
                        put("quantity", 1)
                        put("price", 75.0)
                    }
                )
            }
        }

        val result = build(properties, BrazeEcommerceEvents.CHECKOUT_STARTED)

        assertEquals(JsonPrimitive("O1"), result["checkout_id"])
    }

    // endregion

    // region send-anyway value semantics

    @Test
    fun `given a numeric zero value, when built, then it is treated as resolved`() {
        val properties = buildJsonObject {
            put("product_id", "P1")
            put("name", "Free")
            put("variant", "red")
            put("price", 0)
            put("currency", "USD")
        }

        val result = build(properties, BrazeEcommerceEvents.PRODUCT_VIEWED)

        assertEquals(JsonPrimitive(0), result["price"])
    }

    @Test
    fun `given an empty string for a required field, when built, then it counts as missing and warns`() {
        val properties = buildJsonObject {
            put("product_id", "P1")
            put("name", "Shoe")
            put("variant", "red")
            put("price", 10)
            put("currency", "")
        }

        val result = build(properties, BrazeEcommerceEvents.PRODUCT_VIEWED)

        assertFalse(result.containsKey("currency"))
        verify { logger.warn(match { it.contains("currency") }) }
    }

    @Test
    fun `given all required fields present, when built, then no warning is logged`() {
        val properties = buildJsonObject {
            put("product_id", "P1")
            put("name", "Shoe")
            put("variant", "red")
            put("price", 10)
            put("currency", "USD")
        }

        build(properties, BrazeEcommerceEvents.PRODUCT_VIEWED)

        verify(exactly = 0) { logger.warn(any()) }
    }

    // endregion

    // region type-mismatch warnings

    @Test
    fun `given a numeric float sent as a string, when built, then it is coerced to a number and no warning is logged`() {
        val properties = buildJsonObject {
            put("product_id", "P1")
            put("name", "Shoe")
            put("variant", "red")
            put("price", "29.99") // String that can be coerced to Float
            put("currency", "USD")
        }

        val result = build(properties, BrazeEcommerceEvents.PRODUCT_VIEWED)

        assertEquals(JsonPrimitive(29.99), result["price"])
        verify(exactly = 0) { logger.warn(match { it.contains("type-mismatched") }) }
    }

    @Test
    fun `given an integer for a float field, when built, then it is left as-is without a warning`() {
        val properties = buildJsonObject {
            put("product_id", "P1")
            put("name", "Shoe")
            put("variant", "red")
            put("price", 30) // Integer for a Float field — Braze accepts it, so no coercion
            put("currency", "USD")
        }

        val result = build(properties, BrazeEcommerceEvents.PRODUCT_VIEWED)

        assertEquals(JsonPrimitive(30), result["price"])
        verify(exactly = 0) { logger.warn(match { it.contains("type-mismatched") }) }
    }

    @Test
    fun `given a number for a string field, when built, then it is coerced to a string`() {
        val properties = buildJsonObject {
            put("product_id", 12345) // Number for a String field
            put("name", "Shoe")
            put("variant", "red")
            put("price", 10.0)
            put("currency", "USD")
        }

        val result = build(properties, BrazeEcommerceEvents.PRODUCT_VIEWED)

        assertEquals(JsonPrimitive("12345"), result["product_id"])
        verify(exactly = 0) { logger.warn(match { it.contains("type-mismatched") }) }
    }

    @Test
    fun `given a per-product quantity sent as a numeric string, when built, then it is coerced to an integer`() {
        val properties = buildJsonObject {
            put("order_id", "O1")
            put("total", 50.0)
            put("currency", "USD")
            putJsonArray("products") {
                add(
                    buildJsonObject {
                        put("product_id", "P1")
                        put("name", "Shoe")
                        put("variant", "red")
                        put("quantity", "2") // String that can be coerced to Integer
                        put("price", 50.0)
                    }
                )
            }
        }

        val result = build(properties, BrazeEcommerceEvents.ORDER_REFUNDED)

        assertEquals(JsonPrimitive(2), result.products()[0].jsonObject["quantity"])
        verify(exactly = 0) { logger.warn(match { it.contains("type-mismatched") }) }
    }

    @Test
    fun `given a non-numeric string for a float field, when built, then a warning is logged and value is sent as-is`() {
        val properties = buildJsonObject {
            put("product_id", "P1")
            put("name", "Shoe")
            put("variant", "red")
            put("price", "free") // cannot be coerced to Float
            put("currency", "USD")
        }

        val result = build(properties, BrazeEcommerceEvents.PRODUCT_VIEWED)

        assertEquals(JsonPrimitive("free"), result["price"])
        verify { logger.warn(match { it.contains("type-mismatched") && it.contains("price") }) }
    }

    @Test
    fun `given a per-product quantity sent as a float, when built, then a type-mismatch warning is logged`() {
        val properties = buildJsonObject {
            put("order_id", "O1")
            put("total", 50.0)
            put("currency", "USD")
            putJsonArray("products") {
                add(
                    buildJsonObject {
                        put("product_id", "P1")
                        put("name", "Shoe")
                        put("variant", "red")
                        put("quantity", 2.5) // wrong type: not an Integer
                        put("price", 50.0)
                    }
                )
            }
        }

        build(properties, BrazeEcommerceEvents.ORDER_REFUNDED)

        verify { logger.warn(match { it.contains("type-mismatched") && it.contains("products[].quantity") }) }
    }

    @Test
    fun `given the type field sent as a non-array, when built, then a type-mismatch warning is logged`() {
        val properties = buildJsonObject {
            put("product_id", "P1")
            put("name", "Shoe")
            put("variant", "red")
            put("price", 10)
            put("currency", "USD")
            put("type", "footwear") // wrong type: String instead of Array of strings
        }

        build(properties, BrazeEcommerceEvents.PRODUCT_VIEWED)

        verify { logger.warn(match { it.contains("type-mismatched") && it.contains("type") }) }
    }

    @Test
    fun `given correct field types including zero values, when built, then no type-mismatch warning is logged`() {
        val properties = buildJsonObject {
            put("product_id", "P1")
            put("name", "Shoe")
            put("variant", "red")
            put("price", 0) // valid Float
            put("currency", "USD")
        }

        build(properties, BrazeEcommerceEvents.PRODUCT_VIEWED)

        verify(exactly = 0) { logger.warn(match { it.contains("type-mismatched") }) }
    }

    // endregion
}
