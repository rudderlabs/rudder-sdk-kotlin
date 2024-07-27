package com.rudderstack.core

import com.rudderstack.core.internals.models.Message
import com.rudderstack.core.internals.models.Properties
import com.rudderstack.core.internals.models.RudderOption
import com.rudderstack.core.internals.models.TrackEvent
import com.rudderstack.core.internals.models.emptyJsonObject
import com.rudderstack.core.internals.plugins.Plugin
import com.rudderstack.core.internals.plugins.PluginChain
import com.rudderstack.core.plugins.PocPlugin
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

open class Analytics(
    val configuration: Configuration
) {

    private val pluginChain: PluginChain = PluginChain().also { it.analytics = this }

    internal val analyticsScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    internal val analyticsDispatcher: CoroutineDispatcher = Dispatchers.IO
    internal val retryDispatcher: CoroutineDispatcher = Dispatchers.IO
    internal val storageIODispatcher: CoroutineDispatcher = Dispatchers.IO
    internal val networkIODispatcher: CoroutineDispatcher = Dispatchers.IO

    init {
        setup()
    }

    private fun setup() {
        add(PocPlugin())
    }

    private fun add(plugin: Plugin) {
        this.pluginChain.add(plugin)
    }

    @JvmOverloads
    fun track(
        name: String,
        properties: Properties = emptyJsonObject,
        options: RudderOption,
    ) {
        val event = TrackEvent(
            event = name,
            properties = properties,
            options = options,
        )
        process(event)
    }

    fun process(event: Message) {
        event.applyBaseData()
        analyticsScope.launch(analyticsDispatcher) {
            pluginChain.process(event)
        }
    }

}
