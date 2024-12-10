package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.internals.models.ExternalId
import kotlinx.serialization.encodeToString
import org.junit.Test
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
