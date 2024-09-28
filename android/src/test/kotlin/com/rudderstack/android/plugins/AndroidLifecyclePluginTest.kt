package com.rudderstack.android.plugins

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.rudderstack.android.sdk.Configuration
import com.rudderstack.android.sdk.models.AppVersion
import com.rudderstack.android.sdk.plugins.APPLICATION_BACKGROUNDED
import com.rudderstack.android.sdk.plugins.APPLICATION_INSTALLED
import com.rudderstack.android.sdk.plugins.APPLICATION_OPENED
import com.rudderstack.android.sdk.plugins.APPLICATION_UPDATED
import com.rudderstack.android.sdk.plugins.AndroidLifecyclePlugin
import com.rudderstack.android.sdk.plugins.BUILD_KEY
import com.rudderstack.android.sdk.plugins.FROM_BACKGROUND
import com.rudderstack.android.sdk.plugins.VERSION_KEY
import com.rudderstack.android.utils.MockMemoryStorage
import com.rudderstack.android.utils.mockAnalytics
import com.rudderstack.kotlin.internals.models.Properties
import com.rudderstack.kotlin.internals.models.RudderOption
import com.rudderstack.kotlin.internals.storage.Storage
import com.rudderstack.kotlin.internals.storage.StorageKeys
import com.rudderstack.kotlin.internals.utils.empty
import io.mockk.MockKAnnotations
import io.mockk.Ordering
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Before
import org.junit.Test

class AndroidLifecyclePluginTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val plugin = spyk(AndroidLifecyclePlugin())

    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)

    @MockK
    private lateinit var mockLifecycleOwner: LifecycleOwner

    @MockK
    private lateinit var mockLifecycle: Lifecycle

    private lateinit var mockStorage: Storage

    @MockK
    private lateinit var mockApplication: Application

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)

        mockStorage = MockMemoryStorage()

        every { plugin.getProcessLifecycle() } returns mockLifecycle

        every { mockAnalytics.track(any<String>(), any<JsonObject>(), any<RudderOption>()) } returns Unit
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given trackApplicationLifecycleEvents is false, when lifecycle method is called, then do not track lifecycle events`() {
        // given
        pluginSetup(trackingEnabled = false)

        // when
        plugin.onStart(mockLifecycleOwner)

        // then
        verify(exactly = 0) { mockAnalytics.track(any<String>(), any<JsonObject>(), any<RudderOption>()) }
    }

    @Test
    fun `given trackApplicationLifecycleEvents is true and storage does not have build and version stored, when setup and onStart are called, then events called with correct properties`() =
        runTest(testDispatcher) {
            // given
            pluginSetup()

            val installedProperties = buildJsonObject {
                put(VERSION_KEY, "1.0.0")
                put(BUILD_KEY, 100L)
            }
            val openedProperties = buildJsonObject {
                put(FROM_BACKGROUND, false)
                put(VERSION_KEY, "1.0.0")
            }

            // when
            plugin.onStart(mockLifecycleOwner)

            // Advance the test dispatcher to run coroutines
            testDispatcher.scheduler.advanceUntilIdle()

            // then
            verify(exactly = 1) {
                mockAnalytics.track(
                    name = eq(APPLICATION_INSTALLED),
                    options = eq(RudderOption()),
                    properties = eq(installedProperties)
                )
            }
            verify(exactly = 1) {
                mockAnalytics.track(
                    name = eq(APPLICATION_OPENED),
                    options = eq(RudderOption()),
                    properties = eq(openedProperties)
                )
            }
            // verify that storage has now the correct version and build written
            assert(mockStorage.readLong(StorageKeys.APP_BUILD, -1L) == 100L)
            assert(mockStorage.readString(StorageKeys.APP_VERSION, "") == "1.0.0")
        }

    @Test
    fun `given trackApplicationLifecycleEvents is true and storage does have build and version stored, when setup and onStart are called, then events called with correct properties`() =
        runTest(testDispatcher) {
            // given
            mockStorage.write(StorageKeys.APP_BUILD, 100L)
            mockStorage.write(StorageKeys.APP_VERSION, "1.0.0")
            pluginSetup()

            val installedProperties = buildJsonObject {
                put(VERSION_KEY, "1.0.0")
                put(BUILD_KEY, 100L)
            }
            val openedProperties = buildJsonObject {
                put(FROM_BACKGROUND, false)
                put(VERSION_KEY, "1.0.0")
            }

            // when
            plugin.onStart(mockLifecycleOwner)

            // then
            verify(exactly = 0) {
                mockAnalytics.track(
                    name = eq(APPLICATION_INSTALLED),
                    options = eq(RudderOption()),
                    properties = eq(installedProperties)
                )
            }
            verify(exactly = 1) {
                mockAnalytics.track(
                    name = eq(APPLICATION_OPENED),
                    options = eq(RudderOption()),
                    properties = eq(openedProperties)
                )
            }
        }

    @Test
    fun `given trackApplicationLifecycleEvents is true and storage does have build and version stored, when setup, onStart, onStop, onStart are called in order, then events called with correct properties`() =
        runTest(testDispatcher) {
            // given
            mockStorage.write(StorageKeys.APP_BUILD, 100L)
            mockStorage.write(StorageKeys.APP_VERSION, "1.0.0")
            pluginSetup()

            val installedProperties = buildJsonObject {
                put(VERSION_KEY, "1.0.0")
                put(BUILD_KEY, 100L)
            }
            val openedFirstTimeProperties = buildJsonObject {
                put(FROM_BACKGROUND, false)
                put(VERSION_KEY, "1.0.0")
            }
            val openedSecondTimeProperties = buildJsonObject {
                put(FROM_BACKGROUND, true)
            }

            // when
            plugin.onStart(mockLifecycleOwner)
            plugin.onStop(mockLifecycleOwner)
            plugin.onStart(mockLifecycleOwner)

            // then
            verify(exactly = 0) {
                mockAnalytics.track(
                    name = eq(APPLICATION_INSTALLED),
                    options = eq(RudderOption()),
                    properties = eq(installedProperties)
                )
            }
            verify(ordering = Ordering.ORDERED) {
                mockAnalytics.track(
                    name = eq(APPLICATION_OPENED),
                    options = eq(RudderOption()),
                    properties = eq(openedFirstTimeProperties)
                )
                mockAnalytics.track(
                    name = eq(APPLICATION_BACKGROUNDED),
                    options = eq(RudderOption()),
                    properties = eq(Properties(emptyMap()))
                )
                mockAnalytics.track(
                    name = eq(APPLICATION_OPENED),
                    options = eq(RudderOption()),
                    properties = eq(openedSecondTimeProperties)
                )
            }
        }

    @Test
    fun `given trackApplicationLifecycleEvents is true and stored and current versions are different, when setup and onStart called, then events called with correct properties`() =
        runTest(testDispatcher) {
            // given
            val oldVersion = "0.5.1"
            val oldBuildNumber = 99L
            val installedProperties = buildJsonObject {
                put(VERSION_KEY, "1.0.0")
                put(BUILD_KEY, 100L)
            }
            val updatedProperties = buildJsonObject {
                put("previous_$VERSION_KEY", oldVersion)
                put("previous_$BUILD_KEY", oldBuildNumber)
                put(VERSION_KEY, "1.0.0")
                put(BUILD_KEY, 100L)
            }
            val openedProperties = buildJsonObject {
                put(FROM_BACKGROUND, false)
                put(VERSION_KEY, "1.0.0")
            }
            mockStorage.write(StorageKeys.APP_BUILD, oldBuildNumber)
            mockStorage.write(StorageKeys.APP_VERSION, oldVersion)
            pluginSetup()

            // when
            plugin.onStart(mockLifecycleOwner)

            // then
            verify(exactly = 0) {
                mockAnalytics.track(
                    name = eq(APPLICATION_INSTALLED),
                    options = eq(RudderOption()),
                    properties = eq(installedProperties)
                )
            }
            verify(exactly = 1) {
                mockAnalytics.track(
                    name = eq(APPLICATION_UPDATED),
                    options = eq(RudderOption()),
                    properties = eq(updatedProperties)
                )
            }
            verify(exactly = 1) {
                mockAnalytics.track(
                    name = eq(APPLICATION_OPENED),
                    options = eq(RudderOption()),
                    properties = eq(openedProperties)
                )
            }
        }

    @Test
    fun `given trackApplicationLifecycleEvents is true, when teardown is called, then observer is removed from lifecycle`() =
        runTest(testDispatcher) {
            // given
            pluginSetup()

            // when
            plugin.teardown()
            testDispatcher.scheduler.advanceUntilIdle()

            // then
            verify(exactly = 1) {
                mockLifecycle.removeObserver(plugin)
            }
        }

    @Test
    fun `given trackApplicationLifecycleEvents is false, when setup is called, then build and version are still stored in memory`() =
        runTest(testDispatcher) {
            // when
            pluginSetup(false)
            testDispatcher.scheduler.advanceUntilIdle()

            // then
            assert(mockStorage.readLong(StorageKeys.APP_BUILD, -1L) == 100L)
            assert(mockStorage.readString(StorageKeys.APP_VERSION, "") == "1.0.0")
        }

    private fun pluginSetup(trackingEnabled: Boolean = true) {

        val mockConfiguration = mockk<Configuration> {
            every { application } returns mockApplication
            every { storage } returns mockStorage
            every { trackApplicationLifecycleEvents } returns trackingEnabled
            every { writeKey } returns ""
            every { dataPlaneUrl } returns ""
            every { logger } returns mockk(relaxed = true)
            every { flushPolicies } returns emptyList()
        }

        every { mockAnalytics.configuration } returns mockConfiguration

        every { plugin.getAppVersion() } returns AppVersion(
            currentBuild = 100L,
            currentVersionName = "1.0.0",
            previousBuild = mockStorage.readLong(StorageKeys.APP_BUILD, -1L),
            previousVersionName = mockStorage.readString(StorageKeys.APP_VERSION, String.empty())
        )

        plugin.setup(analytics = mockAnalytics)
    }
}
