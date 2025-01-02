package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing.IntegrationOptionsPlugin
import com.rudderstack.sdk.kotlin.android.utils.isFalseOrNull
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

    private var isDestinationDisabledInSource: Boolean = false

    private lateinit var pluginChain: PluginChain

    protected open fun create(destinationConfig: JsonObject, analytics: Analytics, config: Configuration): Any? {
        return null
    }

    final override fun setup(analytics: Analytics) {
        super.setup(analytics)

        pluginChain = PluginChain().also { it.analytics = analytics }
    }

    internal fun initialize(sourceConfig: SourceConfig) {
        val configDestination = findDestination(sourceConfig)
        isDestinationDisabledInSource = configDestination?.isDestinationEnabled.isFalseOrNull()

        if (isDestinationDisabledInSource) {
            LoggerAnalytics.warn("DestinationPlugin: Destination $key is disabled.")
            return
        }
        configDestination?.let {
            addDefaultPlugins()
            val destination = create(it.destinationConfig, analytics, analytics.configuration as Configuration)
            onDestinationReady(destination)
            isDestinationReady = true
        }
    }

    final override suspend fun intercept(event: Event): Event? {
        if (isDestinationDisabledInSource) {
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

    protected open fun onDestinationReady(destination: Any?) {}

    protected open fun track(event: TrackEvent) {}

    protected open fun screen(event: ScreenEvent) {}

    protected open fun group(event: GroupEvent) {}

    protected open fun identify(event: IdentifyEvent) {}

    protected open fun alias(event: AliasEvent) {}

    open fun flush() {}

    open fun reset() {}

    fun add(plugin: Plugin) {
        pluginChain.add(plugin)
    }

    fun remove(plugin: Plugin) {
        pluginChain.remove(plugin)
    }

    private fun addDefaultPlugins() {
        add(IntegrationOptionsPlugin(key))
    }

    private fun findDestination(sourceConfig: SourceConfig): Destination? {
        return sourceConfig.source.destinations.firstOrNull { it.destinationDefinition.displayName == key }
    }
}
