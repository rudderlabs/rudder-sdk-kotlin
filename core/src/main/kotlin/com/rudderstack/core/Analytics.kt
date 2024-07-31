package com.rudderstack.core

import com.rudderstack.core.internals.models.MessageEvent
import com.rudderstack.core.internals.models.TrackEvent
import com.rudderstack.core.internals.plugins.Plugin
import com.rudderstack.core.internals.plugins.PluginChain
import com.rudderstack.core.internals.utils.safeJsonObject
import com.rudderstack.core.internals.utils.toJsonElement
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
    private val analyticsScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val analyticsDispatcher: CoroutineDispatcher = Dispatchers.IO

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
        properties: Map<String, Any> = emptyMap(),
        options: Map<String, Any> = emptyMap(),
    ) {
        val event = TrackEvent(
            event = name,
            properties = properties.toJsonElement().safeJsonObject,
            options = options.toJsonElement().safeJsonObject,
        )
        process(event)
    }

    private fun process(event: MessageEvent) {
        analyticsScope.launch(analyticsDispatcher) {
            pluginChain.process(event)
        }
    }

}
