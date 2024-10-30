package com.rudderstack.kotlin.sdk.internals.models

import com.rudderstack.kotlin.sdk.internals.storage.Storage
import com.rudderstack.kotlin.sdk.internals.storage.StorageKeys
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UserIdentityTest {

    @MockK
    private lateinit var mockStorage: Storage

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `when initialState called, then should return UserIdentity with stored anonymousId`() {
        val storedAnonymousId = "someAnonymousId"
        every { mockStorage.readString(StorageKeys.ANONYMOUS_ID, any()) } returns storedAnonymousId

        val userIdentity = UserIdentity.initialState(mockStorage)

        assertEquals(storedAnonymousId, userIdentity.anonymousId)
        verify { mockStorage.readString(StorageKeys.ANONYMOUS_ID, any()) }
    }

    @Test
    fun `when SetAnonymousIdAction called, then it should update anonymousId in UserIdentity`() {
        val initialUserIdentity = UserIdentity(anonymousId = "initialId", userId = "userId")
        val newAnonymousId = "newAnonymousId"
        val action = UserIdentity.SetAnonymousIdAction(newAnonymousId)

        val updatedUserIdentity = action.reduce(initialUserIdentity)

        assertEquals(newAnonymousId, updatedUserIdentity.anonymousId)
        assertEquals(initialUserIdentity.userId, updatedUserIdentity.userId)
    }

    @Test
    fun `given anonymousId is non-empty, when storeAnonymousId called, then it should store anonymousId and set isAnonymousByClient to true`() = runTest {
        val userIdentity = UserIdentity(anonymousId = "nonEmptyId", userId = "userId")

        userIdentity.storeAnonymousId(mockStorage)

        coVerify { mockStorage.write(StorageKeys.ANONYMOUS_ID, "nonEmptyId") }
        coVerify { mockStorage.write(StorageKeys.IS_ANONYMOUS_ID_BY_CLIENT, true) }
    }

    @Test
    fun `given anonymousId is empty, when storeAnonymousId called, then it should set isAnonymousByClient to false`() = runTest {
        val userIdentity = UserIdentity(anonymousId = "", userId = "userId")

        userIdentity.storeAnonymousId(mockStorage)

        coVerify { mockStorage.write(StorageKeys.ANONYMOUS_ID, "") }
        coVerify { mockStorage.write(StorageKeys.IS_ANONYMOUS_ID_BY_CLIENT, false) }
    }
}
