package com.rudderstack.sdk.kotlin.core.internals.queue

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.policies.FlushPoliciesFacade
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.internals.utils.encodeToString
import com.rudderstack.sdk.kotlin.core.internals.utils.isSourceEnabled
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(DelicateCoroutinesApi::class)
internal class EventQueue(
    private val analytics: Analytics,
    private var flushPoliciesFacade: FlushPoliciesFacade = FlushPoliciesFacade(analytics.configuration.flushPolicies),
    private val eventUpload: EventUpload = EventUpload(
        analytics = analytics,
    ),
) {

    private var running: Boolean
    private var writeChannel: Channel<QueueMessage>
    private val storage
        get() = analytics.storage
    private val flushSignal = QueueMessage(QueueMessage.QueueMessageType.FLUSH_SIGNAL)
    private var lastEventAnonymousId = storage.readString(
        StorageKeys.LAST_EVENT_ANONYMOUS_ID,
        analytics.anonymousId ?: String.empty()
    )

    init {
        running = false
        writeChannel = Channel(UNLIMITED)
    }

    internal fun put(event: Event) {
        writeChannel.trySend(QueueMessage(QueueMessage.QueueMessageType.MESSAGE, event))
    }

    internal fun start() {
        if (running) return
        running = true

        if (writeChannel.isClosedForSend || writeChannel.isClosedForReceive) {
            writeChannel = Channel(UNLIMITED)
        }
        eventUpload.start()

        observeConfigAndUpdateSchedule()
        write()
    }

    private fun observeConfigAndUpdateSchedule() {
        with(analytics) {
            analyticsScope.launch(analyticsDispatcher) {
                sourceConfigState
                    .map { it.source.isSourceEnabled }
                    .distinctUntilChanged()
                    .collect { isSourceEnabled ->
                        if (isSourceEnabled) {
                            flushPoliciesFacade.schedule(analytics)
                            eventUpload.start()
                        } else {
                            flushPoliciesFacade.cancelSchedule()
                        }
                    }
            }
        }
    }

    internal fun flush() {
        writeChannel.trySend(flushSignal)
    }

    internal fun stop() {
        if (!running) return
        running = false

        eventUpload.cancel()
        writeChannel.close()

        flushPoliciesFacade.cancelSchedule()
    }

    internal fun stringifyBaseEvent(payload: Event): String {
        return payload.encodeToString()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun write() = analytics.analyticsScope.launch(analytics.storageDispatcher) {
        for (queueMessage in writeChannel) {
            val isFlushSignal = (queueMessage.type == QueueMessage.QueueMessageType.FLUSH_SIGNAL)

            if (!isFlushSignal) {
                updateAnonymousIdAndRolloverIfNeeded(queueMessage)
                try {
                    queueMessage.event?.let {
                        stringifyBaseEvent(it).also { stringValue ->
                            LoggerAnalytics.debug("Storing event: $stringValue")
                            storage.write(StorageKeys.EVENT, stringValue)
                        }
                        flushPoliciesFacade.updateState()
                    }
                } catch (e: Exception) {
                    LoggerAnalytics.error("Error adding payload: $queueMessage", e)
                }
            }

            if ((isFlushSignal || flushPoliciesFacade.shouldFlush()) && analytics.isSourceEnabled()) {
                eventUpload.flush()
                flushPoliciesFacade.reset()
            }
        }
    }

    private suspend fun updateAnonymousIdAndRolloverIfNeeded(queueMessage: QueueMessage) {
        val currentEventAnonymousId = queueMessage.event?.anonymousId ?: String.empty()
        if (currentEventAnonymousId != lastEventAnonymousId) {
            withContext(analytics.storageDispatcher) {
                // rollover when last and current anonymousId are different
                storage.rollover()
                lastEventAnonymousId = currentEventAnonymousId
                storage.write(StorageKeys.LAST_EVENT_ANONYMOUS_ID, lastEventAnonymousId)
            }
        }
    }
}

private data class QueueMessage(
    val type: QueueMessageType,
    val event: Event? = null,
) {

    enum class QueueMessageType {
        MESSAGE,
        FLUSH_SIGNAL,
    }
}
