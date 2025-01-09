package com.rudderstack.sdk.kotlin.android.utils

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import com.rudderstack.sdk.kotlin.android.utils.AppSDKVersion.getVersionSDKInt

/**
 * Executes the provided lambda function based on the current SDK version.
 *
 * This function compares the current SDK version with a specified minimum compatible version.
 * If the current SDK version is greater than or equal to the minimum version,
 * it runs the `onCompatibleVersion` lambda. Otherwise, it runs the `onLegacyVersion` lambda.
 *
 * @param minCompatibleVersion The minimum SDK version required to execute `onCompatibleVersion`.
 * @param onCompatibleVersion A lambda function to execute if the current SDK version is compatible.
 * @param onLegacyVersion A lambda function to execute if the current SDK version is below the required minimum.
 */
@ChecksSdkIntAtLeast(parameter = 0, lambda = 1)
internal inline fun runBasedOnSDK(minCompatibleVersion: Int, onCompatibleVersion: () -> Unit, onLegacyVersion: () -> Unit,) {
    if (getVersionSDKInt() >= minCompatibleVersion) {
        onCompatibleVersion()
    } else {
        onLegacyVersion()
    }
}

/**
 * A utility object to retrieve the current SDK version of the Android platform.
 *
 * This wrapper around `Build.VERSION.SDK_INT` allows for easier testing and mocking
 * during unit tests. By encapsulating the SDK version retrieval in a separate function,
 * it becomes possible to mock `getVersionSDKInt()` and simulate different SDK versions
 * without relying on the actual device or emulator environment.
 */
internal object AppSDKVersion {
    fun getVersionSDKInt(): Int {
        return Build.VERSION.SDK_INT
    }
}
