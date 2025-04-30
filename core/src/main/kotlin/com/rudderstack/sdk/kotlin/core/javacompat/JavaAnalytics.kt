package com.rudderstack.sdk.kotlin.core.javacompat

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.Configuration
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.javacompat.JsonInteropHelper.fromMap

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
    constructor(configuration: Configuration) : this(Analytics(configuration = configuration))

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
}
