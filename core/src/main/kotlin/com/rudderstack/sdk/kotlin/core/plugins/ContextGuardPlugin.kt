package com.rudderstack.sdk.kotlin.core.plugins

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.SDKManagedContextKey
import com.rudderstack.sdk.kotlin.core.internals.models.consent.toConsentContextBlock
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.utils.mergeWithHigherPriorityTo

/**
 * A terminal plugin that re-asserts SDK-owned context keys after all customer plugins have run.
 *
 * Registered first in the terminal phase, so its re-stamped event flows into both delivery
 * paths — cloud-mode storage plus the device-mode fan-out queue.
 */
internal class ContextGuardPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Terminal

    override lateinit var analytics: Analytics

    override suspend fun intercept(event: Event): Event {
        enforceConsentStamp(event)
        return event
    }

    /**
     * Re-asserts `context.consentManagement` from the current consent state.
     *
     * Active only while consent management is enabled — while disabled the key is not
     * reserved and the event passes through untouched.
     */
    private fun enforceConsentStamp(event: Event) {
        val state = analytics.consentManagementState.value
        if (!state.enabled) return

        val stamp = state.toConsentContextBlock()
        val consentKey = SDKManagedContextKey.CONSENT_MANAGEMENT.key
        if (event.context[consentKey] == stamp[consentKey]) return

        analytics.logger.warn(
            "ContextGuardPlugin: Replacing the \"consentManagement\" key found in the event context; " +
                "the SDK owns this key while consent management is enabled. Migrate to setConsent()."
        )
        event.context = event.context mergeWithHigherPriorityTo stamp
    }
}
