package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.PluginChain
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val MAX_QUEUE_SIZE = 1000

internal class StartupQueuePlugin(
    private val pluginChain: PluginChain
) : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics

    private val queuedEventsChannel: Channel<Event> = Channel(MAX_QUEUE_SIZE)
    private lateinit var pluginChainWithoutStartup: PluginChain

    init {
        analytics.sourceConfigState.onEach { sourceConfig ->
            val isEnabled = sourceConfig.source.isSourceEnabled
            if (isEnabled) {
                LoggerAnalytics.debug("StartupQueuePlugin sourceConfig fetched")
                processBufferedEvents()
            }
        }.launchIn(analytics.analyticsScope)
    }

    override suspend fun intercept(event: Event): Event? {
        // sourceConfig yet to be fetched, so queueing the event.
        LoggerAnalytics.debug("StartupQueuePlugin queueing event")
        if (queuedEventsChannel.trySend(event).isFailure) {
            // we have reached the max capacity of channel, so dropping the last event.
            queuedEventsChannel.tryReceive()
            queuedEventsChannel.trySend(event)
        }
        return null
    }

    private fun processBufferedEvents() {
        analytics.analyticsScope.launch(analytics.analyticsDispatcher) {
            initPluginChainWithoutStartup()

            for (event in queuedEventsChannel) {
                pluginChainWithoutStartup.process(event)
            }
        }
    }

    private fun initPluginChainWithoutStartup() {
        pluginChainWithoutStartup = PluginChain(
            pluginList = pluginChain.pluginList
        ).also { it.analytics = analytics }
        pluginChainWithoutStartup.remove(this@StartupQueuePlugin)
    }

    override fun teardown() {
        queuedEventsChannel.close()
    }
}
