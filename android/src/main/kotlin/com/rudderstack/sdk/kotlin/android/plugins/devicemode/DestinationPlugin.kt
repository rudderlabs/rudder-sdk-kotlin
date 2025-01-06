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
import com.rudderstack.sdk.kotlin.core.internals.plugins.EventPlugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.PluginChain
import kotlinx.serialization.json.JsonObject

abstract class DestinationPlugin : EventPlugin {

    final override val pluginType: Plugin.PluginType = Plugin.PluginType.Destination

    final override lateinit var analytics: Analytics

    abstract val key: String
    internal var isDestinationReady: Boolean = false
        private set

    private lateinit var pluginChain: PluginChain
    private val pluginList: MutableList<Plugin> = mutableListOf()

    protected open fun create(destinationConfig: JsonObject, analytics: Analytics, config: Configuration): Any? {
        return null
    }

    protected open fun onDestinationReady(destination: Any?) {}

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
            val destination = create(
                configDestination.destinationConfig,
                analytics,
                analytics.configuration as Configuration
            )
            onDestinationReady(destination)
            isDestinationReady = true
            applyDefaultPlugins()
            applyCustomPlugins()
        }
    }

    final override suspend fun intercept(event: Event): Event {
        if (isDestinationReady) {
            event.copy<Event>()
                .let { pluginChain.applyPlugins(Plugin.PluginType.PreProcess, it) }
                ?.let { pluginChain.applyPlugins(Plugin.PluginType.OnProcess, it) }
                ?.let { processEvent(it) }
        }

        return event
    }

    @Suppress("TooGenericExceptionCaught")
    private fun processEvent(event: Event) {
        try {
            when (event) {
                is TrackEvent -> track(event)
                is ScreenEvent -> screen(event)
                is GroupEvent -> group(event)
                is IdentifyEvent -> identify(event)
                is AliasEvent -> alias(event)
            }
        } catch (e: Exception) {
            LoggerAnalytics.error(
                "DestinationPlugin: Error processing event " +
                    "for destination $key: ${e.message}"
            )
        }
    }

    override fun teardown() {
        if (isDestinationReady) {
            pluginChain.removeAll()
        } else {
            pluginList.clear()
        }
    }

    fun add(plugin: Plugin) {
        if (isDestinationReady) {
            pluginChain.add(plugin)
        } else {
            pluginList.add(plugin)
        }
    }

    fun remove(plugin: Plugin) {
        if (isDestinationReady) {
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
