package com.rudderstack.core

import com.rudderstack.core.internals.network.HttpClient
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ServerConfigManagerTest {

    @MockK
    private lateinit var mockGetHttpClient: HttpClient

    @MockK // TODO("Mock coroutine dispatcher in Analytics.")
    private lateinit var mockAnalytics: Analytics

    private lateinit var serverConfigManager: ServerConfigManager

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        serverConfigManager = ServerConfigManager(
            analytics = mockAnalytics,
            httpClientFactory = mockGetHttpClient
        )
    }

    @Test
    fun `test fetchSourceConfig`() = runTest {
        // TODO("Implement test")
        serverConfigManager.fetchSourceConfig()
    }
}
