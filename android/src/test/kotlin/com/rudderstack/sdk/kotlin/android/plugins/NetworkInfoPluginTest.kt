package com.rudderstack.sdk.kotlin.android.plugins

import android.app.Application
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.utils.network.NetworkUtils
import com.rudderstack.sdk.kotlin.android.utils.provideEvent
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

private const val NETWORK_KEY = "network"
private const val NETWORK_CARRIER_KEY = "carrier"
private const val NETWORK_BLUETOOTH_KEY = "bluetooth"
private const val NETWORK_CELLULAR_KEY = "cellular"
private const val NETWORK_WIFI_KEY = "wifi"

private const val CARRIER = "T-Mobile"
private const val CELLULAR = true
private const val WIFI = true
private const val BLUETOOTH = true

class NetworkInfoPluginTest {

    @MockK
    private lateinit var mockAnalytics: Analytics

    @MockK
    private lateinit var mockApplication: Application

    @MockK
    private lateinit var mockNetworkUtils: NetworkUtils

    private lateinit var networkInfoPlugin: NetworkInfoPlugin

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)

        every { (mockAnalytics.configuration as Configuration).application } returns mockApplication
        every { mockNetworkUtils.setup(any()) } returns Unit

        networkInfoPlugin = spyk(NetworkInfoPlugin(mockNetworkUtils))
    }

    @Test
    fun `given network context is present, when network info plugin is intercepted, then network info is attached to the context`() = runTest {
        val message = provideEvent()
        every { networkInfoPlugin.getNetworkInfo() } returns provideNetworkInfoPayload()

        networkInfoPlugin.setup(mockAnalytics)
        networkInfoPlugin.intercept(message)

        val actual = message.context
        JSONAssert.assertEquals(
            provideNetworkInfoPayload().toString(),
            actual.toString(),
            true
        )
    }

    @Test
    fun `given network context is present, when network info is merged with other context, then network info is given higher priority`() = runTest {
        val message = provideEvent()
        every { networkInfoPlugin.getNetworkInfo() } returns provideNetworkInfoPayload()

        networkInfoPlugin.setup(mockAnalytics)
        buildJsonObject {
            put(NETWORK_KEY, String.empty())
        }
        networkInfoPlugin.intercept(message)

        val actual = message.context
        JSONAssert.assertEquals(
            provideNetworkInfoPayload().toString(),
            actual.toString(),
            true
        )
    }

    @Test
    fun `when teardown is called, then network utils teardown is called`() = runTest {
        networkInfoPlugin.teardown()

        verify { mockNetworkUtils.teardown() }
    }
}

private fun provideNetworkInfoPayload(): JsonObject = buildJsonObject {
    put(NETWORK_KEY, buildJsonObject {
        put(NETWORK_CARRIER_KEY, CARRIER)
        put(NETWORK_CELLULAR_KEY, CELLULAR)
        put(NETWORK_WIFI_KEY, WIFI)
        put(NETWORK_BLUETOOTH_KEY, BLUETOOTH)
    })
}
