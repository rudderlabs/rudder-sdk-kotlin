package com.rudderstack.scenarioengine.scenarios.mcp

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rudderstack.scenarioengine.application.mcp.McpServer
import com.rudderstack.scenarioengine.scenarios.ScenarioRunnerTest
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Live-mode entry point for the MCP server (§14.3 "live mode"). Boots an [McpServer] inside
 * the running androidTest process and blocks the test thread until the configured idle
 * timeout elapses. The host machine reaches the server through `adb reverse` set up by the
 * `startMcpLive` Gradle task.
 *
 * **Why this is a `@Test`.** Instrumentation tests are the only way to run code inside the
 * driver process with the engine helpers wired up. Making the entry point a `@Test` lets us
 * reuse [ScenarioRunnerTest]'s @Before / @After plumbing for the mock plane, broadcast
 * transport, lifecycle adapter, etc.
 *
 * **Why guarded by an Assume.** Running connectedAndroidTest normally would otherwise execute
 * this test and block until [LIVE_MAX_IDLE_MS]. The `assumeTrue` on the `mcp.live=true`
 * instrumentation argument means the test is *skipped* in normal runs and only fires when
 * `am instrument -e mcp.live true` is in effect — exactly what `startMcpLive` does.
 *
 * **Why an idle timeout, not block-forever.** A test that genuinely never returns hangs the
 * test runner / Gradle task / the user's terminal. A long but bounded sleep is the practical
 * compromise; the user kills the process explicitly when done by Ctrl+C'ing the Gradle task.
 */
@RunWith(AndroidJUnit4::class)
class McpLiveTest : ScenarioRunnerTest() {

    @Test
    fun mcp_live_session() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(
            "MCP live mode is opt-in; pass `-e mcp.live true` to start it.",
            args.getString("mcp.live") == "true",
        )

        val server = McpServer(helpers = helpers, interpreter = interpreter)
        server.start()
        try {
            Log.i(TAG, "MCP server listening on ${server.localUrl}")
            Log.i(TAG, "From the host, run: adb forward tcp:5111 tcp:5111")
            Log.i(TAG, "Then connect a client to: http://localhost:5111/sse")
            Thread.sleep(LIVE_MAX_IDLE_MS)
        } finally {
            server.stop()
        }
    }

    private companion object {
        const val TAG = "McpLiveTest"

        // 30 minutes. Long enough for an exploratory MCP session; short enough that an
        // accidentally-launched live test self-terminates instead of hanging the emulator.
        const val LIVE_MAX_IDLE_MS = 30L * 60_000L
    }
}
