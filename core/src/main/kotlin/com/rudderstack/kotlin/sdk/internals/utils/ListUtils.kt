package com.rudderstack.kotlin.sdk.internals.utils

import com.rudderstack.kotlin.sdk.internals.models.ExternalId

internal infix fun List<ExternalId>.mergeWithHigherPriorityTo(other: List<ExternalId>): List<ExternalId> {
    val mergedList = this + other
    return mergedList.associateBy { it.type }.values.toList()
}