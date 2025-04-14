package com.rudderstack.sdk.kotlin.core.internals.utils

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED

/**
 * Creates a new channel if the current channel is closed for send or receive.
 */
@OptIn(DelicateCoroutinesApi::class)
internal fun <T> Channel<T>.createNewIfClosed(): Channel<T> {
    return if (isClosedForSend || isClosedForReceive) {
        createUnlimitedUploadChannel()
    } else {
        this
    }
}

/**
 * Creates a new channel with unlimited capacity.
 */
internal fun <T> createUnlimitedUploadChannel(): Channel<T> = Channel(UNLIMITED)

/**
 * Creates a new job if the current job is null or not active.
 */
internal inline fun Job?.createIfInActive(newJob: () -> Job): Job {
    return if (this == null || !this.isActive) {
        newJob()
    } else {
        this
    }
}
