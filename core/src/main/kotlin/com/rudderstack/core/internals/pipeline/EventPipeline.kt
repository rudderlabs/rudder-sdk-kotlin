package com.rudderstack.core.internals.pipeline

import com.rudderstack.core.Analytics
import com.rudderstack.core.internals.logger.TAG
import com.rudderstack.core.internals.models.Message
import com.rudderstack.core.internals.storage.StorageKeys
import com.rudderstack.core.internals.utils.empty
import com.rudderstack.core.internals.utils.parseFilePaths
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

internal const val UPLOAD_SIG = "#!upload"
internal const val FLUSH_POISON = "#!flush"
internal const val ENDPOINT = "v1/batch"

open class EventPipeline(
    private val analytics: Analytics,
    writeKey: String,
    var dataPlaneUrl: String,
) {

    private var writeChannel: Channel<Message>
    private var uploadChannel: Channel<String>

    //protected open val httpClient: HTTPClient = HTTPClient(apiKey, analytics.configuration.requestFactory)

    protected open val storage get() = analytics.configuration.storageProvider

    var running: Boolean
        private set

    init {
        running = false
        writeChannel = Channel(UNLIMITED)
        uploadChannel = Channel(UNLIMITED)
        registerShutdownHook()
    }

    fun put(event: Message) {
        writeChannel.trySend(event)
    }

    fun start() {
        if (running) return
        running = true

        // avoid to re-establish a channel if the pipeline just gets created
        if (writeChannel.isClosedForSend || writeChannel.isClosedForReceive) {
            writeChannel = Channel(UNLIMITED)
            uploadChannel = Channel(UNLIMITED)
        }
        write()
        upload()
    }

    fun stop() {
        if (!running) return
        running = false

        uploadChannel.cancel()
        writeChannel.cancel()
    }

    open fun stringifyBaseEvent(payload: Message): String {
        return Json.encodeToString(payload)
    }

    private fun write() = analytics.analyticsScope.launch(analytics.storageDispatcher) {
        for (event in writeChannel) {
            // write to storage
            val isPoison = (event.messageId == FLUSH_POISON)
            if (!isPoison) try {
                val stringVal = stringifyBaseEvent(event)
                analytics.configuration.logger.debug(TAG, "running $stringVal")
                storage.write(StorageKeys.RUDDER_EVENT, stringVal)
            } catch (e: Exception) {
                analytics.configuration.logger.error(TAG, "Error adding payload: $event", e)
            }

            // if flush condition met, generate paths
            if (isPoison) {
                uploadChannel.trySend(UPLOAD_SIG)
            }
        }
    }

    private fun upload() = analytics.analyticsScope.launch(analytics.networkDispatcher) {
        uploadChannel.consumeEach {
            analytics.configuration.logger.debug(TAG, "performing flush")
            withContext(analytics.storageDispatcher) {
                storage.rollover()
            }

            val fileUrlList = storage.readString(StorageKeys.RUDDER_EVENT, String.empty()).parseFilePaths()
            for (url in fileUrlList) {
                // upload event file
                val file = File(url)
                if (!file.exists()) continue

                var shouldCleanup = true
                try {
//                    val connection = httpClient.upload(apiHost)
//                    connection.outputStream?.let {
//                        // Write the payloads into the OutputStream.
//                        val fileInputStream = FileInputStream(file)
//                        fileInputStream.copyTo(connection.outputStream)
//                        fileInputStream.close()
//                        connection.outputStream.close()
//
//                        // Upload the payloads.
//                        connection.close()
//                    }
                    analytics.configuration.logger.debug(TAG, "uploaded $url")
                } catch (e: Exception) {
                    shouldCleanup = handleUploadException(e, file)
                }

                if (shouldCleanup) {
                    storage.remove(file.path)
                }
            }
        }
    }

    private fun handleUploadException(e: Exception, file: File): Boolean {
        var shouldCleanup = false
//        if (e is HTTPException) {
//            analytics.configuration.logger.error(
//                tag = TAG,
//                log = "exception while uploading",
//                throwable = e
//            )
//            if (e.is4xx() && e.responseCode != 429) {
//                // Simply log and proceed to remove the rejected payloads from the queue.
//                analytics.configuration.logger.error(
//                    tag = TAG,
//                    log = "Payloads were rejected by server. Marked for removal."
//                )
//                shouldCleanup = true
//            } else {
//                analytics.configuration.logger.error(tag = TAG, log = "Error while uploading payloads")
//            }
//        } else {
//            analytics.configuration.logger.error(
//                tag = TAG,
//                log = """
//                    | Error uploading events from batch file
//                    | fileUrl="${file.path}"
//                    | msg=${e.message}
//                """.trimMargin(),
//                throwable = e
//            )
//        }

        return shouldCleanup
    }

    private fun registerShutdownHook() {
        // close the stream if the app shuts down
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                this@EventPipeline.stop()
            }
        })
    }
}

enum class WriteQueueMessageType {
    EVENT,
    FLUSH,
}

data class WriteQueueMessage(
    val type: WriteQueueMessageType,
    val event: Message?,
)
