package com.rudderstack.integration.kotlin.adjust

import android.app.Application
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAttribution
import com.adjust.sdk.AdjustInstance
import com.adjust.sdk.OnAttributionChangedListener
import com.rudderstack.sdk.kotlin.android.utils.application
import com.rudderstack.sdk.kotlin.core.Analytics
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.AdjustEvent
import com.rudderstack.sdk.kotlin.android.Analytics as AndroidAnalytics
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.addLifecycleObserverWithCurrentState
import com.rudderstack.sdk.kotlin.android.utils.removeLifecycleObserverWithCurrentState
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.internals.models.Traits
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.UserIdentity
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import io.mockk.Runs
import io.mockk.just
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import org.junit.jupiter.api.assertThrows

private const val pathToAdjustConfig = "config/adjust_config.json"
private const val pathToNewAdjustConfig = "config/new_adjust_config.json"
private const val APP_TOKEN = "someAppToken"
private const val ADJUST_ENVIRONMENT = AdjustConfig.ENVIRONMENT_SANDBOX
private const val ANONYMOUS_ID = "anonymousId"
private const val USER_ID = "userId"
private const val TRACK_EVENT_ALLOWED = "Track event Android"
private const val TRACK_EVENT_DENY = "Track event random"
private const val TRACK_EVENT_ALLOWED_NEW = "Track event new"
private const val LIFECYCLE_MANAGEMENT_UTILS = "com.rudderstack.sdk.kotlin.android.utils.LifecycleManagementUtilsKt"

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

    @BeforeEach
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
        every { mockAdjustInstance.removeGlobalPartnerParameters() } just Runs
        // Adjust Track API
        every { initAdjustEvent(any()) } returns mockAdjustEvent
        every { mockAdjustEvent.addCallbackParameter(any(), any()) } just Runs
        every { mockAdjustEvent.setRevenue(any(), any()) } just Runs
        every { mockAdjustInstance.addGlobalPartnerParameter(any(), any()) } just Runs
        every { mockAdjustInstance.trackEvent(any()) } just Runs

        // Mock Analytics
        every { mockAnalytics.application } returns mockApplication
        every { mockAnalytics.track(any(), any()) } just Runs

        // Initialise the AdjustIntegration
        adjustIntegration = AdjustIntegration().also {
            it.analytics = mockAnalytics
        }
    }

    @Nested
    inner class LiteMode {

        private val appAdjustInstance: AdjustInstance = mockk(relaxed = true)

        // Lazy: JUnit builds this nested instance before the outer setup initialises the mocks.
        private val liteAdjustIntegration by lazy {
            AdjustIntegration(appAdjustInstance).also { it.analytics = mockAnalytics }
        }

        @OptIn(InternalRudderApi::class)
        @Test
        fun `given lite mode, when create runs before and after a teardown, then Adjust is not initialised, no attribution callback or activity observer is set, and the app's instance is returned`() {
            val androidAnalytics = mockAndroidAnalytics()
            liteAdjustIntegration.analytics = androidAnalytics

            liteAdjustIntegration.create(mockAdjustIntegrationConfig)
            liteAdjustIntegration.teardown()
            liteAdjustIntegration.create(mockAdjustIntegrationConfig)

            assertEquals(appAdjustInstance, liteAdjustIntegration.getDestinationInstance())
            verify(exactly = 0) {
                initAdjustConfig(any(), any(), any())
                Adjust.initSdk(any())
                Adjust.getDefaultInstance()
                mockAdjustConfig.setOnAttributionChangedListener(any())
                androidAnalytics.addLifecycleObserverWithCurrentState(any())
            }
        }

        @Test
        fun `given lite mode, when track, identify, and reset are made, then they reach the app's instance and not the default instance`() {
            liteAdjustIntegration.create(mockAdjustIntegrationConfig)

            liteAdjustIntegration.track(provideTrackEvent(eventName = TRACK_EVENT_ALLOWED, properties = buildJsonObject {}))
            liteAdjustIntegration.identify(provideIdentifyEvent())
            liteAdjustIntegration.reset()

            verify(exactly = 1) {
                appAdjustInstance.trackEvent(mockAdjustEvent)
                appAdjustInstance.addGlobalPartnerParameter(USER_ID, any())
                appAdjustInstance.removeGlobalPartnerParameters()
            }
            verify(atLeast = 1) { appAdjustInstance.addGlobalPartnerParameter(ANONYMOUS_ID, any()) }
            verify(exactly = 0) {
                mockAdjustInstance.trackEvent(any())
                mockAdjustInstance.addGlobalPartnerParameter(any(), any())
                mockAdjustInstance.removeGlobalPartnerParameters()
            }
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

        verify { mockAdjustInstance.addGlobalPartnerParameter(ANONYMOUS_ID, any()) }
        verify { mockAdjustInstance.addGlobalPartnerParameter(USER_ID, any()) }
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
            verify(exactly = 1) { mockAdjustInstance.addGlobalPartnerParameter(ANONYMOUS_ID, any()) }
            verify(exactly = 0) { setRevenue(any(), any()) }
        }
        verify(exactly = 1) { mockAdjustInstance.trackEvent(mockAdjustEvent) }
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
            verify(exactly = 1) { mockAdjustInstance.addGlobalPartnerParameter(ANONYMOUS_ID, any()) }
            verify(exactly = 1) { setRevenue(revenue, currency) }
        }
        verify(exactly = 1) { mockAdjustInstance.trackEvent(mockAdjustEvent) }
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
            verify(exactly = 0) { mockAdjustInstance.addGlobalPartnerParameter(ANONYMOUS_ID, any()) }
            verify(exactly = 0) { setRevenue(any(), any()) }
        }
        verify(exactly = 0) { mockAdjustInstance.trackEvent(mockAdjustEvent) }
    }

    @Test
    fun `when reset call is made, then is successful`() {
        adjustIntegration.create(mockAdjustIntegrationConfig)

        adjustIntegration.reset()

        verify { mockAdjustInstance.removeGlobalPartnerParameters() }
    }

    @Test
    fun `given there's some issue with the app token, when integration is initialised, then exception is thrown`() {
        every { Adjust.initSdk(any()) } throws IllegalArgumentException("Invalid app token")

        assertThrows<IllegalArgumentException>{ adjustIntegration.create(mockAdjustIntegrationConfig) }
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
            verify(exactly = 0) { mockAdjustInstance.addGlobalPartnerParameter(ANONYMOUS_ID, any()) }
            verify(exactly = 0) { setRevenue(any(), any()) }
        }
        verify(exactly = 0) { mockAdjustInstance.trackEvent(mockAdjustEvent) }

        // Track the event with new event mapping
        adjustIntegration.track(newTrackEvent)
        // Verify that the event is tracked
        mockAdjustEvent.apply {
            verify(exactly = 1) { addCallbackParameter("key1", "value1") }
            verify(exactly = 1) { mockAdjustInstance.addGlobalPartnerParameter(ANONYMOUS_ID, any()) }
        }
        verify(exactly = 1) { mockAdjustInstance.trackEvent(mockAdjustEvent) }
    }

    @Test
    fun `given a created integration, when create runs again with a new config, then the new mapping applies and Adjust is initialised once`() {
        adjustIntegration.create(mockAdjustIntegrationConfig)
        adjustIntegration.create(mockNewAdjustIntegrationConfig)
        val newTrackEvent = provideTrackEvent(
            eventName = TRACK_EVENT_ALLOWED_NEW,
            properties = buildJsonObject {
                put("key1", "value1")
            },
        )

        adjustIntegration.track(newTrackEvent)

        verify(exactly = 1) { mockAdjustInstance.trackEvent(mockAdjustEvent) }
        verify(exactly = 1) { Adjust.initSdk(mockAdjustConfig) }
    }

    @OptIn(InternalRudderApi::class)
    @Test
    fun `given a created integration, when it is torn down and created again, then it registers for activity events again`() {
        val androidAnalytics = mockAndroidAnalytics()
        adjustIntegration.analytics = androidAnalytics

        adjustIntegration.create(mockAdjustIntegrationConfig)
        adjustIntegration.teardown()
        adjustIntegration.create(mockAdjustIntegrationConfig)

        verify(exactly = 2) { androidAnalytics.addLifecycleObserverWithCurrentState(adjustIntegration) }
        verify(exactly = 1) { androidAnalytics.removeLifecycleObserverWithCurrentState(adjustIntegration) }
        verify(exactly = 1) { Adjust.initSdk(mockAdjustConfig) }
    }

    @OptIn(InternalRudderApi::class)
    private fun mockAndroidAnalytics(): AndroidAnalytics {
        mockkStatic(LIFECYCLE_MANAGEMENT_UTILS)
        return mockk<AndroidAnalytics>(relaxed = true).also {
            every { it.application } returns mockApplication
            every { it.addLifecycleObserverWithCurrentState(any()) } just Runs
            every { it.removeLifecycleObserverWithCurrentState(any<ActivityLifecycleObserver>()) } just Runs
        }
    }

    @Test
    fun `given an empty config, when create runs, then it throws, update does not, and a later valid config initialises Adjust`() {
        assertThrows<IllegalArgumentException> { adjustIntegration.create(emptyJsonObject) }

        // No exception should be thrown
        adjustIntegration.update(JsonObject(emptyMap()))

        adjustIntegration.create(mockAdjustIntegrationConfig)

        verify(exactly = 1) { Adjust.initSdk(mockAdjustConfig) }
    }

    @Test
    fun `given install attribution tracking is enabled, when attribution callback is triggered, then install attributed event is sent`() {
        val configWithAttribution = buildJsonObject {
            put("appToken", APP_TOKEN)
            put("enableInstallAttributionTracking", true)
            put("customMappings", buildJsonArray { })
        }
        adjustIntegration.create(configWithAttribution)

        val testAttribution = createAdjustAttribution(
            trackerName = "google_ads_campaign",
            trackerToken = "xyz789abc",
            network = "Google Ads",
            campaign = "spring_promotion_2024",
            adgroup = "premium_users",
            creative = "banner_ad_v2",
            clickLabel = "premium_click"
        )

        // Get the attribution listener that was set
        val attributionSlot = slot<OnAttributionChangedListener>()
        verify { mockAdjustConfig.setOnAttributionChangedListener(capture(attributionSlot)) }

        // Trigger the attribution callback
        attributionSlot.captured.onAttributionChanged(testAttribution)

        // Verify that analytics.track was called with all attribution parameters
        verify {
            mockAnalytics.track(
                "Install Attributed",
                match { jsonObject ->
                    val jsonString = jsonObject.toString()
                    jsonString.contains("\"provider\":\"Adjust\"") &&
                    jsonString.contains("\"trackerName\":\"google_ads_campaign\"") &&
                    jsonString.contains("\"trackerToken\":\"xyz789abc\"") &&
                    jsonString.contains("\"source\":\"Google Ads\"") &&
                    jsonString.contains("\"name\":\"spring_promotion_2024\"") &&
                    jsonString.contains("\"adGroup\":\"premium_users\"") &&
                    jsonString.contains("\"adCreative\":\"banner_ad_v2\"") &&
                    jsonString.contains("\"content\":\"premium_click\"")
                }
            )
        }
    }

    @Test
    fun `given install attribution tracking is disabled, when attribution callback is triggered, then no install attributed event is sent`() {
        val configWithoutAttribution = buildJsonObject {
            put("appToken", APP_TOKEN)
            put("enableInstallAttributionTracking", false)
            put("customMappings", buildJsonArray { })
        }
        adjustIntegration.create(configWithoutAttribution)
        
        val testAttribution = createAdjustAttribution(
            trackerName = "testTrackerName",
            trackerToken = "testTrackerToken"
        )
        
        // Get the attribution listener that was set
        val attributionSlot = slot<OnAttributionChangedListener>()
        verify { mockAdjustConfig.setOnAttributionChangedListener(capture(attributionSlot)) }
        
        // Trigger the attribution callback
        attributionSlot.captured.onAttributionChanged(testAttribution)
        
        // Verify that analytics.track was NOT called for install attributed event
        verify(exactly = 0) {
            mockAnalytics.track("Install Attributed", any())
        }
    }

    @Test
    fun `given install attribution tracking is enabled, when attribution data contains campaign info, then campaign object is included in event`() {
        val configWithAttribution = buildJsonObject {
            put("appToken", APP_TOKEN)
            put("enableInstallAttributionTracking", true)
            put("customMappings", buildJsonArray { })
        }
        adjustIntegration.create(configWithAttribution)
        
        val testAttribution = createAdjustAttribution(
            trackerName = "facebook_campaign",
            trackerToken = "abc123def",
            network = "Facebook Ads",
            campaign = "summer_sale_2024",
            adgroup = "mobile_users_18_35",
            creative = "video_ad_creative_1",
            clickLabel = "click_label_test"
        )
        
        // Get the attribution listener that was set
        val attributionSlot = slot<OnAttributionChangedListener>()
        verify { mockAdjustConfig.setOnAttributionChangedListener(capture(attributionSlot)) }
        
        // Trigger the attribution callback
        attributionSlot.captured.onAttributionChanged(testAttribution)
        
        // Verify that analytics.track was called with all attribution parameters
        verify {
            mockAnalytics.track(
                "Install Attributed",
                match { jsonObject ->
                    val jsonString = jsonObject.toString()
                    jsonString.contains("\"provider\":\"Adjust\"") &&
                    jsonString.contains("\"trackerName\":\"facebook_campaign\"") &&
                    jsonString.contains("\"trackerToken\":\"abc123def\"") &&
                    jsonString.contains("\"source\":\"Facebook Ads\"") &&
                    jsonString.contains("\"name\":\"summer_sale_2024\"") &&
                    jsonString.contains("\"adGroup\":\"mobile_users_18_35\"") &&
                    jsonString.contains("\"adCreative\":\"video_ad_creative_1\"") &&
                    jsonString.contains("\"content\":\"click_label_test\"")
                }
            )
        }
    }

    @Test
    fun `given install attribution tracking is enabled, when attribution data has null and empty values, then install attributed event handles them properly`() {
        val configWithAttribution = buildJsonObject {
            put("appToken", APP_TOKEN)
            put("enableInstallAttributionTracking", true)
            put("customMappings", buildJsonArray { })
        }
        adjustIntegration.create(configWithAttribution)
        
        val testAttribution = createAdjustAttribution(
            trackerName = "organic_tracker",
            trackerToken = "org123",
            network = null,  // Test null network
            campaign = "",   // Test empty campaign
            adgroup = "organic_group",
            creative = null, // Test null creative
            clickLabel = ""
        )
        
        // Get the attribution listener that was set
        val attributionSlot = slot<OnAttributionChangedListener>()
        verify { mockAdjustConfig.setOnAttributionChangedListener(capture(attributionSlot)) }
        
        // Trigger the attribution callback
        attributionSlot.captured.onAttributionChanged(testAttribution)
        
        // Verify that analytics.track was called and handles null/empty values properly
        verify {
            mockAnalytics.track(
                "Install Attributed",
                match { jsonObject ->
                    val jsonString = jsonObject.toString()
                    jsonString.contains("\"provider\":\"Adjust\"") &&
                    jsonString.contains("\"trackerName\":\"organic_tracker\"") &&
                    jsonString.contains("\"trackerToken\":\"org123\"") &&
                    jsonString.contains("\"adGroup\":\"organic_group\"")
                    // Note: Null and empty values should not appear in the JSON or should be handled gracefully
                }
            )
        }
    }

    @Test
    fun `given install attribution tracking is enabled, when attribution callback is triggered multiple times, then multiple install attributed events are sent`() {
        val configWithAttribution = buildJsonObject {
            put("appToken", APP_TOKEN)
            put("enableInstallAttributionTracking", true)
            put("customMappings", buildJsonArray { })
        }
        adjustIntegration.create(configWithAttribution)
        
        // Get the attribution listener that was set
        val attributionSlot = slot<OnAttributionChangedListener>()
        verify { mockAdjustConfig.setOnAttributionChangedListener(capture(attributionSlot)) }
        
        // Create different attribution instances for multiple callbacks
        val firstAttribution = createAdjustAttribution(
            trackerName = "first_tracker",
            trackerToken = "token123",
            network = "Google Ads"
        )
        
        val secondAttribution = createAdjustAttribution(
            trackerName = "second_tracker", 
            trackerToken = "token456",
            network = "Facebook Ads"
        )
        
        // Trigger the attribution callback multiple times
        attributionSlot.captured.onAttributionChanged(firstAttribution)
        attributionSlot.captured.onAttributionChanged(secondAttribution)
        attributionSlot.captured.onAttributionChanged(firstAttribution) // Same attribution again
        
        // Verify that analytics.track was called exactly 3 times (once for each callback)
        // Since there's no deduplication logic, each callback triggers a new event
        verify(exactly = 3) {
            mockAnalytics.track("Install Attributed", any())
        }
        
        // Verify the first event contains first attribution data
        verify {
            mockAnalytics.track(
                "Install Attributed",
                match { jsonObject ->
                    val jsonString = jsonObject.toString()
                    jsonString.contains("\"trackerName\":\"first_tracker\"") &&
                    jsonString.contains("\"trackerToken\":\"token123\"") &&
                    jsonString.contains("\"source\":\"Google Ads\"")
                }
            )
        }
        
        // Verify the second event contains second attribution data
        verify {
            mockAnalytics.track(
                "Install Attributed",
                match { jsonObject ->
                    val jsonString = jsonObject.toString()
                    jsonString.contains("\"trackerName\":\"second_tracker\"") &&
                    jsonString.contains("\"trackerToken\":\"token456\"") &&
                    jsonString.contains("\"source\":\"Facebook Ads\"")
                }
            )
        }
    }

    @Test
    fun `given integration is created with enableInstallAttributionTracking true, when update is called with enableInstallAttributionTracking false, then attribution callback does not send event`() {
        // Initial config with attribution tracking enabled
        val initialConfig = buildJsonObject {
            put("appToken", APP_TOKEN)
            put("enableInstallAttributionTracking", true)
            put("customMappings", buildJsonArray { })
        }
        adjustIntegration.create(initialConfig)
        
        // Update config to disable attribution tracking
        val updatedConfig = buildJsonObject {
            put("appToken", APP_TOKEN)
            put("enableInstallAttributionTracking", false)
            put("customMappings", buildJsonArray { })
        }
        adjustIntegration.update(updatedConfig)
        
        val testAttribution = createAdjustAttribution(
            trackerName = "testTrackerName",
            trackerToken = "testTrackerToken",
            network = "TestNetwork"
        )
        
        // Get the attribution listener that was set during create()
        val attributionSlot = slot<OnAttributionChangedListener>()
        verify { mockAdjustConfig.setOnAttributionChangedListener(capture(attributionSlot)) }
        
        // Trigger the attribution callback after update
        attributionSlot.captured.onAttributionChanged(testAttribution)
        
        // Verify that analytics.track was NOT called because tracking was disabled via update
        verify(exactly = 0) {
            mockAnalytics.track("Install Attributed", any())
        }
    }

    @Test
    fun `given integration is created with enableInstallAttributionTracking false, when update is called with enableInstallAttributionTracking true, then attribution callback sends event`() {
        // Initial config with attribution tracking disabled
        val initialConfig = buildJsonObject {
            put("appToken", APP_TOKEN)
            put("enableInstallAttributionTracking", false)
            put("customMappings", buildJsonArray { })
        }
        adjustIntegration.create(initialConfig)
        
        // Update config to enable attribution tracking
        val updatedConfig = buildJsonObject {
            put("appToken", APP_TOKEN)
            put("enableInstallAttributionTracking", true)
            put("customMappings", buildJsonArray { })
        }
        adjustIntegration.update(updatedConfig)
        
        val testAttribution = createAdjustAttribution(
            trackerName = "enabledTrackerName",
            trackerToken = "enabledTrackerToken",
            network = "Google Ads",
            campaign = "test_campaign"
        )
        
        // Get the attribution listener that was set during create()
        val attributionSlot = slot<OnAttributionChangedListener>()
        verify { mockAdjustConfig.setOnAttributionChangedListener(capture(attributionSlot)) }
        
        // Trigger the attribution callback after update
        attributionSlot.captured.onAttributionChanged(testAttribution)
        
        // Verify that analytics.track WAS called because tracking was enabled via update
        verify(exactly = 1) {
            mockAnalytics.track(
                "Install Attributed",
                match { jsonObject ->
                    val jsonString = jsonObject.toString()
                    jsonString.contains("\"provider\":\"Adjust\"") &&
                    jsonString.contains("\"trackerName\":\"enabledTrackerName\"") &&
                    jsonString.contains("\"trackerToken\":\"enabledTrackerToken\"") &&
                    jsonString.contains("\"source\":\"Google Ads\"")
                }
            )
        }
    }

    @Test
    fun `given enableInstallAttributionTracking is not provided in configuration, when attribution callback is triggered, then no install attributed event is sent due to default false value`() {
        // Create config without enableInstallAttributionTracking field to test default behavior
        val configWithoutAttributionField = buildJsonObject {
            put("appToken", APP_TOKEN)
            put("customMappings", buildJsonArray { })
            // Note: enableInstallAttributionTracking is intentionally omitted to test default value (false)
        }
        adjustIntegration.create(configWithoutAttributionField)
        
        val testAttribution = createAdjustAttribution(
            trackerName = "testTrackerName",
            trackerToken = "testTrackerToken",
            network = "Test Network"
        )
        
        // Get the attribution listener that was set
        val attributionSlot = slot<OnAttributionChangedListener>()
        verify { mockAdjustConfig.setOnAttributionChangedListener(capture(attributionSlot)) }
        
        // Trigger the attribution callback
        attributionSlot.captured.onAttributionChanged(testAttribution)
        
        // Verify that analytics.track was NOT called because enableInstallAttributionTracking defaults to false
        verify(exactly = 0) {
            mockAnalytics.track("Install Attributed", any())
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
    traits: Traits = emptyJsonObject,
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

/**
 * Creates an AdjustAttribution instance with the specified production fields.
 * Only sets fields that are actually used in the sendInstallAttributedEvent method.
 */
private fun createAdjustAttribution(
    trackerName: String? = null,
    trackerToken: String? = null,
    network: String? = null,
    campaign: String? = null,
    adgroup: String? = null,
    creative: String? = null,
    clickLabel: String? = null
): AdjustAttribution {
    return AdjustAttribution().also {
        it.trackerName = trackerName
        it.trackerToken = trackerToken
        it.network = network
        it.campaign = campaign
        it.adgroup = adgroup
        it.creative = creative
        it.clickLabel = clickLabel
    }
}
