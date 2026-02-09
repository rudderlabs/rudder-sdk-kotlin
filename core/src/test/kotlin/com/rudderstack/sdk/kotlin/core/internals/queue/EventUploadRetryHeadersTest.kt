package com.rudderstack.sdk.kotlin.core.internals.queue

import com.rudderstack.sdk.kotlin.core.Analytics
import com.rudderstack.sdk.kotlin.core.internals.models.SourceConfig
import com.rudderstack.sdk.kotlin.core.internals.network.HttpClient
import com.rudderstack.sdk.kotlin.core.internals.network.NetworkErrorStatus
import com.rudderstack.sdk.kotlin.core.internals.network.RetryAbleEventUploadError
import com.rudderstack.sdk.kotlin.core.internals.policies.backoff.MaxAttemptsWithBackoff
import com.rudderstack.sdk.kotlin.core.internals.statemanagement.State
import com.rudderstack.sdk.kotlin.core.internals.storage.Storage
import com.rudderstack.sdk.kotlin.core.internals.storage.StorageKeys
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import com.rudderstack.sdk.kotlin.core.internals.utils.empty
import com.rudderstack.sdk.kotlin.core.mockAnalytics
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.runs
import io.mockk.Ordering
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

private const val BATCH_PAYLOAD =
    """{"batch":[{"anonymousId":"test-id"}],"sentAt":"2024-01-01T00:00:00Z"}"""
private const val FILE_PATH =
    "/data/user/0/com.rudderstack.android.sampleapp/app_rudder-android-store/test-key-0"

class EventUploadRetryHeadersTest {

    @MockK
    private lateinit var mockStorage: Storage

    @MockK
    private lateinit var mockHttpClient: HttpClient

    @MockK
    private lateinit var mockMaxAttemptsWithBackoff: MaxAttemptsWithBackoff

    @MockK
    private lateinit var mockRetryHeadersProvider: RetryHeadersProvider

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val mockAnalytics: Analytics = mockAnalytics(testScope, testDispatcher)

    private lateinit var eventUpload: EventUpload

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        every { mockAnalytics.storage } returns mockStorage
        coEvery { mockStorage.close() } just runs
        coEvery { mockStorage.write(StorageKeys.EVENT, any<String>()) } just runs
        every { mockAnalytics.sourceConfigState } returns State(
            SourceConfig(
                source = SourceConfig.initialState().source.copy(isSourceEnabled = true)
            )
        )

        coEvery {
            mockStorage.readString(StorageKeys.EVENT, String.empty())
        } returns FILE_PATH
        every { mockStorage.readBatchContent(FILE_PATH) } returns BATCH_PAYLOAD

