@file:Suppress("DEPRECATION")

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
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.models.SDKManagedContextKey
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.models.emptyJsonObject
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.DateTimeUtils
import com.rudderstack.sdk.kotlin.core.provideAnalyticsConfiguration
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val TEST_WRITE_KEY = "test-write-key"
private const val TEST_DATA_PLANE_URL = "https://test-data-plane.com"
private const val TEST_SYSTEM_TIME = 1234567890000L
private const val CONSENT_KEY = "consentManagement"
private const val PROVIDER_KEY = "provider"
private const val SPOOFED_PROVIDER = "spoofed"
private const val ALLOWED_ID = "marketing"
private const val ALLOWED_IDS_KEY = "allowedConsentIds"
private const val ADDED_ID = "analytics"

/**
 * The cloud delivery path, end to end.
 *
 * `SchemaGuardPlugin` lives in core and re-asserts values from `reservedContextValues`; the
 * unit tests on both sides of that boundary pass even if nothing ever registers the consent
 * supplier. These tests run a real event through the whole chain and read what reaches storage,
 * so they fail if the wiring is missing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConsentCloudStampTest {

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

    // The event is also the first one after construction, so this doubles as proof that the
    // consent supplier is registered before anything can reach the guard.
    @Test
    fun `given a customer plugin that rewrote consentManagement, when the event reaches the cloud path, then it carries the SDK stamp`() =
        runTest(testDispatcher) {
            val analytics = provideAnalytics()
            analytics.add(SpoofingPlugin())

            analytics.track("cloud-path-event")
            testDispatcher.scheduler.runCurrent()
            disableSource(analytics)

            assertEquals(ConsentManagementProvider.CUSTOM.value, deliveredConsentProvider())
        }

    // Consent is judged at capture time: an event recorded under one decision keeps that
    // decision's values even when a later, wider one lands before it is delivered. Widening
    // consent must never retroactively authorise data the user had not consented to.
    @Test
    fun `given an event created before a consent change, when it is delivered, then it carries the consent captured at creation`() =
        runTest(testDispatcher) {
            val analytics = provideAnalytics()

            // Queued but deliberately not drained yet, so the event is still in flight when
            // the consent decision changes underneath it.
            analytics.track("pre-change-event")
            analytics.setConsent(ConsentManagementOptions(allowedConsentIds = listOf(ALLOWED_ID, ADDED_ID)))

            testDispatcher.scheduler.runCurrent()
            disableSource(analytics)

            assertEquals(listOf(ALLOWED_ID), deliveredAllowedConsentIds())
        }

    // Helpers

    private fun provideAnalytics(): Analytics = Analytics(
        configuration = Configuration(
            application = mockApplication,
            writeKey = TEST_WRITE_KEY,
            dataPlaneUrl = TEST_DATA_PLANE_URL,
            consentManagement = ConsentManagementConfiguration(
                enabled = true,
                allowedConsentIds = listOf(ALLOWED_ID),
            ),
        )
    )

    /** Stops the upload loop so the test can finish, exactly as core's own AnalyticsTest does. */
    private fun disableSource(analytics: Analytics) {
        analytics.sourceConfigState.dispatch(
            SourceConfig.UpdateAction(
                SourceConfig(source = SourceConfig.initialState().source.copy(isSourceEnabled = false))
            )
        )
    }

    /** Reads `context.consentManagement.provider` out of the payload written to storage. */
    private fun deliveredConsentProvider(): String? {
        val payload = slot<String>()
        coVerify { mockStorage.write(StorageKeys.EVENT, capture(payload)) }
        val context = Json.parseToJsonElement(payload.captured).jsonObject["context"]?.jsonObject
        return context?.get(CONSENT_KEY)?.jsonObject?.get(PROVIDER_KEY)?.toString()?.trim('"')
    }

    /** Reads `context.consentManagement.allowedConsentIds` out of the payload written to storage. */
    private fun deliveredAllowedConsentIds(): List<String> {
        val payload = slot<String>()
        coVerify { mockStorage.write(StorageKeys.EVENT, capture(payload)) }
        val context = Json.parseToJsonElement(payload.captured).jsonObject["context"]?.jsonObject
        return context?.get(CONSENT_KEY)?.jsonObject?.get(ALLOWED_IDS_KEY)?.jsonArray
            ?.map { it.jsonPrimitive.content }
            .orEmpty()
    }
}

/** Stands in for a customer plugin that overwrites the SDK-owned consent block. */
private class SpoofingPlugin : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess

    override lateinit var analytics: com.rudderstack.sdk.kotlin.core.Analytics

    override suspend fun intercept(event: Event): Event {
        event.context = JsonObject(
            event.context.toMap() + (SDKManagedContextKey.CONSENT_MANAGEMENT.key to spoofedBlock())
        )
        return event
    }

    private fun spoofedBlock(): JsonObject = buildJsonObject { put(PROVIDER_KEY, SPOOFED_PROVIDER) }
}
