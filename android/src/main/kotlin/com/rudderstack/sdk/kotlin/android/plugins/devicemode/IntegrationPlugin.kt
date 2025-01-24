package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Destination
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.plugins.EventPlugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.PluginChain
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import com.rudderstack.sdk.kotlin.core.internals.utils.defaultExceptionHandler
import com.rudderstack.sdk.kotlin.core.internals.utils.safelyExecute
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.withLock

/**
 * Base plugin class for all integration plugins.
 *
 * An integration plugin is a plugin that is responsible for sending events directly
 * to a 3rd party destination without sending it to Rudder server first.
 */
@Suppress("TooManyFunctions")
abstract class IntegrationPlugin : EventPlugin {

    final override val pluginType: Plugin.PluginType = Plugin.PluginType.Destination

    final override lateinit var analytics: Analytics

    /**
     * The key for the destination present in the source config.
     */
    abstract val key: String

    private lateinit var pluginChain: PluginChain
    private val pluginList = CopyOnWriteArrayList<Plugin>()

    @Volatile
    private var isPluginSetup = false

    @Volatile
    private lateinit var destinationResult: DestinationResult
    private val callbacks = mutableListOf<(Any?, DestinationResult) -> Unit>()
    private val mutex = Mutex()

    /**
     * Creates the destination instance. Override this method for the initialisation of destination.
     * This method must return true if the destination was created successfully, false otherwise.
     *
     * @param destinationConfig The configuration for the destination.
     * @return true if the destination was created successfully, false otherwise.
     */
    protected abstract fun create(destinationConfig: JsonObject): Boolean

    /**
     * Updates the destination for subsequent fetches of [SourceConfig].
     * Override this method if any kind of updating/reinitialising is required for a destination when
     * a new [SourceConfig] is fetched from control plane.
     *
     * @param destinationConfig The newly fetched configuration for the destination.
     * @param isDestinationDisabled `true` if the destination is disabled or absent in the dashboard, `false` otherwise.
     *
     * **Note** - If the destination is disabled in dashboard, stop sending events to this destination.
     */
    protected abstract fun update(destinationConfig: JsonObject?, isDestinationDisabled: Boolean)

    /**
     * Returns the instance of the destination which was created.
     *
     * @return The instance of the destination.
     */
    open fun getDestinationInstance(): Any? {
        return null
    }

    /**
     * Override this method to control the behaviour of [Analytics.flush] for this destination.
     */
    open fun flush() {}

    /**
     * Override this method to control the behaviour of [Analytics.reset] for this destination.
     */
    open fun reset() {}

    final override fun setup(analytics: Analytics) {
        super.setup(analytics)

        pluginChain = PluginChain().also { it.analytics = analytics }
        isPluginSetup = true
        applyDefaultPlugins()
        applyCustomPlugins()
    }

    internal suspend fun findAndInitDestination(sourceConfig: SourceConfig) {
        findDestination(sourceConfig)?.let { configDestination ->
            if (!configDestination.isDestinationEnabled) {
                val errorMessage = "Destination $key is disabled in dashboard. " +
                    "No events will be sent to this destination."
                LoggerAnalytics.warn("IntegrationPlugin: $errorMessage")
                destinationResult = Result.Failure(null, SdkNotInitializedException(errorMessage))
                return
            }
            createSafelyAndSetResult(configDestination.destinationConfig)
        } ?: run {
            val errorMessage = "Destination $key not found in the source config. " +
                "No events will be sent to this destination."
            LoggerAnalytics.warn("IntegrationPlugin: $errorMessage")
            destinationResult = Result.Failure(null, SdkNotInitializedException(errorMessage))
        }
        notifyCallbacks()
    }

    internal suspend fun findAndUpdateDestination(sourceConfig: SourceConfig) {
        findDestination(sourceConfig)?.let { configDestination ->
            if (!configDestination.isDestinationEnabled) {
                LoggerAnalytics.warn(
                    "IntegrationPlugin: Destination $key is disabled in dashboard. " +
                        "No events will be sent to this destination."
                )
                updateSafely(null, true)
                return
            }
            updateSafely(configDestination.destinationConfig, false)
        } ?: run {
            updateSafely(null, true)
            LoggerAnalytics.warn(
                "IntegrationPlugin: Destination $key not found in the source config. " +
                    "No events will be sent to this destination."
            )
        }
    }

    final override suspend fun intercept(event: Event): Event {
        event.copy<Event>()
            .let { pluginChain.applyPlugins(Plugin.PluginType.PreProcess, it) }
            ?.let { pluginChain.applyPlugins(Plugin.PluginType.OnProcess, it) }
            ?.let { mutex.withLock { handleEvent(it) } }

        return event
    }

    /**
     * Override this method to cleanup any resources before this integration plugin is removed.
     *
     * **Note**: Calling of `super.teardown()` is recommended when overriding this method.
     */
    override fun teardown() {
        pluginList.clear()
        if (isPluginSetup) {
            pluginChain.removeAll()
        }
    }

    /**
     * This method adds a plugin to modify the events before sending to this destination.
     *
     * @param plugin The plugin to be added.
     */
    fun add(plugin: Plugin) {
        if (isPluginSetup) {
            pluginChain.add(plugin)
        } else {
            pluginList.add(plugin)
        }
    }

    /**
     * This method removes a plugin from the destination.
     *
     * @param plugin The plugin to be removed.
     */
    fun remove(plugin: Plugin) {
        pluginList.remove(plugin)
        if (isPluginSetup) {
            pluginChain.remove(plugin)
        }
    }

    suspend fun onDestinationReady(callback: (Any?, DestinationResult) -> Unit) {
        if (::destinationResult.isInitialized) {
            callback(getDestinationInstance(), destinationResult)
        } else {
            mutex.withLock {
                callbacks.add(callback)
            }
        }
    }

    private suspend fun notifyCallbacks() {
        mutex.withLock {
            callbacks.forEach { callback -> callback(getDestinationInstance(), destinationResult) }
            callbacks.clear()
        }
    }

    private suspend fun createSafelyAndSetResult(destinationConfig: JsonObject) {
        safelyExecute(
            block = {
                when (
                    mutex.withLock { create(destinationConfig) }
                ) {
                    true -> {
                        destinationResult = Result.Success(Unit)
                        LoggerAnalytics.debug("IntegrationPlugin: Destination $key is ready.")
                    }

                    false -> {
                        val errorMessage = "Destination $key failed to initialise."
                        destinationResult = Result.Failure(null, SdkNotInitializedException(errorMessage))
                        LoggerAnalytics.warn("IntegrationPlugin: $errorMessage")
                    }
                }
            },
            onException = {
                destinationResult = Result.Failure(null, it)
                LoggerAnalytics.error("IntegrationPlugin: Error: ${it.message} initializing destination $key.")
            }
        )
    }

    private suspend fun updateSafely(destinationConfig: JsonObject?, isDestinationDisabled: Boolean) {
        safelyExecute(
            block = {
                mutex.withLock { update(destinationConfig, isDestinationDisabled) }
            },
            onException = { exception ->
                defaultExceptionHandler(
                    errorMsg = "IntegrationPlugin: Error updating destination $key",
                    exception = exception,
                )
            }
        )
    }

    private fun applyDefaultPlugins() {
        // todo: add integrations options filtering and event filtering plugins here
    }

    private fun applyCustomPlugins() {
        pluginList.forEach { plugin -> add(plugin) }
        pluginList.clear()
    }

    private fun findDestination(sourceConfig: SourceConfig): Destination? {
        return sourceConfig.source.destinations.firstOrNull { it.destinationDefinition.displayName == key }
    }
}
