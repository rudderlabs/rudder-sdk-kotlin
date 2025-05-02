package com.rudderstack.sdk.kotlin.android.javacompat

import android.app.Activity
import androidx.navigation.NavController
import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.javacompat.JavaAnalytics
import org.jetbrains.annotations.VisibleForTesting

/**
 * Java-compatible wrapper for the Android Analytics class.
 */
class JavaAnalytics private constructor(
    private val analytics: Analytics
) : JavaAnalytics(analytics = analytics) {

    constructor(configuration: Configuration) : this(provideAnalyticsInstance(configuration))

    /**
     * Returns the current session identifier if a session is active, null otherwise
     */
    val sessionId: Long?
        get() = analytics.sessionId

    /**
     * Starts a new analytics session with an automatically generated session ID
     */
    fun startSession() {
        analytics.startSession()
    }

    /**
     * Starts a new analytics session with the specified custom session ID
     */
    fun startSession(sessionId: Long) {
        analytics.startSession(sessionId)
    }

    /**
     * Ends the current analytics session if one is active
     */
    fun endSession() {
        analytics.endSession()
    }

    /**
     * Clears all user data and identifiers from the analytics instance
     */
    override fun reset() {
        analytics.reset()
    }

    /**
     * Forces immediate dispatch of any queued analytics events
     */
    override fun flush() {
        analytics.flush()
    }

    /**
     * Configures automatic screen event tracking for navigation destinations
     */
    fun setNavigationDestinationsTracking(navController: NavController, activity: Activity) {
        analytics.setNavigationDestinationsTracking(navController, activity)
    }

    /**
     * Registers a custom plugin to extend analytics functionality
     */
    override fun add(plugin: Plugin) {
        analytics.add(plugin)
    }

    /**
     * Removes a previously registered plugin from the analytics instance
     */
    override fun remove(plugin: Plugin) {
        analytics.remove(plugin)
    }
}

@VisibleForTesting
internal fun provideAnalyticsInstance(configuration: Configuration) = Analytics(configuration = configuration)
