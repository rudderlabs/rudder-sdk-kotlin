package com.rudderstack.sdk.kotlin.android.plugins.devicemode

import com.rudderstack.sdk.kotlin.android.models.consent.ConsentResolver
import com.rudderstack.sdk.kotlin.android.models.consent.toConsentManagementState
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing.ConsentGatePlugin
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing.EventFilteringPlugin
import com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing.IntegrationOptionsPlugin
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ActivityLifecycleObserver
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
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import com.rudderstack.sdk.kotlin.core.internals.utils.safelyExecute
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import com.rudderstack.sdk.kotlin.android.Analytics as AndroidAnalytics

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

    private val isStandardIntegration: Boolean = this is StandardIntegration

    @Volatile
    internal var isPluginSetup = false
        private set

    // One lock for the state, the stored callbacks, and the remove, so neither can race a state change.
    private val stateLock = Any()

    @Volatile
    private var state: DestinationState = DestinationState.PendingCreate

    internal val isDestinationReady: Boolean
        get() = state == DestinationState.Ready

    // Kept for the handoff gate, so it costs no source-config lookup per event.
    @Volatile
    private var destinationConfig: JsonObject? = null

    // A plugin on this destination's chain that overwrites the key does so on every event, so the
    // replacement is reported once per destination rather than once per event.
    private val hasWarnedAboutRestoredStamp = AtomicBoolean(false)

    /**
     * The key for the destination present in the source config.
     */
    abstract val key: String

    /**
     * Creates the destination. The SDK calls this method once, with the first valid configuration.
     *
     * Initialise the destination SDK here, unless the integration already holds an instance that the app passed.
     * Throw an exception when the destination cannot be created. The SDK then calls this method again with the next
     * valid configuration.
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
        // A config queued before the app removed this integration must not create it.
        if (!isPluginSetup) return
        isDestinationConfigured(sourceConfig)?.let { safelyInitOrUpdateAndNotify(it) }
    }

    private fun isDestinationConfigured(sourceConfig: SourceConfig): JsonObject? {
        val configDestination = findDestination(sourceConfig, key)
        // Recorded for every integration, whatever the outcome below: the handoff gate needs the
        // destination's current consent rules - the same ones ConsentGatePlugin resolves by key -
        // and a rejected update still changes what those rules are.
        destinationConfig = configDestination?.destinationConfig
        if (!isStandardIntegration) {
            analytics.logger.debug("IntegrationPlugin[$key]: Non-standard integration, using empty config")
            return emptyJsonObject
        }
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
     * Applies the consent decision at the handoff boundary, then restores
     * `context.consentManagement` to the value the event was created under.
     *
     * The chain can suspend - a customer plugin added via [add] may do real work - so consent can be
     * revoked after [ConsentGatePlugin] has already passed the event. Gating here too makes that
     * guarantee hold all the way to delivery rather than only at chain entry.
     *
     * Both the live decision and the one recorded at creation are applied, exactly as
     * [ConsentGatePlugin] applies them. The gate resolves against a config cache filled by an
     * asynchronous collector, so a destination registered before the first source config arrives is
     * set up while that cache is still empty, and every event it sees fails open. This boundary
     * resolves against the config the destination was configured with, so it is the first point that
     * can judge those events at all - and without the creation-time half, a later grant would deliver
     * an event recorded while this destination was denied.
     *
     * The stamp is a separate question from the gate: it restores what the event recorded at
     * creation, so a decision taken while the event was in flight cannot rewrite what it says the
     * user had agreed to. An event carrying no captured value is left untouched.
     *
     * Because the captured value never changes, a difference here can only be a destination-chain
     * plugin having overwritten the key after the main-chain guard ran. That guard never sees such
     * a write, so this is the only place the customer can be told about it - hence a warning rather
     * than a debug line, raised once per destination so a plugin spoofing every event cannot flood
     * the log.
     */
    private fun gateAndRestoreConsentStamp(event: Event): Event? {
        val state = analytics.consentState.value
        if (!state.active) return event

        val dropReason = when {
            !ConsentResolver.resolve(state, destinationConfig) ->
                "consent was revoked while the event was in the device-mode chain"

            !allowedWhenCreated(event) ->
                "it was created while this destination was denied"

            else -> null
        }
        if (dropReason != null) {
            analytics.logger.debug(
                "IntegrationPlugin: Dropped event for destination $key - $dropReason " +
                    "(messageId=${event.messageId})."
            )
            return null
        }

        val consentKey = SDKManagedContextKey.CONSENT_MANAGEMENT.key
        val captured = event.capturedReservedContext?.get(consentKey)
        if (captured == null || event.context[consentKey] == captured) return event

        if (hasWarnedAboutRestoredStamp.compareAndSet(false, true)) {
            analytics.logger.warn(
                "IntegrationPlugin: Replacing the \"consentManagement\" key written by a plugin on " +
                    "destination $key; the SDK owns this key while consent management is enabled. " +
                    "Migrate to setConsent()."
            )
        }
        event.context = event.context mergeWithHigherPriorityTo buildJsonObject { put(consentKey, captured) }
        return event
    }

    /**
     * Whether this destination was consented under the decision the event was created with.
     *
     * An event carrying no captured value was created while consent management was inactive, which
     * counts as consented.
     */
    private fun allowedWhenCreated(event: Event): Boolean {
        val captured = event.capturedReservedContext
            ?.get(SDKManagedContextKey.CONSENT_MANAGEMENT.key) as? JsonObject
            ?: return true

        return ConsentResolver.resolve(captured.toConsentManagementState(), destinationConfig)
    }

    /**
     * Override this method to cleanup any resources before this integration plugin is removed.
     *
     * **Note**: Call `super.teardown()` when overriding this method. It resets the destination, so the SDK calls
     * [create] again when the app adds this integration again.
     */
    override fun teardown() {
        pluginList.clear()
        if (isPluginSetup) {
            pluginChain.removeAll()
        }
        resetDestination()
    }

    // The SDK also calls this on remove and shutdown, so the reset does not depend on `super.teardown()`.
    internal fun resetDestination() {
        synchronized(stateLock) {
            isPluginSetup = false
            state = DestinationState.PendingCreate
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
        synchronized(stateLock) {
            if (!isDestinationCreated()) {
                destinationReadyCallbacks.add(callback)
                return
            }
        }
        if (isDestinationReady) {
            callback(getDestinationInstance(), Result.Success(Unit))
        } else {
            callback(
                null,
                Result.Failure(IllegalStateException("Destination $key is absent or disabled in dashboard."))
            )
        }
    }

    private fun safelyInitOrUpdateAndNotify(destinationConfig: JsonObject) {
        if (!isDestinationCreated()) {
            safelyCreateAndNotify(destinationConfig)
        } else {
            safelyUpdateAndNotify(destinationConfig)
        }
    }

    // Not derived from the instance: an integration can hold one before `create` runs, or never hold one.
    private fun isDestinationCreated(): Boolean = state != DestinationState.PendingCreate

    private fun safelyCreateAndNotify(destinationConfig: JsonObject) {
        safelyExecute(
            block = {
                create(destinationConfig)
                analytics.logger.debug("IntegrationPlugin: Destination $key created successfully.")
                changeStateAndNotify(DestinationState.Ready, Result.Success(Unit))
            },
            onException = { exception ->
                analytics.logger.error("IntegrationPlugin: Failed to create destination $key. Error: ${exception.message}")
                changeStateAndNotify(DestinationState.PendingCreate, Result.Failure(exception))
            }
        )
    }

    /**
     * Marks the destination not ready and reports [throwable] to the ready callbacks.
     *
     * The destination is deliberately not updated here: pushing a config into a destination that is
     * being declared failed can throw on integrations whose config has required fields, which would
     * replace the reported reason with a parse error.
     */
    private fun notifyFailureAndMarkNotReady(throwable: Throwable) {
        changeStateAndNotify(notReadyState(), Result.Failure(throwable))
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
                changeStateAndNotify(notReadyState(), Result.Failure(throwable))
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
                changeStateAndNotify(DestinationState.Ready, Result.Success(Unit))
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
                changeStateAndNotify(notReadyState(), Result.Failure(exception))
            }
        )
    }

    // A destination that `create` never set up stays PendingCreate, so the next valid config still calls `create`.
    private fun notReadyState(): DestinationState =
        if (state == DestinationState.PendingCreate) DestinationState.PendingCreate else DestinationState.Paused

    @OptIn(InternalRudderApi::class)
    private fun changeStateAndNotify(newState: DestinationState, destinationResult: DestinationResult) {
        var hasReadinessChanged = false
        val callbacks = synchronized(stateLock) {
            // The app can remove this integration while `create` or `update` runs.
            if (!isPluginSetup) return
            hasReadinessChanged = (state == DestinationState.Ready) != (newState == DestinationState.Ready)
            state = newState
            destinationReadyCallbacks.toList().also { destinationReadyCallbacks.clear() }
        }
        // An integration gets the activity events only while it is ready.
        if (hasReadinessChanged && this is ActivityLifecycleObserver) {
            (analytics as? AndroidAnalytics)?.activityLifecycleManagementPlugin?.refreshObserver(this)
        }
        if (callbacks.isNotEmpty()) {
            analytics.logger.debug("IntegrationPlugin[$key]: Notifying ${callbacks.size} deferred callback(s)")
        }
        val destinationInstance = getDestinationInstance()
        // A failing app callback must not change the state or stop the other callbacks.
        callbacks.forEach { callback ->
            safelyExecute(
                block = { callback(destinationInstance, destinationResult) },
                onException = { exception ->
                    analytics.logger.error(
                        "IntegrationPlugin: A ready callback of destination $key failed. Error: ${exception.message}"
                    )
                }
            )
        }
    }

    private fun applyDefaultPlugins() {
        add(ConsentGatePlugin(key))
        add(EventFilteringPlugin(key))
        add(IntegrationOptionsPlugin(key))
    }

    private sealed interface DestinationState {

        /**
         * The next valid config calls `create`. The destination was never created, `create` failed, or the app removed
         * the integration.
         */
        data object PendingCreate : DestinationState

        /** The SDK forwards events to the destination. */
        data object Ready : DestinationState

        /** Created, but absent or disabled in the dashboard, denied by consent, or `update` failed. */
        data object Paused : DestinationState
    }
}
