package com.rudderstack.sdk.kotlin.android.plugins

import com.rudderstack.sdk.kotlin.android.models.consent.toConsentContextBlock
import com.rudderstack.sdk.kotlin.android.utils.consentState
import com.rudderstack.sdk.kotlin.android.utils.mergeWithHigherPriorityTo
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.SDKManagedContextKey
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Plugin to stamp the current consent state into `context.consentManagement` on every event.
 *
 * While consent management is enabled, the complete block — provider, allowedConsentIds and
 * deniedConsentIds — is written on each event, replacing any value injected via custom context.
 * While disabled, events pass through untouched, so a legacy customContext injection keeps
 * working. The stamp reflects the state at event creation; events already in the pipeline are
 * not restamped.
 *
 * A customer still injecting the key does so on every event, so the replacement is warned about
 * once per analytics instance rather than once per event.
 */
internal class ConsentManagementPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics

    // Events are intercepted on a multi-threaded dispatcher, so the first-warning check has to be
    // atomic - a plain flag would let two concurrent events both warn.
    private val hasWarnedAboutInjectedKey = AtomicBoolean(false)

    override suspend fun intercept(event: Event): Event {
        val state = analytics.consentState.value
        if (!state.active) return event

        if (event.context.containsKey(SDKManagedContextKey.CONSENT_MANAGEMENT.key) &&
            hasWarnedAboutInjectedKey.compareAndSet(false, true)
        ) {
            analytics.logger.warn(
                "ConsentManagementPlugin: Replacing the \"consentManagement\" key found in the event context; " +
                    "the SDK owns this key while consent management is enabled. Migrate to setConsent()."
            )
        }

        event.context = event.context mergeWithHigherPriorityTo state.toConsentContextBlock()
        return event
    }
}
