package com.rudderstack.sdk.kotlin.core.internals.models

import com.rudderstack.sdk.kotlin.core.internals.models.provider.provideUserIdentityState
import com.rudderstack.sdk.kotlin.core.internals.models.useridentity.UserIdentity
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
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
        val initialUserIdentity = provideUserIdentityState(anonymousId = "initialId", userId = "userId")
        val newAnonymousId = "newAnonymousId"
        val action = UserIdentity.SetAnonymousIdAction(newAnonymousId)

        val updatedUserIdentity = action.reduce(initialUserIdentity)

        assertEquals(newAnonymousId, updatedUserIdentity.anonymousId)
        assertEquals(initialUserIdentity.userId, updatedUserIdentity.userId)
    }

    @Test
    fun `given anonymousId is non-empty, when storeAnonymousId called, then it should store anonymousId`() = runTest {
        val userIdentity = provideUserIdentityState(anonymousId = "nonEmptyId", userId = "userId")

        userIdentity.storeAnonymousId(mockStorage)

        coVerify { mockStorage.write(StorageKeys.ANONYMOUS_ID, "nonEmptyId") }
    }
}
