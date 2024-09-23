package com.rudderstack.core.plugins

import com.rudderstack.core.Analytics
import com.rudderstack.core.internals.models.FlushEvent
import com.rudderstack.core.internals.models.Message
import com.rudderstack.core.internals.models.TrackEvent
import com.rudderstack.core.internals.plugins.MessagePlugin
import com.rudderstack.core.internals.plugins.Plugin
import com.rudderstack.core.internals.plugins.PluginChain
import com.rudderstack.core.internals.queue.MessageQueue
import kotlinx.coroutines.launch

internal class RudderStackDataplanePlugin : MessagePlugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Destination
    private val pluginChain: PluginChain = PluginChain()
    override lateinit var analytics: Analytics
    private var messageQueue: MessageQueue? = null

    override fun track(payload: TrackEvent): Message {
        enqueue(payload)
        return payload
    }

    override fun flush(payload: FlushEvent): Message {
        enqueue(payload)
        return payload
    }

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        pluginChain.analytics = analytics
        messageQueue = MessageQueue(analytics).apply { start() }
    }

    internal fun remove(plugin: Plugin) {
        pluginChain.remove(plugin)
    }

    internal fun flush() {
        analytics.analyticsScope.launch(analytics.storageDispatcher) {
            messageQueue?.flush()
        }
    }

    override fun execute(message: Message): Message? {
        val beforeResult = pluginChain.applyPlugins(Plugin.PluginType.PreProcess, message)
        val enrichmentResult = pluginChain.applyPlugins(Plugin.PluginType.OnProcess, beforeResult)
        val destinationResult = enrichmentResult?.let {
            when (it) {
                is TrackEvent -> {
                    track(it)
                }

                is FlushEvent -> {
                    flush(it)
                }
            }
        }
        val afterResult = pluginChain.applyPlugins(Plugin.PluginType.After, destinationResult)
        return afterResult
    }

    private fun enqueue(message: Message) {
        analytics.analyticsScope.launch(analytics.storageDispatcher) {
            messageQueue?.put(message)
        }
    }
}
