package com.rudderstack.sdk.kotlin.core.internals.queue

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.network.EventUploadResult
import com.rudderstack.sdk.kotlin.core.internals.network.HttpClient
import com.rudderstack.sdk.kotlin.core.internals.network.HttpClientImpl
import com.rudderstack.sdk.kotlin.core.internals.network.NonRetryAbleEventUploadError
import com.rudderstack.sdk.kotlin.core.internals.network.RetryAbleEventUploadError
import com.rudderstack.sdk.kotlin.core.internals.network.Success
import com.rudderstack.sdk.kotlin.core.internals.network.createPostConfig
import com.rudderstack.sdk.kotlin.core.internals.network.formatStatusCodeMessage
import com.rudderstack.sdk.kotlin.core.internals.network.toEventUploadResult
import com.rudderstack.sdk.kotlin.core.internals.policies.backoff.MaxAttemptsWithBackoff
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.DateTimeUtils
import com.rudderstack.sdk.kotlin.core.internals.utils.JsonSentAtUpdater
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
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
import com.rudderstack.sdk.kotlin.core.server.DropReason
import com.rudderstack.sdk.kotlin.core.server.asServerConfigurationOrNull
import com.rudderstack.sdk.kotlin.core.server.reportDrop
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.jetbrains.annotations.VisibleForTesting
import kotlin.coroutines.coroutineContext

