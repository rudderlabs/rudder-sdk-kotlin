package com.rudderstack.scenarioengine.infrastructure.lifecycle

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Validates [AdbLifecycleControl]'s `launch` and `background` paths — the two non-destructive
 * methods on the interface that round-trip safely from inside an @Test today.
 *
 * **What's deliberately not covered here.** The destructive methods — `forceStop`, `kill`,
 * `coldStart`, `clearAppData` — all eventually trigger `am force-stop com.rudderstack.testapp`
 * or `pm clear com.rudderstack.testapp`. With AGP's default `<instrumentation
 * android:targetPackage="com.rudderstack.testapp"/>`, these ops crash the instrumentation
 * even under the two-APK split (the test process is not killed by uid; the framework kills
 * it because its `targetPackage` died). The fix requires pointing `targetPackage` at the
 * driver itself *and* making the test APK self-contained — see the comment in
 * `src/androidTest/AndroidManifest.xml`. Tracked for Step 6, when the interpreter wires
 * these Step types in.
 *
 * The independent `pidof` check ([sutIsRunning]) duplicates the adapter's private helper
 * intentionally — testing through the public API but verifying with an external probe
 * catches a class of bug where the adapter's poll and its operation read the same lie.
 */
@RunWith(AndroidJUnit4::class)
class AdbLifecycleControlTest {

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val lifecycle = AdbLifecycleControl(device)

    @Test
    fun launch_then_background_round_trip() = runBlocking {
        lifecycle.launch()
        assertTrue("SUT should be running after launch", sutIsRunning())

        // background() doesn't kill the process, so the SUT stays running.
        lifecycle.background()
        assertTrue("SUT should still be running after background", sutIsRunning())
    }

    private fun sutIsRunning(): Boolean =
        device.executeShellCommand("pidof $SUT_PACKAGE").trim().isNotEmpty()

    private companion object {
        const val SUT_PACKAGE = "com.rudderstack.testapp"
    }
}
