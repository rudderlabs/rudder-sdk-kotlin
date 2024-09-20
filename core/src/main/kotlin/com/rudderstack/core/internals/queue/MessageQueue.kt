package com.rudderstack.core.internals.queue

import com.rudderstack.core.Analytics
import com.rudderstack.core.internals.logger.TAG
import com.rudderstack.core.internals.models.FlushEvent
import com.rudderstack.core.internals.models.Message
import com.rudderstack.core.internals.models.MessageType
import com.rudderstack.core.internals.network.HttpClient
import com.rudderstack.core.internals.network.HttpClientImpl
import com.rudderstack.core.internals.network.Result
import com.rudderstack.core.internals.policies.FlushPoliciesFacade
import com.rudderstack.core.internals.storage.StorageKeys
import com.rudderstack.core.internals.utils.DateTimeUtils
import com.rudderstack.core.internals.utils.empty
import com.rudderstack.core.internals.utils.encodeToBase64
import com.rudderstack.core.internals.utils.encodeToString
import com.rudderstack.core.internals.utils.parseFilePaths
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting
import java.io.File
import java.io.FileNotFoundException

internal const val UPLOAD_SIG = "#!upload"
private const val BATCH_ENDPOINT = "/v1/batch"
private const val SENT_AT_REGEX_PATTERN = """"sentAt":"[^"]*""""

@OptIn(DelicateCoroutinesApi::class)
internal class MessageQueue(
    private val analytics: Analytics,
    private val httpClientFactory: HttpClient = analytics.createPostHttpClientFactory(),
    private var flushPoliciesFacade: FlushPoliciesFacade =
        FlushPoliciesFacade(analytics.configuration.flushPolicies)
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
    }

    // TODO("Listen for anonymousID state change through state management library")
//    private fun updateAnonymousId(newAnonymousId: String) {
//        httpClientFactory.updateAnonymousIdHeaderString(newAnonymousId.encodeToBase64())
//    }

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
                    analytics.configuration.logger.debug(TAG, "running $stringVal")
                    storage.write(StorageKeys.MESSAGE, stringVal)
                    flushPoliciesFacade.updateState()
                } catch (e: Exception) {
                    analytics.configuration.logger.error(TAG, "Error adding payload: $message", e)
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
            analytics.configuration.logger.debug(TAG, "performing flush")
            withContext(analytics.storageDispatcher) {
                storage.rollover()
            }
            val fileUrlList = storage.readString(StorageKeys.MESSAGE, String.empty()).parseFilePaths()
            for (filePath in fileUrlList) {
                val file = File(filePath)
                if (!isFileExists(file)) continue

                var shouldCleanup = false
                try {
                    val batchPayload = updateSentAtTimestamp(readFileAsString(filePath))
                    analytics.configuration.logger.debug(TAG, "-------> readFileAsString: $batchPayload")
                    when (val result: Result<String> = httpClientFactory.sendData(batchPayload)) {
                        is Result.Success -> {
                            analytics.configuration.logger.debug(
                                log = "Event uploaded successfully. Server response: ${result.response}"
                            )
                            shouldCleanup = true
                        }

                        is Result.Failure -> {
                            analytics.configuration.logger.debug(
                                log = "Error when uploading event due to ${result.status} ${result.error}"
                            )
                        }
                    }
                } catch (e: FileNotFoundException) {
                    analytics.configuration.logger.error(TAG, "Message storage file not found", e)
                } catch (e: Exception) {
                    analytics.configuration.logger.error(TAG, "Error when uploading event", e)
                    //  shouldCleanup = handleUploadException(e, file)
                }

                if (shouldCleanup) {
                    storage.remove(file.path).let {
                        analytics.configuration.logger.debug(
                            log = "Removed file: $filePath"
                        )
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

private fun Analytics.createPostHttpClientFactory(): HttpClient {
    val authHeaderString: String = configuration.writeKey.encodeToBase64()
    val isGzipEnabled = configuration.gzipEnabled
    // TODO("Add the way to get the anonymousId")
    val anonymousIdHeaderString = "<ANONYMOUS_ID_HEADER_STRING>".encodeToBase64()

    return HttpClientImpl.createPostHttpClient(
        baseUrl = configuration.dataPlaneUrl,
        endPoint = BATCH_ENDPOINT,
        authHeaderString = authHeaderString,
        isGZIPEnabled = isGzipEnabled,
        anonymousIdHeaderString = anonymousIdHeaderString,
    )
}

private fun updateSentAtTimestamp(jsonString: String): String {
    val latestTimestamp = DateTimeUtils.now()

    val updatedJsonString = jsonString.replace(
        SENT_AT_REGEX_PATTERN.toRegex(),
        """"sentAt":"$latestTimestamp""""
    )

    return updatedJsonString
}
