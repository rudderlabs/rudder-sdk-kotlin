package com.rudderstack.sdk.kotlin.android.connectivity

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import androidx.annotation.VisibleForTesting
import com.rudderstack.sdk.kotlin.android.utils.application
import com.rudderstack.sdk.kotlin.android.utils.runBasedOnSDK
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.Logger
import com.rudderstack.sdk.kotlin.core.internals.models.connectivity.ConnectivityState
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
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
    private val connectivityState: State<Boolean>
) : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.Utility
    override lateinit var analytics: Analytics

    private var connectivityManager: ConnectivityManager? = null
    private var intentFilter: IntentFilter? = null

    private val networkCallback by lazy { createNetworkCallback(connectivityState, analytics.logger) }
    private val broadcastReceiver by lazy { createBroadcastReceiver(connectivityState, analytics.logger) }

    override fun setup(analytics: Analytics) {
        super.setup(analytics)

        safelyExecute(
            block = { registerConnectivityObserver() },
            onException = { exception ->
                defaultExceptionHandler(
                    errorMsg = "Failed to register connectivity subscriber. Setting network availability to true. Exception:",
                    exception = exception,
                    logger = analytics.logger,
                )
                connectivityState.dispatch(ConnectivityState.SetDefaultStateAction())
            },
        )
    }

    // Suppressing deprecation warning as we need to support lower API levels.
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    @Throws(RuntimeException::class)
    private fun registerConnectivityObserver() {
        runBasedOnSDK(
            minCompatibleVersion = MIN_SUPPORTED_VERSION,
            onCompatibleVersion = {
                connectivityManager = this.analytics.application.getSystemService(ConnectivityManager::class.java)
                connectivityManager?.registerDefaultNetworkCallback(networkCallback)
            },
            onLegacyVersion = {
                intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
                this.analytics.application.registerReceiver(broadcastReceiver, intentFilter)
            },
        )
    }

    override fun teardown() {
        runBasedOnSDK(
            minCompatibleVersion = MIN_SUPPORTED_VERSION,
            onCompatibleVersion = { this.connectivityManager?.unregisterNetworkCallback(networkCallback) },
            onLegacyVersion = {
                this.intentFilter?.let {
                    this.analytics.application.unregisterReceiver(broadcastReceiver)
                }
            },
        )
    }
}

@VisibleForTesting
internal fun createNetworkCallback(connectivityState: State<Boolean>, logger: Logger) =
    object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            logger.verbose("AndroidConnectivityObserver: Network available")
            connectivityState.dispatch(ConnectivityState.EnableConnectivityAction())
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            logger.verbose("AndroidConnectivityObserver: Network lost")
            connectivityState.dispatch(ConnectivityState.DisableConnectivityAction())
        }
    }

@VisibleForTesting
// Suppressing deprecation warning as we need to support lower API levels.
@Suppress("DEPRECATION")
internal fun createBroadcastReceiver(connectivityState: State<Boolean>, logger: Logger) =
    object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent?) {
            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo?.let {
                when (it.isConnected) {
                    true -> {
                        logger.verbose("AndroidConnectivityObserver: Network available (broadcast)")
                        connectivityState.dispatch(ConnectivityState.EnableConnectivityAction())
                    }
                    false -> {
                        logger.verbose("AndroidConnectivityObserver: Network lost (broadcast)")
                        connectivityState.dispatch(ConnectivityState.DisableConnectivityAction())
                    }
                }
            } ?: run { // if activeNetworkInfo is null, it means the device is not connected to any network.
                logger.verbose("AndroidConnectivityObserver: Network lost (broadcast)")
                connectivityState.dispatch(ConnectivityState.DisableConnectivityAction())
            }
        }
    }
