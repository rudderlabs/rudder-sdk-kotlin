package com.rudderstack.core.internals.utils

import java.util.WeakHashMap

/**
 * `ExecutionManager` is a singleton object that provides utility methods to ensure
 * tasks are executed only once per instance of an object.
 *
 * It uses a `WeakHashMap` to store the execution state of each instance, allowing
 * garbage collection of keys (instances) that are no longer in use, thus preventing
 * memory leaks.
 */
object ExecutionManager {

    // A WeakHashMap to keep track of whether a task has been executed for a specific instance.
    private val executionState = WeakHashMap<Any, Boolean>()

    /**
     * Extension function to run a given task only once for the calling instance.
     *
     * @receiver Any instance for which the task should be executed.
     * @param task The lambda function representing the task to be executed.
     */
    fun Any.runOnce(task: () -> Unit) {
        synchronized(executionState) {
            // Check if the task has already been executed for this instance
            if (executionState.containsKey(this)) return

            // Mark the task as executed for this instance
            executionState[this] = true

            // Execute the task
            task()
        }
    }
}
