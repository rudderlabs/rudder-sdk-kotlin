package com.rudderstack.core.internals.utils

import java.util.WeakHashMap

object ExecutionManager {

    // Using WeakHashMap to hold references to already executed instances
    private val executionState = WeakHashMap<Any, Boolean>()

    // Function to run a task only once per instance
    fun Any.runOnce(task: () -> Unit) {
        synchronized(executionState) {
            // Check if the task has already been executed for this instance
            if (executionState.containsKey(this)) return

            // Mark as executed for this instance
            executionState[this] = true

            // Run the task
            task()
        }
    }
}
