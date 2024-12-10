package com.rudderstack.kotlin.core.internals.utils

import com.rudderstack.kotlin.core.internals.models.ExternalId

/**
 * Merges the current list with the other list based on the priority of the current list.
 *
 * Suppose there are two lists of ExternalId, `currentList` and `otherList`:
 * 1. If both list contains different ExternalId types, then the merged list will contain all the ExternalIds from both lists.
 * 2. If both list contains some ExternalId of same type, then the ExternalId from the `otherList` will be retained.
 */
internal infix fun List<ExternalId>.mergeWithHigherPriorityTo(other: List<ExternalId>): List<ExternalId> {
    val mergedList = this + other
    return mergedList.associateBy { it.type }.values.toList()
}
