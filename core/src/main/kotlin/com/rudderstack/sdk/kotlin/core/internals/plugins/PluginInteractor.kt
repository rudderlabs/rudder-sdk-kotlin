package com.rudderstack.sdk.kotlin.core.internals.plugins

import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

/**
 * Platform abstraction for managing all plugins and their execution.
 */
@InternalRudderApi
class PluginInteractor(private var pluginList: CopyOnWriteArrayList<Plugin> = CopyOnWriteArrayList()) {

    /**
     * Adds a plugin to the list of plugins.
     */
    fun add(plugin: Plugin) = synchronized(pluginList) {
        pluginList.add(plugin)
    }

    /**
     * Removes a plugin from the list of plugins.
     */
    fun remove(plugin: Plugin) = synchronized(pluginList) {
        pluginList.removeAll { it === plugin }
    }

    /**
     * Removes all plugins from the list.
     */
    fun removeAll() = synchronized(pluginList) {
        pluginList.clear()
    }

    /**
     * Executes all plugins in the list.
     */
    suspend fun execute(event: Event): Event? {
        var result: Event? = event

        pluginList.forEach { plugin ->
            result?.let { message ->
                val copy = message.copy<Event>()
                result = plugin.intercept(copy)
            }
        }

        return result
    }

    /**
     * Applies a closure on all registered plugins.
     */
    fun applyClosure(closure: (Plugin) -> Unit) {
        pluginList.forEach { plugin ->
            closure(plugin)
        }
    }

    suspend fun applySuspendingClosure(closure: suspend (Plugin) -> Unit) {
        pluginList.forEach { plugin ->
            closure(plugin)
        }
    }

    /**
     * Finds a plugin of the given class in the list
     * and returns it if found, otherwise returns null.
     */
    fun <T : Plugin> find(pluginClass: KClass<T>): T? {
        pluginList.forEach {
            if (pluginClass.isInstance(it)) {
                return it as T
            }
        }
        return null
    }

    /**
     * Finds all plugins of the given class in the list
     * and returns them as a list.
     */
    fun <T : Plugin> findAll(pluginClass: KClass<T>): List<T> {
        return pluginList.filter { pluginClass.isInstance(it) } as List<T>
    }
}
