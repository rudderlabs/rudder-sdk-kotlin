package com.rudderstack.android.sdk.plugins.screenrecording

import androidx.activity.ComponentActivity
import androidx.navigation.NavDestination
import com.rudderstack.kotlin.core.Analytics
import com.rudderstack.android.sdk.state.NavContext
import com.rudderstack.android.sdk.utils.mockAnalytics
import com.rudderstack.kotlin.core.internals.statemanagement.FlowState
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
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
class NavControllerActivityObserverTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @MockK
    private lateinit var mockPlugin: NavControllerTrackingPlugin

    @MockK
    private lateinit var mockNavContext: NavContext

    @MockK
    private lateinit var mockActivity: ComponentActivity

    @MockK
    private lateinit var mockNavContextState: FlowState<Set<NavContext>>

    private lateinit var mockAnalytics: Analytics

    private lateinit var observer: NavControllerActivityObserver

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)

        mockAnalytics = mockAnalytics(testScope, testDispatcher)

        every { mockNavContext.callingActivity } returns mockActivity
        every { mockActivity.lifecycle } returns mockk(relaxed = true)
        every { mockActivity.lifecycle.addObserver(any()) } just Runs
        every { mockPlugin.navContextState } returns mockNavContextState
        every { mockPlugin.analytics } returns mockAnalytics
        every { mockNavContextState.dispatch(any()) } just Runs

        observer = NavControllerActivityObserver(mockPlugin, mockNavContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `given observer is just created, when onStart called, then OnDestinationChanged is not called`() {
        val mockDestination = mockk<NavDestination>(relaxed = true)
        every { mockNavContext.navController.currentDestination } returns mockDestination

        observer.onStart(mockActivity)

        verify(exactly = 0) { mockPlugin.onDestinationChanged(mockNavContext.navController, mockDestination, any()) }
    }

    @Test
    fun `given an observer, when addObserver called, then observer is added to activity lifecycle`() = runTest {
        observer.addObserver()

        advanceUntilIdle()

        verify(exactly = 1) { observer.activityLifecycle()?.addObserver(observer) }
    }

    @Test
    fun `given an observer, when removeObserver called, then observer is removed from activity lifecycle`() = runTest {
        observer.removeObserver()

        advanceUntilIdle()

        verify(exactly = 1) { observer.activityLifecycle()?.removeObserver(observer) }
    }

    @Test
    fun `given observer already created, when onStart called again, then OnDestinationChanged is not called again`() {
        val mockDestination = mockk<NavDestination>(relaxed = true)
        every { mockNavContext.navController.currentDestination } returns mockDestination

        observer.onStart(mockActivity)
        observer.onStart(mockActivity)

        verify(exactly = 1) { mockPlugin.onDestinationChanged(mockNavContext.navController, mockDestination, any()) }
    }

    @Test
    fun `given observer, when onDestroyed called, then RemoveNavContextAction dispatched`() {
        observer.onDestroy(mockActivity)

        verify {
            mockNavContextState.dispatch(
                match { it is NavContext.RemoveNavContextAction && it.navContext == mockNavContext }
            )
        }
    }
}
