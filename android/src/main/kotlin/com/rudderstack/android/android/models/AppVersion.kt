package com.rudderstack.android.android.models

internal data class AppVersion(
    val previousBuild: Long,
    val previousVersionName: String?,
    val currentBuild: Long,
    val currentVersionName: String?,
)
