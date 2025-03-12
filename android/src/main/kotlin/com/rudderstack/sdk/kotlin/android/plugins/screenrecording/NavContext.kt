package com.rudderstack.sdk.kotlin.android.plugins.screenrecording

import android.app.Activity
import androidx.navigation.NavController

internal data class NavContext(
    val navController: NavController,
    val callingActivity: Activity,
)
