package com.rudderstack.sdk.kotlin.core

import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.provideBasicStorage
import com.rudderstack.sdk.kotlin.core.internals.utils.MockMemoryStorage
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val USER_ID = "user-id"
private val TRAITS: JsonObject = buildJsonObject { put("key-1", "value-1") }

class AnalyticsTest {

    private val configuration = provideConfiguration()

    private lateinit var analytics: Analytics
    private lateinit var mockStorage: Storage

    @BeforeEach
    fun setup() {
        mockStorage = MockMemoryStorage()

        mockkStatic(::provideBasicStorage)
        every { provideBasicStorage(any()) } returns mockStorage

        analytics = Analytics(configuration = configuration)
    }

    @Test
    fun `when anonymousId is fetched, then it should return UUID as the anonymousId`() {
        val anonymousId = analytics.anonymousId

        // This pattern ensures the string follows the UUID v4 format.
        val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        assertTrue(anonymousId?.matches(uuidRegex) == true)
    }

    @Test
    fun `given sdk is shutdown, when anonymousId is fetched, then it should return null`() {
        analytics.shutdown()

        val anonymousId = analytics.anonymousId

        assertNull(anonymousId)
    }

    @Test
    fun `given userId and traits are set, when they are fetched, then the set values are returned`() {
        // userId and traits can be set only through identify api
        analytics.identify(userId = USER_ID, traits = TRAITS)

        val userId = analytics.userId
        val traits = analytics.traits

        assertEquals(USER_ID, userId)
        assertEquals(TRAITS, traits)
    }

    @Test
    fun `given userId and traits are not set, when they are fetched, then empty values are returned`() {
        val userId = analytics.userId
        val traits = analytics.traits

        assertEquals(String.empty(), userId)
        assertEquals(emptyJsonObject, traits)
    }

    @Test
    fun `given sdk is shutdown, when userId and traits are fetched, then it should return null`() {
        analytics.shutdown()

        val userId = analytics.userId
        val traits = analytics.traits

        assertNull(userId)
        assertNull(traits)
    }
}

private fun provideConfiguration() =
    Configuration(
        writeKey = "<writeKey>",
        dataPlaneUrl = "<data_plane_url>",
    )
