package com.rudderstack.core.internals.policies

import com.rudderstack.core.internals.utils.ExecutionManager.runOnce

class StartupFlushPolicy : FlushPolicy {

    private var flushedAtStartup = true

    fun shouldFlush(): Boolean =
        flushedAtStartup.also {
            runOnce {
                flushedAtStartup = false
            }
        }
}
