package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.internals.models.Traits
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StorageUtilsTest {

    @MockK
    lateinit var mockStorage: Storage

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
    }

    @Test
    fun `given value is of Traits type, when traits are read from storage, then return Traits object`() {
        every { mockStorage.readString(any(), any()) } returns getTraitsString()

        val result = mockStorage.readValuesOrDefault<Traits>(key = StorageKeys.TRAITS, defaultValue = emptyJsonObject)

        val expected = getTraitsJson()
        assertEquals(expected, result)
    }

    @Test
    fun `given value is of Traits type, when empty traits are read from storage, then return default value`() {
        every { mockStorage.readString(any(), any()) } returns "{}"

        val result = mockStorage.readValuesOrDefault<Traits>(key = StorageKeys.TRAITS, defaultValue = emptyJsonObject)

        val expected = emptyJsonObject
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
