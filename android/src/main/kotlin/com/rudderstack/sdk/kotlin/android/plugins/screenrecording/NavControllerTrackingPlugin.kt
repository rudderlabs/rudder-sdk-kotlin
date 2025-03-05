package com.rudderstack.sdk.kotlin.android.plugins.screenrecording

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.rudderstack.sdk.kotlin.android.state.NavContext
import com.rudderstack.sdk.kotlin.android.utils.automaticProperty
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin

internal const val FRAGMENT_NAVIGATOR_NAME = "fragment"
internal const val COMPOSE_NAVIGATOR_NAME = "composable"

internal class NavControllerTrackingPlugin : Plugin, NavController.OnDestinationChangedListener {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Utility

    override lateinit var analytics: Analytics

    private val currentNavContexts: MutableSet<NavContext> = mutableSetOf()
    private val activityObservers: MutableList<NavControllerActivityObserver> = mutableListOf()

    override fun teardown() {
        removeAllContextsAndObservers()
    }

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        when (destination.navigatorName) {
            FRAGMENT_NAVIGATOR_NAME -> trackFragmentScreen(destination)
            COMPOSE_NAVIGATOR_NAME -> trackComposeScreen(destination)
        }
    }

    fun addContextAndObserver(navContext: NavContext) {
        synchronized(this) {
            // adding navContext
            navContext.navController.addOnDestinationChangedListener(this)
            currentNavContexts.add(navContext)

            // adding activity observer
            val activityObserver = provideNavControllerActivityObserver(
                plugin = this,
                navContext = navContext
            )
            activityObserver.addObserver()
            activityObservers.add(activityObserver)
        }
    }

    private fun removeAllContextsAndObservers() {
        currentNavContexts
            .toList()
            .forEach { removeContextAndObserver(it) }
    }

    fun removeContextAndObserver(navContext: NavContext) {
        synchronized(this) {
            // removing navContext
            navContext.navController.removeOnDestinationChangedListener(this)
            currentNavContexts.remove(navContext)

            // removing activity observer
            val activityObserver = activityObservers.find { it.find(navContext) }
            activityObserver?.removeObserver()
            activityObservers.remove(activityObserver)
        }
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

internal fun provideNavControllerActivityObserver(
    plugin: NavControllerTrackingPlugin,
    navContext: NavContext
): NavControllerActivityObserver {
    return NavControllerActivityObserver(plugin, navContext)
}
