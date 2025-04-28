package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.AnalyticsConfiguration
import com.rudderstack.sdk.kotlin.core.Configuration
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.provideAnalyticsConfiguration
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AnalyticsUtilTest {

    @MockK
    private lateinit var mockStorage: Storage

    @MockK
    private lateinit var mockAnalyticsConfiguration: AnalyticsConfiguration

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val configuration = provideConfiguration()
    private lateinit var mockAnalyticsJob: CompletableJob
    private lateinit var mockAnalytics: Analytics

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        // This is needed to trigger analyticsJob.invokeOnCompletion()
        mockAnalyticsJob = SupervisorJob()

        // Mock Analytics Configuration
        mockkStatic(::provideAnalyticsConfiguration)
        every { provideAnalyticsConfiguration(any()) } returns mockAnalyticsConfiguration
        mockAnalyticsConfiguration.apply {
            every { analyticsScope } returns testScope
            every { analyticsDispatcher } returns testDispatcher
            every { storageDispatcher } returns testDispatcher
            every { networkDispatcher } returns testDispatcher

            every { storage } returns mockStorage
            every { analyticsJob } returns mockAnalyticsJob
        }

        mockAnalytics = spyk(Analytics(configuration = configuration))
    }

    @Test
    fun `when handleInvalidWriteKey is called, then shutdown analytics and set isInvalidWriteKey to true`() {
        mockAnalytics.handleInvalidWriteKey()

        verify(exactly = 1) {
            mockAnalyticsConfiguration.isInvalidWriteKey = true
            mockAnalytics.shutdown()
        }
        verifyOrder {
            mockAnalyticsConfiguration.isInvalidWriteKey = true
            mockAnalytics.shutdown()
        }
    }
}

private fun provideConfiguration() =
    Configuration(
        writeKey = "<writeKey>",
        dataPlaneUrl = "<data_plane_url>",
    )
