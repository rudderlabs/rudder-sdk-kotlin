package com.rudderstack.core.internals.policies

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

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
