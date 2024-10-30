package com.rudderstack.kotlin.sdk.internals.utils

import com.rudderstack.kotlin.sdk.internals.models.ExternalIds

// TODO: Add unit test cases
internal infix fun List<ExternalIds>.mergeWithHigherPriorityTo(other: List<ExternalIds>): List<ExternalIds> {
    val mergedList = this + other
    return mergedList.associateBy { it.type }.values.toList()
}
