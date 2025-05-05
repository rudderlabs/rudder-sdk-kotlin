package com.rudderstack.sdk.kotlin.android.storage

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

internal object CheckBuildVersionUseCase {

    /**
     * Checks if the current Android version is at least the specified SDK level.
     *
     * @param sdkLevel The minimum SDK level to check against
     * @return true if the device is running on at least the specified SDK level
     */
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @SuppressLint("ObsoleteSdkInt")
    internal fun isAndroidVersionAtLeast(sdkLevel: Int): Boolean {
        return Build.VERSION.SDK_INT >= sdkLevel
    }
}
