package com.rudderstack.integration.kotlin.facebook

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FacebookDestinationConfig(
    @SerialName("appID") val appId: String,
    val limitedDataUse: Boolean = false,
    @SerialName("dpoCountry") private val _dpoCountry: Int = 0,
    @SerialName("dpoState") private val _dpoState: Int = 0
) {
    val dpoCountry: Int
        get() = if (_dpoCountry == 0 || _dpoCountry == 1) _dpoCountry else 0

    val dpoState: Int
        get() = if (_dpoState == 0 || _dpoState == 1000) _dpoState else 0
}
