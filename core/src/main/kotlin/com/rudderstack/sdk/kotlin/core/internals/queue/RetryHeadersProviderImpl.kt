package com.rudderstack.sdk.kotlin.core.internals.queue

import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.models.RetryMetadata
import com.rudderstack.sdk.kotlin.core.internals.network.RetryAbleEventUploadError
import com.rudderstack.sdk.kotlin.core.internals.network.toRetryReason
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.empty

private const val RSA_RETRY_ATTEMPT = "Rsa-Retry-Attempt"
private const val RSA_SINCE_LAST_ATTEMPT = "Rsa-Since-Last-Attempt"
private const val RSA_RETRY_REASON = "Rsa-Retry-Reason"

/**
 * Minimum value for time elapsed since last attempt.
 *
 * Used to clamp negative deltas caused by clock skew (device time changed backwards).
 * A value of 0 indicates clock skew occurred, since two retry attempts at the exact same
 * millisecond is practically impossible given the backoff delay between attempts.
 */
private const val MIN_SINCE_LAST_ATTEMPT_IN_MILLIS = 0L

/**
 * Initial attempt count for the first retry.
 *
 * Retry attempts are 1-indexed (first retry = 1, second retry = 2, etc.).
 */
private const val FIRST_ATTEMPT = 1

/**
 * Default implementation of [RetryHeadersProvider] using the existing Storage abstraction.
 *
 * @param storage Persists metadata across process restarts
 */
internal class RetryHeadersProviderImpl(
    private val storage: Storage,
) : RetryHeadersProvider {

    override fun getHeaders(batchId: Int, currentTimestampInMillis: Long): Map<String, String> {
        val metadata = getMetadataForBatch(batchId) ?: return emptyMap()

        val elapsedSinceLastAttemptInMillis = currentTimestampInMillis - metadata.lastAttemptTimestampInMillis
        val sinceLastAttemptInMillis = maxOf(MIN_SINCE_LAST_ATTEMPT_IN_MILLIS, elapsedSinceLastAttemptInMillis)

        LoggerAnalytics.verbose(
            "Adding retry headers: attempt=${metadata.attempt}, " +
                "sinceLastAttempt=${sinceLastAttemptInMillis}ms, reason=${metadata.reason}"
        )

        return mapOf(
            RSA_RETRY_ATTEMPT to metadata.attempt.toString(),
            RSA_SINCE_LAST_ATTEMPT to sinceLastAttemptInMillis.toString(),
            RSA_RETRY_REASON to metadata.reason,
        )
    }

    override suspend fun recordFailure(batchId: Int, timestampInMillis: Long, error: RetryAbleEventUploadError) {
        val newAttempt = getMetadataForBatch(batchId)
            ?.let { it.attempt + 1 }
            ?: FIRST_ATTEMPT

        val reason = error.toRetryReason()

        val newMetadata = RetryMetadata(
            batchId = batchId,
            attempt = newAttempt,
            lastAttemptTimestampInMillis = timestampInMillis,
            reason = reason,
        )

        storage.write(StorageKeys.RETRY_METADATA, newMetadata.toJson())
    }

    override suspend fun clear() {
        LoggerAnalytics.verbose("Clearing retry metadata from storage")
        storage.remove(StorageKeys.RETRY_METADATA)
    }

    private fun getMetadataForBatch(batchId: Int): RetryMetadata? {
        val metadata = storage.readString(StorageKeys.RETRY_METADATA, String.empty())
            .takeIf { it.isNotEmpty() }
            ?.toRetryMetadata()
            ?: return null

        return if (metadata.batchId == batchId) {
            metadata
        } else {
            LoggerAnalytics.verbose("Discarding stale retry metadata: batchId mismatch")
            null
        }
    }
}

/**
 * Extension function to convert JSON string to [RetryMetadata].
 * Improves readability in transformation chains.
 */
private fun String.toRetryMetadata(): RetryMetadata? = RetryMetadata.fromJson(this)
