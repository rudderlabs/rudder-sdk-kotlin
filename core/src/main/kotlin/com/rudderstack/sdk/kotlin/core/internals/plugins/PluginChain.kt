package com.rudderstack.sdk.kotlin.core.internals.plugins

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import kotlin.reflect.KClass

@InternalRudderApi
class PluginChain(
    private val pluginList: Map<Plugin.PluginType, PluginInteractor> = mapOf(
        Plugin.PluginType.PreProcess to PluginInteractor(),
        Plugin.PluginType.OnProcess to PluginInteractor(),
        Plugin.PluginType.Destination to PluginInteractor(),
        Plugin.PluginType.After to PluginInteractor(),
        Plugin.PluginType.Manual to PluginInteractor(),
    )
) {

    lateinit var analytics: Analytics

    suspend fun process(event: Event) {
        val preProcessResult = applyPlugins(Plugin.PluginType.PreProcess, event)
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

    fun <T : Plugin> findAll(pluginClass: KClass<T>): List<T> {
        val result = mutableListOf<T>()
        pluginList.forEach { (_, list) ->
            val found = list.findAll(pluginClass)
            result.addAll(found)
        }
        return result
    }

    fun removeAll() {
        applyClosure { it.teardown() }
        pluginList.forEach { (_, mediator) ->
            mediator.removeAll()
        }
    }

    suspend fun applyPlugins(pluginType: Plugin.PluginType, event: Event?): Event? {
        var result: Event? = event
        val mediator = pluginList[pluginType]
        result = applyPlugins(mediator, result)
        return result
    }

    private suspend fun applyPlugins(mediator: PluginInteractor?, event: Event?): Event? {
        var result: Event? = event
        result?.let { e ->
            result = mediator?.execute(e)
        }
        return result
    }
}
