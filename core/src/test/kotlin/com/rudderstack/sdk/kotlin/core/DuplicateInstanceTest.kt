package com.rudderstack.sdk.kotlin.core

import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.storage.LibraryVersion
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val DUPLICATE_ERROR = "An instance for this write key already runs in this process"

class DuplicateInstanceTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val instances = mutableListOf<MockedInstance>()

    @BeforeEach
    fun setup() {
        mockkObject(LoggerAnalytics)
        mockkStatic(::provideAnalyticsConfiguration)
        every { provideAnalyticsConfiguration(any(), any()) } answers { MockedInstance().also { instances += it }.configuration }
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given a running instance for a write key, when a second instance is created for it, then only the first stores events`() {
        val writeKey = uniqueWriteKey()
        val first = Analytics(fileConfiguration(writeKey))

        val second = Analytics(fileConfiguration(writeKey))
        second.track("second-event")
        second.shutdown()
        val third = Analytics(fileConfiguration(writeKey))
        first.track("first-event")
        processEvents(first)

        assertFalse(first.isAnalyticsShutdown)
        assertTrue(second.isAnalyticsShutdown)
        assertTrue(third.isAnalyticsShutdown, "A refused instance must not free the write key")
        assertEquals(0, instances[0].duplicateErrors())
        assertEquals(1, instances[1].duplicateErrors())
        coVerify(exactly = 0) { instances[1].storage.write(any(), any<String>()) }
        coVerify(exactly = 1) { instances[0].storage.write(StorageKeys.EVENT, match<String> { it.contains("first-event") }) }
    }

    @Test
    fun `given the running instance shuts down, when a new instance is created for its write key, then the new instance starts`() {
        val writeKey = uniqueWriteKey()
        Analytics(fileConfiguration(writeKey)).shutdown()

        val next = Analytics(fileConfiguration(writeKey))

        assertFalse(next.isAnalyticsShutdown)
        assertEquals(0, instances[1].duplicateErrors())
    }

    @Test
    fun `given instances that share no store on disk, when they are created, then all of them start`() {
        val sharedKey = uniqueWriteKey()

        val differentKeys = listOf(Analytics(fileConfiguration(uniqueWriteKey())), Analytics(fileConfiguration(uniqueWriteKey())))
        val inMemory = List(2) { Analytics(Configuration(writeKey = sharedKey, dataPlaneUrl = "<data_plane_url>")) }

        (differentKeys + inMemory).forEach { assertFalse(it.isAnalyticsShutdown) }
        instances.forEach { assertEquals(0, it.duplicateErrors()) }
    }

    private fun processEvents(analytics: Analytics) {
        testDispatcher.scheduler.runCurrent()
        analytics.sourceConfigState.dispatch(
            SourceConfig.UpdateAction(SourceConfig(source = SourceConfig.initialState().source.copy(isSourceEnabled = false)))
        )
    }

    private fun fileConfiguration(writeKey: String) =
        Configuration(writeKey = writeKey, dataPlaneUrl = "<data_plane_url>", storageType = StorageType.FILE)

    // Fully qualified: this package declares a test constant named UUID, and other tests mock generateUUID().
    private fun uniqueWriteKey() = "write-key-${java.util.UUID.randomUUID()}"

    private inner class MockedInstance {
        val storage: Storage = mockk(relaxed = true)
        val logger: Logger = mockk(relaxed = true)
        val configuration: AnalyticsConfiguration = mockk(relaxed = true)

        init {
            every { storage.readString(StorageKeys.ANONYMOUS_ID, defaultVal = any()) } returns ANONYMOUS_ID
            every { storage.getLibraryVersion() } returns object : LibraryVersion {
                override fun getLibraryName(): String = "com.rudderstack.kotlin.sdk"
                override fun getVersionName(): String = "1.0.0"
            }
            configuration.apply {
                every { logger } returns this@MockedInstance.logger
                every { storage } returns this@MockedInstance.storage
                every { analyticsScope } returns testScope
                every { analyticsDispatcher } returns testDispatcher
                every { fileStorageDispatcher } returns testDispatcher
                every { keyValueStorageDispatcher } returns testDispatcher
                every { networkDispatcher } returns testDispatcher
                every { analyticsJob } returns SupervisorJob()
            }
        }

        fun duplicateErrors(): Int {
            val messages = mutableListOf<String>()
            verify(atLeast = 0) { logger.error(capture(messages), any()) }
            return messages.count { it.contains(DUPLICATE_ERROR) }
        }
    }
}
