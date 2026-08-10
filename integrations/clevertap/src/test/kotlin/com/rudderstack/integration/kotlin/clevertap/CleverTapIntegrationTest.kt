package com.rudderstack.integration.kotlin.clevertap

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.rudderstack.sdk.kotlin.android.utils.application
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.UserIdentity
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private const val PATH_TO_CONFIG = "config/clevertap_config.json"
private const val PATH_TO_NEW_CONFIG = "config/new_clevertap_config.json"
private const val ACCOUNT_ID = "TEST-ACCOUNT-ID"
private const val ACCOUNT_TOKEN = "TEST-ACCOUNT-TOKEN"
private const val REGION = "in1"
private const val NEW_ACCOUNT_ID = "NEW-ACCOUNT-ID"
private const val NEW_ACCOUNT_TOKEN = "NEW-ACCOUNT-TOKEN"

class CleverTapIntegrationTest {

    private val mockIntegrationConfig: JsonObject = readFileAsJsonObject(PATH_TO_CONFIG)
    private val mockNewIntegrationConfig: JsonObject = readFileAsJsonObject(PATH_TO_NEW_CONFIG)

    @MockK
    private lateinit var mockAnalytics: Analytics

    @MockK
    private lateinit var mockApplication: Application

    @MockK
    private lateinit var mockCleverTap: CleverTapClient

    @MockK
    private lateinit var mockActivity: Activity

    private val mockDestinationInstance = Any()

    private lateinit var integration: CleverTapIntegration

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        mockkObject(CleverTapSdk)
        every { CleverTapSdk.changeCredentials(any(), any()) } just Runs
        every { CleverTapSdk.changeCredentials(any(), any(), any()) } just Runs
        every { CleverTapSdk.setLogLevel(any()) } just Runs
        every { CleverTapSdk.getDefaultInstance(any()) } returns mockCleverTap
        every { CleverTapSdk.setAppForeground(any()) } just Runs
        every { CleverTapSdk.onActivityResumed(any()) } just Runs
        every { CleverTapSdk.onActivityPaused() } just Runs

        every { mockAnalytics.application } returns mockApplication
        every { mockAnalytics.configuration } returns mockk(relaxed = true)
        every { mockAnalytics.configuration.logLevel } returns Logger.LogLevel.DEBUG

        every { mockCleverTap.instance } returns mockDestinationInstance
        every { mockCleverTap.onUserLogin(any()) } just Runs
        every { mockCleverTap.pushEvent(any<String>()) } just Runs
        every { mockCleverTap.pushEvent(any<String>(), any<Map<String, Any>>()) } just Runs
        every { mockCleverTap.pushChargedEvent(any(), any()) } just Runs
        every { mockCleverTap.pushNotificationClickedEvent(any()) } just Runs
        every { mockCleverTap.pushDeepLink(any()) } just Runs

        integration = CleverTapIntegration().also { it.analytics = mockAnalytics }
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Nested
    inner class Create {

        @Test
        fun `given integration is not initialised, when instance is requested, then null is returned`() {
            assertNull(CleverTapIntegration().getDestinationInstance())
        }

        @Test
        fun `given integration is initialised, when instance is requested, then destination SDK instance is returned`() {
            integration.create(mockIntegrationConfig)

            assertEquals(mockDestinationInstance, integration.getDestinationInstance())
        }

        @Test
        fun `when integration is initialised with region, then destination SDK is configured with regional credentials`() {
            integration.create(mockIntegrationConfig)

            verify(exactly = 1) { CleverTapSdk.changeCredentials(ACCOUNT_ID, ACCOUNT_TOKEN, REGION) }
            verify(exactly = 1) { CleverTapSdk.setLogLevel(Logger.LogLevel.DEBUG) }
            verify(exactly = 1) { CleverTapSdk.getDefaultInstance(mockApplication) }
        }

        @Test
        fun `when integration is initialised without region, then destination SDK is configured with default credentials`() {
            integration.create(mockNewIntegrationConfig)

            verify(exactly = 1) { CleverTapSdk.changeCredentials(NEW_ACCOUNT_ID, NEW_ACCOUNT_TOKEN) }
            verify(exactly = 1) { CleverTapSdk.getDefaultInstance(mockApplication) }
        }

        @Test
        fun `given integration is already initialised, when create is called again, then SDK is not re-initialised`() {
            integration.create(mockIntegrationConfig)
            integration.create(mockNewIntegrationConfig)

            verify(exactly = 1) { CleverTapSdk.getDefaultInstance(mockApplication) }
            verify(exactly = 0) { CleverTapSdk.changeCredentials(NEW_ACCOUNT_ID, NEW_ACCOUNT_TOKEN) }
        }
    }

    @Nested
    inner class Identify {

        @Test
        fun `given identify event has traits, when identify is called, then destination receives transformed profile`() {
            integration.create(mockIntegrationConfig)
            val traits = buildJsonObject {
                put("id", "user-123")
                put("name", "Jane Doe")
                put("email", "jane@example.com")
                put("phone", "+15551234567")
                put("gender", "female")
                put("birthday", "1990-01-02")
                put("custom", "value")
            }
            val profileSlot = slot<Map<String, Any>>()

            integration.identify(provideIdentifyEvent(traits = traits))

            verify { mockCleverTap.onUserLogin(capture(profileSlot)) }
            assertEquals("user-123", profileSlot.captured["Identity"])
            assertEquals("Jane Doe", profileSlot.captured["Name"])
            assertEquals("jane@example.com", profileSlot.captured["Email"])
            assertEquals("+15551234567", profileSlot.captured["Phone"])
            assertEquals("F", profileSlot.captured["Gender"])
            assertEquals("value", profileSlot.captured["custom"])
            assertNotNull(profileSlot.captured["DOB"])
        }
    }

