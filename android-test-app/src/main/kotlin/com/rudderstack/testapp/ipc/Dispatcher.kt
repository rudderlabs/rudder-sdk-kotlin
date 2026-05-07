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
 * Step 6a expands the wired surface to Screen, Identify, Group, Alias, Flush, Shutdown,
 * StartSession, and EndSession — covering every public method on `Analytics` that the §21
 * mapping calls out as in-scope for v1. Unrecognized commands still throw and are translated
 * into error broadcasts.
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
            Commands.CMD_SCREEN -> {
                val analytics = app.analytics
                    ?: error("SCREEN called before INIT; no Analytics instance")
                val name = args["name"]?.jsonPrimitive?.content
                    ?: error("SCREEN requires 'name'")
                // The SDK takes `category: String` (default empty), not nullable. Step.Screen's
                // nullable shape is the engine's choice — map null → "" at the boundary.
                val category = args["category"]?.jsonPrimitive?.content.orEmpty()
                val properties = args["properties"]?.jsonObject ?: JsonObject(emptyMap())
                analytics.screen(screenName = name, category = category, properties = properties)
            }
            Commands.CMD_IDENTIFY -> {
                val analytics = app.analytics
                    ?: error("IDENTIFY called before INIT; no Analytics instance")
                val userId = args["userId"]?.jsonPrimitive?.content
                    ?: error("IDENTIFY requires 'userId'")
                val traits = args["traits"]?.jsonObject ?: JsonObject(emptyMap())
                analytics.identify(userId = userId, traits = traits)
            }
            Commands.CMD_GROUP -> {
                val analytics = app.analytics
                    ?: error("GROUP called before INIT; no Analytics instance")
                val groupId = args["groupId"]?.jsonPrimitive?.content
                    ?: error("GROUP requires 'groupId'")
                val traits = args["traits"]?.jsonObject ?: JsonObject(emptyMap())
                analytics.group(groupId = groupId, traits = traits)
            }
            Commands.CMD_ALIAS -> {
                val analytics = app.analytics
                    ?: error("ALIAS called before INIT; no Analytics instance")
                val newId = args["newId"]?.jsonPrimitive?.content
                    ?: error("ALIAS requires 'newId'")
                // SDK's `previousId: String` defaults to empty; map null/missing → "" so the SDK
                // can resolve the previous id from current user state itself.
                val previousId = args["previousId"]?.jsonPrimitive?.content.orEmpty()
                analytics.alias(newId = newId, previousId = previousId)
            }
            Commands.CMD_FLUSH -> {
                val analytics = app.analytics
                    ?: error("FLUSH called before INIT; no Analytics instance")
                analytics.flush()
            }
            Commands.CMD_SHUTDOWN -> {
                // shutdown is idempotent on the engine side: TestApp.shutdownAnalytics already
                // tolerates a null instance. Going through it (rather than `app.analytics.shutdown()`)
                // also clears the cached instance so a follow-up INIT works cleanly.
                app.shutdownAnalytics()
            }
            Commands.CMD_START_SESSION -> {
                val analytics = app.analytics
                    ?: error("START_SESSION called before INIT; no Analytics instance")
                val sessionId = args["sessionId"]?.jsonPrimitive?.longOrNull
                analytics.startSession(sessionId = sessionId)
            }
            Commands.CMD_END_SESSION -> {
                val analytics = app.analytics
                    ?: error("END_SESSION called before INIT; no Analytics instance")
                analytics.endSession()
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
