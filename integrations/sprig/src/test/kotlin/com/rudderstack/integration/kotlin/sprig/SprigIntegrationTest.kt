package com.rudderstack.integration.kotlin.sprig

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.rudderstack.sdk.kotlin.android.utils.application
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.internals.models.Traits
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.UserIdentity
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.userleap.EventPayload
import com.userleap.Sprig
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.BufferedReader

private const val PATH_TO_SPRIG_CONFIG = "config/sprig_config.json"
private const val ANONYMOUS_ID = "anonymousId"
private const val USER_ID = "testUserId"

class SprigIntegrationTest {

    private val mockSprigConfig: JsonObject = readFileAsJsonObject(PATH_TO_SPRIG_CONFIG)

    @MockK
    private lateinit var mockAnalytics: Analytics

    @MockK
    private lateinit var mockApplication: Application

    @MockK
    private lateinit var mockApplicationContext: Context

    private lateinit var sprigIntegration: SprigIntegration

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        mockkObject(Sprig)
        every { Sprig.configure(any(), any()) } just Runs
        every { Sprig.addEventListener(any(), any()) } just Runs
        every { Sprig.setUserIdentifier(any()) } just Runs
        every { Sprig.setEmailAddress(any()) } just Runs
        every { Sprig.setVisitorAttribute(any(), any<String>()) } just Runs
        every { Sprig.setVisitorAttribute(any(), any<Int>()) } just Runs
        every { Sprig.setVisitorAttribute(any(), any<Boolean>()) } just Runs
        every { Sprig.track(any<EventPayload>()) } just Runs
        every { Sprig.trackAndPresent(any<EventPayload>(), any()) } just Runs
        every { Sprig.logout() } just Runs

        every { mockAnalytics.application } returns mockApplication
        every { mockApplication.applicationContext } returns mockApplicationContext

