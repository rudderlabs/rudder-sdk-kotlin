package com.rudderstack.testapp.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.rudderstack.scenarioengine.ipc.Commands
import com.rudderstack.testapp.TestApp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * The driver-facing entry point for commands.
 *
 * Receives [Commands.ACTION_COMMAND] broadcasts, parses the extras, and hands off to the
 * app's [Dispatcher] on [TestApp.scope]. Uses [goAsync] so async dispatch can complete
 * after [onReceive] returns — Android only keeps the receiver alive while a `PendingResult`
 * is outstanding.
 *
 * Any throw inside dispatch becomes a [Commands.EVENT_TYPE_ERROR] event correlated by
 * `callbackId` (preferred) or `resultId`. If neither was provided, the error is logged
 * and lost — that's the driver's choice when it sent fire-and-forget.
 */
class CommandReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Commands.ACTION_COMMAND) return

        val pending = goAsync()
        val app = context.applicationContext as TestApp

        val cmd = intent.getStringExtra(Commands.EXTRA_CMD)
        val argsJson = intent.getStringExtra(Commands.EXTRA_ARGS) ?: "{}"
        val callbackId = intent.getStringExtra(Commands.EXTRA_CALLBACK_ID)
        val resultId = intent.getStringExtra(Commands.EXTRA_RESULT_ID)

        if (cmd == null) {
            Log.w(TAG, "received COMMAND with no '${Commands.EXTRA_CMD}' extra; dropping")
            pending.finish()
            return
        }

        app.scope.launch {
            try {
                val args: JsonObject = Json.parseToJsonElement(argsJson).jsonObject
                app.dispatcher.handle(cmd, args, callbackId, resultId)
            } catch (t: Throwable) {
                Log.e(TAG, "command $cmd failed", t)
                val correlationId = callbackId ?: resultId
                if (correlationId != null) Events.sendError(app, correlationId, t)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "CommandReceiver"
    }
}
