package com.rudderstack.core.plugins

import com.rudderstack.core.Analytics
import com.rudderstack.core.internals.models.FlushMessage
import com.rudderstack.core.internals.models.Message
import com.rudderstack.core.internals.models.TrackMessage
import com.rudderstack.core.internals.queue.MessageQueue
import com.rudderstack.core.internals.plugins.MessagePlugin
import com.rudderstack.core.internals.plugins.Plugin
import com.rudderstack.core.internals.plugins.PluginChain

class RudderstackDataplanePlugin : MessagePlugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Destination
    private val pluginChain: PluginChain = PluginChain()
    override lateinit var analytics: Analytics
    private var messageQueue: MessageQueue? = null

    override fun track(payload: TrackMessage): Message {
        enqueue(payload)
        return payload
    }

    override fun flush(payload: FlushMessage): Message {
        enqueue(payload)
        return payload
    }

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        pluginChain.analytics = analytics
        messageQueue = MessageQueue(analytics).apply { start() }
    }

    fun remove(plugin: Plugin) {
        pluginChain.remove(plugin)
    }

    fun flush() {
        messageQueue?.flush()
    }

    override fun execute(message: Message): Message? {
        val beforeResult = pluginChain.applyPlugins(Plugin.PluginType.PreProcess, message)
        val enrichmentResult = pluginChain.applyPlugins(Plugin.PluginType.OnProcess, beforeResult)
        val destinationResult = enrichmentResult?.let {
            when (it) {
                is TrackMessage -> {
                    track(it)
                }

                is FlushMessage -> {
                    flush(it)
                }
            }
        }
        val afterResult = pluginChain.applyPlugins(Plugin.PluginType.After, destinationResult)
        return afterResult
    }

    private fun enqueue(message: Message) {
        this.messageQueue?.put(message)
    }
}