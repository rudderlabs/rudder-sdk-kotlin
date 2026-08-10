package com.rudderstack.integration.kotlin.clevertap

import kotlinx.serialization.Serializable

@Serializable
internal data class CleverTapDestinationConfig(
    val accountId: String = "",
    val accountToken: String = "",
    val region: String = DEFAULT_REGION,
)

internal const val DEFAULT_REGION = "none"
