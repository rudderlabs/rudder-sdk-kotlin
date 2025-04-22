package com.rudderstack.sdk.kotlin.core.internals.queue

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.network.EventUploadResult
import com.rudderstack.sdk.kotlin.core.internals.network.HttpClient
import com.rudderstack.sdk.kotlin.core.internals.network.HttpClientImpl
import com.rudderstack.sdk.kotlin.core.internals.network.NonRetryAbleError
import com.rudderstack.sdk.kotlin.core.internals.network.NonRetryAbleEventUploadError
import com.rudderstack.sdk.kotlin.core.internals.network.RetryAbleError
import com.rudderstack.sdk.kotlin.core.internals.network.Success
import com.rudderstack.sdk.kotlin.core.internals.network.toEventUploadResult
import com.rudderstack.sdk.kotlin.core.internals.policies.backoff.MaxAttemptsExponentialBackoff
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.JsonSentAtUpdater
import com.rudderstack.sdk.kotlin.core.internals.utils.UseWithCaution
import com.rudderstack.sdk.kotlin.core.internals.utils.createIfInactive
import com.rudderstack.sdk.kotlin.core.internals.utils.createNewIfClosed
import com.rudderstack.sdk.kotlin.core.internals.utils.createUnlimitedCapacityChannel
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.internals.utils.encodeToBase64
import com.rudderstack.sdk.kotlin.core.internals.utils.generateUUID
import com.rudderstack.sdk.kotlin.core.internals.utils.handleInvalidWriteKey
import com.rudderstack.sdk.kotlin.core.internals.utils.parseFilePaths
import kotlinx.coroutines.CancellationException
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
    private var uploadChannel: Channel<String> = createUnlimitedCapacityChannel(),
    private val httpClientFactory: HttpClient = with(analytics.configuration) {
        return@with HttpClientImpl.createPostHttpClient(
            baseUrl = dataPlaneUrl,
            endPoint = BATCH_ENDPOINT,
            authHeaderString = writeKey.encodeToBase64(),
            isGZIPEnabled = gzipEnabled,
            anonymousIdHeaderString = analytics.anonymousId ?: String.empty()
        )
    },
    private val maxAttemptsExponentialBackoff: MaxAttemptsExponentialBackoff = MaxAttemptsExponentialBackoff(),
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
            // ensureActive will help in cancelling the coroutine
            coroutineContext.ensureActive()

            try {
                readFileAsString(filePath)
                    .also { batch -> updateAnonymousIdHeaderIfChanged(batch) }
                    .let { batch -> uploadEvents(batch, filePath) }
            } catch (e: CancellationException) {
                LoggerAnalytics.error("Job was cancelled. Stopping the upload process.", e)
                throw e
            } catch (e: Exception) {
                LoggerAnalytics.error("Error when processing batch payload. Deleting the file.", e)
                cleanup(filePath)
            }
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

    private suspend fun uploadEvents(batchPayload: String, filePath: String) {
        var result: EventUploadResult
        do {
            val updatedPayload = JsonSentAtUpdater.updateSentAt(batchPayload)
            LoggerAnalytics.verbose("Batch Payload: $updatedPayload")
            result = httpClientFactory.sendData(updatedPayload).toEventUploadResult()

            when (result) {
                is Success -> {
                    LoggerAnalytics.debug("Event uploaded successfully. Server response: ${result.response}")
                    maxAttemptsExponentialBackoff.reset()
                    cleanup(filePath)
                }
                is RetryAbleError -> {
                    LoggerAnalytics.debug("EventUpload: Retry able error occurred")
                    maxAttemptsExponentialBackoff.delayWithBackoff()
                }
                is NonRetryAbleError -> {
                    maxAttemptsExponentialBackoff.reset()
                    handleNonRetryAbleError(result, filePath)
                }
            }
        } while (result is RetryAbleError)
    }

    @OptIn(UseWithCaution::class)
    private fun handleNonRetryAbleError(status: NonRetryAbleError, filePath: String) {
        when (status) {
            NonRetryAbleEventUploadError.ERROR_400 -> {
                LoggerAnalytics.error(
                    "Invalid request: missing or malformed body. " +
                        "Ensure the payload is valid JSON and includes either anonymousId or userId."
                )
                cleanup(filePath)
            }

            NonRetryAbleEventUploadError.ERROR_401 -> {
                LoggerAnalytics.error("Invalid write key. Ensure the write key is valid.")
                cancel()
                analytics.handleInvalidWriteKey()
            }

            NonRetryAbleEventUploadError.ERROR_404 -> {
                LoggerAnalytics.error("Source is disabled. Stopping the upload process until the source is enabled again.")
                cancel()
                analytics.sourceConfigState.dispatch(SourceConfig.DisableSourceAction())
            }

            NonRetryAbleEventUploadError.ERROR_413 -> {
                LoggerAnalytics.error("Batch request failed: Payload size exceeds the maximum allowed limit.")
                cleanup(filePath)
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
