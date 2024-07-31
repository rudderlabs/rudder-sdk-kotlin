package com.rudderstack.core.internals.plugins

import com.rudderstack.core.internals.models.MessageEvent
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

internal class PluginInteractor(private var pluginList: CopyOnWriteArrayList<Plugin> = CopyOnWriteArrayList()) {

    fun add(plugin: Plugin) = synchronized(pluginList) {
        pluginList.add(plugin)
    }

    fun remove(plugin: Plugin) = synchronized(pluginList) {
        pluginList.removeAll { it === plugin }
    }

    fun execute(Message: MessageEvent): MessageEvent? {
        var result: MessageEvent? = Message

        pluginList.forEach { plugin ->
            result?.let { event ->
                val copy = event.copy<MessageEvent>()
                when (plugin) {
                    else -> {
                        result = plugin.execute(copy)
                    }
                }
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
