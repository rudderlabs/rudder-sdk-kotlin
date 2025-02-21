package com.rudderstack.integration.kotlin.braze

import android.app.Application
import com.braze.Braze
import com.braze.configuration.BrazeConfig
import com.braze.models.outgoing.BrazeProperties
import com.rudderstack.sdk.kotlin.android.utils.application
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.math.BigDecimal

private const val pathToBrazeConfig = "config/braze_config.json"
//private const val pathToNewBrazeConfig = "config/new_braze_config.json"

private const val INSTALL_ATTRIBUTED = "Install Attributed"

private const val CUSTOM_TRACK_EVENT = "Custom Track Event"

private const val ORDER_COMPLETED = "Order Completed"

class BrazeIntegrationTest {

    private val mockBrazeIntegrationConfig: JsonObject = readFileAsJsonObject(pathToBrazeConfig)

    @MockK
    private lateinit var mockAnalytics: Analytics

    @MockK
    private lateinit var mockApplication: Application

    @MockK
    private lateinit var mockBrazeInstance: Braze

    @MockK
    private lateinit var mockBrazeConfigBuilder: BrazeConfig.Builder

    private lateinit var brazeIntegration: BrazeIntegration

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        // Mock Analytics
        every { mockAnalytics.application } returns mockApplication

        mockkStatic(::initBrazeConfig)
        every { initBrazeConfig() } returns mockBrazeConfigBuilder

        // LogLevel
        every { mockAnalytics.configuration.logLevel } returns Logger.LogLevel.VERBOSE

        // Braze
        mockkObject(Braze)
        every { Braze.configure(any(), any()) } returns true
        every { Braze.getInstance(any()) } returns mockBrazeInstance
        every { mockBrazeConfigBuilder.setApiKey(any()) } returns mockBrazeConfigBuilder
        every { mockBrazeConfigBuilder.setCustomEndpoint(any()) } returns mockBrazeConfigBuilder

