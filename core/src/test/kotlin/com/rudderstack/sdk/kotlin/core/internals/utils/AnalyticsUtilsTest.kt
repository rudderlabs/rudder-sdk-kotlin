package com.rudderstack.sdk.kotlin.core.internals.utils

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.AnalyticsConfiguration
import com.rudderstack.sdk.kotlin.core.Configuration
import com.rudderstack.sdk.kotlin.core.SourceConfigManager
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.platform.PlatformType
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import com.rudderstack.sdk.kotlin.core.internals.storage.LibraryVersion
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.provideAnalyticsConfiguration
import com.rudderstack.sdk.kotlin.core.provideSourceConfigManager
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

private const val ANONYMOUS_ID = "<anonymous-id>"
private const val MESSAGE_ID = "<message-id>"

class AnalyticsUtilsTest {

    @MockK
    private lateinit var mockSourceConfigManager: SourceConfigManager

    @MockK
    private lateinit var mockAnalyticsConfiguration: AnalyticsConfiguration

    @MockK
    private lateinit var mockStorage: Storage

    @MockK
    private lateinit var mockConnectivityState: State<Boolean>

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val configuration = provideConfiguration()

    private lateinit var analytics: Analytics

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        // Mock LoggerAnalytics
        mockkObject(LoggerAnalytics)

        // Mock Analytics Configuration
        mockkStatic(::provideAnalyticsConfiguration)
        every { provideAnalyticsConfiguration(any()) } returns mockAnalyticsConfiguration
        mockAnalyticsConfiguration.apply {
            every { analyticsScope } returns testScope
            every { analyticsDispatcher } returns testDispatcher
            every { fileStorageDispatcher } returns testDispatcher
            every { keyValueStorageDispatcher } returns testDispatcher
            every { networkDispatcher } returns testDispatcher

            // Mock SourceConfig
            mockkStatic(::provideSourceConfigManager)
            every { provideSourceConfigManager(any(), any()) } returns mockSourceConfigManager
            every { sourceConfigManager } returns mockSourceConfigManager

            every { storage } returns mockStorage
            every { connectivityState } returns mockConnectivityState
            every { analyticsJob } returns SupervisorJob()
        }

        // Mocking persisted values
        every { mockStorage.readString(StorageKeys.ANONYMOUS_ID, defaultVal = any()) } returns ANONYMOUS_ID
        every { mockStorage.getLibraryVersion() } returns provideLibraryVersion()

        // Mock util method
        mockkStatic(::generateUUID)
        every { generateUUID() } returns MESSAGE_ID
    }

    @Test
    fun `given analytics is active, when isAnalyticsActive is called, then it should return true`() {
        analytics = Analytics(configuration = configuration)

        val result = analytics.isAnalyticsActive()

        assertTrue(result)
    }

    @Test
    fun `given analytics is shutdown, when isAnalyticsActive is called, then it should return false and log error`() {
        analytics = Analytics(configuration = configuration)
        analytics.shutdown()

        val result = analytics.isAnalyticsActive()

        assertFalse(result)
        verify(exactly = 1) {
            LoggerAnalytics.error("Analytics instance has been shutdown. No further operations are allowed.")
        }
    }

    @ParameterizedTest
    @MethodSource("isSourceEnabledWithEnabledSourceTestCases")
    fun `given source is enabled, when isSourceEnabled is called, then it should return true for all platforms`(
        platformType: PlatformType,
    ) {
        analytics = createAnalyticsWithPlatform(platformType)

        val result = analytics.isSourceEnabled()

        assertTrue(result)
    }

    // TODO: Uncomment this
