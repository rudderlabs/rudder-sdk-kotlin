package com.rudderstack.sdk.kotlin.core.internals.platform

import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Interface for the platform type.
 */
fun interface Platform {

    /**
     * Returns the platform type.
     */
    fun getPlatformType(): PlatformType
}

/**
 * Enum class representing the type of platform.
 */
@Serializable
@InternalRudderApi
enum class PlatformType {
    @SerialName("mobile")
    Mobile,

    @SerialName("server")
    Server,
}