        sprigIntegration = SprigIntegration().also {
            it.analytics = mockAnalytics
        }
    }

    // region create

    @Test
    fun `given integration initialisation has not been made, when instance is requested, then null is returned`() {
        val instance = sprigIntegration.getDestinationInstance()

        assertNull(instance)
    }

    @Test
    fun `given integration is initialised, when instance is requested, then Sprig instance is returned`() {
        sprigIntegration.create(mockSprigConfig)

        val instance = sprigIntegration.getDestinationInstance()

        assertEquals(Sprig, instance)
    }

    @Test
    fun `when integration is initialised, then Sprig configure is called with correct values`() {
        sprigIntegration.create(mockSprigConfig)

        verify { Sprig.configure(mockApplicationContext, "testEnvironmentId123") }
    }

    @Test
    fun `given integration is already initialised, when create is called again, then Sprig configure is not called again`() {
        sprigIntegration.create(mockSprigConfig)
        sprigIntegration.create(mockSprigConfig)

        verify(exactly = 1) { Sprig.configure(any(), any()) }
    }

    @Test
    fun `given an empty config, when create is called, then Sprig is not initialised`() {
        sprigIntegration.create(emptyJsonObject)

        val instance = sprigIntegration.getDestinationInstance()

        assertNull(instance)
        verify(exactly = 0) { Sprig.configure(any(), any()) }
    }

    // endregion

    // region identify

    @Test
    fun `given integration is initialised, when identify is called with userId, then setUserIdentifier is called`() {
        sprigIntegration.create(mockSprigConfig)
        val identifyEvent = provideIdentifyEvent(userId = USER_ID)

        sprigIntegration.identify(identifyEvent)

        verify { Sprig.setUserIdentifier(USER_ID) }
    }

    @Test
    fun `given integration is initialised, when identify is called with email trait, then setEmailAddress is called`() {
        sprigIntegration.create(mockSprigConfig)
        val traits = buildJsonObject { put("email", "test@example.com") }
        val identifyEvent = provideIdentifyEvent(userId = USER_ID, traits = traits)

        sprigIntegration.identify(identifyEvent)

        verify { Sprig.setEmailAddress("test@example.com") }
    }

    @Test
    fun `given integration is initialised, when identify is called with string trait, then setVisitorAttribute is called with string`() {
        sprigIntegration.create(mockSprigConfig)
        val traits = buildJsonObject { put("name", "John Doe") }
        val identifyEvent = provideIdentifyEvent(userId = USER_ID, traits = traits)

        sprigIntegration.identify(identifyEvent)

        verify { Sprig.setVisitorAttribute("name", "John Doe") }
    }

    @Test
    fun `given integration is initialised, when identify is called with int trait, then setVisitorAttribute is called with int`() {
        sprigIntegration.create(mockSprigConfig)
        val traits = buildJsonObject { put("age", 25) }
        val identifyEvent = provideIdentifyEvent(userId = USER_ID, traits = traits)

        sprigIntegration.identify(identifyEvent)

        verify { Sprig.setVisitorAttribute("age", 25) }
    }

    @Test
    fun `given integration is initialised, when identify is called with long trait, then setVisitorAttribute is called with int`() {
        sprigIntegration.create(mockSprigConfig)
        val traits = buildJsonObject { put("loginCount", 1234567890123L) }
        val identifyEvent = provideIdentifyEvent(userId = USER_ID, traits = traits)

        sprigIntegration.identify(identifyEvent)

        verify { Sprig.setVisitorAttribute("loginCount", 1234567890123L.toInt()) }
    }

    @Test
    fun `given integration is initialised, when identify is called with double trait, then setVisitorAttribute is called with truncated int`() {
        sprigIntegration.create(mockSprigConfig)
        val traits = buildJsonObject { put("score", 3.75) }
        val identifyEvent = provideIdentifyEvent(userId = USER_ID, traits = traits)

        sprigIntegration.identify(identifyEvent)

        verify { Sprig.setVisitorAttribute("score", 3) }
    }

    @Test
    fun `given integration is initialised, when identify is called with negative double trait, then setVisitorAttribute is called with int truncated toward zero`() {
        sprigIntegration.create(mockSprigConfig)
        val traits = buildJsonObject { put("delta", -2.9) }
        val identifyEvent = provideIdentifyEvent(userId = USER_ID, traits = traits)

        sprigIntegration.identify(identifyEvent)

        verify { Sprig.setVisitorAttribute("delta", -2) }
    }

    @Test
    fun `given integration is initialised, when identify is called with float trait, then setVisitorAttribute is called with truncated int`() {
        sprigIntegration.create(mockSprigConfig)
        val traits = buildJsonObject { put("ratio", 1.5f) }
        val identifyEvent = provideIdentifyEvent(userId = USER_ID, traits = traits)

        sprigIntegration.identify(identifyEvent)

        verify { Sprig.setVisitorAttribute("ratio", 1) }
    }

    @Test
    fun `given integration is initialised, when identify is called with boolean trait, then setVisitorAttribute is called with boolean`() {
        sprigIntegration.create(mockSprigConfig)
        val traits = buildJsonObject { put("premium", true) }
        val identifyEvent = provideIdentifyEvent(userId = USER_ID, traits = traits)

        sprigIntegration.identify(identifyEvent)

        verify { Sprig.setVisitorAttribute("premium", true) }
    }

    @Test
    fun `given integration is initialised, when identify is called with invalid key starting with exclamation, then attribute is skipped`() {
        sprigIntegration.create(mockSprigConfig)
        val traits = buildJsonObject { put("!invalid", "value") }
        val identifyEvent = provideIdentifyEvent(userId = USER_ID, traits = traits)

        sprigIntegration.identify(identifyEvent)

        verify(exactly = 0) { Sprig.setVisitorAttribute("!invalid", any<String>()) }
    }

    @Test
    fun `given integration is not initialised, when identify is called, then no Sprig methods are called`() {
        val identifyEvent = provideIdentifyEvent(userId = USER_ID)

        sprigIntegration.identify(identifyEvent)

        verify(exactly = 0) { Sprig.setUserIdentifier(any()) }
    }

    @Test
    fun `given integration is initialised, when identify is called with blank userId, then setUserIdentifier is not called`() {
        sprigIntegration.create(mockSprigConfig)
        val identifyEvent = provideIdentifyEvent(userId = "")

        sprigIntegration.identify(identifyEvent)

        verify(exactly = 0) { Sprig.setUserIdentifier(any()) }
    }

    // endregion

    // region track

    @Test
    fun `given integration is initialised and no activity, when track is called, then Sprig track is called`() {
        sprigIntegration.create(mockSprigConfig)
        val trackEvent = provideTrackEvent(eventName = "Test Event")

        sprigIntegration.track(trackEvent)

        val payloadSlot = slot<EventPayload>()
        verify { Sprig.track(capture(payloadSlot)) }
        assertEquals("Test Event", payloadSlot.captured.event)
    }

    @Test
    fun `given integration is initialised and activity is present, when track is called, then trackAndPresent is called`() {
        sprigIntegration.create(mockSprigConfig)
        val mockActivity = mockSurveyHostActivity()
        sprigIntegration.setFragmentActivity(mockActivity)
        val trackEvent = provideTrackEvent(eventName = "Test Event")

        sprigIntegration.track(trackEvent)

        verify { Sprig.trackAndPresent(any<EventPayload>(), mockActivity) }
        verify(exactly = 0) { Sprig.track(any<EventPayload>()) }
    }

    @Test
    fun `given stored activity is below STARTED, when track is called, then Sprig track is used instead of trackAndPresent`() {
        sprigIntegration.create(mockSprigConfig)
        val mockActivity = mockSurveyHostActivity(lifecycleState = Lifecycle.State.CREATED)
        sprigIntegration.setFragmentActivity(mockActivity)
        val trackEvent = provideTrackEvent(eventName = "Test Event")

        sprigIntegration.track(trackEvent)

        verify { Sprig.track(any<EventPayload>()) }
        verify(exactly = 0) { Sprig.trackAndPresent(any<EventPayload>(), any()) }
    }

    @Test
    fun `given stored activity is finishing, when track is called, then Sprig track is used instead of trackAndPresent`() {
        sprigIntegration.create(mockSprigConfig)
        val mockActivity = mockSurveyHostActivity(isFinishing = true)
        sprigIntegration.setFragmentActivity(mockActivity)
        val trackEvent = provideTrackEvent(eventName = "Test Event")

        sprigIntegration.track(trackEvent)

        verify { Sprig.track(any<EventPayload>()) }
        verify(exactly = 0) { Sprig.trackAndPresent(any<EventPayload>(), any()) }
    }

    @Test
    fun `given integration is initialised, when track is called with properties, then properties are passed to EventPayload`() {
        sprigIntegration.create(mockSprigConfig)
        val properties = buildJsonObject {
            put("key1", "value1")
            put("key2", 42)
        }
        val trackEvent = provideTrackEvent(eventName = "Test Event", properties = properties)

        sprigIntegration.track(trackEvent)

        val payloadSlot = slot<EventPayload>()
        verify { Sprig.track(capture(payloadSlot)) }
        assertEquals("value1", payloadSlot.captured.properties?.get("key1"))
        assertEquals(42, payloadSlot.captured.properties?.get("key2"))
    }

    @Test
    fun `given integration is not initialised, when track is called, then no Sprig methods are called`() {
        val trackEvent = provideTrackEvent(eventName = "Test Event")

        sprigIntegration.track(trackEvent)

        verify(exactly = 0) { Sprig.track(any<EventPayload>()) }
        verify(exactly = 0) { Sprig.trackAndPresent(any<EventPayload>(), any()) }
    }

    // endregion

    // region reset

    @Test
    fun `given integration is initialised, when reset is called, then Sprig logout is called`() {
        sprigIntegration.create(mockSprigConfig)

        sprigIntegration.reset()

        verify { Sprig.logout() }
    }

    @Test
    fun `given integration is not initialised, when reset is called, then logout is not called`() {
        sprigIntegration.reset()

        verify(exactly = 0) { Sprig.logout() }
    }

    // endregion

    // region activity lifecycle

    @Test
    fun `given setFragmentActivity is called, when activity is destroyed, then currentActivity is cleared`() {
        sprigIntegration.create(mockSprigConfig)
        val mockActivity = mockSurveyHostActivity()
        sprigIntegration.setFragmentActivity(mockActivity)
        val trackEvent = provideTrackEvent(eventName = "Event Before Destroy")

        // Before destroy - should use trackAndPresent
        sprigIntegration.track(trackEvent)
        verify { Sprig.trackAndPresent(any<EventPayload>(), mockActivity) }

        // Destroy the activity
        sprigIntegration.onActivityDestroyed(mockActivity)

        // After destroy - should use track (no activity)
        val trackEvent2 = provideTrackEvent(eventName = "Event After Destroy")
        sprigIntegration.track(trackEvent2)
        verify { Sprig.track(any<EventPayload>()) }
    }

    @Test
    fun `given a non-FragmentActivity is resumed, when track is called, then Sprig track is used instead of trackAndPresent`() {
        sprigIntegration.create(mockSprigConfig)
        val regularActivity: Activity = mockk()
        sprigIntegration.onActivityResumed(regularActivity)
        val trackEvent = provideTrackEvent(eventName = "Test Event")

        sprigIntegration.track(trackEvent)

        verify { Sprig.track(any<EventPayload>()) }
        verify(exactly = 0) { Sprig.trackAndPresent(any<EventPayload>(), any()) }
    }

    // endregion
}

