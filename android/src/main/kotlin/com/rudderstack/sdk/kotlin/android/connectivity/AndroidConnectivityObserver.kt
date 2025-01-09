@file:Suppress("DEPRECATION")

package com.rudderstack.sdk.kotlin.android.connectivity

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkInfo
import android.os.Build
import androidx.annotation.VisibleForTesting
import com.rudderstack.sdk.kotlin.android.utils.runBasedOnSDK
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.connectivity.ConnectivityObserver
import com.rudderstack.sdk.kotlin.core.internals.connectivity.ConnectivitySubscriber
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.utils.safelyExecute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AndroidConnectivityObserver is an implementation of [ConnectivityObserver] for Android platform.
 *
 * It uses [ConnectivityManager] to observe the network connectivity changes for Android API level 24 and above.
 * For lower API levels, it uses [BroadcastReceiver] to observe the network connectivity changes.
 *
 * It also implements [ConnectivitySubscriber] to provide the network availability status to the subscribers.
 * If the network is available, it notifies the subscriber immediately. Otherwise, it waits for the network to be available.
 *
 * In case of any exception while registering the connectivity subscriber, it sets the network available status to true
 * and notifies the subscribers.
 *
 * **NOTE**: Subscribers are notified exactly once.
 *
 * @param application The [Application] instance.
 * @param analyticsScope The core [Analytics] instance.
 */
internal class AndroidConnectivityObserver(
    private val application: Application,
    private val analyticsScope: CoroutineScope,
) : ConnectivityObserver {

    private var networkAvailable: AtomicBoolean = AtomicBoolean(false)
    private val pendingSubscribers = CopyOnWriteArrayList<suspend () -> Unit>()

    private val networkCallback by lazy {
        createNetworkCallback(networkAvailable) {
            notifySubscribers()
        }
    }
    private val broadcastReceiver: BroadcastReceiver by lazy {
        createBroadcastReceiver(networkAvailable) { notifySubscribers() }
    }

    init {
        safelyExecute(
            block = { registerConnectivityObserver() },
            onException = {
                LoggerAnalytics.error(
                    "Failed to register connectivity subscriber. Setting network availability to true.",
                    it
                )
                networkAvailable = AtomicBoolean(true)
                notifySubscribers()
            },
        )
    }

    @Throws(RuntimeException::class)
    private fun registerConnectivityObserver() {
        runBasedOnSDK(
            minCompatibleVersion = Build.VERSION_CODES.N,
            onCompatibleVersion = {
                val connectivityManager: ConnectivityManager = application.getSystemService(ConnectivityManager::class.java)
                connectivityManager.registerDefaultNetworkCallback(networkCallback)
            },
            onLegacyVersion = {
                val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
                this.application.registerReceiver(broadcastReceiver, intentFilter)
            },
        )
    }

    private fun notifySubscribers() {
        this.analyticsScope.launch {
            pendingSubscribers.also {
                it.forEach { subscriber -> subscriber() }
                it.clear()
            }
        }
    }

    /**
     * Observes the network availability and notifies the subscriber immediately if the network is available.
     * Otherwise, it waits for the network to be available and then notifies the subscriber.
     *
     * **NOTE**: Subscriber are notified exactly once.
     *
     * @param subscriber The subscriber to be notified when the network is available.
     */
    override suspend fun immediateNotifyOrObserveConnectivity(subscriber: suspend () -> Unit) {
        networkAvailable.get().also {
            when (it) {
                true -> subscriber()
                false -> this.pendingSubscribers.add(subscriber)
            }
        }
    }
}

@VisibleForTesting
internal fun createNetworkCallback(networkAvailable: AtomicBoolean, notifySubscriber: () -> Unit) =
    object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            networkAvailable.set(true)
            notifySubscriber()
        }
    }

@VisibleForTesting
internal fun createBroadcastReceiver(networkAvailable: AtomicBoolean, notifySubscriber: () -> Unit) =
    object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            val isConnected = networkInfo != null && networkInfo.isConnected
            if (isConnected) {
                networkAvailable.set(true)
                notifySubscriber()
            }
        }
    }
