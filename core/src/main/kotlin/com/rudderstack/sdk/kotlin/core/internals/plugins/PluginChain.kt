package com.rudderstack.sdk.kotlin.core.internals.plugins

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import kotlin.reflect.KClass

/**
 * PluginChain manages all the different types of plugins.
 */
@InternalRudderApi
class PluginChain(
    private val pluginList: Map<Plugin.PluginType, PluginInteractor> = mapOf(
        Plugin.PluginType.PreProcess to PluginInteractor(),
        Plugin.PluginType.OnProcess to PluginInteractor(),
        Plugin.PluginType.Terminal to PluginInteractor(),
        Plugin.PluginType.Manual to PluginInteractor(),
    )
) {

    /**
     * The analytics instance that the plugins are attached to.
     */
    lateinit var analytics: Analytics

    /**
     * Processes an event through the plugin chain.
     */
    suspend fun process(event: Event) {
        val preProcessResult = applyPlugins(Plugin.PluginType.PreProcess, event)
        val onProcessResult = applyPlugins(Plugin.PluginType.OnProcess, preProcessResult)
        applyPlugins(Plugin.PluginType.Terminal, onProcessResult)
    }

    /**
     * Adds a plugin to the plugin chain.
     */
    fun add(plugin: Plugin) {
        plugin.setup(analytics)
        pluginList[plugin.pluginType]?.add(plugin)
    }

    /**
     * Removes a plugin from the plugin chain.
     */
    fun remove(plugin: Plugin) {
        // remove all plugins with this name in every category
        pluginList.forEach { (_, list) ->
            val wasRemoved = list.remove(plugin)
            if (wasRemoved) {
                plugin.teardown()
            }
        }
    }

    /**
     * Applies a closure to all plugins in the plugin chain.
     */
    fun applyClosure(closure: (Plugin) -> Unit) {
        pluginList.forEach { (_, mediator) ->
            mediator.applyClosure(closure)
        }
    }

    /**
     * Finds all plugins of the given class and type in the plugin chain.
     */
    fun <T : Plugin> findAll(pluginType: Plugin.PluginType, pluginClass: KClass<T>): List<T> {
        val result = mutableListOf<T>()

        pluginList[pluginType]?.findAll(pluginClass)?.let {
            result.addAll(it)
        }
        return result
    }

    /**
     * Finds a plugin of the given class in the plugin chain.
     */
    fun removeAll() {
        applyClosure { it.teardown() }
        pluginList.forEach { (_, mediator) ->
            mediator.removeAll()
        }
    }

    /**
     * Executes an event through all the plugins of a plugin type.
     */
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
