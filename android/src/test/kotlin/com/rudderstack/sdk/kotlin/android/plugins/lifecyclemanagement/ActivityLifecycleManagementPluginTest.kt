package com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagement

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ActivityLifecycleManagementPlugin
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ActivityLifecycleObserver
import com.rudderstack.sdk.kotlin.android.utils.mockAnalytics
import com.rudderstack.sdk.kotlin.core.Analytics
import io.mockk.MockKAnnotations
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

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

    @BeforeEach
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

    @AfterEach
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
    fun `when teardown called and shutdown cancels the scope before the cleanup runs, it should still unregister activity lifecycle callbacks`() = runTest {
        plugin.activityObservers.add(mockObserver)

        plugin.teardown()
        mockAnalytics.analyticsScope.cancel()

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

    @Nested
    inner class AddObserverWithCurrentState {

        @Test
        fun `given a resumed activity, when an observer is added with current state, then it gets started and resumed once`() =
            runTest {
                plugin.onActivityStarted(mockActivity)
                plugin.onActivityResumed(mockActivity)

                plugin.addObserverWithCurrentState(mockObserver)
                advanceUntilIdle()

                assert(plugin.activityObservers.contains(mockObserver))
                verify(exactly = 1) { mockObserver.onActivityStarted(mockActivity) }
                verify(exactly = 1) { mockObserver.onActivityResumed(mockActivity) }
            }

        @Test
        fun `given a paused activity, when an observer is added with current state, then it gets started only`() =
            runTest {
                plugin.onActivityStarted(mockActivity)
                plugin.onActivityResumed(mockActivity)
                plugin.onActivityPaused(mockActivity)

                plugin.addObserverWithCurrentState(mockObserver)
                advanceUntilIdle()

                verify(exactly = 1) { mockObserver.onActivityStarted(mockActivity) }
                verify(exactly = 0) { mockObserver.onActivityResumed(any()) }
            }

        @Test
        fun `given no activity on screen, when an observer is added with current state, then it gets no event`() =
            runTest {
                plugin.onActivityStarted(mockActivity)
                plugin.onActivityStopped(mockActivity)

                plugin.addObserverWithCurrentState(mockObserver)
                advanceUntilIdle()

                assert(plugin.activityObservers.contains(mockObserver))
                verify(exactly = 0) { mockObserver.onActivityStarted(any()) }
                verify(exactly = 0) { mockObserver.onActivityResumed(any()) }
            }

        @Test
        fun `given an observer added with current state, when the next activity starts, then it gets that event normally`() =
            runTest {
                val nextActivity = mockk<Activity>(relaxed = true)
                plugin.addObserverWithCurrentState(mockObserver)
                advanceUntilIdle()

                plugin.onActivityStarted(nextActivity)

                verify(exactly = 1) { mockObserver.onActivityStarted(nextActivity) }
            }
    }

    @Nested
    inner class RemoveObserverWithCurrentState {

        @Test
        fun `given a resumed activity, when an observer is removed with current state, then it gets paused and stopped once and no later event`() =
            runTest {
                plugin.addObserver(mockObserver)
                plugin.onActivityStarted(mockActivity)
                plugin.onActivityResumed(mockActivity)

                plugin.removeObserverWithCurrentState(mockObserver)
                advanceUntilIdle()
                plugin.onActivityPaused(mockActivity)
                plugin.onActivityStopped(mockActivity)

                assert(!plugin.activityObservers.contains(mockObserver))
                verify(exactly = 1) { mockObserver.onActivityPaused(mockActivity) }
                verify(exactly = 1) { mockObserver.onActivityStopped(mockActivity) }
            }

        @Test
        fun `given no activity on screen, when an observer is removed with current state, then it gets no event`() =
            runTest {
                plugin.addObserver(mockObserver)
                plugin.onActivityStarted(mockActivity)
                plugin.onActivityStopped(mockActivity)
                clearMocks(mockObserver)

                plugin.removeObserverWithCurrentState(mockObserver)
                advanceUntilIdle()

                assert(!plugin.activityObservers.contains(mockObserver))
                verify(exactly = 0) { mockObserver.onActivityPaused(any()) }
                verify(exactly = 0) { mockObserver.onActivityStopped(any()) }
            }

        @Test
        fun `given an observer that is not added, when it is removed with current state, then it gets no event`() =
            runTest {
                plugin.onActivityStarted(mockActivity)
                plugin.onActivityResumed(mockActivity)

                plugin.removeObserverWithCurrentState(mockObserver)
                advanceUntilIdle()

                verify(exactly = 0) { mockObserver.onActivityPaused(any()) }
                verify(exactly = 0) { mockObserver.onActivityStopped(any()) }
            }

        @Test
        fun `given a resumed activity, when shutdown cancels the scope before the removal runs, then the observer is still removed, paused, and stopped`() =
            runTest {
                plugin.addObserver(mockObserver)
                plugin.onActivityStarted(mockActivity)
                plugin.onActivityResumed(mockActivity)

                plugin.removeObserverWithCurrentState(mockObserver)
                mockAnalytics.analyticsScope.cancel()
                advanceUntilIdle()

                assert(!plugin.activityObservers.contains(mockObserver))
                verify(exactly = 1) { mockObserver.onActivityPaused(mockActivity) }
                verify(exactly = 1) { mockObserver.onActivityStopped(mockActivity) }
            }
    }
}
