package com.rudderstack.sdk.kotlin.android.models.consent

import com.rudderstack.sdk.kotlin.android.consent.ConsentManagementProvider
import com.rudderstack.sdk.kotlin.android.utils.findDestination
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class ConsentResolverTest {

    // Rule 1: state gate

    @Test
    fun `given consent management disabled, when resolved, then the destination is consented`() {
        val state = consentedState(allowed = listOf("something-else")).copy(active = false)

        assertTrue(ConsentResolver.resolve(state, destinationConfig(entry())))
    }

    // Rule 2: entry lookup

    @Test
    fun `given a null destination config, when resolved, then the destination is consented`() {
        assertTrue(ConsentResolver.resolve(consentedState(allowed = listOf("something-else")), null))
    }

    @Test
    fun `given a destination config without consent entries, when resolved, then the destination is consented`() {
        val config = buildJsonObject { put("apiKey", "test-api-key") }

        assertTrue(ConsentResolver.resolve(consentedState(allowed = listOf("something-else")), config))
    }

    @Test
    fun `given no entry matching the active provider, when resolved, then the destination is consented`() {
        val config = destinationConfig(entry(provider = "ketch"))

        assertTrue(ConsentResolver.resolve(consentedState(allowed = listOf("something-else")), config))
    }

    @Test
    fun `given an entry whose provider differs only by case, when resolved, then it does not match`() {
        val config = destinationConfig(entry(provider = "Custom"))

        assertTrue(ConsentResolver.resolve(consentedState(allowed = listOf("something-else")), config))
    }

    @Test
    fun `given duplicate provider entries, when resolved, then only the first match is consulted`() {
        val config = destinationConfig(
            entry(consents = emptyList()),
            entry(consents = listOf("marketing"))
        )

        assertTrue(ConsentResolver.resolve(consentedState(allowed = listOf("something-else")), config))
    }

    @Test
    fun `given a first match with malformed consents, when resolved, then later entries are not consulted`() {
        val malformedFirst = buildJsonObject {
            put("provider", "custom")
            put("consents", "not-an-array")
        }
        val config = destinationConfig(malformedFirst, entry(consents = listOf("marketing")))

        assertTrue(ConsentResolver.resolve(consentedState(allowed = listOf("something-else")), config))
    }

    // Rule 3: configured ids

    @Test
    fun `given an entry with empty consents, when resolved, then the destination is consented`() {
        val config = destinationConfig(entry(consents = emptyList()))

        assertTrue(ConsentResolver.resolve(consentedState(allowed = listOf("something-else")), config))
    }

    @Test
    fun `given an entry with only blank consent ids, when resolved, then the destination is consented`() {
        val config = destinationConfig(entry(consents = listOf(" ", "")))

        assertTrue(ConsentResolver.resolve(consentedState(allowed = listOf("something-else")), config))
    }

    @Test
    fun `given configured ids with whitespace, when resolved, then ids are trimmed before matching`() {
        val config = destinationConfig(entry(consents = listOf(" marketing ")))

        assertTrue(ConsentResolver.resolve(consentedState(allowed = listOf("marketing")), config))
    }

    // Rules 4 & 5: strategy matching

    @ParameterizedTest(name = "given strategy \"{0}\", when one of two ids is allowed, then consented is {1}")
    @MethodSource("strategyAliases")
    fun `given a resolution strategy alias, when one of two ids is allowed, then the verdict follows normalization`(
        strategy: String,
        expected: Boolean
    ) {
        val config = destinationConfig(entry(consents = listOf("marketing", "analytics"), strategy = strategy))

        assertEquals(expected, ConsentResolver.resolve(consentedState(allowed = listOf("marketing")), config))
    }

    @Test
    fun `given and matching with every id allowed, when resolved, then the destination is consented`() {
        val config = destinationConfig(entry(consents = listOf("marketing", "analytics"), strategy = "and"))

        assertTrue(ConsentResolver.resolve(consentedState(allowed = listOf("marketing", "analytics")), config))
    }

    @Test
    fun `given or matching with no id allowed, when resolved, then the destination is denied`() {
        val config = destinationConfig(entry(consents = listOf("advertising"), strategy = "or"))

        assertFalse(ConsentResolver.resolve(consentedState(allowed = listOf("marketing")), config))
    }

    @Test
    fun `given a missing resolution strategy, when resolved, then it defaults to and matching`() {
        val config = destinationConfig(entry(consents = listOf("marketing", "analytics"), strategy = null))

        assertFalse(ConsentResolver.resolve(consentedState(allowed = listOf("marketing")), config))
    }

    @Test
    fun `given denied consent ids, when resolved, then they are never consulted`() {
        val config = destinationConfig(entry(consents = listOf("marketing"), strategy = "and"))
        val state = consentedState(allowed = listOf("marketing"), denied = listOf("marketing"))

        assertTrue(ConsentResolver.resolve(state, config))
    }

    // Malformed shapes: evaluation fails open

    @Test
    fun `given consent management as a string, when resolved, then the destination is consented`() {
        val config = buildJsonObject { put("consentManagement", "not-an-array") }

        assertTrue(ConsentResolver.resolve(consentedState(allowed = listOf("something-else")), config))
    }

    @Test
    fun `given a non object element among entries, when resolved, then the whole list is voided`() {
        val config = buildJsonObject {
            putJsonArray("consentManagement") {
                add("rogue-element")
                add(entry(consents = listOf("marketing")))
            }
        }

        assertTrue(ConsentResolver.resolve(consentedState(allowed = listOf("something-else")), config))
    }

    @Test
    fun `given a numeric provider, when resolved, then the entry does not match`() {
        val config = destinationConfig(buildJsonObject { put("provider", 123) })

        assertTrue(ConsentResolver.resolve(consentedState(allowed = listOf("something-else")), config))
    }

    @Test
    fun `given a numeric resolution strategy, when resolved, then it defaults to and matching`() {
        val malformedStrategy = buildJsonObject {
            put("provider", "custom")
            putJsonArray("consents") { addJsonObject { put("consent", "marketing") } }
            put("resolutionStrategy", 42)
        }

        assertFalse(
            ConsentResolver.resolve(consentedState(allowed = listOf("something-else")), destinationConfig(malformedStrategy))
        )
    }

    companion object {

        @JvmStatic
        fun strategyAliases() = listOf(
            Arguments.of("all", false),
            Arguments.of("any", true),
            Arguments.of("or", true),
            Arguments.of("OR", true),
            Arguments.of(" or ", true),
            Arguments.of("and", false),
            Arguments.of("", false),
            Arguments.of("unknown-strategy", false),
        )
    }

    // Rule 6: legacy fields are not consent rules

    @Test
    fun `given a destination carrying legacy consent fields, when resolved, then only consentManagement decides`() {
        val sourceConfig = LenientJson.decodeFromString<SourceConfig>(LEGACY_FIELDS_SOURCE_CONFIG)
        val config = findDestination(sourceConfig, "MockDestination")?.destinationConfig

        assertTrue(
            config?.containsKey("oneTrustCookieCategories") == true,
            "The legacy field must survive parsing untouched."
        )
        // C0004 is deliberately absent from the granted list: a resolver that read the legacy
        // categories would deny this destination.
        assertTrue(
            ConsentResolver.resolve(consentedState(allowed = listOf("marketing")), config),
            "Only consentManagement may gate; the legacy categories must be ignored."
        )
    }
}

