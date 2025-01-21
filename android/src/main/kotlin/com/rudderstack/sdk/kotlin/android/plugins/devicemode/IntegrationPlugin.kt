package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing.EventFilteringPlugin
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Destination
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.plugins.EventPlugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.PluginChain
import com.rudderstack.sdk.kotlin.core.internals.utils.defaultExceptionHandler
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

    /**
     * The key for the destination present in the source config.
     */
    abstract val key: String

    @Volatile
    internal var integrationState: IntegrationState = IntegrationState.Uninitialised
        private set

    private lateinit var pluginChain: PluginChain
    private val pluginList = CopyOnWriteArrayList<Plugin>()

    @Volatile
    private var isPluginSetup = false

    /**
     * Creates the destination instance. Override this method for the initialisation of destination.
     * This method must return true if the destination was created successfully, false otherwise.
     *
     * @param destinationConfig The configuration for the destination.
     * @return true if the destination was created successfully, false otherwise.
     */
    protected abstract fun create(destinationConfig: JsonObject): Boolean

    /**
     * Updates the destination.
     * Override this method if any kind of updating/reinitialising is required for a destination when
     * a new [SourceConfig] is fetched from control plane.
     *
     * @param destinationConfig The newly fetched configuration for the destination.
     * @return true if the destination was updated successfully AND ready to accept new events, false otherwise.
     *
     * **Note**: If the destination is not ready to accept new events, return false. The false return value indicates
     * that no change to the state of the destination was made.
     */
    protected open fun update(destinationConfig: JsonObject): Boolean {
        return false
    }

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

    internal fun findAndInitDestination(sourceConfig: SourceConfig) {
        findDestinationAndExecuteBlock(sourceConfig) { destinationConfig ->
            createSafelyAndChangeState(destinationConfig)
        }
    }

    internal fun findAndUpdateDestination(sourceConfig: SourceConfig) {
        findDestinationAndExecuteBlock(sourceConfig) { destinationConfig ->
            updateSafelyAndChangeState(destinationConfig)
        }
    }

    final override suspend fun intercept(event: Event): Event {
        if (integrationState.isReady()) {
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

    private inline fun findDestinationAndExecuteBlock(sourceConfig: SourceConfig, block: (JsonObject) -> Unit) {
        findDestination(sourceConfig, key)?.let { configDestination ->
            if (!configDestination.isDestinationEnabled) {
                val errorMessage = "Destination $key is disabled in dashboard. No events will be sent to this destination."
                LoggerAnalytics.warn(errorMessage)
                integrationState = IntegrationState.Failed(SdkNotInitializedException(errorMessage))
                return
            }
            block(configDestination.destinationConfig)
        } ?: run {
            val errorMessage = "Destination $key not found in the source config. No events will be sent to this destination."
            integrationState = IntegrationState.Failed(SdkNotInitializedException(errorMessage))
            LoggerAnalytics.warn("IntegrationPlugin: $errorMessage")
        }
    }

    private fun createSafelyAndChangeState(destinationConfig: JsonObject) {
        safelyExecute(
            block = {
                when (create(destinationConfig)) {
                    true -> {
                        integrationState = IntegrationState.Ready
                        LoggerAnalytics.debug("IntegrationPlugin: Destination $key is ready.")
                    }
                    false -> {
                        val errorMessage = "Destination $key failed to initialise."
                        integrationState = IntegrationState.Failed(SdkNotInitializedException(errorMessage))
                        LoggerAnalytics.warn("IntegrationPlugin: $errorMessage")
                    }
                }
            },
            onException = {
                integrationState = IntegrationState.Failed(it)
                LoggerAnalytics.error("IntegrationPlugin: Error: ${it.message} initializing destination $key.")
            }
        )
    }

    private fun updateSafelyAndChangeState(destinationConfig: JsonObject) {
        safelyExecute(
            block = {
                when (update(destinationConfig)) {
                    true -> {
                        integrationState = IntegrationState.Ready
                        LoggerAnalytics.debug("IntegrationPlugin: Destination $key updated.")
                    }
                    false -> {
                        val errorMessage = "Destination $key failed to update."
                        LoggerAnalytics.debug("IntegrationPlugin: $errorMessage")
                    }
                }
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
        add(EventFilteringPlugin(key))
    }

    private fun applyCustomPlugins() {
        pluginList.forEach { plugin -> add(plugin) }
        pluginList.clear()
    }
}

internal fun findDestination(sourceConfig: SourceConfig, key: String): Destination? {
    return sourceConfig.source.destinations.firstOrNull { it.destinationDefinition.displayName == key }
}

internal sealed interface IntegrationState {
    data object Ready : IntegrationState
    data object Uninitialised : IntegrationState
    data class Failed(
        val exception: Exception
    ) : IntegrationState

    fun isReady() = this == Ready
}