//    @Test
//    fun `given server platform with source disabled, when isSourceEnabled is called, then it should return true`() {
//        analytics = createAnalyticsWithPlatform(PlatformType.Server)
//        disableSourceConfig()
//
//        val result = analytics.isSourceEnabled()
//
//        assertTrue(result) // Server always returns true
//    }

    @Test
    fun `given mobile platform with source disabled, when isSourceEnabled is called, then it should return false`() {
        analytics = createAnalyticsWithPlatform(PlatformType.Mobile)
        disableSourceConfig()

        val result = analytics.isSourceEnabled()

        assertFalse(result)
    }

    @ParameterizedTest
    @MethodSource("isSourceEnabledWithLoggingEnabledSourceTestCases")
    fun `given source is enabled, when isSourceEnabledWithLogging is called, then it should return true for all platforms without logging`(
        platformType: PlatformType,
    ) {
        analytics = createAnalyticsWithPlatform(platformType)

        val result = analytics.isSourceEnabledWithLogging()

        assertTrue(result)
        verify(exactly = 0) {
            LoggerAnalytics.error("Source is disabled. This operation is not allowed.")
        }
    }

    // TODO: Uncomment this
//    @Test
//    fun `given server platform with source disabled, when isSourceEnabledWithLogging is called, then it should return true without logging`() {
//        analytics = createAnalyticsWithPlatform(PlatformType.Server)
//        disableSourceConfig()
//
//        val result = analytics.isSourceEnabledWithLogging()
//
//        assertTrue(result) // Server always returns true
//        verify(exactly = 0) {
//            LoggerAnalytics.error("Source is disabled. This operation is not allowed.")
//        }
//    }

    @Test
    fun `given mobile platform with source disabled, when isSourceEnabledWithLogging is called, then it should return false and log error`() {
        analytics = createAnalyticsWithPlatform(PlatformType.Mobile)
        disableSourceConfig()

        val result = analytics.isSourceEnabledWithLogging()

        assertFalse(result)
        verify(exactly = 1) {
            LoggerAnalytics.error("Source is disabled. This operation is not allowed.")
        }
    }

    // TODO: Uncomment this
//    @Test
//    fun `given server platform, when disableSource is called, then source should remain enabled`() {
//        analytics = createAnalyticsWithPlatform(PlatformType.Server)
//
//        analytics.disableSource()
//
//        assertTrue(analytics.isSourceEnabled()) // Server platform should not be affected
//    }

    @Test
    fun `given mobile platform, when disableSource is called, then source should be disabled`() {
        analytics = createAnalyticsWithPlatform(PlatformType.Mobile)

        analytics.disableSource()

        assertFalse(analytics.isSourceEnabled())
    }

    @OptIn(UseWithCaution::class)
    @Test
    fun `given analytics is active, when handleInvalidWriteKey is called, then analytics should be shutdown and no longer active`() {
        analytics = Analytics(configuration = configuration)

        analytics.handleInvalidWriteKey()

        assertTrue(analytics.isAnalyticsShutdown)
        assertFalse(analytics.isAnalyticsActive())
    }

    // Helper methods

    private fun createAnalyticsWithPlatform(platformType: PlatformType): Analytics {
        return if (platformType == PlatformType.Mobile) {
            object : Analytics(configuration = configuration) {
                override fun getPlatformType(): PlatformType = PlatformType.Mobile
            }
        } else {
            Analytics(configuration = configuration)
        }
    }

    private fun disableSourceConfig() {
        analytics.sourceConfigState.dispatch(
            SourceConfig.UpdateAction(
                SourceConfig(
                    source = SourceConfig.initialState().source.copy(isSourceEnabled = false)
                )
            )
        )
    }

    private fun provideConfiguration() = Configuration(
        writeKey = "<writeKey>",
        dataPlaneUrl = "<data_plane_url>",
    )

    private fun provideLibraryVersion(): LibraryVersion {
        return object : LibraryVersion {
            override fun getLibraryName(): String = "com.rudderstack.kotlin.sdk"
            override fun getVersionName(): String = "1.0.0"
        }
    }

    companion object {
        @JvmStatic
        fun isSourceEnabledWithEnabledSourceTestCases(): Stream<Arguments> = Stream.of(
            Arguments.of(PlatformType.Server),
            Arguments.of(PlatformType.Mobile),
        )

        @JvmStatic
        fun isSourceEnabledWithLoggingEnabledSourceTestCases(): Stream<Arguments> = Stream.of(
            Arguments.of(PlatformType.Server),
            Arguments.of(PlatformType.Mobile),
        )
    }
}
