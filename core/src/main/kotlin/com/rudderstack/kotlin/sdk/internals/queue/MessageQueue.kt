package com.rudderstack.kotlin.sdk.internals.queue

import com.rudderstack.kotlin.sdk.Analytics
import com.rudderstack.kotlin.sdk.internals.models.FlushEvent
import com.rudderstack.kotlin.sdk.internals.models.LoggerAnalytics
import com.rudderstack.kotlin.sdk.internals.models.Message
import com.rudderstack.kotlin.sdk.internals.models.MessageType
import com.rudderstack.kotlin.sdk.internals.network.HttpClient
import com.rudderstack.kotlin.sdk.internals.network.HttpClientImpl
import com.rudderstack.kotlin.sdk.internals.network.Result
import com.rudderstack.kotlin.sdk.internals.policies.FlushPoliciesFacade
import com.rudderstack.kotlin.sdk.internals.storage.StorageKeys
import com.rudderstack.kotlin.sdk.internals.utils.JsonSentAtUpdater
import com.rudderstack.kotlin.sdk.internals.utils.empty
import com.rudderstack.kotlin.sdk.internals.utils.encodeToBase64
import com.rudderstack.kotlin.sdk.internals.utils.encodeToString
import com.rudderstack.kotlin.sdk.internals.utils.parseFilePaths
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.consumeEach
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
internal class MessageQueue(
    private val analytics: Analytics,
    private var flushPoliciesFacade: FlushPoliciesFacade = FlushPoliciesFacade(analytics.configuration.flushPolicies),
    private val jsonSentAtUpdater: JsonSentAtUpdater = JsonSentAtUpdater(),
    private val httpClientFactory: HttpClient = with(analytics.configuration) {
        return@with HttpClientImpl.createPostHttpClient(
            baseUrl = dataPlaneUrl,
            endPoint = BATCH_ENDPOINT,
            authHeaderString = writeKey.encodeToBase64(),
            isGZIPEnabled = gzipEnabled,
            anonymousIdHeaderString = analytics.getAnonymousId()
        )
    }
) {

    private var running: Boolean
    private var writeChannel: Channel<Message>
    private var uploadChannel: Channel<String>

    private val storage get() = analytics.configuration.storage

    init {
        running = false
        writeChannel = Channel(UNLIMITED)
        uploadChannel = Channel(UNLIMITED)
        registerShutdownHook()
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

    fun put(message: Message) {
        writeChannel.trySend(message)
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
        writeChannel.trySend(FlushEvent(""))
    }

    fun stop() {
        if (!running) return
        running = false

        uploadChannel.cancel()
        writeChannel.cancel()
        flushPoliciesFacade.cancelSchedule()
    }

    internal fun stringifyBaseEvent(payload: Message): String {
        return payload.encodeToString()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun write() = analytics.analyticsScope.launch(analytics.storageDispatcher) {
        for (message in writeChannel) {
            val isFlushSignal = (message.type == MessageType.Flush)

            if (!isFlushSignal) {
                try {
                    val stringVal = stringifyBaseEvent(message)
                    LoggerAnalytics.debug("running $stringVal")
                    storage.write(StorageKeys.MESSAGE, stringVal)
                    flushPoliciesFacade.updateState()
                } catch (e: Exception) {
                    LoggerAnalytics.error("Error adding payload: $message", e)
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

                var shouldCleanup = false
                try {
                    val batchPayload = jsonSentAtUpdater.updateSentAt(readFileAsString(filePath))
                    LoggerAnalytics.debug("-------> readFileAsString: $batchPayload")
                    when (val result: Result<String> = httpClientFactory.sendData(batchPayload)) {
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
                    //  shouldCleanup = handleUploadException(e, file)
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

//   private fun handleUploadException(e: Exception, file: File): Boolean {
//       var shouldCleanup = false
//        if (e is HTTPException) {
//            ... handle the exception
//            )
//        }
//        return shouldCleanup
//    }

    private fun registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                this@MessageQueue.stop()
            }
        })
    }
}
