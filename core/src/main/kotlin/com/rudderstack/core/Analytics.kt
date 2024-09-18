package com.rudderstack.core

import com.rudderstack.core.internals.models.Message
import com.rudderstack.core.internals.models.Properties
import com.rudderstack.core.internals.models.RudderOption
import com.rudderstack.core.internals.models.TrackEvent
import com.rudderstack.core.internals.models.emptyJsonObject
import com.rudderstack.core.internals.plugins.Plugin
import com.rudderstack.core.internals.plugins.PluginChain
import com.rudderstack.core.internals.statemanagement.SingleThreadStore
import com.rudderstack.core.internals.statemanagement.Store
import com.rudderstack.core.plugins.PocPlugin
import com.rudderstack.core.plugins.RudderStackDataplanePlugin
import com.rudderstack.core.state.SourceConfigState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The `Analytics` class is the core of the RudderStack SDK, responsible for tracking events,
 * managing plugins, and handling the analytics lifecycle. It is designed to be highly customizable
 * with configurable coroutine scopes and dispatchers, and it supports a variety of plugins for
 * extensibility.
 *
 * This class handles the entire event processing flow from event creation to processing and sending.
 * It initializes with a `Configuration` object and optionally allows for custom coroutine
 * configurations via the `CoroutineConfiguration` interface.
 *
 * @constructor Primary constructor for creating an `Analytics` instance with a custom coroutine configuration.
 * @param configuration The configuration object that defines settings such as write key, data plane URL, logger, etc.
 * @param coroutineConfig The coroutine configuration that defines the scopes and dispatchers for analytics operations.
 */
@OptIn(ExperimentalCoroutinesApi::class)
open class Analytics protected constructor(
    val configuration: Configuration,
    coroutineConfig: CoroutineConfiguration,
) : CoroutineConfiguration by coroutineConfig {

    // Initial setup of the plugin chain, associating it with this Analytics instance
    private val pluginChain: PluginChain = PluginChain().also { it.analytics = this }
    private var store: Store<SourceConfigState, SourceConfigState.Update>

    init {
        store = SingleThreadStore(
            state = SourceConfigState.initialState(),
            reducer = SourceConfigState.SaveSourceConfigValues(configuration.storageProvider, stateScope),
        )
        setup()
    }

    /**
     * Secondary constructor for creating an `Analytics` instance with a default coroutine configuration.
     * The default configuration includes a coroutine scope with a SupervisorJob and a coroutine exception handler.
     *
     * @param configuration The configuration object defining settings such as write key, data plane URL, logger, etc.
     */
    constructor(configuration: Configuration) : this(
        configuration = configuration,
        coroutineConfig = object : CoroutineConfiguration {
            private val handler = CoroutineExceptionHandler { _, exception ->
                configuration.logger.error(log = exception.stackTraceToString())
            }
            override val analyticsScope: CoroutineScope = CoroutineScope(SupervisorJob() + handler)
            override val stateScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
            override val analyticsDispatcher: CoroutineDispatcher = Dispatchers.IO
            override val storageDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(2)
            override val networkDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)
        }
    )

    /**
     * Tracks a custom event with the specified name, properties, and options.
     * This function constructs a `TrackEvent` message and processes it through the plugin chain.
     *
     * @param name The name of the event to be tracked.
     * @param properties A [Properties] object containing key-value pairs of event properties. Defaults to an empty JSON object.
     * @param options A [RudderOption] object to specify additional event options.
     */
    @JvmOverloads
    fun track(name: String, properties: Properties = emptyJsonObject, options: RudderOption) {
        val message = TrackEvent(
            name = name,
            properties = properties,
            options = options,
        )
        process(message)
    }

    /**
     * Flushes all pending events that are currently queued in the plugin chain.
     * This method specifically targets the `RudderStackDataplanePlugin` to initiate the flush operation.
     */
    fun flush() {
        this.pluginChain.applyClosure {
            if (it is RudderStackDataplanePlugin) {
                it.flush()
            }
        }
    }

    /**
     * Sets up the initial plugin chain by adding the default plugins such as `PocPlugin`
     * and `RudderStackDataplanePlugin`. This function is called during initialization.
     */
    private fun setup() {
        add(PocPlugin())
        add(RudderStackDataplanePlugin())

        analyticsScope.launch(analyticsDispatcher) {
            SourceConfigManager(analytics = this@Analytics, store = store).fetchSourceConfig()
        }
    }

    /**
     * Adds a plugin to the plugin chain. Plugins can modify, enrich, or process events before they are sent to the server.
     *
     * @param plugin The plugin to be added to the plugin chain.
     */
    private fun add(plugin: Plugin) {
        this.pluginChain.add(plugin)
    }

    /**
     * Processes a message through the plugin chain. This method applies base data to the message,
     * then launches a coroutine to asynchronously process the message using the configured `analyticsDispatcher`.
     *
     * @param message The [Message] object representing an event or action to be processed.
     */
    private fun process(message: Message) {
        message.applyBaseData()
        analyticsScope.launch(analyticsDispatcher) {
            pluginChain.process(message)
        }
    }
}
