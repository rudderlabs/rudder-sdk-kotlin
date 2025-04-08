package com.rudderstack.sdk.kotlin.core.internals.queue

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.network.HttpClient
import com.rudderstack.sdk.kotlin.core.internals.network.HttpClientImpl
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.JsonSentAtUpdater
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.internals.utils.encodeToBase64
import com.rudderstack.sdk.kotlin.core.internals.utils.generateUUID
import com.rudderstack.sdk.kotlin.core.internals.utils.parseFilePaths
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting
import java.io.File
import java.io.FileNotFoundException
import kotlin.coroutines.coroutineContext

private const val BATCH_ENDPOINT = "/v1/batch"
private val ANONYMOUS_ID_REGEX = """"anonymousId"\s*:\s*"([^"]+)"""".toRegex()
private const val UPLOAD_SIG = "#!upload"

/**
 * EventUpload is responsible for uploading events to the RudderStack data plane.
 */
internal class EventUpload(
    private val analytics: Analytics,
    private var uploadChannel: Channel<String> = createUnlimitedUploadChannel(),
    private val jsonSentAtUpdater: JsonSentAtUpdater = JsonSentAtUpdater(),
    private val httpClientFactory: HttpClient = with(analytics.configuration) {
        return@with HttpClientImpl.createPostHttpClient(
            baseUrl = dataPlaneUrl,
            endPoint = BATCH_ENDPOINT,
            authHeaderString = writeKey.encodeToBase64(),
            isGZIPEnabled = gzipEnabled,
            anonymousIdHeaderString = analytics.anonymousId ?: String.empty()
        )
    },
) {

    private var lastBatchAnonymousId = String.empty()
    private val storage get() = analytics.storage

    internal fun start() {
        resetUploadChannel()
        upload()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun resetUploadChannel() {
        if (uploadChannel.isClosedForSend || uploadChannel.isClosedForReceive) {
            uploadChannel = createUnlimitedUploadChannel()
        }
    }

    internal fun flush() {
        uploadChannel.trySend(UPLOAD_SIG)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun upload() = analytics.analyticsScope.launch(analytics.networkDispatcher) {
        uploadChannel.consumeEach {
            LoggerAnalytics.debug("performing flush")
            prepareForUpload()
            processAndUploadEvent()
        }
    }

    private suspend fun prepareForUpload() {
        withContext(analytics.storageDispatcher) {
            storage.rollover()
        }
    }

    private suspend fun processAndUploadEvent() {
        val fileUrlList = storage.readString(StorageKeys.EVENT, String.empty()).parseFilePaths()
        for (filePath in fileUrlList) {
            val file = File(filePath)
            if (!doesFileExist(file)) continue
            // ensureActive is at this position so that this coroutine can be cancelled - but any uploaded event MUST be cleared from storage.
            coroutineContext.ensureActive()
            var shouldCleanup = false
            try {
                shouldCleanup = uploadEvents(filePath)
            } catch (e: FileNotFoundException) {
                LoggerAnalytics.error("Message storage file not found", e)
            } catch (e: Exception) {
                LoggerAnalytics.error("Error when uploading event", e)
            }

            if (shouldCleanup) {
                cleanup(file, filePath)
            }
        }
    }

    private fun uploadEvents(filePath: String): Boolean {
        var shouldCleanup = false
        val batchPayload = jsonSentAtUpdater.updateSentAt(readFileAsString(filePath))

        updateAnonymousIdHeaderIfChanged(batchPayload)
        LoggerAnalytics.debug("Batch Payload: $batchPayload")
        when (val result: Result<String, Exception> = httpClientFactory.sendData(batchPayload)) {
            is Result.Success -> {
                LoggerAnalytics.debug("Event uploaded successfully. Server response: ${result.response}")
                shouldCleanup = true
            }

            is Result.Failure -> {
                LoggerAnalytics.debug("Error when uploading event due to ${result.status} ${result.error}")
            }
        }
        return shouldCleanup
    }

    private fun updateAnonymousIdHeaderIfChanged(batchPayload: String) {
        val currentBatchAnonymousId = getAnonymousIdFromBatch(batchPayload)
        if (lastBatchAnonymousId != currentBatchAnonymousId) {
            httpClientFactory.updateAnonymousIdHeaderString(currentBatchAnonymousId.encodeToBase64())
            lastBatchAnonymousId = currentBatchAnonymousId
        }
    }

    @VisibleForTesting
    internal fun getAnonymousIdFromBatch(batchPayload: String): String {
        return ANONYMOUS_ID_REGEX.find(batchPayload)?.groupValues?.get(1) ?: run {
            LoggerAnalytics.error("Fetched empty anonymousId from batch payload, falling back to random UUID.")
            generateUUID()
        }
    }

    private fun cleanup(file: File, filePath: String) {
        storage.remove(file.path).let {
            LoggerAnalytics.debug("Removed file: $filePath")
        }
    }

    internal fun cancel() {
        uploadChannel.cancel()
    }
}

@VisibleForTesting
internal fun doesFileExist(file: File) = file.exists()

@VisibleForTesting
internal fun readFileAsString(filePath: String): String {
    return File(filePath).readText()
}

internal fun createUnlimitedUploadChannel(): Channel<String> = Channel(UNLIMITED)
