package com.rudderstack.kotlin.sdk.state

import com.rudderstack.kotlin.sdk.internals.models.UserIdentity
import com.rudderstack.kotlin.sdk.internals.storage.Storage
import com.rudderstack.kotlin.sdk.internals.storage.StorageKeys
import com.rudderstack.kotlin.sdk.internals.utils.empty
import com.rudderstack.kotlin.sdk.state.UserIdentityState.GenerateUserAnonymousID
import com.rudderstack.kotlin.sdk.state.UserIdentityState.SetIdentityAction
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class UserIdentityStateTest {

    private lateinit var userIdentityState: UserIdentityState
    private lateinit var setIdentityAction: SetIdentityAction

    private val storage: Storage = mockk(relaxed = true)
    private val testScope = TestScope(UnconfinedTestDispatcher())


    @Before
    fun setUp() {
        userIdentityState = UserIdentityState(UserIdentity(anonymousID = String.empty(), userId = String.empty()))
        setIdentityAction = SetIdentityAction(storage)

        every { storage.readString(any(), any()) } returns String.empty()
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `given no anonymous id is stored in the storage, when currentState is called, then UserIdentityState should return empty anonymous id`() {
        val expected = String.empty()
        every { storage.readString(StorageKeys.ANONYMOUS_ID, any()) } returns expected

        val actual = UserIdentityState.currentState(storage).userIdentity.anonymousID

        assertEquals(expected, actual)
    }

    @Test
    fun `given storage has an anonymous id, when currentState is called, then UserIdentityState should use the stored anonymous id`() {
        val expected = "<anonymous-id>"
        every { storage.readString(StorageKeys.ANONYMOUS_ID, any()) } returns expected

        val actual = UserIdentityState.currentState(storage).userIdentity.anonymousID

        assertEquals(expected, actual)
    }

    @Test
    fun `given empty anonymous id in storage, when GenerateUserAnonymousID is invoked, then a new UUID should be generated and stored`() {
        val reducer = GenerateUserAnonymousID(testScope)
        val expected = UUID.randomUUID().toString()

        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString() } returns expected

        val newState = reducer(userIdentityState, setIdentityAction)
        val actual = newState.userIdentity.anonymousID

        testScope.advanceUntilIdle()

        assertEquals(expected, actual)
        coVerify(exactly = 1) { storage.write(StorageKeys.ANONYMOUS_ID, expected) }
        coVerify(exactly = 1) { storage.write(StorageKeys.IS_ANONYMOUS_ID_BY_CLIENT, false) }

        unmockkStatic(UUID::class)
    }

    @Test
    fun `given non-empty anonymous id in action, when GenerateUserAnonymousID is invoked, then anonymous id from action should be used and stored`() {
        val expected = "<client-set-anonymous-id>"

        val reducer = GenerateUserAnonymousID(testScope)
        val newState = reducer(userIdentityState, SetIdentityAction(storage, anonymousID = expected))
        val actual = newState.userIdentity.anonymousID
        testScope.advanceUntilIdle() // Ensure coroutines complete

        assertEquals(expected, actual)
        coVerify(exactly = 1) { storage.write(StorageKeys.ANONYMOUS_ID, expected) }
        coVerify(exactly = 1) { storage.write(StorageKeys.IS_ANONYMOUS_ID_BY_CLIENT, true) }
    }

    @Test
    fun `given an anonymous id already exists in the storage, when action is invoked, then current anonymous id should be retrieved`() {
        val expected = "<existing-anonymous-id>"
        val currentState = userIdentityState.copy(
            userIdentity = userIdentityState.userIdentity.copy(anonymousID = expected)
        )
        val reducer = GenerateUserAnonymousID(testScope)

        val newState = reducer(currentState, setIdentityAction)
        val actual = newState.userIdentity.anonymousID
        testScope.advanceUntilIdle()

        assertEquals(expected, actual)
        coVerify(exactly = 1) { storage.write(StorageKeys.ANONYMOUS_ID, expected) }
        coVerify(exactly = 1) { storage.write(StorageKeys.IS_ANONYMOUS_ID_BY_CLIENT, false) }
    }

    @Test
    fun `given an anonymous id already exists in the storage, when the action is invoked without anonymous id, then an anonymous id should be generated`() {
        val emptyState = UserIdentityState(UserIdentity(anonymousID = String.empty(), userId = String.empty()))
        val reducer = GenerateUserAnonymousID(testScope)
        val expected = UUID.randomUUID().toString()

        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString() } returns expected

        val newState = reducer(emptyState, setIdentityAction)
        val actual = newState.userIdentity.anonymousID
        testScope.advanceUntilIdle()

        assertEquals(expected, actual)
        coVerify { storage.write(StorageKeys.ANONYMOUS_ID, expected) }

        unmockkStatic(UUID::class)
    }
}
