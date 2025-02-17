package com.rudderstack.integration.kotlin.facebook

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val DEFAULT_DPO_COUNTRY = 0
private const val DEFAULT_DPO_STATE = 0
private const val DPO_COUNTRY = 1
private const val DPO_STATE = 1000

@Serializable
internal data class FacebookDestinationConfig(
    @SerialName("appID") val appId: String,
    val limitedDataUse: Boolean = false,
    @SerialName("dpoCountry") private val dpoCountry: Int = DEFAULT_DPO_COUNTRY,
    @SerialName("dpoState") private val dpoState: Int = DEFAULT_DPO_STATE
) {
    val country: Int
        get() = if (dpoCountry == DEFAULT_DPO_COUNTRY || dpoCountry == DPO_COUNTRY) dpoCountry else 0

    val state: Int
        get() = if (dpoState == DEFAULT_DPO_STATE || dpoState == DPO_STATE) dpoState else DEFAULT_DPO_STATE
}
