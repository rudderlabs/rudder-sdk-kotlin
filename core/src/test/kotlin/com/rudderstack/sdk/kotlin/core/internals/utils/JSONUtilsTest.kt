package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.internals.models.ExternalId
import com.rudderstack.sdk.kotlin.core.internals.models.TrackEvent
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.UserIdentity
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

class JSONUtilsTest {

    @Test
    fun `given one external ids is passed, when toJson is called on List of ExternalId type values, then it should return a JsonObject`() {
        val externalIds = listOf(
            ExternalId("brazeExternalId", "1"),
            ExternalId("ga4", "2")
        )

        val externalIdsJson = externalIds.toJsonObject()
        val externalIdsJsonString = LenientJson.encodeToString(externalIdsJson)

        val expectedJson = getOneExternalIdsInStringFormat()
        JSONAssert.assertEquals(expectedJson, externalIdsJsonString, false)
    }

    @Test
    fun `given multiple external ids are passed, when toJson is called on List of ExternalId type values, then it should return a JsonObject`() {
        val externalIds = listOf(
            ExternalId("brazeExternalId", "1"),
            ExternalId("ga4", "2"),
            ExternalId("amplitude", "3"),
            ExternalId("clevertap", "4")
        )

        val externalIdsJson = externalIds.toJsonObject()
        val externalIdsJsonString = LenientJson.encodeToString(externalIdsJson)

        val expectedJson = getMultipleExternalIdsInStringFormat()
        JSONAssert.assertEquals(expectedJson, externalIdsJsonString, false)
    }

    @Test
    fun `given empty external ids is passed, when toJson is called on List of ExternalId type values, then it should return an empty JsonObject`() {
        val externalIds = emptyList<ExternalId>()

        val externalIdsJson = externalIds.toJsonObject()
        val externalIdsJsonString = LenientJson.encodeToString(externalIdsJson)

        val expectedJson = "{}"
        JSONAssert.assertEquals(expectedJson, externalIdsJsonString, false)
    }

    @Test
    fun `given an event with an empty anonymousId, when it is encoded, then anonymousId is absent only with the omit flag`() {
        val event = TrackEvent(
            event = "Order Completed",
            properties = emptyJsonObject,
            userIdentityState = UserIdentity(anonymousId = String.empty(), userId = "user-1", traits = emptyJsonObject),
        ).also { it.updateData(PlatformType.Server) }

        val defaultJson = LenientJson.parseToJsonElement(event.encodeToString()).jsonObject
        val serverJson = LenientJson.parseToJsonElement(event.encodeToString(omitBlankAnonymousId = true)).jsonObject

        assertEquals(String.empty(), defaultJson["anonymousId"]?.jsonPrimitive?.content)
        assertFalse(serverJson.containsKey("anonymousId"))
        assertEquals("user-1", serverJson["userId"]?.jsonPrimitive?.content)
        assertEquals("server", serverJson["channel"]?.jsonPrimitive?.content)
    }
}

private fun getOneExternalIdsInStringFormat() =
    """
        {
          "externalId": [
            {
              "id": "1",
              "type": "brazeExternalId"
            },
            {
              "id": "2",
              "type": "ga4"
            }
          ]
        }
    """.trimIndent()

private fun getMultipleExternalIdsInStringFormat() =
    """
        {
          "externalId": [
            {
              "id": "1",
              "type": "brazeExternalId"
            },
            {
              "id": "2",
              "type": "ga4"
            },
            {
              "id": "3",
              "type": "amplitude"
            },
            {
              "id": "4",
              "type": "clevertap"
            }
          ]
        }
    """.trimIndent()
