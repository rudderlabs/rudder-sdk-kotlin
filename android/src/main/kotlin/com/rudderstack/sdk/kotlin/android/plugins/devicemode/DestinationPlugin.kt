package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.AliasEvent
import com.rudderstack.sdk.kotlin.core.internals.models.Destination
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.GroupEvent
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.PluginChain
import kotlinx.serialization.json.JsonObject

@Suppress("TooManyFunctions")
abstract class DestinationPlugin : Plugin {

    final override val pluginType: Plugin.PluginType = Plugin.PluginType.Destination

    final override lateinit var analytics: Analytics

    abstract val key: String
    var isDestinationReady: Boolean = false
        private set

    private lateinit var pluginChain: PluginChain
    private val pluginList: MutableList<Plugin> = mutableListOf()

    protected open fun create(destinationConfig: JsonObject, analytics: Analytics, config: Configuration): Any? {
        return null
    }

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
            val destination = create(
                configDestination.destinationConfig,
                analytics,
                analytics.configuration as Configuration
            )
            onDestinationReady(destination)
            isDestinationReady = true
            addDefaultPlugins()
            applyCustomPlugins()
        }
    }

    final override suspend fun intercept(event: Event): Event? {
        if (!isDestinationReady) {
            return event
        }

        var processedEvent: Event? = event.copy()
        processedEvent = pluginChain.applyPlugins(Plugin.PluginType.PreProcess, processedEvent)
        processedEvent = pluginChain.applyPlugins(Plugin.PluginType.OnProcess, processedEvent)
        if (processedEvent == null) {
            return event
        }

        when (processedEvent) {
            is TrackEvent -> track(processedEvent)
            is ScreenEvent -> screen(processedEvent)
            is GroupEvent -> group(processedEvent)
            is IdentifyEvent -> identify(processedEvent)
            is AliasEvent -> alias(processedEvent)
        }
        return event
    }

    override fun teardown() {
        pluginChain.removeAll()
        pluginList.clear()
    }

    protected open fun onDestinationReady(destination: Any?) {}

    protected open fun track(event: TrackEvent) {}

    protected open fun screen(event: ScreenEvent) {}

    protected open fun group(event: GroupEvent) {}

    protected open fun identify(event: IdentifyEvent) {}

    protected open fun alias(event: AliasEvent) {}

    open fun flush() {}

    open fun reset() {}

    fun add(plugin: Plugin) {
        if (isDestinationReady) {
            pluginChain.add(plugin)
        } else {
            pluginList.add(plugin)
        }
    }

    fun remove(plugin: Plugin) {
        pluginChain.remove(plugin)
        pluginList.remove(plugin)
    }

    private fun addDefaultPlugins() {
        // todo: add integrations options filtering and event filtering plugins here
    }

    private fun applyCustomPlugins() {
        pluginList.forEach { plugin -> pluginChain.add(plugin) }
        pluginList.clear()
    }

    private fun findDestination(sourceConfig: SourceConfig): Destination? {
        return sourceConfig.source.destinations.firstOrNull { it.destinationDefinition.displayName == key }
    }
}
