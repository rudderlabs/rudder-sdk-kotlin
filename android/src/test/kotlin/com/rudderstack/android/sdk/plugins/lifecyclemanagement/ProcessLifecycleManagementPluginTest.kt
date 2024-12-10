package com.rudderstack.android.sdk.plugins.lifecyclemanagement

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.rudderstack.android.sdk.plugins.lifecyclemanagment.ProcessLifecycleManagementPlugin
import com.rudderstack.android.sdk.plugins.lifecyclemanagment.ProcessLifecycleObserver
import com.rudderstack.android.sdk.utils.mockAnalytics
import com.rudderstack.kotlin.core.Analytics
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProcessLifecycleManagementPluginTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var plugin: ProcessLifecycleManagementPlugin
    private lateinit var mockAnalytics: Analytics

    @MockK
    private lateinit var mockLifecycle: Lifecycle

    @MockK
    private lateinit var mockObserver: ProcessLifecycleObserver

    @MockK
    private lateinit var mockLifecycleOwner: LifecycleOwner

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)

        plugin = spyk(ProcessLifecycleManagementPlugin())

        // Mock the ProcessLifecycleOwner lifecycle
        every { plugin.getProcessLifecycle() } returns mockLifecycle

        mockAnalytics = mockAnalytics(testScope, testDispatcher)

        plugin.setup(mockAnalytics)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `when setup called, then it should register process lifecycle observer`() = runTest {
        advanceUntilIdle()
        verify { mockLifecycle.addObserver(plugin) }
    }

    @Test
    fun `when teardown called, then it should unregister process lifecycle observer`() = runTest {
        plugin.addObserver(mockObserver)

        plugin.teardown()

        advanceUntilIdle()
        verify { mockLifecycle.removeObserver(plugin) }
    }

    @Test
    fun `when addObserver called, then it should add an observer to the list`() {
        plugin.addObserver(mockObserver)

        assert(plugin.processObservers.contains(mockObserver))
    }

    @Test
    fun `when removeObserver called, then it should remove an observer from the list`() {
        plugin.addObserver(mockObserver)
        plugin.removeObserver(mockObserver)

        assert(!plugin.processObservers.contains(mockObserver))
    }

    @Test
    fun `given an observer, when onCreate called, then it should notify all observers`() {
        plugin.addObserver(mockObserver)

        plugin.onCreate(mockLifecycleOwner)

        verify { mockObserver.onCreate(mockLifecycleOwner) }
    }

    @Test
    fun `given an observer, when onStart called, then it should notify all observers`() {
        plugin.addObserver(mockObserver)

        plugin.onStart(mockLifecycleOwner)

        verify { mockObserver.onStart(mockLifecycleOwner) }
    }

    @Test
    fun `given an observer, when onResume called, then it should notify all observers`() {
        plugin.addObserver(mockObserver)

        plugin.onResume(mockLifecycleOwner)

        verify { mockObserver.onResume(mockLifecycleOwner) }
    }

    @Test
    fun `given an observer, when onPause called, then it should notify all observers`() {
        plugin.addObserver(mockObserver)

        plugin.onPause(mockLifecycleOwner)

        verify { mockObserver.onPause(mockLifecycleOwner) }
    }

    @Test
    fun `given an observer, when onStop called, then it should notify all observers`() {
        plugin.addObserver(mockObserver)

        plugin.onStop(mockLifecycleOwner)

        verify { mockObserver.onStop(mockLifecycleOwner) }
    }

    @Test
    fun `given an observer, when onDestroy called, then it should notify all observers`() {
        plugin.addObserver(mockObserver)

        plugin.onDestroy(mockLifecycleOwner)

        verify { mockObserver.onDestroy(mockLifecycleOwner) }
    }
}
