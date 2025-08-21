package com.rudderstack.sdk.kotlin.core

import com.rudderstack.sdk.kotlin.core.internals.logger.KotlinLogger
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.AliasEvent
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.GroupEvent
import com.rudderstack.sdk.kotlin.core.internals.models.IdentifyEvent
import com.rudderstack.sdk.kotlin.core.internals.models.Properties
import com.rudderstack.sdk.kotlin.core.internals.models.RudderOption
import com.rudderstack.sdk.kotlin.core.internals.models.ScreenEvent
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.Traits
import com.rudderstack.sdk.kotlin.core.internals.models.connectivity.ConnectivityState
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.ResetUserIdentityAction
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.SetUserIdAndTraitsAction
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.SetUserIdForAliasEvent
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.UserIdentity
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.resetUserIdentity
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.storeUserId
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.storeUserIdAndTraits
import com.rudderstack.sdk.kotlin.core.internals.platform.Platform
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.PluginChain
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import com.rudderstack.sdk.kotlin.core.internals.storage.provideBasicStorage
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.rudderstack.sdk.kotlin.core.internals.utils.UseWithCaution
import com.rudderstack.sdk.kotlin.core.internals.utils.addNameAndCategoryToProperties
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.internals.utils.isAnalyticsActive
import com.rudderstack.sdk.kotlin.core.internals.utils.isSourceEnabled
import com.rudderstack.sdk.kotlin.core.internals.utils.resolvePreferredPreviousId
import com.rudderstack.sdk.kotlin.core.plugins.LibraryInfoPlugin
import com.rudderstack.sdk.kotlin.core.plugins.RudderStackDataplanePlugin
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting


@Suppress("TooManyFunctions")
open class Analytics protected constructor(
    val configuration: Configuration,
    analyticsConfiguration: AnalyticsConfiguration,
    internal val userIdentityState: State<UserIdentity> = State(
        initialState = UserIdentity.initialState(
            analyticsConfiguration.storage
        )
    ),
) : AnalyticsConfiguration by analyticsConfiguration, Platform {

    private val pluginChain: PluginChain = PluginChain().also { it.analytics = this }

    /**
     * The `sourceConfigState` is a [State] that manages the source configuration for the analytics instance.
     */
    @InternalRudderApi
    val sourceConfigState = State(initialState = SourceConfig.initialState())

    private val processEventChannel: Channel<Event> = Channel(Channel.UNLIMITED)
    private var processEventJob: Job? = null

    @Volatile
    internal var isAnalyticsShutdown = false
        private set

    init {
        runForBaseTypeOnly()
        processEvents()
        setup()
        storeAnonymousId()
    }

    private fun runForBaseTypeOnly() {
        if (this::class == Analytics::class) {
            LoggerAnalytics.setPlatformLogger(logger = KotlinLogger())
            connectivityState.dispatch(ConnectivityState.SetDefaultStateAction())
            setupSourceConfig()
        }
    }

    protected fun setupSourceConfig() {
        this.sourceConfigManager = provideSourceConfigManager(
            analytics = this,
            sourceConfigState = sourceConfigState
        ).apply {
            fetchCachedSourceConfigAndNotifyObservers()
            refreshSourceConfigAndNotifyObservers()
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
        analyticsConfiguration = provideAnalyticsConfiguration(
            storage = provideBasicStorage(configuration.writeKey)
        )
    )

    /**
     * Tracks a custom event with the specified name, properties, and options.
     * This function constructs a `TrackEvent` event and processes it through the plugin chain.
     *
     * @param name The name of the event to be tracked.
     * @param properties A [Properties] object containing key-value pairs of event properties. Defaults to an empty JSON object.
     * @param options A [RudderOption] object to specify additional event options. Defaults to an empty RudderOption object.
     */
    @JvmOverloads
    fun track(name: String, properties: Properties = emptyJsonObject, options: RudderOption = RudderOption()) {
        if (!isAnalyticsActive() || !isSourceEnabled()) return

        val event = TrackEvent(
            event = name,
            properties = properties,
            options = options,
            userIdentityState = userIdentityState.value,
        )

        processEventChannel.trySend(event)
    }

    /**
     * Record a custom screen view event with the specified screen name, category, properties, and options.
     * This function constructs a `ScreenEvent` event and processes it through the plugin chain.
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
        if (!isAnalyticsActive() || !isSourceEnabled()) return

        val updatedProperties = addNameAndCategoryToProperties(screenName, category, properties)

        val event = ScreenEvent(
            screenName = screenName,
            properties = updatedProperties,
            options = options,
            userIdentityState = userIdentityState.value,
        )

        processEventChannel.trySend(event)
    }

    /**
     * Add the user to a group.
     * This function constructs a `GroupEvent` event and processes it through the plugin chain.
     *
     * @param groupId Group ID you want your user to attach to
     * @param traits A [Traits] object containing key-value pairs of event traits. Defaults to an empty JSON object.
     * @param options A [RudderOption] object to specify additional event options. Defaults to an empty RudderOption object.
     */
    @JvmOverloads
    fun group(groupId: String, traits: Traits = emptyJsonObject, options: RudderOption = RudderOption()) {
        if (!isAnalyticsActive() || !isSourceEnabled()) return

        val event = GroupEvent(
            groupId = groupId,
            traits = traits,
            options = options,
            userIdentityState = userIdentityState.value,
        )

        processEventChannel.trySend(event)
    }

    /**
     * The `identify` event allows you to identify a visiting user and associate their actions to that `userId`.
     * It also lets you record traits about the user like their name, email address, etc.
     *
     * @param userId The unique identifier for the user. Defaults to an empty string.
     * @param traits A [Traits] object containing key-value pairs of user traits. Defaults to an empty JSON object.
     * @param options A [RudderOption] object to specify additional event options. Defaults to an empty RudderOption object.
     */
    @JvmOverloads
    fun identify(userId: String = String.empty(), traits: Traits = emptyJsonObject, options: RudderOption = RudderOption()) {
        if (!isAnalyticsActive()) return

        if (!this.userId.isNullOrEmpty() && this.userId != userId) {
            reset()
        }

        userIdentityState.dispatch(
            SetUserIdAndTraitsAction(
                newUserId = userId,
                newTraits = traits,
            )
        )
        analyticsScope.launch(keyValueStorageDispatcher) {
            userIdentityState.value.storeUserIdAndTraits(
                storage = storage
            )
        }

        if (!isSourceEnabled()) return

        val event = IdentifyEvent(
            options = options,
            userIdentityState = userIdentityState.value,
        )

        processEventChannel.trySend(event)
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
    @JvmOverloads
    fun alias(newId: String, previousId: String = String.empty(), options: RudderOption = RudderOption()) {
        if (!isAnalyticsActive()) return

        val updatedPreviousId = userIdentityState.value.resolvePreferredPreviousId(previousId)
        userIdentityState.dispatch(
            SetUserIdForAliasEvent(newId = newId)
        )
        analyticsScope.launch(keyValueStorageDispatcher) {
            userIdentityState.value.storeUserId(storage = storage)
        }

        if (!isSourceEnabled()) return

        val event = AliasEvent(
            previousId = updatedPreviousId,
            options = options,
            userIdentityState = userIdentityState.value,
        )

        processEventChannel.trySend(event)
    }

    /**
     * Flushes all pending events that are currently queued in the plugin chain.
     * This method specifically targets the `RudderStackDataPlanePlugin` to initiate the flush operation.
     */
    open fun flush() {
        if (!isAnalyticsActive() || !isSourceEnabled()) return

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

        processEventChannel.close()
        processEventJob?.invokeOnCompletion {
            shutdownHook()
        }
    }

    private fun shutdownHook() {
        analyticsJob.invokeOnCompletion {
            closeAndCleanupStorage()
            LoggerAnalytics.info("Analytics shutdown completed.")
        }
        analyticsScope.launch {
            this@Analytics.pluginChain.removeAll()
        }.invokeOnCompletion {
            analyticsScope.cancel()
        }
    }

    @OptIn(UseWithCaution::class)
    private fun closeAndCleanupStorage() {
        storage.run {
            close()
            if (isInvalidWriteKey) {
                delete()
            }
        }
    }

    /**
     * Sets up the initial plugin chain by adding the default plugins such as `PocPlugin`
     * and `RudderStackDataPlanePlugin`. This function is called during initialization.
     */
    private fun setup() {
        add(LibraryInfoPlugin())
        add(RudderStackDataplanePlugin())
    }

    /**
     * Adds a plugin to the plugin chain. Plugins can modify, enrich, or process events before they are sent to the server.
     *
     * @param plugin The plugin to be added to the plugin chain.
     */
    open fun add(plugin: Plugin) {
        if (!isAnalyticsActive()) return

        this.pluginChain.add(plugin)
    }

    /**
     * Removes a plugin from the plugin chain.
     *
     * @param plugin The plugin to be removed from the plugin chain.
     */
    open fun remove(plugin: Plugin) {
        if (!isAnalyticsActive()) return

        this.pluginChain.remove(plugin)
    }

    /**
     * Resets the user identity, clears the existing anonymous ID and
     * generate a new one, also clears the user ID and traits.
     */
    open fun reset() {
        if (!isAnalyticsActive()) return

        userIdentityState.dispatch(ResetUserIdentityAction)
        analyticsScope.launch(keyValueStorageDispatcher) {
            userIdentityState.value.resetUserIdentity(
                storage = storage,
            )
        }
    }

    /**
     * Processes each event sequentially through the plugin chain and applies base data to the event.
     * All operations are executed within the `analyticsDispatcher` coroutine context.
     *
     * **NOTE**: This method can be called either before or after the initialization of all plugins (plugin setup occurs in the `init` method).
     * Events sent before this function is invoked will be queued and processed once this function is called, ensuring no events are lost.
     */
    private fun processEvents() {
        processEventJob = analyticsScope.launch(analyticsDispatcher) {
            for (event in processEventChannel) {
                event.updateData(platform = getPlatformType())
                pluginChain.process(event)
            }
        }
    }

    override fun getPlatformType(): PlatformType = PlatformType.Server

    /**
     * Get the stored anonymous ID.
     *
     * The `analyticsInstance.anonymousId` is used to update and get the `anonymousID` value.
     * This ID is typically generated automatically to track users who have not yet been identified
     * (e.g., before they log in or sign up).
     *
     * **Note**: This will return null if the [Analytics] instance is shut down.
     *
     * Get the anonymousId:
     * ```kotlin
     * val anonymousId = analyticsInstance.anonymousId
     * ```
     */
    val anonymousId: String?
        get() {
            if (!isAnalyticsActive()) return null
            return userIdentityState.value.anonymousId
        }

    /**
     * Get the user ID.
     *
     * The `analyticsInstance.userId` is used to get the `userId` value.
     * This ID is assigned when an identify event is made.
     *
     * This can return null if the analytics is shut down.
     *
     * Get the userId:
     * ```kotlin
     * val userId = analyticsInstance.userId
     * ```
     */
    val userId: String?
        get() {
            if (!isAnalyticsActive()) return null
            return userIdentityState.value.userId
        }

    /**
     * Get the user traits.
     *
     * The `analyticsInstance.traits` is used to get the `traits` value.
     * This traits is assigned when an identify event is made.
     *
     * This can return null if the analytics is shut down.
     *
     * Get the traits:
     * ```kotlin
     * val traits = analyticsInstance.traits
     * ```
     */
    val traits: Traits?
        get() {
            if (!isAnalyticsActive()) return null
            return userIdentityState.value.traits
        }

    private fun storeAnonymousId() {
        analyticsScope.launch(keyValueStorageDispatcher) {
            userIdentityState.value.storeAnonymousId(storage = storage)
        }
    }
}

@VisibleForTesting
internal fun provideSourceConfigManager(analytics: Analytics, sourceConfigState: State<SourceConfig>) = SourceConfigManager(
    analytics = analytics,
    sourceConfigState = sourceConfigState
)