private fun Any.readFileAsJsonObject(fileName: String): JsonObject {
    this::class.java.classLoader?.getResourceAsStream(fileName).let { inputStream ->
        inputStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
    }.let { fileAsString ->
        return Json.parseToJsonElement(fileAsString).jsonObject
    }
}

@OptIn(InternalRudderApi::class)
private fun provideIdentifyEvent(
    userId: String = "",
    traits: Traits = emptyJsonObject,
) = IdentifyEvent(
    userIdentityState = UserIdentity(
        anonymousId = ANONYMOUS_ID,
        userId = userId,
        traits = traits,
    )
).also {
    it.applyMockedValues()
    it.context = buildJsonObject { put("traits", traits) }
    it.updateData(PlatformType.Mobile)
}

@OptIn(InternalRudderApi::class)
private fun provideTrackEvent(
    eventName: String,
    properties: JsonObject = emptyJsonObject,
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

private fun mockSurveyHostActivity(
    isFinishing: Boolean = false,
    isDestroyed: Boolean = false,
    lifecycleState: Lifecycle.State = Lifecycle.State.RESUMED,
): FragmentActivity = mockk {
    every { runOnUiThread(any()) } answers { firstArg<Runnable>().run() }
    every { this@mockk.isFinishing } returns isFinishing
    every { this@mockk.isDestroyed } returns isDestroyed
    every { lifecycle } returns mockk {
        every { currentState } returns lifecycleState
    }
}
