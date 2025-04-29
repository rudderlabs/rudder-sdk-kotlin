package com.rudderstack.sdk.kotlin.core.internals.network

import com.rudderstack.sdk.kotlin.core.internals.network.provider.provideHttpClientImplForGetRequest
import com.rudderstack.sdk.kotlin.core.internals.network.provider.provideHttpClientImplForPostRequest
import com.rudderstack.sdk.kotlin.core.internals.utils.Result
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.UnknownHostException

private const val WRONG_BASE_URL = "<wrong-url>"
private const val SUCCESS_RESPONSE = "Success Response"
private const val ERROR_RESPONSE = "Some error occurred"
private const val REQUEST_BODY = "body"

class HttpClientImplTest {

    @MockK
    private lateinit var mockConnectionFactory: HttpURLConnectionFactory

    @MockK
    private lateinit var mockConnection: HttpURLConnection

    private lateinit var getHttpClient: HttpClientImpl
    private lateinit var postHttpClient: HttpClientImpl

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        every { mockConnectionFactory.createConnection(any(), any()) } returns mockConnection

        // Mock extension functions:
        mockkStatic("com.rudderstack.sdk.kotlin.core.internals.network.WebUtilsKt")
        every { mockConnection.getSuccessResponse() } returns SUCCESS_RESPONSE
        every { mockConnection.getErrorResponse() } returns ERROR_RESPONSE

