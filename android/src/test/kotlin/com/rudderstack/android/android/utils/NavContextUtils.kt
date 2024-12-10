package com.rudderstack.android.android.utils

import androidx.activity.ComponentActivity
import com.rudderstack.android.android.state.NavContext
import io.mockk.mockk

internal fun mockNavContext() = NavContext(
    navController = mockk(relaxed = true),
    callingActivity = mockk<ComponentActivity>(relaxed = true)
)
