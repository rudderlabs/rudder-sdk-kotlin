package com.rudderstack.sdk.kotlin.android.utils.network

import android.Manifest.permission
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.wifi.WifiManager
import android.provider.Settings
import android.telephony.TelephonyManager
import com.rudderstack.sdk.kotlin.android.utils.hasPermission

/**
 * A class that provides information about the carrier, cellular, wifi and bluetooth connection state.
 *
 * It acts as a fallback for the cellular and wifi connection state.
 */
internal class DefaultNetworkUtils(
    private var telephonyManager: TelephonyManager? = null,
    private var bluetoothManager: BluetoothManager? = null,
    private var wifiManager: WifiManager? = null,
) {

    private lateinit var context: Context

    internal fun setup(context: Context) {
        this.context = context
        this.telephonyManager = this.context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        this.bluetoothManager = this.context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        this.wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    }

    internal fun getCarrier(): String? = telephonyManager?.networkOperatorName

    internal fun isCellularConnected(): Boolean = if (telephonyManager?.simState == TelephonyManager.SIM_STATE_READY) {
        Settings.Global.getInt(context.contentResolver, "mobile_data", 1) == 1
    } else {
        false
    }

    internal fun isWifiEnabled(): Boolean {
        if (!hasPermission(context, permission.ACCESS_WIFI_STATE)) return false
        return wifiManager?.isWifiEnabled ?: false
    }

    internal fun isBluetoothEnabled(): Boolean = if (hasPermission(context, permission.BLUETOOTH)) {
        bluetoothManager?.adapter?.let { bluetoothAdapter ->
            bluetoothAdapter.isEnabled && bluetoothAdapter.state == BluetoothAdapter.STATE_ON
        } ?: false
    } else {
        false
    }
}
