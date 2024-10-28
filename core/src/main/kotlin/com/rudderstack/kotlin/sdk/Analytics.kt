package com.rudderstack.kotlin.sdk

import com.rudderstack.kotlin.sdk.internals.logger.KotlinLogger
import com.rudderstack.kotlin.sdk.internals.models.GroupEvent
import com.rudderstack.kotlin.sdk.internals.models.LoggerManager
import com.rudderstack.kotlin.sdk.internals.models.Message
import com.rudderstack.kotlin.sdk.internals.models.Properties
import com.rudderstack.kotlin.sdk.internals.models.RudderOption
import com.rudderstack.kotlin.sdk.internals.models.RudderTraits
import com.rudderstack.kotlin.sdk.internals.models.ScreenEvent
import com.rudderstack.kotlin.sdk.internals.models.TrackEvent
import com.rudderstack.kotlin.sdk.internals.models.emptyJsonObject
import com.rudderstack.kotlin.sdk.internals.platform.Platform
import com.rudderstack.kotlin.sdk.internals.platform.PlatformType
import com.rudderstack.kotlin.sdk.internals.plugins.Plugin
import com.rudderstack.kotlin.sdk.internals.plugins.PluginChain
import com.rudderstack.kotlin.sdk.internals.statemanagement.SingleThreadStore
import com.rudderstack.kotlin.sdk.internals.statemanagement.Store
import com.rudderstack.kotlin.sdk.internals.utils.addNameAndCategoryToProperties
import com.rudderstack.kotlin.sdk.internals.utils.empty
import com.rudderstack.kotlin.sdk.plugins.LibraryInfoPlugin
import com.rudderstack.kotlin.sdk.plugins.PocPlugin
import com.rudderstack.kotlin.sdk.plugins.RudderStackDataplanePlugin
import com.rudderstack.kotlin.sdk.state.SourceConfigState
import com.rudderstack.kotlin.sdk.state.UserIdentityState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
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
) : CoroutineConfiguration by coroutineConfig, Platform {

    private val pluginChain: PluginChain = PluginChain().also { it.analytics = this }

    private var configurationStore: Store<SourceConfigState, SourceConfigState.UpdateAction> = SingleThreadStore(
        initialState = SourceConfigState.initialState(),
        reducer = SourceConfigState.SaveSourceConfigValuesReducer(configuration.storage, analyticsScope),
    )
    internal var userIdentityStore: Store<UserIdentityState, UserIdentityState.SetIdentityAction> = SingleThreadStore(
        initialState = UserIdentityState.currentState(configuration.storage),
        reducer = UserIdentityState.GenerateUserAnonymousID(analyticsScope),
    )

    // TODO("Add a way to stop this channel")
    private val processMessageChannel: Channel<Message> = Channel(Channel.UNLIMITED)

    init {
        setLogger()
        processMessages()
        setup()
        initializeUserIdentity()
    }

    private fun setLogger() {
        if (getPlatformType() == PlatformType.Server) {
            LoggerManager.setup(logger = KotlinLogger, logLevel = configuration.logLevel)
        }
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
                LoggerManager.error(exception.stackTraceToString())
            }
            override val analyticsScope: CoroutineScope = CoroutineScope(SupervisorJob() + handler)
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
     * @param options A [RudderOption] object to specify additional event options. Defaults to an empty RudderOption object.
     */
    @JvmOverloads
    fun track(name: String, properties: Properties = emptyJsonObject, options: RudderOption = RudderOption()) {
        val message = TrackEvent(
            event = name,
            properties = properties,
            options = options,
        )

        processMessageChannel.trySend(message)
    }

    /**
     * Record a custom screen view event with the specified screen name, category, properties, and options.
     * This function constructs a `ScreenEvent` message and processes it through the plugin chain.
     *
     * @param screenName The name of the screen to be tracked.
     * @param category The category of the screen to be tracked. Defaults to an empty string.
     * @param properties A [Properties] object containing key-value pairs of event properties. Defaults to an empty JSON object.
     * @param options A [RudderOption] object to specify additional event options. Defaults to an empty RudderOption object.
     */
    @JvmOverloads
    fun screen(
        screenName: String,
        category: String = String.empty(),
        properties: Properties = emptyJsonObject,
        options: RudderOption = RudderOption()
    ) {
        val updatedProperties = addNameAndCategoryToProperties(screenName, category, properties)

        val message = ScreenEvent(
            screenName = screenName,
            properties = updatedProperties,
            options = options,
        )

        processMessageChannel.trySend(message)
    }

    /**
     * Add the user to a group.
     * This function constructs a `GroupEvent` message and processes it through the plugin chain.
     *
     * @param groupId Group ID you want your user to attach to
     * @param traits A [RudderTraits] object containing key-value pairs of event traits. Defaults to an empty JSON object.
     * @param options A [RudderOption] object to specify additional event options. Defaults to an empty RudderOption object.
     */
    @JvmOverloads
    fun group(groupId: String, traits: RudderTraits = emptyJsonObject, options: RudderOption = RudderOption()) {
        val message = GroupEvent(
            groupId = groupId,
            traits = traits,
            options = options,
        )

        processMessageChannel.trySend(message)
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
        add(LibraryInfoPlugin())
        add(PocPlugin())
        add(RudderStackDataplanePlugin())

        analyticsScope.launch(analyticsDispatcher) {
            SourceConfigManager(analytics = this@Analytics, store = configurationStore).fetchSourceConfig()
        }
    }

    /**
     * Adds a plugin to the plugin chain. Plugins can modify, enrich, or process events before they are sent to the server.
     *
     * @param plugin The plugin to be added to the plugin chain.
     */
    fun add(plugin: Plugin) {
        this.pluginChain.add(plugin)
    }

    /**
     * Sets or updates the anonymous ID for the current user identity.
     *
     * The `setAnonymousId` method is used to update the `anonymousID` value within the `UserIdentityStore`.
     * This ID is typically generated automatically to track users who have not yet been identified
     * (e.g., before they log in or sign up). This function dispatches an action to modify the `UserIdentityState`,
     * ensuring that the new ID is correctly stored and managed.
     *
     * @param anonymousId The new anonymous ID to be set for the current user. This ID should be a unique,
     * non-null string used to represent the user anonymously.
     */
    fun setAnonymousId(anonymousId: String) {
        userIdentityStore.dispatch(
            action = UserIdentityState.SetIdentityAction(
                storage = configuration.storage,
                anonymousID = anonymousId
            )
        )
    }

    /**
     * Processes each message sequentially through the plugin chain and applies base data to the message.
     * All operations are executed within the `analyticsDispatcher` coroutine context.
     *
     * **NOTE**: This method can be called either before or after the initialization of all plugins (plugin setup occurs in the `init` method).
     * Events sent before this function is invoked will be queued and processed once this function is called, ensuring no events are lost.
     */
    private fun processMessages() {
        analyticsScope.launch(analyticsDispatcher) {
            for (message in processMessageChannel) {
                message.subscribeToUserIdentityState(userIdentityStore, getPlatformType())
                pluginChain.process(message)
            }
        }
    }

    private fun initializeUserIdentity() {
        userIdentityStore.dispatch(action = UserIdentityState.SetIdentityAction(configuration.storage))
    }

    override fun getPlatformType(): PlatformType = PlatformType.Server
}
