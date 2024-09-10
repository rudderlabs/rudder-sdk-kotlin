package com.rudderstack.core.internals.policies

import com.rudderstack.core.internals.utils.ExecutionManager.runOnce

/**
 * StartupFlushPolicy is a concrete implementation of the FlushPolicy interface
 * that triggers a flush action only once during the startup phase.
 * After the first flush, subsequent calls to check for flushing will return `false`.
 */
class StartupFlushPolicy : FlushPolicy {

    private var flushedAtStartup = true

    internal fun shouldFlush(): Boolean = flushedAtStartup.also {
        runOnce {
            flushedAtStartup = false
        }
    }
}
