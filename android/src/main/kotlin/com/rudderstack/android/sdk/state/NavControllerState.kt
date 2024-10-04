package com.rudderstack.android.sdk.state

import androidx.navigation.NavController
import com.rudderstack.kotlin.sdk.internals.statemanagement.Action
import com.rudderstack.kotlin.sdk.internals.statemanagement.Reducer
import com.rudderstack.kotlin.sdk.internals.statemanagement.State
import java.lang.ref.WeakReference

internal typealias NavControllers = Set<WeakReference<NavController>>

internal data class NavControllerState(
    val navControllers: NavControllers
) : State {

    companion object {

        fun initialState(): NavControllerState {
            return NavControllerState(emptySet())
        }
    }

    internal sealed interface NavControllerAction : Action

    internal class AddNavControllerAction(val navController: NavController) : NavControllerAction

    internal class RemoveNavControllerAction(val navController: NavController) : NavControllerAction

    internal class NavControllerReducer : Reducer<NavControllerState, NavControllerAction> {

        override fun invoke(currentState: NavControllerState, action: NavControllerAction): NavControllerState {
            // here when reference.get() returns null - it means they are garbage collected.
            return when (action) {
                is AddNavControllerAction -> {
                    val controllersToBeRemoved = currentState.navControllers.filter {
                        it.get() == null
                    }.toSet()

                    val updatedNavControllers = currentState.navControllers
                        .minus(controllersToBeRemoved)
                        .plus(WeakReference(action.navController))

                    currentState.copy(
                        navControllers = updatedNavControllers
                    )
                }
                is RemoveNavControllerAction -> {
                    val controllerToBeDeleted = currentState.navControllers.filter {
                        it.get() == action.navController || it.get() == null
                    }.toSet()

                    val updatedNavControllers = currentState.navControllers.minus(controllerToBeDeleted)

                    currentState.copy(
                        navControllers = updatedNavControllers
                    )
                }
            }
        }
    }
}
