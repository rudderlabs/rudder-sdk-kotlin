package com.rudderstack.sdk.kotlin.android.utils

import android.content.Context
import android.content.pm.PackageManager

internal fun hasPermission(context: Context, permission: String): Boolean {
    return context.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}
