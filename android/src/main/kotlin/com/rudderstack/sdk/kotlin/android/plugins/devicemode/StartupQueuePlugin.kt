package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.PluginChain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.collections.HashSet

private const val MAX_QUEUE_SIZE = 1000

@OptIn(ExperimentalCoroutinesApi::class)
internal class StartupQueuePlugin(
    private val pluginChain: PluginChain
) : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics

    private val queuedEventsChannel: Channel<Event> = Channel(MAX_QUEUE_SIZE)
    private val queuedEventsIdsSet = HashSet<String>()

    override fun setup(analytics: Analytics) {
        super.setup(analytics)

        analytics.sourceConfigState.onEach { sourceConfig ->
            LoggerAnalytics.debug("StartupQueuePlugin: sourceConfig fetched")
            if (sourceConfig.source.isSourceEnabled) {
                processQueuedEvents()
            }
        }.launchIn(analytics.analyticsScope)
    }

    override suspend fun intercept(event: Event): Event? {
        if (event.messageId in queuedEventsIdsSet) {
            return event
        }
        LoggerAnalytics.debug("StartupQueuePlugin: queueing event")
        val queuedEventResult = queuedEventsChannel.trySend(event)

        when {
            queuedEventResult.isClosed -> {
                LoggerAnalytics.error("StartupQueuePlugin: queuedEventsChannel is closed, this should not happen.")
                return event
            }

            queuedEventResult.isFailure -> {
                val removedEvent = queuedEventsChannel.tryReceive().getOrNull()
                removedEvent?.let {
                    queuedEventsIdsSet.remove(it.messageId)
                }
                queuedEventsChannel.trySend(event)
            }
        }
        queuedEventsIdsSet.add(event.messageId)

        return null
    }

    private fun processQueuedEvents() {
        analytics.analyticsScope.launch(analytics.analyticsDispatcher) {
            for (event in queuedEventsChannel) {
                pluginChain.process(event)
                if (queuedEventsChannel.isEmpty) {
                    break
                }
            }
            pluginChain.remove(this@StartupQueuePlugin)
        }
    }

    override fun teardown() {
        queuedEventsChannel.cancel()
        queuedEventsIdsSet.clear()
    }
}
