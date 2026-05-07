package com.rudderstack.scenarioengine.infrastructure.transport

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.rudderstack.scenarioengine.infrastructure.lifecycle.AdbLifecycleControl
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises [BroadcastTransport] against the live SUT.
 *
 * Two cases:
 *  - INIT round-trip: cross-package broadcast → dispatcher → callback event → ack.ok=true.
 *    Validates the success path of the command channel and the callback registry.
 *  - Unknown-command round-trip: dispatcher throws → error event → ack.ok=false with the
 *    SUT's error message preserved. Validates the failure path.
 *
 * Uses [AdbLifecycleControl] for setup/teardown — peer-level adapter, fine to compose at
 * the test layer. Direct `runBlocking` (not `runTest`) because instrumentation test timing
 * is real wall-clock; `runTest` skips delays and would defeat the polling helpers.
 */
@RunWith(AndroidJUnit4::class)
class BroadcastTransportTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    // targetContext (the SUT's context) — using instrumentation.context (the .driver context)
    // throws SecurityException at registerReceiver because AMS sees a caller-package vs
    // calling-UID mismatch (test code runs in the SUT process under the SUT's UID with the
    // default targetPackage = SUT). targetContext.packageName matches the running process,
    // so the AMS check passes. Same reasoning applies to ContentResolver.query in the probe.
    private val context = instrumentation.targetContext
    private val lifecycle = AdbLifecycleControl(device)
    private lateinit var transport: BroadcastTransport

    @Before
    fun setUp() = runBlocking {
        // launch() is non-destructive (am start) — safe to call even with the deferred
        // destructive-op survival problem. State carries over between tests, but each test
        // here is independent of prior INIT/TRACK state because they assert on the round-trip
        // of *this* call, not on global SUT state.
        lifecycle.launch()
        transport = BroadcastTransport(context)
    }

    @After
    fun tearDown() {
        transport.close()
        // No forceStop — see Step 6 deferred-work note in src/androidTest/AndroidManifest.xml.
    }

    @Test
    fun init_command_round_trips_with_ok_ack() = runBlocking {
        val ack = transport.sendCommand(
            command = "INIT",
            args = mapOf(
                "writeKey" to "test-key",
                "mockServerUrl" to "http://127.0.0.1:8080",
            ),
        )
        assertTrue("INIT should ack ok=true but got: ${ack.error}", ack.ok)
        assertEquals(null, ack.error)
    }

    @Test
    fun unknown_command_round_trips_with_error_ack() = runBlocking {
        val ack = transport.sendCommand(command = "DEFINITELY_NOT_A_COMMAND", args = emptyMap())
        assertFalse("unknown command should ack ok=false", ack.ok)
        assertNotNull("error message should be present", ack.error)
        assertTrue(
            "error should mention 'not implemented' (got: ${ack.error})",
            ack.error!!.contains("not implemented", ignoreCase = true),
        )
    }
}
