package com.rudderstack.android.sdk.state

import androidx.navigation.NavController
import com.rudderstack.android.sdk.state.NavContextState.AddNavContextAction
import com.rudderstack.android.sdk.state.NavContextState.RemoveNavContextAction
import com.rudderstack.android.sdk.state.NavContextState.NavContextReducer
import io.mockk.MockKAnnotations
import io.mockk.mockk
import io.mockk.unmockkAll
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.lang.ref.WeakReference

class NavContextStateTest {

    private lateinit var reducer: NavContextReducer

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        reducer = NavContextReducer()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `when initialState called, then it should return an empty NavControllerState`() {
        val initialState = NavContextState.initialState()

        // Assert that initial state is empty
        assertTrue(initialState.navControllers.isEmpty())
    }

    @Test
    fun `given empty initial state, when AddNavControllerAction called, then it should add a new NavController to the state`() {
        val navController: NavController = mockk()

        // Initial state with an empty set of NavControllers
        val initialState = NavContextState.initialState()

        val action = AddNavContextAction(navController)

        val newState = reducer.invoke(initialState, action)

        // Assert that the NavController was added
        assertEquals(1, newState.navControllers.size)
        assertEquals(navController, newState.navControllers.first().get())
    }

    @Test
    fun `given garbage collected controller in initial state, when AddNavControllerAction called, then remove null controllers`() {
        val navController: NavController = mockk()

        // Simulate a weak reference to a garbage collected NavController
        val weakNavController = WeakReference<NavController>(null)

        // Initial state with a garbage collected NavController
        val initialState = NavContextState(
            navControllers = setOf(weakNavController)
        )

        val action = AddNavContextAction(navController)

        val newState = reducer.invoke(initialState, action)

        // Assert that the old, garbage collected NavController is removed and the new one is added
        assertEquals(1, newState.navControllers.size)
        assertEquals(navController, newState.navControllers.first().get())
    }

    @Test
    fun `given initial state, when RemoveNavControllerAction called, then it should remove the specified NavController from the state`() {
        val navController1: NavController = mockk()
        val navController2: NavController = mockk()

        // Initial state with two NavControllers
        val initialState = NavContextState(
            navControllers = setOf(
                WeakReference(navController1),
                WeakReference(navController2)
            )
        )

        val action = RemoveNavContextAction(navController1)

        val newState = reducer.invoke(initialState, action)

        // Assert that navController1 was removed and navController2 remains
        assertEquals(1, newState.navControllers.size)
        assertEquals(navController2, newState.navControllers.first().get())
    }

    @Test
    fun `given garbage collected controller in initial state, when RemoveNavControllerAction called, then remove null controllers`() {
        val navController: NavController = mockk()

        // Simulate a weak reference to a garbage collected NavController
        val weakNavController = WeakReference<NavController>(null)

        val initialState = NavContextState(
            navControllers = setOf(
                weakNavController,
                WeakReference(navController)
            )
        )

        val action = RemoveNavContextAction(navController)

        val newState = reducer.invoke(initialState, action)

        // Assert that both the garbage collected and the valid NavController were removed
        assertTrue(newState.navControllers.isEmpty())
    }
}
