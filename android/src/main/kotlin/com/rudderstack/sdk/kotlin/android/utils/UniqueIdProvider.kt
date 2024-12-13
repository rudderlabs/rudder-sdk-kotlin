package com.rudderstack.sdk.kotlin.android.utils

import android.annotation.SuppressLint
import android.app.Application
import android.provider.Settings

/**
 * `UniqueIdProvider` is a utility object that provides unique identifiers for the device.
 *
 * This object includes methods for obtaining a unique Widevine ID and the Android device ID,
 * which can be used to uniquely identify the device for tracking or analytical purposes.
 * The object also handles exceptions gracefully and ensures that resources are properly released.
 */
internal object UniqueIdProvider {

    /**
     * Retrieves the Android device ID.
     *
     * This method accesses the Android ID, a unique identifier maintained by the Android OS.
     * The value is read from the `Settings.Secure.ANDROID_ID` property. If the Android ID is invalid
     * (e.g., empty, "unknown", or a known default value), the method returns `null`.
     *
     * @param application The application context used to access the Android system settings.
     * @return A string representing the Android ID, or `null` if the ID is invalid.
     */
    @SuppressLint("HardwareIds")
    internal fun getDeviceId(application: Application): String? {
        val androidId = Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID)
        val isInvalidId = androidId.isEmpty() ||
            androidId == "9774d56d682e549c" ||
            androidId == "unknown" ||
            androidId == "000000000000000"
        return if (!isInvalidId) androidId else null
    }
}
