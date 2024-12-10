package com.rudderstack.android.android.utils.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.ContextCompat

/**
 * Utility class to check the network status using [ConnectivityManager.registerNetworkCallback].
 */
internal class NetworkCallbackUtils(private val context: Context) {

    /**
     * Checks if the device is connected to a cellular network.
     *
     * - If Wi-Fi is enabled, this will return false, regardless of the cellular connection status.
     * - If Wi-Fi is disabled, it will return true if the device is connected to a cellular network,
     *   or false if it is not.
     */
    internal var isCellularConnected: Boolean = false
        private set

    /**
     * **NOTE**: These callbacks will be triggered only if the WIFI is not connected.
     *
     * Use [DefaultNetworkUtils.isCellularConnected] to check the cellular connection status when Wi-Fi is connected.
     */
    private val cellularCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            isCellularConnected = true
        }

        override fun onLost(network: Network) {
            isCellularConnected = false
        }
    }

    internal var isWifiEnabled: Boolean = false
        private set

    private val wifiCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            isWifiEnabled = true
        }

        override fun onLost(network: Network) {
            isWifiEnabled = false
        }
    }

    @Throws(RuntimeException::class)
    internal fun setup() {
        val connectivityManager =
            ContextCompat.getSystemService(context, ConnectivityManager::class.java) as ConnectivityManager

        val cellularRequest: NetworkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()
        connectivityManager.registerNetworkCallback(cellularRequest, cellularCallback)

        val wifiRequest: NetworkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(wifiRequest, wifiCallback)
    }
}
