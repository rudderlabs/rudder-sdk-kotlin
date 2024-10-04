package com.rudderstack.android.sdk.plugins

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.rudderstack.android.sdk.state.NavControllerState
import com.rudderstack.android.sdk.state.NavControllers
import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.plugins.Plugin
import com.rudderstack.kotlin.sdk.internals.statemanagement.Store
import com.rudderstack.android.sdk.Configuration as AndroidConfiguration

private const val FRAGMENT_NAVIGATOR_NAME = "fragment"
private const val COMPOSE_NAVIGATOR_NAME = "composable"

internal class ScreenRecordingPlugin(
    private val navControllerStore: Store<NavControllerState, NavControllerState.NavControllerAction>
) : Plugin, NavController.OnDestinationChangedListener {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Manual

    override lateinit var analytics: Analytics

    private var currentNavControllers: NavControllers = emptySet()

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        (analytics.configuration as? AndroidConfiguration)?.let {
            navControllerStore.subscribe { currentState, _ ->
                updateNavControllers(currentState.navControllers)
            }
        }
    }

    override fun teardown() {
        super.teardown()
        updateNavControllers(emptySet())
    }

    private fun updateNavControllers(updatedNavControllers: NavControllers) {
        synchronized(this) {
            removeDeletedNavControllers(updatedNavControllers)
            setupAddedNavControllers(updatedNavControllers)
            currentNavControllers = updatedNavControllers
        }
    }

    private fun setupAddedNavControllers(updatedNavControllers: NavControllers) {
        val addedNavControllers =
            updatedNavControllers.minus(currentNavControllers)
        addedNavControllers.forEach { navController ->
            navController.get()?.addOnDestinationChangedListener(this)
        }
    }

    private fun removeDeletedNavControllers(updatedNavControllers: NavControllers) {
        val deletedNavControllers = currentNavControllers.minus(updatedNavControllers)
        deletedNavControllers.forEach { navController ->
            navController.get()?.removeOnDestinationChangedListener(this)
        }
    }

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        when (destination.navigatorName) {
            FRAGMENT_NAVIGATOR_NAME -> trackFragmentScreen(destination)
            COMPOSE_NAVIGATOR_NAME -> trackComposeScreen(destination)
        }
    }

    private fun trackFragmentScreen(destination: NavDestination) {
        analytics.screen(
            screenName = destination.label.toString()
        )
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
        analytics.screen(screenName)
    }
}
