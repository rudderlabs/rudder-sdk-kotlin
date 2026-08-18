package com.rudderstack.sdk.kotlin.core.internals.models

import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class SourceConfigConsentParsingTest {

    @Test
    fun `given metadata at the response root, when decoded, then provider entries are parsed`() {
        val json = sourceConfigJson(
            """
            "consentManagementMetadata": {
                "providers": [
                    { "provider": "ketch", "resolutionStrategy": "" },
                    { "provider": "custom", "resolutionStrategy": "or" }
                ]
            }
            """
        )

        val sourceConfig = LenientJson.decodeFromString<SourceConfig>(json)

        val providers = sourceConfig.consentManagementMetadata?.providers
        assertNotNull(providers)
        assertEquals(listOf("ketch", "custom"), providers?.map { it.provider })
        assertEquals(listOf("", "or"), providers?.map { it.resolutionStrategy })
    }

    @Test
    fun `given no metadata in the response, when decoded, then metadata is null`() {
        val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigJson(metadata = null))

        assertNull(sourceConfig.consentManagementMetadata)
    }

    @Test
    fun `given an unknown provider with extra fields, when decoded, then the entry is preserved`() {
        val json = sourceConfigJson(
            """
            "consentManagementMetadata": {
                "providers": [
                    { "provider": "iubenda", "resolutionStrategy": "and", "legacyField": true }
                ]
            }
            """
        )

        val sourceConfig = LenientJson.decodeFromString<SourceConfig>(json)

        assertEquals("iubenda", sourceConfig.consentManagementMetadata?.providers?.single()?.provider)
    }

    @Test
    fun `given an entry with missing fields, when decoded, then the fields default to null`() {
        val json = sourceConfigJson(""""consentManagementMetadata": { "providers": [ {} ] }""")

        val entry = LenientJson.decodeFromString<SourceConfig>(json).consentManagementMetadata?.providers?.single()

        assertNotNull(entry)
        assertNull(entry?.provider)
        assertNull(entry?.resolutionStrategy)
    }

    @ParameterizedTest(name = "given malformed metadata {0}, when decoded, then metadata is null")
    @MethodSource("malformedMetadata")
    fun `given malformed metadata, when decoded, then metadata is null with the rest of the config intact`(
        metadataFragment: String
    ) {
        val sourceConfig = LenientJson.decodeFromString<SourceConfig>(sourceConfigJson(metadataFragment))

        assertNull(sourceConfig.consentManagementMetadata)
        assertEquals("test-source-id", sourceConfig.source.sourceId)
        assertTrue(sourceConfig.source.isSourceEnabled)
    }

    @Test
    fun `given the initial state, then metadata is null`() {
        assertNull(SourceConfig.initialState().consentManagementMetadata)
    }

    @Test
    fun `given decoded metadata, when encoded then decoded again, then metadata survives the round trip`() {
        val original = LenientJson.decodeFromString<SourceConfig>(
            sourceConfigJson(
                """"consentManagementMetadata": { "providers": [ { "provider": "custom", "resolutionStrategy": "or" } ] }"""
            )
        )

        val roundTripped = LenientJson.decodeFromString<SourceConfig>(
            Json.encodeToString(SourceConfig.serializer(), original)
        )

        assertEquals(original.consentManagementMetadata, roundTripped.consentManagementMetadata)
    }

    @Test
    fun `given a disable source action, when reduced, then metadata is preserved`() {
        val sourceConfig = LenientJson.decodeFromString<SourceConfig>(
            sourceConfigJson(""""consentManagementMetadata": { "providers": [ { "provider": "custom" } ] }""")
        )

        val disabled = SourceConfig.DisableSourceAction().reduce(sourceConfig)

        assertFalse(disabled.source.isSourceEnabled)
        assertEquals(sourceConfig.consentManagementMetadata, disabled.consentManagementMetadata)
    }

    companion object {

        @JvmStatic
        fun malformedMetadata() = listOf(
            """"consentManagementMetadata": "not-an-object"""",
            """"consentManagementMetadata": 42""",
            """"consentManagementMetadata": { "providers": "not-an-array" }""",
            """"consentManagementMetadata": { "providers": [ { "provider": "custom" }, "rogue" ] }""",
            """"consentManagementMetadata": { "providers": [ { "provider": { "nested": true } } ] }""",
        )
    }
}

// Builds a sourceConfig response with the metadata fragment at the root, as a sibling of source.
private fun sourceConfigJson(metadata: String?): String {
    val metadataPart = metadata?.let { "$it," }.orEmpty()
    return """
    {
        $metadataPart
        "source": {
            "id": "test-source-id",
            "name": "test-source",
            "writeKey": "test-write-key",
            "enabled": true,
            "workspaceId": "test-workspace-id",
            "updatedAt": "2026-01-01T00:00:00.000Z"
        }
    }
    """
}
