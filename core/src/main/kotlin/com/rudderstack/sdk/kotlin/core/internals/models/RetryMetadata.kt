package com.rudderstack.sdk.kotlin.core.internals.models

import com.rudderstack.sdk.kotlin.core.internals.logger.LoggerAnalytics
import com.rudderstack.sdk.kotlin.core.internals.utils.LenientJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString

/**
 * Models persisted retry state for batch upload retry headers.
 *
 * Used by `RetryHeadersProvider` to track retry attempts across the upload loop and app restarts.
 *
 * @property batchId Unique integer identifier used to detect stale metadata when batches are evicted
 * @property attempt Current retry attempt number (1 = first retry)
 * @property lastAttemptTimestampInMillis Milliseconds since epoch when the last attempt was made
 * @property reason Categorised failure reason from last attempt
 */
@Serializable
internal data class RetryMetadata(
    val batchId: Int,
    val attempt: Int,
    val lastAttemptTimestampInMillis: Long,
    val reason: String,
) {

    companion object {

        /**
         * Parses JSON string to RetryMetadata.
         *
         * @param jsonString The JSON string to parse
         * @return Parsed metadata or null if parsing fails
         */
        fun fromJson(jsonString: String): RetryMetadata? = try {
            LenientJson.decodeFromString<RetryMetadata>(jsonString)
        } catch (e: SerializationException) {
            LoggerAnalytics.warn("Failed to parse retry metadata: ${e.message}")
            null
        }
    }

    /**
     * Serialises this metadata to a JSON string.
     *
     * @return JSON string representation
     */
    fun toJson(): String = LenientJson.encodeToString(this)
}
