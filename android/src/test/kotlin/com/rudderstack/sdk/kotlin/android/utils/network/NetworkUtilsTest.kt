package com.rudderstack.sdk.kotlin.android.utils.network

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NetworkUtilsTest {

    @MockK
    private lateinit var mockNetworkCallbackUtils: NetworkCallbackUtils

    @MockK
    private lateinit var mockDefaultNetworkUtils: DefaultNetworkUtils

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
    }

    @Test
    fun `given default network utils returns true, when get carrier is called, then carrier is returned`() {
        val networkUtils = NetworkUtils(mockNetworkCallbackUtils, mockDefaultNetworkUtils)
        val expectedCarrier = "T-Mobile"
        every { mockDefaultNetworkUtils.getCarrier() } returns expectedCarrier

        val carrier = networkUtils.getCarrier()

        assertEquals(expectedCarrier, carrier)
    }

    @Test
    fun `given network callback utils returns true, when is cellular connected is called, then cellular connection status is returned`() {
        val networkUtils = NetworkUtils(mockNetworkCallbackUtils, mockDefaultNetworkUtils)
        val expectedCellularStatus = true
        every { mockNetworkCallbackUtils.isCellularConnected } returns expectedCellularStatus

        val isCellularConnected = networkUtils.isCellularConnected()

        assertEquals(expectedCellularStatus, isCellularConnected)
        verify (exactly = 1) { mockNetworkCallbackUtils.isCellularConnected }
        verify (exactly = 0) { mockDefaultNetworkUtils.isCellularConnected() }
    }

    @Test
    fun `given network callback utils is null, when is cellular connected is called, then default network utils is used to get cellular connection status`() {
        val networkUtils = NetworkUtils(networkCallbackUtils = null, defaultNetworkUtils = mockDefaultNetworkUtils,)
        val expectedCellularStatus = true
        every { mockDefaultNetworkUtils.isCellularConnected() } returns expectedCellularStatus

        val isCellularConnected = networkUtils.isCellularConnected()

        assertEquals(expectedCellularStatus, isCellularConnected)
        verify (exactly = 0) { mockNetworkCallbackUtils.isCellularConnected }
        verify (exactly = 1) { mockDefaultNetworkUtils.isCellularConnected() }
    }

    @Test
    fun `given network callback utils returns false, when is cellular connected is called, then default network utils is used to get cellular connection status`() {
        val networkUtils = NetworkUtils(mockNetworkCallbackUtils, mockDefaultNetworkUtils)
        val expectedCellularStatus = true
        every { mockNetworkCallbackUtils.isCellularConnected } returns false
        every { mockDefaultNetworkUtils.isCellularConnected() } returns expectedCellularStatus

        val isCellularConnected = networkUtils.isCellularConnected()

        assertEquals(expectedCellularStatus, isCellularConnected)
        verify (exactly = 1) { mockNetworkCallbackUtils.isCellularConnected }
        verify (exactly = 1) { mockDefaultNetworkUtils.isCellularConnected() }
    }

    @Test
    fun `given network callback utils returns true, when is wifi enabled is called, then wifi status is returned`() {
        val networkUtils = NetworkUtils(mockNetworkCallbackUtils, mockDefaultNetworkUtils)
        val expectedWifiStatus = true
        every { mockNetworkCallbackUtils.isWifiEnabled } returns expectedWifiStatus

        val isWifiEnabled = networkUtils.isWifiEnabled()

        assertEquals(expectedWifiStatus, isWifiEnabled)
        verify (exactly = 1) { mockNetworkCallbackUtils.isWifiEnabled }
        verify (exactly = 0) { mockDefaultNetworkUtils.isWifiEnabled() }
    }

    @Test
    fun `given network callback utils is null, when is wifi enabled is called, then default network utils is used to get wifi status`() {
        val networkUtils = NetworkUtils(networkCallbackUtils = null, defaultNetworkUtils = mockDefaultNetworkUtils)
        val expectedWifiStatus = true
        every { mockDefaultNetworkUtils.isWifiEnabled() } returns expectedWifiStatus

        val isWifiEnabled = networkUtils.isWifiEnabled()

        assertEquals(expectedWifiStatus, isWifiEnabled)
        verify (exactly = 0) { mockNetworkCallbackUtils.isWifiEnabled }
        verify (exactly = 1) { mockDefaultNetworkUtils.isWifiEnabled() }
    }

    @Test
    fun `given network callback utils returns false, when is wifi enabled is called, then default network utils is used to get wifi status`() {
        val networkUtils = NetworkUtils(mockNetworkCallbackUtils, mockDefaultNetworkUtils)
        val expectedWifiStatus = false
        every { mockNetworkCallbackUtils.isWifiEnabled } returns expectedWifiStatus

        val isWifiEnabled = networkUtils.isWifiEnabled()

        assertEquals(expectedWifiStatus, isWifiEnabled)
        verify (exactly = 1) { mockNetworkCallbackUtils.isWifiEnabled }
        verify (exactly = 0) { mockDefaultNetworkUtils.isWifiEnabled() }
    }

    @Test
    fun `given default network utils returns true, when is bluetooth enabled is called, then bluetooth status is returned`() {
        val networkUtils = NetworkUtils(mockNetworkCallbackUtils, mockDefaultNetworkUtils)
        val expectedBluetoothStatus = true
        every { mockDefaultNetworkUtils.isBluetoothEnabled() } returns expectedBluetoothStatus

        val isBluetoothEnabled = networkUtils.isBluetoothEnabled()

        assertEquals(expectedBluetoothStatus, isBluetoothEnabled)
    }

    @Test
    fun `when teardown is called, then network callback utils teardown is called`() {
        val networkUtils = NetworkUtils(mockNetworkCallbackUtils, mockDefaultNetworkUtils)

        networkUtils.teardown()

        verify { mockNetworkCallbackUtils.teardown() }
    }
}
