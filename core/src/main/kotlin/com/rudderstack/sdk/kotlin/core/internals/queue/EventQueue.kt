package com.rudderstack.sdk.kotlin.core.internals.queue

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.Event
import com.rudderstack.sdk.kotlin.core.internals.network.HttpClient
import com.rudderstack.sdk.kotlin.core.internals.network.HttpClientImpl
import com.rudderstack.sdk.kotlin.core.internals.policies.FlushPoliciesFacade
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.JsonSentAtUpdater
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.internals.utils.encodeToBase64
import com.rudderstack.sdk.kotlin.core.internals.utils.encodeToString
import com.rudderstack.sdk.kotlin.core.internals.utils.parseFilePaths
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting
import java.io.File
import java.io.FileNotFoundException

internal const val UPLOAD_SIG = "#!upload"
private const val BATCH_ENDPOINT = "/v1/batch"

@OptIn(DelicateCoroutinesApi::class)
internal class EventQueue(
    private val analytics: Analytics,
    private var flushPoliciesFacade: FlushPoliciesFacade = FlushPoliciesFacade(analytics.configuration.flushPolicies),
    private val jsonSentAtUpdater: JsonSentAtUpdater = JsonSentAtUpdater(),
    private val httpClientFactory: HttpClient = with(analytics.configuration) {
        return@with HttpClientImpl.createPostHttpClient(
            baseUrl = dataPlaneUrl,
            endPoint = BATCH_ENDPOINT,
            authHeaderString = writeKey.encodeToBase64(),
            isGZIPEnabled = gzipEnabled,
            anonymousIdHeaderString = analytics.getAnonymousId() ?: String.empty()
        )
    }
) {

    private var running: Boolean
    private var writeChannel: Channel<QueueMessage>
    private var uploadChannel: Channel<String>
    private val storage get() = analytics.storage
    private val flushSignal = QueueMessage(QueueMessage.QueueMessageType.FLUSH_SIGNAL)

    init {
        running = false
        writeChannel = Channel(UNLIMITED)
        uploadChannel = Channel(UNLIMITED)
        updateAnonymousId()
    }

    private fun updateAnonymousId() {
        analytics.userIdentityState
            .distinctUntilChanged { old, new ->
                old.anonymousId == new.anonymousId
            }
            .onEach { userOptionsState ->
                httpClientFactory.updateAnonymousIdHeaderString(userOptionsState.anonymousId.encodeToBase64())
            }.launchIn(analytics.analyticsScope)
    }

    fun put(event: Event) {
        writeChannel.trySend(QueueMessage(QueueMessage.QueueMessageType.MESSAGE, event))
    }

    fun start() {
        if (running) return
        running = true

        if (writeChannel.isClosedForSend || writeChannel.isClosedForReceive) {
            writeChannel = Channel(UNLIMITED)
            uploadChannel = Channel(UNLIMITED)
        }
        flushPoliciesFacade.schedule(analytics)
        write()
        upload()
    }

    fun flush() {
        writeChannel.trySend(flushSignal)
    }

    fun stop() {
        if (!running) return
        running = false

        uploadChannel.cancel()
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
                try {
                    queueMessage.event?.let {
                        stringifyBaseEvent(it).also { stringValue ->
                            LoggerAnalytics.debug("running $stringValue")
                            storage.write(StorageKeys.MESSAGE, stringValue)
                        }
                        flushPoliciesFacade.updateState()
                    }
                } catch (e: Exception) {
                    LoggerAnalytics.error("Error adding payload: $queueMessage", e)
                }
            }

            if (isFlushSignal || flushPoliciesFacade.shouldFlush()) {
                uploadChannel.trySend(UPLOAD_SIG)
                flushPoliciesFacade.reset()
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun upload() = analytics.analyticsScope.launch(analytics.networkDispatcher) {
        uploadChannel.consumeEach {
            LoggerAnalytics.debug("performing flush")
            withContext(analytics.storageDispatcher) {
                storage.rollover()
            }
            val fileUrlList = storage.readString(StorageKeys.MESSAGE, String.empty()).parseFilePaths()
            for (filePath in fileUrlList) {
                val file = File(filePath)
                if (!isFileExists(file)) continue
                // ensureActive is at this position so that this coroutine can be cancelled - but any uploaded event MUST be cleared from storage.
                ensureActive()
                var shouldCleanup = false
                try {
                    val batchPayload = jsonSentAtUpdater.updateSentAt(readFileAsString(filePath))
                    LoggerAnalytics.debug("-------> readFileAsString: $batchPayload")
                    when (val result: Result<String, Exception> = httpClientFactory.sendData(batchPayload)) {
                        is Result.Success -> {
                            LoggerAnalytics.debug("Event uploaded successfully. Server response: ${result.response}")
                            shouldCleanup = true
                        }

                        is Result.Failure -> {
                            LoggerAnalytics.debug("Error when uploading event due to ${result.status} ${result.error}")
                        }
                    }
                } catch (e: FileNotFoundException) {
                    LoggerAnalytics.error("Message storage file not found", e)
                } catch (e: Exception) {
                    LoggerAnalytics.error("Error when uploading event", e)
                }

                if (shouldCleanup) {
                    storage.remove(file.path).let {
                        LoggerAnalytics.debug("Removed file: $filePath")
                    }
                }
            }
        }
    }

    @VisibleForTesting
    fun isFileExists(file: File) = file.exists()

    @VisibleForTesting
    fun readFileAsString(filePath: String): String {
        return File(filePath).readText()
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
