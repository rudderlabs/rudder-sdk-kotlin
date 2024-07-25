package com.rudderstack.core

import com.rudderstack.core.internals.models.Message
import com.rudderstack.core.internals.models.Properties
import com.rudderstack.core.internals.models.RudderOption
import com.rudderstack.core.internals.models.TrackEvent
import com.rudderstack.core.internals.plugins.Plugin
import com.rudderstack.core.internals.plugins.PluginChain
import com.rudderstack.core.plugins.PocPlugin
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

open class Analytics(
    val configuration: Configuration
) {

    private val pluginChain: PluginChain = PluginChain().also { it.analytics = this }

    internal val analyticsScope: CoroutineScope = CoroutineScope(SupervisorJob())
    internal val analyticsDispatcher: CoroutineDispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
    internal val retryDispatcher: CoroutineDispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
    internal val storageIODispatcher: CoroutineDispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
    internal val networkIODispatcher: CoroutineDispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()

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
        options: RudderOption,
        properties: Properties,
    ) {
        val event = TrackEvent(
            event = name,
            options = options,
            properties = properties
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
