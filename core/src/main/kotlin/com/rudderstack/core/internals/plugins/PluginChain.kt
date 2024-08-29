package com.rudderstack.core.internals.plugins

import com.rudderstack.core.Analytics
import com.rudderstack.core.internals.models.Message

internal class PluginChain(
    private val pluginList: Map<Plugin.PluginType, PluginInteractor> = mapOf(
        Plugin.PluginType.PreProcess to PluginInteractor(),
        Plugin.PluginType.OnProcess to PluginInteractor(),
        Plugin.PluginType.Destination to PluginInteractor(),
        Plugin.PluginType.After to PluginInteractor(),
        Plugin.PluginType.Manual to PluginInteractor(),
    )
) {

    lateinit var analytics: Analytics

    fun process(incomingEvent: Message) {
        if (analytics.configuration.optOut) {
            return
        }
        val preProcessResult = applyPlugins(Plugin.PluginType.PreProcess, incomingEvent)
        val onProcessResult = applyPlugins(Plugin.PluginType.OnProcess, preProcessResult)
        applyPlugins(Plugin.PluginType.Destination, onProcessResult)
    }

    fun add(plugin: Plugin) {
        plugin.setup(analytics)
        pluginList[plugin.pluginType]?.add(plugin)
    }

    fun remove(plugin: Plugin) {
        // remove all plugins with this name in every category
        pluginList.forEach { (_, list) ->
            val wasRemoved = list.remove(plugin)
            if (wasRemoved) {
                plugin.teardown()
            }
        }
    }

    fun applyClosure(closure: (Plugin) -> Unit) {
        pluginList.forEach { (_, mediator) ->
            mediator.applyClosure(closure)
        }
    }

    private fun applyPlugins(pluginType: Plugin.PluginType, event: Message?): Message? {
        var result: Message? = event
        val mediator = pluginList[pluginType]
        result = applyPlugins(mediator, result)
        return result
    }

    private fun applyPlugins(mediator: PluginInteractor?, event: Message?): Message? {
        var result: Message? = event
        result?.let { e ->
            result = mediator?.execute(e)
        }
        return result
    }
}