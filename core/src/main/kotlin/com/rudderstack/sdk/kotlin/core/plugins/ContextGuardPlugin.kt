package com.rudderstack.sdk.kotlin.core.plugins

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.SDKManagedContextKey
import com.rudderstack.sdk.kotlin.core.internals.models.consent.toConsentContextBlock
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
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
        warnOnBaseKeyOverrides(event)
        enforceConsentStamp(event)
        return event
    }

    /**
     * Logs a value-free deprecation warning for each SDK-stamped base key carrying a
     * customer-supplied value — injected via `RudderOption.customContext` or written by a
     * customer plugin (detected against the snapshot). Detection only: the event is never
     * modified, so existing overrides keep working unchanged.
     */
    private fun warnOnBaseKeyOverrides(event: Event) {
        val overriddenKeys = linkedSetOf<String>()
        overriddenKeys += customContextOverrides(event)
        overriddenKeys += snapshotOverrides(event)

        overriddenKeys.forEach { overriddenKey ->
            analytics.logger.warn(
                "ContextGuardPlugin: Detected a custom value for the SDK-managed context key \"$overriddenKey\"; " +
                    "overriding SDK-managed context keys is deprecated and will be unsupported in a future major version."
            )
        }
    }

    /** The base keys this platform actually stamps; warning about any other key would be noise. */
    private val managedBaseKeys: List<String>
        get() = when (analytics.getPlatformType()) {
            PlatformType.Mobile -> SDKManagedContextKey.baseKeys
            PlatformType.Server -> SDKManagedContextKey.coreBaseKeys
        }.map { it.key }

    private fun customContextOverrides(event: Event): List<String> = managedBaseKeys
        .filter { event.options.customContext.containsKey(it) }

    private fun snapshotOverrides(event: Event): List<String> {
        val snapshot = analytics.contextSnapshotPlugin.consumeSnapshot(event.messageId) ?: return emptyList()
        return managedBaseKeys
            .filter { key ->
                val stampedValue = snapshot[key]
                stampedValue != null && event.context[key] != stampedValue
            }
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
