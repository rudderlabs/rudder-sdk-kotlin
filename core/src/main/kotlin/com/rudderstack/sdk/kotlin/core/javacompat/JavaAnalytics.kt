package com.rudderstack.sdk.kotlin.core.javacompat

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.Configuration
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.javacompat.JsonInteropHelper.fromMap
import org.jetbrains.annotations.VisibleForTesting

/**
 * JavaAnalytics is a Java-compatible wrapper around the Analytics class.
 *
 * This class provides method overloads to ensure Java compatibility with the Kotlin Analytics API.
 * It delegates all operations to the underlying Analytics implementation.
 */
@Suppress("TooManyFunctions")
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
     * Get the anonymous ID.
     */
    val anonymousId: String?
        get() = analytics.anonymousId

    /**
     * Get the user ID.
     */
    val userId: String?
        get() = analytics.userId

    /**
     * Get the user traits.
     */
    val traits: Map<String, Any>?
        get() = analytics.traits?.toMap()

    /**
     * Tracks an event with the specified name.
     *
     * @param name The name of the event to track.
     */
    fun track(name: String) {
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
     * @param options A [RudderOption] object to specify additional event options.
     */
    fun track(name: String, options: RudderOption) {
        analytics.track(name = name, options = options)
    }

    /**
     * Tracks an event with the specified name, properties, and options.
     *
     * @param name The name of the event to track.
     * @param properties A map of properties associated with the event.
     * @param options A [RudderOption] object to specify additional event options.
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
     * @param options A [RudderOption] object to specify additional event options.
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
     * @param options A [RudderOption] object to specify additional event options.
     */
    fun screen(screenName: String, category: String, options: RudderOption) {
        analytics.screen(screenName = screenName, category = category, options = options)
    }

    /**
     * Tracks a screen view event with the specified screen name, properties, and options.
     *
     * @param screenName The name of the screen being viewed.
     * @param properties A map of additional properties associated with the screen view.
     * @param options A [RudderOption] object to specify additional event options.
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
     * @param options A [RudderOption] object to specify additional event options.
     */
    fun screen(screenName: String, category: String, properties: Map<String, Any>, options: RudderOption) {
        analytics.screen(screenName = screenName, category = category, properties = fromMap(properties), options = options)
    }

    /**
     * Add the user to a group.
     *
     * @param groupId The unique identifier for the group.
     */
    fun group(groupId: String) {
        analytics.group(groupId = groupId)
    }

    /**
     * Add the user to a group.
     *
     * @param groupId The unique identifier for the group.
     * @param traits A [RudderOption] object to specify additional event options.
     */
    fun group(groupId: String, traits: Map<String, Any>) {
        analytics.group(groupId = groupId, traits = fromMap(traits))
    }

    /**
     * Add the user to a group.
     *
     * @param groupId The unique identifier for the group.
     * @param options A [RudderOption] object to specify additional event options.
     */
    fun group(groupId: String, options: RudderOption) {
        analytics.group(groupId = groupId, options = options)
    }

    /**
     * Add the user to a group.
     *
     * @param groupId The unique identifier for the group.
     * @param traits A map containing additional information about the group.
     * @param options A [RudderOption] object to specify additional event options.
     */
    fun group(groupId: String, traits: Map<String, Any>, options: RudderOption) {
        analytics.group(groupId = groupId, traits = fromMap(traits), options = options)
    }

    /**
     * Identifies a user with the specified user ID.
     *
     * @param userId The unique identifier for the user.
     */
    fun identify(userId: String) {
        analytics.identify(userId = userId)
    }

    /**
     * Identifies a user with the specified traits.
     *
     * @param traits A map of properties or characteristics associated with the current user.
     */
    fun identify(traits: Map<String, Any>) {
        analytics.identify(traits = fromMap(traits))
    }

    /**
     * The `identify` event allows you to identify a visiting user and associate their actions to that `userId`.
     * It also lets you record traits about the user like their name, email address, etc.
     *
     * @param userId The unique identifier for the user.
     * @param traits A map of properties or characteristics associated with the user.
     */
    fun identify(userId: String, traits: Map<String, Any>) {
        analytics.identify(userId = userId, traits = fromMap(traits))
    }

    /**
     * The `identify` event allows you to identify a visiting user and associate their actions to that `userId`.
     * It also lets you record traits about the user like their name, email address, etc.
     *
     * @param userId The unique identifier for the user.
     * @param options A [RudderOption] object to specify additional event options.
     */
    fun identify(userId: String, options: RudderOption) {
        analytics.identify(userId = userId, options = options)
    }

    /**
     * The `identify` event allows you to identify a visiting user and associate their actions to that `userId`.
     * It also lets you record traits about the user like their name, email address, etc.
     *
     * @param traits A map of properties or characteristics associated with the current user.
     * @param options A [RudderOption] object to specify additional event options.
     */
    fun identify(traits: Map<String, Any>, options: RudderOption) {
        analytics.identify(traits = fromMap(traits), options = options)
    }

    /**
     * The `identify` event allows you to identify a visiting user and associate their actions to that `userId`.
     * It also lets you record traits about the user like their name, email address, etc.
     *
     * @param userId The unique identifier for the user.
     * @param traits A map of properties or characteristics associated with the user.
     * @param options A [RudderOption] object to specify additional event options.
     */
    fun identify(userId: String, traits: Map<String, Any>, options: RudderOption) {
        analytics.identify(userId = userId, traits = fromMap(traits), options = options)
    }

    /**
     * The `alias` event allows merging multiple identities of a known user into a single unified profile.
     *
     * @param newId The new identifier to be associated with the user, representing the updated or primary user ID.
     */
    fun alias(newId: String) {
        analytics.alias(newId = newId)
    }

    /**
     * The `alias` event allows merging multiple identities of a known user into a single unified profile.
     *
     * @param newId The new identifier to be associated with the user, representing the updated or primary user ID.
     * @param options A [RudderOption] object to specify additional event options.
     */
    fun alias(newId: String, options: RudderOption) {
        analytics.alias(newId = newId, options = options)
    }

    /**
     * The `alias` event allows merging multiple identities of a known user into a single unified profile.
     *
     * @param newId The new identifier to be associated with the user, representing the updated or primary user ID.
     * @param previousId The previous ID tied to the user, which may be a user-provided value or fall back on prior identifiers.
     */
    fun alias(newId: String, previousId: String) {
        analytics.alias(newId = newId, previousId = previousId)
    }

    /**
     * The `alias` event allows merging multiple identities of a known user into a single unified profile.
     *
     * @param newId The new identifier to be associated with the user, representing the updated or primary user ID.
     * @param previousId The previous ID tied to the user, which may be a user-provided value or fall back on prior identifiers.
     * @param options A [RudderOption] object to specify additional event options.
     */
    fun alias(newId: String, previousId: String, options: RudderOption) {
        analytics.alias(newId = newId, previousId = previousId, options = options)
    }

    /**
     * Forces immediate dispatch of any queued analytics events
     */
    open fun flush() {
        analytics.flush()
    }

    /**
     * Shuts down the analytics instance, releasing any resources and stopping event processing.
     */
    fun shutdown() {
        analytics.shutdown()
    }

    /**
     * Registers a custom plugin to extend analytics functionality
     */
    open fun add(plugin: Plugin) {
        analytics.add(plugin)
    }

    /**
     * Removes a previously registered plugin from the analytics instance
     */
    open fun remove(plugin: Plugin) {
        analytics.remove(plugin)
    }

    /**
     * Clears all user data and identifiers from the analytics instance
     */
    open fun reset() {
        analytics.reset()
    }
}

@VisibleForTesting
internal fun provideAnalyticsInstance(configuration: Configuration) = Analytics(configuration = configuration)