    @Nested
    inner class Track {

        @Test
        fun `given track event has no properties, when track is called, then destination receives event name only`() {
            integration.create(mockIntegrationConfig)

            integration.track(provideTrackEvent(eventName = "Product Viewed"))

            verify { mockCleverTap.pushEvent("Product Viewed") }
        }

        @Test
        fun `given track event has properties, when track is called, then destination receives event properties`() {
            integration.create(mockIntegrationConfig)
            val properties = buildJsonObject {
                put("product_id", "p-123")
                put("price", 12.5)
            }
            val propertiesSlot = slot<Map<String, Any>>()

            integration.track(provideTrackEvent(eventName = "Product Viewed", properties = properties))

            verify { mockCleverTap.pushEvent("Product Viewed", capture(propertiesSlot)) }
            assertEquals("p-123", propertiesSlot.captured["product_id"])
            assertEquals(12.5, propertiesSlot.captured["price"])
        }

        @Test
        fun `given order completed event, when track is called, then destination receives charged event`() {
            integration.create(mockIntegrationConfig)
            val chargeDetailsSlot = slot<HashMap<String, Any>>()
            val itemsSlot = slot<ArrayList<HashMap<String, Any>>>()
            val properties = buildJsonObject {
                put("order_id", "order-1")
                put("revenue", "42.50")
                put("currency", "USD")
                put("products", buildJsonArray {
                    add(buildJsonObject {
                        put("product_id", "sku-1")
                        put("name", "T-Shirt")
                    })
                })
            }

            integration.track(provideTrackEvent(eventName = "Order Completed", properties = properties))

            verify { mockCleverTap.pushChargedEvent(capture(chargeDetailsSlot), capture(itemsSlot)) }
            assertEquals("order-1", chargeDetailsSlot.captured["Charged ID"])
            assertEquals(42.5, chargeDetailsSlot.captured["Amount"])
            assertEquals("USD", chargeDetailsSlot.captured["currency"])
            assertEquals("sku-1", itemsSlot.captured.first()["id"])
            assertEquals("T-Shirt", itemsSlot.captured.first()["name"])
        }
    }

    @Nested
    inner class Screen {

        @Test
        fun `given screen event has properties, when screen is called, then destination receives screen viewed event`() {
            integration.create(mockIntegrationConfig)
            val properties = buildJsonObject { put("section", "hero") }
            val propertiesSlot = slot<Map<String, Any>>()

            integration.screen(provideScreenEvent(screenName = "Home", properties = properties))

            verify { mockCleverTap.pushEvent("Screen Viewed: Home", capture(propertiesSlot)) }
            assertEquals("hero", propertiesSlot.captured["section"])
        }
    }

    @Nested
    inner class ActivityLifecycle {

        @Test
        fun `given activity has push extras and deep link, when activity is created, then destination receives push callbacks`() {
            integration.create(mockIntegrationConfig)
            val extras = mockk<Bundle>()
            val uri = mockk<Uri>()
            val intent = mockk<Intent>()
            every { intent.extras } returns extras
            every { intent.data } returns uri
            every { mockActivity.intent } returns intent

            integration.onActivityCreated(mockActivity, null)

            verify { CleverTapSdk.setAppForeground(true) }
            verify { mockCleverTap.pushNotificationClickedEvent(extras) }
            verify { mockCleverTap.pushDeepLink(uri) }
        }

        @Test
        fun `given activity is resumed, when lifecycle callback is received, then CleverTap resume is invoked`() {
            integration.create(mockIntegrationConfig)

            integration.onActivityResumed(mockActivity)

            verify { CleverTapSdk.onActivityResumed(mockActivity) }
        }

        @Test
        fun `given activity is paused, when lifecycle callback is received, then CleverTap pause is invoked`() {
            integration.create(mockIntegrationConfig)

            integration.onActivityPaused(mockActivity)

            verify { CleverTapSdk.onActivityPaused() }
        }
    }

    @Nested
    inner class PublicHelpers {

        @Test
        fun `given push notification extras, when helper is called, then destination receives notification click`() {
            integration.create(mockIntegrationConfig)
            val extras = mockk<Bundle>()

            integration.pushNotificationClickedEvent(extras)

            verify { mockCleverTap.pushNotificationClickedEvent(extras) }
        }
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
        it.originalTimestamp = "<original-timestamp>"
        it.context = emptyJsonObject
        it.messageId = "<message-id>"
        it.updateData(PlatformType.Mobile)
    }

    @OptIn(InternalRudderApi::class)
    private fun provideScreenEvent(
        screenName: String,
        properties: JsonObject = emptyJsonObject,
    ) = ScreenEvent(
        screenName = screenName,
        properties = properties,
        options = RudderOption(),
    ).also {
        it.originalTimestamp = "<original-timestamp>"
        it.context = emptyJsonObject
        it.messageId = "<message-id>"
        it.updateData(PlatformType.Mobile)
    }

    @OptIn(InternalRudderApi::class)
    private fun provideIdentifyEvent(
        userId: String = "test-user",
        traits: JsonObject = emptyJsonObject,
    ) = IdentifyEvent(
        options = RudderOption(),
        userIdentityState = UserIdentity(
            anonymousId = "<anonymousId>",
            userId = userId,
            traits = traits,
        ),
    ).also {
        it.originalTimestamp = "<original-timestamp>"
        it.context = buildJsonObject { put("traits", traits) }
        it.messageId = "<message-id>"
        it.updateData(PlatformType.Mobile)
    }
}
