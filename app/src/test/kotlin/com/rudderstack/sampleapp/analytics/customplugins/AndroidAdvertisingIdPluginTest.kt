package com.rudderstack.sampleapp.analytics.customplugins

import android.app.Application
import android.content.Context
import com.rudderstack.android.sdk.Analytics
import com.rudderstack.android.sdk.Configuration
import com.rudderstack.kotlin.sdk.internals.models.TrackEvent
import com.rudderstack.kotlin.sdk.internals.models.emptyJsonObject
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

private const val DEVICE = "device"
private const val FAKE_ADVERTISING_ID = "fake-id"

private const val DEVICE_ADVERTISING_ID_KEY = "advertisingId"
private const val DEVICE_AD_TRACKING_ENABLED_KEY = "adTrackingEnabled"

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidAdvertisingIdPluginTest {

    private lateinit var plugin: AndroidAdvertisingIdPlugin
    private lateinit var mockAnalytics: Analytics
    private lateinit var mockContext: Context
    private lateinit var mockConfiguration: Configuration

    private val mockApplication: Application = mockk()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        plugin = spyk(AndroidAdvertisingIdPlugin(), recordPrivateCalls = true)

        mockAnalytics = mockk(relaxed = true)
        mockConfiguration = mockk(relaxed = true)
        mockContext = mockk(relaxed = true)

        every { mockAnalytics.configuration } returns mockConfiguration
        every { mockConfiguration.application } returns mockApplication
        every { mockApplication.applicationContext } returns mockContext

    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given setup is called, when executed, then application is set then verify that updateAdvertisingId is triggered`() =
        runTest {
            plugin.setup(mockAnalytics)

            coVerify { plugin["updateAdvertisingId"]() }
        }

    @Test
    fun `given setup is called, when updateAdvertisingId is executed, then verify that getAdvertisingId is triggered`() =
        runTest {
            plugin.setup(mockAnalytics)

            coVerify { plugin["getAdvertisingId"](mockContext) }
        }

    @Test
    fun `given setup is called, when updateAdvertisingId is called, then verify that getGooglePlayServicesAdvertisingID is called`() =
        runTest {
            plugin.setup(mockAnalytics)

            coVerify { plugin["getGooglePlayServicesAdvertisingID"](mockContext) }
        }

    @Test
    fun `given Google Play advertising ID is fetched successfully, when verify that getAmazonFireAdvertisingID is not called`() =
        runTest {
            val successResult = AndroidAdvertisingIdPlugin.Result.Success(FAKE_ADVERTISING_ID)

            coEvery { plugin["getGooglePlayServicesAdvertisingID"](mockContext) } returns successResult

            coVerify(exactly = 0) { plugin["getAmazonFireAdvertisingID"](mockContext) }
        }

    @Test
    fun `given Google Play advertising ID is not fetched successfully, when verify that getAmazonFireAdvertisingID is called`() =
        runTest {
            val errorResult = AndroidAdvertisingIdPlugin.Result.Error(Exception("Error collecting play services ad id."))

            coEvery { plugin["getGooglePlayServicesAdvertisingID"](mockContext) } returns errorResult

            coVerify(exactly = 1) { plugin["getAmazonFireAdvertisingID"](mockContext) }
        }

    @Test
    fun `given advertising ID returns updated message, verify that execute method returns the updated message`() =
        runTest(testDispatcher) {
            val updatedMessage = TrackEvent(
                event = "Sample Event",
                properties = emptyJsonObject,
            )
            updatedMessage.context = buildJsonObject {
                put(DEVICE, buildJsonObject {
                    put(DEVICE_ADVERTISING_ID_KEY, FAKE_ADVERTISING_ID)
                    put(DEVICE_AD_TRACKING_ENABLED_KEY, true)
                })
            }

            coEvery { plugin["attachAdvertisingId"](updatedMessage) } returns updatedMessage

            Assert.assertEquals(updatedMessage, plugin.execute(updatedMessage))
        }
}