// Helpers

private val LEGACY_FIELDS_SOURCE_CONFIG = """
{
  "source": {
    "id": "source-id", "name": "Android", "writeKey": "write-key", "enabled": true,
    "workspaceId": "workspace-id", "updatedAt": "2026-01-01T00:00:00.000Z",
    "destinations": [
      {
        "id": "destination-id", "name": "Mock Destination", "enabled": true,
        "config": {
          "apiKey": "test-api-key",
          "oneTrustCookieCategories": [ { "oneTrustCookieCategory": "C0004" } ],
          "consentManagement": [
            { "provider": "custom", "consents": [ { "consent": "marketing" } ], "resolutionStrategy": "and" }
          ]
        },
        "destinationDefinitionId": "destination-definition-id",
        "destinationDefinition": { "name": "MOCK DESTINATION", "displayName": "MockDestination" },
        "updatedAt": "2026-01-01T00:00:00.000Z",
        "shouldApplyDeviceModeTransformation": false,
        "propagateEventsUntransformedOnError": false
      }
    ]
  }
}
""".trimIndent()

private fun consentedState(
    allowed: List<String> = emptyList(),
    denied: List<String> = emptyList(),
) = ConsentManagementState(
    active = true,
    provider = ConsentManagementProvider.CUSTOM,
    allowedConsentIds = allowed,
    deniedConsentIds = denied,
)

private fun destinationConfig(vararg entries: JsonObject): JsonObject = buildJsonObject {
    put("apiKey", "test-api-key")
    putJsonArray("consentManagement") { entries.forEach { add(it) } }
}

private fun entry(
    provider: String = "custom",
    consents: List<String> = listOf("marketing"),
    strategy: String? = "and",
): JsonObject = buildJsonObject {
    put("provider", provider)
    putJsonArray("consents") { consents.forEach { addJsonObject { put("consent", it) } } }
    strategy?.let { put("resolutionStrategy", it) }
}
