package com.rudderstack.sdk.kotlin.core.internals.pipeline

import com.rudderstack.sdk.kotlin.core.Configuration
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.storage.inmemory.UNLIMITED_EVENTS
import com.rudderstack.sdk.kotlin.core.server.DropListener
import com.rudderstack.sdk.kotlin.core.server.ServerConfiguration
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private const val WRITE_KEY = "<write-key>"
private const val DATA_PLANE_URL = "https://test.dataplane.com"
private const val MAX_RETRIES = 2
private const val MAX_QUEUED_EVENTS = 500

class PipelineRulesTest {

    @Test
    fun `given a plain configuration, when the rules are built for each platform, then the client rules are set and only mobile uses the source config`() {
        val configuration = Configuration(writeKey = WRITE_KEY, dataPlaneUrl = DATA_PLANE_URL)

        val mobileRules = providePipelineRules(PlatformType.Mobile, configuration)
        val serverPlatformRules = providePipelineRules(PlatformType.Server, configuration)

        val expectedClientRules = PipelineRules(
            usesSourceConfig = true,
            maxRetries = null,
            maxQueuedEvents = UNLIMITED_EVENTS,
            onSourceNotFound = SourceNotFoundAction.STOP_UPLOADS,
            splitsBatchByAnonymousId = true,
            sendsAnonymousIdHeader = true,
            omitsBlankAnonymousId = false,
            continuesAfterEventFailure = false,
            usesFileDispatcherForRollover = true,
            dropListener = null,
        )
        assertEquals(expectedClientRules, mobileRules)
        assertEquals(expectedClientRules.copy(usesSourceConfig = false), serverPlatformRules)
    }

    @Test
    fun `given a server configuration, when the rules are built, then the server rules are set with the limits and the listener of the configuration`() {
        val dropListener: DropListener = mockk()
        val configuration = ServerConfiguration(
            writeKey = WRITE_KEY,
            dataPlaneUrl = DATA_PLANE_URL,
            maxQueuedEvents = MAX_QUEUED_EVENTS,
            maxRetries = MAX_RETRIES,
            dropListener = dropListener,
        )

        val rules = providePipelineRules(PlatformType.Server, configuration)

        val expectedServerRules = PipelineRules(
            usesSourceConfig = false,
            maxRetries = MAX_RETRIES,
            maxQueuedEvents = MAX_QUEUED_EVENTS,
            onSourceNotFound = SourceNotFoundAction.DROP_BATCH,
            splitsBatchByAnonymousId = false,
            sendsAnonymousIdHeader = false,
            omitsBlankAnonymousId = true,
            continuesAfterEventFailure = true,
            usesFileDispatcherForRollover = false,
            dropListener = dropListener,
        )
        assertEquals(expectedServerRules, rules)
    }
}
