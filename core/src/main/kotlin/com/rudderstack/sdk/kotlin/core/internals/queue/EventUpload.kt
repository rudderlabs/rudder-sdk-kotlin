package com.rudderstack.sdk.kotlin.core.internals.queue

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.network.EventUploadResult
import com.rudderstack.sdk.kotlin.core.internals.network.HttpClient
import com.rudderstack.sdk.kotlin.core.internals.network.HttpClientImpl
import com.rudderstack.sdk.kotlin.core.internals.network.NonRetryAbleEventUploadError
import com.rudderstack.sdk.kotlin.core.internals.network.RetryAbleEventUploadError
import com.rudderstack.sdk.kotlin.core.internals.network.Success
import com.rudderstack.sdk.kotlin.core.internals.network.formatStatusCodeMessage
import com.rudderstack.sdk.kotlin.core.internals.network.toEventUploadResult
import com.rudderstack.sdk.kotlin.core.internals.policies.backoff.MaxAttemptsWithBackoff
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.DateTimeUtils
import com.rudderstack.sdk.kotlin.core.internals.utils.JsonSentAtUpdater
import com.rudderstack.sdk.kotlin.core.internals.utils.UseWithCaution
import com.rudderstack.sdk.kotlin.core.internals.utils.createIfInactive
import com.rudderstack.sdk.kotlin.core.internals.utils.createNewIfClosed
import com.rudderstack.sdk.kotlin.core.internals.utils.createUnlimitedCapacityChannel
import com.rudderstack.sdk.kotlin.core.internals.utils.disableSource
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
    private val maxAttemptsWithBackoff: MaxAttemptsWithBackoff = MaxAttemptsWithBackoff(),
    private val retryHeadersProvider: RetryHeadersProvider = RetryHeadersProviderImpl(analytics.storage),
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
        withContext(analytics.fileStorageDispatcher) {
            storage.rollover()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun processAndUploadEvent() {
        val fileUrlList = storage.readString(StorageKeys.EVENT, String.empty()).parseFilePaths()
        for (filePath in fileUrlList) {
            // ensureActive will help in cancelling the coroutine
            coroutineContext.ensureActive()

            try {
                storage.readBatchContent(filePath)?.let { batch ->
                    updateAnonymousIdHeaderIfChanged(batch)
                    uploadEvents(batch, filePath)
                }
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
        val batchId = storage.getBatchId(filePath)
        var result: EventUploadResult
        do {
            val updatedPayload = JsonSentAtUpdater.updateSentAt(batchPayload)
            LoggerAnalytics.verbose("Batch Payload: $updatedPayload")
            val currentTimestampInMillis = DateTimeUtils.getSystemCurrentTime()
            val retryHeaders = retryHeadersProvider.getHeaders(batchId, currentTimestampInMillis)
            result = httpClientFactory.sendData(updatedPayload, retryHeaders).toEventUploadResult()

            when (result) {
                is Success -> {
                    LoggerAnalytics.debug("Event uploaded successfully. Server response: ${result.response}")
                    resetRetryState()
                    cleanup(filePath)
                }

                is RetryAbleEventUploadError -> {
                    LoggerAnalytics.debug("EventUpload: ${result.formatStatusCodeMessage()}. Retry able error occurred.")
                    retryHeadersProvider.recordFailure(batchId, currentTimestampInMillis, result)
                    maxAttemptsWithBackoff.delayWithBackoff()
                }

                is NonRetryAbleEventUploadError -> {
                    resetRetryState()
                    handleNonRetryAbleError(result, filePath)
                }
            }
        } while (result is RetryAbleEventUploadError)
    }

    @OptIn(UseWithCaution::class)
    private fun handleNonRetryAbleError(status: NonRetryAbleEventUploadError, filePath: String) {
        when (status) {
            NonRetryAbleEventUploadError.ERROR_400 -> {
                LoggerAnalytics.error(
                    "EventUpload: ${status.formatStatusCodeMessage()}. Invalid request: Missing or malformed body. " +
                        "Ensure the payload is a valid JSON and includes either 'anonymousId' or 'userId' properties."
                )
                cleanup(filePath)
            }

            NonRetryAbleEventUploadError.ERROR_401 -> {
                LoggerAnalytics.error(
                    "EventUpload: ${status.formatStatusCodeMessage()}. " +
                        "Invalid write key. Ensure the write key is valid."
                )
                cancel()
                analytics.handleInvalidWriteKey()
            }

            NonRetryAbleEventUploadError.ERROR_404 -> {
                LoggerAnalytics.error(
                    "EventUpload: ${status.formatStatusCodeMessage()}. Source is disabled. " +
                        "Stopping the events upload process until the source is enabled again."
                )
                cancel()
                analytics.disableSource()
            }

            NonRetryAbleEventUploadError.ERROR_413 -> {
                LoggerAnalytics.error(
                    "EventUpload: ${status.formatStatusCodeMessage()}. " +
                        "Request failed: Payload size exceeds the maximum allowed limit."
                )
                cleanup(filePath)
            }
        }
    }

    private suspend fun resetRetryState() {
        retryHeadersProvider.clear()
        maxAttemptsWithBackoff.reset()
    }

    private fun cleanup(filePath: String) {
        filePath
            .takeIf { it.isNotEmpty() }
            ?.let { storage.remove(it) }
            ?.let { LoggerAnalytics.debug("Removed file: $filePath") }
    }

    internal fun cancel() {
        uploadJob?.cancel().also {
            uploadJob = null
        }
        uploadChannel.cancel()
    }
}
