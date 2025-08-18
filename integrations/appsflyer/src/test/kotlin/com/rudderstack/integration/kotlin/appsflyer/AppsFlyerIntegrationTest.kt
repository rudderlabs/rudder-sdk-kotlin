package com.rudderstack.integration.kotlin.appsflyer

import android.app.Application
import com.appsflyer.AFInAppEventParameterName
import com.appsflyer.AFInAppEventType
import com.appsflyer.AppsFlyerLib
import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceEvents
import com.rudderstack.sdk.kotlin.core.ecommerce.ECommerceParamNames
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AppsFlyerIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var mockAnalytics: Analytics

    @MockK
    private lateinit var mockAppsFlyerLib: AppsFlyerLib

    @MockK
    private lateinit var mockApplication: Application

    private lateinit var appsFlyerIntegration: AppsFlyerIntegration

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        mockAnalytics = mockAnalytics(testScope, testDispatcher)
        appsFlyerIntegration = spyk(AppsFlyerIntegration())
        
        val mockConfiguration = mockk<Configuration>(relaxed = true)
        every { mockConfiguration.application } returns mockApplication
        every { mockAnalytics.configuration } returns mockConfiguration
        every { appsFlyerIntegration.analytics } returns mockAnalytics

        appsFlyerIntegration.setup(mockAnalytics)

        every { appsFlyerIntegration.provideAppsFlyerInstance() } returns mockAppsFlyerLib
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given integration key, when key is accessed, then returns correct key`() {
        Assertions.assertEquals(APPSFLYER_KEY, appsFlyerIntegration.key)
    }

    @Test
    fun `given destination instance, when getDestinationInstance is called, then returns AppsFlyer instance`() {
        appsFlyerIntegration.setup(mockAnalytics)
        appsFlyerIntegration.create(emptyJsonObject)

        val instance = appsFlyerIntegration.getDestinationInstance()

        Assertions.assertEquals(mockAppsFlyerLib, instance)
    }

    @Test
    fun `given identify event with userId, when identify is called, then setCustomerUserId is called`() {
        appsFlyerIntegration.create(emptyJsonObject)
        val userId = "user123"
        val identifyEvent = IdentifyEvent()
        identifyEvent.userId = userId

        appsFlyerIntegration.identify(identifyEvent)

        verify { mockAppsFlyerLib.setCustomerUserId(userId) }
    }

    @Test
    fun `given identify event with email trait, when identify is called, then setUserEmails is called`() {
        appsFlyerIntegration.create(emptyJsonObject)
        val email = "test@example.com"
        val traits = buildJsonObject {
            put("email", email)
        }
        val identifyEvent = IdentifyEvent()
        identifyEvent.userId = "user123"
        every { mockAnalytics.traits } returns traits

        appsFlyerIntegration.identify(identifyEvent)

        verify { mockAppsFlyerLib.setUserEmails(email) }
    }

    @Test
    fun `given screen event with rich naming disabled, when screen is called, then simple screen name is used`() {
        val config = buildJsonObject { put("useRichEventName", false) }
        appsFlyerIntegration.create(config)

        val screenEvent = ScreenEvent("Home", emptyJsonObject)
        appsFlyerIntegration.screen(screenEvent)

        verify { mockAppsFlyerLib.logEvent(mockApplication, "screen", emptyMap()) }
    }

    @Test
    fun `given screen event with rich naming enabled, when screen is called, then rich screen name is used`() {
        val config = buildJsonObject { put("useRichEventName", true) }
        appsFlyerIntegration.create(config)

        val screenEvent = ScreenEvent("Home", emptyJsonObject)
        appsFlyerIntegration.screen(screenEvent)

        verify { mockAppsFlyerLib.logEvent(mockApplication, "Viewed Home Screen", emptyMap()) }
    }

    @Test
    fun `given ecommerce events, when track is called, then events are logged with AppsFlyer SDK`() {
        appsFlyerIntegration.create(emptyJsonObject)
        val properties = buildJsonObject {
            put(ECommerceParamNames.PRODUCT_ID, "prod123")
            put(ECommerceParamNames.PRICE, 29.99)
        }
        val trackEvent = TrackEvent(event = ECommerceEvents.PRODUCT_VIEWED, properties = properties)

        appsFlyerIntegration.track(trackEvent)

        verify { mockAppsFlyerLib.logEvent(mockApplication, AFInAppEventType.CONTENT_VIEW, mapOf(
            AFInAppEventParameterName.CONTENT_ID to "prod123",
            AFInAppEventParameterName.PRICE to 29.99
        )) }
    }

    @Test
    fun `given custom event, when track is called, then event is logged with AppsFlyer SDK using transformed name`() {
        appsFlyerIntegration.create(emptyJsonObject)
        val customEvent = "My Custom Event"
        val trackEvent = TrackEvent(event = customEvent, properties = emptyJsonObject)

        appsFlyerIntegration.track(trackEvent)

        verify { mockAppsFlyerLib.logEvent(mockApplication, "my_custom_event", emptyMap()) }
    }

    // ===== INTEGRATION BEHAVIOR TESTING =====

    @Test
    fun `given event with various data types, when track is called, then AppsFlyer SDK receives correct parameters`() {
        appsFlyerIntegration.create(emptyJsonObject)
        val properties = buildJsonObject {
            put("string_prop", "test_string")
            put("int_prop", 42)
            put("double_prop", 19.99)
            put("boolean_prop", true)
        }
        val trackEvent = TrackEvent(event = "test_event", properties = properties)

        appsFlyerIntegration.track(trackEvent)

        verify { mockAppsFlyerLib.logEvent(mockApplication, "test_event", mapOf(
            "string_prop" to "test_string",
            "int_prop" to 42,
            "double_prop" to 19.99,
            "boolean_prop" to true
        )) }
    }

    @Test
    fun `given event with complex nested data, when track is called, then AppsFlyer SDK receives flattened parameters`() {
        appsFlyerIntegration.create(emptyJsonObject)
        val properties = buildJsonObject {
            put("simple_array", buildJsonArray {
                add(1)
                add(2)
                add(3)
            })
            put("nested_object", buildJsonObject {
                put("inner_key", "inner_value")
                put("inner_number", 123)
            })
        }
        val trackEvent = TrackEvent(event = "complex_event", properties = properties)

        appsFlyerIntegration.track(trackEvent)

        verify { mockAppsFlyerLib.logEvent(mockApplication, "complex_event", mapOf(
            "simple_array" to listOf(1, 2, 3),
            "nested_object" to mapOf(
                "inner_key" to "inner_value",
                "inner_number" to 123
            )
        )) }
    }

    // ===== EDGE CASES TESTING =====

    @Test
    fun `given identify event with empty userId, when identify is called, then setCustomerUserId is not called`() {
        appsFlyerIntegration.create(emptyJsonObject)
        val identifyEvent = IdentifyEvent()
        identifyEvent.userId = ""

        appsFlyerIntegration.identify(identifyEvent)

        verify(exactly = 0) { mockAppsFlyerLib.setCustomerUserId(any()) }
    }

    @Test
    fun `given identify event with null email trait, when identify is called, then setUserEmails is not called`() {
        appsFlyerIntegration.create(emptyJsonObject)
        val identifyEvent = IdentifyEvent()
        identifyEvent.userId = "user123"
        every { mockAnalytics.traits } returns buildJsonObject {
            put("email", "")
        }

        appsFlyerIntegration.identify(identifyEvent)

        verify(exactly = 0) { mockAppsFlyerLib.setUserEmails(any<String>()) }
    }

    @Test
    fun `given screen event with empty name but property name, when screen is called with rich naming, then uses property name`() {
        val config = buildJsonObject { put("useRichEventName", true) }
        appsFlyerIntegration.create(config)

        val properties = buildJsonObject { put("name", "Settings") }
        val screenEvent = ScreenEvent("", properties)
        appsFlyerIntegration.screen(screenEvent)

        verify { mockAppsFlyerLib.logEvent(mockApplication, "Viewed Settings Screen", mapOf("name" to "Settings")) }
    }

    @Test
    fun `given screen event with no name and no property name, when screen is called with rich naming, then uses generic name`() {
        val config = buildJsonObject { put("useRichEventName", true) }
        appsFlyerIntegration.create(config)

        val screenEvent = ScreenEvent("", emptyJsonObject)
        appsFlyerIntegration.screen(screenEvent)

        verify { mockAppsFlyerLib.logEvent(mockApplication, "Viewed Screen", emptyMap()) }
    }

    @Test
    fun `given event with special characters in property names, when track is called, then AppsFlyer SDK receives properties as-is`() {
        appsFlyerIntegration.create(emptyJsonObject)
        val properties = buildJsonObject {
            put("key-with-hyphen", "hyphen_value")
            put("key_with_underscore", "underscore_value")
            put("key with spaces", "spaces_value")
        }
        val trackEvent = TrackEvent(event = "special_chars", properties = properties)

        appsFlyerIntegration.track(trackEvent)

        verify { mockAppsFlyerLib.logEvent(mockApplication, "special_chars", mapOf(
            "key-with-hyphen" to "hyphen_value",
            "key_with_underscore" to "underscore_value",
            "key with spaces" to "spaces_value"
        )) }
    }

    // ===== CONFIGURATION TESTING =====

    @Test
    fun `given new configuration, when update is called, then configuration is updated without re-creating instance`() {
        appsFlyerIntegration.create(buildJsonObject { put("useRichEventName", false) })
        val originalInstance = appsFlyerIntegration.getDestinationInstance()

        appsFlyerIntegration.update(buildJsonObject { put("useRichEventName", true) })

        // Verify instance is the same
        Assertions.assertEquals(originalInstance, appsFlyerIntegration.getDestinationInstance())

        // Verify new configuration is applied
        val screenEvent = ScreenEvent("Home", emptyJsonObject)
        appsFlyerIntegration.screen(screenEvent)
        verify { mockAppsFlyerLib.logEvent(mockApplication, "Viewed Home Screen", emptyMap()) }
    }

    @Test
    fun `given missing configuration values, when create is called, then defaults are used`() {
        appsFlyerIntegration.create(emptyJsonObject) // No useRichEventName specified

        val screenEvent = ScreenEvent("Home", emptyJsonObject)
        appsFlyerIntegration.screen(screenEvent)

        // Should use default (simple naming)
        verify { mockAppsFlyerLib.logEvent(mockApplication, "screen", emptyMap()) }
    }

    // ===== ERROR HANDLING =====

    @Test
    fun `given AppsFlyer instance is null, when operations are called, then no exceptions are thrown`() {
        every { appsFlyerIntegration.provideAppsFlyerInstance() } returns null
        appsFlyerIntegration.create(emptyJsonObject)

        // These should not throw exceptions
        appsFlyerIntegration.identify(IdentifyEvent().apply { userId = "test" })
        appsFlyerIntegration.track(TrackEvent("test", emptyJsonObject))
        appsFlyerIntegration.screen(ScreenEvent("test", emptyJsonObject))

        // Verify no calls were made to null instance
        verify(exactly = 0) { mockAppsFlyerLib.setCustomerUserId(any()) }
        verify(exactly = 0) { mockAppsFlyerLib.logEvent(any(), any(), any<MutableMap<String, Any>>()) }
    }

    // ===== PROPERTY FILTERING INTEGRATION TESTING =====

    @Test
    fun `given event with mixed reserved and custom properties, when track is called, then only custom properties are sent to AppsFlyer SDK`() {
        appsFlyerIntegration.create(emptyJsonObject)
        val properties = buildJsonObject {
            put(ECommerceParamNames.PRICE, "29.99") // reserved
            put(ECommerceParamNames.CURRENCY, "USD") // reserved
            put("custom_prop1", "value1") // custom
            put("custom_prop2", "value2") // custom
        }
        val trackEvent = TrackEvent(event = "mixed_props", properties = properties)

        appsFlyerIntegration.track(trackEvent)

        verify { mockAppsFlyerLib.logEvent(mockApplication, "mixed_props", mapOf(
            "custom_prop1" to "value1",
            "custom_prop2" to "value2"
        )) }
    }

    // ===== INTEGRATION FLOW TESTING =====

    @Test
    fun `given multiple events in sequence, when called, then all events are processed correctly`() {
        appsFlyerIntegration.create(emptyJsonObject)

        // Identify user
        val identifyEvent = IdentifyEvent().apply { userId = "user123" }
        appsFlyerIntegration.identify(identifyEvent)

        // Track event
        val trackEvent = TrackEvent("purchase", buildJsonObject { put("amount", "99.99") })
        appsFlyerIntegration.track(trackEvent)

        // Screen event
        val screenEvent = ScreenEvent("Checkout", emptyJsonObject)
        appsFlyerIntegration.screen(screenEvent)

        // Verify all calls were made
        verify { mockAppsFlyerLib.setCustomerUserId("user123") }
        verify { mockAppsFlyerLib.logEvent(mockApplication, "purchase", mapOf(
            "amount" to "99.99"
        )) }
        verify { mockAppsFlyerLib.logEvent(mockApplication, "screen", emptyMap()) }
    }
}
