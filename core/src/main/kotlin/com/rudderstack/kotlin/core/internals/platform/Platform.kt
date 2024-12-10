package com.rudderstack.kotlin.core.internals.platform

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
enum class PlatformType {
    @SerialName("mobile")
    Mobile,

    @SerialName("server")
    Server,
}
