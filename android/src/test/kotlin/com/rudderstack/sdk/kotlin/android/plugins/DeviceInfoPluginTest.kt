package com.rudderstack.sdk.kotlin.android.plugins

import android.app.Application
import com.rudderstack.sdk.kotlin.android.Analytics
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.utils.provideEvent
import com.rudderstack.sdk.kotlin.android.utils.putIfNotNull
import com.rudderstack.sdk.kotlin.core.internals.models.Message
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
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
        every { mockConfiguration.storage } returns mockStorage
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
        mockkObject(BuildInfo)
        every { BuildInfo.getManufacturer() } returns MANUFACTURER_VALUE
        every { BuildInfo.getModel() } returns MODEL_VALUE
        every { BuildInfo.getDevice() } returns NAME_VALUE
        every { mockConfiguration.collectDeviceId } returns true
        every { plugin.retrieveDeviceId() } returns UNIQUE_DEVICE_ID

        plugin.setup(mockAnalytics)
        val actualMessage = plugin.attachDeviceInfo(provideEvent())
        unmockkObject(BuildInfo)

        val expectedMessage = provideEvent().apply {
            context = buildJsonObject {
                putAll(context)
                put(DEVICE, provideLocaleContextPayload())
            }
        }

        assertEquals(expectedMessage.context, actualMessage.context)
    }

    @Test
    fun `given collectDeviceId is true, when setup is called, then configuration collectDeviceId is true`() {
        val testDeviceId = UNIQUE_DEVICE_ID
        every { plugin.retrieveDeviceId() } returns testDeviceId
        every { mockConfiguration.collectDeviceId } returns true

        plugin.setup(mockAnalytics)

        assertEquals(true, (mockAnalytics.configuration as? Configuration)?.collectDeviceId)
    }

    @Test
    fun `when execute is called, it attaches device information`() = runTest {
        val mockMessage = mockk<Message>(relaxed = true)
        val updatedMessage = mockk<Message>(relaxed = true)

        every { plugin.attachDeviceInfo(mockMessage) } returns updatedMessage

        val result = plugin.execute(mockMessage)

        assertEquals(updatedMessage, result)
    }

    @Test
    fun `when collectDeviceId is true, retrieveDeviceId returns the device ID`() {
        val testDeviceId = UNIQUE_DEVICE_ID
        every { mockConfiguration.collectDeviceId } returns true
        every { plugin.retrieveDeviceId() } returns testDeviceId

        plugin.setup(mockAnalytics)

        val result = plugin.retrieveDeviceId()
        assertEquals(testDeviceId, result)
    }

    @Test
    fun `when collectDeviceId is false, retrieveDeviceId returns null`() {
        every { mockConfiguration.collectDeviceId } returns false

        plugin.setup(mockAnalytics)

        val result = plugin.retrieveDeviceId()
        assertEquals(null, result)
    }
}

private fun provideLocaleContextPayload(): JsonObject = buildJsonObject {
    putIfNotNull(ID, UNIQUE_DEVICE_ID)
    put(MANUFACTURER, MANUFACTURER_VALUE)
    put(MODEL, MODEL_VALUE)
    put(NAME, NAME_VALUE)
    put(TYPE, ANDROID)
}
