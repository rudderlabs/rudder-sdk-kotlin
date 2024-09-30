package com.rudderstack.kotlin.sdk.internals.policies

/**
 * StartupFlushPolicy is a concrete implementation of the FlushPolicy interface
 * that triggers a flush action only once during the startup phase.
 * After the first flush, subsequent calls to check for flushing will return `false`.
 */
class StartupFlushPolicy : FlushPolicy {

    private var flushedAtStartup = false

    internal fun shouldFlush(): Boolean = if (flushedAtStartup) {
        false
    } else {
        flushedAtStartup = true
        true
    }
}
