package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.android.models.consent.ConsentResolver
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing.ConsentGatePlugin
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing.EventFilteringPlugin
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing.IntegrationOptionsPlugin
import com.rudderstack.sdk.kotlin.android.utils.consentState
import com.rudderstack.sdk.kotlin.android.utils.findDestination
import com.rudderstack.sdk.kotlin.android.utils.mergeWithHigherPriorityTo
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.SDKManagedContextKey
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.plugins.EventPlugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.plugins.PluginChain
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import com.rudderstack.sdk.kotlin.core.internals.utils.safelyExecute
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.CopyOnWriteArrayList

private const val DELIVERY_HALTED_NOTICE = "No events will be sent to this destination."

/**
 * Base plugin class for all integration plugins.
 *
 * An integration plugin is a plugin that is responsible for sending events directly
 * to a 3rd party destination without sending it to Rudder server first.
 */
@Suppress("TooManyFunctions")
abstract class IntegrationPlugin : EventPlugin {

    final override val pluginType: Plugin.PluginType = Plugin.PluginType.Terminal

    final override lateinit var analytics: Analytics

    private lateinit var pluginChain: PluginChain
    private val pluginList = CopyOnWriteArrayList<Plugin>()
    private val destinationReadyCallbacks = mutableListOf<(Any?, DestinationResult) -> Unit>()

    private var isStandardIntegration: Boolean = true

    @Volatile
    private var isPluginSetup = false

    @Volatile
    internal var isDestinationReady = false
        private set

    // Kept for the handoff gate, so it costs no source-config lookup per event.
    @Volatile
    private var destinationConfig: JsonObject? = null

    /**
     * The key for the destination present in the source config.
     */
    abstract val key: String

    /**
     * Creates the destination instance. Override this method for the initialisation of destination.
     * This method must return true if the destination was created successfully, false otherwise.
     *
     * @param destinationConfig The configuration for the destination.
     */
    protected abstract fun create(destinationConfig: JsonObject)

    /**
     * This method will be called when the destination configuration is updated.
     * The value could be either destination config or empty json object.
     *
     * @param destinationConfig The updated configuration for the destination.
     */
    open fun update(destinationConfig: JsonObject) {}

    /**
     * Returns the instance of the destination which was created.
     *
     * @return The instance of the destination.
     */
    abstract fun getDestinationInstance(): Any?

    /**
     * Override this method to control the behaviour of [Analytics.flush] for this destination.
     */
    open fun flush() {}

    /**
     * Override this method to control the behaviour of [Analytics.reset] for this destination.
     */
    open fun reset() {}

    final override fun setup(analytics: Analytics) {
        super.setup(analytics)
        isStandardIntegration = this is StandardIntegration
        pluginChain = PluginChain().also { it.analytics = analytics }
        isPluginSetup = true
        pluginList.forEach { plugin -> add(plugin) }
        pluginList.clear()
        applyDefaultPlugins()
    }

    // todo: refactor this API to ensure that callbacks are invoked
    //  only once and destination is initialised only once even when this method is called multiple times.
    //  There should be no side effect of calling this method multiple times with same SourceConfig.
    internal fun initDestination(sourceConfig: SourceConfig) {
        isDestinationConfigured(sourceConfig)?.let { safelyInitOrUpdateAndNotify(it) }
    }

    private fun isDestinationConfigured(sourceConfig: SourceConfig): JsonObject? {
        if (!isStandardIntegration) {
            analytics.logger.debug("IntegrationPlugin[$key]: Non-standard integration, using empty config")
            return emptyJsonObject
        }
        val configDestination = findDestination(sourceConfig, key)
        // Recorded whatever the outcome below: the handoff gate needs the destination's current
        // consent rules, and a rejected update still changes what those rules are.
        destinationConfig = configDestination?.destinationConfig
        return when {
            configDestination == null -> {
                notifyDashboardFailure("Destination $key not found in the source config. $DELIVERY_HALTED_NOTICE")
                null
            }
            !configDestination.isDestinationEnabled -> {
                notifyDashboardFailure("Destination $key is disabled in dashboard. $DELIVERY_HALTED_NOTICE")
                null
            }
            !ConsentResolver.resolve(analytics.consentState.value, configDestination.destinationConfig) -> {
                notifyConsentDenial("Destination $key is denied by user consent. $DELIVERY_HALTED_NOTICE")
                null
            }
            else -> configDestination.destinationConfig
        }
    }

