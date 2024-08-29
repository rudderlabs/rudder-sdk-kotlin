package com.rudderstack.core

import com.rudderstack.core.internals.models.Message
import com.rudderstack.core.internals.models.Properties
import com.rudderstack.core.internals.models.RudderOption
import com.rudderstack.core.internals.models.TrackEvent
import com.rudderstack.core.internals.models.emptyJsonObject
import com.rudderstack.core.internals.plugins.Plugin
import com.rudderstack.core.internals.plugins.PluginChain
import com.rudderstack.core.plugins.PocPlugin
import com.rudderstack.core.plugins.RudderstackDataplanePlugin
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


@OptIn(ExperimentalCoroutinesApi::class)
open class Analytics protected constructor(
    val configuration: Configuration,
    coroutineConfig: CoroutineConfiguration,
) : CoroutineConfiguration by coroutineConfig {

    private val pluginChain: PluginChain = PluginChain().also { it.analytics = this }

    init {
        setup()
    }

    constructor(configuration: Configuration) : this(
        configuration = configuration,
        coroutineConfig = object : CoroutineConfiguration {
            private val handler = CoroutineExceptionHandler { _, exception ->
                configuration.logger.error(log = exception.stackTraceToString())
            }
            override val analyticsScope: CoroutineScope = CoroutineScope(SupervisorJob() + handler)
            override val analyticsDispatcher: CoroutineDispatcher = Dispatchers.IO
            override val storageDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(2)
            override val networkDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)
        }
    )

    @JvmOverloads
    fun track(
        name: String,
        properties: Properties = emptyJsonObject,
        options: RudderOption,
    ) {
        val message = TrackEvent(
            name = name,
            properties = properties,
            options = options,
        )
        process(message)
    }

    fun flush() {
        this.pluginChain.applyClosure {
            if (it is RudderstackDataplanePlugin) {
                it.flush()
            }
        }
    }

    private fun process(message: Message) {
        message.applyBaseData()
        analyticsScope.launch(analyticsDispatcher) {
            pluginChain.process(message)
        }
    }

    private fun setup() {
        add(PocPlugin())
        add(RudderstackDataplanePlugin())
    }

    private fun add(plugin: Plugin) {
        this.pluginChain.add(plugin)
    }
}
