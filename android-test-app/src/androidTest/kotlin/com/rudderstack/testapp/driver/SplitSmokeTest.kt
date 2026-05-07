package com.rudderstack.testapp.driver

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rudderstack.scenarioengine.ipc.Commands
import com.rudderstack.testapp.ipc.StateProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test for the two-APK split (design doc §11).
 *
 * Validates three things in one shot:
 *  1. The driver test process actually runs under [DRIVER_APP_ID] — i.e. AGP applied
 *     `testApplicationId` and didn't silently fall back to the SUT's id.
 *  2. A cross-package broadcast (driver → SUT) targeting the SUT's [com.rudderstack.testapp.ipc.CommandReceiver]
 *     is delivered, parsed, and dispatched. INIT triggers the SDK to construct an `Analytics` instance.
 *  3. A cross-package ContentProvider read on `com.rudderstack.testapp.state/anonymousId` returns the
 *     SDK's anonymousId once it has finished bootstrapping.
 *
 * Step 4 will replace the raw `sendBroadcast` / `ContentResolver.query` calls with the
 * `BroadcastTransport` and `ContentProviderStateProbe` adapters. For now the test uses raw APIs
 * deliberately — Step 3's purpose is to prove the split works, not to introduce new abstractions.
 */
@RunWith(AndroidJUnit4::class)
class SplitSmokeTest {

    @Test
    fun init_then_state_probe_reads_anonymousId() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val driverContext = instrumentation.context

        // Belt-and-suspenders: catch a regression where someone reverts testApplicationId
        // and the driver silently rejoins the SUT process. If that happens, every other
        // assertion in this test would still pass for the wrong reason.
        assertEquals(
            "test process must run under the driver applicationId, not the SUT's",
            DRIVER_APP_ID,
            driverContext.packageName,
        )

        val initIntent = Intent(Commands.ACTION_COMMAND).apply {
            component = ComponentName(SUT_APP_ID, COMMAND_RECEIVER_CLASS)
            putExtra(Commands.EXTRA_CMD, Commands.CMD_INIT)
            putExtra(Commands.EXTRA_ARGS, INIT_ARGS_JSON)
            // Without FLAG_RECEIVER_FOREGROUND, broadcasts to a backgrounded SUT are
            // silently dropped by the OS background-execution policy on Android 8+.
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }
        driverContext.sendBroadcast(initIntent)

        val anonymousIdUri = Uri.parse("content://${StateProvider.AUTHORITY}/${StateProvider.FIELD_ANONYMOUS_ID}")
        val anonymousId = pollForNonNull(timeoutMs = INIT_TIMEOUT_MS) {
            driverContext.contentResolver.query(anonymousIdUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }

        assertNotNull(
            "anonymousId did not become non-null within ${INIT_TIMEOUT_MS}ms after INIT — " +
                "the broadcast may not have been delivered cross-package, or SDK init failed",
            anonymousId,
        )
    }

    /** Polls [read] every [INIT_POLL_INTERVAL_MS] until it returns non-null or [timeoutMs] elapses. */
    private fun <T : Any> pollForNonNull(timeoutMs: Long, read: () -> T?): T? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val value = read()
            if (value != null) return value
            Thread.sleep(INIT_POLL_INTERVAL_MS)
        }
        return read()
    }

    private companion object {
        const val SUT_APP_ID = "com.rudderstack.testapp"
        const val DRIVER_APP_ID = "com.rudderstack.testapp.driver"
        const val COMMAND_RECEIVER_CLASS = "com.rudderstack.testapp.ipc.CommandReceiver"
        const val INIT_ARGS_JSON =
            """{"writeKey":"test-key","mockServerUrl":"http://127.0.0.1:8080"}"""
        const val INIT_TIMEOUT_MS = 10_000L
        const val INIT_POLL_INTERVAL_MS = 100L
    }
}
