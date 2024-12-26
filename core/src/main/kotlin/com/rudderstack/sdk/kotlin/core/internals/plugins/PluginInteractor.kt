package com.rudderstack.sdk.kotlin.core.internals.plugins

import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

@InternalRudderApi
class PluginInteractor(private var pluginList: CopyOnWriteArrayList<Plugin> = CopyOnWriteArrayList()) {

    fun add(plugin: Plugin) = synchronized(pluginList) {
        pluginList.add(plugin)
    }

    fun remove(plugin: Plugin) = synchronized(pluginList) {
        pluginList.removeAll { it === plugin }
    }

    fun removeAll() = synchronized(pluginList) {
        pluginList.clear()
    }

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

    fun applyClosure(closure: (Plugin) -> Unit) {
        pluginList.forEach { plugin ->
            closure(plugin)
        }
    }

    fun <T : Plugin> find(pluginClass: KClass<T>): T? {
        pluginList.forEach {
            if (pluginClass.isInstance(it)) {
                return it as T
            }
        }
        return null
    }

    fun <T : Plugin> findAll(pluginClass: KClass<T>): List<T> {
        return pluginList.filter { pluginClass.isInstance(it) } as List<T>
    }
}
