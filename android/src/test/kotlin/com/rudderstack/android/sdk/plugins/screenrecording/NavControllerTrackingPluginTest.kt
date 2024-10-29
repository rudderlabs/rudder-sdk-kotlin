package com.rudderstack.android.sdk.plugins.screenrecording

import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.rudderstack.android.sdk.state.NavContext
import com.rudderstack.android.sdk.utils.automaticProperty
import com.rudderstack.android.sdk.utils.mockAnalytics
import com.rudderstack.android.sdk.utils.mockNavContext
import com.rudderstack.kotlin.sdk.internals.statemanagement.FlowState
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import com.rudderstack.android.sdk.Configuration as AndroidConfiguration
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavControllerTrackingPluginTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var plugin: NavControllerTrackingPlugin

    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)

    private lateinit var mockNavContextState: FlowState<Set<NavContext>>

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)

        mockNavContextState = spyk(FlowState(emptySet()))
        plugin = NavControllerTrackingPlugin(mockNavContextState)
        plugin.analytics = mockAnalytics

        val configuration: AndroidConfiguration = mockk()
        every { mockAnalytics.configuration } returns configuration
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `when setup called, then it should subscribe to navContextStore`() = runTest {
        plugin.setup(mockAnalytics)
        advanceUntilIdle()

        coVerify { mockNavContextState.collect(any()) }
    }

    @Test
    fun `when teardown called, then it should dispatch RemoveAllNavContextsAction`() = runTest {
        plugin.teardown()

        verify { mockNavContextState.dispatch(NavContext.RemoveAllNavContextsAction) }
    }

    @Test
    fun `when navContexts are added, then addOnDestinationChangedListener called and currentNavContexts updated`() =
        runTest {
            val navContext1: NavContext = mockNavContext()
            val navContext2: NavContext = mockNavContext()

            plugin.setup(mockAnalytics)
            mockNavContextState.dispatch(NavContext.AddNavContextAction(navContext1))
            mockNavContextState.dispatch(NavContext.AddNavContextAction(navContext2))
            advanceUntilIdle()
            assertEquals(2, plugin.currentNavContexts.size)
            assertTrue(plugin.currentNavContexts.contains(navContext1))
            assertTrue(plugin.currentNavContexts.contains(navContext2))
            verify(exactly = 1) { navContext1.navController.addOnDestinationChangedListener(plugin) }
            verify(exactly = 1) { navContext2.navController.addOnDestinationChangedListener(plugin) }
        }

    @Test
    fun `when navControllers are removed, then removeOnDestinationChangedListener called and currentNavContexts updated`() =
        runTest {
            val navContext1 = mockNavContext()
            val navContext2 = mockNavContext()

            every { (navContext2.callingActivity as LifecycleOwner).lifecycle } returns mockk(relaxed = true)
            every { (navContext2.callingActivity as LifecycleOwner).lifecycle.removeObserver(any()) } returns Unit

            plugin.setup(mockAnalytics)
            mockNavContextState.dispatch(NavContext.AddNavContextAction(navContext1))
            mockNavContextState.dispatch(NavContext.AddNavContextAction(navContext2))
            advanceUntilIdle()
            mockNavContextState.dispatch(NavContext.RemoveNavContextAction(navContext2))
            advanceUntilIdle()

            // Verify that removeOnDestinationChangedListener was called for the removed navContext2 navController
            assertEquals(1, plugin.currentNavContexts.size)
            assertTrue(plugin.currentNavContexts.contains(navContext1))
            verify(exactly = 1) { navContext2.navController.removeOnDestinationChangedListener(plugin) }
            verify(exactly = 0) { navContext1.navController.removeOnDestinationChangedListener(plugin) }
        }

    @Test
    fun `given fragment destinations, when onDestinationChanged called, then it tracks fragment screen`() {
        val testDestinationLabel = "TestFragment"
        val navController: NavController = mockk(relaxed = true)
        val destination: NavDestination = mockk(relaxed = true)
        val bundle: Bundle = mockk(relaxed = true)

        every { destination.navigatorName } returns FRAGMENT_NAVIGATOR_NAME
        every { destination.label } returns testDestinationLabel

        plugin.onDestinationChanged(navController, destination, bundle)

        verify { mockAnalytics.screen(testDestinationLabel, properties = automaticProperty()) }
    }

    @Test
    fun `given composable destinations, when onDestinationChanged called, it tracks composable screen`() {
        val testRoute = "TestComposableRoute"
        val navController: NavController = mockk(relaxed = true)
        val destination: NavDestination = mockk(relaxed = true)
        val bundle: Bundle = mockk(relaxed = true)

        every { destination.navigatorName } returns COMPOSE_NAVIGATOR_NAME
        every { destination.route } returns testRoute

        plugin.onDestinationChanged(navController, destination, bundle)

        verify { mockAnalytics.screen(testRoute, properties = automaticProperty()) }
    }

    @Test
    fun `given composable destinations with args, when onDestinationChanged called, then it tracks the correct screen name without args`() {
        val arg1 = "arg1"
        val arg2 = "arg2"
        val testRoute = "TestComposableRoute/$arg1/$arg2"
        val testRouteWithoutArgs = "TestComposableRoute"

        val navController: NavController = mockk(relaxed = true)
        val destination: NavDestination = mockk(relaxed = true)
        val bundle: Bundle = mockk(relaxed = true)

        every { destination.navigatorName } returns COMPOSE_NAVIGATOR_NAME
        every { destination.route } returns testRoute
        every { destination.arguments } returns mapOf(arg1 to mockk(), arg2 to mockk())

        plugin.onDestinationChanged(navController, destination, bundle)

        verify { mockAnalytics.screen(testRouteWithoutArgs, properties = automaticProperty()) }
    }
}
