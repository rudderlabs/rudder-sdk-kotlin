package com.rudderstack.sdk.kotlin.core.internals.utils

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import kotlinx.coroutines.channels.Channel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue

class CoroutineUtilsTest {

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun `given channel is opened initially, when an attempt is made to create a new channel, it returns the same channel`() {
        val channel = Channel<String>()

        val result = channel.createNewIfClosed()

        assertEquals(channel, result)
        assertFalse(result.isClosedForSend)
        assertFalse(result.isClosedForReceive)
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun `given channel is closed initially, when an attempt is made to create a new channel, it returns a new channel`() {
        val channel = Channel<String>()
        channel.close()

        val result = channel.createNewIfClosed()

        assertNotEquals(channel, result)
        assertFalse(result.isClosedForSend)
        assertFalse(result.isClosedForReceive)
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun `when new channel is created with unlimited capacity, it should be ready to send and receive`() {
        val result: Channel<String> = createUnlimitedCapacityChannel()

        assertNotNull(result)
        assertFalse(result.isClosedForSend)
        assertFalse(result.isClosedForReceive)
    }

    @Test
    fun `given job is null, when an attempt is made to create a new job, it should create a new job`() = runTest {
        val job: Job? = null
        var createJobCalled = false

        val result = job.createIfInactive {
            createJobCalled = true
            launch {}
        }

        assertNotNull(result)
        assertTrue(createJobCalled)
        assertNotEquals(job, result)
    }

    @Test
    fun `given job is inactive, when an attempt is made to create a new job, it should create a new job`() = runTest {
        val originalJob = launch {}
        // Make the job inactive
        originalJob.cancelAndJoin()
        var createJobCalled = false

        val result = originalJob.createIfInactive {
            createJobCalled = true
            launch {}
        }

        assertNotNull(result)
        assert(createJobCalled)
        assertFalse(originalJob.isActive)
        assertTrue(result.isActive)
        assertNotEquals(originalJob, result)
    }

    //
    @Test
    fun `given job is active, when an attempt is made to create a new job, it should return the same job`() = runTest {
        val originalJob = launch {}
        var createJobCalled = false

        val result = originalJob.createIfInactive {
            createJobCalled = true
            launch {}
        }

        assertEquals(originalJob, result)
        assertFalse(createJobCalled)
    }
}

