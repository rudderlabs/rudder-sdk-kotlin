package com.rudderstack.sdk.kotlin.core.internals.network

import com.rudderstack.sdk.kotlin.core.internals.network.NetworkErrorStatus.Companion.toErrorStatus
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class EventUploadResultTest {

    @Test
    fun `given successful network result, when converted, then returns success upload result with matching response`() {
        val responseString = "Test response"
        val networkResult = Result.Success(responseString) as NetworkResult

        val eventUploadResult = networkResult.toEventUploadResult()

        assertTrue(eventUploadResult is Success)
        assertEquals(responseString, (eventUploadResult as Success).response)
    }

    @ParameterizedTest(name = "given {0}, when converted, then returns {1}")
    @MethodSource("retryAbleErrorMappings")
    fun `given retry able network error, when converted, then returns corresponding retry able error`(
        networkError: NetworkErrorStatus,
        expectedError: RetryAbleEventUploadError
    ) {
        val networkResult = Result.Failure(networkError) as NetworkResult

        val eventUploadResult = networkResult.toEventUploadResult()

        assertEquals(expectedError, eventUploadResult)
    }

    @ParameterizedTest(name = "given {0}, when converted, then returns {1}")
    @MethodSource("nonRetryAbleErrorMappings")
    fun `given non-retry able network error, when converted, then returns corresponding non-retry able error`(
        networkError: NetworkErrorStatus,
        expectedError: NonRetryAbleEventUploadError
    ) {
        val networkResult = Result.Failure(networkError) as NetworkResult

        val eventUploadResult = networkResult.toEventUploadResult()

        assertEquals(expectedError, eventUploadResult)
    }

    @ParameterizedTest
    @MethodSource("nonRetryAbleErrorToStatusCodeMappings")
    fun `when non retry able error is given, then it returns the correct status code`(
        error: EventUploadError,
        expectedStatusCode: Int
    ) {
        val statusCode = error.statusCode

        assertEquals(expectedStatusCode, statusCode)
    }

    @ParameterizedTest
    @MethodSource("getListOfRetryAbleErrors")
    fun `when retry able error is provided, then it returns null status code`(
        retryAbleError: RetryAbleEventUploadError
    ) {
        val statusCode = retryAbleError.statusCode

        assertNull(statusCode)
    }

    @Test
    fun `given retry able error with status code, when converted, then returns the correct status code`() {
        val errorCode = 500
        val networkResult = Result.Failure(error = toErrorStatus(errorCode))

        val eventUploadResult = networkResult.toEventUploadResult()

        assertTrue(eventUploadResult is RetryAbleEventUploadError)
        val actualResponseCode = (eventUploadResult as RetryAbleEventUploadError).statusCode
        assertEquals(errorCode, actualResponseCode)
    }

    companion object {

        @JvmStatic
        fun retryAbleErrorMappings(): Stream<Arguments> = Stream.of(
            Arguments.of(NetworkErrorStatus.ErrorRetry(), RetryAbleEventUploadError.ErrorRetry()),
            Arguments.of(NetworkErrorStatus.ErrorNetworkUnavailable, RetryAbleEventUploadError.ErrorNetworkUnavailable),
            Arguments.of(NetworkErrorStatus.ErrorUnknown, RetryAbleEventUploadError.ErrorUnknown)
        )

        @JvmStatic
        fun nonRetryAbleErrorMappings(): Stream<Arguments> = Stream.of(
            Arguments.of(NetworkErrorStatus.Error400, NonRetryAbleEventUploadError.ERROR_400),
            Arguments.of(NetworkErrorStatus.Error401, NonRetryAbleEventUploadError.ERROR_401),
            Arguments.of(NetworkErrorStatus.Error404, NonRetryAbleEventUploadError.ERROR_404),
            Arguments.of(NetworkErrorStatus.Error413, NonRetryAbleEventUploadError.ERROR_413)
        )

        @JvmStatic
        fun nonRetryAbleErrorToStatusCodeMappings(): Stream<Arguments> = Stream.of(
            Arguments.of(NonRetryAbleEventUploadError.ERROR_400, 400),
            Arguments.of(NonRetryAbleEventUploadError.ERROR_401, 401),
            Arguments.of(NonRetryAbleEventUploadError.ERROR_404, 404),
            Arguments.of(NonRetryAbleEventUploadError.ERROR_413, 413),
        )

        @JvmStatic
        fun getListOfRetryAbleErrors(): Stream<Arguments> = Stream.of(
            Arguments.of(RetryAbleEventUploadError.ErrorRetry()),
            Arguments.of(RetryAbleEventUploadError.ErrorNetworkUnavailable),
            Arguments.of(RetryAbleEventUploadError.ErrorUnknown)
        )
    }
}