    // A destination missing from, or disabled in, the dashboard keeps its long-standing handling: it is
    // updated with an empty config before the failure is reported.
    private fun notifyDashboardFailure(errorMessage: String) {
        analytics.logger.warn("IntegrationPlugin: $errorMessage")
        safelyUpdateOnFailureAndNotify(IllegalStateException(errorMessage))
    }

    // A destination denied by consent must not be updated: pushing an empty config can throw, which would
    // replace the consent reason with a parse error, and can reset a live destination's state.
    private fun notifyConsentDenial(errorMessage: String) {
        analytics.logger.warn("IntegrationPlugin: $errorMessage")
        notifyFailureAndMarkNotReady(ConsentDeniedException(errorMessage))
    }

    final override suspend fun intercept(event: Event): Event {
        if (isDestinationReady) {
            event.copy<Event>()
                .let { pluginChain.applyPlugins(Plugin.PluginType.PreProcess, it) }
                ?.let { pluginChain.applyPlugins(Plugin.PluginType.OnProcess, it) }
                ?.let { gateAndRestoreConsentStamp(it) }
                ?.let { handleEvent(it) }
        }

        return event
    }

    /**
     * Applies the live consent decision at the handoff boundary, then restores
     * `context.consentManagement` to the value the event was created under.
     *
     * The chain can suspend - a customer plugin added via [add] may do real work - so consent can be
     * revoked after [ConsentGatePlugin] has already passed the event. Gating here too makes that
     * guarantee hold all the way to delivery rather than only at chain entry.
     *
     * The two halves read deliberately different sources. The gate reads live state, because a
     * revocation must stop delivery now. The stamp does not: it restores what the event recorded at
     * creation, so a decision taken while the event was in flight cannot rewrite what it says the
     * user had agreed to. An event carrying no captured value is left untouched.
     *
     * Because the captured value never changes, a difference here can only be a destination-chain
     * plugin having overwritten the key after the main-chain guard ran.
     */
    private fun gateAndRestoreConsentStamp(event: Event): Event? {
        val state = analytics.consentState.value
        if (!state.active) return event

        if (!ConsentResolver.resolve(state, destinationConfig)) {
            analytics.logger.debug(
                "IntegrationPlugin: Dropped event for destination $key - consent was revoked while the " +
                    "event was in the device-mode chain (messageId=${event.messageId})."
            )
            return null
        }

        val consentKey = SDKManagedContextKey.CONSENT_MANAGEMENT.key
        val captured = event.capturedReservedContext?.get(consentKey)
        if (captured == null || event.context[consentKey] == captured) return event

        analytics.logger.debug(
            "IntegrationPlugin: Restored the consent stamp before delivery to destination $key."
        )
        event.context = event.context mergeWithHigherPriorityTo buildJsonObject { put(consentKey, captured) }
        return event
    }

    /**
     * Override this method to cleanup any resources before this integration plugin is removed.
     *
     * **Note**: Calling of `super.teardown()` is recommended when overriding this method.
     */
    override fun teardown() {
        pluginList.clear()
        if (isPluginSetup) {
            pluginChain.removeAll()
        }
    }

    /**
     * This method adds a plugin to modify the events before sending to this destination.
     *
     * @param plugin The plugin to be added.
     */
    fun add(plugin: Plugin) {
        if (isPluginSetup) {
            analytics.logger.debug("IntegrationPlugin[$key]: Added plugin ${plugin::class.simpleName}")
            pluginChain.add(plugin)
        } else {
            pluginList.add(plugin)
        }
    }

    /**
     * This method removes a plugin from the destination.
     *
     * @param plugin The plugin to be removed.
     */
    fun remove(plugin: Plugin) {
        pluginList.remove(plugin)
        if (isPluginSetup) {
            analytics.logger.debug("IntegrationPlugin[$key]: Removed plugin ${plugin::class.simpleName}")
            pluginChain.remove(plugin)
        }
    }

