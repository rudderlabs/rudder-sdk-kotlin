package com.rudderstack.scenarioengine.infrastructure.spy

import com.rudderstack.scenarioengine.domain.helper.SpyOracle
import com.rudderstack.scenarioengine.domain.spy.SpyObservation
import com.rudderstack.scenarioengine.domain.transport.Transport
import com.rudderstack.scenarioengine.ipc.Commands
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Driver-side [SpyOracle] backed by a [Transport] event subscription.
 *
 * Eagerly collects every [Commands.EVENT_TYPE_SDK_EVENT] broadcast emitted by the SUT,
 * deserializes the payload into a [SpyObservation], and stores it for later assertion.
 * "Eagerly" matters: a scenario typically registers the spy, triggers an SDK action, and
 * then asserts — the observation arrives between step 2 and step 3, so the oracle must
 * already be listening before assertion time, not lazily on the first call.
 *
 * **Replay buffer.** [observationsFlow] is a `MutableSharedFlow` with a generous replay
 * buffer ([REPLAY_BUFFER]) so [awaitObservation] resolves immediately when the matching
 * observation has already arrived. 256 is plenty for the test sizes we run; if a scenario
 * pushes past it, [awaitObservation] still resolves on subsequent matches because we never
 * stop collecting.
 *
 * **Lifecycle.** Owns a [CoroutineScope] that runs the collector. [close] cancels it; the
 * test base class calls close in `@After` so we don't leak a coroutine across tests.
 *
 * Stateful across [close]; once closed, the oracle is unusable.
 *
 * @param transport The same [Transport] the [com.rudderstack.scenarioengine.infrastructure.sut.BroadcastSut]
 *   sends commands through — its [Transport.observeEvents] is the source flow.
 */
class BroadcastSpyOracle(transport: Transport) : SpyOracle {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val observationsFlow = MutableSharedFlow<SpyObservation>(
        replay = REPLAY_BUFFER,
        extraBufferCapacity = REPLAY_BUFFER,
    )
    // Per-tag history keeps observations in arrival order — `observations(tag)` callers want
    // a chronological snapshot, not a multiset. CopyOnWriteArrayList keeps reads cheap (the
    // hot path) at the cost of slower writes (only one writer thread anyway).
    private val perTag = ConcurrentHashMap<String, CopyOnWriteArrayList<SpyObservation>>()

    init {
        scope.launch {
            transport.observeEvents()
                .filter { it.type == Commands.EVENT_TYPE_SDK_EVENT }
                .collect { event ->
                    // A malformed payload should not break the oracle for the rest of the run.
                    // Drop and continue — the SUT-side serializer is the only producer here, so
                    // failure means the JSON shape diverged, which surfaces as a timeout on the
                    // affected awaitObservation call. That's a louder signal than a thrown
                    // exception inside an oracle the test wasn't directly calling into.
                    val observation = runCatching {
                        Json.decodeFromString(SpyObservation.serializer(), event.payload)
                    }.getOrNull() ?: return@collect
                    perTag.getOrPut(observation.tag) { CopyOnWriteArrayList() }.add(observation)
                    observationsFlow.tryEmit(observation)
                }
        }
    }

    override suspend fun awaitObservation(
        tag: String,
        predicate: (SpyObservation) -> Boolean,
        timeoutMs: Long,
    ): SpyObservation = withTimeout(timeoutMs) {
        observationsFlow
            .filter { it.tag == tag && predicate(it) }
            .first()
    }

    override fun observations(tag: String): List<SpyObservation> =
        perTag[tag]?.toList() ?: emptyList()

    /** Stop collecting and free the coroutine scope. After this call the oracle is unusable. */
    fun close() {
        scope.cancel()
    }

    private companion object {
        const val REPLAY_BUFFER = 256
    }
}
