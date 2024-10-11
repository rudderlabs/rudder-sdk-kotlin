package com.rudderstack.android.sdk.state

import com.rudderstack.android.sdk.state.NavContextState.AddNavContextAction
import com.rudderstack.android.sdk.state.NavContextState.RemoveNavContextAction
import com.rudderstack.android.sdk.state.NavContextState.RemoveAllNavContextsAction
import com.rudderstack.android.sdk.state.NavContextState.NavContextReducer
import com.rudderstack.android.sdk.utils.mockNavContext
import io.mockk.MockKAnnotations
import io.mockk.unmockkAll
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

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
        assertTrue(initialState.navContexts.isEmpty())
    }

    @Test
    fun `given empty initial state, when AddNavContextAction called, then it should add a new NavContext to the state`() {
        val navContext: NavContext = mockNavContext()

        // Initial state with an empty set of NavControllers
        val initialState = NavContextState.initialState()

        val action = AddNavContextAction(navContext)

        val newState = reducer.invoke(initialState, action)

        // Assert that the NavController was added
        assertEquals(1, newState.navContexts.size)
        assertEquals(navContext, newState.navContexts.first())
    }

    @Test
    fun `given initial state, when RemoveNavContextAction called, then it should remove the specified NavContext from the state`() {
        val navContext1: NavContext = mockNavContext()
        val navContext2: NavContext = mockNavContext()

        // Initial state with two NavControllers
        val initialState = NavContextState(
            navContexts = setOf(navContext1, navContext2)
        )

        val action = RemoveNavContextAction(navContext1.navController)

        val newState = reducer.invoke(initialState, action)

        // Assert that navController1 was removed and navController2 remains
        assertEquals(1, newState.navContexts.size)
        assertEquals(navContext2, newState.navContexts.first())
    }

    @Test
    fun `given initial state, when RemoveAllNavContextsAction called, then it should remove all the NavContexts from the state`() {
        val navContext1: NavContext = mockNavContext()
        val navContext2: NavContext = mockNavContext()

        // Initial state with two NavControllers
        val initialState = NavContextState(
            navContexts = setOf(navContext1, navContext2)
        )

        val action = RemoveAllNavContextsAction

        val newState = reducer.invoke(initialState, action)

        // Assert that navController1 was removed and navController2 remains
        assertEquals(0, newState.navContexts.size)
    }
}
