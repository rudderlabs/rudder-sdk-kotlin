package com.rudderstack.sdk.kotlin.android.state

import android.app.Activity
import androidx.navigation.NavController

internal data class NavContext(
    val navController: NavController,
    val callingActivity: Activity,
)
