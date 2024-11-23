package com.rudderstack.kotlin.sdk

import com.rudderstack.kotlin.sdk.internals.logger.KotlinLogger
import com.rudderstack.kotlin.sdk.internals.logger.Logger
import com.rudderstack.kotlin.sdk.internals.logger.LoggerAnalytics
import com.rudderstack.kotlin.sdk.internals.models.AliasEvent
import com.rudderstack.kotlin.sdk.internals.models.GroupEvent
import com.rudderstack.kotlin.sdk.internals.models.IdentifyEvent
import com.rudderstack.kotlin.sdk.internals.models.Message
import com.rudderstack.kotlin.sdk.internals.models.Properties
import com.rudderstack.kotlin.sdk.internals.models.RudderOption
import com.rudderstack.kotlin.sdk.internals.models.RudderTraits
import com.rudderstack.kotlin.sdk.internals.models.ScreenEvent
import com.rudderstack.kotlin.sdk.internals.models.SourceConfig
import com.rudderstack.kotlin.sdk.internals.models.TrackEvent
import com.rudderstack.kotlin.sdk.internals.models.emptyJsonObject
import com.rudderstack.kotlin.sdk.internals.models.useridentity.ResetUserIdentityAction
import com.rudderstack.kotlin.sdk.internals.models.useridentity.SetUserIdForAliasEvent
import com.rudderstack.kotlin.sdk.internals.models.useridentity.SetUserIdTraitsAndExternalIdsAction
import com.rudderstack.kotlin.sdk.internals.models.useridentity.UserIdentity
import com.rudderstack.kotlin.sdk.internals.models.useridentity.resetUserIdentity
import com.rudderstack.kotlin.sdk.internals.models.useridentity.storeUserId
import com.rudderstack.kotlin.sdk.internals.models.useridentity.storeUserIdTraitsAndExternalIds
import com.rudderstack.kotlin.sdk.internals.platform.Platform
import com.rudderstack.kotlin.sdk.internals.platform.PlatformType
import com.rudderstack.kotlin.sdk.internals.plugins.Plugin
import com.rudderstack.kotlin.sdk.internals.plugins.PluginChain
import com.rudderstack.kotlin.sdk.internals.statemanagement.FlowState
import com.rudderstack.kotlin.sdk.internals.utils.addNameAndCategoryToProperties
import com.rudderstack.kotlin.sdk.internals.utils.empty
import com.rudderstack.kotlin.sdk.internals.utils.isAnalyticsActive
import com.rudderstack.kotlin.sdk.internals.utils.resolvePreferredPreviousId
import com.rudderstack.kotlin.sdk.plugins.LibraryInfoPlugin
import com.rudderstack.kotlin.sdk.plugins.PocPlugin
import com.rudderstack.kotlin.sdk.plugins.RudderStackDataplanePlugin
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

