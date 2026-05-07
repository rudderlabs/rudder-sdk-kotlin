package com.rudderstack.scenarioengine.infrastructure.sut

import com.rudderstack.scenarioengine.domain.helper.Sut
import com.rudderstack.scenarioengine.domain.step.Step
import com.rudderstack.scenarioengine.domain.transport.Transport
import com.rudderstack.testapp.ipc.Commands

/**
 * Driver-side adapter that turns [Sut] calls into [Transport] command broadcasts.
 *
 * One method, one command, one ack. The transport's `sendCommand` already handles
 * `Map<String, Any?>` → JSON conversion (including `JsonElement` passthrough), so the
 * builder maps below pass `Step` fields straight through without re-encoding.
 *
 * **Step 6a scope.** Implements every SDK-touching helper method except spy plugins (Step 7)
 * and state export/import (Step 11). Adapter and interpreter dispatch stay in lockstep — both
 * surfaces TODO the same Step types.
 *
 * Stateless aside from its [transport] reference. Lifetime is the test's lifetime —
 * caller closes the [transport] in `@After`.
 */
class BroadcastSut(private val transport: Transport) : Sut {

    override suspend fun init(step: Step.Init) {
        val args = mutableMapOf<String, Any?>(
            "writeKey" to step.writeKey,
            "mockServerUrl" to step.mockServerUrl,
            "trackApplicationLifecycleEvents" to step.trackApplicationLifecycleEvents,
            "trackDeepLinks" to step.trackDeepLinks,
            "trackActivities" to step.trackActivities,
            "automaticSessionTracking" to step.automaticSessionTracking,
            "flushAt" to step.flushAt,
        )
        // sessionTimeoutMs is nullable on the SDK's SessionConfiguration too; only forward
        // when set so the SUT-side parser falls back to the SDK's own default.
        step.sessionTimeoutMs?.let { args["sessionTimeoutMs"] = it }
        sendOrFail(Commands.CMD_INIT, args)
    }

    override suspend fun track(step: Step.Track) {
        sendOrFail(
            Commands.CMD_TRACK,
            mapOf(
                "name" to step.name,
                // properties is a JsonObject — Transport.anyToJsonElement passes JsonElement
                // values through unchanged, so no re-encoding is needed.
                "properties" to step.properties,
            ),
        )
    }

    override suspend fun reset(step: Step.Reset) {
        sendOrFail(
            Commands.CMD_RESET,
            mapOf(
                "anonymousId" to step.anonymousId,
                "userId" to step.userId,
                "traits" to step.traits,
                "session" to step.session,
            ),
        )
    }

    override suspend fun screen(step: Step.Screen) {
        val args = mutableMapOf<String, Any?>(
            "name" to step.name,
            "properties" to step.properties,
        )
        // Only forward category when set so the SUT-side default (empty string, matching the
        // SDK) applies — keeps the wire payload tight and the SDK-default behavior unsurprising.
        step.category?.let { args["category"] = it }
        sendOrFail(Commands.CMD_SCREEN, args)
    }

    override suspend fun identify(step: Step.Identify) {
        sendOrFail(
            Commands.CMD_IDENTIFY,
            mapOf(
                "userId" to step.userId,
                "traits" to step.traits,
            ),
        )
    }

    override suspend fun group(step: Step.Group) {
        sendOrFail(
            Commands.CMD_GROUP,
            mapOf(
                "groupId" to step.groupId,
                "traits" to step.traits,
            ),
        )
    }

    override suspend fun alias(step: Step.Alias) {
        val args = mutableMapOf<String, Any?>("newId" to step.newId)
        step.previousId?.let { args["previousId"] = it }
        sendOrFail(Commands.CMD_ALIAS, args)
    }

    override suspend fun flush() {
        sendOrFail(Commands.CMD_FLUSH, emptyMap())
    }

    override suspend fun shutdown() {
        sendOrFail(Commands.CMD_SHUTDOWN, emptyMap())
    }

    override suspend fun startSession(step: Step.StartSession) {
        val args = mutableMapOf<String, Any?>()
        step.sessionId?.let { args["sessionId"] = it }
        sendOrFail(Commands.CMD_START_SESSION, args)
    }

    override suspend fun endSession() {
        sendOrFail(Commands.CMD_END_SESSION, emptyMap())
    }

    override suspend fun addSpyPlugin(tag: String): Unit = TODO("wired in build step 7")

    override suspend fun removeSpyPlugin(tag: String): Unit = TODO("wired in build step 7")

    override suspend fun exportState(): ByteArray = TODO("wired in build step 11")

    override suspend fun importState(blob: ByteArray): Unit = TODO("wired in build step 11")

    private suspend fun sendOrFail(command: String, args: Map<String, Any?>) {
        val ack = transport.sendCommand(command, args)
        if (!ack.ok) {
            error("$command failed: ${ack.error}")
        }
    }
}
