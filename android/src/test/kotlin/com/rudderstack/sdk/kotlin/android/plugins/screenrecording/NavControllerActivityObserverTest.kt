package com.rudderstack.sdk.kotlin.android.plugins.screenrecording

import androidx.activity.ComponentActivity
import androidx.navigation.NavDestination
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.android.utils.mockAnalytics
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
    private lateinit var mockNavContextState: State<Set<NavContext>>

    private lateinit var mockAnalytics: Analytics

    private lateinit var observer: NavControllerActivityObserver

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)

        mockAnalytics = mockAnalytics(testScope, testDispatcher)

        every { mockNavContext.callingActivity } returns mockActivity
        every { mockActivity.lifecycle } returns mockk(relaxed = true)
        every { mockActivity.lifecycle.addObserver(any()) } just Runs
        every { mockPlugin.analytics } returns mockAnalytics
        every { mockNavContextState.dispatch(any()) } just Runs

        observer = NavControllerActivityObserver(mockPlugin, mockNavContext)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `given observer is just created, when onStart called, then no automatic screen event is made`() {
        val mockDestination = mockk<NavDestination>(relaxed = true)
        every { mockNavContext.navController.currentDestination } returns mockDestination

        observer.onStart(mockActivity)

        verify(exactly = 0) { mockPlugin.makeAutomaticScreenEvent(mockDestination) }
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
    fun `given observer already created, when onStart called again, then automatic screen is made once`() {
        val mockDestination = mockk<NavDestination>(relaxed = true)
        every { mockNavContext.navController.currentDestination } returns mockDestination

        observer.onStart(mockActivity)
        observer.onStart(mockActivity)

        verify(exactly = 1) { mockPlugin.makeAutomaticScreenEvent(mockDestination) }
    }

    @Test
    fun `given observer, when onDestroyed called, then context and observer removed from plugin`() {
        observer.onDestroy(mockActivity)

        verify(exactly = 1) { mockPlugin.removeContextAndObserver(mockNavContext) }
    }
}
