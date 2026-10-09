package com.rudderstack.sdk.kotlin.core.server

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.StorageType
import com.rudderstack.sdk.kotlin.core.internals.pipeline.PipelineRules
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private const val WRITE_KEY = "<write-key>"
private const val DATA_PLANE_URL = "https://test.dataplane.com"

class ServerConfigurationTest {

    @Test
    fun `given only the write key and the data plane URL, when the configuration is created, then the server defaults are set`() {
        val configuration = ServerConfiguration(writeKey = WRITE_KEY, dataPlaneUrl = DATA_PLANE_URL)

        assertTrue(configuration.gzipEnabled)
        assertEquals(StorageType.IN_MEMORY, configuration.storageType)
        assertEquals(20_000, configuration.maxQueuedEvents)
        assertEquals(3, configuration.maxRetries)
        assertNull(configuration.dropListener)
    }

    @Test
    fun `given limits outside the valid range, when the configuration is created, then the default limits are set`() {
        val configuration = ServerConfiguration(
            writeKey = WRITE_KEY,
            dataPlaneUrl = DATA_PLANE_URL,
            maxQueuedEvents = 0,
            maxRetries = -1,
        )
        val configurationAboveRetryLimit = ServerConfiguration(
            writeKey = WRITE_KEY,
            dataPlaneUrl = DATA_PLANE_URL,
            maxRetries = ServerConfiguration.MAX_RETRIES_LIMIT + 1,
        )

        assertEquals(ServerConfiguration.DEFAULT_MAX_QUEUED_EVENTS, configuration.maxQueuedEvents)
        assertEquals(ServerConfiguration.DEFAULT_MAX_RETRIES, configuration.maxRetries)
        assertEquals(ServerConfiguration.DEFAULT_MAX_RETRIES, configurationAboveRetryLimit.maxRetries)
    }

    @Test
    fun `given a drop listener that throws, when a drop is reported, then the listener gets the report and the exception is logged`() {
        val exception = IllegalStateException("listener failure")
        val dropListener: DropListener = mockk()
        every { dropListener.onDrop(any(), any()) } throws exception
        val analytics: Analytics = mockk(relaxed = true)
        every { analytics.pipelineRules } returns PipelineRules(usesSourceConfig = false, dropListener = dropListener)

        analytics.reportDrop(DropReason.QUEUE_FULL, eventCount = 1)

        verify(exactly = 1) { dropListener.onDrop(DropReason.QUEUE_FULL, 1) }
        verify(exactly = 1) { analytics.logger.error(any(), exception) }
    }
}
