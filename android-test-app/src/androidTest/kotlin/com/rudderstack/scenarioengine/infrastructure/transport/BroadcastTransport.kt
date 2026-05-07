package com.rudderstack.scenarioengine.infrastructure.transport

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.core.content.ContextCompat
import com.rudderstack.scenarioengine.domain.transport.Transport
import com.rudderstack.scenarioengine.domain.transport.TransportAck
import com.rudderstack.scenarioengine.domain.transport.TransportEvent
import com.rudderstack.scenarioengine.ipc.Commands
import com.rudderstack.testapp.ipc.StateProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Android implementation of the [Transport] domain interface.
 *
 * Driver → SUT goes via cross-package broadcast targeting [Commands.ACTION_COMMAND] with the
 * SUT's [com.rudderstack.testapp.ipc.CommandReceiver] as the explicit component. SUT → driver
 * uses [Commands.ACTION_EVENT] broadcasts received by a dynamic [BroadcastReceiver] this class
 * registers in [Context]; correlation is by `callbackId` / `resultId` extras.
 *
 * State reads are synchronous [Context.getContentResolver] queries against
 * [StateProvider.AUTHORITY] — they don't go through the broadcast path because the doc's
 * §8.3 contract requires them to survive the same `am broadcast` ordering issues that drove
 * the ContentProvider design.
 *
 * **Doc deviation.** §8.4 specifies `UiDevice.executeShellCommand("am broadcast …")` for the
 * command channel. We use [Context.sendBroadcast] with [Intent.FLAG_RECEIVER_FOREGROUND]
 * because the two-APK split (§11) gave the driver its own [Context] capable of cross-package
 * delivery — proven in Step 3's `SplitSmokeTest`. If a future scenario surfaces a delivery
 * gap (e.g. broadcasts to a SUT in `cached` state), revisit and consider falling back to the
 * shell route.
 *
 * Not thread-safe across [close]; once closed, all operations are undefined.
 *
 * @param context the **driver** context, used both for `sendBroadcast` and `ContentResolver`.
 * @param defaultAckTimeoutMs how long [sendCommand] waits for a callback before reporting timeout.
 */
class BroadcastTransport(
    private val context: Context,
    private val defaultAckTimeoutMs: Long = 5_000L,
    private val sutPackage: String = SUT_PACKAGE,
    private val commandReceiverClass: String = COMMAND_RECEIVER_CLASS,
    private val stateAuthority: String = StateProvider.AUTHORITY,
) : Transport {

    private val pending = ConcurrentHashMap<String, CompletableDeferred<TransportEvent>>()
    private val sdkEvents = MutableSharedFlow<TransportEvent>(replay = 0, extraBufferCapacity = SDK_EVENT_BUFFER)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action != Commands.ACTION_EVENT) return
            val type = intent.getStringExtra(Commands.EXTRA_EVENT_TYPE) ?: return
            val id = intent.getStringExtra(Commands.EXTRA_EVENT_ID)
            val payload = intent.getStringExtra(Commands.EXTRA_EVENT_PAYLOAD) ?: "{}"
            val event = TransportEvent(type = type, id = id, payload = payload)
            when (type) {
                Commands.EVENT_TYPE_SDK_EVENT -> sdkEvents.tryEmit(event)
                else -> id?.let { pending.remove(it)?.complete(event) }
            }
        }
    }

    init {
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Commands.ACTION_EVENT),
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    override suspend fun sendCommand(command: String, args: Map<String, Any?>): TransportAck {
        val callbackId = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<TransportEvent>()
        pending[callbackId] = deferred
        return try {
            broadcast(command, args, callbackId = callbackId, resultId = null)
            val event = withTimeout(defaultAckTimeoutMs) { deferred.await() }
            if (event.type == Commands.EVENT_TYPE_ERROR) {
                TransportAck(ok = false, error = parseErrorMessage(event.payload))
            } else {
                TransportAck(ok = true)
            }
        } catch (e: TimeoutCancellationException) {
            pending.remove(callbackId)
            TransportAck(ok = false, error = "timeout after ${defaultAckTimeoutMs}ms waiting for callback")
        }
    }

    override suspend fun sendCommandWithResult(
        command: String,
        args: Map<String, Any?>,
        timeoutMs: Long,
    ): String {
        val resultId = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<TransportEvent>()
        pending[resultId] = deferred
        return try {
            broadcast(command, args, callbackId = null, resultId = resultId)
            val event = withTimeout(timeoutMs) { deferred.await() }
            if (event.type == Commands.EVENT_TYPE_ERROR) {
                error("SUT returned error for $command: ${parseErrorMessage(event.payload)}")
            }
            event.payload
        } catch (e: TimeoutCancellationException) {
            pending.remove(resultId)
            throw IllegalStateException(
                "$command did not produce a result within ${timeoutMs}ms",
                e,
            )
        }
    }

    override fun observeEvents(): Flow<TransportEvent> = sdkEvents.asSharedFlow()

    override suspend fun readState(field: String): String? = withContext(Dispatchers.IO) {
        val uri = Uri.parse("content://$stateAuthority/$field")
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    /** Tear down the receiver registration. After this call, the transport is unusable. */
    fun close() {
        runCatching { context.unregisterReceiver(receiver) }
    }

    private fun broadcast(
        command: String,
        args: Map<String, Any?>,
        callbackId: String?,
        resultId: String?,
    ) {
        val intent = Intent(Commands.ACTION_COMMAND).apply {
            component = ComponentName(sutPackage, commandReceiverClass)
            putExtra(Commands.EXTRA_CMD, command)
            putExtra(Commands.EXTRA_ARGS, mapToCompactJson(args))
            callbackId?.let { putExtra(Commands.EXTRA_CALLBACK_ID, it) }
            resultId?.let { putExtra(Commands.EXTRA_RESULT_ID, it) }
            // Required on Android 8+ for delivery to a backgrounded SUT.
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        context.sendBroadcast(intent)
    }

    private fun mapToCompactJson(args: Map<String, Any?>): String {
        val element = JsonObject(args.mapValues { (_, v) -> anyToJsonElement(v) })
        return Json.encodeToString(JsonObject.serializer(), element)
    }

    private fun anyToJsonElement(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is JsonElement -> value
        is String -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is Map<*, *> -> JsonObject(
            value.entries.associate { (k, v) -> k.toString() to anyToJsonElement(v) },
        )
        is Iterable<*> -> JsonArray(value.map { anyToJsonElement(it) })
        else -> error("unsupported argument type for transport: ${value::class.qualifiedName}")
    }

    private fun parseErrorMessage(payloadJson: String): String =
        runCatching {
            Json.parseToJsonElement(payloadJson).jsonObject["message"]?.jsonPrimitive?.content
        }.getOrNull() ?: payloadJson

    private companion object {
        const val SUT_PACKAGE = "com.rudderstack.testapp"
        const val COMMAND_RECEIVER_CLASS = "com.rudderstack.testapp.ipc.CommandReceiver"
        const val SDK_EVENT_BUFFER = 64
    }
}
