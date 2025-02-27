package com.rudderstack.sdk.kotlin.core.internals.models.useridentity

import com.rudderstack.sdk.kotlin.core.ANONYMOUS_ID
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.provider.provideUserIdentityState
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.generateUUID
import com.rudderstack.sdk.kotlin.core.mockAnalytics
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val NEW_ANONYMOUS_ID = "newAnonymousId"
private const val USER_ID = "user1"
private val TRAITS = buildJsonObject { put("key-1", "value-1") }

class ResetUserIdentityActionTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mockAnalytics: Analytics = mockAnalytics(testScope, testDispatcher)

    @BeforeEach
    fun setup() {
        mockkStatic(::generateUUID)
        every { generateUUID() } returns NEW_ANONYMOUS_ID
    }

    @Test
    fun `given some value is present in the user identity and clearAnonymousId is true, when reset user identity action is performed, then it should reset all values`() {
        val userIdentityState = provideUserIdentityStateAfterFirstIdentifyEventIsMade()

        val result = ResetUserIdentityAction
            .reduce(userIdentityState)

        val expected = provideUserIdentityState(anonymousId = NEW_ANONYMOUS_ID)
        assert(expected == result)
    }

    @Test
    fun `given some value is present in the user identity and clearAnonymousId is false, when reset user identity action is performed, then it should reset all values except anonymous ID`() {
        val userIdentityState = provideUserIdentityStateAfterFirstIdentifyEventIsMade()

        val result = ResetUserIdentityAction
            .reduce(userIdentityState)

        val expected = provideUserIdentityState(anonymousId = ANONYMOUS_ID)
        assert(expected == result)
    }

    @Test
    fun `given empty user identity and clearAnonymousId is true, when reset user identity action is performed, then it should reset only anonymous ID`() {
        val userIdentityState = provideUserIdentityState()

        val result = ResetUserIdentityAction
            .reduce(userIdentityState)

        val expected = provideUserIdentityState(anonymousId = NEW_ANONYMOUS_ID)
        assert(expected == result)
    }

    @Test
    fun `given clearAnonymousId is true, when reset user identity action is performed, then it should reset all values`() =
        runTest {
            val userIdentityState = provideUserIdentityState(anonymousId = NEW_ANONYMOUS_ID)

            userIdentityState.resetUserIdentity(clearAnonymousId = true, storage = mockAnalytics.storage)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) {
                mockAnalytics.storage.apply {
                    write(StorageKeys.ANONYMOUS_ID, NEW_ANONYMOUS_ID)
                    remove(StorageKeys.USER_ID)
                    remove(StorageKeys.TRAITS)
                }
            }
        }

    @Test
    fun `given clearAnonymousId is false, when reset user identity action is performed, then it should reset all values except anonymous ID`() =
        runTest {
            val userIdentityState = provideUserIdentityState(anonymousId = ANONYMOUS_ID)
            val storage = mockAnalytics.storage

            userIdentityState.resetUserIdentity(clearAnonymousId = false, storage = storage)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) {
                storage.write(StorageKeys.ANONYMOUS_ID, ANONYMOUS_ID)
            }
            coVerify(exactly = 1) {
                storage.apply {
                    remove(StorageKeys.USER_ID)
                    remove(StorageKeys.TRAITS)
                }
            }
        }
}

private fun provideUserIdentityStateAfterFirstIdentifyEventIsMade(): UserIdentity =
    UserIdentity(
        anonymousId = ANONYMOUS_ID,
        userId = USER_ID,
        traits = TRAITS,
    )
