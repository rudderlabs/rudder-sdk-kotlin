package com.rudderstack.android.android.state

import android.app.Activity
import androidx.navigation.NavController
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.FlowAction
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
