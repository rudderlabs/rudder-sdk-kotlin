package com.rudderstack.sdk.kotlin.core.internals.models

import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.UserIdentity
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.MockMemoryStorage
import com.rudderstack.sdk.kotlin.core.internals.utils.generateUUID
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserIdentityTest {

    private lateinit var mockStorage: Storage

    @BeforeEach
    fun setup() {
        mockStorage = MockMemoryStorage()
    }

    @Test
    fun `given some stored anonymousId, when initialState called, then it should return UserIdentity with stored anonymousId`() =
        runTest {
            val storedAnonymousId = "someAnonymousId"
            mockStorage.write(StorageKeys.ANONYMOUS_ID, storedAnonymousId)

            val userIdentity = UserIdentity.initialState(mockStorage)

            assertEquals(storedAnonymousId, userIdentity.anonymousId)
        }

    @Test
    fun `given no stored anonymousId, when initialState called, then it should return UserIdentity with generated anonymousId`() {
        val generatedAnonymousId = "generatedAnonymousId"

        mockkStatic(::generateUUID)
        every { generateUUID() } returns generatedAnonymousId

        val userIdentity = UserIdentity.initialState(mockStorage)

        assertEquals(generatedAnonymousId, userIdentity.anonymousId)
    }

    @Test
    fun `when storeAnonymousId called, then it should store the anonymousId in storage`() = runTest {
        val expectedAnonymousId = "someAnonymousId"

        val userIdentity = UserIdentity(
            anonymousId = expectedAnonymousId,
            userId = "someUserId",
            traits = emptyJsonObject
        )

        userIdentity.storeAnonymousId(mockStorage)

        assertEquals(expectedAnonymousId, mockStorage.readString(StorageKeys.ANONYMOUS_ID, "someDefaultValue"))
    }
}
