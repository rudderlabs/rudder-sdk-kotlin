package com.rudderstack.sdk.kotlin.core.internals.queue

import com.rudderstack.sdk.kotlin.core.internals.models.RetryMetadata
import com.rudderstack.sdk.kotlin.core.internals.network.RetryAbleEventUploadError
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RetryHeadersProviderTest {

    private lateinit var storage: Storage
    private lateinit var provider: RetryHeadersProviderImpl

    @BeforeEach
    fun setup() {
        storage = mockk(relaxed = true)
        provider = RetryHeadersProviderImpl(storage)
    }

    @Nested
    inner class GetHeaders {

        @Test
        fun `given no stored metadata, when getHeaders is called, then returns empty map`() {
            every { storage.readString(StorageKeys.RETRY_METADATA, any()) } returns String.empty()

            val result = provider.getHeaders(batchId = 0, currentTimestampInMillis = 1000L)

            assertTrue(result.isEmpty())
        }

        @Test
        fun `given matching batchId with valid metadata, when getHeaders is called, then returns correct headers`() {
            every {
                storage.readString(
                    StorageKeys.RETRY_METADATA,
                    any()
                )
            } returns provideRetryMetadataInStringFormat()

            val result = provider.getHeaders(batchId = 0, currentTimestampInMillis = 4250L)

            assertEquals("2", result["Rsa-Retry-Attempt"])
            assertEquals("3250", result["Rsa-Since-Last-Attempt"])
            assertEquals("server-500", result["Rsa-Retry-Reason"])
            assertEquals(3, result.size)
        }

        @Test
        fun `given batchId mismatch, when getHeaders is called, then returns empty map`() {
            every {
                storage.readString(StorageKeys.RETRY_METADATA, any())
            } returns provideRetryMetadataInStringFormat()

            val result = provider.getHeaders(batchId = 5, currentTimestampInMillis = 2000L)

            assertTrue(result.isEmpty())
        }

        @Test
        fun `given corrupted json in storage, when getHeaders is called, then returns empty map`() {
            every { storage.readString(StorageKeys.RETRY_METADATA, any()) } returns "{invalid json}"

            val result = provider.getHeaders(batchId = 0, currentTimestampInMillis = 1000L)

            assertTrue(result.isEmpty())
        }

        @Test
        fun `given clock skew producing negative delta, when getHeaders is called, then sinceLastAttempt is clamped to 0`() {
            every {
                storage.readString(StorageKeys.RETRY_METADATA, any())
            } returns provideRetryMetadataInStringFormat(attempt = 1, lastAttemptTimestampInMillis = 5000L)

            val result = provider.getHeaders(batchId = 0, currentTimestampInMillis = 3000L)

            assertEquals("0", result["Rsa-Since-Last-Attempt"])
        }
    }

    @Nested
    inner class RecordFailure {

        @Test
        fun `given no existing metadata, when recordFailure is called, then stores metadata with attempt 1`() =
            runTest {
                every {
                    storage.readString(
                        StorageKeys.RETRY_METADATA,
                        any()
                    )
                } returns String.empty()

                provider.recordFailure(
                    batchId = 0,
                    timestampInMillis = 1000L,
                    error = RetryAbleEventUploadError.ErrorRetry(500)
                )

                verifyMetadataWrittenToStorage(
                    batchId = 0,
                    attempt = 1,
                    lastAttemptTimestampInMillis = 1000L,
                    reason = "server-500",
                )
            }

        @Test
        fun `given metadata for batch exists, when recordFailure is called for the same batch, then increments attempt`() =
            runTest {
                every {
                    storage.readString(StorageKeys.RETRY_METADATA, any())
                } returns provideRetryMetadataInStringFormat()

                provider.recordFailure(
                    batchId = 0,
                    timestampInMillis = 4000L,
                    error = RetryAbleEventUploadError.ErrorRetry(503),
                )

                verifyMetadataWrittenToStorage(
                    batchId = 0,
                    attempt = 3,
                    lastAttemptTimestampInMillis = 4000L,
                    reason = "server-503"
                )
            }

        @Test
        fun `given metadata for batch exists, when recordFailure is called for a different batch, then resets to attempt 1`() =
            runTest {
                every {
                    storage.readString(
                        StorageKeys.RETRY_METADATA,
                        any()
                    )
                } returns provideRetryMetadataInStringFormat(attempt = 5)

                provider.recordFailure(
                    batchId = 3,
                    timestampInMillis = 2000L,
                    error = RetryAbleEventUploadError.ErrorTimeout
                )

                verifyMetadataWrittenToStorage(
                    batchId = 3,
                    attempt = 1,
                    lastAttemptTimestampInMillis = 2000L,
                    reason = "client-timeout"
                )
            }

        @Test
        fun `given ErrorNetworkUnavailable, when recordFailure is called, then reason is client-network`() =
            runTest {
                every {
                    storage.readString(
                        StorageKeys.RETRY_METADATA,
                        any()
                    )
                } returns String.empty()

                provider.recordFailure(
                    batchId = 0,
                    timestampInMillis = 1000L,
                    error = RetryAbleEventUploadError.ErrorNetworkUnavailable
                )

                verifyMetadataWrittenToStorage(
                    batchId = 0,
                    attempt = 1,
                    lastAttemptTimestampInMillis = 1000L,
                    reason = "client-network"
                )
            }

        @Test
        fun `given ErrorUnknown, when recordFailure is called, then reason is client-unknown`() =
            runTest {
                every {
                    storage.readString(
                        StorageKeys.RETRY_METADATA,
                        any()
                    )
                } returns String.empty()

                provider.recordFailure(
                    batchId = 0,
                    timestampInMillis = 1000L,
                    error = RetryAbleEventUploadError.ErrorUnknown
                )

                verifyMetadataWrittenToStorage(
                    batchId = 0,
                    attempt = 1,
                    lastAttemptTimestampInMillis = 1000L,
                    reason = "client-unknown"
                )
            }
    }

    @Nested
    inner class Clear {

        @Test
        fun `when clear is called, then removes retry metadata from storage`() = runTest {
            provider.clear()

            coVerify { storage.remove(StorageKeys.RETRY_METADATA) }
        }
    }

    @Nested
    inner class EdgeCases {

        @Test
        fun `given extremely large sinceLastAttempt from weeks offline, when getHeaders is called, then returns large value without overflow`() {
            every {
                storage.readString(StorageKeys.RETRY_METADATA, any())
            } returns provideRetryMetadataInStringFormat()
            val twoWeeksMs = 14L * 24 * 60 * 60 * 1000 // 14 days

            val result =
                provider.getHeaders(batchId = 0, currentTimestampInMillis = 1000L + twoWeeksMs)

            assertEquals(twoWeeksMs.toString(), result["Rsa-Since-Last-Attempt"])
        }

        @Test
        fun `given all three headers present, when getHeaders returns, then no partial headers`() {
            every {
                storage.readString(StorageKeys.RETRY_METADATA, any())
            } returns provideRetryMetadataInStringFormat()

            val result = provider.getHeaders(batchId = 0, currentTimestampInMillis = 2000L)

            assertEquals(3, result.size)
            assertTrue(result.containsKey("Rsa-Retry-Attempt"))
            assertTrue(result.containsKey("Rsa-Since-Last-Attempt"))
            assertTrue(result.containsKey("Rsa-Retry-Reason"))
        }
    }

    private fun provideRetryMetadataInStringFormat(
        batchId: Int = 0,
        attempt: Int = 2,
        lastAttemptTimestampInMillis: Long = 1000L,
        reason: String = "server-500",
    ): String = RetryMetadata(
        batchId = batchId,
        attempt = attempt,
        lastAttemptTimestampInMillis = lastAttemptTimestampInMillis,
        reason = reason,
    ).toJson()

    /**
     * Verifies that retry metadata was written to storage with the expected values.
     * All parameters must match for the verification to pass.
     */
    private fun verifyMetadataWrittenToStorage(
        batchId: Int = 0,
        attempt: Int = 2,
        lastAttemptTimestampInMillis: Long = 1000L,
        reason: String = "server-500"
    ) {
        coVerify {
            storage.write(
                StorageKeys.RETRY_METADATA,
                match<String> { json ->
                    val parsed = RetryMetadata.fromJson(json)
                    parsed != null &&
                            parsed.batchId == batchId &&
                            parsed.attempt == attempt &&
                            parsed.lastAttemptTimestampInMillis == lastAttemptTimestampInMillis &&
                            parsed.reason == reason
                }
            )
        }
    }
}
