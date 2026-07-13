package com.rudderstack.sdk.kotlin.core.internals.statemanagement

import com.rudderstack.sdk.kotlin.core.internals.utils.InternalRudderApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

/**
 * A [State] holds a single value that can only be mutated by dispatching [StateAction]s.
 *
 * It is backed by (composes) a [MutableStateFlow]. Consumers observe the state via [flow] (or [observeDispatched]) and mutate it via [dispatch].
 */
interface State<T> {

    /**
     * Read-only view of the state for observation. Emits the current value immediately on
     * collection and every subsequent value.
     */
    val flow: StateFlow<T>

    /**
     * The current value of the state. Convenience accessor for synchronous reads.
     */
    val value: T get() = flow.value

    /**
     * Dispatches the given [action] to update the state.
     */
    fun dispatch(action: StateAction<T>)

    /**
     * Returns a [Flow] that emits values only once a real value has been [dispatch]ed, i.e. it skips
     * the initial seed value supplied at construction time.
     *
     * Unlike dropping the first emission positionally, this is based on whether a value has actually
     * been dispatched, so it is correct regardless of *when* the collector subscribes:
     * - a collector that subscribes *before* the first dispatch skips the seed and, once dispatching
     *   begins, receives the current dispatched value followed by subsequent updates, and
     * - a collector that subscribes *after* the first dispatch immediately receives the current
     *   (already dispatched) value followed by subsequent updates.
     *
     * Because this is backed by a [StateFlow], values are conflated: the collector observes the latest
     * value at the time it starts collecting and every distinct value thereafter, not necessarily each
     * individual dispatch. The gate is on *whether* a dispatch has occurred, not on the value: nothing
     * is emitted until the first dispatch, so the seed is skipped prior to it. (If a dispatch happens to
     * reduce to a value equal to the seed, that value can still be observed.)
     *
     * Prefer this over collecting [flow] directly whenever a consumer must act only on real values
     * and never on the initial seed.
     */
    fun observeDispatched(): Flow<T>
}

private class StateImpl<T>(initialState: T) : State<T> {

    private val backing = MutableStateFlow(initialState)

    // Flipped to true after the first real dispatch. It is set only *after* the value has been
    // updated, so "dispatched == true" reliably implies the current value is a dispatched one and
    // never the seed - this is what makes observeDispatched() free of subscription-timing races.
    private val dispatched = MutableStateFlow(false)

    override val flow: StateFlow<T> = backing.asStateFlow()

    override fun dispatch(action: StateAction<T>) {
        backing.update { currentValue ->
            action.reduce(currentValue)
        }
        dispatched.value = true
    }

    override fun observeDispatched(): Flow<T> = flow {
        dispatched.first { it }
        emitAll(backing)
    }
}

/**
 * Creates a [State] with the given initial [initialState].
 */
@InternalRudderApi
fun <T> State(initialState: T): State<T> {
    return StateImpl(initialState)
}
