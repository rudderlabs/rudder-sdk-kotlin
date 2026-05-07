package com.rudderstack.scenarioengine.domain.helper

/**
 * The driver-side knob for moving the SUT through Android process and activity lifecycle.
 *
 * Each method is one conceptual operation — no timeouts in the contract; the adapter decides
 * how long to wait and how to verify the operation completed. On Android these all map to
 * `adb shell` invocations under the hood (`am start`, `am kill`, `pm clear`, etc.).
 */
interface LifecycleControl {
    /** Start the SUT's main activity. Used at scenario start; functionally same as [foreground]. */
    suspend fun launch()

    /** Send the SUT to the background (HOME key). Returns once the activity is no longer foreground. */
    suspend fun background()

    /** Bring the SUT back to the foreground (re-launch its main activity). */
    suspend fun foreground()

    /** Kill the SUT process (`am kill`). Returns once the process is no longer running. */
    suspend fun kill()

    /** Force-stop the SUT (`am force-stop`). Stronger than [kill]: also clears scheduled alarms etc. */
    suspend fun forceStop()

    /** Force-stop then re-launch — a full cold start, exercising SDK persistence-on-init. */
    suspend fun coldStart()

    /** Wipe the SUT's app data (`pm clear`). The next [launch] starts from a fresh install state. */
    suspend fun clearAppData()
}
