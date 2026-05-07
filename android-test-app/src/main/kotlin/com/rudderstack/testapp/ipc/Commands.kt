package com.rudderstack.testapp.ipc

/**
 * Wire-level constants shared between the SUT (this side) and the driver (engine side).
 *
 * The action strings, extra keys, and command/event names form the IPC ABI. Renaming
 * any of these here without updating the corresponding driver-side adapter
 * (`BroadcastTransport`, lands in Step 4) will break the engine — treat them as a contract.
 */
internal object Commands {

    /** Driver → SUT broadcast action. Carries one command per intent. */
    const val ACTION_COMMAND = "com.rudderstack.testapp.COMMAND"

    /** SUT → driver broadcast action. Carries acks, results, errors, and unsolicited spy events. */
    const val ACTION_EVENT = "com.rudderstack.testapp.EVENT"

    // ---- Command intent extras (driver → SUT) ----
    /** Required. The command name — see [CMD_INIT], [CMD_TRACK], etc. */
    const val EXTRA_CMD = "cmd"

    /** Required. Compact JSON object string carrying command arguments. */
    const val EXTRA_ARGS = "args"

    /** Optional. If present, SUT acks (or errors) on completion using this id. */
    const val EXTRA_CALLBACK_ID = "callbackId"

    /** Optional. If present, SUT replies with a result payload using this id. */
    const val EXTRA_RESULT_ID = "resultId"

    // ---- Event intent extras (SUT → driver) ----
    /** Event kind — one of [EVENT_TYPE_CALLBACK], [EVENT_TYPE_RESULT], [EVENT_TYPE_ERROR], [EVENT_TYPE_SDK_EVENT]. */
    const val EXTRA_EVENT_TYPE = "type"

    /** Correlation id matching a [EXTRA_CALLBACK_ID] / [EXTRA_RESULT_ID]; absent for unsolicited SDK events. */
    const val EXTRA_EVENT_ID = "id"

    /** JSON body — shape depends on [EXTRA_EVENT_TYPE]. */
    const val EXTRA_EVENT_PAYLOAD = "payload"

    /** Acknowledgement that a command completed without error. Empty payload. */
    const val EVENT_TYPE_CALLBACK = "callback"

    /** A command with [EXTRA_RESULT_ID] returning data. Payload carries the result. */
    const val EVENT_TYPE_RESULT = "result"

    /** A command failed. Payload carries `{message, type}`. */
    const val EVENT_TYPE_ERROR = "error"

    /** Unsolicited spy-plugin observation (Step 7+). Payload carries the observation. */
    const val EVENT_TYPE_SDK_EVENT = "sdkEvent"

    // ---- Command names ----
    // Step 5 wires INIT, TRACK, and RESET end-to-end. RESET lands here (rather than Step 6)
    // because the DSL's `defaultInit = true` path auto-prepends [Init, Reset] — exercising the
    // canonical authoring path requires Reset to round-trip. The remaining event/session/
    // lifecycle commands land in Step 6.
    const val CMD_INIT = "INIT"
    const val CMD_TRACK = "TRACK"
    const val CMD_RESET = "RESET"
}
