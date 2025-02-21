package com.rudderstack.integration.kotlin.adjust

import android.app.Application
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustInstance
import com.rudderstack.sdk.kotlin.android.utils.application
import com.rudderstack.sdk.kotlin.core.Analytics
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.AdjustEvent
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.internals.models.RudderTraits
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.UserIdentity
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import io.mockk.Runs
import io.mockk.just
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val pathToAdjustConfig = "config/adjust_config.json"
private const val pathToNewAdjustConfig = "config/new_adjust_config.json"
private const val APP_TOKEN = "someAppToken"
private const val ADJUST_ENVIRONMENT = AdjustConfig.ENVIRONMENT_SANDBOX
private const val ANONYMOUS_ID = "anonymousId"
private const val USER_ID = "userId"
private const val TRACK_EVENT_ALLOWED = "Track event Android"
private const val TRACK_EVENT_DENY = "Track event random"
private const val TRACK_EVENT_ALLOWED_NEW = "Track event new"

class AdjustIntegrationTest {

    private val mockAdjustIntegrationConfig: JsonObject = readFileAsJsonObject(pathToAdjustConfig)
    private val mockNewAdjustIntegrationConfig: JsonObject = readFileAsJsonObject(pathToNewAdjustConfig)

    @MockK
    private lateinit var mockAnalytics: Analytics

    @MockK
    private lateinit var mockApplication: Application

    // Spy objects
    private lateinit var mockAdjustInstance: AdjustInstance
    private lateinit var mockAdjustConfig: AdjustConfig
    private lateinit var mockAdjustEvent: AdjustEvent

    private lateinit var adjustIntegration: AdjustIntegration

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        // Spy objects
        mockAdjustInstance = spyk(AdjustInstance())
        mockAdjustConfig = spyk(AdjustConfig(mockApplication, APP_TOKEN, ADJUST_ENVIRONMENT))
        mockAdjustEvent = spyk(AdjustEvent(APP_TOKEN))

        // Mock static methods
        mockkStatic(::initAdjustConfig, ::initAdjustEvent)
        mockkStatic(Adjust::class)

        // Mock initAdjustConfig
        every { initAdjustConfig(any(), any(), any()) } returns mockAdjustConfig

        // Mock Adjust SDK methods
        every { Adjust.initSdk(mockAdjustConfig) } just Runs
        every { Adjust.getDefaultInstance() } returns mockAdjustInstance
        // Adjust Identify API
        every { Adjust.addGlobalCallbackParameter(any(), any()) } just Runs
        // Adjust Reset API
        every { Adjust.removeGlobalPartnerParameters() } just Runs
        // Adjust Track API
        every { initAdjustEvent(any()) } returns mockAdjustEvent
        every { mockAdjustEvent.addCallbackParameter(any(), any()) } just Runs
        every { mockAdjustEvent.setRevenue(any(), any()) } just Runs
        every { Adjust.addGlobalPartnerParameter(any(), any()) } just Runs
        every { Adjust.trackEvent(any()) } just Runs

        // Mock Analytics
        every { mockAnalytics.application } returns mockApplication

