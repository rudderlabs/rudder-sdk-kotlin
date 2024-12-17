package com.rudderstack.sdk.kotlin.android.plugins

import android.app.Application
import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.utils.provideEvent
import com.rudderstack.sdk.kotlin.android.utils.UniqueIdProvider
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.putAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private const val DEVICE = "device"
private const val ID = "id"
private const val MANUFACTURER = "manufacturer"
private const val MANUFACTURER_VALUE = "Google"
private const val MODEL = "model"
private const val MODEL_VALUE = "Codename Pixel 9"
private const val NAME = "name"
private const val NAME_VALUE = "Pixel 9"
private const val TYPE = "type"
private const val ANDROID = "Android"
private const val UNIQUE_DEVICE_ID = "unique-device-id"

class DeviceInfoPluginTest {

    private lateinit var plugin: DeviceInfoPlugin
    private lateinit var mockAnalytics: Analytics
    private lateinit var mockConfiguration: Configuration
    private lateinit var mockStorage: Storage

    private val mockApplication: Application = mockk()

    @Before
    fun setup() {
        plugin = spyk(DeviceInfoPlugin())
        mockAnalytics = mockk(relaxed = true)
        mockConfiguration = mockk(relaxed = true)
        mockStorage = mockk(relaxed = true)

        every { mockAnalytics.configuration } returns mockConfiguration
        every { mockConfiguration.application } returns mockApplication
        every { mockAnalytics.storage } returns mockStorage
    }

    @Test
    fun `when setup is called, then application is retrieved`() {
        plugin.setup(mockAnalytics)

        assertEquals(mockApplication, (mockAnalytics.configuration as? Configuration)?.application)
    }

    @Test
    fun `when setup is called, then collectDeviceId is retrieved set`() {
        every { mockConfiguration.collectDeviceId } returns false

        plugin.setup(mockAnalytics)

        assertEquals(false, (mockAnalytics.configuration as? Configuration)?.collectDeviceId)
    }

    @Test
    fun `when attachDeviceInfo is called, then device information is attached to message payload `() {
        val testDeviceId = UNIQUE_DEVICE_ID
        every { plugin.retrieveDeviceId() } returns testDeviceId

        mockkObject(BuildInfo)
        every { BuildInfo.getManufacturer() } returns MANUFACTURER_VALUE
        every { BuildInfo.getModel() } returns MODEL_VALUE
        every { BuildInfo.getDevice() } returns NAME_VALUE

        val message = provideEvent().apply {
            context = buildJsonObject {
                putAll(context)
                put(DEVICE, provideLocaleContextPayload())
            }
        }

        val actual = plugin.attachDeviceInfo(provideEvent())
        assertEquals(message.context, actual.context)
        unmockkObject(BuildInfo)
    }

    @Test
    fun `given collectDeviceId is true, when setup is called, then configuration collectDeviceId is true`() {
        every { mockConfiguration.collectDeviceId } returns true

        plugin.setup(mockAnalytics)

        assertEquals(true, (mockAnalytics.configuration as? Configuration)?.collectDeviceId)
    }

    @Test
    fun `when attachDeviceInfo is called, then device information is attached to message payload`() = runTest {
        val mockEvent = mockk<Event>(relaxed = true)
        val mockUpdatedEvent = mockk<Event>(relaxed = true)

        every { plugin.attachDeviceInfo(mockEvent) } returns mockUpdatedEvent

        val result = plugin.intercept(mockEvent)

        assertEquals(mockUpdatedEvent, result)
    }

    @Test
    fun `given collectDeviceId is true, when retrieveDeviceId is called, then device id is retrieved or generated`() {
        val testDeviceId = UNIQUE_DEVICE_ID
        every { mockConfiguration.collectDeviceId } returns true
        every { plugin.retrieveOrGenerateStoredId(any()) } returns testDeviceId

        plugin.setup(mockAnalytics)

        assertEquals(testDeviceId, plugin.retrieveDeviceId())
    }

    @Test
    fun `given collectDeviceId is false, when retrieveDeviceId is called, then device id equals the anonymous id that is stored`() {
        val testDeviceId = UNIQUE_DEVICE_ID
        every { mockConfiguration.collectDeviceId } returns false
        every { mockStorage.readString(StorageKeys.ANONYMOUS_ID, any()) } returns testDeviceId

        plugin.setup(mockAnalytics)

        assertEquals(testDeviceId, plugin.retrieveDeviceId())
    }

    @Test
    fun `given device id already present in the storage, when retrieveOrGenerateStoredId is called, then returns stored device id`() {
        val testDeviceId = UNIQUE_DEVICE_ID
        plugin.setup(mockAnalytics)
        every { mockStorage.readString(StorageKeys.DEVICE_ID, any()) } returns testDeviceId

        val id = plugin.retrieveOrGenerateStoredId { testDeviceId }

        assertEquals(testDeviceId, id)
    }

    @Test
    fun `given getDeviceId can be retrieved, when generateId is executed, then it returns UniqueIdProvider getDeviceId`() {
        plugin.setup(mockAnalytics)
        val testDeviceId = UNIQUE_DEVICE_ID

        mockkObject(UniqueIdProvider)
        every { UniqueIdProvider.getDeviceId(any()) } returns testDeviceId

        assertEquals(testDeviceId, plugin.generateId())

        unmockkObject(UniqueIdProvider)
    }

    @Test
    fun `given getDeviceId can not be retrieved, when generateId is executed, then it returns UniqueIdProvider getUniqueID`() {
        plugin.setup(mockAnalytics)
        val testDeviceId = UNIQUE_DEVICE_ID

        mockkObject(UniqueIdProvider)
        every { UniqueIdProvider.getDeviceId(any()) } returns null
        every { UniqueIdProvider.getUniqueID() } returns testDeviceId

        assertEquals(testDeviceId, plugin.generateId())

        unmockkObject(UniqueIdProvider)
    }
}

private fun provideLocaleContextPayload(): JsonObject = buildJsonObject {
    put(ID, UNIQUE_DEVICE_ID)
    put(MANUFACTURER, MANUFACTURER_VALUE)
    put(MODEL, MODEL_VALUE)
    put(NAME, NAME_VALUE)
    put(TYPE, ANDROID)
}
