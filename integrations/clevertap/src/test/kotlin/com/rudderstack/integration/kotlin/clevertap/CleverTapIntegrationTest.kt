package com.rudderstack.integration.kotlin.clevertap

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.clevertap.android.sdk.CleverTapAPI
import com.rudderstack.sdk.kotlin.android.Analytics as AndroidAnalytics
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.SdkNotInitializedException
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.addLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.removeLifecycleObserver
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
import io.mockk.mockkStatic
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
import org.junit.jupiter.api.Assertions.assertThrows
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
    private lateinit var mockAnalytics: AndroidAnalytics

    @MockK
    private lateinit var mockApplication: Application

    @MockK
    private lateinit var mockConfiguration: Configuration

    @MockK
    private lateinit var mockCleverTap: CleverTapAPI

    @MockK
    private lateinit var mockActivity: Activity

    private lateinit var integration: CleverTapIntegration

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        mockkStatic(CleverTapAPI::class)
        mockkStatic("com.rudderstack.sdk.kotlin.android.utils.LifecycleManagementUtilsKt")
        every { CleverTapAPI.changeCredentials(any(), any()) } just Runs
        every { CleverTapAPI.changeCredentials(any(), any(), any()) } just Runs
        every { CleverTapAPI.setDebugLevel(any()) } just Runs
        every { CleverTapAPI.getDefaultInstance(any<Application>()) } returns mockCleverTap
        every { CleverTapAPI.setAppForeground(any()) } just Runs
        every { CleverTapAPI.onActivityResumed(any()) } just Runs
        every { CleverTapAPI.onActivityPaused() } just Runs
        every { mockAnalytics.addLifecycleObserver(any<ActivityLifecycleObserver>()) } just Runs
        every { mockAnalytics.removeLifecycleObserver(any<ActivityLifecycleObserver>()) } just Runs

        every { mockAnalytics.configuration } returns mockConfiguration
        every { mockConfiguration.application } returns mockApplication
        every { mockConfiguration.logLevel } returns Logger.LogLevel.DEBUG

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

            assertEquals(mockCleverTap, integration.getDestinationInstance())
        }

        @Test
        fun `when integration is initialised with region, then destination SDK is configured with regional credentials`() {
            integration.create(mockIntegrationConfig)

            verify(exactly = 1) { CleverTapAPI.changeCredentials(ACCOUNT_ID, ACCOUNT_TOKEN, REGION) }
            verify(exactly = 1) { CleverTapAPI.setDebugLevel(CleverTapAPI.LogLevel.DEBUG) }
            verify(exactly = 1) { CleverTapAPI.getDefaultInstance(mockApplication) }
            verify(exactly = 1) { mockAnalytics.addLifecycleObserver(integration) }
        }

        @Test
        fun `when integration is initialised without region, then destination SDK is configured with default credentials`() {
            integration.create(mockNewIntegrationConfig)

            verify(exactly = 1) { CleverTapAPI.changeCredentials(NEW_ACCOUNT_ID, NEW_ACCOUNT_TOKEN) }
            verify(exactly = 1) { CleverTapAPI.getDefaultInstance(mockApplication) }
        }

        @Test
        fun `given integration is already initialised, when create is called again, then SDK is not re-initialised`() {
            integration.create(mockIntegrationConfig)
            integration.create(mockNewIntegrationConfig)

            verify(exactly = 1) { CleverTapAPI.getDefaultInstance(mockApplication) }
            verify(exactly = 0) { CleverTapAPI.changeCredentials(NEW_ACCOUNT_ID, NEW_ACCOUNT_TOKEN) }
        }

        @Test
        fun `given empty config, when create is called, then SdkNotInitializedException is thrown`() {
            val exception = assertThrows(SdkNotInitializedException::class.java) {
                integration.create(emptyJsonObject)
            }

            assertEquals("CleverTapIntegration: Destination config is empty.", exception.message)
            verify(exactly = 0) { CleverTapAPI.getDefaultInstance(any<Application>()) }
        }

        @Test
        fun `given blank credentials, when create is called, then SdkNotInitializedException is thrown`() {
            val blankCredentialsConfig = buildJsonObject {
                put("accountId", " ")
                put("accountToken", "")
            }

            val exception = assertThrows(SdkNotInitializedException::class.java) {
                integration.create(blankCredentialsConfig)
            }

            assertEquals("CleverTapIntegration: Account ID or token is blank.", exception.message)
            verify(exactly = 0) { CleverTapAPI.getDefaultInstance(any<Application>()) }
        }

        @Test
        fun `given CleverTap returns no instance, when create is called, then SdkNotInitializedException is thrown`() {
            every { CleverTapAPI.getDefaultInstance(any<Application>()) } returns null

            val exception = assertThrows(SdkNotInitializedException::class.java) {
                integration.create(mockIntegrationConfig)
            }

            assertEquals("CleverTapIntegration: CleverTap SDK returned no instance.", exception.message)
            assertNull(integration.getDestinationInstance())
            verify(exactly = 0) { mockAnalytics.addLifecycleObserver(any<ActivityLifecycleObserver>()) }
        }

        @Test
        fun `given integration is initialised, when teardown is called, then observer is removed and instance is cleared`() {
            integration.create(mockIntegrationConfig)

            integration.teardown()

            assertNull(integration.getDestinationInstance())
            verify(exactly = 1) { mockAnalytics.removeLifecycleObserver(integration) }
        }

        @Test
        fun `given Rudder log levels, when mapped to CleverTap, then matching CleverTap levels are returned`() {
            assertEquals(CleverTapAPI.LogLevel.VERBOSE, Logger.LogLevel.VERBOSE.toCleverTapLogLevel())
            assertEquals(CleverTapAPI.LogLevel.DEBUG, Logger.LogLevel.DEBUG.toCleverTapLogLevel())
            assertEquals(CleverTapAPI.LogLevel.INFO, Logger.LogLevel.INFO.toCleverTapLogLevel())
            assertEquals(CleverTapAPI.LogLevel.INFO, Logger.LogLevel.WARN.toCleverTapLogLevel())
            assertEquals(CleverTapAPI.LogLevel.INFO, Logger.LogLevel.ERROR.toCleverTapLogLevel())
            assertEquals(CleverTapAPI.LogLevel.OFF, Logger.LogLevel.NONE.toCleverTapLogLevel())
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
            assertEquals("<anonymousId>", profileSlot.captured["anonymousId"])
            assertNotNull(profileSlot.captured["DOB"])
        }

        @Test
        fun `given identify event has nested address and company traits, when identify is called, then destination receives flattened traits`() {
            integration.create(mockIntegrationConfig)
            val traits = buildJsonObject {
                put("address", buildJsonObject {
                    put("city", "San Francisco")
                    put("country", "USA")
                })
                put("company", buildJsonObject {
                    put("id", "company-1")
                    put("name", "Acme")
                    put("role", "buyer")
                })
            }
            val profileSlot = slot<Map<String, Any>>()

            integration.identify(provideIdentifyEvent(traits = traits))

            verify { mockCleverTap.onUserLogin(capture(profileSlot)) }
            assertEquals("San Francisco", profileSlot.captured["city"])
            assertEquals("USA", profileSlot.captured["country"])
            assertEquals("company-1", profileSlot.captured["companyId"])
            assertEquals("Acme", profileSlot.captured["companyName"])
            assertEquals("buyer", profileSlot.captured["role"])
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

        @Test
        fun `given track event has blank name, when track is called, then destination is not invoked`() {
            integration.create(mockIntegrationConfig)

            integration.track(provideTrackEvent(eventName = " "))

            verify(exactly = 0) { mockCleverTap.pushEvent(any<String>()) }
            verify(exactly = 0) { mockCleverTap.pushEvent(any<String>(), any<Map<String, Any>>()) }
            verify(exactly = 0) { mockCleverTap.pushChargedEvent(any(), any()) }
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

        @Test
        fun `given screen event has no properties, when screen is called, then destination receives event name only`() {
            integration.create(mockIntegrationConfig)

            integration.screen(provideScreenEvent(screenName = "Home"))

            verify { mockCleverTap.pushEvent("Screen Viewed: Home") }
            verify(exactly = 0) { mockCleverTap.pushEvent(any<String>(), any<Map<String, Any>>()) }
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

            verify { CleverTapAPI.setAppForeground(true) }
            verify { mockCleverTap.pushNotificationClickedEvent(extras) }
            verify { mockCleverTap.pushDeepLink(uri) }
        }

        @Test
        fun `given activity is resumed, when lifecycle callback is received, then CleverTap resume is invoked`() {
            integration.create(mockIntegrationConfig)

            integration.onActivityResumed(mockActivity)

            verify { CleverTapAPI.onActivityResumed(mockActivity) }
        }

        @Test
        fun `given activity is paused, when lifecycle callback is received, then CleverTap pause is invoked`() {
            integration.create(mockIntegrationConfig)

            integration.onActivityPaused(mockActivity)

            verify { CleverTapAPI.onActivityPaused() }
        }

        @Test
        fun `given CleverTap is not created, when lifecycle callbacks are received, then destination is not invoked`() {
            integration.onActivityCreated(mockActivity, null)
            integration.onActivityResumed(mockActivity)
            integration.onActivityPaused(mockActivity)

            verify(exactly = 0) { CleverTapAPI.setAppForeground(any()) }
            verify(exactly = 0) { CleverTapAPI.onActivityResumed(any()) }
            verify(exactly = 0) { CleverTapAPI.onActivityPaused() }
            verify(exactly = 0) { mockCleverTap.pushNotificationClickedEvent(any()) }
            verify(exactly = 0) { mockCleverTap.pushDeepLink(any()) }
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

        @Test
        fun `given deep link uri, when helper is called, then destination receives deep link`() {
            integration.create(mockIntegrationConfig)
            val uri = mockk<Uri>()

            integration.pushDeepLink(uri)

            verify { mockCleverTap.pushDeepLink(uri) }
        }

        @Test
        fun `given foreground state, when helper is called, then CleverTap foreground state is updated`() {
            integration.setAppForeground(true)

            verify { CleverTapAPI.setAppForeground(true) }
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
