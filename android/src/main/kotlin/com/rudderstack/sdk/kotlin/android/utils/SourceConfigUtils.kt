package com.rudderstack.sdk.kotlin.android.utils

import com.rudderstack.sdk.kotlin.core.internals.models.Destination
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig

internal fun findDestination(sourceConfig: SourceConfig, key: String): Destination? {
    return sourceConfig.source.destinations.firstOrNull { it.destinationDefinition.displayName == key }
}
