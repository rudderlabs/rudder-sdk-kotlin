package com.rudderstack.android.plugins

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.rudderstack.android.utils.MockMemoryStorage
import com.rudderstack.core.Analytics
import com.rudderstack.core.internals.models.Properties
import com.rudderstack.core.internals.models.RudderOption
import com.rudderstack.core.internals.storage.Storage
import com.rudderstack.core.internals.storage.StorageKeys
import io.mockk.Ordering
import io.mockk.every
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
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.rudderstack.android.plugins.AndroidLifecyclePlugin.Companion.APPLICATION_INSTALLED
import com.rudderstack.android.plugins.AndroidLifecyclePlugin.Companion.BUILD_KEY
import kotlinx.serialization.json.put
import com.rudderstack.android.plugins.AndroidLifecyclePlugin.Companion.APPLICATION_BACKGROUNDED
import com.rudderstack.android.plugins.AndroidLifecyclePlugin.Companion.APPLICATION_OPENED
import com.rudderstack.android.plugins.AndroidLifecyclePlugin.Companion.APPLICATION_UPDATED
import com.rudderstack.android.plugins.AndroidLifecyclePlugin.Companion.FROM_BACKGROUND
import com.rudderstack.android.plugins.AndroidLifecyclePlugin.Companion.VERSION_KEY
import java.util.concurrent.atomic.AtomicBoolean

class AndroidLifecyclePluginTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var plugin: AndroidLifecyclePlugin
    private lateinit var analytics: Analytics
    private lateinit var lifecycle: Lifecycle
    private lateinit var storage: Storage
    private lateinit var packageInfo: PackageInfo
    private lateinit var application: Application

    private var shouldTrackApplicationLifecycleEvents: Boolean = true
    private val firstLaunch = AtomicBoolean(false)
    private val trackedApplicationLifecycleEvents = AtomicBoolean(false)

    private val lifecycleOwner: LifecycleOwner = mockk(relaxed = true)

    @Before
    @ExperimentalCoroutinesApi
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        packageInfo = PackageInfo()
        packageInfo.versionCode = 100
        packageInfo.versionName = "1.0.0"

        storage = MockMemoryStorage()
        application = mockk(relaxed = true)
        lifecycle = mockk(relaxed = true)
        analytics = mockk(relaxed = true)
        every { analytics.analyticsScope } returns testScope
        every { analytics.analyticsDispatcher } returns testDispatcher
        every { analytics.mainDispatcher } returns testDispatcher
        every { analytics.track(any<String>(), any<JsonObject>(), any<RudderOption>()) } returns Unit

        val packageManager = mockk<PackageManager> {
            every { getPackageInfo("com.foo", 0) } returns packageInfo
        }
        every { application.packageName } returns "com.foo"
        every { application.packageManager } returns packageManager

        plugin = spyk(AndroidLifecyclePlugin(), recordPrivateCalls = true)

        plugin::class.java.getDeclaredField("application").apply {
            isAccessible = true
            set(plugin, application)
        }

        plugin::class.java.getDeclaredField("com/rudderstack/android/storage").apply {
            isAccessible = true
            set(plugin, storage)
        }
        plugin::class.java.getDeclaredField("packageInfo").apply {
            isAccessible = true
            set(plugin, packageInfo)
        }
        plugin::class.java.getDeclaredField("lifecycle").apply {
            isAccessible = true
            set(plugin, lifecycle)
        }

        firstLaunch.set(false)
        plugin::class.java.getDeclaredField("firstLaunch").apply {
            isAccessible = true
            set(plugin, firstLaunch)
        }

        trackedApplicationLifecycleEvents.set(false)
        plugin::class.java.getDeclaredField("trackedApplicationLifecycleEvents").apply {
            isAccessible = true
            set(plugin, trackedApplicationLifecycleEvents)
        }

        plugin::class.java.getDeclaredField("analytics").apply {
            set(plugin, analytics)
        }
    }

    @After
    @ExperimentalCoroutinesApi
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given trackApplicationLifecycleEvents is false, when lifecycle method is called, then do not track lifecycle events`() {
        // given
        pluginSetup(trackingEnabled = false)

        // when
        plugin.onCreate(lifecycleOwner)
        plugin.onStart(lifecycleOwner)

        // then
        verify(exactly = 0) { analytics.track(any<String>(), any<JsonObject>(), any<RudderOption>()) }
    }

    @Test
    fun `given trackApplicationLifecycleEvents is true and storage does not have build and version stored, when onCreate and onStart are called, then events called with correct properties`() =
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
            plugin.onCreate(lifecycleOwner)
            plugin.onStart(lifecycleOwner)

            // Advance the test dispatcher to run coroutines
            testDispatcher.scheduler.advanceUntilIdle()

            // then
            verify(exactly = 1) {
                analytics.track(
                    name = eq(APPLICATION_INSTALLED),
                    options = eq(RudderOption()),
                    properties = eq(installedProperties)
                )
            }
            verify(exactly = 1) {
                analytics.track(
                    name = eq(APPLICATION_OPENED),
                    options = eq(RudderOption()),
                    properties = eq(openedProperties)
                )
            }
            // verify that storage has now the correct version and build written
            assert(storage.readLong(StorageKeys.APP_BUILD, -1L) == 100L)
            assert(storage.readString(StorageKeys.APP_VERSION, "") == "1.0.0")
        }

    @Test
    fun `given trackApplicationLifecycleEvents is true and storage does have build and version stored, when onCreate and onStart are called, then events called with correct properties`() =
        runTest(testDispatcher) {
            // given
            pluginSetup()
            storage.write(StorageKeys.APP_BUILD, 100L)
            storage.write(StorageKeys.APP_VERSION, "1.0.0")

            val installedProperties = buildJsonObject {
                put(VERSION_KEY, "1.0.0")
                put(BUILD_KEY, 100L)
            }
            val openedProperties = buildJsonObject {
                put(FROM_BACKGROUND, false)
                put(VERSION_KEY, "1.0.0")
            }

            // when
            plugin.onCreate(lifecycleOwner)
            plugin.onStart(lifecycleOwner)

            // then
            verify(exactly = 0) {
                analytics.track(
                    name = eq(APPLICATION_INSTALLED),
                    options = eq(RudderOption()),
                    properties = eq(installedProperties)
                )
            }
            verify(exactly = 1) {
                analytics.track(
                    name = eq(APPLICATION_OPENED),
                    options = eq(RudderOption()),
                    properties = eq(openedProperties)
                )
            }
        }

    @Test
    fun `given trackApplicationLifecycleEvents is true and storage does have build and version stored, when onCreate, onStart, onStop, onStart are called in order, then events called with correct properties`() =
        runTest(testDispatcher) {
            // given
            pluginSetup()
            storage.write(StorageKeys.APP_BUILD, 100L)
            storage.write(StorageKeys.APP_VERSION, "1.0.0")

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
            plugin.onCreate(lifecycleOwner)
            plugin.onStart(lifecycleOwner)
            plugin.onStop(lifecycleOwner)
            plugin.onStart(lifecycleOwner)

            // then
            verify(exactly = 0) {
                analytics.track(
                    name = eq(APPLICATION_INSTALLED),
                    options = eq(RudderOption()),
                    properties = eq(installedProperties)
                )
            }
            verify(ordering = Ordering.ORDERED) {
                analytics.track(
                    name = eq(APPLICATION_OPENED),
                    options = eq(RudderOption()),
                    properties = eq(openedFirstTimeProperties)
                )
                analytics.track(
                    name = eq(APPLICATION_BACKGROUNDED),
                    options = eq(RudderOption()),
                    properties = eq(Properties(emptyMap()))
                )
                analytics.track(
                    name = eq(APPLICATION_OPENED),
                    options = eq(RudderOption()),
                    properties = eq(openedSecondTimeProperties)
                )
            }
        }

    @Test
    fun `given trackApplicationLifecycleEvents is true and stored and current versions are different, when onCreate and onStart called, then events called with correct properties`() =
        runTest(testDispatcher) {
            // given
            pluginSetup()
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
            storage.write(StorageKeys.APP_BUILD, oldBuildNumber)
            storage.write(StorageKeys.APP_VERSION, oldVersion)

            // when
            plugin.onCreate(lifecycleOwner)
            plugin.onStart(lifecycleOwner)

            // then
            verify(exactly = 0) {
                analytics.track(
                    name = eq(APPLICATION_INSTALLED),
                    options = eq(RudderOption()),
                    properties = eq(installedProperties)
                )
            }
            verify(exactly = 1) {
                analytics.track(
                    name = eq(APPLICATION_UPDATED),
                    options = eq(RudderOption()),
                    properties = eq(updatedProperties)
                )
            }
            verify(exactly = 1) {
                analytics.track(
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
                lifecycle.removeObserver(plugin)
            }
        }

    private fun pluginSetup(trackingEnabled: Boolean = true) {

        shouldTrackApplicationLifecycleEvents = trackingEnabled

        plugin::class.java.getDeclaredField("shouldTrackApplicationLifecycleEvents").apply {
            isAccessible = true
            set(plugin, shouldTrackApplicationLifecycleEvents)
        }
    }
}
