package com.rudderstack.integration.kotlin.braze

import android.app.Application
import com.braze.Braze
import com.braze.configuration.BrazeConfig
import com.braze.enums.Month
import com.braze.models.outgoing.BrazeProperties
import com.rudderstack.integration.kotlin.braze.Utility.getCampaignObject
import com.rudderstack.integration.kotlin.braze.Utility.getCustomProperties
import com.rudderstack.integration.kotlin.braze.Utility.getOrderCompletedProperties
import com.rudderstack.integration.kotlin.braze.Utility.getSlightDifferentStandardAndCustomTraits
import com.rudderstack.integration.kotlin.braze.Utility.provideIdentifyEvent
import com.rudderstack.integration.kotlin.braze.Utility.provideTrackEvent
import com.rudderstack.integration.kotlin.braze.Utility.provideUserIdentity
import com.rudderstack.integration.kotlin.braze.Utility.readFileAsJsonObject
import com.rudderstack.sdk.kotlin.android.utils.application
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import io.mockk.MockKAnnotations
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

private const val pathToBrazeConfig = "config/braze_config.json"
private const val pathToBrazeConfigWithDeDupeDisabled = "config/braze_config_with_deDupe_disabled.json"
private const val pathToNewBrazeConfig = "config/new_braze_config.json"
private const val pathToBrazeConfigWithAndroidAppIdentifierKey = "config/braze_config_with_android_app_identifier_key.json"
private const val pathToBrazeConfigWithFlagDisabled = "config/braze_config_with_flag_disabled.json"
private const val pathToBrazeConfigWithBlankAndroidAppIdentifierKey = "config/braze_config_with_blank_android_app_identifier_key.json"

private const val INSTALL_ATTRIBUTED = "Install Attributed"

private const val CUSTOM_TRACK_EVENT = "Custom Track Event"

private const val ORDER_COMPLETED = "Order Completed"

class BrazeIntegrationTest {

    private val mockBrazeIntegrationConfig: JsonObject = readFileAsJsonObject(pathToBrazeConfig)
    private val mockBrazeIntegrationConfigWithDeDupeDisabled: JsonObject =
        readFileAsJsonObject(pathToBrazeConfigWithDeDupeDisabled)
    private val mockNewBrazeIntegrationConfig: JsonObject = readFileAsJsonObject(pathToNewBrazeConfig)

    @MockK
    private lateinit var mockAnalytics: Analytics

    @MockK
    private lateinit var mockApplication: Application

    @MockK
    private lateinit var mockBrazeInstance: Braze

    @MockK
    private lateinit var mockBrazeConfigBuilder: BrazeConfig.Builder

    private lateinit var brazeIntegration: BrazeIntegration

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        // Mock Analytics
        every { mockAnalytics.application } returns mockApplication

        mockkStatic(::initBrazeConfig)
        every { initBrazeConfig() } returns mockBrazeConfigBuilder

