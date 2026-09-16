package com.rudderstack.sdk.kotlin.core.plugins

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.SDKManagedContextKey
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.utils.mergeWithHigherPriorityTo
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * A terminal plugin that re-asserts SDK-owned context keys after all customer plugins have run.
 *
 * Registered first in the terminal phase, so its re-stamped event flows into both delivery
 * paths — cloud-mode storage plus the device-mode fan-out queue.
 */
internal class SchemaGuardPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Terminal

    override lateinit var analytics: Analytics

    override suspend fun intercept(event: Event): Event {
        warnOnBaseKeyOverrides(event)
        enforceReservedKeys(event)
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
                "SchemaGuardPlugin: Detected a custom value for the SDK-managed context key \"$overriddenKey\"; " +
                    "overriding SDK-managed context keys is deprecated and will be unsupported in a future major version."
            )
        }
    }

    /**
     * The base keys this platform actually stamps; warning about any other key would be noise.
     *
     * Resolved once: an instance's platform type never changes, and this is read on the terminal
     * path of every event.
     */
    private val managedBaseKeys: List<String> by lazy {
        when (analytics.getPlatformType()) {
            PlatformType.Mobile -> SDKManagedContextKey.baseKeys
            PlatformType.Server -> SDKManagedContextKey.coreBaseKeys
        }.map { it.key }
    }

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
     * Re-asserts every reserved context key from the value the SDK asserted when the event was
     * created.
     *
     * A key with no registered supplier, or one the SDK asserted no value for at creation, is not
     * reserved for this event and passes through untouched.
     *
     * The value restored is the one captured at creation, not the state at this instant, so a
     * decision taken while the event was in flight cannot rewrite what the event recorded. Any
     * remaining difference is therefore a customer override, which is what the warning reports.
     */
    private fun enforceReservedKeys(event: Event) {
        SDKManagedContextKey.reservedKeys.forEach { managedKey ->
            val reserved = analytics.reservedContextValues[managedKey] ?: return@forEach
            val captured = event.capturedReservedContext?.get(managedKey.key) ?: return@forEach
            if (event.context[managedKey.key] == captured) return@forEach

            analytics.logger.warn(
                "SchemaGuardPlugin: Replacing the \"${managedKey.key}\" key found in the event context; " +
                    reserved.overrideAdvice
            )
            event.context = event.context mergeWithHigherPriorityTo buildJsonObject {
                put(managedKey.key, captured)
            }
        }
    }
}
