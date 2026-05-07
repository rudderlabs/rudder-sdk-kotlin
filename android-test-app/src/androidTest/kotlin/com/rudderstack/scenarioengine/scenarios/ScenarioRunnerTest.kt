package com.rudderstack.scenarioengine.scenarios

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.rudderstack.scenarioengine.application.interpreter.Helpers
import com.rudderstack.scenarioengine.application.interpreter.SequentialInterpreter
import com.rudderstack.scenarioengine.domain.helper.SpyOracle
import com.rudderstack.scenarioengine.domain.scenario.Scenario
import com.rudderstack.scenarioengine.domain.scenario.ScenarioResult
import com.rudderstack.scenarioengine.domain.step.Step
import com.rudderstack.scenarioengine.infrastructure.lifecycle.AdbLifecycleControl
import com.rudderstack.scenarioengine.infrastructure.mockserver.OkHttpMockPlane
import com.rudderstack.scenarioengine.infrastructure.mockserver.OkHttpMockServer
import com.rudderstack.scenarioengine.infrastructure.spy.BroadcastSpyOracle
import com.rudderstack.scenarioengine.infrastructure.state.ContentProviderStateProbe
import com.rudderstack.scenarioengine.infrastructure.sut.BroadcastSut
import com.rudderstack.scenarioengine.infrastructure.transport.BroadcastTransport
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before

/**
 * Base class for scenario-driven instrumentation tests.
 *
 * Owns the wiring that is otherwise tedious to repeat per test:
 *  - brings up [OkHttpMockServer] on a random loopback port,
 *  - constructs the [BroadcastTransport] / [AdbLifecycleControl] / [ContentProviderStateProbe]
 *    adapters against the SUT, composes them into [Helpers],
 *  - tears everything down in `@After`.
 *
 * The SUT context is `instrumentation.targetContext` (not `.context`) — see the same comment
 * in `BroadcastTransportTest`. Tests run in the SUT's process under the SUT's UID; the AMS
 * caller-package check rejects driver-context broadcasts/queries with a [SecurityException].
 *
 * **`mockServerUrl` injection.** Scenarios are authored statically — the mock server's
 * loopback URL doesn't exist until [setUp] runs. [runScenario] rewrites every [Step.Init] in
 * the scenario to set `mockServerUrl = mockPlane.baseUrl` before handing it to the interpreter.
 * Authors never see or set the URL.
 *
 * Subclasses author scenarios as `val scenario = rudderScenario(...)` and call [runScenario]
 * from `@Test` methods.
 */
abstract class ScenarioRunnerTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val context = instrumentation.targetContext

    private lateinit var mockServer: OkHttpMockServer
    private lateinit var mockPlane: OkHttpMockPlane
    private lateinit var transport: BroadcastTransport
    private lateinit var spyOracle: BroadcastSpyOracle
    // `protected` since Step 9: the live-mode MCP test (McpLiveTest) constructs an McpServer
    // around the same helpers + interpreter the runner already wires for normal scenarios.
    // Visibility-widening rather than duplicating the wiring keeps the engine setup in one place.
    protected lateinit var helpers: Helpers
    protected lateinit var interpreter: SequentialInterpreter

    @Before
    fun setUp() = runBlocking {
        mockServer = OkHttpMockServer()
        mockPlane = OkHttpMockPlane(mockServer)
        mockPlane.start()

        transport = BroadcastTransport(context)
        // Construct the oracle on the same transport — it eagerly subscribes to
        // EVENT_TYPE_SDK_EVENT broadcasts so observations emitted between Step.AddSpyPlugin
        // and the assertion are captured.
        spyOracle = BroadcastSpyOracle(transport)
        val lifecycle = AdbLifecycleControl(device)
        val state = ContentProviderStateProbe(context)
        val sut = BroadcastSut(transport)

        helpers = Helpers(
            sut = sut,
            lifecycle = lifecycle,
            mockPlane = mockPlane,
            state = state,
            spy = spyOracle,
        )
        interpreter = SequentialInterpreter(helpers)

        // launch is non-destructive (am start) — safe even with the deferred destructive-op
        // survival problem (see src/androidTest/AndroidManifest.xml).
        lifecycle.launch()
    }

    @After
    fun tearDown() {
        runCatching { spyOracle.close() }
        runCatching { transport.close() }
        runBlocking { runCatching { mockPlane.shutdown() } }
    }

    /**
     * Driver-side spy oracle exposed for subclass assertions. Tests typically `addSpyPlugin`
     * via the DSL, run a scenario, then `spy.awaitObservation(...)` from `@Test` to verify the
     * SDK's internal behavior. The oracle is shared across the whole test method — tags set
     * inside the scenario are visible to assertions after [runScenario] returns.
     */
    protected val spy: SpyOracle get() = spyOracle

    /**
     * Run [scenario] through the interpreter, asserting the result is [ScenarioResult.Passed].
     *
     * Pre-processes the scenario's steps so any [Step.Init] picks up the live
     * [OkHttpMockPlane.baseUrl]. Authors leave `mockServerUrl = ""` (the DSL default) and the
     * runner injects the URL right before dispatch.
     */
    protected fun runScenario(scenario: Scenario) {
        val rewritten = scenario.copy(
            steps = scenario.steps.map { step ->
                if (step is Step.Init) step.copy(mockServerUrl = mockPlane.baseUrl) else step
            },
        )
        val result = runBlocking { interpreter.run(rewritten) }
        when (result) {
            is ScenarioResult.Passed -> Unit
            is ScenarioResult.Failed -> fail(
                "Scenario '${scenario.name}' failed at step ${result.failedAt}: ${result.reason}\n" +
                    "Step results: ${result.stepResults}",
            )
        }
    }
}
