package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Destination
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.plugins.EventPlugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.PluginChain
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import com.rudderstack.sdk.kotlin.core.internals.utils.safelyExecute
import kotlinx.serialization.json.JsonObject
import java.util.concurrent.CopyOnWriteArrayList

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

    private lateinit var pluginChain: PluginChain
    private val pluginList = CopyOnWriteArrayList<Plugin>()
    private val destinationReadyCallbacks = mutableListOf<(Any?, DestinationResult) -> Unit>()

    @Volatile
    private var isPluginSetup = false

    @Volatile
    internal var isDestinationReady = false
        private set

    /**
     * The key for the destination present in the source config.
     */
    abstract val key: String

    /**
     * The configuration for the destination.
     * This variable always holds the latest configuration for the destination from [SourceConfig].
     */
    @Volatile
    var destinationConfig: JsonObject = emptyJsonObject
        private set

    /**
     * Creates the destination instance. Override this method for the initialisation of destination.
     * This method must return true if the destination was created successfully, false otherwise.
     *
     * @param destinationConfig The configuration for the destination.
     * @return true if the destination was created successfully, false otherwise.
     */
    protected abstract fun create(destinationConfig: JsonObject)

    /**
     * Returns the instance of the destination which was created.
     *
     * @return The instance of the destination.
     */
    abstract fun getDestinationInstance(): Any?

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

    internal fun findAndInitDestination(sourceConfig: SourceConfig) {
        findDestination(sourceConfig)?.let { configDestination ->
            if (!configDestination.isDestinationEnabled) {
                val errorMessage = "Destination $key is disabled in dashboard. " +
                    "No events will be sent to this destination."
                LoggerAnalytics.warn("IntegrationPlugin: $errorMessage")
                setFailureConfigAndNotifyCallbacks(IllegalStateException(errorMessage))
                return
            }
            createSafelyAndNotifyCallbacks(configDestination.destinationConfig)
        } ?: run {
            val errorMessage = "Destination $key not found in the source config. " +
                "No events will be sent to this destination."
            LoggerAnalytics.warn("IntegrationPlugin: $errorMessage")
            setFailureConfigAndNotifyCallbacks(IllegalStateException(errorMessage))
        }
    }

    final override suspend fun intercept(event: Event): Event {
        if (isDestinationReady) {
            event.copy<Event>()
                .let { pluginChain.applyPlugins(Plugin.PluginType.PreProcess, it) }
                ?.let { pluginChain.applyPlugins(Plugin.PluginType.OnProcess, it) }
                ?.let { handleEvent(it) }
        }

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

    /**
     * Registers a callback to be invoked when the destination of this plugin is ready.
     *
     * @param callback The callback to be invoked when the destination is ready.
     */
    fun onDestinationReady(callback: (Any?, DestinationResult) -> Unit) {
        getDestinationInstance()?.let { destinationInstance ->
            if (isDestinationReady) {
                callback(destinationInstance, Result.Success(Unit))
            } else {
                callback(
                    null,
                    Result.Failure(null, IllegalStateException("Destination $key is absent or disabled in dashboard."))
                )
            }
        } ?: run {
            synchronized(this) {
                destinationReadyCallbacks.add(callback)
            }
        }
    }

    private fun createSafelyAndNotifyCallbacks(destinationConfig: JsonObject) {
        safelyExecute(
            block = {
                if (getDestinationInstance() == null) {
                    create(destinationConfig)
                }
                LoggerAnalytics.debug("IntegrationPlugin: Destination $key created successfully.")
                setSuccessConfigAndNotifyCallbacks(destinationConfig)
            },
            onException = { exception ->
                LoggerAnalytics.error("IntegrationPlugin: Error: ${exception.message} initializing destination $key.")
                setFailureConfigAndNotifyCallbacks(exception)
            }
        )
    }

    private fun setFailureConfigAndNotifyCallbacks(throwable: Throwable) {
        this.destinationConfig = emptyJsonObject
        this.isDestinationReady = false
        notifyCallbacks(Result.Failure(null, throwable))
    }

    private fun setSuccessConfigAndNotifyCallbacks(destinationConfig: JsonObject) {
        this.destinationConfig = destinationConfig
        this.isDestinationReady = true
        notifyCallbacks(Result.Success(Unit))
    }

    private fun notifyCallbacks(destinationResult: DestinationResult) {
        synchronized(this) {
            destinationReadyCallbacks.forEach { callback -> callback(getDestinationInstance(), destinationResult) }
            destinationReadyCallbacks.clear()
        }
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
