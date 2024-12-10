package com.rudderstack.sdk.kotlin.core.internals.policies

import com.rudderstack.sdk.kotlin.core.Analytics

/**
 * FlushPoliciesFacade manages a collection of different flush policies
 * and provides a unified interface to interact with them.
 * It coordinates the flushing behavior across multiple flush policies,
 * ensuring the appropriate actions are taken based on each policy's state.
 *
 * @property flushPolicies A list of flush policies that this facade will manage.
 */
internal class FlushPoliciesFacade(private val flushPolicies: List<FlushPolicy>) {

    internal fun shouldFlush(): Boolean = flushPolicies.any {
        when (it) {
            is CountFlushPolicy -> it.shouldFlush()
            is StartupFlushPolicy -> it.shouldFlush()
            else -> false
        }
    }

    internal fun updateState() {
        flushPolicies.forEach {
            if (it is CountFlushPolicy) {
                it.updateState()
            }
        }
    }

    internal fun reset() {
        flushPolicies.forEach {
            if (it is CountFlushPolicy) {
                it.reset()
            }
        }
    }

    internal fun schedule(analytics: Analytics) {
        flushPolicies.forEach {
            if (it is FrequencyFlushPolicy) {
                it.schedule(analytics)
            }
        }
    }

    internal fun cancelSchedule() {
        flushPolicies.forEach {
            if (it is FrequencyFlushPolicy) {
                it.cancelSchedule()
            }
        }
    }
}
