package com.rudderstack.sdk.kotlin.core.internals.policies

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StartupFlushPolicyTest {

    @Test
    fun `given this policy is enabled, when shouldFlush is called, then it should return true only once`() {
        val startupFlushPolicy = StartupFlushPolicy()

        // Should only flush the first time requested!
        assertTrue(startupFlushPolicy.shouldFlush())

        // Should now not flush any more!
        assertFalse(startupFlushPolicy.shouldFlush())
    }
}