    /**
     * Registers a callback to be invoked when the destination of this plugin is ready.
     *
     * @param callback The callback to be invoked when the destination is ready.
     */
    // todo: refactor this API to support dynamic callbacks
    fun onDestinationReady(callback: (Any?, DestinationResult) -> Unit) {
        getDestinationInstance()?.let { destinationInstance ->
            if (isDestinationReady) {
                callback(destinationInstance, Result.Success(Unit))
            } else {
                callback(
                    null,
                    Result.Failure(IllegalStateException("Destination $key is absent or disabled in dashboard."))
                )
            }
        } ?: run {
            synchronized(this) {
                destinationReadyCallbacks.add(callback)
            }
        }
    }

    private fun safelyInitOrUpdateAndNotify(destinationConfig: JsonObject) {
        if (getDestinationInstance() == null) {
            safelyCreateAndNotify(destinationConfig)
        } else {
            safelyUpdateAndNotify(destinationConfig)
        }
    }

    private fun safelyCreateAndNotify(destinationConfig: JsonObject) {
        safelyExecute(
            block = {
                create(destinationConfig)
                analytics.logger.debug("IntegrationPlugin: Destination $key created successfully.")
                this.isDestinationReady = true
                notifyCallbacks(Result.Success(Unit))
            },
            onException = { exception ->
                analytics.logger.error("IntegrationPlugin: Failed to create destination $key. Error: ${exception.message}")
                this.isDestinationReady = false
                notifyCallbacks(Result.Failure(exception))
            }
        )
    }

    /**
     * Marks the destination not ready and reports [throwable] to the ready callbacks.
     *
     * The destination is deliberately not updated here: pushing a config into a destination that is
     * being declared failed can throw on integrations whose config has required fields, which would
     * replace the reported reason with a parse error. Notification stays wrapped so a throwing
     * customer callback cannot escape into the re-evaluation coroutine.
     */
    private fun notifyFailureAndMarkNotReady(throwable: Throwable) {
        this.isDestinationReady = false
        safelyExecute(
            block = { notifyCallbacks(Result.Failure(throwable)) },
            onException = { exception ->
                analytics.logger.error(
                    "IntegrationPlugin: Failed to notify destination $key callbacks. Error: ${exception.message}"
                )
            }
        )
    }

    /**
     * Updates the destination with an empty config, then marks it not ready and reports [throwable].
     *
     * The update is what tells a destination the SDK owns no config for it any more, so a destination
     * withheld by the dashboard can clear itself.
     */
    private fun safelyUpdateOnFailureAndNotify(throwable: Throwable) {
        safelyUpdateAndApplyBlock(
            destinationConfig = emptyJsonObject,
            block = {
                analytics.logger.debug("IntegrationPlugin: Destination $key updated with empty destinationConfig.")
                this.isDestinationReady = false
                notifyCallbacks(Result.Failure(throwable))
            }
        )
    }

    private fun safelyUpdateAndNotify(destinationConfig: JsonObject) {
        safelyUpdateAndApplyBlock(
            destinationConfig = destinationConfig,
            block = {
                analytics.logger.debug(
                    "IntegrationPlugin: Destination $key updated with destinationConfig: $destinationConfig."
                )
                this.isDestinationReady = true
                notifyCallbacks(Result.Success(Unit))
            }
        )
    }

    private fun safelyUpdateAndApplyBlock(destinationConfig: JsonObject, block: () -> Unit) {
        safelyExecute(
            block = {
                if (isStandardIntegration) {
                    update(destinationConfig)
                    block()
                }
            },
            onException = { exception ->
                analytics.logger.error("IntegrationPlugin: Failed to update destination $key. Error: ${exception.message}")
                this.isDestinationReady = false
                notifyCallbacks(Result.Failure(exception))
            }
        )
    }

    private fun notifyCallbacks(destinationResult: DestinationResult) {
        synchronized(this) {
            if (destinationReadyCallbacks.isNotEmpty()) {
                analytics.logger.debug(
                    "IntegrationPlugin[$key]: Notifying ${destinationReadyCallbacks.size} deferred callback(s)"
                )
            }
            destinationReadyCallbacks.forEach { callback -> callback(getDestinationInstance(), destinationResult) }
            destinationReadyCallbacks.clear()
        }
    }

    private fun applyDefaultPlugins() {
        add(ConsentGatePlugin(key))
        add(EventFilteringPlugin(key))
        add(IntegrationOptionsPlugin(key))
    }
}
