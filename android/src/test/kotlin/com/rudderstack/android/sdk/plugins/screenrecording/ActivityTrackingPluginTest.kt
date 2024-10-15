package com.rudderstack.android.sdk.plugins.screenrecording

import android.app.Activity
import android.app.Application
import com.rudderstack.android.sdk.Configuration
import com.rudderstack.android.sdk.utils.automaticProperty
import com.rudderstack.android.sdk.utils.mockAnalytics
import com.rudderstack.kotlin.sdk.Analytics
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityTrackingPluginTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var plugin: ActivityTrackingPlugin
    private lateinit var mockAnalytics: Analytics

    @MockK
    private lateinit var mockApplication: Application

    @MockK
    private lateinit var mockConfig: Configuration

    @MockK
    private lateinit var mockActivity: Activity

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)

        plugin = ActivityTrackingPlugin()
        mockAnalytics = mockAnalytics(testScope, testDispatcher)

        every { mockAnalytics.configuration } returns mockConfig
        every { mockConfig.application } returns mockApplication
        every { mockConfig.trackActivities } returns true
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `given trackActivities is true, when setup called, then registerActivityLifecycleCallbacks is called`() {
        plugin.setup(mockAnalytics)

        verify(exactly = 1) { mockApplication.registerActivityLifecycleCallbacks(plugin) }
    }

    @Test
    fun `given trackActivities is true, when teardown called, then unregisterActivityLifecycleCallbacks is called`() {
        plugin.setup(mockAnalytics)

        plugin.teardown()

        verify(exactly = 1) { mockApplication.unregisterActivityLifecycleCallbacks(plugin) }
    }

    @Test
    fun `when onActivityStarted called, then screen event called with proper name of Activity`() {
        val testActivityName = "TestActivity"
        plugin.setup(mockAnalytics)

        every { mockActivity.localClassName } returns testActivityName

        plugin.onActivityStarted(mockActivity)

        verify(exactly = 1) { mockAnalytics.screen(screenName = testActivityName, properties = automaticProperty()) }
    }

    @Test
    fun `given trackActivities is false, when setup called, then registerActivityLifecycleCallbacks is not called`() {
        every { mockConfig.trackActivities } returns false

        plugin.setup(mockAnalytics)

        verify(exactly = 0) { mockApplication.registerActivityLifecycleCallbacks(any()) }
    }
}
