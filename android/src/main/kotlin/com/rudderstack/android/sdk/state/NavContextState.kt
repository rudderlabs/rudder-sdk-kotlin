package com.rudderstack.android.sdk.state

import android.app.Activity
import androidx.navigation.NavController
import com.rudderstack.kotlin.sdk.internals.statemanagement.Action
import com.rudderstack.kotlin.sdk.internals.statemanagement.FlowAction
import com.rudderstack.kotlin.sdk.internals.statemanagement.Reducer
import com.rudderstack.kotlin.sdk.internals.statemanagement.State
import org.jetbrains.annotations.VisibleForTesting

internal data class NavContext(
    val navController: NavController,
    val callingActivity: Activity,
) {

    companion object {

        fun initialState() = emptySet<NavContext>()
    }

    internal sealed interface NavContextAction : FlowAction<Set<NavContext>>

    internal class AddNavContextAction(private val navContext: NavContext) : NavContextAction {

        override fun reduce(currentState: Set<NavContext>): Set<NavContext> {
            return currentState.plus(navContext)
        }
    }

    internal class RemoveNavContextAction(@VisibleForTesting internal val navContext: NavContext) : NavContextAction {

        override fun reduce(currentState: Set<NavContext>): Set<NavContext> {
            return currentState.minus(navContext)
        }
    }

    internal object RemoveAllNavContextsAction : NavContextAction {

        override fun reduce(currentState: Set<NavContext>): Set<NavContext> {
            return emptySet()
        }
    }
}

internal data class NavContextState(
    val navContexts: Set<NavContext>,
) : State {

    companion object {

        fun initialState(): NavContextState {
            return NavContextState(emptySet())
        }
    }

    internal sealed interface NavContextAction : Action

    internal class AddNavContextAction(val navContext: NavContext) : NavContextAction

    internal class RemoveNavContextAction(val navController: NavController) : NavContextAction

    internal object RemoveAllNavContextsAction : NavContextAction

    internal class NavContextReducer : Reducer<NavContextState, NavContextAction> {

        override fun invoke(currentState: NavContextState, action: NavContextAction): NavContextState {
            return when (action) {
                is AddNavContextAction -> {
                    val updatedNavContexts = currentState.navContexts
                        .plus(action.navContext)

                    currentState.copy(
                        navContexts = updatedNavContexts
                    )
                }
                is RemoveNavContextAction -> {
                    val contextsToBeRemoved = currentState.navContexts.filter {
                        it.navController == action.navController
                    }.toSet()

                    val updatedNavContexts = currentState.navContexts.minus(contextsToBeRemoved)

                    currentState.copy(
                        navContexts = updatedNavContexts
                    )
                }
                is RemoveAllNavContextsAction -> {
                    currentState.copy(
                        navContexts = emptySet()
                    )
                }
            }
        }
    }
}
