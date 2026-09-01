package com.rudderstack.sdk.kotlin.core.consent

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.AnalyticsConfiguration
import com.rudderstack.sdk.kotlin.core.Configuration
import com.rudderstack.sdk.kotlin.core.SourceConfigManager
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.provideAnalyticsConfiguration
import com.rudderstack.sdk.kotlin.core.provideSourceConfigManager
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val TEST_WRITE_KEY = "test-write-key"
private const val TEST_DATA_PLANE_URL = "https://test-data-plane.com"

@OptIn(ExperimentalCoroutinesApi::class)
class SetConsentApiTest {

    @MockK
    private lateinit var mockAnalyticsConfiguration: AnalyticsConfiguration

    @MockK
    private lateinit var mockStorage: Storage

    @MockK
    private lateinit var mockConnectivityState: State<Boolean>

    @MockK
    private lateinit var mockSourceConfigManager: SourceConfigManager

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var mockAnalyticsJob: CompletableJob

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        mockAnalyticsJob = SupervisorJob()

        mockkStatic(::provideAnalyticsConfiguration)
        every { provideAnalyticsConfiguration(any(), any()) } returns mockAnalyticsConfiguration
        mockAnalyticsConfiguration.apply {
            every { analyticsScope } returns testScope
            every { analyticsDispatcher } returns testDispatcher
            every { fileStorageDispatcher } returns testDispatcher
            every { keyValueStorageDispatcher } returns testDispatcher
            every { networkDispatcher } returns testDispatcher

            mockkStatic(::provideSourceConfigManager)
            every { provideSourceConfigManager(any(), any()) } returns mockSourceConfigManager
            every { sourceConfigManager } returns mockSourceConfigManager

            every { storage } returns mockStorage
            every { connectivityState } returns mockConnectivityState
            every { analyticsJob } returns mockAnalyticsJob
        }
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    private fun provideAnalytics(consentManagement: ConsentManagementConfiguration): Analytics =
        Analytics(
            configuration = Configuration(
                writeKey = TEST_WRITE_KEY,
                dataPlaneUrl = TEST_DATA_PLANE_URL,
                consentManagement = consentManagement,
            )
        )

    @Test
    fun `given an enabled configuration with lists, when analytics is created, then the consent state is seeded before any event`() {
        val analytics = provideAnalytics(
            ConsentManagementConfiguration(enabled = true, allowedConsentIds = listOf("marketing"))
        )

        assertTrue(analytics.consentManagementState.value.enabled)
        assertEquals(listOf("marketing"), analytics.consentManagementState.value.allowedConsentIds)
    }

    @Test
    fun `given consent management enabled with no consent ids, when analytics is created, then consent management is inactive`() {
        val mockLogger = mockAnalyticsConfiguration.logger

        val analytics = provideAnalytics(ConsentManagementConfiguration(enabled = true))

        assertFalse(analytics.consentManagementState.value.enabled)
        verify(exactly = 1) { mockLogger.info(match { it.contains("inactive for this session") }) }
    }

    @Test
    fun `given consent management disabled, when setConsent is called, then the state is unchanged and a warning is logged`() {
        val analytics = provideAnalytics(ConsentManagementConfiguration(enabled = false))
        val mockLogger = mockAnalyticsConfiguration.logger
        val stateBefore = analytics.consentManagementState.value

        analytics.setConsent(
            ConsentManagementOptions(
                allowedConsentIds = listOf("marketing"),
                deniedConsentIds = listOf("advertising"),
            )
        )

        assertEquals(stateBefore, analytics.consentManagementState.value)
        verify(exactly = 1) { mockLogger.warn(any()) }
    }

    @Test
    fun `given consent management enabled, when setConsent is called, then the state carries the new lists`() {
        val analytics = provideAnalytics(
            ConsentManagementConfiguration(enabled = true, allowedConsentIds = listOf("analytics"))
        )

        analytics.setConsent(
            ConsentManagementOptions(
                allowedConsentIds = listOf("marketing"),
                deniedConsentIds = listOf("advertising"),
            )
        )

        assertEquals(listOf("marketing"), analytics.consentManagementState.value.allowedConsentIds)
        assertEquals(listOf("advertising"), analytics.consentManagementState.value.deniedConsentIds)
    }

    @Test
    fun `given consent management enabled, when setConsent is called with empty options, then the state is unchanged and a warning is logged`() {
        val analytics = provideAnalytics(
            ConsentManagementConfiguration(enabled = true, allowedConsentIds = listOf("analytics"))
        )
        val mockLogger = mockAnalyticsConfiguration.logger
        val stateBefore = analytics.consentManagementState.value

        analytics.setConsent(ConsentManagementOptions())

        assertEquals(stateBefore, analytics.consentManagementState.value)
        verify(exactly = 1) { mockLogger.warn(any()) }
    }

    @Test
    fun `given a consent state set at runtime, when reset is called, then the consent state is identical before and after`() {
        val analytics = provideAnalytics(
            ConsentManagementConfiguration(enabled = true, allowedConsentIds = listOf("analytics"))
        )
        analytics.setConsent(
            ConsentManagementOptions(
                allowedConsentIds = listOf("marketing"),
                deniedConsentIds = listOf("advertising"),
            )
        )
        val stateBefore = analytics.consentManagementState.value

        analytics.reset()

        assertEquals(stateBefore, analytics.consentManagementState.value)
    }
}
