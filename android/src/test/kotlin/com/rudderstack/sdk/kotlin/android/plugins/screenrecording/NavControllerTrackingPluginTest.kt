package com.rudderstack.sdk.kotlin.android.plugins.screenrecording

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.rudderstack.sdk.kotlin.android.state.NavContext
import com.rudderstack.sdk.kotlin.android.utils.automaticProperty
import com.rudderstack.sdk.kotlin.android.utils.mockAnalytics
import com.rudderstack.sdk.kotlin.android.utils.mockNavContext
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
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
import com.rudderstack.sdk.kotlin.android.Configuration as AndroidConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavControllerTrackingPluginTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var plugin: NavControllerTrackingPlugin

    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)

    private lateinit var mockNavContextState: State<Set<NavContext>>

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockNavContextState = spyk(State(emptySet()))
        plugin = NavControllerTrackingPlugin(mockNavContextState)
        plugin.analytics = mockAnalytics

        val configuration: AndroidConfiguration = mockk()
        every { mockAnalytics.configuration } returns configuration
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `when setup called, then it should subscribe to navContextState`() = runTest {
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
    fun `when navContexts are added, then addOnDestinationChangedListener and addObserver called for navContexts`() =
        runTest {
            val navContext1 = mockNavContext()
            val navContext2 = mockNavContext()
            mockkStatic(::provideNavControllerActivityObserver)
            val observer1 = mockActivityObserverProvider(navContext1)
            val observer2 = mockActivityObserverProvider(navContext2)

            plugin.setup(mockAnalytics)
            mockNavContextState.dispatch(NavContext.AddNavContextAction(navContext1))
            mockNavContextState.dispatch(NavContext.AddNavContextAction(navContext2))
            advanceUntilIdle()

            verify(exactly = 1) { navContext1.navController.addOnDestinationChangedListener(plugin) }
            verify(exactly = 1) { navContext2.navController.addOnDestinationChangedListener(plugin) }

            verify(exactly = 1) { observer1.addObserver() }
            verify(exactly = 1) { observer2.addObserver() }
        }

    @Test
    fun `when navControllers are added and one of them is removed, then removeOnDestinationChangedListener and removeObserver called for that navContext`() =
        runTest {
            val navContext1 = mockNavContext()
            val navContext2 = mockNavContext()
            mockkStatic(::provideNavControllerActivityObserver)
            val observer1 = mockActivityObserverProvider(navContext1)
            val observer2 = mockActivityObserverProvider(navContext2)

            plugin.setup(mockAnalytics)
            mockNavContextState.dispatch(NavContext.AddNavContextAction(navContext1))
            mockNavContextState.dispatch(NavContext.AddNavContextAction(navContext2))
            advanceUntilIdle()
            mockNavContextState.dispatch(NavContext.RemoveNavContextAction(navContext2))
            advanceUntilIdle()

            verify(exactly = 1) { navContext2.navController.removeOnDestinationChangedListener(plugin) }
            verify(exactly = 0) { navContext1.navController.removeOnDestinationChangedListener(plugin) }

            verify(exactly = 1) { observer2.removeObserver() }
            verify(exactly = 0) { observer1.removeObserver() }
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

    private fun mockActivityObserverProvider(navContext: NavContext): NavControllerActivityObserver {
        val mockObserver: NavControllerActivityObserver = mockk(relaxed = true)
        every {
            provideNavControllerActivityObserver(
                plugin,
                navContext
            )
        } returns mockObserver
        every { mockObserver.find(navContext) } returns true
        return mockObserver
    }
}
