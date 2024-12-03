package com.rudderstack.sampleapp.analytics.customplugins

import android.app.Application
import android.content.Context
import com.rudderstack.android.sdk.Analytics
import com.rudderstack.android.sdk.Configuration
import com.rudderstack.kotlin.sdk.internals.models.TrackEvent
import com.rudderstack.kotlin.sdk.internals.models.emptyJsonObject
import com.rudderstack.kotlin.sdk.internals.utils.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        plugin = spyk(AndroidAdvertisingIdPlugin(testScope), recordPrivateCalls = true)

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
        runTest(testDispatcher) {
            plugin.setup(mockAnalytics)

            advanceUntilIdle()

            coVerify { plugin.updateAdvertisingId() }
        }

    @Test
    fun `given setup is called, when updateAdvertisingId is executed, then verify that getAdvertisingId is triggered`() =
        runTest(testDispatcher) {
            plugin.setup(mockAnalytics)

            advanceUntilIdle()

            coVerify { plugin.getAdvertisingId(mockContext) }
        }

    @Test
    fun `given setup is called, when updateAdvertisingId is called, then verify that getGooglePlayServicesAdvertisingID is called`() =
        runTest(testDispatcher) {
            plugin.setup(mockAnalytics)

            advanceUntilIdle()

            coVerify { plugin.getGooglePlayServicesAdvertisingID(mockContext) }
        }

    @Test
    fun `given Google Play advertising ID is fetched successfully, when verify that getAmazonFireAdvertisingID is not called`() =
        runTest(testDispatcher) {
            val successResult = Result.Success(FAKE_ADVERTISING_ID)

            coEvery { plugin.getGooglePlayServicesAdvertisingID(mockContext) } returns successResult

            coVerify(exactly = 0) { plugin.getAmazonFireAdvertisingID(mockContext) }
        }

    @Test
    fun `given Google Play advertising ID is not fetched successfully, when Google Play fails and updateAdvertisingId is called, then Fire advertising ID is set`() =
        runTest(testDispatcher) {
            val errorResult = Result.Failure(error = Exception("Error collecting play services ad id."))
            val amazonFireId = FAKE_ADVERTISING_ID
            val successAmazonResult = Result.Success(amazonFireId)

            coEvery { plugin.getGooglePlayServicesAdvertisingID(mockContext) } returns errorResult
            coEvery { plugin.getAmazonFireAdvertisingID(mockContext) } returns successAmazonResult

            plugin.setup(mockAnalytics)

            advanceUntilIdle()

            assertTrue(plugin.adTrackingEnabled)
            assertEquals(amazonFireId, plugin.advertisingId)
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

            coEvery { plugin.attachAdvertisingId(updatedMessage) } returns updatedMessage

            assertEquals(updatedMessage, plugin.execute(updatedMessage))
        }
}
