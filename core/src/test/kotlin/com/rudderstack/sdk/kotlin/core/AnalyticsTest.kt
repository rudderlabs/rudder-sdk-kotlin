package com.rudderstack.sdk.kotlin.core

import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.anonymousId
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.traits
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.userId
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.provideBasicStorage
import com.rudderstack.sdk.kotlin.core.internals.utils.MockMemoryStorage
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import io.mockk.every
import io.mockk.mockkStatic
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Before
import org.junit.Test

private const val CUSTOM_ANONYMOUS_ID = "custom-anonymous-id"
private const val USER_ID = "user-id"
private val TRAITS: JsonObject = buildJsonObject { put("key-1", "value-1")  }

class AnalyticsTest {

    private val configuration = provideConfiguration()

    private lateinit var analytics: Analytics
    private lateinit var mockStorage: Storage

    @Before
    fun setup() {
        mockStorage = MockMemoryStorage()

        mockkStatic(::provideBasicStorage)
        every { provideBasicStorage(any()) } returns mockStorage

        analytics = Analytics(configuration = configuration)
    }

    @After
    fun tearDown() {
        mockStorage.close()
    }

    @Test
    fun `when anonymousId is fetched, then it should return UUID as the anonymousId`() {
        val anonymousId = analytics.anonymousId

        // This pattern ensures the string follows the UUID v4 format.
        val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        assertTrue(anonymousId?.matches(uuidRegex) == true)
    }

    @Test
    fun `given custom anonymousId is set, when anonymousId is fetched, then it should return the custom anonymousId`() {
        analytics.anonymousId = CUSTOM_ANONYMOUS_ID

        val anonymousId = analytics.anonymousId

        assertTrue(anonymousId == CUSTOM_ANONYMOUS_ID)
    }

    @Test
    fun `given empty string is set as custom anonymousId, when anonymousId is fetched, then it should return the empty anonymousId`() {
        analytics.anonymousId = String.empty()

        val anonymousId = analytics.anonymousId

        assertTrue(anonymousId == String.empty())
    }

    @Test
    fun `given sdk is shutdown, when anonymousId is fetched, then it should return null`() {
        analytics.shutdown()

        val anonymousId = analytics.anonymousId

        assertNull(anonymousId)
    }

    @Test
    fun `given userId and traits are set, when they are fetched, then proper values are returned`() {
        analytics.identify(userId = USER_ID, traits = TRAITS)

        val userId = analytics.userId
        val traits = analytics.traits

        assertTrue(userId == USER_ID)
        assertTrue(traits == TRAITS)
    }

    @Test
    fun `given userId and traits are not set, when they are fetched, then empty values are returned`() {
        val userId = analytics.userId
        val traits = analytics.traits

        assertTrue(userId == String.empty())
        assertTrue(traits == emptyJsonObject)
    }

    @Test
    fun `given userId and traits are not set and sdk is shutdown, when they are fetched, then it should return null`() {
        analytics.identify(userId = USER_ID, traits = TRAITS)
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
        dataPlaneUrl = "https://hosted.rudderlabs.com",
    )
