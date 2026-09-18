package com.rudderstack.sdk.kotlin.android.plugins

import com.rudderstack.sdk.kotlin.android.utils.mergeWithHigherPriorityTo
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.SDKManagedContextKey
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Plugin to stamp `context.consentManagement` with the consent the event was created under.
 *
 * The value is taken from the event itself, not from live state: consent is judged at capture
 * time, so a decision taken while this event is in flight belongs to later events and must not
 * rewrite this one. An event created while consent management was inactive carries no value and
 * passes through untouched, so a legacy customContext injection keeps working.
 *
 * `SchemaGuardPlugin` re-asserts the same captured value at the terminal boundary, and device-mode
 * delivery refreshes it before each destination handoff — all three read one source of truth.
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
        val consentKey = SDKManagedContextKey.CONSENT_MANAGEMENT.key
        val captured = event.capturedReservedContext?.get(consentKey) ?: return event

        if (event.context.containsKey(consentKey) && hasWarnedAboutInjectedKey.compareAndSet(false, true)) {
            analytics.logger.warn(
                "ConsentManagementPlugin: Replacing the \"consentManagement\" key found in the event context; " +
                    "the SDK owns this key while consent management is enabled. Migrate to setConsent()."
            )
        }

        event.context = event.context mergeWithHigherPriorityTo buildJsonObject { put(consentKey, captured) }
        return event
    }
}
