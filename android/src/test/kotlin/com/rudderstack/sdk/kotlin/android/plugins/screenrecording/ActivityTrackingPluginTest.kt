package com.rudderstack.sdk.kotlin.android.plugins.screenrecording

import android.app.Activity
import android.app.Application
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.addLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.automaticProperty
import com.rudderstack.sdk.kotlin.android.utils.mockAnalytics
import com.rudderstack.sdk.kotlin.android.utils.removeLifecycleObserver
import com.rudderstack.sdk.kotlin.core.Analytics
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.rudderstack.sdk.kotlin.android.Analytics as AndroidAnalytics

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
        every { (mockAnalytics as AndroidAnalytics).addLifecycleObserver(plugin) } just Runs
        every { mockConfig.trackActivities } returns true
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `given trackActivities is true, when setup called, then addLifecycleObserver is called`() {
        plugin.setup(mockAnalytics)

        verify(exactly = 1) { (mockAnalytics as AndroidAnalytics).addLifecycleObserver(plugin) }
    }

    @Test
    fun `when onActivityStarted called, then screen event called with proper name of Activity`() {
        val testActivityName = "TestActivity"
        plugin.setup(mockAnalytics)

        mockkStatic(::getActivityClassName)
        every { getActivityClassName(mockActivity)} returns testActivityName

        plugin.onActivityStarted(mockActivity)

        verify(exactly = 1) { mockAnalytics.screen(screenName = testActivityName, properties = automaticProperty()) }
    }

    @Test
    fun `given trackActivities is false, when setup called, then addLifecycleObserver is not called`() {
        every { mockConfig.trackActivities } returns false

        plugin.setup(mockAnalytics)

        verify(exactly = 0) { (mockAnalytics as AndroidAnalytics).addLifecycleObserver(any<ActivityLifecycleObserver>()) }
    }

    @Test
    fun `when teardown called, then removeLifecycleObserver is called`() = runTest {
        plugin.setup(mockAnalytics)

        plugin.teardown()

        verify { (mockAnalytics as AndroidAnalytics).removeLifecycleObserver(plugin) }
    }
}
