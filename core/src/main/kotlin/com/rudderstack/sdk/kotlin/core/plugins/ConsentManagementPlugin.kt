package com.rudderstack.sdk.kotlin.core.plugins

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.consent.ConsentManagementState
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.utils.mergeWithHigherPriorityTo
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val CONSENT_MANAGEMENT_KEY = "consentManagement"
private const val PROVIDER_KEY = "provider"
private const val ALLOWED_CONSENT_IDS_KEY = "allowedConsentIds"
private const val DENIED_CONSENT_IDS_KEY = "deniedConsentIds"

/**
 * Plugin to stamp the current consent state into `context.consentManagement` on every event.
 *
 * While consent management is enabled, the complete block — provider, allowedConsentIds and
 * deniedConsentIds — is written on each event, replacing any value injected via custom context.
 * While disabled, events pass through untouched, so a legacy customContext injection keeps
 * working. The stamp reflects the state at event creation; events already in the pipeline are
 * not restamped.
 */
internal class ConsentManagementPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics

    override suspend fun intercept(event: Event): Event {
        val state = analytics.consentManagementState.value
        if (!state.enabled) return event

        if (event.context.containsKey(CONSENT_MANAGEMENT_KEY)) {
            analytics.logger.warn(
                "ConsentManagementPlugin: Replacing the \"consentManagement\" key found in the event context; " +
                    "the SDK owns this key while consent management is enabled. Migrate to setConsent()."
            )
        }

        event.context = event.context mergeWithHigherPriorityTo buildConsentBlock(state)
        return event
    }

    private fun buildConsentBlock(state: ConsentManagementState): JsonObject = buildJsonObject {
        put(
            CONSENT_MANAGEMENT_KEY,
            buildJsonObject {
                put(PROVIDER_KEY, state.provider.value)
                put(ALLOWED_CONSENT_IDS_KEY, buildJsonArray { state.allowedConsentIds.forEach { add(it) } })
                put(DENIED_CONSENT_IDS_KEY, buildJsonArray { state.deniedConsentIds.forEach { add(it) } })
            }
        )
    }
}
