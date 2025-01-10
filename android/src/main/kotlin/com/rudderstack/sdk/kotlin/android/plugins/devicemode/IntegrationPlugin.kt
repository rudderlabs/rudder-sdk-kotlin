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
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import kotlinx.serialization.json.JsonObject
import java.util.concurrent.CopyOnWriteArrayList

@Suppress("TooGenericExceptionCaught")
abstract class IntegrationPlugin : EventPlugin {

    final override val pluginType: Plugin.PluginType = Plugin.PluginType.Destination

    final override lateinit var analytics: Analytics

    abstract val key: String

    @Volatile
    internal var destinationState: DestinationState = DestinationState.Uninitialised
        private set

    private lateinit var pluginChain: PluginChain
    private val pluginList = CopyOnWriteArrayList<Plugin>()

    protected abstract fun create(destinationConfig: JsonObject, analytics: Analytics, config: Configuration): Boolean

    open fun getUnderlyingInstance(): Any? {
        return null
    }

    open fun flush() {}

    open fun reset() {}

    final override fun setup(analytics: Analytics) {
        super.setup(analytics)

        pluginChain = PluginChain().also { it.analytics = analytics }
    }

    internal fun initialize(sourceConfig: SourceConfig) {
        findDestination(sourceConfig)?.let { configDestination ->
            if (!configDestination.isDestinationEnabled) {
                LoggerAnalytics.warn(
                    "DestinationPlugin: Destination $key is disabled in dashboard. " +
                        "No events will be sent to this destination."
                )
                return
            }
            when (
                try {
                    create(
                        configDestination.destinationConfig,
                        analytics,
                        analytics.configuration as Configuration
                    )
                } catch (e: Exception) {
                    LoggerAnalytics.error("DestinationPlugin: Error: ${e.message} initializing destination $key.")
                    false
                }
            ) {
                true -> {
                    destinationState = DestinationState.Ready
                    LoggerAnalytics.debug("DestinationPlugin: Destination $key is ready.")
                    applyDefaultPlugins()
                    applyCustomPlugins()
                }

                false -> {
                    destinationState = DestinationState.Failed
                    LoggerAnalytics.debug("DestinationPlugin: Destination $key is not initialised.")
                }
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

    override fun teardown() {
        if (destinationState.isReady()) {
            pluginChain.removeAll()
        } else {
            pluginList.clear()
        }
    }

    fun add(plugin: Plugin) {
        if (destinationState.isReady()) {
            pluginChain.add(plugin)
        } else {
            pluginList.add(plugin)
        }
    }

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

typealias DestinationResult = Result<Unit, Unit>

internal enum class DestinationState {
    Ready,
    Uninitialised,
    Failed;

    fun isReady() = this == Ready
}
