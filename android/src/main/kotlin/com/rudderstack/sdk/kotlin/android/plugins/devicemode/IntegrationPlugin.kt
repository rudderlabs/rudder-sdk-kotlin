package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Destination
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.plugins.EventPlugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.PluginChain
import kotlinx.serialization.json.JsonObject
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Base plugin class for all integration plugins.
 *
 * An integration plugin is a plugin that is responsible for sending events directly
 * to a 3rd party destination without sending it to Rudder server first.
 */
@Suppress("TooGenericExceptionCaught")
abstract class IntegrationPlugin : EventPlugin {

    final override val pluginType: Plugin.PluginType = Plugin.PluginType.Destination

    final override lateinit var analytics: Analytics

    /**
     * The key for the destination present in the source config.
     */
    abstract val key: String

    @Volatile
    internal var destinationState: DestinationState = DestinationState.Uninitialised
        private set

    private lateinit var pluginChain: PluginChain
    private val pluginList = CopyOnWriteArrayList<Plugin>()

    /**
     * Creates the destination instance. Override this method for the initialisation of destination.
     * This method must return true if the destination was created successfully, false otherwise.
     *
     * @param destinationConfig The configuration for the destination.
     * @param analytics The analytics instance.
     * @param config The configuration instance.
     * @return true if the destination was created successfully, false otherwise.
     */
    protected abstract fun create(destinationConfig: JsonObject, analytics: Analytics, config: Configuration): Boolean

    /**
     * Returns the instance of the destination which was created.
     *
     * @return The instance of the destination.
     */
    open fun getUnderlyingInstance(): Any? {
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
    }

    internal fun initialize(sourceConfig: SourceConfig) {
        findDestination(sourceConfig)?.let { configDestination ->
            if (!configDestination.isDestinationEnabled) {
                val errorMessage = "Destination $key is disabled in dashboard. No events will be sent to this destination."
                LoggerAnalytics.warn(errorMessage)
                destinationState = DestinationState.Failed(SdkNotInitializedException(errorMessage))
                return
            }

            try {
                when (
                    create(
                        configDestination.destinationConfig,
                        analytics,
                        analytics.configuration as Configuration
                    )
                ) {
                    true -> {
                        destinationState = DestinationState.Ready
                        LoggerAnalytics.debug("IntegrationPlugin: Destination $key is ready.")
                        applyDefaultPlugins()
                        applyCustomPlugins()
                    }
                    false -> {
                        val errorMessage = "Destination $key failed to initialise."
                        destinationState = DestinationState.Failed(SdkNotInitializedException(errorMessage))
                        LoggerAnalytics.warn("IntegrationPlugin: $errorMessage")
                    }
                }
            } catch (e: Exception) {
                destinationState = DestinationState.Failed(e)
                LoggerAnalytics.error("IntegrationPlugin: Error: ${e.message} initializing destination $key.")
            }
        }
    }

    final override suspend fun intercept(event: Event): Event {
        if (destinationState.isReady()) {
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
        if (destinationState.isReady()) {
            pluginChain.removeAll()
        } else {
            pluginList.clear()
        }
    }

    /**
     * This method adds a plugin to modify the events before sending to this destination.
     *
     * @param plugin The plugin to be added.
     */
    fun add(plugin: Plugin) {
        if (destinationState.isReady()) {
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
        if (destinationState.isReady()) {
            pluginChain.remove(plugin)
        } else {
            pluginList.remove(plugin)
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

internal sealed interface DestinationState {
    data object Ready : DestinationState
    data object Uninitialised : DestinationState
    data class Failed(
        val exception: Exception
    ) : DestinationState

    fun isReady() = this == Ready
}
