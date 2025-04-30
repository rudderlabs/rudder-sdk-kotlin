package com.rudderstack.sdk.kotlin.core.javacompat

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.Configuration
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.javacompat.JsonInteropHelper.fromMap
import org.jetbrains.annotations.VisibleForTesting

/**
 * JavaAnalytics is a Java-compatible wrapper around the Analytics class.
 *
 * This class provides method overloads to ensure Java compatibility with the Kotlin Analytics API.
 * It delegates all operations to the underlying Analytics implementation.
 */
open class JavaAnalytics protected constructor(
    private val analytics: Analytics
) {

    /**
     * Creates a JavaAnalytics instance with the provided configuration.
     *
     * @param configuration The configuration to initialize the Analytics instance with.
     */
    constructor(configuration: Configuration) : this(provideAnalyticsInstance(configuration))

    /**
     * Tracks an event with the specified name.
     *
     * @param name The name of the event to track.
     */
    fun track(name: String,) {
        analytics.track(name = name)
    }

    /**
     * Tracks an event with the specified name and properties.
     *
     * @param name The name of the event to track.
     * @param properties A map of properties associated with the event.
     */
    fun track(name: String, properties: Map<String, Any>) {
        analytics.track(name = name, properties = fromMap(properties))
    }

    /**
     * Tracks an event with the specified name and options.
     *
     * @param name The name of the event to track.
     * @param options Additional options for tracking the event.
     */
    fun track(name: String, options: RudderOption) {
        analytics.track(name = name, options = options)
    }

    /**
     * Tracks an event with the specified name, properties, and options.
     *
     * @param name The name of the event to track.
     * @param properties A map of properties associated with the event.
     * @param options Additional options for tracking the event.
     */
    fun track(name: String, properties: Map<String, Any>, options: RudderOption) {
        analytics.track(name = name, properties = fromMap(properties), options = options)
    }

    /**
     * Tracks a screen view event with the specified screen name.
     *
     * @param screenName The name of the screen being viewed.
     */
    fun screen(screenName: String) {
        analytics.screen(screenName = screenName)
    }

    /**
     * Tracks a screen view event with the specified screen name and category.
     *
     * @param screenName The name of the screen being viewed.
     * @param category The category of the screen.
     */
    fun screen(screenName: String, category: String) {
        analytics.screen(screenName = screenName, category = category)
    }

    /**
     * Tracks a screen view event with the specified screen name and properties.
     *
     * @param screenName The name of the screen being viewed.
     * @param properties A map of additional properties associated with the screen view.
     */
    fun screen(screenName: String, properties: Map<String, Any>) {
        analytics.screen(screenName = screenName, properties = fromMap(properties))
    }

    /**
     * Tracks a screen view event with the specified screen name and options.
     *
     * @param screenName The name of the screen being viewed.
     * @param options Additional options for tracking the screen view event.
     */
    fun screen(screenName: String, options: RudderOption) {
        analytics.screen(screenName = screenName, options = options)
    }

    /**
     * Tracks a screen view event with the specified screen name, category, and properties.
     *
     * @param screenName The name of the screen being viewed.
     * @param category The category of the screen.
     * @param properties A map of additional properties associated with the screen view.
     */
    fun screen(screenName: String, category: String, properties: Map<String, Any>) {
        analytics.screen(screenName = screenName, category = category, properties = fromMap(properties))
    }

    /**
     * Tracks a screen view event with the specified screen name, category, and options.
     *
     * @param screenName The name of the screen being viewed.
     * @param category The category of the screen.
     * @param options Additional options for tracking the screen view event.
     */
    fun screen(screenName: String, category: String, options: RudderOption) {
        analytics.screen(screenName = screenName, category = category, options = options)
    }

    /**
     * Tracks a screen view event with the specified screen name, properties, and options.
     *
     * @param screenName The name of the screen being viewed.
     * @param properties A map of additional properties associated with the screen view.
     * @param options Additional options for tracking the screen view event.
     */
    fun screen(screenName: String, properties: Map<String, Any>, options: RudderOption) {
        analytics.screen(screenName = screenName, properties = fromMap(properties), options = options)
    }

    /**
     * Tracks a screen view event with the specified screen name, category, properties, and options.
     *
     * @param screenName The name of the screen being viewed.
     * @param category The category of the screen.
     * @param properties A map of additional properties associated with the screen view.
     * @param options Additional options for tracking the screen view event.
     */
    fun screen(screenName: String, category: String, properties: Map<String, Any>, options: RudderOption) {
        analytics.screen(screenName = screenName, category = category, properties = fromMap(properties), options = options)
    }
}

@VisibleForTesting
internal fun provideAnalyticsInstance(configuration: Configuration) = Analytics(configuration = configuration)
