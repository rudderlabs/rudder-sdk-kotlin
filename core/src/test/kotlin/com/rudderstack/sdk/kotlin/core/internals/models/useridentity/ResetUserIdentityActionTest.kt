package com.rudderstack.sdk.kotlin.core.internals.models.useridentity

import com.rudderstack.sdk.kotlin.core.ANONYMOUS_ID
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.provider.provideUserIdentityState
import com.rudderstack.sdk.kotlin.core.internals.models.reset.ResetEntries
import com.rudderstack.sdk.kotlin.core.internals.models.reset.ResetOptions
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
    fun `given some value is present in the user identity, when reset user identity action is performed with default options, then it should reset all values`() {
        val userIdentityState = provideUserIdentityStateAfterFirstIdentifyEventIsMade()

        val result = ResetUserIdentityAction(ResetOptions())
            .reduce(userIdentityState)

        val expected = provideUserIdentityState(anonymousId = NEW_ANONYMOUS_ID)
        assert(expected == result)
    }

    @Test
    fun `given empty user identity, when reset user identity action is performed with default options, then it should reset only anonymous ID`() {
        val userIdentityState = provideUserIdentityState()

        val result = ResetUserIdentityAction(ResetOptions())
            .reduce(userIdentityState)

        val expected = provideUserIdentityState(anonymousId = NEW_ANONYMOUS_ID)
        assert(expected == result)
    }

    @Test
    fun `given user identity with values, when reset user identity action is performed with only anonymousId enabled, then it should reset only anonymous ID`() {
        val userIdentityState = provideUserIdentityStateAfterFirstIdentifyEventIsMade()
        val resetOptions = ResetOptions(ResetEntries(anonymousId = true, userId = false, traits = false))

        val result = ResetUserIdentityAction(resetOptions)
            .reduce(userIdentityState)

        val expected = UserIdentity(
            anonymousId = NEW_ANONYMOUS_ID,
            userId = USER_ID,
            traits = TRAITS
        )
        assert(expected == result)
    }

    @Test
    fun `given user identity with values, when reset user identity action is performed with only userId enabled, then it should reset only user ID`() {
        val userIdentityState = provideUserIdentityStateAfterFirstIdentifyEventIsMade()
        val resetOptions = ResetOptions(ResetEntries(anonymousId = false, userId = true, traits = false))

        val result = ResetUserIdentityAction(resetOptions)
            .reduce(userIdentityState)

        val expected = UserIdentity(
            anonymousId = ANONYMOUS_ID,
            userId = "",
            traits = TRAITS
        )
        assert(expected == result)
    }

    @Test
    fun `given user identity with values, when reset user identity action is performed with only traits enabled, then it should reset only traits`() {
        val userIdentityState = provideUserIdentityStateAfterFirstIdentifyEventIsMade()
        val resetOptions = ResetOptions(ResetEntries(anonymousId = false, userId = false, traits = true))

        val result = ResetUserIdentityAction(resetOptions)
            .reduce(userIdentityState)

        val expected = UserIdentity(
            anonymousId = ANONYMOUS_ID,
            userId = USER_ID,
            traits = com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
        )
        assert(expected == result)
    }

    @Test
    fun `given user identity with values, when reset user identity action is performed with all flags disabled, then nothing should be reset`() {
        val userIdentityState = provideUserIdentityStateAfterFirstIdentifyEventIsMade()
        val resetOptions = ResetOptions(ResetEntries(anonymousId = false, userId = false, traits = false))

        val result = ResetUserIdentityAction(resetOptions)
            .reduce(userIdentityState)

        val expected = provideUserIdentityStateAfterFirstIdentifyEventIsMade()
        assert(expected == result)
    }

    @Test
    fun `when reset user identity action is performed with default options, then it should reset all values in storage`() =
        runTest {
            val userIdentityState = provideUserIdentityState(anonymousId = NEW_ANONYMOUS_ID)

            userIdentityState.resetUserIdentity(storage = mockAnalytics.storage, options = ResetOptions())
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify {
                mockAnalytics.storage.write(StorageKeys.ANONYMOUS_ID, NEW_ANONYMOUS_ID)
                mockAnalytics.storage.remove(StorageKeys.USER_ID)
                mockAnalytics.storage.remove(StorageKeys.TRAITS)
            }
        }

    @Test
    fun `when reset user identity action is performed with only anonymousId enabled, then it should only update anonymous ID in storage`() =
        runTest {
            val userIdentityState = provideUserIdentityState(anonymousId = NEW_ANONYMOUS_ID)
            val resetOptions = ResetOptions(ResetEntries(anonymousId = true, userId = false, traits = false))

            userIdentityState.resetUserIdentity(storage = mockAnalytics.storage, options = resetOptions)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify {
                mockAnalytics.storage.write(StorageKeys.ANONYMOUS_ID, NEW_ANONYMOUS_ID)
            }
        }

    @Test
    fun `when reset user identity action is performed with only userId enabled, then it should only remove user ID from storage`() =
        runTest {
            val userIdentityState = provideUserIdentityState(anonymousId = NEW_ANONYMOUS_ID)
            val resetOptions = ResetOptions(ResetEntries(anonymousId = false, userId = true, traits = false))

            userIdentityState.resetUserIdentity(storage = mockAnalytics.storage, options = resetOptions)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify {
                mockAnalytics.storage.remove(StorageKeys.USER_ID)
            }
        }

    @Test
    fun `when reset user identity action is performed with only traits enabled, then it should only remove traits from storage`() =
        runTest {
            val userIdentityState = provideUserIdentityState(anonymousId = NEW_ANONYMOUS_ID)
            val resetOptions = ResetOptions(ResetEntries(anonymousId = false, userId = false, traits = true))

            userIdentityState.resetUserIdentity(storage = mockAnalytics.storage, options = resetOptions)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify {
                mockAnalytics.storage.remove(StorageKeys.TRAITS)
            }
        }

}

private fun provideUserIdentityStateAfterFirstIdentifyEventIsMade(): UserIdentity =
    UserIdentity(
        anonymousId = ANONYMOUS_ID,
        userId = USER_ID,
        traits = TRAITS,
    )
