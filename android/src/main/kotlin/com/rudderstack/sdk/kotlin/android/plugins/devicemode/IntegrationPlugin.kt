package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing.EventFilteringPlugin
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing.IntegrationOptionsPlugin
import com.rudderstack.sdk.kotlin.android.utils.findDestination
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
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

    final override val pluginType: Plugin.PluginType = Plugin.PluginType.Terminal

    final override lateinit var analytics: Analytics

    private lateinit var pluginChain: PluginChain
    private val pluginList = CopyOnWriteArrayList<Plugin>()
    private val destinationReadyCallbacks = mutableListOf<(Any?, DestinationResult) -> Unit>()

    private var isStandardIntegration: Boolean = true

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
     * Creates the destination instance. Override this method for the initialisation of destination.
     * This method must return true if the destination was created successfully, false otherwise.
     *
     * @param destinationConfig The configuration for the destination.
     */
    protected abstract fun create(destinationConfig: JsonObject)

    /**
     * This method will be called when the destination configuration is updated.
     * The value could be either destination config or empty json object.
     *
     * @param destinationConfig The updated configuration for the destination.
     */
    open fun update(destinationConfig: JsonObject) {}

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
        isStandardIntegration = this is StandardIntegration
        pluginChain = PluginChain().also { it.analytics = analytics }
        isPluginSetup = true
        pluginList.forEach { plugin -> add(plugin) }
        pluginList.clear()
        applyDefaultPlugins()
    }

    // todo: refactor this API to ensure that callbacks are invoked
    //  only once and destination is initialised only once even when this method is called multiple times.
    //  There should be no side effect of calling this method multiple times with same SourceConfig.
    internal fun initDestination(sourceConfig: SourceConfig) {
        isDestinationConfigured(sourceConfig)?.let { destinationConfig ->
            safelyInitOrUpdateAndNotify(destinationConfig)
        }
    }

    private fun isDestinationConfigured(sourceConfig: SourceConfig): JsonObject? {
        if (!isStandardIntegration) {
            return emptyJsonObject
        }
        findDestination(sourceConfig, key)?.let { configDestination ->
            if (!configDestination.isDestinationEnabled) {
                val errorMessage = "Destination $key is disabled in dashboard. " +
                    "No events will be sent to this destination."
                LoggerAnalytics.warn("IntegrationPlugin: $errorMessage")
                safelyUpdateOnFailureAndNotify(IllegalStateException(errorMessage))
                return null
            }
            return configDestination.destinationConfig
        } ?: run {
            val errorMessage = "Destination $key not found in the source config. " +
                "No events will be sent to this destination."
            LoggerAnalytics.warn("IntegrationPlugin: $errorMessage")
            safelyUpdateOnFailureAndNotify(IllegalStateException(errorMessage))
            return null
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
    // todo: refactor this API to support dynamic callbacks
    fun onDestinationReady(callback: (Any?, DestinationResult) -> Unit) {
        getDestinationInstance()?.let { destinationInstance ->
            if (isDestinationReady) {
                callback(destinationInstance, Result.Success(Unit))
            } else {
                callback(
                    null,
                    Result.Failure(IllegalStateException("Destination $key is absent or disabled in dashboard."))
                )
            }
        } ?: run {
            synchronized(this) {
                destinationReadyCallbacks.add(callback)
            }
        }
    }

    private fun safelyInitOrUpdateAndNotify(destinationConfig: JsonObject) {
        if (getDestinationInstance() == null) {
            safelyCreateAndNotify(destinationConfig)
        } else {
            safelyUpdateAndNotify(destinationConfig)
        }
    }

    private fun safelyCreateAndNotify(destinationConfig: JsonObject) {
        safelyExecute(
            block = {
                create(destinationConfig)
                LoggerAnalytics.debug("IntegrationPlugin: Destination $key created successfully.")
                this.isDestinationReady = true
                notifyCallbacks(Result.Success(Unit))
            },
            onException = { exception ->
                LoggerAnalytics.error("IntegrationPlugin: Error: ${exception.message} creating destination $key.")
                this.isDestinationReady = false
                notifyCallbacks(Result.Failure(exception))
            }
        )
    }

    private fun safelyUpdateOnFailureAndNotify(throwable: Throwable) {
        safelyUpdateAndApplyBlock(
            destinationConfig = emptyJsonObject,
            block = {
                LoggerAnalytics.debug("IntegrationPlugin: Destination $key updated with empty destinationConfig.")
                this.isDestinationReady = false
                notifyCallbacks(Result.Failure(throwable))
            }
        )
    }

    private fun safelyUpdateAndNotify(destinationConfig: JsonObject) {
        safelyUpdateAndApplyBlock(
            destinationConfig = destinationConfig,
            block = {
                LoggerAnalytics.debug(
                    "IntegrationPlugin: Destination $key updated with destinationConfig: $destinationConfig."
                )
                this.isDestinationReady = true
                notifyCallbacks(Result.Success(Unit))
            }
        )
    }

    private fun safelyUpdateAndApplyBlock(destinationConfig: JsonObject, block: () -> Unit) {
        safelyExecute(
            block = {
                if (isStandardIntegration) {
                    update(destinationConfig)
                    block()
                }
            },
            onException = { exception ->
                LoggerAnalytics.error("IntegrationPlugin: Error: ${exception.message} updating destination $key.")
                this.isDestinationReady = false
                notifyCallbacks(Result.Failure(exception))
            }
        )
    }

    private fun notifyCallbacks(destinationResult: DestinationResult) {
        synchronized(this) {
            destinationReadyCallbacks.forEach { callback -> callback(getDestinationInstance(), destinationResult) }
            destinationReadyCallbacks.clear()
        }
    }

    private fun applyDefaultPlugins() {
        add(EventFilteringPlugin(key))
        add(IntegrationOptionsPlugin(key))
    }
}