        // LogLevel
        mockkObject(LoggerAnalytics)
        every { LoggerAnalytics.logLevel } returns Logger.LogLevel.VERBOSE

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
    fun `when integration is initialised, then alias id is set`() {
        brazeIntegration.create(mockBrazeIntegrationConfig)

        mockBrazeInstance.currentUser?.apply {
            verify(exactly = 1) {
                addAlias(any(), ALIAS_LABEL)
            }
        }
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
    fun `given the event is Order Completed and with products, when it is made, then multiple purchase events are made`() {
        brazeIntegration.create(mockBrazeIntegrationConfig)
        val trackEvent = provideTrackEvent(
            eventName = ORDER_COMPLETED,
            properties = getOrderCompletedProperties(),
        )
        mockkStatic(::initBrazeProperties)
        val customPropertiesSlot = slot<JsonObject>()
        every { initBrazeProperties(capture(customPropertiesSlot)) } returns BrazeProperties(getCustomProperties().toJSONObject())

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

    @Test
    fun `given the event is identify and it contain all the traits, when the event is made, then preferred ID and all traits should be set`() {
        brazeIntegration.create(mockBrazeIntegrationConfig)
        val identifyEvent = provideIdentifyEvent()

        brazeIntegration.identify(identifyEvent)

        verifyTraits()
    }

    @Test
    fun `when the flush event is made, then the data is flushed`() {
        brazeIntegration.create(mockBrazeIntegrationConfig)
        brazeIntegration.flush()

        verify(exactly = 1) {
            mockBrazeInstance.requestImmediateDataFlush()
        }
    }

    @Test
    fun `given deDupe is enabled and previously an Identify event has been made, when a new Identify event is made with exactly same trait, then nothing should be set after the second identify event`() {
        // mockBrazeIntegrationConfig has deDupe enabled.
        brazeIntegration.create(mockBrazeIntegrationConfig)
        val identifyEvent = provideIdentifyEvent()
        // Making first identify event. It should set all the traits.
        brazeIntegration.identify(identifyEvent)
        // Clearing the mocks to reset the state.
        clearMocks(mockBrazeInstance)

        // Making second identify event with exactly same traits. It should not set any trait.
        brazeIntegration.identify(identifyEvent)

        // No trait should be set.
        verifyNullTraitsSet()
    }

    @Test
    fun `given deDupe is enabled and previously an Identify event has been made, when a new Identify event is made with slight different traits, then only different traits should be set after the second identify event`() {
        // mockBrazeIntegrationConfig has deDupe enabled.
        brazeIntegration.create(mockBrazeIntegrationConfig)
        val identifyEvent = provideIdentifyEvent()
        // Making first identify event. It should set all the traits.
        brazeIntegration.identify(identifyEvent)
        // Clearing the mocks to reset the state.
        clearMocks(mockBrazeInstance)

        val identifyEventWitDifferentTraits = provideIdentifyEvent(
            userIdentityState = provideUserIdentity(
                traits = getSlightDifferentStandardAndCustomTraits(),
            ),
        )
        // Making second identify event with exactly same traits. It should not set any trait.
        brazeIntegration.identify(identifyEventWitDifferentTraits)

        // No trait should be set.
        mockBrazeInstance.currentUser?.apply {
            verify(exactly = 1) {
                setGender(GENDER_FEMALE)
                setCustomUserAttribute("key-4", "value-43")
            }
        }

        mockBrazeInstance.currentUser?.apply {
            verify(exactly = 0) {
                mockBrazeInstance.changeUser(any())
                setDateOfBirth(any(), any(), any())
                setEmail(any())
                setFirstName(any())
                setLastName(any())
                setPhoneNumber(any())
                // Address
                setHomeCity(any())
                setCountry(any())
                // Custom Attributes
                setCustomUserAttribute(any<String>(), any<Boolean>())
                setCustomUserAttribute(any<String>(), any<Long>())
                setCustomUserAttribute(any<String>(), any<Double>())
                setCustomUserAttributeToSecondsFromEpoch(any<String>(), any())
            }
        }
    }

    @Test
    fun `given deDupe is disabled and previously an Identify event has been made, when a new Identify event is made with exactly same trait, then again all traits should be set after the second identify event`() {
        // mockBrazeIntegrationConfig has deDupe disabled.
        brazeIntegration.create(mockBrazeIntegrationConfigWithDeDupeDisabled)
        val identifyEvent = provideIdentifyEvent()
        // Making first identify event. It should set all the traits.
        brazeIntegration.identify(identifyEvent)
        // Clearing the mocks to reset the state.
        clearMocks(mockBrazeInstance)

        // Making second identify event with exactly same traits. It should not set any trait.
        brazeIntegration.identify(identifyEvent)

        // All traits should be set again.
        verifyTraits()
    }

    @Test
    fun `given identify event doesn't contain externalId, when the event is made, then userId is set as the preferred Id`() {
        brazeIntegration.create(mockBrazeIntegrationConfig)
        val identifyEvent = provideIdentifyEvent(options = RudderOption())

        brazeIntegration.identify(identifyEvent)

        verify(exactly = 1) {
            mockBrazeInstance.changeUser(USER_ID)
        }
    }

    @Test
    fun `given hybrid mode is enabled, when identify event is made, then no traits is set`() {
        brazeIntegration.create(mockNewBrazeIntegrationConfig)
        val identifyEvent = provideIdentifyEvent()

        brazeIntegration.identify(identifyEvent)

        verifyNullTraitsSet()
    }

    @Test
    fun `given hybrid mode is enabled, when a custom track event is made, then no event is logged`() {
        brazeIntegration.create(mockNewBrazeIntegrationConfig)
        val trackEvent = provideTrackEvent(eventName = CUSTOM_TRACK_EVENT)

        brazeIntegration.track(trackEvent)

        verify(exactly = 0) {
            mockBrazeInstance.logCustomEvent(any())
            mockBrazeInstance.logCustomEvent(any(), any())
        }
    }

    @Test
    fun `given hybrid mode is enabled, when an order completed event is made, then no event is logged`() {
        brazeIntegration.create(mockNewBrazeIntegrationConfig)
        val trackEvent = provideTrackEvent(
            eventName = ORDER_COMPLETED,
            properties = getOrderCompletedProperties(),
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

    @Test
    fun `given hybrid mode is enabled, when an install attributed event is made, then no event is logged`() {
        brazeIntegration.create(mockNewBrazeIntegrationConfig)
        val trackEvent = provideTrackEvent(
            eventName = INSTALL_ATTRIBUTED,
            properties = getCampaignObject(),
        )

        brazeIntegration.track(trackEvent)

        mockBrazeInstance.currentUser?.apply {
            verify(exactly = 0) {
                setAttributionData(any())
            }
        }
    }

    @Test
    fun `given hybrid mode is enabled, when a flush event is made, then the data is flushed`() {
        brazeIntegration.create(mockNewBrazeIntegrationConfig)

        brazeIntegration.flush()

        verify(exactly = 1) {
            mockBrazeInstance.requestImmediateDataFlush()
        }
    }

    @Test
    fun `given on dynamic config update the deDupe is enabled, when the config is updated and identify event is made, then no trait should be set`() {
        brazeIntegration.create(mockBrazeIntegrationConfigWithDeDupeDisabled)
        val identifyEvent = provideIdentifyEvent()
        // Making first identify event. It should set all the traits.
        brazeIntegration.identify(identifyEvent)
        // Clearing the mocks to reset the state.
        clearMocks(mockBrazeInstance)

        // Update the config with deDup enabled
        brazeIntegration.update(mockBrazeIntegrationConfig)
        // Making second identify event with exactly same traits. It should not set any trait, as config with deDup enabled was updated.
        brazeIntegration.identify(identifyEvent)

        // No trait should be set.
        verifyNullTraitsSet()
    }

    @Test
    fun `given an empty integration config, when either SDK is initialised or config is updated, then no error should be thrown`() {
        val emptyJsonObject = JsonObject(emptyMap())

        // No exception should be thrown
        brazeIntegration.create(emptyJsonObject)

        // No exception should be thrown
        brazeIntegration.update(emptyJsonObject)
    }

    @Test
    fun `given platform-specific key is enabled and androidAppIdentifierKey is present, when integration is initialised, then androidAppIdentifierKey should be used`() {
        val configWithAndroidAppIdentifierKey: JsonObject = readFileAsJsonObject(pathToBrazeConfigWithAndroidAppIdentifierKey)
        val appIdentifierKeySlot = slot<String>()
        every { mockBrazeConfigBuilder.setApiKey(capture(appIdentifierKeySlot)) } returns mockBrazeConfigBuilder

        brazeIntegration.create(configWithAndroidAppIdentifierKey)

        verify(exactly = 1) {
            mockBrazeConfigBuilder.setApiKey("androidSpecificApiKey")
        }
    }

    @Test
    fun `given platform-specific key flag is disabled, when integration is initialised, then legacy appKey should be used`() {
        val configWithFlagDisabled: JsonObject = readFileAsJsonObject(pathToBrazeConfigWithFlagDisabled)
        val appIdentifierKeySlot = slot<String>()
        every { mockBrazeConfigBuilder.setApiKey(capture(appIdentifierKeySlot)) } returns mockBrazeConfigBuilder

        brazeIntegration.create(configWithFlagDisabled)

        verify(exactly = 1) {
            mockBrazeConfigBuilder.setApiKey("legacyAppKey")
        }
    }

    @Test
    fun `given platform-specific key is enabled but androidAppIdentifierKey is missing, when integration is initialised, then legacy appKey should be used as fallback`() {
        val appIdentifierKeySlot = slot<String>()
        every { mockBrazeConfigBuilder.setApiKey(capture(appIdentifierKeySlot)) } returns mockBrazeConfigBuilder

        brazeIntegration.create(mockBrazeIntegrationConfig)

        verify(exactly = 1) {
            mockBrazeConfigBuilder.setApiKey("someAppKey")
        }
    }

    @Test
    fun `given platform-specific key is enabled but androidAppIdentifierKey is blank, when integration is initialised, then legacy appKey should be used as fallback`() {
        val configWithBlankAndroidAppIdentifierKey: JsonObject = readFileAsJsonObject(pathToBrazeConfigWithBlankAndroidAppIdentifierKey)
        val appIdentifierKeySlot = slot<String>()
        every { mockBrazeConfigBuilder.setApiKey(capture(appIdentifierKeySlot)) } returns mockBrazeConfigBuilder

        brazeIntegration.create(configWithBlankAndroidAppIdentifierKey)

        verify(exactly = 1) {
            mockBrazeConfigBuilder.setApiKey("legacyAppKey")
        }
    }

    private fun verifyTraits() {
        mockBrazeInstance.currentUser?.apply {
            verify(exactly = 1) {
                mockBrazeInstance.changeUser(BRAZE_EXTERNAL_ID)
                setDateOfBirth(
                    year = 1990,
                    month = Month.JANUARY,
                    day = 1,
                )
                setEmail(EMAIL)
                setFirstName(FIRST_NAME)
                setLastName(LAST_NAME)
                setGender(GENDER_MALE)
                setPhoneNumber(PHONE_NUMBER)
                // Address
                setHomeCity(CITY)
                setCountry(COUNTRY)
                // Custom Attributes
                setCustomUserAttribute("key-1", true)
                setCustomUserAttribute("key-2", 1234L)
                setCustomUserAttribute("key-3", 678.45)
                setCustomUserAttribute("key-4", "value-4")
                setCustomUserAttributeToSecondsFromEpoch("key-5", Utility.tryDateConversion(Utility.DATE_STRING) ?: 0)
            }
        }
    }

    private fun verifyNullTraitsSet() {
        mockBrazeInstance.currentUser?.apply {
            verify(exactly = 0) {
                mockBrazeInstance.changeUser(any())
                setDateOfBirth(any(), any(), any())
                setEmail(any())
                setFirstName(any())
                setLastName(any())
                setGender(any())
                setPhoneNumber(any())
                // Address
                setHomeCity(any())
                setCountry(any())
                // Custom Attributes
                setCustomUserAttribute(any<String>(), any<Boolean>())
                setCustomUserAttribute(any<String>(), any<Long>())
                setCustomUserAttribute(any<String>(), any<Double>())
                // Currently, anonymousId is being also set. Therefore, we are unable to make a proper assertion.
//                setCustomUserAttribute(any<String>(), any<String>())
                setCustomUserAttributeToSecondsFromEpoch(any<String>(), any())
            }
        }
    }
}
