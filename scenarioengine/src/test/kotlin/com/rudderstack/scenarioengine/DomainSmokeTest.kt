package com.rudderstack.scenarioengine.domain

import com.rudderstack.scenarioengine.domain.scenario.Scenario
import com.rudderstack.scenarioengine.domain.scenario.ScenarioMetadata
import com.rudderstack.scenarioengine.domain.step.FieldMatch
import com.rudderstack.scenarioengine.domain.step.MatchOp
import com.rudderstack.scenarioengine.domain.step.StateField
import com.rudderstack.scenarioengine.domain.step.Step
import com.rudderstack.scenarioengine.domain.step.StepEventType
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DomainSmokeTest {

    @Test
    fun `scenario can be constructed with a representative mix of step types`() {
        val scenario = Scenario(
            name = "smoke.basic_track",
            description = "track event reaches the data plane with correct payload",
            steps = listOf(
                Step.Init(
                    writeKey = "test-write-key",
                    mockServerUrl = "http://localhost:8080",
                ),
                Step.Reset(),
                Step.Track(
                    name = "Purchase",
                    properties = buildJsonObject {
                        put("amount", 99)
                        put("currency", "USD")
                    },
                ),
                Step.WaitForEvent(
                    type = StepEventType.TRACK,
                    name = "Purchase",
                    match = listOf(
                        FieldMatch("properties.amount", MatchOp.EQ, JsonPrimitive(99)),
                        FieldMatch("properties.currency", MatchOp.EQ, JsonPrimitive("USD")),
                    ),
                ),
                Step.AssertField("properties.amount", JsonPrimitive(99)),
                Step.AssertState(StateField.ANONYMOUS_ID, expected = null),
                Step.Background,
                Step.Foreground,
            ),
            metadata = ScenarioMetadata(tags = setOf("smoke"), runtimeEstimateMs = 5_000),
        )

        assertEquals("smoke.basic_track", scenario.name)
        assertEquals(8, scenario.steps.size)
        assertTrue(scenario.steps.first() is Step.Init)
        assertTrue(scenario.metadata.tags.contains("smoke"))

        val track = scenario.steps.filterIsInstance<Step.Track>().single()
        assertEquals("Purchase", track.name)
        assertEquals(JsonPrimitive(99), track.properties["amount"])

        val wait = scenario.steps.filterIsInstance<Step.WaitForEvent>().single()
        assertEquals(2, wait.match.size)
        assertEquals(MatchOp.EQ, wait.match.first().op)
    }

    @Test
    fun `import state equality uses content not reference`() {
        val a = Step.ImportState(byteArrayOf(1, 2, 3))
        val b = Step.ImportState(byteArrayOf(1, 2, 3))
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
