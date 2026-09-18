package com.rudderstack.sdk.kotlin.android.plugins.devicemode.eventprocessing

import com.rudderstack.sdk.kotlin.android.models.consent.ConsentManagementState
import com.rudderstack.sdk.kotlin.android.models.consent.ConsentResolver
import com.rudderstack.sdk.kotlin.android.models.consent.toConsentManagementState
import com.rudderstack.sdk.kotlin.android.utils.consentState
import com.rudderstack.sdk.kotlin.android.utils.findDestination
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.SDKManagedContextKey
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

/**
 * A plugin to drop events for a destination unless it is consented both when the event was
 * created and at the moment of delivery.
 *
 * The two checks answer different questions, and an event must satisfy both. Live state is read
 * per event, so a revocation applies to the very next one. The value captured at creation is read
 * from the event, so a later grant cannot reach back and authorise an event recorded while this
 * destination was denied.
 *
 * The result is conservative in both directions: granting consent never delivers anything
 * retroactively, and revoking it stops delivery immediately. Evaluation fails open - without
 * consent data or destination consent configuration, every event passes through.
 */
internal class ConsentGatePlugin(private val key: String) : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics

    @Volatile
    private var destinationConfig: JsonObject? = null

    private var configJob: Job? = null

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        seedDestinationConfig()
        configJob = listenForConfigChanges()
    }

    override fun teardown() {
        configJob?.cancel()
    }

    override suspend fun intercept(event: Event): Event? {
        val allowedNow = ConsentResolver.resolve(analytics.consentState.value, destinationConfig)
        val allowedWhenCreated = capturedConsent(event)
            ?.let { ConsentResolver.resolve(it, destinationConfig) }
            ?: true

        return if (allowedNow && allowedWhenCreated) {
            event
        } else {
            analytics.logger.debug(
                "ConsentGatePlugin: Dropped event for destination: $key (messageId=${event.messageId})"
            )
            null
        }
    }

    /**
     * The consent this event was created under, or `null` when it carries none - an event created
     * while consent management was inactive, which the caller treats as consented.
     */
    private fun capturedConsent(event: Event): ConsentManagementState? =
        (event.capturedReservedContext?.get(SDKManagedContextKey.CONSENT_MANAGEMENT.key) as? JsonObject)
            ?.toConsentManagementState()

    /**
     * Reads the config already held in state, synchronously.
     *
     * The collector below delivers asynchronously, so without this the gate is blind between [setup]
     * and its first emission - and a destination registered after the source config arrived is set up
     * inside that window, where an unresolvable config fails open.
     */
    private fun seedDestinationConfig() {
        destinationConfig = findDestination(analytics.sourceConfigState.value, key)?.destinationConfig
    }

    private fun listenForConfigChanges(): Job = analytics.analyticsScope.launch {
        analytics.sourceConfigState
            .observeDispatched()
            .collect { sourceConfig ->
                destinationConfig = findDestination(sourceConfig, key)?.destinationConfig
            }
    }
}
