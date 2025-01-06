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

    private lateinit var destinationPluginChain: PluginChain
    private val destinationPluginList: MutableList<DestinationPlugin> = mutableListOf()
    private val queuedEventsChannel: Channel<Event> = Channel(MAX_QUEUE_SIZE)

    override fun setup(analytics: Analytics) {
        super.setup(analytics)

        destinationPluginChain = PluginChain().also { it.analytics = analytics }
        analytics.analyticsScope.launch(analytics.analyticsDispatcher) {
            analytics.sourceConfigState
                .filter { it.source.isSourceEnabled }
                .first()
                .let { sourceConfig ->
                    destinationPluginList.forEach { destinationPlugin ->
                        destinationPluginChain.add(destinationPlugin)
                        // device mode destination SDKs are initialized here
                        destinationPlugin.initialize(sourceConfig)
                    }
                    processEvents()
                }
        }
    }

    override suspend fun intercept(event: Event): Event {
        LoggerAnalytics.debug("DeviceModeDestinationPlugin: queueing event")
        val queuedEventResult = queuedEventsChannel.trySend(event)

        if (queuedEventResult.isFailure) {
            popAndSendEvent(event)
        }

        return event
    }

    override fun teardown() {
        destinationPluginChain.removeAll()
        queuedEventsChannel.cancel()
        destinationPluginList.clear()
    }

    fun addDestination(plugin: DestinationPlugin) {
        destinationPluginList.add(plugin)
    }

    fun removeDestination(plugin: DestinationPlugin) {
        destinationPluginList.remove(plugin)
        destinationPluginChain.remove(plugin)
    }

    fun reset() {
        destinationPluginChain.applyClosure { plugin ->
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
        destinationPluginChain.applyClosure { plugin ->
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

    private fun popAndSendEvent(event: Event) {
        queuedEventsChannel.tryReceive()
        queuedEventsChannel.trySend(event)
    }

    private fun processEvents() {
        analytics.analyticsScope.launch(analytics.analyticsDispatcher) {
            for (event in queuedEventsChannel) {
                destinationPluginChain.process(event)
            }
        }
    }
}
