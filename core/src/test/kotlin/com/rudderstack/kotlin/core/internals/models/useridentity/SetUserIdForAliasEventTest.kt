package com.rudderstack.kotlin.core.internals.models.useridentity

import com.rudderstack.kotlin.core.Analytics
import com.rudderstack.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.kotlin.core.mockAnalytics
import io.mockk.coVerify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

private const val DEFAULT_ANONYMOUS_ID = "anonymousId"
private const val USER_ID = "userId"
private const val ALIAS_ID = "aliasId"

class SetUserIdForAliasEventTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mockAnalytics: Analytics = mockAnalytics(testScope, testDispatcher)

    @Test
    fun `when user id is set for alias event, then it should update the user id`() {
        val userIdentityState = provideUserIdentityInitialState()

        val result = SetUserIdForAliasEvent(ALIAS_ID)
            .reduce(userIdentityState)

        val expected = provideUserIdentityStateAfterUserIdIsSetForAliasEvent()
        assert(expected == result)
    }

    @Test
    fun `when user id is stored, then those values should be persisted in storage`() = runTest {
        val userIdentityState = provideUserIdentityStateAfterUserIdIsSetForAliasEvent()

        userIdentityState.storeUserIdTraitsAndExternalIds(mockAnalytics.configuration.storage)

        coVerify {
            mockAnalytics.configuration.storage.write(StorageKeys.USER_ID, ALIAS_ID)
        }
    }
}

private fun provideUserIdentityInitialState(): UserIdentity =
    UserIdentity(
        anonymousId = DEFAULT_ANONYMOUS_ID,
        userId = USER_ID,
        traits = emptyJsonObject,
        externalIds = emptyList()
    )

private fun provideUserIdentityStateAfterUserIdIsSetForAliasEvent(): UserIdentity =
    UserIdentity(
        anonymousId = DEFAULT_ANONYMOUS_ID,
        userId = ALIAS_ID,
        traits = emptyJsonObject,
        externalIds = emptyList()
    )
