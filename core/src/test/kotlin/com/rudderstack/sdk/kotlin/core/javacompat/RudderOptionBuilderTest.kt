package com.rudderstack.sdk.kotlin.core.javacompat

import com.rudderstack.sdk.kotlin.core.internals.models.ExternalId
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RudderOptionBuilderTest {

    @Test
    fun `given integrations map, when setIntegrations called, then integrations are set correctly in result`() {
        val builder = RudderOptionBuilder()
        val integrations: Map<String, Any> = getMap()

        val result = builder.setIntegrations(integrations).build()

        val expected = getJsonObject()
        assertEquals(expected, result.integrations)
    }

    @Test
    fun `given external ids list, when setExternalId called, then externalIds are set correctly in result`() {
        val builder = RudderOptionBuilder()
        val externalIds = getExternalIds()

        val result = builder.setExternalId(externalIds).build()

        assertEquals(externalIds, result.externalIds)
    }

    @Test
    fun `given custom context map, when setCustomContext called, then customContext is set correctly in result`() {
        val builder = RudderOptionBuilder()
        val customContext = getMap()

        val result = builder.setCustomContext(customContext).build()

        val expected = getJsonObject()
        assertEquals(expected, result.customContext)
    }

    @Test
    fun `when all setters chained, then builds complete RudderOption with all values set`() {
        val integrations = getMap()
        val externalIds = getExternalIds()
        val customContext = getMap()

        val result = RudderOptionBuilder()
            .setIntegrations(integrations)
            .setExternalId(externalIds)
            .setCustomContext(customContext)
            .build()

        val expectedIntegrations = getJsonObject()
        val expectedExternalIds = getExternalIds()
        val expectedCustomContext = getJsonObject()
        assertEquals(expectedIntegrations, result.integrations)
        assertEquals(expectedExternalIds, result.externalIds)
        assertEquals(expectedCustomContext, result.customContext)
    }
}

private fun getMap() = mapOf(
    "All" to true,
    "Google Analytics" to false,
    "key-1" to mapOf(
        "key-2" to "value-2",
        "key-3" to 23,
    )
)

private fun getExternalIds() = listOf(
    ExternalId("id1", "value1"),
    ExternalId("id2", "value2")
)

private fun getJsonObject() = buildJsonObject {
    put("All", true)
    put("Google Analytics", false)
    put("key-1", buildJsonObject {
        put("key-2", "value-2")
        put("key-3", 23)
    })
}
