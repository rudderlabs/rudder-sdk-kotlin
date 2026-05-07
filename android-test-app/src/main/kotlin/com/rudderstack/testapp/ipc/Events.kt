package com.rudderstack.testapp.ipc

import android.content.Context
import android.content.Intent
import com.rudderstack.scenarioengine.ipc.Commands
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Helpers for sending [Commands.ACTION_EVENT] broadcasts back to the driver.
 *
 * One static helper per event type. Payloads are JSON strings — the wire format the
 * driver-side `BroadcastTransport` (Step 4) parses. SUT-side code never sees the
 * driver's parsed event types.
 */
internal object Events {

    /** Send an ack that the command identified by [id] completed successfully. */
    fun sendCallback(context: Context, id: String) {
        broadcast(context, type = Commands.EVENT_TYPE_CALLBACK, id = id, payload = "{}")
    }

    /**
     * Send a structured error correlated to [id]. Payload shape: `{"message": ..., "type": ...}`,
     * where `type` is the exception's qualified class name when available.
     */
    fun sendError(context: Context, id: String, error: Throwable) {
        val payload = buildJsonObject {
            put("message", error.message ?: error::class.simpleName ?: "unknown error")
            put("type", error::class.qualifiedName ?: error::class.simpleName ?: "unknown")
        }.toString()
        broadcast(context, type = Commands.EVENT_TYPE_ERROR, id = id, payload = payload)
    }

    /**
     * Send a result payload for a command that requested one. [payloadJson] must be a valid
     * JSON document; this helper does not parse or validate.
     */
    fun sendResult(context: Context, id: String, payloadJson: String) {
        broadcast(context, type = Commands.EVENT_TYPE_RESULT, id = id, payload = payloadJson)
    }

    /**
     * Send an unsolicited SDK event observation (Step 7's spy plugins). [id] is null because
     * spy events are not request-correlated — they're a hot stream.
     */
    fun sendSdkEvent(context: Context, payloadJson: String) {
        broadcast(context, type = Commands.EVENT_TYPE_SDK_EVENT, id = null, payload = payloadJson)
    }

    private fun broadcast(context: Context, type: String, id: String?, payload: String) {
        val intent = Intent(Commands.ACTION_EVENT).apply {
            putExtra(Commands.EXTRA_EVENT_TYPE, type)
            id?.let { putExtra(Commands.EXTRA_EVENT_ID, it) }
            putExtra(Commands.EXTRA_EVENT_PAYLOAD, payload)
        }
        context.sendBroadcast(intent)
    }
}