        getHttpClient = provideHttpClientImplForGetRequest(connectionFactory = mockConnectionFactory)
        postHttpClient = provideHttpClientImplForPostRequest(connectionFactory = mockConnectionFactory)
    }

    @Test
    fun `given connection is successful, when getData is called, then return Success`() {
        every { mockConnection.responseCode } returns 200

        val result = getHttpClient.getData()

        assertSuccess(result)
    }

    @Test
    fun `given connection is successful and response code is 2xx, when getData is called, then return Success`() {
        every { mockConnection.responseCode } returns 299

        val result = getHttpClient.getData()

        assertSuccess(result)
    }

    @Test
    fun `given that their will be an error while making a connection, when getData is called, then return Failure`() {
        val exception = UnknownHostException(ERROR_RESPONSE)
        every { mockConnection.connect() } throws exception

        val result = getHttpClient.getData()

        assertFailure(result, NetworkErrorStatus.ErrorNetworkUnavailable)
    }

    @Test
    fun `given connection is unsuccessful and response code is 400, when getData is called, then return Failure`() {
        every { mockConnection.responseCode } returns 400

        val result = getHttpClient.getData()

        assertFailure(
            result,
            NetworkErrorStatus.Error400
        )
    }

    @Test
    fun `given connection is unsuccessful and response code is 404, when getData is called, then return Failure`() {
        every { mockConnection.responseCode } returns 404

        val result = getHttpClient.getData()

        assertFailure(
            result,
            NetworkErrorStatus.Error404
        )
    }

    @Test
    fun `given connection is unsuccessful and response code is 5XX, when getData is called, then return retry able Failure`() {
        every { mockConnection.responseCode } returns 500

        val result = getHttpClient.getData()

        assertFailure(
            result,
            NetworkErrorStatus.ErrorRetry(statusCode = 500)
        )
    }

    @Test
    fun `given connection is unsuccessful and response code is 429, when getData is called, then return retry able Failure`() {
        every { mockConnection.responseCode } returns 429

        val result = getHttpClient.getData()

        assertFailure(
            result,
            NetworkErrorStatus.ErrorRetry(statusCode = 429)
        )
    }

    @Test
    fun `given connection is unsuccessful and response code is 4XX, when getData is called, then return Failure`() {
        every { mockConnection.responseCode } returns 450

        val result = getHttpClient.getData()

        assertFailure(
            result,
            NetworkErrorStatus.ErrorRetry(statusCode = 450)
        )
    }

    @Test
    fun `given wrong url, when getData is called, then throw MalformedURLException`() {
        assertThrows<MalformedURLException> {
            getHttpClient = provideHttpClientImplForGetRequest(
                connectionFactory = mockConnectionFactory,
                baseUrl = WRONG_BASE_URL,
            )
            getHttpClient.getData()
        }
    }

    @Test
    fun `given invalid write key, when getData is called, then return write key Failure`() {
        every { mockConnection.responseCode } returns 401

        val result = getHttpClient.getData()

        assertFailure(
            result,
            NetworkErrorStatus.Error401
        )
    }

    @Test
    fun `given connection is successful, when sendData is called, then return Success`() {
        every { mockConnection.responseCode } returns 200

        val result = getHttpClient.sendData(REQUEST_BODY)

        assertSuccess(result)
    }

    @Test
    fun `given connection is successful and response code is 2xx, when sendData is called, then return Success`() {
        every { mockConnection.responseCode } returns 299

        val result = postHttpClient.sendData(REQUEST_BODY)

        assertSuccess(result)
    }

    @Test
    fun `given that their will be an error while making a connection, when sendData is called, then return Failure`() {
        val exception = IOException(ERROR_RESPONSE)
        every { mockConnection.connect() } throws exception

        val result = postHttpClient.sendData(REQUEST_BODY)

        assertFailure(result, NetworkErrorStatus.ErrorRetry())
    }

    @Test
    fun `given connection is unsuccessful and response code is 400, when sendData is called, then return Failure`() {
        every { mockConnection.responseCode } returns 400

        val result = postHttpClient.sendData(REQUEST_BODY)

        assertFailure(
            result,
            NetworkErrorStatus.Error400
        )
    }

    @Test
    fun `given connection is unsuccessful and response code is 404, when sendData is called, then return Failure`() {
        every { mockConnection.responseCode } returns 404

        val result = postHttpClient.sendData(REQUEST_BODY)

        assertFailure(
            result,
            NetworkErrorStatus.Error404
        )
    }

    @Test
    fun `given connection is unsuccessful and response code is 429, when sendData is called, then return retry able Failure`() {
        every { mockConnection.responseCode } returns 429

        val result = postHttpClient.sendData(REQUEST_BODY)

        assertFailure(
            result,
            NetworkErrorStatus.ErrorRetry(statusCode = 429)
        )
    }

    @Test
    fun `given connection is unsuccessful and response code is 4XX, when sendData is called, then return Failure`() {
        every { mockConnection.responseCode } returns 450

        val result = postHttpClient.sendData(REQUEST_BODY)

        assertFailure(
            result,
            NetworkErrorStatus.ErrorRetry(statusCode = 450)
        )
    }

    @Test
    fun `given connection is unsuccessful and response code is 5XX, when sendData is called, then return retry able Failure`() {
        every { mockConnection.responseCode } returns 500

        val result = postHttpClient.sendData(REQUEST_BODY)

        assertFailure(
            result,
            NetworkErrorStatus.ErrorRetry(statusCode = 500)
        )
    }

    @Test
    fun `given wrong url, when sendData is called, then throw MalformedURLException`() {
        assertThrows<MalformedURLException> {
            postHttpClient = provideHttpClientImplForGetRequest(
                connectionFactory = mockConnectionFactory,
                baseUrl = WRONG_BASE_URL,
            )
            postHttpClient.sendData(REQUEST_BODY)
        }
    }

    @Test
    fun `given invalid write key, when sendData is called, then return write key Failure`() {
        every { mockConnection.responseCode } returns 401

        val result = postHttpClient.sendData(REQUEST_BODY)

        assertFailure(
            result,
            NetworkErrorStatus.Error401
        )
    }

    @Test
    fun `when two different instances of HttpClient are created for get and post requests, then they should not be equal`() {
        assertNotEquals(getHttpClient, postHttpClient)
    }

    @Test
    fun `given network is unavailable, when sendData is called, then return network unavailable error`() {
        // This is to simulate a network error.
        every { mockConnection.connect() } throws ConnectException()

        val result = postHttpClient.sendData(REQUEST_BODY)

        assertFailure(
            result,
            NetworkErrorStatus.ErrorNetworkUnavailable
        )
    }

    @Test
    fun `given IO exception is thrown, when sendData is called, then return retry able error`() {
        // This is to simulate a network error.
        every { mockConnection.connect() } throws IOException()

        val result = postHttpClient.sendData(REQUEST_BODY)

        assertFailure(
            result,
            NetworkErrorStatus.ErrorRetry()
        )
    }

    @Test
    fun `given any unknown exception is thrown, when sendData is called, then return unknown exception`() {
        // This is to simulate a network error.
        every { mockConnection.connect() } throws Exception()

        val result = postHttpClient.sendData(REQUEST_BODY)

        assertFailure(
            result,
            NetworkErrorStatus.ErrorUnknown
        )
    }

    private fun assertSuccess(result: NetworkResult) {
        assertTrue(result is Result.Success)
        verify { mockConnection.connect() }
        verify { mockConnection.disconnect() }
    }

    private fun assertFailure(
        result: NetworkResult,
        expectedStatus: NetworkErrorStatus
    ) {
        assertTrue(result is Result.Failure)
        verify { mockConnection.connect() }
        verify { mockConnection.disconnect() }

        result as Result.Failure // Safe cast after assertTrue

        assertEquals(expectedStatus, result.error)
    }
}
