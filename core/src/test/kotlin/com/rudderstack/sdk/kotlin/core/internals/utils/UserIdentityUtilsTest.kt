package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.internals.models.provider.provideUserIdentityState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private const val EMPTY_PREVIOUS_ID = ""
private const val USER_ID = "user-id-1"
private const val ANONYMOUS_ID = "anonymous-id-1"
private const val PREVIOUS_ID = "previous-id-1"

class UserIdentityUtilsTest {

    @Test
    fun `given user id is present, when empty previous id is passed, then user id is returned`() {
        val userIdentity = provideUserIdentityState(userId = USER_ID)

        val result = userIdentity.resolvePreferredPreviousId(EMPTY_PREVIOUS_ID)

        assertEquals(USER_ID, result)
    }

    @Test
    fun `given user id is not present, when empty previous id is passed, then anonymous id is returned`() {
        val userIdentity = provideUserIdentityState(anonymousId = ANONYMOUS_ID, userId = String.empty())

        val result = userIdentity.resolvePreferredPreviousId(EMPTY_PREVIOUS_ID)

        assertEquals(ANONYMOUS_ID, result)
    }

    @Test
    fun `given none of the ids are empty, when previous id is passed, then previous id is returned`() {
        val userIdentity = provideUserIdentityState(anonymousId = ANONYMOUS_ID, userId = USER_ID)

        val result = userIdentity.resolvePreferredPreviousId(PREVIOUS_ID)

        assertEquals(PREVIOUS_ID, result)
    }
}
