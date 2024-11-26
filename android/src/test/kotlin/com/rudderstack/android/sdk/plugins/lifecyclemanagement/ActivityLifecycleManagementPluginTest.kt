package com.rudderstack.android.sdk.plugins.lifecyclemanagement

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.rudderstack.android.sdk.Configuration
import com.rudderstack.android.sdk.plugins.lifecyclemanagment.ActivityLifecycleManagementPlugin
import com.rudderstack.android.sdk.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.android.sdk.utils.mockAnalytics
import com.rudderstack.kotlin.sdk.Analytics
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityLifecycleManagementPluginTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var plugin: ActivityLifecycleManagementPlugin
    private lateinit var mockAnalytics: Analytics

    @MockK
    private lateinit var mockApplication: Application

    @MockK
    private lateinit var mockObserver: ActivityLifecycleObserver

    @MockK
    private lateinit var mockActivity: Activity

    @MockK
    private lateinit var mockBundle: Bundle

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)

        plugin = ActivityLifecycleManagementPlugin()
        mockAnalytics = mockAnalytics(testScope, testDispatcher)

        every { mockAnalytics.configuration } returns mockk<Configuration> {
            every { application } returns mockApplication
        }

        plugin.setup(mockAnalytics)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `when setup should, then it should register activity lifecycle callbacks`() = runTest {
        advanceUntilIdle()
        verify { mockApplication.registerActivityLifecycleCallbacks(plugin) }
    }

    @Test
    fun `when teardown called, it should unregister activity lifecycle callbacks`() = runTest {
        plugin.activityObservers.add(mockObserver)

        plugin.teardown()

        advanceUntilIdle()
        verify { mockApplication.unregisterActivityLifecycleCallbacks(plugin) }
    }

    @Test
    fun `when addObserver called, then it should add an observer to the list`() {
        plugin.addObserver(mockObserver)

        assert(plugin.activityObservers.contains(mockObserver))
    }

    @Test
    fun `when removeObserver called, then it should remove an observer from the list`() {
        plugin.addObserver(mockObserver)
        plugin.removeObserver(mockObserver)

        assert(!plugin.activityObservers.contains(mockObserver))
    }

    @Test
    fun `given an observer, when onActivityCreated called, then it should notify all observers`() {
        plugin.addObserver(mockObserver)

        plugin.onActivityCreated(mockActivity, mockBundle)

        verify { mockObserver.onActivityCreated(mockActivity, mockBundle) }
    }

    @Test
    fun `given an observer, when onActivityStarted called, then it should notify all observers`() {
        plugin.addObserver(mockObserver)
        plugin.onActivityStarted(mockActivity)

        verify { mockObserver.onActivityStarted(mockActivity) }
    }

    @Test
    fun `given an observer, when onActivityResumed called, then it should notify all observers`() {
        plugin.addObserver(mockObserver)
        plugin.onActivityResumed(mockActivity)

        verify { mockObserver.onActivityResumed(mockActivity) }
    }

    @Test
    fun `given an observer, when onActivityPaused called, then it should notify all observers`() {
        plugin.addObserver(mockObserver)
        plugin.onActivityPaused(mockActivity)

        verify { mockObserver.onActivityPaused(mockActivity) }
    }

    @Test
    fun `given an observer, when onActivityStopped called, then it should notify all observers`() {
        plugin.addObserver(mockObserver)
        plugin.onActivityStopped(mockActivity)

        verify { mockObserver.onActivityStopped(mockActivity) }
    }

    @Test
    fun `given an observer, when onActivitySaveInstanceState called, then it should notify all observers`() {
        plugin.addObserver(mockObserver)
        plugin.onActivitySaveInstanceState(mockActivity, mockBundle)

        verify { mockObserver.onActivitySaveInstanceState(mockActivity, mockBundle) }
    }

    @Test
    fun `given an observer, when onActivityDestroyed called, then it should notify all observers`() {
        plugin.addObserver(mockObserver)
        plugin.onActivityDestroyed(mockActivity)

        verify { mockObserver.onActivityDestroyed(mockActivity) }
    }
}
