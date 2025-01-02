package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.PluginChain
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val MAX_QUEUE_SIZE = 1000

/*
 * This plugin will queue the events till the sourceConfig is fetched and
 * will host all the device mode destination plugins in its PluginChain instance.
 */
internal class DeviceModeDestinationPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Destination

    override lateinit var analytics: Analytics

    private lateinit var pluginChain: PluginChain
    private val pluginList: MutableList<DestinationPlugin> = mutableListOf()
    private val queuedEventsChannel: Channel<Event> = Channel(MAX_QUEUE_SIZE)

    override fun setup(analytics: Analytics) {
        super.setup(analytics)

        pluginChain = PluginChain().also { it.analytics = analytics }
        analytics.analyticsScope.launch(analytics.analyticsDispatcher) {
            analytics.sourceConfigState
                .filter { it.source.isSourceEnabled }
                .first()
                .let { sourceConfig ->
                    pluginList.forEach { plugin ->
                        // only destination plugins are added here
                        pluginChain.add(plugin)
                        // device mode destination SDKs are initialized here
                        plugin.initialize(sourceConfig)
                    }
                    processQueuedEvents()
                }
        }
    }

    override suspend fun intercept(event: Event): Event? {
        LoggerAnalytics.debug("DeviceModeDestinationPlugin: queueing event")
        val queuedEventResult = queuedEventsChannel.trySend(event)

        if (queuedEventResult.isFailure) {
            queuedEventsChannel.tryReceive()
            queuedEventsChannel.trySend(event)
        }

        return event
    }

    override fun teardown() {
        pluginChain.removeAll()
        queuedEventsChannel.cancel()
        pluginList.clear()
    }

    fun add(plugin: DestinationPlugin) {
        pluginList.add(plugin)
    }

    fun remove(plugin: DestinationPlugin) {
        pluginList.remove(plugin)
        pluginChain.remove(plugin)
    }

    fun reset() {
        pluginChain.applyClosure { plugin ->
            if (plugin is DestinationPlugin) {
                if (plugin.isDestinationReady) {
                    plugin.reset()
                } else {
                    LoggerAnalytics.debug(
                        "DeviceModeDestinationPlugin: Destination ${plugin.key} is " +
                            "not ready yet. Reset discarded."
                    )
                }
            }
        }
    }

    fun flush() {
        pluginChain.applyClosure { plugin ->
            if (plugin is DestinationPlugin) {
                if (plugin.isDestinationReady) {
                    plugin.flush()
                } else {
                    LoggerAnalytics.debug(
                        "DeviceModeDestinationPlugin: Destination ${plugin.key} is " +
                            "not ready yet. Flush discarded."
                    )
                }
            }
        }
    }

    private fun processQueuedEvents() {
        analytics.analyticsScope.launch(analytics.analyticsDispatcher) {
            for (event in queuedEventsChannel) {
                pluginChain.process(event)
            }
        }
    }
}
