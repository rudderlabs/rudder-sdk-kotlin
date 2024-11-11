package com.rudderstack.kotlin.sdk.internals.models.useridentity

import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.models.ExternalId
import com.rudderstack.kotlin.sdk.internals.models.emptyJsonObject
import com.rudderstack.kotlin.sdk.internals.storage.StorageKeys
import com.rudderstack.kotlin.sdk.internals.utils.LenientJson
import com.rudderstack.kotlin.sdk.internals.utils.empty
import com.rudderstack.kotlin.sdk.internals.utils.mergeWithHigherPriorityTo
import com.rudderstack.kotlin.sdk.mockAnalytics
import io.mockk.coVerify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Test

private const val DEFAULT_ANONYMOUS_ID = "anonymousId"
private const val USER_1 = "user1"
private const val USER_2 = "user2"
private val TRAITS_1 = buildJsonObject { put("key-1", "value-1") }
private val TRAITS_2 = buildJsonObject { put("key-2", "value-2") }
private val EXTERNAL_IDS_1 = listOf(ExternalId("externalIdType1", "externalIdValue1"))
private val EXTERNAL_IDS_2 = listOf(ExternalId("externalIdType2", "externalIdValue2"))
private val TRAITS_1_OVERLAP = buildJsonObject { put("key-1", "value-2") }
private val EXTERNAL_IDS_1_OVERLAP = listOf(ExternalId("externalIdType1", "externalIdValue2"))

class SetUserIdTraitsAndExternalIdsActionTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mockAnalytics: Analytics = mockAnalytics(testScope, testDispatcher)

    @Test
    fun `given identify event is made for the first time, when identify event is made, then it should update the user id, traits and external ids`() =
        runTest {
            val userIdentityState = provideUserIdentityInitialState()

            val result = SetUserIdTraitsAndExternalIdsAction(USER_1, TRAITS_1, EXTERNAL_IDS_1, mockAnalytics)
                .reduce(userIdentityState)
            testDispatcher.scheduler.advanceUntilIdle()

            val expected = provideUserIdentityStateAfterFirstIdentifyEventIsMade()
            assert(expected == result)
            verifyUserIdChangedBehaviour()
        }

    @Test
    fun `given two identify events are made with same user id but without any overlap of values, when identify event is made, then it should merge all the values`() =
        runTest {
            val userIdentityState = provideUserIdentityStateAfterFirstIdentifyEventIsMade()

            val result = SetUserIdTraitsAndExternalIdsAction(USER_1, TRAITS_2, EXTERNAL_IDS_2, mockAnalytics)
                .reduce(userIdentityState)
            testDispatcher.scheduler.advanceUntilIdle()

            val expected = provideUserIdentityWithMergedTraitsAndExternalIds()
            assert(expected == result)
        }

    @Test
    fun `given two identify events are made with same user id but with overlap of values, when identify event is made, then it should override the values`() =
        runTest {
            val userIdentityState = provideUserIdentityStateAfterFirstIdentifyEventIsMade()

            val result = SetUserIdTraitsAndExternalIdsAction(USER_1, TRAITS_1_OVERLAP, EXTERNAL_IDS_1_OVERLAP, mockAnalytics)
                .reduce(userIdentityState)
            testDispatcher.scheduler.advanceUntilIdle()

            val expected = provideUserIdentityWithOverriddenTraitsAndExternalIds()
            assert(expected == result)
        }

    @Test
    fun `given two identify events with different user id is made, when identify event is made, then it should update the user id, traits and external ids`() =
        runTest {
            val userIdentityState = provideUserIdentityStateAfterFirstIdentifyEventIsMade()

            val result = SetUserIdTraitsAndExternalIdsAction(USER_2, TRAITS_2, EXTERNAL_IDS_2, mockAnalytics)
                .reduce(userIdentityState)
            testDispatcher.scheduler.advanceUntilIdle()

            val expected = provideUserIdentityAfterSecondIdentifyEventIsMade()
            assert(expected == result)
            verifyUserIdChangedBehaviour()
        }

    @Test
    fun `when values are stored, then those values should be persisted in storage`() = runTest {
        val userIdentityState = provideUserIdentityStateAfterFirstIdentifyEventIsMade()

        userIdentityState.storeUserIdTraitsAndExternalIds(mockAnalytics.configuration.storage)

        coVerify {
            mockAnalytics.configuration.storage.write(StorageKeys.USER_ID, USER_1)
            mockAnalytics.configuration.storage.write(StorageKeys.TRAITS, LenientJson.encodeToString(TRAITS_1))
            mockAnalytics.configuration.storage.write(StorageKeys.EXTERNAL_IDS, LenientJson.encodeToString(EXTERNAL_IDS_1))
        }
    }

    private fun verifyUserIdChangedBehaviour() {
        coVerify { mockAnalytics.configuration.storage.remove(StorageKeys.USER_ID) }
        coVerify { mockAnalytics.configuration.storage.remove(StorageKeys.TRAITS) }
        coVerify { mockAnalytics.configuration.storage.remove(StorageKeys.EXTERNAL_IDS) }
    }
}

private fun provideUserIdentityInitialState(): UserIdentity =
    UserIdentity(
        anonymousId = DEFAULT_ANONYMOUS_ID,
        userId = String.empty(),
        traits = emptyJsonObject,
        externalIds = emptyList()
    )

private fun provideUserIdentityStateAfterFirstIdentifyEventIsMade(): UserIdentity =
    UserIdentity(
        anonymousId = DEFAULT_ANONYMOUS_ID,
        userId = USER_1,
        traits = TRAITS_1,
        externalIds = EXTERNAL_IDS_1
    )

private fun provideUserIdentityWithMergedTraitsAndExternalIds(): UserIdentity = UserIdentity(
    anonymousId = DEFAULT_ANONYMOUS_ID,
    userId = USER_1,
    traits = TRAITS_1 mergeWithHigherPriorityTo TRAITS_2,
    externalIds = EXTERNAL_IDS_1 mergeWithHigherPriorityTo EXTERNAL_IDS_2,
)

private fun provideUserIdentityWithOverriddenTraitsAndExternalIds(): UserIdentity = UserIdentity(
    anonymousId = DEFAULT_ANONYMOUS_ID,
    userId = USER_1,
    traits = TRAITS_1_OVERLAP,
    externalIds = EXTERNAL_IDS_1_OVERLAP
)

private fun provideUserIdentityAfterSecondIdentifyEventIsMade(): UserIdentity =
    UserIdentity(
        anonymousId = DEFAULT_ANONYMOUS_ID,
        userId = USER_2,
        traits = TRAITS_2,
        externalIds = EXTERNAL_IDS_2
)
