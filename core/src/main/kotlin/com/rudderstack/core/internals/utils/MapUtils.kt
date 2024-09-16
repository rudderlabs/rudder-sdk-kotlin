package com.rudderstack.core.internals.utils

/**
 * Merges the current map with another map, giving higher priority to the other map.
 *
 * @param other The map to merge with the current map.
 */
infix fun <K, V> Map<K, V>.mergeWithHigherPriorityTo(other: Map<K, V>): Map<K, V> {
    return this + other
}