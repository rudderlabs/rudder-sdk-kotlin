package com.rudderstack.sdk.kotlin.android.storage

import android.os.Build

internal object CheckBuildVersionUseCase {

    /**
     * Checks if the current Android version is at least the specified SDK level.
     *
     * @param sdkLevel The minimum SDK level to check against
     * @return true if the device is running on at least the specified SDK level
     */
    internal fun isAndroidVersionAtLeast(sdkLevel: Int): Boolean {
        return Build.VERSION.SDK_INT >= sdkLevel
    }
}
