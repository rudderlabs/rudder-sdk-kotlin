package com.rudderstack.sdk.kotlin.android.connectivity

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import androidx.annotation.VisibleForTesting
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.utils.runBasedOnSDK
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.connectivity.ConnectivityState
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.FlowState
import com.rudderstack.sdk.kotlin.core.internals.utils.defaultExceptionHandler
import com.rudderstack.sdk.kotlin.core.internals.utils.safelyExecute

private const val MIN_SUPPORTED_VERSION = Build.VERSION_CODES.N

/**
 * Plugin to observe the network connectivity state of the Android device.
 *
 * It uses [ConnectivityManager] to observe the network connectivity changes for Android API level 24 and above.
 * For lower API levels, it uses [BroadcastReceiver] to observe the network connectivity changes.
 *
 * In case of any exception while registering the connectivity observers, it sets the connection availability to `true`.
 *
 * @param connectivityState The state management for connectivity.
 */
@Suppress("MaximumLineLength")
internal class AndroidConnectivityObserverPlugin(
    private val connectivityState: FlowState<Boolean>
) : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess
    override lateinit var analytics: Analytics

    private lateinit var application: Application

    private var connectivityManager: ConnectivityManager? = null
    private var intentFilter: IntentFilter? = null

    private val networkCallback by lazy { createNetworkCallback(::toggleConnectivityState) }
    private val broadcastReceiver by lazy { createBroadcastReceiver(::toggleConnectivityState) }

    private fun toggleConnectivityState(newState: Boolean) {
        connectivityState.dispatch(ConnectivityState.ToggleStateAction(newState))
    }

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        application = (analytics.configuration as Configuration).application

        safelyExecute(
            block = { registerConnectivityObserver() },
            onException = { exception ->
                defaultExceptionHandler(
                    errorMsg = "Failed to register connectivity subscriber. Setting network availability to true. Exception:",
                    exception = exception
                )
            },
            onFinally = { connectivityState.dispatch(ConnectivityState.SetDefaultStateAction()) }
        )
    }

    @Suppress("Deprecated")
    @Throws(RuntimeException::class)
    private fun registerConnectivityObserver() {
        runBasedOnSDK(
            minCompatibleVersion = MIN_SUPPORTED_VERSION,
            onCompatibleVersion = {
                connectivityManager = application.getSystemService(ConnectivityManager::class.java)
                connectivityManager?.registerDefaultNetworkCallback(networkCallback)
            },
            onLegacyVersion = {
                intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
                this.application.registerReceiver(broadcastReceiver, intentFilter)
            },
        )
    }

    override fun teardown() {
        runBasedOnSDK(
            minCompatibleVersion = MIN_SUPPORTED_VERSION,
            onCompatibleVersion = { this.connectivityManager?.unregisterNetworkCallback(networkCallback) },
            onLegacyVersion = {
                this.intentFilter?.let {
                    this.application.unregisterReceiver(broadcastReceiver)
                }
            },
        )
    }
}

@VisibleForTesting
internal fun createNetworkCallback(toggleConnectivityState: (Boolean) -> Unit) =
    object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            toggleConnectivityState(true)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            toggleConnectivityState(false)
        }
    }

@VisibleForTesting
@Suppress("Deprecated")
internal fun createBroadcastReceiver(toggleConnectivityState: (Boolean) -> Unit) = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo?.let {
            when (it.isConnected) {
                true -> toggleConnectivityState(true)
                false -> toggleConnectivityState(false)
            }
        } ?: run { // if activeNetworkInfo is null, it means the device is not connected to any network.
            toggleConnectivityState(false)
        }
    }
}