        // Initialize BrazeIntegration
        brazeIntegration = BrazeIntegration().also { it.analytics = mockAnalytics }
    }

    @Test
    fun `given integration initialisation attempt has not made, when instance is requested, then null is returned`() {
        val actualAdjustInstance = brazeIntegration.getDestinationInstance()

        assertNull(actualAdjustInstance)
    }

    @Test
    fun `given integration is initialised, when instance is requested, then instance is returned`() {
        brazeIntegration.create(mockBrazeIntegrationConfig)

        val actualBrazeInstance = brazeIntegration.getDestinationInstance()

        assertEquals(mockBrazeInstance, actualBrazeInstance)
        verify(exactly = 1) {
            mockBrazeConfigBuilder.setApiKey(any())
            mockBrazeConfigBuilder.setCustomEndpoint(any())
            Braze.configure(mockApplication, mockBrazeConfigBuilder.build())
            Braze.getInstance(mockApplication)
        }
    }

    @Test
    fun `given integration is initialised, when integration is re-initialised, then the same instance is returned`() {
        // Initialise the integration for the first time
        brazeIntegration.create(mockBrazeIntegrationConfig)
        val oldBrazeInstance = brazeIntegration.getDestinationInstance()
        // Simulate the re-initialisation of the integration by returning a new AdjustInstance
        val mockNewBrazeInstance: Braze = mockk()
        every { Braze.getInstance(any()) } returns mockNewBrazeInstance

        // Re-initialise the integration
        brazeIntegration.create(mockBrazeIntegrationConfig)
        val newBrazeInstance = brazeIntegration.getDestinationInstance()

        assertEquals(mockBrazeInstance, oldBrazeInstance)
        assertEquals(mockBrazeInstance, newBrazeInstance)
    }

    @Test
    fun `given the event is Install Attributed and campaign object is present, when it is made, then the attribution data is tracked`() {
        brazeIntegration.create(mockBrazeIntegrationConfig)
        val trackEvent = provideTrackEvent(
            eventName = INSTALL_ATTRIBUTED,
            properties = getCampaignObject(),
        )

        brazeIntegration.track(trackEvent)

        verify { mockBrazeInstance.currentUser?.setAttributionData(any()) }
    }

    @Test
    fun `given the event is Install Attributed and campaign object is not present, when it is made, then it is logged as a custom event`() {
        brazeIntegration.create(mockBrazeIntegrationConfig)
        val trackEvent = provideTrackEvent(eventName = INSTALL_ATTRIBUTED)

        brazeIntegration.track(trackEvent)

        verify { mockBrazeInstance.logCustomEvent(INSTALL_ATTRIBUTED) }
    }

    @Test
    fun `given the event is custom event without properties, when it is made, then it is logged as a custom event`() {
        brazeIntegration.create(mockBrazeIntegrationConfig)
        val trackEvent = provideTrackEvent(eventName = CUSTOM_TRACK_EVENT)

        brazeIntegration.track(trackEvent)

        verify { mockBrazeInstance.logCustomEvent(CUSTOM_TRACK_EVENT) }
    }

    @Test
    fun `given the event is custom event with properties, when it is made, then it is logged as a custom event with properties`() {
        brazeIntegration.create(mockBrazeIntegrationConfig)
        val trackEvent = provideTrackEvent(
            eventName = CUSTOM_TRACK_EVENT,
            properties = getCustomProperties(),
        )

        brazeIntegration.track(trackEvent)

        verify { mockBrazeInstance.logCustomEvent(CUSTOM_TRACK_EVENT, any<BrazeProperties>()) }
    }

    @Test
    fun `given the event is Order Completed and with products, when it is made, then it is logged as a custom event with products`() {
        brazeIntegration.create(mockBrazeIntegrationConfig)
        val trackEvent = provideTrackEvent(
            eventName = ORDER_COMPLETED,
            properties = getOrderCompletedProperties(),
        )
        mockkStatic(::toBrazeProperties)
        val customPropertiesSlot = slot<JsonObject>()
        every { toBrazeProperties(capture(customPropertiesSlot)) } returns BrazeProperties()

        brazeIntegration.track(trackEvent)

        verify(exactly = 1) {
            mockBrazeInstance.logPurchase(productId = "10011", currencyCode = "USD", price = BigDecimal("100.11"), properties = any<BrazeProperties>())
            mockBrazeInstance.logPurchase(productId = "20022", currencyCode = "USD", price = BigDecimal("200.22"), properties = any<BrazeProperties>())
        }
        // We are unable to make a proper assertion on BrazeProperties() object. Therefore, we used the following workaround
        assertEquals(getCustomProperties(), customPropertiesSlot.captured)
    }

    @Test
    fun `given the event is Order Completed and without products, when it is made, then no event is logged`() {
        brazeIntegration.create(mockBrazeIntegrationConfig)
        val trackEvent = provideTrackEvent(
            eventName = ORDER_COMPLETED,
            properties = getCustomProperties(),
        )

        brazeIntegration.track(trackEvent)

        verify(exactly = 0) {
            mockBrazeInstance.logPurchase(productId = any(), currencyCode = any(), price = any(), properties = any<BrazeProperties>())
        }
    }
}

private fun Any.readFileAsJsonObject(fileName: String): JsonObject {
    this::class.java.classLoader?.getResourceAsStream(fileName).let { inputStream ->
        inputStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
    }.let { fileAsString ->
        return Json.parseToJsonElement(fileAsString).jsonObject
    }
}

@OptIn(InternalRudderApi::class)
private fun provideTrackEvent(
    eventName: String,
    properties: JsonObject = JsonObject(emptyMap()),
) = TrackEvent(
    event = eventName,
    properties = properties,
    options = RudderOption(),
).also {
    it.applyMockedValues()
    it.updateData(PlatformType.Mobile)
}

private fun Event.applyMockedValues() {
    this.originalTimestamp = "<original-timestamp>"
    this.context = emptyJsonObject
    this.messageId = "<message-id>"
}

private fun getCampaignObject(): JsonObject = buildJsonObject {
    put("campaign", buildJsonObject {
        put("source", "Source value")
        put("name", "Name value")
        put("ad_group", "ad_group value")
        put("ad_creative", "ad_creative value")
    })
}

private fun getCustomProperties(): JsonObject = buildJsonObject {
    put("key1", "value1")
    put("key2", "value2")
    put("Product-Key-1", "Product-Value-1")
    put("Product-Key-2", "Product-Value-2")
}

private fun getOrderCompletedProperties(): JsonObject = buildJsonObject {
    put("key1", "value1")
    put("key2", "value2")
    put(
        "products", buildJsonArray {
            add(buildJsonObject {
                put("product_id", "10011")
                put("price", 100.11)
                put("Product-Key-1", "Product-Value-1")
            })
            add(buildJsonObject {
                put("product_id", "20022")
                put("price", 200.22)
                put("Product-Key-2", "Product-Value-2")
            })
        }
    )
}
