package com.rudderstack.kotlin.sdk.internals.utils

import com.rudderstack.kotlin.sdk.internals.models.ExternalId
import com.rudderstack.kotlin.sdk.internals.models.RudderTraits
import com.rudderstack.kotlin.sdk.internals.models.emptyJsonObject
import com.rudderstack.kotlin.sdk.internals.storage.Storage
import com.rudderstack.kotlin.sdk.internals.storage.StorageKeys
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class StorageUtilsTest {

    @MockK
    lateinit var mockStorage: Storage

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
    }

    @Test
    fun `given value is of RudderTraits type, when traits are read from storage, then return RudderTraits object`() {
        every { mockStorage.readString(any(), any()) } returns getTraitsString()

        val result = mockStorage.readValuesOrDefault<RudderTraits>(key = StorageKeys.TRAITS, defaultValue = emptyJsonObject)

        val expected = getTraitsJson()
        assertEquals(expected, result)
    }

    @Test
    fun `given value is of RudderTraits type, when empty traits are read from storage, then return default value`() {
        every { mockStorage.readString(any(), any()) } returns "{}"

        val result = mockStorage.readValuesOrDefault<RudderTraits>(key = StorageKeys.TRAITS, defaultValue = emptyJsonObject)

        val expected = emptyJsonObject
        assertEquals(expected, result)
    }

    @Test
    fun `given value is of List of ExternalIds type, when externalIds are read from storage, then return List of ExternalIds object`() {
        every { mockStorage.readString(any(), any()) } returns getListOfExternalIdsString()

        val result = mockStorage.readValuesOrDefault<List<ExternalId>>(key = StorageKeys.EXTERNAL_IDS, defaultValue = emptyList())

        val expected = getListOfExternalIds()
        assertEquals(expected, result)
    }

    @Test
    fun `given value is of List of ExternalIds type, when empty externalIds are read from storage, then return default value`() {
        every { mockStorage.readString(any(), any()) } returns "[]"

        val result = mockStorage.readValuesOrDefault<List<ExternalId>>(key = StorageKeys.EXTERNAL_IDS, defaultValue = emptyList())

        val expected = emptyList<ExternalId>()
        assertEquals(expected, result)
    }
}

private fun getTraitsString() =
    """
        {
          "key-1": "value-1",
          "anonymousId": "<anonymousId>"
        }
    """.trimIndent()

private fun getTraitsJson() =
    buildJsonObject {
        put("key-1", "value-1")
        put("anonymousId", "<anonymousId>")
    }

private fun getListOfExternalIdsString() =
    """
        [
          {
            "id": "id 1",
            "type": "brazeExternalId"
          },
          {
            "id": "id 2",
            "type": "ga4"
          }
        ]
    """.trimIndent()

private fun getListOfExternalIds() =
    listOf(
        ExternalId(type = "brazeExternalId", id = "id 1"),
        ExternalId(type = "ga4", id = "id 2"),
    )
