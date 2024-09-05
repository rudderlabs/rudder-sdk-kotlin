package com.rudderstack.core.internals.queue

import com.rudderstack.core.Analytics
import com.rudderstack.core.internals.logger.TAG
import com.rudderstack.core.internals.models.MessageType
import com.rudderstack.core.internals.models.FlushMessage
import com.rudderstack.core.internals.models.Message
import com.rudderstack.core.internals.storage.StorageKeys
import com.rudderstack.core.internals.utils.empty
import com.rudderstack.core.internals.utils.parseFilePaths
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.annotations.VisibleForTesting
import java.io.File
import java.io.FileNotFoundException

internal const val UPLOAD_SIG = "#!upload"

@OptIn(DelicateCoroutinesApi::class)
internal class MessageQueue(
    private val analytics: Analytics,
) {
    private var running: Boolean
    private var writeChannel: Channel<Message>
    private var uploadChannel: Channel<String>

    private val storage get() = analytics.configuration.storageProvider

    init {
        running = false
        writeChannel = Channel(UNLIMITED)
        uploadChannel = Channel(UNLIMITED)
        registerShutdownHook()
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
        write()
        upload()
    }

    fun flush() {
        writeChannel.trySend(FlushMessage(""))
    }

    fun stop() {
        if (!running) return
        running = false

        uploadChannel.cancel()
        writeChannel.cancel()
    }

    internal fun stringifyBaseEvent(payload: Message): String {
        return Json.encodeToString(payload)
    }

    private fun write() = analytics.analyticsScope.launch(analytics.storageDispatcher) {
        for (message in writeChannel) {
            val isFlushSignal = (message.type == MessageType.Flush)

            if (!isFlushSignal) try {
                val stringVal = stringifyBaseEvent(message)
                analytics.configuration.logger.debug(TAG, "running $stringVal")
                storage.write(StorageKeys.RUDDER_MESSAGE, stringVal)
            } catch (e: Exception) {
                analytics.configuration.logger.error(TAG, "Error adding payload: $message", e)
            }

            if (isFlushSignal) {
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
            val fileUrlList = storage.readString(StorageKeys.RUDDER_MESSAGE, String.empty()).parseFilePaths()
            for (url in fileUrlList) {
                val file = File(url)
                analytics.configuration.logger.debug(TAG, "-------> url $url")
                if (!file.exists()) continue
                var shouldCleanup = true
                try {
                    analytics.configuration.logger.debug(
                        TAG,
                        "-------> storage.readEventsContent() " + storage.readMessageContent()
                    )
                    val eventsData = storage.readMessageContent()
                    for (events in eventsData) {
                        try {
                            val text = readFileAsString(events)
                            analytics.configuration.logger.debug(TAG, "-------> readFileAsString: $text")
                        } catch (e: FileNotFoundException) {
                            analytics.configuration.logger.debug(TAG, "Message storage file not found")
                        } catch (e: Exception) {
                            analytics.configuration.logger.debug(TAG, "Error when uploading event")
                        }
                    }
                } catch (e: Exception) {
                    shouldCleanup = handleUploadException(e, file)
                }

                if (shouldCleanup) {
                    storage.remove(file.path)
                }
            }
        }
    }

    @VisibleForTesting
    fun readFileAsString(filePath: String): String {
        return File(filePath).readText()
    }

    private fun handleUploadException(e: Exception, file: File): Boolean {
        var shouldCleanup = false
//        if (e is HTTPException) {
//            ... handle the exception
//            )
//        }
        return shouldCleanup
    }

    private fun registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                this@MessageQueue.stop()
            }
        })
    }
}