package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.PluginChain
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val MAX_QUEUE_SIZE = 1000

/*
 * This plugin will host all the device mode destination plugins in its PluginChain instance.
 */
internal class DeviceModeDestinationPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Destination

    override lateinit var analytics: Analytics

    private lateinit var pluginChain: PluginChain
    private val queuedEventsChannel: Channel<Event> = Channel(MAX_QUEUE_SIZE)

    override fun setup(analytics: Analytics) {
        super.setup(analytics)

        pluginChain = PluginChain().also { it.analytics = analytics }
        analytics.analyticsScope.launch(analytics.analyticsDispatcher) {
            analytics.sourceConfigState.first().let { sourceConfig ->
                if (sourceConfig.source.isSourceEnabled) {
                    processQueuedEvents()
                }
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

    fun add(plugin: Plugin) {
        pluginChain.add(plugin)
    }

    fun remove(plugin: Plugin) {
        pluginChain.remove(plugin)
    }

    private fun processQueuedEvents() {
        analytics.analyticsScope.launch(analytics.analyticsDispatcher) {
            for (event in queuedEventsChannel) {
                pluginChain.process(event)
            }
        }
    }
}
