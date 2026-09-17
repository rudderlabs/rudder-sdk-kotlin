package com.rudderstack.sdk.kotlin.android.consent

import android.app.Application
import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.plugins.DeviceInfoPlugin
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ActivityLifecycleManagementPlugin
import com.rudderstack.sdk.kotlin.android.plugins.lifecyclemanagment.ProcessLifecycleManagementPlugin
import com.rudderstack.sdk.kotlin.core.AnalyticsConfiguration
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.utils.DateTimeUtils
import com.rudderstack.sdk.kotlin.core.provideAnalyticsConfiguration
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val TEST_WRITE_KEY = "test-write-key"
private const val TEST_DATA_PLANE_URL = "https://test-data-plane.com"
private const val TEST_SYSTEM_TIME = 1234567890000L

@OptIn(ExperimentalCoroutinesApi::class)
class SetConsentApiTest {

    @MockK
    private lateinit var mockApplication: Application

    @MockK
    private lateinit var mockAnalyticsConfiguration: AnalyticsConfiguration

    @MockK
    private lateinit var mockStorage: Storage

    @MockK
    private lateinit var mockLogger: Logger

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        // Although we don't need this in the current test class, it is needed due to the known issue with the use of Dispatchers.main.
        Dispatchers.setMain(testDispatcher)

        mockkConstructor(DeviceInfoPlugin::class)
        every { anyConstructed<DeviceInfoPlugin>().getDeviceInfo() } returns emptyJsonObject

        mockkConstructor(ProcessLifecycleManagementPlugin::class)
        every { anyConstructed<ProcessLifecycleManagementPlugin>().setup(any()) } just Runs

        mockkConstructor(ActivityLifecycleManagementPlugin::class)
        every { anyConstructed<ActivityLifecycleManagementPlugin>().setup(any()) } just Runs

        mockkStatic(::provideAnalyticsConfiguration)
        every { provideAnalyticsConfiguration(any(), any()) } returns mockAnalyticsConfiguration
        mockAnalyticsConfiguration.apply {
            every { logger } returns mockLogger
            every { analyticsScope } returns testScope
            every { analyticsDispatcher } returns testDispatcher
            every { fileStorageDispatcher } returns testDispatcher
            every { keyValueStorageDispatcher } returns testDispatcher
            every { networkDispatcher } returns testDispatcher
            every { integrationsDispatcher } returns testDispatcher
            every { storage } returns mockStorage
        }

        mockkObject(DateTimeUtils)
        every { DateTimeUtils.getSystemCurrentTime() } returns TEST_SYSTEM_TIME

        mockkObject(LoggerAnalytics)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun provideAnalytics(consentManagement: ConsentManagementConfiguration): Analytics =
        Analytics(
            configuration = Configuration(
                application = mockApplication,
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

        assertTrue(analytics.consentManagementState.value.active)
        assertEquals(listOf("marketing"), analytics.consentManagementState.value.allowedConsentIds)
    }

    @Test
    fun `given consent management enabled with no consent ids, when analytics is created, then consent management is inactive`() {
        val analytics = provideAnalytics(ConsentManagementConfiguration(enabled = true))

        assertFalse(analytics.consentManagementState.value.active)
        verify(exactly = 1) { mockLogger.info(match { it.contains("inactive for this session") }) }
    }

    @Test
    fun `given consent management disabled, when setConsent is called, then the state is unchanged and a warning is logged`() {
        val analytics = provideAnalytics(ConsentManagementConfiguration(enabled = false))
        val stateBefore = analytics.consentManagementState.value

        analytics.setConsent(
            ConsentManagementOptions(
                allowedConsentIds = listOf("marketing"),
                deniedConsentIds = listOf("advertising"),
            )
        )

        assertEquals(stateBefore, analytics.consentManagementState.value)
        verify(exactly = 1) { mockLogger.warn(match { it.contains("Consent management is not active") }) }
    }

    @Test
    fun `given consent management enabled with no consent ids, when setConsent is called, then the warning names the missing ids`() {
        val analytics = provideAnalytics(ConsentManagementConfiguration(enabled = true))
        val stateBefore = analytics.consentManagementState.value

        analytics.setConsent(ConsentManagementOptions(allowedConsentIds = listOf("marketing")))

        assertEquals(stateBefore, analytics.consentManagementState.value)
        // "disabled" would be untrue here — the customer did enable it, they just supplied no ids.
        verify(exactly = 1) { mockLogger.warn(match { it.contains("provide at least one consent ID") }) }
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
        val stateBefore = analytics.consentManagementState.value

        analytics.setConsent(ConsentManagementOptions())

        assertEquals(stateBefore, analytics.consentManagementState.value)
        verify(exactly = 1) { mockLogger.warn(any()) }
    }

    @Test
    fun `given options whose consent ids are only whitespace, when setConsent is called, then the update is refused`() {
        val analytics = provideAnalytics(
            ConsentManagementConfiguration(enabled = true, allowedConsentIds = listOf("analytics"))
        )
        val stateBefore = analytics.consentManagementState.value

        analytics.setConsent(
            ConsentManagementOptions(allowedConsentIds = listOf("   ", ""), deniedConsentIds = listOf(" "))
        )

        // Trimming happens before the emptiness check, so a CMP returning padded strings is
        // refused rather than recorded as a one-entry list.
        assertEquals(stateBefore, analytics.consentManagementState.value)
        verify(exactly = 1) { mockLogger.warn(match { it.contains("requires at least one consent ID") }) }
    }

    @Test
    fun `given a shutdown analytics instance, when setConsent is called, then the state is unchanged`() {
        val analytics = provideAnalytics(
            ConsentManagementConfiguration(enabled = true, allowedConsentIds = listOf("analytics"))
        )
        val stateBefore = analytics.consentManagementState.value
        analytics.shutdown()

        analytics.setConsent(
            ConsentManagementOptions(
                allowedConsentIds = listOf("marketing"),
                deniedConsentIds = listOf("advertising"),
            )
        )

        assertEquals(stateBefore, analytics.consentManagementState.value)
        verify { mockLogger.warn(match { it.contains("has been shutdown") }) }
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
