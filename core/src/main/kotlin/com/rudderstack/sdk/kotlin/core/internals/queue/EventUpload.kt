package com.rudderstack.sdk.kotlin.core.internals.queue

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.network.HttpClient
import com.rudderstack.sdk.kotlin.core.internals.network.HttpClientImpl
import com.rudderstack.sdk.kotlin.core.internals.network.NetworkErrorStatus
import com.rudderstack.sdk.kotlin.core.internals.network.NetworkResult
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.JsonSentAtUpdater
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import com.rudderstack.sdk.kotlin.core.internals.utils.createIfInactive
import com.rudderstack.sdk.kotlin.core.internals.utils.createNewIfClosed
import com.rudderstack.sdk.kotlin.core.internals.utils.createUnlimitedUploadChannel
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.internals.utils.encodeToBase64
import com.rudderstack.sdk.kotlin.core.internals.utils.generateUUID
import com.rudderstack.sdk.kotlin.core.internals.utils.parseFilePaths
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting
import java.io.File
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

    // This job is required to mainly stop the upload process when the source is disabled.
    // The type is null to clear the job reference when the source is disabled.
    private var uploadJob: Job? = null

    internal fun start() {
        uploadChannel = uploadChannel.createNewIfClosed()
        uploadJob = uploadJob.createIfInactive(newJob = ::upload)
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

    @Suppress("TooGenericExceptionCaught")
    private suspend fun processAndUploadEvent() {
        val fileUrlList = storage.readString(StorageKeys.EVENT, String.empty()).parseFilePaths()
        for (filePath in fileUrlList) {
            if (!doesFileExist(filePath)) continue
            // ensureActive is at this position so that this coroutine can be cancelled - but any uploaded event MUST be cleared from storage.
            coroutineContext.ensureActive()

            try {
                prepareBatch(filePath)
                    .takeIf { batch -> batch.isNotEmpty() }
                    ?.also { batch -> updateAnonymousIdHeaderIfChanged(batch) }
                    ?.let { batch -> uploadEvents(batch, filePath) }
            } catch (e: Exception) {
                LoggerAnalytics.error("Error when uploading event", e)
                cleanup(filePath)
            }
        }
    }

    private fun prepareBatch(filePath: String): String {
        return runCatching {
            readFileAsString(filePath)
                .let { jsonSentAtUpdater.updateSentAt(it) }
        }.getOrElse { exception ->
            LoggerAnalytics.error(
                "Error when preparing batch payload for file: $filePath. Deleting the file. Exception: ",
                exception
            )
            cleanup(filePath)
            String.empty()
        }
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

    private fun uploadEvents(batchPayload: String, filePath: String) {
        LoggerAnalytics.debug("Batch Payload: $batchPayload")
        when (val result: NetworkResult = httpClientFactory.sendData(batchPayload)) {
            is Result.Success -> {
                LoggerAnalytics.debug("Event uploaded successfully. Server response: ${result.response}")
                cleanup(filePath)
            }

            is Result.Failure -> {
                LoggerAnalytics.debug("Error when uploading event due to ${result.error}")
                handleFailure(result.error, filePath)
            }
        }
    }

    @VisibleForTesting
    internal fun handleFailure(status: NetworkErrorStatus, filePath: String) {
        // TODO: Implement the step to reset the backoff logic
        when (status) {
            NetworkErrorStatus.ERROR_400 -> {
                // TODO: Log the error
//                cleanup(filePath)
            }

            NetworkErrorStatus.ERROR_401 -> {
                // TODO: Log the error
                // TODO: Delete all the files related to this writeKey
//                analytics.shutdown()
            }

            NetworkErrorStatus.ERROR_404 -> {
                LoggerAnalytics.error("Source is disabled. Stopping the upload process until the source is enabled again.")
                cancel()
                analytics.sourceConfigState.dispatch(SourceConfig.DisableSourceAction())
            }

            NetworkErrorStatus.ERROR_413 -> {
                LoggerAnalytics.error("Batch request failed: Payload size exceeds the maximum allowed limit.")
                cleanup(filePath)
            }

            NetworkErrorStatus.ERROR_RETRY,
            NetworkErrorStatus.ERROR_NETWORK_UNAVAILABLE,
            NetworkErrorStatus.ERROR_UNKNOWN,
            -> {
                // TODO: Add exponential backoff
            }
        }
    }

    private fun cleanup(filePath: String) {
        filePath
            .takeIf { it.isNotEmpty() }
            ?.let { storage.remove(it) }
            ?.let { LoggerAnalytics.debug("Removed file: $it") }
    }

    internal fun cancel() {
        uploadJob?.cancel().also {
            uploadJob = null
        }
        uploadChannel.cancel()
    }
}

@VisibleForTesting
internal fun doesFileExist(filePath: String): Boolean {
    val file = File(filePath)
    return file.exists()
}

@VisibleForTesting
internal fun readFileAsString(filePath: String): String {
    return File(filePath).readText()
}
