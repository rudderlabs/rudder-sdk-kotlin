package com.rudderstack.android.sdk.plugins

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.rudderstack.android.sdk.Configuration
import com.rudderstack.android.sdk.utils.mergeWithHigherPriorityTo
import com.rudderstack.android.sdk.utils.network.NetworkUtils
import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.models.Message
import com.rudderstack.kotlin.sdk.internals.plugins.Plugin
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
 * A plugin that attaches network information to the message payload.
 *
 * It requires the `ACCESS_NETWORK_STATE` permission to access network information.
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
            networkUtils.setup(context = context, logger = it.logger)
        }
    }

    override fun execute(message: Message): Message = attachNetworkInfo(message)

    private fun attachNetworkInfo(message: Message): Message {
        analytics.configuration.logger.debug(log = "Attaching network info to the message payload")

        message.context = message.context mergeWithHigherPriorityTo getNetworkInfo()

        return message
    }

    @VisibleForTesting
    internal fun getNetworkInfo(): JsonObject = buildJsonObject {
        put(
            NETWORK_KEY,
            buildJsonObject {
                if (hasPermission(context)) {
                    put(NETWORK_CARRIER_KEY, networkUtils.getCarrier())
                    put(NETWORK_CELLULAR_KEY, networkUtils.isCellularConnected())
                    put(NETWORK_WIFI_KEY, networkUtils.isWifiEnabled())
                    put(NETWORK_BLUETOOTH_KEY, networkUtils.isBluetoothEnabled())
                }
            }
        )
    }
}

private fun hasPermission(context: Context, permission: String = Manifest.permission.ACCESS_NETWORK_STATE): Boolean {
    return context.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}