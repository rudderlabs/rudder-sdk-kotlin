package com.rudderstack.android.sdk.utils.network

import android.content.Context
import com.rudderstack.kotlin.sdk.internals.logger.Logger

internal class NetworkUtils(
    private var networkCallbackUtils: NetworkCallbackUtils? = null,
    private var defaultNetworkUtils: DefaultNetworkUtils = DefaultNetworkUtils(),
) {

    private lateinit var logger: Logger

    // Catching a generic exception since the exact exception is annotated with @hide and cannot be caught directly.
    @Suppress("TooGenericExceptionCaught")
    internal fun setup(context: Context, logger: Logger) {
        this.defaultNetworkUtils.setup(context)
        this.logger = logger

        try {
            this.networkCallbackUtils = NetworkCallbackUtils(context).apply { setup() }
        } catch (e: RuntimeException) {
            logger.error(log = "Error while setting up NetworkCallbackUtil: ${e.stackTraceToString()}")
        }
    }

    internal fun getCarrier(): String = defaultNetworkUtils.getCarrier()

    internal fun isCellularConnected(): Boolean {
        val isCellularViaCallback = networkCallbackUtils?.isCellularConnected ?: false
        return isCellularViaCallback || defaultNetworkUtils.isCellularConnected()
    }

    internal fun isWifiEnabled(): Boolean {
        val isWifiViaCallback = networkCallbackUtils?.isWifiEnabled ?: false
        return isWifiViaCallback || defaultNetworkUtils.isWifiEnabled()
    }

    internal fun isBluetoothEnabled(): Boolean = defaultNetworkUtils.isBluetoothEnabled()
}
