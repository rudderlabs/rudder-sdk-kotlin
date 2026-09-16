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
     *
     * A plugin may return a newly constructed event rather than the one it was handed - the
     * contract allows it, and nothing about the return type says otherwise. Such an event carries
     * none of the SDK's own bookkeeping, because those are not constructor parameters, so the
     * chain re-establishes them here. Without this they are lost silently: no compile error, no
     * crash, and the reserved-key guard, the consent gate and the override detection all fall back
     * to treating the event as if the SDK had recorded nothing about it.
     *
     * Only a genuine replacement is touched; the common case of a plugin returning the event it
     * was given is left alone.
     */
    suspend fun execute(event: Event): Event? {
        var result: Event? = event

        pluginList.forEach { plugin ->
            result?.let { message ->
                val copy = message.copy<Event>()
                result = plugin.intercept(copy)?.also { returned ->
                    if (returned !== copy) returned.restoreSdkOwnedState(from = copy)
                }
            }
        }

        return result
    }

    /**
     * Re-applies the state the SDK owns rather than the plugin: the event's identity, the options
     * it was created with, and the values the SDK asserted for its reserved context keys. Payload
     * fields are deliberately left alone - reshaping those is what a plugin is for.
     */
    private fun Event.restoreSdkOwnedState(from: Event) {
        messageId = from.messageId
        options = from.options
        capturedReservedContext = from.capturedReservedContext
    }

    /**
     * Applies a closure on all registered plugins.
     */
    fun applyClosure(closure: (Plugin) -> Unit) {
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
