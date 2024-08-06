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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

open class Analytics protected constructor(
    val configuration: Configuration,
    coroutineConfig: CoroutineConfiguration,
) : CoroutineConfiguration by coroutineConfig {

    private val pluginChain: PluginChain = PluginChain().also { it.analytics = this }

    init {
        setup()
    }

    constructor(
        configuration: Configuration,
    ) : this(
        configuration = configuration,
        coroutineConfig = object : CoroutineConfiguration {
            private val handler = CoroutineExceptionHandler { _, exception ->
                TODO("Handle the exception as needed")
            }

            override val analyticsScope: CoroutineScope =
                CoroutineScope(SupervisorJob() + handler)
            override val analyticsDispatcher: CoroutineDispatcher = Dispatchers.IO

            @OptIn(ExperimentalCoroutinesApi::class)
            override val networkDispatcher: CoroutineDispatcher =
                Dispatchers.IO.limitedParallelism(1)
        }
    )

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

    private fun process(event: Message) {
        event.applyBaseData()
        analyticsScope.launch(analyticsDispatcher) {
            pluginChain.process(event)
        }
    }

}
