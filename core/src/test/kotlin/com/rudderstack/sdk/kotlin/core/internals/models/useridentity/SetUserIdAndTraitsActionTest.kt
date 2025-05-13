package com.rudderstack.sdk.kotlin.core.internals.models.useridentity

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.internals.utils.mergeWithHigherPriorityTo
import com.rudderstack.sdk.kotlin.core.mockAnalytics
import io.mockk.coVerify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test

private const val DEFAULT_ANONYMOUS_ID = "anonymousId"
private const val USER_1 = "user1"
private const val USER_2 = "user2"
private val TRAITS_1 = buildJsonObject { put("key-1", "value-1") }
private val TRAITS_2 = buildJsonObject { put("key-2", "value-2") }
private val TRAITS_1_OVERLAP = buildJsonObject { put("key-1", "value-2") }

class SetUserIdAndTraitsActionTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mockAnalytics: Analytics = mockAnalytics(testScope, testDispatcher)

    @Test
    fun `given identify event is made for the first time, when identify event is made, then it should update all the values`() =
        runTest {
            val userIdentityState = provideUserIdentityInitialState()

            val result = SetUserIdAndTraitsAction(USER_1, TRAITS_1)
                .reduce(userIdentityState)
            testDispatcher.scheduler.advanceUntilIdle()

            val expected = provideUserIdentityStateAfterFirstIdentifyEventIsMade()
            assert(expected == result)
        }

    @Test
    fun `given two identify events are made with same user id but without any overlap of values, when identify event is made, then it should merge all the values`() =
        runTest {
            val userIdentityState = provideUserIdentityStateAfterFirstIdentifyEventIsMade()

            val result = SetUserIdAndTraitsAction(USER_1, TRAITS_2)
                .reduce(userIdentityState)
            testDispatcher.scheduler.advanceUntilIdle()

            val expected = provideUserIdentityWithMergedTraitsAndExternalIds()
            assert(expected == result)
        }

    @Test
    fun `given two identify events are made with same user id but with overlap of values, when identify event is made, then it should override the values`() =
        runTest {
            val userIdentityState = provideUserIdentityStateAfterFirstIdentifyEventIsMade()

            val result = SetUserIdAndTraitsAction(USER_1, TRAITS_1_OVERLAP)
                .reduce(userIdentityState)
            testDispatcher.scheduler.advanceUntilIdle()

            val expected = provideUserIdentityWithOverriddenTraitsAndExternalIds()
            assert(expected == result)
        }

    @Test
    fun `given two identify events with different user id is made, when identify event is made, then it should update all the values`() =
        runTest {
            val userIdentityState = provideUserIdentityStateAfterFirstIdentifyEventIsMade()

            val result = SetUserIdAndTraitsAction(USER_2, TRAITS_2)
                .reduce(userIdentityState)
            testDispatcher.scheduler.advanceUntilIdle()

            val expected = provideUserIdentityAfterSecondIdentifyEventIsMade()
            assert(expected == result)
        }

    @Test
    fun `when values are stored, then it should be persisted in storage`() = runTest {
        val userIdentityState = provideUserIdentityStateAfterFirstIdentifyEventIsMade()

        userIdentityState.storeUserIdAndTraits(mockAnalytics.storage)

        coVerify {
            mockAnalytics.storage.write(StorageKeys.USER_ID, USER_1)
            mockAnalytics.storage.write(StorageKeys.TRAITS, LenientJson.encodeToString(TRAITS_1))
        }
    }
}

private fun provideUserIdentityInitialState(): UserIdentity =
    UserIdentity(
        anonymousId = DEFAULT_ANONYMOUS_ID,
        userId = String.empty(),
        traits = emptyJsonObject,
    )

private fun provideUserIdentityStateAfterFirstIdentifyEventIsMade(): UserIdentity =
    UserIdentity(
        anonymousId = DEFAULT_ANONYMOUS_ID,
        userId = USER_1,
        traits = TRAITS_1,
    )

private fun provideUserIdentityWithMergedTraitsAndExternalIds(): UserIdentity = UserIdentity(
    anonymousId = DEFAULT_ANONYMOUS_ID,
    userId = USER_1,
    traits = TRAITS_1 mergeWithHigherPriorityTo TRAITS_2,
)

private fun provideUserIdentityWithOverriddenTraitsAndExternalIds(): UserIdentity = UserIdentity(
    anonymousId = DEFAULT_ANONYMOUS_ID,
    userId = USER_1,
    traits = TRAITS_1_OVERLAP,
)

private fun provideUserIdentityAfterSecondIdentifyEventIsMade(): UserIdentity =
    UserIdentity(
        anonymousId = DEFAULT_ANONYMOUS_ID,
        userId = USER_2,
        traits = TRAITS_2,
)
