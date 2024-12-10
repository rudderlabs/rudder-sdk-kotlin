package com.rudderstack.kotlin.core.internals.plugins

import com.rudderstack.kotlin.core.internals.models.Message
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

internal class PluginInteractor(private var pluginList: CopyOnWriteArrayList<Plugin> = CopyOnWriteArrayList()) {

    fun add(plugin: Plugin) = synchronized(pluginList) {
        pluginList.add(plugin)
    }

    fun remove(plugin: Plugin) = synchronized(pluginList) {
        pluginList.removeAll { it === plugin }
    }

    fun removeAll() = synchronized(pluginList) {
        pluginList.clear()
    }

    suspend fun execute(message: Message): Message? {
        var result: Message? = message

        pluginList.forEach { plugin ->
            result?.let { message ->
                val copy = message.copy<Message>()
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
