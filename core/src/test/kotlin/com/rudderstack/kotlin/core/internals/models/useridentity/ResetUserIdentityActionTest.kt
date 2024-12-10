package com.rudderstack.kotlin.core.internals.models.useridentity

import com.rudderstack.kotlin.core.ANONYMOUS_ID
import com.rudderstack.kotlin.core.Analytics
import com.rudderstack.kotlin.core.internals.models.ExternalId
import com.rudderstack.kotlin.core.internals.models.provider.provideUserIdentityState
import com.rudderstack.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.kotlin.core.internals.utils.generateUUID
import com.rudderstack.kotlin.core.mockAnalytics
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Before
import org.junit.Test

private const val NEW_ANONYMOUS_ID = "newAnonymousId"
private const val USER_ID = "user1"
private val TRAITS = buildJsonObject { put("key-1", "value-1") }
private val EXTERNAL_IDS = listOf(ExternalId("externalIdType1", "externalIdValue1"))

class ResetUserIdentityActionTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mockAnalytics: Analytics = mockAnalytics(testScope, testDispatcher)

    @Before
    fun setup() {
        mockkStatic(::generateUUID)
        every { generateUUID() } returns NEW_ANONYMOUS_ID
    }

    @Test
    fun `given some value is present in the user identity and clearAnonymousId is true, when reset user identity action is performed, then it should reset all values`() {
        val userIdentityState = provideUserIdentityStateAfterFirstIdentifyEventIsMade()

        val result = ResetUserIdentityAction(clearAnonymousId = true)
            .reduce(userIdentityState)

        val expected = provideUserIdentityState(anonymousId = NEW_ANONYMOUS_ID)
        assert(expected == result)
    }

    @Test
    fun `given some value is present in the user identity and clearAnonymousId is false, when reset user identity action is performed, then it should reset all values except anonymous ID`() {
        val userIdentityState = provideUserIdentityStateAfterFirstIdentifyEventIsMade()

        val result = ResetUserIdentityAction(clearAnonymousId = false)
            .reduce(userIdentityState)

        val expected = provideUserIdentityState(anonymousId = ANONYMOUS_ID)
        assert(expected == result)
    }

    @Test
    fun `given empty user identity and clearAnonymousId is true, when reset user identity action is performed, then it should reset only anonymous ID`() {
        val userIdentityState = provideUserIdentityState()

        val result = ResetUserIdentityAction(clearAnonymousId = true)
            .reduce(userIdentityState)

        val expected = provideUserIdentityState(anonymousId = NEW_ANONYMOUS_ID)
        assert(expected == result)
    }

    @Test
    fun `given clearAnonymousId is true, when reset user identity action is performed, then it should reset all values`() =
        runTest {
            val userIdentityState = provideUserIdentityState(anonymousId = NEW_ANONYMOUS_ID)

            userIdentityState.resetUserIdentity(clearAnonymousId = true, storage = mockAnalytics.configuration.storage)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) {
                mockAnalytics.configuration.storage.apply {
                    write(StorageKeys.ANONYMOUS_ID, NEW_ANONYMOUS_ID)
                    remove(StorageKeys.USER_ID)
                    remove(StorageKeys.TRAITS)
                    remove(StorageKeys.EXTERNAL_IDS)
                }
            }
        }

    @Test
    fun `given clearAnonymousId is false, when reset user identity action is performed, then it should reset all values except anonymous ID`() =
        runTest {
            val userIdentityState = provideUserIdentityState(anonymousId = ANONYMOUS_ID)
            val storage = mockAnalytics.configuration.storage

            userIdentityState.resetUserIdentity(clearAnonymousId = false, storage = storage)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 0) {
                storage.write(StorageKeys.ANONYMOUS_ID, ANONYMOUS_ID)
            }
            coVerify(exactly = 1) {
                storage.apply {
                    remove(StorageKeys.USER_ID)
                    remove(StorageKeys.TRAITS)
                    remove(StorageKeys.EXTERNAL_IDS)
                }
            }
        }
}

private fun provideUserIdentityStateAfterFirstIdentifyEventIsMade(): UserIdentity =
    UserIdentity(
        anonymousId = ANONYMOUS_ID,
        userId = USER_ID,
        traits = TRAITS,
        externalIds = EXTERNAL_IDS
    )
