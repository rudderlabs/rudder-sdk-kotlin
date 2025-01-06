package com.rudderstack.sdk.kotlin.android.storage

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

internal class CheckBuildVersionUseCase {

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @SuppressLint("ObsoleteSdkInt")
    internal fun isAndroidVersionLollipopAndAbove(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1
    }
}
