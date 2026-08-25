package com.rudderstack.sdk.kotlin.core.plugins

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.SDKManagedContextKey
import com.rudderstack.sdk.kotlin.core.internals.models.consent.toConsentContextBlock
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.utils.mergeWithHigherPriorityTo

/**
 * Plugin to stamp the current consent state into `context.consentManagement` on every event.
 *
 * While consent management is enabled, the complete block — provider, allowedConsentIds and
 * deniedConsentIds — is written on each event, replacing any value injected via custom context.
 * While disabled, events pass through untouched, so a legacy customContext injection keeps
 * working. `ContextGuardPlugin` re-asserts the stamp at the terminal boundary, and device-mode
 * delivery refreshes it before each destination handoff.
 */
internal class ConsentManagementPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics

    override suspend fun intercept(event: Event): Event {
        val state = analytics.consentManagementState.value
        if (!state.enabled) return event

        if (event.context.containsKey(SDKManagedContextKey.CONSENT_MANAGEMENT.key)) {
            analytics.logger.warn(
                "ConsentManagementPlugin: Replacing the \"consentManagement\" key found in the event context; " +
                    "the SDK owns this key while consent management is enabled. Migrate to setConsent()."
            )
        }

        event.context = event.context mergeWithHigherPriorityTo state.toConsentContextBlock()
        return event
    }
}
