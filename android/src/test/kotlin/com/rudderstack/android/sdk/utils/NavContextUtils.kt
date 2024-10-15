package com.rudderstack.android.sdk.utils

import androidx.activity.ComponentActivity
import com.rudderstack.android.sdk.state.NavContext
import io.mockk.mockk

internal fun mockNavContext() = NavContext(
    navController = mockk(relaxed = true),
    callingActivity = mockk<ComponentActivity>(relaxed = true)
)
