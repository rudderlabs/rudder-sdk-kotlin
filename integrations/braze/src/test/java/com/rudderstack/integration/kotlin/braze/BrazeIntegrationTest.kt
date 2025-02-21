package com.rudderstack.integration.kotlin.braze

import android.app.Application
import com.braze.Braze
import com.braze.configuration.BrazeConfig
import com.braze.models.outgoing.BrazeProperties
import com.rudderstack.integration.kotlin.braze.Utility.getCampaignObject
import com.rudderstack.integration.kotlin.braze.Utility.getCustomProperties
import com.rudderstack.integration.kotlin.braze.Utility.getOrderCompletedProperties
import com.rudderstack.integration.kotlin.braze.Utility.provideTrackEvent
import com.rudderstack.integration.kotlin.braze.Utility.readFileAsJsonObject
import com.rudderstack.sdk.kotlin.android.utils.application
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
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
import kotlinx.serialization.json.JsonObject
import org.junit.Before
import org.junit.Test
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
            mockBrazeInstance.logPurchase(
                productId = "10011",
                currencyCode = "USD",
                price = BigDecimal("100.11"),
                properties = any<BrazeProperties>()
            )
            mockBrazeInstance.logPurchase(
                productId = "20022",
                currencyCode = "USD",
                price = BigDecimal("200.22"),
                properties = any<BrazeProperties>()
            )
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
            mockBrazeInstance.logPurchase(
                productId = any(),
                currencyCode = any(),
                price = any(),
                properties = any<BrazeProperties>()
            )
        }
    }
}
