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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.AfterEach
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
        assert(appsFlyerIntegration.key == APPSFLYER_KEY)
    }

    @Test
    fun `given destination instance, when getDestinationInstance is called, then returns AppsFlyer instance`() {
        appsFlyerIntegration.setup(mockAnalytics)
        appsFlyerIntegration.create(emptyJsonObject)

        val instance = appsFlyerIntegration.getDestinationInstance()

        assert(instance == mockAppsFlyerLib)
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

        verify { mockAppsFlyerLib.logEvent(mockApplication, "screen", any<MutableMap<String, Any>>()) }
    }

    @Test
    fun `given screen event with rich naming enabled, when screen is called, then rich screen name is used`() {
        val config = buildJsonObject { put("useRichEventName", true) }
        appsFlyerIntegration.create(config)

        val screenEvent = ScreenEvent("Home", emptyJsonObject)
        appsFlyerIntegration.screen(screenEvent)

        verify { mockAppsFlyerLib.logEvent(mockApplication, "Viewed Home Screen", any<MutableMap<String, Any>>()) }
    }

    @Test
    fun `given product viewed event, when track is called, then correct AppsFlyer parameters are set`() {
        appsFlyerIntegration.create(emptyJsonObject)
        val properties = buildJsonObject {
            put(ECommerceParamNames.PRODUCT_ID, "prod123")
            put(ECommerceParamNames.PRICE, "29.99")
            put(ECommerceParamNames.CATEGORY, "Electronics")
            put(ECommerceParamNames.CURRENCY, "USD")
        }
        val trackEvent = TrackEvent(event = ECommerceEvents.PRODUCT_VIEWED, properties = properties)

        appsFlyerIntegration.track(trackEvent)

        verify { 
            mockAppsFlyerLib.logEvent(
                mockApplication, 
                AFInAppEventType.CONTENT_VIEW, 
                match<MutableMap<String, Any>> { params ->
                    params[AFInAppEventParameterName.CONTENT_ID] == "prod123" &&
                    params[AFInAppEventParameterName.PRICE] == "29.99" &&
                    params[AFInAppEventParameterName.CONTENT_TYPE] == "Electronics" &&
                    params[AFInAppEventParameterName.CURRENCY] == "USD"
                }
            ) 
        }
    }

    @Test
    fun `given order completed event, when track is called, then order parameters are set correctly`() {
        appsFlyerIntegration.create(emptyJsonObject)
        val properties = buildJsonObject {
            put(ECommerceParamNames.ORDER_ID, "order123")
            put(ECommerceParamNames.TOTAL, "99.99")
            put(ECommerceParamNames.REVENUE, "89.99")
            put(ECommerceParamNames.CURRENCY, "USD")
        }
        val trackEvent = TrackEvent(event = ECommerceEvents.ORDER_COMPLETED, properties = properties)

        appsFlyerIntegration.track(trackEvent)

        verify { 
            mockAppsFlyerLib.logEvent(
                mockApplication, 
                AFInAppEventType.PURCHASE, 
                match<MutableMap<String, Any>> { params ->
                    params[AFInAppEventParameterName.RECEIPT_ID] == "order123" &&
                    params["af_order_id"] == "order123" &&
                    params[AFInAppEventParameterName.PRICE] == "99.99" &&
                    params[AFInAppEventParameterName.REVENUE] == "89.99" &&
                    params[AFInAppEventParameterName.CURRENCY] == "USD"
                }
            ) 
        }
    }

    @Test
    fun `given products searched event, when track is called, then search string is set`() {
        appsFlyerIntegration.create(emptyJsonObject)
        val properties = buildJsonObject {
            put(ECommerceParamNames.QUERY, "laptop")
        }
        val trackEvent = TrackEvent(event = ECommerceEvents.PRODUCTS_SEARCHED, properties = properties)

        appsFlyerIntegration.track(trackEvent)

        verify { 
            mockAppsFlyerLib.logEvent(
                mockApplication, 
                AFInAppEventType.SEARCH, 
                match<MutableMap<String, Any>> { params ->
                    params[AFInAppEventParameterName.SEARCH_STRING] == "laptop"
                }
            ) 
        }
    }

    @Test
    fun `given custom event, when track is called, then event name is converted to lowercase with underscores`() {
        appsFlyerIntegration.create(emptyJsonObject)
        val customEvent = "My Custom Event"
        val trackEvent = TrackEvent(event = customEvent, properties = emptyJsonObject)

        appsFlyerIntegration.track(trackEvent)

        verify { mockAppsFlyerLib.logEvent(mockApplication, "my_custom_event", any<MutableMap<String, Any>>()) }
    }
}
