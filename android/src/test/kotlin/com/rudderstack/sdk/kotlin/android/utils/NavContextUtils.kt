package com.rudderstack.sdk.kotlin.android.utils

import androidx.activity.ComponentActivity
import com.rudderstack.sdk.kotlin.android.plugins.screenrecording.NavContext
import io.mockk.mockk

internal fun mockNavContext() = NavContext(
    navController = mockk(relaxed = true),
    callingActivity = mockk<ComponentActivity>(relaxed = true)
)
