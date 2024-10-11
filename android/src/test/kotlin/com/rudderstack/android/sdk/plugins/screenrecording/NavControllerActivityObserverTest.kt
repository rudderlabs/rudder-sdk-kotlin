package com.rudderstack.android.sdk.plugins.screenrecording

import androidx.activity.ComponentActivity
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.rudderstack.android.sdk.state.NavContext
import com.rudderstack.android.sdk.state.NavContextState
import com.rudderstack.kotlin.sdk.internals.statemanagement.Store
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

class NavControllerActivityObserverTest {

    @MockK
    private lateinit var mockPlugin: ScreenRecordingPlugin

    @MockK
    private lateinit var mockNavContext: NavContext

    @MockK
    private lateinit var mockActivity: ComponentActivity

    @MockK
    private lateinit var mockNavContextStore: Store<NavContextState, NavContextState.NavContextAction>

    private lateinit var observer: NavControllerActivityObserver

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        every { mockNavContext.callingActivity } returns mockActivity
        every { mockActivity.lifecycle } returns mockk(relaxed = true)
        every { mockActivity.lifecycle.addObserver(any()) } just Runs
        every { mockPlugin.navContextStore } returns mockNavContextStore
        every { mockNavContextStore.dispatch(any()) } just Runs

        observer = NavControllerActivityObserver(mockPlugin, mockNavContext)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given navContext, when isObserverForContext called, then it returns true for that navContext`() {
        assertTrue(observer.isObserverForContext(mockNavContext))
    }

    @Test
    fun `given different navContext, when isObserverForContext called, then it returns false for that navContext`() {
        val anotherNavContext: NavContext = mockk()

        assertFalse(observer.isObserverForContext(anotherNavContext))
    }

    @Test
    fun `given observer is just created, when onStart called, then OnDestinationChanged is not called`() {
        val mockDestination = mockk<NavDestination>(relaxed = true)
        every { mockNavContext.navController.currentDestination } returns mockDestination

        observer.onStart(mockActivity)

        verify(exactly = 0) { mockPlugin.onDestinationChanged(mockNavContext.navController, mockDestination, any()) }
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
        val mockNavController = mockk<NavController>(relaxed = true)
        every { mockNavContext.navController } returns mockNavController

        observer.onDestroy(mockActivity)

        verify {
            mockNavContextStore.dispatch(
                match { it is NavContextState.RemoveNavContextAction && it.navController == mockNavController }
            )
        }
    }
}