        eventUpload = EventUpload(
            analytics = mockAnalytics,
            httpClientFactory = mockHttpClient,
            maxAttemptsWithBackoff = mockMaxAttemptsWithBackoff,
            retryHeadersProvider = mockRetryHeadersProvider,
        )
    }

    @Test
    fun `given first attempt succeeds, when upload is called, then no retry headers are sent and metadata is cleared`() {
        every { mockHttpClient.sendData(any(), any()) } returns Result.Success("Ok")

        processMessage()

        verify(exactly = 1) { mockRetryHeadersProvider.getHeaders(0, any()) }
        verify(exactly = 1) { mockHttpClient.sendData(any(), emptyMap()) }
        coVerify(exactly = 1) { mockRetryHeadersProvider.clear() }
        coVerify(exactly = 0) { mockRetryHeadersProvider.recordFailure(any(), any(), any()) }
    }

    @Test
    fun `given retryable error occurs, when upload retries, then recordFailure is called with the error`() {
        stubSendDataToFailThenSucceed()

        processMessage()

        coVerify(exactly = 1) {
            mockRetryHeadersProvider.recordFailure(
                batchId = 0,
                timestampInMillis = any(),
                error = match { it is RetryAbleEventUploadError.ErrorRetry },
            )
        }
        coVerify(exactly = 1) { mockRetryHeadersProvider.clear() }
    }

    @Test
    fun `given retryable error occurs, when upload retries, then headers are passed to sendData`() {
        stubSendDataToFailThenSucceed()

        processMessage()

        verify { mockHttpClient.sendData(body = any(), additionalHeaders = provideRetryHeadersWithReason()) }
    }

    /**
     * Stubs a scenario where sendData fails with the given [errors] in order,
     * then succeeds on the next call. Also stubs getHeaders to return empty headers
     * on the first attempt and retry headers on subsequent attempts.
     *
     * Example: `stubSendDataToFailThenSucceed(listOf(ErrorRetry(500), ErrorTimeout))`
     * - Call 1: fails with ErrorRetry(500), getHeaders returns empty
     * - Call 2: fails with ErrorTimeout, getHeaders returns retry headers
     * - Call 3: succeeds, getHeaders returns retry headers
     */
    private fun stubSendDataToFailThenSucceed(
        errors: List<NetworkErrorStatus> = listOf(NetworkErrorStatus.ErrorRetry(500)),
    ) {
        // Shared counter tracks call progression across both stubs
        var callCount = 0
        // First call: no retry headers; subsequent calls: with retry headers
        every { mockRetryHeadersProvider.getHeaders(any(), any()) } answers {
            if (callCount > 0) provideRetryHeadersWithReason() else emptyMap()
        }
        // Fail with each error in order, then succeed
        every { mockHttpClient.sendData(any(), any()) } answers {
            val index = callCount++
            if (index < errors.size) Result.Failure(errors[index])
            else Result.Success("Ok")
        }
    }

    @ParameterizedTest(name = "given non-retryable {0}, when upload is called, then metadata is cleared")
    @MethodSource("nonRetryableErrors")
    fun `given non-retryable error, when upload is called, then metadata is cleared`(
        errorStatus: NetworkErrorStatus,
    ) {
        every { mockHttpClient.sendData(any(), any()) } returns Result.Failure(errorStatus)

        processMessage()

        coVerify(exactly = 1) { mockRetryHeadersProvider.clear() }
    }

    @Test
    fun `given multiple retryable errors, when upload eventually succeeds, then headers reflect each error reason in order`() {
        // Each entry: error to fail with -> retry reason reflected in the next attempt's headers
        val errorToReason = listOf(
            NetworkErrorStatus.ErrorRetry(500) to "server-500",
            NetworkErrorStatus.ErrorNetworkUnavailable to "client-network",
            NetworkErrorStatus.ErrorTimeout to "client-timeout",
            NetworkErrorStatus.ErrorUnknown to "client-unknown",
        )
        val networkErrorStatusList = errorToReason.map { it.first }
        val expectedRetryHeadersList =
            errorToReason.map { provideRetryHeadersWithReason(it.second) }

        // Simulate retry progression:
        // getHeaders returns empty on the first attempt,
        // then returns headers reflecting the previous failure's reason.
        var callCount = 0
        every { mockRetryHeadersProvider.getHeaders(any(), any()) } answers {
            if (callCount > 0) expectedRetryHeadersList[callCount - 1] else emptyMap()
        }
        // sendData fails with each error in order, then succeeds.
        every { mockHttpClient.sendData(any(), any()) } answers {
            val index = callCount++
            if (index < networkErrorStatusList.size) Result.Failure(networkErrorStatusList[index])
            else Result.Success("Ok")
        }

        processMessage()

        verifyOrder {
            // Attempt 1: no headers, fails with ErrorRetry(500)
            mockHttpClient.sendData(any(), emptyMap())
            // Attempt 2: reason reflects previous ErrorRetry(500)
            mockHttpClient.sendData(any(), provideRetryHeadersWithReason("server-500"))
            // Attempt 3: reason reflects previous ErrorNetworkUnavailable
            mockHttpClient.sendData(any(), provideRetryHeadersWithReason("client-network"))
            // Attempt 4: reason reflects previous ErrorTimeout
            mockHttpClient.sendData(any(), provideRetryHeadersWithReason("client-timeout"))
            // Attempt 5: reason reflects previous ErrorUnknown, succeeds
            mockHttpClient.sendData(any(), provideRetryHeadersWithReason("client-unknown"))
        }
        coVerify(ordering = Ordering.ORDERED) {
            mockRetryHeadersProvider.recordFailure(
                batchId = 0,
                timestampInMillis = any(),
                error = match { it is RetryAbleEventUploadError.ErrorRetry })
            mockRetryHeadersProvider.recordFailure(
                batchId = 0,
                timestampInMillis = any(),
                error = match { it is RetryAbleEventUploadError.ErrorNetworkUnavailable })
            mockRetryHeadersProvider.recordFailure(
                batchId = 0,
                timestampInMillis = any(),
                error = match { it is RetryAbleEventUploadError.ErrorTimeout })
            mockRetryHeadersProvider.recordFailure(
                batchId = 0,
                timestampInMillis = any(),
                error = match { it is RetryAbleEventUploadError.ErrorUnknown })
        }
        coVerify { mockRetryHeadersProvider.clear() }
    }

    private fun processMessage() {
        eventUpload.start()
        eventUpload.flush()
        testDispatcher.scheduler.advanceUntilIdle()
    }

    private fun provideRetryHeadersWithReason(reason: String = "server-500") = mapOf(
        "Rsa-Retry-Attempt" to "1",
        "Rsa-Since-Last-Attempt" to "3000",
        "Rsa-Retry-Reason" to reason,
    )

    companion object {

        @JvmStatic
        private fun nonRetryableErrors(): Stream<Arguments> = Stream.of(
            Arguments.of(NetworkErrorStatus.Error400),
            Arguments.of(NetworkErrorStatus.Error401),
            Arguments.of(NetworkErrorStatus.Error404),
            Arguments.of(NetworkErrorStatus.Error413),
        )
    }
}
