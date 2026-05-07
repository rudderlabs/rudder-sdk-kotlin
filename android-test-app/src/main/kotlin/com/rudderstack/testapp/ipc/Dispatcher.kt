package com.rudderstack.testapp.ipc

import com.rudderstack.scenarioengine.domain.step.Step
import com.rudderstack.sdk.kotlin.android.models.reset.ResetEntries
import com.rudderstack.sdk.kotlin.android.models.reset.ResetOptions
import com.rudderstack.testapp.TestApp
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * The single SDK-touching surface in the SUT.
 *
 * Every command name maps to exactly one branch here. Adding support for a new command
 * means: (1) add its constant in [Commands], (2) add a `when` branch here, (3) update the
 * driver-side [com.rudderstack.scenarioengine.domain.helper.Sut] adapter when it lands.
 *
 * Step 5 wires [Commands.CMD_INIT], [Commands.CMD_TRACK], and [Commands.CMD_RESET] end-to-end.
 * Every other command throws; the receiver translates the throw into an error broadcast so the
 * driver sees a clear failure rather than a silent no-op.
 */
class Dispatcher(private val app: TestApp) {

    /**
     * Execute the command represented by [cmd] + [args].
     *
     * On success, broadcasts a [Commands.EVENT_TYPE_CALLBACK] correlated by [callbackId] (if any).
     * Result-bearing commands (Step 11+) write their payload via [Events.sendResult] and skip the callback.
     * Throws on any failure; the caller is expected to translate into an error broadcast.
     */
    suspend fun handle(
        cmd: String,
        args: JsonObject,
        callbackId: String?,
        @Suppress("UNUSED_PARAMETER") resultId: String?,
    ) {
        when (cmd) {
            Commands.CMD_INIT -> {
                app.initAnalytics(parseInit(args))
            }
            Commands.CMD_TRACK -> {
                val analytics = app.analytics
                    ?: error("TRACK called before INIT; no Analytics instance")
                val name = args["name"]?.jsonPrimitive?.content
                    ?: error("TRACK requires 'name'")
                val properties = args["properties"]?.jsonObject ?: JsonObject(emptyMap())
                analytics.track(name, properties)
            }
            Commands.CMD_RESET -> {
                val analytics = app.analytics
                    ?: error("RESET called before INIT; no Analytics instance")
                // All four flags map 1:1 to the Android SDK's ResetEntries fields. Each defaults
                // to true — both here and on Step.Reset — matching the SDK's full-reset semantics.
                val entries = ResetEntries(
                    anonymousId = args["anonymousId"]?.jsonPrimitive?.boolean ?: true,
                    userId = args["userId"]?.jsonPrimitive?.boolean ?: true,
                    traits = args["traits"]?.jsonPrimitive?.boolean ?: true,
                    session = args["session"]?.jsonPrimitive?.boolean ?: true,
                )
                analytics.reset(ResetOptions(entries = entries))
            }
            else -> error("not implemented yet: $cmd")
        }
        if (callbackId != null) Events.sendCallback(app, callbackId)
    }

    /**
     * Translate the JSON args of an INIT command into a [Step.Init]. Required fields throw
     * on absence; optional fields fall back to [Step.Init]'s own defaults — which are
     * deliberately tighter than the SDK's (e.g. lifecycle-tracking off, flushAt = 1) to
     * keep tests deterministic.
     */
    private fun parseInit(args: JsonObject): Step.Init {
        val mockServerUrl = args["mockServerUrl"]?.jsonPrimitive?.content
            ?: error("INIT requires 'mockServerUrl'")
        // Reject the DSL's empty-sentinel default. The driver-side runner is supposed to
        // rewrite Step.Init.mockServerUrl to the live mock server URL before dispatch; an
        // empty value here means the rewrite was skipped — fail loud rather than silently
        // sending events to "".
        require(mockServerUrl.isNotEmpty()) {
            "INIT received empty 'mockServerUrl' — runner did not inject the live mock server URL"
        }
        return Step.Init(
            writeKey = args["writeKey"]?.jsonPrimitive?.content
                ?: error("INIT requires 'writeKey'"),
            mockServerUrl = mockServerUrl,
            trackApplicationLifecycleEvents = args["trackApplicationLifecycleEvents"]?.jsonPrimitive?.boolean ?: false,
            trackDeepLinks = args["trackDeepLinks"]?.jsonPrimitive?.boolean ?: false,
            trackActivities = args["trackActivities"]?.jsonPrimitive?.boolean ?: false,
            automaticSessionTracking = args["automaticSessionTracking"]?.jsonPrimitive?.boolean ?: false,
            sessionTimeoutMs = args["sessionTimeoutMs"]?.jsonPrimitive?.longOrNull,
            flushAt = args["flushAt"]?.jsonPrimitive?.intOrNull ?: 1,
        )
    }
}
