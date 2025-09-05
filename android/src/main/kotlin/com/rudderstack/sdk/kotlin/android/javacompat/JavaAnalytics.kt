package com.rudderstack.sdk.kotlin.android.javacompat

import android.app.Activity
import androidx.navigation.NavController
import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.core.internals.models.reset.ResetOptions
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
     * Returns the current session ID.
     *
     * @return The current session ID, or null if no session is active.
     */
    val sessionId: Long?
        get() = analytics.sessionId

    /**
     * Starts a new session with the given optional session ID.
     */
    fun startSession() {
        analytics.startSession()
    }

    /**
     * Starts a new session with the specified session ID
     *
     * @param sessionId The ID of the session to start.
     */
    fun startSession(sessionId: Long) {
        analytics.startSession(sessionId)
    }

    /**
     * Ends the current session.
     */
    fun endSession() {
        analytics.endSession()
    }

    override fun reset() {
        analytics.reset()
    }

    /**
     * Resets the user identity, clears the existing anonymous ID and
     * generate a new one, also clears the user ID and traits.
     */
    override fun reset(options: ResetOptions) {
        analytics.reset(options)
    }

    /**
     * Flushes all pending events that are currently queued in the plugin chain.
     * This method specifically targets the `RudderStackDataPlanePlugin` to initiate the flush operation.
     */
    override fun flush() {
        analytics.flush()
    }

    /**
     * Configures automatic screen event tracking for navigation destinations
     *
     * @param navController The NavController instance used for navigation.
     * @param activity The Activity instance where the navigation occurs.
     */
    fun setNavigationDestinationsTracking(navController: NavController, activity: Activity) {
        analytics.setNavigationDestinationsTracking(navController, activity)
    }

    /**
     * Adds a plugin to the plugin chain.
     *
     * @param plugin The plugin to be added to the plugin chain.
     */
    override fun add(plugin: Plugin) {
        analytics.add(plugin)
    }

    /**
     * Removes a plugin from the plugin chain.
     *
     * @param plugin The plugin to be removed from the plugin chain.
     */
    override fun remove(plugin: Plugin) {
        analytics.remove(plugin)
    }
}

@VisibleForTesting
internal fun provideAnalyticsInstance(configuration: Configuration) = Analytics(configuration = configuration)
