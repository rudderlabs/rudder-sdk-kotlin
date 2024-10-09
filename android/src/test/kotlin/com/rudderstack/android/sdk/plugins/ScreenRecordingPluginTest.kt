package com.rudderstack.android.sdk.plugins

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.rudderstack.android.sdk.state.NavControllerState
import com.rudderstack.android.sdk.utils.mockAnalytics
import com.rudderstack.kotlin.sdk.internals.statemanagement.Store
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
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import com.rudderstack.android.sdk.Configuration as AndroidConfiguration
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.lang.ref.WeakReference

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenRecordingPluginTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var plugin: ScreenRecordingPlugin

    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)

    @MockK
    private lateinit var mockNavControllerStore: Store<NavControllerState, NavControllerState.NavControllerAction>

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)

        plugin = ScreenRecordingPlugin(mockNavControllerStore)
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
    fun `when setup called, then it should subscribe to navControllerStore and update navControllers`() = runTest {
        val initialState = NavControllerState.initialState()
        val mockDispatch: (NavControllerState.NavControllerAction) -> Unit = mockk(relaxed = true)

        every { mockNavControllerStore.subscribe(any()) } answers {
            firstArg<(NavControllerState, (NavControllerState.NavControllerAction) -> Unit) -> Unit>().invoke(
                initialState,
                mockDispatch
            )
        }

        plugin.setup(mockAnalytics)

        verify { mockNavControllerStore.subscribe(any()) }
    }

    @Test
    fun `when teardown called, then it should clear all navControllers`() = runTest {
        val navController1: NavController = mockk(relaxed = true)
        val navController2: NavController = mockk(relaxed = true)

        val initialState = NavControllerState(
            navControllers = setOf(
                WeakReference(navController1),
                WeakReference(navController2)
            )
        )

        every { mockNavControllerStore.subscribe(any()) } answers {
            val subscription = firstArg<(NavControllerState, (NavControllerState.NavControllerAction) -> Unit) -> Unit>()
            subscription.invoke(initialState, mockk())
        }

        plugin.setup(mockAnalytics)
        plugin.teardown()

        verify(exactly = 1) { navController1.removeOnDestinationChangedListener(plugin) }
        verify(exactly = 1) { navController2.removeOnDestinationChangedListener(plugin) }
    }

    @Test
    fun `when navControllers are added, then addOnDestinationChangedListener for navControllers gets called`() = runTest {
        val navController1: NavController = mockk(relaxed = true)
        val navController2: NavController = mockk(relaxed = true)

        val initialState = NavControllerState.initialState()
        val updatedState = NavControllerState(
            navControllers = setOf(
                WeakReference(navController1),
                WeakReference(navController2)
            )
        )

        every { mockNavControllerStore.subscribe(any()) } answers {
            val subscription = firstArg<(NavControllerState, (NavControllerState.NavControllerAction) -> Unit) -> Unit>()
            // Simulate first subscribe with initial state (empty set), then updated state with navControllers
            subscription.invoke(initialState, mockk())
            subscription.invoke(updatedState, mockk())
        }

        plugin.setup(mockAnalytics)

        // Verify that addOnDestinationChangedListener was called for each new navController
        verify(exactly = 1) { navController1.addOnDestinationChangedListener(plugin) }
        verify(exactly = 1) { navController2.addOnDestinationChangedListener(plugin) }
    }

    @Test
    fun `when navControllers are removed, then removeOnDestinationChangedListener for navControllers gets called`() = runTest {
            val navController1: NavController = mockk(relaxed = true)
            val navController2: NavController = mockk(relaxed = true)
            val weakNavController1 = WeakReference(navController1)
            val weakNavController2 = WeakReference(navController2)

            val initialState = NavControllerState(
                navControllers = setOf(
                    weakNavController1,
                    weakNavController2
                )
            )
            val updatedState = NavControllerState(
                navControllers = setOf(weakNavController1) // navController2 removed
            )

            every { mockNavControllerStore.subscribe(any()) } answers {
                val subscription = firstArg<(NavControllerState, (NavControllerState.NavControllerAction) -> Unit) -> Unit>()
                // Simulate state update where navController2 is removed
                subscription.invoke(initialState, mockk())
                subscription.invoke(updatedState, mockk())
            }

            plugin.setup(mockAnalytics)

            // Verify that removeOnDestinationChangedListener was called for the removed navController2
            verify(exactly = 1) { navController2.removeOnDestinationChangedListener(plugin) }
            verify(exactly = 0) { navController1.removeOnDestinationChangedListener(plugin) }
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

        verify { mockAnalytics.screen(testDestinationLabel) }
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

        verify { mockAnalytics.screen(testRoute) }
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

        verify { mockAnalytics.screen(testRouteWithoutArgs) }
    }
}