private const val BATCH_ENDPOINT = "/v1/batch"
private val ANONYMOUS_ID_REGEX = """"anonymousId"\s*:\s*"([^"]+)"""".toRegex()
private const val UPLOAD_SIG = "#!upload"
private const val BATCH_KEY = "batch"

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
            postConfig = createPostConfig(
                isGZIPEnabled = gzipEnabled,
                anonymousIdHeaderString = analytics.anonymousId
                    ?.takeIf { asServerConfigurationOrNull() == null }
                    ?: String.empty(),
            ),
            logger = analytics.logger,
        )
    },
    private val maxAttemptsWithBackoff: MaxAttemptsWithBackoff =
        analytics.configuration.asServerConfigurationOrNull()
            ?.let { MaxAttemptsWithBackoff(logger = analytics.logger, maxAttempts = it.maxRetries) }
            ?: MaxAttemptsWithBackoff(logger = analytics.logger),
    private val retryHeadersProvider: RetryHeadersProvider = RetryHeadersProviderImpl(analytics.storage, analytics.logger),
) {

    private var lastBatchAnonymousId = String.empty()
    private val storage get() = analytics.storage
    private val serverConfiguration = analytics.configuration.asServerConfigurationOrNull()

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
            analytics.logger.debug("EventUpload: Performing flush")
            prepareForUpload()
            processAndUploadEvent()
        }
    }

    private suspend fun prepareForUpload() {
        analytics.logger.verbose("EventUpload: Preparing for upload — rolling over current batch file")
        if (serverConfiguration != null) {
            // The write loop can hold the file dispatcher under load, and an upload must not wait for it.
            storage.rollover()
        } else {
            withContext(analytics.fileStorageDispatcher) {
                storage.rollover()
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun processAndUploadEvent() {
        val fileUrlList = storage.readString(StorageKeys.EVENT, String.empty()).parseFilePaths()
        analytics.logger.debug("EventUpload: Processing ${fileUrlList.size} batch file(s) for upload")
        for (filePath in fileUrlList) {
            // ensureActive will help in cancelling the coroutine
            coroutineContext.ensureActive()

            try {
                storage.readBatchContent(filePath)?.let { batch ->
                    updateAnonymousIdHeaderIfChanged(batch)
                    uploadEvents(batch, filePath)
                } ?: analytics.logger.warn("EventUpload: Batch file returned null content, skipping")
            } catch (e: CancellationException) {
                analytics.logger.error("EventUpload: Job was cancelled. Stopping the upload process.", e)
                throw e
            } catch (e: Exception) {
                analytics.logger.error("EventUpload: Error when processing batch payload. Deleting the file.", e)
                cleanup(filePath)
            }
        }
    }

    private fun updateAnonymousIdHeaderIfChanged(batchPayload: String) {
        if (serverConfiguration != null) return

        val currentBatchAnonymousId = getAnonymousIdFromBatch(batchPayload)
        if (lastBatchAnonymousId != currentBatchAnonymousId) {
            httpClientFactory.updateAnonymousIdHeaderString(currentBatchAnonymousId.encodeToBase64())
            lastBatchAnonymousId = currentBatchAnonymousId
        }
    }

    @VisibleForTesting
    internal fun getAnonymousIdFromBatch(batchPayload: String): String {
        return ANONYMOUS_ID_REGEX.find(batchPayload)?.groupValues?.get(1) ?: run {
            analytics.logger.warn("EventUpload: Fetched empty anonymousId from batch payload, falling back to random UUID")
            generateUUID()
        }
    }

    private suspend fun uploadEvents(batchPayload: String, filePath: String) {
        val batchId = storage.getBatchId(filePath)
        var failedAttempts = 0
        var result: EventUploadResult
        do {
            val updatedPayload = JsonSentAtUpdater.updateSentAt(batchPayload, analytics.logger)
            analytics.logger.verbose("EventUpload: Uploading batch payload of size: ${updatedPayload.length} bytes")
            val currentTimestampInMillis = DateTimeUtils.getSystemCurrentTime()
            val retryHeaders = retryHeadersProvider.getHeaders(batchId, currentTimestampInMillis)
            result = httpClientFactory.sendData(updatedPayload, retryHeaders).toEventUploadResult()

            when (result) {
                is Success -> {
                    analytics.logger.debug("EventUpload: Event uploaded successfully. Server response: ${result.response}")
                    resetRetryState()
                    cleanup(filePath)
                }

                is RetryAbleEventUploadError -> {
                    analytics.logger.debug("EventUpload: ${result.formatStatusCodeMessage()}. Retry able error occurred.")
                    if (serverConfiguration != null && ++failedAttempts > serverConfiguration.maxRetries) {
                        dropBatchAfterLastRetry(batchPayload, filePath)
                        return
                    }
                    retryHeadersProvider.recordFailure(batchId, currentTimestampInMillis, result)
                    analytics.logger.debug("EventUpload: Retry attempt recorded. Backing off before next attempt")
                    maxAttemptsWithBackoff.delayWithBackoff()
                }

                is NonRetryAbleEventUploadError -> {
                    resetRetryState()
                    handleNonRetryAbleError(result, batchPayload, filePath)
                }
            }
        } while (result is RetryAbleEventUploadError)
    }

    private suspend fun dropBatchAfterLastRetry(batchPayload: String, filePath: String) {
        analytics.logger.error("EventUpload: The upload failed after the last retry. Dropping the batch.")
        resetRetryState()
        dropBatch(DropReason.RETRIES_EXHAUSTED, batchPayload, filePath)
    }

    private fun dropBatch(reason: DropReason, batchPayload: String, filePath: String) {
        cleanup(filePath)
        if (serverConfiguration?.dropListener != null) {
            analytics.reportDrop(reason, countEventsInBatch(batchPayload))
        }
    }

    @OptIn(UseWithCaution::class)
    private fun handleNonRetryAbleError(status: NonRetryAbleEventUploadError, batchPayload: String, filePath: String) {
        when (status) {
            NonRetryAbleEventUploadError.ERROR_400 -> {
                analytics.logger.error(
                    "EventUpload: ${status.formatStatusCodeMessage()}. Invalid request: Missing or malformed body. " +
                        "Ensure the payload is a valid JSON and includes either 'anonymousId' or 'userId' properties."
                )
                dropBatch(DropReason.REJECTED_BY_SERVER, batchPayload, filePath)
            }

            NonRetryAbleEventUploadError.ERROR_401 -> {
                analytics.logger.error(
                    "EventUpload: ${status.formatStatusCodeMessage()}. " +
                        "Invalid write key. Ensure the write key is valid."
                )
                cancel()
                analytics.handleInvalidWriteKey()
            }

            NonRetryAbleEventUploadError.ERROR_404 -> if (serverConfiguration != null) {
                analytics.logger.error(
                    "EventUpload: ${status.formatStatusCodeMessage()}. Source is disabled. Dropping the batch."
                )
                dropBatch(DropReason.REJECTED_BY_SERVER, batchPayload, filePath)
            } else {
                analytics.logger.error(
                    "EventUpload: ${status.formatStatusCodeMessage()}. Source is disabled. " +
                        "Stopping the events upload process until the source is enabled again."
                )
                cancel()
                analytics.disableSource()
            }

            NonRetryAbleEventUploadError.ERROR_413 -> {
                analytics.logger.error(
                    "EventUpload: ${status.formatStatusCodeMessage()}. " +
                        "Request failed: Payload size exceeds the maximum allowed limit."
                )
                dropBatch(DropReason.REJECTED_BY_SERVER, batchPayload, filePath)
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
            ?.let { analytics.logger.debug("EventUpload: Removed file: $filePath") }
            ?: analytics.logger.warn("EventUpload: cleanup() called with empty filePath")
    }

    internal fun cancel() {
        uploadJob?.cancel().also {
            uploadJob = null
        }
        uploadChannel.cancel()
    }
}

private fun countEventsInBatch(batchPayload: String): Int {
    return runCatching {
        LenientJson.parseToJsonElement(batchPayload).jsonObject[BATCH_KEY]?.jsonArray?.size
    }.getOrNull() ?: 0
}
