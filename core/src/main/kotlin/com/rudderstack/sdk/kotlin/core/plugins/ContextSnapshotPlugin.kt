package com.rudderstack.sdk.kotlin.core.plugins

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.SDKManagedContextKey
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import kotlinx.serialization.json.JsonElement

/**
 * Records the SDK-stamped base context values for the event currently in the pipeline.
 *
 * Registered by platform modules after all SDK context stampers, so the recorded values are
 * the SDK's own; any later difference at the terminal boundary is a customer override. The
 * main pipeline processes one event at a time, so a single slot suffices.
 */
@InternalRudderApi
class ContextSnapshotPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: Analytics

    @Volatile
    private var snapshot: Snapshot? = null

    override suspend fun intercept(event: Event): Event {
        snapshot = Snapshot(
            messageId = event.messageId,
            baseKeyValues = SDKManagedContextKey.baseKeys
                .mapNotNull { managedKey -> event.context[managedKey.key]?.let { managedKey.key to it } }
                .toMap()
        )
        return event
    }

    /**
     * Returns the recorded base-key values for [messageId], clearing the slot. Returns `null`
     * on a messageId mismatch — a stale snapshot must never produce a false warning.
     */
    fun consumeSnapshot(messageId: String): Map<String, JsonElement>? {
        val current = snapshot
        snapshot = null
        return current?.takeIf { it.messageId == messageId }?.baseKeyValues
    }

    private data class Snapshot(
        val messageId: String,
        val baseKeyValues: Map<String, JsonElement>,
    )
}
