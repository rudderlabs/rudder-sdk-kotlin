package com.rudderstack.android.storage

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

class CheckBuildVersionUseCase {

    /**
     * @return true, if user has a device with Android 7 (API level 24) or higher
     * @return false, if user has a device with Android API level less than 24
     */
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
    @SuppressLint("ObsoleteSdkInt")
    fun isAndroidVersionNougatAndAbove(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    }
}
