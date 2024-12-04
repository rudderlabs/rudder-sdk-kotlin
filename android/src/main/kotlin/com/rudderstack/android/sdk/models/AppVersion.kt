package com.rudderstack.android.sdk.models

internal data class AppVersion(
    val previousBuild: Long,
    val previousVersionName: String?,
    val currentBuild: Long,
    val currentVersionName: String?,
)