private lateinit var analyticsJob: Job

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
@Suppress("TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class)
open class Analytics protected constructor(
    val configuration: Configuration,
    coroutineConfig: CoroutineConfiguration,
) : CoroutineConfiguration by coroutineConfig, Platform {

    private val pluginChain: PluginChain = PluginChain().also { it.analytics = this }

    private val sourceConfigState = FlowState(initialState = SourceConfig.initialState())

    internal val userIdentityState = FlowState(initialState = UserIdentity.initialState(configuration.storage))

    private val processMessageChannel: Channel<Message> = Channel(Channel.UNLIMITED)
    private var processMessageJob: Job? = null

    @Volatile
    internal var isAnalyticsShutdown = false
        private set

    init {
        processMessages()
        setup()
        storeAnonymousId()
    }

    /**
     * Configures the logger for analytics with a specified `Logger` instance.
     *
     * This function sets up the `LoggerAnalytics` with the provided `logger` instance,
     * applying the log level specified in the configuration.
     *
     * @param logger The `Logger` instance to use for logging. Defaults to an instance of `KotlinLogger`.
     */
    fun setLogger(logger: Logger) {
        if (!isAnalyticsActive()) return
        LoggerAnalytics.setup(logger = logger, logLevel = configuration.logLevel)
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
                LoggerAnalytics.error(exception.stackTraceToString())
            }
            override val analyticsScope: CoroutineScope = run {
                analyticsJob = SupervisorJob()
                CoroutineScope(analyticsJob + handler)
            }
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
        if (!isAnalyticsActive()) return

        val message = TrackEvent(
            event = name,
            properties = properties,
            options = options,
            userIdentityState = userIdentityState.value,
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
        if (!isAnalyticsActive()) return

        val updatedProperties = addNameAndCategoryToProperties(screenName, category, properties)

        val message = ScreenEvent(
            screenName = screenName,
            properties = updatedProperties,
            options = options,
            userIdentityState = userIdentityState.value,
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
        if (!isAnalyticsActive()) return

        val message = GroupEvent(
            groupId = groupId,
            traits = traits,
            options = options,
            userIdentityState = userIdentityState.value,
        )

        processMessageChannel.trySend(message)
    }

    /**
     * The `identify` event allows you to identify a visiting user and associate their actions to that `userId`.
     * It also lets you record traits about the user like their name, email address, etc.
     *
     * @param userId The unique identifier for the user. Defaults to an empty string.
     * @param traits A [RudderTraits] object containing key-value pairs of user traits. Defaults to an empty JSON object.
     * @param options A [RudderOption] object to specify additional event options. Defaults to an empty RudderOption object.
     */
    @JvmOverloads
    fun identify(
        userId: String = String.empty(),
        traits: RudderTraits = emptyJsonObject,
        options: RudderOption = RudderOption()
    ) {
        if (!isAnalyticsActive()) return

        userIdentityState.dispatch(
            SetUserIdTraitsAndExternalIdsAction(
                newUserId = userId,
                newTraits = traits,
                newExternalIds = options.externalIds,
                analytics = this
            )
        )
        analyticsScope.launch {
            userIdentityState.value.storeUserIdTraitsAndExternalIds(
                storage = configuration.storage
            )
        }

        val message = IdentifyEvent(
            options = options,
            userIdentityState = userIdentityState.value,
        )

        processMessageChannel.trySend(message)
    }

    /**
     * The `alias` event allows merging multiple identities of a known user into a single unified profile.
     *
     * This event is specifically designed for merging user identities and does not modify the user’s traits
     * or other common properties. It links the [newId] with the [previousId], enabling consistent tracking
     * across different identifiers for the same user.
     *
     * @param newId The new identifier to be associated with the user, representing the updated or primary user ID.
     * @param previousId The previous ID tied to the user, which may be a user-provided value or fall back on prior identifiers.
     * @param options A [RudderOption] object to specify additional event options. Defaults to an empty RudderOption object.
     */
    fun alias(newId: String, previousId: String = String.empty(), options: RudderOption = RudderOption()) {
        if (!isAnalyticsActive()) return

        val updatedPreviousId = userIdentityState.value.resolvePreferredPreviousId(previousId)
        userIdentityState.dispatch(
            SetUserIdForAliasEvent(newId = newId)
        )
        analyticsScope.launch {
            userIdentityState.value.storeUserId(storage = configuration.storage)
        }

        val message = AliasEvent(
            previousId = updatedPreviousId,
            options = options,
            userIdentityState = userIdentityState.value,
        )

        processMessageChannel.trySend(message)
    }

    /**
     * Flushes all pending events that are currently queued in the plugin chain.
     * This method specifically targets the `RudderStackDataplanePlugin` to initiate the flush operation.
     */
    fun flush() {
        if (!isAnalyticsActive()) return

        this.pluginChain.applyClosure {
            if (it is RudderStackDataplanePlugin) {
                it.flush()
            }
        }
    }

    /**
     * Shuts down the analytics instance, stopping all the operations, removing all the plugins and freeing up the resources.
     * All the events made up to to the point of shutdown are written down on disk, but they are flushed only after next initialisation.
     *
     *  **NOTE**: This operation is irreversible. However, no saved data is lost in shutdown.
     */
    fun shutdown() {
        if (!isAnalyticsActive()) return

        isAnalyticsShutdown = true
        LoggerAnalytics.info("Initiating Analytics shutdown.")

        processMessageChannel.close()
        processMessageJob?.invokeOnCompletion {
            shutdownHook()
        }
    }

    private fun shutdownHook() {
        analyticsJob.invokeOnCompletion {
            this@Analytics.configuration.storage.close()
            LoggerAnalytics.info("Analytics shutdown completed.")
        }
        analyticsScope.launch {
            this@Analytics.pluginChain.removeAll()
        }.invokeOnCompletion {
            analyticsScope.cancel()
        }
    }

    /**
     * Sets up the initial plugin chain by adding the default plugins such as `PocPlugin`
     * and `RudderStackDataplanePlugin`. This function is called during initialization.
     */
    private fun setup() {
        setLogger(logger = KotlinLogger())
        add(LibraryInfoPlugin())
        add(PocPlugin())
        add(RudderStackDataplanePlugin())

        analyticsScope.launch(analyticsDispatcher) {
            SourceConfigManager(
                analytics = this@Analytics,
                sourceConfigState = sourceConfigState
            ).fetchAndUpdateSourceConfig()
        }
    }

    /**
     * Adds a plugin to the plugin chain. Plugins can modify, enrich, or process events before they are sent to the server.
     *
     * @param plugin The plugin to be added to the plugin chain.
     */
    fun add(plugin: Plugin) {
        if (!isAnalyticsActive()) return

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
        if (!isAnalyticsActive()) return

        userIdentityState.dispatch(UserIdentity.SetAnonymousIdAction(anonymousId))
        storeAnonymousId()
    }

    /**
     * The `getAnonymousId` method always retrieves the current anonymous ID.
     */
    fun getAnonymousId(): String {
        if (!isAnalyticsActive()) return String.empty()

        return userIdentityState.value.anonymousId
    }

    /**
     * Resets the user identity, clearing the user ID, traits, and external IDs.
     * If clearAnonymousId is true, clears the existing anonymous ID and generate a new one.
     *
     * @param clearAnonymousId A boolean flag to determine whether to clear the anonymous ID. Defaults to false.
     */
    open fun reset(clearAnonymousId: Boolean = false) {
        if (!isAnalyticsActive()) return

        userIdentityState.dispatch(ResetUserIdentityAction(clearAnonymousId))
        analyticsScope.launch {
            userIdentityState.value.resetUserIdentity(
                clearAnonymousId = clearAnonymousId,
                storage = configuration.storage,
            )
        }
    }

    /**
     * Processes each message sequentially through the plugin chain and applies base data to the message.
     * All operations are executed within the `analyticsDispatcher` coroutine context.
     *
     * **NOTE**: This method can be called either before or after the initialization of all plugins (plugin setup occurs in the `init` method).
     * Events sent before this function is invoked will be queued and processed once this function is called, ensuring no events are lost.
     */
    private fun processMessages() {
        processMessageJob = analyticsScope.launch(analyticsDispatcher) {
            for (message in processMessageChannel) {
                message.updateData(platform = getPlatformType())
                pluginChain.process(message)
            }
        }
    }

    private fun storeAnonymousId() {
        analyticsScope.launch(storageDispatcher) {
            userIdentityState.value.storeAnonymousId(storage = configuration.storage)
        }
    }

    override fun getPlatformType(): PlatformType = PlatformType.Server
}
