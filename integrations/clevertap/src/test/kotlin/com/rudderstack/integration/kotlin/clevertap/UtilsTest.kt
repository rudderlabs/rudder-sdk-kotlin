package com.rudderstack.integration.kotlin.clevertap

import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.UserIdentity
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UtilsTest {

    @Nested
    inner class ConfigParsing {

        @Test
        fun `given valid config json, when parse config is called, then config is decoded`() {
            val config = readFileAsJsonObject("config/clevertap_config.json")
                .parseConfig<CleverTapDestinationConfig>(mockk<Logger>(relaxed = true))

            assertEquals("TEST-ACCOUNT-ID", config?.accountId)
            assertEquals("TEST-ACCOUNT-TOKEN", config?.accountToken)
            assertEquals("in1", config?.region)
        }

        @Test
        fun `given empty config json, when parse config is called, then null is returned`() {
            val config = emptyJsonObject.parseConfig<CleverTapDestinationConfig>(mockk<Logger>(relaxed = true))

            assertEquals(null, config)
        }
    }

    @Nested
    inner class ValueConversion {

        @Test
        fun `when toAnyMap converts a JsonObject with mixed primitives, then types are preserved`() {
            val input = buildJsonObject {
                put("str", "hello")
                put("int", 42)
                put("long", 4_200_000_000)
                put("bool", true)
                put("dbl", 3.14)
                put("null", JsonNull)
                put("nested", buildJsonObject { put("child", "value") })
                put("array", buildJsonArray {
                    add("one")
                    add(2)
                    add(JsonNull)
                })
            }

            val result = input.toAnyMap()

            assertEquals("hello", result["str"])
            assertEquals(42, result["int"])
            assertEquals(4_200_000_000, result["long"])
            assertEquals(true, result["bool"])
            assertEquals(3.14, result["dbl"])
            assertFalse(result.containsKey("null"))
            assertEquals(mapOf("child" to "value"), result["nested"])
            assertEquals(listOf("one", 2), result["array"])
        }
    }

    @Nested
    inner class TraitMapping {

        @Test
        fun `given rudder traits, when profile is built, then CleverTap trait names are used`() {
            val traits = buildJsonObject {
                put("id", "user-123")
                put("name", "Jane")
                put("phone", "+15551234567")
                put("email", "jane@example.com")
                put("gender", "male")
                put("birthday", "1990-01-02")
                put("company", buildJsonObject {
                    put("id", "company-1")
                    put("name", "Acme")
                    put("role", "buyer")
                })
            }

            val profile = provideIdentifyEvent(traits).toCleverTapProfile(mockk(relaxed = true))

            assertEquals("user-123", profile["Identity"])
            assertEquals("Jane", profile["Name"])
            assertEquals("+15551234567", profile["Phone"])
            assertEquals("jane@example.com", profile["Email"])
            assertEquals("<anonymousId>", profile["anonymousId"])
            assertEquals("M", profile["Gender"])
            assertEquals("company-1", profile["companyId"])
            assertEquals("Acme", profile["companyName"])
            assertEquals("buyer", profile["role"])
            assertNotNull(profile["DOB"])
        }

        @Test
        fun `given identify event has user id without id trait, when profile is built, then user id is used as Identity`() {
            val traits = buildJsonObject {
                put("name", "Jane")
                put("email", "jane@example.com")
            }

            val profile = provideIdentifyEvent(traits = traits).toCleverTapProfile(mockk(relaxed = true))

            assertEquals("test-user", profile["Identity"])
            assertEquals("Jane", profile["Name"])
            assertEquals("jane@example.com", profile["Email"])
        }

        @Test
        fun `given traits include explicit anonymousId, when profile is built, then event anonymousId does not overwrite it`() {
            val traits = buildJsonObject {
                put("anonymousId", "explicit-anonymous-id")
            }

            val profile = provideIdentifyEvent(traits = traits).toCleverTapProfile(mockk(relaxed = true))

            assertEquals("explicit-anonymous-id", profile["anonymousId"])
        }

        @Test
        fun `given address and company traits are not objects, when profile is built, then original values are preserved`() {
            val traits = buildJsonObject {
                put("address", "123 Main St")
                put("company", buildJsonArray {
                    add("Acme")
                    add("RudderStack")
                })
            }

            val profile = provideIdentifyEvent(traits = traits).toCleverTapProfile(mockk(relaxed = true))

            assertEquals("123 Main St", profile["address"])
            assertEquals(listOf("Acme", "RudderStack"), profile["company"])
        }

        @Test
        fun `given invalid birthday, when profile is built, then DOB is dropped and warning is logged`() {
            val logger = mockk<Logger>(relaxed = true)
            val traits = buildJsonObject {
                put("birthday", "1990-13-45")
            }

            val profile = provideIdentifyEvent(traits = traits).toCleverTapProfile(logger)

            assertFalse(profile.containsKey("birthday"))
            assertNull(profile["DOB"])
            verify {
                logger.warn("CleverTapIntegration: Cannot parse birthday '1990-13-45'. Expected format yyyy-MM-dd.")
            }
        }
    }

    @Nested
    inner class EventMapping {

        private val mockLogger = mockk<Logger>(relaxed = true)

        @Test
        fun `given order completed properties, when track event is built, then charged event fields are mapped`() {
            val properties = buildJsonObject {
                put("order_id", "order-1")
                put("revenue", 25.5)
                put("products", buildJsonArray {
                    add(buildJsonObject {
                        put("product_id", "sku-1")
                        put("name", "Hat")
                    })
                })
            }

            val event = properties.toCleverTapTrackEvent("Order Completed", mockLogger) as CleverTapTrackEvent.ChargedEvent

            assertEquals("order-1", event.chargeDetails["Charged ID"])
            assertEquals(25.5, event.chargeDetails["Amount"])
            assertEquals("sku-1", event.items.first()["id"])
            assertEquals("Hat", event.items.first()["name"])
        }

        @Test
        fun `given order completed has missing products and non-numeric revenue, when track event is built, then amount is omitted`() {
            val properties = buildJsonObject {
                put("order_id", "order-1")
                put("revenue", "not-a-number")
            }

            val event = properties.toCleverTapTrackEvent("Order Completed", mockLogger) as CleverTapTrackEvent.ChargedEvent

            assertEquals("order-1", event.chargeDetails["Charged ID"])
            assertFalse(event.chargeDetails.containsKey("Amount"))
            assertEquals(emptyList<HashMap<String, Any>>(), event.items)
            verify { mockLogger.warn(match { it.contains("Cannot parse revenue") }) }
        }

        @Test
        fun `given order completed has a numeric revenue string, when track event is built, then amount is mapped`() {
            val properties = buildJsonObject { put("revenue", "42.50") }

            val event = properties.toCleverTapTrackEvent("Order Completed", mockLogger) as CleverTapTrackEvent.ChargedEvent

            assertEquals(42.5, event.chargeDetails["Amount"])
            verify(exactly = 0) { mockLogger.warn(any()) }
        }

        @Test
        fun `given order completed has no revenue, when track event is built, then amount is absent and nothing is logged`() {
            val properties = buildJsonObject { put("order_id", "order-1") }

            val event = properties.toCleverTapTrackEvent("Order Completed", mockLogger) as CleverTapTrackEvent.ChargedEvent

            assertFalse(event.chargeDetails.containsKey("Amount"))
            verify(exactly = 0) { mockLogger.warn(any()) }
        }

        @Test
        fun `given order completed has a null revenue, when track event is built, then amount is omitted`() {
            val properties = buildJsonObject { put("revenue", JsonNull) }

            val event = properties.toCleverTapTrackEvent("Order Completed", mockLogger) as CleverTapTrackEvent.ChargedEvent

            assertFalse(event.chargeDetails.containsKey("Amount"))
            verify { mockLogger.warn(match { it.contains("Cannot parse revenue") }) }
        }

        @Test
        fun `given order completed has a boolean revenue, when track event is built, then amount is omitted`() {
            val properties = buildJsonObject { put("revenue", true) }

            val event = properties.toCleverTapTrackEvent("Order Completed", mockLogger) as CleverTapTrackEvent.ChargedEvent

            assertFalse(event.chargeDetails.containsKey("Amount"))
            verify { mockLogger.warn(match { it.contains("Cannot parse revenue") }) }
        }

        @Test
        fun `given order completed has an object revenue, when track event is built, then amount is omitted`() {
            val properties = buildJsonObject {
                put("revenue", buildJsonObject { put("value", 10) })
            }

            val event = properties.toCleverTapTrackEvent("Order Completed", mockLogger) as CleverTapTrackEvent.ChargedEvent

            assertFalse(event.chargeDetails.containsKey("Amount"))
            verify { mockLogger.warn(match { it.contains("Cannot parse revenue") }) }
        }

        @Test
        fun `given order completed has empty products, when track event is built, then no items are sent`() {
            val properties = buildJsonObject {
                put("products", buildJsonArray {})
            }

            val event = properties.toCleverTapTrackEvent("Order Completed", mockLogger) as CleverTapTrackEvent.ChargedEvent

            assertEquals(emptyList<HashMap<String, Any>>(), event.items)
        }

        @Test
        fun `given order completed has malformed products, when track event is built, then invalid items are ignored`() {
            val properties = buildJsonObject {
                put("products", buildJsonArray {
                    add("bad-product")
                    add(buildJsonObject { put("product_id", "sku-1") })
                })
            }

            val event = properties.toCleverTapTrackEvent("Order Completed", mockLogger) as CleverTapTrackEvent.ChargedEvent

            assertEquals(1, event.items.size)
            assertEquals("sku-1", event.items.first()["id"])
        }

        @Test
        fun `given custom properties, when screen event is built, then screen viewed event name is used`() {
            val event = buildJsonObject { put("section", "hero") }.toCleverTapScreenEvent("Home")

            assertEquals("Screen Viewed: Home", event.eventName)
            assertEquals(mapOf("section" to "hero"), event.properties)
        }
    }

    @OptIn(InternalRudderApi::class)
    private fun provideIdentifyEvent(traits: kotlinx.serialization.json.JsonObject) = IdentifyEvent(
        options = RudderOption(),
        userIdentityState = UserIdentity(
            anonymousId = "<anonymousId>",
            userId = "test-user",
            traits = traits,
        ),
    ).also {
        it.context = buildJsonObject { put("traits", traits) }
        it.updateData(PlatformType.Mobile)
    }
}
