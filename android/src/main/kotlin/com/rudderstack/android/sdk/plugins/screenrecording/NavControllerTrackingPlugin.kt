package com.rudderstack.android.sdk.plugins.screenrecording

import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.rudderstack.android.sdk.state.NavContext
import com.rudderstack.android.sdk.utils.automaticProperty
import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.plugins.Plugin
import com.rudderstack.kotlin.sdk.internals.statemanagement.FlowState
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import com.rudderstack.android.sdk.Configuration as AndroidConfiguration

internal const val FRAGMENT_NAVIGATOR_NAME = "fragment"
internal const val COMPOSE_NAVIGATOR_NAME = "composable"

// plugin for automatically tracking navControllers
internal class NavControllerTrackingPlugin(
    internal val navContextState: FlowState<Set<NavContext>>
) : Plugin, NavController.OnDestinationChangedListener {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Manual

    override lateinit var analytics: Analytics

    @VisibleForTesting
    internal val currentNavContexts: MutableSet<NavContext> = mutableSetOf()

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        (analytics.configuration as? AndroidConfiguration)?.let {
            navContextState.onEach { currentState ->
                updateNavContexts(currentState)
            }.launchIn(analytics.analyticsScope)
        }
    }

    override fun teardown() {
        navContextState.dispatch(NavContext.RemoveAllNavContextsAction)
    }

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        when (destination.navigatorName) {
            FRAGMENT_NAVIGATOR_NAME -> trackFragmentScreen(destination)
            COMPOSE_NAVIGATOR_NAME -> trackComposeScreen(destination)
        }
    }

    private fun updateNavContexts(updatedNavContexts: Set<NavContext>) {
        synchronized(this) {
            removeDeletedNavContexts(updatedNavContexts)
            setupAddedNavContexts(updatedNavContexts)
        }
    }

    private fun removeDeletedNavContexts(updatedNavContexts: Set<NavContext>) {
        val deletedNavContexts = currentNavContexts.minus(updatedNavContexts)
        deletedNavContexts.forEach { navContext ->
            removeContext(navContext)
        }
    }

    private fun setupAddedNavContexts(updatedNavContexts: Set<NavContext>) {
        val addedNavContexts = updatedNavContexts.minus(currentNavContexts)
        addedNavContexts.forEach { navContext ->
            addContextAndObserver(navContext)
        }
    }

    private fun removeContext(navContext: NavContext) {
        navContext.navController.removeOnDestinationChangedListener(this)
        currentNavContexts.remove(navContext)
    }

    private fun addContextAndObserver(navContext: NavContext) {
        // adding navContext
        navContext.navController.addOnDestinationChangedListener(this)
        currentNavContexts.add(navContext)

        // adding activity observer
        NavControllerActivityObserver(
            plugin = this,
            navContext = navContext
        )
    }

    private fun trackFragmentScreen(destination: NavDestination) {
        analytics.screen(screenName = destination.label.toString(), properties = automaticProperty())
    }

    private fun trackComposeScreen(destination: NavDestination) {
        val argumentKeys = destination.arguments.keys
        val screenName = destination.route?.let {
            if (argumentKeys.isEmpty()) {
                it
            } else {
                val argumentsIndex = it.indexOf('/')
                if (argumentsIndex == -1) {
                    it
                } else {
                    it.substring(0, argumentsIndex)
                }
            }
        }.toString()
        analytics.screen(screenName = screenName, properties = automaticProperty())
    }
}
