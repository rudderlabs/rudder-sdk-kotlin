package com.rudderstack.sdk.kotlin.android.plugins

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.rudderstack.sdk.kotlin.android.utils.mockAnalytics
import com.rudderstack.sdk.kotlin.core.SourceConfigManager
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SourceConfigRefreshPluginTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mockAnalytics = mockAnalytics(testScope, testDispatcher)
    private lateinit var plugin: SourceConfigRefreshPlugin

    @MockK
    private lateinit var mockLifecycle: Lifecycle

    @MockK
    private lateinit var mockSourceConfigManager: SourceConfigManager

    @MockK
    private lateinit var mockLifecycleOwner: LifecycleOwner

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)
        every { mockAnalytics.sourceConfigManager } returns mockSourceConfigManager

        plugin = spyk(SourceConfigRefreshPlugin())
        every { plugin.getProcessLifecycle() } returns mockLifecycle
        plugin.setup(mockAnalytics)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `when the app moves to the foreground and then to the background, then the plugin starts and stops the refresh loop`() =
        runTest(testDispatcher) {
            advanceUntilIdle()
            verify { mockLifecycle.addObserver(plugin) }

            plugin.onStart(mockLifecycleOwner)
            plugin.onStop(mockLifecycleOwner)

            verifyOrder {
                mockSourceConfigManager.startPeriodicRefresh()
                mockSourceConfigManager.stopPeriodicRefresh()
            }
        }

    @Test
    fun `when teardown is called and shutdown cancels the scope before the cleanup runs, then the plugin still removes its observer and stops the loop`() =
        runTest(testDispatcher) {
            plugin.teardown()
            mockAnalytics.analyticsScope.cancel()
            advanceUntilIdle()

            verify { mockLifecycle.removeObserver(plugin) }
            verify { mockSourceConfigManager.stopPeriodicRefresh() }
        }
}
