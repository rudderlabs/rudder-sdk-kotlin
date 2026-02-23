package com.rudderstack.sdk.kotlin.core.internals.queue

import com.rudderstack.sdk.kotlin.core.internals.network.RetryAbleEventUploadError

/**
 * Provides retry headers for event batch upload requests.
 *
 * On retry attempts (not the first attempt), this provider returns headers containing:
 * - `Rsa-Retry-Attempt`: Current retry attempt number (1 for first retry)
 * - `Rsa-Since-Last-Attempt`: Time elapsed since last attempt in milliseconds
 * - `Rsa-Retry-Reason`: Categorised reason for the retry
 */
internal interface RetryHeadersProvider {

    /**
     * Returns retry headers for the current upload attempt.
     *
     * Behaviour:
     * - If no metadata exists → returns empty map (first attempt)
     * - If stored batchId does not match current batchId → returns empty map (stale metadata)
     * - If stored batchId matches → returns populated headers
     *
     * @param batchId Unique batch identifier (e.g., 0, 1)
     * @param currentTimestampInMillis Current time in milliseconds since epoch
     * @return Map of header name to value, empty for first attempt
     */
    fun getHeaders(batchId: Int, currentTimestampInMillis: Long): Map<String, String>

    /**
     * Records a failed upload attempt for retry tracking.
     *
     * Behaviour:
     * - If batchId matches stored metadata → increments attempt count
     * - If batchId differs (or no metadata) → creates new metadata with attempt=1
     *
     * @param batchId Unique integer identifier of the batch that failed
     * @param timestampInMillis Timestamp of the failed attempt in milliseconds since epoch
     * @param error The retryable error that occurred
     */
    suspend fun recordFailure(batchId: Int, timestampInMillis: Long, error: RetryAbleEventUploadError)

    /**
     * Clears all retry metadata.
     *
     * Call this when:
     * - Batch upload succeeds
     * - Non-retryable error occurs (400, 401, 404, 413)
     */
    suspend fun clear()
}