        // Initialise the AdjustIntegration
        adjustIntegration = AdjustIntegration().also {
            it.analytics = mockAnalytics
        }
    }

    @Test
    fun `given integration initialisation attempt has not made, when instance is requested, then null is returned`() {
        val actualAdjustInstance = adjustIntegration.getDestinationInstance()

        assertNull(actualAdjustInstance)
    }

    @Test
    fun `given integration is initialised, when instance is requested, then adjust instance is returned`() {
        adjustIntegration.create(mockAdjustIntegrationConfig)

        val actualAdjustInstance = adjustIntegration.getDestinationInstance()

        assertEquals(mockAdjustInstance, actualAdjustInstance)
    }

    @Test
    fun `given integration is initialised, when integration is re-initialised, then the same adjust instance is returned`() {
        // Initialise the integration for the first time
        adjustIntegration.create(mockAdjustIntegrationConfig)
        val oldAdjustInstance = adjustIntegration.getDestinationInstance()
        // Simulate the re-initialisation of the integration by returning a new AdjustInstance
        val mockNewAdjustInstance: AdjustInstance = mockk()
        every { Adjust.getDefaultInstance() } returns mockNewAdjustInstance

        // Re-initialise the integration
        adjustIntegration.create(mockAdjustIntegrationConfig)
        val nextAdjustInstance = adjustIntegration.getDestinationInstance()

        assertEquals(mockAdjustInstance, oldAdjustInstance)
        assertEquals(mockAdjustInstance, nextAdjustInstance)
    }

    @Test
    fun `when integration is initialised, then adjust config is set with correct values`() {
        adjustIntegration.create(mockAdjustIntegrationConfig)

        // Verify Adjust Config initialisation
        mockAdjustConfig.apply {
            // Verify Setting of logLevel
            verify { setLogLevel(any()) }
            // Verify Setting of all listeners
            verify { onAttributionChangedListener = any() }
            verify { onEventTrackingSucceededListener = any() }
            verify { onEventTrackingFailedListener = any() }
            verify { onSessionTrackingSucceededListener = any() }
            verify { onSessionTrackingFailedListener = any() }
            verify { setOnDeferredDeeplinkResponseListener(any()) }
            verify { enableSendingInBackground() }
        }
    }

    @Test
    fun `when identify event is made, then it is successful`() {
        adjustIntegration.create(mockAdjustIntegrationConfig)
        val identifyEvent = provideIdentifyEvent()

        adjustIntegration.identify(identifyEvent)

        verify { Adjust.addGlobalPartnerParameter(ANONYMOUS_ID, any()) }
        verify { Adjust.addGlobalPartnerParameter(USER_ID, any()) }
    }

    @Test
    fun `given event-to-token mapping is present, when track event is made without revenue, then it is successful`() {
        adjustIntegration.create(mockAdjustIntegrationConfig)
        val trackEvent = provideTrackEvent(
            eventName = TRACK_EVENT_ALLOWED,
            properties = buildJsonObject {
                put("key1", "value1")
                put("key2", 23.45)
            },
        )

        adjustIntegration.track(trackEvent)

        mockAdjustEvent.apply {
            verify(exactly = 1) { addCallbackParameter("key1", "value1") }
            verify(exactly = 1) { addCallbackParameter("key2", "23.45") }
            verify(exactly = 1) { Adjust.addGlobalPartnerParameter(ANONYMOUS_ID, any()) }
            verify(exactly = 0) { setRevenue(any(), any()) }
        }
        verify(exactly = 1) { Adjust.trackEvent(mockAdjustEvent) }
    }

    @Test
    fun `given event-to-token mapping is present, when track event is made with revenue, then it is successful`() {
        val revenue = 1234.34
        val currency = "USD"
        adjustIntegration.create(mockAdjustIntegrationConfig)
        val trackEvent = provideTrackEvent(
            eventName = TRACK_EVENT_ALLOWED,
            properties = buildJsonObject {
                put("key1", "value1")
                put(PropertiesConstants.CURRENCY, currency)
                put(PropertiesConstants.REVENUE, revenue)
            },
        )

        adjustIntegration.track(trackEvent)

        mockAdjustEvent.apply {
            verify(exactly = 1) { addCallbackParameter("key1", "value1") }
            verify(exactly = 1) { Adjust.addGlobalPartnerParameter(ANONYMOUS_ID, any()) }
            verify(exactly = 1) { setRevenue(revenue, currency) }
        }
        verify(exactly = 1) { Adjust.trackEvent(mockAdjustEvent) }
    }

    @Test
    fun `given event-to-token mapping is not present, when track event is made, then it is unsuccessful`() {
        adjustIntegration.create(mockAdjustIntegrationConfig)
        val trackEvent = provideTrackEvent(
            eventName = TRACK_EVENT_DENY,
            properties = buildJsonObject {
                put("key1", "value1")
            },
        )

        adjustIntegration.track(trackEvent)

        mockAdjustEvent.apply {
            verify(exactly = 0) { addCallbackParameter("key1", "value1") }
            verify(exactly = 0) { Adjust.addGlobalPartnerParameter(ANONYMOUS_ID, any()) }
            verify(exactly = 0) { setRevenue(any(), any()) }
        }
        verify(exactly = 0) { Adjust.trackEvent(mockAdjustEvent) }
    }

    @Test
    fun `when reset call is made, then is successful`() {
        adjustIntegration.create(mockAdjustIntegrationConfig)

        adjustIntegration.reset()

        verify { Adjust.removeGlobalPartnerParameters() }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `given there's some issue with the app token, when integration is initialised, then exception is thrown`() {
        every { Adjust.initSdk(any()) } throws IllegalArgumentException("Invalid app token")

        adjustIntegration.create(mockAdjustIntegrationConfig)
    }

    @Test
    fun `given on dynamic config update the event-to-token mapping is changed, when destination config is updated dynamically, then only new event mapping is allowed`() {
        // Initialise the integration
        adjustIntegration.create(mockAdjustIntegrationConfig)
        val trackEvent = provideTrackEvent(
            eventName = TRACK_EVENT_ALLOWED,
            properties = buildJsonObject {
                put("key1", "value1")
            },
        )
        val newTrackEvent = provideTrackEvent(
            eventName = TRACK_EVENT_ALLOWED_NEW,
            properties = buildJsonObject {
                put("key1", "value1")
            },
        )

        // Update the integration with new config
        adjustIntegration.update(mockNewAdjustIntegrationConfig)

        // Track the event with old event mapping
        adjustIntegration.track(trackEvent)
        // Verify that the event is not tracked
        mockAdjustEvent.apply {
            verify(exactly = 0) { addCallbackParameter("key1", "value1") }
            verify(exactly = 0) { Adjust.addGlobalPartnerParameter(ANONYMOUS_ID, any()) }
            verify(exactly = 0) { setRevenue(any(), any()) }
        }
        verify(exactly = 0) { Adjust.trackEvent(mockAdjustEvent) }

        // Track the event with new event mapping
        adjustIntegration.track(newTrackEvent)
        // Verify that the event is tracked
        mockAdjustEvent.apply {
            verify(exactly = 1) { addCallbackParameter("key1", "value1") }
            verify(exactly = 1) { Adjust.addGlobalPartnerParameter(ANONYMOUS_ID, any()) }
        }
        verify(exactly = 1) { Adjust.trackEvent(mockAdjustEvent) }
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
private fun provideIdentifyEvent() = IdentifyEvent(
    userIdentityState = provideUserIdentityState()
).also {
    it.applyMockedValues()
    it.updateData(PlatformType.Mobile)
}

@OptIn(InternalRudderApi::class)
private fun provideTrackEvent(
    eventName: String,
    properties: JsonObject,
) = TrackEvent(
    event = eventName,
    properties = properties,
    options = RudderOption(),
).also {
    it.applyMockedValues()
    it.updateData(PlatformType.Mobile)
}

private fun provideUserIdentityState(
    anonymousId: String = ANONYMOUS_ID,
    userId: String = USER_ID,
    traits: RudderTraits = emptyJsonObject,
) = UserIdentity(
    anonymousId = anonymousId,
    userId = userId,
    traits = traits,
)

private fun Event.applyMockedValues() {
    this.originalTimestamp = "<original-timestamp>"
    this.context = emptyJsonObject
    this.messageId = "<message-id>"
}
