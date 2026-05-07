package com.rudderstack.scenarioengine.infrastructure.state

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.rudderstack.scenarioengine.infrastructure.lifecycle.AdbLifecycleControl
import com.rudderstack.scenarioengine.infrastructure.transport.BroadcastTransport
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Validates that [ContentProviderStateProbe] reads `anonymousId` cross-package after the
 * SDK is initialized. The "null before init" half of the assertion is deliberately
 * dropped here — without `pm clear` (deferred to Step 6, see manifest comment),
 * we can't guarantee a fresh-uninitialized SUT. The non-null-after-init half still
 * proves the cross-package ContentResolver path works; the null path is exercised
 * indirectly by the manifest's StateProvider implementation, where `analytics?.anonymousId`
 * with `analytics == null` returns null by definition.
 *
 * Uses [BroadcastTransport] to drive INIT — peer-level adapter, fine to compose at the
 * test layer. The polling loop after INIT exists because the SDK's `anonymousId` is
 * computed asynchronously inside Analytics' constructor.
 */
@RunWith(AndroidJUnit4::class)
class ContentProviderStateProbeTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    // targetContext — see the same comment in BroadcastTransportTest. ContentResolver.query
    // crosses a binder check that AMS rejects when caller-package and calling-UID don't match.
    private val context = instrumentation.targetContext
    private val lifecycle = AdbLifecycleControl(device)
    private val probe = ContentProviderStateProbe(context)

    @Before
    fun setUp() = runBlocking { lifecycle.launch() }

    @Test
    fun anonymousId_is_non_null_after_init() = runBlocking {
        val transport = BroadcastTransport(context)
        try {
            transport.sendCommand(
                command = "INIT",
                args = mapOf(
                    "writeKey" to "test-key",
                    "mockServerUrl" to "http://127.0.0.1:8080",
                ),
            )

            val anonymousId = pollForNonNull(timeoutMs = ANONYMOUS_ID_TIMEOUT_MS) { probe.anonymousId() }
            assertNotNull(
                "anonymousId did not become non-null within ${ANONYMOUS_ID_TIMEOUT_MS}ms after INIT",
                anonymousId,
            )
        } finally {
            transport.close()
        }
    }

    private suspend fun <T : Any> pollForNonNull(timeoutMs: Long, read: suspend () -> T?): T? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val value = read()
            if (value != null) return value
            delay(POLL_INTERVAL_MS)
        }
        return read()
    }

    private companion object {
        const val ANONYMOUS_ID_TIMEOUT_MS = 5_000L
        const val POLL_INTERVAL_MS = 100L
    }
}
