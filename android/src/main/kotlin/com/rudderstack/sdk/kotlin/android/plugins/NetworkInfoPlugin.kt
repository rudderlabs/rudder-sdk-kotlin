package com.rudderstack.sdk.kotlin.android.plugins

import android.Manifest.permission
import android.content.Context
import com.rudderstack.sdk.kotlin.android.Configuration
import com.rudderstack.sdk.kotlin.android.utils.hasPermission
import com.rudderstack.sdk.kotlin.android.utils.mergeWithHigherPriorityTo
import com.rudderstack.sdk.kotlin.android.utils.network.DefaultNetworkUtils
import com.rudderstack.sdk.kotlin.android.utils.network.NetworkUtils
import com.rudderstack.sdk.kotlin.android.utils.putIfNotNull
import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.plugins.Plugin
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.annotations.VisibleForTesting

private const val NETWORK_KEY = "network"
private const val NETWORK_CARRIER_KEY = "carrier"
private const val NETWORK_BLUETOOTH_KEY = "bluetooth"
private const val NETWORK_CELLULAR_KEY = "cellular"
private const val NETWORK_WIFI_KEY = "wifi"

/**
 * A plugin that attaches network information to the event payload.
 *
 * It requires the following permissions:
 *   1. The `ACCESS_NETWORK_STATE` permission to access network information.
 *   2. The `BLUETOOTH` permission to access Bluetooth features.
 *   3. The `ACCESS_WIFI_STATE` permission to access the Wi-Fi state,
 *   in the default network util plugin [DefaultNetworkUtils.isWifiEnabled].
 *
 */
internal class NetworkInfoPlugin(
    private val networkUtils: NetworkUtils = NetworkUtils(),
) : Plugin {

    override val pluginType: Plugin.PluginType = Plugin.PluginType.PreProcess
    override lateinit var analytics: Analytics
    private lateinit var context: Context

    override fun setup(analytics: Analytics) {
        super.setup(analytics)
        (analytics.configuration as Configuration).let {
            context = it.application
            networkUtils.setup(context = context)
        }
    }

    override suspend fun intercept(event: Event): Event = attachNetworkInfo(event)

    private fun attachNetworkInfo(event: Event): Event {
        LoggerAnalytics.debug("Attaching network info to the event payload")

        event.context = event.context mergeWithHigherPriorityTo getNetworkInfo()

        return event
    }

    @VisibleForTesting
    internal fun getNetworkInfo(): JsonObject = buildJsonObject {
        put(
            NETWORK_KEY,
            buildJsonObject {
                putIfNotNull(NETWORK_CARRIER_KEY, networkUtils.getCarrier())
                put(NETWORK_CELLULAR_KEY, networkUtils.isCellularConnected())
                // If relevant permissions are not granted, skip adding the wifi state
                if (hasPermission(context, permission.ACCESS_NETWORK_STATE) ||
                    hasPermission(context, permission.ACCESS_WIFI_STATE)
                ) {
                    put(NETWORK_WIFI_KEY, networkUtils.isWifiEnabled())
                }
                // As per our spec, set this value only if the permission is granted
                if (hasPermission(context, permission.BLUETOOTH)) {
                    put(NETWORK_BLUETOOTH_KEY, networkUtils.isBluetoothEnabled())
                }
            }
        )
    }

    override fun teardown() {
        networkUtils.teardown()
    }
}
