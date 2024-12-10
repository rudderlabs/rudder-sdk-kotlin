package com.rudderstack.sdk.kotlin.android.utils

import android.os.SystemClock

internal fun getMonotonicCurrentTime() = SystemClock.elapsedRealtime()
