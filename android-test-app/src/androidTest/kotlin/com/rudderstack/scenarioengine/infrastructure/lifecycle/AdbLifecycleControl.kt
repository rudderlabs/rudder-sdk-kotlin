package com.rudderstack.scenarioengine.infrastructure.lifecycle

import androidx.test.uiautomator.UiDevice
import com.rudderstack.scenarioengine.domain.helper.LifecycleControl
import kotlinx.coroutines.delay

/**
 * Android implementation of [LifecycleControl] driving the SUT through `am` / `pm` shell
 * commands via UiAutomator.
 *
 * Exclusively uses [UiDevice.executeShellCommand] — chosen over [android.content.Context]
 * APIs because operations like `am force-stop` and `pm clear` require shell uid privileges
 * the driver app's uid doesn't have. UiAutomator runs commands as the shell user.
 *
 * Each operation that affects process state polls `pidof` until the process reaches the
 * desired state, with a soft timeout. The contract (see [LifecycleControl]) deliberately
 * doesn't expose timeouts — they're an implementation concern. If the SUT doesn't move
 * within the timeout the adapter throws; tests should consider that a real SUT bug, not a
 * flake to retry.
 *
 * Not safe to call concurrently against the same [device] / SUT — each method assumes the
 * SUT is idle until the call returns.
 *
 * @param device the UiAutomator handle for the test device.
 * @param sutPackage applicationId of the SUT — defaults to the Step 3 split's SUT id.
 * @param mainActivity fully-qualified activity class for [launch] / [foreground] / [coldStart].
 * @param processSettleTimeoutMs how long to poll `pidof` before declaring the SUT stuck.
 */
class AdbLifecycleControl(
    private val device: UiDevice,
    private val sutPackage: String = SUT_PACKAGE,
    private val mainActivity: String = "$SUT_PACKAGE.MainActivity",
    private val processSettleTimeoutMs: Long = 5_000L,
) : LifecycleControl {

    override suspend fun launch() {
        device.executeShellCommand("am start -n $sutPackage/$mainActivity")
        waitUntil(running = true)
    }

    override suspend fun background() {
        device.executeShellCommand("input keyevent KEYCODE_HOME")
    }

    override suspend fun foreground() {
        // Same shell command as launch — the activity manager handles bring-to-front semantics.
        launch()
    }

    override suspend fun kill() {
        // **Doc deviation.** §10.2 specifies `am kill`. In practice AMS treats `am kill` as a
        // best-effort, AMS-decides-when call: a SUT with a recently-init'd SDK keeps "important"
        // priority for several seconds even after `am make-uid-idle`, leaving `am kill` a no-op
        // within any reasonable test timeout.
        //
        // `kill -9 PID` is the unconditional equivalent and preserves the Step.Kill *intent* —
        // an OS-level process kill where app data is preserved and only the process dies
        // (distinct from `am force-stop`'s broader cleanup of alarms / pending intents).
        // Issued via `run-as <SUT-package>` so the kill executes as the SUT's own UID; the
        // shell UID (2000) lacks SIGKILL privileges against app processes. `run-as` requires
        // the target to be debuggable, which the test SUT always is.
        val pid = device.executeShellCommand("pidof $sutPackage").trim()
        if (pid.isNotEmpty()) {
            device.executeShellCommand("run-as $sutPackage kill -9 $pid")
            waitUntil(running = false)
        }
    }

    override suspend fun forceStop() {
        device.executeShellCommand("am force-stop $sutPackage")
        waitUntil(running = false)
    }

    override suspend fun coldStart() {
        device.executeShellCommand("am force-stop $sutPackage")
        waitUntil(running = false)
        // -S forces a fresh start even if the activity is already at the top of its task.
        device.executeShellCommand("am start -S -n $sutPackage/$mainActivity")
        waitUntil(running = true)
    }

    override suspend fun clearAppData() {
        device.executeShellCommand("pm clear $sutPackage")
    }

    /**
     * Polls `pidof` every [POLL_INTERVAL_MS] until the SUT process state matches [running] or
     * [processSettleTimeoutMs] elapses. Throws on timeout — by then something is wrong with
     * the SUT or the emulator, not with the test.
     */
    private suspend fun waitUntil(running: Boolean) {
        val deadline = System.currentTimeMillis() + processSettleTimeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (isRunning() == running) return
            delay(POLL_INTERVAL_MS)
        }
        val state = if (running) "running" else "stopped"
        error("$sutPackage did not become $state within ${processSettleTimeoutMs}ms")
    }

    private fun isRunning(): Boolean =
        device.executeShellCommand("pidof $sutPackage").trim().isNotEmpty()

    private companion object {
        const val SUT_PACKAGE = "com.rudderstack.testapp"
        const val POLL_INTERVAL_MS = 100L
    }
}
