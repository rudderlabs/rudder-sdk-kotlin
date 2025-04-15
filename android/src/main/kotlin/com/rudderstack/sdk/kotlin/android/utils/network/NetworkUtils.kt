package com.rudderstack.sdk.kotlin.android.utils.network

import android.content.Context
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics

/**
 * A class that provides information about the network state.
 * This class uses both a [NetworkCallbackUtils] and a [DefaultNetworkUtils] to get information about the network.
 *
 * The [NetworkCallbackUtils] is used to get information about the cellular and wifi connection state.
 *
 * The [DefaultNetworkUtils] is used to get information about the carrier and bluetooth connection state
 * and also as a fallback for the cellular and wifi connection state.
 */
internal class NetworkUtils(
    private var networkCallbackUtils: NetworkCallbackUtils? = null,
    private var defaultNetworkUtils: DefaultNetworkUtils = DefaultNetworkUtils(),
) {

    // Catching a generic exception since the exact exception is annotated with @hide and cannot be caught directly.
    @Suppress("TooGenericExceptionCaught")
    internal fun setup(context: Context) {
        this.defaultNetworkUtils.setup(context)

        try {
            this.networkCallbackUtils = NetworkCallbackUtils(context).apply { setup() }
        } catch (e: RuntimeException) {
            LoggerAnalytics.error("Error while setting up NetworkCallbackUtil: ${e.stackTraceToString()}")
        }
    }

    internal fun getCarrier(): String? = defaultNetworkUtils.getCarrier()

    internal fun isCellularConnected(): Boolean {
        val isCellularViaCallback = networkCallbackUtils?.isCellularConnected ?: false
        return isCellularViaCallback || defaultNetworkUtils.isCellularConnected()
    }

    internal fun isWifiEnabled(): Boolean {
        return networkCallbackUtils?.isWifiEnabled ?: defaultNetworkUtils.isWifiEnabled()
    }

    internal fun isBluetoothEnabled(): Boolean = defaultNetworkUtils.isBluetoothEnabled()

    internal fun teardown() {
        networkCallbackUtils?.teardown()
    }
}
