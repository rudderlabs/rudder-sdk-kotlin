package com.rudderstack.core.internals.network

import com.rudderstack.core.internals.network.provider.provideErrorMessage
import com.rudderstack.core.internals.network.provider.provideHttpClientImplForGetRequest
import com.rudderstack.core.internals.network.provider.provideHttpClientImplForPostRequest
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
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


    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        every { mockConnectionFactory.createConnection(any(), any()) } returns mockConnection

        // Mock extension functions:
        mockkStatic("com.rudderstack.core.internals.network.WebUtilsKt")
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

        assertFailure(result, ErrorStatus.GENERAL_ERROR, exception)
    }

    @Test
    fun `given connection is unsuccessful and response code is 400, when getData is called, then return Failure`() {
        every { mockConnection.responseCode } returns 400

        val result = getHttpClient.getData()

        assertFailure(
            result,
            ErrorStatus.BAD_REQUEST,
            IOException(provideErrorMessage(400, mockConnection))
        )
    }

    @Test
    fun `given connection is unsuccessful and response code is 404, when getData is called, then return Failure`() {
        every { mockConnection.responseCode } returns 404

        val result = getHttpClient.getData()

        assertFailure(
            result,
            ErrorStatus.RESOURCE_NOT_FOUND,
            IOException(provideErrorMessage(404, mockConnection))
        )
    }

    @Test
    fun `given connection is unsuccessful and response code is 5XX, when getData is called, then return retry able Failure`() {
        every { mockConnection.responseCode } returns 500

        val result = getHttpClient.getData()

        assertFailure(
            result,
            ErrorStatus.SERVER_ERROR,
            IOException(provideErrorMessage(500, mockConnection))
        )
    }

    @Test
    fun `given connection is unsuccessful and response code is 429, when getData is called, then return retry able Failure`() {
        every { mockConnection.responseCode } returns 429

        val result = getHttpClient.getData()

        assertFailure(
            result,
            ErrorStatus.TOO_MANY_REQUESTS,
            IOException(provideErrorMessage(429, mockConnection))
        )
    }

    @Test
    fun `given connection is unsuccessful and response code is 4XX, when getData is called, then return Failure`() {
        every { mockConnection.responseCode } returns 450

        val result = getHttpClient.getData()

        assertFailure(
            result,
            ErrorStatus.GENERAL_ERROR,
            IOException(provideErrorMessage(450, mockConnection))
        )
    }

    @Test(expected = MalformedURLException::class)
    fun `given wrong url, when getData is called, then throw MalformedURLException`() {
        getHttpClient = provideHttpClientImplForGetRequest(
            connectionFactory = mockConnectionFactory,
            baseUrl = WRONG_BASE_URL,
        )
        getHttpClient.getData()
    }

    @Test
    fun `given invalid write key, when getData is called, then return write key Failure`() {
        every { mockConnection.responseCode } returns 401

        val result = getHttpClient.getData()

        assertFailure(
            result,
            ErrorStatus.INVALID_WRITE_KEY,
            IOException(provideErrorMessage(401, mockConnection))
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

        assertFailure(result, ErrorStatus.GENERAL_ERROR, exception)
    }

    @Test
    fun `given connection is unsuccessful and response code is 400, when sendData is called, then return Failure`() {
        every { mockConnection.responseCode } returns 400

        val result = postHttpClient.sendData(REQUEST_BODY)

        assertFailure(
            result,
            ErrorStatus.BAD_REQUEST,
            IOException(provideErrorMessage(400, mockConnection))
        )
    }

    @Test
    fun `given connection is unsuccessful and response code is 404, when sendData is called, then return Failure`() {
        every { mockConnection.responseCode } returns 404

        val result = postHttpClient.sendData(REQUEST_BODY)

        assertFailure(
            result,
            ErrorStatus.RESOURCE_NOT_FOUND,
            IOException(provideErrorMessage(404, mockConnection))
        )
    }

    @Test
    fun `given connection is unsuccessful and response code is 429, when sendData is called, then return retry able Failure`() {
        every { mockConnection.responseCode } returns 429

        val result = postHttpClient.sendData(REQUEST_BODY)

        assertFailure(
            result,
            ErrorStatus.TOO_MANY_REQUESTS,
            IOException(provideErrorMessage(429, mockConnection))
        )
    }

    @Test
    fun `given connection is unsuccessful and response code is 4XX, when sendData is called, then return Failure`() {
        every { mockConnection.responseCode } returns 450

        val result = postHttpClient.sendData(REQUEST_BODY)

        assertFailure(
            result,
            ErrorStatus.GENERAL_ERROR,
            IOException(provideErrorMessage(450, mockConnection))
        )
    }

    @Test
    fun `given connection is unsuccessful and response code is 5XX, when sendData is called, then return retry able Failure`() {
        every { mockConnection.responseCode } returns 500

        val result = postHttpClient.sendData(REQUEST_BODY)

        assertFailure(
            result,
            ErrorStatus.SERVER_ERROR,
            IOException(provideErrorMessage(500, mockConnection))
        )
    }

    @Test(expected = MalformedURLException::class)
    fun `given wrong url, when sendData is called, then throw MalformedURLException`() {
        postHttpClient = provideHttpClientImplForGetRequest(
            connectionFactory = mockConnectionFactory,
            baseUrl = WRONG_BASE_URL,
        )
        postHttpClient.sendData(REQUEST_BODY)
    }

    @Test
    fun `given invalid write key, when sendData is called, then return write key Failure`() {
        every { mockConnection.responseCode } returns 401

        val result = postHttpClient.sendData(REQUEST_BODY)

        assertFailure(
            result,
            ErrorStatus.INVALID_WRITE_KEY,
            IOException(provideErrorMessage(401, mockConnection))
        )
    }

    @Test
    fun `when two different instances of HttpClient are created for get and post requests, then they should not be equal`() {
        assertNotEquals(getHttpClient, postHttpClient)
    }

    private fun assertSuccess(result: Result<String>) {
        assertTrue(result is Result.Success)
        verify { mockConnection.connect() }
        verify { mockConnection.disconnect() }
    }

    private fun assertFailure(
        result: Result<String>,
        expectedStatus: ErrorStatus,
        expectedException: Exception
    ) {
        assertTrue(result is Result.Failure)
        verify { mockConnection.connect() }
        verify { mockConnection.disconnect() }

        result as Result.Failure // Safe cast after assertTrue

        assertEquals(expectedStatus, result.status)
        assertThrows(expectedException::class.java) { throw result.error }
    }
}
