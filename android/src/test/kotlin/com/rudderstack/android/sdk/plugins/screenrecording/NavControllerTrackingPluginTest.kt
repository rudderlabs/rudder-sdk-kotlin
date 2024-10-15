package com.rudderstack.android.sdk.plugins.screenrecording

import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.rudderstack.android.sdk.state.NavContext
import com.rudderstack.android.sdk.state.NavContextState
import com.rudderstack.android.sdk.utils.automaticProperty
import com.rudderstack.android.sdk.utils.mockAnalytics
import com.rudderstack.android.sdk.utils.mockNavContext
import com.rudderstack.kotlin.sdk.internals.statemanagement.Store
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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

    @MockK
    private lateinit var mockNavContextStore: Store<NavContextState, NavContextState.NavContextAction>

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)

        plugin = NavControllerTrackingPlugin(mockNavContextStore)
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
        val initialState = NavContextState.initialState()
        val mockDispatch: (NavContextState.NavContextAction) -> Unit = mockk(relaxed = true)

        every { mockNavContextStore.subscribe(any()) } answers {
            firstArg<(NavContextState, (NavContextState.NavContextAction) -> Unit) -> Unit>().invoke(
                initialState,
                mockDispatch
            )
        }

        plugin.setup(mockAnalytics)

        verify { mockNavContextStore.subscribe(any()) }
    }

    @Test
    fun `when teardown called, then it should dispatch RemoveAllNavContextsAction`() = runTest {
        plugin.teardown()

        verify { mockNavContextStore.dispatch(NavContextState.RemoveAllNavContextsAction) }
    }

    @Test
    fun `when navContexts are added, then addOnDestinationChangedListener called and currentNavContexts updated`() =
        runTest {
            val navContext1: NavContext = mockNavContext()
            val navContext2: NavContext = mockNavContext()

            val initialState = NavContextState.initialState()
            val updatedState = NavContextState(
                navContexts = setOf(navContext1, navContext2)
            )

            every { mockNavContextStore.subscribe(any()) } answers {
                val subscription = firstArg<(NavContextState, (NavContextState.NavContextAction) -> Unit) -> Unit>()
                // Simulate first subscribe with initial state (empty set), then updated state with navControllers
                subscription.invoke(initialState, mockk())
                subscription.invoke(updatedState, mockk())
            }

            plugin.setup(mockAnalytics)

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

            val initialState = NavContextState(
                navContexts = setOf(navContext1, navContext2)
            )
            val updatedState = NavContextState(
                navContexts = setOf(navContext1) // navContext2 removed
            )

            every { (navContext2.callingActivity as LifecycleOwner).lifecycle } returns mockk(relaxed = true)
            every { (navContext2.callingActivity as LifecycleOwner).lifecycle.removeObserver(any()) } returns Unit
            every { mockNavContextStore.subscribe(any()) } answers {
                val subscription = firstArg<(NavContextState, (NavContextState.NavContextAction) -> Unit) -> Unit>()
                // Simulate state update where navContext2 is removed
                subscription.invoke(initialState, mockk())
                subscription.invoke(updatedState, mockk())
            }

            plugin.setup(mockAnalytics)

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
