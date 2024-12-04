package com.rudderstack.android.sdk.plugins

import android.app.Application
import androidx.lifecycle.LifecycleOwner
import com.rudderstack.android.sdk.Configuration
import com.rudderstack.android.sdk.models.AppVersion
import com.rudderstack.android.sdk.utils.MockMemoryStorage
import com.rudderstack.android.sdk.utils.addLifecycleObserver
import com.rudderstack.android.sdk.utils.mockAnalytics
import com.rudderstack.android.sdk.utils.removeLifecycleObserver
import com.rudderstack.kotlin.sdk.internals.models.Properties
import com.rudderstack.kotlin.sdk.internals.models.RudderOption
import com.rudderstack.kotlin.sdk.internals.storage.Storage
import com.rudderstack.kotlin.sdk.internals.storage.StorageKeys
import com.rudderstack.kotlin.sdk.internals.utils.empty
import io.mockk.MockKAnnotations
import io.mockk.Ordering
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
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
import com.rudderstack.android.sdk.Analytics as AndroidAnalytics

class AndroidLifecyclePluginTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val plugin = spyk(AndroidLifecyclePlugin())

    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)

    @MockK
    private lateinit var mockLifecycleOwner: LifecycleOwner

    private lateinit var mockStorage: Storage

    @MockK
    private lateinit var mockApplication: Application

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)

        mockStorage = MockMemoryStorage()

        every { (mockAnalytics as AndroidAnalytics).addLifecycleObserver(plugin) } just Runs
        every { mockAnalytics.track(any<String>(), any<JsonObject>(), any<RudderOption>()) } returns Unit
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given trackApplicationLifecycleEvents is false, when plugin is setup, then addObserver is not called`() {
        pluginSetup(trackingEnabled = false)

        verify(exactly = 0) { (mockAnalytics as AndroidAnalytics).addLifecycleObserver(plugin) }
    }

    @Test
    fun `given trackApplicationLifecycleEvents is true, when plugin is setup, then addObserver is called`() {
        pluginSetup(trackingEnabled = true)

        verify(exactly = 1) { (mockAnalytics as AndroidAnalytics).addLifecycleObserver(plugin) }
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
    fun `given trackApplicationsLifecycleEvents is true and stored version is null, when setup and onStart called, then events called with correct properties`() =
        runTest {
            val oldBuildNumber = 99L
            val installedProperties = buildJsonObject {
                put(BUILD_KEY, 100L)
            }
            val updatedProperties = buildJsonObject {
                put(VERSION_KEY, "1.0.0")
                put("previous_$BUILD_KEY", oldBuildNumber)
                put(BUILD_KEY, 100L)
            }
            val openedProperties = buildJsonObject {
                put(VERSION_KEY, "1.0.0")
                put(FROM_BACKGROUND, false)
            }
            mockStorage.write(StorageKeys.APP_BUILD, oldBuildNumber)

            // when
            pluginSetup()
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
    fun `given trackApplicationLifecycleEvents is false, when setup is called, then build and version are still stored in memory`() =
        runTest(testDispatcher) {
            // when
            pluginSetup(false)
            testDispatcher.scheduler.advanceUntilIdle()

            // then
            assert(mockStorage.readLong(StorageKeys.APP_BUILD, -1L) == 100L)
            assert(mockStorage.readString(StorageKeys.APP_VERSION, "") == "1.0.0")
        }

    @Test
    fun `when teardown called, then removeLifecycleObserver is called`() = runTest {
        plugin.setup(mockAnalytics)

        plugin.teardown()

        verify { (mockAnalytics as AndroidAnalytics).removeLifecycleObserver(plugin) }
    }

    private fun pluginSetup(trackingEnabled: Boolean = true) {

        val mockConfiguration = mockk<Configuration> {
            every { application } returns mockApplication
            every { storage } returns mockStorage
            every { trackApplicationLifecycleEvents } returns trackingEnabled
            every { writeKey } returns ""
            every { dataPlaneUrl } returns ""
            every { flushPolicies } returns emptyList()
        }

        every { mockAnalytics.configuration } returns mockConfiguration

        every { plugin.getAppVersion() } returns AppVersion(
            currentBuild = 100L,
            currentVersionName = "1.0.0",
            previousBuild = mockStorage.readLong(StorageKeys.APP_BUILD, -1L),
            previousVersionName = mockStorage.readString(StorageKeys.APP_VERSION, String.empty()).ifEmpty { null }
        )

        plugin.setup(analytics = mockAnalytics)
    }
}
